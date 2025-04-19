/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.unit.cli;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.base.UnitTest;
import org.rinna.cli.command.UndoCommand;
import org.rinna.cli.service.ServiceManager;
import org.rinna.domain.HistoryEntry;
import org.rinna.domain.HistoryEntryType;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkflowState;
import org.rinna.usecase.HistoryService;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.WorkflowService;

/**
 * Unit tests for the UndoCommand class.
 */
@DisplayName("UndoCommand Unit Tests")
public class UndoCommandTest extends UnitTest {
    
    // Constants for multi-step undo testing
    private static final int MAX_UNDO_STEPS = 3;

    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private WorkflowService workflowService;
    
    @Mock
    private HistoryService historyService;
    
    @Mock
    private ItemService itemService;
    
    @Mock
    private WorkItem mockWorkItem;
    
    @Mock
    private HistoryEntry mockHistoryEntry;
    
    private UndoCommand undoCommand;
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final String currentUser = System.getProperty("user.name");
    private final UUID mockItemId = UUID.randomUUID();
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up System.out/err capturing
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Set up default command
        undoCommand = new UndoCommand();
        setMockServiceManager();
        
        // Reset captors between tests
        outputCaptor.reset();
        errorCaptor.reset();
    }
    
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    /**
     * Sets up a mock ServiceManager to be used by the UndoCommand.
     */
    private void setMockServiceManager() {
        // Mock ServiceManager.getInstance() to return our mock
        try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(serviceManager);
            
            // Set up mock responses
            when(serviceManager.getWorkflowService()).thenReturn(workflowService);
            when(serviceManager.getHistoryService()).thenReturn(historyService);
            when(serviceManager.getItemService()).thenReturn(itemService);
        }
    }
    
    /**
     * Sets up mock data for a successful undo scenario.
     *
     * @param historyEntryType the type of history entry to mock
     */
    private void setupMockDataForSuccessfulUndo(HistoryEntryType historyEntryType) {
        setupMockDataForSuccessfulUndo(historyEntryType, 0); // Default to most recent change
    }
    
    private void setupMockDataForSuccessfulUndo(HistoryEntryType historyEntryType, int stepIndex) {
        // Mock work item in progress
        List<WorkItem> inProgressItems = Collections.singletonList(mockWorkItem);
        when(workflowService.getItemsInState(WorkflowState.IN_PROGRESS, currentUser))
            .thenReturn(inProgressItems);
        
        // Common work item mocks
        when(mockWorkItem.id()).thenReturn(mockItemId);
        when(mockWorkItem.state()).thenReturn(WorkflowState.IN_PROGRESS);
        when(mockWorkItem.assignee()).thenReturn(currentUser);
        
        // Create multiple history entries if we're testing multi-step undo
        List<HistoryEntry> historyEntries;
        if (stepIndex > 0) {
            historyEntries = createMultipleHistoryEntries(MAX_UNDO_STEPS + 1); // Create more than max allowed
            when(historyService.getHistory(mockItemId)).thenReturn(historyEntries);
            
            // Set up the specific history entry being undone
            mockHistoryEntry = historyEntries.get(stepIndex);
        } else {
            // Mock a single history entry for basic undo
            historyEntries = Collections.singletonList(mockHistoryEntry);
            when(historyService.getHistory(mockItemId)).thenReturn(historyEntries);
            
            // Common history entry mocks
            when(mockHistoryEntry.id()).thenReturn(UUID.randomUUID());
            when(mockHistoryEntry.workItemId()).thenReturn(mockItemId);
            when(mockHistoryEntry.type()).thenReturn(historyEntryType);
            when(mockHistoryEntry.user()).thenReturn(currentUser);
            when(mockHistoryEntry.timestamp()).thenReturn(Instant.now());
            
            // Type-specific metadata
            Map<String, Object> metadata = new HashMap<>();
            switch (historyEntryType) {
                case STATE_CHANGE:
                    metadata.put("previousState", "READY");
                    metadata.put("newState", "IN_PROGRESS");
                    break;
                case FIELD_CHANGE:
                    metadata.put("field", "title");
                    metadata.put("previousValue", "Old Title");
                    metadata.put("newValue", "New Title");
                    break;
                case ASSIGNMENT_CHANGE:
                    metadata.put("previousAssignee", "alice");
                    metadata.put("newAssignee", currentUser);
                    break;
                case PRIORITY_CHANGE:
                    metadata.put("previousPriority", "LOW");
                    metadata.put("newPriority", "HIGH");
                    break;
            }
            when(mockHistoryEntry.metadata()).thenReturn(metadata);
        }
        
        // Simulate confirmation
        System.setIn(new ByteArrayInputStream("y\n".getBytes()));
    }
    
    private List<HistoryEntry> createMultipleHistoryEntries(int count) {
        List<HistoryEntry> entries = new ArrayList<>();
        HistoryEntryType[] types = {
            HistoryEntryType.STATE_CHANGE,
            HistoryEntryType.FIELD_CHANGE,
            HistoryEntryType.PRIORITY_CHANGE,
            HistoryEntryType.ASSIGNMENT_CHANGE
        };
        
        for (int i = 0; i < count; i++) {
            HistoryEntry entry = mock(HistoryEntry.class);
            HistoryEntryType type = types[i % types.length];
            
            when(entry.id()).thenReturn(UUID.randomUUID());
            when(entry.workItemId()).thenReturn(mockItemId);
            when(entry.type()).thenReturn(type);
            when(entry.user()).thenReturn(currentUser);
            // Newer entries have more recent timestamps
            when(entry.timestamp()).thenReturn(Instant.now().minusSeconds(i * 60));
            
            // Type-specific metadata
            Map<String, Object> metadata = new HashMap<>();
            switch (type) {
                case STATE_CHANGE:
                    metadata.put("previousState", "STATE_" + i);
                    metadata.put("newState", "STATE_" + (i + 1));
                    break;
                case FIELD_CHANGE:
                    metadata.put("field", "title");
                    metadata.put("previousValue", "Title " + i);
                    metadata.put("newValue", "Title " + (i + 1));
                    break;
                case ASSIGNMENT_CHANGE:
                    metadata.put("previousAssignee", "user" + i);
                    metadata.put("newAssignee", "user" + (i + 1));
                    break;
                case PRIORITY_CHANGE:
                    metadata.put("previousPriority", "PRIORITY_" + i);
                    metadata.put("newPriority", "PRIORITY_" + (i + 1));
                    break;
            }
            when(entry.metadata()).thenReturn(metadata);
            
            entries.add(entry);
        }
        
        return entries;
    }
    
    @Test
    @DisplayName("Should undo state change successfully")
    void shouldUndoStateChangeSuccessfully() {
        // Setup
        setupMockDataForSuccessfulUndo(HistoryEntryType.STATE_CHANGE);
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        verify(workflowService).transition(eq(mockItemId), eq(currentUser), eq(WorkflowState.READY), anyString());
        assertTrue(outputCaptor.toString().contains("Successfully reverted state to READY"), 
                "Should show success message");
    }
    
    @Test
    @DisplayName("Should undo field change successfully")
    void shouldUndoFieldChangeSuccessfully() {
        // Setup
        setupMockDataForSuccessfulUndo(HistoryEntryType.FIELD_CHANGE);
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        verify(itemService).updateTitle(eq(mockItemId), eq("Old Title"), eq(currentUser));
        assertTrue(outputCaptor.toString().contains("Successfully reverted title"), 
                "Should show success message");
    }
    
    @Test
    @DisplayName("Should undo assignment change successfully")
    void shouldUndoAssignmentChangeSuccessfully() {
        // Setup
        setupMockDataForSuccessfulUndo(HistoryEntryType.ASSIGNMENT_CHANGE);
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        verify(itemService).assignTo(eq(mockItemId), eq("alice"), eq(currentUser));
        assertTrue(outputCaptor.toString().contains("Successfully reassigned"), 
                "Should show success message");
    }
    
    @Test
    @DisplayName("Should undo priority change successfully")
    void shouldUndoPriorityChangeSuccessfully() {
        // Setup
        setupMockDataForSuccessfulUndo(HistoryEntryType.PRIORITY_CHANGE);
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        verify(itemService).updatePriority(eq(mockItemId), eq(Priority.LOW), eq(currentUser));
        assertTrue(outputCaptor.toString().contains("Successfully reverted priority"), 
                "Should show success message");
    }
    
    @Test
    @DisplayName("Should skip confirmation when force flag is set")
    void shouldSkipConfirmationWithForceFlag() {
        // Setup
        setupMockDataForSuccessfulUndo(HistoryEntryType.STATE_CHANGE);
        undoCommand.setForce(true);
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        verify(workflowService).transition(any(), any(), any(), any());
        assertFalse(outputCaptor.toString().contains("Are you sure"), 
                "Should not show confirmation prompt");
    }
    
    @Test
    @DisplayName("Should cancel undo when user enters 'n'")
    void shouldCancelUndoWhenUserDenies() {
        // Setup
        setupMockDataForSuccessfulUndo(HistoryEntryType.STATE_CHANGE);
        System.setIn(new ByteArrayInputStream("n\n".getBytes()));
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed but not perform undo");
        verify(workflowService, never()).transition(any(), any(), any(), any());
        assertTrue(outputCaptor.toString().contains("Undo operation canceled"), 
                "Should show cancellation message");
    }
    
    @Test
    @DisplayName("Should fail when no work item is in progress")
    void shouldFailWithNoWorkItemInProgress() {
        // Setup
        when(workflowService.getItemsInState(WorkflowState.IN_PROGRESS, currentUser))
            .thenReturn(Collections.emptyList());
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("No work item is currently in progress"), 
                "Should show appropriate error message");
    }
    
    @Test
    @DisplayName("Should fail when no history entries exist")
    void shouldFailWithNoHistoryEntries() {
        // Setup
        List<WorkItem> inProgressItems = Collections.singletonList(mockWorkItem);
        when(workflowService.getItemsInState(WorkflowState.IN_PROGRESS, currentUser))
            .thenReturn(inProgressItems);
        when(mockWorkItem.id()).thenReturn(mockItemId);
        when(historyService.getHistory(mockItemId)).thenReturn(Collections.emptyList());
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("No recent changes found to undo"), 
                "Should show appropriate error message");
    }
    
    @Test
    @DisplayName("Should fail when work item is in restricted state")
    void shouldFailWithRestrictedState() {
        // Setup
        List<WorkItem> inProgressItems = Collections.singletonList(mockWorkItem);
        when(workflowService.getItemsInState(WorkflowState.IN_PROGRESS, currentUser))
            .thenReturn(inProgressItems);
        when(mockWorkItem.id()).thenReturn(mockItemId);
        when(mockWorkItem.state()).thenReturn(WorkflowState.RELEASED);
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Cannot undo changes to items in RELEASED state"), 
                "Should show appropriate error message");
    }
    
    @Test
    @DisplayName("Should fail when entry is too old")
    void shouldFailWithOldEntry() {
        // Setup
        List<WorkItem> inProgressItems = Collections.singletonList(mockWorkItem);
        when(workflowService.getItemsInState(WorkflowState.IN_PROGRESS, currentUser))
            .thenReturn(inProgressItems);
        when(mockWorkItem.id()).thenReturn(mockItemId);
        when(mockWorkItem.state()).thenReturn(WorkflowState.IN_PROGRESS);
        
        // Mock an old history entry
        HistoryEntry oldEntry = mock(HistoryEntry.class);
        when(oldEntry.workItemId()).thenReturn(mockItemId);
        when(oldEntry.timestamp()).thenReturn(Instant.now().minus(25, ChronoUnit.HOURS));
        List<HistoryEntry> historyEntries = Collections.singletonList(oldEntry);
        when(historyService.getHistory(mockItemId)).thenReturn(historyEntries);
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Cannot undo changes older than 24 hours"), 
                "Should show appropriate error message");
    }
    
    @Test
    @DisplayName("Should use specified item ID when provided")
    void shouldUseSpecifiedItemId() {
        // Setup
        setupMockDataForSuccessfulUndo(HistoryEntryType.STATE_CHANGE);
        String specificItemId = UUID.randomUUID().toString();
        undoCommand.setItemId(specificItemId);
        
        // Mock for specific item ID
        when(workflowService.getItem(UUID.fromString(specificItemId))).thenReturn(mockWorkItem);
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        verify(workflowService).getItem(UUID.fromString(specificItemId));
        verify(historyService).getHistory(mockItemId);
    }
    
    @Test
    @DisplayName("Should fail when item is not found")
    void shouldFailWhenItemNotFound() {
        // Setup
        String specificItemId = UUID.randomUUID().toString();
        undoCommand.setItemId(specificItemId);
        when(workflowService.getItem(UUID.fromString(specificItemId))).thenReturn(null);
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Work item not found"), 
                "Should show appropriate error message");
    }
    
    @Test
    @DisplayName("Should fail when user does not have permission")
    void shouldFailWhenUserLacksPermission() {
        // Setup
        String specificItemId = UUID.randomUUID().toString();
        undoCommand.setItemId(specificItemId);
        when(workflowService.getItem(UUID.fromString(specificItemId))).thenReturn(mockWorkItem);
        when(mockWorkItem.assignee()).thenReturn("different-user");
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("You do not have permission"), 
                "Should show appropriate error message");
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"invalid-uuid", "123", "", "null", "undefined", "true", "[]"})
    @DisplayName("Should fail with invalid UUID")
    void shouldFailWithInvalidUuid(String invalidId) {
        // Setup
        undoCommand.setItemId(invalidId);
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Invalid work item ID"), 
                "Should show appropriate error message");
    }
    
    @Test
    @DisplayName("Should handle service exception gracefully")
    void shouldHandleServiceExceptionGracefully() {
        // Setup
        setupMockDataForSuccessfulUndo(HistoryEntryType.STATE_CHANGE);
        doThrow(new RuntimeException("Service error")).when(workflowService)
            .transition(any(), any(), any(), any());
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Error reverting state"), 
                "Should show appropriate error message");
    }
    
    @ParameterizedTest
    @CsvSource({
        "title, field, Previous Title, Current Title",
        "description, field, Previous Desc, Current Desc",
        "custom_field, field, Previous Value, Current Value"
    })
    @DisplayName("Should handle different field types")
    void shouldHandleDifferentFieldTypes(String fieldName, String fieldType, 
                                         String prevValue, String currValue) {
        // Setup
        setupMockDataForSuccessfulUndo(HistoryEntryType.FIELD_CHANGE);
        
        // Override metadata for this test
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("field", fieldName);
        metadata.put("previousValue", prevValue);
        metadata.put("newValue", currValue);
        when(mockHistoryEntry.metadata()).thenReturn(metadata);
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        if ("title".equals(fieldName)) {
            verify(itemService).updateTitle(any(), eq(prevValue), any());
        } else if ("description".equals(fieldName)) {
            verify(itemService).updateDescription(any(), eq(prevValue), any());
        } else {
            verify(itemService).updateField(any(), eq(fieldName), eq(prevValue), any());
        }
    }
    
    @Test
    @DisplayName("Should handle null or empty confirmation input")
    void shouldHandleNullOrEmptyConfirmationInput() {
        // Setup
        setupMockDataForSuccessfulUndo(HistoryEntryType.STATE_CHANGE);
        System.setIn(new ByteArrayInputStream("\n".getBytes()));
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed but not perform undo");
        verify(workflowService, never()).transition(any(), any(), any(), any());
        assertTrue(outputCaptor.toString().contains("Undo operation canceled"), 
                "Should show cancellation message");
    }
    
    @Test
    @DisplayName("Should handle malicious metadata in history entry")
    void shouldHandleMaliciousMetadata() {
        // Setup
        setupMockDataForSuccessfulUndo(HistoryEntryType.FIELD_CHANGE);
        
        // Override metadata with potentially harmful values
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("field", "title");
        metadata.put("previousValue", "'; DROP TABLE USERS; --");
        metadata.put("newValue", "Hacked Title");
        when(mockHistoryEntry.metadata()).thenReturn(metadata);
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        // The key point is we don't execute the SQL - we just pass the string to our service
        verify(itemService).updateTitle(any(), eq("'; DROP TABLE USERS; --"), any());
    }
    
    @Test
    @DisplayName("Should handle unsupported history entry type")
    void shouldHandleUnsupportedHistoryEntryType() {
        // Setup
        setupMockDataForSuccessfulUndo(HistoryEntryType.COMMENT);
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Unsupported change type"), 
                "Should show appropriate error message");
    }
    
    // Multi-step undo tests
    
    @Test
    @DisplayName("Should undo a specific step when step parameter is provided")
    void shouldUndoSpecificStep() {
        // Setup
        setupMockDataForSuccessfulUndo(HistoryEntryType.STATE_CHANGE, 2);  // Undo the 3rd most recent change (index 2)
        undoCommand.setStep(2);  // 0-based index
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        verify(workflowService).transition(eq(mockItemId), eq(currentUser), any(WorkflowState.class), anyString());
        assertTrue(outputCaptor.toString().contains("Successfully reverted"), 
                "Should show success message");
    }
    
    @Test
    @DisplayName("Should fail when step parameter exceeds maximum allowed")
    void shouldFailWhenStepExceedsMaximum() {
        // Setup
        setupMockDataForSuccessfulUndo(HistoryEntryType.STATE_CHANGE);
        undoCommand.setStep(MAX_UNDO_STEPS);  // This exceeds the max (which is 0-indexed)
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Cannot undo more than 3 steps back"), 
                "Should show appropriate error message");
    }
    
    @Test
    @DisplayName("Should list available changes when steps parameter is provided")
    void shouldListAvailableChangesWithStepsParameter() {
        // Setup
        List<HistoryEntry> historyEntries = createMultipleHistoryEntries(MAX_UNDO_STEPS);
        when(historyService.getHistory(any())).thenReturn(historyEntries);
        when(workflowService.getItemsInState(any(), any())).thenReturn(Collections.singletonList(mockWorkItem));
        when(mockWorkItem.id()).thenReturn(mockItemId);
        when(mockWorkItem.assignee()).thenReturn(currentUser);
        undoCommand.setSteps(true);
        
        // Simulate user selecting option 1
        System.setIn(new ByteArrayInputStream("1\ny\n".getBytes()));
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        assertTrue(outputCaptor.toString().contains("Available changes to undo"), 
                "Should list available changes");
        assertTrue(outputCaptor.toString().contains("Select a change to undo"), 
                "Should prompt user to select a change");
    }
    
    @Test
    @DisplayName("Should fail when user provides invalid selection for steps")
    void shouldFailWithInvalidStepsSelection() {
        // Setup
        List<HistoryEntry> historyEntries = createMultipleHistoryEntries(MAX_UNDO_STEPS);
        when(historyService.getHistory(any())).thenReturn(historyEntries);
        when(workflowService.getItemsInState(any(), any())).thenReturn(Collections.singletonList(mockWorkItem));
        when(mockWorkItem.id()).thenReturn(mockItemId);
        when(mockWorkItem.assignee()).thenReturn(currentUser);
        undoCommand.setSteps(true);
        
        // Simulate user providing invalid input
        System.setIn(new ByteArrayInputStream("abc\n".getBytes()));
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Invalid selection"), 
                "Should show error for invalid selection");
    }
    
    @Test
    @DisplayName("Should fail when undo attempt is for a previously active work item")
    void shouldFailWhenUndoingPreviouslyActiveWorkItem() {
        // Setup
        when(mockWorkItem.id()).thenReturn(mockItemId);
        when(workflowService.getItem(mockItemId)).thenReturn(mockWorkItem);
        when(mockWorkItem.assignee()).thenReturn(currentUser);
        
        // Mock the work item history service to indicate a different active item
        UUID currentActiveItemId = UUID.randomUUID();
        when(workflowService.getCurrentActiveItemId(currentUser)).thenReturn(currentActiveItemId);
        
        // Set up to undo a different item than the active one
        undoCommand.setItemId(mockItemId.toString());
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Undo history is cleared when changing work items"), 
                "Should show appropriate error message");
    }
    
    @Test
    @DisplayName("Should fail when trying to undo a closed work item")
    void shouldFailWhenUndoingClosedWorkItem() {
        // Setup
        when(mockWorkItem.id()).thenReturn(mockItemId);
        when(workflowService.getItem(mockItemId)).thenReturn(mockWorkItem);
        when(mockWorkItem.assignee()).thenReturn(currentUser);
        when(mockWorkItem.state()).thenReturn(WorkflowState.DONE);
        
        // Set up to undo a closed item
        undoCommand.setItemId(mockItemId.toString());
        
        // Execute
        Integer result = undoCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Undo history is cleared when work item is closed"), 
                "Should show appropriate error message");
    }
}