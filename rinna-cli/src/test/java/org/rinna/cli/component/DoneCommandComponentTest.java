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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.command.DoneCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.InvalidTransitionException;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Component integration tests for the DoneCommand.
 * These tests verify the integration between DoneCommand and its services.
 */
@DisplayName("DoneCommand Component Integration Tests")
public class DoneCommandComponentTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Mock
    private ServiceManager mockServiceManager;
    
    private MockWorkflowService mockWorkflowService;
    
    @Mock
    private ConfigurationService mockConfigService;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private ContextManager mockContextManager;
    
    private MockedStatic<ServiceManager> serviceManagerMock;
    private MockedStatic<ContextManager> contextManagerMock;
    
    private static final String TEST_ITEM_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String OPERATION_ID = "test-operation-id";
    private ArgumentCaptor<Map<String, Object>> operationParamsCaptor;
    private ArgumentCaptor<Object> operationResultCaptor;
    private ArgumentCaptor<Throwable> operationExceptionCaptor;
    private ArgumentCaptor<WorkItem> contextItemCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        mockWorkflowService = new MockWorkflowService();
        
        // Set up work items in the mock service
        setupWorkItems();
        
        // Set up mock service manager
        when(mockServiceManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up operation tracking
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn(OPERATION_ID);
        
        // Set up mocked statics
        serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
        serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        
        contextManagerMock = Mockito.mockStatic(ContextManager.class);
        contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
        
        // Set up argument captors
        operationParamsCaptor = ArgumentCaptor.forClass(Map.class);
        operationResultCaptor = ArgumentCaptor.forClass(Object.class);
        operationExceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        contextItemCaptor = ArgumentCaptor.forClass(WorkItem.class);
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        serviceManagerMock.close();
        contextManagerMock.close();
    }
    
    private void setupWorkItems() {
        // Create test work item in progress
        WorkItem testItem = new WorkItem();
        testItem.setId(TEST_ITEM_ID);
        testItem.setTitle("Test Work Item");
        testItem.setDescription("Test Description");
        testItem.setType(WorkItemType.TASK);
        testItem.setPriority(Priority.MEDIUM);
        testItem.setState(WorkflowState.IN_PROGRESS);
        testItem.setAssignee("test.user");
        testItem.setCreated(LocalDateTime.now().minusDays(1));
        testItem.setUpdated(LocalDateTime.now().minusHours(1));
        mockWorkflowService.addItem(testItem);
    }

    @Nested
    @DisplayName("Service Integration Tests")
    class ServiceIntegrationTests {
        
        @Test
        @DisplayName("Should verify the integration between DoneCommand and WorkflowService")
        void shouldVerifyIntegrationBetweenDoneCommandAndWorkflowService() {
            // Given
            DoneCommand command = new DoneCommand();
            command.setItemId(TEST_ITEM_ID);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            assertEquals(1, mockWorkflowService.getCanTransitionCallCount());
            assertEquals(1, mockWorkflowService.getTransitionCallCount());
            assertEquals(TEST_ITEM_ID, mockWorkflowService.getLastTransitionItemId());
            assertEquals(WorkflowState.DONE, mockWorkflowService.getLastTransitionTargetState());
            
            // Verify context manager integration
            verify(mockContextManager).setLastViewedItem(contextItemCaptor.capture());
            assertEquals(TEST_ITEM_ID, contextItemCaptor.getValue().getId());
            assertEquals(WorkflowState.DONE, contextItemCaptor.getValue().getState());
        }
        
        @Test
        @DisplayName("Should verify the integration between DoneCommand and WorkflowService with comment")
        void shouldVerifyIntegrationBetweenDoneCommandAndWorkflowServiceWithComment() {
            // Given
            when(mockConfigService.getCurrentUser()).thenReturn("test.user");
            
            DoneCommand command = new DoneCommand();
            command.setItemId(TEST_ITEM_ID);
            command.setComment("Test completion comment");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            assertEquals(1, mockWorkflowService.getTransitionWithCommentCount());
            assertEquals("Test completion comment", mockWorkflowService.getLastTransitionComment());
            assertEquals("test.user", mockWorkflowService.getLastTransitionUser());
        }
        
        @Test
        @DisplayName("Should verify the integration between DoneCommand and ConfigurationService")
        void shouldVerifyIntegrationBetweenDoneCommandAndConfigurationService() {
            // Given
            when(mockConfigService.getCurrentUser()).thenReturn("configured.user");
            
            DoneCommand command = new DoneCommand();
            command.setItemId(TEST_ITEM_ID);
            command.setComment("Test comment");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockConfigService).getCurrentUser();
            assertEquals("configured.user", mockWorkflowService.getLastTransitionUser());
        }
        
        @Test
        @DisplayName("Should verify the integration between DoneCommand and MetadataService for successful operation")
        void shouldVerifyIntegrationBetweenDoneCommandAndMetadataServiceForSuccessfulOperation() {
            // Given
            DoneCommand command = new DoneCommand();
            command.setItemId(TEST_ITEM_ID);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            verify(mockMetadataService).startOperation(eq("done"), eq("UPDATE"), operationParamsCaptor.capture());
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), operationResultCaptor.capture());
            
            Map<String, Object> params = operationParamsCaptor.getValue();
            Object result = operationResultCaptor.getValue();
            
            assertNotNull(params, "Operation parameters should not be null");
            assertTrue(params.containsKey("itemId"), "Parameters should include itemId");
            assertEquals(TEST_ITEM_ID, params.get("itemId"));
            
            assertTrue(result instanceof Map, "Result should be a Map");
            Map<String, Object> resultMap = (Map<String, Object>) result;
            assertTrue(resultMap.containsKey("itemId"), "Result should include itemId");
            assertTrue(resultMap.containsKey("title"), "Result should include title");
            assertTrue(resultMap.containsKey("newState"), "Result should include newState");
            assertTrue(resultMap.containsKey("completionDate"), "Result should include completionDate");
        }
        
        @Test
        @DisplayName("Should verify the integration between DoneCommand and MetadataService for failed operation")
        void shouldVerifyIntegrationBetweenDoneCommandAndMetadataServiceForFailedOperation() {
            // Given
            DoneCommand command = new DoneCommand();
            command.setItemId("invalid-id"); // Invalid UUID format
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            
            verify(mockMetadataService).startOperation(eq("done"), eq("UPDATE"), operationParamsCaptor.capture());
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), operationExceptionCaptor.capture());
            
            Map<String, Object> params = operationParamsCaptor.getValue();
            Throwable exception = operationExceptionCaptor.getValue();
            
            assertNotNull(params, "Operation parameters should not be null");
            assertTrue(params.containsKey("itemId"), "Parameters should include itemId");
            assertEquals("invalid-id", params.get("itemId"));
            
            assertNotNull(exception, "Exception should not be null");
            assertTrue(exception instanceof IllegalArgumentException, "Exception should be IllegalArgumentException");
        }
    }
    
    @Nested
    @DisplayName("Command Execution Tests")
    class CommandExecutionTests {
        
        @Test
        @DisplayName("Should successfully execute the command with text output")
        void shouldSuccessfullyExecuteCommandWithTextOutput() {
            // Given
            DoneCommand command = new DoneCommand();
            command.setItemId(TEST_ITEM_ID);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            
            assertTrue(output.contains("Work item " + TEST_ITEM_ID + " marked as DONE"));
            assertTrue(output.contains("Title: Test Work Item"));
            assertTrue(output.contains("Updated state: DONE"));
            assertTrue(output.contains("Completion date:"));
        }
        
        @Test
        @DisplayName("Should successfully execute the command with JSON output")
        void shouldSuccessfullyExecuteCommandWithJsonOutput() {
            // Given
            DoneCommand command = new DoneCommand();
            command.setItemId(TEST_ITEM_ID);
            command.setFormat("json");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString().trim();
            
            assertTrue(output.startsWith("{"));
            assertTrue(output.endsWith("}"));
            assertTrue(output.contains("\"itemId\""));
            assertTrue(output.contains("\"title\""));
            assertTrue(output.contains("\"newState\""));
            assertTrue(output.contains("\"completionDate\""));
        }
        
        @Test
        @DisplayName("Should successfully execute the command with verbose JSON output")
        void shouldSuccessfullyExecuteCommandWithVerboseJsonOutput() {
            // Given
            DoneCommand command = new DoneCommand();
            command.setItemId(TEST_ITEM_ID);
            command.setFormat("json");
            command.setVerbose(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString().trim();
            
            assertTrue(output.contains("\"type\""));
            assertTrue(output.contains("\"priority\""));
            assertTrue(output.contains("\"description\""));
        }
        
        @Test
        @DisplayName("Should fail when trying to transition item that cannot be transitioned")
        void shouldFailWhenTryingToTransitionItemThatCannotBeTransitioned() {
            // Given
            WorkItem blockedItem = new WorkItem();
            blockedItem.setId("blocked-item");
            blockedItem.setTitle("Blocked Item");
            blockedItem.setState(WorkflowState.BLOCKED);
            mockWorkflowService.addItem(blockedItem);
            mockWorkflowService.setAllowTransition(false);
            
            DoneCommand command = new DoneCommand();
            command.setItemId("blocked-item");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            String errorOutput = errorCaptor.toString();
            
            assertTrue(errorOutput.contains("Error: Cannot transition work item to DONE state"));
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle invalid item ID format")
        void shouldHandleInvalidItemIdFormat() {
            // Given
            DoneCommand command = new DoneCommand();
            command.setItemId("invalid-id");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            assertTrue(errorCaptor.toString().contains("Error: Invalid work item ID format"));
        }
        
        @Test
        @DisplayName("Should handle missing item ID")
        void shouldHandleMissingItemId() {
            // Given
            DoneCommand command = new DoneCommand();
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            assertTrue(errorCaptor.toString().contains("Error: Work item ID is required"));
        }
        
        @Test
        @DisplayName("Should handle service exception")
        void shouldHandleServiceException() {
            // Given
            mockWorkflowService.setThrowInvalidTransitionException(true);
            
            DoneCommand command = new DoneCommand();
            command.setItemId(TEST_ITEM_ID);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            assertTrue(errorCaptor.toString().contains("Error:"));
            
            // Verify metadata service received the failure
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(InvalidTransitionException.class));
        }
    }
}