/*
 * Report service class for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.report;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.ServiceManager;

import java.io.File;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Service for generating reports about work items.
 */
public class ReportService {
    private static final Logger LOGGER = Logger.getLogger(ReportService.class.getName());
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static ReportService instance;
    
    private final ReportGenerator reportGenerator;
    private final TemplateManager templateManager;
    
    /**
     * Private constructor for singleton pattern.
     */
    private ReportService() {
        this.reportGenerator = new ReportGenerator();
        this.templateManager = TemplateManager.getInstance();
    }
    
    /**
     * Gets the singleton instance of ReportService.
     * 
     * @return the instance
     */
    public static synchronized ReportService getInstance() {
        if (instance == null) {
            instance = new ReportService();
        }
        return instance;
    }
    
    /**
     * Generates a report with the specified configuration.
     * 
     * @param config the report configuration
     * @return true if the report was generated successfully
     */
    public boolean generateReport(ReportConfig config) {
        LOGGER.info("Generating " + config.getType() + " report in " + config.getFormat() + " format");
        
        // Get work items from the service
        ItemService itemService = ServiceManager.getInstance().getItemService();
        List<WorkItem> workItems = itemService.getAllItems();
        
        // Check if email is enabled
        if (config.isEmailEnabled() && !config.getEmailRecipients().isEmpty()) {
            LOGGER.info("Email enabled, will send report to " + config.getEmailRecipients().size() + " recipients");
            
            // Save the original output path
            String originalOutputPath = config.getOutputPath();
            
            try {
                // Create a temporary file
                // Create temporary file in target directory to ensure it gets cleaned up with the project
                // Use absolute path to ensure consistent behavior regardless of current working directory
                String projectRoot = System.getProperty("user.dir");
                File targetDir = new File(projectRoot, "target/temp");
                if (!targetDir.exists()) {
                    targetDir.mkdirs();
                }
                File tempFile = new File(targetDir, "report_" + System.currentTimeMillis() + "." + config.getFormat().getFileExtension());
                
                // Set the temp file as output path
                config.setOutputPath(tempFile.getAbsolutePath());
                
                // Generate the report to the temp file
                boolean success = reportGenerator.generateReport(config, workItems);
                
                if (!success) {
                    LOGGER.warning("Failed to generate report to temp file");
                    return false;
                }
                
                // Read the report content
                String reportContent = new String(java.nio.file.Files.readAllBytes(tempFile.toPath()));
                
                // Send the report via email
                EmailService emailService = EmailService.getInstance();
                boolean emailSuccess = emailService.sendReport(config, reportContent);
                
                if (!emailSuccess) {
                    LOGGER.warning("Failed to send report email");
                }
                
                // Restore the original output path
                config.setOutputPath(originalOutputPath);
                
                // If we have an output path, generate the report to it as well
                if (originalOutputPath != null && !originalOutputPath.isEmpty()) {
                    return reportGenerator.generateReport(config, workItems);
                }
                
                return true;
            } catch (Exception e) {
                LOGGER.severe("Error sending report email: " + e.getMessage());
                return false;
            }
        } else {
            // Normal report generation
            return reportGenerator.generateReport(config, workItems);
        }
    }
    
    /**
     * Generates a report with the specified type and format.
     * 
     * @param type the report type
     * @param format the report format
     * @return true if the report was generated successfully
     */
    public boolean generateReport(ReportType type, ReportFormat format) {
        ReportConfig config = ReportConfig.createDefault(type);
        config.setFormat(format);
        return generateReport(config);
    }
    
    /**
     * Generates a report with the specified type and format and saves it to a file.
     * 
     * @param type the report type
     * @param format the report format
     * @param outputPath the output file path
     * @return true if the report was generated successfully
     */
    public boolean generateReport(ReportType type, ReportFormat format, String outputPath) {
        ReportConfig config = ReportConfig.createDefault(type);
        config.setFormat(format);
        config.setOutputPath(outputPath);
        return generateReport(config);
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
            LOGGER.warning("Unknown report type: " + typeStr + ", defaulting to SUMMARY");
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
        return ReportFormat.fromString(formatStr);
    }
    
    /**
     * Resolves the output path for a report.
     * 
     * @param basePath the base path (can be null)
     * @param type the report type
     * @param format the report format
     * @return the resolved output path, or null if the report should be output to console
     */
    public String resolveOutputPath(String basePath, ReportType type, ReportFormat format) {
        if (basePath == null || basePath.isEmpty() || basePath.equals("-")) {
            return null;
        }
        
        // Convert to absolute path if needed
        String absolutePath = basePath;
        File baseFile = new File(basePath);
        
        // If not already an absolute path, make it absolute
        if (!baseFile.isAbsolute()) {
            String projectRoot = System.getProperty("user.dir");
            baseFile = new File(projectRoot, basePath);
            absolutePath = baseFile.getAbsolutePath();
        }
        
        // Check if the path is a directory
        if (baseFile.isDirectory() || basePath.endsWith("/") || basePath.endsWith("\\")) {
            // Create a filename based on the report type and format
            String fileName = "rinna_" + type.name().toLowerCase() + "_report." + format.getFileExtension();
            return Paths.get(absolutePath, fileName).toString();
        }
        
        // If no extension is provided, add the format extension
        if (!absolutePath.contains(".")) {
            return absolutePath + "." + format.getFileExtension();
        }
        
        // Use the absolute path
        return absolutePath;
    }
    
    /**
     * Parses a date string to a LocalDate.
     * 
     * @param dateStr the date string
     * @return the parsed date, or null if the string is null, empty, or invalid
     */
    public LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            return null;
        }
        
        try {
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            LOGGER.warning("Invalid date format: " + dateStr + ", expected yyyy-MM-dd");
            return null;
        }
    }
    
    /**
     * Gets a report template.
     * 
     * @param name the template name
     * @param format the report format
     * @return the report template, or null if not found
     */
    public ReportTemplate getTemplate(String name, ReportFormat format) {
        try {
            return templateManager.getTemplate(name, format);
        } catch (Exception e) {
            LOGGER.warning("Failed to load template: " + name + " for format " + format + ": " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Applies a template to the given data.
     * 
     * @param config the report configuration
     * @param data the report data
     * @return the rendered template
     */
    public String applyTemplate(ReportConfig config, java.util.Map<String, Object> data) {
        return templateManager.applyTemplate(config, data);
    }
}