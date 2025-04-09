/*
 * Model class for the Rinna workflow management system
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
 * Represents a work item in the Rinna system.
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
}