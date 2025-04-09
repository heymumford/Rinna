/*
 * BacklogCommand - CLI command for managing developer backlogs
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.command.impl;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MockBacklogService;
import org.rinna.cli.service.MockItemService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Command implementation for managing developer backlogs.
 */
public class BacklogCommand {
    
    private final MockBacklogService backlogService;
    private final MockItemService itemService;
    
    /**
     * Creates a new BacklogCommand.
     *
     * @param backlogService the backlog service
     * @param itemService the item service
     */
    public BacklogCommand(MockBacklogService backlogService, MockItemService itemService) {
        this.backlogService = backlogService;
        this.itemService = itemService;
    }
    
    /**
     * Lists all items in the user's backlog.
     *
     * @return the command output
     */
    public String executeList() {
        // Get the current user
        String currentUser = System.getProperty("user.name", "system");
        
        // Get the user's backlog
        List<WorkItem> backlog = backlogService.getBacklog(currentUser);
        
        if (backlog.isEmpty()) {
            return "Your backlog is empty. Use 'rin backlog add <id>' to add items.";
        }
        
        StringBuilder output = new StringBuilder(100);
        output.append("Your backlog contains ").append(backlog.size()).append(" items:\n\n");
        
        for (int i = 0; i < backlog.size(); i++) {
            WorkItem item = backlog.get(i);
            output.append(formatBacklogItem(i + 1, item));
            
            if (i < backlog.size() - 1) {
                output.append('\n');
            }
        }
        
        return output.toString();
    }
    
    /**
     * Adds an item to the user's backlog.
     *
     * @param args the command arguments
     * @return the command output
     */
    public String executeAdd(String[] args) {
        if (args.length < 1) {
            return formatError("Missing work item ID", "backlog add <id>");
        }
        
        UUID id = parseItemId(args[0]);
        if (id == null) {
            return formatError("Invalid item ID format: " + args[0], "backlog add <id>");
        }
        
        // Get the current user
        String currentUser = System.getProperty("user.name", "system");
        
        try {
            // First check if the item exists
            WorkItem item = itemService.getItem(id.toString());
            if (item == null) {
                return formatError("Work item not found: " + args[0], "backlog add <id>");
            }
            
            // Add the item to the user's backlog
            boolean added = backlogService.addToBacklog(currentUser, item);
            if (!added) {
                return formatError("Failed to add item to backlog", "backlog add <id>");
            }
            
            return "Added to your backlog:\n\n" + formatItem(item);
        } catch (Exception e) {
            return formatError("Failed to add item to backlog: " + e.getMessage(), "backlog add <id>");
        }
    }
    
    /**
     * Removes an item from the user's backlog.
     *
     * @param args the command arguments
     * @return the command output
     */
    public String executeRemove(String[] args) {
        if (args.length < 1) {
            return formatError("Missing work item ID", "backlog remove <id>");
        }
        
        UUID id = parseItemId(args[0]);
        if (id == null) {
            return formatError("Invalid item ID format: " + args[0], "backlog remove <id>");
        }
        
        // Get the current user
        String currentUser = System.getProperty("user.name", "system");
        
        try {
            // Remove from backlog
            boolean removed = backlogService.removeFromBacklog(currentUser, id.toString());
            if (!removed) {
                return formatError("Item " + args[0] + " not found in your backlog", "backlog remove <id>");
            }
            
            return "Removed item " + args[0] + " from your backlog.";
        } catch (Exception e) {
            return formatError("Failed to remove item from backlog: " + e.getMessage(), "backlog remove <id>");
        }
    }
    
    /**
     * Moves an item to the top of the user's backlog.
     *
     * @param args the command arguments
     * @return the command output
     */
    public String executeTop(String[] args) {
        if (args.length < 1) {
            return formatError("Missing work item ID", "backlog top <id>");
        }
        
        UUID id = parseItemId(args[0]);
        if (id == null) {
            return formatError("Invalid item ID format: " + args[0], "backlog top <id>");
        }
        
        // Get the current user
        String currentUser = System.getProperty("user.name", "system");
        
        try {
            // First check if the item exists
            WorkItem item = itemService.getItem(id.toString());
            if (item == null) {
                return formatError("Work item not found: " + args[0], "backlog top <id>");
            }
            
            // Move the item to the top of the backlog
            boolean moved = backlogService.moveToTop(id);
            if (!moved) {
                return formatError("Failed to move item to top of backlog. Item " + args[0] + " may not be in your backlog.", 
                                  "backlog top <id>");
            }
            
            return "Moved item " + args[0] + " to the top of your backlog.";
        } catch (Exception e) {
            return formatError("Failed to move item to top of backlog: " + e.getMessage(), "backlog top <id>");
        }
    }
    
    /**
     * Moves an item up one position in the user's backlog.
     *
     * @param args the command arguments
     * @return the command output
     */
    public String moveUp(String[] args) {
        if (args.length < 1) {
            return formatError("Missing work item ID", "backlog move <id> up");
        }
        
        UUID id = parseItemId(args[0]);
        if (id == null) {
            return formatError("Invalid item ID format: " + args[0], "backlog move <id> up");
        }
        
        // Get the current user
        String currentUser = System.getProperty("user.name", "system");
        
        try {
            // First check if the item exists
            WorkItem item = itemService.getItem(id.toString());
            if (item == null) {
                return formatError("Work item not found: " + args[0], "backlog move <id> up");
            }
            
            // Move the item up in the backlog
            boolean moved = backlogService.moveUp(id);
            if (!moved) {
                return formatError("Failed to move item up in backlog. Item " + args[0] + 
                                  " may not be in your backlog or is already at the top.", 
                                  "backlog move <id> up");
            }
            
            return "Moved item " + args[0] + " up in your backlog.";
        } catch (Exception e) {
            return formatError("Failed to move item up in backlog: " + e.getMessage(), "backlog move <id> up");
        }
    }
    
    /**
     * Moves an item down one position in the user's backlog.
     *
     * @param args the command arguments
     * @return the command output
     */
    public String moveDown(String[] args) {
        if (args.length < 1) {
            return formatError("Missing work item ID", "backlog move <id> down");
        }
        
        UUID id = parseItemId(args[0]);
        if (id == null) {
            return formatError("Invalid item ID format: " + args[0], "backlog move <id> down");
        }
        
        // Get the current user
        String currentUser = System.getProperty("user.name", "system");
        
        try {
            // First check if the item exists
            WorkItem item = itemService.getItem(id.toString());
            if (item == null) {
                return formatError("Work item not found: " + args[0], "backlog move <id> down");
            }
            
            // Move the item down in the backlog
            boolean moved = backlogService.moveDown(id);
            if (!moved) {
                return formatError("Failed to move item down in backlog. Item " + args[0] + 
                                  " may not be in your backlog or is already at the bottom.", 
                                  "backlog move <id> down");
            }
            
            return "Moved item " + args[0] + " down in your backlog.";
        } catch (Exception e) {
            return formatError("Failed to move item down in backlog: " + e.getMessage(), "backlog move <id> down");
        }
    }
    
    /**
     * Moves an item to the bottom of the user's backlog.
     *
     * @param args the command arguments
     * @return the command output
     */
    public String executeBottom(String[] args) {
        if (args.length < 1) {
            return formatError("Missing work item ID", "backlog bottom <id>");
        }
        
        UUID id = parseItemId(args[0]);
        if (id == null) {
            return formatError("Invalid item ID format: " + args[0], "backlog bottom <id>");
        }
        
        // Get the current user
        String currentUser = System.getProperty("user.name", "system");
        
        try {
            // First check if the item exists
            WorkItem item = itemService.getItem(id.toString());
            if (item == null) {
                return formatError("Work item not found: " + args[0], "backlog bottom <id>");
            }
            
            // Move the item to the bottom of the backlog
            boolean moved = backlogService.moveToBottom(id);
            if (!moved) {
                return formatError("Failed to move item to bottom of backlog. Item " + args[0] + 
                                  " may not be in your backlog.", 
                                  "backlog bottom <id>");
            }
            
            return "Moved item " + args[0] + " to the bottom of your backlog.";
        } catch (Exception e) {
            return formatError("Failed to move item to bottom of backlog: " + e.getMessage(), "backlog bottom <id>");
        }
    }
    
    /**
     * Creates a new work item in the user's backlog.
     *
     * @param args the command arguments
     * @return the command output
     */
    public String executeCreate(String[] args) {
        if (args.length < 1) {
            return formatError("Missing work item title", "backlog create <title>");
        }
        
        // Join all arguments as the title
        String title = String.join(" ", args);
        
        // Get the current user
        String currentUser = System.getProperty("user.name", "system");
        
        try {
            // Create a new work item
            WorkItem item = new WorkItem();
            item.setId(UUID.randomUUID());
            item.setTitle(title);
            item.setType(WorkItemType.TASK); // Default type is TASK
            item.setStatus(WorkflowState.CREATED); // Start in CREATED state
            item.setPriority(Priority.MEDIUM); // Default priority is MEDIUM
            item.setAssignee(currentUser); // Assign to current user
            item.setReporter(currentUser); // Reporter is current user
            
            // Save the item
            WorkItem createdItem = itemService.createItem(item);
            
            // Add to backlog
            boolean added = backlogService.addToBacklog(currentUser, createdItem);
            if (!added) {
                return formatError("Failed to add item to backlog", "backlog create <title>");
            }
            
            return "Created new task in your backlog:\n\n" + formatItem(createdItem);
        } catch (Exception e) {
            return formatError("Failed to create work item: " + e.getMessage(), "backlog create <title>");
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
        // No need to catch NPE explicitly since we've added null check above
    }
    
    /**
     * Formats a backlog item for display.
     *
     * @param position the position in the backlog
     * @param item the work item
     * @return formatted item string
     */
    private String formatBacklogItem(int position, WorkItem item) {
        StringBuilder output = new StringBuilder(80);
        output.append(position).append(". ");
        output.append('[').append(item.getType()).append("] ");
        output.append(item.getTitle());
        
        if (item.getPriority() != null) {
            output.append(" (").append(item.getPriority()).append(')');
        }
        
        return output.toString();
    }
    
    /**
     * Formats an item for display.
     *
     * @param item the work item
     * @return formatted item string
     */
    private String formatItem(WorkItem item) {
        StringBuilder output = new StringBuilder(200);
        
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
               "Example: rin " + usage.replace("<id>", "WI-123").replace("<title>", "Fix login bug");
    }
}