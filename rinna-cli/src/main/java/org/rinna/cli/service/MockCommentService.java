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

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.rinna.cli.domain.model.Comment;
import org.rinna.cli.domain.model.CommentType;
import org.rinna.cli.domain.model.WorkflowState;

/**
 * Mock implementation of a comment service for testing.
 */
public final class MockCommentService {
    private final Map<UUID, CommentImpl> comments = new HashMap<>();
    
    /**
     * Implementation of the Comment interface.
     */
    public static class CommentImpl implements Comment {
        private final UUID id;
        private final UUID workItemId;
        private final String user;
        private final Instant timestamp;
        private final String text;
        private final CommentType type;
        
        public CommentImpl(UUID id, UUID workItemId, String user, Instant timestamp, String text, CommentType type) {
            this.id = id;
            this.workItemId = workItemId;
            this.user = user;
            this.timestamp = timestamp;
            this.text = text;
            this.type = type;
        }
        
        @Override
        public UUID id() {
            return id;
        }
        
        @Override
        public UUID workItemId() {
            return workItemId;
        }
        
        @Override
        public String user() {
            return user;
        }
        
        @Override
        public Instant timestamp() {
            return timestamp;
        }
        
        @Override
        public String text() {
            return text;
        }
        
        @Override
        public CommentType type() {
            return type;
        }
    }
    
    /**
     * Constructor initializing some sample comments.
     */
    public MockCommentService() {
        // Initialize with some sample data
        UUID itemId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String user = System.getProperty("user.name");
        
        // Create standard comment directly rather than calling overridable method
        CommentImpl comment1 = new CommentImpl(
            UUID.randomUUID(),
            itemId,
            user,
            Instant.now(),
            "Starting work on authentication feature",
            CommentType.STANDARD
        );
        comments.put(comment1.id(), comment1);
        
        // Create transition comment directly rather than calling overridable method
        CommentImpl comment2 = new CommentImpl(
            UUID.randomUUID(),
            itemId,
            user,
            Instant.now(),
            String.format("Status changed from %s to %s: %s", 
                          WorkflowState.PLANNED, 
                          WorkflowState.IN_PROGRESS, 
                          "Moving to in progress"),
            CommentType.SYSTEM
        );
        comments.put(comment2.id(), comment2);
    }
    
    /**
     * Creates a standard comment.
     *
     * @param workItemId the work item ID
     * @param user the user adding the comment
     * @param text the comment text
     * @return the created comment
     */
    public Comment createStandard(UUID workItemId, String user, String text) {
        return create(workItemId, user, text, CommentType.STANDARD);
    }
    
    /**
     * Creates a transition comment.
     *
     * @param workItemId the work item ID
     * @param user the user adding the comment
     * @param fromState the original state
     * @param toState the new state
     * @param text the comment text
     * @return the created comment
     */
    public Comment createTransition(UUID workItemId, String user, WorkflowState fromState, WorkflowState toState, String text) {
        CommentImpl comment = new CommentImpl(
            UUID.randomUUID(),
            workItemId,
            user,
            Instant.now(),
            String.format("Status changed from %s to %s: %s", fromState, toState, text),
            CommentType.SYSTEM
        );
        return comment;
    }
    
    /**
     * Creates a comment with the specified type.
     *
     * @param workItemId the work item ID
     * @param user the user adding the comment
     * @param text the comment text
     * @param type the comment type
     * @return the created comment
     */
    public Comment create(UUID workItemId, String user, String text, CommentType type) {
        CommentImpl comment = new CommentImpl(
            UUID.randomUUID(),
            workItemId,
            user,
            Instant.now(),
            text,
            type
        );
        return comment;
    }
    
    /**
     * Adds a comment to a work item.
     *
     * @param workItemId the work item ID
     * @param author the user adding the comment
     * @param text the comment text
     * @param type the comment type
     * @return the created comment
     */
    public Comment addComment(UUID workItemId, String author, String text, CommentType type) {
        CommentImpl comment = (CommentImpl)create(workItemId, author, text, type);
        comments.put(comment.id(), comment);
        return comment;
    }
    
    /**
     * Adds a standard comment to a work item.
     *
     * @param workItemId the work item ID
     * @param author the user adding the comment
     * @param text the comment text
     */
    public void addComment(UUID workItemId, String author, String text) {
        CommentImpl comment = (CommentImpl)createStandard(workItemId, author, text);
        comments.put(comment.id(), comment);
    }
    
    /**
     * Adds a transition comment to a work item.
     *
     * @param workItemId the work item ID
     * @param author the user adding the comment
     * @param fromState the original state
     * @param toState the new state
     * @param comment the comment text
     * @return the created comment
     */
    public Comment addTransitionComment(UUID workItemId, String author, WorkflowState fromState, WorkflowState toState, String comment) {
        CommentImpl transitionComment = (CommentImpl)createTransition(
            workItemId, 
            author, 
            fromState, 
            toState, 
            comment
        );
        comments.put(transitionComment.id(), transitionComment);
        return transitionComment;
    }
    
    /**
     * Adds a transition comment to a work item.
     *
     * @param workItemId the work item ID
     * @param author the user adding the comment
     * @param fromState the original state as a string
     * @param toState the new state as a string
     * @param comment the comment text
     * @return the created comment
     */
    public Comment addTransitionComment(UUID workItemId, String author, String fromState, String toState, String comment) {
        return addTransitionComment(
            workItemId, 
            author, 
            WorkflowState.valueOf(fromState), 
            WorkflowState.valueOf(toState), 
            comment
        );
    }
    
    /**
     * Gets a comment by its ID.
     *
     * @param commentId the comment ID
     * @return an Optional containing the comment, or empty if not found
     */
    public Optional<Comment> getComment(UUID commentId) {
        return Optional.ofNullable(comments.get(commentId));
    }
    
    /**
     * Gets all comments for a work item.
     *
     * @param workItemId the work item ID
     * @return the list of comments, sorted by timestamp (most recent first)
     */
    public List<CommentImpl> getComments(UUID workItemId) {
        return comments.values().stream()
            .filter(comment -> comment.workItemId().equals(workItemId))
            .sorted((c1, c2) -> c2.timestamp().compareTo(c1.timestamp())) // Most recent first
            .collect(Collectors.toList());
    }
    
    /**
     * Gets comments in a specific time range for a work item.
     *
     * @param workItemId the work item ID
     * @param from the start time
     * @param to the end time
     * @return the list of comments, sorted by timestamp (most recent first)
     */
    public List<CommentImpl> getCommentsInTimeRange(UUID workItemId, Instant from, Instant to) {
        return comments.values().stream()
            .filter(comment -> comment.workItemId().equals(workItemId) && 
                   !comment.timestamp().isBefore(from) && 
                   !comment.timestamp().isAfter(to))
            .sorted((c1, c2) -> c2.timestamp().compareTo(c1.timestamp())) // Most recent first
            .collect(Collectors.toList());
    }
    
    /**
     * Gets comments from the last X hours for a work item.
     *
     * @param workItemId the work item ID
     * @param hours the number of hours
     * @return the list of comments, sorted by timestamp (most recent first)
     */
    public List<CommentImpl> getCommentsFromLastHours(UUID workItemId, int hours) {
        Instant now = Instant.now();
        Instant from = now.minus(hours, ChronoUnit.HOURS);
        return getCommentsInTimeRange(workItemId, from, now);
    }
    
    /**
     * Gets comments from the last X days for a work item.
     *
     * @param workItemId the work item ID
     * @param days the number of days
     * @return the list of comments, sorted by timestamp (most recent first)
     */
    public List<CommentImpl> getCommentsFromLastDays(UUID workItemId, int days) {
        Instant now = Instant.now();
        Instant from = now.minus(days, ChronoUnit.DAYS);
        return getCommentsInTimeRange(workItemId, from, now);
    }
    
    /**
     * Gets comments from the last X weeks for a work item.
     *
     * @param workItemId the work item ID
     * @param weeks the number of weeks
     * @return the list of comments, sorted by timestamp (most recent first)
     */
    public List<CommentImpl> getCommentsFromLastWeeks(UUID workItemId, int weeks) {
        Instant now = Instant.now();
        Instant from = now.minus(weeks * 7, ChronoUnit.DAYS);
        return getCommentsInTimeRange(workItemId, from, now);
    }
    
    /**
     * Deletes a comment by its ID.
     *
     * @param commentId the comment ID
     * @return true if the comment was deleted, false if not found
     */
    public boolean deleteComment(UUID commentId) {
        return comments.remove(commentId) != null;
    }
}