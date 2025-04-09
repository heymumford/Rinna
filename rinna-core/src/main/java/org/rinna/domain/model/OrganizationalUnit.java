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
import java.util.Optional;
import java.util.UUID;

/**
 * Represents an organizational unit in the Rinna system.
 * An organizational unit is a group of people working together, which could be
 * a department, team, or any other organizational structure.
 */
public interface OrganizationalUnit {
    /**
     * Returns the unique identifier for this organizational unit.
     * 
     * @return the UUID of this organizational unit
     */
    UUID getId();
    
    /**
     * Returns the name of this organizational unit.
     * 
     * @return the name
     */
    String getName();
    
    /**
     * Returns the description of this organizational unit.
     * 
     * @return the description
     */
    String getDescription();
    
    /**
     * Returns the type of this organizational unit.
     * 
     * @return the type
     */
    OrganizationalUnitType getType();
    
    /**
     * Returns the parent organizational unit, if any.
     * 
     * @return an Optional containing the parent organizational unit ID, or empty if none
     */
    Optional<UUID> getParentId();
    
    /**
     * Returns the owner or manager of this organizational unit.
     * 
     * @return the owner
     */
    String getOwner();
    
    /**
     * Returns the creation timestamp of this organizational unit.
     * 
     * @return the creation timestamp
     */
    Instant getCreatedAt();
    
    /**
     * Returns the last update timestamp of this organizational unit.
     * 
     * @return the last update timestamp
     */
    Instant getUpdatedAt();
    
    /**
     * Returns the cognitive capacity of this organizational unit.
     * This represents the total cognitive load capacity of the unit.
     * 
     * @return the cognitive capacity
     */
    int getCognitiveCapacity();
    
    /**
     * Returns the current cognitive load of this organizational unit.
     * This is determined by the total cognitive load of all work items assigned to the unit.
     * 
     * @return the current cognitive load
     */
    int getCurrentCognitiveLoad();
    
    /**
     * Returns the members of this organizational unit.
     * 
     * @return a list of member IDs
     */
    List<String> getMembers();
    
    /**
     * Returns whether this organizational unit is active.
     * 
     * @return true if active, false otherwise
     */
    boolean isActive();
    
    /**
     * Returns the CYNEFIN domain expertise of this organizational unit.
     * This represents the domains in which this unit specializes.
     * 
     * @return a list of CYNEFIN domains
     */
    List<CynefinDomain> getDomainExpertise();
    
    /**
     * Returns the work paradigms this organizational unit is proficient in.
     * 
     * @return a list of work paradigms
     */
    List<WorkParadigm> getWorkParadigms();
    
    /**
     * Returns the tags associated with this organizational unit.
     * 
     * @return a list of tags
     */
    List<String> getTags();
}