/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.domain.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.rinna.cli.domain.model.DomainPriority;
import org.rinna.cli.domain.model.DomainWorkItem;
import org.rinna.cli.domain.model.DomainWorkItemType;

/**
 * Service interface for managing work items.
 * This interface defines the application use cases for work items.
 */
public interface ItemService {
    /**
     * Creates a new work item.
     * 
     * @param id the ID of the new work item
     * @param title the title of the new work item
     * @param type the type of the new work item
     * @param priority the priority of the new work item
     * @param description the description of the new work item
     * @return the created work item
     */
    DomainWorkItem create(UUID id, String title, DomainWorkItemType type, DomainPriority priority, String description);
    
    /**
     * Finds a work item by its ID.
     * 
     * @param id the ID of the work item
     * @return an Optional containing the work item, or empty if not found
     */
    Optional<DomainWorkItem> findById(UUID id);
    
    /**
     * Finds all work items.
     * 
     * @return a list of all work items
     */
    List<DomainWorkItem> findAll();
    
    /**
     * Finds work items by their type.
     * 
     * @param type the type of work items to find
     * @return a list of work items of the given type
     */
    List<DomainWorkItem> findByType(DomainWorkItemType type);
    
    /**
     * Finds work items by their assignee.
     * 
     * @param assignee the assignee of work items to find
     * @return a list of work items assigned to the given assignee
     */
    List<DomainWorkItem> findByAssignee(String assignee);
    
    /**
     * Updates the assignee of a work item.
     * 
     * @param id the ID of the work item
     * @param assignee the new assignee
     * @return the updated work item
     */
    DomainWorkItem updateAssignee(UUID id, String assignee);
    
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
}