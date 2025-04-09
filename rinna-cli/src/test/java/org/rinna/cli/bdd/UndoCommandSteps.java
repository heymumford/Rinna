/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.bdd;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.rinna.cli.command.UndoCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Step definitions for the Undo Command BDD tests.
 */
public class UndoCommandSteps {

    private TestContext context;
    private MockItemService mockItemService;
    private MockWorkflowService mockWorkflowService;
    private MockHistoryService mockHistoryService;
    private MetadataService mockMetadataService;
    private ServiceManager mockServiceManager;
    private ContextManager mockContextManager;
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final InputStream originalIn = System.in;
    
    private UndoCommand undoCommand;
    private ByteArrayInputStream mockInput;
    private int exitCode;
    
    private static final String TEST_OPERATION_ID = "test-operation-id";
    private static final String TEST_WORK_ITEM_ID = "123e4567-e89b-12d3-a456-426614174000";

    public UndoCommandSteps(TestContext context) {
        this.context = context;
    }

    @Before
    public void setup() {
        // Capture stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mocks
        mockItemService = mock(MockItemService.class);
        mockWorkflowService = mock(MockWorkflowService.class);
        mockHistoryService = mock(MockHistoryService.class);
        mockMetadataService = mock(MetadataService.class);
        mockServiceManager = mock(ServiceManager.class);
        mockContextManager = mock(ContextManager.class);
        
        // Set up mock service manager
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
        when(mockServiceManager.getMockHistoryService()).thenReturn(mockHistoryService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up operation ID for metadata service
        when(mockMetadataService.startOperation(eq("undo"), eq("UPDATE"), any())).thenReturn(TEST_OPERATION_ID);
        when(mockMetadataService.trackOperation(anyString(), any())).thenReturn("sub-operation-id");
        
        // Mock ContextManager.getInstance()
        mockStatic(ContextManager.class);
        when(ContextManager.getInstance()).thenReturn(mockContextManager);
        
        // Add mocks to test context
        context.set("mockItemService", mockItemService);
        context.set("mockWorkflowService", mockWorkflowService);
        context.set("mockHistoryService", mockHistoryService);
        context.set("mockMetadataService", mockMetadataService);
        context.set("mockServiceManager", mockServiceManager);
        context.set("mockContextManager", mockContextManager);
    }

    @After
    public void tearDown() {
        // Restore stdout, stderr, and stdin
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
        
        // Reset output captors
        outputCaptor.reset();
        errorCaptor.reset();
    }

    @Given("the system is initialized")
    public void theSystemIsInitialized() {
        // Nothing to do here, setup is handled in the @Before method
    }

    @Given("the user is authenticated")
    public void theUserIsAuthenticated() {
        // Set current user
        context.set("currentUser", System.getProperty("user.name"));
    }

    @Given("a work item exists with id {string}")
    public void aWorkItemExistsWithId(String workItemId) {
        // Create a test work item
        WorkItem workItem = createTestWorkItem(workItemId);
        when(mockItemService.getItem(workItemId)).thenReturn(workItem);
        when(mockItemService.getItem(TEST_WORK_ITEM_ID)).thenReturn(workItem);
        
        // Store in context
        context.set("workItem", workItem);
        context.set("workItemId", workItemId);
    }

    @Given("the work item is assigned to the current user")
    public void theWorkItemIsAssignedToTheCurrentUser() {
        WorkItem workItem = context.get("workItem", WorkItem.class);
        String currentUser = context.get("currentUser", String.class);
        workItem.setAssignee(currentUser);
    }

    @Given("the work item has history entries")
    public void theWorkItemHasHistoryEntries() {
        // Create basic history with state change
        List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
        history.add(createHistoryEntry("STATE_CHANGE", "john.doe", "NEW → IN_PROGRESS"));
        
        // Configure mock
        when(mockHistoryService.getHistory(any(UUID.class))).thenReturn(history);
        
        // Store in context
        context.set("history", history);
    }

    @Given("the work item has a priority change in its history")
    public void theWorkItemHasAPriorityChangeInItsHistory() {
        // Create history with priority change
        List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
        history.add(createHistoryEntry("FIELD_CHANGE", "john.doe", "Priority: LOW → MEDIUM"));
        
        // Configure mock
        when(mockHistoryService.getHistory(any(UUID.class))).thenReturn(history);
        
        // Store in context
        context.set("history", history);
        context.set("previousPriority", Priority.LOW);
    }

    @Given("the work item has a title change in its history")
    public void theWorkItemHasATitleChangeInItsHistory() {
        // Create history with title change
        List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
        history.add(createHistoryEntry("FIELD_CHANGE", "john.doe", "Title: Old Title → New Title"));
        
        // Configure mock
        when(mockHistoryService.getHistory(any(UUID.class))).thenReturn(history);
        
        // Store in context
        context.set("history", history);
        context.set("previousTitle", "Old Title");
    }

    @Given("the work item has an assignment change in its history")
    public void theWorkItemHasAnAssignmentChangeInItsHistory() {
        // Create history with assignment change
        List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
        history.add(createHistoryEntry("ASSIGNMENT", "john.doe", "alice.smith → john.doe"));
        
        // Configure mock
        when(mockHistoryService.getHistory(any(UUID.class))).thenReturn(history);
        
        // Store in context
        context.set("history", history);
        context.set("previousAssignee", "alice.smith");
    }

    @Given("the user has a work item in progress")
    public void theUserHasAWorkItemInProgress() {
        WorkItem workItem = context.get("workItem", WorkItem.class);
        String currentUser = context.get("currentUser", String.class);
        
        // Ensure the work item is in progress and assigned to current user
        workItem.setState(WorkflowState.IN_PROGRESS);
        workItem.setAssignee(currentUser);
        
        // Configure workflowService to return this item
        when(mockWorkflowService.findByStatus(WorkflowState.IN_PROGRESS))
            .thenReturn(Collections.singletonList(workItem));
        when(mockWorkflowService.getCurrentWorkItem(currentUser)).thenReturn(workItem);
        
        // Set up context manager
        when(mockContextManager.getLastViewedWorkItem()).thenReturn(UUID.fromString(workItem.getId()));
    }

    @Given("the work item has multiple history entries")
    public void theWorkItemHasMultipleHistoryEntries() {
        // Create multiple history entries
        List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
        history.add(createHistoryEntry("STATE_CHANGE", "john.doe", "NEW → IN_PROGRESS"));
        history.add(createHistoryEntry("FIELD_CHANGE", "jane.smith", "Title: Old Title → New Title"));
        history.add(createHistoryEntry("ASSIGNMENT", "john.doe", "alice.smith → john.doe"));
        
        // Configure mock
        when(mockHistoryService.getHistory(any(UUID.class))).thenReturn(history);
        
        // Store in context
        context.set("history", history);
        context.set("previousTitle", "Old Title"); // For step 1
    }

    @Given("the work item is in {string} state")
    public void theWorkItemIsInState(String state) {
        WorkItem workItem = context.get("workItem", WorkItem.class);
        workItem.setState(WorkflowState.valueOf(state));
    }

    @Given("the work item has no history entries")
    public void theWorkItemHasNoHistoryEntries() {
        // Configure mock to return empty history
        when(mockHistoryService.getHistory(any(UUID.class))).thenReturn(Collections.emptyList());
    }

    @When("the user runs the {string} command with the work item ID {string}")
    public void theUserRunsTheCommandWithTheWorkItemID(String command, String workItemId) {
        undoCommand = new UndoCommand(mockServiceManager);
        undoCommand.setItemId(workItemId);
        
        // Store in context
        context.set("undoCommand", undoCommand);
        context.set("workItemIdArg", workItemId);
    }

    @When("the user confirms the undo operation")
    public void theUserConfirmsTheUndoOperation() {
        // Set up user input to confirm
        mockInput = new ByteArrayInputStream("y\n".getBytes());
        System.setIn(mockInput);
        
        // Run the command
        undoCommand = context.get("undoCommand", UndoCommand.class);
        exitCode = undoCommand.call();
        
        // Store in context
        context.set("exitCode", exitCode);
    }

    @When("the user runs the {string} command without specifying a work item ID")
    public void theUserRunsTheCommandWithoutSpecifyingAWorkItemID(String command) {
        undoCommand = new UndoCommand(mockServiceManager);
        // No item ID set
        
        // Store in context
        context.set("undoCommand", undoCommand);
    }

    @When("the user runs the {string} command with the work item ID {string}")
    public void theUserRunsTheUndoCommandWithOptions(String commandWithOptions, String workItemId) {
        undoCommand = new UndoCommand(mockServiceManager);
        undoCommand.setItemId(workItemId);
        
        // Parse options from command string
        if (commandWithOptions.contains("--steps")) {
            undoCommand.setSteps(true);
        }
        if (commandWithOptions.contains("--step")) {
            String stepStr = commandWithOptions.split("--step")[1].trim().split("\\s+")[0];
            undoCommand.setStep(Integer.parseInt(stepStr));
        }
        if (commandWithOptions.contains("--force")) {
            undoCommand.setForce(true);
        }
        if (commandWithOptions.contains("--format json")) {
            undoCommand.setFormat("json");
        }
        if (commandWithOptions.contains("--verbose")) {
            undoCommand.setVerbose(true);
        }
        
        // Store in context
        context.set("undoCommand", undoCommand);
        context.set("workItemIdArg", workItemId);
    }

    @When("the user cancels the undo operation")
    public void theUserCancelsTheUndoOperation() {
        // Set up user input to cancel
        mockInput = new ByteArrayInputStream("n\n".getBytes());
        System.setIn(mockInput);
        
        // Run the command
        undoCommand = context.get("undoCommand", UndoCommand.class);
        exitCode = undoCommand.call();
        
        // Store in context
        context.set("exitCode", exitCode);
    }

    @When("the user runs the {string} command with a non-existent work item ID {string}")
    public void theUserRunsTheCommandWithANonExistentWorkItemID(String command, String nonExistentId) {
        // Configure item service to return null for this ID
        when(mockItemService.getItem(nonExistentId)).thenReturn(null);
        when(mockItemService.findItemByShortId(nonExistentId)).thenReturn(null);
        
        undoCommand = new UndoCommand(mockServiceManager);
        undoCommand.setItemId(nonExistentId);
        
        // Run the command
        exitCode = undoCommand.call();
        
        // Store in context
        context.set("undoCommand", undoCommand);
        context.set("exitCode", exitCode);
    }

    @When("the user runs the {string} command with the work item ID {string} with force flag")
    public void theUserRunsTheCommandWithTheWorkItemIDWithForceFlag(String command, String workItemId) {
        undoCommand = new UndoCommand(mockServiceManager);
        undoCommand.setItemId(workItemId);
        undoCommand.setForce(true);
        
        if (command.contains("--format json")) {
            undoCommand.setFormat("json");
        }
        
        // Run the command
        exitCode = undoCommand.call();
        
        // Store in context
        context.set("undoCommand", undoCommand);
        context.set("exitCode", exitCode);
    }

    @Then("the previous state should be restored")
    public void thePreviousStateShouldBeRestored() {
        // Verify workflowService.transition was called
        verify(mockWorkflowService).transition(
            anyString(),
            anyString(),
            eq(WorkflowState.NEW),
            anyString()
        );
    }

    @Then("a success message should be displayed")
    public void aSuccessMessageShouldBeDisplayed() {
        String output = outputCaptor.toString();
        assertTrue(output.contains("Successfully reverted"), 
                "Output should contain success message but was: " + output);
    }

    @Then("an operation should be tracked with type {string} and action {string}")
    public void anOperationShouldBeTrackedWithTypeAndAction(String type, String action) {
        verify(mockMetadataService).startOperation(eq(type), eq(action), any());
    }

    @Then("the previous priority should be restored")
    public void thePreviousPriorityShouldBeRestored() {
        Priority previousPriority = context.get("previousPriority", Priority.class);
        
        // Verify itemService.updatePriority was called
        verify(mockItemService).updatePriority(
            any(UUID.class),
            eq(previousPriority),
            anyString()
        );
    }

    @Then("the previous title should be restored")
    public void thePreviousTitleShouldBeRestored() {
        String previousTitle = context.get("previousTitle", String.class);
        
        // Verify itemService.updateTitle was called
        verify(mockItemService).updateTitle(
            any(UUID.class),
            eq(previousTitle),
            anyString()
        );
    }

    @Then("the previous assignee should be restored")
    public void thePreviousAssigneeShouldBeRestored() {
        String previousAssignee = context.get("previousAssignee", String.class);
        
        // Verify itemService.assignTo was called
        verify(mockItemService).assignTo(
            any(UUID.class),
            eq(previousAssignee),
            anyString()
        );
    }

    @Then("the previous state of the in-progress work item should be restored")
    public void thePreviousStateOfTheInProgressWorkItemShouldBeRestored() {
        // Verify workflowService.findByStatus was called
        verify(mockWorkflowService).findByStatus(WorkflowState.IN_PROGRESS);
        
        // Verify workflowService.transition was called
        verify(mockWorkflowService).transition(
            anyString(),
            anyString(),
            eq(WorkflowState.NEW),
            anyString()
        );
    }

    @Then("a list of available undo steps should be displayed")
    public void aListOfAvailableUndoStepsShouldBeDisplayed() {
        String output = outputCaptor.toString();
        assertTrue(output.contains("Available changes to undo"), 
                "Output should list available undo steps");
    }

    @Then("no changes should be made to the work item")
    public void noChangesShouldBeMadeToTheWorkItem() {
        // Verify no state transition or updates were called
        verify(mockWorkflowService, never()).transition(
            anyString(),
            anyString(),
            any(WorkflowState.class),
            anyString()
        );
        
        verify(mockItemService, never()).updateTitle(
            any(UUID.class),
            anyString(),
            anyString()
        );
        
        verify(mockItemService, never()).updatePriority(
            any(UUID.class),
            any(Priority.class),
            anyString()
        );
        
        verify(mockItemService, never()).assignTo(
            any(UUID.class),
            anyString(),
            anyString()
        );
    }

    @Then("the specified step should be undone")
    public void theSpecifiedStepShouldBeUndone() {
        String previousTitle = context.get("previousTitle", String.class);
        
        // Verify the specific step was undone (in this case the title change at index 1)
        verify(mockItemService).updateTitle(
            any(UUID.class),
            eq(previousTitle),
            anyString()
        );
    }

    @Then("a cancellation message should be displayed")
    public void aCancellationMessageShouldBeDisplayed() {
        String output = outputCaptor.toString();
        assertTrue(output.contains("Undo operation canceled"), 
                "Output should contain cancellation message");
    }

    @Then("the most recent change should be undone without confirmation")
    public void theMostRecentChangeShouldBeUndoneWithoutConfirmation() {
        // Verify undoCommand was executed with force flag
        assertTrue(undoCommand.isForce());
        
        // Verify workflowService.transition was called
        verify(mockWorkflowService).transition(
            anyString(),
            anyString(),
            eq(WorkflowState.NEW),
            anyString()
        );
    }

    @Then("the output should be in JSON format")
    public void theOutputShouldBeInJsonFormat() {
        String output = outputCaptor.toString();
        assertTrue(output.contains("\"action\":"), "Output should be in JSON format");
        assertTrue(output.contains("\"workItemId\":"), "Output should contain workItemId field");
    }

    @Then("the response should contain success status information")
    public void theResponseShouldContainSuccessStatusInformation() {
        String output = outputCaptor.toString();
        assertTrue(output.contains("\"success\": true"), "JSON output should contain success status");
    }

    @Then("an error message should be displayed indicating the work item was not found")
    public void anErrorMessageShouldBeDisplayedIndicatingTheWorkItemWasNotFound() {
        String errorOutput = errorCaptor.toString();
        assertTrue(errorOutput.contains("Error: Work item not found"), 
                "Error output should indicate work item not found");
    }

    @Then("an operation should be tracked with type {string} and failed status")
    public void anOperationShouldBeTrackedWithTypeAndFailedStatus(String type) {
        verify(mockMetadataService).startOperation(eq(type), anyString(), any());
        verify(mockMetadataService).failOperation(anyString(), any());
    }

    @Then("an error message should be displayed about restricted state")
    public void anErrorMessageShouldBeDisplayedAboutRestrictedState() {
        String errorOutput = errorCaptor.toString();
        assertTrue(errorOutput.contains("Error: Undo history is cleared when work item is closed"), 
                "Error output should indicate restrictions for DONE state");
    }

    @Then("an error message should be displayed about invalid step index")
    public void anErrorMessageShouldBeDisplayedAboutInvalidStepIndex() {
        String errorOutput = errorCaptor.toString();
        assertTrue(
            errorOutput.contains("Error: Cannot undo more than") || 
            errorOutput.contains("Error: Only") || 
            errorOutput.contains("changes are available to undo"), 
            "Error output should indicate invalid step index"
        );
    }

    @Then("an error message should be displayed about no changes to undo")
    public void anErrorMessageShouldBeDisplayedAboutNoChangesToUndo() {
        String errorOutput = errorCaptor.toString();
        assertTrue(errorOutput.contains("Error: No recent changes found to undo"), 
                "Error output should indicate no changes to undo");
    }

    // Helper methods

    private WorkItem createTestWorkItem(String workItemId) {
        WorkItem workItem = new WorkItem();
        workItem.setId(workItemId);
        workItem.setTitle("Test Work Item");
        workItem.setDescription("This is a test description");
        workItem.setType(WorkItemType.TASK);
        workItem.setPriority(Priority.MEDIUM);
        workItem.setState(WorkflowState.IN_PROGRESS);
        workItem.setAssignee(System.getProperty("user.name")); // Current user
        workItem.setCreated(LocalDateTime.now());
        workItem.setUpdated(LocalDateTime.now());
        return workItem;
    }

    private MockHistoryService.HistoryEntryRecord createHistoryEntry(String type, String user, String content) {
        MockHistoryService.HistoryEntryType entryType;
        try {
            entryType = MockHistoryService.HistoryEntryType.valueOf(type);
        } catch (IllegalArgumentException e) {
            entryType = MockHistoryService.HistoryEntryType.UPDATED; // Default
        }
        
        return new MockHistoryService.HistoryEntryRecord(
            UUID.randomUUID(),
            UUID.fromString(context.get("workItemId", String.class)),
            entryType,
            user,
            content,
            Map.of(),
            Date.from(Instant.now().minus(1, ChronoUnit.HOURS))
        );
    }
}