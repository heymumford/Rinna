/*
 * AdminComplianceCommandTest - Tests for the AdminComplianceCommand class
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mockito;
import org.rinna.cli.service.ComplianceService;
import org.rinna.cli.service.MockComplianceService;
import org.rinna.cli.service.ServiceManager;

/**
 * Comprehensive test class for the AdminComplianceCommand functionality.
 * Tests all aspects of the command following TDD principles.
 */
@DisplayName("AdminComplianceCommand Tests")
class AdminComplianceCommandTest {

    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private InputStream originalIn;
    
    private ServiceManager mockServiceManager;
    private ComplianceService mockComplianceService;
    private AdminComplianceCommand command;
    
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
        mockComplianceService = Mockito.mock(ComplianceService.class);
        
        // Configure mocks
        Mockito.when(mockServiceManager.getComplianceService()).thenReturn(mockComplianceService);
        
        // Create the command with our mocked ServiceManager
        command = new AdminComplianceCommand(mockServiceManager);
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
    private void setScanner(AdminComplianceCommand command, Scanner scanner) throws Exception {
        Field scannerField = AdminComplianceCommand.class.getDeclaredField("scanner");
        scannerField.setAccessible(true);
        scannerField.set(command, scanner);
    }

    /**
     * Tests for the help documentation of the AdminComplianceCommand.
     */
    @Nested
    @DisplayName("Help Documentation Tests")
    class HelpDocumentationTests {
        
        @Test
        @DisplayName("Should display help when no operation specified")
        void shouldDisplayHelpWhenNoOperationSpecified() {
            // Given
            AdminComplianceCommand command = new AdminComplianceCommand(mockServiceManager);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(outContent.toString().contains("Usage: rin admin compliance <operation>"), 
                    "Help message should be displayed");
            assertTrue(outContent.toString().contains("Operations:"), 
                    "Operations should be listed");
        }
        
        @Test
        @DisplayName("Should display help for explicit help operation")
        void shouldDisplayHelpForHelpOperation() {
            // Given
            AdminComplianceCommand command = new AdminComplianceCommand(mockServiceManager);
            command.setOperation("help");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Usage: rin admin compliance <operation>"), 
                    "Help message should be displayed");
            assertTrue(outContent.toString().contains("Operations:"), 
                    "Operations should be listed");
        }
        
        @Test
        @DisplayName("Should display help with operations details")
        void shouldDisplayHelpWithOperationsDetails() {
            // Given
            AdminComplianceCommand command = new AdminComplianceCommand(mockServiceManager);
            
            // When
            command.setOperation("help");
            command.call();
            
            // Then
            String output = outContent.toString();
            assertTrue(output.contains("report"), "Should show report operation");
            assertTrue(output.contains("configure"), "Should show configure operation");
            assertTrue(output.contains("validate"), "Should show validate operation");
            assertTrue(output.contains("status"), "Should show status operation");
        }
        
        @Test
        @DisplayName("Should display help for unknown operation")
        void shouldDisplayHelpForUnknownOperation() {
            // Given
            AdminComplianceCommand command = new AdminComplianceCommand(mockServiceManager);
            command.setOperation("unknown");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Unknown compliance operation"), 
                    "Should show error message");
            assertTrue(outContent.toString().contains("Usage: rin admin compliance <operation>"), 
                    "Help message should be displayed");
        }
    }

    /**
     * Tests for the positive scenarios of the AdminComplianceCommand.
     */
    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("Should generate compliance report with default parameters")
        void shouldGenerateComplianceReportWithDefaultParameters() {
            // Given
            command.setOperation("report");
            command.setArgs(new String[0]);
            
            // Mock compliance service to return report
            when(mockComplianceService.generateComplianceReport(eq("GDPR"), eq("current")))
                    .thenReturn("Test compliance report for GDPR, current period");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test compliance report for GDPR, current period"), 
                    "Should display compliance report");
            verify(mockComplianceService).generateComplianceReport(eq("GDPR"), eq("current"));
        }
        
        @Test
        @DisplayName("Should generate compliance report with specified parameters")
        void shouldGenerateComplianceReportWithSpecifiedParameters() {
            // Given
            command.setOperation("report");
            command.setArgs(new String[]{"--type=HIPAA", "--period=Q1-2025"});
            
            // Mock compliance service to return report
            when(mockComplianceService.generateComplianceReport(eq("HIPAA"), eq("Q1-2025")))
                    .thenReturn("Test compliance report for HIPAA, Q1-2025");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test compliance report for HIPAA, Q1-2025"), 
                    "Should display compliance report");
            verify(mockComplianceService).generateComplianceReport(eq("HIPAA"), eq("Q1-2025"));
        }
        
        @Test
        @DisplayName("Should configure project compliance interactively")
        void shouldConfigureProjectComplianceInteractively() throws Exception {
            // Given
            command.setOperation("configure");
            command.setArgs(new String[]{"--project=test-project"});
            
            // Mock scanner
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine())
                .thenReturn("1,3")    // Frameworks (GDPR, SOC2)
                .thenReturn("compliance-reviewer@example.com");  // Reviewer
            setScanner(command, mockScanner);
            
            // Expected frameworks
            List<String> expectedFrameworks = Arrays.asList("GDPR", "SOC2");
            
            // Mock compliance service to return success
            when(mockComplianceService.configureProjectCompliance(
                    eq("test-project"), 
                    eq(expectedFrameworks), 
                    eq("compliance-reviewer@example.com")))
                .thenReturn(true);
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Compliance configuration updated successfully"), 
                    "Should show success message");
            verify(mockComplianceService).configureProjectCompliance(
                    eq("test-project"), 
                    eq(expectedFrameworks), 
                    eq("compliance-reviewer@example.com"));
        }
        
        @Test
        @DisplayName("Should configure project compliance with framework names")
        void shouldConfigureProjectComplianceWithFrameworkNames() throws Exception {
            // Given
            command.setOperation("configure");
            command.setArgs(new String[]{"--project=test-project"});
            
            // Mock scanner
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine())
                .thenReturn("GDPR, ISO27001")    // Frameworks
                .thenReturn("compliance-reviewer@example.com");  // Reviewer
            setScanner(command, mockScanner);
            
            // Expected frameworks
            List<String> expectedFrameworks = Arrays.asList("GDPR", "ISO27001");
            
            // Mock compliance service to return success
            when(mockComplianceService.configureProjectCompliance(
                    eq("test-project"), 
                    eq(expectedFrameworks), 
                    eq("compliance-reviewer@example.com")))
                .thenReturn(true);
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Compliance configuration updated successfully"), 
                    "Should show success message");
            verify(mockComplianceService).configureProjectCompliance(
                    eq("test-project"), 
                    eq(expectedFrameworks), 
                    eq("compliance-reviewer@example.com"));
        }
        
        @Test
        @DisplayName("Should configure project compliance with default framework")
        void shouldConfigureProjectComplianceWithDefaultFramework() throws Exception {
            // Given
            command.setOperation("configure");
            command.setArgs(new String[]{"--project=test-project"});
            
            // Mock scanner
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine())
                .thenReturn("")    // Default framework (GDPR)
                .thenReturn("compliance-reviewer@example.com");  // Reviewer
            setScanner(command, mockScanner);
            
            // Expected frameworks
            List<String> expectedFrameworks = List.of("GDPR");
            
            // Mock compliance service to return success
            when(mockComplianceService.configureProjectCompliance(
                    eq("test-project"), 
                    eq(expectedFrameworks), 
                    eq("compliance-reviewer@example.com")))
                .thenReturn(true);
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Compliance configuration updated successfully"), 
                    "Should show success message");
            verify(mockComplianceService).configureProjectCompliance(
                    eq("test-project"), 
                    eq(expectedFrameworks), 
                    eq("compliance-reviewer@example.com"));
        }
        
        @Test
        @DisplayName("Should validate project compliance")
        void shouldValidateProjectCompliance() {
            // Given
            command.setOperation("validate");
            command.setArgs(new String[]{"--project=test-project"});
            
            // Mock compliance service to return validation report
            when(mockComplianceService.validateProjectCompliance(eq("test-project")))
                    .thenReturn("Test validation report for test-project");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test validation report for test-project"), 
                    "Should display validation report");
            verify(mockComplianceService).validateProjectCompliance(eq("test-project"));
        }
        
        @Test
        @DisplayName("Should display project compliance status")
        void shouldDisplayProjectComplianceStatus() {
            // Given
            command.setOperation("status");
            command.setArgs(new String[]{"--project=test-project"});
            
            // Mock compliance service to return status
            when(mockComplianceService.getProjectComplianceStatus(eq("test-project")))
                    .thenReturn("Test compliance status for test-project");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test compliance status for test-project"), 
                    "Should display project compliance status");
            verify(mockComplianceService).getProjectComplianceStatus(eq("test-project"));
        }
        
        @Test
        @DisplayName("Should display system compliance status")
        void shouldDisplaySystemComplianceStatus() {
            // Given
            command.setOperation("status");
            command.setArgs(new String[0]);
            
            // Mock compliance service to return status
            when(mockComplianceService.getSystemComplianceStatus())
                    .thenReturn("Test system compliance status");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test system compliance status"), 
                    "Should display system compliance status");
            verify(mockComplianceService).getSystemComplianceStatus();
        }
        
        @ParameterizedTest
        @DisplayName("Should generate reports for different frameworks")
        @ValueSource(strings = {"GDPR", "HIPAA", "SOC2", "PCI-DSS", "ISO27001"})
        void shouldGenerateReportsForDifferentFrameworks(String framework) {
            // Given
            command.setOperation("report");
            command.setArgs(new String[]{"--type=" + framework});
            
            // Mock compliance service to return report
            when(mockComplianceService.generateComplianceReport(eq(framework), eq("current")))
                    .thenReturn("Test compliance report for " + framework);
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test compliance report for " + framework), 
                    "Should display compliance report for " + framework);
            verify(mockComplianceService).generateComplianceReport(eq(framework), eq("current"));
        }
        
        @ParameterizedTest
        @DisplayName("Should generate reports for different periods")
        @ValueSource(strings = {"current", "Q1-2025", "Q2-2025", "annual-2024", "H1-2025"})
        void shouldGenerateReportsForDifferentPeriods(String period) {
            // Given
            command.setOperation("report");
            command.setArgs(new String[]{"--period=" + period});
            
            // Mock compliance service to return report
            when(mockComplianceService.generateComplianceReport(eq("GDPR"), eq(period)))
                    .thenReturn("Test compliance report for period " + period);
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Test compliance report for period " + period), 
                    "Should display compliance report for period " + period);
            verify(mockComplianceService).generateComplianceReport(eq("GDPR"), eq(period));
        }
    }

    /**
     * Tests for the negative scenarios of the AdminComplianceCommand.
     */
    @Nested
    @DisplayName("Negative Test Cases")
    class NegativeTestCases {
        
        @Test
        @DisplayName("Should handle compliance service unavailable")
        void shouldHandleComplianceServiceUnavailable() {
            // Given
            ServiceManager noServiceManager = mock(ServiceManager.class);
            when(noServiceManager.getComplianceService()).thenReturn(null);
            
            AdminComplianceCommand command = new AdminComplianceCommand(noServiceManager);
            command.setOperation("report");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Compliance service is not available"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle exception when generating compliance report")
        void shouldHandleExceptionWhenGeneratingComplianceReport() {
            // Given
            command.setOperation("report");
            
            // Mock compliance service to throw exception
            when(mockComplianceService.generateComplianceReport(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Test error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error generating compliance report"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle missing project in configure command")
        void shouldHandleMissingProjectInConfigureCommand() {
            // Given
            command.setOperation("configure");
            command.setArgs(new String[0]);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Missing required parameter --project"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle missing reviewer in configure command")
        void shouldHandleMissingReviewerInConfigureCommand() throws Exception {
            // Given
            command.setOperation("configure");
            command.setArgs(new String[]{"--project=test-project"});
            
            // Mock scanner
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine())
                .thenReturn("1")    // GDPR framework
                .thenReturn("");    // Empty reviewer
            setScanner(command, mockScanner);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Compliance reviewer cannot be empty"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle failure when configuring project compliance")
        void shouldHandleFailureWhenConfiguringProjectCompliance() throws Exception {
            // Given
            command.setOperation("configure");
            command.setArgs(new String[]{"--project=test-project"});
            
            // Mock scanner
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine())
                .thenReturn("1")    // GDPR framework
                .thenReturn("compliance-reviewer@example.com");  // Reviewer
            setScanner(command, mockScanner);
            
            // Mock compliance service to return failure
            when(mockComplianceService.configureProjectCompliance(
                    anyString(), anyList(), anyString()))
                .thenReturn(false);
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Failed to update compliance configuration"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle exception when configuring project compliance")
        void shouldHandleExceptionWhenConfiguringProjectCompliance() throws Exception {
            // Given
            command.setOperation("configure");
            command.setArgs(new String[]{"--project=test-project"});
            
            // Mock scanner
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine())
                .thenReturn("1")    // GDPR framework
                .thenReturn("compliance-reviewer@example.com");  // Reviewer
            setScanner(command, mockScanner);
            
            // Mock compliance service to throw exception
            when(mockComplianceService.configureProjectCompliance(
                    anyString(), anyList(), anyString()))
                .thenThrow(new RuntimeException("Test error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error configuring compliance"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle missing project in validate command")
        void shouldHandleMissingProjectInValidateCommand() {
            // Given
            command.setOperation("validate");
            command.setArgs(new String[0]);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error: Missing required parameter --project"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle exception when validating project compliance")
        void shouldHandleExceptionWhenValidatingProjectCompliance() {
            // Given
            command.setOperation("validate");
            command.setArgs(new String[]{"--project=test-project"});
            
            // Mock compliance service to throw exception
            when(mockComplianceService.validateProjectCompliance(anyString()))
                    .thenThrow(new RuntimeException("Test error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error validating compliance"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle exception when getting project compliance status")
        void shouldHandleExceptionWhenGettingProjectComplianceStatus() {
            // Given
            command.setOperation("status");
            command.setArgs(new String[]{"--project=test-project"});
            
            // Mock compliance service to throw exception
            when(mockComplianceService.getProjectComplianceStatus(anyString()))
                    .thenThrow(new RuntimeException("Test error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error getting compliance status"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle exception when getting system compliance status")
        void shouldHandleExceptionWhenGettingSystemComplianceStatus() {
            // Given
            command.setOperation("status");
            command.setArgs(new String[0]);
            
            // Mock compliance service to throw exception
            when(mockComplianceService.getSystemComplianceStatus())
                    .thenThrow(new RuntimeException("Test error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error getting compliance status"), 
                    "Should show error message");
        }
        
        // Additional negative tests
        
        @Test
        @DisplayName("Should handle invalid framework input format")
        void shouldHandleInvalidFrameworkInputFormat() throws Exception {
            // Given
            command.setOperation("configure");
            command.setArgs(new String[]{"--project=test-project"});
            
            // Mock scanner with invalid framework input (not numeric or name)
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine())
                .thenReturn("invalid,input")  // Invalid framework input
                .thenReturn("compliance-reviewer@example.com");  // Reviewer
            setScanner(command, mockScanner);
            
            // List containing the actual framework names from the invalid input
            List<String> expectedFrameworks = Arrays.asList("invalid", "input");
            
            // Mock compliance service to return success
            when(mockComplianceService.configureProjectCompliance(
                    eq("test-project"), 
                    eq(expectedFrameworks), 
                    eq("compliance-reviewer@example.com")))
                .thenReturn(true);
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            verify(mockComplianceService).configureProjectCompliance(
                    eq("test-project"), 
                    eq(expectedFrameworks), 
                    eq("compliance-reviewer@example.com"));
        }
        
        @Test
        @DisplayName("Should handle out-of-range framework number")
        void shouldHandleOutOfRangeFrameworkNumber() throws Exception {
            // Given
            command.setOperation("configure");
            command.setArgs(new String[]{"--project=test-project"});
            
            // Mock scanner with out-of-range framework number
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine())
                .thenReturn("6,7")  // Out-of-range framework numbers
                .thenReturn("compliance-reviewer@example.com");  // Reviewer
            setScanner(command, mockScanner);
            
            // Empty list because framework numbers are out of range
            List<String> expectedFrameworks = new ArrayList<>();
            
            // Mock compliance service to return success
            when(mockComplianceService.configureProjectCompliance(
                    eq("test-project"), 
                    eq(expectedFrameworks), 
                    eq("compliance-reviewer@example.com")))
                .thenReturn(true);
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            verify(mockComplianceService).configureProjectCompliance(
                    eq("test-project"), 
                    eq(expectedFrameworks), 
                    eq("compliance-reviewer@example.com"));
        }
        
        @Test
        @DisplayName("Should handle null arguments")
        void shouldHandleNullArguments() {
            // Given
            command.setOperation("report");
            command.setArgs(null);
            
            // Mock compliance service with default values
            when(mockComplianceService.generateComplianceReport(eq("GDPR"), eq("current")))
                    .thenReturn("Test compliance report");
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            verify(mockComplianceService).generateComplianceReport(eq("GDPR"), eq("current"));
        }
        
        @Test
        @DisplayName("Should handle NullPointerException when validating project compliance")
        void shouldHandleNullPointerExceptionWhenValidatingProjectCompliance() {
            // Given
            command.setOperation("validate");
            command.setArgs(new String[]{"--project=test-project"});
            
            // Mock compliance service to throw NullPointerException
            when(mockComplianceService.validateProjectCompliance(anyString()))
                    .thenThrow(new NullPointerException("Test NPE"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error validating compliance"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle IllegalArgumentException when generating compliance report")
        void shouldHandleIllegalArgumentExceptionWhenGeneratingComplianceReport() {
            // Given
            command.setOperation("report");
            command.setArgs(new String[]{"--type=INVALID_FRAMEWORK"});
            
            // Mock compliance service to throw IllegalArgumentException
            when(mockComplianceService.generateComplianceReport(eq("INVALID_FRAMEWORK"), anyString()))
                    .thenThrow(new IllegalArgumentException("Invalid framework"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error generating compliance report"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle unexpected RuntimeException in report command")
        void shouldHandleUnexpectedRuntimeExceptionInReportCommand() {
            // Given
            command.setOperation("report");
            
            // Mock compliance service to throw RuntimeException
            when(mockComplianceService.generateComplianceReport(anyString(), anyString()))
                    .thenThrow(new RuntimeException("Unexpected error"));
                    
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode, "Should return error code");
            assertTrue(errContent.toString().contains("Error generating compliance report"), 
                    "Should show error message");
        }
        
        @Test
        @DisplayName("Should handle OutOfMemoryError when generating compliance report")
        void shouldHandleOutOfMemoryErrorWhenGeneratingComplianceReport() {
            // Given
            command.setOperation("report");
            
            // Mock compliance service to throw OutOfMemoryError
            when(mockComplianceService.generateComplianceReport(anyString(), anyString()))
                    .thenThrow(new OutOfMemoryError("Test OOM"));
                    
            // When
            assertThrows(OutOfMemoryError.class, command::call, 
                    "Should propagate OutOfMemoryError");
        }
    }

    /**
     * Tests for the contract between AdminComplianceCommand and its dependencies.
     */
    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should call ServiceManager.getComplianceService")
        void shouldCallServiceManagerGetComplianceService() {
            // Given
            command.setOperation("status");
            
            // When
            command.call();
            
            // Then
            verify(mockServiceManager).getComplianceService();
        }
        
        @Test
        @DisplayName("Should call ComplianceService.generateComplianceReport with correct parameters")
        void shouldCallComplianceServiceGenerateComplianceReportWithCorrectParameters() {
            // Given
            command.setOperation("report");
            command.setArgs(new String[]{"--type=HIPAA", "--period=Q2-2025"});
            
            // When
            command.call();
            
            // Then
            verify(mockComplianceService).generateComplianceReport(eq("HIPAA"), eq("Q2-2025"));
        }
        
        @Test
        @DisplayName("Should call ComplianceService.configureProjectCompliance with correct parameters")
        void shouldCallComplianceServiceConfigureProjectComplianceWithCorrectParameters() throws Exception {
            // Given
            command.setOperation("configure");
            command.setArgs(new String[]{"--project=test-project"});
            
            // Mock scanner
            Scanner mockScanner = mock(Scanner.class);
            when(mockScanner.nextLine())
                .thenReturn("1,4")    // GDPR and PCI-DSS frameworks
                .thenReturn("compliance-reviewer@example.com");  // Reviewer
            setScanner(command, mockScanner);
            
            // Expected frameworks
            List<String> expectedFrameworks = Arrays.asList("GDPR", "PCI-DSS");
            
            // When
            command.call();
            
            // Then
            verify(mockComplianceService).configureProjectCompliance(
                    eq("test-project"), 
                    eq(expectedFrameworks), 
                    eq("compliance-reviewer@example.com"));
        }
        
        @Test
        @DisplayName("Should call ComplianceService.validateProjectCompliance with correct project")
        void shouldCallComplianceServiceValidateProjectComplianceWithCorrectProject() {
            // Given
            command.setOperation("validate");
            command.setArgs(new String[]{"--project=test-project"});
            
            // When
            command.call();
            
            // Then
            verify(mockComplianceService).validateProjectCompliance(eq("test-project"));
        }
        
        @Test
        @DisplayName("Should call ComplianceService.getProjectComplianceStatus with correct project")
        void shouldCallComplianceServiceGetProjectComplianceStatusWithCorrectProject() {
            // Given
            command.setOperation("status");
            command.setArgs(new String[]{"--project=test-project"});
            
            // When
            command.call();
            
            // Then
            verify(mockComplianceService).getProjectComplianceStatus(eq("test-project"));
        }
    }

    /**
     * Tests for integration scenarios of the AdminComplianceCommand.
     */
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Should generate compliance report with real service")
        void shouldGenerateComplianceReportWithRealService() {
            // Given
            ComplianceService realComplianceService = new MockComplianceService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getComplianceService()).thenReturn(realComplianceService);
            
            AdminComplianceCommand command = new AdminComplianceCommand(realServiceManager);
            command.setOperation("report");
            command.setArgs(new String[]{"--type=GDPR", "--period=current"});
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Compliance Report: GDPR"), 
                    "Should display GDPR compliance report header");
            assertTrue(outContent.toString().contains("Reporting Period: current"), 
                    "Should display current reporting period");
            assertTrue(outContent.toString().contains("Compliance Overview:"), 
                    "Should display compliance overview section");
        }
        
        @Test
        @DisplayName("Should display project compliance status with real service")
        void shouldDisplayProjectComplianceStatusWithRealService() {
            // Given
            ComplianceService realComplianceService = new MockComplianceService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getComplianceService()).thenReturn(realComplianceService);
            
            // First configure project compliance
            AdminComplianceCommand configCommand = new AdminComplianceCommand(realServiceManager);
            configCommand.setOperation("configure");
            configCommand.setArgs(new String[]{"--project=test-project"});
            
            // Set up scanner for interactive configure
            setInput("1\ntest-reviewer@example.com\n");
            
            // Execute configure command
            configCommand.call();
            
            // Clear output buffer
            outContent.reset();
            
            // Then check status
            AdminComplianceCommand statusCommand = new AdminComplianceCommand(realServiceManager);
            statusCommand.setOperation("status");
            statusCommand.setArgs(new String[]{"--project=test-project"});
            
            // When
            int exitCode = statusCommand.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Project Compliance Status: test-project"), 
                    "Should display project compliance status header");
            assertTrue(outContent.toString().contains("Frameworks: GDPR"), 
                    "Should display configured framework");
            assertTrue(outContent.toString().contains("Reviewer: test-reviewer@example.com"), 
                    "Should display configured reviewer");
        }
        
        @Test
        @DisplayName("Should validate project compliance with real service")
        void shouldValidateProjectComplianceWithRealService() {
            // Given
            ComplianceService realComplianceService = new MockComplianceService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getComplianceService()).thenReturn(realComplianceService);
            
            // First configure project compliance
            AdminComplianceCommand configCommand = new AdminComplianceCommand(realServiceManager);
            configCommand.setOperation("configure");
            configCommand.setArgs(new String[]{"--project=test-project"});
            
            // Set up scanner for interactive configure
            setInput("2,5\ntest-reviewer@example.com\n");
            
            // Execute configure command
            configCommand.call();
            
            // Clear output buffer
            outContent.reset();
            
            // Then validate
            AdminComplianceCommand validateCommand = new AdminComplianceCommand(realServiceManager);
            validateCommand.setOperation("validate");
            validateCommand.setArgs(new String[]{"--project=test-project"});
            
            // When
            int exitCode = validateCommand.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("Project Compliance Validation: test-project"), 
                    "Should display project validation header");
            assertTrue(outContent.toString().contains("Frameworks: HIPAA, ISO27001"), 
                    "Should display configured frameworks");
            assertTrue(outContent.toString().contains("Validation Results:"), 
                    "Should display validation results section");
        }
        
        @Test
        @DisplayName("Should display system compliance status with real service")
        void shouldDisplaySystemComplianceStatusWithRealService() {
            // Given
            ComplianceService realComplianceService = new MockComplianceService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getComplianceService()).thenReturn(realComplianceService);
            
            AdminComplianceCommand command = new AdminComplianceCommand(realServiceManager);
            command.setOperation("status");
            command.setArgs(new String[0]);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode, "Should return success code");
            assertTrue(outContent.toString().contains("System Compliance Status"), 
                    "Should display system compliance status header");
            assertTrue(outContent.toString().contains("Default Framework:"), 
                    "Should display default framework");
            assertTrue(outContent.toString().contains("Projects Summary:"), 
                    "Should display projects summary section");
            assertTrue(outContent.toString().contains("Issues Summary:"), 
                    "Should display issues summary section");
        }
        
        @Test
        @DisplayName("Should perform full compliance workflow with real service")
        void shouldPerformFullComplianceWorkflowWithRealService() {
            // Given
            ComplianceService realComplianceService = new MockComplianceService();
            ServiceManager realServiceManager = mock(ServiceManager.class);
            when(realServiceManager.getComplianceService()).thenReturn(realComplianceService);
            
            // 1. Configure project compliance
            AdminComplianceCommand configCommand = new AdminComplianceCommand(realServiceManager);
            configCommand.setOperation("configure");
            configCommand.setArgs(new String[]{"--project=test-workflow"});
            
            // Set up scanner for interactive configure
            setInput("1,3\ncompliance-lead@example.com\n");
            
            // Execute configure command
            int configResult = configCommand.call();
            assertEquals(0, configResult, "Configure command should succeed");
            
            // Clear output buffer
            outContent.reset();
            
            // 2. Generate a compliance report
            AdminComplianceCommand reportCommand = new AdminComplianceCommand(realServiceManager);
            reportCommand.setOperation("report");
            reportCommand.setArgs(new String[]{"--type=GDPR"});
            
            int reportResult = reportCommand.call();
            assertEquals(0, reportResult, "Report command should succeed");
            assertTrue(outContent.toString().contains("Compliance Report: GDPR"), 
                    "Should show GDPR report");
            
            // Clear output buffer
            outContent.reset();
            
            // 3. Validate project compliance
            AdminComplianceCommand validateCommand = new AdminComplianceCommand(realServiceManager);
            validateCommand.setOperation("validate");
            validateCommand.setArgs(new String[]{"--project=test-workflow"});
            
            int validateResult = validateCommand.call();
            assertEquals(0, validateResult, "Validate command should succeed");
            assertTrue(outContent.toString().contains("Project Compliance Validation: test-workflow"), 
                    "Should show validation for test-workflow");
            
            // Clear output buffer
            outContent.reset();
            
            // 4. Check project compliance status
            AdminComplianceCommand statusCommand = new AdminComplianceCommand(realServiceManager);
            statusCommand.setOperation("status");
            statusCommand.setArgs(new String[]{"--project=test-workflow"});
            
            int statusResult = statusCommand.call();
            assertEquals(0, statusResult, "Status command should succeed");
            assertTrue(outContent.toString().contains("Project Compliance Status: test-workflow"), 
                    "Should show status for test-workflow");
            assertTrue(outContent.toString().contains("Frameworks: GDPR, SOC2"), 
                    "Should show configured frameworks");
            assertTrue(outContent.toString().contains("Reviewer: compliance-lead@example.com"), 
                    "Should show configured reviewer");
        }
    }
    
    // Original test methods that were already in the file - we keep these for backward compatibility
    
    @Test
    void testReportOperation() {
        // Set up the command
        command.setOperation("report");
        command.setArgs(new String[]{"--type=GDPR", "--period=Q1-2025"});
        
        // Set up the mock to return a sample report
        String sampleReport = "Sample GDPR report for Q1-2025";
        when(mockComplianceService.generateComplianceReport("GDPR", "Q1-2025")).thenReturn(sampleReport);
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result);
        
        // Verify the service method was called with the correct parameters
        verify(mockComplianceService).generateComplianceReport("GDPR", "Q1-2025");
        
        // Verify the output
        String output = outContent.toString();
        assertTrue(output.contains(sampleReport));
    }
    
    @Test
    void testConfigureOperation() {
        // Mock the user input
        String userInput = "1,2\njohnsmith\n";
        System.setIn(new ByteArrayInputStream(userInput.getBytes()));
        
        // Set up the command
        command.setOperation("configure");
        command.setArgs(new String[]{"--project=demo"});
        
        // Configure mock to return success
        when(mockComplianceService.configureProjectCompliance(eq("demo"), any(List.class), eq("johnsmith"))).thenReturn(true);
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result);
        
        // Verify the correct method was called
        verify(mockComplianceService).configureProjectCompliance(eq("demo"), any(List.class), eq("johnsmith"));
    }
    
    @Test
    void testValidateOperation() {
        // Set up the command
        command.setOperation("validate");
        command.setArgs(new String[]{"--project=demo"});
        
        // Set up the mock to return a sample validation result
        String validationResult = "Project demo is partially compliant";
        when(mockComplianceService.validateProjectCompliance("demo")).thenReturn(validationResult);
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result);
        
        // Verify the service method was called
        verify(mockComplianceService).validateProjectCompliance("demo");
        
        // Verify the output
        String output = outContent.toString();
        assertTrue(output.contains(validationResult));
    }
    
    @Test
    void testStatusOperation() {
        // Set up the command
        command.setOperation("status");
        command.setArgs(new String[]{"--project=demo"});
        
        // Set up the mock to return a sample status
        String statusResult = "Compliance Status for project demo: 85% compliant";
        when(mockComplianceService.getProjectComplianceStatus("demo")).thenReturn(statusResult);
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result);
        
        // Verify the service method was called
        verify(mockComplianceService).getProjectComplianceStatus("demo");
        
        // Verify the output
        String output = outContent.toString();
        assertTrue(output.contains(statusResult));
    }
    
    @Test
    void testSystemStatusOperation() {
        // Set up the command
        command.setOperation("status");
        command.setArgs(new String[]{});
        
        // Set up the mock to return a sample system status
        String statusResult = "System Compliance Status: 78% compliant";
        when(mockComplianceService.getSystemComplianceStatus()).thenReturn(statusResult);
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result);
        
        // Verify the service method was called
        verify(mockComplianceService).getSystemComplianceStatus();
        
        // Verify the output
        String output = outContent.toString();
        assertTrue(output.contains(statusResult));
    }
}