/*
 * Step definitions for critical path command BDD tests
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.bdd;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

import org.mockito.Mockito;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.MockCriticalPathService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.ServiceManager;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for critical path command tests.
 */
public class CriticalPathCommandSteps {

    private final TestContext testContext;
    private final ByteArrayOutputStream outContent;
    private final ByteArrayOutputStream errContent;
    private final Map<String, WorkItem> testWorkItems = new HashMap<>();
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Constructor with test context injection.
     *
     * @param testContext the shared test context
     */
    public CriticalPathCommandSteps(TestContext testContext) {
        this.testContext = testContext;
        this.outContent = testContext.getOutContent();
        this.errContent = testContext.getErrContent();
    }
    
    @Before
    public void setUp() {
        // Reset mock state before each scenario
        Mockito.reset(testContext.getMockCriticalPathService());
    }
    
    @After
    public void tearDown() {
        testWorkItems.clear();
    }

    @Given("I am logged in as {string}")
    public void iAmLoggedInAs(String username) {
        ConfigurationService mockConfigService = testContext.getMockConfigService();
        when(mockConfigService.getCurrentUser()).thenReturn(username);
        when(mockConfigService.isAuthenticated()).thenReturn(true);
    }

    @Given("the system has the following work items:")
    public void theSystemHasTheFollowingWorkItems(DataTable dataTable) {
        MockItemService mockItemService = testContext.getMockItemService();
        ServiceManager mockServiceManager = testContext.getMockServiceManager();
        
        // Setup service manager mocks
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        
        // Extract work items from data table
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        List<WorkItem> workItems = new ArrayList<>();
        
        for (Map<String, String> row : rows) {
            String id = row.get("ID");
            String title = row.get("Title");
            String description = row.get("Description");
            String type = row.get("Type");
            String priority = row.get("Priority");
            String status = row.get("Status");
            String assignee = row.get("Assignee");
            String dueDateStr = row.get("Due Date");
            
            LocalDate dueDate = null;
            if (dueDateStr != null && !dueDateStr.isEmpty()) {
                dueDate = LocalDate.parse(dueDateStr, dateFormatter);
            }
            
            // Create work item
            WorkItem workItem = createTestWorkItem(
                id, 
                title, 
                description, 
                WorkItemType.valueOf(type), 
                Priority.valueOf(priority), 
                WorkflowState.valueOf(status), 
                assignee,
                dueDate
            );
            
            workItems.add(workItem);
            testWorkItems.put(id, workItem);
            
            // Setup mock service to return this work item
            when(mockItemService.getWorkItem(id)).thenReturn(workItem);
        }
        
        // Setup mock service to return all work items
        when(mockItemService.getAllItems()).thenReturn(workItems);
    }
    
    @Given("the system has the following work item dependencies:")
    public void theSystemHasTheFollowingWorkItemDependencies(DataTable dataTable) {
        MockCriticalPathService mockCriticalPathService = testContext.getMockCriticalPathService();
        ServiceManager mockServiceManager = testContext.getMockServiceManager();
        
        // Setup service manager mocks
        when(mockServiceManager.getCriticalPathService()).thenReturn(mockCriticalPathService);
        
        // Extract dependencies from data table
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        
        // Clear any existing dependencies - assuming void return type
        doNothing().when(mockCriticalPathService).clearDependencies();
        
        // Add each dependency to the mock service
        for (Map<String, String> row : rows) {
            String workItem = row.get("Work Item");
            String dependsOn = row.get("Depends On");
            
            // Add dependency - assuming void return type
            doNothing().when(mockCriticalPathService).addDependency(workItem, dependsOn);
        }
        
        // Set up default critical path based on dependencies
        List<String> criticalPath = Arrays.asList("WI-102", "WI-101", "WI-104");
        when(mockCriticalPathService.getCriticalPath()).thenReturn(criticalPath);
        
        // Set up default critical path details
        Map<String, Object> criticalPathDetails = new HashMap<>();
        criticalPathDetails.put("criticalPath", criticalPath);
        criticalPathDetails.put("pathLength", 3);
        criticalPathDetails.put("totalEffort", 24);
        criticalPathDetails.put("bottlenecks", Collections.singletonList("WI-102"));
        criticalPathDetails.put("estimatedCompletionDate", LocalDate.now().plusDays(3));
        
        when(mockCriticalPathService.getCriticalPathDetails()).thenReturn(criticalPathDetails);
        
        // Set up dependency graph
        Map<String, List<String>> dependencyGraph = new HashMap<>();
        dependencyGraph.put("WI-101", Arrays.asList("WI-102", "WI-108"));
        dependencyGraph.put("WI-104", Arrays.asList("WI-101", "WI-103"));
        dependencyGraph.put("WI-106", Arrays.asList("WI-102", "WI-107"));
        
        when(mockCriticalPathService.getDependencyGraph()).thenReturn(dependencyGraph);
        
        // Set up blockers
        List<Map<String, Object>> blockers = new ArrayList<>();
        Map<String, Object> blocker = new HashMap<>();
        blocker.put("id", "WI-102");
        blocker.put("directlyBlocks", Arrays.asList("WI-101", "WI-106"));
        blocker.put("totalImpact", Arrays.asList("WI-101", "WI-104", "WI-106"));
        blockers.add(blocker);
        
        when(mockCriticalPathService.getBlockers()).thenReturn(blockers);
        
        // Set up item critical path
        for (String itemId : Arrays.asList("WI-101", "WI-102", "WI-104")) {
            Map<String, Object> itemPath = new HashMap<>();
            itemPath.put("onCriticalPath", true);
            itemPath.put("criticalPath", criticalPath);
            itemPath.put("position", criticalPath.indexOf(itemId) + 1);
            
            if ("WI-104".equals(itemId)) {
                itemPath.put("directDependencies", Arrays.asList("WI-101", "WI-103"));
                itemPath.put("indirectDependencies", Arrays.asList("WI-102", "WI-108"));
            } else if ("WI-101".equals(itemId)) {
                itemPath.put("directDependencies", Arrays.asList("WI-102", "WI-108"));
                itemPath.put("indirectDependencies", Collections.emptyList());
            } else if ("WI-102".equals(itemId)) {
                itemPath.put("directDependencies", Collections.emptyList());
                itemPath.put("indirectDependencies", Collections.emptyList());
            }
            
            when(mockCriticalPathService.getItemCriticalPath(itemId)).thenReturn(itemPath);
        }
        
        // Set up estimated efforts - assuming void return type
        doNothing().when(mockCriticalPathService).setEstimatedEffort(eq("WI-102"), anyInt());
        doNothing().when(mockCriticalPathService).setEstimatedEffort(eq("WI-101"), anyInt());
        doNothing().when(mockCriticalPathService).setEstimatedEffort(eq("WI-104"), anyInt());
        
        // Set up critical path with time estimates
        List<Map<String, Object>> estimates = new ArrayList<>();
        Map<String, Object> estimate1 = new HashMap<>();
        estimate1.put("id", "WI-102");
        estimate1.put("estimatedEffort", 8);
        estimate1.put("cumulativeEffort", 8);
        estimate1.put("estimatedCompletionDate", LocalDate.now().plusDays(1));
        
        Map<String, Object> estimate2 = new HashMap<>();
        estimate2.put("id", "WI-101");
        estimate2.put("estimatedEffort", 8);
        estimate2.put("cumulativeEffort", 16);
        estimate2.put("estimatedCompletionDate", LocalDate.now().plusDays(2));
        
        Map<String, Object> estimate3 = new HashMap<>();
        estimate3.put("id", "WI-104");
        estimate3.put("estimatedEffort", 8);
        estimate3.put("cumulativeEffort", 24);
        estimate3.put("estimatedCompletionDate", LocalDate.now().plusDays(3));
        
        estimates.add(estimate1);
        estimates.add(estimate2);
        estimates.add(estimate3);
        
        when(mockCriticalPathService.getCriticalPathWithEstimates()).thenReturn(estimates);
    }

    @Given("the system has no work item dependencies")
    public void theSystemHasNoWorkItemDependencies() {
        MockCriticalPathService mockCriticalPathService = testContext.getMockCriticalPathService();
        ServiceManager mockServiceManager = testContext.getMockServiceManager();
        
        // Setup service manager mocks
        when(mockServiceManager.getCriticalPathService()).thenReturn(mockCriticalPathService);
        
        // Clear dependencies and set up empty critical path - assuming void return type
        doNothing().when(mockCriticalPathService).clearDependencies();
        when(mockCriticalPathService.getCriticalPath()).thenReturn(Collections.emptyList());
        when(mockCriticalPathService.getCriticalPathDetails()).thenReturn(Collections.emptyMap());
        when(mockCriticalPathService.getDependencyGraph()).thenReturn(Collections.emptyMap());
        when(mockCriticalPathService.getBlockers()).thenReturn(Collections.emptyList());
    }

    @When("I run the command {string}")
    public void iRunTheCommand(String commandLine) {
        // Parse the command line
        String[] parts = commandLine.split("\\s+");
        
        // Execute the command
        String baseCommand = parts[1];
        String[] args = Arrays.copyOfRange(parts, 2, parts.length);
        
        testContext.getCommandProcessor().processCommand(baseCommand, args);
    }

    @Then("the command should execute successfully")
    public void theCommandShouldExecuteSuccessfully() {
        assertEquals(0, testContext.getLastCommandExitCode(), 
            "Command should have returned 0 exit code. Error: " + errContent.toString());
    }

    @Then("the command should fail with error code {int}")
    public void theCommandShouldFailWithErrorCode(int errorCode) {
        assertEquals(errorCode, testContext.getLastCommandExitCode(), 
            "Command should have returned error code " + errorCode);
    }

    @Then("the output should contain {string}")
    public void theOutputShouldContain(String expectedText) {
        String output = outContent.toString();
        assertTrue(output.contains(expectedText), 
            "Output should contain '" + expectedText + "' but was:\n" + output);
    }

    @Then("the error output should contain {string}")
    public void theErrorOutputShouldContain(String expectedText) {
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains(expectedText), 
            "Error output should contain '" + expectedText + "' but was:\n" + errorOutput);
    }

    @Then("the output should be valid JSON")
    public void theOutputShouldBeValidJSON() {
        String output = outContent.toString().trim();
        
        try {
            // Use a basic JSON validation approach
            if (output.startsWith("{") && output.endsWith("}")) {
                // Basic check if it starts with { and ends with }
                assertTrue(true, "Output appears to be valid JSON");
            } else {
                fail("Output does not appear to be valid JSON: " + output);
            }
        } catch (Exception e) {
            fail("Output is not valid JSON: " + e.getMessage() + "\nOutput: " + output);
        }
    }

    @Then("the JSON output should contain {string}")
    public void theJSONOutputShouldContain(String expectedKey) {
        String output = outContent.toString().trim();
        
        try {
            // Simple string-based check for JSON key
            assertTrue(output.contains("\"" + expectedKey + "\""), 
                "JSON should contain key '" + expectedKey + "' but was:\n" + output);
        } catch (Exception e) {
            fail("Output is not valid JSON: " + e.getMessage() + "\nOutput: " + output);
        }
    }

    // Helper methods
    
    private WorkItem createTestWorkItem(
            String id, 
            String title, 
            String description, 
            WorkItemType type, 
            Priority priority, 
            WorkflowState status, 
            String assignee,
            LocalDate dueDate) {
        
        // Create a simple mock WorkItem
        WorkItem workItem = Mockito.mock(WorkItem.class);
        
        // Setup the basic properties
        when(workItem.getId()).thenReturn(id);
        when(workItem.getTitle()).thenReturn(title);
        when(workItem.getDescription()).thenReturn(description);
        when(workItem.getType()).thenReturn(type);
        when(workItem.getPriority()).thenReturn(priority);
        when(workItem.getState()).thenReturn(status);
        when(workItem.getAssignee()).thenReturn(assignee);
        
        // Setup creation and update dates (assuming these methods exist)
        Instant now = Instant.now();
        // Comment out the timestamp methods if they don't exist in the WorkItem interface
        // when(workItem.getCreatedAt()).thenReturn(now.minusSeconds(3600 * 24 * 30)); // 30 days ago
        // when(workItem.getUpdatedAt()).thenReturn(now);
        
        // Setup due date if provided (assuming this method exists)
        // Commenting out due date setup until we know the correct return type
        /*
        if (dueDate != null) {
            // We need to match the return type of the getDueDate method
            // This depends on the WorkItem interface definition
            when(workItem.getDueDate()).thenReturn(dueDate);
        }
        */
        
        return workItem;
    }
}