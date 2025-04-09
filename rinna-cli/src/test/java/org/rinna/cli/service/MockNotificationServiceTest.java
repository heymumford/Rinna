/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.rinna.cli.notifications.Notification;
import org.rinna.cli.notifications.NotificationType;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MockNotificationService Tests")
class MockNotificationServiceTest {
    private MockNotificationService notificationService;
    
    @BeforeEach
    void setUp() {
        notificationService = new MockNotificationService();
    }
    
    @Nested
    @DisplayName("Notification Creation Tests")
    class NotificationCreationTests {
        @Test
        @DisplayName("Should create and add notification with default read status")
        void shouldCreateAndAddNotificationWithDefaultReadStatus() {
            // When
            UUID notificationId = notificationService.addNotification("Test message", NotificationType.SYSTEM);
            
            // Then
            assertNotNull(notificationId);
            Notification notification = notificationService.getNotification(notificationId);
            assertNotNull(notification);
            assertEquals("Test message", notification.getMessage());
            assertEquals(NotificationType.SYSTEM, notification.getType());
            assertFalse(notificationService.isNotificationRead(notificationId));
        }
        
        @Test
        @DisplayName("Should create and add read notification")
        void shouldCreateAndAddReadNotification() {
            // When
            UUID notificationId = notificationService.addNotification("Read message", NotificationType.UPDATE, true);
            
            // Then
            assertNotNull(notificationId);
            Notification notification = notificationService.getNotification(notificationId);
            assertNotNull(notification);
            assertEquals("Read message", notification.getMessage());
            assertEquals(NotificationType.UPDATE, notification.getType());
            assertTrue(notificationService.isNotificationRead(notificationId));
        }
        
        @Test
        @DisplayName("Should add notification with specified ID")
        void shouldAddNotificationWithSpecifiedId() {
            // Given
            UUID specificId = UUID.randomUUID();
            
            // When
            notificationService.addNotificationWithId(specificId, "Specific ID message", NotificationType.COMMENT, false);
            
            // Then
            Notification notification = notificationService.getNotification(specificId);
            assertNotNull(notification);
            assertEquals(specificId, notification.getId());
            assertEquals("Specific ID message", notification.getMessage());
            assertEquals(NotificationType.COMMENT, notification.getType());
            assertFalse(notificationService.isNotificationRead(specificId));
        }
        
        @Test
        @DisplayName("Should add multiple test notifications")
        void shouldAddMultipleTestNotifications() {
            // When
            notificationService.addTestNotifications(5, 3);
            
            // Then
            List<Notification> allNotifications = notificationService.getAllNotifications();
            List<Notification> unreadNotifications = notificationService.getUnreadNotifications();
            
            assertEquals(5, allNotifications.size());
            assertEquals(3, unreadNotifications.size());
            
            // Verify the notifications alternate between SYSTEM and UPDATE types
            for (int i = 0; i < allNotifications.size(); i++) {
                Notification notification = allNotifications.get(i);
                assertEquals("Test notification " + (i+1), notification.getMessage());
                
                if (i % 2 == 0) {
                    assertEquals(NotificationType.SYSTEM, notification.getType());
                } else {
                    assertEquals(NotificationType.UPDATE, notification.getType());
                }
                
                // First 3 should be unread, last 2 should be read
                if (i < 3) {
                    assertFalse(notificationService.isNotificationRead(notification.getId()));
                } else {
                    assertTrue(notificationService.isNotificationRead(notification.getId()));
                }
            }
        }
    }
    
    @Nested
    @DisplayName("Notification Retrieval Tests")
    class NotificationRetrievalTests {
        @BeforeEach
        void setupNotifications() {
            notificationService.addNotification("Notification 1", NotificationType.SYSTEM, false);
            notificationService.addNotification("Notification 2", NotificationType.UPDATE, true);
            notificationService.addNotification("Notification 3", NotificationType.COMMENT, false);
        }
        
        @Test
        @DisplayName("Should get all notifications")
        void shouldGetAllNotifications() {
            // When
            List<Notification> notifications = notificationService.getAllNotifications();
            
            // Then
            assertEquals(3, notifications.size());
            
            // Verify notification contents
            boolean hasNotification1 = notifications.stream()
                .anyMatch(n -> "Notification 1".equals(n.getMessage()) && n.getType() == NotificationType.SYSTEM);
            boolean hasNotification2 = notifications.stream()
                .anyMatch(n -> "Notification 2".equals(n.getMessage()) && n.getType() == NotificationType.UPDATE);
            boolean hasNotification3 = notifications.stream()
                .anyMatch(n -> "Notification 3".equals(n.getMessage()) && n.getType() == NotificationType.COMMENT);
                
            assertTrue(hasNotification1);
            assertTrue(hasNotification2);
            assertTrue(hasNotification3);
        }
        
        @Test
        @DisplayName("Should get unread notifications")
        void shouldGetUnreadNotifications() {
            // When
            List<Notification> unreadNotifications = notificationService.getUnreadNotifications();
            
            // Then
            assertEquals(2, unreadNotifications.size());
            
            // Verify unread notifications
            boolean hasNotification1 = unreadNotifications.stream()
                .anyMatch(n -> "Notification 1".equals(n.getMessage()));
            boolean hasNotification3 = unreadNotifications.stream()
                .anyMatch(n -> "Notification 3".equals(n.getMessage()));
            boolean hasNotification2 = unreadNotifications.stream()
                .anyMatch(n -> "Notification 2".equals(n.getMessage()));
                
            assertTrue(hasNotification1);
            assertTrue(hasNotification3);
            assertFalse(hasNotification2);
        }
        
        @Test
        @DisplayName("Should get notification by ID")
        void shouldGetNotificationById() {
            // Given
            UUID notificationId = notificationService.getAllNotifications().get(0).getId();
            
            // When
            Notification notification = notificationService.getNotification(notificationId);
            
            // Then
            assertNotNull(notification);
            assertEquals(notificationId, notification.getId());
        }
        
        @Test
        @DisplayName("Should return null for non-existent notification ID")
        void shouldReturnNullForNonExistentNotificationId() {
            // When
            Notification notification = notificationService.getNotification(UUID.randomUUID());
            
            // Then
            assertNull(notification);
        }
        
        @Test
        @DisplayName("Should get unread count")
        void shouldGetUnreadCount() {
            // When
            int unreadCount = notificationService.getUnreadCount();
            
            // Then
            assertEquals(2, unreadCount);
        }
    }
    
    @Nested
    @DisplayName("Notification Status Management Tests")
    class NotificationStatusManagementTests {
        private UUID notification1Id;
        private UUID notification2Id;
        private UUID notification3Id;
        
        @BeforeEach
        void setupNotifications() {
            notification1Id = notificationService.addNotification("Notification 1", NotificationType.SYSTEM, false);
            notification2Id = notificationService.addNotification("Notification 2", NotificationType.UPDATE, false);
            notification3Id = notificationService.addNotification("Notification 3", NotificationType.COMMENT, true);
        }
        
        @Test
        @DisplayName("Should mark notification as read")
        void shouldMarkNotificationAsRead() {
            // Given
            assertFalse(notificationService.isNotificationRead(notification1Id));
            
            // When
            boolean result = notificationService.markAsRead(notification1Id);
            
            // Then
            assertTrue(result);
            assertTrue(notificationService.isNotificationRead(notification1Id));
        }
        
        @Test
        @DisplayName("Should return false when marking non-existent notification as read")
        void shouldReturnFalseWhenMarkingNonExistentNotificationAsRead() {
            // When
            boolean result = notificationService.markAsRead(UUID.randomUUID());
            
            // Then
            assertFalse(result);
        }
        
        @Test
        @DisplayName("Should mark all notifications as read")
        void shouldMarkAllNotificationsAsRead() {
            // Given
            assertEquals(2, notificationService.getUnreadCount());
            
            // When
            int markedCount = notificationService.markAllAsRead();
            
            // Then
            assertEquals(2, markedCount);
            assertEquals(0, notificationService.getUnreadCount());
            assertTrue(notificationService.isNotificationRead(notification1Id));
            assertTrue(notificationService.isNotificationRead(notification2Id));
            assertTrue(notificationService.isNotificationRead(notification3Id));
        }
    }
    
    @Nested
    @DisplayName("Notification Clearing Tests")
    class NotificationClearingTests {
        @BeforeEach
        void setupNotifications() {
            // Add 5 notifications to test clearing
            notificationService.addTestNotifications(5, 3);
        }
        
        @Test
        @DisplayName("Should clear old notifications with default days")
        void shouldClearOldNotificationsWithDefaultDays() {
            // Given
            assertEquals(5, notificationService.getAllNotifications().size());
            
            // When
            int clearedCount = notificationService.clearOldNotifications();
            
            // Then
            assertEquals(2, clearedCount);
            assertEquals(3, notificationService.getAllNotifications().size());
        }
        
        @Test
        @DisplayName("Should clear old notifications with specified days")
        void shouldClearOldNotificationsWithSpecifiedDays() {
            // Given
            assertEquals(5, notificationService.getAllNotifications().size());
            
            // When
            int clearedCount = notificationService.clearOldNotifications(10);
            
            // Then
            assertEquals(3, clearedCount);
            assertEquals(2, notificationService.getAllNotifications().size());
        }
        
        @Test
        @DisplayName("Should return zero when no notifications to clear")
        void shouldReturnZeroWhenNoNotificationsToClear() {
            // Given
            notificationService.reset();
            assertEquals(0, notificationService.getAllNotifications().size());
            
            // When
            int clearedCount = notificationService.clearOldNotifications();
            
            // Then
            assertEquals(0, clearedCount);
        }
    }
    
    @Nested
    @DisplayName("Service Management Tests")
    class ServiceManagementTests {
        @Test
        @DisplayName("Should reset service state")
        void shouldResetServiceState() {
            // Given
            notificationService.addTestNotifications(5, 3);
            assertEquals(5, notificationService.getAllNotifications().size());
            assertEquals(3, notificationService.getUnreadCount());
            
            // When
            notificationService.reset();
            
            // Then
            assertEquals(0, notificationService.getAllNotifications().size());
            assertEquals(0, notificationService.getUnreadCount());
        }
    }
}