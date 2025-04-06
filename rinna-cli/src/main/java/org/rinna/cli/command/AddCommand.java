package org.rinna.cli.command;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItemType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * Command for adding a new work item.
 */
@Command(
    name = "add",
    description = "Add a new work item",
    mixinStandardHelpOptions = true
)
public class AddCommand implements Callable<Integer> {

    @Parameters(description = "The title of the work item")
    private String title;
    
    @Option(names = {"-t", "--type"}, description = "Type of work item (FEATURE, BUG, TASK)")
    private WorkItemType type = WorkItemType.TASK;
    
    @Option(names = {"-p", "--priority"}, description = "Priority level (LOW, MEDIUM, HIGH, CRITICAL)")
    private Priority priority = Priority.MEDIUM;
    
    @Option(names = {"-P", "--project"}, description = "Project name")
    private String project;
    
    @Option(names = {"-a", "--assignee"}, description = "Assignee username")
    private String assignee;
    
    @Option(names = {"-d", "--description"}, description = "Description of the work item")
    private String description;
    
    @Override
    public Integer call() {
        try {
            // This would call a service to create a work item
            // For now, just print what would be created
            System.out.println("✓ Work item created successfully");
            System.out.println();
            System.out.println("ID:        WI-" + (100 + (int)(Math.random() * 900)));
            System.out.println("Title:     " + title);
            System.out.println("Type:      " + type);
            System.out.println("Priority:  " + priority);
            
            if (project != null && !project.isEmpty()) {
                System.out.println("Project:   " + project);
            }
            
            if (assignee != null && !assignee.isEmpty()) {
                System.out.println("Assignee:  " + assignee);
            }
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error creating work item: " + e.getMessage());
            return 1;
        }
    }
}
