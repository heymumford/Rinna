/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import org.rinna.cli.report.ReportConfig;
import org.rinna.cli.report.ReportFormat;
import org.rinna.cli.report.ReportService;
import org.rinna.cli.report.ReportType;

import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of ReportService for testing.
 */
public class MockReportService {
    private final Map<String, Map<String, Object>> scheduledReports = new ConcurrentHashMap<>();
    private boolean generateReportCalled = false;
    private ReportConfig lastReportConfig = null;
    private List<String> generatedReportContents = new ArrayList<>();
    
    /**
     * Checks if generateReport was called.
     * 
     * @return true if generateReport was called
     */
    public boolean wasGenerateReportCalled() {
        return generateReportCalled;
    }
    
    /**
     * Gets the last report configuration that was used.
     * 
     * @return the last report configuration
     */
    public ReportConfig getLastReportConfig() {
        return lastReportConfig;
    }
    
    /**
     * Gets the contents of all generated reports.
     * 
     * @return the report contents
     */
    public List<String> getGeneratedReportContents() {
        return new ArrayList<>(generatedReportContents);
    }
    
    /**
     * Resets the mock state.
     */
    public void reset() {
        generateReportCalled = false;
        lastReportConfig = null;
        generatedReportContents.clear();
        scheduledReports.clear();
    }
    
    /**
     * Mocks generating a report.
     * 
     * @param config the report configuration
     * @return true if the report was generated successfully
     */
    public boolean generateReport(ReportConfig config) {
        generateReportCalled = true;
        lastReportConfig = config;
        
        // Generate a mock report based on the configuration
        StringBuilder report = new StringBuilder();
        report.append("=== ").append(config.getType().name()).append(" Report ===\n");
        report.append("Generated: ").append(LocalDate.now()).append("\n");
        report.append("Format: ").append(config.getFormat()).append("\n");
        if (config.getTitle() != null) {
            report.append("Title: ").append(config.getTitle()).append("\n");
        }
        
        // Add type-specific content
        switch (config.getType()) {
            case SUMMARY:
                report.append("Total items: 5\n");
                report.append("Open items: 3\n");
                report.append("Completed items: 2\n");
                break;
                
            case DETAILED:
                report.append("Item WI-123: Test item (OPEN)\n");
                report.append("Item WI-124: Another item (IN_PROGRESS)\n");
                report.append("Item WI-125: Third item (DONE)\n");
                break;
                
            case STATUS:
                report.append("OPEN: 2\n");
                report.append("IN_PROGRESS: 1\n");
                report.append("DONE: 2\n");
                break;
                
            case PROGRESS:
                report.append("Week 1: 10%\n");
                report.append("Week 2: 30%\n");
                report.append("Week 3: 50%\n");
                report.append("Week 4: 80%\n");
                break;
                
            default:
                report.append("Generic report content\n");
        }
        
        generatedReportContents.add(report.toString());
        return true;
    }
    
    /**
     * Mocks generating a report with the specified type and format.
     * 
     * @param type the report type
     * @param format the report format
     * @return true if the report was generated successfully
     */
    public boolean generateReport(ReportType type, ReportFormat format) {
        ReportConfig config = new ReportConfig();
        config.setType(type);
        config.setFormat(format);
        return generateReport(config);
    }
    
    /**
     * Mocks generating a report with the specified type and format and saves it to a file.
     * 
     * @param type the report type
     * @param format the report format
     * @param outputPath the output file path
     * @return true if the report was generated successfully
     */
    public boolean generateReport(ReportType type, ReportFormat format, String outputPath) {
        ReportConfig config = new ReportConfig();
        config.setType(type);
        config.setFormat(format);
        config.setOutputPath(outputPath);
        return generateReport(config);
    }
    
    /**
     * Gets all scheduled reports.
     * 
     * @return the list of scheduled reports
     */
    public List<Map<String, Object>> getScheduledReports() {
        return new ArrayList<>(scheduledReports.values());
    }
    
    /**
     * Gets a scheduled report by ID.
     * 
     * @param id the ID of the report to get
     * @return the scheduled report, or null if not found
     */
    public Map<String, Object> getScheduledReport(String id) {
        return scheduledReports.get(id);
    }
    
    /**
     * Schedules a report.
     * 
     * @param reportConfig the report configuration
     * @return the ID of the scheduled report
     */
    public Map<String, Object> scheduleReport(Map<String, Object> reportConfig) {
        String id = UUID.randomUUID().toString();
        reportConfig.put("id", id);
        scheduledReports.put(id, reportConfig);
        return reportConfig;
    }
    
    /**
     * Updates a scheduled report.
     * 
     * @param id the ID of the report to update
     * @param updates the updates to apply
     * @return true if the report was updated successfully
     */
    public boolean updateScheduledReport(String id, Map<String, Object> updates) {
        Map<String, Object> report = scheduledReports.get(id);
        if (report != null) {
            report.putAll(updates);
            return true;
        }
        return false;
    }
    
    /**
     * Deletes a scheduled report.
     * 
     * @param id the ID of the report to delete
     * @return true if the report was deleted successfully
     */
    public boolean deleteScheduledReport(String id) {
        return scheduledReports.remove(id) != null;
    }
    
    /**
     * Parses a report type string to a ReportType.
     *
     * @param typeStr the report type string
     * @return the corresponding ReportType, or null if not found
     */
    public ReportType parseReportType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            return ReportType.SUMMARY;
        }
        
        try {
            return ReportType.valueOf(typeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try to match by prefix
            for (ReportType type : ReportType.values()) {
                if (type.name().toLowerCase().startsWith(typeStr.toLowerCase())) {
                    return type;
                }
            }
            
            // Default to summary
            return ReportType.SUMMARY;
        }
    }
    
    /**
     * Parses a format string to a ReportFormat.
     *
     * @param formatStr the format string
     * @return the corresponding ReportFormat, or TEXT if not found
     */
    public ReportFormat parseReportFormat(String formatStr) {
        if (formatStr == null || formatStr.isEmpty()) {
            return ReportFormat.TEXT;
        }
        
        try {
            return ReportFormat.valueOf(formatStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Try to match by prefix
            for (ReportFormat format : ReportFormat.values()) {
                if (format.name().toLowerCase().startsWith(formatStr.toLowerCase())) {
                    return format;
                }
            }
            
            // Default to text
            return ReportFormat.TEXT;
        }
    }
}