package org.rinna.cli.command;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.service.AuthorizationService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.domain.model.OperationRecord;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for the OperationsCommand which provides visibility
 * into operation tracking within the system.
 */
public class OperationsCommandTest {

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
        when(mockAuthorizationService.isAuthenticated()).thenReturn(true);
        when(mockAuthorizationService.hasPermission(anyString())).thenReturn(true);

        command = new OperationsCommand(mockServiceManager);
    }

    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        System.setErr(originalErr);
        closeable.close();
    }

    @Nested
    @DisplayName("Help Documentation Tests")
    class HelpTests {

        @Test
        @DisplayName("Should display help documentation when help flag is set")
        void shouldDisplayHelpDocumentation() {
            // Given
            command.setHelp(true);

            // When
            int exitCode = command.call();

            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            assertTrue(output.contains("Usage:"), "Help output should contain 'Usage:'");
            assertTrue(output.contains("operations"), "Help output should contain command name");
            assertTrue(output.contains("Options:"), "Help output should contain 'Options:'");
            
            // Help flag should not result in operation tracking
            verify(mockMetadataService, never()).recordOperation(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should include all available options in help output")
        void shouldIncludeAllOptionsInHelp() {
            // Given
            command.setHelp(true);

            // When
            int exitCode = command.call();

            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            
            // Check for all important command options
            assertTrue(output.contains("--limit"), "Help should mention limit option");
            assertTrue(output.contains("--filter"), "Help should mention filter option");
            assertTrue(output.contains("--json"), "Help should mention json option");
            assertTrue(output.contains("--verbose"), "Help should mention verbose option");
            assertTrue(output.contains("--recent"), "Help should mention recent option");
        }
    }

    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTests {

        @Test
        @DisplayName("Should list operations with default settings")
        void shouldListOperationsWithDefaultSettings() {
            // Given
            List<OperationRecord> mockOperations = createMockOperations(5);
            when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);

            // When
            int exitCode = command.call();

            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            
            // Verify that output contains operation information
            assertTrue(output.contains("Operation ID:"), "Output should contain operation IDs");
            assertTrue(output.contains("Type:"), "Output should contain operation types");
            assertTrue(output.contains("Status:"), "Output should contain operation status");
            
            // Verify service interaction
            verify(mockMetadataService).getRecentOperations(eq(10)); // default limit is 10
            
            // Verify operation tracking
            verify(mockMetadataService).recordOperation(eq("command.operations"), 
                                                       eq("LIST_OPERATIONS"), 
                                                       argThat(map -> map.containsKey("limit") && 
                                                                    ((int)map.get("limit")) == 10));
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
            
            // Verify service interaction with correct limit
            verify(mockMetadataService).getRecentOperations(eq(3));
            
            // Verify operation tracking with correct limit
            verify(mockMetadataService).recordOperation(eq("command.operations"), 
                                                       eq("LIST_OPERATIONS"), 
                                                       argThat(map -> map.containsKey("limit") && 
                                                                    ((int)map.get("limit")) == 3));
        }

        @Test
        @DisplayName("Should filter operations by type")
        void shouldFilterOperationsByType() {
            // Given
            List<OperationRecord> allOperations = createMockOperations(10);
            when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(allOperations);
            command.setFilter("ADD_ITEM");

            // When
            int exitCode = command.call();

            // Then
            assertEquals(0, exitCode);
            
            // Verify service interaction
            verify(mockMetadataService).getRecentOperations(anyInt());
            
            // Verify operation tracking with filter
            verify(mockMetadataService).recordOperation(eq("command.operations"), 
                                                       eq("LIST_OPERATIONS"), 
                                                       argThat(map -> map.containsKey("filter") && 
                                                                    map.get("filter").equals("ADD_ITEM")));
            
            // Check that output contains filtered content
            String output = outputCaptor.toString();
            assertTrue(output.contains("ADD_ITEM"), "Output should contain filtered operation type");
            assertFalse(output.contains("UPDATE_ITEM"), "Output should not contain filtered out operation types");
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
            String output = outputCaptor.toString();
            
            // Basic JSON validation
            assertTrue(output.contains("[") && output.contains("]"), "Output should be in JSON array format");
            assertTrue(output.contains("\"operationId\""), "JSON should contain operationId field");
            assertTrue(output.contains("\"operationType\""), "JSON should contain operationType field");
            assertTrue(output.contains("\"status\""), "JSON should contain status field");
            
            // Verify operation tracking with JSON format
            verify(mockMetadataService).recordOperation(eq("command.operations"), 
                                                       eq("LIST_OPERATIONS"), 
                                                       argThat(map -> map.containsKey("format") && 
                                                                    map.get("format").equals("json")));
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
            String output = outputCaptor.toString();
            
            // Check for verbose information
            assertTrue(output.contains("Parameters:"), "Verbose output should show parameters");
            assertTrue(output.contains("Results:"), "Verbose output should show results");
            assertTrue(output.contains("Error Details:"), "Verbose output should show error details");
            
            // Verify operation tracking with verbose mode
            verify(mockMetadataService).recordOperation(eq("command.operations"), 
                                                       eq("LIST_OPERATIONS"), 
                                                       argThat(map -> map.containsKey("verbose") && 
                                                                    ((boolean)map.get("verbose"))));
        }

        @ParameterizedTest
        @ValueSource(ints = {5, 10, 20})
        @DisplayName("Should show recent operations with custom recent count")
        void shouldShowRecentOperationsWithCustomCount(int recentCount) {
            // Given
            List<OperationRecord> mockOperations = createMockOperations(recentCount);
            when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);
            command.setRecent(recentCount);

            // When
            int exitCode = command.call();

            // Then
            assertEquals(0, exitCode);
            
            // Verify service interaction with correct count
            verify(mockMetadataService).getRecentOperations(eq(recentCount));
            
            // Verify operation tracking with recent count
            verify(mockMetadataService).recordOperation(eq("command.operations"), 
                                                       eq("LIST_OPERATIONS"), 
                                                       argThat(map -> map.containsKey("recent") && 
                                                                    ((int)map.get("recent")) == recentCount));
        }
    }

    @Nested
    @DisplayName("Negative Test Cases")
    class NegativeTests {

        @Test
        @DisplayName("Should handle authentication failure gracefully")
        void shouldHandleAuthenticationFailure() {
            // Given
            when(mockAuthorizationService.isAuthenticated()).thenReturn(false);

            // When
            int exitCode = command.call();

            // Then
            assertNotEquals(0, exitCode);
            String error = errorCaptor.toString();
            assertTrue(error.contains("authentication") || error.contains("logged in"),
                    "Error message should mention authentication issue");
            
            // No operation should be tracked for authentication failure
            verify(mockMetadataService, never()).recordOperation(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should handle permission failure gracefully")
        void shouldHandlePermissionFailure() {
            // Given
            when(mockAuthorizationService.hasPermission(anyString())).thenReturn(false);

            // When
            int exitCode = command.call();

            // Then
            assertNotEquals(0, exitCode);
            String error = errorCaptor.toString();
            assertTrue(error.contains("permission") || error.contains("authorized"),
                    "Error message should mention permission issue");
            
            // No operation should be tracked for permission failure
            verify(mockMetadataService, never()).recordOperation(anyString(), anyString(), any());
        }

        @Test
        @DisplayName("Should handle service exceptions gracefully")
        void shouldHandleServiceExceptions() {
            // Given
            when(mockMetadataService.getRecentOperations(anyInt()))
                .thenThrow(new RuntimeException("Service error"));

            // When
            int exitCode = command.call();

            // Then
            assertNotEquals(0, exitCode);
            String error = errorCaptor.toString();
            assertTrue(error.contains("Error") && error.contains("Service error"),
                    "Error message should contain the exception message");
            
            // The initial operation should be started but marked as failed
            verify(mockMetadataService).recordOperation(eq("command.operations"), 
                                                      eq("LIST_OPERATIONS"), 
                                                      any());
        }

        @Test
        @DisplayName("Should handle empty operation list gracefully")
        void shouldHandleEmptyOperationList() {
            // Given
            when(mockMetadataService.getRecentOperations(anyInt()))
                .thenReturn(new ArrayList<>());

            // When
            int exitCode = command.call();

            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            assertTrue(output.contains("No operations found") || 
                       output.contains("No recent operations"),
                    "Should show a message about no operations");
            
            // Operation should still be tracked
            verify(mockMetadataService).recordOperation(eq("command.operations"), 
                                                      eq("LIST_OPERATIONS"), 
                                                      any());
        }

        @Test
        @DisplayName("Should handle invalid filter with no matches")
        void shouldHandleInvalidFilterWithNoMatches() {
            // Given
            List<OperationRecord> mockOperations = createMockOperations(5);
            when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);
            command.setFilter("NON_EXISTENT_TYPE");

            // When
            int exitCode = command.call();

            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            assertTrue(output.contains("No operations found") || 
                       output.contains("No matching operations"),
                    "Should show a message about no matching operations");
            
            // Operation should still be tracked
            verify(mockMetadataService).recordOperation(eq("command.operations"), 
                                                      eq("LIST_OPERATIONS"), 
                                                      argThat(map -> map.containsKey("filter") && 
                                                                   map.get("filter").equals("NON_EXISTENT_TYPE")));
        }
    }

    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {

        @Test
        @DisplayName("Should track own operation correctly")
        void shouldTrackOwnOperation() {
            // Given
            List<OperationRecord> mockOperations = createMockOperations(5);
            when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify operation tracking with parameters
            verify(mockMetadataService).recordOperation(
                eq("command.operations"),
                eq("LIST_OPERATIONS"),
                argThat(params -> 
                    params.containsKey("limit") && 
                    ((int)params.get("limit")) == 10
                )
            );
        }

        @Test
        @DisplayName("Should include all parameters in operation tracking")
        void shouldIncludeAllParametersInOperationTracking() {
            // Given
            List<OperationRecord> mockOperations = createMockOperations(5);
            when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);
            
            command.setLimit(15);
            command.setFilter("UPDATE_ITEM");
            command.setJsonOutput(true);
            command.setVerbose(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify operation tracking with all parameters
            verify(mockMetadataService).recordOperation(
                eq("command.operations"),
                eq("LIST_OPERATIONS"),
                argThat(params -> 
                    params.containsKey("limit") && ((int)params.get("limit")) == 15 &&
                    params.containsKey("filter") && params.get("filter").equals("UPDATE_ITEM") &&
                    params.containsKey("format") && params.get("format").equals("json") &&
                    params.containsKey("verbose") && ((boolean)params.get("verbose"))
                )
            );
        }

        @Test
        @DisplayName("Should respect value from constructor injection")
        void shouldRespectValueFromConstructorInjection() {
            // Given
            ServiceManager customMockServiceManager = mock(ServiceManager.class);
            MetadataService customMockMetadataService = mock(MetadataService.class);
            AuthorizationService customMockAuthService = mock(AuthorizationService.class);
            
            when(customMockServiceManager.getMetadataService()).thenReturn(customMockMetadataService);
            when(customMockServiceManager.getAuthorizationService()).thenReturn(customMockAuthService);
            when(customMockAuthService.isAuthenticated()).thenReturn(true);
            when(customMockAuthService.hasPermission(anyString())).thenReturn(true);
            
            List<OperationRecord> mockOperations = createMockOperations(5);
            when(customMockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);
            
            OperationsCommand customCommand = new OperationsCommand(customMockServiceManager);
            
            // When
            int exitCode = customCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify correct service was called
            verify(customMockMetadataService).getRecentOperations(anyInt());
            verify(mockMetadataService, never()).getRecentOperations(anyInt());
        }

        @ParameterizedTest
        @CsvSource({
            "text, false",
            "json, true"
        })
        @DisplayName("Should respect output format setting")
        void shouldRespectOutputFormatSetting(String format, boolean isJson) {
            // Given
            List<OperationRecord> mockOperations = createMockOperations(3);
            when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);
            
            command.setJsonOutput(isJson);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            
            if (isJson) {
                assertTrue(output.contains("{") && output.contains("}"),
                        "JSON output should contain object brackets");
                assertTrue(output.contains("\"operationId\""),
                        "JSON output should contain field names in quotes");
            } else {
                assertTrue(output.contains("Operation ID:"),
                        "Text output should contain field labels");
                assertFalse(output.contains("\"operationId\""),
                        "Text output should not contain JSON field names");
            }
            
            // Verify operation tracking with correct format
            verify(mockMetadataService).recordOperation(
                eq("command.operations"),
                eq("LIST_OPERATIONS"),
                argThat(params -> 
                    (!isJson && !params.containsKey("format")) ||
                    (isJson && params.containsKey("format") && params.get("format").equals("json"))
                )
            );
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should use static ServiceManager instance if none provided")
        void shouldUseStaticServiceManagerInstanceIfNoneProvided() {
            // We need a mock for the static method
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                // Given
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                List<OperationRecord> mockOperations = createMockOperations(3);
                when(mockMetadataService.getRecentOperations(anyInt())).thenReturn(mockOperations);
                
                // Create command using default constructor
                OperationsCommand defaultCommand = new OperationsCommand();
                
                // When
                int exitCode = defaultCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify static getInstance was called
                mockStatic.verify(ServiceManager::getInstance);
                
                // Verify service was called
                verify(mockMetadataService).getRecentOperations(anyInt());
            }
        }
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