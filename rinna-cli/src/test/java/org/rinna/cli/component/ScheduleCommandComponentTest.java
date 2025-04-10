package org.rinna.cli.component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

/**
 * Component tests for ScheduleCommand.
 * These tests verify the integration with services and proper operation tracking.
 */
public class ScheduleCommandComponentTest {
    
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
    
    // Mock static objects
    private MockedStatic<ServiceManager> mockStaticServiceManager;
    private MockedStatic<ReportScheduler> mockStaticReportScheduler;
    private MockedStatic<OutputFormatter> mockStaticOutputFormatter;
    
    // Operation tracking
    private ArgumentCaptor<String> operationNameCaptor;
    private ArgumentCaptor<String> operationActionCaptor;
    private ArgumentCaptor<Map<String, Object>> operationParamsCaptor;
    
    @BeforeEach
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
        
        // Mock operation tracking
        when(mockMetadataService.startOperation(
                operationNameCaptor.capture(),
                operationActionCaptor.capture(),
                operationParamsCaptor.capture()
        )).thenReturn("op-123");
    }
    
    @AfterEach
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
     * Test listing scheduled reports.
     */
    @Test
    public void testListSchedules() {
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
        
        // Mock the getScheduledReports method
        when(mockScheduler.getScheduledReports()).thenReturn(scheduledReports);
        
        // Create and configure the command
        ScheduleCommand command = new ScheduleCommand(mockServiceManager);
        command.setAction("list");
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result, "Command should succeed with exit code 0");
        
        // Verify the output
        String output = outContent.toString();
        assertTrue(output.contains("Scheduled Reports"), 
                "Output should contain 'Scheduled Reports' but was:\n" + output);
        assertTrue(output.contains("Daily Status"), 
                "Output should contain 'Daily Status' but was:\n" + output);
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule"), eq("EXECUTE"), anyMap());
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule-list"), eq("READ"), anyMap());
        verify(mockMetadataService, times(2)).completeOperation(anyString(), anyMap());
    }
    
    /**
     * Test listing scheduled reports with JSON output.
     */
    @Test
    public void testListSchedulesWithJsonOutput() {
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
        
        // Mock the getScheduledReports method
        when(mockScheduler.getScheduledReports()).thenReturn(scheduledReports);
        
        // Create and configure the command
        ScheduleCommand command = new ScheduleCommand(mockServiceManager);
        command.setAction("list");
        command.setFormat("json");
        
        // Mock OutputFormatter for JSON output
        OutputFormatter mockFormatter = mock(OutputFormatter.class);
        mockStaticOutputFormatter.when(() -> new OutputFormatter(true)).thenReturn(mockFormatter);
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result, "Command should succeed with exit code 0");
        
        // Verify OutputFormatter was used for JSON output
        verify(mockStaticOutputFormatter, atLeastOnce()).when(() -> new OutputFormatter(true));
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule"), eq("EXECUTE"), anyMap());
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule-list"), eq("READ"), anyMap());
        verify(mockMetadataService, times(2)).completeOperation(anyString(), anyMap());
        
        // Verify format parameter was tracked
        List<Map<String, Object>> allParams = operationParamsCaptor.getAllValues();
        boolean foundFormatParam = false;
        for (Map<String, Object> params : allParams) {
            if (params.containsKey("format") && params.get("format").equals("json")) {
                foundFormatParam = true;
                break;
            }
        }
        assertTrue(foundFormatParam, "Format parameter should be tracked");
    }
    
    /**
     * Test adding a scheduled report.
     */
    @Test
    public void testAddSchedule() {
        // Create and configure the command
        ScheduleCommand command = new ScheduleCommand(mockServiceManager);
        command.setAction("add");
        command.setName("Test Report");
        command.setScheduleType("daily");
        command.setTime("10:00");
        command.setReportType("status");
        
        // Mock successful report type parsing
        when(mockReportService.parseReportType("status")).thenReturn(ReportType.STATUS);
        
        // Mock successful report addition
        when(mockScheduler.addScheduledReport(any(ScheduledReport.class))).thenReturn(true);
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result, "Command should succeed with exit code 0");
        
        // Verify the output
        String output = outContent.toString();
        assertTrue(output.contains("added successfully"), 
                "Output should contain 'added successfully' but was:\n" + output);
        
        // Verify report creation
        ArgumentCaptor<ScheduledReport> reportCaptor = ArgumentCaptor.forClass(ScheduledReport.class);
        verify(mockScheduler).addScheduledReport(reportCaptor.capture());
        
        // Verify report properties
        ScheduledReport capturedReport = reportCaptor.getValue();
        assertEquals("Test Report", capturedReport.getName(), "Report name should match");
        assertEquals(ScheduleType.DAILY, capturedReport.getScheduleType(), "Schedule type should match");
        assertEquals("10:00", capturedReport.getTime(), "Time should match");
        assertEquals(ReportType.STATUS, capturedReport.getConfig().getType(), "Report type should match");
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule"), eq("EXECUTE"), anyMap());
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule-add"), eq("CREATE"), anyMap());
        verify(mockMetadataService, times(2)).completeOperation(anyString(), anyMap());
    }
    
    /**
     * Test adding a scheduled report with missing required parameter.
     */
    @Test
    public void testAddScheduleWithMissingParameter() {
        // Create and configure the command with missing name
        ScheduleCommand command = new ScheduleCommand(mockServiceManager);
        command.setAction("add");
        command.setScheduleType("daily");
        command.setTime("10:00");
        command.setReportType("status");
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the command failed
        assertEquals(1, result, "Command should fail with exit code 1");
        
        // Verify the error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Name is required"), 
                "Error output should indicate name is required but was:\n" + errorOutput);
        
        // Verify operation tracking for failure
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule"), eq("EXECUTE"), anyMap());
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule-add"), eq("CREATE"), anyMap());
        verify(mockMetadataService, atLeastOnce()).failOperation(anyString(), any(Exception.class));
    }
    
    /**
     * Test removing a scheduled report.
     */
    @Test
    public void testRemoveSchedule() {
        // Create a scheduled report
        ScheduledReport report = new ScheduledReport();
        report.setId("schedule-123");
        report.setName("Test Report");
        
        // Mock the getScheduledReport method
        when(mockScheduler.getScheduledReport("schedule-123")).thenReturn(report);
        
        // Mock successful removal
        when(mockScheduler.removeScheduledReport("schedule-123")).thenReturn(true);
        
        // Create and configure the command
        ScheduleCommand command = new ScheduleCommand(mockServiceManager);
        command.setAction("remove");
        command.setId("schedule-123");
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result, "Command should succeed with exit code 0");
        
        // Verify the output
        String output = outContent.toString();
        assertTrue(output.contains("removed successfully"), 
                "Output should contain 'removed successfully' but was:\n" + output);
        
        // Verify removal was called
        verify(mockScheduler).removeScheduledReport("schedule-123");
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule"), eq("EXECUTE"), anyMap());
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule-remove"), eq("DELETE"), anyMap());
        verify(mockMetadataService, times(2)).completeOperation(anyString(), anyMap());
    }
    
    /**
     * Test showing a scheduled report.
     */
    @Test
    public void testShowSchedule() {
        // Create a scheduled report
        ScheduledReport report = new ScheduledReport();
        report.setId("schedule-123");
        report.setName("Test Report");
        report.setScheduleType(ScheduleType.DAILY);
        report.setTime("09:00");
        ReportConfig config = new ReportConfig();
        config.setType(ReportType.STATUS);
        config.setFormat(ReportFormat.TEXT);
        report.setConfig(config);
        
        // Mock the getScheduledReport method
        when(mockScheduler.getScheduledReport("schedule-123")).thenReturn(report);
        
        // Create and configure the command
        ScheduleCommand command = new ScheduleCommand(mockServiceManager);
        command.setAction("show");
        command.setId("schedule-123");
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result, "Command should succeed with exit code 0");
        
        // Verify the output
        String output = outContent.toString();
        assertTrue(output.contains("ID: schedule-123"), 
                "Output should contain report ID but was:\n" + output);
        assertTrue(output.contains("Test Report"), 
                "Output should contain report name but was:\n" + output);
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule"), eq("EXECUTE"), anyMap());
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule-show"), eq("READ"), anyMap());
        verify(mockMetadataService, times(2)).completeOperation(anyString(), anyMap());
    }
    
    /**
     * Test starting the scheduler.
     */
    @Test
    public void testStartScheduler() {
        // Create and configure the command
        ScheduleCommand command = new ScheduleCommand(mockServiceManager);
        command.setAction("start");
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result, "Command should succeed with exit code 0");
        
        // Verify the output
        String output = outContent.toString();
        assertTrue(output.contains("started"), 
                "Output should contain 'started' but was:\n" + output);
        
        // Verify scheduler.start() was called
        verify(mockScheduler).start();
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule"), eq("EXECUTE"), anyMap());
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule-start"), eq("EXECUTE"), anyMap());
        verify(mockMetadataService, times(2)).completeOperation(anyString(), anyMap());
    }
    
    /**
     * Test stopping the scheduler.
     */
    @Test
    public void testStopScheduler() {
        // Create and configure the command
        ScheduleCommand command = new ScheduleCommand(mockServiceManager);
        command.setAction("stop");
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result, "Command should succeed with exit code 0");
        
        // Verify the output
        String output = outContent.toString();
        assertTrue(output.contains("stopped"), 
                "Output should contain 'stopped' but was:\n" + output);
        
        // Verify scheduler.stop() was called
        verify(mockScheduler).stop();
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule"), eq("EXECUTE"), anyMap());
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule-stop"), eq("EXECUTE"), anyMap());
        verify(mockMetadataService, times(2)).completeOperation(anyString(), anyMap());
    }
    
    /**
     * Test handling unknown action.
     */
    @Test
    public void testUnknownAction() {
        // Create and configure the command
        ScheduleCommand command = new ScheduleCommand(mockServiceManager);
        command.setAction("unknown-action");
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the command failed
        assertEquals(1, result, "Command should fail with exit code 1");
        
        // Verify the error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Unknown action"), 
                "Error output should indicate unknown action but was:\n" + errorOutput);
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("schedule"), eq("EXECUTE"), anyMap());
        verify(mockMetadataService, atLeastOnce()).failOperation(anyString(), any(Exception.class));
    }
}