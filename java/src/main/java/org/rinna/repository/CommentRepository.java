/*
 * Repository interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.Comment;
import org.rinna.domain.model.CommentType;

/**
 * Repository interface for managing work item comments.
 */
public interface CommentRepository {
    /**
     * Saves a comment to the repository.
     *
     * @param comment the comment to save
     * @return the saved comment
     */
    Comment save(Comment comment);
    
    /**
     * Finds a comment by its ID.
     *
     * @param id the comment ID
     * @return an Optional containing the comment, or empty if not found
     */
    Optional<Comment> findById(UUID id);
    
    /**
     * Finds all comments for a specific work item.
     *
     * @param workItemId the work item ID
     * @return the list of comments for the work item
     */
    List<Comment> findByWorkItemId(UUID workItemId);
    
    /**
     * Finds comments for a work item of a specific type.
     *
     * @param workItemId the work item ID
     * @param type the comment type
     * @return the list of comments matching the criteria
     */
    List<Comment> findByWorkItemIdAndType(UUID workItemId, CommentType type);
    
    /**
     * Finds comments for a work item within a specific time range.
     *
     * @param workItemId the work item ID
     * @param from the start timestamp (inclusive)
     * @param to the end timestamp (inclusive)
     * @return the list of comments within the time range
     */
    List<Comment> findByWorkItemIdAndTimeRange(UUID workItemId, Instant from, Instant to);
    
    /**
     * Finds comments by author.
     *
     * @param author the author name
     * @return the list of comments by the author
     */
    List<Comment> findByAuthor(String author);
    
    /**
     * Deletes a comment by its ID.
     *
     * @param id the comment ID
     * @return true if the comment was deleted, false otherwise
     */
    boolean deleteById(UUID id);
    
    /**
     * Deletes all comments for a specific work item.
     *
     * @param workItemId the work item ID
     * @return the number of comments deleted
     */
    int deleteByWorkItemId(UUID workItemId);
}
