/*
 * WorkItemDependency - Represents a dependency between work items
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a dependency between two work items.
 * A dependency indicates that one work item must be completed before another can be started.
 */
public class WorkItemDependency {
    private final UUID id;
    private final UUID dependentId;     // The work item that depends on another
    private final UUID dependencyId;    // The work item that must be completed first
    private final String dependencyType; // The type of dependency (e.g., "BLOCKS", "RELATES_TO")
    private final LocalDateTime createdAt;
    private final String createdBy;
    
    /**
     * Creates a new WorkItemDependency with the given attributes.
     *
     * @param id the unique identifier for this dependency
     * @param dependentId the ID of the work item that depends on another
     * @param dependencyId the ID of the work item that must be completed first
     * @param dependencyType the type of dependency
     * @param createdAt the date and time when the dependency was created
     * @param createdBy the user who created the dependency
     */
    private WorkItemDependency(UUID id, UUID dependentId, UUID dependencyId, String dependencyType, 
                              LocalDateTime createdAt, String createdBy) {
        this.id = id;
        this.dependentId = dependentId;
        this.dependencyId = dependencyId;
        this.dependencyType = dependencyType;
        this.createdAt = createdAt;
        this.createdBy = createdBy;
    }
    
    /**
     * Returns the unique identifier for this dependency.
     *
     * @return the ID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Returns the ID of the work item that depends on another.
     *
     * @return the dependent work item ID
     */
    public UUID getDependentId() {
        return dependentId;
    }
    
    /**
     * Returns the ID of the work item that must be completed first.
     *
     * @return the dependency work item ID
     */
    public UUID getDependencyId() {
        return dependencyId;
    }
    
    /**
     * Returns the type of dependency.
     *
     * @return the dependency type
     */
    public String getDependencyType() {
        return dependencyType;
    }
    
    /**
     * Returns the date and time when the dependency was created.
     *
     * @return the creation date and time
     */
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Returns the user who created the dependency.
     *
     * @return the creator's username
     */
    public String getCreatedBy() {
        return createdBy;
    }
    
    /**
     * Builder for creating new WorkItemDependency instances.
     */
    public static class Builder {
        private UUID id;
        private UUID dependentId;
        private UUID dependencyId;
        private String dependencyType = "BLOCKS"; // Default type
        private LocalDateTime createdAt;
        private String createdBy;
        
        /**
         * Creates a new Builder with a randomly generated ID and the current timestamp.
         */
        public Builder() {
            this.id = UUID.randomUUID();
            this.createdAt = LocalDateTime.now();
        }
        
        /**
         * Sets the ID of the work item that depends on another.
         *
         * @param dependentId the dependent work item ID
         * @return this builder for method chaining
         */
        public Builder dependentId(UUID dependentId) {
            this.dependentId = dependentId;
            return this;
        }
        
        /**
         * Sets the ID of the work item that must be completed first.
         *
         * @param dependencyId the dependency work item ID
         * @return this builder for method chaining
         */
        public Builder dependencyId(UUID dependencyId) {
            this.dependencyId = dependencyId;
            return this;
        }
        
        /**
         * Sets the type of dependency.
         *
         * @param dependencyType the dependency type
         * @return this builder for method chaining
         */
        public Builder dependencyType(String dependencyType) {
            this.dependencyType = dependencyType;
            return this;
        }
        
        /**
         * Sets the user who created the dependency.
         *
         * @param createdBy the creator's username
         * @return this builder for method chaining
         */
        public Builder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        
        /**
         * Creates a new WorkItemDependency with the attributes set on this builder.
         *
         * @return a new WorkItemDependency
         * @throws IllegalStateException if dependentId or dependencyId is null
         */
        public WorkItemDependency build() {
            if (dependentId == null) {
                throw new IllegalStateException("Dependent work item ID is required");
            }
            if (dependencyId == null) {
                throw new IllegalStateException("Dependency work item ID is required");
            }
            
            return new WorkItemDependency(id, dependentId, dependencyId, dependencyType, createdAt, createdBy);
        }
    }
}