package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class FakeX509TrustManagerTest {

    @Test
    void testConstructor_CreatesInstance() {
        FakeX509TrustManager trustManager = new FakeX509TrustManager();
        assertNotNull(trustManager);
    }

    @Test
    void testFakeX509TrustManager_IsObject() {
        FakeX509TrustManager trustManager = new FakeX509TrustManager();
        assertTrue(trustManager instanceof Object);
    }

    @Test
    void testFakeX509TrustManager_ClassNameIsCorrect() {
        FakeX509TrustManager trustManager = new FakeX509TrustManager();
        assertEquals("com.acme.modres.security.FakeX509TrustManager",
                trustManager.getClass().getName());
    }
}
