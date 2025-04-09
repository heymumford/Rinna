/*
 * BDD Step definitions for Notification commands
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.bdd;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.rinna.cli.notifications.NotificationType;
import org.rinna.cli.security.SecurityManager;
import org.rinna.cli.service.MockNotificationService;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Step definitions for notification command tests.
 */
public class NotificationCommandSteps {
    
    private final TestContext testContext = TestContext.getInstance();
    private final MockNotificationService mockNotificationService;
    private final CommandProcessor commandProcessor;
    
    /**
     * Constructor initializes test context and mock services.
     */
    public NotificationCommandSteps() {
        mockNotificationService = testContext.getMockNotificationService();
        commandProcessor = testContext.getCommandProcessor();
        mockNotificationService.reset(); // Start with a clean state
    }
    
    /**
     * Set up authentication context for authenticated user.
     */
    @Given("the current user is authenticated")
    public void theCurrentUserIsAuthenticated() {
        Mockito.when(testContext.getMockSecurityService().isAuthenticated()).thenReturn(true);
        Mockito.when(testContext.getMockSecurityService().getCurrentUser()).thenReturn("testuser");
    }
    
    /**
     * Set up authentication context for unauthenticated user.
     */
    @Given("the current user is not authenticated")
    public void theCurrentUserIsNotAuthenticated() {
        Mockito.when(testContext.getMockSecurityService().isAuthenticated()).thenReturn(false);
        Mockito.when(testContext.getMockSecurityService().getCurrentUser()).thenReturn(null);
    }
    
    /**
     * Set up notifications for the test user from a datatable.
     * 
     * @param dataTable table with notification data
     */
    @Given("the user has the following notifications:")
    public void theUserHasTheFollowingNotifications(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        
        for (Map<String, String> row : rows) {
            String typeStr = row.get("Type");
            String message = row.get("Message");
            boolean read = Boolean.parseBoolean(row.get("Read"));
            
            NotificationType type = NotificationType.valueOf(typeStr);
            mockNotificationService.addNotification(message, type, read);
        }
    }
    
    /**
     * Set up a specific notification with a known ID.
     * 
     * @param notificationId the ID for the notification
     * @param message the notification message
     */
    @Given("the user has an unread notification with ID {string} and message {string}")
    public void theUserHasAnUnreadNotificationWithIdAndMessage(String notificationId, String message) {
        UUID id = UUID.fromString(notificationId);
        mockNotificationService.addNotificationWithId(id, message, NotificationType.SYSTEM, false);
        
        // Set up the mock to return true when marking this notification as read
        Mockito.when(mockNotificationService.markAsRead(id)).thenReturn(true);
    }
    
    /**
     * Set up multiple unread notifications.
     * 
     * @param count the number of unread notifications
     */
    @Given("the user has {int} unread notifications")
    public void theUserHasUnreadNotifications(int count) {
        mockNotificationService.addTestNotifications(count, count);
        
        // Set up mock to return the unread count
        Mockito.when(mockNotificationService.getUnreadCount()).thenReturn(count);
    }
    
    /**
     * Set up notifications with some that are older than a certain number of days.
     * 
     * @param total total number of notifications
     * @param older number of notifications older than the specified days
     */
    @Given("the user has {int} notifications, with {int} older than {int} days")
    public void theUserHasNotificationsWithSomeOlderThanDays(int total, int older, int days) {
        mockNotificationService.addTestNotifications(total, 0); // All read for simplicity
        
        // Set up mock for clearOldNotifications
        Mockito.when(mockNotificationService.clearOldNotifications(days)).thenReturn(older);
    }
    
    /**
     * Execute a command and capture the output.
     */
    @When("I run the command {string}")
    public void iRunTheCommand(String commandLine) {
        // Setup the mock methods for the service manager
        setupMockServiceManager();
        
        // Execute the command
        testContext.redirectConsoleOutput();
        testContext.resetCapturedOutput();
        commandProcessor.processCommand(commandLine);
        testContext.restoreConsoleOutput();
        
        // Capture output for assertions
        String output = testContext.getStandardOutput() + testContext.getErrorOutput();
        testContext.setLastCommandOutput(output);
    }
    
    /**
     * Verify the command execution was successful.
     */
    @Then("the command should execute successfully")
    public void theCommandShouldExecuteSuccessfully() {
        Assertions.assertEquals(0, testContext.getLastCommandExitCode(), 
            "Expected command to succeed with exit code 0, but got " + testContext.getLastCommandExitCode());
    }
    
    /**
     * Verify the command execution failed with a specific exit code.
     */
    @Then("the command should fail with exit code {int}")
    public void theCommandShouldFailWithExitCode(int expectedExitCode) {
        Assertions.assertEquals(expectedExitCode, testContext.getLastCommandExitCode(), 
            "Expected command to fail with exit code " + expectedExitCode + 
            ", but got " + testContext.getLastCommandExitCode());
    }
    
    /**
     * Verify that the output contains a specific text.
     */
    @Then("the output should contain {string}")
    public void theOutputShouldContain(String expectedText) {
        Assertions.assertTrue(testContext.getLastCommandOutput().contains(expectedText), 
            "Expected output to contain '" + expectedText + "', but it was not found in:\n" + 
            testContext.getLastCommandOutput());
    }
    
    /**
     * Verify that the output does not contain a specific text.
     */
    @Then("the output should not contain {string}")
    public void theOutputShouldNotContain(String expectedText) {
        Assertions.assertFalse(testContext.getLastCommandOutput().contains(expectedText), 
            "Expected output to NOT contain '" + expectedText + "', but it was found in:\n" + 
            testContext.getLastCommandOutput());
    }
    
    /**
     * Setup mock responses for the service manager methods.
     */
    private void setupMockServiceManager() {
        // Set up mock to return our MockNotificationService
        Mockito.when(testContext.getMockServiceManager().getNotificationService())
               .thenReturn(mockNotificationService);
       
        // Set up authentication mocking
        try (org.mockito.MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
            securityManagerMock.when(SecurityManager::getInstance).thenReturn(testContext.getMockSecurityService());
        }
    }
}
