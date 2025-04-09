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
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockCommentService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Command to add a comment to a work item.
 * Follows the ViewCommand pattern with operation tracking.
 */
public class CommentCommand implements Callable<Integer> {
    // Command parameters
    private String username;
    private String commentText;
    private UUID itemId;
    private String format = "text"; // Output format (text or json)
    private boolean verbose = false;
    
    // Service dependencies
    private final ServiceManager serviceManager;
    private final MockCommentService commentService;
    private final MockWorkflowService workflowService;
    private final MockItemService itemService;
    private final MetadataService metadataService;
    private final ContextManager contextManager;
    
    /**
     * Constructs a new comment command with the default service manager.
     */
    public CommentCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Constructs a new comment command with the provided service manager.
     * This constructor allows for dependency injection, making the command more testable.
     * 
     * @param serviceManager the service manager to use
     */
    public CommentCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.commentService = serviceManager.getMockCommentService();
        this.workflowService = serviceManager.getMockWorkflowService();
        this.itemService = serviceManager.getMockItemService();
        this.metadataService = serviceManager.getMetadataService();
        this.contextManager = ContextManager.getInstance();
        this.username = System.getProperty("user.name");
    }
    
    /**
     * Executes the command with proper operation tracking.
     */
    @Override
    public Integer call() {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        params.put("format", format);
        params.put("verbose", verbose);
        
        if (itemId != null) {
            params.put("itemId", itemId.toString());
        }
        
        if (commentText != null) {
            params.put("hasComment", true);
            params.put("commentLength", commentText.length());
        }
        
        // Start tracking main operation
        String operationId = metadataService.startOperation("comment", "CREATE", params);
        
        try {
            // 1. Validate comment text
            validateComment(operationId);
            
            // 2. Resolve the item ID if not specified
            resolveItemId(operationId);
            
            // 3. Create a sub-operation for adding the comment
            Map<String, Object> addParams = new HashMap<>();
            addParams.put("itemId", itemId.toString());
            addParams.put("username", username);
            String addOpId = metadataService.startOperation("comment-add", "CREATE", addParams);
            
            try {
                // 4. Add the comment
                commentService.addComment(itemId, username, commentText);
                
                // 5. Record success
                Map<String, Object> addResult = new HashMap<>();
                addResult.put("success", true);
                addResult.put("itemId", itemId.toString());
                metadataService.completeOperation(addOpId, addResult);
                
                // 6. Output the result
                boolean isJsonOutput = "json".equalsIgnoreCase(format);
                
                // Record the item in context for future commands
                contextManager.setCurrentItemId(itemId.toString());
                
                int resultCode;
                if (verbose) {
                    // Display all comments for the item
                    resultCode = displayDetailedResult(itemId, isJsonOutput, operationId);
                } else {
                    // Display simple success message
                    resultCode = displaySimpleResult(itemId, isJsonOutput, operationId);
                }
                
                return resultCode;
            } catch (Exception e) {
                metadataService.failOperation(addOpId, e);
                throw e;
            }
        } catch (IllegalArgumentException e) {
            return handleError("Invalid work item ID - " + e.getMessage(), e, operationId);
        } catch (Exception e) {
            return handleError("Failed to add comment - " + e.getMessage(), e, operationId);
        }
    }
    
    /**
     * Validates the comment text.
     * 
     * @param operationId the operation ID for tracking
     * @throws IllegalArgumentException if the comment text is missing or empty
     */
    private void validateComment(String operationId) throws IllegalArgumentException {
        if (commentText == null || commentText.isBlank()) {
            IllegalArgumentException e = new IllegalArgumentException("Comment text is required");
            metadataService.failOperation(operationId, e);
            throw e;
        }
    }
    
    /**
     * Resolves the item ID if not specified.
     * 
     * @param operationId the operation ID for tracking
     * @throws IllegalArgumentException if no work item is currently in progress
     */
    private void resolveItemId(String operationId) throws IllegalArgumentException {
        if (itemId == null) {
            Map<String, Object> resolveParams = new HashMap<>();
            resolveParams.put("username", username);
            String resolveOpId = metadataService.startOperation("comment-resolve-item", "READ", resolveParams);
            
            try {
                Optional<WorkItem> currentWip = workflowService.getCurrentWorkInProgress(username);
                
                if (currentWip.isEmpty()) {
                    IllegalArgumentException e = new IllegalArgumentException("No work item is currently in progress");
                    metadataService.failOperation(resolveOpId, e);
                    throw e;
                }
                
                itemId = UUID.fromString(currentWip.get().getId());
                
                Map<String, Object> resolveResult = new HashMap<>();
                resolveResult.put("itemId", itemId.toString());
                resolveResult.put("itemTitle", currentWip.get().getTitle());
                metadataService.completeOperation(resolveOpId, resolveResult);
                
                if (verbose) {
                    System.out.println("Using current work item: " + itemId);
                }
            } catch (Exception e) {
                metadataService.failOperation(resolveOpId, e);
                throw e;
            }
        }
    }
    
    /**
     * Handles errors with appropriate output based on verbose setting.
     * 
     * @param message the error message
     * @param e the exception
     * @param operationId the operation ID for tracking
     * @return the exit code
     */
    private int handleError(String message, Exception e, String operationId) {
        // Record failure
        if (operationId != null) {
            metadataService.failOperation(operationId, e);
        }
        
        boolean isJsonOutput = "json".equalsIgnoreCase(format);
        
        if (isJsonOutput) {
            OutputFormatter formatter = new OutputFormatter(true);
            Map<String, Object> error = new HashMap<>();
            error.put("error", message);
            error.put("message", e.getMessage());
            formatter.outputObject("error", error);
        } else {
            System.err.println("Error: " + message);
            if (verbose) {
                e.printStackTrace();
            }
        }
        
        return 1;
    }
    
    /**
     * Displays a simple success message.
     * 
     * @param itemId the work item ID
     * @param isJsonOutput whether to output in JSON format
     * @param operationId the operation ID for tracking
     * @return the exit code
     */
    private int displaySimpleResult(UUID itemId, boolean isJsonOutput, String operationId) {
        if (isJsonOutput) {
            OutputFormatter formatter = new OutputFormatter(true);
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("workItemId", itemId.toString());
            result.put("action", "comment_added");
            result.put("username", username);
            
            formatter.outputObject("comment", result);
        } else {
            System.out.println("Comment added to work item " + itemId);
        }
        
        // Record operation success
        Map<String, Object> finalResult = new HashMap<>();
        finalResult.put("success", true);
        finalResult.put("itemId", itemId.toString());
        metadataService.completeOperation(operationId, finalResult);
        
        return 0;
    }
    
    /**
     * Displays detailed information including all comments.
     * 
     * @param itemId the work item ID
     * @param isJsonOutput whether to output in JSON format
     * @param operationId the operation ID for tracking
     * @return the exit code
     */
    private int displayDetailedResult(UUID itemId, boolean isJsonOutput, String operationId) {
        try {
            // Create a sub-operation for listing comments
            Map<String, Object> listParams = new HashMap<>();
            listParams.put("itemId", itemId.toString());
            String listOpId = metadataService.startOperation("comment-list", "READ", listParams);
            
            try {
                List<? extends Comment> comments = commentService.getComments(itemId);
                
                Map<String, Object> listResult = new HashMap<>();
                listResult.put("commentCount", comments.size());
                metadataService.completeOperation(listOpId, listResult);
                
                if (isJsonOutput) {
                    displayCommentsAsJson(itemId, comments);
                } else {
                    displayCommentsAsText(itemId, comments);
                }
                
                // Record operation success
                Map<String, Object> finalResult = new HashMap<>();
                finalResult.put("success", true);
                finalResult.put("itemId", itemId.toString());
                finalResult.put("commentCount", comments.size());
                metadataService.completeOperation(operationId, finalResult);
                
                return 0;
            } catch (Exception e) {
                metadataService.failOperation(listOpId, e);
                return handleError("Error retrieving comments: " + e.getMessage(), e, operationId);
            }
        } catch (Exception e) {
            return handleError("Error retrieving comments: " + e.getMessage(), e, operationId);
        }
    }
    
    /**
     * Displays comments in JSON format.
     * 
     * @param itemId the work item ID
     * @param comments the list of comments
     */
    private void displayCommentsAsJson(UUID itemId, List<? extends Comment> comments) {
        OutputFormatter formatter = new OutputFormatter(true);
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
        
        formatter.outputObject("comments", result);
    }
    
    /**
     * Displays comments in text format.
     * 
     * @param itemId the work item ID
     * @param comments the list of comments
     */
    private void displayCommentsAsText(UUID itemId, List<? extends Comment> comments) {
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
    }
    
    /**
     * Sets the comment text.
     *
     * @param comment the comment text
     * @return this command for chaining
     */
    public CommentCommand setComment(String comment) {
        this.commentText = comment;
        return this;
    }
    
    /**
     * Sets the work item ID to comment on.
     * If not set, the comment will be added to the current work item in progress.
     *
     * @param itemId the work item ID
     * @return this command for chaining
     */
    public CommentCommand setItemId(String itemId) {
        try {
            this.itemId = UUID.fromString(itemId);
        } catch (IllegalArgumentException e) {
            // Check if this is a short ID format (e.g., "WI-123")
            if (itemId != null && itemId.contains("-")) {
                try {
                    // Attempt to get the full ID from the service using the short ID
                    String shortId = itemId;
                    WorkItem workItem = itemService.findItemByShortId(shortId);
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
        return this;
    }
    
    /**
     * Sets the user adding the comment.
     *
     * @param username the user name
     * @return this command for chaining
     */
    public CommentCommand setUsername(String username) {
        this.username = username;
        return this;
    }

    /**
     * Sets the output format.
     *
     * @param format the output format ("text" or "json")
     * @return this command for chaining
     */
    public CommentCommand setFormat(String format) {
        this.format = format;
        return this;
    }

    /**
     * Sets the output format to JSON.
     * This is a legacy setter that maps to the format parameter.
     *
     * @param jsonOutput true for JSON output
     * @return this command for chaining
     */
    public CommentCommand setJsonOutput(boolean jsonOutput) {
        if (jsonOutput) {
            this.format = "json";
        }
        return this;
    }

    /**
     * Sets verbose output mode.
     *
     * @param verbose true for verbose output
     * @return this command for chaining
     */
    public CommentCommand setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }
}
