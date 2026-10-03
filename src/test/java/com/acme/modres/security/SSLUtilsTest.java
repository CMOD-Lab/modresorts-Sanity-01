package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SSLUtilsTest {

    @Test
    void testSSLUtils_CanBeInstantiated() {
        SSLUtils sslUtils = new SSLUtils();
        assertNotNull(sslUtils);
    }

    @Test
    void testSSLUtils_ClassNameIsCorrect() {
        SSLUtils sslUtils = new SSLUtils();
        assertEquals("com.acme.modres.security.SSLUtils", sslUtils.getClass().getName());
    }

    @Test
    void testSSLUtils_IsObject() {
        SSLUtils sslUtils = new SSLUtils();
        assertTrue(sslUtils instanceof Object);
    }
}
