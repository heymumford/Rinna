/*
 * TDD example tests for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;

/**
 * Test-Driven Development examples for the Rinna workflow management system.
 */
public class TddTest {

    @Test
    public void testWorkItemCreation() {
        // Create a new work item
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("TDD Task")
                .description("This is a TDD test task")
                .type(WorkItemType.FEATURE)
                .priority(Priority.HIGH)
                .assignee("tdduser")
                .build();
        
        // Verify the request properties
        assertEquals("TDD Task", request.getTitle());
        assertEquals("This is a TDD test task", request.getDescription());
        assertEquals(WorkItemType.FEATURE, request.getType());
        assertEquals(Priority.HIGH, request.getPriority());
        assertEquals("tdduser", request.getAssignee());
    }
    
    @Test
    public void testWorkflowStates() {
        // Test workflow states enum
        for (WorkflowState state : WorkflowState.values()) {
            assertNotNull(state);
            assertNotNull(state.name());
        }
        
        // Test specific state transitions
        assertTrue(WorkflowState.FOUND.canTransitionTo(WorkflowState.TRIAGED));
        assertTrue(WorkflowState.TRIAGED.canTransitionTo(WorkflowState.TO_DO));
        assertTrue(WorkflowState.TO_DO.canTransitionTo(WorkflowState.IN_PROGRESS));
        assertTrue(WorkflowState.IN_PROGRESS.canTransitionTo(WorkflowState.DONE));
        
        // Test invalid transitions
        assertFalse(WorkflowState.DONE.canTransitionTo(WorkflowState.FOUND));
        assertFalse(WorkflowState.TRIAGED.canTransitionTo(WorkflowState.DONE));
    }
}
