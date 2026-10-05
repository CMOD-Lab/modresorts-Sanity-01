package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;

@ExtendWith(MockitoExtension.class)
public class UpperServletTest {

    private UpperServlet upperServlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        upperServlet = new UpperServlet();
    }

    @Test
    void testConstructor_createsInstance() {
        assertNotNull(upperServlet);
    }

    @Test
    void testDoGet_setsContentType() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(request.getParameter("input")).thenReturn("hello");
        when(response.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(request, response);

        verify(response).setContentType("text/html");
    }

    @Test
    void testDoGet_convertsToUpperCase() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(request.getParameter("input")).thenReturn("hello");
        when(response.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(request, response);

        assertTrue(stringWriter.toString().contains("HELLO"));
    }

    @Test
    void testDoGet_withNullInput_usesEmptyString() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(request.getParameter("input")).thenReturn(null);
        when(response.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(request, response);

        assertNotNull(stringWriter.toString());
        assertTrue(stringWriter.toString().contains("upper case input"));
    }

    @Test
    void testDoGet_withSpecialHtmlChars_encodesCorrectly() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(request.getParameter("input")).thenReturn("<script>");
        when(response.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(request, response);

        String output = stringWriter.toString();
        assertFalse(output.contains("<SCRIPT>"));
        assertTrue(output.contains("&lt;") || output.contains("&gt;"));
    }

    @Test
    void testEncodeHtml_withAmpersand() throws Exception {
        Method encodeHtml = UpperServlet.class.getDeclaredMethod("encodeHtml", String.class);
        encodeHtml.setAccessible(true);
        String result = (String) encodeHtml.invoke(upperServlet, "A&B");
        assertEquals("A&amp;B", result);
    }

    @Test
    void testEncodeHtml_withLessThan() throws Exception {
        Method encodeHtml = UpperServlet.class.getDeclaredMethod("encodeHtml", String.class);
        encodeHtml.setAccessible(true);
        String result = (String) encodeHtml.invoke(upperServlet, "A<B");
        assertEquals("A&lt;B", result);
    }

    @Test
    void testEncodeHtml_withGreaterThan() throws Exception {
        Method encodeHtml = UpperServlet.class.getDeclaredMethod("encodeHtml", String.class);
        encodeHtml.setAccessible(true);
        String result = (String) encodeHtml.invoke(upperServlet, "A>B");
        assertEquals("A&gt;B", result);
    }

    @Test
    void testEncodeHtml_withDoubleQuote() throws Exception {
        Method encodeHtml = UpperServlet.class.getDeclaredMethod("encodeHtml", String.class);
        encodeHtml.setAccessible(true);
        String result = (String) encodeHtml.invoke(upperServlet, "A\"B");
        assertEquals("A&quot;B", result);
    }

    @Test
    void testEncodeHtml_withSingleQuote() throws Exception {
        Method encodeHtml = UpperServlet.class.getDeclaredMethod("encodeHtml", String.class);
        encodeHtml.setAccessible(true);
        String result = (String) encodeHtml.invoke(upperServlet, "A'B");
        assertEquals("A&#x27;B", result);
    }

    @Test
    void testEncodeHtml_withNull_returnsEmptyString() throws Exception {
        Method encodeHtml = UpperServlet.class.getDeclaredMethod("encodeHtml", String.class);
        encodeHtml.setAccessible(true);
        String result = (String) encodeHtml.invoke(upperServlet, (Object) null);
        assertEquals("", result);
    }

    @Test
    void testEncodeHtml_withNoSpecialChars_returnsUnchanged() throws Exception {
        Method encodeHtml = UpperServlet.class.getDeclaredMethod("encodeHtml", String.class);
        encodeHtml.setAccessible(true);
        String result = (String) encodeHtml.invoke(upperServlet, "HELLO WORLD");
        assertEquals("HELLO WORLD", result);
    }

    @Test
    void testDoGet_withMixedCase_convertsToUpperCase() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(request.getParameter("input")).thenReturn("HeLLo WoRLd");
        when(response.getWriter()).thenReturn(printWriter);

        upperServlet.doGet(request, response);

        assertTrue(stringWriter.toString().contains("HELLO WORLD"));
    }
}
