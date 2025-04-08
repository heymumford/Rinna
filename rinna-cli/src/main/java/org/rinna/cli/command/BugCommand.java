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

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command to create a bug report quickly.
 * This command creates a new bug work item with provided details.
 * Format options include text (default) and JSON.
 * 
 * Usage examples:
 * - rin bug "Bug title" --description="Description"
 * - rin bug "Bug title" --priority=high --assignee=username
 * - rin bug "Bug title" --version=1.0.0 --format=json
 * - rin bug "Bug title" --verbose
 */
public class BugCommand implements Callable<Integer> {
    
    private String title;
    private String description;
    private Priority priority = Priority.MEDIUM; // Default priority
    private String assignee;
    private String version;
    
    private String format = "text"; // Default output format
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final MetadataService metadataService;
    
    /**
     * Creates a new BugCommand with default services.
     */
    public BugCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new BugCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public BugCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.metadataService = serviceManager.getMetadataService();
    }
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("title", title);
        params.put("description", description != null ? description : "");
        params.put("priority", priority != null ? priority.toString() : "MEDIUM");
        params.put("assignee", assignee);
        params.put("version", version);
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("bug", "CREATE", params);
        
        try {
            if (title == null || title.isEmpty()) {
                System.err.println("Error: Bug title is required");
                metadataService.failOperation(operationId, new IllegalArgumentException("Missing bug title"));
                return 1;
            }
            
            // Get the services we need
            ItemService itemService = serviceManager.getItemService();
            ConfigurationService configService = serviceManager.getConfigurationService();
            
            // Create a new bug work item
            WorkItem bug = new WorkItem();
            bug.setTitle(title);
            bug.setDescription(description != null ? description : "");
            bug.setType(WorkItemType.BUG);
            bug.setPriority(priority);
            bug.setStatus(WorkflowState.CREATED);
            
            // Get the current project from the context
            String projectKey = serviceManager.getProjectContext().getCurrentProject();
            bug.setProject(projectKey);
            
            // Set the version if provided, otherwise use default from config
            if (version != null && !version.isEmpty()) {
                bug.setVersion(version);
            } else {
                bug.setVersion(configService.getDefaultVersion());
            }
            
            // Set the assignee if provided, otherwise use default or logged-in user
            if (assignee != null && !assignee.isEmpty()) {
                bug.setAssignee(assignee);
            } else if (configService.getAutoAssignBugs()) {
                // Auto-assign to the default assignee or the current user
                String defaultAssignee = configService.getDefaultBugAssignee();
                if (defaultAssignee != null && !defaultAssignee.isEmpty()) {
                    bug.setAssignee(defaultAssignee);
                } else {
                    bug.setAssignee(System.getProperty("user.name"));
                }
            }
            
            // Set the reporter to the current user
            bug.setReporter(System.getProperty("user.name"));
            
            // Set timestamps
            LocalDateTime now = LocalDateTime.now();
            bug.setCreated(now);
            bug.setUpdated(now);
            
            // Add any additional metadata
            Map<String, String> metadata = new HashMap<>();
            metadata.put("source", "cli");
            metadata.put("creationType", "quick-bug");
            
            // Create the bug using the service
            WorkItem created = itemService.createItem(bug);
            
            // Output the result
            if ("json".equalsIgnoreCase(format)) {
                outputJson(created, operationId);
            } else {
                outputText(created, operationId);
            }
            
            // Record the successful operation with result details
            Map<String, Object> result = new HashMap<>();
            result.put("bug_id", created.getId());
            result.put("title", created.getTitle());
            result.put("status", created.getStatus().toString());
            result.put("priority", created.getPriority().toString());
            result.put("assignee", created.getAssignee());
            
            metadataService.completeOperation(operationId, result);
            return 0;
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error creating bug: " + e.getMessage();
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
     * Output the created bug in text format.
     *
     * @param bug the created bug
     * @param operationId the operation ID for tracking
     */
    private void outputText(WorkItem bug, String operationId) {
        System.out.println("\nCreated bug:");
        System.out.printf("ID: %s%n", bug.getId());
        System.out.printf("Title: %s%n", bug.getTitle());
        System.out.printf("Type: %s%n", bug.getType());
        System.out.printf("Priority: %s%n", bug.getPriority());
        System.out.printf("Status: %s%n", bug.getStatus());
        System.out.printf("Project: %s%n", bug.getProject());
        
        if (bug.getAssignee() != null && !bug.getAssignee().isEmpty()) {
            System.out.printf("Assignee: %s%n", bug.getAssignee());
        }
        
        if (bug.getReporter() != null && !bug.getReporter().isEmpty()) {
            System.out.printf("Reporter: %s%n", bug.getReporter());
        }
        
        if (bug.getVersion() != null && !bug.getVersion().isEmpty()) {
            System.out.printf("Version: %s%n", bug.getVersion());
        }
        
        if (verbose && bug.getDescription() != null && !bug.getDescription().isEmpty()) {
            System.out.println("\nDescription:");
            System.out.println(bug.getDescription());
        }
    }
    
    /**
     * Output the created bug in JSON format.
     *
     * @param bug the created bug
     * @param operationId the operation ID for tracking
     */
    private void outputJson(WorkItem bug, String operationId) {
        // Use the OutputFormatter utility for consistent JSON formatting
        String json = OutputFormatter.toJson(bug, verbose);
        System.out.println(json);
    }
    
    /**
     * Gets the bug title.
     *
     * @return the bug title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Sets the bug title.
     *
     * @param title the bug title
     */
    public void setTitle(String title) {
        this.title = title;
    }
    
    /**
     * Gets the bug description.
     *
     * @return the bug description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the bug description.
     *
     * @param description the bug description
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    /**
     * Gets the bug priority.
     *
     * @return the bug priority
     */
    public Priority getPriority() {
        return priority;
    }
    
    /**
     * Sets the bug priority.
     *
     * @param priority the bug priority
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }
    
    /**
     * Gets the bug assignee.
     *
     * @return the bug assignee
     */
    public String getAssignee() {
        return assignee;
    }
    
    /**
     * Sets the bug assignee.
     *
     * @param assignee the bug assignee
     */
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
    
    /**
     * Gets the version.
     *
     * @return the version
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Sets the version.
     *
     * @param version the version
     */
    public void setVersion(String version) {
        this.version = version;
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
     * Sets whether to output in JSON format.
     * This is a convenience method for picocli integration.
     *
     * @param jsonOutput true to output in JSON format
     */
    public void setJsonOutput(boolean jsonOutput) {
        if (jsonOutput) {
            this.format = "json";
        }
    }
    
    /**
     * Gets whether to output in JSON format.
     * This is a convenience method for picocli integration.
     *
     * @return true if output format is JSON
     */
    public boolean isJsonOutput() {
        return "json".equalsIgnoreCase(format);
    }
}