/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.report.ReportConfig;
import org.rinna.cli.report.ReportFormat;
import org.rinna.cli.report.ReportScheduler;
import org.rinna.cli.report.ReportScheduler.ScheduleType;
import org.rinna.cli.report.ReportScheduler.ScheduledReport;
import org.rinna.cli.report.ReportType;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockReportService;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Test class for ScheduleCommand with focus on MetadataService integration.
 * Tests follow the ViewCommand pattern test approach.
 */
@ExtendWith(MockitoExtension.class)
public class ScheduleCommandTest {

    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private MockReportService mockReportService;
    
    @Mock
    private ConfigurationService mockConfigService;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private ContextManager mockContextManager;
    
    @Mock
    private ReportScheduler mockScheduler;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> resultCaptor;
    
    private ScheduleCommand command;
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private static final String TEST_USER = "testuser";
    private static final String MOCK_OPERATION_ID = "op-123";
    private static final String TEST_REPORT_ID = "test-report-id";
    private static final String TEST_REPORT_NAME = "Test Report";
    
    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Setup mock service dependencies
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getMockReportService()).thenReturn(mockReportService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
        
        // Setup metadata service tracking
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn(MOCK_OPERATION_ID);
        
        // Create the command with mocked dependencies
        command = new ScheduleCommand(mockServiceManager);
        command.setUsername(TEST_USER);
        
        // Reset output streams for each test
        outContent.reset();
        errContent.reset();
    }
    
    /**
     * Test successful operation tracking for listing reports.
     */
    @Nested
    @DisplayName("List Operation Tests")
    class ListOperationTests {
        
        @Test
        @DisplayName("Should track main operation when listing scheduled reports")
        void shouldTrackMainOperationWhenListingScheduledReports() {
            // Setup - default action is "list"
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                when(mockScheduler.getScheduledReports()).thenReturn(new ArrayList<>());
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(0, result);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("schedule"), eq("EXECUTE"), paramsCaptor.capture());
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("list", params.get("action"));
                assertEquals(TEST_USER, params.get("username"));
                assertEquals("text", params.get("format"));
                assertEquals(false, params.get("verbose"));
                
                // Verify sub-operation tracking
                verify(mockMetadataService).startOperation(eq("schedule-list"), eq("READ"), any());
                
                // Verify operation completion tracking
                verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
                Map<String, Object> result_data = resultCaptor.getValue();
                assertEquals(0, result_data.get("count"));
            }
        }
        
        @Test
        @DisplayName("Should handle and track errors when listing reports fails")
        void shouldHandleAndTrackErrorsWhenListingReportsFails() {
            // Setup
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                when(mockScheduler.getScheduledReports()).thenThrow(new RuntimeException("Failed to get reports"));
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(1, result);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("schedule"), eq("EXECUTE"), any());
                
                // Verify sub-operation tracking
                verify(mockMetadataService).startOperation(eq("schedule-list"), eq("READ"), any());
                
                // Verify operation failure tracking
                verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(RuntimeException.class));
                
                // Check error output
                String errorOutput = errContent.toString();
                assertTrue(errorOutput.contains("Error: Failed to get reports"));
            }
        }
        
        @Test
        @DisplayName("Should display scheduled reports in JSON format when specified")
        void shouldDisplayScheduledReportsInJsonFormatWhenSpecified() {
            // Setup
            command.setFormat("json");
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                // Create sample scheduled reports
                List<ScheduledReport> reports = new ArrayList<>();
                ScheduledReport report = new ScheduledReport();
                report.setId(TEST_REPORT_ID);
                report.setName(TEST_REPORT_NAME);
                report.setScheduleType(ScheduleType.DAILY);
                report.setTime("09:00");
                
                ReportConfig config = new ReportConfig();
                config.setType(ReportType.SUMMARY);
                config.setFormat(ReportFormat.TEXT);
                report.setConfig(config);
                
                reports.add(report);
                when(mockScheduler.getScheduledReports()).thenReturn(reports);
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(0, result);
                
                // Check JSON output
                String output = outContent.toString();
                assertTrue(output.contains("\"count\": 1"));
                assertTrue(output.contains("\"id\": \"" + TEST_REPORT_ID + "\""));
                assertTrue(output.contains("\"name\": \"" + TEST_REPORT_NAME + "\""));
                assertTrue(output.contains("\"scheduleType\": \"DAILY\""));
            }
        }
    }
    
    /**
     * Test successful operation tracking for adding reports.
     */
    @Nested
    @DisplayName("Add Operation Tests")
    class AddOperationTests {
        
        @Test
        @DisplayName("Should track main operation when adding a scheduled report")
        void shouldTrackMainOperationWhenAddingScheduledReport() {
            // Setup
            command.setAction("add");
            command.setName(TEST_REPORT_NAME);
            command.setScheduleType("daily");
            command.setTime("09:00");
            command.setReportType("summary");
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                // Set up the mockReportService to return a specific ReportType
                when(mockReportService.parseReportType("summary")).thenReturn(ReportType.SUMMARY);
                
                // Set up the mockScheduler to accept a report and return true
                doAnswer(invocation -> {
                    ScheduledReport report = invocation.getArgument(0);
                    // Set a fixed ID for testing
                    report.setId(TEST_REPORT_ID);
                    return true;
                }).when(mockScheduler).addScheduledReport(any(ScheduledReport.class));
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(0, result);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("schedule"), eq("EXECUTE"), paramsCaptor.capture());
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("add", params.get("action"));
                assertEquals(TEST_USER, params.get("username"));
                assertEquals(TEST_REPORT_NAME, params.get("name"));
                
                // Verify sub-operation tracking
                verify(mockMetadataService).startOperation(eq("schedule-add"), eq("CREATE"), paramsCaptor.capture());
                Map<String, Object> addParams = paramsCaptor.getValue();
                assertEquals("add", addParams.get("action"));
                assertEquals(TEST_REPORT_NAME, addParams.get("name"));
                assertEquals("daily", addParams.get("scheduleType"));
                assertEquals("summary", addParams.get("reportType"));
                
                // Verify scheduler interaction
                verify(mockScheduler).addScheduledReport(any(ScheduledReport.class));
                
                // Verify operation completion tracking
                verify(mockMetadataService, atLeastOnce()).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
                
                // Check the captured results - use anyOf to match either of the result maps
                boolean foundSuccessResult = false;
                boolean foundIdInResults = false;
                for (Map<String, Object> capturedResult : resultCaptor.getAllValues()) {
                    if (capturedResult.containsKey("success") && (Boolean) capturedResult.get("success")) {
                        foundSuccessResult = true;
                    }
                    if (capturedResult.containsKey("id") && TEST_REPORT_ID.equals(capturedResult.get("id"))) {
                        foundIdInResults = true;
                    }
                }
                assertTrue(foundSuccessResult, "Should have a result map with success=true");
                assertTrue(foundIdInResults, "Should have a result map with the report ID");
            }
        }
        
        @Test
        @DisplayName("Should handle and track validation errors when adding a report")
        void shouldHandleAndTrackValidationErrorsWhenAddingReport() {
            // Setup - missing required parameters
            command.setAction("add");
            // No name, schedule type, time, or report type
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(1, result);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("schedule"), eq("EXECUTE"), any());
                
                // Verify sub-operation tracking
                verify(mockMetadataService).startOperation(eq("schedule-add"), eq("CREATE"), any());
                
                // Verify operation failure tracking
                verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalArgumentException.class));
                
                // Check error output
                String errorOutput = errContent.toString();
                assertTrue(errorOutput.contains("Error: Name is required"));
            }
        }
        
        @Test
        @DisplayName("Should track operation when adding report with email settings")
        void shouldTrackOperationWhenAddingReportWithEmailSettings() {
            // Setup
            command.setAction("add");
            command.setName(TEST_REPORT_NAME);
            command.setScheduleType("daily");
            command.setTime("09:00");
            command.setReportType("summary");
            command.setEmailEnabled(true);
            command.setEmailRecipients("user1@example.com,user2@example.com");
            command.setEmailSubject("Daily Summary Report");
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                // Set up the mockReportService to return a specific ReportType
                when(mockReportService.parseReportType("summary")).thenReturn(ReportType.SUMMARY);
                
                // Capture the ScheduledReport passed to addScheduledReport
                ArgumentCaptor<ScheduledReport> reportCaptor = ArgumentCaptor.forClass(ScheduledReport.class);
                when(mockScheduler.addScheduledReport(reportCaptor.capture())).thenReturn(true);
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(0, result);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("schedule"), eq("EXECUTE"), any());
                
                // Verify sub-operation tracking
                verify(mockMetadataService).startOperation(eq("schedule-add"), eq("CREATE"), any());
                
                // Verify scheduler interaction
                verify(mockScheduler).addScheduledReport(any(ScheduledReport.class));
                
                // Verify email settings
                ScheduledReport capturedReport = reportCaptor.getValue();
                ReportConfig capturedConfig = capturedReport.getConfig();
                assertTrue(capturedConfig.isEmailEnabled());
                assertEquals(2, capturedConfig.getEmailRecipients().size());
                assertTrue(capturedConfig.getEmailRecipients().contains("user1@example.com"));
                assertTrue(capturedConfig.getEmailRecipients().contains("user2@example.com"));
                assertEquals("Daily Summary Report", capturedConfig.getEmailSubject());
            }
        }
    }
    
    /**
     * Test successful operation tracking for removing reports.
     */
    @Nested
    @DisplayName("Remove Operation Tests")
    class RemoveOperationTests {
        
        @Test
        @DisplayName("Should track main operation when removing a scheduled report")
        void shouldTrackMainOperationWhenRemovingScheduledReport() {
            // Setup
            command.setAction("remove");
            command.setId(TEST_REPORT_ID);
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                // Create a sample report to return
                ScheduledReport report = new ScheduledReport();
                report.setId(TEST_REPORT_ID);
                report.setName(TEST_REPORT_NAME);
                
                when(mockScheduler.getScheduledReport(TEST_REPORT_ID)).thenReturn(report);
                when(mockScheduler.removeScheduledReport(TEST_REPORT_ID)).thenReturn(true);
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(0, result);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("schedule"), eq("EXECUTE"), paramsCaptor.capture());
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("remove", params.get("action"));
                assertEquals(TEST_USER, params.get("username"));
                assertEquals(TEST_REPORT_ID, params.get("id"));
                
                // Verify sub-operation tracking
                verify(mockMetadataService).startOperation(eq("schedule-remove"), eq("DELETE"), paramsCaptor.capture());
                Map<String, Object> removeParams = paramsCaptor.getValue();
                assertEquals("remove", removeParams.get("action"));
                assertEquals(TEST_REPORT_ID, removeParams.get("id"));
                
                // Verify scheduler interaction
                verify(mockScheduler).removeScheduledReport(TEST_REPORT_ID);
                
                // Verify operation completion tracking
                verify(mockMetadataService, atLeastOnce()).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
                
                // Check the captured results
                boolean foundSuccessResult = false;
                boolean foundIdInResults = false;
                for (Map<String, Object> capturedResult : resultCaptor.getAllValues()) {
                    if (capturedResult.containsKey("success") && (Boolean) capturedResult.get("success")) {
                        foundSuccessResult = true;
                    }
                    if (capturedResult.containsKey("id") && TEST_REPORT_ID.equals(capturedResult.get("id"))) {
                        foundIdInResults = true;
                    }
                }
                assertTrue(foundSuccessResult, "Should have a result map with success=true");
                assertTrue(foundIdInResults, "Should have a result map with the report ID");
            }
        }
        
        @Test
        @DisplayName("Should handle and track validation errors when removing a report")
        void shouldHandleAndTrackValidationErrorsWhenRemovingReport() {
            // Setup - missing required parameter
            command.setAction("remove");
            // No ID
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(1, result);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("schedule"), eq("EXECUTE"), any());
                
                // Verify sub-operation tracking
                verify(mockMetadataService).startOperation(eq("schedule-remove"), eq("DELETE"), any());
                
                // Verify operation failure tracking
                verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalArgumentException.class));
                
                // Check error output
                String errorOutput = errContent.toString();
                assertTrue(errorOutput.contains("Error: ID is required"));
            }
        }
        
        @Test
        @DisplayName("Should handle and track not found errors when removing a report")
        void shouldHandleAndTrackNotFoundErrorsWhenRemovingReport() {
            // Setup
            command.setAction("remove");
            command.setId(TEST_REPORT_ID);
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                when(mockScheduler.getScheduledReport(TEST_REPORT_ID)).thenReturn(null);
                when(mockScheduler.removeScheduledReport(TEST_REPORT_ID)).thenReturn(false);
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(1, result);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("schedule"), eq("EXECUTE"), any());
                
                // Verify sub-operation tracking
                verify(mockMetadataService).startOperation(eq("schedule-remove"), eq("DELETE"), any());
                
                // Verify operation failure tracking
                verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalStateException.class));
                
                // Check error output
                String errorOutput = errContent.toString();
                assertTrue(errorOutput.contains("Error: Scheduled report not found with ID"));
            }
        }
    }
    
    /**
     * Test successful operation tracking for showing report details.
     */
    @Nested
    @DisplayName("Show Operation Tests")
    class ShowOperationTests {
        
        @Test
        @DisplayName("Should track main operation when showing a scheduled report")
        void shouldTrackMainOperationWhenShowingScheduledReport() {
            // Setup
            command.setAction("show");
            command.setId(TEST_REPORT_ID);
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                // Create a sample report to return
                ScheduledReport report = new ScheduledReport();
                report.setId(TEST_REPORT_ID);
                report.setName(TEST_REPORT_NAME);
                report.setScheduleType(ScheduleType.DAILY);
                report.setTime("09:00");
                
                ReportConfig config = new ReportConfig();
                config.setType(ReportType.SUMMARY);
                config.setFormat(ReportFormat.TEXT);
                report.setConfig(config);
                
                when(mockScheduler.getScheduledReport(TEST_REPORT_ID)).thenReturn(report);
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(0, result);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("schedule"), eq("EXECUTE"), paramsCaptor.capture());
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("show", params.get("action"));
                assertEquals(TEST_USER, params.get("username"));
                assertEquals(TEST_REPORT_ID, params.get("id"));
                
                // Verify sub-operation tracking
                verify(mockMetadataService).startOperation(eq("schedule-show"), eq("READ"), paramsCaptor.capture());
                Map<String, Object> showParams = paramsCaptor.getValue();
                assertEquals("show", showParams.get("action"));
                assertEquals(TEST_REPORT_ID, showParams.get("id"));
                
                // Verify scheduler interaction
                verify(mockScheduler).getScheduledReport(TEST_REPORT_ID);
                
                // Verify operation completion tracking
                verify(mockMetadataService, atLeastOnce()).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
                
                // Check the captured results
                boolean foundNameInResults = false;
                boolean foundTypeInResults = false;
                for (Map<String, Object> capturedResult : resultCaptor.getAllValues()) {
                    if (capturedResult.containsKey("name") && TEST_REPORT_NAME.equals(capturedResult.get("name"))) {
                        foundNameInResults = true;
                    }
                    if (capturedResult.containsKey("type") && "DAILY".equals(capturedResult.get("type"))) {
                        foundTypeInResults = true;
                    }
                }
                assertTrue(foundNameInResults, "Should have a result map with the report name");
                assertTrue(foundTypeInResults, "Should have a result map with the report type");
            }
        }
        
        @Test
        @DisplayName("Should handle and track validation errors when showing a report")
        void shouldHandleAndTrackValidationErrorsWhenShowingReport() {
            // Setup - missing required parameter
            command.setAction("show");
            // No ID
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(1, result);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("schedule"), eq("EXECUTE"), any());
                
                // Verify sub-operation tracking
                verify(mockMetadataService).startOperation(eq("schedule-show"), eq("READ"), any());
                
                // Verify operation failure tracking
                verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalArgumentException.class));
                
                // Check error output
                String errorOutput = errContent.toString();
                assertTrue(errorOutput.contains("Error: ID is required"));
            }
        }
        
        @Test
        @DisplayName("Should handle and track not found errors when showing a report")
        void shouldHandleAndTrackNotFoundErrorsWhenShowingReport() {
            // Setup
            command.setAction("show");
            command.setId(TEST_REPORT_ID);
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                when(mockScheduler.getScheduledReport(TEST_REPORT_ID)).thenReturn(null);
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(1, result);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("schedule"), eq("EXECUTE"), any());
                
                // Verify sub-operation tracking
                verify(mockMetadataService).startOperation(eq("schedule-show"), eq("READ"), any());
                
                // Verify operation failure tracking
                verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalArgumentException.class));
                
                // Check error output
                String errorOutput = errContent.toString();
                assertTrue(errorOutput.contains("Error: Scheduled report not found with ID"));
            }
        }
    }
    
    /**
     * Test successful operation tracking for starting and stopping the scheduler.
     */
    @Nested
    @DisplayName("Scheduler Control Tests")
    class SchedulerControlTests {
        
        @Test
        @DisplayName("Should track main operation when starting the scheduler")
        void shouldTrackMainOperationWhenStartingScheduler() {
            // Setup
            command.setAction("start");
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                List<ScheduledReport> reports = new ArrayList<>();
                reports.add(new ScheduledReport());
                when(mockScheduler.getScheduledReports()).thenReturn(reports);
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(0, result);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("schedule"), eq("EXECUTE"), paramsCaptor.capture());
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("start", params.get("action"));
                assertEquals(TEST_USER, params.get("username"));
                
                // Verify sub-operation tracking
                verify(mockMetadataService).startOperation(eq("schedule-start"), eq("EXECUTE"), paramsCaptor.capture());
                Map<String, Object> startParams = paramsCaptor.getValue();
                assertEquals("start", startParams.get("action"));
                
                // Verify scheduler interaction
                verify(mockScheduler).start();
                
                // Verify operation completion tracking
                verify(mockMetadataService, atLeastOnce()).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
                
                // Check the captured results
                boolean foundStatusInResults = false;
                boolean foundReportCountInResults = false;
                for (Map<String, Object> capturedResult : resultCaptor.getAllValues()) {
                    if (capturedResult.containsKey("status") && "started".equals(capturedResult.get("status"))) {
                        foundStatusInResults = true;
                    }
                    if (capturedResult.containsKey("reportCount") && Integer.valueOf(1).equals(capturedResult.get("reportCount"))) {
                        foundReportCountInResults = true;
                    }
                }
                assertTrue(foundStatusInResults, "Should have a result map with status=started");
                assertTrue(foundReportCountInResults, "Should have a result map with reportCount=1");
            }
        }
        
        @Test
        @DisplayName("Should track main operation when stopping the scheduler")
        void shouldTrackMainOperationWhenStoppingScheduler() {
            // Setup
            command.setAction("stop");
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(0, result);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("schedule"), eq("EXECUTE"), paramsCaptor.capture());
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("stop", params.get("action"));
                assertEquals(TEST_USER, params.get("username"));
                
                // Verify sub-operation tracking
                verify(mockMetadataService).startOperation(eq("schedule-stop"), eq("EXECUTE"), paramsCaptor.capture());
                Map<String, Object> stopParams = paramsCaptor.getValue();
                assertEquals("stop", stopParams.get("action"));
                
                // Verify scheduler interaction
                verify(mockScheduler).stop();
                
                // Verify operation completion tracking
                verify(mockMetadataService, atLeastOnce()).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
                
                // Check the captured results
                boolean foundStatusInResults = false;
                for (Map<String, Object> capturedResult : resultCaptor.getAllValues()) {
                    if (capturedResult.containsKey("status") && "stopped".equals(capturedResult.get("status"))) {
                        foundStatusInResults = true;
                    }
                }
                assertTrue(foundStatusInResults, "Should have a result map with status=stopped");
            }
        }
        
        @Test
        @DisplayName("Should handle and track errors when start operation fails")
        void shouldHandleAndTrackErrorsWhenStartOperationFails() {
            // Setup
            command.setAction("start");
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                doThrow(new RuntimeException("Failed to start scheduler")).when(mockScheduler).start();
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(1, result);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("schedule"), eq("EXECUTE"), any());
                
                // Verify sub-operation tracking
                verify(mockMetadataService).startOperation(eq("schedule-start"), eq("EXECUTE"), any());
                
                // Verify operation failure tracking
                verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(RuntimeException.class));
                
                // Check error output
                String errorOutput = errContent.toString();
                assertTrue(errorOutput.contains("Error: Failed to start scheduler"));
            }
        }
    }
    
    /**
     * Test handling of unknown actions.
     */
    @Nested
    @DisplayName("Unknown Action Tests")
    class UnknownActionTests {
        
        @Test
        @DisplayName("Should track operation when action is unknown")
        void shouldTrackOperationWhenActionIsUnknown() {
            // Setup
            command.setAction("invalid");
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(1, result);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("schedule"), eq("EXECUTE"), paramsCaptor.capture());
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("invalid", params.get("action"));
                
                // Verify operation failure tracking
                verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalArgumentException.class));
                
                // Check error output
                String errorOutput = errContent.toString();
                assertTrue(errorOutput.contains("Error: Unknown action: invalid"));
                assertTrue(errorOutput.contains("Valid actions: list, add, remove, start, stop, show"));
            }
        }
    }
    
    /**
     * Test weekly and monthly schedule validations
     */
    @Nested
    @DisplayName("Schedule Validation Tests")
    class ScheduleValidationTests {
        
        @Test
        @DisplayName("Should validate day of week for weekly schedules")
        void shouldValidateDayOfWeekForWeeklySchedules() {
            // Setup
            command.setAction("add");
            command.setName(TEST_REPORT_NAME);
            command.setScheduleType("weekly");
            command.setTime("09:00");
            command.setReportType("summary");
            // Missing day of week
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                // Set up the mockReportService to return a specific ReportType
                when(mockReportService.parseReportType("summary")).thenReturn(ReportType.SUMMARY);
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(1, result);
                
                // Verify operation failure tracking
                verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalArgumentException.class));
                
                // Check error output
                String errorOutput = errContent.toString();
                assertTrue(errorOutput.contains("Error: Day of week is required for weekly schedules"));
            }
        }
        
        @Test
        @DisplayName("Should validate day of month for monthly schedules")
        void shouldValidateDayOfMonthForMonthlySchedules() {
            // Setup
            command.setAction("add");
            command.setName(TEST_REPORT_NAME);
            command.setScheduleType("monthly");
            command.setTime("09:00");
            command.setReportType("summary");
            // Missing day of month
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                // Set up the mockReportService to return a specific ReportType
                when(mockReportService.parseReportType("summary")).thenReturn(ReportType.SUMMARY);
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(1, result);
                
                // Verify operation failure tracking
                verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalArgumentException.class));
                
                // Check error output
                String errorOutput = errContent.toString();
                assertTrue(errorOutput.contains("Error: Day of month is required for monthly schedules"));
            }
        }
        
        @Test
        @DisplayName("Should add report with valid weekly schedule")
        void shouldAddReportWithValidWeeklySchedule() {
            // Setup
            command.setAction("add");
            command.setName(TEST_REPORT_NAME);
            command.setScheduleType("weekly");
            command.setTime("09:00");
            command.setDayOfWeek("monday");
            command.setReportType("summary");
            
            try (MockedStatic<ReportScheduler> schedulerMockedStatic = Mockito.mockStatic(ReportScheduler.class)) {
                schedulerMockedStatic.when(ReportScheduler::getInstance).thenReturn(mockScheduler);
                
                // Set up the mockReportService to return a specific ReportType
                when(mockReportService.parseReportType("summary")).thenReturn(ReportType.SUMMARY);
                
                // Capture the ScheduledReport passed to addScheduledReport
                ArgumentCaptor<ScheduledReport> reportCaptor = ArgumentCaptor.forClass(ScheduledReport.class);
                when(mockScheduler.addScheduledReport(reportCaptor.capture())).thenReturn(true);
                
                // Execute
                int result = command.call();
                
                // Verify
                assertEquals(0, result);
                
                // Verify scheduler interaction
                verify(mockScheduler).addScheduledReport(any(ScheduledReport.class));
                
                // Verify report settings
                ScheduledReport capturedReport = reportCaptor.getValue();
                assertEquals(ScheduleType.WEEKLY, capturedReport.getScheduleType());
                assertEquals(DayOfWeek.MONDAY, capturedReport.getDayOfWeek());
                assertEquals("09:00", capturedReport.getTime());
            }
        }
    }
}