package org.rinna.cli.command;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command for viewing a specific work item.
 */
@Command(
    name = "view",
    description = "View details of a work item",
    mixinStandardHelpOptions = true
)
public class ViewCommand implements Callable<Integer> {

    @Parameters(description = "The ID of the work item to view")
    public String id;

    @Option(names = {"--format"}, description = "Output format (text, json)")
    public String format = "text";

    @Option(names = {"-v", "--verbose"}, description = "Show all details")
    public boolean verbose = false;

    @Override
    public Integer call() {
        try {
            // In a real implementation, we would call a service to get the work item
            // WorkItemService workItemService = ServiceLocator.getWorkItemService();
            // Optional<WorkItem> itemOpt = workItemService.findById(id);
            // if (!itemOpt.isPresent()) {
            //    System.err.println("Work item not found: " + id);
            //    return 1;
            // }
            // WorkItem item = itemOpt.get();
            
            // For demonstration, return a fake work item if the ID matches our sample pattern
            if (!id.matches("WI-\\d+")) {
                System.err.println("Work item not found: " + id);
                return 1;
            }
            
            // Create a sample work item for demonstration
            SampleWorkItem item = createSampleWorkItem(id);
            
            // Display the item
            if ("json".equalsIgnoreCase(format)) {
                displayAsJson(item);
            } else {
                displayAsText(item);
            }
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error viewing work item: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }
    
    private void displayAsText(SampleWorkItem item) {
        System.out.println("=" + "=".repeat(item.id.length()) + "===");
        System.out.println("= " + item.id + " =");
        System.out.println("=" + "=".repeat(item.id.length()) + "===");
        System.out.println();
        
        System.out.println("Title:       " + item.title);
        System.out.println("Type:        " + item.type);
        System.out.println("Priority:    " + item.priority);
        System.out.println("Status:      " + item.status);
        System.out.println("Project:     " + item.project);
        System.out.println("Assignee:    " + (item.assignee != null ? item.assignee : "-"));
        System.out.println("Created:     " + item.createdAt.format(DateTimeFormatter.ISO_LOCAL_DATE));
        
        if (item.description != null && !item.description.isEmpty()) {
            System.out.println();
            System.out.println("Description:");
            System.out.println("-".repeat(80));
            System.out.println(item.description);
            System.out.println("-".repeat(80));
        }
        
        if (!item.metadata.isEmpty()) {
            System.out.println();
            System.out.println("Metadata:");
            item.metadata.forEach((key, value) -> {
                System.out.println("  " + key + ": " + value);
            });
        }
    }
    
    private void displayAsJson(SampleWorkItem item) {
        // A simple JSON representation (in a real implementation, use Jackson)
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"id\": \"").append(item.id).append("\",\n");
        json.append("  \"title\": \"").append(item.title).append("\",\n");
        json.append("  \"type\": \"").append(item.type).append("\",\n");
        json.append("  \"priority\": \"").append(item.priority).append("\",\n");
        json.append("  \"status\": \"").append(item.status).append("\",\n");
        json.append("  \"project\": \"").append(item.project).append("\",\n");
        json.append("  \"assignee\": ").append(item.assignee != null ? "\"" + item.assignee + "\"" : "null").append(",\n");
        json.append("  \"createdAt\": \"").append(item.createdAt).append("\",\n");
        
        if (item.description != null && !item.description.isEmpty()) {
            json.append("  \"description\": \"").append(item.description.replace("\"", "\\\"")).append("\",\n");
        }
        
        if (!item.metadata.isEmpty()) {
            json.append("  \"metadata\": {\n");
            int i = 0;
            for (Map.Entry<String, String> entry : item.metadata.entrySet()) {
                json.append("    \"").append(entry.getKey()).append("\": \"")
                    .append(entry.getValue()).append("\"");
                if (i < item.metadata.size() - 1) {
                    json.append(",");
                }
                json.append("\n");
                i++;
            }
            json.append("  }\n");
        } else {
            json.append("  \"metadata\": {}\n");
        }
        
        json.append("}");
        System.out.println(json);
    }
    
    private SampleWorkItem createSampleWorkItem(String id) {
        // Generate sample data based on the ID
        int idNum = Integer.parseInt(id.substring(3));
        WorkItemType type = (idNum % 3 == 0) ? WorkItemType.BUG : 
                           (idNum % 3 == 1) ? WorkItemType.FEATURE : WorkItemType.TASK;
        
        Priority priority = (idNum % 4 == 0) ? Priority.CRITICAL : 
                           (idNum % 4 == 1) ? Priority.HIGH : 
                           (idNum % 4 == 2) ? Priority.MEDIUM : Priority.LOW;
        
        WorkflowState status = (idNum % 5 == 0) ? WorkflowState.FOUND : 
                              (idNum % 5 == 1) ? WorkflowState.TRIAGED : 
                              (idNum % 5 == 2) ? WorkflowState.READY : 
                              (idNum % 5 == 3) ? WorkflowState.IN_DEV : WorkflowState.CLOSED;
        
        String[] projects = {"auth-system", "data-layer", "api-gateway", "search-service", "notification"};
        String project = projects[idNum % projects.length];
        
        String[] assignees = {"alice", "bob", "carol", "dave", null};
        String assignee = assignees[idNum % assignees.length];
        
        SampleWorkItem item = new SampleWorkItem();
        item.id = id;
        item.title = generateTitle(type, project);
        item.type = type;
        item.priority = priority;
        item.status = status;
        item.project = project;
        item.assignee = assignee;
        item.createdAt = LocalDateTime.now().minusDays(idNum % 30);
        item.description = generateDescription(type, project);
        
        // Add some metadata
        item.metadata.put("reporter", "system");
        item.metadata.put("estimated_hours", String.valueOf(idNum % 20 + 1));
        if (type == WorkItemType.BUG) {
            item.metadata.put("severity", priority.name());
            item.metadata.put("affects_version", "1.2." + (idNum % 10));
        }
        
        return item;
    }
    
    private String generateTitle(WorkItemType type, String project) {
        if (type == WorkItemType.BUG) {
            String[] bugTitles = {
                "Fix connection handling in " + project,
                project + " crashes when processing large inputs",
                "Memory leak in " + project + " under high load",
                "Incorrect error handling in " + project,
                project + " UI display issues on mobile devices"
            };
            return bugTitles[(int)(Math.random() * bugTitles.length)];
        } else if (type == WorkItemType.FEATURE) {
            String[] featureTitles = {
                "Add support for OAuth2 in " + project,
                "Implement caching layer for " + project,
                "New dashboard for " + project + " analytics",
                "Export functionality for " + project + " data",
                "Integration with external APIs for " + project
            };
            return featureTitles[(int)(Math.random() * featureTitles.length)];
        } else {
            String[] taskTitles = {
                "Update documentation for " + project,
                "Performance optimization for " + project,
                "Security review for " + project,
                "Refactor " + project + " codebase",
                "Add more test coverage for " + project
            };
            return taskTitles[(int)(Math.random() * taskTitles.length)];
        }
    }
    
    private String generateDescription(WorkItemType type, String project) {
        if (type == WorkItemType.BUG) {
            return "When using the " + project + " under high load conditions, users are experiencing unexpected behavior. " +
                   "This issue has been reproduced in our testing environment.\n\n" +
                   "Steps to reproduce:\n" +
                   "1. Configure the system with default settings\n" +
                   "2. Run the performance test suite\n" +
                   "3. Observe the error in logs\n\n" +
                   "Expected: System should handle the load gracefully\n" +
                   "Actual: System throws exceptions and fails to process requests";
        } else if (type == WorkItemType.FEATURE) {
            return "We need to implement a new feature in the " + project + " to support upcoming business requirements.\n\n" +
                   "Requirements:\n" +
                   "- The feature should integrate with existing systems\n" +
                   "- Should be configurable through the admin panel\n" +
                   "- Must support high availability\n" +
                   "- Should include comprehensive documentation\n\n" +
                   "This feature has been requested by the business team and is planned for the next release.";
        } else {
            return "This task involves improving the current state of the " + project + ".\n\n" +
                   "Tasks to complete:\n" +
                   "- Review the current implementation\n" +
                   "- Identify areas for improvement\n" +
                   "- Implement changes according to best practices\n" +
                   "- Update documentation\n" +
                   "- Ensure test coverage";
        }
    }
    
    // Simple class to represent a work item for demonstration
    private static class SampleWorkItem {
        String id;
        String title;
        WorkItemType type;
        Priority priority;
        WorkflowState status;
        String project;
        String assignee;
        LocalDateTime createdAt;
        String description;
        Map<String, String> metadata = new HashMap<>();
    }
}