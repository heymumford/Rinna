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
 * Represents a webhook configuration in the Rinna system.
 */
public class WebhookConfig {
    private final UUID id;
    private final UUID projectId;
    private final String source;
    private final String secret;
    private final String description;
    private final Instant createdAt;
    private final boolean active;
    
    /**
     * Creates a new WebhookConfig instance.
     *
     * @param id the unique identifier
     * @param projectId the associated project ID
     * @param source the webhook source (e.g., "github", "gitlab")
     * @param secret the webhook secret for verification
     * @param description the webhook description
     * @param createdAt the creation timestamp
     * @param active whether the webhook is active
     */
    public WebhookConfig(UUID id, UUID projectId, String source, String secret,
                       String description, Instant createdAt, boolean active) {
        this.id = id;
        this.projectId = projectId;
        this.source = source;
        this.secret = secret;
        this.description = description;
        this.createdAt = createdAt;
        this.active = active;
    }
    
    /**
     * Creates a new active WebhookConfig with a random ID and current timestamp.
     *
     * @param projectId the associated project ID
     * @param source the webhook source
     * @param secret the webhook secret
     * @param description the webhook description
     */
    public WebhookConfig(UUID projectId, String source, String secret, String description) {
        this(UUID.randomUUID(), projectId, source, secret, description, Instant.now(), true);
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
     * Returns the associated project ID.
     *
     * @return the project ID
     */
    public UUID getProjectId() {
        return projectId;
    }
    
    /**
     * Returns the webhook source.
     *
     * @return the source
     */
    public String getSource() {
        return source;
    }
    
    /**
     * Returns the webhook secret.
     *
     * @return the secret
     */
    public String getSecret() {
        return secret;
    }
    
    /**
     * Returns the webhook description.
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
     * Returns whether the webhook is active.
     *
     * @return true if active, false otherwise
     */
    public boolean isActive() {
        return active;
    }
}