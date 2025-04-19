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
import java.util.UUID;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.ServiceManager;

/**
 * Test class for EditCommand.
 */
@ExtendWith(MockitoExtension.class)
class EditCommandTest {

    private EditCommand editCommand;
    @Mock
    private ServiceManager mockServiceManager;
    @Mock
    private ContextManager mockContextManager;
    @Mock
    private MockItemService mockItemService;
    
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    private InputStream originalIn;

    @BeforeEach
    void setUp() {
        // Set up output/error stream capture
        originalOut = System.out;
        originalErr = System.err;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        originalIn = System.in;
        
        // Create the command
        editCommand = new EditCommand();
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
    }

    /**
     * Helper method to set up scanner input for interactive tests.
     */
    private void setInput(String input) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);
    }

    /**
     * Helper method to create a test work item.
     */
    private WorkItem createTestWorkItem(UUID id, String title, WorkItemType type, Priority priority, WorkflowState state) {
        WorkItem item = new WorkItem();
        item.setId(id.toString());
        item.setTitle(title);
        item.setType(type);
        item.setPriority(priority);
        item.setStatus(state);
        item.setDescription("Test description for " + title);
        item.setAssignee("test-user@example.com");
        return item;
    }

    /**
     * Helper method to clear captured console output.
     */
    private void clearOutput() {
        outContent.reset();
        errContent.reset();
    }

    @Nested
    @DisplayName("Help Documentation Tests")
    class HelpDocumentationTests {
        
        // Note: EditCommand doesn't have explicit help documentation methods
        // but we can test error messages that guide users
        
        @Test
        @DisplayName("call should display error when no ID is provided and no context is available")
        void callShouldDisplayErrorWhenNoIdProvidedAndNoContextAvailable() {
            // Arrange
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(null);
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String error = errContent.toString();
                assertEquals(1, result, "Should return error code 1");
                assertTrue(error.contains("Error: No work item context available"), 
                           "Should indicate missing context");
                assertTrue(error.contains("Please specify an ID with id=X"), 
                           "Should provide guidance on how to specify ID");
            }
        }
        
        @Test
        @DisplayName("call should display error for invalid UUID format")
        void callShouldDisplayErrorForInvalidUuidFormat() {
            // Arrange
            editCommand.setItemId("not-a-uuid");
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String error = errContent.toString();
                assertEquals(1, result, "Should return error code 1");
                assertTrue(error.contains("Error: Invalid work item ID format"), 
                           "Should indicate invalid ID format");
            }
        }
        
        @Test
        @DisplayName("call should display error for invalid parameter format")
        void callShouldDisplayErrorForInvalidParameterFormat() {
            // Arrange
            editCommand.setIdParameter("wrong-format");
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String error = errContent.toString();
                assertEquals(1, result, "Should return error code 1");
                assertTrue(error.contains("Error: Invalid parameter format"), 
                           "Should indicate invalid parameter format");
                assertTrue(error.contains("Expected id=UUID"), 
                           "Should provide guidance on correct format");
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should display available fields for editing")
        void interactiveEditShouldDisplayAvailableFieldsForEditing() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                
                // Set up input to cancel the edit
                setInput("0\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String output = outContent.toString();
                assertEquals(0, result, "Should return success code 0");
                assertTrue(output.contains("[1] Title: Test Item"), "Should display title field");
                assertTrue(output.contains("[2] Description:"), "Should display description field");
                assertTrue(output.contains("[3] Priority: MEDIUM"), "Should display priority field");
                assertTrue(output.contains("[4] State: IN_PROGRESS"), "Should display state field");
                assertTrue(output.contains("[5] Assignee:"), "Should display assignee field");
                assertTrue(output.contains("[0] Cancel"), "Should display cancel option");
                assertTrue(output.contains("Enter the number of the field to update"), 
                           "Should prompt for field selection");
            }
        }
    }

    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("call should use explicit item ID when provided")
        void callShouldUseExplicitItemIdWhenProvided() {
            // Arrange
            UUID id = UUID.randomUUID();
            editCommand.setItemId(id.toString());
            
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                
                // Set up input to cancel the edit
                setInput("0\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                assertEquals(0, result, "Should return success code 0");
                verify(mockItemService).getItem(id.toString());
            }
        }
        
        @Test
        @DisplayName("call should use ID from id parameter when provided")
        void callShouldUseIdFromIdParameterWhenProvided() {
            // Arrange
            UUID id = UUID.randomUUID();
            editCommand.setIdParameter("id=" + id);
            
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                
                // Set up input to cancel the edit
                setInput("0\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                assertEquals(0, result, "Should return success code 0");
                verify(mockItemService).getItem(id.toString());
            }
        }
        
        @Test
        @DisplayName("call should use last viewed work item when no ID is provided")
        void callShouldUseLastViewedWorkItemWhenNoIdProvided() {
            // Arrange
            UUID id = UUID.randomUUID();
            
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                
                // Set up input to cancel the edit
                setInput("0\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                assertEquals(0, result, "Should return success code 0");
                verify(mockContextManager).getLastViewedWorkItem();
                verify(mockItemService).getItem(id.toString());
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should update title when title field is selected")
        void interactiveEditShouldUpdateTitleWhenTitleFieldIsSelected() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Original Title", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                when(mockItemService.updateTitle(eq(id), anyString(), anyString())).thenReturn(testItem);
                
                // Set up input to update the title
                setInput("1\nUpdated Title\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String output = outContent.toString();
                assertEquals(0, result, "Should return success code 0");
                assertTrue(output.contains("Title updated successfully"), 
                           "Should confirm title update");
                
                verify(mockItemService).updateTitle(eq(id), eq("Updated Title"), anyString());
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should update description when description field is selected")
        void interactiveEditShouldUpdateDescriptionWhenDescriptionFieldIsSelected() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                when(mockItemService.updateDescription(eq(id), anyString(), anyString())).thenReturn(testItem);
                
                // Set up input to update the description
                setInput("2\nUpdated description text\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String output = outContent.toString();
                assertEquals(0, result, "Should return success code 0");
                assertTrue(output.contains("Description updated successfully"), 
                           "Should confirm description update");
                
                verify(mockItemService).updateDescription(eq(id), eq("Updated description text"), anyString());
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should update priority when priority field is selected")
        void interactiveEditShouldUpdatePriorityWhenPriorityFieldIsSelected() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                when(mockItemService.updatePriority(eq(id), eq(Priority.HIGH), anyString())).thenReturn(testItem);
                
                // Set up input to update the priority
                setInput("3\nHIGH\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String output = outContent.toString();
                assertEquals(0, result, "Should return success code 0");
                assertTrue(output.contains("Priority updated successfully"), 
                           "Should confirm priority update");
                
                verify(mockItemService).updatePriority(eq(id), eq(Priority.HIGH), anyString());
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should update state when state field is selected")
        void interactiveEditShouldUpdateStateWhenStateFieldIsSelected() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                when(mockItemService.updateState(eq(id), eq(WorkflowState.REVIEW), anyString())).thenReturn(testItem);
                
                // Set up input to update the state
                setInput("4\nREVIEW\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String output = outContent.toString();
                assertEquals(0, result, "Should return success code 0");
                assertTrue(output.contains("State updated successfully"), 
                           "Should confirm state update");
                
                verify(mockItemService).updateState(eq(id), eq(WorkflowState.REVIEW), anyString());
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should update assignee when assignee field is selected")
        void interactiveEditShouldUpdateAssigneeWhenAssigneeFieldIsSelected() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                when(mockItemService.assignTo(eq(id), anyString(), anyString())).thenReturn(testItem);
                
                // Set up input to update the assignee
                setInput("5\nnew-assignee@example.com\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String output = outContent.toString();
                assertEquals(0, result, "Should return success code 0");
                assertTrue(output.contains("Assignee updated successfully"), 
                           "Should confirm assignee update");
                
                verify(mockItemService).assignTo(eq(id), eq("new-assignee@example.com"), anyString());
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should cancel editing when cancel option is selected")
        void interactiveEditShouldCancelEditingWhenCancelOptionIsSelected() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                
                // Set up input to cancel the edit
                setInput("0\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String output = outContent.toString();
                assertEquals(0, result, "Should return success code 0");
                assertTrue(output.contains("Update cancelled"), 
                           "Should confirm update was cancelled");
                
                // Verify no update methods were called
                verify(mockItemService, never()).updateTitle(any(UUID.class), anyString(), anyString());
                verify(mockItemService, never()).updateDescription(any(UUID.class), anyString(), anyString());
                verify(mockItemService, never()).updatePriority(any(UUID.class), any(Priority.class), anyString());
                verify(mockItemService, never()).updateState(any(UUID.class), any(WorkflowState.class), anyString());
                verify(mockItemService, never()).assignTo(any(UUID.class), anyString(), anyString());
            }
        }
    }

    @Nested
    @DisplayName("Negative Test Cases")
    class NegativeTestCases {
        
        @Test
        @DisplayName("call should handle work item not found")
        void callShouldHandleWorkItemNotFound() {
            // Arrange
            UUID id = UUID.randomUUID();
            editCommand.setItemId(id.toString());
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockItemService.getItem(id.toString())).thenReturn(null);
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String error = errContent.toString();
                assertEquals(1, result, "Should return error code 1");
                assertTrue(error.contains("Error: Work item not found"), 
                           "Should indicate item not found");
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should handle empty input")
        void interactiveEditShouldHandleEmptyInput() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                
                // Set up empty input
                setInput("\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String error = errContent.toString();
                assertEquals(1, result, "Should return error code 1");
                assertTrue(error.contains("Error: No selection made"), 
                           "Should indicate no selection was made");
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should handle invalid selection")
        void interactiveEditShouldHandleInvalidSelection() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                
                // Set up invalid selection input
                setInput("99\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String error = errContent.toString();
                assertEquals(1, result, "Should return error code 1");
                assertTrue(error.contains("Error: Invalid selection"), 
                           "Should indicate invalid selection");
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should handle non-numeric selection")
        void interactiveEditShouldHandleNonNumericSelection() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                
                // Set up non-numeric selection input
                setInput("abc\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String error = errContent.toString();
                assertEquals(1, result, "Should return error code 1");
                assertTrue(error.contains("Error: Invalid selection"), 
                           "Should indicate invalid selection");
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should handle empty value")
        void interactiveEditShouldHandleEmptyValue() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                
                // Set up input with valid selection but empty value
                setInput("1\n\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String error = errContent.toString();
                assertEquals(1, result, "Should return error code 1");
                assertTrue(error.contains("Error: Empty value not allowed"), 
                           "Should indicate empty value not allowed");
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should handle invalid priority value")
        void interactiveEditShouldHandleInvalidPriorityValue() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                
                // Set up input with priority field but invalid priority value
                setInput("3\nINVALID_PRIORITY\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String error = errContent.toString();
                assertEquals(1, result, "Should return error code 1");
                assertTrue(error.contains("Error: Invalid priority value"), 
                           "Should indicate invalid priority value");
                assertTrue(error.contains("Must be one of:"), 
                           "Should list valid priority options");
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should handle invalid state value")
        void interactiveEditShouldHandleInvalidStateValue() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                
                // Set up input with state field but invalid state value
                setInput("4\nINVALID_STATE\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String error = errContent.toString();
                assertEquals(1, result, "Should return error code 1");
                assertTrue(error.contains("Error: Invalid state value"), 
                           "Should indicate invalid state value");
                assertTrue(error.contains("Must be one of:"), 
                           "Should list valid state options");
            }
        }
        
        @Test
        @DisplayName("call should handle general exceptions")
        void callShouldHandleGeneralExceptions() {
            // Arrange
            UUID id = UUID.randomUUID();
            editCommand.setItemId(id.toString());
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockItemService.getItem(id.toString())).thenThrow(new RuntimeException("Test exception"));
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String error = errContent.toString();
                assertEquals(1, result, "Should return error code 1");
                assertTrue(error.contains("Error: Test exception"), 
                           "Should display the exception message");
            }
        }
    }

    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("call should get work item from ItemService")
        void callShouldGetWorkItemFromItemService() {
            // Arrange
            UUID id = UUID.randomUUID();
            editCommand.setItemId(id.toString());
            
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class)) {
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                
                // Set up input to cancel the edit
                setInput("0\n");
                
                // Act
                editCommand.call();
                
                // Assert
                verify(mockServiceManager).getItemService();
                verify(mockItemService).getItem(id.toString());
            }
        }
        
        @Test
        @DisplayName("call should get last viewed work item from ContextManager when no ID is provided")
        void callShouldGetLastViewedWorkItemFromContextManagerWhenNoIdProvided() {
            // Arrange
            UUID id = UUID.randomUUID();
            
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                
                // Set up input to cancel the edit
                setInput("0\n");
                
                // Act
                editCommand.call();
                
                // Assert
                verify(mockContextManager).getLastViewedWorkItem();
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should call updateTitle when title field is selected")
        void interactiveEditShouldCallUpdateTitleWhenTitleFieldIsSelected() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                when(mockItemService.updateTitle(any(UUID.class), anyString(), anyString())).thenReturn(testItem);
                
                // Set up input to update the title
                setInput("1\nNew Title\n");
                
                // Act
                editCommand.call();
                
                // Assert
                verify(mockItemService).updateTitle(id, "New Title", System.getProperty("user.name"));
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should call updateDescription when description field is selected")
        void interactiveEditShouldCallUpdateDescriptionWhenDescriptionFieldIsSelected() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                when(mockItemService.updateDescription(any(UUID.class), anyString(), anyString())).thenReturn(testItem);
                
                // Set up input to update the description
                setInput("2\nNew Description\n");
                
                // Act
                editCommand.call();
                
                // Assert
                verify(mockItemService).updateDescription(id, "New Description", System.getProperty("user.name"));
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should call updatePriority when priority field is selected")
        void interactiveEditShouldCallUpdatePriorityWhenPriorityFieldIsSelected() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                when(mockItemService.updatePriority(any(UUID.class), any(Priority.class), anyString())).thenReturn(testItem);
                
                // Set up input to update the priority
                setInput("3\nHIGH\n");
                
                // Act
                editCommand.call();
                
                // Assert
                verify(mockItemService).updatePriority(id, Priority.HIGH, System.getProperty("user.name"));
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should call updateState when state field is selected")
        void interactiveEditShouldCallUpdateStateWhenStateFieldIsSelected() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                when(mockItemService.updateState(any(UUID.class), any(WorkflowState.class), anyString())).thenReturn(testItem);
                
                // Set up input to update the state
                setInput("4\nDONE\n");
                
                // Act
                editCommand.call();
                
                // Assert
                verify(mockItemService).updateState(id, WorkflowState.DONE, System.getProperty("user.name"));
            }
        }
        
        @Test
        @DisplayName("interactiveEdit should call assignTo when assignee field is selected")
        void interactiveEditShouldCallAssignToWhenAssigneeFieldIsSelected() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Test Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                when(mockItemService.assignTo(any(UUID.class), anyString(), anyString())).thenReturn(testItem);
                
                // Set up input to update the assignee
                setInput("5\nnew-user@example.com\n");
                
                // Act
                editCommand.call();
                
                // Assert
                verify(mockItemService).assignTo(id, "new-user@example.com", System.getProperty("user.name"));
            }
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Should support complete workflow for editing a work item's title")
        void shouldSupportCompleteWorkflowForEditingWorkItemTitle() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Original Title", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            WorkItem updatedItem = createTestWorkItem(
                id, "Updated Title", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                when(mockItemService.updateTitle(eq(id), eq("Updated Title"), anyString())).thenReturn(updatedItem);
                
                // Set up input for the complete workflow
                setInput("1\nUpdated Title\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String output = outContent.toString();
                assertEquals(0, result, "Workflow should complete successfully");
                
                // Verify the workflow steps
                assertTrue(output.contains("Work Item: " + id), "Should display work item ID");
                assertTrue(output.contains("[1] Title: Original Title"), "Should display original title");
                assertTrue(output.contains("Enter the number of the field to update"), "Should prompt for field selection");
                assertTrue(output.contains("Enter new value"), "Should prompt for new value");
                assertTrue(output.contains("Title updated successfully"), "Should confirm update success");
                
                // Verify service interactions in correct order
                verify(mockContextManager).getLastViewedWorkItem();
                verify(mockItemService).getItem(id.toString());
                verify(mockItemService).updateTitle(eq(id), eq("Updated Title"), anyString());
            }
        }
        
        @Test
        @DisplayName("Should support complete workflow for editing a work item's state")
        void shouldSupportCompleteWorkflowForEditingWorkItemState() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Task Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            WorkItem updatedItem = createTestWorkItem(
                id, "Task Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.DONE);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                when(mockItemService.updateState(eq(id), eq(WorkflowState.DONE), anyString())).thenReturn(updatedItem);
                
                // Set up input for the complete workflow
                setInput("4\nDONE\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String output = outContent.toString();
                assertEquals(0, result, "Workflow should complete successfully");
                
                // Verify the workflow steps
                assertTrue(output.contains("Work Item: " + id), "Should display work item ID");
                assertTrue(output.contains("[4] State: IN_PROGRESS"), "Should display original state");
                assertTrue(output.contains("Enter the number of the field to update"), "Should prompt for field selection");
                assertTrue(output.contains("Enter new value"), "Should prompt for new value");
                assertTrue(output.contains("State updated successfully"), "Should confirm update success");
                
                // Verify service interactions in correct order
                verify(mockContextManager).getLastViewedWorkItem();
                verify(mockItemService).getItem(id.toString());
                verify(mockItemService).updateState(eq(id), eq(WorkflowState.DONE), anyString());
            }
        }
        
        @Test
        @DisplayName("Should handle error handling workflow for invalid state value")
        void shouldHandleErrorHandlingWorkflowForInvalidStateValue() {
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(
                id, "Task Item", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
                when(mockItemService.getItem(id.toString())).thenReturn(testItem);
                
                // Set up input with invalid state value
                setInput("4\nINVALID_STATE\n");
                
                // Act
                int result = editCommand.call();
                
                // Assert
                String output = outContent.toString();
                String error = errContent.toString();
                assertEquals(1, result, "Command should fail due to invalid state");
                
                // Verify error handling workflow
                assertTrue(output.contains("Work Item: " + id), "Should display work item ID");
                assertTrue(output.contains("[4] State: IN_PROGRESS"), "Should display original state");
                assertTrue(output.contains("Enter the number of the field to update"), "Should prompt for field selection");
                assertTrue(output.contains("Enter new value"), "Should prompt for new value");
                assertTrue(error.contains("Error: Invalid state value"), "Should indicate invalid state value");
                assertTrue(error.contains("Must be one of:"), "Should list valid state options");
                
                // Verify no update occurred
                verify(mockItemService, never()).updateState(any(UUID.class), any(WorkflowState.class), anyString());
            }
        }
        
        @Test
        @DisplayName("Should gracefully handle work item lookup using different ID approaches")
        void shouldGracefullyHandleWorkItemLookupUsingDifferentIdApproaches() {
            // Arrange
            UUID id1 = UUID.randomUUID();
            UUID id2 = UUID.randomUUID();
            UUID id3 = UUID.randomUUID();
            
            WorkItem item1 = createTestWorkItem(id1, "Item 1", WorkItemType.TASK, Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            WorkItem item2 = createTestWorkItem(id2, "Item 2", WorkItemType.BUG, Priority.HIGH, WorkflowState.REVIEW);
            WorkItem item3 = createTestWorkItem(id3, "Item 3", WorkItemType.FEATURE, Priority.LOW, WorkflowState.READY);
            
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockServiceManager.getItemService()).thenReturn(mockItemService);
                
                // For each test case, create a new command instance to avoid scanner issues
                EditCommand cmd1 = new EditCommand();
                cmd1.setItemId(id1.toString());
                
                EditCommand cmd2 = new EditCommand();
                cmd2.setIdParameter("id=" + id2);
                
                EditCommand cmd3 = new EditCommand();
                
                // Set up mocks for each test case
                when(mockItemService.getItem(id1.toString())).thenReturn(item1);
                when(mockItemService.getItem(id2.toString())).thenReturn(item2);
                when(mockItemService.getItem(id3.toString())).thenReturn(item3);
                when(mockContextManager.getLastViewedWorkItem()).thenReturn(id3);
                
                // Set up each command with input to cancel the edit
                setInput("0\n");
                
                // Act - execute the first command
                int result1 = cmd1.call();
                String output1 = outContent.toString();
                clearOutput();
                
                // Reset input for the next command
                setInput("0\n");
                
                // Execute the second command
                int result2 = cmd2.call();
                String output2 = outContent.toString();
                clearOutput();
                
                // Reset input for the last command
                setInput("0\n");
                
                // Execute the third command
                int result3 = cmd3.call();
                String output3 = outContent.toString();
                
                // Assert
                assertEquals(0, result1, "First approach should succeed");
                assertEquals(0, result2, "Second approach should succeed");
                assertEquals(0, result3, "Third approach should succeed");
                
                assertTrue(output1.contains("Work Item: " + id1), "Should show correct item ID for first approach");
                assertTrue(output1.contains("Title: Item 1"), "Should show correct title for first approach");
                
                assertTrue(output2.contains("Work Item: " + id2), "Should show correct item ID for second approach");
                assertTrue(output2.contains("Title: Item 2"), "Should show correct title for second approach");
                
                assertTrue(output3.contains("Work Item: " + id3), "Should show correct item ID for third approach");
                assertTrue(output3.contains("Title: Item 3"), "Should show correct title for third approach");
                
                // Verify service interactions
                verify(mockItemService).getItem(id1.toString());
                verify(mockItemService).getItem(id2.toString());
                verify(mockItemService).getItem(id3.toString());
                verify(mockContextManager).getLastViewedWorkItem();
            }
        }
    }
}