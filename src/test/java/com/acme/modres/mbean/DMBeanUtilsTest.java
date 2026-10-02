package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import javax.management.MBeanOperationInfo;
import java.util.ArrayList;
import java.util.List;

class DMBeanUtilsTest {

    @Test
    void testGetOps_withNullOpList_returnsNull() {
        MBeanOperationInfo[] result = DMBeanUtils.getOps(null);
        assertNull(result);
    }

    @Test
    void testGetOps_withNullInnerList_returnsNull() {
        OpMetadataList opList = new OpMetadataList();
        opList.setOpMetadatList(null);
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNull(result);
    }

    @Test
    void testGetOps_withEmptyList_returnsNull() {
        OpMetadataList opList = new OpMetadataList();
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNull(result);
    }

    @Test
    void testGetOps_withSingleOperation_returnsArray() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("increaseMaxLimit", "Increase the max limit", "void", 1));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals(1, result.length);
    }

    @Test
    void testGetOps_withMultipleOperations_returnsCorrectCount() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op1", "desc1", "void", 1));
        opList.add(new OpMetadata("op2", "desc2", "int", 2));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals(2, result.length);
    }

    @Test
    void testGetOps_withSingleOperation_correctName() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("increaseMaxLimit", "Increase the max limit", "void", 1));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("increaseMaxLimit", result[0].getName());
    }

    @Test
    void testGetOps_withSingleOperation_correctDescription() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op1", "Test description", "void", 1));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("Test description", result[0].getDescription());
    }

    @Test
    void testGetOps_withSingleOperation_correctReturnType() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op1", "desc", "void", 1));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("void", result[0].getReturnType());
    }

    @Test
    void testGetOps_withSingleOperation_correctImpact() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op1", "desc", "void", MBeanOperationInfo.ACTION));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals(MBeanOperationInfo.ACTION, result[0].getImpact());
    }

    @Test
    void testGetOps_withThreeOperations_returnsAllThree() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op1", "desc1", "void", 1));
        opList.add(new OpMetadata("op2", "desc2", "String", 2));
        opList.add(new OpMetadata("op3", "desc3", "int", 3));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    void testGetOps_preservesOperationOrder() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("firstOp", "desc1", "void", 1));
        opList.add(new OpMetadata("secondOp", "desc2", "void", 1));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("firstOp", result[0].getName());
        assertEquals("secondOp", result[1].getName());
    }
}
