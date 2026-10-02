package com.acme.modres.mbean;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class IOUtilsTest {

    @Test
    void testGetFileFromRelativePath_withExistingResource_returnsFile() {
        // "reservations.json" is a known resource in src/main/resources
        java.io.File file = IOUtils.getFileFromRelativePath("reservations.json");
        assertNotNull(file);
    }

    @Test
    void testGetFileFromRelativePath_withExistingResource_fileExists() {
        java.io.File file = IOUtils.getFileFromRelativePath("reservations.json");
        assertNotNull(file);
        assertTrue(file.exists());
    }

    @Test
    void testGetFileFromRelativePath_withNonExistingResource_returnsNull() {
        java.io.File file = IOUtils.getFileFromRelativePath("nonexistent.json");
        assertNull(file);
    }

    @Test
    void testGetOpListFromConfig_returnsNonNull() {
        com.acme.modres.mbean.OpMetadataList opList = IOUtils.getOpListFromConfig();
        assertNotNull(opList);
    }

    @Test
    void testGetOpListFromConfig_returnsListWithOperations() {
        com.acme.modres.mbean.OpMetadataList opList = IOUtils.getOpListFromConfig();
        assertNotNull(opList);
        assertNotNull(opList.getOpMetadatList());
        assertFalse(opList.getOpMetadatList().isEmpty());
    }

    @Test
    void testGetOpListFromConfig_containsExpectedOperations() {
        com.acme.modres.mbean.OpMetadataList opList = IOUtils.getOpListFromConfig();
        assertNotNull(opList);
        boolean foundIncreaseMaxLimit = false;
        for (OpMetadata op : opList.getOpMetadatList()) {
            if ("increaseMaxLimit".equals(op.getName())) {
                foundIncreaseMaxLimit = true;
                break;
            }
        }
        assertTrue(foundIncreaseMaxLimit);
    }

    @Test
    void testGetReservationListFromConfig_returnsNonNull() {
        com.acme.modres.mbean.reservation.ReservationList reservationList = IOUtils.getReservationListFromConfig();
        assertNotNull(reservationList);
    }

    @Test
    void testGetReservationListFromConfig_returnsListWithReservations() {
        com.acme.modres.mbean.reservation.ReservationList reservationList = IOUtils.getReservationListFromConfig();
        assertNotNull(reservationList);
        assertNotNull(reservationList.getReservations());
        assertFalse(reservationList.getReservations().isEmpty());
    }

    @Test
    void testGetReservationListFromConfig_containsExpectedReservations() {
        com.acme.modres.mbean.reservation.ReservationList reservationList = IOUtils.getReservationListFromConfig();
        assertNotNull(reservationList);
        // The reservations.json has 2 reservations
        assertEquals(2, reservationList.getReservations().size());
    }

    @Test
    void testGetFileFromRelativePath_withOpsJson_returnsFile() {
        java.io.File file = IOUtils.getFileFromRelativePath("ops.json");
        assertNotNull(file);
        assertTrue(file.exists());
    }
}
