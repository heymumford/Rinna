/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemCreateRequest;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Mock implementation of item service functionality for CLI use.
 */
public class MockItemService implements ItemService {
    
    private final List<WorkItem> items = new ArrayList<>();
    
    /**
     * Constructor initializing some sample work items.
     */
    public MockItemService() {
        // Add some sample items
        WorkItem item1 = new WorkItem();
        item1.setId("123e4567-e89b-12d3-a456-426614174000");
        item1.setTitle("Implement authentication feature");
        item1.setDescription("Create JWT-based authentication for API endpoints");
        item1.setType(WorkItemType.TASK);
        item1.setPriority(Priority.HIGH);
        item1.setStatus(WorkflowState.IN_PROGRESS);
        item1.setAssignee(System.getProperty("user.name"));
        item1.setProject("DEMO");
        item1.setCreated(LocalDateTime.now().minusDays(5));
        item1.setUpdated(LocalDateTime.now().minusHours(2));
        items.add(item1);
        
        WorkItem item2 = new WorkItem();
        item2.setId("223e4567-e89b-12d3-a456-426614174001");
        item2.setTitle("Fix bug in payment module");
        item2.setDescription("Transaction history is not updating after payment completion");
        item2.setType(WorkItemType.BUG);
        item2.setPriority(Priority.CRITICAL);
        item2.setStatus(WorkflowState.READY);
        item2.setProject("DEMO");
        item2.setCreated(LocalDateTime.now().minusDays(2));
        item2.setUpdated(LocalDateTime.now().minusDays(2));
        items.add(item2);
        
        WorkItem item3 = new WorkItem();
        item3.setId("323e4567-e89b-12d3-a456-426614174002");
        item3.setTitle("Update documentation");
        item3.setDescription("Add API reference documentation for new endpoints");
        item3.setType(WorkItemType.TASK);
        item3.setPriority(Priority.LOW);
        item3.setStatus(WorkflowState.DONE);
        item3.setAssignee("jane");
        item3.setProject("DOCS");
        item3.setCreated(LocalDateTime.now().minusDays(10));
        item3.setUpdated(LocalDateTime.now().minusDays(1));
        items.add(item3);
    }
    
    /**
     * Gets all work items.
     *
     * @return a list of all work items
     */
    public List<WorkItem> getAllItems() {
        return new ArrayList<>(items);
    }
    
    /**
     * Gets a specific work item by ID.
     *
     * @param id the work item ID
     * @return the work item, or null if not found
     */
    public WorkItem getItem(String id) {
        return items.stream()
            .filter(item -> id.equals(item.getId()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Creates a new work item.
     *
     * @param item the work item to create
     * @return the created work item
     */
    public WorkItem createItem(WorkItem item) {
        // Ensure the item has an ID
        if (item.getId() == null) {
            item.setId(UUID.randomUUID().toString());
        }
        
        // Set creation and update timestamps
        item.setCreated(LocalDateTime.now());
        item.setUpdated(LocalDateTime.now());
        
        // Add to our list
        items.add(item);
        
        return item;
    }
    
    /**
     * Updates an existing work item.
     *
     * @param item the work item to update
     * @return the updated work item, or null if not found
     */
    public WorkItem updateItem(WorkItem item) {
        // Find the existing item
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(item.getId())) {
                index = i;
                break;
            }
        }
        
        if (index < 0) {
            // Item not found
            return null;
        }
        
        // Get the existing item
        WorkItem existingItem = items.get(index);
        
        // Preserve the creation date
        item.setCreated(existingItem.getCreated());
        
        // Update the modified date
        item.setUpdated(LocalDateTime.now());
        
        // Replace the item in our list
        items.set(index, item);
        
        return item;
    }
    
    /**
     * Deletes a work item.
     *
     * @param id the ID of the work item to delete
     * @return true if deleted, false if not found
     */
    public boolean deleteItem(String id) {
        // Find the existing item
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(id)) {
                index = i;
                break;
            }
        }
        
        if (index < 0) {
            // Item not found
            return false;
        }
        
        // Remove the item
        items.remove(index);
        return true;
    }
    
    /**
     * Finds work items by type.
     * 
     * @param type the work item type
     * @return a list of matching work items
     */
    public List<WorkItem> findByType(WorkItemType type) {
        List<WorkItem> result = new ArrayList<>();
        for (WorkItem item : items) {
            if (type.equals(item.getType())) {
                result.add(item);
            }
        }
        return result;
    }
    
    /**
     * Finds work items by status.
     * 
     * @param status the workflow status
     * @return a list of matching work items
     */
    public List<WorkItem> findByStatus(WorkflowState status) {
        List<WorkItem> result = new ArrayList<>();
        for (WorkItem item : items) {
            if (status.equals(item.getStatus())) {
                result.add(item);
            }
        }
        return result;
    }
    
    /**
     * Finds work items by state (alias for findByStatus).
     * 
     * @param state the workflow state
     * @return a list of matching work items
     */
    public List<WorkItem> findByState(WorkflowState state) {
        return findByStatus(state);
    }
    
    /**
     * Finds work items by assignee.
     * 
     * @param assignee the assignee
     * @return a list of matching work items
     */
    public List<WorkItem> findByAssignee(String assignee) {
        List<WorkItem> result = new ArrayList<>();
        for (WorkItem item : items) {
            if (assignee.equals(item.getAssignee())) {
                result.add(item);
            }
        }
        return result;
    }
    
    /**
     * Updates the assignee of a work item.
     * 
     * @param id the work item ID
     * @param assignee the new assignee
     * @return the updated work item, or null if not found
     */
    public WorkItem updateAssignee(String id, String assignee) {
        WorkItem item = getItem(id);
        if (item != null) {
            item.setAssignee(assignee);
            item.setUpdated(LocalDateTime.now());
            return item;
        }
        return null;
    }
    
    /**
     * Checks if a work item exists.
     * 
     * @param id the work item ID
     * @return true if the work item exists, false otherwise
     */
    public boolean exists(String id) {
        return items.stream().anyMatch(item -> id.equals(item.getId()));
    }
    
    /**
     * Finds a work item by its short ID (e.g., "WI-123").
     * 
     * @param shortId the short ID of the work item
     * @return the work item, or null if not found
     */
    public WorkItem findItemByShortId(String shortId) {
        // Parse the prefix and number from the short ID
        if (shortId == null || !shortId.contains("-")) {
            return null;
        }
        
        String[] parts = shortId.split("-", 2);
        if (parts.length != 2) {
            return null;
        }
        
        String prefix = parts[0];
        String numberPart = parts[1];
        
        // Try to find a matching item by prefix and ID ending
        for (WorkItem item : items) {
            String id = item.getId();
            // Check if the item has the right prefix in the type
            boolean prefixMatch = false;
            
            if (item.getType() != null) {
                String typePrefix = item.getType().toString().substring(0, Math.min(item.getType().toString().length(), prefix.length()));
                prefixMatch = typePrefix.equalsIgnoreCase(prefix);
            }
            
            // Check if the item ID ends with the numeric part
            if (prefixMatch && id != null && id.endsWith(numberPart)) {
                return item;
            }
            
            // Also check if the item has a key field that matches exactly
            if (item.getProject() != null && (item.getProject() + "-" + numberPart).equalsIgnoreCase(shortId)) {
                return item;
            }
        }
        
        return null;
    }
    
    /**
     * Updates metadata for a work item.
     * 
     * @param id the work item ID
     * @param metadata the metadata to update
     * @return true if updated, false if not found
     */
    public boolean updateMetadata(String id, Map<String, String> metadata) {
        WorkItem item = getItem(id);
        if (item != null) {
            if (metadata.containsKey("project")) {
                item.setProject(metadata.get("project"));
            }
            // Handle other metadata fields as needed
            item.setUpdated(LocalDateTime.now());
            return true;
        }
        return false;
    }
    
    /**
     * Updates custom fields (metadata) for a work item.
     * 
     * @param id the work item ID
     * @param customFields map of custom field names to values
     * @return the updated work item
     */
    @Override
    public WorkItem updateCustomFields(String id, Map<String, String> customFields) {
        WorkItem item = getItem(id);
        if (item != null) {
            // The WorkItem class doesn't directly support arbitrary custom fields,
            // so we'll map known fields to actual properties and store the rest
            // in a custom fields map in the future.
            
            // Handle known fields
            if (customFields.containsKey("project")) {
                item.setProject(customFields.get("project"));
            }
            if (customFields.containsKey("version")) {
                item.setVersion(customFields.get("version"));
            }
            if (customFields.containsKey("reporter")) {
                item.setReporter(customFields.get("reporter"));
            }
            
            // Set last update time
            item.setUpdated(LocalDateTime.now());
            
            // Add custom fields code here when the model supports it
            
            return item;
        }
        return null;
    }
    
    /**
     * Get all work items.
     * 
     * @return a list of all work items
     */
    public List<WorkItem> getAllWorkItems() {
        return new ArrayList<>(items);
    }
    
    /**
     * Creates a new work item from a request object.
     *
     * @param request the work item create request
     * @return the created work item
     */
    @Override
    public WorkItem createWorkItem(WorkItemCreateRequest request) {
        // Create a new work item from the request
        WorkItem item = new WorkItem();
        item.setId(UUID.randomUUID().toString());
        item.setTitle(request.getTitle());
        item.setDescription(request.getDescription());
        item.setType(request.getType());
        item.setPriority(request.getPriority());
        item.setProject(request.getProject());
        item.setAssignee(request.getAssignee());
        item.setCreated(LocalDateTime.now());
        item.setUpdated(LocalDateTime.now());
        item.setStatus(WorkflowState.CREATED); // Default status for new items
        
        // Add to our items list
        items.add(item);
        
        return item;
    }
    
    /**
     * Update the title of a work item.
     * 
     * @param id the work item ID
     * @param title the new title
     * @param user the user making the change
     * @return the updated work item, or null if not found
     */
    public WorkItem updateTitle(UUID id, String title, String user) {
        WorkItem item = getItem(id.toString());
        if (item != null) {
            item.setTitle(title);
            item.setUpdated(LocalDateTime.now());
            return item;
        }
        return null;
    }
    
    /**
     * Update the description of a work item.
     * 
     * @param id the work item ID
     * @param description the new description
     * @param user the user making the change
     * @return the updated work item, or null if not found
     */
    public WorkItem updateDescription(UUID id, String description, String user) {
        WorkItem item = getItem(id.toString());
        if (item != null) {
            item.setDescription(description);
            item.setUpdated(LocalDateTime.now());
            return item;
        }
        return null;
    }
    
    /**
     * Update the priority of a work item.
     * 
     * @param id the work item ID
     * @param priority the new priority
     * @param user the user making the change
     * @return the updated work item, or null if not found
     */
    public WorkItem updatePriority(UUID id, Priority priority, String user) {
        WorkItem item = getItem(id.toString());
        if (item != null) {
            item.setPriority(priority);
            item.setUpdated(LocalDateTime.now());
            return item;
        }
        return null;
    }
    
    /**
     * Update a generic field of a work item.
     * 
     * @param id the work item ID
     * @param field the field name to update
     * @param value the new value
     * @param user the user making the change
     * @return the updated work item, or null if not found
     */
    public WorkItem updateField(UUID id, String field, String value, String user) {
        WorkItem item = getItem(id.toString());
        if (item != null) {
            switch (field.toLowerCase()) {
                case "title":
                    item.setTitle(value);
                    break;
                case "description":
                    item.setDescription(value);
                    break;
                case "version":
                    item.setVersion(value);
                    break;
                case "project":
                    item.setProject(value);
                    break;
                case "reporter":
                    item.setReporter(value);
                    break;
                // Add other fields as needed
                default:
                    throw new IllegalArgumentException("Unsupported field: " + field);
            }
            item.setUpdated(LocalDateTime.now());
            return item;
        }
        return null;
    }
    
    /**
     * Update the state of a work item.
     * 
     * @param id the work item ID
     * @param state the new state
     * @param user the user making the change
     * @return the updated work item, or null if not found
     */
    public WorkItem updateState(UUID id, WorkflowState state, String user) {
        WorkItem item = getItem(id.toString());
        if (item != null) {
            item.setStatus(state);
            item.setUpdated(LocalDateTime.now());
            return item;
        }
        return null;
    }
    
    /**
     * Assign a work item to a user.
     * 
     * @param id the work item ID
     * @param assignee the new assignee
     * @param user the user making the change
     * @return the updated work item, or null if not found
     */
    public WorkItem assignTo(UUID id, String assignee, String user) {
        WorkItem item = getItem(id.toString());
        if (item != null) {
            item.setAssignee(assignee);
            item.setUpdated(LocalDateTime.now());
            return item;
        }
        return null;
    }
}