/**
 * Performance tests for the Report System
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.performance.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.rinna.base.PerformanceTest;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.report.ReportConfig;
import org.rinna.cli.report.ReportFormat;
import org.rinna.cli.report.ReportService;
import org.rinna.cli.report.ReportType;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.ServiceManager;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Performance tests for the Report System.
 */
@Tag("performance")
public class ReportPerformanceTest extends PerformanceTest {
    
    @TempDir
    Path tempDir;
    
    private ItemService itemService;
    private ReportService reportService;
    
    private List<WorkItem> testItems;
    private static final int SMALL_DATASET_SIZE = 100;
    private static final int MEDIUM_DATASET_SIZE = 1000;
    private static final int LARGE_DATASET_SIZE = 10000;
    
    private static final long MAX_SMALL_REPORT_TIME_MS = 500;
    private static final long MAX_MEDIUM_REPORT_TIME_MS = 2000;
    private static final long MAX_LARGE_REPORT_TIME_MS = 10000;
    
    @BeforeEach
    void setUp() throws Exception {
        // Initialize services
        ServiceManager serviceManager = ServiceManager.getInstance();
        itemService = serviceManager.getItemService();
        reportService = ReportService.getInstance();
        
        // Set up template directory
        Path templateDir = tempDir.resolve("templates");
        Files.createDirectories(templateDir);
        System.setProperty("report.template.dir", templateDir.toString());
    }
    
    @Test
    void testGenerateSummaryReport_SmallDataset() {
        // Create and add test items
        testItems = createTestWorkItems(SMALL_DATASET_SIZE);
        addItemsToService(testItems);
        
        // Set up output file
        Path outputFile = tempDir.resolve("summary_small.txt");
        
        // Create report config
        ReportConfig config = new ReportConfig(ReportType.SUMMARY, ReportFormat.TEXT);
        config.setOutputPath(outputFile.toString());
        
        // Measure report generation time
        long startTime = System.nanoTime();
        boolean result = reportService.generateReport(config);
        long endTime = System.nanoTime();
        long elapsedTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        // Verify
        assertTrue(result, "Report generation should succeed");
        assertTrue(Files.exists(outputFile.toAbsolutePath()), "Output file should exist");
        assertTrue(elapsedTimeMs <= MAX_SMALL_REPORT_TIME_MS,
                "Small report should be generated within " + MAX_SMALL_REPORT_TIME_MS + "ms (actual: " + elapsedTimeMs + "ms)");
    }
    
    @Test
    void testGenerateSummaryReport_MediumDataset() {
        // Create and add test items
        testItems = createTestWorkItems(MEDIUM_DATASET_SIZE);
        addItemsToService(testItems);
        
        // Set up output file
        Path outputFile = tempDir.resolve("summary_medium.txt");
        
        // Create report config
        ReportConfig config = new ReportConfig(ReportType.SUMMARY, ReportFormat.TEXT);
        config.setOutputPath(outputFile.toString());
        
        // Measure report generation time
        long startTime = System.nanoTime();
        boolean result = reportService.generateReport(config);
        long endTime = System.nanoTime();
        long elapsedTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        // Verify
        assertTrue(result, "Report generation should succeed");
        assertTrue(Files.exists(outputFile.toAbsolutePath()), "Output file should exist");
        assertTrue(elapsedTimeMs <= MAX_MEDIUM_REPORT_TIME_MS,
                "Medium report should be generated within " + MAX_MEDIUM_REPORT_TIME_MS + "ms (actual: " + elapsedTimeMs + "ms)");
    }
    
    @Test
    void testGenerateSummaryReport_LargeDataset() {
        // Create and add test items
        testItems = createTestWorkItems(LARGE_DATASET_SIZE);
        addItemsToService(testItems);
        
        // Set up output file
        Path outputFile = tempDir.resolve("summary_large.txt");
        
        // Create report config
        ReportConfig config = new ReportConfig(ReportType.SUMMARY, ReportFormat.TEXT);
        config.setOutputPath(outputFile.toString());
        
        // Measure report generation time
        long startTime = System.nanoTime();
        boolean result = reportService.generateReport(config);
        long endTime = System.nanoTime();
        long elapsedTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        // Verify
        assertTrue(result, "Report generation should succeed");
        assertTrue(Files.exists(outputFile.toAbsolutePath()), "Output file should exist");
        assertTrue(elapsedTimeMs <= MAX_LARGE_REPORT_TIME_MS,
                "Large report should be generated within " + MAX_LARGE_REPORT_TIME_MS + "ms (actual: " + elapsedTimeMs + "ms)");
    }
    
    @ParameterizedTest
    @EnumSource(ReportFormat.class)
    void testReportFormats_Performance(ReportFormat format) {
        // Create and add test items (medium dataset)
        testItems = createTestWorkItems(MEDIUM_DATASET_SIZE);
        addItemsToService(testItems);
        
        // Set up output file
        Path outputFile = tempDir.resolve("report_format_test." + format.getFileExtension());
        
        // Create report config
        ReportConfig config = new ReportConfig(ReportType.SUMMARY, format);
        config.setOutputPath(outputFile.toString());
        
        // Measure report generation time
        long startTime = System.nanoTime();
        boolean result = reportService.generateReport(config);
        long endTime = System.nanoTime();
        long elapsedTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        // Verify
        assertTrue(result, "Report generation with format " + format + " should succeed");
        assertTrue(Files.exists(outputFile.toAbsolutePath()), "Output file should exist");
        assertTrue(elapsedTimeMs <= MAX_MEDIUM_REPORT_TIME_MS,
                format + " report should be generated within " + MAX_MEDIUM_REPORT_TIME_MS + "ms (actual: " + elapsedTimeMs + "ms)");
    }
    
    @ParameterizedTest
    @EnumSource(ReportType.class)
    void testReportTypes_Performance(ReportType type) {
        // Create and add test items (medium dataset)
        testItems = createTestWorkItems(MEDIUM_DATASET_SIZE);
        addItemsToService(testItems);
        
        // Set up output file
        Path outputFile = tempDir.resolve("report_type_test.txt");
        
        // Create report config
        ReportConfig config = new ReportConfig(type, ReportFormat.TEXT);
        config.setOutputPath(outputFile.toString());
        
        // Measure report generation time
        long startTime = System.nanoTime();
        boolean result = reportService.generateReport(config);
        long endTime = System.nanoTime();
        long elapsedTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        // Verify
        assertTrue(result, "Report generation with type " + type + " should succeed");
        assertTrue(Files.exists(outputFile.toAbsolutePath()), "Output file should exist");
        
        // Allow more time for detailed reports on large datasets
        long maxAllowedTime = type == ReportType.DETAILED ? MAX_MEDIUM_REPORT_TIME_MS * 2 : MAX_MEDIUM_REPORT_TIME_MS;
        assertTrue(elapsedTimeMs <= maxAllowedTime,
                type + " report should be generated within " + maxAllowedTime + "ms (actual: " + elapsedTimeMs + "ms)");
    }
    
    @Test
    void testFilteredReport_Performance() {
        // Create and add test items (large dataset)
        testItems = createTestWorkItems(LARGE_DATASET_SIZE);
        addItemsToService(testItems);
        
        // Set up output file
        Path outputFile = tempDir.resolve("filtered_report.txt");
        
        // Create report config with filters
        ReportConfig config = new ReportConfig(ReportType.DETAILED, ReportFormat.TEXT);
        config.setOutputPath(outputFile.toString());
        config.addFilter("type", WorkItemType.BUG.name());
        config.addFilter("state", WorkflowState.TODO.name());
        
        // Measure report generation time
        long startTime = System.nanoTime();
        boolean result = reportService.generateReport(config);
        long endTime = System.nanoTime();
        long elapsedTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        // Verify
        assertTrue(result, "Filtered report generation should succeed");
        assertTrue(Files.exists(outputFile.toAbsolutePath()), "Output file should exist");
        assertTrue(elapsedTimeMs <= MAX_MEDIUM_REPORT_TIME_MS,
                "Filtered report should be generated within " + MAX_MEDIUM_REPORT_TIME_MS + "ms (actual: " + elapsedTimeMs + "ms)");
    }
    
    @Test
    void testGroupedReport_Performance() {
        // Create and add test items (large dataset)
        testItems = createTestWorkItems(LARGE_DATASET_SIZE);
        addItemsToService(testItems);
        
        // Set up output file
        Path outputFile = tempDir.resolve("grouped_report.txt");
        
        // Create report config with grouping
        ReportConfig config = new ReportConfig(ReportType.STATUS, ReportFormat.TEXT);
        config.setOutputPath(outputFile.toString());
        config.setGroupBy("state");
        
        // Measure report generation time
        long startTime = System.nanoTime();
        boolean result = reportService.generateReport(config);
        long endTime = System.nanoTime();
        long elapsedTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        // Verify
        assertTrue(result, "Grouped report generation should succeed");
        assertTrue(Files.exists(outputFile.toAbsolutePath()), "Output file should exist");
        assertTrue(elapsedTimeMs <= MAX_LARGE_REPORT_TIME_MS,
                "Grouped report should be generated within " + MAX_LARGE_REPORT_TIME_MS + "ms (actual: " + elapsedTimeMs + "ms)");
    }
    
    @Test
    void testSchedulerPerformance() {
        // Create a report scheduler with many scheduled reports
        int numReports = 100;
        
        // Measure the time to check all reports
        long startTime = System.nanoTime();
        
        // Simulate checking all reports (this would typically be done by the scheduler)
        for (int i = 0; i < numReports; i++) {
            ReportConfig config = ReportConfig.createDefault(ReportType.SUMMARY);
            String time = String.format("%02d:%02d", i % 24, (i * 7) % 60);
            boolean shouldRun = checkScheduledReportTime(time, "DAILY", null, 0);
        }
        
        long endTime = System.nanoTime();
        long elapsedTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        // Verify
        long maxAllowedTime = 500; // 500ms for checking 100 reports
        assertTrue(elapsedTimeMs <= maxAllowedTime,
                "Scheduler check for " + numReports + " reports should complete within " + maxAllowedTime + 
                "ms (actual: " + elapsedTimeMs + "ms)");
    }
    
    /**
     * Creates test work items for testing.
     */
    private List<WorkItem> createTestWorkItems(int count) {
        List<WorkItem> items = new ArrayList<>(count);
        Random random = new Random(12345); // Fixed seed for reproducibility
        
        WorkItemType[] types = WorkItemType.values();
        WorkflowState[] states = WorkflowState.values();
        String[] priorities = {"HIGH", "MEDIUM", "LOW"};
        String[] assignees = {"johndoe", "janedoe", "alexsmith", "sarahlee", "michaelb", null};
        
        for (int i = 0; i < count; i++) {
            WorkItem item = new WorkItem();
            item.setId("WI-" + (i + 1));
            item.setTitle("Test Item " + (i + 1) + " " + UUID.randomUUID().toString().substring(0, 8));
            item.setType(types[random.nextInt(types.length)]);
            item.setState(states[random.nextInt(states.length)]);
            item.setPriority(priorities[random.nextInt(priorities.length)]);
            item.setAssignee(assignees[random.nextInt(assignees.length)]);
            
            items.add(item);
        }
        
        return items;
    }
    
    /**
     * Adds items to the item service.
     */
    private void addItemsToService(List<WorkItem> items) {
        for (WorkItem item : items) {
            itemService.addItem(item);
        }
    }
    
    /**
     * Simulates the scheduler's time checking logic to test performance.
     */
    private boolean checkScheduledReportTime(String time, String scheduleType, String dayOfWeek, int dayOfMonth) {
        // Get current time as 24-hour format HH:MM
        String currentTime = "12:00"; // Fixed time for testing
        
        // Split times
        String[] timeParts = time.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);
        
        String[] currentTimeParts = currentTime.split(":");
        int currentHour = Integer.parseInt(currentTimeParts[0]);
        int currentMinute = Integer.parseInt(currentTimeParts[1]);
        
        // Check if the time matches
        if (currentHour != hour || currentMinute != minute) {
            return false;
        }
        
        // Check schedule type
        if ("DAILY".equals(scheduleType)) {
            return true;
        } else if ("WEEKLY".equals(scheduleType)) {
            // Weekend for testing purposes
            return "SUNDAY".equals(dayOfWeek);
        } else if ("MONTHLY".equals(scheduleType)) {
            // 15th of the month for testing purposes
            return dayOfMonth == 15;
        }
        
        return false;
    }
}