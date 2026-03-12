package com.nike.wingtips.apache.httpclient.tag;

import org.apache.http.Header;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.HttpRequestWrapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
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
 * Tests the functionality of {@link ApacheHttpClientTagAdapter}.
 *
 * @author Nic Munroe
 */
public class ApacheHttpClientTagAdapterTest {

    private ApacheHttpClientTagAdapter implSpy;
    private HttpRequest requestMock;
    private HttpResponse responseMock;
    private RequestLine requestLineMock;

    @BeforeEach
    public void beforeMethod() {
        implSpy = spy(new ApacheHttpClientTagAdapter());
        requestMock = mock(HttpRequest.class);
        responseMock = mock(HttpResponse.class);

        requestLineMock = mock(RequestLine.class);

        doReturn(requestLineMock).when(requestMock).getRequestLine();
    }

    @Test
    public void getDefaultInstance_returns_DEFAULT_INSTANCE() {
        // expect
        assertThat(ApacheHttpClientTagAdapter.getDefaultInstance())
            .isSameAs(ApacheHttpClientTagAdapter.DEFAULT_INSTANCE);
    }

    public static Stream<Arguments> getRequestPath_works_as_expected_for_a_request_that_is_an_HttpRequestWrapper_DataProvider() {
        return Stream.of(
            Arguments.of("http://foo.bar/some/path", "/some/path"),
            Arguments.of("http://foo.bar/some/path?thing=stuff", "/some/path"),
            Arguments.of("/some/path", "/some/path"),
            Arguments.of("/some/path?thing=stuff", "/some/path"),
            Arguments.of("http://foo.bar/", "/"),
            Arguments.of("http://foo.bar/?thing=stuff", "/"),
            Arguments.of("/", "/"),
            Arguments.of("/?thing=stuff", "/")
        );
    }

    @ParameterizedTest
    @MethodSource("getRequestPath_works_as_expected_for_a_request_that_is_an_HttpRequestWrapper_DataProvider")
    public void getRequestPath_works_as_expected_for_a_request_that_is_an_HttpRequestWrapper(
        String uriString, String expectedResult
    ) {
        // given
        HttpRequestWrapper requestWrapperMock = mock(HttpRequestWrapper.class);
        doReturn(URI.create(uriString)).when(requestWrapperMock).getURI();

        // when
        String result = implSpy.getRequestPath(requestWrapperMock);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    public static Stream<Arguments> getRequestPath_works_as_expected_for_a_request_that_is_not_an_HttpRequestWrapper_DataProvider() {
        return Stream.of(
            Arguments.of("http://foo.bar/some/path", "/some/path"),
            Arguments.of("http://foo.bar/", "/"),
            Arguments.of("http://foo.bar:4242/some/path", "/some/path"),
            Arguments.of("http://foo.bar:4242/", "/"),
            Arguments.of("https://foo.bar/some/path", "/some/path"),
            Arguments.of("https://foo.bar/", "/"),
            Arguments.of("https://foo.bar:4242/some/path", "/some/path"),
            Arguments.of("https://foo.bar:4242/", "/"),
            Arguments.of("http://foo.bar/some/path?thing=stuff", "/some/path"),
            Arguments.of("http://foo.bar/?thing=stuff", "/"),
            Arguments.of("http://foo.bar:4242/some/path?thing=stuff", "/some/path"),
            Arguments.of("http://foo.bar:4242/?thing=stuff", "/"),
            Arguments.of("https://foo.bar/some/path?thing=stuff", "/some/path"),
            Arguments.of("https://foo.bar/?thing=stuff", "/"),
            Arguments.of("https://foo.bar:4242/some/path?thing=stuff", "/some/path"),
            Arguments.of("https://foo.bar:4242/?thing=stuff", "/"),
            Arguments.of("http://no.real.path", "/"),
            Arguments.of("https://no.real.path", "/"),
            Arguments.of("http://no.real.path?thing=stuff", "/"),
            Arguments.of("https://no.real.path?thing=stuff", "/"),
            Arguments.of("/some/path", "/some/path"),
            Arguments.of("/some/path?thing=stuff", "/some/path"),
            Arguments.of("/", "/"),
            Arguments.of("/?thing=stuff", "/"),
            Arguments.of("nothttp://foo.bar/some/path", null),
            Arguments.of("missing/leading/slash", null),
            Arguments.of("http//missing.scheme.colon/some/path", null),
            Arguments.of("http:/missing.scheme.double.slash/some/path", null)
        );
    }

    @ParameterizedTest
    @MethodSource("getRequestPath_works_as_expected_for_a_request_that_is_not_an_HttpRequestWrapper_DataProvider")
    public void getRequestPath_works_as_expected_for_a_request_that_is_not_an_HttpRequestWrapper(
        String uri, String expectedPath
    ) {
        // given
        doReturn(uri).when(requestLineMock).getUri();

        // when
        String result = implSpy.getRequestPath(requestMock);

        // then
        assertThat(result).isEqualTo(expectedPath);
        verify(requestMock).getRequestLine();
        verify(requestLineMock).getUri();
        verifyNoMoreInteractions(requestMock, requestLineMock);
    }

    @Test
    public void getRequestPath_returns_null_if_passed_null() {
        // expect
        assertThat(implSpy.getRequestPath(null)).isNull();
    }

    private enum GetResponseHttpStatusScenario {
        HAPPY_PATH(false, false, 42),
        RESPONSE_OBJ_IS_NULL(true, false, null),
        STATUS_LINE_IS_NULL(false, true, null);

        public final HttpResponse responseObjMock;
        public final Integer expectedResult;

        GetResponseHttpStatusScenario(boolean responseObjIsNull, boolean statusLineIsNull, Integer expectedResult) {
            HttpResponse response = null;
            if (!responseObjIsNull) {
                response = mock(HttpResponse.class);

                StatusLine statusLine = null;
                if (!statusLineIsNull) {
                    statusLine = mock(StatusLine.class);
                    doReturn(expectedResult).when(statusLine).getStatusCode();
                }

                doReturn(statusLine).when(response).getStatusLine();
            }

            this.responseObjMock = response;
            this.expectedResult = expectedResult;
        }
    }

    public static Stream<Arguments> getResponseHttpStatus_gets_status_code_from_given_response_StatusLine_DataProvider() {
        return Stream.of(
            Arguments.of(GetResponseHttpStatusScenario.HAPPY_PATH),
            Arguments.of(GetResponseHttpStatusScenario.RESPONSE_OBJ_IS_NULL),
            Arguments.of(GetResponseHttpStatusScenario.STATUS_LINE_IS_NULL)
        );
    }

    @ParameterizedTest
    @MethodSource("getResponseHttpStatus_gets_status_code_from_given_response_StatusLine_DataProvider")
    public void getResponseHttpStatus_gets_status_code_from_given_response_StatusLine(
        GetResponseHttpStatusScenario scenario
    ) {
        // when
        Integer result = implSpy.getResponseHttpStatus(scenario.responseObjMock);

        // then
        assertThat(result).isEqualTo(scenario.expectedResult);
    }

    public static Stream<Arguments> getRequestHttpMethod_extracts_result_from_RequestLine_DataProvider() {
        return Stream.of(
            Arguments.of(false, "GET"),
            Arguments.of(false, "POST"),
            Arguments.of(false, "FOO"),
            Arguments.of(false, null),
            Arguments.of(true, null)
        );
    }

    @ParameterizedTest
    @MethodSource("getRequestHttpMethod_extracts_result_from_RequestLine_DataProvider")
    public void getRequestHttpMethod_extracts_result_from_RequestLine(
        boolean requestIsNull, String expectedResult
    ) {
        // given
        requestMock = (requestIsNull) ? null : requestMock;
        doReturn(expectedResult).when(requestLineMock).getMethod();

        // when
        String result = implSpy.getRequestHttpMethod(requestMock);

        // then
        assertThat(result).isEqualTo(expectedResult);
    }

    @Test
    public void getRequestUriPathTemplate_returns_null() {
        // expect
        assertThat(implSpy.getRequestUriPathTemplate(requestMock, responseMock)).isNull();
    }

    private enum GetRequestUrlScenario {
        REQUEST_OBJ_IS_NULL(
            true, false, null, true, null, null
        ),
        REQUEST_OBJ_IS_NOT_WRAPPER(
            false, false, "/some/base/uri", true, null, "/some/base/uri"
        ),
        REQUEST_IS_WRAPPER_BUT_DOES_NOT_START_WITH_SLASH(
            false, true, "some/base/uri", true, null, "some/base/uri"
        ),
        REQUEST_IS_WRAPPER_AND_STARTS_WITH_SLASH_BUT_TARGET_IS_NULL(
            false, true, "/some/base/uri", true, null, "/some/base/uri"
        ),
        REQUEST_IS_WRAPPER_AND_STARTS_WITH_SLASH_AND_TARGET_HAS_URI(
            false, true, "/some/base/uri", false, "http://foo.bar:4242", "http://foo.bar:4242/some/base/uri"
        );

        public final HttpRequest requestMock;
        public final String expectedResult;

        GetRequestUrlScenario(
            boolean requestIsNull, boolean requestIsWrapper, Object baseUri, boolean targetIsNull,
            String targetUri, String expectedResult
        ) {
            this.expectedResult = expectedResult;
            HttpRequest request = null;

            if (!requestIsNull) {
                request = (requestIsWrapper) ? mock(HttpRequestWrapper.class) : mock(HttpRequest.class);
                RequestLine requestLine = mock(RequestLine.class);
                doReturn(requestLine).when(request).getRequestLine();
                doReturn(baseUri).when(requestLine).getUri();

                if (!targetIsNull) {
                    HttpHost target = HttpHost.create(targetUri);
                    doReturn(target).when((HttpRequestWrapper) request).getTarget();
                }
            }

            this.requestMock = request;
        }
    }

    public static Stream<Arguments> getRequestUrl_works_as_expected_DataProvider() {
        return Stream.of(
            Arguments.of(GetRequestUrlScenario.REQUEST_OBJ_IS_NULL),
            Arguments.of(GetRequestUrlScenario.REQUEST_OBJ_IS_NOT_WRAPPER),
            Arguments.of(GetRequestUrlScenario.REQUEST_IS_WRAPPER_BUT_DOES_NOT_START_WITH_SLASH),
            Arguments.of(GetRequestUrlScenario.REQUEST_IS_WRAPPER_AND_STARTS_WITH_SLASH_BUT_TARGET_IS_NULL),
            Arguments.of(GetRequestUrlScenario.REQUEST_IS_WRAPPER_AND_STARTS_WITH_SLASH_AND_TARGET_HAS_URI)
        );
    }

    @ParameterizedTest
    @MethodSource("getRequestUrl_works_as_expected_DataProvider")
    public void getRequestUrl_works_as_expected(GetRequestUrlScenario scenario) {
        // when
        String result = implSpy.getRequestUrl(scenario.requestMock);

        // then
        assertThat(result).isEqualTo(scenario.expectedResult);
    }

    @Test
    public void getHeaderSingleValue_works_as_expected_for_header_that_exists() {
        // given
        String headerKey = "headerKey-" + UUID.randomUUID().toString();
        String expectedHeaderValue = "headerValue-" + UUID.randomUUID().toString();

        Header matchingHeaderMock = mock(Header.class);

        doReturn(matchingHeaderMock).when(requestMock).getFirstHeader(headerKey);
        doReturn(expectedHeaderValue).when(matchingHeaderMock).getValue();

        // when
        String result = implSpy.getHeaderSingleValue(requestMock, headerKey);

        // then
        assertThat(result).isEqualTo(expectedHeaderValue);
        verify(requestMock).getFirstHeader(headerKey);
        verify(matchingHeaderMock).getValue();
    }

    @Test
    public void getHeaderSingleValue_returns_null_if_no_matching_header_found() {
        // given
        String headerKey = "headerKey-" + UUID.randomUUID().toString();

        doReturn(null).when(requestMock).getFirstHeader(anyString());

        // when
        String result = implSpy.getHeaderSingleValue(requestMock, headerKey);

        // then
        assertThat(result).isNull();
        verify(requestMock).getFirstHeader(headerKey);
    }

    @Test
    public void getHeaderSingleValue_returns_null_if_passed_null_request() {
        // when
        String result = implSpy.getHeaderSingleValue(null, "foo");

        // then
        assertThat(result).isNull();
    }

    @Test
    public void getHeaderMultipleValue_works_as_expected_for_headers_that_exist() {
        // given
        String headerKey = "headerKey-" + UUID.randomUUID().toString();
        List<String> expectedHeaderValues = Arrays.asList(
            "headerValue1-" + UUID.randomUUID().toString(),
            "headerValue1-" + UUID.randomUUID().toString()
        );

        Header[] matchingHeaderMocksArray = expectedHeaderValues
            .stream()
            .map(v -> {
                Header headerMock = mock(Header.class);
                doReturn(v).when(headerMock).getValue();
                return headerMock;
            })
            .toArray(Header[]::new);

        doReturn(matchingHeaderMocksArray).when(requestMock).getHeaders(headerKey);

        // when
        List<String> result = implSpy.getHeaderMultipleValue(requestMock, headerKey);

        // then
        assertThat(result).isEqualTo(expectedHeaderValues);
        verify(requestMock).getHeaders(headerKey);
        for (Header headerMock : matchingHeaderMocksArray) {
            verify(headerMock).getValue();
        }
    }

    public static Stream<Arguments> getHeaderMultipleValue_returns_null_when_no_matching_headers_found_DataProvider() {
        return Stream.of(
            Arguments.of(true),
            Arguments.of(false)
        );
    }

    @ParameterizedTest
    @MethodSource("getHeaderMultipleValue_returns_null_when_no_matching_headers_found_DataProvider")
    public void getHeaderMultipleValue_returns_null_when_no_matching_headers_found(boolean nullMatchingHeaders) {
        // given
        String headerKey = "headerKey-" + UUID.randomUUID().toString();
        Header[] nullOrEmptyMatchingHeadersArray = (nullMatchingHeaders) ? null : new Header[0];

        doReturn(nullOrEmptyMatchingHeadersArray).when(requestMock).getHeaders(headerKey);

        // when
        List<String> result = implSpy.getHeaderMultipleValue(requestMock, headerKey);

        // then
        assertThat(result).isNull();
        verify(requestMock).getHeaders(headerKey);
    }

    @Test
    public void getHeaderMultipleValue_returns_null_when_passed_null_request() {
        // expect
        assertThat(implSpy.getHeaderMultipleValue(null, "foo")).isNull();
    }

    @Test
    public void getSpanHandlerTagValue_returns_expected_value() {
        assertThat(implSpy.getSpanHandlerTagValue(requestMock, responseMock)).isEqualTo("apache.httpclient");
    }
}