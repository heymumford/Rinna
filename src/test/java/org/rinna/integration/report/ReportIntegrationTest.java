/**
 * Integration tests for the Report System
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.integration.report;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rinna.base.IntegrationTest;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.report.*;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.ServiceManager;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the Report System.
 */
public class ReportIntegrationTest extends IntegrationTest {
    
    @TempDir
    Path tempDir;
    
    private ItemService itemService;
    private ReportService reportService;
    private EmailService emailService;
    
    private List<WorkItem> testItems;
    
    @BeforeEach
    void setUp() throws Exception {
        // Initialize services
        ServiceManager serviceManager = ServiceManager.getInstance();
        itemService = serviceManager.getItemService();
        reportService = ReportService.getInstance();
        emailService = EmailService.getInstance();
        
        // Set up template directory
        Path templateDir = tempDir.resolve("templates");
        Files.createDirectories(templateDir);
        System.setProperty("report.template.dir", templateDir.toString());
        
        // Create test items
        testItems = createTestWorkItems();
        
        // Add test items to the service
        for (WorkItem item : testItems) {
            itemService.addItem(item);
        }
    }
    
    @AfterEach
    void tearDown() {
        // Clean up
        System.clearProperty("report.template.dir");
    }
    
    @Test
    void testGenerateSummaryReport() throws Exception {
        // Set up output file
        Path outputFile = tempDir.resolve("summary_report.txt");
        
        // Create report config
        ReportConfig config = new ReportConfig(ReportType.SUMMARY, ReportFormat.TEXT);
        config.setOutputPath(outputFile.toString());
        
        // Generate report
        boolean result = reportService.generateReport(config);
        
        // Verify
        assertTrue(result, "Report generation should succeed");
        assertTrue(Files.exists(outputFile), "Output file should exist");
        
        // Check report content
        String reportContent = Files.readString(outputFile);
        assertNotNull(reportContent, "Report content should not be null");
        assertFalse(reportContent.isEmpty(), "Report content should not be empty");
        
        // Verify report content includes key information
        assertTrue(reportContent.contains("Work Item Summary Report"), "Report should include title");
        assertTrue(reportContent.contains("Total Items: " + testItems.size()), "Report should include total count");
        
        // Verify report includes state breakdown
        long doneCount = testItems.stream().filter(i -> i.getState() == WorkflowState.DONE).count();
        long inProgressCount = testItems.stream().filter(i -> i.getState() == WorkflowState.IN_PROGRESS).count();
        long todoCount = testItems.stream().filter(i -> i.getState() == WorkflowState.TODO).count();
        
        assertTrue(reportContent.contains("Completed: " + doneCount), "Report should include completed count");
        assertTrue(reportContent.contains("In Progress: " + inProgressCount), "Report should include in progress count");
        assertTrue(reportContent.contains("Not Started: " + todoCount), "Report should include to-do count");
    }
    
    @Test
    void testGenerateDetailedReportWithFiltering() throws Exception {
        // Set up output file
        Path outputFile = tempDir.resolve("detailed_bug_report.html");
        
        // Create report config
        ReportConfig config = new ReportConfig(ReportType.DETAILED, ReportFormat.HTML);
        config.setOutputPath(outputFile.toString());
        config.addFilter("type", WorkItemType.BUG.name());
        
        // Generate report
        boolean result = reportService.generateReport(config);
        
        // Verify
        assertTrue(result, "Report generation should succeed");
        assertTrue(Files.exists(outputFile), "Output file should exist");
        
        // Check report content
        String reportContent = Files.readString(outputFile);
        assertNotNull(reportContent, "Report content should not be null");
        assertFalse(reportContent.isEmpty(), "Report content should not be empty");
        
        // Verify report content includes only bugs
        assertTrue(reportContent.contains("WI-03"), "Report should include bug WI-03");
        assertTrue(reportContent.contains("Fix login bug"), "Report should include bug title");
        
        // Verify report doesn't include non-bugs
        assertFalse(reportContent.contains("WI-01"), "Report should not include task WI-01");
        assertFalse(reportContent.contains("WI-02"), "Report should not include task WI-02");
    }
    
    @Test
    void testGenerateStatusReportWithGrouping() throws Exception {
        // Set up output file
        Path outputFile = tempDir.resolve("status_report.md");
        
        // Create report config
        ReportConfig config = new ReportConfig(ReportType.STATUS, ReportFormat.MARKDOWN);
        config.setOutputPath(outputFile.toString());
        config.setGroupBy("state");
        
        // Generate report
        boolean result = reportService.generateReport(config);
        
        // Verify
        assertTrue(result, "Report generation should succeed");
        assertTrue(Files.exists(outputFile), "Output file should exist");
        
        // Check report content
        String reportContent = Files.readString(outputFile);
        assertNotNull(reportContent, "Report content should not be null");
        assertFalse(reportContent.isEmpty(), "Report content should not be empty");
        
        // Verify report has sections for each state
        assertTrue(reportContent.contains("# DONE"), "Report should have DONE section");
        assertTrue(reportContent.contains("# IN_PROGRESS"), "Report should have IN_PROGRESS section");
        assertTrue(reportContent.contains("# TODO"), "Report should have TODO section");
        
        // Verify items are in correct sections
        assertTrue(reportContent.contains("# DONE") && reportContent.contains("WI-01"), 
                "DONE section should include WI-01");
        assertTrue(reportContent.contains("# IN_PROGRESS") && reportContent.contains("WI-02"), 
                "IN_PROGRESS section should include WI-02");
        assertTrue(reportContent.contains("# TODO") && reportContent.contains("WI-03"), 
                "TODO section should include WI-03");
    }
    
    @Test
    void testEmailReport() throws Exception {
        // Set up mock email receiver
        TestEmailReceiver emailReceiver = new TestEmailReceiver();
        emailService.setTestReceiver(emailReceiver);
        
        // Create report config
        ReportConfig config = new ReportConfig(ReportType.SUMMARY, ReportFormat.HTML);
        config.setEmailEnabled(true);
        config.addEmailRecipient("test@example.com");
        config.setEmailSubject("Test Report");
        
        // Generate report
        boolean result = reportService.generateReport(config);
        
        // Verify
        assertTrue(result, "Report generation should succeed");
        assertTrue(emailReceiver.wasEmailSent(), "Email should be sent");
        assertEquals("test@example.com", emailReceiver.getLastRecipient(), "Email recipient should match");
        assertEquals("Test Report", emailReceiver.getLastSubject(), "Email subject should match");
        assertNotNull(emailReceiver.getLastContent(), "Email content should not be null");
        assertTrue(emailReceiver.getLastContent().contains("Work Item Summary Report"), 
                "Email content should include report title");
    }
    
    @Test
    void testWithCustomTemplate() throws Exception {
        // Create a custom template
        Path templateFile = tempDir.resolve("templates/custom_summary.html");
        Files.writeString(templateFile, "<html><body><h1>{{ title }}</h1><p>Items: {{ totalCount }}</p></body></html>");
        
        // Set up output file
        Path outputFile = tempDir.resolve("custom_report.html");
        
        // Create report config
        ReportConfig config = new ReportConfig(ReportType.SUMMARY, ReportFormat.HTML);
        config.setOutputPath(outputFile.toString());
        config.setTemplateName("custom_summary");
        
        // Generate report
        boolean result = reportService.generateReport(config);
        
        // Verify
        assertTrue(result, "Report generation should succeed");
        assertTrue(Files.exists(outputFile), "Output file should exist");
        
        // Check report content
        String reportContent = Files.readString(outputFile);
        assertNotNull(reportContent, "Report content should not be null");
        assertFalse(reportContent.isEmpty(), "Report content should not be empty");
        
        // Verify template was used
        assertTrue(reportContent.contains("<html><body>"), "Report should use custom template");
        assertTrue(reportContent.contains("<h1>Work Item Summary Report</h1>"), 
                "Report should include the title variable");
        assertTrue(reportContent.contains("<p>Items: " + testItems.size() + "</p>"), 
                "Report should include the totalCount variable");
    }
    
    @Test
    void testScheduledReport() throws Exception {
        // Set up the report scheduler
        ReportScheduler scheduler = ReportScheduler.getInstance();
        scheduler.clearScheduledReports();
        
        // Create temp directory for scheduler
        Path schedulerDir = tempDir.resolve("schedules");
        Files.createDirectories(schedulerDir);
        System.setProperty("report.scheduler.config.dir", schedulerDir.toString());
        
        try {
            // Create a scheduled report
            ReportScheduler.ScheduledReport report = new ReportScheduler.ScheduledReport();
            report.setId("test-report");
            report.setName("Test Scheduled Report");
            report.setScheduleType(ReportScheduler.ScheduleType.DAILY);
            report.setTime("12:00");
            
            ReportConfig config = ReportConfig.createDefault(ReportType.SUMMARY);
            config.setFormat(ReportFormat.HTML);
            config.setOutputPath(tempDir.resolve("scheduled_report.html").toString());
            report.setConfig(config);
            
            // Add the report to the scheduler
            boolean added = scheduler.addScheduledReport(report);
            assertTrue(added, "Should successfully add scheduled report");
            
            // Verify the report was saved to disk
            File reportFile = schedulerDir.resolve(report.getId() + ".properties").toFile();
            assertTrue(reportFile.exists(), "Report should be persisted to disk");
            
            // Verify we can retrieve the report
            ReportScheduler.ScheduledReport retrieved = scheduler.getScheduledReport(report.getId());
            assertNotNull(retrieved, "Should retrieve the scheduled report");
            assertEquals("Test Scheduled Report", retrieved.getName(), "Retrieved report name should match");
            assertEquals(ReportScheduler.ScheduleType.DAILY, retrieved.getScheduleType(), 
                    "Retrieved report type should match");
            assertEquals("12:00", retrieved.getTime(), "Retrieved report time should match");
            
            // Test report execution at specific time
            TestReportExecutor executor = new TestReportExecutor();
            scheduler.setTestExecutor(executor);
            
            // Run the scheduler's check logic - forcing execution
            scheduler.startWithTestTime("12:00");
            
            // Verify the report was executed
            assertTrue(executor.wasReportExecuted(), "Report should be executed");
            assertEquals(report.getId(), executor.getLastExecutedReport().getId(), 
                    "Executed report should match");
            
            // Test report is not executed at different time
            executor.reset();
            scheduler.startWithTestTime("13:00");
            assertFalse(executor.wasReportExecuted(), "Report should not be executed at different time");
            
            // Test removing the report
            boolean removed = scheduler.removeScheduledReport(report.getId());
            assertTrue(removed, "Should successfully remove scheduled report");
            assertFalse(reportFile.exists(), "Report file should be deleted");
            assertNull(scheduler.getScheduledReport(report.getId()), 
                    "Should not retrieve removed report");
        } finally {
            // Stop the scheduler
            scheduler.stop();
            System.clearProperty("report.scheduler.config.dir");
        }
    }
    
    /**
     * Creates test work items for testing.
     */
    private List<WorkItem> createTestWorkItems() {
        WorkItem item1 = new WorkItem();
        item1.setId("WI-01");
        item1.setTitle("Setup CI/CD");
        item1.setType(WorkItemType.TASK);
        item1.setState(WorkflowState.DONE);
        item1.setPriority("HIGH");
        item1.setAssignee("johndoe");
        
        WorkItem item2 = new WorkItem();
        item2.setId("WI-02");
        item2.setTitle("Design database");
        item2.setType(WorkItemType.TASK);
        item2.setState(WorkflowState.IN_PROGRESS);
        item2.setPriority("MEDIUM");
        item2.setAssignee("janedoe");
        
        WorkItem item3 = new WorkItem();
        item3.setId("WI-03");
        item3.setTitle("Fix login bug");
        item3.setType(WorkItemType.BUG);
        item3.setState(WorkflowState.TODO);
        item3.setPriority("HIGH");
        item3.setAssignee("alexsmith");
        
        WorkItem item4 = new WorkItem();
        item4.setId("WI-04");
        item4.setTitle("Update docs");
        item4.setType(WorkItemType.TASK);
        item4.setState(WorkflowState.IN_PROGRESS);
        item4.setPriority("LOW");
        item4.setAssignee("johndoe");
        
        WorkItem item5 = new WorkItem();
        item5.setId("WI-05");
        item5.setTitle("Performance issue");
        item5.setType(WorkItemType.BUG);
        item5.setState(WorkflowState.TODO);
        item5.setPriority("MEDIUM");
        
        WorkItem item6 = new WorkItem();
        item6.setId("WI-06");
        item6.setTitle("Security review");
        item6.setType(WorkItemType.TASK);
        item6.setState(WorkflowState.DONE);
        item6.setPriority("HIGH");
        item6.setAssignee("janedoe");
        
        return Arrays.asList(item1, item2, item3, item4, item5, item6);
    }
    
    /**
     * Test helper class for email testing.
     */
    private static class TestEmailReceiver {
        private boolean emailSent = false;
        private String lastRecipient;
        private String lastSubject;
        private String lastContent;
        
        public void receiveEmail(String recipient, String subject, String content) {
            this.emailSent = true;
            this.lastRecipient = recipient;
            this.lastSubject = subject;
            this.lastContent = content;
        }
        
        public boolean wasEmailSent() {
            return emailSent;
        }
        
        public String getLastRecipient() {
            return lastRecipient;
        }
        
        public String getLastSubject() {
            return lastSubject;
        }
        
        public String getLastContent() {
            return lastContent;
        }
    }
    
    /**
     * Test helper class for scheduled report execution testing.
     */
    private static class TestReportExecutor {
        private boolean reportExecuted = false;
        private ReportScheduler.ScheduledReport lastExecutedReport;
        
        public void executeReport(ReportScheduler.ScheduledReport report) {
            this.reportExecuted = true;
            this.lastExecutedReport = report;
        }
        
        public boolean wasReportExecuted() {
            return reportExecuted;
        }
        
        public ReportScheduler.ScheduledReport getLastExecutedReport() {
            return lastExecutedReport;
        }
        
        public void reset() {
            this.reportExecuted = false;
            this.lastExecutedReport = null;
        }
    }
}