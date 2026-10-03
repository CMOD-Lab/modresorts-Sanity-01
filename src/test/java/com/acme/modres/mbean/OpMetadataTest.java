package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class OpMetadataTest {

    @Test
    void testDefaultConstructor_CreatesInstance() {
        OpMetadata op = new OpMetadata();
        assertNotNull(op);
    }

    @Test
    void testParameterizedConstructor_SetsAllFields() {
        // Arrange & Act
        OpMetadata op = new OpMetadata("testOp", "Test description", "void", 1);

        // Assert
        assertEquals("testOp", op.getName());
        assertEquals("Test description", op.getDescription());
        assertEquals("void", op.getType());
        assertEquals(1, op.getImpact());
    }

    @Test
    void testGetName_ReturnsCorrectName() {
        OpMetadata op = new OpMetadata("myOp", "desc", "void", 0);
        assertEquals("myOp", op.getName());
    }

    @Test
    void testGetDescription_ReturnsCorrectDescription() {
        OpMetadata op = new OpMetadata("op", "My description", "void", 0);
        assertEquals("My description", op.getDescription());
    }

    @Test
    void testGetType_ReturnsCorrectType() {
        OpMetadata op = new OpMetadata("op", "desc", "java.lang.String", 0);
        assertEquals("java.lang.String", op.getType());
    }

    @Test
    void testGetImpact_ReturnsCorrectImpact() {
        OpMetadata op = new OpMetadata("op", "desc", "void", 4);
        assertEquals(4, op.getImpact());
    }

    @Test
    void testSetName_UpdatesName() {
        OpMetadata op = new OpMetadata();
        op.setName("newName");
        assertEquals("newName", op.getName());
    }

    @Test
    void testSetDescription_UpdatesDescription() {
        OpMetadata op = new OpMetadata();
        op.setDescription("new description");
        assertEquals("new description", op.getDescription());
    }

    @Test
    void testSetType_UpdatesType() {
        OpMetadata op = new OpMetadata();
        op.setType("int");
        assertEquals("int", op.getType());
    }

    @Test
    void testSetImpact_UpdatesImpact() {
        OpMetadata op = new OpMetadata();
        op.setImpact(2);
        assertEquals(2, op.getImpact());
    }

    @Test
    void testDefaultConstructor_FieldsAreNull() {
        OpMetadata op = new OpMetadata();
        assertNull(op.getName());
        assertNull(op.getDescription());
        assertNull(op.getType());
        assertEquals(0, op.getImpact());
    }

    @Test
    void testSetName_WithNull_SetsNull() {
        OpMetadata op = new OpMetadata("name", "desc", "void", 0);
        op.setName(null);
        assertNull(op.getName());
    }

    @Test
    void testSetDescription_WithNull_SetsNull() {
        OpMetadata op = new OpMetadata("name", "desc", "void", 0);
        op.setDescription(null);
        assertNull(op.getDescription());
    }

    @Test
    void testSetType_WithNull_SetsNull() {
        OpMetadata op = new OpMetadata("name", "desc", "void", 0);
        op.setType(null);
        assertNull(op.getType());
    }

    @Test
    void testSetImpact_WithZero_SetsZero() {
        OpMetadata op = new OpMetadata("name", "desc", "void", 5);
        op.setImpact(0);
        assertEquals(0, op.getImpact());
    }

    @Test
    void testSetImpact_WithNegative_SetsNegative() {
        OpMetadata op = new OpMetadata();
        op.setImpact(-1);
        assertEquals(-1, op.getImpact());
    }
}
