package com.nike.wingtips.lightstep;

import com.nike.wingtips.Span;
import com.nike.wingtips.TraceAndSpanIdGenerator;
import com.nike.wingtips.lifecyclelistener.SpanLifecycleListener;

import io.opentracing.Tracer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

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

    private final Logger lightStepToWingtipsLogger = LoggerFactory.getLogger("LIGHTSTEP_SPAN_CONVERSION_OR_HANDLING_ERROR");

    private final AtomicLong spanHandlingErrorCounter = new AtomicLong(0);
    private long lastSpanHandlingErrorLogTimeEpochMillis = 0;
    private static final long MIN_SPAN_HANDLING_ERROR_LOG_INTERVAL_MILLIS = TimeUnit.SECONDS.toMillis(60);

    private Tracer tracer = null;

    public WingtipsToLightStepLifecycleListener(String serviceName, String accessToken, String satelliteUrl, int satellitePort ) {
        try {
            tracer = new com.lightstep.tracer.jre.JRETracer(
                new com.lightstep.tracer.shared.Options.OptionsBuilder()
                    .withAccessToken(accessToken)
                    .withComponentName(serviceName)
                    .withCollectorHost(satelliteUrl)
                    .withCollectorPort(satellitePort)
                    .build()
            );
        }
        catch (Throwable ex) {
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
        long durationMicros = TimeUnit.NANOSECONDS.toMicros(wingtipsSpan.getDurationNanos());
        long stopTimeMicros = startTimeMicros + durationMicros;

        String lsSpanId = convertWingTipToLightStep(wingtipsSpan.getSpanId());
        String lsParentId = convertWingTipToLightStep(wingtipsSpan.getParentSpanId());
        String lsTraceId = convertWingTipToLightStep(wingtipsSpan.getTraceId());

        // TODO: Replace spancontext object with converted wingtip spancontext (possible long or hex or lowerhex)
        try {
            io.opentracing.Span lsSpan = tracer.buildSpan(operationName)
                .withStartTimestamp(wingtipsSpan.getSpanStartTimeEpochMicros())
                .start();
            lsSpan.finish(stopTimeMicros);
        }
        // TODO: refactor/revisit
        catch (Throwable ex) {
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

    private String convertWingTipToLightStep(String wingtipId) {
        if (isAllowedNumChars(wingtipId)) {
            if (isLowerHex(wingtipId)) {
                // Already lowerhex with correct number of chars, no modifications needed.
                return wingtipId;
            }
            else if (isHex(wingtipId, true)) {
                // It wasn't lowerhex, but it is hex and it is the correct number of chars.
                // We can trivially convert to valid lowerhex by lowercasing the ID.
                String convertedId = wingtipId.toLowerCase();
                return convertedId;
            }
        }

        // If the originalId can be parsed as a long, then its sanitized ID is the lowerhex representation of that long.
        Long originalIdAsRawLong = attemptToConvertToLong(wingtipId);
        if (originalIdAsRawLong != null) {
            String convertedId = TraceAndSpanIdGenerator.longToUnsignedLowerHexString(originalIdAsRawLong);
            return convertedId;
        }

        return wingtipId;
    }
    /**
     *
     */
    private boolean isLowerHex(String id) {
        return isHex(id, false);
    }

    /**
     * Copied from Wingtips-Zipkin2 and slightly modified.
     *
     * @param id The ID to check for hexadecimal conformity.
     * @return true if the given id is hexadecimal, false if there are any characters that are not hexadecimal, with
     * the {@code allowUppercase} parameter determining whether uppercase hex characters are allowed.
     */
    private boolean isHex(String id, boolean allowUppercase) {
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
                // do nothing and move onto the next character.
            }
        }

        return true;
    }

    private boolean isAllowedNumChars(final String id) {
        return id.length() <= 16;
    }

    private Long attemptToConvertToLong(final String id) {
        try {
            return Long.valueOf(id);
        } catch (final NumberFormatException nfe) {
            return null;
        }
    }
}

