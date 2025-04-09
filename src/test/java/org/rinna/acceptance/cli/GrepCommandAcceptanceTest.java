/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.acceptance.cli;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.junit.jupiter.api.extension.ExtendWith;
import org.rinna.acceptance.base.AcceptanceTestRunner;
import org.rinna.base.AcceptanceTest;
import org.rinna.bdd.TestContext;
import org.rinna.cli.RinnaCli;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.service.DefaultSearchService;
import org.rinna.service.InMemoryItemService;
import org.rinna.service.ItemServiceFactory;
import org.rinna.service.SearchServiceFactory;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Acceptance tests for the GrepCommand using Cucumber BDD.
 * These tests verify that the command behaves as expected from an end-user perspective.
 */
@ExtendWith(AcceptanceTestRunner.class)
public class GrepCommandAcceptanceTest extends AcceptanceTest {
    
    private TestContext context;
    private InMemoryItemService itemService;
    private DefaultSearchService searchService;
    private RinnaCli cli;
    
    private ByteArrayOutputStream outputCaptor;
    private ByteArrayOutputStream errorCaptor;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    private int lastExitCode;
    
    public GrepCommandAcceptanceTest(TestContext context) {
        this.context = context;
    }
    
    @Before
    public void setUp() {
        // Set up output capturing
        outputCaptor = new ByteArrayOutputStream();
        errorCaptor = new ByteArrayOutputStream();
        originalOut = System.out;
        originalErr = System.err;
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Create services
        itemService = (InMemoryItemService) ItemServiceFactory.createItemService();
        searchService = (DefaultSearchService) SearchServiceFactory.createSearchService();
        
        // Register services
        ServiceRegistry.registerService(ItemService.class, itemService);
        ServiceRegistry.registerService(SearchService.class, searchService);
        
        // Create CLI
        cli = new RinnaCli();
        
        // Initialize test data
        context.resetWorkItems();
    }
    
    @After
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Given("I have the following work items")
    public void iHaveTheFollowingWorkItems(List<Map<String, String>> workItems) {
        for (Map<String, String> workItem : workItems) {
            String id = workItem.get("ID");
            String title = workItem.get("Title");
            String description = workItem.getOrDefault("Description", "Description for " + title);
            String typeStr = workItem.getOrDefault("Type", "TASK");
            String priorityStr = workItem.getOrDefault("Priority", "MEDIUM");
            String state = workItem.getOrDefault("State", "READY");
            String assignee = workItem.getOrDefault("Assignee", "developer");
            
            WorkItemType type = WorkItemType.valueOf(typeStr);
            Priority priority = Priority.valueOf(priorityStr);
            
            UUID itemId = itemService.createWorkItem(title, description, type, priority, assignee);
            
            // Update state if different from default
            if (!state.equals("READY")) {
                itemService.updateState(itemId, WorkflowState.valueOf(state), "system");
            }
            
            // Store the ID in the context for later reference
            context.registerWorkItem(id, itemId);
        }
    }
    
    @When("I run {string}")
    public void iRun(String command) {
        // Split the command into parts
        String[] parts = command.split("\\s+");
        
        // Execute the command (skip "rin" if present)
        if (parts[0].equals("rin")) {
            String[] subCommand = Arrays.copyOfRange(parts, 1, parts.length);
            lastExitCode = cli.execute(subCommand);
        } else {
            lastExitCode = cli.execute(parts);
        }
    }
    
    @Then("the command should succeed")
    public void theCommandShouldSucceed() {
        assertEquals(0, lastExitCode, "Command should succeed with exit code 0");
        assertEquals("", errorCaptor.toString().trim(), "Error output should be empty for successful command");
    }
    
    @Then("the command should fail")
    public void theCommandShouldFail() {
        assertEquals(1, lastExitCode, "Command should fail with exit code 1");
        assertFalse(errorCaptor.toString().trim().isEmpty(), "Error output should contain error message");
    }
    
    @Then("I should see work item {int} in the results")
    public void iShouldSeeWorkItemInTheResults(int id) {
        String idStr = Integer.toString(id);
        UUID workItemId = context.getWorkItemId(idStr);
        
        assertNotNull(workItemId, "Work item ID should be registered in context");
        
        String output = outputCaptor.toString();
        assertTrue(output.contains(workItemId.toString()), 
                "Output should contain work item ID " + id);
    }
    
    @Then("I should not see work item {int} in the results")
    public void iShouldNotSeeWorkItemInTheResults(int id) {
        String idStr = Integer.toString(id);
        UUID workItemId = context.getWorkItemId(idStr);
        
        assertNotNull(workItemId, "Work item ID should be registered in context");
        
        String output = outputCaptor.toString();
        assertFalse(output.contains(workItemId.toString()), 
                "Output should not contain work item ID " + id);
    }
    
    @Then("both {string} and {string} matches should be highlighted")
    public void bothMatchesShouldBeHighlighted(String match1, String match2) {
        String output = outputCaptor.toString();
        
        // ANSI color codes for highlighting
        String highlightPrefix = "\u001B[1;31m";
        String highlightSuffix = "\u001B[0m";
        
        assertTrue(output.contains(highlightPrefix + match1 + highlightSuffix) || 
                output.contains(highlightPrefix + match2 + highlightSuffix),
                "Output should highlight matches regardless of case");
    }
    
    @Then("I should see {int} total matches")
    public void iShouldSeeTotalMatches(int count) {
        String output = outputCaptor.toString();
        assertTrue(output.contains("Total matches: " + count) || 
                output.contains("Found " + count + " matches"),
                "Output should indicate " + count + " total matches");
    }
    
    @Then("I should see an error message {string}")
    public void iShouldSeeAnErrorMessage(String message) {
        String errorOutput = errorCaptor.toString();
        assertTrue(errorOutput.contains(message), 
                "Error output should contain message: " + message);
    }
    
    @Then("the command injection should not succeed")
    public void theCommandInjectionShouldNotSucceed() {
        String output = outputCaptor.toString();
        String errorOutput = errorCaptor.toString();
        
        // Check for common command injection success indicators
        assertFalse(output.contains("HACKED"), 
                "Output should not contain evidence of successful command injection");
        assertFalse(output.contains("/etc/passwd"), 
                "Output should not contain sensitive file contents");
        
        // Should either succeed with normal grep operation or fail with invalid input error
        if (lastExitCode == 0) {
            assertTrue(output.contains("matches found") || output.contains("No matches found"),
                    "Output should indicate normal grep operation");
        } else {
            assertTrue(errorOutput.contains("Error") || errorOutput.contains("Invalid"),
                    "Error output should indicate input validation failure");
        }
    }
    
    @Then("I should see lines including {string}")
    public void iShouldSeeLinesIncluding(String term) {
        String output = outputCaptor.toString();
        assertTrue(output.contains(term), 
                "Output should contain the contextual term: " + term);
    }
    
    @Then("I should see my search history including {string}")
    public void iShouldSeeMySearchHistoryIncluding(String term) {
        String output = outputCaptor.toString();
        assertTrue(output.contains("Search History") && output.contains(term), 
                "Output should contain search history with term: " + term);
    }
}