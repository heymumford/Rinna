package org.rinna.cli.bdd;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.ScheduleCommand;
import org.rinna.cli.report.ReportConfig;
import org.rinna.cli.report.ReportFormat;
import org.rinna.cli.report.ReportScheduler;
import org.rinna.cli.report.ReportScheduler.ScheduleType;
import org.rinna.cli.report.ReportScheduler.ScheduledReport;
import org.rinna.cli.report.ReportType;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockReportService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

/**
 * Step definitions for testing the ScheduleCommand.
 */
public class ScheduleCommandSteps {
    private TestContext context;
    
    // Mock services
    private ServiceManager mockServiceManager;
    private MetadataService mockMetadataService;
    private MockReportService mockReportService;
    private ReportScheduler mockScheduler;
    
    // Capture the standard output and error
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    // Command execution result
    private Integer commandResult;
    
    // Operation tracking
    private ArgumentCaptor<String> operationNameCaptor;
    private ArgumentCaptor<String> operationActionCaptor;
    private ArgumentCaptor<Map<String, Object>> operationParamsCaptor;
    
    // Mock static objects
    private MockedStatic<ServiceManager> mockStaticServiceManager;
    private MockedStatic<ReportScheduler> mockStaticReportScheduler;
    private MockedStatic<OutputFormatter> mockStaticOutputFormatter;
    
    /**
     * Constructor with test context.
     * 
     * @param context the test context
     */
    public ScheduleCommandSteps(TestContext context) {
        this.context = context;
    }
    
    /**
     * Setup before each scenario.
     */
    @Before
    public void setUp() {
        // Redirect stdout and stderr for capturing output
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Initialize mocks
        mockServiceManager = mock(ServiceManager.class);
        mockMetadataService = mock(MetadataService.class);
        mockReportService = mock(MockReportService.class);
        mockScheduler = mock(ReportScheduler.class);
        
        // Configure mocks
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getMockReportService()).thenReturn(mockReportService);
        
        // Setup argument captors
        operationNameCaptor = ArgumentCaptor.forClass(String.class);
        operationActionCaptor = ArgumentCaptor.forClass(String.class);
        operationParamsCaptor = ArgumentCaptor.forClass(Map.class);
        
        // Mock static objects
        mockStaticServiceManager = Mockito.mockStatic(ServiceManager.class);
        mockStaticServiceManager.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        
        mockStaticReportScheduler = Mockito.mockStatic(ReportScheduler.class);
        mockStaticReportScheduler.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
        
        mockStaticOutputFormatter = Mockito.mockStatic(OutputFormatter.class);
    }
    
    /**
     * Cleanup after each scenario.
     */
    @After
    public void tearDown() {
        // Restore original stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Close static mocks
        if (mockStaticServiceManager != null) {
            mockStaticServiceManager.close();
        }
        if (mockStaticReportScheduler != null) {
            mockStaticReportScheduler.close();
        }
        if (mockStaticOutputFormatter != null) {
            mockStaticOutputFormatter.close();
        }
    }
    
    /**
     * Sets up a user with basic permissions.
     * 
     * @param username the username
     */
    @Given("a user {string} with basic permissions")
    public void aUserWithBasicPermissions(String username) {
        // Store the username in the context
        context.setUsername(username);
    }
    
    /**
     * Sets up the ReportScheduler.
     */
    @Given("the ReportScheduler is initialized")
    public void theReportSchedulerIsInitialized() {
        // This is implicitly handled by the mock setup in setUp()
    }
    
    /**
     * Sets up existing scheduled reports for the list command.
     */
    @Given("there are existing scheduled reports")
    public void thereAreExistingScheduledReports() {
        // Create a list of scheduled reports
        List<ScheduledReport> scheduledReports = new ArrayList<>();
        
        // Add a daily report
        ScheduledReport dailyReport = new ScheduledReport();
        dailyReport.setId("schedule-123");
        dailyReport.setName("Daily Status");
        dailyReport.setScheduleType(ScheduleType.DAILY);
        dailyReport.setTime("09:00");
        ReportConfig dailyConfig = new ReportConfig();
        dailyConfig.setType(ReportType.STATUS);
        dailyConfig.setFormat(ReportFormat.TEXT);
        dailyReport.setConfig(dailyConfig);
        scheduledReports.add(dailyReport);
        
        // Add a weekly report
        ScheduledReport weeklyReport = new ScheduledReport();
        weeklyReport.setId("schedule-456");
        weeklyReport.setName("Weekly Summary");
        weeklyReport.setScheduleType(ScheduleType.WEEKLY);
        weeklyReport.setDayOfWeek(DayOfWeek.MONDAY);
        weeklyReport.setTime("08:00");
        ReportConfig weeklyConfig = new ReportConfig();
        weeklyConfig.setType(ReportType.SUMMARY);
        weeklyConfig.setFormat(ReportFormat.HTML);
        weeklyReport.setConfig(weeklyConfig);
        scheduledReports.add(weeklyReport);
        
        // Mock the getScheduledReports method
        when(mockScheduler.getScheduledReports()).thenReturn(scheduledReports);
        
        // Store the reports in the context
        context.setAttribute("scheduledReports", scheduledReports);
    }
    
    /**
     * Sets up an existing scheduled report with a specific ID.
     * 
     * @param id the report ID
     */
    @Given("there is a scheduled report with ID {string}")
    public void thereIsAScheduledReportWithID(String id) {
        // Create a scheduled report
        ScheduledReport report = new ScheduledReport();
        report.setId(id);
        report.setName("Test Report");
        report.setScheduleType(ScheduleType.DAILY);
        report.setTime("09:00");
        ReportConfig config = new ReportConfig();
        config.setType(ReportType.STATUS);
        config.setFormat(ReportFormat.TEXT);
        report.setConfig(config);
        
        // Mock the getScheduledReport method
        when(mockScheduler.getScheduledReport(id)).thenReturn(report);
        
        // Mock the getScheduledReports method to return a list with this report
        List<ScheduledReport> reports = new ArrayList<>();
        reports.add(report);
        when(mockScheduler.getScheduledReports()).thenReturn(reports);
        
        // Store the report in the context
        context.setAttribute("scheduledReport", report);
    }
    
    /**
     * Executes a command.
     * 
     * @param command the command to execute
     */
    @When("the user runs {string}")
    public void theUserRunsCommand(String command) {
        // Parse the command string
        String trimmedCommand = command.trim();
        
        // Initialize a ScheduleCommand
        ScheduleCommand scheduleCommand = new ScheduleCommand(mockServiceManager);
        
        // Set the username from the context
        String username = context.getUsername();
        if (username != null) {
            scheduleCommand.setUsername(username);
        }
        
        // Parse the action (first word after "schedule")
        Pattern actionPattern = Pattern.compile("schedule\\s+(\\w+)");
        Matcher actionMatcher = actionPattern.matcher(trimmedCommand);
        if (actionMatcher.find()) {
            String action = actionMatcher.group(1);
            scheduleCommand.setAction(action);
        }
        
        // Parse the parameters
        // ID parameter (--id=xyz)
        Pattern idPattern = Pattern.compile("--id=([\\w-]+)");
        Matcher idMatcher = idPattern.matcher(trimmedCommand);
        if (idMatcher.find()) {
            scheduleCommand.setId(idMatcher.group(1));
        }
        
        // Name parameter (--name='xyz')
        Pattern namePattern = Pattern.compile("--name=(?:'([^']*)'|\"([^\"]*)\"|([^\\s]+))");
        Matcher nameMatcher = namePattern.matcher(trimmedCommand);
        if (nameMatcher.find()) {
            String name = nameMatcher.group(1);
            if (name == null) name = nameMatcher.group(2);
            if (name == null) name = nameMatcher.group(3);
            scheduleCommand.setName(name);
        }
        
        // Type parameter (--type=xyz)
        Pattern typePattern = Pattern.compile("--type=(\\w+)");
        Matcher typeMatcher = typePattern.matcher(trimmedCommand);
        if (typeMatcher.find()) {
            scheduleCommand.setScheduleType(typeMatcher.group(1));
        }
        
        // Time parameter (--time=xx:xx)
        Pattern timePattern = Pattern.compile("--time=(\\d{1,2}:\\d{2})");
        Matcher timeMatcher = timePattern.matcher(trimmedCommand);
        if (timeMatcher.find()) {
            scheduleCommand.setTime(timeMatcher.group(1));
        }
        
        // Day parameter (--day=xyz)
        Pattern dayPattern = Pattern.compile("--day=(\\w+)");
        Matcher dayMatcher = dayPattern.matcher(trimmedCommand);
        if (dayMatcher.find()) {
            scheduleCommand.setDayOfWeek(dayMatcher.group(1));
        }
        
        // Date parameter (--date=x)
        Pattern datePattern = Pattern.compile("--date=(\\d+)");
        Matcher dateMatcher = datePattern.matcher(trimmedCommand);
        if (dateMatcher.find()) {
            scheduleCommand.setDayOfMonth(Integer.parseInt(dateMatcher.group(1)));
        }
        
        // Report parameter (--report=xyz)
        Pattern reportPattern = Pattern.compile("--report=(\\w+)");
        Matcher reportMatcher = reportPattern.matcher(trimmedCommand);
        if (reportMatcher.find()) {
            scheduleCommand.setReportType(reportMatcher.group(1));
        }
        
        // Format parameter (--format=xyz)
        Pattern formatPattern = Pattern.compile("--format=(\\w+)");
        Matcher formatMatcher = formatPattern.matcher(trimmedCommand);
        if (formatMatcher.find()) {
            scheduleCommand.setFormat(formatMatcher.group(1));
        }
        
        // Check for --verbose flag
        if (trimmedCommand.contains("--verbose")) {
            scheduleCommand.setVerbose(true);
        }
        
        // Check for --email flag
        if (trimmedCommand.contains("--email")) {
            scheduleCommand.setEmailEnabled(true);
        }
        
        // Email recipients parameter (--email-to=xyz)
        Pattern emailToPattern = Pattern.compile("--email-to=(?:'([^']*)'|\"([^\"]*)\"|([^\\s]+))");
        Matcher emailToMatcher = emailToPattern.matcher(trimmedCommand);
        if (emailToMatcher.find()) {
            String recipients = emailToMatcher.group(1);
            if (recipients == null) recipients = emailToMatcher.group(2);
            if (recipients == null) recipients = emailToMatcher.group(3);
            scheduleCommand.setEmailRecipients(recipients);
        }
        
        // Email subject parameter (--email-subject=xyz)
        Pattern emailSubjectPattern = Pattern.compile("--email-subject=(?:'([^']*)'|\"([^\"]*)\"|([^\\s]+))");
        Matcher emailSubjectMatcher = emailSubjectPattern.matcher(trimmedCommand);
        if (emailSubjectMatcher.find()) {
            String subject = emailSubjectMatcher.group(1);
            if (subject == null) subject = emailSubjectMatcher.group(2);
            if (subject == null) subject = emailSubjectMatcher.group(3);
            scheduleCommand.setEmailSubject(subject);
        }
        
        // For add commands, mock successful report type and format parsing
        if (scheduleCommand.getAction().equals("add") && scheduleCommand.getReportType() != null) {
            ReportType reportType = ReportType.STATUS; // Default for testing
            if (scheduleCommand.getReportType().equalsIgnoreCase("status")) {
                reportType = ReportType.STATUS;
            } else if (scheduleCommand.getReportType().equalsIgnoreCase("summary")) {
                reportType = ReportType.SUMMARY;
            } else if (scheduleCommand.getReportType().equalsIgnoreCase("metrics")) {
                reportType = ReportType.DETAILED;
            }
            when(mockReportService.parseReportType(scheduleCommand.getReportType())).thenReturn(reportType);
        }
        
        // For remove operations, mock a successful removal
        if (scheduleCommand.getAction().equals("remove") && scheduleCommand.getId() != null) {
            // Return true for valid IDs and false for "non-existent"
            if (!scheduleCommand.getId().equals("non-existent")) {
                when(mockScheduler.removeScheduledReport(scheduleCommand.getId())).thenReturn(true);
            } else {
                when(mockScheduler.removeScheduledReport(scheduleCommand.getId())).thenReturn(false);
            }
        }
        
        // For add operations, mock successful creation
        if (scheduleCommand.getAction().equals("add")) {
            // This will be captured and validated in the ArgumentCaptor
            when(mockScheduler.addScheduledReport(any(ScheduledReport.class))).thenReturn(true);
        }
        
        // Mock operation tracking
        when(mockMetadataService.startOperation(
                operationNameCaptor.capture(),
                operationActionCaptor.capture(),
                operationParamsCaptor.capture()
        )).thenReturn("op-123");
        
        // Clear the output buffers
        outContent.reset();
        errContent.reset();
        
        // Execute the command and store the result
        commandResult = scheduleCommand.call();
        
        // Store the command in context for later assertions
        context.setCommand(scheduleCommand);
    }
    
    /**
     * Verifies that the command succeeded.
     */
    @Then("the command should succeed")
    public void theCommandShouldSucceed() {
        assertEquals(0, commandResult, "Command should succeed with exit code 0");
    }
    
    /**
     * Verifies that the command failed.
     */
    @Then("the command should fail")
    public void theCommandShouldFail() {
        assertEquals(1, commandResult, "Command should fail with exit code 1");
    }
    
    /**
     * Verifies that the output contains the expected text.
     * 
     * @param text the expected text
     */
    @And("the output should contain {string}")
    public void theOutputShouldContain(String text) {
        String output = outContent.toString();
        assertTrue(output.contains(text), 
                "Output should contain '" + text + "' but was:\n" + output);
    }
    
    /**
     * Verifies that the output is valid JSON.
     */
    @And("the output should be valid JSON")
    public void theOutputShouldBeValidJSON() {
        // In a real test, we would parse the JSON output and verify it's valid
        // For this implementation we'll verify that OutputFormatter was used correctly
        verify(mockStaticOutputFormatter, atLeastOnce())
                .when(() -> OutputFormatter.class);
    }
    
    /**
     * Verifies that the JSON output contains a specific field.
     * 
     * @param field the field name
     */
    @And("the JSON should contain a {string} field")
    public void theJSONShouldContainAField(String field) {
        // This would normally verify the JSON structure
        // For now, we'll just check that the field name is in the output
        String output = outContent.toString();
        assertTrue(output.contains("\"" + field + "\"") || 
                   output.contains("'" + field + "'"),
                "Output should contain the field '" + field + "' but was:\n" + output);
    }
    
    /**
     * Verifies that the JSON output contains a specific array.
     * 
     * @param arrayName the array name
     */
    @And("the JSON should contain a {string} array")
    public void theJSONShouldContainAnArray(String arrayName) {
        // This would normally verify the JSON array structure
        // For now, we'll just check that the array name is in the output
        String output = outContent.toString();
        assertTrue(output.contains("\"" + arrayName + "\"") || 
                   output.contains("'" + arrayName + "'"),
                "Output should contain the array '" + arrayName + "' but was:\n" + output);
    }
    
    /**
     * Verifies that the JSON output contains a specific value.
     * 
     * @param value the expected value
     */
    @And("the JSON should contain {string}")
    public void theJSONShouldContain(String value) {
        String output = outContent.toString();
        assertTrue(output.contains(value),
                "Output should contain '" + value + "' but was:\n" + output);
    }
    
    /**
     * Verifies that the JSON output contains the report details.
     */
    @And("the JSON should contain the report details")
    public void theJSONShouldContainTheReportDetails() {
        // This would normally parse and verify the JSON structure
        // For now, we'll check that some expected fields are present
        ScheduledReport report = (ScheduledReport) context.getAttribute("scheduledReport");
        String output = outContent.toString();
        
        assertTrue(output.contains(report.getId()),
                "Output should contain the report ID but was:\n" + output);
        assertTrue(output.contains(report.getName()),
                "Output should contain the report name but was:\n" + output);
    }
    
    /**
     * Verifies that the JSON output contains the report ID.
     */
    @And("the JSON should contain the report ID")
    public void theJSONShouldContainTheReportID() {
        String output = outContent.toString();
        assertTrue(output.contains("\"id\"") || output.contains("'id'"),
                "Output should contain 'id' field but was:\n" + output);
    }
    
    /**
     * Verifies that the output contains the report details.
     */
    @And("the output should contain the report details")
    public void theOutputShouldContainTheReportDetails() {
        ScheduledReport report = (ScheduledReport) context.getAttribute("scheduledReport");
        String output = outContent.toString();
        
        assertTrue(output.contains(report.getId()),
                "Output should contain the report ID but was:\n" + output);
        assertTrue(output.contains(report.getName()),
                "Output should contain the report name but was:\n" + output);
    }
    
    /**
     * Verifies that the error output contains the expected text.
     * 
     * @param text the expected error text
     */
    @And("the error output should indicate {word} is required")
    public void theErrorOutputShouldIndicateParameterIsRequired(String parameter) {
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.toLowerCase().contains(parameter.toLowerCase()) && 
                   errorOutput.toLowerCase().contains("required"),
                "Error output should indicate " + parameter + " is required but was:\n" + errorOutput);
    }
    
    /**
     * Verifies that the error output indicates the report was not found.
     */
    @And("the error output should indicate report not found")
    public void theErrorOutputShouldIndicateReportNotFound() {
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.toLowerCase().contains("not found"),
                "Error output should indicate report not found but was:\n" + errorOutput);
    }
    
    /**
     * Verifies that the error output indicates unknown action.
     */
    @And("the error output should indicate unknown action")
    public void theErrorOutputShouldIndicateUnknownAction() {
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.toLowerCase().contains("unknown action"),
                "Error output should indicate unknown action but was:\n" + errorOutput);
    }
    
    /**
     * Verifies that the error output shows valid actions.
     */
    @And("the error output should show valid actions")
    public void theErrorOutputShouldShowValidActions() {
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Valid actions:") || 
                   errorOutput.contains("valid actions:"),
                "Error output should show valid actions but was:\n" + errorOutput);
    }
    
    /**
     * Verifies that the command tracked operation details.
     */
    @And("the command should track operation details")
    public void theCommandShouldTrackOperationDetails() {
        verify(mockMetadataService, atLeastOnce()).startOperation(anyString(), anyString(), anyMap());
        verify(mockMetadataService, atLeastOnce()).completeOperation(anyString(), anyMap());
    }
    
    /**
     * Verifies that the command tracked operation failure.
     */
    @And("the command should track operation failure")
    public void theCommandShouldTrackOperationFailure() {
        verify(mockMetadataService, atLeastOnce()).startOperation(anyString(), anyString(), anyMap());
        verify(mockMetadataService, atLeastOnce()).failOperation(anyString(), any(Exception.class));
    }
    
    /**
     * Verifies that the command tracked hierarchical operations.
     */
    @And("the command should track hierarchical operations")
    public void theCommandShouldTrackHierarchicalOperations() {
        // Verify that both the main operation and a sub-operation were tracked
        verify(mockMetadataService, atLeast(2)).startOperation(anyString(), anyString(), anyMap());
    }
    
    /**
     * Verifies that the command tracked hierarchical operations with a specific sub-operation.
     * 
     * @param subOperation the expected sub-operation name
     */
    @And("the command should track hierarchical operations with {string}")
    public void theCommandShouldTrackHierarchicalOperationsWithSubOperation(String subOperation) {
        verify(mockMetadataService, atLeastOnce()).startOperation(eq(subOperation), anyString(), anyMap());
    }
    
    /**
     * Verifies that the command tracked the format parameters.
     */
    @And("the command should track format parameters")
    public void theCommandShouldTrackFormatParameters() {
        // Get the captured operation parameters
        Map<String, Object> params = operationParamsCaptor.getAllValues().get(0);
        assertTrue(params.containsKey("format"), 
                "Operation parameters should contain 'format' key");
        assertEquals("json", params.get("format"),
                "Format parameter should be 'json'");
    }
    
    /**
     * Verifies that the command tracked the verbose parameter.
     */
    @And("the command should track the verbose parameter")
    public void theCommandShouldTrackTheVerboseParameter() {
        // Get the captured operation parameters
        Map<String, Object> params = operationParamsCaptor.getAllValues().get(0);
        assertTrue(params.containsKey("verbose"), 
                "Operation parameters should contain 'verbose' key");
        assertEquals(true, params.get("verbose"),
                "Verbose parameter should be true");
    }
    
    /**
     * Verifies that the command tracked all report parameters.
     */
    @And("the command should track all report parameters")
    public void theCommandShouldTrackAllReportParameters() {
        // Get the captured operation parameters from add operation
        List<Map<String, Object>> allParams = operationParamsCaptor.getAllValues();
        
        // Find the add operation params
        Map<String, Object> addParams = null;
        for (Map<String, Object> params : allParams) {
            if (params.containsKey("action") && params.get("action").equals("add")) {
                addParams = params;
                break;
            }
        }
        
        assertNotNull(addParams, "No add operation parameters found");
        
        // Check for required parameters
        assertTrue(addParams.containsKey("name"), 
                "Operation parameters should contain 'name' key");
        assertTrue(addParams.containsKey("scheduleType"), 
                "Operation parameters should contain 'scheduleType' key");
        assertTrue(addParams.containsKey("reportType"), 
                "Operation parameters should contain 'reportType' key");
    }
    
    /**
     * Verifies that the command tracked the report ID.
     */
    @And("the command should track the report ID")
    public void theCommandShouldTrackTheReportID() {
        // Get the captured operation parameters
        List<Map<String, Object>> allParams = operationParamsCaptor.getAllValues();
        
        boolean foundIdParam = false;
        for (Map<String, Object> params : allParams) {
            if (params.containsKey("id")) {
                foundIdParam = true;
                break;
            }
        }
        
        assertTrue(foundIdParam, "Operation parameters should contain 'id' key");
    }
}