package com.nike.wingtips.util.asynchelperwrapper;

import com.nike.internal.util.Pair;
import com.nike.wingtips.Span;
import com.nike.wingtips.Tracer;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;

import static com.nike.wingtips.util.asynchelperwrapper.ConsumerWithTracing.withTracing;
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
 * Tests the functionality of {@link ConsumerWithTracing}.
 *
 * @author Nic Munroe
 */
public class ConsumerWithTracingTest {

    private Consumer consumerMock;
    List<Deque<Span>> currentSpanStackWhenConsumerWasCalled;
    List<Map<String, String>> currentMdcInfoWhenConsumerWasCalled;
    boolean throwExceptionDuringCall;
    Object inObj;

    @BeforeEach
    public void beforeMethod() {
        consumerMock = mock(Consumer.class);

        inObj = new Object();
        throwExceptionDuringCall = false;
        currentSpanStackWhenConsumerWasCalled = new ArrayList<>();
        currentMdcInfoWhenConsumerWasCalled = new ArrayList<>();
        doAnswer(invocation -> {
            currentSpanStackWhenConsumerWasCalled.add(Tracer.getInstance().getCurrentSpanStackCopy());
            currentMdcInfoWhenConsumerWasCalled.add(MDC.getCopyOfContextMap());
            if (throwExceptionDuringCall)
                throw new RuntimeException("kaboom");
            return null;
        }).when(consumerMock).accept(inObj);

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
        ConsumerWithTracing instance = (useStaticFactory)
                                         ? withTracing(consumerMock)
                                         : new ConsumerWithTracing(consumerMock);

        // then
        assertThat(instance.origConsumer).isSameAs(consumerMock);
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
        ConsumerWithTracing instance = (useStaticFactory)
                                         ? withTracing(consumerMock, Pair.of(spanStackMock, mdcInfoMock))
                                         : new ConsumerWithTracing(consumerMock, Pair.of(spanStackMock, mdcInfoMock)
                                         );

        // then
        assertThat(instance.origConsumer).isSameAs(consumerMock);
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
        ConsumerWithTracing instance = (useStaticFactory)
                                         ? withTracing(consumerMock, (Pair)null)
                                         : new ConsumerWithTracing(consumerMock, (Pair)null);

        // then
        assertThat(instance.origConsumer).isSameAs(consumerMock);
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
        ConsumerWithTracing instance = (useStaticFactory)
                                         ? withTracing(consumerMock, spanStackMock, mdcInfoMock)
                                         : new ConsumerWithTracing(consumerMock, spanStackMock, mdcInfoMock);

        // then
        assertThat(instance.origConsumer).isSameAs(consumerMock);
        assertThat(instance.spanStackForExecution).isEqualTo(spanStackMock);
        assertThat(instance.mdcContextMapForExecution).isEqualTo(mdcInfoMock);
    }

    @Test
    public void constructors_throw_exception_if_passed_null_operator() {
        // given
        final Deque<Span> spanStackMock = mock(Deque.class);
        final Map<String, String> mdcInfoMock = mock(Map.class);

        // expect
        assertThat(catchThrowable(() -> new ConsumerWithTracing(null)))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(catchThrowable(() -> withTracing(null)))
            .isInstanceOf(IllegalArgumentException.class);

        // and expect
        assertThat(catchThrowable(() -> new ConsumerWithTracing(null, Pair.of(spanStackMock, mdcInfoMock))))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(catchThrowable(() -> withTracing(null, Pair.of(spanStackMock, mdcInfoMock))))
            .isInstanceOf(IllegalArgumentException.class);

        // and expect
        assertThat(catchThrowable(() -> new ConsumerWithTracing(null, spanStackMock, mdcInfoMock)))
            .isInstanceOf(IllegalArgumentException.class);

        assertThat(catchThrowable(() -> withTracing(null, spanStackMock, mdcInfoMock)))
            .isInstanceOf(IllegalArgumentException.class);
    }

    public static Stream<Arguments> apply_handles_tracing_and_mdc_info_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("apply_handles_tracing_and_mdc_info_as_expected_DataProvider")
    public void apply_handles_tracing_and_mdc_info_as_expected(boolean throwException) {
        // given
        throwExceptionDuringCall = throwException;
        Tracer.getInstance().startRequestWithRootSpan("foo");
        Deque<Span> spanStack = Tracer.getInstance().getCurrentSpanStackCopy();
        Map<String, String> mdcInfo = MDC.getCopyOfContextMap();
        ConsumerWithTracing instance = new ConsumerWithTracing(
            consumerMock, spanStack, mdcInfo
        );
        resetTracing();
        assertThat(Tracer.getInstance().getCurrentSpanStackCopy()).isNull();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();

        // when
        Throwable ex = catchThrowable(() -> instance.accept(inObj));

        // then
        verify(consumerMock).accept(inObj);
        if (throwException) {
            assertThat(ex).isNotNull();
        }
        else {
            assertThat(ex).isNull();
        }

        assertThat(currentSpanStackWhenConsumerWasCalled.get(0)).isEqualTo(spanStack);
        assertThat(currentMdcInfoWhenConsumerWasCalled.get(0)).isEqualTo(mdcInfo);

        assertThat(Tracer.getInstance().getCurrentSpanStackCopy()).isNull();
        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }

}