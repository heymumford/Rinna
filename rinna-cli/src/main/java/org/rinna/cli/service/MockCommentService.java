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

import org.rinna.domain.model.Comment;
import org.rinna.domain.model.CommentRecord;
import org.rinna.domain.model.CommentType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.domain.service.CommentService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Mock implementation of the CommentService interface for testing.
 */
public class MockCommentService implements CommentService {
    private final Map<UUID, Comment> comments = new HashMap<>();
    
    /**
     * Constructor initializing some sample comments.
     */
    public MockCommentService() {
        // Initialize with some sample data
        UUID itemId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
        String user = System.getProperty("user.name");
        
        Comment comment1 = CommentRecord.createStandard(
            itemId, 
            user, 
            "Starting work on authentication feature"
        );
        comments.put(comment1.id(), comment1);
        
        Comment comment2 = CommentRecord.createTransition(
            itemId, 
            user, 
            WorkflowState.TO_DO, 
            WorkflowState.IN_PROGRESS, 
            "Moving to in progress"
        );
        comments.put(comment2.id(), comment2);
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
        Comment comment = CommentRecord.create(workItemId, author, text, type);
        comments.put(comment.id(), comment);
        return comment;
    }
    
    /**
     * Adds a standard comment to a work item.
     *
     * @param workItemId the work item ID
     * @param author the user adding the comment
     * @param text the comment text
     * @return the created comment
     */
    @Override
    public Comment addComment(UUID workItemId, String author, String text) {
        return addComment(workItemId, author, text, CommentType.STANDARD);
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
        Comment transitionComment = CommentRecord.createTransition(
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
    @Override
    public List<Comment> getComments(UUID workItemId) {
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
    public List<Comment> getCommentsInTimeRange(UUID workItemId, Instant from, Instant to) {
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
    @Override
    public List<Comment> getCommentsFromLastHours(UUID workItemId, int hours) {
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
    @Override
    public List<Comment> getCommentsFromLastDays(UUID workItemId, int days) {
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
    public List<Comment> getCommentsFromLastWeeks(UUID workItemId, int weeks) {
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