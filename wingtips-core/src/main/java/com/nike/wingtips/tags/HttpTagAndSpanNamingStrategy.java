package com.nike.wingtips.tags;

import com.nike.internal.util.StringUtils;
import com.nike.wingtips.Span;
import com.nike.wingtips.SpanMutator;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * There are many libraries that facilitate HTTP Requests. This abstract class allows for a consistent approach to
 * naming and tagging a span with request and response details without having knowledge of the underlying libraries
 * facilitating the request/response.
 *
 * <p>Callers interface with the following public methods, which are final and surrounded with try/catch to avoid
 * having implementation exceptions bubble out (errors are logged, but do not propagate up to callers):
 * <ul>
 *     <li>{@link #getInitialSpanName(Object, HttpTagAndSpanNamingAdapter)}</li>
 *     <li>{@link #handleRequestTagging(Span, Object, HttpTagAndSpanNamingAdapter)}</li>
 *     <li>{@link #handleResponseTaggingAndFinalSpanName(Span, Object, Object, Throwable, HttpTagAndSpanNamingAdapter)}</li>
 * </ul>
 *
 * <p>Those caller-facing methods don't do anything themselves other than delegate to protected (overrideable) methods
 * that are intended for concrete implementations to flesh out. Some of those protected methods are abstract and
 * *must* be implemented, others have a default implementation that should serve for most use cases, but are still
 * overrideable if needed.
 *
 * <p>From a caller's standpoint, integration with this class is typically done in a request/response interceptor or
 * filter, in a pattern that looks something like:
 * <pre>
 * HttpTagAndSpanNamingStrategy&lt;RequestObj, ResponseObj> tagAndSpanNamingStrategy = ...;
 * HttpTagAndSpanNamingAdapter&lt;RequestObj, ResponseObj> tagAndSpanNamingAdapter = ...;
 *
 * public ResponseObj interceptRequest(RequestObj request) {
 *     // This code assumes you're surrounding the call with a span.
 *     Span spanAroundCall = generateSpanAroundCall(
 *         // Use the tag/name strategy's getInitialSpanName() method to generate the span name.
 *         tagAndSpanNamingStrategy.getInitialSpanName(request, tagAndSpanNamingAdapter)
 *     );
 *
 *     Throwable errorForTagging = null;
 *     ResponseObj response = null;
 *     try {
 *         // Do the request tagging.
 *         tagAndSpanNamingStrategy.handleRequestTagging(spanAroundCall, request, tagAndSpanNamingAdapter);
 *         
 *         // Keep a handle on the response for later so we can do response tagging and final span name.
 *         response = executeRequest(request);
 *
 *         return response;
 *     } catch(Throwable exception) {
 *         // Keep a handle on any error that gets thrown so it can contribute to the response tagging,
 *         //      but otherwise throw the exception like normal.
 *         errorForTagging = exception;
 *         
 *         throw exception;
 *     }
 *     finally {
 *         try {
 *             // Handle response/error tagging and final span name.
 *             tagAndSpanNamingStrategy.handleResponseTaggingAndFinalSpanName(
 *                 spanAroundCall, request, response, errorForTagging, tagAndSpanNamingAdapter
 *             );
 *         }
 *         finally {
 *             // Span.close() contains the span-finishing logic we want - if the spanAroundCall was an overall span
 *             //      (new trace) then tracer.completeRequestSpan() will be called, otherwise it's a subspan and
 *             //      tracer.completeSubSpan() will be called.
 *             spanAroundCall.close();
 *         }
 *     }
 * }
 * </pre>
 *
 * <p>Async request/response scenarios will look different, and/or the framework may require a different solution where
 * it's not a simple single method call that you surround with the naming and tagging logic, but the critical pieces
 * should always be the same:
 * <ol>
 *     <li>
 *         Call {@link #getInitialSpanName(Object, HttpTagAndSpanNamingAdapter)} to generate the initial span name.
 *         That method can return null, so you should have a backup naming strategy in case null is returned.
 *     </li>
 *     <li>Call {@link #handleRequestTagging(Span, Object, HttpTagAndSpanNamingAdapter)} before making the request.</li>
 *     <li>Keep track of the request, response, and any exception that gets thrown, so they can be used later.</li>
 *     <li>
 *         After the response finishes, or an exception is thrown that stops the request from completing normally, then
 *         call
 *         {@link #handleResponseTaggingAndFinalSpanName(Span, Object, Object, Throwable, HttpTagAndSpanNamingAdapter)},
 *         passing in the request, response, and exception (if any) that you've kept track of.
 *     </li>
 *     <li>Use try, catch, and finally blocks to keep things safe and contained in case things go wrong.</li>
 * </ol>
 *
 * @param <REQ> The object type representing the http request
 * @param <RES> The object type representing the http response
 *
 * @author Brandon Currie
 * @author Nic Munroe
 */
@SuppressWarnings("WeakerAccess")
public abstract class HttpTagAndSpanNamingStrategy<REQ, RES> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public final @Nullable String getInitialSpanName(
        @NotNull REQ request,
        @NotNull HttpTagAndSpanNamingAdapter<REQ, ?> adapter
    ) {
        //noinspection ConstantConditions
        if (request == null || adapter == null) {
            return null;
        }

        try {
            return doGetInitialSpanName(request, adapter);
        }
        catch (Throwable t) {
            // Impl methods should never throw an exception. If you're seeing this error pop up, the impl needs to
            //      be fixed.
            logger.error(
                "An unexpected error occurred while getting the initial span name. The error will be swallowed to "
                + "avoid doing any damage and null will be returned, but your span name may not be what you expect. "
                + "This error should be fixed.",
                t
            );
            return null;
        }
    }

    public final void handleRequestTagging(
        @NotNull Span span,
        @NotNull REQ request,
        @NotNull HttpTagAndSpanNamingAdapter<REQ, ?> adapter
    ) {
        //noinspection ConstantConditions
        if (span == null || request == null || adapter == null) {
            return;
        }

        try {
            doHandleRequestTagging(span, request, adapter);
        }
        catch (Throwable t) {
            // Impl methods should never throw an exception. If you're seeing this error pop up, the impl needs to
            //      be fixed.
            logger.error(
                "An unexpected error occurred while handling request tagging. The error will be swallowed to avoid "
                + "doing any damage, but your span may be missing some expected tags. This error should be fixed.",
                t
            );
        }
    }

    public final void handleResponseTaggingAndFinalSpanName(
        @NotNull Span span,
        @Nullable REQ request,
        @Nullable RES response,
        @Nullable Throwable error,
        @NotNull HttpTagAndSpanNamingAdapter<REQ, RES> adapter
    ) {
        //noinspection ConstantConditions
        if (span == null || adapter == null) {
            return;
        }

        try {
            doHandleResponseAndErrorTagging(span, request, response, error, adapter);
        }
        catch (Throwable t) {
            // Impl methods should never throw an exception. If you're seeing this error pop up, the impl needs to
            //      be fixed.
            logger.error(
                "An unexpected error occurred while handling response tagging. The error will be swallowed to avoid "
                + "doing any damage, but your span may be missing some expected tags. This error should be fixed.",
                t
            );
        }

        try {
            doDetermineAndSetFinalSpanName(span, request, response, error, adapter);
        }
        catch (Throwable t) {
            // Impl methods should never throw an exception. If you're seeing this error pop up, the impl needs to
            //      be fixed.
            logger.error(
                "An unexpected error occurred while finalizing the span name. The error will be swallowed to avoid "
                + "doing any damage, but the final span name may not be what you expect. This error should be fixed.",
                t
            );
        }

        try {
            doWingtipsTagging(span, request, response, error, adapter);
        }
        catch (Throwable t) {
            // Impl methods should never throw an exception. If you're seeing this error pop up, the impl needs to
            //      be fixed.
            logger.error(
                "An unexpected error occurred while finalizing the span name. The error will be swallowed to avoid "
                + "doing any damage, but the final span name may not be what you expect. This error should be fixed.",
                t
            );
        }
    }

    protected @Nullable String doGetInitialSpanName(
        @NotNull REQ request,
        @NotNull HttpTagAndSpanNamingAdapter<REQ, ?> adapter
    ) {
        return adapter.getInitialSpanName(request);
    }

    @SuppressWarnings("unused")
    protected void doDetermineAndSetFinalSpanName(
        @NotNull Span span,
        @Nullable REQ request,
        @Nullable RES response,
        @Nullable Throwable error,
        @NotNull HttpTagAndSpanNamingAdapter<REQ, RES> adapter
    ) {
        String finalSpanName = adapter.getFinalSpanName(request, response);

        if (StringUtils.isNotBlank(finalSpanName)) {
            SpanMutator.changeSpanName(span, finalSpanName);
        }
    }

    protected void putTagIfValueIsNotBlank(
        @NotNull Span span,
        @NotNull String tagKey,
        @Nullable Object tagValue
    ) {
        //noinspection ConstantConditions
        if (tagValue == null || span == null || tagKey == null) {
            return;
        }

        // tagValue is not null. Convert to string and check for blank.
        String tagValueString = tagValue.toString();

        if (StringUtils.isBlank(tagValueString)) {
            return;
        }

        // tagValue is not blank. Add it to the given span.
        span.putTag(tagKey, tagValueString);
    }

    @SuppressWarnings("unused")
    protected void doWingtipsTagging(
        @NotNull Span span,
        @Nullable REQ request,
        @Nullable RES response,
        @Nullable Throwable error,
        @NotNull HttpTagAndSpanNamingAdapter<REQ, RES> adapter
    ) {
        putTagIfValueIsNotBlank(span, WingtipsTags.SPAN_HANDLER, adapter.getSpanHandlerTagValue(request, response));
    }

    protected abstract void doHandleRequestTagging(
        @NotNull Span span,
        @NotNull REQ request,
        @NotNull HttpTagAndSpanNamingAdapter<REQ, ?> adapter
    );

    protected abstract void doHandleResponseAndErrorTagging(
        @NotNull Span span,
        @Nullable REQ request,
        @Nullable RES response,
        @Nullable Throwable error,
        @NotNull HttpTagAndSpanNamingAdapter<REQ, RES> adapter
    );

}
