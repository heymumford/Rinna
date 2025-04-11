/*
 * Domain model for field prioritization in AI predictions
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
 * Represents the priority of a field for AI prediction.
 * <p>
 * Field priorities determine which fields should be given preference
 * for AI-based prediction based on various factors like usage frequency,
 * value to users, and data completeness.
 * </p>
 */
public record AIFieldPriority(
    UUID id,
    String fieldName,
    double priorityScore,
    int usageCount,
    double completionRate,
    double valueRating,
    LocalDateTime lastUpdated
) {
    /**
     * Constructor with validation.
     */
    public AIFieldPriority {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(fieldName, "fieldName must not be null");
        Objects.requireNonNull(lastUpdated, "lastUpdated must not be null");
        
        // Validate priority score is between 0 and 1
        if (priorityScore < 0 || priorityScore > 1) {
            throw new IllegalArgumentException("priorityScore must be between 0 and 1");
        }
        
        // Validate completion rate is between 0 and 1
        if (completionRate < 0 || completionRate > 1) {
            throw new IllegalArgumentException("completionRate must be between 0 and 1");
        }
        
        // Validate value rating is between 0 and 1
        if (valueRating < 0 || valueRating > 1) {
            throw new IllegalArgumentException("valueRating must be between 0 and 1");
        }
    }
    
    /**
     * Creates a new instance with an incremented usage count.
     *
     * @return A new AIFieldPriority with incremented usage count
     */
    public AIFieldPriority withIncrementedUsage() {
        return new AIFieldPriority(
            this.id,
            this.fieldName,
            this.priorityScore,
            this.usageCount + 1,
            this.completionRate,
            this.valueRating,
            LocalDateTime.now()
        );
    }
    
    /**
     * Creates a new instance with updated completion rate.
     *
     * @param newCompletionRate The new completion rate
     * @return A new AIFieldPriority with updated completion rate
     */
    public AIFieldPriority withCompletionRate(double newCompletionRate) {
        if (newCompletionRate < 0 || newCompletionRate > 1) {
            throw new IllegalArgumentException("completionRate must be between 0 and 1");
        }
        
        return new AIFieldPriority(
            this.id,
            this.fieldName,
            this.priorityScore,
            this.usageCount,
            newCompletionRate,
            this.valueRating,
            LocalDateTime.now()
        );
    }
    
    /**
     * Creates a new instance with updated value rating.
     *
     * @param newValueRating The new value rating
     * @return A new AIFieldPriority with updated value rating
     */
    public AIFieldPriority withValueRating(double newValueRating) {
        if (newValueRating < 0 || newValueRating > 1) {
            throw new IllegalArgumentException("valueRating must be between 0 and 1");
        }
        
        return new AIFieldPriority(
            this.id,
            this.fieldName,
            this.priorityScore,
            this.usageCount,
            this.completionRate,
            newValueRating,
            LocalDateTime.now()
        );
    }
    
    /**
     * Creates a new instance with recalculated priority score.
     * <p>
     * Priority score is calculated based on usage count, completion rate, and value rating.
     * </p>
     *
     * @return A new AIFieldPriority with recalculated priority score
     */
    public AIFieldPriority withRecalculatedPriority() {
        // Calculate priority score as weighted average of normalized usage, 
        // inverse completion rate (less complete fields get higher priority),
        // and value rating
        
        // Normalize usage count (assuming 100 usages is high)
        double normalizedUsage = Math.min(this.usageCount / 100.0, 1.0);
        
        // Calculate inverse completion rate (1 - completionRate)
        double inverseCompletion = 1.0 - this.completionRate;
        
        // Weight factors - can be adjusted based on importance
        double usageWeight = 0.3;
        double completionWeight = 0.4;
        double valueWeight = 0.3;
        
        // Calculate weighted average
        double newPriorityScore = 
            (normalizedUsage * usageWeight) +
            (inverseCompletion * completionWeight) +
            (this.valueRating * valueWeight);
        
        return new AIFieldPriority(
            this.id,
            this.fieldName,
            newPriorityScore,
            this.usageCount,
            this.completionRate,
            this.valueRating,
            LocalDateTime.now()
        );
    }
    
    /**
     * Returns a builder for creating AIFieldPriority instances.
     * 
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for AIFieldPriority.
     */
    public static class Builder {
        private UUID id = UUID.randomUUID();
        private String fieldName;
        private double priorityScore = 0.5; // Default middle priority
        private int usageCount = 0;
        private double completionRate = 0.0; // Default to not completed
        private double valueRating = 0.5; // Default middle value
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
         * Sets the priority score.
         * 
         * @param priorityScore The priority score
         * @return This builder
         */
        public Builder priorityScore(double priorityScore) {
            this.priorityScore = priorityScore;
            return this;
        }
        
        /**
         * Sets the usage count.
         * 
         * @param usageCount The usage count
         * @return This builder
         */
        public Builder usageCount(int usageCount) {
            this.usageCount = usageCount;
            return this;
        }
        
        /**
         * Sets the completion rate.
         * 
         * @param completionRate The completion rate
         * @return This builder
         */
        public Builder completionRate(double completionRate) {
            this.completionRate = completionRate;
            return this;
        }
        
        /**
         * Sets the value rating.
         * 
         * @param valueRating The value rating
         * @return This builder
         */
        public Builder valueRating(double valueRating) {
            this.valueRating = valueRating;
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
         * Builds a new AIFieldPriority instance.
         * 
         * @return A new AIFieldPriority
         */
        public AIFieldPriority build() {
            return new AIFieldPriority(
                id,
                fieldName,
                priorityScore,
                usageCount,
                completionRate,
                valueRating,
                lastUpdated
            );
        }
    }
}