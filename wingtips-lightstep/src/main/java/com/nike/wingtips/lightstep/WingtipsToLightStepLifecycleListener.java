package com.nike.wingtips.lightstep;

import com.nike.wingtips.Span;
import com.nike.wingtips.TraceAndSpanIdGenerator;
import com.nike.wingtips.lifecyclelistener.SpanLifecycleListener;

import com.lightstep.tracer.shared.SpanContext;

import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.opentracing.Tracer;

/**
 * A{@link SpanLifecycleListener} that converts Wingtips {@link Span}s to [LightStep implementation of] an OpenTracing
 * {@link io.opentracing.Span}, and sends that data to the LightStep Satellites via
 * the {@link com.lightstep.tracer.jre.JRETracer}.
 *
 * We're adapting some of the prior work built in the Wingtips Zipkin2 plugin to handle conversion of span/trace/parent
 * IDs as well as frequency gates for exception logging.
 *
 * Required options used in the constructor are the LightStep access token (generated in project settings within
 * LightStep), service name (which will be assigned to all spans), Satellite URL and Satellite Port, which should both
 * reflect the address for the load balancer in front of the LightStep Satellites.
 *
 * @author parker@lightstep.com
 */

public class WingtipsToLightStepLifecycleListener implements SpanLifecycleListener {

    // we borrowed the logging and exception log rate limiting from the Zipkin plugin.
    private final Logger lightStepToWingtipsLogger = LoggerFactory.getLogger("LIGHTSTEP_SPAN_CONVERSION_OR_HANDLING_ERROR");

    protected final boolean enableIdSanitization = true;

    private final AtomicLong spanHandlingErrorCounter = new AtomicLong(0);
    private long lastSpanHandlingErrorLogTimeEpochMillis = 0;
    private static final long MIN_SPAN_HANDLING_ERROR_LOG_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(60);

    protected String serviceName;
    protected String accessToken;
    protected String satelliteUrl;
    protected int satellitePort;

    protected Tracer tracer = null;

    // Basic constructor which requires values to configure tracer and point span traffic from the transport library
    // to the LightStep Satellites.
    public WingtipsToLightStepLifecycleListener(String serviceName, String accessToken, String satelliteUrl, int satellitePort) {
        this.serviceName = serviceName;
        this.accessToken = accessToken;
        this.satelliteUrl = satelliteUrl;
        this.satellitePort = satellitePort;

        try {
            this.tracer = new com.lightstep.tracer.jre.JRETracer(
                    new com.lightstep.tracer.shared.Options.OptionsBuilder()
                            .withAccessToken(this.accessToken)
                            .withComponentName(this.serviceName)
                            .withCollectorHost(this.satelliteUrl)
                            .withCollectorPort(this.satellitePort)
                            .withVerbosity(4)
                            .build()
            );
        } catch (Throwable ex) {
            lightStepToWingtipsLogger.warn(
                    "There has been an issue with initializing the LightStep tracer: ", ex.toString());
        }
    }

    @Override
    public void spanSampled(Span wingtipsSpan) {
        // Do nothing
    }

    @Override
    public void spanStarted(Span wingtipsSpan) {
        // Do nothing
    }

    @Override
    public void spanCompleted(Span wingtipsSpan) {
        String operationName = wingtipsSpan.getSpanName();
        long startTimeMicros = wingtipsSpan.getSpanStartTimeEpochMicros();

        // Given we should only be in this method on span completion, we are not going to wrap this conversion in a
        // try/catch. duration should be set on the Wingtips span.
        long durationMicros = TimeUnit.NANOSECONDS.toMicros(wingtipsSpan.getDurationNanos());
        long stopTimeMicros = startTimeMicros + durationMicros;

        // parentId will get changed to reflect the Wingtips parent id. If there is no id then a value of 0 will get
        // converted into null on the LightStep Satellite. LightStep will require our Ids to be in long format.
        String wtSanitizedSpanId = sanitizeIdIfNecessary(wingtipsSpan.getSpanId(), false);
        String wtSanitizedTraceId = sanitizeIdIfNecessary(wingtipsSpan.getTraceId(), true);
        String wtSanitizedParentId = sanitizeIdIfNecessary(wingtipsSpan.getParentSpanId(), false);
        
        long lsParentId = 0;
        long lsSpanId = TraceAndSpanIdGenerator.unsignedLowerHexStringToLong(wtSanitizedSpanId);
        long lsTraceId = TraceAndSpanIdGenerator.unsignedLowerHexStringToLong(wtSanitizedTraceId);

        if (wingtipsSpan.getParentSpanId() != null) {
            lsParentId = TraceAndSpanIdGenerator.unsignedLowerHexStringToLong(wtSanitizedParentId);
        }

        // LightStep doesn't have the concept of purpose, but we use the OT semantic idea of span kind, including
        // server and client. These are added as tags.
        String tagPurpose = wingtipsSpan.getSpanPurpose().toString();

        SpanContext lsSpanContext = new SpanContext(lsTraceId, lsParentId);

        try {
            io.opentracing.Span lsSpan = tracer.buildSpan(operationName)
                    .withStartTimestamp(wingtipsSpan.getSpanStartTimeEpochMicros())
                    .asChildOf(lsSpanContext)
                    .ignoreActiveSpan()
                    .withTag("lightstep.trace_id", lsTraceId)
                    .withTag("lightstep.span_id", lsSpanId)
                    .start();

            for (Span.TimestampedAnnotation wingtipsAnnotation : wingtipsSpan.getTimestampedAnnotations()) {
                lsSpan.log(wingtipsAnnotation.getTimestampEpochMicros(), wingtipsAnnotation.getValue());
            }

            lsSpan.setTag("span.type", tagPurpose);

            for (Map.Entry<String, String> wtTag : wingtipsSpan.getTags().entrySet()) {
                lsSpan.setTag(wtTag.getKey(), wtTag.getValue());
            }

            if (!wtSanitizedSpanId.equals(wingtipsSpan.getSpanId())) {
                lsSpan.setTag("invalid.span_id", wingtipsSpan.getSpanId());
            }
            if (!wtSanitizedTraceId.equals(wingtipsSpan.getTraceId())) {
                lsSpan.setTag("invalid.trace_id", wingtipsSpan.getTraceId());
            }
            if (wtSanitizedParentId != null && !wtSanitizedParentId.equals(wingtipsSpan.getParentSpanId())) {
                lsSpan.setTag("invalid.parent_id", wingtipsSpan.getParentSpanId());
            }

            // on finish, the tracer library initialized on the creation of this listener will cache and transport the span
            // data to the LightStep Satellite.
            lsSpan.finish(stopTimeMicros);

        } catch (Throwable ex) {
            long currentBadSpanCount = spanHandlingErrorCounter.incrementAndGet();
            // Adopted from WingtipsToZipkinLifecycleListener from Wingtips-Zipkin2 plugin.
            // Only log once every MIN_SPAN_HANDLING_ERROR_LOG_INTERVAL_MILLIS time interval to prevent log spam from a
            // malicious (or broken) caller.
            long currentTimeMillis = System.currentTimeMillis();
            long timeSinceLastLogMsgMillis = currentTimeMillis - lastSpanHandlingErrorLogTimeEpochMillis;

            if (timeSinceLastLogMsgMillis >= MIN_SPAN_HANDLING_ERROR_LOG_INTERVAL_MILLIS) {
                // We're not synchronizing the read and write to lastSpanHandlingErrorLogTimeEpochMillis, and that's ok.
                // If we get a few extra log messages due to a race condition it's not the end of the world - we're
                // still satisfying the goal of not allowing a malicious caller to endlessly spam the logs.
                lastSpanHandlingErrorLogTimeEpochMillis = currentTimeMillis;

                lightStepToWingtipsLogger.warn(
                        "There have been {} spans that were not LightStep compatible, or that experienced an error during span handling. Latest example: "
                                + "wingtips_span_with_error=\"{}\", conversion_or_handling_error=\"{}\"",
                        currentBadSpanCount, wingtipsSpan.toKeyValueString(), ex.toString()
                );
            }
        }
    }

    protected String sanitizeIdIfNecessary(final String originalId, final boolean allow128Bit) {
        if (!enableIdSanitization) {
            return originalId;
        }

        if (originalId == null) {
            return null;
        }

        if (isAllowedNumChars(originalId, allow128Bit)) {
            if (isLowerHex(originalId)) {
                // Already lowerhex with correct number of chars, no modifications needed.
                return originalId;
            }
            else if (isHex(originalId, true)) {
                // It wasn't lowerhex, but it is hex and it is the correct number of chars.
                //      We can trivially convert to valid lowerhex by lowercasing the ID.
                String sanitizedId = originalId.toLowerCase();
                return sanitizedId;
            }
        }

        // If the originalId can be parsed as a long, then its sanitized ID is the lowerhex representation of that long.
        Long originalIdAsRawLong = attemptToConvertToLong(originalId);
        if (originalIdAsRawLong != null) {
            String sanitizedId = TraceAndSpanIdGenerator.longToUnsignedLowerHexString(originalIdAsRawLong);
            return sanitizedId;
        }

        // If the originalId can be parsed as a UUID and is allowed to be 128 bit,
        //      then its sanitized ID is that UUID with the dashes ripped out and forced lowercase.
        if (allow128Bit && attemptToConvertToUuid(originalId) != null) {
            String sanitizedId = originalId.replace("-", "").toLowerCase();
            return sanitizedId;
        }

        // No convenient/sensible conversion to a valid lowerhex ID was found.
        //      Do a SHA256 hash of the original ID to get a (deterministic) valid sanitized lowerhex ID that can be
        //      converted to a long, but only take the number of characters we're allowed to take. Truncation
        //      of a SHA digest like this is specifically allowed by the SHA algorithm - see Section 7
        //      ("TRUNCATION OF A MESSAGE DIGEST") here:
        //      https://csrc.nist.gov/csrc/media/publications/fips/180/4/final/documents/fips180-4-draft-aug2014.pdf
        int allowedNumChars = allow128Bit ? 32 : 16;
        String sanitizedId = DigestUtils.sha256Hex(originalId).toLowerCase().substring(0, allowedNumChars);
        return sanitizedId;
    }

    protected boolean isLowerHex(String id) {
        return isHex(id, false);
    }

    /**
     * Copied from {@code zipkin2.Span.validateHex(String)} and slightly modified.
     *
     * @param id The ID to check for hexadecimal conformity.
     * @param allowUppercase Pass true to allow uppercase A-F letters, false to force lowercase-hexadecimal check
     * (only a-f letters allowed).
     * @return true if the given id is hexadecimal, false if there are any characters that are not hexadecimal, with
     * the {@code allowUppercase} parameter determining whether uppercase hex characters are allowed.
     */
    protected boolean isHex(String id, boolean allowUppercase) {
        for (int i = 0, length = id.length(); i < length; i++) {
            char c = id.charAt(i);
            if ((c < '0' || c > '9') && (c < 'a' || c > 'f')) {
                // Not 0-9, and not a-f. So it's not lowerhex. If we don't allow uppercase then we can return false.
                if (!allowUppercase) {
                    return false;
                }
                else if (c < 'A' || c > 'F') {
                    // Uppercase is allowed but it's not A-F either, so we still have to return false.
                    return false;
                }

                // If we reach here inside this if-block, then it's an uppercase A-F and allowUppercase is true, so
                //      do nothing and move onto the next character.
            }
        }

        return true;
    }

    protected boolean isAllowedNumChars(final String id, final boolean allow128Bit) {
        if (allow128Bit) {
            return id.length() <= 16 || id.length() == 32;
        } else {
            return id.length() <= 16;
        }
    }

    protected Long attemptToConvertToLong(final String id) {
        try {
            return Long.valueOf(id);
        } catch (final NumberFormatException nfe) {
            return null;
        }
    }

    protected UUID attemptToConvertToUuid(String originalId) {
        try {
            return UUID.fromString(originalId);
        }
        catch(Exception t) {
            return null;
        }
    }
}
