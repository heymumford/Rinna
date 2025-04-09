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
import java.time.format.DateTimeFormatter;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive test suite for DoneCommand.
 *
 * This test class follows TDD best practices with these categories:
 * 1. Help Documentation Tests - Testing the help/usage output
 * 2. Positive Test Cases - Testing normal successful operations
 * 3. Negative Test Cases - Testing error handling for invalid inputs
 * 4. Contract Tests - Testing the contract between this class and its dependencies
 * 5. Integration Tests - Testing end-to-end scenarios
 */
@DisplayName("DoneCommand Tests")
class DoneCommandTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private ServiceManager mockServiceManager;
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
        mockWorkflowService = new MockWorkflowService();
        mockConfigService = new MockConfigurationService();
        
        // Create test work item
        WorkItem testItem = new WorkItem();
        testItem.setId(TEST_ITEM_ID);
        testItem.setTitle("Test Work Item");
        testItem.setDescription("Test Description");
        testItem.setType(WorkItemType.TASK);
        testItem.setPriority(Priority.MEDIUM);
        testItem.setStatus(WorkflowState.IN_PROGRESS); // Item in progress can be marked as done
        testItem.setAssignee("test.user");
        testItem.setCreated(LocalDateTime.now().minusDays(1));
        testItem.setUpdated(LocalDateTime.now().minusHours(1));
        mockWorkflowService.addItem(testItem);
        
        // Set up mock service manager
        when(mockServiceManager.getWorkflowService()).thenReturn(mockWorkflowService);
        ConfigurationService configService = mock(ConfigurationService.class);
        when(configService.getCurrentUser()).thenReturn(mockConfigService.getCurrentUser());
        when(mockServiceManager.getConfigurationService()).thenReturn(configService);
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
        
        private HelpTestDoneCommand helpCommand;
        
        @BeforeEach
        void setUp() {
            helpCommand = new HelpTestDoneCommand();
        }
        
        @Test
        @DisplayName("Should display help when -h flag is used")
        void shouldDisplayHelpWithHFlag() {
            // Execute
            helpCommand.testExecuteHelp("-h");
            
            // Verify
            String output = outputCaptor.toString();
            assertAll(
                () -> assertTrue(output.contains("Command to mark a work item as done")),
                () -> assertTrue(output.contains("--id")),
                () -> assertTrue(output.contains("--comment"))
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
                () -> assertTrue(output.contains("Command to mark a work item as done")),
                () -> assertTrue(output.contains("--id")),
                () -> assertTrue(output.contains("--comment"))
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
            assertTrue(output.contains("--comment")); // Comment is optional
        }
    }
    
    /**
     * Tests for successful scenarios.
     */
    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("Should mark work item as done")
        void shouldMarkWorkItemAsDone() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockWorkflowService.getItem(TEST_ITEM_ID);
                assertAll(
                    () -> assertEquals(WorkflowState.DONE, updatedItem.getStatus()),
                    () -> assertTrue(outputCaptor.toString().contains("Work item " + TEST_ITEM_ID + " marked as DONE")),
                    () -> assertTrue(outputCaptor.toString().contains("Updated state: DONE"))
                );
            }
        }
        
        @Test
        @DisplayName("Should mark work item as done with comment")
        void shouldMarkWorkItemAsDoneWithComment() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setCurrentUser("current.user");
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                command.setComment("Task completed successfully");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockWorkflowService.getItem(TEST_ITEM_ID);
                assertAll(
                    () -> assertEquals(WorkflowState.DONE, updatedItem.getStatus()),
                    () -> assertEquals(1, mockWorkflowService.getTransitionWithCommentCount()),
                    () -> assertEquals("Task completed successfully", mockWorkflowService.getLastTransitionComment()),
                    () -> assertEquals("current.user", mockWorkflowService.getLastTransitionUser())
                );
            }
        }
        
        @Test
        @DisplayName("Should use current user from config service for transition with comment")
        void shouldUseCurrentUserFromConfigServiceForTransitionWithComment() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setCurrentUser("task.completer");
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                command.setComment("Task done");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertEquals("task.completer", mockWorkflowService.getLastTransitionUser());
            }
        }
        
        @Test
        @DisplayName("Should use anonymous for transition when no current user")
        void shouldUseAnonymousForTransitionWhenNoCurrentUser() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setCurrentUser(null);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                command.setComment("Anonymous completion");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertEquals("anonymous", mockWorkflowService.getLastTransitionUser());
            }
        }
        
        @Test
        @DisplayName("Should output work item title")
        void shouldOutputWorkItemTitle() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertTrue(outputCaptor.toString().contains("Title: Test Work Item"));
            }
        }
        
        @Test
        @DisplayName("Should output formatted completion date")
        void shouldOutputFormattedCompletionDate() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set a specific update time for testing
                LocalDateTime completionTime = LocalDateTime.now();
                mockWorkflowService.setUpdateTimeForNextTransition(completionTime);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Format the expected completion date
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String expectedDateString = completionTime.format(formatter);
                
                assertTrue(outputCaptor.toString().contains("Completion date: " + expectedDateString));
            }
        }
        
        @Test
        @DisplayName("Should output current time when updated time is null")
        void shouldOutputCurrentTimeWhenUpdatedTimeIsNull() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Set null update time
                mockWorkflowService.setUpdateTimeForNextTransition(null);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertTrue(outputCaptor.toString().contains("Completion date: "));
            }
        }
        
        @Test
        @DisplayName("Should successfully handle work item in READY state")
        void shouldSuccessfullyHandleWorkItemInReadyState() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create a work item in READY state
                WorkItem readyItem = new WorkItem();
                readyItem.setId("ready-item-id");
                readyItem.setTitle("Ready Item");
                readyItem.setStatus(WorkflowState.READY);
                mockWorkflowService.addItem(readyItem);
                
                DoneCommand command = new DoneCommand();
                command.setItemId("ready-item-id");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockWorkflowService.getItem("ready-item-id");
                assertEquals(WorkflowState.DONE, updatedItem.getStatus());
            }
        }
        
        @Test
        @DisplayName("Should handle transition with empty comment")
        void shouldHandleTransitionWithEmptyComment() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                command.setComment("");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertEquals(1, mockWorkflowService.getTransitionCallCount());
                assertEquals(0, mockWorkflowService.getTransitionWithCommentCount());
            }
        }
        
        @Test
        @DisplayName("Should use simple transition when no comment provided")
        void shouldUseSimpleTransitionWhenNoCommentProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                // No comment
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                assertEquals(1, mockWorkflowService.getTransitionCallCount());
                assertEquals(0, mockWorkflowService.getTransitionWithCommentCount());
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
        @DisplayName("Should fail when item ID is missing")
        void shouldFailWhenItemIdIsMissing() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                DoneCommand command = new DoneCommand();
                // No item ID set
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error: Work item ID is required"));
            }
        }
        
        @Test
        @DisplayName("Should fail when item ID is empty")
        void shouldFailWhenItemIdIsEmpty() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                DoneCommand command = new DoneCommand();
                command.setItemId("");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error: Work item ID is required"));
            }
        }
        
        @Test
        @DisplayName("Should fail when item ID is not a valid UUID")
        void shouldFailWhenItemIdIsNotAValidUuid() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                DoneCommand command = new DoneCommand();
                command.setItemId("not-a-uuid");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error: Invalid work item ID format"));
            }
        }
        
        @Test
        @DisplayName("Should fail when item cannot be transitioned to DONE state")
        void shouldFailWhenItemCannotBeTransitionedToDoneState() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Force canTransition to return false
                mockWorkflowService.setAllowTransition(false);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error: Cannot transition work item to DONE state"));
            }
        }
        
        @Test
        @DisplayName("Should show available transitions when transition is not allowed")
        void shouldShowAvailableTransitionsWhenTransitionIsNotAllowed() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockWorkflowService.setAllowTransition(false);
                mockWorkflowService.setAvailableTransitions(Arrays.asList(WorkflowState.READY, WorkflowState.BLOCKED));
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                String error = errorCaptor.toString();
                assertTrue(error.contains("Valid transitions: [READY, BLOCKED]"));
            }
        }
        
        @Test
        @DisplayName("Should fail when work item does not exist")
        void shouldFailWhenWorkItemDoesNotExist() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                DoneCommand command = new DoneCommand();
                command.setItemId("123e4567-e89b-12d3-a456-426614174999"); // Non-existent ID
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                // canTransition will check if the item exists and return false if not
                assertTrue(errorCaptor.toString().contains("Error: Cannot transition work item to DONE state"));
            }
        }
        
        @Test
        @DisplayName("Should fail when workflow service throws InvalidTransitionException")
        void shouldFailWhenWorkflowServiceThrowsInvalidTransitionException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockWorkflowService.setThrowInvalidTransitionException(true);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error: Test invalid transition exception"));
            }
        }
        
        @Test
        @DisplayName("Should fail when workflow service throws runtime exception")
        void shouldFailWhenWorkflowServiceThrowsRuntimeException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockWorkflowService.setThrowExceptionOnTransition(true);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error transitioning work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when getting service manager returns null")
        void shouldFailWhenGettingServiceManagerReturnsNull() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(null);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error transitioning work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when workflow service is null")
        void shouldFailWhenWorkflowServiceIsNull() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockServiceManager.getMockWorkflowService()).thenReturn(null);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error transitioning work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when configuration service is null")
        void shouldFailWhenConfigurationServiceIsNull() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockServiceManager.getConfigurationService()).thenReturn(null);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                command.setComment("Test comment"); // Comment requires config service for user
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error transitioning work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when workflow service returns null after transition")
        void shouldFailWhenWorkflowServiceReturnsNullAfterTransition() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockWorkflowService.setReturnNullOnTransition(true);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error transitioning work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when canTransition throws exception")
        void shouldFailWhenCanTransitionThrowsException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockWorkflowService.setThrowExceptionOnCanTransition(true);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error transitioning work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when getAvailableTransitions throws exception")
        void shouldFailWhenGetAvailableTransitionsThrowsException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockWorkflowService.setAllowTransition(false);
                mockWorkflowService.setThrowExceptionOnGetAvailableTransitions(true);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error transitioning work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when getCurrentUser throws exception")
        void shouldFailWhenGetCurrentUserThrowsException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setThrowExceptionOnGetCurrentUser(true);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                command.setComment("Test comment"); // Comment requires current user
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error transitioning work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail when getting mock workflow service throws exception")
        void shouldFailWhenGettingMockWorkflowServiceThrowsException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockServiceManager.getMockWorkflowService()).thenThrow(new RuntimeException("Service not available"));
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error transitioning work item: Service not available"));
            }
        }
        
        @Test
        @DisplayName("Should fail when transition called on item that can't be transitioned")
        void shouldFailWhenTransitionCalledOnItemThatCantBeTransitioned() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create a test item in a state that can't be transitioned to DONE
                WorkItem blockedItem = new WorkItem();
                blockedItem.setId("blocked-item-id");
                blockedItem.setTitle("Blocked Item");
                blockedItem.setStatus(WorkflowState.BLOCKED);
                mockWorkflowService.addItem(blockedItem);
                
                // Force canTransition to return false for this item
                mockWorkflowService.addForbiddenTransitionItem("blocked-item-id");
                
                DoneCommand command = new DoneCommand();
                command.setItemId("blocked-item-id");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error: Cannot transition work item to DONE state"));
            }
        }
        
        @Test
        @DisplayName("Should fail when transition returns null item")
        void shouldFailWhenTransitionReturnsNullItem() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockWorkflowService.setReturnNullOnTransition(true);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error transitioning work item:"));
            }
        }
        
        @Test
        @DisplayName("Should fail gracefully when transition throws unknown exception")
        void shouldFailGracefullyWhenTransitionThrowsUnknownException() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockWorkflowService.setThrowUnknownExceptionOnTransition(true);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(1, exitCode);
                assertTrue(errorCaptor.toString().contains("Error transitioning work item:"));
            }
        }
    }
    
    /**
     * Tests for verifying the contract between DoneCommand and its dependencies.
     */
    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should call canTransition on workflow service")
        void shouldCallCanTransitionOnWorkflowService() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                assertEquals(1, mockWorkflowService.getCanTransitionCallCount());
                assertEquals(TEST_ITEM_ID, mockWorkflowService.getLastTransitionItemId());
                assertEquals(WorkflowState.DONE, mockWorkflowService.getLastTransitionTargetState());
            }
        }
        
        @Test
        @DisplayName("Should call transition on workflow service")
        void shouldCallTransitionOnWorkflowService() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                assertEquals(1, mockWorkflowService.getTransitionCallCount());
                assertEquals(TEST_ITEM_ID, mockWorkflowService.getLastTransitionItemId());
                assertEquals(WorkflowState.DONE, mockWorkflowService.getLastTransitionTargetState());
            }
        }
        
        @Test
        @DisplayName("Should call transition with comment when comment provided")
        void shouldCallTransitionWithCommentWhenCommentProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setCurrentUser("test.user");
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                command.setComment("Completion comment");
                
                // When
                command.call();
                
                // Then
                assertEquals(1, mockWorkflowService.getTransitionWithCommentCount());
                assertEquals(TEST_ITEM_ID, mockWorkflowService.getLastTransitionItemId());
                assertEquals(WorkflowState.DONE, mockWorkflowService.getLastTransitionTargetState());
                assertEquals("Completion comment", mockWorkflowService.getLastTransitionComment());
                assertEquals("test.user", mockWorkflowService.getLastTransitionUser());
            }
        }
        
        @Test
        @DisplayName("Should call getAvailableTransitions when transition not allowed")
        void shouldCallGetAvailableTransitionsWhenTransitionNotAllowed() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockWorkflowService.setAllowTransition(false);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                
                // When
                command.call();
                
                // Then
                assertEquals(1, mockWorkflowService.getGetAvailableTransitionsCallCount());
                assertEquals(TEST_ITEM_ID, mockWorkflowService.getLastAvailableTransitionsItemId());
            }
        }
        
        @Test
        @DisplayName("Should get current user from configuration service when comment provided")
        void shouldGetCurrentUserFromConfigurationServiceWhenCommentProvided() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                command.setComment("Test comment");
                
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
        
        @Test
        @DisplayName("Should mark in-progress item as done with comment and output details")
        void shouldMarkInProgressItemAsDoneWithCommentAndOutputDetails() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                mockConfigService.setCurrentUser("developer.user");
                LocalDateTime completionTime = LocalDateTime.now();
                mockWorkflowService.setUpdateTimeForNextTransition(completionTime);
                
                DoneCommand command = new DoneCommand();
                command.setItemId(TEST_ITEM_ID);
                command.setComment("Feature implementation completed");
                
                // When
                int exitCode = command.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem updatedItem = mockWorkflowService.getItem(TEST_ITEM_ID);
                
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                String expectedDateString = completionTime.format(formatter);
                
                assertAll(
                    () -> assertEquals(WorkflowState.DONE, updatedItem.getStatus()),
                    () -> assertEquals("developer.user", mockWorkflowService.getLastTransitionUser()),
                    () -> assertEquals("Feature implementation completed", mockWorkflowService.getLastTransitionComment()),
                    () -> assertTrue(outputCaptor.toString().contains("Work item " + TEST_ITEM_ID + " marked as DONE")),
                    () -> assertTrue(outputCaptor.toString().contains("Title: Test Work Item")),
                    () -> assertTrue(outputCaptor.toString().contains("Updated state: DONE")),
                    () -> assertTrue(outputCaptor.toString().contains("Completion date: " + expectedDateString))
                );
            }
        }
        
        @Test
        @DisplayName("Should transition different items from different states to DONE")
        void shouldTransitionDifferentItemsFromDifferentStatesToDone() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create items in different states
                WorkItem readyItem = new WorkItem();
                readyItem.setId("ready-item-id");
                readyItem.setTitle("Ready Item");
                readyItem.setStatus(WorkflowState.READY);
                mockWorkflowService.addItem(readyItem);
                
                WorkItem inProgressItem = new WorkItem();
                inProgressItem.setId("in-progress-item-id");
                inProgressItem.setTitle("In Progress Item");
                inProgressItem.setStatus(WorkflowState.IN_PROGRESS);
                mockWorkflowService.addItem(inProgressItem);
                
                // Process ready item
                DoneCommand readyCommand = new DoneCommand();
                readyCommand.setItemId("ready-item-id");
                int readyExitCode = readyCommand.call();
                
                // Reset output for next command
                outputCaptor.reset();
                
                // Process in-progress item
                DoneCommand inProgressCommand = new DoneCommand();
                inProgressCommand.setItemId("in-progress-item-id");
                int inProgressExitCode = inProgressCommand.call();
                
                // Then
                assertAll(
                    () -> assertEquals(0, readyExitCode),
                    () -> assertEquals(0, inProgressExitCode),
                    () -> assertEquals(WorkflowState.DONE, mockWorkflowService.getItem("ready-item-id").getStatus()),
                    () -> assertEquals(WorkflowState.DONE, mockWorkflowService.getItem("in-progress-item-id").getStatus())
                );
            }
        }
        
        @Test
        @DisplayName("Should handle complete workflow with multiple transitions")
        void shouldHandleCompleteWorkflowWithMultipleTransitions() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // Create a new item in CREATED state
                WorkItem newItem = new WorkItem();
                newItem.setId("workflow-item-id");
                newItem.setTitle("Workflow Item");
                newItem.setStatus(WorkflowState.CREATED);
                mockWorkflowService.addItem(newItem);
                
                // First, transition to READY
                mockWorkflowService.transition("workflow-item-id", WorkflowState.READY);
                
                // Then to IN_PROGRESS
                mockWorkflowService.transition("workflow-item-id", WorkflowState.IN_PROGRESS);
                
                // Finally, use DoneCommand to transition to DONE
                DoneCommand doneCommand = new DoneCommand();
                doneCommand.setItemId("workflow-item-id");
                doneCommand.setComment("Completed full workflow");
                
                // When
                int exitCode = doneCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                WorkItem finalItem = mockWorkflowService.getItem("workflow-item-id");
                assertEquals(WorkflowState.DONE, finalItem.getStatus());
            }
        }
        
        @Test
        @DisplayName("Should handle multiple commands in sequence")
        void shouldHandleMultipleCommandsInSequence() {
            // Given
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                // First command - will succeed
                DoneCommand command1 = new DoneCommand();
                command1.setItemId(TEST_ITEM_ID);
                
                // Second command - should fail as item is already in DONE state
                DoneCommand command2 = new DoneCommand();
                command2.setItemId(TEST_ITEM_ID);
                
                // When
                int exitCode1 = command1.call();
                
                // Reset outputs for second command
                outputCaptor.reset();
                errorCaptor.reset();
                
                // Force canTransition to return false as item is already in DONE state
                mockWorkflowService.setAllowTransition(false);
                
                int exitCode2 = command2.call();
                
                // Then
                assertAll(
                    () -> assertEquals(0, exitCode1),
                    () -> assertEquals(1, exitCode2),
                    () -> assertEquals(WorkflowState.DONE, mockWorkflowService.getItem(TEST_ITEM_ID).getStatus()),
                    () -> assertTrue(errorCaptor.toString().contains("Error: Cannot transition work item to DONE state"))
                );
            }
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
        private int getAvailableTransitionsCallCount = 0;
        private String lastTransitionItemId;
        private String lastTransitionUser;
        private WorkflowState lastTransitionTargetState;
        private String lastTransitionComment;
        private String lastAvailableTransitionsItemId;
        private boolean allowTransition = true;
        private boolean throwInvalidTransitionException = false;
        private boolean throwExceptionOnTransition = false;
        private boolean throwUnknownExceptionOnTransition = false;
        private boolean returnNullOnTransition = false;
        private boolean throwExceptionOnCanTransition = false;
        private boolean throwExceptionOnGetAvailableTransitions = false;
        private List<WorkflowState> availableTransitions = new ArrayList<>();
        private LocalDateTime updateTimeForNextTransition;
        private final Set<String> forbiddenTransitionItems = new HashSet<>();
        
        public void addItem(WorkItem item) {
            items.put(item.getId(), item);
        }
        
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
            
            if (throwUnknownExceptionOnTransition) {
                throw new Error("Unknown error during transition");
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
            
            // Update the timestamp if specified
            if (updateTimeForNextTransition != null) {
                item.setUpdated(updateTimeForNextTransition);
                // Reset after use
                updateTimeForNextTransition = null;
            } else {
                item.setUpdated(LocalDateTime.now());
            }
            
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
            
            if (throwUnknownExceptionOnTransition) {
                throw new Error("Unknown error during transition");
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
            
            // Update the timestamp if specified
            if (updateTimeForNextTransition != null) {
                item.setUpdated(updateTimeForNextTransition);
                // Reset after use
                updateTimeForNextTransition = null;
            } else {
                item.setUpdated(LocalDateTime.now());
            }
            
            return item;
        }
        
        @Override
        public boolean canTransition(String itemId, WorkflowState targetState) {
            canTransitionCallCount++;
            lastTransitionItemId = itemId;
            lastTransitionTargetState = targetState;
            
            if (throwExceptionOnCanTransition) {
                throw new RuntimeException("Test canTransition exception");
            }
            
            // Check if this is a forbidden transition item
            if (forbiddenTransitionItems.contains(itemId)) {
                return false;
            }
            
            // Check if the item exists
            WorkItem item = items.get(itemId);
            if (item == null) {
                return false;
            }
            
            return allowTransition;
        }
        
        @Override
        public List<WorkflowState> getAvailableTransitions(String itemId) {
            getAvailableTransitionsCallCount++;
            lastAvailableTransitionsItemId = itemId;
            
            if (throwExceptionOnGetAvailableTransitions) {
                throw new RuntimeException("Test getAvailableTransitions exception");
            }
            
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
        
        // Methods for test control
        public void setAllowTransition(boolean allowTransition) {
            this.allowTransition = allowTransition;
        }
        
        public void setThrowInvalidTransitionException(boolean throwException) {
            this.throwInvalidTransitionException = throwException;
        }
        
        public void setThrowExceptionOnTransition(boolean throwException) {
            this.throwExceptionOnTransition = throwException;
        }
        
        public void setThrowUnknownExceptionOnTransition(boolean throwException) {
            this.throwUnknownExceptionOnTransition = throwException;
        }
        
        public void setReturnNullOnTransition(boolean returnNull) {
            this.returnNullOnTransition = returnNull;
        }
        
        public void setThrowExceptionOnCanTransition(boolean throwException) {
            this.throwExceptionOnCanTransition = throwException;
        }
        
        public void setThrowExceptionOnGetAvailableTransitions(boolean throwException) {
            this.throwExceptionOnGetAvailableTransitions = throwException;
        }
        
        public void setAvailableTransitions(List<WorkflowState> transitions) {
            this.availableTransitions = transitions;
        }
        
        public void setUpdateTimeForNextTransition(LocalDateTime updateTime) {
            this.updateTimeForNextTransition = updateTime;
        }
        
        public void addForbiddenTransitionItem(String itemId) {
            this.forbiddenTransitionItems.add(itemId);
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
        
        public int getGetAvailableTransitionsCallCount() {
            return getAvailableTransitionsCallCount;
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
        
        public String getLastAvailableTransitionsItemId() {
            return lastAvailableTransitionsItemId;
        }
    }
    
    /**
     * Mock implementation of ConfigurationService for testing.
     */
    private static class MockConfigurationService {
        private String currentUser;
        private boolean getCurrentUserWasCalled = false;
        private boolean throwExceptionOnGetCurrentUser = false;
        
        public MockConfigurationService() {
            this.currentUser = "test.user";
        }
        
        public String getCurrentUser() {
            getCurrentUserWasCalled = true;
            
            if (throwExceptionOnGetCurrentUser) {
                throw new RuntimeException("Test getCurrentUser exception");
            }
            
            return currentUser;
        }
        
        // Setters for test control
        public void setCurrentUser(String currentUser) {
            this.currentUser = currentUser;
        }
        
        public void setThrowExceptionOnGetCurrentUser(boolean throwException) {
            this.throwExceptionOnGetCurrentUser = throwException;
        }
        
        // Getters for test verification
        public boolean getCurrentUserWasCalled() {
            return getCurrentUserWasCalled;
        }
    }
    
    /**
     * Helper class for testing help documentation.
     */
    private static class HelpTestDoneCommand {
        public void testExecuteHelp(String helpArg) {
            // Manually simulate help output for DoneCommand
            StringBuilder helpOutput = new StringBuilder();
            helpOutput.append("Command to mark a work item as done\n\n");
            helpOutput.append("Usage: done [OPTIONS]\n\n");
            helpOutput.append("Options:\n");
            helpOutput.append("  --id           The ID of the work item to mark as done (required)\n");
            helpOutput.append("  --comment      Optional comment about completion\n");
            helpOutput.append("  -h, --help     Show this help message\n");
            
            System.out.println(helpOutput);
        }
    }
}