package com.nike.wingtips.lightstep;

import com.nike.wingtips.Span;
import com.nike.wingtips.TraceAndSpanIdGenerator;
import com.nike.wingtips.lifecyclelistener.SpanLifecycleListener;

import io.opentracing.Tracer;
import com.lightstep.tracer.shared.SpanContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
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

        long lsSpanId = TraceAndSpanIdGenerator.unsignedLowerHexStringToLong(wingtipsSpan.getSpanId());
        long lsParentId = TraceAndSpanIdGenerator.unsignedLowerHexStringToLong(wingtipsSpan.getParentSpanId());
        long lsTraceId = TraceAndSpanIdGenerator.unsignedLowerHexStringToLong(wingtipsSpan.getTraceId());

        String tagPurpose = wingtipsSpan.getSpanPurpose().toString();

        SpanContext lsSpanContext = new SpanContext(lsTraceId, lsParentId);

        try {
            io.opentracing.Span lsSpan = tracer.buildSpan(operationName)
                .withStartTimestamp(wingtipsSpan.getSpanStartTimeEpochMicros())
                    .asChildOf(lsSpanContext)
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
            lsSpan.finish(stopTimeMicros);
        }

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
}

