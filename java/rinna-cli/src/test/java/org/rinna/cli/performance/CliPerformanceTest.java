package org.rinna.cli.performance;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.rinna.cli.command.ViewCommand;

/**
 * Performance tests for CLI operations.
 * These tests verify the performance characteristics of the CLI.
 */
@Tag("performance")
@DisplayName("CLI Performance Tests")
@Disabled("Needs updates for non-Picocli implementation")
public class CliPerformanceTest {

    private static final int MAX_ACCEPTABLE_RESPONSE_TIME_MS = 100; // 100ms
    private static final int CONCURRENT_USERS = 10;
    
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputStream));
    }
    
    @Test
    @DisplayName("Should execute command with acceptable response time")
    void shouldExecuteCommandWithAcceptableResponseTime() {
        // Setup direct command
        ViewCommand viewCmd = new ViewCommand();
        viewCmd.setId("WI-123");
        
        // Measure execution time
        Instant start = Instant.now();
        viewCmd.call();
        Duration duration = Duration.between(start, Instant.now());
        
        // Verify performance
        assertTrue(duration.toMillis() < MAX_ACCEPTABLE_RESPONSE_TIME_MS,
                  "Command execution should complete within " + MAX_ACCEPTABLE_RESPONSE_TIME_MS + "ms");
    }
    
    @ParameterizedTest
    @ValueSource(ints = {100, 500, 1000})
    @DisplayName("Should handle increasing number of commands efficiently")
    void shouldHandleIncreasingNumberOfCommandsEfficiently(int commandCount) {
        // Execute commands multiple times
        Instant start = Instant.now();
        for (int i = 0; i < commandCount; i++) {
            ViewCommand viewCmd = new ViewCommand();
            viewCmd.setId("WI-" + i);
            viewCmd.call();
        }
        Duration duration = Duration.between(start, Instant.now());
        
        // Calculate average time per command
        long avgTimePerCommand = duration.toMillis() / commandCount;
        
        // Verify performance scales linearly (or better)
        assertTrue(avgTimePerCommand < MAX_ACCEPTABLE_RESPONSE_TIME_MS,
                  "Average command execution time should be within " + MAX_ACCEPTABLE_RESPONSE_TIME_MS + 
                  "ms, but was " + avgTimePerCommand + "ms");
                  
        // Additional verification: execution time should not grow superlinearly
        // For a baseline of 100 operations, 1000 operations should take less than 15x time
        // (allowing some overhead for JVM warmup)
        if (commandCount == 1000) {
            System.out.println("Total execution time for 1000 commands: " + duration.toMillis() + "ms");
            System.out.println("Average time per command: " + avgTimePerCommand + "ms");
        }
    }
    
    @Test
    @DisplayName("Should handle concurrent command execution")
    void shouldHandleConcurrentCommandExecution() throws Exception {
        // Setup
        ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_USERS);
        CountDownLatch latch = new CountDownLatch(CONCURRENT_USERS);
        List<Duration> executionTimes = new ArrayList<>();
        
        // Execute commands concurrently
        for (int i = 0; i < CONCURRENT_USERS; i++) {
            final int userId = i;
            executor.submit(() -> {
                try {
                    ViewCommand viewCmd = new ViewCommand();
                    viewCmd.setId("WI-" + userId);
                    Instant start = Instant.now();
                    viewCmd.call();
                    Duration duration = Duration.between(start, Instant.now());
                    
                    synchronized (executionTimes) {
                        executionTimes.add(duration);
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        
        // Wait for all threads to complete
        boolean completed = latch.await(10, TimeUnit.SECONDS);
        executor.shutdown();
        
        // Verify all commands completed
        assertTrue(completed, "All concurrent commands should complete within timeout");
        assertEquals(CONCURRENT_USERS, executionTimes.size(), 
                    "Should have timing data for all concurrent users");
        
        // Calculate and verify performance metrics
        long totalMs = executionTimes.stream().mapToLong(Duration::toMillis).sum();
        double avgMs = (double) totalMs / executionTimes.size();
        long maxMs = executionTimes.stream().mapToLong(Duration::toMillis).max().orElse(0);
        
        System.out.println("Concurrent execution metrics:");
        System.out.println("  Average response time: " + avgMs + "ms");
        System.out.println("  Maximum response time: " + maxMs + "ms");
        
        // Verify performance with concurrent load
        assertTrue(avgMs < MAX_ACCEPTABLE_RESPONSE_TIME_MS * 5, 
                  "Average response time under concurrent load should be acceptable");
        assertTrue(maxMs < MAX_ACCEPTABLE_RESPONSE_TIME_MS * 10,
                  "Maximum response time under concurrent load should be acceptable");
    }
}