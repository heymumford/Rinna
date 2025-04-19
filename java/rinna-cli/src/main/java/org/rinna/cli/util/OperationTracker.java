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

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.rinna.cli.service.MetadataService;

/**
 * Utility class to simplify operation tracking in CLI commands.
 * This class provides a fluent interface for tracking operations with the MetadataService,
 * reducing boilerplate code and standardizing error handling.
 */
public class OperationTracker {

    private final MetadataService metadataService;
    private String commandName;
    private String operationType = "EXECUTE";
    private final Map<String, Object> parameters = new HashMap<>();
    private String parentOperationId;

    /**
     * Creates a new operation tracker with the specified metadata service.
     *
     * @param metadataService the metadata service to use
     */
    public OperationTracker(MetadataService metadataService) {
        this.metadataService = metadataService;
    }

    /**
     * Sets the command name for this operation.
     *
     * @param commandName the command name
     * @return this tracker for method chaining
     */
    public OperationTracker command(String commandName) {
        this.commandName = commandName;
        return this;
    }

    /**
     * Sets the operation type for this operation.
     *
     * @param operationType the operation type
     * @return this tracker for method chaining
     */
    public OperationTracker operationType(String operationType) {
        this.operationType = operationType;
        return this;
    }

    /**
     * Sets the parent operation ID for hierarchical tracking.
     *
     * @param parentOperationId the parent operation ID
     * @return this tracker for method chaining
     */
    public OperationTracker parent(String parentOperationId) {
        this.parentOperationId = parentOperationId;
        return this;
    }

    /**
     * Adds a parameter to track with this operation.
     *
     * @param key the parameter name
     * @param value the parameter value
     * @return this tracker for method chaining
     */
    public OperationTracker param(String key, Object value) {
        this.parameters.put(key, value);
        return this;
    }

    /**
     * Adds multiple parameters to track with this operation.
     *
     * @param params the parameters to add
     * @return this tracker for method chaining
     */
    public OperationTracker params(Map<String, Object> params) {
        if (params != null) {
            this.parameters.putAll(params);
        }
        return this;
    }

    /**
     * Starts tracking the operation and returns the operation ID.
     *
     * @return the operation ID
     * @throws IllegalStateException if command name is not set
     */
    public String start() {
        if (commandName == null || commandName.isEmpty()) {
            throw new IllegalStateException("Command name must be set before starting an operation");
        }
        return metadataService.startOperation(commandName, operationType, new HashMap<>(parameters));
    }

    /**
     * Executes an operation and tracks its result or failure.
     *
     * @param <T> the return type of the operation
     * @param operation the operation to execute
     * @return the result of the operation
     * @throws RuntimeException if the operation fails
     */
    public <T> T execute(Supplier<T> operation) {
        String operationId = start();
        try {
            T result = operation.get();
            if (result != null) {
                metadataService.completeOperation(operationId, result);
            } else {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                metadataService.completeOperation(operationId, resultData);
            }
            return result;
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            throw e;
        }
    }

    /**
     * Executes an operation that doesn't return a value and tracks its result or failure.
     *
     * @param operation the operation to execute
     * @throws RuntimeException if the operation fails
     */
    public void executeVoid(Runnable operation) {
        String operationId = start();
        try {
            operation.run();
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            metadataService.completeOperation(operationId, resultData);
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            throw e;
        }
    }

    /**
     * Executes an operation with a specific result to track on success.
     *
     * @param <T> the return type of the operation
     * @param operation the operation to execute
     * @param resultSupplier supplier for the result to track (which may differ from the operation result)
     * @return the result of the operation
     * @throws RuntimeException if the operation fails
     */
    public <T, R> T executeWithResult(Supplier<T> operation, Supplier<R> resultSupplier) {
        String operationId = start();
        try {
            T result = operation.get();
            R trackingResult = resultSupplier.get();
            metadataService.completeOperation(operationId, trackingResult);
            return result;
        } catch (Exception e) {
            metadataService.failOperation(operationId, e);
            throw e;
        }
    }

    /**
     * Executes a hierarchical sub-operation within a parent operation context.
     *
     * @param <T> the return type of the operation
     * @param subCommandName the sub-command name
     * @param operation the operation to execute
     * @return the result of the operation
     * @throws RuntimeException if the operation fails
     */
    public <T> T executeSubOperation(String subCommandName, Supplier<T> operation) {
        OperationTracker subTracker = new OperationTracker(metadataService)
                .command(subCommandName)
                .params(parameters);
        
        // If we're tracking operation detail, record the sub-operation in the parent
        if (parentOperationId != null) {
            metadataService.trackOperationDetail(parentOperationId, "subOperation", subCommandName);
        }
        
        return subTracker.execute(operation);
    }

    /**
     * Creates a sub-tracker for hierarchical operation tracking.
     *
     * @param subCommandName the sub-command name
     * @return a new operation tracker for the sub-operation
     */
    public OperationTracker createSubTracker(String subCommandName) {
        String currentOperationId = start();
        return new OperationTracker(metadataService)
                .command(subCommandName)
                .parent(currentOperationId);
    }

    /**
     * Tracks an error that occurs during an operation.
     *
     * @param operationId the operation ID
     * @param operationName the operation name
     * @param errorMessage the error message
     * @param exception the exception that occurred
     */
    public void trackError(String operationId, String operationName, String errorMessage, Exception exception) {
        metadataService.trackOperationError(operationId, operationName, errorMessage, exception);
    }

    /**
     * Marks an operation as complete with a result.
     *
     * @param operationId the operation ID
     * @param result the result of the operation
     */
    public void complete(String operationId, Object result) {
        metadataService.completeOperation(operationId, result);
    }

    /**
     * Marks an operation as failed with an exception.
     *
     * @param operationId the operation ID
     * @param exception the exception that caused the failure
     */
    public void fail(String operationId, Throwable exception) {
        metadataService.failOperation(operationId, exception);
    }
}