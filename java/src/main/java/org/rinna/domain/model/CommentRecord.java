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
 * Record representing a comment on a work item.
 * Comments are immutable and are attached to work items.
 */
public record CommentRecord(
    UUID id,
    UUID workItemId,
    String author,
    String text,
    Instant timestamp,
    CommentType type
) implements Comment {
    
    /**
     * Creates a new comment with the given parameters.
     *
     * @param workItemId the ID of the work item this comment is associated with
     * @param author the author of the comment
     * @param text the comment text
     * @param type the type of comment
     * @return a new comment record
     */
    public static CommentRecord create(UUID workItemId, String author, String text, CommentType type) {
        return new CommentRecord(
            UUID.randomUUID(),
            workItemId,
            author,
            text,
            Instant.now(),
            type
        );
    }
    
    /**
     * Creates a new standard comment with the given parameters.
     *
     * @param workItemId the ID of the work item this comment is associated with
     * @param author the author of the comment
     * @param text the comment text
     * @return a new comment record
     */
    public static CommentRecord createStandard(UUID workItemId, String author, String text) {
        return create(workItemId, author, text, CommentType.STANDARD);
    }
    
    /**
     * Creates a new system comment with the given parameters.
     *
     * @param workItemId the ID of the work item this comment is associated with
     * @param text the comment text
     * @return a new comment record
     */
    public static CommentRecord createSystem(UUID workItemId, String text) {
        return create(workItemId, "System", text, CommentType.SYSTEM);
    }
    
    /**
     * Creates a new transition comment with the given parameters.
     *
     * @param workItemId the ID of the work item this comment is associated with
     * @param author the author of the comment
     * @param fromState the state the work item is transitioning from
     * @param toState the state the work item is transitioning to
     * @param text the comment text (optional)
     * @return a new comment record
     */
    public static CommentRecord createTransition(
        UUID workItemId, 
        String author, 
        WorkflowState fromState, 
        WorkflowState toState, 
        String text
    ) {
        String transitionText = String.format(
            "Changed status from %s to %s", 
            fromState.name(), 
            toState.name()
        );
        
        if (text != null && !text.isEmpty()) {
            transitionText += ": " + text;
        }
        
        return create(workItemId, author, transitionText, CommentType.TRANSITION);
    }
}
