package com.nike.wingtips.tags;

import com.nike.wingtips.Span;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@RunWith(DataProviderRunner.class)
public class OpenTracingTagStrategyTest {

    final Integer responseStatus = 200;
    final String httpUrl = "/endpoint";
    final String httpMethod = "GET";
    private Map<String, String> spanTags = new HashMap<>();
    private Span spanMock;
    private Object nullObj = null; // No need for this to have a value
    private String errorResponseTagValue = null;
    private HttpTagAndSpanNamingAdapter<Object, Object> tagAdapter = new HttpTagAndSpanNamingAdapter<Object, Object>() {

        @Override
        public @Nullable String getErrorResponseTagValue(@Nullable Object response) {
            return errorResponseTagValue;
        }

        @Override
        public @Nullable Integer getResponseHttpStatus(@Nullable Object response) {
            return responseStatus;
        }

        @Override
        public @Nullable String getRequestHttpMethod(@Nullable Object request) {
            return httpMethod;
        }

        @Override
        public @Nullable String getHeaderSingleValue(@Nullable Object request, @NotNull String headerKey) {
            // TODO: This
            return null;
        }

        @Override
        public @Nullable List<String> getHeaderMultipleValue(@Nullable Object request, @NotNull String headerKey) {
            // TODO: This
            return null;
        }

        @Override
        public @Nullable String getRequestUriPathTemplate(@Nullable Object request, @Nullable Object response) {
            // TODO: This
            return null;
        }

        @Override
        public @Nullable String getRequestUrl(@Nullable Object request) {
            return httpUrl;
        }

        @Override
        public @Nullable String getRequestPath(@Nullable Object request) {
            return null;
        }

        public @Nullable String getSpanHandlerTagValue(@Nullable Object request, @Nullable Object response) {
            // TODO: this
            return null;
        }

    };
    private OpenTracingTagStrategy<Object, Object> openTracingTagStrategy = new OpenTracingTagStrategy<>();

    @Before
    public void setup() {
        spanMock = mock(Span.class);

        spanTags.clear();
        doReturn(spanTags).when(spanMock).getTags();
    }

    @DataProvider(value = {
        "foobar",
        "null"
    }, splitBy = "\\|")
    @Test
    public void tagspanwithresponseattributes_behaves_as_expected(String adapterErrorResponseTagValue) {
        // given
        this.errorResponseTagValue = adapterErrorResponseTagValue;

        // when
        openTracingTagStrategy.doHandleResponseAndErrorTagging(spanMock, nullObj, nullObj, null, tagAdapter);

        // then
        verify(spanMock).putTag(eq(KnownOpenTracingTags.HTTP_STATUS), eq(responseStatus.toString()));

        if (adapterErrorResponseTagValue != null) {
            // the error tag should only have a value if isErroredResponse is true
            verify(spanMock).putTag(eq(KnownOpenTracingTags.ERROR), eq(adapterErrorResponseTagValue));
        }
        else {
            // there shouldn't be any value for the error tag
            verify(spanMock, never()).putTag(eq(KnownOpenTracingTags.ERROR), any(String.class));
        }
    }

    @Test
    public void tagspanwithrequestattributes_behaves_as_expected() {
        // when
        openTracingTagStrategy.doHandleRequestTagging(spanMock, nullObj, tagAdapter);

        // then
        verify(spanMock).putTag(eq(KnownOpenTracingTags.HTTP_METHOD), eq(httpMethod));
        verify(spanMock).putTag(eq(KnownOpenTracingTags.HTTP_URL), eq(httpUrl));
    }
}
