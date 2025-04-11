/*
 * Domain model for AI model configuration
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model.ai;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Configuration for an AI prediction model.
 * <p>
 * This model stores configuration parameters for AI prediction models,
 * including which fields they support and how they should be trained.
 * </p>
 */
public record AIModelConfig(
    UUID id,
    String modelId,
    String modelName,
    String modelVersion,
    String modelType,
    List<String> supportedFields,
    Map<String, Object> parameters,
    boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime lastUpdated
) {
    /**
     * Constructor with validation.
     */
    public AIModelConfig {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(modelId, "modelId must not be null");
        Objects.requireNonNull(modelName, "modelName must not be null");
        Objects.requireNonNull(modelVersion, "modelVersion must not be null");
        Objects.requireNonNull(modelType, "modelType must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(lastUpdated, "lastUpdated must not be null");
        
        // Create defensive copies of mutable collections
        if (supportedFields != null) {
            supportedFields = Collections.unmodifiableList(List.copyOf(supportedFields));
        } else {
            supportedFields = Collections.emptyList();
        }
        
        if (parameters != null) {
            parameters = Collections.unmodifiableMap(Map.copyOf(parameters));
        } else {
            parameters = Collections.emptyMap();
        }
    }
    
    /**
     * Creates a new instance with updated enabled status.
     *
     * @param enabled Whether the model is enabled
     * @return A new AIModelConfig with updated status
     */
    public AIModelConfig withEnabled(boolean enabled) {
        return new AIModelConfig(
            this.id,
            this.modelId,
            this.modelName,
            this.modelVersion,
            this.modelType,
            this.supportedFields,
            this.parameters,
            enabled,
            this.createdAt,
            LocalDateTime.now()
        );
    }
    
    /**
     * Creates a new instance with updated parameters.
     *
     * @param parameters The new parameters
     * @return A new AIModelConfig with updated parameters
     */
    public AIModelConfig withParameters(Map<String, Object> parameters) {
        return new AIModelConfig(
            this.id,
            this.modelId,
            this.modelName,
            this.modelVersion,
            this.modelType,
            this.supportedFields,
            parameters,
            this.enabled,
            this.createdAt,
            LocalDateTime.now()
        );
    }
    
    /**
     * Creates a new instance with updated supported fields.
     *
     * @param supportedFields The new supported fields
     * @return A new AIModelConfig with updated supported fields
     */
    public AIModelConfig withSupportedFields(List<String> supportedFields) {
        return new AIModelConfig(
            this.id,
            this.modelId,
            this.modelName,
            this.modelVersion,
            this.modelType,
            supportedFields,
            this.parameters,
            this.enabled,
            this.createdAt,
            LocalDateTime.now()
        );
    }
    
    /**
     * Returns a builder for creating AIModelConfig instances.
     * 
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for AIModelConfig.
     */
    public static class Builder {
        private UUID id = UUID.randomUUID();
        private String modelId;
        private String modelName;
        private String modelVersion = "1.0.0";
        private String modelType = "DEFAULT";
        private List<String> supportedFields = Collections.emptyList();
        private Map<String, Object> parameters = Collections.emptyMap();
        private boolean enabled = true;
        private LocalDateTime createdAt = LocalDateTime.now();
        private LocalDateTime lastUpdated = LocalDateTime.now();
        
        /**
         * Sets the ID.
         * 
         * @param id The ID
         * @return This builder
         */
        public Builder id(UUID id) {
            this.id = id;
            return this;
        }
        
        /**
         * Sets the model ID.
         * 
         * @param modelId The model ID
         * @return This builder
         */
        public Builder modelId(String modelId) {
            this.modelId = modelId;
            return this;
        }
        
        /**
         * Sets the model name.
         * 
         * @param modelName The model name
         * @return This builder
         */
        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }
        
        /**
         * Sets the model version.
         * 
         * @param modelVersion The model version
         * @return This builder
         */
        public Builder modelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
            return this;
        }
        
        /**
         * Sets the model type.
         * 
         * @param modelType The model type
         * @return This builder
         */
        public Builder modelType(String modelType) {
            this.modelType = modelType;
            return this;
        }
        
        /**
         * Sets the supported fields.
         * 
         * @param supportedFields The supported fields
         * @return This builder
         */
        public Builder supportedFields(List<String> supportedFields) {
            this.supportedFields = supportedFields;
            return this;
        }
        
        /**
         * Sets the parameters.
         * 
         * @param parameters The parameters
         * @return This builder
         */
        public Builder parameters(Map<String, Object> parameters) {
            this.parameters = parameters;
            return this;
        }
        
        /**
         * Sets whether the model is enabled.
         * 
         * @param enabled Whether the model is enabled
         * @return This builder
         */
        public Builder enabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }
        
        /**
         * Sets the creation timestamp.
         * 
         * @param createdAt The creation timestamp
         * @return This builder
         */
        public Builder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        /**
         * Sets the last updated timestamp.
         * 
         * @param lastUpdated The last updated timestamp
         * @return This builder
         */
        public Builder lastUpdated(LocalDateTime lastUpdated) {
            this.lastUpdated = lastUpdated;
            return this;
        }
        
        /**
         * Builds a new AIModelConfig instance.
         * 
         * @return A new AIModelConfig
         */
        public AIModelConfig build() {
            return new AIModelConfig(
                id,
                modelId,
                modelName,
                modelVersion,
                modelType,
                supportedFields,
                parameters,
                enabled,
                createdAt,
                lastUpdated
            );
        }
    }
}