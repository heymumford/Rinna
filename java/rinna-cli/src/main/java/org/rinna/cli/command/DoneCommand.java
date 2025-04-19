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

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.InvalidTransitionException;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Command to mark a work item as done.
 */
public class DoneCommand implements Callable<Integer> {
    
    private String itemId;
    private String comment;
    private String format = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final MockWorkflowService workflowService;
    private final ConfigurationService configService;
    private final MetadataService metadataService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Default constructor for picocli.
     */
    public DoneCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Constructor with ServiceManager for testing.
     *
     * @param serviceManager The service manager
     */
    public DoneCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.workflowService = serviceManager.getMockWorkflowService();
        this.configService = serviceManager.getConfigurationService();
        this.metadataService = serviceManager.getMetadataService();
    }
    
    @Override
    public Integer call() {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("itemId", itemId);
        if (comment != null && !comment.isEmpty()) {
            params.put("comment", comment);
        }
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking operation
        String operationId = metadataService.startOperation("done", "UPDATE", params);
        
        // Validate input
        if (itemId == null || itemId.isEmpty()) {
            String errorMessage = "Work item ID is required";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        try {
            // Try to parse the ID as a UUID
            UUID uuid;
            try {
                uuid = UUID.fromString(itemId);
            } catch (IllegalArgumentException e) {
                String errorMessage = "Invalid work item ID format: " + itemId;
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, e);
                return 1;
            }
            
            // Verify the transition is valid
            if (!workflowService.canTransition(itemId, WorkflowState.DONE)) {
                String errorMessage = "Cannot transition work item to DONE state";
                System.err.println("Error: " + errorMessage);
                System.err.println("Valid transitions: " + workflowService.getAvailableTransitions(itemId));
                metadataService.failOperation(operationId, new InvalidTransitionException(errorMessage));
                return 1;
            }
            
            // Transition the work item to DONE state
            WorkItem updatedItem;
            try {
                if (comment != null && !comment.isEmpty()) {
                    // Use the user name from the configuration service
                    String user = configService.getCurrentUser();
                    if (user == null) {
                        user = "anonymous";
                    }
                    
                    updatedItem = workflowService.transition(itemId, user, WorkflowState.DONE, comment);
                } else {
                    updatedItem = workflowService.transition(itemId, WorkflowState.DONE);
                }
                
                // Create result data for operation tracking
                Map<String, Object> result = new HashMap<>();
                result.put("itemId", itemId);
                result.put("title", updatedItem.getTitle());
                result.put("newState", updatedItem.getStatus().toString());
                result.put("completionDate", updatedItem.getUpdated() != null ? 
                    updatedItem.getUpdated().format(dateFormatter) : 
                    java.time.LocalDateTime.now().format(dateFormatter));
                
                // Update context with the updated item
                ContextManager.getInstance().setLastViewedItem(updatedItem);
                
                // Output the result based on format
                if ("json".equalsIgnoreCase(format)) {
                    displayResultJson(updatedItem, result, operationId);
                } else {
                    displayResultText(updatedItem, result, operationId);
                }
                
                return 0;
            } catch (InvalidTransitionException e) {
                String errorMessage = e.getMessage();
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, e);
                return 1;
            }
        } catch (Exception e) {
            String errorMessage = "Error transitioning work item: " + e.getMessage();
            System.err.println(errorMessage);
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Display the result in text format.
     *
     * @param updatedItem The updated work item
     * @param result The result data for tracking
     * @param operationId The operation ID
     */
    private void displayResultText(WorkItem updatedItem, Map<String, Object> result, String operationId) {
        System.out.println("Work item " + itemId + " marked as DONE");
        System.out.println("Title: " + updatedItem.getTitle());
        System.out.println("Updated state: " + updatedItem.getStatus());
        
        String completionDate = (String) result.get("completionDate");
        System.out.println("Completion date: " + completionDate);
        
        // Complete the operation
        metadataService.completeOperation(operationId, result);
    }
    
    /**
     * Display the result in JSON format.
     *
     * @param updatedItem The updated work item
     * @param result The result data for tracking
     * @param operationId The operation ID
     */
    private void displayResultJson(WorkItem updatedItem, Map<String, Object> result, String operationId) {
        Map<String, Object> jsonData = new HashMap<>(result);
        
        // Add additional information for verbose output
        if (verbose) {
            jsonData.put("type", updatedItem.getType());
            jsonData.put("priority", updatedItem.getPriority());
            jsonData.put("description", updatedItem.getDescription());
            jsonData.put("created", updatedItem.getCreated() != null ? 
                updatedItem.getCreated().format(dateFormatter) : null);
        }
        
        // Convert to JSON and print
        String json = OutputFormatter.toJson(jsonData, verbose);
        System.out.println(json);
        
        // Complete the operation
        metadataService.completeOperation(operationId, result);
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
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
