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
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockHistoryService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.util.OutputFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Command to display the details of a work item.
 */
public class CatCommand implements Callable<Integer> {
    
    private String itemId;
    private boolean showLineNumbers = false;
    private boolean showAllFormatting = false;
    private boolean showHistory = false;
    private boolean showRelationships = false;
    private String format = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final ContextManager contextManager;
    private final MetadataService metadataService;
    
    /**
     * Sets the work item ID to display.
     *
     * @param itemId the work item ID
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    /**
     * Sets whether to show line numbers.
     *
     * @param showLineNumbers true to show line numbers
     */
    public void setShowLineNumbers(boolean showLineNumbers) {
        this.showLineNumbers = showLineNumbers;
    }
    
    /**
     * Sets whether to show all formatting.
     *
     * @param showAllFormatting true to show all formatting
     */
    public void setShowAllFormatting(boolean showAllFormatting) {
        this.showAllFormatting = showAllFormatting;
    }
    
    /**
     * Sets whether to show history.
     *
     * @param showHistory true to show history
     */
    public void setShowHistory(boolean showHistory) {
        this.showHistory = showHistory;
    }
    
    /**
     * Sets whether to show relationships.
     *
     * @param showRelationships true to show relationships
     */
    public void setShowRelationships(boolean showRelationships) {
        this.showRelationships = showRelationships;
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
    
    /**
     * Creates a new CatCommand with default services.
     */
    public CatCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new CatCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public CatCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.contextManager = ContextManager.getInstance();
        this.metadataService = serviceManager.getMetadataService();
    }
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("item_id", itemId);
        params.put("show_line_numbers", showLineNumbers);
        params.put("show_all_formatting", showAllFormatting);
        params.put("show_history", showHistory);
        params.put("show_relationships", showRelationships);
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("cat", "READ", params);
        
        try {
            // Get the services through service manager
            ItemService itemService = serviceManager.getItemService();
            MockHistoryService historyService = serviceManager.getMockHistoryService();
            
            // Determine the work item ID to display
            UUID workItemId;
            if (itemId != null && !itemId.isEmpty()) {
                try {
                    workItemId = UUID.fromString(itemId);
                } catch (IllegalArgumentException e) {
                    String errorMessage = "Invalid work item ID format: " + itemId;
                    System.err.println("Error: " + errorMessage);
                    metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                    return 1;
                }
            } else {
                // Use the last viewed work item
                workItemId = contextManager.getLastViewedWorkItem();
                if (workItemId == null) {
                    String errorMessage = "No work item context available. Please specify an ID.";
                    System.err.println("Error: " + errorMessage);
                    metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                    return 1;
                }
            }
            
            // Get the work item
            WorkItem workItem = itemService.getItem(workItemId.toString());
            if (workItem == null) {
                String errorMessage = "Work item not found: " + workItemId;
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            // Update the last viewed work item
            contextManager.setLastViewedWorkItem(workItemId);
            
            // Display the work item based on format
            if ("json".equalsIgnoreCase(format)) {
                displayJsonOutput(workItem, (MockHistoryService) historyService);
            } else {
                displayWorkItem(workItem, (MockHistoryService) historyService);
            }
            
            // Record the successful operation
            Map<String, Object> result = new HashMap<>();
            result.put("item_id", workItem.getId());
            result.put("title", workItem.getTitle());
            result.put("status", workItem.getState() != null ? workItem.getState().toString() : null);
            
            metadataService.completeOperation(operationId, result);
            return 0;
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error displaying work item: " + e.getMessage();
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
     * Displays a work item with the specified options.
     *
     * @param workItem the work item to display
     * @param historyService the history service for fetching history entries
     */
    private void displayWorkItem(WorkItem workItem, MockHistoryService historyService) {
        // Split descriptions into lines for potential line numbering
        String description = workItem.getDescription() != null ? workItem.getDescription() : "";
        String[] descriptionLines = description.split("\\r?\\n");
        
        // Header
        System.out.println("====== WORK ITEM " + workItem.getId() + " ======");
        
        // Title
        if (showLineNumbers) {
            System.out.println("1  Title: " + workItem.getTitle());
        } else {
            System.out.println("Title: " + workItem.getTitle());
        }
        
        // Basic metadata
        int lineNum = 2;
        if (showLineNumbers) {
            System.out.println(lineNum++ + "  Type: " + workItem.getType());
            System.out.println(lineNum++ + "  Priority: " + workItem.getPriority());
            System.out.println(lineNum++ + "  State: " + workItem.getState());
            System.out.println(lineNum++ + "  Assignee: " + workItem.getAssignee());
            System.out.println(lineNum++ + "  Reporter: " + "N/A");
            System.out.println(lineNum++ + "  Created: " + formatDate(workItem.getCreated()));
            System.out.println(lineNum++ + "  Updated: " + formatDate(workItem.getUpdated()));
        } else {
            System.out.println("Type: " + workItem.getType());
            System.out.println("Priority: " + workItem.getPriority());
            System.out.println("State: " + workItem.getState());
            System.out.println("Assignee: " + workItem.getAssignee());
            System.out.println("Reporter: " + "N/A");
            System.out.println("Created: " + formatDate(workItem.getCreated()));
            System.out.println("Updated: " + formatDate(workItem.getUpdated()));
        }
        
        // Description (with optional line numbers)
        System.out.println();
        if (showLineNumbers) {
            System.out.println(lineNum++ + "  Description:");
        } else {
            System.out.println("Description:");
        }
        
        // Display description lines
        for (String line : descriptionLines) {
            if (showLineNumbers) {
                System.out.println(lineNum++ + "  " + formatLine(line));
            } else {
                System.out.println(formatLine(line));
            }
        }
        
        // Display relationships if requested
        if (showRelationships) {
            // Relationships not implemented in mock version
            if (showLineNumbers) {
                System.out.println(lineNum++ + "  Relationships: None");
            } else {
                System.out.println("Relationships: None");
            }
        }
        
        // Display history if requested
        if (showHistory) {
            displayHistory(UUID.fromString(workItem.getId()), historyService, showLineNumbers ? lineNum : 0);
        }
    }
    
    /**
     * Formats a date for display.
     *
     * @param date the date to format
     * @return the formatted date string
     */
    private String formatDate(java.time.LocalDateTime date) {
        return date.toString();
    }
    
    /**
     * Formats a line based on display options.
     *
     * @param line the line to format
     * @return the formatted line
     */
    private String formatLine(String line) {
        if (showAllFormatting) {
            // Replace tabs with visible markers
            line = line.replace("\t", "→   ");
            
            // Mark line endings
            line = line + "¶";
        }
        return line;
    }
    
    // Remaining code removed
    
    /**
     * Displays history for a work item.
     *
     * @param workItemId the work item ID
     * @param historyService the history service
     * @param startLineNum the starting line number (if line numbers are enabled)
     */
    private void displayHistory(UUID workItemId, MockHistoryService historyService, int startLineNum) {
        int lineNum = startLineNum;
        System.out.println();
        if (showLineNumbers) {
            System.out.println(lineNum++ + "  History:");
        } else {
            System.out.println("History:");
        }
        
        List<MockHistoryService.HistoryEntryRecord> history = historyService.getHistory(workItemId);
        
        if (history.isEmpty()) {
            if (showLineNumbers) {
                System.out.println(lineNum++ + "  No history entries");
            } else {
                System.out.println("No history entries");
            }
            return;
        }
        
        for (MockHistoryService.HistoryEntryRecord entry : history) {
            String historyLine = formatDate(java.time.LocalDateTime.ofInstant(entry.getTimestamp(), 
                                 java.time.ZoneId.systemDefault())) + " - " + 
                                 entry.getType() + " by " + entry.getUser();
            
            // Add content information
            historyLine += ": " + entry.getContent();
            
            if (showLineNumbers) {
                System.out.println(lineNum++ + "  " + historyLine);
            } else {
                System.out.println(historyLine);
            }
        }
    }
    
    /**
     * Displays the work item in JSON format.
     *
     * @param workItem the work item to display
     * @param historyService the history service for fetching history
     */
    private void displayJsonOutput(WorkItem workItem, MockHistoryService historyService) {
        // Prepare a Map to be converted to JSON
        Map<String, Object> jsonData = new HashMap<>();
        
        // Add basic work item information
        jsonData.put("id", workItem.getId());
        jsonData.put("title", workItem.getTitle());
        jsonData.put("description", workItem.getDescription());
        jsonData.put("type", workItem.getType() != null ? workItem.getType().toString() : null);
        jsonData.put("priority", workItem.getPriority() != null ? workItem.getPriority().toString() : null);
        jsonData.put("status", workItem.getState() != null ? workItem.getState().toString() : null);
        jsonData.put("assignee", workItem.getAssignee());
        jsonData.put("created", workItem.getCreated() != null ? workItem.getCreated().toString() : null);
        jsonData.put("updated", workItem.getUpdated() != null ? workItem.getUpdated().toString() : null);
        
        // Add history if requested
        if (showHistory) {
            List<MockHistoryService.HistoryEntryRecord> history = historyService.getHistory(UUID.fromString(workItem.getId()));
            List<Map<String, Object>> historyEntries = new ArrayList<>();
            
            for (MockHistoryService.HistoryEntryRecord entry : history) {
                Map<String, Object> entryMap = new HashMap<>();
                entryMap.put("timestamp", entry.getTimestamp().toString());
                entryMap.put("type", entry.getType());
                entryMap.put("user", entry.getUser());
                entryMap.put("content", entry.getContent());
                historyEntries.add(entryMap);
            }
            
            jsonData.put("history", historyEntries);
        }
        
        // Add display options to the output
        Map<String, Object> options = new HashMap<>();
        options.put("showLineNumbers", showLineNumbers);
        options.put("showAllFormatting", showAllFormatting);
        options.put("showHistory", showHistory);
        options.put("showRelationships", showRelationships);
        jsonData.put("displayOptions", options);
        
        // Use the OutputFormatter for consistent JSON output
        String jsonOutput = OutputFormatter.toJson(jsonData, verbose);
        System.out.println(jsonOutput);
    }
}