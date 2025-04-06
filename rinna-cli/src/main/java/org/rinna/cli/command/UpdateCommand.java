package org.rinna.cli.command;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.util.concurrent.Callable;

/**
 * Command for updating a work item.
 */
@Command(
    name = "update",
    description = "Update a work item",
    mixinStandardHelpOptions = true
)
public class UpdateCommand implements Callable<Integer> {

    @Parameters(description = "ID of the work item to update")
    private String id;
    
    @Option(names = {"-t", "--title"}, description = "New title")
    private String title;
    
    @Option(names = {"--type"}, description = "New type")
    private WorkItemType type;
    
    @Option(names = {"-p", "--priority"}, description = "New priority")
    private Priority priority;
    
    @Option(names = {"-s", "--status"}, description = "New status")
    private WorkflowState status;
    
    @Option(names = {"-a", "--assignee"}, description = "New assignee")
    private String assignee;
    
    @Option(names = {"-d", "--description"}, description = "New description")
    private String description;
    
    @Override
    public Integer call() {
        try {
            // Validate ID format
            if (!id.matches("WI-\\d+")) {
                System.err.println("Invalid work item ID format. Expected: WI-XXX");
                return 1;
            }
            
            // In a real implementation, we would:
            // 1. Call service to get the work item
            // 2. Update the fields
            // 3. Save the changes
            
            // For now, just show what would have been updated
            System.out.println("Work item updated successfully");
            System.out.println();
            System.out.println("ID:        " + id);
            
            if (title != null) {
                System.out.println("Title:     " + title + " (was: Original Title)");
            }
            
            if (type != null) {
                System.out.println("Type:      " + type + " (was: TASK)");
            }
            
            if (priority != null) {
                System.out.println("Priority:  " + priority + " (was: MEDIUM)");
            }
            
            if (status != null) {
                System.out.println("Status:    " + status + " (was: FOUND)");
            }
            
            if (assignee != null) {
                System.out.println("Assignee:  " + assignee + " (was: -)");
            }
            
            if (description != null) {
                System.out.println();
                System.out.println("Description: updated");
            }
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error updating work item: " + e.getMessage());
            return 1;
        }
    }
}
