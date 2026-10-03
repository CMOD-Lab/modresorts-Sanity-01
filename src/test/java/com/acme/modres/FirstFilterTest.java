package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FirstFilterTest {

    private FirstFilter firstFilter;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private FilterChain mockFilterChain;

    @Mock
    private FilterConfig mockFilterConfig;

    @BeforeEach
    void setUp() {
        firstFilter = new FirstFilter();
    }

    @Test
    void testInit_DoesNotThrow() throws Exception {
        assertDoesNotThrow(() -> firstFilter.init(mockFilterConfig));
    }

    @Test
    void testDestroy_DoesNotThrow() {
        assertDoesNotThrow(() -> firstFilter.destroy());
    }

    @Test
    void testDoFilter_WithUserParameter_WritesWelcomeMessage() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockRequest.getParameter("user")).thenReturn("testUser");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        firstFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockResponse).setContentType("text/plain");
        verify(mockResponse).getWriter();
        assertTrue(stringWriter.toString().contains("testUser"));
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    void testDoFilter_WithNullUser_UsesDefaultUser() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockRequest.getParameter("user")).thenReturn(null);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        firstFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockResponse).setContentType("text/plain");
        assertTrue(stringWriter.toString().contains("defaultUser"));
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    void testDoFilter_WithEmptyUser_WritesWelcomeWithEmptyUser() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockRequest.getParameter("user")).thenReturn("");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        firstFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockResponse).setContentType("text/plain");
        assertTrue(stringWriter.toString().contains("Welcome"));
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    void testDoFilter_CallsChainDoFilter() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockRequest.getParameter("user")).thenReturn("user1");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        firstFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockFilterChain, times(1)).doFilter(mockRequest, mockResponse);
    }

    @Test
    void testDoFilter_SetsContentTypeToTextPlain() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockRequest.getParameter("user")).thenReturn("user1");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        firstFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockResponse).setContentType("text/plain");
    }

    @Test
    void testFirstFilter_IsInstanceOfFilter() {
        assertTrue(firstFilter instanceof jakarta.servlet.Filter);
    }
}
