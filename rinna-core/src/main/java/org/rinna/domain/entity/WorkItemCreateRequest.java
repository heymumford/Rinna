/*
 * Domain entity for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.entity;

import java.util.Optional;
import java.util.UUID;

/**
 * Represents a request to create a new work item.
 * This is a value object that contains the data needed to create a work item.
 */
public class WorkItemCreateRequest {
    private final String title;
    private final String description;
    private final WorkItemType type;
    private final Priority priority;
    private final String assignee;
    private final UUID parentId;
    
    /**
     * Creates a new WorkItemCreateRequest with the given parameters.
     * 
     * @param title the title of the work item
     * @param description the description of the work item
     * @param type the type of the work item
     * @param priority the priority of the work item
     * @param assignee the assignee of the work item
     * @param parentId the parent ID of the work item, or null if no parent
     */
    public WorkItemCreateRequest(
            String title, 
            String description, 
            WorkItemType type, 
            Priority priority, 
            String assignee, 
            UUID parentId) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.assignee = assignee;
        this.parentId = parentId;
    }
    
    /**
     * Returns the title.
     * 
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Returns the description.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns the work item type.
     * 
     * @return the type
     */
    public WorkItemType getType() {
        return type;
    }
    
    /**
     * Returns the priority.
     * 
     * @return the priority
     */
    public Priority getPriority() {
        return priority;
    }
    
    /**
     * Returns the assignee.
     * 
     * @return the assignee
     */
    public String getAssignee() {
        return assignee;
    }
    
    /**
     * Returns the parent ID as an Optional.
     * 
     * @return an Optional containing the parent ID, or empty if no parent
     */
    public Optional<UUID> getParentId() {
        return Optional.ofNullable(parentId);
    }
    
    /**
     * Builder for creating WorkItemCreateRequest instances.
     */
    public static class Builder {
        private String title;
        private String description;
        private WorkItemType type;
        private Priority priority = Priority.MEDIUM; // Default
        private String assignee;
        private UUID parentId;
        
        /**
         * Sets the title.
         * 
         * @param title the title
         * @return this builder
         */
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        /**
         * Sets the description.
         * 
         * @param description the description
         * @return this builder
         */
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        /**
         * Sets the type.
         * 
         * @param type the type
         * @return this builder
         */
        public Builder type(WorkItemType type) {
            this.type = type;
            return this;
        }
        
        /**
         * Sets the priority.
         * 
         * @param priority the priority
         * @return this builder
         */
        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }
        
        /**
         * Sets the assignee.
         * 
         * @param assignee the assignee
         * @return this builder
         */
        public Builder assignee(String assignee) {
            this.assignee = assignee;
            return this;
        }
        
        /**
         * Sets the parent ID.
         * 
         * @param parentId the parent ID
         * @return this builder
         */
        public Builder parentId(UUID parentId) {
            this.parentId = parentId;
            return this;
        }
        
        /**
         * Builds a new WorkItemCreateRequest.
         * 
         * @return a new WorkItemCreateRequest
         */
        public WorkItemCreateRequest build() {
            if (title == null || title.isEmpty()) {
                throw new IllegalStateException("Title is required");
            }
            if (type == null) {
                throw new IllegalStateException("Type is required");
            }
            
            return new WorkItemCreateRequest(
                    title, 
                    description, 
                    type, 
                    priority, 
                    assignee, 
                    parentId);
        }
    }
}