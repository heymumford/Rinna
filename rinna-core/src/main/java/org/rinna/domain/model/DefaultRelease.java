/*
 * Domain entity implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Default implementation of the Release interface.
 */
public final class DefaultRelease implements Release {
    private final UUID id;
    private final String version;
    private final String description;
    private final Instant createdAt;
    private final List<UUID> workItems;
    
    /**
     * Private constructor for the Builder pattern.
     *
     * @param builder the builder
     */
    private DefaultRelease(Builder builder) {
        this.id = builder.id;
        this.version = builder.version;
        this.description = builder.description;
        this.createdAt = builder.createdAt;
        this.workItems = new ArrayList<>(builder.workItems);
    }
    
    @Override
    public UUID getId() {
        return id;
    }
    
    @Override
    public String getVersion() {
        return version;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    @Override
    public List<UUID> getWorkItems() {
        return Collections.unmodifiableList(workItems);
    }
    
    /**
     * Builder for DefaultRelease.
     */
    public static class Builder {
        private UUID id = UUID.randomUUID();
        private String version;
        private String description;
        private Instant createdAt = Instant.now();
        private final List<UUID> workItems = new ArrayList<>();
        
        /**
         * Sets the ID.
         *
         * @param id the ID
         * @return this builder
         */
        public Builder id(UUID id) {
            this.id = id;
            return this;
        }
        
        /**
         * Sets the version.
         *
         * @param version the version
         * @return this builder
         */
        public Builder version(String version) {
            this.version = version;
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
         * Sets the creation timestamp.
         *
         * @param createdAt the creation timestamp
         * @return this builder
         */
        public Builder createdAt(Instant createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        /**
         * Adds a work item.
         *
         * @param workItemId the work item ID
         * @return this builder
         */
        public Builder addWorkItem(UUID workItemId) {
            this.workItems.add(workItemId);
            return this;
        }
        
        /**
         * Sets the work items.
         *
         * @param workItems the work items
         * @return this builder
         */
        public Builder workItems(List<UUID> workItems) {
            this.workItems.clear();
            if (workItems != null) {
                this.workItems.addAll(workItems);
            }
            return this;
        }
        
        /**
         * Builds a new DefaultRelease.
         *
         * @return a new DefaultRelease
         */
        public DefaultRelease build() {
            if (version == null || version.isEmpty()) {
                throw new IllegalArgumentException("Version cannot be null or empty");
            }
            
            // Validate semantic versioning format (major.minor.patch)
            if (!version.matches("^\\d+\\.\\d+\\.\\d+$")) {
                throw new IllegalArgumentException("Version must follow semantic versioning format (major.minor.patch)");
            }
            
            return new DefaultRelease(this);
        }
    }
}