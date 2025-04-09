/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.command.impl;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.service.DiagnosticsService;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for AdminDiagnosticsCommand.
 */
@ExtendWith(MockitoExtension.class)
class AdminDiagnosticsCommandTest {

    private AdminDiagnosticsCommand adminDiagnosticsCommand;
    @Mock
    private ServiceManager mockServiceManager;
    @Mock
    private DiagnosticsService mockDiagnosticsService;

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        // Set up output stream capture
        originalOut = System.out;
        originalErr = System.err;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Set up service manager
        when(mockServiceManager.getDiagnosticsService()).thenReturn(mockDiagnosticsService);
        
        // Create the command
        adminDiagnosticsCommand = new AdminDiagnosticsCommand(mockServiceManager);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    /**
     * Helper method to set up scanner input for interactive tests.
     */
    private void setInput(String input) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
            Scanner scanner = new Scanner(in);
            Field scannerField = AdminDiagnosticsCommand.class.getDeclaredField("scanner");
            scannerField.setAccessible(true);
            scannerField.set(adminDiagnosticsCommand, scanner);
        } catch (Exception e) {
            fail("Failed to set up scanner input: " + e.getMessage());
        }
    }

    /**
     * Helper method to clear captured console output.
     */
    private void clearOutput() {
        outContent.reset();
        errContent.reset();
    }

    @Nested
    @DisplayName("Help Documentation Tests")
    class HelpDocumentationTests {
        
        @Test
        @DisplayName("call should display help when no operation is specified")
        void callShouldDisplayHelpWhenNoOperationSpecified() {
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(output.contains("Usage: rin admin diagnostics <operation>"), 
                       "Should display usage information");
            assertTrue(output.contains("Operations:"), 
                       "Should list available operations");
            assertTrue(output.contains("run"), 
                       "Should include run operation");
            assertTrue(output.contains("schedule"), 
                       "Should include schedule operation");
            assertTrue(output.contains("database"), 
                       "Should include database operation");
        }
        
        @Test
        @DisplayName("call with 'help' operation should display help and return success")
        void callWithHelpOperationShouldDisplayHelpAndReturnSuccess() {
            // Arrange
            adminDiagnosticsCommand.setOperation("help");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains("Usage: rin admin diagnostics <operation>"), 
                       "Should display usage information");
            assertTrue(output.contains("Options for 'run':"), 
                       "Should include options for run operation");
        }
        
        @Test
        @DisplayName("call with unknown operation should display help and return error")
        void callWithUnknownOperationShouldDisplayHelpAndReturnError() {
            // Arrange
            adminDiagnosticsCommand.setOperation("unknown");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String output = outContent.toString();
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Unknown diagnostics operation: unknown"), 
                       "Should indicate unknown operation");
            assertTrue(output.contains("Usage: rin admin diagnostics <operation>"), 
                       "Should display usage information");
        }
        
        @Test
        @DisplayName("call should display error when diagnostics service is not available")
        void callShouldDisplayErrorWhenDiagnosticsServiceIsNotAvailable() {
            // Arrange
            when(mockServiceManager.getDiagnosticsService()).thenReturn(null);
            adminDiagnosticsCommand.setOperation("run");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Diagnostics service is not available."), 
                       "Should indicate diagnostics service is not available");
        }
    }

    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("run operation should execute diagnostics")
        void runOperationShouldExecuteDiagnostics() {
            // Arrange
            String mockResults = 
                "=== System Diagnostics Results ===\n" +
                "API Server Status: OK\n" +
                "Database Status: OK\n" +
                "Storage Status: OK\n" +
                "Memory Status: OK\n" +
                "Thread Pools: OK\n" +
                "Network Status: OK\n" +
                "\nAll diagnostics passed successfully!";
            
            when(mockDiagnosticsService.runDiagnostics(false)).thenReturn(mockResults);
            adminDiagnosticsCommand.setOperation("run");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains(mockResults), 
                       "Should display diagnostic results returned by service");
            verify(mockDiagnosticsService).runDiagnostics(false);
        }
        
        @Test
        @DisplayName("run operation with full flag should execute full diagnostics")
        void runOperationWithFullFlagShouldExecuteFullDiagnostics() {
            // Arrange
            String mockDetailedResults = 
                "=== Full System Diagnostics Results ===\n" +
                "API Server: OK (Response time: 30ms, Endpoints: 25/25 available)\n" +
                "Database: OK (Response time: 45ms, Connections: 5/100, Queries: 1250/s)\n" +
                "Storage: OK (Usage: 250GB/1TB, Read: 50MB/s, Write: 25MB/s)\n" +
                "Memory: OK (Usage: 4.5GB/16GB, JVM Heap: 2GB/8GB)\n" +
                "Thread Pools: OK (Active: 23, Queued: 5, Available: 172)\n" +
                "Network: OK (Connections: 45, Throughput: 15MB/s)\n" +
                "\nAll diagnostics passed successfully!";
            
            when(mockDiagnosticsService.runDiagnostics(true)).thenReturn(mockDetailedResults);
            adminDiagnosticsCommand.setOperation("run");
            adminDiagnosticsCommand.setArgs(new String[]{"--full"});
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains(mockDetailedResults), 
                       "Should display full diagnostic results returned by service");
            verify(mockDiagnosticsService).runDiagnostics(true);
        }
        
        @Test
        @DisplayName("database operation with analyze flag should analyze database performance")
        void databaseOperationWithAnalyzeFlagShouldAnalyzeDatabasePerformance() {
            // Arrange
            String mockDbReport = 
                "=== Database Performance Analysis ===\n" +
                "Query Performance: Excellent\n" +
                "Index Usage: Good\n" +
                "Connection Pool: Optimal\n" +
                "Slow Queries: 0\n" +
                "Lock Contention: None\n";
            
            when(mockDiagnosticsService.analyzeDatabasePerformance()).thenReturn(mockDbReport);
            adminDiagnosticsCommand.setOperation("database");
            adminDiagnosticsCommand.setArgs(new String[]{"--analyze"});
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains(mockDbReport), 
                       "Should display database analysis report returned by service");
            verify(mockDiagnosticsService).analyzeDatabasePerformance();
        }
        
        @Test
        @DisplayName("schedule list operation should display scheduled diagnostics")
        void scheduleListOperationShouldDisplayScheduledDiagnostics() {
            // Arrange
            String mockSchedules = 
                "=== Scheduled Diagnostics ===\n" +
                "1. Daily API Check (ID: diag-001)\n" +
                "   Schedule: daily at 02:00\n" +
                "   Checks: api\n" +
                "   Recipients: admin@example.com\n" +
                "\n" +
                "2. Weekly Full Check (ID: diag-002)\n" +
                "   Schedule: weekly at 04:00\n" +
                "   Checks: all\n" +
                "   Recipients: admin@example.com, ops@example.com\n";
            
            when(mockDiagnosticsService.listScheduledDiagnostics()).thenReturn(mockSchedules);
            adminDiagnosticsCommand.setOperation("schedule");
            adminDiagnosticsCommand.setArgs(new String[]{"list"});
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains(mockSchedules), 
                       "Should display scheduled diagnostics returned by service");
            verify(mockDiagnosticsService).listScheduledDiagnostics();
        }
        
        @Test
        @DisplayName("schedule operation should allow scheduling diagnostics")
        void scheduleOperationShouldAllowSchedulingDiagnostics() {
            // Arrange
            String mockTaskId = "diag-003";
            List<String> expectedChecks = Arrays.asList("api", "database");
            List<String> expectedRecipients = Arrays.asList("admin@example.com", "dev@example.com");
            
            when(mockDiagnosticsService.scheduleDiagnostics(
                eq(expectedChecks), 
                eq("daily"), 
                eq("03:30"), 
                eq(expectedRecipients)
            )).thenReturn(mockTaskId);
            
            adminDiagnosticsCommand.setOperation("schedule");
            
            // Setup scanner input for interactive prompt
            setInput("api, database\n2\n03:30\nadmin@example.com, dev@example.com\n");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains("Diagnostic task scheduled successfully!"), 
                       "Should confirm task scheduling");
            assertTrue(output.contains("Task ID: " + mockTaskId), 
                       "Should display the task ID");
            
            verify(mockDiagnosticsService).scheduleDiagnostics(
                eq(expectedChecks), 
                eq("daily"), 
                eq("03:30"), 
                eq(expectedRecipients)
            );
        }
        
        @Test
        @DisplayName("schedule operation should accept 'all' for check types")
        void scheduleOperationShouldAcceptAllForCheckTypes() {
            // Arrange
            String mockTaskId = "diag-004";
            List<String> expectedChecks = Arrays.asList("all");
            List<String> expectedRecipients = Arrays.asList("admin@example.com");
            
            when(mockDiagnosticsService.scheduleDiagnostics(
                eq(expectedChecks), 
                eq("weekly"), 
                eq("02:00"), 
                eq(expectedRecipients)
            )).thenReturn(mockTaskId);
            
            adminDiagnosticsCommand.setOperation("schedule");
            
            // Setup scanner input for interactive prompt
            setInput("all\n3\n\nadmin@example.com\n");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains("Diagnostic task scheduled successfully!"), 
                       "Should confirm task scheduling");
            
            verify(mockDiagnosticsService).scheduleDiagnostics(
                eq(expectedChecks), 
                eq("weekly"), 
                eq("02:00"), 
                eq(expectedRecipients)
            );
        }
        
        @Test
        @DisplayName("warning resolve operation should display and resolve warning")
        void warningResolveOperationShouldDisplayAndResolveWarning() {
            // Arrange
            String warningId = "warn-001";
            Map<String, String> mockWarningDetails = new HashMap<>();
            mockWarningDetails.put("type", "Memory Usage");
            mockWarningDetails.put("timestamp", "2025-04-08 10:15:30");
            mockWarningDetails.put("severity", "Medium");
            mockWarningDetails.put("description", "Memory usage exceeded 80% threshold");
            
            List<String> mockActions = Arrays.asList(
                "Analyze memory usage", 
                "Reclaim unused memory", 
                "Increase memory allocation"
            );
            
            when(mockDiagnosticsService.getWarningDetails(warningId)).thenReturn(mockWarningDetails);
            when(mockDiagnosticsService.getAvailableWarningActions(warningId)).thenReturn(mockActions);
            when(mockDiagnosticsService.performWarningAction(eq(warningId), eq("Reclaim unused memory")))
                .thenReturn(true);
            
            adminDiagnosticsCommand.setOperation("warning");
            adminDiagnosticsCommand.setArgs(new String[]{"resolve", "--id=" + warningId});
            
            // Setup scanner input for interactive prompt
            setInput("2\n");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            
            // Verify warning details are displayed
            assertTrue(output.contains("Warning Resolution: " + warningId), 
                       "Should display warning resolution header");
            assertTrue(output.contains("Warning Type: Memory Usage"), 
                       "Should display warning type");
            assertTrue(output.contains("Severity: Medium"), 
                       "Should display warning severity");
            
            // Verify action selection and execution
            assertTrue(output.contains("Available Actions:"), 
                       "Should display available actions");
            assertTrue(output.contains("Reclaim unused memory"), 
                       "Should list the selected action");
            assertTrue(output.contains("Performing action: Reclaim unused memory"), 
                       "Should confirm selected action");
            assertTrue(output.contains("Action performed successfully!"), 
                       "Should confirm action success");
            
            verify(mockDiagnosticsService).getWarningDetails(warningId);
            verify(mockDiagnosticsService).getAvailableWarningActions(warningId);
            verify(mockDiagnosticsService).performWarningAction(warningId, "Reclaim unused memory");
        }
        
        @Test
        @DisplayName("action operation with memory-reclaim flag should reclaim memory")
        void actionOperationWithMemoryReclaimFlagShouldReclaimMemory() {
            // Arrange
            when(mockDiagnosticsService.performMemoryReclamation()).thenReturn(true);
            adminDiagnosticsCommand.setOperation("action");
            adminDiagnosticsCommand.setArgs(new String[]{"--memory-reclaim"});
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains("Memory reclamation completed successfully!"), 
                       "Should confirm memory reclamation");
            verify(mockDiagnosticsService).performMemoryReclamation();
        }
        
        @Test
        @DisplayName("schedule operation should use default values when input is empty")
        void scheduleOperationShouldUseDefaultValuesWhenInputIsEmpty() {
            // Arrange
            String mockTaskId = "diag-005";
            List<String> expectedChecks = Arrays.asList("api", "database");
            List<String> expectedRecipients = Arrays.asList("admin@example.com");
            
            when(mockDiagnosticsService.scheduleDiagnostics(
                eq(expectedChecks), 
                eq("daily"), 
                eq("02:00"), 
                eq(expectedRecipients)
            )).thenReturn(mockTaskId);
            
            adminDiagnosticsCommand.setOperation("schedule");
            
            // Setup scanner input for interactive prompt with empty inputs for defaults
            setInput("api, database\n\n\nadmin@example.com\n");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains("Diagnostic task scheduled successfully!"), 
                       "Should confirm task scheduling");
            
            verify(mockDiagnosticsService).scheduleDiagnostics(
                eq(expectedChecks), 
                eq("daily"), 
                eq("02:00"), 
                eq(expectedRecipients)
            );
        }
        
        @Test
        @DisplayName("schedule operation should accept frequency name instead of number")
        void scheduleOperationShouldAcceptFrequencyNameInsteadOfNumber() {
            // Arrange
            String mockTaskId = "diag-006";
            List<String> expectedChecks = Arrays.asList("api");
            List<String> expectedRecipients = Arrays.asList("admin@example.com");
            
            when(mockDiagnosticsService.scheduleDiagnostics(
                eq(expectedChecks), 
                eq("monthly"), 
                eq("03:00"), 
                eq(expectedRecipients)
            )).thenReturn(mockTaskId);
            
            adminDiagnosticsCommand.setOperation("schedule");
            
            // Setup scanner input for interactive prompt with frequency name
            setInput("api\nmonthly\n03:00\nadmin@example.com\n");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains("Diagnostic task scheduled successfully!"), 
                       "Should confirm task scheduling");
            
            verify(mockDiagnosticsService).scheduleDiagnostics(
                eq(expectedChecks), 
                eq("monthly"), 
                eq("03:00"), 
                eq(expectedRecipients)
            );
        }
    }

    @Nested
    @DisplayName("Negative Test Cases")
    class NegativeTestCases {
        
        @Test
        @DisplayName("run operation should handle service exceptions")
        void runOperationShouldHandleServiceExceptions() {
            // Arrange
            when(mockDiagnosticsService.runDiagnostics(anyBoolean()))
                .thenThrow(new RuntimeException("Service unavailable"));
            adminDiagnosticsCommand.setOperation("run");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error running diagnostics: Service unavailable"), 
                       "Should display error message from service");
        }
        
        @Test
        @DisplayName("database operation should reject missing analyze flag")
        void databaseOperationShouldRejectMissingAnalyzeFlag() {
            // Arrange
            adminDiagnosticsCommand.setOperation("database");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Missing required flag --analyze"), 
                       "Should indicate missing required flag");
        }
        
        @Test
        @DisplayName("database operation should handle service exceptions")
        void databaseOperationShouldHandleServiceExceptions() {
            // Arrange
            when(mockDiagnosticsService.analyzeDatabasePerformance())
                .thenThrow(new RuntimeException("Database analysis failed"));
            adminDiagnosticsCommand.setOperation("database");
            adminDiagnosticsCommand.setArgs(new String[]{"--analyze"});
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error analyzing database performance: Database analysis failed"), 
                       "Should display error message from service");
        }
        
        @Test
        @DisplayName("schedule operation should reject empty check types")
        void scheduleOperationShouldRejectEmptyCheckTypes() {
            // Arrange
            adminDiagnosticsCommand.setOperation("schedule");
            
            // Setup scanner input for interactive prompt with empty check types
            setInput("\n");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Check types cannot be empty"), 
                       "Should indicate empty check types");
        }
        
        @Test
        @DisplayName("schedule operation should handle invalid frequency selection")
        void scheduleOperationShouldHandleInvalidFrequencySelection() {
            // Arrange
            adminDiagnosticsCommand.setOperation("schedule");
            
            // Setup scanner input for interactive prompt with invalid frequency
            setInput("api\ninvalid\n");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            String output = outContent.toString();
            
            // In this case, the command might not fail but should show a warning
            // and use the default frequency
            assertTrue(error.contains("Error: Invalid frequency") || 
                       output.contains("Invalid frequency"), 
                       "Should indicate invalid frequency");
        }
        
        @Test
        @DisplayName("schedule operation should handle service exceptions")
        void scheduleOperationShouldHandleServiceExceptions() {
            // Arrange
            when(mockDiagnosticsService.scheduleDiagnostics(anyList(), anyString(), anyString(), anyList()))
                .thenThrow(new RuntimeException("Scheduling failed"));
            adminDiagnosticsCommand.setOperation("schedule");
            
            // Setup scanner input for interactive prompt
            setInput("api\ndaily\n02:00\nadmin@example.com\n");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error scheduling diagnostics: Scheduling failed"), 
                       "Should display error message from service");
        }
        
        @Test
        @DisplayName("schedule list operation should handle service exceptions")
        void scheduleListOperationShouldHandleServiceExceptions() {
            // Arrange
            when(mockDiagnosticsService.listScheduledDiagnostics())
                .thenThrow(new RuntimeException("List retrieval failed"));
            adminDiagnosticsCommand.setOperation("schedule");
            adminDiagnosticsCommand.setArgs(new String[]{"list"});
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error listing scheduled diagnostics: List retrieval failed"), 
                       "Should display error message from service");
        }
        
        @Test
        @DisplayName("warning operation should reject missing resolve subcommand")
        void warningOperationShouldRejectMissingResolveSubcommand() {
            // Arrange
            adminDiagnosticsCommand.setOperation("warning");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Missing 'resolve' subcommand"), 
                       "Should indicate missing resolve subcommand");
        }
        
        @Test
        @DisplayName("warning resolve operation should reject missing id parameter")
        void warningResolveOperationShouldRejectMissingIdParameter() {
            // Arrange
            adminDiagnosticsCommand.setOperation("warning");
            adminDiagnosticsCommand.setArgs(new String[]{"resolve"});
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Missing required parameter --id"), 
                       "Should indicate missing ID parameter");
        }
        
        @Test
        @DisplayName("warning resolve operation should handle non-existent warning")
        void warningResolveOperationShouldHandleNonExistentWarning() {
            // Arrange
            String nonExistentId = "warn-999";
            when(mockDiagnosticsService.getWarningDetails(nonExistentId))
                .thenReturn(new HashMap<>());
            
            adminDiagnosticsCommand.setOperation("warning");
            adminDiagnosticsCommand.setArgs(new String[]{"resolve", "--id=" + nonExistentId});
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Warning not found: " + nonExistentId), 
                       "Should indicate warning not found");
        }
        
        @Test
        @DisplayName("warning resolve operation should handle invalid action selection")
        void warningResolveOperationShouldHandleInvalidActionSelection() {
            // Arrange
            String warningId = "warn-001";
            Map<String, String> mockWarningDetails = new HashMap<>();
            mockWarningDetails.put("type", "Memory Usage");
            mockWarningDetails.put("severity", "Medium");
            
            List<String> mockActions = Arrays.asList("Action 1", "Action 2");
            
            when(mockDiagnosticsService.getWarningDetails(warningId)).thenReturn(mockWarningDetails);
            when(mockDiagnosticsService.getAvailableWarningActions(warningId)).thenReturn(mockActions);
            
            adminDiagnosticsCommand.setOperation("warning");
            adminDiagnosticsCommand.setArgs(new String[]{"resolve", "--id=" + warningId});
            
            // Setup scanner input for interactive prompt with invalid selection
            setInput("99\n");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Invalid selection"), 
                       "Should indicate invalid action selection");
        }
        
        @Test
        @DisplayName("warning resolve operation should handle non-numeric action selection")
        void warningResolveOperationShouldHandleNonNumericActionSelection() {
            // Arrange
            String warningId = "warn-001";
            Map<String, String> mockWarningDetails = new HashMap<>();
            mockWarningDetails.put("type", "Memory Usage");
            mockWarningDetails.put("severity", "Medium");
            
            List<String> mockActions = Arrays.asList("Action 1", "Action 2");
            
            when(mockDiagnosticsService.getWarningDetails(warningId)).thenReturn(mockWarningDetails);
            when(mockDiagnosticsService.getAvailableWarningActions(warningId)).thenReturn(mockActions);
            
            adminDiagnosticsCommand.setOperation("warning");
            adminDiagnosticsCommand.setArgs(new String[]{"resolve", "--id=" + warningId});
            
            // Setup scanner input for interactive prompt with non-numeric selection
            setInput("abc\n");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Invalid selection"), 
                       "Should indicate invalid action selection");
        }
        
        @Test
        @DisplayName("warning resolve operation should handle action failure")
        void warningResolveOperationShouldHandleActionFailure() {
            // Arrange
            String warningId = "warn-001";
            Map<String, String> mockWarningDetails = new HashMap<>();
            mockWarningDetails.put("type", "Memory Usage");
            mockWarningDetails.put("severity", "Medium");
            
            List<String> mockActions = Arrays.asList("Action 1", "Action 2");
            
            when(mockDiagnosticsService.getWarningDetails(warningId)).thenReturn(mockWarningDetails);
            when(mockDiagnosticsService.getAvailableWarningActions(warningId)).thenReturn(mockActions);
            when(mockDiagnosticsService.performWarningAction(anyString(), anyString())).thenReturn(false);
            
            adminDiagnosticsCommand.setOperation("warning");
            adminDiagnosticsCommand.setArgs(new String[]{"resolve", "--id=" + warningId});
            
            // Setup scanner input for interactive prompt
            setInput("1\n");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Failed to perform action"), 
                       "Should indicate action failure");
        }
        
        @Test
        @DisplayName("warning resolve operation should handle service exceptions")
        void warningResolveOperationShouldHandleServiceExceptions() {
            // Arrange
            String warningId = "warn-001";
            when(mockDiagnosticsService.getWarningDetails(warningId))
                .thenThrow(new RuntimeException("Service unavailable"));
            
            adminDiagnosticsCommand.setOperation("warning");
            adminDiagnosticsCommand.setArgs(new String[]{"resolve", "--id=" + warningId});
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error resolving warning: Service unavailable"), 
                       "Should display error message from service");
        }
        
        @Test
        @DisplayName("action operation should reject missing action flag")
        void actionOperationShouldRejectMissingActionFlag() {
            // Arrange
            adminDiagnosticsCommand.setOperation("action");
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Missing required action flag"), 
                       "Should indicate missing action flag");
            assertTrue(error.toString().contains("--memory-reclaim") || 
                       outContent.toString().contains("--memory-reclaim"), 
                       "Should suggest available actions");
        }
        
        @Test
        @DisplayName("action operation should handle memory reclamation failure")
        void actionOperationShouldHandleMemoryReclamationFailure() {
            // Arrange
            when(mockDiagnosticsService.performMemoryReclamation()).thenReturn(false);
            adminDiagnosticsCommand.setOperation("action");
            adminDiagnosticsCommand.setArgs(new String[]{"--memory-reclaim"});
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Memory reclamation failed"), 
                       "Should indicate memory reclamation failure");
        }
        
        @Test
        @DisplayName("action operation should handle service exceptions")
        void actionOperationShouldHandleServiceExceptions() {
            // Arrange
            when(mockDiagnosticsService.performMemoryReclamation())
                .thenThrow(new RuntimeException("Memory reclamation failed"));
            adminDiagnosticsCommand.setOperation("action");
            adminDiagnosticsCommand.setArgs(new String[]{"--memory-reclaim"});
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error performing memory reclamation: Memory reclamation failed"), 
                       "Should display error message from service");
        }
    }

    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should use diagnosticsService.runDiagnostics for run operation")
        void shouldUseRunDiagnosticsServiceForRunOperation() {
            // Arrange
            adminDiagnosticsCommand.setOperation("run");
            
            // Act
            adminDiagnosticsCommand.call();
            
            // Assert
            verify(mockDiagnosticsService).runDiagnostics(false);
        }
        
        @Test
        @DisplayName("Should use diagnosticsService.analyzeDatabasePerformance for database operation")
        void shouldUseAnalyzeDatabasePerformanceServiceForDatabaseOperation() {
            // Arrange
            adminDiagnosticsCommand.setOperation("database");
            adminDiagnosticsCommand.setArgs(new String[]{"--analyze"});
            
            // Act
            adminDiagnosticsCommand.call();
            
            // Assert
            verify(mockDiagnosticsService).analyzeDatabasePerformance();
        }
        
        @Test
        @DisplayName("Should use diagnosticsService.listScheduledDiagnostics for schedule list operation")
        void shouldUseListScheduledDiagnosticsServiceForScheduleListOperation() {
            // Arrange
            adminDiagnosticsCommand.setOperation("schedule");
            adminDiagnosticsCommand.setArgs(new String[]{"list"});
            
            // Act
            adminDiagnosticsCommand.call();
            
            // Assert
            verify(mockDiagnosticsService).listScheduledDiagnostics();
        }
        
        @Test
        @DisplayName("Should use diagnosticsService.scheduleDiagnostics for schedule operation")
        void shouldUseScheduleDiagnosticsServiceForScheduleOperation() {
            // Arrange
            String mockTaskId = "diag-test";
            when(mockDiagnosticsService.scheduleDiagnostics(anyList(), anyString(), anyString(), anyList()))
                .thenReturn(mockTaskId);
                
            adminDiagnosticsCommand.setOperation("schedule");
            setInput("api\ndaily\n02:00\nadmin@example.com\n");
            
            // Act
            adminDiagnosticsCommand.call();
            
            // Assert
            verify(mockDiagnosticsService).scheduleDiagnostics(
                eq(Arrays.asList("api")), 
                eq("daily"), 
                eq("02:00"), 
                eq(Arrays.asList("admin@example.com"))
            );
        }
        
        @Test
        @DisplayName("Should use multiple diagnosticsService methods for warning resolve operation")
        void shouldUseMultipleDiagnosticsServiceMethodsForWarningResolveOperation() {
            // Arrange
            String warningId = "warn-test";
            
            // Setup mock responses
            Map<String, String> mockDetails = new HashMap<>();
            mockDetails.put("type", "Test Warning");
            
            List<String> mockActions = Arrays.asList("Test Action");
            
            when(mockDiagnosticsService.getWarningDetails(warningId)).thenReturn(mockDetails);
            when(mockDiagnosticsService.getAvailableWarningActions(warningId)).thenReturn(mockActions);
            when(mockDiagnosticsService.performWarningAction(anyString(), anyString())).thenReturn(true);
            
            adminDiagnosticsCommand.setOperation("warning");
            adminDiagnosticsCommand.setArgs(new String[]{"resolve", "--id=" + warningId});
            setInput("1\n");
            
            // Act
            adminDiagnosticsCommand.call();
            
            // Assert
            verify(mockDiagnosticsService).getWarningDetails(warningId);
            verify(mockDiagnosticsService).getAvailableWarningActions(warningId);
            verify(mockDiagnosticsService).performWarningAction(eq(warningId), eq("Test Action"));
        }
        
        @Test
        @DisplayName("Should use diagnosticsService.performMemoryReclamation for action operation with memory-reclaim flag")
        void shouldUsePerformMemoryReclamationServiceForActionOperation() {
            // Arrange
            when(mockDiagnosticsService.performMemoryReclamation()).thenReturn(true);
            adminDiagnosticsCommand.setOperation("action");
            adminDiagnosticsCommand.setArgs(new String[]{"--memory-reclaim"});
            
            // Act
            adminDiagnosticsCommand.call();
            
            // Assert
            verify(mockDiagnosticsService).performMemoryReclamation();
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Should handle full workflow for running diagnostics and addressing warning")
        void shouldHandleFullWorkflowForRunningDiagnosticsAndAddressingWarning() {
            // Arrange - mock responses for a multi-step workflow
            String warningId = "diag-warning-001";
            
            // Step 1: Run diagnostics that find an issue
            String diagnosticsOutput = 
                "=== System Diagnostics Results ===\n" +
                "API Server Status: OK\n" +
                "Database Status: OK\n" +
                "Storage Status: OK\n" +
                "Memory Status: WARNING\n" +
                "Thread Pools: OK\n" +
                "Network Status: OK\n" +
                "\nDiagnostics complete with warnings:\n" +
                "- Memory usage is high (85%) - Warning ID: " + warningId;
                
            when(mockDiagnosticsService.runDiagnostics(anyBoolean())).thenReturn(diagnosticsOutput);
            
            // Step 2: Get warning details
            Map<String, String> warningDetails = new HashMap<>();
            warningDetails.put("type", "Memory Usage");
            warningDetails.put("timestamp", "2025-04-08 10:15:30");
            warningDetails.put("severity", "Medium");
            warningDetails.put("description", "Memory usage exceeded 80% threshold");
            
            when(mockDiagnosticsService.getWarningDetails(warningId)).thenReturn(warningDetails);
            
            // Step 3: Get available actions
            List<String> actions = Arrays.asList("Reclaim unused memory");
            when(mockDiagnosticsService.getAvailableWarningActions(warningId)).thenReturn(actions);
            
            // Step 4: Perform action
            when(mockDiagnosticsService.performWarningAction(eq(warningId), anyString())).thenReturn(true);
            
            // Step 5: Run diagnostics again to confirm resolution
            String followupDiagnostics = 
                "=== System Diagnostics Results ===\n" +
                "API Server Status: OK\n" +
                "Database Status: OK\n" +
                "Storage Status: OK\n" +
                "Memory Status: OK\n" +
                "Thread Pools: OK\n" +
                "Network Status: OK\n" +
                "\nAll diagnostics passed successfully!";
                
            // Create new command instances for each step
            AdminDiagnosticsCommand runCommand = new AdminDiagnosticsCommand(mockServiceManager);
            runCommand.setOperation("run");
            
            AdminDiagnosticsCommand warningCommand = new AdminDiagnosticsCommand(mockServiceManager);
            warningCommand.setOperation("warning");
            warningCommand.setArgs(new String[]{"resolve", "--id=" + warningId});
            
            // For this command, we'll override the diagnostics result for the second run
            AdminDiagnosticsCommand verifyCommand = new AdminDiagnosticsCommand(mockServiceManager);
            verifyCommand.setOperation("run");
            
            // Mock the second call to runDiagnostics
            when(mockDiagnosticsService.runDiagnostics(false))
                .thenReturn(diagnosticsOutput)
                .thenReturn(followupDiagnostics);
                
            // Setup scanner input for warning resolution
            setInput("1\n");
            
            // Act - execute each step in sequence
            int runResult = runCommand.call();
            clearOutput();
            
            int warningResult = warningCommand.call();
            clearOutput();
            
            int verifyResult = verifyCommand.call();
            
            // Assert
            assertEquals(0, runResult, "Initial diagnostics should succeed");
            assertEquals(0, warningResult, "Warning resolution should succeed");
            assertEquals(0, verifyResult, "Follow-up diagnostics should succeed");
            
            // Verify the output contains confirmation of resolved issue
            String verifyOutput = outContent.toString();
            assertTrue(verifyOutput.contains("All diagnostics passed successfully"), 
                       "Final output should confirm all diagnostics passed");
            
            // Verify service calls
            verify(mockDiagnosticsService, times(2)).runDiagnostics(false);
            verify(mockDiagnosticsService).getWarningDetails(warningId);
            verify(mockDiagnosticsService).getAvailableWarningActions(warningId);
            verify(mockDiagnosticsService).performWarningAction(eq(warningId), eq("Reclaim unused memory"));
        }
        
        @Test
        @DisplayName("Should handle workflow for scheduling and listing diagnostics")
        void shouldHandleWorkflowForSchedulingAndListingDiagnostics() {
            // Arrange - mock responses for multi-step workflow
            String mockTaskId = "diag-schedule-001";
            
            // Step 1: Schedule new diagnostics
            when(mockDiagnosticsService.scheduleDiagnostics(
                anyList(), anyString(), anyString(), anyList()
            )).thenReturn(mockTaskId);
            
            // Step 2: List scheduled diagnostics including the new one
            String mockSchedulesList = 
                "=== Scheduled Diagnostics ===\n" +
                "1. Daily API and Database Check (ID: " + mockTaskId + ")\n" +
                "   Schedule: daily at 03:00\n" +
                "   Checks: api, database\n" +
                "   Recipients: admin@example.com\n";
                
            when(mockDiagnosticsService.listScheduledDiagnostics()).thenReturn(mockSchedulesList);
            
            // Create command instances for each step
            AdminDiagnosticsCommand scheduleCommand = new AdminDiagnosticsCommand(mockServiceManager);
            scheduleCommand.setOperation("schedule");
            
            AdminDiagnosticsCommand listCommand = new AdminDiagnosticsCommand(mockServiceManager);
            listCommand.setOperation("schedule");
            listCommand.setArgs(new String[]{"list"});
            
            // Setup scanner input for scheduling
            setInput("api, database\n2\n03:00\nadmin@example.com\n");
            
            // Act - execute each step in sequence
            int scheduleResult = scheduleCommand.call();
            clearOutput();
            
            int listResult = listCommand.call();
            
            // Assert
            assertEquals(0, scheduleResult, "Schedule command should succeed");
            assertEquals(0, listResult, "List command should succeed");
            
            // Verify the output includes the scheduled task
            String listOutput = outContent.toString();
            assertTrue(listOutput.contains(mockTaskId), 
                       "List output should include the newly scheduled task ID");
            assertTrue(listOutput.contains("api, database"), 
                       "List output should include the check types");
            
            // Verify service calls
            verify(mockDiagnosticsService).scheduleDiagnostics(
                eq(Arrays.asList("api", "database")), 
                eq("daily"), 
                eq("03:00"), 
                eq(Arrays.asList("admin@example.com"))
            );
            verify(mockDiagnosticsService).listScheduledDiagnostics();
        }
        
        @Test
        @DisplayName("Should handle workflow for database analysis")
        void shouldHandleWorkflowForDatabaseAnalysis() {
            // Arrange
            String mockDatabaseReport = 
                "=== Database Performance Analysis ===\n" +
                "Query Performance: Good\n" +
                "Index Usage: Excellent\n" +
                "Connection Pool: Optimal\n" +
                "Slow Queries: 2\n" +
                "Lock Contention: Low\n" +
                "\nRecommendations:\n" +
                "- Add index on users.last_login column\n" +
                "- Optimize query on work_items table\n";
                
            when(mockDiagnosticsService.analyzeDatabasePerformance()).thenReturn(mockDatabaseReport);
            
            adminDiagnosticsCommand.setOperation("database");
            adminDiagnosticsCommand.setArgs(new String[]{"--analyze"});
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Command should succeed");
            assertTrue(output.contains("Query Performance: Good"), 
                       "Output should include performance details");
            assertTrue(output.contains("Recommendations:"), 
                       "Output should include recommendations");
            
            verify(mockDiagnosticsService).analyzeDatabasePerformance();
        }
        
        @Test
        @DisplayName("Should handle workflow for memory reclamation action")
        void shouldHandleWorkflowForMemoryReclamationAction() {
            // Arrange
            when(mockDiagnosticsService.performMemoryReclamation()).thenReturn(true);
            
            adminDiagnosticsCommand.setOperation("action");
            adminDiagnosticsCommand.setArgs(new String[]{"--memory-reclaim"});
            
            // Act
            int result = adminDiagnosticsCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Command should succeed");
            assertTrue(output.contains("Memory reclamation completed successfully!"), 
                       "Output should confirm successful memory reclamation");
            
            verify(mockDiagnosticsService).performMemoryReclamation();
        }
    }
}