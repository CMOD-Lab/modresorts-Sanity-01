package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class ReservationTest {

    @Test
    void testDefaultConstructor_CreatesInstance() {
        Reservation reservation = new Reservation();
        assertNotNull(reservation);
    }

    @Test
    void testParameterizedConstructor_SetsFromDate() {
        Reservation reservation = new Reservation("01/01/2024", "01/15/2024");
        assertEquals("01/01/2024", reservation.getFromDate());
    }

    @Test
    void testParameterizedConstructor_SetsToDate() {
        Reservation reservation = new Reservation("01/01/2024", "01/15/2024");
        assertEquals("01/15/2024", reservation.getToDate());
    }

    @Test
    void testGetFromDate_ReturnsCorrectDate() {
        Reservation reservation = new Reservation("03/15/2024", "03/20/2024");
        assertEquals("03/15/2024", reservation.getFromDate());
    }

    @Test
    void testGetToDate_ReturnsCorrectDate() {
        Reservation reservation = new Reservation("03/15/2024", "03/20/2024");
        assertEquals("03/20/2024", reservation.getToDate());
    }

    @Test
    void testSetFromDate_UpdatesFromDate() {
        Reservation reservation = new Reservation();
        reservation.setFromDate("05/01/2024");
        assertEquals("05/01/2024", reservation.getFromDate());
    }

    @Test
    void testSetToDate_UpdatesToDate() {
        Reservation reservation = new Reservation();
        reservation.setToDate("05/31/2024");
        assertEquals("05/31/2024", reservation.getToDate());
    }

    @Test
    void testDefaultConstructor_FromDateIsNull() {
        Reservation reservation = new Reservation();
        assertNull(reservation.getFromDate());
    }

    @Test
    void testDefaultConstructor_ToDateIsNull() {
        Reservation reservation = new Reservation();
        assertNull(reservation.getToDate());
    }

    @Test
    void testSetFromDate_WithNull_SetsNull() {
        Reservation reservation = new Reservation("01/01/2024", "01/15/2024");
        reservation.setFromDate(null);
        assertNull(reservation.getFromDate());
    }

    @Test
    void testSetToDate_WithNull_SetsNull() {
        Reservation reservation = new Reservation("01/01/2024", "01/15/2024");
        reservation.setToDate(null);
        assertNull(reservation.getToDate());
    }

    @Test
    void testParameterizedConstructor_WithNullDates() {
        Reservation reservation = new Reservation(null, null);
        assertNull(reservation.getFromDate());
        assertNull(reservation.getToDate());
    }

    @Test
    void testSetFromDate_OverwritesPreviousValue() {
        Reservation reservation = new Reservation("01/01/2024", "01/15/2024");
        reservation.setFromDate("06/01/2024");
        assertEquals("06/01/2024", reservation.getFromDate());
    }

    @Test
    void testSetToDate_OverwritesPreviousValue() {
        Reservation reservation = new Reservation("01/01/2024", "01/15/2024");
        reservation.setToDate("06/30/2024");
        assertEquals("06/30/2024", reservation.getToDate());
    }
}
