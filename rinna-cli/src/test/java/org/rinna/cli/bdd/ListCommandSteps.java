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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.ListCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockSearchService;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Step definitions for the List Command BDD tests.
 */
public class ListCommandSteps {

    private TestContext testContext;
    private ListCommand listCommand;
    
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    private String currentUser;
    private List<WorkItem> testWorkItems;
    private WorkItem workItemBug;
    private WorkItem workItemTask;
    private WorkItem workItemFeature;
    
    private int commandResult;
    
    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private MockSearchService mockSearchService;
    
    @Mock
    private MockItemService mockItemService;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private ContextManager mockContextManager;
    
    private MockedStatic<ServiceManager> mockedStaticServiceManager;
    private MockedStatic<ContextManager> mockedStaticContextManager;
    
    private ArgumentCaptor<Map<String, String>> criteriaCaptor;
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    private ArgumentCaptor<Map<String, Object>> resultCaptor;
    private Map<String, String> lastSearchCriteria;
    
    public ListCommandSteps(TestContext testContext) {
        this.testContext = testContext;
    }
    
    @Before
    public void setUp() {
        // Initialize mocks
        mockServiceManager = Mockito.mock(ServiceManager.class);
        mockSearchService = Mockito.mock(MockSearchService.class);
        mockItemService = Mockito.mock(MockItemService.class);
        mockMetadataService = Mockito.mock(MetadataService.class);
        mockContextManager = Mockito.mock(ContextManager.class);
        
        // Set up argument captors
        criteriaCaptor = ArgumentCaptor.forClass(Map.class);
        paramsCaptor = ArgumentCaptor.forClass(Map.class);
        resultCaptor = ArgumentCaptor.forClass(Map.class);
        
        // Set up static mocks
        mockedStaticServiceManager = Mockito.mockStatic(ServiceManager.class);
        mockedStaticContextManager = Mockito.mockStatic(ContextManager.class);
        
        mockedStaticServiceManager.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        mockedStaticContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
        
        // Set up the service manager mock
        when(mockServiceManager.getMockSearchService()).thenReturn(mockSearchService);
        when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);
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
        initializeTestWorkItems();
        
        // Create a new ListCommand instance
        listCommand = new ListCommand(mockServiceManager);
        
        // Set up mock search service default behavior
        when(mockSearchService.findWorkItems(anyMap(), anyInt())).thenReturn(testWorkItems);
        lastSearchCriteria = new HashMap<>();
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
    
    private void initializeTestWorkItems() {
        testWorkItems = new ArrayList<>();
        
        // Create a BUG work item
        workItemBug = new WorkItem();
        workItemBug.setId("WI-001");
        workItemBug.setTitle("Bug in login system");
        workItemBug.setType(WorkItemType.BUG);
        workItemBug.setPriority(Priority.HIGH);
        workItemBug.setStatus(WorkflowState.IN_PROGRESS);
        workItemBug.setAssignee("alice");
        workItemBug.setProject("Project-A");
        workItemBug.setDescription("Users are unable to log in using social media accounts");
        workItemBug.setCreated(LocalDateTime.now().minusDays(5));
        workItemBug.setUpdated(LocalDateTime.now().minusHours(12));
        
        // Create a TASK work item
        workItemTask = new WorkItem();
        workItemTask.setId("WI-002");
        workItemTask.setTitle("Update documentation");
        workItemTask.setType(WorkItemType.TASK);
        workItemTask.setPriority(Priority.MEDIUM);
        workItemTask.setStatus(WorkflowState.READY);
        workItemTask.setAssignee("bob");
        workItemTask.setProject("Project-A");
        workItemTask.setDescription("Update API documentation with new endpoints");
        workItemTask.setCreated(LocalDateTime.now().minusDays(3));
        workItemTask.setUpdated(LocalDateTime.now().minusDays(2));
        
        // Create a FEATURE work item
        workItemFeature = new WorkItem();
        workItemFeature.setId("WI-003");
        workItemFeature.setTitle("Add export to CSV feature");
        workItemFeature.setType(WorkItemType.FEATURE);
        workItemFeature.setPriority(Priority.LOW);
        workItemFeature.setStatus(WorkflowState.DONE);
        workItemFeature.setAssignee("charlie");
        workItemFeature.setProject("Project-B");
        workItemFeature.setDescription("Add functionality to export work items to CSV format");
        workItemFeature.setCreated(LocalDateTime.now().minusDays(10));
        workItemFeature.setUpdated(LocalDateTime.now().minusHours(24));
        
        // Add items to the list
        testWorkItems.add(workItemBug);
        testWorkItems.add(workItemTask);
        testWorkItems.add(workItemFeature);
    }
    
    @Given("a user {string} with basic permissions")
    public void aUserWithBasicPermissions(String username) {
        currentUser = username;
    }
    
    @Given("work items with various types, priorities, and states exist")
    public void workItemsWithVariousTypesPrioritiesAndStatesExist() {
        // Test items are already initialized in setUp method
        when(mockSearchService.findWorkItems(anyMap(), anyInt())).thenReturn(testWorkItems);
    }
    
    @When("the user runs {string}")
    public void theUserRunsCommand(String command) {
        // Parse and execute the command
        parseAndExecuteCommand(command);
    }
    
    @When("the user runs {string} with no matching items")
    public void theUserRunsCommandWithNoMatchingItems(String command) {
        // Return empty list for search
        when(mockSearchService.findWorkItems(anyMap(), anyInt())).thenReturn(new ArrayList<>());
        
        // Parse and execute the command
        parseAndExecuteCommand(command);
    }
    
    @When("the search service throws an exception")
    public void theSearchServiceThrowsAnException() {
        // Configure mock to throw exception
        when(mockSearchService.findWorkItems(anyMap(), anyInt()))
            .thenThrow(new RuntimeException("Test exception"));
        
        // Execute the command with default parameters
        commandResult = listCommand.call();
    }
    
    private void parseAndExecuteCommand(String command) {
        // Reset default parameters
        listCommand = new ListCommand(mockServiceManager);
        
        // Extract parameters from the command string
        if (command.contains("--type=")) {
            String typeValue = extractParamValue(command, "--type=");
            listCommand.setType(WorkItemType.valueOf(typeValue));
        }
        
        if (command.contains("--priority=")) {
            String priorityValue = extractParamValue(command, "--priority=");
            listCommand.setPriority(Priority.valueOf(priorityValue));
        }
        
        if (command.contains("--state=")) {
            String stateValue = extractParamValue(command, "--state=");
            listCommand.setState(WorkflowState.valueOf(stateValue));
        }
        
        if (command.contains("--project=")) {
            String projectValue = extractParamValue(command, "--project=");
            listCommand.setProject(projectValue);
        }
        
        if (command.contains("--assignee=")) {
            String assigneeValue = extractParamValue(command, "--assignee=");
            listCommand.setAssignee(assigneeValue);
        }
        
        if (command.contains("--limit=")) {
            String limitValue = extractParamValue(command, "--limit=");
            listCommand.setLimit(Integer.parseInt(limitValue));
        }
        
        if (command.contains("--sort-by=")) {
            String sortByValue = extractParamValue(command, "--sort-by=");
            listCommand.setSortBy(sortByValue);
        }
        
        if (command.contains("--descending")) {
            listCommand.setDescending(true);
        }
        
        if (command.contains("--format=")) {
            String formatValue = extractParamValue(command, "--format=");
            listCommand.setFormat(formatValue);
        }
        
        if (command.contains("--verbose")) {
            listCommand.setVerbose(true);
        }
        
        // Capture search criteria
        doCaptureCriteria();
        
        // Execute the command
        commandResult = listCommand.call();
    }
    
    private String extractParamValue(String command, String paramPrefix) {
        int startIndex = command.indexOf(paramPrefix) + paramPrefix.length();
        int endIndex = command.indexOf(" ", startIndex);
        
        if (endIndex == -1) {
            endIndex = command.length();
        }
        
        return command.substring(startIndex, endIndex);
    }
    
    private void doCaptureCriteria() {
        when(mockSearchService.findWorkItems(criteriaCaptor.capture(), anyInt()))
            .thenAnswer(invocation -> {
                lastSearchCriteria = criteriaCaptor.getValue();
                
                // Filter the test work items based on the criteria
                List<WorkItem> filteredItems = new ArrayList<>(testWorkItems);
                
                // Apply type filter
                if (lastSearchCriteria.containsKey("type")) {
                    String typeFilter = lastSearchCriteria.get("type");
                    filteredItems.removeIf(item -> item.getType() == null || 
                                          !item.getType().name().equals(typeFilter));
                }
                
                // Apply priority filter
                if (lastSearchCriteria.containsKey("priority")) {
                    String priorityFilter = lastSearchCriteria.get("priority");
                    filteredItems.removeIf(item -> item.getPriority() == null || 
                                          !item.getPriority().name().equals(priorityFilter));
                }
                
                // Apply state filter
                if (lastSearchCriteria.containsKey("state")) {
                    String stateFilter = lastSearchCriteria.get("state");
                    filteredItems.removeIf(item -> item.getState() == null || 
                                          !item.getState().name().equals(stateFilter));
                }
                
                // Apply project filter
                if (lastSearchCriteria.containsKey("project")) {
                    String projectFilter = lastSearchCriteria.get("project");
                    filteredItems.removeIf(item -> item.getProject() == null || 
                                          !item.getProject().equals(projectFilter));
                }
                
                // Apply assignee filter
                if (lastSearchCriteria.containsKey("assignee")) {
                    String assigneeFilter = lastSearchCriteria.get("assignee");
                    filteredItems.removeIf(item -> item.getAssignee() == null || 
                                          !item.getAssignee().equals(assigneeFilter));
                }
                
                return filteredItems;
            });
    }
    
    @Then("the command should succeed")
    public void theCommandShouldSucceed() {
        assertEquals("Command should succeed with exit code 0", 0, commandResult);
    }
    
    @Then("the command should fail")
    public void theCommandShouldFail() {
        assertEquals("Command should fail with non-zero exit code", 1, commandResult);
    }
    
    @Then("the output should display all work items in a tabular format")
    public void theOutputShouldDisplayAllWorkItemsInATabularFormat() {
        String output = outContent.toString();
        assertTrue("Output should include Work Items header", 
                output.contains("Work Items:"));
        
        // Check for border lines
        assertTrue("Output should include table border", 
                output.contains("-----------------"));
        
        // Check that all items are displayed
        for (WorkItem item : testWorkItems) {
            assertTrue("Output should include item with ID " + item.getId(), 
                    output.contains(item.getId()));
            assertTrue("Output should include item with title " + item.getTitle(), 
                    output.contains(item.getTitle()));
        }
    }
    
    @Then("the output should include column headers for ID, TITLE, TYPE, PRIORITY, STATUS, and ASSIGNEE")
    public void theOutputShouldIncludeColumnHeadersForIdTitleTypePriorityStatusAndAssignee() {
        String output = outContent.toString();
        assertTrue("Output should include ID column header", output.contains("ID"));
        assertTrue("Output should include TITLE column header", output.contains("TITLE"));
        assertTrue("Output should include TYPE column header", output.contains("TYPE"));
        assertTrue("Output should include PRIORITY column header", output.contains("PRIORITY"));
        assertTrue("Output should include STATUS column header", output.contains("STATUS"));
        assertTrue("Output should include ASSIGNEE column header", output.contains("ASSIGNEE"));
    }
    
    @Then("the command should track operation details")
    public void theCommandShouldTrackOperationDetails() {
        verify(mockMetadataService).startOperation(eq("list"), eq("READ"), any(Map.class));
        verify(mockMetadataService).completeOperation(anyString(), any(Map.class));
    }
    
    @Then("the output should only display items of type {string}")
    public void theOutputShouldOnlyDisplayItemsOfType(String type) {
        String output = outContent.toString();
        
        // Find the item with the specified type
        WorkItem matchingItem = testWorkItems.stream()
            .filter(item -> item.getType() != null && item.getType().name().equals(type))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No test item with type " + type));
        
        // Assert that the matching item is in the output
        assertTrue("Output should include item of type " + type,
                output.contains(matchingItem.getId()) && output.contains(matchingItem.getTitle()));
        
        // Assert that items of other types are not in the output
        for (WorkItem item : testWorkItems) {
            if (item.getType() == null || !item.getType().name().equals(type)) {
                assertFalse("Output should not include item with different type: " + item.getId(),
                        output.contains(item.getId()) && output.contains(item.getTitle()));
            }
        }
    }
    
    @Then("the command should track filter parameters")
    public void theCommandShouldTrackFilterParameters() {
        verify(mockMetadataService).startOperation(eq("list"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify that filter parameters are included in tracking
        for (Map.Entry<String, String> entry : lastSearchCriteria.entrySet()) {
            assertTrue("Parameter " + entry.getKey() + " should be tracked",
                    params.containsKey(entry.getKey()));
            assertEquals("Parameter " + entry.getKey() + " should have correct value",
                    entry.getValue(), params.get(entry.getKey()));
        }
    }
    
    @Then("the output should only display items with priority {string}")
    public void theOutputShouldOnlyDisplayItemsWithPriority(String priority) {
        String output = outContent.toString();
        
        // Find the item with the specified priority
        WorkItem matchingItem = testWorkItems.stream()
            .filter(item -> item.getPriority() != null && item.getPriority().name().equals(priority))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No test item with priority " + priority));
        
        // Assert that the matching item is in the output
        assertTrue("Output should include item with priority " + priority,
                output.contains(matchingItem.getId()) && output.contains(matchingItem.getTitle()));
        
        // Assert that items with other priorities are not in the output
        for (WorkItem item : testWorkItems) {
            if (item.getPriority() == null || !item.getPriority().name().equals(priority)) {
                assertFalse("Output should not include item with different priority: " + item.getId(),
                        output.contains(item.getId()) && output.contains(item.getTitle()));
            }
        }
    }
    
    @Then("the output should only display items in {string} state")
    public void theOutputShouldOnlyDisplayItemsInState(String state) {
        String output = outContent.toString();
        
        // Find the item with the specified state
        WorkItem matchingItem = testWorkItems.stream()
            .filter(item -> item.getState() != null && item.getState().name().equals(state))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No test item with state " + state));
        
        // Assert that the matching item is in the output
        assertTrue("Output should include item in state " + state,
                output.contains(matchingItem.getId()) && output.contains(matchingItem.getTitle()));
        
        // Assert that items in other states are not in the output
        for (WorkItem item : testWorkItems) {
            if (item.getState() == null || !item.getState().name().equals(state)) {
                assertFalse("Output should not include item with different state: " + item.getId(),
                        output.contains(item.getId()) && output.contains(item.getTitle()));
            }
        }
    }
    
    @Then("the output should only display items from project {string}")
    public void theOutputShouldOnlyDisplayItemsFromProject(String project) {
        String output = outContent.toString();
        
        // Find the item with the specified project
        WorkItem matchingItem = testWorkItems.stream()
            .filter(item -> item.getProject() != null && item.getProject().equals(project))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No test item from project " + project));
        
        // Assert that the matching item is in the output
        assertTrue("Output should include item from project " + project,
                output.contains(matchingItem.getId()) && output.contains(matchingItem.getTitle()));
        
        // Assert that items from other projects are not in the output
        for (WorkItem item : testWorkItems) {
            if (item.getProject() == null || !item.getProject().equals(project)) {
                assertFalse("Output should not include item from different project: " + item.getId(),
                        output.contains(item.getId()) && output.contains(item.getTitle()));
            }
        }
    }
    
    @Then("the output should only display items assigned to {string}")
    public void theOutputShouldOnlyDisplayItemsAssignedTo(String assignee) {
        String output = outContent.toString();
        
        // Find the item with the specified assignee
        WorkItem matchingItem = testWorkItems.stream()
            .filter(item -> item.getAssignee() != null && item.getAssignee().equals(assignee))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No test item assigned to " + assignee));
        
        // Assert that the matching item is in the output
        assertTrue("Output should include item assigned to " + assignee,
                output.contains(matchingItem.getId()) && output.contains(matchingItem.getTitle()));
        
        // Assert that items with other assignees are not in the output
        for (WorkItem item : testWorkItems) {
            if (item.getAssignee() == null || !item.getAssignee().equals(assignee)) {
                assertFalse("Output should not include item with different assignee: " + item.getId(),
                        output.contains(item.getId()) && output.contains(item.getTitle()));
            }
        }
    }
    
    @Then("the output should display at most {int} items")
    public void theOutputShouldDisplayAtMostItems(Integer limit) {
        String output = outContent.toString();
        
        // Count occurrences of item IDs in the output
        int count = 0;
        for (WorkItem item : testWorkItems) {
            if (output.contains(item.getId())) {
                count++;
            }
        }
        
        assertTrue("Output should display at most " + limit + " items, but found " + count,
                count <= limit);
    }
    
    @Then("the output should indicate there are more items that could be displayed")
    public void theOutputShouldIndicateThereAreMoreItemsThatCouldBeDisplayed() {
        String output = outContent.toString();
        assertTrue("Output should indicate more items available",
                output.contains("more items") || output.contains("to see more items"));
    }
    
    @Then("the command should track the limit parameter")
    public void theCommandShouldTrackTheLimitParameter() {
        verify(mockMetadataService).startOperation(eq("list"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify that limit parameter is included in tracking
        assertTrue("Limit parameter should be tracked", params.containsKey("limit"));
    }
    
    @Then("the output should display items sorted by title in ascending order")
    public void theOutputShouldDisplayItemsSortedByTitleInAscendingOrder() {
        // This is a bit tricky to verify in a string output
        // We'll check if the items appear in the correct order in the output
        String output = outContent.toString();
        
        // Get expected sorted order of titles
        List<String> sortedTitles = testWorkItems.stream()
            .map(WorkItem::getTitle)
            .sorted()
            .toList();
        
        // Check if first item appears before second item in the output
        int pos1 = output.indexOf(sortedTitles.get(0));
        int pos2 = output.indexOf(sortedTitles.get(1));
        int pos3 = output.indexOf(sortedTitles.get(2));
        
        assertTrue("First sorted item should appear before second item", pos1 < pos2);
        assertTrue("Second sorted item should appear before third item", pos2 < pos3);
    }
    
    @Then("the command should track sort parameters")
    public void theCommandShouldTrackSortParameters() {
        verify(mockMetadataService).startOperation(eq("list"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify that sort parameters are included in tracking
        assertTrue("Sort parameter should be tracked", params.containsKey("sort_by"));
        assertTrue("Descending parameter should be tracked", params.containsKey("descending"));
    }
    
    @Then("the output should display items sorted by priority in descending order")
    public void theOutputShouldDisplayItemsSortedByPriorityInDescendingOrder() {
        // Check if the items appear in descending priority order in the output
        String output = outContent.toString();
        
        // Find positions of different priority items
        int highPriorityPos = output.indexOf(workItemBug.getTitle()); // HIGH priority
        int mediumPriorityPos = output.indexOf(workItemTask.getTitle()); // MEDIUM priority
        int lowPriorityPos = output.indexOf(workItemFeature.getTitle()); // LOW priority
        
        // In descending order, HIGH should come before MEDIUM, and MEDIUM before LOW
        assertTrue("HIGH priority item should appear before MEDIUM priority item", 
                highPriorityPos < mediumPriorityPos);
        assertTrue("MEDIUM priority item should appear before LOW priority item", 
                mediumPriorityPos < lowPriorityPos);
    }
    
    @Then("the output should be valid JSON")
    public void theOutputShouldBeValidJson() {
        String output = outContent.toString();
        assertTrue("Output should start with {", output.trim().startsWith("{"));
        assertTrue("Output should end with }", output.trim().endsWith("}"));
        assertTrue("Output should contain JSON field notation", output.contains(":"));
    }
    
    @Then("the JSON should include an array of work items")
    public void theJsonShouldIncludeAnArrayOfWorkItems() {
        String output = outContent.toString();
        assertTrue("JSON should include items array", output.contains("\"items\":"));
        // Check for some typical fields in the JSON output
        assertTrue("JSON should include item ID", output.contains("\"id\":"));
        assertTrue("JSON should include item title", output.contains("\"title\":"));
        assertTrue("JSON should include item type", output.contains("\"type\":"));
    }
    
    @Then("the command should track format parameters")
    public void theCommandShouldTrackFormatParameters() {
        verify(mockMetadataService).startOperation(eq("list"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify that format parameter is included in tracking
        assertTrue("Format parameter should be tracked", params.containsKey("format"));
    }
    
    @Then("the output should include additional details for each work item")
    public void theOutputShouldIncludeAdditionalDetailsForEachWorkItem() {
        String output = outContent.toString();
        
        // Check for at least one item's description
        assertTrue("Output should include item description", 
                output.contains(workItemBug.getDescription()));
        
        // Check for additional details like dates
        assertTrue("Output should include creation date information", 
                output.contains("Created:"));
    }
    
    @Then("the command should track the verbose parameter")
    public void theCommandShouldTrackTheVerboseParameter() {
        verify(mockMetadataService).startOperation(eq("list"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify that verbose parameter is included in tracking
        assertTrue("Verbose parameter should be tracked", params.containsKey("verbose"));
    }
    
    @Then("the output should only display items matching all filter criteria")
    public void theOutputShouldOnlyDisplayItemsMatchingAllFilterCriteria() {
        String output = outContent.toString();
        
        // Identify which test items should match all criteria
        boolean anyMatches = false;
        for (WorkItem item : testWorkItems) {
            boolean matches = true;
            
            // Check each criterion
            if (lastSearchCriteria.containsKey("type") && 
                (item.getType() == null || !item.getType().name().equals(lastSearchCriteria.get("type")))) {
                matches = false;
            }
            
            if (lastSearchCriteria.containsKey("priority") && 
                (item.getPriority() == null || !item.getPriority().name().equals(lastSearchCriteria.get("priority")))) {
                matches = false;
            }
            
            if (lastSearchCriteria.containsKey("state") && 
                (item.getState() == null || !item.getState().name().equals(lastSearchCriteria.get("state")))) {
                matches = false;
            }
            
            // If this item matches all criteria, it should be in the output
            if (matches) {
                anyMatches = true;
                assertTrue("Item matching all criteria should be in output: " + item.getId(),
                        output.contains(item.getId()) && output.contains(item.getTitle()));
            } else {
                // If this item doesn't match all criteria, it should not be in the output
                assertFalse("Item not matching all criteria should not be in output: " + item.getId(),
                        output.contains(item.getId()) && output.contains(item.getTitle()));
            }
        }
        
        // If nothing matches, we should see the "no items found" message
        if (!anyMatches) {
            assertTrue("Output should indicate no matching items when no items match all criteria",
                    output.contains("No work items found"));
        }
    }
    
    @Then("the command should track all filter parameters")
    public void theCommandShouldTrackAllFilterParameters() {
        verify(mockMetadataService).startOperation(eq("list"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify that all filter parameters are included in tracking
        for (String key : lastSearchCriteria.keySet()) {
            assertTrue("Filter parameter " + key + " should be tracked", params.containsKey(key));
        }
    }
    
    @Then("the output should indicate no work items were found")
    public void theOutputShouldIndicateNoWorkItemsWereFound() {
        String output = outContent.toString();
        assertTrue("Output should indicate no items found",
                output.contains("No work items found matching the criteria"));
    }
    
    @Then("the command should track operation completion with zero results")
    public void theCommandShouldTrackOperationCompletionWithZeroResults() {
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        Map<String, Object> result = resultCaptor.getValue();
        
        // Verify that result contains count=0
        assertTrue("Result should include count parameter", result.containsKey("count"));
        assertEquals("Count should be zero", 0, result.get("count"));
    }
    
    @Then("the error output should indicate an issue with listing work items")
    public void theErrorOutputShouldIndicateAnIssueWithListingWorkItems() {
        String errorOutput = errContent.toString();
        assertTrue("Error output should indicate problem with listing items",
                errorOutput.contains("Error listing work items"));
    }
    
    @Then("the command should track operation failure")
    public void theCommandShouldTrackOperationFailure() {
        verify(mockMetadataService).failOperation(anyString(), any(Exception.class));
    }
}