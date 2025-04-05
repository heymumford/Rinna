/*
 * Domain entity for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a project in the Rinna system.
 * Projects are containers for work items and help organize work across different codebases or products.
 */
public interface Project {
    
    /**
     * Returns the unique identifier for this project.
     *
     * @return the project ID
     */
    UUID getId();
    
    /**
     * Returns the unique key for this project.
     * The key is a human-readable identifier used in URLs and references.
     *
     * @return the project key
     */
    String getKey();
    
    /**
     * Returns the name of this project.
     *
     * @return the project name
     */
    String getName();
    
    /**
     * Returns the description of this project.
     *
     * @return the project description
     */
    String getDescription();
    
    /**
     * Returns the creation timestamp of this project.
     *
     * @return the creation timestamp
     */
    Instant getCreatedAt();
    
    /**
     * Returns the last update timestamp of this project.
     *
     * @return the last update timestamp
     */
    Instant getUpdatedAt();
    
    /**
     * Returns whether this project is active.
     *
     * @return true if the project is active, false otherwise
     */
    boolean isActive();
    
    /**
     * Builder for creating Project instances.
     */
    interface Builder {
        
        /**
         * Sets the project key.
         *
         * @param key the project key
         * @return this builder
         */
        Builder key(String key);
        
        /**
         * Sets the project name.
         *
         * @param name the project name
         * @return this builder
         */
        Builder name(String name);
        
        /**
         * Sets the project description.
         *
         * @param description the project description
         * @return this builder
         */
        Builder description(String description);
        
        /**
         * Sets whether the project is active.
         *
         * @param active true if the project is active, false otherwise
         * @return this builder
         */
        Builder active(boolean active);
        
        /**
         * Builds and returns a new Project instance.
         *
         * @return a new Project instance
         */
        Project build();
    }
}