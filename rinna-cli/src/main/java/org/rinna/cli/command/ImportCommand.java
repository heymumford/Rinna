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
import org.rinna.cli.service.ServiceManager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Command to import tasks from markdown files.
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
    private ServiceManager serviceManager;
    
    /**
     * Constructor that initializes the service manager.
     */
    public ImportCommand() {
        this.serviceManager = ServiceManager.getInstance();
    }
    
    /**
     * Sets the file path for the markdown file to import.
     *
     * @param filePath the path to the markdown file
     */
    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
    
    @Override
    public Integer call() {
        if (filePath == null || filePath.isEmpty()) {
            System.err.println("Error: No file path provided");
            System.err.println("Usage: rin import <file.md>");
            return 1;
        }
        
        // Verify the file is a markdown file
        if (!filePath.toLowerCase().endsWith(".md")) {
            System.err.println("Error: File must be a markdown (.md) file");
            return 1;
        }
        
        File file = new File(filePath);
        if (!file.exists()) {
            System.err.println("Error: File not found: " + filePath);
            return 1;
        }
        
        try {
            String content = Files.readString(file.toPath());
            
            // Check if the file is empty
            if (content.trim().isEmpty()) {
                System.err.println("Error: No tasks found in file");
                return 1;
            }
            
            // Parse the markdown file
            ImportResult result = parseMarkdownFile(content);
            
            if (result.getImportedTasks().isEmpty()) {
                System.err.println("Error: No tasks found in file");
                return 1;
            }
            
            // Save imported tasks
            saveImportedTasks(result.getImportedTasks());
            
            // Handle any unparsed content
            if (!result.getUnparsedContent().isEmpty()) {
                generateReport(result.getUnparsedContent());
                System.out.println("Warning: Some content couldn't be parsed. Imported " 
                        + result.getImportedTasks().size() + " task(s). See import-report.txt for details.");
            } else {
                System.out.println("Successfully imported " + result.getImportedTasks().size() + " task(s).");
            }
            
            return 0;
            
        } catch (IOException e) {
            System.err.println("Error: Failed to read file: " + e.getMessage());
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
     * Saves the imported tasks to the system.
     *
     * @param tasks the list of tasks to save
     */
    private void saveImportedTasks(List<WorkItem> tasks) {
        // In a real implementation, this would save to a database or service
        // For this mock implementation, we'll just log the saved tasks
        for (WorkItem task : tasks) {
            System.out.println("Imported: " + task);
        }
    }
    
    /**
     * Generates a report of content that couldn't be parsed.
     *
     * @param unparsedContent the list of unparsed content lines
     */
    private void generateReport(List<String> unparsedContent) {
        // Get project root directory for absolute path
        String projectRoot = System.getProperty("user.dir");
        File reportFile = new File(projectRoot, "target/import-report.txt");
        
        // Ensure target directory exists
        reportFile.getParentFile().mkdirs();
        
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(reportFile))) {
            writer.write("Import Report - Unparsed Content\n");
            writer.write("====================================\n\n");
            writer.write("The following content could not be parsed as tasks:\n\n");
            
            for (String line : unparsedContent) {
                writer.write(line + "\n");
            }
            
            writer.write("\n====================================\n");
            writer.write("End of Report\n");
            
            System.out.println("Report generated at: " + reportFile.getAbsolutePath());
            
        } catch (IOException e) {
            System.err.println("Warning: Failed to generate report: " + e.getMessage());
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
