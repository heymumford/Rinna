/*
 * Domain entity for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Represents a release in the Rinna system.
 * This is a core entity in the domain model.
 */
public interface Release {
    /**
     * Returns the unique identifier for this release.
     * 
     * @return the UUID of this release
     */
    UUID getId();
    
    /**
     * Returns the version of this release.
     * 
     * @return the version
     */
    String getVersion();
    
    /**
     * Returns the description of this release.
     * 
     * @return the description
     */
    String getDescription();
    
    /**
     * Returns the creation timestamp of this release.
     * 
     * @return the creation timestamp
     */
    Instant getCreatedAt();
    
    /**
     * Returns the list of work items associated with this release.
     * 
     * @return the list of work items
     */
    List<UUID> getWorkItems();
}