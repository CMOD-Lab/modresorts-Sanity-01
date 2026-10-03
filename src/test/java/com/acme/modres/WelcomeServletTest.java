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
public class WelcomeServletTest {

    private WelcomeServlet welcomeServlet;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @BeforeEach
    void setUp() {
        welcomeServlet = new WelcomeServlet();
    }

    @Test
    void testDoGet_SetsContentTypeToTextPlain() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        welcomeServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).setContentType("text/plain");
    }

    @Test
    void testDoGet_WritesEnjoyMessage() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        welcomeServlet.doGet(mockRequest, mockResponse);

        // Assert
        assertTrue(stringWriter.toString().contains("Enjoy!"));
    }

    @Test
    void testDoGet_ResponseWriterCalled() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act
        welcomeServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).getWriter();
    }

    @Test
    void testWelcomeServlet_IsHttpServlet() {
        assertTrue(welcomeServlet instanceof jakarta.servlet.http.HttpServlet);
    }

    @Test
    void testDoGet_DoesNotThrow() throws Exception {
        // Arrange
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(mockResponse.getWriter()).thenReturn(printWriter);

        // Act & Assert
        assertDoesNotThrow(() -> welcomeServlet.doGet(mockRequest, mockResponse));
    }
}
