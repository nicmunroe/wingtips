package com.nike.wingtips.tags;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

/**
 * Tests the functionality of the default methods found in {@link HttpTagAndSpanNamingAdapter}.
 *
 * @author Nic Munroe
 */
public class HttpTagAndSpanNamingAdapterTest {

    private HttpTagAndSpanNamingAdapter<Object, Object> implSpy;

    @BeforeEach
    public void beforeMethod() {
        implSpy = spy(new BasicImpl());
    }

    public static Stream<Arguments> getErrorResponseTagValue_works_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of(0, null),
            Arguments.of(100, null),
            Arguments.of(200, null),
            Arguments.of(300, null),
            Arguments.of(399, null),
            Arguments.of(400, "400"),
            Arguments.of(499, "499"),
            Arguments.of(500, "500"),
            Arguments.of(599, "599"),
            Arguments.of(999, "999")
        );
    }

    @ParameterizedTest
    @MethodSource("getErrorResponseTagValue_works_as_expected_DataProvider")
    public void getErrorResponseTagValue_works_as_expected(Integer responseStatusCode, String expectedReturnVal) {
        // given
        doReturn(responseStatusCode).when(implSpy).getResponseHttpStatus(anyObject());

        // when
        // Null response object makes no difference - it's entirely dependent on what getResponseHttpStatus() returns.
        String resultForNonNullResponseObj = implSpy.getErrorResponseTagValue(new Object());
        String resultForNullResponseObj = implSpy.getErrorResponseTagValue(null);

        // then
        assertThat(resultForNonNullResponseObj).isEqualTo(expectedReturnVal);
        assertThat(resultForNullResponseObj).isEqualTo(expectedReturnVal);
    }

    public static Stream<Arguments> getSpanNamePrefix_returns_null_DataProvider() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("getSpanNamePrefix_returns_null_DataProvider")
    public void getSpanNamePrefix_returns_null(boolean passNullRequestObj) {
        // given
        Object requestObj = (passNullRequestObj) ? null : new Object();

        // when
        String result = implSpy.getSpanNamePrefix(requestObj);

        // then
        assertThat(result).isNull();
    }

    public static Stream<Arguments> getInitialSpanName_and_getFinalSpanName_work_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of("somePrefix", "someHttpMethod", "some/path/template", null, "somePrefix-someHttpMethod some/path/template"),
            Arguments.of("somePrefix", "someHttpMethod", "some/path/template", 299, "somePrefix-someHttpMethod some/path/template"),
            Arguments.of("somePrefix", "someHttpMethod", "some/path/template", 300, "somePrefix-someHttpMethod redirected"),
            Arguments.of("somePrefix", "someHttpMethod", "some/path/template", 399, "somePrefix-someHttpMethod redirected"),
            Arguments.of("somePrefix", "someHttpMethod", "some/path/template", 400, "somePrefix-someHttpMethod some/path/template"),
            Arguments.of("somePrefix", "someHttpMethod", "some/path/template", 404, "somePrefix-someHttpMethod not_found"),
            Arguments.of("somePrefix", "someHttpMethod", "some/path/template", 499, "somePrefix-someHttpMethod some/path/template"),
            Arguments.of("somePrefix", "someHttpMethod", "some/path/template", 500, "somePrefix-someHttpMethod some/path/template"),
            Arguments.of(null, "someHttpMethod", "some/path/template", null, "someHttpMethod some/path/template"),
            Arguments.of(null, "someHttpMethod", "some/path/template", 300, "someHttpMethod redirected"),
            Arguments.of(null, "someHttpMethod", "some/path/template", 404, "someHttpMethod not_found"),
            Arguments.of("somePrefix", null, "some/path/template", null, "somePrefix-UNKNOWN_HTTP_METHOD some/path/template"),
            Arguments.of("somePrefix", null, "some/path/template", 300, "somePrefix-UNKNOWN_HTTP_METHOD redirected"),
            Arguments.of("somePrefix", null, "some/path/template", 404, "somePrefix-UNKNOWN_HTTP_METHOD not_found"),
            Arguments.of(null, null, "some/path/template", null, "UNKNOWN_HTTP_METHOD some/path/template"),
            Arguments.of(null, null, "some/path/template", 300, "UNKNOWN_HTTP_METHOD redirected"),
            Arguments.of(null, null, "some/path/template", 404, "UNKNOWN_HTTP_METHOD not_found"),
            Arguments.of("somePrefix", "someHttpMethod", null, null, "somePrefix-someHttpMethod"),
            Arguments.of("somePrefix", "someHttpMethod", null, 300, "somePrefix-someHttpMethod redirected"),
            Arguments.of("somePrefix", "someHttpMethod", null, 404, "somePrefix-someHttpMethod not_found"),
            Arguments.of(null, "someHttpMethod", null, null, "someHttpMethod"),
            Arguments.of(null, "someHttpMethod", null, 300, "someHttpMethod redirected"),
            Arguments.of(null, "someHttpMethod", null, 404, "someHttpMethod not_found")
        );
    }

    @ParameterizedTest
    @MethodSource("getInitialSpanName_and_getFinalSpanName_work_as_expected_DataProvider")
    public void getInitialSpanName_and_getFinalSpanName_work_as_expected(
        String prefix, String httpMethod, String pathTemplate, Integer responseStatusCode, String expectedResult
    ) {
        // given
        doReturn(prefix).when(implSpy).getSpanNamePrefix(anyObject());
        doReturn(httpMethod).when(implSpy).getRequestHttpMethod(anyObject());
        doReturn(pathTemplate).when(implSpy).getRequestUriPathTemplate(anyObject(), anyObject());
        doReturn(responseStatusCode).when(implSpy).getResponseHttpStatus(anyObject());

        // when
        // getInitialSpanName() and getFinalSpanName() effectively have the same logic - it all boils down to what
        //      getSpanNamePrefix(), getRequestHttpMethod(), getRequestUriPathTemplate(), and getResponseHttpStatus()
        //      return. In practice those methods may return different values at the beginning when there's only a
        //      request object, and at the end when there's a response object (and possibly a changed request object).
        //      But for the purposes of this test, they are the same.
        String initialSpanNameResult = implSpy.getInitialSpanName(new Object());
        String finalSpanNameResult = implSpy.getFinalSpanName(new Object(), new Object());

        // then
        assertThat(initialSpanNameResult).isEqualTo(expectedResult);
        assertThat(finalSpanNameResult).isEqualTo(expectedResult);
    }

    // A basic impl that implements the required abstract methods, but doesn't override anything. None of these
    //      methods will be tested - we are only going to test the non-abstract default methods
    //      of HttpTagAndSpanNamingAdapter.
    private static class BasicImpl extends HttpTagAndSpanNamingAdapter<Object, Object> {
        @Override
        public @Nullable String getRequestUrl(@Nullable Object request) {
            return null;
        }

        @Override
        public @Nullable String getRequestPath(@Nullable Object request) {
            return null;
        }

        @Override
        public @Nullable String getRequestUriPathTemplate(
            @Nullable Object request, @Nullable Object response
        ) {
            return null;
        }

        @Override
        public @Nullable Integer getResponseHttpStatus(@Nullable Object response) {
            return null;
        }

        @Override
        public @Nullable String getRequestHttpMethod(@Nullable Object request) {
            return null;
        }

        @Override
        public @Nullable String getHeaderSingleValue(
            @Nullable Object request, @NotNull String headerKey
        ) {
            return null;
        }

        @Override
        public @Nullable List<String> getHeaderMultipleValue(
            @Nullable Object request, @NotNull String headerKey
        ) {
            return null;
        }

        @Override
        public @Nullable String getSpanHandlerTagValue(
            @Nullable Object request, @Nullable Object response
        ) {
            return null;
        }
    }

}