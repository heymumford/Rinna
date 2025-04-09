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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.json.JSONObject;
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
import org.rinna.cli.command.LsCommand;
import org.rinna.cli.domain.model.WorkItemRelationshipType;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockHistoryService;
import org.rinna.cli.service.MockHistoryService.HistoryEntryRecord;
import org.rinna.cli.service.MockHistoryService.HistoryEntryType;
import org.rinna.cli.service.MockRelationshipService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Component integration tests for the LsCommand.
 * These tests verify the integration between LsCommand and its services.
 */
@DisplayName("LsCommand Component Integration Tests")
public class LsCommandComponentTest {

    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private ItemService mockItemService;
    
    @Mock
    private MockRelationshipService mockRelationshipService;
    
    @Mock
    private MockHistoryService mockHistoryService;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private ContextManager mockContextManager;
    
    private MockedStatic<ServiceManager> serviceManagerMock;
    private MockedStatic<ContextManager> contextManagerMock;
    private MockedStatic<OutputFormatter> outputFormatterMock;
    
    private static final String TEST_ITEM_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final UUID TEST_UUID = UUID.fromString(TEST_ITEM_ID);
    private static final String OPERATION_ID = "test-operation-id";
    private ArgumentCaptor<Map<String, Object>> operationParamsCaptor;
    private ArgumentCaptor<Map<String, Object>> operationResultCaptor;
    private ArgumentCaptor<Throwable> operationExceptionCaptor;
    private ArgumentCaptor<UUID> contextItemIdCaptor;
    private ArgumentCaptor<Map<String, Object>> jsonDataCaptor;
    private List<WorkItem> testWorkItems;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Create test work items
        testWorkItems = createTestWorkItems();
        
        // Set up mock service manager
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockRelationshipService()).thenReturn(mockRelationshipService);
        when(mockServiceManager.getMockHistoryService()).thenReturn(mockHistoryService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up operation tracking
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn(OPERATION_ID);
        
        // Set up mocked statics
        serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
        serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        
        contextManagerMock = Mockito.mockStatic(ContextManager.class);
        contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
        
        outputFormatterMock = Mockito.mockStatic(OutputFormatter.class);
        outputFormatterMock.when(() -> OutputFormatter.toJson(any(), anyBoolean())).thenAnswer(invocation -> {
            Map<String, Object> jsonData = invocation.getArgument(0);
            jsonDataCaptor.capture();
            return new JSONObject(jsonData).toString(2);
        });
        
        // Set up argument captors
        operationParamsCaptor = ArgumentCaptor.forClass(Map.class);
        operationResultCaptor = ArgumentCaptor.forClass(Map.class);
        operationExceptionCaptor = ArgumentCaptor.forClass(Throwable.class);
        contextItemIdCaptor = ArgumentCaptor.forClass(UUID.class);
        jsonDataCaptor = ArgumentCaptor.forClass(Map.class);
        
        // Default mocks for item service
        when(mockItemService.getAllItems()).thenReturn(testWorkItems);
        when(mockItemService.getItem(TEST_ITEM_ID)).thenReturn(findItemById(TEST_ITEM_ID));
        
        // Default mocks for relationship service
        when(mockRelationshipService.getParentWorkItem(any(UUID.class))).thenReturn(null);
        when(mockRelationshipService.getChildWorkItems(any(UUID.class))).thenReturn(new ArrayList<>());
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Close static mocks
        serviceManagerMock.close();
        contextManagerMock.close();
        outputFormatterMock.close();
    }
    
    private List<WorkItem> createTestWorkItems() {
        List<WorkItem> items = new ArrayList<>();
        
        // Add the test item with specified ID
        WorkItem testItem = new WorkItem();
        testItem.setId(TEST_ITEM_ID);
        testItem.setTitle("Test Work Item");
        testItem.setDescription("Test Description");
        testItem.setType(WorkItemType.TASK);
        testItem.setPriority(Priority.MEDIUM);
        testItem.setState(WorkflowState.IN_PROGRESS);
        testItem.setAssignee("test.user");
        testItem.setProject("Test Project");
        testItem.setCreated(LocalDateTime.now().minusDays(1));
        testItem.setUpdated(LocalDateTime.now().minusHours(1));
        items.add(testItem);
        
        // Add a few more items with different states
        WorkItem item2 = new WorkItem();
        item2.setId(UUID.randomUUID().toString());
        item2.setTitle("Another Work Item");
        item2.setDescription("Another Description");
        item2.setType(WorkItemType.BUG);
        item2.setPriority(Priority.HIGH);
        item2.setState(WorkflowState.OPEN);
        item2.setAssignee("other.user");
        item2.setProject("Test Project");
        item2.setCreated(LocalDateTime.now().minusDays(2));
        item2.setUpdated(LocalDateTime.now().minusHours(2));
        items.add(item2);
        
        WorkItem item3 = new WorkItem();
        item3.setId(UUID.randomUUID().toString());
        item3.setTitle("Completed Work Item");
        item3.setDescription("Completed Description");
        item3.setType(WorkItemType.TASK);
        item3.setPriority(Priority.LOW);
        item3.setState(WorkflowState.DONE);
        item3.setAssignee("test.user");
        item3.setProject("Test Project");
        item3.setCreated(LocalDateTime.now().minusDays(3));
        item3.setUpdated(LocalDateTime.now().minusHours(3));
        items.add(item3);
        
        return items;
    }
    
    private WorkItem findItemById(String id) {
        for (WorkItem item : testWorkItems) {
            if (item.getId().equals(id)) {
                return item;
            }
        }
        return null;
    }
    
    private List<HistoryEntryRecord> createMockHistory(UUID itemId) {
        List<HistoryEntryRecord> history = new ArrayList<>();
        
        // Add state change history entry
        history.add(new HistoryEntryRecord(
            itemId, 
            HistoryEntryType.STATE_CHANGE, 
            "test.user", 
            "State changed from NEW to OPEN", 
            null, 
            Instant.now().minus(3, ChronoUnit.DAYS)
        ));
        
        // Add field change history entry
        history.add(new HistoryEntryRecord(
            itemId, 
            HistoryEntryType.FIELD_CHANGE, 
            "test.user", 
            "Field 'priority' changed from 'LOW' to 'MEDIUM'", 
            "Priority", 
            Instant.now().minus(2, ChronoUnit.DAYS)
        ));
        
        // Add assignment history entry
        history.add(new HistoryEntryRecord(
            itemId, 
            HistoryEntryType.ASSIGNMENT, 
            "admin", 
            "Assignment changed from 'unassigned' to 'test.user'", 
            null, 
            Instant.now().minus(1, ChronoUnit.DAYS)
        ));
        
        return history;
    }

    @Nested
    @DisplayName("Service Integration Tests")
    class ServiceIntegrationTests {
        
        @Test
        @DisplayName("Should verify the integration between LsCommand and ItemService for listing all items")
        void shouldVerifyIntegrationBetweenLsCommandAndItemServiceForListingAllItems() {
            // Given
            LsCommand command = new LsCommand();
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockItemService).getAllItems();
            verify(mockMetadataService).startOperation(eq("ls"), eq("READ"), any());
        }
        
        @Test
        @DisplayName("Should verify the integration between LsCommand and ItemService for a specific item")
        void shouldVerifyIntegrationBetweenLsCommandAndItemServiceForASpecificItem() {
            // Given
            LsCommand command = new LsCommand();
            command.setItemId(TEST_ITEM_ID);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockItemService).getItem(TEST_ITEM_ID);
            verify(mockMetadataService).startOperation(eq("ls"), eq("READ"), any());
            verify(mockContextManager).setLastViewedWorkItem(TEST_UUID);
        }
        
        @Test
        @DisplayName("Should verify the integration between LsCommand and RelationshipService")
        void shouldVerifyIntegrationBetweenLsCommandAndRelationshipService() {
            // Given
            UUID parentId = UUID.randomUUID();
            List<UUID> childIds = List.of(UUID.randomUUID(), UUID.randomUUID());
            
            when(mockRelationshipService.getParentWorkItem(TEST_UUID)).thenReturn(parentId);
            when(mockRelationshipService.getRelationshipType(eq(TEST_UUID), eq(parentId))).thenReturn(WorkItemRelationshipType.PARENT_CHILD);
            when(mockRelationshipService.getChildWorkItems(TEST_UUID)).thenReturn(childIds);
            
            LsCommand command = new LsCommand();
            command.setItemId(TEST_ITEM_ID);
            command.setLongFormat(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockRelationshipService).getParentWorkItem(TEST_UUID);
            verify(mockRelationshipService).getRelationshipType(TEST_UUID, parentId);
            verify(mockRelationshipService).getChildWorkItems(TEST_UUID);
        }
        
        @Test
        @DisplayName("Should verify the integration between LsCommand and HistoryService")
        void shouldVerifyIntegrationBetweenLsCommandAndHistoryService() {
            // Given
            List<HistoryEntryRecord> mockHistory = createMockHistory(TEST_UUID);
            when(mockHistoryService.getHistory(TEST_UUID)).thenReturn(mockHistory);
            
            LsCommand command = new LsCommand();
            command.setItemId(TEST_ITEM_ID);
            command.setLongFormat(true);
            command.setAllFormat(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockHistoryService).getHistory(TEST_UUID);
        }
        
        @Test
        @DisplayName("Should verify the integration between LsCommand and ContextManager")
        void shouldVerifyIntegrationBetweenLsCommandAndContextManager() {
            // Given
            LsCommand command = new LsCommand();
            command.setItemId(TEST_ITEM_ID);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockContextManager).setLastViewedWorkItem(TEST_UUID);
        }
        
        @Test
        @DisplayName("Should verify the integration between LsCommand and MetadataService for operation tracking")
        void shouldVerifyIntegrationBetweenLsCommandAndMetadataServiceForOperationTracking() {
            // Given
            LsCommand command = new LsCommand();
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            verify(mockMetadataService).startOperation(eq("ls"), eq("READ"), operationParamsCaptor.capture());
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), operationResultCaptor.capture());
            
            Map<String, Object> params = operationParamsCaptor.getValue();
            Map<String, Object> result = operationResultCaptor.getValue();
            
            // Verify operation parameters
            assertEquals(null, params.get("item_id"));
            assertEquals(false, params.get("long_format"));
            assertEquals(false, params.get("all_format"));
            assertEquals("text", params.get("format"));
            assertEquals(false, params.get("verbose"));
            
            // Verify operation result
            assertEquals(testWorkItems.size(), result.get("items_found"));
            assertEquals("text", result.get("format"));
            assertEquals(false, result.get("long_format"));
        }
        
        @Test
        @DisplayName("Should verify error handling when item is not found")
        void shouldVerifyErrorHandlingWhenItemIsNotFound() {
            // Given
            String nonExistentId = "non-existent-id";
            when(mockItemService.getItem(nonExistentId)).thenReturn(null);
            
            LsCommand command = new LsCommand();
            command.setItemId(nonExistentId);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            assertTrue(errorCaptor.toString().contains("Work item not found"));
        }
        
        @Test
        @DisplayName("Should verify error handling with invalid UUID format")
        void shouldVerifyErrorHandlingWithInvalidUuidFormat() {
            // Given
            String invalidId = "invalid-uuid-format";
            when(mockItemService.getItem(invalidId)).thenThrow(new IllegalArgumentException("Invalid UUID format"));
            
            LsCommand command = new LsCommand();
            command.setItemId(invalidId);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
            assertTrue(errorCaptor.toString().contains("Invalid work item ID format"));
        }
    }
    
    @Nested
    @DisplayName("Output Format Tests")
    class OutputFormatTests {
        
        @Test
        @DisplayName("Should output in text format by default")
        void shouldOutputInTextFormatByDefault() {
            // Given
            LsCommand command = new LsCommand();
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            assertTrue(output.contains("ID"));
            assertTrue(output.contains("Title"));
            assertTrue(output.contains("State"));
            assertTrue(output.contains(TEST_ITEM_ID));
        }
        
        @Test
        @DisplayName("Should output in detailed text format with long option")
        void shouldOutputInDetailedTextFormatWithLongOption() {
            // Given
            LsCommand command = new LsCommand();
            command.setLongFormat(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            String output = outputCaptor.toString();
            assertTrue(output.contains("Detailed Work Item Listing"));
            assertTrue(output.contains("Work Item:"));
            assertTrue(output.contains("Description:"));
        }
        
        @Test
        @DisplayName("Should output in JSON format when specified")
        void shouldOutputInJsonFormatWhenSpecified() {
            // Given
            LsCommand command = new LsCommand();
            command.setFormat("json");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            outputFormatterMock.verify(() -> OutputFormatter.toJson(any(), eq(false)));
        }
        
        @Test
        @DisplayName("Should output in verbose JSON format when specified")
        void shouldOutputInVerboseJsonFormatWhenSpecified() {
            // Given
            LsCommand command = new LsCommand();
            command.setFormat("json");
            command.setVerbose(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            outputFormatterMock.verify(() -> OutputFormatter.toJson(any(), eq(true)));
        }
    }
    
    @Nested
    @DisplayName("Command Option Tests")
    class CommandOptionTests {
        
        @Test
        @DisplayName("Should handle long format option")
        void shouldHandleLongFormatOption() {
            // Given
            LsCommand command = new LsCommand();
            command.setLongFormat(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockMetadataService).startOperation(eq("ls"), eq("READ"), operationParamsCaptor.capture());
            Map<String, Object> params = operationParamsCaptor.getValue();
            assertEquals(true, params.get("long_format"));
        }
        
        @Test
        @DisplayName("Should handle all format option")
        void shouldHandleAllFormatOption() {
            // Given
            LsCommand command = new LsCommand();
            command.setAllFormat(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockMetadataService).startOperation(eq("ls"), eq("READ"), operationParamsCaptor.capture());
            Map<String, Object> params = operationParamsCaptor.getValue();
            assertEquals(true, params.get("all_format"));
        }
        
        @Test
        @DisplayName("Should handle format option")
        void shouldHandleFormatOption() {
            // Given
            LsCommand command = new LsCommand();
            command.setFormat("json");
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockMetadataService).startOperation(eq("ls"), eq("READ"), operationParamsCaptor.capture());
            Map<String, Object> params = operationParamsCaptor.getValue();
            assertEquals("json", params.get("format"));
        }
        
        @Test
        @DisplayName("Should handle verbose option")
        void shouldHandleVerboseOption() {
            // Given
            LsCommand command = new LsCommand();
            command.setVerbose(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockMetadataService).startOperation(eq("ls"), eq("READ"), operationParamsCaptor.capture());
            Map<String, Object> params = operationParamsCaptor.getValue();
            assertEquals(true, params.get("verbose"));
        }
        
        @Test
        @DisplayName("Should handle combination of options")
        void shouldHandleCombinationOfOptions() {
            // Given
            LsCommand command = new LsCommand();
            command.setLongFormat(true);
            command.setAllFormat(true);
            command.setFormat("json");
            command.setVerbose(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            verify(mockMetadataService).startOperation(eq("ls"), eq("READ"), operationParamsCaptor.capture());
            Map<String, Object> params = operationParamsCaptor.getValue();
            assertEquals(true, params.get("long_format"));
            assertEquals(true, params.get("all_format"));
            assertEquals("json", params.get("format"));
            assertEquals(true, params.get("verbose"));
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle exception thrown by ItemService")
        void shouldHandleExceptionThrownByItemService() {
            // Given
            RuntimeException testException = new RuntimeException("Test exception");
            when(mockItemService.getAllItems()).thenThrow(testException);
            
            LsCommand command = new LsCommand();
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), eq(testException));
            assertTrue(errorCaptor.toString().contains("Error: Error listing work items"));
        }
        
        @Test
        @DisplayName("Should handle exception with verbose flag enabled")
        void shouldHandleExceptionWithVerboseFlagEnabled() {
            // Given
            RuntimeException testException = new RuntimeException("Test exception");
            when(mockItemService.getAllItems()).thenThrow(testException);
            
            LsCommand command = new LsCommand();
            command.setVerbose(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(1, exitCode);
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), eq(testException));
            assertTrue(errorCaptor.toString().contains("Error: Error listing work items"));
            // With verbose flag, stack trace would be printed
        }
    }
    
    @Nested
    @DisplayName("Special Case Tests")
    class SpecialCaseTests {
        
        @Test
        @DisplayName("Should handle empty work item list")
        void shouldHandleEmptyWorkItemList() {
            // Given
            when(mockItemService.getAllItems()).thenReturn(new ArrayList<>());
            
            LsCommand command = new LsCommand();
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            assertTrue(outputCaptor.toString().contains("No work items found"));
            
            // Verify operation tracking
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), operationResultCaptor.capture());
            Map<String, Object> result = operationResultCaptor.getValue();
            assertEquals(0, result.get("items_found"));
        }
        
        @Test
        @DisplayName("Should update context with last viewed item when using long format")
        void shouldUpdateContextWithLastViewedItemWhenUsingLongFormat() {
            // Given
            LsCommand command = new LsCommand();
            command.setLongFormat(true);
            
            // When
            int exitCode = command.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // The last item in the list should be set as last viewed
            WorkItem lastItem = testWorkItems.get(testWorkItems.size() - 1);
            verify(mockContextManager).setLastViewedWorkItem(UUID.fromString(lastItem.getId()));
        }
    }
}