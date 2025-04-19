/**
 * Template manager for Rinna reports
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.report;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Manages templates for report generation.
 */
public final class TemplateManager {
    private static final Logger LOGGER = Logger.getLogger(TemplateManager.class.getName());
    private static TemplateManager instance;
    
    private final Map<String, ReportTemplate> templateCache = new HashMap<>();
    
    /**
     * Private constructor for singleton pattern.
     */
    private TemplateManager() {
        // Initialize the templates
        initializeTemplates();
    }
    
    /**
     * Gets the singleton instance.
     * 
     * @return the template manager instance
     */
    public static synchronized TemplateManager getInstance() {
        if (instance == null) {
            instance = new TemplateManager();
        }
        return instance;
    }
    
    /**
     * Initializes the templates.
     */
    private void initializeTemplates() {
        // Create default templates
        ReportTemplate.createDefaultTemplates();
    }
    
    /**
     * Gets a template by name and format.
     * 
     * @param name the template name
     * @param format the report format
     * @return the report template
     * @throws IOException if the template cannot be loaded
     */
    public ReportTemplate getTemplate(String name, ReportFormat format) throws IOException {
        String cacheKey = name + ":" + format.name();
        
        // Check the cache first
        if (templateCache.containsKey(cacheKey)) {
            return templateCache.get(cacheKey);
        }
        
        // Load the template
        try {
            ReportTemplate template = new ReportTemplate(name, format);
            templateCache.put(cacheKey, template);
            return template;
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to load template: " + cacheKey, e);
            throw e;
        }
    }
    
    /**
     * Clears the template cache.
     */
    public void clearCache() {
        templateCache.clear();
    }
    
    /**
     * Reloads a template from disk.
     * 
     * @param name the template name
     * @param format the report format
     * @return the reloaded template
     * @throws IOException if the template cannot be loaded
     */
    public ReportTemplate reloadTemplate(String name, ReportFormat format) throws IOException {
        String cacheKey = name + ":" + format.name();
        templateCache.remove(cacheKey);
        return getTemplate(name, format);
    }
    
    /**
     * Creates a context map from a report config and data.
     * 
     * @param config the report configuration
     * @param data the report data
     * @return the context map
     */
    public Map<String, Object> createContext(ReportConfig config, Map<String, Object> data) {
        Map<String, Object> context = new HashMap<>(data);
        
        // Add standard context variables
        context.put("title", config.getTitle() != null ? config.getTitle() : "Rinna Report");
        context.put("description", config.getDescription() != null ? config.getDescription() : "");
        context.put("timestamp", java.time.LocalDateTime.now().toString());
        
        // Add underlines for text format (used in headings)
        if (config.getFormat() == ReportFormat.TEXT && config.getTitle() != null) {
            context.put("underline", "=".repeat(config.getTitle().length()));
        }
        
        return context;
    }
    
    /**
     * Applies a template to the given data.
     * 
     * @param config the report configuration
     * @param data the report data
     * @return the rendered template
     */
    public String applyTemplate(ReportConfig config, Map<String, Object> data) {
        try {
            String templateName = config.getType().name().toLowerCase();
            ReportFormat format = config.getFormat();
            
            // Special case for JSON format - no template needed
            if (format == ReportFormat.JSON) {
                return data.toString();
            }
            
            // Get the template
            ReportTemplate template = getTemplate(templateName, format);
            
            // Create the context
            Map<String, Object> context = createContext(config, data);
            
            // Apply the template
            return template.apply(context);
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to apply template", e);
            return "Error applying template: " + e.getMessage();
        }
    }
}