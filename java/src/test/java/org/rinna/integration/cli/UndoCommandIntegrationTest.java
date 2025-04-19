/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.integration.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rinna.base.IntegrationTest;
import org.rinna.cli.command.UndoCommand;
import org.rinna.cli.service.MockHistoryService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.domain.HistoryEntryType;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkflowState;

/**
 * Integration tests for UndoCommand with real service implementations.
 */
@DisplayName("UndoCommand Integration Tests")
public class UndoCommandIntegrationTest extends IntegrationTest {

    private MockWorkflowService workflowService;
    private MockHistoryService historyService;
    private MockItemService itemService;
    private ServiceManager serviceManager;
    
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private UUID testItemId;
    private String currentUser;
    
    @BeforeEach
    void setUp() {
        // Redirect stdout and stderr for testing
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
        
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
    }
    
    @AfterEach
    void tearDown() {
        // Restore original stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Test
    @DisplayName("Should undo state change through service chain")
    void shouldUndoStateChangeThroughServiceChain() {
        // Arrange - create a history entry for state change
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("previousState", "READY");
        metadata.put("newState", "IN_PROGRESS");
        
        MockHistoryService.MockHistoryEntry historyEntry = new MockHistoryService.MockHistoryEntry(
            UUID.randomUUID(),
            testItemId,
            HistoryEntryType.STATE_CHANGE,
            currentUser,
            Instant.now(),
            metadata
        );
        
        historyService.addMockEntry(historyEntry);
        
        // Create the command and simulate confirmation
        UndoCommand undoCommand = new UndoCommand();
        undoCommand.setItemId(testItemId.toString());
        undoCommand.setForce(true); // Skip confirmation for testing
        
        // Override ServiceManager to use our test instances
        overrideServiceManager();
        
        // Act
        int result = undoCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        assertTrue(outputStream.toString().contains("Successfully"), 
                   "Should show success message");
    }
    
    @Test
    @DisplayName("Integration test of time-based undo restrictions")
    void testTimeBasedUndoRestrictions() {
        // Arrange - create an old history entry
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("previousState", "READY");
        metadata.put("newState", "IN_PROGRESS");
        
        MockHistoryService.MockHistoryEntry historyEntry = new MockHistoryService.MockHistoryEntry(
            UUID.randomUUID(),
            testItemId,
            HistoryEntryType.STATE_CHANGE,
            currentUser,
            Instant.now().minus(25, ChronoUnit.HOURS), // 25 hours old
            metadata
        );
        
        historyService.addMockEntry(historyEntry);
        
        // Create the command
        UndoCommand undoCommand = new UndoCommand();
        undoCommand.setItemId(testItemId.toString());
        
        // Override ServiceManager to use our test instances
        overrideServiceManager();
        
        // Act
        int result = undoCommand.call();
        
        // Assert
        assertEquals(1, result, "Command should fail");
        assertTrue(errorStream.toString().contains("Cannot undo changes older than 24 hours"), 
                   "Should show time limit error");
    }
    
    @Test
    @DisplayName("Integration test of complete undo workflow with confirmation")
    void testCompleteUndoWorkflowWithConfirmation() {
        // Arrange - create a history entry
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("field", "title");
        metadata.put("previousValue", "Original Title");
        metadata.put("newValue", "Test Item");
        
        MockHistoryService.MockHistoryEntry historyEntry = new MockHistoryService.MockHistoryEntry(
            UUID.randomUUID(),
            testItemId,
            HistoryEntryType.FIELD_CHANGE,
            currentUser,
            Instant.now().minus(1, ChronoUnit.HOURS),
            metadata
        );
        
        historyService.addMockEntry(historyEntry);
        
        // Create the command and simulate confirmation
        UndoCommand undoCommand = new UndoCommand();
        undoCommand.setItemId(testItemId.toString());
        
        // Simulate user confirmation
        System.setIn(new ByteArrayInputStream("y\n".getBytes()));
        
        // Override ServiceManager to use our test instances
        overrideServiceManager();
        
        // Act
        int result = undoCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        assertTrue(outputStream.toString().contains("current title"), 
                   "Should show current title");
        assertTrue(outputStream.toString().contains("previous title"), 
                   "Should show previous title");
        assertTrue(outputStream.toString().contains("Are you sure"), 
                   "Should ask for confirmation");
    }
    
    @Test
    @DisplayName("Integration test of multiple consecutive undo operations")
    void testMultipleConsecutiveUndoOperations() {
        // Arrange - create multiple history entries
        // 1. State change
        Map<String, Object> stateMetadata = new HashMap<>();
        stateMetadata.put("previousState", "READY");
        stateMetadata.put("newState", "IN_PROGRESS");
        
        MockHistoryService.MockHistoryEntry stateEntry = new MockHistoryService.MockHistoryEntry(
            UUID.randomUUID(),
            testItemId,
            HistoryEntryType.STATE_CHANGE,
            currentUser,
            Instant.now().minus(1, ChronoUnit.HOURS),
            stateMetadata
        );
        
        // 2. Priority change
        Map<String, Object> priorityMetadata = new HashMap<>();
        priorityMetadata.put("previousPriority", "LOW");
        priorityMetadata.put("newPriority", "MEDIUM");
        
        MockHistoryService.MockHistoryEntry priorityEntry = new MockHistoryService.MockHistoryEntry(
            UUID.randomUUID(),
            testItemId,
            HistoryEntryType.PRIORITY_CHANGE,
            currentUser,
            Instant.now().minus(2, ChronoUnit.HOURS),
            priorityMetadata
        );
        
        // Add entries in chronological order (newest first)
        historyService.addMockEntry(stateEntry);
        historyService.addMockEntry(priorityEntry);
        
        // Create the command and skip confirmation
        UndoCommand undoCommand = new UndoCommand();
        undoCommand.setItemId(testItemId.toString());
        undoCommand.setForce(true);
        
        // Override ServiceManager to use our test instances
        overrideServiceManager();
        
        // Act - first undo (state change)
        int result1 = undoCommand.call();
        
        // Clear output for next test
        outputStream.reset();
        errorStream.reset();
        
        // Act - second undo (priority change)
        int result2 = undoCommand.call();
        
        // Assert
        assertEquals(0, result1, "First undo should succeed");
        assertEquals(0, result2, "Second undo should succeed");
    }
    
    @Test
    @DisplayName("Integration test of handling restricted workflow states")
    void testHandlingRestrictedWorkflowStates() {
        // Arrange - create a test item in RELEASED state
        UUID releasedItemId = UUID.randomUUID();
        MockItemService.MockWorkItem releasedItem = new MockItemService.MockWorkItem(
            releasedItemId,
            "Released Item",
            "This item is in RELEASED state",
            "TASK",
            Priority.MEDIUM,
            WorkflowState.RELEASED,
            currentUser
        );
        
        itemService.addMockItem(releasedItem);
        workflowService.addMockItem(releasedItem);
        
        // Create a history entry
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("previousState", "TESTING");
        metadata.put("newState", "RELEASED");
        
        MockHistoryService.MockHistoryEntry historyEntry = new MockHistoryService.MockHistoryEntry(
            UUID.randomUUID(),
            releasedItemId,
            HistoryEntryType.STATE_CHANGE,
            currentUser,
            Instant.now(),
            metadata
        );
        
        historyService.addMockEntry(historyEntry);
        
        // Create the command
        UndoCommand undoCommand = new UndoCommand();
        undoCommand.setItemId(releasedItemId.toString());
        
        // Override ServiceManager to use our test instances
        overrideServiceManager();
        
        // Act
        int result = undoCommand.call();
        
        // Assert
        assertEquals(1, result, "Command should fail");
        assertTrue(errorStream.toString().contains("Cannot undo changes to items in RELEASED state"), 
                   "Should show restricted state error");
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
}