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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Command to import tasks from markdown files.
 * This command parses markdown files and creates work items from the tasks found.
 * 
 * Usage examples:
 * - rin import tasks.md
 * - rin import --file=tasks.md
 * - rin import tasks.md --format=json
 * - rin import tasks.md --verbose
 */
public class ImportCommand implements Callable<Integer> {
    private static final Pattern HEADING_PATTERN = Pattern.compile("(#+)\\s+(.+)");
    private static final Pattern TASK_PATTERN = Pattern.compile("[-*]\\s+(?:\\[(\\w+)\\]\\s+)?(.+)");
    private static final Pattern PRIORITY_PATTERN = Pattern.compile("\\[(\\w+)\\]\\s+(.+)");
    
    // Mapping from markdown headings to workflow states
    private static final Map<String, WorkflowState> STATUS_MAPPING = new HashMap<>();
    static {
        STATUS_MAPPING.put("todo", WorkflowState.READY);
        STATUS_MAPPING.put("to do", WorkflowState.READY);
        STATUS_MAPPING.put("to-do", WorkflowState.READY);
        STATUS_MAPPING.put("in progress", WorkflowState.IN_PROGRESS);
        STATUS_MAPPING.put("in-progress", WorkflowState.IN_PROGRESS);
        STATUS_MAPPING.put("review", WorkflowState.REVIEW);
        STATUS_MAPPING.put("testing", WorkflowState.TESTING);
        STATUS_MAPPING.put("test", WorkflowState.TESTING);
        STATUS_MAPPING.put("done", WorkflowState.DONE);
        STATUS_MAPPING.put("completed", WorkflowState.DONE);
        STATUS_MAPPING.put("blocked", WorkflowState.BLOCKED);
        STATUS_MAPPING.put("backlog", WorkflowState.CREATED);
    }
    
    // Mapping from priority text to Priority enum
    private static final Map<String, Priority> PRIORITY_MAPPING = new HashMap<>();
    static {
        PRIORITY_MAPPING.put("critical", Priority.CRITICAL);
        PRIORITY_MAPPING.put("high", Priority.HIGH);
        PRIORITY_MAPPING.put("medium", Priority.MEDIUM);
        PRIORITY_MAPPING.put("low", Priority.LOW);
    }
    
    private String filePath;
    private String format = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final MetadataService metadataService;
    
    /**
     * Creates a new ImportCommand with default services.
     */
    public ImportCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new ImportCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public ImportCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.metadataService = serviceManager.getMetadataService();
    }
    
    /**
     * Sets the file path for the markdown file to import.
     *
     * @param filePath the path to the markdown file
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
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
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("file_path", filePath);
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("import", "CREATE", params);
        
        try {
            if (filePath == null || filePath.isEmpty()) {
                String errorMessage = "No file path provided";
                System.err.println("Error: " + errorMessage);
                System.err.println("Usage: rin import <file.md>");
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            // Verify the file is a markdown file
            if (!filePath.toLowerCase().endsWith(".md")) {
                String errorMessage = "File must be a markdown (.md) file";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            File file = new File(filePath);
            if (!file.exists()) {
                String errorMessage = "File not found: " + filePath;
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            try {
                String content = Files.readString(file.toPath());
                
                // Check if the file is empty
                if (content.trim().isEmpty()) {
                    String errorMessage = "No tasks found in file";
                    System.err.println("Error: " + errorMessage);
                    metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                    return 1;
                }
                
                // Parse the markdown file
                ImportResult result = parseMarkdownFile(content);
                
                if (result.getImportedTasks().isEmpty()) {
                    String errorMessage = "No tasks found in file";
                    System.err.println("Error: " + errorMessage);
                    metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                    return 1;
                }
                
                // Save imported tasks
                List<String> savedTaskIds = saveImportedTasks(result.getImportedTasks(), operationId);
                
                // Handle any unparsed content
                boolean hasUnparsedContent = !result.getUnparsedContent().isEmpty();
                if (hasUnparsedContent) {
                    generateReport(result.getUnparsedContent(), operationId);
                    
                    if ("json".equalsIgnoreCase(format)) {
                        Map<String, Object> jsonResult = new HashMap<>();
                        jsonResult.put("status", "partial_success");
                        jsonResult.put("imported_count", result.getImportedTasks().size());
                        jsonResult.put("unparsed_count", result.getUnparsedContent().size());
                        jsonResult.put("imported_ids", savedTaskIds);
                        jsonResult.put("report_file", "target/import-report.txt");
                        
                        String json = OutputFormatter.toJson(jsonResult, verbose);
                        System.out.println(json);
                    } else {
                        System.out.println("Warning: Some content couldn't be parsed. Imported " 
                                + result.getImportedTasks().size() + " task(s). See import-report.txt for details.");
                    }
                } else {
                    if ("json".equalsIgnoreCase(format)) {
                        Map<String, Object> jsonResult = new HashMap<>();
                        jsonResult.put("status", "success");
                        jsonResult.put("imported_count", result.getImportedTasks().size());
                        jsonResult.put("imported_ids", savedTaskIds);
                        
                        String json = OutputFormatter.toJson(jsonResult, verbose);
                        System.out.println(json);
                    } else {
                        System.out.println("Successfully imported " + result.getImportedTasks().size() + " task(s).");
                    }
                }
                
                // Record the successful operation
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("imported_count", result.getImportedTasks().size());
                resultData.put("imported_ids", savedTaskIds);
                if (hasUnparsedContent) {
                    resultData.put("unparsed_count", result.getUnparsedContent().size());
                    resultData.put("status", "partial_success");
                } else {
                    resultData.put("status", "success");
                }
                
                metadataService.completeOperation(operationId, resultData);
                return 0;
                
            } catch (IOException e) {
                String errorMessage = "Failed to read file: " + e.getMessage();
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, e);
                return 1;
            }
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error importing tasks: " + e.getMessage();
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
     * Parses a markdown file and extracts tasks with their status and priority.
     *
     * @param content the content of the markdown file
     * @return the result of the import operation
     */
    public ImportResult parseMarkdownFile(String content) {
        List<WorkItem> importedTasks = new ArrayList<>();
        List<String> unparsedContent = new ArrayList<>();
        
        WorkflowState currentStatus = WorkflowState.CREATED; // Default status
        String[] lines = content.split("\\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
            // Skip empty lines
            if (line.isEmpty()) {
                continue;
            }
            
            // Try to parse as heading (potential status)
            Matcher headingMatcher = HEADING_PATTERN.matcher(line);
            if (headingMatcher.matches()) {
                String headingText = headingMatcher.group(2).trim().toLowerCase();
                WorkflowState mappedStatus = STATUS_MAPPING.get(headingText);
                if (mappedStatus != null) {
                    currentStatus = mappedStatus;
                }
                continue;
            }
            
            // Try to parse as task
            Matcher taskMatcher = TASK_PATTERN.matcher(line);
            if (taskMatcher.matches()) {
                String taskTitle = taskMatcher.group(taskMatcher.groupCount());
                
                // Check for priority in the task title
                Priority taskPriority = Priority.MEDIUM; // Default priority
                Matcher priorityMatcher = PRIORITY_PATTERN.matcher(taskTitle);
                if (priorityMatcher.matches()) {
                    String priorityText = priorityMatcher.group(1).toLowerCase();
                    Priority mappedPriority = PRIORITY_MAPPING.get(priorityText);
                    if (mappedPriority != null) {
                        taskPriority = mappedPriority;
                        taskTitle = priorityMatcher.group(2).trim();
                    }
                }
                
                // Create the work item
                WorkItem task = new WorkItem();
                task.setId("IMP-" + (importedTasks.size() + 1));
                task.setTitle(taskTitle);
                task.setState(currentStatus);
                task.setPriority(taskPriority);
                task.setType(WorkItemType.TASK); // Default type
                task.setProject("Imported");
                
                importedTasks.add(task);
            } else if (line.startsWith("-") || line.startsWith("*")) {
                // This line looks like a task but didn't match our pattern
                unparsedContent.add(line);
            }
        }
        
        return new ImportResult(importedTasks, unparsedContent);
    }
    
    /**
     * Saves the imported tasks to the system using item and workflow services.
     *
     * @param tasks the list of tasks to save
     * @param operationId the operation ID for tracking
     * @return the list of saved task IDs
     */
    private List<String> saveImportedTasks(List<WorkItem> tasks, String operationId) {
        // Get the item service and workflow service from the service manager
        org.rinna.cli.service.ItemService itemService = serviceManager.getMockItemService();
        org.rinna.cli.service.WorkflowService workflowService = serviceManager.getMockWorkflowService();
        org.rinna.cli.service.BacklogService backlogService = serviceManager.getMockBacklogService();
        String currentUser = serviceManager.getConfigurationService().getCurrentUser();
        
        // If no user is set, use system username
        if (currentUser == null || currentUser.isEmpty()) {
            currentUser = System.getProperty("user.name");
        }
        
        List<String> savedTaskIds = new ArrayList<>();
        
        // Process each task
        for (WorkItem task : tasks) {
            try {
                // Create the work item
                org.rinna.cli.model.WorkItemCreateRequest createRequest = 
                    new org.rinna.cli.model.WorkItemCreateRequest.Builder()
                        .title(task.getTitle())
                        .description("")
                        .type(task.getType())
                        .priority(task.getPriority())
                        .project(task.getProject())
                        .build();
                
                // Save the work item using the item service
                WorkItem createdItem = itemService.createWorkItem(createRequest);
                savedTaskIds.add(createdItem.getId());
                
                // If the state isn't the default CREATED state, transition it
                if (task.getState() != WorkflowState.CREATED) {
                    workflowService.transition(
                        createdItem.getId(),
                        currentUser,
                        task.getState(),
                        "Auto-transitioned during import"
                    );
                }
                
                // If the state is CREATED, add to backlog
                if (task.getState() == WorkflowState.CREATED) {
                    backlogService.addToBacklog(currentUser, createdItem);
                }
                
                if (!"json".equalsIgnoreCase(format)) {
                    System.out.println("Imported: " + createdItem.getTitle() + " [" + 
                        createdItem.getId() + "] as " + createdItem.getState());
                }
                
                // Track task creation in metadata service
                Map<String, Object> taskResult = new HashMap<>();
                taskResult.put("id", createdItem.getId());
                taskResult.put("title", createdItem.getTitle());
                taskResult.put("state", createdItem.getState().toString());
                taskResult.put("type", createdItem.getType().toString());
                
                // We don't want to call completeOperation here since we're tracking the entire import
                // operation together, not individual task creations
                
            } catch (Exception e) {
                System.err.println("Error importing task: " + task.getTitle() + " - " + e.getMessage());
                
                // We don't want to call failOperation here since it would mark the whole import
                // operation as failed, and we want to continue with other tasks
            }
        }
        
        return savedTaskIds;
    }
    
    /**
     * Generates a report of content that couldn't be parsed.
     *
     * @param unparsedContent the list of unparsed content lines
     * @param operationId the operation ID for tracking
     */
    private void generateReport(List<String> unparsedContent, String operationId) {
        // Get project root directory for absolute path
        String projectRoot = System.getProperty("user.dir");
        File reportFile = new File(projectRoot, "target/import-report.txt");
        
        // Ensure target directory exists
        reportFile.getParentFile().mkdirs();
        
        try (java.io.BufferedWriter writer = java.nio.file.Files.newBufferedWriter(
                reportFile.toPath(), java.nio.charset.StandardCharsets.UTF_8)) {
            writer.write("Import Report - Unparsed Content\n");
            writer.write("====================================\n\n");
            writer.write("The following content could not be parsed as tasks:\n\n");
            
            for (String line : unparsedContent) {
                writer.write(line + "\n");
            }
            
            writer.write("\n====================================\n");
            writer.write("End of Report\n");
            writer.write("Operation ID: " + operationId + "\n");
            
            if (!"json".equalsIgnoreCase(format)) {
                System.out.println("Report generated at: " + reportFile.getAbsolutePath());
            }
            
        } catch (IOException e) {
            String errorMessage = "Failed to generate report: " + e.getMessage();
            System.err.println("Warning: " + errorMessage);
            
            // We don't call failOperation here since it would mark the entire import as failed,
            // and we want to continue with the successful parts
        }
    }
    
    /**
     * Inner class to hold the result of the import operation.
     */
    public static class ImportResult {
        private final List<WorkItem> importedTasks;
        private final List<String> unparsedContent;
        
        /**
         * Constructor for import result.
         *
         * @param importedTasks the list of imported tasks
         * @param unparsedContent the list of unparsed content lines
         */
        public ImportResult(List<WorkItem> importedTasks, List<String> unparsedContent) {
            this.importedTasks = importedTasks;
            this.unparsedContent = unparsedContent;
        }
        
        /**
         * Returns the list of imported tasks.
         *
         * @return the list of imported tasks
         */
        public List<WorkItem> getImportedTasks() {
            return importedTasks;
        }
        
        /**
         * Returns the list of unparsed content lines.
         *
         * @return the list of unparsed content lines
         */
        public List<String> getUnparsedContent() {
            return unparsedContent;
        }
    }
}
