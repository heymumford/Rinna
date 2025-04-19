/*
 * Model class for the Rinna workflow management system
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

    /**
     * Returns the unique identifier for this work item.
     * Alias for getId() to support record-style access.
     * 
     * @return the UUID of this work item
     */
    default UUID id() {
        return getId();
    }

    /**
     * Returns the title of this work item.
     * Alias for getTitle() to support record-style access.
     * 
     * @return the title
     */
    default String title() {
        return getTitle();
    }

    /**
     * Returns the type of this work item.
     * Alias for getType() to support record-style access.
     * 
     * @return the work item type
     */
    default WorkItemType type() {
        return getType();
    }

    /**
     * Checks if this work item is completed.
     * 
     * @return true if the work item is completed, false otherwise
     */
    default boolean isCompleted() {
        return getStatus() == WorkflowState.DONE;
    }

    /**
     * Checks if this work item is cancelled.
     * 
     * @return true if the work item is cancelled, false otherwise
     */
    default boolean isCancelled() {
        // Since there's no CANCELLED state, we'll assume it's never cancelled
        return false;
    }

    /**
     * Checks if this work item is blocked.
     * 
     * @return true if the work item is blocked, false otherwise
     */
    default boolean isBlocked() {
        // Since there's no BLOCKED state, we'll assume it's never blocked
        return false;
    }

    /**
     * Returns the Cynefin domain of this work item.
     * 
     * @return the Cynefin domain
     */
    default CynefinDomain cynefinDomain() {
        return CynefinDomain.COMPLICATED; // Default value, should be overridden by implementations
    }

    /**
     * Returns the work paradigm of this work item.
     * 
     * @return the work paradigm
     */
    default WorkParadigm workParadigm() {
        return WorkParadigm.TASK; // Default value, should be overridden by implementations
    }

    /**
     * Returns the tags associated with this work item.
     * 
     * @return a list of tags
     */
    default List<String> tags() {
        return List.of(); // Default empty list, should be overridden by implementations
    }
}
