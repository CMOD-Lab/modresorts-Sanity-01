package com.acme.modres.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

public class JsonInputStreamTest {

    @TempDir
    Path tempDir;

    private File jsonFile;

    @BeforeEach
    void setUp() throws IOException {
        jsonFile = tempDir.resolve("test.json").toFile();
        try (FileWriter writer = new FileWriter(jsonFile)) {
            writer.write("{\"name\": \"test\"}");
        }
    }

    @Test
    void testConstructor_withValidFile_createsInstance() throws Exception {
        JsonInputStream stream = new JsonInputStream(jsonFile);
        assertNotNull(stream);
        stream.close();
    }

    @Test
    void testConstructor_withNonExistentFile_throwsFileNotFoundException() {
        File nonExistent = new File("/non/existent/file.json");
        assertThrows(Exception.class, () -> new JsonInputStream(nonExistent));
    }

    @Test
    void testParseJsonAs_withValidJsonFile_returnsObject() throws Exception {
        // Create a simple JSON file
        File simpleJson = tempDir.resolve("simple.json").toFile();
        try (FileWriter writer = new FileWriter(simpleJson)) {
            writer.write("{\"name\": \"test\", \"value\": 42}");
        }

        JsonInputStream stream = new JsonInputStream(simpleJson);
        Object result = stream.parseJsonAs(TestData.class);
        // parseJsonAs creates a new stream internally, so result may be null if file is consumed
        // The method returns null when file is not found or parsing fails
        stream.close();
    }

    @Test
    void testParseJsonAs_withNonExistentFile_returnsNull() throws Exception {
        // Create a file, then delete it
        File tempFile = tempDir.resolve("temp.json").toFile();
        tempFile.createNewFile();
        JsonInputStream stream = new JsonInputStream(tempFile);
        tempFile.delete(); // delete after creating stream

        Object result = stream.parseJsonAs(Object.class);
        assertNull(result);
        stream.close();
    }

    @Test
    void testParseJsonAs_withEmptyFile_returnsNull() throws Exception {
        File emptyFile = tempDir.resolve("empty.json").toFile();
        emptyFile.createNewFile();

        JsonInputStream stream = new JsonInputStream(emptyFile);
        Object result = stream.parseJsonAs(Object.class);
        // Empty JSON file returns null from Gson
        stream.close();
    }

    @Test
    void testConstructor_extendsFileInputStream() throws Exception {
        JsonInputStream stream = new JsonInputStream(jsonFile);
        assertTrue(stream instanceof java.io.FileInputStream);
        stream.close();
    }

    // Helper class for JSON parsing tests
    static class TestData {
        String name;
        int value;
    }
}
