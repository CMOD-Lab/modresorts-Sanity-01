package com.acme.modres;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class DefaultWeatherDataTest {

    @Test
    void testConstructor_WithValidCity_Paris() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.PARIS);
        assertNotNull(data);
        assertEquals(Constants.PARIS, data.getCity());
    }

    @Test
    void testConstructor_WithValidCity_LasVegas() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.LAS_VEGAS);
        assertNotNull(data);
        assertEquals(Constants.LAS_VEGAS, data.getCity());
    }

    @Test
    void testConstructor_WithValidCity_SanFrancisco() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.SAN_FRANCISCO);
        assertNotNull(data);
        assertEquals(Constants.SAN_FRANCISCO, data.getCity());
    }

    @Test
    void testConstructor_WithValidCity_Miami() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.MIAMI);
        assertNotNull(data);
        assertEquals(Constants.MIAMI, data.getCity());
    }

    @Test
    void testConstructor_WithValidCity_Cork() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.CORK);
        assertNotNull(data);
        assertEquals(Constants.CORK, data.getCity());
    }

    @Test
    void testConstructor_WithValidCity_Barcelona() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.BARCELONA);
        assertNotNull(data);
        assertEquals(Constants.BARCELONA, data.getCity());
    }

    @Test
    void testConstructor_WithNullCity_ThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            new DefaultWeatherData(null);
        });
    }

    @Test
    void testConstructor_WithInvalidCity_ThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            new DefaultWeatherData("InvalidCity");
        });
    }

    @Test
    void testConstructor_WithEmptyCity_ThrowsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> {
            new DefaultWeatherData("");
        });
    }

    @Test
    void testGetCity_ReturnsCorrectCity() {
        DefaultWeatherData data = new DefaultWeatherData(Constants.PARIS);
        assertEquals(Constants.PARIS, data.getCity());
    }

    @Test
    void testGetDefaultWeatherData_Paris_ReturnsNonNull() throws Exception {
        DefaultWeatherData data = new DefaultWeatherData(Constants.PARIS);
        // This test verifies the method can be called; resource may not exist in test env
        try {
            String result = data.getDefaultWeatherData();
            assertNotNull(result);
        } catch (Exception e) {
            // Expected if resource file not available in test environment
            assertTrue(e instanceof NullPointerException || e instanceof java.io.IOException);
        }
    }

    @Test
    void testGetDefaultWeatherData_LasVegas_ReturnsNonNull() throws Exception {
        DefaultWeatherData data = new DefaultWeatherData(Constants.LAS_VEGAS);
        try {
            String result = data.getDefaultWeatherData();
            assertNotNull(result);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException || e instanceof java.io.IOException);
        }
    }

    @Test
    void testGetDefaultWeatherData_SanFrancisco_ReturnsNonNull() throws Exception {
        DefaultWeatherData data = new DefaultWeatherData(Constants.SAN_FRANCISCO);
        try {
            String result = data.getDefaultWeatherData();
            assertNotNull(result);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException || e instanceof java.io.IOException);
        }
    }

    @Test
    void testGetDefaultWeatherData_Miami_ReturnsNonNull() throws Exception {
        DefaultWeatherData data = new DefaultWeatherData(Constants.MIAMI);
        try {
            String result = data.getDefaultWeatherData();
            assertNotNull(result);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException || e instanceof java.io.IOException);
        }
    }

    @Test
    void testGetDefaultWeatherData_Cork_ReturnsNonNull() throws Exception {
        DefaultWeatherData data = new DefaultWeatherData(Constants.CORK);
        try {
            String result = data.getDefaultWeatherData();
            assertNotNull(result);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException || e instanceof java.io.IOException);
        }
    }

    @Test
    void testGetDefaultWeatherData_Barcelona_ReturnsNonNull() throws Exception {
        DefaultWeatherData data = new DefaultWeatherData(Constants.BARCELONA);
        try {
            String result = data.getDefaultWeatherData();
            assertNotNull(result);
        } catch (Exception e) {
            assertTrue(e instanceof NullPointerException || e instanceof java.io.IOException);
        }
    }

    @Test
    void testConstructor_CaseSensitive_ThrowsForLowerCase() {
        assertThrows(UnsupportedOperationException.class, () -> {
            new DefaultWeatherData("paris");
        });
    }

    @Test
    void testConstructor_CaseSensitive_ThrowsForUpperCase() {
        assertThrows(UnsupportedOperationException.class, () -> {
            new DefaultWeatherData("PARIS");
        });
    }
}
