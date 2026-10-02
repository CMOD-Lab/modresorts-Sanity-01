package com.acme.modres.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

class ZipValidatorTest {

    @TempDir
    Path tempDir;

    private File createValidZipFile(String fileName) throws IOException {
        File zipFile = tempDir.resolve(fileName).toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry entry = new ZipEntry("test.txt");
            zos.putNextEntry(entry);
            zos.write("test content".getBytes());
            zos.closeEntry();
        }
        return zipFile;
    }

    private File createEmptyZipFile(String fileName) throws IOException {
        File zipFile = tempDir.resolve(fileName).toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // Empty zip - no entries
        }
        return zipFile;
    }

    @Test
    void testConstructor_withValidZipFile_createsInstance() throws Exception {
        File zipFile = createValidZipFile("test.zip");
        ZipValidator validator = new ZipValidator(zipFile);
        assertNotNull(validator);
        validator.close();
    }

    @Test
    void testConstructor_withNonExistingFile_throwsException() {
        File nonExistentFile = new File("/nonexistent/path/test.zip");
        assertThrows(Exception.class, () -> new ZipValidator(nonExistentFile));
    }

    @Test
    void testIsValid_withEmptyZip_returnsTrue() throws Throwable {
        File zipFile = createEmptyZipFile("empty.zip");
        ZipValidator validator = new ZipValidator(zipFile);
        boolean result = validator.isValid();
        assertTrue(result);
        validator.close();
    }

    @Test
    void testIsValid_withNonExistentFile_returnsFalse() throws Throwable {
        // Create a valid zip first, then delete it
        File zipFile = createEmptyZipFile("todelete.zip");
        ZipValidator validator = new ZipValidator(zipFile);
        validator.close();
        zipFile.delete();
        // Now the file doesn't exist
        boolean result = validator.isValid();
        assertFalse(result);
    }

    @Test
    void testZipValidator_extendsZipFile() throws Exception {
        File zipFile = createValidZipFile("extends.zip");
        ZipValidator validator = new ZipValidator(zipFile);
        assertTrue(validator instanceof java.util.zip.ZipFile);
        validator.close();
    }
}
