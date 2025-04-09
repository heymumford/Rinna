/*
 * Test class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rinna.domain.model.Comment;
import org.rinna.domain.model.CommentRecord;
import org.rinna.domain.model.CommentType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.repository.CommentRepository;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for the InMemoryCommentRepository.
 */
public class InMemoryCommentRepositoryTest {

    private CommentRepository repository;
    private UUID workItem1Id;
    private UUID workItem2Id;
    private UUID commentId;
    private Instant now;

    @BeforeEach
    void setUp() {
        repository = new InMemoryCommentRepository();
        workItem1Id = UUID.randomUUID();
        workItem2Id = UUID.randomUUID();
        now = Instant.now();
    }

    @Test
    void testSaveAndFindById() {
        // Create a comment
        Comment comment = CommentRecord.createStandard(workItem1Id, "alice", "Test comment");
        
        // Save the comment
        Comment savedComment = repository.save(comment);
        assertNotNull(savedComment);
        assertEquals(comment.id(), savedComment.id());
        
        // Find the comment by ID
        Optional<Comment> foundComment = repository.findById(comment.id());
        assertTrue(foundComment.isPresent());
        assertEquals(comment.id(), foundComment.get().id());
        assertEquals(comment.text(), foundComment.get().text());
        assertEquals(comment.author(), foundComment.get().author());
        assertEquals(comment.workItemId(), foundComment.get().workItemId());
        assertEquals(comment.type(), foundComment.get().type());
    }
    
    @Test
    void testFindByIdNotFound() {
        Optional<Comment> notFoundComment = repository.findById(UUID.randomUUID());
        assertFalse(notFoundComment.isPresent());
    }
    
    @Test
    void testFindByWorkItemId() {
        // Create and save multiple comments for the same work item
        Comment comment1 = CommentRecord.createStandard(workItem1Id, "alice", "First comment");
        Comment comment2 = CommentRecord.createStandard(workItem1Id, "bob", "Second comment");
        Comment comment3 = CommentRecord.createStandard(workItem2Id, "charlie", "Comment for another work item");
        
        repository.save(comment1);
        repository.save(comment2);
        repository.save(comment3);
        
        // Find all comments for work item 1
        List<Comment> workItem1Comments = repository.findByWorkItemId(workItem1Id);
        assertEquals(2, workItem1Comments.size());
        assertTrue(workItem1Comments.stream().anyMatch(c -> c.id().equals(comment1.id())));
        assertTrue(workItem1Comments.stream().anyMatch(c -> c.id().equals(comment2.id())));
        
        // Find all comments for work item 2
        List<Comment> workItem2Comments = repository.findByWorkItemId(workItem2Id);
        assertEquals(1, workItem2Comments.size());
        assertEquals(comment3.id(), workItem2Comments.get(0).id());
    }
    
    @Test
    void testFindByWorkItemIdAndType() {
        // Create and save comments of different types
        Comment standardComment = CommentRecord.createStandard(workItem1Id, "alice", "Standard comment");
        Comment systemComment = CommentRecord.createSystem(workItem1Id, "System notification");
        Comment transitionComment = CommentRecord.createTransition(workItem1Id, "bob", 
                WorkflowState.TO_DO, WorkflowState.IN_PROGRESS, "Started work");
        
        repository.save(standardComment);
        repository.save(systemComment);
        repository.save(transitionComment);
        
        // Find comments by type
        List<Comment> standardComments = repository.findByWorkItemIdAndType(workItem1Id, CommentType.STANDARD);
        assertEquals(1, standardComments.size());
        assertEquals(standardComment.id(), standardComments.get(0).id());
        
        List<Comment> systemComments = repository.findByWorkItemIdAndType(workItem1Id, CommentType.SYSTEM);
        assertEquals(1, systemComments.size());
        assertEquals(systemComment.id(), systemComments.get(0).id());
        
        List<Comment> transitionComments = repository.findByWorkItemIdAndType(workItem1Id, CommentType.TRANSITION);
        assertEquals(1, transitionComments.size());
        assertEquals(transitionComment.id(), transitionComments.get(0).id());
        
        // Verify no comments of a type that doesn't exist
        List<Comment> metadataComments = repository.findByWorkItemIdAndType(workItem1Id, CommentType.METADATA_CHANGE);
        assertTrue(metadataComments.isEmpty());
    }
    
    @Test
    void testFindByWorkItemIdAndTimeRange() {
        // Create comments with specific timestamps
        Instant now = Instant.now();
        Instant yesterday = now.minus(1, ChronoUnit.DAYS);
        Instant twoDaysAgo = now.minus(2, ChronoUnit.DAYS);
        Instant threeDaysAgo = now.minus(3, ChronoUnit.DAYS);
        
        Comment recentComment = new CommentRecord(
                UUID.randomUUID(), workItem1Id, "alice", "Recent comment", now, CommentType.STANDARD);
        
        Comment yesterdayComment = new CommentRecord(
                UUID.randomUUID(), workItem1Id, "bob", "Yesterday comment", yesterday, CommentType.STANDARD);
        
        Comment oldComment = new CommentRecord(
                UUID.randomUUID(), workItem1Id, "charlie", "Old comment", twoDaysAgo, CommentType.STANDARD);
        
        Comment veryOldComment = new CommentRecord(
                UUID.randomUUID(), workItem1Id, "dave", "Very old comment", threeDaysAgo, CommentType.STANDARD);
        
        repository.save(recentComment);
        repository.save(yesterdayComment);
        repository.save(oldComment);
        repository.save(veryOldComment);
        
        // Find comments in different time ranges
        List<Comment> dayComments = repository.findByWorkItemIdAndTimeRange(
                workItem1Id, yesterday.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
        assertEquals(2, dayComments.size());
        assertTrue(dayComments.stream().anyMatch(c -> c.id().equals(recentComment.id())));
        assertTrue(dayComments.stream().anyMatch(c -> c.id().equals(yesterdayComment.id())));
        
        List<Comment> weekComments = repository.findByWorkItemIdAndTimeRange(
                workItem1Id, threeDaysAgo.minus(1, ChronoUnit.HOURS), now.plus(1, ChronoUnit.HOURS));
        assertEquals(4, weekComments.size());
    }
    
    @Test
    void testFindByAuthor() {
        // Create comments from different authors
        Comment aliceComment1 = CommentRecord.createStandard(workItem1Id, "alice", "Alice's first comment");
        Comment aliceComment2 = CommentRecord.createStandard(workItem2Id, "alice", "Alice's second comment");
        Comment bobComment = CommentRecord.createStandard(workItem1Id, "bob", "Bob's comment");
        
        repository.save(aliceComment1);
        repository.save(aliceComment2);
        repository.save(bobComment);
        
        // Find all comments by Alice
        List<Comment> aliceComments = repository.findByAuthor("alice");
        assertEquals(2, aliceComments.size());
        assertTrue(aliceComments.stream().anyMatch(c -> c.id().equals(aliceComment1.id())));
        assertTrue(aliceComments.stream().anyMatch(c -> c.id().equals(aliceComment2.id())));
        
        // Find all comments by Bob
        List<Comment> bobComments = repository.findByAuthor("bob");
        assertEquals(1, bobComments.size());
        assertEquals(bobComment.id(), bobComments.get(0).id());
        
        // Find comments for non-existent author
        List<Comment> charlieComments = repository.findByAuthor("charlie");
        assertTrue(charlieComments.isEmpty());
    }
    
    @Test
    void testDeleteById() {
        // Create and save a comment
        Comment comment = CommentRecord.createStandard(workItem1Id, "alice", "Test comment");
        repository.save(comment);
        
        // Verify the comment exists
        assertTrue(repository.findById(comment.id()).isPresent());
        
        // Delete the comment
        boolean deleted = repository.deleteById(comment.id());
        assertTrue(deleted);
        
        // Verify the comment was deleted
        assertFalse(repository.findById(comment.id()).isPresent());
        
        // Try to delete a non-existent comment
        boolean nonExistentDeleted = repository.deleteById(UUID.randomUUID());
        assertFalse(nonExistentDeleted);
    }
    
    @Test
    void testDeleteByWorkItemId() {
        // Create and save multiple comments for different work items
        Comment comment1 = CommentRecord.createStandard(workItem1Id, "alice", "First comment for work item 1");
        Comment comment2 = CommentRecord.createStandard(workItem1Id, "bob", "Second comment for work item 1");
        Comment comment3 = CommentRecord.createStandard(workItem2Id, "charlie", "Comment for work item 2");
        
        repository.save(comment1);
        repository.save(comment2);
        repository.save(comment3);
        
        // Verify all comments exist
        assertEquals(2, repository.findByWorkItemId(workItem1Id).size());
        assertEquals(1, repository.findByWorkItemId(workItem2Id).size());
        
        // Delete all comments for work item 1
        int deleted = repository.deleteByWorkItemId(workItem1Id);
        assertEquals(2, deleted);
        
        // Verify comments for work item 1 were deleted
        assertTrue(repository.findByWorkItemId(workItem1Id).isEmpty());
        
        // Verify comments for work item 2 still exist
        assertEquals(1, repository.findByWorkItemId(workItem2Id).size());
        
        // Try to delete comments for a work item with no comments
        int nonExistentDeleted = repository.deleteByWorkItemId(UUID.randomUUID());
        assertEquals(0, nonExistentDeleted);
    }
    
    @Test
    void testSaveUpdatesExistingComment() {
        // Create and save a comment
        Comment originalComment = CommentRecord.createStandard(workItem1Id, "alice", "Original text");
        repository.save(originalComment);
        
        // Create a new comment with the same ID but different text
        Comment updatedComment = new CommentRecord(
                originalComment.id(),
                originalComment.workItemId(),
                originalComment.author(),
                "Updated text",
                originalComment.timestamp(),
                originalComment.type()
        );
        
        // Save the updated comment
        repository.save(updatedComment);
        
        // Verify the comment was updated
        Optional<Comment> foundComment = repository.findById(originalComment.id());
        assertTrue(foundComment.isPresent());
        assertEquals("Updated text", foundComment.get().text());
    }
    
    @Test
    void testCommentSorting() {
        // Create comments with specific timestamps
        Instant now = Instant.now();
        Instant oneHourAgo = now.minus(1, ChronoUnit.HOURS);
        Instant twoHoursAgo = now.minus(2, ChronoUnit.HOURS);
        
        Comment comment1 = new CommentRecord(
                UUID.randomUUID(), workItem1Id, "alice", "Recent comment", now, CommentType.STANDARD);
        
        Comment comment2 = new CommentRecord(
                UUID.randomUUID(), workItem1Id, "bob", "One hour old comment", oneHourAgo, CommentType.STANDARD);
        
        Comment comment3 = new CommentRecord(
                UUID.randomUUID(), workItem1Id, "charlie", "Two hours old comment", twoHoursAgo, CommentType.STANDARD);
        
        // Save in reverse chronological order
        repository.save(comment3);
        repository.save(comment2);
        repository.save(comment1);
        
        // Verify comments are returned in most recent first order
        List<Comment> comments = repository.findByWorkItemId(workItem1Id);
        assertEquals(3, comments.size());
        assertEquals(comment1.id(), comments.get(0).id()); // Most recent first
        assertEquals(comment2.id(), comments.get(1).id());
        assertEquals(comment3.id(), comments.get(2).id());
    }
}