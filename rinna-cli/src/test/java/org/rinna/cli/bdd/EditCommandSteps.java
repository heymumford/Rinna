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
import io.cucumber.java.en.And;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.EditCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Step definitions for the Edit Command feature.
 */
public class EditCommandSteps {
    
    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private MockItemService mockItemService;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private ContextManager mockContextManager;
    
    private EditCommand editCommand;
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final java.io.InputStream originalIn = System.in;
    
    private MockedStatic<ServiceManager> serviceManagerMock;
    private MockedStatic<ContextManager> contextManagerMock;
    
    private int returnCode;
    private StringBuilder userInput = new StringBuilder();
    private UUID workItemId;
    private WorkItem testWorkItem;
    
    @Before
    public void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
        contextManagerMock = Mockito.mockStatic(ContextManager.class);
        
        // Set up mocks
        serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
        
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn("operation-id");
        when(mockMetadataService.trackOperation(anyString(), any())).thenReturn("field-operation-id");
        
        // Initialize command
        editCommand = new EditCommand(mockServiceManager);
    }
    
    @After
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
        
        serviceManagerMock.close();
        contextManagerMock.close();
        
        outContent.reset();
        errContent.reset();
        userInput.setLength(0);
    }
    
    @Given("a valid user session")
    public void aValidUserSession() {
        // Session setup already done in setUp()
    }
    
    @Given("an existing work item with ID {string}")
    public void anExistingWorkItemWithId(String id) {
        workItemId = UUID.randomUUID();
        testWorkItem = createTestWorkItem(workItemId, "Test Item", "Test description", 
                             Priority.MEDIUM, WorkflowState.IN_PROGRESS, "user@example.com");
        
        when(mockItemService.getItem(workItemId.toString())).thenReturn(testWorkItem);
    }
    
    @Given("an existing work item with ID {string} and title {string}")
    public void anExistingWorkItemWithIdAndTitle(String id, String title) {
        workItemId = UUID.randomUUID();
        testWorkItem = createTestWorkItem(workItemId, title, "Test description", 
                             Priority.MEDIUM, WorkflowState.IN_PROGRESS, "user@example.com");
        
        when(mockItemService.getItem(workItemId.toString())).thenReturn(testWorkItem);
        
        // Mock the update response
        WorkItem updatedItem = createTestWorkItem(workItemId, "Updated Title", "Test description", 
                                 Priority.MEDIUM, WorkflowState.IN_PROGRESS, "user@example.com");
        when(mockItemService.updateTitle(eq(workItemId), anyString(), anyString())).thenReturn(updatedItem);
    }
    
    @Given("an existing work item with ID {string} and description {string}")
    public void anExistingWorkItemWithIdAndDescription(String id, String description) {
        workItemId = UUID.randomUUID();
        testWorkItem = createTestWorkItem(workItemId, "Test Item", description, 
                             Priority.MEDIUM, WorkflowState.IN_PROGRESS, "user@example.com");
        
        when(mockItemService.getItem(workItemId.toString())).thenReturn(testWorkItem);
        
        // Mock the update response
        WorkItem updatedItem = createTestWorkItem(workItemId, "Test Item", "Updated description", 
                                 Priority.MEDIUM, WorkflowState.IN_PROGRESS, "user@example.com");
        when(mockItemService.updateDescription(eq(workItemId), anyString(), anyString())).thenReturn(updatedItem);
    }
    
    @Given("an existing work item with ID {string} and priority {string}")
    public void anExistingWorkItemWithIdAndPriority(String id, String priority) {
        workItemId = UUID.randomUUID();
        testWorkItem = createTestWorkItem(workItemId, "Test Item", "Test description", 
                             Priority.valueOf(priority), WorkflowState.IN_PROGRESS, "user@example.com");
        
        when(mockItemService.getItem(workItemId.toString())).thenReturn(testWorkItem);
        
        // Mock the update response for valid priority
        WorkItem updatedItem = createTestWorkItem(workItemId, "Test Item", "Test description", 
                                 Priority.HIGH, WorkflowState.IN_PROGRESS, "user@example.com");
        when(mockItemService.updatePriority(eq(workItemId), eq(Priority.HIGH), anyString())).thenReturn(updatedItem);
    }
    
    @Given("an existing work item with ID {string} and state {string}")
    public void anExistingWorkItemWithIdAndState(String id, String state) {
        workItemId = UUID.randomUUID();
        testWorkItem = createTestWorkItem(workItemId, "Test Item", "Test description", 
                             Priority.MEDIUM, WorkflowState.valueOf(state), "user@example.com");
        
        when(mockItemService.getItem(workItemId.toString())).thenReturn(testWorkItem);
        
        // Mock the update response for valid state
        WorkItem updatedItem = createTestWorkItem(workItemId, "Test Item", "Test description", 
                                 Priority.MEDIUM, WorkflowState.DONE, "user@example.com");
        when(mockItemService.updateState(eq(workItemId), eq(WorkflowState.DONE), anyString())).thenReturn(updatedItem);
    }
    
    @Given("an existing work item with ID {string} and assignee {string}")
    public void anExistingWorkItemWithIdAndAssignee(String id, String assignee) {
        workItemId = UUID.randomUUID();
        testWorkItem = createTestWorkItem(workItemId, "Test Item", "Test description", 
                             Priority.MEDIUM, WorkflowState.IN_PROGRESS, assignee);
        
        when(mockItemService.getItem(workItemId.toString())).thenReturn(testWorkItem);
        
        // Mock the update response
        WorkItem updatedItem = createTestWorkItem(workItemId, "Test Item", "Test description", 
                                 Priority.MEDIUM, WorkflowState.IN_PROGRESS, "user2@example.com");
        when(mockItemService.assignTo(eq(workItemId), eq("user2@example.com"), anyString())).thenReturn(updatedItem);
    }
    
    @Given("I previously viewed work item with ID {string}")
    public void iPreviouslyViewedWorkItemWithId(String id) {
        workItemId = UUID.randomUUID();
        testWorkItem = createTestWorkItem(workItemId, "Test Item", "Test description", 
                             Priority.MEDIUM, WorkflowState.IN_PROGRESS, "user@example.com");
        
        when(mockContextManager.getLastViewedWorkItem()).thenReturn(workItemId);
        when(mockItemService.getItem(workItemId.toString())).thenReturn(testWorkItem);
    }
    
    @When("I execute the command {string}")
    public void iExecuteTheCommand(String command) {
        editCommand = new EditCommand(mockServiceManager);
        
        String[] args = command.split("\\s+");
        if (args.length > 1) {
            String arg = args[1];
            if (arg.startsWith("id=")) {
                editCommand.setIdParameter(arg);
                workItemId = UUID.fromString(arg.substring(3));
            } else if (arg.equals("--json")) {
                editCommand.setFormat("json");
                if (args.length > 2 && args[2].startsWith("id=")) {
                    editCommand.setIdParameter(args[2]);
                    workItemId = UUID.fromString(args[2].substring(3));
                }
            } else {
                editCommand.setItemId(arg);
                workItemId = UUID.fromString(arg);
            }
        }
        
        // Set input for the next step
        if (\!userInput.toString().isEmpty()) {
            setUserInput(userInput.toString());
        } else {
            // Default to cancel to avoid hanging
            setUserInput("0\n");
        }
        
        returnCode = editCommand.call();
    }
    
    @When("there is no previously viewed work item")
    public void thereIsNoPreviouslyViewedWorkItem() {
        when(mockContextManager.getLastViewedWorkItem()).thenReturn(null);
    }
    
    @When("I select field {string} for title")
    public void iSelectFieldForTitle(String fieldNumber) {
        userInput.append(fieldNumber).append("\n");
    }
    
    @When("I select field {string} for description")
    public void iSelectFieldForDescription(String fieldNumber) {
        userInput.append(fieldNumber).append("\n");
    }
    
    @When("I select field {string} for priority")
    public void iSelectFieldForPriority(String fieldNumber) {
        userInput.append(fieldNumber).append("\n");
    }
    
    @When("I select field {string} for state")
    public void iSelectFieldForState(String fieldNumber) {
        userInput.append(fieldNumber).append("\n");
    }
    
    @When("I select field {string} for assignee")
    public void iSelectFieldForAssignee(String fieldNumber) {
        userInput.append(fieldNumber).append("\n");
    }
    
    @When("I select field {string} to cancel")
    public void iSelectFieldToCancel(String fieldNumber) {
        userInput.append(fieldNumber).append("\n");
    }
    
    @When("I enter a new value {string}")
    public void iEnterANewValue(String value) {
        userInput.append(value).append("\n");
        setUserInput(userInput.toString());
    }
    
    @When("I enter an invalid value {string}")
    public void iEnterAnInvalidValue(String value) {
        userInput.append(value).append("\n");
        setUserInput(userInput.toString());
    }
    
    @Then("I should see the work item details")
    public void iShouldSeeTheWorkItemDetails() {
        String output = outContent.toString();
        assertTrue(output.contains("Work Item:"), "Should display work item header");
        assertTrue(output.contains("[1] Title:"), "Should display title field");
        assertTrue(output.contains("[2] Description:"), "Should display description field");
        assertTrue(output.contains("[3] Priority:"), "Should display priority field");
        assertTrue(output.contains("[4] State:"), "Should display state field");
        assertTrue(output.contains("[5] Assignee:"), "Should display assignee field");
    }
    
    @Then("I should see the work item details in JSON format")
    public void iShouldSeeTheWorkItemDetailsInJsonFormat() {
        String output = outContent.toString();
        assertTrue(output.contains("\"id\":"), "Should display id in JSON");
        assertTrue(output.contains("\"title\":"), "Should display title in JSON");
        assertTrue(output.contains("\"description\":"), "Should display description in JSON");
        assertTrue(output.contains("\"priority\":"), "Should display priority in JSON");
        assertTrue(output.contains("\"state\":"), "Should display state in JSON");
        assertTrue(output.contains("\"assignee\":"), "Should display assignee in JSON");
    }
    
    @Then("I should see edit options in JSON format")
    public void iShouldSeeEditOptionsInJsonFormat() {
        String output = outContent.toString();
        assertTrue(output.contains("\"actions\":"), "Should display actions in JSON");
        assertTrue(output.contains("\"Update Title\""), "Should display title action in JSON");
        assertTrue(output.contains("\"Update Description\""), "Should display description action in JSON");
        assertTrue(output.contains("\"Update Priority\""), "Should display priority action in JSON");
        assertTrue(output.contains("\"Update State\""), "Should display state action in JSON");
        assertTrue(output.contains("\"Update Assignee\""), "Should display assignee action in JSON");
        assertTrue(output.contains("\"Cancel\""), "Should display cancel action in JSON");
    }
    
    @Then("I should see the following fields that can be edited")
    public void iShouldSeeTheFollowingFieldsThatCanBeEdited(List<String> fields) {
        String output = outContent.toString();
        for (String field : fields) {
            assertTrue(output.contains(field), "Should display " + field + " field");
        }
    }
    
    @Then("I should see a prompt for selecting a field to edit")
    public void iShouldSeeAPromptForSelectingAFieldToEdit() {
        String output = outContent.toString();
        assertTrue(output.contains("Enter the number of the field to update"), 
                  "Should display prompt for field selection");
    }
    
    @Then("the work item title should be updated")
    public void theWorkItemTitleShouldBeUpdated() {
        verify(mockItemService).updateTitle(eq(workItemId), eq("Updated Title"), anyString());
    }
    
    @Then("I should see a success message for title update")
    public void iShouldSeeASuccessMessageForTitleUpdate() {
        String output = outContent.toString();
        assertTrue(output.contains("Title updated successfully"), 
                  "Should display success message for title update");
    }
    
    @Then("the work item description should be updated")
    public void theWorkItemDescriptionShouldBeUpdated() {
        verify(mockItemService).updateDescription(eq(workItemId), eq("Updated description with more details"), anyString());
    }
    
    @Then("I should see a success message for description update")
    public void iShouldSeeASuccessMessageForDescriptionUpdate() {
        String output = outContent.toString();
        assertTrue(output.contains("Description updated successfully"), 
                  "Should display success message for description update");
    }
    
    @Then("the work item priority should be updated to {string}")
    public void theWorkItemPriorityShouldBeUpdatedTo(String priority) {
        verify(mockItemService).updatePriority(eq(workItemId), eq(Priority.valueOf(priority)), anyString());
    }
    
    @Then("I should see a success message for priority update")
    public void iShouldSeeASuccessMessageForPriorityUpdate() {
        String output = outContent.toString();
        assertTrue(output.contains("Priority updated successfully"), 
                  "Should display success message for priority update");
    }
    
    @Then("the work item state should be updated to {string}")
    public void theWorkItemStateShouldBeUpdatedTo(String state) {
        verify(mockItemService).updateState(eq(workItemId), eq(WorkflowState.valueOf(state)), anyString());
    }
    
    @Then("I should see a success message for state update")
    public void iShouldSeeASuccessMessageForStateUpdate() {
        String output = outContent.toString();
        assertTrue(output.contains("State updated successfully"), 
                  "Should display success message for state update");
    }
    
    @Then("the work item assignee should be updated to {string}")
    public void theWorkItemAssigneeShouldBeUpdatedTo(String assignee) {
        verify(mockItemService).assignTo(eq(workItemId), eq(assignee), anyString());
    }
    
    @Then("I should see a success message for assignee update")
    public void iShouldSeeASuccessMessageForAssigneeUpdate() {
        String output = outContent.toString();
        assertTrue(output.contains("Assignee updated successfully"), 
                  "Should display success message for assignee update");
    }
    
    @Then("the edit should be cancelled")
    public void theEditShouldBeCancelled() {
        // Verify no update methods were called
        verify(mockItemService, never()).updateTitle(any(UUID.class), anyString(), anyString());
        verify(mockItemService, never()).updateDescription(any(UUID.class), anyString(), anyString());
        verify(mockItemService, never()).updatePriority(any(UUID.class), any(Priority.class), anyString());
        verify(mockItemService, never()).updateState(any(UUID.class), any(WorkflowState.class), anyString());
        verify(mockItemService, never()).assignTo(any(UUID.class), anyString(), anyString());
    }
    
    @Then("I should see a cancellation message")
    public void iShouldSeeACancellationMessage() {
        String output = outContent.toString();
        assertTrue(output.contains("Update cancelled"), 
                  "Should display cancellation message");
    }
    
    @Then("I should see the work item details for {string}")
    public void iShouldSeeTheWorkItemDetailsFor(String id) {
        String output = outContent.toString();
        assertTrue(output.contains("Work Item: " + workItemId), 
                  "Should display work item ID");
    }
    
    @Then("I should see an error message about invalid priority")
    public void iShouldSeeAnErrorMessageAboutInvalidPriority() {
        String error = errContent.toString();
        assertTrue(error.contains("Error: Invalid priority value"), 
                  "Should display invalid priority error");
    }
    
    @Then("the error message should list valid priority values")
    public void theErrorMessageShouldListValidPriorityValues() {
        String error = errContent.toString();
        for (Priority priority : Priority.values()) {
            assertTrue(error.contains(priority.name()), 
                      "Should list " + priority.name() + " as valid priority");
        }
    }
    
    @Then("I should see an error message about invalid state")
    public void iShouldSeeAnErrorMessageAboutInvalidState() {
        String error = errContent.toString();
        assertTrue(error.contains("Error: Invalid state value"), 
                  "Should display invalid state error");
    }
    
    @Then("the error message should list valid state values")
    public void theErrorMessageShouldListValidStateValues() {
        String error = errContent.toString();
        for (WorkflowState state : WorkflowState.values()) {
            assertTrue(error.contains(state.name()), 
                      "Should list " + state.name() + " as valid state");
        }
    }
    
    @Then("I should see an error message about item not found")
    public void iShouldSeeAnErrorMessageAboutItemNotFound() {
        String error = errContent.toString();
        assertTrue(error.contains("Error: Work item not found"), 
                  "Should display item not found error");
    }
    
    @Then("I should see an error message about invalid ID format")
    public void iShouldSeeAnErrorMessageAboutInvalidIdFormat() {
        String error = errContent.toString();
        assertTrue(error.contains("Error: Invalid") && error.contains("format"), 
                  "Should display invalid ID format error");
    }
    
    @Then("I should see an error message about no context available")
    public void iShouldSeeAnErrorMessageAboutNoContextAvailable() {
        String error = errContent.toString();
        assertTrue(error.contains("Error: No work item context available"), 
                  "Should display no context available error");
    }
    
    @Then("the error message should provide guidance on how to specify an ID")
    public void theErrorMessageShouldProvideGuidanceOnHowToSpecifyAnId() {
        String error = errContent.toString();
        assertTrue(error.contains("Please specify an ID with id=X"), 
                  "Should provide guidance on how to specify an ID");
    }
    
    @Then("the command should track this operation with MetadataService")
    public void theCommandShouldTrackThisOperationWithMetadataService() {
        verify(mockMetadataService).startOperation(eq("edit"), eq("UPDATE"), any(Map.class));
    }
    
    @Then("the command should track this operation failure with MetadataService")
    public void theCommandShouldTrackThisOperationFailureWithMetadataService() {
        verify(mockMetadataService).failOperation(anyString(), any(Throwable.class));
    }
    
    @Then("the command should track a field-level operation for {string}")
    public void theCommandShouldTrackFieldLevelOperationFor(String fieldName) {
        verify(mockMetadataService).trackOperation(eq("edit-field"), any(Map.class));
        
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockMetadataService).trackOperation(anyString(), captor.capture());
        
        Map<String, Object> params = captor.getValue();
        assertEquals(fieldName, params.get("field"));
    }
    
    @Then("the command should track a field-level operation failure for {string}")
    public void theCommandShouldTrackFieldLevelOperationFailureFor(String fieldName) {
        verify(mockMetadataService).trackOperation(eq("edit-field"), any(Map.class));
        verify(mockMetadataService).failOperation(eq("field-operation-id"), any(Throwable.class));
        
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockMetadataService).trackOperation(anyString(), captor.capture());
        
        Map<String, Object> params = captor.getValue();
        assertEquals(fieldName, params.get("field"));
    }
    
    @Then("no field updates should be tracked")
    public void noFieldUpdatesShouldBeTracked() {
        verify(mockMetadataService, never()).trackOperation(eq("edit-field"), any(Map.class));
    }
    
    @Then("the tracking parameters should include {string} as {string}")
    public void theTrackingParametersShouldIncludeAs(String paramName, String paramValue) {
        ArgumentCaptor<Map<String, Object>> captor = ArgumentCaptor.forClass(Map.class);
        verify(mockMetadataService).startOperation(anyString(), anyString(), captor.capture());
        
        Map<String, Object> params = captor.getValue();
        assertEquals(paramValue, params.get(paramName).toString());
    }
    
    private void setUserInput(String input) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);
    }
    
    private WorkItem createTestWorkItem(UUID id, String title, String description, 
                                      Priority priority, WorkflowState state, String assignee) {
        WorkItem item = new WorkItem();
        item.setId(id.toString());
        item.setTitle(title);
        item.setDescription(description);
        item.setPriority(priority);
        item.setStatus(state);
        item.setAssignee(assignee);
        item.setType(WorkItemType.TASK);
        return item;
    }
}
