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
 * Interface for domain work items.
 */
public interface DomainWorkItem {
    
    /**
     * Gets the ID of the work item.
     *
     * @return the work item ID
     */
    UUID getId();
    
    /**
     * Gets the title of the work item.
     *
     * @return the title
     */
    String getTitle();
    
    /**
     * Gets the description of the work item.
     *
     * @return the description
     */
    String getDescription();
    
    /**
     * Gets the type of the work item.
     *
     * @return the work item type
     */
    DomainWorkItemType getType();
    
    /**
     * Gets the priority of the work item.
     *
     * @return the priority
     */
    DomainPriority getPriority();
    
    /**
     * Gets the state of the work item.
     *
     * @return the workflow state
     */
    DomainWorkflowState getState();
    
    /**
     * Gets the assignee of the work item.
     *
     * @return the assignee
     */
    String getAssignee();
    
    /**
     * Gets the reporter of the work item.
     *
     * @return the reporter
     */
    String getReporter();
    
    /**
     * Gets the creation timestamp of the work item.
     *
     * @return the creation timestamp
     */
    Instant getCreatedAt();
    
    /**
     * Gets the last update timestamp of the work item.
     *
     * @return the last update timestamp
     */
    Instant getUpdatedAt();
}
