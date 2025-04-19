/*
 * Notification service for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.notifications;

import java.io.*;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.rinna.cli.security.SecurityManager;

/**
 * Service for managing and delivering notifications to users.
 */
public final class NotificationService {
    private static NotificationService instance;
    
    private static final String NOTIFICATIONS_DIR = System.getProperty("user.home") + "/.rinna/notifications";
    private static final int MAX_UNREAD_TO_DISPLAY = 5;
    
    private final Map<String, List<Notification>> userNotifications = new ConcurrentHashMap<>();
    private boolean initialized = false;
    
    /**
     * Gets the singleton instance of the notification service.
     *
     * @return the singleton instance
     */
    public static synchronized NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }
    
    private NotificationService() {
        // Private constructor to enforce singleton pattern
    }
    
    /**
     * Initialize the notification service.
     */
    public void initialize() {
        if (initialized) {
            return;
        }
        
        // Create notifications directory if it doesn't exist
        File notificationsDir = new File(NOTIFICATIONS_DIR);
        if (!notificationsDir.exists()) {
            notificationsDir.mkdirs();
        }
        
        // Load notifications for the current user
        loadNotificationsForCurrentUser();
        
        initialized = true;
    }
    
    /**
     * Adds a new notification.
     *
     * @param notification the notification to add
     */
    public void addNotification(Notification notification) {
        initialize();
        
        String targetUser = notification.getTargetUser();
        userNotifications.computeIfAbsent(targetUser, k -> new ArrayList<>()).add(notification);
        
        // Save the notification
        saveNotifications(targetUser);
    }
    
    /**
     * Gets all notifications for the current user.
     *
     * @return the list of notifications
     */
    public List<Notification> getNotificationsForCurrentUser() {
        initialize();
        
        SecurityManager securityManager = SecurityManager.getInstance();
        String currentUser = securityManager.getCurrentUser();
        
        if (currentUser == null) {
            return Collections.emptyList();
        }
        
        return userNotifications.getOrDefault(currentUser, Collections.emptyList());
    }
    
    /**
     * Gets unread notifications for the current user.
     *
     * @return the list of unread notifications
     */
    public List<Notification> getUnreadNotificationsForCurrentUser() {
        return getNotificationsForCurrentUser().stream()
            .filter(n -> !n.isRead())
            .collect(Collectors.toList());
    }
    
    /**
     * Gets the count of unread notifications for the current user.
     *
     * @return the count of unread notifications
     */
    public int getUnreadCount() {
        return getUnreadNotificationsForCurrentUser().size();
    }
    
    /**
     * Marks a notification as read.
     *
     * @param notificationId the ID of the notification to mark as read
     * @return true if the notification was found and marked as read
     */
    public boolean markAsRead(UUID notificationId) {
        initialize();
        
        SecurityManager securityManager = SecurityManager.getInstance();
        String currentUser = securityManager.getCurrentUser();
        
        if (currentUser == null) {
            return false;
        }
        
        List<Notification> notifications = userNotifications.getOrDefault(currentUser, Collections.emptyList());
        Optional<Notification> notification = notifications.stream()
            .filter(n -> n.getId().equals(notificationId))
            .findFirst();
        
        if (notification.isPresent()) {
            notification.get().markAsRead();
            saveNotifications(currentUser);
            return true;
        }
        
        return false;
    }
    
    /**
     * Marks all notifications for the current user as read.
     */
    public void markAllAsRead() {
        initialize();
        
        SecurityManager securityManager = SecurityManager.getInstance();
        String currentUser = securityManager.getCurrentUser();
        
        if (currentUser == null) {
            return;
        }
        
        List<Notification> notifications = userNotifications.getOrDefault(currentUser, Collections.emptyList());
        notifications.forEach(Notification::markAsRead);
        saveNotifications(currentUser);
    }
    
    /**
     * Gets recent notifications filtered by type.
     *
     * @param type the notification type to filter by
     * @param days the number of days to include
     * @return the list of matching notifications
     */
    public List<Notification> getRecentNotificationsByType(NotificationType type, int days) {
        initialize();
        
        SecurityManager securityManager = SecurityManager.getInstance();
        String currentUser = securityManager.getCurrentUser();
        
        if (currentUser == null) {
            return Collections.emptyList();
        }
        
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        
        return userNotifications.getOrDefault(currentUser, Collections.emptyList()).stream()
            .filter(n -> n.getType() == type && n.getTimestamp().isAfter(cutoff))
            .collect(Collectors.toList());
    }
    
    /**
     * Deletes old notifications beyond a certain age.
     *
     * @param days the maximum age in days to keep
     * @return the number of notifications deleted
     */
    public int deleteOldNotifications(int days) {
        initialize();
        
        SecurityManager securityManager = SecurityManager.getInstance();
        String currentUser = securityManager.getCurrentUser();
        
        if (currentUser == null) {
            return 0;
        }
        
        List<Notification> notifications = userNotifications.getOrDefault(currentUser, Collections.emptyList());
        int initialSize = notifications.size();
        
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        notifications.removeIf(n -> n.getTimestamp().isBefore(cutoff));
        
        if (initialSize != notifications.size()) {
            saveNotifications(currentUser);
        }
        
        return initialSize - notifications.size();
    }
    
    /**
     * Displays unread notifications for the current user.
     */
    public void displayUnreadNotifications() {
        initialize();
        
        List<Notification> unread = getUnreadNotificationsForCurrentUser();
        if (unread.isEmpty()) {
            return;
        }
        
        System.out.println();
        System.out.println("=== Unread Notifications ===");
        
        // Display only the most recent unread notifications
        int displayCount = Math.min(unread.size(), MAX_UNREAD_TO_DISPLAY);
        for (int i = 0; i < displayCount; i++) {
            System.out.println(unread.get(i).format());
        }
        
        // If there are more notifications, show a message
        if (unread.size() > MAX_UNREAD_TO_DISPLAY) {
            System.out.println("... and " + (unread.size() - MAX_UNREAD_TO_DISPLAY) + " more notifications");
        }
        
        System.out.println("===========================");
        System.out.println();
    }
    
    /**
     * Adds a system notification for the current user.
     *
     * @param message the notification message
     * @param priority the notification priority
     */
    public void addSystemNotification(String message, Notification.Priority priority) {
        SecurityManager securityManager = SecurityManager.getInstance();
        String currentUser = securityManager.getCurrentUser();
        
        if (currentUser == null) {
            return;
        }
        
        Notification notification = new Notification(
            NotificationType.SYSTEM, 
            message, 
            "system", 
            currentUser, 
            null, 
            priority
        );
        
        addNotification(notification);
    }
    
    /**
     * Loads notifications for the current user from disk.
     */
    private void loadNotificationsForCurrentUser() {
        SecurityManager securityManager = SecurityManager.getInstance();
        String currentUser = securityManager.getCurrentUser();
        
        if (currentUser == null) {
            return;
        }
        
        File userNotificationsFile = getUserNotificationsFile(currentUser);
        if (!userNotificationsFile.exists()) {
            return;
        }
        
        try (ObjectInputStream ois = new ObjectInputStream(
                java.nio.file.Files.newInputStream(userNotificationsFile.toPath()))) {
            @SuppressWarnings("unchecked")
            List<Notification> notifications = (List<Notification>) ois.readObject();
            userNotifications.put(currentUser, notifications);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error loading notifications: " + e.getMessage());
            // If there's an error, start with an empty list
            userNotifications.put(currentUser, new ArrayList<>());
        }
    }
    
    /**
     * Saves notifications for a user to disk.
     *
     * @param username the username
     */
    private void saveNotifications(String username) {
        File userNotificationsFile = getUserNotificationsFile(username);
        
        try (ObjectOutputStream oos = new ObjectOutputStream(
                java.nio.file.Files.newOutputStream(userNotificationsFile.toPath()))) {
            oos.writeObject(userNotifications.getOrDefault(username, Collections.emptyList()));
        } catch (IOException e) {
            System.err.println("Error saving notifications: " + e.getMessage());
        }
    }
    
    /**
     * Gets the file for storing a user's notifications.
     *
     * @param username the username
     * @return the notifications file
     */
    private File getUserNotificationsFile(String username) {
        File notificationsDir = new File(NOTIFICATIONS_DIR);
        if (!notificationsDir.exists()) {
            notificationsDir.mkdirs();
        }
        return new File(notificationsDir, username + ".dat");
    }
}