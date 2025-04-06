package org.rinna.cli.command;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.util.concurrent.Callable;

/**
 * Command for listing work items.
 */
@Command(
    name = "list",
    description = "List work items with optional filtering",
    mixinStandardHelpOptions = true
)
public class ListCommand implements Callable<Integer> {

    @Option(names = {"-t", "--type"}, description = "Filter by type (FEATURE, BUG, TASK)")
    private WorkItemType type;
    
    @Option(names = {"-p", "--priority"}, description = "Filter by priority (LOW, MEDIUM, HIGH, CRITICAL)")
    private Priority priority;
    
    @Option(names = {"-s", "--status"}, description = "Filter by workflow state")
    private WorkflowState status;
    
    @Option(names = {"-P", "--project"}, description = "Filter by project")
    private String project;
    
    @Option(names = {"-a", "--assignee"}, description = "Filter by assignee")
    private String assignee;
    
    @Option(names = {"-l", "--limit"}, description = "Maximum number of items to show")
    private int limit = 20;
    
    @Option(names = {"--format"}, description = "Output format (table, json)")
    private String format = "table";
    
    @Override
    public Integer call() {
        try {
            // In a real implementation, we would call a service to get work items
            // Here, we just display sample data
            System.out.println("Work Items:");
            System.out.println("--------------------------------------------------------------------------------");
            System.out.printf("%-8s %-40s %-10s %-10s %-15s %s%n", 
                             "ID", "TITLE", "TYPE", "PRIORITY", "PROJECT", "ASSIGNEE");
            System.out.println("--------------------------------------------------------------------------------");
            
            // Generate some fake items
            for (int i = 0; i < Math.min(limit, 10); i++) {
                int id = 100 + i;
                WorkItemType itemType;
                if (i % 3 == 0) {
                    itemType = WorkItemType.BUG;
                } else if (i % 3 == 1) {
                    itemType = WorkItemType.FEATURE;
                } else {
                    itemType = WorkItemType.TASK;
                }
                
                // Skip if doesn't match type filter
                if (type != null && itemType != type) {
                    continue;
                }
                
                Priority itemPriority;
                if (i % 4 == 0) {
                    itemPriority = Priority.CRITICAL;
                } else if (i % 4 == 1) {
                    itemPriority = Priority.HIGH;
                } else if (i % 4 == 2) {
                    itemPriority = Priority.MEDIUM;
                } else {
                    itemPriority = Priority.LOW;
                }
                
                // Skip if doesn't match priority filter
                if (priority != null && itemPriority != priority) {
                    continue;
                }
                
                String itemProject;
                if (i % 5 == 0) {
                    itemProject = "auth-system";
                } else if (i % 5 == 1) {
                    itemProject = "data-layer";
                } else if (i % 5 == 2) {
                    itemProject = "api-gateway";
                } else if (i % 5 == 3) {
                    itemProject = "search-service";
                } else {
                    itemProject = "notification";
                }
                
                // Skip if doesn't match project filter
                if (project != null && !project.isEmpty() && !project.equals(itemProject)) {
                    continue;
                }
                
                String itemAssignee;
                if (i % 6 == 0) {
                    itemAssignee = "alice";
                } else if (i % 6 == 1) {
                    itemAssignee = "bob";
                } else if (i % 6 == 2) {
                    itemAssignee = "carol";
                } else if (i % 6 == 3) {
                    itemAssignee = "dave";
                } else if (i % 6 == 4) {
                    itemAssignee = "eve";
                } else {
                    itemAssignee = null;
                }
                
                // Skip if doesn't match assignee filter
                if (assignee != null && !assignee.isEmpty() && 
                    (itemAssignee == null || !assignee.equals(itemAssignee))) {
                    continue;
                }
                
                String title = generateTitle(itemType, itemProject);
                if (title.length() > 40) {
                    title = title.substring(0, 37) + "...";
                }
                
                System.out.printf("%-8s %-40s %-10s %-10s %-15s %s%n",
                                 "WI-" + id,
                                 title,
                                 itemType,
                                 itemPriority,
                                 itemProject,
                                 itemAssignee != null ? itemAssignee : "-");
            }
            
            System.out.println("--------------------------------------------------------------------------------");
            System.out.printf("Displaying %d item(s)%n", Math.min(limit, 10));
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error listing work items: " + e.getMessage());
            return 1;
        }
    }
    
    private String generateTitle(WorkItemType type, String project) {
        String title;
        if (type == WorkItemType.BUG) {
            String[] bugTitles = {
                "Fix connection handling in " + project,
                project + " crashes when processing large inputs",
                "Memory leak in " + project + " under high load",
                "Incorrect error handling in " + project,
                project + " UI display issues on mobile devices"
            };
            title = bugTitles[(int)(Math.random() * bugTitles.length)];
        } else if (type == WorkItemType.FEATURE) {
            String[] featureTitles = {
                "Add support for OAuth2 in " + project,
                "Implement caching layer for " + project,
                "New dashboard for " + project + " analytics",
                "Export functionality for " + project + " data",
                "Integration with external APIs for " + project
            };
            title = featureTitles[(int)(Math.random() * featureTitles.length)];
        } else {
            String[] taskTitles = {
                "Update documentation for " + project,
                "Performance optimization for " + project,
                "Security review for " + project,
                "Refactor " + project + " codebase",
                "Add more test coverage for " + project
            };
            title = taskTitles[(int)(Math.random() * taskTitles.length)];
        }
        return title;
    }
}
