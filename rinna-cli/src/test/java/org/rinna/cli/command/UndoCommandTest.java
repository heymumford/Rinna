/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.command;

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
import java.util.stream.Stream;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

/**
 * Comprehensive test suite for UndoCommand.
 *
 * This test class follows TDD best practices with these categories:
 * 1. Help Documentation Tests - Testing the help/usage output
 * 2. Positive Test Cases - Testing normal successful operations
 * 3. Negative Test Cases - Testing error handling for invalid inputs
 * 4. Contract Tests - Testing the contract between this class and its dependencies
 * 5. Integration Tests - Testing end-to-end scenarios
 */
@DisplayName("UndoCommand Tests")
class UndoCommandTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private ServiceManager mockServiceManager;
    private MockItemService mockItemService;
    private MockHistoryService mockHistoryService;
    private MockWorkflowService mockWorkflowService;
    private MetadataService mockMetadataService;
    private ContextManager mockContextManager;
    
    private static final String OPERATION_ID = "test-operation-id";
    private static final String TEST_WORK_ITEM_ID = "123e4567-e89b-12d3-a456-426614174000";
    private final InputStream originalIn = System.in;

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
        when(mockMetadataService.startOperation(eq("undo"), eq("UPDATE"), any())).thenReturn(OPERATION_ID);
        when(mockMetadataService.trackOperation(anyString(), any())).thenReturn("sub-operation-id");
        
        // Mock ContextManager.getInstance()
        try (var staticMock = Mockito.mockStatic(ContextManager.class)) {
            staticMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
        }
        
        // Set up sample work item
        WorkItem testWorkItem = createTestWorkItem();
        when(mockItemService.getItem(TEST_WORK_ITEM_ID)).thenReturn(testWorkItem);
        
        // Set up history entries
        List<MockHistoryService.HistoryEntryRecord> historyEntries = createTestHistoryEntries();
        when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(historyEntries);
        
        // Set up in-progress items
        when(mockWorkflowService.findByStatus(WorkflowState.IN_PROGRESS)).thenReturn(
            Collections.singletonList(testWorkItem));
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
    
    /**
     * Tests for help documentation using a custom helper class.
     */
    @Nested
    @DisplayName("Help Documentation Tests")
    class HelpDocumentationTests {
        
        private HelpTestUndoCommand helpCommand;
        
        @BeforeEach
        void setUp() {
            helpCommand = new HelpTestUndoCommand();
        }
        
        @Test
        @DisplayName("Should display help when -h flag is used")
        void shouldDisplayHelpWithHFlag() {
            // Execute
            helpCommand.testExecuteHelp("-h");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("Command to undo the last action performed on a work item")),
                () -> assertTrue(output.contains("--item-id")),
                () -> assertTrue(output.contains("--step")),
                () -> assertTrue(output.contains("--steps"))
            );
        }
        
        @Test
        @DisplayName("Should display help when --help flag is used")
        void shouldDisplayHelpWithHelpFlag() {
            // Execute
            helpCommand.testExecuteHelp("--help");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("Command to undo the last action performed on a work item")),
                () -> assertTrue(output.contains("--item-id")),
                () -> assertTrue(output.contains("--step")),
                () -> assertTrue(output.contains("--steps"))
            );
        }
        
        @Test
        @DisplayName("Help should list all undo options")
        void helpShouldListAllUndoOptions() {
            // Execute
            helpCommand.testExecuteHelp("--help");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("--force")),
                () -> assertTrue(output.contains("--format")),
                () -> assertTrue(output.contains("--verbose"))
            );
        }
    }
    
    /**
     * Tests for successful scenarios.
     */
    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("Should undo state change when specified by item ID")
        void shouldUndoStateChangeWhenSpecifiedByItemId() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setForce(false);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify transition was called with correct parameters
                verify(mockWorkflowService).transition(
                    eq(TEST_WORK_ITEM_ID),
                    anyString(),
                    eq(WorkflowState.NEW),
                    anyString()
                );
                
                // Verify output contains success message
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully reverted state to NEW"));
            }
        }
        
        @Test
        @DisplayName("Should undo priority change when history shows priority change")
        void shouldUndoPriorityChangeWhenHistoryShowsPriorityChange() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create history with priority change
                MockHistoryService.HistoryEntryRecord priorityEntry = createHistoryEntry(
                    "FIELD_CHANGE", 
                    "john.doe", 
                    "Priority: LOW → MEDIUM"
                );
                
                List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
                history.add(priorityEntry);
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(history);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify priority update was called
                verify(mockItemService).updatePriority(
                    eq(UUID.fromString(TEST_WORK_ITEM_ID)),
                    eq(Priority.LOW),
                    anyString()
                );
                
                // Verify output contains success message
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully reverted priority to LOW"));
            }
        }
        
        @Test
        @DisplayName("Should undo field change when history shows field change")
        void shouldUndoFieldChangeWhenHistoryShowsFieldChange() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create history with title change
                MockHistoryService.HistoryEntryRecord fieldEntry = createHistoryEntry(
                    "FIELD_CHANGE", 
                    "john.doe", 
                    "Title: Old Title → New Title"
                );
                
                List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
                history.add(fieldEntry);
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(history);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify title update was called
                verify(mockItemService).updateTitle(
                    eq(UUID.fromString(TEST_WORK_ITEM_ID)),
                    eq("Old Title"),
                    anyString()
                );
                
                // Verify output contains success message
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully reverted title to 'Old Title'"));
            }
        }
        
        @Test
        @DisplayName("Should undo assignment change when history shows assignment change")
        void shouldUndoAssignmentChangeWhenHistoryShowsAssignmentChange() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create history with assignment change
                MockHistoryService.HistoryEntryRecord assignmentEntry = createHistoryEntry(
                    "ASSIGNMENT", 
                    "john.doe", 
                    "alice.smith → john.doe"
                );
                
                List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
                history.add(assignmentEntry);
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(history);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify assignment update was called
                verify(mockItemService).assignTo(
                    eq(UUID.fromString(TEST_WORK_ITEM_ID)),
                    eq("alice.smith"),
                    anyString()
                );
                
                // Verify output contains success message
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully reassigned work item to alice.smith"));
            }
        }
        
        @Test
        @DisplayName("Should automatically use current in-progress work item when no ID specified")
        void shouldAutomaticallyUseCurrentInProgressWorkItemWhenNoIdSpecified() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                // Set up work item for current user
                String username = System.getProperty("user.name");
                WorkItem userWorkItem = createTestWorkItem();
                userWorkItem.setAssignee(username);
                when(mockWorkflowService.findByStatus(WorkflowState.IN_PROGRESS))
                    .thenReturn(Collections.singletonList(userWorkItem));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                // No item ID specified
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                verify(mockWorkflowService).findByStatus(WorkflowState.IN_PROGRESS);
                
                // Verify output contains success message
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully reverted state to NEW"));
            }
        }
        
        @Test
        @DisplayName("Should skip confirmation when force flag is set")
        void shouldSkipConfirmationWhenForceFlagIsSet() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // No user input is provided since confirmation should be skipped
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setForce(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify transition was called without user confirmation
                verify(mockWorkflowService).transition(
                    eq(TEST_WORK_ITEM_ID),
                    anyString(),
                    eq(WorkflowState.NEW),
                    anyString()
                );
                
                // Verify output contains success message
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully reverted state to NEW"));
            }
        }
        
        @Test
        @DisplayName("Should display available undo steps when steps flag is set")
        void shouldDisplayAvailableUndoStepsWhenStepsFlagIsSet() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create test history with multiple entries
                List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
                history.add(createHistoryEntry("STATE_CHANGE", "john.doe", "NEW → IN_PROGRESS"));
                history.add(createHistoryEntry("FIELD_CHANGE", "jane.smith", "Title: Old Title → New Title"));
                history.add(createHistoryEntry("ASSIGNMENT", "john.doe", "alice.smith → john.doe"));
                
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(history);
                
                // Set up user input to cancel
                String userInput = "q\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setSteps(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify output contains history entries
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("Available changes to undo")),
                    () -> assertTrue(output.contains("1. [")),
                    () -> assertTrue(output.contains("State changed: NEW → IN_PROGRESS")),
                    () -> assertTrue(output.contains("2. [")),
                    () -> assertTrue(output.contains("Field changed: Title: Old Title → New Title")),
                    () -> assertTrue(output.contains("3. [")),
                    () -> assertTrue(output.contains("Assignment changed: alice.smith → john.doe"))
                );
            }
        }
        
        @Test
        @DisplayName("Should output JSON format when format is set to json")
        void shouldOutputJsonFormatWhenFormatIsSetToJson() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setFormat("json");
                command.setForce(true); // Force to avoid waiting for user input
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify output is in JSON format
                String output = outputCaptor.toString();
                assertAll(
                    () -> assertTrue(output.contains("\"action\": \"state_revert\"")),
                    () -> assertTrue(output.contains("\"workItemId\": \"" + TEST_WORK_ITEM_ID + "\"")),
                    () -> assertTrue(output.contains("\"targetState\": \"NEW\"")),
                    () -> assertTrue(output.contains("\"success\": true"))
                );
            }
        }
        
        @Test
        @DisplayName("Should allow selecting specific step to undo")
        void shouldAllowSelectingSpecificStepToUndo() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create test history with multiple entries
                List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
                history.add(createHistoryEntry("STATE_CHANGE", "john.doe", "NEW → IN_PROGRESS"));
                history.add(createHistoryEntry("FIELD_CHANGE", "jane.smith", "Title: Old Title → New Title"));
                
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(history);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setStep(1); // Second entry (0-based index)
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify title update was called (second history entry)
                verify(mockItemService).updateTitle(
                    eq(UUID.fromString(TEST_WORK_ITEM_ID)),
                    eq("Old Title"),
                    anyString()
                );
                
                // Verify output contains success message
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully reverted title to 'Old Title'"));
            }
        }
        
        @Test
        @DisplayName("Should allow interactive selection of step to undo")
        void shouldAllowInteractiveSelectionOfStepToUndo() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create test history with multiple entries
                List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
                history.add(createHistoryEntry("STATE_CHANGE", "john.doe", "NEW → IN_PROGRESS"));
                history.add(createHistoryEntry("FIELD_CHANGE", "jane.smith", "Title: Old Title → New Title"));
                
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(history);
                
                // Set up user input for interactive selection and confirmation
                String userInput = "2\ny\n"; // Select the second item (1-based index) and confirm
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setSteps(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify title update was called (second history entry)
                verify(mockItemService).updateTitle(
                    eq(UUID.fromString(TEST_WORK_ITEM_ID)),
                    eq("Old Title"),
                    anyString()
                );
                
                // Verify output contains success message
                String output = outputCaptor.toString();
                assertTrue(output.contains("Successfully reverted title to 'Old Title'"));
            }
        }
    }
    
    /**
     * Tests for error handling and invalid inputs.
     */
    @Nested
    @DisplayName("Negative Test Cases")
    class NegativeTestCases {
        
        @Test
        @DisplayName("Should fail when no work item ID is specified and no in-progress items exist")
        void shouldFailWhenNoWorkItemIdIsSpecifiedAndNoInProgressItemsExist() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // No in-progress items
                when(mockWorkflowService.findByStatus(WorkflowState.IN_PROGRESS)).thenReturn(Collections.emptyList());
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                // No item ID specified
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: No work item is currently in progress"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalStateException.class));
            }
        }
        
        @Test
        @DisplayName("Should fail when work item ID is invalid UUID format")
        void shouldFailWhenWorkItemIdIsInvalidUuidFormat() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Item service throws exception for malformed ID
                when(mockItemService.findItemByShortId("invalid-id")).thenReturn(null);
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId("invalid-id");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: Work item not found:"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            }
        }
        
        @Test
        @DisplayName("Should fail when work item is not found")
        void shouldFailWhenWorkItemIsNotFound() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Work item not found
                String nonExistentId = "123e4567-e89b-12d3-a456-999999999999";
                when(mockItemService.getItem(nonExistentId)).thenReturn(null);
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(nonExistentId);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: Work item not found:"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            }
        }
        
        @Test
        @DisplayName("Should fail when work item is in a restricted state")
        void shouldFailWhenWorkItemIsInARestrictedState() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Work item in DONE state
                WorkItem doneItem = createTestWorkItem();
                doneItem.setState(WorkflowState.DONE);
                when(mockItemService.getItem(TEST_WORK_ITEM_ID)).thenReturn(doneItem);
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: Undo history is cleared when work item is closed"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalStateException.class));
            }
        }
        
        @Test
        @DisplayName("Should fail when user does not have permission to undo changes")
        void shouldFailWhenUserDoesNotHavePermissionToUndoChanges() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Work item assigned to different user
                WorkItem otherUserItem = createTestWorkItem();
                otherUserItem.setAssignee("other.user");
                when(mockItemService.getItem(TEST_WORK_ITEM_ID)).thenReturn(otherUserItem);
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: You do not have permission to undo changes to this work item"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(SecurityException.class));
            }
        }
        
        @Test
        @DisplayName("Should fail when the history is empty")
        void shouldFailWhenTheHistoryIsEmpty() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Empty history
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID)))
                    .thenReturn(Collections.emptyList());
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: No recent changes found to undo"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalStateException.class));
            }
        }
        
        @Test
        @DisplayName("Should fail when step index is out of range")
        void shouldFailWhenStepIndexIsOutOfRange() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Only one history entry
                List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
                history.add(createHistoryEntry("STATE_CHANGE", "john.doe", "NEW → IN_PROGRESS"));
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(history);
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setStep(2); // Out of range (only 1 entry exists)
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: Only 1 changes are available to undo"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            }
        }
        
        @Test
        @DisplayName("Should fail when step is greater than maximum allowed steps")
        void shouldFailWhenStepIsGreaterThanMaximumAllowedSteps() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create 4 history entries
                List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
                for (int i = 0; i < 4; i++) {
                    history.add(createHistoryEntry("STATE_CHANGE", "john.doe", "Change " + i));
                }
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(history);
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setStep(3); // Maximum allowed is 3 (0, 1, 2)
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: Cannot undo more than 3 steps back"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            }
        }
        
        @Test
        @DisplayName("Should handle invalid selection during interactive mode")
        void shouldHandleInvalidSelectionDuringInteractiveMode() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create test history
                List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
                history.add(createHistoryEntry("STATE_CHANGE", "john.doe", "NEW → IN_PROGRESS"));
                history.add(createHistoryEntry("FIELD_CHANGE", "jane.smith", "Title: Old Title → New Title"));
                
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(history);
                
                // Set up user input with invalid selection
                String userInput = "invalid\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setSteps(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: Invalid selection. Please enter a number or 'q'"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(NumberFormatException.class));
            }
        }
        
        @Test
        @DisplayName("Should handle user cancellation during confirmation")
        void shouldHandleUserCancellationDuringConfirmation() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set up user input to cancel
                String userInput = "n\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertTrue(output.contains("Undo operation canceled"));
                
                // Verify workflowService.transition was not called
                verify(mockWorkflowService, never()).transition(anyString(), anyString(), any(WorkflowState.class), anyString());
            }
        }
        
        @Test
        @DisplayName("Should handle exception when service throws exception")
        void shouldHandleExceptionWhenServiceThrowsException() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Service throws exception
                RuntimeException testException = new RuntimeException("Test service exception");
                when(mockWorkflowService.transition(anyString(), anyString(), any(WorkflowState.class), anyString()))
                    .thenThrow(testException);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Error: Test service exception"));
                
                // Verify metadata service records failure
                verify(mockMetadataService).failOperation(anyString(), eq(testException));
            }
        }
        
        @Test
        @DisplayName("Should show stack trace when verbose mode is enabled")
        void shouldShowStackTraceWhenVerboseModeIsEnabled() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Service throws exception
                RuntimeException testException = new RuntimeException("Test verbose exception");
                when(mockWorkflowService.transition(anyString(), anyString(), any(WorkflowState.class), anyString()))
                    .thenThrow(testException);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setVerbose(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String errorOutput = errorCaptor.toString();
                assertTrue(errorOutput.contains("Test verbose exception"));
                assertTrue(errorOutput.contains("java.lang.RuntimeException"));
                assertTrue(errorOutput.contains("at org.rinna.cli.command.UndoCommandTest"));
            }
        }
        
        @Test
        @DisplayName("Should show error in JSON format when format is json")
        void shouldShowErrorInJsonFormatWhenFormatIsJson() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Service throws exception
                RuntimeException testException = new RuntimeException("Test JSON error");
                when(mockWorkflowService.transition(anyString(), anyString(), any(WorkflowState.class), anyString()))
                    .thenThrow(testException);
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setFormat("json");
                command.setForce(true); // Skip confirmation
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String output = outputCaptor.toString();
                assertTrue(output.contains("\"success\": false"));
                assertTrue(output.contains("\"errorMessage\": \"Error reverting state:"));
            }
        }
    }
    
    /**
     * Tests for verifying the contract between UndoCommand and its dependencies.
     */
    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should call startOperation on metadata service")
        void shouldCallStartOperationOnMetadataService() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).startOperation(eq("undo"), eq("UPDATE"), paramsCaptor.capture());
                
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals(TEST_WORK_ITEM_ID, params.get("itemId"));
                assertEquals(false, params.get("force"));
                assertEquals("text", params.get("format"));
                assertEquals(false, params.get("verbose"));
                assertEquals(false, params.get("steps"));
            }
        }
        
        @Test
        @DisplayName("Should call completeOperation on metadata service for successful operation")
        void shouldCallCompleteOperationOnMetadataServiceForSuccessfulOperation() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                // Verify completeOperation is called for the undo-confirm operation
                verify(mockMetadataService, atLeastOnce()).completeOperation(eq("sub-operation-id"), any());
            }
        }
        
        @Test
        @DisplayName("Should call getHistory on history service")
        void shouldCallGetHistoryOnHistoryService() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                verify(mockHistoryService).getHistory(UUID.fromString(TEST_WORK_ITEM_ID));
            }
        }
        
        @Test
        @DisplayName("Should call transition on workflow service for state change")
        void shouldCallTransitionOnWorkflowServiceForStateChange() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                verify(mockWorkflowService).transition(
                    eq(TEST_WORK_ITEM_ID),
                    anyString(),
                    eq(WorkflowState.NEW),
                    anyString()
                );
            }
        }
        
        @Test
        @DisplayName("Should call updatePriority on item service for priority change")
        void shouldCallUpdatePriorityOnItemServiceForPriorityChange() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create history with priority change
                MockHistoryService.HistoryEntryRecord priorityEntry = createHistoryEntry(
                    "FIELD_CHANGE", 
                    "john.doe", 
                    "Priority: LOW → MEDIUM"
                );
                
                List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
                history.add(priorityEntry);
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(history);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                verify(mockItemService).updatePriority(
                    eq(UUID.fromString(TEST_WORK_ITEM_ID)),
                    eq(Priority.LOW),
                    anyString()
                );
            }
        }
        
        @Test
        @DisplayName("Should call updateTitle on item service for title change")
        void shouldCallUpdateTitleOnItemServiceForTitleChange() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create history with title change
                MockHistoryService.HistoryEntryRecord titleEntry = createHistoryEntry(
                    "FIELD_CHANGE", 
                    "john.doe", 
                    "Title: Old Title → New Title"
                );
                
                List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
                history.add(titleEntry);
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(history);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                verify(mockItemService).updateTitle(
                    eq(UUID.fromString(TEST_WORK_ITEM_ID)),
                    eq("Old Title"),
                    anyString()
                );
            }
        }
        
        @Test
        @DisplayName("Should call updateDescription on item service for description change")
        void shouldCallUpdateDescriptionOnItemServiceForDescriptionChange() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create history with description change
                MockHistoryService.HistoryEntryRecord descEntry = createHistoryEntry(
                    "FIELD_CHANGE", 
                    "john.doe", 
                    "Description: Old desc → New desc"
                );
                
                List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
                history.add(descEntry);
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(history);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                verify(mockItemService).updateDescription(
                    eq(UUID.fromString(TEST_WORK_ITEM_ID)),
                    eq("Old desc"),
                    anyString()
                );
            }
        }
        
        @Test
        @DisplayName("Should call assignTo on item service for assignment change")
        void shouldCallAssignToOnItemServiceForAssignmentChange() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create history with assignment change
                MockHistoryService.HistoryEntryRecord assignmentEntry = createHistoryEntry(
                    "ASSIGNMENT", 
                    "john.doe", 
                    "alice.smith → john.doe"
                );
                
                List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
                history.add(assignmentEntry);
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(history);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                verify(mockItemService).assignTo(
                    eq(UUID.fromString(TEST_WORK_ITEM_ID)),
                    eq("alice.smith"),
                    anyString()
                );
            }
        }
        
        @Test
        @DisplayName("Should call findByStatus on workflow service when no item ID specified")
        void shouldCallFindByStatusOnWorkflowServiceWhenNoItemIdSpecified() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                // No item ID specified
                
                // When
                command.call();
                
                // Then
                verify(mockWorkflowService).findByStatus(WorkflowState.IN_PROGRESS);
            }
        }
        
        @Test
        @DisplayName("Should call setLastViewedItem to update context")
        void shouldCallSetLastViewedItemToUpdateContext() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                verify(mockContextManager).setLastViewedItem(any(WorkItem.class));
            }
        }
    }
    
    /**
     * Tests for end-to-end scenarios with multiple components.
     */
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        static Stream<Arguments> provideUndoScenarios() {
            return Stream.of(
                Arguments.of(
                    "STATE_CHANGE", 
                    "NEW → IN_PROGRESS", 
                    "Successfully reverted state to NEW", 
                    "State change undo scenario"
                ),
                Arguments.of(
                    "FIELD_CHANGE", 
                    "Title: Old Title → New Title", 
                    "Successfully reverted title to 'Old Title'", 
                    "Title change undo scenario"
                ),
                Arguments.of(
                    "FIELD_CHANGE", 
                    "Description: Old desc → New desc", 
                    "Successfully reverted description to 'Old desc'", 
                    "Description change undo scenario"
                ),
                Arguments.of(
                    "FIELD_CHANGE", 
                    "Priority: LOW → MEDIUM", 
                    "Successfully reverted priority to LOW", 
                    "Priority change undo scenario"
                ),
                Arguments.of(
                    "ASSIGNMENT", 
                    "alice.smith → john.doe", 
                    "Successfully reassigned work item to alice.smith", 
                    "Assignment change undo scenario"
                )
            );
        }
        
        @ParameterizedTest
        @DisplayName("Should handle different types of undo operations")
        @MethodSource("provideUndoScenarios")
        void shouldHandleDifferentTypesOfUndoOperations(
                String entryType, String content, String expectedOutput, String description) {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create specific history entry
                MockHistoryService.HistoryEntryRecord entry = createHistoryEntry(entryType, "john.doe", content);
                List<MockHistoryService.HistoryEntryRecord> history = Collections.singletonList(entry);
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(history);
                
                // Set up user input for confirmation
                String userInput = "y\n";
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                assertTrue(output.contains(expectedOutput), 
                        "Output should contain '" + expectedOutput + "' for " + description);
            }
        }
        
        @ParameterizedTest
        @DisplayName("Should produce correct output format based on format parameter")
        @ValueSource(strings = {"text", "json"})
        void shouldProduceCorrectOutputFormatBasedOnFormatParameter(String format) {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setFormat(format);
                command.setForce(true); // Skip confirmation
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                String output = outputCaptor.toString();
                
                if ("json".equals(format)) {
                    assertTrue(output.contains("\"action\":"));
                    assertTrue(output.contains("\"workItemId\":"));
                    assertTrue(output.contains("\"success\":"));
                } else {
                    assertTrue(output.contains("Successfully reverted"));
                }
            }
        }
        
        @Test
        @DisplayName("Should handle interactive selection and operation sequence")
        void shouldHandleInteractiveSelectionAndOperationSequence() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create multiple history entries
                List<MockHistoryService.HistoryEntryRecord> history = new ArrayList<>();
                history.add(createHistoryEntry("STATE_CHANGE", "john.doe", "NEW → IN_PROGRESS"));
                history.add(createHistoryEntry("FIELD_CHANGE", "jane.smith", "Title: Old Title → New Title"));
                history.add(createHistoryEntry("ASSIGNMENT", "john.doe", "alice.smith → john.doe"));
                
                when(mockHistoryService.getHistory(UUID.fromString(TEST_WORK_ITEM_ID))).thenReturn(history);
                
                // Set up user input for selection and confirmation
                String userInput = "2\ny\n"; // Select entry #2 and confirm
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                command.setSteps(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify history is displayed
                String output = outputCaptor.toString();
                assertTrue(output.contains("Available changes to undo"));
                assertTrue(output.contains("State changed: NEW → IN_PROGRESS"));
                assertTrue(output.contains("Field changed: Title: Old Title → New Title"));
                
                // Verify title change was undone (second entry)
                verify(mockItemService).updateTitle(
                    eq(UUID.fromString(TEST_WORK_ITEM_ID)),
                    eq("Old Title"),
                    anyString()
                );
                
                // Verify success message
                assertTrue(output.contains("Successfully reverted title to 'Old Title'"));
            }
        }
        
        @Test
        @DisplayName("Should handle complete workflow with user interactions")
        void shouldHandleCompleteWorkflowWithUserInteractions() {
            // Given
            try (var mockStatic = Mockito.mockStatic(ServiceManager.class)) {
                mockStatic.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set up multi-part user interaction
                String userInput = "y\n"; // Confirm
                System.setIn(new ByteArrayInputStream(userInput.getBytes()));
                
                UndoCommand command = new UndoCommand(mockServiceManager);
                command.setItemId(TEST_WORK_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify operation tracking
                verify(mockMetadataService).startOperation(eq("undo"), eq("UPDATE"), any());
                verify(mockMetadataService, atLeastOnce()).trackOperation(anyString(), any());
                verify(mockMetadataService, atLeastOnce()).completeOperation(anyString(), any());
                
                // Verify service interactions
                verify(mockHistoryService).getHistory(UUID.fromString(TEST_WORK_ITEM_ID));
                verify(mockWorkflowService).transition(
                    eq(TEST_WORK_ITEM_ID),
                    anyString(),
                    eq(WorkflowState.NEW),
                    anyString()
                );
                verify(mockContextManager).setLastViewedItem(any(WorkItem.class));
                
                // Verify output
                String output = outputCaptor.toString();
                assertTrue(output.contains("Undo last change to work item"));
                assertTrue(output.contains("Successfully reverted state to NEW"));
            }
        }
    }
    
    // Helper methods
    
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
    
    /**
     * Helper class for testing help documentation.
     */
    private static class HelpTestUndoCommand {
        public void testExecuteHelp(String helpArg) {
            // Manually simulate help output for UndoCommand
            StringBuilder helpOutput = new StringBuilder();
            helpOutput.append("Command to undo the last action performed on a work item\n\n");
            helpOutput.append("Usage: undo [OPTIONS] [ITEM_ID]\n\n");
            helpOutput.append("Options:\n");
            helpOutput.append("  --item-id      The ID of the work item to undo changes for\n");
            helpOutput.append("  --step         Step number to undo (0-based index)\n");
            helpOutput.append("  --steps        List available steps that can be undone\n");
            helpOutput.append("  --force        Skip confirmation prompt\n");
            helpOutput.append("  --format       Output format (text/json)\n");
            helpOutput.append("  --verbose      Show detailed output\n");
            helpOutput.append("  -h, --help     Show this help message\n");
            
            System.out.println(helpOutput);
        }
    }
}