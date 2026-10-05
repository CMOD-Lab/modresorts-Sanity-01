package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class ReservationCheckerDataTest {

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
        boolean result = checkerData.setSelectedDate("01/15/2024");
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
    void testSetSelectedDate_validDate_setsSelectedDate() {
        checkerData.setSelectedDate("06/15/2024");
        assertNotNull(checkerData.getSelectedDate());
    }

    @Test
    void testGetSelectedDate_beforeSet_isNull() {
        assertNull(checkerData.getSelectedDate());
    }

    @Test
    void testIsAvailible_defaultTrue() {
        assertTrue(checkerData.isAvailible());
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
    void testGetReservationList_returnsCorrectList() {
        List<Reservation> reservations = new ArrayList<>();
        reservations.add(new Reservation("01/01/2024", "01/15/2024"));
        ReservationList rl = new ReservationList(reservations);
        ReservationCheckerData data = new ReservationCheckerData(rl);
        assertSame(rl, data.getReservationList());
        assertEquals(1, data.getReservationList().getReservations().size());
    }

    @Test
    void testSetSelectedDate_differentFormats_invalidReturnsfalse() {
        // "15/01/2024" is parsed as month=15 which is invalid but SimpleDateFormat is lenient by default
        // "2024-01-15" is not in MM/dd/yyyy format so it fails
        assertFalse(checkerData.setSelectedDate("2024-01-15"));
    }

    @Test
    void testSetSelectedDate_validFormat_correctDate() {
        checkerData.setSelectedDate("12/25/2024");
        assertNotNull(checkerData.getSelectedDate());
    }
}
