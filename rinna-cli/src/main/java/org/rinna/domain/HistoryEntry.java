/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.domain;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a history entry for a work item.
 */
public interface HistoryEntry {
    
    /**
     * Gets the ID of the history entry.
     *
     * @return the history entry ID
     */
    UUID id();
    
    /**
     * Gets the ID of the work item this history entry belongs to.
     *
     * @return the work item ID
     */
    UUID workItemId();
    
    /**
     * Gets the type of the history entry.
     *
     * @return the history entry type
     */
    HistoryEntryType type();
    
    /**
     * Gets the user who made the change.
     *
     * @return the user
     */
    String user();
    
    /**
     * Gets the timestamp when the change was made.
     *
     * @return the timestamp
     */
    Instant timestamp();
    
    /**
     * Gets the metadata associated with the history entry.
     * 
     * This may include:
     * - For STATE_CHANGE: previousState, newState
     * - For FIELD_CHANGE: field, previousValue, newValue
     * - For ASSIGNMENT_CHANGE: previousAssignee, newAssignee
     * - For PRIORITY_CHANGE: previousPriority, newPriority
     *
     * @return the metadata
     */
    Map<String, Object> metadata();
}