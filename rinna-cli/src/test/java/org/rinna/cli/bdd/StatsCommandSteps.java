/*
 * Step definitions for statistics command BDD tests
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
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import org.mockito.Mockito;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockStatisticsService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.stats.StatisticType;
import org.rinna.cli.stats.StatisticValue;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for statistics command tests.
 */
public class StatsCommandSteps {

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
    public StatsCommandSteps(TestContext testContext) {
        this.testContext = testContext;
        this.outContent = testContext.getOutContent();
        this.errContent = testContext.getErrContent();
    }
    
    @Before
    public void setUp() {
        // Reset mock state before each scenario
        Mockito.reset(testContext.getMockStatisticsService());
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
        
        // Set up mock statistics service to return stats based on the test work items
        setupMockStatistics(workItems);
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

    @Then("the output should contain {string}")
    public void theOutputShouldContain(String expectedText) {
        String output = outContent.toString();
        assertTrue(output.contains(expectedText), 
            "Output should contain '" + expectedText + "' but was:\n" + output);
    }

    @Then("the output should not contain {string}")
    public void theOutputShouldNotContain(String expectedText) {
        String output = outContent.toString();
        assertFalse(output.contains(expectedText), 
            "Output should not contain '" + expectedText + "' but was:\n" + output);
    }

    @Then("the output should be valid JSON")
    public void theOutputShouldBeValidJSON() {
        String output = outContent.toString().trim();
        
        try {
            new JSONObject(output);
            // If we got here, it's valid JSON
            assertTrue(true);
        } catch (Exception e) {
            fail("Output is not valid JSON: " + e.getMessage() + "\nOutput: " + output);
        }
    }

    @Then("the JSON output should contain {string}")
    public void theJSONOutputShouldContain(String expectedKey) {
        String output = outContent.toString().trim();
        
        try {
            JSONObject json = new JSONObject(output);
            assertTrue(json.has(expectedKey), 
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
        
        // Setup creation and update dates
        Instant now = Instant.now();
        when(workItem.getCreatedAt()).thenReturn(now.minusSeconds(3600 * 24 * 30)); // 30 days ago
        when(workItem.getUpdatedAt()).thenReturn(now);
        
        // Setup due date if provided
        if (dueDate != null) {
            Instant dueDateInstant = dueDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            when(workItem.getDueDate()).thenReturn(dueDateInstant);
        }
        
        return workItem;
    }
    
    private void setupMockStatistics(List<WorkItem> workItems) {
        MockStatisticsService mockStatisticsService = testContext.getMockStatisticsService();
        
        // Count data
        int totalItems = workItems.size();
        long completedCount = workItems.stream()
            .filter(item -> item.getState() == WorkflowState.DONE)
            .count();
        long inProgressCount = workItems.stream()
            .filter(item -> item.getState() == WorkflowState.IN_PROGRESS)
            .count();
        long openCount = workItems.stream()
            .filter(item -> item.getState() == WorkflowState.OPEN)
            .count();
        
        // Completion rate
        double completionRate = (double) completedCount / totalItems * 100.0;
        
        // Set up the basic summary stats
        StatisticValue totalStat = StatisticValue.createCount(StatisticType.TOTAL_ITEMS, totalItems, "Total work items");
        StatisticValue completionStat = StatisticValue.createPercentage(StatisticType.COMPLETION_RATE, completionRate, "Completion rate");
        StatisticValue wipStat = StatisticValue.createCount(StatisticType.WORK_IN_PROGRESS, (int) inProgressCount, "Work in progress");
        
        when(mockStatisticsService.getStatistic(StatisticType.TOTAL_ITEMS)).thenReturn(totalStat);
        when(mockStatisticsService.getStatistic(StatisticType.COMPLETION_RATE)).thenReturn(completionStat);
        when(mockStatisticsService.getStatistic(StatisticType.WORK_IN_PROGRESS)).thenReturn(wipStat);
        
        // Distribution by status
        Map<String, Double> statusDistribution = new HashMap<>();
        statusDistribution.put("DONE", (double) completedCount);
        statusDistribution.put("IN_PROGRESS", (double) inProgressCount);
        statusDistribution.put("OPEN", (double) openCount);
        
        StatisticValue statusStat = StatisticValue.createDistribution(
            StatisticType.ITEMS_BY_STATE, statusDistribution, "Work items by status");
        
        when(mockStatisticsService.getDistributionStatistics("status")).thenReturn(statusStat);
        
        // Distribution by priority
        Map<String, Double> priorityMap = workItems.stream()
            .collect(Collectors.groupingBy(
                item -> item.getPriority().name(),
                Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (double) entry.getValue()));
        
        StatisticValue priorityStat = StatisticValue.createDistribution(
            StatisticType.ITEMS_BY_PRIORITY, priorityMap, "Work items by priority");
        
        when(mockStatisticsService.getDistributionStatistics("priority")).thenReturn(priorityStat);
        
        // Distribution by type
        Map<String, Double> typeMap = workItems.stream()
            .collect(Collectors.groupingBy(
                item -> item.getType().name(),
                Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (double) entry.getValue()));
        
        StatisticValue typeStat = StatisticValue.createDistribution(
            StatisticType.ITEMS_BY_TYPE, typeMap, "Work items by type");
        
        when(mockStatisticsService.getDistributionStatistics("type")).thenReturn(typeStat);
        
        // Distribution by assignee
        Map<String, Double> assigneeMap = workItems.stream()
            .filter(item -> item.getAssignee() != null && !item.getAssignee().isEmpty())
            .collect(Collectors.groupingBy(
                WorkItem::getAssignee,
                Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (double) entry.getValue()));
        
        StatisticValue assigneeStat = StatisticValue.createDistribution(
            StatisticType.ITEMS_BY_ASSIGNEE, assigneeMap, "Work items by assignee");
        
        when(mockStatisticsService.getDistributionStatistics("assignee")).thenReturn(assigneeStat);
        
        // Set up summary statistics
        List<StatisticValue> summaryStats = Arrays.asList(totalStat, completionStat, wipStat);
        when(mockStatisticsService.getSummaryStatistics()).thenReturn(summaryStats);
        
        // Set up all statistics
        List<StatisticValue> allStats = new ArrayList<>();
        allStats.add(totalStat);
        allStats.add(completionStat);
        allStats.add(wipStat);
        allStats.add(statusStat);
        allStats.add(priorityStat);
        allStats.add(typeStat);
        allStats.add(assigneeStat);
        
        when(mockStatisticsService.getAllStatistics()).thenReturn(allStats);
        
        // Set up detailed metrics
        List<StatisticValue> completionMetrics = Arrays.asList(
            completionStat,
            StatisticValue.createTime(StatisticType.AVG_COMPLETION_TIME, 5.3, "days", "Average completion time"),
            StatisticValue.createCount(StatisticType.ITEMS_COMPLETED, (int)completedCount, "Items completed")
        );
        
        when(mockStatisticsService.getDetailedMetrics("completion")).thenReturn(completionMetrics);
        
        List<StatisticValue> workflowMetrics = Arrays.asList(
            StatisticValue.createTime(StatisticType.LEAD_TIME, 6.8, "days", "Lead time"),
            StatisticValue.createTime(StatisticType.CYCLE_TIME, 4.2, "days", "Cycle time"),
            wipStat
        );
        
        when(mockStatisticsService.getDetailedMetrics("workflow")).thenReturn(workflowMetrics);
        
        // Dashboard data
        Map<String, Object> dashboardData = new HashMap<>();
        dashboardData.put("summary", summaryStats.stream()
            .collect(Collectors.toMap(s -> s.getType().name(), StatisticValue::getValue)));
        dashboardData.put("statusDistribution", statusDistribution);
        dashboardData.put("priorityDistribution", priorityMap);
        dashboardData.put("typeDistribution", typeMap);
        
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("labels", Arrays.asList("Week 1", "Week 2", "Week 3", "Week 4"));
        progressData.put("completed", Arrays.asList(2, 3, 5, 7));
        progressData.put("created", Arrays.asList(3, 2, 3, 1));
        dashboardData.put("progressChart", progressData);
        
        when(mockStatisticsService.getDashboardStatistics()).thenReturn(dashboardData);
    }
}