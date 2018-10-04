package com.nike.wingtips.apache.httpclient.tag;

import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.apache.http.HttpRequest;
import org.apache.http.RequestLine;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Tests the functionality of {@link ApacheHttpClientTagAdapter}.
 *
 * @author Nic Munroe
 */
@RunWith(DataProviderRunner.class)
public class ApacheHttpClientTagAdapterTest {

    private ApacheHttpClientTagAdapter adapter;

    @Before
    public void beforeMethod() {
        adapter = new ApacheHttpClientTagAdapter();
    }

    @DataProvider(value = {
        // Basic HTTP URIs
        "http://foo.bar/some/path                       |   /some/path",
        "http://foo.bar/                                |   /",

        "http://foo.bar:4242/some/path                  |   /some/path",
        "http://foo.bar:4242/                           |   /",

        // Same thing, but for HTTPS
        "https://foo.bar/some/path                      |   /some/path",
        "https://foo.bar/                               |   /",

        "https://foo.bar:4242/some/path                 |   /some/path",
        "https://foo.bar:4242/                          |   /",

        // Basic HTTP URIs with query string
        "http://foo.bar/some/path?thing=stuff           |   /some/path",
        "http://foo.bar/?thing=stuff                    |   /",

        "http://foo.bar:4242/some/path?thing=stuff      |   /some/path",
        "http://foo.bar:4242/?thing=stuff               |   /",

        // Same thing, but for HTTPS (with query string)
        "https://foo.bar/some/path?thing=stuff          |   /some/path",
        "https://foo.bar/?thing=stuff                   |   /",

        "https://foo.bar:4242/some/path?thing=stuff     |   /some/path",
        "https://foo.bar:4242/?thing=stuff              |   /",

        // URIs missing path
        "http://no.real.path                            |   /",
        "https://no.real.path                           |   /",
        "http://no.real.path?thing=stuff                |   /",
        "https://no.real.path?thing=stuff               |   /",

        // URIs missing scheme and host - just path
        "/some/path                                     |   /some/path",
        "/some/path?thing=stuff                         |   /some/path",
        "/                                              |   /",
        "/?thing=stuff                                  |   /",

        // Broken URIs
        "nothttp://foo.bar/some/path                    |   null",
        "missing/leading/slash                          |   null",
        "http//missing.scheme.colon/some/path           |   null",
        "http:/missing.scheme.double.slash/some/path    |   null",
    }, splitBy = "\\|")
    @Test
    public void getRequestPath_works_as_expected_for_request_that_are_not_HttpRequestWrapper(
        String uri, String expectedPath
    ) {
        // given
        HttpRequest nonWrapperRequestMock = mock(HttpRequest.class);
        RequestLine requestLineMock = mock(RequestLine.class);

        doReturn(requestLineMock).when(nonWrapperRequestMock).getRequestLine();
        doReturn(uri).when(requestLineMock).getUri();

        // when
        String result = adapter.getRequestPath(nonWrapperRequestMock);

        // then
        assertThat(result).isEqualTo(expectedPath);
        verify(nonWrapperRequestMock).getRequestLine();
        verify(requestLineMock).getUri();
        verifyNoMoreInteractions(nonWrapperRequestMock, requestLineMock);
    }

}