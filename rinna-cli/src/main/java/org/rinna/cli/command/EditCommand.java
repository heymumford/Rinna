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

import org.rinna.cli.domain.service.ItemService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.util.OutputFormatter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Command to edit a work item interactively.
 * Uses the last viewed work item by default, or accepts an explicit ID.
 * Follows the ViewCommand pattern with operation tracking.
 */
public class EditCommand implements Callable<Integer> {
    
    private String itemId;
    private String idParameter;
    private boolean force;
    private String format = "text";
    private boolean verbose = false;
    private String username = System.getProperty("user.name");
    
    private final ServiceManager serviceManager;
    private final MockItemService itemService;
    private final ContextManager contextManager;
    private final MetadataService metadataService;
    
    /**
     * Default constructor for picocli.
     */
    public EditCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Constructor with ServiceManager for testing.
     *
     * @param serviceManager The service manager
     */
    public EditCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.itemService = (MockItemService) serviceManager.getItemService();
        this.contextManager = ContextManager.getInstance();
        this.metadataService = serviceManager.getMetadataService();
    }
    
    /**
     * Sets the work item ID to edit.
     *
     * @param itemId the work item ID
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    /**
     * Sets the work item ID from the id= parameter.
     *
     * @param idParameter the id= parameter value
     */
    public void setIdParameter(String idParameter) {
        this.idParameter = idParameter;
    }
    
    /**
     * Sets the force flag.
     *
     * @param force true to skip confirmation prompts
     */
    public void setForce(boolean force) {
        this.force = force;
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
     * Sets the JSON output flag.
     *
     * @param jsonOutput true for JSON output, false for text
     */
    public void setJsonOutput(boolean jsonOutput) {
        this.format = jsonOutput ? "json" : "text";
    }
    
    /**
     * Sets whether to use verbose output.
     *
     * @param verbose true for verbose output, false for concise
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    @Override
    public Integer call() {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        params.put("force", force);
        params.put("format", format);
        params.put("verbose", verbose);
        
        if (itemId != null) {
            params.put("itemId", itemId);
        }
        if (idParameter != null) {
            params.put("idParameter", idParameter);
        }
        
        // Start tracking operation
        String operationId = metadataService.startOperation("edit", "UPDATE", params);
        
        try {
            // Determine the work item ID to edit
            UUID workItemId = determineWorkItemId(operationId);
            if (workItemId == null) {
                return 1;
            }
            
            // Get the work item
            WorkItem workItem = itemService.getItem(workItemId.toString());
            if (workItem == null) {
                String errorMessage = "Work item not found: " + workItemId;
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            // Update context with the viewed item
            contextManager.setLastViewedItem(workItem);
            
            // Display the work item and prompt for changes
            return interactiveEdit(workItem, operationId);
            
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            System.err.println("Error: " + errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Determine which work item ID to edit based on parameters and context.
     *
     * @param operationId the operation ID for tracking
     * @return the UUID of the work item to edit, or null if not found
     */
    private UUID determineWorkItemId(String operationId) {
        // First priority: explicit ID as a regular parameter
        if (itemId != null && !itemId.isEmpty()) {
            try {
                return UUID.fromString(itemId);
            } catch (IllegalArgumentException e) {
                String errorMessage = "Invalid work item ID format: " + itemId;
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, e);
                return null;
            }
        } 
        // Second priority: ID from id= parameter
        else if (idParameter != null && !idParameter.isEmpty()) {
            if (idParameter.startsWith("id=")) {
                String id = idParameter.substring(3);
                try {
                    return UUID.fromString(id);
                } catch (IllegalArgumentException e) {
                    String errorMessage = "Invalid work item ID format in parameter: " + id;
                    System.err.println("Error: " + errorMessage);
                    metadataService.failOperation(operationId, e);
                    return null;
                }
            } else {
                String errorMessage = "Invalid parameter format. Expected id=UUID";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return null;
            }
        } 
        // Last priority: last viewed work item
        else {
            UUID workItemId = contextManager.getLastViewedWorkItem();
            if (workItemId == null) {
                String errorMessage = "No work item context available. Please specify an ID with id=X";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalStateException(errorMessage));
                return null;
            }
            return workItemId;
        }
    }
    
    /**
     * Interactively edits a work item.
     *
     * @param workItem the work item to edit
     * @param operationId the operation ID for tracking
     * @return exit code
     */
    private Integer interactiveEdit(WorkItem workItem, String operationId) {
        // Display work item information based on format
        if ("json".equalsIgnoreCase(format)) {
            displayWorkItemJson(workItem);
        } else {
            displayWorkItemText(workItem);
        }
        
        // Get user input
        Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8.name());
        String input = scanner.nextLine().trim();
        
        if (input.isEmpty()) {
            String errorMessage = "No selection made";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        try {
            int selection = Integer.parseInt(input);
            
            if (selection == 0) {
                System.out.println("Update cancelled");
                Map<String, Object> result = new HashMap<>();
                result.put("action", "cancel");
                result.put("itemId", workItem.getId());
                metadataService.completeOperation(operationId, result);
                return 0;
            }
            
            if (selection < 1 || selection > 5) {
                String errorMessage = "Invalid selection";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            // Get the field name for logging
            String fieldName = getFieldName(selection);
            
            // Create a suboperation for the specific field update
            Map<String, Object> fieldParams = new HashMap<>();
            fieldParams.put("workItemId", workItem.getId());
            fieldParams.put("field", fieldName);
            fieldParams.put("oldValue", getFieldValue(workItem, selection));
            
            System.out.println("Enter new value: ");
            String newValue = scanner.nextLine().trim();
            
            if (newValue.isEmpty()) {
                String errorMessage = "Empty value not allowed";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            fieldParams.put("newValue", newValue);
            
            String fieldOperationId = metadataService.trackOperation("edit-field", fieldParams);
            
            try {
                // Apply the change based on selection
                WorkItem updatedItem = updateField(workItem, selection, newValue, fieldOperationId);
                
                if (updatedItem != null) {
                    // Update the context with the updated item
                    contextManager.setLastViewedItem(updatedItem);
                    
                    // Prepare result data
                    Map<String, Object> result = new HashMap<>();
                    result.put("itemId", updatedItem.getId());
                    result.put("field", fieldName);
                    result.put("oldValue", getFieldValue(workItem, selection));
                    result.put("newValue", getFieldValue(updatedItem, selection));
                    result.put("success", true);
                    
                    // Output the result based on format
                    if ("json".equalsIgnoreCase(format)) {
                        result.put("workItem", createWorkItemMap(updatedItem, verbose));
                        System.out.println(OutputFormatter.toJson(result, verbose));
                    } else {
                        System.out.println(fieldName + " updated successfully");
                    }
                    
                    // Complete both operations
                    metadataService.completeOperation(fieldOperationId, result);
                    metadataService.completeOperation(operationId, result);
                    
                    return 0;
                } else {
                    String errorMessage = "Failed to update " + fieldName;
                    System.err.println("Error: " + errorMessage);
                    metadataService.failOperation(fieldOperationId, new RuntimeException(errorMessage));
                    metadataService.failOperation(operationId, new RuntimeException(errorMessage));
                    return 1;
                }
            } catch (Exception e) {
                String errorMessage = e.getMessage();
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(fieldOperationId, e);
                metadataService.failOperation(operationId, e);
                return 1;
            }
        } catch (NumberFormatException e) {
            String errorMessage = "Invalid selection";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Displays work item information in text format.
     *
     * @param workItem the work item to display
     */
    private void displayWorkItemText(WorkItem workItem) {
        System.out.println("Work Item: " + workItem.getId());
        System.out.println("[1] Title: " + workItem.getTitle());
        System.out.println("[2] Description: " + workItem.getDescription());
        System.out.println("[3] Priority: " + workItem.getPriority());
        System.out.println("[4] State: " + workItem.getState());
        System.out.println("[5] Assignee: " + workItem.getAssignee());
        System.out.println("[0] Cancel");
        System.out.println();
        System.out.println("Enter the number of the field to update: ");
    }
    
    /**
     * Displays work item information in JSON format.
     *
     * @param workItem the work item to display
     */
    private void displayWorkItemJson(WorkItem workItem) {
        Map<String, Object> jsonData = createWorkItemMap(workItem, verbose);
        
        // Add action options
        Map<String, String> actions = new HashMap<>();
        actions.put("1", "Update Title");
        actions.put("2", "Update Description");
        actions.put("3", "Update Priority");
        actions.put("4", "Update State");
        actions.put("5", "Update Assignee");
        actions.put("0", "Cancel");
        
        jsonData.put("actions", actions);
        jsonData.put("prompt", "Enter the number of the field to update");
        
        System.out.println(OutputFormatter.toJson(jsonData, verbose));
        System.out.println("Enter the number of the field to update: ");
    }
    
    /**
     * Creates a map of work item data for JSON output.
     *
     * @param workItem the work item
     * @param verbose whether to include detailed information
     * @return a map of work item data
     */
    private Map<String, Object> createWorkItemMap(WorkItem workItem, boolean verbose) {
        Map<String, Object> data = new HashMap<>();
        data.put("id", workItem.getId());
        data.put("title", workItem.getTitle());
        data.put("description", workItem.getDescription());
        data.put("priority", workItem.getPriority());
        data.put("state", workItem.getState());
        data.put("assignee", workItem.getAssignee());
        
        if (verbose) {
            data.put("type", workItem.getType());
            data.put("project", workItem.getProject());
            data.put("created", workItem.getCreated() != null ? workItem.getCreated().toString() : null);
            data.put("updated", workItem.getUpdated() != null ? workItem.getUpdated().toString() : null);
            data.put("reporter", workItem.getReporter());
            data.put("version", workItem.getVersion());
        }
        
        return data;
    }
    
    /**
     * Gets the name of a field based on its selection number.
     *
     * @param selection the selection number
     * @return the field name
     */
    private String getFieldName(int selection) {
        switch (selection) {
            case 1: return "Title";
            case 2: return "Description";
            case 3: return "Priority";
            case 4: return "State";
            case 5: return "Assignee";
            default: return "Unknown";
        }
    }
    
    /**
     * Gets the value of a field based on its selection number.
     *
     * @param workItem the work item
     * @param selection the selection number
     * @return the field value
     */
    private String getFieldValue(WorkItem workItem, int selection) {
        switch (selection) {
            case 1: return workItem.getTitle();
            case 2: return workItem.getDescription();
            case 3: return workItem.getPriority() != null ? workItem.getPriority().toString() : "null";
            case 4: return workItem.getState() != null ? workItem.getState().toString() : "null";
            case 5: return workItem.getAssignee();
            default: return "Unknown";
        }
    }
    
    /**
     * Updates a field of a work item.
     *
     * @param workItem the work item to update
     * @param selection the selection number
     * @param newValue the new value
     * @param fieldOperationId the operation ID for field update tracking
     * @return the updated work item, or null if update fails
     */
    private WorkItem updateField(WorkItem workItem, int selection, String newValue, String fieldOperationId) {
        UUID id = UUID.fromString(workItem.getId());
        
        try {
            switch (selection) {
                case 1: // Title
                    return itemService.updateTitle(id, newValue, username);
                case 2: // Description
                    return itemService.updateDescription(id, newValue, username);
                case 3: // Priority
                    try {
                        Priority priority = Priority.valueOf(newValue.toUpperCase());
                        return itemService.updatePriority(id, priority, username);
                    } catch (IllegalArgumentException e) {
                        String errorMessage = "Invalid priority value. Must be one of: " + 
                                Arrays.toString(Priority.values());
                        System.err.println("Error: " + errorMessage);
                        metadataService.failOperation(fieldOperationId, e);
                        return null;
                    }
                case 4: // State
                    try {
                        WorkflowState state = WorkflowState.valueOf(newValue.toUpperCase());
                        return itemService.updateState(id, state, username);
                    } catch (IllegalArgumentException e) {
                        String errorMessage = "Invalid state value. Must be one of: " + 
                                Arrays.toString(WorkflowState.values());
                        System.err.println("Error: " + errorMessage);
                        metadataService.failOperation(fieldOperationId, e);
                        return null;
                    }
                case 5: // Assignee
                    return itemService.assignTo(id, newValue, username);
                default:
                    String errorMessage = "Invalid selection";
                    System.err.println("Error: " + errorMessage);
                    metadataService.failOperation(fieldOperationId, new IllegalArgumentException(errorMessage));
                    return null;
            }
        } catch (Exception e) {
            String errorMessage = "Error updating field: " + e.getMessage();
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(fieldOperationId, e);
            return null;
        }
    }
}