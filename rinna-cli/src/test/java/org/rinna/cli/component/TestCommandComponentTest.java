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
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.command.TestCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Component test for the TestCommand class, focusing on:
 * - Proper integration with MetadataService for hierarchical operation tracking
 * - Integration with workflow and item services for state transitions
 * - Proper tester assignment and field updating
 * - Proper error handling and output formatting
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TestCommand Component Tests")
public class TestCommandComponentTest {

    private static final String OPERATION_ID = "op-12345";
    private static final String VERIFY_OP_ID = "verify-op-12345";
    private static final String TRANSITION_OP_ID = "transition-op-12345";
    private static final String ASSIGN_OP_ID = "assign-op-12345";
    private static final String DISPLAY_OP_ID = "display-op-12345";
    private static final String WORK_ITEM_ID = "WI-123";

    @Mock
    private ServiceManager mockServiceManager;

    @Mock
    private MetadataService mockMetadataService;

    @Mock
    private MockItemService mockItemService;

    @Mock
    private MockWorkflowService mockWorkflowService;

    @Mock
    private ConfigurationService mockConfigService;

    @Mock
    private ContextManager mockContextManager;

    private TestCommand testCommand;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        // Configure mock service manager
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);

        // Configure mock metadata service for operation tracking
        when(mockMetadataService.startOperation(eq("test-command"), eq("UPDATE"), anyMap())).thenReturn(OPERATION_ID);
        when(mockMetadataService.startOperation(eq("test-verify-item"), eq("READ"), anyMap())).thenReturn(VERIFY_OP_ID);
        when(mockMetadataService.startOperation(eq("test-transition"), eq("UPDATE"), anyMap())).thenReturn(TRANSITION_OP_ID);
        when(mockMetadataService.startOperation(eq("test-assign-tester"), eq("UPDATE"), anyMap())).thenReturn(ASSIGN_OP_ID);
        when(mockMetadataService.startOperation(eq("test-display-result"), eq("READ"), anyMap())).thenReturn(DISPLAY_OP_ID);

        // Configure mock config service
        when(mockConfigService.getCurrentUser()).thenReturn("testuser");

        // Initialize the command with mocked services
        testCommand = new TestCommand(mockServiceManager);
        
        // Redirect stdout and stderr for output validation
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    // Helper method to create a test work item
    private WorkItem createTestWorkItem(WorkflowState state) {
        WorkItem workItem = new WorkItem();
        workItem.setId(WORK_ITEM_ID);
        workItem.setTitle("Test Work Item");
        workItem.setDescription("This is a test work item for testing the test command");
        workItem.setType(WorkItemType.TASK);
        workItem.setPriority(Priority.MEDIUM);
        workItem.setState(state);
        return workItem;
    }

    @Nested
    @DisplayName("Service Integration Tests")
    class ServiceIntegrationTests {
        
        @Test
        @DisplayName("Should integrate with MetadataService for hierarchical operation tracking")
        void shouldIntegrateWithMetadataServiceForHierarchicalOperationTracking() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                WorkItem workItem = createTestWorkItem(WorkflowState.IN_PROGRESS);
                when(mockItemService.getItem(WORK_ITEM_ID)).thenReturn(workItem);
                
                testCommand.setItemId(WORK_ITEM_ID);
                testCommand.setTester("tester1");
                
                // When
                int exitCode = testCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify main operation tracking
                verify(mockMetadataService).startOperation(eq("test-command"), eq("UPDATE"), any(Map.class));
                
                // Verify item verification operation tracking
                verify(mockMetadataService).startOperation(eq("test-verify-item"), eq("READ"), any(Map.class));
                verify(mockMetadataService).completeOperation(eq(VERIFY_OP_ID), any(Map.class));
                
                // Verify transition operation tracking
                verify(mockMetadataService).startOperation(eq("test-transition"), eq("UPDATE"), any(Map.class));
                verify(mockMetadataService).completeOperation(eq(TRANSITION_OP_ID), any(Map.class));
                
                // Verify assign tester operation tracking
                verify(mockMetadataService).startOperation(eq("test-assign-tester"), eq("UPDATE"), any(Map.class));
                verify(mockMetadataService).completeOperation(eq(ASSIGN_OP_ID), any(Map.class));
                
                // Verify display result operation tracking
                verify(mockMetadataService).startOperation(eq("test-display-result"), eq("READ"), any(Map.class));
                verify(mockMetadataService).completeOperation(eq(DISPLAY_OP_ID), any(Map.class));
                
                // Verify main operation completion
                verify(mockMetadataService).completeOperation(eq(OPERATION_ID), any(Map.class));
            }
        }
        
        @Test
        @DisplayName("Should integrate with WorkflowService for state transitions")
        void shouldIntegrateWithWorkflowServiceForStateTransitions() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                WorkItem workItem = createTestWorkItem(WorkflowState.IN_PROGRESS);
                when(mockItemService.getItem(WORK_ITEM_ID)).thenReturn(workItem);
                
                testCommand.setItemId(WORK_ITEM_ID);
                testCommand.setUsername("testuser");
                
                // When
                int exitCode = testCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify workflow service was called to transition the item
                verify(mockWorkflowService).transition(
                    eq(WORK_ITEM_ID), 
                    eq("testuser"), 
                    eq(WorkflowState.TESTING), 
                    eq("Moved to testing")
                );
                
                // Verify operation parameters
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).startOperation(eq("test-transition"), eq("UPDATE"), paramsCaptor.capture());
                
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals(WORK_ITEM_ID, params.get("itemId"));
                assertEquals("TESTING", params.get("targetState"));
                
                // Verify operation result
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(TRANSITION_OP_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals(true, result.get("success"));
                assertEquals("IN_PROGRESS", result.get("previousState"));
                assertEquals("TESTING", result.get("newState"));
            }
        }
        
        @Test
        @DisplayName("Should integrate with ItemService for tester assignment")
        void shouldIntegrateWithItemServiceForTesterAssignment() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                WorkItem workItem = createTestWorkItem(WorkflowState.IN_PROGRESS);
                when(mockItemService.getItem(WORK_ITEM_ID)).thenReturn(workItem);
                
                testCommand.setItemId(WORK_ITEM_ID);
                testCommand.setTester("tester1");
                
                // When
                int exitCode = testCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify item service was called to update custom fields
                ArgumentCaptor<Map<String, String>> fieldsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockItemService).updateCustomFields(eq(WORK_ITEM_ID), fieldsCaptor.capture());
                
                Map<String, String> fields = fieldsCaptor.getValue();
                assertEquals("tester1", fields.get("tester"));
                assertNotNull(fields.get("lastTestDate"));
                
                // Verify operation parameters
                ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).startOperation(eq("test-assign-tester"), eq("UPDATE"), paramsCaptor.capture());
                
                Map<String, Object> params = paramsCaptor.getValue();
                assertEquals(WORK_ITEM_ID, params.get("itemId"));
                assertEquals("tester1", params.get("tester"));
                
                // Verify operation result
                ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
                verify(mockMetadataService).completeOperation(eq(ASSIGN_OP_ID), resultCaptor.capture());
                
                Map<String, Object> result = resultCaptor.getValue();
                assertEquals(true, result.get("success"));
                assertEquals("tester1", result.get("tester"));
            }
        }
        
        @Test
        @DisplayName("Should integrate with ContextManager to set current item")
        void shouldIntegrateWithContextManagerToSetCurrentItem() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                WorkItem workItem = createTestWorkItem(WorkflowState.IN_PROGRESS);
                when(mockItemService.getItem(WORK_ITEM_ID)).thenReturn(workItem);
                
                testCommand.setItemId(WORK_ITEM_ID);
                
                // When
                int exitCode = testCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify context manager was called to set current item
                verify(mockContextManager).setCurrentItemId(WORK_ITEM_ID);
            }
        }
    }
    
    @Nested
    @DisplayName("Command Functionality Tests")
    class CommandFunctionalityTests {
        
        @Test
        @DisplayName("Should handle work item already in TESTING state")
        void shouldHandleWorkItemAlreadyInTestingState() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                WorkItem workItem = createTestWorkItem(WorkflowState.TESTING);
                when(mockItemService.getItem(WORK_ITEM_ID)).thenReturn(workItem);
                
                testCommand.setItemId(WORK_ITEM_ID);
                
                // When
                int exitCode = testCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify workflow service was NOT called to transition the item
                verify(mockWorkflowService, never()).transition(
                    anyString(), 
                    anyString(), 
                    any(WorkflowState.class), 
                    anyString()
                );
                
                // Verify transition operation was NOT started
                verify(mockMetadataService, never()).startOperation(eq("test-transition"), anyString(), anyMap());
                
                // Verify output contains "already in TESTING state"
                String output = outputStream.toString();
                assertTrue(output.contains("already in TESTING state"));
            }
        }
        
        @Test
        @DisplayName("Should include tester in transition comment when provided")
        void shouldIncludeTesterInTransitionCommentWhenProvided() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                WorkItem workItem = createTestWorkItem(WorkflowState.IN_PROGRESS);
                when(mockItemService.getItem(WORK_ITEM_ID)).thenReturn(workItem);
                
                testCommand.setItemId(WORK_ITEM_ID);
                testCommand.setTester("tester1");
                
                // When
                int exitCode = testCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify workflow service was called with the correct comment
                verify(mockWorkflowService).transition(
                    eq(WORK_ITEM_ID), 
                    eq("testuser"), 
                    eq(WorkflowState.TESTING), 
                    eq("Moved to testing (Tester: tester1)")
                );
            }
        }
        
        @Test
        @DisplayName("Should handle transition without tester assignment")
        void shouldHandleTransitionWithoutTesterAssignment() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                WorkItem workItem = createTestWorkItem(WorkflowState.IN_PROGRESS);
                when(mockItemService.getItem(WORK_ITEM_ID)).thenReturn(workItem);
                
                testCommand.setItemId(WORK_ITEM_ID);
                // No tester set
                
                // When
                int exitCode = testCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify workflow service was called with the correct comment
                verify(mockWorkflowService).transition(
                    eq(WORK_ITEM_ID), 
                    eq("testuser"), 
                    eq(WorkflowState.TESTING), 
                    eq("Moved to testing")
                );
                
                // Verify assign tester operation was NOT started
                verify(mockMetadataService, never()).startOperation(eq("test-assign-tester"), anyString(), anyMap());
                
                // Verify item service was NOT called to update custom fields
                verify(mockItemService, never()).updateCustomFields(anyString(), anyMap());
                
                // Verify output contains "No tester assigned"
                String output = outputStream.toString();
                assertTrue(output.contains("No tester assigned"));
            }
        }
    }
    
    @Nested
    @DisplayName("Output Format Tests")
    class OutputFormatTests {
        
        @Test
        @DisplayName("Should output in text format by default")
        void shouldOutputInTextFormatByDefault() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                WorkItem workItem = createTestWorkItem(WorkflowState.IN_PROGRESS);
                when(mockItemService.getItem(WORK_ITEM_ID)).thenReturn(workItem);
                
                testCommand.setItemId(WORK_ITEM_ID);
                testCommand.setTester("tester1");
                
                // When
                int exitCode = testCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify text output format
                String output = outputStream.toString();
                assertTrue(output.contains("Setting work item WI-123 to testing state"));
                assertTrue(output.contains("Updated state: TESTING"));
                assertTrue(output.contains("Assigned tester: tester1"));
            }
        }
        
        @Test
        @DisplayName("Should output in JSON format when specified")
        void shouldOutputInJsonFormatWhenSpecified() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class);
                 MockedStatic<OutputFormatter> mockedOutputFormatter = Mockito.mockStatic(OutputFormatter.class)) {
                
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                mockedOutputFormatter.when(() -> OutputFormatter.outputObject(anyString(), anyMap()))
                    .thenAnswer(invocation -> {
                        String key = invocation.getArgument(0);
                        Map<String, Object> value = invocation.getArgument(1);
                        System.out.println("{\"" + key + "\":" + value + "}");
                        return null;
                    });
                
                WorkItem workItem = createTestWorkItem(WorkflowState.IN_PROGRESS);
                when(mockItemService.getItem(WORK_ITEM_ID)).thenReturn(workItem);
                
                testCommand.setItemId(WORK_ITEM_ID);
                testCommand.setTester("tester1");
                testCommand.setFormat("json");
                
                // When
                int exitCode = testCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify OutputFormatter was called
                mockedOutputFormatter.verify(() -> OutputFormatter.outputObject(eq("testResult"), any(Map.class)));
            }
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle missing item ID")
        void shouldHandleMissingItemId() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // No item ID set
                
                // When
                int exitCode = testCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify error message
                String error = errorStream.toString();
                assertTrue(error.contains("Work item ID is required"));
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            }
        }
        
        @Test
        @DisplayName("Should handle non-existent work item")
        void shouldHandleNonExistentWorkItem() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                when(mockItemService.getItem(WORK_ITEM_ID)).thenReturn(null);
                
                testCommand.setItemId(WORK_ITEM_ID);
                
                // When
                int exitCode = testCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify error message
                String error = errorStream.toString();
                assertTrue(error.contains("Failed to verify work item"));
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(VERIFY_OP_ID), any(IllegalArgumentException.class));
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            }
        }
        
        @Test
        @DisplayName("Should handle transition failure")
        void shouldHandleTransitionFailure() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                WorkItem workItem = createTestWorkItem(WorkflowState.IN_PROGRESS);
                when(mockItemService.getItem(WORK_ITEM_ID)).thenReturn(workItem);
                doThrow(new RuntimeException("Invalid transition")).when(mockWorkflowService)
                    .transition(anyString(), anyString(), any(WorkflowState.class), anyString());
                
                testCommand.setItemId(WORK_ITEM_ID);
                
                // When
                int exitCode = testCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify error message
                String error = errorStream.toString();
                assertTrue(error.contains("Failed to transition work item"));
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(TRANSITION_OP_ID), any(RuntimeException.class));
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(RuntimeException.class));
            }
        }
        
        @Test
        @DisplayName("Should handle tester assignment failure")
        void shouldHandleTesterAssignmentFailure() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                WorkItem workItem = createTestWorkItem(WorkflowState.IN_PROGRESS);
                when(mockItemService.getItem(WORK_ITEM_ID)).thenReturn(workItem);
                doThrow(new RuntimeException("Update failed")).when(mockItemService)
                    .updateCustomFields(anyString(), anyMap());
                
                testCommand.setItemId(WORK_ITEM_ID);
                testCommand.setTester("tester1");
                
                // When
                int exitCode = testCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify error message
                String error = errorStream.toString();
                assertTrue(error.contains("Failed to assign tester"));
                
                // Verify operation failure was tracked
                verify(mockMetadataService).failOperation(eq(ASSIGN_OP_ID), any(RuntimeException.class));
                verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(RuntimeException.class));
            }
        }
        
        @Test
        @DisplayName("Should handle error with JSON output")
        void shouldHandleErrorWithJsonOutput() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class);
                 MockedStatic<OutputFormatter> mockedOutputFormatter = Mockito.mockStatic(OutputFormatter.class)) {
                
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                mockedOutputFormatter.when(() -> OutputFormatter.outputObject(anyString(), anyMap()))
                    .thenAnswer(invocation -> {
                        String key = invocation.getArgument(0);
                        Map<String, Object> value = invocation.getArgument(1);
                        System.out.println("{\"" + key + "\":" + value + "}");
                        return null;
                    });
                
                testCommand.setFormat("json");
                // No item ID set
                
                // When
                int exitCode = testCommand.call();
                
                // Then
                assertEquals(1, exitCode);
                
                // Verify OutputFormatter was called with error information
                mockedOutputFormatter.verify(() -> OutputFormatter.outputObject(eq("error"), argThat(map -> 
                    map.containsKey("error") && map.containsKey("message"))));
            }
        }
    }
}