/*
 * Unit test for the DefaultWorkflowService
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rinna.adapter.repository.InMemoryItemRepository;
import org.rinna.domain.model.CommentType;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.repository.CommentRepository;
import org.rinna.repository.HistoryRepository;
import org.rinna.repository.ItemRepository;
import org.rinna.usecase.CommentService;
import org.rinna.usecase.HistoryService;
import org.rinna.usecase.InvalidTransitionException;
import org.rinna.usecase.WorkflowService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link DefaultWorkflowService}.
 */
class DefaultWorkflowServiceTest {

    private ItemRepository itemRepository;
    private CommentService commentService;
    private HistoryService historyService;
    private WorkflowService workflowService;
    
    private UUID todoItemId;
    private UUID inProgressItemId;
    private WorkItem todoItem;
    private WorkItem inProgressItem;

    @BeforeEach
    void setUp() {
        // Create real repository
        itemRepository = new InMemoryItemRepository();
        
        // Create mock services for verification
        commentService = mock(CommentService.class);
        historyService = mock(HistoryService.class);
        
        // Create service under test
        workflowService = new DefaultWorkflowService(itemRepository, commentService, historyService);
        
        // Create test work items
        WorkItemCreateRequest todoRequest = new WorkItemCreateRequest.Builder()
                .title("Todo Item")
                .description("Item in TO_DO state")
                .type(WorkItemType.TASK)
                .status(WorkflowState.TO_DO)
                .priority(Priority.MEDIUM)
                .assignee("user1")
                .build();
        
        WorkItemCreateRequest inProgressRequest = new WorkItemCreateRequest.Builder()
                .title("In Progress Item")
                .description("Item in IN_PROGRESS state")
                .type(WorkItemType.TASK)
                .status(WorkflowState.IN_PROGRESS)
                .priority(Priority.MEDIUM)
                .assignee("user2")
                .build();
        
        // Save items to repository
        todoItem = itemRepository.create(todoRequest);
        todoItemId = todoItem.getId();
        
        inProgressItem = itemRepository.create(inProgressRequest);
        inProgressItemId = inProgressItem.getId();
    }

    @Test
    void testTransitionValidSimple() throws InvalidTransitionException {
        // Transition from TO_DO to IN_PROGRESS (valid transition)
        WorkItem transitionedItem = workflowService.transition(todoItemId, WorkflowState.IN_PROGRESS);
        
        // Verify transitioned item
        assertNotNull(transitionedItem);
        assertEquals(todoItemId, transitionedItem.getId());
        assertEquals(WorkflowState.IN_PROGRESS, transitionedItem.getStatus());
        
        // Verify by finding again
        Optional<WorkItem> foundItem = itemRepository.findById(todoItemId);
        assertTrue(foundItem.isPresent());
        assertEquals(WorkflowState.IN_PROGRESS, foundItem.get().getStatus());
        
        // Verify history was recorded
        verify(historyService).recordStateChange(
            eq(todoItemId),
            eq("System"),
            eq(WorkflowState.TO_DO.name()),
            eq(WorkflowState.IN_PROGRESS.name()),
            isNull()
        );
    }

    @Test
    void testTransitionWithUserAndComment() throws InvalidTransitionException {
        // Transition from TO_DO to IN_PROGRESS with user and comment
        WorkItem transitionedItem = workflowService.transition(
            todoItemId,
            "alice",
            WorkflowState.IN_PROGRESS,
            "Starting work on this"
        );
        
        // Verify transitioned item
        assertNotNull(transitionedItem);
        assertEquals(todoItemId, transitionedItem.getId());
        assertEquals(WorkflowState.IN_PROGRESS, transitionedItem.getStatus());
        
        // Verify comment was added
        verify(commentService).addTransitionComment(
            eq(todoItemId),
            eq("alice"),
            eq(WorkflowState.TO_DO.name()),
            eq(WorkflowState.IN_PROGRESS.name()),
            eq("Starting work on this")
        );
        
        // Verify history was recorded
        verify(historyService).recordStateChange(
            eq(todoItemId),
            eq("alice"),
            eq(WorkflowState.TO_DO.name()),
            eq(WorkflowState.IN_PROGRESS.name()),
            eq("Starting work on this")
        );
    }

    @Test
    void testTransitionInvalid() {
        // Attempt to transition from TO_DO to FOUND (invalid transition)
        InvalidTransitionException exception = assertThrows(
            InvalidTransitionException.class,
            () -> workflowService.transition(todoItemId, WorkflowState.FOUND)
        );
        
        // Verify exception details
        assertEquals(todoItemId, exception.getItemId());
        assertEquals(WorkflowState.TO_DO, exception.getCurrentState());
        assertEquals(WorkflowState.FOUND, exception.getTargetState());
        
        // Verify item was not changed
        Optional<WorkItem> foundItem = itemRepository.findById(todoItemId);
        assertTrue(foundItem.isPresent());
        assertEquals(WorkflowState.TO_DO, foundItem.get().getStatus());
        
        // Verify no history or comments were recorded
        verifyNoInteractions(commentService, historyService);
    }

    @Test
    void testTransitionWithInProgressLimit() throws InvalidTransitionException {
        // Create another TO_DO item for the same user
        WorkItemCreateRequest anotherRequest = new WorkItemCreateRequest.Builder()
                .title("Another Todo Item")
                .description("Another item in TO_DO state")
                .type(WorkItemType.TASK)
                .status(WorkflowState.TO_DO)
                .priority(Priority.MEDIUM)
                .assignee("user2") // Same as inProgressItem
                .build();
        
        WorkItem anotherItem = itemRepository.create(anotherRequest);
        
        // Attempt to transition to IN_PROGRESS (should fail since user already has a task in progress)
        InvalidTransitionException exception = assertThrows(
            InvalidTransitionException.class,
            () -> workflowService.transition(anotherItem.getId(), "user2", WorkflowState.IN_PROGRESS, null)
        );
        
        // Verify exception message mentions the already in-progress item
        assertTrue(exception.getMessage().contains(inProgressItemId.toString()));
        assertTrue(exception.getMessage().contains("already has work item"));
    }

    @Test
    void testCanTransition() {
        // Check valid transitions
        assertTrue(workflowService.canTransition(todoItemId, WorkflowState.IN_PROGRESS));
        assertTrue(workflowService.canTransition(todoItemId, WorkflowState.DONE));
        
        // Check invalid transitions
        assertFalse(workflowService.canTransition(todoItemId, WorkflowState.FOUND));
        assertFalse(workflowService.canTransition(todoItemId, WorkflowState.TRIAGED));
        assertFalse(workflowService.canTransition(todoItemId, WorkflowState.IN_TEST));
        assertFalse(workflowService.canTransition(todoItemId, WorkflowState.RELEASED));
        
        // Check non-existent item
        assertFalse(workflowService.canTransition(UUID.randomUUID(), WorkflowState.IN_PROGRESS));
    }

    @Test
    void testGetAvailableTransitions() {
        // Get available transitions for TO_DO item
        List<WorkflowState> todoTransitions = workflowService.getAvailableTransitions(todoItemId);
        
        // Verify transitions
        assertEquals(2, todoTransitions.size());
        assertTrue(todoTransitions.contains(WorkflowState.IN_PROGRESS));
        assertTrue(todoTransitions.contains(WorkflowState.DONE));
        
        // Get available transitions for IN_PROGRESS item
        List<WorkflowState> inProgressTransitions = workflowService.getAvailableTransitions(inProgressItemId);
        
        // Verify transitions
        assertEquals(2, inProgressTransitions.size());
        assertTrue(inProgressTransitions.contains(WorkflowState.IN_TEST));
        assertTrue(inProgressTransitions.contains(WorkflowState.TO_DO));
        
        // Check non-existent item
        List<WorkflowState> nonExistentTransitions = workflowService.getAvailableTransitions(UUID.randomUUID());
        assertTrue(nonExistentTransitions.isEmpty());
    }

    @Test
    void testGetCurrentWorkInProgress() {
        // Get current work in progress for user2 (who has an in-progress item)
        Optional<WorkItem> userWip = workflowService.getCurrentWorkInProgress("user2");
        
        // Verify result
        assertTrue(userWip.isPresent());
        assertEquals(inProgressItemId, userWip.get().getId());
        
        // Get current work in progress for user1 (who has no in-progress item)
        Optional<WorkItem> noWip = workflowService.getCurrentWorkInProgress("user1");
        
        // Verify result
        assertFalse(noWip.isPresent());
        
        // Get current work in progress for non-existent user
        Optional<WorkItem> nonExistentUserWip = workflowService.getCurrentWorkInProgress("non-existent");
        assertFalse(nonExistentUserWip.isPresent());
    }

    @Test
    void testAssignWorkItem() throws InvalidTransitionException {
        // Assign work item to a new user
        WorkItem assignedItem = workflowService.assignWorkItem(todoItemId, "manager", "alice");
        
        // Verify assigned item
        assertNotNull(assignedItem);
        assertEquals(todoItemId, assignedItem.getId());
        assertEquals("alice", assignedItem.getAssignee());
        
        // Verify by finding again
        Optional<WorkItem> foundItem = itemRepository.findById(todoItemId);
        assertTrue(foundItem.isPresent());
        assertEquals("alice", foundItem.get().getAssignee());
        
        // Verify history was recorded
        verify(historyService).recordFieldChange(
            eq(todoItemId),
            eq("manager"),
            eq("assignee"),
            eq("user1"),
            eq("alice")
        );
    }

    @Test
    void testAssignWorkItemWithComment() throws InvalidTransitionException {
        // Assign work item with comment
        WorkItem assignedItem = workflowService.assignWorkItem(
            todoItemId,
            "manager",
            "alice",
            "Please handle this task"
        );
        
        // Verify assigned item
        assertNotNull(assignedItem);
        assertEquals(todoItemId, assignedItem.getId());
        assertEquals("alice", assignedItem.getAssignee());
        
        // Verify comment was added
        verify(commentService).addComment(
            eq(todoItemId),
            eq("manager"),
            eq("Please handle this task"),
            eq(CommentType.ASSIGNMENT_CHANGE)
        );
        
        // Verify history was recorded
        verify(historyService).recordFieldChange(
            eq(todoItemId),
            eq("manager"),
            eq("assignee"),
            eq("user1"),
            eq("alice")
        );
    }

    @Test
    void testAssignWorkItemInProgress() {
        // Attempt to assign in-progress work item that's assigned to another user
        InvalidTransitionException exception = assertThrows(
            InvalidTransitionException.class,
            () -> workflowService.assignWorkItem(inProgressItemId, "manager", "alice")
        );
        
        // Verify exception message
        assertTrue(exception.getMessage().contains("Cannot reassign work item in progress"));
        assertTrue(exception.getMessage().contains("user2"));
        
        // Verify item was not changed
        Optional<WorkItem> foundItem = itemRepository.findById(inProgressItemId);
        assertTrue(foundItem.isPresent());
        assertEquals("user2", foundItem.get().getAssignee());
        
        // Verify no history or comments were recorded
        verifyNoInteractions(commentService, historyService);
    }

    @Test
    void testConstructorWithNullParameters() {
        // Attempt to create service with null repository
        assertThrows(NullPointerException.class, () -> 
            new DefaultWorkflowService(null, commentService, historyService));
        
        // Attempt to create service with null comment service
        assertThrows(NullPointerException.class, () -> 
            new DefaultWorkflowService(itemRepository, null, historyService));
        
        // Attempt to create service with null history service
        assertThrows(NullPointerException.class, () -> 
            new DefaultWorkflowService(itemRepository, commentService, null));
    }
}