/*
 * Model class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.model;

import org.junit.jupiter.api.Test;
import org.rinna.domain.model.DefaultWorkItem;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DefaultWorkItem class.
 */
public class DefaultWorkItemTest {

    @Test
    public void testCreateFromRequest() {
        // Create a request
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("Test Item")
                .description("Test Description")
                .type(WorkItemType.FEATURE)
                .priority(Priority.HIGH)
                .assignee("tester")
                .build();
        
        // Create a work item from the request
        UUID id = UUID.randomUUID();
        DefaultWorkItem item = new DefaultWorkItem(id, request);
        
        // Verify the work item was created correctly
        assertEquals(id, item.getId());
        assertEquals("Test Item", item.getTitle());
        assertEquals("Test Description", item.getDescription());
        assertEquals(WorkItemType.FEATURE, item.getType());
        assertEquals(WorkflowState.FOUND, item.getStatus());
        assertEquals(Priority.HIGH, item.getPriority());
        assertEquals("tester", item.getAssignee());
        assertNotNull(item.getCreatedAt());
        assertNotNull(item.getUpdatedAt());
        assertFalse(item.getParentId().isPresent());
    }
    
    @Test
    public void testSetStatus() {
        // Create a work item
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        DefaultWorkItem item = new DefaultWorkItem(
                id,
                "Test Item",
                "Test Description",
                WorkItemType.FEATURE,
                WorkflowState.FOUND,
                Priority.MEDIUM,
                "tester",
                now,
                now,
                null,
                null,
                "PUBLIC",
                false);
        
        // Initial status should be FOUND
        assertEquals(WorkflowState.FOUND, item.getStatus());
        
        // Update the status to TRIAGED
        item.setStatus(WorkflowState.TRIAGED);
        
        // Verify the status was updated
        assertEquals(WorkflowState.TRIAGED, item.getStatus());
        
        // Verify that updated timestamp changed
        assertTrue(item.getUpdatedAt().isAfter(now));
    }
    
    @Test
    public void testSetPriority() {
        // Create a work item
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        DefaultWorkItem item = new DefaultWorkItem(
                id,
                "Test Item",
                "Test Description",
                WorkItemType.FEATURE,
                WorkflowState.FOUND,
                Priority.MEDIUM,
                "tester",
                now,
                now,
                null,
                null,
                "PUBLIC",
                false);
        
        // Initial priority should be MEDIUM
        assertEquals(Priority.MEDIUM, item.getPriority());
        
        // Update the priority to HIGH
        item.setPriority(Priority.HIGH);
        
        // Verify the priority was updated
        assertEquals(Priority.HIGH, item.getPriority());
        
        // Verify that updated timestamp changed
        assertTrue(item.getUpdatedAt().isAfter(now));
    }
    
    @Test
    public void testSetAssignee() {
        // Create a work item
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        DefaultWorkItem item = new DefaultWorkItem(
                id,
                "Test Item",
                "Test Description",
                WorkItemType.FEATURE,
                WorkflowState.FOUND,
                Priority.MEDIUM,
                "tester",
                now,
                now,
                null,
                null,
                "PUBLIC",
                false);
        
        // Initial assignee should be "tester"
        assertEquals("tester", item.getAssignee());
        
        // Update the assignee to "newassignee"
        item.setAssignee("newassignee");
        
        // Verify the assignee was updated
        assertEquals("newassignee", item.getAssignee());
        
        // Verify that updated timestamp changed
        assertTrue(item.getUpdatedAt().isAfter(now));
    }
    
    @Test
    public void testEqualsAndHashCode() {
        // Create two work items with the same ID
        UUID id = UUID.randomUUID();
        DefaultWorkItem item1 = new DefaultWorkItem(
                id,
                "Test Item 1",
                "Test Description 1",
                WorkItemType.FEATURE,
                WorkflowState.FOUND,
                Priority.MEDIUM,
                "tester1",
                Instant.now(),
                Instant.now(),
                null,
                null,
                "PUBLIC",
                false);
        
        DefaultWorkItem item2 = new DefaultWorkItem(
                id,
                "Test Item 2", // Different title
                "Test Description 2", // Different description
                WorkItemType.BUG, // Different type
                WorkflowState.TRIAGED, // Different status
                Priority.HIGH, // Different priority
                "tester2", // Different assignee
                Instant.now().plusSeconds(1000), // Different timestamps
                Instant.now().plusSeconds(2000),
                null,
                null,
                "PUBLIC",
                false);
        
        // Create a third work item with a different ID
        DefaultWorkItem item3 = new DefaultWorkItem(
                UUID.randomUUID(),
                "Test Item 1",
                "Test Description 1",
                WorkItemType.FEATURE,
                WorkflowState.FOUND,
                Priority.MEDIUM,
                "tester1",
                Instant.now(),
                Instant.now(),
                null,
                null,
                "PUBLIC",
                false);
        
        // Items with the same ID should be equal
        assertEquals(item1, item2);
        assertEquals(item1.hashCode(), item2.hashCode());
        
        // Items with different IDs should not be equal
        assertNotEquals(item1, item3);
        assertNotEquals(item1.hashCode(), item3.hashCode());
    }
}