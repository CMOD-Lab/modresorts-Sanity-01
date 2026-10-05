package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import javax.management.MBeanOperationInfo;
import java.util.ArrayList;
import java.util.List;

public class DMBeanUtilsTest {

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
        // empty list - numOps == 0, so ops remains null
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNull(result);
    }

    @Test
    void testGetOps_withSingleOperation_returnsArray() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("increaseMaxLimit", "Increases the limit", "java.lang.String", MBeanOperationInfo.ACTION));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals(1, result.length);
    }

    @Test
    void testGetOps_withSingleOperation_correctName() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("testOp", "Test Operation", "void", MBeanOperationInfo.ACTION));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("testOp", result[0].getName());
    }

    @Test
    void testGetOps_withSingleOperation_correctDescription() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("testOp", "Test Operation", "void", MBeanOperationInfo.ACTION));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("Test Operation", result[0].getDescription());
    }

    @Test
    void testGetOps_withSingleOperation_correctReturnType() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("testOp", "Test Operation", "java.lang.String", MBeanOperationInfo.ACTION));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("java.lang.String", result[0].getReturnType());
    }

    @Test
    void testGetOps_withMultipleOperations_returnsCorrectCount() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op1", "desc1", "void", MBeanOperationInfo.ACTION));
        opList.add(new OpMetadata("op2", "desc2", "void", MBeanOperationInfo.INFO));
        opList.add(new OpMetadata("op3", "desc3", "void", MBeanOperationInfo.UNKNOWN));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals(3, result.length);
    }

    @Test
    void testGetOps_withMultipleOperations_preservesOrder() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("firstOp", "First", "void", MBeanOperationInfo.ACTION));
        opList.add(new OpMetadata("secondOp", "Second", "void", MBeanOperationInfo.INFO));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        assertEquals("firstOp", result[0].getName());
        assertEquals("secondOp", result[1].getName());
    }

    @Test
    void testGetOps_withNullSignature_noSignatureParams() {
        OpMetadataList opList = new OpMetadataList();
        opList.add(new OpMetadata("op1", "desc1", "void", MBeanOperationInfo.ACTION));
        MBeanOperationInfo[] result = DMBeanUtils.getOps(opList);
        assertNotNull(result);
        // signature is null, so signature array should be empty
        assertNotNull(result[0].getSignature());
        assertEquals(0, result[0].getSignature().length);
    }
}
