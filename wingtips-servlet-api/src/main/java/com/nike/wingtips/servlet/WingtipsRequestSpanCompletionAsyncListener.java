package com.nike.wingtips.servlet;

import com.nike.wingtips.Span;
import com.nike.wingtips.Tracer;
import com.nike.wingtips.servlet.ServletRuntime.Servlet3Runtime;
import com.nike.wingtips.tags.HttpTagAndSpanNamingAdapter;
import com.nike.wingtips.tags.HttpTagAndSpanNamingStrategy;
import com.nike.wingtips.tags.NoOpHttpTagAdapter;
import com.nike.wingtips.tags.NoOpHttpTagStrategy;
import com.nike.wingtips.util.TracingState;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.AsyncListener;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.nike.wingtips.util.AsyncWingtipsHelperJava7.runnableWithTracing;

/**
 * Helper class for {@link Servlet3Runtime} that implements {@link AsyncListener}, whose job is to complete the
 * overall request span when an async servlet request finishes. You should not need to worry about this class - it
 * is an internal implementation detail for {@link Servlet3Runtime}.
 *
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
class WingtipsRequestSpanCompletionAsyncListener implements AsyncListener {

    protected final TracingState originalRequestTracingState;
    // Used to prevent two threads from trying to close the span at the same time 
    protected final AtomicBoolean alreadyCompleted = new AtomicBoolean(false);
    protected final HttpTagAndSpanNamingStrategy<HttpServletRequest, HttpServletResponse> tagAndNamingStrategy;
    protected final HttpTagAndSpanNamingAdapter<HttpServletRequest, HttpServletResponse> tagAndNamingAdapter;

    WingtipsRequestSpanCompletionAsyncListener(
        TracingState originalRequestTracingState,
        HttpTagAndSpanNamingStrategy<HttpServletRequest, HttpServletResponse> tagAndNamingStrategy,
        HttpTagAndSpanNamingAdapter<HttpServletRequest, HttpServletResponse> tagAndNamingAdapter
    ) {
        this.originalRequestTracingState = originalRequestTracingState;

        // We should never get a null tag strategy or tag adapter in reality, but just in case we do, default to
        //      the no-op impls.
        if (tagAndNamingStrategy == null) {
            tagAndNamingStrategy = NoOpHttpTagStrategy.getDefaultInstance();
        }

        if (tagAndNamingAdapter == null) {
            tagAndNamingAdapter = NoOpHttpTagAdapter.getDefaultInstance();
        }

        this.tagAndNamingStrategy = tagAndNamingStrategy;
        this.tagAndNamingAdapter = tagAndNamingAdapter;
    }

    @Override
    public void onComplete(AsyncEvent event) {
        completeRequestSpan(event);
    }

    @Override
    public void onTimeout(AsyncEvent event) {
        completeRequestSpan(event);
    }

    @Override
    public void onError(AsyncEvent event) {
        completeRequestSpan(event);
    }

    @Override
    public void onStartAsync(AsyncEvent event) {
        // Another async event was started (e.g. via asyncContext.dispatch(...), which means this listener was
        //      removed and won't be called on completion unless we re-register (as per the javadocs for this
        //      method from the interface).
        AsyncContext eventAsyncContext = event.getAsyncContext();
        if (eventAsyncContext != null) {
            eventAsyncContext.addListener(this, event.getSuppliedRequest(), event.getSuppliedResponse());
        }
    }

    /**
     * The response object available from {@code AsyncContext#getResponse()} is only
     * guaranteed to be a {@code ServletResponse} but it <em>should</em> be an instance of
     * {@code HttpServletResponse}.
     */
    @SuppressWarnings("deprecation")
    protected void completeRequestSpan(AsyncEvent event) {
        // Async servlet stuff can trigger multiple completion methods depending on how the request is processed,
        //      but we only care about the first.
        if (alreadyCompleted.getAndSet(true)) {
            return;
        }

        ServletRequest request = event.getSuppliedRequest();
        ServletResponse response = event.getSuppliedResponse();

        final HttpServletRequest httpRequest = (request instanceof HttpServletRequest)
                                         ? (HttpServletRequest) request
                                         : null;
        final HttpServletResponse httpResponse = (response instanceof HttpServletResponse)
                                           ? (HttpServletResponse) response
                                           : null;
        final Throwable error = event.getThrowable();

        // Reattach the original tracing state and handle span finalization/completion.
        //noinspection deprecation
        runnableWithTracing(
            new Runnable() {
                @Override
                public void run() {
                    Span span = Tracer.getInstance().getCurrentSpan();

                    try {
                        // Handle response/error tagging and final span name.
                        tagAndNamingStrategy.handleResponseTaggingAndFinalSpanName(
                            span, httpRequest, httpResponse, error, tagAndNamingAdapter
                        );
                    }
                    finally {
                        // Complete the overall request span.
                        Tracer.getInstance().completeRequestSpan();
                    }
                }
            },
            originalRequestTracingState
        ).run();
    }
}
