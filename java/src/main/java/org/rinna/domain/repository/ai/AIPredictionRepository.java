/*
 * Repository interface for AI predictions
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

import org.rinna.domain.model.ai.AISmartFieldPrediction;

/**
 * Repository interface for AI field predictions.
 */
public interface AIPredictionRepository {

    /**
     * Saves a prediction.
     *
     * @param prediction The prediction to save
     * @return The saved prediction
     */
    AISmartFieldPrediction save(AISmartFieldPrediction prediction);
    
    /**
     * Finds a prediction by ID.
     *
     * @param id The prediction ID
     * @return The prediction, if found
     */
    Optional<AISmartFieldPrediction> findById(UUID id);
    
    /**
     * Finds predictions for a work item.
     *
     * @param workItemId The work item ID
     * @return The predictions for the work item
     */
    List<AISmartFieldPrediction> findByWorkItemId(UUID workItemId);
    
    /**
     * Finds predictions for a field in a work item.
     *
     * @param workItemId The work item ID
     * @param fieldName The field name
     * @return The predictions for the field
     */
    List<AISmartFieldPrediction> findByWorkItemIdAndFieldName(UUID workItemId, String fieldName);
    
    /**
     * Updates the user acceptance status of a prediction.
     *
     * @param predictionId The prediction ID
     * @param accepted Whether the prediction was accepted
     * @return The updated prediction, if found
     */
    Optional<AISmartFieldPrediction> updateAcceptanceStatus(UUID predictionId, boolean accepted);
    
    /**
     * Finds the most recent predictions for a work item.
     *
     * @param workItemId The work item ID
     * @param limit The maximum number of predictions to return
     * @return The most recent predictions for the work item
     */
    List<AISmartFieldPrediction> findMostRecentByWorkItemId(UUID workItemId, int limit);
    
    /**
     * Deletes a prediction.
     *
     * @param id The prediction ID
     */
    void delete(UUID id);
    
    /**
     * Deletes all predictions for a work item.
     *
     * @param workItemId The work item ID
     */
    void deleteByWorkItemId(UUID workItemId);
    
    /**
     * Finds predictions for a specific field created since the specified date.
     *
     * @param fieldName The field name
     * @param since The date from which to find predictions
     * @return The predictions for the field since the specified date
     */
    List<AISmartFieldPrediction> findByFieldNameSince(String fieldName, LocalDateTime since);
    
    /**
     * Finds predictions for a specific field, model, and value.
     *
     * @param fieldName The field name
     * @param modelId The model ID
     * @param predictedValue The predicted value
     * @return The predictions matching the criteria
     */
    List<AISmartFieldPrediction> findByFieldNameAndModelIdAndValue(String fieldName, String modelId, Object predictedValue);
    
    /**
     * Clears all predictions from the repository.
     * Primarily used for testing and administration.
     */
    void clear();
}