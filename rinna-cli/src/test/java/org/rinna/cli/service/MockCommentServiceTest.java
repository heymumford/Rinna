/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.rinna.cli.domain.model.Comment;
import org.rinna.cli.domain.model.CommentType;
import org.rinna.cli.domain.model.WorkflowState;
import org.rinna.cli.service.MockCommentService.CommentImpl;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MockCommentService Tests")
class MockCommentServiceTest {
    private MockCommentService commentService;
    private UUID testWorkItemId;
    
    @BeforeEach
    void setUp() {
        commentService = new MockCommentService();
        // Using the same UUID that's initialized in the MockCommentService constructor
        testWorkItemId = UUID.fromString("123e4567-e89b-12d3-a456-426614174000");
    }
    
    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {
        @Test
        @DisplayName("Should initialize with sample comments")
        void shouldInitializeWithSampleComments() {
            // When
            List<CommentImpl> comments = commentService.getComments(testWorkItemId);
            
            // Then
            assertFalse(comments.isEmpty(), "Should have sample comments");
            assertEquals(2, comments.size(), "Should have exactly 2 sample comments");
            
            // Verify types
            boolean hasStandardComment = comments.stream()
                .anyMatch(c -> c.type() == CommentType.STANDARD);
            boolean hasSystemComment = comments.stream()
                .anyMatch(c -> c.type() == CommentType.SYSTEM);
                
            assertTrue(hasStandardComment, "Should have a standard comment");
            assertTrue(hasSystemComment, "Should have a system comment");
            
            // Verify content
            boolean hasAuthFeatureComment = comments.stream()
                .anyMatch(c -> c.text().contains("authentication feature"));
            boolean hasStatusChangeComment = comments.stream()
                .anyMatch(c -> c.text().contains("Status changed"));
                
            assertTrue(hasAuthFeatureComment, "Should have authentication feature comment");
            assertTrue(hasStatusChangeComment, "Should have status change comment");
        }
    }
    
    @Nested
    @DisplayName("Comment Creation Tests")
    class CommentCreationTests {
        @Test
        @DisplayName("Should create standard comment")
        void shouldCreateStandardComment() {
            // Given
            UUID workItemId = UUID.randomUUID();
            String user = "testuser";
            String text = "This is a test comment";
            
            // When
            Comment comment = commentService.createStandard(workItemId, user, text);
            
            // Then
            assertNotNull(comment);
            assertEquals(workItemId, comment.workItemId());
            assertEquals(user, comment.user());
            assertEquals(text, comment.text());
            assertEquals(CommentType.STANDARD, comment.type());
            assertNotNull(comment.id(), "Should generate a UUID");
            assertNotNull(comment.timestamp(), "Should set timestamp");
        }
        
        @Test
        @DisplayName("Should create transition comment")
        void shouldCreateTransitionComment() {
            // Given
            UUID workItemId = UUID.randomUUID();
            String user = "testuser";
            WorkflowState fromState = WorkflowState.READY;
            WorkflowState toState = WorkflowState.IN_PROGRESS;
            String reason = "Starting development";
            
            // When
            Comment comment = commentService.createTransition(workItemId, user, fromState, toState, reason);
            
            // Then
            assertNotNull(comment);
            assertEquals(workItemId, comment.workItemId());
            assertEquals(user, comment.user());
            assertEquals(CommentType.SYSTEM, comment.type());
            assertTrue(comment.text().contains("Status changed from READY to IN_PROGRESS: Starting development"));
            assertNotNull(comment.id(), "Should generate a UUID");
            assertNotNull(comment.timestamp(), "Should set timestamp");
        }
        
        @Test
        @DisplayName("Should create comment with specified type")
        void shouldCreateCommentWithSpecifiedType() {
            // Given
            UUID workItemId = UUID.randomUUID();
            String user = "testuser";
            String text = "This is code: `print('hello world')`";
            CommentType type = CommentType.CODE;
            
            // When
            Comment comment = commentService.create(workItemId, user, text, type);
            
            // Then
            assertNotNull(comment);
            assertEquals(workItemId, comment.workItemId());
            assertEquals(user, comment.user());
            assertEquals(text, comment.text());
            assertEquals(type, comment.type());
        }
    }
    
    @Nested
    @DisplayName("Comment Addition Tests")
    class CommentAdditionTests {
        @Test
        @DisplayName("Should add comment to work item")
        void shouldAddCommentToWorkItem() {
            // Given
            UUID workItemId = UUID.randomUUID();
            String author = "testuser";
            String text = "This is a new comment";
            CommentType type = CommentType.STANDARD;
            
            // When
            Comment addedComment = commentService.addComment(workItemId, author, text, type);
            List<CommentImpl> comments = commentService.getComments(workItemId);
            
            // Then
            assertNotNull(addedComment);
            assertEquals(1, comments.size());
            assertEquals(addedComment.id(), comments.get(0).id());
            assertEquals(text, comments.get(0).text());
        }
        
        @Test
        @DisplayName("Should add standard comment with simplified method")
        void shouldAddStandardCommentWithSimplifiedMethod() {
            // Given
            UUID workItemId = UUID.randomUUID();
            String author = "testuser";
            String text = "This is a standard comment";
            
            // When
            commentService.addComment(workItemId, author, text);
            List<CommentImpl> comments = commentService.getComments(workItemId);
            
            // Then
            assertEquals(1, comments.size());
            assertEquals(text, comments.get(0).text());
            assertEquals(CommentType.STANDARD, comments.get(0).type());
        }
        
        @Test
        @DisplayName("Should add transition comment")
        void shouldAddTransitionComment() {
            // Given
            UUID workItemId = UUID.randomUUID();
            String author = "testuser";
            WorkflowState fromState = WorkflowState.READY;
            WorkflowState toState = WorkflowState.IN_PROGRESS;
            String comment = "Starting work";
            
            // When
            Comment addedComment = commentService.addTransitionComment(
                workItemId, author, fromState, toState, comment);
            List<CommentImpl> comments = commentService.getComments(workItemId);
            
            // Then
            assertNotNull(addedComment);
            assertEquals(1, comments.size());
            assertEquals(addedComment.id(), comments.get(0).id());
            assertTrue(comments.get(0).text().contains("Status changed from READY to IN_PROGRESS"));
            assertEquals(CommentType.SYSTEM, comments.get(0).type());
        }
        
        @Test
        @DisplayName("Should add transition comment with string states")
        void shouldAddTransitionCommentWithStringStates() {
            // Given
            UUID workItemId = UUID.randomUUID();
            String author = "testuser";
            String fromState = "READY";
            String toState = "IN_PROGRESS";
            String comment = "Starting work";
            
            // When
            Comment addedComment = commentService.addTransitionComment(
                workItemId, author, fromState, toState, comment);
            List<CommentImpl> comments = commentService.getComments(workItemId);
            
            // Then
            assertNotNull(addedComment);
            assertEquals(1, comments.size());
            assertEquals(addedComment.id(), comments.get(0).id());
            assertTrue(comments.get(0).text().contains("Status changed from READY to IN_PROGRESS"));
            assertEquals(CommentType.SYSTEM, comments.get(0).type());
        }
    }
    
    @Nested
    @DisplayName("Comment Retrieval Tests")
    class CommentRetrievalTests {
        private UUID workItemId;
        private UUID commentId;
        
        @BeforeEach
        void setupComments() {
            workItemId = UUID.randomUUID();
            
            // Add several comments with different timestamps
            Comment comment1 = commentService.addComment(workItemId, "user1", "Comment 1", CommentType.STANDARD);
            commentId = comment1.id();
            
            // Add a comment from 1 hour ago
            Instant hourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
            CommentImpl comment2 = new MockCommentService.CommentImpl(
                UUID.randomUUID(), workItemId, "user2", hourAgo, "Comment 2", CommentType.STANDARD);
            commentService.getComment(comment2.id()); // Force initialization
            commentService.addComment(workItemId, "user2", "Comment 2"); // This will have a fresh timestamp
            
            // Add a comment from 3 days ago
            Instant threeDaysAgo = Instant.now().minus(3, ChronoUnit.DAYS);
            CommentImpl comment3 = new MockCommentService.CommentImpl(
                UUID.randomUUID(), workItemId, "user3", threeDaysAgo, "Comment 3", CommentType.SYSTEM);
            commentService.getComment(comment3.id()); // Force initialization
            commentService.addComment(workItemId, "user3", "Comment 3", CommentType.SYSTEM);
        }
        
        @Test
        @DisplayName("Should get comment by ID")
        void shouldGetCommentById() {
            // When
            Optional<Comment> comment = commentService.getComment(commentId);
            
            // Then
            assertTrue(comment.isPresent());
            assertEquals("Comment 1", comment.get().text());
            assertEquals("user1", comment.get().user());
        }
        
        @Test
        @DisplayName("Should return empty Optional for non-existent comment")
        void shouldReturnEmptyOptionalForNonExistentComment() {
            // When
            Optional<Comment> comment = commentService.getComment(UUID.randomUUID());
            
            // Then
            assertFalse(comment.isPresent());
        }
        
        @Test
        @DisplayName("Should get all comments for work item")
        void shouldGetAllCommentsForWorkItem() {
            // When
            List<CommentImpl> comments = commentService.getComments(workItemId);
            
            // Then
            assertEquals(3, comments.size());
            
            // Comments should be in reverse chronological order (most recent first)
            assertTrue(comments.get(0).timestamp().isAfter(comments.get(1).timestamp()));
            assertTrue(comments.get(1).timestamp().isAfter(comments.get(2).timestamp()));
        }
        
        @Test
        @DisplayName("Should get comments in time range")
        void shouldGetCommentsInTimeRange() {
            // Given
            Instant now = Instant.now();
            Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
            
            // When
            List<CommentImpl> recentComments = commentService.getCommentsInTimeRange(workItemId, twoDaysAgo, now);
            
            // Then: Should get only the comments from the last 2 days
            assertTrue(recentComments.size() < 3);
            
            for (CommentImpl comment : recentComments) {
                assertTrue(comment.timestamp().isAfter(twoDaysAgo) || comment.timestamp().equals(twoDaysAgo));
                assertTrue(comment.timestamp().isBefore(now) || comment.timestamp().equals(now));
            }
        }
        
        @Test
        @DisplayName("Should get comments from last hours")
        void shouldGetCommentsFromLastHours() {
            // When
            List<CommentImpl> lastHourComments = commentService.getCommentsFromLastHours(workItemId, 1);
            List<CommentImpl> last24HoursComments = commentService.getCommentsFromLastHours(workItemId, 24);
            
            // Then
            assertTrue(lastHourComments.size() <= last24HoursComments.size());
            
            Instant oneHourAgo = Instant.now().minus(1, ChronoUnit.HOURS);
            for (CommentImpl comment : lastHourComments) {
                assertTrue(comment.timestamp().isAfter(oneHourAgo) || comment.timestamp().equals(oneHourAgo));
            }
        }
        
        @Test
        @DisplayName("Should get comments from last days")
        void shouldGetCommentsFromLastDays() {
            // When
            List<CommentImpl> lastDayComments = commentService.getCommentsFromLastDays(workItemId, 1);
            List<CommentImpl> last7DaysComments = commentService.getCommentsFromLastDays(workItemId, 7);
            
            // Then
            assertTrue(lastDayComments.size() <= last7DaysComments.size());
            
            Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
            for (CommentImpl comment : lastDayComments) {
                assertTrue(comment.timestamp().isAfter(oneDayAgo) || comment.timestamp().equals(oneDayAgo));
            }
        }
        
        @Test
        @DisplayName("Should get comments from last weeks")
        void shouldGetCommentsFromLastWeeks() {
            // When
            List<CommentImpl> lastWeekComments = commentService.getCommentsFromLastWeeks(workItemId, 1);
            
            // Then
            Instant oneWeekAgo = Instant.now().minus(7, ChronoUnit.DAYS);
            for (CommentImpl comment : lastWeekComments) {
                assertTrue(comment.timestamp().isAfter(oneWeekAgo) || comment.timestamp().equals(oneWeekAgo));
            }
        }
    }
    
    @Nested
    @DisplayName("Comment Deletion Tests")
    class CommentDeletionTests {
        @Test
        @DisplayName("Should delete comment by ID")
        void shouldDeleteCommentById() {
            // Given
            UUID workItemId = UUID.randomUUID();
            Comment comment = commentService.addComment(workItemId, "user1", "Test comment");
            UUID commentId = comment.id();
            
            // When
            boolean deleted = commentService.deleteComment(commentId);
            Optional<Comment> retrievedComment = commentService.getComment(commentId);
            
            // Then
            assertTrue(deleted);
            assertFalse(retrievedComment.isPresent());
        }
        
        @Test
        @DisplayName("Should return false when deleting non-existent comment")
        void shouldReturnFalseWhenDeletingNonExistentComment() {
            // When
            boolean deleted = commentService.deleteComment(UUID.randomUUID());
            
            // Then
            assertFalse(deleted);
        }
    }
}