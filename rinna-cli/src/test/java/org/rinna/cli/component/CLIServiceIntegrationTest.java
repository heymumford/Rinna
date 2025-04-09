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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.command.CommentCommand;
import org.rinna.cli.command.HistoryCommand;
import org.rinna.cli.command.WorkflowCommand;
import org.rinna.cli.model.Comment;
import org.rinna.cli.model.HistoryEntry;
import org.rinna.cli.model.HistoryEntryType;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MockCommentService;
import org.rinna.cli.service.MockHistoryService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockSearchService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.ServiceStatus;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Component tests for the CLI service integration.
 * These tests verify that the CLI service components work together correctly.
 */
@Tag("component")
@DisplayName("CLI Service Integration Tests")
class CLIServiceIntegrationTest {

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    @Mock
    private MockItemService mockItemService;
    
    @Mock
    private MockHistoryService mockHistoryService;
    
    @Mock
    private MockWorkflowService mockWorkflowService;
    
    @Mock
    private MockSearchService mockSearchService;
    
    @Mock
    private MockCommentService mockCommentService;
    
    private AutoCloseable mocks;
    
    @BeforeEach
    void setUp() {
        // Initialize mocks
        mocks = MockitoAnnotations.openMocks(this);
        
        // Create fresh streams for each test to avoid cross-test contamination
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }
    
    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Close mocks
        if (mocks != null) {
            mocks.close();
        }
    }
    
    @Nested
    @DisplayName("Service Manager Tests")
    class ServiceManagerTests {
        @Test
        @DisplayName("Should get singleton instance")
        void shouldGetSingletonInstance() {
            // When
            ServiceManager instance1 = ServiceManager.getInstance();
            ServiceManager instance2 = ServiceManager.getInstance();
            
            // Then
            assertSame(instance1, instance2, "Multiple calls to getInstance() should return the same instance");
        }
        
        @Test
        @DisplayName("Should provide access to service implementations")
        void shouldProvideAccessToServiceImplementations() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                
                // Configure mock services
                when(mockManager.getItemService()).thenReturn(mockItemService);
                when(mockManager.getHistoryService()).thenReturn(mockHistoryService);
                when(mockManager.getWorkflowService()).thenReturn(mockWorkflowService);
                when(mockManager.getSearchService()).thenReturn(mockSearchService);
                
                // When accessing services
                MockItemService itemService = ServiceManager.getInstance().getItemService();
                MockHistoryService historyService = ServiceManager.getInstance().getHistoryService();
                MockWorkflowService workflowService = ServiceManager.getInstance().getWorkflowService();
                MockSearchService searchService = ServiceManager.getInstance().getSearchService();
                
                // Then
                assertSame(mockItemService, itemService, "Should return the mock item service");
                assertSame(mockHistoryService, historyService, "Should return the mock history service");
                assertSame(mockWorkflowService, workflowService, "Should return the mock workflow service");
                assertSame(mockSearchService, searchService, "Should return the mock search service");
            }
        }
    }
    
    @Nested
    @DisplayName("Service Status Tests")
    class ServiceStatusTests {
        @Test
        @DisplayName("Should correctly determine service availability")
        void shouldCorrectlyDetermineServiceAvailability() {
            // When
            ServiceStatus status = new ServiceStatus(true, "Service is running");
            ServiceStatus unavailableStatus = new ServiceStatus(false, "Service is down");
            
            // Then
            assertTrue(status.isAvailable(), "Service should be available");
            assertEquals("Service is running", status.getMessage(), "Status message should match");
            assertFalse(unavailableStatus.isAvailable(), "Service should be unavailable");
        }
        
        @Test
        @DisplayName("Should format status message correctly")
        void shouldFormatStatusMessageCorrectly() {
            // When
            ServiceStatus status = new ServiceStatus(true, "Service is running");
            
            // Then
            String formattedStatus = status.toString();
            assertTrue(formattedStatus.contains("available"), "Formatted status should indicate availability");
            assertTrue(formattedStatus.contains("Service is running"), "Formatted status should include message");
        }
    }
    
    @Nested
    @DisplayName("Cross-Service Interaction Tests")
    class CrossServiceInteractionTests {
        private WorkItem testItem;
        
        @BeforeEach
        void setupTestData() {
            // Create test work item
            testItem = new WorkItem();
            testItem.setId(UUID.randomUUID().toString());
            testItem.setTitle("Test Integration Item");
            testItem.setType(WorkItemType.TASK);
            testItem.setPriority(Priority.HIGH);
            testItem.setState(WorkflowState.READY);
            
            // Configure item service
            when(mockItemService.getItem(testItem.getId())).thenReturn(testItem);
            
            // Configure workflow service
            when(mockWorkflowService.getItem(testItem.getId())).thenReturn(testItem);
            when(mockWorkflowService.getCurrentState(testItem.getId())).thenReturn(WorkflowState.READY);
            when(mockWorkflowService.transition(eq(testItem.getId()), any(WorkflowState.class)))
                .thenAnswer(invocation -> {
                    WorkflowState newState = invocation.getArgument(1);
                    testItem.setState(newState);
                    return testItem;
                });
            
            // Configure history service
            when(mockHistoryService.getHistoryForItem(testItem.getId()))
                .thenReturn(new ArrayList<>());
        }
        
        @Test
        @DisplayName("Should coordinate between workflow and history services")
        void shouldCoordinateBetweenWorkflowAndHistoryServices() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getWorkflowService()).thenReturn(mockWorkflowService);
                when(mockManager.getHistoryService()).thenReturn(mockHistoryService);
                
                // Simulate a workflow state change that should also record history
                mockWorkflowService.transition(testItem.getId(), WorkflowState.IN_PROGRESS);
                
                // Verify coordination between services
                verify(mockHistoryService).recordStateChange(
                    eq(testItem.getId()),
                    eq(WorkflowState.READY.toString()),
                    eq(WorkflowState.IN_PROGRESS.toString()),
                    anyString(),
                    anyString()
                );
                
                // Verify the state was updated
                assertEquals(WorkflowState.IN_PROGRESS, testItem.getState());
            }
        }
        
        @Test
        @DisplayName("Should handle workflow transitions with comments")
        void shouldHandleWorkflowTransitionsWithComments() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getWorkflowService()).thenReturn(mockWorkflowService);
                when(mockManager.getHistoryService()).thenReturn(mockHistoryService);
                
                // Simulate a workflow state change with comment
                String comment = "Starting work on this task";
                mockWorkflowService.transition(testItem.getId(), "user1", WorkflowState.IN_PROGRESS, comment);
                
                // Verify coordination between services
                verify(mockHistoryService).recordStateChange(
                    eq(testItem.getId()),
                    eq(WorkflowState.READY.toString()),
                    eq(WorkflowState.IN_PROGRESS.toString()),
                    eq("user1"),
                    eq(comment)
                );
                
                // Verify the state was updated
                assertEquals(WorkflowState.IN_PROGRESS, testItem.getState());
            }
        }
    }
    
    @Nested
    @DisplayName("Command to Service Integration Tests")
    class CommandToServiceIntegrationTests {
        private WorkItem testItem;
        private String testItemId;
        
        @BeforeEach
        void setupTestItem() {
            // Create test work item
            testItemId = UUID.randomUUID().toString();
            testItem = new WorkItem();
            testItem.setId(testItemId);
            testItem.setTitle("Test Command Item");
            testItem.setDescription("Item for testing command-service integration");
            testItem.setType(WorkItemType.FEATURE);
            testItem.setPriority(Priority.HIGH);
            testItem.setState(WorkflowState.READY);
            testItem.setCreated(LocalDateTime.now().minusDays(2));
            testItem.setUpdated(LocalDateTime.now().minusHours(6));
            
            // Configure item service
            when(mockItemService.getItem(testItemId)).thenReturn(testItem);
            
            // Create history entries for state changes
            HistoryEntry entry1 = new HistoryEntry();
            entry1.setId(UUID.randomUUID().toString());
            entry1.setItemId(testItemId);
            entry1.setType(HistoryEntryType.STATE_CHANGE);
            entry1.setOldValue(WorkflowState.CREATED.name());
            entry1.setNewValue(WorkflowState.READY.name());
            entry1.setTimestamp(LocalDateTime.now().minusDays(1));
            entry1.setUser("user1");
            entry1.setComment("Initial triage");
            
            // Configure history service to return entries
            when(mockHistoryService.getHistoryForItem(eq(testItemId)))
                .thenReturn(List.of(entry1));
            
            // Create test comments
            Comment comment1 = new Comment();
            comment1.setId(UUID.randomUUID().toString());
            comment1.setItemId(testItemId);
            comment1.setText("This is an initial comment on the work item");
            comment1.setCreated(LocalDateTime.now().minusDays(1));
            comment1.setAuthor("user1");
            
            // Configure comment service
            when(mockCommentService.getCommentsForItem(eq(testItemId)))
                .thenReturn(List.of(comment1));
            
            // Configure comment service to add comments
            when(mockCommentService.addComment(eq(testItemId), anyString(), anyString()))
                .thenAnswer(invocation -> {
                    String text = invocation.getArgument(1);
                    Comment newComment = new Comment();
                    newComment.setId(UUID.randomUUID().toString());
                    newComment.setItemId(testItemId);
                    newComment.setText(text);
                    newComment.setCreated(LocalDateTime.now());
                    newComment.setAuthor("current-user");
                    return newComment;
                });
        }
        
        @Test
        @DisplayName("Should execute workflow command to transition state")
        void shouldExecuteWorkflowCommandToTransitionState() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);
                when(mockManager.getWorkflowService()).thenReturn(mockWorkflowService);
                when(mockManager.getHistoryService()).thenReturn(mockHistoryService);
                
                // Configure workflow service behavior
                doNothing().when(mockWorkflowService).transition(anyString(), any(WorkflowState.class));
                
                // Setup WorkflowCommand
                WorkflowCommand workflowCmd = new WorkflowCommand();
                workflowCmd.setId(testItemId);
                workflowCmd.setState(WorkflowState.IN_PROGRESS);
                
                // Execute command
                int exitCode = workflowCmd.call();
                
                // Verify workflow service interaction
                verify(mockWorkflowService).transition(eq(testItemId), eq(WorkflowState.IN_PROGRESS));
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");
                
                // Verify output
                String output = outputStream.toString();
                assertTrue(output.contains("transitioned") || output.contains("updated"), 
                    "Output should confirm state transition");
            }
        }
        
        @Test
        @DisplayName("Should execute history command to display item history")
        void shouldExecuteHistoryCommandToDisplayItemHistory() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);
                when(mockManager.getHistoryService()).thenReturn(mockHistoryService);
                
                // Setup HistoryCommand
                HistoryCommand historyCmd = new HistoryCommand();
                historyCmd.setId(testItemId);
                
                // Execute command
                int exitCode = historyCmd.call();
                
                // Verify history service interaction
                verify(mockHistoryService).getHistoryForItem(eq(testItemId));
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");
                
                // Verify output
                String output = outputStream.toString();
                assertTrue(output.contains("CREATED → READY"), "Output should show state transition");
                assertTrue(output.contains("Initial triage"), "Output should include transition comment");
            }
        }
        
        @Test
        @DisplayName("Should execute comment command to add item comment")
        void shouldExecuteCommentCommandToAddItemComment() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);
                when(mockManager.getCommentService()).thenReturn(mockCommentService);
                
                // Setup CommentCommand
                CommentCommand commentCmd = new CommentCommand();
                commentCmd.setId(testItemId);
                commentCmd.setText("New implementation details added");
                
                // Execute command
                int exitCode = commentCmd.call();
                
                // Verify comment service interaction
                verify(mockCommentService).addComment(
                    eq(testItemId), 
                    eq("New implementation details added"), 
                    anyString()
                );
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");
                
                // Verify output
                String output = outputStream.toString();
                assertTrue(output.contains("Comment added") || output.contains("Added comment"), 
                    "Output should confirm comment was added");
            }
        }
        
        @Test
        @DisplayName("Should execute comment command to list item comments")
        void shouldExecuteCommentCommandToListItemComments() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);
                when(mockManager.getCommentService()).thenReturn(mockCommentService);
                
                // Setup CommentCommand
                CommentCommand commentCmd = new CommentCommand();
                commentCmd.setId(testItemId);
                commentCmd.setList(true);
                
                // Execute command
                int exitCode = commentCmd.call();
                
                // Verify comment service interaction
                verify(mockCommentService).getCommentsForItem(eq(testItemId));
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");
                
                // Verify output
                String output = outputStream.toString();
                assertTrue(output.contains("This is an initial comment on the work item"), 
                    "Output should display comment text");
                assertTrue(output.contains("user1"), "Output should display comment author");
            }
        }
    }
}