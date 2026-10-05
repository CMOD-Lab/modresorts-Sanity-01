package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

@ExtendWith(MockitoExtension.class)
public class SecondFilterTest {

    private SecondFilter secondFilter;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @Mock
    private FilterConfig filterConfig;

    @BeforeEach
    void setUp() {
        secondFilter = new SecondFilter();
    }

    @Test
    void testConstructor_createsInstance() {
        assertNotNull(secondFilter);
    }

    @Test
    void testInit_doesNotThrow() {
        assertDoesNotThrow(() -> secondFilter.init(filterConfig));
    }

    @Test
    void testDestroy_doesNotThrow() {
        assertDoesNotThrow(() -> secondFilter.destroy());
    }

    @Test
    void testDoFilter_withBodyContent_writesContent() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        BufferedReader reader = new BufferedReader(new StringReader("Hello World"));

        when(request.getReader()).thenReturn(reader);
        when(response.getWriter()).thenReturn(printWriter);

        secondFilter.doFilter(request, response, filterChain);

        assertTrue(stringWriter.toString().contains("Hello World"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilter_setsContentType() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        BufferedReader reader = new BufferedReader(new StringReader("test content"));

        when(request.getReader()).thenReturn(reader);
        when(response.getWriter()).thenReturn(printWriter);

        secondFilter.doFilter(request, response, filterChain);

        verify(response).setContentType("text/plain");
    }

    @Test
    void testDoFilter_appendsSiteMessage() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        BufferedReader reader = new BufferedReader(new StringReader("Welcome"));

        when(request.getReader()).thenReturn(reader);
        when(response.getWriter()).thenReturn(printWriter);

        secondFilter.doFilter(request, response, filterChain);

        assertTrue(stringWriter.toString().contains("to our site!"));
    }

    @Test
    void testDoFilter_withEmptyBody_writesOnlySiteMessage() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        BufferedReader reader = new BufferedReader(new StringReader(""));

        when(request.getReader()).thenReturn(reader);
        when(response.getWriter()).thenReturn(printWriter);

        secondFilter.doFilter(request, response, filterChain);

        assertTrue(stringWriter.toString().contains("to our site!"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilter_callsFilterChain() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        BufferedReader reader = new BufferedReader(new StringReader("content"));

        when(request.getReader()).thenReturn(reader);
        when(response.getWriter()).thenReturn(printWriter);

        secondFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }
}
