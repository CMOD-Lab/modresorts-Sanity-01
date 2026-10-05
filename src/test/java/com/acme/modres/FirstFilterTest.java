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

import java.io.PrintWriter;
import java.io.StringWriter;

@ExtendWith(MockitoExtension.class)
public class FirstFilterTest {

    private FirstFilter firstFilter;

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
        firstFilter = new FirstFilter();
    }

    @Test
    void testConstructor_createsInstance() {
        assertNotNull(firstFilter);
    }

    @Test
    void testInit_doesNotThrow() {
        assertDoesNotThrow(() -> firstFilter.init(filterConfig));
    }

    @Test
    void testDestroy_doesNotThrow() {
        assertDoesNotThrow(() -> firstFilter.destroy());
    }

    @Test
    void testDoFilter_withUserParameter_writesWelcomeWithUser() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getParameter("user")).thenReturn("Alice");
        when(response.getWriter()).thenReturn(printWriter);

        firstFilter.doFilter(request, response, filterChain);

        verify(response).setContentType("text/plain");
        assertTrue(stringWriter.toString().contains("Alice"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilter_withNullUser_usesDefaultUser() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getParameter("user")).thenReturn(null);
        when(response.getWriter()).thenReturn(printWriter);

        firstFilter.doFilter(request, response, filterChain);

        assertTrue(stringWriter.toString().contains("defaultUser"));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testDoFilter_setsContentType() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getParameter("user")).thenReturn("Bob");
        when(response.getWriter()).thenReturn(printWriter);

        firstFilter.doFilter(request, response, filterChain);

        verify(response).setContentType("text/plain");
    }

    @Test
    void testDoFilter_callsFilterChain() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getParameter("user")).thenReturn("Charlie");
        when(response.getWriter()).thenReturn(printWriter);

        firstFilter.doFilter(request, response, filterChain);

        verify(filterChain, times(1)).doFilter(request, response);
    }

    @Test
    void testDoFilter_withEmptyUser_writesWelcomeWithEmptyUser() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(request.getParameter("user")).thenReturn("");
        when(response.getWriter()).thenReturn(printWriter);

        firstFilter.doFilter(request, response, filterChain);

        assertTrue(stringWriter.toString().contains("Welcome"));
    }
}
