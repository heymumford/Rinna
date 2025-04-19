/*
 * Report format enum for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.report;

/**
 * Defines different output formats for reports.
 */
public enum ReportFormat {
    /**
     * Plain text format.
     */
    TEXT("txt"),
    
    /**
     * Comma-separated values format.
     */
    CSV("csv"),
    
    /**
     * JSON format for machine-readable output.
     */
    JSON("json"),
    
    /**
     * Markdown format.
     */
    MARKDOWN("md"),
    
    /**
     * HTML format.
     */
    HTML("html"),
    
    /**
     * XML format.
     */
    XML("xml");
    
    private final String fileExtension;
    
    /**
     * Constructs a ReportFormat with the specified file extension.
     * 
     * @param fileExtension the file extension for this format
     */
    ReportFormat(String fileExtension) {
        this.fileExtension = fileExtension;
    }
    
    /**
     * Gets the file extension for this format.
     * 
     * @return the file extension
     */
    public String getFileExtension() {
        return fileExtension;
    }
    
    /**
     * Gets the file extension for this format.
     * This is provided for compatibility with ReportTemplate.
     * 
     * @return the file extension
     */
    public String getExtension() {
        return fileExtension;
    }
    
    /**
     * Parse a format string to a ReportFormat.
     * 
     * @param format the format string
     * @return the corresponding ReportFormat, or TEXT if not found
     */
    public static ReportFormat fromString(String format) {
        if (format == null) {
            return TEXT;
        }
        
        try {
            return valueOf(format.toUpperCase());
        } catch (IllegalArgumentException e) {
            // Handle specific aliases
            if ("md".equalsIgnoreCase(format)) {
                return MARKDOWN;
            }
            // Default to TEXT for unknown formats
            return TEXT;
        }
    }
}