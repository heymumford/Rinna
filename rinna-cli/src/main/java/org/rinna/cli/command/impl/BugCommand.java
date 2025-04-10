/*
 * BugCommand - CLI command handler for bug creation and management
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.command.impl;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MockBacklogService;
import org.rinna.cli.service.MockItemService;

/**
 * Command handler for bug creation and management.
 */
public class BugCommand {

    private final MockItemService itemService;
    private final MockBacklogService backlogService;

    /**
     * Creates a new BugCommand with the specified services.
     *
     * @param itemService the item service to create bugs
     * @param backlogService the backlog service to add bugs to backlog
     */
    public BugCommand(MockItemService itemService, MockBacklogService backlogService) {
        this.itemService = itemService;
        this.backlogService = backlogService;
    }

    /**
     * Executes the bug command to create a new bug report.
     *
     * @param args Command arguments
     * @return A formatted response
     */
    public String execute(String[] args) {
        if (args.length < 1) {
            return formatError("Missing bug title", "bug <title>");
        }

        // Defaults
        Priority priority = Priority.MEDIUM;
        boolean addToBacklog = false;
        String summary = String.join(" ", args);
        
        // Process flags if present
        int i = 0;
        while (i < args.length) {
            if ("-p".equals(args[i]) || "--priority".equals(args[i])) {
                if (i + 1 < args.length) {
                    try {
                        priority = Priority.valueOf(args[i + 1].toUpperCase());
                        String flagValue = args[i] + " " + args[i + 1];
                        // Remove priority flag and value from summary
                        summary = summary.replace(flagValue, "").trim();
                        // Increment by 2 to skip both flag and value
                        i += 2;
                    } catch (IllegalArgumentException e) {
                        return formatError("Invalid priority level: " + args[i + 1], 
                                          "bug -p|--priority {LOW|MEDIUM|HIGH|CRITICAL} <title>");
                    }
                } else {
                    return formatError("Missing priority level after " + args[i], 
                                      "bug -p|--priority {LOW|MEDIUM|HIGH|CRITICAL} <title>");
                }
            } else if ("-b".equals(args[i]) || "--backlog".equals(args[i])) {
                addToBacklog = true;
                
                // Remove backlog flag from summary
                summary = summary.replace(args[i], "").trim();
                i++;
            } else {
                i++;
            }
        }
        
        try {
            // Create a new bug work item
            WorkItem bug = new WorkItem();
            UUID id = UUID.randomUUID();
            bug.setId(id);
            bug.setTitle(summary);
            bug.setType(WorkItemType.BUG);
            bug.setPriority(priority);
            bug.setStatus(WorkflowState.FOUND);
            bug.setDescription("New bug report: " + summary);
            
            // Set the reporter to the current user
            String currentUser = System.getProperty("user.name");
            bug.setReporter(currentUser);
            
            // Save the bug using the item service
            WorkItem createdBug = itemService.createItem(bug);
            
            // Add custom fields/metadata 
            Map<String, String> metadata = new HashMap<>();
            metadata.put("reported_date", java.time.LocalDateTime.now().toString());
            metadata.put("reporter", currentUser);
            metadata.put("bug_type", "functional");
            itemService.updateCustomFields(createdBug.getId(), metadata);
            
            // Add to backlog if requested
            if (addToBacklog) {
                backlogService.addToBacklog(currentUser, createdBug);
            }
            
            return formatBugCreated(createdBug, addToBacklog);
        } catch (Exception e) {
            return formatError("Failed to create bug: " + e.getMessage(), "bug <title>");
        }
    }
    
    
    /**
     * Retrieves details about a specific bug.
     *
     * @param args Command arguments
     * @return Formatted bug details
     */
    public String executeShow(String[] args) {
        if (args.length < 1) {
            return formatError("Missing bug ID", "bug show <id>");
        }
        
        UUID id = parseItemId(args[0]);
        if (id == null) {
            return formatError("Invalid bug ID format", "bug show <id>");
        }
        
        try {
            // Fetch the bug from the item service
            WorkItem bug = itemService.getItem(id.toString());
            
            if (bug == null) {
                return formatError("Bug not found: " + args[0], "bug show <id>");
            }
            
            // Verify this is actually a bug type work item
            if (bug.getType() != WorkItemType.BUG) {
                return formatError("Work item " + args[0] + " is not a bug", "bug show <id>");
            }
            
            return formatBugDetails(bug);
        } catch (Exception e) {
            return formatError("Failed to retrieve bug: " + e.getMessage(), "bug show <id>");
        }
    }
    
    /**
     * Parses an item ID string to a UUID.
     *
     * @param itemId the item ID string (e.g., "WI-123" or a UUID)
     * @return the UUID, or null if invalid
     */
    private UUID parseItemId(String itemId) {
        if (itemId == null) {
            return null;
        }
        
        // Check if it's a direct UUID
        try {
            return UUID.fromString(itemId);
        } catch (IllegalArgumentException e) {
            // Not a direct UUID, try to parse as a work item ID (e.g., "WI-123")
            if (itemId.startsWith("WI-") || itemId.startsWith("BUG-")) {
                String prefix = itemId.substring(0, itemId.indexOf('-') + 1);
                String idPart = itemId.substring(prefix.length());
                
                try {
                    // Try to get the real item by short id from the item service
                    WorkItem item = itemService.findItemByShortId(itemId);
                    if (item != null) {
                        return UUID.fromString(item.getId());
                    }
                } catch (Exception ex) {
                    // Fall back to deterministic UUID if item service lookup fails
                }
                
                // Deterministic UUID based on the ID as fallback
                return UUID.nameUUIDFromBytes(
                    ("work-item-" + idPart).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            
            // Handle bug-specific IDs
            if (itemId.startsWith("B-")) {
                String idPart = itemId.substring(2);
                return UUID.nameUUIDFromBytes(
                    ("bug-" + idPart).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            }
            
            return null;
        }
    }
    
    /**
     * Formats the bug creation success message.
     * 
     * @param bug The created bug work item
     * @param addedToBacklog Whether the bug was added to backlog
     * @return Formatted success message
     */
    private String formatBugCreated(WorkItem bug, boolean addedToBacklog) {
        StringBuilder result = new StringBuilder(200);
        result.append("Bug created successfully:\n\n");
        
        result.append("ID: ").append(bug.getId()).append('\n');
        result.append("Title: ").append(bug.getTitle()).append('\n');
        result.append("Priority: ").append(bug.getPriority()).append('\n');
        result.append("Status: ").append(bug.getStatus()).append('\n');
        
        if (addedToBacklog) {
            result.append('\n').append("Bug has been added to your backlog.");
        } else {
            result.append('\n').append("Tip: Use 'rin backlog add ").append(bug.getId())
                  .append("' to add this bug to your backlog.");
        }
        
        return result.toString();
    }
    
    /**
     * Formats detailed bug information.
     * 
     * @param bug The bug work item
     * @return Formatted bug details
     */
    private String formatBugDetails(WorkItem bug) {
        StringBuilder result = new StringBuilder(200);
        result.append("Bug Details:\n\n");
        
        result.append("ID: ").append(bug.getId()).append('\n');
        result.append("Title: ").append(bug.getTitle()).append('\n');
        result.append("Priority: ").append(bug.getPriority()).append('\n');
        result.append("Status: ").append(bug.getStatus()).append('\n');
        result.append("Description: ").append(bug.getDescription()).append('\n');
        
        return result.toString();
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
               "Example: rin " + usage.replace("<title>", "Login form submits twice")
                                    .replace("<id>", "WI-123");
    }
}