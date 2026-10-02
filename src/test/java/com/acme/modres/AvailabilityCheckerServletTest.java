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
import java.lang.reflect.Field;

import com.acme.modres.mbean.reservation.Reservation;
import com.acme.modres.mbean.reservation.ReservationCheckerData;
import com.acme.modres.mbean.reservation.ReservationList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AvailabilityCheckerServletTest {

    private AvailabilityCheckerServlet servlet;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new AvailabilityCheckerServlet();
        // Inject a ReservationCheckerData with an empty list to avoid file loading
        ReservationList reservationList = new ReservationList();
        ReservationCheckerData checkerData = new ReservationCheckerData(reservationList);
        Field field = AvailabilityCheckerServlet.class.getDeclaredField("reservationCheckerData");
        field.setAccessible(true);
        field.set(servlet, checkerData);
    }

    @Test
    void testServlet_isNotNull() {
        assertNotNull(servlet);
    }

    @Test
    void testDoGet_withInvalidDate_returns500() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("date")).thenReturn("invalid-date");

        servlet.doGet(request, response);

        verify(response).setStatus(500);
    }

    @Test
    void testDoGet_withValidDate_noReservations_returns200() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("date")).thenReturn("04/12/2024");

        servlet.doGet(request, response);

        verify(response).setStatus(200);
    }

    @Test
    void testDoGet_withValidDate_setsContentTypeJson() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("date")).thenReturn("04/12/2024");

        servlet.doGet(request, response);

        verify(response).setContentType("application/json");
    }

    @Test
    void testDoGet_withValidDate_setsCharacterEncoding() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("date")).thenReturn("04/12/2024");

        servlet.doGet(request, response);

        verify(response).setCharacterEncoding("UTF-8");
    }

    @Test
    void testDoGet_withValidDate_writesAvailabilityJson() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("date")).thenReturn("04/12/2024");

        servlet.doGet(request, response);

        pw.flush();
        assertTrue(sw.toString().contains("availability"));
    }

    @Test
    void testDoGet_withNullDate_returns500() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("date")).thenReturn(null);

        servlet.doGet(request, response);

        verify(response).setStatus(500);
    }

    @Test
    void testDoGet_withDateInsideReservation_returns201() throws Exception {
        // Set up a reservation from 04/10 to 04/15
        ReservationList reservationList = new ReservationList();
        reservationList.add(new Reservation("04/10/2024", "04/15/2024"));
        ReservationCheckerData checkerData = new ReservationCheckerData(reservationList);
        Field field = AvailabilityCheckerServlet.class.getDeclaredField("reservationCheckerData");
        field.setAccessible(true);
        field.set(servlet, checkerData);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("date")).thenReturn("04/12/2024");

        servlet.doGet(request, response);

        verify(response).setStatus(201);
    }

    @Test
    void testDoGet_withDateOutsideReservation_returns200() throws Exception {
        // Set up a reservation from 04/10 to 04/15
        ReservationList reservationList = new ReservationList();
        reservationList.add(new Reservation("04/10/2024", "04/15/2024"));
        ReservationCheckerData checkerData = new ReservationCheckerData(reservationList);
        Field field = AvailabilityCheckerServlet.class.getDeclaredField("reservationCheckerData");
        field.setAccessible(true);
        field.set(servlet, checkerData);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("date")).thenReturn("04/20/2024");

        servlet.doGet(request, response);

        verify(response).setStatus(200);
    }

    @Test
    void testDoPost_delegatesToDoGet() throws Exception {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        when(response.getWriter()).thenReturn(pw);
        when(request.getParameter("date")).thenReturn("04/12/2024");

        assertDoesNotThrow(() -> servlet.doPost(request, response));
    }

    @Test
    void testAvailabilityCheckerServlet_extendsHttpServlet() {
        assertTrue(servlet instanceof jakarta.servlet.http.HttpServlet);
    }
}
