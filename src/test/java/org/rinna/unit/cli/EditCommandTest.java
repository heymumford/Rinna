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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.base.UnitTest;
import org.rinna.cli.command.EditCommand;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.ServiceManager;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.usecase.ItemService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the EditCommand class.
 */
@DisplayName("EditCommand Unit Tests")
public class EditCommandTest extends UnitTest {
    
    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private ContextManager contextManager;
    
    @Mock
    private ItemService itemService;
    
    @Mock
    private WorkItem mockWorkItem;
    
    private EditCommand editCommand;
    
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
        editCommand = new EditCommand();
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
     * Sets up a mock ServiceManager to be used by the EditCommand.
     */
    private void setMockServiceManager() {
        try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(serviceManager);
            
            // Set up mock responses
            when(serviceManager.getItemService()).thenReturn(itemService);
        }
        
        try (var contextManagerMock = mockStatic(ContextManager.class)) {
            contextManagerMock.when(ContextManager::getInstance).thenReturn(contextManager);
        }
    }
    
    /**
     * Sets up a mock work item for testing.
     */
    private void setupMockWorkItem() {
        when(mockWorkItem.id()).thenReturn(workItemId);
        when(mockWorkItem.title()).thenReturn("Implement login screen");
        when(mockWorkItem.description()).thenReturn("Create a login screen with validation");
        when(mockWorkItem.type()).thenReturn(WorkItemType.TASK);
        when(mockWorkItem.priority()).thenReturn(Priority.MEDIUM);
        when(mockWorkItem.state()).thenReturn(WorkflowState.IN_PROGRESS);
        when(mockWorkItem.assignee()).thenReturn("bob");
        when(mockWorkItem.reporter()).thenReturn("alice");
        when(mockWorkItem.createdAt()).thenReturn(Instant.now().minusSeconds(86400)); // 1 day ago
        when(mockWorkItem.updatedAt()).thenReturn(Instant.now().minusSeconds(3600));  // 1 hour ago
        
        when(itemService.getItem(workItemId)).thenReturn(mockWorkItem);
    }
    
    /**
     * Simulates user input for the interactive editor.
     */
    private void simulateUserInput(String input) {
        System.setIn(new ByteArrayInputStream(input.getBytes()));
    }
    
    @Test
    @DisplayName("Edit command should use last viewed work item")
    void editCommandShouldUseLastViewedWorkItem() {
        // Setup
        when(contextManager.getLastViewedWorkItem()).thenReturn(workItemId);
        simulateUserInput("0\n"); // Cancel option
        
        // Execute
        Integer result = editCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify work item was fetched using the last viewed ID
        verify(itemService).getItem(workItemId);
        
        // Verify the editor was displayed
        String output = outputCaptor.toString();
        assertTrue(output.contains("Work Item: " + workItemId), 
                "Output should show work item ID");
        assertTrue(output.contains("Implement login screen"), 
                "Output should show work item title");
    }
    
    @Test
    @DisplayName("Edit command should use explicit ID parameter")
    void editCommandShouldUseExplicitIdParameter() {
        // Setup
        UUID explicitId = UUID.randomUUID();
        WorkItem explicitWorkItem = mock(WorkItem.class);
        when(explicitWorkItem.id()).thenReturn(explicitId);
        when(explicitWorkItem.title()).thenReturn("Explicit Work Item");
        
        when(itemService.getItem(explicitId)).thenReturn(explicitWorkItem);
        
        editCommand.setItemId(explicitId.toString());
        simulateUserInput("0\n"); // Cancel option
        
        // Execute
        Integer result = editCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify work item was fetched using the explicit ID
        verify(itemService).getItem(explicitId);
        
        // Verify the editor was displayed for the explicit item
        String output = outputCaptor.toString();
        assertTrue(output.contains("Work Item: " + explicitId), 
                "Output should show explicit work item ID");
        assertTrue(output.contains("Explicit Work Item"), 
                "Output should show explicit work item title");
    }
    
    @Test
    @DisplayName("Edit command with id= parameter should work")
    void editCommandWithIdParameterShouldWork() {
        // Setup
        UUID explicitId = UUID.randomUUID();
        WorkItem explicitWorkItem = mock(WorkItem.class);
        when(explicitWorkItem.id()).thenReturn(explicitId);
        when(explicitWorkItem.title()).thenReturn("Parameter Work Item");
        
        when(itemService.getItem(explicitId)).thenReturn(explicitWorkItem);
        
        editCommand.setIdParameter("id=" + explicitId.toString());
        simulateUserInput("0\n"); // Cancel option
        
        // Execute
        Integer result = editCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify work item was fetched using the parameter ID
        verify(itemService).getItem(explicitId);
        
        // Verify the editor was displayed for the parameter item
        String output = outputCaptor.toString();
        assertTrue(output.contains("Work Item: " + explicitId), 
                "Output should show parameter work item ID");
        assertTrue(output.contains("Parameter Work Item"), 
                "Output should show parameter work item title");
    }
    
    @Test
    @DisplayName("Explicit ID should override last viewed work item")
    void explicitIdShouldOverrideLastViewedWorkItem() {
        // Setup
        UUID lastViewedId = UUID.randomUUID();
        when(contextManager.getLastViewedWorkItem()).thenReturn(lastViewedId);
        
        UUID explicitId = UUID.randomUUID();
        WorkItem explicitWorkItem = mock(WorkItem.class);
        when(explicitWorkItem.id()).thenReturn(explicitId);
        when(explicitWorkItem.title()).thenReturn("Override Work Item");
        
        when(itemService.getItem(explicitId)).thenReturn(explicitWorkItem);
        
        editCommand.setItemId(explicitId.toString());
        simulateUserInput("0\n"); // Cancel option
        
        // Execute
        Integer result = editCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify work item was fetched using the explicit ID, not the last viewed ID
        verify(itemService).getItem(explicitId);
        verify(itemService, never()).getItem(lastViewedId);
        
        // Verify the editor was displayed for the explicit item
        String output = outputCaptor.toString();
        assertTrue(output.contains("Work Item: " + explicitId), 
                "Output should show explicit work item ID");
        assertTrue(output.contains("Override Work Item"), 
                "Output should show explicit work item title");
    }
    
    @Test
    @DisplayName("Edit command should fail when no context is available")
    void editCommandShouldFailWhenNoContextIsAvailable() {
        // Setup
        when(contextManager.getLastViewedWorkItem()).thenReturn(null);
        
        // Execute
        Integer result = editCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("No work item context available"), 
                "Error output should indicate no context available");
    }
    
    @Test
    @DisplayName("Edit command should fail when work item not found")
    void editCommandShouldFailWhenWorkItemNotFound() {
        // Setup
        UUID nonExistentId = UUID.randomUUID();
        when(contextManager.getLastViewedWorkItem()).thenReturn(nonExistentId);
        when(itemService.getItem(nonExistentId)).thenReturn(null);
        
        // Execute
        Integer result = editCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Work item not found"), 
                "Error output should indicate item not found");
    }
    
    @Test
    @DisplayName("Edit command should update title")
    void editCommandShouldUpdateTitle() {
        // Setup
        when(contextManager.getLastViewedWorkItem()).thenReturn(workItemId);
        simulateUserInput("1\nUpdated Title\n"); // Select title option and enter new title
        
        // Execute
        Integer result = editCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify title was updated
        verify(itemService).updateTitle(eq(workItemId), eq("Updated Title"), anyString());
    }
    
    @Test
    @DisplayName("Edit command should update description")
    void editCommandShouldUpdateDescription() {
        // Setup
        when(contextManager.getLastViewedWorkItem()).thenReturn(workItemId);
        simulateUserInput("2\nUpdated description with more details\n"); // Select description option and enter new description
        
        // Execute
        Integer result = editCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify description was updated
        verify(itemService).updateDescription(eq(workItemId), eq("Updated description with more details"), anyString());
    }
    
    @Test
    @DisplayName("Edit command should update priority")
    void editCommandShouldUpdatePriority() {
        // Setup
        when(contextManager.getLastViewedWorkItem()).thenReturn(workItemId);
        simulateUserInput("3\nHIGH\n"); // Select priority option and enter new priority
        
        // Execute
        Integer result = editCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify priority was updated
        verify(itemService).updatePriority(eq(workItemId), eq(Priority.HIGH), anyString());
    }
    
    @Test
    @DisplayName("Edit command should handle security vulnerabilities")
    void editCommandShouldHandleSecurityVulnerabilities() {
        // Setup
        editCommand.setIdParameter("id=5; rm -rf /");
        
        // Execute
        Integer result = editCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Invalid work item ID format"), 
                "Error output should indicate invalid ID format");
    }
}