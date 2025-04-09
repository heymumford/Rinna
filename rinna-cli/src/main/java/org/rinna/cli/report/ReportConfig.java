/*
 * Report configuration class for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.report;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Configuration for report generation.
 */
public class ReportConfig {
    private ReportType type;
    private ReportFormat format;
    private String outputPath;
    private boolean includeHeader = true;
    private boolean includeTimestamp = true;
    private boolean prettyPrint = true;
    private String title;
    private String description;
    private int maxItems = 0; // 0 means no limit
    private LocalDate startDate;
    private LocalDate endDate;
    private String projectId;
    private List<String> includedFields = new ArrayList<>();
    private List<String> excludedFields = new ArrayList<>();
    private Map<String, String> filters = new HashMap<>();
    private String sortField;
    private boolean ascending = true;
    private boolean groupByEnabled = false;
    private String groupByField;
    private boolean emailEnabled = false;
    private List<String> emailRecipients = new ArrayList<>();
    private String emailSubject;
    
    // Template support
    private String templateName;
    private boolean useTemplate = true;
    
    /**
     * Constructs a new report configuration with default values.
     */
    public ReportConfig() {
        this.type = ReportType.SUMMARY;
        this.format = ReportFormat.TEXT;
        this.outputPath = null; // null means output to console
        this.title = "Rinna Work Item Report";
    }
    
    /**
     * Constructs a new report configuration with the specified type and format.
     * 
     * @param type the report type
     * @param format the report format
     */
    public ReportConfig(ReportType type, ReportFormat format) {
        this.type = type;
        this.format = format;
        this.outputPath = null; // null means output to console
        this.title = "Rinna " + type.name() + " Report";
    }
    
    /**
     * Gets the report type.
     * 
     * @return the report type
     */
    public ReportType getType() {
        return type;
    }
    
    /**
     * Sets the report type.
     * 
     * @param type the report type
     * @return this configuration for chaining
     */
    public ReportConfig setType(ReportType type) {
        this.type = type;
        return this;
    }
    
    /**
     * Gets the report format.
     * 
     * @return the report format
     */
    public ReportFormat getFormat() {
        return format;
    }
    
    /**
     * Sets the report format.
     * 
     * @param format the report format
     * @return this configuration for chaining
     */
    public ReportConfig setFormat(ReportFormat format) {
        this.format = format;
        return this;
    }
    
    /**
     * Gets the output path.
     * 
     * @return the output path, or null if output to console
     */
    public String getOutputPath() {
        return outputPath;
    }
    
    /**
     * Sets the output path.
     * 
     * @param outputPath the output path, or null to output to console
     * @return this configuration for chaining
     */
    public ReportConfig setOutputPath(String outputPath) {
        this.outputPath = outputPath;
        return this;
    }
    
    /**
     * Checks if the header should be included.
     * 
     * @return true if the header should be included
     */
    public boolean isIncludeHeader() {
        return includeHeader;
    }
    
    /**
     * Sets whether to include the header.
     * 
     * @param includeHeader true to include the header
     * @return this configuration for chaining
     */
    public ReportConfig setIncludeHeader(boolean includeHeader) {
        this.includeHeader = includeHeader;
        return this;
    }
    
    /**
     * Checks if the timestamp should be included.
     * 
     * @return true if the timestamp should be included
     */
    public boolean isIncludeTimestamp() {
        return includeTimestamp;
    }
    
    /**
     * Sets whether to include the timestamp.
     * 
     * @param includeTimestamp true to include the timestamp
     * @return this configuration for chaining
     */
    public ReportConfig setIncludeTimestamp(boolean includeTimestamp) {
        this.includeTimestamp = includeTimestamp;
        return this;
    }
    
    /**
     * Checks if pretty printing is enabled.
     * 
     * @return true if pretty printing is enabled
     */
    public boolean isPrettyPrint() {
        return prettyPrint;
    }
    
    /**
     * Sets whether to use pretty printing.
     * 
     * @param prettyPrint true to use pretty printing
     * @return this configuration for chaining
     */
    public ReportConfig setPrettyPrint(boolean prettyPrint) {
        this.prettyPrint = prettyPrint;
        return this;
    }
    
    /**
     * Gets the report title.
     * 
     * @return the report title
     */
    public String getTitle() {
        return title;
    }
    
    /**
     * Sets the report title.
     * 
     * @param title the report title
     * @return this configuration for chaining
     */
    public ReportConfig setTitle(String title) {
        this.title = title;
        return this;
    }
    
    /**
     * Gets the report description.
     * 
     * @return the report description
     */
    public String getDescription() {
        return description;
    }
    
    /**
     * Sets the report description.
     * 
     * @param description the report description
     * @return this configuration for chaining
     */
    public ReportConfig setDescription(String description) {
        this.description = description;
        return this;
    }
    
    /**
     * Gets the maximum number of items to include.
     * 
     * @return the maximum number of items, or 0 for no limit
     */
    public int getMaxItems() {
        return maxItems;
    }
    
    /**
     * Sets the maximum number of items to include.
     * 
     * @param maxItems the maximum number of items, or 0 for no limit
     * @return this configuration for chaining
     */
    public ReportConfig setMaxItems(int maxItems) {
        this.maxItems = maxItems;
        return this;
    }
    
    /**
     * Gets the start date for filtering.
     * 
     * @return the start date
     */
    public LocalDate getStartDate() {
        return startDate;
    }
    
    /**
     * Sets the start date for filtering.
     * 
     * @param startDate the start date
     * @return this configuration for chaining
     */
    public ReportConfig setStartDate(LocalDate startDate) {
        this.startDate = startDate;
        return this;
    }
    
    /**
     * Gets the end date for filtering.
     * 
     * @return the end date
     */
    public LocalDate getEndDate() {
        return endDate;
    }
    
    /**
     * Sets the end date for filtering.
     * 
     * @param endDate the end date
     * @return this configuration for chaining
     */
    public ReportConfig setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        return this;
    }
    
    /**
     * Gets the project ID for filtering.
     * 
     * @return the project ID
     */
    public String getProjectId() {
        return projectId;
    }
    
    /**
     * Sets the project ID for filtering.
     * 
     * @param projectId the project ID
     * @return this configuration for chaining
     */
    public ReportConfig setProjectId(String projectId) {
        this.projectId = projectId;
        return this;
    }
    
    /**
     * Gets the list of fields to include.
     * 
     * @return the list of included fields
     */
    public List<String> getIncludedFields() {
        return new ArrayList<>(includedFields);
    }
    
    /**
     * Sets the list of fields to include.
     * 
     * @param includedFields the list of fields to include
     * @return this configuration for chaining
     */
    public ReportConfig setIncludedFields(List<String> includedFields) {
        this.includedFields = new ArrayList<>(includedFields);
        return this;
    }
    
    /**
     * Adds a field to include.
     * 
     * @param field the field to include
     * @return this configuration for chaining
     */
    public ReportConfig addIncludedField(String field) {
        this.includedFields.add(field);
        return this;
    }
    
    /**
     * Gets the list of fields to exclude.
     * 
     * @return the list of excluded fields
     */
    public List<String> getExcludedFields() {
        return new ArrayList<>(excludedFields);
    }
    
    /**
     * Sets the list of fields to exclude.
     * 
     * @param excludedFields the list of fields to exclude
     * @return this configuration for chaining
     */
    public ReportConfig setExcludedFields(List<String> excludedFields) {
        this.excludedFields = new ArrayList<>(excludedFields);
        return this;
    }
    
    /**
     * Adds a field to exclude.
     * 
     * @param field the field to exclude
     * @return this configuration for chaining
     */
    public ReportConfig addExcludedField(String field) {
        this.excludedFields.add(field);
        return this;
    }
    
    /**
     * Gets the filters map.
     * 
     * @return the filters map
     */
    public Map<String, String> getFilters() {
        return new HashMap<>(filters);
    }
    
    /**
     * Sets the filters map.
     * 
     * @param filters the filters map
     * @return this configuration for chaining
     */
    public ReportConfig setFilters(Map<String, String> filters) {
        this.filters = new HashMap<>(filters);
        return this;
    }
    
    /**
     * Adds a filter.
     * 
     * @param field the field to filter on
     * @param value the filter value
     * @return this configuration for chaining
     */
    public ReportConfig addFilter(String field, String value) {
        this.filters.put(field, value);
        return this;
    }
    
    /**
     * Gets the sort field.
     * 
     * @return the sort field
     */
    public String getSortField() {
        return sortField;
    }
    
    /**
     * Sets the sort field.
     * 
     * @param sortField the sort field
     * @return this configuration for chaining
     */
    public ReportConfig setSortField(String sortField) {
        this.sortField = sortField;
        return this;
    }
    
    /**
     * Checks if sorting is ascending.
     * 
     * @return true if sorting is ascending
     */
    public boolean isAscending() {
        return ascending;
    }
    
    /**
     * Sets whether sorting is ascending.
     * 
     * @param ascending true for ascending, false for descending
     * @return this configuration for chaining
     */
    public ReportConfig setAscending(boolean ascending) {
        this.ascending = ascending;
        return this;
    }
    
    /**
     * Checks if grouping is enabled.
     * 
     * @return true if grouping is enabled
     */
    public boolean isGroupByEnabled() {
        return groupByEnabled;
    }
    
    /**
     * Gets the group by field.
     * 
     * @return the group by field
     */
    public String getGroupByField() {
        return groupByField;
    }
    
    /**
     * Sets grouping by the specified field.
     * 
     * @param field the field to group by
     * @return this configuration for chaining
     */
    public ReportConfig setGroupBy(String field) {
        if (field != null && !field.isEmpty()) {
            this.groupByEnabled = true;
            this.groupByField = field;
        } else {
            this.groupByEnabled = false;
            this.groupByField = null;
        }
        return this;
    }
    
    /**
     * Checks if email is enabled.
     * 
     * @return true if email is enabled
     */
    public boolean isEmailEnabled() {
        return emailEnabled;
    }
    
    /**
     * Sets whether email is enabled.
     * 
     * @param emailEnabled true to enable email
     * @return this configuration for chaining
     */
    public ReportConfig setEmailEnabled(boolean emailEnabled) {
        this.emailEnabled = emailEnabled;
        return this;
    }
    
    /**
     * Gets the email recipients.
     * 
     * @return the list of email recipients
     */
    public List<String> getEmailRecipients() {
        return new ArrayList<>(emailRecipients);
    }
    
    /**
     * Sets the email recipients.
     * 
     * @param emailRecipients the list of email recipients
     * @return this configuration for chaining
     */
    public ReportConfig setEmailRecipients(List<String> emailRecipients) {
        this.emailRecipients = new ArrayList<>(emailRecipients);
        return this;
    }
    
    /**
     * Adds an email recipient.
     * 
     * @param recipient the email recipient to add
     * @return this configuration for chaining
     */
    public ReportConfig addEmailRecipient(String recipient) {
        this.emailRecipients.add(recipient);
        return this;
    }
    
    /**
     * Gets the email subject.
     * 
     * @return the email subject
     */
    public String getEmailSubject() {
        return emailSubject;
    }
    
    /**
     * Sets the email subject.
     * 
     * @param emailSubject the email subject
     * @return this configuration for chaining
     */
    public ReportConfig setEmailSubject(String emailSubject) {
        this.emailSubject = emailSubject;
        return this;
    }
    
    /**
     * Gets the template name.
     * 
     * @return the template name
     */
    public String getTemplateName() {
        return templateName != null ? templateName : type.name().toLowerCase();
    }
    
    /**
     * Sets the template name.
     * 
     * @param templateName the template name
     * @return this configuration for chaining
     */
    public ReportConfig setTemplateName(String templateName) {
        this.templateName = templateName;
        return this;
    }
    
    /**
     * Checks if templates should be used.
     * 
     * @return true if templates should be used
     */
    public boolean isUseTemplate() {
        return useTemplate;
    }
    
    /**
     * Sets whether to use templates.
     * 
     * @param useTemplate true to use templates
     * @return this configuration for chaining
     */
    public ReportConfig setUseTemplate(boolean useTemplate) {
        this.useTemplate = useTemplate;
        return this;
    }
    
    /**
     * Creates a default configuration for the specified report type.
     * 
     * @param type the report type
     * @return a default configuration
     */
    public static ReportConfig createDefault(ReportType type) {
        ReportConfig config = new ReportConfig();
        config.setType(type);
        
        // Set reasonable defaults based on report type
        switch (type) {
            case SUMMARY:
                config.setTitle("Work Item Summary Report");
                config.addIncludedField("id");
                config.addIncludedField("title");
                config.addIncludedField("type");
                config.addIncludedField("state");
                config.addIncludedField("priority");
                config.addIncludedField("assignee");
                break;
                
            case DETAILED:
                config.setTitle("Detailed Work Item Report");
                break;
                
            case STATUS:
                config.setTitle("Work Item Status Report");
                config.setGroupBy("state");
                break;
                
            case PROGRESS:
                config.setTitle("Work Item Progress Report");
                break;
                
            case ASSIGNEE:
                config.setTitle("Work Item Assignee Report");
                config.setGroupBy("assignee");
                break;
                
            case PRIORITY:
                config.setTitle("Work Item Priority Report");
                config.setGroupBy("priority");
                break;
                
            case OVERDUE:
                config.setTitle("Overdue Work Items Report");
                config.addFilter("state", "!DONE,!COMPLETED");
                break;
                
            case TIMELINE:
                config.setTitle("Work Item Timeline Report");
                config.setSortField("dueDate");
                config.setAscending(true);
                break;
                
            case BURNDOWN:
                config.setTitle("Burndown Report");
                break;
                
            case ACTIVITY:
                config.setTitle("Recent Activity Report");
                config.setSortField("updatedAt");
                config.setAscending(false);
                break;
                
            case CUSTOM:
                config.setTitle("Custom Work Item Report");
                break;
        }
        
        return config;
    }
}