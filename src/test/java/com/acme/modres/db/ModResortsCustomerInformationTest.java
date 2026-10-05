package com.acme.modres.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.lang.reflect.Field;

@ExtendWith(MockitoExtension.class)
public class ModResortsCustomerInformationTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    private ModResortsCustomerInformation customerInfo;

    @BeforeEach
    void setUp() throws Exception {
        customerInfo = new ModResortsCustomerInformation();
        // Inject mock dataSource via reflection
        Field dataSourceField = ModResortsCustomerInformation.class.getDeclaredField("dataSource");
        dataSourceField.setAccessible(true);
        dataSourceField.set(customerInfo, dataSource);
    }

    @Test
    void testConstructor_createsInstance() {
        assertNotNull(customerInfo);
    }

    @Test
    void testGetCustomerInformation_withResults_returnsCustomerList() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("INFO")).thenReturn("Customer1", "Customer2");

        ArrayList<String> result = customerInfo.getCustomerInformation();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Customer1", result.get(0));
        assertEquals("Customer2", result.get(1));
    }

    @Test
    void testGetCustomerInformation_withNoResults_returnsEmptyList() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        ArrayList<String> result = customerInfo.getCustomerInformation();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCustomerInformation_withSQLException_returnsEmptyList() throws Exception {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        ArrayList<String> result = customerInfo.getCustomerInformation();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCustomerInformation_closesResources() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        customerInfo.getCustomerInformation();

        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void testGetCustomerInformation_withSingleResult_returnsOneItem() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("INFO")).thenReturn("SingleCustomer");

        ArrayList<String> result = customerInfo.getCustomerInformation();

        assertEquals(1, result.size());
        assertEquals("SingleCustomer", result.get(0));
    }

    @Test
    void testGetCustomerInformation_returnsArrayList() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Object result = customerInfo.getCustomerInformation();

        assertTrue(result instanceof ArrayList);
    }
}
