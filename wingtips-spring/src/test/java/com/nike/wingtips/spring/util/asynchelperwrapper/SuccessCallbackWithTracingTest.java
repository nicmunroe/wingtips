package com.nike.wingtips.spring.util.asynchelperwrapper;

import com.nike.internal.util.Pair;
import com.nike.wingtips.Span;
import com.nike.wingtips.Tracer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.util.concurrent.SuccessCallback;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.nike.wingtips.spring.util.asynchelperwrapper.SuccessCallbackWithTracing.withTracing;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

/**
 * Tests the functionality of {@link SuccessCallbackWithTracing}.
 *
 * @author Nic Munroe
 */
public class SuccessCallbackWithTracingTest {

    private SuccessCallback successCallbackMock;
    List<Deque<Span>> currentSpanStackWhenSuccessCallbackWasCalled;
    List<Map<String, String>> currentMdcInfoWhenSuccessCallbackWasCalled;
    boolean throwExceptionDuringCall;
    Object inObj;

    @BeforeEach
    public void beforeMethod() {
        successCallbackMock = mock(SuccessCallback.class);

        inObj = new Object();
        throwExceptionDuringCall = false;
        currentSpanStackWhenSuccessCallbackWasCalled = new ArrayList<>();
        currentMdcInfoWhenSuccessCallbackWasCalled = new ArrayList<>();
        doAnswer(invocation -> {
            currentSpanStackWhenSuccessCallbackWasCalled.add(Tracer.getInstance().getCurrentSpanStackCopy());
            currentMdcInfoWhenSuccessCallbackWasCalled.add(MDC.getCopyOfContextMap());
            if (throwExceptionDuringCall)
                throw new RuntimeException("kaboom");
            return null;
        }).when(successCallbackMock).onSuccess(inObj);

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
        SuccessCallbackWithTracing instance = (useStaticFactory)
                                       ? withTracing(successCallbackMock)
                                       : new SuccessCallbackWithTracing(successCallbackMock);

        // then
        assertThat(instance.origSuccessCallback).isSameAs(successCallbackMock);
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
        SuccessCallbackWithTracing instance = (useStaticFactory)
                                       ? withTracing(successCallbackMock, Pair.of(spanStackMock, mdcInfoMock))
                                       : new SuccessCallbackWithTracing(successCallbackMock, Pair.of(spanStackMock, mdcInfoMock)
                                       );

        // then
        assertThat(instance.origSuccessCallback).isSameAs(successCallbackMock);
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
        SuccessCallbackWithTracing instance = (useStaticFactory)
                                       ? withTracing(successCallbackMock, (Pair)null)
                                       : new SuccessCallbackWithTracing(successCallbackMock, (Pair)null);

        // then
        assertThat(instance.origSuccessCallback).isSameAs(successCallbackMock);
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
        SuccessCallbackWithTracing instance = (useStaticFactory)
                                       ? withTracing(successCallbackMock, spanStackMock, mdcInfoMock)
                                       : new SuccessCallbackWithTracing(successCallbackMock, spanStackMock, mdcInfoMock);

        // then
        assertThat(instance.origSuccessCallback).isSameAs(successCallbackMock);
        assertThat(instance.spanStackForExecution).isEqualTo(spanStackMock);
        assertThat(instance.mdcContextMapForExecution).isEqualTo(mdcInfoMock);
    }

    @Test
    public void constructors_throw_exception_if_passed_null_operator() {
        // given
        final Deque<Span> spanStackMock = mock(Deque.class);
        final Map<String, String> mdcInfoMock = mock(Map.class);

        // expect
        assertThat(catchThrowable(() -> new SuccessCallbackWithTracing(null)))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(catchThrowable(() -> withTracing(null)))
            .isInstanceOf(IllegalArgumentException.class);

        // and expect
        assertThat(catchThrowable(() -> new SuccessCallbackWithTracing(null, Pair.of(spanStackMock, mdcInfoMock))))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(catchThrowable(() -> withTracing(null, Pair.of(spanStackMock, mdcInfoMock))))
            .isInstanceOf(IllegalArgumentException.class);

        // and expect
        assertThat(catchThrowable(() -> new SuccessCallbackWithTracing(null, spanStackMock, mdcInfoMock)))
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
        SuccessCallbackWithTracing instance = new SuccessCallbackWithTracing(
            successCallbackMock, spanStack, mdcInfo
        );
        resetTracing();
        assertThat(Tracer.getInstance().getCurrentSpanStackCopy()).isNull();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();

        // when
        Throwable ex = catchThrowable(() -> instance.onSuccess(inObj));

        // then
        verify(successCallbackMock).onSuccess(inObj);
        if (throwException) {
            assertThat(ex).isNotNull();
        }
        else {
            assertThat(ex).isNull();
        }

        assertThat(currentSpanStackWhenSuccessCallbackWasCalled.get(0)).isEqualTo(spanStack);
        assertThat(currentMdcInfoWhenSuccessCallbackWasCalled.get(0)).isEqualTo(mdcInfo);

        assertThat(Tracer.getInstance().getCurrentSpanStackCopy()).isNull();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }
}