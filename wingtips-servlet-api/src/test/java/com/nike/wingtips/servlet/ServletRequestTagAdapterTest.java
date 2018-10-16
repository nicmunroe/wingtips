package com.nike.wingtips.servlet;

import com.nike.wingtips.servlet.tag.ServletRequestTagAdapter;

import com.tngtech.java.junit.dataprovider.DataProviderRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;

@RunWith(DataProviderRunner.class)
public class ServletRequestTagAdapterTest {

    private HttpServletRequest request;
    private HttpServletResponse response;
    private ServletRequestTagAdapter adapter;
    @Before
    public void setup() {
        adapter = new ServletRequestTagAdapter();
        request = Mockito.mock(HttpServletRequest.class);
        response = Mockito.mock(HttpServletResponse.class);
    }
    
    @Test
    public void fivehundred_responses_are_errors() {
        doReturn(500).when(response).getStatus();
        assertThat(adapter.getErrorResponseTagValue(response)).isNotEmpty();
        
        doReturn(499).when(response).getStatus();
        assertThat(adapter.getErrorResponseTagValue(response)).isEmpty();
    }
    
    @Test
    public void getRequestUri() {
        String path ="/endpoint";
        doReturn(path).when(request).getRequestURI();
        assert(path.equals(adapter.getRequestPath(request)));
    }
    
    @Test
    public void getRequestUrl() {
        StringBuffer url = new StringBuffer("http://www.google.com");
        doReturn(url).when(request).getRequestURL();
        assert(url.toString().equals(adapter.getRequestUrl(request)));
    }

    @Test
    public void getResponseHttpStatus() {
        int status = 200;
        doReturn(status).when(response).getStatus();
        assert(String.valueOf(status).equals(adapter.getResponseHttpStatus(response)));
    }

    @Test
    public void getRequestHttpMethod() {
        String method = "GET";
        doReturn(method).when(request).getMethod();
        assert(method.equals(adapter.getRequestHttpMethod(request)));
    }
}
