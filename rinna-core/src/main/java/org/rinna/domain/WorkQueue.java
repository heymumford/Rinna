/*
 * Domain entity for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Represents a queue of work items in the Rinna system.
 * This is a core entity in the domain model for work prioritization.
 */
public interface WorkQueue {
    /**
     * Returns the unique identifier for this work queue.
     * 
     * @return the UUID of this work queue
     */
    UUID getId();
    
    /**
     * Returns the name of this work queue.
     * 
     * @return the name
     */
    String getName();
    
    /**
     * Returns the description of this work queue.
     * 
     * @return the description
     */
    String getDescription();
    
    /**
     * Returns whether this queue is active.
     * 
     * @return true if active, false otherwise
     */
    boolean isActive();
    
    /**
     * Returns all work items in this queue in priority order.
     * 
     * @return the list of work items
     */
    List<WorkItem> getItems();
    
    /**
     * Returns the work items in this queue filtered by type.
     * 
     * @param type the type to filter by
     * @return the filtered list of work items
     */
    List<WorkItem> getItemsByType(WorkItemType type);
    
    /**
     * Returns the work items in this queue filtered by state.
     * 
     * @param state the state to filter by
     * @return the filtered list of work items
     */
    List<WorkItem> getItemsByState(WorkflowState state);
    
    /**
     * Returns the work items in this queue filtered by priority.
     * 
     * @param priority the priority to filter by
     * @return the filtered list of work items
     */
    List<WorkItem> getItemsByPriority(Priority priority);
    
    /**
     * Returns the work items in this queue filtered by assignee.
     * 
     * @param assignee the assignee to filter by
     * @return the filtered list of work items
     */
    List<WorkItem> getItemsByAssignee(String assignee);
    
    /**
     * Returns the next work item to be worked on.
     * 
     * @return an Optional containing the next work item, or empty if the queue is empty
     */
    Optional<WorkItem> getNextItem();
    
    /**
     * Adds a work item to this queue.
     * 
     * @param item the work item to add
     */
    void addItem(WorkItem item);
    
    /**
     * Removes a work item from this queue.
     * 
     * @param itemId the ID of the work item to remove
     * @return true if the item was removed, false if it wasn't in the queue
     */
    boolean removeItem(UUID itemId);
    
    /**
     * Reprioritizes the queue based on the default prioritization algorithm.
     * This may change the order of items in the queue.
     */
    void reprioritize();
    
    /**
     * Returns the number of items in this queue.
     * 
     * @return the number of items
     */
    int size();
    
    /**
     * Returns whether this queue is empty.
     * 
     * @return true if empty, false otherwise
     */
    boolean isEmpty();
}