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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Optimized implementation of the metadata service for tracking CLI operations.
 * This implementation is designed for high-volume operation tracking with:
 * - Asynchronous operation completion and failure handling
 * - Batched processing for efficient resource usage
 * - In-memory caching with time-based expiration
 * - Parameter object pooling to reduce GC pressure
 * - Rate limiting to prevent overwhelming the system
 * - Aggregation for repetitive operations
 */
public final class OptimizedMetadataService implements MetadataService {
    
    private static OptimizedMetadataService instance;
    
    // Core storage for operations
    private final Map<String, OperationMetadata> operations = new ConcurrentHashMap<>();
    
    // Queue for batch processing operation completion
    private final BlockingQueue<OperationUpdate> completionQueue = new LinkedBlockingQueue<>();
    
    // Executor services for async processing
    private final ExecutorService asyncExecutor;
    private final ScheduledExecutorService scheduledExecutor;
    
    // Parameter pooling
    private final Map<String, Map<String, Object>> parameterPool = new ConcurrentHashMap<>();
    
    // Cache expiration
    private final long cacheExpirationMinutes;
    
    // Recent operations cache
    private final List<Map<String, Object>> recentOperations = new ArrayList<>();
    private final int maxRecentOps = 1000;
    
    // Operation rate limiting
    private final Map<String, AtomicInteger> operationRateLimits = new ConcurrentHashMap<>();
    private final Map<String, String> operationAggregationMap = new ConcurrentHashMap<>();
    
    // Statistics cache to avoid recalculation
    private final Map<String, Map<String, Object>> statisticsCache = new ConcurrentHashMap<>();
    private long statisticsCacheExpiry = System.currentTimeMillis();
    private final long statisticsCacheTtlMs = 5000; // 5 seconds TTL
    
    // Index for fast lookups by command name and operation type
    private final Map<String, List<String>> commandNameIndex = new ConcurrentHashMap<>();
    private final Map<String, List<String>> operationTypeIndex = new ConcurrentHashMap<>();
    
    /**
     * Private constructor for singleton pattern.
     */
    private OptimizedMetadataService() {
        this(5, 10, 60);
    }
    
    /**
     * Constructor with configurable parameters.
     * 
     * @param asyncThreads Number of threads for async processing
     * @param batchSize Size of operation batches to process
     * @param cacheExpirationMinutes Time in minutes before cached operations expire
     */
    public OptimizedMetadataService(int asyncThreads, int batchSize, long cacheExpirationMinutes) {
        this.cacheExpirationMinutes = cacheExpirationMinutes;
        this.asyncExecutor = Executors.newFixedThreadPool(asyncThreads);
        this.scheduledExecutor = Executors.newScheduledThreadPool(2);
        
        // Initialize parameter pool with common parameter combinations
        initializeParameterPool();
        
        // Start background task for batch processing
        scheduledExecutor.scheduleWithFixedDelay(
            this::processPendingOperations, 
            100, // Initial delay 100ms
            100, // Process every 100ms
            TimeUnit.MILLISECONDS
        );
        
        // Start background task for cache cleanup
        scheduledExecutor.scheduleWithFixedDelay(
            this::cleanupExpiredOperations,
            1,   // Initial delay 1 minute
            5,   // Clean every 5 minutes
            TimeUnit.MINUTES
        );
        
        // Clear rate limiting counters periodically
        scheduledExecutor.scheduleWithFixedDelay(
            this::resetRateLimits,
            1,   // Initial delay 1 minute 
            1,   // Reset every 1 minute
            TimeUnit.MINUTES
        );
        
        // Initialize with sample data
        initializeSampleData();
    }
    
    /**
     * Gets the singleton instance of the service.
     *
     * @return The singleton instance
     */
    public static synchronized OptimizedMetadataService getInstance() {
        if (instance == null) {
            instance = new OptimizedMetadataService();
        }
        return instance;
    }
    
    /**
     * Initializes the parameter pool with common parameter combinations.
     */
    private void initializeParameterPool() {
        // Empty parameters
        parameterPool.put("empty", new HashMap<>());
        
        // Common list operation parameters
        Map<String, Object> listParams = new HashMap<>();
        listParams.put("operation", "list");
        parameterPool.put("list", listParams);
        
        // Common list with limit
        Map<String, Object> listLimitParams = new HashMap<>();
        listLimitParams.put("operation", "list");
        listLimitParams.put("limit", 10);
        parameterPool.put("list-limit", listLimitParams);
        
        // Common view operation parameters
        Map<String, Object> viewParams = new HashMap<>();
        viewParams.put("operation", "view");
        parameterPool.put("view", viewParams);
        
        // Common add operation parameters
        Map<String, Object> addParams = new HashMap<>();
        addParams.put("operation", "add");
        parameterPool.put("add", addParams);
        
        // Common update operation parameters
        Map<String, Object> updateParams = new HashMap<>();
        updateParams.put("operation", "update");
        parameterPool.put("update", updateParams);
        
        // Common format parameters
        Map<String, Object> jsonParams = new HashMap<>();
        jsonParams.put("format", "json");
        parameterPool.put("json-format", jsonParams);
        
        Map<String, Object> textParams = new HashMap<>();
        textParams.put("format", "text");
        parameterPool.put("text-format", textParams);
    }
    
    /**
     * Processes pending operation updates from the queue in batches.
     */
    private void processPendingOperations() {
        int batchSize = 50;
        List<OperationUpdate> updates = new ArrayList<>(batchSize);
        
        // Drain up to batchSize items from the queue
        completionQueue.drainTo(updates, batchSize);
        
        if (!updates.isEmpty()) {
            for (OperationUpdate update : updates) {
                if (update.isCompletion) {
                    processOperationCompletion(update.operationId, update.resultOrError);
                } else {
                    processOperationFailure(update.operationId, (Throwable) update.resultOrError);
                }
            }
        }
    }
    
    /**
     * Processes an operation completion.
     * 
     * @param operationId The operation ID
     * @param result The operation result
     */
    private void processOperationCompletion(String operationId, Object result) {
        OperationMetadata metadata = operations.get(operationId);
        if (metadata != null) {
            metadata.setStatus("COMPLETED");
            metadata.setEndTime(LocalDateTime.now());
            metadata.setResult(result);
            
            // Add to recent operations
            addToRecentOperations(metadata);
        }
    }
    
    /**
     * Processes an operation failure.
     * 
     * @param operationId The operation ID
     * @param exception The exception that occurred
     */
    private void processOperationFailure(String operationId, Throwable exception) {
        OperationMetadata metadata = operations.get(operationId);
        if (metadata != null) {
            metadata.setStatus("FAILED");
            metadata.setEndTime(LocalDateTime.now());
            metadata.setErrorMessage(exception.getMessage());
            
            // Add to recent operations
            addToRecentOperations(metadata);
        }
    }
    
    /**
     * Cleans up expired operations from the cache.
     */
    private void cleanupExpiredOperations() {
        LocalDateTime cutoffTime = LocalDateTime.now().minusMinutes(cacheExpirationMinutes);
        
        // Create a predicate to identify expired operations
        Predicate<OperationMetadata> isExpired = op -> 
            op.getEndTime() != null && op.getEndTime().isBefore(cutoffTime);
        
        // Identify expired operation IDs
        List<String> expiredIds = operations.entrySet().stream()
            .filter(entry -> isExpired.test(entry.getValue()))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Remove expired operations from the main store
        for (String id : expiredIds) {
            OperationMetadata metadata = operations.remove(id);
            
            // Also remove from indexes
            if (metadata != null) {
                removeFromIndex(commandNameIndex, metadata.getCommandName(), id);
                removeFromIndex(operationTypeIndex, metadata.getOperationType(), id);
            }
        }
        
        // Clean up recent operations
        synchronized (recentOperations) {
            recentOperations.removeIf(op -> {
                try {
                    String startTimeStr = (String) op.get("startTime");
                    String endTimeStr = (String) op.get("endTime");
                    
                    if (endTimeStr != null) {
                        LocalDateTime endTime = LocalDateTime.parse(endTimeStr);
                        return endTime.isBefore(cutoffTime);
                    } else if (startTimeStr != null) {
                        LocalDateTime startTime = LocalDateTime.parse(startTimeStr);
                        return startTime.isBefore(cutoffTime);
                    }
                } catch (Exception e) {
                    // Skip entries with invalid dates
                }
                return false;
            });
        }
        
        // Invalidate statistics cache on cleanup
        statisticsCacheExpiry = 0;
    }
    
    /**
     * Resets operation rate limits.
     */
    private void resetRateLimits() {
        operationRateLimits.clear();
        operationAggregationMap.clear();
    }
    
    /**
     * Adds an operation to the recent operations list.
     * 
     * @param metadata The operation metadata
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
        
        // Add selected parameters (filtering sensitive data)
        if (metadata.getParameters() != null) {
            Map<String, Object> safeParams = new HashMap<>();
            for (Map.Entry<String, Object> entry : metadata.getParameters().entrySet()) {
                // Skip sensitive parameters
                if (!isSensitiveParameter(entry.getKey())) {
                    safeParams.put(entry.getKey(), entry.getValue());
                }
            }
            opMap.put("parameters", safeParams);
        }
        
        // Add result or error message
        if ("COMPLETED".equals(metadata.getStatus()) && metadata.getResult() != null) {
            opMap.put("result", truncateLongString(metadata.getResult().toString(), 1000));
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
    
    /**
     * Truncates a string if it exceeds a maximum length.
     * 
     * @param input The input string
     * @param maxLength The maximum length
     * @return The truncated string
     */
    private String truncateLongString(String input, int maxLength) {
        if (input == null) {
            return null;
        }
        if (input.length() <= maxLength) {
            return input;
        }
        return input.substring(0, maxLength) + "...";
    }
    
    /**
     * Checks if a parameter name is sensitive.
     * 
     * @param paramName The parameter name
     * @return true if sensitive, false otherwise
     */
    private boolean isSensitiveParameter(String paramName) {
        if (paramName == null) {
            return false;
        }
        
        String lowerName = paramName.toLowerCase();
        return lowerName.contains("password") || 
               lowerName.contains("secret") || 
               lowerName.contains("token") || 
               lowerName.contains("key") || 
               lowerName.contains("credential");
    }
    
    /**
     * Helper class for queuing operation updates.
     */
    private static class OperationUpdate {
        final String operationId;
        final Object resultOrError;
        final boolean isCompletion;
        
        OperationUpdate(String operationId, Object resultOrError, boolean isCompletion) {
            this.operationId = operationId;
            this.resultOrError = resultOrError;
            this.isCompletion = isCompletion;
        }
    }
    
    /**
     * Removes an operation ID from an index.
     * 
     * @param index The index map
     * @param key The index key
     * @param operationId The operation ID to remove
     */
    private void removeFromIndex(Map<String, List<String>> index, String key, String operationId) {
        if (key != null) {
            List<String> ids = index.get(key);
            if (ids != null) {
                synchronized (ids) {
                    ids.remove(operationId);
                    if (ids.isEmpty()) {
                        index.remove(key);
                    }
                }
            }
        }
    }
    
    /**
     * Adds an operation ID to an index.
     * 
     * @param index The index map
     * @param key The index key
     * @param operationId The operation ID to add
     */
    private void addToIndex(Map<String, List<String>> index, String key, String operationId) {
        if (key != null) {
            List<String> ids = index.computeIfAbsent(key, k -> new ArrayList<>());
            synchronized (ids) {
                ids.add(operationId);
            }
        }
    }
    
    /**
     * Gets a pooled parameter map, or creates a new one if not found.
     * 
     * @param key The parameter pool key
     * @return The parameter map (copy)
     */
    private Map<String, Object> getPooledParameters(String key) {
        // Return a copy of the pooled parameters to prevent modification of the originals
        Map<String, Object> params = parameterPool.get(key);
        return params != null ? new HashMap<>(params) : new HashMap<>();
    }
    
    /**
     * Checks if an operation should be rate limited.
     * 
     * @param commandName The command name
     * @param parameters The operation parameters
     * @return true if rate limited, false otherwise
     */
    private boolean isRateLimited(String commandName, Map<String, Object> parameters) {
        // Create a key based on command name and significant parameters
        StringBuilder keyBuilder = new StringBuilder(commandName);
        
        // Add key parameters to the rate limiting key
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                // Only include certain parameters in the rate limiting key
                if (isKeyParameter(entry.getKey())) {
                    keyBuilder.append(":").append(entry.getKey()).append("=");
                    keyBuilder.append(entry.getValue() != null ? entry.getValue().toString() : "null");
                }
            }
        }
        
        String key = keyBuilder.toString();
        
        // Check if this operation is already being aggregated
        if (operationAggregationMap.containsKey(key)) {
            return true;
        }
        
        // Get current count and increment
        AtomicInteger count = operationRateLimits.computeIfAbsent(key, k -> new AtomicInteger(0));
        int newCount = count.incrementAndGet();
        
        // If count exceeds threshold, start aggregating
        if (newCount > 20) { // More than 20 of the same operation per minute
            operationAggregationMap.put(key, commandName);
            return true;
        }
        
        return false;
    }
    
    /**
     * Determines if a parameter should be used for rate limiting.
     * 
     * @param paramName The parameter name
     * @return true if a key parameter, false otherwise
     */
    private boolean isKeyParameter(String paramName) {
        if (paramName == null) {
            return false;
        }
        
        return paramName.equals("operation") || 
               paramName.equals("itemId") || 
               paramName.equals("type") || 
               paramName.equals("status") || 
               paramName.equals("action");
    }
    
    /**
     * Initialize sample operation data for testing.
     */
    private void initializeSampleData() {
        // Get current user information
        String username = System.getProperty("user.name", "unknown");
        String clientInfo = "CLI client " + System.getProperty("os.name");
        
        // Sample timestamps
        LocalDateTime now = LocalDateTime.now();
        
        // List command sample
        Map<String, Object> listParams = getPooledParameters("list-limit");
        listParams.put("status", "OPEN");
        String listOpId = UUID.randomUUID().toString();
        OperationMetadata listOp = new OperationMetadata(
            listOpId, "list", "READ", listParams, now.minusMinutes(30), username, clientInfo);
        listOp.setStatus("COMPLETED");
        listOp.setEndTime(now.minusMinutes(29));
        listOp.setResult("Listed 5 items");
        operations.put(listOpId, listOp);
        addToIndex(commandNameIndex, "list", listOpId);
        addToIndex(operationTypeIndex, "READ", listOpId);
        
        // View command sample
        Map<String, Object> viewParams = getPooledParameters("view");
        viewParams.put("itemId", "WI-123");
        String viewOpId = UUID.randomUUID().toString();
        OperationMetadata viewOp = new OperationMetadata(
            viewOpId, "view", "READ", viewParams, now.minusMinutes(25), username, clientInfo);
        viewOp.setStatus("COMPLETED");
        viewOp.setEndTime(now.minusMinutes(24));
        viewOp.setResult("Displayed item WI-123");
        operations.put(viewOpId, viewOp);
        addToIndex(commandNameIndex, "view", viewOpId);
        addToIndex(operationTypeIndex, "READ", viewOpId);
        
        // Add command sample
        Map<String, Object> addParams = getPooledParameters("add");
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
        addToIndex(commandNameIndex, "add", addOpId);
        addToIndex(operationTypeIndex, "CREATE", addOpId);
        
        // Update command sample
        Map<String, Object> updateParams = getPooledParameters("update");
        updateParams.put("itemId", "WI-124");
        updateParams.put("status", "IN_PROGRESS");
        String updateOpId = UUID.randomUUID().toString();
        OperationMetadata updateOp = new OperationMetadata(
            updateOpId, "update", "UPDATE", updateParams, now.minusMinutes(15), username, clientInfo);
        updateOp.setStatus("COMPLETED");
        updateOp.setEndTime(now.minusMinutes(14));
        updateOp.setResult("Updated item WI-124");
        operations.put(updateOpId, updateOp);
        addToIndex(commandNameIndex, "update", updateOpId);
        addToIndex(operationTypeIndex, "UPDATE", updateOpId);
        
        // Failed operation sample
        Map<String, Object> failedParams = getPooledParameters("view");
        failedParams.put("itemId", "WI-999");
        String failedOpId = UUID.randomUUID().toString();
        OperationMetadata failedOp = new OperationMetadata(
            failedOpId, "view", "READ", failedParams, now.minusMinutes(10), username, clientInfo);
        failedOp.setStatus("FAILED");
        failedOp.setEndTime(now.minusMinutes(9));
        failedOp.setErrorMessage("Item not found: WI-999");
        operations.put(failedOpId, failedOp);
        addToIndex(commandNameIndex, "view", failedOpId);
        addToIndex(operationTypeIndex, "READ", failedOpId);
    }

    @Override
    public String startOperation(String commandName, String operationType, Map<String, Object> parameters) {
        // Check for rate limiting
        if (isRateLimited(commandName, parameters)) {
            // If rate limited, try to find an existing operation to aggregate with
            String aggregationKey = buildAggregationKey(commandName, parameters);
            if (operationAggregationMap.containsKey(aggregationKey)) {
                return operationAggregationMap.get(aggregationKey);
            }
        }
        
        // Generate operation ID
        String operationId = UUID.randomUUID().toString();
        
        // Get user info
        String username = System.getProperty("user.name", "unknown");
        String clientInfo = "CLI client " + System.getProperty("os.name");
        
        // Use defensive copy of parameters to prevent modification
        Map<String, Object> paramsCopy = parameters != null ? 
            new HashMap<>(parameters) : getPooledParameters("empty");
        
        // Create metadata and store
        OperationMetadata metadata = new OperationMetadata(
            operationId, commandName, operationType, paramsCopy, LocalDateTime.now(), username, clientInfo);
        operations.put(operationId, metadata);
        
        // Add to indexes for fast lookups
        addToIndex(commandNameIndex, commandName, operationId);
        addToIndex(operationTypeIndex, operationType, operationId);
        
        // For high-volume identical operations, store for aggregation
        if (operationRateLimits.getOrDefault(commandName, new AtomicInteger()).get() > 10) {
            String key = buildAggregationKey(commandName, parameters);
            operationAggregationMap.put(key, operationId);
        }
        
        // Invalidate statistics cache when operations are added
        statisticsCacheExpiry = 0;
        
        return operationId;
    }
    
    /**
     * Builds an aggregation key from command name and parameters.
     * 
     * @param commandName The command name
     * @param parameters The operation parameters
     * @return The aggregation key
     */
    private String buildAggregationKey(String commandName, Map<String, Object> parameters) {
        StringBuilder key = new StringBuilder(commandName);
        
        if (parameters != null) {
            for (Map.Entry<String, Object> entry : parameters.entrySet()) {
                if (isKeyParameter(entry.getKey())) {
                    key.append(":").append(entry.getKey()).append("=")
                       .append(entry.getValue() != null ? entry.getValue() : "null");
                }
            }
        }
        
        return key.toString();
    }

    @Override
    public void completeOperation(String operationId, Object result) {
        // Queue the completion for batch processing
        completionQueue.offer(new OperationUpdate(operationId, result, true));
        
        // For immediate visibility in the operations map
        OperationMetadata metadata = operations.get(operationId);
        if (metadata != null) {
            metadata.setStatus("COMPLETED");
            metadata.setEndTime(LocalDateTime.now());
            metadata.setResult(result);
        }
        
        // Invalidate statistics cache when operations are completed
        statisticsCacheExpiry = 0;
    }

    @Override
    public void failOperation(String operationId, Throwable exception) {
        // Queue the failure for batch processing
        completionQueue.offer(new OperationUpdate(operationId, exception, false));
        
        // For immediate visibility in the operations map
        OperationMetadata metadata = operations.get(operationId);
        if (metadata != null) {
            metadata.setStatus("FAILED");
            metadata.setEndTime(LocalDateTime.now());
            metadata.setErrorMessage(exception.getMessage());
        }
        
        // Invalidate statistics cache when operations fail
        statisticsCacheExpiry = 0;
    }

    @Override
    public OperationMetadata getOperationMetadata(String operationId) {
        return operations.get(operationId);
    }

    @Override
    public List<OperationMetadata> listOperations(String commandName, String operationType, int limit) {
        // First try using indexes for faster lookup
        if (commandName != null && operationType != null) {
            // If both filters are provided, use intersection of indexes
            List<String> commandIds = commandNameIndex.getOrDefault(commandName, new ArrayList<>());
            List<String> typeIds = operationTypeIndex.getOrDefault(operationType, new ArrayList<>());
            
            // Find intersection
            List<String> intersectionIds = new ArrayList<>(commandIds);
            intersectionIds.retainAll(typeIds);
            
            return getOperationsFromIds(intersectionIds, limit);
        } else if (commandName != null) {
            // If only command name filter is provided
            List<String> ids = commandNameIndex.getOrDefault(commandName, new ArrayList<>());
            return getOperationsFromIds(ids, limit);
        } else if (operationType != null) {
            // If only operation type filter is provided
            List<String> ids = operationTypeIndex.getOrDefault(operationType, new ArrayList<>());
            return getOperationsFromIds(ids, limit);
        }
        
        // Fallback to full search if no indexes can be used
        return operations.values().stream()
            .sorted(Comparator.comparing(OperationMetadata::getStartTime).reversed())
            .limit(limit)
            .collect(Collectors.toList());
    }
    
    /**
     * Gets operation metadata from a list of operation IDs.
     * 
     * @param ids The operation IDs
     * @param limit The maximum number of operations to return
     * @return The list of operation metadata
     */
    private List<OperationMetadata> getOperationsFromIds(List<String> ids, int limit) {
        List<OperationMetadata> result = new ArrayList<>();
        int count = 0;
        
        for (String id : ids) {
            OperationMetadata metadata = operations.get(id);
            if (metadata != null) {
                result.add(metadata);
                count++;
                if (count >= limit) {
                    break;
                }
            }
        }
        
        // Sort by start time descending
        result.sort(Comparator.comparing(OperationMetadata::getStartTime).reversed());
        return result;
    }

    @Override
    public Map<String, Object> getOperationStatistics(String commandName, LocalDateTime from, LocalDateTime to) {
        // Use cached statistics if available and not expired
        String cacheKey = buildStatisticsCacheKey(commandName, from, to);
        
        if (statisticsCacheExpiry > System.currentTimeMillis()) {
            Map<String, Object> cachedStats = statisticsCache.get(cacheKey);
            if (cachedStats != null) {
                return new HashMap<>(cachedStats);
            }
        }
        
        // Initialize statistics map
        Map<String, Object> statistics = new HashMap<>();
        
        // Use optimized filtering
        List<OperationMetadata> filteredOps;
        
        if (commandName != null) {
            // Use command name index for filtering
            List<String> ids = commandNameIndex.getOrDefault(commandName, new ArrayList<>());
            filteredOps = ids.stream()
                .map(operations::get)
                .filter(op -> op != null)
                .filter(op -> from == null || !op.getStartTime().isBefore(from))
                .filter(op -> to == null || !op.getStartTime().isAfter(to))
                .collect(Collectors.toList());
        } else {
            // No command name filter, so use standard filtering
            filteredOps = operations.values().stream()
                .filter(op -> from == null || !op.getStartTime().isBefore(from))
                .filter(op -> to == null || !op.getStartTime().isAfter(to))
                .collect(Collectors.toList());
        }
        
        // Calculate total operations
        statistics.put("totalOperations", filteredOps.size());
        
        // Calculate completed operations
        long completedOps = filteredOps.stream()
            .filter(op -> "COMPLETED".equals(op.getStatus()))
            .count();
        statistics.put("completedOperations", completedOps);
        
        // Calculate failed operations
        long failedOps = filteredOps.stream()
            .filter(op -> "FAILED".equals(op.getStatus()))
            .count();
        statistics.put("failedOperations", failedOps);
        
        // Calculate success rate
        double successRate = filteredOps.isEmpty() ? 0 : 
            (double) completedOps / filteredOps.size() * 100;
        statistics.put("successRate", successRate);
        
        // Calculate average duration
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
        
        // Cache statistics
        statisticsCache.put(cacheKey, new HashMap<>(statistics));
        statisticsCacheExpiry = System.currentTimeMillis() + statisticsCacheTtlMs;
        
        return statistics;
    }
    
    /**
     * Builds a cache key for statistics.
     * 
     * @param commandName The command name filter
     * @param from The start time filter
     * @param to The end time filter
     * @return The cache key
     */
    private String buildStatisticsCacheKey(String commandName, LocalDateTime from, LocalDateTime to) {
        StringBuilder key = new StringBuilder();
        key.append("stats:");
        key.append(commandName != null ? commandName : "all");
        key.append(":");
        key.append(from != null ? from.toString() : "nostart");
        key.append(":");
        key.append(to != null ? to.toString() : "noend");
        return key.toString();
    }

    @Override
    public int clearOperationHistory(int days) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(days);
        
        // Identify operations to remove
        List<String> keysToRemove = operations.entrySet().stream()
            .filter(entry -> entry.getValue().getStartTime().isBefore(cutoffDate))
            .map(Map.Entry::getKey)
            .collect(Collectors.toList());
        
        // Remove operations and update indexes
        for (String key : keysToRemove) {
            OperationMetadata metadata = operations.remove(key);
            
            if (metadata != null) {
                removeFromIndex(commandNameIndex, metadata.getCommandName(), key);
                removeFromIndex(operationTypeIndex, metadata.getOperationType(), key);
            }
        }
        
        // Clean up recent operations
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
        
        // Invalidate statistics cache
        statisticsCache.clear();
        statisticsCacheExpiry = 0;
        
        return keysToRemove.size();
    }

    @Override
    public void trackOperationError(String parentOperationId, String operationName, 
                                  String errorMessage, Exception exception) {
        OperationMetadata metadata = operations.get(parentOperationId);
        if (metadata != null) {
            // Create a sub-operation map to track this specific error
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("operation", operationName);
            errorData.put("errorMessage", errorMessage);
            errorData.put("exceptionType", exception.getClass().getSimpleName());
            
            // Use a separate executor for detail updates to not block the main queue
            asyncExecutor.execute(() -> {
                // Update the parent operation with this error information
                if (metadata.getParameters() == null) {
                    metadata.getParameters().put("errors", new ArrayList<Map<String, Object>>());
                }
                
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> errors = (List<Map<String, Object>>) 
                    metadata.getParameters().getOrDefault("errors", new ArrayList<Map<String, Object>>());
                errors.add(errorData);
                metadata.getParameters().put("errors", errors);
                
                // Also update the error message in the main operation if not already set
                if (metadata.getErrorMessage() == null) {
                    metadata.setErrorMessage(errorMessage);
                }
            });
        }
    }

    @Override
    public void trackOperationDetail(String operationId, String key, Object value) {
        // Use async execution for detail tracking to avoid blocking
        asyncExecutor.execute(() -> {
            OperationMetadata metadata = operations.get(operationId);
            if (metadata != null && key != null) {
                // Create the details map if it doesn't exist
                if (!metadata.getParameters().containsKey("details")) {
                    metadata.getParameters().put("details", new HashMap<String, Object>());
                }
                
                // Add or update the detail
                @SuppressWarnings("unchecked")
                Map<String, Object> details = (Map<String, Object>) metadata.getParameters().get("details");
                details.put(key, value);
                
                // Update the recent operations list if this is a completed operation
                if ("COMPLETED".equals(metadata.getStatus())) {
                    synchronized (recentOperations) {
                        for (Map<String, Object> op : recentOperations) {
                            if (operationId.equals(op.get("id"))) {
                                // Update the details in the recent operations list
                                @SuppressWarnings("unchecked")
                                Map<String, Object> recentOpDetails = (Map<String, Object>) 
                                    op.getOrDefault("details", new HashMap<String, Object>());
                                recentOpDetails.put(key, value);
                                op.put("details", recentOpDetails);
                                break;
                            }
                        }
                    }
                }
            }
        });
    }
    
    /**
     * Gets a list of recent operations in simplified format.
     * 
     * @param limit The maximum number of operations to return
     * @return The list of operations
     */
    public List<Map<String, Object>> getRecentOperations(int limit) {
        synchronized (recentOperations) {
            if (limit <= 0 || limit >= recentOperations.size()) {
                return new ArrayList<>(recentOperations);
            } else {
                return new ArrayList<>(recentOperations.subList(0, limit));
            }
        }
    }
    
    /**
     * Adds a parameter combination to the parameter pool.
     * 
     * @param key The pool key
     * @param parameters The parameters to cache
     */
    public void addToParameterPool(String key, Map<String, Object> parameters) {
        parameterPool.put(key, new HashMap<>(parameters));
    }
    
    /**
     * Sets rate limit threshold for a specific command.
     * 
     * @param commandName The command name
     * @param threshold The rate limit threshold
     */
    public void setRateLimit(String commandName, int threshold) {
        // Implementation for configuring rate limits
        // This would update an internal configuration map
    }
    
    /**
     * Shut down the executor services.
     */
    public void shutdown() {
        scheduledExecutor.shutdown();
        asyncExecutor.shutdown();
        
        try {
            // Process any pending operations
            processPendingOperations();
            
            // Wait for tasks to complete
            if (!scheduledExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }
            if (!asyncExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                asyncExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduledExecutor.shutdownNow();
            asyncExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}