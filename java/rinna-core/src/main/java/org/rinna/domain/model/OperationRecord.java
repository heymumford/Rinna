package org.rinna.domain.model;

import java.time.Instant;
import java.util.Map;

/**
 * Represents a record of an operation performed in the system.
 * This class is used to track and audit system operations.
 */
public class OperationRecord {
    private final String operationId;
    private final String operationType;
    private final String status;
    private final Instant startTime;
    private final Instant endTime;
    private final Map<String, Object> parameters;
    private final Map<String, Object> results;
    private final String errorDetails;

    /**
     * Creates a new operation record with the specified details.
     *
     * @param operationId   Unique identifier for the operation
     * @param operationType Type of operation (e.g., "ADD_ITEM", "UPDATE_ITEM")
     * @param status        Current status of the operation (e.g., "SUCCESS", "FAILED")
     * @param startTime     Time when the operation started
     * @param endTime       Time when the operation completed (may be null if not completed)
     * @param parameters    Parameters provided to the operation
     * @param results       Results produced by the operation
     * @param errorDetails  Details of any error that occurred (may be null)
     */
    public OperationRecord(String operationId, String operationType, String status, 
                          Instant startTime, Instant endTime, 
                          Map<String, Object> parameters, Map<String, Object> results, 
                          String errorDetails) {
        this.operationId = operationId;
        this.operationType = operationType;
        this.status = status;
        this.startTime = startTime;
        this.endTime = endTime;
        this.parameters = parameters;
        this.results = results;
        this.errorDetails = errorDetails;
    }

    public String getOperationId() {
        return operationId;
    }

    public String getOperationType() {
        return operationType;
    }

    public String getStatus() {
        return status;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Map<String, Object> getResults() {
        return results;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    @Override
    public String toString() {
        return "OperationRecord{" +
                "operationId='" + operationId + '\'' +
                ", operationType='" + operationType + '\'' +
                ", status='" + status + '\'' +
                ", startTime=" + startTime +
                ", endTime=" + endTime +
                ", parameters=" + parameters +
                ", results=" + results +
                ", errorDetails='" + errorDetails + '\'' +
                '}';
    }
}