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
import jakarta.servlet.http.HttpSession;

@ExtendWith(MockitoExtension.class)
public class LogoutServletTest {

    private LogoutServlet logoutServlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HttpSession session;

    @BeforeEach
    void setUp() {
        logoutServlet = new LogoutServlet();
    }

    @Test
    void testConstructor_createsInstance() {
        assertNotNull(logoutServlet);
    }

    @Test
    void testDoGet_withActiveSession_invalidatesSession() throws Exception {
        when(request.getSession(false)).thenReturn(session);

        logoutServlet.doGet(request, response);

        verify(session).invalidate();
    }

    @Test
    void testDoGet_withNoSession_doesNotThrow() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        assertDoesNotThrow(() -> logoutServlet.doGet(request, response));
    }

    @Test
    void testDoGet_redirectsToLoginPage() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        logoutServlet.doGet(request, response);

        verify(response).sendRedirect("login.jsp");
    }

    @Test
    void testDoGet_withActiveSession_redirectsToLoginPage() throws Exception {
        when(request.getSession(false)).thenReturn(session);

        logoutServlet.doGet(request, response);

        verify(response).sendRedirect("login.jsp");
    }

    @Test
    void testDoGet_withNullSession_doesNotCallInvalidate() throws Exception {
        when(request.getSession(false)).thenReturn(null);

        logoutServlet.doGet(request, response);

        verify(session, never()).invalidate();
    }

    @Test
    void testDoGet_withSessionThrowingException_stillRedirects() throws Exception {
        when(request.getSession(false)).thenReturn(session);
        doThrow(new RuntimeException("Session error")).when(session).invalidate();

        // Should not throw, just log the error
        assertDoesNotThrow(() -> logoutServlet.doGet(request, response));
    }
}
