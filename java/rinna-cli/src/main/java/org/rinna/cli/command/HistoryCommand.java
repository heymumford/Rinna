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

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockCommentService;
import org.rinna.cli.service.MockHistoryService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Command to display the history of a work item.
 * Uses the new architecture with adapters to domain services.
 */
public class HistoryCommand implements Callable<Integer> {
    private String user;
    private UUID itemId;
    private String timeRange;
    private boolean showComments = true;
    private boolean showStateChanges = true;
    private boolean showAssignments = true;
    private boolean showFieldChanges = true;
    private String format = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final MockWorkflowService workflowService;
    private final ItemService itemService;
    private final MockHistoryService historyService;
    private final MockCommentService commentService;
    private final MetadataService metadataService;
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Default constructor for picocli.
     */
    public HistoryCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Constructor with ServiceManager for testing.
     *
     * @param serviceManager The service manager
     */
    public HistoryCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.workflowService = serviceManager.getMockWorkflowService();
        this.itemService = serviceManager.getItemService();
        this.historyService = serviceManager.getMockHistoryService();
        this.commentService = serviceManager.getMockCommentService();
        this.metadataService = serviceManager.getMetadataService();
        this.user = System.getProperty("user.name");
    }
    
    /**
     * Sets the work item ID to show history for.
     * If not set, history for the current work item in progress will be shown.
     *
     * @param itemId the work item ID
     */
    public void setItemId(String itemId) {
        try {
            this.itemId = UUID.fromString(itemId);
        } catch (IllegalArgumentException e) {
            // Check if this is a short ID format (e.g., "WI-123")
            if (itemId != null && itemId.contains("-")) {
                try {
                    // Attempt to get the full ID from the service using the short ID
                    String shortId = itemId;
                    WorkItem workItem = serviceManager.getItemService().findItemByShortId(shortId);
                    if (workItem != null) {
                        this.itemId = UUID.fromString(workItem.getId());
                    } else {
                        throw new IllegalArgumentException("Work item not found with ID: " + shortId);
                    }
                } catch (Exception ex) {
                    throw new IllegalArgumentException("Invalid work item ID: " + itemId);
                }
            } else {
                throw new IllegalArgumentException("Invalid work item ID format: " + itemId);
            }
        }
    }
    
    /**
     * Sets the time range to filter history by.
     * Examples: "1h" (last hour), "1d" (last day), "1w" (last week)
     *
     * @param timeRange the time range
     */
    public void setTimeRange(String timeRange) {
        this.timeRange = timeRange;
    }
    
    /**
     * Sets whether to show comments in the history.
     *
     * @param showComments true to show comments, false to hide them
     */
    public void setShowComments(boolean showComments) {
        this.showComments = showComments;
    }
    
    /**
     * Sets whether to show state changes in the history.
     *
     * @param showStateChanges true to show state changes, false to hide them
     */
    public void setShowStateChanges(boolean showStateChanges) {
        this.showStateChanges = showStateChanges;
    }
    
    /**
     * Sets whether to show assignments in the history.
     *
     * @param showAssignments true to show assignments, false to hide them
     */
    public void setShowAssignments(boolean showAssignments) {
        this.showAssignments = showAssignments;
    }
    
    /**
     * Sets whether to show field changes in the history.
     *
     * @param showFieldChanges true to show field changes, false to hide them
     */
    public void setShowFieldChanges(boolean showFieldChanges) {
        this.showFieldChanges = showFieldChanges;
    }
    
    /**
     * Sets the user viewing the history.
     *
     * @param user the user name
     */
    public void setUser(String user) {
        this.user = user;
    }
    
    /**
     * Sets whether to output in JSON format.
     *
     * @param jsonOutput true for JSON output, false for text
     */
    public void setJsonOutput(boolean jsonOutput) {
        this.format = jsonOutput ? "json" : "text";
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
     * Sets whether to use verbose output.
     *
     * @param verbose true for verbose output, false for concise
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    @Override
    public Integer call() {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        if (itemId != null) {
            params.put("itemId", itemId.toString());
        }
        if (timeRange != null) {
            params.put("timeRange", timeRange);
        }
        params.put("showComments", showComments);
        params.put("showStateChanges", showStateChanges);
        params.put("showAssignments", showAssignments);
        params.put("showFieldChanges", showFieldChanges);
        params.put("format", format);
        params.put("verbose", verbose);
        params.put("user", user);
        
        // Start tracking operation
        String operationId = metadataService.startOperation("history", "READ", params);
        
        try {
            if (itemId == null) {
                // Find the current work item in progress
                Optional<WorkItem> currentWip = workflowService.getCurrentWorkInProgress(user);
                
                if (currentWip.isEmpty()) {
                    String errorMessage = "No work item is currently in progress";
                    System.err.println("Error: " + errorMessage);
                    System.err.println("Tip: Use 'rin list --status=IN_PROGRESS' to see in-progress items");
                    System.err.println("     Use 'rin history <item-id>' to view history for a specific item");
                    metadataService.failOperation(operationId, new IllegalStateException(errorMessage));
                    return 1;
                }
                
                itemId = UUID.fromString(currentWip.get().getId());
                if (!"json".equalsIgnoreCase(format)) {
                    System.out.println("Showing history for current work item: " + itemId);
                }
            }
            
            // Get item details for display purposes
            WorkItem workItem = itemService.getItem(itemId.toString());
            
            if (workItem == null) {
                String errorMessage = "Work item not found with ID: " + itemId;
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            // Verify item access and existence using domain services
            if (!workItem.isVisible(user)) {
                String errorMessage = "You do not have permission to view this work item";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new SecurityException(errorMessage));
                return 1;
            }
            
            // Update context with the viewed item
            ContextManager.getInstance().setLastViewedWorkItem(UUID.fromString(workItem.getId()));
            
            // Declare variables for history and comments
            List<MockHistoryService.HistoryEntryRecord> historyEntries;
            List<MockCommentService.CommentImpl> commentEntries;
            
            // Result data for operation tracking
            Map<String, Object> result = new HashMap<>();
            result.put("itemId", itemId.toString());
            result.put("itemTitle", workItem.getTitle());
            result.put("itemType", workItem.getType());
            result.put("itemStatus", workItem.getStatus());
                
            if (timeRange != null && !timeRange.isBlank()) {
                // Parse time range
                Pair<ChronoUnit, Integer> parsedRange = parseTimeRange(timeRange);
                
                if (parsedRange == null) {
                    String errorMessage = "Invalid time range format. Use format like '1h', '1d', or '1w'";
                    System.err.println("Error: " + errorMessage);
                    metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                    return 1;
                }
                
                ChronoUnit unit = parsedRange.getFirst();
                int amount = parsedRange.getSecond();
                
                // Add time range to result data
                result.put("timeRangeUnit", unit.toString());
                result.put("timeRangeAmount", amount);
                
                // Fetch history for the specified time range using mock services
                if (unit == ChronoUnit.HOURS) {
                    historyEntries = historyService.getHistoryFromLastHours(itemId, amount);
                    commentEntries = commentService.getCommentsFromLastHours(itemId, amount);
                    if (!"json".equalsIgnoreCase(format)) {
                        System.out.println("Showing history from the last " + amount + " hour(s)");
                    }
                } else if (unit == ChronoUnit.DAYS) {
                    historyEntries = historyService.getHistoryFromLastDays(itemId, amount);
                    commentEntries = commentService.getCommentsFromLastDays(itemId, amount);
                    if (!"json".equalsIgnoreCase(format)) {
                        System.out.println("Showing history from the last " + amount + " day(s)");
                    }
                } else {
                    historyEntries = historyService.getHistoryFromLastWeeks(itemId, amount);
                    commentEntries = commentService.getCommentsFromLastWeeks(itemId, amount);
                    if (!"json".equalsIgnoreCase(format)) {
                        System.out.println("Showing history from the last " + amount + " week(s)");
                    }
                }
            } else {
                // Fetch all history
                historyEntries = historyService.getHistory(itemId);
                commentEntries = commentService.getComments(itemId);
                
                if (!"json".equalsIgnoreCase(format)) {
                    System.out.println("Showing all history");
                }
            }
            
            // Add history stats to result
            int filteredHistoryCount = countFilteredEntries(historyEntries);
            int filteredCommentCount = showComments && commentEntries != null ? commentEntries.size() : 0;
            
            result.put("historyEntryCount", filteredHistoryCount);
            result.put("commentCount", filteredCommentCount);
            result.put("totalEntryCount", filteredHistoryCount + filteredCommentCount);
            
            // Display history based on format
            if ("json".equalsIgnoreCase(format)) {
                displayHistoryJson(historyEntries, commentEntries, workItem, result, operationId);
            } else {
                displayHistoryText(historyEntries, commentEntries, workItem, result, operationId);
            }
            
            return 0;
            
        } catch (IllegalArgumentException e) {
            String errorMessage = "Invalid work item ID - " + e.getMessage();
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, e);
            return 1;
        } catch (Exception e) {
            String errorMessage = "Failed to fetch history - " + e.getMessage();
            System.err.println("Error: " + errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Counts history entries that pass the current filters.
     *
     * @param historyEntries the history entries to count
     * @return the number of entries that pass the filters
     */
    private int countFilteredEntries(List<MockHistoryService.HistoryEntryRecord> historyEntries) {
        int count = 0;
        for (MockHistoryService.HistoryEntryRecord entry : historyEntries) {
            if (entry.getType() == MockHistoryService.HistoryEntryType.STATE_CHANGE && !showStateChanges) {
                continue;
            }
            if (entry.getType() == MockHistoryService.HistoryEntryType.ASSIGNMENT && !showAssignments) {
                continue;
            }
            if (entry.getType() == MockHistoryService.HistoryEntryType.FIELD_CHANGE && !showFieldChanges) {
                continue;
            }
            count++;
        }
        return count;
    }
    
    /**
     * Parses a time range string in the format "1h", "1d", or "1w".
     *
     * @param timeRange the time range string
     * @return a pair containing the time unit and amount, or null if invalid
     */
    private Pair<ChronoUnit, Integer> parseTimeRange(String timeRange) {
        try {
            // Extract the numeric part and unit
            String numericPart = timeRange.replaceAll("[^0-9]", "");
            String unitPart = timeRange.replaceAll("[0-9]", "").toLowerCase();
            
            int amount = Integer.parseInt(numericPart);
            
            if (amount <= 0) {
                return null;
            }
            
            return switch (unitPart) {
                case "h" -> new Pair<>(ChronoUnit.HOURS, amount);
                case "d" -> new Pair<>(ChronoUnit.DAYS, amount);
                case "w" -> new Pair<>(ChronoUnit.WEEKS, amount);
                default -> null;
            };
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Simple pair class for holding two values.
     *
     * @param <A> the type of the first value
     * @param <B> the type of the second value
     */
    private static class Pair<A, B> {
        private final A first;
        private final B second;
        
        public Pair(A first, B second) {
            this.first = first;
            this.second = second;
        }
        
        public A getFirst() {
            return first;
        }
        
        public B getSecond() {
            return second;
        }
    }
    
    /**
     * Displays history entries and comments in text format.
     *
     * @param historyEntries the history entries to display
     * @param commentEntries the comments to display
     * @param workItem the work item the history belongs to
     * @param result the result data for tracking
     * @param operationId the operation ID
     */
    private void displayHistoryText(List<MockHistoryService.HistoryEntryRecord> historyEntries, 
                                   List<MockCommentService.CommentImpl> commentEntries,
                                   WorkItem workItem,
                                   Map<String, Object> result,
                                   String operationId) {
        if (historyEntries.isEmpty() && (commentEntries == null || commentEntries.isEmpty())) {
            System.out.println("No history found for the specified time range");
            metadataService.completeOperation(operationId, result);
            return;
        }
        
        System.out.println("\nHistory for " + workItem.getType() + " " + workItem.getId() + ": " + workItem.getTitle());
        System.out.println("-----------------------------------------------------------------");
        System.out.println(String.format("%-20s | %-15s | %-40s", "Timestamp", "User", "Event"));
        System.out.println("-----------------------------------------------------------------");
        
        // Display history entries
        for (MockHistoryService.HistoryEntryRecord entry : historyEntries) {
            // Skip based on filtering options
            if (entry.getType() == MockHistoryService.HistoryEntryType.STATE_CHANGE && !showStateChanges) {
                continue;
            }
            if (entry.getType() == MockHistoryService.HistoryEntryType.ASSIGNMENT && !showAssignments) {
                continue;
            }
            if (entry.getType() == MockHistoryService.HistoryEntryType.FIELD_CHANGE && !showFieldChanges) {
                continue;
            }
            
            LocalDateTime timestamp = LocalDateTime.ofInstant(entry.getTimestamp(), ZoneId.systemDefault());
            System.out.println(String.format("%-20s | %-15s | %-40s",
                dateFormatter.format(timestamp),
                entry.getUser(),
                entry.getContent()
            ));
        }
        
        // Display comments
        if (showComments && commentEntries != null) {
            for (MockCommentService.CommentImpl comment : commentEntries) {
                LocalDateTime timestamp = LocalDateTime.ofInstant(comment.timestamp(), ZoneId.systemDefault());
                System.out.println(String.format("%-20s | %-15s | %-40s",
                    dateFormatter.format(timestamp),
                    comment.user(),
                    "Comment: " + comment.text()
                ));
            }
        }
        
        System.out.println("-----------------------------------------------------------------");
        
        // Complete the operation
        metadataService.completeOperation(operationId, result);
    }
    
    /**
     * Displays history entries and comments in JSON format.
     *
     * @param historyEntries the history entries to display
     * @param commentEntries the comments to display
     * @param workItem the work item the history belongs to
     * @param result the result data for tracking
     * @param operationId the operation ID
     */
    private void displayHistoryJson(List<MockHistoryService.HistoryEntryRecord> historyEntries,
                                   List<MockCommentService.CommentImpl> commentEntries,
                                   WorkItem workItem,
                                   Map<String, Object> result,
                                   String operationId) {
        // Create JSON data structure
        Map<String, Object> jsonData = new HashMap<>();
        
        // Add work item information
        Map<String, Object> workItemData = new HashMap<>();
        workItemData.put("id", workItem.getId());
        workItemData.put("title", workItem.getTitle());
        workItemData.put("type", workItem.getType());
        workItemData.put("status", workItem.getStatus());
        
        // Add verbose work item information if requested
        if (verbose) {
            workItemData.put("priority", workItem.getPriority());
            workItemData.put("assignee", workItem.getAssignee());
            workItemData.put("description", workItem.getDescription());
            workItemData.put("created", workItem.getCreated() != null ? 
                workItem.getCreated().format(dateFormatter) : null);
            workItemData.put("updated", workItem.getUpdated() != null ? 
                workItem.getUpdated().format(dateFormatter) : null);
        }
        
        jsonData.put("workItem", workItemData);
        
        // Process history entries
        List<Map<String, Object>> historyList = new ArrayList<>();
        
        // Add history entries
        for (MockHistoryService.HistoryEntryRecord entry : historyEntries) {
            // Skip based on filtering options
            if (entry.getType() == MockHistoryService.HistoryEntryType.STATE_CHANGE && !showStateChanges) {
                continue;
            }
            if (entry.getType() == MockHistoryService.HistoryEntryType.ASSIGNMENT && !showAssignments) {
                continue;
            }
            if (entry.getType() == MockHistoryService.HistoryEntryType.FIELD_CHANGE && !showFieldChanges) {
                continue;
            }
            
            Map<String, Object> entryData = new HashMap<>();
            entryData.put("id", entry.getTimestamp().toString());
            entryData.put("workItemId", entry.getWorkItemId());
            entryData.put("timestamp", entry.getTimestamp().toString());
            entryData.put("user", entry.getUser());
            entryData.put("type", entry.getType().toString());
            entryData.put("content", entry.getContent());
            
            historyList.add(entryData);
        }
        
        // Add comments
        if (showComments && commentEntries != null) {
            for (MockCommentService.CommentImpl comment : commentEntries) {
                Map<String, Object> commentData = new HashMap<>();
                commentData.put("id", comment.id());
                commentData.put("timestamp", comment.timestamp().toString());
                commentData.put("user", comment.user());
                commentData.put("type", "COMMENT");
                commentData.put("content", comment.text());
                commentData.put("commentType", comment.type().toString());
                
                historyList.add(commentData);
            }
        }
        
        jsonData.put("history", historyList);
        jsonData.put("stats", result);
        
        // Output JSON
        String json = OutputFormatter.toJson(jsonData, verbose);
        System.out.println(json);
        
        // Complete the operation
        metadataService.completeOperation(operationId, result);
    }
}
