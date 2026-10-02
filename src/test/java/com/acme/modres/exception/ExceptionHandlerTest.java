package com.acme.modres.exception;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import jakarta.servlet.ServletException;
import java.util.logging.Logger;

class ExceptionHandlerTest {

    private static final Logger logger = Logger.getLogger(ExceptionHandlerTest.class.getName());

    @Test
    void testHandleException_withNullException_throwsServletException() {
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, "Test error message", logger)
        );
    }

    @Test
    void testHandleException_withNonNullException_throwsServletException() {
        Exception cause = new RuntimeException("cause");
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, "Test error message", logger)
        );
    }

    @Test
    void testHandleException_withNullException_messageInServletException() {
        String errorMsg = "Custom error message";
        ServletException ex = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, errorMsg, logger)
        );
        assertEquals(errorMsg, ex.getMessage());
    }

    @Test
    void testHandleException_withNonNullException_messageInServletException() {
        String errorMsg = "Custom error message";
        Exception cause = new RuntimeException("root cause");
        ServletException ex = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, errorMsg, logger)
        );
        assertEquals(errorMsg, ex.getMessage());
    }

    @Test
    void testHandleException_withNonNullException_causeIsSet() {
        Exception cause = new RuntimeException("root cause");
        ServletException ex = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(cause, "error", logger)
        );
        assertNotNull(ex.getCause());
        assertEquals(cause, ex.getCause());
    }

    @Test
    void testHandleException_withNullException_causeIsNull() {
        ServletException ex = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, "error", logger)
        );
        assertNull(ex.getCause());
    }

    @Test
    void testHandleException_withIOException_throwsServletException() {
        java.io.IOException ioEx = new java.io.IOException("IO error");
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(ioEx, "IO error occurred", logger)
        );
    }

    @Test
    void testHandleException_withEmptyMessage_throwsServletException() {
        assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, "", logger)
        );
    }

    @Test
    void testHandleException_withNullException_emptyMessage() {
        ServletException ex = assertThrows(ServletException.class, () ->
            ExceptionHandler.handleException(null, "", logger)
        );
        assertEquals("", ex.getMessage());
    }
}
