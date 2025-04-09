/**
 * Component tests for ReportCommand
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
import org.rinna.cli.command.ReportCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Component tests for ReportCommand.
 */
public class ReportCommandComponentTest extends ComponentTest {
    
    @TempDir
    Path tempDir;
    
    private Path outputDir;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private ItemService itemService;
    
    @BeforeEach
    void setUp() throws IOException {
        // Get item service
        itemService = ServiceManager.getInstance().getItemService();
        
        // Clear existing items
        itemService.clearItems();
        
        // Create test items
        createTestWorkItems();
        
        // Create output directory
        outputDir = tempDir.resolve("output");
        Files.createDirectories(outputDir);
        
        // Redirect stdout and stderr
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(outputStream));
    }
    
    @AfterEach
    void tearDown() {
        // Restore stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Clear test items
        itemService.clearItems();
    }
    
    private void createTestWorkItems() {
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
            
            // Set description
            item.setDescription("Description for item " + i);
            
            // Add item
            itemService.addItem(item);
        }
    }
    
    @Test
    void testBasicSummaryReport() {
        // Create command
        ReportCommand cmd = new ReportCommand();
        cmd.setType("summary");
        
        // Execute command
        int exitCode = cmd.call();
        
        // Verify
        assertEquals(0, exitCode, "Command should succeed");
        
        String output = outputStream.toString();
        assertFalse(output.isEmpty(), "Output should not be empty");
        assertTrue(output.contains("Work Item Summary Report"), "Output should contain report title");
        assertTrue(output.contains("Total Items: 6"), "Output should contain total items count");
    }
    
    @Test
    void testReportWithFormat() {
        // Create command
        ReportCommand cmd = new ReportCommand();
        cmd.setType("summary");
        cmd.setFormat("html");
        
        // Set output file
        Path outputFile = outputDir.resolve("report.html");
        cmd.setOutput(outputFile.toString());
        
        // Execute command
        int exitCode = cmd.call();
        
        // Verify
        assertEquals(0, exitCode, "Command should succeed");
        
        try {
            assertTrue(Files.exists(outputFile), "Output file should exist");
            
            String content = Files.readString(outputFile);
            assertFalse(content.isEmpty(), "File should not be empty");
            assertTrue(content.contains("<!DOCTYPE html>"), "File should contain HTML");
        } catch (IOException e) {
            fail("Failed to read output file: " + e.getMessage());
        }
    }
    
    @Test
    void testReportWithFilter() {
        // Create command
        ReportCommand cmd = new ReportCommand();
        cmd.setType("detailed");
        cmd.addFilter("type", "BUG");
        
        // Execute command
        int exitCode = cmd.call();
        
        // Verify
        assertEquals(0, exitCode, "Command should succeed");
        
        String output = outputStream.toString();
        // Output should contain only BUG items
        assertTrue(output.contains("WI-01"), "Output should contain WI-01 (BUG)");
        assertTrue(output.contains("WI-03"), "Output should contain WI-03 (BUG)");
        assertTrue(output.contains("WI-05"), "Output should contain WI-05 (BUG)");
        assertFalse(output.contains("WI-02"), "Output should not contain WI-02 (TASK)");
        assertFalse(output.contains("WI-04"), "Output should not contain WI-04 (TASK)");
        assertFalse(output.contains("WI-06"), "Output should not contain WI-06 (TASK)");
    }
    
    @Test
    void testReportWithGrouping() {
        // Create command
        ReportCommand cmd = new ReportCommand();
        cmd.setType("status");
        cmd.setGroupBy("state");
        
        // Execute command
        int exitCode = cmd.call();
        
        // Verify
        assertEquals(0, exitCode, "Command should succeed");
        
        String output = outputStream.toString();
        assertTrue(output.contains("DONE"), "Output should contain DONE state section");
        assertTrue(output.contains("IN_PROGRESS"), "Output should contain IN_PROGRESS state section");
        assertTrue(output.contains("TODO"), "Output should contain TODO state section");
    }
    
    @Test
    void testReportWithSorting() {
        // Create command
        ReportCommand cmd = new ReportCommand();
        cmd.setType("status");
        cmd.setSortField("priority");
        cmd.setAscending(false); // High to low
        
        // Execute command
        int exitCode = cmd.call();
        
        // Verify
        assertEquals(0, exitCode, "Command should succeed");
        
        String output = outputStream.toString();
        // Find positions of priority strings
        int highPos = output.indexOf("HIGH");
        int mediumPos = output.indexOf("MEDIUM");
        int lowPos = output.indexOf("LOW");
        
        assertTrue(highPos < mediumPos, "HIGH priority should come before MEDIUM priority");
        assertTrue(mediumPos < lowPos, "MEDIUM priority should come before LOW priority");
    }
    
    @Test
    void testReportWithCustomTitle() {
        // Create command
        ReportCommand cmd = new ReportCommand();
        cmd.setType("summary");
        cmd.setTitle("Custom Report Title");
        
        // Execute command
        int exitCode = cmd.call();
        
        // Verify
        assertEquals(0, exitCode, "Command should succeed");
        
        String output = outputStream.toString();
        assertTrue(output.contains("Custom Report Title"), "Output should contain custom title");
    }
    
    @Test
    void testReportWithLimit() {
        // Create command
        ReportCommand cmd = new ReportCommand();
        cmd.setType("detailed");
        cmd.setLimit(3); // Only show 3 items
        
        // Execute command
        int exitCode = cmd.call();
        
        // Verify
        assertEquals(0, exitCode, "Command should succeed");
        
        String output = outputStream.toString();
        
        // Count the number of work items in the output
        int count = 0;
        int pos = 0;
        while ((pos = output.indexOf("WI-", pos + 1)) != -1) {
            count++;
        }
        
        assertEquals(3, count, "Output should contain only 3 work items");
    }
    
    @Test
    void testReportWithInvalidType() {
        // Create command
        ReportCommand cmd = new ReportCommand();
        cmd.setType("nonexistent");
        
        // Execute command
        int exitCode = cmd.call();
        
        // Verify command should succeed with default type
        assertEquals(0, exitCode, "Command should succeed with default type");
        
        String output = outputStream.toString();
        assertTrue(output.contains("SUMMARY"), "Output should default to SUMMARY report");
    }
    
    @Test
    void testReportWithInvalidFormat() {
        // Create command
        ReportCommand cmd = new ReportCommand();
        cmd.setType("summary");
        cmd.setFormat("nonexistent");
        
        // Execute command
        int exitCode = cmd.call();
        
        // Verify command should succeed with default format
        assertEquals(0, exitCode, "Command should succeed with default format");
        
        String output = outputStream.toString();
        // Should be a text report by default
        assertFalse(output.contains("<!DOCTYPE html>"), "Output should not be HTML");
        assertFalse(output.contains("{"), "Output should not be JSON");
    }
    
    @Test
    void testReportWithInvalidOutputPath() {
        // Create command
        ReportCommand cmd = new ReportCommand();
        cmd.setType("summary");
        cmd.setOutput("/nonexistent/directory/report.txt");
        
        // Execute command
        int exitCode = cmd.call();
        
        // Verify command should fail
        assertEquals(1, exitCode, "Command should fail with invalid output path");
        
        String output = outputStream.toString();
        assertTrue(output.contains("Error") || output.contains("error"), 
            "Output should contain error message");
    }
    
    @Test
    void testReportWithNoItems() {
        // Clear all items
        itemService.clearItems();
        
        // Create command
        ReportCommand cmd = new ReportCommand();
        cmd.setType("summary");
        
        // Execute command
        int exitCode = cmd.call();
        
        // Verify
        assertEquals(0, exitCode, "Command should succeed with no items");
        
        String output = outputStream.toString();
        assertTrue(output.contains("Total Items: 0"), "Output should show zero items");
    }
    
    @Test
    void testReportWithEmailFlag() {
        // Create command
        ReportCommand cmd = new ReportCommand();
        cmd.setType("summary");
        cmd.setEmailEnabled(true);
        cmd.setEmailRecipients("test@example.com");
        
        // Execute command
        int exitCode = cmd.call();
        
        // Verify
        assertEquals(0, exitCode, "Command should succeed");
        
        String output = outputStream.toString();
        assertTrue(output.contains("mail") || output.contains("email"), 
            "Output should mention email");
    }
}