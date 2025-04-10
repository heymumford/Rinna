package org.rinna.cli.component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.command.OperationsCommand;
import org.rinna.cli.service.AuthorizationService;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.domain.model.OperationRecord;

/**
 * Component tests for the OperationsCommand, focusing on integration with
 * dependent services and operation tracking.
 */
public class OperationsCommandComponentTest {

    private AutoCloseable closeable;
    private ByteArrayOutputStream outputCaptor;
    private ByteArrayOutputStream errorCaptor;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private OperationsCommand command;

    @Mock
    private ServiceManager mockServiceManager;

    @Mock
    private MetadataService mockMetadataService;

    @Mock
    private AuthorizationService mockAuthorizationService;
    
    @Mock
    private ConfigurationService mockConfigurationService;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        outputCaptor = new ByteArrayOutputStream();
        errorCaptor = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));

        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getAuthorizationService()).thenReturn(mockAuthorizationService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigurationService);
        when(mockAuthorizationService.isAuthenticated()).thenReturn(true);
        when(mockAuthorizationService.hasPermission(anyString())).thenReturn(true);
        
        when(mockConfigurationService.getStringValue(eq("output.format"), anyString())).thenReturn("text");
        when(mockConfigurationService.getIntValue(eq("operations.default_limit"), anyInt())).thenReturn(10);

        command = new OperationsCommand(mockServiceManager);
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        System.setErr(originalErr);
        closeable.close();
    }

    @Test
    @DisplayName("Should retrieve operations from MetadataService")
    void shouldRetrieveOperationsFromMetadataService() {
        // Given
        List<OperationRecord> mockOperations = createMockOperations(5);
        when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);

        // When
        int exitCode = command.call();

        // Then
        assertEquals(0, exitCode);
        verify(mockMetadataService).getRecentOperations(eq(10)); // default limit
        
        // Verify operation tracking
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), any());
        
        // Verify output
        String output = outputCaptor.toString();
        assertTrue(output.contains("Operation ID:"), "Output should include operation IDs");
        assertTrue(output.contains("op-1"), "Output should include specific operation IDs");
    }

    @Test
    @DisplayName("Should respect limit parameter")
    void shouldRespectLimitParameter() {
        // Given
        List<OperationRecord> mockOperations = createMockOperations(3);
        when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);
        command.setLimit(3);

        // When
        int exitCode = command.call();

        // Then
        assertEquals(0, exitCode);
        verify(mockMetadataService).getRecentOperations(eq(3));
        
        // Verify operation tracking includes limit
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), captor.capture());
        
        Map<String, Object> params = captor.getValue();
        assertTrue(params.containsKey("limit"), "Parameters should include limit");
        assertEquals(3, params.get("limit"), "Limit parameter should be 3");
    }

    @Test
    @DisplayName("Should filter operations by type")
    void shouldFilterOperationsByType() {
        // Given
        List<OperationRecord> mockOperations = createMockOperations(10);
        when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);
        command.setFilter("ADD_ITEM");

        // When
        int exitCode = command.call();

        // Then
        assertEquals(0, exitCode);
        
        // Verify operation tracking includes filter
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), captor.capture());
        
        Map<String, Object> params = captor.getValue();
        assertTrue(params.containsKey("filter"), "Parameters should include filter");
        assertEquals("ADD_ITEM", params.get("filter"), "Filter parameter should match input");
        
        // Verify output filtered
        String output = outputCaptor.toString();
        assertTrue(output.contains("ADD_ITEM"), "Output should contain filtered operation type");
        assertFalse(output.contains("Type: UPDATE_ITEM"), "Output should not contain filtered out operation types");
    }

    @Test
    @DisplayName("Should output in JSON format when requested")
    void shouldOutputInJsonFormat() {
        // Given
        List<OperationRecord> mockOperations = createMockOperations(5);
        when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);
        command.setJsonOutput(true);

        // When
        int exitCode = command.call();

        // Then
        assertEquals(0, exitCode);
        
        // Verify operation tracking includes format
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), captor.capture());
        
        Map<String, Object> params = captor.getValue();
        assertTrue(params.containsKey("format"), "Parameters should include format");
        assertEquals("json", params.get("format"), "Format parameter should be json");
        
        // Verify JSON output
        String output = outputCaptor.toString();
        assertTrue(output.contains("[") && output.contains("]"), 
                "Output should be in JSON format with array brackets");
        assertTrue(output.contains("\"operationId\""), 
                "JSON output should contain field names in quotes");
    }

    @Test
    @DisplayName("Should show detailed information in verbose mode")
    void shouldShowDetailedInformationInVerboseMode() {
        // Given
        List<OperationRecord> mockOperations = createMockOperations(3);
        when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);
        command.setVerbose(true);

        // When
        int exitCode = command.call();

        // Then
        assertEquals(0, exitCode);
        
        // Verify operation tracking includes verbose flag
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), captor.capture());
        
        Map<String, Object> params = captor.getValue();
        assertTrue(params.containsKey("verbose"), "Parameters should include verbose");
        assertEquals(true, params.get("verbose"), "Verbose parameter should be true");
        
        // Verify verbose output
        String output = outputCaptor.toString();
        assertTrue(output.contains("Parameters:"), "Verbose output should include parameters section");
        assertTrue(output.contains("Results:"), "Verbose output should include results section");
    }

    @Test
    @DisplayName("Should handle empty result list gracefully")
    void shouldHandleEmptyResultListGracefully() {
        // Given
        when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(new ArrayList<>());

        // When
        int exitCode = command.call();

        // Then
        assertEquals(0, exitCode);
        
        // Verify operation still tracked
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), any());
        
        // Verify appropriate message
        String output = outputCaptor.toString();
        assertTrue(output.contains("No operations found"), 
                "Output should indicate no operations found");
    }

    @Test
    @DisplayName("Should handle filter with no matches gracefully")
    void shouldHandleFilterWithNoMatchesGracefully() {
        // Given
        List<OperationRecord> mockOperations = createMockOperations(5);
        when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);
        command.setFilter("NON_EXISTENT_TYPE");

        // When
        int exitCode = command.call();

        // Then
        assertEquals(0, exitCode);
        
        // Verify operation tracked with filter
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), captor.capture());
        
        Map<String, Object> params = captor.getValue();
        assertTrue(params.containsKey("filter"), "Parameters should include filter");
        
        // Verify appropriate message
        String output = outputCaptor.toString();
        assertTrue(output.contains("No operations found") || output.contains("No matching operations"), 
                "Output should indicate no matching operations");
    }

    @Test
    @DisplayName("Should handle service exceptions gracefully")
    void shouldHandleServiceExceptionsGracefully() {
        // Given
        when(mockMetadataService.getRecentOperations(anyInt()))
            .thenThrow(new RuntimeException("Service error"));

        // When
        int exitCode = command.call();

        // Then
        assertNotEquals(0, exitCode);
        
        // Verify operation still started but should be marked as failed
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), any());
        
        // Verify error message
        String error = errorCaptor.toString();
        assertTrue(error.contains("Error") && error.contains("Service error"), 
                "Error output should contain the exception message");
    }

    @Test
    @DisplayName("Should respect system configuration for default limit")
    void shouldRespectSystemConfigurationForDefaultLimit() {
        // Given
        when(mockConfigurationService.getIntValue(eq("operations.default_limit"), anyInt())).thenReturn(25);
        List<OperationRecord> mockOperations = createMockOperations(25);
        when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);
        
        // Create a new command to pick up the config
        OperationsCommand configAwareCommand = new OperationsCommand(mockServiceManager);

        // When
        int exitCode = configAwareCommand.call();

        // Then
        assertEquals(0, exitCode);
        
        // Verify service called with configured limit
        verify(mockMetadataService).getRecentOperations(eq(25));
        
        // Verify operation tracked with configured limit
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), captor.capture());
        
        Map<String, Object> params = captor.getValue();
        assertTrue(params.containsKey("limit"), "Parameters should include limit");
        assertEquals(25, params.get("limit"), "Limit parameter should match configured value");
    }

    @ParameterizedTest
    @ValueSource(ints = {5, 15, 30})
    @DisplayName("Should respect the recent parameter for number of operations")
    void shouldRespectRecentParameterForNumberOfOperations(int recentCount) {
        // Given
        List<OperationRecord> mockOperations = createMockOperations(recentCount);
        when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);
        command.setRecent(recentCount);

        // When
        int exitCode = command.call();

        // Then
        assertEquals(0, exitCode);
        
        // Verify service called with recent count
        verify(mockMetadataService).getRecentOperations(eq(recentCount));
        
        // Verify operation tracked with recent count
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockMetadataService).recordOperation(eq("command.operations"), eq("LIST_OPERATIONS"), captor.capture());
        
        Map<String, Object> params = captor.getValue();
        assertTrue(params.containsKey("recent"), "Parameters should include recent");
        assertEquals(recentCount, params.get("recent"), "Recent parameter should match input");
    }

    @Test
    @DisplayName("Should not track operation when displaying help")
    void shouldNotTrackOperationWhenDisplayingHelp() {
        // Given
        command.setHelp(true);

        // When
        int exitCode = command.call();

        // Then
        assertEquals(0, exitCode);
        
        // Verify no operation tracking
        verify(mockMetadataService, never()).recordOperation(anyString(), anyString(), any());
        
        // Verify help output
        String output = outputCaptor.toString();
        assertTrue(output.contains("Usage:") && output.contains("Options:"), 
                "Output should contain usage instructions");
    }

    // Helper method to create mock operations
    private List<OperationRecord> createMockOperations(int count) {
        List<OperationRecord> operations = new ArrayList<>();
        String[] types = {"ADD_ITEM", "UPDATE_ITEM", "LIST_ITEMS", "VIEW_ITEM", "DELETE_ITEM"};
        String[] statuses = {"COMPLETED", "FAILED", "IN_PROGRESS"};
        
        for (int i = 0; i < count; i++) {
            String id = "op-" + (i + 1);
            String type = types[i % types.length];
            String status = statuses[i % statuses.length];
            Instant startTime = Instant.now().minus(i, ChronoUnit.HOURS);
            Instant endTime = status.equals("IN_PROGRESS") ? null : startTime.plus(5, ChronoUnit.MINUTES);
            
            Map<String, Object> params = new HashMap<>();
            params.put("param1", "value" + i);
            params.put("count", i);
            
            Map<String, Object> results = new HashMap<>();
            if (status.equals("COMPLETED")) {
                results.put("result", "success");
                results.put("itemCount", i + 1);
            }
            
            String errorDetails = status.equals("FAILED") ? "Error occurred: test error " + i : null;
            
            OperationRecord record = new OperationRecord(id, type, status, startTime, endTime, params, results, errorDetails);
            operations.add(record);
        }
        
        return operations;
    }
}