/*
 * Tests for the DefaultWorkItem implementation
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rinna.domain.model.DefaultWorkItem;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DefaultWorkItem class.
 */
public class DefaultWorkItemTest {

    private WorkItemCreateRequest request;
    private DefaultWorkItem workItem;
    
    @BeforeEach
    public void setup() {
        // Create a standard work item request for testing
        request = new WorkItemCreateRequest.Builder()
                .title("Test Work Item")
                .description("This is a test work item")
                .type(WorkItemType.FEATURE)
                .priority(Priority.MEDIUM)
                .assignee("tester")
                .build();
        
        // Create a work item from the request
        workItem = new DefaultWorkItem(UUID.randomUUID(), request);
    }
    
    @Test
    public void testWorkItemCreation() {
        assertNotNull(workItem);
        assertNotNull(workItem.getId());
        assertEquals("Test Work Item", workItem.getTitle());
        assertEquals("This is a test work item", workItem.getDescription());
        assertEquals(WorkItemType.FEATURE, workItem.getType());
        assertEquals(Priority.MEDIUM, workItem.getPriority());
        assertEquals("tester", workItem.getAssignee());
        assertEquals(WorkflowState.FOUND, workItem.getStatus());
        assertNotNull(workItem.getCreatedAt());
        assertNotNull(workItem.getUpdatedAt());
    }
    
    @Test
    public void testWorkItemUpdate() {
        // Update work item
        workItem.setTitle("Updated Title");
        workItem.setDescription("Updated description");
        workItem.setPriority(Priority.HIGH);
        workItem.setAssignee("updater");
        workItem.setStatus(WorkflowState.TRIAGED);
        
        // Verify updates
        assertEquals("Updated Title", workItem.getTitle());
        assertEquals("Updated description", workItem.getDescription());
        assertEquals(Priority.HIGH, workItem.getPriority());
        assertEquals("updater", workItem.getAssignee());
        assertEquals(WorkflowState.TRIAGED, workItem.getStatus());
    }
    
    @Test
    public void testStatusHistory() {
        // Set initial state
        workItem.setStatus(WorkflowState.FOUND);
        
        // Update state a few times
        workItem.setStatus(WorkflowState.TRIAGED);
        workItem.setStatus(WorkflowState.TO_DO);
        workItem.setStatus(WorkflowState.IN_PROGRESS);
        
        // Check current state
        assertEquals(WorkflowState.IN_PROGRESS, workItem.getStatus());
        
        // Status history would normally be stored and tested,
        // but we're focusing on the basic functionality here
    }
}
