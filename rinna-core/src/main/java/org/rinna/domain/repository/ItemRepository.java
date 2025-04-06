/*
 * Domain repository interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.repository;

import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for work items.
 * This interface defines the persistence operations for work items.
 * It follows the Repository pattern from Domain-Driven Design.
 */
public interface ItemRepository {
    /**
     * Saves a work item.
     * 
     * @param item the work item to save
     * @return the saved work item
     */
    WorkItem save(WorkItem item);
    
    /**
     * Creates a new work item from a create request.
     * 
     * @param request the create request
     * @return the created work item
     */
    WorkItem create(WorkItemCreateRequest request);
    
    /**
     * Finds a work item by its ID.
     * 
     * @param id the ID of the work item
     * @return an Optional containing the work item, or empty if not found
     */
    Optional<WorkItem> findById(UUID id);
    
    /**
     * Finds all work items.
     * 
     * @return a list of all work items
     */
    List<WorkItem> findAll();
    
    /**
     * Finds work items by their type.
     * 
     * @param type the type of work items to find
     * @return a list of work items of the given type
     */
    List<WorkItem> findByType(String type);
    
    /**
     * Finds work items by their status.
     * 
     * @param status the status of work items to find
     * @return a list of work items with the given status
     */
    List<WorkItem> findByStatus(String status);
    
    /**
     * Finds work items by their assignee.
     * 
     * @param assignee the assignee of work items to find
     * @return a list of work items assigned to the given assignee
     */
    List<WorkItem> findByAssignee(String assignee);
    
    /**
     * Deletes a work item by its ID.
     * 
     * @param id the ID of the work item to delete
     */
    void deleteById(UUID id);
}