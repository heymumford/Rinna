/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.util;

import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.concurrent.Callable;

/**
 * Example command implementation showing how to use the new operation tracking utilities.
 * This class demonstrates best practices for implementing commands with the new utilities.
 */
public class CommandExample implements Callable<Integer> {
    
    private final ServiceManager serviceManager;
    private final MetadataService metadataService;
    private final OperationTracker tracker;
    private final ErrorHandler errorHandler;
    
    private String operation;
    private String[] args = new String[0];
    private String format = "text";
    private boolean verbose = false;
    
    /**
     * Creates a new example command with the specified ServiceManager.
     *
     * @param serviceManager the service manager
     */
    public CommandExample(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.metadataService = serviceManager.getMetadataService();
        
        // Initialize the operation tracker and error handler
        this.tracker = new OperationTracker(metadataService);
        this.errorHandler = new ErrorHandler(metadataService);
    }
    
    /**
     * Sets the operation to perform.
     *
     * @param operation the operation
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    /**
     * Sets whether to use JSON output format.
     *
     * @param jsonOutput true to use JSON output
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
     * Sets whether to display verbose output.
     *
     * @param verbose true for verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    /**
     * Sets the arguments for the operation.
     *
     * @param args the arguments
     */
    public void setArgs(String[] args) {
        this.args = args;
    }
    
    @Override
    public Integer call() {
        // Set up the error handler with the current configuration
        errorHandler.verbose(verbose)
                   .outputFormat(format);
        
        // Set up the operation tracker with the command name and parameters
        tracker.command("example-command")
              .param("operation", operation != null ? operation : "help")
              .param("format", format)
              .param("verbose", verbose);
        
        if (args.length > 0) {
            tracker.param("argsCount", args.length)
                  .param("arg0", args[0]);
        }
        
        try {
            // Start tracking the main operation
            String operationId = tracker.start();
            
            // Delegate to the appropriate operation
            switch (operation) {
                case "simpleOperation":
                    return handleSimpleOperation(operationId);
                    
                case "nestedOperation":
                    return handleNestedOperation(operationId);
                    
                case "batchOperation":
                    return handleBatchOperation(operationId);
                    
                case "help":
                default:
                    displayHelp(operationId);
                    return 0;
            }
        } catch (Exception e) {
            // Unexpected error that wasn't caught by specific handlers
            return errorHandler.handleUnexpectedError("unknown", "example-command", e);
        }
    }
    
    /**
     * Handles a simple operation using the operation tracker.
     *
     * @param parentOperationId the parent operation ID
     * @return the exit code
     */
    private int handleSimpleOperation(String parentOperationId) {
        // Create a sub-tracker for this operation
        OperationTracker subTracker = tracker.createSubTracker("example-command-simple");
        
        try {
            // Execute the operation with the tracker (success case)
            Map<String, Object> result = subTracker
                .param("simpleParam", "value")
                .execute(() -> {
                    // Simulate some work...
                    System.out.println("Executing simple operation...");
                    
                    // Return a result
                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "success");
                    data.put("message", "Simple operation completed successfully");
                    return data;
                });
            
            // Complete the parent operation
            Map<String, Object> parentResult = new HashMap<>();
            parentResult.put("success", true);
            parentResult.put("operation", "simpleOperation");
            parentResult.put("result", result);
            
            return errorHandler.handleSuccess(parentOperationId, parentResult);
        } catch (Exception e) {
            // Handle the error
            return errorHandler.handleError(parentOperationId, "example-command", 
                "Failed to execute simple operation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handles a nested operation with hierarchical tracking.
     *
     * @param parentOperationId the parent operation ID
     * @return the exit code
     */
    private int handleNestedOperation(String parentOperationId) {
        // Create a sub-tracker for this operation
        OperationTracker subTracker = tracker.createSubTracker("example-command-nested");
        
        try {
            // Execute the first sub-operation
            Map<String, Object> firstResult = subTracker
                .param("step", "first")
                .execute(() -> {
                    System.out.println("Executing first step...");
                    
                    // Create a nested sub-tracker for an even deeper operation
                    OperationTracker nestedTracker = subTracker.createSubTracker("example-command-nested-deep");
                    
                    // Execute the deep nested operation
                    Map<String, Object> deepResult = nestedTracker
                        .param("depth", "deep")
                        .execute(() -> {
                            System.out.println("Executing deep nested operation...");
                            
                            Map<String, Object> data = new HashMap<>();
                            data.put("status", "success");
                            data.put("depth", "deep");
                            return data;
                        });
                    
                    // Return first step result including the deep result
                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "success");
                    data.put("step", "first");
                    data.put("deepResult", deepResult);
                    return data;
                });
            
            // Execute the second sub-operation
            Map<String, Object> secondResult = subTracker
                .param("step", "second")
                .execute(() -> {
                    System.out.println("Executing second step...");
                    
                    // Return second step result
                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "success");
                    data.put("step", "second");
                    return data;
                });
            
            // Complete the parent operation
            Map<String, Object> parentResult = new HashMap<>();
            parentResult.put("success", true);
            parentResult.put("operation", "nestedOperation");
            parentResult.put("firstResult", firstResult);
            parentResult.put("secondResult", secondResult);
            
            return errorHandler.handleSuccess(parentOperationId, parentResult);
        } catch (Exception e) {
            // Handle the error
            return errorHandler.handleError(parentOperationId, "example-command", 
                "Failed to execute nested operation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Handles a batch operation with the metadata optimizer.
     *
     * @param parentOperationId the parent operation ID
     * @return the exit code
     */
    private int handleBatchOperation(String parentOperationId) {
        // Create a sub-tracker for this operation
        OperationTracker subTracker = tracker.createSubTracker("example-command-batch");
        
        try {
            // Create a metadata optimizer
            MetadataOptimizer optimizer = new MetadataOptimizer(metadataService);
            
            // Create a list of items to process
            List<String> items = List.of("item1", "item2", "item3", "item4", "item5");
            
            // Process the batch
            System.out.println("Processing batch of " + items.size() + " items...");
            
            int successCount = optimizer.processBatch(
                "example-command-batch-item",
                "PROCESS",
                items,
                // Parameter supplier for each item
                item -> {
                    Map<String, Object> params = new HashMap<>();
                    params.put("item", item);
                    return params;
                },
                // Item processor
                item -> {
                    System.out.println("Processing item: " + item);
                    // Simulate work...
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Processing interrupted", e);
                    }
                });
            
            // Ensure all operations are completed
            optimizer.flushQueue();
            optimizer.shutdown(5);
            
            // Complete the parent operation
            Map<String, Object> parentResult = new HashMap<>();
            parentResult.put("success", true);
            parentResult.put("operation", "batchOperation");
            parentResult.put("itemCount", items.size());
            parentResult.put("successCount", successCount);
            
            return errorHandler.handleSuccess(parentOperationId, parentResult);
        } catch (Exception e) {
            // Handle the error
            return errorHandler.handleError(parentOperationId, "example-command", 
                "Failed to execute batch operation: " + e.getMessage(), e);
        }
    }
    
    /**
     * Displays help information for the command.
     *
     * @param operationId the operation ID for tracking
     */
    private void displayHelp(String operationId) {
        System.out.println("Usage: rin example-command <operation> [options]");
        System.out.println();
        System.out.println("Operations:");
        System.out.println("  simpleOperation  - Execute a simple operation");
        System.out.println("  nestedOperation  - Execute a nested operation with hierarchical tracking");
        System.out.println("  batchOperation   - Execute a batch operation with optimized tracking");
        System.out.println("  help             - Display this help message");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --json           - Output in JSON format");
        System.out.println("  --verbose        - Show verbose output");
        
        // Complete the operation
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("operation", "help");
        errorHandler.handleSuccess(operationId, result);
    }
}