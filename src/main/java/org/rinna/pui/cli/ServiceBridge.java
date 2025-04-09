/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.pui.cli;

import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.MockSearchService;
import org.rinna.cli.service.MockMetadataService;
import org.rinna.cli.service.MockHistoryService;
import org.rinna.cli.service.MockBacklogService;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkflowState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Bridge between the PUI components and CLI services.
 * This class provides simplified access to CLI services for PUI components.
 */
public class ServiceBridge {
    
    private static ServiceBridge instance;
    
    private final ServiceManager serviceManager;
    private final MockItemService itemService;
    private final MockWorkflowService workflowService;
    private final MockSearchService searchService;
    private final MockMetadataService metadataService;
    private final MockHistoryService historyService;
    private final MockBacklogService backlogService;
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private ServiceBridge() {
        this.serviceManager = ServiceManager.getInstance();
        this.itemService = serviceManager.getMockItemService();
        this.workflowService = serviceManager.getMockWorkflowService();
        this.searchService = serviceManager.getMockSearchService();
        this.metadataService = serviceManager.getMetadataService();
        this.historyService = serviceManager.getMockHistoryService();
        this.backlogService = serviceManager.getMockBacklogService();
    }
    
    /**
     * Gets the singleton instance of ServiceBridge.
     * 
     * @return the singleton instance
     */
    public static synchronized ServiceBridge getInstance() {
        if (instance == null) {
            instance = new ServiceBridge();
        }
        return instance;
    }
    
    /**
     * Gets all work items.
     * 
     * @return a list of all work items
     */
    public List<WorkItem> getAllWorkItems() {
        // Start operation tracking
        String operationId = metadataService.startOperation("pui_bridge", "QUERY", new HashMap<>());
        
        try {
            List<WorkItem> items = itemService.getAllItems();
            metadataService.completeOperation(operationId, Map.of("count", items.size()));
            return items;
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            throw e;
        }
    }
    
    /**
     * Gets a work item by ID.
     * 
     * @param id the work item ID
     * @return the work item, or null if not found
     */
    public WorkItem getWorkItem(String id) {
        // Start operation tracking
        Map<String, Object> params = new HashMap<>();
        params.put("item_id", id);
        String operationId = metadataService.startOperation("pui_bridge", "GET", params);
        
        try {
            WorkItem item = itemService.getItem(id);
            metadataService.completeOperation(operationId, Map.of("found", item != null));
            return item;
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            throw e;
        }
    }
    
    /**
     * Creates a new work item.
     * 
     * @param title the work item title
     * @param description the work item description
     * @param type the work item type
     * @param priority the work item priority
     * @param assignee the assignee
     * @return the created work item
     */
    public WorkItem createWorkItem(String title, String description, WorkItemType type, 
                               Priority priority, String assignee) {
        // Start operation tracking
        Map<String, Object> params = new HashMap<>();
        params.put("title", title);
        params.put("description", truncateString(description, 50));
        params.put("type", type);
        params.put("priority", priority);
        params.put("assignee", assignee);
        
        String operationId = metadataService.startOperation("pui_bridge", "CREATE", params);
        
        try {
            WorkItem workItem = new WorkItem();
            workItem.setTitle(title);
            workItem.setDescription(description);
            workItem.setType(type);
            workItem.setPriority(priority);
            workItem.setAssignee(assignee);
            workItem.setStatus(WorkflowState.CREATED);
            
            WorkItem createdItem = itemService.createItem(workItem);
            
            // Record operation
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("id", createdItem.getId());
            resultData.put("title", createdItem.getTitle());
            metadataService.completeOperation(operationId, resultData);
            
            return createdItem;
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            throw e;
        }
    }
    
    /**
     * Updates a work item.
     * 
     * @param item the work item to update
     * @return the updated work item
     */
    public WorkItem updateWorkItem(WorkItem item) {
        // Start operation tracking
        Map<String, Object> params = new HashMap<>();
        params.put("item_id", item.getId());
        params.put("title", item.getTitle());
        params.put("type", item.getType());
        params.put("priority", item.getPriority());
        params.put("assignee", item.getAssignee());
        
        String operationId = metadataService.startOperation("pui_bridge", "UPDATE", params);
        
        try {
            WorkItem updatedItem = itemService.updateItem(item);
            
            // Record operation
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("id", updatedItem.getId());
            resultData.put("title", updatedItem.getTitle());
            metadataService.completeOperation(operationId, resultData);
            
            return updatedItem;
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            throw e;
        }
    }
    
    /**
     * Deletes a work item.
     * 
     * @param id the ID of the work item to delete
     * @return true if the item was deleted, false otherwise
     */
    public boolean deleteWorkItem(String id) {
        // Start operation tracking
        Map<String, Object> params = new HashMap<>();
        params.put("item_id", id);
        
        String operationId = metadataService.startOperation("pui_bridge", "DELETE", params);
        
        try {
            boolean success = itemService.deleteItem(id);
            
            // Record operation
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", success);
            metadataService.completeOperation(operationId, resultData);
            
            return success;
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            throw e;
        }
    }
    
    /**
     * Transitions a work item to a new state.
     * 
     * @param id the work item ID
     * @param newState the new state
     * @return true if the transition was successful, false otherwise
     */
    public boolean transitionWorkItem(String id, WorkflowState newState) {
        // Start operation tracking
        Map<String, Object> params = new HashMap<>();
        params.put("item_id", id);
        params.put("new_state", newState);
        
        String operationId = metadataService.startOperation("pui_bridge", "TRANSITION", params);
        
        try {
            WorkItem item = itemService.getItem(id);
            if (item == null) {
                metadataService.failOperation(operationId, new IllegalArgumentException("Item not found: " + id));
                return false;
            }
            
            boolean success = workflowService.transition(UUID.fromString(id), newState);
            
            // Record operation
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", success);
            metadataService.completeOperation(operationId, resultData);
            
            return success;
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            throw e;
        }
    }
    
    /**
     * Searches for work items.
     * 
     * @param query the search query
     * @return a list of matching work items
     */
    public List<WorkItem> searchWorkItems(String query) {
        // Start operation tracking
        Map<String, Object> params = new HashMap<>();
        params.put("query", query);
        
        String operationId = metadataService.startOperation("pui_bridge", "SEARCH", params);
        
        try {
            List<WorkItem> results = searchService.search(query);
            
            // Record operation
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("count", results.size());
            metadataService.completeOperation(operationId, resultData);
            
            return results;
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            throw e;
        }
    }
    
    /**
     * Gets the history for a work item.
     * 
     * @param id the work item ID
     * @return a list of history entries
     */
    public List<Map<String, Object>> getWorkItemHistory(String id) {
        // Start operation tracking
        Map<String, Object> params = new HashMap<>();
        params.put("item_id", id);
        
        String operationId = metadataService.startOperation("pui_bridge", "HISTORY", params);
        
        try {
            // Get history entries from history service
            List<Map<String, Object>> history = new ArrayList<>();
            
            // Convert history entries to maps for PUI
            historyService.getItemHistory(id).forEach(entry -> {
                Map<String, Object> entryMap = new HashMap<>();
                entryMap.put("timestamp", entry.getTimestamp().toString());
                entryMap.put("user", entry.getUsername());
                entryMap.put("type", entry.getType().toString());
                entryMap.put("description", entry.getDescription());
                
                // Add any additional data based on entry type
                if (entry.getAdditionalData() != null) {
                    for (Map.Entry<String, String> dataEntry : entry.getAdditionalData().entrySet()) {
                        // Skip sensitive data
                        if (!dataEntry.getKey().toLowerCase().contains("password") && 
                            !dataEntry.getKey().toLowerCase().contains("secret")) {
                            entryMap.put(dataEntry.getKey(), dataEntry.getValue());
                        }
                    }
                }
                
                history.add(entryMap);
            });
            
            // Record operation
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("count", history.size());
            metadataService.completeOperation(operationId, resultData);
            
            return history;
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            throw e;
        }
    }
    
    /**
     * Gets all operations executed by the services.
     * 
     * @param limit the maximum number of operations to return
     * @return a list of operations
     */
    public List<Map<String, Object>> getRecentOperations(int limit) {
        return metadataService.getRecentOperations(limit);
    }
    
    /**
     * Truncates a string to the specified length and adds an ellipsis if truncated.
     * 
     * @param str the string to truncate
     * @param maxLength the maximum length
     * @return the truncated string
     */
    private String truncateString(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
}