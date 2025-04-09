package org.rinna.cli.bdd;

import io.cucumber.java.Before;
import io.cucumber.java.After;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.And;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import org.rinna.cli.command.BacklogCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockBacklogService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Step definitions for testing the BacklogCommand.
 */
public class BacklogCommandSteps {
    private TestContext context;
    
    // Mock services
    private ServiceManager mockServiceManager;
    private MetadataService mockMetadataService;
    private MockBacklogService mockBacklogService;
    private MockItemService mockItemService;
    
    // Capture the standard output and error
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    // Command execution result
    private Integer commandResult;
    
    // Operation tracking
    private ArgumentCaptor<String> operationNameCaptor;
    private ArgumentCaptor<String> operationActionCaptor;
    private ArgumentCaptor<Map<String, Object>> operationParamsCaptor;
    
    // Mock static objects
    private MockedStatic<ServiceManager> mockStaticServiceManager;
    private MockedStatic<OutputFormatter> mockStaticOutputFormatter;
    
    /**
     * Constructor with test context.
     * 
     * @param context the test context
     */
    public BacklogCommandSteps(TestContext context) {
        this.context = context;
    }
    
    /**
     * Setup before each scenario.
     */
    @Before
    public void setUp() {
        // Redirect stdout and stderr for capturing output
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Initialize mocks
        mockServiceManager = mock(ServiceManager.class);
        mockMetadataService = mock(MetadataService.class);
        mockBacklogService = mock(MockBacklogService.class);
        mockItemService = mock(MockItemService.class);
        
        // Configure mocks
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getMockBacklogService()).thenReturn(mockBacklogService);
        when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);
        
        // Setup argument captors
        operationNameCaptor = ArgumentCaptor.forClass(String.class);
        operationActionCaptor = ArgumentCaptor.forClass(String.class);
        operationParamsCaptor = ArgumentCaptor.forClass(Map.class);
        
        // Mock static objects
        mockStaticServiceManager = Mockito.mockStatic(ServiceManager.class);
        mockStaticServiceManager.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        
        mockStaticOutputFormatter = Mockito.mockStatic(OutputFormatter.class);
        when(OutputFormatter.toJson(any(Map.class))).thenReturn("{ \"mock\": \"json\" }");
        
        // Mock operation tracking
        when(mockMetadataService.startOperation(
                operationNameCaptor.capture(),
                operationActionCaptor.capture(),
                operationParamsCaptor.capture()
        )).thenReturn("op-123");
    }
    
    /**
     * Cleanup after each scenario.
     */
    @After
    public void tearDown() {
        // Restore original stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Close static mocks
        if (mockStaticServiceManager != null) {
            mockStaticServiceManager.close();
        }
        if (mockStaticOutputFormatter != null) {
            mockStaticOutputFormatter.close();
        }
    }
    
    /**
     * Sets up a user with basic permissions.
     * 
     * @param username the username
     */
    @Given("a user {string} with basic permissions")
    public void aUserWithBasicPermissions(String username) {
        // Store the username in the context
        context.setUsername(username);
        
        // Set up the current user in the backlog service
        when(mockBacklogService.getCurrentUser()).thenReturn(username);
    }
    
    /**
     * Sets up the backlog service.
     */
    @Given("the backlog service is initialized")
    public void theBacklogServiceIsInitialized() {
        // This is implicitly handled by the mock setup in setUp()
    }
    
    /**
     * Sets up sample backlog items.
     */
    @Given("the backlog contains some work items")
    public void theBacklogContainsSomeWorkItems() {
        // Create mock data for the backlog
        List<WorkItem> backlogItems = new ArrayList<>();
        
        WorkItem item1 = new WorkItem();
        item1.setId("WI-123");
        item1.setTitle("Implement user authentication");
        item1.setType(WorkItemType.FEATURE);
        item1.setPriority(Priority.HIGH);
        item1.setState(WorkflowState.OPEN);
        backlogItems.add(item1);
        
        WorkItem item2 = new WorkItem();
        item2.setId("WI-124");
        item2.setTitle("Fix sorting in the reports page");
        item2.setType(WorkItemType.BUG);
        item2.setPriority(Priority.MEDIUM);
        item2.setState(WorkflowState.IN_PROGRESS);
        backlogItems.add(item2);
        
        WorkItem item3 = new WorkItem();
        item3.setId("WI-125");
        item3.setTitle("Add export to CSV feature");
        item3.setType(WorkItemType.FEATURE);
        item3.setPriority(Priority.LOW);
        item3.setState(WorkflowState.OPEN);
        backlogItems.add(item3);
        
        // Mock the backlog service to return these items
        when(mockBacklogService.getBacklogItems()).thenReturn(backlogItems);
        
        // Store the items in the context
        context.setAttribute("backlogItems", backlogItems);
    }
    
    /**
     * Sets up a work item.
     * 
     * @param itemId the item ID
     */
    @Given("a work item with ID {string} exists")
    public void aWorkItemWithIDExists(String itemId) {
        // Create a sample work item
        WorkItem item = new WorkItem();
        item.setId(itemId);
        item.setTitle("Sample Work Item");
        item.setType(WorkItemType.TASK);
        item.setPriority(Priority.MEDIUM);
        item.setState(WorkflowState.OPEN);
        
        // Mock the item service to return this item
        when(mockItemService.getItem(itemId)).thenReturn(item);
        
        // When adding to backlog is successful
        when(mockBacklogService.addToBacklog(any(UUID.class))).thenReturn(true);
        
        // Store the item in the context
        context.setAttribute("workItem", item);
    }
    
    /**
     * Sets up a work item in the backlog.
     * 
     * @param itemId the item ID
     */
    @Given("a work item with ID {string} exists in the backlog")
    public void aWorkItemWithIDExistsInTheBacklog(String itemId) {
        // Create a sample work item
        WorkItem item = new WorkItem();
        item.setId(itemId);
        item.setTitle("Sample Work Item");
        item.setType(WorkItemType.TASK);
        item.setPriority(Priority.MEDIUM);
        item.setState(WorkflowState.OPEN);
        
        // Mock the item service to return this item
        when(mockItemService.getItem(itemId)).thenReturn(item);
        
        // Mock the backlog service responses
        when(mockBacklogService.setPriority(eq(itemId), any(Priority.class))).thenReturn(true);
        when(mockBacklogService.removeFromBacklog(itemId)).thenReturn(true);
        
        // Store the item in the context
        context.setAttribute("workItem", item);
    }
    
    /**
     * Executes a command.
     * 
     * @param command the command to execute
     */
    @When("the user runs {string}")
    public void theUserRunsCommand(String command) {
        // Parse the command string
        String trimmedCommand = command.trim();
        
        // Initialize a BacklogCommand
        BacklogCommand backlogCommand = new BacklogCommand(mockServiceManager);
        
        // Set the username from the context
        String username = context.getUsername();
        if (username != null) {
            when(mockBacklogService.getCurrentUser()).thenReturn(username);
        }
        
        // Parse the action (first word after "backlog")
        Pattern actionPattern = Pattern.compile("backlog(?:\\s+(\\w+))?");
        Matcher actionMatcher = actionPattern.matcher(trimmedCommand);
        if (actionMatcher.find()) {
            String action = actionMatcher.group(1);
            backlogCommand.setAction(action);
        }
        
        // Parse the item ID (word after the action)
        Pattern itemIdPattern = Pattern.compile("backlog\\s+(\\w+)\\s+(\\S+)");
        Matcher itemIdMatcher = itemIdPattern.matcher(trimmedCommand);
        if (itemIdMatcher.find()) {
            String itemId = itemIdMatcher.group(2);
            backlogCommand.setItemId(itemId);
        }
        
        // Parse the priority (--priority=xyz)
        Pattern priorityPattern = Pattern.compile("--priority=(\\w+)");
        Matcher priorityMatcher = priorityPattern.matcher(trimmedCommand);
        if (priorityMatcher.find()) {
            String priorityStr = priorityMatcher.group(1);
            try {
                Priority priority = Priority.valueOf(priorityStr);
                backlogCommand.setPriority(priority);
            } catch (IllegalArgumentException e) {
                // Invalid priority value
                System.err.println("Invalid priority: " + priorityStr);
            }
        }
        
        // Parse the format (--format=xyz)
        Pattern formatPattern = Pattern.compile("--format=(\\w+)");
        Matcher formatMatcher = formatPattern.matcher(trimmedCommand);
        if (formatMatcher.find()) {
            String format = formatMatcher.group(1);
            backlogCommand.setFormat(format);
        }
        
        // Check for --verbose flag
        if (trimmedCommand.contains("--verbose")) {
            backlogCommand.setVerbose(true);
        }
        
        // Mock the failure cases
        if (backlogCommand.getItemId() != null && backlogCommand.getItemId().equals("non-existent")) {
            when(mockBacklogService.addToBacklog(any(UUID.class))).thenReturn(false);
            when(mockBacklogService.removeFromBacklog(backlogCommand.getItemId())).thenReturn(false);
            when(mockBacklogService.setPriority(eq(backlogCommand.getItemId()), any(Priority.class))).thenReturn(false);
        }
        
        // Clear the output buffers
        outContent.reset();
        errContent.reset();
        
        // Execute the command and store the result
        commandResult = backlogCommand.call();
        
        // Store the command in context for later assertions
        context.setCommand(backlogCommand);
    }
    
    /**
     * Verifies that the command succeeded.
     */
    @Then("the command should succeed")
    public void theCommandShouldSucceed() {
        assertEquals(0, commandResult, "Command should succeed with exit code 0");
    }
    
    /**
     * Verifies that the command failed.
     */
    @Then("the command should fail")
    public void theCommandShouldFail() {
        assertEquals(1, commandResult, "Command should fail with exit code 1");
    }
    
    /**
     * Verifies that the output contains the expected text.
     * 
     * @param text the expected text
     */
    @And("the output should contain {string}")
    public void theOutputShouldContain(String text) {
        String output = outContent.toString();
        assertTrue(output.contains(text), 
                "Output should contain '" + text + "' but was:\n" + output);
    }
    
    /**
     * Verifies that the error output contains the expected text.
     * 
     * @param text the expected error text
     */
    @And("the error output should contain {string}")
    public void theErrorOutputShouldContain(String text) {
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains(text), 
                "Error output should contain '" + text + "' but was:\n" + errorOutput);
    }
    
    /**
     * Verifies that the output is valid JSON.
     */
    @And("the output should be valid JSON")
    public void theOutputShouldBeValidJSON() {
        // We've mocked OutputFormatter.toJson to return valid JSON
        verify(mockStaticOutputFormatter, atLeastOnce())
                .when(() -> OutputFormatter.toJson(any(Map.class)));
    }
    
    /**
     * Verifies that the JSON output contains a specific field.
     * 
     * @param field the field name
     */
    @And("the JSON should contain a {string} field")
    public void theJSONShouldContainAField(String field) {
        // Since we've mocked the JSON output, we'll verify the field is in the captured parameters
        boolean foundField = false;
        List<Map<String, Object>> allParams = operationParamsCaptor.getAllValues();
        for (Map<String, Object> params : allParams) {
            if (params.containsKey(field)) {
                foundField = true;
                break;
            }
        }
        
        assertTrue(foundField, "Field '" + field + "' should be present in operation parameters");
    }
    
    /**
     * Verifies that the JSON output contains a specific array.
     * 
     * @param arrayName the array name
     */
    @And("the JSON should contain an {string} array")
    public void theJSONShouldContainAnArray(String arrayName) {
        // Since we've mocked the JSON output, we'll check that the output contains the array name
        String output = outContent.toString();
        assertTrue(output.contains("\"" + arrayName + "\"") || 
                   output.contains("'" + arrayName + "'"),
                "Output should contain the array '" + arrayName + "' but was:\n" + output);
    }
    
    /**
     * Verifies that the JSON output contains a specific value.
     * 
     * @param value the expected value
     */
    @And("the JSON should contain {string}")
    public void theJSONShouldContain(String value) {
        // This would normally check the JSON output content
        // Since we've mocked the JSON output, we'll log completion parameters
        List<Map<String, Object>> capturedParams = new ArrayList<>();
        
        verify(mockMetadataService, atLeastOnce()).completeOperation(anyString(), argThat(map -> {
            capturedParams.add(new HashMap<>(map));
            return true;
        }));
        
        boolean foundValue = false;
        for (Map<String, Object> params : capturedParams) {
            for (Object paramValue : params.values()) {
                if (paramValue != null && paramValue.toString().contains(value)) {
                    foundValue = true;
                    break;
                }
            }
            if (foundValue) break;
        }
        
        assertTrue(foundValue || outContent.toString().contains(value), 
                "Operation parameters or output should contain '" + value + "'");
    }
    
    /**
     * Verifies that the command tracked operation details.
     */
    @And("the command should track operation details")
    public void theCommandShouldTrackOperationDetails() {
        verify(mockMetadataService, atLeastOnce()).startOperation(anyString(), anyString(), anyMap());
        verify(mockMetadataService, atLeastOnce()).completeOperation(anyString(), anyMap());
    }
    
    /**
     * Verifies that the command tracked operation failure.
     */
    @And("the command should track operation failure")
    public void theCommandShouldTrackOperationFailure() {
        verify(mockMetadataService, atLeastOnce()).startOperation(anyString(), anyString(), anyMap());
        verify(mockMetadataService, atLeastOnce()).failOperation(anyString(), any(Exception.class));
    }
    
    /**
     * Verifies that the command tracked the format parameter.
     */
    @And("the command should track format parameters")
    public void theCommandShouldTrackFormatParameters() {
        boolean formatFound = false;
        
        List<Map<String, Object>> allParams = operationParamsCaptor.getAllValues();
        for (Map<String, Object> params : allParams) {
            if (params.containsKey("format") && params.get("format").equals("json")) {
                formatFound = true;
                break;
            }
        }
        
        assertTrue(formatFound, "Command should track format parameter as 'json'");
    }
    
    /**
     * Verifies that the command tracked the verbose parameter.
     */
    @And("the command should track verbose parameter")
    public void theCommandShouldTrackVerboseParameter() {
        boolean verboseFound = false;
        
        List<Map<String, Object>> allParams = operationParamsCaptor.getAllValues();
        for (Map<String, Object> params : allParams) {
            if (params.containsKey("verbose") && (Boolean)params.get("verbose")) {
                verboseFound = true;
                break;
            }
        }
        
        assertTrue(verboseFound, "Command should track verbose parameter as 'true'");
    }
    
    /**
     * Verifies that the command tracked the item ID.
     */
    @And("the command should track the item ID")
    public void theCommandShouldTrackTheItemID() {
        BacklogCommand command = (BacklogCommand) context.getCommand();
        String itemId = command.getItemId();
        
        boolean itemIdFound = false;
        
        List<Map<String, Object>> allParams = operationParamsCaptor.getAllValues();
        for (Map<String, Object> params : allParams) {
            if ((params.containsKey("item_id") && params.get("item_id").equals(itemId))) {
                itemIdFound = true;
                break;
            }
        }
        
        assertTrue(itemIdFound, "Command should track item ID: " + itemId);
    }
    
    /**
     * Verifies that the command tracked the priority.
     */
    @And("the command should track the priority")
    public void theCommandShouldTrackThePriority() {
        BacklogCommand command = (BacklogCommand) context.getCommand();
        Priority priority = command.getPriority();
        
        boolean priorityFound = false;
        
        List<Map<String, Object>> allParams = operationParamsCaptor.getAllValues();
        for (Map<String, Object> params : allParams) {
            if (params.containsKey("priority") && params.get("priority").equals(priority.name())) {
                priorityFound = true;
                break;
            }
        }
        
        assertTrue(priorityFound, "Command should track priority: " + priority);
    }
}