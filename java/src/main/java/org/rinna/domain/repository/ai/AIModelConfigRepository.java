/*
 * Repository interface for AI model configurations
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.repository.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.ai.AIModelConfig;

/**
 * Repository interface for AI model configurations.
 */
public interface AIModelConfigRepository {

    /**
     * Saves a model configuration.
     *
     * @param modelConfig The model configuration to save
     * @return The saved model configuration
     */
    AIModelConfig save(AIModelConfig modelConfig);
    
    /**
     * Finds a model configuration by ID.
     *
     * @param id The model configuration ID
     * @return The model configuration, if found
     */
    Optional<AIModelConfig> findById(UUID id);
    
    /**
     * Finds a model configuration by model ID.
     *
     * @param modelId The model ID
     * @return The model configuration, if found
     */
    Optional<AIModelConfig> findByModelId(String modelId);
    
    /**
     * Finds model configurations by model type.
     *
     * @param modelType The model type
     * @return The model configurations of the specified type
     */
    List<AIModelConfig> findByModelType(String modelType);
    
    /**
     * Finds all enabled model configurations.
     *
     * @return All enabled model configurations
     */
    List<AIModelConfig> findAllEnabled();
    
    /**
     * Finds all model configurations.
     *
     * @return All model configurations
     */
    List<AIModelConfig> findAll();
    
    /**
     * Updates the enabled status of a model configuration.
     *
     * @param modelId The model ID
     * @param enabled Whether the model is enabled
     * @return The updated model configuration, if found
     */
    Optional<AIModelConfig> updateEnabledStatus(String modelId, boolean enabled);
    
    /**
     * Updates the parameters of a model configuration.
     *
     * @param modelId The model ID
     * @param parameters The new parameters
     * @return The updated model configuration, if found
     */
    Optional<AIModelConfig> updateParameters(String modelId, Map<String, Object> parameters);
    
    /**
     * Updates the supported fields of a model configuration.
     *
     * @param modelId The model ID
     * @param supportedFields The new supported fields
     * @return The updated model configuration, if found
     */
    Optional<AIModelConfig> updateSupportedFields(String modelId, List<String> supportedFields);
    
    /**
     * Deletes a model configuration.
     *
     * @param id The model configuration ID
     */
    void delete(UUID id);
    
    /**
     * Clears all model configurations from the repository.
     * Primarily used for testing and administration.
     */
    void clear();
}