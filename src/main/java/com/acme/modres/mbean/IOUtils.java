package com.acme.modres.mbean;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import com.acme.modres.mbean.reservation.ReservationList;
import com.acme.modres.util.JsonInputStream;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobContainerClientBuilder;

public final class IOUtils {

  private static final String AZURE_STORAGE_CONNECTION_STRING_ENV = "AZURE_STORAGE_CONNECTION_STRING";
  private static final String AZURE_STORAGE_CONTAINER_ENV = "AZURE_STORAGE_CONTAINER_NAME";
  private static final String DEFAULT_CONTAINER_NAME = "modres-temp-config";

  private IOUtils() {
  }

  public static InputStream getResourceAsStream(String path) {
    return IOUtils.class.getClassLoader().getResourceAsStream(path);
  }

  public static File getFileFromRelativePath(String path) {
    try (InputStream initialStream = getResourceAsStream(path)) {
      if (initialStream == null) {
        return null;
      }

      byte[] content = readAllBytes(initialStream);
      uploadToAzureBlobStorage(path, content);
      return createInMemoryJsonFile(path, content);
    } catch (Exception e) {
      e.printStackTrace();
      return null;
    }
  }

  private static void uploadToAzureBlobStorage(String path, byte[] content) {
    String connectionString = System.getenv(AZURE_STORAGE_CONNECTION_STRING_ENV);
    if (connectionString == null || connectionString.trim().isEmpty()) {
      return;
    }

    String containerName = System.getenv(AZURE_STORAGE_CONTAINER_ENV);
    if (containerName == null || containerName.trim().isEmpty()) {
      containerName = DEFAULT_CONTAINER_NAME;
    }

    BlobContainerClient containerClient = new BlobContainerClientBuilder()
        .connectionString(connectionString)
        .containerName(containerName)
        .buildClient();

    if (!containerClient.exists()) {
      containerClient.create();
    }

    BlobClient blobClient = containerClient.getBlobClient(path);
    blobClient.upload(new ByteArrayInputStream(content), content.length, true);
  }

  private static File createInMemoryJsonFile(String path, byte[] content) {
    return new InMemoryJsonFile(path, content);
  }

  private static byte[] readAllBytes(InputStream inputStream) throws IOException {
    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int bytesRead;
    while ((bytesRead = inputStream.read(buffer)) != -1) {
      outputStream.write(buffer, 0, bytesRead);
    }
    return outputStream.toByteArray();
  }

  public static OpMetadataList getOpListFromConfig() {
    File file = getFileFromRelativePath("ops.json");
    try (JsonInputStream is = new JsonInputStream(file)) {
      OpMetadataList opList = new OpMetadataList();
      opList = (OpMetadataList) is.parseJsonAs(OpMetadataList.class);
      return opList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  public static ReservationList getReservationListFromConfig() {
    File file = getFileFromRelativePath("reservations.json");
    try (JsonInputStream is = new JsonInputStream(file)) {
      ReservationList reservationList = new ReservationList();
      reservationList = (ReservationList) is.parseJsonAs(ReservationList.class);
      return reservationList;
    } catch (IOException e) {
      e.printStackTrace();
      return null;
    }
  }

  private static final class InMemoryJsonFile extends File {
    private static final long serialVersionUID = 1L;
    private final byte[] content;

    private InMemoryJsonFile(String path, byte[] content) {
      super(path == null ? "resource.json" : path);
      this.content = content == null ? new byte[0] : content.clone();
    }

    @Override
    public boolean exists() {
      return true;
    }

    @Override
    public long length() {
      return content.length;
    }

    private byte[] getContent() {
      return content.clone();
    }

    @Override
    public String getPath() {
      return new String(content, StandardCharsets.UTF_8);
    }
  }

  public static byte[] getInMemoryFileContent(File file) {
    if (file instanceof InMemoryJsonFile) {
      return ((InMemoryJsonFile) file).getContent();
    }
    return null;
  }
}
