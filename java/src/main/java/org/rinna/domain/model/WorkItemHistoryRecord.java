/*
 * Domain entity record for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Record representing the history of a work item.
 * The history is a collection of events and comments that have occurred
 * for a specific work item, sorted by timestamp.
 */
public record WorkItemHistoryRecord(
    UUID workItemId,
    List<HistoryEntry> entries
) implements WorkItemHistory {
    
    @Override
    public List<HistoryEntry> entriesByType(HistoryEntryType type) {
        return entries.stream()
            .filter(entry -> entry.type() == type)
            .sorted((e1, e2) -> e2.timestamp().compareTo(e1.timestamp())) // Most recent first
            .collect(Collectors.toList());
    }
    
    @Override
    public List<HistoryEntry> entriesInTimeRange(Instant from, Instant to) {
        return entries.stream()
            .filter(entry -> !entry.timestamp().isBefore(from) && !entry.timestamp().isAfter(to))
            .sorted((e1, e2) -> e2.timestamp().compareTo(e1.timestamp())) // Most recent first
            .collect(Collectors.toList());
    }
    
    @Override
    public List<HistoryEntry> entriesFromLastHours(int hours) {
        Instant now = Instant.now();
        Instant from = now.minus(hours, ChronoUnit.HOURS);
        return entriesInTimeRange(from, now);
    }
    
    @Override
    public List<HistoryEntry> entriesFromLastDays(int days) {
        Instant now = Instant.now();
        Instant from = now.minus(days, ChronoUnit.DAYS);
        return entriesInTimeRange(from, now);
    }
    
    @Override
    public List<HistoryEntry> entriesFromLastWeeks(int weeks) {
        Instant now = Instant.now();
        Instant from = now.minus(weeks * 7, ChronoUnit.DAYS);
        return entriesInTimeRange(from, now);
    }
    
    /**
     * Creates a WorkItemHistoryRecord from a list of history entries.
     *
     * @param workItemId the ID of the work item
     * @param entries the list of history entries
     * @return a new WorkItemHistoryRecord
     */
    public static WorkItemHistoryRecord create(UUID workItemId, List<HistoryEntry> entries) {
        // Sort entries by timestamp (most recent first)
        List<HistoryEntry> sortedEntries = entries.stream()
            .sorted((e1, e2) -> e2.timestamp().compareTo(e1.timestamp()))
            .collect(Collectors.toList());
        
        return new WorkItemHistoryRecord(workItemId, sortedEntries);
    }
}
