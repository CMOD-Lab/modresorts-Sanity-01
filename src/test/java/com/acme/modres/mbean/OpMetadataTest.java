package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

public class OpMetadataTest {

    private OpMetadata opMetadata;

    @BeforeEach
    void setUp() {
        opMetadata = new OpMetadata();
    }

    @Test
    void testDefaultConstructor_createsInstance() {
        OpMetadata metadata = new OpMetadata();
        assertNotNull(metadata);
    }

    @Test
    void testParameterizedConstructor_setsAllFields() {
        OpMetadata metadata = new OpMetadata("testOp", "Test Operation", "void", 1);
        assertEquals("testOp", metadata.getName());
        assertEquals("Test Operation", metadata.getDescription());
        assertEquals("void", metadata.getType());
        assertEquals(1, metadata.getImpact());
    }

    @Test
    void testParameterizedConstructor_withNullName() {
        OpMetadata metadata = new OpMetadata(null, "desc", "void", 0);
        assertNull(metadata.getName());
    }

    @Test
    void testParameterizedConstructor_withNullDescription() {
        OpMetadata metadata = new OpMetadata("name", null, "void", 0);
        assertNull(metadata.getDescription());
    }

    @Test
    void testParameterizedConstructor_withNullType() {
        OpMetadata metadata = new OpMetadata("name", "desc", null, 0);
        assertNull(metadata.getType());
    }

    @Test
    void testSetName_andGetName() {
        opMetadata.setName("increaseMaxLimit");
        assertEquals("increaseMaxLimit", opMetadata.getName());
    }

    @Test
    void testSetDescription_andGetDescription() {
        opMetadata.setDescription("Increases the max limit");
        assertEquals("Increases the max limit", opMetadata.getDescription());
    }

    @Test
    void testSetType_andGetType() {
        opMetadata.setType("java.lang.String");
        assertEquals("java.lang.String", opMetadata.getType());
    }

    @Test
    void testSetImpact_andGetImpact() {
        opMetadata.setImpact(2);
        assertEquals(2, opMetadata.getImpact());
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
        opMetadata.setImpact(-1);
        assertEquals(-1, opMetadata.getImpact());
    }

    @Test
    void testDefaultConstructor_fieldsAreNull() {
        assertNull(opMetadata.getName());
        assertNull(opMetadata.getDescription());
        assertNull(opMetadata.getType());
        assertEquals(0, opMetadata.getImpact());
    }
}
