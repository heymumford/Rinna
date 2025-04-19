/*
 * Notification entity for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.notifications;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Represents a notification in the Rinna system.
 */
public class Notification {
    private final UUID id;
    private final NotificationType type;
    private final String message;
    private final String source;
    private final String targetUser;
    private final Instant timestamp;
    private final String itemId;
    private final Priority priority;
    private boolean read;
    
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Notification priority levels.
     */
    public enum Priority {
        LOW, MEDIUM, HIGH, URGENT
    }
    
    /**
     * Creates a new notification.
     *
     * @param type the notification type
     * @param message the notification message
     * @param source the source of the notification (user or system)
     * @param targetUser the user who should receive the notification
     * @param itemId the related work item ID, if any
     * @param priority the priority of the notification
     */
    public Notification(NotificationType type, String message, String source, 
                       String targetUser, String itemId, Priority priority) {
        this.id = UUID.randomUUID();
        this.type = type;
        this.message = message;
        this.source = source;
        this.targetUser = targetUser;
        this.timestamp = Instant.now();
        this.itemId = itemId;
        this.priority = priority;
        this.read = false;
    }
    
    /**
     * Creates a simple notification with default values.
     *
     * @param type the notification type
     * @param message the notification message
     * @param targetUser the user who should receive the notification
     * @return a new notification
     */
    public static Notification create(NotificationType type, String message, String targetUser) {
        return new Notification(type, message, "system", targetUser, null, Priority.MEDIUM);
    }
    
    /**
     * Creates a work item notification.
     *
     * @param type the notification type
     * @param message the notification message
     * @param source the source of the notification (user or system)
     * @param targetUser the user who should receive the notification
     * @param itemId the related work item ID
     * @param priority the priority of the notification
     * @return a new notification
     */
    public static Notification createWorkItemNotification(
            NotificationType type, String message, String source, 
            String targetUser, String itemId, Priority priority) {
        return new Notification(type, message, source, targetUser, itemId, priority);
    }
    
    public UUID getId() {
        return id;
    }
    
    public NotificationType getType() {
        return type;
    }
    
    public String getMessage() {
        return message;
    }
    
    public String getSource() {
        return source;
    }
    
    public String getTargetUser() {
        return targetUser;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public Priority getPriority() {
        return priority;
    }
    
    public boolean isRead() {
        return read;
    }
    
    public void markAsRead() {
        this.read = true;
    }
    
    public String getFormattedTimestamp() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        return dateTime.format(FORMATTER);
    }
    
    /**
     * Returns a formatted string representation of this notification.
     *
     * @return a formatted string
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        sb.append(getFormattedTimestamp())
          .append(" [").append(type).append("]");
        
        if (priority == Priority.HIGH || priority == Priority.URGENT) {
            sb.append(" [").append(priority).append("]");
        }
        
        sb.append(" ").append(message);
        
        if (itemId != null) {
            sb.append(" (Item: ").append(itemId).append(")");
        }
        
        if (!read) {
            sb.append(" [UNREAD]");
        }
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return format();
    }
}