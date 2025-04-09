/**
 * Unit tests for ReportService
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.unit.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.base.UnitTest;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.report.*;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.ServiceManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ReportService.
 */
public class ReportServiceTest extends UnitTest {

    @Mock
    private ItemService itemService;
    
    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private EmailService emailService;
    
    @Mock
    private TemplateManager templateManager;
    
    @Mock
    private ReportGenerator reportGenerator;
    
    private ReportService reportService;
    
    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        
        // Mock ServiceManager.getInstance() to return our mock
        mockStatic(ServiceManager.class);
        when(ServiceManager.getInstance()).thenReturn(serviceManager);
        when(serviceManager.getItemService()).thenReturn(itemService);
        
        // Mock EmailService.getInstance() to return our mock
        mockStatic(EmailService.class);
        when(EmailService.getInstance()).thenReturn(emailService);
        
        // Mock TemplateManager.getInstance() to return our mock
        mockStatic(TemplateManager.class);
        when(TemplateManager.getInstance()).thenReturn(templateManager);
        
        // Create the report service with our mocked dependencies
        reportService = new ReportService() {
            @Override
            protected ReportGenerator createReportGenerator() {
                return reportGenerator;
            }
        };
    }
    
    @Test
    void testGenerateReport_Basic() {
        // Setup
        ReportConfig config = new ReportConfig(ReportType.SUMMARY, ReportFormat.TEXT);
        List<WorkItem> workItems = new ArrayList<>();
        when(itemService.getAllItems()).thenReturn(workItems);
        when(reportGenerator.generateReport(eq(config), eq(workItems))).thenReturn(true);
        
        // Execute
        boolean result = reportService.generateReport(config);
        
        // Verify
        assertTrue(result, "Report generation should succeed");
        verify(itemService).getAllItems();
        verify(reportGenerator).generateReport(eq(config), eq(workItems));
        verifyNoInteractions(emailService);
    }
    
    @Test
    void testGenerateReport_WithEmail() throws Exception {
        // Setup
        ReportConfig config = new ReportConfig(ReportType.SUMMARY, ReportFormat.HTML);
        config.setEmailEnabled(true);
        config.addEmailRecipient("test@example.com");
        config.setEmailSubject("Test Report");
        
        List<WorkItem> workItems = new ArrayList<>();
        when(itemService.getAllItems()).thenReturn(workItems);
        when(reportGenerator.generateReport(any(ReportConfig.class), eq(workItems))).thenReturn(true);
        when(emailService.sendReport(eq(config), anyString())).thenReturn(true);
        
        // Execute
        boolean result = reportService.generateReport(config);
        
        // Verify
        assertTrue(result, "Report generation with email should succeed");
        verify(itemService).getAllItems();
        verify(reportGenerator).generateReport(any(ReportConfig.class), eq(workItems));
        verify(emailService).sendReport(eq(config), anyString());
    }
    
    @Test
    void testGenerateReport_WithEmailAndOutputPath() throws Exception {
        // Setup
        ReportConfig config = new ReportConfig(ReportType.SUMMARY, ReportFormat.HTML);
        config.setOutputPath("/path/to/output.html");
        config.setEmailEnabled(true);
        config.addEmailRecipient("test@example.com");
        
        List<WorkItem> workItems = new ArrayList<>();
        when(itemService.getAllItems()).thenReturn(workItems);
        when(reportGenerator.generateReport(any(ReportConfig.class), eq(workItems))).thenReturn(true);
        when(emailService.sendReport(eq(config), anyString())).thenReturn(true);
        
        // Execute
        boolean result = reportService.generateReport(config);
        
        // Verify
        assertTrue(result, "Report generation with email and output path should succeed");
        verify(itemService).getAllItems();
        verify(reportGenerator, times(2)).generateReport(any(ReportConfig.class), eq(workItems));
        verify(emailService).sendReport(eq(config), anyString());
    }
    
    @Test
    void testGenerateReport_EmailFailure() throws Exception {
        // Setup
        ReportConfig config = new ReportConfig(ReportType.SUMMARY, ReportFormat.HTML);
        config.setEmailEnabled(true);
        config.addEmailRecipient("test@example.com");
        
        List<WorkItem> workItems = new ArrayList<>();
        when(itemService.getAllItems()).thenReturn(workItems);
        when(reportGenerator.generateReport(any(ReportConfig.class), eq(workItems))).thenReturn(true);
        when(emailService.sendReport(eq(config), anyString())).thenReturn(false);
        
        // Execute
        boolean result = reportService.generateReport(config);
        
        // Verify
        assertTrue(result, "Report generation should succeed even if email fails");
        verify(itemService).getAllItems();
        verify(reportGenerator).generateReport(any(ReportConfig.class), eq(workItems));
        verify(emailService).sendReport(eq(config), anyString());
    }
    
    @Test
    void testGenerateReport_WithTypeAndFormat() {
        // Setup
        ReportType type = ReportType.DETAILED;
        ReportFormat format = ReportFormat.JSON;
        
        List<WorkItem> workItems = new ArrayList<>();
        when(itemService.getAllItems()).thenReturn(workItems);
        when(reportGenerator.generateReport(any(ReportConfig.class), eq(workItems))).thenReturn(true);
        
        // Execute
        boolean result = reportService.generateReport(type, format);
        
        // Verify
        assertTrue(result, "Report generation with type and format should succeed");
        verify(itemService).getAllItems();
        verify(reportGenerator).generateReport(any(ReportConfig.class), eq(workItems));
        
        // Capture the config to verify it has the correct type and format
        ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
        verify(reportGenerator).generateReport(configCaptor.capture(), eq(workItems));
        
        ReportConfig capturedConfig = configCaptor.getValue();
        assertEquals(type, capturedConfig.getType(), "Config should have the correct type");
        assertEquals(format, capturedConfig.getFormat(), "Config should have the correct format");
    }
    
    @Test
    void testGenerateReport_WithTypeFormatAndPath() {
        // Setup
        ReportType type = ReportType.ASSIGNEE;
        ReportFormat format = ReportFormat.CSV;
        String outputPath = "/path/to/report.csv";
        
        List<WorkItem> workItems = new ArrayList<>();
        when(itemService.getAllItems()).thenReturn(workItems);
        when(reportGenerator.generateReport(any(ReportConfig.class), eq(workItems))).thenReturn(true);
        
        // Execute
        boolean result = reportService.generateReport(type, format, outputPath);
        
        // Verify
        assertTrue(result, "Report generation with type, format, and path should succeed");
        verify(itemService).getAllItems();
        verify(reportGenerator).generateReport(any(ReportConfig.class), eq(workItems));
        
        // Capture the config to verify it has the correct type, format, and path
        ArgumentCaptor<ReportConfig> configCaptor = ArgumentCaptor.forClass(ReportConfig.class);
        verify(reportGenerator).generateReport(configCaptor.capture(), eq(workItems));
        
        ReportConfig capturedConfig = configCaptor.getValue();
        assertEquals(type, capturedConfig.getType(), "Config should have the correct type");
        assertEquals(format, capturedConfig.getFormat(), "Config should have the correct format");
        assertEquals(outputPath, capturedConfig.getOutputPath(), "Config should have the correct output path");
    }
    
    @ParameterizedTest
    @CsvSource({
        "summary, SUMMARY",
        "SUMMARY, SUMMARY",
        "detailed, DETAILED",
        "det, DETAILED",
        "status, STATUS",
        "activity, ACTIVITY",
        "act, ACTIVITY"
    })
    void testParseReportType_ValidTypes(String input, ReportType expected) {
        assertEquals(expected, reportService.parseReportType(input), 
                "Should correctly parse '" + input + "' to " + expected);
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid", "unknown", "none"})
    void testParseReportType_InvalidTypes(String input) {
        assertEquals(ReportType.SUMMARY, reportService.parseReportType(input), 
                "Should default to SUMMARY for invalid input: '" + input + "'");
    }
    
    @Test
    void testParseReportFormat() {
        // This mostly delegates to ReportFormat.fromString which is tested separately
        assertEquals(ReportFormat.JSON, reportService.parseReportFormat("json"), 
                "Should correctly parse 'json' to JSON");
        assertEquals(ReportFormat.TEXT, reportService.parseReportFormat("invalid"), 
                "Should default to TEXT for invalid format");
    }
    
    @ParameterizedTest
    @CsvSource({
        "/path/to/dir/, SUMMARY, TEXT, /path/to/dir/rinna_summary_report.txt",
        "/path/to/dir, DETAILED, JSON, /path/to/dir/rinna_detailed_report.json",
        "report, SUMMARY, HTML, report.html",
        "custom.csv, STATUS, CSV, custom.csv",
        "/full/path/report.json, ACTIVITY, HTML, /full/path/report.json"
    })
    void testResolveOutputPath_ValidPaths(String basePath, ReportType type, ReportFormat format, String expected) {
        assertEquals(expected, reportService.resolveOutputPath(basePath, type, format), 
                "Should correctly resolve output path");
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"-"})
    void testResolveOutputPath_ConsolePaths(String basePath) {
        assertNull(reportService.resolveOutputPath(basePath, ReportType.SUMMARY, ReportFormat.TEXT), 
                "Should return null for console output");
    }
    
    @ParameterizedTest
    @CsvSource({
        "2025-01-01, 2025, 1, 1",
        "2024-12-31, 2024, 12, 31",
        "2023-06-15, 2023, 6, 15"
    })
    void testParseDate_ValidDates(String input, int year, int month, int day) {
        LocalDate expected = LocalDate.of(year, month, day);
        assertEquals(expected, reportService.parseDate(input), 
                "Should correctly parse date: " + input);
    }
    
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"invalid", "01/01/2025", "2025.01.01"})
    void testParseDate_InvalidDates(String input) {
        assertNull(reportService.parseDate(input), 
                "Should return null for invalid date: " + input);
    }
    
    @Test
    void testGetTemplate() {
        // Setup
        String name = "summary";
        ReportFormat format = ReportFormat.HTML;
        ReportTemplate mockTemplate = mock(ReportTemplate.class);
        
        when(templateManager.getTemplate(name, format)).thenReturn(mockTemplate);
        
        // Execute
        ReportTemplate result = reportService.getTemplate(name, format);
        
        // Verify
        assertEquals(mockTemplate, result, "Should return the template from the manager");
        verify(templateManager).getTemplate(name, format);
    }
    
    @Test
    void testGetTemplate_NotFound() throws Exception {
        // Setup
        String name = "nonexistent";
        ReportFormat format = ReportFormat.HTML;
        
        when(templateManager.getTemplate(name, format)).thenThrow(new Exception("Template not found"));
        
        // Execute
        ReportTemplate result = reportService.getTemplate(name, format);
        
        // Verify
        assertNull(result, "Should return null when template not found");
        verify(templateManager).getTemplate(name, format);
    }
    
    @Test
    void testApplyTemplate() {
        // Setup
        ReportConfig config = new ReportConfig();
        Map<String, Object> data = new HashMap<>();
        String expectedOutput = "Rendered template";
        
        when(templateManager.applyTemplate(config, data)).thenReturn(expectedOutput);
        
        // Execute
        String result = reportService.applyTemplate(config, data);
        
        // Verify
        assertEquals(expectedOutput, result, "Should return the output from the template manager");
        verify(templateManager).applyTemplate(config, data);
    }
    
    @Test
    void testSingleton() {
        // Clear the instance with reflection
        ReportService.resetInstance();
        
        // Get the singleton instance
        ReportService instance1 = ReportService.getInstance();
        ReportService instance2 = ReportService.getInstance();
        
        // Verify that they are the same instance
        assertSame(instance1, instance2, "getInstance should always return the same instance");
    }
}