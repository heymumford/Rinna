/**
 * Report template for Rinna reports
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.report;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a template for generating reports.
 */
public final class ReportTemplate {
    private static final Logger LOGGER = Logger.getLogger(ReportTemplate.class.getName());
    private static final String DEFAULT_TEMPLATE_PATH = "templates/reports";
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\{\\{\\s*([\\w\\.]+)\\s*\\}\\}");
    
    private String name;
    private String content;
    private ReportFormat format;
    
    /**
     * Creates a new template with the given name and format.
     * 
     * @param name the template name
     * @param format the report format
     * @throws IOException if the template file cannot be read
     */
    public ReportTemplate(String name, ReportFormat format) throws IOException {
        this.name = name;
        this.format = format;
        
        // Load the template content
        loadContent();
    }
    
    /**
     * Loads the template content from the file system.
     * 
     * @throws IOException if the template file cannot be read
     */
    private void loadContent() throws IOException {
        // Find the template file
        Path templatePath = findTemplatePath();
        
        if (templatePath == null) {
            throw new IOException("Template not found: " + name + " for format " + format);
        }
        
        // Read the template content
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                Files.newInputStream(templatePath), StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
            
            this.content = sb.toString();
        }
    }
    
    /**
     * Finds the template file path.
     * 
     * @return the template file path or null if not found
     */
    private Path findTemplatePath() {
        // Check for the template in several locations
        Path[] searchPaths = {
            Paths.get(DEFAULT_TEMPLATE_PATH, name + "." + format.getExtension()),
            Paths.get(DEFAULT_TEMPLATE_PATH, name + "." + format.name().toLowerCase()),
            Paths.get(DEFAULT_TEMPLATE_PATH, format.name().toLowerCase(), name + "." + format.getExtension()),
            Paths.get(System.getProperty("user.home"), ".rinna", "templates", "reports", 
                name + "." + format.getExtension()),
            Paths.get(System.getProperty("user.home"), ".rinna", "templates", "reports", 
                format.name().toLowerCase(), name + "." + format.getExtension())
        };
        
        for (Path path : searchPaths) {
            if (Files.exists(path)) {
                return path;
            }
        }
        
        return null;
    }
    
    /**
     * Applies the template with the given variables.
     * 
     * @param variables the template variables
     * @return the processed template content
     */
    public String apply(Map<String, Object> variables) {
        if (content == null) {
            return "";
        }
        
        String result = content;
        Matcher matcher = VARIABLE_PATTERN.matcher(result);
        
        while (matcher.find()) {
            String variableName = matcher.group(1);
            Object value = getVariableValue(variables, variableName);
            
            String valueStr = value != null ? value.toString() : "";
            result = result.replace(matcher.group(), valueStr);
        }
        
        return result;
    }
    
    /**
     * Gets a variable value by name, supporting dot notation for nested objects.
     * 
     * @param variables the variables map
     * @param name the variable name
     * @return the variable value or null if not found
     */
    private Object getVariableValue(Map<String, Object> variables, String name) {
        if (name.contains(".")) {
            String[] parts = name.split("\\.");
            Object current = variables.get(parts[0]);
            
            for (int i = 1; i < parts.length && current != null; i++) {
                if (current instanceof Map) {
                    current = ((Map<?, ?>) current).get(parts[i]);
                } else {
                    return null;
                }
            }
            
            return current;
        } else {
            return variables.get(name);
        }
    }
    
    /**
     * Gets the name of this template.
     * 
     * @return the template name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Gets the format of this template.
     * 
     * @return the template format
     */
    public ReportFormat getFormat() {
        return format;
    }
    
    /**
     * Gets the content of this template.
     * 
     * @return the template content
     */
    public String getContent() {
        return content;
    }
    
    /**
     * Creates default templates for all supported formats.
     */
    public static void createDefaultTemplates() {
        try {
            Path templateDir = Paths.get(DEFAULT_TEMPLATE_PATH);
            Files.createDirectories(templateDir);
            
            // Create default templates for each format
            for (ReportFormat format : ReportFormat.values()) {
                createDefaultTemplatesForFormat(templateDir, format);
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Failed to create default templates", e);
        }
    }
    
    /**
     * Creates default templates for a specific format.
     * 
     * @param templateDir the template directory
     * @param format the report format
     * @throws IOException if the template files cannot be created
     */
    private static void createDefaultTemplatesForFormat(Path templateDir, ReportFormat format) throws IOException {
        Path formatDir = templateDir.resolve(format.name().toLowerCase());
        Files.createDirectories(formatDir);
        
        // Default templates for different report types
        Map<String, String> templates = new HashMap<>();
        
        switch (format) {
            case HTML:
                templates.put("summary", createHtmlSummaryTemplate());
                templates.put("status", createHtmlStatusTemplate());
                templates.put("detailed", createHtmlDetailedTemplate());
                break;
            case MARKDOWN:
                templates.put("summary", createMarkdownSummaryTemplate());
                templates.put("status", createMarkdownStatusTemplate());
                templates.put("detailed", createMarkdownDetailedTemplate());
                break;
            case TEXT:
                templates.put("summary", createTextSummaryTemplate());
                templates.put("status", createTextStatusTemplate());
                templates.put("detailed", createTextDetailedTemplate());
                break;
            case CSV:
                templates.put("summary", createCsvSummaryTemplate());
                templates.put("status", createCsvStatusTemplate());
                templates.put("detailed", createCsvDetailedTemplate());
                break;
            case JSON:
                // JSON format doesn't use templates
                break;
            case XML:
                templates.put("summary", createXmlSummaryTemplate());
                templates.put("status", createXmlStatusTemplate());
                templates.put("detailed", createXmlDetailedTemplate());
                break;
        }
        
        // Write the templates to files
        for (Map.Entry<String, String> entry : templates.entrySet()) {
            String templateName = entry.getKey();
            String templateContent = entry.getValue();
            
            Path templateFile = formatDir.resolve(templateName + "." + format.getExtension());
            Files.writeString(templateFile, templateContent);
        }
    }
    
    /**
     * Creates a default HTML summary template.
     * 
     * @return the template content
     */
    private static String createHtmlSummaryTemplate() {
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "    <title>{{ title }}</title>\n" +
               "    <style>\n" +
               "        body { font-family: Arial, sans-serif; margin: 20px; }\n" +
               "        h1 { color: #2c3e50; }\n" +
               "        table { border-collapse: collapse; width: 100%; }\n" +
               "        th, td { border: 1px solid #ddd; padding: 8px; }\n" +
               "        th { background-color: #f2f2f2; text-align: left; }\n" +
               "        tr:nth-child(even) { background-color: #f9f9f9; }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <h1>{{ title }}</h1>\n" +
               "    <p>{{ description }}</p>\n" +
               "    <p>Generated on: {{ timestamp }}</p>\n" +
               "    \n" +
               "    <h2>Summary</h2>\n" +
               "    <table>\n" +
               "        <tr>\n" +
               "            <th>Metric</th>\n" +
               "            <th>Value</th>\n" +
               "        </tr>\n" +
               "        <tr>\n" +
               "            <td>Total Items</td>\n" +
               "            <td>{{ summary.totalItems }}</td>\n" +
               "        </tr>\n" +
               "        <tr>\n" +
               "            <td>Completed</td>\n" +
               "            <td>{{ summary.completed }}</td>\n" +
               "        </tr>\n" +
               "        <tr>\n" +
               "            <td>In Progress</td>\n" +
               "            <td>{{ summary.inProgress }}</td>\n" +
               "        </tr>\n" +
               "        <tr>\n" +
               "            <td>Not Started</td>\n" +
               "            <td>{{ summary.notStarted }}</td>\n" +
               "        </tr>\n" +
               "    </table>\n" +
               "    \n" +
               "    <h2>Items by Type</h2>\n" +
               "    <table>\n" +
               "        <tr>\n" +
               "            <th>Type</th>\n" +
               "            <th>Count</th>\n" +
               "        </tr>\n" +
               "        {{ typeRows }}\n" +
               "    </table>\n" +
               "    \n" +
               "    <h2>Items by Priority</h2>\n" +
               "    <table>\n" +
               "        <tr>\n" +
               "            <th>Priority</th>\n" +
               "            <th>Count</th>\n" +
               "        </tr>\n" +
               "        {{ priorityRows }}\n" +
               "    </table>\n" +
               "</body>\n" +
               "</html>";
    }
    
    /**
     * Creates a default HTML status template.
     * 
     * @return the template content
     */
    private static String createHtmlStatusTemplate() {
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "    <title>{{ title }}</title>\n" +
               "    <style>\n" +
               "        body { font-family: Arial, sans-serif; margin: 20px; }\n" +
               "        h1 { color: #2c3e50; }\n" +
               "        table { border-collapse: collapse; width: 100%; }\n" +
               "        th, td { border: 1px solid #ddd; padding: 8px; }\n" +
               "        th { background-color: #f2f2f2; text-align: left; }\n" +
               "        tr:nth-child(even) { background-color: #f9f9f9; }\n" +
               "        .high { color: #c0392b; font-weight: bold; }\n" +
               "        .medium { color: #f39c12; }\n" +
               "        .low { color: #27ae60; }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <h1>{{ title }}</h1>\n" +
               "    <p>{{ description }}</p>\n" +
               "    <p>Generated on: {{ timestamp }}</p>\n" +
               "    \n" +
               "    <h2>Current Status</h2>\n" +
               "    <table>\n" +
               "        <tr>\n" +
               "            <th>ID</th>\n" +
               "            <th>Title</th>\n" +
               "            <th>Type</th>\n" +
               "            <th>Priority</th>\n" +
               "            <th>State</th>\n" +
               "            <th>Assignee</th>\n" +
               "        </tr>\n" +
               "        {{ itemRows }}\n" +
               "    </table>\n" +
               "</body>\n" +
               "</html>";
    }
    
    /**
     * Creates a default HTML detailed template.
     * 
     * @return the template content
     */
    private static String createHtmlDetailedTemplate() {
        return "<!DOCTYPE html>\n" +
               "<html>\n" +
               "<head>\n" +
               "    <title>{{ title }}</title>\n" +
               "    <style>\n" +
               "        body { font-family: Arial, sans-serif; margin: 20px; }\n" +
               "        h1 { color: #2c3e50; }\n" +
               "        h2 { color: #3498db; margin-top: 30px; }\n" +
               "        .item { border: 1px solid #ddd; padding: 15px; margin-bottom: 20px; }\n" +
               "        .item-header { background-color: #f2f2f2; padding: 10px; margin-bottom: 10px; }\n" +
               "        .item-id { font-weight: bold; }\n" +
               "        .item-title { font-size: 18px; margin: 5px 0; }\n" +
               "        .item-meta { color: #7f8c8d; font-size: 14px; }\n" +
               "        .item-description { margin-top: 10px; }\n" +
               "        .high { color: #c0392b; font-weight: bold; }\n" +
               "        .medium { color: #f39c12; }\n" +
               "        .low { color: #27ae60; }\n" +
               "    </style>\n" +
               "</head>\n" +
               "<body>\n" +
               "    <h1>{{ title }}</h1>\n" +
               "    <p>{{ description }}</p>\n" +
               "    <p>Generated on: {{ timestamp }}</p>\n" +
               "    \n" +
               "    <h2>Detailed Work Items</h2>\n" +
               "    {{ detailedItems }}\n" +
               "</body>\n" +
               "</html>";
    }
    
    /**
     * Creates a default Markdown summary template.
     * 
     * @return the template content
     */
    private static String createMarkdownSummaryTemplate() {
        return "# {{ title }}\n\n" +
               "{{ description }}\n\n" +
               "Generated on: {{ timestamp }}\n\n" +
               "## Summary\n\n" +
               "| Metric | Value |\n" +
               "|--------|-------|\n" +
               "| Total Items | {{ summary.totalItems }} |\n" +
               "| Completed | {{ summary.completed }} |\n" +
               "| In Progress | {{ summary.inProgress }} |\n" +
               "| Not Started | {{ summary.notStarted }} |\n\n" +
               "## Items by Type\n\n" +
               "| Type | Count |\n" +
               "|------|-------|\n" +
               "{{ typeRows }}\n\n" +
               "## Items by Priority\n\n" +
               "| Priority | Count |\n" +
               "|----------|-------|\n" +
               "{{ priorityRows }}\n";
    }
    
    /**
     * Creates a default Markdown status template.
     * 
     * @return the template content
     */
    private static String createMarkdownStatusTemplate() {
        return "# {{ title }}\n\n" +
               "{{ description }}\n\n" +
               "Generated on: {{ timestamp }}\n\n" +
               "## Current Status\n\n" +
               "| ID | Title | Type | Priority | State | Assignee |\n" +
               "|----|-------|------|----------|-------|----------|\n" +
               "{{ itemRows }}\n";
    }
    
    /**
     * Creates a default Markdown detailed template.
     * 
     * @return the template content
     */
    private static String createMarkdownDetailedTemplate() {
        return "# {{ title }}\n\n" +
               "{{ description }}\n\n" +
               "Generated on: {{ timestamp }}\n\n" +
               "## Detailed Work Items\n\n" +
               "{{ detailedItems }}\n";
    }
    
    /**
     * Creates a default text summary template.
     * 
     * @return the template content
     */
    private static String createTextSummaryTemplate() {
        return "{{ title }}\n" +
               "{{ underline }}\n\n" +
               "{{ description }}\n\n" +
               "Generated on: {{ timestamp }}\n\n" +
               "Summary:\n" +
               "- Total Items: {{ summary.totalItems }}\n" +
               "- Completed: {{ summary.completed }}\n" +
               "- In Progress: {{ summary.inProgress }}\n" +
               "- Not Started: {{ summary.notStarted }}\n\n" +
               "Items by Type:\n" +
               "{{ typeRows }}\n\n" +
               "Items by Priority:\n" +
               "{{ priorityRows }}\n";
    }
    
    /**
     * Creates a default text status template.
     * 
     * @return the template content
     */
    private static String createTextStatusTemplate() {
        return "{{ title }}\n" +
               "{{ underline }}\n\n" +
               "{{ description }}\n\n" +
               "Generated on: {{ timestamp }}\n\n" +
               "Current Status:\n" +
               "{{ itemRows }}\n";
    }
    
    /**
     * Creates a default text detailed template.
     * 
     * @return the template content
     */
    private static String createTextDetailedTemplate() {
        return "{{ title }}\n" +
               "{{ underline }}\n\n" +
               "{{ description }}\n\n" +
               "Generated on: {{ timestamp }}\n\n" +
               "Detailed Work Items:\n" +
               "{{ detailedItems }}\n";
    }
    
    /**
     * Creates a default CSV summary template.
     * 
     * @return the template content
     */
    private static String createCsvSummaryTemplate() {
        return "Metric,Value\n" +
               "Total Items,{{ summary.totalItems }}\n" +
               "Completed,{{ summary.completed }}\n" +
               "In Progress,{{ summary.inProgress }}\n" +
               "Not Started,{{ summary.notStarted }}\n\n" +
               "Type,Count\n" +
               "{{ typeRows }}\n\n" +
               "Priority,Count\n" +
               "{{ priorityRows }}\n";
    }
    
    /**
     * Creates a default CSV status template.
     * 
     * @return the template content
     */
    private static String createCsvStatusTemplate() {
        return "ID,Title,Type,Priority,State,Assignee\n" +
               "{{ itemRows }}\n";
    }
    
    /**
     * Creates a default CSV detailed template.
     * 
     * @return the template content
     */
    private static String createCsvDetailedTemplate() {
        return "ID,Title,Type,Priority,State,Assignee,Description,Created,Updated,Due Date\n" +
               "{{ detailedItems }}\n";
    }
    
    /**
     * Creates a default XML summary template.
     * 
     * @return the template content
     */
    private static String createXmlSummaryTemplate() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<report>\n" +
               "    <title>{{ title }}</title>\n" +
               "    <description>{{ description }}</description>\n" +
               "    <timestamp>{{ timestamp }}</timestamp>\n" +
               "    <summary>\n" +
               "        <totalItems>{{ summary.totalItems }}</totalItems>\n" +
               "        <completed>{{ summary.completed }}</completed>\n" +
               "        <inProgress>{{ summary.inProgress }}</inProgress>\n" +
               "        <notStarted>{{ summary.notStarted }}</notStarted>\n" +
               "    </summary>\n" +
               "    <typeDistribution>\n" +
               "        {{ typeRows }}\n" +
               "    </typeDistribution>\n" +
               "    <priorityDistribution>\n" +
               "        {{ priorityRows }}\n" +
               "    </priorityDistribution>\n" +
               "</report>";
    }
    
    /**
     * Creates a default XML status template.
     * 
     * @return the template content
     */
    private static String createXmlStatusTemplate() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<report>\n" +
               "    <title>{{ title }}</title>\n" +
               "    <description>{{ description }}</description>\n" +
               "    <timestamp>{{ timestamp }}</timestamp>\n" +
               "    <items>\n" +
               "        {{ itemRows }}\n" +
               "    </items>\n" +
               "</report>";
    }
    
    /**
     * Creates a default XML detailed template.
     * 
     * @return the template content
     */
    private static String createXmlDetailedTemplate() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
               "<report>\n" +
               "    <title>{{ title }}</title>\n" +
               "    <description>{{ description }}</description>\n" +
               "    <timestamp>{{ timestamp }}</timestamp>\n" +
               "    <items>\n" +
               "        {{ detailedItems }}\n" +
               "    </items>\n" +
               "</report>";
    }
}