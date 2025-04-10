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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.rinna.cli.command.GrepCommand;
import org.rinna.cli.domain.model.SearchResult;
import org.rinna.cli.domain.model.SearchResult.Match;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockSearchService;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for the grep command feature.
 */
public class GrepCommandSteps {
    
    private final TestContext testContext = TestContext.getInstance();
    private final MockItemService mockItemService;
    private final MockSearchService mockSearchService;
    
    public GrepCommandSteps() {
        mockItemService = testContext.getMockItemService();
        mockSearchService = testContext.getMockSearchService();
    }
    
    @Given("the system has work items with the following details:")
    public void theSystemHasWorkItemsWithTheFollowingDetails(DataTable dataTable) {
        List<Map<String, String>> workItems = dataTable.asMaps();
        
        // Clear existing items
        mockItemService.clearItems();
        mockSearchService.clearResults();
        
        for (Map<String, String> row : workItems) {
            // Create work item
            WorkItem item = new WorkItem();
            item.setId(row.get("ID"));
            item.setTitle(row.get("Title"));
            item.setDescription(row.get("Description"));
            item.setType(WorkItemType.valueOf(row.get("Type")));
            item.setPriority(Priority.valueOf(row.get("Priority")));
            item.setState(WorkflowState.valueOf(row.get("Status")));
            item.setAssignee(row.get("Assignee"));
            
            // Add to mock service
            mockItemService.addItem(item);
            
            // Create search results that will match when searched for relevant terms
            createSearchResultsForItem(item);
        }
    }
    
    @When("I run the grep command with pattern {string}")
    public void iRunTheGrepCommandWithPattern(String pattern) {
        GrepCommand command = new GrepCommand();
        command.setPattern(pattern);
        
        // Execute command and capture output
        testContext.resetCapturedOutput();
        int exitCode = command.call();
        
        // Store results
        testContext.setLastCommandOutput(testContext.getStandardOutput());
        testContext.setLastCommandExitCode(exitCode);
    }
    
    @When("I run the grep command with pattern {string} and case-sensitive option")
    public void iRunTheGrepCommandWithPatternAndCaseSensitiveOption(String pattern) {
        GrepCommand command = new GrepCommand();
        command.setPattern(pattern);
        command.setCaseSensitive(true);
        
        // Execute command and capture output
        testContext.resetCapturedOutput();
        int exitCode = command.call();
        
        // Store results
        testContext.setLastCommandOutput(testContext.getStandardOutput());
        testContext.setLastCommandExitCode(exitCode);
    }
    
    @When("I run the grep command with pattern {string} and format {string}")
    public void iRunTheGrepCommandWithPatternAndFormat(String pattern, String format) {
        GrepCommand command = new GrepCommand();
        command.setPattern(pattern);
        command.setOutputFormat(format);
        
        // Execute command and capture output
        testContext.resetCapturedOutput();
        int exitCode = command.call();
        
        // Store results
        testContext.setLastCommandOutput(testContext.getStandardOutput());
        testContext.setLastCommandExitCode(exitCode);
    }
    
    @When("I run the grep command with pattern {string} and count-only option")
    public void iRunTheGrepCommandWithPatternAndCountOnlyOption(String pattern) {
        GrepCommand command = new GrepCommand();
        command.setPattern(pattern);
        command.setCountOnly(true);
        
        // Execute command and capture output
        testContext.resetCapturedOutput();
        int exitCode = command.call();
        
        // Store results
        testContext.setLastCommandOutput(testContext.getStandardOutput());
        testContext.setLastCommandExitCode(exitCode);
    }
    
    @When("I run the grep command with pattern {string} and context {int}")
    public void iRunTheGrepCommandWithPatternAndContext(String pattern, int context) {
        GrepCommand command = new GrepCommand();
        command.setPattern(pattern);
        command.setContext(context);
        
        // Execute command and capture output
        testContext.resetCapturedOutput();
        int exitCode = command.call();
        
        // Store results
        testContext.setLastCommandOutput(testContext.getStandardOutput());
        testContext.setLastCommandExitCode(exitCode);
    }
    
    @When("I run the grep command with an empty pattern")
    public void iRunTheGrepCommandWithAnEmptyPattern() {
        GrepCommand command = new GrepCommand();
        command.setPattern("");
        
        // Execute command and capture output
        testContext.resetCapturedOutput();
        int exitCode = command.call();
        
        // Store results
        testContext.setLastCommandOutput(testContext.getStandardOutput());
        testContext.setLastCommandExitCode(exitCode);
    }
    
    @Then("the command should execute successfully")
    public void theCommandShouldExecuteSuccessfully() {
        assertEquals(0, testContext.getLastCommandExitCode(), 
                "Command should have executed successfully with exit code 0");
    }
    
    @Then("the command should fail")
    public void theCommandShouldFail() {
        assertEquals(1, testContext.getLastCommandExitCode(), 
                "Command should have failed with exit code 1");
    }
    
    @Then("the output should contain {string}")
    public void theOutputShouldContain(String expectedText) {
        String output = testContext.getLastCommandOutput();
        assertTrue(output.contains(expectedText), 
                "Output should contain: " + expectedText);
    }
    
    @Then("the output should not contain {string}")
    public void theOutputShouldNotContain(String unexpectedText) {
        String output = testContext.getLastCommandOutput();
        assertFalse(output.contains(unexpectedText), 
                "Output should not contain: " + unexpectedText);
    }
    
    @Then("the output should include the work item details for matching items")
    public void theOutputShouldIncludeTheWorkItemDetailsForMatchingItems() {
        String output = testContext.getLastCommandOutput();
        assertTrue(output.contains("Work Item:"), "Output should include work item header");
        assertTrue(output.contains("Type:") && output.contains("Priority:") && output.contains("Status:"), 
                "Output should include work item details");
    }
    
    @Then("the output should be valid JSON")
    public void theOutputShouldBeValidJSON() {
        String output = testContext.getLastCommandOutput();
        assertTrue(output.trim().startsWith("{"), "JSON output should start with {");
        assertTrue(output.trim().endsWith("}"), "JSON output should end with }");
        assertTrue(output.contains("\"pattern\":"), "JSON should contain pattern field");
        assertTrue(output.contains("\"results\":"), "JSON should contain results array");
    }
    
    @Then("the JSON output should contain work items with {string} in their content")
    public void theJSONOutputShouldContainWorkItemsWithInTheirContent(String pattern) {
        String output = testContext.getLastCommandOutput();
        assertTrue(output.contains("\"id\":"), "JSON should contain item IDs");
        assertTrue(output.contains("\"title\":"), "JSON should contain item titles");
        assertTrue(output.contains("\"matches\":"), "JSON should contain matches");
    }
    
    @Then("the output should start with a CSV header")
    public void theOutputShouldStartWithACSVHeader() {
        String output = testContext.getLastCommandOutput();
        assertTrue(output.startsWith("WorkItemId,Title,Type,Priority,Status,MatchText,MatchStart,MatchEnd"), 
                "CSV output should start with header");
    }
    
    @Then("the CSV output should contain work items with {string} in their content")
    public void theCSVOutputShouldContainWorkItemsWithInTheirContent(String pattern) {
        String output = testContext.getLastCommandOutput();
        // Skip header line and check for data rows
        String[] lines = output.split("\\r?\\n");
        assertTrue(lines.length > 1, "CSV should have data rows after header");
        
        // Check for quoted fields (CSV format)
        boolean hasQuotedFields = false;
        for (int i = 1; i < lines.length; i++) {
            if (lines[i].contains("\"")) {
                hasQuotedFields = true;
                break;
            }
        }
        assertTrue(hasQuotedFields, "CSV should contain quoted fields");
    }
    
    @Then("the output should show context lines around the matched text")
    public void theOutputShouldShowContextLinesAroundTheMatchedText() {
        String output = testContext.getLastCommandOutput();
        // Context lines are displayed with line numbers
        boolean hasLineNumbers = output.matches(".*\\d+:.*");
        assertTrue(hasLineNumbers, "Output should show line numbers for context");
    }
    
    @Then("the output should indicate no matches were found")
    public void theOutputShouldIndicateNoMatchesWereFound() {
        String output = testContext.getLastCommandOutput();
        assertTrue(output.contains("No matches found"), 
                "Output should indicate no matches were found");
    }
    
    @Then("the error output should explain that a pattern is required")
    public void theErrorOutputShouldExplainThatAPatternIsRequired() {
        String errorOutput = testContext.getErrorOutput();
        assertTrue(errorOutput.contains("No search pattern provided"), 
                "Error output should explain that a pattern is required");
    }
    
    /**
     * Helper method to create search results for a work item.
     */
    private void createSearchResultsForItem(WorkItem item) {
        // Create search results for title and description
        String title = item.getTitle();
        String description = item.getDescription();
        
        if (title != null) {
            addSearchResultForContent(item, title, title.toLowerCase());
        }
        
        if (description != null) {
            addSearchResultForContent(item, description, description.toLowerCase());
        }
    }
    
    /**
     * Helper method to add a search result for content.
     */
    private void addSearchResultForContent(WorkItem item, String content, String searchContent) {
        // Add results for common search terms
        String[] searchTerms = {"authentication", "bug", "documentation", "performance", "security"};
        
        for (String term : searchTerms) {
            if (searchContent.contains(term)) {
                SearchResult result = new SearchResult(
                        UUID.fromString(item.getId()),
                        content,
                        1,
                        "workitem",
                        content,
                        term
                );
                
                // Add match at position of term
                int index = searchContent.indexOf(term);
                result.addMatch(new Match(index, index + term.length(), content.substring(index, index + term.length())));
                
                // Add to mock search service
                mockSearchService.addResult(result);
            }
        }
    }
}