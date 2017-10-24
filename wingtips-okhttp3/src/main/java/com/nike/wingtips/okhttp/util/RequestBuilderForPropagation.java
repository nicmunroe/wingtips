package com.nike.wingtips.okhttp.util;

import com.nike.wingtips.Span;
import com.nike.wingtips.http.HttpObjectForPropagation;
import com.nike.wingtips.okhttp.WingtipsOkHttpClientInterceptor;

import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * An implementation of {@link HttpObjectForPropagation} that knows how to set headers on OkHttp {@link
 * Request.Builder} objects. This allows you to use the {@link
 * com.nike.wingtips.http.HttpRequestTracingUtils#propagateTracingHeaders(HttpObjectForPropagation, Span)} helper
 * method with OkHttp requests.
 *
 * <p>Wingtips users shouldn't need to use this class most of the time. Instead please refer to {@link
 * WingtipsOkHttpClientInterceptor} for an interceptor to integrate tracing with {@link OkHttpClient}s.
 *
 * @author Nic Munroe
 */
public class RequestBuilderForPropagation implements HttpObjectForPropagation {

    protected final Request.Builder requestBuilder;

    public RequestBuilderForPropagation(Request.Builder requestBuilder) {
        if (requestBuilder == null) {
            throw new IllegalArgumentException("requestBuilder cannot be null");
        }
        this.requestBuilder = requestBuilder;
    }

    @Override
    public void setHeader(String headerKey, String headerValue) {
        requestBuilder.header(headerKey, headerValue);
    }
}
