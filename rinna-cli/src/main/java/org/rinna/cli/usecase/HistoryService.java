/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.usecase;

import java.util.List;
import java.util.UUID;

import org.rinna.cli.domain.model.HistoryEntry;
import org.rinna.cli.domain.model.HistoryEntryType;

/**
 * Service interface for managing history entries.
 * This is a CLI-specific version of the service interface.
 */
public interface HistoryService {
    
    /**
     * Records a state change in the history.
     *
     * @param workItemId the work item ID
     * @param user the user who made the change
     * @param previousState the previous state
     * @param newState the new state
     */
    void recordStateChange(UUID workItemId, String user, String previousState, String newState);
    
    /**
     * Records a state change in the history with a comment.
     *
     * @param workItemId the work item ID
     * @param user the user who made the change
     * @param previousState the previous state
     * @param newState the new state
     * @param comment the comment
     */
    void recordStateChange(UUID workItemId, String user, String previousState, String newState, String comment);
    
    /**
     * Records a field change in the history.
     *
     * @param workItemId the work item ID
     * @param user the user who made the change
     * @param field the field that was changed
     * @param previousValue the previous value
     * @param newValue the new value
     */
    void recordFieldChange(UUID workItemId, String user, String field, String previousValue, String newValue);
    
    /**
     * Records an assignment change in the history.
     *
     * @param workItemId the work item ID
     * @param user the user who made the change
     * @param previousAssignee the previous assignee
     * @param newAssignee the new assignee
     */
    void recordAssignmentChange(UUID workItemId, String user, String previousAssignee, String newAssignee);
    
    /**
     * Records a priority change in the history.
     *
     * @param workItemId the work item ID
     * @param user the user who made the change
     * @param previousPriority the previous priority
     * @param newPriority the new priority
     */
    void recordPriorityChange(UUID workItemId, String user, String previousPriority, String newPriority);
    
    /**
     * Records a comment in the history.
     *
     * @param workItemId the work item ID
     * @param user the user who made the comment
     * @param comment the comment text
     */
    void recordComment(UUID workItemId, String user, String comment);
    
    /**
     * Records a relationship change in the history.
     *
     * @param workItemId the work item ID
     * @param user the user who made the change
     * @param changeType the type of change (added, removed)
     * @param relatedItemId the related item ID
     */
    void recordRelationshipChange(UUID workItemId, String user, String changeType, UUID relatedItemId);
    
    /**
     * Gets the full history for a work item.
     *
     * @param workItemId the work item ID
     * @return the list of history entries, sorted by timestamp (most recent first)
     */
    List<HistoryEntry> getHistory(UUID workItemId);
    
    /**
     * Gets history entries of a specific type for a work item.
     *
     * @param workItemId the work item ID
     * @param type the history entry type
     * @return the list of history entries, sorted by timestamp (most recent first)
     */
    List<HistoryEntry> getHistoryByType(UUID workItemId, HistoryEntryType type);
    
    /**
     * Gets history entries from the last X hours for a work item.
     *
     * @param workItemId the work item ID
     * @param hours the number of hours
     * @return the list of history entries, sorted by timestamp (most recent first)
     */
    List<HistoryEntry> getHistoryFromLastHours(UUID workItemId, int hours);
    
    /**
     * Gets history entries from the last X days for a work item.
     *
     * @param workItemId the work item ID
     * @param days the number of days
     * @return the list of history entries, sorted by timestamp (most recent first)
     */
    List<HistoryEntry> getHistoryFromLastDays(UUID workItemId, int days);
}