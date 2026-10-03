package com.acme.modres.mbean.reservation;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

import com.acme.modres.Constants;

public class ReservationCheckerData {
  private ReservationList reservations;
  private Date selectedDate;
  private LocalDate selectedLocalDate;
  private boolean available; // changed from Boolean to boolean

  public ReservationCheckerData(ReservationList reservations) {
    this.reservations = reservations;
    this.available = true;
  }

  public ReservationList getReservationList() {
    return reservations;
  }

  public Date getSelectedDate() {
    return selectedDate;
  }

  public LocalDate getSelectedLocalDate() {
    return selectedLocalDate;
  }

  public boolean setSelectedDate(String dateStr) {
    try {
      // Parse using modern java.time API
      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);
      selectedLocalDate = LocalDate.parse(dateStr, formatter);
      // Also set legacy Date for backward compatibility
      selectedDate = Date.from(selectedLocalDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
    } catch (DateTimeParseException e) {
      // Fallback to legacy parsing
      try {
        selectedDate = new SimpleDateFormat(Constants.DATA_FORMAT).parse(dateStr);
        selectedLocalDate = selectedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
      } catch (Exception ex) {
        return false;
      }
    }
    return true;
  }

  public boolean isAvailible() {
    return available;
  }

  public void setAvailablility(boolean available) { // fix parameter type
    this.available = available;
  }
}
