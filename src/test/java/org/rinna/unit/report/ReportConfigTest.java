/**
 * Unit tests for ReportConfig
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.unit.report;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.rinna.base.UnitTest;
import org.rinna.cli.report.ReportConfig;
import org.rinna.cli.report.ReportFormat;
import org.rinna.cli.report.ReportType;

/**
 * Unit tests for ReportConfig class.
 */
public class ReportConfigTest extends UnitTest {

    @Test
    void testDefaultConstructor() {
        ReportConfig config = new ReportConfig();
        
        assertEquals(ReportType.SUMMARY, config.getType(), "Default type should be SUMMARY");
        assertEquals(ReportFormat.TEXT, config.getFormat(), "Default format should be TEXT");
        assertNull(config.getOutputPath(), "Default output path should be null");
        assertEquals("Rinna Work Item Report", config.getTitle(), "Default title should be set");
        assertTrue(config.isIncludeHeader(), "Header should be included by default");
        assertTrue(config.isIncludeTimestamp(), "Timestamp should be included by default");
        assertTrue(config.isPrettyPrint(), "Pretty print should be enabled by default");
        assertEquals(0, config.getMaxItems(), "Default max items should be 0 (no limit)");
        assertTrue(config.isUseTemplate(), "Templates should be used by default");
    }
    
    @Test
    void testConstructorWithTypeAndFormat() {
        ReportConfig config = new ReportConfig(ReportType.DETAILED, ReportFormat.JSON);
        
        assertEquals(ReportType.DETAILED, config.getType(), "Type should match constructor parameter");
        assertEquals(ReportFormat.JSON, config.getFormat(), "Format should match constructor parameter");
        assertNull(config.getOutputPath(), "Default output path should be null");
        assertEquals("Rinna DETAILED Report", config.getTitle(), "Title should include the type");
    }
    
    @Test
    void testSetters() {
        ReportConfig config = new ReportConfig();
        
        // Test setters with chaining
        config.setType(ReportType.PRIORITY)
              .setFormat(ReportFormat.HTML)
              .setOutputPath("/path/to/output.html")
              .setTitle("Custom Title")
              .setIncludeHeader(false)
              .setIncludeTimestamp(false)
              .setPrettyPrint(false)
              .setMaxItems(100)
              .setStartDate(LocalDate.of(2025, 1, 1))
              .setEndDate(LocalDate.of(2025, 12, 31))
              .setProjectId("PROJ-123")
              .setSortField("priority")
              .setAscending(false)
              .setGroupBy("assignee")
              .setUseTemplate(false)
              .setTemplateName("custom_template");
        
        // Verify all values
        assertEquals(ReportType.PRIORITY, config.getType());
        assertEquals(ReportFormat.HTML, config.getFormat());
        assertEquals("/path/to/output.html", config.getOutputPath());
        assertEquals("Custom Title", config.getTitle());
        assertFalse(config.isIncludeHeader());
        assertFalse(config.isIncludeTimestamp());
        assertFalse(config.isPrettyPrint());
        assertEquals(100, config.getMaxItems());
        assertEquals(LocalDate.of(2025, 1, 1), config.getStartDate());
        assertEquals(LocalDate.of(2025, 12, 31), config.getEndDate());
        assertEquals("PROJ-123", config.getProjectId());
        assertEquals("priority", config.getSortField());
        assertFalse(config.isAscending());
        assertTrue(config.isGroupByEnabled());
        assertEquals("assignee", config.getGroupByField());
        assertFalse(config.isUseTemplate());
        assertEquals("custom_template", config.getTemplateName());
    }
    
    @Test
    void testFieldInclusions() {
        ReportConfig config = new ReportConfig();
        
        // Add included fields
        config.addIncludedField("id")
              .addIncludedField("title")
              .addIncludedField("priority");
        
        // Verify included fields
        List<String> includedFields = config.getIncludedFields();
        assertEquals(3, includedFields.size(), "Should have 3 included fields");
        assertTrue(includedFields.contains("id"), "Should include 'id' field");
        assertTrue(includedFields.contains("title"), "Should include 'title' field");
        assertTrue(includedFields.contains("priority"), "Should include 'priority' field");
        
        // Set included fields
        List<String> newIncludedFields = Arrays.asList("type", "status", "assignee");
        config.setIncludedFields(newIncludedFields);
        
        // Verify new included fields
        includedFields = config.getIncludedFields();
        assertEquals(3, includedFields.size(), "Should have 3 included fields");
        assertTrue(includedFields.contains("type"), "Should include 'type' field");
        assertTrue(includedFields.contains("status"), "Should include 'status' field");
        assertTrue(includedFields.contains("assignee"), "Should include 'assignee' field");
        
        // Test excluded fields
        config.addExcludedField("description")
              .addExcludedField("comments");
        
        // Verify excluded fields
        List<String> excludedFields = config.getExcludedFields();
        assertEquals(2, excludedFields.size(), "Should have 2 excluded fields");
        assertTrue(excludedFields.contains("description"), "Should exclude 'description' field");
        assertTrue(excludedFields.contains("comments"), "Should exclude 'comments' field");
    }
    
    @Test
    void testFilters() {
        ReportConfig config = new ReportConfig();
        
        // Add filters
        config.addFilter("state", "IN_PROGRESS")
              .addFilter("priority", "HIGH")
              .addFilter("assignee", "johndoe");
        
        // Verify filters
        Map<String, String> filters = config.getFilters();
        assertEquals(3, filters.size(), "Should have 3 filters");
        assertEquals("IN_PROGRESS", filters.get("state"), "State filter should match");
        assertEquals("HIGH", filters.get("priority"), "Priority filter should match");
        assertEquals("johndoe", filters.get("assignee"), "Assignee filter should match");
        
        // Set filters
        Map<String, String> newFilters = new HashMap<>();
        newFilters.put("type", "BUG");
        newFilters.put("dueDate", "2025-06-30");
        config.setFilters(newFilters);
        
        // Verify new filters
        filters = config.getFilters();
        assertEquals(2, filters.size(), "Should have 2 filters");
        assertEquals("BUG", filters.get("type"), "Type filter should match");
        assertEquals("2025-06-30", filters.get("dueDate"), "Due date filter should match");
    }
    
    @Test
    void testEmailSettings() {
        ReportConfig config = new ReportConfig();
        
        // Default email settings
        assertFalse(config.isEmailEnabled(), "Email should be disabled by default");
        assertTrue(config.getEmailRecipients().isEmpty(), "Should have no recipients by default");
        assertNull(config.getEmailSubject(), "Email subject should be null by default");
        
        // Configure email settings
        config.setEmailEnabled(true)
              .addEmailRecipient("user1@example.com")
              .addEmailRecipient("user2@example.com")
              .setEmailSubject("Test Report");
        
        // Verify email settings
        assertTrue(config.isEmailEnabled(), "Email should be enabled");
        List<String> recipients = config.getEmailRecipients();
        assertEquals(2, recipients.size(), "Should have 2 recipients");
        assertTrue(recipients.contains("user1@example.com"), "Should include first recipient");
        assertTrue(recipients.contains("user2@example.com"), "Should include second recipient");
        assertEquals("Test Report", config.getEmailSubject(), "Email subject should match");
        
        // Set recipients list
        List<String> newRecipients = Arrays.asList("team@example.com", "manager@example.com");
        config.setEmailRecipients(newRecipients);
        
        // Verify new recipients
        recipients = config.getEmailRecipients();
        assertEquals(2, recipients.size(), "Should have 2 recipients");
        assertTrue(recipients.contains("team@example.com"), "Should include team recipient");
        assertTrue(recipients.contains("manager@example.com"), "Should include manager recipient");
    }
    
    @Test
    void testGroupBy() {
        ReportConfig config = new ReportConfig();
        
        // Default group by settings
        assertFalse(config.isGroupByEnabled(), "Group by should be disabled by default");
        assertNull(config.getGroupByField(), "Group by field should be null by default");
        
        // Enable grouping
        config.setGroupBy("state");
        
        // Verify grouping
        assertTrue(config.isGroupByEnabled(), "Group by should be enabled");
        assertEquals("state", config.getGroupByField(), "Group by field should match");
        
        // Disable grouping with null field
        config.setGroupBy(null);
        
        // Verify grouping disabled
        assertFalse(config.isGroupByEnabled(), "Group by should be disabled");
        assertNull(config.getGroupByField(), "Group by field should be null");
        
        // Disable grouping with empty field
        config.setGroupBy("state");
        config.setGroupBy("");
        
        // Verify grouping disabled
        assertFalse(config.isGroupByEnabled(), "Group by should be disabled");
        assertNull(config.getGroupByField(), "Group by field should be null");
    }
    
    @ParameterizedTest
    @EnumSource(ReportType.class)
    void testCreateDefaultConfigs(ReportType type) {
        ReportConfig config = ReportConfig.createDefault(type);
        
        assertEquals(type, config.getType(), "Config type should match");
        assertNotNull(config.getTitle(), "Title should not be null");
        assertTrue(config.getTitle().contains(type.name()), "Title should include the report type");
        
        // Specific checks based on report type
        switch (type) {
            case SUMMARY:
                assertFalse(config.getIncludedFields().isEmpty(), "Summary should include specific fields");
                assertTrue(config.getIncludedFields().contains("id"), "Summary should include id field");
                assertTrue(config.getIncludedFields().contains("title"), "Summary should include title field");
                break;
                
            case STATUS:
                assertTrue(config.isGroupByEnabled(), "Status report should have grouping enabled");
                assertEquals("state", config.getGroupByField(), "Status report should group by state");
                break;
                
            case ASSIGNEE:
                assertTrue(config.isGroupByEnabled(), "Assignee report should have grouping enabled");
                assertEquals("assignee", config.getGroupByField(), "Assignee report should group by assignee");
                break;
                
            case PRIORITY:
                assertTrue(config.isGroupByEnabled(), "Priority report should have grouping enabled");
                assertEquals("priority", config.getGroupByField(), "Priority report should group by priority");
                break;
                
            case OVERDUE:
                assertFalse(config.getFilters().isEmpty(), "Overdue report should have filters");
                assertTrue(config.getFilters().containsKey("state"), "Overdue report should filter by state");
                break;
                
            case TIMELINE:
                assertEquals("dueDate", config.getSortField(), "Timeline report should sort by dueDate");
                assertTrue(config.isAscending(), "Timeline report should sort in ascending order");
                break;
                
            case ACTIVITY:
                assertEquals("updatedAt", config.getSortField(), "Activity report should sort by updatedAt");
                assertFalse(config.isAscending(), "Activity report should sort in descending order");
                break;
        }
    }
    
    @Test
    void testGetTemplateName() {
        ReportConfig config = new ReportConfig(ReportType.SUMMARY, ReportFormat.HTML);
        
        // Default template name (no explicit template set)
        assertEquals("summary", config.getTemplateName(), "Default template name should be lowercase type name");
        
        // Explicit template name
        config.setTemplateName("custom_template");
        assertEquals("custom_template", config.getTemplateName(), "Template name should match set value");
        
        // Change type, custom template should remain
        config.setType(ReportType.DETAILED);
        assertEquals("custom_template", config.getTemplateName(), "Custom template should be preserved when type changes");
    }
    
    @Test
    void testDefensiveCopying() {
        ReportConfig config = new ReportConfig();
        
        // Test included fields
        List<String> includedFields = Arrays.asList("id", "title", "state");
        config.setIncludedFields(includedFields);
        
        // Modify original list
        includedFields.add("newField");
        
        // Verify config's list is unchanged
        assertEquals(3, config.getIncludedFields().size(), "Included fields should not be affected by changes to original list");
        
        // Test getting and modifying list
        List<String> retrievedFields = config.getIncludedFields();
        retrievedFields.add("anotherField");
        
        // Verify config's list is unchanged
        assertEquals(3, config.getIncludedFields().size(), "Included fields should not be affected by changes to retrieved list");
        
        // Similar tests for other collections
        Map<String, String> filters = new HashMap<>();
        filters.put("state", "DONE");
        config.setFilters(filters);
        
        filters.put("newKey", "newValue");
        assertEquals(1, config.getFilters().size(), "Filters should not be affected by changes to original map");
        
        Map<String, String> retrievedFilters = config.getFilters();
        retrievedFilters.put("anotherKey", "anotherValue");
        assertEquals(1, config.getFilters().size(), "Filters should not be affected by changes to retrieved map");
    }
}