/*
 * MockItemServiceTest for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the MockItemService class.
 * 
 * This test suite follows best practices:
 * 1. CRUD Tests - Testing Create, Read, Update, Delete operations
 * 2. Query Tests - Testing search and filter operations
 * 3. Field Update Tests - Testing specific field update methods
 * 4. Validation Tests - Testing behavior with invalid inputs
 */
@DisplayName("MockItemService Tests")
class MockItemServiceTest {
    
    private MockItemService itemService;
    
    @BeforeEach
    void setUp() {
        // Create a new instance for each test
        itemService = new MockItemService();
    }
    
    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {
        
        @Test
        @DisplayName("Should initialize with sample work items")
        void shouldInitializeWithSampleWorkItems() {
            // When
            List<WorkItem> items = itemService.getAllItems();
            
            // Then
            assertFalse(items.isEmpty(), "Initial items list should not be empty");
            assertEquals(3, items.size(), "Should have 3 sample items");
            
            // Verify sample items exist
            assertTrue(items.stream().anyMatch(item -> "Implement authentication feature".equals(item.getTitle())),
                "Should contain 'Implement authentication feature' item");
            assertTrue(items.stream().anyMatch(item -> "Fix bug in payment module".equals(item.getTitle())),
                "Should contain 'Fix bug in payment module' item");
            assertTrue(items.stream().anyMatch(item -> "Update documentation".equals(item.getTitle())),
                "Should contain 'Update documentation' item");
        }
        
        @Test
        @DisplayName("Should initialize items with appropriate types and states")
        void shouldInitializeItemsWithAppropriateTypesAndStates() {
            // When
            List<WorkItem> items = itemService.getAllItems();
            
            // Then
            assertTrue(items.stream().anyMatch(item -> WorkItemType.TASK.equals(item.getType()) && 
                                               WorkflowState.IN_PROGRESS.equals(item.getStatus())),
                "Should contain a TASK item with IN_PROGRESS status");
            
            assertTrue(items.stream().anyMatch(item -> WorkItemType.BUG.equals(item.getType()) && 
                                               WorkflowState.READY.equals(item.getStatus())),
                "Should contain a BUG item with READY status");
            
            assertTrue(items.stream().anyMatch(item -> WorkItemType.TASK.equals(item.getType()) && 
                                               WorkflowState.DONE.equals(item.getStatus())),
                "Should contain a TASK item with DONE status");
        }
    }
    
    @Nested
    @DisplayName("CRUD Operation Tests")
    class CrudOperationTests {
        
        @Test
        @DisplayName("Should create a new work item with generated ID")
        void shouldCreateNewWorkItemWithGeneratedId() {
            // Given
            WorkItem newItem = new WorkItem();
            newItem.setTitle("Test work item");
            newItem.setDescription("This is a test");
            newItem.setType(WorkItemType.TASK);
            newItem.setPriority(Priority.MEDIUM);
            newItem.setStatus(WorkflowState.OPEN);
            
            // When
            WorkItem createdItem = itemService.createItem(newItem);
            
            // Then
            assertNotNull(createdItem, "Created item should not be null");
            assertNotNull(createdItem.getId(), "Created item should have an ID");
            assertEquals("Test work item", createdItem.getTitle(), "Title should match");
            assertEquals(WorkItemType.TASK, createdItem.getType(), "Type should match");
            assertEquals(Priority.MEDIUM, createdItem.getPriority(), "Priority should match");
            assertEquals(WorkflowState.OPEN, createdItem.getStatus(), "Status should match");
            assertNotNull(createdItem.getCreated(), "Created timestamp should be set");
            assertNotNull(createdItem.getUpdated(), "Updated timestamp should be set");
            
            // Verify item is in the list
            List<WorkItem> allItems = itemService.getAllItems();
            assertTrue(allItems.contains(createdItem), "All items should contain the created item");
            assertEquals(4, allItems.size(), "Should now have 4 items");
        }
        
        @Test
        @DisplayName("Should create a new work item with provided ID")
        void shouldCreateNewWorkItemWithProvidedId() {
            // Given
            String customId = UUID.randomUUID().toString();
            WorkItem newItem = new WorkItem();
            newItem.setId(customId);
            newItem.setTitle("Test work item with custom ID");
            newItem.setType(WorkItemType.FEATURE);
            newItem.setPriority(Priority.HIGH);
            newItem.setStatus(WorkflowState.OPEN);
            
            // When
            WorkItem createdItem = itemService.createItem(newItem);
            
            // Then
            assertNotNull(createdItem, "Created item should not be null");
            assertEquals(customId, createdItem.getId(), "ID should match the provided custom ID");
            assertEquals("Test work item with custom ID", createdItem.getTitle(), "Title should match");
            
            // Verify item is in the list and retrievable by ID
            WorkItem retrievedItem = itemService.getItem(customId);
            assertNotNull(retrievedItem, "Item should be retrievable by ID");
            assertEquals(customId, retrievedItem.getId(), "Retrieved item ID should match");
        }
        
        @Test
        @DisplayName("Should get a specific work item by ID")
        void shouldGetSpecificWorkItemById() {
            // Given
            List<WorkItem> allItems = itemService.getAllItems();
            WorkItem firstItem = allItems.get(0);
            String itemId = firstItem.getId();
            
            // When
            WorkItem retrievedItem = itemService.getItem(itemId);
            
            // Then
            assertNotNull(retrievedItem, "Retrieved item should not be null");
            assertEquals(itemId, retrievedItem.getId(), "Retrieved item ID should match");
            assertEquals(firstItem.getTitle(), retrievedItem.getTitle(), "Title should match");
            assertEquals(firstItem.getType(), retrievedItem.getType(), "Type should match");
            assertEquals(firstItem.getPriority(), retrievedItem.getPriority(), "Priority should match");
            assertEquals(firstItem.getStatus(), retrievedItem.getStatus(), "Status should match");
        }
        
        @Test
        @DisplayName("Should return null when getting non-existent item")
        void shouldReturnNullWhenGettingNonExistentItem() {
            // Given
            String nonExistentId = "non-existent-id";
            
            // When
            WorkItem retrievedItem = itemService.getItem(nonExistentId);
            
            // Then
            assertNull(retrievedItem, "Should return null for non-existent item");
        }
        
        @Test
        @DisplayName("Should update an existing work item")
        void shouldUpdateExistingWorkItem() {
            // Given
            List<WorkItem> allItems = itemService.getAllItems();
            WorkItem itemToUpdate = allItems.get(0);
            String itemId = itemToUpdate.getId();
            
            WorkItem updatedItem = new WorkItem();
            updatedItem.setId(itemId);
            updatedItem.setTitle("Updated title");
            updatedItem.setDescription("Updated description");
            updatedItem.setType(WorkItemType.FEATURE);
            updatedItem.setPriority(Priority.HIGH);
            updatedItem.setStatus(WorkflowState.IN_PROGRESS);
            updatedItem.setAssignee("newassignee");
            updatedItem.setProject("NEWPROJ");
            
            // Capture original creation time to verify it's preserved
            LocalDateTime originalCreationTime = itemToUpdate.getCreated();
            
            // When
            WorkItem result = itemService.updateItem(updatedItem);
            
            // Then
            assertNotNull(result, "Updated item result should not be null");
            assertEquals(itemId, result.getId(), "ID should not change");
            assertEquals("Updated title", result.getTitle(), "Title should be updated");
            assertEquals("Updated description", result.getDescription(), "Description should be updated");
            assertEquals(WorkItemType.FEATURE, result.getType(), "Type should be updated");
            assertEquals(Priority.HIGH, result.getPriority(), "Priority should be updated");
            assertEquals(WorkflowState.IN_PROGRESS, result.getStatus(), "Status should be updated");
            assertEquals("newassignee", result.getAssignee(), "Assignee should be updated");
            assertEquals("NEWPROJ", result.getProject(), "Project should be updated");
            
            // Verify creation time is preserved
            assertEquals(originalCreationTime, result.getCreated(), "Creation time should be preserved");
            
            // Verify updated time is changed
            assertNotEquals(originalCreationTime, result.getUpdated(), "Update time should be different from creation time");
            
            // Verify item is updated in the list
            WorkItem retrievedItem = itemService.getItem(itemId);
            assertEquals("Updated title", retrievedItem.getTitle(), "Title should be updated in the stored item");
        }
        
        @Test
        @DisplayName("Should return null when updating non-existent item")
        void shouldReturnNullWhenUpdatingNonExistentItem() {
            // Given
            WorkItem nonExistentItem = new WorkItem();
            nonExistentItem.setId("non-existent-id");
            nonExistentItem.setTitle("This item doesn't exist");
            
            // When
            WorkItem result = itemService.updateItem(nonExistentItem);
            
            // Then
            assertNull(result, "Should return null when updating non-existent item");
        }
        
        @Test
        @DisplayName("Should delete an existing work item")
        void shouldDeleteExistingWorkItem() {
            // Given
            List<WorkItem> allItems = itemService.getAllItems();
            WorkItem itemToDelete = allItems.get(0);
            String itemId = itemToDelete.getId();
            
            // When
            boolean deleted = itemService.deleteItem(itemId);
            
            // Then
            assertTrue(deleted, "Delete operation should return true");
            
            // Verify item is no longer in the list
            List<WorkItem> remainingItems = itemService.getAllItems();
            assertEquals(allItems.size() - 1, remainingItems.size(), "One item should be removed");
            assertNull(itemService.getItem(itemId), "Deleted item should not be retrievable");
        }
        
        @Test
        @DisplayName("Should return false when deleting non-existent item")
        void shouldReturnFalseWhenDeletingNonExistentItem() {
            // Given
            String nonExistentId = "non-existent-id";
            
            // When
            boolean deleted = itemService.deleteItem(nonExistentId);
            
            // Then
            assertFalse(deleted, "Delete operation should return false for non-existent item");
            
            // Verify list size hasn't changed
            List<WorkItem> allItems = itemService.getAllItems();
            assertEquals(3, allItems.size(), "No items should be removed");
        }
    }
    
    @Nested
    @DisplayName("Query Tests")
    class QueryTests {
        
        @Test
        @DisplayName("Should find work items by type")
        void shouldFindWorkItemsByType() {
            // When
            List<WorkItem> taskItems = itemService.findByType(WorkItemType.TASK);
            List<WorkItem> bugItems = itemService.findByType(WorkItemType.BUG);
            List<WorkItem> featureItems = itemService.findByType(WorkItemType.FEATURE);
            
            // Then
            assertEquals(2, taskItems.size(), "Should find 2 TASK items");
            assertEquals(1, bugItems.size(), "Should find 1 BUG item");
            assertEquals(0, featureItems.size(), "Should find 0 FEATURE items");
            
            for (WorkItem item : taskItems) {
                assertEquals(WorkItemType.TASK, item.getType(), "All task items should have TASK type");
            }
            
            for (WorkItem item : bugItems) {
                assertEquals(WorkItemType.BUG, item.getType(), "All bug items should have BUG type");
            }
        }
        
        @Test
        @DisplayName("Should find work items by status")
        void shouldFindWorkItemsByStatus() {
            // When
            List<WorkItem> inProgressItems = itemService.findByStatus(WorkflowState.IN_PROGRESS);
            List<WorkItem> doneItems = itemService.findByStatus(WorkflowState.DONE);
            List<WorkItem> readyItems = itemService.findByStatus(WorkflowState.READY);
            List<WorkItem> openItems = itemService.findByStatus(WorkflowState.OPEN);
            
            // Then
            assertEquals(1, inProgressItems.size(), "Should find 1 IN_PROGRESS item");
            assertEquals(1, doneItems.size(), "Should find 1 DONE item");
            assertEquals(1, readyItems.size(), "Should find 1 READY item");
            assertEquals(0, openItems.size(), "Should find 0 OPEN items");
            
            for (WorkItem item : inProgressItems) {
                assertEquals(WorkflowState.IN_PROGRESS, item.getStatus(), "All in-progress items should have IN_PROGRESS status");
            }
            
            for (WorkItem item : doneItems) {
                assertEquals(WorkflowState.DONE, item.getStatus(), "All done items should have DONE status");
            }
            
            for (WorkItem item : readyItems) {
                assertEquals(WorkflowState.READY, item.getStatus(), "All ready items should have READY status");
            }
        }
        
        @Test
        @DisplayName("Should find work items by state (alias for status)")
        void shouldFindWorkItemsByState() {
            // When
            List<WorkItem> inProgressItemsByState = itemService.findByState(WorkflowState.IN_PROGRESS);
            List<WorkItem> inProgressItemsByStatus = itemService.findByStatus(WorkflowState.IN_PROGRESS);
            
            // Then
            assertEquals(inProgressItemsByStatus.size(), inProgressItemsByState.size(), 
                "findByState should return the same number of items as findByStatus");
            
            // Verify the items are the same
            assertTrue(inProgressItemsByState.containsAll(inProgressItemsByStatus) && 
                      inProgressItemsByStatus.containsAll(inProgressItemsByState),
                "findByState should return the same items as findByStatus");
        }
        
        @Test
        @DisplayName("Should find work items by assignee")
        void shouldFindWorkItemsByAssignee() {
            // Given
            List<WorkItem> allItems = itemService.getAllItems();
            WorkItem assignedItem = allItems.stream()
                .filter(item -> item.getAssignee() != null && item.getAssignee().equals("jane"))
                .findFirst()
                .orElse(null);
            
            assumeNotNull(assignedItem, "Sample dataset must contain an item assigned to 'jane'");
            
            // When
            List<WorkItem> janeItems = itemService.findByAssignee("jane");
            List<WorkItem> nonExistentAssigneeItems = itemService.findByAssignee("nonexistent");
            
            // Then
            assertEquals(1, janeItems.size(), "Should find 1 item assigned to 'jane'");
            assertEquals(0, nonExistentAssigneeItems.size(), "Should find 0 items for non-existent assignee");
            
            for (WorkItem item : janeItems) {
                assertEquals("jane", item.getAssignee(), "All items should have assignee 'jane'");
            }
        }
        
        @Test
        @DisplayName("Should find item by short ID")
        void shouldFindItemByShortId() {
            // Create a work item with a known ID pattern
            WorkItem newItem = new WorkItem();
            String idEndingWith123 = "abcdef-123";
            newItem.setId(idEndingWith123);
            newItem.setTitle("Findable by short ID");
            newItem.setType(WorkItemType.TASK);
            newItem.setPriority(Priority.MEDIUM);
            newItem.setStatus(WorkflowState.OPEN);
            newItem.setProject("TEST");
            itemService.createItem(newItem);
            
            // When
            WorkItem foundByTypePrefix = itemService.findItemByShortId("TASK-123");
            WorkItem foundByProjectPrefix = itemService.findItemByShortId("TEST-123");
            WorkItem notFound = itemService.findItemByShortId("NOSUCH-456");
            
            // Then
            assertNotNull(foundByTypePrefix, "Should find item by type prefix and ID suffix");
            assertEquals(idEndingWith123, foundByTypePrefix.getId(), "Found item ID should match");
            
            assertNotNull(foundByProjectPrefix, "Should find item by project prefix and ID suffix");
            assertEquals(idEndingWith123, foundByProjectPrefix.getId(), "Found item ID should match");
            
            assertNull(notFound, "Should not find non-existent item");
        }
        
        @Test
        @DisplayName("Should check if an item exists")
        void shouldCheckIfItemExists() {
            // Given
            List<WorkItem> allItems = itemService.getAllItems();
            String existingId = allItems.get(0).getId();
            String nonExistingId = "non-existent-id";
            
            // When
            boolean existsResult = itemService.exists(existingId);
            boolean notExistsResult = itemService.exists(nonExistingId);
            
            // Then
            assertTrue(existsResult, "Should return true for existing item");
            assertFalse(notExistsResult, "Should return false for non-existent item");
        }
    }
    
    @Nested
    @DisplayName("Field Update Tests")
    class FieldUpdateTests {
        
        @Test
        @DisplayName("Should update the assignee of a work item")
        void shouldUpdateAssigneeOfWorkItem() {
            // Given
            List<WorkItem> allItems = itemService.getAllItems();
            WorkItem itemToUpdate = allItems.get(0);
            String itemId = itemToUpdate.getId();
            String newAssignee = "newassignee";
            
            // When
            WorkItem updatedItem = itemService.updateAssignee(itemId, newAssignee);
            
            // Then
            assertNotNull(updatedItem, "Updated item should not be null");
            assertEquals(newAssignee, updatedItem.getAssignee(), "Assignee should be updated");
            
            // Verify item is updated in the list
            WorkItem retrievedItem = itemService.getItem(itemId);
            assertEquals(newAssignee, retrievedItem.getAssignee(), "Assignee should be updated in the stored item");
        }
        
        @Test
        @DisplayName("Should return null when updating assignee of non-existent item")
        void shouldReturnNullWhenUpdatingAssigneeOfNonExistentItem() {
            // Given
            String nonExistentId = "non-existent-id";
            String newAssignee = "newassignee";
            
            // When
            WorkItem result = itemService.updateAssignee(nonExistentId, newAssignee);
            
            // Then
            assertNull(result, "Should return null when updating non-existent item");
        }
        
        @Test
        @DisplayName("Should update the title of a work item")
        void shouldUpdateTitleOfWorkItem() {
            // Given
            List<WorkItem> allItems = itemService.getAllItems();
            WorkItem itemToUpdate = allItems.get(0);
            UUID itemId = UUID.fromString(itemToUpdate.getId());
            String newTitle = "Updated title";
            String user = "testuser";
            
            // When
            WorkItem updatedItem = itemService.updateTitle(itemId, newTitle, user);
            
            // Then
            assertNotNull(updatedItem, "Updated item should not be null");
            assertEquals(newTitle, updatedItem.getTitle(), "Title should be updated");
            
            // Verify item is updated in the list
            WorkItem retrievedItem = itemService.getItem(itemId.toString());
            assertEquals(newTitle, retrievedItem.getTitle(), "Title should be updated in the stored item");
        }
        
        @Test
        @DisplayName("Should update the description of a work item")
        void shouldUpdateDescriptionOfWorkItem() {
            // Given
            List<WorkItem> allItems = itemService.getAllItems();
            WorkItem itemToUpdate = allItems.get(0);
            UUID itemId = UUID.fromString(itemToUpdate.getId());
            String newDescription = "Updated description";
            String user = "testuser";
            
            // When
            WorkItem updatedItem = itemService.updateDescription(itemId, newDescription, user);
            
            // Then
            assertNotNull(updatedItem, "Updated item should not be null");
            assertEquals(newDescription, updatedItem.getDescription(), "Description should be updated");
            
            // Verify item is updated in the list
            WorkItem retrievedItem = itemService.getItem(itemId.toString());
            assertEquals(newDescription, retrievedItem.getDescription(), "Description should be updated in the stored item");
        }
        
        @Test
        @DisplayName("Should update the priority of a work item")
        void shouldUpdatePriorityOfWorkItem() {
            // Given
            List<WorkItem> allItems = itemService.getAllItems();
            WorkItem itemToUpdate = allItems.get(0);
            UUID itemId = UUID.fromString(itemToUpdate.getId());
            Priority newPriority = Priority.CRITICAL;
            String user = "testuser";
            
            // When
            WorkItem updatedItem = itemService.updatePriority(itemId, newPriority, user);
            
            // Then
            assertNotNull(updatedItem, "Updated item should not be null");
            assertEquals(newPriority, updatedItem.getPriority(), "Priority should be updated");
            
            // Verify item is updated in the list
            WorkItem retrievedItem = itemService.getItem(itemId.toString());
            assertEquals(newPriority, retrievedItem.getPriority(), "Priority should be updated in the stored item");
        }
        
        @Test
        @DisplayName("Should update the state of a work item")
        void shouldUpdateStateOfWorkItem() {
            // Given
            List<WorkItem> allItems = itemService.getAllItems();
            WorkItem itemToUpdate = allItems.get(0);
            UUID itemId = UUID.fromString(itemToUpdate.getId());
            WorkflowState newState = WorkflowState.DONE;
            String user = "testuser";
            
            // When
            WorkItem updatedItem = itemService.updateState(itemId, newState, user);
            
            // Then
            assertNotNull(updatedItem, "Updated item should not be null");
            assertEquals(newState, updatedItem.getStatus(), "State should be updated");
            
            // Verify item is updated in the list
            WorkItem retrievedItem = itemService.getItem(itemId.toString());
            assertEquals(newState, retrievedItem.getStatus(), "State should be updated in the stored item");
        }
        
        @Test
        @DisplayName("Should update a generic field of a work item")
        void shouldUpdateGenericFieldOfWorkItem() {
            // Given
            List<WorkItem> allItems = itemService.getAllItems();
            WorkItem itemToUpdate = allItems.get(0);
            UUID itemId = UUID.fromString(itemToUpdate.getId());
            String field = "version";
            String value = "1.2.3";
            String user = "testuser";
            
            // When
            WorkItem updatedItem = itemService.updateField(itemId, field, value, user);
            
            // Then
            assertNotNull(updatedItem, "Updated item should not be null");
            assertEquals(value, updatedItem.getVersion(), "Version should be updated");
            
            // Verify item is updated in the list
            WorkItem retrievedItem = itemService.getItem(itemId.toString());
            assertEquals(value, retrievedItem.getVersion(), "Version should be updated in the stored item");
        }
        
        @Test
        @DisplayName("Should throw exception when updating unsupported field")
        void shouldThrowExceptionWhenUpdatingUnsupportedField() {
            // Given
            List<WorkItem> allItems = itemService.getAllItems();
            WorkItem itemToUpdate = allItems.get(0);
            UUID itemId = UUID.fromString(itemToUpdate.getId());
            String field = "unsupported";
            String value = "value";
            String user = "testuser";
            
            // When/Then
            assertThrows(IllegalArgumentException.class, () -> {
                itemService.updateField(itemId, field, value, user);
            }, "Should throw IllegalArgumentException for unsupported field");
        }
        
        @Test
        @DisplayName("Should assign a work item to a user")
        void shouldAssignWorkItemToUser() {
            // Given
            List<WorkItem> allItems = itemService.getAllItems();
            WorkItem itemToUpdate = allItems.get(0);
            UUID itemId = UUID.fromString(itemToUpdate.getId());
            String newAssignee = "newassignee";
            String user = "testuser";
            
            // When
            WorkItem updatedItem = itemService.assignTo(itemId, newAssignee, user);
            
            // Then
            assertNotNull(updatedItem, "Updated item should not be null");
            assertEquals(newAssignee, updatedItem.getAssignee(), "Assignee should be updated");
            
            // Verify item is updated in the list
            WorkItem retrievedItem = itemService.getItem(itemId.toString());
            assertEquals(newAssignee, retrievedItem.getAssignee(), "Assignee should be updated in the stored item");
        }
        
        @Test
        @DisplayName("Should update metadata for a work item")
        void shouldUpdateMetadataForWorkItem() {
            // Given
            List<WorkItem> allItems = itemService.getAllItems();
            WorkItem itemToUpdate = allItems.get(0);
            String itemId = itemToUpdate.getId();
            Map<String, String> metadata = new HashMap<>();
            metadata.put("project", "NEWPROJ");
            
            // When
            boolean updated = itemService.updateMetadata(itemId, metadata);
            
            // Then
            assertTrue(updated, "Update operation should return true");
            
            // Verify item is updated in the list
            WorkItem retrievedItem = itemService.getItem(itemId);
            assertEquals("NEWPROJ", retrievedItem.getProject(), "Project should be updated in the stored item");
        }
        
        @Test
        @DisplayName("Should return false when updating metadata for non-existent item")
        void shouldReturnFalseWhenUpdatingMetadataForNonExistentItem() {
            // Given
            String nonExistentId = "non-existent-id";
            Map<String, String> metadata = new HashMap<>();
            metadata.put("project", "NEWPROJ");
            
            // When
            boolean updated = itemService.updateMetadata(nonExistentId, metadata);
            
            // Then
            assertFalse(updated, "Update operation should return false for non-existent item");
        }
    }
    
    // Helper method to assume a condition is true, used to verify test prerequisites
    private static void assumeNotNull(Object object, String message) {
        if (object == null) {
            throw new AssumptionViolatedException(message);
        }
    }
    
    // Custom exception for assumption violations
    private static class AssumptionViolatedException extends RuntimeException {
        public AssumptionViolatedException(String message) {
            super(message);
        }
    }
}