/*
 * AdminBackupCommandTest - Tests for the AdminBackupCommand class
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
import java.util.Scanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rinna.cli.service.BackupService;
import org.rinna.cli.service.MockBackupService;
import org.rinna.cli.service.ServiceManager;

/**
 * Comprehensive test class for the AdminBackupCommand functionality.
 * Tests all aspects of the command following TDD principles.
 */
@DisplayName("AdminBackupCommand Tests")
class AdminBackupCommandTest {

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private InputStream originalIn;
    
    private ServiceManager mockServiceManager;
    private BackupService mockBackupService;
    
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
        mockBackupService = Mockito.mock(BackupService.class);
        
        // Configure mocks
        Mockito.when(mockServiceManager.getBackupService()).thenReturn(mockBackupService);
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
    private void setScanner(AdminBackupCommand command, Scanner scanner) throws Exception {
        Field scannerField = AdminBackupCommand.class.getDeclaredField("scanner");
        scannerField.setAccessible(true);
        scannerField.set(command, scanner);
    }

    /**
     * Tests for the help documentation of the AdminBackupCommand.
     */
    @Nested
    @DisplayName("Help Documentation Tests")
    class HelpDocumentationTests {
        
        @Test
        @DisplayName("Should display help when no operation specified")
        void shouldDisplayHelpWhenNoOperationSpecified() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(outContent.toString().contains("Usage: rin admin backup <operation>"), 
                    "Help message should be displayed");
            assertTrue(outContent.toString().contains("Operations:"), 
                    "Operations should be listed");
        }
        
        @Test
        @DisplayName("Should display help for explicit help operation")
        void shouldDisplayHelpForHelpOperation() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("help");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Usage: rin admin backup <operation>"), 
                    "Help message should be displayed");
            assertTrue(outContent.toString().contains("Operations:"), 
                    "Operations should be listed");
        }
        
        @Test
        @DisplayName("Should display help with all operations details")
        void shouldDisplayHelpWithAllOperationsDetails() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            
            // When
            command.setOperation("help");
            command.call();
            
            // Then
            String output = outContent.toString();
            assertTrue(output.contains("configure"), "Should show configure operation");
            assertTrue(output.contains("status"), "Should show status operation");
            assertTrue(output.contains("start"), "Should show start operation");
            assertTrue(output.contains("list"), "Should show list operation");
            assertTrue(output.contains("strategy"), "Should show strategy operation");
            assertTrue(output.contains("history"), "Should show history operation");
            assertTrue(output.contains("security"), "Should show security operation");
            assertTrue(output.contains("verify"), "Should show verify operation");
            assertTrue(output.contains("notifications"), "Should show notifications operation");
            assertTrue(output.contains("locations"), "Should show locations operation");
        }
        
        @Test
        @DisplayName("Should display help for unknown operation")
        void shouldDisplayHelpForUnknownOperation() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("unknown");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Unknown backup operation"), 
                    "Should show error message");
            assertTrue(outContent.toString().contains("Usage: rin admin backup <operation>"), 
                    "Help message should be displayed");
        }
    }

    /**
     * Tests for the positive scenarios of the AdminBackupCommand.
     */
    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("Should display backup status")
        void shouldDisplayBackupStatus() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("status");
            
            // Mock backup service to return status
            when(mockBackupService.getBackupStatus())
                    .thenReturn("Test backup status");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test backup status"), 
                    "Should display backup status");
            verify(mockBackupService).getBackupStatus();
        }
        
        @Test
        @DisplayName("Should start backup with default type")
        void shouldStartBackupWithDefaultType() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("start");
            command.setArgs(new String[0]);
            
            // Mock backup service to return ID
            when(mockBackupService.startBackup("full"))
                    .thenReturn("BK-12345678");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Backup completed successfully"), 
                    "Should show success message");
            assertTrue(outContent.toString().contains("Backup ID: BK-12345678"), 
                    "Should show backup ID");
            verify(mockBackupService).startBackup("full");
        }
        
        @Test
        @DisplayName("Should start backup with specified type")
        void shouldStartBackupWithSpecifiedType() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("start");
            command.setArgs(new String[]{"--type=incremental"});
            
            // Mock backup service to return ID
            when(mockBackupService.startBackup("incremental"))
                    .thenReturn("BK-12345678");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Backup completed successfully"), 
                    "Should show success message");
            verify(mockBackupService).startBackup("incremental");
        }
        
        @Test
        @DisplayName("Should list backups")
        void shouldListBackups() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("list");
            
            // Mock backup service to return listing
            when(mockBackupService.listBackups())
                    .thenReturn("Test backup listing");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test backup listing"), 
                    "Should display backup listing");
            verify(mockBackupService).listBackups();
        }
        
        @Test
        @DisplayName("Should display backup history")
        void shouldDisplayBackupHistory() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("history");
            
            // Mock backup service to return history
            when(mockBackupService.getBackupHistory())
                    .thenReturn("Test backup history");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test backup history"), 
                    "Should display backup history");
            verify(mockBackupService).getBackupHistory();
        }
        
        @Test
        @DisplayName("Should verify backup with latest ID")
        void shouldVerifyBackupWithLatestId() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("verify");
            command.setArgs(new String[0]);
            
            // Mock backup service to return verification report
            when(mockBackupService.verifyBackup("latest"))
                    .thenReturn("Test verification report");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test verification report"), 
                    "Should display verification report");
            verify(mockBackupService).verifyBackup("latest");
        }
        
        @Test
        @DisplayName("Should verify backup with specified ID")
        void shouldVerifyBackupWithSpecifiedId() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("verify");
            command.setArgs(new String[]{"--backup-id=BK-12345678"});
            
            // Mock backup service to return verification report
            when(mockBackupService.verifyBackup("BK-12345678"))
                    .thenReturn("Test verification report");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test verification report"), 
                    "Should display verification report");
            verify(mockBackupService).verifyBackup("BK-12345678");
        }
        
        @Test
        @DisplayName("Should display backup strategy status")
        void shouldDisplayBackupStrategyStatus() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("strategy");
            command.setArgs(new String[]{"status"});
            
            // Mock backup service to return strategy status
            when(mockBackupService.getBackupStrategyStatus())
                    .thenReturn("Test strategy status");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test strategy status"), 
                    "Should display strategy status");
            verify(mockBackupService).getBackupStrategyStatus();
        }
        
        @Test
        @DisplayName("Should display backup security status")
        void shouldDisplayBackupSecurityStatus() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("security");
            command.setArgs(new String[]{"status"});
            
            // Mock backup service to return security status
            when(mockBackupService.getBackupSecurityStatus())
                    .thenReturn("Test security status");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test security status"), 
                    "Should display security status");
            verify(mockBackupService).getBackupSecurityStatus();
        }
        
        @Test
        @DisplayName("Should display backup notifications status")
        void shouldDisplayBackupNotificationsStatus() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("notifications");
            command.setArgs(new String[]{"status"});
            
            // Mock backup service to return notifications status
            when(mockBackupService.getBackupNotificationsStatus())
                    .thenReturn("Test notifications status");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test notifications status"), 
                    "Should display notifications status");
            verify(mockBackupService).getBackupNotificationsStatus();
        }
    }

    /**
     * Tests for the negative scenarios of the AdminBackupCommand.
     */
    @Nested
    @DisplayName("Negative Test Cases")
    class NegativeTestCases {
        
        @Test
        @DisplayName("Should handle backup service unavailable")
        void shouldHandleBackupServiceUnavailable() {
            // Given
            ServiceManager noServiceManager = mock(ServiceManager.class);
            when(noServiceManager.getBackupService()).thenReturn(null);
            
            AdminBackupCommand command = new AdminBackupCommand(noServiceManager);
            command.setOperation("status");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Backup service is not available"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle invalid backup type in start command")
        void shouldHandleInvalidBackupTypeInStartCommand() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("start");
            command.setArgs(new String[]{"--type=invalid"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Invalid backup type"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle exception when starting backup")
        void shouldHandleExceptionWhenStartingBackup() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("start");
            
            // Mock backup service to throw exception
            when(mockBackupService.startBackup(anyString()))
                    .thenThrow(new RuntimeException("Test error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error starting backup"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle exception when getting backup status")
        void shouldHandleExceptionWhenGettingBackupStatus() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("status");
            
            // Mock backup service to throw exception
            when(mockBackupService.getBackupStatus())
                    .thenThrow(new RuntimeException("Test error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error getting backup status"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle exception when listing backups")
        void shouldHandleExceptionWhenListingBackups() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("list");
            
            // Mock backup service to throw exception
            when(mockBackupService.listBackups())
                    .thenThrow(new RuntimeException("Test error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error listing backups"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle exception when getting backup history")
        void shouldHandleExceptionWhenGettingBackupHistory() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("history");
            
            // Mock backup service to throw exception
            when(mockBackupService.getBackupHistory())
                    .thenThrow(new RuntimeException("Test error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error getting backup history"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle exception when verifying backup")
        void shouldHandleExceptionWhenVerifyingBackup() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("verify");
            
            // Mock backup service to throw exception
            when(mockBackupService.verifyBackup(anyString()))
                    .thenThrow(new RuntimeException("Test error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error verifying backup"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle missing strategy subcommand")
        void shouldHandleMissingStrategySubcommand() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("strategy");
            command.setArgs(new String[0]);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Missing strategy subcommand"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle unknown strategy operation")
        void shouldHandleUnknownStrategyOperation() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("strategy");
            command.setArgs(new String[]{"unknown"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Unknown strategy operation"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle exception when getting strategy status")
        void shouldHandleExceptionWhenGettingStrategyStatus() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("strategy");
            command.setArgs(new String[]{"status"});
            
            // Mock backup service to throw exception
            when(mockBackupService.getBackupStrategyStatus())
                    .thenThrow(new RuntimeException("Test error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error getting backup strategy status"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle missing security subcommand")
        void shouldHandleMissingSecuritySubcommand() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("security");
            command.setArgs(new String[0]);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Missing security subcommand"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle unknown security operation")
        void shouldHandleUnknownSecurityOperation() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("security");
            command.setArgs(new String[]{"unknown"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Unknown security operation"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle missing locations subcommand")
        void shouldHandleMissingLocationsSubcommand() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("locations");
            command.setArgs(new String[0]);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Missing locations subcommand"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle unknown locations operation")
        void shouldHandleUnknownLocationsOperation() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("locations");
            command.setArgs(new String[]{"unknown"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Unknown locations operation"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle missing notifications subcommand")
        void shouldHandleMissingNotificationsSubcommand() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("notifications");
            command.setArgs(new String[0]);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Missing notifications subcommand"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle unknown notifications operation")
        void shouldHandleUnknownNotificationsOperation() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("notifications");
            command.setArgs(new String[]{"unknown"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Unknown notifications operation"), 
                    "Should show error message");
        }
        
        // Additional negative tests
        
        @Test
        @DisplayName("Should handle exception when configuring backup strategy")
        void shouldHandleExceptionWhenConfiguringBackupStrategy() throws Exception {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("strategy");
            command.setArgs(new String[]{"configure"});
            
            // Mock scanner
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine())
                .thenReturn("2")    // Strategy (incremental)
                .thenReturn("weekly") // Full frequency
                .thenReturn("daily");  // Incremental frequency
            setScanner(command, mockScanner);
            
            // Mock backup service to throw exception
            when(mockBackupService.configureBackupStrategy(anyString(), anyString(), anyString()))
                    .thenThrow(new RuntimeException("Test error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error configuring backup strategy"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle failure when configuring backup strategy")
        void shouldHandleFailureWhenConfiguringBackupStrategy() throws Exception {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("strategy");
            command.setArgs(new String[]{"configure"});
            
            // Mock scanner
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine())
                .thenReturn("2")    // Strategy (incremental)
                .thenReturn("weekly") // Full frequency
                .thenReturn("daily");  // Incremental frequency
            setScanner(command, mockScanner);
            
            // Mock backup service to return failure
            when(mockBackupService.configureBackupStrategy(anyString(), anyString(), anyString()))
                    .thenReturn(false);
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Failed to update backup strategy"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle empty passphrase in security configuration")
        void shouldHandleEmptyPassphraseInSecurityConfiguration() throws Exception {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("security");
            command.setArgs(new String[]{"configure"});
            
            // Mock scanner
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine())
                .thenReturn("yes")   // Enable encryption
                .thenReturn("1")     // AES-256 
                .thenReturn("")     // Empty passphrase
                .thenReturn("");    // Confirm passphrase
            setScanner(command, mockScanner);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Passphrase cannot be empty"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle mismatched passphrases in security configuration")
        void shouldHandleMismatchedPassphrasesInSecurityConfiguration() throws Exception {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("security");
            command.setArgs(new String[]{"configure"});
            
            // Mock scanner
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine())
                .thenReturn("yes")      // Enable encryption
                .thenReturn("1")        // AES-256 
                .thenReturn("password") // Passphrase
                .thenReturn("different"); // Different confirmation
            setScanner(command, mockScanner);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Passphrases do not match"), 
                    "Should show error message");
        }
    }

    /**
     * Tests for the contract between AdminBackupCommand and its dependencies.
     */
    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should call ServiceManager.getBackupService")
        void shouldCallServiceManagerGetBackupService() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("status");
            
            // When
            command.call();
            
            // Then
            verify(mockServiceManager).getBackupService();
        }
        
        @Test
        @DisplayName("Should call BackupService.getBackupStatus")
        void shouldCallBackupServiceGetBackupStatus() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("status");
            
            // When
            command.call();
            
            // Then
            verify(mockBackupService).getBackupStatus();
        }
        
        @Test
        @DisplayName("Should call BackupService.startBackup with correct type")
        void shouldCallBackupServiceStartBackupWithCorrectType() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("start");
            command.setArgs(new String[]{"--type=differential"});
            
            // When
            command.call();
            
            // Then
            verify(mockBackupService).startBackup("differential");
        }
        
        @Test
        @DisplayName("Should call BackupService.listBackups")
        void shouldCallBackupServiceListBackups() {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("list");
            
            // When
            command.call();
            
            // Then
            verify(mockBackupService).listBackups();
        }
        
        @Test
        @DisplayName("Should call BackupService.configureBackupStrategy with correct parameters")
        void shouldCallBackupServiceConfigureBackupStrategyWithCorrectParameters() throws Exception {
            // Given
            AdminBackupCommand command = new AdminBackupCommand(mockServiceManager);
            command.setOperation("strategy");
            command.setArgs(new String[]{"configure"});
            
            // Mock scanner
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine())
                .thenReturn("2")     // Strategy (incremental)
                .thenReturn("weekly") // Full frequency
                .thenReturn("daily"); // Incremental frequency
            setScanner(command, mockScanner);
            
            // Mock service to return success
            when(mockBackupService.configureBackupStrategy("incremental", "weekly", "daily"))
                    .thenReturn(true);
            
            // When
            command.call();
            
            // Then
            verify(mockBackupService).configureBackupStrategy("incremental", "weekly", "daily");
        }
    }

    /**
     * Tests for integration scenarios of the AdminBackupCommand.
     */
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Should integrate with real backup service for status operation")
        void shouldIntegrateWithRealBackupServiceForStatusOperation() {
            // Given
            BackupService realBackupService = new MockBackupService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getBackupService()).thenReturn(realBackupService);
            
            AdminBackupCommand command = new AdminBackupCommand(realServiceManager);
            command.setOperation("status");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Backup Status"), 
                    "Should display backup status header");
            assertTrue(outContent.toString().contains("Configuration:"), 
                    "Should display configuration section");
            assertTrue(outContent.toString().contains("Strategy:"), 
                    "Should display strategy section");
            assertTrue(outContent.toString().contains("Security:"), 
                    "Should display security section");
            assertTrue(outContent.toString().contains("Last Backup:"), 
                    "Should display last backup section");
        }
        
        @Test
        @DisplayName("Should integrate with real backup service for list operation")
        void shouldIntegrateWithRealBackupServiceForListOperation() {
            // Given
            BackupService realBackupService = new MockBackupService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getBackupService()).thenReturn(realBackupService);
            
            AdminBackupCommand command = new AdminBackupCommand(realServiceManager);
            command.setOperation("list");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Available Backups"), 
                    "Should display backups header");
            assertTrue(outContent.toString().contains("ID"), 
                    "Should display ID column");
            assertTrue(outContent.toString().contains("Type"), 
                    "Should display Type column");
            assertTrue(outContent.toString().contains("Date"), 
                    "Should display Date column");
            assertTrue(outContent.toString().contains("Size"), 
                    "Should display Size column");
            assertTrue(outContent.toString().contains("Total backups:"), 
                    "Should display total backups");
        }
        
        @Test
        @DisplayName("Should integrate with real backup service for history operation")
        void shouldIntegrateWithRealBackupServiceForHistoryOperation() {
            // Given
            BackupService realBackupService = new MockBackupService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getBackupService()).thenReturn(realBackupService);
            
            AdminBackupCommand command = new AdminBackupCommand(realServiceManager);
            command.setOperation("history");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Backup History"), 
                    "Should display history header");
            assertTrue(outContent.toString().contains("Recent Backup Operations:"), 
                    "Should display operations section");
            assertTrue(outContent.toString().contains("Backup Statistics:"), 
                    "Should display statistics section");
        }
        
        @Test
        @DisplayName("Should integrate with real backup service for verify operation")
        void shouldIntegrateWithRealBackupServiceForVerifyOperation() {
            // Given
            BackupService realBackupService = new MockBackupService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getBackupService()).thenReturn(realBackupService);
            
            AdminBackupCommand command = new AdminBackupCommand(realServiceManager);
            command.setOperation("verify");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Backup Verification Report"), 
                    "Should display verification header");
            assertTrue(outContent.toString().contains("Verification Results:"), 
                    "Should display verification results");
            assertTrue(outContent.toString().contains("Integrity check:"), 
                    "Should display integrity check");
        }
        
        @Test
        @DisplayName("Should perform full workflow with real backup service")
        void shouldPerformFullWorkflowWithRealBackupService() {
            // Given
            BackupService realBackupService = new MockBackupService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getBackupService()).thenReturn(realBackupService);
            
            // 1. Start a backup
            AdminBackupCommand startCommand = new AdminBackupCommand(realServiceManager);
            startCommand.setOperation("start");
            startCommand.setArgs(new String[]{"--type=full"});
            
            // When
            int startExitCode = startCommand.call();
            
            // Then
            assertEquals(0, startExitCode, "Start command should return success code");
            assertTrue(outContent.toString().contains("Backup completed successfully"), 
                    "Should show backup success message");
            
            // Extract backup ID
            String output = outContent.toString();
            String backupId = output.substring(output.indexOf("Backup ID: ") + 11).trim();
            
            // Clear output buffer
            outContent.reset();
            
            // 2. List backups to verify new backup is included
            AdminBackupCommand listCommand = new AdminBackupCommand(realServiceManager);
            listCommand.setOperation("list");
            
            // When
            int listExitCode = listCommand.call();
            
            // Then
            assertEquals(0, listExitCode, "List command should return success code");
            assertTrue(outContent.toString().contains("Available Backups"), 
                    "Should display backups header");
            assertTrue(outContent.toString().contains(backupId), 
                    "Should include the new backup ID");
            
            // Clear output buffer
            outContent.reset();
            
            // 3. Verify the backup
            AdminBackupCommand verifyCommand = new AdminBackupCommand(realServiceManager);
            verifyCommand.setOperation("verify");
            verifyCommand.setArgs(new String[]{"--backup-id=" + backupId});
            
            // When
            int verifyExitCode = verifyCommand.call();
            
            // Then
            assertEquals(0, verifyExitCode, "Verify command should return success code");
            assertTrue(outContent.toString().contains("Backup Verification Report"), 
                    "Should display verification header");
            assertTrue(outContent.toString().contains("Backup ID: " + backupId), 
                    "Should verify the correct backup");
            assertTrue(outContent.toString().contains("Integrity check: PASSED"), 
                    "Should show passing integrity check");
        }
    }
}