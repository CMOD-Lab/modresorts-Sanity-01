package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CustomPermissionTest {

    @Test
    void testConstructorWithName_createsInstance() {
        CustomPermission permission = new CustomPermission("my-operation");
        assertNotNull(permission);
    }

    @Test
    void testConstructorWithName_getName() {
        CustomPermission permission = new CustomPermission("my-operation");
        assertEquals("my-operation", permission.getName());
    }

    @Test
    void testConstructorWithNameAndActions_createsInstance() {
        CustomPermission permission = new CustomPermission("my-operation", "read");
        assertNotNull(permission);
    }

    @Test
    void testConstructorWithNameAndActions_getName() {
        CustomPermission permission = new CustomPermission("my-operation", "write");
        assertEquals("my-operation", permission.getName());
    }

    @Test
    void testConstructorWithNameAndActions_nullActions() {
        CustomPermission permission = new CustomPermission("my-operation", null);
        assertNotNull(permission);
        assertEquals("my-operation", permission.getName());
    }

    @Test
    void testConstructorWithName_extendsBasicPermission() {
        CustomPermission permission = new CustomPermission("test-perm");
        assertTrue(permission instanceof java.security.BasicPermission);
    }

    @Test
    void testConstructorWithNameAndActions_extendsBasicPermission() {
        CustomPermission permission = new CustomPermission("test-perm", "action");
        assertTrue(permission instanceof java.security.BasicPermission);
    }

    @Test
    void testImplies_samePermission_returnsTrue() {
        CustomPermission p1 = new CustomPermission("my-operation");
        CustomPermission p2 = new CustomPermission("my-operation");
        assertTrue(p1.implies(p2));
    }

    @Test
    void testImplies_differentPermission_returnsFalse() {
        CustomPermission p1 = new CustomPermission("my-operation");
        CustomPermission p2 = new CustomPermission("other-operation");
        assertFalse(p1.implies(p2));
    }

    @Test
    void testGetActions_returnsEmptyString() {
        CustomPermission permission = new CustomPermission("my-operation");
        // BasicPermission.getActions() returns empty string
        assertEquals("", permission.getActions());
    }
}
