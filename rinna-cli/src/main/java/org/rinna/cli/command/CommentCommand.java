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

import org.rinna.cli.domain.model.Comment;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.service.MockCommentService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.ModelMapper;
import org.rinna.cli.util.OutputFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Command to add a comment to the current work item in progress.
 */
public class CommentCommand implements Callable<Integer> {
    private String user;
    private String comment;
    private UUID itemId;
    private boolean jsonOutput = false;
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final MockCommentService commentService;
    private final MockWorkflowService workflowService;
    
    /**
     * Constructor initializing the service manager.
     */
    public CommentCommand() {
        this.serviceManager = ServiceManager.getInstance();
        this.commentService = serviceManager.getMockCommentService();
        this.workflowService = serviceManager.getMockWorkflowService();
        this.user = System.getProperty("user.name");
    }
    
    /**
     * Sets the comment text.
     *
     * @param comment the comment text
     */
    public void setComment(String comment) {
        this.comment = comment;
    }
    
    /**
     * Sets the work item ID to comment on.
     * If not set, the comment will be added to the current work item in progress.
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
                    WorkItem workItem = serviceManager.getMockItemService().findItemByShortId(shortId);
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
     * Sets the user adding the comment.
     *
     * @param user the user name
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Sets the output format to JSON.
     *
     * @param jsonOutput true for JSON output
     */
    public void setJsonOutput(boolean jsonOutput) {
        this.jsonOutput = jsonOutput;
    }

    /**
     * Sets verbose output mode.
     *
     * @param verbose true for verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    @Override
    public Integer call() {
        try {
            // Track operation metadata
            Map<String, Object> operationParams = new HashMap<>();
            operationParams.put("user", user);
            if (itemId != null) operationParams.put("itemId", itemId.toString());
            if (comment != null) operationParams.put("hasComment", true);
            
            serviceManager.getMetadataService().trackOperation("comment", operationParams);
            
            // Validate comment text
            if (comment == null || comment.isBlank()) {
                System.err.println("Error: Comment text is required");
                return 1;
            }
            
            // Get item ID if not specified
            if (itemId == null) {
                Optional<WorkItem> currentWip = workflowService.getCurrentWorkInProgress(user);
                
                if (currentWip.isEmpty()) {
                    System.err.println("Error: No work item is currently in progress");
                    System.err.println("Tip: Use 'rin list --status=IN_PROGRESS' to see in-progress items");
                    System.err.println("     Use 'rin comment <item-id> <comment>' to comment on a specific item");
                    return 1;
                }
                
                itemId = UUID.fromString(currentWip.get().getId());
                if (verbose) {
                    System.out.println("Using current work item: " + itemId);
                }
            }
            
            // Add the comment
            commentService.addComment(itemId, user, comment);
            
            // Output the result
            if (verbose) {
                return outputDetailedResult(itemId);
            } else {
                return outputSimpleResult(itemId);
            }
            
        } catch (IllegalArgumentException e) {
            return handleError("Invalid work item ID - " + e.getMessage(), e);
        } catch (Exception e) {
            return handleError("Failed to add comment - " + e.getMessage(), e);
        }
    }
    
    /**
     * Handles errors with appropriate output based on verbose setting.
     * 
     * @param message the error message
     * @param e the exception
     * @return the exit code
     */
    private int handleError(String message, Exception e) {
        System.err.println("Error: " + message);
        if (verbose) {
            e.printStackTrace();
        }
        return 1;
    }
    
    /**
     * Outputs a simple success message.
     * 
     * @param itemId the work item ID
     * @return the exit code
     */
    private int outputSimpleResult(UUID itemId) {
        if (jsonOutput) {
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("workItemId", itemId.toString());
            result.put("action", "comment_added");
            result.put("user", user);
            
            System.out.println(OutputFormatter.toJson(result));
        } else {
            System.out.println("Comment added to work item " + itemId);
        }
        return 0;
    }
    
    /**
     * Outputs detailed information including all comments.
     * 
     * @param itemId the work item ID
     * @return the exit code
     */
    private int outputDetailedResult(UUID itemId) {
        try {
            List<? extends Comment> comments = commentService.getComments(itemId);
            
            if (jsonOutput) {
                return outputJsonComments(itemId, comments);
            } else {
                return outputTextComments(itemId, comments);
            }
        } catch (Exception e) {
            return handleError("Error retrieving comments: " + e.getMessage(), e);
        }
    }
    
    /**
     * Outputs comments in JSON format.
     * 
     * @param itemId the work item ID
     * @param comments the list of comments
     * @return the exit code
     */
    private int outputJsonComments(UUID itemId, List<? extends Comment> comments) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("workItemId", itemId.toString());
        result.put("action", "comment_added");
        result.put("commentCount", comments.size());
        
        // Convert comments to map for JSON output
        List<Map<String, Object>> commentsList = comments.stream()
            .map(comment -> {
                Map<String, Object> map = new HashMap<>();
                map.put("id", comment.id().toString());
                map.put("user", comment.user());
                map.put("timestamp", comment.timestamp().toString());
                map.put("text", comment.text());
                map.put("type", comment.type().toString());
                return map;
            })
            .toList();
        
        result.put("comments", commentsList);
        
        System.out.println(OutputFormatter.toJson(result));
        return 0;
    }
    
    /**
     * Outputs comments in text format.
     * 
     * @param itemId the work item ID
     * @param comments the list of comments
     * @return the exit code
     */
    private int outputTextComments(UUID itemId, List<? extends Comment> comments) {
        System.out.println("\nComments for work item " + itemId + ":");
        System.out.println("----------------------------------------");
        
        if (comments.isEmpty()) {
            System.out.println("No comments found.");
        } else {
            for (Comment comment : comments) {
                System.out.printf("[%s] %s (%s):%n", 
                    comment.timestamp().toString().replace("T", " ").substring(0, 19),
                    comment.user(),
                    comment.type().toString().toLowerCase());
                System.out.println(comment.text());
                System.out.println("----------------------------------------");
            }
        }
        return 0;
    }
}
