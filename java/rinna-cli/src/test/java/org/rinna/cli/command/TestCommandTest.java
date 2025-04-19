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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;

/**
 * Test class for TestCommand with focus on MetadataService integration.
 * Tests follow the ViewCommand pattern test approach.
 */
@ExtendWith(MockitoExtension.class)
public class TestCommandTest {

    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private MockItemService mockItemService;
    
    @Mock
    private MockWorkflowService mockWorkflowService;
    
    @Mock
    private ConfigurationService mockConfigService;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private ContextManager mockContextManager;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> resultCaptor;
    
    @Captor
    private ArgumentCaptor<Map<String, String>> customFieldsCaptor;
    
    private TestCommand command;
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private static final String TEST_ITEM_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_USER = "testuser";
    private static final String TEST_TESTER = "testertestuser";
    private static final String MOCK_OPERATION_ID = "op-123";
    
    private WorkItem testWorkItem;
    
    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Setup mock service dependencies
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
        
        // Setup metadata service tracking
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn(MOCK_OPERATION_ID);
        
        // Setup default work item
        testWorkItem = new WorkItem();
        testWorkItem.setId(TEST_ITEM_ID);
        testWorkItem.setTitle("Test Work Item");
        testWorkItem.setState(WorkflowState.IN_PROGRESS);
        
        // Create the command with mocked dependencies
        command = new TestCommand(mockServiceManager);
        command.setUsername(TEST_USER);
        
        // Reset output streams for each test
        outContent.reset();
        errContent.reset();
    }
    
    /**
     * Test successful operation tracking.
     */
    @Nested
    @DisplayName("Operation Tracking Tests")
    class OperationTrackingTests {
        
        @Test
        @DisplayName("Should track main operation when setting a work item to testing")
        void shouldTrackMainOperationWhenSettingWorkItemToTesting() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            when(mockItemService.getItem(TEST_ITEM_ID)).thenReturn(testWorkItem);
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Verify main operation tracking
            verify(mockMetadataService).startOperation(eq("test-command"), eq("UPDATE"), paramsCaptor.capture());
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(TEST_USER, params.get("username"));
            assertEquals(TEST_ITEM_ID, params.get("itemId"));
            assertEquals("text", params.get("format"));
            assertEquals(false, params.get("verbose"));
            
            // Verify operation completion tracking
            verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
            Map<String, Object> result_data = resultCaptor.getValue();
            assertTrue((Boolean) result_data.get("success"));
            assertEquals(TEST_ITEM_ID, result_data.get("itemId"));
            assertEquals("TESTING", result_data.get("state"));
        }
        
        @Test
        @DisplayName("Should track hierarchical operations when verifying work item")
        void shouldTrackHierarchicalOperationsWhenVerifyingWorkItem() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            when(mockItemService.getItem(TEST_ITEM_ID)).thenReturn(testWorkItem);
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Verify verify-item sub-operation tracking
            verify(mockMetadataService).startOperation(eq("test-verify-item"), eq("READ"), paramsCaptor.capture());
            Map<String, Object> verifyParams = paramsCaptor.getValue();
            assertEquals(TEST_ITEM_ID, verifyParams.get("itemId"));
            
            // Verify verify-item operation completion
            verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
            Map<String, Object> verifyResult = resultCaptor.getValue();
            assertTrue((Boolean) verifyResult.containsKey("itemExists"));
            assertTrue((Boolean) verifyResult.get("itemExists"));
            assertEquals(testWorkItem.getState().toString(), verifyResult.get("currentState"));
        }
        
        @Test
        @DisplayName("Should track transition operation when setting to testing state")
        void shouldTrackTransitionOperationWhenSettingToTestingState() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            when(mockItemService.getItem(TEST_ITEM_ID)).thenReturn(testWorkItem);
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Verify transition sub-operation tracking
            verify(mockMetadataService).startOperation(eq("test-transition"), eq("UPDATE"), paramsCaptor.capture());
            Map<String, Object> transitionParams = paramsCaptor.getValue();
            assertEquals(TEST_ITEM_ID, transitionParams.get("itemId"));
            assertEquals("TESTING", transitionParams.get("targetState"));
            
            // Verify workflow service was called to make the transition
            verify(mockWorkflowService).transition(
                eq(TEST_ITEM_ID), 
                eq(TEST_USER), 
                eq(WorkflowState.TESTING), 
                eq("Moved to testing")
            );
            
            // Verify transition operation completion
            verify(mockMetadataService, atLeastOnce()).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
        }
        
        @Test
        @DisplayName("Should track tester assignment operation when tester is provided")
        void shouldTrackTesterAssignmentOperationWhenTesterIsProvided() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            command.setTester(TEST_TESTER);
            when(mockItemService.getItem(TEST_ITEM_ID)).thenReturn(testWorkItem);
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Verify assign-tester sub-operation tracking
            verify(mockMetadataService).startOperation(eq("test-assign-tester"), eq("UPDATE"), paramsCaptor.capture());
            Map<String, Object> assignParams = paramsCaptor.getValue();
            assertEquals(TEST_ITEM_ID, assignParams.get("itemId"));
            assertEquals(TEST_TESTER, assignParams.get("tester"));
            
            // Verify custom fields were updated
            verify(mockItemService).updateCustomFields(eq(TEST_ITEM_ID), customFieldsCaptor.capture());
            Map<String, String> customFields = customFieldsCaptor.getValue();
            assertEquals(TEST_TESTER, customFields.get("tester"));
            assertNotNull(customFields.get("lastTestDate"));
            
            // Verify operation completion tracking
            verify(mockMetadataService, atLeastOnce()).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
        }
        
        @Test
        @DisplayName("Should track display operation when showing result")
        void shouldTrackDisplayOperationWhenShowingResult() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            when(mockItemService.getItem(TEST_ITEM_ID)).thenReturn(testWorkItem);
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Verify display-result sub-operation tracking
            verify(mockMetadataService).startOperation(eq("test-display-result"), eq("READ"), paramsCaptor.capture());
            Map<String, Object> displayParams = paramsCaptor.getValue();
            assertEquals(TEST_ITEM_ID, displayParams.get("itemId"));
            assertEquals("text", displayParams.get("format"));
            
            // Verify display operation completion
            verify(mockMetadataService, atLeastOnce()).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
        }
        
        @Test
        @DisplayName("Should track operation failure when work item validation fails")
        void shouldTrackOperationFailureWhenWorkItemValidationFails() {
            // Setup - missing item ID
            // No item ID set
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(1, result);
            
            // Verify operation failure tracking
            verify(mockMetadataService).startOperation(eq("test-command"), eq("UPDATE"), any());
            verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalArgumentException.class));
            
            // Check error output
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("Error: Work item ID is required"));
        }
        
        @Test
        @DisplayName("Should track operation failure when work item cannot be found")
        void shouldTrackOperationFailureWhenWorkItemCannotBeFound() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            when(mockItemService.getItem(TEST_ITEM_ID)).thenReturn(null);
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(1, result);
            
            // Verify verify-item operation failure tracking
            verify(mockMetadataService).startOperation(eq("test-verify-item"), eq("READ"), any());
            verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalArgumentException.class));
            
            // Check error output
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("Error: Failed to verify work item"));
        }
        
        @Test
        @DisplayName("Should track operation failure when transition fails")
        void shouldTrackOperationFailureWhenTransitionFails() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            when(mockItemService.getItem(TEST_ITEM_ID)).thenReturn(testWorkItem);
            doThrow(new RuntimeException("Invalid transition")).when(mockWorkflowService)
                .transition(eq(TEST_ITEM_ID), eq(TEST_USER), eq(WorkflowState.TESTING), anyString());
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(1, result);
            
            // Verify transition operation failure tracking
            verify(mockMetadataService).startOperation(eq("test-transition"), eq("UPDATE"), any());
            verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(RuntimeException.class));
            
            // Check error output
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("Error: Failed to transition work item"));
        }
        
        @Test
        @DisplayName("Should track operation failure when tester assignment fails")
        void shouldTrackOperationFailureWhenTesterAssignmentFails() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            command.setTester(TEST_TESTER);
            when(mockItemService.getItem(TEST_ITEM_ID)).thenReturn(testWorkItem);
            doThrow(new RuntimeException("Failed to update custom fields")).when(mockItemService)
                .updateCustomFields(eq(TEST_ITEM_ID), any());
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(1, result);
            
            // Verify assign-tester operation failure tracking
            verify(mockMetadataService).startOperation(eq("test-assign-tester"), eq("UPDATE"), any());
            verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(RuntimeException.class));
            
            // Check error output
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("Error: Failed to assign tester"));
        }
    }
    
    /**
     * Test JSON output formatting.
     */
    @Nested
    @DisplayName("JSON Output Tests")
    class JsonOutputTests {
        
        @Test
        @DisplayName("Should output JSON when format is set to json")
        void shouldOutputJsonWhenFormatIsSetToJson() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            command.setFormat("json");
            when(mockItemService.getItem(TEST_ITEM_ID)).thenReturn(testWorkItem);
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Check JSON output
            String output = outContent.toString();
            assertTrue(output.contains("\"success\": true"));
            assertTrue(output.contains("\"itemId\": \"" + TEST_ITEM_ID + "\""));
            assertTrue(output.contains("\"state\": \"TESTING\""));
            assertTrue(output.contains("\"changed\": true"));
        }
        
        @Test
        @DisplayName("Should include tester info in JSON output when tester is provided")
        void shouldIncludeTesterInfoInJsonOutputWhenTesterIsProvided() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            command.setTester(TEST_TESTER);
            command.setFormat("json");
            when(mockItemService.getItem(TEST_ITEM_ID)).thenReturn(testWorkItem);
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Check JSON output
            String output = outContent.toString();
            assertTrue(output.contains("\"success\": true"));
            assertTrue(output.contains("\"itemId\": \"" + TEST_ITEM_ID + "\""));
            assertTrue(output.contains("\"tester\": \"" + TEST_TESTER + "\""));
            assertTrue(output.contains("\"lastTestDate\": \"" + LocalDate.now().toString() + "\""));
        }
        
        @Test
        @DisplayName("Should output JSON error when validation fails")
        void shouldOutputJsonErrorWhenValidationFails() {
            // Setup - missing item ID
            command.setFormat("json");
            // No item ID set
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(1, result);
            
            // Check JSON error output
            String output = outContent.toString();
            assertTrue(output.contains("\"error\": \"Work item ID is required\""));
        }
    }
    
    /**
     * Test special workflow behaviors.
     */
    @Nested
    @DisplayName("Workflow Behavior Tests")
    class WorkflowBehaviorTests {
        
        @Test
        @DisplayName("Should skip transition if work item is already in testing state")
        void shouldSkipTransitionIfWorkItemIsAlreadyInTestingState() {
            // Setup
            testWorkItem.setState(WorkflowState.TESTING);
            command.setItemId(TEST_ITEM_ID);
            when(mockItemService.getItem(TEST_ITEM_ID)).thenReturn(testWorkItem);
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Verify transition operation was not called
            verify(mockWorkflowService, never()).transition(anyString(), anyString(), any(WorkflowState.class), anyString());
            
            // Check output
            String output = outContent.toString();
            assertTrue(output.contains("Work item already in TESTING state"));
        }
        
        @Test
        @DisplayName("Should include tester in transition comment when tester is provided")
        void shouldIncludeTesterInTransitionCommentWhenTesterIsProvided() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            command.setTester(TEST_TESTER);
            when(mockItemService.getItem(TEST_ITEM_ID)).thenReturn(testWorkItem);
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Verify workflow transition was called with correct comment
            verify(mockWorkflowService).transition(
                eq(TEST_ITEM_ID), 
                eq(TEST_USER), 
                eq(WorkflowState.TESTING), 
                eq("Moved to testing (Tester: " + TEST_TESTER + ")")
            );
        }
        
        @Test
        @DisplayName("Should set context current item ID after successful operation")
        void shouldSetContextCurrentItemIdAfterSuccessfulOperation() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            when(mockItemService.getItem(TEST_ITEM_ID)).thenReturn(testWorkItem);
            
            // Mock context manager to verify interaction
            // Need to inject contextManager into command
            TestCommand spyCommand = spy(command);
            try {
                // Use reflection to access the private contextManager field
                java.lang.reflect.Field contextManagerField = TestCommand.class.getDeclaredField("contextManager");
                contextManagerField.setAccessible(true);
                contextManagerField.set(spyCommand, mockContextManager);
            } catch (Exception e) {
                fail("Failed to set mockContextManager: " + e.getMessage());
            }
            
            // Execute
            int result = spyCommand.call();
            
            // Verify
            assertEquals(0, result);
            
            // Verify context manager was called to set current item ID
            verify(mockContextManager).setCurrentItemId(TEST_ITEM_ID);
        }
    }
}