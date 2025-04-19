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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.AddCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for testing the AddCommand using BDD.
 */
public class AddCommandSteps {

    private final TestContext context;
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private MockedStatic<ServiceManager> mockedStaticServiceManager;
    private MockedStatic<OutputFormatter> mockedStaticOutputFormatter;
    private int commandResult;
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    private ArgumentCaptor<Object> resultCaptor;
    private WorkItem createdWorkItem;
    private String currentUser;
    private String currentProject;

    /**
     * Creates a new set of AddCommand step definitions using the provided test context.
     * 
     * @param context the test context to use
     */
    public AddCommandSteps(TestContext context) {
        this.context = context;
    }

    @Before
    public void setUp() {
        // Redirect stdout and stderr
        originalOut = System.out;
        originalErr = System.err;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));

        // Set up static mocks
        mockedStaticServiceManager = Mockito.mockStatic(ServiceManager.class);
        mockedStaticServiceManager.when(ServiceManager::getInstance).thenReturn(context.getServiceManager());

        mockedStaticOutputFormatter = Mockito.mockStatic(OutputFormatter.class);
        mockedStaticOutputFormatter.when(() -> OutputFormatter.toJson(any(), anyBoolean()))
            .thenAnswer(invocation -> {
                Map<String, Object> map = (Map<String, Object>) invocation.getArgument(0);
                return "{\"result\":\"" + map.get("result") + "\",\"item_id\":\"" + map.get("item_id") + "\",\"operationId\":\"" + map.get("operationId") + "\"}";
            });

        // Set up argument captors
        paramsCaptor = ArgumentCaptor.forClass(Map.class);
        resultCaptor = ArgumentCaptor.forClass(Object.class);

        // Create a sample work item for the mock to return
        createdWorkItem = new WorkItem();
        createdWorkItem.setId(UUID.randomUUID().toString());
        createdWorkItem.setTitle("Default Title");
        createdWorkItem.setDescription("");
        createdWorkItem.setType(WorkItemType.TASK);
        createdWorkItem.setPriority(Priority.MEDIUM);
        createdWorkItem.setStatus(WorkflowState.CREATED);
        createdWorkItem.setCreated(LocalDateTime.now());
        createdWorkItem.setUpdated(LocalDateTime.now());

        // Set up default mock behavior
        when(context.getMetadataService().startOperation(anyString(), anyString(), any()))
            .thenReturn("mock-operation-id");
        when(context.getMockItemService().createWorkItem(any(WorkItem.class)))
            .thenReturn(createdWorkItem);
        
        // Set default user and project
        currentUser = "testuser";
        currentProject = "default-project";
        when(context.getConfigurationService().getCurrentUser()).thenReturn(currentUser);
        when(context.getConfigurationService().getCurrentProject()).thenReturn(currentProject);
    }

    @After
    public void tearDown() {
        // Restore stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);

        // Close static mocks
        if (mockedStaticServiceManager != null) {
            mockedStaticServiceManager.close();
        }
        if (mockedStaticOutputFormatter != null) {
            mockedStaticOutputFormatter.close();
        }
    }

    @Given("a user {string} with basic permissions")
    public void aUserWithBasicPermissions(String username) {
        currentUser = username;
        when(context.getConfigurationService().getCurrentUser()).thenReturn(username);
    }

    @Given("the current user is {string}")
    public void theCurrentUserIs(String username) {
        currentUser = username;
        when(context.getConfigurationService().getCurrentUser()).thenReturn(username);
    }

    @Given("the current project is {string}")
    public void theCurrentProjectIs(String project) {
        currentProject = project;
        when(context.getConfigurationService().getCurrentProject()).thenReturn(project);
    }

    @When("the user runs {string}")
    public void theUserRuns(String commandLine) {
        // Parse the command line to extract parameters
        Map<String, String> params = parseCommandLine(commandLine);
        
        // Set up the command with extracted parameters
        AddCommand command = new AddCommand(context.getServiceManager());
        
        if (params.containsKey("title")) {
            command.setTitle(params.get("title"));
            
            // Update the mock work item
            createdWorkItem.setTitle(params.get("title"));
        }
        
        if (params.containsKey("description")) {
            command.setDescription(params.get("description"));
            createdWorkItem.setDescription(params.get("description"));
        }
        
        if (params.containsKey("type")) {
            WorkItemType type = WorkItemType.valueOf(params.get("type"));
            command.setType(type);
            createdWorkItem.setType(type);
        }
        
        if (params.containsKey("priority")) {
            Priority priority = Priority.valueOf(params.get("priority"));
            command.setPriority(priority);
            createdWorkItem.setPriority(priority);
        }
        
        if (params.containsKey("assignee")) {
            command.setAssignee(params.get("assignee"));
            createdWorkItem.setAssignee(params.get("assignee"));
        } else {
            createdWorkItem.setAssignee(currentUser);
        }
        
        if (params.containsKey("project")) {
            command.setProject(params.get("project"));
            createdWorkItem.setProject(params.get("project"));
        } else {
            createdWorkItem.setProject(currentProject);
        }
        
        if (params.containsKey("format")) {
            command.setFormat(params.get("format"));
        }
        
        if (params.containsKey("verbose")) {
            command.setVerbose(Boolean.parseBoolean(params.get("verbose")));
        }
        
        createdWorkItem.setReporter(currentUser);
        
        // Execute the command
        commandResult = command.call();
    }

    @When("the user runs {string} without a title")
    public void theUserRunsWithoutTitle(String commandLine) {
        AddCommand command = new AddCommand(context.getServiceManager());
        // Do not set a title
        commandResult = command.call();
    }

    @Then("the command should succeed")
    public void theCommandShouldSucceed() {
        assertEquals(0, commandResult, "Command execution should succeed with exit code 0");
    }

    @Then("the command should fail")
    public void theCommandShouldFail() {
        assertEquals(1, commandResult, "Command execution should fail with exit code 1");
    }

    @Then("the output should contain a work item ID")
    public void theOutputShouldContainAWorkItemID() {
        String output = outContent.toString();
        assertTrue(output.contains(createdWorkItem.getId()), 
            "Output should contain the work item ID: " + createdWorkItem.getId());
    }

    @Then("the output should contain {string}")
    public void theOutputShouldContain(String expectedText) {
        String output = outContent.toString();
        assertTrue(output.contains(expectedText), 
            "Output should contain the text: " + expectedText);
    }

    @Then("the output should be valid JSON")
    public void theOutputShouldBeValidJSON() {
        String output = outContent.toString().trim();
        assertTrue(output.startsWith("{"), "Output should start with {");
        assertTrue(output.endsWith("}"), "Output should end with }");
    }

    @Then("the JSON should contain {string}")
    public void theJSONShouldContain(String expectedText) {
        String output = outContent.toString();
        assertTrue(output.contains(expectedText), 
            "JSON output should contain: " + expectedText);
    }

    @Then("the JSON should contain an item_id field")
    public void theJSONShouldContainAnItemIdField() {
        String output = outContent.toString();
        assertTrue(output.contains("\"item_id\":"), 
            "JSON output should contain an item_id field");
        assertTrue(output.contains(createdWorkItem.getId()), 
            "JSON output should contain the work item ID: " + createdWorkItem.getId());
    }

    @Then("the error output should indicate title is required")
    public void theErrorOutputShouldIndicateTitleIsRequired() {
        String error = errContent.toString();
        assertTrue(error.contains("Title is required"), 
            "Error output should indicate that title is required");
    }

    @Then("the command should track operation details")
    public void theCommandShouldTrackOperationDetails() {
        // Verify startOperation was called with correct parameters
        verify(context.getMetadataService()).startOperation(eq("add"), eq("CREATE"), paramsCaptor.capture());
        
        // Verify completeOperation was called with the work item ID
        verify(context.getMetadataService()).completeOperation(anyString(), any());
    }

    @Then("the command should track operation failure")
    public void theCommandShouldTrackOperationFailure() {
        // Verify startOperation was called
        verify(context.getMetadataService()).startOperation(eq("add"), eq("CREATE"), any());
        
        // Verify failOperation was called with an exception
        verify(context.getMetadataService()).failOperation(anyString(), any(Exception.class));
    }

    @Then("the command should track the type parameter")
    public void theCommandShouldTrackTheTypeParameter() {
        verify(context.getMetadataService()).startOperation(eq("add"), eq("CREATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertNotNull(params.get("type"), "Operation parameters should include type");
    }

    @Then("the command should track the priority parameter")
    public void theCommandShouldTrackThePriorityParameter() {
        verify(context.getMetadataService()).startOperation(eq("add"), eq("CREATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertNotNull(params.get("priority"), "Operation parameters should include priority");
    }

    @Then("the command should track the assignee parameter")
    public void theCommandShouldTrackTheAssigneeParameter() {
        verify(context.getMetadataService()).startOperation(eq("add"), eq("CREATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertNotNull(params.get("assignee"), "Operation parameters should include assignee");
    }

    @Then("the command should track the project parameter")
    public void theCommandShouldTrackTheProjectParameter() {
        verify(context.getMetadataService()).startOperation(eq("add"), eq("CREATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertNotNull(params.get("project"), "Operation parameters should include project");
    }

    @Then("the command should track all parameters")
    public void theCommandShouldTrackAllParameters() {
        verify(context.getMetadataService()).startOperation(eq("add"), eq("CREATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        assertNotNull(params.get("title"), "Operation parameters should include title");
        assertNotNull(params.get("type"), "Operation parameters should include type");
        assertNotNull(params.get("priority"), "Operation parameters should include priority");
        assertNotNull(params.get("assignee"), "Operation parameters should include assignee");
        assertNotNull(params.get("project"), "Operation parameters should include project");
    }

    @Then("the command should track the format parameter")
    public void theCommandShouldTrackTheFormatParameter() {
        verify(context.getMetadataService()).startOperation(eq("add"), eq("CREATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("json", params.get("format"), "Operation parameters should include format=json");
    }

    @Then("the command should track the verbose parameter")
    public void theCommandShouldTrackTheVerboseParameter() {
        verify(context.getMetadataService()).startOperation(eq("add"), eq("CREATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals(true, params.get("verbose"), "Operation parameters should include verbose=true");
    }

    @Then("when verbose output is requested, the reporter should be {string}")
    public void whenVerboseOutputIsRequestedTheReporterShouldBe(String expectedReporter) {
        // First, we need to run the command with verbose output
        AddCommand command = new AddCommand(context.getServiceManager());
        command.setTitle("Reporter test");
        command.setVerbose(true);
        command.call();
        
        String output = outContent.toString();
        assertTrue(output.contains("Reporter: " + expectedReporter), 
            "Verbose output should show reporter as " + expectedReporter);
    }

    /**
     * Helper method to parse command line arguments into a map of parameters.
     * 
     * @param commandLine The command line string to parse
     * @return A map of parameter names to values
     */
    private Map<String, String> parseCommandLine(String commandLine) {
        Map<String, String> params = new java.util.HashMap<>();
        
        // Remove the command name
        String[] parts = commandLine.split(" ", 2);
        if (parts.length < 2) {
            return params;
        }
        
        String argsString = parts[1];
        
        // Pattern to match --parameter=value or --parameter='value with spaces'
        Pattern pattern = Pattern.compile("--([a-zA-Z0-9_-]+)(?:=([^'\"\\s]+)|='([^']*)'|=\"([^\"]*)\")?");
        Matcher matcher = pattern.matcher(argsString);
        
        while (matcher.find()) {
            String name = matcher.group(1);
            String value = null;
            
            // Check which capture group contains the value
            if (matcher.group(2) != null) {
                value = matcher.group(2); // Unquoted value
            } else if (matcher.group(3) != null) {
                value = matcher.group(3); // Single-quoted value
            } else if (matcher.group(4) != null) {
                value = matcher.group(4); // Double-quoted value
            } else {
                // Boolean flag (no value)
                value = "true";
            }
            
            params.put(name, value);
        }
        
        return params;
    }
}