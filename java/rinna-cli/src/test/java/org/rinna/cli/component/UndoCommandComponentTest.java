/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

import org.junit.jupiter.api.*;
import org.rinna.cli.command.UndoCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

/**
 * Component test for the UndoCommand class.
 * 
 * These tests verify the integration between UndoCommand and its dependent services.
 */
@DisplayName("UndoCommand Component Tests")
public class UndoCommandComponentTest {

    private static final String TEST_WORK_ITEM_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String TEST_OPERATION_ID = "test-operation-id";
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final InputStream originalIn = System.in;
    
    private ServiceManager mockServiceManager;
    private MockItemService mockItemService;
    private MockHistoryService mockHistoryService;
    private MockWorkflowService mockWorkflowService;
    private MetadataService mockMetadataService;
    private ContextManager mockContextManager;
    
    @BeforeEach
    void setUp() {
        // Capture stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mock services
        mockServiceManager = mock(ServiceManager.class);
        mockItemService = mock(MockItemService.class);
        mockHistoryService = mock(MockHistoryService.class);
        mockWorkflowService = mock(MockWorkflowService.class);
        mockMetadataService = mock(MetadataService.class);
        mockContextManager = mock(ContextManager.class);
        
        // Configure mock service manager
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockHistoryService()).thenReturn(mockHistoryService);
        when(mockServiceManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up operation ID for metadata service
        when(mockMetadataService.startOperation(eq("undo"), eq("UPDATE"), any())).thenReturn(TEST_OPERATION_ID);
        when(mockMetadataService.trackOperation(anyString(), any())).thenReturn("sub-operation-id");
        
        // Mock ContextManager.getInstance()
        try (var staticMock = mockStatic(ContextManager.class)) {
            staticMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
        }
        
        // Set up sample work item
        WorkItem testWorkItem = createTestWorkItem();
        when(mockItemService.getItem(TEST_WORK_ITEM_ID)).thenReturn(testWorkItem);
        when(mockWorkflowService.getCurrentWorkItem(anyString())).thenReturn(testWorkItem);
        when(mockWorkflowService.findByStatus(WorkflowState.IN_PROGRESS))
            .thenReturn(Collections.singletonList(testWorkItem));
        
        // Set up basic history entries
        List<MockHistoryService.HistoryEntryRecord> historyEntries = createTestHistoryEntries();
        when(mockHistoryService.getHistory(any(UUID.class))).thenReturn(historyEntries);
    }
    
    @AfterEach
    void tearDown() {
        // Restore stdout, stderr, and stdin
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
        
        // Reset output capture
        outputCaptor.reset();
        errorCaptor.reset();
    }
    
    @Test
    @DisplayName("Should successfully undo state change and track operation")
    void shouldSuccessfullyUndoStateChangeAndTrackOperation() {
        // Given
        setupUserConfirmation("y");
        
        // Create UndoCommand
        UndoCommand command = new UndoCommand(mockServiceManager);
        command.setItemId(TEST_WORK_ITEM_ID);
        
        // When
        int exitCode = command.call();
        
        // Then
        assertEquals(0, exitCode);
        
        // Verify interactions between components
        verify(mockMetadataService).startOperation(eq("undo"), eq("UPDATE"), any());
        verify(mockHistoryService).getHistory(any(UUID.class));
        
        verify(mockWorkflowService).transition(
            eq(TEST_WORK_ITEM_ID),
            anyString(),
            eq(WorkflowState.NEW),
            anyString()
        );
        
        verify(mockMetadataService).completeOperation(anyString(), any());
        
        String output = outputCaptor.toString();
        assertTrue(output.contains("Successfully reverted state to NEW"));
    }
    
    @Test
    @DisplayName("Should successfully undo field changes and track operation")
    void shouldSuccessfullyUndoFieldChangesAndTrackOperation() {
        // Given
        setupUserConfirmation("y");
        
        // Set up history with field changes
        List<MockHistoryService.HistoryEntryRecord> fieldHistory = new ArrayList<>();
        fieldHistory.add(createHistoryEntry("FIELD_CHANGE", "john.doe", "Title: Old Title → New Title"));
        when(mockHistoryService.getHistory(any(UUID.class))).thenReturn(fieldHistory);
        
        // Create UndoCommand
        UndoCommand command = new UndoCommand(mockServiceManager);
        command.setItemId(TEST_WORK_ITEM_ID);
        
        // When
        int exitCode = command.call();
        
        // Then
        assertEquals(0, exitCode);
        
        // Verify interactions between components
        verify(mockItemService).updateTitle(
            eq(UUID.fromString(TEST_WORK_ITEM_ID)),
            eq("Old Title"),
            anyString()
        );
        
        verify(mockMetadataService).completeOperation(anyString(), any());
        
        String output = outputCaptor.toString();
        assertTrue(output.contains("Successfully reverted title to 'Old Title'"));
    }
    
    @Test
    @DisplayName("Should track operation failure when trying to undo with invalid input")
    void shouldTrackOperationFailureWhenTryingToUndoWithInvalidInput() {
        // Given
        // Invalid work item ID
        UndoCommand command = new UndoCommand(mockServiceManager);
        command.setItemId("invalid-id");
        
        // Configure ItemService to return null for invalid IDs
        when(mockItemService.getItem("invalid-id")).thenReturn(null);
        when(mockItemService.findItemByShortId("invalid-id")).thenReturn(null);
        
        // When
        int exitCode = command.call();
        
        // Then
        assertEquals(1, exitCode);
        
        // Verify operation tracking
        verify(mockMetadataService).startOperation(eq("undo"), eq("UPDATE"), any());
        verify(mockMetadataService).failOperation(eq(TEST_OPERATION_ID), any());
        
        String errorOutput = errorCaptor.toString();
        assertTrue(errorOutput.contains("Error: Work item not found"));
    }
    
    @Test
    @DisplayName("Should handle multiple dependent services when displaying steps")
    void shouldHandleMultipleDependentServicesWhenDisplayingSteps() {
        // Given
        setupUserConfirmation("q");
        
        // Create multiple history entries to test steps display
        List<MockHistoryService.HistoryEntryRecord> multipleHistory = new ArrayList<>();
        multipleHistory.add(createHistoryEntry("STATE_CHANGE", "john.doe", "NEW → IN_PROGRESS"));
        multipleHistory.add(createHistoryEntry("FIELD_CHANGE", "jane.smith", "Title: Old Title → New Title"));
        multipleHistory.add(createHistoryEntry("ASSIGNMENT", "john.doe", "alice.smith → john.doe"));
        
        when(mockHistoryService.getHistory(any(UUID.class))).thenReturn(multipleHistory);
        
        // Create UndoCommand
        UndoCommand command = new UndoCommand(mockServiceManager);
        command.setItemId(TEST_WORK_ITEM_ID);
        command.setSteps(true);
        
        // When
        int exitCode = command.call();
        
        // Then
        assertEquals(0, exitCode);
        
        // Verify interactions with multiple services
        verify(mockHistoryService).getHistory(any(UUID.class));
        verify(mockMetadataService).startOperation(eq("undo"), eq("UPDATE"), any());
        verify(mockMetadataService).completeOperation(anyString(), any());
        
        // Verify context was updated
        verify(mockContextManager).setLastViewedItem(any(WorkItem.class));
        
        String output = outputCaptor.toString();
        assertTrue(output.contains("Available changes to undo"));
        assertTrue(output.contains("State changed: NEW → IN_PROGRESS"));
        assertTrue(output.contains("Field changed: Title: Old Title → New Title"));
        assertTrue(output.contains("Assignment changed: alice.smith → john.doe"));
    }
    
    @Test
    @DisplayName("Should properly coordinate between history service and workflow service")
    void shouldProperlyCoordinateBetweenHistoryServiceAndWorkflowService() {
        // Given
        setupUserConfirmation("y");
        
        // Create UndoCommand for current work item (no ID specified)
        UndoCommand command = new UndoCommand(mockServiceManager);
        // No item ID specified
        
        // When
        int exitCode = command.call();
        
        // Then
        assertEquals(0, exitCode);
        
        // Verify interactions between components
        verify(mockWorkflowService).findByStatus(WorkflowState.IN_PROGRESS);
        verify(mockHistoryService).getHistory(any(UUID.class));
        
        verify(mockWorkflowService).transition(
            anyString(),
            anyString(),
            eq(WorkflowState.NEW),
            anyString()
        );
        
        verify(mockMetadataService).completeOperation(anyString(), any());
        
        String output = outputCaptor.toString();
        assertTrue(output.contains("Successfully reverted state to NEW"));
    }
    
    @Test
    @DisplayName("Should honor force flag and skip confirmation")
    void shouldHonorForceFlagAndSkipConfirmation() {
        // Given
        // Create UndoCommand with force flag
        UndoCommand command = new UndoCommand(mockServiceManager);
        command.setItemId(TEST_WORK_ITEM_ID);
        command.setForce(true);
        
        // When
        int exitCode = command.call();
        
        // Then
        assertEquals(0, exitCode);
        
        // Verify transition was called without waiting for user input
        verify(mockWorkflowService).transition(
            eq(TEST_WORK_ITEM_ID),
            anyString(),
            eq(WorkflowState.NEW),
            anyString()
        );
        
        verify(mockMetadataService).completeOperation(anyString(), any());
        
        String output = outputCaptor.toString();
        assertTrue(output.contains("Successfully reverted state to NEW"));
    }
    
    @Test
    @DisplayName("Should properly integrate with metadata service for operation tracking")
    void shouldProperlyIntegrateWithMetadataServiceForOperationTracking() {
        // Given
        setupUserConfirmation("y");
        
        // Create UndoCommand
        UndoCommand command = new UndoCommand(mockServiceManager);
        command.setItemId(TEST_WORK_ITEM_ID);
        
        // When
        int exitCode = command.call();
        
        // Then
        assertEquals(0, exitCode);
        
        // Verify metadata service interactions for operation tracking
        verify(mockMetadataService).startOperation(eq("undo"), eq("UPDATE"), any());
        verify(mockMetadataService, atLeastOnce()).trackOperation(anyString(), any());
        verify(mockMetadataService, atLeastOnce()).completeOperation(anyString(), any());
        
        // Verify confirmation data is tracked
        verify(mockMetadataService).trackOperation(eq("undo-confirm"), any());
    }
    
    // Helper methods
    
    private void setupUserConfirmation(String input) {
        ByteArrayInputStream mockInput = new ByteArrayInputStream(input.getBytes());
        System.setIn(mockInput);
    }
    
    private WorkItem createTestWorkItem() {
        WorkItem workItem = new WorkItem();
        workItem.setId(TEST_WORK_ITEM_ID);
        workItem.setTitle("Test Work Item");
        workItem.setDescription("This is a test description");
        workItem.setType(WorkItemType.TASK);
        workItem.setPriority(Priority.MEDIUM);
        workItem.setState(WorkflowState.IN_PROGRESS);
        workItem.setAssignee(System.getProperty("user.name")); // Current user
        workItem.setCreated(LocalDateTime.now());
        workItem.setUpdated(LocalDateTime.now());
        return workItem;
    }
    
    private List<MockHistoryService.HistoryEntryRecord> createTestHistoryEntries() {
        List<MockHistoryService.HistoryEntryRecord> entries = new ArrayList<>();
        entries.add(createHistoryEntry("STATE_CHANGE", "john.doe", "NEW → IN_PROGRESS"));
        return entries;
    }
    
    private MockHistoryService.HistoryEntryRecord createHistoryEntry(String type, String user, String content) {
        MockHistoryService.HistoryEntryType entryType;
        try {
            entryType = MockHistoryService.HistoryEntryType.valueOf(type);
        } catch (IllegalArgumentException e) {
            entryType = MockHistoryService.HistoryEntryType.UPDATED; // Default
        }
        
        return new MockHistoryService.HistoryEntryRecord(
            UUID.randomUUID(),
            UUID.fromString(TEST_WORK_ITEM_ID),
            entryType,
            user,
            content,
            Map.of(),
            Date.from(Instant.now().minus(1, ChronoUnit.HOURS))
        );
    }
}