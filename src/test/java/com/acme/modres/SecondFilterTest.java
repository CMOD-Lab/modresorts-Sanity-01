package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
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
public class SecondFilterTest {

    private SecondFilter secondFilter;

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
        secondFilter = new SecondFilter();
    }

    @Test
    void testInit_DoesNotThrow() throws Exception {
        assertDoesNotThrow(() -> secondFilter.init(mockFilterConfig));
    }

    @Test
    void testDestroy_DoesNotThrow() {
        assertDoesNotThrow(() -> secondFilter.destroy());
    }

    @Test
    void testDoFilter_WithRequestBody_WritesContent() throws Exception {
        // Arrange
        String requestBody = "Hello";
        BufferedReader reader = new BufferedReader(new StringReader(requestBody));
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getReader()).thenReturn(reader);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        secondFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockResponse).setContentType("text/plain");
        assertTrue(stringWriter.toString().contains("Hello"));
        assertTrue(stringWriter.toString().contains("to our site!"));
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    void testDoFilter_WithEmptyBody_WritesEmptyContent() throws Exception {
        // Arrange
        BufferedReader reader = new BufferedReader(new StringReader(""));
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getReader()).thenReturn(reader);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        secondFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockResponse).setContentType("text/plain");
        assertTrue(stringWriter.toString().contains("to our site!"));
        verify(mockFilterChain).doFilter(mockRequest, mockResponse);
    }

    @Test
    void testDoFilter_SetsContentTypeToTextPlain() throws Exception {
        // Arrange
        BufferedReader reader = new BufferedReader(new StringReader("test"));
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getReader()).thenReturn(reader);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        secondFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockResponse).setContentType("text/plain");
    }

    @Test
    void testDoFilter_CallsChainDoFilter() throws Exception {
        // Arrange
        BufferedReader reader = new BufferedReader(new StringReader("content"));
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getReader()).thenReturn(reader);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        secondFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        verify(mockFilterChain, times(1)).doFilter(mockRequest, mockResponse);
    }

    @Test
    void testDoFilter_WithMultilineBody_JoinsLines() throws Exception {
        // Arrange
        String requestBody = "line1\nline2\nline3";
        BufferedReader reader = new BufferedReader(new StringReader(requestBody));
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        when(mockRequest.getReader()).thenReturn(reader);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        secondFilter.doFilter(mockRequest, mockResponse, mockFilterChain);

        // Assert
        String output = stringWriter.toString();
        assertTrue(output.contains("line1"));
        assertTrue(output.contains("line2"));
        assertTrue(output.contains("line3"));
    }

    @Test
    void testSecondFilter_IsInstanceOfFilter() {
        assertTrue(secondFilter instanceof jakarta.servlet.Filter);
    }
}
