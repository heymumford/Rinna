/*
 * Service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.usecase;

import org.rinna.domain.model.HistoryEntry;
import org.rinna.domain.model.HistoryEntryType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing work item history.
 */
public interface HistoryService {
    /**
     * Records a history entry for a work item.
     *
     * @param workItemId the ID of the work item
     * @param type the type of history entry
     * @param user the user who created the entry
     * @param content the entry content
     * @param additionalData additional data for the entry (optional)
     * @return the created history entry
     */
    HistoryEntry recordHistoryEntry(UUID workItemId, HistoryEntryType type, String user, String content, String additionalData);
    
    /**
     * Records a state change for a work item.
     *
     * @param workItemId the ID of the work item
     * @param user the user who made the change
     * @param oldState the previous state
     * @param newState the new state
     * @param comment an optional comment about the change
     * @return the created history entry
     */
    HistoryEntry recordStateChange(UUID workItemId, String user, String oldState, String newState, String comment);
    
    /**
     * Records a field change for a work item.
     *
     * @param workItemId the ID of the work item
     * @param user the user who made the change
     * @param field the field that was changed
     * @param oldValue the previous value
     * @param newValue the new value
     * @return the created history entry
     */
    HistoryEntry recordFieldChange(UUID workItemId, String user, String field, String oldValue, String newValue);
    
    /**
     * Gets all history entries for a work item.
     *
     * @param workItemId the work item ID
     * @return the list of history entries for the work item
     */
    List<HistoryEntry> getHistory(UUID workItemId);
    
    /**
     * Gets history entries of a specific type for a work item.
     *
     * @param workItemId the work item ID
     * @param type the entry type
     * @return the list of history entries of the specified type
     */
    List<HistoryEntry> getHistoryByType(UUID workItemId, HistoryEntryType type);
    
    /**
     * Gets history entries for a work item within a specific time range.
     *
     * @param workItemId the work item ID
     * @param from the start timestamp (inclusive)
     * @param to the end timestamp (inclusive)
     * @return the list of history entries within the time range
     */
    List<HistoryEntry> getHistoryInTimeRange(UUID workItemId, Instant from, Instant to);
    
    /**
     * Gets history entries for a work item from the last N hours.
     *
     * @param workItemId the work item ID
     * @param hours the number of hours to look back
     * @return the list of history entries from the specified time period
     */
    List<HistoryEntry> getHistoryFromLastHours(UUID workItemId, int hours);
    
    /**
     * Gets history entries for a work item from the last N days.
     *
     * @param workItemId the work item ID
     * @param days the number of days to look back
     * @return the list of history entries from the specified time period
     */
    List<HistoryEntry> getHistoryFromLastDays(UUID workItemId, int days);
    
    /**
     * Gets history entries for a work item from the last N weeks.
     *
     * @param workItemId the work item ID
     * @param weeks the number of weeks to look back
     * @return the list of history entries from the specified time period
     */
    List<HistoryEntry> getHistoryFromLastWeeks(UUID workItemId, int weeks);
}
