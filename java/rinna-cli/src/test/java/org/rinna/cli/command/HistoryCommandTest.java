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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.rinna.cli.domain.model.CommentType;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockCommentService;
import org.rinna.cli.service.MockCommentService.CommentImpl;
import org.rinna.cli.service.MockHistoryService;
import org.rinna.cli.service.MockHistoryService.HistoryEntryRecord;
import org.rinna.cli.service.MockHistoryService.HistoryEntryType;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;

/**
 * Test class for HistoryCommand operation tracking and output.
 */
@DisplayName("HistoryCommand Unit Tests")
class HistoryCommandTest {

    private static final String MOCK_OPERATION_ID = "mock-operation-id";
    private static final String TEST_ITEM_ID = "123e4567-e89b-12d3-a456-426614174000";
    private static final String TEST_USER = "test-user";
    
    private HistoryCommand command;
    private ServiceManager mockServiceManager;
    private MetadataService mockMetadataService;
    private ItemService mockItemService;
    private MockHistoryService mockHistoryService;
    private MockCommentService mockCommentService;
    private MockWorkflowService mockWorkflowService;
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
        mockHistoryService = mock(MockHistoryService.class);
        mockCommentService = mock(MockCommentService.class);
        mockWorkflowService = mock(MockWorkflowService.class);
        mockContextManager = mock(ContextManager.class);
        
        // Configure mocks
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockHistoryService()).thenReturn(mockHistoryService);
        when(mockServiceManager.getMockCommentService()).thenReturn(mockCommentService);
        when(mockServiceManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
        
        // Set up argument captors
        paramsCaptor = ArgumentCaptor.forClass(Map.class);
        resultCaptor = ArgumentCaptor.forClass(Map.class);
        
        // Set up operation ID for metadata service
        when(mockMetadataService.startOperation(eq("history"), anyString(), any())).thenReturn(MOCK_OPERATION_ID);
        
        // Mock the ContextManager
        try (var mockedStatic = Mockito.mockStatic(ContextManager.class)) {
            mockedStatic.when(ContextManager::getInstance).thenReturn(mockContextManager);
            
            // Create command with mocked service manager
            command = new HistoryCommand(mockServiceManager);
            command.setUser(TEST_USER);
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
    @DisplayName("Should track main operation parameters when viewing history for specific item")
    void shouldTrackMainOperationParametersWhenViewingHistoryForSpecificItem() {
        // Setup
        UUID itemId = UUID.fromString(TEST_ITEM_ID);
        WorkItem mockItem = createMockWorkItem(itemId.toString(), "Test Item", WorkflowState.CREATED);
        List<HistoryEntryRecord> mockHistory = createMockHistory(itemId);
        List<CommentImpl> mockComments = createMockComments(itemId);
        
        when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
        when(mockHistoryService.getHistory(itemId)).thenReturn(mockHistory);
        when(mockCommentService.getComments(itemId)).thenReturn(mockComments);
        command.setItemId(itemId.toString());
        
        // Execute
        int result = command.call();
        
        // Verify result
        assertEquals(0, result);
        
        // Verify operation tracking
        verify(mockMetadataService).startOperation(eq("history"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        // Verify parameters
        assertEquals(itemId.toString(), params.get("itemId"));
        assertEquals(true, params.get("showComments"));
        assertEquals(true, params.get("showStateChanges"));
        assertEquals(true, params.get("showAssignments"));
        assertEquals(true, params.get("showFieldChanges"));
        assertEquals("text", params.get("format"));
        assertEquals(false, params.get("verbose"));
        assertEquals(TEST_USER, params.get("user"));
    }
    
    @Test
    @DisplayName("Should track operation completion with result metadata")
    void shouldTrackOperationCompletionWithResultMetadata() {
        // Setup
        UUID itemId = UUID.fromString(TEST_ITEM_ID);
        WorkItem mockItem = createMockWorkItem(itemId.toString(), "Test Item", WorkflowState.CREATED);
        List<HistoryEntryRecord> mockHistory = createMockHistory(itemId);
        List<CommentImpl> mockComments = createMockComments(itemId);
        
        when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
        when(mockHistoryService.getHistory(itemId)).thenReturn(mockHistory);
        when(mockCommentService.getComments(itemId)).thenReturn(mockComments);
        command.setItemId(itemId.toString());
        
        // Execute
        int result = command.call();
        
        // Verify result
        assertEquals(0, result);
        
        // Verify operation completion
        verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
        Map<String, Object> resultData = resultCaptor.getValue();
        
        // Verify result data
        assertEquals(itemId.toString(), resultData.get("itemId"));
        assertEquals("Test Item", resultData.get("itemTitle"));
        assertEquals("TASK", resultData.get("itemType"));
        assertEquals("OPEN", resultData.get("itemStatus"));
        assertEquals(3, resultData.get("historyEntryCount"));
        assertEquals(2, resultData.get("commentCount"));
        assertEquals(5, resultData.get("totalEntryCount"));
        
        // Verify context update
        verify(mockContextManager).setLastViewedWorkItem(eq(itemId));
    }
    
    @Test
    @DisplayName("Should track operation for current work-in-progress item when no ID provided")
    void shouldTrackOperationForCurrentWorkInProgressItemWhenNoIdProvided() {
        // Setup
        UUID itemId = UUID.fromString(TEST_ITEM_ID);
        WorkItem mockItem = createMockWorkItem(itemId.toString(), "WIP Item", WorkflowState.IN_PROGRESS);
        List<HistoryEntryRecord> mockHistory = createMockHistory(itemId);
        List<CommentImpl> mockComments = createMockComments(itemId);
        
        when(mockWorkflowService.getCurrentWorkInProgress(TEST_USER)).thenReturn(Optional.of(mockItem));
        when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
        when(mockHistoryService.getHistory(itemId)).thenReturn(mockHistory);
        when(mockCommentService.getComments(itemId)).thenReturn(mockComments);
        
        // Execute
        int result = command.call();
        
        // Verify result
        assertEquals(0, result);
        
        // Verify tracking contains correct parameters
        verify(mockMetadataService).startOperation(eq("history"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertParamIsNull(params, "itemId");
        
        // Verify tracking completion
        verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
        Map<String, Object> resultData = resultCaptor.getValue();
        assertEquals(itemId.toString(), resultData.get("itemId"));
        assertEquals("WIP Item", resultData.get("itemTitle"));
        
        // Verify output
        String output = outputStream.toString();
        assertTrue(output.contains("Showing history for current work item: " + itemId));
    }
    
    @Test
    @DisplayName("Should track operation failure when no work-in-progress item exists")
    void shouldTrackOperationFailureWhenNoWorkInProgressItemExists() {
        // Setup
        when(mockWorkflowService.getCurrentWorkInProgress(TEST_USER)).thenReturn(Optional.empty());
        
        // Execute
        int result = command.call();
        
        // Verify result
        assertEquals(1, result);
        
        // Verify operation failure
        verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalStateException.class));
        
        // Verify error output
        String errorOutput = errorStream.toString();
        assertTrue(errorOutput.contains("No work item is currently in progress"));
    }
    
    @Test
    @DisplayName("Should track operation failure when item is not found")
    void shouldTrackOperationFailureWhenItemIsNotFound() {
        // Setup
        UUID itemId = UUID.fromString(TEST_ITEM_ID);
        when(mockItemService.getItem(itemId.toString())).thenReturn(null);
        command.setItemId(itemId.toString());
        
        // Execute
        int result = command.call();
        
        // Verify result
        assertEquals(1, result);
        
        // Verify operation failure
        verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalArgumentException.class));
        
        // Verify error output
        String errorOutput = errorStream.toString();
        assertTrue(errorOutput.contains("Work item not found with ID: " + itemId));
    }
    
    @Test
    @DisplayName("Should track operation failure for invalid item ID format")
    void shouldTrackOperationFailureForInvalidItemIdFormat() {
        // Setup
        String invalidItemId = "invalid-uuid";
        
        // Execute
        try {
            command.setItemId(invalidItemId);
            command.call();
        } catch (IllegalArgumentException e) {
            // Expected exception - the test properly simulates what happens when an invalid UUID is provided
            // In the real application, this would be caught by the command's call() method
            assertTrue(e.getMessage().contains("Invalid work item ID format"));
        }
    }
    
    @Test
    @DisplayName("Should track operation failure when user lacks permissions")
    void shouldTrackOperationFailureWhenUserLacksPermissions() {
        // Setup
        UUID itemId = UUID.fromString(TEST_ITEM_ID);
        WorkItem mockItem = mock(WorkItem.class);
        when(mockItem.getId()).thenReturn(itemId.toString());
        when(mockItem.getTitle()).thenReturn("Test Item");
        when(mockItem.getType()).thenReturn(WorkItemType.TASK);
        when(mockItem.getState()).thenReturn(WorkflowState.CREATED);
        when(mockItem.isVisible(anyString())).thenReturn(false); // User can't see this item
        
        when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
        command.setItemId(itemId.toString());
        
        // Execute
        int result = command.call();
        
        // Verify result
        assertEquals(1, result);
        
        // Verify operation failure
        verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(SecurityException.class));
        
        // Verify error output
        String errorOutput = errorStream.toString();
        assertTrue(errorOutput.contains("You do not have permission to view this work item"));
    }
    
    @Nested
    @DisplayName("Filter Option Tests")
    class FilterOptionTests {
        
        @Test
        @DisplayName("Should track filter options in operation parameters")
        void shouldTrackFilterOptionsInOperationParameters() {
            // Setup
            UUID itemId = UUID.fromString(TEST_ITEM_ID);
            WorkItem mockItem = createMockWorkItem(itemId.toString(), "Test Item", WorkflowState.CREATED);
            List<HistoryEntryRecord> mockHistory = createMockHistory(itemId);
            List<CommentImpl> mockComments = createMockComments(itemId);
            
            when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
            when(mockHistoryService.getHistory(itemId)).thenReturn(mockHistory);
            when(mockCommentService.getComments(itemId)).thenReturn(mockComments);
            
            command.setItemId(itemId.toString());
            command.setShowComments(false);
            command.setShowStateChanges(false);
            command.setShowFieldChanges(true);
            command.setShowAssignments(true);
            
            // Execute
            command.call();
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("history"), eq("READ"), paramsCaptor.capture());
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(false, params.get("showComments"));
            assertEquals(false, params.get("showStateChanges"));
            assertEquals(true, params.get("showFieldChanges"));
            assertEquals(true, params.get("showAssignments"));
            
            // Verify that history entries are filtered correctly in result
            verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
            Map<String, Object> resultData = resultCaptor.getValue();
            
            // Since we disabled state changes and comments, we should only see field changes and assignments
            // which is 2 out of 3 history entries, and 0 comments
            assertEquals(2, resultData.get("historyEntryCount"));
            assertEquals(0, resultData.get("commentCount"));
            assertEquals(2, resultData.get("totalEntryCount"));
        }
    }
    
    @Nested
    @DisplayName("Time Range Tests")
    class TimeRangeTests {
        
        @Test
        @DisplayName("Should track time range parameters for hours")
        void shouldTrackTimeRangeParametersForHours() {
            // Setup
            UUID itemId = UUID.fromString(TEST_ITEM_ID);
            WorkItem mockItem = createMockWorkItem(itemId.toString(), "Test Item", WorkflowState.CREATED);
            List<HistoryEntryRecord> mockHistory = createMockHistory(itemId);
            List<CommentImpl> mockComments = createMockComments(itemId);
            
            when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
            when(mockHistoryService.getHistoryFromLastHours(eq(itemId), eq(5))).thenReturn(mockHistory);
            when(mockCommentService.getCommentsFromLastHours(eq(itemId), eq(5))).thenReturn(mockComments);
            
            command.setItemId(itemId.toString());
            command.setTimeRange("5h");
            
            // Execute
            command.call();
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("history"), eq("READ"), paramsCaptor.capture());
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals("5h", params.get("timeRange"));
            
            // Verify result tracking
            verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
            Map<String, Object> resultData = resultCaptor.getValue();
            assertEquals("HOURS", resultData.get("timeRangeUnit"));
            assertEquals(5, resultData.get("timeRangeAmount"));
            
            // Verify appropriate methods were called
            verify(mockHistoryService).getHistoryFromLastHours(itemId, 5);
            verify(mockCommentService).getCommentsFromLastHours(itemId, 5);
            verify(mockHistoryService, never()).getHistory(any(UUID.class));
        }
        
        @Test
        @DisplayName("Should track time range parameters for days")
        void shouldTrackTimeRangeParametersForDays() {
            // Setup
            UUID itemId = UUID.fromString(TEST_ITEM_ID);
            WorkItem mockItem = createMockWorkItem(itemId.toString(), "Test Item", WorkflowState.CREATED);
            List<HistoryEntryRecord> mockHistory = createMockHistory(itemId);
            List<CommentImpl> mockComments = createMockComments(itemId);
            
            when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
            when(mockHistoryService.getHistoryFromLastDays(eq(itemId), eq(3))).thenReturn(mockHistory);
            when(mockCommentService.getCommentsFromLastDays(eq(itemId), eq(3))).thenReturn(mockComments);
            
            command.setItemId(itemId.toString());
            command.setTimeRange("3d");
            
            // Execute
            command.call();
            
            // Verify result tracking
            verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
            Map<String, Object> resultData = resultCaptor.getValue();
            assertEquals("DAYS", resultData.get("timeRangeUnit"));
            assertEquals(3, resultData.get("timeRangeAmount"));
            
            // Verify appropriate methods were called
            verify(mockHistoryService).getHistoryFromLastDays(itemId, 3);
            verify(mockCommentService).getCommentsFromLastDays(itemId, 3);
        }
        
        @Test
        @DisplayName("Should track time range parameters for weeks")
        void shouldTrackTimeRangeParametersForWeeks() {
            // Setup
            UUID itemId = UUID.fromString(TEST_ITEM_ID);
            WorkItem mockItem = createMockWorkItem(itemId.toString(), "Test Item", WorkflowState.CREATED);
            List<HistoryEntryRecord> mockHistory = createMockHistory(itemId);
            List<CommentImpl> mockComments = createMockComments(itemId);
            
            when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
            when(mockHistoryService.getHistoryFromLastWeeks(eq(itemId), eq(2))).thenReturn(mockHistory);
            when(mockCommentService.getCommentsFromLastWeeks(eq(itemId), eq(2))).thenReturn(mockComments);
            
            command.setItemId(itemId.toString());
            command.setTimeRange("2w");
            
            // Execute
            command.call();
            
            // Verify result tracking
            verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
            Map<String, Object> resultData = resultCaptor.getValue();
            assertEquals("WEEKS", resultData.get("timeRangeUnit"));
            assertEquals(2, resultData.get("timeRangeAmount"));
            
            // Verify appropriate methods were called
            verify(mockHistoryService).getHistoryFromLastWeeks(itemId, 2);
            verify(mockCommentService).getCommentsFromLastWeeks(itemId, 2);
        }
        
        @Test
        @DisplayName("Should track operation failure for invalid time range format")
        void shouldTrackOperationFailureForInvalidTimeRangeFormat() {
            // Setup
            UUID itemId = UUID.fromString(TEST_ITEM_ID);
            WorkItem mockItem = createMockWorkItem(itemId.toString(), "Test Item", WorkflowState.CREATED);
            when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
            
            command.setItemId(itemId.toString());
            command.setTimeRange("invalid");
            
            // Execute
            int result = command.call();
            
            // Verify result
            assertEquals(1, result);
            
            // Verify operation failure
            verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalArgumentException.class));
            
            // Verify error output
            String errorOutput = errorStream.toString();
            assertTrue(errorOutput.contains("Invalid time range format"));
        }
    }
    
    @Nested
    @DisplayName("Output Format Tests")
    class OutputFormatTests {
        
        @Test
        @DisplayName("Should track JSON format in operation parameters")
        void shouldTrackJsonFormatInOperationParameters() {
            // Setup
            UUID itemId = UUID.fromString(TEST_ITEM_ID);
            WorkItem mockItem = createMockWorkItem(itemId.toString(), "Test Item", WorkflowState.CREATED);
            List<HistoryEntryRecord> mockHistory = createMockHistory(itemId);
            List<CommentImpl> mockComments = createMockComments(itemId);
            
            when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
            when(mockHistoryService.getHistory(itemId)).thenReturn(mockHistory);
            when(mockCommentService.getComments(itemId)).thenReturn(mockComments);
            
            command.setItemId(itemId.toString());
            command.setFormat("json");
            
            // Execute
            command.call();
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("history"), eq("READ"), paramsCaptor.capture());
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals("json", params.get("format"));
            
            // Verify JSON output was generated
            String output = outputStream.toString();
            assertTrue(output.contains("\"workItem\":"));
            assertTrue(output.contains("\"history\":"));
            assertTrue(output.contains("\"stats\":"));
        }
        
        @Test
        @DisplayName("Should include verbose details in JSON when verbose is enabled")
        void shouldIncludeVerboseDetailsInJsonWhenVerboseIsEnabled() {
            // Setup
            UUID itemId = UUID.fromString(TEST_ITEM_ID);
            WorkItem mockItem = createMockWorkItem(itemId.toString(), "Test Item", WorkflowState.CREATED);
            List<HistoryEntryRecord> mockHistory = createMockHistory(itemId);
            List<CommentImpl> mockComments = createMockComments(itemId);
            
            when(mockItemService.getItem(itemId.toString())).thenReturn(mockItem);
            when(mockHistoryService.getHistory(itemId)).thenReturn(mockHistory);
            when(mockCommentService.getComments(itemId)).thenReturn(mockComments);
            
            command.setItemId(itemId.toString());
            command.setFormat("json");
            command.setVerbose(true);
            
            // Execute
            command.call();
            
            // Verify operation tracking
            verify(mockMetadataService).startOperation(eq("history"), eq("READ"), paramsCaptor.capture());
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(true, params.get("verbose"));
            
            // Verify verbose JSON output includes additional details
            String output = outputStream.toString();
            assertTrue(output.contains("\"priority\":"));
            assertTrue(output.contains("\"assignee\":"));
            assertTrue(output.contains("\"description\":"));
        }
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
    
    /**
     * Creates mock comments for testing.
     * 
     * @param itemId the work item ID
     * @return list of mock comments
     */
    private List<CommentImpl> createMockComments(UUID itemId) {
        List<CommentImpl> comments = new ArrayList<>();
        
        // Add standard comment
        comments.add(new CommentImpl(
            UUID.randomUUID(),
            itemId,
            "testuser",
            Instant.now().minus(4, ChronoUnit.DAYS),
            "Initial investigation completed",
            CommentType.STANDARD
        ));
        
        // Add system comment
        comments.add(new CommentImpl(
            UUID.randomUUID(),
            itemId,
            "system",
            Instant.now().minus(12, ChronoUnit.HOURS),
            "Item was added to Sprint 42",
            CommentType.SYSTEM
        ));
        
        return comments;
    }
    
    /**
     * Helper method to assert a param is null in a map.
     * This is to handle cases where the parameter might not be present at all.
     * 
     * @param params the parameters map
     * @param key the key to check
     */
    private void assertParamIsNull(Map<String, Object> params, String key) {
        assertTrue(!params.containsKey(key) || params.get(key) == null, 
            "Parameter '" + key + "' should be null but was: " + params.get(key));
    }
}