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
 * Represents an API token for authentication in the Rinna system.
 */
public class APIToken {
    private final UUID id;
    private final String token;
    private final UUID projectId;
    private final String description;
    private final Instant createdAt;
    private final Instant expiresAt;
    private final boolean active;
    
    /**
     * Creates a new APIToken instance.
     *
     * @param id the unique identifier
     * @param token the token value
     * @param projectId the associated project ID
     * @param description the token description
     * @param createdAt the creation timestamp
     * @param expiresAt the expiration timestamp
     * @param active whether the token is active
     */
    public APIToken(UUID id, String token, UUID projectId, String description,
                   Instant createdAt, Instant expiresAt, boolean active) {
        this.id = id;
        this.token = token;
        this.projectId = projectId;
        this.description = description;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.active = active;
    }
    
    /**
     * Creates a new active APIToken with a random ID and current timestamp.
     *
     * @param token the token value
     * @param projectId the associated project ID
     * @param description the token description
     * @param expiresAt the expiration timestamp
     */
    public APIToken(String token, UUID projectId, String description, Instant expiresAt) {
        this(UUID.randomUUID(), token, projectId, description, Instant.now(), expiresAt, true);
    }
    
    /**
     * Returns the unique identifier.
     *
     * @return the ID
     */
    public UUID getId() {
        return id;
    }
    
    /**
     * Returns the token value.
     *
     * @return the token
     */
    public String getToken() {
        return token;
    }
    
    /**
     * Returns the associated project ID.
     *
     * @return the project ID
     */
    public UUID getProjectId() {
        return projectId;
    }
    
    /**
     * Returns the token description.
     *
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Returns the creation timestamp.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Returns the expiration timestamp.
     *
     * @return the expiration timestamp
     */
    public Instant getExpiresAt() {
        return expiresAt;
    }
    
    /**
     * Returns whether the token is active.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return active;
    }
    
    /**
     * Returns whether the token has expired.
     *
     * @return true if expired, false otherwise
     */
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
    
    /**
     * Returns whether the token is valid (active and not expired).
     *
     * @return true if valid, false otherwise
     */
    public boolean isValid() {
        return active && !isExpired();
    }
}