package com.acme.modres.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class ZipValidatorTest {

    @TempDir
    Path tempDir;

    private File createValidZipFile(String filename) throws IOException {
        File zipFile = tempDir.resolve(filename).toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            ZipEntry entry = new ZipEntry("test.txt");
            zos.putNextEntry(entry);
            zos.write("test content".getBytes());
            zos.closeEntry();
        }
        return zipFile;
    }

    private File createEmptyZipFile(String filename) throws IOException {
        File zipFile = tempDir.resolve(filename).toFile();
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(zipFile))) {
            // empty zip - no entries
        }
        return zipFile;
    }

    @Test
    void testConstructor_withValidZipFile_createsInstance() throws Throwable {
        File zipFile = createValidZipFile("test.zip");
        ZipValidator validator = new ZipValidator(zipFile);
        assertNotNull(validator);
        validator.close();
    }

    @Test
    void testConstructor_withNonZipFile_throwsException() throws IOException {
        File nonZip = tempDir.resolve("notazip.txt").toFile();
        nonZip.createNewFile();
        assertThrows(Exception.class, () -> new ZipValidator(nonZip));
    }

    @Test
    void testIsValid_withEmptyZip_returnsTrue() throws Throwable {
        File emptyZip = createEmptyZipFile("empty.zip");
        ZipValidator validator = new ZipValidator(emptyZip);
        // isValid creates a new ZipValidator internally and checks entries
        // Empty zip has no entries, so returns true
        boolean result = validator.isValid();
        assertTrue(result);
        validator.close();
    }

    @Test
    void testIsValid_withNonExistentFile_returnsFalse() throws Throwable {
        File zipFile = createValidZipFile("existing.zip");
        ZipValidator validator = new ZipValidator(zipFile);
        zipFile.delete(); // delete the file after creating validator
        boolean result = validator.isValid();
        assertFalse(result);
        validator.close();
    }

    @Test
    void testConstructor_extendsZipFile() throws Throwable {
        File zipFile = createValidZipFile("extends.zip");
        ZipValidator validator = new ZipValidator(zipFile);
        assertTrue(validator instanceof java.util.zip.ZipFile);
        validator.close();
    }

    @Test
    void testIsValid_withZipContainingEntries_returnsFalse() throws Throwable {
        File zipFile = createValidZipFile("withentries.zip");
        ZipValidator validator = new ZipValidator(zipFile);
        // isValid creates a new ZipValidator and checks if entries.hasMoreElements()
        // If entries exist, it returns false (not valid by this logic)
        boolean result = validator.isValid();
        assertFalse(result);
        validator.close();
    }
}
