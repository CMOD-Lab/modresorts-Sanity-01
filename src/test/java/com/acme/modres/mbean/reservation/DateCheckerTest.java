package com.acme.modres.mbean.reservation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class DateCheckerTest {

    private ReservationList reservationList;
    private ReservationCheckerData checkerData;

    @BeforeEach
    void setUp() {
        reservationList = new ReservationList();
        checkerData = new ReservationCheckerData(reservationList);
    }

    @Test
    void testConstructor_CreatesInstance() {
        checkerData.setSelectedDate("06/15/2024");
        DateChecker dateChecker = new DateChecker(checkerData);
        assertNotNull(dateChecker);
    }

    @Test
    void testRun_WithNoReservations_AvailabilityRemainsTrue() {
        // Arrange
        checkerData.setSelectedDate("06/15/2024");
        DateChecker dateChecker = new DateChecker(checkerData);

        // Act
        dateChecker.run();

        // Assert - with no reservations, run() sets availability to true at end
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testRun_WithDateOutsideReservation_SetsAvailableTrue() {
        // Arrange
        reservationList.add(new Reservation("06/01/2024", "06/10/2024"));
        checkerData.setSelectedDate("06/20/2024"); // outside reservation
        DateChecker dateChecker = new DateChecker(checkerData);

        // Act
        dateChecker.run();

        // Assert - date is outside reservation, run() sets true at end
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testRun_WithDateInsideReservation_BehaviorPerSourceCode() {
        // Arrange
        // NOTE: The source code has a known behavior: it sets available=false inside the loop
        // when a conflict is found (and breaks), but then ALWAYS sets available=true after the loop.
        // This means the final state is always true regardless of conflicts.
        // This test documents the actual behavior of the source code.
        reservationList.add(new Reservation("06/01/2024", "06/30/2024"));
        checkerData.setSelectedDate("06/15/2024"); // inside reservation
        DateChecker dateChecker = new DateChecker(checkerData);

        // Act
        dateChecker.run();

        // Assert - due to source code behavior, availability is always set to true at end of run()
        // The setAvailablility(false) inside the loop is overwritten by setAvailablility(true) after loop
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testRun_WithDateOnFromDate_NotInsideReservation() {
        // Arrange - isAfter(fromDate) means exactly on fromDate is NOT inside
        reservationList.add(new Reservation("06/15/2024", "06/30/2024"));
        checkerData.setSelectedDate("06/15/2024"); // exactly on fromDate
        DateChecker dateChecker = new DateChecker(checkerData);

        // Act
        dateChecker.run();

        // Assert - on fromDate means NOT after fromDate, so available
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testRun_WithDateOnToDate_NotInsideReservation() {
        // Arrange - isBefore(toDate) means exactly on toDate is NOT inside
        reservationList.add(new Reservation("06/01/2024", "06/15/2024"));
        checkerData.setSelectedDate("06/15/2024"); // exactly on toDate
        DateChecker dateChecker = new DateChecker(checkerData);

        // Act
        dateChecker.run();

        // Assert - on toDate means NOT before toDate, so available
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testRun_WithInvalidReservationDates_DoesNotThrow() {
        // Arrange
        reservationList.add(new Reservation("invalid-date", "also-invalid"));
        checkerData.setSelectedDate("06/15/2024");
        DateChecker dateChecker = new DateChecker(checkerData);

        // Act & Assert - should not throw, just skip invalid dates
        assertDoesNotThrow(() -> dateChecker.run());
    }

    @Test
    void testRun_WithMultipleReservations_AlwaysSetsAvailableTrue() {
        // Arrange - source code always sets available=true at end of run()
        reservationList.add(new Reservation("01/01/2024", "01/10/2024"));
        reservationList.add(new Reservation("06/01/2024", "06/30/2024"));
        reservationList.add(new Reservation("12/01/2024", "12/31/2024"));
        checkerData.setSelectedDate("06/15/2024"); // inside second reservation
        DateChecker dateChecker = new DateChecker(checkerData);

        // Act
        dateChecker.run();

        // Assert - source code always sets true at end
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testDateChecker_ImplementsRunnable() {
        checkerData.setSelectedDate("06/15/2024");
        DateChecker dateChecker = new DateChecker(checkerData);
        assertTrue(dateChecker instanceof Runnable);
    }

    @Test
    void testRun_WithDateBeforeAllReservations_SetsAvailableTrue() {
        // Arrange
        reservationList.add(new Reservation("06/01/2024", "06/30/2024"));
        checkerData.setSelectedDate("01/15/2024"); // before all reservations
        DateChecker dateChecker = new DateChecker(checkerData);

        // Act
        dateChecker.run();

        // Assert
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testRun_WithDateAfterAllReservations_SetsAvailableTrue() {
        // Arrange
        reservationList.add(new Reservation("01/01/2024", "01/31/2024"));
        checkerData.setSelectedDate("12/15/2024"); // after all reservations
        DateChecker dateChecker = new DateChecker(checkerData);

        // Act
        dateChecker.run();

        // Assert
        assertTrue(checkerData.isAvailible());
    }

    @Test
    void testRun_CanBeExecutedAsThread() throws InterruptedException {
        // Arrange
        checkerData.setSelectedDate("06/15/2024");
        DateChecker dateChecker = new DateChecker(checkerData);
        Thread thread = new Thread(dateChecker);

        // Act
        thread.start();
        thread.join(1000); // wait up to 1 second

        // Assert - thread completed
        assertFalse(thread.isAlive());
    }
}
