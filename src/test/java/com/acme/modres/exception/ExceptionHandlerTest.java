package com.acme.modres.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.ServletException;
import java.util.logging.Logger;

public class ExceptionHandlerTest {

    private static final Logger logger = Logger.getLogger(ExceptionHandlerTest.class.getName());

    @Test
    void testHandleException_withNullException_throwsServletException() {
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, "Test error message", logger)
        );
    }

    @Test
    void testHandleException_withNullException_exceptionMessageMatches() {
        ServletException ex = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, "Custom error message", logger)
        );
        assertEquals("Custom error message", ex.getMessage());
    }

    @Test
    void testHandleException_withException_throwsServletException() {
        Exception cause = new RuntimeException("Root cause");
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, "Wrapper error", logger)
        );
    }

    @Test
    void testHandleException_withException_wrapsOriginalException() {
        Exception cause = new RuntimeException("Root cause");
        ServletException ex = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, "Wrapper error", logger)
        );
        assertNotNull(ex.getCause());
        assertEquals("Root cause", ex.getCause().getMessage());
    }

    @Test
    void testHandleException_withException_messageMatches() {
        Exception cause = new IllegalArgumentException("Bad argument");
        ServletException ex = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, "Error occurred", logger)
        );
        assertEquals("Error occurred", ex.getMessage());
    }

    @Test
    void testHandleException_withNullException_noWrappedCause() {
        ServletException ex = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, "No cause error", logger)
        );
        // When e is null, ServletException(errorMsg) is called - no cause
        assertNull(ex.getCause());
    }

    @Test
    void testHandleException_withIOException_throwsServletException() {
        Exception cause = new java.io.IOException("IO error");
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, "IO error occurred", logger)
        );
    }

    @Test
    void testHandleException_withEmptyMessage_throwsServletException() {
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, "", logger)
        );
    }

    @Test
    void testHandleException_withNullMessage_throwsServletException() {
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, null, logger)
        );
    }
}
