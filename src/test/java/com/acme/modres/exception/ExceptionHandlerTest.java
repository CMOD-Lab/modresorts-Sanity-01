package com.acme.modres.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.ServletException;
import java.util.logging.Logger;

public class ExceptionHandlerTest {

    private static final Logger logger = Logger.getLogger(ExceptionHandlerTest.class.getName());

    @Test
    void testHandleException_WithNullException_ThrowsServletException() {
        // Arrange
        String errorMsg = "Test error message";

        // Act & Assert
        assertThrows(ServletException.class, () -> {
            ExceptionHandler.handleException(null, errorMsg, logger);
        });
    }

    @Test
    void testHandleException_WithException_ThrowsServletException() {
        // Arrange
        Exception cause = new RuntimeException("Root cause");
        String errorMsg = "Test error message";

        // Act & Assert
        assertThrows(ServletException.class, () -> {
            ExceptionHandler.handleException(cause, errorMsg, logger);
        });
    }

    @Test
    void testHandleException_WithNullException_MessagePreserved() {
        // Arrange
        String errorMsg = "Specific error message";

        // Act & Assert
        ServletException thrown = assertThrows(ServletException.class, () -> {
            ExceptionHandler.handleException(null, errorMsg, logger);
        });
        assertEquals(errorMsg, thrown.getMessage());
    }

    @Test
    void testHandleException_WithException_MessagePreserved() {
        // Arrange
        Exception cause = new IllegalArgumentException("Bad argument");
        String errorMsg = "Wrapped error message";

        // Act & Assert
        ServletException thrown = assertThrows(ServletException.class, () -> {
            ExceptionHandler.handleException(cause, errorMsg, logger);
        });
        assertEquals(errorMsg, thrown.getMessage());
    }

    @Test
    void testHandleException_WithException_CausePreserved() {
        // Arrange
        Exception cause = new IllegalStateException("State error");
        String errorMsg = "Wrapped error";

        // Act & Assert
        ServletException thrown = assertThrows(ServletException.class, () -> {
            ExceptionHandler.handleException(cause, errorMsg, logger);
        });
        assertEquals(cause, thrown.getCause());
    }

    @Test
    void testHandleException_WithNullException_NoCause() {
        // Arrange
        String errorMsg = "No cause error";

        // Act & Assert
        ServletException thrown = assertThrows(ServletException.class, () -> {
            ExceptionHandler.handleException(null, errorMsg, logger);
        });
        assertNull(thrown.getCause());
    }

    @Test
    void testHandleException_WithEmptyMessage_ThrowsServletException() {
        // Arrange
        String errorMsg = "";

        // Act & Assert
        assertThrows(ServletException.class, () -> {
            ExceptionHandler.handleException(null, errorMsg, logger);
        });
    }

    @Test
    void testHandleException_WithIOException_ThrowsServletException() {
        // Arrange
        Exception cause = new java.io.IOException("IO error");
        String errorMsg = "IO error occurred";

        // Act & Assert
        assertThrows(ServletException.class, () -> {
            ExceptionHandler.handleException(cause, errorMsg, logger);
        });
    }
}
