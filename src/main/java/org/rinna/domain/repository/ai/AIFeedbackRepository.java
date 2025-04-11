/*
 * Repository interface for AI user feedback
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.repository.ai;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.ai.AIUserFeedback;

/**
 * Repository interface for AI user feedback.
 */
public interface AIFeedbackRepository {

    /**
     * Saves user feedback.
     *
     * @param feedback The feedback to save
     * @return The saved feedback
     */
    AIUserFeedback save(AIUserFeedback feedback);
    
    /**
     * Finds feedback by ID.
     *
     * @param id The feedback ID
     * @return The feedback, if found
     */
    Optional<AIUserFeedback> findById(UUID id);
    
    /**
     * Finds feedback for a prediction.
     *
     * @param predictionId The prediction ID
     * @return The feedback for the prediction
     */
    List<AIUserFeedback> findByPredictionId(UUID predictionId);
    
    /**
     * Finds feedback from a user.
     *
     * @param userId The user ID
     * @return The feedback from the user
     */
    List<AIUserFeedback> findByUserId(UUID userId);
    
    /**
     * Finds feedback by type.
     *
     * @param type The feedback type
     * @return The feedback of the specified type
     */
    List<AIUserFeedback> findByType(AIUserFeedback.FeedbackType type);
    
    /**
     * Finds the most recent feedback.
     *
     * @param limit The maximum number of feedback entries to return
     * @return The most recent feedback
     */
    List<AIUserFeedback> findMostRecent(int limit);
    
    /**
     * Deletes feedback.
     *
     * @param id The feedback ID
     */
    void delete(UUID id);
    
    /**
     * Deletes all feedback for a prediction.
     *
     * @param predictionId The prediction ID
     */
    void deleteByPredictionId(UUID predictionId);
    
    /**
     * Finds feedback from a user for a specific field.
     * This is determined by looking up the field name from the corresponding prediction.
     *
     * @param userId The user ID
     * @param fieldName The field name
     * @return The feedback from the user for the specified field
     */
    List<AIUserFeedback> findByUserIdAndFieldName(UUID userId, String fieldName);
    
    /**
     * Finds feedback from a user created since the specified date.
     *
     * @param userId The user ID
     * @param since The date from which to find feedback
     * @return The feedback from the user since the specified date
     */
    List<AIUserFeedback> findByUserIdSince(UUID userId, LocalDateTime since);
    
    /**
     * Counts the number of times a user has accepted predictions for a specific field.
     *
     * @param userId The user ID
     * @param fieldName The field name 
     * @return The number of accepted predictions
     */
    int countAcceptedByUserIdAndFieldName(UUID userId, String fieldName);
    
    /**
     * Clears all feedback from the repository.
     * Primarily used for testing and administration.
     */
    void clear();
}