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
import java.util.Map;
import java.util.HashMap;
import java.util.function.Consumer;

/**
 * Standardized error handling for CLI commands with integrated operation tracking.
 * This utility provides consistent error reporting and ensures that all errors
 * are properly tracked in the metadata service.
 */
public class ErrorHandler {
    
    private final MetadataService metadataService;
    private boolean verbose = false;
    private String outputFormat = "text";
    private Consumer<String> errorOutputConsumer = System.err::println;
    
    /**
     * Creates a new error handler with the specified metadata service.
     *
     * @param metadataService the metadata service for tracking errors
     */
    public ErrorHandler(MetadataService metadataService) {
        this.metadataService = metadataService;
    }
    
    /**
     * Sets whether to display verbose error information.
     *
     * @param verbose true for verbose output
     * @return this handler for method chaining
     */
    public ErrorHandler verbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }
    
    /**
     * Sets the output format to use for error messages.
     *
     * @param outputFormat the output format ("text" or "json")
     * @return this handler for method chaining
     */
    public ErrorHandler outputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
        return this;
    }
    
    /**
     * Sets a custom consumer for error output.
     * By default, errors are sent to System.err.
     *
     * @param errorOutputConsumer the consumer for error output
     * @return this handler for method chaining
     */
    public ErrorHandler errorOutputConsumer(Consumer<String> errorOutputConsumer) {
        this.errorOutputConsumer = errorOutputConsumer;
        return this;
    }
    
    /**
     * Handles an error by tracking it and outputting an appropriate message.
     *
     * @param operationId the operation ID for tracking
     * @param commandName the command name
     * @param errorMessage the error message
     * @param exception the exception that occurred
     * @return the error exit code (1)
     */
    public int handleError(String operationId, String commandName, String errorMessage, Exception exception) {
        // Track the error in the metadata service
        metadataService.failOperation(operationId, exception);
        metadataService.trackOperationError(operationId, commandName, errorMessage, exception);
        
        // Output the error message
        outputError(errorMessage, exception);
        
        // Return a standard error exit code
        return 1;
    }
    
    /**
     * Handles an error by tracking it and outputting an appropriate message.
     * This version does not take the exception, for cases where there isn't one.
     *
     * @param operationId the operation ID for tracking
     * @param commandName the command name
     * @param errorMessage the error message
     * @return the error exit code (1)
     */
    public int handleError(String operationId, String commandName, String errorMessage) {
        // Create a generic exception for tracking
        Exception exception = new RuntimeException(errorMessage);
        return handleError(operationId, commandName, errorMessage, exception);
    }
    
    /**
     * Handles a validation error, which is tracked but may have a different format.
     *
     * @param operationId the operation ID for tracking
     * @param commandName the command name
     * @param validationErrors a map of field names to error messages
     * @return the error exit code (1)
     */
    public int handleValidationError(String operationId, String commandName, Map<String, String> validationErrors) {
        // Build a combined error message
        StringBuilder errorMessage = new StringBuilder("Validation errors:\n");
        for (Map.Entry<String, String> entry : validationErrors.entrySet()) {
            errorMessage.append(" - ").append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
        }
        
        // Track the validation errors in the metadata service
        Exception exception = new IllegalArgumentException("Validation failed");
        metadataService.failOperation(operationId, exception);
        
        // Track each validation error separately
        for (Map.Entry<String, String> entry : validationErrors.entrySet()) {
            Map<String, Object> errorDetail = new HashMap<>();
            errorDetail.put("field", entry.getKey());
            errorDetail.put("message", entry.getValue());
            metadataService.trackOperationDetail(operationId, "validationError_" + entry.getKey(), errorDetail);
        }
        
        // Output the error message
        outputError(errorMessage.toString(), exception);
        
        // Return a standard error exit code
        return 1;
    }
    
    /**
     * Handles an unexpected error with more detailed tracking.
     *
     * @param operationId the operation ID for tracking
     * @param commandName the command name
     * @param exception the exception that occurred
     * @return the error exit code (1)
     */
    public int handleUnexpectedError(String operationId, String commandName, Throwable exception) {
        // Create a more descriptive error message for unexpected errors
        String errorMessage = "Unexpected error in " + commandName + ": " + exception.getMessage();
        
        // Track detailed error information
        Map<String, Object> errorDetail = new HashMap<>();
        errorDetail.put("exceptionType", exception.getClass().getName());
        errorDetail.put("message", exception.getMessage());
        
        if (exception.getCause() != null) {
            errorDetail.put("cause", exception.getCause().getMessage());
            errorDetail.put("causeType", exception.getCause().getClass().getName());
        }
        
        metadataService.trackOperationDetail(operationId, "unexpectedError", errorDetail);
        metadataService.failOperation(operationId, exception);
        
        // Output the error message
        outputError(errorMessage, exception);
        
        // Return a standard error exit code
        return 1;
    }
    
    /**
     * Outputs an error message in the appropriate format.
     *
     * @param message the error message
     * @param exception the exception that occurred (may be null)
     */
    private void outputError(String message, Throwable exception) {
        if ("json".equalsIgnoreCase(outputFormat)) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("result", "error");
            errorData.put("message", message);
            
            if (verbose && exception != null) {
                errorData.put("exceptionType", exception.getClass().getName());
                if (exception.getCause() != null) {
                    errorData.put("cause", exception.getCause().getMessage());
                }
                
                // Add stack trace for debugging
                StringBuilder stackTrace = new StringBuilder();
                for (StackTraceElement element : exception.getStackTrace()) {
                    stackTrace.append(element.toString()).append("\n");
                }
                errorData.put("stackTrace", stackTrace.toString());
            }
            
            errorOutputConsumer.accept(OutputFormatter.toJson(errorData, verbose));
        } else {
            errorOutputConsumer.accept("Error: " + message);
            
            if (verbose && exception != null) {
                // Print stack trace for debugging in verbose mode
                exception.printStackTrace();
            }
        }
    }
    
    /**
     * Handles command success by tracking it and returning a success code.
     *
     * @param operationId the operation ID for tracking
     * @param result the operation result
     * @return the success exit code (0)
     */
    public int handleSuccess(String operationId, Object result) {
        metadataService.completeOperation(operationId, result);
        return 0;
    }
    
    /**
     * Creates a standard result map for successful operations.
     *
     * @param commandName the command name
     * @param data any additional result data
     * @return a map containing the result data
     */
    public Map<String, Object> createSuccessResult(String commandName, Map<String, Object> data) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("command", commandName);
        
        if (data != null) {
            result.putAll(data);
        }
        
        return result;
    }
}