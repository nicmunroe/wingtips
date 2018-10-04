package com.nike.wingtips.tags;

import com.nike.internal.util.StringUtils;
import com.nike.wingtips.http.HttpRequestTracingUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Implementations know how to extract basic HTTP properties from HTTP Request
 * and Response objects.  These are used by a {@link HttpTagAndSpanNamingStrategy}
 * to extract the necessary tag values.
 * 
 * @author Brandon Currie
 * 
 */
public abstract class HttpTagAndSpanNamingAdapter<REQ,RES> {

	/**
     * Returns the value that should be used for the "error" {@link com.nike.wingtips.Span#putTag(String, String)}
     * associated with the given response, or null if this response does not indicate an error. The criteria for
     * determining an error can change depending on whether it's a client span or server span, or there may even be
     * project-specific logic.
     *
     * <p>By default, this method considers a response to indicate an error if the {@link
     * #getResponseHttpStatus(Object)} is greater than or equal to 400, and will return that status code as a string.
     * This is a client-span-centric view - implementations of this class that represent server spans may want to
     * override this to have it only consider status codes greater than or equal to 500 to be errors.
     *
     * <p>NOTE: It's important that you return something non-empty and non-blank if want the span to be considered an
     * error. In particular, empty strings or strings that consist of only whitespace may be treated by callers
     * of this method the same as {@code null}.
     *
     * @return The value that should be used for the "error" {@link com.nike.wingtips.Span#putTag(String, String)}
     * associated with the given response, or null if this response does not indicate an error.
     */
    public @Nullable String getErrorResponseTagValue(@Nullable RES response) {
        Integer statusCode = getResponseHttpStatus(response);
        if (statusCode != null && statusCode >= 400) {
            return statusCode.toString();
        }

        // Status code does not indicate an error, so return null.
        return null;
    }

    /**
     * @return The full URL of the request, including scheme, host, and query params.
     * 
     * @param request - The request object to be inspected
     */
    public abstract @Nullable String getRequestUrl(@Nullable REQ request);
    
    /**
     * @return The path of the request (similar to {@link #getRequestUrl(Object)}, but omits scheme, host, and query
     * params).
     * 
     * @param request - The request object to be inspected
     */
    public abstract @Nullable String getRequestPath(@Nullable REQ request);

    // Impls must handle null request and/
    // Can return null - callers should gracefully handle null return value.
    public abstract @Nullable String getRequestUriPathTemplate(@Nullable REQ request, @Nullable RES response);
    
    /**
     * Returns the http status code from the provided response object
     * @param response To be inspected to determine the http status code
     * @return The {@code String} representation of the http status code
     */
    public abstract @Nullable Integer getResponseHttpStatus(@Nullable RES response);

    /**
     * The HTTP Method used. e.g "GET" or "POST" ..
     * @param request The request object to be inspected
     * @return The HTTP Method
     */
    public abstract @Nullable String getRequestHttpMethod(@Nullable REQ request);

    public abstract @Nullable String getHeaderSingleValue(@Nullable REQ request, @NotNull String headerKey);

    // May return either null or empty list if there are no matches.
    public abstract @Nullable List<String> getHeaderMultipleValue(@Nullable REQ request, @NotNull String headerKey);

    // Can return null - callers should omit prefix if this returns null.
    public @Nullable String getSpanNamePrefix(@Nullable REQ request) {
        return null;
    }

    // Can return null - callers should have a reasonable fallback if this returns null/blank
    public @Nullable String getInitialSpanName(@Nullable REQ request) {
        String prefix = getSpanNamePrefix(request);

        String defaultSpanName = HttpRequestTracingUtils.generateSafeSpanName(request, null, this);

        return (StringUtils.isBlank(prefix))
               ? defaultSpanName
               : prefix + "-" + defaultSpanName;
    }

    // Can return null - callers should not change span name if this returns null/blank
    public @Nullable String getFinalSpanName(@Nullable REQ request, @Nullable RES response) {
        return HttpRequestTracingUtils.generateSafeSpanName(request, response, this);
    }

    /**
     * @return The value that should be used for the {@link WingtipsTags#SPAN_HANDLER} tag, or null if you don't want
     * that tag added to spans. See the javadocs for {@link WingtipsTags#SPAN_HANDLER} for more details on what this
     * value should look like.
     */
    public abstract @Nullable String getSpanHandlerTagValue(@Nullable REQ request, @Nullable RES response);
}
