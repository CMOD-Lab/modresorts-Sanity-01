package com.acme.modres.mbean;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Logger;

import com.acme.modres.mbean.reservation.ReservationList;
import com.google.gson.Gson;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Utility class for I/O operations.
 *
 * Cloud-readiness fix (cr-java-0062): Replaced local file system write
 * operations (FileOutputStream to a temp file) with in-memory processing
 * using ByteArrayOutputStream. Classpath resources are now read directly
 * into memory and parsed without writing to the ephemeral local file system,
 * ensuring data durability and compatibility with containerized / serverless
 * AWS environments.
 *
 * Cloud-readiness fix (cr-java-0112): Eliminated reliance on ephemeral local
 * temporary directories (File.createTempFile / /tmp) by migrating temporary
 * file operations to Amazon S3. Intermediate data is now stored in and
 * retrieved from S3, ensuring persistence across container restarts and
 * enabling multi-instance access in AWS cloud environments.
 */
public final class IOUtils {

  private static final Logger logger = Logger.getLogger(IOUtils.class.getName());

  /**
   * Reads a classpath resource into a byte array without writing to the local
   * file system. Replaces the previous pattern of creating a temp File and
   * writing to it via FileOutputStream (cloud-incompatible local write).
   *
   * @param path classpath-relative resource path
   * @return byte array containing the resource content, or null on error
   */
  public static byte[] getBytesFromResource(String path) {
    InputStream initialStream = null;
    try {
      initialStream = IOUtils.class.getClassLoader().getResourceAsStream(path);
      if (initialStream == null) {
        return null;
      }
      // Use ByteArrayOutputStream to buffer the resource in memory
      // instead of writing to a local FileOutputStream (ephemeral in cloud).
      ByteArrayOutputStream baos = new ByteArrayOutputStream();
      byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = initialStream.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }
      return baos.toByteArray();
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    } finally {
      if (initialStream != null) {
        try {
          initialStream.close();
        } catch (IOException e) {
          // ignore close error
        }
      }
    }
  }

  /**
   * Stores a classpath resource as an object in Amazon S3 and returns the S3
   * object key under which it was stored.
   *
   * Replaces the previous implementation that wrote the resource to a local
   * temporary file via File.createTempFile() (cr-java-0112). Local /tmp storage
   * is ephemeral in cloud containers — data is lost on container restart and is
   * not accessible across multiple instances. Amazon S3 provides durable,
   * shared, persistent storage that survives container lifecycle events.
   *
   * S3 configuration is driven by environment variables:
   *   AWS_REGION          – AWS region (default: us-east-1)
   *   S3_BUCKET_NAME      – target S3 bucket name (required)
   *   S3_TEMP_PREFIX      – optional key prefix for temp objects (default: "tmp/")
   *
   * @param path classpath-relative resource path
   * @return the S3 object key where the resource was stored, or null on error
   */
  public static String storeResourceToS3(String path) {
    String awsRegion = System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION") : "us-east-1";
    String s3BucketName = System.getenv("S3_BUCKET_NAME");
    String s3TempPrefix = System.getenv("S3_TEMP_PREFIX") != null ? System.getenv("S3_TEMP_PREFIX") : "tmp/";

    if (s3BucketName == null || s3BucketName.isEmpty()) {
      logger.warning("S3_BUCKET_NAME environment variable is not set; cannot store resource to S3: " + path);
      return null;
    }

    // Read the classpath resource into memory first
    byte[] resourceBytes = getBytesFromResource(path);
    if (resourceBytes == null) {
      logger.warning("Could not read classpath resource: " + path);
      return null;
    }

    // Derive a safe S3 object key from the resource path
    String s3ObjectKey = s3TempPrefix + path.replace('\\', '/');

    // Upload the resource bytes directly to S3 — no local temp file required.
    // Use try-with-resources to ensure S3Client is automatically closed,
    // preventing resource leaks in containerised AWS environments.
    try (S3Client s3Client = S3Client.builder()
        .region(Region.of(awsRegion))
        .build()) {

      PutObjectRequest putRequest = PutObjectRequest.builder()
          .bucket(s3BucketName)
          .key(s3ObjectKey)
          .contentLength((long) resourceBytes.length)
          .build();

      s3Client.putObject(putRequest, RequestBody.fromBytes(resourceBytes));
      logger.info("Resource stored to S3: s3://" + s3BucketName + "/" + s3ObjectKey);
      return s3ObjectKey;

    } catch (S3Exception e) {
      logger.severe("Failed to store resource to S3: " + e.awsErrorDetails().errorMessage());
      e.printStackTrace();
      return null;
    } catch (Exception e) {
      logger.severe("Unexpected error storing resource to S3: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Retrieves an object from Amazon S3 and returns its content as a byte array.
   *
   * This method complements {@link #storeResourceToS3(String)} and provides
   * a cloud-native alternative to reading from a local temporary file.
   *
   * @param s3ObjectKey the S3 object key to retrieve
   * @return byte array containing the object content, or null on error
   */
  public static byte[] getBytesFromS3(String s3ObjectKey) {
    String awsRegion = System.getenv("AWS_REGION") != null ? System.getenv("AWS_REGION") : "us-east-1";
    String s3BucketName = System.getenv("S3_BUCKET_NAME");

    if (s3BucketName == null || s3BucketName.isEmpty()) {
      logger.warning("S3_BUCKET_NAME environment variable is not set; cannot retrieve object from S3: " + s3ObjectKey);
      return null;
    }

    try (S3Client s3Client = S3Client.builder()
        .region(Region.of(awsRegion))
        .build()) {

      GetObjectRequest getRequest = GetObjectRequest.builder()
          .bucket(s3BucketName)
          .key(s3ObjectKey)
          .build();

      try (ResponseInputStream<GetObjectResponse> s3Object = s3Client.getObject(getRequest)) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int bytesRead;
        while ((bytesRead = s3Object.read(buffer)) != -1) {
          baos.write(buffer, 0, bytesRead);
        }
        return baos.toByteArray();
      }

    } catch (S3Exception e) {
      logger.severe("Failed to retrieve object from S3: " + e.awsErrorDetails().errorMessage());
      e.printStackTrace();
      return null;
    } catch (Exception e) {
      logger.severe("Unexpected error retrieving object from S3: " + e.getMessage());
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Parses a classpath JSON resource directly from an InputStream into the
   * target class, bypassing any local file system write.
   *
   * @param path classpath-relative resource path
   * @param cls  target class for JSON deserialization
   * @return deserialized object, or null on error
   */
  private static <T> T parseJsonFromClasspath(String path, Class<T> cls) {
    InputStream stream = IOUtils.class.getClassLoader().getResourceAsStream(path);
    if (stream == null) {
      return null;
    }
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, "UTF-8"))) {
      Gson gson = new Gson();
      return gson.fromJson(reader, cls);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  public static OpMetadataList getOpListFromConfig() {
    // Read directly from classpath InputStream — no local file write required.
    OpMetadataList result = parseJsonFromClasspath("ops.json", OpMetadataList.class);
    return result;
  }

  public static ReservationList getReservationListFromConfig() {
    // Read directly from classpath InputStream — no local file write required.
    ReservationList result = parseJsonFromClasspath("reservations.json", ReservationList.class);
    return result;
  }

}
