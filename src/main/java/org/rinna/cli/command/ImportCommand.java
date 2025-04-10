package org.rinna.cli.command;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rinna.domain.Priority;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.usecase.ItemService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Command for importing tasks from markdown files.
 */
public class ImportCommand {
    private static final Logger logger = LoggerFactory.getLogger(ImportCommand.class);
    
    private final ItemService itemService;
    
    // Regular expressions for parsing
    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#+)\\s+(.+)$");
    private static final Pattern TASK_PATTERN = Pattern.compile("^[\\-*]\\s+(.+)$");
    private static final Pattern PRIORITY_PATTERN = Pattern.compile("^\\[(High|Medium|Low)]\\s+(.+)$");
    
    // Status mapping
    private static final Map<String, WorkflowState> STATUS_MAPPING = new HashMap<>();
    
    static {
        // Standard status mappings (case-insensitive)
        STATUS_MAPPING.put("todo", WorkflowState.TO_DO);
        STATUS_MAPPING.put("to do", WorkflowState.TO_DO);
        STATUS_MAPPING.put("to-do", WorkflowState.TO_DO);
        STATUS_MAPPING.put("in progress", WorkflowState.IN_PROGRESS);
        STATUS_MAPPING.put("in-progress", WorkflowState.IN_PROGRESS);
        STATUS_MAPPING.put("done", WorkflowState.DONE);
        STATUS_MAPPING.put("completed", WorkflowState.DONE);
        STATUS_MAPPING.put("finished", WorkflowState.DONE);
        STATUS_MAPPING.put("blocked", WorkflowState.BLOCKED);
        STATUS_MAPPING.put("on hold", WorkflowState.BLOCKED);
        STATUS_MAPPING.put("backlog", WorkflowState.BACKLOG);
        STATUS_MAPPING.put("triage", WorkflowState.TRIAGE);
        STATUS_MAPPING.put("in test", WorkflowState.IN_TEST);
        STATUS_MAPPING.put("testing", WorkflowState.IN_TEST);
    }
    
    public ImportCommand(ItemService itemService) {
        this.itemService = itemService;
    }
    
    /**
     * Execute the import command.
     *
     * @param args Command arguments
     * @return Exit code (0 for success, non-zero for failure)
     */
    public int execute(String[] args) {
        if (args.length < 1) {
            System.err.println("Error: File path is required");
            printUsage();
            return 1;
        }
        
        String filePath = args[0];
        File file = new File(filePath);
        
        // Check if file exists
        if (!file.exists()) {
            System.err.println("Error: File not found: " + filePath);
            return 1;
        }
        
        // Check if file is markdown
        if (!filePath.endsWith(".md")) {
            System.err.println("Error: File must be a markdown (.md) file");
            return 1;
        }
        
        try {
            String content = Files.readString(Path.of(filePath));
            
            // Check if file is empty
            if (content.trim().isEmpty()) {
                System.err.println("Error: No tasks found in file");
                return 1;
            }
            
            // Parse the markdown file
            ImportResult result = parseMarkdownFile(content);
            
            // Save the imported tasks
            for (WorkItem task : result.getImportedTasks()) {
                itemService.createWorkItem(task);
            }
            
            // Generate report for unparsed content if needed
            if (!result.getUnparsedContent().isEmpty()) {
                generateImportReport(result.getUnparsedContent());
                System.out.println("Warning: Some content couldn't be parsed. Imported " + 
                    result.getImportedTasks().size() + " tasks. See import-report.txt for details.");
            } else {
                System.out.println("Successfully imported " + result.getImportedTasks().size() + " tasks.");
            }
            
            return 0;
            
        } catch (IOException e) {
            System.err.println("Error: Failed to read file: " + e.getMessage());
            return 1;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Parse a markdown file and extract tasks.
     *
     * @param content Markdown content
     * @return ImportResult containing imported tasks and unparsed content
     */
    public ImportResult parseMarkdownFile(String content) {
        List<WorkItem> importedTasks = new ArrayList<>();
        List<String> unparsedContent = new ArrayList<>();
        
        WorkflowState currentStatus = WorkflowState.BACKLOG; // Default status
        String[] lines = content.split("\\n");
        
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            
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
                String taskTitle = taskMatcher.group(1).trim();
                
                // Check for priority in the task title
                Priority priority = Priority.MEDIUM; // Default priority
                Matcher priorityMatcher = PRIORITY_PATTERN.matcher(taskTitle);
                if (priorityMatcher.matches()) {
                    String priorityText = priorityMatcher.group(1).toUpperCase();
                    priority = Priority.valueOf(priorityText);
                    taskTitle = priorityMatcher.group(2).trim();
                }
                
                // Create the work item
                WorkItem task = new WorkItem();
                task.setId("IMP-" + (importedTasks.size() + 1));
                task.setTitle(taskTitle);
                task.setStatus(currentStatus);
                task.setPriority(priority);
                task.setType(WorkItemType.TASK); // Default type
                task.setSource("imported");
                
                importedTasks.add(task);
                continue;
            }
            
            // If we reach here, we couldn't parse the line
            unparsedContent.add(line);
        }
        
        return new ImportResult(importedTasks, unparsedContent);
    }
    
    /**
     * Generate a report of content that couldn't be parsed.
     *
     * @param unparsedContent List of lines that couldn't be parsed
     */
    private void generateImportReport(List<String> unparsedContent) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("import-report.txt"))) {
            writer.write("The following lines couldn't be parsed as tasks:\n\n");
            
            for (String line : unparsedContent) {
                writer.write(line + "\n");
            }
            
            writer.write("\nPlease format your markdown with the following conventions:\n");
            writer.write("1. Use headings (# Title) for status sections\n");
            writer.write("2. Use list items (- Task or * Task) for tasks\n");
            writer.write("3. Optionally use [High/Medium/Low] prefix for priority\n");
            
        } catch (IOException e) {
            logger.error("Failed to write import report", e);
            System.err.println("Warning: Failed to create import report: " + e.getMessage());
        }
    }
    
    /**
     * Print usage information.
     */
    private void printUsage() {
        System.out.println("Usage: rin import <file.md>");
        System.out.println("  Imports tasks from markdown files.");
        System.out.println();
        System.out.println("Formatting Guidelines:");
        System.out.println("  - Use headings for status sections, e.g., '# To Do', '## In Progress'");
        System.out.println("  - List tasks with '- ' or '* ' bullets");
        System.out.println("  - Optionally specify priority with '[High]', '[Medium]', or '[Low]' prefix");
    }
    
    /**
     * Class to hold the result of parsing a markdown file.
     */
    public static class ImportResult {
        private final List<WorkItem> importedTasks;
        private final List<String> unparsedContent;
        
        public ImportResult(List<WorkItem> importedTasks, List<String> unparsedContent) {
            this.importedTasks = importedTasks;
            this.unparsedContent = unparsedContent;
        }
        
        public List<WorkItem> getImportedTasks() {
            return importedTasks;
        }
        
        public List<String> getUnparsedContent() {
            return unparsedContent;
        }
    }
}