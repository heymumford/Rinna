package com.rinna.service.impl;

import com.rinna.model.WorkItem;
import com.rinna.model.WorkItemCreateRequest;
import com.rinna.model.WorkItemType;
import com.rinna.model.WorkflowState;
import com.rinna.service.InvalidTransitionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DefaultWorkflowServiceTest {

    private InMemoryItemService itemService;
    private DefaultWorkflowService workflowService;
    private WorkItem testItem;

    @BeforeEach
    void setUp() {
        itemService = new InMemoryItemService();
        workflowService = new DefaultWorkflowService(itemService);
        
        // Create a test item in the TO_DO state
        testItem = itemService.create(WorkItemCreateRequest.builder()
                .title("Test Bug")
                .type(WorkItemType.BUG)
                .build());
    }

    @Test
    void shouldTransitionToValidState() throws InvalidTransitionException {
        // When
        WorkItem updatedItem = workflowService.transition(testItem.getId(), WorkflowState.IN_PROGRESS);

        // Then
        assertThat(updatedItem.getStatus()).isEqualTo(WorkflowState.IN_PROGRESS);
    }

    @Test
    void shouldThrowExceptionForInvalidTransition() {
        // Then
        assertThrows(InvalidTransitionException.class, () ->
                workflowService.transition(testItem.getId(), WorkflowState.DONE));
    }

    @Test
    void shouldThrowExceptionForNonExistentItem() {
        // Given
        UUID nonExistentId = UUID.randomUUID();

        // Then
        assertThrows(IllegalArgumentException.class, () ->
                workflowService.transition(nonExistentId, WorkflowState.IN_PROGRESS));
    }

    @Test
    void shouldCheckIfTransitionIsPossible() {
        // When & Then
        assertThat(workflowService.canTransition(testItem.getId(), WorkflowState.IN_PROGRESS)).isTrue();
        assertThat(workflowService.canTransition(testItem.getId(), WorkflowState.DONE)).isFalse();
    }

    @Test
    void shouldReturnAvailableTransitions() {
        // When
        List<WorkflowState> availableTransitions = workflowService.getAvailableTransitions(testItem.getId());

        // Then
        assertThat(availableTransitions).containsExactly(WorkflowState.IN_PROGRESS);
    }

    @Test
    void shouldFollowCompleteWorkflow() throws InvalidTransitionException {
        // Create an item in FOUND state
        WorkItem item = itemService.create(WorkItemCreateRequest.builder()
                .title("Complete Workflow Test")
                .type(WorkItemType.BUG)
                .build());
        
        // Override the initial state to FOUND
        item = itemService.update(((com.rinna.model.DefaultWorkItem) item).withStatus(WorkflowState.FOUND));
        
        // Transition through the workflow
        item = workflowService.transition(item.getId(), WorkflowState.TRIAGED);
        assertThat(item.getStatus()).isEqualTo(WorkflowState.TRIAGED);
        
        item = workflowService.transition(item.getId(), WorkflowState.TO_DO);
        assertThat(item.getStatus()).isEqualTo(WorkflowState.TO_DO);
        
        item = workflowService.transition(item.getId(), WorkflowState.IN_PROGRESS);
        assertThat(item.getStatus()).isEqualTo(WorkflowState.IN_PROGRESS);
        
        item = workflowService.transition(item.getId(), WorkflowState.IN_TEST);
        assertThat(item.getStatus()).isEqualTo(WorkflowState.IN_TEST);
        
        item = workflowService.transition(item.getId(), WorkflowState.DONE);
        assertThat(item.getStatus()).isEqualTo(WorkflowState.DONE);
        
        // No further transitions possible
        assertThat(workflowService.getAvailableTransitions(item.getId())).isEmpty();
    }
}