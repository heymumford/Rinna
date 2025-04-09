/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.domain.service;

import org.rinna.cli.domain.model.Comment;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Domain interface for managing comments on work items.
 * This interface is specific to the CLI module's domain layer and defines
 * operations for creating, retrieving, updating, and deleting comments.
 */
public interface CommentService {
    
    /**
     * Adds a comment to a work item.
     *
     * @param workItemId the work item ID
     * @param user the user making the comment
     * @param text the comment text
     * @return the added comment
     */
    Comment addComment(UUID workItemId, String user, String text);
    
    /**
     * Gets all comments for a work item.
     *
     * @param workItemId the work item ID
     * @return a list of comments
     */
    List<Comment> getComments(UUID workItemId);
    
    /**
     * Gets a specific comment.
     *
     * @param commentId the comment ID
     * @return the comment, or empty if not found
     */
    Optional<Comment> getComment(UUID commentId);
    
    /**
     * Updates a comment.
     *
     * @param commentId the comment ID
     * @param text the new comment text
     * @param user the user making the update
     * @return the updated comment, or empty if not found
     */
    Optional<Comment> updateComment(UUID commentId, String text, String user);
    
    /**
     * Deletes a comment.
     *
     * @param commentId the comment ID
     * @param user the user deleting the comment
     * @return true if successful, false otherwise
     */
    boolean deleteComment(UUID commentId, String user);
    
    /**
     * Gets the number of comments for a work item.
     *
     * @param workItemId the work item ID
     * @return the number of comments
     */
    int getCommentCount(UUID workItemId);
}