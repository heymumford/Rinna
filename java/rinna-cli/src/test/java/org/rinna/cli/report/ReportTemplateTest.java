/**
 * Tests for ReportTemplate
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.report;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests for ReportTemplate.
 */
public class ReportTemplateTest {
    
    @TempDir
    Path tempDir;
    
    private Path templateDir;
    private Path htmlTemplateDir;
    
    @BeforeEach
    void setUp() throws IOException {
        // Create template directories
        templateDir = tempDir.resolve("templates/reports");
        Files.createDirectories(templateDir);
        
        htmlTemplateDir = templateDir.resolve("html");
        Files.createDirectories(htmlTemplateDir);
        
        // Create a test template
        String testTemplate = "<!DOCTYPE html>\n" +
            "<html>\n" +
            "<head>\n" +
            "    <title>{{ title }}</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "    <h1>{{ title }}</h1>\n" +
            "    <p>{{ description }}</p>\n" +
            "    <p>Items: {{ count }}</p>\n" +
            "</body>\n" +
            "</html>";
        
        Files.writeString(htmlTemplateDir.resolve("test.html"), testTemplate);
    }
    
    @Test
    void testTemplateApplication() throws IOException {
        // Override the default template path with system property
        String originalPath = System.getProperty("templates.path");
        System.setProperty("templates.path", tempDir.toString());
        
        try {
            // Create template and data
            ReportTemplate template = new ReportTemplate("test", ReportFormat.HTML);
            
            Map<String, Object> data = new HashMap<>();
            data.put("title", "Test Report");
            data.put("description", "This is a test report");
            data.put("count", 42);
            
            // Apply template
            String result = template.apply(data);
            
            // Verify result
            assertTrue(result.contains("<title>Test Report</title>"));
            assertTrue(result.contains("<h1>Test Report</h1>"));
            assertTrue(result.contains("<p>This is a test report</p>"));
            assertTrue(result.contains("<p>Items: 42</p>"));
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
    void testTemplateWithNestedVariables() throws IOException {
        // Create a test template with nested variables
        String testTemplate = "User: {{ user.name }}, Age: {{ user.age }}, Address: {{ user.address.city }}";
        
        Files.writeString(templateDir.resolve("nested.txt"), testTemplate);
        
        // Override the default template path with system property
        String originalPath = System.getProperty("templates.path");
        System.setProperty("templates.path", tempDir.toString());
        
        try {
            // Create template and data
            ReportTemplate template = new ReportTemplate("nested", ReportFormat.TEXT);
            
            Map<String, Object> address = new HashMap<>();
            address.put("city", "New York");
            address.put("street", "123 Main St");
            
            Map<String, Object> user = new HashMap<>();
            user.put("name", "John Doe");
            user.put("age", 30);
            user.put("address", address);
            
            Map<String, Object> data = new HashMap<>();
            data.put("user", user);
            
            // Apply template
            String result = template.apply(data);
            
            // Verify result
            assertEquals("User: John Doe, Age: 30, Address: New York", result.trim());
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
    void testTemplateWithMissingVariables() throws IOException {
        // Create a test template with missing variables
        String testTemplate = "Title: {{ title }}, Description: {{ description }}, Missing: {{ missing }}";
        
        Files.writeString(templateDir.resolve("missing.txt"), testTemplate);
        
        // Override the default template path with system property
        String originalPath = System.getProperty("templates.path");
        System.setProperty("templates.path", tempDir.toString());
        
        try {
            // Create template and data
            ReportTemplate template = new ReportTemplate("missing", ReportFormat.TEXT);
            
            Map<String, Object> data = new HashMap<>();
            data.put("title", "Test Report");
            data.put("description", "This is a test report");
            
            // Apply template
            String result = template.apply(data);
            
            // Verify result - missing variables should be replaced with empty string
            assertEquals("Title: Test Report, Description: This is a test report, Missing: ", result.trim());
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