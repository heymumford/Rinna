/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import org.rinna.domain.model.HistoryEntry;
import org.rinna.domain.model.HistoryEntryRecord;
import org.rinna.domain.model.HistoryEntryType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.domain.service.HistoryService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A mock implementation of the HistoryService interface for testing.
 */
public class MockHistoryService implements HistoryService {
    
    private final Map<UUID, List<HistoryEntry>> historyStore = new ConcurrentHashMap<>();
    
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
    @Override
    public HistoryEntry recordHistoryEntry(UUID workItemId, HistoryEntryType type, String user, 
                                           String content, String additionalData) {
        HistoryEntry entry = HistoryEntryRecord.create(workItemId, type, user, content, additionalData);
        addEntry(workItemId, entry);
        return entry;
    }
    
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
    @Override
    public HistoryEntry recordStateChange(UUID workItemId, String user, String oldState, 
                                          String newState, String comment) {
        WorkflowState oldWorkflowState = WorkflowState.valueOf(oldState);
        WorkflowState newWorkflowState = WorkflowState.valueOf(newState);
        
        HistoryEntry entry = HistoryEntryRecord.createStateChange(
            workItemId, user, oldWorkflowState, newWorkflowState, comment);
        
        addEntry(workItemId, entry);
        return entry;
    }
    
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
    @Override
    public HistoryEntry recordFieldChange(UUID workItemId, String user, String field, 
                                          String oldValue, String newValue) {
        HistoryEntry entry = HistoryEntryRecord.createFieldChange(
            workItemId, user, field, oldValue, newValue);
        
        addEntry(workItemId, entry);
        return entry;
    }
    
    /**
     * Gets all history entries for a work item.
     *
     * @param workItemId the work item ID
     * @return the list of history entries for the work item
     */
    @Override
    public List<HistoryEntry> getHistory(UUID workItemId) {
        List<HistoryEntry> history = historyStore.getOrDefault(workItemId, new ArrayList<>());
        return new ArrayList<>(history);
    }
    
    /**
     * Gets history entries of a specific type for a work item.
     *
     * @param workItemId the work item ID
     * @param type the entry type
     * @return the list of history entries of the specified type
     */
    @Override
    public List<HistoryEntry> getHistoryByType(UUID workItemId, HistoryEntryType type) {
        List<HistoryEntry> history = historyStore.getOrDefault(workItemId, new ArrayList<>());
        return history.stream()
                .filter(entry -> entry.type() == type)
                .collect(Collectors.toList());
    }
    
    /**
     * Gets history entries for a work item within a specific time range.
     *
     * @param workItemId the work item ID
     * @param from the start timestamp (inclusive)
     * @param to the end timestamp (inclusive)
     * @return the list of history entries within the time range
     */
    @Override
    public List<HistoryEntry> getHistoryInTimeRange(UUID workItemId, Instant from, Instant to) {
        List<HistoryEntry> history = historyStore.getOrDefault(workItemId, new ArrayList<>());
        return history.stream()
                .filter(entry -> !entry.timestamp().isBefore(from) && !entry.timestamp().isAfter(to))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets history entries for a work item from the last N hours.
     *
     * @param workItemId the work item ID
     * @param hours the number of hours to look back
     * @return the list of history entries from the specified time period
     */
    @Override
    public List<HistoryEntry> getHistoryFromLastHours(UUID workItemId, int hours) {
        Instant now = Instant.now();
        Instant from = now.minus(hours, ChronoUnit.HOURS);
        return getHistoryInTimeRange(workItemId, from, now);
    }
    
    /**
     * Gets history entries for a work item from the last N days.
     *
     * @param workItemId the work item ID
     * @param days the number of days to look back
     * @return the list of history entries from the specified time period
     */
    @Override
    public List<HistoryEntry> getHistoryFromLastDays(UUID workItemId, int days) {
        Instant now = Instant.now();
        Instant from = now.minus(days, ChronoUnit.DAYS);
        return getHistoryInTimeRange(workItemId, from, now);
    }
    
    /**
     * Gets history entries for a work item from the last N weeks.
     *
     * @param workItemId the work item ID
     * @param weeks the number of weeks to look back
     * @return the list of history entries from the specified time period
     */
    @Override
    public List<HistoryEntry> getHistoryFromLastWeeks(UUID workItemId, int weeks) {
        Instant now = Instant.now();
        Instant from = now.minus(weeks * 7, ChronoUnit.DAYS);
        return getHistoryInTimeRange(workItemId, from, now);
    }
    
    /**
     * Add a history entry to the store.
     *
     * @param workItemId the work item ID
     * @param entry the history entry
     */
    private void addEntry(UUID workItemId, HistoryEntry entry) {
        historyStore.computeIfAbsent(workItemId, k -> new ArrayList<>()).add(0, entry);
    }
    
    /**
     * Add a mock history entry for testing.
     *
     * @param entry the entry to add
     */
    public void addMockEntry(HistoryEntry entry) {
        addEntry(entry.workItemId(), entry);
    }
}