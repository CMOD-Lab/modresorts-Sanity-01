package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class LogoutServletTest {

    private LogoutServlet logoutServlet;

    @Mock
    private HttpServletRequest mockRequest;

    @Mock
    private HttpServletResponse mockResponse;

    @Mock
    private HttpSession mockSession;

    @BeforeEach
    void setUp() {
        logoutServlet = new LogoutServlet();
    }

    @Test
    void testDoGet_WithActiveSession_InvalidatesSession() throws Exception {
        // Arrange
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockRequest.getCookies()).thenReturn(null);

        // Act
        logoutServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockSession).invalidate();
        verify(mockResponse).sendRedirect("login.jsp");
    }

    @Test
    void testDoGet_WithNoSession_DoesNotThrow() throws Exception {
        // Arrange
        when(mockRequest.getSession(false)).thenReturn(null);
        when(mockRequest.getCookies()).thenReturn(null);

        // Act & Assert
        assertDoesNotThrow(() -> logoutServlet.doGet(mockRequest, mockResponse));
        verify(mockResponse).sendRedirect("login.jsp");
    }

    @Test
    void testDoGet_WithCookies_ClearsCookies() throws Exception {
        // Arrange
        Cookie cookie1 = new Cookie("session", "abc123");
        Cookie cookie2 = new Cookie("user", "testUser");
        Cookie[] cookies = {cookie1, cookie2};

        when(mockRequest.getSession(false)).thenReturn(null);
        when(mockRequest.getCookies()).thenReturn(cookies);

        // Act
        logoutServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse, times(2)).addCookie(any(Cookie.class));
        verify(mockResponse).sendRedirect("login.jsp");
    }

    @Test
    void testDoGet_WithNullCookies_DoesNotThrow() throws Exception {
        // Arrange
        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockRequest.getCookies()).thenReturn(null);

        // Act & Assert
        assertDoesNotThrow(() -> logoutServlet.doGet(mockRequest, mockResponse));
    }

    @Test
    void testDoGet_RedirectsToLoginPage() throws Exception {
        // Arrange
        when(mockRequest.getSession(false)).thenReturn(null);
        when(mockRequest.getCookies()).thenReturn(null);

        // Act
        logoutServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockResponse).sendRedirect("login.jsp");
    }

    @Test
    void testDoGet_WithSessionAndCookies_InvalidatesSessionAndClearsCookies() throws Exception {
        // Arrange
        Cookie cookie = new Cookie("auth", "token123");
        Cookie[] cookies = {cookie};

        when(mockRequest.getSession(false)).thenReturn(mockSession);
        when(mockRequest.getCookies()).thenReturn(cookies);

        // Act
        logoutServlet.doGet(mockRequest, mockResponse);

        // Assert
        verify(mockSession).invalidate();
        verify(mockResponse, times(1)).addCookie(any(Cookie.class));
        verify(mockResponse).sendRedirect("login.jsp");
    }

    @Test
    void testLogoutServlet_IsHttpServlet() {
        assertTrue(logoutServlet instanceof jakarta.servlet.http.HttpServlet);
    }
}
