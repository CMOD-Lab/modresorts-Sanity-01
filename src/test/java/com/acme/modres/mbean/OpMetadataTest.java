package com.acme.modres.mbean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

class OpMetadataTest {

    private OpMetadata opMetadata;

    @BeforeEach
    void setUp() {
        opMetadata = new OpMetadata();
    }

    @Test
    void testDefaultConstructor_createsInstance() {
        assertNotNull(opMetadata);
    }

    @Test
    void testParameterizedConstructor_setsAllFields() {
        OpMetadata op = new OpMetadata("testOp", "Test description", "void", 1);
        assertEquals("testOp", op.getName());
        assertEquals("Test description", op.getDescription());
        assertEquals("void", op.getType());
        assertEquals(1, op.getImpact());
    }

    @Test
    void testSetName_andGetName() {
        opMetadata.setName("increaseMaxLimit");
        assertEquals("increaseMaxLimit", opMetadata.getName());
    }

    @Test
    void testSetDescription_andGetDescription() {
        opMetadata.setDescription("Increase the max limit");
        assertEquals("Increase the max limit", opMetadata.getDescription());
    }

    @Test
    void testSetType_andGetType() {
        opMetadata.setType("void");
        assertEquals("void", opMetadata.getType());
    }

    @Test
    void testSetImpact_andGetImpact() {
        opMetadata.setImpact(10);
        assertEquals(10, opMetadata.getImpact());
    }

    @Test
    void testSetName_withNull() {
        opMetadata.setName(null);
        assertNull(opMetadata.getName());
    }

    @Test
    void testSetDescription_withNull() {
        opMetadata.setDescription(null);
        assertNull(opMetadata.getDescription());
    }

    @Test
    void testSetType_withNull() {
        opMetadata.setType(null);
        assertNull(opMetadata.getType());
    }

    @Test
    void testSetImpact_withZero() {
        opMetadata.setImpact(0);
        assertEquals(0, opMetadata.getImpact());
    }

    @Test
    void testSetImpact_withNegativeValue() {
        opMetadata.setImpact(-5);
        assertEquals(-5, opMetadata.getImpact());
    }

    @Test
    void testParameterizedConstructor_withNullValues() {
        OpMetadata op = new OpMetadata(null, null, null, 0);
        assertNull(op.getName());
        assertNull(op.getDescription());
        assertNull(op.getType());
        assertEquals(0, op.getImpact());
    }

    @Test
    void testSetName_withEmptyString() {
        opMetadata.setName("");
        assertEquals("", opMetadata.getName());
    }

    @Test
    void testDefaultValues_areNull() {
        assertNull(opMetadata.getName());
        assertNull(opMetadata.getDescription());
        assertNull(opMetadata.getType());
        assertEquals(0, opMetadata.getImpact());
    }
}
