package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class CustomPermissionTest {

    @Test
    void testConstructor_WithName_CreatesInstance() {
        CustomPermission permission = new CustomPermission("test.permission");
        assertNotNull(permission);
    }

    @Test
    void testConstructor_WithNameAndActions_CreatesInstance() {
        CustomPermission permission = new CustomPermission("test.permission", "read,write");
        assertNotNull(permission);
    }

    @Test
    void testGetName_WithSingleArgConstructor_ReturnsName() {
        CustomPermission permission = new CustomPermission("my.permission");
        assertEquals("my.permission", permission.getName());
    }

    @Test
    void testGetName_WithTwoArgConstructor_ReturnsName() {
        CustomPermission permission = new CustomPermission("my.permission", "read");
        assertEquals("my.permission", permission.getName());
    }

    @Test
    void testConstructor_WithDotSeparatedName_CreatesInstance() {
        CustomPermission permission = new CustomPermission("com.acme.modres.permission");
        assertNotNull(permission);
    }

    @Test
    void testConstructor_WithWildcardName_CreatesInstance() {
        CustomPermission permission = new CustomPermission("com.acme.*");
        assertNotNull(permission);
    }

    @Test
    void testCustomPermission_ExtendsBasicPermission() {
        CustomPermission permission = new CustomPermission("test");
        assertTrue(permission instanceof java.security.BasicPermission);
    }

    @Test
    void testCustomPermission_ExtendsPermission() {
        CustomPermission permission = new CustomPermission("test");
        assertTrue(permission instanceof java.security.Permission);
    }

    @Test
    void testImplies_SamePermission_ReturnsTrue() {
        CustomPermission p1 = new CustomPermission("test.permission");
        CustomPermission p2 = new CustomPermission("test.permission");
        assertTrue(p1.implies(p2));
    }

    @Test
    void testImplies_WildcardImpliesSpecific_ReturnsTrue() {
        CustomPermission wildcard = new CustomPermission("test.*");
        CustomPermission specific = new CustomPermission("test.specific");
        assertTrue(wildcard.implies(specific));
    }

    @Test
    void testConstructor_WithEmptyActions_CreatesInstance() {
        CustomPermission permission = new CustomPermission("test.permission", "");
        assertNotNull(permission);
    }

    @Test
    void testConstructor_WithNullActions_CreatesInstance() {
        CustomPermission permission = new CustomPermission("test.permission", null);
        assertNotNull(permission);
    }
}
