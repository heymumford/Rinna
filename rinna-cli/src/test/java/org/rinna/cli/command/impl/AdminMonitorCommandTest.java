/*
 * AdminMonitorCommandTest - Tests for AdminMonitorCommand CLI command
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
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
import org.rinna.cli.service.MockMonitoringService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.MonitoringService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for AdminMonitorCommand.
 */
@ExtendWith(MockitoExtension.class)
class AdminMonitorCommandTest {

    private AdminMonitorCommand adminMonitorCommand;
    @Mock
    private ServiceManager mockServiceManager;
    @Mock
    private MockMonitoringService mockMonitoringService;
    
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
        when(mockServiceManager.getMonitoringService()).thenReturn(mockMonitoringService);
        
        // Create the command
        adminMonitorCommand = new AdminMonitorCommand(mockServiceManager);
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
            Field scannerField = AdminMonitorCommand.class.getDeclaredField("scanner");
            scannerField.setAccessible(true);
            scannerField.set(adminMonitorCommand, scanner);
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
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(output.contains("Usage: rin admin monitor <operation>"), 
                       "Should display usage information");
            assertTrue(output.contains("Operations:"), 
                       "Should list available operations");
            assertTrue(output.contains("dashboard"), 
                       "Should include dashboard operation");
            assertTrue(output.contains("server"), 
                       "Should include server operation");
            assertTrue(output.contains("configure"), 
                       "Should include configure operation");
        }
        
        @Test
        @DisplayName("call with 'help' operation should display help and return success")
        void callWithHelpOperationShouldDisplayHelpAndReturnSuccess() {
            // Arrange
            adminMonitorCommand.setOperation("help");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains("Usage: rin admin monitor <operation>"), 
                       "Should display usage information");
            assertTrue(output.contains("Operations:"), 
                       "Should list available operations");
        }
        
        @Test
        @DisplayName("call with unknown operation should display help and return error")
        void callWithUnknownOperationShouldDisplayHelpAndReturnError() {
            // Arrange
            adminMonitorCommand.setOperation("unknown");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Unknown monitoring operation: unknown"), 
                       "Should indicate unknown operation");
            assertTrue(output.contains("Usage: rin admin monitor <operation>"), 
                       "Should display usage information");
        }
        
        @Test
        @DisplayName("call should display error when monitoring service is not available")
        void callShouldDisplayErrorWhenMonitoringServiceIsNotAvailable() {
            // Arrange
            when(mockServiceManager.getMonitoringService()).thenReturn(null);
            adminMonitorCommand.setOperation("dashboard");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Monitoring service is not available."), 
                       "Should indicate monitoring service is not available");
        }
    }

    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("dashboard operation should display system health dashboard")
        void dashboardOperationShouldDisplaySystemHealthDashboard() {
            // Arrange
            String mockDashboard = 
                "=== System Health Dashboard ===\n" +
                "CPU: 25% | Memory: 65% | Disk: 45%\n" +
                "Server Status: OK | API Status: OK\n" +
                "Active Users: 5 | Open Sessions: 3\n";
            
            when(mockMonitoringService.getDashboard()).thenReturn(mockDashboard);
            adminMonitorCommand.setOperation("dashboard");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains(mockDashboard), 
                       "Should display dashboard returned by service");
            verify(mockMonitoringService).getDashboard();
        }
        
        @Test
        @DisplayName("server operation should display server metrics")
        void serverOperationShouldDisplayServerMetrics() {
            // Arrange
            String mockMetrics = 
                "=== Server Metrics ===\n" +
                "CPU Usage: 25%\n" +
                "Memory Usage: 65%\n" +
                "Disk Usage: 45%\n" +
                "Network: 10Mbps\n";
            
            when(mockMonitoringService.getServerMetrics(false)).thenReturn(mockMetrics);
            adminMonitorCommand.setOperation("server");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains(mockMetrics), 
                       "Should display server metrics returned by service");
            verify(mockMonitoringService).getServerMetrics(false);
        }
        
        @Test
        @DisplayName("server operation with detailed flag should display detailed metrics")
        void serverOperationWithDetailedFlagShouldDisplayDetailedMetrics() {
            // Arrange
            String mockDetailedMetrics = 
                "=== Detailed Server Metrics ===\n" +
                "CPU Usage: 25% (User: 15%, System: 10%)\n" +
                "Memory Usage: 65% (Used: 6.5GB, Free: 3.5GB)\n" +
                "Disk Usage: 45% (Used: 450GB, Free: 550GB)\n" +
                "Network: 10Mbps (In: 5Mbps, Out: 5Mbps)\n";
            
            when(mockMonitoringService.getServerMetrics(true)).thenReturn(mockDetailedMetrics);
            adminMonitorCommand.setOperation("server");
            adminMonitorCommand.setArgs(new String[]{"--detailed"});
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains(mockDetailedMetrics), 
                       "Should display detailed server metrics returned by service");
            verify(mockMonitoringService).getServerMetrics(true);
        }
        
        @Test
        @DisplayName("report operation should generate system performance report")
        void reportOperationShouldGenerateSystemPerformanceReport() {
            // Arrange
            String mockReport = 
                "=== Daily Performance Report ===\n" +
                "Generated: 2025-04-08 12:00:00\n" +
                "Average CPU: 30%\n" +
                "Average Memory: 55%\n" +
                "Peak Load: 85% at 08:30:00\n" +
                "Total Errors: 5\n";
            
            when(mockMonitoringService.generateReport("daily")).thenReturn(mockReport);
            adminMonitorCommand.setOperation("report");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains(mockReport), 
                       "Should display performance report returned by service");
            verify(mockMonitoringService).generateReport("daily");
        }
        
        @Test
        @DisplayName("report operation with period should generate report for specified period")
        void reportOperationWithPeriodShouldGenerateReportForSpecifiedPeriod() {
            // Arrange
            String mockWeeklyReport = 
                "=== Weekly Performance Report ===\n" +
                "Generated: 2025-04-08 12:00:00\n" +
                "Average CPU: 35%\n" +
                "Average Memory: 60%\n" +
                "Peak Load: 90% on Tuesday at 14:30:00\n" +
                "Total Errors: 12\n";
            
            when(mockMonitoringService.generateReport("weekly")).thenReturn(mockWeeklyReport);
            adminMonitorCommand.setOperation("report");
            adminMonitorCommand.setArgs(new String[]{"--period=weekly"});
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains(mockWeeklyReport), 
                       "Should display weekly performance report returned by service");
            verify(mockMonitoringService).generateReport("weekly");
        }
        
        @Test
        @DisplayName("sessions operation should display active user sessions")
        void sessionsOperationShouldDisplayActiveUserSessions() {
            // Arrange
            String mockSessions = 
                "=== Active User Sessions ===\n" +
                "user1 - Started: 10:15:30, IP: 192.168.1.101\n" +
                "user2 - Started: 11:20:15, IP: 192.168.1.102\n" +
                "admin - Started: 09:05:45, IP: 192.168.1.100\n";
            
            when(mockMonitoringService.getActiveSessions()).thenReturn(mockSessions);
            adminMonitorCommand.setOperation("sessions");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains(mockSessions), 
                       "Should display active sessions returned by service");
            verify(mockMonitoringService).getActiveSessions();
        }
        
        @Test
        @DisplayName("thresholds operation should display monitoring thresholds")
        void thresholdsOperationShouldDisplayMonitoringThresholds() {
            // Arrange
            String mockThresholds = 
                "=== Monitoring Thresholds ===\n" +
                "CPU Load: 85%\n" +
                "Memory Usage: 90%\n" +
                "Disk Usage: 85%\n" +
                "Network Connections: 1000\n" +
                "Response Time: 500ms\n" +
                "Error Rate: 1%\n" +
                "Refresh Interval: 60s\n";
            
            when(mockMonitoringService.getThresholds()).thenReturn(mockThresholds);
            adminMonitorCommand.setOperation("thresholds");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains(mockThresholds), 
                       "Should display monitoring thresholds returned by service");
            verify(mockMonitoringService).getThresholds();
        }
        
        @Test
        @DisplayName("configure operation should allow updating threshold value")
        void configureOperationShouldAllowUpdatingThresholdValue() {
            // Arrange
            when(mockMonitoringService.configureThreshold("CPU Load", "80")).thenReturn(true);
            adminMonitorCommand.setOperation("configure");
            
            // Setup scanner input for interactive prompt
            setInput("1\n80\n");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains("Monitoring threshold for CPU Load updated to 80"), 
                       "Should confirm threshold update");
            verify(mockMonitoringService).configureThreshold("CPU Load", "80");
        }
        
        @Test
        @DisplayName("configure operation should use default value when input is empty")
        void configureOperationShouldUseDefaultValueWhenInputIsEmpty() {
            // Arrange
            when(mockMonitoringService.configureThreshold("Memory Usage", "90")).thenReturn(true);
            adminMonitorCommand.setOperation("configure");
            
            // Setup scanner input for interactive prompt - empty input for threshold value
            setInput("2\n\n");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains("Monitoring threshold for Memory Usage updated to 90"), 
                       "Should confirm threshold update with default value");
            verify(mockMonitoringService).configureThreshold("Memory Usage", "90");
        }
        
        @Test
        @DisplayName("alerts list operation should display monitoring alerts")
        void alertsListOperationShouldDisplayMonitoringAlerts() {
            // Arrange
            String mockAlerts = 
                "=== Monitoring Alerts ===\n" +
                "High CPU Usage - Metric: CPU Load, Threshold: 90%\n" +
                "Low Disk Space - Metric: Disk Usage, Threshold: 95%\n" +
                "Service Latency - Metric: Response Time, Threshold: 1000ms\n";
            
            when(mockMonitoringService.listAlerts()).thenReturn(mockAlerts);
            adminMonitorCommand.setOperation("alerts");
            adminMonitorCommand.setArgs(new String[]{"list"});
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains(mockAlerts), 
                       "Should display alerts returned by service");
            verify(mockMonitoringService).listAlerts();
        }
        
        @Test
        @DisplayName("alerts remove operation should remove an alert")
        void alertsRemoveOperationShouldRemoveAlert() {
            // Arrange
            when(mockMonitoringService.removeAlert("High CPU Usage")).thenReturn(true);
            adminMonitorCommand.setOperation("alerts");
            adminMonitorCommand.setArgs(new String[]{"remove", "High CPU Usage"});
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains("Monitoring alert 'High CPU Usage' removed successfully"), 
                       "Should confirm alert removal");
            verify(mockMonitoringService).removeAlert("High CPU Usage");
        }
        
        @Test
        @DisplayName("alerts add operation should add a new alert")
        void alertsAddOperationShouldAddNewAlert() {
            // Arrange
            when(mockMonitoringService.addAlert(
                eq("Critical CPU Alert"), 
                eq("CPU Load"), 
                eq("95"), 
                eq(Arrays.asList("admin@example.com", "ops@example.com"))))
                .thenReturn(true);
                
            adminMonitorCommand.setOperation("alerts");
            adminMonitorCommand.setArgs(new String[]{"add"});
            
            // Setup scanner input for interactive prompt
            setInput("Critical CPU Alert\n1\n95\nadmin@example.com, ops@example.com\n");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains("Monitoring alert 'Critical CPU Alert' created successfully"), 
                       "Should confirm alert creation");
            verify(mockMonitoringService).addAlert(
                eq("Critical CPU Alert"), 
                eq("CPU Load"), 
                eq("95"), 
                eq(Arrays.asList("admin@example.com", "ops@example.com")));
        }
    }

    @Nested
    @DisplayName("Negative Test Cases")
    class NegativeTestCases {
        
        @Test
        @DisplayName("dashboard operation should handle service exceptions")
        void dashboardOperationShouldHandleServiceExceptions() {
            // Arrange
            when(mockMonitoringService.getDashboard()).thenThrow(new RuntimeException("Service unavailable"));
            adminMonitorCommand.setOperation("dashboard");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error displaying dashboard: Service unavailable"), 
                       "Should display error message from service");
        }
        
        @Test
        @DisplayName("server operation should handle service exceptions")
        void serverOperationShouldHandleServiceExceptions() {
            // Arrange
            when(mockMonitoringService.getServerMetrics(anyBoolean()))
                .thenThrow(new RuntimeException("Metrics unavailable"));
            adminMonitorCommand.setOperation("server");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error getting server metrics: Metrics unavailable"), 
                       "Should display error message from service");
        }
        
        @Test
        @DisplayName("report operation should reject invalid period")
        void reportOperationShouldRejectInvalidPeriod() {
            // Arrange
            adminMonitorCommand.setOperation("report");
            adminMonitorCommand.setArgs(new String[]{"--period=invalid"});
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Invalid period"), 
                       "Should indicate invalid period");
            assertTrue(error.contains("Must be one of: hourly, daily, weekly, monthly"), 
                       "Should list valid period options");
        }
        
        @Test
        @DisplayName("report operation should handle service exceptions")
        void reportOperationShouldHandleServiceExceptions() {
            // Arrange
            when(mockMonitoringService.generateReport(anyString()))
                .thenThrow(new RuntimeException("Report generation failed"));
            adminMonitorCommand.setOperation("report");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error generating report: Report generation failed"), 
                       "Should display error message from service");
        }
        
        @Test
        @DisplayName("configure operation should handle invalid threshold selection")
        void configureOperationShouldHandleInvalidThresholdSelection() {
            // Arrange
            adminMonitorCommand.setOperation("configure");
            
            // Setup scanner input for interactive prompt with invalid selection
            setInput("0\n");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Invalid selection"), 
                       "Should indicate invalid threshold selection");
        }
        
        @Test
        @DisplayName("configure operation should handle service failure")
        void configureOperationShouldHandleServiceFailure() {
            // Arrange
            when(mockMonitoringService.configureThreshold(anyString(), anyString())).thenReturn(false);
            adminMonitorCommand.setOperation("configure");
            
            // Setup scanner input for interactive prompt
            setInput("1\n80\n");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Failed to update monitoring threshold"), 
                       "Should indicate failure to update threshold");
        }
        
        @Test
        @DisplayName("configure operation should handle service exceptions")
        void configureOperationShouldHandleServiceExceptions() {
            // Arrange
            when(mockMonitoringService.configureThreshold(anyString(), anyString()))
                .thenThrow(new RuntimeException("Configuration failed"));
            adminMonitorCommand.setOperation("configure");
            
            // Setup scanner input for interactive prompt
            setInput("1\n80\n");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error configuring threshold: Configuration failed"), 
                       "Should display error message from service");
        }
        
        @Test
        @DisplayName("alerts operation should reject missing subcommand")
        void alertsOperationShouldRejectMissingSubcommand() {
            // Arrange
            adminMonitorCommand.setOperation("alerts");
            adminMonitorCommand.setArgs(new String[0]);
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Missing alerts subcommand"), 
                       "Should indicate missing subcommand");
            assertTrue(error.contains("Use 'add', 'list', or 'remove'"), 
                       "Should list valid subcommands");
        }
        
        @Test
        @DisplayName("alerts operation should reject unknown subcommand")
        void alertsOperationShouldRejectUnknownSubcommand() {
            // Arrange
            adminMonitorCommand.setOperation("alerts");
            adminMonitorCommand.setArgs(new String[]{"unknown"});
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            String output = outContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Unknown alerts operation: unknown"), 
                       "Should indicate unknown subcommand");
            assertTrue(output.contains("Valid operations: add, list, remove"), 
                       "Should list valid subcommands");
        }
        
        @Test
        @DisplayName("alerts remove operation should reject missing alert name")
        void alertsRemoveOperationShouldRejectMissingAlertName() {
            // Arrange
            adminMonitorCommand.setOperation("alerts");
            adminMonitorCommand.setArgs(new String[]{"remove"});
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Missing alert name to remove"), 
                       "Should indicate missing alert name");
        }
        
        @Test
        @DisplayName("alerts remove operation should handle non-existent alert")
        void alertsRemoveOperationShouldHandleNonExistentAlert() {
            // Arrange
            when(mockMonitoringService.removeAlert(anyString())).thenReturn(false);
            adminMonitorCommand.setOperation("alerts");
            adminMonitorCommand.setArgs(new String[]{"remove", "Non-Existent Alert"});
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Failed to remove monitoring alert"), 
                       "Should indicate failure to remove alert");
            assertTrue(error.contains("Does it exist?"), 
                       "Should suggest alert might not exist");
        }
        
        @Test
        @DisplayName("alerts add operation should reject empty alert name")
        void alertsAddOperationShouldRejectEmptyAlertName() {
            // Arrange
            adminMonitorCommand.setOperation("alerts");
            adminMonitorCommand.setArgs(new String[]{"add"});
            
            // Setup scanner input for interactive prompt with empty name
            setInput("\n");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Alert name cannot be empty"), 
                       "Should indicate empty alert name");
        }
        
        @Test
        @DisplayName("alerts add operation should reject invalid metric selection")
        void alertsAddOperationShouldRejectInvalidMetricSelection() {
            // Arrange
            adminMonitorCommand.setOperation("alerts");
            adminMonitorCommand.setArgs(new String[]{"add"});
            
            // Setup scanner input for interactive prompt with invalid metric selection
            setInput("Test Alert\n0\n");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Invalid selection"), 
                       "Should indicate invalid metric selection");
        }
        
        @Test
        @DisplayName("alerts add operation should reject empty threshold")
        void alertsAddOperationShouldRejectEmptyThreshold() {
            // Arrange
            adminMonitorCommand.setOperation("alerts");
            adminMonitorCommand.setArgs(new String[]{"add"});
            
            // Setup scanner input for interactive prompt with empty threshold
            setInput("Test Alert\n1\n\n");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Threshold cannot be empty"), 
                       "Should indicate empty threshold");
        }
        
        @Test
        @DisplayName("alerts add operation should reject empty recipients")
        void alertsAddOperationShouldRejectEmptyRecipients() {
            // Arrange
            adminMonitorCommand.setOperation("alerts");
            adminMonitorCommand.setArgs(new String[]{"add"});
            
            // Setup scanner input for interactive prompt with empty recipients
            setInput("Test Alert\n1\n90\n\n");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Recipients cannot be empty"), 
                       "Should indicate empty recipients");
        }
        
        @Test
        @DisplayName("alerts add operation should handle service failure")
        void alertsAddOperationShouldHandleServiceFailure() {
            // Arrange
            when(mockMonitoringService.addAlert(anyString(), anyString(), anyString(), anyList()))
                .thenReturn(false);
            adminMonitorCommand.setOperation("alerts");
            adminMonitorCommand.setArgs(new String[]{"add"});
            
            // Setup scanner input for interactive prompt
            setInput("Test Alert\n1\n90\nadmin@example.com\n");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error: Failed to create monitoring alert"), 
                       "Should indicate failure to create alert");
        }
        
        @Test
        @DisplayName("sessions operation should handle service exceptions")
        void sessionsOperationShouldHandleServiceExceptions() {
            // Arrange
            when(mockMonitoringService.getActiveSessions())
                .thenThrow(new RuntimeException("Session data unavailable"));
            adminMonitorCommand.setOperation("sessions");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error getting active sessions: Session data unavailable"), 
                       "Should display error message from service");
        }
        
        @Test
        @DisplayName("thresholds operation should handle service exceptions")
        void thresholdsOperationShouldHandleServiceExceptions() {
            // Arrange
            when(mockMonitoringService.getThresholds())
                .thenThrow(new RuntimeException("Threshold data unavailable"));
            adminMonitorCommand.setOperation("thresholds");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String error = errContent.toString();
            assertEquals(1, result, "Should return error code 1");
            assertTrue(error.contains("Error getting monitoring thresholds: Threshold data unavailable"), 
                       "Should display error message from service");
        }
    }

    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should use monitoringService.getDashboard for dashboard operation")
        void shouldUseDashboardServiceForDashboardOperation() {
            // Arrange
            adminMonitorCommand.setOperation("dashboard");
            
            // Act
            adminMonitorCommand.call();
            
            // Assert
            verify(mockMonitoringService).getDashboard();
        }
        
        @Test
        @DisplayName("Should use monitoringService.getServerMetrics for server operation")
        void shouldUseServerMetricsServiceForServerOperation() {
            // Arrange
            adminMonitorCommand.setOperation("server");
            
            // Act
            adminMonitorCommand.call();
            
            // Assert
            verify(mockMonitoringService).getServerMetrics(false);
        }
        
        @Test
        @DisplayName("Should use monitoringService.generateReport for report operation")
        void shouldUseGenerateReportServiceForReportOperation() {
            // Arrange
            adminMonitorCommand.setOperation("report");
            
            // Act
            adminMonitorCommand.call();
            
            // Assert
            verify(mockMonitoringService).generateReport("daily");
        }
        
        @Test
        @DisplayName("Should use monitoringService.configureThreshold for configure operation")
        void shouldUseConfigureThresholdServiceForConfigureOperation() {
            // Arrange
            adminMonitorCommand.setOperation("configure");
            setInput("1\n80\n");
            
            when(mockMonitoringService.configureThreshold(anyString(), anyString())).thenReturn(true);
            
            // Act
            adminMonitorCommand.call();
            
            // Assert
            verify(mockMonitoringService).configureThreshold("CPU Load", "80");
        }
        
        @Test
        @DisplayName("Should use monitoringService.listAlerts for alerts list operation")
        void shouldUseListAlertsServiceForAlertsListOperation() {
            // Arrange
            adminMonitorCommand.setOperation("alerts");
            adminMonitorCommand.setArgs(new String[]{"list"});
            
            // Act
            adminMonitorCommand.call();
            
            // Assert
            verify(mockMonitoringService).listAlerts();
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Should handle complete workflow for configuring a threshold")
        void shouldHandleCompleteWorkflowForConfiguringThreshold() {
            // Arrange
            when(mockMonitoringService.configureThreshold("CPU Load", "75")).thenReturn(true);
            adminMonitorCommand.setOperation("configure");
            
            // Setup scanner input for interactive prompt
            setInput("1\n75\n");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            
            // Verify the workflow stages appeared in the output
            assertTrue(output.contains("Monitoring Configuration"), 
                       "Should display configuration header");
            assertTrue(output.contains("Select threshold to configure:"), 
                       "Should prompt for threshold selection");
            assertTrue(output.contains("Enter new threshold value for CPU Load"), 
                       "Should prompt for new threshold value");
            assertTrue(output.contains("Monitoring threshold for CPU Load updated to 75"), 
                       "Should confirm threshold update");
            
            // Verify the service was called correctly
            verify(mockMonitoringService).configureThreshold("CPU Load", "75");
        }
        
        @Test
        @DisplayName("Should handle complete workflow for adding a monitoring alert")
        void shouldHandleCompleteWorkflowForAddingMonitoringAlert() {
            // Arrange
            when(mockMonitoringService.addAlert(
                anyString(), anyString(), anyString(), anyList())).thenReturn(true);
            adminMonitorCommand.setOperation("alerts");
            adminMonitorCommand.setArgs(new String[]{"add"});
            
            // Setup scanner input for interactive prompt
            setInput("Memory Alert\n2\n95\nadmin@example.com, ops@example.com\n");
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            
            // Verify the workflow stages appeared in the output
            assertTrue(output.contains("Create Monitoring Alert"), 
                       "Should display alert creation header");
            assertTrue(output.contains("Enter alert name:"), 
                       "Should prompt for alert name");
            assertTrue(output.contains("Select metric:"), 
                       "Should prompt for metric selection");
            assertTrue(output.contains("Enter threshold value:"), 
                       "Should prompt for threshold value");
            assertTrue(output.contains("Enter notification recipients"), 
                       "Should prompt for recipients");
            assertTrue(output.contains("Monitoring alert 'Memory Alert' created successfully"), 
                       "Should confirm alert creation");
            
            // Verify the service was called correctly
            verify(mockMonitoringService).addAlert(
                eq("Memory Alert"), 
                eq("Memory Usage"), 
                eq("95"), 
                eq(Arrays.asList("admin@example.com", "ops@example.com")));
        }
        
        @Test
        @DisplayName("Should handle complete workflow for generating report with custom period")
        void shouldHandleCompleteWorkflowForGeneratingReportWithCustomPeriod() {
            // Arrange
            String mockMonthlyReport = 
                "=== Monthly Performance Report ===\n" +
                "Generated: 2025-04-08 12:00:00\n" +
                "Average CPU: 40%\n" +
                "Average Memory: 65%\n" +
                "Peak Load: 95% on April 3 at 10:15:00\n" +
                "Total Errors: 45\n";
                
            when(mockMonitoringService.generateReport("monthly")).thenReturn(mockMonthlyReport);
            adminMonitorCommand.setOperation("report");
            adminMonitorCommand.setArgs(new String[]{"--period=monthly"});
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains(mockMonthlyReport), 
                       "Should display monthly performance report returned by service");
            
            // Verify the service was called correctly
            verify(mockMonitoringService).generateReport("monthly");
        }
        
        @Test
        @DisplayName("Should handle workflow for removing an alert")
        void shouldHandleWorkflowForRemovingAlert() {
            // Arrange
            when(mockMonitoringService.removeAlert("Memory Alert")).thenReturn(true);
            adminMonitorCommand.setOperation("alerts");
            adminMonitorCommand.setArgs(new String[]{"remove", "Memory Alert"});
            
            // Act
            int result = adminMonitorCommand.call();
            
            // Assert
            String output = outContent.toString();
            assertEquals(0, result, "Should return success code 0");
            assertTrue(output.contains("Monitoring alert 'Memory Alert' removed successfully"), 
                       "Should confirm alert removal");
            
            // Verify the service was called correctly
            verify(mockMonitoringService).removeAlert("Memory Alert");
        }
        
        @Test
        @DisplayName("Should handle workflow for checking all monitoring components")
        void shouldHandleWorkflowForCheckingAllMonitoringComponents() {
            // Arrange - mock responses for all monitoring operations
            when(mockMonitoringService.getDashboard()).thenReturn("Dashboard data");
            when(mockMonitoringService.getServerMetrics(false)).thenReturn("Server metrics");
            when(mockMonitoringService.getThresholds()).thenReturn("Thresholds data");
            when(mockMonitoringService.getActiveSessions()).thenReturn("Sessions data");
            
            // Create a new command for each operation to avoid scanner issues
            AdminMonitorCommand dashboardCommand = new AdminMonitorCommand(mockServiceManager);
            dashboardCommand.setOperation("dashboard");
            
            AdminMonitorCommand serverCommand = new AdminMonitorCommand(mockServiceManager);
            serverCommand.setOperation("server");
            
            AdminMonitorCommand thresholdsCommand = new AdminMonitorCommand(mockServiceManager);
            thresholdsCommand.setOperation("thresholds");
            
            AdminMonitorCommand sessionsCommand = new AdminMonitorCommand(mockServiceManager);
            sessionsCommand.setOperation("sessions");
            
            // Act - execute each operation in sequence
            int dashboardResult = dashboardCommand.call();
            clearOutput();
            
            int serverResult = serverCommand.call();
            clearOutput();
            
            int thresholdsResult = thresholdsCommand.call();
            clearOutput();
            
            int sessionsResult = sessionsCommand.call();
            
            // Assert - all operations should succeed
            assertEquals(0, dashboardResult, "Dashboard operation should succeed");
            assertEquals(0, serverResult, "Server operation should succeed");
            assertEquals(0, thresholdsResult, "Thresholds operation should succeed");
            assertEquals(0, sessionsResult, "Sessions operation should succeed");
            
            // Verify all services were called
            verify(mockMonitoringService).getDashboard();
            verify(mockMonitoringService).getServerMetrics(false);
            verify(mockMonitoringService).getThresholds();
            verify(mockMonitoringService).getActiveSessions();
        }
    }
}