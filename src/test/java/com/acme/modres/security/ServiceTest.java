package com.acme.modres.security;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public class ServiceTest {

    private Service service;

    @BeforeEach
    void setUp() {
        service = new Service();
    }

    @Test
    void testConstructor_CreatesInstance() {
        assertNotNull(service);
    }

    @Test
    void testOperationConstant_IsCorrect() {
        assertEquals("my-operation", Service.OPERATION);
    }

    @Test
    void testOperation_DoesNotThrow() {
        assertDoesNotThrow(() -> service.operation());
    }

    @Test
    void testOperation_PrintsMessage() {
        // Arrange
        ByteArrayOutputStream outContent = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        System.setOut(new PrintStream(outContent));

        try {
            // Act
            service.operation();

            // Assert
            assertTrue(outContent.toString().contains("Operation is executed"));
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    void testOperationConstant_IsStatic() throws Exception {
        // Verify OPERATION is accessible as static field
        java.lang.reflect.Field field = Service.class.getField("OPERATION");
        assertTrue(java.lang.reflect.Modifier.isStatic(field.getModifiers()));
    }

    @Test
    void testOperationConstant_IsFinal() throws Exception {
        // Verify OPERATION is a final field
        java.lang.reflect.Field field = Service.class.getField("OPERATION");
        assertTrue(java.lang.reflect.Modifier.isFinal(field.getModifiers()));
    }

    @Test
    void testOperation_CanBeCalledMultipleTimes() {
        // Act & Assert - should not throw on multiple calls
        assertDoesNotThrow(() -> {
            service.operation();
            service.operation();
            service.operation();
        });
    }

    @Test
    void testService_IsObject() {
        assertTrue(service instanceof Object);
    }
}
