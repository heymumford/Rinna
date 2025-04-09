/*
 * Domain entity for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a workstream in the Rinna system.
 * A workstream is a cross-cutting collection of work items that deliver
 * a specific business capability across multiple projects.
 */
public interface Workstream {
    /**
     * Returns the unique identifier for this workstream.
     * 
     * @return the UUID of this workstream
     */
    UUID getId();
    
    /**
     * Returns the name of this workstream.
     * 
     * @return the name
     */
    String getName();
    
    /**
     * Returns the description of this workstream.
     * 
     * @return the description
     */
    String getDescription();
    
    /**
     * Returns the owner of this workstream.
     * 
     * @return the owner (typically a Product Owner)
     */
    String getOwner();
    
    /**
     * Returns the current status of this workstream.
     * 
     * @return the status (e.g., DRAFT, ACTIVE, COMPLETED)
     */
    String getStatus();
    
    /**
     * Returns the priority level of this workstream.
     * 
     * @return the priority level
     */
    Priority getPriority();
    
    /**
     * Returns the creation timestamp of this workstream.
     * 
     * @return the creation timestamp
     */
    Instant getCreatedAt();
    
    /**
     * Returns the last update timestamp of this workstream.
     * 
     * @return the last update timestamp
     */
    Instant getUpdatedAt();
    
    /**
     * Returns the organization unit this workstream belongs to.
     * 
     * @return an Optional containing the organization unit ID
     */
    Optional<UUID> getOrganizationId();
    
    /**
     * Returns the CYNEFIN domain that characterizes this workstream.
     *
     * @return an Optional containing the CYNEFIN domain
     */
    Optional<CynefinDomain> getCynefinDomain();
    
    /**
     * Returns whether this workstream spans multiple projects.
     *
     * @return true if this workstream spans multiple projects
     */
    boolean isCrossProject();
    
    /**
     * Returns the expected completion date for this workstream.
     *
     * @return an Optional containing the target completion date
     */
    Optional<Instant> getTargetDate();
}