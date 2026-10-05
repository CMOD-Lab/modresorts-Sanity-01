package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class ReservationTest {

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
    void testDefaultConstructor_fieldsAreNull() {
        assertNull(reservation.getFromDate());
        assertNull(reservation.getToDate());
    }

    @Test
    void testParameterizedConstructor_setsFromDate() {
        Reservation r = new Reservation("01/01/2024", "01/15/2024");
        assertEquals("01/01/2024", r.getFromDate());
    }

    @Test
    void testParameterizedConstructor_setsToDate() {
        Reservation r = new Reservation("01/01/2024", "01/15/2024");
        assertEquals("01/15/2024", r.getToDate());
    }

    @Test
    void testParameterizedConstructor_withNullDates() {
        Reservation r = new Reservation(null, null);
        assertNull(r.getFromDate());
        assertNull(r.getToDate());
    }

    @Test
    void testSetFromDate_andGetFromDate() {
        reservation.setFromDate("03/10/2024");
        assertEquals("03/10/2024", reservation.getFromDate());
    }

    @Test
    void testSetToDate_andGetToDate() {
        reservation.setToDate("03/20/2024");
        assertEquals("03/20/2024", reservation.getToDate());
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
    void testSetFromDate_overwritesPreviousValue() {
        reservation.setFromDate("01/01/2024");
        reservation.setFromDate("06/01/2024");
        assertEquals("06/01/2024", reservation.getFromDate());
    }

    @Test
    void testSetToDate_overwritesPreviousValue() {
        reservation.setToDate("01/31/2024");
        reservation.setToDate("06/30/2024");
        assertEquals("06/30/2024", reservation.getToDate());
    }

    @Test
    void testParameterizedConstructor_withEmptyStrings() {
        Reservation r = new Reservation("", "");
        assertEquals("", r.getFromDate());
        assertEquals("", r.getToDate());
    }
}
