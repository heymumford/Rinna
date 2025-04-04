/*
 * Domain entity for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.entity;

import java.util.HashMap;
import java.util.Map;
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
    private final UUID projectId;
    private final String visibility;
    private final boolean localOnly;
    private final Map<String, String> metadata;
    
    /**
     * Creates a new WorkItemCreateRequest with the given parameters.
     * 
     * @param title the title of the work item
     * @param description the description of the work item
     * @param type the type of the work item
     * @param priority the priority of the work item
     * @param assignee the assignee of the work item
     * @param parentId the parent ID of the work item, or null if no parent
     * @param projectId the project ID, or null if not associated with a project
     * @param visibility the visibility status
     * @param localOnly whether the work item is local only
     * @param metadata additional metadata as key-value pairs
     */
    public WorkItemCreateRequest(
            String title, 
            String description, 
            WorkItemType type, 
            Priority priority, 
            String assignee, 
            UUID parentId,
            UUID projectId,
            String visibility,
            boolean localOnly,
            Map<String, String> metadata) {
        this.title = title;
        this.description = description;
        this.type = type;
        this.priority = priority;
        this.assignee = assignee;
        this.parentId = parentId;
        this.projectId = projectId;
        this.visibility = visibility != null ? visibility : "PUBLIC";
        this.localOnly = localOnly;
        this.metadata = metadata != null ? metadata : new HashMap<>();
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
     * Returns the project ID as an Optional.
     * 
     * @return an Optional containing the project ID, or empty if not associated with a project
     */
    public Optional<UUID> getProjectId() {
        return Optional.ofNullable(projectId);
    }
    
    /**
     * Returns the visibility status.
     * 
     * @return the visibility status
     */
    public String getVisibility() {
        return visibility;
    }
    
    /**
     * Returns whether this work item is local only.
     * 
     * @return true if local only, false otherwise
     */
    public boolean isLocalOnly() {
        return localOnly;
    }
    
    /**
     * Returns the metadata.
     * 
     * @return the metadata as a map of key-value pairs
     */
    public Map<String, String> getMetadata() {
        return new HashMap<>(metadata);
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
        private UUID projectId;
        private String visibility = "PUBLIC"; // Default
        private boolean localOnly = false; // Default
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
         * Sets the project ID.
         * 
         * @param projectId the project ID
         * @return this builder
         */
        public Builder projectId(UUID projectId) {
            this.projectId = projectId;
            return this;
        }
        
        /**
         * Sets the visibility status.
         * 
         * @param visibility the visibility status
         * @return this builder
         */
        public Builder visibility(String visibility) {
            this.visibility = visibility;
            return this;
        }
        
        /**
         * Sets whether the work item is local only.
         * 
         * @param localOnly true if local only, false otherwise
         * @return this builder
         */
        public Builder localOnly(boolean localOnly) {
            this.localOnly = localOnly;
            return this;
        }
        
        /**
         * Adds a metadata entry.
         * 
         * @param key the metadata key
         * @param value the metadata value
         * @return this builder
         */
        public Builder addMetadata(String key, String value) {
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
                    parentId,
                    projectId,
                    visibility,
                    localOnly,
                    metadata);
        }
    }
}