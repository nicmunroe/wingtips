package com.nike.wingtips.servlet.tag;

import com.nike.wingtips.tags.KnownZipkinTags;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.util.stream.Stream;

/**
 * Tests the functionality of {@link ServletRequestTagAdapter}.
 */
public class ServletRequestTagAdapterTest {

    private ServletRequestTagAdapter adapterSpy;
    private HttpServletRequest requestMock;
    private HttpServletResponse responseMock;

    @BeforeEach
    public void setup() {
        adapterSpy = spy(new ServletRequestTagAdapter());
        requestMock = mock(HttpServletRequest.class);
        responseMock = mock(HttpServletResponse.class);
    }

    public static Stream<Arguments> getErrorResponseTagValue_works_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of(null, null),
            Arguments.of(200, null),
            Arguments.of(300, null),
            Arguments.of(400, null),
            Arguments.of(499, null),
            Arguments.of(500, "500"),
            Arguments.of(599, "599"),
            Arguments.of(999, "999")
        );
    }

    @ParameterizedTest
    @MethodSource("getErrorResponseTagValue_works_as_expected_DataProvider")
    public void getErrorResponseTagValue_works_as_expected(Integer statusCode, String expectedTagValue) {
        // given
        doReturn(statusCode).when(adapterSpy).getResponseHttpStatus(any(HttpServletResponse.class));

        // when
        String result = adapterSpy.getErrorResponseTagValue(responseMock);

        // then
        assertThat(result).isEqualTo(expectedTagValue);
        verify(adapterSpy).getErrorResponseTagValue(responseMock);
        verify(adapterSpy).getResponseHttpStatus(responseMock);
        verifyNoMoreInteractions(adapterSpy);
    }

    public static Stream<Arguments> getRequestUrl_works_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of("http://some.host:4242/foo/bar", "queryStr=stuff", "http://some.host:4242/foo/bar?queryStr=stuff"),
            Arguments.of("http://some.host:4242/foo/bar", null, "http://some.host:4242/foo/bar"),
            Arguments.of("http://some.host:4242/foo/bar", "", "http://some.host:4242/foo/bar"),
            Arguments.of("http://some.host:4242/foo/bar", "[whitespace]", "http://some.host:4242/foo/bar")
        );
    }

    @ParameterizedTest
    @MethodSource("getRequestUrl_works_as_expected_DataProvider")
    public void getRequestUrl_works_as_expected(
        String requestUrlNoQueryString, String queryString, String expectedResult
    ) {
        // given
        if ("[whitespace]".equals(queryString)) {
            queryString = "  \t\r\n  ";
        }
        StringBuffer requestUrlNoQueryStrBuffer = new StringBuffer(requestUrlNoQueryString);

        doReturn(requestUrlNoQueryStrBuffer).when(requestMock).getRequestURL();
        doReturn(queryString).when(requestMock).getQueryString();

        // when
        String result = adapterSpy.getRequestUrl(requestMock);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void getRequestUrl_returns_null_if_passed_null() {
        // expect
        assertThat(adapterSpy.getRequestUrl(null)).isNull();
    }

    @Test
    public void getResponseHttpStatus_works_as_expected() {
        // given
        Integer expectedResult = 42;
        doReturn(expectedResult).when(responseMock).getStatus();

        // when
        Integer result = adapterSpy.getResponseHttpStatus(responseMock);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void getResponseHttpStatus_returns_null_if_passed_null() {
        // expect
        assertThat(adapterSpy.getResponseHttpStatus(null)).isNull();
    }

    @Test
    public void getRequestHttpMethod_works_as_expected() {
        // given
        String expectedResult = UUID.randomUUID().toString();
        doReturn(expectedResult).when(requestMock).getMethod();

        // when
        String result = adapterSpy.getRequestHttpMethod(requestMock);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void getRequestHttpMethod_returns_null_if_passed_null() {
        // expect
        assertThat(adapterSpy.getRequestHttpMethod(null)).isNull();
    }

    @Test
    public void getRequestPath_works_as_expected() {
        // given
        String expectedResult = UUID.randomUUID().toString();
        doReturn(expectedResult).when(requestMock).getRequestURI();

        // when
        String result = adapterSpy.getRequestPath(requestMock);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void getRequestPath_returns_null_if_passed_null() {
        // expect
        assertThat(adapterSpy.getRequestPath(null)).isNull();
    }

    // Basically a copy of the HttpSpanFactory.determineUriPathTemplate() test, since getRequestUriPathTemplate
    //      just delegates to HttpSpanFactory.determineUriPathTemplate().

    public static Stream<Arguments> getRequestUriPathTemplate_works_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of("/some/http/route", "/some/spring/pattern", "/some/http/route"),
            Arguments.of("/some/http/route", null, "/some/http/route"),
            Arguments.of("/some/http/route", "", "/some/http/route"),
            Arguments.of("/some/http/route", "[whitespace]", "/some/http/route"),
            Arguments.of(null, "/some/spring/pattern", "/some/spring/pattern"),
            Arguments.of("", "/some/spring/pattern", "/some/spring/pattern"),
            Arguments.of("[whitespace]", "/some/spring/pattern", "/some/spring/pattern"),
            Arguments.of(null, null, null),
            Arguments.of("", "", null),
            Arguments.of("[whitespace]", "[whitespace]", null)
        );
    }

    @ParameterizedTest
    @MethodSource("getRequestUriPathTemplate_works_as_expected_DataProvider")
    public void getRequestUriPathTemplate_works_as_expected(
        String httpRouteRequestAttr,
        String springMatchingPatternRequestAttr,
        String expectedResult
    ) {
        // given
        if ("[whitespace]".equals(httpRouteRequestAttr)) {
            httpRouteRequestAttr = "  \t\r\n  ";
        }

        if ("[whitespace]".equals(springMatchingPatternRequestAttr)) {
            springMatchingPatternRequestAttr = "  \t\r\n  ";
        }

        doReturn(httpRouteRequestAttr).when(requestMock).getAttribute(KnownZipkinTags.HTTP_ROUTE);
        doReturn(springMatchingPatternRequestAttr)
            .when(requestMock).getAttribute("org.springframework.web.servlet.HandlerMapping.bestMatchingPattern");

        // when
        String result = adapterSpy.getRequestUriPathTemplate(requestMock, responseMock);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void getHeaderSingleValue_works_as_expected() {
        // given
        String headerKey = UUID.randomUUID().toString();

        String expectedResult = UUID.randomUUID().toString();
        doReturn(expectedResult).when(requestMock).getHeader(anyString());

        // when
        String result = adapterSpy.getHeaderSingleValue(requestMock, headerKey);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(requestMock).getHeader(headerKey);
    }

    @Test
    public void getHeaderSingleValue_returns_null_if_passed_null_request() {
        // expect
        assertThat(adapterSpy.getHeaderSingleValue(null, "foo")).isNull();
    }

    @Test
    public void getHeaderMultipleValue_works_as_expected() {
        // given
        String headerKey = UUID.randomUUID().toString();

        List<String> expectedResult = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString());
        doReturn(Collections.enumeration(expectedResult)).when(requestMock).getHeaders(anyString());

        // when
        List<String> result = adapterSpy.getHeaderMultipleValue(requestMock, headerKey);

        // then
        assertThat(result).isEqualTo(expectedResult);
        verify(requestMock).getHeaders(headerKey);
    }

    @Test
    public void getHeaderMultipleValue_returns_null_if_request_headers_Enumeration_is_null() {
        // given
        String headerKey = UUID.randomUUID().toString();

        doReturn(null).when(requestMock).getHeaders(anyString());

        // when
        List<String> result = adapterSpy.getHeaderMultipleValue(requestMock, headerKey);

        // then
        assertThat(result).isNull();
        verify(requestMock).getHeaders(headerKey);
    }

    @Test
    public void getHeaderMultipleValue_returns_null_if_passed_null_request() {
        // expect
        assertThat(adapterSpy.getHeaderMultipleValue(null, "foo")).isNull();
    }

    @Test
    public void getSpanHandlerTagValue_returns_expected_value() {
        // expect
        assertThat(adapterSpy.getSpanHandlerTagValue(requestMock, responseMock)).isEqualTo("servlet");
    }
}
