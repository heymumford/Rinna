/*
 * Domain service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.usecase;

import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Service interface for managing work items.
 * This interface defines the application use cases for work items.
 */
public interface ItemService {
    /**
     * Creates a new work item.
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
     * Updates the assignee of a work item.
     * 
     * @param id the ID of the work item
     * @param assignee the new assignee
     * @return the updated work item
     */
    WorkItem updateAssignee(UUID id, String assignee);
    
    /**
     * Deletes a work item by its ID.
     * 
     * @param id the ID of the work item to delete
     */
    void deleteById(UUID id);
}