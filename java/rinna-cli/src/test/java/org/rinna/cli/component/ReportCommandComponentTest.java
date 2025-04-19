/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.component;

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
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.command.ReportCommand;
import org.rinna.cli.report.ReportConfig;
import org.rinna.cli.report.ReportFormat;
import org.rinna.cli.report.ReportType;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockReportService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Component test for the ReportCommand class, focusing on:
 * - Proper integration with MetadataService for hierarchical operation tracking
 * - Integration with MockReportService for report generation
 * - Proper handling of different report types and formats
 * - Proper config creation and parameter passing
 * - Error handling and output formatting
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReportCommand Component Tests")
public class ReportCommandComponentTest {

    private static final String OPERATION_ID = "op-12345";
    private static final String CONFIG_OP_ID = "config-op-12345";
    private static final String GENERATE_OP_ID = "gen-op-12345";

    @Mock
    private ServiceManager mockServiceManager;

    @Mock
    private MetadataService mockMetadataService;

    @Mock
    private MockReportService mockReportService;

    @Mock
    private ContextManager mockContextManager;

    private ReportCommand reportCommand;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        // Configure mock service manager
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getMockReportService()).thenReturn(mockReportService);

        // Configure mock metadata service
        when(mockMetadataService.startOperation(eq("report"), eq("READ"), anyMap())).thenReturn(OPERATION_ID);
        when(mockMetadataService.startOperation(eq("report-config"), eq("CREATE"), anyMap())).thenReturn(CONFIG_OP_ID);
        when(mockMetadataService.startOperation(eq("report-generate"), eq("EXECUTE"), anyMap())).thenReturn(GENERATE_OP_ID);

        // Configure mock report service
        when(mockReportService.generateReport(any(ReportConfig.class))).thenReturn(true);
        when(mockReportService.parseReportType(anyString())).thenAnswer(invocation -> {
            String type = invocation.getArgument(0);
            try {
                return ReportType.valueOf(type.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ReportType.SUMMARY;
            }
        });
        when(mockReportService.parseReportFormat(anyString())).thenAnswer(invocation -> {
            String format = invocation.getArgument(0);
            try {
                return ReportFormat.valueOf(format.toUpperCase());
            } catch (IllegalArgumentException e) {
                return ReportFormat.TEXT;
            }
        });

        // Initialize the command with mocked services
        reportCommand = new ReportCommand(mockServiceManager);
        
        // Redirect stdout and stderr for output validation
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    @Nested
    @DisplayName("Service Integration Tests")
    class ServiceIntegrationTests {
        
        @Test
        @DisplayName("Should integrate with MetadataService for hierarchical operation tracking")
        void shouldIntegrateWithMetadataServiceForHierarchicalOperationTracking() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("report"), eq("READ"), any(Map.class));
                
                // Verify config creation operation tracking
                verify(mockMetadataService).startOperation(eq("report-config"), eq("CREATE"), any(Map.class));
                verify(mockMetadataService).completeOperation(eq(CONFIG_OP_ID), any(Map.class));
                
                // Verify generate operation tracking
                verify(mockMetadataService).startOperation(eq("report-generate"), eq("EXECUTE"), any(Map.class));
                verify(mockMetadataService).completeOperation(eq(GENERATE_OP_ID), any(Map.class));
                
                // Verify main operation completion
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), any(Map.class));
            }
        }
        
        @Test
        @DisplayName("Should pass operation parameters to MetadataService for tracking")
        void shouldPassOperationParametersToMetadataServiceForTracking() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                reportCommand.setType("status");
                reportCommand.setFormat("json");
                reportCommand.setTitle("Test Report");
                reportCommand.setProjectId("PROJ-123");
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify main operation parameters
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).startOperation(eq("report"), eq("READ"), paramsCaptor.capture());
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals("status", params.get("type"));
                assertEquals("json", params.get("format"));
                assertEquals("Test Report", params.get("title"));
                assertEquals("PROJ-123", params.get("projectId"));
                
                // Verify generate operation parameters
                ArgumentCaptor<Map<String, Object>> genParamsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).startOperation(eq("report-generate"), eq("EXECUTE"), genParamsCaptor.capture());
                Map<String, Object> genParams = genParamsCaptor.getValue();
                assertEquals("STATUS", genParams.get("reportType"));
                assertEquals("JSON", genParams.get("reportFormat"));
                
                // Verify generate operation result parameters
                ArgumentCaptor<Map<String, Object>> genResultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(GENERATE_OP_ID), genResultCaptor.capture());
                Map<String, Object> genResult = genResultCaptor.getValue();
                assertEquals(true, genResult.get("success"));
            }
        }
        
        @Test
        @DisplayName("Should integrate with ReportService for generating reports")
        void shouldIntegrateWithReportServiceForGeneratingReports() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                reportCommand.setType("summary");
                reportCommand.setFormat("html");
                reportCommand.setTitle("Test Summary Report");
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify report service interactions
                verify(mockReportService).parseReportType("summary");
                verify(mockReportService).parseReportFormat("html");
                
                // Verify report generation with correct config
                ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
                verify(mockReportService).generateReport(configCaptor.capture());
                
                ReportConfig capturedConfig = configCaptor.getValue();
                assertEquals(ReportType.SUMMARY, capturedConfig.getType());
                assertEquals(ReportFormat.HTML, capturedConfig.getFormat());
                assertEquals("Test Summary Report", capturedConfig.getTitle());
            }
        }
    }
    
    @Nested
    @DisplayName("Report Configuration Tests")
    class ReportConfigurationTests {
        
        @Test
        @DisplayName("Should create report config with correct type and format")
        void shouldCreateReportConfigWithCorrectTypeAndFormat() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                reportCommand.setType("detailed");
                reportCommand.setFormat("csv");
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify report configuration
                ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
                verify(mockReportService).generateReport(configCaptor.capture());
                
                ReportConfig capturedConfig = configCaptor.getValue();
                assertEquals(ReportType.DETAILED, capturedConfig.getType());
                assertEquals(ReportFormat.CSV, capturedConfig.getFormat());
            }
        }
        
        @Test
        @DisplayName("Should configure output path correctly")
        void shouldConfigureOutputPathCorrectly() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                reportCommand.setType("summary");
                reportCommand.setFormat("html");
                reportCommand.setOutput("/tmp/report.html");
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify report configuration
                ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
                verify(mockReportService).generateReport(configCaptor.capture());
                
                ReportConfig capturedConfig = configCaptor.getValue();
                assertEquals("/tmp/report.html", capturedConfig.getOutputPath());
            }
        }
        
        @Test
        @DisplayName("Should configure filtering options correctly")
        void shouldConfigureFilteringOptionsCorrectly() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                reportCommand.setType("summary");
                reportCommand.setStartDate("2025-01-01");
                reportCommand.setEndDate("2025-12-31");
                reportCommand.setProjectId("PROJ-123");
                reportCommand.addFilter("state", "IN_PROGRESS");
                reportCommand.addFilter("priority", "HIGH");
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify report configuration
                ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
                verify(mockReportService).generateReport(configCaptor.capture());
                
                ReportConfig capturedConfig = configCaptor.getValue();
                assertEquals(LocalDate.of(2025, 1, 1), capturedConfig.getStartDate());
                assertEquals(LocalDate.of(2025, 12, 31), capturedConfig.getEndDate());
                assertEquals("PROJ-123", capturedConfig.getProjectId());
                
                Map<String, String> filters = capturedConfig.getFilters();
                assertEquals("IN_PROGRESS", filters.get("state"));
                assertEquals("HIGH", filters.get("priority"));
            }
        }
        
        @Test
        @DisplayName("Should configure sorting options correctly")
        void shouldConfigureSortingOptionsCorrectly() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                reportCommand.setType("summary");
                reportCommand.setSortField("priority");
                reportCommand.setAscending(false);
                reportCommand.setGroupBy("state");
                reportCommand.setLimit(10);
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify report configuration
                ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
                verify(mockReportService).generateReport(configCaptor.capture());
                
                ReportConfig capturedConfig = configCaptor.getValue();
                assertEquals("priority", capturedConfig.getSortField());
                assertFalse(capturedConfig.isAscending());
                assertTrue(capturedConfig.isGroupByEnabled());
                assertEquals("state", capturedConfig.getGroupByField());
                assertEquals(10, capturedConfig.getMaxItems());
            }
        }
        
        @Test
        @DisplayName("Should configure email options correctly")
        void shouldConfigureEmailOptionsCorrectly() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                reportCommand.setType("summary");
                reportCommand.setEmailEnabled(true);
                reportCommand.addEmailRecipient("user1@example.com");
                reportCommand.addEmailRecipient("user2@example.com");
                reportCommand.setEmailSubject("Test Report");
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify report configuration
                ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
                verify(mockReportService).generateReport(configCaptor.capture());
                
                ReportConfig capturedConfig = configCaptor.getValue();
                assertTrue(capturedConfig.isEmailEnabled());
                List<String> recipients = capturedConfig.getEmailRecipients();
                assertEquals(2, recipients.size());
                assertTrue(recipients.contains("user1@example.com"));
                assertTrue(recipients.contains("user2@example.com"));
                assertEquals("Test Report", capturedConfig.getEmailSubject());
            }
        }
        
        @Test
        @DisplayName("Should configure template options correctly")
        void shouldConfigureTemplateOptionsCorrectly() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                reportCommand.setType("summary");
                reportCommand.setTemplateName("custom-template");
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify report configuration
                ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
                verify(mockReportService).generateReport(configCaptor.capture());
                
                ReportConfig capturedConfig = configCaptor.getValue();
                assertEquals("custom-template", capturedConfig.getTemplateName());
                assertTrue(capturedConfig.isUseTemplate());
            }
        }
        
        @Test
        @DisplayName("Should disable templates when noTemplate is set")
        void shouldDisableTemplatesWhenNoTemplateIsSet() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                reportCommand.setType("summary");
                reportCommand.setNoTemplate(true);
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify report configuration
                ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
                verify(mockReportService).generateReport(configCaptor.capture());
                
                ReportConfig capturedConfig = configCaptor.getValue();
                assertFalse(capturedConfig.isUseTemplate());
            }
        }
    }
    
    @Nested
    @DisplayName("Report Output Tests")
    class ReportOutputTests {
        
        @Test
        @DisplayName("Should display successful report generation with text format")
        void shouldDisplaySuccessfulReportGenerationWithTextFormat() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                reportCommand.setType("summary");
                reportCommand.setFormat("text");
                reportCommand.setOutput("/tmp/report.txt");
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify output
                String output = outputStream.toString();
                assertTrue(output.contains("Report generated successfully: /tmp/report.txt"));
            }
        }
        
        @Test
        @DisplayName("Should display successful report generation with JSON format")
        void shouldDisplaySuccessfulReportGenerationWithJsonFormat() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class);
                 MockedStatic<OutputFormatter> mockedOutputFormatter = Mockito.mockStatic(OutputFormatter.class)) {
                
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                mockedOutputFormatter.when(() -> OutputFormatter.toJson(any(), anyBoolean()))
                    .thenReturn("{\"success\":true,\"type\":\"SUMMARY\",\"format\":\"JSON\",\"outputPath\":\"/tmp/report.json\"}");
                
                reportCommand.setType("summary");
                reportCommand.setFormat("json");
                reportCommand.setOutput("/tmp/report.json");
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify output
                String output = outputStream.toString();
                assertTrue(output.contains("\"success\":true"));
                assertTrue(output.contains("\"type\":\"SUMMARY\""));
                assertTrue(output.contains("\"format\":\"JSON\""));
                assertTrue(output.contains("\"outputPath\":\"/tmp/report.json\""));
            }
        }
        
        @Test
        @DisplayName("Should display email delivery information when email is enabled")
        void shouldDisplayEmailDeliveryInformationWhenEmailIsEnabled() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                reportCommand.setType("summary");
                reportCommand.setFormat("text");
                reportCommand.setEmailEnabled(true);
                reportCommand.addEmailRecipient("user1@example.com");
                reportCommand.addEmailRecipient("user2@example.com");
                
                // Mock ReportConfig to have email enabled in the displayReportResult method
                when(mockReportService.generateReport(any(ReportConfig.class))).thenAnswer(invocation -> {
                    ReportConfig config = invocation.getArgument(0);
                    // Make sure email settings are preserved
                    assertTrue(config.isEmailEnabled());
                    assertEquals(2, config.getEmailRecipients().size());
                    return true;
                });
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify output includes email information
                String output = outputStream.toString();
                assertTrue(output.contains("Report sent to:"));
            }
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle report generation failure with text format")
        void shouldHandleReportGenerationFailureWithTextFormat() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockReportService.generateReport(any(ReportConfig.class))).thenReturn(false);
                
                reportCommand.setType("summary");
                reportCommand.setFormat("text");
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify error output
                String error = errorStream.toString();
                assertTrue(error.contains("Failed to generate report"));
                
                // Verify operation completion with failure
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals(false, result.get("success"));
                assertEquals(1, result.get("resultCode"));
            }
        }
        
        @Test
        @DisplayName("Should handle report generation failure with JSON format")
        void shouldHandleReportGenerationFailureWithJsonFormat() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class);
                 MockedStatic<OutputFormatter> mockedOutputFormatter = Mockito.mockStatic(OutputFormatter.class)) {
                
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                mockedOutputFormatter.when(() -> OutputFormatter.toJson(any(), anyBoolean()))
                    .thenReturn("{\"success\":false,\"type\":\"SUMMARY\",\"format\":\"JSON\"}");
                
                when(mockReportService.generateReport(any(ReportConfig.class))).thenReturn(false);
                
                reportCommand.setType("summary");
                reportCommand.setFormat("json");
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify JSON error output
                String output = outputStream.toString();
                assertTrue(output.contains("\"success\":false"));
            }
        }
        
        @Test
        @DisplayName("Should handle exceptions during report parsing with text format")
        void shouldHandleExceptionsDuringReportParsingWithTextFormat() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockReportService.parseReportType(anyString())).thenThrow(new RuntimeException("Invalid report type"));
                
                reportCommand.setType("invalid");
                reportCommand.setFormat("text");
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify error output
                String error = errorStream.toString();
                assertTrue(error.contains("Invalid report type"));
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(RuntimeException.class));
            }
        }
        
        @Test
        @DisplayName("Should handle exceptions during report parsing with JSON format")
        void shouldHandleExceptionsDuringReportParsingWithJsonFormat() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class);
                 MockedStatic<OutputFormatter> mockedOutputFormatter = Mockito.mockStatic(OutputFormatter.class)) {
                
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                mockedOutputFormatter.when(() -> OutputFormatter.toJson(any(), anyBoolean()))
                    .thenReturn("{\"error\":\"Error executing report command\",\"message\":\"Invalid report type\"}");
                
                when(mockReportService.parseReportType(anyString())).thenThrow(new RuntimeException("Invalid report type"));
                
                reportCommand.setType("invalid");
                reportCommand.setFormat("json");
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify JSON error output
                String output = outputStream.toString();
                assertTrue(output.contains("\"error\":\"Error executing report command\""));
                assertTrue(output.contains("\"message\":\"Invalid report type\""));
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(RuntimeException.class));
            }
        }
        
        @Test
        @DisplayName("Should handle operation tracking failures")
        void shouldHandleOperationTrackingFailures() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Make the config operation tracking fail
                when(mockMetadataService.startOperation(eq("report-config"), eq("CREATE"), anyMap()))
                    .thenThrow(new RuntimeException("Operation tracking failed"));
                
                reportCommand.setType("summary");
                
                // When
                int exitCode = reportCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(RuntimeException.class));
                
                // Verify error output
                String error = errorStream.toString();
                assertTrue(error.contains("Operation tracking failed"));
            }
        }
    }
}