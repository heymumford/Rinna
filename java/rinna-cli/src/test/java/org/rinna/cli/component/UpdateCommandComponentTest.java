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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.UpdateCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.InvalidTransitionException;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.ModelMapper;
import org.rinna.cli.util.OutputFormatter;

/**
 * Component test for the UpdateCommand class.
 * This test focuses on the integration of UpdateCommand with the services it depends on.
 */
@DisplayName("UpdateCommand Component Tests")
public class UpdateCommandComponentTest {

    private ServiceManager mockServiceManager;
    private MockItemService mockItemService;
    private MockWorkflowService mockWorkflowService;
    private ConfigurationService mockConfigService;
    private MetadataService mockMetadataService;
    
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    private UUID testItemId;
    private WorkItem testWorkItem;
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    private ArgumentCaptor<Map<String, Object>> resultCaptor;
    
    private MockedStatic<ServiceManager> mockedStaticServiceManager;
    private MockedStatic<ModelMapper> mockedStaticModelMapper;
    private MockedStatic<OutputFormatter> mockedStaticOutputFormatter;
    
    @BeforeEach
    void setUp() {
        // Set up mocks
        mockServiceManager = mock(ServiceManager.class);
        mockItemService = mock(MockItemService.class);
        mockWorkflowService = mock(MockWorkflowService.class);
        mockConfigService = mock(ConfigurationService.class);
        mockMetadataService = mock(MetadataService.class);
        
        // Set up static mocks
        mockedStaticServiceManager = Mockito.mockStatic(ServiceManager.class);
        mockedStaticServiceManager.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        
        mockedStaticModelMapper = Mockito.mockStatic(ModelMapper.class);
        mockedStaticModelMapper.when(() -> ModelMapper.toDomainWorkItem(any(WorkItem.class)))
            .thenAnswer(invocation -> {
                WorkItem item = invocation.getArgument(0);
                return item; // For simplicity, just return the same object
            });
        
        mockedStaticOutputFormatter = Mockito.mockStatic(OutputFormatter.class);
        mockedStaticOutputFormatter.when(() -> OutputFormatter.toJson(any(), anyBoolean()))
            .thenAnswer(invocation -> {
                Object item = invocation.getArgument(0);
                if (item instanceof Map) {
                    Map<String, Object> map = (Map<String, Object>) item;
                    return "{\"id\":\"" + map.get("id") + "\",\"updatedFields\":" + map.get("updatedFields") + "}";
                }
                return "{\"mockJson\":\"value\"}";
            });
        
        // Set up service manager
        when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up metadata service
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn("operation-id");
        
        // Set up current user
        when(mockConfigService.getCurrentUser()).thenReturn("testuser");
        
        // Set up argument captors
        paramsCaptor = ArgumentCaptor.forClass(Map.class);
        resultCaptor = ArgumentCaptor.forClass(Map.class);
        
        // Redirect stdout and stderr for verification
        originalOut = System.out;
        originalErr = System.err;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Initialize test data
        testItemId = UUID.randomUUID();
        testWorkItem = createTestWorkItem();
        
        // Default ItemService behavior
        when(mockItemService.getItem(testItemId.toString())).thenReturn(testWorkItem);
        when(mockItemService.updateItem(any(WorkItem.class))).thenReturn(testWorkItem);
    }
    
    @AfterEach
    void tearDown() {
        // Restore original stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Close static mocks
        if (mockedStaticServiceManager != null) {
            mockedStaticServiceManager.close();
        }
        if (mockedStaticModelMapper != null) {
            mockedStaticModelMapper.close();
        }
        if (mockedStaticOutputFormatter != null) {
            mockedStaticOutputFormatter.close();
        }
    }
    
    private WorkItem createTestWorkItem() {
        WorkItem item = new WorkItem();
        item.setId(testItemId.toString());
        item.setTitle("Original title");
        item.setType(WorkItemType.TASK);
        item.setPriority(Priority.MEDIUM);
        item.setStatus(WorkflowState.CREATED);
        item.setAssignee("alice");
        item.setDescription("Original description");
        item.setReporter("bob");
        item.setProject("TestProject");
        item.setVersion("1.0.0");
        item.setCreated(LocalDateTime.now().minusDays(5));
        item.setUpdated(LocalDateTime.now().minusHours(12));
        item.setDueDate(LocalDateTime.now().plusDays(2));
        
        return item;
    }
    
    @Test
    @DisplayName("Should integrate with ItemService to update work item fields")
    void shouldIntegrateWithItemServiceToUpdateWorkItemFields() {
        // Arrange
        UpdateCommand command = new UpdateCommand(mockServiceManager);
        String testItemIdString = testItemId.toString();
        command.setId(testItemIdString);
        command.setTitle("Updated title");
        command.setPriority(Priority.HIGH);
        
        // Set up updated work item
        WorkItem updatedItem = createTestWorkItem();
        updatedItem.setTitle("Updated title");
        updatedItem.setPriority(Priority.HIGH);
        when(mockItemService.updateItem(any(WorkItem.class))).thenReturn(updatedItem);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify integration with ItemService
        ArgumentCaptor<WorkItem> itemCaptor = ArgumentCaptor.forClass(WorkItem.class);
        verify(mockItemService).updateItem(itemCaptor.capture());
        WorkItem capturedItem = itemCaptor.getValue();
        assertEquals("Updated title", capturedItem.getTitle(), "Title should be updated");
        assertEquals(Priority.HIGH, capturedItem.getPriority(), "Priority should be updated");
        
        // Verify output contains expected elements
        String output = outContent.toString();
        assertTrue(output.contains("Updated work item: " + testItemIdString), "Output should include work item ID");
        assertTrue(output.contains("Title: Updated title"), "Output should include updated title");
        assertTrue(output.contains("Priority: HIGH"), "Output should include updated priority");
        
        // Verify metadata tracking
        verify(mockMetadataService).startOperation(eq("update"), eq("UPDATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals(testItemIdString, params.get("id"), "Operation tracking should include item ID");
        assertEquals("Updated title", params.get("title"), "Operation tracking should include title");
        assertEquals("HIGH", params.get("priority"), "Operation tracking should include priority");
        
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        Map<String, Object> result2 = resultCaptor.getValue();
        assertEquals(testItemIdString, result2.get("itemId"), "Result should include item ID");
        assertTrue(((List<String>)result2.get("updatedFields")).contains("title"), "Result should include updated fields");
        assertTrue(((List<String>)result2.get("updatedFields")).contains("priority"), "Result should include updated fields");
    }
    
    @Test
    @DisplayName("Should integrate with WorkflowService for state transitions")
    void shouldIntegrateWithWorkflowServiceForStateTransitions() {
        // Arrange
        UpdateCommand command = new UpdateCommand(mockServiceManager);
        String testItemIdString = testItemId.toString();
        command.setId(testItemIdString);
        command.setStatus(WorkflowState.IN_PROGRESS);
        
        // Set up WorkflowService to allow transition
        when(mockWorkflowService.canTransition(testItemIdString, WorkflowState.IN_PROGRESS)).thenReturn(true);
        
        // Set up updated work item
        WorkItem updatedItem = createTestWorkItem();
        updatedItem.setStatus(WorkflowState.IN_PROGRESS);
        when(mockWorkflowService.transition(testItemIdString, WorkflowState.IN_PROGRESS)).thenReturn(updatedItem);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify integration with WorkflowService
        verify(mockWorkflowService).canTransition(testItemIdString, WorkflowState.IN_PROGRESS);
        verify(mockWorkflowService).transition(testItemIdString, WorkflowState.IN_PROGRESS);
        
        // Verify ItemService was not used for the update (workflow service handles it)
        verify(mockItemService, never()).updateItem(any(WorkItem.class));
        
        // Verify output contains expected elements
        String output = outContent.toString();
        assertTrue(output.contains("Status: IN_PROGRESS"), "Output should include updated status");
        
        // Verify metadata tracking
        verify(mockMetadataService).startOperation(eq("update"), eq("UPDATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("IN_PROGRESS", params.get("status"), "Operation tracking should include status");
        
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        Map<String, Object> result2 = resultCaptor.getValue();
        assertEquals(true, result2.get("stateChanged"), "Result should indicate state changed");
        assertEquals("IN_PROGRESS", result2.get("newState"), "Result should include new state");
    }
    
    @Test
    @DisplayName("Should integrate with WorkflowService for transitions with comments")
    void shouldIntegrateWithWorkflowServiceForTransitionsWithComments() {
        // Arrange
        UpdateCommand command = new UpdateCommand(mockServiceManager);
        String testItemIdString = testItemId.toString();
        command.setId(testItemIdString);
        command.setStatus(WorkflowState.IN_PROGRESS);
        command.setComment("Transition comment");
        
        // Set up WorkflowService to allow transition
        when(mockWorkflowService.canTransition(testItemIdString, WorkflowState.IN_PROGRESS)).thenReturn(true);
        
        // Set up updated work item
        WorkItem updatedItem = createTestWorkItem();
        updatedItem.setStatus(WorkflowState.IN_PROGRESS);
        when(mockWorkflowService.transition(anyString(), anyString(), any(WorkflowState.class), anyString())).thenReturn(updatedItem);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify integration with WorkflowService with comment
        verify(mockWorkflowService).transition(eq(testItemIdString), eq("testuser"), eq(WorkflowState.IN_PROGRESS), eq("Transition comment"));
        
        // Verify metadata tracking includes comment
        verify(mockMetadataService).startOperation(eq("update"), eq("UPDATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertTrue(params.containsKey("comment"), "Operation tracking should include comment");
    }
    
    @Test
    @DisplayName("Should integrate with OutputFormatter for JSON output")
    void shouldIntegrateWithOutputFormatterForJsonOutput() {
        // Arrange
        UpdateCommand command = new UpdateCommand(mockServiceManager);
        String testItemIdString = testItemId.toString();
        command.setId(testItemIdString);
        command.setTitle("JSON Title");
        command.setFormat("json");
        
        // Set up updated work item
        WorkItem updatedItem = createTestWorkItem();
        updatedItem.setTitle("JSON Title");
        when(mockItemService.updateItem(any(WorkItem.class))).thenReturn(updatedItem);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify OutputFormatter integration
        mockedStaticOutputFormatter.verify(() -> OutputFormatter.toJson(any(), eq(false)));
        
        // Verify output is JSON formatted
        String output = outContent.toString();
        assertTrue(output.contains("{"), "Output should be JSON formatted");
        assertTrue(output.contains("}"), "Output should be JSON formatted");
        
        // Verify metadata tracking includes format
        verify(mockMetadataService).startOperation(eq("update"), eq("UPDATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("json", params.get("format"), "Operation tracking should include format parameter");
    }
    
    @Test
    @DisplayName("Should handle invalid transition errors gracefully")
    void shouldHandleInvalidTransitionErrorsGracefully() {
        // Arrange
        UpdateCommand command = new UpdateCommand(mockServiceManager);
        String testItemIdString = testItemId.toString();
        command.setId(testItemIdString);
        command.setStatus(WorkflowState.DONE);
        
        // Set up WorkflowService to reject transition
        when(mockWorkflowService.canTransition(testItemIdString, WorkflowState.DONE)).thenReturn(false);
        when(mockWorkflowService.getAvailableTransitions(testItemIdString))
            .thenReturn(Arrays.asList(WorkflowState.IN_PROGRESS));
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(1, result, "Command should fail with invalid transition");
        
        // Verify error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Cannot transition work item to DONE state"), 
                "Error output should indicate invalid transition");
        assertTrue(errorOutput.contains("Valid transitions:"), 
                "Error output should include valid transitions");
        
        // Verify metadata tracking includes failure
        verify(mockMetadataService).failOperation(anyString(), any(IllegalStateException.class));
    }
    
    @Test
    @DisplayName("Should handle ItemService exceptions gracefully")
    void shouldHandleItemServiceExceptionsGracefully() {
        // Arrange
        UpdateCommand command = new UpdateCommand(mockServiceManager);
        String testItemIdString = testItemId.toString();
        command.setId(testItemIdString);
        command.setTitle("Error title");
        
        // Set up ItemService to throw exception
        when(mockItemService.updateItem(any(WorkItem.class)))
            .thenThrow(new RuntimeException("Test exception"));
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(1, result, "Command should fail when item service throws exception");
        
        // Verify error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Test exception"), 
                "Error output should include exception message");
        
        // Verify metadata tracking includes exception
        verify(mockMetadataService).failOperation(anyString(), any(Exception.class));
    }
    
    @Test
    @DisplayName("Should handle WorkflowService exceptions gracefully")
    void shouldHandleWorkflowServiceExceptionsGracefully() {
        // Arrange
        UpdateCommand command = new UpdateCommand(mockServiceManager);
        String testItemIdString = testItemId.toString();
        command.setId(testItemIdString);
        command.setStatus(WorkflowState.IN_PROGRESS);
        
        // Set up WorkflowService to allow transition but throw on actual transition
        when(mockWorkflowService.canTransition(testItemIdString, WorkflowState.IN_PROGRESS)).thenReturn(true);
        when(mockWorkflowService.transition(testItemIdString, WorkflowState.IN_PROGRESS))
            .thenThrow(new InvalidTransitionException("Invalid transition"));
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(1, result, "Command should fail when workflow service throws exception");
        
        // Verify error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Invalid transition"), 
                "Error output should include exception message");
        
        // Verify metadata tracking includes exception
        verify(mockMetadataService).failOperation(anyString(), any(IllegalStateException.class));
    }
    
    @Test
    @DisplayName("Should handle non-existent work item gracefully")
    void shouldHandleNonExistentWorkItemGracefully() {
        // Arrange
        UpdateCommand command = new UpdateCommand(mockServiceManager);
        String nonExistentId = UUID.randomUUID().toString();
        command.setId(nonExistentId);
        command.setTitle("Updated title");
        
        // Set up ItemService to return null (item not found)
        when(mockItemService.getItem(nonExistentId)).thenReturn(null);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(1, result, "Command should fail when work item is not found");
        
        // Verify error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Work item not found"), 
                "Error output should indicate work item not found");
        
        // Verify metadata tracking includes failure with appropriate exception
        verify(mockMetadataService).failOperation(anyString(), any(IllegalArgumentException.class));
    }
}