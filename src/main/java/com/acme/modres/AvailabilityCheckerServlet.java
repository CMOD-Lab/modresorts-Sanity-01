package com.acme.modres;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.naming.InitialContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.acme.modres.mbean.IOUtils;
import com.acme.modres.mbean.reservation.DateChecker;
import com.acme.modres.mbean.reservation.ReservationCheckerData;
import com.acme.modres.mbean.reservation.Reservation;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@WebServlet({ "/resorts/availability" })
public class AvailabilityCheckerServlet extends HttpServlet {
  private static final long serialVersionUID = 1L;

  private static final Logger logger = Logger.getLogger(AvailabilityCheckerServlet.class.getName());

  private static InitialContext context;

  private ReservationCheckerData reservationCheckerData;

  @Override
  public void init() {
    // load reserved dates
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

      DateTimeFormatter formatter = DateTimeFormatter.ofPattern(Constants.DATA_FORMAT).withZone(ZoneOffset.UTC);
      for (Reservation reservation : reservations) {
        try {
          LocalDate fromDate = LocalDate.parse(reservation.getFromDate(), formatter);
          LocalDate toDate = LocalDate.parse(reservation.getToDate(), formatter);
          LocalDate selectedDate = reservationCheckerData.getSelectedDate();

          if (selectedDate.isAfter(fromDate) && selectedDate.isBefore(toDate)) {
            isAvailible = false;
            break;
          }
        } catch (DateTimeParseException ex) {
          ex.printStackTrace();
        }
      }

      reservationCheckerData.setAvailablility(isAvailible);

      // Adjust the status code based on availability
      if (!isAvailible) {
        statusCode = 201;
      }
    }

    // Send the response
    PrintWriter out = response.getWriter();
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    out.print("{\"availability\": \"" + String.valueOf(reservationCheckerData.isAvailible()) + "\"}");
    response.setStatus(statusCode);
  }

  /**
   * Returns the weather information for a given city
   */
  protected void doPost(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    doGet(request, response);
  }

  protected int exportRevervations(String selectedDateStr) {
    // Retrieve S3 bucket name and object key from environment variables
    // to eliminate hard-coded file path dependencies on the host file system.
    String s3BucketName = System.getenv("S3_BUCKET_NAME");
    String s3ObjectKey = System.getenv("S3_RESERVATIONS_ZIP_KEY") != null
        ? System.getenv("S3_RESERVATIONS_ZIP_KEY")
        : "reservations/reservations.zip";

    try {
      // Read the reservations.json resource directly into memory from the classpath
      // using IOUtils.getBytesFromResource(), replacing the java.io.File-based
      // FileInputStream read loop that assumed local file system availability.
      byte[] resourceBytes = IOUtils.getBytesFromResource("reservations.json");
      if (resourceBytes == null) {
        logger.warning("Could not read reservations.json from classpath");
        return -1;
      }

      // Build the zip content entirely in memory using ByteArrayOutputStream
      // instead of writing to a local file path (e.g. user.home + "/reservations.zip").
      // This removes the java.io.File persistent storage dependency (cr-java-0063).
      // Use try-with-resources to ensure ZipOutputStream is automatically closed,
      // preventing resource leaks in containerized AWS environments (cr-java-0098).
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      try (ZipOutputStream zipOut = new ZipOutputStream(baos)) {
        ZipEntry zipEntry = new ZipEntry("reservations.json");
        zipOut.putNextEntry(zipEntry);
        zipOut.write(resourceBytes, 0, resourceBytes.length);
        zipOut.closeEntry();
      }

      // Upload the in-memory zip content directly to Amazon S3
      // using AWS SDK for Java v2, replacing the local file system write.
      // Use try-with-resources to ensure S3Client is automatically closed,
      // preventing resource leaks in containerized AWS environments (cr-java-0098).
      byte[] zipBytes = baos.toByteArray();

      String awsRegion = System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION") : "us-east-1";
      try (S3Client s3Client = S3Client.builder()
          .region(Region.of(awsRegion))
          .build()) {

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(s3BucketName)
            .key(s3ObjectKey)
            .contentType("application/zip")
            .contentLength((long) zipBytes.length)
            .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(zipBytes));
      }

      logger.info("Reservations zip uploaded to S3: s3://" + s3BucketName + "/" + s3ObjectKey);
      return 0;

    } catch (IOException e) {
      e.printStackTrace();
    } catch (S3Exception e) {
      e.printStackTrace();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    return -1;
  }

}
