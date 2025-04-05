/*
 * Domain entity for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a request to create a new work item.
 * This is a value object that contains the data needed to create a work item.
 */
public record WorkItemCreateRequest(
    String title,
    String description,
    WorkItemType type,
    Priority priority,
    String assignee,
    UUID parentId,
    UUID projectId,
    String visibility,
    boolean localOnly,
    Map<String, String> metadata
) {
    /**
     * Creates a new WorkItemCreateRequest with validated parameters.
     */
    public WorkItemCreateRequest {
        if (title == null || title.isEmpty()) {
            throw new IllegalArgumentException("Title is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type is required");
        }
        visibility = visibility != null ? visibility : "PUBLIC";
        metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
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
     * Returns a defensive copy of the metadata.
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
        
        public Builder title(String title) {
            this.title = title;
            return this;
        }
        
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        public Builder type(WorkItemType type) {
            this.type = type;
            return this;
        }
        
        public Builder priority(Priority priority) {
            this.priority = priority;
            return this;
        }
        
        public Builder assignee(String assignee) {
            this.assignee = assignee;
            return this;
        }
        
        public Builder parentId(UUID parentId) {
            this.parentId = parentId;
            return this;
        }
        
        public Builder projectId(UUID projectId) {
            this.projectId = projectId;
            return this;
        }
        
        public Builder visibility(String visibility) {
            this.visibility = visibility;
            return this;
        }
        
        public Builder localOnly(boolean localOnly) {
            this.localOnly = localOnly;
            return this;
        }
        
        public Builder addMetadata(String key, String value) {
            this.metadata.put(key, value);
            return this;
        }
        
        public Builder metadata(Map<String, String> metadata) {
            this.metadata = new HashMap<>(metadata);
            return this;
        }
        
        public WorkItemCreateRequest build() {
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
                metadata
            );
        }
    }
}