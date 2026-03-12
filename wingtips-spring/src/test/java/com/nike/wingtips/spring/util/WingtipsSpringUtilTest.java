package com.nike.wingtips.spring.util;

import com.nike.internal.util.Pair;
import com.nike.wingtips.Span;
import com.nike.wingtips.Span.SpanPurpose;
import com.nike.wingtips.Tracer;
import com.nike.wingtips.spring.interceptor.WingtipsAsyncClientHttpRequestInterceptor;
import com.nike.wingtips.spring.interceptor.WingtipsClientHttpRequestInterceptor;
import com.nike.wingtips.spring.interceptor.tag.SpringHttpClientTagAdapter;
import com.nike.wingtips.spring.util.asynchelperwrapper.FailureCallbackWithTracing;
import com.nike.wingtips.spring.util.asynchelperwrapper.ListenableFutureCallbackWithTracing;
import com.nike.wingtips.spring.util.asynchelperwrapper.SuccessCallbackWithTracing;
import com.nike.wingtips.tags.HttpTagAndSpanNamingAdapter;
import com.nike.wingtips.tags.HttpTagAndSpanNamingStrategy;
import com.nike.wingtips.tags.ZipkinHttpTagStrategy;
import com.nike.wingtips.testutil.Whitebox;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.ListenableFutureCallback;
import org.springframework.util.concurrent.SuccessCallback;
import org.springframework.web.client.AsyncRestTemplate;
import org.springframework.web.client.RestTemplate;

import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static com.nike.wingtips.TraceHeaders.PARENT_SPAN_ID;
import static com.nike.wingtips.TraceHeaders.SPAN_ID;
import static com.nike.wingtips.TraceHeaders.TRACE_ID;
import static com.nike.wingtips.TraceHeaders.TRACE_SAMPLED;
import static com.nike.wingtips.spring.testutils.TestUtils.convertSampleableBooleanToExpectedB3Value;
import static com.nike.wingtips.spring.util.WingtipsSpringUtil.failureCallbackWithTracing;
import static com.nike.wingtips.spring.util.WingtipsSpringUtil.listenableFutureCallbackWithTracing;
import static com.nike.wingtips.spring.util.WingtipsSpringUtil.successCallbackWithTracing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

/**
 * Tests the functionality of {@link WingtipsSpringUtil}.
 *
 * @author Nic Munroe
 */
public class WingtipsSpringUtilTest {

    private HttpMessage httpMessageMock;
    private HttpHeaders headersMock;

    private SuccessCallback successCallbackMock;
    private FailureCallback failureCallbackMock;
    private ListenableFutureCallback listenableFutureCallbackMock;

    private HttpTagAndSpanNamingStrategy<HttpRequest, ClientHttpResponse> tagStrategyMock;
    private HttpTagAndSpanNamingAdapter<HttpRequest, ClientHttpResponse> tagAdapterMock;

    @BeforeEach
    public void beforeMethod() {
        resetTracing();

        httpMessageMock = mock(HttpMessage.class);
        headersMock = mock(HttpHeaders.class);
        doReturn(headersMock).when(httpMessageMock).getHeaders();

        successCallbackMock = mock(SuccessCallback.class);
        failureCallbackMock = mock(FailureCallback.class);
        listenableFutureCallbackMock = mock(ListenableFutureCallback.class);

        tagStrategyMock = mock(HttpTagAndSpanNamingStrategy.class);
        tagAdapterMock = mock(HttpTagAndSpanNamingAdapter.class);
    }

    @AfterEach
    public void afterMethod() {
        resetTracing();
    }

    private void verifyInterceptorFieldValues(
        Object wingtipsInterceptor,
        boolean expectedSubspanOptionValue,
        HttpTagAndSpanNamingStrategy<HttpRequest, ClientHttpResponse> expectedTagStrategy,
        HttpTagAndSpanNamingAdapter<HttpRequest, ClientHttpResponse> expectedTagAdapter
    ) {
        assertThat(Whitebox.getInternalState(wingtipsInterceptor, "surroundCallsWithSubspan"))
            .isEqualTo(expectedSubspanOptionValue);
        assertThat(Whitebox.getInternalState(wingtipsInterceptor, "tagAndNamingStrategy"))
            .isSameAs(expectedTagStrategy);
        assertThat(Whitebox.getInternalState(wingtipsInterceptor, "tagAndNamingAdapter"))
            .isSameAs(expectedTagAdapter);
    }

    @Test
    public void code_coverage_hoops() {
        // Jump!
        Throwable ex = catchThrowable(WingtipsSpringUtil::new);
        assertThat(ex).isNull();
    }

    @Test
    public void createTracingEnabledRestTemplate_no_args_returns_RestTemplate_with_wingtips_interceptor_added_with_expected_fields() {
        // when
        RestTemplate result = WingtipsSpringUtil.createTracingEnabledRestTemplate();

        // then
        assertThat(result.getInterceptors()).hasSize(1);
        assertThat(result.getInterceptors().get(0)).isInstanceOf(WingtipsClientHttpRequestInterceptor.class);
        verifyInterceptorFieldValues(
            result.getInterceptors().get(0),
            true,
            ZipkinHttpTagStrategy.getDefaultInstance(),
            SpringHttpClientTagAdapter.getDefaultInstance()
        );
    }

    public static Stream<Arguments> createTracingEnabledRestTemplate_single_arg_returns_RestTemplate_with_wingtips_interceptor_added_with_expected_fields_DataProvider() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("createTracingEnabledRestTemplate_single_arg_returns_RestTemplate_with_wingtips_interceptor_added_with_expected_fields_DataProvider")
    public void createTracingEnabledRestTemplate_single_arg_returns_RestTemplate_with_wingtips_interceptor_added_with_expected_fields(
        boolean subspanOptionOn
    ) {
        // when
        RestTemplate result = WingtipsSpringUtil.createTracingEnabledRestTemplate(subspanOptionOn);

        // then
        assertThat(result.getInterceptors()).hasSize(1);
        assertThat(result.getInterceptors().get(0)).isInstanceOf(WingtipsClientHttpRequestInterceptor.class);
        verifyInterceptorFieldValues(
            result.getInterceptors().get(0),
            subspanOptionOn,
            ZipkinHttpTagStrategy.getDefaultInstance(),
            SpringHttpClientTagAdapter.getDefaultInstance()
        );
    }

    public static Stream<Arguments> createTracingEnabledRestTemplate_with_tag_and_span_naming_args_returns_RestTemplate_with_wingtips_interceptor_added_with_expected_fields_DataProvider() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("createTracingEnabledRestTemplate_with_tag_and_span_naming_args_returns_RestTemplate_with_wingtips_interceptor_added_with_expected_fields_DataProvider")
    public void createTracingEnabledRestTemplate_with_tag_and_span_naming_args_returns_RestTemplate_with_wingtips_interceptor_added_with_expected_fields(
        boolean subspanOptionOn
    ) {
        // when
        RestTemplate result = WingtipsSpringUtil.createTracingEnabledRestTemplate(
            subspanOptionOn, tagStrategyMock, tagAdapterMock
        );

        // then
        assertThat(result.getInterceptors()).hasSize(1);
        assertThat(result.getInterceptors().get(0)).isInstanceOf(WingtipsClientHttpRequestInterceptor.class);
        verifyInterceptorFieldValues(
            result.getInterceptors().get(0),
            subspanOptionOn,
            tagStrategyMock,
            tagAdapterMock
        );
    }

    private enum NullConstructorArgsScenario {
        NULL_STRATEGY_ARG(
            null,
            mock(HttpTagAndSpanNamingAdapter.class),
            "tagAndNamingStrategy cannot be null - if you really want no strategy, use NoOpHttpTagStrategy"
        ),
        NULL_ADAPTER_ARG(
            mock(HttpTagAndSpanNamingStrategy.class),
            null,
            "tagAndNamingAdapter cannot be null - if you really want no adapter, use NoOpHttpTagAdapter"
        );

        public final HttpTagAndSpanNamingStrategy<HttpRequest, ClientHttpResponse> strategy;
        public final HttpTagAndSpanNamingAdapter<HttpRequest, ClientHttpResponse> adapter;
        public final String expectedExceptionMessage;

        NullConstructorArgsScenario(
            HttpTagAndSpanNamingStrategy<HttpRequest, ClientHttpResponse> strategy,
            HttpTagAndSpanNamingAdapter<HttpRequest, ClientHttpResponse> adapter,
            String expectedExceptionMessage
        ) {
            this.strategy = strategy;
            this.adapter = adapter;
            this.expectedExceptionMessage = expectedExceptionMessage;
        }
    }

    public static Stream<Arguments> createTracingEnabledRestTemplate_with_tag_and_span_naming_args_throws_IllegalArgumentException_if_passed_null_args_DataProvider() {
        return Stream.of(
            Arguments.of(NullConstructorArgsScenario.NULL_STRATEGY_ARG),
            Arguments.of(NullConstructorArgsScenario.NULL_ADAPTER_ARG)
        );
    }

    @ParameterizedTest
    @MethodSource("createTracingEnabledRestTemplate_with_tag_and_span_naming_args_throws_IllegalArgumentException_if_passed_null_args_DataProvider")
    public void createTracingEnabledRestTemplate_with_tag_and_span_naming_args_throws_IllegalArgumentException_if_passed_null_args(
        NullConstructorArgsScenario scenario
    ) {
        // when
        Throwable ex = catchThrowable(
            () -> WingtipsSpringUtil.createTracingEnabledRestTemplate(true, scenario.strategy, scenario.adapter)
        );

        // then
        assertThat(ex)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(scenario.expectedExceptionMessage);
    }

    @Test
    public void createTracingEnabledAsyncRestTemplate_no_args_returns_AsyncRestTemplate_with_wingtips_interceptor_added_with_expected_fields() {
        // when
        AsyncRestTemplate result = WingtipsSpringUtil.createTracingEnabledAsyncRestTemplate();

        // then
        assertThat(result.getInterceptors()).hasSize(1);
        assertThat(result.getInterceptors().get(0)).isInstanceOf(WingtipsAsyncClientHttpRequestInterceptor.class);
        verifyInterceptorFieldValues(
            result.getInterceptors().get(0),
            true,
            ZipkinHttpTagStrategy.getDefaultInstance(),
            SpringHttpClientTagAdapter.getDefaultInstance()
        );
    }

    public static Stream<Arguments> createTracingEnabledAsyncRestTemplate_single_arg_returns_AsyncRestTemplate_with_wingtips_interceptor_added_with_expected_fields_DataProvider() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("createTracingEnabledAsyncRestTemplate_single_arg_returns_AsyncRestTemplate_with_wingtips_interceptor_added_with_expected_fields_DataProvider")
    public void createTracingEnabledAsyncRestTemplate_single_arg_returns_AsyncRestTemplate_with_wingtips_interceptor_added_with_expected_fields(
        boolean subspanOptionOn
    ) {
        // when
        AsyncRestTemplate result = WingtipsSpringUtil.createTracingEnabledAsyncRestTemplate(subspanOptionOn);

        // then
        assertThat(result.getInterceptors()).hasSize(1);
        assertThat(result.getInterceptors().get(0)).isInstanceOf(WingtipsAsyncClientHttpRequestInterceptor.class);
        verifyInterceptorFieldValues(
            result.getInterceptors().get(0),
            subspanOptionOn,
            ZipkinHttpTagStrategy.getDefaultInstance(),
            SpringHttpClientTagAdapter.getDefaultInstance()
        );
    }

    public static Stream<Arguments> createTracingEnabledAsyncRestTemplate_with_tag_and_span_naming_args_returns_AsyncRestTemplate_with_wingtips_interceptor_added_with_expected_fields_DataProvider() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("createTracingEnabledAsyncRestTemplate_with_tag_and_span_naming_args_returns_AsyncRestTemplate_with_wingtips_interceptor_added_with_expected_fields_DataProvider")
    public void createTracingEnabledAsyncRestTemplate_with_tag_and_span_naming_args_returns_AsyncRestTemplate_with_wingtips_interceptor_added_with_expected_fields(
        boolean subspanOptionOn
    ) {
        // when
        AsyncRestTemplate result = WingtipsSpringUtil.createTracingEnabledAsyncRestTemplate(
            subspanOptionOn, tagStrategyMock, tagAdapterMock
        );

        // then
        assertThat(result.getInterceptors()).hasSize(1);
        assertThat(result.getInterceptors().get(0)).isInstanceOf(WingtipsAsyncClientHttpRequestInterceptor.class);
        verifyInterceptorFieldValues(
            result.getInterceptors().get(0),
            subspanOptionOn,
            tagStrategyMock,
            tagAdapterMock
        );
    }

    public static Stream<Arguments> createTracingEnabledAsyncRestTemplate_with_tag_and_span_naming_args_throws_IllegalArgumentException_if_passed_null_args_DataProvider() {
        return Stream.of(
            Arguments.of(NullConstructorArgsScenario.NULL_STRATEGY_ARG),
            Arguments.of(NullConstructorArgsScenario.NULL_ADAPTER_ARG)
        );
    }

    @ParameterizedTest
    @MethodSource("createTracingEnabledAsyncRestTemplate_with_tag_and_span_naming_args_throws_IllegalArgumentException_if_passed_null_args_DataProvider")
    public void createTracingEnabledAsyncRestTemplate_with_tag_and_span_naming_args_throws_IllegalArgumentException_if_passed_null_args(
        NullConstructorArgsScenario scenario
    ) {
        // when
        Throwable ex = catchThrowable(
            () -> WingtipsSpringUtil.createTracingEnabledAsyncRestTemplate(true, scenario.strategy, scenario.adapter)
        );

        // then
        assertThat(ex)
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage(scenario.expectedExceptionMessage);
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
        boolean httpMessageIsNull, boolean spanIsNull
    ) {
        // given
        if (httpMessageIsNull)
            httpMessageMock = null;

        Span span = (spanIsNull)
                    ? null
                    : Span.newBuilder(UUID.randomUUID().toString(), SpanPurpose.CLIENT)
                          .withParentSpanId(UUID.randomUUID().toString())
                          .build();

        // when
        WingtipsSpringUtil.propagateTracingHeaders(httpMessageMock, span);

        // then
        if (httpMessageIsNull || spanIsNull) {
            verifyNoInteractions(headersMock);
        }
        else {
            verify(headersMock).set(TRACE_ID, span.getTraceId());
            verify(headersMock).set(SPAN_ID, span.getSpanId());
            verify(headersMock).set(TRACE_SAMPLED, convertSampleableBooleanToExpectedB3Value(span.isSampleable()));
            verify(headersMock).set(PARENT_SPAN_ID, span.getParentSpanId());
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
        Span span = Span.newBuilder("foo", SpanPurpose.CLIENT)
                        .withSampleable(sampleable)
                        .build();

        // when
        WingtipsSpringUtil.propagateTracingHeaders(httpMessageMock, span);

        // then
        verify(headersMock).set(TRACE_SAMPLED, convertSampleableBooleanToExpectedB3Value(span.isSampleable()));
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
        Span span = Span.newBuilder("foo", SpanPurpose.CLIENT)
                        .withParentSpanId(parentSpanId)
                        .build();

        // when
        WingtipsSpringUtil.propagateTracingHeaders(httpMessageMock, span);

        // then
        if (parentSpanIdExists) {
            verify(headersMock).set(PARENT_SPAN_ID, parentSpanId);
        }
        else {
            verify(headersMock, never()).set(eq(PARENT_SPAN_ID), anyString());
        }
    }

    public static Stream<Arguments> getRequestMethodAsString_works_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of(HttpMethod.GET),
            Arguments.of(HttpMethod.HEAD),
            Arguments.of(HttpMethod.POST),
            Arguments.of(HttpMethod.PUT),
            Arguments.of(HttpMethod.PATCH),
            Arguments.of(HttpMethod.DELETE),
            Arguments.of(HttpMethod.OPTIONS),
            Arguments.of(HttpMethod.TRACE),
            Arguments.of((Object) null)
        );
    }

    @ParameterizedTest
    @MethodSource("getRequestMethodAsString_works_as_expected_DataProvider")
    public void getRequestMethodAsString_works_as_expected(
        HttpMethod method
    ) {
        // given
        String expectedResult = (method == null) ? "UNKNOWN_HTTP_METHOD" : method.name();

        // when
        String result = WingtipsSpringUtil.getRequestMethodAsString(method);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    private void resetTracing() {
        MDC.clear();
        Tracer.getInstance().unregisterFromThread();
    }

    private Pair<Deque<Span>, Map<String, String>> generateTracingInfo() {
        resetTracing();
        Tracer.getInstance().startRequestWithRootSpan("someSpan");
        Pair<Deque<Span>, Map<String, String>> result = Pair.of(
            Tracer.getInstance().getCurrentSpanStackCopy(), new HashMap<>(MDC.getCopyOfContextMap())
        );
        resetTracing();
        return result;
    }

    private Pair<Deque<Span>, Map<String, String>> setupCurrentThreadWithTracingInfo() {
        resetTracing();
        Tracer.getInstance().startRequestWithRootSpan("request-" + UUID.randomUUID().toString());
        return Pair.of(Tracer.getInstance().getCurrentSpanStackCopy(), MDC.getCopyOfContextMap());
    }

    private void verifySuccessCallbackWithTracing(SuccessCallback result,
                                                  SuccessCallback expectedCoreInstance,
                                                  Deque<Span> expectedSpanStack,
                                                  Map<String, String> expectedMdcInfo) {
        assertThat(result).isInstanceOf(SuccessCallbackWithTracing.class);
        assertThat(Whitebox.getInternalState(result, "origSuccessCallback")).isSameAs(expectedCoreInstance);
        assertThat(Whitebox.getInternalState(result, "spanStackForExecution")).isEqualTo(expectedSpanStack);
        assertThat(Whitebox.getInternalState(result, "mdcContextMapForExecution")).isEqualTo(expectedMdcInfo);
    }

    @Test
    public void successCallbackWithTracing_using_current_thread_info_works_as_expected() {
        // given
        Pair<Deque<Span>, Map<String, String>> setupInfo = setupCurrentThreadWithTracingInfo();

        // when
        SuccessCallback result = successCallbackWithTracing(successCallbackMock);

        // then
        verifySuccessCallbackWithTracing(result, successCallbackMock, setupInfo.getLeft(), setupInfo.getRight());
    }

    @Test
    public void successCallbackWithTracing_pair_works_as_expected() {
        // given
        Pair<Deque<Span>, Map<String, String>> setupInfo = generateTracingInfo();

        // when
        SuccessCallback result = successCallbackWithTracing(successCallbackMock, setupInfo);

        // then
        verifySuccessCallbackWithTracing(result, successCallbackMock, setupInfo.getLeft(), setupInfo.getRight());
    }

    @Test
    public void successCallbackWithTracing_separate_args_works_as_expected() {
        // given
        Pair<Deque<Span>, Map<String, String>> setupInfo = generateTracingInfo();

        // when
        SuccessCallback result = successCallbackWithTracing(
            successCallbackMock, setupInfo.getLeft(), setupInfo.getRight()
        );

        // then
        verifySuccessCallbackWithTracing(result, successCallbackMock, setupInfo.getLeft(), setupInfo.getRight());
    }

    private void verifyFailureCallbackWithTracing(FailureCallback result,
                                                  FailureCallback expectedCoreInstance,
                                                  Deque<Span> expectedSpanStack,
                                                  Map<String, String> expectedMdcInfo) {
        assertThat(result).isInstanceOf(FailureCallbackWithTracing.class);
        assertThat(Whitebox.getInternalState(result, "origFailureCallback")).isSameAs(expectedCoreInstance);
        assertThat(Whitebox.getInternalState(result, "spanStackForExecution")).isEqualTo(expectedSpanStack);
        assertThat(Whitebox.getInternalState(result, "mdcContextMapForExecution")).isEqualTo(expectedMdcInfo);
    }

    @Test
    public void failureCallbackWithTracing_using_current_thread_info_works_as_expected() {
        // given
        Pair<Deque<Span>, Map<String, String>> setupInfo = setupCurrentThreadWithTracingInfo();

        // when
        FailureCallback result = failureCallbackWithTracing(failureCallbackMock);

        // then
        verifyFailureCallbackWithTracing(result, failureCallbackMock, setupInfo.getLeft(), setupInfo.getRight());
    }

    @Test
    public void failureCallbackWithTracing_pair_works_as_expected() {
        // given
        Pair<Deque<Span>, Map<String, String>> setupInfo = generateTracingInfo();

        // when
        FailureCallback result = failureCallbackWithTracing(failureCallbackMock, setupInfo);

        // then
        verifyFailureCallbackWithTracing(result, failureCallbackMock, setupInfo.getLeft(), setupInfo.getRight());
    }

    @Test
    public void failureCallbackWithTracing_separate_args_works_as_expected() {
        // given
        Pair<Deque<Span>, Map<String, String>> setupInfo = generateTracingInfo();

        // when
        FailureCallback result = failureCallbackWithTracing(
            failureCallbackMock, setupInfo.getLeft(), setupInfo.getRight()
        );

        // then
        verifyFailureCallbackWithTracing(result, failureCallbackMock, setupInfo.getLeft(), setupInfo.getRight());
    }

    private void verifyListenableFutureCallbackWithTracing(ListenableFutureCallback result,
                                                           ListenableFutureCallback expectedCoreInstance,
                                                           Deque<Span> expectedSpanStack,
                                                           Map<String, String> expectedMdcInfo) {
        assertThat(result).isInstanceOf(ListenableFutureCallbackWithTracing.class);
        assertThat(Whitebox.getInternalState(result, "origListenableFutureCallback")).isSameAs(expectedCoreInstance);
        assertThat(Whitebox.getInternalState(result, "spanStackForExecution")).isEqualTo(expectedSpanStack);
        assertThat(Whitebox.getInternalState(result, "mdcContextMapForExecution")).isEqualTo(expectedMdcInfo);
    }

    @Test
    public void listenableFutureCallbackWithTracing_using_current_thread_info_works_as_expected() {
        // given
        Pair<Deque<Span>, Map<String, String>> setupInfo = setupCurrentThreadWithTracingInfo();

        // when
        ListenableFutureCallback result = listenableFutureCallbackWithTracing(listenableFutureCallbackMock);

        // then
        verifyListenableFutureCallbackWithTracing(result, listenableFutureCallbackMock, setupInfo.getLeft(), setupInfo.getRight());
    }

    @Test
    public void listenableFutureCallbackWithTracing_pair_works_as_expected() {
        // given
        Pair<Deque<Span>, Map<String, String>> setupInfo = generateTracingInfo();

        // when
        ListenableFutureCallback result = listenableFutureCallbackWithTracing(listenableFutureCallbackMock, setupInfo);

        // then
        verifyListenableFutureCallbackWithTracing(result, listenableFutureCallbackMock, setupInfo.getLeft(), setupInfo.getRight());
    }

    @Test
    public void listenableFutureCallbackWithTracing_separate_args_works_as_expected() {
        // given
        Pair<Deque<Span>, Map<String, String>> setupInfo = generateTracingInfo();

        // when
        ListenableFutureCallback result = listenableFutureCallbackWithTracing(
            listenableFutureCallbackMock, setupInfo.getLeft(), setupInfo.getRight()
        );

        // then
        verifyListenableFutureCallbackWithTracing(result, listenableFutureCallbackMock, setupInfo.getLeft(), setupInfo.getRight());
    }

}