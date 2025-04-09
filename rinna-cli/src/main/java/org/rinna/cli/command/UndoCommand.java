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
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.model.Priority;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockHistoryService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import static org.rinna.cli.service.MockHistoryService.HistoryEntryType.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

/**
 * Command to undo the last action performed on a work item.
 * Supports undoing up to 3 steps back on the current active work item.
 * Follows the ViewCommand pattern with operation tracking.
 */
public class UndoCommand implements Callable<Integer> {
    
    private String itemId;
    private boolean force = false;
    private String username = System.getProperty("user.name");
    private Integer step = null; // Step number to undo (0-based index)
    private boolean steps = false; // Flag to list available steps
    private String format = "text"; // Output format (text or json)
    private boolean verbose = false; // Flag to show detailed output
    
    // Time limit for undo operations (24 hours)
    private static final int TIME_LIMIT_HOURS = 24;
    
    // Maximum number of steps back that can be undone
    private static final int MAX_UNDO_STEPS = 3;
    
    // Restricted states that prevent undo operations
    private static final List<WorkflowState> RESTRICTED_STATES = List.of(
            WorkflowState.DONE
    );
    
    private final ServiceManager serviceManager;
    private final MockWorkflowService workflowService;
    private final MockHistoryService historyService;
    private final MockItemService itemService;
    private final MetadataService metadataService;
    private final ContextManager contextManager;
    
    /**
     * Default constructor for picocli.
     */
    public UndoCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Constructor with ServiceManager for testing.
     *
     * @param serviceManager The service manager
     */
    public UndoCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.workflowService = serviceManager.getMockWorkflowService();
        this.historyService = serviceManager.getMockHistoryService();
        this.itemService = (MockItemService) serviceManager.getItemService();
        this.metadataService = serviceManager.getMetadataService();
        this.contextManager = ContextManager.getInstance();
    }
    
    /**
     * Gets the work item ID.
     *
     * @return the work item ID
     */
    public String getItemId() {
        return itemId;
    }
    
    /**
     * Sets the work item ID.
     *
     * @param itemId the work item ID to set
     */
    public void setItemId(String itemId) {
        this.itemId = itemId;
    }
    
    /**
     * Gets the force flag.
     *
     * @return the force flag
     */
    public boolean isForce() {
        return force;
    }
    
    /**
     * Sets the force flag.
     *
     * @param force the force flag to set
     */
    public void setForce(boolean force) {
        this.force = force;
    }
    
    /**
     * Gets the step number to undo (0-based index).
     *
     * @return the step number
     */
    public Integer getStep() {
        return step;
    }
    
    /**
     * Sets the step number to undo (0-based index).
     *
     * @param step the step number
     */
    public void setStep(Integer step) {
        this.step = step;
    }
    
    /**
     * Gets the steps flag.
     *
     * @return the steps flag
     */
    public boolean isSteps() {
        return steps;
    }
    
    /**
     * Sets the steps flag to list available undo steps.
     *
     * @param steps the steps flag
     */
    public void setSteps(boolean steps) {
        this.steps = steps;
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
     * Sets the JSON output format flag.
     *
     * @param jsonOutput true to output in JSON format
     */
    public void setJsonOutput(boolean jsonOutput) {
        this.format = jsonOutput ? "json" : "text";
    }
    
    /**
     * Sets the verbose output flag.
     *
     * @param verbose true to enable verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    @Override
    public Integer call() {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        params.put("force", force);
        params.put("format", format);
        params.put("verbose", verbose);
        
        if (itemId != null) {
            params.put("itemId", itemId);
        }
        if (step != null) {
            params.put("step", step);
        }
        params.put("steps", steps);
        
        // Start tracking operation
        String operationId = metadataService.startOperation("undo", "UPDATE", params);
        
        try {
            // If no specific item ID was provided, get the current work item in progress
            UUID workItemId;
            WorkItem workItem;
            
            if (itemId == null || itemId.isEmpty()) {
                // Get current work item
                workItemId = findCurrentWorkItemId(operationId);
                if (workItemId == null) {
                    return 1;
                }
                workItem = itemService.getItem(workItemId.toString());
            } else {
                try {
                    workItemId = UUID.fromString(itemId);
                    workItem = itemService.getItem(workItemId.toString());
                    
                    if (workItem == null) {
                        String errorMessage = "Work item not found: " + itemId;
                        System.err.println("Error: " + errorMessage);
                        metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                        return 1;
                    }
                    
                    // Check ownership
                    if (!username.equals(workItem.getAssignee())) {
                        String errorMessage = "You do not have permission to undo changes to this work item";
                        System.err.println("Error: " + errorMessage);
                        metadataService.failOperation(operationId, new SecurityException(errorMessage));
                        return 1;
                    }
                } catch (IllegalArgumentException e) {
                    // Try to find by short ID format
                    try {
                        workItem = itemService.findItemByShortId(itemId);
                        if (workItem != null) {
                            workItemId = UUID.fromString(workItem.getId());
                        } else {
                            String errorMessage = "Work item not found: " + itemId;
                            System.err.println("Error: " + errorMessage);
                            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                            return 1;
                        }
                    } catch (Exception ex) {
                        String errorMessage = "Invalid work item ID: " + itemId;
                        System.err.println("Error: " + errorMessage);
                        metadataService.failOperation(operationId, ex);
                        return 1;
                    }
                }
            }
            
            // Update context with the viewed item
            contextManager.setLastViewedItem(workItem);
            
            // Check if the item is in a restricted state
            if (RESTRICTED_STATES.contains(workItem.getStatus())) {
                String errorMessage;
                if (workItem.getStatus() == WorkflowState.DONE) {
                    errorMessage = "Undo history is cleared when work item is closed";
                } else {
                    errorMessage = "Cannot undo changes to items in " + workItem.getStatus() + " state";
                }
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalStateException(errorMessage));
                return 1;
            }
            
            // Check if the item is the current active item for the user
            WorkItem currentActive = workflowService.getCurrentWorkItem(username);
            if (currentActive != null && !workItemId.toString().equals(currentActive.getId())) {
                String errorMessage = "Undo history is cleared when changing work items";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalStateException(errorMessage));
                return 1;
            }
            
            // Get the history entries
            List<MockHistoryService.HistoryEntryRecord> history = historyService.getHistory(workItemId);
            
            if (history.isEmpty()) {
                String errorMessage = "No recent changes found to undo";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalStateException(errorMessage));
                return 1;
            }
            
            // Limit to MAX_UNDO_STEPS entries
            List<MockHistoryService.HistoryEntryRecord> recentHistory = history.stream()
                .limit(MAX_UNDO_STEPS)
                .collect(Collectors.toList());
            
            // Add history info to operation params
            params.put("historyCount", recentHistory.size());
            
            // Handle step parameter if provided
            if (step != null) {
                return handleSpecificStepUndo(step, recentHistory, workItem, workItemId, operationId);
            }
            
            // Handle steps flag for interactive selection
            if (steps) {
                return handleInteractiveUndoSelection(recentHistory, workItem, workItemId, operationId);
            }
            
            // Default behavior: undo the most recent change
            MockHistoryService.HistoryEntryRecord lastEntry = recentHistory.get(0);
            
            // Check time limit
            Instant entryTime = lastEntry.getTimestamp();
            Instant timeLimit = Instant.now().minus(TIME_LIMIT_HOURS, ChronoUnit.HOURS);
            
            if (entryTime.isBefore(timeLimit)) {
                String errorMessage = "Cannot undo changes older than " + TIME_LIMIT_HOURS + " hours";
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalStateException(errorMessage));
                return 1;
            }
            
            return processUndoForEntry(lastEntry, workItem, workItemId, operationId);
            
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            System.err.println("Error: " + errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Finds the current work item ID for the user.
     *
     * @param operationId the operation ID for tracking
     * @return the work item ID, or null if not found
     */
    private UUID findCurrentWorkItemId(String operationId) {
        List<WorkItem> inProgressItems = workflowService.findByStatus(WorkflowState.IN_PROGRESS);
        List<WorkItem> userItems = new ArrayList<>();
        
        // Filter for items assigned to the current user
        for (WorkItem item : inProgressItems) {
            if (username.equals(item.getAssignee())) {
                userItems.add(item);
            }
        }
        
        if (userItems.isEmpty()) {
            String errorMessage = "No work item is currently in progress for user " + username;
            System.err.println("Error: " + errorMessage);
            System.err.println("Tip: Use 'rin list --status=IN_PROGRESS' to see all in-progress items");
            metadataService.failOperation(operationId, new IllegalStateException(errorMessage));
            return null;
        }
        
        WorkItem workItem = userItems.get(0);
        return UUID.fromString(workItem.getId());
    }
    
    /**
     * Handles undoing a specific step by index.
     *
     * @param stepIndex the step index to undo
     * @param recentHistory the recent history entries
     * @param workItem the work item
     * @param workItemId the work item ID
     * @param operationId the operation ID for tracking
     * @return exit code
     */
    private int handleSpecificStepUndo(int stepIndex, List<MockHistoryService.HistoryEntryRecord> recentHistory, 
                                    WorkItem workItem, UUID workItemId, String operationId) {
        if (stepIndex >= MAX_UNDO_STEPS) {
            String errorMessage = "Cannot undo more than " + MAX_UNDO_STEPS + " steps back";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        if (stepIndex >= recentHistory.size()) {
            String errorMessage = "Only " + recentHistory.size() + " changes are available to undo";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        // Get the specified entry
        MockHistoryService.HistoryEntryRecord entryToUndo = recentHistory.get(stepIndex);
        
        // Create a suboperation for this specific undo
        Map<String, Object> stepParams = new HashMap<>();
        stepParams.put("workItemId", workItemId.toString());
        stepParams.put("step", stepIndex);
        stepParams.put("entryType", entryToUndo.getType().toString());
        stepParams.put("entryTimestamp", entryToUndo.getTimestamp().toString());
        
        String stepOperationId = metadataService.trackOperation("undo-step", stepParams);
        
        // Process the undo operation
        int result = processUndoForEntry(entryToUndo, workItem, workItemId, stepOperationId);
        
        // Update the parent operation
        if (result == 0) {
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("step", stepIndex);
            resultData.put("entryType", entryToUndo.getType().toString());
            metadataService.completeOperation(operationId, resultData);
        } else {
            // The stepOperationId already has the failure recorded
            metadataService.failOperation(operationId, new RuntimeException("Failed to undo step " + stepIndex));
        }
        
        return result;
    }
    
    /**
     * Handles the interactive selection of an undo step.
     *
     * @param recentHistory the recent history entries
     * @param workItem the work item
     * @param workItemId the work item ID
     * @param operationId the operation ID for tracking
     * @return exit code
     */
    private int handleInteractiveUndoSelection(List<MockHistoryService.HistoryEntryRecord> recentHistory, 
                                           WorkItem workItem, UUID workItemId, String operationId) {
        // Display the available changes
        if ("json".equalsIgnoreCase(format)) {
            // Output changes in JSON format
            outputHistoryJson(recentHistory, workItem, operationId);
            return 0;
        }
        
        System.out.println("Available changes to undo for work item " + workItemId + " (" + workItem.getTitle() + "):");
        System.out.println("----------------------------------------");
        
        for (int i = 0; i < recentHistory.size(); i++) {
            MockHistoryService.HistoryEntryRecord entry = recentHistory.get(i);
            System.out.printf("%d. [%s] %s by %s: ", 
                (i + 1), // Display 1-based index to users
                entry.getTimestamp(),
                entry.getType(),
                entry.getUser());
            
            // Display change details based on type
            switch (entry.getType()) {
                case STATE_CHANGE:
                    System.out.printf("State changed: %s\n", entry.getContent());
                    break;
                case FIELD_CHANGE:
                    System.out.printf("Field changed: %s\n", entry.getContent());
                    break;
                case ASSIGNMENT:
                    System.out.printf("Assignment changed: %s\n", entry.getContent());
                    break;
                // Note: PRIORITY_CHANGE is handled as a FIELD_CHANGE
                case LINK:
                    System.out.printf("Relationship changed: %s\n", entry.getContent());
                    break;
                default:
                    System.out.printf("%s\n", entry.getContent());
            }
        }
        
        // Prompt for selection
        System.out.println("\nSelect a change to undo (1-" + recentHistory.size() + ") or 'q' to cancel: ");
        Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8.name());
        String input = scanner.nextLine().trim();
        
        if (input.equalsIgnoreCase("q") || input.isEmpty()) {
            System.out.println("Undo operation canceled");
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("action", "cancel");
            resultData.put("reason", "user cancelled selection");
            metadataService.completeOperation(operationId, resultData);
            
            return 0;
        }
        
        try {
            int selection = Integer.parseInt(input);
            if (selection < 1 || selection > recentHistory.size()) {
                String errorMessage = "Selection must be between 1 and " + recentHistory.size();
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            // Get the selected entry (convert from 1-based to 0-based index)
            MockHistoryService.HistoryEntryRecord selectedEntry = recentHistory.get(selection - 1);
            
            // Create a suboperation for this specific undo
            Map<String, Object> selectionParams = new HashMap<>();
            selectionParams.put("workItemId", workItemId.toString());
            selectionParams.put("selection", selection);
            selectionParams.put("entryType", selectedEntry.getType().toString());
            selectionParams.put("entryTimestamp", selectedEntry.getTimestamp().toString());
            
            String selectionOperationId = metadataService.trackOperation("undo-selection", selectionParams);
            
            // Process the undo operation
            int result = processUndoForEntry(selectedEntry, workItem, workItemId, selectionOperationId);
            
            // Update the parent operation
            if (result == 0) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                resultData.put("selection", selection);
                resultData.put("entryType", selectedEntry.getType().toString());
                metadataService.completeOperation(operationId, resultData);
            } else {
                // The selectionOperationId already has the failure recorded
                metadataService.failOperation(operationId, new RuntimeException("Failed to undo selected step " + selection));
            }
            
            return result;
            
        } catch (NumberFormatException e) {
            String errorMessage = "Invalid selection. Please enter a number or 'q'";
            System.err.println("Error: " + errorMessage);
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Outputs history entries in JSON format.
     *
     * @param history the history entries
     * @param workItem the work item
     * @param operationId the operation ID for tracking
     */
    private void outputHistoryJson(List<MockHistoryService.HistoryEntryRecord> history, 
                                  WorkItem workItem, String operationId) {
        // Create a map for JSON output
        Map<String, Object> jsonData = new HashMap<>();
        
        // Add work item information
        Map<String, Object> workItemData = new HashMap<>();
        workItemData.put("id", workItem.getId());
        workItemData.put("title", workItem.getTitle());
        workItemData.put("type", workItem.getType().toString());
        workItemData.put("status", workItem.getStatus().toString());
        
        // Add verbose information if requested
        if (verbose) {
            workItemData.put("description", workItem.getDescription());
            workItemData.put("assignee", workItem.getAssignee());
            workItemData.put("priority", workItem.getPriority().toString());
            workItemData.put("created", workItem.getCreated() != null ? workItem.getCreated().toString() : null);
            workItemData.put("updated", workItem.getUpdated() != null ? workItem.getUpdated().toString() : null);
        }
        
        jsonData.put("workItem", workItemData);
        
        // Add history entries
        List<Map<String, Object>> historyList = new ArrayList<>();
        
        for (int i = 0; i < history.size(); i++) {
            MockHistoryService.HistoryEntryRecord entry = history.get(i);
            
            Map<String, Object> entryData = new HashMap<>();
            entryData.put("index", i + 1);
            entryData.put("timestamp", entry.getTimestamp().toString());
            entryData.put("type", entry.getType().toString());
            entryData.put("user", entry.getUser());
            entryData.put("content", entry.getContent());
            
            if (entry.getAdditionalData() != null && !entry.getAdditionalData().isEmpty()) {
                entryData.put("additionalData", entry.getAdditionalData());
            }
            
            historyList.add(entryData);
        }
        
        jsonData.put("history", historyList);
        
        // Convert to JSON and output
        System.out.println(OutputFormatter.toJson(jsonData, verbose));
        
        // Complete the operation
        Map<String, Object> resultData = new HashMap<>();
        resultData.put("action", "list");
        resultData.put("itemId", workItem.getId());
        resultData.put("historyCount", history.size());
        metadataService.completeOperation(operationId, resultData);
    }
    
    /**
     * Processes the undo operation for a specific history entry.
     *
     * @param entry the history entry to undo
     * @param workItem the work item
     * @param workItemId the work item ID
     * @param operationId the operation ID for tracking
     * @return exit code
     */
    private int processUndoForEntry(MockHistoryService.HistoryEntryRecord entry, WorkItem workItem, 
                                   UUID workItemId, String operationId) {
        // Extract entry information for operation tracking
        Map<String, Object> entryInfo = new HashMap<>();
        entryInfo.put("entryType", entry.getType().toString());
        entryInfo.put("entryTimestamp", entry.getTimestamp().toString());
        entryInfo.put("entryUser", entry.getUser());
        entryInfo.put("entryContent", entry.getContent());
        
        metadataService.completeOperation(operationId, entryInfo);
        
        // Create a new operation for the confirmation step
        Map<String, Object> confirmParams = new HashMap<>();
        confirmParams.put("workItemId", workItemId.toString());
        confirmParams.put("entryType", entry.getType().toString());
        confirmParams.put("force", force);
        
        String confirmOperationId = metadataService.trackOperation("undo-confirm", confirmParams);
        
        // Display the change and ask for confirmation
        boolean proceed = displayChangeAndConfirm(entry, workItem, confirmOperationId);
        
        if (!proceed) {
            System.out.println("Undo operation canceled");
            
            Map<String, Object> cancelResult = new HashMap<>();
            cancelResult.put("action", "cancel");
            cancelResult.put("reason", "user confirmation declined");
            metadataService.completeOperation(confirmOperationId, cancelResult);
            
            return 0;
        }
        
        // Apply the undo based on the type of change
        switch (entry.getType()) {
            case STATE_CHANGE:
                return undoStateChange(entry, workItemId, confirmOperationId);
            case FIELD_CHANGE:
                // Check if it's a priority-related field change
                if (entry.getContent().toLowerCase().contains("priority")) {
                    return undoPriorityChange(entry, workItemId, confirmOperationId);
                }
                return undoFieldChange(entry, workItemId, confirmOperationId);
            case ASSIGNMENT:
                return undoAssignmentChange(entry, workItemId, confirmOperationId);
            default:
                String errorMessage = "Unsupported change type: " + entry.getType();
                System.err.println("Error: " + errorMessage);
                metadataService.failOperation(confirmOperationId, new UnsupportedOperationException(errorMessage));
                return 1;
        }
    }
    
    /**
     * Display the change details and ask for confirmation.
     *
     * @param historyEntry the history entry
     * @param workItem the current work item
     * @param operationId the operation ID for tracking
     * @return true if the user confirms or force is enabled, false otherwise
     */
    private boolean displayChangeAndConfirm(MockHistoryService.HistoryEntryRecord historyEntry, 
                                          WorkItem workItem, String operationId) {
        String currentValue;
        String previousValue;
        String fieldName;
        
        switch (historyEntry.getType()) {
            case STATE_CHANGE:
                fieldName = "state";
                currentValue = workItem.getStatus().toString();
                String[] states = historyEntry.getContent().split("→");
                previousValue = states[0].trim();
                break;
            case FIELD_CHANGE:
                String[] fieldParts = historyEntry.getContent().split(":", 2);
                fieldName = fieldParts[0].trim();
                String[] values = fieldParts[1].split("→");
                previousValue = values[0].trim();
                currentValue = values[1].trim();
                break;
            case ASSIGNMENT:
                fieldName = "assignee";
                currentValue = workItem.getAssignee();
                String[] assignees = historyEntry.getContent().split("→");
                previousValue = assignees[0].trim();
                break;
            // PRIORITY_CHANGE is handled within FIELD_CHANGE now
            default:
                fieldName = "unknown";
                currentValue = "unknown";
                previousValue = "unknown";
        }
        
        // Create confirmation data for tracking
        Map<String, Object> confirmData = new HashMap<>();
        confirmData.put("field", fieldName);
        confirmData.put("currentValue", currentValue);
        confirmData.put("previousValue", previousValue);
        confirmData.put("changedBy", historyEntry.getUser());
        confirmData.put("timestamp", historyEntry.getTimestamp().toString());
        
        if ("json".equalsIgnoreCase(format)) {
            // Add information about the undo action
            confirmData.put("action", "undo");
            confirmData.put("workItem", workItem.getId());
            
            // Output confirmation in JSON format
            System.out.println(OutputFormatter.toJson(confirmData, verbose));
            
            // In JSON mode, only proceed if force is enabled
            if (force) {
                confirmData.put("confirmed", true);
                confirmData.put("confirmMethod", "force");
                metadataService.completeOperation(operationId, confirmData);
                return true;
            } else {
                confirmData.put("confirmed", false);
                confirmData.put("reason", "json mode requires --force flag for non-interactive operation");
                metadataService.completeOperation(operationId, confirmData);
                return false;
            }
        }
        
        System.out.println("Undo last change to work item " + workItem.getId());
        System.out.println("Current " + fieldName + ": " + currentValue);
        System.out.println("Previous " + fieldName + ": " + previousValue);
        System.out.println("Changed by: " + historyEntry.getUser() + " at " + historyEntry.getTimestamp());
        
        if (force) {
            confirmData.put("confirmed", true);
            confirmData.put("confirmMethod", "force");
            metadataService.completeOperation(operationId, confirmData);
            return true;
        }
        
        System.out.print("Are you sure you want to revert this change? [y/N]: ");
        Scanner scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8.name());
        String input = scanner.nextLine().trim().toLowerCase();
        
        boolean confirmed = input.equals("y") || input.equals("yes");
        
        confirmData.put("confirmed", confirmed);
        confirmData.put("confirmMethod", "interactive");
        confirmData.put("userResponse", input);
        metadataService.completeOperation(operationId, confirmData);
        
        return confirmed;
    }
    
    /**
     * Undo a state change.
     *
     * @param historyEntry the history entry
     * @param workItemId the work item ID
     * @param operationId the operation ID for tracking
     * @return exit code
     */
    private int undoStateChange(MockHistoryService.HistoryEntryRecord historyEntry, UUID workItemId, String operationId) {
        // Parse content which should be in format "PreviousState → NewState"
        String[] states = historyEntry.getContent().split("→");
        String previousState = states[0].trim();
        WorkflowState targetState = WorkflowState.valueOf(previousState);
        
        // Create state change data for tracking
        Map<String, Object> stateChangeData = new HashMap<>();
        stateChangeData.put("action", "state_revert");
        stateChangeData.put("workItemId", workItemId.toString());
        stateChangeData.put("targetState", targetState.toString());
        
        try {
            workflowService.transition(workItemId.toString(), username, targetState, "Undoing previous state change");
            
            // Update tracking data with success
            stateChangeData.put("success", true);
            
            // Output the result based on format
            if ("json".equalsIgnoreCase(format)) {
                System.out.println(OutputFormatter.toJson(stateChangeData, verbose));
            } else {
                System.out.println("Successfully reverted state to " + targetState);
            }
            
            // Complete the operation
            metadataService.completeOperation(operationId, stateChangeData);
            
            return 0;
        } catch (Exception e) {
            String errorMsg = "Error reverting state: " + e.getMessage();
            System.err.println("Error: " + errorMsg);
            if (verbose && !"json".equalsIgnoreCase(format)) {
                e.printStackTrace();
            }
            
            // Update tracking data with failure
            stateChangeData.put("success", false);
            stateChangeData.put("errorMessage", errorMsg);
            
            // Output the result in JSON format if requested
            if ("json".equalsIgnoreCase(format)) {
                System.out.println(OutputFormatter.toJson(stateChangeData, verbose));
            }
            
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Undo a field change.
     *
     * @param historyEntry the history entry
     * @param workItemId the work item ID
     * @param operationId the operation ID for tracking
     * @return exit code
     */
    private int undoFieldChange(MockHistoryService.HistoryEntryRecord historyEntry, UUID workItemId, String operationId) {
        // Parse content which should be in format "Field: PreviousValue → NewValue"
        String[] parts = historyEntry.getContent().split(":", 2);
        String field = parts[0].trim();
        String[] values = parts[1].split("→");
        String previousValue = values[0].trim();
        
        // Create field change data for tracking
        Map<String, Object> fieldChangeData = new HashMap<>();
        fieldChangeData.put("action", "field_revert");
        fieldChangeData.put("workItemId", workItemId.toString());
        fieldChangeData.put("field", field);
        fieldChangeData.put("value", previousValue);
        
        try {
            // Update the field with its previous value
            if ("title".equals(field.toLowerCase())) {
                itemService.updateTitle(workItemId, previousValue, username);
            } else if ("description".equals(field.toLowerCase())) {
                itemService.updateDescription(workItemId, previousValue, username);
            } else {
                // Generic field update (for extensibility)
                itemService.updateField(workItemId, field, previousValue, username);
            }
            
            // Update tracking data with success
            fieldChangeData.put("success", true);
            
            // Output the result based on format
            if ("json".equalsIgnoreCase(format)) {
                System.out.println(OutputFormatter.toJson(fieldChangeData, verbose));
            } else {
                System.out.println("Successfully reverted " + field + " to '" + previousValue + "'");
            }
            
            // Complete the operation
            metadataService.completeOperation(operationId, fieldChangeData);
            
            return 0;
        } catch (Exception e) {
            String errorMsg = "Error reverting field: " + e.getMessage();
            System.err.println("Error: " + errorMsg);
            if (verbose && !"json".equalsIgnoreCase(format)) {
                e.printStackTrace();
            }
            
            // Update tracking data with failure
            fieldChangeData.put("success", false);
            fieldChangeData.put("errorMessage", errorMsg);
            
            // Output the result in JSON format if requested
            if ("json".equalsIgnoreCase(format)) {
                System.out.println(OutputFormatter.toJson(fieldChangeData, verbose));
            }
            
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Undo an assignment change.
     *
     * @param historyEntry the history entry
     * @param workItemId the work item ID
     * @param operationId the operation ID for tracking
     * @return exit code
     */
    private int undoAssignmentChange(MockHistoryService.HistoryEntryRecord historyEntry, UUID workItemId, String operationId) {
        // Parse content which should be in format "PreviousAssignee → NewAssignee"
        String[] assignees = historyEntry.getContent().split("→");
        String previousAssignee = assignees[0].trim();
        
        // Create assignment change data for tracking
        Map<String, Object> assignmentData = new HashMap<>();
        assignmentData.put("action", "assignment_revert");
        assignmentData.put("workItemId", workItemId.toString());
        assignmentData.put("assignee", previousAssignee);
        
        try {
            itemService.assignTo(workItemId, previousAssignee, username);
            
            // Update tracking data with success
            assignmentData.put("success", true);
            
            // Output the result based on format
            if ("json".equalsIgnoreCase(format)) {
                System.out.println(OutputFormatter.toJson(assignmentData, verbose));
            } else {
                System.out.println("Successfully reassigned work item to " + previousAssignee);
            }
            
            // Complete the operation
            metadataService.completeOperation(operationId, assignmentData);
            
            return 0;
        } catch (Exception e) {
            String errorMsg = "Error reverting assignment: " + e.getMessage();
            System.err.println("Error: " + errorMsg);
            if (verbose && !"json".equalsIgnoreCase(format)) {
                e.printStackTrace();
            }
            
            // Update tracking data with failure
            assignmentData.put("success", false);
            assignmentData.put("errorMessage", errorMsg);
            
            // Output the result in JSON format if requested
            if ("json".equalsIgnoreCase(format)) {
                System.out.println(OutputFormatter.toJson(assignmentData, verbose));
            }
            
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Undo a priority change.
     *
     * @param historyEntry the history entry
     * @param workItemId the work item ID
     * @param operationId the operation ID for tracking
     * @return exit code
     */
    private int undoPriorityChange(MockHistoryService.HistoryEntryRecord historyEntry, UUID workItemId, String operationId) {
        // Extract the priority content from the history entry
        String[] content = historyEntry.getContent().split(":");
        if (content.length > 1) {
            content = content[1].split("→");
        } else {
            content = historyEntry.getContent().split("→");
        }
        
        String previousPriority = content[0].trim();
        
        // Create priority change data for tracking
        Map<String, Object> priorityData = new HashMap<>();
        priorityData.put("action", "priority_revert");
        priorityData.put("workItemId", workItemId.toString());
        priorityData.put("priority", previousPriority);
        
        try {
            Priority priority = Priority.valueOf(previousPriority);
            itemService.updatePriority(workItemId, priority, username);
            
            // Update tracking data with success
            priorityData.put("success", true);
            
            // Output the result based on format
            if ("json".equalsIgnoreCase(format)) {
                System.out.println(OutputFormatter.toJson(priorityData, verbose));
            } else {
                System.out.println("Successfully reverted priority to " + priority);
            }
            
            // Complete the operation
            metadataService.completeOperation(operationId, priorityData);
            
            return 0;
        } catch (Exception e) {
            String errorMsg = "Error reverting priority: " + e.getMessage();
            System.err.println("Error: " + errorMsg);
            if (verbose && !"json".equalsIgnoreCase(format)) {
                e.printStackTrace();
            }
            
            // Update tracking data with failure
            priorityData.put("success", false);
            priorityData.put("errorMessage", errorMsg);
            
            // Output the result in JSON format if requested
            if ("json".equalsIgnoreCase(format)) {
                System.out.println(OutputFormatter.toJson(priorityData, verbose));
            }
            
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
}