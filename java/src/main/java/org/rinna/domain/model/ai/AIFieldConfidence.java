/*
 * Domain model for AI field prediction confidence tracking
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model.ai;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Tracks confidence metrics for AI field predictions.
 * <p>
 * This model stores historical performance data about AI predictions
 * for specific fields, allowing the system to adjust confidence scores
 * based on past accuracy.
 * </p>
 */
public record AIFieldConfidence(
    UUID id,
    String fieldName,
    String modelId,
    int predictionCount,
    int acceptedCount,
    int modifiedCount,
    int rejectedCount,
    double acceptanceRate,
    LocalDateTime lastUpdated
) {
    /**
     * Constructor with validation.
     */
    public AIFieldConfidence {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(fieldName, "fieldName must not be null");
        Objects.requireNonNull(modelId, "modelId must not be null");
        Objects.requireNonNull(lastUpdated, "lastUpdated must not be null");
        
        // Validate prediction count is non-negative
        if (predictionCount < 0) {
            throw new IllegalArgumentException("predictionCount must not be negative");
        }
        
        // Validate accepted count is non-negative
        if (acceptedCount < 0) {
            throw new IllegalArgumentException("acceptedCount must not be negative");
        }
        
        // Validate modified count is non-negative
        if (modifiedCount < 0) {
            throw new IllegalArgumentException("modifiedCount must not be negative");
        }
        
        // Validate rejected count is non-negative
        if (rejectedCount < 0) {
            throw new IllegalArgumentException("rejectedCount must not be negative");
        }
        
        // Validate acceptance rate is between 0 and 1
        if (acceptanceRate < 0 || acceptanceRate > 1) {
            throw new IllegalArgumentException("acceptanceRate must be between 0 and 1");
        }
        
        // Validate sum of counts matches total predictions
        if (acceptedCount + modifiedCount + rejectedCount > predictionCount) {
            throw new IllegalArgumentException(
                    "Sum of acceptedCount, modifiedCount, and rejectedCount must not exceed predictionCount");
        }
    }
    
    /**
     * Creates a new instance with updated counts based on feedback.
     *
     * @param feedbackType The feedback type
     * @return A new AIFieldConfidence with updated counts
     */
    public AIFieldConfidence withFeedback(AIUserFeedback.FeedbackType feedbackType) {
        int newPredictionCount = this.predictionCount + 1;
        int newAcceptedCount = this.acceptedCount;
        int newModifiedCount = this.modifiedCount;
        int newRejectedCount = this.rejectedCount;
        
        switch (feedbackType) {
            case ACCEPTED:
                newAcceptedCount++;
                break;
            case MODIFIED:
            case HELPFUL_BUT_MODIFIED:
                newModifiedCount++;
                break;
            case REJECTED:
            case REPORTED:
                newRejectedCount++;
                break;
            default:
                throw new IllegalArgumentException("Unknown feedback type: " + feedbackType);
        }
        
        // Calculate new acceptance rate
        double newAcceptanceRate = (double) (newAcceptedCount) / newPredictionCount;
        
        return new AIFieldConfidence(
            this.id,
            this.fieldName,
            this.modelId,
            newPredictionCount,
            newAcceptedCount,
            newModifiedCount,
            newRejectedCount,
            newAcceptanceRate,
            LocalDateTime.now()
        );
    }
    
    /**
     * Returns a builder for creating AIFieldConfidence instances.
     * 
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for AIFieldConfidence.
     */
    public static class Builder {
        private UUID id = UUID.randomUUID();
        private String fieldName;
        private String modelId;
        private int predictionCount = 0;
        private int acceptedCount = 0;
        private int modifiedCount = 0;
        private int rejectedCount = 0;
        private double acceptanceRate = 0.0;
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
         * Sets the field name.
         * 
         * @param fieldName The field name
         * @return This builder
         */
        public Builder fieldName(String fieldName) {
            this.fieldName = fieldName;
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
         * Sets the prediction count.
         * 
         * @param predictionCount The prediction count
         * @return This builder
         */
        public Builder predictionCount(int predictionCount) {
            this.predictionCount = predictionCount;
            return this;
        }
        
        /**
         * Sets the accepted count.
         * 
         * @param acceptedCount The accepted count
         * @return This builder
         */
        public Builder acceptedCount(int acceptedCount) {
            this.acceptedCount = acceptedCount;
            return this;
        }
        
        /**
         * Sets the modified count.
         * 
         * @param modifiedCount The modified count
         * @return This builder
         */
        public Builder modifiedCount(int modifiedCount) {
            this.modifiedCount = modifiedCount;
            return this;
        }
        
        /**
         * Sets the rejected count.
         * 
         * @param rejectedCount The rejected count
         * @return This builder
         */
        public Builder rejectedCount(int rejectedCount) {
            this.rejectedCount = rejectedCount;
            return this;
        }
        
        /**
         * Sets the acceptance rate.
         * 
         * @param acceptanceRate The acceptance rate
         * @return This builder
         */
        public Builder acceptanceRate(double acceptanceRate) {
            this.acceptanceRate = acceptanceRate;
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
         * Builds a new AIFieldConfidence instance.
         * 
         * @return A new AIFieldConfidence
         */
        public AIFieldConfidence build() {
            // Calculate acceptance rate if not explicitly set
            if (acceptanceRate == 0.0 && predictionCount > 0) {
                acceptanceRate = (double) acceptedCount / predictionCount;
            }
            
            return new AIFieldConfidence(
                id,
                fieldName,
                modelId,
                predictionCount,
                acceptedCount,
                modifiedCount,
                rejectedCount,
                acceptanceRate,
                lastUpdated
            );
        }
    }
}