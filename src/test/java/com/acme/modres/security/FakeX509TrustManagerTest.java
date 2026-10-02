package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class FakeX509TrustManagerTest {

    @Test
    void testConstructor_createsInstance() {
        FakeX509TrustManager trustManager = new FakeX509TrustManager();
        assertNotNull(trustManager);
    }

    @Test
    void testFakeX509TrustManager_isInstantiable() {
        FakeX509TrustManager t1 = new FakeX509TrustManager();
        FakeX509TrustManager t2 = new FakeX509TrustManager();
        assertNotNull(t1);
        assertNotNull(t2);
        assertNotSame(t1, t2);
    }
}
