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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.SearchService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.WorkflowService;
import org.rinna.cli.util.OutputFormatter;

/**
 * Command to perform bulk updates on work items.
 * Follows the ViewCommand pattern with proper MetadataService integration
 * for tracking bulk update operations.
 */
public class BulkCommand implements Callable<Integer> {
    // Command parameters
    private Map<String, String> filters;
    private Map<String, String> updates;
    private String format = "text";
    private boolean verbose = false;
    private String username;
    
    // Services
    private final ServiceManager serviceManager;
    private final ConfigurationService configService;
    private final MetadataService metadataService;
    private final ItemService itemService;
    private final WorkflowService workflowService;
    private final SearchService searchService;
    private final ContextManager contextManager;
    
    /**
     * Default constructor using singleton service manager.
     */
    public BulkCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Constructor with service manager for dependency injection.
     * Primarily used for testing.
     *
     * @param serviceManager the service manager to use
     */
    public BulkCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.configService = serviceManager.getConfigurationService();
        this.metadataService = serviceManager.getMetadataService();
        this.itemService = serviceManager.getMockItemService();
        this.workflowService = serviceManager.getMockWorkflowService();
        this.searchService = serviceManager.getMockSearchService();
        this.contextManager = ContextManager.getInstance();
        
        // Initialize collections
        this.filters = new HashMap<>();
        this.updates = new HashMap<>();
        
        // Get current user from configuration
        this.username = configService.getCurrentUser();
        if (this.username == null || this.username.isEmpty()) {
            this.username = System.getProperty("user.name");
        }
    }
    
    /**
     * Sets a filter parameter.
     *
     * @param name the name of the filter
     * @param value the value of the filter
     * @return this command instance for method chaining
     */
    public BulkCommand setFilter(String name, String value) {
        filters.put(name, value);
        return this;
    }
    
    /**
     * Sets an update parameter.
     *
     * @param name the name of the field to update
     * @param value the new value for the field
     * @return this command instance for method chaining
     */
    public BulkCommand setUpdate(String name, String value) {
        updates.put(name, value);
        return this;
    }
    
    /**
     * Sets the output format.
     * 
     * @param format the output format ("text" or "json")
     * @return this command instance for method chaining
     */
    public BulkCommand setFormat(String format) {
        this.format = format;
        return this;
    }
    
    /**
     * Sets the JSON output flag (for backward compatibility).
     * 
     * @param jsonOutput true to output in JSON format, false for text
     * @return this command instance for method chaining
     */
    public BulkCommand setJsonOutput(boolean jsonOutput) {
        this.format = jsonOutput ? "json" : "text";
        return this;
    }
    
    /**
     * Sets the verbose output flag.
     * 
     * @param verbose true for verbose output, false for normal output
     * @return this command instance for method chaining
     */
    public BulkCommand setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }
    
    /**
     * Sets the username.
     * 
     * @param username the username
     * @return this command instance for method chaining
     */
    public BulkCommand setUsername(String username) {
        this.username = username;
        return this;
    }
    
    @Override
    public Integer call() {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        params.put("format", format);
        params.put("verbose", verbose);
        params.put("filterCount", filters.size());
        params.put("updateCount", updates.size());
        
        // Include each filter and update in the parameters
        for (Map.Entry<String, String> entry : filters.entrySet()) {
            params.put("filter." + entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, String> entry : updates.entrySet()) {
            params.put("update." + entry.getKey(), entry.getValue());
        }
        
        // Start tracking main operation
        String operationId = metadataService.startOperation("bulk-command", "UPDATE", params);
        
        try {
            // Validate parameters
            if (filters.isEmpty()) {
                displayError("No filters specified", 
                          "Usage: rin bulk --<filter>=<value> --set-<field>=<value>");
                
                metadataService.failOperation(operationId,
                    new IllegalArgumentException("No filters specified"));
                
                return 1;
            }
            
            if (updates.isEmpty()) {
                displayError("No updates specified", 
                          "Usage: rin bulk --<filter>=<value> --set-<field>=<value>");
                
                metadataService.failOperation(operationId,
                    new IllegalArgumentException("No updates specified"));
                
                return 1;
            }
            
            // Track filter operation
            String filterOpId = metadataService.startOperation(
                "bulk-filter", "SEARCH", 
                Map.of(
                    "username", username,
                    "filterCount", filters.size()
                ));
            
            // Get work items based on filters
            List<WorkItem> filteredItems;
            try {
                filteredItems = filterWorkItems();
                
                // Complete filter operation with results
                Map<String, Object> filterResult = new HashMap<>();
                filterResult.put("itemCount", filteredItems.size());
                filterResult.put("success", true);
                metadataService.completeOperation(filterOpId, filterResult);
            } catch (Exception e) {
                // Complete filter operation with error
                metadataService.failOperation(filterOpId, e);
                displayError("Error filtering items: " + e.getMessage(), null);
                metadataService.failOperation(operationId, e);
                return 1;
            }
            
            if (filteredItems.isEmpty()) {
                // Track display warning operation
                String warningOpId = metadataService.startOperation(
                    "bulk-warning-display", "READ", 
                    Map.of("format", format));
                
                if ("json".equalsIgnoreCase(format)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("result", "warning");
                    response.put("message", "No tasks found matching the filter criteria");
                    System.out.println(OutputFormatter.toJson(response));
                } else {
                    System.out.println("Warning: No tasks found matching the filter criteria");
                }
                
                metadataService.completeOperation(warningOpId, Map.of("success", true));
                
                // Complete main operation with warning
                Map<String, Object> result = new HashMap<>();
                result.put("itemCount", 0);
                result.put("warning", "No tasks found");
                result.put("success", true);
                
                metadataService.completeOperation(operationId, result);
                
                return 0;
            }
            
            // Track update operation
            String updateOpId = metadataService.startOperation(
                "bulk-update-apply", "UPDATE", 
                Map.of(
                    "username", username,
                    "updateCount", updates.size(),
                    "itemCount", filteredItems.size()
                ));
            
            // Apply updates to filtered items
            try {
                Map<String, Integer> updateCounts = new HashMap<>();
                int updatedCount = applyUpdates(filteredItems, updates, updateCounts, updateOpId);
                
                // Complete update operation with results
                Map<String, Object> updateResult = new HashMap<>();
                updateResult.put("updatedCount", updatedCount);
                updateResult.put("success", true);
                for (Map.Entry<String, Integer> entry : updateCounts.entrySet()) {
                    updateResult.put("field." + entry.getKey(), entry.getValue());
                }
                
                metadataService.completeOperation(updateOpId, updateResult);
                
                // Track display operation
                String displayOpId = metadataService.startOperation(
                    "bulk-result-display", "READ", 
                    Map.of(
                        "format", format,
                        "updatedCount", updatedCount,
                        "itemCount", filteredItems.size()
                    ));
                
                // Display results
                if ("json".equalsIgnoreCase(format)) {
                    displayResultAsJson(updatedCount, filteredItems.size(), updateCounts);
                } else {
                    displayResultAsText(updatedCount, filteredItems.size(), updateCounts);
                }
                
                metadataService.completeOperation(displayOpId, Map.of("success", true));
                
                // Complete main operation
                Map<String, Object> result = new HashMap<>();
                result.put("itemCount", filteredItems.size());
                result.put("updatedCount", updatedCount);
                result.put("success", true);
                
                metadataService.completeOperation(operationId, result);
                
                return 0;
            } catch (IllegalArgumentException e) {
                displayError(e.getMessage(), null);
                
                metadataService.failOperation(updateOpId, e);
                metadataService.failOperation(operationId, e);
                return 1;
            }
        } catch (Exception e) {
            displayError(e.getMessage(), null);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Display results in JSON format.
     */
    private void displayResultAsJson(int updatedCount, int itemCount, Map<String, Integer> updateCounts) {
        Map<String, Object> response = new HashMap<>();
        response.put("result", "success");
        response.put("message", "Successfully updated " + updatedCount + " field(s) across " + itemCount + " task(s)");
        response.put("updatedCount", updatedCount);
        response.put("itemCount", itemCount);
        
        // Include field-specific counts
        if (!updateCounts.isEmpty()) {
            Map<String, Object> fieldsMap = new HashMap<>();
            updateCounts.forEach(fieldsMap::put);
            response.put("fields", fieldsMap);
        }
        
        System.out.println(OutputFormatter.toJson(response));
    }
    
    /**
     * Display results in text format.
     */
    private void displayResultAsText(int updatedCount, int itemCount, Map<String, Integer> updateCounts) {
        System.out.println("Successfully updated " + updatedCount + " field(s) across " + itemCount + " task(s).");
        
        // Print update summary
        updateCounts.forEach((field, count) -> {
            System.out.println("Updated " + field + " for " + count + " items");
        });
    }
    
    /**
     * Display an error message in the appropriate format.
     * 
     * @param message the error message
     * @param details additional details (can be null)
     */
    private void displayError(String message, String details) {
        if ("json".equalsIgnoreCase(format)) {
            Map<String, Object> error = new HashMap<>();
            error.put("result", "error");
            error.put("message", message);
            
            if (details != null && !details.isEmpty()) {
                error.put("details", details);
            }
            
            System.out.println(OutputFormatter.toJson(error));
        } else {
            System.err.println("Error: " + message);
            
            if (details != null && !details.isEmpty()) {
                System.err.println(details);
            }
        }
    }
    
    /**
     * Filters work items based on the specified filters.
     * Now includes detailed operation tracking for monitoring and auditing purposes.
     *
     * @return the list of work items matching the filters
     */
    private List<WorkItem> filterWorkItems() {
        String filterMethodOpId = metadataService.startOperation(
            "bulk-filter-method", "SEARCH", 
            Map.of(
                "username", username,
                "filterCount", filters.size(),
                "filterKeys", String.join(",", filters.keySet())
            ));
            
        try {
            // Start primary filter operation with tracking
            String primaryFilterOpId = metadataService.startOperation(
                "bulk-primary-filter", "SEARCH", 
                Map.of(
                    "username", username,
                    "primaryFilterKey", filters.keySet().stream().findFirst().orElse("none")
                ));
                
            List<WorkItem> allItems = new ArrayList<>();
            String primaryFilterType = "none";
            
            try {
                // Build up the filter pipeline in a more sophisticated way
                // We'll try to use the most efficient search mechanism based on the filters
                
                // First check if we have a specific status filter - this is often indexed
                if (filters.containsKey("status")) {
                    try {
                        primaryFilterType = "status";
                        WorkflowState statusFilter = WorkflowState.valueOf(filters.get("status").toUpperCase());
                        allItems.addAll(workflowService.findByStatus(statusFilter));
                    } catch (IllegalArgumentException e) {
                        metadataService.failOperation(primaryFilterOpId, e);
                        throw new IllegalArgumentException("Invalid status: " + filters.get("status"));
                    }
                }
                // Or if we have a specific assignee filter - also commonly indexed
                else if (filters.containsKey("assignee")) {
                    primaryFilterType = "assignee";
                    String assigneeFilter = filters.get("assignee");
                    allItems.addAll(itemService.findByAssignee(assigneeFilter));
                }
                // Or if we are filtering by type
                else if (filters.containsKey("type")) {
                    try {
                        primaryFilterType = "type";
                        WorkItemType typeFilter = WorkItemType.valueOf(filters.get("type").toUpperCase());
                        allItems.addAll(itemService.findByType(typeFilter));
                    } catch (IllegalArgumentException e) {
                        metadataService.failOperation(primaryFilterOpId, e);
                        throw new IllegalArgumentException("Invalid type: " + filters.get("type"));
                    }
                }
                // Or if we have a text search
                else if (filters.containsKey("text")) {
                    primaryFilterType = "text";
                    String textFilter = filters.get("text");
                    allItems.addAll(searchService.findItemsByText(textFilter));
                }
                // Or if we have a project filter
                else if (filters.containsKey("project")) {
                    primaryFilterType = "project";
                    String projectFilter = filters.get("project");
                    // Use a metadata filter to get items by project
                    Map<String, String> metadata = new HashMap<>();
                    metadata.put("project", projectFilter);
                    allItems.addAll(searchService.findItemsByMetadata(metadata));
                }
                // If no specific high-efficiency filters, get all items
                else {
                    primaryFilterType = "all";
                    allItems.addAll(itemService.getAllWorkItems());
                }
                
                // Complete primary filter operation
                Map<String, Object> primaryFilterResult = new HashMap<>();
                primaryFilterResult.put("primaryFilterType", primaryFilterType);
                primaryFilterResult.put("itemCount", allItems.size());
                primaryFilterResult.put("success", true);
                
                metadataService.completeOperation(primaryFilterOpId, primaryFilterResult);
            } catch (Exception e) {
                metadataService.failOperation(primaryFilterOpId, e);
                throw e; // Rethrow for higher level handling
            }
            
            if (allItems.isEmpty()) {
                // Complete filter method operation with empty result
                Map<String, Object> emptyResult = new HashMap<>();
                emptyResult.put("itemCount", 0);
                emptyResult.put("message", "No items found in primary filter");
                emptyResult.put("success", true);
                
                metadataService.completeOperation(filterMethodOpId, emptyResult);
                return allItems; // No point in further filtering
            }
            
            // Start secondary filter operation with tracking
            String secondaryFilterOpId = metadataService.startOperation(
                "bulk-secondary-filter", "FILTER", 
                Map.of(
                    "username", username,
                    "secondaryFilterCount", Math.max(0, filters.size() - 1),
                    "initialItemCount", allItems.size()
                ));
                
            try {
                // Now do a secondary filter pass to apply any additional filters
                List<WorkItem> filteredItems = new ArrayList<>(allItems);
                int itemsRemovedBySecondaryFilters = 0;
                
                // Apply secondary filters that weren't used in the initial query
                for (Map.Entry<String, String> filter : filters.entrySet()) {
                    String filterName = filter.getKey();
                    String filterValue = filter.getValue();
                    
                    // Track the specific filter being applied
                    String specificFilterOpId = metadataService.startOperation(
                        "bulk-filter-" + filterName, "FILTER", 
                        Map.of(
                            "filterName", filterName,
                            "filterValue", filterValue,
                            "itemCountBefore", filteredItems.size()
                        ));
                        
                    int sizeBefore = filteredItems.size();
                    
                    try {
                        // Skip filters we already applied in the primary filter stage
                        if ((filterName.equals("status") && primaryFilterType.equals("status")) ||
                            (filterName.equals("assignee") && primaryFilterType.equals("assignee")) ||
                            (filterName.equals("type") && primaryFilterType.equals("type")) ||
                            (filterName.equals("text") && primaryFilterType.equals("text")) ||
                            (filterName.equals("project") && primaryFilterType.equals("project"))) {
                            
                            // Complete with skip status
                            metadataService.completeOperation(specificFilterOpId, 
                                Map.of(
                                    "status", "skipped",
                                    "reason", "Already applied in primary filter",
                                    "success", true
                                ));
                                
                            continue;
                        }
                        
                        // Apply the remaining filters
                        switch (filterName) {
                            case "source":
                                filteredItems.removeIf(item -> !"imported".equals(item.getProject()));
                                break;
                            case "status":
                                try {
                                    WorkflowState statusFilter = WorkflowState.valueOf(filterValue.toUpperCase());
                                    filteredItems.removeIf(item -> item.getState() != statusFilter);
                                } catch (IllegalArgumentException e) {
                                    metadataService.failOperation(specificFilterOpId, e);
                                    throw new IllegalArgumentException("Invalid status: " + filterValue);
                                }
                                break;
                            case "priority":
                                try {
                                    Priority priorityFilter = Priority.valueOf(filterValue.toUpperCase());
                                    filteredItems.removeIf(item -> item.getPriority() != priorityFilter);
                                } catch (IllegalArgumentException e) {
                                    metadataService.failOperation(specificFilterOpId, e);
                                    throw new IllegalArgumentException("Invalid priority: " + filterValue);
                                }
                                break;
                            case "assignee":
                                filteredItems.removeIf(item -> !filterValue.equals(item.getAssignee()));
                                break;
                            case "type":
                                try {
                                    WorkItemType typeFilter = WorkItemType.valueOf(filterValue.toUpperCase());
                                    filteredItems.removeIf(item -> item.getType() != typeFilter);
                                } catch (IllegalArgumentException e) {
                                    metadataService.failOperation(specificFilterOpId, e);
                                    throw new IllegalArgumentException("Invalid type: " + filterValue);
                                }
                                break;
                            case "project":
                                filteredItems.removeIf(item -> !filterValue.equals(item.getProject()));
                                break;
                            case "created-after":
                                try {
                                    java.time.LocalDateTime dateFilter = java.time.LocalDateTime.parse(filterValue);
                                    filteredItems.removeIf(item -> item.getCreated() == null || item.getCreated().isBefore(dateFilter));
                                } catch (Exception e) {
                                    metadataService.failOperation(specificFilterOpId, e);
                                    throw new IllegalArgumentException("Invalid date format for created-after: " + filterValue + 
                                            ". Expected format: yyyy-MM-ddTHH:mm:ss");
                                }
                                break;
                            case "created-before":
                                try {
                                    java.time.LocalDateTime dateFilter = java.time.LocalDateTime.parse(filterValue);
                                    filteredItems.removeIf(item -> item.getCreated() == null || item.getCreated().isAfter(dateFilter));
                                } catch (Exception e) {
                                    metadataService.failOperation(specificFilterOpId, e);
                                    throw new IllegalArgumentException("Invalid date format for created-before: " + filterValue + 
                                            ". Expected format: yyyy-MM-ddTHH:mm:ss");
                                }
                                break;
                            case "reporter":
                                filteredItems.removeIf(item -> !filterValue.equals(item.getReporter()));
                                break;
                            case "title-contains":
                                filteredItems.removeIf(item -> !item.getTitle().toLowerCase().contains(filterValue.toLowerCase()));
                                break;
                            default:
                                // Complete with unknown filter warning
                                metadataService.completeOperation(specificFilterOpId, 
                                    Map.of(
                                        "status", "warning",
                                        "message", "Unknown filter: " + filterName,
                                        "success", true
                                    ));
                                
                                System.err.println("Warning: Unknown filter: " + filterName);
                                continue;
                        }
                        
                        // Calculate items removed by this filter
                        int itemsRemoved = sizeBefore - filteredItems.size();
                        itemsRemovedBySecondaryFilters += itemsRemoved;
                        
                        // Complete specific filter operation
                        Map<String, Object> specificFilterResult = new HashMap<>();
                        specificFilterResult.put("itemCountBefore", sizeBefore);
                        specificFilterResult.put("itemCountAfter", filteredItems.size());
                        specificFilterResult.put("itemsRemoved", itemsRemoved);
                        specificFilterResult.put("success", true);
                        
                        metadataService.completeOperation(specificFilterOpId, specificFilterResult);
                    } catch (Exception e) {
                        // Only fail the specific filter operation if not already failed
                        metadataService.failOperation(specificFilterOpId, e);
                        throw e; // Rethrow for higher level handling
                    }
                }
                
                // Complete secondary filter operation
                Map<String, Object> secondaryFilterResult = new HashMap<>();
                secondaryFilterResult.put("initialCount", allItems.size());
                secondaryFilterResult.put("finalCount", filteredItems.size());
                secondaryFilterResult.put("itemsRemoved", itemsRemovedBySecondaryFilters);
                secondaryFilterResult.put("success", true);
                
                metadataService.completeOperation(secondaryFilterOpId, secondaryFilterResult);
                
                // Complete filter method operation
                Map<String, Object> filterMethodResult = new HashMap<>();
                filterMethodResult.put("initialCount", allItems.size());
                filterMethodResult.put("finalCount", filteredItems.size());
                filterMethodResult.put("filtersApplied", filters.size());
                filterMethodResult.put("success", true);
                
                metadataService.completeOperation(filterMethodOpId, filterMethodResult);
                
                return filteredItems;
            } catch (Exception e) {
                metadataService.failOperation(secondaryFilterOpId, e);
                metadataService.failOperation(filterMethodOpId, e);
                throw e; // Rethrow for higher level handling
            }
        } catch (Exception e) {
            metadataService.failOperation(filterMethodOpId, e);
            throw e; // Rethrow for higher level handling
        }
    }
    
    /**
     * Applies updates to the filtered work items.
     * Now includes detailed operation tracking with parameters for monitoring and auditing.
     *
     * @param items the list of work items to update
     * @param updates the map of updates to apply
     * @param updateCounts a map to track the count of updates by field type
     * @param operationId the parent operation ID for tracking
     * @return the number of updates applied
     */
    private int applyUpdates(List<WorkItem> items, Map<String, String> updates, 
                            Map<String, Integer> updateCounts, String operationId) {
        // Track overall update process
        String applyUpdatesOpId = metadataService.startOperation(
            "bulk-apply-updates-method", "UPDATE", 
            Map.of(
                "username", username,
                "itemCount", items.size(),
                "updateCount", updates.size(),
                "updateTypes", String.join(",", updates.keySet()),
                "parentOperationId", operationId
            ));
            
        try {
            // Get the current user for tracking changes
            String currentUser = configService.getCurrentUser();
            if (currentUser == null || currentUser.isEmpty()) {
                currentUser = System.getProperty("user.name");
            }
            
            // Initialize update tracking
            int totalUpdates = 0;
            
            // Track each update type with separate operations
            Map<String, String> updateTypeOperations = new HashMap<>();
            for (String updateType : updates.keySet()) {
                String updateTypeOpId = metadataService.startOperation(
                    "bulk-update-type-" + updateType, "UPDATE", 
                    Map.of(
                        "updateType", updateType,
                        "updateValue", updates.get(updateType),
                        "itemCount", items.size(),
                        "parentOperationId", applyUpdatesOpId
                    ));
                updateTypeOperations.put(updateType, updateTypeOpId);
            }
            
            // Process each item with operation tracking
            for (WorkItem item : items) {
                // Start item-specific operation tracking
                String itemUpdateOpId = metadataService.startOperation(
                    "bulk-update-item", "UPDATE", 
                    Map.of(
                        "itemId", item.getId(),
                        "title", item.getTitle(),
                        "currentState", item.getState().toString(),
                        "updateCount", updates.size(),
                        "parentOperationId", applyUpdatesOpId
                    ));
                    
                try {
                    UUID itemId = UUID.fromString(item.getId());
                    boolean itemModified = false;
                    Map<String, Object> itemUpdates = new HashMap<>();
                    
                    // Process status changes through workflow transition
                    if (updates.containsKey("set-status")) {
                        String statusUpdateOpId = metadataService.startOperation(
                            "bulk-update-item-status", "UPDATE", 
                            Map.of(
                                "itemId", item.getId(),
                                "currentStatus", item.getState().toString(),
                                "targetStatus", updates.get("set-status"),
                                "parentOperationId", itemUpdateOpId
                            ));
                            
                        try {
                            WorkflowState newState = WorkflowState.valueOf(updates.get("set-status").toUpperCase());
                            if (item.getState() != newState) {
                                workflowService.transition(
                                    item.getId(), 
                                    currentUser, 
                                    newState, 
                                    "Bulk update: set status to " + newState
                                );
                                
                                updateCounts.put("status", updateCounts.getOrDefault("status", 0) + 1);
                                totalUpdates++;
                                itemModified = true;
                                
                                // Update our local object to match database for other changes
                                item.setState(newState);
                                
                                // Record the successful update
                                itemUpdates.put("status", Map.of(
                                    "from", item.getState().toString(),
                                    "to", newState.toString(),
                                    "success", true
                                ));
                                
                                // Complete status update operation
                                metadataService.completeOperation(statusUpdateOpId, 
                                    Map.of(
                                        "success", true,
                                        "fromState", item.getState().toString(),
                                        "toState", newState.toString()
                                    ));
                            } else {
                                // No change needed - status already matches
                                metadataService.completeOperation(statusUpdateOpId, 
                                    Map.of(
                                        "success", true,
                                        "message", "Status already set to " + newState.toString(),
                                        "noChangeRequired", true
                                    ));
                            }
                        } catch (IllegalArgumentException e) {
                            metadataService.failOperation(statusUpdateOpId, 
                                new IllegalArgumentException("Invalid status: " + updates.get("set-status")));
                            throw new IllegalArgumentException("Invalid status: " + updates.get("set-status"));
                        } catch (Exception e) {
                            metadataService.failOperation(statusUpdateOpId, e);
                            throw e;
                        }
                    }
                    
                    // Process priority changes
                    if (updates.containsKey("set-priority")) {
                        String priorityUpdateOpId = metadataService.startOperation(
                            "bulk-update-item-priority", "UPDATE", 
                            Map.of(
                                "itemId", item.getId(),
                                "currentPriority", item.getPriority().toString(),
                                "targetPriority", updates.get("set-priority"),
                                "parentOperationId", itemUpdateOpId
                            ));
                            
                        try {
                            Priority newPriority = Priority.valueOf(updates.get("set-priority").toUpperCase());
                            if (item.getPriority() != newPriority) {
                                itemService.updatePriority(itemId, newPriority, currentUser);
                                
                                updateCounts.put("priority", updateCounts.getOrDefault("priority", 0) + 1);
                                totalUpdates++;
                                itemModified = true;
                                
                                // Update our local object
                                Priority oldPriority = item.getPriority();
                                item.setPriority(newPriority);
                                
                                // Record the successful update
                                itemUpdates.put("priority", Map.of(
                                    "from", oldPriority.toString(),
                                    "to", newPriority.toString(),
                                    "success", true
                                ));
                                
                                // Complete priority update operation
                                metadataService.completeOperation(priorityUpdateOpId, 
                                    Map.of(
                                        "success", true,
                                        "fromPriority", oldPriority.toString(),
                                        "toPriority", newPriority.toString()
                                    ));
                            } else {
                                // No change needed - priority already matches
                                metadataService.completeOperation(priorityUpdateOpId, 
                                    Map.of(
                                        "success", true,
                                        "message", "Priority already set to " + newPriority.toString(),
                                        "noChangeRequired", true
                                    ));
                            }
                        } catch (IllegalArgumentException e) {
                            metadataService.failOperation(priorityUpdateOpId, 
                                new IllegalArgumentException("Invalid priority: " + updates.get("set-priority")));
                            throw new IllegalArgumentException("Invalid priority: " + updates.get("set-priority"));
                        } catch (Exception e) {
                            metadataService.failOperation(priorityUpdateOpId, e);
                            throw e;
                        }
                    }
                    
                    // Process assignee changes
                    if (updates.containsKey("set-assignee")) {
                        String assigneeUpdateOpId = metadataService.startOperation(
                            "bulk-update-item-assignee", "UPDATE", 
                            Map.of(
                                "itemId", item.getId(),
                                "currentAssignee", item.getAssignee() != null ? item.getAssignee() : "unassigned",
                                "targetAssignee", updates.get("set-assignee"),
                                "parentOperationId", itemUpdateOpId
                            ));
                            
                        try {
                            String newAssignee = updates.get("set-assignee");
                            String currentAssignee = item.getAssignee() != null ? item.getAssignee() : "unassigned";
                            
                            if (!newAssignee.equals(currentAssignee)) {
                                itemService.assignTo(itemId, newAssignee, currentUser);
                                
                                updateCounts.put("assignee", updateCounts.getOrDefault("assignee", 0) + 1);
                                totalUpdates++;
                                itemModified = true;
                                
                                // Update our local object
                                item.setAssignee(newAssignee);
                                
                                // Record the successful update
                                itemUpdates.put("assignee", Map.of(
                                    "from", currentAssignee,
                                    "to", newAssignee,
                                    "success", true
                                ));
                                
                                // Complete assignee update operation
                                metadataService.completeOperation(assigneeUpdateOpId, 
                                    Map.of(
                                        "success", true,
                                        "fromAssignee", currentAssignee,
                                        "toAssignee", newAssignee
                                    ));
                            } else {
                                // No change needed - assignee already matches
                                metadataService.completeOperation(assigneeUpdateOpId, 
                                    Map.of(
                                        "success", true,
                                        "message", "Assignee already set to " + newAssignee,
                                        "noChangeRequired", true
                                    ));
                            }
                        } catch (Exception e) {
                            metadataService.failOperation(assigneeUpdateOpId, e);
                            throw e;
                        }
                    }
                    
                    // Process title changes
                    if (updates.containsKey("set-title")) {
                        String titleUpdateOpId = metadataService.startOperation(
                            "bulk-update-item-title", "UPDATE", 
                            Map.of(
                                "itemId", item.getId(),
                                "parentOperationId", itemUpdateOpId
                            ));
                            
                        try {
                            String newTitle = updates.get("set-title");
                            String currentTitle = item.getTitle();
                            
                            if (!newTitle.equals(currentTitle)) {
                                itemService.updateTitle(itemId, newTitle, currentUser);
                                
                                updateCounts.put("title", updateCounts.getOrDefault("title", 0) + 1);
                                totalUpdates++;
                                itemModified = true;
                                
                                // Update our local object
                                item.setTitle(newTitle);
                                
                                // Record the successful update
                                itemUpdates.put("title", Map.of(
                                    "updated", true,
                                    "success", true
                                ));
                                
                                // Complete title update operation
                                metadataService.completeOperation(titleUpdateOpId, 
                                    Map.of(
                                        "success", true,
                                        "updated", true
                                    ));
                            } else {
                                // No change needed - title already matches
                                metadataService.completeOperation(titleUpdateOpId, 
                                    Map.of(
                                        "success", true,
                                        "message", "Title unchanged (already matches)",
                                        "noChangeRequired", true
                                    ));
                            }
                        } catch (Exception e) {
                            metadataService.failOperation(titleUpdateOpId, e);
                            throw e;
                        }
                    }
                    
                    // Process description changes
                    if (updates.containsKey("set-description")) {
                        String descUpdateOpId = metadataService.startOperation(
                            "bulk-update-item-description", "UPDATE", 
                            Map.of(
                                "itemId", item.getId(),
                                "parentOperationId", itemUpdateOpId
                            ));
                            
                        try {
                            String newDescription = updates.get("set-description");
                            String currentDescription = item.getDescription() != null ? item.getDescription() : "";
                            
                            if (!newDescription.equals(currentDescription)) {
                                itemService.updateDescription(itemId, newDescription, currentUser);
                                
                                updateCounts.put("description", updateCounts.getOrDefault("description", 0) + 1);
                                totalUpdates++;
                                itemModified = true;
                                
                                // Update our local object
                                item.setDescription(newDescription);
                                
                                // Record the successful update
                                itemUpdates.put("description", Map.of(
                                    "updated", true,
                                    "success", true
                                ));
                                
                                // Complete description update operation
                                metadataService.completeOperation(descUpdateOpId, 
                                    Map.of(
                                        "success", true,
                                        "updated", true
                                    ));
                            } else {
                                // No change needed - description already matches
                                metadataService.completeOperation(descUpdateOpId, 
                                    Map.of(
                                        "success", true,
                                        "message", "Description unchanged (already matches)",
                                        "noChangeRequired", true
                                    ));
                            }
                        } catch (Exception e) {
                            metadataService.failOperation(descUpdateOpId, e);
                            throw e;
                        }
                    }
                    
                    // Process custom field updates
                    if (updates.keySet().stream().anyMatch(key -> key.startsWith("field-"))) {
                        String customFieldsUpdateOpId = metadataService.startOperation(
                            "bulk-update-item-custom-fields", "UPDATE", 
                            Map.of(
                                "itemId", item.getId(),
                                "parentOperationId", itemUpdateOpId
                            ));
                            
                        try {
                            Map<String, String> customFields = new HashMap<>();
                            for (Map.Entry<String, String> update : updates.entrySet()) {
                                // Check if this is a custom field update (they start with "field-")
                                if (update.getKey().startsWith("field-")) {
                                    String fieldName = update.getKey().substring("field-".length());
                                    String fieldValue = update.getValue();
                                    
                                    customFields.put(fieldName, fieldValue);
                                }
                            }
                            
                            // Apply custom field updates if any
                            if (!customFields.isEmpty()) {
                                itemService.updateCustomFields(item.getId(), customFields);
                                
                                updateCounts.put("custom-fields", updateCounts.getOrDefault("custom-fields", 0) + 1);
                                totalUpdates++;
                                itemModified = true;
                                
                                // Record the successful update
                                itemUpdates.put("customFields", Map.of(
                                    "count", customFields.size(),
                                    "fields", String.join(",", customFields.keySet()),
                                    "success", true
                                ));
                                
                                // Complete custom fields update operation
                                Map<String, Object> customFieldsResult = new HashMap<>();
                                customFieldsResult.put("success", true);
                                customFieldsResult.put("fieldCount", customFields.size());
                                customFieldsResult.put("fields", String.join(",", customFields.keySet()));
                                
                                metadataService.completeOperation(customFieldsUpdateOpId, customFieldsResult);
                            } else {
                                // No custom fields to update
                                metadataService.completeOperation(customFieldsUpdateOpId, 
                                    Map.of(
                                        "success", true,
                                        "message", "No custom fields to update",
                                        "noChangeRequired", true
                                    ));
                            }
                        } catch (Exception e) {
                            metadataService.failOperation(customFieldsUpdateOpId, e);
                            throw e;
                        }
                    }
                    
                    // Complete the item update operation
                    Map<String, Object> itemResult = new HashMap<>();
                    itemResult.put("itemId", item.getId());
                    itemResult.put("modified", itemModified);
                    itemResult.put("updateCount", itemUpdates.size());
                    itemResult.put("success", true);
                    
                    if (itemModified) {
                        // Track item update in metadata service
                        Map<String, String> data = new HashMap<>();
                        data.put("itemId", item.getId());
                        data.put("user", currentUser);
                        data.put("action", "Bulk update applied to item");
                        data.put("updateTypes", String.join(",", itemUpdates.keySet()));
                        
                        metadataService.trackOperationWithData(
                            "bulk-update", 
                            data
                        );
                        
                        if (verbose) {
                            System.out.println("Updated: " + item.getId() + " - " + item.getTitle());
                        }
                    }
                    
                    metadataService.completeOperation(itemUpdateOpId, itemResult);
                } catch (Exception e) {
                    metadataService.failOperation(itemUpdateOpId, e);
                    System.err.println("Error updating " + item.getId() + ": " + e.getMessage());
                }
            }
            
            // Complete each update type operation
            for (Map.Entry<String, String> entry : updateTypeOperations.entrySet()) {
                String updateType = entry.getKey();
                String updateTypeOpId = entry.getValue();
                
                int fieldUpdateCount = updateCounts.getOrDefault(
                    updateType.replace("set-", "").replace("field-", "custom-fields"), 0);
                
                Map<String, Object> updateTypeResult = new HashMap<>();
                updateTypeResult.put("updateType", updateType);
                updateTypeResult.put("updateCount", fieldUpdateCount);
                updateTypeResult.put("success", true);
                
                metadataService.completeOperation(updateTypeOpId, updateTypeResult);
            }
            
            // Complete apply updates operation
            Map<String, Object> applyResult = new HashMap<>();
            applyResult.put("totalUpdates", totalUpdates);
            applyResult.put("success", true);
            
            // Add all update counts
            for (Map.Entry<String, Integer> entry : updateCounts.entrySet()) {
                applyResult.put("field." + entry.getKey(), entry.getValue());
            }
            
            metadataService.completeOperation(applyUpdatesOpId, applyResult);
            
            return totalUpdates;
        } catch (Exception e) {
            metadataService.failOperation(applyUpdatesOpId, e);
            throw e; // Rethrow for higher level handling
        }
    }
    
}
