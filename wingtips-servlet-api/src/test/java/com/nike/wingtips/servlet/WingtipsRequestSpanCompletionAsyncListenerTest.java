package com.nike.wingtips.servlet;

import com.nike.wingtips.Span;
import com.nike.wingtips.Tracer;
import com.nike.wingtips.lifecyclelistener.SpanLifecycleListener;
import com.nike.wingtips.tags.HttpTagAndSpanNamingAdapter;
import com.nike.wingtips.tags.HttpTagAndSpanNamingStrategy;
import com.nike.wingtips.util.TracingState;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.AsyncEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Verifies the functionality of {@link WingtipsRequestSpanCompletionAsyncListener}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class WingtipsRequestSpanCompletionAsyncListenerTest {

    private WingtipsRequestSpanCompletionAsyncListener implSpy;
    private TracingState tracingState;
    private Span tracingStateSpan;
    private AsyncEvent asyncEventMock;
    private HttpServletResponse responseMock;
    private HttpTagAndSpanNamingStrategy<HttpServletRequest,HttpServletResponse> tagStrategyMock;
    private HttpTagAndSpanNamingAdapter<HttpServletRequest,HttpServletResponse> tagAdapterMock;
    private Throwable exception;

    @Before
    public void beforeMethod() {
        Tracer.getInstance().startRequestWithRootSpan("someRequestSpan");
        tracingState = TracingState.getCurrentThreadTracingState();
        assertThat(tracingState.spanStack).hasSize(1);
        tracingStateSpan = tracingState.spanStack.peek();
        tagStrategyMock = mock(HttpTagAndSpanNamingStrategy.class);
        tagAdapterMock = mock(HttpTagAndSpanNamingAdapter.class);
        implSpy = spy(new WingtipsRequestSpanCompletionAsyncListener(tracingState, tagStrategyMock, tagAdapterMock));
        asyncEventMock = mock(AsyncEvent.class);
        responseMock = mock(HttpServletResponse.class);
        exception = new Exception("Kaboom");
        
        doReturn(responseMock).when(asyncEventMock).getSuppliedResponse();
        doReturn(exception).when(asyncEventMock).getThrowable();
        
        resetTracing();
    }

    @After
    public void afterMethod() {
        resetTracing();
    }

    private void resetTracing() {
        MDC.clear();
        Tracer.getInstance().unregisterFromThread();
        List<SpanLifecycleListener> listeners = new ArrayList<>(Tracer.getInstance().getSpanLifecycleListeners());
        for (SpanLifecycleListener listener : listeners) {
            Tracer.getInstance().removeSpanLifecycleListener(listener);
        }
    }

    @Test
    public void constructor_sets_fields_as_expected() {
        // given
        TracingState tracingStateMock = mock(TracingState.class);

        // when
        WingtipsRequestSpanCompletionAsyncListener impl =
            new WingtipsRequestSpanCompletionAsyncListener(tracingStateMock, tagStrategyMock, tagAdapterMock);

        // then
        assertThat(impl.originalRequestTracingState).isSameAs(tracingStateMock);
        assertThat(impl.tagStrategy).isSameAs(tagStrategyMock);
        assertThat(impl.alreadyCompleted.get()).isFalse();
    }

    @Test
    public void onComplete_calls_tags_and_completeRequestSpan_and_does_nothing_else() throws IOException {
        // when
        implSpy.onComplete(asyncEventMock);

        // then
        verify(implSpy).onComplete(asyncEventMock);
        verify(implSpy).completeRequestSpan(asyncEventMock);
        verify(tagStrategyMock).handleResponseTaggingAndFinalSpanName(
            any(Span.class), any(HttpServletRequest.class), any(HttpServletResponse.class), any(Throwable.class),
            eq(tagAdapterMock)
        );
        verifyNoMoreInteractions(implSpy);
    }

    @Test
    public void onTimeout_calls_tags_and_completeRequestSpan_and_does_nothing_else() throws IOException {
        // when
        implSpy.onTimeout(asyncEventMock);

        // then
        verify(implSpy).onTimeout(asyncEventMock);
        verify(implSpy).completeRequestSpan(asyncEventMock);
        verify(tagStrategyMock).handleResponseTaggingAndFinalSpanName(
            any(Span.class), any(HttpServletRequest.class), any(HttpServletResponse.class), any(Throwable.class),
            eq(tagAdapterMock)
        );
        verifyNoMoreInteractions(implSpy);
    }

    @Test
    public void onError_tags_error_and_calls_completeRequestSpan_and_does_nothing_else() throws IOException {
        // when
        implSpy.onError(asyncEventMock);

        // then
        verify(implSpy).onError(asyncEventMock);
        verify(implSpy).completeRequestSpan(asyncEventMock);
        verify(tagStrategyMock).handleResponseTaggingAndFinalSpanName(
            any(Span.class), any(HttpServletRequest.class), any(HttpServletResponse.class), any(Throwable.class),
            eq(tagAdapterMock)
        );
        verifyNoMoreInteractions(implSpy);
    }

    @Test
    public void onStartAsync_propagates_the_listener_to_the_new_AsyncContext() throws IOException {
        // given
        AsyncContext asyncContextMock = mock(AsyncContext.class);
        doReturn(asyncContextMock).when(asyncEventMock).getAsyncContext();

        // when
        implSpy.onStartAsync(asyncEventMock);

        // then
        verify(asyncContextMock).addListener(implSpy);
    }

    @Test
    public void onStartAsync_does_nothing_if_asyncEvent_has_null_AsyncContext() throws IOException {
        // given
        // This should never happen in reality, but we protect against null pointer exceptions anyway.
        doReturn(null).when(asyncEventMock).getAsyncContext();

        // when
        implSpy.onStartAsync(asyncEventMock);

        // then
        verify(implSpy).onStartAsync(asyncEventMock);
        verifyNoMoreInteractions(implSpy);
    }

    @Test
    public void completeRequestSpan_completes_request_span_as_expected() {
        // given
        Tracer.getInstance().startRequestWithRootSpan("someOtherUnrelatedSpan");
        TracingState unrelatedThreadTracingState = TracingState.getCurrentThreadTracingState();
        SpanRecorder spanRecorder = new SpanRecorder();
        Tracer.getInstance().addSpanLifecycleListener(spanRecorder);
        assertThat(tracingStateSpan.isCompleted()).isFalse();
        assertThat(implSpy.alreadyCompleted.get()).isFalse();

        // when
        implSpy.completeRequestSpan(asyncEventMock);

        // then
        assertThat(spanRecorder.completedSpans).hasSize(1);
        Span completedSpan = spanRecorder.completedSpans.get(0);
        assertThat(completedSpan).isSameAs(tracingStateSpan);
        assertThat(tracingStateSpan.isCompleted()).isTrue();
        assertThat(implSpy.alreadyCompleted.get()).isTrue();
        verify(tagStrategyMock).handleResponseTaggingAndFinalSpanName(any(Span.class), any(HttpServletRequest.class), any(HttpServletResponse.class), any(Throwable.class), any(HttpTagAndSpanNamingAdapter.class));

        // Tracing state got reset back to original from when the method was called.
        assertThat(TracingState.getCurrentThreadTracingState()).isEqualTo(unrelatedThreadTracingState);
    }

    @Test
    public void completeRequestSpan_marks_listener_as_completed_even_if_unexpected_exception_occurs() {
        // given
        Tracer.getInstance().startRequestWithRootSpan("someOtherUnrelatedSpan");
        TracingState unrelatedThreadTracingState = TracingState.getCurrentThreadTracingState();

        final RuntimeException expectedExplosion = new RuntimeException("kaboom");

        SpanRecorder explodingSpanRecorder = new SpanRecorder() {
            @Override
            public void spanCompleted(Span span) {
                throw expectedExplosion;
            }
        };
        Tracer.getInstance().addSpanLifecycleListener(explodingSpanRecorder);
        assertThat(implSpy.alreadyCompleted.get()).isFalse();

        // when
        Throwable actualEx = catchThrowable(() -> implSpy.completeRequestSpan(asyncEventMock));

        // then
        assertThat(actualEx).isSameAs(expectedExplosion);
        assertThat(explodingSpanRecorder.completedSpans).hasSize(0);
        assertThat(implSpy.alreadyCompleted.get()).isTrue();

        // Tracing state got reset back to original from when the method was called.
        assertThat(TracingState.getCurrentThreadTracingState()).isEqualTo(unrelatedThreadTracingState);
    }

    @Test
    public void completeRequestSpan_does_nothing_if_listener_is_already_marked_completed() {
        // given
        implSpy.alreadyCompleted.set(true);

        assertThat(tracingStateSpan.isCompleted()).isFalse();

        // when
        implSpy.completeRequestSpan(asyncEventMock);

        // then
        assertThat(tracingStateSpan.isCompleted()).isFalse();
        assertThat(implSpy.alreadyCompleted.get()).isTrue();
    }
    
    @Test
    public void completeRequestSpan_completes_span_when_tagstrategy_fails() {
        // given
        doThrow(new RuntimeException("boom")).when(tagStrategyMock).handleResponseTaggingAndFinalSpanName(any(Span.class), any(HttpServletRequest.class), any(HttpServletResponse.class), any(Throwable.class), any(HttpTagAndSpanNamingAdapter.class));

        // then
        completeRequestSpan_completes_request_span_as_expected();
    }

    public static class SpanRecorder implements SpanLifecycleListener {

        public final List<Span> completedSpans = new ArrayList<>();

        @Override
        public void spanStarted(Span span) { }

        @Override
        public void spanSampled(Span span) { }

        @Override
        public void spanCompleted(Span span) {
            completedSpans.add(span);
        }
    }

}