/*
 * Domain service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.service;

import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;

import java.util.List;
import java.util.Map;
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
    
    /**
     * Checks if a work item exists by its ID.
     *
     * @param id the work item ID
     * @return true if the work item exists
     */
    boolean existsById(UUID id);
    
    /**
     * Updates metadata for a work item.
     *
     * @param id the work item ID
     * @param metadata the metadata to update
     * @return true if the metadata was updated successfully
     */
    boolean updateMetadata(UUID id, Map<String, String> metadata);
    
    /**
     * Creates a new work item with basic information.
     *
     * @param title the work item title
     * @param type the work item type
     * @param priority the work item priority
     * @return the created work item
     */
    default WorkItem createWorkItem(String title, WorkItemType type, Priority priority) {
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
            .title(title)
            .type(type)
            .priority(priority)
            .build();
        return create(request);
    }
    
    /**
     * Finds work items by type.
     *
     * @param type the work item type
     * @return list of work items of the specified type
     */
    default List<WorkItem> findByType(WorkItemType type) {
        return findByType(type.name());
    }
}