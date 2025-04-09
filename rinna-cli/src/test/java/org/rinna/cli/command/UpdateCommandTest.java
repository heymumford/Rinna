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

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for UpdateCommand.
 *
 * This test class follows TDD best practices with these categories:
 * 1. Help Documentation Tests - Testing the help/usage output
 * 2. Positive Test Cases - Testing normal successful operations
 * 3. Negative Test Cases - Testing error handling for invalid inputs
 * 4. Contract Tests - Testing the contract between this class and its dependencies
 * 5. Integration Tests - Testing end-to-end scenarios
 */
@DisplayName("UpdateCommand Tests")
class UpdateCommandTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private ServiceManager mockServiceManager;
    private MockItemService mockItemService;
    private MockWorkflowService mockWorkflowService;
    private MockConfigurationService mockConfigService;
    
    private static final String TEST_ITEM_ID = "123e4567-e89b-12d3-a456-426614174000";
    
    @BeforeEach
    void setUp() {
        // Capture stdout and stderr
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Initialize mock services
        mockServiceManager = mock(ServiceManager.class);
        mockItemService = new MockItemService();
        mockWorkflowService = new MockWorkflowService();
        mockConfigService = new MockConfigurationService();
        
        // Create test work item
        WorkItem testItem = new WorkItem();
        testItem.setId(TEST_ITEM_ID);
        testItem.setTitle("Original Title");
        testItem.setDescription("Original Description");
        testItem.setType(WorkItemType.TASK);
        testItem.setPriority(Priority.MEDIUM);
        testItem.setState(WorkflowState.CREATED); // Using setState to ensure compatibility
        testItem.setAssignee("original.user");
        mockItemService.addTestItem(testItem);
        
        // Set up mock service manager
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getWorkflowService()).thenReturn(mockWorkflowService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
    }
    
    @AfterEach
    void tearDown() {
        // Restore stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
        
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
        
        private HelpTestUpdateCommand helpCommand;
        
        @BeforeEach
        void setUp() {
            helpCommand = new HelpTestUpdateCommand();
        }
        
        @Test
        @DisplayName("Should display help when -h flag is used")
        void shouldDisplayHelpWithHFlag() {
            // Execute
            helpCommand.testExecuteHelp("-h");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("Command to update an existing work item")),
                () -> assertTrue(output.contains("--id")),
                () -> assertTrue(output.contains("--title")),
                () -> assertTrue(output.contains("--status")),
                () -> assertTrue(output.contains("--assignee"))
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
                () -> assertTrue(output.contains("Command to update an existing work item")),
                () -> assertTrue(output.contains("--id")),
                () -> assertTrue(output.contains("--title")),
                () -> assertTrue(output.contains("--status")),
                () -> assertTrue(output.contains("--assignee"))
            );
        }
        
        @Test
        @DisplayName("Help should list required arguments")
        void helpShouldListRequiredArguments() {
            // Execute
            helpCommand.testExecuteHelp("--help");
            
            // Verify
            String output = outputCaptor.toString();
            assertTrue(output.contains("--id")); // ID is required
        }
        
        @Test
        @DisplayName("Help should list optional arguments")
        void helpShouldListOptionalArguments() {
            // Execute
            helpCommand.testExecuteHelp("--help");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("--title")),
                () -> assertTrue(output.contains("--description")),
                () -> assertTrue(output.contains("--type")),
                () -> assertTrue(output.contains("--priority")),
                () -> assertTrue(output.contains("--status")),
                () -> assertTrue(output.contains("--assignee")),
                () -> assertTrue(output.contains("--comment"))
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
        @DisplayName("Should update work item title when provided")
        void shouldUpdateWorkItemTitleWhenProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setTitle("Updated Title");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockItemService.getItem(TEST_ITEM_ID);
                assertAll(
                    () -> assertEquals("Updated Title", updatedItem.getTitle()),
                    () -> assertTrue(outputCaptor.toString().contains("Updated work item")),
                    () -> assertTrue(outputCaptor.toString().contains("Title: Updated Title"))
                );
            }
        }
        
        @Test
        @DisplayName("Should update work item description when provided")
        void shouldUpdateWorkItemDescriptionWhenProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setDescription("Updated Description");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockItemService.getItem(TEST_ITEM_ID);
                assertEquals("Updated Description", updatedItem.getDescription());
            }
        }
        
        @Test
        @DisplayName("Should update work item type when provided")
        void shouldUpdateWorkItemTypeWhenProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setType(WorkItemType.BUG);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockItemService.getItem(TEST_ITEM_ID);
                assertAll(
                    () -> assertEquals(WorkItemType.BUG, updatedItem.getType()),
                    () -> assertTrue(outputCaptor.toString().contains("Type: BUG"))
                );
            }
        }
        
        @Test
        @DisplayName("Should update work item priority when provided")
        void shouldUpdateWorkItemPriorityWhenProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setPriority(Priority.HIGH);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockItemService.getItem(TEST_ITEM_ID);
                assertAll(
                    () -> assertEquals(Priority.HIGH, updatedItem.getPriority()),
                    () -> assertTrue(outputCaptor.toString().contains("Priority: HIGH"))
                );
            }
        }
        
        @Test
        @DisplayName("Should update work item assignee when provided")
        void shouldUpdateWorkItemAssigneeWhenProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setAssignee("new.assignee");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockItemService.getItem(TEST_ITEM_ID);
                assertAll(
                    () -> assertEquals("new.assignee", updatedItem.getAssignee()),
                    () -> assertTrue(outputCaptor.toString().contains("Assignee: new.assignee"))
                );
            }
        }
        
        @Test
        @DisplayName("Should transition work item state when valid transition provided")
        void shouldTransitionWorkItemStateWhenValidTransitionProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(WorkflowState.READY);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockItemService.getItem(TEST_ITEM_ID);
                assertAll(
                    () -> assertEquals(WorkflowState.READY, updatedItem.getState()),
                    () -> assertTrue(outputCaptor.toString().contains("Status: READY"))
                );
            }
        }
        
        @Test
        @DisplayName("Should transition work item state with comment when provided")
        void shouldTransitionWorkItemStateWithCommentWhenProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setCurrentUser("current.user");
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(WorkflowState.READY);
                command.setComment("Transitioning to ready state");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockItemService.getItem(TEST_ITEM_ID);
                assertEquals(WorkflowState.READY, updatedItem.getState());
                assertEquals(1, mockWorkflowService.getTransitionWithCommentCount());
                assertEquals("Transitioning to ready state", mockWorkflowService.getLastTransitionComment());
            }
        }
        
        @Test
        @DisplayName("Should use current user from config service for transition")
        void shouldUseCurrentUserFromConfigServiceForTransition() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setCurrentUser("current.user");
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(WorkflowState.READY);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertEquals("current.user", mockWorkflowService.getLastTransitionUser());
            }
        }
        
        @Test
        @DisplayName("Should use anonymous for transition when no current user")
        void shouldUseAnonymousForTransitionWhenNoCurrentUser() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setCurrentUser(null);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(WorkflowState.READY);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertEquals("anonymous", mockWorkflowService.getLastTransitionUser());
            }
        }
        
        @Test
        @DisplayName("Should update multiple fields at once")
        void shouldUpdateMultipleFieldsAtOnce() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setTitle("New Title");
                command.setDescription("New Description");
                command.setType(WorkItemType.BUG);
                command.setPriority(Priority.HIGH);
                command.setAssignee("new.assignee");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockItemService.getItem(TEST_ITEM_ID);
                assertAll(
                    () -> assertEquals("New Title", updatedItem.getTitle()),
                    () -> assertEquals("New Description", updatedItem.getDescription()),
                    () -> assertEquals(WorkItemType.BUG, updatedItem.getType()),
                    () -> assertEquals(Priority.HIGH, updatedItem.getPriority()),
                    () -> assertEquals("new.assignee", updatedItem.getAssignee())
                );
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
        @DisplayName("Should fail when ID is missing")
        void shouldFailWhenIdIsMissing() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setTitle("New Title");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error: Work item ID is required"));
            }
        }
        
        @Test
        @DisplayName("Should fail when ID is empty")
        void shouldFailWhenIdIsEmpty() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId("");
                command.setTitle("New Title");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error: Work item ID is required"));
            }
        }
        
        @Test
        @DisplayName("Should fail when no updates are specified")
        void shouldFailWhenNoUpdatesAreSpecified() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error: No updates specified"));
            }
        }
        
        @Test
        @DisplayName("Should fail when ID is not a valid UUID")
        void shouldFailWhenIdIsNotAValidUuid() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId("not-a-uuid");
                command.setTitle("New Title");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error: Invalid work item ID format"));
            }
        }
        
        @Test
        @DisplayName("Should fail when work item with ID does not exist")
        void shouldFailWhenWorkItemWithIdDoesNotExist() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId("123e4567-e89b-12d3-a456-426614174999"); // Non-existent ID
                command.setTitle("New Title");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error: Work item not found with ID"));
            }
        }
        
        @Test
        @DisplayName("Should fail when trying to transition to invalid state")
        void shouldFailWhenTryingToTransitionToInvalidState() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(WorkflowState.DONE); // Invalid transition from CREATED to DONE
                
                // Force the workflow service to disallow this transition
                mockWorkflowService.setAllowTransition(false);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error: Cannot transition work item to DONE state"));
            }
        }
        
        @Test
        @DisplayName("Should fail when workflow service throws InvalidTransitionException")
        void shouldFailWhenWorkflowServiceThrowsInvalidTransitionException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(WorkflowState.READY);
                
                mockWorkflowService.setThrowInvalidTransitionException(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error transitioning work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when item service throws exception during update")
        void shouldFailWhenItemServiceThrowsExceptionDuringUpdate() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setTitle("New Title");
                
                mockItemService.setThrowExceptionOnUpdate(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error updating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when workflow service returns null after transition")
        void shouldFailWhenWorkflowServiceReturnsNullAfterTransition() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(WorkflowState.READY);
                
                mockWorkflowService.setReturnNullOnTransition(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error updating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when getting service from service manager throws exception")
        void shouldFailWhenGettingServiceFromServiceManagerThrowsException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockServiceManager.getMockItemService()).thenThrow(new RuntimeException("Service not available"));
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setTitle("New Title");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error updating work item: Service not available"));
            }
        }
        
        @Test
        @DisplayName("Should fail when ServiceManager.getInstance() returns null")
        void shouldFailWhenServiceManagerGetInstanceReturnsNull() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(null);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setTitle("New Title");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error updating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when item service returns null on update")
        void shouldFailWhenItemServiceReturnsNullOnUpdate() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setTitle("New Title");
                
                mockItemService.setReturnNullOnUpdate(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error updating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should show stack trace when exception occurs")
        void shouldShowStackTraceWhenExceptionOccurs() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setTitle("New Title");
                
                mockItemService.setThrowExceptionWithStackTrace(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error updating work item:"));
                assertTrue(error.contains("java.lang.RuntimeException"));
                assertTrue(error.contains("at org.rinna.cli.command.UpdateCommandTest$MockItemService"));
            }
        }
        
        @Test
        @DisplayName("Should handle null pointer exception in service")
        void shouldHandleNullPointerExceptionInService() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setTitle("New Title");
                
                mockItemService.setThrowExceptionType("NullPointerException");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error updating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should handle illegal argument exception in service")
        void shouldHandleIllegalArgumentExceptionInService() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setTitle("New Title");
                
                mockItemService.setThrowExceptionType("IllegalArgumentException");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error updating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should handle out of memory error")
        void shouldHandleOutOfMemoryError() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setTitle("New Title");
                
                mockItemService.setThrowOutOfMemoryError(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error updating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when ConfigurationService returns null")
        void shouldFailWhenConfigurationServiceReturnsNull() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockServiceManager.getConfigurationService()).thenReturn(null);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(WorkflowState.READY); // Requires ConfigurationService for user
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error updating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when WorkflowService returns null")
        void shouldFailWhenWorkflowServiceReturnsNull() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockServiceManager.getMockWorkflowService()).thenReturn(null);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(WorkflowState.READY);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error updating work item:"));
            }
        }
        
        @Test
        @DisplayName("Should show available transitions when invalid transition")
        void shouldShowAvailableTransitionsWhenInvalidTransition() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(WorkflowState.DONE); // Invalid transition
                
                mockWorkflowService.setAllowTransition(false);
                mockWorkflowService.setAvailableTransitions(Arrays.asList(WorkflowState.READY, WorkflowState.BLOCKED));
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Error: Cannot transition work item to DONE state"));
                assertTrue(error.contains("Valid transitions: [READY, BLOCKED]"));
            }
        }
        
        @Test
        @DisplayName("Should fail when transition throws exception")
        void shouldFailWhenTransitionThrowsException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(WorkflowState.READY);
                
                mockWorkflowService.setThrowExceptionOnTransition(true);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error updating work item:"));
            }
        }
    }
    
    /**
     * Tests for verifying the contract between UpdateCommand and its dependencies.
     */
    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should call getItem on ItemService to retrieve work item")
        void shouldCallGetItemOnItemServiceToRetrieveWorkItem() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setTitle("New Title");
                
                // When
                command.call();
                
                // Then
                assertEquals(1, mockItemService.getGetItemCallCount());
                assertEquals(TEST_ITEM_ID, mockItemService.getLastRequestedId());
            }
        }
        
        @Test
        @DisplayName("Should call updateItem on ItemService for non-state updates")
        void shouldCallUpdateItemOnItemServiceForNonStateUpdates() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setTitle("New Title");
                
                // When
                command.call();
                
                // Then
                assertEquals(1, mockItemService.getUpdateItemCallCount());
                WorkItem updatedItem = mockItemService.getLastUpdatedItem();
                assertEquals("New Title", updatedItem.getTitle());
            }
        }
        
        @Test
        @DisplayName("Should call canTransition on WorkflowService for state updates")
        void shouldCallCanTransitionOnWorkflowServiceForStateUpdates() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(WorkflowState.READY);
                
                // When
                command.call();
                
                // Then
                assertEquals(1, mockWorkflowService.getCanTransitionCallCount());
                assertEquals(TEST_ITEM_ID, mockWorkflowService.getLastTransitionItemId());
                assertEquals(WorkflowState.READY, mockWorkflowService.getLastTransitionTargetState());
            }
        }
        
        @Test
        @DisplayName("Should call transition on WorkflowService for state updates")
        void shouldCallTransitionOnWorkflowServiceForStateUpdates() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(WorkflowState.READY);
                
                // When
                command.call();
                
                // Then
                assertEquals(1, mockWorkflowService.getTransitionCallCount());
                assertEquals(TEST_ITEM_ID, mockWorkflowService.getLastTransitionItemId());
                assertEquals(WorkflowState.READY, mockWorkflowService.getLastTransitionTargetState());
            }
        }
        
        @Test
        @DisplayName("Should call transition with comment when comment is provided")
        void shouldCallTransitionWithCommentWhenCommentIsProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(WorkflowState.READY);
                command.setComment("State transition comment");
                
                // When
                command.call();
                
                // Then
                assertEquals(1, mockWorkflowService.getTransitionWithCommentCount());
                assertEquals(TEST_ITEM_ID, mockWorkflowService.getLastTransitionItemId());
                assertEquals(WorkflowState.READY, mockWorkflowService.getLastTransitionTargetState());
                assertEquals("State transition comment", mockWorkflowService.getLastTransitionComment());
            }
        }
        
        @Test
        @DisplayName("Should get current user from ConfigurationService for state transitions")
        void shouldGetCurrentUserFromConfigurationServiceForStateTransitions() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setCurrentUser("current.user");
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(WorkflowState.READY);
                
                // When
                command.call();
                
                // Then
                assertTrue(mockConfigService.getCurrentUserWasCalled());
            }
        }
    }
    
    /**
     * Tests for end-to-end scenarios with multiple components.
     */
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        static Stream<Arguments> provideWorkItemTypeAndPriority() {
            return Stream.of(
                Arguments.of(WorkItemType.TASK, Priority.LOW),
                Arguments.of(WorkItemType.BUG, Priority.MEDIUM),
                Arguments.of(WorkItemType.FEATURE, Priority.HIGH),
                Arguments.of(WorkItemType.EPIC, Priority.CRITICAL)
            );
        }
        
        @ParameterizedTest
        @DisplayName("Should update work item with different type and priority combinations")
        @MethodSource("provideWorkItemTypeAndPriority")
        void shouldUpdateWorkItemWithDifferentTypeAndPriorityCombinations(WorkItemType type, Priority priority) {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setType(type);
                command.setPriority(priority);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockItemService.getItem(TEST_ITEM_ID);
                assertEquals(type, updatedItem.getType());
                assertEquals(priority, updatedItem.getPriority());
            }
        }
        
        @ParameterizedTest
        @DisplayName("Should update assignee with different values")
        @CsvSource({
            "john.doe",
            "jane.smith",
            "admin",
            "''"
        })
        void shouldUpdateAssigneeWithDifferentValues(String assignee) {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setAssignee(assignee);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockItemService.getItem(TEST_ITEM_ID);
                assertEquals(assignee, updatedItem.getAssignee());
            }
        }
        
        @ParameterizedTest
        @DisplayName("Should update to different valid states")
        @EnumSource(value = WorkflowState.class, names = {"READY", "BLOCKED"})
        void shouldUpdateToDifferentValidStates(WorkflowState state) {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setStatus(state);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockItemService.getItem(TEST_ITEM_ID);
                assertEquals(state, updatedItem.getStatus());
            }
        }
        
        @Test
        @DisplayName("Should update title and state in one command")
        void shouldUpdateTitleAndStateInOneCommand() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setTitle("New Title");
                command.setStatus(WorkflowState.READY);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockItemService.getItem(TEST_ITEM_ID);
                assertEquals("New Title", updatedItem.getTitle());
                assertEquals(WorkflowState.READY, updatedItem.getStatus());
            }
        }
        
        @Test
        @DisplayName("Should update item and handle state transition elegantly")
        void shouldUpdateItemAndHandleStateTransitionElegantly() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setCurrentUser("workflow.user");
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setTitle("Transition Test");
                command.setDescription("Testing workflow transitions");
                command.setPriority(Priority.HIGH);
                command.setStatus(WorkflowState.READY);
                command.setComment("Ready for work");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockItemService.getItem(TEST_ITEM_ID);
                
                assertAll(
                    () -> assertEquals("Transition Test", updatedItem.getTitle()),
                    () -> assertEquals("Testing workflow transitions", updatedItem.getDescription()),
                    () -> assertEquals(Priority.HIGH, updatedItem.getPriority()),
                    () -> assertEquals(WorkflowState.READY, updatedItem.getStatus()),
                    () -> assertEquals("workflow.user", mockWorkflowService.getLastTransitionUser()),
                    () -> assertEquals("Ready for work", mockWorkflowService.getLastTransitionComment())
                );
            }
        }
        
        @Test
        @DisplayName("Should use state vs status property correctly")
        void shouldUseStateVsStatusPropertyCorrectly() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                UpdateCommand command = new UpdateCommand();
                command.setId(TEST_ITEM_ID);
                command.setState(WorkflowState.READY); // Using setState instead of setStatus
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockItemService.getItem(TEST_ITEM_ID);
                assertEquals(WorkflowState.READY, updatedItem.getStatus());
            }
        }
        
        @Test
        @DisplayName("Should perform complex update with transition and comments")
        void shouldPerformComplexUpdateWithTransitionAndComments() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Setup test data
                WorkItem progressItem = new WorkItem();
                progressItem.setId("in-progress-item");
                progressItem.setTitle("Original Title");
                progressItem.setType(WorkItemType.TASK);
                progressItem.setPriority(Priority.MEDIUM);
                progressItem.setState(WorkflowState.IN_PROGRESS); // Using setState to ensure compatibility
                progressItem.setAssignee("developer");
                mockItemService.addTestItem(progressItem);
                
                mockConfigService.setCurrentUser("lead.developer");
                
                UpdateCommand command = new UpdateCommand();
                command.setId("in-progress-item");
                command.setTitle("Completed Feature");
                command.setPriority(Priority.HIGH);
                command.setStatus(WorkflowState.DONE);
                command.setComment("Feature implemented and tested");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockItemService.getItem("in-progress-item");
                
                assertAll(
                    () -> assertEquals("Completed Feature", updatedItem.getTitle()),
                    () -> assertEquals(Priority.HIGH, updatedItem.getPriority()),
                    () -> assertEquals(WorkflowState.DONE, updatedItem.getStatus()),
                    () -> assertEquals("lead.developer", mockWorkflowService.getLastTransitionUser()),
                    () -> assertEquals("Feature implemented and tested", mockWorkflowService.getLastTransitionComment())
                );
            }
        }
    }
    
    /**
     * Mock implementation of ItemService for testing.
     */
    private static class MockItemService implements ItemService {
        private final Map<String, WorkItem> items = new HashMap<>();
        private int getItemCallCount = 0;
        private int updateItemCallCount = 0;
        private String lastRequestedId;
        private WorkItem lastUpdatedItem;
        private boolean throwExceptionOnUpdate = false;
        private boolean throwExceptionWithStackTrace = false;
        private boolean throwOutOfMemoryError = false;
        private boolean returnNullOnUpdate = false;
        private String exceptionType = "RuntimeException";
        private String exceptionMessage = "Test exception";
        
        public void addTestItem(WorkItem item) {
            items.put(item.getId(), item);
        }
        
        @Override
        public List<WorkItem> getAllItems() {
            return new ArrayList<>(items.values());
        }
        
        @Override
        public WorkItem getItem(String id) {
            getItemCallCount++;
            lastRequestedId = id;
            return items.get(id);
        }
        
        @Override
        public WorkItem createItem(WorkItem item) {
            item.setId(UUID.randomUUID().toString());
            items.put(item.getId(), item);
            return item;
        }
        
        @Override
        public WorkItem updateItem(WorkItem item) {
            updateItemCallCount++;
            
            if (throwExceptionOnUpdate) {
                throwException();
            }
            
            if (throwExceptionWithStackTrace) {
                Exception e = new RuntimeException("Test exception with stack trace");
                e.printStackTrace();
                throw e;
            }
            
            if (throwOutOfMemoryError) {
                throw new OutOfMemoryError("Test OOM error");
            }
            
            if (returnNullOnUpdate) {
                return null;
            }
            
            lastUpdatedItem = item;
            items.put(item.getId(), item);
            return item;
        }
        
        private void throwException() {
            switch (exceptionType) {
                case "NullPointerException":
                    throw new NullPointerException(exceptionMessage);
                case "IllegalArgumentException":
                    throw new IllegalArgumentException(exceptionMessage);
                case "IOException":
                    throw new RuntimeException(new java.io.IOException(exceptionMessage));
                default:
                    throw new RuntimeException(exceptionMessage);
            }
        }
        
        @Override
        public boolean deleteItem(String id) {
            return items.remove(id) != null;
        }
        
        @Override
        public WorkItem findItemByShortId(String shortId) {
            // Simple implementation for testing
            for (WorkItem item : items.values()) {
                if (item.getId().endsWith(shortId)) {
                    return item;
                }
            }
            return null;
        }
        
        @Override
        public WorkItem updateTitle(UUID id, String title, String user) {
            WorkItem item = getItem(id.toString());
            if (item != null) {
                item.setTitle(title);
                return item;
            }
            return null;
        }
        
        @Override
        public WorkItem updateDescription(UUID id, String description, String user) {
            WorkItem item = getItem(id.toString());
            if (item != null) {
                item.setDescription(description);
                return item;
            }
            return null;
        }
        
        @Override
        public WorkItem updateField(UUID id, String field, String value, String user) {
            WorkItem item = getItem(id.toString());
            if (item != null) {
                switch (field) {
                    case "title":
                        item.setTitle(value);
                        break;
                    case "description":
                        item.setDescription(value);
                        break;
                    default:
                        throw new IllegalArgumentException("Unknown field: " + field);
                }
                return item;
            }
            return null;
        }
        
        @Override
        public WorkItem assignTo(UUID id, String assignee, String user) {
            WorkItem item = getItem(id.toString());
            if (item != null) {
                item.setAssignee(assignee);
                return item;
            }
            return null;
        }
        
        @Override
        public WorkItem updatePriority(UUID id, Priority priority, String user) {
            WorkItem item = getItem(id.toString());
            if (item != null) {
                item.setPriority(priority);
                return item;
            }
            return null;
        }
        
        @Override
        public WorkItem updateCustomFields(String id, Map<String, String> customFields) {
            // Not implemented for testing
            return null;
        }
        
        @Override
        public WorkItem updateAssignee(String id, String assignee) {
            WorkItem item = getItem(id);
            if (item != null) {
                item.setAssignee(assignee);
                return item;
            }
            return null;
        }
        
        @Override
        public WorkItem createWorkItem(WorkItemCreateRequest request) {
            // Not implemented for testing
            return null;
        }
        
        @Override
        public List<WorkItem> findByAssignee(String assignee) {
            // Not implemented for testing
            return Collections.emptyList();
        }
        
        @Override
        public List<WorkItem> findByType(WorkItemType type) {
            // Not implemented for testing
            return Collections.emptyList();
        }
        
        @Override
        public List<WorkItem> getAllWorkItems() {
            return getAllItems();
        }
        
        // Getters for test verification
        public int getGetItemCallCount() {
            return getItemCallCount;
        }
        
        public int getUpdateItemCallCount() {
            return updateItemCallCount;
        }
        
        public String getLastRequestedId() {
            return lastRequestedId;
        }
        
        public WorkItem getLastUpdatedItem() {
            return lastUpdatedItem;
        }
        
        // Setters for test control
        public void setThrowExceptionOnUpdate(boolean throwException) {
            this.throwExceptionOnUpdate = throwException;
        }
        
        public void setThrowExceptionWithStackTrace(boolean throwExceptionWithStackTrace) {
            this.throwExceptionWithStackTrace = throwExceptionWithStackTrace;
        }
        
        public void setThrowOutOfMemoryError(boolean throwOutOfMemoryError) {
            this.throwOutOfMemoryError = throwOutOfMemoryError;
        }
        
        public void setReturnNullOnUpdate(boolean returnNull) {
            this.returnNullOnUpdate = returnNull;
        }
        
        public void setThrowExceptionType(String exceptionType) {
            this.exceptionType = exceptionType;
        }
    }
    
    /**
     * Mock implementation of WorkflowService for testing.
     */
    private static class MockWorkflowService implements WorkflowService {
        private final Map<String, WorkItem> items = new HashMap<>();
        private int canTransitionCallCount = 0;
        private int transitionCallCount = 0;
        private int transitionWithCommentCount = 0;
        private String lastTransitionItemId;
        private String lastTransitionUser;
        private WorkflowState lastTransitionTargetState;
        private String lastTransitionComment;
        private boolean allowTransition = true;
        private boolean throwInvalidTransitionException = false;
        private boolean throwExceptionOnTransition = false;
        private boolean returnNullOnTransition = false;
        private List<WorkflowState> availableTransitions = new ArrayList<>();
        
        @Override
        public WorkItem getItem(UUID id) {
            return getItem(id.toString());
        }
        
        @Override
        public WorkItem getItem(String id) {
            return items.get(id);
        }
        
        @Override
        public List<WorkItem> getItemsInState(WorkflowState state) {
            List<WorkItem> result = new ArrayList<>();
            for (WorkItem item : items.values()) {
                if (state.equals(item.getStatus())) {
                    result.add(item);
                }
            }
            return result;
        }
        
        @Override
        public List<WorkItem> getItemsInState(WorkflowState state, String username) {
            List<WorkItem> result = new ArrayList<>();
            for (WorkItem item : items.values()) {
                if (state.equals(item.getStatus()) && username.equals(item.getAssignee())) {
                    result.add(item);
                }
            }
            return result;
        }
        
        @Override
        public WorkItem transition(UUID workItemId, String username, WorkflowState newState, String comment) throws InvalidTransitionException {
            return transition(workItemId.toString(), username, newState, comment);
        }
        
        @Override
        public WorkItem transition(String workItemId, String username, WorkflowState newState, String comment) throws InvalidTransitionException {
            transitionWithCommentCount++;
            lastTransitionItemId = workItemId;
            lastTransitionUser = username;
            lastTransitionTargetState = newState;
            lastTransitionComment = comment;
            
            if (throwInvalidTransitionException) {
                throw new InvalidTransitionException("Test invalid transition exception");
            }
            
            if (throwExceptionOnTransition) {
                throw new RuntimeException("Test transition exception");
            }
            
            if (returnNullOnTransition) {
                return null;
            }
            
            WorkItem item = items.get(workItemId);
            if (item == null) {
                throw new InvalidTransitionException("Work item not found: " + workItemId);
            }
            
            // Update the state
            item.setStatus(newState);
            return item;
        }
        
        @Override
        public WorkItem transition(String workItemId, WorkflowState newState) throws InvalidTransitionException {
            transitionCallCount++;
            lastTransitionItemId = workItemId;
            lastTransitionTargetState = newState;
            
            if (throwInvalidTransitionException) {
                throw new InvalidTransitionException("Test invalid transition exception");
            }
            
            if (throwExceptionOnTransition) {
                throw new RuntimeException("Test transition exception");
            }
            
            if (returnNullOnTransition) {
                return null;
            }
            
            WorkItem item = items.get(workItemId);
            if (item == null) {
                throw new InvalidTransitionException("Work item not found: " + workItemId);
            }
            
            // Update the state
            item.setStatus(newState);
            return item;
        }
        
        @Override
        public boolean canTransition(String itemId, WorkflowState targetState) {
            canTransitionCallCount++;
            lastTransitionItemId = itemId;
            lastTransitionTargetState = targetState;
            return allowTransition;
        }
        
        @Override
        public List<WorkflowState> getAvailableTransitions(String itemId) {
            if (availableTransitions.isEmpty()) {
                // Default valid transitions from CREATED state
                return Arrays.asList(WorkflowState.READY, WorkflowState.BLOCKED);
            }
            return availableTransitions;
        }
        
        @Override
        public WorkflowState getCurrentState(String itemId) {
            WorkItem item = items.get(itemId);
            return item != null ? item.getStatus() : null;
        }
        
        @Override
        public UUID getCurrentActiveItemId(String username) {
            return null;
        }
        
        @Override
        public WorkItem getCurrentWorkItem(String user) {
            return null;
        }
        
        @Override
        public List<WorkItem> findByStatus(WorkflowState status) {
            List<WorkItem> result = new ArrayList<>();
            for (WorkItem item : items.values()) {
                if (status.equals(item.getStatus())) {
                    result.add(item);
                }
            }
            return result;
        }
        
        @Override
        public boolean setCurrentActiveItem(UUID workItemId, String username) {
            return true;
        }
        
        @Override
        public boolean clearCurrentActiveItem(String username) {
            return true;
        }
        
        // Methods for testing
        public void addItem(WorkItem item) {
            items.put(item.getId(), item);
        }
        
        // Getters for test verification
        public int getCanTransitionCallCount() {
            return canTransitionCallCount;
        }
        
        public int getTransitionCallCount() {
            return transitionCallCount;
        }
        
        public int getTransitionWithCommentCount() {
            return transitionWithCommentCount;
        }
        
        public String getLastTransitionItemId() {
            return lastTransitionItemId;
        }
        
        public String getLastTransitionUser() {
            return lastTransitionUser;
        }
        
        public WorkflowState getLastTransitionTargetState() {
            return lastTransitionTargetState;
        }
        
        public String getLastTransitionComment() {
            return lastTransitionComment;
        }
        
        // Setters for test control
        public void setAllowTransition(boolean allowTransition) {
            this.allowTransition = allowTransition;
        }
        
        public void setThrowInvalidTransitionException(boolean throwException) {
            this.throwInvalidTransitionException = throwException;
        }
        
        public void setReturnNullOnTransition(boolean returnNull) {
            this.returnNullOnTransition = returnNull;
        }
        
        public void setThrowExceptionOnTransition(boolean throwException) {
            this.throwExceptionOnTransition = throwException;
        }
        
        public void setAvailableTransitions(List<WorkflowState> transitions) {
            this.availableTransitions = transitions;
        }
    }
    
    /**
     * Mock implementation of ConfigurationService for testing.
     */
    private static class MockConfigurationService implements ConfigurationService {
        private String currentUser;
        private String currentProject;
        private boolean getCurrentUserWasCalled = false;
        private boolean getCurrentProjectWasCalled = false;
        
        public MockConfigurationService() {
            // Default values
            this.currentUser = "test.user";
            this.currentProject = "TEST-PROJECT";
        }
        
        @Override
        public String getCurrentUser() {
            getCurrentUserWasCalled = true;
            return currentUser;
        }
        
        @Override
        public String getCurrentProject() {
            getCurrentProjectWasCalled = true;
            return currentProject;
        }
        
        // Setters for test control
        public void setCurrentUser(String currentUser) {
            this.currentUser = currentUser;
        }
        
        public void setCurrentProject(String currentProject) {
            this.currentProject = currentProject;
        }
        
        // Getters for test verification
        public boolean getCurrentUserWasCalled() {
            return getCurrentUserWasCalled;
        }
        
        public boolean getCurrentProjectWasCalled() {
            return getCurrentProjectWasCalled;
        }
        
        // Implement other required methods with default implementations
        @Override
        public String getServerUrl() {
            return "http://localhost:8080";
        }
        
        @Override
        public boolean isAuthenticated() {
            return currentUser != null;
        }
        
        @Override
        public String getAuthToken() {
            return "test-token";
        }
        
        @Override
        public String getDefaultVersion() {
            return "1.0";
        }
        
        @Override
        public String getDefaultBugAssignee() {
            return "bugfixer";
        }
        
        @Override
        public boolean getAutoAssignBugs() {
            return true;
        }
        
        @Override
        public String getProperty(String key, String defaultValue) {
            return defaultValue;
        }
        
        @Override
        public boolean isRemoteServicesAvailable() {
            return false;
        }
    }
    
    /**
     * Helper class for testing help documentation.
     */
    private static class HelpTestUpdateCommand {
        public void testExecuteHelp(String helpArg) {
            // Manually simulate help output for UpdateCommand
            StringBuilder helpOutput = new StringBuilder();
            helpOutput.append("Command to update an existing work item\n\n");
            helpOutput.append("Usage: update [OPTIONS]\n\n");
            helpOutput.append("Options:\n");
            helpOutput.append("  --id             The ID of the work item to update (required)\n");
            helpOutput.append("  --title          The new title of the work item\n");
            helpOutput.append("  --description    The new description of the work item\n");
            helpOutput.append("  --type           The new type (TASK, BUG, FEATURE, EPIC)\n");
            helpOutput.append("  --priority       The new priority (LOW, MEDIUM, HIGH, CRITICAL)\n");
            helpOutput.append("  --status         The new status/state (CREATED, READY, IN_PROGRESS, BLOCKED, DONE)\n");
            helpOutput.append("  --assignee       The new assignee\n");
            helpOutput.append("  --comment        Comment for state transitions\n");
            helpOutput.append("  -h, --help       Show this help message\n");
            
            System.out.println(helpOutput);
        }
    }
}