package com.nike.wingtips.okhttp;

import com.nike.wingtips.Span;
import com.nike.wingtips.Tracer;
import com.nike.wingtips.http.HttpRequestTracingUtils;
import com.nike.wingtips.okhttp.util.RequestBuilderForPropagation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import static com.nike.wingtips.http.HttpRequestTracingUtils.propagateTracingHeaders;

/**
 * TODO: Class Description
 *
 * @author Nic Munroe
 */
public class WingtipsOkHttpClientInterceptor implements Interceptor {

    /**
     * If this is true then all downstream calls that this interceptor intercepts will be surrounded by a
     * subspan which will be started immediately before the call and completed as soon as the call completes.
     */
    protected final boolean surroundCallsWithSubspan;

    /**
     * Default constructor - sets {@link #surroundCallsWithSubspan} to true.
     */
    public WingtipsOkHttpClientInterceptor() {
        this(true);
    }

    /**
     * Constructor that lets you choose whether downstream calls will be surrounded with a subspan.
     *
     * @param surroundCallsWithSubspan pass in true to have downstream calls surrounded with a new span, false to only
     * propagate the current span's info downstream (no subspan).
     */
    public WingtipsOkHttpClientInterceptor(boolean surroundCallsWithSubspan) {
        this.surroundCallsWithSubspan = surroundCallsWithSubspan;
    }

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();


        Tracer tracer = Tracer.getInstance();
        Span spanAroundCall = null;
        try {
            if (surroundCallsWithSubspan) {
                // Will start a new trace if necessary, or a subspan if a trace is already in progress.
                spanAroundCall = tracer.startSpanInCurrentContext(getSubspanSpanName(request), Span.SpanPurpose.CLIENT);
            }

            Request.Builder requestWithTracing = request.newBuilder();

            propagateTracingHeaders(new RequestBuilderForPropagation(requestWithTracing), tracer.getCurrentSpan());

            return chain.proceed(requestWithTracing.build());
        }
        finally {
            if (spanAroundCall != null) {
                // Span.close() contains the logic we want - if the spanAroundCall was an overall span (new trace)
                //      then tracer.completeRequestSpan() will be called, otherwise it's a subspan and
                //      tracer.completeSubSpan() will be called.
                spanAroundCall.close();
            }
        }
    }

    /**
     * Returns the name that should be used for the subspan surrounding the call. Defaults to {@code
     * okhttp_downstream_call-[HTTP_METHOD]_[REQUEST_URI]} with any query string stripped, e.g. for a GET
     * call to https://foo.bar/baz?stuff=things, this would return {@code
     * "okhttp_downstream_call-GET_https://foo.bar/baz"}. You can override this method
     * to return something else if you want a different subspan name format.
     *
     * @param request The request that is about to be executed.
     * @return The name that should be used for the subspan surrounding the call.
     */
    protected String getSubspanSpanName(Request request) {
        return HttpRequestTracingUtils.getSubspanSpanNameForHttpRequest(
            "okhttp_downstream_call", request.method(), request.url().toString()
        );
    }
}
