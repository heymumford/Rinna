/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Request object for creating a work item.
 */
public class WorkItemCreateRequest {
    private String title;
    private String description;
    private WorkItemType type;
    private Priority priority;
    private String project;
    private String assignee;
    private Map<String, String> metadata;
    
    private WorkItemCreateRequest(Builder builder) {
        this.title = builder.title;
        this.description = builder.description;
        this.type = builder.type;
        this.priority = builder.priority;
        this.project = builder.project;
        this.assignee = builder.assignee;
        this.metadata = builder.metadata;
    }
    
    /**
     * Gets the title.
     *
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Gets the description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Gets the work item type.
     *
     * @return the work item type
     */
    public WorkItemType getType() {
        return type;
    }
    
    /**
     * Gets the priority.
     *
     * @return the priority
     */
    public Priority getPriority() {
        return priority;
    }
    
    /**
     * Gets the project.
     *
     * @return the project
     */
    public String getProject() {
        return project;
    }
    
    /**
     * Gets the assignee.
     *
     * @return the assignee
     */
    public String getAssignee() {
        return assignee;
    }
    
    /**
     * Gets the metadata.
     *
     * @return the metadata
     */
    public Map<String, String> getMetadata() {
        return metadata;
    }
    
    /**
     * Builder for WorkItemCreateRequest.
     */
    public static class Builder {
        private String title;
        private String description;
        private WorkItemType type = WorkItemType.TASK; // Default
        private Priority priority = Priority.MEDIUM; // Default
        private String project;
        private String assignee;
        private Map<String, String> metadata = new HashMap<>();
        
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
         * Sets the work item type.
         *
         * @param type the work item type
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
         * Sets the project.
         *
         * @param project the project
         * @return this builder
         */
        public Builder project(String project) {
            this.project = project;
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
         * Adds a metadata item.
         *
         * @param key the metadata key
         * @param value the metadata value
         * @return this builder
         */
        public Builder metadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }
        
        /**
         * Sets the metadata map.
         *
         * @param metadata the metadata map
         * @return this builder
         */
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }
        
        /**
         * Builds the WorkItemCreateRequest.
         *
         * @return a new WorkItemCreateRequest
         */
        public WorkItemCreateRequest build() {
            return new WorkItemCreateRequest(this);
        }
    }
}