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

import org.rinna.cli.model.WorkflowState;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * A mock implementation of history service functionality for CLI use.
 */
public class MockHistoryService {
    
    /**
     * History entry type enumeration.
     */
    public enum HistoryEntryType {
        CREATION,
        STATE_CHANGE,
        FIELD_CHANGE,
        COMMENT,
        ASSIGNMENT,
        LINK
    }
    
    /**
     * History entry record.
     */
    public static class HistoryEntryRecord {
        private final UUID workItemId;
        private final HistoryEntryType type;
        private final String user;
        private final String content;
        private final String additionalData;
        private final Instant timestamp;
        
        /**
         * Creates a new history entry record.
         *
         * @param workItemId the work item ID
         * @param type the entry type
         * @param user the user who created the entry
         * @param content the entry content
         * @param additionalData additional data
         * @param timestamp the timestamp
         */
        public HistoryEntryRecord(UUID workItemId, HistoryEntryType type, String user, String content, 
                                  String additionalData, Instant timestamp) {
            this.workItemId = workItemId;
            this.type = type;
            this.user = user;
            this.content = content;
            this.additionalData = additionalData;
            this.timestamp = timestamp;
        }
        
        /**
         * Creates a standard history entry.
         *
         * @param workItemId the work item ID
         * @param type the entry type
         * @param user the user who created the entry
         * @param content the entry content
         * @param additionalData additional data
         * @return the created entry
         */
        public static HistoryEntryRecord create(UUID workItemId, HistoryEntryType type, String user, 
                                               String content, String additionalData) {
            return new HistoryEntryRecord(workItemId, type, user, content, additionalData, Instant.now());
        }
        
        /**
         * Creates a state change history entry.
         *
         * @param workItemId the work item ID
         * @param user the user who made the change
         * @param oldState the previous state
         * @param newState the new state
         * @param comment an optional comment
         * @return the created entry
         */
        public static HistoryEntryRecord createStateChange(UUID workItemId, String user, 
                                                          WorkflowState oldState, WorkflowState newState, 
                                                          String comment) {
            String content = "State changed from " + oldState + " to " + newState;
            return new HistoryEntryRecord(workItemId, HistoryEntryType.STATE_CHANGE, user, content, comment, Instant.now());
        }
        
        /**
         * Creates a field change history entry.
         *
         * @param workItemId the work item ID
         * @param user the user who made the change
         * @param field the field that was changed
         * @param oldValue the previous value
         * @param newValue the new value
         * @return the created entry
         */
        public static HistoryEntryRecord createFieldChange(UUID workItemId, String user, String field, 
                                                          String oldValue, String newValue) {
            String content = "Field '" + field + "' changed from '" + oldValue + "' to '" + newValue + "'";
            return new HistoryEntryRecord(workItemId, HistoryEntryType.FIELD_CHANGE, user, content, null, Instant.now());
        }
        
        /**
         * Gets the work item ID.
         *
         * @return the work item ID
         */
        public UUID getWorkItemId() {
            return workItemId;
        }
        
        /**
         * Gets the entry type.
         *
         * @return the entry type
         */
        public HistoryEntryType getType() {
            return type;
        }
        
        /**
         * Gets the user who created the entry.
         *
         * @return the user
         */
        public String getUser() {
            return user;
        }
        
        /**
         * Gets the entry content.
         *
         * @return the content
         */
        public String getContent() {
            return content;
        }
        
        /**
         * Gets the additional data.
         *
         * @return the additional data
         */
        public String getAdditionalData() {
            return additionalData;
        }
        
        /**
         * Gets the timestamp.
         *
         * @return the timestamp
         */
        public Instant getTimestamp() {
            return timestamp;
        }
    }
    
    private final Map<UUID, List<HistoryEntryRecord>> historyStore = new ConcurrentHashMap<>();
    
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
    public HistoryEntryRecord recordHistoryEntry(UUID workItemId, HistoryEntryType type, String user, 
                                           String content, String additionalData) {
        HistoryEntryRecord entry = HistoryEntryRecord.create(workItemId, type, user, content, additionalData);
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
    public HistoryEntryRecord recordStateChange(UUID workItemId, String user, String oldState, 
                                          String newState, String comment) {
        WorkflowState oldWorkflowState = WorkflowState.valueOf(oldState);
        WorkflowState newWorkflowState = WorkflowState.valueOf(newState);
        
        HistoryEntryRecord entry = HistoryEntryRecord.createStateChange(
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
    public HistoryEntryRecord recordFieldChange(UUID workItemId, String user, String field, 
                                          String oldValue, String newValue) {
        HistoryEntryRecord entry = HistoryEntryRecord.createFieldChange(
            workItemId, user, field, oldValue, newValue);
        
        addEntry(workItemId, entry);
        return entry;
    }
    
    /**
     * Records an assignment change for a work item.
     *
     * @param workItemId the ID of the work item
     * @param user the user who made the change
     * @param previousAssignee the previous assignee
     * @param newAssignee the new assignee
     * @return the created history entry
     */
    public HistoryEntryRecord recordAssignment(UUID workItemId, String user, 
                                        String previousAssignee, String newAssignee) {
        String content = "Assignment changed from '" + previousAssignee + "' to '" + newAssignee + "'";
        HistoryEntryRecord entry = new HistoryEntryRecord(workItemId, HistoryEntryType.ASSIGNMENT, 
                                                       user, content, null, Instant.now());
        addEntry(workItemId, entry);
        return entry;
    }
    
    /**
     * Records a priority change for a work item.
     *
     * @param workItemId the ID of the work item
     * @param user the user who made the change
     * @param previousPriority the previous priority
     * @param newPriority the new priority
     * @return the created history entry
     */
    public HistoryEntryRecord recordPriorityChange(UUID workItemId, String user, 
                                            String previousPriority, String newPriority) {
        String content = "Priority changed from '" + previousPriority + "' to '" + newPriority + "'";
        HistoryEntryRecord entry = new HistoryEntryRecord(workItemId, HistoryEntryType.FIELD_CHANGE, 
                                                       user, content, "Priority", Instant.now());
        addEntry(workItemId, entry);
        return entry;
    }
    
    /**
     * Adds a comment to a work item.
     *
     * @param workItemId the ID of the work item
     * @param user the user who made the comment
     * @param comment the comment text
     * @return the created history entry
     */
    public HistoryEntryRecord addComment(UUID workItemId, String user, String comment) {
        HistoryEntryRecord entry = new HistoryEntryRecord(workItemId, HistoryEntryType.COMMENT, 
                                                       user, comment, null, Instant.now());
        addEntry(workItemId, entry);
        return entry;
    }
    
    /**
     * Records a link change for a work item.
     *
     * @param workItemId the ID of the work item
     * @param user the user who made the change
     * @param changeType the type of change (added, removed)
     * @param relatedItemId the related item ID
     * @return the created history entry
     */
    public HistoryEntryRecord recordLink(UUID workItemId, String user, 
                                   String changeType, String relatedItemId) {
        String content = changeType + " link to item " + relatedItemId;
        HistoryEntryRecord entry = new HistoryEntryRecord(workItemId, HistoryEntryType.LINK, 
                                                       user, content, relatedItemId, Instant.now());
        addEntry(workItemId, entry);
        return entry;
    }
    
    /**
     * Gets all history entries for a work item.
     *
     * @param workItemId the work item ID
     * @return the list of history entries for the work item
     */
    public List<HistoryEntryRecord> getHistory(UUID workItemId) {
        List<HistoryEntryRecord> history = historyStore.getOrDefault(workItemId, new ArrayList<>());
        return new ArrayList<>(history);
    }
    
    /**
     * Gets history entries of a specific type for a work item.
     *
     * @param workItemId the work item ID
     * @param type the entry type
     * @return the list of history entries of the specified type
     */
    public List<HistoryEntryRecord> getHistoryByType(UUID workItemId, HistoryEntryType type) {
        List<HistoryEntryRecord> history = historyStore.getOrDefault(workItemId, new ArrayList<>());
        return history.stream()
                .filter(entry -> entry.getType() == type)
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
    public List<HistoryEntryRecord> getHistoryInTimeRange(UUID workItemId, Instant from, Instant to) {
        List<HistoryEntryRecord> history = historyStore.getOrDefault(workItemId, new ArrayList<>());
        return history.stream()
                .filter(entry -> !entry.getTimestamp().isBefore(from) && !entry.getTimestamp().isAfter(to))
                .collect(Collectors.toList());
    }
    
    /**
     * Gets history entries for a work item within a specific time range (alias for getHistoryInTimeRange).
     *
     * @param workItemId the work item ID
     * @param from the start timestamp (inclusive)
     * @param to the end timestamp (inclusive)
     * @return the list of history entries within the time range
     */
    public List<HistoryEntryRecord> getHistoryByTimeRange(UUID workItemId, Instant from, Instant to) {
        return getHistoryInTimeRange(workItemId, from, to);
    }
    
    /**
     * Gets history entries for a work item from the last N hours.
     *
     * @param workItemId the work item ID
     * @param hours the number of hours to look back
     * @return the list of history entries from the specified time period
     */
    public List<HistoryEntryRecord> getHistoryFromLastHours(UUID workItemId, int hours) {
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
    public List<HistoryEntryRecord> getHistoryFromLastDays(UUID workItemId, int days) {
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
    public List<HistoryEntryRecord> getHistoryFromLastWeeks(UUID workItemId, int weeks) {
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
    private void addEntry(UUID workItemId, HistoryEntryRecord entry) {
        historyStore.computeIfAbsent(workItemId, k -> new ArrayList<>()).add(0, entry);
    }
    
    /**
     * Add a mock history entry for testing.
     *
     * @param entry the entry to add
     */
    public void addMockEntry(HistoryEntryRecord entry) {
        addEntry(entry.getWorkItemId(), entry);
    }
}