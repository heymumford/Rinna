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
 * Represents metadata for a work item in the Rinna system.
 * This is used to store arbitrary key-value pairs associated with a work item.
 */
public class WorkItemMetadata {
    private final UUID id;
    private final UUID workItemId;
    private final String key;
    private final String value;
    private final Instant createdAt;
    
    /**
     * Creates a new WorkItemMetadata instance.
     *
     * @param id the unique identifier
     * @param workItemId the ID of the associated work item
     * @param key the metadata key
     * @param value the metadata value
     * @param createdAt the creation timestamp
     */
    public WorkItemMetadata(UUID id, UUID workItemId, String key, String value, Instant createdAt) {
        this.id = id;
        this.workItemId = workItemId;
        this.key = key;
        this.value = value;
        this.createdAt = createdAt;
    }
    
    /**
     * Creates a new WorkItemMetadata instance with a generated ID and current timestamp.
     *
     * @param workItemId the ID of the associated work item
     * @param key the metadata key
     * @param value the metadata value
     */
    public WorkItemMetadata(UUID workItemId, String key, String value) {
        this(UUID.randomUUID(), workItemId, key, value, Instant.now());
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
     * Returns the ID of the associated work item.
     *
     * @return the work item ID
     */
    public UUID getWorkItemId() {
        return workItemId;
    }
    
    /**
     * Returns the metadata key.
     *
     * @return the key
     */
    public String getKey() {
        return key;
    }
    
    /**
     * Returns the metadata value.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }
    
    /**
     * Returns the creation timestamp.
     *
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
}