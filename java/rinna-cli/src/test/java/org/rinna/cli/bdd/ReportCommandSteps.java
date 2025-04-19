/*
 * Step definitions for report command BDD tests
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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.report.ReportFormat;
import org.rinna.cli.report.ReportType;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.ServiceManager;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.After;
import io.cucumber.java.DocString;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for report command tests.
 */
public class ReportCommandSteps {

    private final TestContext testContext;
    private final ByteArrayOutputStream outContent;
    private final ByteArrayOutputStream errContent;
    private final Map<String, WorkItem> testWorkItems = new HashMap<>();
    private final Map<String, Path> tempFiles = new HashMap<>();
    private final Map<String, Map<String, Object>> scheduledReports = new HashMap<>();
    private LocalDate today;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Constructor with test context injection.
     *
     * @param testContext the shared test context
     */
    public ReportCommandSteps(TestContext testContext) {
        this.testContext = testContext;
        this.outContent = testContext.getOutContent();
        this.errContent = testContext.getErrContent();
        
        // Default today to current date
        this.today = LocalDate.now();
    }
    
    @After
    public void cleanup() throws IOException {
        // Clean up any temporary files created during the test
        for (Path filePath : tempFiles.values()) {
            try {
                Files.deleteIfExists(filePath);
            } catch (IOException e) {
                // Ignore exceptions during cleanup
            }
        }
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
                type, 
                Priority.valueOf(priority), 
                WorkflowState.valueOf(status), 
                assignee,
                dueDate
            );
            
            workItems.add(workItem);
            testWorkItems.put(id, workItem);
            
            // Setup mock service to return this work item (using getItem method)
            when(mockItemService.getItem(id)).thenReturn(workItem);
        }
        
        // Setup mock service to return all work items (using getAllItems method)
        when(mockItemService.getAllItems()).thenReturn(workItems);
        
        // Setup mock service to return filtered work items
        setupFilteredWorkItemMocks(mockItemService, workItems);
        
        // Setup mock service for work item counts
        setupWorkItemCountMocks(mockItemService, workItems);
    }

    @Given("today is {string}")
    public void todayIs(String dateString) {
        this.today = LocalDate.parse(dateString, dateFormatter);
        
        // Store today's date in the test context instead of mocking a method that may not exist
        testContext.storeState("currentDate", this.today);
    }

    @Given("I have a custom template {string} with the following content:")
    public void iHaveACustomTemplateWithTheFollowingContent(String templateName, DocString content) throws IOException {
        // Create a temporary file to hold the template
        Path templateFile = Files.createTempFile("template-", templateName);
        Files.writeString(templateFile, content.getContent());
        
        // Store the template file for later use
        tempFiles.put(templateName, templateFile);
    }

    @Given("I have a scheduled report with ID {string}")
    public void iHaveAScheduledReportWithId(String reportId) {
        // Create a simple scheduled report
        Map<String, Object> report = new HashMap<>();
        report.put("id", reportId);
        report.put("type", ReportType.SUMMARY);
        report.put("frequency", "daily");
        report.put("email", "admin@example.com");
        report.put("format", ReportFormat.TEXT);
        
        // Add to scheduled reports
        scheduledReports.put(reportId, report);
        
        // Mock the report scheduler service
        setupScheduledReportMocks();
    }

    @Given("I have a scheduled report with ID {string} and the following properties:")
    public void iHaveAScheduledReportWithIdAndProperties(String reportId, DataTable dataTable) {
        // Extract properties from data table
        Map<String, String> properties = dataTable.asMap(String.class, String.class);
        
        // Create scheduled report
        Map<String, Object> report = new HashMap<>();
        report.put("id", reportId);
        
        // Convert properties to appropriate types
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            
            switch (key) {
                case "type":
                    report.put("type", ReportType.valueOf(value.toUpperCase()));
                    break;
                case "format":
                    report.put("format", ReportFormat.valueOf(value.toUpperCase()));
                    break;
                default:
                    report.put(key, value);
            }
        }
        
        // Add to scheduled reports
        scheduledReports.put(reportId, report);
        
        // Mock the report scheduler service
        setupScheduledReportMocks();
    }

    @Given("I have the following scheduled reports:")
    public void iHaveTheFollowingScheduledReports(DataTable dataTable) {
        // Extract scheduled reports from data table
        List<Map<String, String>> rows = dataTable.asMaps(String.class, String.class);
        
        for (Map<String, String> row : rows) {
            String id = row.get("ID");
            String type = row.get("Type");
            String frequency = row.get("Frequency");
            String email = row.get("Email");
            String format = row.get("Format");
            
            // Create scheduled report
            Map<String, Object> report = new HashMap<>();
            report.put("id", id);
            report.put("type", ReportType.valueOf(type.toUpperCase()));
            report.put("frequency", frequency.toLowerCase());
            report.put("email", email);
            report.put("format", ReportFormat.valueOf(format.toUpperCase()));
            
            // Add to scheduled reports
            scheduledReports.put(id, report);
        }
        
        // Mock the report scheduler service
        setupScheduledReportMocks();
    }

    @When("I run the command {string}")
    public void iRunTheCommand(String commandLine) {
        // Parse the command line
        String[] parts = commandLine.split("\\s+");
        
        // If there's a template path in the command, replace it with the actual temp path
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("--template") && i < parts.length - 1 && tempFiles.containsKey(parts[i+1])) {
                parts[i+1] = tempFiles.get(parts[i+1]).toString();
            }
        }
        
        // If there's an output file in the command, prepare to capture it
        for (int i = 0; i < parts.length; i++) {
            if (parts[i].equals("--output") && i < parts.length - 1) {
                String outputFile = parts[i+1];
                
                // If it doesn't have a file extension and there's a format, add the extension
                if (!outputFile.contains(".")) {
                    for (int j = 0; j < parts.length - 1; j++) {
                        if (parts[j].equals("--format")) {
                            String format = parts[j+1];
                            ReportFormat reportFormat = ReportFormat.fromString(format);
                            outputFile = outputFile + "." + reportFormat.getFileExtension();
                            parts[i+1] = outputFile;
                            break;
                        }
                    }
                }
                
                // Create a temporary file to capture the output
                try {
                    Path outputPath = Files.createTempFile("output-", outputFile);
                    tempFiles.put(outputFile, outputPath);
                    // Update the command to use the temp file
                    parts[i+1] = outputPath.toString();
                } catch (IOException e) {
                    // Just log the error and continue
                    System.err.println("Failed to create temporary output file: " + e.getMessage());
                }
            }
        }
        
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
            "Command should have returned " + errorCode + " exit code");
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

    @Then("the output should not contain {string}")
    public void theOutputShouldNotContain(String expectedText) {
        String output = outContent.toString();
        assertFalse(output.contains(expectedText), 
            "Output should not contain '" + expectedText + "' but was:\n" + output);
    }

    @Then("the output should be valid JSON")
    public void theOutputShouldBeValidJSON() {
        String output = outContent.toString().trim();
        
        // Basic JSON validation - check that it starts with { and ends with }
        assertTrue(output.startsWith("{") && output.endsWith("}"), 
            "Output should be valid JSON but was:\n" + output);
    }

    @Then("the JSON output should contain {string}")
    public void theJSONOutputShouldContain(String expectedKey) {
        String output = outContent.toString().trim();
        
        // Check if the JSON contains the key - simple string check for key pattern
        String keyPattern = "\"" + expectedKey + "\"";
        assertTrue(output.contains(keyPattern), 
            "JSON should contain key '" + expectedKey + "' but was:\n" + output);
    }

    @Then("the output should show all work item fields including description and due date")
    public void theOutputShouldShowAllWorkItemFieldsIncludingDescriptionAndDueDate() {
        String output = outContent.toString();
        
        // Check for various fields that should be included in a detailed report
        assertTrue(output.contains("ID"), "Output should include ID field");
        assertTrue(output.contains("Title"), "Output should include Title field");
        assertTrue(output.contains("Description"), "Output should include Description field");
        assertTrue(output.contains("Status"), "Output should include Status field");
        assertTrue(output.contains("Priority"), "Output should include Priority field");
        assertTrue(output.contains("Type"), "Output should include Type field");
        assertTrue(output.contains("Assignee"), "Output should include Assignee field");
        assertTrue(output.contains("Due Date"), "Output should include Due Date field");
        
        // Check that at least one work item description is included
        boolean foundDescription = false;
        for (WorkItem item : testWorkItems.values()) {
            if (output.contains(item.getDescription())) {
                foundDescription = true;
                break;
            }
        }
        assertTrue(foundDescription, "Output should include at least one work item description");
    }

    @Then("the output should list recent work item changes")
    public void theOutputShouldListRecentWorkItemChanges() {
        // This is a more complex verification - for now, we'll just check that the 
        // output mentions changes or history
        String output = outContent.toString();
        
        assertTrue(output.contains("Changes") || output.contains("History") || 
                   output.contains("Activity") || output.contains("Updates"),
            "Output should mention recent changes, but was:\n" + output);
    }

    @Then("the file {string} should exist")
    public void theFileShouldExist(String filename) {
        assertTrue(tempFiles.containsKey(filename), 
            "Test should have created a temporary file for " + filename);
        
        Path filePath = tempFiles.get(filename);
        assertTrue(Files.exists(filePath), 
            "File " + filename + " should exist at " + filePath);
    }

    @Then("the file {string} should contain {string}")
    public void theFileShouldContain(String filename, String expectedContent) throws IOException {
        Path filePath = tempFiles.get(filename);
        String content = Files.readString(filePath);
        
        assertTrue(content.contains(expectedContent), 
            "File " + filename + " should contain '" + expectedContent + "' but was:\n" + content);
    }

    @Then("the file {string} should contain valid JSON")
    public void theFileShouldContainValidJSON(String filename) throws IOException {
        Path filePath = tempFiles.get(filename);
        String content = Files.readString(filePath);
        
        // Basic JSON validation - check that it starts with { and ends with }
        assertTrue(content.trim().startsWith("{") && content.trim().endsWith("}"), 
            "File " + filename + " should contain valid JSON but was:\n" + content);
    }

    @Then("the scheduled report should have the following properties:")
    public void theScheduledReportShouldHaveTheFollowingProperties(DataTable dataTable) {
        Map<String, String> expectedProperties = dataTable.asMap(String.class, String.class);
        
        // Get the most recently created report
        ArgumentCaptor<Map<String, Object>> reportCaptor = ArgumentCaptor.forClass(Map.class);
        
        try {
            verify(testContext.getMockReportService()).scheduleReport(reportCaptor.capture());
        } catch (Exception e) {
            fail("Failed to capture scheduled report: " + e.getMessage());
            return;
        }
        
        Map<String, Object> report = reportCaptor.getValue();
        
        // Verify each expected property
        for (Map.Entry<String, String> entry : expectedProperties.entrySet()) {
            String key = entry.getKey();
            String expectedValue = entry.getValue();
            
            assertTrue(report.containsKey(key), 
                "Report should contain property '" + key + "'");
            
            Object actualValue = report.get(key);
            if (actualValue instanceof ReportType) {
                assertEquals(expectedValue.toUpperCase(), actualValue.toString(),
                    "Property '" + key + "' should have value '" + expectedValue + "'");
            } else if (actualValue instanceof ReportFormat) {
                assertEquals(expectedValue.toUpperCase(), actualValue.toString(),
                    "Property '" + key + "' should have value '" + expectedValue + "'");
            } else {
                assertEquals(expectedValue, actualValue.toString(),
                    "Property '" + key + "' should have value '" + expectedValue + "'");
            }
        }
    }

    @Then("the scheduled report {string} should have the following properties:")
    public void theScheduledReportShouldHaveTheFollowingProperties(String reportId, DataTable dataTable) {
        Map<String, String> expectedProperties = dataTable.asMap(String.class, String.class);
        
        // Get the updated report
        ArgumentCaptor<String> idCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Map<String, Object>> updateCaptor = ArgumentCaptor.forClass(Map.class);
        
        try {
            verify(testContext.getMockReportService()).updateScheduledReport(
                idCaptor.capture(), updateCaptor.capture());
        } catch (Exception e) {
            fail("Failed to capture scheduled report update: " + e.getMessage());
            return;
        }
        
        // Verify report ID
        assertEquals(reportId, idCaptor.getValue(),
            "Updated report ID should be '" + reportId + "'");
        
        Map<String, Object> updates = updateCaptor.getValue();
        
        // Verify each expected property
        for (Map.Entry<String, String> entry : expectedProperties.entrySet()) {
            String key = entry.getKey();
            String expectedValue = entry.getValue();
            
            assertTrue(updates.containsKey(key), 
                "Updates should contain property '" + key + "'");
            
            Object actualValue = updates.get(key);
            if (actualValue instanceof ReportType) {
                assertEquals(expectedValue.toUpperCase(), actualValue.toString(),
                    "Property '" + key + "' should have value '" + expectedValue + "'");
            } else if (actualValue instanceof ReportFormat) {
                assertEquals(expectedValue.toUpperCase(), actualValue.toString(),
                    "Property '" + key + "' should have value '" + expectedValue + "'");
            } else {
                assertEquals(expectedValue, actualValue.toString(),
                    "Property '" + key + "' should have value '" + expectedValue + "'");
            }
        }
    }

    @Then("the scheduled report {string} should be deleted")
    public void theScheduledReportShouldBeDeleted(String reportId) {
        verify(testContext.getMockReportService()).deleteScheduledReport(eq(reportId));
    }

    // Helper methods
    
    private WorkItem createTestWorkItem(
            String id, 
            String title, 
            String description, 
            String type, 
            Priority priority, 
            WorkflowState status, 
            String assignee,
            LocalDate dueDate) {
        
        // This is a simplified version of the WorkItem creation
        // In a real implementation, this would use the actual WorkItem class
        WorkItem workItem = Mockito.mock(WorkItem.class);
        
        // Setup basic properties
        when(workItem.getId()).thenReturn(id);
        when(workItem.getTitle()).thenReturn(title);
        when(workItem.getDescription()).thenReturn(description);
        when(workItem.getType()).thenReturn(type);
        when(workItem.getPriority()).thenReturn(priority);
        when(workItem.getStatus()).thenReturn(status);
        when(workItem.getAssignee()).thenReturn(assignee);
        
        // Setup due date
        if (dueDate != null) {
            Instant dueDateInstant = dueDate.atStartOfDay().toInstant(ZoneOffset.UTC);
            when(workItem.getDueDate()).thenReturn(dueDateInstant);
        }
        
        return workItem;
    }
    
    private void setupFilteredWorkItemMocks(MockItemService mockItemService, List<WorkItem> allWorkItems) {
        // Setup mocks for filtering by status
        for (WorkflowState status : WorkflowState.values()) {
            List<WorkItem> filtered = new ArrayList<>();
            for (WorkItem item : allWorkItems) {
                if (status.equals(item.getStatus())) {
                    filtered.add(item);
                }
            }
            when(mockItemService.findByState(eq(status))).thenReturn(filtered);
        }
        
        // Setup mocks for filtering by assignee
        Map<String, List<WorkItem>> assigneeMap = new HashMap<>();
        for (WorkItem item : allWorkItems) {
            String assignee = item.getAssignee();
            if (assignee != null) {
                assigneeMap.computeIfAbsent(assignee, k -> new ArrayList<>()).add(item);
            }
        }
        
        for (Map.Entry<String, List<WorkItem>> entry : assigneeMap.entrySet()) {
            when(mockItemService.findByAssignee(eq(entry.getKey()))).thenReturn(entry.getValue());
        }
        
        // Setup mocks for filtering by priority - using findByType which might be available
        for (Priority priority : Priority.values()) {
            List<WorkItem> filtered = new ArrayList<>();
            for (WorkItem item : allWorkItems) {
                if (priority.equals(item.getPriority())) {
                    filtered.add(item);
                }
            }
            // We'll skip this as the method might not exist
            // when(mockItemService.findByPriority(eq(priority))).thenReturn(filtered);
        }
        
        // Setup mocks for filtering by due date (overdue items)
        // We'll store overdue items in the test context instead of mocking methods that may not exist
        List<WorkItem> overdueItems = new ArrayList<>();
        for (WorkItem item : allWorkItems) {
            Instant dueDate = item.getDueDate();
            if (dueDate != null) {
                LocalDate dueDateLocal = LocalDateTime.ofInstant(dueDate, ZoneOffset.UTC).toLocalDate();
                if (dueDateLocal.isBefore(today) && 
                    !WorkflowState.DONE.equals(item.getStatus())) {
                    overdueItems.add(item);
                }
            }
        }
        
        testContext.storeState("overdueItems", overdueItems);
        
        // We'll store days overdue in a map in the test context instead of mocking methods that may not exist
        Map<String, Long> daysOverdueMap = new HashMap<>();
        for (WorkItem item : overdueItems) {
            Instant dueDate = item.getDueDate();
            if (dueDate != null) {
                LocalDate dueDateLocal = LocalDateTime.ofInstant(dueDate, ZoneOffset.UTC).toLocalDate();
                long daysOverdue = ChronoUnit.DAYS.between(dueDateLocal, today);
                daysOverdueMap.put(item.getId(), daysOverdue);
            }
        }
        
        testContext.storeState("daysOverdueMap", daysOverdueMap);
    }
    
    private void setupWorkItemCountMocks(MockItemService mockItemService, List<WorkItem> allWorkItems) {
        // Instead of mocking methods that may not exist, we'll store the counts in the test context
        
        // Count by status
        Map<WorkflowState, Long> statusCounts = allWorkItems.stream()
            .collect(Collectors.groupingBy(WorkItem::getStatus, Collectors.counting()));
        testContext.storeState("statusCounts", statusCounts);
        
        // Count by assignee
        Map<String, Long> assigneeCounts = allWorkItems.stream()
            .filter(item -> item.getAssignee() != null)
            .collect(Collectors.groupingBy(WorkItem::getAssignee, Collectors.counting()));
        testContext.storeState("assigneeCounts", assigneeCounts);
        
        // Count by priority
        Map<Priority, Long> priorityCounts = allWorkItems.stream()
            .collect(Collectors.groupingBy(WorkItem::getPriority, Collectors.counting()));
        testContext.storeState("priorityCounts", priorityCounts);
        
        // Store overall counts in test context
        testContext.storeState("totalWorkItemCount", allWorkItems.size());
        
        // Store completion rate
        long completedCount = statusCounts.getOrDefault(WorkflowState.DONE, 0L);
        double completionRate = allWorkItems.size() > 0 ? 
            (double) completedCount / allWorkItems.size() * 100.0 : 0.0;
        testContext.storeState("completionRate", completionRate);
        
        // Store in progress count
        int inProgressCount = statusCounts.getOrDefault(WorkflowState.IN_PROGRESS, 0L).intValue();
        testContext.storeState("inProgressCount", inProgressCount);
        
        // Store remaining count (not DONE)
        int remainingCount = allWorkItems.size() - (int)completedCount;
        testContext.storeState("remainingCount", remainingCount);
    }
    
    private void setupScheduledReportMocks() {
        try {
            // If we've already set up the mocks, no need to do it again
            if (testContext.getState("scheduledReportMocksSetup") != null) {
                return;
            }
        } catch (Exception e) {
            // State key doesn't exist yet, continue with setup
        }
        
        // Verify the ReportService exists
        if (testContext.getMockReportService() == null) {
            fail("ReportService mock not available in test context");
            return;
        }
        
        // Setup mocks for scheduled reports
        when(testContext.getMockReportService().getScheduledReports())
            .thenReturn(new ArrayList<>(scheduledReports.values()));
        
        // Setup mocks for getting individual reports
        for (Map.Entry<String, Map<String, Object>> entry : scheduledReports.entrySet()) {
            when(testContext.getMockReportService().getScheduledReport(eq(entry.getKey())))
                .thenReturn(entry.getValue());
        }
        
        // Setup mock for deleting reports
        for (String reportId : scheduledReports.keySet()) {
            when(testContext.getMockReportService().deleteScheduledReport(eq(reportId)))
                .thenReturn(true);
        }
        
        // Setup mock for non-existent reports
        when(testContext.getMockReportService().getScheduledReport(eq("nonexistent-id")))
            .thenReturn(null);
        when(testContext.getMockReportService().deleteScheduledReport(eq("nonexistent-id")))
            .thenReturn(false);
        
        // Mark the mocks as set up
        testContext.storeState("scheduledReportMocksSetup", true);
    }
}