/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.*;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.notifications.Notification;
import org.rinna.cli.notifications.NotificationService;
import org.rinna.cli.notifications.NotificationType;
import org.rinna.cli.security.SecurityManager;

/**
 * Unit tests for the NotifyCommand class.
 * 
 * This test suite follows best practices:
 * 1. Action Handling Tests - Testing each notification action
 * 2. Authentication Tests - Testing authentication requirements
 * 3. Error Handling Tests - Testing error scenarios
 * 4. Output Format Tests - Testing different output formats (text/JSON)
 */
@DisplayName("NotifyCommand Tests")
class NotifyCommandTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private SecurityManager mockSecurityManager;
    private NotificationService mockNotificationService;
    
    // Test data
    private List<Notification> testNotifications;
    private List<Notification> unreadNotifications;
    private UUID testNotificationId;
    
    @BeforeEach
    void setUp() {
        // Capture stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mock security manager and notification service
        mockSecurityManager = mock(SecurityManager.class);
        mockNotificationService = mock(NotificationService.class);
        
        // Set up default responses for security
        when(mockSecurityManager.isAuthenticated()).thenReturn(true);
        when(mockSecurityManager.getCurrentUser()).thenReturn("testuser");
        
        // Set up test notifications
        setupTestNotifications();
        
        // Set up default notification service behavior
        setupNotificationServiceBehavior();
    }
    
    private void setupTestNotifications() {
        testNotifications = new ArrayList<>();
        unreadNotifications = new ArrayList<>();
        testNotificationId = UUID.randomUUID();
        
        // Create some test notifications
        Notification notification1 = mock(Notification.class);
        when(notification1.getId()).thenReturn(testNotificationId);
        when(notification1.getType()).thenReturn(NotificationType.SYSTEM);
        when(notification1.getMessage()).thenReturn("Test system notification");
        when(notification1.getSource()).thenReturn("system");
        when(notification1.getTimestamp()).thenReturn(Instant.now());
        when(notification1.getPriority()).thenReturn(Notification.Priority.MEDIUM);
        when(notification1.isRead()).thenReturn(false);
        when(notification1.format()).thenReturn("2025-04-08 10:00:00 [SYSTEM] Test system notification [UNREAD]");
        
        Notification notification2 = mock(Notification.class);
        when(notification2.getId()).thenReturn(UUID.randomUUID());
        when(notification2.getType()).thenReturn(NotificationType.ASSIGNMENT);
        when(notification2.getMessage()).thenReturn("New work item assigned to you");
        when(notification2.getSource()).thenReturn("system");
        when(notification2.getTimestamp()).thenReturn(Instant.now());
        when(notification2.getPriority()).thenReturn(Notification.Priority.HIGH);
        when(notification2.isRead()).thenReturn(true);
        when(notification2.format()).thenReturn("2025-04-08 09:45:00 [ASSIGNMENT] [HIGH] New work item assigned to you");
        
        Notification notification3 = mock(Notification.class);
        when(notification3.getId()).thenReturn(UUID.randomUUID());
        when(notification3.getType()).thenReturn(NotificationType.COMMENT);
        when(notification3.getMessage()).thenReturn("New comment on work item WI-123");
        when(notification3.getSource()).thenReturn("user1");
        when(notification3.getTimestamp()).thenReturn(Instant.now());
        when(notification3.getPriority()).thenReturn(Notification.Priority.MEDIUM);
        when(notification3.isRead()).thenReturn(false);
        when(notification3.format()).thenReturn("2025-04-08 09:30:00 [COMMENT] New comment on work item WI-123 [UNREAD]");
        
        testNotifications.add(notification1);
        testNotifications.add(notification2);
        testNotifications.add(notification3);
        
        unreadNotifications.add(notification1);
        unreadNotifications.add(notification3);
    }
    
    private void setupNotificationServiceBehavior() {
        when(mockNotificationService.getNotificationsForCurrentUser()).thenReturn(testNotifications);
        when(mockNotificationService.getUnreadNotificationsForCurrentUser()).thenReturn(unreadNotifications);
        when(mockNotificationService.getUnreadCount()).thenReturn(unreadNotifications.size());
        when(mockNotificationService.markAsRead(testNotificationId)).thenReturn(true);
        doNothing().when(mockNotificationService).markAllAsRead();
        when(mockNotificationService.deleteOldNotifications(30)).thenReturn(1);
    }
    
    @AfterEach
    void tearDown() {
        // Restore stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Reset output capture
        outputCaptor.reset();
        errorCaptor.reset();
    }
    
    /**
     * Tests for handling different notification actions.
     */
    @Nested
    @DisplayName("Action Handling Tests")
    class ActionHandlingTests {
        
        @Test
        @DisplayName("Should list all notifications with 'list' action")
        void shouldListAllNotificationsWithListAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("list");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("=== Your Notifications ==="));
                assertTrue(output.contains("[SYSTEM] Test system notification"));
                assertTrue(output.contains("[ASSIGNMENT] [HIGH] New work item assigned to you"));
                assertTrue(output.contains("[COMMENT] New comment on work item WI-123"));
                
                verify(mockNotificationService).getNotificationsForCurrentUser();
            }
        }
        
        @Test
        @DisplayName("Should list unread notifications with 'unread' action")
        void shouldListUnreadNotificationsWithUnreadAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("unread");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("=== Unread Notifications ==="));
                assertTrue(output.contains("[SYSTEM] Test system notification"));
                assertTrue(output.contains("[COMMENT] New comment on work item WI-123"));
                assertFalse(output.contains("[ASSIGNMENT] [HIGH] New work item assigned to you")); // This one is read
                
                verify(mockNotificationService).getUnreadNotificationsForCurrentUser();
            }
        }
        
        @Test
        @DisplayName("Should mark notification as read with 'read' action")
        void shouldMarkNotificationAsReadWithReadAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("read");
                command.setNotificationId(testNotificationId);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Notification marked as read"));
                
                verify(mockNotificationService).markAsRead(testNotificationId);
            }
        }
        
        @Test
        @DisplayName("Should handle 'markread' action same as 'read' action")
        void shouldHandleMarkreadActionSameAsReadAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("markread");
                command.setNotificationId(testNotificationId);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Notification marked as read"));
                
                verify(mockNotificationService).markAsRead(testNotificationId);
            }
        }
        
        @Test
        @DisplayName("Should mark all notifications as read with 'markall' action")
        void shouldMarkAllNotificationsAsReadWithMarkallAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("markall");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("All notifications marked as read"));
                
                verify(mockNotificationService).markAllAsRead();
            }
        }
        
        @Test
        @DisplayName("Should clear old notifications with 'clear' action")
        void shouldClearOldNotificationsWithClearAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("clear");
                command.setDays(30);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Cleared 1 old notification(s)"));
                
                verify(mockNotificationService).deleteOldNotifications(30);
            }
        }
        
        @Test
        @DisplayName("Should display help with 'help' action")
        void shouldDisplayHelpWithHelpAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("help");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("Notification Command Usage:")),
                    () -> assertTrue(output.contains("rin notify list")),
                    () -> assertTrue(output.contains("rin notify unread")),
                    () -> assertTrue(output.contains("rin notify read <id>")),
                    () -> assertTrue(output.contains("rin notify markall")),
                    () -> assertTrue(output.contains("rin notify clear"))
                );
            }
        }
    }
    
    /**
     * Tests for authentication requirements.
     */
    @Nested
    @DisplayName("Authentication Tests")
    class AuthenticationTests {
        
        @Test
        @DisplayName("Should fail when user is not authenticated")
        void shouldFailWhenUserIsNotAuthenticated() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(false);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("list");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("You must be logged in to access notifications"));
                assertTrue(error.contains("Use 'rin login' to authenticate"));
            }
        }
        
        @Test
        @DisplayName("Should show JSON error when not authenticated with JSON output")
        void shouldShowJsonErrorWhenNotAuthenticatedWithJsonOutput() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                when(mockSecurityManager.isAuthenticated()).thenReturn(false);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("list");
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"error\""));
                assertTrue(output.contains("\"message\": \"You must be logged in to access notifications\""));
                assertTrue(output.contains("\"action\": \"Use 'rin login' to authenticate\""));
            }
        }
    }
    
    /**
     * Tests for error handling scenarios.
     */
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should show error for unknown action")
        void shouldShowErrorForUnknownAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("unknown");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Unknown action: unknown"));
            }
        }
        
        @Test
        @DisplayName("Should show error when notification ID is missing for read action")
        void shouldShowErrorWhenNotificationIdIsMissingForReadAction() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("read");
                // No notification ID set
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Notification ID required"));
                assertTrue(error.contains("Usage: rin notify read <notification-id>"));
            }
        }
        
        @Test
        @DisplayName("Should show error when notification marking fails")
        void shouldShowErrorWhenNotificationMarkingFails() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                UUID invalidId = UUID.randomUUID();
                when(mockNotificationService.markAsRead(invalidId)).thenReturn(false);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("read");
                command.setNotificationId(invalidId);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Notification not found or already read"));
            }
        }
        
        @Test
        @DisplayName("Should handle exception thrown by notification service")
        void shouldHandleExceptionThrownByNotificationService() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                when(mockNotificationService.getNotificationsForCurrentUser())
                    .thenThrow(new RuntimeException("Failed to retrieve notifications"));
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("list");
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Failed to retrieve notifications"));
            }
        }
        
        @Test
        @DisplayName("Should show stack trace with verbose flag")
        void shouldShowStackTraceWithVerboseFlag() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                when(mockNotificationService.getNotificationsForCurrentUser())
                    .thenThrow(new RuntimeException("Failed to retrieve notifications"));
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("list");
                command.setVerbose(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(1, result);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Failed to retrieve notifications"));
                assertTrue(error.contains("java.lang.RuntimeException"));
                assertTrue(error.contains("at org.rinna.cli.command.NotifyCommandTest"));
            }
        }
    }
    
    /**
     * Tests for different output formats.
     */
    @Nested
    @DisplayName("Output Format Tests")
    class OutputFormatTests {
        
        @Test
        @DisplayName("Should output in JSON format when list action with JSON flag")
        void shouldOutputInJsonFormatWhenListActionWithJsonFlag() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("list");
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"success\""));
                assertTrue(output.contains("\"action\": \"list\""));
                assertTrue(output.contains("\"count\":"));
                assertTrue(output.contains("\"notifications\":"));
                assertTrue(output.contains("\"id\":"));
                assertTrue(output.contains("\"type\":"));
                assertTrue(output.contains("\"message\":"));
            }
        }
        
        @Test
        @DisplayName("Should output in JSON format when markall action with JSON flag")
        void shouldOutputInJsonFormatWhenMarkallActionWithJsonFlag() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("markall");
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"success\""));
                assertTrue(output.contains("\"action\": \"markall\""));
                assertTrue(output.contains("\"count\":"));
                assertTrue(output.contains("\"message\": \"All notifications marked as read\""));
            }
        }
        
        @Test
        @DisplayName("Should output in JSON format when clear action with JSON flag")
        void shouldOutputInJsonFormatWhenClearActionWithJsonFlag() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("clear");
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"success\""));
                assertTrue(output.contains("\"action\": \"clear\""));
                assertTrue(output.contains("\"deleted\":"));
                assertTrue(output.contains("\"days\":"));
            }
        }
        
        @Test
        @DisplayName("Should output help in JSON format when help action with JSON flag")
        void shouldOutputHelpInJsonFormatWhenHelpActionWithJsonFlag() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("help");
                command.setJsonOutput(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"result\": \"success\""));
                assertTrue(output.contains("\"command\": \"notify\""));
                assertTrue(output.contains("\"usage\":"));
                assertTrue(output.contains("\"actions\":"));
                assertTrue(output.contains("\"options\":"));
            }
        }
        
        @Test
        @DisplayName("Should output additional details when verbose flag is set")
        void shouldOutputAdditionalDetailsWhenVerboseFlagIsSet() {
            // Given
            try (MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class);
                 MockedStatic<NotificationService> notificationServiceMock = Mockito.mockStatic(NotificationService.class)) {
                
                securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityManager);
                notificationServiceMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);
                
                NotifyCommand command = new NotifyCommand();
                command.setAction("list");
                command.setVerbose(true);
                
                // When
                int result = command.call();
                
                // Then
                assertEquals(0, result);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Total notifications:"));
                assertTrue(output.contains("Unread notifications:"));
            }
        }
    }
}