/*
 * Repository interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.HistoryEntry;
import org.rinna.domain.model.HistoryEntryType;

/**
 * Repository interface for managing work item history entries.
 */
public interface HistoryRepository {
    /**
     * Saves a history entry to the repository.
     *
     * @param entry the history entry to save
     * @return the saved history entry
     */
    HistoryEntry save(HistoryEntry entry);
    
    /**
     * Finds a history entry by its ID.
     *
     * @param id the entry ID
     * @return an Optional containing the entry, or empty if not found
     */
    Optional<HistoryEntry> findById(UUID id);
    
    /**
     * Finds all history entries for a specific work item.
     *
     * @param workItemId the work item ID
     * @return the list of history entries for the work item
     */
    List<HistoryEntry> findByWorkItemId(UUID workItemId);
    
    /**
     * Finds history entries for a work item of a specific type.
     *
     * @param workItemId the work item ID
     * @param type the entry type
     * @return the list of history entries matching the criteria
     */
    List<HistoryEntry> findByWorkItemIdAndType(UUID workItemId, HistoryEntryType type);
    
    /**
     * Finds history entries for a work item within a specific time range.
     *
     * @param workItemId the work item ID
     * @param from the start timestamp (inclusive)
     * @param to the end timestamp (inclusive)
     * @return the list of history entries within the time range
     */
    List<HistoryEntry> findByWorkItemIdAndTimeRange(UUID workItemId, Instant from, Instant to);
    
    /**
     * Finds history entries by user.
     *
     * @param user the user name
     * @return the list of history entries by the user
     */
    List<HistoryEntry> findByUser(String user);
    
    /**
     * Deletes a history entry by its ID.
     *
     * @param id the entry ID
     * @return true if the entry was deleted, false otherwise
     */
    boolean deleteById(UUID id);
    
    /**
     * Deletes all history entries for a specific work item.
     *
     * @param workItemId the work item ID
     * @return the number of entries deleted
     */
    int deleteByWorkItemId(UUID workItemId);
}
