package com.acme.modres;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

import com.azure.messaging.servicebus.ServiceBusClientBuilder;
import com.azure.messaging.servicebus.ServiceBusMessage;
import com.azure.messaging.servicebus.ServiceBusSenderClient;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

import com.acme.modres.mbean.IOUtils;
import com.acme.modres.mbean.reservation.Reservation;
import com.acme.modres.mbean.reservation.ReservationCheckerData;

@WebServlet({ "/resorts/availability" })
public class AvailabilityCheckerServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;
  private static final DateTimeFormatter RESERVATION_DATE_FORMATTER = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT);

  private static final Logger logger = Logger.getLogger(AvailabilityCheckerServlet.class.getName());

  private static InitialContext context;

  private static final String AZURE_STORAGE_CONNECTION_STRING_ENV = "AZURE_STORAGE_CONNECTION_STRING";
  private static final String AZURE_STORAGE_CONTAINER_ENV = "AZURE_STORAGE_CONTAINER_NAME";
  private static final String DEFAULT_AZURE_STORAGE_CONTAINER = "modresorts-exports";
  private static final String AZURE_SERVICE_BUS_CONNECTION_STRING_ENV = "AZURE_SERVICE_BUS_CONNECTION_STRING";
  private static final String AZURE_SERVICE_BUS_QUEUE_NAME_ENV = "AZURE_SERVICE_BUS_QUEUE_NAME";
  private static final String DEFAULT_AZURE_SERVICE_BUS_QUEUE = "reservation-export-jobs";

  private ReservationCheckerData reservationCheckerData;

  @Override
  public void init() {
    this.reservationCheckerData = new ReservationCheckerData(IOUtils.getReservationListFromConfig());
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    String methodName = "doGet";
    logger.entering(AvailabilityCheckerServlet.class.getName(), methodName);
    int statusCode = 200;

    String selectedDateStr = request.getParameter("date");
    boolean parsedDate = reservationCheckerData.setSelectedDate(selectedDateStr);
    if (!parsedDate || reservationCheckerData.getReservationList() == null) {
      statusCode = 500;
      reservationCheckerData.setAvailablility(false);
    } else {
      List<Reservation> reservations = reservationCheckerData.getReservationList().getReservations();
      boolean isAvailible = true;
      LocalDate selectedDate = Instant.ofEpochMilli(reservationCheckerData.getSelectedDate().getTime())
          .atZone(ZoneOffset.UTC)
          .toLocalDate();

      for (Reservation reservation : reservations) {
        LocalDate fromDate = LocalDate.parse(reservation.getFromDate(), RESERVATION_DATE_FORMATTER);
        LocalDate toDate = LocalDate.parse(reservation.getToDate(), RESERVATION_DATE_FORMATTER);

        if (selectedDate.isAfter(fromDate) && selectedDate.isBefore(toDate)) {
          isAvailible = false;
          break;
        }
      }

      reservationCheckerData.setAvailablility(isAvailible);
      if (!isAvailible) {
        statusCode = 201;
      }

      scheduleReservationExport(selectedDateStr);
    }

    PrintWriter out = response.getWriter();
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    out.print("{\"availability\": \"" + String.valueOf(reservationCheckerData.isAvailible()) + "\"}");
    response.setStatus(statusCode);
  }

  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {
    doGet(request, response);
  }

  protected int exportRevervations(String selectedDateStr) {
    try (InputStream reservationStream = IOUtils.getResourceAsStream("reservations.json")) {
      if (reservationStream == null) {
        logger.severe("Reservation export source file is not available.");
        return -1;
      }

      BlobContainerClient containerClient = createBlobContainerClient();
      String blobName = buildBlobName(selectedDateStr);
      BlobClient blobClient = containerClient.getBlobClient(blobName);
      try (InputStream zipStream = BinaryReservationZipData.from("reservations.json", reservationStream).toStream()) {
        blobClient.upload(zipStream, true);
      }
      return blobClient.exists() ? 0 : -1;
    } catch (IOException | UncheckedIOException e) {
      logger.severe("Failed to export reservations to Azure Blob Storage: " + e.getMessage());
    } catch (RuntimeException e) {
      logger.severe("Azure Blob Storage configuration error: " + e.getMessage());
    }
    return -1;
  }

  private void scheduleReservationExport(String selectedDateStr) {
    String connectionString = System.getenv(AZURE_SERVICE_BUS_CONNECTION_STRING_ENV);
    if (connectionString == null || connectionString.trim().isEmpty()) {
      logger.fine("Azure Service Bus connection string not configured; skipping scheduled reservation export.");
      return;
    }

    String queueName = getEnvOrDefault(AZURE_SERVICE_BUS_QUEUE_NAME_ENV, DEFAULT_AZURE_SERVICE_BUS_QUEUE);
    try (ServiceBusSenderClient senderClient = new ServiceBusClientBuilder()
        .connectionString(connectionString)
        .sender()
        .queueName(queueName)
        .buildClient()) {
      ServiceBusMessage message = new ServiceBusMessage(selectedDateStr == null ? "all-dates" : selectedDateStr);
      message.setSubject("reservation-export");
      message.setScheduledEnqueueTime(Instant.now().plusSeconds(60));
      senderClient.scheduleMessage(message, message.getScheduledEnqueueTime());
    } catch (RuntimeException ex) {
      logger.warning("Failed to schedule reservation export with Azure Service Bus: " + ex.getMessage());
    }
  }

  private BlobContainerClient createBlobContainerClient() {
    String connectionString = getRequiredEnv(AZURE_STORAGE_CONNECTION_STRING_ENV);
    String containerName = getEnvOrDefault(AZURE_STORAGE_CONTAINER_ENV, DEFAULT_AZURE_STORAGE_CONTAINER);

    BlobContainerClient containerClient = new BlobContainerClientBuilder()
        .connectionString(connectionString)
        .containerName(containerName)
        .buildClient();

    if (!containerClient.exists()) {
      containerClient.create();
    }
    return containerClient;
  }

  private String buildBlobName(String selectedDateStr) {
    String normalizedDate = (selectedDateStr == null || selectedDateStr.trim().isEmpty())
        ? "all-dates"
        : selectedDateStr.trim().replaceAll("[^a-zA-Z0-9-_]", "-");
    return "reservations/" + normalizedDate + "-" + UUID.randomUUID() + ".zip";
  }

  private String getRequiredEnv(String envName) {
    String value = System.getenv(envName);
    if (value == null || value.trim().isEmpty()) {
      throw new IllegalStateException("Missing required environment variable: " + envName);
    }
    return value;
  }

  private String getEnvOrDefault(String envName, String defaultValue) {
    String value = System.getenv(envName);
    return (value == null || value.trim().isEmpty()) ? defaultValue : value.trim();
  }

  private static final class BinaryReservationZipData {
    private final byte[] data;

    private BinaryReservationZipData(byte[] data) {
      this.data = data;
    }

    private static BinaryReservationZipData from(String entryName, InputStream sourceStream) throws IOException {
      try (InputStream fis = sourceStream;
          java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
          java.util.zip.ZipOutputStream zipOut = new java.util.zip.ZipOutputStream(baos)) {
        java.util.zip.ZipEntry zipEntry = new java.util.zip.ZipEntry(entryName);
        zipOut.putNextEntry(zipEntry);

        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
          zipOut.write(bytes, 0, length);
        }
        zipOut.closeEntry();
        zipOut.finish();
        return new BinaryReservationZipData(baos.toByteArray());
      }
    }

    private java.io.InputStream toStream() {
      return new java.io.ByteArrayInputStream(data);
    }
  }
}
