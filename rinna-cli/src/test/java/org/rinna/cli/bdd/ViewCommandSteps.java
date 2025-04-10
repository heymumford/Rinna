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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.ViewCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.ModelMapper;
import org.rinna.cli.util.OutputFormatter;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for the View Command BDD tests.
 */
public class ViewCommandSteps {

    private TestContext testContext;
    private ViewCommand viewCommand;
    
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    private String currentUser;
    private UUID testItemId;
    private WorkItem testWorkItem;
    
    private int commandResult;
    
    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private ItemService mockItemService;
    
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
    
    public ViewCommandSteps(TestContext testContext) {
        this.testContext = testContext;
    }
    
    @Before
    public void setUp() {
        // Initialize mocks
        mockServiceManager = Mockito.mock(ServiceManager.class);
        mockItemService = Mockito.mock(ItemService.class);
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
        mockedStaticOutputFormatter.when(() -> OutputFormatter.toJson(any(WorkItem.class), anyBoolean()))
            .thenAnswer(invocation -> {
                WorkItem item = invocation.getArgument(0);
                boolean verbose = invocation.getArgument(1);
                
                StringBuilder json = new StringBuilder();
                json.append("{\n");
                json.append("  \"id\": \"").append(item.getId()).append("\",\n");
                json.append("  \"title\": \"").append(item.getTitle()).append("\",\n");
                
                if (verbose) {
                    json.append("  \"description\": \"").append(item.getDescription()).append("\",\n");
                    json.append("  \"reporter\": \"").append(item.getReporter()).append("\",\n");
                    json.append("  \"created\": \"").append(item.getCreated()).append("\",\n");
                    json.append("  \"updated\": \"").append(item.getUpdated()).append("\",\n");
                    json.append("  \"dueDate\": \"").append(item.getDueDate()).append("\",\n");
                    json.append("  \"project\": \"").append(item.getProject()).append("\",\n");
                    json.append("  \"version\": \"").append(item.getVersion()).append("\",\n");
                }
                
                json.append("  \"type\": \"").append(item.getType()).append("\",\n");
                json.append("  \"priority\": \"").append(item.getPriority()).append("\",\n");
                json.append("  \"status\": \"").append(item.getStatus()).append("\",\n");
                json.append("  \"assignee\": \"").append(item.getAssignee()).append("\"\n");
                json.append("}");
                
                return json.toString();
            });
        
        // Set up the service manager mock
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up the metadata service mock
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn("operation-id");
        
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
        testWorkItem = createTestWorkItem();
        
        // Create a new ViewCommand instance
        viewCommand = new ViewCommand(mockServiceManager);
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
    
    private WorkItem createTestWorkItem() {
        WorkItem item = new WorkItem();
        item.setId(testItemId.toString());
        item.setTitle("Fix login bug");
        item.setType(WorkItemType.BUG);
        item.setPriority(Priority.HIGH);
        item.setStatus(WorkflowState.IN_PROGRESS);
        item.setAssignee("alice");
        
        return item;
    }
    
    @Given("a user {string} with basic permissions")
    public void aUserWithBasicPermissions(String username) {
        currentUser = username;
    }
    
    @Given("a work item {string} with title {string} exists")
    public void aWorkItemWithTitleExists(String itemId, String title) {
        // Extract numeric part from WI-123 format
        if (itemId.startsWith("WI-")) {
            String numericPart = itemId.split("-")[1];
            testItemId = new UUID(0, Long.parseLong(numericPart));
        } else {
            testItemId = UUID.randomUUID();
        }
        
        testWorkItem.setId(testItemId.toString());
        testWorkItem.setTitle(title);
        
        // Set up mock behavior
        mockedStaticModelMapper.when(() -> ModelMapper.toUUID(itemId)).thenReturn(testItemId);
        when(mockItemService.getItem(testItemId.toString())).thenReturn(testWorkItem);
    }
    
    @Given("the work item has detailed information")
    public void theWorkItemHasDetailedInformation() {
        // Add detailed information to the test work item
        testWorkItem.setDescription("A critical bug in the login system prevents users from logging in using social media accounts.");
        testWorkItem.setReporter("bob");
        testWorkItem.setProject("UserAuth");
        testWorkItem.setVersion("2.1.0");
        testWorkItem.setCreated(LocalDateTime.now().minusDays(5));
        testWorkItem.setUpdated(LocalDateTime.now().minusHours(12));
        testWorkItem.setDueDate(LocalDateTime.now().plusDays(2));
    }
    
    @When("the user runs {string}")
    public void theUserRunsCommand(String command) {
        // Parse and execute the command
        parseAndExecuteCommand(command);
    }
    
    @When("the user runs {string} without an item ID")
    public void theUserRunsCommandWithoutAnItemId(String command) {
        // Run view command without setting an item ID
        viewCommand = new ViewCommand(mockServiceManager);
        commandResult = viewCommand.call();
    }
    
    @When("the item service throws an exception")
    public void theItemServiceThrowsAnException() {
        // Set up the id and mock to throw an exception
        viewCommand.setId("WI-123");
        when(mockItemService.getItem(anyString())).thenThrow(new RuntimeException("Test exception"));
        
        // Execute the command
        commandResult = viewCommand.call();
    }
    
    private void parseAndExecuteCommand(String command) {
        // Reset viewCommand to ensure clean state
        viewCommand = new ViewCommand(mockServiceManager);
        
        // Extract item ID if present
        if (command.contains("view ")) {
            String[] parts = command.split(" ");
            if (parts.length > 1 && !parts[1].startsWith("--")) {
                viewCommand.setId(parts[1]);
                
                // Set up ModelMapper mock for valid item IDs
                if (parts[1].equals("WI-123")) {
                    mockedStaticModelMapper.when(() -> ModelMapper.toUUID(parts[1])).thenReturn(testItemId);
                } else if (parts[1].equals("invalid-id")) {
                    mockedStaticModelMapper.when(() -> ModelMapper.toUUID(parts[1]))
                        .thenThrow(new IllegalArgumentException("Invalid UUID format"));
                } else if (parts[1].equals("non-existent-id")) {
                    mockedStaticModelMapper.when(() -> ModelMapper.toUUID(parts[1])).thenReturn(UUID.randomUUID());
                    when(mockItemService.getItem(anyString())).thenReturn(null);
                }
            }
        }
        
        // Parse format flag if present
        if (command.contains("--format=json")) {
            viewCommand.setFormat("json");
        }
        
        // Parse verbose flag if present
        if (command.contains("--verbose")) {
            viewCommand.setVerbose(true);
        }
        
        // Execute the command
        commandResult = viewCommand.call();
    }
    
    @Then("the command should succeed")
    public void theCommandShouldSucceed() {
        assertEquals("Command should succeed with exit code 0", 0, commandResult);
    }
    
    @Then("the command should fail")
    public void theCommandShouldFail() {
        assertEquals("Command should fail with non-zero exit code", 1, commandResult);
    }
    
    @Then("the output should display basic work item information")
    public void theOutputShouldDisplayBasicWorkItemInformation() {
        String output = outContent.toString();
        
        // Verify basic work item information is present
        assertTrue("Output should contain work item ID", output.contains(testWorkItem.getId()));
        assertTrue("Output should contain work item title", output.contains(testWorkItem.getTitle()));
        assertTrue("Output should contain work item type", output.contains(testWorkItem.getType().toString()));
        assertTrue("Output should contain work item priority", output.contains(testWorkItem.getPriority().toString()));
        assertTrue("Output should contain work item status", output.contains(testWorkItem.getStatus().toString()));
        assertTrue("Output should contain work item assignee", output.contains(testWorkItem.getAssignee()));
    }
    
    @Then("the command should track operation details")
    public void theCommandShouldTrackOperationDetails() {
        verify(mockMetadataService).startOperation(eq("view"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify params contain expected details
        assertTrue("Parameters should contain item_id", params.containsKey("item_id"));
        assertTrue("Parameters should contain format", params.containsKey("format"));
        assertTrue("Parameters should contain verbose", params.containsKey("verbose"));
        
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        Map<String, Object> result = resultCaptor.getValue();
        
        // Verify result contains expected details
        assertTrue("Result should contain item_id", result.containsKey("item_id"));
        assertTrue("Result should contain title", result.containsKey("title"));
        assertTrue("Result should contain status", result.containsKey("status"));
    }
    
    @Then("the output should be valid JSON")
    public void theOutputShouldBeValidJson() {
        String output = outContent.toString();
        
        // Basic JSON format validation
        assertTrue("Output should start with {", output.trim().startsWith("{"));
        assertTrue("Output should end with }", output.trim().endsWith("}"));
        assertTrue("Output should contain field-value pairs with quotes", output.contains("\"id\":"));
    }
    
    @Then("the JSON should include work item details")
    public void theJsonShouldIncludeWorkItemDetails() {
        String output = outContent.toString();
        
        // Verify basic work item details are present in JSON
        assertTrue("JSON should contain work item ID", output.contains("\"id\": \"" + testWorkItem.getId() + "\""));
        assertTrue("JSON should contain work item title", output.contains("\"title\": \"" + testWorkItem.getTitle() + "\""));
        assertTrue("JSON should contain work item type", output.contains("\"type\": \"" + testWorkItem.getType() + "\""));
        assertTrue("JSON should contain work item priority", output.contains("\"priority\": \"" + testWorkItem.getPriority() + "\""));
        assertTrue("JSON should contain work item status", output.contains("\"status\": \"" + testWorkItem.getStatus() + "\""));
        assertTrue("JSON should contain work item assignee", output.contains("\"assignee\": \"" + testWorkItem.getAssignee() + "\""));
    }
    
    @Then("the command should track format parameters")
    public void theCommandShouldTrackFormatParameters() {
        verify(mockMetadataService).startOperation(eq("view"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify format is tracked
        assertEquals("Parameter format should be json", "json", params.get("format"));
    }
    
    @Then("the output should include additional work item details")
    public void theOutputShouldIncludeAdditionalWorkItemDetails() {
        String output = outContent.toString();
        
        // Verify detailed work item information is present
        assertTrue("Output should contain work item description", output.contains(testWorkItem.getDescription()));
        assertTrue("Output should contain reporter information", output.contains(testWorkItem.getReporter()));
        assertTrue("Output should contain created date", output.contains("Created:"));
        assertTrue("Output should contain updated date", output.contains("Updated:"));
        assertTrue("Output should contain project information", output.contains(testWorkItem.getProject()));
        assertTrue("Output should contain version information", output.contains(testWorkItem.getVersion()));
        assertTrue("Output should contain due date information", output.contains("Due Date:"));
    }
    
    @Then("the command should track the verbose parameter")
    public void theCommandShouldTrackTheVerboseParameter() {
        verify(mockMetadataService).startOperation(eq("view"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify verbose flag is tracked
        assertEquals("Parameter verbose should be true", true, params.get("verbose"));
    }
    
    @Then("the JSON should include extended work item details")
    public void theJsonShouldIncludeExtendedWorkItemDetails() {
        String output = outContent.toString();
        
        // Verify extended details are present in JSON
        assertTrue("JSON should contain work item description", output.contains("\"description\": \"" + testWorkItem.getDescription() + "\""));
        assertTrue("JSON should contain reporter", output.contains("\"reporter\": \"" + testWorkItem.getReporter() + "\""));
        assertTrue("JSON should contain created date", output.contains("\"created\":"));
        assertTrue("JSON should contain updated date", output.contains("\"updated\":"));
        assertTrue("JSON should contain project", output.contains("\"project\": \"" + testWorkItem.getProject() + "\""));
        assertTrue("JSON should contain version", output.contains("\"version\": \"" + testWorkItem.getVersion() + "\""));
        assertTrue("JSON should contain due date", output.contains("\"dueDate\":"));
    }
    
    @Then("the command should track both format and verbose parameters")
    public void theCommandShouldTrackBothFormatAndVerboseParameters() {
        verify(mockMetadataService).startOperation(eq("view"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify both format and verbose are tracked
        assertEquals("Parameter format should be json", "json", params.get("format"));
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
    
    @Then("the error output should indicate issue retrieving work item")
    public void theErrorOutputShouldIndicateIssueRetrievingWorkItem() {
        String errorOutput = errContent.toString();
        assertTrue("Error output should indicate issue retrieving work item", 
                errorOutput.contains("Error retrieving work item"));
    }
    
    @Then("the command should track operation failure")
    public void theCommandShouldTrackOperationFailure() {
        verify(mockMetadataService).failOperation(anyString(), any(Exception.class));
    }
}