package com.acme.modres;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.io.IOException;

/**
 * LogoutServlet - handles user logout using standard Jakarta EE session invalidation.
 * Previously used com.ibm.websphere.security.WSSecurityHelper.revokeSSOCookies()
 * which is WebSphere-specific. Replaced with standard HttpSession.invalidate()
 * for Java 17 / Jakarta EE 10 compatibility.
 */
@WebServlet({ "/logout" })
public class LogoutServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  @Override
  protected void doGet(HttpServletRequest request,
      HttpServletResponse response) throws IOException {

    try {
      // Invalidate the session using standard Jakarta EE API
      // Previously: WSSecurityHelper.revokeSSOCookies(request, response)
      HttpSession session = request.getSession(false);
      if (session != null) {
        session.invalidate();
      }
    } catch (Exception e) {
      System.err.println("[ERROR] Error logging out");
      e.printStackTrace();
    }

    response.sendRedirect("login.jsp");
  }
}
