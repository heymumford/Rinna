/*
 * Step definitions for workflow command BDD tests
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.bdd;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mockito.Mockito;
import org.rinna.cli.command.WorkflowCommand;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockCommentService;
import org.rinna.cli.service.MockHistoryService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemCreateRequest;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.model.Priority;
import org.rinna.domain.model.Comment;
import org.rinna.domain.model.CommentType;
import org.rinna.domain.model.HistoryEntry;
import org.rinna.domain.model.HistoryEntryType;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Step definitions for workflow command tests.
 */
public class WorkflowCommandSteps {

    private final TestContext testContext;
    private final ByteArrayOutputStream outContent;
    private final ByteArrayOutputStream errContent;
    private final Map<String, WorkItem> testWorkItems = new HashMap<>();
    private final Map<String, List<HistoryEntry>> workItemHistory = new HashMap<>();
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructor with test context injection.
     *
     * @param testContext the shared test context
     */
    public WorkflowCommandSteps(TestContext testContext) {
        this.testContext = testContext;
        this.outContent = testContext.getOutContent();
        this.errContent = testContext.getErrContent();
    }

    @Given("I am logged in as {string}")
    public void iAmLoggedInAs(String username) {
        ConfigurationService mockConfigService = testContext.getMockConfigService();
        when(mockConfigService.getCurrentUser()).thenReturn(username);
        when(mockConfigService.isAuthenticated()).thenReturn(true);
    }

    @Given("the system has the following work items:")
    public void theSystemHasTheFollowingWorkItems(DataTable dataTable) {
        MockItemService mockItemService = testContext.getMockItemService();
        MockWorkflowService mockWorkflowService = testContext.getMockWorkflowService();
        ServiceManager mockServiceManager = testContext.getMockServiceManager();
        
        // Setup service manager mocks
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getWorkflowService()).thenReturn(mockWorkflowService);
        
        // Extract work items from data table
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        List<WorkItem> workItems = new ArrayList<>();
        
        for (Map<String, String> row : rows) {
            String id = row.get("ID");
            String title = row.get("Title");
            String description = row.get("Description");
            String type = row.get("Type");
            String priority = row.get("Priority");
            String status = row.get("Status");
            String assignee = row.get("Assignee");
            
            // Create work item
            WorkItem workItem = createTestWorkItem(
                id, 
                title, 
                description, 
                type, 
                Priority.valueOf(priority), 
                WorkflowState.valueOf(status), 
                assignee
            );
            
            workItems.add(workItem);
            testWorkItems.put(id, workItem);
            
            // Initialize empty history for this work item
            workItemHistory.put(id, new ArrayList<>());
            
            // Setup mock service to return this work item
            when(mockItemService.getItem(id)).thenReturn(workItem);
        }
        
        // Setup mock service to return all work items
        when(mockItemService.getAllItems()).thenReturn(workItems);
        
        // Setup mock workflow service for valid transitions
        setupDefaultWorkflowTransitions(mockWorkflowService);
    }

    @Given("the work item {string} is blocked with reason {string}")
    public void theWorkItemIsBlockedWithReason(String workItemId, String reason) {
        MockItemService mockItemService = testContext.getMockItemService();
        WorkItem workItem = testWorkItems.get(workItemId);
        
        // Update the work item status to BLOCKED
        WorkItem blockedWorkItem = updateWorkItemStatus(workItem, WorkflowState.BLOCKED);
        testWorkItems.put(workItemId, blockedWorkItem);
        
        // Mock the blocking reason
        when(mockItemService.getBlockingReason(workItemId)).thenReturn(reason);
        
        // Update the mock service to return the updated work item
        when(mockItemService.getItem(workItemId)).thenReturn(blockedWorkItem);
    }

    @Given("the work item {string} has the following workflow history:")
    public void theWorkItemHasTheFollowingWorkflowHistory(String workItemId, DataTable dataTable) {
        MockHistoryService mockHistoryService = testContext.getMockHistoryService();
        
        // Extract history entries from data table
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        List<HistoryEntry> historyEntries = new ArrayList<>();
        
        for (Map<String, String> row : rows) {
            String fromState = row.get("From");
            String toState = row.get("To");
            String user = row.get("User");
            String timestamp = row.get("Timestamp");
            String comment = row.get("Comment");
            
            // Create history entry
            HistoryEntry entry = createHistoryEntry(
                workItemId,
                fromState, 
                toState, 
                user, 
                timestamp, 
                comment
            );
            
            historyEntries.add(entry);
        }
        
        // Store history entries for this work item
        workItemHistory.put(workItemId, historyEntries);
        
        // Setup mock service to return this work item's history
        when(mockHistoryService.getWorkItemHistory(workItemId)).thenReturn(historyEntries);
    }

    @When("I run the command {string}")
    public void iRunTheCommand(String commandLine) {
        // Parse the command line
        String[] parts = commandLine.split("\\s+");
        String[] args = Arrays.copyOfRange(parts, 2, parts.length);  // Skip "rin workflow" prefix
        
        testContext.getCommandProcessor().processCommand(parts[1], args);
    }

    @Then("the command should execute successfully")
    public void theCommandShouldExecuteSuccessfully() {
        assertEquals(0, testContext.getLastExitCode(), 
            "Command should have returned 0 exit code. Error: " + errContent.toString());
    }

    @Then("the command should fail with error code {int}")
    public void theCommandShouldFailWithErrorCode(int errorCode) {
        assertEquals(errorCode, testContext.getLastExitCode(), 
            "Command should have returned " + errorCode + " exit code");
    }

    @Then("the output should contain {string}")
    public void theOutputShouldContain(String expectedText) {
        String output = outContent.toString();
        assertTrue(output.contains(expectedText), 
            "Output should contain '" + expectedText + "' but was:\n" + output);
    }

    @Then("the error output should contain {string}")
    public void theErrorOutputShouldContain(String expectedText) {
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains(expectedText), 
            "Error output should contain '" + expectedText + "' but was:\n" + errorOutput);
    }

    @Then("the output should not contain {string}")
    public void theOutputShouldNotContain(String expectedText) {
        String output = outContent.toString();
        assertFalse(output.contains(expectedText), 
            "Output should not contain '" + expectedText + "' but was:\n" + output);
    }

    @Then("the work item {string} should have status {string}")
    public void theWorkItemShouldHaveStatus(String workItemId, String status) {
        MockItemService mockItemService = testContext.getMockItemService();
        verify(mockItemService).updateState(eq(UUID.fromString(workItemId)), eq(WorkflowState.valueOf(status)), any());
    }

    @Then("the work item {string} should have blocking reason {string}")
    public void theWorkItemShouldHaveBlockingReason(String workItemId, String reason) {
        MockWorkflowService mockWorkflowService = testContext.getMockWorkflowService();
        verify(mockWorkflowService).blockWorkItem(eq(workItemId), eq(reason));
    }

    @Then("the work item {string} should not have a blocking reason")
    public void theWorkItemShouldNotHaveABlockingReason(String workItemId) {
        MockWorkflowService mockWorkflowService = testContext.getMockWorkflowService();
        verify(mockWorkflowService).unblockWorkItem(eq(workItemId));
    }

    @Then("the work item history should show a transition to {string}")
    public void theWorkItemHistoryShouldShowATransitionTo(String status) {
        MockHistoryService mockHistoryService = testContext.getMockHistoryService();
        verify(mockHistoryService).addHistoryEntry(any(HistoryEntry.class));
    }

    @Then("the work item {string} should have a comment {string}")
    public void theWorkItemShouldHaveAComment(String workItemId, String commentText) {
        MockCommentService mockCommentService = testContext.getMockCommentService();
        verify(mockCommentService).addComment(eq(workItemId), eq(commentText), any());
    }

    @Then("the output should contain all workflow transitions in chronological order")
    public void theOutputShouldContainAllWorkflowTransitionsInChronologicalOrder() {
        String output = outContent.toString();
        
        // Check that the output contains all transitions in the correct order
        List<Map<String, String>> expectedTransitions = new ArrayList<>();
        expectedTransitions.add(Map.of(
            "From", "CREATED", 
            "To", "READY", 
            "User", "admin", 
            "Comment", "Initial state"
        ));
        
        expectedTransitions.add(Map.of(
            "From", "READY", 
            "To", "IN_PROGRESS", 
            "User", "testuser", 
            "Comment", "Started implementation"
        ));
        
        expectedTransitions.add(Map.of(
            "From", "IN_PROGRESS", 
            "To", "BLOCKED", 
            "User", "testuser", 
            "Comment", "Blocked on API issue"
        ));
        
        expectedTransitions.add(Map.of(
            "From", "BLOCKED", 
            "To", "IN_PROGRESS", 
            "User", "testuser", 
            "Comment", "API issue resolved"
        ));
        
        int lastPos = -1;
        for (Map<String, String> transition : expectedTransitions) {
            String from = transition.get("From");
            String to = transition.get("To");
            String user = transition.get("User");
            String comment = transition.get("Comment");
            
            // Create a pattern that matches this transition
            String pattern = String.format("%s → %s.*%s.*%s", from, to, user, comment);
            
            // Find the position of this pattern in the output
            int pos = findPatternPosition(output, pattern);
            
            assertTrue(pos > -1, "Output should contain transition: " + pattern);
            assertTrue(pos > lastPos, "Transitions should be in chronological order");
            
            lastPos = pos;
        }
    }

    @Then("the output should show comments associated with each transition")
    public void theOutputShouldShowCommentsAssociatedWithEachTransition() {
        String output = outContent.toString();
        
        // Check that the output contains all comments
        List<String> expectedComments = Arrays.asList(
            "Initial state",
            "Started implementation",
            "Blocked on API issue",
            "API issue resolved"
        );
        
        for (String comment : expectedComments) {
            assertTrue(output.contains(comment), 
                "Output should contain comment: " + comment);
        }
    }

    @Then("the work item {string} should have assignee {string}")
    public void theWorkItemShouldHaveAssignee(String workItemId, String assignee) {
        MockItemService mockItemService = testContext.getMockItemService();
        verify(mockItemService).assignTo(eq(UUID.fromString(workItemId)), eq(assignee), any());
    }

    @Then("the work item {string} should have priority {string}")
    public void theWorkItemShouldHavePriority(String workItemId, String priority) {
        MockItemService mockItemService = testContext.getMockItemService();
        verify(mockItemService).updatePriority(eq(UUID.fromString(workItemId)), eq(Priority.valueOf(priority)), any());
    }

    // Helper methods
    
    private WorkItem createTestWorkItem(
            String id, 
            String title, 
            String description, 
            String type, 
            Priority priority, 
            WorkflowState status, 
            String assignee) {
        
        // This is a simplified version of the WorkItem creation
        // In a real implementation, this would use the actual WorkItem class
        WorkItem workItem = Mockito.mock(WorkItem.class);
        
        // Setup basic properties
        when(workItem.getId()).thenReturn(id);
        when(workItem.getTitle()).thenReturn(title);
        when(workItem.getDescription()).thenReturn(description);
        when(workItem.getType()).thenReturn(type);
        when(workItem.getPriority()).thenReturn(priority);
        when(workItem.getStatus()).thenReturn(status);
        when(workItem.getAssignee()).thenReturn(assignee);
        
        return workItem;
    }
    
    private WorkItem updateWorkItemStatus(WorkItem workItem, WorkflowState newStatus) {
        // Create a new mock work item with the updated status
        WorkItem updatedWorkItem = Mockito.mock(WorkItem.class);
        
        // Copy all properties from the original work item
        when(updatedWorkItem.getId()).thenReturn(workItem.getId());
        when(updatedWorkItem.getTitle()).thenReturn(workItem.getTitle());
        when(updatedWorkItem.getDescription()).thenReturn(workItem.getDescription());
        when(updatedWorkItem.getType()).thenReturn(workItem.getType());
        when(updatedWorkItem.getPriority()).thenReturn(workItem.getPriority());
        when(updatedWorkItem.getAssignee()).thenReturn(workItem.getAssignee());
        
        // Update the status
        when(updatedWorkItem.getStatus()).thenReturn(newStatus);
        
        return updatedWorkItem;
    }
    
    private HistoryEntry createHistoryEntry(
            String workItemId,
            String fromState, 
            String toState, 
            String user, 
            String timestamp, 
            String comment) {
        
        // This is a simplified version of the HistoryEntry creation
        // In a real implementation, this would use the actual HistoryEntry class
        HistoryEntry entry = Mockito.mock(HistoryEntry.class);
        
        // Parse timestamp
        LocalDateTime dateTime = LocalDateTime.parse(timestamp, dateTimeFormatter);
        Instant instant = dateTime.toInstant(ZoneOffset.UTC);
        
        // Setup basic properties
        when(entry.getWorkItemId()).thenReturn(workItemId);
        when(entry.getType()).thenReturn(HistoryEntryType.STATUS_CHANGE);
        when(entry.getFromValue()).thenReturn(fromState);
        when(entry.getToValue()).thenReturn(toState);
        when(entry.getUser()).thenReturn(user);
        when(entry.getTimestamp()).thenReturn(instant);
        when(entry.getComment()).thenReturn(comment);
        
        return entry;
    }
    
    private void setupDefaultWorkflowTransitions(MockWorkflowService mockWorkflowService) {
        // Setup allowed transitions for each workflow state
        Map<WorkflowState, List<WorkflowState>> allowedTransitions = new HashMap<>();
        
        allowedTransitions.put(WorkflowState.CREATED, Arrays.asList(WorkflowState.READY));
        allowedTransitions.put(WorkflowState.READY, Arrays.asList(WorkflowState.IN_PROGRESS, WorkflowState.BLOCKED));
        allowedTransitions.put(WorkflowState.IN_PROGRESS, Arrays.asList(WorkflowState.BLOCKED, WorkflowState.DONE));
        allowedTransitions.put(WorkflowState.BLOCKED, Arrays.asList(WorkflowState.READY, WorkflowState.IN_PROGRESS));
        allowedTransitions.put(WorkflowState.DONE, Arrays.asList(WorkflowState.READY));
        
        // Mock the getAllowedTransitions method
        for (Map.Entry<WorkflowState, List<WorkflowState>> entry : allowedTransitions.entrySet()) {
            WorkflowState fromState = entry.getKey();
            List<WorkflowState> transitions = entry.getValue();
            
            when(mockWorkflowService.getAllowedTransitions(fromState)).thenReturn(transitions);
        }
        
        // Mock getAllWorkflowStates
        when(mockWorkflowService.getAllWorkflowStates()).thenReturn(Arrays.asList(
            WorkflowState.CREATED,
            WorkflowState.READY,
            WorkflowState.IN_PROGRESS,
            WorkflowState.BLOCKED,
            WorkflowState.DONE
        ));
        
        // Setup the isValidTransition method
        for (Map.Entry<WorkflowState, List<WorkflowState>> entry : allowedTransitions.entrySet()) {
            WorkflowState fromState = entry.getKey();
            List<WorkflowState> transitions = entry.getValue();
            
            for (WorkflowState toState : WorkflowState.values()) {
                // Using canTransition method instead of isValidTransition
                WorkItem mockItem = mock(WorkItem.class);
                when(mockItem.getState()).thenReturn(fromState);
                String mockItemId = "WI-" + fromState.toString();
                when(mockWorkflowService.getItem(mockItemId)).thenReturn(mockItem);
                when(mockWorkflowService.canTransition(mockItemId, toState))
                    .thenReturn(transitions.contains(toState));
            }
        }
    }
    
    private int findPatternPosition(String text, String pattern) {
        return text.indexOf(pattern);
    }
}