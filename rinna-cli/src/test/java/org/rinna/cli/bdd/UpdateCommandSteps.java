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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.UpdateCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.InvalidTransitionException;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.ModelMapper;
import org.rinna.cli.util.OutputFormatter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Step definitions for the Update Command BDD tests.
 */
public class UpdateCommandSteps {

    private TestContext testContext;
    private UpdateCommand updateCommand;
    
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    private String currentUser;
    private UUID testItemId;
    private WorkItem testWorkItem;
    private WorkItem updatedWorkItem;
    private List<String> availableTransitions;
    
    private int commandResult;
    
    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private MockItemService mockItemService;
    
    @Mock
    private MockWorkflowService mockWorkflowService;
    
    @Mock
    private ConfigurationService mockConfigService;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private ContextManager mockContextManager;
    
    private MockedStatic<ServiceManager> mockedStaticServiceManager;
    private MockedStatic<ContextManager> mockedStaticContextManager;
    private MockedStatic<ModelMapper> mockedStaticModelMapper;
    private MockedStatic<OutputFormatter> mockedStaticOutputFormatter;
    
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    private ArgumentCaptor<Map<String, Object>> resultCaptor;
    
    public UpdateCommandSteps(TestContext testContext) {
        this.testContext = testContext;
    }
    
    @Before
    public void setUp() {
        // Initialize mocks
        mockServiceManager = Mockito.mock(ServiceManager.class);
        mockItemService = Mockito.mock(MockItemService.class);
        mockWorkflowService = Mockito.mock(MockWorkflowService.class);
        mockConfigService = Mockito.mock(ConfigurationService.class);
        mockMetadataService = Mockito.mock(MetadataService.class);
        mockContextManager = Mockito.mock(ContextManager.class);
        
        // Set up argument captors
        paramsCaptor = ArgumentCaptor.forClass(Map.class);
        resultCaptor = ArgumentCaptor.forClass(Map.class);
        
        // Set up static mocks
        mockedStaticServiceManager = Mockito.mockStatic(ServiceManager.class);
        mockedStaticContextManager = Mockito.mockStatic(ContextManager.class);
        mockedStaticModelMapper = Mockito.mockStatic(ModelMapper.class);
        mockedStaticOutputFormatter = Mockito.mockStatic(OutputFormatter.class);
        
        mockedStaticServiceManager.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        mockedStaticContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
        
        // Mock OutputFormatter for JSON testing
        mockedStaticOutputFormatter.when(() -> OutputFormatter.toJson(any(Map.class), anyBoolean()))
            .thenAnswer(invocation -> {
                Map<String, Object> result = invocation.getArgument(0);
                
                StringBuilder json = new StringBuilder();
                json.append("{\n");
                json.append("  \"id\": \"").append(result.get("id")).append("\",\n");
                
                // Add updated fields
                if (result.containsKey("updatedFields")) {
                    json.append("  \"updatedFields\": {\n");
                    
                    Map<String, Object> fields = (Map<String, Object>) result.get("updatedFields");
                    int count = 0;
                    for (Map.Entry<String, Object> entry : fields.entrySet()) {
                        json.append("    \"").append(entry.getKey()).append("\": ");
                        
                        if (entry.getValue() instanceof String) {
                            json.append("\"").append(entry.getValue()).append("\"");
                        } else {
                            json.append(entry.getValue());
                        }
                        
                        if (++count < fields.size()) {
                            json.append(",");
                        }
                        json.append("\n");
                    }
                    
                    json.append("  },\n");
                }
                
                // Add work item details
                if (result.containsKey("workItem")) {
                    json.append("  \"workItem\": {...}\n");
                }
                
                json.append("}");
                return json.toString();
            });
            
        // Mock ModelMapper for domain conversion
        mockedStaticModelMapper.when(() -> ModelMapper.toDomainWorkItem(any(WorkItem.class)))
            .thenReturn(new org.rinna.cli.domain.model.DomainWorkItem());
        
        // Set up the service manager mock
        when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up the metadata service mock
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn("operation-id");
        
        // Set up the config service mock
        when(mockConfigService.getCurrentUser()).thenReturn("testuser");
        
        // Redirect stdout and stderr for verification
        originalOut = System.out;
        originalErr = System.err;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Initialize test data
        currentUser = "testuser";
        testItemId = UUID.fromString("00000000-0000-0000-0000-000000000123");
        testWorkItem = createInitialWorkItem();
        updatedWorkItem = createInitialWorkItem(); // Clone to be modified during tests
        availableTransitions = new ArrayList<>(Arrays.asList("READY", "IN_PROGRESS"));
        
        // Create a new UpdateCommand instance
        updateCommand = new UpdateCommand(mockServiceManager);
    }
    
    @After
    public void tearDown() {
        // Restore original stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Close static mocks
        if (mockedStaticServiceManager != null) {
            mockedStaticServiceManager.close();
        }
        if (mockedStaticContextManager != null) {
            mockedStaticContextManager.close();
        }
        if (mockedStaticModelMapper != null) {
            mockedStaticModelMapper.close();
        }
        if (mockedStaticOutputFormatter != null) {
            mockedStaticOutputFormatter.close();
        }
    }
    
    private WorkItem createInitialWorkItem() {
        WorkItem item = new WorkItem();
        item.setId(testItemId.toString());
        item.setTitle("Original title");
        item.setDescription("Original description");
        item.setType(WorkItemType.TASK);
        item.setPriority(Priority.MEDIUM);
        item.setStatus(WorkflowState.CREATED);
        item.setAssignee("alice");
        item.setReporter("bob");
        item.setProject("TestProject");
        item.setCreated(LocalDateTime.now().minusDays(5));
        item.setUpdated(LocalDateTime.now().minusHours(12));
        
        return item;
    }
    
    @Given("a user {string} with basic permissions")
    public void aUserWithBasicPermissions(String username) {
        currentUser = username;
        when(mockConfigService.getCurrentUser()).thenReturn(username);
    }
    
    @Given("a work item {string} with title {string} exists")
    public void aWorkItemWithTitleExists(String itemId, String title) {
        // Extract numeric part from WI-123 format or use generated UUID
        if (itemId.startsWith("WI-")) {
            String numericPart = itemId.split("-")[1];
            testItemId = new UUID(0, Long.parseLong(numericPart));
        } else {
            testItemId = UUID.randomUUID();
        }
        
        testWorkItem.setId(testItemId.toString());
        testWorkItem.setTitle(title);
        updatedWorkItem.setId(testItemId.toString());
        updatedWorkItem.setTitle(title);
        
        // Set up mock behavior
        when(mockItemService.getItem(testItemId.toString())).thenReturn(testWorkItem);
        when(mockItemService.updateItem(any(WorkItem.class))).thenReturn(updatedWorkItem);
    }
    
    @Given("the work item has status {string}")
    public void theWorkItemHasStatus(String status) {
        WorkflowState state = WorkflowState.valueOf(status);
        testWorkItem.setStatus(state);
        updatedWorkItem.setStatus(state);
        
        // Update the mock to return the work item with the specified status
        when(mockItemService.getItem(testItemId.toString())).thenReturn(testWorkItem);
    }
    
    @Given("the transition to {string} is valid")
    public void theTransitionToIsValid(String targetState) {
        when(mockWorkflowService.canTransition(testItemId.toString(), WorkflowState.valueOf(targetState))).thenReturn(true);
        
        // Set up mock to return updated work item with new state after transition
        WorkItem transitionedItem = new WorkItem();
        transitionedItem.setId(testItemId.toString());
        transitionedItem.setTitle(testWorkItem.getTitle());
        transitionedItem.setDescription(testWorkItem.getDescription());
        transitionedItem.setType(testWorkItem.getType());
        transitionedItem.setPriority(testWorkItem.getPriority());
        transitionedItem.setStatus(WorkflowState.valueOf(targetState));
        transitionedItem.setAssignee(testWorkItem.getAssignee());
        
        when(mockWorkflowService.transition(eq(testItemId.toString()), eq(WorkflowState.valueOf(targetState))))
            .thenReturn(transitionedItem);
        
        when(mockWorkflowService.transition(eq(testItemId.toString()), anyString(), eq(WorkflowState.valueOf(targetState)), anyString()))
            .thenReturn(transitionedItem);
        
        updatedWorkItem.setStatus(WorkflowState.valueOf(targetState));
    }
    
    @Given("the transition to {string} is invalid")
    public void theTransitionToIsInvalid(String targetState) {
        when(mockWorkflowService.canTransition(testItemId.toString(), WorkflowState.valueOf(targetState))).thenReturn(false);
        when(mockWorkflowService.getAvailableTransitions(testItemId.toString())).thenReturn(availableTransitions);
        
        // Set up mock to throw exception when illegal transition is attempted
        doThrow(new InvalidTransitionException("Invalid transition to " + targetState))
            .when(mockWorkflowService).transition(eq(testItemId.toString()), eq(WorkflowState.valueOf(targetState)));
    }
    
    @When("the user runs {string}")
    public void theUserRunsCommand(String command) {
        // Parse and execute the command
        parseAndExecuteCommand(command);
    }
    
    @When("the user runs {string} without an item ID")
    public void theUserRunsCommandWithoutAnItemId(String command) {
        // Run update command without setting an item ID
        updateCommand = new UpdateCommand(mockServiceManager);
        updateCommand.setTitle("Test title"); // Set at least one field to update
        commandResult = updateCommand.call();
    }
    
    @When("the user runs {string} without specifying any fields")
    public void theUserRunsCommandWithoutSpecifyingAnyFields(String command) {
        // Run update command with ID but without any fields to update
        updateCommand = new UpdateCommand(mockServiceManager);
        updateCommand.setId("WI-123");
        commandResult = updateCommand.call();
    }
    
    private void parseAndExecuteCommand(String command) {
        // Reset updateCommand to ensure clean state
        updateCommand = new UpdateCommand(mockServiceManager);
        
        // Extract item ID if present
        if (command.contains("update ")) {
            String[] parts = command.split(" ");
            if (parts.length > 1 && !parts[1].startsWith("--")) {
                String itemId = parts[1];
                updateCommand.setId(itemId);
                
                // Set up UUID handling based on item ID
                if (itemId.equals("invalid-id")) {
                    when(mockItemService.getItem(itemId))
                        .thenThrow(new IllegalArgumentException("Invalid UUID format"));
                } else if (itemId.equals("non-existent-id")) {
                    when(mockItemService.getItem(itemId)).thenReturn(null);
                }
            }
        }
        
        // Parse title parameter
        if (command.contains("--title=")) {
            String title = extractParameterValue(command, "--title=");
            updateCommand.setTitle(title);
            updatedWorkItem.setTitle(title);
        }
        
        // Parse description parameter
        if (command.contains("--description=")) {
            String description = extractParameterValue(command, "--description=");
            updateCommand.setDescription(description);
            updatedWorkItem.setDescription(description);
        }
        
        // Parse type parameter
        if (command.contains("--type=")) {
            String typeStr = extractParameterValue(command, "--type=").toUpperCase();
            WorkItemType type = WorkItemType.valueOf(typeStr);
            updateCommand.setType(type);
            updatedWorkItem.setType(type);
        }
        
        // Parse priority parameter
        if (command.contains("--priority=")) {
            String priorityStr = extractParameterValue(command, "--priority=").toUpperCase();
            Priority priority = Priority.valueOf(priorityStr);
            updateCommand.setPriority(priority);
            updatedWorkItem.setPriority(priority);
        }
        
        // Parse status/state parameter
        if (command.contains("--status=")) {
            String statusStr = extractParameterValue(command, "--status=").toUpperCase();
            WorkflowState status = WorkflowState.valueOf(statusStr);
            updateCommand.setStatus(status);
            // Don't update the updatedWorkItem status here - it's handled by the test setup
        }
        
        // Parse assignee parameter
        if (command.contains("--assignee=")) {
            String assignee = extractParameterValue(command, "--assignee=");
            updateCommand.setAssignee(assignee);
            updatedWorkItem.setAssignee(assignee);
        }
        
        // Parse comment parameter
        if (command.contains("--comment=")) {
            String comment = extractParameterValue(command, "--comment=");
            updateCommand.setComment(comment);
        }
        
        // Parse format parameter
        if (command.contains("--format=")) {
            String format = extractParameterValue(command, "--format=");
            updateCommand.setFormat(format);
        } else if (command.contains("--json")) {
            updateCommand.setJsonOutput(true);
        }
        
        // Parse verbose flag
        if (command.contains("--verbose")) {
            updateCommand.setVerbose(true);
        }
        
        // Execute the command
        commandResult = updateCommand.call();
    }
    
    private String extractParameterValue(String command, String paramPrefix) {
        // Handle quotes
        int startIndex;
        int endIndex;
        
        if (command.contains(paramPrefix + "'")) {
            // Single quotes
            startIndex = command.indexOf(paramPrefix + "'") + paramPrefix.length() + 1;
            endIndex = command.indexOf("'", startIndex);
        } else if (command.contains(paramPrefix + "\"")) {
            // Double quotes
            startIndex = command.indexOf(paramPrefix + "\"") + paramPrefix.length() + 1;
            endIndex = command.indexOf("\"", startIndex);
        } else {
            // No quotes
            startIndex = command.indexOf(paramPrefix) + paramPrefix.length();
            endIndex = command.indexOf(" ", startIndex);
            if (endIndex == -1) {
                endIndex = command.length();
            }
        }
        
        return command.substring(startIndex, endIndex);
    }
    
    @Then("the command should succeed")
    public void theCommandShouldSucceed() {
        assertEquals("Command should succeed with exit code 0", 0, commandResult);
    }
    
    @Then("the command should fail")
    public void theCommandShouldFail() {
        assertEquals("Command should fail with non-zero exit code", 1, commandResult);
    }
    
    @Then("the output should confirm the work item was updated")
    public void theOutputShouldConfirmTheWorkItemWasUpdated() {
        String output = outContent.toString();
        assertTrue("Output should confirm the work item was updated", 
                output.contains("Updated work item: " + testItemId.toString()));
    }
    
    @Then("the output should show the updated title")
    public void theOutputShouldShowTheUpdatedTitle() {
        String output = outContent.toString();
        assertTrue("Output should show the updated title", 
                output.contains("Title: " + updatedWorkItem.getTitle()));
    }
    
    @Then("the command should track operation details")
    public void theCommandShouldTrackOperationDetails() {
        verify(mockMetadataService).startOperation(eq("update"), eq("UPDATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify params contain expected details
        assertTrue("Parameters should contain id", params.containsKey("id"));
        
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        Map<String, Object> result = resultCaptor.getValue();
        
        // Verify result contains expected details
        assertTrue("Result should contain itemId", result.containsKey("itemId"));
        assertTrue("Result should contain updatedFields", result.containsKey("updatedFields"));
    }
    
    @Then("the command should track the updated field")
    public void theCommandShouldTrackTheUpdatedField() {
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        Map<String, Object> result = resultCaptor.getValue();
        
        assertTrue("Result should contain updatedFields", result.containsKey("updatedFields"));
        List<String> updatedFields = (List<String>) result.get("updatedFields");
        assertTrue("updatedFields should not be empty", !updatedFields.isEmpty());
    }
    
    @Then("the output should show the updated type")
    public void theOutputShouldShowTheUpdatedType() {
        String output = outContent.toString();
        assertTrue("Output should show the updated type", 
                output.contains("Type: " + updatedWorkItem.getType()));
    }
    
    @Then("the output should show the updated priority")
    public void theOutputShouldShowTheUpdatedPriority() {
        String output = outContent.toString();
        assertTrue("Output should show the updated priority", 
                output.contains("Priority: " + updatedWorkItem.getPriority()));
    }
    
    @Then("the output should show the updated assignee")
    public void theOutputShouldShowTheUpdatedAssignee() {
        String output = outContent.toString();
        assertTrue("Output should show the updated assignee", 
                output.contains("Assignee: " + updatedWorkItem.getAssignee()));
    }
    
    @Then("the output should show the updated status")
    public void theOutputShouldShowTheUpdatedStatus() {
        String output = outContent.toString();
        assertTrue("Output should show the updated status", 
                output.contains("Status: " + updatedWorkItem.getStatus()));
    }
    
    @Then("the command should track the state change")
    public void theCommandShouldTrackTheStateChange() {
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        Map<String, Object> result = resultCaptor.getValue();
        
        assertTrue("Result should contain stateChanged flag", result.containsKey("stateChanged"));
        assertTrue("Result should indicate state was changed", (Boolean) result.get("stateChanged"));
        assertTrue("Result should contain newState value", result.containsKey("newState"));
    }
    
    @Then("the command should track both the state change and comment")
    public void theCommandShouldTrackBothTheStateChangeAndComment() {
        verify(mockMetadataService).startOperation(eq("update"), eq("UPDATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify params contain both status and comment
        assertTrue("Parameters should contain status", params.containsKey("status"));
        assertTrue("Parameters should contain comment", params.containsKey("comment"));
        
        // Verify result contains state change details
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        Map<String, Object> result = resultCaptor.getValue();
        
        assertTrue("Result should contain stateChanged flag", result.containsKey("stateChanged"));
        assertTrue("Result should indicate state was changed", (Boolean) result.get("stateChanged"));
    }
    
    @Then("the output should show all updated fields")
    public void theOutputShouldShowAllUpdatedFields() {
        String output = outContent.toString();
        
        if (updateCommand.getTitle() != null) {
            assertTrue("Output should show updated title", 
                    output.contains("Title: " + updatedWorkItem.getTitle()));
        }
        
        if (updateCommand.getPriority() != null) {
            assertTrue("Output should show updated priority", 
                    output.contains("Priority: " + updatedWorkItem.getPriority()));
        }
        
        if (updateCommand.getAssignee() != null) {
            assertTrue("Output should show updated assignee", 
                    output.contains("Assignee: " + updatedWorkItem.getAssignee()));
        }
    }
    
    @Then("the command should track all updated fields")
    public void theCommandShouldTrackAllUpdatedFields() {
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        Map<String, Object> result = resultCaptor.getValue();
        
        assertTrue("Result should contain updatedFields", result.containsKey("updatedFields"));
        List<String> updatedFields = (List<String>) result.get("updatedFields");
        
        // Check if all fields that were set in the command are included in the result
        if (updateCommand.getTitle() != null) {
            assertTrue("updatedFields should include title", updatedFields.contains("title"));
        }
        
        if (updateCommand.getPriority() != null) {
            assertTrue("updatedFields should include priority", updatedFields.contains("priority"));
        }
        
        if (updateCommand.getAssignee() != null) {
            assertTrue("updatedFields should include assignee", updatedFields.contains("assignee"));
        }
    }
    
    @Then("the output should be valid JSON")
    public void theOutputShouldBeValidJson() {
        String output = outContent.toString();
        
        // Basic JSON format validation
        assertTrue("Output should start with {", output.trim().startsWith("{"));
        assertTrue("Output should end with }", output.trim().endsWith("}"));
        assertTrue("Output should contain field-value pairs with quotes", output.contains("\"id\":"));
    }
    
    @Then("the JSON should include the updated fields")
    public void theJsonShouldIncludeTheUpdatedFields() {
        String output = outContent.toString();
        
        // Verify that the JSON output includes updatedFields section
        assertTrue("JSON should include updatedFields section", output.contains("\"updatedFields\":"));
        
        // Check for fields that were updated
        if (updateCommand.getTitle() != null) {
            assertTrue("JSON should include updated title", output.contains("\"title\":"));
        }
        
        if (updateCommand.getPriority() != null) {
            assertTrue("JSON should include updated priority", output.contains("\"priority\":"));
        }
        
        if (updateCommand.getStatus() != null) {
            assertTrue("JSON should include updated status", output.contains("\"status\":"));
        }
    }
    
    @Then("the command should track format parameters")
    public void theCommandShouldTrackFormatParameters() {
        verify(mockMetadataService).startOperation(eq("update"), eq("UPDATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify format is tracked
        assertEquals("Parameter format should be json", "json", params.get("format"));
    }
    
    @Then("the output should include additional work item details")
    public void theOutputShouldIncludeAdditionalWorkItemDetails() {
        String output = outContent.toString();
        
        if (updateCommand.getDescription() != null) {
            assertTrue("Output should include description", output.contains("Description:"));
            assertTrue("Output should include the description text", 
                    output.contains(updatedWorkItem.getDescription()));
        }
        
        // Check for additional fields in verbose output
        assertTrue("Output should include reporter information", 
                output.contains("Reporter: " + updatedWorkItem.getReporter()));
        assertTrue("Output should include created date information", output.contains("Created:"));
        assertTrue("Output should include updated date information", output.contains("Updated:"));
    }
    
    @Then("the output should include operation tracking information")
    public void theOutputShouldIncludeOperationTrackingInformation() {
        String output = outContent.toString();
        assertTrue("Output should include operation ID", output.contains("Operation ID:"));
    }
    
    @Then("the command should track the verbose parameter")
    public void theCommandShouldTrackTheVerboseParameter() {
        verify(mockMetadataService).startOperation(eq("update"), eq("UPDATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify verbose flag is tracked
        assertEquals("Parameter verbose should be true", true, params.get("verbose"));
    }
    
    @Then("the error output should indicate item ID is required")
    public void theErrorOutputShouldIndicateItemIdIsRequired() {
        String errorOutput = errContent.toString();
        assertTrue("Error output should indicate item ID is required", 
                errorOutput.contains("Work item ID is required"));
    }
    
    @Then("the error output should indicate invalid item ID format")
    public void theErrorOutputShouldIndicateInvalidItemIdFormat() {
        String errorOutput = errContent.toString();
        assertTrue("Error output should indicate invalid item ID format", 
                errorOutput.contains("Invalid work item ID format"));
    }
    
    @Then("the error output should indicate work item not found")
    public void theErrorOutputShouldIndicateWorkItemNotFound() {
        String errorOutput = errContent.toString();
        assertTrue("Error output should indicate work item not found", 
                errorOutput.contains("Work item not found"));
    }
    
    @Then("the error output should indicate no updates specified")
    public void theErrorOutputShouldIndicateNoUpdatesSpecified() {
        String errorOutput = errContent.toString();
        assertTrue("Error output should indicate no updates specified", 
                errorOutput.contains("No updates specified"));
    }
    
    @Then("the error output should indicate invalid transition")
    public void theErrorOutputShouldIndicateInvalidTransition() {
        String errorOutput = errContent.toString();
        assertTrue("Error output should indicate invalid transition", 
                errorOutput.contains("Cannot transition work item"));
    }
    
    @Then("the error output should include available transitions")
    public void theErrorOutputShouldIncludeAvailableTransitions() {
        String errorOutput = errContent.toString();
        
        // Check for the presence of available transitions in the error message
        for (String transition : availableTransitions) {
            assertTrue("Error output should include available transition: " + transition, 
                    errorOutput.contains(transition));
        }
    }
    
    @Then("the command should track operation failure")
    public void theCommandShouldTrackOperationFailure() {
        verify(mockMetadataService).failOperation(anyString(), any(Exception.class));
    }
}