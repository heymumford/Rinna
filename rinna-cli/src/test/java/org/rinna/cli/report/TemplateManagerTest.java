/**
 * Tests for TemplateManager
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for TemplateManager.
 */
public class TemplateManagerTest {
    
    @TempDir
    Path tempDir;
    
    private Path templateDir;
    private TemplateManager templateManager;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create template directories
        templateDir = tempDir.resolve("templates/reports");
        Files.createDirectories(templateDir);
        
        // Create a test template
        String textTemplate = "Title: {{ title }}\nDescription: {{ description }}\nCount: {{ count }}";
        Files.writeString(templateDir.resolve("summary.txt"), textTemplate);
        
        // Create an HTML template
        String htmlTemplate = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <title>{{ title }}</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <h1>{{ title }}</h1>\n" +
            "    <p>{{ description }}</p>\n" +
            "</body>\n" +
            "</html>";
        
        Path htmlDir = templateDir.resolve("html");
        Files.createDirectories(htmlDir);
        Files.writeString(htmlDir.resolve("summary.html"), htmlTemplate);
        
        // Get template manager instance
        templateManager = TemplateManager.getInstance();
    }
    
    @Test
    void testCreateContext() {
        // Create a report config
        ReportConfig config = new ReportConfig(ReportType.SUMMARY, ReportFormat.TEXT);
        config.setTitle("Test Report");
        config.setIncludeHeader(true);
        config.setIncludeTimestamp(true);
        
        // Create data
        Map<String, Object> data = new HashMap<>();
        data.put("count", 42);
        
        // Create context
        Map<String, Object> context = templateManager.createContext(config, data);
        
        // Verify context
        assertEquals("Test Report", context.get("title"));
        assertEquals(42, context.get("count"));
        assertNotNull(context.get("timestamp"));
    }
    
    @Test
    void testTemplateCache() throws IOException {
        // Override the default template path with system property
        String originalPath = System.getProperty("templates.path");
        System.setProperty("templates.path", tempDir.toString());
        
        try {
            // Clear the cache
            templateManager.clearCache();
            
            // Get template - should load from disk
            ReportTemplate template1 = templateManager.getTemplate("summary", ReportFormat.TEXT);
            assertNotNull(template1);
            
            // Get template again - should come from cache
            ReportTemplate template2 = templateManager.getTemplate("summary", ReportFormat.TEXT);
            assertNotNull(template2);
            
            // Should be the same instance
            assertSame(template1, template2);
            
            // Reload template - should load from disk again
            ReportTemplate template3 = templateManager.reloadTemplate("summary", ReportFormat.TEXT);
            assertNotNull(template3);
            
            // Should not be the same instance
            assertNotSame(template1, template3);
        } finally {
            // Restore original property
            if (originalPath != null) {
                System.setProperty("templates.path", originalPath);
            } else {
                System.clearProperty("templates.path");
            }
        }
    }
    
    @Test
    void testApplyTemplate() throws IOException {
        // Override the default template path with system property
        String originalPath = System.getProperty("templates.path");
        System.setProperty("templates.path", tempDir.toString());
        
        try {
            // Create a report config
            ReportConfig config = new ReportConfig(ReportType.SUMMARY, ReportFormat.TEXT);
            config.setTitle("Test Report");
            config.setDescription("This is a test report");
            
            // Create data
            Map<String, Object> data = new HashMap<>();
            data.put("count", 42);
            
            // Apply template
            String result = templateManager.applyTemplate(config, data);
            
            // Verify result
            assertTrue(result.contains("Title: Test Report"));
            assertTrue(result.contains("Description: This is a test report"));
            assertTrue(result.contains("Count: 42"));
        } finally {
            // Restore original property
            if (originalPath != null) {
                System.setProperty("templates.path", originalPath);
            } else {
                System.clearProperty("templates.path");
            }
        }
    }
    
    @Test
    void testApplyHTMLTemplate() throws IOException {
        // Override the default template path with system property
        String originalPath = System.getProperty("templates.path");
        System.setProperty("templates.path", tempDir.toString());
        
        try {
            // Create a report config
            ReportConfig config = new ReportConfig(ReportType.SUMMARY, ReportFormat.HTML);
            config.setTitle("HTML Report");
            config.setDescription("This is an HTML report");
            
            // Create data
            Map<String, Object> data = new HashMap<>();
            
            // Apply template
            String result = templateManager.applyTemplate(config, data);
            
            // Verify result
            assertTrue(result.contains("<title>HTML Report</title>"));
            assertTrue(result.contains("<h1>HTML Report</h1>"));
            assertTrue(result.contains("<p>This is an HTML report</p>"));
        } finally {
            // Restore original property
            if (originalPath != null) {
                System.setProperty("templates.path", originalPath);
            } else {
                System.clearProperty("templates.path");
            }
        }
    }
}