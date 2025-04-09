/*
 * Mock notification service for Rinna CLI tests
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.service;

import org.rinna.cli.notifications.Notification;
import org.rinna.cli.notifications.Notification.Priority;
import org.rinna.cli.notifications.NotificationType;
import org.rinna.cli.notifications.NotificationService;
import org.rinna.cli.security.SecurityManager;
import org.rinna.cli.config.SecurityConfig;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mock implementation of notification service for testing and development.
 * This implementation stores notifications in memory with file-based persistence
 * for development and testing scenarios.
 */
public class MockNotificationService {
    private static final String DATA_DIR = System.getProperty("user.home") + "/.rinna/mock-data";
    private static final String NOTIFICATIONS_FILE = DATA_DIR + "/mock-notifications.dat";
    private static final String NOTIFICATION_PROPS_FILE = DATA_DIR + "/notification-settings.properties";
    
    private final List<Notification> notifications = new ArrayList<>();
    private final Map<UUID, Boolean> readStatus = new HashMap<>();
    private final Properties notificationProps = new Properties();
    
    private static MockNotificationService instance;
    
    /**
     * Get the singleton instance.
     * 
     * @return the instance
     */
    public static synchronized MockNotificationService getInstance() {
        if (instance == null) {
            instance = new MockNotificationService();
        }
        return instance;
    }
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private MockNotificationService() {
        initializeStorageDirectory();
        loadNotifications();
        loadProperties();
    }
    
    /**
     * Initialize the storage directory for persistence.
     */
    private void initializeStorageDirectory() {
        File dataDir = new File(DATA_DIR);
        if (!dataDir.exists()) {
            dataDir.mkdirs();
        }
    }
    
    /**
     * Load notification properties from file.
     */
    private void loadProperties() {
        File propsFile = new File(NOTIFICATION_PROPS_FILE);
        if (propsFile.exists()) {
            try (FileInputStream fis = new FileInputStream(propsFile)) {
                notificationProps.load(fis);
            } catch (IOException e) {
                // Log error but continue with defaults
                System.err.println("Error loading notification properties: " + e.getMessage());
            }
        } else {
            // Initialize with default properties
            notificationProps.setProperty("notification.retention.days", "30");
            notificationProps.setProperty("notification.max.unread", "100");
            notificationProps.setProperty("notification.enabled", "true");
            saveProperties();
        }
    }
    
    /**
     * Save notification properties to file.
     */
    private void saveProperties() {
        try (FileOutputStream fos = new FileOutputStream(NOTIFICATION_PROPS_FILE)) {
            notificationProps.store(fos, "Notification Configuration");
        } catch (IOException e) {
            System.err.println("Error saving notification properties: " + e.getMessage());
        }
    }
    
    /**
     * Load notifications from file.
     */
    @SuppressWarnings("unchecked")
    private void loadNotifications() {
        File file = new File(NOTIFICATIONS_FILE);
        if (file.exists()) {
            try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
                notifications.clear();
                readStatus.clear();
                
                List<Notification> loadedNotifications = (List<Notification>) ois.readObject();
                Map<UUID, Boolean> loadedReadStatus = (Map<UUID, Boolean>) ois.readObject();
                
                notifications.addAll(loadedNotifications);
                readStatus.putAll(loadedReadStatus);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Error loading notifications: " + e.getMessage());
                // If there's an error, start with empty collections (already cleared above)
            }
        }
    }
    
    /**
     * Save notifications to file.
     */
    private void saveNotifications() {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(NOTIFICATIONS_FILE))) {
            oos.writeObject(notifications);
            oos.writeObject(readStatus);
        } catch (IOException e) {
            System.err.println("Error saving notifications: " + e.getMessage());
        }
    }
    
    /**
     * Add a notification.
     * 
     * @param message notification message
     * @param type notification type
     * @return notification ID
     */
    public UUID addNotification(String message, NotificationType type) {
        return addNotification(message, type, false);
    }
    
    /**
     * Add a notification with read status.
     * 
     * @param message notification message
     * @param type notification type
     * @param read whether the notification has been read
     * @return notification ID
     */
    public UUID addNotification(String message, NotificationType type, boolean read) {
        SecurityManager securityManager = SecurityManager.getInstance();
        String currentUser = securityManager.getCurrentUser();
        if (currentUser == null) {
            currentUser = System.getProperty("user.name", "testuser");
        }
        
        Notification notification = Notification.create(type, message, currentUser);
        notifications.add(notification);
        readStatus.put(notification.getId(), read);
        
        // Also add to the real notification service if available
        try {
            NotificationService realService = NotificationService.getInstance();
            realService.addNotification(notification);
        } catch (Exception e) {
            // Ignore if real service is not available
        }
        
        // Save to persistence
        saveNotifications();
        
        // Log the notification creation for audit purposes
        logNotificationAction("CREATE", notification.getId().toString(), type.toString(), message);
        
        return notification.getId();
    }
    
    /**
     * Add a notification with specific ID.
     * 
     * @param id notification ID
     * @param message notification message
     * @param type notification type
     * @param read whether the notification has been read
     */
    public void addNotificationWithId(UUID id, String message, NotificationType type, boolean read) {
        if (id == null) {
            throw new IllegalArgumentException("Notification ID cannot be null");
        }
        
        SecurityManager securityManager = SecurityManager.getInstance();
        String currentUser = securityManager.getCurrentUser();
        if (currentUser == null) {
            currentUser = System.getProperty("user.name", "testuser");
        }
        
        // We need to use reflection to set the ID since it's final in the real class
        try {
            Notification notification = Notification.create(type, message, currentUser);
            
            // Use reflection to access the private final id field
            java.lang.reflect.Field idField = Notification.class.getDeclaredField("id");
            idField.setAccessible(true);
            
            // Create a modifiers field to remove the final modifier
            java.lang.reflect.Field modifiersField = java.lang.reflect.Field.class.getDeclaredField("modifiers");
            modifiersField.setAccessible(true);
            modifiersField.setInt(idField, idField.getModifiers() & ~java.lang.reflect.Modifier.FINAL);
            
            // Set the ID
            idField.set(notification, id);
            
            // Add the notification
            notifications.add(notification);
            readStatus.put(id, read);
            
            // Save to persistence
            saveNotifications();
            
            // Log the notification creation for audit purposes
            logNotificationAction("CREATE_WITH_ID", id.toString(), type.toString(), message);
        } catch (Exception e) {
            throw new RuntimeException("Failed to create notification with specific ID", e);
        }
    }
    
    /**
     * Get all notifications.
     * 
     * @return list of all notifications
     */
    public List<Notification> getAllNotifications() {
        return new ArrayList<>(notifications);
    }
    
    /**
     * Get notifications for the current user.
     * 
     * @return list of notifications for the current user
     */
    public List<Notification> getCurrentUserNotifications() {
        SecurityManager securityManager = SecurityManager.getInstance();
        String currentUser = securityManager.getCurrentUser();
        if (currentUser == null) {
            return Collections.emptyList();
        }
        
        return notifications.stream()
                .filter(n -> currentUser.equals(n.getTargetUser()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get unread notifications.
     * 
     * @return list of unread notifications
     */
    public List<Notification> getUnreadNotifications() {
        return notifications.stream()
                .filter(n -> !readStatus.getOrDefault(n.getId(), false))
                .collect(Collectors.toList());
    }
    
    /**
     * Get unread notifications for the current user.
     * 
     * @return list of unread notifications for the current user
     */
    public List<Notification> getUnreadNotificationsForCurrentUser() {
        SecurityManager securityManager = SecurityManager.getInstance();
        String currentUser = securityManager.getCurrentUser();
        if (currentUser == null) {
            return Collections.emptyList();
        }
        
        return notifications.stream()
                .filter(n -> currentUser.equals(n.getTargetUser()))
                .filter(n -> !readStatus.getOrDefault(n.getId(), false))
                .collect(Collectors.toList());
    }
    
    /**
     * Mark a notification as read.
     * 
     * @param id notification ID
     * @return true if notification exists and was marked as read
     */
    public boolean markAsRead(UUID id) {
        if (readStatus.containsKey(id) && !readStatus.get(id)) {
            readStatus.put(id, true);
            
            // Save to persistence
            saveNotifications();
            
            // Log the action for audit purposes
            logNotificationAction("MARK_READ", id.toString(), null, null);
            
            return true;
        }
        return false;
    }
    
    /**
     * Mark all notifications as read.
     * 
     * @return number of notifications marked as read
     */
    public int markAllAsRead() {
        int count = 0;
        for (UUID id : readStatus.keySet()) {
            if (!readStatus.get(id)) {
                readStatus.put(id, true);
                count++;
            }
        }
        
        if (count > 0) {
            // Save to persistence
            saveNotifications();
            
            // Log the action for audit purposes
            logNotificationAction("MARK_ALL_READ", null, null, "Marked " + count + " notifications as read");
        }
        
        return count;
    }
    
    /**
     * Mark all notifications for the current user as read.
     * 
     * @return number of notifications marked as read
     */
    public int markAllAsReadForCurrentUser() {
        SecurityManager securityManager = SecurityManager.getInstance();
        String currentUser = securityManager.getCurrentUser();
        if (currentUser == null) {
            return 0;
        }
        
        int count = 0;
        for (Notification notification : notifications) {
            if (currentUser.equals(notification.getTargetUser()) && 
                !readStatus.getOrDefault(notification.getId(), false)) {
                readStatus.put(notification.getId(), true);
                count++;
            }
        }
        
        if (count > 0) {
            // Save to persistence
            saveNotifications();
            
            // Log the action for audit purposes
            logNotificationAction("MARK_ALL_USER_READ", null, null, 
                    "Marked " + count + " notifications as read for user " + currentUser);
        }
        
        return count;
    }
    
    /**
     * Clear old notifications.
     * 
     * @return number of notifications cleared
     */
    public int clearOldNotifications() {
        int days = Integer.parseInt(notificationProps.getProperty("notification.retention.days", "30"));
        return clearOldNotifications(days);
    }
    
    /**
     * Clear notifications older than specified days.
     * 
     * @param days number of days to keep notifications for
     * @return number of notifications cleared
     */
    public int clearOldNotifications(int days) {
        if (days <= 0) {
            throw new IllegalArgumentException("Days must be a positive number");
        }
        
        Instant cutoffDate = Instant.now().minus(days, ChronoUnit.DAYS);
        
        List<Notification> toRemove = notifications.stream()
                .filter(n -> n.getTimestamp().isBefore(cutoffDate))
                .collect(Collectors.toList());
        
        int count = toRemove.size();
        
        if (count > 0) {
            notifications.removeAll(toRemove);
            
            for (Notification n : toRemove) {
                readStatus.remove(n.getId());
            }
            
            // Save to persistence
            saveNotifications();
            
            // Log the action for audit purposes
            logNotificationAction("CLEAR_OLD", null, null, 
                    "Cleared " + count + " notifications older than " + days + " days");
        }
        
        return count;
    }
    
    /**
     * Get a notification by ID.
     * 
     * @param id notification ID
     * @return notification or null if not found
     */
    public Notification getNotification(UUID id) {
        return notifications.stream()
                .filter(n -> n.getId().equals(id))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Check if a notification is read.
     * 
     * @param id notification ID
     * @return true if read, false if unread or not found
     */
    public boolean isNotificationRead(UUID id) {
        return readStatus.getOrDefault(id, false);
    }
    
    /**
     * Get the count of unread notifications.
     * 
     * @return number of unread notifications
     */
    public int getUnreadCount() {
        return (int) notifications.stream()
                .filter(n -> !readStatus.getOrDefault(n.getId(), false))
                .count();
    }
    
    /**
     * Get the count of unread notifications for the current user.
     * 
     * @return number of unread notifications for the current user
     */
    public int getUnreadCountForCurrentUser() {
        SecurityManager securityManager = SecurityManager.getInstance();
        String currentUser = securityManager.getCurrentUser();
        if (currentUser == null) {
            return 0;
        }
        
        return (int) notifications.stream()
                .filter(n -> currentUser.equals(n.getTargetUser()))
                .filter(n -> !readStatus.getOrDefault(n.getId(), false))
                .count();
    }
    
    /**
     * Get notifications by type.
     * 
     * @param type the notification type
     * @return list of notifications of the specified type
     */
    public List<Notification> getNotificationsByType(NotificationType type) {
        return notifications.stream()
                .filter(n -> n.getType() == type)
                .collect(Collectors.toList());
    }
    
    /**
     * Get recent notifications by type.
     *
     * @param type the notification type
     * @param days the number of days to include
     * @return list of notifications of the specified type within the time period
     */
    public List<Notification> getRecentNotificationsByType(NotificationType type, int days) {
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        
        return notifications.stream()
                .filter(n -> n.getType() == type)
                .filter(n -> n.getTimestamp().isAfter(cutoff))
                .collect(Collectors.toList());
    }
    
    /**
     * Add a system notification for the current user.
     *
     * @param message the notification message
     * @param priority the notification priority
     * @return the notification ID
     */
    public UUID addSystemNotification(String message, Priority priority) {
        SecurityManager securityManager = SecurityManager.getInstance();
        String currentUser = securityManager.getCurrentUser();
        if (currentUser == null) {
            currentUser = System.getProperty("user.name", "system");
        }
        
        Notification notification = new Notification(
            NotificationType.SYSTEM, 
            message, 
            "system", 
            currentUser, 
            null, 
            priority
        );
        
        notifications.add(notification);
        readStatus.put(notification.getId(), false);
        
        // Save to persistence
        saveNotifications();
        
        // Log the notification creation for audit purposes
        logNotificationAction("CREATE_SYSTEM", notification.getId().toString(), 
                "SYSTEM", message);
        
        return notification.getId();
    }
    
    /**
     * Add multiple test notifications.
     * 
     * @param count number of notifications to add
     * @param unreadCount number of notifications that should be unread
     */
    public void addTestNotifications(int count, int unreadCount) {
        SecurityManager securityManager = SecurityManager.getInstance();
        String currentUser = securityManager.getCurrentUser();
        if (currentUser == null) {
            currentUser = System.getProperty("user.name", "testuser");
        }
        
        for (int i = 0; i < count; i++) {
            boolean isRead = i >= unreadCount;
            NotificationType type = i % 2 == 0 ? NotificationType.SYSTEM : NotificationType.UPDATE;
            
            // Create notification with realistic test data
            Notification notification = new Notification(
                type,
                "Test notification " + (i+1) + " - " + 
                    (type == NotificationType.SYSTEM ? "System update available" : "Work item updated"),
                "system",
                currentUser,
                type == NotificationType.SYSTEM ? null : "WI-" + (1000 + i),
                i % 4 == 0 ? Priority.HIGH : Priority.MEDIUM
            );
            
            notifications.add(notification);
            readStatus.put(notification.getId(), isRead);
        }
        
        // Save to persistence
        saveNotifications();
        
        // Log the action for audit purposes
        logNotificationAction("ADD_TEST", null, null, 
                "Added " + count + " test notifications with " + unreadCount + " unread");
    }
    
    /**
     * Reset the mock service state.
     */
    public void reset() {
        notifications.clear();
        readStatus.clear();
        
        // Save to persistence
        saveNotifications();
        
        // Log the action for audit purposes
        logNotificationAction("RESET", null, null, "Reset notification service state");
    }
    
    /**
     * Configure notification settings.
     * 
     * @param retentionDays number of days to retain notifications
     * @param maxUnread maximum number of unread notifications to keep
     * @param enabled whether notifications are enabled
     */
    public void configureNotifications(int retentionDays, int maxUnread, boolean enabled) {
        if (retentionDays <= 0) {
            throw new IllegalArgumentException("Retention days must be positive");
        }
        
        if (maxUnread <= 0) {
            throw new IllegalArgumentException("Maximum unread must be positive");
        }
        
        notificationProps.setProperty("notification.retention.days", String.valueOf(retentionDays));
        notificationProps.setProperty("notification.max.unread", String.valueOf(maxUnread));
        notificationProps.setProperty("notification.enabled", String.valueOf(enabled));
        
        saveProperties();
        
        // Log the action for audit purposes
        logNotificationAction("CONFIGURE", null, null, 
                "Updated notification configuration: retention=" + retentionDays + 
                ", maxUnread=" + maxUnread + ", enabled=" + enabled);
    }
    
    /**
     * Log a notification action for audit purposes.
     * 
     * @param action the action performed
     * @param notificationId the notification ID (if applicable)
     * @param notificationType the notification type (if applicable)
     * @param details additional details about the action
     */
    private void logNotificationAction(String action, String notificationId, 
                                     String notificationType, String details) {
        try {
            // Create a log file if it doesn't exist
            File logDir = new File(DATA_DIR);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            File logFile = new File(DATA_DIR, "notification-audit.log");
            if (!logFile.exists()) {
                logFile.createNewFile();
            }
            
            // Get the current user and timestamp
            SecurityManager securityManager = SecurityManager.getInstance();
            String currentUser = securityManager.getCurrentUser();
            if (currentUser == null) {
                currentUser = System.getProperty("user.name", "system");
            }
            
            String timestamp = Instant.now().toString();
            
            // Format the log entry
            StringBuilder logEntry = new StringBuilder();
            logEntry.append(timestamp).append(" | ");
            logEntry.append(action).append(" | ");
            logEntry.append(currentUser).append(" | ");
            logEntry.append(notificationId != null ? notificationId : "N/A").append(" | ");
            logEntry.append(notificationType != null ? notificationType : "N/A").append(" | ");
            logEntry.append(details != null ? details : "");
            logEntry.append(System.lineSeparator());
            
            // Append to the log file
            Files.write(logFile.toPath(), logEntry.toString().getBytes(),
                    java.nio.file.StandardOpenOption.APPEND);
        } catch (IOException e) {
            // Silently fail for logging - don't disrupt normal operation
            System.err.println("Error logging notification action: " + e.getMessage());
        }
    }
}
