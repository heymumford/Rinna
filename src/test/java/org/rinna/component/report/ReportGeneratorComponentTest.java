/**
 * Component tests for ReportGenerator
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.component.report;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rinna.base.ComponentTest;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.report.ReportConfig;
import org.rinna.cli.report.ReportFormat;
import org.rinna.cli.report.ReportGenerator;
import org.rinna.cli.report.ReportTemplate;
import org.rinna.cli.report.ReportType;
import org.rinna.cli.report.TemplateManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Component tests for ReportGenerator.
 */
public class ReportGeneratorComponentTest extends ComponentTest {
    
    @TempDir
    Path tempDir;
    
    private Path templateDir;
    private Path outputDir;
    private ReportGenerator reportGenerator;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private List<WorkItem> testItems;
    private String originalTemplatesPath;
    
    @BeforeEach
    void setUp() throws IOException {
        // Save original templates path
        originalTemplatesPath = System.getProperty("templates.path");
        
        // Create template directories
        templateDir = tempDir.resolve("templates/reports");
        Files.createDirectories(templateDir);
        
        // Create output directory
        outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        
        // Set templates path for tests
        System.setProperty("templates.path", tempDir.toString());
        
        // Create default templates
        ReportTemplate.createDefaultTemplates();
        
        // Create test data
        testItems = createTestWorkItems();
        
        // Redirect stdout for output testing
        System.setOut(new PrintStream(outputStream));
        
        // Create report generator
        reportGenerator = new ReportGenerator();
    }
    
    @AfterEach
    void tearDown() {
        // Restore original stdout
        System.setOut(originalOut);
        
        // Restore original templates path
        if (originalTemplatesPath != null) {
            System.setProperty("templates.path", originalTemplatesPath);
        } else {
            System.clearProperty("templates.path");
        }
    }
    
    private List<WorkItem> createTestWorkItems() {
        List<WorkItem> items = new ArrayList<>();
        
        // Create test work items
        for (int i = 1; i <= 6; i++) {
            WorkItem item = new WorkItem();
            item.setId("WI-" + String.format("%02d", i));
            item.setTitle("Test Item " + i);
            
            // Set type - alternate between TASK and BUG
            item.setType(i % 2 == 0 ? WorkItemType.TASK : WorkItemType.BUG);
            
            // Set state - cycle through states
            switch (i % 3) {
                case 0:
                    item.setState(WorkflowState.DONE);
                    break;
                case 1:
                    item.setState(WorkflowState.TODO);
                    break;
                case 2:
                    item.setState(WorkflowState.IN_PROGRESS);
                    break;
            }
            
            // Set priority - cycle through priorities
            switch (i % 3) {
                case 0:
                    item.setPriority(Priority.LOW);
                    break;
                case 1:
                    item.setPriority(Priority.HIGH);
                    break;
                case 2:
                    item.setPriority(Priority.MEDIUM);
                    break;
            }
            
            // Set assignee for some items
            if (i % 3 != 0) {
                item.setAssignee("user" + (i % 3));
            }
            
            // Set dates
            item.setCreatedAt(LocalDateTime.now().minusDays(10));
            item.setUpdatedAt(LocalDateTime.now().minusDays(i));
            
            // Add description
            item.setDescription("Description for item " + i);
            
            // Add item to list
            items.add(item);
        }
        
        return items;
    }
    
    @Test
    void testGenerateTextReport() {
        // Create config
        ReportConfig config = ReportConfig.createDefault(ReportType.SUMMARY);
        config.setFormat(ReportFormat.TEXT);
        
        // Generate report
        boolean success = reportGenerator.generateReport(config, testItems);
        
        // Verify
        assertTrue(success, "Report generation should succeed");
        
        String output = outputStream.toString();
        assertFalse(output.isEmpty(), "Output should not be empty");
        assertTrue(output.contains("Work Item Summary Report"), "Output should contain report title");
        assertTrue(output.contains("Total Items: 6"), "Output should contain total items count");
        assertTrue(output.contains("TASK"), "Output should contain task type");
        assertTrue(output.contains("BUG"), "Output should contain bug type");
    }
    
    @Test
    void testGenerateHtmlReport() throws IOException {
        // Create config
        ReportConfig config = ReportConfig.createDefault(ReportType.SUMMARY);
        config.setFormat(ReportFormat.HTML);
        
        // Set output file
        Path outputFile = outputDir.resolve("report.html");
        config.setOutputPath(outputFile.toString());
        
        // Generate report
        boolean success = reportGenerator.generateReport(config, testItems);
        
        // Verify
        assertTrue(success, "Report generation should succeed");
        assertTrue(Files.exists(outputFile), "Output file should exist");
        
        String content = Files.readString(outputFile);
        assertFalse(content.isEmpty(), "Output should not be empty");
        assertTrue(content.contains("<!DOCTYPE html>"), "Output should be HTML");
        assertTrue(content.contains("<title>Work Item Summary Report</title>"), "Output should contain report title");
        assertTrue(content.contains("Total Items: 6"), "Output should contain total items count");
    }
    
    @Test
    void testGenerateJsonReport() throws IOException {
        // Create config
        ReportConfig config = ReportConfig.createDefault(ReportType.SUMMARY);
        config.setFormat(ReportFormat.JSON);
        
        // Set output file
        Path outputFile = outputDir.resolve("report.json");
        config.setOutputPath(outputFile.toString());
        
        // Generate report
        boolean success = reportGenerator.generateReport(config, testItems);
        
        // Verify
        assertTrue(success, "Report generation should succeed");
        assertTrue(Files.exists(outputFile), "Output file should exist");
        
        String content = Files.readString(outputFile);
        assertFalse(content.isEmpty(), "Output should not be empty");
        assertTrue(content.contains("{"), "Output should be JSON");
        assertTrue(content.contains("}"), "Output should be JSON");
        assertTrue(content.contains("\"title\":"), "Output should contain title field");
        assertTrue(content.contains("\"items\":"), "Output should contain items array");
    }
    
    @Test
    void testGenerateReportWithFiltering() {
        // Create config
        ReportConfig config = ReportConfig.createDefault(ReportType.DETAILED);
        config.setFormat(ReportFormat.TEXT);
        
        // Add filter for bug type
        config.addFilter("type", "BUG");
        
        // Generate report
        boolean success = reportGenerator.generateReport(config, testItems);
        
        // Verify
        assertTrue(success, "Report generation should succeed");
        
        String output = outputStream.toString();
        assertFalse(output.isEmpty(), "Output should not be empty");
        
        // Should only contain BUG items (WI-01, WI-03, WI-05)
        assertTrue(output.contains("WI-01"), "Output should contain WI-01");
        assertTrue(output.contains("WI-03"), "Output should contain WI-03");
        assertTrue(output.contains("WI-05"), "Output should contain WI-05");
        
        // Should not contain TASK items (WI-02, WI-04, WI-06)
        assertFalse(output.contains("WI-02"), "Output should not contain WI-02");
        assertFalse(output.contains("WI-04"), "Output should not contain WI-04");
        assertFalse(output.contains("WI-06"), "Output should not contain WI-06");
    }
    
    @Test
    void testGenerateReportWithSorting() {
        // Create config
        ReportConfig config = ReportConfig.createDefault(ReportType.STATUS);
        config.setFormat(ReportFormat.TEXT);
        
        // Set sorting by priority
        config.setSortField("priority");
        config.setAscending(false); // High to low
        
        // Generate report
        boolean success = reportGenerator.generateReport(config, testItems);
        
        // Verify
        assertTrue(success, "Report generation should succeed");
        
        String output = outputStream.toString();
        
        // Verify that HIGH priority items come before MEDIUM priority items
        int highPos = output.indexOf("HIGH");
        int mediumPos = output.indexOf("MEDIUM");
        int lowPos = output.indexOf("LOW");
        
        assertTrue(highPos < mediumPos, "HIGH priority should come before MEDIUM priority");
        assertTrue(mediumPos < lowPos, "MEDIUM priority should come before LOW priority");
    }
    
    @Test
    void testGenerateReportWithGrouping() {
        // Create config
        ReportConfig config = ReportConfig.createDefault(ReportType.STATUS);
        config.setFormat(ReportFormat.TEXT);
        
        // Set grouping by state
        config.setGroupBy("state");
        
        // Generate report
        boolean success = reportGenerator.generateReport(config, testItems);
        
        // Verify
        assertTrue(success, "Report generation should succeed");
        
        String output = outputStream.toString();
        
        // Verify that output contains sections for each state
        assertTrue(output.contains("TODO"), "Output should contain TODO state");
        assertTrue(output.contains("IN_PROGRESS"), "Output should contain IN_PROGRESS state");
        assertTrue(output.contains("DONE"), "Output should contain DONE state");
    }
    
    @Test
    void testGenerateReportWithTemplates() throws IOException {
        // Create a custom template
        String customTemplate = "# {{ title }} #\n\n" +
            "Items: {{ summary.totalItems }}\n\n" +
            "## Types ##\n" +
            "{{ typeRows }}\n\n" +
            "## Priorities ##\n" +
            "{{ priorityRows }}\n";
        
        Path customTemplateFile = templateDir.resolve("text/custom.txt");
        Files.writeString(customTemplateFile, customTemplate);
        
        // Create config
        ReportConfig config = ReportConfig.createDefault(ReportType.SUMMARY);
        config.setFormat(ReportFormat.TEXT);
        config.setTemplateName("custom");
        
        // Generate report
        boolean success = reportGenerator.generateReport(config, testItems);
        
        // Verify
        assertTrue(success, "Report generation should succeed");
        
        String output = outputStream.toString();
        assertFalse(output.isEmpty(), "Output should not be empty");
        assertTrue(output.contains("# Work Item Summary Report #"), "Output should use custom template");
        assertTrue(output.contains("Items: 6"), "Output should contain items count in custom format");
        assertTrue(output.contains("## Types ##"), "Output should contain custom types heading");
        assertTrue(output.contains("## Priorities ##"), "Output should contain custom priorities heading");
    }
    
    @Test
    void testGenerateReportFallbackWhenTemplateNotFound() {
        // Create config
        ReportConfig config = ReportConfig.createDefault(ReportType.SUMMARY);
        config.setFormat(ReportFormat.TEXT);
        config.setTemplateName("nonexistent");
        
        // Generate report
        boolean success = reportGenerator.generateReport(config, testItems);
        
        // Verify
        assertTrue(success, "Report generation should succeed even with missing template");
        
        String output = outputStream.toString();
        assertFalse(output.isEmpty(), "Output should not be empty");
        assertTrue(output.contains("Work Item Summary Report"), "Output should fall back to built-in formatting");
    }
    
    @Test
    void testOutputToInvalidPath() throws IOException {
        // Create a path that doesn't exist and isn't creatable
        File nonExistentDir = new File("/nonexistent/directory");
        Path outputFile = nonExistentDir.toPath().resolve("report.txt");
        
        // Create config
        ReportConfig config = ReportConfig.createDefault(ReportType.SUMMARY);
        config.setFormat(ReportFormat.TEXT);
        config.setOutputPath(outputFile.toString());
        
        // Generate report
        boolean success = reportGenerator.generateReport(config, testItems);
        
        // Verify
        assertFalse(success, "Report generation should fail with invalid path");
    }
}