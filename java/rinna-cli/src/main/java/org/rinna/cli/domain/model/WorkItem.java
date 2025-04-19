/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a work item in the workflow system.
 */
public interface WorkItem {
    /**
     * Gets the ID of the work item.
     *
     * @return the work item ID
     */
    UUID id();
    
    /**
     * Gets the title of the work item.
     *
     * @return the title
     */
    String title();
    
    /**
     * Gets the description of the work item.
     *
     * @return the description
     */
    String description();
    
    /**
     * Gets the type of the work item.
     *
     * @return the type
     */
    WorkItemType type();
    
    /**
     * Gets the priority of the work item.
     *
     * @return the priority
     */
    Priority priority();
    
    /**
     * Gets the state of the work item.
     *
     * @return the state
     */
    WorkflowState state();
    
    /**
     * Gets the assignee of the work item.
     *
     * @return the assignee
     */
    String assignee();
    
    /**
     * Gets the reporter of the work item.
     *
     * @return the reporter
     */
    String reporter();
    
    /**
     * Gets the creation timestamp of the work item.
     *
     * @return the creation timestamp
     */
    Instant createdAt();
    
    /**
     * Gets the last update timestamp of the work item.
     *
     * @return the last update timestamp
     */
    Instant updatedAt();
}