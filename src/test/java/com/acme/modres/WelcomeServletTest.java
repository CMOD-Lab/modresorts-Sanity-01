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

@ExtendWith(MockitoExtension.class)
public class WelcomeServletTest {

    private WelcomeServlet welcomeServlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() {
        welcomeServlet = new WelcomeServlet();
    }

    @Test
    void testConstructor_createsInstance() {
        assertNotNull(welcomeServlet);
    }

    @Test
    void testDoGet_setsContentType() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        welcomeServlet.doGet(request, response);

        verify(response).setContentType("text/plain");
    }

    @Test
    void testDoGet_writesEnjoyMessage() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        welcomeServlet.doGet(request, response);

        assertTrue(stringWriter.toString().contains("Enjoy!"));
    }

    @Test
    void testDoGet_doesNotThrow() throws Exception {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        assertDoesNotThrow(() -> welcomeServlet.doGet(request, response));
    }

    @Test
    void testSerialVersionUID_isCorrect() throws Exception {
        // Verify the class has a serialVersionUID
        java.lang.reflect.Field field = WelcomeServlet.class.getDeclaredField("serialVersionUID");
        field.setAccessible(true);
        assertEquals(1L, field.getLong(null));
    }
}
