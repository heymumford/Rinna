/*
 * Service interface for AI model management
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.usecase.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.rinna.domain.model.ai.AIFieldConfidence;
import org.rinna.domain.model.ai.AIModelConfig;

/**
 * Service interface for AI model management.
 */
public interface AIModelManagementService {

    /**
     * Registers a new AI model.
     *
     * @param modelId The model ID
     * @param modelName The model name
     * @param modelVersion The model version
     * @param modelType The model type
     * @param supportedFields The fields supported by the model
     * @param parameters The model parameters
     * @return The model configuration
     */
    AIModelConfig registerModel(
            String modelId,
            String modelName,
            String modelVersion,
            String modelType,
            List<String> supportedFields,
            Map<String, Object> parameters);
    
    /**
     * Gets a model configuration by ID.
     *
     * @param modelId The model ID
     * @return The model configuration, if found
     */
    Optional<AIModelConfig> getModelConfig(String modelId);
    
    /**
     * Gets all model configurations.
     *
     * @return All model configurations
     */
    List<AIModelConfig> getAllModelConfigs();
    
    /**
     * Gets all enabled model configurations.
     *
     * @return All enabled model configurations
     */
    List<AIModelConfig> getEnabledModelConfigs();
    
    /**
     * Enables or disables a model.
     *
     * @param modelId The model ID
     * @param enabled Whether the model should be enabled
     * @return The updated model configuration, if found
     */
    Optional<AIModelConfig> setModelEnabled(String modelId, boolean enabled);
    
    /**
     * Updates the parameters of a model.
     *
     * @param modelId The model ID
     * @param parameters The new parameters
     * @return The updated model configuration, if found
     */
    Optional<AIModelConfig> updateModelParameters(String modelId, Map<String, Object> parameters);
    
    /**
     * Updates the supported fields of a model.
     *
     * @param modelId The model ID
     * @param supportedFields The new supported fields
     * @return The updated model configuration, if found
     */
    Optional<AIModelConfig> updateSupportedFields(String modelId, List<String> supportedFields);
    
    /**
     * Gets the confidence metrics for a field.
     *
     * @param fieldName The field name
     * @return The confidence metrics for the field
     */
    List<AIFieldConfidence> getFieldConfidence(String fieldName);
    
    /**
     * Gets the confidence metrics for a model.
     *
     * @param modelId The model ID
     * @return The confidence metrics for the model
     */
    List<AIFieldConfidence> getModelConfidence(String modelId);
    
    /**
     * Gets the field performance metrics.
     *
     * @return The field performance metrics
     */
    Map<String, Map<String, Object>> getFieldPerformanceMetrics();
    
    /**
     * Gets the model performance metrics.
     *
     * @return The model performance metrics
     */
    Map<String, Map<String, Object>> getModelPerformanceMetrics();
    
    /**
     * Unregisters a model.
     *
     * @param modelId The model ID
     * @return True if the model was unregistered, false otherwise
     */
    boolean unregisterModel(String modelId);
}