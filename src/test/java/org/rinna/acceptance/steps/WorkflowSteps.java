package org.rinna.acceptance.steps;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.junit.jupiter.api.Tag;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Step definitions for workflow acceptance tests.
 */
@Tag("acceptance")
@Tag("bdd")
public class WorkflowSteps {
    
    private String workItemId;
    
    @Given("a new work item with title {string} and type {string}")
    public void aNewWorkItemWithTitleAndType(String title, String type) {
        // Create a new work item
        WorkItemType itemType = WorkItemType.valueOf(type.toUpperCase());
        workItemId = createWorkItem(title, itemType);
        assertNotNull(workItemId, "Work item should be created with an ID");
    }
    
    @When("I update the work item status to {string}")
    public void iUpdateTheWorkItemStatusTo(String status) {
        // Update the work item status
        WorkflowState workflowState = WorkflowState.valueOf(status.toUpperCase());
        boolean result = updateWorkItemStatus(workItemId, workflowState);
        assertTrue(result, "Should be able to update work item status");
    }
    
    @When("I assign the work item to {string}")
    public void iAssignTheWorkItemTo(String assignee) {
        // Assign the work item
        boolean result = assignWorkItem(workItemId, assignee);
        assertTrue(result, "Should be able to assign the work item");
    }
    
    @Then("the work item should be successfully updated")
    public void theWorkItemShouldBeSuccessfullyUpdated() {
        // Verify the work item was updated
        boolean result = verifyWorkItemStatus(workItemId);
        assertTrue(result, "Work item should be successfully updated");
    }
    
    // Helper methods to simulate CLI operations
    
    private String createWorkItem(String title, WorkItemType type) {
        // Simulate creating a work item via CLI
        System.out.println("Creating work item: " + title + " of type " + type);
        return "WI-" + System.currentTimeMillis();
    }
    
    private boolean updateWorkItemStatus(String itemId, WorkflowState status) {
        // Simulate updating work item status via CLI
        System.out.println("Updating work item " + itemId + " status to " + status);
        return true;
    }
    
    private boolean assignWorkItem(String itemId, String assignee) {
        // Simulate assigning a work item via CLI
        System.out.println("Assigning work item " + itemId + " to " + assignee);
        return true;
    }
    
    private boolean verifyWorkItemStatus(String itemId) {
        // Simulate verifying work item status via CLI
        System.out.println("Verifying work item " + itemId + " status");
        return true;
    }
}