package com.nike.wingtips.tags;

import com.nike.wingtips.Span;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

/**
 * Tests the functionality of {@link OpenTracingHttpTagStrategy}.
 */
public class OpenTracingHttpTagStrategyTest {

    private OpenTracingHttpTagStrategy<Object, Object> implSpy;
    private Span spanMock;
    private Object requestMock;
    private Object responseMock;
    private Throwable errorMock;
    private HttpTagAndSpanNamingAdapter<Object, Object> adapterMock;

    @BeforeEach
    public void beforeMethod() {
        implSpy = spy(new OpenTracingHttpTagStrategy<>());

        spanMock = mock(Span.class);
        requestMock = mock(Object.class);
        responseMock = mock(Object.class);
        errorMock = mock(Throwable.class);
        adapterMock = mock(HttpTagAndSpanNamingAdapter.class);
    }

    @Test
    public void getDefaultInstance_returns_DEFAULT_INSTANCE() {
        // expect
        assertThat(OpenTracingHttpTagStrategy.getDefaultInstance()).isSameAs(OpenTracingHttpTagStrategy.DEFAULT_INSTANCE);
    }

    @Test
    public void doHandleRequestTagging_puts_expected_tags_based_on_adapter_results() {
        // given
        String adapterHttpMethod = "httpmethod-" + UUID.randomUUID().toString();
        String adapterHttpUrl = "url-" + UUID.randomUUID().toString();

        doReturn(adapterHttpMethod).when(adapterMock).getRequestHttpMethod(any());
        doReturn(adapterHttpUrl).when(adapterMock).getRequestUrl(any());

        // when
        implSpy.doHandleRequestTagging(spanMock, requestMock, adapterMock);

        // then
        verify(adapterMock).getRequestHttpMethod(requestMock);
        verify(adapterMock).getRequestUrl(requestMock);

        verify(implSpy).putTagIfValueIsNotBlank(spanMock, KnownOpenTracingTags.HTTP_METHOD, adapterHttpMethod);
        verify(implSpy).putTagIfValueIsNotBlank(spanMock, KnownOpenTracingTags.HTTP_URL, adapterHttpUrl);
    }

    private enum ErrorTaggingScenario {
        ERROR_IS_NOT_NULL(new RuntimeException("boom"), null, true),
        ERROR_IS_NULL_BUT_ADAPTER_ERROR_TAG_VALUE_IS_NOT_BLANK(null, "foo", true),
        ERROR_IS_NULL_AND_ADAPTER_ERROR_TAG_VALUE_IS_NULL(null, null, false),
        ERROR_IS_NULL_AND_ADAPTER_ERROR_TAG_VALUE_IS_EMPTY(null, "", false),
        ERROR_IS_NULL_AND_ADAPTER_ERROR_TAG_VALUE_IS_WHITESPACE(null, "  \n\r\t  ", false);

        public final Throwable error;
        public final String adapterErrorTagValue;
        public final boolean expectErrorTagPutOnSpan;

        ErrorTaggingScenario(Throwable error, String adapterErrorTagValue, boolean expectErrorTagPutOnSpan) {
            this.error = error;
            this.adapterErrorTagValue = adapterErrorTagValue;
            this.expectErrorTagPutOnSpan = expectErrorTagPutOnSpan;
        }
    }

    public static Stream<Arguments> doHandleResponseAndErrorTagging_puts_expected_tags_based_on_adapter_results_and_error_existence_DataProvider() {
        return Stream.of(
            Arguments.of(ErrorTaggingScenario.ERROR_IS_NOT_NULL),
            Arguments.of(ErrorTaggingScenario.ERROR_IS_NULL_BUT_ADAPTER_ERROR_TAG_VALUE_IS_NOT_BLANK),
            Arguments.of(ErrorTaggingScenario.ERROR_IS_NULL_AND_ADAPTER_ERROR_TAG_VALUE_IS_NULL),
            Arguments.of(ErrorTaggingScenario.ERROR_IS_NULL_AND_ADAPTER_ERROR_TAG_VALUE_IS_EMPTY),
            Arguments.of(ErrorTaggingScenario.ERROR_IS_NULL_AND_ADAPTER_ERROR_TAG_VALUE_IS_WHITESPACE)
        );
    }

    @ParameterizedTest
    @MethodSource("doHandleResponseAndErrorTagging_puts_expected_tags_based_on_adapter_results_and_error_existence_DataProvider")
    public void doHandleResponseAndErrorTagging_puts_expected_tags_based_on_adapter_results_and_error_existence(
        ErrorTaggingScenario scenario
    ) {
        // given
        Integer adapterHttpStatus = 42;
        doReturn(adapterHttpStatus).when(adapterMock).getResponseHttpStatus(any());

        doReturn(scenario.adapterErrorTagValue).when(adapterMock).getErrorResponseTagValue(any());

        // when
        implSpy.doHandleResponseAndErrorTagging(spanMock, requestMock, responseMock, scenario.error, adapterMock);

        // then
        verify(adapterMock).getResponseHttpStatus(responseMock);

        verify(implSpy).putTagIfValueIsNotBlank(spanMock, KnownOpenTracingTags.HTTP_STATUS, adapterHttpStatus);

        if (scenario.error == null) {
            // This call is only made if error is null.
            verify(adapterMock).getErrorResponseTagValue(responseMock);
        }
        else {
            verify(adapterMock, never()).getErrorResponseTagValue(any());
        }

        if (scenario.expectErrorTagPutOnSpan) {
            verify(spanMock).putTag(KnownOpenTracingTags.ERROR, "true");
        }
    }

}
