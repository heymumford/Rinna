package org.rinna.bdd;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.domain.model.HistoryEntry;
import org.rinna.domain.model.HistoryEntryType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for the undo action feature.
 */
public class UndoActionSteps {
    private static final Logger logger = LoggerFactory.getLogger(UndoActionSteps.class);
    
    private TestContext context;
    private WorkItem currentWorkItem;
    private String commandOutput;
    private int commandExitCode;
    private List<HistoryEntry> historyEntries;
    private boolean confirmationRequested;
    private boolean confirmationProvided;
    
    /**
     * Constructor with test context.
     *
     * @param context the test context
     */
    public UndoActionSteps(TestContext context) {
        this.context = context;
        this.historyEntries = new ArrayList<>();
    }
    
    @Given("I am logged in as a developer")
    public void iAmLoggedInAsADeveloper() {
        context.put("currentUser", "bob");
    }
    
    @Given("I have a work item in progress")
    public void iHaveAWorkItemInProgress() {
        WorkItem workItem = new WorkItem();
        workItem.setId(UUID.randomUUID().toString());
        workItem.setTitle("Implement authentication feature");
        workItem.setType(org.rinna.cli.model.WorkItemType.TASK);
        workItem.setPriority(Priority.MEDIUM);
        workItem.setState(WorkflowState.IN_PROGRESS);
        workItem.setAssignee("bob");
        
        // Store the work item in the context
        context.put("currentWorkItem", workItem);
        this.currentWorkItem = workItem;
        logger.info("Created work item in progress: {}", workItem.getId());
    }
    
    @Given("I have changed the state of my work item to {string}")
    public void iHaveChangedTheStateOfMyWorkItemTo(String newState) {
        // Store the previous state
        WorkflowState prevState = currentWorkItem.getState();
        context.put("previousState", prevState);
        
        // Update the state
        WorkflowState newStateEnum = WorkflowState.valueOf(newState);
        currentWorkItem.setState(newStateEnum);
        
        // Create history entry
        addHistoryEntry(
            HistoryEntryType.STATE_CHANGE,
            "bob",
            String.format("State changed from %s to %s", prevState, newStateEnum),
            String.format("{\"oldState\":\"%s\",\"newState\":\"%s\"}", prevState, newStateEnum)
        );
        
        logger.info("Changed work item state from {} to {}", prevState, newStateEnum);
    }
    
    @Given("I have changed the state of my work item to {string} more than {int} hours ago")
    public void iHaveChangedTheStateOfMyWorkItemToMoreThanHoursAgo(String newState, int hours) {
        iHaveChangedTheStateOfMyWorkItemTo(newState);
        
        // Adjust the timestamp to be in the past
        HistoryEntry lastEntry = historyEntries.get(historyEntries.size() - 1);
        context.put("oldHistoryEntry", true);
        context.put("historyTimestamp", Instant.now().minus(hours + 1, ChronoUnit.HOURS));
        
        logger.info("Set history entry to be {} hours old", hours + 1);
    }
    
    @Given("I have updated the title of my work item to {string}")
    public void iHaveUpdatedTheTitleOfMyWorkItemTo(String newTitle) {
        // Store the previous title
        String prevTitle = currentWorkItem.getTitle();
        context.put("previousTitle", prevTitle);
        
        // Update the title
        currentWorkItem.setTitle(newTitle);
        
        // Create history entry
        addHistoryEntry(
            HistoryEntryType.FIELD_CHANGE,
            "bob",
            String.format("Title changed from '%s' to '%s'", prevTitle, newTitle),
            String.format("{\"field\":\"title\",\"oldValue\":\"%s\",\"newValue\":\"%s\"}", prevTitle, newTitle)
        );
        
        logger.info("Changed work item title from '{}' to '{}'", prevTitle, newTitle);
    }
    
    @Given("I have assigned my work item to {string}")
    public void iHaveAssignedMyWorkItemTo(String newAssignee) {
        // Store the previous assignee
        String prevAssignee = currentWorkItem.getAssignee();
        context.put("previousAssignee", prevAssignee);
        
        // Update the assignee
        currentWorkItem.setAssignee(newAssignee);
        
        // Create history entry
        addHistoryEntry(
            HistoryEntryType.ASSIGNMENT_CHANGE,
            "bob",
            String.format("Assignee changed from '%s' to '%s'", prevAssignee, newAssignee),
            String.format("{\"field\":\"assignee\",\"oldValue\":\"%s\",\"newValue\":\"%s\"}", prevAssignee, newAssignee)
        );
        
        logger.info("Changed work item assignee from '{}' to '{}'", prevAssignee, newAssignee);
    }
    
    @Given("I have changed the priority of my work item to {string}")
    public void iHaveChangedThePriorityOfMyWorkItemTo(String newPriority) {
        // Store the previous priority
        Priority prevPriority = currentWorkItem.getPriority();
        context.put("previousPriority", prevPriority);
        
        // Update the priority
        Priority newPriorityEnum = Priority.valueOf(newPriority);
        currentWorkItem.setPriority(newPriorityEnum);
        
        // Create history entry
        addHistoryEntry(
            HistoryEntryType.PRIORITY_CHANGE,
            "bob",
            String.format("Priority changed from %s to %s", prevPriority, newPriorityEnum),
            String.format("{\"field\":\"priority\",\"oldValue\":\"%s\",\"newValue\":\"%s\"}", prevPriority, newPriorityEnum)
        );
        
        logger.info("Changed work item priority from {} to {}", prevPriority, newPriorityEnum);
    }
    
    @Given("I have made the following changes to my work item:")
    public void iHaveMadeTheFollowingChangesToMyWorkItem(DataTable dataTable) {
        List<Map<String, String>> changes = dataTable.asMaps(String.class, String.class);
        
        for (Map<String, String> change : changes) {
            String field = change.get("Field");
            String originalValue = change.get("Original Value");
            String newValue = change.get("New Value");
            
            switch (field) {
                case "state":
                    context.put("previousState", WorkflowState.valueOf(originalValue));
                    currentWorkItem.setState(WorkflowState.valueOf(newValue));
                    addHistoryEntry(
                        HistoryEntryType.STATE_CHANGE,
                        "bob",
                        String.format("State changed from %s to %s", originalValue, newValue),
                        String.format("{\"oldState\":\"%s\",\"newState\":\"%s\"}", originalValue, newValue)
                    );
                    break;
                case "priority":
                    context.put("previousPriority", Priority.valueOf(originalValue));
                    currentWorkItem.setPriority(Priority.valueOf(newValue));
                    addHistoryEntry(
                        HistoryEntryType.PRIORITY_CHANGE,
                        "bob",
                        String.format("Priority changed from %s to %s", originalValue, newValue),
                        String.format("{\"field\":\"priority\",\"oldValue\":\"%s\",\"newValue\":\"%s\"}", originalValue, newValue)
                    );
                    break;
                case "assignee":
                    context.put("previousAssignee", originalValue);
                    currentWorkItem.setAssignee(newValue);
                    addHistoryEntry(
                        HistoryEntryType.ASSIGNMENT_CHANGE,
                        "bob",
                        String.format("Assignee changed from '%s' to '%s'", originalValue, newValue),
                        String.format("{\"field\":\"assignee\",\"oldValue\":\"%s\",\"newValue\":\"%s\"}", originalValue, newValue)
                    );
                    break;
                case "title":
                    context.put("previousTitle", originalValue);
                    currentWorkItem.setTitle(newValue);
                    addHistoryEntry(
                        HistoryEntryType.FIELD_CHANGE,
                        "bob",
                        String.format("Title changed from '%s' to '%s'", originalValue, newValue),
                        String.format("{\"field\":\"title\",\"oldValue\":\"%s\",\"newValue\":\"%s\"}", originalValue, newValue)
                    );
                    break;
            }
            
            logger.info("Changed work item {} from '{}' to '{}'", field, originalValue, newValue);
        }
    }
    
    @Given("I have not made any changes to my work item")
    public void iHaveNotMadeAnyChangesToMyWorkItem() {
        // Clear any history entries
        historyEntries.clear();
        logger.info("No history entries for work item");
    }
    
    @Given("I have no work item in progress")
    public void iHaveNoWorkItemInProgress() {
        context.remove("currentWorkItem");
        this.currentWorkItem = null;
        logger.info("No work item in progress");
    }
    
    @Given("I have successfully undone this change")
    public void iHaveSuccessfullyUndoneThisChange() {
        // Remove the last history entry
        if (!historyEntries.isEmpty()) {
            historyEntries.remove(historyEntries.size() - 1);
        }
        
        // Restore previous state
        WorkflowState prevState = (WorkflowState) context.get("previousState");
        if (prevState != null) {
            currentWorkItem.setState(prevState);
        }
        
        logger.info("Undo already performed");
    }
    
    @Given("my work item has been promoted to {string} state")
    public void myWorkItemHasBeenPromotedToState(String state) {
        WorkflowState newState = WorkflowState.valueOf(state);
        WorkflowState prevState = currentWorkItem.getState();
        currentWorkItem.setState(newState);
        
        addHistoryEntry(
            HistoryEntryType.STATE_CHANGE,
            "bob",
            String.format("State changed from %s to %s", prevState, newState),
            String.format("{\"oldState\":\"%s\",\"newState\":\"%s\"}", prevState, newState)
        );
        
        logger.info("Work item promoted to {} state", state);
    }
    
    @Given("there is a work item assigned to {string}")
    public void thereIsAWorkItemAssignedTo(String assignee) {
        WorkItem workItem = new WorkItem();
        workItem.setId("WI-123");
        workItem.setTitle("Feature for another developer");
        workItem.setType(org.rinna.cli.model.WorkItemType.TASK);
        workItem.setPriority(Priority.MEDIUM);
        workItem.setState(WorkflowState.IN_PROGRESS);
        workItem.setAssignee(assignee);
        
        context.put("otherWorkItem", workItem);
        logger.info("Created work item assigned to {}: {}", assignee, workItem.getId());
    }
    
    @Given("I am logged in as {string}")
    public void iAmLoggedInAs(String user) {
        context.put("currentUser", user);
        logger.info("Logged in as {}", user);
    }
    
    @When("I run {string}")
    public void iRun(String command) {
        logger.info("Running command: {}", command);
        
        // Set the basic command result variables
        commandExitCode = 0;
        commandOutput = "";
        confirmationRequested = false;
        confirmationProvided = false;
        
        // Parse the command
        if (command.startsWith("rin undo")) {
            boolean forceFlag = command.contains("--force");
            String itemIdParam = null;
            
            // Check for item ID parameter
            if (command.contains("--item=")) {
                int startIndex = command.indexOf("--item=") + 7;
                int endIndex = command.indexOf(" ", startIndex);
                if (endIndex == -1) {
                    endIndex = command.length();
                }
                itemIdParam = command.substring(startIndex, endIndex);
            }
            
            // Simulate undo command execution
            executeUndoCommand(forceFlag, itemIdParam);
        } else {
            commandExitCode = 1;
            commandOutput = "Unknown command: " + command;
        }
        
        // Store in context
        context.put("commandOutput", commandOutput);
        context.put("commandExitCode", commandExitCode);
        context.put("confirmationRequested", confirmationRequested);
    }
    
    @When("I confirm the undo action")
    public void iConfirmTheUndoAction() {
        logger.info("Confirming undo action");
        confirmationProvided = true;
        
        // Get the last history entry
        if (historyEntries.isEmpty()) {
            commandExitCode = 1;
            commandOutput = "Error: No more changes to undo";
            return;
        }
        
        HistoryEntry lastEntry = historyEntries.get(historyEntries.size() - 1);
        
        // Apply the undo based on entry type
        if (lastEntry.type() == HistoryEntryType.STATE_CHANGE) {
            applyStateChangeUndo(lastEntry);
        } else if (lastEntry.type() == HistoryEntryType.FIELD_CHANGE) {
            applyFieldChangeUndo(lastEntry);
        } else if (lastEntry.type() == HistoryEntryType.ASSIGNMENT_CHANGE) {
            applyAssignmentChangeUndo(lastEntry);
        } else if (lastEntry.type() == HistoryEntryType.PRIORITY_CHANGE) {
            applyPriorityChangeUndo(lastEntry);
        }
        
        // Remove the entry from history
        historyEntries.remove(historyEntries.size() - 1);
        
        // Add a new history entry for the undo action
        addHistoryEntry(
            HistoryEntryType.OTHER,
            (String) context.get("currentUser"),
            "Undid previous change",
            null
        );
        
        commandOutput = "Successfully undid the last change.";
        commandExitCode = 0;
        
        context.put("confirmationProvided", confirmationProvided);
        context.put("commandOutput", commandOutput);
        context.put("commandExitCode", commandExitCode);
    }
    
    @When("I cancel the undo action")
    public void iCancelTheUndoAction() {
        logger.info("Canceling undo action");
        confirmationProvided = false;
        
        commandOutput = "Undo operation canceled";
        commandExitCode = 0;
        
        context.put("confirmationProvided", confirmationProvided);
        context.put("commandOutput", commandOutput);
        context.put("commandExitCode", commandExitCode);
    }
    
    @Then("I should see the current state {string}")
    public void iShouldSeeTheCurrentState(String state) {
        Assert.assertTrue("Output should show current state " + state, 
            commandOutput.contains("Current state: " + state));
    }
    
    @Then("I should see the previous state {string}")
    public void iShouldSeeThePreviousState(String state) {
        Assert.assertTrue("Output should show previous state " + state, 
            commandOutput.contains("Previous state: " + state));
    }
    
    @Then("I should be prompted for confirmation")
    public void iShouldBePromptedForConfirmation() {
        Assert.assertTrue("Should be prompted for confirmation", confirmationRequested);
        Assert.assertTrue("Output should contain confirmation prompt", 
            commandOutput.contains("Do you want to undo this change? [y/N]"));
    }
    
    @Then("I should not be prompted for confirmation")
    public void iShouldNotBePromptedForConfirmation() {
        Assert.assertFalse("Should not be prompted for confirmation", confirmationRequested);
        Assert.assertFalse("Output should not contain confirmation prompt", 
            commandOutput.contains("Do you want to undo this change? [y/N]"));
    }
    
    @Then("the state should be reverted to {string}")
    public void theStateShouldBeRevertedTo(String state) {
        Assert.assertEquals("Work item state should be reverted", 
            WorkflowState.valueOf(state), currentWorkItem.getState());
    }
    
    @Then("the command should succeed")
    public void theCommandShouldSucceed() {
        Assert.assertEquals("Command should succeed with exit code 0", 0, commandExitCode);
    }
    
    @Then("the command should succeed with message {string}")
    public void theCommandShouldSucceedWithMessage(String message) {
        Assert.assertEquals("Command should succeed with exit code 0", 0, commandExitCode);
        Assert.assertTrue("Output should contain message: " + message, 
            commandOutput.contains(message));
    }
    
    @Then("a history entry should be recorded for the undo action")
    public void aHistoryEntryShouldBeRecordedForTheUndoAction() {
        // Check if the last history entry is about the undo action
        Assert.assertFalse("History entries should not be empty", historyEntries.isEmpty());
        HistoryEntry lastEntry = historyEntries.get(historyEntries.size() - 1);
        Assert.assertEquals("Last history entry should be about undo", 
            "Undid previous change", lastEntry.content());
    }
    
    @Then("I should see the current title {string}")
    public void iShouldSeeTheCurrentTitle(String title) {
        Assert.assertTrue("Output should show current title " + title, 
            commandOutput.contains("Current title: " + title));
    }
    
    @Then("I should see the previous title {string}")
    public void iShouldSeeThePreviousTitle(String title) {
        Assert.assertTrue("Output should show previous title " + title, 
            commandOutput.contains("Previous title: " + title));
    }
    
    @Then("the title should be reverted to {string}")
    public void theTitleShouldBeRevertedTo(String title) {
        Assert.assertEquals("Work item title should be reverted", 
            title, currentWorkItem.getTitle());
    }
    
    @Then("I should see the current assignee {string}")
    public void iShouldSeeTheCurrentAssignee(String assignee) {
        Assert.assertTrue("Output should show current assignee " + assignee, 
            commandOutput.contains("Current assignee: " + assignee));
    }
    
    @Then("I should see the previous assignee {string}")
    public void iShouldSeeThePreviousAssignee(String assignee) {
        Assert.assertTrue("Output should show previous assignee " + assignee, 
            commandOutput.contains("Previous assignee: " + assignee));
    }
    
    @Then("the assignee should be reverted to {string}")
    public void theAssigneeShouldBeRevertedTo(String assignee) {
        Assert.assertEquals("Work item assignee should be reverted", 
            assignee, currentWorkItem.getAssignee());
    }
    
    @Then("I should see the current priority {string}")
    public void iShouldSeeTheCurrentPriority(String priority) {
        Assert.assertTrue("Output should show current priority " + priority, 
            commandOutput.contains("Current priority: " + priority));
    }
    
    @Then("I should see the previous priority {string}")
    public void iShouldSeeThePreviousPriority(String priority) {
        Assert.assertTrue("Output should show previous priority " + priority, 
            commandOutput.contains("Previous priority: " + priority));
    }
    
    @Then("the priority should be reverted to {string}")
    public void thePriorityShouldBeRevertedTo(String priority) {
        Assert.assertEquals("Work item priority should be reverted", 
            Priority.valueOf(priority), currentWorkItem.getPriority());
    }
    
    @Then("the state should remain {string}")
    public void theStateShouldRemain(String state) {
        Assert.assertEquals("Work item state should remain unchanged", 
            WorkflowState.valueOf(state), currentWorkItem.getState());
    }
    
    @Then("the command should fail")
    public void theCommandShouldFail() {
        Assert.assertNotEquals("Command should fail with non-zero exit code", 0, commandExitCode);
    }
    
    @Then("I should see an error message {string}")
    public void iShouldSeeAnErrorMessage(String message) {
        Assert.assertTrue("Output should contain error message: " + message, 
            commandOutput.contains(message));
    }
    
    @Then("I should see a tip about viewing in-progress items")
    public void iShouldSeeATipAboutViewingInProgressItems() {
        Assert.assertTrue("Output should contain tip about viewing in-progress items", 
            commandOutput.contains("Tip: Use 'rin list --status=IN_PROGRESS'"));
    }
    
    /**
     * Helper method to add a history entry.
     */
    private void addHistoryEntry(HistoryEntryType type, String user, String content, String additionalData) {
        UUID entryId = UUID.randomUUID();
        UUID workItemId = UUID.fromString(currentWorkItem.getId());
        Instant timestamp = context.containsKey("historyTimestamp") ? 
            (Instant) context.get("historyTimestamp") : Instant.now();
        
        MockHistoryEntry entry = new MockHistoryEntry(
            entryId,
            workItemId,
            type,
            user,
            timestamp,
            content,
            additionalData
        );
        
        historyEntries.add(entry);
    }
    
    /**
     * Helper method to execute the undo command.
     */
    private void executeUndoCommand(boolean forceFlag, String itemIdParam) {
        // Check if there's a work item in progress
        if (currentWorkItem == null && itemIdParam == null) {
            commandExitCode = 1;
            commandOutput = "Error: No work item is currently in progress\n" +
                            "Tip: Use 'rin list --status=IN_PROGRESS' to see in-progress items\n" +
                            "     Use 'rin undo --item=<id>' to undo changes to a specific item";
            return;
        }
        
        // If item ID is specified, check if it's valid
        if (itemIdParam != null) {
            WorkItem targetItem = null;
            if (currentWorkItem != null && currentWorkItem.getId().equals(itemIdParam)) {
                targetItem = currentWorkItem;
            } else if (context.containsKey("otherWorkItem")) {
                WorkItem otherItem = (WorkItem) context.get("otherWorkItem");
                if (otherItem.getId().equals(itemIdParam)) {
                    targetItem = otherItem;
                }
            }
            
            if (targetItem == null) {
                commandExitCode = 1;
                commandOutput = "Error: Work item not found: " + itemIdParam;
                return;
            }
            
            // Check if the user has permission
            String currentUser = (String) context.get("currentUser");
            if (!targetItem.getAssignee().equals(currentUser)) {
                commandExitCode = 1;
                commandOutput = "Error: You do not have permission to undo changes to this work item";
                return;
            }
        }
        
        // Check if there are any history entries
        if (historyEntries.isEmpty()) {
            commandExitCode = 1;
            commandOutput = "Error: No recent changes found to undo";
            return;
        }
        
        // Get the last history entry
        HistoryEntry lastEntry = historyEntries.get(historyEntries.size() - 1);
        
        // Check if the entry is too old
        if (context.containsKey("oldHistoryEntry") && (boolean) context.get("oldHistoryEntry")) {
            commandExitCode = 1;
            commandOutput = "Error: Cannot undo changes older than 24 hours";
            return;
        }
        
        // Check if the work item is in a state that prevents undoing
        if (currentWorkItem.getState() == WorkflowState.RELEASED) {
            commandExitCode = 1;
            commandOutput = "Error: Cannot undo changes to items in RELEASED state";
            return;
        }
        
        // Build the output showing current and previous values
        StringBuilder outputBuilder = new StringBuilder();
        outputBuilder.append("Preparing to undo the last change:\n\n");
        
        if (lastEntry.type() == HistoryEntryType.STATE_CHANGE) {
            // Extract previous and current states from additionalData
            String additionalData = lastEntry.additionalData();
            String currentState = extractValueFromJson(additionalData, "newState");
            String previousState = extractValueFromJson(additionalData, "oldState");
            
            outputBuilder.append("Current state: ").append(currentState).append("\n");
            outputBuilder.append("Previous state: ").append(previousState).append("\n");
        } else if (lastEntry.type() == HistoryEntryType.FIELD_CHANGE) {
            // Extract field, previous and current values from additionalData
            String additionalData = lastEntry.additionalData();
            String field = extractValueFromJson(additionalData, "field");
            String currentValue = extractValueFromJson(additionalData, "newValue");
            String previousValue = extractValueFromJson(additionalData, "oldValue");
            
            if ("title".equals(field)) {
                outputBuilder.append("Current title: ").append(currentValue).append("\n");
                outputBuilder.append("Previous title: ").append(previousValue).append("\n");
            } else {
                outputBuilder.append("Current ").append(field).append(": ").append(currentValue).append("\n");
                outputBuilder.append("Previous ").append(field).append(": ").append(previousValue).append("\n");
            }
        } else if (lastEntry.type() == HistoryEntryType.ASSIGNMENT_CHANGE) {
            // Extract previous and current assignees from additionalData
            String additionalData = lastEntry.additionalData();
            String currentAssignee = extractValueFromJson(additionalData, "newValue");
            String previousAssignee = extractValueFromJson(additionalData, "oldValue");
            
            outputBuilder.append("Current assignee: ").append(currentAssignee).append("\n");
            outputBuilder.append("Previous assignee: ").append(previousAssignee).append("\n");
        } else if (lastEntry.type() == HistoryEntryType.PRIORITY_CHANGE) {
            // Extract previous and current priorities from additionalData
            String additionalData = lastEntry.additionalData();
            String currentPriority = extractValueFromJson(additionalData, "newValue");
            String previousPriority = extractValueFromJson(additionalData, "oldValue");
            
            outputBuilder.append("Current priority: ").append(currentPriority).append("\n");
            outputBuilder.append("Previous priority: ").append(previousPriority).append("\n");
        }
        
        // If force flag is used, apply the undo immediately
        if (forceFlag) {
            // Apply the undo based on entry type
            if (lastEntry.type() == HistoryEntryType.STATE_CHANGE) {
                applyStateChangeUndo(lastEntry);
            } else if (lastEntry.type() == HistoryEntryType.FIELD_CHANGE) {
                applyFieldChangeUndo(lastEntry);
            } else if (lastEntry.type() == HistoryEntryType.ASSIGNMENT_CHANGE) {
                applyAssignmentChangeUndo(lastEntry);
            } else if (lastEntry.type() == HistoryEntryType.PRIORITY_CHANGE) {
                applyPriorityChangeUndo(lastEntry);
            }
            
            // Remove the entry from history
            historyEntries.remove(historyEntries.size() - 1);
            
            // Add a new history entry for the undo action
            addHistoryEntry(
                HistoryEntryType.OTHER,
                (String) context.get("currentUser"),
                "Undid previous change",
                null
            );
            
            outputBuilder.append("\nSuccessfully undid the last change.");
            commandExitCode = 0;
        } else {
            // Request confirmation
            outputBuilder.append("\nDo you want to undo this change? [y/N]: ");
            confirmationRequested = true;
        }
        
        commandOutput = outputBuilder.toString();
    }
    
    /**
     * Apply undo for state change.
     */
    private void applyStateChangeUndo(HistoryEntry entry) {
        String additionalData = entry.additionalData();
        String previousState = extractValueFromJson(additionalData, "oldState");
        currentWorkItem.setState(WorkflowState.valueOf(previousState));
    }
    
    /**
     * Apply undo for field change.
     */
    private void applyFieldChangeUndo(HistoryEntry entry) {
        String additionalData = entry.additionalData();
        String field = extractValueFromJson(additionalData, "field");
        String previousValue = extractValueFromJson(additionalData, "oldValue");
        
        if ("title".equals(field)) {
            currentWorkItem.setTitle(previousValue);
        }
    }
    
    /**
     * Apply undo for assignment change.
     */
    private void applyAssignmentChangeUndo(HistoryEntry entry) {
        String additionalData = entry.additionalData();
        String previousAssignee = extractValueFromJson(additionalData, "oldValue");
        currentWorkItem.setAssignee(previousAssignee);
    }
    
    /**
     * Apply undo for priority change.
     */
    private void applyPriorityChangeUndo(HistoryEntry entry) {
        String additionalData = entry.additionalData();
        String previousPriority = extractValueFromJson(additionalData, "oldValue");
        currentWorkItem.setPriority(Priority.valueOf(previousPriority));
    }
    
    /**
     * Helper method to extract a value from a JSON string.
     */
    private String extractValueFromJson(String json, String key) {
        // Simple JSON parsing for test purposes
        int keyIndex = json.indexOf("\"" + key + "\"");
        if (keyIndex == -1) {
            return null;
        }
        
        int valueStartIndex = json.indexOf("\"", keyIndex + key.length() + 3) + 1;
        int valueEndIndex = json.indexOf("\"", valueStartIndex);
        
        return json.substring(valueStartIndex, valueEndIndex);
    }
    
    /**
     * Mock implementation of HistoryEntry for testing.
     */
    private static class MockHistoryEntry implements HistoryEntry {
        private final UUID id;
        private final UUID workItemId;
        private final HistoryEntryType type;
        private final String user;
        private final Instant timestamp;
        private final String content;
        private final String additionalData;
        
        public MockHistoryEntry(UUID id, UUID workItemId, HistoryEntryType type, String user, 
                               Instant timestamp, String content, String additionalData) {
            this.id = id;
            this.workItemId = workItemId;
            this.type = type;
            this.user = user;
            this.timestamp = timestamp;
            this.content = content;
            this.additionalData = additionalData;
        }
        
        @Override
        public UUID id() {
            return id;
        }
        
        @Override
        public UUID workItemId() {
            return workItemId;
        }
        
        @Override
        public HistoryEntryType type() {
            return type;
        }
        
        @Override
        public String user() {
            return user;
        }
        
        @Override
        public Instant timestamp() {
            return timestamp;
        }
        
        @Override
        public String content() {
            return content;
        }
        
        @Override
        public String additionalData() {
            return additionalData;
        }
    }
}