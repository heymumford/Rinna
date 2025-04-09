/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.command;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.SearchService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command to list work items with filtering capabilities.
 * This command searches for and displays work items based on specified criteria.
 * Format options include text (default) and JSON.
 * 
 * Usage examples:
 * - rin list
 * - rin list --type=TASK
 * - rin list --priority=HIGH
 * - rin list --state=IN_PROGRESS
 * - rin list --project=ProjectName
 * - rin list --assignee=username
 * - rin list --format=json
 * - rin list --limit=50
 * - rin list --sort-by=priority
 * - rin list --descending
 * - rin list --verbose
 */
public class ListCommand implements Callable<Integer> {
    
    private WorkItemType type;
    private Priority priority;
    private int limit = 100;
    private String project;
    private String assignee;
    private WorkflowState state;
    private String sortBy;
    private boolean descending = false;
    private String format = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final MetadataService metadataService;
    
    /**
     * Creates a new ListCommand with default services.
     */
    public ListCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new ListCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public ListCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.metadataService = serviceManager.getMetadataService();
    }
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("type", type != null ? type.name() : null);
        params.put("priority", priority != null ? priority.name() : null);
        params.put("state", state != null ? state.name() : null);
        params.put("project", project);
        params.put("assignee", assignee);
        params.put("limit", limit);
        params.put("sort_by", sortBy);
        params.put("descending", descending);
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("list", "READ", params);
        
        try {
            // Get the search service from the service manager
            SearchService searchService = serviceManager.getMockSearchService();
            
            // Build search criteria
            Map<String, String> criteria = new HashMap<>();
            
            if (type != null) {
                criteria.put("type", type.name());
            }
            
            if (priority != null) {
                criteria.put("priority", priority.name());
            }
            
            if (state != null) {
                criteria.put("state", state.name());
            }
            
            if (project != null && !project.isEmpty()) {
                criteria.put("project", project);
            }
            
            if (assignee != null && !assignee.isEmpty()) {
                criteria.put("assignee", assignee);
            }
            
            // Perform the search
            List<WorkItem> items = searchService.findWorkItems(criteria, limit);
            
            // Sort the items if needed
            if (sortBy != null && !sortBy.isEmpty()) {
                items = sortItems(items, sortBy, descending);
            }
            
            // No items found
            if (items.isEmpty()) {
                System.out.println("No work items found matching the criteria.");
                
                // Record the successful operation with zero results
                Map<String, Object> result = new HashMap<>();
                result.put("count", 0);
                metadataService.completeOperation(operationId, result);
                
                return 0;
            }
            
            // Output based on format
            if ("json".equalsIgnoreCase(format)) {
                outputJsonResults(items);
            } else {
                outputTextResults(items);
            }
            
            // Record the successful operation
            Map<String, Object> result = new HashMap<>();
            result.put("count", items.size());
            result.put("displayed", Math.min(items.size(), limit));
            result.put("criteria", criteria);
            
            metadataService.completeOperation(operationId, result);
            return 0;
            
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error listing work items: " + e.getMessage();
            System.err.println(errorMessage);
            
            // Record detailed error information if verbose mode is enabled
            if (verbose) {
                e.printStackTrace();
            }
            
            // Record the failed operation with error details
            metadataService.failOperation(operationId, e);
            
            return 1;
        }
    }
    
    /**
     * Outputs work items in JSON format.
     * 
     * @param items the work items to output
     */
    private void outputJsonResults(List<WorkItem> items) {
        Map<String, Object> result = new HashMap<>();
        result.put("count", items.size());
        
        // Convert WorkItem objects to simple Maps to avoid serialization issues
        List<Map<String, Object>> itemMaps = new ArrayList<>();
        for (WorkItem item : items) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", item.getId());
            itemMap.put("title", item.getTitle());
            itemMap.put("type", item.getType() != null ? item.getType().name() : null);
            itemMap.put("priority", item.getPriority() != null ? item.getPriority().name() : null);
            itemMap.put("state", item.getState() != null ? item.getState().name() : null);
            itemMap.put("assignee", item.getAssignee());
            itemMaps.add(itemMap);
        }
        result.put("items", itemMaps);
        
        // Use the OutputFormatter for consistent JSON output
        String json = OutputFormatter.toJson(result, verbose);
        System.out.println(json);
    }
    
    /**
     * Outputs work items in text format.
     * 
     * @param items the work items to output
     */
    private void outputTextResults(List<WorkItem> items) {
        System.out.println("Work Items:");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-20s %-40s %-10s %-10s %-12s %-10s%n", 
                "ID", "TITLE", "TYPE", "PRIORITY", "STATUS", "ASSIGNEE");
        System.out.println("--------------------------------------------------------------------------------");
        
        int totalItems = items.size();
        int displayedItems = Math.min(totalItems, limit);
        
        for (int i = 0; i < displayedItems; i++) {
            WorkItem item = items.get(i);
            String title = item.getTitle();
            if (title == null) {
                title = "(No title)";
            } else if (title.length() > 38) {
                title = title.substring(0, 35) + "...";
            }
            
            System.out.printf("%-20s %-40s %-10s %-10s %-12s %-10s%n", 
                    item.getId(),
                    title,
                    item.getType() != null ? item.getType() : "-",
                    item.getPriority() != null ? item.getPriority() : "-",
                    item.getState() != null ? item.getState() : "-",
                    item.getAssignee() != null ? item.getAssignee() : "-");
            
            // Show additional details in verbose mode
            if (verbose) {
                if (item.getDescription() != null && !item.getDescription().isEmpty()) {
                    String desc = item.getDescription();
                    if (desc.length() > 77) {
                        desc = desc.substring(0, 74) + "...";
                    }
                    System.out.printf("          %s%n", desc);
                }
                
                StringBuilder details = new StringBuilder();
                
                if (item.getCreated() != null) {
                    details.append("Created: ").append(item.getCreated());
                }
                
                if (item.getDueDate() != null) {
                    if (details.length() > 0) details.append(" | ");
                    details.append("Due: ").append(item.getDueDate());
                }
                
                if (item.getProject() != null && !item.getProject().isEmpty()) {
                    if (details.length() > 0) details.append(" | ");
                    details.append("Project: ").append(item.getProject());
                }
                
                if (details.length() > 0) {
                    System.out.printf("          %s%n", details.toString());
                    System.out.println();
                }
            }
        }
        
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("Displaying %d of %d item(s)%n", displayedItems, totalItems);
        
        if (totalItems > displayedItems) {
            System.out.printf("(Use --limit=%d to see more items)%n", totalItems);
        }
    }
    
    /**
     * Sorts the list of work items by the specified field.
     *
     * @param items the list of work items
     * @param field the field to sort by
     * @param descending whether to sort in descending order
     * @return the sorted list
     */
    private List<WorkItem> sortItems(List<WorkItem> items, String field, boolean descending) {
        if (items == null || items.isEmpty()) {
            return items;
        }
        
        // Create a new list to avoid modifying the original
        List<WorkItem> sortedItems = new ArrayList<>(items);
        
        // Sort the list based on the specified field
        sortedItems.sort((item1, item2) -> {
            int result = 0;
            
            switch (field.toLowerCase()) {
                case "id":
                    result = compareStrings(item1.getId(), item2.getId());
                    break;
                case "title":
                    result = compareStrings(item1.getTitle(), item2.getTitle());
                    break;
                case "type":
                    result = compareEnums(item1.getType(), item2.getType());
                    break;
                case "priority":
                    result = compareEnums(item1.getPriority(), item2.getPriority());
                    break;
                case "status":
                case "state":
                    result = compareEnums(item1.getState(), item2.getState());
                    break;
                case "assignee":
                    result = compareStrings(item1.getAssignee(), item2.getAssignee());
                    break;
                case "created":
                    result = compareDates(item1.getCreated(), item2.getCreated());
                    break;
                case "updated":
                    result = compareDates(item1.getUpdated(), item2.getUpdated());
                    break;
                default:
                    // Default to sort by ID
                    result = compareStrings(item1.getId(), item2.getId());
            }
            
            // Reverse the result if descending order is requested
            return descending ? -result : result;
        });
        
        return sortedItems;
    }
    
    /**
     * Compares two strings for sorting, handling null values.
     *
     * @param s1 the first string
     * @param s2 the second string
     * @return comparison result
     */
    private int compareStrings(String s1, String s2) {
        if (s1 == null && s2 == null) return 0;
        if (s1 == null) return -1;
        if (s2 == null) return 1;
        return s1.compareTo(s2);
    }
    
    /**
     * Compares two enum values for sorting, handling null values.
     *
     * @param e1 the first enum
     * @param e2 the second enum
     * @return comparison result
     */
    private <T extends Enum<T>> int compareEnums(T e1, T e2) {
        if (e1 == null && e2 == null) return 0;
        if (e1 == null) return -1;
        if (e2 == null) return 1;
        return e1.name().compareTo(e2.name());
    }
    
    /**
     * Compares two dates for sorting, handling null values.
     *
     * @param d1 the first date
     * @param d2 the second date
     * @return comparison result
     */
    private int compareDates(java.time.LocalDateTime d1, java.time.LocalDateTime d2) {
        if (d1 == null && d2 == null) return 0;
        if (d1 == null) return -1;
        if (d2 == null) return 1;
        return d1.compareTo(d2);
    }
    
    /**
     * Gets the work item type filter.
     *
     * @return the work item type
     */
    public WorkItemType getType() {
        return type;
    }
    
    /**
     * Sets the work item type filter.
     *
     * @param type the work item type
     */
    public void setType(WorkItemType type) {
        this.type = type;
    }
    
    /**
     * Gets the priority filter.
     *
     * @return the priority
     */
    public Priority getPriority() {
        return priority;
    }
    
    /**
     * Sets the priority filter.
     *
     * @param priority the priority
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }
    
    /**
     * Gets the maximum number of items to display.
     *
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }
    
    /**
     * Sets the maximum number of items to display.
     *
     * @param limit the limit
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    /**
     * Gets the project filter.
     *
     * @return the project
     */
    public String getProject() {
        return project;
    }
    
    /**
     * Sets the project filter.
     *
     * @param project the project
     */
    public void setProject(String project) {
        this.project = project;
    }
    
    /**
     * Gets the assignee filter.
     *
     * @return the assignee
     */
    public String getAssignee() {
        return assignee;
    }
    
    /**
     * Sets the assignee filter.
     *
     * @param assignee the assignee
     */
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
    
    /**
     * Gets the workflow state filter.
     *
     * @return the workflow state
     */
    public WorkflowState getState() {
        return state;
    }
    
    /**
     * Sets the workflow state filter.
     *
     * @param state the workflow state
     */
    public void setState(WorkflowState state) {
        this.state = state;
    }
    
    /**
     * Gets the field to sort by.
     *
     * @return the sort field
     */
    public String getSortBy() {
        return sortBy;
    }
    
    /**
     * Sets the field to sort by.
     *
     * @param sortBy the sort field
     */
    public void setSortBy(String sortBy) {
        this.sortBy = sortBy;
    }
    
    /**
     * Gets whether to sort in descending order.
     *
     * @return true if descending order is enabled
     */
    public boolean isDescending() {
        return descending;
    }
    
    /**
     * Sets whether to sort in descending order.
     *
     * @param descending true to enable descending order
     */
    public void setDescending(boolean descending) {
        this.descending = descending;
    }
    
    /**
     * Gets the output format.
     *
     * @return the output format
     */
    public String getFormat() {
        return format;
    }
    
    /**
     * Sets the output format (text or json).
     *
     * @param format the output format
     */
    public void setFormat(String format) {
        this.format = format;
    }
    
    /**
     * Gets whether verbose output is enabled.
     *
     * @return true if verbose output is enabled
     */
    public boolean isVerbose() {
        return verbose;
    }
    
    /**
     * Sets whether verbose output is enabled.
     *
     * @param verbose true to enable verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
}