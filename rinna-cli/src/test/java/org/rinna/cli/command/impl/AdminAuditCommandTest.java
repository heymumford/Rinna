/*
 * AdminAuditCommandTest - Tests for the AdminAuditCommand class
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.command.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rinna.cli.service.AuditService;
import org.rinna.cli.service.MockAuditService;
import org.rinna.cli.service.ServiceManager;

/**
 * Comprehensive test class for the AdminAuditCommand functionality.
 * Tests all aspects of the command following TDD principles.
 */
@DisplayName("AdminAuditCommand Tests")
class AdminAuditCommandTest {

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private InputStream originalIn;
    
    private ServiceManager mockServiceManager;
    private AuditService mockAuditService;
    
    /**
     * Sets up the test environment before each test.
     */
    @BeforeEach
    void setUp() {
        // Set up System.out/err capture
        originalOut = System.out;
        originalErr = System.err;
        originalIn = System.in;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Create mocks
        mockServiceManager = Mockito.mock(ServiceManager.class);
        mockAuditService = Mockito.mock(AuditService.class);
        
        // Configure mocks
        Mockito.when(mockServiceManager.getAuditService()).thenReturn(mockAuditService);
    }

    /**
     * Tears down the test environment after each test.
     */
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
    }
    
    /**
     * Helper method to set up input for interactive tests.
     *
     * @param input the input string
     */
    private void setInput(String input) {
        ByteArrayInputStream inputStream = new ByteArrayInputStream(input.getBytes());
        System.setIn(inputStream);
    }
    
    /**
     * Helper method to set a mocked scanner in the command instance.
     *
     * @param command the command to modify
     * @param scanner the scanner to set
     * @throws Exception if reflection fails
     */
    private void setScanner(AdminAuditCommand command, Scanner scanner) throws Exception {
        Field scannerField = AdminAuditCommand.class.getDeclaredField("scanner");
        scannerField.setAccessible(true);
        scannerField.set(command, scanner);
    }

    /**
     * Tests for the help documentation of the AdminAuditCommand.
     */
    @Nested
    @DisplayName("Help Documentation Tests")
    class HelpDocumentationTests {
        
        @Test
        @DisplayName("Should display help when no operation specified")
        void shouldDisplayHelpWhenNoOperationSpecified() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(outContent.toString().contains("Usage: rin admin audit <operation>"), 
                    "Help message should be displayed");
            assertTrue(outContent.toString().contains("Operations:"), 
                    "Operations should be listed");
        }
        
        @Test
        @DisplayName("Should display help for explicit help operation")
        void shouldDisplayHelpForHelpOperation() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("help");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Usage: rin admin audit <operation>"), 
                    "Help message should be displayed");
            assertTrue(outContent.toString().contains("Operations:"), 
                    "Operations should be listed");
        }
        
        @Test
        @DisplayName("Should display help with operations details")
        void shouldDisplayHelpWithOperationsDetails() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            
            // When
            command.setOperation("help");
            command.call();
            
            // Then
            String output = outContent.toString();
            assertTrue(output.contains("list"), "Should show list operation");
            assertTrue(output.contains("configure"), "Should show configure operation");
            assertTrue(output.contains("status"), "Should show status operation");
            assertTrue(output.contains("export"), "Should show export operation");
            assertTrue(output.contains("mask"), "Should show mask operation");
            assertTrue(output.contains("alert"), "Should show alert operation");
            assertTrue(output.contains("investigation"), "Should show investigation operation");
        }
        
        @Test
        @DisplayName("Should display help for unknown operation")
        void shouldDisplayHelpForUnknownOperation() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("unknown");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Unknown audit operation"), 
                    "Should show error message");
            assertTrue(outContent.toString().contains("Usage: rin admin audit <operation>"), 
                    "Help message should be displayed");
        }
    }

    /**
     * Tests for the positive scenarios of the AdminAuditCommand.
     */
    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("Should list audit logs without filters")
        void shouldListAuditLogsWithoutFilters() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("list");
            command.setArgs(new String[0]);
            
            // Mock audit service to return logs
            when(mockAuditService.listAuditLogs(isNull(), isNull(), isNull()))
                    .thenReturn("Test audit logs");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test audit logs"), 
                    "Should display audit logs");
            verify(mockAuditService).listAuditLogs(isNull(), isNull(), isNull());
        }
        
        @Test
        @DisplayName("Should list audit logs with user filter")
        void shouldListAuditLogsWithUserFilter() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("list");
            command.setArgs(new String[]{"--user=admin"});
            
            // Mock audit service to return logs
            when(mockAuditService.listAuditLogs(eq("admin"), isNull(), isNull()))
                    .thenReturn("Admin audit logs");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Admin audit logs"), 
                    "Should display admin audit logs");
            verify(mockAuditService).listAuditLogs(eq("admin"), isNull(), isNull());
        }
        
        @Test
        @DisplayName("Should list audit logs with days filter")
        void shouldListAuditLogsWithDaysFilter() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("list");
            command.setArgs(new String[]{"--days=7"});
            
            // Mock audit service to return logs
            when(mockAuditService.listAuditLogs(isNull(), eq(7), isNull()))
                    .thenReturn("Last 7 days audit logs");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Last 7 days audit logs"), 
                    "Should display audit logs from last 7 days");
            verify(mockAuditService).listAuditLogs(isNull(), eq(7), isNull());
        }
        
        @Test
        @DisplayName("Should configure audit retention with command line arg")
        void shouldConfigureAuditRetentionWithCommandLineArg() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("configure");
            command.setArgs(new String[]{"--retention=90"});
            
            // Mock audit service to return success
            when(mockAuditService.configureRetention(eq(90))).thenReturn(true);
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Audit log retention period updated to 90 days"), 
                    "Should show success message");
            verify(mockAuditService).configureRetention(eq(90));
        }
        
        @Test
        @DisplayName("Should configure audit retention interactively")
        void shouldConfigureAuditRetentionInteractively() throws Exception {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("configure");
            command.setArgs(new String[0]);
            
            // Mock interactive input
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine()).thenReturn("60");
            setScanner(command, mockScanner);
            
            // Mock audit service to return success
            when(mockAuditService.configureRetention(eq(60))).thenReturn(true);
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Audit log retention period updated to 60 days"), 
                    "Should show success message");
            verify(mockAuditService).configureRetention(eq(60));
        }
        
        @Test
        @DisplayName("Should configure audit retention with default value")
        void shouldConfigureAuditRetentionWithDefaultValue() throws Exception {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("configure");
            command.setArgs(new String[0]);
            
            // Mock interactive input (empty string for default)
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine()).thenReturn("");
            setScanner(command, mockScanner);
            
            // Mock audit service to return success
            when(mockAuditService.configureRetention(eq(30))).thenReturn(true);
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Audit log retention period updated to 30 days"), 
                    "Should show success message");
            verify(mockAuditService).configureRetention(eq(30));
        }
        
        @Test
        @DisplayName("Should show audit status")
        void shouldShowAuditStatus() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("status");
            
            // Mock audit service
            when(mockAuditService.getAuditStatus()).thenReturn("Audit system is operational");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Audit system is operational"), 
                    "Should display audit status");
            verify(mockAuditService).getAuditStatus();
        }
        
        @Test
        @DisplayName("Should export audit logs with default dates")
        void shouldExportAuditLogsWithDefaultDates() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("export");
            command.setArgs(new String[]{"--format=csv"});
            
            // Mock audit service
            when(mockAuditService.exportAuditLogs(any(LocalDate.class), any(LocalDate.class), eq("csv")))
                    .thenReturn("/path/to/exported/logs.csv");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Exported audit logs to /path/to/exported/logs.csv"), 
                    "Should display export path");
            verify(mockAuditService).exportAuditLogs(any(LocalDate.class), any(LocalDate.class), eq("csv"));
        }
        
        @Test
        @DisplayName("Should export audit logs with specified dates")
        void shouldExportAuditLogsWithSpecifiedDates() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("export");
            command.setArgs(new String[]{"--from=2025-01-01", "--to=2025-01-31", "--format=json"});
            
            // Expected dates
            LocalDate fromDate = LocalDate.of(2025, 1, 1);
            LocalDate toDate = LocalDate.of(2025, 1, 31);
            
            // Mock audit service
            when(mockAuditService.exportAuditLogs(eq(fromDate), eq(toDate), eq("json")))
                    .thenReturn("/path/to/exported/logs.json");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Exported audit logs to /path/to/exported/logs.json"), 
                    "Should display export path");
            verify(mockAuditService).exportAuditLogs(eq(fromDate), eq(toDate), eq("json"));
        }
        
        @Test
        @DisplayName("Should configure masking interactively")
        void shouldConfigureMaskingInteractively() throws Exception {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("mask");
            command.setArgs(new String[]{"configure"});
            
            // Mock interactive input
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine()).thenReturn("email, credit_card");
            setScanner(command, mockScanner);
            
            // Expected fields
            List<String> expectedFields = Arrays.asList("email", "credit_card");
            
            // Mock audit service
            when(mockAuditService.configureMasking(eq(expectedFields))).thenReturn(true);
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Data masking configuration updated successfully"), 
                    "Should show success message");
            verify(mockAuditService).configureMasking(eq(expectedFields));
        }
        
        @Test
        @DisplayName("Should show masking status")
        void shouldShowMaskingStatus() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("mask");
            command.setArgs(new String[]{"status"});
            
            // Mock audit service
            when(mockAuditService.getMaskingStatus()).thenReturn("Masking enabled for: email, ssn");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Masking enabled for: email, ssn"), 
                    "Should display masking status");
            verify(mockAuditService).getMaskingStatus();
        }
    }

    /**
     * Tests for the negative scenarios of the AdminAuditCommand.
     */
    @Nested
    @DisplayName("Negative Test Cases")
    class NegativeTestCases {
        
        @Test
        @DisplayName("Should handle audit service unavailable")
        void shouldHandleAuditServiceUnavailable() {
            // Given
            ServiceManager noServiceManager = mock(ServiceManager.class);
            when(noServiceManager.getAuditService()).thenReturn(null);
            
            AdminAuditCommand command = new AdminAuditCommand(noServiceManager);
            command.setOperation("list");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Audit service is not available"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle invalid days parameter in list command")
        void shouldHandleInvalidDaysParameterInListCommand() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("list");
            command.setArgs(new String[]{"--days=invalid"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Invalid value for --days"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle invalid limit parameter in list command")
        void shouldHandleInvalidLimitParameterInListCommand() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("list");
            command.setArgs(new String[]{"--limit=invalid"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Invalid value for --limit"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle exception when listing audit logs")
        void shouldHandleExceptionWhenListingAuditLogs() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("list");
            
            // Mock audit service to throw exception
            when(mockAuditService.listAuditLogs(isNull(), isNull(), isNull()))
                    .thenThrow(new RuntimeException("Test error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error listing audit logs"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle invalid retention parameter in configure command")
        void shouldHandleInvalidRetentionParameterInConfigureCommand() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("configure");
            command.setArgs(new String[]{"--retention=invalid"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Invalid value for --retention"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle negative retention period in configure command")
        void shouldHandleNegativeRetentionPeriodInConfigureCommand() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("configure");
            command.setArgs(new String[]{"--retention=-10"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Retention period must be greater than 0"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle zero retention period in configure command")
        void shouldHandleZeroRetentionPeriodInConfigureCommand() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("configure");
            command.setArgs(new String[]{"--retention=0"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Retention period must be greater than 0"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle failed configuration update")
        void shouldHandleFailedConfigurationUpdate() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("configure");
            command.setArgs(new String[]{"--retention=90"});
            
            // Mock audit service to return failure
            when(mockAuditService.configureRetention(eq(90))).thenReturn(false);
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Failed to update audit retention period"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle exception when getting audit status")
        void shouldHandleExceptionWhenGettingAuditStatus() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("status");
            
            // Mock audit service to throw exception
            when(mockAuditService.getAuditStatus())
                    .thenThrow(new RuntimeException("Test error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error getting audit status"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle invalid date format in export command")
        void shouldHandleInvalidDateFormatInExportCommand() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("export");
            command.setArgs(new String[]{"--from=01/01/2025"});  // Wrong format
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Invalid date format for --from"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle from date after to date in export command")
        void shouldHandleFromDateAfterToDateInExportCommand() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("export");
            command.setArgs(new String[]{"--from=2025-02-01", "--to=2025-01-01"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: From date must be before to date"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle exception when exporting audit logs")
        void shouldHandleExceptionWhenExportingAuditLogs() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("export");
            command.setArgs(new String[]{"--format=csv"});
            
            // Mock audit service to throw exception
            when(mockAuditService.exportAuditLogs(any(LocalDate.class), any(LocalDate.class), eq("csv")))
                    .thenThrow(new RuntimeException("Export error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error exporting audit logs"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle missing mask subcommand")
        void shouldHandleMissingMaskSubcommand() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("mask");
            command.setArgs(new String[0]);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Missing mask subcommand"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle unknown mask operation")
        void shouldHandleUnknownMaskOperation() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("mask");
            command.setArgs(new String[]{"unknown"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Unknown mask operation"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle failed masking configuration")
        void shouldHandleFailedMaskingConfiguration() throws Exception {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("mask");
            command.setArgs(new String[]{"configure"});
            
            // Mock interactive input
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine()).thenReturn("email, credit_card");
            setScanner(command, mockScanner);
            
            // Mock audit service to return failure
            when(mockAuditService.configureMasking(any())).thenReturn(false);
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Failed to update data masking configuration"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle exception when getting masking status")
        void shouldHandleExceptionWhenGettingMaskingStatus() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("mask");
            command.setArgs(new String[]{"status"});
            
            // Mock audit service to throw exception
            when(mockAuditService.getMaskingStatus())
                    .thenThrow(new RuntimeException("Test error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error getting masking status"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle missing alert subcommand")
        void shouldHandleMissingAlertSubcommand() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("alert");
            command.setArgs(new String[0]);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Missing alert subcommand"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle unknown alert operation")
        void shouldHandleUnknownAlertOperation() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("alert");
            command.setArgs(new String[]{"unknown"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Unknown alert operation"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle missing investigation subcommand")
        void shouldHandleMissingInvestigationSubcommand() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("investigation");
            command.setArgs(new String[0]);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Missing investigation subcommand"), 
                    "Should show error message");
        }
    }

    /**
     * Tests for the contract between AdminAuditCommand and its dependencies.
     */
    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should call ServiceManager.getAuditService")
        void shouldCallServiceManagerGetAuditService() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("status");
            
            // When
            command.call();
            
            // Then
            verify(mockServiceManager).getAuditService();
        }
        
        @Test
        @DisplayName("Should call AuditService.listAuditLogs with correct parameters")
        void shouldCallAuditServiceListAuditLogsWithCorrectParameters() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("list");
            command.setArgs(new String[]{"--user=admin", "--days=7", "--limit=10"});
            
            // When
            command.call();
            
            // Then
            verify(mockAuditService).listAuditLogs(eq("admin"), eq(7), eq(10));
        }
        
        @Test
        @DisplayName("Should call AuditService.configureRetention with correct parameter")
        void shouldCallAuditServiceConfigureRetentionWithCorrectParameter() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("configure");
            command.setArgs(new String[]{"--retention=90"});
            
            // When
            command.call();
            
            // Then
            verify(mockAuditService).configureRetention(eq(90));
        }
        
        @Test
        @DisplayName("Should call AuditService.getAuditStatus")
        void shouldCallAuditServiceGetAuditStatus() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("status");
            
            // When
            command.call();
            
            // Then
            verify(mockAuditService).getAuditStatus();
        }
        
        @Test
        @DisplayName("Should call AuditService.exportAuditLogs with correct parameters")
        void shouldCallAuditServiceExportAuditLogsWithCorrectParameters() {
            // Given
            AdminAuditCommand command = new AdminAuditCommand(mockServiceManager);
            command.setOperation("export");
            command.setArgs(new String[]{"--from=2025-01-01", "--to=2025-01-31", "--format=json"});
            
            // Expected dates
            LocalDate fromDate = LocalDate.of(2025, 1, 1);
            LocalDate toDate = LocalDate.of(2025, 1, 31);
            
            // When
            command.call();
            
            // Then
            verify(mockAuditService).exportAuditLogs(eq(fromDate), eq(toDate), eq("json"));
        }
    }

    /**
     * Tests for integration scenarios of the AdminAuditCommand.
     */
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Should perform end-to-end export operation")
        void shouldPerformEndToEndExportOperation() {
            // Given
            AuditService realAuditService = new MockAuditService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getAuditService()).thenReturn(realAuditService);
            
            AdminAuditCommand command = new AdminAuditCommand(realServiceManager);
            command.setOperation("export");
            command.setArgs(new String[]{"--from=2025-01-01", "--to=2025-01-31", "--format=csv"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Exported audit logs to"), 
                    "Should show export confirmation");
            assertTrue(outContent.toString().contains("audit_export_2025-01-01_to_2025-01-31.csv"), 
                    "Should show correct file name");
        }
        
        @Test
        @DisplayName("Should perform end-to-end configuration operation")
        void shouldPerformEndToEndConfigurationOperation() throws Exception {
            // Given
            AuditService realAuditService = new MockAuditService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getAuditService()).thenReturn(realAuditService);
            
            AdminAuditCommand command = new AdminAuditCommand(realServiceManager);
            command.setOperation("configure");
            command.setArgs(new String[]{"--retention=90"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Audit log retention period updated to 90 days"), 
                    "Should show configuration confirmation");
        }
        
        @Test
        @DisplayName("Should perform end-to-end status display")
        void shouldPerformEndToEndStatusDisplay() {
            // Given
            AuditService realAuditService = new MockAuditService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getAuditService()).thenReturn(realAuditService);
            
            AdminAuditCommand command = new AdminAuditCommand(realServiceManager);
            command.setOperation("status");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Audit System Status"), 
                    "Should display audit status header");
            assertTrue(outContent.toString().contains("Audit logging: Enabled"), 
                    "Should display enabled status");
        }
        
        @Test
        @DisplayName("Should perform end-to-end list operation")
        void shouldPerformEndToEndListOperation() {
            // Given
            AuditService realAuditService = new MockAuditService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getAuditService()).thenReturn(realAuditService);
            
            AdminAuditCommand command = new AdminAuditCommand(realServiceManager);
            command.setOperation("list");
            command.setArgs(new String[]{"--user=admin", "--days=7"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Audit Logs"), 
                    "Should display audit logs header");
            assertTrue(outContent.toString().contains("Filtered by user: admin"), 
                    "Should show user filter");
            assertTrue(outContent.toString().contains("last 7 days"), 
                    "Should show days filter");
        }
        
        @Test
        @DisplayName("Should perform mask configuration and status check")
        void shouldPerformMaskConfigurationAndStatusCheck() throws Exception {
            // Given
            AuditService realAuditService = new MockAuditService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getAuditService()).thenReturn(realAuditService);
            
            // First configure masking
            AdminAuditCommand configCommand = new AdminAuditCommand(realServiceManager);
            configCommand.setOperation("mask");
            configCommand.setArgs(new String[]{"configure"});
            
            // Mock interactive input
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine()).thenReturn("email, ssn");
            setScanner(configCommand, mockScanner);
            
            // When
            int configExitCode = configCommand.call();
            
            // Then
            assertEquals(0, configExitCode, "Should return success code for configuration");
            assertTrue(outContent.toString().contains("Data masking configuration updated successfully"), 
                    "Should show configuration confirmation");
            
            // Clear output buffer
            outContent.reset();
            
            // Now check status
            AdminAuditCommand statusCommand = new AdminAuditCommand(realServiceManager);
            statusCommand.setOperation("mask");
            statusCommand.setArgs(new String[]{"status"});
            
            // When
            int statusExitCode = statusCommand.call();
            
            // Then
            assertEquals(0, statusExitCode, "Should return success code for status");
            assertTrue(outContent.toString().contains("Data Masking Configuration"), 
                    "Should display masking configuration header");
            assertTrue(outContent.toString().contains("Masking enabled: true"), 
                    "Should show enabled status");
            assertTrue(outContent.toString().contains("email, ssn"), 
                    "Should show configured fields");
        }
        
        @Test
        @DisplayName("Should create and retrieve investigation case")
        void shouldCreateAndRetrieveInvestigationCase() {
            // Given
            AuditService realAuditService = new MockAuditService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getAuditService()).thenReturn(realAuditService);
            
            // First create investigation
            AdminAuditCommand createCommand = new AdminAuditCommand(realServiceManager);
            createCommand.setOperation("investigation");
            createCommand.setArgs(new String[]{"create", "--user=admin", "--days=7"});
            
            // When
            int createExitCode = createCommand.call();
            
            // Then
            assertEquals(0, createExitCode, "Should return success code for creation");
            assertTrue(outContent.toString().contains("Investigation case created successfully"), 
                    "Should show creation confirmation");
            
            // Extract case ID
            String output = outContent.toString();
            String caseId = output.substring(output.indexOf("Case ID: ") + 9).trim();
            
            // Clear output buffer
            outContent.reset();
            
            // Now get findings
            AdminAuditCommand findingsCommand = new AdminAuditCommand(realServiceManager);
            findingsCommand.setOperation("investigation");
            findingsCommand.setArgs(new String[]{"findings", "--case=" + caseId});
            
            // When
            int findingsExitCode = findingsCommand.call();
            
            // Then
            assertEquals(0, findingsExitCode, "Should return success code for findings");
            assertTrue(outContent.toString().contains("Investigation Findings: " + caseId), 
                    "Should display findings with correct case ID");
            assertTrue(outContent.toString().contains("Subject: admin"), 
                    "Should show correct subject");
        }
        
        @Test
        @DisplayName("Should perform investigation action")
        void shouldPerformInvestigationAction() {
            // Given
            AuditService realAuditService = new MockAuditService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getAuditService()).thenReturn(realAuditService);
            
            AdminAuditCommand command = new AdminAuditCommand(realServiceManager);
            command.setOperation("investigation");
            command.setArgs(new String[]{"actions", "--action=LOCK_ACCOUNT", "--user=suspect.user"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Investigation action 'LOCK_ACCOUNT' performed successfully"), 
                    "Should show action confirmation");
            assertTrue(outContent.toString().contains("suspect.user"), 
                    "Should show target user");
        }
    }
}