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
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.base.UnitTest;
import org.rinna.cli.command.UpdateCommand;
import org.rinna.cli.service.ServiceManager;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.usecase.ItemService;

/**
 * Unit tests for the UpdateCommand class focusing on interactive update functionality.
 */
@DisplayName("Interactive UpdateCommand Unit Tests")
public class InteractiveUpdateCommandTest extends UnitTest {
    
    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private ItemService itemService;
    
    @Mock
    private WorkItem mockWorkItem;
    
    private UpdateCommand updateCommand;
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final ByteArrayInputStream originalIn = System.in;
    
    private final UUID workItemId = UUID.randomUUID();
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up System.out/err capturing
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Set up default command
        updateCommand = new UpdateCommand();
        setMockServiceManager();
        
        // Reset captors between tests
        outputCaptor.reset();
        errorCaptor.reset();
        
        // Set up mock work item
        setupMockWorkItem();
    }
    
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
    }
    
    /**
     * Sets up a mock ServiceManager to be used by the UpdateCommand.
     */
    private void setMockServiceManager() {
        try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(serviceManager);
            
            // Set up mock responses
            when(serviceManager.getItemService()).thenReturn(itemService);
        }
    }
    
    /**
     * Sets up a mock work item for testing.
     */
    private void setupMockWorkItem() {
        when(mockWorkItem.id()).thenReturn(workItemId);
        when(mockWorkItem.title()).thenReturn("Implement registration form");
        when(mockWorkItem.description()).thenReturn("Create a new registration form with validation");
        when(mockWorkItem.type()).thenReturn(WorkItemType.TASK);
        when(mockWorkItem.priority()).thenReturn(Priority.MEDIUM);
        when(mockWorkItem.state()).thenReturn(WorkflowState.IN_PROGRESS);
        when(mockWorkItem.assignee()).thenReturn("bob");
        when(mockWorkItem.reporter()).thenReturn("alice");
        when(mockWorkItem.createdAt()).thenReturn(Instant.now().minusSeconds(86400)); // 1 day ago
        when(mockWorkItem.updatedAt()).thenReturn(Instant.now().minusSeconds(3600)); // 1 hour ago
        
        when(itemService.getItem(workItemId)).thenReturn(mockWorkItem);
    }
    
    /**
     * Simulates user input for the interactive update.
     */
    private void simulateUserInput(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes()));
    }
    
    @Test
    @DisplayName("Should display interactive update options and update title")
    void shouldDisplayInteractiveUpdateOptionsAndUpdateTitle() {
        // Setup
        updateCommand.setId(workItemId.toString());
        updateCommand.setInteractive(true);
        
        // Simulate user input: Select option 1 (title) and enter new title
        simulateUserInput("1\nUpdated Title\n");
        
        // Execute
        Integer result = updateCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify display of options with field numbers
        String output = outputCaptor.toString();
        assertTrue(output.contains("[1] Title:"), "Should display title with option number");
        assertTrue(output.contains("[2] Description:"), "Should display description with option number");
        assertTrue(output.contains("[3] Priority:"), "Should display priority with option number");
        assertTrue(output.contains("[4] State:"), "Should display state with option number");
        
        // Verify title update
        verify(itemService).updateTitle(eq(workItemId), eq("Updated Title"), anyString());
    }
    
    @Test
    @DisplayName("Should handle description update")
    void shouldHandleDescriptionUpdate() {
        // Setup
        updateCommand.setId(workItemId.toString());
        updateCommand.setInteractive(true);
        
        // Simulate user input: Select option 2 (description) and enter new description
        simulateUserInput("2\nThis is a new description with more details\n");
        
        // Execute
        Integer result = updateCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify description update
        verify(itemService).updateDescription(eq(workItemId), eq("This is a new description with more details"), anyString());
    }
    
    @Test
    @DisplayName("Should handle priority update")
    void shouldHandlePriorityUpdate() {
        // Setup
        updateCommand.setId(workItemId.toString());
        updateCommand.setInteractive(true);
        
        // Simulate user input: Select option 3 (priority) and enter new priority
        simulateUserInput("3\nHIGH\n");
        
        // Execute
        Integer result = updateCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify priority update
        verify(itemService).updatePriority(eq(workItemId), eq(Priority.HIGH), anyString());
    }
    
    @Test
    @DisplayName("Should handle state update")
    void shouldHandleStateUpdate() {
        // Setup
        updateCommand.setId(workItemId.toString());
        updateCommand.setInteractive(true);
        
        // Simulate user input: Select option 4 (state) and enter new state
        simulateUserInput("4\nTESTING\n");
        
        // Execute
        Integer result = updateCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify state update
        verify(itemService).updateState(eq(workItemId), eq(WorkflowState.TESTING), anyString());
    }
    
    @Test
    @DisplayName("Should handle assignee update")
    void shouldHandleAssigneeUpdate() {
        // Setup
        updateCommand.setId(workItemId.toString());
        updateCommand.setInteractive(true);
        
        // Simulate user input: Select option 5 (assignee) and enter new assignee
        simulateUserInput("5\ncharlie\n");
        
        // Execute
        Integer result = updateCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify assignee update
        verify(itemService).assignTo(eq(workItemId), eq("charlie"), anyString());
    }
    
    @Test
    @DisplayName("Should handle cancel option")
    void shouldHandleCancelOption() {
        // Setup
        updateCommand.setId(workItemId.toString());
        updateCommand.setInteractive(true);
        
        // Simulate user input: Cancel option (0)
        simulateUserInput("0\n");
        
        // Execute
        Integer result = updateCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify no updates were made
        verify(itemService, never()).updateTitle(any(), any(), any());
        verify(itemService, never()).updateDescription(any(), any(), any());
        verify(itemService, never()).updatePriority(any(), any(), any());
        verify(itemService, never()).updateState(any(), any(), any());
        verify(itemService, never()).assignTo(any(), any(), any());
        
        assertTrue(outputCaptor.toString().contains("Update cancelled"), 
                "Should show cancellation message");
    }
    
    @Test
    @DisplayName("Should handle invalid option number")
    void shouldHandleInvalidOptionNumber() {
        // Setup
        updateCommand.setId(workItemId.toString());
        updateCommand.setInteractive(true);
        
        // Simulate user input: Invalid option (99) then cancel
        simulateUserInput("99\n0\n");
        
        // Execute
        Integer result = updateCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed with valid second input");
        
        // Verify error message was shown
        assertTrue(errorCaptor.toString().contains("Invalid selection"), 
                "Should show error for invalid selection");
        
        // Verify no updates were made
        verify(itemService, never()).updateTitle(any(), any(), any());
        verify(itemService, never()).updateDescription(any(), any(), any());
        verify(itemService, never()).updatePriority(any(), any(), any());
        verify(itemService, never()).updateState(any(), any(), any());
        verify(itemService, never()).assignTo(any(), any(), any());
    }
    
    @Test
    @DisplayName("Should handle empty input")
    void shouldHandleEmptyInput() {
        // Setup
        updateCommand.setId(workItemId.toString());
        updateCommand.setInteractive(true);
        
        // Simulate user input: Empty input for selection, then cancel
        simulateUserInput("\n0\n");
        
        // Execute
        Integer result = updateCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed with valid second input");
        
        // Verify error message was shown
        assertTrue(errorCaptor.toString().contains("Invalid selection"), 
                "Should show error for empty input");
        
        // Verify no updates were made
        verify(itemService, never()).updateTitle(any(), any(), any());
    }
    
    @Test
    @DisplayName("Should handle non-numeric input")
    void shouldHandleNonNumericInput() {
        // Setup
        updateCommand.setId(workItemId.toString());
        updateCommand.setInteractive(true);
        
        // Simulate user input: Non-numeric input, then cancel
        simulateUserInput("abc\n0\n");
        
        // Execute
        Integer result = updateCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed with valid second input");
        
        // Verify error message was shown
        assertTrue(errorCaptor.toString().contains("Invalid selection"), 
                "Should show error for non-numeric input");
        
        // Verify no updates were made
        verify(itemService, never()).updateTitle(any(), any(), any());
    }
    
    @Test
    @DisplayName("Should handle empty update value")
    void shouldHandleEmptyUpdateValue() {
        // Setup
        updateCommand.setId(workItemId.toString());
        updateCommand.setInteractive(true);
        
        // Simulate user input: Select title (1), but enter empty value, then cancel
        simulateUserInput("1\n\n0\n");
        
        // Execute
        Integer result = updateCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify no updates were made due to empty value
        verify(itemService, never()).updateTitle(any(), any(), any());
        
        assertTrue(errorCaptor.toString().contains("Empty value not allowed"), 
                "Should show error for empty update value");
    }
    
    @Test
    @DisplayName("Should handle invalid priority value")
    void shouldHandleInvalidPriorityValue() {
        // Setup
        updateCommand.setId(workItemId.toString());
        updateCommand.setInteractive(true);
        
        // Simulate user input: Select priority (3), enter invalid value, enter valid value
        simulateUserInput("3\nINVALID_PRIORITY\nHIGH\n");
        
        // Execute
        Integer result = updateCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed with valid second input");
        
        // Verify error message was shown
        assertTrue(errorCaptor.toString().contains("Invalid priority value"), 
                "Should show error for invalid priority");
        
        // Verify priority was updated with the valid value
        verify(itemService).updatePriority(eq(workItemId), eq(Priority.HIGH), anyString());
    }
    
    @Test
    @DisplayName("Should handle invalid state value")
    void shouldHandleInvalidStateValue() {
        // Setup
        updateCommand.setId(workItemId.toString());
        updateCommand.setInteractive(true);
        
        // Simulate user input: Select state (4), enter invalid value, enter valid value
        simulateUserInput("4\nINVALID_STATE\nDONE\n");
        
        // Execute
        Integer result = updateCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed with valid second input");
        
        // Verify error message was shown
        assertTrue(errorCaptor.toString().contains("Invalid state value"), 
                "Should show error for invalid state");
        
        // Verify state was updated with the valid value
        verify(itemService).updateState(eq(workItemId), eq(WorkflowState.DONE), anyString());
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"invalid-id", "123", "", "null", "undefined"})
    @DisplayName("Should fail with invalid work item ID")
    void shouldFailWithInvalidWorkItemId(String invalidId) {
        // Setup
        updateCommand.setId(invalidId);
        updateCommand.setInteractive(true);
        
        // Execute
        Integer result = updateCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Invalid work item ID"), 
                "Should show error message for invalid ID");
    }
    
    @Test
    @DisplayName("Should fail when work item not found")
    void shouldFailWhenWorkItemNotFound() {
        // Setup
        updateCommand.setId(UUID.randomUUID().toString());
        updateCommand.setInteractive(true);
        when(itemService.getItem(any(UUID.class))).thenReturn(null);
        
        // Execute
        Integer result = updateCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Work item not found"), 
                "Should show error message for not found item");
    }
    
    @Test
    @DisplayName("Should handle malicious input safely")
    void shouldHandleMaliciousInputSafely() {
        // Setup
        updateCommand.setId(workItemId.toString());
        updateCommand.setInteractive(true);
        
        // Simulate user input: Select title (1), enter malicious input
        simulateUserInput("1\n'; DROP TABLE WORKITEMS; --\n");
        
        // Execute
        Integer result = updateCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify title was updated with the input as literal text (not executed)
        verify(itemService).updateTitle(eq(workItemId), eq("'; DROP TABLE WORKITEMS; --"), anyString());
    }
}