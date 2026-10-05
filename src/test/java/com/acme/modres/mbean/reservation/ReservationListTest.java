package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class ReservationListTest {

    private ReservationList reservationList;

    @BeforeEach
    void setUp() {
        reservationList = new ReservationList();
    }

    @Test
    void testDefaultConstructor_createsInstance() {
        assertNotNull(reservationList);
    }

    @Test
    void testDefaultConstructor_emptyList() {
        assertNotNull(reservationList.getReservations());
        assertTrue(reservationList.getReservations().isEmpty());
    }

    @Test
    void testParameterizedConstructor_withList() {
        List<Reservation> reservations = new ArrayList<>();
        reservations.add(new Reservation("01/01/2024", "01/15/2024"));
        ReservationList rl = new ReservationList(reservations);
        assertEquals(1, rl.getReservations().size());
    }

    @Test
    void testParameterizedConstructor_withEmptyList() {
        ReservationList rl = new ReservationList(new ArrayList<>());
        assertTrue(rl.getReservations().isEmpty());
    }

    @Test
    void testAdd_singleReservation() {
        Reservation r = new Reservation("01/01/2024", "01/15/2024");
        reservationList.add(r);
        assertEquals(1, reservationList.getReservations().size());
    }

    @Test
    void testAdd_multipleReservations() {
        reservationList.add(new Reservation("01/01/2024", "01/15/2024"));
        reservationList.add(new Reservation("02/01/2024", "02/15/2024"));
        reservationList.add(new Reservation("03/01/2024", "03/15/2024"));
        assertEquals(3, reservationList.getReservations().size());
    }

    @Test
    void testAdd_preservesOrder() {
        Reservation r1 = new Reservation("01/01/2024", "01/15/2024");
        Reservation r2 = new Reservation("02/01/2024", "02/15/2024");
        reservationList.add(r1);
        reservationList.add(r2);
        assertEquals("01/01/2024", reservationList.getReservations().get(0).getFromDate());
        assertEquals("02/01/2024", reservationList.getReservations().get(1).getFromDate());
    }

    @Test
    void testGetReservations_returnsCorrectList() {
        Reservation r = new Reservation("05/01/2024", "05/10/2024");
        reservationList.add(r);
        List<Reservation> list = reservationList.getReservations();
        assertNotNull(list);
        assertEquals(1, list.size());
        assertEquals("05/01/2024", list.get(0).getFromDate());
    }

    @Test
    void testParameterizedConstructor_preservesReservationData() {
        List<Reservation> reservations = new ArrayList<>();
        reservations.add(new Reservation("07/01/2024", "07/10/2024"));
        reservations.add(new Reservation("08/01/2024", "08/10/2024"));
        ReservationList rl = new ReservationList(reservations);
        assertEquals("07/01/2024", rl.getReservations().get(0).getFromDate());
        assertEquals("08/01/2024", rl.getReservations().get(1).getFromDate());
    }
}
