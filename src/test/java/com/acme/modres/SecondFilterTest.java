package com.acme.modres;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecondFilterTest {

    private SecondFilter filter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain chain;

    @Mock
    private FilterConfig filterConfig;

    @BeforeEach
    void setUp() {
        filter = new SecondFilter();
    }

    @Test
    void testFilter_isNotNull() {
        assertNotNull(filter);
    }

    @Test
    void testInit_doesNotThrow() {
        assertDoesNotThrow(() -> filter.init(filterConfig));
    }

    @Test
    void testDestroy_doesNotThrow() {
        assertDoesNotThrow(() -> filter.destroy());
    }

    @Test
    void testDoFilter_setsContentTypePlainText() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        BufferedReader reader = new BufferedReader(new StringReader("Hello"));
        when(request.getReader()).thenReturn(reader);

        filter.doFilter(request, response, chain);

        verify(response).setContentType("text/plain");
    }

    @Test
    void testDoFilter_writesRequestContent() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        BufferedReader reader = new BufferedReader(new StringReader("Welcome"));
        when(request.getReader()).thenReturn(reader);

        filter.doFilter(request, response, chain);

        pw.flush();
        assertTrue(sw.toString().contains("Welcome"));
    }

    @Test
    void testDoFilter_callsChainDoFilter() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        BufferedReader reader = new BufferedReader(new StringReader("test"));
        when(request.getReader()).thenReturn(reader);

        filter.doFilter(request, response, chain);

        verify(chain).doFilter(request, response);
    }

    @Test
    void testDoFilter_withEmptyBody_doesNotThrow() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        BufferedReader reader = new BufferedReader(new StringReader(""));
        when(request.getReader()).thenReturn(reader);

        assertDoesNotThrow(() -> filter.doFilter(request, response, chain));
    }

    @Test
    void testDoFilter_appendsToOurSite() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        BufferedReader reader = new BufferedReader(new StringReader("Hello"));
        when(request.getReader()).thenReturn(reader);

        filter.doFilter(request, response, chain);

        pw.flush();
        assertTrue(sw.toString().contains("to our site!"));
    }

    @Test
    void testSecondFilter_implementsFilter() {
        assertTrue(filter instanceof jakarta.servlet.Filter);
    }
}
