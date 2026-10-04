package com.acme.modres.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class ZipValidator extends ZipFile {

  public ZipValidator(File file) throws ZipException, IOException {
    super(file);
    this.file = file;
  }

  private File file;

  public boolean isValid() throws Throwable {
    if (file.exists()) {
      ZipValidator zipFile = new ZipValidator(file);
      Enumeration<? extends ZipEntry> entries = zipFile.entries();
      if (!entries.hasMoreElements()) {
        return true;
      }
      zipFile.close();
    }
    return false;
  }

  /**
   * Validates a zip archive provided as a byte array.
   * Used when the zip content is held in memory (e.g., before uploading to S3)
   * rather than written to the local file system.
   *
   * @param zipBytes the byte array containing the zip archive content
   * @return true if the byte array represents a valid (non-empty) zip archive,
   *         false otherwise
   */
  public static boolean isValidZipBytes(byte[] zipBytes) {
    if (zipBytes == null || zipBytes.length == 0) {
      return false;
    }
    try (ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes))) {
      ZipEntry entry = zis.getNextEntry();
      return entry != null;
    } catch (IOException e) {
      return false;
    }
  }

}
