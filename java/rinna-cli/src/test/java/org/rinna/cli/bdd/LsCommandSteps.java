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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.LsCommand;
import org.rinna.cli.domain.model.WorkItemRelationshipType;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockHistoryService;
import org.rinna.cli.service.MockHistoryService.HistoryEntryRecord;
import org.rinna.cli.service.MockHistoryService.HistoryEntryType;
import org.rinna.cli.service.MockRelationshipService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for the LsCommand Cucumber tests.
 */
public class LsCommandSteps {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    private LsCommand lsCommand;
    private int exitCode;

    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private ItemService mockItemService;
    
    @Mock
    private MockRelationshipService mockRelationshipService;
    
    @Mock
    private MockHistoryService mockHistoryService;
    
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
    private ArgumentCaptor<UUID> contextItemIdCaptor;

    private MockedStatic<ServiceManager> serviceManagerMock;
    private MockedStatic<OutputFormatter> outputFormatterMock;
    private MockedStatic<ContextManager> contextManagerMock;

    private static final String TEST_ITEM_ID = "123e4567-e89b-12d3-a456-426614174000";
    private List<WorkItem> testWorkItems;

    @Before
    public void setUp() {
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        mockServiceManager = mock(ServiceManager.class);
        mockItemService = mock(ItemService.class);
        mockRelationshipService = mock(MockRelationshipService.class);
        mockHistoryService = mock(MockHistoryService.class);
        mockMetadataService = mock(MetadataService.class);
        mockContextManager = mock(ContextManager.class);
        
        // Set up work items for testing
        testWorkItems = createTestWorkItems();
        
        // Set up mock service manager
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockRelationshipService()).thenReturn(mockRelationshipService);
        when(mockServiceManager.getMockHistoryService()).thenReturn(mockHistoryService);
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
        operationResultCaptor = ArgumentCaptor.forClass(Map.class);
        operationExceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        contextItemIdCaptor = ArgumentCaptor.forClass(UUID.class);
        
        // Default mock for getting all items
        when(mockItemService.getAllItems()).thenReturn(testWorkItems);
        
        // Default mock for getting a specific item
        when(mockItemService.getItem(TEST_ITEM_ID)).thenReturn(findItemById(TEST_ITEM_ID));
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
    
    private List<WorkItem> createTestWorkItems() {
        List<WorkItem> items = new ArrayList<>();
        
        // Add the test item with specified ID
        WorkItem testItem = new WorkItem();
        testItem.setId(TEST_ITEM_ID);
        testItem.setTitle("Test Work Item");
        testItem.setDescription("Test Description");
        testItem.setType(WorkItemType.TASK);
        testItem.setPriority(Priority.MEDIUM);
        testItem.setState(WorkflowState.IN_PROGRESS);
        testItem.setAssignee("test.user");
        testItem.setProject("Test Project");
        testItem.setCreated(LocalDateTime.now().minusDays(1));
        testItem.setUpdated(LocalDateTime.now().minusHours(1));
        items.add(testItem);
        
        // Add a few more items with different states
        WorkItem item2 = new WorkItem();
        item2.setId(UUID.randomUUID().toString());
        item2.setTitle("Another Work Item");
        item2.setDescription("Another Description");
        item2.setType(WorkItemType.BUG);
        item2.setPriority(Priority.HIGH);
        item2.setState(WorkflowState.OPEN);
        item2.setAssignee("other.user");
        item2.setProject("Test Project");
        item2.setCreated(LocalDateTime.now().minusDays(2));
        item2.setUpdated(LocalDateTime.now().minusHours(2));
        items.add(item2);
        
        WorkItem item3 = new WorkItem();
        item3.setId(UUID.randomUUID().toString());
        item3.setTitle("Completed Work Item");
        item3.setDescription("Completed Description");
        item3.setType(WorkItemType.TASK);
        item3.setPriority(Priority.LOW);
        item3.setState(WorkflowState.DONE);
        item3.setAssignee("test.user");
        item3.setProject("Test Project");
        item3.setCreated(LocalDateTime.now().minusDays(3));
        item3.setUpdated(LocalDateTime.now().minusHours(3));
        items.add(item3);
        
        return items;
    }
    
    private WorkItem findItemById(String id) {
        for (WorkItem item : testWorkItems) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }
    
    private List<HistoryEntryRecord> createMockHistory(UUID itemId) {
        List<HistoryEntryRecord> history = new ArrayList<>();
        
        // Add state change history entry
        history.add(new HistoryEntryRecord(
            itemId, 
            HistoryEntryType.STATE_CHANGE, 
            "test.user", 
            "State changed from NEW to OPEN", 
            null, 
            Instant.now().minus(3, ChronoUnit.DAYS)
        ));
        
        // Add field change history entry
        history.add(new HistoryEntryRecord(
            itemId, 
            HistoryEntryType.FIELD_CHANGE, 
            "test.user", 
            "Field 'priority' changed from 'LOW' to 'MEDIUM'", 
            "Priority", 
            Instant.now().minus(2, ChronoUnit.DAYS)
        ));
        
        // Add assignment history entry
        history.add(new HistoryEntryRecord(
            itemId, 
            HistoryEntryType.ASSIGNMENT, 
            "admin", 
            "Assignment changed from 'unassigned' to 'test.user'", 
            null, 
            Instant.now().minus(1, ChronoUnit.DAYS)
        ));
        
        return history;
    }

    @Given("the user is authenticated with username {string}")
    public void theUserIsAuthenticatedWithUsername(String username) {
        // Authentication is not directly used in LsCommand but could be in the future
    }

    @Given("the system has work items in various states")
    public void theSystemHasWorkItemsInVariousStates() {
        // The setup is done in the setUp method
    }
    
    @Given("the system has no work items")
    public void theSystemHasNoWorkItems() {
        when(mockItemService.getAllItems()).thenReturn(new ArrayList<>());
    }
    
    @Given("the system has work items with parent-child relationships")
    public void theSystemHasWorkItemsWithParentChildRelationships() {
        // Set up parent-child relationships for testing
        UUID testUUID = UUID.fromString(TEST_ITEM_ID);
        UUID parentId = UUID.randomUUID();
        List<UUID> childIds = List.of(UUID.randomUUID(), UUID.randomUUID());
        
        when(mockRelationshipService.getParentWorkItem(testUUID)).thenReturn(parentId);
        when(mockRelationshipService.getRelationshipType(eq(testUUID), any(UUID.class)))
            .thenReturn(WorkItemRelationshipType.PARENT_CHILD);
        when(mockRelationshipService.getChildWorkItems(testUUID)).thenReturn(childIds);
        
        // For the other test items
        for (WorkItem item : testWorkItems) {
            if (!item.getId().equals(TEST_ITEM_ID)) {
                UUID id = UUID.fromString(item.getId());
                when(mockRelationshipService.getParentWorkItem(id)).thenReturn(null);
                when(mockRelationshipService.getChildWorkItems(id)).thenReturn(new ArrayList<>());
            }
        }
    }

    @When("the user executes ls command without options")
    public void theUserExecutesLsCommandWithoutOptions() {
        lsCommand = new LsCommand();
        exitCode = lsCommand.call();
    }
    
    @When("the user executes ls command with long format option")
    public void theUserExecutesLsCommandWithLongFormatOption() {
        lsCommand = new LsCommand();
        lsCommand.setLongFormat(true);
        exitCode = lsCommand.call();
    }
    
    @When("the user executes ls command with long and all format options")
    public void theUserExecutesLsCommandWithLongAndAllFormatOptions() {
        lsCommand = new LsCommand();
        lsCommand.setLongFormat(true);
        lsCommand.setAllFormat(true);
        exitCode = lsCommand.call();
        
        // Set up mock history for testing
        UUID testUUID = UUID.fromString(TEST_ITEM_ID);
        when(mockHistoryService.getHistory(testUUID)).thenReturn(createMockHistory(testUUID));
    }
    
    @When("the user executes ls command with item ID {string}")
    public void theUserExecutesLsCommandWithItemId(String itemId) {
        lsCommand = new LsCommand();
        lsCommand.setItemId(itemId);
        exitCode = lsCommand.call();
    }
    
    @When("the user executes ls command with item ID {string} and long format")
    public void theUserExecutesLsCommandWithItemIdAndLongFormat(String itemId) {
        lsCommand = new LsCommand();
        lsCommand.setItemId(itemId);
        lsCommand.setLongFormat(true);
        exitCode = lsCommand.call();
    }
    
    @When("the user executes ls command with item ID {string} and long and all format")
    public void theUserExecutesLsCommandWithItemIdAndLongAndAllFormat(String itemId) {
        lsCommand = new LsCommand();
        lsCommand.setItemId(itemId);
        lsCommand.setLongFormat(true);
        lsCommand.setAllFormat(true);
        exitCode = lsCommand.call();
        
        // Set up mock history for when the history is requested
        if (itemId.equals(TEST_ITEM_ID)) {
            UUID testUUID = UUID.fromString(TEST_ITEM_ID);
            when(mockHistoryService.getHistory(testUUID)).thenReturn(createMockHistory(testUUID));
        }
    }
    
    @When("the user executes ls command with JSON format")
    public void theUserExecutesLsCommandWithJsonFormat() {
        lsCommand = new LsCommand();
        lsCommand.setFormat("json");
        exitCode = lsCommand.call();
    }
    
    @When("the user executes ls command with item ID {string} and JSON format")
    public void theUserExecutesLsCommandWithItemIdAndJsonFormat(String itemId) {
        lsCommand = new LsCommand();
        lsCommand.setItemId(itemId);
        lsCommand.setFormat("json");
        exitCode = lsCommand.call();
    }
    
    @When("the user executes ls command with item ID {string} and JSON format and verbose flag")
    public void theUserExecutesLsCommandWithItemIdAndJsonFormatAndVerboseFlag(String itemId) {
        lsCommand = new LsCommand();
        lsCommand.setItemId(itemId);
        lsCommand.setFormat("json");
        lsCommand.setVerbose(true);
        exitCode = lsCommand.call();
    }

    @Then("the command should execute successfully")
    public void theCommandShouldExecuteSuccessfully() {
        assertEquals(0, exitCode);
    }
    
    @Then("the command should fail with exit code {int}")
    public void theCommandShouldFailWithExitCode(int expectedCode) {
        assertEquals(expectedCode, exitCode);
    }
    
    @And("the output should display work items in tabular format")
    public void theOutputShouldDisplayWorkItemsInTabularFormat() {
        String output = outputCaptor.toString();
        assertTrue(output.contains(TEST_ITEM_ID));
        assertTrue(output.contains("Test Work Item"));
        assertTrue(output.contains("IN_PROGRESS"));
    }
    
    @And("the output should contain column headers")
    public void theOutputShouldContainColumnHeaders() {
        String output = outputCaptor.toString();
        assertTrue(output.contains("ID"));
        assertTrue(output.contains("Title"));
        assertTrue(output.contains("State"));
        assertTrue(output.contains("Priority"));
        assertTrue(output.contains("Assignee"));
    }
    
    @And("the output should include the work item IDs")
    public void theOutputShouldIncludeTheWorkItemIds() {
        String output = outputCaptor.toString();
        assertTrue(output.contains(TEST_ITEM_ID));
        // Check for other test work item IDs 
        for (WorkItem item : testWorkItems) {
            if (!item.getId().equals(TEST_ITEM_ID)) {
                assertTrue(output.contains(item.getId()));
            }
        }
    }
    
    @And("the output should display detailed work item information")
    public void theOutputShouldDisplayDetailedWorkItemInformation() {
        String output = outputCaptor.toString();
        assertTrue(output.contains("Work Item:"));
        assertTrue(output.contains("Title:"));
        assertTrue(output.contains("Description:"));
        assertTrue(output.contains("Type:"));
        assertTrue(output.contains("Priority:"));
        assertTrue(output.contains("State:"));
        assertTrue(output.contains("Assignee:"));
        assertTrue(output.contains("Project:"));
        assertTrue(output.contains("Created:"));
        assertTrue(output.contains("Updated:"));
    }
    
    @And("the output should contain {string}")
    public void theOutputShouldContain(String expectedText) {
        String output = outputCaptor.toString();
        assertTrue(output.contains(expectedText), "Output should contain: " + expectedText);
    }
    
    @And("the output should include history information")
    public void theOutputShouldIncludeHistoryInformation() {
        String output = outputCaptor.toString();
        assertTrue(output.contains("History:"));
        assertTrue(output.contains("STATE_CHANGE"));
        assertTrue(output.contains("FIELD_CHANGE"));
        assertTrue(output.contains("ASSIGNMENT"));
    }
    
    @And("the output should display information for only that work item")
    public void theOutputShouldDisplayInformationForOnlyThatWorkItem() {
        String output = outputCaptor.toString();
        assertTrue(output.contains(TEST_ITEM_ID));
        assertTrue(output.contains("Test Work Item"));
        assertTrue(output.contains("IN_PROGRESS"));
        // Make sure other work items are not displayed
        for (WorkItem item : testWorkItems) {
            if (!item.getId().equals(TEST_ITEM_ID)) {
                assertFalse(output.contains(item.getTitle()));
            }
        }
    }
    
    @And("the context manager should update with the viewed item")
    public void theContextManagerShouldUpdateWithTheViewedItem() {
        verify(mockContextManager).setLastViewedWorkItem(contextItemIdCaptor.capture());
        UUID capturedId = contextItemIdCaptor.getValue();
        assertEquals(TEST_ITEM_ID, capturedId.toString());
    }
    
    @And("the output should display detailed information for that work item")
    public void theOutputShouldDisplayDetailedInformationForThatWorkItem() {
        String output = outputCaptor.toString();
        assertTrue(output.contains("Work Item: " + TEST_ITEM_ID));
        assertTrue(output.contains("Title: Test Work Item"));
        assertTrue(output.contains("Description: Test Description"));
        assertTrue(output.contains("Type: TASK"));
        assertTrue(output.contains("Priority: MEDIUM"));
        assertTrue(output.contains("State: IN_PROGRESS"));
        assertTrue(output.contains("Assignee: test.user"));
        assertTrue(output.contains("Project: Test Project"));
    }
    
    @And("the output should include relationship information")
    public void theOutputShouldIncludeRelationshipInformation() {
        String output = outputCaptor.toString();
        assertTrue(output.contains("Parent:"));
        assertTrue(output.contains("Children:"));
    }
    
    @And("the output should include timestamp information")
    public void theOutputShouldIncludeTimestampInformation() {
        String output = outputCaptor.toString();
        // History entries have timestamps
        assertTrue(output.contains("TEST_EXAMPLE_TIMESTAMP") || output.matches(".*\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}.*"));
    }
    
    @And("the output should be in valid JSON format")
    public void theOutputShouldBeInValidJsonFormat() {
        String output = outputCaptor.toString().trim();
        try {
            new JSONObject(output);
            // If we get here, it's valid JSON
            assertTrue(true);
        } catch (Exception e) {
            try {
                // Try as JSONArray in case it's an array format
                new JSONArray(output);
                assertTrue(true);
            } catch (Exception e2) {
                fail("Output is not valid JSON: " + e.getMessage());
            }
        }
    }
    
    @And("the JSON output should contain array of work items")
    public void theJsonOutputShouldContainArrayOfWorkItems() {
        String output = outputCaptor.toString().trim();
        JSONObject json = new JSONObject(output);
        assertTrue(json.has("workItems"), "JSON should contain workItems array");
        assertTrue(json.getJSONArray("workItems").length() > 0, "workItems array should not be empty");
    }
    
    @And("the JSON output should contain metadata like item count")
    public void theJsonOutputShouldContainMetadataLikeItemCount() {
        String output = outputCaptor.toString().trim();
        JSONObject json = new JSONObject(output);
        assertTrue(json.has("count"), "JSON should contain count field");
        assertTrue(json.has("displayOptions"), "JSON should contain displayOptions field");
    }
    
    @And("the JSON output should contain item details")
    public void theJsonOutputShouldContainItemDetails() {
        String output = outputCaptor.toString().trim();
        JSONObject json = new JSONObject(output);
        assertTrue(json.has("id"), "JSON should contain id field");
        assertTrue(json.has("title"), "JSON should contain title field");
        assertTrue(json.has("state"), "JSON should contain state field");
        assertTrue(json.has("priority"), "JSON should contain priority field");
        assertTrue(json.has("assignee"), "JSON should contain assignee field");
    }
    
    @And("the JSON output should contain relationship information")
    public void theJsonOutputShouldContainRelationshipInformation() {
        String output = outputCaptor.toString().trim();
        JSONObject json = new JSONObject(output);
        assertTrue(json.has("relationships"), "JSON should contain relationships field");
    }
    
    @And("the JSON output should contain extended item information")
    public void theJsonOutputShouldContainExtendedItemInformation() {
        String output = outputCaptor.toString().trim();
        JSONObject json = new JSONObject(output);
        assertTrue(json.has("description"), "JSON should contain description field");
        assertTrue(json.has("project"), "JSON should contain project field");
        assertTrue(json.has("created"), "JSON should contain created field");
        assertTrue(json.has("updated"), "JSON should contain updated field");
    }
    
    @And("the error output should contain {string}")
    public void theErrorOutputShouldContain(String expectedText) {
        String errorOutput = errorCaptor.toString();
        assertTrue(errorOutput.contains(expectedText), "Error output should contain: " + expectedText);
    }
    
    @And("the output should include parent relationship information")
    public void theOutputShouldIncludeParentRelationshipInformation() {
        String output = outputCaptor.toString();
        assertTrue(output.contains("Child of:"), "Output should show parent relationship");
    }
    
    @And("the output should include child relationship information")
    public void theOutputShouldIncludeChildRelationshipInformation() {
        String output = outputCaptor.toString();
        assertTrue(output.contains("Parent of:"), "Output should show child relationship");
    }
    
    @And("an operation with name {string} and type {string} should be tracked")
    public void anOperationWithNameAndTypeShouldBeTracked(String name, String type) {
        verify(mockMetadataService).startOperation(eq(name), eq(type), operationParamsCaptor.capture());
        capturedOperationParams = operationParamsCaptor.getValue();
        assertNotNull(capturedOperationParams, "Operation parameters should not be null");
    }
    
    @And("the operation parameters should include format options")
    public void theOperationParametersShouldIncludeFormatOptions() {
        assertNotNull(capturedOperationParams, "Operation parameters should not be null");
        assertTrue(capturedOperationParams.containsKey("format"), "Parameters should include format");
        assertTrue(capturedOperationParams.containsKey("long_format"), "Parameters should include long_format");
        assertTrue(capturedOperationParams.containsKey("all_format"), "Parameters should include all_format");
    }
    
    @And("the operation should be completed successfully")
    public void theOperationShouldBeCompletedSuccessfully() {
        verify(mockMetadataService).completeOperation(eq(operationId), operationResultCaptor.capture());
        assertNotNull(operationResultCaptor.getValue(), "Operation result should not be null");
    }
    
    @And("the operation result should include item count")
    public void theOperationResultShouldIncludeItemCount() {
        Map<String, Object> result = operationResultCaptor.getValue();
        assertTrue(result.containsKey("items_found"), "Result should include items_found");
        assertNotNull(result.get("items_found"), "items_found should not be null");
        assertTrue(result.get("items_found") instanceof Integer, "items_found should be an integer");
        if (lsCommand.getItemId() == null) {
            assertEquals(testWorkItems.size(), result.get("items_found"));
        }
    }
    
    @And("the operation should be marked as failed")
    public void theOperationShouldBeMarkedAsFailed() {
        verify(mockMetadataService).failOperation(eq(operationId), operationExceptionCaptor.capture());
        assertNotNull(operationExceptionCaptor.getValue(), "Exception should not be null");
    }
    
    @And("the failure reason should be captured")
    public void theFailureReasonShouldBeCaptured() {
        Throwable exception = operationExceptionCaptor.getValue();
        assertNotNull(exception, "Exception should not be null");
    }
}