package com.nike.wingtips.tags;

import com.nike.wingtips.Span;
import com.nike.wingtips.Span.SpanPurpose;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

/**
 * Tests the functionality of the default methods found in {@link HttpTagAndSpanNamingStrategy}.
 *
 * @author Nic Munroe
 */
public class HttpTagAndSpanNamingStrategyTest {

    private HttpTagAndSpanNamingStrategy<Object, Object> implSpy;
    private Span spanMock;
    private Object requestObjectMock;
    private Object responseObjectMock;
    private Throwable errorMock;
    private HttpTagAndSpanNamingAdapter<Object, Object> adapterMock;

    @BeforeEach
    public void beforeMethod() {
        implSpy = spy(new BasicImpl());

        spanMock = mock(Span.class);
        requestObjectMock = mock(Object.class);
        responseObjectMock = mock(Object.class);
        errorMock = mock(Throwable.class);
        adapterMock = mock(HttpTagAndSpanNamingAdapter.class);
    }

    @Test
    public void getInitialSpanName_defers_to_doGetInitialSpanName() {
        // given
        String delegateMethodResult = UUID.randomUUID().toString();
        doReturn(delegateMethodResult)
            .when(implSpy).doGetInitialSpanName(anyObject(), any(HttpTagAndSpanNamingAdapter.class));

        // when
        String result = implSpy.getInitialSpanName(requestObjectMock, adapterMock);

        // then
        assertThat(result).isEqualTo(delegateMethodResult);
        verify(implSpy).doGetInitialSpanName(requestObjectMock, adapterMock);
        verifyNoMoreInteractions(implSpy);
        verifyNoInteractions(requestObjectMock, adapterMock);
    }

    private static final Span staticSpanMock = mock(Span.class);
    private static final Object staticRequestObjMock = mock(Object.class);
    private static final HttpTagAndSpanNamingAdapter<Object, Object> staticAdapterMock =
        mock(HttpTagAndSpanNamingAdapter.class);

    private enum NullArgCornerCaseScenario {
        SPAN_IS_NULL(null, staticRequestObjMock, staticAdapterMock),
        REQUEST_OBJ_IS_NULL(staticSpanMock, null, staticAdapterMock),
        ADAPTER_IS_NULL(staticSpanMock, staticRequestObjMock, null);

        public final Span spanMock;
        public final Object requestObjMock;
        public final HttpTagAndSpanNamingAdapter<Object, Object> adapterMock;

        NullArgCornerCaseScenario(
            Span spanMock, Object requestObjMock, HttpTagAndSpanNamingAdapter<Object, Object> adapterMock
        ) {
            this.spanMock = spanMock;
            this.requestObjMock = requestObjMock;
            this.adapterMock = adapterMock;
        }
    }

    public static Stream<Arguments> getInitialSpanName_returns_null_in_null_arg_corner_cases_DataProvider() {
        return Stream.of(
            Arguments.of(NullArgCornerCaseScenario.REQUEST_OBJ_IS_NULL),
            Arguments.of(NullArgCornerCaseScenario.ADAPTER_IS_NULL)
        );
    }

    @ParameterizedTest
    @MethodSource("getInitialSpanName_returns_null_in_null_arg_corner_cases_DataProvider")
    @SuppressWarnings("ConstantConditions")
    public void getInitialSpanName_returns_null_in_null_arg_corner_cases(NullArgCornerCaseScenario scenario) {
        // given
        Object requestObjectMock = scenario.requestObjMock;
        HttpTagAndSpanNamingAdapter<Object, Object> adapterMock = scenario.adapterMock;

        // when
        String result = implSpy.getInitialSpanName(requestObjectMock, adapterMock);

        // then
        assertThat(result).isNull();
        verifyNoInteractions(implSpy);
        if (requestObjectMock != null) {
            verifyNoInteractions(requestObjectMock);
        }
        if (adapterMock != null) {
            verifyNoInteractions(adapterMock);
        }
    }

    @Test
    public void getInitialSpanName_returns_null_if_delegate_method_throws_exception() {
        // given
        doThrow(new RuntimeException("boom"))
            .when(implSpy).doGetInitialSpanName(anyObject(), any(HttpTagAndSpanNamingAdapter.class));

        // when
        String result = implSpy.getInitialSpanName(requestObjectMock, adapterMock);

        // then
        assertThat(result).isNull();
        verify(implSpy).doGetInitialSpanName(requestObjectMock, adapterMock);
        verifyNoMoreInteractions(implSpy);
        verifyNoInteractions(requestObjectMock, adapterMock);
    }

    @Test
    public void handleRequestTagging_defers_to_doHandleRequestTagging() {
        // given
        doNothing().when(implSpy).doHandleRequestTagging(
            any(Span.class), anyObject(), any(HttpTagAndSpanNamingAdapter.class)
        );

        // when
        implSpy.handleRequestTagging(spanMock, requestObjectMock, adapterMock);

        // then
        verify(implSpy).doHandleRequestTagging(spanMock, requestObjectMock, adapterMock);
        verifyNoMoreInteractions(implSpy);
        verifyNoInteractions(spanMock, requestObjectMock, adapterMock);
    }

    public static Stream<Arguments> handleRequestTagging_does_nothing_in_null_arg_corner_cases_DataProvider() {
        return Stream.of(
            Arguments.of(NullArgCornerCaseScenario.SPAN_IS_NULL),
            Arguments.of(NullArgCornerCaseScenario.REQUEST_OBJ_IS_NULL),
            Arguments.of(NullArgCornerCaseScenario.ADAPTER_IS_NULL)
        );
    }

    @ParameterizedTest
    @MethodSource("handleRequestTagging_does_nothing_in_null_arg_corner_cases_DataProvider")
    @SuppressWarnings("ConstantConditions")
    public void handleRequestTagging_does_nothing_in_null_arg_corner_cases(NullArgCornerCaseScenario scenario) {
        // given
        Span spanMock = scenario.spanMock;
        Object requestObjectMock = scenario.requestObjMock;
        HttpTagAndSpanNamingAdapter<Object, Object> adapterMock = scenario.adapterMock;

        // when
        implSpy.handleRequestTagging(spanMock, requestObjectMock, adapterMock);

        // then
        verifyNoInteractions(implSpy);
        if (spanMock != null) {
            verifyNoInteractions(spanMock);
        }
        if (requestObjectMock != null) {
            verifyNoInteractions(requestObjectMock);
        }
        if (adapterMock != null) {
            verifyNoInteractions(adapterMock);
        }
    }

    @Test
    public void handleRequestTagging_does_nothing_if_delegate_method_throws_exception() {
        // given
        doThrow(new RuntimeException("boom"))
            .when(implSpy).doHandleRequestTagging(any(Span.class), anyObject(), any(HttpTagAndSpanNamingAdapter.class));

        // when
        implSpy.handleRequestTagging(spanMock, requestObjectMock, adapterMock);

        // then
        verify(implSpy).doHandleRequestTagging(spanMock, requestObjectMock, adapterMock);
        verifyNoMoreInteractions(implSpy);
        verifyNoInteractions(spanMock, requestObjectMock, adapterMock);
    }

    @Test
    public void handleResponseTaggingAndFinalSpanName_defers_to_doHandleResponseAndErrorTagging_and_doDetermineAndSetFinalSpanName_and_doExtraWingtipsTagging() {
        // given
        doNothing().when(implSpy).doHandleResponseAndErrorTagging(
            any(Span.class), anyObject(), anyObject(), any(Throwable.class), any(HttpTagAndSpanNamingAdapter.class)
        );
        doNothing().when(implSpy).doDetermineAndSetFinalSpanName(
            any(Span.class), anyObject(), anyObject(), any(Throwable.class), any(HttpTagAndSpanNamingAdapter.class)
        );
        doNothing().when(implSpy).doExtraWingtipsTagging(
            any(Span.class), anyObject(), anyObject(), any(Throwable.class), any(HttpTagAndSpanNamingAdapter.class)
        );

        // when
        implSpy.handleResponseTaggingAndFinalSpanName(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );

        // then
        verify(implSpy).doHandleResponseAndErrorTagging(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );
        verify(implSpy).doDetermineAndSetFinalSpanName(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );
        verify(implSpy).doExtraWingtipsTagging(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );
        verifyNoMoreInteractions(implSpy);
        verifyNoInteractions(spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock);
    }

    public static Stream<Arguments> handleResponseTaggingAndFinalSpanName_does_nothing_in_null_arg_corner_cases_DataProvider() {
        return Stream.of(
            Arguments.of(NullArgCornerCaseScenario.SPAN_IS_NULL),
            Arguments.of(NullArgCornerCaseScenario.ADAPTER_IS_NULL)
        );
    }

    @ParameterizedTest
    @MethodSource("handleResponseTaggingAndFinalSpanName_does_nothing_in_null_arg_corner_cases_DataProvider")
    @SuppressWarnings("ConstantConditions")
    public void handleResponseTaggingAndFinalSpanName_does_nothing_in_null_arg_corner_cases(
        NullArgCornerCaseScenario scenario
    ) {
        // given
        Span spanMock = scenario.spanMock;
        HttpTagAndSpanNamingAdapter<Object, Object> adapterMock = scenario.adapterMock;

        // when
        implSpy.handleResponseTaggingAndFinalSpanName(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );

        // then
        verifyNoInteractions(implSpy);
        if (spanMock != null) {
            verifyNoInteractions(spanMock);
        }
        if (adapterMock != null) {
            verifyNoInteractions(adapterMock);
        }
        verifyNoInteractions(requestObjectMock, responseObjectMock, errorMock);
    }

    @Test
    public void handleResponseTaggingAndFinalSpanName_executes_other_two_delegates_when_doHandleResponseAndErrorTagging_throws_exception() {
        // given
        doThrow(new RuntimeException("boom")).when(implSpy).doHandleResponseAndErrorTagging(
            any(Span.class), anyObject(), anyObject(), any(Throwable.class), any(HttpTagAndSpanNamingAdapter.class)
        );

        doNothing().when(implSpy).doDetermineAndSetFinalSpanName(
            any(Span.class), anyObject(), anyObject(), any(Throwable.class), any(HttpTagAndSpanNamingAdapter.class)
        );
        doNothing().when(implSpy).doExtraWingtipsTagging(
            any(Span.class), anyObject(), anyObject(), any(Throwable.class), any(HttpTagAndSpanNamingAdapter.class)
        );

        // when
        implSpy.handleResponseTaggingAndFinalSpanName(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );

        // then
        verify(implSpy).doHandleResponseAndErrorTagging(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );
        verify(implSpy).doDetermineAndSetFinalSpanName(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );
        verify(implSpy).doExtraWingtipsTagging(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );
        verifyNoMoreInteractions(implSpy);
        verifyNoInteractions(spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock);
    }

    @Test
    public void handleResponseTaggingAndFinalSpanName_executes_other_two_delegates_when_doDetermineAndSetFinalSpanName_throws_exception() {
        // given
        doThrow(new RuntimeException("boom")).when(implSpy).doDetermineAndSetFinalSpanName(
            any(Span.class), anyObject(), anyObject(), any(Throwable.class), any(HttpTagAndSpanNamingAdapter.class)
        );

        doNothing().when(implSpy).doHandleResponseAndErrorTagging(
            any(Span.class), anyObject(), anyObject(), any(Throwable.class), any(HttpTagAndSpanNamingAdapter.class)
        );
        doNothing().when(implSpy).doExtraWingtipsTagging(
            any(Span.class), anyObject(), anyObject(), any(Throwable.class), any(HttpTagAndSpanNamingAdapter.class)
        );

        // when
        implSpy.handleResponseTaggingAndFinalSpanName(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );

        // then
        verify(implSpy).doHandleResponseAndErrorTagging(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );
        verify(implSpy).doDetermineAndSetFinalSpanName(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );
        verify(implSpy).doExtraWingtipsTagging(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );
        verifyNoMoreInteractions(implSpy);
        verifyNoInteractions(spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock);
    }

    @Test
    public void handleResponseTaggingAndFinalSpanName_executes_other_two_delegates_when_doExtraWingtipsTagging_throws_exception() {
        // given
        doThrow(new RuntimeException("boom")).when(implSpy).doExtraWingtipsTagging(
            any(Span.class), anyObject(), anyObject(), any(Throwable.class), any(HttpTagAndSpanNamingAdapter.class)
        );

        doNothing().when(implSpy).doHandleResponseAndErrorTagging(
            any(Span.class), anyObject(), anyObject(), any(Throwable.class), any(HttpTagAndSpanNamingAdapter.class)
        );
        doNothing().when(implSpy).doDetermineAndSetFinalSpanName(
            any(Span.class), anyObject(), anyObject(), any(Throwable.class), any(HttpTagAndSpanNamingAdapter.class)
        );

        // when
        implSpy.handleResponseTaggingAndFinalSpanName(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );

        // then
        verify(implSpy).doHandleResponseAndErrorTagging(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );
        verify(implSpy).doDetermineAndSetFinalSpanName(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );
        verify(implSpy).doExtraWingtipsTagging(
            spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock
        );
        verifyNoMoreInteractions(implSpy);
        verifyNoInteractions(spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock);
    }

    @Test
    public void doGetInitialSpanName_delegates_to_adapter_getInitialSpanName() {
        // given
        String adapterResult = UUID.randomUUID().toString();
        doReturn(adapterResult).when(adapterMock).getInitialSpanName(anyObject());

        // when
        String result = implSpy.doGetInitialSpanName(requestObjectMock, adapterMock);

        // then
        assertThat(result).isEqualTo(adapterResult);
    }

    @Test
    public void doDetermineAndSetFinalSpanName_delegates_to_adapter_getFinalSpanName_and_changes_span_name_if_result_is_not_blank() {
        // given
        String adapterSpanNameResult = UUID.randomUUID().toString();
        doReturn(adapterSpanNameResult).when(adapterMock).getFinalSpanName(anyObject(), anyObject());

        Span span = Span.newBuilder("originalSpanName", SpanPurpose.SERVER).build();

        assertThat(span.getSpanName()).isNotEqualTo(adapterSpanNameResult);

        // when
        implSpy.doDetermineAndSetFinalSpanName(span, requestObjectMock, responseObjectMock, errorMock, adapterMock);

        // then
        assertThat(span.getSpanName()).isEqualTo(adapterSpanNameResult);
        verify(adapterMock).getFinalSpanName(requestObjectMock, responseObjectMock);
    }

    public static Stream<Arguments> doDetermineAndSetFinalSpanName_delegates_to_adapter_getFinalSpanName_and_does_NOT_change_span_name_if_result_IS_blank_DataProvider() {
        return Stream.of(
            Arguments.of((Object) null),
            Arguments.of(""),
            Arguments.of("[whitespace]")
        );
    }

    @ParameterizedTest
    @MethodSource("doDetermineAndSetFinalSpanName_delegates_to_adapter_getFinalSpanName_and_does_NOT_change_span_name_if_result_IS_blank_DataProvider")
    public void doDetermineAndSetFinalSpanName_delegates_to_adapter_getFinalSpanName_and_does_NOT_change_span_name_if_result_IS_blank(
        String blankAdapterResult
    ) {
        // given
        if ("[whitespace]".equals(blankAdapterResult)) {
            blankAdapterResult = "   \n\r\t   ";
        }

        doReturn(blankAdapterResult).when(adapterMock).getFinalSpanName(anyObject(), anyObject());

        String originalSpanName = "originalSpanName";
        Span span = Span.newBuilder(originalSpanName, SpanPurpose.SERVER).build();

        // when
        implSpy.doDetermineAndSetFinalSpanName(span, requestObjectMock, responseObjectMock, errorMock, adapterMock);

        // then
        assertThat(span.getSpanName()).isEqualTo(originalSpanName);
        verify(adapterMock).getFinalSpanName(requestObjectMock, responseObjectMock);
    }

    @Test
    public void doExtraWingtipsTagging_adds_SPAN_HANDLER_tag_if_adapter_getSpanHandlerTagValue_is_not_blank() {
        // given
        String adapterSpanHandlerTagValue = UUID.randomUUID().toString();
        doReturn(adapterSpanHandlerTagValue).when(adapterMock).getSpanHandlerTagValue(anyObject(), anyObject());

        // when
        implSpy.doExtraWingtipsTagging(spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock);

        // then
        verify(implSpy).putTagIfValueIsNotBlank(spanMock, WingtipsTags.SPAN_HANDLER, adapterSpanHandlerTagValue);
        verify(spanMock).putTag(WingtipsTags.SPAN_HANDLER, adapterSpanHandlerTagValue);
        verifyNoMoreInteractions(spanMock);
    }

    public static Stream<Arguments> doExtraWingtipsTagging_does_NOT_add_SPAN_HANDLER_tag_if_adapter_getSpanHandlerTagValue_IS_blank_DataProvider() {
        return Stream.of(
            Arguments.of((Object) null),
            Arguments.of(""),
            Arguments.of("[whitespace]")
        );
    }

    @ParameterizedTest
    @MethodSource("doExtraWingtipsTagging_does_NOT_add_SPAN_HANDLER_tag_if_adapter_getSpanHandlerTagValue_IS_blank_DataProvider")
    public void doExtraWingtipsTagging_does_NOT_add_SPAN_HANDLER_tag_if_adapter_getSpanHandlerTagValue_IS_blank(
        String blankAdapterResult
    ) {
        // given
        if ("[whitespace]".equals(blankAdapterResult)) {
            blankAdapterResult = "   \n\r\t   ";
        }
        doReturn(blankAdapterResult).when(adapterMock).getSpanHandlerTagValue(anyObject(), anyObject());

        // when
        implSpy.doExtraWingtipsTagging(spanMock, requestObjectMock, responseObjectMock, errorMock, adapterMock);

        // then
        verify(implSpy).putTagIfValueIsNotBlank(spanMock, WingtipsTags.SPAN_HANDLER, blankAdapterResult);
        verifyNoInteractions(spanMock);
    }

    @Test
    public void putTagIfValueIsNotBlank_calls_span_putTag_as_expected_for_tagValue_toString_when_result_is_not_blank() {
        // given
        Object tagValueMock = mock(Object.class);
        String tagValueToStringResult = "tag_value-" + UUID.randomUUID().toString();
        doReturn(tagValueToStringResult).when(tagValueMock).toString();

        String tagKey = "tag_key-" + UUID.randomUUID().toString();

        // when
        implSpy.putTagIfValueIsNotBlank(spanMock, tagKey, tagValueMock);

        // then
        verify(spanMock).putTag(tagKey, tagValueToStringResult);
    }

    public static Stream<Arguments> putTagIfValueIsNotBlank_does_nothing_when_tagValue_toString_IS_blank_DataProvider() {
        return Stream.of(
            Arguments.of((Object) null),
            Arguments.of(""),
            Arguments.of("[whitespace]")
        );
    }

    @ParameterizedTest
    @MethodSource("putTagIfValueIsNotBlank_does_nothing_when_tagValue_toString_IS_blank_DataProvider")
    public void putTagIfValueIsNotBlank_does_nothing_when_tagValue_toString_IS_blank(
        String blankTagValueToStringResult
    ) {
        // given
        if ("[whitespace]".equals(blankTagValueToStringResult)) {
            blankTagValueToStringResult = "   \n\r\t   ";
        }

        Object tagValueMock = mock(Object.class);
        doReturn(blankTagValueToStringResult).when(tagValueMock).toString();

        String tagKey = "tag_key-" + UUID.randomUUID().toString();

        // when
        implSpy.putTagIfValueIsNotBlank(spanMock, tagKey, tagValueMock);

        // then
        verify(spanMock, never()).putTag(anyString(), anyString());
    }

    public static Stream<Arguments> putTagIfValueIsNotBlank_does_nothing_when_any_arg_is_null_DataProvider() {
        return Stream.of(
            Arguments.of(true, false, false),
            Arguments.of(false, true, false),
            Arguments.of(false, false, true)
        );
    }

    @ParameterizedTest
    @MethodSource("putTagIfValueIsNotBlank_does_nothing_when_any_arg_is_null_DataProvider")
    public void putTagIfValueIsNotBlank_does_nothing_when_any_arg_is_null(
        boolean spanIsNull, boolean tagKeyIsNull, boolean tagValueIsNull
    ) {
        // given
        Span span = (spanIsNull) ? null : spanMock;
        String tagKey = (tagKeyIsNull) ? null : UUID.randomUUID().toString();
        String tagValue = (tagValueIsNull) ? null : UUID.randomUUID().toString();

        // when
        implSpy.putTagIfValueIsNotBlank(span, tagKey, tagValue);

        // then
        verify(spanMock, never()).putTag(anyString(), anyString());
    }

    // A basic impl that implements the required abstract methods, but doesn't override anything. None of these
    //      methods will be tested - we are only going to test the non-abstract default methods
    //      of HttpTagAndSpanNamingStrategy.
    private static class BasicImpl extends HttpTagAndSpanNamingStrategy<Object, Object> {
        @Override
        protected void doHandleRequestTagging(
            @NotNull Span span, @NotNull Object request, @NotNull HttpTagAndSpanNamingAdapter<Object, ?> adapter
        ) {

        }

        @Override
        protected void doHandleResponseAndErrorTagging(
            @NotNull Span span, @Nullable Object request, @Nullable Object response, @Nullable Throwable error,
            @NotNull HttpTagAndSpanNamingAdapter<Object, Object> adapter
        ) {

        }
    }

}