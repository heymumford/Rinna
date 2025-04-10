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
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockBacklogService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Command to manage the project backlog.
 * This command allows viewing, adding items to, prioritizing, and removing items from the backlog.
 *
 * Usage examples:
 * - rin backlog - Display all backlog items
 * - rin backlog list - Display all backlog items
 * - rin backlog add WI-123 - Add an item to the backlog
 * - rin backlog prioritize WI-123 --priority=HIGH - Set item priority
 * - rin backlog remove WI-123 - Remove an item from the backlog
 * - rin backlog list --format=json - View backlog in JSON format
 * - rin backlog list --verbose - Show detailed backlog information
 */
public class BacklogCommand implements Callable<Integer> {
    
    private String action;
    private String itemId;
    private Priority priority;
    private String format = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final MockBacklogService backlogService;
    private final MetadataService metadataService;
    
    /**
     * Creates a new BacklogCommand with default services.
     */
    public BacklogCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new BacklogCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public BacklogCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.backlogService = serviceManager.getMockBacklogService();
        this.metadataService = serviceManager.getMetadataService();
    }
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("action", action != null ? action : "list");
        if (itemId != null) params.put("item_id", itemId);
        if (priority != null) params.put("priority", priority.name());
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("backlog", "BACKLOG", params);
        
        try {
            if (action == null || action.isEmpty()) {
                // Default action is to list the backlog
                return listBacklog(operationId);
            }
            
            // Using switch with proper error handling
            switch (action.toLowerCase()) {
                case "list":
                    return listBacklog(operationId);
                case "add":
                    return addToBacklog(operationId);
                case "prioritize":
                case "priority":
                    return prioritizeBacklog(operationId);
                case "remove":
                    return removeFromBacklog(operationId);
                default:
                    String errorMessage = "Unknown backlog action: " + action;
                    System.err.println("Error: " + errorMessage);
                    System.err.println("Valid actions: list, add, prioritize, remove");
                    metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                    return 1;
            }
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error executing backlog command: " + e.getMessage();
            System.err.println("Error: " + e.getMessage());
            
            // Record detailed error information if verbose mode is enabled
            if (verbose) {
                e.printStackTrace();
            }
            
            // Record the failed operation with error details
            metadataService.failOperation(operationId, e);
            
            return 1;
        }
    }
    
    private Integer listBacklog(String operationId) {
        try {
            // In a real implementation, this would fetch backlog items from a service
            // For now, we'll just create mock data
            List<Map<String, Object>> backlogItems = new ArrayList<>();
            
            // Create mock data
            Map<String, Object> item1 = new HashMap<>();
            item1.put("id", "WI-123");
            item1.put("title", "Implement user authentication");
            item1.put("type", "FEATURE");
            item1.put("priority", "HIGH");
            backlogItems.add(item1);
            
            Map<String, Object> item2 = new HashMap<>();
            item2.put("id", "WI-124");
            item2.put("title", "Fix sorting in the reports page");
            item2.put("type", "BUG");
            item2.put("priority", "MEDIUM");
            backlogItems.add(item2);
            
            Map<String, Object> item3 = new HashMap<>();
            item3.put("id", "WI-125");
            item3.put("title", "Add export to CSV feature");
            item3.put("type", "FEATURE");
            item3.put("priority", "LOW");
            backlogItems.add(item3);
            
            Map<String, Object> item4 = new HashMap<>();
            item4.put("id", "WI-126");
            item4.put("title", "Improve application performance");
            item4.put("type", "TASK");
            item4.put("priority", "MEDIUM");
            backlogItems.add(item4);
            
            // Handle output based on format
            if ("json".equalsIgnoreCase(format)) {
                return outputBacklogAsJson(backlogItems, operationId);
            } else {
                return outputBacklogAsText(backlogItems, operationId);
            }
        } catch (Exception e) {
            String errorMessage = "Failed to list backlog items: " + e.getMessage();
            System.err.println("Error: " + errorMessage);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    private Integer outputBacklogAsText(List<Map<String, Object>> backlogItems, String operationId) {
        System.out.println("Backlog Items:");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-10s %-40s %-10s %-10s%n", 
                (Object)"ID", (Object)"TITLE", (Object)"TYPE", (Object)"PRIORITY");
        System.out.println("--------------------------------------------------------------------------------");
        
        for (Map<String, Object> item : backlogItems) {
            System.out.printf("%-10s %-40s %-10s %-10s%n", 
                    item.get("id"), item.get("title"), item.get("type"), item.get("priority"));
        }
        
        System.out.println("--------------------------------------------------------------------------------");
        System.out.println("Displaying " + backlogItems.size() + " of " + backlogItems.size() + " backlog item(s)");
        
        // Record successful operation
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("format", "text");
        resultData.put("count", backlogItems.size());
        metadataService.completeOperation(operationId, resultData);
        
        return 0;
    }
    
    private Integer outputBacklogAsJson(List<Map<String, Object>> backlogItems, String operationId) {
        Map<String, Object> output = new HashMap<>();
        output.put("count", backlogItems.size());
        output.put("items", backlogItems);
        
        System.out.println(OutputFormatter.toJson(output));
        
        // Record successful operation
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("format", "json");
        resultData.put("count", backlogItems.size());
        metadataService.completeOperation(operationId, resultData);
        
        return 0;
    }
    
    private Integer addToBacklog(String operationId) {
        try {
            if (itemId == null || itemId.isEmpty()) {
                String errorMessage = "Item ID is required for adding to backlog";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            // In a real implementation, this would add an item to the backlog via a service
            // Simulate success for mock implementation
            boolean success = backlogService.addToBacklog(UUID.fromString(itemId));
            
            if (success) {
                System.out.println("Item " + itemId + " added to backlog successfully");
                
                // Record successful operation
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("item_id", itemId);
                resultData.put("status", "added");
                metadataService.completeOperation(operationId, resultData);
                
                return 0;
            } else {
                String errorMessage = "Failed to add item " + itemId + " to backlog";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new RuntimeException(errorMessage));
                return 1;
            }
        } catch (Exception e) {
            String errorMessage = "Failed to add item to backlog: " + e.getMessage();
            System.err.println("Error: " + errorMessage);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    private Integer prioritizeBacklog(String operationId) {
        try {
            if (itemId == null || itemId.isEmpty()) {
                String errorMessage = "Item ID is required for prioritization";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            if (priority == null) {
                String errorMessage = "Priority is required for prioritization";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            // In a real implementation, this would update the item priority via a service
            // Simulate success for mock implementation
            boolean success = backlogService.setPriority(itemId, priority);
            
            if (success) {
                System.out.printf("Updated priority of %s to %s%n", (Object)itemId, (Object)priority);
                
                // Record successful operation
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("item_id", itemId);
                resultData.put("priority", priority.name());
                resultData.put("status", "updated");
                metadataService.completeOperation(operationId, resultData);
                
                return 0;
            } else {
                String errorMessage = "Failed to update priority for item " + itemId;
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new RuntimeException(errorMessage));
                return 1;
            }
        } catch (Exception e) {
            String errorMessage = "Failed to prioritize backlog item: " + e.getMessage();
            System.err.println("Error: " + errorMessage);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    private Integer removeFromBacklog(String operationId) {
        try {
            if (itemId == null || itemId.isEmpty()) {
                String errorMessage = "Item ID is required for removal";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            // In a real implementation, this would remove the item from the backlog via a service
            // Simulate success for mock implementation
            boolean success = backlogService.removeFromBacklog(itemId);
            
            if (success) {
                System.out.printf("Removed %s from backlog%n", (Object)itemId);
                
                // Record successful operation
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("item_id", itemId);
                resultData.put("status", "removed");
                metadataService.completeOperation(operationId, resultData);
                
                return 0;
            } else {
                String errorMessage = "Failed to remove item " + itemId + " from backlog";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new RuntimeException(errorMessage));
                return 1;
            }
        } catch (Exception e) {
            String errorMessage = "Failed to remove backlog item: " + e.getMessage();
            System.err.println("Error: " + errorMessage);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    public String getAction() {
        return action;
    }
    
    public void setAction(String action) {
        this.action = action;
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public Priority getPriority() {
        return priority;
    }
    
    public void setPriority(Priority priority) {
        this.priority = priority;
    }
    
    public String getFormat() {
        return format;
    }
    
    public void setFormat(String format) {
        this.format = format;
    }
    
    public boolean isVerbose() {
        return verbose;
    }
    
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}