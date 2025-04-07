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

import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MockItemService;

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
    
    @Override
    public Integer call() {
        try {
            ServiceManager serviceManager = ServiceManager.getInstance();
            MockItemService itemService = (MockItemService) serviceManager.getItemService();
            
            // Get all work items
            List<WorkItem> allWorkItems = itemService.getAllWorkItems();
            
            // Apply filters
            List<WorkItem> filteredItems = filterWorkItems(allWorkItems);
            
            // Display results
            displayResults(filteredItems);
            
            return 0;
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
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
     * Displays the search results.
     *
     * @param workItems the work items to display
     */
    private void displayResults(List<WorkItem> workItems) {
        // Update the last viewed item to the first result (if any)
        if (!workItems.isEmpty()) {
            // Converting UUID to String if needed
            ContextManager.getInstance().setLastViewedWorkItem(workItems.get(0).getId().toString());
        }
        
        // Count only mode
        if (countOnly) {
            System.out.println("Found " + workItems.size() + " work items");
            return;
        }
        
        // No results
        if (workItems.isEmpty()) {
            System.out.println("No matching work items found");
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