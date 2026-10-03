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
    void testDefaultConstructor_CreatesInstance() {
        assertNotNull(reservationList);
    }

    @Test
    void testDefaultConstructor_EmptyReservations() {
        assertNotNull(reservationList.getReservations());
        assertTrue(reservationList.getReservations().isEmpty());
    }

    @Test
    void testParameterizedConstructor_SetsReservations() {
        // Arrange
        List<Reservation> reservations = new ArrayList<>();
        reservations.add(new Reservation("01/01/2024", "01/15/2024"));

        // Act
        ReservationList list = new ReservationList(reservations);

        // Assert
        assertEquals(1, list.getReservations().size());
    }

    @Test
    void testAdd_SingleReservation_ListHasOneItem() {
        // Arrange
        Reservation reservation = new Reservation("01/01/2024", "01/15/2024");

        // Act
        reservationList.add(reservation);

        // Assert
        assertEquals(1, reservationList.getReservations().size());
    }

    @Test
    void testAdd_MultipleReservations_ListHasCorrectCount() {
        // Arrange
        Reservation r1 = new Reservation("01/01/2024", "01/15/2024");
        Reservation r2 = new Reservation("02/01/2024", "02/15/2024");
        Reservation r3 = new Reservation("03/01/2024", "03/15/2024");

        // Act
        reservationList.add(r1);
        reservationList.add(r2);
        reservationList.add(r3);

        // Assert
        assertEquals(3, reservationList.getReservations().size());
    }

    @Test
    void testAdd_ReservationIsRetrievable() {
        // Arrange
        Reservation reservation = new Reservation("05/01/2024", "05/31/2024");

        // Act
        reservationList.add(reservation);

        // Assert
        assertEquals("05/01/2024", reservationList.getReservations().get(0).getFromDate());
        assertEquals("05/31/2024", reservationList.getReservations().get(0).getToDate());
    }

    @Test
    void testGetReservations_ReturnsNonNull() {
        assertNotNull(reservationList.getReservations());
    }

    @Test
    void testParameterizedConstructor_WithEmptyList() {
        // Arrange
        List<Reservation> emptyList = new ArrayList<>();

        // Act
        ReservationList list = new ReservationList(emptyList);

        // Assert
        assertNotNull(list.getReservations());
        assertTrue(list.getReservations().isEmpty());
    }

    @Test
    void testParameterizedConstructor_WithMultipleReservations() {
        // Arrange
        List<Reservation> reservations = new ArrayList<>();
        reservations.add(new Reservation("01/01/2024", "01/15/2024"));
        reservations.add(new Reservation("02/01/2024", "02/15/2024"));

        // Act
        ReservationList list = new ReservationList(reservations);

        // Assert
        assertEquals(2, list.getReservations().size());
    }

    @Test
    void testAdd_NullReservation_AddsNull() {
        // Act
        reservationList.add(null);

        // Assert
        assertEquals(1, reservationList.getReservations().size());
        assertNull(reservationList.getReservations().get(0));
    }
}
