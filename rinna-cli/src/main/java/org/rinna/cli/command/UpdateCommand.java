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

import org.rinna.cli.domain.model.DomainWorkItem;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.ModelMapper;
import org.rinna.cli.util.OutputFormatter;

/**
 * Command to update an existing work item.
 * This command allows updating various fields of a work item and transitioning its state.
 * 
 * Usage examples:
 * - rin update WI-123 --title="Updated title"
 * - rin update WI-123 --description="New description"
 * - rin update WI-123 --priority=HIGH
 * - rin update WI-123 --status=IN_PROGRESS
 * - rin update WI-123 --assignee=john.doe
 * - rin update WI-123 --comment="Updating for project needs"
 * - rin update WI-123 --title="Updated title" --format=json
 * - rin update WI-123 --priority=HIGH --verbose
 */
public class UpdateCommand implements Callable<Integer> {
    
    private String id;
    private String title;
    private String description;
    private WorkItemType type;
    private Priority priority;
    private WorkflowState status;
    private String assignee;
    private String comment;
    private String format = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final MockItemService itemService;
    private final MockWorkflowService workflowService;
    private final ConfigurationService configService;
    private final MetadataService metadataService;
    
    /**
     * Creates a new UpdateCommand with default services.
     */
    public UpdateCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new UpdateCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public UpdateCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.itemService = serviceManager.getMockItemService();
        this.workflowService = serviceManager.getMockWorkflowService();
        this.configService = serviceManager.getConfigurationService();
        this.metadataService = serviceManager.getMetadataService();
    }
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("id", id);
        if (title != null) params.put("title", title);
        if (description != null) params.put("description", description.length() > 50 ? description.substring(0, 47) + "..." : description);
        if (type != null) params.put("type", type.name());
        if (priority != null) params.put("priority", priority.name());
        if (status != null) params.put("status", status.name());
        if (assignee != null) params.put("assignee", assignee);
        if (comment != null) params.put("comment", comment.length() > 50 ? comment.substring(0, 47) + "..." : comment);
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("update", "UPDATE", params);
        
        try {
            // Validate inputs
            if (id == null || id.isEmpty()) {
                String errorMessage = "Work item ID is required";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            // Verify at least one field is being updated
            if (title == null && description == null && type == null && 
                priority == null && status == null && assignee == null) {
                String errorMessage = "No updates specified";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            // Execute the update
            WorkItem updatedItem = performUpdate(operationId);
            
            // Output the result
            if ("json".equalsIgnoreCase(format)) {
                return outputJson(updatedItem, operationId);
            } else {
                return outputText(updatedItem, operationId);
            }
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error updating work item: " + e.getMessage();
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
    
    /**
     * Performs the update operation on the work item.
     *
     * @param operationId the operation ID for tracking
     * @return the updated work item
     * @throws Exception if an error occurs during the update
     */
    private WorkItem performUpdate(String operationId) throws Exception {
        // Try to parse the ID as a UUID
        UUID uuid;
        try {
            uuid = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            String errorMessage = "Invalid work item ID format: " + id;
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            throw new IllegalArgumentException(errorMessage);
        }
        
        // Get the current work item
        WorkItem item = itemService.getItem(id);
        if (item == null) {
            String errorMessage = "Work item not found with ID: " + id;
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            throw new IllegalArgumentException(errorMessage);
        }
        
        boolean stateChanged = false;
        
        // Update the fields
        if (title != null) {
            item.setTitle(title);
        }
        
        if (description != null) {
            item.setDescription(description);
        }
        
        if (type != null) {
            item.setType(type);
        }
        
        if (priority != null) {
            item.setPriority(priority);
        }
        
        // Check if the status needs to be updated
        if (status != null && !status.equals(item.getStatus())) {
            stateChanged = true;
        }
        
        if (assignee != null) {
            item.setAssignee(assignee);
        }
        
        WorkItem updatedItem;
        
        // If the state has changed, use the workflow service to transition
        if (stateChanged) {
            // Get current user
            String user = configService.getCurrentUser();
            if (user == null || user.isEmpty()) {
                user = "anonymous";
            }
            
            // Verify we can transition to the new state
            if (!workflowService.canTransition(id, status)) {
                String errorMessage = "Cannot transition work item to " + status + " state. Valid transitions: " 
                        + workflowService.getAvailableTransitions(id);
                metadataService.failOperation(operationId, new IllegalStateException(errorMessage));
                throw new IllegalStateException(errorMessage);
            }
            
            try {
                // Transition with comment if provided
                if (comment != null && !comment.isEmpty()) {
                    updatedItem = workflowService.transition(id, user, status, comment);
                } else {
                    updatedItem = workflowService.transition(id, status);
                }
            } catch (org.rinna.cli.service.InvalidTransitionException e) {
                String errorMessage = "Invalid transition: " + e.getMessage(); 
                metadataService.failOperation(operationId, new IllegalStateException(errorMessage));
                throw new IllegalStateException(errorMessage);
            }
        } else {
            // Just update the fields
            updatedItem = itemService.updateItem(item);
        }
        
        return updatedItem;
    }
    
    /**
     * Outputs the updated work item in JSON format.
     *
     * @param item the updated work item
     * @param operationId the operation ID for tracking
     * @return 0 for success, non-zero for failure
     */
    private Integer outputJson(WorkItem item, String operationId) {
        // Create a map of the updated fields
        Map<String, Object> updatedFields = new HashMap<>();
        if (title != null) updatedFields.put("title", item.getTitle());
        if (description != null) updatedFields.put("description", item.getDescription());
        if (type != null) updatedFields.put("type", item.getType());
        if (priority != null) updatedFields.put("priority", item.getPriority());
        if (status != null) updatedFields.put("status", item.getStatus());
        if (assignee != null) updatedFields.put("assignee", item.getAssignee());
        
        Map<String, Object> result = new HashMap<>();
        result.put("id", item.getId());
        result.put("updatedFields", updatedFields);
        result.put("workItem", ModelMapper.toDomainWorkItem(item));
        
        // Use the OutputFormatter for consistent JSON output
        String json = OutputFormatter.toJson(result, verbose);
        System.out.println(json);
        
        // Record the successful operation
        Map<String, Object> operationResult = new HashMap<>();
        operationResult.put("itemId", item.getId());
        operationResult.put("updatedFields", new ArrayList<>(updatedFields.keySet()));
        if (status != null) {
            operationResult.put("stateChanged", true);
            operationResult.put("newState", status.name());
        }
        
        metadataService.completeOperation(operationId, operationResult);
        return 0;
    }
    
    /**
     * Outputs the updated work item in text format.
     *
     * @param item the updated work item
     * @param operationId the operation ID for tracking
     * @return 0 for success, non-zero for failure
     */
    private Integer outputText(WorkItem item, String operationId) {
        System.out.println("Updated work item: " + item.getId());
        System.out.println("Title: " + item.getTitle());
        
        if (type != null) {
            System.out.println("Type: " + item.getType());
        }
        
        if (priority != null) {
            System.out.println("Priority: " + item.getPriority());
        }
        
        if (status != null) {
            System.out.println("Status: " + item.getStatus());
        }
        
        if (assignee != null) {
            System.out.println("Assignee: " + (item.getAssignee() != null ? item.getAssignee() : "-"));
        }
        
        if (verbose) {
            if (description != null) {
                System.out.println("\nDescription:");
                System.out.println(item.getDescription());
            }
            
            if (item.getReporter() != null) {
                System.out.println("Reporter: " + item.getReporter());
            }
            if (item.getCreated() != null) {
                System.out.println("Created: " + item.getCreated());
            }
            if (item.getUpdated() != null) {
                System.out.println("Updated: " + item.getUpdated());
            }
            
            System.out.println("\nOperation ID: " + operationId);
        }
        
        // Record the successful operation
        Map<String, Object> operationResult = new HashMap<>();
        operationResult.put("itemId", item.getId());
        
        // Track which fields were updated
        Map<String, Object> updatedFields = new HashMap<>();
        if (title != null) updatedFields.put("title", item.getTitle());
        if (description != null) updatedFields.put("description", "updated");
        if (type != null) updatedFields.put("type", item.getType().name());
        if (priority != null) updatedFields.put("priority", item.getPriority().name());
        if (status != null) updatedFields.put("status", item.getStatus().name());
        if (assignee != null) updatedFields.put("assignee", item.getAssignee());
        
        operationResult.put("updatedFields", new ArrayList<>(updatedFields.keySet()));
        
        if (status != null) {
            operationResult.put("stateChanged", true);
            operationResult.put("newState", status.name());
        }
        
        metadataService.completeOperation(operationId, operationResult);
        return 0;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public WorkItemType getType() {
        return type;
    }
    
    public void setType(WorkItemType type) {
        this.type = type;
    }
    
    public Priority getPriority() {
        return priority;
    }
    
    public void setPriority(Priority priority) {
        this.priority = priority;
    }
    
    public WorkflowState getStatus() {
        return status;
    }
    
    public void setStatus(WorkflowState status) {
        this.status = status;
    }
    
    // For backward compatibility
    public WorkflowState getState() {
        return status;
    }
    
    public void setState(WorkflowState state) {
        this.status = state;
    }
    
    public String getAssignee() {
        return assignee;
    }
    
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
    
    public String getComment() {
        return comment;
    }
    
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    /**
     * Sets the JSON output flag.
     * This is a backwards compatibility method for the --json flag.
     *
     * @param jsonOutput true to output in JSON format, false for text
     */
    public void setJsonOutput(boolean jsonOutput) {
        if (jsonOutput) {
            this.format = "json";
        }
    }
    
    /**
     * Gets whether JSON output is enabled.
     *
     * @return true if JSON output is enabled
     */
    public boolean isJsonOutput() {
        return "json".equalsIgnoreCase(format);
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
