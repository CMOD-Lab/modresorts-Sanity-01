package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

class ReservationCheckerDataTest {

    private ReservationList reservationList;
    private ReservationCheckerData checkerData;

    @BeforeEach
    void setUp() {
        reservationList = new ReservationList();
        checkerData = new ReservationCheckerData(reservationList);
    }

    @Test
    void testConstructor_createsInstance() {
        assertNotNull(checkerData);
    }

    @Test
    void testConstructor_defaultAvailabilityIsTrue() {
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testConstructor_setsReservationList() {
        assertNotNull(checkerData.getReservationList());
        assertSame(reservationList, checkerData.getReservationList());
    }

    @Test
    void testSetSelectedDate_validDate_returnsTrue() {
        boolean result = checkerData.setSelectedDate("04/12/2024");
        assertTrue(result);
    }

    @Test
    void testSetSelectedDate_invalidDate_returnsFalse() {
        boolean result = checkerData.setSelectedDate("not-a-date");
        assertFalse(result);
    }

    @Test
    void testSetSelectedDate_nullDate_returnsFalse() {
        boolean result = checkerData.setSelectedDate(null);
        assertFalse(result);
    }

    @Test
    void testSetSelectedDate_emptyString_returnsFalse() {
        boolean result = checkerData.setSelectedDate("");
        assertFalse(result);
    }

    @Test
    void testGetSelectedDate_afterValidSet_notNull() {
        checkerData.setSelectedDate("04/12/2024");
        assertNotNull(checkerData.getSelectedDate());
    }

    @Test
    void testGetSelectedDate_beforeSet_isNull() {
        assertNull(checkerData.getSelectedDate());
    }

    @Test
    void testSetAvailablility_toFalse() {
        checkerData.setAvailablility(false);
        assertFalse(checkerData.isAvailible());
    }

    @Test
    void testSetAvailablility_toTrue() {
        checkerData.setAvailablility(false);
        checkerData.setAvailablility(true);
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testIsAvailible_defaultTrue() {
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testGetReservationList_withReservations() {
        List<Reservation> reservations = new ArrayList<>();
        reservations.add(new Reservation("04/10/2024", "04/15/2024"));
        ReservationList rl = new ReservationList(reservations);
        ReservationCheckerData data = new ReservationCheckerData(rl);
        assertNotNull(data.getReservationList());
        assertEquals(1, data.getReservationList().getReservations().size());
    }

    @Test
    void testSetSelectedDate_validDateFormat_setsCorrectly() {
        boolean result = checkerData.setSelectedDate("01/01/2024");
        assertTrue(result);
        Date selectedDate = checkerData.getSelectedDate();
        assertNotNull(selectedDate);
    }

    @Test
    void testSetSelectedDate_wrongFormat_returnsFalse() {
        boolean result = checkerData.setSelectedDate("2024-01-01");
        assertFalse(result);
    }
}
