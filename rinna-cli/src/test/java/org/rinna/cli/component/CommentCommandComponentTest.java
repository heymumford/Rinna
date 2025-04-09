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
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.CommentCommand;
import org.rinna.cli.domain.model.CommentType;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockCommentService;
import org.rinna.cli.service.MockCommentService.CommentImpl;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Component tests for CommentCommand focusing on service integration.
 */
@DisplayName("CommentCommand Component Tests")
public class CommentCommandComponentTest {

    private static final String TEST_USER = "testuser";
    private static final UUID TEST_ITEM_ID = UUID.randomUUID();
    private static final String TEST_COMMENT = "This is a test comment";
    
    private CommentCommand commentCommand;
    
    private ServiceManager mockServiceManager;
    private MockItemService mockItemService;
    private MockCommentService mockCommentService;
    private MockWorkflowService mockWorkflowService;
    private MetadataService mockMetadataService;
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
        mockItemService = Mockito.mock(MockItemService.class);
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
        
        when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);
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
        commentCommand = new CommentCommand(mockServiceManager);
        commentCommand.setUsername(TEST_USER);
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
    @DisplayName("Should integrate properly with CommentService for adding comments")
    void shouldIntegrateProperlyWithCommentServiceForAddingComments() {
        // Arrange
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        
        commentCommand.setItemId(TEST_ITEM_ID.toString());
        commentCommand.setComment(TEST_COMMENT);
        
        // Act
        int result = commentCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        
        // Verify integration with comment service
        verify(mockCommentService).addComment(TEST_ITEM_ID, TEST_USER, TEST_COMMENT);
        
        // Verify context update
        verify(mockContextManager).setCurrentItemId(TEST_ITEM_ID.toString());
    }
    
    @Test
    @DisplayName("Should integrate properly with WorkflowService for current work in progress")
    void shouldIntegrateProperlyWithWorkflowServiceForCurrentWorkInProgress() {
        // Arrange
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        when(mockWorkflowService.getCurrentWorkInProgress(TEST_USER)).thenReturn(Optional.of(mockItem));
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        
        commentCommand.setComment(TEST_COMMENT);
        // Don't set item ID to test WIP resolution
        
        // Act
        int result = commentCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        
        // Verify integration with workflow service
        verify(mockWorkflowService).getCurrentWorkInProgress(TEST_USER);
        
        // Verify comment was added to the resolved WIP item
        verify(mockCommentService).addComment(TEST_ITEM_ID, TEST_USER, TEST_COMMENT);
    }
    
    @Test
    @DisplayName("Should integrate properly with ItemService for item retrieval")
    void shouldIntegrateProperlyWithItemServiceForItemRetrieval() {
        // Arrange
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        
        commentCommand.setItemId(TEST_ITEM_ID.toString());
        commentCommand.setComment(TEST_COMMENT);
        
        // Act
        int result = commentCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        
        // Verify integration with item service
        verify(mockItemService).getItem(TEST_ITEM_ID.toString());
    }
    
    @Test
    @DisplayName("Should integrate properly with MetadataService for operation tracking")
    void shouldIntegrateProperlyWithMetadataServiceForOperationTracking() {
        // Arrange
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        
        commentCommand.setItemId(TEST_ITEM_ID.toString());
        commentCommand.setComment(TEST_COMMENT);
        
        // Act
        int result = commentCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        
        // Verify integration with metadata service for operation tracking
        verify(mockMetadataService).startOperation(eq("comment"), eq("CREATE"), paramsCaptor.capture());
        verify(mockMetadataService).startOperation(eq("comment-add"), eq("CREATE"), any());
        verify(mockMetadataService).completeOperation(anyString(), any());
        
        // Verify tracked parameters
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals(TEST_USER, params.get("username"));
        assertEquals(TEST_ITEM_ID.toString(), params.get("itemId"));
        assertEquals(true, params.get("hasComment"));
        assertEquals(TEST_COMMENT.length(), params.get("commentLength"));
    }
    
    @Test
    @DisplayName("Should handle verbose mode with hierarchical operation tracking")
    void shouldHandleVerboseModeWithHierarchicalOperationTracking() {
        // Arrange
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        List<CommentImpl> comments = createMockComments(TEST_ITEM_ID);
        
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        when(mockCommentService.getComments(TEST_ITEM_ID)).thenReturn(comments);
        
        commentCommand.setItemId(TEST_ITEM_ID.toString());
        commentCommand.setComment(TEST_COMMENT);
        commentCommand.setVerbose(true);
        
        // Act
        int result = commentCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        
        // Verify hierarchical operation tracking
        verify(mockMetadataService).startOperation(eq("comment"), eq("CREATE"), any());
        verify(mockMetadataService).startOperation(eq("comment-add"), eq("CREATE"), any());
        verify(mockMetadataService).startOperation(eq("comment-list"), eq("READ"), any());
        verify(mockMetadataService, times(3)).completeOperation(anyString(), any());
        
        // Verify comment listing integration
        verify(mockCommentService).getComments(TEST_ITEM_ID);
        
        // Check output contains comment details
        String output = outContent.toString();
        assertTrue(output.contains("Comments for work item"));
        assertTrue(output.contains("Initial investigation"));
    }
    
    @Test
    @DisplayName("Should handle JSON output formatting")
    void shouldHandleJsonOutputFormatting() {
        // Arrange
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        
        commentCommand.setItemId(TEST_ITEM_ID.toString());
        commentCommand.setComment(TEST_COMMENT);
        commentCommand.setFormat("json");
        
        // Act
        int result = commentCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        
        // Check JSON output format
        String output = outContent.toString();
        assertTrue(output.contains("\"success\": true"));
        assertTrue(output.contains("\"workItemId\": \"" + TEST_ITEM_ID + "\""));
        assertTrue(output.contains("\"action\": \"comment_added\""));
    }
    
    @Test
    @DisplayName("Should handle JSON output with verbose mode")
    void shouldHandleJsonOutputWithVerboseMode() {
        // Arrange
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        List<CommentImpl> comments = createMockComments(TEST_ITEM_ID);
        
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        when(mockCommentService.getComments(TEST_ITEM_ID)).thenReturn(comments);
        
        commentCommand.setItemId(TEST_ITEM_ID.toString());
        commentCommand.setComment(TEST_COMMENT);
        commentCommand.setFormat("json");
        commentCommand.setVerbose(true);
        
        // Act
        int result = commentCommand.call();
        
        // Assert
        assertEquals(0, result, "Command should succeed");
        
        // Check JSON output contains comment details
        String output = outContent.toString();
        assertTrue(output.contains("\"comments\": ["));
        assertTrue(output.contains("\"text\":"));
        assertTrue(output.contains("\"user\":"));
        assertTrue(output.contains("\"timestamp\":"));
        assertTrue(output.contains("\"commentCount\":"));
    }
    
    @Test
    @DisplayName("Should handle short ID resolution with ItemService")
    void shouldHandleShortIdResolutionWithItemService() {
        // Arrange
        WorkItem mockItem = createMockWorkItem(TEST_ITEM_ID.toString(), "Test Item", WorkflowState.IN_PROGRESS);
        when(mockItemService.findItemByShortId("WI-123")).thenReturn(mockItem);
        when(mockItemService.getItem(TEST_ITEM_ID.toString())).thenReturn(mockItem);
        
        try {
            commentCommand.setItemId("WI-123");
            commentCommand.setComment(TEST_COMMENT);
            
            // Act
            int result = commentCommand.call();
            
            // Assert
            assertEquals(0, result, "Command should succeed");
            
            // Verify short ID resolution
            verify(mockItemService).findItemByShortId("WI-123");
            
            // Verify comment was added to the resolved item
            verify(mockCommentService).addComment(TEST_ITEM_ID, TEST_USER, TEST_COMMENT);
        } catch (IllegalArgumentException e) {
            // This should not happen if mocking is set up correctly
            throw new AssertionError("Short ID resolution failed: " + e.getMessage());
        }
    }
    
    // Helper methods
    
    private WorkItem createMockWorkItem(String id, String title, WorkflowState state) {
        WorkItem item = new WorkItem();
        item.setId(id);
        item.setTitle(title);
        item.setState(state);
        item.setType(WorkItemType.TASK);
        return item;
    }
    
    private List<CommentImpl> createMockComments(UUID itemId) {
        List<CommentImpl> comments = new ArrayList<>();
        
        // Add standard comment
        comments.add(new CommentImpl(
            UUID.randomUUID(),
            itemId,
            TEST_USER,
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