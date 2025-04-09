/*
 * Notification type enum for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.notifications;

/**
 * Represents the type of notification.
 */
public enum NotificationType {
    /**
     * A work item has been assigned to you.
     */
    ASSIGNMENT,
    
    /**
     * A work item has been updated or changed.
     */
    UPDATE,
    
    /**
     * A new comment has been added to a work item.
     */
    COMMENT,
    
    /**
     * A reminder about an upcoming deadline.
     */
    DEADLINE,
    
    /**
     * A mention of you in a comment or description.
     */
    MENTION,
    
    /**
     * A security-related notification.
     */
    SECURITY,
    
    /**
     * A system notification.
     */
    SYSTEM,
    
    /**
     * A work item has been completed.
     */
    COMPLETION,
    
    /**
     * A work item requires your attention.
     */
    ATTENTION
}