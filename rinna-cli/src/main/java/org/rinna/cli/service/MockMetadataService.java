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
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Mock implementation of the metadata service for tracking CLI operations.
 */
public final class MockMetadataService implements MetadataService {
    
    private static MockMetadataService instance;
    private final Map<String, OperationMetadata> operations = new ConcurrentHashMap<>();
    
    // Limit for storing recent operations as Maps for easy access by PUI
    private final List<Map<String, Object>> recentOperations = new ArrayList<>();
    private final int maxRecentOps = 100;
    
    /**
     * Private constructor for singleton pattern.
     */
    private MockMetadataService() {
        // Initialize with some sample data for testing
        initializeSampleData();
    }
    
    /**
     * Gets the singleton instance of the service.
     *
     * @return The singleton instance
     */
    public static synchronized MockMetadataService getInstance() {
        if (instance == null) {
            instance = new MockMetadataService();
        }
        return instance;
    }
    
    /**
     * Initialize sample operation data for testing.
     */
    private void initializeSampleData() {
        // Current user for sample data
        String username = System.getProperty("user.name", "unknown");
        String clientInfo = "CLI client " + System.getProperty("os.name");
        
        // Add some sample completed operations
        LocalDateTime now = LocalDateTime.now();
        
        // List command sample
        Map<String, Object> listParams = new HashMap<>();
        listParams.put("status", "OPEN");
        listParams.put("limit", 10);
        String listOpId = UUID.randomUUID().toString();
        OperationMetadata listOp = new OperationMetadata(
            listOpId, "list", "READ", listParams, now.minusMinutes(30), username, clientInfo);
        listOp.setStatus("COMPLETED");
        listOp.setEndTime(now.minusMinutes(29));
        listOp.setResult("Listed 5 items");
        operations.put(listOpId, listOp);
        
        // View command sample
        Map<String, Object> viewParams = new HashMap<>();
        viewParams.put("itemId", "WI-123");
        String viewOpId = UUID.randomUUID().toString();
        OperationMetadata viewOp = new OperationMetadata(
            viewOpId, "view", "READ", viewParams, now.minusMinutes(25), username, clientInfo);
        viewOp.setStatus("COMPLETED");
        viewOp.setEndTime(now.minusMinutes(24));
        viewOp.setResult("Displayed item WI-123");
        operations.put(viewOpId, viewOp);
        
        // Add command sample
        Map<String, Object> addParams = new HashMap<>();
        addParams.put("title", "Fix navigation bug");
        addParams.put("type", "BUG");
        addParams.put("priority", "HIGH");
        String addOpId = UUID.randomUUID().toString();
        OperationMetadata addOp = new OperationMetadata(
            addOpId, "add", "CREATE", addParams, now.minusMinutes(20), username, clientInfo);
        addOp.setStatus("COMPLETED");
        addOp.setEndTime(now.minusMinutes(19));
        addOp.setResult("Created item WI-124");
        operations.put(addOpId, addOp);
        
        // Update command sample
        Map<String, Object> updateParams = new HashMap<>();
        updateParams.put("itemId", "WI-124");
        updateParams.put("status", "IN_PROGRESS");
        String updateOpId = UUID.randomUUID().toString();
        OperationMetadata updateOp = new OperationMetadata(
            updateOpId, "update", "UPDATE", updateParams, now.minusMinutes(15), username, clientInfo);
        updateOp.setStatus("COMPLETED");
        updateOp.setEndTime(now.minusMinutes(14));
        updateOp.setResult("Updated item WI-124");
        operations.put(updateOpId, updateOp);
        
        // Failed operation sample
        Map<String, Object> failedParams = new HashMap<>();
        failedParams.put("itemId", "WI-999");
        String failedOpId = UUID.randomUUID().toString();
        OperationMetadata failedOp = new OperationMetadata(
            failedOpId, "view", "READ", failedParams, now.minusMinutes(10), username, clientInfo);
        failedOp.setStatus("FAILED");
        failedOp.setEndTime(now.minusMinutes(9));
        failedOp.setErrorMessage("Item not found: WI-999");
        operations.put(failedOpId, failedOp);
    }
    
    @Override
    public String startOperation(String commandName, String operationType, Map<String, Object> parameters) {
        // Generate a unique ID for the operation
        String operationId = UUID.randomUUID().toString();
        
        // Get current user information
        String username = System.getProperty("user.name", "unknown");
        String clientInfo = "CLI client " + System.getProperty("os.name");
        
        // Create and store the metadata
        OperationMetadata metadata = new OperationMetadata(
            operationId, commandName, operationType, parameters, LocalDateTime.now(), username, clientInfo);
        operations.put(operationId, metadata);
        
        // Integrate with audit service
        AuditService auditService = ServiceManager.getInstance().getAuditService();
        if (auditService != null) {
            try {
                // Convert parameters to a string representation for audit log
                StringBuilder paramString = new StringBuilder();
                if (parameters != null) {
                    for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                        paramString.append(entry.getKey()).append("=");
                        if (entry.getValue() != null) {
                            paramString.append(entry.getValue().toString());
                        } else {
                            paramString.append("null");
                        }
                        paramString.append(" ");
                    }
                }
                
                // This would integrate with the audit service in a production implementation
                // For now, we're just simulating the integration
            } catch (Exception e) {
                // Log the exception but don't interrupt the operation
                System.err.println("Error logging to audit service: " + e.getMessage());
            }
        }
        
        return operationId;
    }
    
    @Override
    public void completeOperation(String operationId, Object result) {
        OperationMetadata metadata = operations.get(operationId);
        if (metadata != null) {
            metadata.setStatus("COMPLETED");
            metadata.setEndTime(LocalDateTime.now());
            metadata.setResult(result);
            
            // Add to recent operations as a simplified map for PUI
            addToRecentOperations(metadata);
        }
    }
    
    @Override
    public void failOperation(String operationId, Throwable exception) {
        OperationMetadata metadata = operations.get(operationId);
        if (metadata != null) {
            metadata.setStatus("FAILED");
            metadata.setEndTime(LocalDateTime.now());
            metadata.setErrorMessage(exception.getMessage());
            
            // Add to recent operations as a simplified map for PUI
            addToRecentOperations(metadata);
        }
    }
    
    /**
     * Adds an operation to the recent operations list for PUI access.
     * 
     * @param metadata The operation metadata to add
     */
    private synchronized void addToRecentOperations(OperationMetadata metadata) {
        Map<String, Object> opMap = new HashMap<>();
        opMap.put("id", metadata.getId());
        opMap.put("command", metadata.getCommandName());
        opMap.put("type", metadata.getOperationType());
        opMap.put("status", metadata.getStatus());
        opMap.put("startTime", metadata.getStartTime().toString());
        
        if (metadata.getEndTime() != null) {
            opMap.put("endTime", metadata.getEndTime().toString());
            long durationMs = ChronoUnit.MILLIS.between(metadata.getStartTime(), metadata.getEndTime());
            opMap.put("durationMs", durationMs);
        }
        
        opMap.put("user", metadata.getUsername());
        
        // Add selected parameters that are safe to display
        if (metadata.getParameters() != null) {
            Map<String, Object> safeParams = new HashMap<>();
            for (Map.Entry<String, Object> entry : metadata.getParameters().entrySet()) {
                // Skip password parameters for security
                if (!entry.getKey().toLowerCase().contains("password") && 
                    !entry.getKey().toLowerCase().contains("secret")) {
                    safeParams.put(entry.getKey(), entry.getValue());
                }
            }
            opMap.put("parameters", safeParams);
        }
        
        // Add result or error message
        if ("COMPLETED".equals(metadata.getStatus()) && metadata.getResult() != null) {
            opMap.put("result", metadata.getResult().toString());
        } else if ("FAILED".equals(metadata.getStatus()) && metadata.getErrorMessage() != null) {
            opMap.put("error", metadata.getErrorMessage());
        }
        
        // Add to front of list (most recent first)
        synchronized (recentOperations) {
            recentOperations.add(0, opMap);
            
            // Maintain maximum size
            if (recentOperations.size() > maxRecentOps) {
                recentOperations.remove(recentOperations.size() - 1);
            }
        }
    }
    
    @Override
    public OperationMetadata getOperationMetadata(String operationId) {
        return operations.get(operationId);
    }
    
    @Override
    public List<OperationMetadata> listOperations(String commandName, String operationType, int limit) {
        return operations.values().stream()
            .filter(op -> commandName == null || commandName.equals(op.getCommandName()))
            .filter(op -> operationType == null || operationType.equals(op.getOperationType()))
            .sorted(Comparator.comparing(OperationMetadata::getStartTime).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Object> getOperationStatistics(String commandName, LocalDateTime from, LocalDateTime to) {
        Map<String, Object> statistics = new HashMap<>();
        
        // Filter operations based on the parameters
        List<OperationMetadata> filteredOps = operations.values().stream()
            .filter(op -> commandName == null || commandName.equals(op.getCommandName()))
            .filter(op -> from == null || !op.getStartTime().isBefore(from))
            .filter(op -> to == null || !op.getStartTime().isAfter(to))
            .collect(Collectors.toList());
        
        // Calculate basic statistics
        statistics.put("totalOperations", filteredOps.size());
        
        long completedOps = filteredOps.stream()
            .filter(op -> "COMPLETED".equals(op.getStatus()))
            .count();
        statistics.put("completedOperations", completedOps);
        
        long failedOps = filteredOps.stream()
            .filter(op -> "FAILED".equals(op.getStatus()))
            .count();
        statistics.put("failedOperations", failedOps);
        
        // Calculate success rate
        double successRate = filteredOps.isEmpty() ? 0 : 
            (double) completedOps / filteredOps.size() * 100;
        statistics.put("successRate", successRate);
        
        // Calculate average duration for completed operations
        List<OperationMetadata> completedOperations = filteredOps.stream()
            .filter(op -> "COMPLETED".equals(op.getStatus()))
            .filter(op -> op.getEndTime() != null)
            .collect(Collectors.toList());
        
        if (!completedOperations.isEmpty()) {
            double avgDurationMs = completedOperations.stream()
                .mapToLong(op -> ChronoUnit.MILLIS.between(op.getStartTime(), op.getEndTime()))
                .average()
                .orElse(0);
            statistics.put("averageDurationMs", avgDurationMs);
        }
        
        // Calculate operation counts by type
        Map<String, Long> operationsByType = filteredOps.stream()
            .collect(Collectors.groupingBy(OperationMetadata::getOperationType, Collectors.counting()));
        statistics.put("operationsByType", operationsByType);
        
        // Calculate operation counts by command
        Map<String, Long> operationsByCommand = filteredOps.stream()
            .collect(Collectors.groupingBy(OperationMetadata::getCommandName, Collectors.counting()));
        statistics.put("operationsByCommand", operationsByCommand);
        
        return statistics;
    }
    
    @Override
    public int clearOperationHistory(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        
        List<String> keysToRemove = operations.entrySet().stream()
            .filter(entry -> entry.getValue().getStartTime().isBefore(cutoffDate))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        for (String key : keysToRemove) {
            operations.remove(key);
        }
        
        // Also clean up old operations from recent operations
        synchronized (recentOperations) {
            recentOperations.removeIf(op -> {
                try {
                    String startTimeStr = (String) op.get("startTime");
                    if (startTimeStr != null) {
                        LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
                        return startTime.isBefore(cutoffDate);
                    }
                } catch (Exception e) {
                    // Skip entries with invalid dates
                }
                return false;
            });
        }
        
        return keysToRemove.size();
    }
    
    /**
     * Gets a list of recent operations in simplified format for PUI components.
     * 
     * @param limit The maximum number of operations to return
     * @return List of operation data as maps
     */
    public List<Map<String, Object>> getRecentOperations(int limit) {
        synchronized (recentOperations) {
            // Return a copy of the list to prevent concurrent modification issues
            if (limit <= 0 || limit >= recentOperations.size()) {
                return new ArrayList<>(recentOperations);
            } else {
                return new ArrayList<>(recentOperations.subList(0, limit));
            }
        }
    }
    
    /**
     * Formats operation metadata as a JSON string.
     *
     * @param metadata The operation metadata
     * @return JSON string representation
     */
    public String formatAsJson(OperationMetadata metadata) {
        if (metadata == null) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"id\": \"").append(metadata.getId()).append("\",\n");
        json.append("  \"commandName\": \"").append(metadata.getCommandName()).append("\",\n");
        json.append("  \"operationType\": \"").append(metadata.getOperationType()).append("\",\n");
        json.append("  \"status\": \"").append(metadata.getStatus()).append("\",\n");
        json.append("  \"startTime\": \"").append(metadata.getStartTime()).append("\",\n");
        
        if (metadata.getEndTime() != null) {
            json.append("  \"endTime\": \"").append(metadata.getEndTime()).append("\",\n");
            json.append("  \"durationMs\": ").append(ChronoUnit.MILLIS.between(metadata.getStartTime(), metadata.getEndTime())).append(",\n");
        }
        
        json.append("  \"username\": \"").append(metadata.getUsername()).append("\",\n");
        json.append("  \"clientInfo\": \"").append(metadata.getClientInfo()).append("\",\n");
        
        // Add parameters
        json.append("  \"parameters\": {\n");
        if (metadata.getParameters() != null) {
            List<String> paramEntries = new ArrayList<>();
            for (Map.Entry<String, Object> entry : metadata.getParameters().entrySet()) {
                String value = entry.getValue() != null ? entry.getValue().toString() : "null";
                value = value.replace("\"", "\\\""); // Escape quotes
                paramEntries.add("    \"" + entry.getKey() + "\": \"" + value + "\"");
            }
            json.append(String.join(",\n", paramEntries));
        }
        json.append("\n  }");
        
        // Add result or error message
        if ("COMPLETED".equals(metadata.getStatus()) && metadata.getResult() != null) {
            String result = metadata.getResult().toString().replace("\"", "\\\""); // Escape quotes
            json.append(",\n  \"result\": \"").append(result).append("\"");
        } else if ("FAILED".equals(metadata.getStatus()) && metadata.getErrorMessage() != null) {
            String error = metadata.getErrorMessage().replace("\"", "\\\""); // Escape quotes
            json.append(",\n  \"errorMessage\": \"").append(error).append("\"");
        }
        
        json.append("\n}");
        return json.toString();
    }
    
    /**
     * Formats a list of operations as a JSON array.
     *
     * @param operations List of operation metadata
     * @return JSON array string representation
     */
    public String formatAsJson(List<OperationMetadata> operations) {
        if (operations == null || operations.isEmpty()) {
            return "[]";
        }
        
        List<String> jsonOps = operations.stream()
            .map(this::formatAsJson)
            .collect(Collectors.toList());
        
        return "[\n" + String.join(",\n", jsonOps) + "\n]";
    }
    
    /**
     * Formats statistics as a JSON string.
     *
     * @param statistics The statistics map
     * @return JSON string representation
     */
    public String formatStatisticsAsJson(Map<String, Object> statistics) {
        if (statistics == null || statistics.isEmpty()) {
            return "{}";
        }
        
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        
        List<String> entries = new ArrayList<>();
        for (Map.Entry<String, Object> entry : statistics.entrySet()) {
            if (entry.getValue() instanceof Map) {
                // Handle nested maps
                @SuppressWarnings("unchecked")
                Map<String, Object> nestedMap = (Map<String, Object>) entry.getValue();
                StringBuilder nestedJson = new StringBuilder();
                nestedJson.append("  \"").append(entry.getKey()).append("\": {\n");
                
                List<String> nestedEntries = new ArrayList<>();
                for (Map.Entry<String, Object> nestedEntry : nestedMap.entrySet()) {
                    nestedEntries.add("    \"" + nestedEntry.getKey() + "\": " + nestedEntry.getValue());
                }
                
                nestedJson.append(String.join(",\n", nestedEntries));
                nestedJson.append("\n  }");
                entries.add(nestedJson.toString());
            } else {
                // Handle primitive values
                entries.add("  \"" + entry.getKey() + "\": " + entry.getValue());
            }
        }
        
        json.append(String.join(",\n", entries));
        json.append("\n}");
        return json.toString();
    }
}