/*
 * Test for the Rinna Clean Architecture implementation
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna;

import org.junit.jupiter.api.Test;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.usecase.InvalidTransitionException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for verifying that the Clean Architecture implementation works correctly.
 */
public class CleanArchitectureTest {

    @Test
    public void testCreateAndRetrieveWorkItem() {
        // Initialize Rinna with Clean Architecture structure
        Rinna rinna = Rinna.initialize();
        
        // Create a work item
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("Test Task")
                .description("This is a test task")
                .type(WorkItemType.FEATURE)
                .priority(Priority.MEDIUM)
                .assignee("testuser")
                .build();
        
        WorkItem createdItem = rinna.items().create(request);
        
        // Verify the item was created correctly
        assertNotNull(createdItem);
        assertEquals("Test Task", createdItem.getTitle());
        assertEquals("This is a test task", createdItem.getDescription());
        assertEquals(WorkItemType.FEATURE, createdItem.getType());
        assertEquals(Priority.MEDIUM, createdItem.getPriority());
        assertEquals("testuser", createdItem.getAssignee());
        assertEquals(WorkflowState.FOUND, createdItem.getStatus());
        
        // Retrieve the item and verify it's the same
        UUID itemId = createdItem.getId();
        WorkItem retrievedItem = rinna.items().findById(itemId).orElse(null);
        assertNotNull(retrievedItem);
        assertEquals(createdItem.getId(), retrievedItem.getId());
        assertEquals(createdItem.getTitle(), retrievedItem.getTitle());
    }
    
    @Test
    public void testWorkflowTransition() throws InvalidTransitionException {
        // Initialize Rinna with Clean Architecture structure
        Rinna rinna = Rinna.initialize();
        
        // Create a work item
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("Test Task")
                .type(WorkItemType.FEATURE)
                .build();
        
        WorkItem createdItem = rinna.items().create(request);
        UUID itemId = createdItem.getId();
        
        // Initial state should be FOUND
        assertEquals(WorkflowState.FOUND, createdItem.getStatus());
        
        // Valid transition: FOUND -> TRIAGED
        WorkItem triagedItem = rinna.workflow().transition(itemId, WorkflowState.TRIAGED);
        assertEquals(WorkflowState.TRIAGED, triagedItem.getStatus());
        
        // Valid transition: TRIAGED -> TO_DO
        WorkItem toDoItem = rinna.workflow().transition(itemId, WorkflowState.TO_DO);
        assertEquals(WorkflowState.TO_DO, toDoItem.getStatus());
        
        // Valid transition: TO_DO -> IN_PROGRESS
        WorkItem inProgressItem = rinna.workflow().transition(itemId, WorkflowState.IN_PROGRESS);
        assertEquals(WorkflowState.IN_PROGRESS, inProgressItem.getStatus());
        
        // Invalid transition: IN_PROGRESS -> TRIAGED
        assertThrows(InvalidTransitionException.class, () -> {
            rinna.workflow().transition(itemId, WorkflowState.TRIAGED);
        });
    }
    
    @Test
    public void testDependencyRule() {
        // This test verifies that the dependency rule of Clean Architecture is followed
        // Domain entities and use cases should not depend on adapters or framework
        
        // Create Rinna instance
        Rinna rinna = Rinna.initialize();
        
        // Create a work item using the use case
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("Test Task")
                .type(WorkItemType.FEATURE)
                .build();
        
        WorkItem createdItem = rinna.items().create(request);
        
        // Verify the returned item is from the domain, not an adapter type
        assertTrue(createdItem.getClass().getName().contains("domain.entity"));
        
        // Verify we can interact with the domain object
        assertEquals(WorkflowState.FOUND, createdItem.getStatus());
        assertEquals(WorkItemType.FEATURE, createdItem.getType());
    }
}