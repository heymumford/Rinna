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
import io.cucumber.datatable.DataTable;
import org.rinna.cli.command.BugCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Step definitions for the Bug Command BDD tests.
 */
public class BugCommandSteps {

    private TestContext context;
    private MockItemService mockItemService;
    private ConfigurationService mockConfigService;
    private MetadataService mockMetadataService;
    private ServiceManager mockServiceManager;
    private ProjectContext mockProjectContext;
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private BugCommand bugCommand;
    private int exitCode;
    
    private static final String TEST_OPERATION_ID = "test-operation-id";
    private static final String TEST_BUG_ID = "BUG-123";
    private static final String TEST_PROJECT_KEY = "TEST";

    public BugCommandSteps(TestContext context) {
        this.context = context;
    }

    @Before
    public void setup() {
        // Capture stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mocks
        mockItemService = mock(MockItemService.class);
        mockConfigService = mock(ConfigurationService.class);
        mockMetadataService = mock(MetadataService.class);
        mockServiceManager = mock(ServiceManager.class);
        mockProjectContext = mock(ProjectContext.class);
        
        // Configure mock service manager
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getProjectContext()).thenReturn(mockProjectContext);
        
        // Set up operation ID for metadata service
        when(mockMetadataService.startOperation(eq("bug"), eq("CREATE"), any())).thenReturn(TEST_OPERATION_ID);
        
        // Set up default project context
        when(mockProjectContext.getCurrentProject()).thenReturn(TEST_PROJECT_KEY);
        
        // Set up default config values
        when(mockConfigService.getDefaultVersion()).thenReturn("1.0.0");
        when(mockConfigService.getAutoAssignBugs()).thenReturn(true);
        when(mockConfigService.getDefaultBugAssignee()).thenReturn(null); // Use current user by default
        
        // Set up item service to return a sample bug when createItem is called
        when(mockItemService.createItem(any(WorkItem.class))).thenAnswer(invocation -> {
            WorkItem item = invocation.getArgument(0);
            item.setId(TEST_BUG_ID);
            return item;
        });
        
        // Mock ServiceManager.getInstance
        mockStatic(ServiceManager.class);
        when(ServiceManager.getInstance()).thenReturn(mockServiceManager);
        
        // Add mocks to test context
        context.set("mockItemService", mockItemService);
        context.set("mockConfigService", mockConfigService);
        context.set("mockMetadataService", mockMetadataService);
        context.set("mockServiceManager", mockServiceManager);
        context.set("mockProjectContext", mockProjectContext);
    }

    @After
    public void tearDown() {
        // Restore stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
        
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
        // Set current user in context
        context.set("currentUser", System.getProperty("user.name"));
    }

    @Given("a project context is set")
    public void aProjectContextIsSet() {
        // Already set up in @Before
        context.set("projectKey", TEST_PROJECT_KEY);
    }

    @Given("the default bug assignee is set to {string}")
    public void theDefaultBugAssigneeIsSetTo(String assignee) {
        when(mockConfigService.getDefaultBugAssignee()).thenReturn(assignee);
    }

    @Given("the item service will throw an exception")
    public void theItemServiceWillThrowAnException() {
        RuntimeException testException = new RuntimeException("Test service exception");
        when(mockItemService.createItem(any(WorkItem.class))).thenThrow(testException);
        context.set("testException", testException);
    }

    @When("the user runs the {string} command with title {string}")
    public void theUserRunsTheCommandWithTitle(String command, String title) {
        bugCommand = new BugCommand(mockServiceManager);
        bugCommand.setTitle(title);
        
        // Execute the command
        exitCode = bugCommand.call();
        
        // Store results in context
        context.set("bugCommand", bugCommand);
        context.set("exitCode", exitCode);
    }

    @When("the user runs the {string} command with the following parameters:")
    public void theUserRunsTheCommandWithTheFollowingParameters(String command, DataTable dataTable) {
        // Convert DataTable to map of parameters
        Map<String, String> params = dataTable.asMap();
        
        bugCommand = new BugCommand(mockServiceManager);
        
        // Set parameters from the table
        if (params.containsKey("title")) {
            bugCommand.setTitle(params.get("title"));
        }
        
        if (params.containsKey("description")) {
            bugCommand.setDescription(params.get("description"));
        }
        
        if (params.containsKey("priority")) {
            bugCommand.setPriority(Priority.valueOf(params.get("priority")));
        }
        
        if (params.containsKey("assignee")) {
            bugCommand.setAssignee(params.get("assignee"));
        }
        
        if (params.containsKey("version")) {
            bugCommand.setVersion(params.get("version"));
        }
        
        // Execute the command
        exitCode = bugCommand.call();
        
        // Store results in context
        context.set("bugCommand", bugCommand);
        context.set("exitCode", exitCode);
        context.set("params", params);
    }

    @When("the user runs the {string} command with title {string} and priority {string}")
    public void theUserRunsTheCommandWithTitleAndPriority(String command, String title, String priority) {
        bugCommand = new BugCommand(mockServiceManager);
        bugCommand.setTitle(title);
        bugCommand.setPriority(Priority.valueOf(priority));
        
        // Execute the command
        exitCode = bugCommand.call();
        
        // Store results in context
        context.set("bugCommand", bugCommand);
        context.set("exitCode", exitCode);
    }

    @When("the user runs the {string} command with title {string} and version {string}")
    public void theUserRunsTheCommandWithTitleAndVersion(String command, String title, String version) {
        bugCommand = new BugCommand(mockServiceManager);
        bugCommand.setTitle(title);
        bugCommand.setVersion(version);
        
        // Execute the command
        exitCode = bugCommand.call();
        
        // Store results in context
        context.set("bugCommand", bugCommand);
        context.set("exitCode", exitCode);
    }

    @When("the user runs the {string} command with title {string} and format {string}")
    public void theUserRunsTheCommandWithTitleAndFormat(String command, String title, String format) {
        bugCommand = new BugCommand(mockServiceManager);
        bugCommand.setTitle(title);
        bugCommand.setFormat(format);
        
        // Execute the command
        exitCode = bugCommand.call();
        
        // Store results in context
        context.set("bugCommand", bugCommand);
        context.set("exitCode", exitCode);
    }

    @When("the user runs the {string} command with title {string} and description {string} and verbose mode")
    public void theUserRunsTheCommandWithTitleAndDescriptionAndVerboseMode(String command, String title, String description) {
        bugCommand = new BugCommand(mockServiceManager);
        bugCommand.setTitle(title);
        bugCommand.setDescription(description);
        bugCommand.setVerbose(true);
        
        // Execute the command
        exitCode = bugCommand.call();
        
        // Store results in context
        context.set("bugCommand", bugCommand);
        context.set("exitCode", exitCode);
    }

    @When("the user runs the {string} command without a title")
    public void theUserRunsTheCommandWithoutATitle(String command) {
        bugCommand = new BugCommand(mockServiceManager);
        // No title set
        
        // Execute the command
        exitCode = bugCommand.call();
        
        // Store results in context
        context.set("bugCommand", bugCommand);
        context.set("exitCode", exitCode);
    }

    @When("the user runs the {string} command with an empty title")
    public void theUserRunsTheCommandWithAnEmptyTitle(String command) {
        bugCommand = new BugCommand(mockServiceManager);
        bugCommand.setTitle(""); // Empty title
        
        // Execute the command
        exitCode = bugCommand.call();
        
        // Store results in context
        context.set("bugCommand", bugCommand);
        context.set("exitCode", exitCode);
    }

    @When("the user runs the {string} command with title {string} and verbose mode")
    public void theUserRunsTheCommandWithTitleAndVerboseMode(String command, String title) {
        bugCommand = new BugCommand(mockServiceManager);
        bugCommand.setTitle(title);
        bugCommand.setVerbose(true);
        
        // Execute the command
        exitCode = bugCommand.call();
        
        // Store results in context
        context.set("bugCommand", bugCommand);
        context.set("exitCode", exitCode);
    }

    @Then("a bug should be created successfully")
    public void aBugShouldBeCreatedSuccessfully() {
        assertEquals(0, exitCode, "Command should exit with code 0");
        verify(mockItemService).createItem(any(WorkItem.class));
    }

    @Then("the bug should have the title {string}")
    public void theBugShouldHaveTheTitle(String title) {
        // Capture the work item that was created
        verify(mockItemService).createItem(argThat(workItem -> 
            title.equals(workItem.getTitle())
        ));
    }

    @Then("the bug should have the default priority {string}")
    public void theBugShouldHaveTheDefaultPriority(String priority) {
        // Capture the work item that was created
        verify(mockItemService).createItem(argThat(workItem -> 
            Priority.valueOf(priority).equals(workItem.getPriority())
        ));
    }

    @Then("the bug should have CREATED status")
    public void theBugShouldHaveCreatedStatus() {
        // Capture the work item that was created
        verify(mockItemService).createItem(argThat(workItem -> 
            WorkflowState.CREATED.equals(workItem.getStatus())
        ));
    }

    @Then("the bug should have the description {string}")
    public void theBugShouldHaveTheDescription(String description) {
        // Capture the work item that was created
        verify(mockItemService).createItem(argThat(workItem -> 
            description.equals(workItem.getDescription())
        ));
    }

    @Then("the bug should have the priority {string}")
    public void theBugShouldHaveThePriority(String priority) {
        // Capture the work item that was created
        verify(mockItemService).createItem(argThat(workItem -> 
            Priority.valueOf(priority).equals(workItem.getPriority())
        ));
    }

    @Then("the bug should have the assignee {string}")
    public void theBugShouldHaveTheAssignee(String assignee) {
        // Capture the work item that was created
        verify(mockItemService).createItem(argThat(workItem -> 
            assignee.equals(workItem.getAssignee())
        ));
    }

    @Then("the bug should be assigned to the current user")
    public void theBugShouldBeAssignedToTheCurrentUser() {
        String currentUser = System.getProperty("user.name");
        
        // Capture the work item that was created
        verify(mockItemService).createItem(argThat(workItem -> 
            currentUser.equals(workItem.getAssignee())
        ));
    }

    @Then("the bug should have the version {string}")
    public void theBugShouldHaveTheVersion(String version) {
        // Capture the work item that was created
        verify(mockItemService).createItem(argThat(workItem -> 
            version.equals(workItem.getVersion())
        ));
    }

    @Then("the bug should have the default version from configuration")
    public void theBugShouldHaveTheDefaultVersionFromConfiguration() {
        String defaultVersion = "1.0.0"; // Should match what was set up in @Before
        
        // Capture the work item that was created
        verify(mockItemService).createItem(argThat(workItem -> 
            defaultVersion.equals(workItem.getVersion())
        ));
    }

    @Then("the output should be in JSON format")
    public void theOutputShouldBeInJsonFormat() {
        String output = outputCaptor.toString();
        assertTrue(output.contains("\"id\":"), "Output should contain JSON id field");
        assertTrue(output.contains("\"title\":"), "Output should contain JSON title field");
        assertTrue(output.contains("\"type\": \"BUG\""), "Output should contain JSON type field");
    }

    @Then("the JSON should contain the bug details")
    public void theJsonShouldContainTheBugDetails() {
        String output = outputCaptor.toString();
        assertTrue(output.contains("\"id\": \"" + TEST_BUG_ID + "\""), "JSON should contain bug ID");
        assertTrue(output.contains("\"project\": \"" + TEST_PROJECT_KEY + "\""), "JSON should contain project key");
        assertTrue(output.contains("\"status\": \"CREATED\""), "JSON should contain status");
    }

    @Then("the output should include the description")
    public void theOutputShouldIncludeTheDescription() {
        String output = outputCaptor.toString();
        assertTrue(output.contains("Description:"), "Output should include description section");
        assertTrue(output.contains("Detailed description"), "Output should include the description text");
    }

    @Then("an operation should be tracked with type {string} and action {string}")
    public void anOperationShouldBeTrackedWithTypeAndAction(String type, String action) {
        verify(mockMetadataService).startOperation(eq(type), eq(action), any());
        verify(mockMetadataService).completeOperation(eq(TEST_OPERATION_ID), any());
    }

    @Then("an operation should be tracked with type {string} and action {string} and failed status")
    public void anOperationShouldBeTrackedWithTypeAndActionAndFailedStatus(String type, String action) {
        verify(mockMetadataService).startOperation(eq(type), eq(action), any());
        verify(mockMetadataService).failOperation(eq(TEST_OPERATION_ID), any());
    }

    @Then("the command should fail with an error message about missing title")
    public void theCommandShouldFailWithAnErrorMessageAboutMissingTitle() {
        assertEquals(1, exitCode, "Command should exit with code 1");
        String errorOutput = errorCaptor.toString();
        assertTrue(errorOutput.contains("Error: Bug title is required"), "Error should indicate missing title");
    }

    @Then("no bug should be created")
    public void noBugShouldBeCreated() {
        verify(mockItemService, never()).createItem(any(WorkItem.class));
    }

    @Then("the command should fail with an error message about creating bug")
    public void theCommandShouldFailWithAnErrorMessageAboutCreatingBug() {
        assertEquals(1, exitCode, "Command should exit with code 1");
        String errorOutput = errorCaptor.toString();
        assertTrue(errorOutput.contains("Error creating bug:"), "Error should indicate bug creation issue");
    }

    @Then("the error output should include a stack trace")
    public void theErrorOutputShouldIncludeAStackTrace() {
        String errorOutput = errorCaptor.toString();
        assertTrue(errorOutput.contains("at org.rinna"), "Output should contain stack trace elements");
    }
}