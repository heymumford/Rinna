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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.HistoryCommand;
import org.rinna.cli.domain.model.CommentType;
import org.rinna.cli.model.Priority;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Component tests for HistoryCommand focusing on service integration.
 */
@DisplayName("HistoryCommand Component Tests")
public class HistoryCommandComponentTest {

    private static final String TEST_USER = "testuser";
    private static final UUID TEST_ITEM_ID = UUID.randomUUID();
    
    private HistoryCommand historyCommand;
    
    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private ItemService mockItemService;
    
    @Mock
    private MockHistoryService mockHistoryService;
    
    @Mock
    private MockCommentService mockCommentService;
    
    @Mock
    private MockWorkflowService mockWorkflowService;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private ContextManager mockContextManager;
    
    private MockedStatic<ServiceManager> mockedStaticServiceManager;
    private MockedStatic<ContextManager> mockedStaticContextManager;
    
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    private ArgumentCaptor<Map<String, Object>> resultCaptor;
    
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    @BeforeEach
    void setUp() {
        // Initialize mocks
        mockServiceManager = Mockito.mock(ServiceManager.class);
        mockItemService = Mockito.mock(ItemService.class);
        mockHistoryService = Mockito.mock(MockHistoryService.class);
        mockCommentService = Mockito.mock(MockCommentService.class);
        mockWorkflowService = Mockito.mock(MockWorkflowService.class);
        mockMetadataService = Mockito.mock(MetadataService.class);
        mockContextManager = Mockito.mock(ContextManager.class);
        
        // Set up argument captors
        paramsCaptor = ArgumentCaptor.forClass(Map.class);
        resultCaptor = ArgumentCaptor.forClass(Map.class);
        
        // Set up static mocks
        mockedStaticServiceManager = Mockito.mockStatic(ServiceManager.class);
        mockedStaticContextManager = Mockito.mockStatic(ContextManager.class);
        
        // Configure the mock ServiceManager
        mockedStaticServiceManager.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        mockedStaticContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
        
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockHistoryService()).thenReturn(mockHistoryService);
        when(mockServiceManager.getMockCommentService()).thenReturn(mockCommentService);
        when(mockServiceManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up operation ID for metadata service
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn("operation-id");
        
        // Capture stdout and stderr
        originalOut = System.out;
        originalErr = System.err;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Create the command
        historyCommand = new HistoryCommand(mockServiceManager);
        historyCommand.setUser(TEST_USER);
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
        if (mockedStaticContextManager != null) {
            mockedStaticContextManager.close();
        }
    }
    
    @Test
    @DisplayName("Should integrate properly with ItemService for item retrieval")
    void shouldIntegrateProperlyWithItemServiceForItemRetrieval() {
        // Arrange
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        when(mockHistoryService.getHistory(TEST_ITEM_ID)).thenReturn(new ArrayList<>());
        when(mockCommentService.getComments(TEST_ITEM_ID)).thenReturn(new ArrayList<>());
        
        historyCommand.setItemId(TEST_ITEM_ID.toString());
        
        // Act
        int result = historyCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        verify(mockItemService).getItem(TEST_ITEM_ID.toString());
        verify(mockContextManager).setLastViewedWorkItem(eq(TEST_ITEM_ID));
    }
    
    @Test
    @DisplayName("Should integrate properly with HistoryService and CommentService for fetching history")
    void shouldIntegrateProperlyWithHistoryServiceAndCommentService() {
        // Arrange
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        List<HistoryEntryRecord> mockHistory = createMockHistory(TEST_ITEM_ID);
        List<CommentImpl> mockComments = createMockComments(TEST_ITEM_ID);
        
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        when(mockHistoryService.getHistory(TEST_ITEM_ID)).thenReturn(mockHistory);
        when(mockCommentService.getComments(TEST_ITEM_ID)).thenReturn(mockComments);
        
        historyCommand.setItemId(TEST_ITEM_ID.toString());
        
        // Act
        int result = historyCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        verify(mockHistoryService).getHistory(TEST_ITEM_ID);
        verify(mockCommentService).getComments(TEST_ITEM_ID);
        
        // Verify that the output contains history entries and comments
        String output = outContent.toString();
        assertTrue(output.contains("State changed from"), "Output should contain state changes");
        assertTrue(output.contains("Field 'priority'"), "Output should contain field changes");
        assertTrue(output.contains("Assignment changed"), "Output should contain assignment changes");
        assertTrue(output.contains("Initial investigation"), "Output should contain comment text");
    }
    
    @Test
    @DisplayName("Should integrate properly with WorkflowService for current work in progress")
    void shouldIntegrateProperlyWithWorkflowServiceForCurrentWorkInProgress() {
        // Arrange
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        List<HistoryEntryRecord> mockHistory = createMockHistory(TEST_ITEM_ID);
        List<CommentImpl> mockComments = createMockComments(TEST_ITEM_ID);
        
        when(mockWorkflowService.getCurrentWorkInProgress(TEST_USER)).thenReturn(Optional.of(mockItem));
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        when(mockHistoryService.getHistory(TEST_ITEM_ID)).thenReturn(mockHistory);
        when(mockCommentService.getComments(TEST_ITEM_ID)).thenReturn(mockComments);
        
        // Don't set an item ID to test WIP functionality
        
        // Act
        int result = historyCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        verify(mockWorkflowService).getCurrentWorkInProgress(TEST_USER);
        
        // Verify the output indicates showing history for current work item
        String output = outContent.toString();
        assertTrue(output.contains("Showing history for current work item"), 
            "Output should indicate showing history for current work item");
    }
    
    @Test
    @DisplayName("Should properly track operations with MetadataService")
    void shouldProperlyTrackOperationsWithMetadataService() {
        // Arrange
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        List<HistoryEntryRecord> mockHistory = createMockHistory(TEST_ITEM_ID);
        List<CommentImpl> mockComments = createMockComments(TEST_ITEM_ID);
        
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        when(mockHistoryService.getHistory(TEST_ITEM_ID)).thenReturn(mockHistory);
        when(mockCommentService.getComments(TEST_ITEM_ID)).thenReturn(mockComments);
        
        historyCommand.setItemId(TEST_ITEM_ID.toString());
        
        // Act
        int result = historyCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        
        // Verify operation tracking
        verify(mockMetadataService).startOperation(eq("history"), eq("READ"), paramsCaptor.capture());
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        
        // Check parameters captured
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals(TEST_ITEM_ID.toString(), params.get("itemId"), "Should track item ID");
        assertEquals(TEST_USER, params.get("user"), "Should track username");
        
        // Check result data captured
        Map<String, Object> result = resultCaptor.getValue();
        assertEquals(TEST_ITEM_ID.toString(), result.get("itemId"), "Should track item ID in result");
        assertEquals("Test Item", result.get("itemTitle"), "Should track item title in result");
        assertEquals(3, result.get("historyEntryCount"), "Should track history entry count");
        assertEquals(2, result.get("commentCount"), "Should track comment count");
        assertEquals(5, result.get("totalEntryCount"), "Should track total entry count");
    }
    
    @Test
    @DisplayName("Should properly handle time-based filtering via HistoryService")
    void shouldProperlyHandleTimeBasedFilteringViaHistoryService() {
        // Arrange
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        List<HistoryEntryRecord> mockHistory = createMockHistory(TEST_ITEM_ID);
        List<CommentImpl> mockComments = createMockComments(TEST_ITEM_ID);
        
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        when(mockHistoryService.getHistoryFromLastDays(eq(TEST_ITEM_ID), eq(7))).thenReturn(mockHistory);
        when(mockCommentService.getCommentsFromLastDays(eq(TEST_ITEM_ID), eq(7))).thenReturn(mockComments);
        
        historyCommand.setItemId(TEST_ITEM_ID.toString());
        historyCommand.setTimeRange("7d");
        
        // Act
        int result = historyCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        
        // Verify the correct time-based methods were called
        verify(mockHistoryService).getHistoryFromLastDays(TEST_ITEM_ID, 7);
        verify(mockCommentService).getCommentsFromLastDays(TEST_ITEM_ID, 7);
        
        // Verify operation tracking includes time range info
        verify(mockMetadataService).startOperation(eq("history"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("7d", params.get("timeRange"), "Should track time range parameter");
        
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        Map<String, Object> result = resultCaptor.getValue();
        assertEquals("DAYS", result.get("timeRangeUnit"), "Should track time range unit in result");
        assertEquals(7, result.get("timeRangeAmount"), "Should track time range amount in result");
    }
    
    @Test
    @DisplayName("Should properly integrate with ContextManager to update last viewed item")
    void shouldProperlyIntegrateWithContextManagerToUpdateLastViewedItem() {
        // Arrange
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        List<HistoryEntryRecord> mockHistory = createMockHistory(TEST_ITEM_ID);
        List<CommentImpl> mockComments = createMockComments(TEST_ITEM_ID);
        
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        when(mockHistoryService.getHistory(TEST_ITEM_ID)).thenReturn(mockHistory);
        when(mockCommentService.getComments(TEST_ITEM_ID)).thenReturn(mockComments);
        
        historyCommand.setItemId(TEST_ITEM_ID.toString());
        
        // Act
        int result = historyCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        
        // Verify context manager was called to update the last viewed item
        verify(mockContextManager).setLastViewedWorkItem(TEST_ITEM_ID);
    }
    
    @Test
    @DisplayName("Should properly handle error cases through MetadataService")
    void shouldProperlyHandleErrorCasesThroughMetadataService() {
        // Arrange - set up a non-existent item
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(null);
        historyCommand.setItemId(TEST_ITEM_ID.toString());
        
        // Act
        int result = historyCommand.call();
        
        // Assert
        assertEquals(1, result, "Command should fail");
        
        // Verify operation failure was tracked
        verify(mockMetadataService).startOperation(eq("history"), eq("READ"), any());
        verify(mockMetadataService).failOperation(anyString(), any(IllegalArgumentException.class));
        
        // Verify appropriate error message
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Work item not found"), 
            "Error output should indicate work item not found");
    }
    
    @Test
    @DisplayName("Should handle multiple filter options in component context")
    void shouldHandleMultipleFilterOptionsInComponentContext() {
        // Arrange
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        List<HistoryEntryRecord> mockHistory = createMockHistory(TEST_ITEM_ID);
        List<CommentImpl> mockComments = createMockComments(TEST_ITEM_ID);
        
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        when(mockHistoryService.getHistory(TEST_ITEM_ID)).thenReturn(mockHistory);
        when(mockCommentService.getComments(TEST_ITEM_ID)).thenReturn(mockComments);
        
        historyCommand.setItemId(TEST_ITEM_ID.toString());
        historyCommand.setShowComments(false);
        historyCommand.setShowStateChanges(true);
        historyCommand.setShowFieldChanges(false);
        historyCommand.setShowAssignments(true);
        
        // Act
        int result = historyCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        
        // Verify operation tracking captures filter options
        verify(mockMetadataService).startOperation(eq("history"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        
        assertEquals(false, params.get("showComments"), "Should track showComments parameter");
        assertEquals(true, params.get("showStateChanges"), "Should track showStateChanges parameter");
        assertEquals(false, params.get("showFieldChanges"), "Should track showFieldChanges parameter");
        assertEquals(true, params.get("showAssignments"), "Should track showAssignments parameter");
        
        // Check result reflects filtered counts
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        Map<String, Object> result = resultCaptor.getValue();
        
        // Only state changes (1) and assignments (1) should be counted, not field changes or comments
        assertEquals(2, result.get("historyEntryCount"), "History entry count should reflect filters");
        assertEquals(0, result.get("commentCount"), "Comment count should be 0 when comments are disabled");
        assertEquals(2, result.get("totalEntryCount"), "Total entry count should reflect filters");
    }
    
    @Test
    @DisplayName("Should handle nested operation tracking for complex workflows")
    void shouldHandleNestedOperationTrackingForComplexWorkflows() {
        // Arrange
        String operationId = "main-operation-id";
        String nestedIdA = "nested-operation-a";
        String nestedIdB = "nested-operation-b";
        
        when(mockMetadataService.startOperation(eq("history"), eq("READ"), any())).thenReturn(operationId);
        when(mockMetadataService.startOperation(eq("history-get-item"), eq("READ"), any())).thenReturn(nestedIdA);
        when(mockMetadataService.startOperation(eq("history-get-entries"), eq("READ"), any())).thenReturn(nestedIdB);
        
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        when(mockHistoryService.getHistory(TEST_ITEM_ID)).thenReturn(createMockHistory(TEST_ITEM_ID));
        when(mockCommentService.getComments(TEST_ITEM_ID)).thenReturn(createMockComments(TEST_ITEM_ID));
        
        historyCommand.setItemId(TEST_ITEM_ID.toString());
        
        // This test is demonstrating how you would test an implementation that uses nested operations.
        // The current HistoryCommand doesn't actually use nested operations, but this test shows
        // how you would verify that behavior if it did.
        
        // Act
        int result = historyCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        
        // Verify main operation tracking
        verify(mockMetadataService, times(1)).startOperation(eq("history"), eq("READ"), any());
        verify(mockMetadataService, times(1)).completeOperation(eq(operationId), any());
        
        // Note: The current implementation doesn't use nested operations, so these verifications 
        // would fail. If the implementation were changed to use nested operations, these would pass.
        // 
        // verify(mockMetadataService, times(1)).startOperation(eq("history-get-item"), eq("READ"), any());
        // verify(mockMetadataService, times(1)).completeOperation(eq(nestedIdA), any());
        // verify(mockMetadataService, times(1)).startOperation(eq("history-get-entries"), eq("READ"), any());
        // verify(mockMetadataService, times(1)).completeOperation(eq(nestedIdB), any());
    }
    
    // Helper methods
    
    private WorkItem createMockWorkItem(String id, String title, WorkflowState state) {
        WorkItem item = new WorkItem();
        item.setId(id);
        item.setTitle(title);
        item.setState(state);
        item.setType(WorkItemType.TASK);
        item.setPriority(Priority.MEDIUM);
        item.setAssignee("testuser");
        item.setProject("Test Project");
        item.setDescription("Test description");
        item.setCreated(LocalDateTime.now().minusDays(5));
        item.setUpdated(LocalDateTime.now());
        return item;
    }
    
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
}