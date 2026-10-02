package com.acme.modres;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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
class WelcomeServletTest {

    private WelcomeServlet servlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        servlet = new WelcomeServlet();
    }

    @Test
    void testServlet_isNotNull() {
        assertNotNull(servlet);
    }

    @Test
    void testDoGet_setsContentTypePlainText() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);

        servlet.doGet(request, response);

        verify(response).setContentType("text/plain");
    }

    @Test
    void testDoGet_writesEnjoyMessage() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);

        servlet.doGet(request, response);

        pw.flush();
        assertTrue(sw.toString().contains("Enjoy!"));
    }

    @Test
    void testDoGet_doesNotThrow() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);

        assertDoesNotThrow(() -> servlet.doGet(request, response));
    }

    @Test
    void testWelcomeServlet_extendsHttpServlet() {
        assertTrue(servlet instanceof jakarta.servlet.http.HttpServlet);
    }
}
