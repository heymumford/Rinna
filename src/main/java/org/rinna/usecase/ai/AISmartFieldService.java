/*
 * Service interface for AI smart field population
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.usecase.ai;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.rinna.domain.model.ai.AIFieldPriority;
import org.rinna.domain.model.ai.AISmartFieldPrediction;
import org.rinna.domain.model.ai.AIUserFeedback;

/**
 * Service interface for AI-based smart field population.
 */
public interface AISmartFieldService {

    /**
     * Predicts values for empty fields in a work item.
     *
     * @param workItemId The work item ID
     * @param currentFields The current field values
     * @return A map of field names to predicted values
     */
    Map<String, Object> predictFieldValues(UUID workItemId, Map<String, Object> currentFields);
    
    /**
     * Predicts values for specific fields in a work item.
     *
     * @param workItemId The work item ID
     * @param currentFields The current field values
     * @param fieldsToPredict The fields to predict
     * @return A map of field names to predicted values
     */
    Map<String, Object> predictSpecificFields(
            UUID workItemId, Map<String, Object> currentFields, List<String> fieldsToPredict);
            
    /**
     * Predicts values for specific fields in a work item with explicit user context.
     *
     * @param workItemId The work item ID
     * @param userId The user ID for personalized predictions
     * @param currentFields The current field values
     * @param fieldsToPredict The fields to predict
     * @return A map of field names to predicted values
     */
    default Map<String, Object> predictSpecificFields(
            UUID workItemId, UUID userId, Map<String, Object> currentFields, List<String> fieldsToPredict) {
        // Default implementation delegates to the standard method
        return predictSpecificFields(workItemId, currentFields, fieldsToPredict);
    }
    
    /**
     * Gets the confidence score for a prediction.
     *
     * @param predictionId The prediction ID
     * @return The confidence score, if the prediction exists
     */
    Optional<Double> getConfidenceScore(UUID predictionId);
    
    /**
     * Gets the confidence score for a field.
     *
     * @param fieldName The field name
     * @param predictedValue The predicted value
     * @return The confidence score
     */
    double getConfidenceScore(String fieldName, Object predictedValue);
    
    /**
     * Provides feedback on a prediction.
     *
     * @param predictionId The prediction ID
     * @param userId The user ID
     * @param feedbackType The feedback type
     * @param comment An optional comment
     * @param replacementValue An optional replacement value
     * @return The feedback
     */
    AIUserFeedback provideFeedback(
            UUID predictionId, 
            UUID userId, 
            AIUserFeedback.FeedbackType feedbackType, 
            String comment, 
            Object replacementValue);
    
    /**
     * Gets the most recent predictions for a work item.
     *
     * @param workItemId The work item ID
     * @param limit The maximum number of predictions to return
     * @return The most recent predictions for the work item
     */
    List<AISmartFieldPrediction> getRecentPredictions(UUID workItemId, int limit);
    
    /**
     * Gets the field priorities.
     *
     * @param limit The maximum number of field priorities to return
     * @return The field priorities sorted by priority score
     */
    List<AIFieldPriority> getFieldPriorities(int limit);
    
    /**
     * Marks a field as used.
     *
     * @param fieldName The field name
     */
    void trackFieldUsage(String fieldName);
    
    /**
     * Updates the completion rate for a field.
     *
     * @param fieldName The field name
     * @param completionRate The new completion rate
     */
    void updateFieldCompletionRate(String fieldName, double completionRate);
    
    /**
     * Updates the value rating for a field.
     *
     * @param fieldName The field name
     * @param valueRating The new value rating
     */
    void updateFieldValueRating(String fieldName, double valueRating);
    
    /**
     * Gets the supported fields for prediction.
     *
     * @return A list of field names that can be predicted
     */
    List<String> getSupportedFields();
    
    /**
     * Gets the common patterns for a specific user.
     * This can be used to display to the user what patterns have been identified.
     *
     * @param userId The user ID
     * @return A map of field names to common pattern values and strengths
     */
    default Map<String, Map<String, Object>> getUserPatterns(UUID userId) {
        // Default implementation returns an empty map
        return Collections.emptyMap();
    }
    
    /**
     * Clears the user pattern cache for a specific user.
     * This forces the system to rebuild the user's patterns on the next prediction.
     *
     * @param userId The user ID
     */
    default void clearUserPatternCache(UUID userId) {
        // Default implementation does nothing
    }
    
    /**
     * Gets the evidence factors that influence a specific prediction.
     * This allows for explainability of AI decisions.
     *
     * @param predictionId The prediction ID
     * @return The evidence factors for the prediction, if found
     */
    default Optional<Set<String>> getPredictionEvidence(UUID predictionId) {
        // Default implementation returns empty
        return Optional.empty();
    }
}