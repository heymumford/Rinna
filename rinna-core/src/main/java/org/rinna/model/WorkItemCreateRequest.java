/*
 * Model class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.model;

import java.util.Optional;
import java.util.UUID;

/**
 * Request object for creating a new work item.
 */
public class WorkItemCreateRequest {
    private final String title;
    private final String description;
    private final WorkItemType type;
    private final Priority priority;
    private final String assignee;
    private final UUID parentId;
    
    private WorkItemCreateRequest(Builder builder) {
        this.title = builder.title;
        this.description = builder.description;
        this.type = builder.type;
        this.priority = builder.priority;
        this.assignee = builder.assignee;
        this.parentId = builder.parentId;
    }
    
    /**
     * Returns the title for the new work item.
     * 
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Returns the description for the new work item.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns the type for the new work item.
     * 
     * @return the work item type
     */
    public WorkItemType getType() {
        return type;
    }
    
    /**
     * Returns the priority for the new work item.
     * 
     * @return the priority
     */
    public Priority getPriority() {
        return priority;
    }
    
    /**
     * Returns the assignee for the new work item.
     * 
     * @return the assignee
     */
    public String getAssignee() {
        return assignee;
    }
    
    /**
     * Returns the parent ID for the new work item if it has a parent.
     * 
     * @return an Optional containing the parent ID, or empty if no parent
     */
    public Optional<UUID> getParentId() {
        return Optional.ofNullable(parentId);
    }
    
    /**
     * Returns a new builder for creating a WorkItemCreateRequest.
     * 
     * @return a new builder instance
     */
    public static Builder builder() {
        return new Builder();
    }
    
    /**
     * Builder for creating WorkItemCreateRequest instances.
     */
    public static class Builder {
        private String title;
        private String description = "";
        private WorkItemType type;
        private Priority priority = Priority.MEDIUM;
        private String assignee;
        private UUID parentId;
        
        /**
         * Sets the title for the new work item.
         * 
         * @param title the title
         * @return this builder instance
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        /**
         * Sets the description for the new work item.
         * 
         * @param description the description
         * @return this builder instance
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Sets the type for the new work item.
         * 
         * @param type the work item type
         * @return this builder instance
         */
        public Builder type(WorkItemType type) {
            this.type = type;
            return this;
        }
        
        /**
         * Sets the priority for the new work item.
         * 
         * @param priority the priority
         * @return this builder instance
         */
        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }
        
        /**
         * Sets the assignee for the new work item.
         * 
         * @param assignee the assignee
         * @return this builder instance
         */
        public Builder assignee(String assignee) {
            this.assignee = assignee;
            return this;
        }
        
        /**
         * Sets the parent ID for the new work item.
         * 
         * @param parentId the parent ID
         * @return this builder instance
         */
        public Builder parentId(UUID parentId) {
            this.parentId = parentId;
            return this;
        }
        
        /**
         * Builds a new WorkItemCreateRequest with the configured values.
         * 
         * @return a new WorkItemCreateRequest instance
         */
        public WorkItemCreateRequest build() {
            if (title == null || title.trim().isEmpty()) {
                throw new IllegalStateException("Title is required");
            }
            if (type == null) {
                throw new IllegalStateException("Type is required");
            }
            return new WorkItemCreateRequest(this);
        }
    }
}