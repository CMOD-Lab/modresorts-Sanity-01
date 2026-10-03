package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UpperServletTest {

    private UpperServlet upperServlet;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @BeforeEach
    void setUp() {
        upperServlet = new UpperServlet();
    }

    @Test
    void testDoGet_WithInput_ReturnsUpperCase() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockRequest.getParameter("input")).thenReturn("hello");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        upperServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setContentType("text/html");
        assertTrue(stringWriter.toString().contains("HELLO"));
    }

    @Test
    void testDoGet_WithNullInput_ReturnsEmptyUpperCase() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockRequest.getParameter("input")).thenReturn(null);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        upperServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setContentType("text/html");
        String output = stringWriter.toString();
        assertTrue(output.contains("upper case input"));
    }

    @Test
    void testDoGet_WithHtmlSpecialChars_EncodesAmpersand() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockRequest.getParameter("input")).thenReturn("a&b");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        upperServlet.doGet(mockRequest, mockResponse);

        // Assert
        String output = stringWriter.toString();
        assertTrue(output.contains("&amp;"));
    }

    @Test
    void testDoGet_WithHtmlSpecialChars_EncodesLessThan() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockRequest.getParameter("input")).thenReturn("<script>");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        upperServlet.doGet(mockRequest, mockResponse);

        // Assert
        String output = stringWriter.toString();
        assertTrue(output.contains("&lt;"));
        assertTrue(output.contains("&gt;"));
    }

    @Test
    void testDoGet_WithHtmlSpecialChars_EncodesDoubleQuotes() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockRequest.getParameter("input")).thenReturn("say \"hello\"");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        upperServlet.doGet(mockRequest, mockResponse);

        // Assert
        String output = stringWriter.toString();
        assertTrue(output.contains("&quot;"));
    }

    @Test
    void testDoGet_WithHtmlSpecialChars_EncodesSingleQuote() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockRequest.getParameter("input")).thenReturn("it's");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        upperServlet.doGet(mockRequest, mockResponse);

        // Assert
        String output = stringWriter.toString();
        assertTrue(output.contains("&#x27;"));
    }

    @Test
    void testDoGet_SetsContentTypeToTextHtml() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockRequest.getParameter("input")).thenReturn("test");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        upperServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setContentType("text/html");
    }

    @Test
    void testDoGet_WithMixedCase_ReturnsAllUpperCase() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockRequest.getParameter("input")).thenReturn("HeLLo WoRLd");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        upperServlet.doGet(mockRequest, mockResponse);

        // Assert
        String output = stringWriter.toString();
        assertTrue(output.contains("HELLO WORLD"));
    }

    @Test
    void testDoGet_WithNumbers_ReturnsNumbers() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockRequest.getParameter("input")).thenReturn("12345");
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        upperServlet.doGet(mockRequest, mockResponse);

        // Assert
        String output = stringWriter.toString();
        assertTrue(output.contains("12345"));
    }

    @Test
    void testUpperServlet_IsHttpServlet() {
        assertTrue(upperServlet instanceof jakarta.servlet.http.HttpServlet);
    }
}
