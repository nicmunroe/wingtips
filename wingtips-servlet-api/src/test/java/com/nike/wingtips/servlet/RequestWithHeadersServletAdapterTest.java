package com.nike.wingtips.servlet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Fail.fail;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Tests the functionality of {@link RequestWithHeadersServletAdapter}
 */
public class RequestWithHeadersServletAdapterTest {

    private HttpServletRequest requestMock;
    private RequestWithHeadersServletAdapter adapter;

    @BeforeEach
    public void setupMethod() {
        requestMock = mock(HttpServletRequest.class);
        adapter = new RequestWithHeadersServletAdapter(requestMock);
    }

    @Test
    public void constructor_throws_illegal_argument_exception_if_passed_null_arg() {
        assertThrows(IllegalArgumentException.class, () -> {
            // expect
            new RequestWithHeadersServletAdapter(null);
            fail("Expected IllegalArgumentException but no exception was thrown");
        });
    }

    @Test
    public void getHeader_delegates_to_servlet_request() {
        // given
        String headerName = "someHeader";
        String headerValue = UUID.randomUUID().toString();
        doReturn(headerValue).when(requestMock).getHeader(headerName);

        // when
        String actual = adapter.getHeader(headerName);

        // then
        assertThat(actual).isEqualTo(headerValue);
        verify(requestMock).getHeader(headerName);
    }

    @Test
    public void getAttribute_delegates_to_servlet_request() {
        // given
        String attributeKey = "someAttr";
        Object attributeValue = UUID.randomUUID();
        doReturn(attributeValue).when(requestMock).getAttribute(attributeKey);

        // when
        Object actual = adapter.getAttribute(attributeKey);

        // then
        assertThat(actual).isEqualTo(attributeValue);
        verify(requestMock).getAttribute(attributeKey);
    }

}