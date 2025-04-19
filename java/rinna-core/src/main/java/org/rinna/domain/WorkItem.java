/*
 * Domain entity for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a work item in the Rinna system.
 * This is a core entity in the domain model.
 */
public interface WorkItem {
    /**
     * Returns the unique identifier for this work item.
     * 
     * @return the UUID of this work item
     */
    UUID getId();
    
    /**
     * Returns the title of this work item.
     * 
     * @return the title
     */
    String getTitle();
    
    /**
     * Returns the description of this work item.
     * 
     * @return the description
     */
    String getDescription();
    
    /**
     * Returns the type of this work item.
     * 
     * @return the work item type
     */
    WorkItemType getType();
    
    /**
     * Returns the current workflow state of this work item.
     * 
     * @return the workflow state
     */
    WorkflowState getStatus();
    
    /**
     * Returns the priority of this work item.
     * 
     * @return the priority
     */
    Priority getPriority();
    
    /**
     * Returns the assignee of this work item.
     * 
     * @return the assignee
     */
    String getAssignee();
    
    /**
     * Returns the creation timestamp of this work item.
     * 
     * @return the creation timestamp
     */
    Instant getCreatedAt();
    
    /**
     * Returns the last update timestamp of this work item.
     * 
     * @return the last update timestamp
     */
    Instant getUpdatedAt();
    
    /**
     * Returns the parent ID of this work item if it has a parent.
     * 
     * @return an Optional containing the parent ID, or empty if no parent
     */
    Optional<UUID> getParentId();
    
    /**
     * Returns the project ID of this work item.
     * 
     * @return an Optional containing the project ID, or empty if not associated with a project
     */
    Optional<UUID> getProjectId();
    
    /**
     * Returns the visibility status of this work item.
     * Visibility can be PUBLIC, TEAM, or PRIVATE.
     * 
     * @return the visibility status
     */
    String getVisibility();
    
    /**
     * Returns whether this work item is local only.
     * Local-only items are created by the local client and not yet synchronized.
     * 
     * @return true if the item is local only, false otherwise
     */
    boolean isLocalOnly();
}