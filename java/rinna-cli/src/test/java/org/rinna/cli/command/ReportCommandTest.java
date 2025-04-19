/*
 * Report Command Test for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.command;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.rinna.cli.report.ReportConfig;
import org.rinna.cli.report.ReportFormat;
import org.rinna.cli.report.ReportType;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockReportService;
import org.rinna.cli.service.ServiceManager;

/**
 * Test class for ReportCommand.
 */
@DisplayName("ReportCommand Unit Tests")
class ReportCommandTest {

    private static final String MOCK_OPERATION_ID = "mock-operation-id";
    private static final String MOCK_CONFIG_OP_ID = "mock-config-op-id";
    private static final String MOCK_GEN_OP_ID = "mock-gen-op-id";
    private static final String TEST_USER = "test-user";

    private ReportCommand command;
    private ServiceManager mockServiceManager;
    private MetadataService mockMetadataService;
    private MockReportService mockReportService;
    private ContextManager mockContextManager;
    
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    private ArgumentCaptor<Map<String, Object>> resultCaptor;
    
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;

    @BeforeEach
    void setUp() {
        // Set up mocks
        mockServiceManager = mock(ServiceManager.class);
        mockMetadataService = mock(MetadataService.class);
        mockReportService = mock(MockReportService.class);
        mockContextManager = mock(ContextManager.class);
        
        // Configure mocks
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getMockReportService()).thenReturn(mockReportService);
        
        // Set up argument captors
        paramsCaptor = ArgumentCaptor.forClass(Map.class);
        resultCaptor = ArgumentCaptor.forClass(Map.class);
        
        // Set up operation ID for metadata service
        when(mockMetadataService.startOperation(eq("report"), anyString(), any())).thenReturn(MOCK_OPERATION_ID);
        when(mockMetadataService.startOperation(eq("report-config"), anyString(), any())).thenReturn(MOCK_CONFIG_OP_ID);
        when(mockMetadataService.startOperation(eq("report-generate"), anyString(), any())).thenReturn(MOCK_GEN_OP_ID);
        
        // Configure report service mock to return default values
        when(mockReportService.parseReportType(anyString())).thenAnswer(invocation -> {
            String typeStr = invocation.getArgument(0);
            return typeStr != null ? ReportType.valueOf(typeStr.toUpperCase()) : ReportType.SUMMARY;
        });
        
        when(mockReportService.parseReportFormat(anyString())).thenAnswer(invocation -> {
            String formatStr = invocation.getArgument(0);
            return formatStr != null ? ReportFormat.valueOf(formatStr.toUpperCase()) : ReportFormat.TEXT;
        });
        
        when(mockReportService.generateReport(any(ReportConfig.class))).thenReturn(true);
        
        // Mock static context manager
        try (var mockedStatic = Mockito.mockStatic(ContextManager.class)) {
            mockedStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
            // Create command with mocked service manager
            command = new ReportCommand(mockServiceManager);
            command.setUsername(TEST_USER);
        }
        
        // Capture stdout and stderr
        originalOut = System.out;
        originalErr = System.err;
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @Test
    @DisplayName("Should track main operation when generating a default report")
    void shouldTrackMainOperationWhenGeneratingDefaultReport() {
        // Execute the command
        int result = command.call();
        
        // Verify result
        assertEquals(0, result);
        
        // Verify main operation tracking
        verify(mockMetadataService).startOperation(eq("report"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("summary", params.get("type"));
        assertEquals("text", params.get("format"));
        assertEquals(TEST_USER, params.get("username"));
        assertEquals(false, params.get("verbose"));
        
        // Verify operation completion tracking
        verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
        Map<String, Object> result_data = resultCaptor.getValue();
        assertEquals(true, result_data.get("success"));
        assertEquals(0, result_data.get("resultCode"));
    }
    
    @Test
    @DisplayName("Should track hierarchical operations with report config creation and generation")
    void shouldTrackHierarchicalOperationsForReportGeneration() {
        // Configure command
        command.setType("status");
        command.setFormat("html");
        command.setTitle("Test Report");
        
        // Execute the command
        int result = command.call();
        
        // Verify result
        assertEquals(0, result);
        
        // Verify main operation
        verify(mockMetadataService).startOperation(eq("report"), eq("READ"), paramsCaptor.capture());
        
        // Verify config creation operation
        verify(mockMetadataService).startOperation(eq("report-config"), eq("CREATE"), any());
        verify(mockMetadataService).completeOperation(eq(MOCK_CONFIG_OP_ID), argThat(map -> 
            map instanceof Map && ((Map<?, ?>)map).containsKey("reportType") && 
            "STATUS".equals(((Map<?, ?>)map).get("reportType"))
        ));
        
        // Verify report generation operation
        verify(mockMetadataService).startOperation(eq("report-generate"), eq("EXECUTE"), argThat(map -> 
            map instanceof Map && ((Map<?, ?>)map).containsKey("reportType") && 
            "STATUS".equals(((Map<?, ?>)map).get("reportType"))
        ));
        verify(mockMetadataService).completeOperation(eq(MOCK_GEN_OP_ID), argThat(map -> 
            map instanceof Map && ((Map<?, ?>)map).containsKey("success") && 
            Boolean.TRUE.equals(((Map<?, ?>)map).get("success"))
        ));
        
        // Verify main operation completion
        verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), argThat(map -> 
            map instanceof Map && ((Map<?, ?>)map).containsKey("success") && 
            Boolean.TRUE.equals(((Map<?, ?>)map).get("success"))
        ));
    }
    
    @Test
    @DisplayName("Should handle report generation failure with proper error tracking")
    void shouldHandleReportGenerationFailure() {
        // Configure the mock to return failure
        when(mockReportService.generateReport(any(ReportConfig.class))).thenReturn(false);
        
        // Execute the command
        int result = command.call();
        
        // Verify result
        assertEquals(1, result);
        
        // Verify generation operation tracking
        verify(mockMetadataService).completeOperation(eq(MOCK_GEN_OP_ID), argThat(map -> 
            map instanceof Map && ((Map<?, ?>)map).containsKey("success") && 
            Boolean.FALSE.equals(((Map<?, ?>)map).get("success"))
        ));
        
        // Verify main operation completion with error status
        verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), argThat(map -> 
            map instanceof Map && ((Map<?, ?>)map).containsKey("success") && 
            Boolean.FALSE.equals(((Map<?, ?>)map).get("success")) && 
            Integer.valueOf(1).equals(((Map<?, ?>)map).get("resultCode"))
        ));
        
        // Verify error message was output
        assertTrue(errorStream.toString().contains("Failed to generate report"));
    }
    
    @Test
    @DisplayName("Should track exception when the report service throws an exception")
    void shouldTrackExceptionWhenReportServiceThrowsException() {
        // Configure the mock to throw exception
        RuntimeException testException = new RuntimeException("Test exception");
        when(mockReportService.parseReportType(anyString())).thenThrow(testException);
        
        // Execute the command
        int result = command.call();
        
        // Verify result
        assertEquals(1, result);
        
        // Verify operation failure tracking
        verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), eq(testException));
        
        // Verify error message was output
        assertTrue(errorStream.toString().contains("Test exception"));
    }
    
    @Test
    @DisplayName("Should track operation with custom title and filters")
    void shouldTrackOperationWithCustomTitleAndFilters() {
        // Configure command with custom parameters
        command.setType("detailed");
        command.setTitle("Custom Report Title");
        command.addFilter("state", "IN_PROGRESS");
        command.addFilter("priority", "HIGH");
        
        // Execute the command
        command.call();
        
        // Verify operation tracking has custom parameters
        verify(mockMetadataService).startOperation(eq("report"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("detailed", params.get("type"));
        assertEquals("Custom Report Title", params.get("title"));
        
        // Verify report service is called with correct parameters
        ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
        verify(mockReportService).generateReport(configCaptor.capture());
        ReportConfig capturedConfig = configCaptor.getValue();
        assertEquals(ReportType.DETAILED, capturedConfig.getType());
        assertEquals("Custom Report Title", capturedConfig.getTitle());
        Map<String, String> filters = capturedConfig.getFilters();
        assertEquals("IN_PROGRESS", filters.get("state"));
        assertEquals("HIGH", filters.get("priority"));
    }
    
    @Nested
    @DisplayName("JSON Output Format Tests")
    class JsonOutputTests {
        
        @BeforeEach
        void setupJsonOutput() {
            command.setFormat("json");
        }
        
        @Test
        @DisplayName("Should output report result in JSON format")
        void shouldOutputReportResultInJsonFormat() {
            // Execute the command
            int result = command.call();
            
            // Verify result
            assertEquals(0, result);
            
            // Verify JSON output
            String output = outputStream.toString();
            assertTrue(output.contains("\"success\": true"));
            assertTrue(output.contains("\"type\": \"SUMMARY\""));
            assertTrue(output.contains("\"format\": \"JSON\""));
        }
        
        @Test
        @DisplayName("Should output error in JSON format when exception occurs")
        void shouldOutputErrorInJsonFormatWhenExceptionOccurs() {
            // Configure the mock to throw exception
            RuntimeException testException = new RuntimeException("Test JSON exception");
            when(mockReportService.parseReportType(anyString())).thenThrow(testException);
            
            // Execute the command
            int result = command.call();
            
            // Verify result
            assertEquals(1, result);
            
            // Verify JSON error output
            String output = outputStream.toString();
            assertTrue(output.contains("\"error\": \"Error executing report command\""));
            assertTrue(output.contains("\"message\": \"Test JSON exception\""));
        }
    }
    
    @Nested
    @DisplayName("Email Configuration Tests")
    class EmailConfigurationTests {
        
        @Test
        @DisplayName("Should configure email delivery in the report config")
        void shouldConfigureEmailDeliveryInTheReportConfig() {
            // Configure email settings
            command.setEmailEnabled(true);
            command.addEmailRecipient("test@example.com");
            command.addEmailRecipient("another@example.com");
            command.setEmailSubject("Test Report Subject");
            
            // Execute the command
            command.call();
            
            // Verify operation tracking includes email settings
            verify(mockMetadataService).startOperation(eq("report"), eq("READ"), paramsCaptor.capture());
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(true, params.get("emailEnabled"));
            
            // Verify report config has email settings
            ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
            verify(mockReportService).generateReport(configCaptor.capture());
            ReportConfig capturedConfig = configCaptor.getValue();
            assertTrue(capturedConfig.isEmailEnabled());
            List<String> recipients = capturedConfig.getEmailRecipients();
            assertEquals(2, recipients.size());
            assertTrue(recipients.contains("test@example.com"));
            assertTrue(recipients.contains("another@example.com"));
            assertEquals("Test Report Subject", capturedConfig.getEmailSubject());
        }
        
        @Test
        @DisplayName("Should parse comma-separated email recipients")
        void shouldParseCommaSeparatedEmailRecipients() {
            // Configure comma-separated email
            command.setEmailEnabled(true);
            command.setEmailRecipients("first@example.com, second@example.com,third@example.com");
            
            // Execute the command
            command.call();
            
            // Verify report config has all email recipients
            ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
            verify(mockReportService).generateReport(configCaptor.capture());
            ReportConfig capturedConfig = configCaptor.getValue();
            List<String> recipients = capturedConfig.getEmailRecipients();
            assertEquals(3, recipients.size());
            assertTrue(recipients.contains("first@example.com"));
            assertTrue(recipients.contains("second@example.com"));
            assertTrue(recipients.contains("third@example.com"));
        }
    }
    
    @Nested
    @DisplayName("Date Range Tests")
    class DateRangeTests {
        
        @Test
        @DisplayName("Should configure date range in the report config")
        void shouldConfigureDateRangeInTheReportConfig() {
            // Configure date range
            command.setStartDate("2025-01-01");
            command.setEndDate("2025-12-31");
            
            // Execute the command
            command.call();
            
            // Verify operation tracking includes date range
            verify(mockMetadataService).startOperation(eq("report"), eq("READ"), paramsCaptor.capture());
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals("2025-01-01", params.get("startDate"));
            assertEquals("2025-12-31", params.get("endDate"));
            
            // Verify report config has date range
            ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
            verify(mockReportService).generateReport(configCaptor.capture());
            ReportConfig capturedConfig = configCaptor.getValue();
            assertEquals(LocalDate.of(2025, 1, 1), capturedConfig.getStartDate());
            assertEquals(LocalDate.of(2025, 12, 31), capturedConfig.getEndDate());
        }
        
        @Test
        @DisplayName("Should handle invalid date formats")
        void shouldHandleInvalidDateFormats() {
            // Configure invalid date
            command.setStartDate("invalid-date");
            
            // Execute the command
            command.call();
            
            // Verify report config does not have start date
            ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
            verify(mockReportService).generateReport(configCaptor.capture());
            ReportConfig capturedConfig = configCaptor.getValue();
            assertNull(capturedConfig.getStartDate());
        }
    }
    
    @Nested
    @DisplayName("Output Path Tests")
    class OutputPathTests {
        
        @Test
        @DisplayName("Should configure output path in the report config")
        void shouldConfigureOutputPathInTheReportConfig() {
            // Configure output path
            command.setOutput("/test/path/report.html");
            command.setFormat("html");
            
            // Execute the command
            command.call();
            
            // Verify operation tracking includes output path
            verify(mockMetadataService).startOperation(eq("report"), eq("READ"), paramsCaptor.capture());
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals("/test/path/report.html", params.get("output"));
            
            // Verify report config has output path
            ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
            verify(mockReportService).generateReport(configCaptor.capture());
            ReportConfig capturedConfig = configCaptor.getValue();
            assertEquals("/test/path/report.html", capturedConfig.getOutputPath());
        }
        
        @Test
        @DisplayName("Should handle console output when path is null")
        void shouldHandleConsoleOutputWhenPathIsNull() {
            // No output path specified (null)
            
            // Execute the command
            command.call();
            
            // Verify report config has null output path
            ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
            verify(mockReportService).generateReport(configCaptor.capture());
            ReportConfig capturedConfig = configCaptor.getValue();
            assertNull(capturedConfig.getOutputPath());
        }
        
        @Test
        @DisplayName("Should handle console output when path is dash")
        void shouldHandleConsoleOutputWhenPathIsDash() {
            // Configure output path as dash
            command.setOutput("-");
            
            // Execute the command
            command.call();
            
            // Verify report config has null output path
            ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
            verify(mockReportService).generateReport(configCaptor.capture());
            ReportConfig capturedConfig = configCaptor.getValue();
            assertNull(capturedConfig.getOutputPath());
        }
    }
}