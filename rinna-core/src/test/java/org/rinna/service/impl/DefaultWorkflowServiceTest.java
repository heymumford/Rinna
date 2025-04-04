/*
 * Service component for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rinna.adapter.persistence.InMemoryItemRepository;
import org.rinna.adapter.service.DefaultWorkflowService;
import org.rinna.domain.entity.DefaultWorkItem;
import org.rinna.domain.entity.Priority;
import org.rinna.domain.entity.WorkItem;
import org.rinna.domain.entity.WorkItemCreateRequest;
import org.rinna.domain.entity.WorkItemType;
import org.rinna.domain.entity.WorkflowState;
import org.rinna.domain.repository.ItemRepository;
import org.rinna.domain.usecase.InvalidTransitionException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DefaultWorkflowService class.
 */
public class DefaultWorkflowServiceTest {
    
    private ItemRepository itemRepository;
    private DefaultWorkflowService workflowService;
    private UUID testItemId;
    
    @BeforeEach
    public void setUp() {
        // Create a new repository and service for each test
        itemRepository = new InMemoryItemRepository();
        workflowService = new DefaultWorkflowService(itemRepository);
        
        // Create a test work item
        DefaultWorkItem testItem = new DefaultWorkItem(
                UUID.randomUUID(),
                "Test Item",
                "Test Description",
                WorkItemType.FEATURE,
                WorkflowState.FOUND,
                Priority.MEDIUM,
                "tester",
                Instant.now(),
                Instant.now(),
                null);
        
        itemRepository.save(testItem);
        testItemId = testItem.getId();
    }
    
    @Test
    public void testTransition() throws InvalidTransitionException {
        // Transition from FOUND to TRIAGED (valid)
        WorkItem updatedItem = workflowService.transition(testItemId, WorkflowState.TRIAGED);
        
        // Verify the status was updated
        assertEquals(WorkflowState.TRIAGED, updatedItem.getStatus());
        
        // Verify the item in the repository was also updated
        Optional<WorkItem> retrievedItem = itemRepository.findById(testItemId);
        assertTrue(retrievedItem.isPresent());
        assertEquals(WorkflowState.TRIAGED, retrievedItem.get().getStatus());
    }
    
    @Test
    public void testInvalidTransition() {
        // Transition from FOUND to IN_PROGRESS (invalid)
        assertThrows(InvalidTransitionException.class, () -> {
            workflowService.transition(testItemId, WorkflowState.IN_PROGRESS);
        });
        
        // Verify the item in the repository was not updated
        Optional<WorkItem> retrievedItem = itemRepository.findById(testItemId);
        assertTrue(retrievedItem.isPresent());
        assertEquals(WorkflowState.FOUND, retrievedItem.get().getStatus());
    }
    
    @Test
    public void testCanTransition() {
        // FOUND -> TRIAGED: Valid
        assertTrue(workflowService.canTransition(testItemId, WorkflowState.TRIAGED));
        
        // FOUND -> IN_PROGRESS: Invalid
        assertFalse(workflowService.canTransition(testItemId, WorkflowState.IN_PROGRESS));
        
        // Non-existent item
        assertFalse(workflowService.canTransition(UUID.randomUUID(), WorkflowState.TRIAGED));
    }
    
    @Test
    public void testGetAvailableTransitions() {
        // Get available transitions from FOUND
        List<WorkflowState> transitions = workflowService.getAvailableTransitions(testItemId);
        
        // Should only contain TRIAGED
        assertEquals(1, transitions.size());
        assertEquals(WorkflowState.TRIAGED, transitions.get(0));
        
        // Transition to TRIAGED
        try {
            workflowService.transition(testItemId, WorkflowState.TRIAGED);
        } catch (InvalidTransitionException e) {
            fail("Should be able to transition from FOUND to TRIAGED");
        }
        
        // Get available transitions from TRIAGED
        transitions = workflowService.getAvailableTransitions(testItemId);
        
        // Should contain TO_DO and DONE
        assertEquals(2, transitions.size());
        assertTrue(transitions.contains(WorkflowState.TO_DO));
        assertTrue(transitions.contains(WorkflowState.DONE));
    }
    
    @Test
    public void testNonExistentItem() {
        UUID nonExistentId = UUID.randomUUID();
        
        // Cannot transition for non-existent item
        assertFalse(workflowService.canTransition(nonExistentId, WorkflowState.TRIAGED));
        
        // No available transitions for non-existent item
        List<WorkflowState> transitions = workflowService.getAvailableTransitions(nonExistentId);
        assertTrue(transitions.isEmpty());
        
        // Transition throws IllegalArgumentException for non-existent item
        assertThrows(IllegalArgumentException.class, () -> {
            workflowService.transition(nonExistentId, WorkflowState.TRIAGED);
        });
    }
}