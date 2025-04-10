/*
 * Test class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemRecord;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.repository.ItemRepository;

/**
 * Test class for the InMemoryItemRepository.
 */
public class InMemoryItemRepositoryTest {

    private ItemRepository repository;
    private WorkItemCreateRequest defaultRequest;

    @BeforeEach
    void setUp() {
        repository = new InMemoryItemRepository();
        ((InMemoryItemRepository) repository).clear(); // Clear the repository before each test
        
        // Create a default work item request for testing
        defaultRequest = new WorkItemCreateRequest.Builder()
                .title("Test Work Item")
                .description("This is a test work item")
                .type(WorkItemType.TASK)
                .priority(Priority.MEDIUM)
                .assignee("alice")
                .visibility("PUBLIC")
                .build();
    }

    @Test
    void testCreateAndFindById() {
        // Create a work item
        WorkItem item = repository.create(defaultRequest);
        assertNotNull(item);
        assertNotNull(item.getId());
        assertEquals("Test Work Item", item.getTitle());
        assertEquals("This is a test work item", item.getDescription());
        assertEquals(WorkItemType.TASK, item.getType());
        assertEquals(WorkflowState.FOUND, item.getStatus()); // Initial state
        assertEquals(Priority.MEDIUM, item.getPriority());
        assertEquals("alice", item.getAssignee());
        
        // Find the item by ID
        Optional<WorkItem> foundItem = repository.findById(item.getId());
        assertTrue(foundItem.isPresent());
        assertEquals(item.getId(), foundItem.get().getId());
        assertEquals(item.getTitle(), foundItem.get().getTitle());
    }
    
    @Test
    void testFindByIdNotFound() {
        Optional<WorkItem> notFoundItem = repository.findById(UUID.randomUUID());
        assertFalse(notFoundItem.isPresent());
    }
    
    @Test
    void testSaveExistingItem() {
        // Create a work item
        WorkItem item = repository.create(defaultRequest);
        
        // Update the item
        WorkItem updatedItem = ((WorkItemRecord) item).withPriority(Priority.HIGH);
        WorkItem savedItem = repository.save(updatedItem);
        
        // Verify the update
        assertEquals(Priority.HIGH, savedItem.getPriority());
        
        // Find the item by ID and verify the update
        Optional<WorkItem> foundItem = repository.findById(item.getId());
        assertTrue(foundItem.isPresent());
        assertEquals(Priority.HIGH, foundItem.get().getPriority());
    }
    
    @Test
    void testFindAll() {
        // Create multiple items
        WorkItem item1 = repository.create(defaultRequest);
        
        WorkItemCreateRequest request2 = new WorkItemCreateRequest.Builder()
                .title("Another Work Item")
                .description("This is another test work item")
                .type(WorkItemType.BUG)
                .priority(Priority.HIGH)
                .assignee("bob")
                .build();
        WorkItem item2 = repository.create(request2);
        
        // Find all items
        List<WorkItem> items = repository.findAll();
        assertEquals(2, items.size());
        assertTrue(items.stream().anyMatch(i -> i.getId().equals(item1.getId())));
        assertTrue(items.stream().anyMatch(i -> i.getId().equals(item2.getId())));
    }
    
    @Test
    void testFindByType() {
        // Create items of different types
        repository.create(defaultRequest); // TASK
        
        WorkItemCreateRequest bugRequest = new WorkItemCreateRequest.Builder()
                .title("Bug")
                .description("This is a bug")
                .type(WorkItemType.BUG)
                .build();
        repository.create(bugRequest);
        
        WorkItemCreateRequest featureRequest = new WorkItemCreateRequest.Builder()
                .title("Feature")
                .description("This is a feature")
                .type(WorkItemType.FEATURE)
                .build();
        repository.create(featureRequest);
        
        // Find items by type
        List<WorkItem> tasks = repository.findByType("TASK");
        assertEquals(1, tasks.size());
        assertEquals(WorkItemType.TASK, tasks.get(0).getType());
        
        List<WorkItem> bugs = repository.findByType("BUG");
        assertEquals(1, bugs.size());
        assertEquals(WorkItemType.BUG, bugs.get(0).getType());
        
        List<WorkItem> features = repository.findByType("FEATURE");
        assertEquals(1, features.size());
        assertEquals(WorkItemType.FEATURE, features.get(0).getType());
        
        // Test with invalid type
        List<WorkItem> invalid = repository.findByType("INVALID");
        assertTrue(invalid.isEmpty());
    }
    
    @Test
    void testFindByStatus() {
        // Create an item
        WorkItem item = repository.create(defaultRequest);
        
        // Initially in FOUND state
        List<WorkItem> foundItems = repository.findByStatus("FOUND");
        assertEquals(1, foundItems.size());
        assertEquals(item.getId(), foundItems.get(0).getId());
        
        // Update the item state
        WorkItem updatedItem = ((WorkItemRecord) item).withStatus(WorkflowState.TO_DO);
        repository.save(updatedItem);
        
        // Find by new state
        List<WorkItem> toDoItems = repository.findByStatus("TO_DO");
        assertEquals(1, toDoItems.size());
        assertEquals(item.getId(), toDoItems.get(0).getId());
        
        // Find by old state (should be empty)
        List<WorkItem> emptyFoundItems = repository.findByStatus("FOUND");
        assertTrue(emptyFoundItems.isEmpty());
        
        // Test with invalid status
        List<WorkItem> invalid = repository.findByStatus("INVALID");
        assertTrue(invalid.isEmpty());
    }
    
    @Test
    void testFindByAssignee() {
        // Create items with different assignees
        repository.create(defaultRequest); // Assigned to "alice"
        
        WorkItemCreateRequest bobRequest = new WorkItemCreateRequest.Builder()
                .title("Bob's Item")
                .description("This is Bob's work item")
                .type(WorkItemType.TASK)
                .assignee("bob")
                .build();
        repository.create(bobRequest);
        
        WorkItemCreateRequest unassignedRequest = new WorkItemCreateRequest.Builder()
                .title("Unassigned Item")
                .description("This is an unassigned work item")
                .type(WorkItemType.TASK)
                .build();
        repository.create(unassignedRequest);
        
        // Find by assignee
        List<WorkItem> aliceItems = repository.findByAssignee("alice");
        assertEquals(1, aliceItems.size());
        assertEquals("alice", aliceItems.get(0).getAssignee());
        
        List<WorkItem> bobItems = repository.findByAssignee("bob");
        assertEquals(1, bobItems.size());
        assertEquals("bob", bobItems.get(0).getAssignee());
        
        // Find unassigned items (null assignee)
        List<WorkItem> unassignedItems = repository.findByAssignee(null);
        assertEquals(1, unassignedItems.size());
        assertNull(unassignedItems.get(0).getAssignee());
        
        // Find by non-existent assignee
        List<WorkItem> charlieItems = repository.findByAssignee("charlie");
        assertTrue(charlieItems.isEmpty());
    }
    
    @Test
    void testDeleteById() {
        // Create an item
        WorkItem item = repository.create(defaultRequest);
        
        // Verify it exists
        assertTrue(repository.findById(item.getId()).isPresent());
        
        // Delete it
        repository.deleteById(item.getId());
        
        // Verify it's gone
        assertFalse(repository.findById(item.getId()).isPresent());
        
        // Delete a non-existent item (should not throw)
        repository.deleteById(UUID.randomUUID());
    }
    
    @Test
    void testClear() {
        // Create multiple items
        repository.create(defaultRequest);
        repository.create(new WorkItemCreateRequest.Builder()
                .title("Another Item")
                .type(WorkItemType.BUG)
                .build());
        
        // Verify items exist
        assertEquals(2, repository.findAll().size());
        
        // Clear the repository
        ((InMemoryItemRepository) repository).clear();
        
        // Verify all items are gone
        assertTrue(repository.findAll().isEmpty());
    }
}