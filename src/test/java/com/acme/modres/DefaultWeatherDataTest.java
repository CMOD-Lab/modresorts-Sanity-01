package com.acme.modres;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DefaultWeatherDataTest {

    @Test
    void testConstructor_withValidCity_Paris() {
        DefaultWeatherData data = new DefaultWeatherData("Paris");
        assertNotNull(data);
        assertEquals("Paris", data.getCity());
    }

    @Test
    void testConstructor_withValidCity_LasVegas() {
        DefaultWeatherData data = new DefaultWeatherData("Las_Vegas");
        assertNotNull(data);
        assertEquals("Las_Vegas", data.getCity());
    }

    @Test
    void testConstructor_withValidCity_SanFrancisco() {
        DefaultWeatherData data = new DefaultWeatherData("San_Francisco");
        assertNotNull(data);
        assertEquals("San_Francisco", data.getCity());
    }

    @Test
    void testConstructor_withValidCity_Miami() {
        DefaultWeatherData data = new DefaultWeatherData("Miami");
        assertNotNull(data);
        assertEquals("Miami", data.getCity());
    }

    @Test
    void testConstructor_withValidCity_Cork() {
        DefaultWeatherData data = new DefaultWeatherData("Cork");
        assertNotNull(data);
        assertEquals("Cork", data.getCity());
    }

    @Test
    void testConstructor_withValidCity_Barcelona() {
        DefaultWeatherData data = new DefaultWeatherData("Barcelona");
        assertNotNull(data);
        assertEquals("Barcelona", data.getCity());
    }

    @Test
    void testConstructor_withNullCity_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> new DefaultWeatherData(null));
    }

    @Test
    void testConstructor_withInvalidCity_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> new DefaultWeatherData("InvalidCity"));
    }

    @Test
    void testConstructor_withEmptyCity_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> new DefaultWeatherData(""));
    }

    @Test
    void testConstructor_withLowercaseCity_throwsUnsupportedOperationException() {
        assertThrows(UnsupportedOperationException.class, () -> new DefaultWeatherData("paris"));
    }

    @Test
    void testGetCity_returnsCorrectCity() {
        DefaultWeatherData data = new DefaultWeatherData("Paris");
        assertEquals("Paris", data.getCity());
    }

    @Test
    void testGetDefaultWeatherData_Paris_returnsNonNull() throws Exception {
        DefaultWeatherData data = new DefaultWeatherData("Paris");
        String result = data.getDefaultWeatherData();
        assertNotNull(result);
    }

    @Test
    void testGetDefaultWeatherData_LasVegas_returnsNonNull() throws Exception {
        DefaultWeatherData data = new DefaultWeatherData("Las_Vegas");
        String result = data.getDefaultWeatherData();
        assertNotNull(result);
    }

    @Test
    void testGetDefaultWeatherData_SanFrancisco_returnsNonNull() throws Exception {
        DefaultWeatherData data = new DefaultWeatherData("San_Francisco");
        String result = data.getDefaultWeatherData();
        assertNotNull(result);
    }

    @Test
    void testGetDefaultWeatherData_Miami_returnsNonNull() throws Exception {
        DefaultWeatherData data = new DefaultWeatherData("Miami");
        String result = data.getDefaultWeatherData();
        assertNotNull(result);
    }

    @Test
    void testGetDefaultWeatherData_Cork_returnsNonNull() throws Exception {
        DefaultWeatherData data = new DefaultWeatherData("Cork");
        String result = data.getDefaultWeatherData();
        assertNotNull(result);
    }

    @Test
    void testGetDefaultWeatherData_Barcelona_returnsNonNull() throws Exception {
        DefaultWeatherData data = new DefaultWeatherData("Barcelona");
        String result = data.getDefaultWeatherData();
        assertNotNull(result);
    }

    @Test
    void testGetDefaultWeatherData_Paris_returnsNonEmptyString() throws Exception {
        DefaultWeatherData data = new DefaultWeatherData("Paris");
        String result = data.getDefaultWeatherData();
        assertFalse(result.isEmpty());
    }

    @Test
    void testConstructor_nullCity_exceptionMessage() {
        UnsupportedOperationException ex = assertThrows(
            UnsupportedOperationException.class,
            () -> new DefaultWeatherData(null)
        );
        assertEquals("City is not defined", ex.getMessage());
    }

    @Test
    void testConstructor_invalidCity_exceptionMessageContainsCity() {
        UnsupportedOperationException ex = assertThrows(
            UnsupportedOperationException.class,
            () -> new DefaultWeatherData("Tokyo")
        );
        assertTrue(ex.getMessage().contains("Tokyo") || ex.getMessage().contains("invalid"));
    }
}
