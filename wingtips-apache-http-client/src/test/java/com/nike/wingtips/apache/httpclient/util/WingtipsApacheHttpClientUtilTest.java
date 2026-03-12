package com.nike.wingtips.apache.httpclient.util;

import com.nike.wingtips.Span;

import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static com.nike.wingtips.TraceHeaders.PARENT_SPAN_ID;
import static com.nike.wingtips.TraceHeaders.SPAN_ID;
import static com.nike.wingtips.TraceHeaders.TRACE_ID;
import static com.nike.wingtips.TraceHeaders.TRACE_SAMPLED;
import static com.nike.wingtips.http.HttpRequestTracingUtils.convertSampleableBooleanToExpectedB3Value;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

/**
 * Tests the functionality of {@link WingtipsApacheHttpClientUtil}.
 *
 * @author Nic Munroe
 */
public class WingtipsApacheHttpClientUtilTest {

    private HttpRequest requestMock;
    private RequestLine requestLineMock;

    @BeforeEach
    public void beforeMethod() {
        requestMock = mock(HttpRequest.class);
        requestLineMock = mock(RequestLine.class);

        doReturn(requestLineMock).when(requestMock).getRequestLine();
    }

    @Test
    public void code_coverage_hoops() {
        // jump!
        new WingtipsApacheHttpClientUtil();
    }

    public static Stream<Arguments> propagateTracingHeaders_works_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of(true, true),
            Arguments.of(true, false),
            Arguments.of(false, true),
            Arguments.of(false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("propagateTracingHeaders_works_as_expected_DataProvider")
    public void propagateTracingHeaders_works_as_expected(
        boolean requestIsNull, boolean spanIsNull
    ) {
        // given
        if (requestIsNull)
            requestMock = null;

        Span spanSpy = (spanIsNull)
                       ? null
                       : spy(Span.newBuilder(UUID.randomUUID().toString(), Span.SpanPurpose.CLIENT)
                                 .withParentSpanId(UUID.randomUUID().toString())
                                 .build());

        // when
        WingtipsApacheHttpClientUtil.propagateTracingHeaders(requestMock, spanSpy);

        // then
        if (requestIsNull || spanIsNull) {
            if (requestMock != null)
                verifyNoInteractions(requestMock);

            if (spanSpy != null)
                verifyNoInteractions(spanSpy);
        }
        else {
            verify(requestMock).setHeader(TRACE_ID, spanSpy.getTraceId());
            verify(requestMock).setHeader(SPAN_ID, spanSpy.getSpanId());
            verify(requestMock)
                .setHeader(TRACE_SAMPLED, convertSampleableBooleanToExpectedB3Value(spanSpy.isSampleable()));
            verify(requestMock).setHeader(PARENT_SPAN_ID, spanSpy.getParentSpanId());
        }
    }

    // See https://github.com/openzipkin/b3-propagation - we should pass "1" if it's sampleable, "0" if it's not.

    public static Stream<Arguments> propagateTracingHeaders_uses_B3_spec_for_sampleable_header_value_DataProvider() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("propagateTracingHeaders_uses_B3_spec_for_sampleable_header_value_DataProvider")
    public void propagateTracingHeaders_uses_B3_spec_for_sampleable_header_value(
        boolean sampleable
    ) {
        // given
        Span span = Span.newBuilder("foo", Span.SpanPurpose.CLIENT)
                        .withSampleable(sampleable)
                        .build();

        // when
        WingtipsApacheHttpClientUtil.propagateTracingHeaders(requestMock, span);

        // then
        verify(requestMock)
            .setHeader(TRACE_SAMPLED, convertSampleableBooleanToExpectedB3Value(span.isSampleable()));
    }

    public static Stream<Arguments> propagateTracingHeaders_only_sends_parent_span_id_header_if_parent_span_id_exists_DataProvider() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("propagateTracingHeaders_only_sends_parent_span_id_header_if_parent_span_id_exists_DataProvider")
    public void propagateTracingHeaders_only_sends_parent_span_id_header_if_parent_span_id_exists(
        boolean parentSpanIdExists
    ) {
        // given
        String parentSpanId = (parentSpanIdExists) ? UUID.randomUUID().toString() : null;
        Span span = Span.newBuilder("foo", Span.SpanPurpose.CLIENT)
                        .withParentSpanId(parentSpanId)
                        .build();

        // when
        WingtipsApacheHttpClientUtil.propagateTracingHeaders(requestMock, span);

        // then
        if (parentSpanIdExists) {
            verify(requestMock).setHeader(PARENT_SPAN_ID, parentSpanId);
        }
        else {
            verify(requestMock, never()).setHeader(eq(PARENT_SPAN_ID), anyString());
        }
    }

    public static Stream<Arguments> getFallbackSubspanSpanName_works_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of("someHttpMethod", "apachehttpclient_downstream_call-someHttpMethod"),
            Arguments.of(null, "apachehttpclient_downstream_call-UNKNOWN_HTTP_METHOD"),
            Arguments.of("", "apachehttpclient_downstream_call-UNKNOWN_HTTP_METHOD"),
            Arguments.of("[whitespace]", "apachehttpclient_downstream_call-UNKNOWN_HTTP_METHOD")
        );
    }

    @ParameterizedTest
    @MethodSource("getFallbackSubspanSpanName_works_as_expected_DataProvider")
    public void getFallbackSubspanSpanName_works_as_expected(String httpMethod, String expectedResult) {
        // given
        if ("[whitespace]".equals(httpMethod)) {
            httpMethod = "  \n\r\t  ";
        }
        doReturn(httpMethod).when(requestLineMock).getMethod();

        // when
        String result = WingtipsApacheHttpClientUtil.getFallbackSubspanSpanName(requestMock);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }
}