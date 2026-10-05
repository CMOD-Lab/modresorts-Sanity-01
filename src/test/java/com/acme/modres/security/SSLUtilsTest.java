package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SSLUtilsTest {

    @Test
    void testConstructor_createsInstance() {
        SSLUtils sslUtils = new SSLUtils();
        assertNotNull(sslUtils);
    }

    @Test
    void testInstance_isNotNull() {
        SSLUtils utils = new SSLUtils();
        assertNotNull(utils);
    }

    @Test
    void testClass_canBeInstantiated() {
        assertDoesNotThrow(() -> new SSLUtils());
    }
}
