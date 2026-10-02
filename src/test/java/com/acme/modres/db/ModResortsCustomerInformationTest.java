package com.acme.modres.db;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ModResortsCustomerInformationTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private ModResortsCustomerInformation customerInformation;

    @Test
    void testGetCustomerInformation_withMockedDataSource_returnsResults() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, true, false);
        when(resultSet.getString("INFO")).thenReturn("Customer1", "Customer2");

        ArrayList<String> result = customerInformation.getCustomerInformation();

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("Customer1", result.get(0));
        assertEquals("Customer2", result.get(1));
    }

    @Test
    void testGetCustomerInformation_withEmptyResultSet_returnsEmptyList() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        ArrayList<String> result = customerInformation.getCustomerInformation();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCustomerInformation_withSQLException_returnsEmptyList() throws SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException("Connection failed"));

        ArrayList<String> result = customerInformation.getCustomerInformation();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCustomerInformation_closesResources() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        customerInformation.getCustomerInformation();

        verify(resultSet).close();
        verify(preparedStatement).close();
        verify(connection).close();
    }

    @Test
    void testGetCustomerInformation_withSingleResult_returnsOneItem() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("INFO")).thenReturn("SingleCustomer");

        ArrayList<String> result = customerInformation.getCustomerInformation();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("SingleCustomer", result.get(0));
    }

    @Test
    void testModResortsCustomerInformation_isInstantiable() {
        ModResortsCustomerInformation info = new ModResortsCustomerInformation();
        assertNotNull(info);
    }

    @Test
    void testGetCustomerInformation_returnsArrayList() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(false);

        Object result = customerInformation.getCustomerInformation();

        assertTrue(result instanceof ArrayList);
    }

    @Test
    void testGetCustomerInformation_withPrepareStatementException_returnsEmptyList() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenThrow(new SQLException("PrepareStatement failed"));

        ArrayList<String> result = customerInformation.getCustomerInformation();

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }
}
