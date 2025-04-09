/*
 * Service component for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.unit.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.rinna.persistence.InMemoryItemRepository;
import org.rinna.service.DefaultItemService;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.repository.ItemRepository;
import org.rinna.usecase.ItemService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the DefaultItemService class with InMemoryItemRepository.
 */
@Tag("unit")
public class InMemoryItemServiceTest {
    
    private ItemRepository itemRepository;
    private ItemService itemService;
    
    @BeforeEach
    public void setUp() {
        // Create a new repository and service for each test
        itemRepository = new InMemoryItemRepository();
        itemService = new DefaultItemService(itemRepository);
    }
    
    @Test
    public void testCreateAndFindById() {
        // Create a work item
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("Test Item")
                .description("Test Description")
                .type(WorkItemType.FEATURE)
                .priority(Priority.MEDIUM)
                .assignee("tester")
                .build();
        
        WorkItem createdItem = itemService.create(request);
        
        // Verify the item was created correctly
        assertNotNull(createdItem);
        assertEquals("Test Item", createdItem.getTitle());
        assertEquals("Test Description", createdItem.getDescription());
        assertEquals(WorkItemType.FEATURE, createdItem.getType());
        assertEquals(Priority.MEDIUM, createdItem.getPriority());
        assertEquals("tester", createdItem.getAssignee());
        assertEquals(WorkflowState.FOUND, createdItem.getStatus());
        
        // Find the item by ID
        UUID itemId = createdItem.getId();
        Optional<WorkItem> foundItem = itemService.findById(itemId);
        
        // Verify the item was found and is the same as the created item
        assertTrue(foundItem.isPresent());
        assertEquals(createdItem.getId(), foundItem.get().getId());
        assertEquals(createdItem.getTitle(), foundItem.get().getTitle());
    }
    
    @Test
    public void testFindAll() {
        // Repository starts empty
        assertTrue(itemService.findAll().isEmpty());
        
        // Create a few work items
        WorkItem item1 = itemService.create(new WorkItemCreateRequest.Builder()
                .title("Item 1")
                .type(WorkItemType.FEATURE)
                .build());
        
        WorkItem item2 = itemService.create(new WorkItemCreateRequest.Builder()
                .title("Item 2")
                .type(WorkItemType.BUG)
                .build());
        
        WorkItem item3 = itemService.create(new WorkItemCreateRequest.Builder()
                .title("Item 3")
                .type(WorkItemType.CHORE)
                .build());
        
        // Find all items
        List<WorkItem> allItems = itemService.findAll();
        
        // Verify all items were found
        assertEquals(3, allItems.size());
        assertTrue(allItems.stream().anyMatch(item -> item.getId().equals(item1.getId())));
        assertTrue(allItems.stream().anyMatch(item -> item.getId().equals(item2.getId())));
        assertTrue(allItems.stream().anyMatch(item -> item.getId().equals(item3.getId())));
    }
    
    @Test
    public void testFindByType() {
        // Create items of different types
        WorkItem feature = itemService.create(new WorkItemCreateRequest.Builder()
                .title("Feature")
                .type(WorkItemType.FEATURE)
                .build());
        
        WorkItem bug = itemService.create(new WorkItemCreateRequest.Builder()
                .title("Bug")
                .type(WorkItemType.BUG)
                .build());
        
        WorkItem chore = itemService.create(new WorkItemCreateRequest.Builder()
                .title("Chore")
                .type(WorkItemType.CHORE)
                .build());
        
        // Find by type
        List<WorkItem> features = itemService.findByType("FEATURE");
        List<WorkItem> bugs = itemService.findByType("BUG");
        List<WorkItem> chores = itemService.findByType("CHORE");
        
        // Verify the results
        assertEquals(1, features.size());
        assertEquals(feature.getId(), features.get(0).getId());
        
        assertEquals(1, bugs.size());
        assertEquals(bug.getId(), bugs.get(0).getId());
        
        assertEquals(1, chores.size());
        assertEquals(chore.getId(), chores.get(0).getId());
    }
    
    @Test
    public void testFindByStatus() {
        // All items start with status FOUND
        WorkItem item1 = itemService.create(new WorkItemCreateRequest.Builder()
                .title("Item 1")
                .type(WorkItemType.FEATURE)
                .build());
        
        WorkItem item2 = itemService.create(new WorkItemCreateRequest.Builder()
                .title("Item 2")
                .type(WorkItemType.BUG)
                .build());
        
        // Find by status
        List<WorkItem> foundItems = itemService.findByStatus("FOUND");
        
        // Verify all items are found
        assertEquals(2, foundItems.size());
    }
    
    @Test
    public void testFindByAssignee() {
        // Create items with different assignees
        WorkItem item1 = itemService.create(new WorkItemCreateRequest.Builder()
                .title("Item 1")
                .type(WorkItemType.FEATURE)
                .assignee("alice")
                .build());
        
        WorkItem item2 = itemService.create(new WorkItemCreateRequest.Builder()
                .title("Item 2")
                .type(WorkItemType.BUG)
                .assignee("bob")
                .build());
        
        WorkItem item3 = itemService.create(new WorkItemCreateRequest.Builder()
                .title("Item 3")
                .type(WorkItemType.CHORE)
                .assignee("alice")
                .build());
        
        // Find by assignee
        List<WorkItem> aliceItems = itemService.findByAssignee("alice");
        List<WorkItem> bobItems = itemService.findByAssignee("bob");
        
        // Verify the results
        assertEquals(2, aliceItems.size());
        assertTrue(aliceItems.stream().anyMatch(item -> item.getId().equals(item1.getId())));
        assertTrue(aliceItems.stream().anyMatch(item -> item.getId().equals(item3.getId())));
        
        assertEquals(1, bobItems.size());
        assertEquals(item2.getId(), bobItems.get(0).getId());
    }
    
    @Test
    public void testUpdateAssignee() {
        // Create a work item
        WorkItem item = itemService.create(new WorkItemCreateRequest.Builder()
                .title("Test Item")
                .type(WorkItemType.FEATURE)
                .assignee("alice")
                .build());
        
        UUID itemId = item.getId();
        
        // Verify initial assignee
        assertEquals("alice", item.getAssignee());
        
        // Update assignee
        WorkItem updatedItem = itemService.updateAssignee(itemId, "bob");
        
        // Verify the assignee was updated
        assertEquals("bob", updatedItem.getAssignee());
        
        // Verify the item in the repository was also updated
        Optional<WorkItem> retrievedItem = itemService.findById(itemId);
        assertTrue(retrievedItem.isPresent());
        assertEquals("bob", retrievedItem.get().getAssignee());
    }
    
    @Test
    public void testDeleteById() {
        // Create a work item
        WorkItem item = itemService.create(new WorkItemCreateRequest.Builder()
                .title("Test Item")
                .type(WorkItemType.FEATURE)
                .build());
        
        UUID itemId = item.getId();
        
        // Verify the item exists
        assertTrue(itemService.findById(itemId).isPresent());
        
        // Delete the item
        itemService.deleteById(itemId);
        
        // Verify the item was deleted
        assertFalse(itemService.findById(itemId).isPresent());
    }
}
