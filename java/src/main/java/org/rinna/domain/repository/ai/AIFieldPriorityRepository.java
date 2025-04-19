/*
 * Repository interface for AI field priorities
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.repository.ai;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.ai.AIFieldPriority;

/**
 * Repository interface for AI field priorities.
 */
public interface AIFieldPriorityRepository {

    /**
     * Saves a field priority.
     *
     * @param fieldPriority The field priority to save
     * @return The saved field priority
     */
    AIFieldPriority save(AIFieldPriority fieldPriority);
    
    /**
     * Finds a field priority by ID.
     *
     * @param id The field priority ID
     * @return The field priority, if found
     */
    Optional<AIFieldPriority> findById(UUID id);
    
    /**
     * Finds a field priority by field name.
     *
     * @param fieldName The field name
     * @return The field priority, if found
     */
    Optional<AIFieldPriority> findByFieldName(String fieldName);
    
    /**
     * Finds all field priorities.
     *
     * @return All field priorities
     */
    List<AIFieldPriority> findAll();
    
    /**
     * Finds field priorities by minimum priority score.
     *
     * @param minPriorityScore The minimum priority score
     * @return The field priorities with a priority score >= the minimum
     */
    List<AIFieldPriority> findByMinimumPriorityScore(double minPriorityScore);
    
    /**
     * Finds field priorities sorted by priority score in descending order.
     *
     * @param limit The maximum number of field priorities to return
     * @return The field priorities sorted by priority score
     */
    List<AIFieldPriority> findAllSortedByPriorityScore(int limit);
    
    /**
     * Increments the usage count for a field.
     *
     * @param fieldName The field name
     * @return The updated field priority, if found
     */
    Optional<AIFieldPriority> incrementUsageCount(String fieldName);
    
    /**
     * Updates the completion rate for a field.
     *
     * @param fieldName The field name
     * @param completionRate The new completion rate
     * @return The updated field priority, if found
     */
    Optional<AIFieldPriority> updateCompletionRate(String fieldName, double completionRate);
    
    /**
     * Updates the value rating for a field.
     *
     * @param fieldName The field name
     * @param valueRating The new value rating
     * @return The updated field priority, if found
     */
    Optional<AIFieldPriority> updateValueRating(String fieldName, double valueRating);
    
    /**
     * Recalculates the priority score for a field.
     *
     * @param fieldName The field name
     * @return The updated field priority, if found
     */
    Optional<AIFieldPriority> recalculatePriorityScore(String fieldName);
    
    /**
     * Deletes a field priority.
     *
     * @param id The field priority ID
     */
    void delete(UUID id);
    
    /**
     * Clears all field priorities from the repository.
     * Primarily used for testing and administration.
     */
    void clear();
}