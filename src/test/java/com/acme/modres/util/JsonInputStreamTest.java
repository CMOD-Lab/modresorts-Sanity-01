package com.acme.modres.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;

class JsonInputStreamTest {

    @TempDir
    Path tempDir;

    @Test
    void testConstructor_withExistingFile_createsInstance() throws Exception {
        File file = tempDir.resolve("test.json").toFile();
        file.createNewFile();
        JsonInputStream jis = new JsonInputStream(file);
        assertNotNull(jis);
        jis.close();
    }

    @Test
    void testConstructor_withNonExistingFile_throwsFileNotFoundException() {
        File file = new File("/nonexistent/path/test.json");
        assertThrows(java.io.FileNotFoundException.class, () -> new JsonInputStream(file));
    }

    @Test
    void testParseJsonAs_withValidJson_returnsObject() throws Exception {
        File file = tempDir.resolve("test.json").toFile();
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("{\"name\":\"test\",\"description\":\"desc\",\"type\":\"void\",\"impact\":1}");
        }
        JsonInputStream jis = new JsonInputStream(file);
        Object result = jis.parseJsonAs(com.acme.modres.mbean.OpMetadata.class);
        assertNotNull(result);
        jis.close();
    }

    @Test
    void testParseJsonAs_withValidJson_returnsCorrectType() throws Exception {
        File file = tempDir.resolve("test.json").toFile();
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("{\"name\":\"testOp\",\"description\":\"desc\",\"type\":\"void\",\"impact\":1}");
        }
        JsonInputStream jis = new JsonInputStream(file);
        Object result = jis.parseJsonAs(com.acme.modres.mbean.OpMetadata.class);
        assertTrue(result instanceof com.acme.modres.mbean.OpMetadata);
        jis.close();
    }

    @Test
    void testParseJsonAs_withValidJson_correctFieldValues() throws Exception {
        File file = tempDir.resolve("test.json").toFile();
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("{\"name\":\"myOp\",\"description\":\"myDesc\",\"type\":\"void\",\"impact\":5}");
        }
        JsonInputStream jis = new JsonInputStream(file);
        com.acme.modres.mbean.OpMetadata result = (com.acme.modres.mbean.OpMetadata) jis.parseJsonAs(com.acme.modres.mbean.OpMetadata.class);
        assertNotNull(result);
        assertEquals("myOp", result.getName());
        assertEquals("myDesc", result.getDescription());
        jis.close();
    }

    @Test
    void testParseJsonAs_withEmptyFile_returnsNull() throws Exception {
        File file = tempDir.resolve("empty.json").toFile();
        file.createNewFile();
        JsonInputStream jis = new JsonInputStream(file);
        Object result = jis.parseJsonAs(com.acme.modres.mbean.OpMetadata.class);
        assertNull(result);
        jis.close();
    }

    @Test
    void testParseJsonAs_withInvalidJson_returnsNull() throws Exception {
        File file = tempDir.resolve("invalid.json").toFile();
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("not valid json {{{");
        }
        JsonInputStream jis = new JsonInputStream(file);
        Object result = jis.parseJsonAs(com.acme.modres.mbean.OpMetadata.class);
        assertNull(result);
        jis.close();
    }

    @Test
    void testParseJsonAs_withReservationListJson_returnsObject() throws Exception {
        File file = tempDir.resolve("reservations.json").toFile();
        try (FileWriter fw = new FileWriter(file)) {
            fw.write("{\"reservations\":[{\"fromDate\":\"04/10/2024\",\"toDate\":\"04/15/2024\"}]}");
        }
        JsonInputStream jis = new JsonInputStream(file);
        Object result = jis.parseJsonAs(com.acme.modres.mbean.reservation.ReservationList.class);
        assertNotNull(result);
        jis.close();
    }
}
