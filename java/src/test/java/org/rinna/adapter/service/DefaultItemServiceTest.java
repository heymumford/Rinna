/*
 * Unit test for the DefaultItemService
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rinna.adapter.repository.InMemoryItemRepository;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.repository.ItemRepository;
import org.rinna.usecase.ItemService;

/**
 * Unit tests for {@link DefaultItemService}.
 */
class DefaultItemServiceTest {

    private ItemRepository repository;
    private ItemService service;
    private WorkItemCreateRequest defaultRequest;
    private UUID existingItemId;

    @BeforeEach
    void setUp() {
        repository = new InMemoryItemRepository();
        service = new DefaultItemService(repository);
        
        // Create a default request for testing
        defaultRequest = new WorkItemCreateRequest.Builder()
                .title("Test Work Item")
                .description("This is a test work item")
                .type(WorkItemType.TASK)
                .status(WorkflowState.TO_DO)
                .priority(Priority.MEDIUM)
                .assignee("tester")
                .build();
        
        // Create an existing item for testing
        WorkItem item = service.create(defaultRequest);
        existingItemId = item.getId();
    }

    @Test
    void testCreateWithValidRequest() {
        // Create a new work item
        WorkItem createdItem = service.create(defaultRequest);
        
        // Verify created item
        assertNotNull(createdItem);
        assertNotNull(createdItem.getId());
        assertEquals("Test Work Item", createdItem.getTitle());
        assertEquals("This is a test work item", createdItem.getDescription());
        assertEquals(WorkItemType.TASK, createdItem.getType());
        assertEquals(WorkflowState.TO_DO, createdItem.getStatus());
        assertEquals(Priority.MEDIUM, createdItem.getPriority());
        assertEquals("tester", createdItem.getAssignee());
    }

    @Test
    void testCreateWithNullRequest() {
        // Attempt to create with null request
        assertThrows(NullPointerException.class, () -> service.create(null));
    }

    @Test
    void testFindByIdExisting() {
        // Find existing item
        Optional<WorkItem> foundItem = service.findById(existingItemId);
        
        // Verify found item
        assertTrue(foundItem.isPresent());
        assertEquals(existingItemId, foundItem.get().getId());
        assertEquals("Test Work Item", foundItem.get().getTitle());
    }

    @Test
    void testFindByIdNonExistent() {
        // Find non-existent item
        Optional<WorkItem> foundItem = service.findById(UUID.randomUUID());
        
        // Verify not found
        assertFalse(foundItem.isPresent());
    }

    @Test
    void testFindByIdNull() {
        // Attempt to find with null ID
        assertThrows(NullPointerException.class, () -> service.findById(null));
    }

    @Test
    void testFindAll() {
        // Initially should have one item from setup
        List<WorkItem> allItems = service.findAll();
        assertEquals(1, allItems.size());
        
        // Create additional items
        service.create(defaultRequest);
        service.create(defaultRequest);
        
        // Find all items
        allItems = service.findAll();
        
        // Verify found items
        assertEquals(3, allItems.size());
    }

    @Test
    void testFindByType() {
        // Create items of different types
        WorkItemCreateRequest bugRequest = new WorkItemCreateRequest.Builder()
                .title("Bug Item")
                .description("This is a bug")
                .type(WorkItemType.BUG)
                .status(WorkflowState.TO_DO)
                .priority(Priority.HIGH)
                .build();
        
        service.create(bugRequest);
        
        // Find by type TASK
        List<WorkItem> taskItems = service.findByType(WorkItemType.TASK.name());
        
        // Verify found items
        assertEquals(1, taskItems.size());
        assertEquals(WorkItemType.TASK, taskItems.get(0).getType());
        
        // Find by type BUG
        List<WorkItem> bugItems = service.findByType(WorkItemType.BUG.name());
        
        // Verify found items
        assertEquals(1, bugItems.size());
        assertEquals(WorkItemType.BUG, bugItems.get(0).getType());
        
        // Find by non-existent type
        List<WorkItem> nonExistentItems = service.findByType("NON_EXISTENT");
        
        // Verify no items found
        assertTrue(nonExistentItems.isEmpty());
    }

    @Test
    void testFindByTypeNull() {
        // Attempt to find by null type
        assertThrows(NullPointerException.class, () -> service.findByType(null));
    }

    @Test
    void testFindByStatus() {
        // Create items with different statuses
        WorkItemCreateRequest inProgressRequest = new WorkItemCreateRequest.Builder()
                .title("In Progress Item")
                .description("This is an in-progress item")
                .type(WorkItemType.TASK)
                .status(WorkflowState.IN_PROGRESS)
                .priority(Priority.MEDIUM)
                .build();
        
        service.create(inProgressRequest);
        
        // Find by status TO_DO
        List<WorkItem> todoItems = service.findByStatus(WorkflowState.TO_DO.name());
        
        // Verify found items
        assertEquals(1, todoItems.size());
        assertEquals(WorkflowState.TO_DO, todoItems.get(0).getStatus());
        
        // Find by status IN_PROGRESS
        List<WorkItem> inProgressItems = service.findByStatus(WorkflowState.IN_PROGRESS.name());
        
        // Verify found items
        assertEquals(1, inProgressItems.size());
        assertEquals(WorkflowState.IN_PROGRESS, inProgressItems.get(0).getStatus());
        
        // Find by non-existent status
        List<WorkItem> nonExistentItems = service.findByStatus("NON_EXISTENT");
        
        // Verify no items found
        assertTrue(nonExistentItems.isEmpty());
    }

    @Test
    void testFindByStatusNull() {
        // Attempt to find by null status
        assertThrows(NullPointerException.class, () -> service.findByStatus(null));
    }

    @Test
    void testFindByAssignee() {
        // Create items with different assignees
        WorkItemCreateRequest aliceRequest = new WorkItemCreateRequest.Builder()
                .title("Alice's Item")
                .description("This is assigned to Alice")
                .type(WorkItemType.TASK)
                .status(WorkflowState.TO_DO)
                .priority(Priority.MEDIUM)
                .assignee("alice")
                .build();
        
        service.create(aliceRequest);
        
        // Find by assignee "tester"
        List<WorkItem> testerItems = service.findByAssignee("tester");
        
        // Verify found items
        assertEquals(1, testerItems.size());
        assertEquals("tester", testerItems.get(0).getAssignee());
        
        // Find by assignee "alice"
        List<WorkItem> aliceItems = service.findByAssignee("alice");
        
        // Verify found items
        assertEquals(1, aliceItems.size());
        assertEquals("alice", aliceItems.get(0).getAssignee());
        
        // Find by non-existent assignee
        List<WorkItem> nonExistentItems = service.findByAssignee("non-existent");
        
        // Verify no items found
        assertTrue(nonExistentItems.isEmpty());
        
        // Find unassigned items
        List<WorkItem> unassignedItems = service.findByAssignee(null);
        
        // Verify no items found (all test items have assignees)
        assertTrue(unassignedItems.isEmpty());
    }

    @Test
    void testUpdateAssignee() {
        // Update assignee
        WorkItem updatedItem = service.updateAssignee(existingItemId, "new-assignee");
        
        // Verify updated item
        assertNotNull(updatedItem);
        assertEquals(existingItemId, updatedItem.getId());
        assertEquals("new-assignee", updatedItem.getAssignee());
        
        // Verify by finding again
        Optional<WorkItem> foundItem = service.findById(existingItemId);
        assertTrue(foundItem.isPresent());
        assertEquals("new-assignee", foundItem.get().getAssignee());
    }

    @Test
    void testUpdateAssigneeNonExistent() {
        // Attempt to update non-existent item
        UUID nonExistentId = UUID.randomUUID();
        
        // Verify exception is thrown
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateAssignee(nonExistentId, "new-assignee")
        );
        
        // Verify exception message
        assertTrue(exception.getMessage().contains("Work item not found"));
    }

    @Test
    void testUpdateAssigneeNullId() {
        // Attempt to update with null ID
        assertThrows(NullPointerException.class, () -> service.updateAssignee(null, "new-assignee"));
    }

    @Test
    void testDeleteById() {
        // Verify item exists before deletion
        assertTrue(service.findById(existingItemId).isPresent());
        
        // Delete item
        service.deleteById(existingItemId);
        
        // Verify item was deleted
        assertFalse(service.findById(existingItemId).isPresent());
    }

    @Test
    void testDeleteByIdNonExistent() {
        // Attempt to delete non-existent item (should not throw exception)
        UUID nonExistentId = UUID.randomUUID();
        service.deleteById(nonExistentId);
    }

    @Test
    void testDeleteByIdNull() {
        // Attempt to delete with null ID
        assertThrows(NullPointerException.class, () -> service.deleteById(null));
    }

    @Test
    void testConstructorWithNullRepository() {
        // Attempt to create service with null repository
        assertThrows(NullPointerException.class, () -> new DefaultItemService(null));
    }
}