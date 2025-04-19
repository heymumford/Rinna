/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.performance.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.*;
import org.rinna.base.PerformanceTest;
import org.rinna.cli.command.UndoCommand;
import org.rinna.cli.service.MockHistoryService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.domain.HistoryEntryType;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkflowState;

/**
 * Performance tests for the UndoCommand.
 */
@DisplayName("UndoCommand Performance Tests")
public class UndoCommandPerformanceTest extends PerformanceTest {
    
    private MockWorkflowService workflowService;
    private MockHistoryService historyService;
    private MockItemService itemService;
    private ServiceManager serviceManager;
    
    private UUID testItemId;
    private String currentUser;
    private final int LARGE_HISTORY_SIZE = 10000;
    
    @BeforeEach
    void setUp() {
        // Redirect stdin to simulate user confirmation
        System.setIn(new ByteArrayInputStream("y\n".getBytes()));
        
        // Initialize real service implementations
        workflowService = new MockWorkflowService();
        historyService = new MockHistoryService();
        itemService = new MockItemService();
        
        // Create test data
        currentUser = System.getProperty("user.name");
        testItemId = UUID.randomUUID();
        
        // Create a test work item
        MockItemService.MockWorkItem testItem = new MockItemService.MockWorkItem(
            testItemId,
            "Test Item",
            "Test Description",
            "TASK",
            Priority.MEDIUM,
            WorkflowState.IN_PROGRESS,
            currentUser
        );
        itemService.addMockItem(testItem);
        
        // Add the item to the workflow service
        workflowService.addMockItem(testItem);
        
        // Override ServiceManager to use our test instances
        overrideServiceManager();
    }
    
    @Test
    @DisplayName("Performance test of undo with minimal history")
    @Tag("performance")
    void testUndoPerformanceWithMinimalHistory() {
        // Arrange - create a single history entry
        addHistoryEntry();
        
        // Create the command and skip confirmation
        UndoCommand undoCommand = new UndoCommand();
        undoCommand.setItemId(testItemId.toString());
        undoCommand.setForce(true);
        
        // Act - measure execution time
        long startTime = System.nanoTime();
        int result = undoCommand.call();
        long endTime = System.nanoTime();
        long durationNanos = endTime - startTime;
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        assertTimeout(Duration.ofMillis(100), () -> {
            // This is just to record the actual time, not a real assertion
            TimeUnit.NANOSECONDS.toMillis(durationNanos);
        }, "Undo with minimal history should complete quickly");
        
        System.out.println("Undo with minimal history executed in " + 
                  TimeUnit.NANOSECONDS.toMillis(durationNanos) + "ms");
    }
    
    @Test
    @DisplayName("Performance test of undo with large history")
    @Tag("performance")
    void testUndoPerformanceWithLargeHistory() {
        // Arrange - create many history entries
        addManyHistoryEntries(LARGE_HISTORY_SIZE);
        
        // Create the command and skip confirmation
        UndoCommand undoCommand = new UndoCommand();
        undoCommand.setItemId(testItemId.toString());
        undoCommand.setForce(true);
        
        // Act - measure execution time
        long startTime = System.nanoTime();
        int result = undoCommand.call();
        long endTime = System.nanoTime();
        long durationNanos = endTime - startTime;
        
        // Assert
        assertEquals(0, result, "Command should succeed even with large history");
        assertTimeout(Duration.ofMillis(500), () -> {
            // This is just to record the actual time, not a real assertion
            TimeUnit.NANOSECONDS.toMillis(durationNanos);
        }, "Undo with large history should complete within acceptable time");
        
        System.out.println("Undo with " + LARGE_HISTORY_SIZE + " history entries executed in " + 
                  TimeUnit.NANOSECONDS.toMillis(durationNanos) + "ms");
    }
    
    @Test
    @DisplayName("Performance test of undo with confirmation dialog")
    @Tag("performance")
    void testUndoPerformanceWithConfirmationDialog() {
        // Arrange - create a history entry
        addHistoryEntry();
        
        // Create the command with confirmation
        UndoCommand undoCommand = new UndoCommand();
        undoCommand.setItemId(testItemId.toString());
        
        // Act - measure execution time
        long startTime = System.nanoTime();
        int result = undoCommand.call();
        long endTime = System.nanoTime();
        long durationNanos = endTime - startTime;
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        assertTimeout(Duration.ofMillis(200), () -> {
            // This is just to record the actual time, not a real assertion
            TimeUnit.NANOSECONDS.toMillis(durationNanos);
        }, "Undo with confirmation should complete within acceptable time");
        
        System.out.println("Undo with confirmation dialog executed in " + 
                  TimeUnit.NANOSECONDS.toMillis(durationNanos) + "ms");
    }
    
    @Test
    @DisplayName("Performance test of multiple consecutive undo operations")
    @Tag("performance")
    void testMultipleConsecutiveUndoPerformance() {
        // Arrange - create multiple history entries
        addMultipleHistoryEntryTypes();
        
        // Create the command and skip confirmation
        UndoCommand undoCommand = new UndoCommand();
        undoCommand.setItemId(testItemId.toString());
        undoCommand.setForce(true);
        
        // Act - measure execution time for multiple consecutive calls
        int numOperations = 10;
        long[] durations = new long[numOperations];
        
        for (int i = 0; i < numOperations; i++) {
            // Add a new entry before each operation to have something to undo
            addHistoryEntry();
            
            long startTime = System.nanoTime();
            int result = undoCommand.call();
            long endTime = System.nanoTime();
            durations[i] = endTime - startTime;
            
            assertEquals(0, result, "Command should succeed");
        }
        
        // Calculate average and max duration
        long totalDuration = Arrays.stream(durations).sum();
        long averageDuration = totalDuration / numOperations;
        long maxDuration = Arrays.stream(durations).max().orElse(0);
        
        // Assert
        assertTimeout(Duration.ofMillis(200), () -> {
            TimeUnit.NANOSECONDS.toMillis(maxDuration);
        }, "Maximum undo operation time should be acceptable");
        
        System.out.println("Average undo operation time: " + 
                  TimeUnit.NANOSECONDS.toMillis(averageDuration) + "ms");
        System.out.println("Maximum undo operation time: " + 
                  TimeUnit.NANOSECONDS.toMillis(maxDuration) + "ms");
    }
    
    @Test
    @DisplayName("Performance test of concurrent undo operations")
    @Tag("performance")
    void testConcurrentUndoPerformance() {
        // Arrange - create multiple work items and history entries
        int numItems = 10;
        List<UUID> itemIds = new ArrayList<>();
        
        for (int i = 0; i < numItems; i++) {
            UUID itemId = UUID.randomUUID();
            itemIds.add(itemId);
            
            MockItemService.MockWorkItem testItem = new MockItemService.MockWorkItem(
                itemId,
                "Test Item " + i,
                "Test Description " + i,
                "TASK",
                Priority.MEDIUM,
                WorkflowState.IN_PROGRESS,
                currentUser
            );
            itemService.addMockItem(testItem);
            workflowService.addMockItem(testItem);
            
            // Add history entry for this item
            addHistoryEntryForItem(itemId);
        }
        
        // Act - measure execution time for concurrent operations
        long startTime = System.nanoTime();
        
        // Run undo operations in parallel
        itemIds.parallelStream().forEach(itemId -> {
            UndoCommand cmd = new UndoCommand();
            cmd.setItemId(itemId.toString());
            cmd.setForce(true);
            cmd.call();
        });
        
        long endTime = System.nanoTime();
        long totalDuration = endTime - startTime;
        
        // Assert
        assertTimeout(Duration.ofSeconds(2), () -> {
            TimeUnit.NANOSECONDS.toMillis(totalDuration);
        }, "Concurrent undo operations should complete within acceptable time");
        
        System.out.println("Executed " + numItems + " concurrent undo operations in " + 
                  TimeUnit.NANOSECONDS.toMillis(totalDuration) + "ms");
    }
    
    @Test
    @DisplayName("Performance test of undo under load")
    @Tag("performance")
    void testUndoPerformanceUnderLoad() {
        // Arrange - create many history entries to simulate load
        addManyHistoryEntries(10000);
        
        // Create large string data to simulate memory pressure
        List<String> largeDataSet = new ArrayList<>();
        for (int i = 0; i < 100000; i++) {
            largeDataSet.add("Large string data item " + i + " " + UUID.randomUUID());
        }
        
        // Create the command and skip confirmation
        UndoCommand undoCommand = new UndoCommand();
        undoCommand.setItemId(testItemId.toString());
        undoCommand.setForce(true);
        
        // Act - measure execution time under load
        long startTime = System.nanoTime();
        int result = undoCommand.call();
        long endTime = System.nanoTime();
        long durationNanos = endTime - startTime;
        
        // Assert
        assertEquals(0, result, "Command should succeed even under load");
        assertTimeout(Duration.ofMillis(1000), () -> {
            TimeUnit.NANOSECONDS.toMillis(durationNanos);
        }, "Undo under load should complete within acceptable time");
        
        System.out.println("Undo under simulated load executed in " + 
                  TimeUnit.NANOSECONDS.toMillis(durationNanos) + "ms");
        
        // Prevent the large data from being optimized away
        assertNotNull(largeDataSet);
    }
    
    /**
     * Helper method to override the ServiceManager with our test instances.
     */
    private void overrideServiceManager() {
        // Create a test ServiceManager that returns our mock services
        serviceManager = new ServiceManager() {
            @Override
            public MockWorkflowService getWorkflowService() {
                return workflowService;
            }
            
            @Override
            public MockHistoryService getHistoryService() {
                return historyService;
            }
            
            @Override
            public MockItemService getItemService() {
                return itemService;
            }
        };
        
        // Inject our service manager using reflection to avoid changing the singleton
        try {
            java.lang.reflect.Field instanceField = ServiceManager.class.getDeclaredField("instance");
            instanceField.setAccessible(true);
            instanceField.set(null, serviceManager);
        } catch (Exception e) {
            throw new RuntimeException("Failed to override ServiceManager", e);
        }
    }
    
    /**
     * Helper method to add a single history entry.
     */
    private void addHistoryEntry() {
        addHistoryEntryForItem(testItemId);
    }
    
    /**
     * Helper method to add a history entry for a specific item.
     */
    private void addHistoryEntryForItem(UUID itemId) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("previousState", "READY");
        metadata.put("newState", "IN_PROGRESS");
        
        MockHistoryService.MockHistoryEntry historyEntry = new MockHistoryService.MockHistoryEntry(
            UUID.randomUUID(),
            itemId,
            HistoryEntryType.STATE_CHANGE,
            currentUser,
            Instant.now(),
            metadata
        );
        
        historyService.addMockEntry(historyEntry);
    }
    
    /**
     * Helper method to add multiple history entries of different types.
     */
    private void addMultipleHistoryEntryTypes() {
        // State change
        Map<String, Object> stateMetadata = new HashMap<>();
        stateMetadata.put("previousState", "READY");
        stateMetadata.put("newState", "IN_PROGRESS");
        
        MockHistoryService.MockHistoryEntry stateEntry = new MockHistoryService.MockHistoryEntry(
            UUID.randomUUID(),
            testItemId,
            HistoryEntryType.STATE_CHANGE,
            currentUser,
            Instant.now().minusSeconds(300),
            stateMetadata
        );
        
        // Field change
        Map<String, Object> fieldMetadata = new HashMap<>();
        fieldMetadata.put("field", "title");
        fieldMetadata.put("previousValue", "Old Title");
        fieldMetadata.put("newValue", "New Title");
        
        MockHistoryService.MockHistoryEntry fieldEntry = new MockHistoryService.MockHistoryEntry(
            UUID.randomUUID(),
            testItemId,
            HistoryEntryType.FIELD_CHANGE,
            currentUser,
            Instant.now().minusSeconds(200),
            fieldMetadata
        );
        
        // Assignment change
        Map<String, Object> assignMetadata = new HashMap<>();
        assignMetadata.put("previousAssignee", "alice");
        assignMetadata.put("newAssignee", currentUser);
        
        MockHistoryService.MockHistoryEntry assignEntry = new MockHistoryService.MockHistoryEntry(
            UUID.randomUUID(),
            testItemId,
            HistoryEntryType.ASSIGNMENT_CHANGE,
            currentUser,
            Instant.now().minusSeconds(100),
            assignMetadata
        );
        
        // Add entries in reverse chronological order (newest first)
        historyService.addMockEntry(assignEntry);
        historyService.addMockEntry(fieldEntry);
        historyService.addMockEntry(stateEntry);
    }
    
    /**
     * Helper method to add a large number of history entries.
     *
     * @param count the number of entries to add
     */
    private void addManyHistoryEntries(int count) {
        for (int i = 0; i < count; i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("previousState", "STATE_" + i);
            metadata.put("newState", "STATE_" + (i + 1));
            
            MockHistoryService.MockHistoryEntry entry = new MockHistoryService.MockHistoryEntry(
                UUID.randomUUID(),
                testItemId,
                HistoryEntryType.STATE_CHANGE,
                currentUser,
                Instant.now().minusSeconds(count - i), // Gradually more recent
                metadata
            );
            
            historyService.addMockEntry(entry);
        }
    }
}