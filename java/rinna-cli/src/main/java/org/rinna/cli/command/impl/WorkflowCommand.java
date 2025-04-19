/*
 * WorkflowCommand - CLI command handler for transition workflow commands
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.command.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;

/**
 * Command handler for workflow transition commands such as 'test' and 'done'.
 */
public class WorkflowCommand {
    
    private final MockItemService itemService;
    private final MockWorkflowService workflowService;
    
    /**
     * Creates a new WorkflowCommand with the specified services.
     *
     * @param itemService the item service to access work items
     * @param workflowService the workflow service to transition items
     */
    public WorkflowCommand(MockItemService itemService, MockWorkflowService workflowService) {
        this.itemService = itemService;
        this.workflowService = workflowService;
    }
    
    /**
     * Handles the 'test' command to transition a work item to IN_TEST status.
     *
     * @param args the command line arguments
     * @return the command output
     */
    public String executeTest(String[] args) {
        if (args.length < 1) {
            return formatError("Missing work item ID", "test <id>");
        }
        
        UUID id = parseItemId(args[0]);
        if (id == null) {
            return formatError("Invalid item ID format: " + args[0], "test <id>");
        }
        
        // Get the work item
        WorkItem item = itemService.getItem(id.toString());
        if (item == null) {
            return formatError("Work item not found: " + args[0], "test <id>");
        }
        
        try {
            // Get the current user
            String currentUser = System.getProperty("user.name", "system");
            
            // Create a comment for the transition
            String comment = "Moved to testing";
            if (args.length > 1) {
                // If additional args are provided, use them as a tester name
                String tester = args[1];
                comment += " (Tester: " + tester + ")";
                
                // Store tester info in custom fields
                Map<String, String> customFields = new HashMap<>();
                customFields.put("tester", tester);
                customFields.put("lastTestDate", java.time.LocalDate.now().toString());
                itemService.updateCustomFields(id.toString(), customFields);
            }
            
            // Transition the work item
            item = workflowService.transition(
                id.toString(),
                currentUser,
                WorkflowState.IN_TEST,
                comment
            );
            
            return formatSuccessTransition(item, WorkflowState.IN_TEST);
        } catch (Exception e) {
            return formatError("Failed to transition work item: " + e.getMessage(), "test <id>");
        }
    }
    
    /**
     * Handles the 'done' command to transition a work item to DONE status.
     *
     * @param args the command line arguments
     * @return the command output
     */
    public String executeDone(String[] args) {
        if (args.length < 1) {
            return formatError("Missing work item ID", "done <id>");
        }
        
        UUID id = parseItemId(args[0]);
        if (id == null) {
            return formatError("Invalid item ID format: " + args[0], "done <id>");
        }
        
        // Get the work item
        WorkItem item = itemService.getItem(id.toString());
        if (item == null) {
            return formatError("Work item not found: " + args[0], "done <id>");
        }
        
        try {
            // Get the current user
            String currentUser = System.getProperty("user.name", "system");
            
            // Create a comment for the transition
            String comment = "Completed work";
            if (args.length > 1) {
                comment += " (" + String.join(" ", Arrays.copyOfRange(args, 1, args.length)) + ")";
            }
            
            // Transition the work item
            item = workflowService.transition(
                id.toString(),
                currentUser,
                WorkflowState.DONE,
                comment
            );
            
            String output = formatSuccessTransition(item, WorkflowState.DONE);
            
            // Add a congratulatory message for bugs
            if (item.getType() == WorkItemType.BUG) {
                output = output.concat("\nCongratulations on fixing this bug! 🎉");
            }
            
            return output;
        } catch (Exception e) {
            return formatError("Failed to transition work item: " + e.getMessage(), "done <id>");
        }
    }
    
    
    /**
     * Parses an item ID string to a UUID.
     *
     * @param itemId the item ID string (e.g., "WI-123" or a UUID)
     * @return the UUID, or null if invalid
     */
    private UUID parseItemId(String itemId) {
        // Check if it's a direct UUID
        try {
            return UUID.fromString(itemId);
        } catch (IllegalArgumentException e) {
            // Not a direct UUID, try to parse as a work item ID (e.g., "WI-123")
            if (itemId != null && itemId.startsWith("WI-")) {
                String idPart = itemId.substring(3);
                // In a real implementation, we would look up the UUID by the work item ID
                // Here we'll mock it by creating a deterministic UUID based on the ID
                return UUID.nameUUIDFromBytes(("work-item-" + idPart).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            return null;
        }
    }
    
    /**
     * Formats a success message for a workflow transition.
     *
     * @param item the updated work item
     * @param targetState the state the item was transitioned to
     * @return formatted success message
     */
    private String formatSuccessTransition(WorkItem item, WorkflowState targetState) {
        StringBuilder output = new StringBuilder(150);
        output.append("Updated work item: Status changed to ").append(targetState).append('\n').append('\n');
        
        output.append("ID: ").append(item.getId()).append('\n');
        output.append("Title: ").append(item.getTitle()).append('\n');
        output.append("Type: ").append(item.getType()).append('\n');
        output.append("Status: ").append(item.getStatus()).append('\n');
        output.append("Priority: ").append(item.getPriority()).append('\n');
        
        if (item.getAssignee() != null && !item.getAssignee().isEmpty()) {
            output.append("Assigned to: ").append(item.getAssignee()).append('\n');
        }
        
        return output.toString();
    }
    
    /**
     * Formats an error message.
     *
     * @param message the error message
     * @param usage the usage example
     * @return formatted error message
     */
    private String formatError(String message, String usage) {
        return "Error: " + message + "\n\n" +
               "Usage: rin " + usage + "\n\n" +
               "Example: rin " + usage.replace("<id>", "WI-123");
    }
}