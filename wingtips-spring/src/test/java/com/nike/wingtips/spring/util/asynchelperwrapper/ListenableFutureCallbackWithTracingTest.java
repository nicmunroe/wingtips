package com.nike.wingtips.spring.util.asynchelperwrapper;

import com.nike.internal.util.Pair;
import com.nike.wingtips.Span;
import com.nike.wingtips.Tracer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.MDC;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import static com.nike.wingtips.spring.util.asynchelperwrapper.ListenableFutureCallbackWithTracing.withTracing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests the functionality of {@link ListenableFutureCallbackWithTracing}.
 *
 * @author Nic Munroe
 */
public class ListenableFutureCallbackWithTracingTest {

    private ListenableFutureCallback listenableFutureCallbackMock;
    List<Deque<Span>> currentSpanStackWhenListenableFutureCallbackWasCalled;
    List<Map<String, String>> currentMdcInfoWhenListenableFutureCallbackWasCalled;
    boolean throwExceptionDuringCall;
    Object successInObj;
    Throwable failureInObj;

    @BeforeEach
    public void beforeMethod() {
        listenableFutureCallbackMock = mock(ListenableFutureCallback.class);

        successInObj = new Object();
        failureInObj = new Exception("kaboom");
        throwExceptionDuringCall = false;
        currentSpanStackWhenListenableFutureCallbackWasCalled = new ArrayList<>();
        currentMdcInfoWhenListenableFutureCallbackWasCalled = new ArrayList<>();
        doAnswer(invocation -> {
            currentSpanStackWhenListenableFutureCallbackWasCalled.add(Tracer.getInstance().getCurrentSpanStackCopy());
            currentMdcInfoWhenListenableFutureCallbackWasCalled.add(MDC.getCopyOfContextMap());
            if (throwExceptionDuringCall)
                throw new RuntimeException("kaboom");
            return null;
        }).when(listenableFutureCallbackMock).onSuccess(successInObj);
        doAnswer(invocation -> {
            currentSpanStackWhenListenableFutureCallbackWasCalled.add(Tracer.getInstance().getCurrentSpanStackCopy());
            currentMdcInfoWhenListenableFutureCallbackWasCalled.add(MDC.getCopyOfContextMap());
            if (throwExceptionDuringCall)
                throw new RuntimeException("kaboom");
            return null;
        }).when(listenableFutureCallbackMock).onFailure(failureInObj);

        resetTracing();
    }

    @AfterEach
    public void afterMethod() {
        resetTracing();
    }

    private void resetTracing() {
        MDC.clear();
        Tracer.getInstance().unregisterFromThread();
    }

    public static Stream<Arguments> current_thread_info_constructor_sets_fields_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("current_thread_info_constructor_sets_fields_as_expected_DataProvider")
    public void current_thread_info_constructor_sets_fields_as_expected(boolean useStaticFactory) {
        // given
        Tracer.getInstance().startRequestWithRootSpan("request-" + UUID.randomUUID().toString());
        Deque<Span> spanStackMock = Tracer.getInstance().getCurrentSpanStackCopy();
        Map<String, String> mdcInfoMock = MDC.getCopyOfContextMap();

        // when
        ListenableFutureCallbackWithTracing instance = (useStaticFactory)
                                       ? withTracing(listenableFutureCallbackMock)
                                       : new ListenableFutureCallbackWithTracing(listenableFutureCallbackMock);

        // then
        assertThat(instance.origListenableFutureCallback).isSameAs(listenableFutureCallbackMock);
        assertThat(instance.spanStackForExecution).isEqualTo(spanStackMock);
        assertThat(instance.mdcContextMapForExecution).isEqualTo(mdcInfoMock);
    }

    public static Stream<Arguments> pair_constructor_sets_fields_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of(true, true, true),
            Arguments.of(true, false, true),
            Arguments.of(false, true, true),
            Arguments.of(false, false, true),
            Arguments.of(true, true, false),
            Arguments.of(true, false, false),
            Arguments.of(false, true, false),
            Arguments.of(false, false, false)
        );
    }

    @ParameterizedTest
    @MethodSource("pair_constructor_sets_fields_as_expected_DataProvider")
    public void pair_constructor_sets_fields_as_expected(
        boolean nullSpanStack, boolean nullMdcInfo, boolean useStaticFactory
    ) {
        // given
        Deque<Span> spanStackMock = (nullSpanStack) ? null : mock(Deque.class);
        Map<String, String> mdcInfoMock = (nullMdcInfo) ? null : mock(Map.class);

        // when
        ListenableFutureCallbackWithTracing instance = (useStaticFactory)
                                       ? withTracing(listenableFutureCallbackMock, Pair.of(spanStackMock, mdcInfoMock))
                                       : new ListenableFutureCallbackWithTracing(listenableFutureCallbackMock, Pair.of(spanStackMock, mdcInfoMock)
                                       );

        // then
        assertThat(instance.origListenableFutureCallback).isSameAs(listenableFutureCallbackMock);
        assertThat(instance.spanStackForExecution).isEqualTo(spanStackMock);
        assertThat(instance.mdcContextMapForExecution).isEqualTo(mdcInfoMock);
    }

    public static Stream<Arguments> pair_constructor_sets_fields_as_expected_when_pair_is_null_DataProvider() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("pair_constructor_sets_fields_as_expected_when_pair_is_null_DataProvider")
    public void pair_constructor_sets_fields_as_expected_when_pair_is_null(boolean useStaticFactory) {
        // when
        ListenableFutureCallbackWithTracing instance = (useStaticFactory)
                                       ? withTracing(listenableFutureCallbackMock, (Pair)null)
                                       : new ListenableFutureCallbackWithTracing(listenableFutureCallbackMock, (Pair)null);

        // then
        assertThat(instance.origListenableFutureCallback).isSameAs(listenableFutureCallbackMock);
        assertThat(instance.spanStackForExecution).isNull();
        assertThat(instance.mdcContextMapForExecution).isNull();
    }

    public static Stream<Arguments> kitchen_sink_constructor_sets_fields_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("kitchen_sink_constructor_sets_fields_as_expected_DataProvider")
    public void kitchen_sink_constructor_sets_fields_as_expected(boolean useStaticFactory) {
        // given
        Deque<Span> spanStackMock = mock(Deque.class);
        Map<String, String> mdcInfoMock = mock(Map.class);

        // when
        ListenableFutureCallbackWithTracing instance = (useStaticFactory)
                                       ? withTracing(listenableFutureCallbackMock, spanStackMock, mdcInfoMock)
                                       : new ListenableFutureCallbackWithTracing(listenableFutureCallbackMock, spanStackMock, mdcInfoMock);

        // then
        assertThat(instance.origListenableFutureCallback).isSameAs(listenableFutureCallbackMock);
        assertThat(instance.spanStackForExecution).isEqualTo(spanStackMock);
        assertThat(instance.mdcContextMapForExecution).isEqualTo(mdcInfoMock);
    }

    @Test
    public void constructors_throw_exception_if_passed_null_operator() {
        // given
        final Deque<Span> spanStackMock = mock(Deque.class);
        final Map<String, String> mdcInfoMock = mock(Map.class);

        // expect
        assertThat(catchThrowable(() -> new ListenableFutureCallbackWithTracing(null)))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(catchThrowable(() -> withTracing(null)))
            .isInstanceOf(IllegalArgumentException.class);

        // and expect
        assertThat(catchThrowable(() -> new ListenableFutureCallbackWithTracing(null, Pair.of(spanStackMock, mdcInfoMock))))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(catchThrowable(() -> withTracing(null, Pair.of(spanStackMock, mdcInfoMock))))
            .isInstanceOf(IllegalArgumentException.class);

        // and expect
        assertThat(catchThrowable(() -> new ListenableFutureCallbackWithTracing(null, spanStackMock, mdcInfoMock)))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(catchThrowable(() -> withTracing(null, spanStackMock, mdcInfoMock)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    public static Stream<Arguments> onSuccess_handles_tracing_and_mdc_info_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("onSuccess_handles_tracing_and_mdc_info_as_expected_DataProvider")
    public void onSuccess_handles_tracing_and_mdc_info_as_expected(boolean throwException) {
        // given
        throwExceptionDuringCall = throwException;
        Tracer.getInstance().startRequestWithRootSpan("foo");
        Deque<Span> spanStack = Tracer.getInstance().getCurrentSpanStackCopy();
        Map<String, String> mdcInfo = MDC.getCopyOfContextMap();
        ListenableFutureCallbackWithTracing instance = new ListenableFutureCallbackWithTracing(
            listenableFutureCallbackMock, spanStack, mdcInfo
        );
        resetTracing();
        assertThat(Tracer.getInstance().getCurrentSpanStackCopy()).isNull();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();

        // when
        Throwable ex = catchThrowable(() -> instance.onSuccess(successInObj));

        // then
        verify(listenableFutureCallbackMock).onSuccess(successInObj);
        if (throwException) {
            assertThat(ex).isNotNull();
        }
        else {
            assertThat(ex).isNull();
        }

        assertThat(currentSpanStackWhenListenableFutureCallbackWasCalled.get(0)).isEqualTo(spanStack);
        assertThat(currentMdcInfoWhenListenableFutureCallbackWasCalled.get(0)).isEqualTo(mdcInfo);

        assertThat(Tracer.getInstance().getCurrentSpanStackCopy()).isNull();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

    public static Stream<Arguments> onFailure_handles_tracing_and_mdc_info_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("onFailure_handles_tracing_and_mdc_info_as_expected_DataProvider")
    public void onFailure_handles_tracing_and_mdc_info_as_expected(boolean throwException) {
        // given
        throwExceptionDuringCall = throwException;
        Tracer.getInstance().startRequestWithRootSpan("foo");
        Deque<Span> spanStack = Tracer.getInstance().getCurrentSpanStackCopy();
        Map<String, String> mdcInfo = MDC.getCopyOfContextMap();
        ListenableFutureCallbackWithTracing instance = new ListenableFutureCallbackWithTracing(
            listenableFutureCallbackMock, spanStack, mdcInfo
        );
        resetTracing();
        assertThat(Tracer.getInstance().getCurrentSpanStackCopy()).isNull();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();

        // when
        Throwable ex = catchThrowable(() -> instance.onFailure(failureInObj));

        // then
        verify(listenableFutureCallbackMock).onFailure(failureInObj);
        if (throwException) {
            assertThat(ex).isNotNull();
        }
        else {
            assertThat(ex).isNull();
        }

        assertThat(currentSpanStackWhenListenableFutureCallbackWasCalled.get(0)).isEqualTo(spanStack);
        assertThat(currentMdcInfoWhenListenableFutureCallbackWasCalled.get(0)).isEqualTo(mdcInfo);

        assertThat(Tracer.getInstance().getCurrentSpanStackCopy()).isNull();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }
}