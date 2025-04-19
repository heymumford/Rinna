/*
 * Item service interface for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemCreateRequest;
import org.rinna.cli.model.WorkItemType;

/**
 * Service interface for work item operations.
 */
public interface ItemService {
    
    /**
     * Gets all work items.
     *
     * @return a list of all work items
     */
    List<WorkItem> getAllItems();
    
    /**
     * Gets a specific work item by ID.
     *
     * @param id the work item ID
     * @return the work item, or null if not found
     */
    WorkItem getItem(String id);
    
    /**
     * Creates a new work item.
     *
     * @param item the work item to create
     * @return the created work item
     */
    WorkItem createItem(WorkItem item);
    
    /**
     * Creates a new work item (alias for createItem).
     *
     * @param item the work item to create
     * @return the created work item
     */
    default WorkItem createWorkItem(WorkItem item) {
        return createItem(item);
    }
    
    /**
     * Creates a new work item from a request object.
     *
     * @param request the work item create request
     * @return the created work item
     */
    WorkItem createWorkItem(WorkItemCreateRequest request);
    
    /**
     * Finds work items by assignee.
     *
     * @param assignee the assignee username
     * @return a list of work items assigned to the specified user
     */
    List<WorkItem> findByAssignee(String assignee);
    
    /**
     * Finds work items by type.
     *
     * @param type the work item type
     * @return a list of work items with the specified type
     */
    List<WorkItem> findByType(WorkItemType type);
    
    /**
     * Gets all work items in the system.
     *
     * @return a list of all work items
     */
    List<WorkItem> getAllWorkItems();
    
    /**
     * Updates an existing work item.
     *
     * @param item the work item to update
     * @return the updated work item
     */
    WorkItem updateItem(WorkItem item);
    
    /**
     * Deletes a work item by ID.
     *
     * @param id the work item ID
     * @return true if successful, false otherwise
     */
    boolean deleteItem(String id);
    
    /**
     * Finds a work item by its short ID (e.g., "WI-123").
     * 
     * @param shortId the short ID of the work item
     * @return the work item, or null if not found
     */
    WorkItem findItemByShortId(String shortId);
    
    /**
     * Updates the title of a work item.
     * 
     * @param id the work item ID
     * @param title the new title
     * @param user the user making the change
     * @return the updated work item
     */
    WorkItem updateTitle(UUID id, String title, String user);
    
    /**
     * Updates the description of a work item.
     * 
     * @param id the work item ID
     * @param description the new description
     * @param user the user making the change
     * @return the updated work item
     */
    WorkItem updateDescription(UUID id, String description, String user);
    
    /**
     * Updates a generic field of a work item.
     * 
     * @param id the work item ID
     * @param field the field name to update
     * @param value the new value
     * @param user the user making the change
     * @return the updated work item
     */
    WorkItem updateField(UUID id, String field, String value, String user);
    
    /**
     * Assigns a work item to a user.
     * 
     * @param id the work item ID
     * @param assignee the new assignee
     * @param user the user making the change
     * @return the updated work item
     */
    WorkItem assignTo(UUID id, String assignee, String user);
    
    /**
     * Updates the priority of a work item.
     * 
     * @param id the work item ID
     * @param priority the new priority
     * @param user the user making the change
     * @return the updated work item
     */
    WorkItem updatePriority(UUID id, Priority priority, String user);
    
    /**
     * Updates custom fields (metadata) for a work item.
     * 
     * @param id the work item ID
     * @param customFields map of custom field names to values
     * @return the updated work item
     */
    WorkItem updateCustomFields(String id, Map<String, String> customFields);
    
    /**
     * Updates the assignee of a work item.
     * 
     * @param id the work item ID
     * @param assignee the new assignee
     * @return the updated work item
     */
    WorkItem updateAssignee(String id, String assignee);
}