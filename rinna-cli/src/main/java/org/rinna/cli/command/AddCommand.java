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
import java.util.concurrent.Callable;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Command to add a new work item to the system.
 * This command creates a new work item with the specified attributes.
 * Format options include text (default) and JSON.
 * 
 * Usage examples:
 * - rin add --title="Fix login issue"
 * - rin add --title="Update docs" --type=DOCUMENTATION
 * - rin add --title="Security vulnerability" --priority=HIGH
 * - rin add --title="Add feature" --description="Implement new feature X"
 * - rin add --title="Fix UI layout" --assignee=john.doe
 * - rin add --title="API enhancement" --project=backend
 * - rin add --title="New feature" --format=json
 * - rin add --title="Bug fix" --verbose
 */
public class AddCommand implements Callable<Integer> {
    
    private String title;
    private String description;
    private WorkItemType type = WorkItemType.TASK; // Default type
    private Priority priority = Priority.MEDIUM; // Default priority
    private String assignee;
    private String project;
    private String format = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final MetadataService metadataService;
    
    /**
     * Creates a new AddCommand with default services.
     */
    public AddCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new AddCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public AddCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.metadataService = serviceManager.getMetadataService();
    }
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("title", title);
        parameters.put("type", type != null ? type.toString() : null);
        parameters.put("priority", priority != null ? priority.toString() : null);
        if (description != null && !description.isEmpty()) {
            parameters.put("description", description.length() > 50 ? description.substring(0, 47) + "..." : description);
        }
        if (assignee != null && !assignee.isEmpty()) {
            parameters.put("assignee", assignee);
        }
        if (project != null && !project.isEmpty()) {
            parameters.put("project", project);
        }
        parameters.put("format", format);
        parameters.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("add", "CREATE", parameters);
        
        if (title == null || title.isEmpty()) {
            String errorMessage = "Title is required";
            System.err.println("Error: " + errorMessage);
            
            // Record the failed operation
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        try {
            // Get the item service from the service manager
            ItemService itemService = serviceManager.getMockItemService();
            ConfigurationService configService = serviceManager.getConfigurationService();
            
            // Create a new work item
            WorkItem item = new WorkItem();
            item.setTitle(title);
            item.setDescription(description != null ? description : "");
            item.setType(type);
            item.setPriority(priority);
            item.setStatus(WorkflowState.CREATED);
            
            // Set assignee if provided, otherwise use current user
            if (assignee != null && !assignee.isEmpty()) {
                item.setAssignee(assignee);
            } else {
                // Try to get current user from configuration
                String currentUser = configService.getCurrentUser();
                if (currentUser != null && !currentUser.isEmpty()) {
                    item.setAssignee(currentUser);
                }
            }
            
            // Set reporter to current user
            String currentUser = configService.getCurrentUser();
            if (currentUser != null && !currentUser.isEmpty()) {
                item.setReporter(currentUser);
            } else {
                item.setReporter("anonymous");
            }
            
            // Set project if provided, otherwise use current project
            if (project != null && !project.isEmpty()) {
                item.setProject(project);
            } else {
                // Try to get current project from configuration
                String currentProject = configService.getCurrentProject();
                if (currentProject != null && !currentProject.isEmpty()) {
                    item.setProject(currentProject);
                }
            }
            
            // Create the work item
            WorkItem createdItem = itemService.createWorkItem(item);
            
            // Output based on format
            if ("json".equalsIgnoreCase(format)) {
                // Use OutputFormatter for consistent JSON output
                Map<String, Object> resultMap = new HashMap<>();
                resultMap.put("result", "success");
                resultMap.put("item_id", createdItem.getId());
                resultMap.put("operationId", operationId);
                
                String json = OutputFormatter.toJson(resultMap, verbose);
                System.out.println(json);
            } else {
                displayTextOutput(createdItem, operationId);
            }
            
            // Record the successful operation
            Map<String, Object> result = new HashMap<>();
            result.put("itemId", createdItem.getId());
            result.put("title", createdItem.getTitle());
            result.put("type", createdItem.getType() != null ? createdItem.getType().toString() : null);
            result.put("priority", createdItem.getPriority() != null ? createdItem.getPriority().toString() : null);
            
            metadataService.completeOperation(operationId, createdItem.getId());
            return 0;
            
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error creating work item: " + e.getMessage();
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
     * @param operationId the operation ID for tracking
     */
    private void displayTextOutput(WorkItem workItem, String operationId) {
        System.out.println("Created work item: " + workItem.getId());
        System.out.println("Title: " + workItem.getTitle());
        System.out.println("Type: " + workItem.getType());
        System.out.println("Priority: " + workItem.getPriority());
        System.out.println("Status: " + workItem.getStatus());
        
        if (workItem.getAssignee() != null && !workItem.getAssignee().isEmpty()) {
            System.out.println("Assignee: " + workItem.getAssignee());
        }
        
        if (workItem.getProject() != null && !workItem.getProject().isEmpty()) {
            System.out.println("Project: " + workItem.getProject());
        }
        
        // Show additional details in verbose mode
        if (verbose) {
            if (workItem.getDescription() != null && !workItem.getDescription().isEmpty()) {
                System.out.println("\nDescription:");
                System.out.println(workItem.getDescription());
            }
            
            if (workItem.getReporter() != null && !workItem.getReporter().isEmpty()) {
                System.out.println("Reporter: " + workItem.getReporter());
            }
            
            if (workItem.getCreated() != null) {
                System.out.println("Created: " + workItem.getCreated());
            }
            
            System.out.println("\nOperation ID: " + operationId);
            System.out.println("Operation tracked in metadata service.");
        }
    }
    
    /**
     * Gets the title of the work item to create.
     * 
     * @return the title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Sets the title of the work item to create.
     * 
     * @param title the title
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Gets the description of the work item to create.
     * 
     * @return the description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the description of the work item to create.
     * 
     * @param description the description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Gets the type of the work item to create.
     * 
     * @return the type
     */
    public WorkItemType getType() {
        return type;
    }
    
    /**
     * Sets the type of the work item to create.
     * 
     * @param type the type
     */
    public void setType(WorkItemType type) {
        this.type = type;
    }
    
    /**
     * Gets the priority of the work item to create.
     * 
     * @return the priority
     */
    public Priority getPriority() {
        return priority;
    }
    
    /**
     * Sets the priority of the work item to create.
     * 
     * @param priority the priority
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }
    
    /**
     * Gets the assignee of the work item to create.
     * 
     * @return the assignee
     */
    public String getAssignee() {
        return assignee;
    }
    
    /**
     * Sets the assignee of the work item to create.
     * 
     * @param assignee the assignee
     */
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
    
    /**
     * Gets the project of the work item to create.
     * 
     * @return the project
     */
    public String getProject() {
        return project;
    }
    
    /**
     * Sets the project of the work item to create.
     * 
     * @param project the project
     */
    public void setProject(String project) {
        this.project = project;
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
}
