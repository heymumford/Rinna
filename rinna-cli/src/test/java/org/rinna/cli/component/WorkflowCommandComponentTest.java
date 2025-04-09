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
import org.rinna.cli.command.WorkflowCommand;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.InvalidTransitionException;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Component tests for the WorkflowCommand class.
 */
@Tag("component")
@DisplayName("Workflow Command Component Tests")
class WorkflowCommandComponentTest {

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    @Mock
    private MockItemService mockItemService;
    
    @Mock
    private MockWorkflowService mockWorkflowService;
    
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
    @DisplayName("State Transition Tests")
    class StateTransitionTests {
        private WorkItem testItem;
        private String testItemId;
        
        @BeforeEach
        void setUpTestItem() {
            // Create test work item
            testItem = new WorkItem();
            testItemId = UUID.randomUUID().toString();
            testItem.setId(testItemId);
            testItem.setTitle("Test Workflow Item");
            testItem.setDescription("Item for testing workflow transitions");
            testItem.setType(WorkItemType.TASK);
            testItem.setPriority(Priority.HIGH);
            testItem.setState(WorkflowState.READY);
            
            // Set up item service mock
            when(mockItemService.getItem(testItemId)).thenReturn(testItem);
            
            // Set up workflow service mock
            when(mockWorkflowService.getItem(testItemId)).thenReturn(testItem);
            when(mockWorkflowService.getCurrentState(testItemId)).thenReturn(WorkflowState.READY);
            
            // Set up available transitions
            when(mockWorkflowService.getAvailableTransitions(testItemId))
                .thenReturn(Arrays.asList(WorkflowState.IN_PROGRESS, WorkflowState.BLOCKED));
            
            // Set up transition method
            when(mockWorkflowService.transition(eq(testItemId), eq(WorkflowState.IN_PROGRESS)))
                .thenAnswer(invocation -> {
                    testItem.setState(WorkflowState.IN_PROGRESS);
                    return testItem;
                });
            
            // Set up invalid transition
            when(mockWorkflowService.transition(eq(testItemId), eq(WorkflowState.DONE)))
                .thenThrow(new InvalidTransitionException(testItemId, WorkflowState.READY, WorkflowState.DONE));
        }
        
        @Test
        @DisplayName("Should show current state of work item")
        void shouldShowCurrentStateOfWorkItem() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);
                when(mockManager.getWorkflowService()).thenReturn(mockWorkflowService);
                
                // Setup WorkflowCommand for viewing state
                WorkflowCommand workflowCmd = new WorkflowCommand();
                workflowCmd.setId(testItemId);
                
                // Execute command
                int exitCode = workflowCmd.call();
                
                // Verify service interaction
                verify(mockWorkflowService).getCurrentState(eq(testItemId));
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");
                
                // Verify output contains current state
                String output = outputStream.toString();
                assertTrue(output.contains("READY"), "Output should contain current state");
            }
        }
        
        @Test
        @DisplayName("Should transition work item to new state")
        void shouldTransitionWorkItemToNewState() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);
                when(mockManager.getWorkflowService()).thenReturn(mockWorkflowService);
                
                // Setup WorkflowCommand for transitioning state
                WorkflowCommand workflowCmd = new WorkflowCommand();
                workflowCmd.setId(testItemId);
                workflowCmd.setState(WorkflowState.IN_PROGRESS);
                
                // Execute command
                int exitCode = workflowCmd.call();
                
                // Verify service interaction
                verify(mockWorkflowService).transition(eq(testItemId), eq(WorkflowState.IN_PROGRESS));
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");
                
                // Verify output confirms transition
                String output = outputStream.toString();
                assertTrue(output.contains("transitioned") || output.contains("updated"), 
                    "Output should confirm transition");
                assertTrue(output.contains("IN_PROGRESS"), "Output should mention new state");
            }
        }
        
        @Test
        @DisplayName("Should show error for invalid transition")
        void shouldShowErrorForInvalidTransition() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);
                when(mockManager.getWorkflowService()).thenReturn(mockWorkflowService);
                
                // Setup WorkflowCommand for invalid transition
                WorkflowCommand workflowCmd = new WorkflowCommand();
                workflowCmd.setId(testItemId);
                workflowCmd.setState(WorkflowState.DONE);
                
                // Execute command
                int exitCode = workflowCmd.call();
                
                // Verify service interaction
                verify(mockWorkflowService).transition(eq(testItemId), eq(WorkflowState.DONE));
                
                // Verify command handles error appropriately
                assertEquals(1, exitCode, "Command should return error code for invalid transition");
                
                // Verify error output
                String error = errorStream.toString();
                assertTrue(error.contains("Invalid transition") || error.contains("Cannot transition"), 
                    "Error should explain invalid transition");
            }
        }
        
        @Test
        @DisplayName("Should show available transitions")
        void shouldShowAvailableTransitions() {
            try (MockedStatic<ServiceManager> serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);
                when(mockManager.getWorkflowService()).thenReturn(mockWorkflowService);
                
                // Setup WorkflowCommand for viewing available transitions
                WorkflowCommand workflowCmd = new WorkflowCommand();
                workflowCmd.setId(testItemId);
                workflowCmd.setShowAvailable(true);
                
                // Execute command
                int exitCode = workflowCmd.call();
                
                // Verify service interaction
                verify(mockWorkflowService).getAvailableTransitions(eq(testItemId));
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");
                
                // Verify output shows available transitions
                String output = outputStream.toString();
                assertTrue(output.contains("IN_PROGRESS"), "Output should list IN_PROGRESS as available transition");
                assertTrue(output.contains("BLOCKED"), "Output should list BLOCKED as available transition");
            }
        }
    }
}