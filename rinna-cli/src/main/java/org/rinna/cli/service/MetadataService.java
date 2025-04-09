/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for tracking metadata across CLI operations.
 * This service provides a standardized way to log and track operations
 * performed by CLI commands, enhancing auditability and traceability.
 */
public interface MetadataService {
    
    /**
     * Records the start of an operation.
     *
     * @param commandName The name of the command being executed
     * @param operationType The type of operation (e.g., CREATE, UPDATE, DELETE)
     * @param parameters Additional parameters for the operation
     * @return The operation ID
     */
    String startOperation(String commandName, String operationType, Map<String, Object> parameters);
    
    /**
     * Records the successful completion of an operation.
     *
     * @param operationId The operation ID returned by startOperation
     * @param result The result of the operation
     */
    void completeOperation(String operationId, Object result);
    
    /**
     * Records the failure of an operation.
     *
     * @param operationId The operation ID returned by startOperation
     * @param exception The exception that occurred
     */
    void failOperation(String operationId, Throwable exception);
    
    /**
     * Gets the metadata for an operation.
     *
     * @param operationId The operation ID
     * @return The operation metadata
     */
    OperationMetadata getOperationMetadata(String operationId);
    
    /**
     * Lists recent operations with optional filtering.
     *
     * @param commandName Optional command name filter
     * @param operationType Optional operation type filter
     * @param limit Maximum number of operations to return
     * @return List of operation metadata
     */
    List<OperationMetadata> listOperations(String commandName, String operationType, int limit);
    
    /**
     * Gets statistics about operations.
     *
     * @param commandName Optional command name filter
     * @param from Optional start date filter
     * @param to Optional end date filter
     * @return Map of statistic name to value
     */
    Map<String, Object> getOperationStatistics(String commandName, LocalDateTime from, LocalDateTime to);
    
    /**
     * Simple method to track an operation without detailed parameters.
     * This is a convenience method for commands that don't need detailed tracking.
     *
     * @param commandName The name of the command being executed
     * @param parameters Optional parameters for the operation
     * @return The operation ID
     */
    default String trackOperation(String commandName, Map<String, Object> parameters) {
        return startOperation(commandName, "EXECUTE", parameters);
    }
    
    /**
     * Simple method to track an operation with string data parameters.
     * This is a convenience method for commands that don't need detailed tracking.
     *
     * @param commandName The name of the command being executed
     * @param data Map of string data parameters 
     * @return The operation ID
     */
    default String trackOperationWithData(String commandName, Map<String, String> data) {
        Map<String, Object> objectData = new HashMap<>(data.size());
        data.forEach(objectData::put);
        return startOperation(commandName, "EXECUTE", objectData);
    }
    
    /**
     * Clears operation history older than the specified days.
     *
     * @param days Number of days to retain
     * @return Number of operations cleared
     */
    int clearOperationHistory(int days);
    
    /**
     * Class representing operation metadata.
     */
    class OperationMetadata {
        private String id;
        private String commandName;
        private String operationType;
        private Map<String, Object> parameters;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String status;
        private Object result;
        private String errorMessage;
        private String username;
        private String clientInfo;
        
        // Constructors, getters, and setters
        
        public OperationMetadata(String id, String commandName, String operationType, Map<String, Object> parameters,
                                LocalDateTime startTime, String username, String clientInfo) {
            this.id = id;
            this.commandName = commandName;
            this.operationType = operationType;
            this.parameters = parameters;
            this.startTime = startTime;
            this.status = "IN_PROGRESS";
            this.username = username;
            this.clientInfo = clientInfo;
        }
        
        public String getId() {
            return id;
        }
        
        public String getCommandName() {
            return commandName;
        }
        
        public String getOperationType() {
            return operationType;
        }
        
        public Map<String, Object> getParameters() {
            return parameters;
        }
        
        public LocalDateTime getStartTime() {
            return startTime;
        }
        
        public LocalDateTime getEndTime() {
            return endTime;
        }
        
        public void setEndTime(LocalDateTime endTime) {
            this.endTime = endTime;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public Object getResult() {
            return result;
        }
        
        public void setResult(Object result) {
            this.result = result;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
        
        public String getUsername() {
            return username;
        }
        
        public String getClientInfo() {
            return clientInfo;
        }
    }
}