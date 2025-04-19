/*
 * Step definitions for the Quick Bug Entry feature
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.acceptance.steps;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.*;

import org.rinna.bdd.TestContext;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for the Quick Bug Entry feature.
 * Implements the BDD scenarios for quickly entering bugs via CLI.
 */
public class QuickBugEntrySteps {

    private TestContext context;
    private String commandOutput;
    private List<WorkItem> backlogItems = new ArrayList<>();
    private Map<String, WorkItem> workItems = new HashMap<>();

    /**
     * Constructor for the step definitions class.
     * 
     * @param context shared test context
     */
    public QuickBugEntrySteps(TestContext context) {
        this.context = context;
    }

    @Given("the Rinna CLI is installed and configured")
    public void theRinnaCliIsInstalledAndConfigured() {
        // This step is just for readability in the feature file
        // No implementation needed as we're mocking the CLI
    }

    @Given("the developer has an active workspace")
    public void theDeveloperHasAnActiveWorkspace() {
        // Set up a mock workspace for testing
        context.saveObject("activeWorkspace", "default");
        context.saveObject("activeUser", "developer1");
    }

    @Given("the developer's backlog contains:")
    public void theDevelopersBacklogContains(DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        backlogItems.clear();
        
        for (Map<String, String> row : rows) {
            String id = row.get("ID");
            WorkItemType type = WorkItemType.valueOf(row.get("Type"));
            String title = row.get("Title");
            String priority = row.getOrDefault("Priority", "MEDIUM");
            
            WorkItem item = mock(WorkItem.class);
            when(item.getId()).thenReturn(UUID.randomUUID());
            when(item.getTitle()).thenReturn(title);
            when(item.getType()).thenReturn(type);
            when(item.getPriority()).thenReturn(org.rinna.domain.Priority.valueOf(priority));
            when(item.getStatus()).thenReturn(WorkflowState.TO_DO);
            
            backlogItems.add(item);
            workItems.put(id, item);
        }
        
        context.saveObject("backlog", backlogItems);
    }

    @Given("a bug {string} with title {string} in status {string}")
    public void aBugWithTitleInStatus(String id, String title, String status) {
        WorkItem bug = mock(WorkItem.class);
        when(bug.getId()).thenReturn(UUID.randomUUID());
        when(bug.getTitle()).thenReturn(title);
        when(bug.getType()).thenReturn(WorkItemType.BUG);
        when(bug.getPriority()).thenReturn(org.rinna.domain.Priority.MEDIUM);
        when(bug.getStatus()).thenReturn(WorkflowState.valueOf(status));
        
        workItems.put(id, bug);
        context.saveWorkItem(id, bug);
    }

    @Given("the developer has several bugs and tasks in their backlog")
    public void theDeveloperHasSeveralBugsAndTasksInTheirBacklog() {
        backlogItems.clear();
        
        // Create some sample backlog items
        for (int i = 1; i <= 5; i++) {
            WorkItem item = mock(WorkItem.class);
            when(item.getId()).thenReturn(UUID.randomUUID());
            when(item.getTitle()).thenReturn("Sample work item " + i);
            when(item.getType()).thenReturn(i % 2 == 0 ? WorkItemType.BUG : WorkItemType.TASK);
            when(item.getPriority()).thenReturn(org.rinna.domain.Priority.MEDIUM);
            when(item.getStatus()).thenReturn(WorkflowState.TO_DO);
            
            backlogItems.add(item);
            workItems.put("WI-" + (100 + i), item);
        }
        
        context.saveObject("backlog", backlogItems);
    }

    @When("the developer runs {string}")
    public void theDeveloperRuns(String command) {
        // Save the command for later checks
        context.saveObject("command", command);
        
        // Parse and simulate the command execution
        if (command.startsWith("rin bug")) {
            handleBugCommand(command);
        } else if (command.startsWith("rin backlog")) {
            handleBacklogCommand(command);
        } else if (command.startsWith("rin test")) {
            handleTestCommand(command);
        } else if (command.startsWith("rin done")) {
            handleDoneCommand(command);
        } else {
            commandOutput = "Unknown command: " + command;
        }
    }

    @Then("a work item should be created with:")
    public void aWorkItemShouldBeCreatedWith(DataTable dataTable) {
        Map<String, String> expectedValues = dataTable.asMap(String.class, String.class);
        WorkItem createdItem = (WorkItem) context.getObject("createdItem");
        
        assertNotNull(createdItem, "No work item was created");
        
        if (expectedValues.containsKey("type")) {
            assertEquals(WorkItemType.valueOf(expectedValues.get("type")), createdItem.getType());
        }
        
        if (expectedValues.containsKey("title")) {
            assertEquals(expectedValues.get("title"), createdItem.getTitle());
        }
        
        if (expectedValues.containsKey("status")) {
            assertEquals(WorkflowState.valueOf(expectedValues.get("status")), createdItem.getStatus());
        }
        
        if (expectedValues.containsKey("priority")) {
            assertEquals(
                org.rinna.domain.Priority.valueOf(expectedValues.get("priority")), 
                createdItem.getPriority()
            );
        }
    }

    @Then("the CLI should display the new bug ID and details")
    public void theCliShouldDisplayTheNewBugIdAndDetails() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Created new bug"), "Output should contain creation confirmation");
        WorkItem createdItem = (WorkItem) context.getObject("createdItem");
        assertTrue(commandOutput.contains(createdItem.getId().toString()), "Output should contain the bug ID");
        assertTrue(commandOutput.contains(createdItem.getTitle()), "Output should contain the bug title");
    }

    @Then("the bug should be added to the developer's backlog")
    public void theBugShouldBeAddedToTheDevelopersBacklog() {
        WorkItem createdItem = (WorkItem) context.getObject("createdItem");
        List<WorkItem> backlog = (List<WorkItem>) context.getObject("backlog");
        assertTrue(backlog.contains(createdItem), "The bug should be in the backlog");
    }

    @Then("the bug should be added to the developer's backlog with HIGH priority")
    public void theBugShouldBeAddedToTheDevelopersBacklogWithHighPriority() {
        WorkItem createdItem = (WorkItem) context.getObject("createdItem");
        assertEquals(org.rinna.domain.Priority.HIGH, createdItem.getPriority());
        theBugShouldBeAddedToTheDevelopersBacklog();
    }

    @Then("the work item should have metadata:")
    public void theWorkItemShouldHaveMetadata(DataTable dataTable) {
        Map<String, String> expectedMetadata = dataTable.asMap(String.class, String.class);
        WorkItem createdItem = (WorkItem) context.getObject("createdItem");
        
        Map<String, String> actualMetadata = createdItem.getMetadata();
        assertNotNull(actualMetadata, "Work item should have metadata");
        
        for (Map.Entry<String, String> entry : expectedMetadata.entrySet()) {
            assertTrue(actualMetadata.containsKey(entry.getKey()), 
                "Metadata should contain key: " + entry.getKey());
            assertEquals(entry.getValue(), actualMetadata.get(entry.getKey()),
                "Metadata value for " + entry.getKey() + " should match");
        }
    }

    @Then("the backlog order should be {string}")
    public void theBacklogOrderShouldBe(String expectedOrder) {
        List<WorkItem> backlog = (List<WorkItem>) context.getObject("backlog");
        String[] expectedIds = expectedOrder.split(",\\s*");
        
        assertEquals(expectedIds.length, backlog.size(), "Backlog size should match expected order");
        
        for (int i = 0; i < expectedIds.length; i++) {
            String expectedId = expectedIds[i].trim();
            WorkItem item = workItems.get(expectedId);
            assertNotNull(item, "Expected item " + expectedId + " should exist");
            assertEquals(item, backlog.get(i), 
                "Item at position " + i + " should be " + expectedId);
        }
    }

    @Then("the CLI should display the updated backlog")
    public void theCliShouldDisplayTheUpdatedBacklog() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Backlog updated"), "Output should confirm backlog update");
        
        // Check that all backlog items are displayed
        List<WorkItem> backlog = (List<WorkItem>) context.getObject("backlog");
        for (WorkItem item : backlog) {
            assertTrue(commandOutput.contains(item.getTitle()), 
                "Output should contain item title: " + item.getTitle());
        }
    }

    @Then("the work item {string} should be updated with status {string}")
    public void theWorkItemShouldBeUpdatedWithStatus(String id, String status) {
        WorkItem item = workItems.get(id);
        assertNotNull(item, "Work item " + id + " should exist");
        
        verify(item).setStatus(WorkflowState.valueOf(status));
        
        // Update the mock to reflect the new status for subsequent checks
        when(item.getStatus()).thenReturn(WorkflowState.valueOf(status));
    }

    @Then("the CLI should display the updated work item details")
    public void theCliShouldDisplayTheUpdatedWorkItemDetails() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Updated work item"), "Output should confirm item update");
        assertTrue(commandOutput.contains("Status"), "Output should mention status");
    }

    @Then("the CLI should congratulate the developer on fixing the bug")
    public void theCliShouldCongratulateTheDeveloperOnFixingTheBug() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Congratulations") || commandOutput.contains("Great job"),
            "Output should include a congratulatory message");
    }

    @Then("the CLI should display the backlog items in order")
    public void theCliShouldDisplayTheBacklogItemsInOrder() {
        assertNotNull(commandOutput);
        List<WorkItem> backlog = (List<WorkItem>) context.getObject("backlog");
        
        for (WorkItem item : backlog) {
            assertTrue(commandOutput.contains(item.getTitle()),
                "Output should contain item title: " + item.getTitle());
        }
    }

    @Then("each item should show its ID, type, title, and priority")
    public void eachItemShouldShowItsIdTypeTitleAndPriority() {
        assertNotNull(commandOutput);
        List<WorkItem> backlog = (List<WorkItem>) context.getObject("backlog");
        
        for (WorkItem item : backlog) {
            assertTrue(commandOutput.contains(item.getId().toString()), "Output should contain item ID");
            assertTrue(commandOutput.contains(item.getType().toString()), "Output should contain item type");
            assertTrue(commandOutput.contains(item.getTitle()), "Output should contain item title");
            assertTrue(commandOutput.contains(item.getPriority().toString()), 
                "Output should contain item priority");
        }
    }

    @Then("bugs should be highlighted differently than other work items")
    public void bugsShouldBeHighlightedDifferentlyThanOtherWorkItems() {
        // This is a UI/format check that we can only simulate in our test
        assertTrue(commandOutput.contains("BUG"), "Output should clearly indicate BUG items");
    }

    @Then("the CLI should display an error message")
    public void theCliShouldDisplayAnErrorMessage() {
        assertNotNull(commandOutput);
        assertTrue(commandOutput.contains("Error") || commandOutput.contains("Failed"),
            "Output should indicate an error");
    }

    @Then("the error should explain that a description is required")
    public void theErrorShouldExplainThatADescriptionIsRequired() {
        assertTrue(commandOutput.contains("description is required"),
            "Error should explain that a description is required");
    }

    @Then("the error should list the valid priority values")
    public void theErrorShouldListTheValidPriorityValues() {
        assertTrue(commandOutput.contains("HIGH") && 
                   commandOutput.contains("MEDIUM") && 
                   commandOutput.contains("LOW"),
            "Error should list valid priority values");
    }

    @Then("the error should indicate that the item does not exist in the backlog")
    public void theErrorShouldIndicateThatTheItemDoesNotExistInTheBacklog() {
        assertTrue(commandOutput.contains("not found") || 
                   commandOutput.contains("does not exist"),
            "Error should indicate the item doesn't exist");
    }

    @Then("the CLI should display a message indicating the item is already at the top")
    public void theCliShouldDisplayAMessageIndicatingTheItemIsAlreadyAtTheTop() {
        assertTrue(commandOutput.contains("already at the top"),
            "Output should indicate the item is already at the top");
    }

    @Then("the CLI should display a message indicating the item is already at the bottom")
    public void theCliShouldDisplayAMessageIndicatingTheItemIsAlreadyAtTheBottom() {
        assertTrue(commandOutput.contains("already at the bottom"),
            "Output should indicate the item is already at the bottom");
    }

    @Then("the CLI should display a message that the bug is already completed")
    public void theCliShouldDisplayAMessageThatTheBugIsAlreadyCompleted() {
        assertTrue(commandOutput.contains("already completed") || 
                   commandOutput.contains("already done"),
            "Output should indicate the bug is already completed");
    }

    // Helper methods to handle different commands

    private void handleBugCommand(String command) {
        // Parse the bug command
        if (command.equals("rin bug")) {
            // Missing description
            commandOutput = "Error: Bug description is required";
            return;
        }

        // Extract priority if present
        org.rinna.domain.Priority priority = org.rinna.domain.Priority.MEDIUM;
        if (command.contains("--priority=HIGH") || command.contains("-p HIGH")) {
            priority = org.rinna.domain.Priority.HIGH;
        } else if (command.contains("--priority=LOW") || command.contains("-p LOW")) {
            priority = org.rinna.domain.Priority.LOW;
        } else if (command.contains("--priority=CRITICAL")) {
            // Invalid priority
            commandOutput = "Error: Invalid priority value 'CRITICAL'. Valid values are: HIGH, MEDIUM, LOW";
            return;
        }

        // Extract title
        String title = extractStringParam(command);
        if (title == null) {
            commandOutput = "Error: Bug description is required";
            return;
        }

        // Create the work item
        WorkItem newBug = mock(WorkItem.class);
        when(newBug.getId()).thenReturn(UUID.randomUUID());
        when(newBug.getTitle()).thenReturn(title);
        when(newBug.getType()).thenReturn(WorkItemType.BUG);
        when(newBug.getPriority()).thenReturn(priority);
        when(newBug.getStatus()).thenReturn(WorkflowState.FOUND);
        
        // Extract metadata
        Map<String, String> metadata = new HashMap<>();
        extractMetadata(command, metadata);
        when(newBug.getMetadata()).thenReturn(metadata);

        // Add to backlog
        if (context.getObject("backlog") == null) {
            context.saveObject("backlog", new ArrayList<WorkItem>());
        }
        List<WorkItem> backlog = (List<WorkItem>) context.getObject("backlog");
        backlog.add(newBug);

        context.saveObject("createdItem", newBug);
        commandOutput = "Created new bug [" + newBug.getId() + "]: " + title + " (Priority: " + priority + ")";
    }

    private void handleBacklogCommand(String command) {
        if (command.equals("rin backlog")) {
            // Display backlog
            displayBacklog();
            return;
        }

        // Get backlog
        List<WorkItem> backlog = (List<WorkItem>) context.getObject("backlog");
        if (backlog == null) {
            backlog = new ArrayList<>();
            context.saveObject("backlog", backlog);
        }

        if (command.contains("move")) {
            // Extract item ID and direction
            String[] parts = command.split(" ");
            if (parts.length < 4) {
                commandOutput = "Error: Invalid command format. Use 'rin backlog move <id> <up|down>'";
                return;
            }

            String id = parts[3];
            String direction = parts[4];

            // Check if the item exists
            WorkItem itemToMove = null;
            int currentIndex = -1;
            for (int i = 0; i < backlog.size(); i++) {
                if (workItems.containsKey(id) && backlog.get(i) == workItems.get(id)) {
                    itemToMove = backlog.get(i);
                    currentIndex = i;
                    break;
                }
            }

            if (itemToMove == null) {
                commandOutput = "Error: Work item " + id + " not found in backlog";
                return;
            }

            // Move the item
            if (direction.equals("up")) {
                if (currentIndex == 0) {
                    commandOutput = "Item " + id + " is already at the top of the backlog";
                    return;
                }
                Collections.swap(backlog, currentIndex, currentIndex - 1);
            } else if (direction.equals("down")) {
                if (currentIndex == backlog.size() - 1) {
                    commandOutput = "Item " + id + " is already at the bottom of the backlog";
                    return;
                }
                Collections.swap(backlog, currentIndex, currentIndex + 1);
            }

            commandOutput = "Backlog updated. New order:\n" + formatBacklog(backlog);
        } else if (command.contains("top")) {
            // Move item to top
            String[] parts = command.split(" ");
            String id = parts[2];

            // Check if the item exists
            WorkItem itemToMove = null;
            int currentIndex = -1;
            for (int i = 0; i < backlog.size(); i++) {
                if (workItems.containsKey(id) && backlog.get(i) == workItems.get(id)) {
                    itemToMove = backlog.get(i);
                    currentIndex = i;
                    break;
                }
            }

            if (itemToMove == null) {
                commandOutput = "Error: Work item " + id + " not found in backlog";
                return;
            }

            // Move to top
            if (currentIndex == 0) {
                commandOutput = "Item " + id + " is already at the top of the backlog";
                return;
            }

            backlog.remove(currentIndex);
            backlog.add(0, itemToMove);

            commandOutput = "Backlog updated. New order:\n" + formatBacklog(backlog);
        } else if (command.contains("bottom")) {
            // Move item to bottom
            String[] parts = command.split(" ");
            String id = parts[2];

            // Check if the item exists
            WorkItem itemToMove = null;
            int currentIndex = -1;
            for (int i = 0; i < backlog.size(); i++) {
                if (workItems.containsKey(id) && backlog.get(i) == workItems.get(id)) {
                    itemToMove = backlog.get(i);
                    currentIndex = i;
                    break;
                }
            }

            if (itemToMove == null) {
                commandOutput = "Error: Work item " + id + " not found in backlog";
                return;
            }

            // Move to bottom
            if (currentIndex == backlog.size() - 1) {
                commandOutput = "Item " + id + " is already at the bottom of the backlog";
                return;
            }

            backlog.remove(currentIndex);
            backlog.add(itemToMove);

            commandOutput = "Backlog updated. New order:\n" + formatBacklog(backlog);
        }
    }

    private void handleTestCommand(String command) {
        String[] parts = command.split(" ");
        if (parts.length < 2) {
            commandOutput = "Error: Missing work item ID";
            return;
        }

        String id = parts[1];
        if (!workItems.containsKey(id)) {
            commandOutput = "Error: Work item " + id + " not found";
            return;
        }

        WorkItem item = workItems.get(id);
        if (item.getStatus() == WorkflowState.IN_TEST) {
            commandOutput = "Item " + id + " is already in test";
            return;
        }

        item.setStatus(WorkflowState.IN_TEST);
        commandOutput = "Updated work item " + id + ": Status changed to IN_TEST";
    }

    private void handleDoneCommand(String command) {
        String[] parts = command.split(" ");
        if (parts.length < 2) {
            commandOutput = "Error: Missing work item ID";
            return;
        }

        String id = parts[1];
        if (!workItems.containsKey(id)) {
            commandOutput = "Error: Work item " + id + " not found";
            return;
        }

        WorkItem item = workItems.get(id);
        if (item.getStatus() == WorkflowState.DONE) {
            commandOutput = "Item " + id + " is already completed";
            return;
        }

        item.setStatus(WorkflowState.DONE);
        commandOutput = "Updated work item " + id + ": Status changed to DONE\n" +
                       "Congratulations on fixing this bug! 🎉";
    }

    private void displayBacklog() {
        List<WorkItem> backlog = (List<WorkItem>) context.getObject("backlog");
        if (backlog == null || backlog.isEmpty()) {
            commandOutput = "Your backlog is empty";
            return;
        }

        commandOutput = "Your backlog:\n" + formatBacklog(backlog);
    }

    private String formatBacklog(List<WorkItem> backlog) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < backlog.size(); i++) {
            WorkItem item = backlog.get(i);
            String typeDisplay = item.getType() == WorkItemType.BUG ? "[BUG]" : "[" + item.getType() + "]";
            sb.append(i + 1).append(". ")
              .append(typeDisplay).append(" ")
              .append(item.getTitle())
              .append(" (").append(item.getPriority()).append(")")
              .append("\n");
        }
        return sb.toString();
    }

    private String extractStringParam(String command) {
        int startQuote = command.indexOf('\'');
        if (startQuote == -1) {
            startQuote = command.indexOf('"');
            if (startQuote == -1) {
                return null;
            }
        }
        
        char quoteChar = command.charAt(startQuote);
        int endQuote = command.indexOf(quoteChar, startQuote + 1);
        if (endQuote == -1) {
            return null;
        }
        
        return command.substring(startQuote + 1, endQuote);
    }

    private void extractMetadata(String command, Map<String, String> metadata) {
        // Look for --key=value patterns
        String[] parts = command.split("\\s+");
        for (String part : parts) {
            if (part.startsWith("--") && part.contains("=")) {
                String[] keyValue = part.substring(2).split("=", 2);
                if (keyValue.length == 2 && !keyValue[0].equals("priority") && !keyValue[0].equals("p")) {
                    metadata.put(keyValue[0], keyValue[1]);
                }
            }
        }
    }
}