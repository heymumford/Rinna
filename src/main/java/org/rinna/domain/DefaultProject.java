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
 * Default implementation of the Project interface.
 */
public class DefaultProject implements Project {
    private final UUID id;
    private final String key;
    private final String name;
    private final String description;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final boolean active;
    
    private DefaultProject(UUID id, String key, String name, String description, 
                         Instant createdAt, Instant updatedAt, boolean active) {
        this.id = id;
        this.key = key;
        this.name = name;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.active = active;
    }
    
    @Override
    public UUID getId() {
        return id;
    }
    
    @Override
    public String getKey() {
        return key;
    }
    
    @Override
    public String getName() {
        return name;
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
    public Instant getUpdatedAt() {
        return updatedAt;
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    /**
     * Default builder implementation for creating DefaultProject instances.
     */
    public static class Builder implements Project.Builder {
        private String key;
        private String name;
        private String description;
        private boolean active = true;
        
        @Override
        public Builder key(String key) {
            this.key = key;
            return this;
        }
        
        @Override
        public Builder name(String name) {
            this.name = name;
            return this;
        }
        
        @Override
        public Builder description(String description) {
            this.description = description;
            return this;
        }
        
        @Override
        public Builder active(boolean active) {
            this.active = active;
            return this;
        }
        
        @Override
        public Project build() {
            Instant now = Instant.now();
            return new DefaultProject(
                UUID.randomUUID(),
                key,
                name,
                description,
                now,
                now,
                active
            );
        }
    }
}