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

import org.rinna.cli.service.ItemService;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.function.Predicate;

/**
 * Command to find work items based on various criteria, similar to the Unix find command.
 * - "rin find -name pattern" - Find work items with title matching pattern
 * - "rin find -type TASK" - Find work items of type TASK
 * - "rin find -state IN_PROGRESS" - Find work items in IN_PROGRESS state
 * - "rin find -priority HIGH" - Find work items with HIGH priority
 * - "rin find -assignee username" - Find work items assigned to username
 * - "rin find -reporter username" - Find work items reported by username
 * - "rin find -newer 2023-05-20" - Find work items created after date
 * - "rin find -mtime -7" - Find work items modified in the last 7 days
 * - "rin find --format=json" - Output results in JSON format
 */
public class FindCommand implements Callable<Integer> {
    
    private String namePattern;
    private WorkItemType type;
    private WorkflowState state;
    private Priority priority;
    private String assignee;
    private String reporter;
    private Instant createdAfter;
    private Instant createdBefore;
    private Instant updatedAfter;
    private Instant updatedBefore;
    private boolean printDetails = false;
    private boolean countOnly = false;
    private String format = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final ContextManager contextManager;
    private final MetadataService metadataService;
    
    /**
     * Creates a new FindCommand with default services.
     */
    public FindCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new FindCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public FindCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.contextManager = ContextManager.getInstance();
        this.metadataService = serviceManager.getMetadataService();
    }
    
    /**
     * Sets the name pattern to search for.
     *
     * @param namePattern the name pattern
     */
    public void setNamePattern(String namePattern) {
        this.namePattern = namePattern;
    }
    
    /**
     * Sets the type to search for.
     *
     * @param type the type
     */
    public void setType(WorkItemType type) {
        this.type = type;
    }
    
    /**
     * Sets the state to search for.
     *
     * @param state the state
     */
    public void setState(WorkflowState state) {
        this.state = state;
    }
    
    /**
     * Sets the priority to search for.
     *
     * @param priority the priority
     */
    public void setPriority(Priority priority) {
        this.priority = priority;
    }
    
    /**
     * Sets the assignee to search for.
     *
     * @param assignee the assignee
     */
    public void setAssignee(String assignee) {
        this.assignee = assignee;
    }
    
    /**
     * Sets the reporter to search for.
     *
     * @param reporter the reporter
     */
    public void setReporter(String reporter) {
        this.reporter = reporter;
    }
    
    /**
     * Sets the created after date.
     *
     * @param createdAfter the created after date
     */
    public void setCreatedAfter(Instant createdAfter) {
        this.createdAfter = createdAfter;
    }
    
    /**
     * Sets the created before date.
     *
     * @param createdBefore the created before date
     */
    public void setCreatedBefore(Instant createdBefore) {
        this.createdBefore = createdBefore;
    }
    
    /**
     * Sets the updated after date.
     *
     * @param updatedAfter the updated after date
     */
    public void setUpdatedAfter(Instant updatedAfter) {
        this.updatedAfter = updatedAfter;
    }
    
    /**
     * Sets the updated before date.
     *
     * @param updatedBefore the updated before date
     */
    public void setUpdatedBefore(Instant updatedBefore) {
        this.updatedBefore = updatedBefore;
    }
    
    /**
     * Sets whether to print details.
     *
     * @param printDetails true to print details
     */
    public void setPrintDetails(boolean printDetails) {
        this.printDetails = printDetails;
    }
    
    /**
     * Sets whether to only count results.
     *
     * @param countOnly true to only count results
     */
    public void setCountOnly(boolean countOnly) {
        this.countOnly = countOnly;
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
     * Sets whether verbose output is enabled.
     *
     * @param verbose true to enable verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        if (namePattern != null) params.put("name_pattern", namePattern);
        if (type != null) params.put("type", type.toString());
        if (state != null) params.put("state", state.toString());
        if (priority != null) params.put("priority", priority.toString());
        if (assignee != null) params.put("assignee", assignee);
        if (reporter != null) params.put("reporter", reporter);
        if (createdAfter != null) params.put("created_after", createdAfter.toString());
        if (createdBefore != null) params.put("created_before", createdBefore.toString());
        if (updatedAfter != null) params.put("updated_after", updatedAfter.toString());
        if (updatedBefore != null) params.put("updated_before", updatedBefore.toString());
        params.put("print_details", printDetails);
        params.put("count_only", countOnly);
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("find", "SEARCH", params);
        
        try {
            // Get service via service interface
            ItemService itemService = serviceManager.getItemService();
            
            // Get all work items
            List<WorkItem> allWorkItems = itemService.getAllItems();
            
            // Apply filters
            List<WorkItem> filteredItems = filterWorkItems(allWorkItems);
            
            // Display results
            if ("json".equalsIgnoreCase(format)) {
                displayResultsJson(filteredItems, operationId);
            } else {
                displayResults(filteredItems, operationId);
            }
            
            return 0;
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error executing find command: " + e.getMessage();
            System.err.println("Error: " + errorMessage);
            
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
     * Filters the work items based on the specified criteria.
     *
     * @param workItems the work items to filter
     * @return the filtered work items
     */
    private List<WorkItem> filterWorkItems(List<WorkItem> workItems) {
        List<WorkItem> filteredItems = new ArrayList<>();
        
        for (WorkItem item : workItems) {
            if (matchesAll(item)) {
                filteredItems.add(item);
            }
        }
        
        return filteredItems;
    }
    
    /**
     * Checks if a work item matches all specified criteria.
     *
     * @param item the work item to check
     * @return true if the work item matches all criteria
     */
    private boolean matchesAll(WorkItem item) {
        // Check name pattern
        if (namePattern != null && !item.getTitle().toLowerCase().contains(namePattern.toLowerCase())) {
            return false;
        }
        
        // Check type
        if (type != null && !item.getType().equals(type)) {
            return false;
        }
        
        // Check state
        if (state != null && !item.getStatus().equals(state)) {
            return false;
        }
        
        // Check priority
        if (priority != null && !item.getPriority().equals(priority)) {
            return false;
        }
        
        // Check assignee
        if (assignee != null && !item.getAssignee().equalsIgnoreCase(assignee)) {
            return false;
        }
        
        // Check reporter
        if (reporter != null) {
            // For now, assume reporter info might not be available
            return false;
        }
        
        // Check created after
        if (createdAfter != null && !item.getCreated().atZone(ZoneId.systemDefault()).toInstant().isAfter(createdAfter)) {
            return false;
        }
        
        // Check created before
        if (createdBefore != null && !item.getCreated().atZone(ZoneId.systemDefault()).toInstant().isBefore(createdBefore)) {
            return false;
        }
        
        // Check updated after
        if (updatedAfter != null && !item.getUpdated().atZone(ZoneId.systemDefault()).toInstant().isAfter(updatedAfter)) {
            return false;
        }
        
        // Check updated before
        if (updatedBefore != null && !item.getUpdated().atZone(ZoneId.systemDefault()).toInstant().isBefore(updatedBefore)) {
            return false;
        }
        
        // All criteria met
        return true;
    }
    
    /**
     * Displays the search results in text format.
     *
     * @param workItems the work items to display
     * @param operationId the operation tracking ID
     */
    private void displayResults(List<WorkItem> workItems, String operationId) {
        // Update the last viewed item to the first result (if any)
        if (!workItems.isEmpty()) {
            UUID firstItemId = parseItemId(workItems.get(0).getId());
            if (firstItemId != null) {
                contextManager.setLastViewedWorkItem(firstItemId);
            }
        }
        
        // Count only mode
        if (countOnly) {
            System.out.println("Found " + workItems.size() + " work items");
            
            // Record the successful operation
            Map<String, Object> result = new HashMap<>();
            result.put("count", workItems.size());
            result.put("format", "text");
            result.put("count_only", true);
            metadataService.completeOperation(operationId, result);
            
            return;
        }
        
        // No results
        if (workItems.isEmpty()) {
            System.out.println("No matching work items found");
            
            // Record the successful operation
            Map<String, Object> result = new HashMap<>();
            result.put("count", 0);
            result.put("format", "text");
            metadataService.completeOperation(operationId, result);
            
            return;
        }
        
        // Display results
        System.out.println("Found " + workItems.size() + " work items:");
        
        if (printDetails) {
            // Detailed display
            for (WorkItem item : workItems) {
                System.out.println();
                System.out.println("ID: " + item.getId());
                System.out.println("Title: " + item.getTitle());
                System.out.println("Type: " + item.getType());
                System.out.println("Priority: " + item.getPriority());
                System.out.println("State: " + item.getStatus());
                System.out.println("Assignee: " + item.getAssignee());
                // System.out.println("Reporter: " + item.getReporter()); // Reporter not available in WorkItem
                System.out.println("Created: " + formatDate(item.getCreated().atZone(ZoneId.systemDefault()).toInstant()));
                System.out.println("Updated: " + formatDate(item.getUpdated().atZone(ZoneId.systemDefault()).toInstant()));
            }
        } else {
            // Summary display
            for (WorkItem item : workItems) {
                System.out.printf("%-36s | %-30s | %-12s | %-8s | %s%n",
                        item.getId(),
                        truncate(item.getTitle(), 30),
                        item.getStatus(),
                        item.getPriority(),
                        item.getAssignee());
            }
        }
        
        // Record the successful operation
        Map<String, Object> result = new HashMap<>();
        result.put("count", workItems.size());
        result.put("format", "text");
        result.put("detailed", printDetails);
        if (!workItems.isEmpty()) {
            result.put("first_item_id", workItems.get(0).getId());
        }
        metadataService.completeOperation(operationId, result);
    }
    
    /**
     * Displays the search results in JSON format.
     *
     * @param workItems the work items to display
     * @param operationId the operation tracking ID
     */
    private void displayResultsJson(List<WorkItem> workItems, String operationId) {
        // Update the last viewed item to the first result (if any)
        if (!workItems.isEmpty()) {
            UUID firstItemId = parseItemId(workItems.get(0).getId());
            if (firstItemId != null) {
                contextManager.setLastViewedWorkItem(firstItemId);
            }
        }
        
        // Create JSON data
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("count", workItems.size());
        
        // Add search criteria
        Map<String, Object> criteria = new HashMap<>();
        if (namePattern != null) criteria.put("name_pattern", namePattern);
        if (type != null) criteria.put("type", type.toString());
        if (state != null) criteria.put("state", state.toString());
        if (priority != null) criteria.put("priority", priority.toString());
        if (assignee != null) criteria.put("assignee", assignee);
        if (reporter != null) criteria.put("reporter", reporter);
        if (createdAfter != null) criteria.put("created_after", createdAfter.toString());
        if (createdBefore != null) criteria.put("created_before", createdBefore.toString());
        if (updatedAfter != null) criteria.put("updated_after", updatedAfter.toString());
        if (updatedBefore != null) criteria.put("updated_before", updatedBefore.toString());
        jsonData.put("criteria", criteria);
        
        // Add work items
        List<Map<String, Object>> items = new ArrayList<>();
        for (WorkItem item : workItems) {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", item.getId());
            itemMap.put("title", item.getTitle());
            itemMap.put("type", item.getType() != null ? item.getType().toString() : null);
            itemMap.put("priority", item.getPriority() != null ? item.getPriority().toString() : null);
            itemMap.put("state", item.getStatus() != null ? item.getStatus().toString() : null);
            itemMap.put("assignee", item.getAssignee());
            
            // Add detailed information if requested
            if (printDetails) {
                itemMap.put("description", item.getDescription());
                itemMap.put("created", item.getCreated() != null ? item.getCreated().toString() : null);
                itemMap.put("updated", item.getUpdated() != null ? item.getUpdated().toString() : null);
                // Add any other available fields
                itemMap.put("project", item.getProject());
            }
            
            items.add(itemMap);
        }
        jsonData.put("items", items);
        
        // Display as JSON
        String json = OutputFormatter.toJson(jsonData, verbose);
        System.out.println(json);
        
        // Record the successful operation
        Map<String, Object> result = new HashMap<>();
        result.put("count", workItems.size());
        result.put("format", "json");
        result.put("detailed", printDetails);
        if (!workItems.isEmpty()) {
            result.put("first_item_id", workItems.get(0).getId());
        }
        metadataService.completeOperation(operationId, result);
    }
    
    /**
     * Safely parses a WorkItem ID to a UUID.
     *
     * @param id the ID to parse, which could be a string or UUID
     * @return the parsed UUID, or null if parsing fails
     */
    private UUID parseItemId(Object id) {
        try {
            if (id instanceof UUID) {
                return (UUID) id;
            } else if (id instanceof String) {
                return UUID.fromString((String) id);
            }
        } catch (Exception e) {
            // Ignore parsing errors
        }
        return null;
    }
    
    /**
     * Formats a date for display.
     *
     * @param date the date to format
     * @return the formatted date string
     */
    private String formatDate(Instant date) {
        return date.toString();
    }
    
    /**
     * Truncates a string to the specified length.
     *
     * @param str the string to truncate
     * @param maxLength the maximum length
     * @return the truncated string
     */
    private String truncate(String str, int maxLength) {
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Parses a date string to an Instant.
     *
     * @param dateStr the date string
     * @return the parsed Instant, or null if parsing fails
     */
    public static Instant parseDate(String dateStr) {
        try {
            // Try ISO date format
            LocalDate date = LocalDate.parse(dateStr);
            return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
        } catch (DateTimeParseException e1) {
            try {
                // Try ISO date-time format
                LocalDateTime dateTime = LocalDateTime.parse(dateStr);
                return dateTime.atZone(ZoneId.systemDefault()).toInstant();
            } catch (DateTimeParseException e2) {
                try {
                    // Try simple date format (yyyy-MM-dd)
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
                    LocalDate date = LocalDate.parse(dateStr, formatter);
                    return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
                } catch (DateTimeParseException e3) {
                    // Failed to parse date
                    return null;
                }
            }
        }
    }
    
    /**
     * Calculates an Instant from a relative time specification.
     *
     * @param days the number of days relative to now (negative for past, positive for future)
     * @return the calculated Instant
     */
    public static Instant daysFromNow(int days) {
        LocalDate date = LocalDate.now().plusDays(days);
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant();
    }
}