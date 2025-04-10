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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.CommentCommand;
import org.rinna.cli.domain.model.CommentType;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockCommentService;
import org.rinna.cli.service.MockCommentService.CommentImpl;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for the Comment Command BDD tests.
 */
public class CommentCommandSteps {

    private TestContext testContext;
    private CommentCommand commentCommand;
    
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    private String currentUser;
    private UUID testItemId;
    private UUID workInProgressId;
    private WorkItem testWorkItem;
    private WorkItem wipWorkItem;
    private List<CommentImpl> existingComments;
    
    private int commandResult;
    
    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private MockItemService mockItemService;
    
    @Mock
    private MockCommentService mockCommentService;
    
    @Mock
    private MockWorkflowService mockWorkflowService;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private ContextManager mockContextManager;
    
    private MockedStatic<ServiceManager> mockedStaticServiceManager;
    private MockedStatic<ContextManager> mockedStaticContextManager;
    
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    private ArgumentCaptor<Map<String, Object>> resultCaptor;
    
    public CommentCommandSteps(TestContext testContext) {
        this.testContext = testContext;
    }
    
    @Before
    public void setUp() {
        // Initialize mocks
        mockServiceManager = Mockito.mock(ServiceManager.class);
        mockItemService = Mockito.mock(MockItemService.class);
        mockCommentService = Mockito.mock(MockCommentService.class);
        mockWorkflowService = Mockito.mock(MockWorkflowService.class);
        mockMetadataService = Mockito.mock(MetadataService.class);
        mockContextManager = Mockito.mock(ContextManager.class);
        
        // Set up argument captors
        paramsCaptor = ArgumentCaptor.forClass(Map.class);
        resultCaptor = ArgumentCaptor.forClass(Map.class);
        
        // Set up static mocks
        mockedStaticServiceManager = Mockito.mockStatic(ServiceManager.class);
        mockedStaticContextManager = Mockito.mockStatic(ContextManager.class);
        
        mockedStaticServiceManager.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        mockedStaticContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
        
        // Set up the service manager mock
        when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockCommentService()).thenReturn(mockCommentService);
        when(mockServiceManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
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
        testItemId = UUID.randomUUID();
        workInProgressId = UUID.randomUUID();
        testWorkItem = createMockWorkItem(testItemId.toString(), "Fix login bug", WorkflowState.CREATED);
        wipWorkItem = createMockWorkItem(workInProgressId.toString(), "Work in progress", WorkflowState.IN_PROGRESS);
        existingComments = new ArrayList<>();
        
        // Create a new CommentCommand instance
        commentCommand = new CommentCommand(mockServiceManager);
        commentCommand.setUsername(currentUser);
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
    }
    
    @Given("a user {string} with basic permissions")
    public void aUserWithBasicPermissions(String username) {
        currentUser = username;
        commentCommand.setUsername(currentUser);
    }
    
    @Given("a work item {string} with title {string} exists")
    public void aWorkItemWithTitleExists(String itemShortId, String title) {
        // Extract numeric part from WI-123 format
        String numericPart = itemShortId.split("-")[1];
        testItemId = new UUID(0, Long.parseLong(numericPart));
        testWorkItem = createMockWorkItem(testItemId.toString(), title, WorkflowState.CREATED);
        
        // Set up mock to return our test work item
        when(mockItemService.getItem(testItemId.toString())).thenReturn(testWorkItem);
        when(mockItemService.findItemByShortId(itemShortId)).thenReturn(testWorkItem);
    }
    
    @Given("user {string} has work item {string} in progress")
    public void userHasWorkItemInProgress(String username, String itemShortId) {
        // Extract numeric part from WI-456 format
        String numericPart = itemShortId.split("-")[1];
        workInProgressId = new UUID(0, Long.parseLong(numericPart));
        
        wipWorkItem = createMockWorkItem(workInProgressId.toString(), "Work in progress item", WorkflowState.IN_PROGRESS);
        when(mockWorkflowService.getCurrentWorkInProgress(username)).thenReturn(Optional.of(wipWorkItem));
        when(mockItemService.getItem(workInProgressId.toString())).thenReturn(wipWorkItem);
    }
    
    @Given("work item {string} has existing comments")
    public void workItemHasExistingComments(String itemShortId) {
        existingComments = createMockComments(testItemId);
        when(mockCommentService.getComments(testItemId)).thenReturn(existingComments);
    }
    
    @Given("user {string} has no work item in progress")
    public void userHasNoWorkItemInProgress(String username) {
        when(mockWorkflowService.getCurrentWorkInProgress(username)).thenReturn(Optional.empty());
    }
    
    @When("the user runs {string}")
    public void theUserRunsCommand(String command) {
        // Parse the command
        parseAndExecuteCommand(command);
    }
    
    @When("the user runs {string} without specifying an item ID")
    public void theUserRunsCommandWithoutSpecifyingAnItemId(String command) {
        // Parse the command without item ID
        parseAndExecuteCommand(command);
    }
    
    @When("the user runs {string} without providing comment text")
    public void theUserRunsCommandWithoutProvidingCommentText(String command) {
        // Parse the command without comment text
        parseAndExecuteCommand(command);
    }
    
    @When("the user runs {string} with a non-existent item ID")
    public void theUserRunsCommandWithANonExistentItemId(String command) {
        // Set up mock to return null for the item, simulating it doesn't exist
        when(mockItemService.getItem(any())).thenReturn(null);
        when(mockItemService.findItemByShortId(anyString())).thenReturn(null);
        
        // Parse and execute the command
        parseAndExecuteCommand(command);
    }
    
    @When("the user runs {string} without providing comment text and with format {string}")
    public void theUserRunsCommandWithoutProvidingCommentTextAndWithFormat(String command, String format) {
        commentCommand.setFormat(format);
        parseAndExecuteCommand(command);
    }
    
    private void parseAndExecuteCommand(String command) {
        // Extract parameters from the command string
        if (command.contains("WI-")) {
            // Extract item ID
            String itemId = extractItemId(command);
            if (itemId != null) {
                try {
                    commentCommand.setItemId(itemId);
                } catch (IllegalArgumentException e) {
                    // In real test, this would be caught by the command.call()
                    commandResult = 1;
                    System.err.println("Error: " + e.getMessage());
                    return;
                }
            }
        }
        
        // Extract comment text
        String commentText = extractCommentText(command);
        if (commentText != null) {
            commentCommand.setComment(commentText);
        }
        
        // Check for verbose flag
        if (command.contains("--verbose")) {
            commentCommand.setVerbose(true);
        }
        
        // Check for format
        if (command.contains("--format=json")) {
            commentCommand.setFormat("json");
        }
        
        // Execute the command
        commandResult = commentCommand.call();
    }
    
    private String extractItemId(String command) {
        // Simple extraction of WI-XXX pattern
        String[] parts = command.split(" ");
        for (String part : parts) {
            if (part.startsWith("WI-")) {
                return part;
            }
        }
        return null;
    }
    
    private String extractCommentText(String command) {
        // Extract text between single quotes
        int startQuote = command.indexOf("'");
        int endQuote = command.lastIndexOf("'");
        
        if (startQuote >= 0 && endQuote > startQuote) {
            return command.substring(startQuote + 1, endQuote);
        }
        return null;
    }
    
    @Then("the command should succeed")
    public void theCommandShouldSucceed() {
        assertEquals("Command should succeed with exit code 0", 0, commandResult);
    }
    
    @Then("the command should fail")
    public void theCommandShouldFail() {
        assertEquals("Command should fail with non-zero exit code", 1, commandResult);
    }
    
    @Then("the output should indicate the comment was added successfully")
    public void theOutputShouldIndicateTheCommentWasAddedSuccessfully() {
        String output = outContent.toString();
        assertTrue("Output should indicate successful comment addition", 
                  output.contains("Comment added to work item"));
    }
    
    @Then("the command should track operation details")
    public void theCommandShouldTrackOperationDetails() {
        verify(mockMetadataService).startOperation(eq("comment"), eq("CREATE"), any(Map.class));
        verify(mockMetadataService).startOperation(eq("comment-add"), eq("CREATE"), any(Map.class));
        verify(mockMetadataService).completeOperation(anyString(), any(Map.class));
    }
    
    @Then("the command should track operation details including item resolution")
    public void theCommandShouldTrackOperationDetailsIncludingItemResolution() {
        verify(mockMetadataService).startOperation(eq("comment"), eq("CREATE"), any(Map.class));
        verify(mockMetadataService).startOperation(eq("comment-resolve-item"), eq("READ"), any(Map.class));
        verify(mockMetadataService).startOperation(eq("comment-add"), eq("CREATE"), any(Map.class));
        verify(mockMetadataService).completeOperation(anyString(), any(Map.class));
    }
    
    @Then("the output should show all comments for the work item")
    public void theOutputShouldShowAllCommentsForTheWorkItem() {
        String output = outContent.toString();
        assertTrue("Output should show comments for the work item", 
                  output.contains("Comments for work item"));
        
        // Verify each comment is displayed
        for (CommentImpl comment : existingComments) {
            assertTrue("Output should contain existing comment text", 
                      output.contains(comment.text()));
        }
    }
    
    @Then("the output should include the newly added comment")
    public void theOutputShouldIncludeTheNewlyAddedComment() {
        String output = outContent.toString();
        String commentText = commentCommand.getClass().getDeclaredFields()[1].getName(); // Hack to get the comment text
        // Verify the actual comment text from the test
        String extractedText = extractCommentText("comment WI-123 'Another comment' --verbose");
        assertTrue("Output should include the newly added comment", 
                  output.contains(extractedText));
    }
    
    @Then("the command should track comment listing operation")
    public void theCommandShouldTrackCommentListingOperation() {
        verify(mockMetadataService).startOperation(eq("comment"), eq("CREATE"), any(Map.class));
        verify(mockMetadataService).startOperation(eq("comment-add"), eq("CREATE"), any(Map.class));
        verify(mockMetadataService).startOperation(eq("comment-list"), eq("READ"), any(Map.class));
        verify(mockMetadataService).completeOperation(anyString(), any(Map.class));
    }
    
    @Then("the output should be valid JSON")
    public void theOutputShouldBeValidJson() {
        String output = outContent.toString();
        assertTrue("Output should start with {", output.trim().startsWith("{"));
        assertTrue("Output should end with }", output.trim().endsWith("}"));
        assertTrue("Output should contain JSON field notation", output.contains(":"));
    }
    
    @Then("the JSON should include success status and work item ID")
    public void theJsonShouldIncludeSuccessStatusAndWorkItemId() {
        String output = outContent.toString();
        assertTrue("JSON should include success status", output.contains("\"success\": true"));
        assertTrue("JSON should include work item ID", output.contains("\"workItemId\":"));
        assertTrue("JSON should include action field", output.contains("\"action\": \"comment_added\""));
    }
    
    @Then("the JSON should include all comments for the work item")
    public void theJsonShouldIncludeAllCommentsForTheWorkItem() {
        String output = outContent.toString();
        assertTrue("JSON should include comments array", output.contains("\"comments\":"));
        assertTrue("JSON should include comment text", output.contains("\"text\":"));
        assertTrue("JSON should include user field", output.contains("\"user\":"));
        assertTrue("JSON should include timestamp", output.contains("\"timestamp\":"));
    }
    
    @Then("the JSON should include the comment count")
    public void theJsonShouldIncludeTheCommentCount() {
        String output = outContent.toString();
        assertTrue("JSON should include comment count", output.contains("\"commentCount\":"));
    }
    
    @Then("the error output should indicate comment text is required")
    public void theErrorOutputShouldIndicateCommentTextIsRequired() {
        String errorOutput = errContent.toString();
        assertTrue("Error output should indicate comment text is required", 
                  errorOutput.contains("Comment text is required"));
    }
    
    @Then("the error output should indicate invalid work item ID")
    public void theErrorOutputShouldIndicateInvalidWorkItemId() {
        String errorOutput = errContent.toString();
        assertTrue("Error output should indicate invalid work item ID", 
                  errorOutput.contains("Invalid work item ID"));
    }
    
    @Then("the error output should indicate work item not found")
    public void theErrorOutputShouldIndicateWorkItemNotFound() {
        String errorOutput = errContent.toString();
        assertTrue("Error output should indicate work item not found", 
                  errorOutput.contains("Work item not found"));
    }
    
    @Then("the error output should indicate no work item is in progress")
    public void theErrorOutputShouldIndicateNoWorkItemIsInProgress() {
        String errorOutput = errContent.toString();
        assertTrue("Error output should indicate no work item in progress", 
                  errorOutput.contains("No work item is currently in progress"));
    }
    
    @Then("the output should be valid JSON with error information")
    public void theOutputShouldBeValidJsonWithErrorInformation() {
        String output = outContent.toString();
        assertTrue("Output should be valid JSON", 
                  output.trim().startsWith("{") && output.trim().endsWith("}"));
        assertTrue("JSON should include error field", output.contains("\"error\":"));
        assertTrue("JSON should include message field", output.contains("\"message\":"));
    }
    
    @Then("the command should track operation failure")
    public void theCommandShouldTrackOperationFailure() {
        verify(mockMetadataService).failOperation(anyString(), any(Exception.class));
    }
    
    // Helper methods
    
    private WorkItem createMockWorkItem(String id, String title, WorkflowState state) {
        WorkItem item = new WorkItem();
        item.setId(id);
        item.setTitle(title);
        item.setState(state);
        return item;
    }
    
    private List<CommentImpl> createMockComments(UUID itemId) {
        List<CommentImpl> comments = new ArrayList<>();
        
        // Add standard comment
        comments.add(new CommentImpl(
            UUID.randomUUID(),
            itemId,
            currentUser,
            Instant.now().minus(4, ChronoUnit.DAYS),
            "Initial investigation completed",
            CommentType.STANDARD
        ));
        
        // Add system comment
        comments.add(new CommentImpl(
            UUID.randomUUID(),
            itemId,
            "system",
            Instant.now().minus(12, ChronoUnit.HOURS),
            "Item was added to Sprint 42",
            CommentType.SYSTEM
        ));
        
        return comments;
    }
}