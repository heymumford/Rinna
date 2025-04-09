/*
 * Domain entity interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Interface representing the history of a work item.
 * The history is a collection of events and comments that have occurred
 * for a specific work item, sorted by timestamp.
 */
public interface WorkItemHistory {
    /**
     * Returns the ID of the work item this history is for.
     *
     * @return the work item's UUID
     */
    UUID workItemId();
    
    /**
     * Returns all history entries for the work item, sorted by timestamp.
     *
     * @return the list of history entries
     */
    List<HistoryEntry> entries();
    
    /**
     * Returns history entries filtered by type.
     *
     * @param type the type of entries to filter by
     * @return the filtered list of history entries
     */
    List<HistoryEntry> entriesByType(HistoryEntryType type);
    
    /**
     * Returns history entries within a specific time range.
     *
     * @param from the start timestamp (inclusive)
     * @param to the end timestamp (inclusive)
     * @return the filtered list of history entries
     */
    List<HistoryEntry> entriesInTimeRange(Instant from, Instant to);
    
    /**
     * Returns history entries from the last N hours.
     *
     * @param hours the number of hours to look back
     * @return the filtered list of history entries
     */
    List<HistoryEntry> entriesFromLastHours(int hours);
    
    /**
     * Returns history entries from the last N days.
     *
     * @param days the number of days to look back
     * @return the filtered list of history entries
     */
    List<HistoryEntry> entriesFromLastDays(int days);
    
    /**
     * Returns history entries from the last N weeks.
     *
     * @param weeks the number of weeks to look back
     * @return the filtered list of history entries
     */
    List<HistoryEntry> entriesFromLastWeeks(int weeks);
}
