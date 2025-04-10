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

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.rinna.cli.service.MetadataService.OperationMetadata;

/**
 * Test class for the OptimizedMetadataService.
 * This test focuses on verifying the performance benefits of the optimized implementation.
 */
@Tag("unit")
class OptimizedMetadataServiceTest {
    
    private OptimizedMetadataService optimizedService;
    private MockMetadataService mockService;
    private ExecutorService executorService;
    
    @BeforeEach
    void setUp() {
        // Create new instances for each test
        optimizedService = new OptimizedMetadataService(4, 50, 60);
        mockService = MockMetadataService.getInstance();
        executorService = Executors.newFixedThreadPool(8);
    }
    
    @AfterEach
    void tearDown() {
        optimizedService.shutdown();
        executorService.shutdown();
    }
    
    /**
     * Tests the optimized service for high-volume operation tracking.
     * This test verifies that the service can handle a large number of operations efficiently.
     */
    @Test
    void testHighVolumeOperationTracking() throws Exception {
        // Number of operations to test
        int numOperations = 1000;
        
        // Track start time
        long startTimeOptimized = System.currentTimeMillis();
        
        // Create a latch to wait for all operations to complete
        CountDownLatch latchOptimized = new CountDownLatch(numOperations);
        
        // Track operations concurrently
        for (int i = 0; i < numOperations; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    Map<String, Object> params = new HashMap<>();
                    params.put("index", index);
                    params.put("threadId", Thread.currentThread().getId());
                    
                    String opId = optimizedService.startOperation("test", "READ", params);
                    
                    // Simulate some processing
                    Thread.sleep(1);
                    
                    // Complete the operation
                    optimizedService.completeOperation(opId, "Result for " + index);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latchOptimized.countDown();
                }
            });
        }
        
        // Wait for all operations to complete or timeout after 10 seconds
        boolean completed = latchOptimized.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "Not all operations completed in time");
        
        // Track end time
        long endTimeOptimized = System.currentTimeMillis();
        long durationOptimized = endTimeOptimized - startTimeOptimized;
        
        // Now test the mock service for comparison
        long startTimeMock = System.currentTimeMillis();
        
        // Create a latch to wait for all operations to complete
        CountDownLatch latchMock = new CountDownLatch(numOperations);
        
        // Track operations concurrently
        for (int i = 0; i < numOperations; i++) {
            final int index = i;
            executorService.submit(() -> {
                try {
                    Map<String, Object> params = new HashMap<>();
                    params.put("index", index);
                    params.put("threadId", Thread.currentThread().getId());
                    
                    String opId = mockService.startOperation("test", "READ", params);
                    
                    // Simulate some processing
                    Thread.sleep(1);
                    
                    // Complete the operation
                    mockService.completeOperation(opId, "Result for " + index);
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    latchMock.countDown();
                }
            });
        }
        
        // Wait for all operations to complete or timeout after 10 seconds
        completed = latchMock.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "Not all mock operations completed in time");
        
        // Track end time
        long endTimeMock = System.currentTimeMillis();
        long durationMock = endTimeMock - startTimeMock;
        
        // The optimized service should handle operations more efficiently
        System.out.println("Optimized service duration: " + durationOptimized + "ms");
        System.out.println("Mock service duration: " + durationMock + "ms");
        
        // Verify that all operations were tracked
        List<OperationMetadata> operations = optimizedService.listOperations("test", "READ", numOperations);
        assertEquals(numOperations, operations.size(), "Not all operations were tracked");
    }
    
    /**
     * Tests the optimized service for batch operation completion.
     * This test verifies that the service can efficiently handle batched operation completions.
     */
    @Test
    void testBatchOperationCompletion() throws Exception {
        // Number of operations to test
        int numOperations = 500;
        
        // Store operation IDs for later completion
        List<String> operationIds = new ArrayList<>();
        
        // Start operations but don't complete them yet
        for (int i = 0; i < numOperations; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put("index", i);
            
            String opId = optimizedService.startOperation("batch-test", "BATCH", params);
            operationIds.add(opId);
        }
        
        // Verify that all operations were started
        List<OperationMetadata> pendingOps = optimizedService.listOperations("batch-test", "BATCH", numOperations);
        assertEquals(numOperations, pendingOps.size(), "Not all operations were started");
        
        // Verify that all operations are in progress
        for (OperationMetadata op : pendingOps) {
            assertEquals("IN_PROGRESS", op.getStatus(), "Operation should be in progress");
        }
        
        // Now complete all operations in parallel
        long startTime = System.currentTimeMillis();
        
        CountDownLatch latch = new CountDownLatch(numOperations);
        
        for (String opId : operationIds) {
            executorService.submit(() -> {
                try {
                    optimizedService.completeOperation(opId, "Batch result");
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all completions
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        assertTrue(completed, "Not all operations were completed in time");
        
        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;
        
        System.out.println("Batch completion duration: " + duration + "ms");
        
        // Give a little time for batch processing to complete
        Thread.sleep(500);
        
        // Verify that all operations were completed
        List<OperationMetadata> completedOps = optimizedService.listOperations("batch-test", "BATCH", numOperations);
        
        int completedCount = 0;
        for (OperationMetadata op : completedOps) {
            if ("COMPLETED".equals(op.getStatus())) {
                completedCount++;
            }
        }
        
        assertEquals(numOperations, completedCount, "Not all operations were marked as completed");
    }
    
    /**
     * Tests the optimized service for statistics generation performance.
     * This test verifies that the service can efficiently generate statistics.
     */
    @Test
    void testStatisticsPerformance() throws Exception {
        // Generate a large number of operations
        int numOperations = 2000;
        
        // Create different types of operations
        List<String> commandNames = List.of("list", "view", "add", "update", "delete");
        List<String> operationTypes = List.of("READ", "CREATE", "UPDATE", "DELETE");
        
        // Create operations
        for (int i = 0; i < numOperations; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put("index", i);
            
            String commandName = commandNames.get(i % commandNames.size());
            String operationType = operationTypes.get(i % operationTypes.size());
            
            String opId = optimizedService.startOperation(commandName, operationType, params);
            
            // Complete most operations, fail some
            if (i % 10 != 0) {
                optimizedService.completeOperation(opId, "Result " + i);
            } else {
                optimizedService.failOperation(opId, new RuntimeException("Test failure"));
            }
        }
        
        // Measure the time to generate statistics for different use cases
        
        // Case 1: Overall statistics
        long startTime1 = System.currentTimeMillis();
        Map<String, Object> overallStats = optimizedService.getOperationStatistics(null, null, null);
        long duration1 = System.currentTimeMillis() - startTime1;
        
        // Case 2: Filtered by command
        long startTime2 = System.currentTimeMillis();
        Map<String, Object> listStats = optimizedService.getOperationStatistics("list", null, null);
        long duration2 = System.currentTimeMillis() - startTime2;
        
        // Case 3: Filtered by time
        LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
        long startTime3 = System.currentTimeMillis();
        Map<String, Object> recentStats = optimizedService.getOperationStatistics(null, oneMinuteAgo, null);
        long duration3 = System.currentTimeMillis() - startTime3;
        
        // Case 4: Second call to same statistics (should use cache)
        long startTime4 = System.currentTimeMillis();
        Map<String, Object> cachedStats = optimizedService.getOperationStatistics(null, null, null);
        long duration4 = System.currentTimeMillis() - startTime4;
        
        System.out.println("Overall statistics generation: " + duration1 + "ms");
        System.out.println("Filtered statistics generation: " + duration2 + "ms");
        System.out.println("Time-filtered statistics generation: " + duration3 + "ms");
        System.out.println("Cached statistics retrieval: " + duration4 + "ms");
        
        // Verify that cached retrieval is significantly faster
        assertTrue(duration4 < duration1, "Cached retrieval should be faster");
        
        // Verify statistics content
        assertEquals(numOperations, (int) overallStats.get("totalOperations"), "Total operations count incorrect");
        
        // Verify command filtering
        int expectedListOps = numOperations / commandNames.size();
        assertEquals(expectedListOps, (int) listStats.get("totalOperations"), "Filtered operations count incorrect");
    }
    
    /**
     * Tests the optimized service for concurrent read/write operations.
     * This test verifies that the service can handle concurrent access without corruption.
     */
    @Test
    void testConcurrentReadWrite() throws Exception {
        // Number of operations to perform concurrently
        int numThreads = 16;
        int opsPerThread = 50;
        
        // Create a shared map to track results
        Map<String, Object> results = new ConcurrentHashMap<>();
        
        // Create a countdown latch to wait for all threads
        CountDownLatch latch = new CountDownLatch(numThreads);
        
        // Start threads that will perform read and write operations concurrently
        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            executorService.submit(() -> {
                try {
                    for (int j = 0; j < opsPerThread; j++) {
                        int opType = j % 3;
                        
                        switch (opType) {
                            case 0: // Write operation
                                Map<String, Object> params = new HashMap<>();
                                params.put("threadNum", threadNum);
                                params.put("iteration", j);
                                
                                String opId = optimizedService.startOperation("concurrent-test", "WRITE", params);
                                results.put("write-" + threadNum + "-" + j, opId);
                                
                                // 50% complete, 50% fail
                                if (j % 2 == 0) {
                                    optimizedService.completeOperation(opId, "Result " + j);
                                } else {
                                    optimizedService.failOperation(opId, new RuntimeException("Test failure"));
                                }
                                break;
                                
                            case 1: // Read operation
                                List<OperationMetadata> ops = optimizedService.listOperations("concurrent-test", null, 10);
                                results.put("read-" + threadNum + "-" + j, ops.size());
                                break;
                                
                            case 2: // Stats operation
                                Map<String, Object> stats = optimizedService.getOperationStatistics("concurrent-test", null, null);
                                results.put("stats-" + threadNum + "-" + j, stats.get("totalOperations"));
                                break;
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    results.put("error-" + threadNum, e.getMessage());
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        boolean completed = latch.await(30, TimeUnit.SECONDS);
        assertTrue(completed, "Not all threads completed in time");
        
        // Verify that no errors occurred
        List<String> errors = results.entrySet().stream()
            .filter(e -> e.getKey().startsWith("error-"))
            .map(e -> e.getKey() + ": " + e.getValue())
            .collect(Collectors.toList());
        
        assertTrue(errors.isEmpty(), "Errors occurred during concurrent execution: " + errors);
        
        // Verify the final state
        List<OperationMetadata> finalOps = optimizedService.listOperations("concurrent-test", "WRITE", 1000);
        int expectedOps = numThreads * opsPerThread / 3; // Only 1/3 of operations are writes
        assertEquals(expectedOps, finalOps.size(), "Wrong number of operations tracked");
    }
    
    /**
     * Tests the rate limiting capabilities of the optimized service.
     */
    @Test
    void testRateLimiting() throws Exception {
        // Create a large number of identical operations
        int numOperations = 100;
        
        // Use a common set of parameters
        Map<String, Object> params = new HashMap<>();
        params.put("itemId", "WI-123");
        params.put("operation", "view");
        
        // Track operation IDs
        Set<String> operationIds = ConcurrentHashMap.newKeySet();
        
        // Execute operations rapidly
        for (int i = 0; i < numOperations; i++) {
            String opId = optimizedService.startOperation("rate-limit-test", "READ", params);
            operationIds.add(opId);
            optimizedService.completeOperation(opId, "Result " + i);
        }
        
        // Due to rate limiting, we should have fewer unique operation IDs than operations
        assertTrue(operationIds.size() < numOperations, 
                   "Rate limiting should have reduced the number of unique operations");
        
        System.out.println("Operations requested: " + numOperations);
        System.out.println("Unique operations created: " + operationIds.size());
    }
    
    /**
     * Tests the parameter pooling capabilities of the optimized service.
     */
    @Test
    void testParameterPooling() throws Exception {
        // Add a custom parameter configuration to the pool
        Map<String, Object> customParams = new HashMap<>();
        customParams.put("operation", "custom");
        customParams.put("format", "text");
        customParams.put("verbose", false);
        
        optimizedService.addToParameterPool("custom-op", customParams);
        
        // Start an operation using the pooled parameters
        String opId = optimizedService.startOperation(
            "param-pool-test", "READ", optimizedService.getPooledParameters("custom-op"));
        
        // Verify that the operation has the correct parameters
        OperationMetadata metadata = optimizedService.getOperationMetadata(opId);
        assertEquals("custom", metadata.getParameters().get("operation"));
        assertEquals("text", metadata.getParameters().get("format"));
        assertEquals(false, metadata.getParameters().get("verbose"));
    }
    
    /**
     * Tests the performance of the list operations method with different filtering approaches.
     */
    @Test
    void testListOperationsPerformance() throws Exception {
        // Create a large number of operations with different command names and types
        int numOperations = 1000;
        String[] commandNames = {"list", "view", "add", "update", "delete"};
        String[] operationTypes = {"READ", "CREATE", "UPDATE", "DELETE"};
        
        // Create operations
        for (int i = 0; i < numOperations; i++) {
            Map<String, Object> params = new HashMap<>();
            params.put("index", i);
            
            String commandName = commandNames[i % commandNames.length];
            String operationType = operationTypes[i % operationTypes.length];
            
            String opId = optimizedService.startOperation(commandName, operationType, params);
            optimizedService.completeOperation(opId, "Result " + i);
        }
        
        // Measure the time to list operations with different filters
        
        // Case 1: No filters
        long startTime1 = System.currentTimeMillis();
        List<OperationMetadata> allOps = optimizedService.listOperations(null, null, numOperations);
        long duration1 = System.currentTimeMillis() - startTime1;
        
        // Case 2: Command name filter
        long startTime2 = System.currentTimeMillis();
        List<OperationMetadata> listOps = optimizedService.listOperations("list", null, numOperations);
        long duration2 = System.currentTimeMillis() - startTime2;
        
        // Case 3: Operation type filter
        long startTime3 = System.currentTimeMillis();
        List<OperationMetadata> readOps = optimizedService.listOperations(null, "READ", numOperations);
        long duration3 = System.currentTimeMillis() - startTime3;
        
        // Case 4: Both filters
        long startTime4 = System.currentTimeMillis();
        List<OperationMetadata> listReadOps = optimizedService.listOperations("list", "READ", numOperations);
        long duration4 = System.currentTimeMillis() - startTime4;
        
        System.out.println("List all operations: " + duration1 + "ms");
        System.out.println("List by command name: " + duration2 + "ms");
        System.out.println("List by operation type: " + duration3 + "ms");
        System.out.println("List by both filters: " + duration4 + "ms");
        
        // Verify operation counts
        assertEquals(numOperations, allOps.size(), "All operations count incorrect");
        
        int expectedListOps = numOperations / commandNames.length;
        assertTrue(Math.abs(listOps.size() - expectedListOps) <= 1, "List operations count incorrect");
        
        int expectedReadOps = numOperations / operationTypes.length;
        assertTrue(Math.abs(readOps.size() - expectedReadOps) <= 1, "Read operations count incorrect");
        
        int expectedListReadOps = numOperations / (commandNames.length * operationTypes.length);
        assertTrue(Math.abs(listReadOps.size() - expectedListReadOps) <= 1, "List-Read operations count incorrect");
    }
}