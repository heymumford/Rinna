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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.ModelMapper;
import org.rinna.cli.util.OutputFormatter;

/**
 * Command to view details of a work item.
 * This command retrieves and displays information about a specific work item.
 * Format options include text (default) and JSON.
 * 
 * Usage examples:
 * - rin view [item-id]
 * - rin view --format=json [item-id]
 * - rin view --verbose [item-id]
 */
public class ViewCommand implements Callable<Integer> {
    
    private String id;
    private String format = "text";
    private boolean verbose;
    
    private final ServiceManager serviceManager;
    private final MetadataService metadataService;
    
    /**
     * Creates a new ViewCommand with default services.
     */
    public ViewCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new ViewCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public ViewCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.metadataService = serviceManager.getMetadataService();
    }
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("item_id", id);
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("view", "READ", params);
        
        try {
            if (id == null || id.isEmpty()) {
                System.err.println("Error: Work item ID is required");
                metadataService.failOperation(operationId, new IllegalArgumentException("Missing work item ID"));
                return 1;
            }
            
            // Get the item service from the service manager
            ItemService itemService = serviceManager.getItemService();
            
            // Try to parse the ID as a UUID
            UUID uuid;
            try {
                uuid = ModelMapper.toUUID(id);
            } catch (IllegalArgumentException e) {
                System.err.println("Error: Invalid work item ID format: " + id);
                metadataService.failOperation(operationId, e);
                return 1;
            }
            
            // Get the work item from the service
            WorkItem workItem = itemService.getItem(uuid.toString());
            
            if (workItem != null) {
                // Output based on format
                if ("json".equalsIgnoreCase(format)) {
                    // Use the OutputFormatter for consistent JSON output
                    String jsonOutput = OutputFormatter.toJson(workItem, verbose);
                    System.out.println(jsonOutput);
                } else {
                    // Text format with improved layout
                    displayTextOutput(workItem);
                }
                
                // Record the successful operation
                Map<String, Object> result = new HashMap<>();
                result.put("item_id", workItem.getId());
                result.put("title", workItem.getTitle());
                result.put("status", workItem.getStatus() != null ? workItem.getStatus().toString() : null);
                
                metadataService.completeOperation(operationId, result);
                return 0;
            } else {
                String errorMessage = "Work item not found with ID: " + id;
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error retrieving work item: " + e.getMessage();
            System.err.println(errorMessage);
            
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
     * Displays work item information in text format.
     * 
     * @param workItem the work item to display
     */
    private void displayTextOutput(WorkItem workItem) {
        System.out.println("Work Item: " + workItem.getId());
        System.out.println("Title: " + workItem.getTitle());
        System.out.println("Status: " + (workItem.getStatus() != null ? workItem.getStatus() : "Not set"));
        System.out.println("Type: " + (workItem.getType() != null ? workItem.getType() : "Not set"));
        System.out.println("Priority: " + (workItem.getPriority() != null ? workItem.getPriority() : "Not set"));
        System.out.println("Assignee: " + (workItem.getAssignee() != null ? workItem.getAssignee() : "Unassigned"));
        
        // Show additional details in verbose mode
        if (verbose) {
            System.out.println("\nDescription:");
            System.out.println(workItem.getDescription() != null ? workItem.getDescription() : "No description");
            System.out.println("\nReporter: " + (workItem.getReporter() != null ? workItem.getReporter() : "Unknown"));
            System.out.println("Created: " + (workItem.getCreated() != null ? workItem.getCreated() : "Unknown"));
            System.out.println("Updated: " + (workItem.getUpdated() != null ? workItem.getUpdated() : "Unknown"));
            
            // Show project information if available
            if (workItem.getProject() != null) {
                System.out.println("Project: " + workItem.getProject());
            }
            
            // Show due date if available
            if (workItem.getDueDate() != null) {
                System.out.println("Due Date: " + workItem.getDueDate());
            }
            
            // Show version if available
            if (workItem.getVersion() != null) {
                System.out.println("Version: " + workItem.getVersion());
            }
        }
    }
    
    /**
     * Gets the work item ID to view.
     *
     * @return the work item ID
     */
    public String getId() {
        return id;
    }
    
    /**
     * Sets the work item ID to view.
     *
     * @param id the work item ID
     */
    public void setId(String id) {
        this.id = id;
    }
    
    /**
     * Gets the output format.
     *
     * @return the output format
     */
    public String getFormat() {
        return format;
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
     * Gets whether verbose output is enabled.
     *
     * @return true if verbose output is enabled
     */
    public boolean isVerbose() {
        return verbose;
    }
    
    /**
     * Sets whether verbose output is enabled.
     *
     * @param verbose true to enable verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}
