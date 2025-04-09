/**
 * Component tests for ReportScheduler
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
import org.rinna.cli.report.ReportConfig;
import org.rinna.cli.report.ReportFormat;
import org.rinna.cli.report.ReportScheduler;
import org.rinna.cli.report.ReportScheduler.ScheduleType;
import org.rinna.cli.report.ReportScheduler.ScheduledReport;
import org.rinna.cli.report.ReportType;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Component tests for ReportScheduler.
 */
public class ReportSchedulerComponentTest extends ComponentTest {
    
    @TempDir
    Path tempDir;
    
    private ReportScheduler scheduler;
    private Path configDir;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create config directory
        configDir = tempDir.resolve("config");
        Files.createDirectories(configDir);
        
        // Set report scheduler config path
        System.setProperty("report.scheduler.config.dir", configDir.toString());
        
        // Get scheduler instance
        scheduler = ReportScheduler.getInstance();
        
        // Clear any existing schedules
        scheduler.clearScheduledReports();
    }
    
    @AfterEach
    void tearDown() {
        // Stop the scheduler if running
        scheduler.stop();
        
        // Clear schedules
        scheduler.clearScheduledReports();
        
        // Reset system property
        System.clearProperty("report.scheduler.config.dir");
    }
    
    @Test
    void testAddScheduledReport() {
        // Create report
        ScheduledReport report = createSampleReport("Test Report", ScheduleType.DAILY);
        
        // Add report
        boolean result = scheduler.addScheduledReport(report);
        
        // Verify
        assertTrue(result, "Adding report should succeed");
        
        // Check that report was added
        List<ScheduledReport> reports = scheduler.getScheduledReports();
        assertEquals(1, reports.size(), "Should have one scheduled report");
        
        // Check report properties
        ScheduledReport savedReport = reports.get(0);
        assertEquals("Test Report", savedReport.getName(), "Report name should match");
        assertEquals(ScheduleType.DAILY, savedReport.getScheduleType(), "Report type should match");
        assertEquals("08:00", savedReport.getTime(), "Report time should match");
    }
    
    @Test
    void testGetScheduledReport() {
        // Create and add report
        ScheduledReport report = createSampleReport("Test Report", ScheduleType.DAILY);
        scheduler.addScheduledReport(report);
        
        // Get report by ID
        ScheduledReport retrieved = scheduler.getScheduledReport(report.getId());
        
        // Verify
        assertNotNull(retrieved, "Retrieved report should not be null");
        assertEquals(report.getId(), retrieved.getId(), "Report ID should match");
        assertEquals("Test Report", retrieved.getName(), "Report name should match");
    }
    
    @Test
    void testRemoveScheduledReport() {
        // Create and add report
        ScheduledReport report = createSampleReport("Test Report", ScheduleType.DAILY);
        scheduler.addScheduledReport(report);
        
        // Remove report
        boolean result = scheduler.removeScheduledReport(report.getId());
        
        // Verify
        assertTrue(result, "Removing report should succeed");
        
        // Check that report was removed
        List<ScheduledReport> reports = scheduler.getScheduledReports();
        assertTrue(reports.isEmpty(), "Should have no scheduled reports");
        
        // Check that getting the report returns null
        ScheduledReport retrieved = scheduler.getScheduledReport(report.getId());
        assertNull(retrieved, "Retrieved report should be null after removal");
    }
    
    @Test
    void testStartAndStopScheduler() throws InterruptedException {
        // Start scheduler
        scheduler.start();
        
        // Verify scheduler is running
        assertTrue(scheduler.isRunning(), "Scheduler should be running");
        
        // Stop scheduler
        scheduler.stop();
        
        // Verify scheduler is stopped
        assertFalse(scheduler.isRunning(), "Scheduler should be stopped");
    }
    
    @Test
    void testPersistAndLoadReports() throws IOException {
        // Create and add reports
        ScheduledReport report1 = createSampleReport("Daily Report", ScheduleType.DAILY);
        ScheduledReport report2 = createSampleReport("Weekly Report", ScheduleType.WEEKLY);
        report2.setDayOfWeek(DayOfWeek.MONDAY);
        
        scheduler.addScheduledReport(report1);
        scheduler.addScheduledReport(report2);
        
        // Create a new scheduler instance (which should load from disk)
        System.setProperty("report.scheduler.config.dir", configDir.toString());
        ReportScheduler newScheduler = new ReportScheduler(false);
        
        // Check that reports were loaded
        List<ScheduledReport> reports = newScheduler.getScheduledReports();
        assertEquals(2, reports.size(), "Should have two scheduled reports");
        
        // Check report types
        boolean hasDaily = false;
        boolean hasWeekly = false;
        
        for (ScheduledReport report : reports) {
            if (report.getName().equals("Daily Report")) {
                hasDaily = true;
                assertEquals(ScheduleType.DAILY, report.getScheduleType(), "Report type should be DAILY");
            } else if (report.getName().equals("Weekly Report")) {
                hasWeekly = true;
                assertEquals(ScheduleType.WEEKLY, report.getScheduleType(), "Report type should be WEEKLY");
                assertEquals(DayOfWeek.MONDAY, report.getDayOfWeek(), "Day of week should be MONDAY");
            }
        }
        
        assertTrue(hasDaily, "Should have a daily report");
        assertTrue(hasWeekly, "Should have a weekly report");
    }
    
    @Test
    void testShouldRunNow() {
        // Create reports with different schedule types
        ScheduledReport dailyReport = createSampleReport("Daily Report", ScheduleType.DAILY);
        
        ScheduledReport weeklyReport = createSampleReport("Weekly Report", ScheduleType.WEEKLY);
        weeklyReport.setDayOfWeek(LocalDateTime.now().getDayOfWeek());
        
        ScheduledReport monthlyReport = createSampleReport("Monthly Report", ScheduleType.MONTHLY);
        monthlyReport.setDayOfMonth(LocalDateTime.now().getDayOfMonth());
        
        // Set time to now
        String currentTime = String.format("%02d:%02d", 
            LocalDateTime.now().getHour(), 
            LocalDateTime.now().getMinute());
        
        dailyReport.setTime(currentTime);
        weeklyReport.setTime(currentTime);
        monthlyReport.setTime(currentTime);
        
        // Check if reports should run now
        assertTrue(scheduler.shouldRunNow(dailyReport), "Daily report should run now");
        assertTrue(scheduler.shouldRunNow(weeklyReport), "Weekly report should run now");
        assertTrue(scheduler.shouldRunNow(monthlyReport), "Monthly report should run now");
        
        // Set time to future
        String futureTime = String.format("%02d:%02d", 
            (LocalDateTime.now().getHour() + 1) % 24, 
            LocalDateTime.now().getMinute());
        
        dailyReport.setTime(futureTime);
        weeklyReport.setTime(futureTime);
        monthlyReport.setTime(futureTime);
        
        // Check that reports should not run now
        assertFalse(scheduler.shouldRunNow(dailyReport), "Daily report should not run now");
        assertFalse(scheduler.shouldRunNow(weeklyReport), "Weekly report should not run now");
        assertFalse(scheduler.shouldRunNow(monthlyReport), "Monthly report should not run now");
    }
    
    @Test
    void testClearScheduledReports() {
        // Create and add reports
        ScheduledReport report1 = createSampleReport("Report 1", ScheduleType.DAILY);
        ScheduledReport report2 = createSampleReport("Report 2", ScheduleType.WEEKLY);
        
        scheduler.addScheduledReport(report1);
        scheduler.addScheduledReport(report2);
        
        // Verify reports were added
        assertEquals(2, scheduler.getScheduledReports().size(), "Should have two scheduled reports");
        
        // Clear reports
        scheduler.clearScheduledReports();
        
        // Verify reports were cleared
        assertTrue(scheduler.getScheduledReports().isEmpty(), "Should have no scheduled reports after clearing");
    }
    
    /**
     * Creates a sample scheduled report for testing.
     *
     * @param name the report name
     * @param type the schedule type
     * @return the scheduled report
     */
    private ScheduledReport createSampleReport(String name, ScheduleType type) {
        ScheduledReport report = new ScheduledReport();
        report.setName(name);
        report.setScheduleType(type);
        report.setTime("08:00");
        
        ReportConfig config = ReportConfig.createDefault(ReportType.SUMMARY);
        config.setFormat(ReportFormat.HTML);
        report.setConfig(config);
        
        return report;
    }
}