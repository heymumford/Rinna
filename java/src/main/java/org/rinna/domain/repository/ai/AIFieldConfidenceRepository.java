/*
 * Repository interface for AI field confidence tracking
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.repository.ai;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.ai.AIFieldConfidence;
import org.rinna.domain.model.ai.AIUserFeedback;

/**
 * Repository interface for AI field confidence tracking.
 */
public interface AIFieldConfidenceRepository {

    /**
     * Saves field confidence data.
     *
     * @param fieldConfidence The field confidence to save
     * @return The saved field confidence
     */
    AIFieldConfidence save(AIFieldConfidence fieldConfidence);
    
    /**
     * Finds field confidence by ID.
     *
     * @param id The field confidence ID
     * @return The field confidence, if found
     */
    Optional<AIFieldConfidence> findById(UUID id);
    
    /**
     * Finds field confidence by field name and model ID.
     *
     * @param fieldName The field name
     * @param modelId The model ID
     * @return The field confidence, if found
     */
    Optional<AIFieldConfidence> findByFieldNameAndModelId(String fieldName, String modelId);
    
    /**
     * Finds field confidence records by model ID.
     *
     * @param modelId The model ID
     * @return The field confidence records for the model
     */
    List<AIFieldConfidence> findByModelId(String modelId);
    
    /**
     * Finds field confidence records by field name.
     *
     * @param fieldName The field name
     * @return The field confidence records for the field
     */
    List<AIFieldConfidence> findByFieldName(String fieldName);
    
    /**
     * Finds all field confidence records.
     *
     * @return All field confidence records
     */
    List<AIFieldConfidence> findAll();
    
    /**
     * Finds field confidence records by minimum acceptance rate.
     *
     * @param minAcceptanceRate The minimum acceptance rate
     * @return The field confidence records with an acceptance rate >= the minimum
     */
    List<AIFieldConfidence> findByMinimumAcceptanceRate(double minAcceptanceRate);
    
    /**
     * Updates field confidence based on user feedback.
     *
     * @param fieldName The field name
     * @param modelId The model ID
     * @param feedbackType The feedback type
     * @return The updated field confidence, if found
     */
    Optional<AIFieldConfidence> updateWithFeedback(
            String fieldName, String modelId, AIUserFeedback.FeedbackType feedbackType);
    
    /**
     * Deletes field confidence.
     *
     * @param id The field confidence ID
     */
    void delete(UUID id);
    
    /**
     * Clears all field confidence data from the repository.
     * Primarily used for testing and administration.
     */
    void clear();
}