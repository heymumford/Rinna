/*
 * SQLite persistence tests for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.data.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.repository.ItemRepository;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the SQLiteItemRepository class.
 */
class SqliteItemRepositoryTest {
    
    @TempDir
    Path tempDir;
    
    private SqliteRepositoryFactory factory;
    private ItemRepository itemRepository;
    
    @BeforeEach
    void setUp() {
        SqliteConnectionManager connectionManager = new SqliteConnectionManager(
                tempDir.toString(), "test-rinna.db");
        factory = new SqliteRepositoryFactory(connectionManager);
        itemRepository = factory.getItemRepository();
    }
    
    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }
    
    @Test
    void createAndFindById() {
        // Create a work item
        Map<String, String> metadata = new HashMap<>();
        metadata.put("creator", "test-user");
        metadata.put("tag", "unit-test");
        
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("Test Work Item")
                .description("This is a test work item")
                .type(WorkItemType.FEATURE)
                .priority(Priority.MEDIUM)
                .assignee("tester")
                .metadata(metadata)
                .build();
        
        WorkItem createdItem = itemRepository.create(request);
        assertNotNull(createdItem);
        assertNotNull(createdItem.getId());
        assertEquals("Test Work Item", createdItem.getTitle());
        assertEquals("This is a test work item", createdItem.getDescription());
        assertEquals(WorkItemType.FEATURE, createdItem.getType());
        assertEquals(WorkflowState.FOUND, createdItem.getStatus()); // Initial state
        assertEquals(Priority.MEDIUM, createdItem.getPriority());
        assertEquals("tester", createdItem.getAssignee());
        
        // Find by ID
        Optional<WorkItem> foundItem = itemRepository.findById(createdItem.getId());
        assertTrue(foundItem.isPresent());
        assertEquals(createdItem.getId(), foundItem.get().getId());
        assertEquals(createdItem.getTitle(), foundItem.get().getTitle());
    }
    
    @Test
    void findAll() {
        // Create a few work items
        createTestWorkItem("Item 1", WorkItemType.FEATURE);
        createTestWorkItem("Item 2", WorkItemType.BUG);
        createTestWorkItem("Item 3", WorkItemType.CHORE);
        
        // Find all
        List<WorkItem> items = itemRepository.findAll();
        assertEquals(3, items.size());
    }
    
    @Test
    void findByType() {
        // Create work items of different types
        createTestWorkItem("Feature 1", WorkItemType.FEATURE);
        createTestWorkItem("Feature 2", WorkItemType.FEATURE);
        createTestWorkItem("Bug 1", WorkItemType.BUG);
        
        // Find by type
        List<WorkItem> features = itemRepository.findByType(WorkItemType.FEATURE);
        assertEquals(2, features.size());
        
        List<WorkItem> bugs = itemRepository.findByType(WorkItemType.BUG);
        assertEquals(1, bugs.size());
        
        List<WorkItem> chores = itemRepository.findByType(WorkItemType.CHORE);
        assertEquals(0, chores.size());
    }
    
    @Test
    void findByStatus() {
        // Create work items
        WorkItem item1 = createTestWorkItem("Item 1", WorkItemType.FEATURE);
        WorkItem item2 = createTestWorkItem("Item 2", WorkItemType.BUG);
        
        // All items start with FOUND status
        List<WorkItem> foundItems = itemRepository.findByStatus(WorkflowState.FOUND);
        assertEquals(2, foundItems.size());
        
        // Update one item to a different status (using a helper method that simulates workflow)
        itemRepository.save(simulateStatusChange(item1, WorkflowState.TRIAGED));
        
        // Check items by status
        List<WorkItem> triaged = itemRepository.findByStatus(WorkflowState.TRIAGED);
        assertEquals(1, triaged.size());
        
        List<WorkItem> stillFound = itemRepository.findByStatus(WorkflowState.FOUND);
        assertEquals(1, stillFound.size());
    }
    
    @Test
    void findByAssignee() {
        // Create work items with different assignees
        createTestWorkItemWithAssignee("Item 1", "alice");
        createTestWorkItemWithAssignee("Item 2", "bob");
        createTestWorkItemWithAssignee("Item 3", "alice");
        
        // Find by assignee
        List<WorkItem> aliceItems = itemRepository.findByAssignee("alice");
        assertEquals(2, aliceItems.size());
        
        List<WorkItem> bobItems = itemRepository.findByAssignee("bob");
        assertEquals(1, bobItems.size());
        
        List<WorkItem> charlieItems = itemRepository.findByAssignee("charlie");
        assertEquals(0, charlieItems.size());
    }
    
    @Test
    void updateMetadata() {
        // Create a work item with metadata
        Map<String, String> initialMetadata = new HashMap<>();
        initialMetadata.put("key1", "value1");
        initialMetadata.put("key2", "value2");
        
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("Test Work Item")
                .type(WorkItemType.FEATURE)
                .metadata(initialMetadata)
                .build();
        
        WorkItem createdItem = itemRepository.create(request);
        
        // Update metadata
        Map<String, String> updatedMetadata = new HashMap<>();
        updatedMetadata.put("key1", "updated-value1");
        updatedMetadata.put("key3", "value3");
        
        itemRepository.updateMetadata(createdItem.getId(), updatedMetadata);
        
        // Verify metadata was updated
        // Note: We'd need to access the metadata repository directly to verify this
        // Instead we'll use custom field search
        List<WorkItem> itemsWithKey1 = itemRepository.findByCustomField("key1", "updated-value1");
        assertEquals(1, itemsWithKey1.size());
        
        List<WorkItem> itemsWithKey3 = itemRepository.findByCustomField("key3", "value3");
        assertEquals(1, itemsWithKey3.size());
        
        List<WorkItem> itemsWithKey2 = itemRepository.findByCustomField("key2", "value2");
        assertEquals(0, itemsWithKey2.size()); // This was removed in the update
    }
    
    @Test
    void deleteById() {
        // Create a work item
        WorkItem item = createTestWorkItem("Item to delete", WorkItemType.FEATURE);
        
        // Verify it exists
        Optional<WorkItem> foundBefore = itemRepository.findById(item.getId());
        assertTrue(foundBefore.isPresent());
        
        // Delete it
        itemRepository.deleteById(item.getId());
        
        // Verify it's gone
        Optional<WorkItem> foundAfter = itemRepository.findById(item.getId());
        assertFalse(foundAfter.isPresent());
    }
    
    // Helper methods
    
    private WorkItem createTestWorkItem(String title, WorkItemType type) {
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title(title)
                .description("Test description")
                .type(type)
                .build();
        
        return itemRepository.create(request);
    }
    
    private WorkItem createTestWorkItemWithAssignee(String title, String assignee) {
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title(title)
                .description("Test description")
                .type(WorkItemType.FEATURE)
                .assignee(assignee)
                .build();
        
        return itemRepository.create(request);
    }
    
    private WorkItem simulateStatusChange(WorkItem item, WorkflowState newStatus) {
        // This is a helper method to simulate what a workflow service would do
        // We're directly using reflection to access the withStatus method here
        // In a real application, this would be handled by a service
        if (item instanceof org.rinna.domain.model.WorkItemRecord record) {
            return record.withStatus(newStatus);
        } else {
            throw new UnsupportedOperationException("Cannot change status of unknown implementation");
        }
    }
}