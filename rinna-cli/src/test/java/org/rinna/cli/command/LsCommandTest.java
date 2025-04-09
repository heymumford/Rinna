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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.rinna.cli.domain.model.WorkItemRelationshipType;
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

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test class for LsCommand operation tracking and output.
 */
@DisplayName("LsCommand Unit Tests")
class LsCommandTest {

    private static final String MOCK_OPERATION_ID = "mock-operation-id";
    private static final String TEST_ITEM_ID = "123e4567-e89b-12d3-a456-426614174000";
    
    private LsCommand command;
    private ServiceManager mockServiceManager;
    private MetadataService mockMetadataService;
    private ItemService mockItemService;
    private MockRelationshipService mockRelationshipService;
    private MockHistoryService mockHistoryService;
    private ContextManager mockContextManager;
    
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    private ArgumentCaptor<Map<String, Object>> resultCaptor;
    
    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    @BeforeEach
    void setUp() {
        // Set up mocks
        mockServiceManager = mock(ServiceManager.class);
        mockMetadataService = mock(MetadataService.class);
        mockItemService = mock(ItemService.class);
        mockRelationshipService = mock(MockRelationshipService.class);
        mockHistoryService = mock(MockHistoryService.class);
        mockContextManager = mock(ContextManager.class);
        
        // Configure mocks
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockRelationshipService()).thenReturn(mockRelationshipService);
        when(mockServiceManager.getMockHistoryService()).thenReturn(mockHistoryService);
        
        // Set up argument captors
        paramsCaptor = ArgumentCaptor.forClass(Map.class);
        resultCaptor = ArgumentCaptor.forClass(Map.class);
        
        // Set up operation ID for metadata service
        when(mockMetadataService.startOperation(eq("ls"), anyString(), any())).thenReturn(MOCK_OPERATION_ID);
        
        // Mock the ContextManager
        try (var mockedStatic = Mockito.mockStatic(ContextManager.class)) {
            mockedStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
            
            // Create command with mocked service manager
            command = new LsCommand(mockServiceManager);
        }
        
        // Capture stdout and stderr
        originalOut = System.out;
        originalErr = System.err;
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }
    
    @AfterEach
    void tearDown() {
        // Restore original stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Test
    @DisplayName("Should track main operation parameters when listing all items")
    void shouldTrackMainOperationParametersWhenListingAllItems() {
        // Setup
        List<WorkItem> mockItems = new ArrayList<>();
        when(mockItemService.getAllItems()).thenReturn(mockItems);
        
        // Execute
        command.call();
        
        // Verify operation tracking
        verify(mockMetadataService).startOperation(eq("ls"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify parameters
        assertEquals(null, params.get("item_id"));
        assertEquals(false, params.get("long_format"));
        assertEquals(false, params.get("all_format"));
        assertEquals("text", params.get("format"));
        assertEquals(false, params.get("verbose"));
    }
    
    @Test
    @DisplayName("Should track operation completion with result count when listing all items")
    void shouldTrackOperationCompletionWithResultCount() {
        // Setup
        List<WorkItem> mockItems = createMockWorkItems(3);
        when(mockItemService.getAllItems()).thenReturn(mockItems);
        
        // Execute
        int result = command.call();
        
        // Verify result
        assertEquals(0, result);
        
        // Verify operation completion
        verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
        Map<String, Object> resultData = resultCaptor.getValue();
        
        // Verify result data
        assertEquals(3, resultData.get("items_found"));
        assertEquals("text", resultData.get("format"));
        assertEquals(false, resultData.get("long_format"));
    }
    
    @Test
    @DisplayName("Should track operation failure when item service throws an exception")
    void shouldTrackOperationFailureWhenItemServiceThrowsException() {
        // Setup
        RuntimeException testException = new RuntimeException("Test exception");
        when(mockItemService.getAllItems()).thenThrow(testException);
        command.setVerbose(true);
        
        // Execute
        int result = command.call();
        
        // Verify result
        assertEquals(1, result);
        
        // Verify operation failure tracking
        verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), eq(testException));
        
        // Verify error output
        String errorOutput = errorStream.toString();
        assertTrue(errorOutput.contains("Error: Error listing work items: Test exception"));
    }
    
    @Test
    @DisplayName("Should track single item lookup operation")
    void shouldTrackSingleItemLookupOperation() {
        // Setup
        UUID itemId = UUID.fromString(TEST_ITEM_ID);
        WorkItem mockItem = createMockWorkItem(itemId.toString(), "Test Item", WorkflowState.CREATED);
        when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
        command.setItemId(itemId.toString());
        
        // Execute
        int result = command.call();
        
        // Verify result
        assertEquals(0, result);
        
        // Verify operation parameters
        verify(mockMetadataService).startOperation(eq("ls"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals(itemId.toString(), params.get("item_id"));
        
        // Verify operation completion with item details
        verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
        Map<String, Object> resultData = resultCaptor.getValue();
        assertEquals(itemId.toString(), resultData.get("item_id"));
        assertEquals("Test Item", resultData.get("title"));
        assertEquals("OPEN", resultData.get("status"));
        
        // Verify context update
        verify(mockContextManager).setLastViewedWorkItem(eq(itemId));
    }
    
    @Test
    @DisplayName("Should track operation failure when item is not found")
    void shouldTrackOperationFailureWhenItemIsNotFound() {
        // Setup
        String nonExistentItemId = TEST_ITEM_ID;
        when(mockItemService.getItem(nonExistentItemId)).thenReturn(null);
        command.setItemId(nonExistentItemId);
        
        // Execute
        int result = command.call();
        
        // Verify result
        assertEquals(1, result);
        
        // Verify operation failure tracking with appropriate error
        verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalArgumentException.class));
        
        // Verify error output
        String errorOutput = errorStream.toString();
        assertTrue(errorOutput.contains("Error: Work item not found: " + nonExistentItemId));
    }
    
    @Test
    @DisplayName("Should track operation failure for invalid item ID format")
    void shouldTrackOperationFailureForInvalidItemIdFormat() {
        // Setup
        String invalidItemId = "invalid-uuid";
        when(mockItemService.getItem(invalidItemId)).thenThrow(new IllegalArgumentException("Invalid UUID format"));
        command.setItemId(invalidItemId);
        
        // Execute
        int result = command.call();
        
        // Verify result
        assertEquals(1, result);
        
        // Verify operation failure tracking with appropriate error
        verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalArgumentException.class));
        
        // Verify error output
        String errorOutput = errorStream.toString();
        assertTrue(errorOutput.contains("Error: Invalid work item ID format: " + invalidItemId));
    }
    
    @Nested
    @DisplayName("Format Option Tests")
    class FormatOptionTests {
        
        @Test
        @DisplayName("Should track long format option in operation parameters")
        void shouldTrackLongFormatOptionInOperationParameters() {
            // Setup
            List<WorkItem> mockItems = createMockWorkItems(2);
            when(mockItemService.getAllItems()).thenReturn(mockItems);
            command.setLongFormat(true);
            
            // Execute
            command.call();
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("ls"), eq("READ"), paramsCaptor.capture());
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(true, params.get("long_format"));
            
            // Verify result tracking
            verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
            Map<String, Object> resultData = resultCaptor.getValue();
            assertEquals(true, resultData.get("long_format"));
        }
        
        @Test
        @DisplayName("Should track all format option in operation parameters")
        void shouldTrackAllFormatOptionInOperationParameters() {
            // Setup
            List<WorkItem> mockItems = createMockWorkItems(2);
            when(mockItemService.getAllItems()).thenReturn(mockItems);
            command.setAllFormat(true);
            
            // Execute
            command.call();
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("ls"), eq("READ"), paramsCaptor.capture());
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(true, params.get("all_format"));
        }
        
        @Test
        @DisplayName("Should track JSON format option in operation parameters")
        void shouldTrackJsonFormatOptionInOperationParameters() {
            // Setup
            List<WorkItem> mockItems = createMockWorkItems(2);
            when(mockItemService.getAllItems()).thenReturn(mockItems);
            command.setFormat("json");
            
            // Execute
            command.call();
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("ls"), eq("READ"), paramsCaptor.capture());
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals("json", params.get("format"));
            
            // Verify result tracking
            verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
            Map<String, Object> resultData = resultCaptor.getValue();
            assertEquals("json", resultData.get("format"));
        }
    }
    
    @Nested
    @DisplayName("Relationship Tests")
    class RelationshipTests {
        
        @Test
        @DisplayName("Should include parent relationship in operation tracking")
        void shouldIncludeParentRelationshipInOperationTracking() {
            // Setup
            UUID itemId = UUID.fromString(TEST_ITEM_ID);
            UUID parentId = UUID.randomUUID();
            WorkItem mockItem = createMockWorkItem(itemId.toString(), "Test Item", WorkflowState.CREATED);
            
            when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
            when(mockRelationshipService.getParentWorkItem(itemId)).thenReturn(parentId);
            when(mockRelationshipService.getRelationshipType(itemId, parentId))
                .thenReturn(WorkItemRelationshipType.PARENT_CHILD);
            when(mockRelationshipService.getChildWorkItems(itemId)).thenReturn(new ArrayList<>());
            
            command.setItemId(itemId.toString());
            command.setLongFormat(true);
            
            // Execute
            command.call();
            
            // Verify output contains parent relationship
            String output = outputStream.toString();
            assertTrue(output.contains("Parent: " + parentId + " (PARENT_CHILD)"));
        }
        
        @Test
        @DisplayName("Should include child relationships in operation tracking")
        void shouldIncludeChildRelationshipsInOperationTracking() {
            // Setup
            UUID itemId = UUID.fromString(TEST_ITEM_ID);
            UUID childId1 = UUID.randomUUID();
            UUID childId2 = UUID.randomUUID();
            List<UUID> children = List.of(childId1, childId2);
            WorkItem mockItem = createMockWorkItem(itemId.toString(), "Test Item", WorkflowState.CREATED);
            
            when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
            when(mockRelationshipService.getParentWorkItem(itemId)).thenReturn(null);
            when(mockRelationshipService.getChildWorkItems(itemId)).thenReturn(children);
            
            command.setItemId(itemId.toString());
            command.setLongFormat(true);
            
            // Execute
            command.call();
            
            // Verify output contains child relationships
            String output = outputStream.toString();
            assertTrue(output.contains("Children: " + String.join(", ", List.of(children.toString()))));
        }
    }
    
    @Nested
    @DisplayName("History Tests")
    class HistoryTests {
        
        @Test
        @DisplayName("Should include history in operation tracking when all format is enabled")
        void shouldIncludeHistoryInOperationTrackingWhenAllFormatIsEnabled() {
            // Setup
            UUID itemId = UUID.fromString(TEST_ITEM_ID);
            WorkItem mockItem = createMockWorkItem(itemId.toString(), "Test Item", WorkflowState.CREATED);
            List<HistoryEntryRecord> mockHistory = createMockHistory(itemId);
            
            when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
            when(mockRelationshipService.getParentWorkItem(itemId)).thenReturn(null);
            when(mockRelationshipService.getChildWorkItems(itemId)).thenReturn(new ArrayList<>());
            when(mockHistoryService.getHistory(itemId)).thenReturn(mockHistory);
            
            command.setItemId(itemId.toString());
            command.setLongFormat(true);
            command.setAllFormat(true);
            
            // Execute
            command.call();
            
            // Verify output contains history section
            String output = outputStream.toString();
            assertTrue(output.contains("History:"));
            assertTrue(output.contains("STATE_CHANGE"));
            assertTrue(output.contains("FIELD_CHANGE"));
        }
        
        @Test
        @DisplayName("Should exclude history in operation tracking when all format is disabled")
        void shouldExcludeHistoryInOperationTrackingWhenAllFormatIsDisabled() {
            // Setup
            UUID itemId = UUID.fromString(TEST_ITEM_ID);
            WorkItem mockItem = createMockWorkItem(itemId.toString(), "Test Item", WorkflowState.CREATED);
            
            when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
            when(mockRelationshipService.getParentWorkItem(itemId)).thenReturn(null);
            when(mockRelationshipService.getChildWorkItems(itemId)).thenReturn(new ArrayList<>());
            
            command.setItemId(itemId.toString());
            command.setLongFormat(true);
            command.setAllFormat(false);
            
            // Execute
            command.call();
            
            // Verify history was not included
            String output = outputStream.toString();
            assertTrue(!output.contains("History:"));
            verify(mockHistoryService, Mockito.never()).getHistory(itemId);
        }
    }
    
    @Nested
    @DisplayName("JSON Output Tests")
    class JsonOutputTests {
        
        @Test
        @DisplayName("Should output in JSON format when specified")
        void shouldOutputInJsonFormatWhenSpecified() {
            // Setup
            List<WorkItem> mockItems = createMockWorkItems(2);
            when(mockItemService.getAllItems()).thenReturn(mockItems);
            command.setFormat("json");
            
            // Execute
            command.call();
            
            // Verify JSON output format
            String output = outputStream.toString();
            assertTrue(output.contains("\"count\":"));
            assertTrue(output.contains("\"workItems\":"));
            assertTrue(output.contains("\"displayOptions\":"));
        }
        
        @Test
        @DisplayName("Should include detailed information in JSON when using long format")
        void shouldIncludeDetailedInformationInJsonWhenUsingLongFormat() {
            // Setup
            List<WorkItem> mockItems = createMockWorkItems(2);
            when(mockItemService.getAllItems()).thenReturn(mockItems);
            command.setFormat("json");
            command.setLongFormat(true);
            
            // Execute
            command.call();
            
            // Verify JSON output includes detailed information
            String output = outputStream.toString();
            assertTrue(output.contains("\"description\":"));
            assertTrue(output.contains("\"project\":"));
            assertTrue(output.contains("\"created\":"));
            assertTrue(output.contains("\"updated\":"));
        }
        
        @Test
        @DisplayName("Should include history in JSON when using all format")
        void shouldIncludeHistoryInJsonWhenUsingAllFormat() {
            // Setup
            UUID itemId = UUID.fromString(TEST_ITEM_ID);
            WorkItem mockItem = createMockWorkItem(itemId.toString(), "Test Item", WorkflowState.CREATED);
            List<HistoryEntryRecord> mockHistory = createMockHistory(itemId);
            
            when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
            when(mockRelationshipService.getParentWorkItem(itemId)).thenReturn(null);
            when(mockRelationshipService.getChildWorkItems(itemId)).thenReturn(new ArrayList<>());
            when(mockHistoryService.getHistory(itemId)).thenReturn(mockHistory);
            
            command.setItemId(itemId.toString());
            command.setFormat("json");
            command.setLongFormat(true);
            command.setAllFormat(true);
            
            // Execute
            command.call();
            
            // Verify JSON output includes history
            String output = outputStream.toString();
            assertTrue(output.contains("\"history\":"));
        }
    }
    
    /**
     * Creates a list of mock work items for testing.
     * 
     * @param count number of items to create
     * @return list of mock work items
     */
    private List<WorkItem> createMockWorkItems(int count) {
        List<WorkItem> items = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            String id = UUID.randomUUID().toString();
            items.add(createMockWorkItem(id, "Test Item " + (i + 1), WorkflowState.CREATED));
        }
        return items;
    }
    
    /**
     * Creates a single mock work item for testing.
     * 
     * @param id the work item ID
     * @param title the work item title
     * @param state the work item state
     * @return the mock work item
     */
    private WorkItem createMockWorkItem(String id, String title, WorkflowState state) {
        WorkItem item = new WorkItem();
        item.setId(id);
        item.setTitle(title);
        item.setState(state);
        item.setType(WorkItemType.TASK);
        item.setPriority(org.rinna.cli.model.Priority.MEDIUM);
        item.setAssignee("testuser");
        item.setProject("Test Project");
        item.setDescription("Test description");
        item.setCreated(LocalDateTime.now().minusDays(5));
        item.setUpdated(LocalDateTime.now());
        return item;
    }
    
    /**
     * Creates mock history entries for testing.
     * 
     * @param itemId the work item ID
     * @return list of mock history entries
     */
    private List<HistoryEntryRecord> createMockHistory(UUID itemId) {
        List<HistoryEntryRecord> history = new ArrayList<>();
        
        // Add state change history entry
        history.add(new HistoryEntryRecord(
            itemId, 
            HistoryEntryType.STATE_CHANGE, 
            "testuser", 
            "State changed from NEW to OPEN", 
            null, 
            Instant.now().minus(3, ChronoUnit.DAYS)
        ));
        
        // Add field change history entry
        history.add(new HistoryEntryRecord(
            itemId, 
            HistoryEntryType.FIELD_CHANGE, 
            "testuser", 
            "Field 'priority' changed from 'LOW' to 'MEDIUM'", 
            "Priority", 
            Instant.now().minus(2, ChronoUnit.DAYS)
        ));
        
        // Add assignment history entry
        history.add(new HistoryEntryRecord(
            itemId, 
            HistoryEntryType.ASSIGNMENT, 
            "admin", 
            "Assignment changed from 'unassigned' to 'testuser'", 
            null, 
            Instant.now().minus(1, ChronoUnit.DAYS)
        ));
        
        return history;
    }
}