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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

import org.json.JSONObject;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.DoneCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.InvalidTransitionException;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for the DoneCommand Cucumber tests.
 */
public class DoneCommandSteps {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private DoneCommand doneCommand;
    private int exitCode;

    @Mock
    private ServiceManager mockServiceManager;
    
    private MockWorkflowService mockWorkflowService;
    
    @Mock
    private ConfigurationService mockConfigService;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private ContextManager mockContextManager;
    
    private Map<String, Object> capturedOperationParams;
    private Object capturedOperationResult;
    private String operationId = "test-operation-id";
    private ArgumentCaptor<Map<String, Object>> operationParamsCaptor;
    private ArgumentCaptor<Object> operationResultCaptor;
    private ArgumentCaptor<Throwable> operationExceptionCaptor;

    private MockedStatic<ServiceManager> serviceManagerMock;
    private MockedStatic<OutputFormatter> outputFormatterMock;
    private MockedStatic<ContextManager> contextManagerMock;

    private static final String TEST_ITEM_ID = "123e4567-e89b-12d3-a456-426614174000";

    @Before
    public void setUp() {
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        mockServiceManager = mock(ServiceManager.class);
        mockWorkflowService = new MockWorkflowService();
        mockConfigService = mock(ConfigurationService.class);
        mockMetadataService = mock(MetadataService.class);
        mockContextManager = mock(ContextManager.class);
        
        // Set up work items in the mock service
        setupWorkItems();
        
        // Set up mock service manager
        when(mockServiceManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up mocked statics
        serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
        serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        
        outputFormatterMock = Mockito.mockStatic(OutputFormatter.class);
        outputFormatterMock.when(() -> OutputFormatter.toJson(any(), anyBoolean())).thenAnswer(invocation -> {
            Map<String, Object> jsonData = invocation.getArgument(0);
            return new JSONObject(jsonData).toString(2);
        });
        
        contextManagerMock = Mockito.mockStatic(ContextManager.class);
        contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
        
        // Set up operation tracking
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn(operationId);
        
        // Set up argument captors
        operationParamsCaptor = ArgumentCaptor.forClass(Map.class);
        operationResultCaptor = ArgumentCaptor.forClass(Object.class);
        operationExceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
    }

    @After
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        outputCaptor.reset();
        errorCaptor.reset();
        
        // Close static mocks
        serviceManagerMock.close();
        outputFormatterMock.close();
        contextManagerMock.close();
    }
    
    private void setupWorkItems() {
        // Create test work item in progress
        WorkItem testItem = new WorkItem();
        testItem.setId(TEST_ITEM_ID);
        testItem.setTitle("Test Work Item");
        testItem.setDescription("Test Description");
        testItem.setType(WorkItemType.TASK);
        testItem.setPriority(Priority.MEDIUM);
        testItem.setState(WorkflowState.IN_PROGRESS);
        testItem.setAssignee("test.user");
        testItem.setCreated(LocalDateTime.now().minusDays(1));
        testItem.setUpdated(LocalDateTime.now().minusHours(1));
        mockWorkflowService.addItem(testItem);
    }

    @Given("the user is logged in with username {string}")
    public void theUserIsLoggedInWithUsername(String username) {
        when(mockConfigService.getCurrentUser()).thenReturn(username);
    }

    @Given("there are work items in various states")
    public void thereAreWorkItemsInVariousStates() {
        // The setup is done in the setUp method
    }

    @Given("a work item with ID {string} that cannot be transitioned to {string}")
    public void aWorkItemWithIdThatCannotBeTransitionedTo(String itemId, String targetState) {
        WorkItem blockedItem = new WorkItem();
        blockedItem.setId(itemId);
        blockedItem.setTitle("Blocked Item");
        blockedItem.setState(WorkflowState.BLOCKED);
        mockWorkflowService.addItem(blockedItem);
        mockWorkflowService.setAllowTransition(false);
        mockWorkflowService.setAvailableTransitions(Arrays.asList(WorkflowState.READY, WorkflowState.IN_PROGRESS));
    }

    @Given("a work item with ID {string} that throws exception when transitioned")
    public void aWorkItemWithIdThatThrowsExceptionWhenTransitioned(String itemId) {
        WorkItem errorItem = new WorkItem();
        errorItem.setId(itemId);
        errorItem.setTitle("Error Item");
        errorItem.setState(WorkflowState.IN_PROGRESS);
        mockWorkflowService.addItem(errorItem);
        mockWorkflowService.setThrowExceptionOnTransition(true);
    }

    @Given("a work item with ID {string} in {string} state")
    public void aWorkItemWithIdInState(String itemId, String state) {
        WorkItem item = new WorkItem();
        item.setId(itemId);
        item.setTitle(state + " Item");
        item.setState(WorkflowState.fromString(state));
        mockWorkflowService.addItem(item);
    }

    @When("the user executes done command with item ID {string}")
    public void theUserExecutesDoneCommandWithItemId(String itemId) {
        doneCommand = new DoneCommand();
        doneCommand.setItemId(itemId);
        exitCode = doneCommand.call();
    }

    @When("the user executes done command with item ID {string} and comment {string}")
    public void theUserExecutesDoneCommandWithItemIdAndComment(String itemId, String comment) {
        doneCommand = new DoneCommand();
        doneCommand.setItemId(itemId);
        doneCommand.setComment(comment);
        exitCode = doneCommand.call();
    }

    @When("the user executes done command with item ID {string} without comment")
    public void theUserExecutesDoneCommandWithItemIdWithoutComment(String itemId) {
        doneCommand = new DoneCommand();
        doneCommand.setItemId(itemId);
        exitCode = doneCommand.call();
    }

    @When("the user executes done command with item ID {string} and format {string}")
    public void theUserExecutesDoneCommandWithItemIdAndFormat(String itemId, String format) {
        doneCommand = new DoneCommand();
        doneCommand.setItemId(itemId);
        doneCommand.setFormat(format);
        exitCode = doneCommand.call();
    }

    @When("the user executes done command with item ID {string} and format {string} and verbose flag")
    public void theUserExecutesDoneCommandWithItemIdAndFormatAndVerboseFlag(String itemId, String format) {
        doneCommand = new DoneCommand();
        doneCommand.setItemId(itemId);
        doneCommand.setFormat(format);
        doneCommand.setVerbose(true);
        exitCode = doneCommand.call();
    }

    @When("the user executes done command without item ID")
    public void theUserExecutesDoneCommandWithoutItemId() {
        doneCommand = new DoneCommand();
        exitCode = doneCommand.call();
    }

    @When("the user executes done command with empty item ID")
    public void theUserExecutesDoneCommandWithEmptyItemId() {
        doneCommand = new DoneCommand();
        doneCommand.setItemId("");
        exitCode = doneCommand.call();
    }

    @Then("the command should execute successfully")
    public void theCommandShouldExecuteSuccessfully() {
        assertEquals(0, exitCode);
    }

    @Then("the command should fail with exit code {int}")
    public void theCommandShouldFailWithExitCode(int expectedExitCode) {
        assertEquals(expectedExitCode, exitCode);
    }

    @And("the work item status should be changed to {string}")
    public void theWorkItemStatusShouldBeChangedTo(String expectedStatus) {
        WorkItem item = mockWorkflowService.getItem(doneCommand.getItemId());
        assertNotNull(item, "Work item should exist");
        assertEquals(WorkflowState.fromString(expectedStatus), item.getState());
    }

    @And("the output should contain {string}")
    public void theOutputShouldContain(String expectedText) {
        assertTrue(outputCaptor.toString().contains(expectedText));
    }

    @And("the error output should contain {string}")
    public void theErrorOutputShouldContain(String expectedText) {
        assertTrue(errorCaptor.toString().contains(expectedText));
    }

    @And("the comment should be added to the work item")
    public void theCommentShouldBeAddedToTheWorkItem() {
        assertEquals(1, mockWorkflowService.getTransitionWithCommentCount());
        assertEquals(doneCommand.getComment(), mockWorkflowService.getLastTransitionComment());
    }

    @And("the transition should be done by user {string}")
    public void theTransitionShouldBeDoneByUser(String expectedUser) {
        assertEquals(expectedUser, mockWorkflowService.getLastTransitionUser());
    }

    @And("the simple transition method should be used")
    public void theSimpleTransitionMethodShouldBeUsed() {
        assertEquals(1, mockWorkflowService.getTransitionCallCount());
        assertEquals(0, mockWorkflowService.getTransitionWithCommentCount());
    }

    @And("the output should be in valid JSON format")
    public void theOutputShouldBeInValidJsonFormat() {
        String output = outputCaptor.toString().trim();
        try {
            new JSONObject(output);
            // If we get here, it's valid JSON
            assertTrue(true);
        } catch (Exception e) {
            fail("Output is not valid JSON: " + e.getMessage());
        }
    }

    @And("the JSON output should contain key {string}")
    public void theJsonOutputShouldContainKey(String expectedKey) {
        String output = outputCaptor.toString().trim();
        JSONObject json = new JSONObject(output);
        assertTrue(json.has(expectedKey), "JSON should contain key " + expectedKey);
    }

    @And("an operation with name {string} and type {string} should be started")
    public void anOperationWithNameAndTypeShouldBeStarted(String operationName, String operationType) {
        verify(mockMetadataService).startOperation(eq(operationName), eq(operationType), operationParamsCaptor.capture());
        capturedOperationParams = operationParamsCaptor.getValue();
        assertNotNull(capturedOperationParams, "Operation parameters should not be null");
    }

    @And("the operation parameters should include item ID")
    public void theOperationParametersShouldIncludeItemId() {
        assertNotNull(capturedOperationParams, "Operation parameters should not be null");
        assertTrue(capturedOperationParams.containsKey("itemId"), "Parameters should include itemId");
        assertEquals(doneCommand.getItemId(), capturedOperationParams.get("itemId"));
    }

    @And("the operation should be completed")
    public void theOperationShouldBeCompleted() {
        verify(mockMetadataService).completeOperation(eq(operationId), operationResultCaptor.capture());
        capturedOperationResult = operationResultCaptor.getValue();
        assertNotNull(capturedOperationResult, "Operation result should not be null");
    }

    @And("the operation should be marked as failed")
    public void theOperationShouldBeMarkedAsFailed() {
        verify(mockMetadataService).failOperation(eq(operationId), operationExceptionCaptor.capture());
        assertNotNull(operationExceptionCaptor.getValue(), "Exception should not be null");
    }

    @And("the operation result should include item ID and title")
    public void theOperationResultShouldIncludeItemIdAndTitle() {
        assertTrue(capturedOperationResult instanceof Map, "Result should be a Map");
        Map<String, Object> result = (Map<String, Object>) capturedOperationResult;
        assertTrue(result.containsKey("itemId"), "Result should include itemId");
        assertTrue(result.containsKey("title"), "Result should include title");
        assertEquals(doneCommand.getItemId(), result.get("itemId"));
    }

    @And("the failure reason should be captured")
    public void theFailureReasonShouldBeCaptured() {
        Throwable exception = operationExceptionCaptor.getValue();
        assertNotNull(exception, "Exception should not be null");
        
        // Could be either IllegalArgumentException or InvalidTransitionException
        assertTrue(
            exception instanceof IllegalArgumentException || 
            exception instanceof InvalidTransitionException,
            "Exception should be of expected type but was " + exception.getClass().getSimpleName()
        );
    }
}