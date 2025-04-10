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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Utility class for optimizing metadata tracking for high-volume operations.
 * This class provides:
 * 1. Batch operation tracking to reduce overhead
 * 2. Caching of common parameters to avoid repeated creation
 * 3. Asynchronous operation completion to avoid blocking the command thread
 * 4. Rate limiting to prevent overwhelming the metadata service
 */
public class MetadataOptimizer {
    
    private final MetadataService metadataService;
    private final Map<String, Object> parameterCache = new ConcurrentHashMap<>();
    private final ExecutorService asyncExecutor;
    private final int batchSize;
    private final int maxQueueSize;
    private final long maxWaitTimeMs;
    
    // For tracking batch operations
    private final List<Map.Entry<String, Object>> completionQueue = new ArrayList<>();
    private final AtomicInteger queueSize = new AtomicInteger(0);
    private long lastFlushTime = System.currentTimeMillis();
    
    /**
     * Creates a new metadata optimizer with default settings.
     *
     * @param metadataService the metadata service to optimize
     */
    public MetadataOptimizer(MetadataService metadataService) {
        this(metadataService, 50, 1000, 2000, 2);
    }
    
    /**
     * Creates a new metadata optimizer with custom settings.
     *
     * @param metadataService the metadata service to optimize
     * @param batchSize the number of operations to batch before flushing
     * @param maxQueueSize the maximum size of the queue before forcing a flush
     * @param maxWaitTimeMs the maximum time to wait before flushing the queue
     * @param threadPoolSize the number of threads in the async executor pool
     */
    public MetadataOptimizer(MetadataService metadataService, int batchSize, int maxQueueSize, 
                            long maxWaitTimeMs, int threadPoolSize) {
        this.metadataService = metadataService;
        this.batchSize = batchSize;
        this.maxQueueSize = maxQueueSize;
        this.maxWaitTimeMs = maxWaitTimeMs;
        this.asyncExecutor = Executors.newFixedThreadPool(threadPoolSize);
        
        // Initialize the parameter cache with common parameters
        initializeParameterCache();
        
        // Start a background thread to periodically flush the queue
        startFlushThread();
    }
    
    /**
     * Initializes the parameter cache with common parameter maps to avoid
     * repeated creation of the same objects.
     */
    private void initializeParameterCache() {
        // Cache empty parameters map
        Map<String, Object> emptyParams = new HashMap<>();
        parameterCache.put("empty", emptyParams);
        
        // Cache common parameter combinations
        Map<String, Object> listParams = new HashMap<>();
        listParams.put("operation", "list");
        parameterCache.put("list", listParams);
        
        Map<String, Object> viewParams = new HashMap<>();
        viewParams.put("operation", "view");
        parameterCache.put("view", viewParams);
        
        Map<String, Object> addParams = new HashMap<>();
        addParams.put("operation", "add");
        parameterCache.put("add", addParams);
        
        Map<String, Object> updateParams = new HashMap<>();
        updateParams.put("operation", "update");
        parameterCache.put("update", updateParams);
        
        Map<String, Object> deleteParams = new HashMap<>();
        deleteParams.put("operation", "delete");
        parameterCache.put("delete", deleteParams);
    }
    
    /**
     * Starts a background thread to periodically flush the operation queue.
     */
    private void startFlushThread() {
        Thread flushThread = new Thread(() -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    long currentTime = System.currentTimeMillis();
                    if ((currentTime - lastFlushTime > maxWaitTimeMs && queueSize.get() > 0) || 
                        queueSize.get() >= maxQueueSize) {
                        flushQueue();
                    }
                    Thread.sleep(100); // Check every 100ms
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        flushThread.setDaemon(true);
        flushThread.setName("MetadataOptimizer-FlushThread");
        flushThread.start();
    }
    
    /**
     * Gets cached parameters by key, or creates a new map if not cached.
     *
     * @param key the cache key
     * @return the cached parameters or a new map
     */
    public Map<String, Object> getCachedParameters(String key) {
        return new HashMap<>((Map<String, Object>) parameterCache.getOrDefault(key, new HashMap<>()));
    }
    
    /**
     * Adds parameters to the cache.
     *
     * @param key the cache key
     * @param parameters the parameters to cache
     */
    public void cacheParameters(String key, Map<String, Object> parameters) {
        parameterCache.put(key, new HashMap<>(parameters));
    }
    
    /**
     * Starts an operation with cached parameters.
     *
     * @param commandName the command name
     * @param operationType the operation type
     * @param parameterCacheKey the key for cached parameters
     * @return the operation ID
     */
    public String startOperationWithCachedParams(String commandName, String operationType, String parameterCacheKey) {
        Map<String, Object> parameters = getCachedParameters(parameterCacheKey);
        return metadataService.startOperation(commandName, operationType, parameters);
    }
    
    /**
     * Completes an operation asynchronously to avoid blocking the command thread.
     *
     * @param operationId the operation ID
     * @param result the result of the operation
     */
    public void completeOperationAsync(String operationId, Object result) {
        asyncExecutor.submit(() -> metadataService.completeOperation(operationId, result));
    }
    
    /**
     * Fails an operation asynchronously to avoid blocking the command thread.
     *
     * @param operationId the operation ID
     * @param exception the exception that caused the failure
     */
    public void failOperationAsync(String operationId, Throwable exception) {
        asyncExecutor.submit(() -> metadataService.failOperation(operationId, exception));
    }
    
    /**
     * Adds an operation to the batch completion queue.
     * The queue will be flushed when it reaches the batch size.
     *
     * @param operationId the operation ID
     * @param result the result of the operation
     */
    public synchronized void batchCompleteOperation(String operationId, Object result) {
        completionQueue.add(Map.entry(operationId, result));
        int size = queueSize.incrementAndGet();
        
        if (size >= batchSize) {
            flushQueue();
        }
    }
    
    /**
     * Flushes the operation queue, completing all queued operations.
     */
    public synchronized void flushQueue() {
        if (queueSize.get() > 0) {
            List<Map.Entry<String, Object>> operations;
            synchronized (completionQueue) {
                operations = new ArrayList<>(completionQueue);
                completionQueue.clear();
                queueSize.set(0);
            }
            
            // Process operations in chunks to avoid blocking for too long
            int chunkSize = 10;
            for (int i = 0; i < operations.size(); i += chunkSize) {
                int end = Math.min(i + chunkSize, operations.size());
                List<Map.Entry<String, Object>> chunk = operations.subList(i, end);
                
                asyncExecutor.submit(() -> {
                    for (Map.Entry<String, Object> entry : chunk) {
                        metadataService.completeOperation(entry.getKey(), entry.getValue());
                    }
                });
            }
            
            lastFlushTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Performs a batch operation on a list of items, tracking each operation efficiently.
     *
     * @param <T> the type of items in the list
     * @param commandName the command name
     * @param operationType the operation type
     * @param items the items to process
     * @param parameterSupplier a function to create parameters for each item
     * @param processor a function to process each item
     * @return the number of successfully processed items
     */
    public <T> int processBatch(String commandName, String operationType, List<T> items,
                               java.util.function.Function<T, Map<String, Object>> parameterSupplier,
                               Consumer<T> processor) {
        AtomicInteger successCount = new AtomicInteger(0);
        
        // Process items in parallel for large batches, or sequentially for small batches
        if (items.size() > 100) {
            items.parallelStream().forEach(item -> {
                Map<String, Object> parameters = parameterSupplier.apply(item);
                String operationId = metadataService.startOperation(commandName, operationType, parameters);
                
                try {
                    processor.accept(item);
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("item", item.toString());
                    batchCompleteOperation(operationId, result);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failOperationAsync(operationId, e);
                }
            });
        } else {
            for (T item : items) {
                Map<String, Object> parameters = parameterSupplier.apply(item);
                String operationId = metadataService.startOperation(commandName, operationType, parameters);
                
                try {
                    processor.accept(item);
                    Map<String, Object> result = new HashMap<>();
                    result.put("success", true);
                    result.put("item", item.toString());
                    batchCompleteOperation(operationId, result);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failOperationAsync(operationId, e);
                }
            }
        }
        
        // Make sure all operations are completed
        flushQueue();
        
        return successCount.get();
    }
    
    /**
     * Shuts down the async executor and processes any remaining operations.
     *
     * @param timeoutSeconds the timeout in seconds to wait for completion
     * @return true if the executor was successfully shut down, false if the timeout was reached
     */
    public boolean shutdown(int timeoutSeconds) {
        // Process any remaining operations
        flushQueue();
        
        // Shut down the executor
        asyncExecutor.shutdown();
        try {
            return asyncExecutor.awaitTermination(timeoutSeconds, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }
}