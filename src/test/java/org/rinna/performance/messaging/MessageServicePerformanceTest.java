/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.performance.messaging;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Execution;
import org.junit.jupiter.api.ExecutionMode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.rinna.performance.base.PerformanceTest;
import org.rinna.cli.messaging.MessageStatus;
import org.rinna.cli.messaging.RinnaMessage;
import org.rinna.cli.service.MockMessageService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Performance tests for MessageService operations.
 */
@Tag("performance")
@Execution(ExecutionMode.SAME_THREAD)
// Performance tests are not typically part of smoke tests as they take longer to run
// and are more resource-intensive
class MessageServicePerformanceTest extends PerformanceTest {

    private MockMessageService messageService;
    private static final int WARMUP_COUNT = 3;
    private static final int MEASUREMENT_COUNT = 5;
    
    /**
     * Assert that the measured performance meets the expectation.
     *
     * @param actual The actual duration measured
     * @param threshold The maximum acceptable duration
     * @param operation Description of the operation being tested
     */
    private void assertPerformance(Duration actual, Duration threshold, String operation) {
        logger.info("Performance test for {}: actual duration {}ms, threshold {}ms", 
                operation, actual.toMillis(), threshold.toMillis());
        
        Assertions.assertTrue(
            actual.compareTo(threshold) <= 0,
            String.format("Performance test failed for %s: %dms exceeded threshold of %dms", 
                operation, actual.toMillis(), threshold.toMillis())
        );
    }
    
    @BeforeEach
    void setUp() {
        messageService = new MockMessageService();
    }
    
    /**
     * Setup test data for performance testing.
     *
     * @param messageCount number of messages to create
     * @return list of message IDs
     */
    private List<String> setupTestMessages(int messageCount) {
        List<String> messageIds = new ArrayList<>();
        
        for (int i = 0; i < messageCount; i++) {
            String messageId = "perf-msg-" + UUID.randomUUID().toString().substring(0, 8);
            RinnaMessage message = new RinnaMessage(
                messageId,
                "eric",
                "steve",
                "Performance test message #" + i,
                "Tracer",
                Instant.now(),
                MessageStatus.UNREAD
            );
            messageService.sendMessage(message);
            messageIds.add(messageId);
        }
        
        return messageIds;
    }
    
    /**
     * Test message sending performance with various message sizes.
     *
     * @param messageSizeKb the size of the message in kilobytes
     */
    @ParameterizedTest
    @ValueSource(ints = {1, 10, 50, 100})
    @DisplayName("Test sending messages of different sizes")
    void testSendMessagePerformance(int messageSizeKb) {
        // Generate a message of specified size
        StringBuilder messageContent = new StringBuilder(messageSizeKb * 1024);
        for (int i = 0; i < messageSizeKb * 1024 / 10; i++) {
            messageContent.append("0123456789");
        }
        
        final String content = messageContent.toString();
        
        // Warm up
        for (int i = 0; i < WARMUP_COUNT; i++) {
            sendSingleMessage(content);
        }
        
        // Measure
        long totalDuration = 0;
        for (int i = 0; i < MEASUREMENT_COUNT; i++) {
            long start = System.nanoTime();
            sendSingleMessage(content);
            long duration = System.nanoTime() - start;
            totalDuration += duration;
        }
        
        long averageDuration = totalDuration / MEASUREMENT_COUNT;
        double averageDurationMs = averageDuration / 1_000_000.0;
        
        System.out.printf("Average time to send a %d KB message: %.2f ms%n", messageSizeKb, averageDurationMs);
        
        // Set performance thresholds based on message size
        long maxDurationMs = switch (messageSizeKb) {
            case 1 -> 10;    // 10 ms for 1KB
            case 10 -> 20;   // 20 ms for 10KB
            case 50 -> 50;   // 50 ms for 50KB
            case 100 -> 100; // 100 ms for 100KB
            default -> 150;  // Default threshold
        };
        
        // Assert performance meets expectations
        assertPerformance(
            Duration.ofNanos(averageDuration),
            Duration.ofMillis(maxDurationMs),
            "Sending a " + messageSizeKb + "KB message"
        );
    }
    
    /**
     * Helper method to send a single message.
     *
     * @param content the message content
     * @return the message ID
     */
    private String sendSingleMessage(String content) {
        String messageId = "perf-msg-" + UUID.randomUUID().toString().substring(0, 8);
        RinnaMessage message = new RinnaMessage(
            messageId,
            "eric",
            "steve",
            content,
            "Tracer",
            Instant.now(),
            MessageStatus.UNREAD
        );
        messageService.sendMessage(message);
        return messageId;
    }
    
    /**
     * Test performance of retrieving messages with varying inbox sizes.
     *
     * @param messageCount the number of messages in the inbox
     */
    @ParameterizedTest
    @ValueSource(ints = {10, 100, 1000})
    @DisplayName("Test retrieving messages with different inbox sizes")
    void testGetMessagesPerformance(int messageCount) {
        // Setup test data
        setupTestMessages(messageCount);
        
        // Warm up
        for (int i = 0; i < WARMUP_COUNT; i++) {
            messageService.getMessagesForUser("steve");
        }
        
        // Measure
        long totalDuration = 0;
        for (int i = 0; i < MEASUREMENT_COUNT; i++) {
            long start = System.nanoTime();
            messageService.getMessagesForUser("steve");
            long duration = System.nanoTime() - start;
            totalDuration += duration;
        }
        
        long averageDuration = totalDuration / MEASUREMENT_COUNT;
        double averageDurationMs = averageDuration / 1_000_000.0;
        
        System.out.printf("Average time to retrieve %d messages: %.2f ms%n", messageCount, averageDurationMs);
        
        // Set performance thresholds based on message count
        long maxDurationMs = switch (messageCount) {
            case 10 -> 5;      // 5 ms for 10 messages
            case 100 -> 15;    // 15 ms for 100 messages
            case 1000 -> 50;   // 50 ms for 1000 messages
            default -> 100;    // Default threshold
        };
        
        // Assert performance meets expectations
        assertPerformance(
            Duration.ofNanos(averageDuration),
            Duration.ofMillis(maxDurationMs),
            "Retrieving " + messageCount + " messages"
        );
    }
    
    /**
     * Test filtering messages by sender performance.
     */
    @Test
    @DisplayName("Test filtering messages by sender performance")
    void testFilterMessagesBySenderPerformance() {
        final int MESSAGE_COUNT = 1000;
        
        // Setup test data with multiple senders
        for (int i = 0; i < MESSAGE_COUNT / 2; i++) {
            String messageId = "perf-msg-eric-" + UUID.randomUUID().toString().substring(0, 8);
            RinnaMessage message = new RinnaMessage(
                messageId,
                "eric",
                "steve",
                "Message from Eric #" + i,
                "Tracer",
                Instant.now(),
                MessageStatus.UNREAD
            );
            messageService.sendMessage(message);
        }
        
        for (int i = 0; i < MESSAGE_COUNT / 2; i++) {
            String messageId = "perf-msg-maria-" + UUID.randomUUID().toString().substring(0, 8);
            RinnaMessage message = new RinnaMessage(
                messageId,
                "maria",
                "steve",
                "Message from Maria #" + i,
                "Quantum",
                Instant.now(),
                MessageStatus.UNREAD
            );
            messageService.sendMessage(message);
        }
        
        // Warm up
        for (int i = 0; i < WARMUP_COUNT; i++) {
            messageService.getMessagesForUserBySender("steve", "eric");
        }
        
        // Measure
        long totalDuration = 0;
        for (int i = 0; i < MEASUREMENT_COUNT; i++) {
            long start = System.nanoTime();
            messageService.getMessagesForUserBySender("steve", "eric");
            long duration = System.nanoTime() - start;
            totalDuration += duration;
        }
        
        long averageDuration = totalDuration / MEASUREMENT_COUNT;
        double averageDurationMs = averageDuration / 1_000_000.0;
        
        System.out.printf("Average time to filter %d messages by sender: %.2f ms%n", MESSAGE_COUNT, averageDurationMs);
        
        // Assert performance meets expectations
        assertPerformance(
            Duration.ofNanos(averageDuration),
            Duration.ofMillis(30),
            "Filtering " + MESSAGE_COUNT + " messages by sender"
        );
    }
    
    /**
     * Test deleting messages performance.
     */
    @Test
    @DisplayName("Test deleting messages performance")
    void testDeleteMessagesPerformance() {
        final int DELETE_COUNT = 100;
        
        // Setup test data
        List<String> messageIds = setupTestMessages(DELETE_COUNT);
        
        // Warm up with a few deletes
        for (int i = 0; i < WARMUP_COUNT && i < messageIds.size(); i++) {
            messageService.deleteMessage(messageIds.get(i), "steve");
        }
        
        // Measure
        long totalDuration = 0;
        int deleteStart = WARMUP_COUNT;
        int remainingMessages = DELETE_COUNT - WARMUP_COUNT;
        int measurementCount = Math.min(MEASUREMENT_COUNT, remainingMessages);
        
        for (int i = 0; i < measurementCount; i++) {
            long start = System.nanoTime();
            messageService.deleteMessage(messageIds.get(deleteStart + i), "steve");
            long duration = System.nanoTime() - start;
            totalDuration += duration;
        }
        
        if (measurementCount > 0) {
            long averageDuration = totalDuration / measurementCount;
            double averageDurationMs = averageDuration / 1_000_000.0;
            
            System.out.printf("Average time to delete a message: %.2f ms%n", averageDurationMs);
            
            // Assert performance meets expectations
            assertPerformance(
                Duration.ofNanos(averageDuration),
                Duration.ofMillis(5),
                "Deleting a message"
            );
        }
    }
    
    /**
     * Test performance of filtering messages by project.
     */
    @Test
    @DisplayName("Test filtering messages by project performance")
    void testFilterMessagesByProjectPerformance() {
        final int MESSAGE_COUNT = 1000;
        
        // Setup test data with multiple projects
        for (int i = 0; i < MESSAGE_COUNT / 2; i++) {
            String messageId = "perf-msg-tracer-" + UUID.randomUUID().toString().substring(0, 8);
            RinnaMessage message = new RinnaMessage(
                messageId,
                "eric",
                "steve",
                "Tracer project message #" + i,
                "Tracer",
                Instant.now(),
                MessageStatus.UNREAD
            );
            messageService.sendMessage(message);
        }
        
        for (int i = 0; i < MESSAGE_COUNT / 2; i++) {
            String messageId = "perf-msg-quantum-" + UUID.randomUUID().toString().substring(0, 8);
            RinnaMessage message = new RinnaMessage(
                messageId,
                "maria",
                "steve",
                "Quantum project message #" + i,
                "Quantum",
                Instant.now(),
                MessageStatus.UNREAD
            );
            messageService.sendMessage(message);
        }
        
        // Warm up
        for (int i = 0; i < WARMUP_COUNT; i++) {
            messageService.getMessagesForUserByProject("steve", "Tracer");
        }
        
        // Measure
        long totalDuration = 0;
        for (int i = 0; i < MEASUREMENT_COUNT; i++) {
            long start = System.nanoTime();
            messageService.getMessagesForUserByProject("steve", "Tracer");
            long duration = System.nanoTime() - start;
            totalDuration += duration;
        }
        
        long averageDuration = totalDuration / MEASUREMENT_COUNT;
        double averageDurationMs = averageDuration / 1_000_000.0;
        
        System.out.printf("Average time to filter %d messages by project: %.2f ms%n", MESSAGE_COUNT, averageDurationMs);
        
        // Assert performance meets expectations
        assertPerformance(
            Duration.ofNanos(averageDuration),
            Duration.ofMillis(25),
            "Filtering " + MESSAGE_COUNT + " messages by project"
        );
    }

    /**
     * Test performance of message operations with increased load.
     */
    @Test
    @DisplayName("Test message operations under high load")
    void testMessageOperationsUnderLoad() {
        final int MESSAGE_COUNT = 5000;
        
        // Setup a large number of test messages
        setupTestMessages(MESSAGE_COUNT);
        
        // Measure the time it takes to search for a specific message
        long totalSearchDuration = 0;
        for (int i = 0; i < MEASUREMENT_COUNT; i++) {
            // Create a specific message to search for
            String targetMessageId = sendSingleMessage("This is a target message for search test #" + i);
            
            long start = System.nanoTime();
            RinnaMessage result = messageService.getMessage(targetMessageId);
            long duration = System.nanoTime() - start;
            totalSearchDuration += duration;
            
            // Verify we found the right message
            Assertions.assertNotNull(result);
            Assertions.assertEquals(targetMessageId, result.getId());
        }
        
        long averageSearchDuration = totalSearchDuration / MEASUREMENT_COUNT;
        double averageSearchMs = averageSearchDuration / 1_000_000.0;
        
        System.out.printf("Average time to find a specific message among %d messages: %.2f ms%n", 
                MESSAGE_COUNT, averageSearchMs);
        
        // Assert search performance
        assertPerformance(
            Duration.ofNanos(averageSearchDuration),
            Duration.ofMillis(10),
            "Finding a specific message among " + MESSAGE_COUNT + " messages"
        );
    }
    
    @Test
    @DisplayName("Benchmark concurrent message operations")
    void benchmarkConcurrentOperations() throws Exception {
        final int MESSAGE_COUNT = 100;
        final int THREAD_COUNT = 4;
        
        // Setup initial messages
        setupTestMessages(MESSAGE_COUNT);
        
        // Create threads for concurrent operations
        Thread[] threads = new Thread[THREAD_COUNT];
        long[] durations = new long[THREAD_COUNT];
        
        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                try {
                    long start = System.nanoTime();
                    
                    // Each thread performs different operations
                    switch (threadId % 4) {
                        case 0 -> {
                            // Thread 0: Send messages
                            for (int i = 0; i < MESSAGE_COUNT / THREAD_COUNT; i++) {
                                sendSingleMessage("Concurrent message " + i + " from thread " + threadId);
                            }
                        }
                        case 1 -> {
                            // Thread 1: Read messages
                            for (int i = 0; i < MESSAGE_COUNT / THREAD_COUNT; i++) {
                                messageService.getMessagesForUser("steve");
                            }
                        }
                        case 2 -> {
                            // Thread 2: Filter messages
                            for (int i = 0; i < MESSAGE_COUNT / THREAD_COUNT; i++) {
                                messageService.getMessagesForUserBySender("steve", "eric");
                            }
                        }
                        case 3 -> {
                            // Thread 3: Mark messages as read
                            List<RinnaMessage> messages = messageService.getMessagesForUser("steve");
                            for (int i = 0; i < Math.min(MESSAGE_COUNT / THREAD_COUNT, messages.size()); i++) {
                                messageService.markMessageAsRead(messages.get(i).getId());
                            }
                        }
                    }
                    
                    durations[threadId] = System.nanoTime() - start;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        
        // Start all threads
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to finish
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Calculate average duration across all threads
        long totalDuration = 0;
        for (long duration : durations) {
            totalDuration += duration;
        }
        long averageDuration = totalDuration / THREAD_COUNT;
        double averageDurationMs = averageDuration / 1_000_000.0;
        
        System.out.printf("Average time for concurrent operations across %d threads: %.2f ms%n", 
                THREAD_COUNT, averageDurationMs);
        
        // Assert performance meets expectations for concurrent operations
        assertPerformance(
            Duration.ofNanos(averageDuration),
            Duration.ofMillis(200),
            "Concurrent message operations across " + THREAD_COUNT + " threads"
        );
    }
    
    /**
     * Stress test the message service with a high throughput scenario.
     */
    @Test
    @DisplayName("Stress test message throughput")
    void stressTestMessageThroughput() throws Exception {
        final int MESSAGE_COUNT = 200;
        final int THREAD_COUNT = 8;
        final int OPERATIONS_PER_THREAD = 50;
        
        // Create threads for high-throughput operations
        Thread[] threads = new Thread[THREAD_COUNT];
        
        // Track operation counts for verification
        final int[] successfulOperations = new int[THREAD_COUNT];
        
        for (int t = 0; t < THREAD_COUNT; t++) {
            final int threadId = t;
            threads[t] = new Thread(() -> {
                try {
                    for (int i = 0; i < OPERATIONS_PER_THREAD; i++) {
                        // Each thread does a mix of operations
                        int operationType = (i + threadId) % 3;
                        
                        switch (operationType) {
                            case 0 -> {
                                // Send a new message
                                String messageId = sendSingleMessage("Stress test message " + i + 
                                                                    " from thread " + threadId);
                                if (messageId != null) {
                                    successfulOperations[threadId]++;
                                }
                            }
                            case 1 -> {
                                // Get and mark a message as read
                                List<RinnaMessage> messages = messageService.getUnreadMessagesForUser("steve");
                                if (!messages.isEmpty()) {
                                    boolean success = messageService.markMessageAsRead(messages.get(0).getId());
                                    if (success) {
                                        successfulOperations[threadId]++;
                                    }
                                }
                            }
                            case 2 -> {
                                // Get messages by project
                                List<RinnaMessage> messages = messageService.getMessagesForUserByProject(
                                        "steve", threadId % 2 == 0 ? "Tracer" : "Quantum");
                                successfulOperations[threadId]++;
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error in stress test thread {}: {}", threadId, e.getMessage());
                    e.printStackTrace();
                }
            });
        }
        
        // Setup some initial messages
        setupTestMessages(MESSAGE_COUNT);
        
        // Start all threads and measure total execution time
        long startTime = System.nanoTime();
        
        for (Thread thread : threads) {
            thread.start();
        }
        
        // Wait for all threads to finish
        for (Thread thread : threads) {
            thread.join();
        }
        
        long endTime = System.nanoTime();
        long totalDuration = endTime - startTime;
        double totalDurationMs = totalDuration / 1_000_000.0;
        
        // Calculate operations per second
        int totalOperations = 0;
        for (int count : successfulOperations) {
            totalOperations += count;
        }
        
        double operationsPerSecond = (totalOperations * 1000.0) / totalDurationMs;
        
        System.out.printf("Stress test: %d operations completed in %.2f ms (%.2f ops/sec) across %d threads%n", 
                totalOperations, totalDurationMs, operationsPerSecond, THREAD_COUNT);
        
        // Assert minimum throughput requirements
        Assertions.assertTrue(
            operationsPerSecond >= 1000, // At least 1000 operations per second
            String.format("Throughput too low: %.2f ops/sec", operationsPerSecond)
        );
    }
}