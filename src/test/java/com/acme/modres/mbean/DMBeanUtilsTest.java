package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javax.management.MBeanOperationInfo;
import java.util.ArrayList;
import java.util.List;

public class DMBeanUtilsTest {

    @Test
    void testGetOps_WithNullOpList_ReturnsNull() {
        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(null);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetOps_WithEmptyOpList_ReturnsNull() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetOps_WithNullInternalList_ReturnsNull() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();
        opList.setOpMetadatList(null);

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNull(result);
    }

    @Test
    void testGetOps_WithOneOperation_ReturnsArrayOfOne() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();
        OpMetadata op = new OpMetadata("testOp", "Test operation", "void", MBeanOperationInfo.ACTION);
        opList.add(op);

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.length);
    }

    @Test
    void testGetOps_WithMultipleOperations_ReturnsCorrectCount() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op1", "Operation 1", "void", MBeanOperationInfo.ACTION));
        opList.add(new OpMetadata("op2", "Operation 2", "String", MBeanOperationInfo.INFO));
        opList.add(new OpMetadata("op3", "Operation 3", "int", MBeanOperationInfo.ACTION_INFO));

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    void testGetOps_WithOperation_CorrectName() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();
        OpMetadata op = new OpMetadata("myOperation", "My operation", "void", MBeanOperationInfo.ACTION);
        opList.add(op);

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNotNull(result);
        assertEquals("myOperation", result[0].getName());
    }

    @Test
    void testGetOps_WithOperation_CorrectDescription() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();
        OpMetadata op = new OpMetadata("op", "My description", "void", MBeanOperationInfo.ACTION);
        opList.add(op);

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNotNull(result);
        assertEquals("My description", result[0].getDescription());
    }

    @Test
    void testGetOps_WithOperation_CorrectReturnType() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();
        OpMetadata op = new OpMetadata("op", "desc", "java.lang.String", MBeanOperationInfo.ACTION);
        opList.add(op);

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNotNull(result);
        assertEquals("java.lang.String", result[0].getReturnType());
    }

    @Test
    void testGetOps_WithOperation_CorrectImpact() {
        // Arrange
        OpMetadataList opList = new OpMetadataList();
        OpMetadata op = new OpMetadata("op", "desc", "void", MBeanOperationInfo.INFO);
        opList.add(op);

        // Act
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);

        // Assert
        assertNotNull(result);
        assertEquals(MBeanOperationInfo.INFO, result[0].getImpact());
    }
}
