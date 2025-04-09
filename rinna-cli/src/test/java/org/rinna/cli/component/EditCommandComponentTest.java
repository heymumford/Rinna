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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import org.rinna.cli.command.EditCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Component tests for the EditCommand class.
 * These tests focus on the integration between EditCommand and its dependencies.
 */
@Tag("component")
@DisplayName("Edit Command Component Tests")
class EditCommandComponentTest {

    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private MockItemService mockItemService;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private ContextManager mockContextManager;
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    private final java.io.InputStream originalIn = System.in;
    
    private EditCommand editCommand;
    
    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        MockitoAnnotations.openMocks(this);
        
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn("operation-id");
        when(mockMetadataService.trackOperation(anyString(), any())).thenReturn("field-operation-id");
        
        editCommand = new EditCommand(mockServiceManager);
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        System.setIn(originalIn);
    }
    
    private void setUserInput(String input) {
        ByteArrayInputStream in = new ByteArrayInputStream(input.getBytes());
        System.setIn(in);
    }
    
    private WorkItem createTestWorkItem(UUID id, String title, Priority priority, WorkflowState state) {
        WorkItem item = new WorkItem();
        item.setId(id.toString());
        item.setTitle(title);
        item.setDescription("Test description for " + title);
        item.setPriority(priority);
        item.setStatus(state);
        item.setAssignee("test-user@example.com");
        item.setType(WorkItemType.TASK);
        item.setCreated(LocalDateTime.now().minusDays(5));
        item.setUpdated(LocalDateTime.now().minusHours(2));
        item.setVersion("1.0");
        return item;
    }
    
    @Test
    @DisplayName("Should properly integrate with ItemService for retrieving work item")
    void shouldIntegrateWithItemServiceForRetrievingWorkItem() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
             MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
            
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
            
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(id, "Test Item", Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            when(mockItemService.getItem(id.toString())).thenReturn(testItem);
            editCommand.setItemId(id.toString());
            
            // Set up input to cancel the edit
            setUserInput("0\n");
            
            // Act
            int result = editCommand.call();
            
            // Assert
            assertEquals(0, result, "Command should execute successfully");
            verify(mockItemService).getItem(id.toString());
            
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).startOperation(eq("edit"), eq("UPDATE"), paramsCaptor.capture());
            
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(id.toString(), params.get("itemId"));
        }
    }
    
    @Test
    @DisplayName("Should properly integrate with ContextManager for last viewed item")
    void shouldIntegrateWithContextManagerForLastViewedItem() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
             MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
            
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
            
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(id, "Test Item", Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
            when(mockItemService.getItem(id.toString())).thenReturn(testItem);
            
            // Set up input to cancel the edit
            setUserInput("0\n");
            
            // Act
            int result = editCommand.call();
            
            // Assert
            assertEquals(0, result, "Command should execute successfully");
            verify(mockContextManager).getLastViewedWorkItem();
            verify(mockItemService).getItem(id.toString());
            
            // Verify context is updated after viewing
            verify(mockContextManager).setLastViewedItem(testItem);
        }
    }
    
    @Test
    @DisplayName("Should properly integrate with ItemService.updateTitle for updating title")
    void shouldIntegrateWithItemServiceUpdateTitleForUpdatingTitle() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
             MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
            
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
            
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(id, "Original Title", Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            WorkItem updatedItem = createTestWorkItem(id, "Updated Title", Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            when(mockItemService.getItem(id.toString())).thenReturn(testItem);
            when(mockItemService.updateTitle(eq(id), eq("Updated Title"), anyString())).thenReturn(updatedItem);
            
            editCommand.setItemId(id.toString());
            
            // Set up input to update the title
            setUserInput("1\nUpdated Title\n");
            
            // Act
            int result = editCommand.call();
            
            // Assert
            assertEquals(0, result, "Command should execute successfully");
            
            // Verify item service interactions
            verify(mockItemService).updateTitle(eq(id), eq("Updated Title"), anyString());
            
            // Verify field-level operation tracking
            ArgumentCaptor<Map<String, Object>> fieldParamsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).trackOperation(eq("edit-field"), fieldParamsCaptor.capture());
            
            Map<String, Object> fieldParams = fieldParamsCaptor.getValue();
            assertEquals("Title", fieldParams.get("field"));
            assertEquals("Original Title", fieldParams.get("oldValue"));
            assertEquals("Updated Title", fieldParams.get("newValue"));
            
            // Verify operation completion
            verify(mockMetadataService).completeOperation(eq("field-operation-id"), any());
            verify(mockMetadataService).completeOperation(eq("operation-id"), any());
            
            // Verify context is updated with the updated item
            verify(mockContextManager).setLastViewedItem(updatedItem);
        }
    }
    
    @Test
    @DisplayName("Should properly integrate with ItemService.updateState for updating state")
    void shouldIntegrateWithItemServiceUpdateStateForUpdatingState() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
             MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
            
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
            
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(id, "Test Item", Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            WorkItem updatedItem = createTestWorkItem(id, "Test Item", Priority.MEDIUM, WorkflowState.DONE);
            
            when(mockItemService.getItem(id.toString())).thenReturn(testItem);
            when(mockItemService.updateState(eq(id), eq(WorkflowState.DONE), anyString())).thenReturn(updatedItem);
            
            editCommand.setItemId(id.toString());
            
            // Set up input to update the state
            setUserInput("4\nDONE\n");
            
            // Act
            int result = editCommand.call();
            
            // Assert
            assertEquals(0, result, "Command should execute successfully");
            
            // Verify item service interactions
            verify(mockItemService).updateState(eq(id), eq(WorkflowState.DONE), anyString());
            
            // Verify field-level operation tracking
            ArgumentCaptor<Map<String, Object>> fieldParamsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).trackOperation(eq("edit-field"), fieldParamsCaptor.capture());
            
            Map<String, Object> fieldParams = fieldParamsCaptor.getValue();
            assertEquals("State", fieldParams.get("field"));
            assertEquals("IN_PROGRESS", fieldParams.get("oldValue"));
            assertEquals("DONE", fieldParams.get("newValue"));
            
            // Verify operation completion
            verify(mockMetadataService).completeOperation(eq("field-operation-id"), any());
            verify(mockMetadataService).completeOperation(eq("operation-id"), any());
        }
    }
    
    @Test
    @DisplayName("Should properly integrate with ItemService.updatePriority for updating priority")
    void shouldIntegrateWithItemServiceUpdatePriorityForUpdatingPriority() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
             MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
            
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
            
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(id, "Test Item", Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            WorkItem updatedItem = createTestWorkItem(id, "Test Item", Priority.HIGH, WorkflowState.IN_PROGRESS);
            
            when(mockItemService.getItem(id.toString())).thenReturn(testItem);
            when(mockItemService.updatePriority(eq(id), eq(Priority.HIGH), anyString())).thenReturn(updatedItem);
            
            editCommand.setItemId(id.toString());
            
            // Set up input to update the priority
            setUserInput("3\nHIGH\n");
            
            // Act
            int result = editCommand.call();
            
            // Assert
            assertEquals(0, result, "Command should execute successfully");
            
            // Verify item service interactions
            verify(mockItemService).updatePriority(eq(id), eq(Priority.HIGH), anyString());
            
            // Verify field-level operation tracking
            verify(mockMetadataService).trackOperation(eq("edit-field"), any());
            verify(mockMetadataService).completeOperation(eq("field-operation-id"), any());
        }
    }
    
    @Test
    @DisplayName("Should properly track operations with MetadataService")
    void shouldProperlyTrackOperationsWithMetadataService() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
             MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
            
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
            
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(id, "Test Item", Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            WorkItem updatedItem = createTestWorkItem(id, "Test Item", Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            updatedItem.setAssignee("new-user@example.com");
            
            when(mockItemService.getItem(id.toString())).thenReturn(testItem);
            when(mockItemService.assignTo(eq(id), eq("new-user@example.com"), anyString())).thenReturn(updatedItem);
            
            editCommand.setItemId(id.toString());
            
            // Set up input to update the assignee
            setUserInput("5\nnew-user@example.com\n");
            
            // Define operation IDs for tracking
            when(mockMetadataService.startOperation(eq("edit"), eq("UPDATE"), any()))
                .thenReturn("main-operation-id");
            when(mockMetadataService.trackOperation(eq("edit-field"), any()))
                .thenReturn("field-operation-id");
            
            // Act
            int result = editCommand.call();
            
            // Assert
            assertEquals(0, result, "Command should execute successfully");
            
            // Verify main operation tracking
            verify(mockMetadataService).startOperation(eq("edit"), eq("UPDATE"), any());
            
            // Verify field operation tracking
            verify(mockMetadataService).trackOperation(eq("edit-field"), any());
            
            // Verify completion of both operations in the correct order
            ArgumentCaptor<Map<String, Object>> fieldResultCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).completeOperation(eq("field-operation-id"), fieldResultCaptor.capture());
            
            Map<String, Object> fieldResult = fieldResultCaptor.getValue();
            assertEquals("Assignee", fieldResult.get("field"));
            assertEquals(true, fieldResult.get("success"));
            
            // Verify main operation completion
            ArgumentCaptor<Map<String, Object>> mainResultCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).completeOperation(eq("main-operation-id"), mainResultCaptor.capture());
            
            Map<String, Object> mainResult = mainResultCaptor.getValue();
            assertEquals(true, mainResult.get("success"));
        }
    }
    
    @Test
    @DisplayName("Should properly handle invalid priority with MetadataService failure tracking")
    void shouldProperlyHandleInvalidPriorityWithMetadataServiceFailureTracking() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
             MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
            
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
            
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(id, "Test Item", Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            when(mockItemService.getItem(id.toString())).thenReturn(testItem);
            editCommand.setItemId(id.toString());
            
            // Set up input to provide invalid priority
            setUserInput("3\nINVALID_PRIORITY\n");
            
            // Define operation IDs for tracking
            when(mockMetadataService.startOperation(eq("edit"), eq("UPDATE"), any()))
                .thenReturn("main-operation-id");
            when(mockMetadataService.trackOperation(eq("edit-field"), any()))
                .thenReturn("field-operation-id");
            
            // Act
            int result = editCommand.call();
            
            // Assert
            assertEquals(1, result, "Command should fail");
            
            // Verify error handling in console output
            String error = errContent.toString();
            assertTrue(error.contains("Error: Invalid priority value"), 
                      "Should display invalid priority error");
            
            // Verify field-level failure tracking
            verify(mockMetadataService).failOperation(eq("field-operation-id"), any(IllegalArgumentException.class));
            
            // Verify main operation failure tracking
            verify(mockMetadataService).failOperation(eq("main-operation-id"), any(IllegalArgumentException.class));
            
            // Verify no update was performed
            verify(mockItemService, never()).updatePriority(any(), any(), anyString());
        }
    }
    
    @Test
    @DisplayName("Should handle JSON output format and include detailed work item data")
    void shouldHandleJsonOutputFormatAndIncludeDetailedWorkItemData() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
             MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
            
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
            
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(id, "Test Item", Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            WorkItem updatedItem = createTestWorkItem(id, "Updated Item", Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            when(mockItemService.getItem(id.toString())).thenReturn(testItem);
            when(mockItemService.updateTitle(eq(id), eq("Updated Item"), anyString())).thenReturn(updatedItem);
            
            editCommand.setItemId(id.toString());
            editCommand.setFormat("json");
            editCommand.setVerbose(true);
            
            // Set up input to update the title
            setUserInput("1\nUpdated Item\n");
            
            // Act
            int result = editCommand.call();
            
            // Assert
            assertEquals(0, result, "Command should execute successfully");
            
            // Verify JSON output
            String output = outContent.toString();
            assertTrue(output.contains("\"id\":"), "Should include ID in JSON output");
            assertTrue(output.contains("\"title\":"), "Should include title in JSON output");
            assertTrue(output.contains("\"actions\":"), "Should include actions in JSON output");
            
            // Since verbose is true, should include additional fields
            assertTrue(output.contains("\"type\":"), "Should include type in verbose JSON output");
            assertTrue(output.contains("\"created\":"), "Should include created date in verbose JSON output");
            assertTrue(output.contains("\"updated\":"), "Should include updated date in verbose JSON output");
            
            // Verify operation tracking includes format parameter
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).startOperation(eq("edit"), eq("UPDATE"), paramsCaptor.capture());
            
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals("json", params.get("format"));
            assertEquals(true, params.get("verbose"));
        }
    }
    
    @Test
    @DisplayName("Should properly handle work item update failure")
    void shouldProperlyHandleWorkItemUpdateFailure() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
             MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
            
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
            
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(id, "Test Item", Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            when(mockItemService.getItem(id.toString())).thenReturn(testItem);
            // Simulate update failure by returning null
            when(mockItemService.updateDescription(eq(id), anyString(), anyString())).thenReturn(null);
            
            editCommand.setItemId(id.toString());
            
            // Set up input to update the description
            setUserInput("2\nUpdated description\n");
            
            // Act
            int result = editCommand.call();
            
            // Assert
            assertEquals(1, result, "Command should fail");
            
            // Verify error handling in console output
            String error = errContent.toString();
            assertTrue(error.contains("Error: Failed to update Description"), 
                      "Should display description update failure error");
            
            // Verify field-level failure tracking
            verify(mockMetadataService).failOperation(eq("field-operation-id"), any(RuntimeException.class));
            
            // Verify main operation failure tracking
            verify(mockMetadataService).failOperation(eq("operation-id"), any(RuntimeException.class));
        }
    }
    
    @Test
    @DisplayName("Should handle exception during ItemService.getItem call")
    void shouldHandleExceptionDuringItemServiceGetItemCall() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
             MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
            
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
            
            // Arrange
            UUID id = UUID.randomUUID();
            
            when(mockContextManager.getLastViewedWorkItem()).thenReturn(id);
            when(mockItemService.getItem(id.toString())).thenThrow(new RuntimeException("Database connection error"));
            
            // Act
            int result = editCommand.call();
            
            // Assert
            assertEquals(1, result, "Command should fail");
            
            // Verify error handling in console output
            String error = errContent.toString();
            assertTrue(error.contains("Error: Database connection error"), 
                      "Should display the exception message");
            
            // Verify operation failure tracking
            verify(mockMetadataService).failOperation(eq("operation-id"), any(RuntimeException.class));
        }
    }
    
    @Test
    @DisplayName("Should properly handle cancellation and track it correctly")
    void shouldProperlyHandleCancellationAndTrackItCorrectly() {
        try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
             MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
            
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
            contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
            
            // Arrange
            UUID id = UUID.randomUUID();
            WorkItem testItem = createTestWorkItem(id, "Test Item", Priority.MEDIUM, WorkflowState.IN_PROGRESS);
            
            when(mockItemService.getItem(id.toString())).thenReturn(testItem);
            editCommand.setItemId(id.toString());
            
            // Set up input to cancel the edit
            setUserInput("0\n");
            
            // Act
            int result = editCommand.call();
            
            // Assert
            assertEquals(0, result, "Command should execute successfully");
            
            // Verify console output
            String output = outContent.toString();
            assertTrue(output.contains("Update cancelled"), 
                      "Should display cancellation message");
            
            // Verify operation completion (not failure) with cancel action
            ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).completeOperation(eq("operation-id"), resultCaptor.capture());
            
            Map<String, Object> result = resultCaptor.getValue();
            assertEquals("cancel", result.get("action"));
            
            // Verify no update methods were called
            verify(mockItemService, never()).updateTitle(any(UUID.class), anyString(), anyString());
            verify(mockItemService, never()).updateDescription(any(UUID.class), anyString(), anyString());
            verify(mockItemService, never()).updatePriority(any(UUID.class), any(Priority.class), anyString());
            verify(mockItemService, never()).updateState(any(UUID.class), any(WorkflowState.class), anyString());
            verify(mockItemService, never()).assignTo(any(UUID.class), anyString(), anyString());
        }
    }
}
