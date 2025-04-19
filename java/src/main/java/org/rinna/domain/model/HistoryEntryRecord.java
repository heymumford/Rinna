/*
 * Domain entity record for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Record representing a history entry for a work item.
 * History entries are immutable and track changes to work items.
 */
public record HistoryEntryRecord(
    UUID id,
    UUID workItemId,
    HistoryEntryType type,
    String user,
    Instant timestamp,
    String content,
    String additionalData
) implements HistoryEntry {
    
    /**
     * Creates a new history entry with the given parameters.
     *
     * @param workItemId the ID of the work item this entry is associated with
     * @param type the type of history entry
     * @param user the user who created this entry
     * @param content the entry content or description
     * @param additionalData additional data for the entry (optional)
     * @return a new history entry record
     */
    public static HistoryEntryRecord create(
        UUID workItemId,
        HistoryEntryType type,
        String user,
        String content,
        String additionalData
    ) {
        return new HistoryEntryRecord(
            UUID.randomUUID(),
            workItemId,
            type,
            user,
            Instant.now(),
            content,
            additionalData
        );
    }
    
    /**
     * Creates a new state change history entry.
     *
     * @param workItemId the ID of the work item
     * @param user the user who made the change
     * @param oldState the previous state
     * @param newState the new state
     * @param comment an optional comment about the change
     * @return a new history entry record
     */
    public static HistoryEntryRecord createStateChange(
        UUID workItemId,
        String user,
        WorkflowState oldState,
        WorkflowState newState,
        String comment
    ) {
        String content = String.format("State changed from %s to %s", oldState, newState);
        String additionalData = String.format("{\"oldState\":\"%s\",\"newState\":\"%s\"}", oldState, newState);
        
        if (comment != null && !comment.isBlank()) {
            content = content + ": " + comment;
        }
        
        return create(workItemId, HistoryEntryType.STATE_CHANGE, user, content, additionalData);
    }
    
    /**
     * Creates a new comment history entry.
     *
     * @param comment the comment that was added
     * @return a new history entry record
     */
    public static HistoryEntryRecord fromComment(Comment comment) {
        return create(
            comment.workItemId(),
            HistoryEntryType.COMMENT,
            comment.author(),
            comment.text(),
            String.format("{\"commentId\":\"%s\",\"commentType\":\"%s\"}", comment.id(), comment.type())
        );
    }
    
    /**
     * Creates a new field change history entry.
     *
     * @param workItemId the ID of the work item
     * @param user the user who made the change
     * @param field the field that was changed
     * @param oldValue the previous value
     * @param newValue the new value
     * @return a new history entry record
     */
    public static HistoryEntryRecord createFieldChange(
        UUID workItemId,
        String user,
        String field,
        String oldValue,
        String newValue
    ) {
        String content = String.format("%s changed from '%s' to '%s'", field, oldValue, newValue);
        String additionalData = String.format("{\"field\":\"%s\",\"oldValue\":\"%s\",\"newValue\":\"%s\"}",
            field, oldValue, newValue);
        
        return create(workItemId, HistoryEntryType.FIELD_CHANGE, user, content, additionalData);
    }
}
