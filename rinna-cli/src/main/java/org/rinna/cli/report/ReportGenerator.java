/*
 * Report generator class for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.report;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.model.Priority;
import org.rinna.cli.adapter.ReportItemAdapter;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Generates reports based on ReportConfig and work items.
 */
public class ReportGenerator {
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    private final TemplateManager templateManager;
    
    /**
     * Constructs a new report generator.
     */
    public ReportGenerator() {
        this.templateManager = TemplateManager.getInstance();
    }

    /**
     * Generates a report based on the provided configuration and work items.
     *
     * @param config the report configuration
     * @param workItems the work items to include in the report
     * @return true if the report was generated successfully
     */
    public boolean generateReport(ReportConfig config, List<WorkItem> workItems) {
        // Apply filters
        List<WorkItem> filteredItems = filterWorkItems(workItems, config);
        
        // Apply sorting
        if (config.getSortField() != null && !config.getSortField().isEmpty()) {
            filteredItems = sortWorkItems(filteredItems, config.getSortField(), config.isAscending());
        }
        
        // Apply limit
        if (config.getMaxItems() > 0 && filteredItems.size() > config.getMaxItems()) {
            filteredItems = filteredItems.subList(0, config.getMaxItems());
        }
        
        // Generate the report based on the format
        String reportContent = formatReport(config, filteredItems);
        
        // Output the report
        return outputReport(config, reportContent);
    }
    
    /**
     * Filters work items based on the report configuration.
     *
     * @param workItems the work items to filter
     * @param config the report configuration
     * @return the filtered work items
     */
    private List<WorkItem> filterWorkItems(List<WorkItem> workItems, ReportConfig config) {
        return workItems.stream()
            .filter(item -> matchesFilters(item, config))
            .filter(item -> matchesDateRange(item, config))
            .filter(item -> matchesProject(item, config))
            .collect(Collectors.toList());
    }
    
    /**
     * Checks if a work item matches the filters in the report configuration.
     *
     * @param item the work item to check
     * @param config the report configuration
     * @return true if the item matches the filters
     */
    private boolean matchesFilters(WorkItem item, ReportConfig config) {
        Map<String, String> filters = config.getFilters();
        
        if (filters.isEmpty()) {
            return true;
        }
        
        for (Map.Entry<String, String> filter : filters.entrySet()) {
            String field = filter.getKey();
            String value = filter.getValue();
            
            // Handle negation
            boolean negate = value.startsWith("!");
            if (negate) {
                value = value.substring(1);
            }
            
            // Handle multiple values
            String[] values = value.split(",");
            
            boolean matches = false;
            for (String val : values) {
                switch (field.toLowerCase()) {
                    case "id":
                        matches = item.getId().equals(val);
                        break;
                    case "title":
                        matches = item.getTitle().toLowerCase().contains(val.toLowerCase());
                        break;
                    case "description":
                        matches = item.getDescription() != null && 
                                item.getDescription().toLowerCase().contains(val.toLowerCase());
                        break;
                    case "type":
                        matches = item.getType().toString().equalsIgnoreCase(val);
                        break;
                    case "state":
                        matches = item.getState().toString().equalsIgnoreCase(val);
                        break;
                    case "priority":
                        matches = item.getPriority().toString().equalsIgnoreCase(val);
                        break;
                    case "assignee":
                        matches = item.getAssignee() != null && 
                                item.getAssignee().equalsIgnoreCase(val);
                        break;
                    default:
                        // Unknown field, skip
                        continue;
                }
                
                if (matches) {
                    break;
                }
            }
            
            // Negate the match if needed
            if (negate) {
                matches = !matches;
            }
            
            if (!matches) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Checks if a work item matches the date range in the report configuration.
     *
     * @param item the work item to check
     * @param config the report configuration
     * @return true if the item matches the date range
     */
    private boolean matchesDateRange(WorkItem item, ReportConfig config) {
        // If no date range is specified, all items match
        if (config.getStartDate() == null && config.getEndDate() == null) {
            return true;
        }
        
        ReportItemAdapter adapter = new ReportItemAdapter(item);
        LocalDate creationDate = adapter.getCreatedAt().atZone(ZoneId.systemDefault()).toLocalDate();
        
        // Check start date
        if (config.getStartDate() != null && creationDate.isBefore(config.getStartDate())) {
            return false;
        }
        
        // Check end date
        if (config.getEndDate() != null && creationDate.isAfter(config.getEndDate())) {
            return false;
        }
        
        return true;
    }
    
    /**
     * Checks if a work item matches the project in the report configuration.
     *
     * @param item the work item to check
     * @param config the report configuration
     * @return true if the item matches the project
     */
    private boolean matchesProject(WorkItem item, ReportConfig config) {
        // If no project is specified, all items match
        if (config.getProjectId() == null || config.getProjectId().isEmpty()) {
            return true;
        }
        
        return config.getProjectId().equals(item.getProjectId());
    }
    
    /**
     * Sorts work items based on the specified field and order.
     *
     * @param workItems the work items to sort
     * @param sortField the field to sort by
     * @param ascending true for ascending order, false for descending
     * @return the sorted work items
     */
    private List<WorkItem> sortWorkItems(List<WorkItem> workItems, String sortField, boolean ascending) {
        Comparator<WorkItem> comparator = null;
        
        switch (sortField.toLowerCase()) {
            case "id":
                comparator = Comparator.comparing(WorkItem::getId);
                break;
            case "title":
                comparator = Comparator.comparing(WorkItem::getTitle);
                break;
            case "type":
                comparator = Comparator.comparing(item -> item.getType().toString());
                break;
            case "state":
                comparator = Comparator.comparing(item -> item.getState().toString());
                break;
            case "priority":
                comparator = Comparator.comparing(item -> item.getPriority().ordinal());
                break;
            case "assignee":
                comparator = Comparator.comparing(
                    item -> item.getAssignee() != null ? item.getAssignee() : "", 
                    String.CASE_INSENSITIVE_ORDER
                );
                break;
            case "createdat":
            case "created":
                comparator = Comparator.comparing(item -> new ReportItemAdapter(item).getCreatedAt());
                break;
            case "updatedat":
            case "updated":
                comparator = Comparator.comparing(item -> new ReportItemAdapter(item).getUpdatedAt());
                break;
            case "duedate":
            case "due":
                comparator = Comparator.comparing(
                    item -> item.getDueDate() != null ? item.getDueDate() : LocalDate.MAX
                );
                break;
            default:
                // Default to sorting by ID
                comparator = Comparator.comparing(WorkItem::getId);
        }
        
        // Reverse the comparator if descending
        if (!ascending) {
            comparator = comparator.reversed();
        }
        
        return workItems.stream()
            .sorted(comparator)
            .collect(Collectors.toList());
    }
    
    /**
     * Formats the report based on the report configuration and filtered work items.
     *
     * @param config the report configuration
     * @param workItems the filtered work items
     * @return the formatted report content
     */
    private String formatReport(ReportConfig config, List<WorkItem> workItems) {
        // Check if we should use templates
        if (config.isUseTemplate()) {
            try {
                // Create report data
                Map<String, Object> reportData = createReportData(config, workItems);
                
                // Try to apply template
                return templateManager.applyTemplate(config, reportData);
            } catch (Exception e) {
                System.err.println("Warning: Failed to apply template: " + e.getMessage());
                System.err.println("Falling back to built-in formatting...");
                // Fall through to built-in formatting
            }
        }
        
        // Use built-in formatting as fallback
        switch (config.getFormat()) {
            case TEXT:
                return formatTextReport(config, workItems);
            case CSV:
                return formatCsvReport(config, workItems);
            case JSON:
                return formatJsonReport(config, workItems);
            case MARKDOWN:
                return formatMarkdownReport(config, workItems);
            case HTML:
                return formatHtmlReport(config, workItems);
            case XML:
                return formatXmlReport(config, workItems);
            default:
                return formatTextReport(config, workItems);
        }
    }
    
    /**
     * Creates a data map for templates.
     *
     * @param config the report configuration
     * @param workItems the filtered work items
     * @return the data map
     */
    private Map<String, Object> createReportData(ReportConfig config, List<WorkItem> workItems) {
        Map<String, Object> data = new HashMap<>();
        
        // Add basic data
        data.put("title", config.getTitle());
        data.put("description", "Report of " + workItems.size() + " work items");
        data.put("timestamp", LocalDateTime.now().format(TIME_FORMATTER));
        data.put("count", workItems.size());
        
        // Add items
        data.put("items", workItems);
        
        // Create summary data
        Map<String, Object> summary = new HashMap<>();
        
        summary.put("totalItems", workItems.size());
        
        long completed = workItems.stream()
            .filter(item -> item.getStatus() == WorkflowState.DONE)
            .count();
        summary.put("completed", completed);
        
        long inProgress = workItems.stream()
            .filter(item -> item.getStatus() == WorkflowState.IN_PROGRESS)
            .count();
        summary.put("inProgress", inProgress);
        
        long notStarted = workItems.stream()
            .filter(item -> item.getStatus() == WorkflowState.TO_DO)
            .count();
        summary.put("notStarted", notStarted);
        
        data.put("summary", summary);
        
        // Add type distribution
        Map<WorkItemType, Long> typeCount = workItems.stream()
            .collect(Collectors.groupingBy(WorkItem::getType, Collectors.counting()));
        data.put("types", typeCount);
        
        // Generate type rows for templates
        StringBuilder typeRows = new StringBuilder();
        for (Map.Entry<WorkItemType, Long> entry : typeCount.entrySet()) {
            String typeName = entry.getKey().toString();
            Long count = entry.getValue();
            
            if (config.getFormat() == ReportFormat.MARKDOWN) {
                typeRows.append("| ").append(typeName).append(" | ").append(count).append(" |\n");
            } else if (config.getFormat() == ReportFormat.CSV) {
                typeRows.append(quoteForCsv(typeName)).append(",").append(count).append("\n");
            } else if (config.getFormat() == ReportFormat.XML) {
                typeRows.append("        <type name=\"").append(typeName).append("\" count=\"")
                      .append(count).append("\" />\n");
            } else if (config.getFormat() == ReportFormat.HTML) {
                typeRows.append("<tr><td>").append(typeName).append("</td><td>")
                      .append(count).append("</td></tr>\n");
            } else {
                typeRows.append(typeName).append(": ").append(count).append("\n");
            }
        }
        data.put("typeRows", typeRows.toString());
        
        // Add priority distribution
        Map<Priority, Long> priorityCount = workItems.stream()
            .collect(Collectors.groupingBy(WorkItem::getPriority, Collectors.counting()));
        data.put("priorities", priorityCount);
        
        // Generate priority rows for templates
        StringBuilder priorityRows = new StringBuilder();
        for (Map.Entry<Priority, Long> entry : priorityCount.entrySet()) {
            String priorityName = entry.getKey().toString();
            Long count = entry.getValue();
            
            if (config.getFormat() == ReportFormat.MARKDOWN) {
                priorityRows.append("| ").append(priorityName).append(" | ").append(count).append(" |\n");
            } else if (config.getFormat() == ReportFormat.CSV) {
                priorityRows.append(quoteForCsv(priorityName)).append(",").append(count).append("\n");
            } else if (config.getFormat() == ReportFormat.XML) {
                priorityRows.append("        <priority name=\"").append(priorityName).append("\" count=\"")
                      .append(count).append("\" />\n");
            } else if (config.getFormat() == ReportFormat.HTML) {
                priorityRows.append("<tr><td>").append(priorityName).append("</td><td>")
                      .append(count).append("</td></tr>\n");
            } else {
                priorityRows.append(priorityName).append(": ").append(count).append("\n");
            }
        }
        data.put("priorityRows", priorityRows.toString());
        
        // Generate item rows for templates
        StringBuilder itemRows = new StringBuilder();
        for (WorkItem item : workItems) {
            if (config.getFormat() == ReportFormat.MARKDOWN) {
                itemRows.append("| ").append(item.getId()).append(" | ")
                      .append(item.getTitle()).append(" | ")
                      .append(item.getType()).append(" | ")
                      .append(item.getPriority()).append(" | ")
                      .append(item.getState()).append(" | ")
                      .append(item.getAssignee() != null ? item.getAssignee() : "").append(" |\n");
            } else if (config.getFormat() == ReportFormat.CSV) {
                itemRows.append(quoteForCsv(item.getId())).append(",")
                      .append(quoteForCsv(item.getTitle())).append(",")
                      .append(quoteForCsv(item.getType().toString())).append(",")
                      .append(quoteForCsv(item.getPriority().toString())).append(",")
                      .append(quoteForCsv(item.getState().toString())).append(",")
                      .append(quoteForCsv(item.getAssignee() != null ? item.getAssignee() : "")).append("\n");
            } else if (config.getFormat() == ReportFormat.XML) {
                itemRows.append("    <item id=\"").append(item.getId()).append("\">\n")
                      .append("      <title>").append(escapeXml(item.getTitle())).append("</title>\n")
                      .append("      <type>").append(item.getType()).append("</type>\n")
                      .append("      <priority>").append(item.getPriority()).append("</priority>\n")
                      .append("      <state>").append(item.getState()).append("</state>\n");
                
                if (item.getAssignee() != null) {
                    itemRows.append("      <assignee>").append(escapeXml(item.getAssignee())).append("</assignee>\n");
                }
                
                itemRows.append("    </item>\n");
            } else if (config.getFormat() == ReportFormat.HTML) {
                itemRows.append("    <tr>\n")
                      .append("      <td>").append(item.getId()).append("</td>\n")
                      .append("      <td>").append(escapeHtml(item.getTitle())).append("</td>\n")
                      .append("      <td>").append(item.getType()).append("</td>\n")
                      .append("      <td class=\"priority-").append(item.getPriority()).append("\">")
                      .append(item.getPriority()).append("</td>\n")
                      .append("      <td class=\"state-").append(item.getState()).append("\">")
                      .append(item.getState()).append("</td>\n")
                      .append("      <td>").append(item.getAssignee() != null ? 
                          escapeHtml(item.getAssignee()) : "").append("</td>\n")
                      .append("    </tr>\n");
            } else {
                itemRows.append(item.getId()).append(": ").append(item.getTitle()).append("\n")
                      .append("  Type: ").append(item.getType()).append("\n")
                      .append("  Priority: ").append(item.getPriority()).append("\n")
                      .append("  State: ").append(item.getState()).append("\n");
                
                if (item.getAssignee() != null) {
                    itemRows.append("  Assignee: ").append(item.getAssignee()).append("\n");
                }
                
                itemRows.append("\n");
            }
        }
        data.put("itemRows", itemRows.toString());
        
        // Generate detailed items for templates
        StringBuilder detailedItems = new StringBuilder();
        for (WorkItem item : workItems) {
            if (config.getFormat() == ReportFormat.MARKDOWN) {
                detailedItems.append("### ").append(item.getId()).append(": ").append(item.getTitle()).append("\n\n");
                detailedItems.append("- **Type:** ").append(item.getType()).append("\n");
                detailedItems.append("- **Priority:** ").append(item.getPriority()).append("\n");
                detailedItems.append("- **State:** ").append(item.getState()).append("\n");
                
                if (item.getAssignee() != null) {
                    detailedItems.append("- **Assignee:** ").append(item.getAssignee()).append("\n");
                }
                
                if (item.getDescription() != null) {
                    detailedItems.append("\n").append(item.getDescription()).append("\n\n");
                } else {
                    detailedItems.append("\n");
                }
            } else if (config.getFormat() == ReportFormat.CSV) {
                detailedItems.append(quoteForCsv(item.getId())).append(",")
                      .append(quoteForCsv(item.getTitle())).append(",")
                      .append(quoteForCsv(item.getType().toString())).append(",")
                      .append(quoteForCsv(item.getPriority().toString())).append(",")
                      .append(quoteForCsv(item.getState().toString())).append(",")
                      .append(quoteForCsv(item.getAssignee() != null ? item.getAssignee() : "")).append(",")
                      .append(quoteForCsv(item.getDescription() != null ? item.getDescription() : "")).append(",")
                      .append(quoteForCsv(new ReportItemAdapter(item).getCreatedFormatted(TIME_FORMATTER))).append(",")
                      .append(quoteForCsv(new ReportItemAdapter(item).getUpdatedFormatted(TIME_FORMATTER))).append(",")
                      .append(quoteForCsv(item.getDueDate() != null ? 
                          item.getDueDate().format(DATE_FORMATTER) : "")).append("\n");
            } else if (config.getFormat() == ReportFormat.XML) {
                detailedItems.append("    <item id=\"").append(item.getId()).append("\">\n")
                      .append("      <title>").append(escapeXml(item.getTitle())).append("</title>\n")
                      .append("      <type>").append(item.getType()).append("</type>\n")
                      .append("      <priority>").append(item.getPriority()).append("</priority>\n")
                      .append("      <state>").append(item.getState()).append("</state>\n");
                
                if (item.getAssignee() != null) {
                    detailedItems.append("      <assignee>").append(escapeXml(item.getAssignee())).append("</assignee>\n");
                }
                
                if (item.getDescription() != null) {
                    detailedItems.append("      <description>").append(escapeXml(item.getDescription())).append("</description>\n");
                }
                
                detailedItems.append("      <createdAt>").append(new ReportItemAdapter(item).getCreatedFormatted(TIME_FORMATTER)).append("</createdAt>\n")
                      .append("      <updatedAt>").append(new ReportItemAdapter(item).getUpdatedFormatted(TIME_FORMATTER)).append("</updatedAt>\n");
                
                if (item.getDueDate() != null) {
                    detailedItems.append("      <dueDate>").append(item.getDueDate().format(DATE_FORMATTER)).append("</dueDate>\n");
                }
                
                detailedItems.append("    </item>\n");
            } else if (config.getFormat() == ReportFormat.HTML) {
                detailedItems.append("<div class=\"item\">\n")
                      .append("  <div class=\"item-header\">\n")
                      .append("    <span class=\"item-id\">").append(item.getId()).append("</span> - \n")
                      .append("    <span class=\"item-title\">").append(escapeHtml(item.getTitle())).append("</span>\n")
                      .append("  </div>\n")
                      .append("  <div class=\"item-meta\">\n")
                      .append("    Type: <span class=\"type-").append(item.getType()).append("\">")
                      .append(item.getType()).append("</span> | \n")
                      .append("    Priority: <span class=\"priority-").append(item.getPriority()).append("\">")
                      .append(item.getPriority()).append("</span> | \n")
                      .append("    State: <span class=\"state-").append(item.getState()).append("\">")
                      .append(item.getState()).append("</span>");
                
                if (item.getAssignee() != null) {
                    detailedItems.append(" | Assignee: ").append(escapeHtml(item.getAssignee()));
                }
                
                detailedItems.append("\n  </div>\n");
                
                if (item.getDescription() != null) {
                    detailedItems.append("  <div class=\"item-description\">\n")
                          .append("    ").append(escapeHtml(item.getDescription()).replace("\n", "<br>")).append("\n")
                          .append("  </div>\n");
                }
                
                detailedItems.append("  <div class=\"item-dates\">\n")
                      .append("    Created: ").append(new ReportItemAdapter(item).getCreatedFormatted(TIME_FORMATTER));
                
                if (item.getDueDate() != null) {
                    detailedItems.append(" | Due: ").append(item.getDueDate().format(DATE_FORMATTER));
                }
                
                detailedItems.append("\n  </div>\n")
                      .append("</div>\n");
            } else {
                detailedItems.append(item.getId()).append(": ").append(item.getTitle()).append("\n")
                      .append("  Type: ").append(item.getType()).append("\n")
                      .append("  Priority: ").append(item.getPriority()).append("\n")
                      .append("  State: ").append(item.getState()).append("\n");
                
                if (item.getAssignee() != null) {
                    detailedItems.append("  Assignee: ").append(item.getAssignee()).append("\n");
                }
                
                if (item.getDescription() != null) {
                    detailedItems.append("  Description: ").append(item.getDescription()).append("\n");
                }
                
                detailedItems.append("  Created: ").append(new ReportItemAdapter(item).getCreatedFormatted(TIME_FORMATTER)).append("\n")
                      .append("  Updated: ").append(new ReportItemAdapter(item).getUpdatedFormatted(TIME_FORMATTER)).append("\n");
                
                if (item.getDueDate() != null) {
                    detailedItems.append("  Due Date: ").append(item.getDueDate().format(DATE_FORMATTER)).append("\n");
                }
                
                detailedItems.append("\n");
            }
        }
        data.put("detailedItems", detailedItems.toString());
        
        return data;
    }
    
    /**
     * Formats the report as plain text.
     *
     * @param config the report configuration
     * @param workItems the filtered work items
     * @return the formatted text report
     */
    private String formatTextReport(ReportConfig config, List<WorkItem> workItems) {
        StringBuilder sb = new StringBuilder();
        
        // Add header
        if (config.isIncludeHeader()) {
            sb.append(config.getTitle()).append("\n");
            sb.append("=".repeat(config.getTitle().length())).append("\n\n");
            
            if (config.isIncludeTimestamp()) {
                sb.append("Generated: ").append(LocalDateTime.now().format(TIME_FORMATTER)).append("\n\n");
            }
        }
        
        // Check if we need to group
        if (config.isGroupByEnabled()) {
            Map<String, List<WorkItem>> groupedItems = groupWorkItems(workItems, config.getGroupByField());
            
            for (Map.Entry<String, List<WorkItem>> entry : groupedItems.entrySet()) {
                String groupName = entry.getKey();
                List<WorkItem> groupItems = entry.getValue();
                
                sb.append(groupName).append(" (").append(groupItems.size()).append(" items)\n");
                sb.append("-".repeat(groupName.length() + 12)).append("\n");
                
                for (WorkItem item : groupItems) {
                    formatTextWorkItem(sb, item, config);
                }
                
                sb.append("\n");
            }
        } else {
            // No grouping
            for (WorkItem item : workItems) {
                formatTextWorkItem(sb, item, config);
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Formats a work item as plain text.
     *
     * @param sb the string builder to append to
     * @param item the work item to format
     * @param config the report configuration
     */
    private void formatTextWorkItem(StringBuilder sb, WorkItem item, ReportConfig config) {
        List<String> includedFields = config.getIncludedFields();
        List<String> excludedFields = config.getExcludedFields();
        
        sb.append(item.getId()).append(": ").append(item.getTitle()).append("\n");
        
        // Add type, state, and priority if not excluded
        if (shouldIncludeField("type", includedFields, excludedFields)) {
            sb.append("  Type: ").append(item.getType()).append("\n");
        }
        
        if (shouldIncludeField("state", includedFields, excludedFields)) {
            sb.append("  State: ").append(item.getState()).append("\n");
        }
        
        if (shouldIncludeField("priority", includedFields, excludedFields)) {
            sb.append("  Priority: ").append(item.getPriority()).append("\n");
        }
        
        if (shouldIncludeField("assignee", includedFields, excludedFields) && item.getAssignee() != null) {
            sb.append("  Assignee: ").append(item.getAssignee()).append("\n");
        }
        
        if (shouldIncludeField("description", includedFields, excludedFields) && item.getDescription() != null) {
            sb.append("  Description: ").append(item.getDescription()).append("\n");
        }
        
        if (shouldIncludeField("duedate", includedFields, excludedFields) && item.getDueDate() != null) {
            sb.append("  Due Date: ").append(item.getDueDate().format(DATE_FORMATTER)).append("\n");
        }
        
        if (shouldIncludeField("createdat", includedFields, excludedFields)) {
            sb.append("  Created: ").append(new ReportItemAdapter(item).getCreatedFormatted(TIME_FORMATTER)).append("\n");
        }
        
        if (shouldIncludeField("updatedat", includedFields, excludedFields)) {
            sb.append("  Updated: ").append(new ReportItemAdapter(item).getUpdatedFormatted(TIME_FORMATTER)).append("\n");
        }
        
        sb.append("\n");
    }
    
    /**
     * Formats the report as CSV.
     *
     * @param config the report configuration
     * @param workItems the filtered work items
     * @return the formatted CSV report
     */
    private String formatCsvReport(ReportConfig config, List<WorkItem> workItems) {
        StringBuilder sb = new StringBuilder();
        List<String> includedFields = config.getIncludedFields();
        List<String> excludedFields = config.getExcludedFields();
        
        // Determine fields to include
        List<String> fields = new ArrayList<>();
        fields.add("id");
        fields.add("title");
        
        // Add other common fields if not excluded
        if (shouldIncludeField("type", includedFields, excludedFields)) fields.add("type");
        if (shouldIncludeField("state", includedFields, excludedFields)) fields.add("state");
        if (shouldIncludeField("priority", includedFields, excludedFields)) fields.add("priority");
        if (shouldIncludeField("assignee", includedFields, excludedFields)) fields.add("assignee");
        if (shouldIncludeField("description", includedFields, excludedFields)) fields.add("description");
        if (shouldIncludeField("duedate", includedFields, excludedFields)) fields.add("dueDate");
        if (shouldIncludeField("createdat", includedFields, excludedFields)) fields.add("createdAt");
        if (shouldIncludeField("updatedat", includedFields, excludedFields)) fields.add("updatedAt");
        
        // Add header row
        if (config.isIncludeHeader()) {
            sb.append(String.join(",", fields)).append("\n");
        }
        
        // Add data rows
        for (WorkItem item : workItems) {
            List<String> values = new ArrayList<>();
            
            for (String field : fields) {
                switch (field.toLowerCase()) {
                    case "id":
                        values.add(quoteForCsv(item.getId()));
                        break;
                    case "title":
                        values.add(quoteForCsv(item.getTitle()));
                        break;
                    case "type":
                        values.add(quoteForCsv(item.getType().toString()));
                        break;
                    case "state":
                        values.add(quoteForCsv(item.getState().toString()));
                        break;
                    case "priority":
                        values.add(quoteForCsv(item.getPriority().toString()));
                        break;
                    case "assignee":
                        values.add(quoteForCsv(item.getAssignee() != null ? item.getAssignee() : ""));
                        break;
                    case "description":
                        values.add(quoteForCsv(item.getDescription() != null ? item.getDescription() : ""));
                        break;
                    case "duedate":
                        values.add(quoteForCsv(item.getDueDate() != null ? 
                                item.getDueDate().format(DATE_FORMATTER) : ""));
                        break;
                    case "createdat":
                        values.add(quoteForCsv(new ReportItemAdapter(item).getCreatedFormatted(TIME_FORMATTER)));
                        break;
                    case "updatedat":
                        values.add(quoteForCsv(new ReportItemAdapter(item).getUpdatedFormatted(TIME_FORMATTER)));
                        break;
                }
            }
            
            sb.append(String.join(",", values)).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Quotes a string for CSV format.
     *
     * @param value the value to quote
     * @return the quoted value
     */
    private String quoteForCsv(String value) {
        if (value == null) {
            return "\"\"";
        }
        
        // Escape quotes by doubling them
        String escaped = value.replace("\"", "\"\"");
        
        // Quote the value
        return "\"" + escaped + "\"";
    }
    
    /**
     * Formats the report as JSON.
     *
     * @param config the report configuration
     * @param workItems the filtered work items
     * @return the formatted JSON report
     */
    private String formatJsonReport(ReportConfig config, List<WorkItem> workItems) {
        StringBuilder sb = new StringBuilder();
        List<String> includedFields = config.getIncludedFields();
        List<String> excludedFields = config.getExcludedFields();
        String indent = config.isPrettyPrint() ? "  " : "";
        
        sb.append("{\n");
        
        // Add metadata
        if (config.isIncludeHeader()) {
            sb.append(indent).append("\"report\": {\n");
            sb.append(indent).append(indent).append("\"title\": \"").append(escapeJson(config.getTitle())).append("\",\n");
            
            if (config.isIncludeTimestamp()) {
                sb.append(indent).append(indent).append("\"generated\": \"")
                  .append(LocalDateTime.now().format(TIME_FORMATTER)).append("\",\n");
            }
            
            sb.append(indent).append(indent).append("\"count\": ").append(workItems.size()).append("\n");
            sb.append(indent).append("},\n");
        }
        
        // Add work items
        sb.append(indent).append("\"items\": [\n");
        
        for (int i = 0; i < workItems.size(); i++) {
            WorkItem item = workItems.get(i);
            
            sb.append(indent).append(indent).append("{\n");
            
            // Add ID and title
            sb.append(indent).append(indent).append(indent).append("\"id\": \"").append(escapeJson(item.getId())).append("\",\n");
            sb.append(indent).append(indent).append(indent).append("\"title\": \"").append(escapeJson(item.getTitle())).append("\"");
            
            // Add other fields
            if (shouldIncludeField("type", includedFields, excludedFields)) {
                sb.append(",\n").append(indent).append(indent).append(indent)
                  .append("\"type\": \"").append(item.getType()).append("\"");
            }
            
            if (shouldIncludeField("state", includedFields, excludedFields)) {
                sb.append(",\n").append(indent).append(indent).append(indent)
                  .append("\"state\": \"").append(item.getState()).append("\"");
            }
            
            if (shouldIncludeField("priority", includedFields, excludedFields)) {
                sb.append(",\n").append(indent).append(indent).append(indent)
                  .append("\"priority\": \"").append(item.getPriority()).append("\"");
            }
            
            if (shouldIncludeField("assignee", includedFields, excludedFields) && item.getAssignee() != null) {
                sb.append(",\n").append(indent).append(indent).append(indent)
                  .append("\"assignee\": \"").append(escapeJson(item.getAssignee())).append("\"");
            }
            
            if (shouldIncludeField("description", includedFields, excludedFields) && item.getDescription() != null) {
                sb.append(",\n").append(indent).append(indent).append(indent)
                  .append("\"description\": \"").append(escapeJson(item.getDescription())).append("\"");
            }
            
            if (shouldIncludeField("duedate", includedFields, excludedFields) && item.getDueDate() != null) {
                sb.append(",\n").append(indent).append(indent).append(indent)
                  .append("\"dueDate\": \"").append(item.getDueDate().format(DATE_FORMATTER)).append("\"");
            }
            
            if (shouldIncludeField("createdat", includedFields, excludedFields)) {
                sb.append(",\n").append(indent).append(indent).append(indent)
                  .append("\"createdAt\": \"").append(new ReportItemAdapter(item).getCreatedFormatted(TIME_FORMATTER)).append("\"");
            }
            
            if (shouldIncludeField("updatedat", includedFields, excludedFields)) {
                sb.append(",\n").append(indent).append(indent).append(indent)
                  .append("\"updatedAt\": \"").append(new ReportItemAdapter(item).getUpdatedFormatted(TIME_FORMATTER)).append("\"");
            }
            
            sb.append("\n").append(indent).append(indent).append("}");
            
            // Add comma if not the last item
            if (i < workItems.size() - 1) {
                sb.append(",");
            }
            
            sb.append("\n");
        }
        
        sb.append(indent).append("]\n");
        sb.append("}\n");
        
        return sb.toString();
    }
    
    /**
     * Escapes a string for JSON format.
     *
     * @param value the value to escape
     * @return the escaped value
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        
        return value.replace("\\", "\\\\")
                   .replace("\"", "\\\"")
                   .replace("\n", "\\n")
                   .replace("\r", "\\r")
                   .replace("\t", "\\t");
    }
    
    /**
     * Formats the report as Markdown.
     *
     * @param config the report configuration
     * @param workItems the filtered work items
     * @return the formatted Markdown report
     */
    private String formatMarkdownReport(ReportConfig config, List<WorkItem> workItems) {
        StringBuilder sb = new StringBuilder();
        List<String> includedFields = config.getIncludedFields();
        List<String> excludedFields = config.getExcludedFields();
        
        // Add header
        if (config.isIncludeHeader()) {
            sb.append("# ").append(config.getTitle()).append("\n\n");
            
            if (config.isIncludeTimestamp()) {
                sb.append("Generated: ").append(LocalDateTime.now().format(TIME_FORMATTER)).append("\n\n");
            }
        }
        
        // Check if we need to group
        if (config.isGroupByEnabled()) {
            Map<String, List<WorkItem>> groupedItems = groupWorkItems(workItems, config.getGroupByField());
            
            for (Map.Entry<String, List<WorkItem>> entry : groupedItems.entrySet()) {
                String groupName = entry.getKey();
                List<WorkItem> groupItems = entry.getValue();
                
                sb.append("## ").append(groupName).append(" (").append(groupItems.size()).append(" items)\n\n");
                
                // Add table for this group
                formatMarkdownTable(sb, groupItems, includedFields, excludedFields);
                sb.append("\n");
            }
        } else {
            // No grouping, just one table
            formatMarkdownTable(sb, workItems, includedFields, excludedFields);
        }
        
        return sb.toString();
    }
    
    /**
     * Formats a Markdown table for a list of work items.
     *
     * @param sb the string builder to append to
     * @param workItems the work items to format
     * @param includedFields the fields to include
     * @param excludedFields the fields to exclude
     */
    private void formatMarkdownTable(StringBuilder sb, List<WorkItem> workItems, 
                                     List<String> includedFields, List<String> excludedFields) {
        // Determine fields to include
        List<String> fields = new ArrayList<>();
        fields.add("ID");
        fields.add("Title");
        
        // Add other common fields if not excluded
        if (shouldIncludeField("type", includedFields, excludedFields)) fields.add("Type");
        if (shouldIncludeField("state", includedFields, excludedFields)) fields.add("State");
        if (shouldIncludeField("priority", includedFields, excludedFields)) fields.add("Priority");
        if (shouldIncludeField("assignee", includedFields, excludedFields)) fields.add("Assignee");
        if (shouldIncludeField("duedate", includedFields, excludedFields)) fields.add("Due Date");
        
        // Add header row
        sb.append("| ").append(String.join(" | ", fields)).append(" |\n");
        
        // Add separator row
        sb.append("| ").append(fields.stream().map(f -> "---").collect(Collectors.joining(" | "))).append(" |\n");
        
        // Add data rows
        for (WorkItem item : workItems) {
            List<String> values = new ArrayList<>();
            
            for (String field : fields) {
                switch (field) {
                    case "ID":
                        values.add(item.getId());
                        break;
                    case "Title":
                        values.add(item.getTitle());
                        break;
                    case "Type":
                        values.add(item.getType().toString());
                        break;
                    case "State":
                        values.add(item.getState().toString());
                        break;
                    case "Priority":
                        values.add(item.getPriority().toString());
                        break;
                    case "Assignee":
                        values.add(item.getAssignee() != null ? item.getAssignee() : "");
                        break;
                    case "Due Date":
                        values.add(item.getDueDate() != null ? 
                                item.getDueDate().format(DATE_FORMATTER) : "");
                        break;
                }
            }
            
            sb.append("| ").append(String.join(" | ", values)).append(" |\n");
        }
        
        // Add description as separate sections if included
        if (shouldIncludeField("description", includedFields, excludedFields)) {
            sb.append("\n");
            
            for (WorkItem item : workItems) {
                if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                    sb.append("### ").append(item.getId()).append(": ").append(item.getTitle()).append("\n\n");
                    sb.append(item.getDescription()).append("\n\n");
                }
            }
        }
    }
    
    /**
     * Formats the report as HTML.
     *
     * @param config the report configuration
     * @param workItems the filtered work items
     * @return the formatted HTML report
     */
    private String formatHtmlReport(ReportConfig config, List<WorkItem> workItems) {
        StringBuilder sb = new StringBuilder();
        List<String> includedFields = config.getIncludedFields();
        List<String> excludedFields = config.getExcludedFields();
        
        // Start HTML document
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html lang=\"en\">\n");
        sb.append("<head>\n");
        sb.append("  <meta charset=\"UTF-8\">\n");
        sb.append("  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        sb.append("  <title>").append(escapeHtml(config.getTitle())).append("</title>\n");
        sb.append("  <style>\n");
        sb.append("    body { font-family: Arial, sans-serif; margin: 20px; }\n");
        sb.append("    h1 { color: #333; }\n");
        sb.append("    table { border-collapse: collapse; width: 100%; margin-bottom: 20px; }\n");
        sb.append("    th, td { border: 1px solid #ddd; padding: 8px; text-align: left; }\n");
        sb.append("    th { background-color: #f2f2f2; }\n");
        sb.append("    tr:nth-child(even) { background-color: #f9f9f9; }\n");
        sb.append("    .meta { color: #666; margin-bottom: 20px; }\n");
        sb.append("    .priority-HIGH { color: #d9534f; }\n");
        sb.append("    .priority-MEDIUM { color: #f0ad4e; }\n");
        sb.append("    .priority-LOW { color: #5bc0de; }\n");
        sb.append("    .state-TODO { color: #777; }\n");
        sb.append("    .state-IN_PROGRESS { color: #337ab7; }\n");
        sb.append("    .state-DONE { color: #5cb85c; }\n");
        sb.append("  </style>\n");
        sb.append("</head>\n");
        sb.append("<body>\n");
        
        // Add header
        if (config.isIncludeHeader()) {
            sb.append("  <h1>").append(escapeHtml(config.getTitle())).append("</h1>\n");
            
            if (config.isIncludeTimestamp()) {
                sb.append("  <div class=\"meta\">Generated: ")
                  .append(LocalDateTime.now().format(TIME_FORMATTER))
                  .append("</div>\n");
            }
        }
        
        // Check if we need to group
        if (config.isGroupByEnabled()) {
            Map<String, List<WorkItem>> groupedItems = groupWorkItems(workItems, config.getGroupByField());
            
            for (Map.Entry<String, List<WorkItem>> entry : groupedItems.entrySet()) {
                String groupName = entry.getKey();
                List<WorkItem> groupItems = entry.getValue();
                
                sb.append("  <h2>").append(escapeHtml(groupName))
                  .append(" (").append(groupItems.size()).append(" items)</h2>\n");
                
                // Add table for this group
                formatHtmlTable(sb, groupItems, includedFields, excludedFields);
            }
        } else {
            // No grouping, just one table
            formatHtmlTable(sb, workItems, includedFields, excludedFields);
        }
        
        // End HTML document
        sb.append("</body>\n");
        sb.append("</html>\n");
        
        return sb.toString();
    }
    
    /**
     * Formats an HTML table for a list of work items.
     *
     * @param sb the string builder to append to
     * @param workItems the work items to format
     * @param includedFields the fields to include
     * @param excludedFields the fields to exclude
     */
    private void formatHtmlTable(StringBuilder sb, List<WorkItem> workItems, 
                                List<String> includedFields, List<String> excludedFields) {
        // Determine fields to include
        List<String> fields = new ArrayList<>();
        fields.add("ID");
        fields.add("Title");
        
        // Add other common fields if not excluded
        if (shouldIncludeField("type", includedFields, excludedFields)) fields.add("Type");
        if (shouldIncludeField("state", includedFields, excludedFields)) fields.add("State");
        if (shouldIncludeField("priority", includedFields, excludedFields)) fields.add("Priority");
        if (shouldIncludeField("assignee", includedFields, excludedFields)) fields.add("Assignee");
        if (shouldIncludeField("description", includedFields, excludedFields)) fields.add("Description");
        if (shouldIncludeField("duedate", includedFields, excludedFields)) fields.add("Due Date");
        if (shouldIncludeField("createdat", includedFields, excludedFields)) fields.add("Created");
        if (shouldIncludeField("updatedat", includedFields, excludedFields)) fields.add("Updated");
        
        // Start table
        sb.append("  <table>\n");
        
        // Add header row
        sb.append("    <tr>\n");
        for (String field : fields) {
            sb.append("      <th>").append(field).append("</th>\n");
        }
        sb.append("    </tr>\n");
        
        // Add data rows
        for (WorkItem item : workItems) {
            sb.append("    <tr>\n");
            
            for (String field : fields) {
                switch (field) {
                    case "ID":
                        sb.append("      <td>").append(item.getId()).append("</td>\n");
                        break;
                    case "Title":
                        sb.append("      <td>").append(escapeHtml(item.getTitle())).append("</td>\n");
                        break;
                    case "Type":
                        sb.append("      <td>").append(item.getType()).append("</td>\n");
                        break;
                    case "State":
                        sb.append("      <td class=\"state-").append(item.getState()).append("\">")
                          .append(item.getState()).append("</td>\n");
                        break;
                    case "Priority":
                        sb.append("      <td class=\"priority-").append(item.getPriority()).append("\">")
                          .append(item.getPriority()).append("</td>\n");
                        break;
                    case "Assignee":
                        sb.append("      <td>").append(item.getAssignee() != null ? 
                                escapeHtml(item.getAssignee()) : "").append("</td>\n");
                        break;
                    case "Description":
                        sb.append("      <td>").append(item.getDescription() != null ? 
                                escapeHtml(item.getDescription()) : "").append("</td>\n");
                        break;
                    case "Due Date":
                        sb.append("      <td>").append(item.getDueDate() != null ? 
                                item.getDueDate().format(DATE_FORMATTER) : "").append("</td>\n");
                        break;
                    case "Created":
                        sb.append("      <td>").append(new ReportItemAdapter(item).getCreatedFormatted(TIME_FORMATTER))
                          .append("</td>\n");
                        break;
                    case "Updated":
                        sb.append("      <td>").append(new ReportItemAdapter(item).getUpdatedFormatted(TIME_FORMATTER))
                          .append("</td>\n");
                        break;
                }
            }
            
            sb.append("    </tr>\n");
        }
        
        // End table
        sb.append("  </table>\n");
    }
    
    /**
     * Escapes a string for HTML format.
     *
     * @param value the value to escape
     * @return the escaped value
     */
    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        
        return value.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&#039;");
    }
    
    /**
     * Formats the report as XML.
     *
     * @param config the report configuration
     * @param workItems the filtered work items
     * @return the formatted XML report
     */
    private String formatXmlReport(ReportConfig config, List<WorkItem> workItems) {
        StringBuilder sb = new StringBuilder();
        List<String> includedFields = config.getIncludedFields();
        List<String> excludedFields = config.getExcludedFields();
        String indent = config.isPrettyPrint() ? "  " : "";
        
        // XML declaration
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        
        // Root element
        sb.append("<report>\n");
        
        // Add metadata
        if (config.isIncludeHeader()) {
            sb.append(indent).append("<metadata>\n");
            sb.append(indent).append(indent).append("<title>").append(escapeXml(config.getTitle())).append("</title>\n");
            
            if (config.isIncludeTimestamp()) {
                sb.append(indent).append(indent).append("<generated>")
                  .append(LocalDateTime.now().format(TIME_FORMATTER)).append("</generated>\n");
            }
            
            sb.append(indent).append(indent).append("<count>").append(workItems.size()).append("</count>\n");
            sb.append(indent).append("</metadata>\n");
        }
        
        // Check if we need to group
        if (config.isGroupByEnabled()) {
            Map<String, List<WorkItem>> groupedItems = groupWorkItems(workItems, config.getGroupByField());
            
            for (Map.Entry<String, List<WorkItem>> entry : groupedItems.entrySet()) {
                String groupName = entry.getKey();
                List<WorkItem> groupItems = entry.getValue();
                
                sb.append(indent).append("<group name=\"").append(escapeXml(groupName))
                  .append("\" count=\"").append(groupItems.size()).append("\">\n");
                
                // Add items in this group
                for (WorkItem item : groupItems) {
                    formatXmlWorkItem(sb, item, includedFields, excludedFields, indent + indent);
                }
                
                sb.append(indent).append("</group>\n");
            }
        } else {
            // No grouping, just list items
            sb.append(indent).append("<items>\n");
            
            for (WorkItem item : workItems) {
                formatXmlWorkItem(sb, item, includedFields, excludedFields, indent + indent);
            }
            
            sb.append(indent).append("</items>\n");
        }
        
        // Close root element
        sb.append("</report>\n");
        
        return sb.toString();
    }
    
    /**
     * Formats a work item as XML.
     *
     * @param sb the string builder to append to
     * @param item the work item to format
     * @param includedFields the fields to include
     * @param excludedFields the fields to exclude
     * @param indent the indentation to use
     */
    private void formatXmlWorkItem(StringBuilder sb, WorkItem item, 
                                  List<String> includedFields, List<String> excludedFields, String indent) {
        sb.append(indent).append("<item id=\"").append(item.getId()).append("\">\n");
        
        // Add title
        sb.append(indent).append("  <title>").append(escapeXml(item.getTitle())).append("</title>\n");
        
        // Add other fields
        if (shouldIncludeField("type", includedFields, excludedFields)) {
            sb.append(indent).append("  <type>").append(item.getType()).append("</type>\n");
        }
        
        if (shouldIncludeField("state", includedFields, excludedFields)) {
            sb.append(indent).append("  <state>").append(item.getState()).append("</state>\n");
        }
        
        if (shouldIncludeField("priority", includedFields, excludedFields)) {
            sb.append(indent).append("  <priority>").append(item.getPriority()).append("</priority>\n");
        }
        
        if (shouldIncludeField("assignee", includedFields, excludedFields) && item.getAssignee() != null) {
            sb.append(indent).append("  <assignee>").append(escapeXml(item.getAssignee())).append("</assignee>\n");
        }
        
        if (shouldIncludeField("description", includedFields, excludedFields) && item.getDescription() != null) {
            sb.append(indent).append("  <description>").append(escapeXml(item.getDescription())).append("</description>\n");
        }
        
        if (shouldIncludeField("duedate", includedFields, excludedFields) && item.getDueDate() != null) {
            sb.append(indent).append("  <dueDate>").append(item.getDueDate().format(DATE_FORMATTER)).append("</dueDate>\n");
        }
        
        if (shouldIncludeField("createdat", includedFields, excludedFields)) {
            sb.append(indent).append("  <createdAt>").append(new ReportItemAdapter(item).getCreatedFormatted(TIME_FORMATTER)).append("</createdAt>\n");
        }
        
        if (shouldIncludeField("updatedat", includedFields, excludedFields)) {
            sb.append(indent).append("  <updatedAt>").append(new ReportItemAdapter(item).getUpdatedFormatted(TIME_FORMATTER)).append("</updatedAt>\n");
        }
        
        sb.append(indent).append("</item>\n");
    }
    
    /**
     * Escapes a string for XML format.
     *
     * @param value the value to escape
     * @return the escaped value
     */
    private String escapeXml(String value) {
        if (value == null) {
            return "";
        }
        
        return value.replace("&", "&amp;")
                   .replace("<", "&lt;")
                   .replace(">", "&gt;")
                   .replace("\"", "&quot;")
                   .replace("'", "&apos;");
    }
    
    /**
     * Checks if a field should be included in the report.
     *
     * @param field the field to check
     * @param includedFields the list of fields to include
     * @param excludedFields the list of fields to exclude
     * @return true if the field should be included
     */
    private boolean shouldIncludeField(String field, List<String> includedFields, List<String> excludedFields) {
        // If included fields is empty, include all fields except excluded
        if (includedFields.isEmpty()) {
            return !excludedFields.contains(field);
        }
        
        // If included fields is not empty, only include fields in the list
        return includedFields.contains(field);
    }
    
    /**
     * Groups work items by the specified field.
     *
     * @param workItems the work items to group
     * @param groupByField the field to group by
     * @return the grouped work items
     */
    private Map<String, List<WorkItem>> groupWorkItems(List<WorkItem> workItems, String groupByField) {
        Map<String, List<WorkItem>> result = new HashMap<>();
        
        for (WorkItem item : workItems) {
            String groupValue = getFieldValue(item, groupByField);
            
            if (!result.containsKey(groupValue)) {
                result.put(groupValue, new ArrayList<>());
            }
            
            result.get(groupValue).add(item);
        }
        
        return result;
    }
    
    /**
     * Gets the value of a field from a work item.
     *
     * @param item the work item
     * @param field the field name
     * @return the field value as a string
     */
    private String getFieldValue(WorkItem item, String field) {
        if (field == null) {
            return "Ungrouped";
        }
        
        switch (field.toLowerCase()) {
            case "id":
                return item.getId();
            case "title":
                return item.getTitle();
            case "type":
                return item.getType().toString();
            case "state":
                return item.getState().toString();
            case "priority":
                return item.getPriority().toString();
            case "assignee":
                return item.getAssignee() != null ? item.getAssignee() : "Unassigned";
            default:
                return "Unknown";
        }
    }
    
    /**
     * Outputs the report to the specified destination.
     *
     * @param config the report configuration
     * @param content the report content
     * @return true if the report was output successfully
     */
    private boolean outputReport(ReportConfig config, String content) {
        // If output path is null, write to console
        if (config.getOutputPath() == null || config.getOutputPath().isEmpty()) {
            System.out.println(content);
            return true;
        }
        
        // Write to file
        try {
            // Ensure we're working with an absolute path
            String outputPath = config.getOutputPath();
            Path outputPathObj = Paths.get(outputPath);
            
            // If not an absolute path, make it absolute
            if (!outputPathObj.isAbsolute()) {
                String projectRoot = System.getProperty("user.dir");
                outputPathObj = Paths.get(projectRoot, outputPath);
                outputPath = outputPathObj.toString();
            }
            
            // Create parent directories if needed
            Files.createDirectories(outputPathObj.getParent());
            
            // Write to file
            try (PrintWriter writer = new PrintWriter(new FileWriter(outputPath))) {
                writer.print(content);
            }
            
            return true;
        } catch (IOException e) {
            System.err.println("Error writing report to file: " + e.getMessage());
            return false;
        }
    }
}