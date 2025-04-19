/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.rinna.cli.command.NotifyCommand;
import org.rinna.cli.command.ReportCommand;
import org.rinna.cli.notifications.Notification;
import org.rinna.cli.notifications.NotificationType;

/**
 * Integration tests for CLI interaction with external services.
 * These tests verify that the CLI can interact with external systems such as
 * email, notification services, and file system operations.
 * 
 * Note: Some tests may be skipped if external services are not available.
 */
@Tag("integration")
@DisplayName("External Service Integration Tests")
class ExternalServiceIntegrationTest {

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    @BeforeEach
    void setUp() {
        // Capture console output
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Nested
    @DisplayName("Notification Service Tests")
    class NotificationServiceTests {
        
        @Test
        @DisplayName("Should list existing notifications")
        void shouldListExistingNotifications() {
            // Set up test notifications using service directly
            Notification notification = Notification.create(
                NotificationType.SYSTEM, 
                "Test notification for integration test", 
                "system"
            );
            
            // Get notification service
            org.rinna.cli.service.MockNotificationService notificationService = 
                new org.rinna.cli.service.MockNotificationService();
            
            // Add a test notification
            notificationService.addNotificationWithId(
                notification.getId(), 
                notification.getMessage(), 
                notification.getType(), 
                false
            );
            
            // Register with service manager
            org.rinna.cli.service.ServiceManager.registerNotificationService(notificationService);
            
            // Setup NotifyCommand for listing
            NotifyCommand notifyCmd = new NotifyCommand();
            notifyCmd.setList(true);
            
            // Execute command
            int exitCode = notifyCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "Command should execute successfully");
            
            // Verify output contains the notification
            String output = outputStream.toString();
            assertTrue(output.contains("Test notification for integration test"), 
                "Output should contain notification message");
        }
        
        @Test
        @DisplayName("Should mark notification as read")
        void shouldMarkNotificationAsRead() {
            // Create a mock notification
            UUID notificationId = UUID.randomUUID();
            
            // Get notification service
            org.rinna.cli.service.MockNotificationService notificationService = 
                new org.rinna.cli.service.MockNotificationService();
            
            // Add a test notification
            notificationService.addNotificationWithId(
                notificationId, 
                "Test notification to mark as read", 
                NotificationType.SYSTEM, 
                false
            );
            
            // Register with service manager
            org.rinna.cli.service.ServiceManager.registerNotificationService(notificationService);
            
            // Setup NotifyCommand for marking as read
            NotifyCommand notifyCmd = new NotifyCommand();
            notifyCmd.setMarkRead(true);
            notifyCmd.setId(notificationId.toString());
            
            // Execute command
            int exitCode = notifyCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "Command should execute successfully");
            
            // Verify notification was marked as read
            assertTrue(notificationService.isNotificationRead(notificationId), 
                "Notification should be marked as read");
        }
    }
    
    @Nested
    @DisplayName("Report Generation Tests")
    @EnabledIfSystemProperty(named = "rinna.test.reports", matches = "true")
    class ReportGenerationTests {
        
        private Path reportDir;
        
        @BeforeEach
        void setupReportDir() throws Exception {
            // Create temporary directory for reports
            reportDir = Files.createTempDirectory("rinna-reports-");
        }
        
        @AfterEach
        void cleanupReportDir() throws Exception {
            // Clean up report files
            if (reportDir != null) {
                Files.walk(reportDir)
                    .sorted(java.util.Comparator.reverseOrder())
                    .map(Path::toFile)
                    .forEach(File::delete);
            }
        }
        
        @Test
        @DisplayName("Should generate simple text report")
        void shouldGenerateSimpleTextReport() {
            // Skip test if the report generator is not available
            assumeTrue(hasReportGenerator(), "Report generator is not available");
            
            // Setup ReportCommand for text report
            ReportCommand reportCmd = new ReportCommand();
            reportCmd.setType("summary");
            reportCmd.setOutput(reportDir.resolve("report.txt").toString());
            reportCmd.setFormat("text");
            
            // Execute command
            int exitCode = reportCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "Command should execute successfully");
            
            // Verify report file was created
            File reportFile = reportDir.resolve("report.txt").toFile();
            assertTrue(reportFile.exists(), "Report file should exist");
            assertTrue(reportFile.length() > 0, "Report file should contain data");
        }
        
        @Test
        @DisplayName("Should generate HTML report")
        void shouldGenerateHtmlReport() {
            // Skip test if the report generator is not available
            assumeTrue(hasReportGenerator(), "Report generator is not available");
            
            // Setup ReportCommand for HTML report
            ReportCommand reportCmd = new ReportCommand();
            reportCmd.setType("summary");
            reportCmd.setOutput(reportDir.resolve("report.html").toString());
            reportCmd.setFormat("html");
            
            // Execute command
            int exitCode = reportCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "Command should execute successfully");
            
            // Verify report file was created
            File reportFile = reportDir.resolve("report.html").toFile();
            assertTrue(reportFile.exists(), "Report file should exist");
            assertTrue(reportFile.length() > 0, "Report file should contain data");
            
            // Check if file contains HTML
            try {
                String content = Files.readString(reportFile.toPath());
                assertTrue(content.contains("<!DOCTYPE html>") || content.contains("<html>"), 
                    "File should contain HTML content");
            } catch (Exception e) {
                fail("Failed to read report file: " + e.getMessage());
            }
        }
        
        // Helper method to check if report generator is available
        private boolean hasReportGenerator() {
            try {
                Class.forName("org.rinna.cli.report.ReportGenerator");
                return true;
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
    }
}