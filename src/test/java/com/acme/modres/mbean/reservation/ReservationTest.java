package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReservationTest {

    private Reservation reservation;

    @BeforeEach
    void setUp() {
        reservation = new Reservation();
    }

    @Test
    void testDefaultConstructor_createsInstance() {
        assertNotNull(reservation);
    }

    @Test
    void testDefaultConstructor_nullFields() {
        assertNull(reservation.getFromDate());
        assertNull(reservation.getToDate());
    }

    @Test
    void testParameterizedConstructor_setsFields() {
        Reservation r = new Reservation("04/10/2024", "04/15/2024");
        assertEquals("04/10/2024", r.getFromDate());
        assertEquals("04/15/2024", r.getToDate());
    }

    @Test
    void testSetFromDate_andGetFromDate() {
        reservation.setFromDate("01/01/2024");
        assertEquals("01/01/2024", reservation.getFromDate());
    }

    @Test
    void testSetToDate_andGetToDate() {
        reservation.setToDate("12/31/2024");
        assertEquals("12/31/2024", reservation.getToDate());
    }

    @Test
    void testSetFromDate_withNull() {
        reservation.setFromDate(null);
        assertNull(reservation.getFromDate());
    }

    @Test
    void testSetToDate_withNull() {
        reservation.setToDate(null);
        assertNull(reservation.getToDate());
    }

    @Test
    void testSetFromDate_withEmptyString() {
        reservation.setFromDate("");
        assertEquals("", reservation.getFromDate());
    }

    @Test
    void testSetToDate_withEmptyString() {
        reservation.setToDate("");
        assertEquals("", reservation.getToDate());
    }

    @Test
    void testParameterizedConstructor_withNullValues() {
        Reservation r = new Reservation(null, null);
        assertNull(r.getFromDate());
        assertNull(r.getToDate());
    }

    @Test
    void testSetFromDate_overwritesPreviousValue() {
        reservation.setFromDate("01/01/2024");
        reservation.setFromDate("06/15/2024");
        assertEquals("06/15/2024", reservation.getFromDate());
    }

    @Test
    void testSetToDate_overwritesPreviousValue() {
        reservation.setToDate("12/31/2024");
        reservation.setToDate("07/20/2024");
        assertEquals("07/20/2024", reservation.getToDate());
    }
}
