package com.acme.modres.mbean.reservation;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.acme.modres.Constants;

public class DateChecker implements Runnable {
  private static final DateTimeFormatter RESERVATION_DATE_FORMATTER = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);

  ReservationCheckerData data;
  List<Reservation> reservations;

  public DateChecker(ReservationCheckerData data) {
    this.data = data;
    this.reservations = data.getReservationList().getReservations();
  }

  public void run() {
    boolean available = true;
    LocalDate selectedDate = Instant.ofEpochMilli(data.getSelectedDate().getTime()).atZone(ZoneOffset.UTC).toLocalDate();
    for (int i = 0; i < reservations.size(); i++) {
      Reservation reservation = reservations.get(i);

      LocalDate fromDate = LocalDate.parse(reservation.getFromDate(), RESERVATION_DATE_FORMATTER);
      LocalDate toDate = LocalDate.parse(reservation.getToDate(), RESERVATION_DATE_FORMATTER);
      if (selectedDate.isAfter(fromDate) && selectedDate.isBefore(toDate)) {
        available = false;
        break;
      }
    }
    data.setAvailablility(available);
  }
}
