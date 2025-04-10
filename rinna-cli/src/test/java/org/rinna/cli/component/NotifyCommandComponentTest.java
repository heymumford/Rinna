/**
 * Component test for NotifyCommand with a focus on service integration.
 * 
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.command.NotifyCommand;
import org.rinna.cli.notifications.Notification;
import org.rinna.cli.notifications.Notification.Priority;
import org.rinna.cli.notifications.NotificationType;
import org.rinna.cli.security.SecurityManager;
import org.rinna.cli.service.*;
import org.rinna.cli.util.OutputFormatter;

/**
 * Component tests for the NotifyCommand, focusing on integration with
 * dependent services and hierarchical operation tracking.
 */
public class NotifyCommandComponentTest {

    private AutoCloseable closeable;
    private ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private PrintStream originalOut = System.out;
    private PrintStream originalErr = System.err;
    private NotifyCommand notifyCommand;
    
    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private ConfigurationService mockConfigService;
    
    @Mock
    private MockNotificationService mockNotificationService;
    
    @Mock
    private SecurityManager mockSecurityManager;
    
    private static final String OPERATION_ID = "main-operation-id";
    private static final String SUB_OPERATION_ID = "sub-operation-id";
    private static final String CURRENT_USER = "testuser";
    private static final UUID TEST_NOTIFICATION_ID = UUID.randomUUID();
    
    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Standard service mocking setup
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
        when(mockServiceManager.getMockNotificationService()).thenReturn(mockNotificationService);
        
        // Configure metadata service to return consistent operation IDs
        when(mockMetadataService.startOperation(eq("notify-command"), eq("MANAGE"), any())).thenReturn(OPERATION_ID);
        when(mockMetadataService.startOperation(startsWith("notify-"), anyString(), any())).thenReturn(SUB_OPERATION_ID);
        
        // Configure security manager
        when(mockSecurityManager.isAuthenticated()).thenReturn(true);
        when(mockSecurityManager.getCurrentUser()).thenReturn(CURRENT_USER);
        
        // Configure config service
        when(mockConfigService.getCurrentUser()).thenReturn(CURRENT_USER);
        
        // Initialize command with dependencies
        try (var securityManagerMock = mockStatic(SecurityManager.class)) {
            securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
            notifyCommand = new NotifyCommand(mockServiceManager);
        }
    }
    
    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        System.setErr(originalErr);
        closeable.close();
    }
    
    /**
     * Helper method to create sample notifications.
     */
    private List<Notification> createSampleNotifications() {
        List<Notification> notifications = new ArrayList<>();
        
        // Create notifications with various types
        Notification systemNotification = new Notification(
            NotificationType.SYSTEM,
            "System update available",
            "system",
            CURRENT_USER,
            null,
            Priority.HIGH
        );
        
        Notification updateNotification = new Notification(
            NotificationType.UPDATE,
            "Work item WI-101 has been updated",
            "user1",
            CURRENT_USER,
            "WI-101",
            Priority.MEDIUM
        );
        
        Notification commentNotification = new Notification(
            NotificationType.COMMENT,
            "New comment on WI-102",
            "user2",
            CURRENT_USER,
            "WI-102",
            Priority.LOW
        );
        
        notifications.add(systemNotification);
        notifications.add(updateNotification);
        notifications.add(commentNotification);
        
        return notifications;
    }
    
    @Nested
    @DisplayName("Service Integration Tests")
    class ServiceIntegrationTests {
        
        @Test
        @DisplayName("Should integrate with MetadataService for hierarchical operation tracking")
        void shouldIntegrateWithMetadataServiceForHierarchicalOperationTracking() {
            // Given
            when(mockNotificationService.getCurrentUserNotifications()).thenReturn(createSampleNotifications());
            
            // When
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify main operation tracking
            verify(mockMetadataService).startOperation(eq("notify-command"), eq("MANAGE"), any());
            
            // Verify hierarchical operations for service initialization
            verify(mockMetadataService).startOperation(eq("notify-init-service"), eq("INITIALIZE"), any());
            verify(mockMetadataService).completeOperation(eq(SUB_OPERATION_ID), argThat(map -> 
                map.containsKey("serviceType") && "mock".equals(map.get("serviceType"))));
            
            // Verify hierarchical operations for the list action (default action)
            verify(mockMetadataService).startOperation(eq("notify-list"), eq("READ"), any());
            verify(mockMetadataService).startOperation(eq("notify-list-fetch"), eq("READ"), any());
            verify(mockMetadataService).completeOperation(eq(SUB_OPERATION_ID), argThat(map -> 
                map.containsKey("count") && ((int)map.get("count")) == 3));
            verify(mockMetadataService).startOperation(eq("notify-list-display"), eq("READ"), any());
            verify(mockMetadataService).completeOperation(eq(SUB_OPERATION_ID), argThat(map -> 
                map.containsKey("success") && ((boolean)map.get("success"))));
            
            // Verify main operation completion
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), argThat(map -> 
                map.containsKey("result") && "success".equals(map.get("result"))));
        }
        
        @Test
        @DisplayName("Should integrate with NotificationService to get current user notifications")
        void shouldIntegrateWithNotificationServiceToGetCurrentUserNotifications() {
            // Given
            List<Notification> testNotifications = createSampleNotifications();
            when(mockNotificationService.getCurrentUserNotifications()).thenReturn(testNotifications);
            
            // When
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify interaction with notification service
            verify(mockNotificationService).getCurrentUserNotifications();
            
            // Verify output contains notification information
            String output = outContent.toString();
            assertTrue(output.contains("Your Notifications"));
            assertTrue(output.contains("System update available"));
            assertTrue(output.contains("Work item WI-101 has been updated"));
            assertTrue(output.contains("New comment on WI-102"));
        }
        
        @Test
        @DisplayName("Should handle authentication check")
        void shouldHandleAuthenticationCheck() {
            // Given
            when(mockSecurityManager.isAuthenticated()).thenReturn(false);
            
            // When
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify error message
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("You must be logged in to access notifications"));
            
            // Verify notification service was not called
            verify(mockNotificationService, never()).getCurrentUserNotifications();
            
            // Verify operation was failed
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(SecurityException.class));
        }
    }
    
    @Nested
    @DisplayName("List Action Tests")
    class ListActionTests {
        
        @Test
        @DisplayName("Should display notifications in text format by default")
        void shouldDisplayNotificationsInTextFormatByDefault() {
            // Given
            List<Notification> testNotifications = createSampleNotifications();
            when(mockNotificationService.getCurrentUserNotifications()).thenReturn(testNotifications);
            
            // When
            notifyCommand.setAction("list");
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify output format
            String output = outContent.toString();
            assertTrue(output.contains("=== Your Notifications ==="));
            assertTrue(output.contains("System update available"));
            
            // Verify operation tracking with specific parameters
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).startOperation(eq("notify-list"), eq("READ"), paramsCaptor.capture());
            
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(CURRENT_USER, params.get("username"));
            assertEquals("text", params.get("format"));
        }
        
        @Test
        @DisplayName("Should display notifications in JSON format when requested")
        void shouldDisplayNotificationsInJsonFormatWhenRequested() {
            // Given
            List<Notification> testNotifications = createSampleNotifications();
            when(mockNotificationService.getCurrentUserNotifications()).thenReturn(testNotifications);
            try (var outputFormatterMock = mockStatic(OutputFormatter.class)) {
                outputFormatterMock.when(() -> OutputFormatter.toJson(any())).thenReturn("{\"mockJson\":true}");
                
                // When
                notifyCommand.setAction("list");
                notifyCommand.setFormat("json");
                int exitCode = notifyCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify JSON output
                String output = outContent.toString();
                assertTrue(output.contains("{\"mockJson\":true}"));
                
                // Verify operation tracking with JSON format parameter
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).startOperation(eq("notify-list"), eq("READ"), paramsCaptor.capture());
                
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("json", params.get("format"));
                
                // Verify operation completion with correct parameters
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(SUB_OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertTrue(result.containsKey("count"));
                assertEquals(3, result.get("count"));
            }
        }
        
        @Test
        @DisplayName("Should handle empty notifications list")
        void shouldHandleEmptyNotificationsList() {
            // Given
            when(mockNotificationService.getCurrentUserNotifications()).thenReturn(new ArrayList<>());
            
            // When
            notifyCommand.setAction("list");
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify output
            String output = outContent.toString();
            assertTrue(output.contains("You have no notifications."));
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("notify-list-fetch"), eq("READ"), any());
            verify(mockMetadataService).completeOperation(eq(SUB_OPERATION_ID), argThat(map -> 
                map.containsKey("count") && ((int)map.get("count")) == 0));
        }
        
        @Test
        @DisplayName("Should display additional details in verbose mode")
        void shouldDisplayAdditionalDetailsInVerboseMode() {
            // Given
            List<Notification> testNotifications = createSampleNotifications();
            when(mockNotificationService.getCurrentUserNotifications()).thenReturn(testNotifications);
            when(mockNotificationService.isNotificationRead(any())).thenReturn(false);
            
            // When
            notifyCommand.setAction("list");
            notifyCommand.setVerbose(true);
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify verbose output with counts
            String output = outContent.toString();
            assertTrue(output.contains("Total notifications:"));
            assertTrue(output.contains("Unread notifications:"));
            
            // Verify operation tracking includes verbose parameter
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).startOperation(eq("notify-command"), eq("MANAGE"), paramsCaptor.capture());
            
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(true, params.get("verbose"));
        }
    }
    
    @Nested
    @DisplayName("Unread Action Tests")
    class UnreadActionTests {
        
        @Test
        @DisplayName("Should display only unread notifications")
        void shouldDisplayOnlyUnreadNotifications() {
            // Given
            List<Notification> unreadNotifications = createSampleNotifications().subList(0, 2);
            when(mockNotificationService.getUnreadNotificationsForCurrentUser()).thenReturn(unreadNotifications);
            
            // When
            notifyCommand.setAction("unread");
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify output
            String output = outContent.toString();
            assertTrue(output.contains("=== Unread Notifications ==="));
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("notify-unread"), eq("READ"), any());
            verify(mockMetadataService).startOperation(eq("notify-unread-fetch"), eq("READ"), any());
            
            // Verify completion with correct count
            ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).completeOperation(eq(SUB_OPERATION_ID), resultCaptor.capture());
            
            Map<String, Object> result = resultCaptor.getValue();
            assertEquals(2, result.get("count"));
        }
        
        @Test
        @DisplayName("Should display unread notifications in JSON format when requested")
        void shouldDisplayUnreadNotificationsInJsonFormatWhenRequested() {
            // Given
            List<Notification> unreadNotifications = createSampleNotifications();
            when(mockNotificationService.getUnreadNotificationsForCurrentUser()).thenReturn(unreadNotifications);
            try (var outputFormatterMock = mockStatic(OutputFormatter.class)) {
                outputFormatterMock.when(() -> OutputFormatter.toJson(any())).thenReturn("{\"mockJson\":true}");
                
                // When
                notifyCommand.setAction("unread");
                notifyCommand.setJsonOutput(true);
                int exitCode = notifyCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify JSON output
                String output = outContent.toString();
                assertTrue(output.contains("{\"mockJson\":true}"));
                
                // Verify operation tracking with JSON format in parameters
                verify(mockMetadataService).startOperation(eq("notify-unread-display"), eq("READ"), 
                    argThat(map -> "json".equals(map.get("format"))));
            }
        }
        
        @Test
        @DisplayName("Should handle empty unread notifications list")
        void shouldHandleEmptyUnreadNotificationsList() {
            // Given
            when(mockNotificationService.getUnreadNotificationsForCurrentUser()).thenReturn(new ArrayList<>());
            
            // When
            notifyCommand.setAction("unread");
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify output
            String output = outContent.toString();
            assertTrue(output.contains("You have no unread notifications."));
            
            // Verify operation tracking with count = 0
            verify(mockMetadataService).completeOperation(eq(SUB_OPERATION_ID), argThat(map -> 
                map.containsKey("count") && ((int)map.get("count")) == 0));
        }
    }
    
    @Nested
    @DisplayName("Read Action Tests")
    class ReadActionTests {
        
        @Test
        @DisplayName("Should mark a notification as read")
        void shouldMarkANotificationAsRead() {
            // Given
            when(mockNotificationService.markAsRead(TEST_NOTIFICATION_ID)).thenReturn(true);
            
            // When
            notifyCommand.setAction("read");
            notifyCommand.setNotificationId(TEST_NOTIFICATION_ID);
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify output
            String output = outContent.toString();
            assertTrue(output.contains("Notification marked as read."));
            
            // Verify interaction with notification service
            verify(mockNotificationService).markAsRead(TEST_NOTIFICATION_ID);
            
            // Verify hierarchical operation tracking
            verify(mockMetadataService).startOperation(eq("notify-mark-read"), eq("UPDATE"), 
                argThat(map -> map.containsKey("notificationId") && 
                              TEST_NOTIFICATION_ID.toString().equals(map.get("notificationId"))));
            
            verify(mockMetadataService).startOperation(eq("notify-mark-read-action"), eq("UPDATE"), 
                argThat(map -> map.containsKey("notificationId")));
            
            verify(mockMetadataService).completeOperation(eq(SUB_OPERATION_ID), argThat(map -> 
                map.containsKey("success") && ((boolean)map.get("success"))));
            
            verify(mockMetadataService).startOperation(eq("notify-mark-read-display"), eq("READ"), any());
            
            // Verify main operation completion
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), argThat(map -> 
                map.containsKey("result") && "success".equals(map.get("result"))));
        }
        
        @Test
        @DisplayName("Should handle notification not found or already read")
        void shouldHandleNotificationNotFoundOrAlreadyRead() {
            // Given
            when(mockNotificationService.markAsRead(TEST_NOTIFICATION_ID)).thenReturn(false);
            
            // When
            notifyCommand.setAction("read");
            notifyCommand.setNotificationId(TEST_NOTIFICATION_ID);
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify error message
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("Notification not found or already read"));
            
            // Verify operation tracking includes failure
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalStateException.class));
        }
        
        @Test
        @DisplayName("Should fail when notification ID is not provided")
        void shouldFailWhenNotificationIdIsNotProvided() {
            // Given
            notifyCommand.setAction("read");
            // Intentionally not setting notification ID
            
            // When
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify error message
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("Notification ID required"));
            
            // Verify notification service was not called
            verify(mockNotificationService, never()).markAsRead(any());
            
            // Verify operation tracking includes failure
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
        }
        
        @Test
        @DisplayName("Should display detailed information in JSON format when requested")
        void shouldDisplayDetailedInformationInJsonFormatWhenRequested() {
            // Given
            when(mockNotificationService.markAsRead(TEST_NOTIFICATION_ID)).thenReturn(true);
            try (var outputFormatterMock = mockStatic(OutputFormatter.class)) {
                outputFormatterMock.when(() -> OutputFormatter.toJson(any())).thenReturn("{\"result\":\"success\",\"action\":\"read\"}");
                
                // When
                notifyCommand.setAction("read");
                notifyCommand.setNotificationId(TEST_NOTIFICATION_ID);
                notifyCommand.setJsonOutput(true);
                int exitCode = notifyCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify JSON output
                String output = outContent.toString();
                assertTrue(output.contains("{\"result\":\"success\",\"action\":\"read\"}"));
                
                // Verify operation tracking with format parameter
                verify(mockMetadataService).startOperation(eq("notify-mark-read-display"), eq("READ"), 
                    argThat(map -> "json".equals(map.get("format"))));
            }
        }
    }
    
    @Nested
    @DisplayName("Mark All Action Tests")
    class MarkAllActionTests {
        
        @Test
        @DisplayName("Should mark all notifications as read")
        void shouldMarkAllNotificationsAsRead() {
            // Given
            when(mockNotificationService.getUnreadCountForCurrentUser()).thenReturn(5);
            
            // When
            notifyCommand.setAction("markall");
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify output
            String output = outContent.toString();
            assertTrue(output.contains("All notifications marked as read."));
            
            // Verify interaction with notification service
            verify(mockNotificationService).markAllAsReadForCurrentUser();
            
            // Verify hierarchical operation tracking
            verify(mockMetadataService).startOperation(eq("notify-markall"), eq("UPDATE"), any());
            verify(mockMetadataService).startOperation(eq("notify-markall-count"), eq("READ"), any());
            verify(mockMetadataService).completeOperation(eq(SUB_OPERATION_ID), argThat(map -> 
                map.containsKey("unreadCount") && ((int)map.get("unreadCount")) == 5));
            
            verify(mockMetadataService).startOperation(eq("notify-markall-update"), eq("UPDATE"), 
                argThat(map -> map.containsKey("unreadCount") && ((int)map.get("unreadCount")) == 5));
            
            verify(mockMetadataService).startOperation(eq("notify-markall-display"), eq("READ"), any());
            
            // Verify main operation completion
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), argThat(map -> 
                map.containsKey("success") && ((boolean)map.get("success"))));
        }
        
        @Test
        @DisplayName("Should display marked count in verbose mode")
        void shouldDisplayMarkedCountInVerboseMode() {
            // Given
            when(mockNotificationService.getUnreadCountForCurrentUser()).thenReturn(7);
            
            // When
            notifyCommand.setAction("markall");
            notifyCommand.setVerbose(true);
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify verbose output with count
            String output = outContent.toString();
            assertTrue(output.contains("Marked 7 notifications as read."));
            
            // Verify operation tracking includes verbose parameter
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).startOperation(eq("notify-command"), eq("MANAGE"), paramsCaptor.capture());
            
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(true, params.get("verbose"));
        }
        
        @Test
        @DisplayName("Should display information in JSON format when requested")
        void shouldDisplayInformationInJsonFormatWhenRequested() {
            // Given
            when(mockNotificationService.getUnreadCountForCurrentUser()).thenReturn(3);
            try (var outputFormatterMock = mockStatic(OutputFormatter.class)) {
                outputFormatterMock.when(() -> OutputFormatter.toJson(any())).thenReturn("{\"result\":\"success\",\"action\":\"markall\",\"count\":3}");
                
                // When
                notifyCommand.setAction("markall");
                notifyCommand.setJsonOutput(true);
                int exitCode = notifyCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify JSON output
                String output = outContent.toString();
                assertTrue(output.contains("{\"result\":\"success\",\"action\":\"markall\",\"count\":3}"));
                
                // Verify operation tracking with format parameter
                verify(mockMetadataService).startOperation(eq("notify-markall-display"), eq("READ"), 
                    argThat(map -> "json".equals(map.get("format")) && ((int)map.get("unreadCount")) == 3));
            }
        }
    }
    
    @Nested
    @DisplayName("Clear Action Tests")
    class ClearActionTests {
        
        @Test
        @DisplayName("Should clear old notifications with default retention period")
        void shouldClearOldNotificationsWithDefaultRetentionPeriod() {
            // Given
            when(mockNotificationService.clearOldNotifications(30)).thenReturn(4);
            
            // When
            notifyCommand.setAction("clear");
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify output
            String output = outContent.toString();
            assertTrue(output.contains("Cleared 4 old notification(s)."));
            
            // Verify interaction with notification service
            verify(mockNotificationService).clearOldNotifications(30);
            
            // Verify hierarchical operation tracking
            verify(mockMetadataService).startOperation(eq("notify-clear"), eq("DELETE"), any());
            verify(mockMetadataService).startOperation(eq("notify-clear-delete"), eq("DELETE"), 
                argThat(map -> map.containsKey("days") && ((int)map.get("days")) == 30));
            
            verify(mockMetadataService).completeOperation(eq(SUB_OPERATION_ID), argThat(map -> 
                map.containsKey("deleted") && ((int)map.get("deleted")) == 4));
            
            verify(mockMetadataService).startOperation(eq("notify-clear-display"), eq("READ"), any());
            
            // Verify main operation completion
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), argThat(map -> 
                map.containsKey("result") && "success".equals(map.get("result"))));
        }
        
        @Test
        @DisplayName("Should clear old notifications with custom retention period")
        void shouldClearOldNotificationsWithCustomRetentionPeriod() {
            // Given
            when(mockNotificationService.clearOldNotifications(7)).thenReturn(2);
            
            // When
            notifyCommand.setAction("clear");
            notifyCommand.setDays(7);
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify output
            String output = outContent.toString();
            assertTrue(output.contains("Cleared 2 old notification(s)."));
            
            // Verify interaction with notification service
            verify(mockNotificationService).clearOldNotifications(7);
            
            // Verify operation tracking with days parameter
            verify(mockMetadataService).startOperation(eq("notify-clear-delete"), eq("DELETE"), 
                argThat(map -> map.containsKey("days") && ((int)map.get("days")) == 7));
        }
        
        @Test
        @DisplayName("Should display retention period in verbose mode")
        void shouldDisplayRetentionPeriodInVerboseMode() {
            // Given
            when(mockNotificationService.clearOldNotifications(14)).thenReturn(3);
            
            // When
            notifyCommand.setAction("clear");
            notifyCommand.setDays(14);
            notifyCommand.setVerbose(true);
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify verbose output with retention period
            String output = outContent.toString();
            assertTrue(output.contains("Retention period: 14 days"));
            
            // Verify operation tracking includes verbose parameter
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).startOperation(eq("notify-command"), eq("MANAGE"), paramsCaptor.capture());
            
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(true, params.get("verbose"));
        }
        
        @Test
        @DisplayName("Should handle no old notifications to clear")
        void shouldHandleNoOldNotificationsToClear() {
            // Given
            when(mockNotificationService.clearOldNotifications(anyInt())).thenReturn(0);
            
            // When
            notifyCommand.setAction("clear");
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify output
            String output = outContent.toString();
            assertTrue(output.contains("No old notifications to clear."));
            
            // Verify operation tracking with count = 0
            verify(mockMetadataService).completeOperation(eq(SUB_OPERATION_ID), argThat(map -> 
                map.containsKey("deleted") && ((int)map.get("deleted")) == 0));
        }
        
        @Test
        @DisplayName("Should display information in JSON format when requested")
        void shouldDisplayInformationInJsonFormatWhenRequested() {
            // Given
            when(mockNotificationService.clearOldNotifications(30)).thenReturn(5);
            try (var outputFormatterMock = mockStatic(OutputFormatter.class)) {
                outputFormatterMock.when(() -> OutputFormatter.toJson(any())).thenReturn("{\"result\":\"success\",\"action\":\"clear\",\"deleted\":5}");
                
                // When
                notifyCommand.setAction("clear");
                notifyCommand.setJsonOutput(true);
                int exitCode = notifyCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify JSON output
                String output = outContent.toString();
                assertTrue(output.contains("{\"result\":\"success\",\"action\":\"clear\",\"deleted\":5}"));
                
                // Verify operation tracking with format parameter
                verify(mockMetadataService).startOperation(eq("notify-clear-display"), eq("READ"), 
                    argThat(map -> "json".equals(map.get("format"))));
            }
        }
    }
    
    @Nested
    @DisplayName("Help Action Tests")
    class HelpActionTests {
        
        @Test
        @DisplayName("Should display help information in text format")
        void shouldDisplayHelpInformationInTextFormat() {
            // When
            notifyCommand.setAction("help");
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify help output
            String output = outContent.toString();
            assertTrue(output.contains("Notification Command Usage:"));
            assertTrue(output.contains("rin notify"));
            assertTrue(output.contains("Options:"));
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("notify-help"), eq("READ"), any());
            verify(mockMetadataService).completeOperation(eq(SUB_OPERATION_ID), argThat(map -> 
                map.containsKey("success") && ((boolean)map.get("success"))));
        }
        
        @Test
        @DisplayName("Should display help information in JSON format when requested")
        void shouldDisplayHelpInformationInJsonFormatWhenRequested() {
            // Given
            try (var outputFormatterMock = mockStatic(OutputFormatter.class)) {
                outputFormatterMock.when(() -> OutputFormatter.toJson(any())).thenReturn("{\"mockHelpJson\":true}");
                
                // When
                notifyCommand.setAction("help");
                notifyCommand.setJsonOutput(true);
                int exitCode = notifyCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify JSON output
                String output = outContent.toString();
                assertTrue(output.contains("{\"mockHelpJson\":true}"));
                
                // Verify operation tracking with format parameter
                verify(mockMetadataService).startOperation(eq("notify-help"), eq("READ"), 
                    argThat(map -> "json".equals(map.get("format"))));
            }
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle unknown action with error message and fallback to help")
        void shouldHandleUnknownActionWithErrorMessageAndFallbackToHelp() {
            // When
            notifyCommand.setAction("invalidAction");
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify error message
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("Unknown action: invalidAction"));
            
            // Verify help is displayed as fallback
            String output = outContent.toString();
            assertTrue(output.contains("Notification Command Usage:"));
            
            // Verify operation tracking includes failure
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            
            // Verify fallback help operation
            verify(mockMetadataService).startOperation(eq("notify-help-fallback"), eq("READ"), any());
        }
        
        @Test
        @DisplayName("Should handle exceptions during initialization")
        void shouldHandleExceptionsDuringInitialization() {
            // Given - configure service manager to throw exception when getting mock notification service
            when(mockServiceManager.getMockNotificationService()).thenThrow(new RuntimeException("Service initialization failed"));
            
            // When
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify error message
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("Service initialization failed"));
            
            // Verify operation tracking includes failure
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(RuntimeException.class));
        }
        
        @Test
        @DisplayName("Should handle service exceptions in list action")
        void shouldHandleServiceExceptionsInListAction() {
            // Given
            when(mockNotificationService.getCurrentUserNotifications()).thenThrow(new RuntimeException("Failed to get notifications"));
            
            // When
            notifyCommand.setAction("list");
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify error message
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("Error listing notifications: Failed to get notifications"));
            
            // Verify operation tracking includes failure
            verify(mockMetadataService).failOperation(eq(SUB_OPERATION_ID), any(RuntimeException.class));
        }
        
        @Test
        @DisplayName("Should display stack trace in verbose mode on error")
        void shouldDisplayStackTraceInVerboseModeOnError() {
            // Given
            RuntimeException testException = new RuntimeException("Test exception with stack trace");
            when(mockNotificationService.getCurrentUserNotifications()).thenThrow(testException);
            
            // When
            notifyCommand.setAction("list");
            notifyCommand.setVerbose(true);
            int exitCode = notifyCommand.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify error output contains stack trace elements
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("Error listing notifications: Test exception with stack trace"));
            assertTrue(errorOutput.contains("java.lang.RuntimeException"));
            assertTrue(errorOutput.contains("at org.rinna.cli.component.NotifyCommandComponentTest"));
        }
        
        @Test
        @DisplayName("Should format errors as JSON when JSON output is requested")
        void shouldFormatErrorsAsJsonWhenJsonOutputIsRequested() {
            // Given
            when(mockNotificationService.getCurrentUserNotifications()).thenThrow(new RuntimeException("JSON error test"));
            try (var outputFormatterMock = mockStatic(OutputFormatter.class)) {
                outputFormatterMock.when(() -> OutputFormatter.toJson(argThat(map -> 
                    map.containsKey("result") && "error".equals(map.get("result"))))).thenReturn("{\"result\":\"error\",\"message\":\"JSON error test\"}");
                
                // When
                notifyCommand.setAction("list");
                notifyCommand.setJsonOutput(true);
                int exitCode = notifyCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify JSON error output
                String output = outContent.toString();
                assertTrue(output.contains("{\"result\":\"error\",\"message\":\"JSON error test\"}"));
                
                // Verify no error output to stderr (since it went to stdout as JSON)
                assertEquals("", errContent.toString());
            }
        }
    }
}