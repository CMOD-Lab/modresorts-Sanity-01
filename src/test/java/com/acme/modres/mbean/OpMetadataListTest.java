package com.acme.modres.mbean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

class OpMetadataListTest {

    private OpMetadataList opMetadataList;

    @BeforeEach
    void setUp() {
        opMetadataList = new OpMetadataList();
    }

    @Test
    void testDefaultConstructor_createsInstance() {
        assertNotNull(opMetadataList);
    }

    @Test
    void testDefaultConstructor_emptyList() {
        assertNotNull(opMetadataList.getOpMetadatList());
        assertTrue(opMetadataList.getOpMetadatList().isEmpty());
    }

    @Test
    void testAdd_singleElement() {
        OpMetadata op = new OpMetadata("op1", "desc1", "void", 1);
        opMetadataList.add(op);
        assertEquals(1, opMetadataList.getOpMetadatList().size());
    }

    @Test
    void testAdd_multipleElements() {
        OpMetadata op1 = new OpMetadata("op1", "desc1", "void", 1);
        OpMetadata op2 = new OpMetadata("op2", "desc2", "int", 2);
        opMetadataList.add(op1);
        opMetadataList.add(op2);
        assertEquals(2, opMetadataList.getOpMetadatList().size());
    }

    @Test
    void testAdd_preservesOrder() {
        OpMetadata op1 = new OpMetadata("op1", "desc1", "void", 1);
        OpMetadata op2 = new OpMetadata("op2", "desc2", "int", 2);
        opMetadataList.add(op1);
        opMetadataList.add(op2);
        assertEquals("op1", opMetadataList.getOpMetadatList().get(0).getName());
        assertEquals("op2", opMetadataList.getOpMetadatList().get(1).getName());
    }

    @Test
    void testSetOpMetadatList_replacesExistingList() {
        List<OpMetadata> newList = new ArrayList<>();
        newList.add(new OpMetadata("newOp", "newDesc", "void", 5));
        opMetadataList.setOpMetadatList(newList);
        assertEquals(1, opMetadataList.getOpMetadatList().size());
        assertEquals("newOp", opMetadataList.getOpMetadatList().get(0).getName());
    }

    @Test
    void testSetOpMetadatList_withEmptyList() {
        opMetadataList.add(new OpMetadata("op1", "desc1", "void", 1));
        opMetadataList.setOpMetadatList(new ArrayList<>());
        assertTrue(opMetadataList.getOpMetadatList().isEmpty());
    }

    @Test
    void testGetOpMetadatList_returnsCorrectList() {
        OpMetadata op = new OpMetadata("testOp", "testDesc", "void", 0);
        opMetadataList.add(op);
        List<OpMetadata> result = opMetadataList.getOpMetadatList();
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("testOp", result.get(0).getName());
    }

    @Test
    void testSetOpMetadatList_withNull() {
        opMetadataList.setOpMetadatList(null);
        assertNull(opMetadataList.getOpMetadatList());
    }
}
