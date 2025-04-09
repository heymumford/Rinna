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
import io.cucumber.java.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.rinna.cli.command.CatCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Step definitions for cat command BDD tests.
 */
public class CatCommandSteps {
    
    private final TestContext testContext;
    private CatCommand catCommand;
    private String commandOutput;
    private String errorOutput;
    private int exitCode;
    
    // Capture console output
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    // Test work items
    private final Map<String, WorkItem> workItems = new HashMap<>();
    
    public CatCommandSteps(TestContext testContext) {
        this.testContext = testContext;
    }
    
    @Before
    public void setup() {
        // Redirect console output
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mocks
        if (testContext.getMockServiceManager() == null) {
            ServiceManager mockServiceManager = mock(ServiceManager.class);
            testContext.setMockServiceManager(mockServiceManager);
            
            // Setup mock services
            MockItemService mockItemService = mock(MockItemService.class);
            testContext.setMockItemService(mockItemService);
            
            ConfigurationService mockConfigService = mock(ConfigurationService.class);
            when(mockConfigService.getCurrentUser()).thenReturn("test.user");
            testContext.setMockConfigService(mockConfigService);
            
            MetadataService mockMetadataService = mock(MetadataService.class);
            when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn("test-operation-id");
            testContext.setMockMetadataService(mockMetadataService);
            
            MockHistoryService mockHistoryService = mock(MockHistoryService.class);
            testContext.setMockHistoryService(mockHistoryService);
            
            ContextManager mockContextManager = mock(ContextManager.class);
            testContext.setMockContextManager(mockContextManager);
            
            // Configure service manager
            when(mockServiceManager.getItemService()).thenReturn(mockItemService);
            when(mockServiceManager.getMockHistoryService()).thenReturn(mockHistoryService);
            when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
            when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
            
            // Mock ContextManager.getInstance()
            testContext.mockContextManager();
        }
    }
    
    @After
    public void tearDown() {
        // Restore console output
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Reset output capture
        outputCaptor.reset();
        errorCaptor.reset();
    }
    
    @Given("the user is logged in")
    public void userIsLoggedIn() {
        when(testContext.getMockConfigService().getCurrentUser()).thenReturn("test.user");
    }
    
    @Given("there is a work item with ID {string} and title {string}")
    public void thereIsWorkItemWithIdAndTitle(String id, String title) {
        WorkItem workItem = new WorkItem();
        workItem.setId(id);
        workItem.setTitle(title);
        workItem.setCreated(LocalDateTime.now());
        workItem.setUpdated(LocalDateTime.now());
        
        workItems.put(id, workItem);
        when(testContext.getMockItemService().getItem(id)).thenReturn(workItem);
    }
    
    @Given("the work item {string} has description {string}")
    public void workItemHasDescription(String id, String description) {
        WorkItem workItem = workItems.get(id);
        if (workItem != null) {
            workItem.setDescription(description);
        }
    }
    
    @Given("the work item {string} has type {string}, state {string}, and priority {string}")
    public void workItemHasTypeStateAndPriority(String id, String type, String state, String priority) {
        WorkItem workItem = workItems.get(id);
        if (workItem != null) {
            workItem.setType(WorkItemType.valueOf(type));
            workItem.setState(WorkflowState.valueOf(state));
            workItem.setPriority(Priority.valueOf(priority));
        }
    }
    
    @Given("the work item {string} has history entries")
    public void workItemHasHistoryEntries(String id, DataTable dataTable) {
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        List<MockHistoryService.HistoryEntryRecord> historyEntries = new ArrayList<>();
        
        for (Map<String, String> row : rows) {
            MockHistoryService.HistoryEntryRecord entry = new MockHistoryService.HistoryEntryRecord(
                UUID.randomUUID(),
                UUID.fromString(id),
                row.get("type"),
                row.get("user"),
                row.get("content"),
                new Date()
            );
            historyEntries.add(entry);
        }
        
        when(testContext.getMockHistoryService().getHistory(UUID.fromString(id))).thenReturn(historyEntries);
    }
    
    @Given("the user has previously viewed work item {string}")
    public void userHasPreviouslyViewedWorkItem(String id) {
        when(testContext.getMockContextManager().getLastViewedWorkItem()).thenReturn(UUID.fromString(id));
    }
    
    @Given("the work item {string} has description with special formatting {string}")
    public void workItemHasDescriptionWithSpecialFormatting(String id, String formattedText) {
        WorkItem workItem = workItems.get(id);
        if (workItem != null) {
            workItem.setDescription(formattedText);
        }
    }
    
    @Given("no work item has been previously viewed")
    public void noWorkItemHasBeenPreviouslyViewed() {
        when(testContext.getMockContextManager().getLastViewedWorkItem()).thenReturn(null);
    }
    
    @When("the user runs {string} command")
    public void userRunsCommand(String command) {
        catCommand = new CatCommand(testContext.getMockServiceManager());
        
        // Parse the command options
        String[] commandParts = command.split("\\s+");
        if (commandParts.length > 1) {
            // First part is "cat", rest are arguments
            String id = null;
            for (int i = 1; i < commandParts.length; i++) {
                String arg = commandParts[i];
                if (arg.startsWith("--")) {
                    // Handle options
                    String option = arg.substring(2);
                    if (option.startsWith("format=")) {
                        catCommand.setFormat(option.substring(7));
                    } else if (option.equals("show-line-numbers")) {
                        catCommand.setShowLineNumbers(true);
                    } else if (option.equals("show-all-formatting")) {
                        catCommand.setShowAllFormatting(true);
                    } else if (option.equals("show-history")) {
                        catCommand.setShowHistory(true);
                    } else if (option.equals("show-relationships")) {
                        catCommand.setShowRelationships(true);
                    } else if (option.equals("verbose")) {
                        catCommand.setVerbose(true);
                    }
                } else if (!arg.startsWith("-")) {
                    // Assume it's the ID
                    id = arg;
                }
            }
            
            if (id != null) {
                catCommand.setItemId(id);
            }
        }
        
        // Execute the command
        try {
            exitCode = catCommand.call();
            commandOutput = outputCaptor.toString();
            errorOutput = errorCaptor.toString();
        } catch (Exception e) {
            exitCode = 1;
            errorOutput = e.getMessage();
        }
    }
    
    @When("the user runs {string} command without ID")
    public void userRunsCommandWithoutId(String command) {
        catCommand = new CatCommand(testContext.getMockServiceManager());
        
        // Parse the command options (exclude ID)
        String[] commandParts = command.split("\\s+");
        for (int i = 1; i < commandParts.length; i++) {
            String arg = commandParts[i];
            if (arg.startsWith("--")) {
                // Handle options
                String option = arg.substring(2);
                if (option.startsWith("format=")) {
                    catCommand.setFormat(option.substring(7));
                } else if (option.equals("show-line-numbers")) {
                    catCommand.setShowLineNumbers(true);
                } else if (option.equals("show-all-formatting")) {
                    catCommand.setShowAllFormatting(true);
                } else if (option.equals("show-history")) {
                    catCommand.setShowHistory(true);
                } else if (option.equals("show-relationships")) {
                    catCommand.setShowRelationships(true);
                } else if (option.equals("verbose")) {
                    catCommand.setVerbose(true);
                }
            }
        }
        
        // Execute the command
        try {
            exitCode = catCommand.call();
            commandOutput = outputCaptor.toString();
            errorOutput = errorCaptor.toString();
        } catch (Exception e) {
            exitCode = 1;
            errorOutput = e.getMessage();
        }
    }
    
    @When("the user runs {string} command for non-existent work item")
    public void userRunsCommandForNonExistentWorkItem(String command) {
        catCommand = new CatCommand(testContext.getMockServiceManager());
        
        // Extract the ID from the command
        String[] commandParts = command.split("\\s+");
        if (commandParts.length > 1) {
            String id = commandParts[1];
            catCommand.setItemId(id);
            when(testContext.getMockItemService().getItem(id)).thenReturn(null);
        }
        
        // Execute the command
        try {
            exitCode = catCommand.call();
            commandOutput = outputCaptor.toString();
            errorOutput = errorCaptor.toString();
        } catch (Exception e) {
            exitCode = 1;
            errorOutput = e.getMessage();
        }
    }
    
    @Then("the command should succeed")
    public void commandShouldSucceed() {
        assertEquals(0, exitCode, "Command should succeed with exit code 0");
    }
    
    @Then("the command should fail")
    public void commandShouldFail() {
        assertEquals(1, exitCode, "Command should fail with exit code 1");
    }
    
    @Then("the output should contain {string}")
    public void outputShouldContain(String text) {
        assertTrue(commandOutput.contains(text),
                  "Output should contain '" + text + "', but was: " + commandOutput);
    }
    
    @Then("the error output should contain {string}")
    public void errorOutputShouldContain(String text) {
        assertTrue(errorOutput.contains(text),
                  "Error output should contain '" + text + "', but was: " + errorOutput);
    }
    
    @Then("the output should contain line numbers before content")
    public void outputShouldContainLineNumbersBeforeContent() {
        assertTrue(commandOutput.contains("1  Title:"),
                  "Output should contain line numbers before content");
    }
    
    @Then("the output should contain tab markers and line ending markers")
    public void outputShouldContainTabMarkersAndLineEndingMarkers() {
        assertTrue(commandOutput.contains("→   "),
                  "Output should contain tab markers (→)");
        assertTrue(commandOutput.contains("¶"),
                  "Output should contain line ending markers (¶)");
    }
    
    @Then("the output should be in JSON format")
    public void outputShouldBeInJsonFormat() {
        String trimmedOutput = commandOutput.trim();
        assertTrue(trimmedOutput.startsWith("{") && trimmedOutput.endsWith("}"),
                  "Output should be in JSON format");
    }
    
    @Then("the JSON output should contain {string}, {string}, and {string} fields")
    public void jsonOutputShouldContainFields(String field1, String field2, String field3) {
        assertAll(
            () -> assertTrue(commandOutput.contains("\"" + field1 + "\":"),
                           "JSON output should contain '" + field1 + "' field"),
            () -> assertTrue(commandOutput.contains("\"" + field2 + "\":"),
                           "JSON output should contain '" + field2 + "' field"),
            () -> assertTrue(commandOutput.contains("\"" + field3 + "\":"),
                           "JSON output should contain '" + field3 + "' field")
        );
    }
    
    @Then("the JSON output should contain a {string} array")
    public void jsonOutputShouldContainArray(String arrayName) {
        assertTrue(commandOutput.contains("\"" + arrayName + "\": ["),
                  "JSON output should contain '" + arrayName + "' array");
    }
    
    @Then("each history entry should have {string}, {string}, {string}, and {string} fields")
    public void eachHistoryEntryShouldHaveFields(String field1, String field2, String field3, String field4) {
        assertAll(
            () -> assertTrue(commandOutput.contains("\"" + field1 + "\":"),
                           "History entry should contain '" + field1 + "' field"),
            () -> assertTrue(commandOutput.contains("\"" + field2 + "\":"),
                           "History entry should contain '" + field2 + "' field"),
            () -> assertTrue(commandOutput.contains("\"" + field3 + "\":"),
                           "History entry should contain '" + field3 + "' field"),
            () -> assertTrue(commandOutput.contains("\"" + field4 + "\":"),
                           "History entry should contain '" + field4 + "' field")
        );
    }
}