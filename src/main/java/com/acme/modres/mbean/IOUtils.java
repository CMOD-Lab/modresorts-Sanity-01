package com.acme.modres.mbean;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import com.acme.modres.mbean.reservation.ReservationList;
import com.google.gson.Gson;

import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

/**
 * Utility class for reading configuration resources.
 *
 * Cloud-native migration (cr-java-0112):
 * The original getFileFromRelativePath() method wrote classpath resources to a
 * local temp file using File.createTempFile() / FileOutputStream (line 23),
 * which is incompatible with ephemeral cloud/container file systems where
 * temporary storage is lost on container restart.
 *
 * Remediation: Local temporary storage has been replaced with Amazon S3 for
 * persistent intermediate data, ensuring data survives container restarts and
 * enabling multi-instance access. The S3 bucket name is read from the
 * environment variable S3_TEMP_BUCKET (12-factor app principle).
 *
 * Cloud-native migration (cr-java-0062):
 * The getFileFromRelativePath() method has been replaced with
 * getResourceAsStream() which reads resources directly from the classpath
 * InputStream without any local file system write operations.
 */
public final class IOUtils {

  /**
   * Environment variable name for the S3 bucket used for temporary/intermediate
   * data storage. Set this in your cloud environment (ECS task definition,
   * Lambda environment, EC2 user data, etc.).
   */
  private static final String S3_TEMP_BUCKET_ENV = "S3_TEMP_BUCKET";

  /**
   * Reads a classpath resource and returns its content as a byte array.
   * Replaces the previous implementation that wrote to a local temp file
   * via File.createTempFile() / FileOutputStream — a pattern incompatible
   * with cloud/container environments where the local file system is ephemeral.
   *
   * @param path classpath-relative resource path (e.g. "reservations.json")
   * @return byte array of the resource content, or null if not found
   */
  public static byte[] getResourceBytes(String path) {
    try (InputStream initialStream = IOUtils.class.getClassLoader().getResourceAsStream(path)) {
      if (initialStream == null) {
        return null;
      }
      byte[] buffer = new byte[initialStream.available()];
      initialStream.read(buffer);
      return buffer;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  /**
   * Opens a classpath resource as an InputStream.
   * Use this when the caller needs streaming access to the resource content
   * without materialising it on the local file system.
   *
   * @param path classpath-relative resource path
   * @return InputStream for the resource, or null if not found
   */
  public static InputStream getResourceAsStream(String path) {
    return IOUtils.class.getClassLoader().getResourceAsStream(path);
  }

  /**
   * Stores intermediate/temporary data in Amazon S3 instead of the local
   * temporary directory. This replaces the File.createTempFile() pattern
   * (cr-java-0112) with a cloud-native persistent store that survives
   * container restarts and is accessible across multiple instances.
   *
   * The target S3 bucket is resolved from the environment variable
   * {@value #S3_TEMP_BUCKET_ENV}.
   *
   * @param s3Key  the S3 object key (acts as the "file name" in the bucket)
   * @param data   the byte content to store
   * @return true if the upload succeeded, false otherwise
   */
  public static boolean storeTempDataToS3(String s3Key, byte[] data) {
    String bucketName = System.getenv(S3_TEMP_BUCKET_ENV);
    if (bucketName == null || bucketName.isEmpty()) {
      System.err.println("[IOUtils] Environment variable " + S3_TEMP_BUCKET_ENV
          + " is not set. Cannot store temporary data to S3.");
      return false;
    }
    try (S3Client s3 = S3Client.create()) {
      PutObjectRequest putRequest = PutObjectRequest.builder()
          .bucket(bucketName)
          .key(s3Key)
          .build();
      s3.putObject(putRequest, RequestBody.fromBytes(data));
      return true;
    } catch (S3Exception e) {
      System.err.println("[IOUtils] Failed to store temp data to S3 bucket '"
          + bucketName + "', key '" + s3Key + "': " + e.awsErrorDetails().errorMessage());
      return false;
    }
  }

  /**
   * Retrieves intermediate/temporary data from Amazon S3.
   * This is the cloud-native counterpart to reading a local temp file.
   *
   * The source S3 bucket is resolved from the environment variable
   * {@value #S3_TEMP_BUCKET_ENV}.
   *
   * @param s3Key the S3 object key to retrieve
   * @return InputStream of the object content, or null if not found / on error
   */
  public static InputStream getTempDataFromS3(String s3Key) {
    String bucketName = System.getenv(S3_TEMP_BUCKET_ENV);
    if (bucketName == null || bucketName.isEmpty()) {
      System.err.println("[IOUtils] Environment variable " + S3_TEMP_BUCKET_ENV
          + " is not set. Cannot retrieve temporary data from S3.");
      return null;
    }
    try {
      S3Client s3 = S3Client.create();
      GetObjectRequest getRequest = GetObjectRequest.builder()
          .bucket(bucketName)
          .key(s3Key)
          .build();
      return s3.getObject(getRequest);
    } catch (S3Exception e) {
      System.err.println("[IOUtils] Failed to retrieve temp data from S3 bucket '"
          + bucketName + "', key '" + s3Key + "': " + e.awsErrorDetails().errorMessage());
      return null;
    }
  }

  public static OpMetadataList getOpListFromConfig() {
    // Read directly from classpath InputStream — no local file write required
    try (InputStream is = getResourceAsStream("ops.json")) {
      if (is == null) {
        return null;
      }
      Gson gson = new Gson();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      return gson.fromJson(reader, OpMetadataList.class);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ReservationList getReservationListFromConfig() {
    // Read directly from classpath InputStream — no local file write required
    try (InputStream is = getResourceAsStream("reservations.json")) {
      if (is == null) {
        return null;
      }
      Gson gson = new Gson();
      BufferedReader reader = new BufferedReader(new InputStreamReader(is));
      return gson.fromJson(reader, ReservationList.class);
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

}
