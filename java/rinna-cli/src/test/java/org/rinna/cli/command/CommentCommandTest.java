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
import java.time.Instant;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.domain.model.CommentType;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;
import org.rinna.cli.service.MockCommentService.CommentImpl;

/**
 * Test class for CommentCommand with focus on MetadataService integration.
 * Tests follow the ViewCommand pattern test approach.
 */
@ExtendWith(MockitoExtension.class)
public class CommentCommandTest {

    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private MockCommentService mockCommentService;
    
    @Mock
    private MockItemService mockItemService;
    
    @Mock
    private MockWorkflowService mockWorkflowService;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private ContextManager mockContextManager;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> resultCaptor;
    
    private CommentCommand command;
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private static final String TEST_ITEM_ID = "550e8400-e29b-41d4-a716-446655440000";
    private static final String TEST_USER = "testuser";
    private static final String TEST_COMMENT = "This is a test comment";
    private static final String MOCK_OPERATION_ID = "op-123";
    
    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Setup mock service dependencies
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getMockCommentService()).thenReturn(mockCommentService);
        when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockWorkflowService()).thenReturn(mockWorkflowService);
        
        // Setup metadata service tracking
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn(MOCK_OPERATION_ID);
        
        // Create the command with mocked dependencies
        command = new CommentCommand(mockServiceManager);
        command.setUsername(TEST_USER);
    }
    
    /**
     * Test successful operation tracking.
     */
    @Nested
    @DisplayName("Operation Tracking Tests")
    class OperationTrackingTests {
        
        @Test
        @DisplayName("Should track main operation when adding comment")
        void shouldTrackMainOperationWhenAddingComment() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            command.setComment(TEST_COMMENT);
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Verify main operation tracking
            verify(mockMetadataService).startOperation(eq("comment"), eq("CREATE"), paramsCaptor.capture());
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(TEST_USER, params.get("username"));
            assertEquals(TEST_ITEM_ID, params.get("itemId"));
            assertTrue((Boolean) params.get("hasComment"));
            assertEquals(TEST_COMMENT.length(), params.get("commentLength"));
            
            // Verify operation completion tracking
            verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
            Map<String, Object> result_data = resultCaptor.getValue();
            assertTrue((Boolean) result_data.get("success"));
            assertEquals(TEST_ITEM_ID, result_data.get("itemId"));
        }
        
        @Test
        @DisplayName("Should track hierarchical operations when adding comment")
        void shouldTrackHierarchicalOperationsWhenAddingComment() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            command.setComment(TEST_COMMENT);
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Verify sub-operation tracking for comment-add
            verify(mockMetadataService).startOperation(eq("comment-add"), eq("CREATE"), paramsCaptor.capture());
            Map<String, Object> addParams = paramsCaptor.getValue();
            assertEquals(TEST_ITEM_ID, addParams.get("itemId"));
            assertEquals(TEST_USER, addParams.get("username"));
            
            // Verify comment operation completion
            verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), any());
        }
        
        @Test
        @DisplayName("Should track detailed result operation when verbose is enabled")
        void shouldTrackDetailedResultOperationWhenVerboseIsEnabled() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            command.setComment(TEST_COMMENT);
            command.setVerbose(true);
            
            // Setup comments
            List<CommentImpl> comments = Arrays.asList(
                new CommentImpl(UUID.randomUUID(), UUID.fromString(TEST_ITEM_ID), TEST_USER, Instant.now(), "First comment", CommentType.STANDARD),
                new CommentImpl(UUID.randomUUID(), UUID.fromString(TEST_ITEM_ID), TEST_USER, Instant.now(), TEST_COMMENT, CommentType.STANDARD)
            );
            when(mockCommentService.getComments(any(UUID.class))).thenReturn(comments);
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Verify comment-list operation tracking
            verify(mockMetadataService).startOperation(eq("comment-list"), eq("READ"), paramsCaptor.capture());
            Map<String, Object> listParams = paramsCaptor.getValue();
            assertEquals(TEST_ITEM_ID, listParams.get("itemId"));
            
            // Verify comment-list operation completion
            verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), resultCaptor.capture());
            Map<String, Object> finalResult = resultCaptor.getValue();
            assertTrue((Boolean) finalResult.get("success"));
            assertEquals(TEST_ITEM_ID, finalResult.get("itemId"));
            assertEquals(2, finalResult.get("commentCount"));
        }
        
        @Test
        @DisplayName("Should track item resolution operation when item ID is not provided")
        void shouldTrackItemResolutionOperationWhenItemIdIsNotProvided() {
            // Setup
            command.setComment(TEST_COMMENT);
            
            // Setup a work item in progress
            WorkItem workItem = new WorkItem();
            workItem.setId(TEST_ITEM_ID);
            workItem.setTitle("Test Item");
            workItem.setState(WorkflowState.IN_PROGRESS);
            when(mockWorkflowService.getCurrentWorkInProgress(TEST_USER)).thenReturn(Optional.of(workItem));
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Verify item resolution operation tracking
            verify(mockMetadataService).startOperation(eq("comment-resolve-item"), eq("READ"), paramsCaptor.capture());
            Map<String, Object> resolveParams = paramsCaptor.getValue();
            assertEquals(TEST_USER, resolveParams.get("username"));
            
            // Verify item resolution operation completion
            verify(mockMetadataService).completeOperation(eq(MOCK_OPERATION_ID), any());
        }
        
        @Test
        @DisplayName("Should track operation failure when validation fails")
        void shouldTrackOperationFailureWhenValidationFails() {
            // Setup - missing comment text
            command.setItemId(TEST_ITEM_ID);
            // No comment text set
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(1, result);
            
            // Verify operation failure tracking
            verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalArgumentException.class));
            
            // Check error output
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("Error: Invalid work item ID"));
        }
        
        @Test
        @DisplayName("Should track operation failure when no work item is in progress")
        void shouldTrackOperationFailureWhenNoWorkItemIsInProgress() {
            // Setup
            command.setComment(TEST_COMMENT);
            // No item ID and no work in progress
            when(mockWorkflowService.getCurrentWorkInProgress(TEST_USER)).thenReturn(Optional.empty());
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(1, result);
            
            // Verify resolve operation failure tracking
            verify(mockMetadataService).startOperation(eq("comment-resolve-item"), eq("READ"), any());
            verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(IllegalArgumentException.class));
            
            // Check error output
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("No work item is currently in progress"));
        }
        
        @Test
        @DisplayName("Should track operation failure when retrieving comments fails")
        void shouldTrackOperationFailureWhenRetrievingCommentsFails() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            command.setComment(TEST_COMMENT);
            command.setVerbose(true);
            
            // Setup exception
            when(mockCommentService.getComments(any(UUID.class))).thenThrow(new RuntimeException("Failed to retrieve comments"));
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(1, result);
            
            // Verify comment-list operation failure tracking
            verify(mockMetadataService).startOperation(eq("comment-list"), eq("READ"), any());
            verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(RuntimeException.class));
            
            // Check error output
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("Error retrieving comments"));
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
            command.setComment(TEST_COMMENT);
            command.setFormat("json");
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Check JSON output
            String output = outContent.toString();
            assertTrue(output.contains("\"success\": true"));
            assertTrue(output.contains("\"workItemId\": \"" + TEST_ITEM_ID + "\""));
            assertTrue(output.contains("\"action\": \"comment_added\""));
        }
        
        @Test
        @DisplayName("Should output detailed JSON when verbose is enabled")
        void shouldOutputDetailedJsonWhenVerboseIsEnabled() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            command.setComment(TEST_COMMENT);
            command.setFormat("json");
            command.setVerbose(true);
            
            // Setup comments
            UUID commentId = UUID.randomUUID();
            List<CommentImpl> comments = Arrays.asList(
                new CommentImpl(commentId, UUID.fromString(TEST_ITEM_ID), TEST_USER, Instant.now(), TEST_COMMENT, CommentType.STANDARD)
            );
            when(mockCommentService.getComments(any(UUID.class))).thenReturn(comments);
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Check JSON output
            String output = outContent.toString();
            assertTrue(output.contains("\"success\": true"));
            assertTrue(output.contains("\"commentCount\": 1"));
            assertTrue(output.contains("\"comments\": ["));
            assertTrue(output.contains("\"id\": \"" + commentId + "\""));
            assertTrue(output.contains("\"text\": \"" + TEST_COMMENT + "\""));
        }
        
        @Test
        @DisplayName("Should output JSON error when validation fails")
        void shouldOutputJsonErrorWhenValidationFails() {
            // Setup - missing comment text
            command.setItemId(TEST_ITEM_ID);
            command.setFormat("json");
            // No comment text set
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(1, result);
            
            // Check JSON error output
            String output = outContent.toString();
            assertTrue(output.contains("\"error\": \"Invalid work item ID"));
        }
    }
    
    /**
     * Test error handling with proper tracking.
     */
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should handle invalid item ID format")
        void shouldHandleInvalidItemIdFormat() {
            // Setup with invalid UUID
            command.setComment(TEST_COMMENT);
            when(mockItemService.findItemByShortId("invalid-id")).thenReturn(null);
            
            // This will throw an exception in setItemId
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                command.setItemId("invalid-id");
            });
            
            // Verify exception message
            assertTrue(exception.getMessage().contains("Invalid work item ID"));
        }
        
        @Test
        @DisplayName("Should handle short ID format")
        void shouldHandleShortIdFormat() {
            // Setup
            WorkItem workItem = new WorkItem();
            workItem.setId(TEST_ITEM_ID);
            workItem.setTitle("Test Item");
            
            when(mockItemService.findItemByShortId("WI-123")).thenReturn(workItem);
            
            // Set short ID
            command.setItemId("WI-123");
            command.setComment(TEST_COMMENT);
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(0, result);
            
            // Verify service interactions
            verify(mockCommentService).addComment(UUID.fromString(TEST_ITEM_ID), TEST_USER, TEST_COMMENT);
        }
        
        @Test
        @DisplayName("Should handle comment service exceptions")
        void shouldHandleCommentServiceExceptions() {
            // Setup
            command.setItemId(TEST_ITEM_ID);
            command.setComment(TEST_COMMENT);
            
            // Setup exception
            doThrow(new RuntimeException("Failed to add comment"))
                .when(mockCommentService).addComment(any(UUID.class), anyString(), anyString());
            
            // Execute
            int result = command.call();
            
            // Verify
            assertEquals(1, result);
            
            // Verify operation failure tracking
            verify(mockMetadataService).failOperation(eq(MOCK_OPERATION_ID), any(RuntimeException.class));
            
            // Check error output
            String errorOutput = errContent.toString();
            assertTrue(errorOutput.contains("Failed to add comment"));
        }
    }
    
}