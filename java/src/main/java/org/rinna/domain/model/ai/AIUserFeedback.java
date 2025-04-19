/*
 * Domain model for user feedback on AI predictions
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
 * Represents user feedback on AI-generated field predictions.
 * <p>
 * Feedback is collected to improve future predictions and measure
 * the effectiveness of the AI prediction system.
 * </p>
 */
public record AIUserFeedback(
    UUID id,
    UUID predictionId,
    UUID userId,
    FeedbackType type,
    String comment,
    Object replacementValue,
    LocalDateTime createdAt
) {
    /**
     * Enum representing the types of feedback a user can provide.
     */
    public enum FeedbackType {
        /**
         * User accepted the prediction as-is.
         */
        ACCEPTED,
        
        /**
         * User rejected the prediction without modification.
         */
        REJECTED,
        
        /**
         * User modified the prediction value.
         */
        MODIFIED,
        
        /**
         * User found the prediction helpful but chose a different value.
         */
        HELPFUL_BUT_MODIFIED,
        
        /**
         * User reported the prediction as inappropriate or incorrect.
         */
        REPORTED
    }
    
    /**
     * Constructor with validation.
     */
    public AIUserFeedback {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(predictionId, "predictionId must not be null");
        Objects.requireNonNull(userId, "userId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        
        // Ensure replacement value is non-null for MODIFIED and HELPFUL_BUT_MODIFIED
        if ((type == FeedbackType.MODIFIED || type == FeedbackType.HELPFUL_BUT_MODIFIED) 
                && replacementValue == null) {
            throw new IllegalArgumentException(
                    "replacementValue must not be null for " + type + " feedback type");
        }
    }
    
    /**
     * Creates a new instance with a comment.
     *
     * @param comment The user comment
     * @return A new AIUserFeedback with the comment
     */
    public AIUserFeedback withComment(String comment) {
        return new AIUserFeedback(
            this.id,
            this.predictionId,
            this.userId,
            this.type,
            comment,
            this.replacementValue,
            this.createdAt
        );
    }
    
    /**
     * Returns a builder for creating AIUserFeedback instances.
     * 
     * @return A new builder
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for AIUserFeedback.
     */
    public static class Builder {
        private UUID id = UUID.randomUUID();
        private UUID predictionId;
        private UUID userId;
        private FeedbackType type;
        private String comment;
        private Object replacementValue;
        private LocalDateTime createdAt = LocalDateTime.now();
        
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
         * Sets the prediction ID.
         * 
         * @param predictionId The prediction ID
         * @return This builder
         */
        public Builder predictionId(UUID predictionId) {
            this.predictionId = predictionId;
            return this;
        }
        
        /**
         * Sets the user ID.
         * 
         * @param userId The user ID
         * @return This builder
         */
        public Builder userId(UUID userId) {
            this.userId = userId;
            return this;
        }
        
        /**
         * Sets the feedback type.
         * 
         * @param type The feedback type
         * @return This builder
         */
        public Builder type(FeedbackType type) {
            this.type = type;
            return this;
        }
        
        /**
         * Sets the comment.
         * 
         * @param comment The comment
         * @return This builder
         */
        public Builder comment(String comment) {
            this.comment = comment;
            return this;
        }
        
        /**
         * Sets the replacement value.
         * 
         * @param replacementValue The replacement value
         * @return This builder
         */
        public Builder replacementValue(Object replacementValue) {
            this.replacementValue = replacementValue;
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
         * Builds a new AIUserFeedback instance.
         * 
         * @return A new AIUserFeedback
         */
        public AIUserFeedback build() {
            return new AIUserFeedback(
                id,
                predictionId,
                userId,
                type,
                comment,
                replacementValue,
                createdAt
            );
        }
    }
}