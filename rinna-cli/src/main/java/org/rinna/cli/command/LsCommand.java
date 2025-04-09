/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.command;

import org.rinna.cli.domain.model.WorkItemRelationshipType;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockHistoryService;
import org.rinna.cli.service.MockRelationshipService;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Command to list work items with Linux-style options.
 * - "rin ls" - Short summary listing with inheritance
 * - "rin ls -l" - Detailed listing with all fields
 * - "rin ls -al" - Detailed listing with fields and history
 * - "rin ls [ID]" - Summary of specific work item
 * - "rin ls -l [ID]" - Detailed view of specific work item
 * - "rin ls -al [ID]" - Detailed view with history of specific work item
 * - "rin ls --format=json" - JSON output format
 * - "rin ls --verbose" - Show detailed error information
 */
public class LsCommand implements Callable<Integer> {
    
    private String itemId;
    private boolean longFormat;
    private boolean allFormat;
    private String format = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final ContextManager contextManager;
    private final MetadataService metadataService;
    
    /**
     * Sets the work item ID to list.
     *
     * @param itemId the work item ID
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    /**
     * Sets the long format flag.
     *
     * @param longFormat true to show detailed information
     */
    public void setLongFormat(boolean longFormat) {
        this.longFormat = longFormat;
    }
    
    /**
     * Sets the all format flag.
     *
     * @param allFormat true to show history information
     */
    public void setAllFormat(boolean allFormat) {
        this.allFormat = allFormat;
    }
    
    /**
     * Sets the output format (text or json).
     *
     * @param format the output format
     */
    public void setFormat(String format) {
        this.format = format;
    }
    
    /**
     * Sets whether verbose output is enabled.
     *
     * @param verbose true to enable verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    /**
     * Creates a new LsCommand with default services.
     */
    public LsCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new LsCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public LsCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.contextManager = ContextManager.getInstance();
        this.metadataService = serviceManager.getMetadataService();
    }
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("item_id", itemId);
        params.put("long_format", longFormat);
        params.put("all_format", allFormat);
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("ls", "READ", params);
        
        try {
            // Get services through service interfaces
            ItemService itemService = serviceManager.getItemService();
            MockRelationshipService relationshipService = serviceManager.getMockRelationshipService();
            MockHistoryService historyService = serviceManager.getMockHistoryService();
            
            // Specific work item
            if (itemId != null && !itemId.isEmpty()) {
                try {
                    WorkItem item = itemService.getItem(itemId);
                    
                    if (item == null) {
                        String errorMessage = "Work item not found: " + itemId;
                        System.err.println("Error: " + errorMessage);
                        metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                        return 1;
                    }
                    
                    // Update the last viewed item
                    UUID id = UUID.fromString(item.getId());
                    contextManager.setLastViewedWorkItem(id);
                    
                    // Display the work item
                    if ("json".equalsIgnoreCase(format)) {
                        displayItemJson(item, relationshipService, historyService, allFormat);
                    } else if (longFormat) {
                        printDetailedWorkItem(item, relationshipService, historyService, allFormat);
                    } else {
                        printSummaryWorkItem(item, relationshipService);
                    }
                    
                    // Record the successful operation
                    Map<String, Object> result = new HashMap<>();
                    result.put("item_id", item.getId());
                    result.put("title", item.getTitle());
                    result.put("status", item.getState() != null ? item.getState().toString() : null);
                    metadataService.completeOperation(operationId, result);
                    
                    return 0;
                } catch (IllegalArgumentException e) {
                    String errorMessage = "Invalid work item ID format: " + itemId;
                    System.err.println("Error: " + errorMessage);
                    metadataService.failOperation(operationId, e);
                    return 1;
                }
            }
            
            // List all work items
            List<WorkItem> workItems = itemService.getAllItems();
            
            if (workItems.isEmpty()) {
                System.out.println("No work items found");
                Map<String, Object> result = new HashMap<>();
                result.put("items_found", 0);
                metadataService.completeOperation(operationId, result);
                return 0;
            }
            
            // Sort by ID
            Collections.sort(workItems, Comparator.comparing(WorkItem::getId));
            
            // Display based on format
            if ("json".equalsIgnoreCase(format)) {
                displayItemsJson(workItems, relationshipService, historyService, allFormat);
            } else {
                // Display headers based on format
                if (longFormat) {
                    System.out.println("Detailed Work Item Listing");
                    System.out.println("=========================");
                    System.out.println();
                } else {
                    System.out.println("ID                                      | Title                    | State      | Priority | Assignee");
                    System.out.println("--------------------------------------- | ------------------------ | ---------- | -------- | --------");
                }
                
                // Display work items
                for (WorkItem item : workItems) {
                    if (longFormat) {
                        printDetailedWorkItem(item, relationshipService, historyService, allFormat);
                        System.out.println();
                    } else {
                        printSummaryWorkItem(item, relationshipService);
                    }
                    
                    // Update last viewed item to the last one in the list
                    if (item.equals(workItems.get(workItems.size() - 1)) && longFormat) {
                        contextManager.setLastViewedWorkItem(UUID.fromString(item.getId()));
                    }
                }
            }
            
            // Record the successful operation
            Map<String, Object> result = new HashMap<>();
            result.put("items_found", workItems.size());
            result.put("format", format);
            result.put("long_format", longFormat);
            metadataService.completeOperation(operationId, result);
            
            return 0;
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error listing work items: " + e.getMessage();
            System.err.println("Error: " + errorMessage);
            
            // Record detailed error information if verbose mode is enabled
            if (verbose) {
                e.printStackTrace();
            }
            
            // Record the failed operation with error details
            metadataService.failOperation(operationId, e);
            
            return 1;
        }
    }
    
    /**
     * Prints a summary of a work item.
     *
     * @param item the work item
     * @param relationshipService the relationship service
     */
    private void printSummaryWorkItem(WorkItem item, MockRelationshipService relationshipService) {
        System.out.printf("%-39s | %-24s | %-10s | %-8s | %s%n",
                item.getId(),
                item.getTitle(),
                item.getState(),
                item.getPriority(),
                item.getAssignee());
        
        // Show relationships
        UUID itemId = UUID.fromString(item.getId());
        UUID parentId = relationshipService.getParentWorkItem(itemId);
        if (parentId != null) {
            System.out.printf("  ↳ Child of: %s%n", parentId);
        }
        
        List<UUID> children = relationshipService.getChildWorkItems(itemId);
        if (!children.isEmpty()) {
            System.out.printf("  ↳ Parent of: %s%n", String.join(", ", children.toString()));
        }
    }
    
    /**
     * Prints detailed information about a work item.
     *
     * @param item the work item
     * @param relationshipService the relationship service
     * @param historyService the history service
     * @param includeHistory whether to include history information
     */
    private void printDetailedWorkItem(WorkItem item, MockRelationshipService relationshipService,
                                      MockHistoryService historyService, boolean includeHistory) {
        System.out.println("Work Item: " + item.getId());
        System.out.println("Title: " + item.getTitle());
        System.out.println("Description: " + item.getDescription());
        System.out.println("Type: " + item.getType());
        System.out.println("Priority: " + item.getPriority());
        System.out.println("State: " + item.getState());
        System.out.println("Assignee: " + item.getAssignee());
        System.out.println("Project: " + item.getProject());
        System.out.println("Created: " + item.getCreated());
        System.out.println("Updated: " + item.getUpdated());
        
        // Relationships
        UUID itemId = UUID.fromString(item.getId());
        UUID parentId = relationshipService.getParentWorkItem(itemId);
        if (parentId != null) {
            WorkItemRelationshipType relationType = relationshipService.getRelationshipType(itemId, parentId);
            System.out.println("Parent: " + parentId + " (" + relationType + ")");
        } else {
            System.out.println("Parent: None");
        }
        
        List<UUID> children = relationshipService.getChildWorkItems(itemId);
        if (!children.isEmpty()) {
            System.out.println("Children: " + String.join(", ", children.toString()));
        } else {
            System.out.println("Children: None");
        }
        
        // Include history if requested
        if (includeHistory) {
            List<MockHistoryService.HistoryEntryRecord> history = ((MockHistoryService) historyService).getHistory(itemId);
            
            if (!history.isEmpty()) {
                System.out.println("\nHistory:");
                
                for (MockHistoryService.HistoryEntryRecord entry : history) {
                    System.out.printf("%s: %s by %s", 
                            entry.getTimestamp(),
                            entry.getType(),
                            entry.getUser());
                    
                    // Display type-specific details
                    switch (entry.getType()) {
                        case STATE_CHANGE:
                            // Format: Previous State → New State
                            System.out.printf(": %s%n", entry.getContent());
                            break;
                        case FIELD_CHANGE:
                            // Format: Field: Previous Value → New Value
                            System.out.printf(": %s%n", entry.getContent());
                            break;
                        case ASSIGNMENT:
                            // Format: Previous Assignee → New Assignee
                            System.out.printf(": %s%n", entry.getContent());
                            break;
                        case LINK:
                            // New type in the updated model
                            System.out.printf(": %s%n", entry.getContent());
                            break;
                        default:
                            System.out.println();
                    }
                }
            } else {
                System.out.println("\nHistory: None");
            }
        }
    }
    
    /**
     * Displays a single work item in JSON format.
     *
     * @param item the work item to display
     * @param relationshipService the relationship service
     * @param historyService the history service for history entries
     * @param includeHistory whether to include history information
     */
    private void displayItemJson(WorkItem item, MockRelationshipService relationshipService, 
                                MockHistoryService historyService, boolean includeHistory) {
        Map<String, Object> jsonData = new HashMap<>();
        
        // Add basic work item information
        jsonData.put("id", item.getId());
        jsonData.put("title", item.getTitle());
        jsonData.put("description", item.getDescription());
        jsonData.put("type", item.getType() != null ? item.getType().toString() : null);
        jsonData.put("priority", item.getPriority() != null ? item.getPriority().toString() : null);
        jsonData.put("state", item.getState() != null ? item.getState().toString() : null);
        jsonData.put("assignee", item.getAssignee());
        jsonData.put("project", item.getProject());
        jsonData.put("created", item.getCreated() != null ? item.getCreated().toString() : null);
        jsonData.put("updated", item.getUpdated() != null ? item.getUpdated().toString() : null);
        
        // Add relationships
        Map<String, Object> relationships = new HashMap<>();
        UUID itemId = UUID.fromString(item.getId());
        
        // Parent relationship
        UUID parentId = relationshipService.getParentWorkItem(itemId);
        if (parentId != null) {
            relationships.put("parent", parentId.toString());
            relationships.put("parentRelationType", 
                    relationshipService.getRelationshipType(itemId, parentId).toString());
        }
        
        // Child relationships
        List<UUID> children = relationshipService.getChildWorkItems(itemId);
        if (!children.isEmpty()) {
            List<String> childIds = new ArrayList<>();
            for (UUID childId : children) {
                childIds.add(childId.toString());
            }
            relationships.put("children", childIds);
        }
        
        jsonData.put("relationships", relationships);
        
        // Add history if requested
        if (includeHistory) {
            List<Map<String, Object>> historyEntries = new ArrayList<>();
            
            // Cast is needed since we need to access the specific record type
            if (historyService instanceof MockHistoryService) {
                MockHistoryService mockHistoryService = (MockHistoryService) historyService;
                List<MockHistoryService.HistoryEntryRecord> history = mockHistoryService.getHistory(itemId);
                
                for (MockHistoryService.HistoryEntryRecord entry : history) {
                    Map<String, Object> entryMap = new HashMap<>();
                    entryMap.put("timestamp", entry.getTimestamp().toString());
                    entryMap.put("type", entry.getType().toString());
                    entryMap.put("user", entry.getUser());
                    entryMap.put("content", entry.getContent());
                    historyEntries.add(entryMap);
                }
            }
            
            jsonData.put("history", historyEntries);
        }
        
        // Add display options to the output
        Map<String, Object> options = new HashMap<>();
        options.put("longFormat", longFormat);
        options.put("allFormat", allFormat);
        jsonData.put("displayOptions", options);
        
        // Use the OutputFormatter for consistent JSON output
        String jsonOutput = OutputFormatter.toJson(jsonData, verbose);
        System.out.println(jsonOutput);
    }
    
    /**
     * Displays multiple work items in JSON format.
     *
     * @param items the work items to display
     * @param relationshipService the relationship service
     * @param historyService the history service for history entries
     * @param includeHistory whether to include history information
     */
    private void displayItemsJson(List<WorkItem> items, MockRelationshipService relationshipService,
                                 MockHistoryService historyService, boolean includeHistory) {
        Map<String, Object> jsonData = new HashMap<>();
        List<Map<String, Object>> itemsList = new ArrayList<>();
        
        for (WorkItem item : items) {
            Map<String, Object> itemMap = new HashMap<>();
            
            // Add basic work item information
            itemMap.put("id", item.getId());
            itemMap.put("title", item.getTitle());
            itemMap.put("state", item.getState() != null ? item.getState().toString() : null);
            itemMap.put("priority", item.getPriority() != null ? item.getPriority().toString() : null);
            itemMap.put("assignee", item.getAssignee());
            
            // Add detailed information if requested
            if (longFormat) {
                itemMap.put("description", item.getDescription());
                itemMap.put("type", item.getType() != null ? item.getType().toString() : null);
                itemMap.put("project", item.getProject());
                itemMap.put("created", item.getCreated() != null ? item.getCreated().toString() : null);
                itemMap.put("updated", item.getUpdated() != null ? item.getUpdated().toString() : null);
                
                // Add relationships
                Map<String, Object> relationships = new HashMap<>();
                UUID itemId = UUID.fromString(item.getId());
                
                // Parent relationship
                UUID parentId = relationshipService.getParentWorkItem(itemId);
                if (parentId != null) {
                    relationships.put("parent", parentId.toString());
                    relationships.put("parentRelationType", 
                            relationshipService.getRelationshipType(itemId, parentId).toString());
                }
                
                // Child relationships
                List<UUID> children = relationshipService.getChildWorkItems(itemId);
                if (!children.isEmpty()) {
                    List<String> childIds = new ArrayList<>();
                    for (UUID childId : children) {
                        childIds.add(childId.toString());
                    }
                    relationships.put("children", childIds);
                }
                
                itemMap.put("relationships", relationships);
                
                // Add history if requested
                if (includeHistory) {
                    List<Map<String, Object>> historyEntries = new ArrayList<>();
                    
                    // Cast is needed since we need to access the specific record type
                    if (historyService instanceof MockHistoryService) {
                        MockHistoryService mockHistoryService = (MockHistoryService) historyService;
                        List<MockHistoryService.HistoryEntryRecord> history = mockHistoryService.getHistory(itemId);
                        
                        for (MockHistoryService.HistoryEntryRecord entry : history) {
                            Map<String, Object> entryMap = new HashMap<>();
                            entryMap.put("timestamp", entry.getTimestamp().toString());
                            entryMap.put("type", entry.getType().toString());
                            entryMap.put("user", entry.getUser());
                            entryMap.put("content", entry.getContent());
                            historyEntries.add(entryMap);
                        }
                    }
                    
                    itemMap.put("history", historyEntries);
                }
            }
            
            itemsList.add(itemMap);
        }
        
        jsonData.put("workItems", itemsList);
        jsonData.put("count", items.size());
        
        // Add display options to the output
        Map<String, Object> options = new HashMap<>();
        options.put("longFormat", longFormat);
        options.put("allFormat", allFormat);
        jsonData.put("displayOptions", options);
        
        // Use the OutputFormatter for consistent JSON output
        String jsonOutput = OutputFormatter.toJson(jsonData, verbose);
        System.out.println(jsonOutput);
    }
}