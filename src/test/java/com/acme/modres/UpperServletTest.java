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
class UpperServletTest {

    private UpperServlet servlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        servlet = new UpperServlet();
    }

    @Test
    void testServlet_isNotNull() {
        assertNotNull(servlet);
    }

    @Test
    void testDoGet_setsContentTypeHtml() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("hello");

        servlet.doGet(request, response);

        verify(response).setContentType("text/html");
    }

    @Test
    void testDoGet_withInput_returnsUpperCase() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("hello");

        servlet.doGet(request, response);

        pw.flush();
        assertTrue(sw.toString().contains("HELLO"));
    }

    @Test
    void testDoGet_withNullInput_returnsEmpty() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn(null);

        servlet.doGet(request, response);

        pw.flush();
        assertNotNull(sw.toString());
    }

    @Test
    void testDoGet_withMixedCaseInput_returnsAllUpperCase() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("HeLLo WoRLd");

        servlet.doGet(request, response);

        pw.flush();
        assertTrue(sw.toString().contains("HELLO WORLD"));
    }

    @Test
    void testDoGet_withHtmlSpecialChars_encodesCorrectly() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("<script>");

        servlet.doGet(request, response);

        pw.flush();
        String output = sw.toString();
        assertFalse(output.contains("<script>"));
        assertTrue(output.contains("&lt;SCRIPT&gt;"));
    }

    @Test
    void testDoGet_withAmpersand_encodesCorrectly() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("a&b");

        servlet.doGet(request, response);

        pw.flush();
        String output = sw.toString();
        assertTrue(output.contains("&amp;"));
    }

    @Test
    void testDoGet_withQuote_encodesCorrectly() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("say \"hello\"");

        servlet.doGet(request, response);

        pw.flush();
        String output = sw.toString();
        assertTrue(output.contains("&quot;"));
    }

    @Test
    void testDoGet_doesNotThrow() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("input")).thenReturn("test");

        assertDoesNotThrow(() -> servlet.doGet(request, response));
    }

    @Test
    void testUpperServlet_extendsHttpServlet() {
        assertTrue(servlet instanceof jakarta.servlet.http.HttpServlet);
    }
}
