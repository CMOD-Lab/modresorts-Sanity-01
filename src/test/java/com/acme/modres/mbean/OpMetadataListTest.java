package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

public class OpMetadataListTest {

    private OpMetadataList opMetadataList;

    @BeforeEach
    void setUp() {
        opMetadataList = new OpMetadataList();
    }

    @Test
    void testDefaultConstructor_CreatesInstance() {
        assertNotNull(opMetadataList);
    }

    @Test
    void testDefaultConstructor_EmptyList() {
        assertNotNull(opMetadataList.getOpMetadatList());
        assertTrue(opMetadataList.getOpMetadatList().isEmpty());
    }

    @Test
    void testAdd_SingleItem_ListHasOneItem() {
        // Arrange
        OpMetadata op = new OpMetadata("op1", "desc1", "void", 0);

        // Act
        opMetadataList.add(op);

        // Assert
        assertEquals(1, opMetadataList.getOpMetadatList().size());
    }

    @Test
    void testAdd_MultipleItems_ListHasCorrectCount() {
        // Arrange
        OpMetadata op1 = new OpMetadata("op1", "desc1", "void", 0);
        OpMetadata op2 = new OpMetadata("op2", "desc2", "String", 1);
        OpMetadata op3 = new OpMetadata("op3", "desc3", "int", 2);

        // Act
        opMetadataList.add(op1);
        opMetadataList.add(op2);
        opMetadataList.add(op3);

        // Assert
        assertEquals(3, opMetadataList.getOpMetadatList().size());
    }

    @Test
    void testAdd_ItemIsRetrievable() {
        // Arrange
        OpMetadata op = new OpMetadata("myOp", "My operation", "void", 0);

        // Act
        opMetadataList.add(op);

        // Assert
        assertEquals("myOp", opMetadataList.getOpMetadatList().get(0).getName());
    }

    @Test
    void testGetOpMetadatList_ReturnsNonNull() {
        assertNotNull(opMetadataList.getOpMetadatList());
    }

    @Test
    void testSetOpMetadatList_UpdatesList() {
        // Arrange
        List<OpMetadata> newList = new ArrayList<>();
        newList.add(new OpMetadata("op1", "desc1", "void", 0));
        newList.add(new OpMetadata("op2", "desc2", "String", 1));

        // Act
        opMetadataList.setOpMetadatList(newList);

        // Assert
        assertEquals(2, opMetadataList.getOpMetadatList().size());
    }

    @Test
    void testSetOpMetadatList_WithNull_SetsNull() {
        // Act
        opMetadataList.setOpMetadatList(null);

        // Assert
        assertNull(opMetadataList.getOpMetadatList());
    }

    @Test
    void testSetOpMetadatList_WithEmptyList_SetsEmptyList() {
        // Arrange
        List<OpMetadata> emptyList = new ArrayList<>();

        // Act
        opMetadataList.setOpMetadatList(emptyList);

        // Assert
        assertNotNull(opMetadataList.getOpMetadatList());
        assertTrue(opMetadataList.getOpMetadatList().isEmpty());
    }

    @Test
    void testAdd_NullItem_AddsNull() {
        // Act
        opMetadataList.add(null);

        // Assert
        assertEquals(1, opMetadataList.getOpMetadatList().size());
        assertNull(opMetadataList.getOpMetadatList().get(0));
    }
}
