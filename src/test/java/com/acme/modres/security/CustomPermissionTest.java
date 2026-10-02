package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.security.BasicPermission;

class CustomPermissionTest {

    @Test
    void testConstructorWithName_createsInstance() {
        CustomPermission permission = new CustomPermission("test-permission");
        assertNotNull(permission);
    }

    @Test
    void testConstructorWithNameAndActions_createsInstance() {
        CustomPermission permission = new CustomPermission("test-permission", "read");
        assertNotNull(permission);
    }

    @Test
    void testConstructorWithName_getName() {
        CustomPermission permission = new CustomPermission("my-permission");
        assertEquals("my-permission", permission.getName());
    }

    @Test
    void testConstructorWithNameAndActions_getName() {
        CustomPermission permission = new CustomPermission("my-permission", "write");
        assertEquals("my-permission", permission.getName());
    }

    @Test
    void testIsInstanceOfBasicPermission() {
        CustomPermission permission = new CustomPermission("test");
        assertTrue(permission instanceof BasicPermission);
    }

    @Test
    void testConstructorWithName_withWildcard() {
        CustomPermission permission = new CustomPermission("*");
        assertNotNull(permission);
        assertEquals("*", permission.getName());
    }

    @Test
    void testConstructorWithNameAndActions_withNullActions() {
        CustomPermission permission = new CustomPermission("test-permission", null);
        assertNotNull(permission);
    }

    @Test
    void testConstructorWithNameAndActions_withEmptyActions() {
        CustomPermission permission = new CustomPermission("test-permission", "");
        assertNotNull(permission);
    }

    @Test
    void testImplies_samePermission_returnsTrue() {
        CustomPermission p1 = new CustomPermission("test");
        CustomPermission p2 = new CustomPermission("test");
        assertTrue(p1.implies(p2));
    }

    @Test
    void testImplies_wildcardImpliesSpecific_returnsTrue() {
        CustomPermission wildcard = new CustomPermission("*");
        CustomPermission specific = new CustomPermission("specific");
        assertTrue(wildcard.implies(specific));
    }

    @Test
    void testConstructorWithName_differentNames() {
        CustomPermission p1 = new CustomPermission("permission1");
        CustomPermission p2 = new CustomPermission("permission2");
        assertNotEquals(p1.getName(), p2.getName());
    }
}
