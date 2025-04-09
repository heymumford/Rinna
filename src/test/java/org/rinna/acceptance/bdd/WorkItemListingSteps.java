package org.rinna.acceptance.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import org.rinna.adapter.repository.InMemoryItemRepository;
import org.rinna.adapter.service.DefaultItemService;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.usecase.ItemService;
import org.rinna.repository.ItemRepository;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("acceptance")
public class WorkItemListingSteps {
    
    private final ItemRepository itemRepository = new InMemoryItemRepository();
    private final ItemService itemService = new DefaultItemService(itemRepository);
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private String commandOutput;
    
    @Given("there are no work items in the repository")
    public void thereAreNoWorkItemsInTheRepository() {
        ((InMemoryItemRepository) itemRepository).clear();
        List<WorkItem> items = itemService.findAll();
        assertTrue(items.isEmpty(), "Repository should be empty");
    }
    
    @Given("there are work items in the repository")
    public void thereAreWorkItemsInTheRepository() {
        ((InMemoryItemRepository) itemRepository).clear();
        
        // Add some sample work items
        itemService.create(new WorkItemCreateRequest(
            "Fix authentication bug", 
            "Users can't log in with correct credentials", 
            WorkItemType.BUG,
            Priority.HIGH,
            WorkflowState.IN_PROGRESS,
            "alice",
            null,
            null,
            "PUBLIC",
            true
        ));
        
        itemService.create(new WorkItemCreateRequest(
            "Add user profile page", 
            "Create a page for users to view and edit their profile", 
            WorkItemType.FEATURE,
            Priority.MEDIUM,
            WorkflowState.TODO,
            null,
            null,
            null,
            "PUBLIC",
            true
        ));
        
        List<WorkItem> items = itemService.findAll();
        assertEquals(2, items.size(), "Repository should have 2 items");
    }
    
    @Given("there are work items of different types in the repository")
    public void thereAreWorkItemsOfDifferentTypesInTheRepository() {
        ((InMemoryItemRepository) itemRepository).clear();
        
        // Add work items of different types
        itemService.create(new WorkItemCreateRequest(
            "Fix authentication bug", 
            "Users can't log in with correct credentials", 
            WorkItemType.BUG,
            Priority.HIGH,
            WorkflowState.IN_PROGRESS,
            "alice",
            null,
            null,
            "PUBLIC",
            true
        ));
        
        itemService.create(new WorkItemCreateRequest(
            "Add user profile page", 
            "Create a page for users to view and edit their profile", 
            WorkItemType.FEATURE,
            Priority.MEDIUM,
            WorkflowState.TODO,
            null,
            null,
            null,
            "PUBLIC",
            true
        ));
        
        itemService.create(new WorkItemCreateRequest(
            "Update documentation", 
            "Update API documentation with new endpoints", 
            WorkItemType.TASK,
            Priority.LOW,
            WorkflowState.TODO,
            "bob",
            null,
            null,
            "PUBLIC",
            true
        ));
        
        List<WorkItem> items = itemService.findAll();
        assertEquals(3, items.size(), "Repository should have 3 items");
        
        long typeCount = items.stream().map(WorkItem::getType).distinct().count();
        assertEquals(3, typeCount, "There should be 3 different types of work items");
    }
    
    @Given("there are work items with different statuses in the repository")
    public void thereAreWorkItemsWithDifferentStatusesInTheRepository() {
        ((InMemoryItemRepository) itemRepository).clear();
        
        // Add work items with different statuses
        itemService.create(new WorkItemCreateRequest(
            "Fix authentication bug", 
            "Users can't log in with correct credentials", 
            WorkItemType.BUG,
            Priority.HIGH,
            WorkflowState.IN_PROGRESS,
            "alice",
            null,
            null,
            "PUBLIC",
            true
        ));
        
        itemService.create(new WorkItemCreateRequest(
            "Add user profile page", 
            "Create a page for users to view and edit their profile", 
            WorkItemType.FEATURE,
            Priority.MEDIUM,
            WorkflowState.TODO,
            null,
            null,
            null,
            "PUBLIC",
            true
        ));
        
        itemService.create(new WorkItemCreateRequest(
            "Update documentation", 
            "Update API documentation with new endpoints", 
            WorkItemType.TASK,
            Priority.LOW,
            WorkflowState.DONE,
            "bob",
            null,
            null,
            "PUBLIC",
            true
        ));
        
        List<WorkItem> items = itemService.findAll();
        assertEquals(3, items.size(), "Repository should have 3 items");
        
        long statusCount = items.stream().map(WorkItem::getStatus).distinct().count();
        assertEquals(3, statusCount, "There should be 3 different statuses of work items");
    }
    
    @When("I run the {string} command")
    public void iRunTheCommand(String command) {
        // Redirect System.out for testing
        System.setOut(new PrintStream(outputStream));
        
        // Here, we'll mock running the command rather than actually launching a subprocess
        try {
            if (command.equals("rin list")) {
                mockRinListCommand(null, null);
            } else if (command.equals("rin list --type FEATURE")) {
                mockRinListCommand("FEATURE", null);
            } else if (command.equals("rin list --status TODO")) {
                mockRinListCommand(null, "TODO");
            } else {
                fail("Unsupported command: " + command);
            }
            
            commandOutput = outputStream.toString();
        } finally {
            // Restore original System.out
            System.setOut(originalOut);
        }
    }
    
    @Then("I should see a message indicating there are no work items")
    public void iShouldSeeAMessageIndicatingThereAreNoWorkItems() {
        assertNotNull(commandOutput, "Command output should not be null");
        assertTrue(commandOutput.contains("No work items found"), 
            "Output should contain 'No work items found'");
    }
    
    @And("I should see instructions for adding work items")
    public void iShouldSeeInstructionsForAddingWorkItems() {
        assertNotNull(commandOutput, "Command output should not be null");
        assertTrue(commandOutput.contains("To add a new work item, use:"), 
            "Output should contain instructions for adding work items");
        assertTrue(commandOutput.contains("rin add --type"), 
            "Output should contain example command for adding items");
    }
    
    @Then("I should see a list of all work items")
    public void iShouldSeeAListOfAllWorkItems() {
        assertNotNull(commandOutput, "Command output should not be null");
        assertTrue(commandOutput.contains("Work Items:"), 
            "Output should contain 'Work Items:' header");
        assertFalse(commandOutput.contains("No work items found"), 
            "Output should not say no items found");
        
        // Verify each item in repository is shown in output
        List<WorkItem> items = itemService.findAll();
        for (WorkItem item : items) {
            assertTrue(commandOutput.contains(item.getTitle()), 
                "Output should contain item title: " + item.getTitle());
        }
    }
    
    @And("each item should display its ID, title, type, priority, and status")
    public void eachItemShouldDisplayItsIdTitleTypePriorityAndStatus() {
        List<WorkItem> items = itemService.findAll();
        for (WorkItem item : items) {
            assertTrue(commandOutput.contains(item.getId().toString()), 
                "Output should contain item ID: " + item.getId());
            assertTrue(commandOutput.contains(item.getTitle()), 
                "Output should contain item title: " + item.getTitle());
            assertTrue(commandOutput.contains(item.getType().toString()), 
                "Output should contain item type: " + item.getType());
            assertTrue(commandOutput.contains(item.getPriority().toString()), 
                "Output should contain item priority: " + item.getPriority());
            assertTrue(commandOutput.contains(item.getStatus().toString()), 
                "Output should contain item status: " + item.getStatus());
        }
    }
    
    @Then("I should see only the work items of type {string}")
    public void iShouldSeeOnlyTheWorkItemsOfType(String type) {
        assertNotNull(commandOutput, "Command output should not be null");
        
        List<WorkItem> items = itemService.findAll();
        for (WorkItem item : items) {
            if (item.getType().toString().equals(type)) {
                assertTrue(commandOutput.contains(item.getTitle()), 
                    "Output should contain matching item: " + item.getTitle());
            } else {
                assertFalse(commandOutput.contains(item.getTitle()), 
                    "Output should not contain non-matching item: " + item.getTitle());
            }
        }
    }
    
    @Then("I should see only the work items with status {string}")
    public void iShouldSeeOnlyTheWorkItemsWithStatus(String status) {
        assertNotNull(commandOutput, "Command output should not be null");
        
        List<WorkItem> items = itemService.findAll();
        for (WorkItem item : items) {
            if (item.getStatus().toString().equals(status)) {
                assertTrue(commandOutput.contains(item.getTitle()), 
                    "Output should contain matching item: " + item.getTitle());
            } else {
                assertFalse(commandOutput.contains(item.getTitle()), 
                    "Output should not contain non-matching item: " + item.getTitle());
            }
        }
    }
    
    // Helper methods
    
    private void mockRinListCommand(String type, String status) {
        // This simulates what our CLI tool would do
        List<WorkItem> items;
        
        if (type != null) {
            items = itemService.findByType(type);
        } else if (status != null) {
            items = itemService.findByStatus(status);
        } else {
            items = itemService.findAll();
        }
        
        if (items.isEmpty()) {
            System.out.println("No work items found in the repository.");
            System.out.println();
            System.out.println("To add a new work item, use:");
            System.out.println("  rin add --type FEATURE --title \"Your feature title\" --description \"Description\"");
            System.out.println("  rin add --type BUG --title \"Bug description\" --priority HIGH");
            System.out.println("  rin add --type TASK --title \"Task to do\" --assignee \"username\"");
            return;
        }
        
        System.out.println("Work Items:");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-36s %-40s %-10s %-10s %-12s %s%n", 
                         "ID", "TITLE", "TYPE", "PRIORITY", "STATUS", "ASSIGNEE");
        System.out.println("--------------------------------------------------------------------------------");
        
        for (WorkItem item : items) {
            System.out.printf("%-36s %-40s %-10s %-10s %-12s %s%n",
                             item.getId().toString(),
                             truncate(item.getTitle(), 40),
                             item.getType(),
                             item.getPriority(),
                             item.getStatus(),
                             item.getAssignee() != null ? item.getAssignee() : "-");
        }
        
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("Displaying %d item(s)%n", items.size());
    }
    
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}
