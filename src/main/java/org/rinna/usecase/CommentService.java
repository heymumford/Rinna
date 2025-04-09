/*
 * Service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.usecase;

import org.rinna.domain.model.Comment;
import org.rinna.domain.model.CommentType;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing work item comments.
 */
public interface CommentService {
    /**
     * Adds a comment to a work item.
     *
     * @param workItemId the ID of the work item
     * @param author the author of the comment
     * @param text the comment text
     * @param type the type of comment (optional, defaults to STANDARD)
     * @return the created comment
     */
    Comment addComment(UUID workItemId, String author, String text, CommentType type);
    
    /**
     * Adds a standard comment to a work item.
     *
     * @param workItemId the ID of the work item
     * @param author the author of the comment
     * @param text the comment text
     * @return the created comment
     */
    Comment addComment(UUID workItemId, String author, String text);
    
    /**
     * Adds a transition comment when a work item changes state.
     *
     * @param workItemId the ID of the work item
     * @param author the author of the comment
     * @param fromState the previous state
     * @param toState the new state
     * @param comment an optional comment explaining the transition
     * @return the created comment
     */
    Comment addTransitionComment(UUID workItemId, String author, String fromState, String toState, String comment);
    
    /**
     * Gets a comment by its ID.
     *
     * @param commentId the comment ID
     * @return an Optional containing the comment, or empty if not found
     */
    Optional<Comment> getComment(UUID commentId);
    
    /**
     * Gets all comments for a work item.
     *
     * @param workItemId the work item ID
     * @return the list of comments for the work item
     */
    List<Comment> getComments(UUID workItemId);
    
    /**
     * Gets all comments for a work item within a specific time range.
     *
     * @param workItemId the work item ID
     * @param from the start timestamp (inclusive)
     * @param to the end timestamp (inclusive)
     * @return the list of comments within the time range
     */
    List<Comment> getCommentsInTimeRange(UUID workItemId, Instant from, Instant to);
    
    /**
     * Gets comments for a work item from the last N hours.
     *
     * @param workItemId the work item ID
     * @param hours the number of hours to look back
     * @return the list of comments from the specified time period
     */
    List<Comment> getCommentsFromLastHours(UUID workItemId, int hours);
    
    /**
     * Gets comments for a work item from the last N days.
     *
     * @param workItemId the work item ID
     * @param days the number of days to look back
     * @return the list of comments from the specified time period
     */
    List<Comment> getCommentsFromLastDays(UUID workItemId, int days);
    
    /**
     * Gets comments for a work item from the last N weeks.
     *
     * @param workItemId the work item ID
     * @param weeks the number of weeks to look back
     * @return the list of comments from the specified time period
     */
    List<Comment> getCommentsFromLastWeeks(UUID workItemId, int weeks);
    
    /**
     * Deletes a comment.
     *
     * @param commentId the comment ID
     * @return true if the comment was deleted, false otherwise
     */
    boolean deleteComment(UUID commentId);
}
