package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class SSLUtilsTest {

    @Test
    void testConstructor_createsInstance() {
        SSLUtils sslUtils = new SSLUtils();
        assertNotNull(sslUtils);
    }

    @Test
    void testSSLUtils_isInstantiable() {
        SSLUtils s1 = new SSLUtils();
        SSLUtils s2 = new SSLUtils();
        assertNotNull(s1);
        assertNotNull(s2);
    }
}
