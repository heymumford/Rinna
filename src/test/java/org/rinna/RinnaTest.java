/*
 * Basic tests for the Rinna workflow management system
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

import static org.junit.jupiter.api.Assertions.*;

/**
 * Core tests for the Rinna workflow management system.
 */
public class RinnaTest {

    @Test
    public void testWorkItemCreation() {
        // Simple test for creating a work item
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("Test Task")
                .description("This is a test task")
                .type(WorkItemType.FEATURE)
                .priority(Priority.MEDIUM)
                .assignee("testuser")
                .build();
        
        assertNotNull(request);
        assertEquals("Test Task", request.getTitle());
        assertEquals("This is a test task", request.getDescription());
        assertEquals(WorkItemType.FEATURE, request.getType());
        assertEquals(Priority.MEDIUM, request.getPriority());
        assertEquals("testuser", request.getAssignee());
    }
    
    @Test
    public void testWorkflowStates() {
        // Test workflow states
        WorkflowState[] states = WorkflowState.values();
        assertTrue(states.length > 0);
        
        // Check a few common states
        assertEquals(WorkflowState.FOUND, WorkflowState.valueOf("FOUND"));
        assertEquals(WorkflowState.TO_DO, WorkflowState.valueOf("TO_DO"));
        assertEquals(WorkflowState.IN_PROGRESS, WorkflowState.valueOf("IN_PROGRESS"));
        assertEquals(WorkflowState.DONE, WorkflowState.valueOf("DONE"));
    }
}
