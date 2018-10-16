package com.nike.wingtips.servlet.tag;

import com.nike.wingtips.servlet.HttpSpanFactory;
import com.nike.wingtips.tags.HttpTagAndSpanNamingAdapter;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class ServletRequestTagAdapter extends HttpTagAndSpanNamingAdapter<HttpServletRequest, HttpServletResponse> {

    @SuppressWarnings("WeakerAccess")
    protected static ServletRequestTagAdapter DEFAULT_INSTANCE = new ServletRequestTagAdapter();

    /**
     * @return A reusable, thread-safe, singleton instance of this class that can be used by anybody who wants to use
     * this class and does not need any customization.
     */
    @SuppressWarnings("unchecked")
    public static ServletRequestTagAdapter getDefaultInstance() {
        return DEFAULT_INSTANCE;
    }

    /**
     * Since this class represents server requests/responses (not clients), we only want to consider HTTP status codes
     * greater than or equal to 500 to be an error. From a server's perspective, a 4xx response is the correct
     * response to a bad request, and should therefore not be considered an error (again, from the server's
     * perspective - the client may feel differently).
     *
     * @param response The response object.
     * @return The value of {@link #getResponseHttpStatus(HttpServletResponse)} if it is greater than or equal to 500,
     * or null otherwise.
     */
    @Override
    public @Nullable String getErrorResponseTagValue(@Nullable HttpServletResponse response) {
        Integer statusCode = getResponseHttpStatus(response);
        if (statusCode != null && statusCode >= 500) {
            return statusCode.toString();
        }

        // Status code does not indicate an error, so return null.
        return null;
    }

    /**
     * The default is to use {@code request.getRequestURI()}.
     * Another plausible alternative the full URL without parameters: {@code request.getRequestURL()}
     *
     * @param request
     *     - The {@code HttpServletRequest}
     */
    @Override
    public @Nullable String getRequestUrl(@Nullable HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        
        return request.getRequestURL().toString();
    }

    @Override
    public @Nullable Integer getResponseHttpStatus(@Nullable HttpServletResponse response) {
        if (response == null) {
            return null;
        }

        return response.getStatus();
    }

    @Override
    public @Nullable String getRequestHttpMethod(@Nullable HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        return request.getMethod();
    }


    @Override
    public @Nullable String getRequestPath(@Nullable HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        return request.getRequestURI();
    }

    @Override
    public @Nullable String getRequestUriPathTemplate(
        @Nullable HttpServletRequest request,
        @Nullable HttpServletResponse response
    ) {
        return HttpSpanFactory.determineUriPathTemplate(request);
    }

    @Override
    public @Nullable String getHeaderSingleValue(@Nullable HttpServletRequest request, @NotNull String headerKey) {
        if (request == null) {
            return null;
        }

        return request.getHeader(headerKey);
    }

    @Override
    public @Nullable List<String> getHeaderMultipleValue(
        @Nullable HttpServletRequest request, @NotNull String headerKey
    ) {
        if (request == null) {
            return null;
        }

        Enumeration<String> matchingHeadersEnum = request.getHeaders(headerKey);

        if (matchingHeadersEnum == null) {
            return null;
        }

        return Collections.list(matchingHeadersEnum);
    }

    @Override
    public @Nullable String getSpanHandlerTagValue(
        @Nullable HttpServletRequest request, @Nullable HttpServletResponse response
    ) {
        return "servlet";
    }
}
