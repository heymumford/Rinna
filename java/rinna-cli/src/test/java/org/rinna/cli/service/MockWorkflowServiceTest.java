/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

@DisplayName("MockWorkflowService Tests")
class MockWorkflowServiceTest {
    private MockWorkflowService workflowService;
    private String currentUser;
    
    @BeforeEach
    void setUp() {
        workflowService = new MockWorkflowService();
        currentUser = System.getProperty("user.name");
    }
    
    @Nested
    @DisplayName("Initialization Tests")
    class InitializationTests {
        @Test
        @DisplayName("Should initialize with sample work items")
        void shouldInitializeWithSampleWorkItems() {
            // When we create a MockWorkflowService instance
            // Then it should have pre-populated work items
            List<WorkItem> inProgressItems = workflowService.getItemsInState(WorkflowState.IN_PROGRESS);
            List<WorkItem> readyItems = workflowService.getItemsInState(WorkflowState.READY);
            
            // Verify
            assertFalse(inProgressItems.isEmpty(), "Should have items in IN_PROGRESS state");
            assertFalse(readyItems.isEmpty(), "Should have items in READY state");
            
            // Check specific items
            WorkItem inProgressItem = inProgressItems.get(0);
            assertEquals("Implement authentication feature", inProgressItem.getTitle());
            assertEquals(WorkItemType.TASK, inProgressItem.getType());
            assertEquals(Priority.HIGH, inProgressItem.getPriority());
            assertEquals(currentUser, inProgressItem.getAssignee());
            
            WorkItem readyItem = readyItems.get(0);
            assertEquals("Fix bug in payment module", readyItem.getTitle());
            assertEquals(WorkItemType.BUG, readyItem.getType());
            assertEquals(Priority.CRITICAL, readyItem.getPriority());
        }
    }
    
    @Nested
    @DisplayName("Item Retrieval Tests")
    class ItemRetrievalTests {
        @Test
        @DisplayName("Should get item by UUID")
        void shouldGetItemByUuid() {
            // Given
            WorkItem expectedItem = workflowService.getItemsInState(WorkflowState.IN_PROGRESS).get(0);
            UUID id = UUID.fromString(expectedItem.getId());
            
            // When
            WorkItem actualItem = workflowService.getItem(id);
            
            // Then
            assertNotNull(actualItem, "Should find item by UUID");
            assertEquals(expectedItem.getId(), actualItem.getId());
            assertEquals(expectedItem.getTitle(), actualItem.getTitle());
        }
        
        @Test
        @DisplayName("Should get item by string ID")
        void shouldGetItemByStringId() {
            // Given
            WorkItem expectedItem = workflowService.getItemsInState(WorkflowState.IN_PROGRESS).get(0);
            String id = expectedItem.getId();
            
            // When
            WorkItem actualItem = workflowService.getItem(id);
            
            // Then
            assertNotNull(actualItem, "Should find item by string ID");
            assertEquals(expectedItem.getId(), actualItem.getId());
            assertEquals(expectedItem.getTitle(), actualItem.getTitle());
        }
        
        @Test
        @DisplayName("Should return null for non-existent item ID")
        void shouldReturnNullForNonExistentItemId() {
            // Given
            String nonExistentId = UUID.randomUUID().toString();
            
            // When
            WorkItem item = workflowService.getItem(nonExistentId);
            
            // Then
            assertNull(item, "Should return null for non-existent ID");
        }
        
        @Test
        @DisplayName("Should get items by workflow state")
        void shouldGetItemsByWorkflowState() {
            // When
            List<WorkItem> inProgressItems = workflowService.getItemsInState(WorkflowState.IN_PROGRESS);
            List<WorkItem> readyItems = workflowService.getItemsInState(WorkflowState.READY);
            List<WorkItem> doneItems = workflowService.getItemsInState(WorkflowState.DONE);
            
            // Then
            assertFalse(inProgressItems.isEmpty(), "Should have IN_PROGRESS items");
            assertFalse(readyItems.isEmpty(), "Should have READY items");
            assertTrue(doneItems.isEmpty(), "Should not have DONE items initially");
            
            // All returned items should have the correct state
            for (WorkItem item : inProgressItems) {
                assertEquals(WorkflowState.IN_PROGRESS, item.getState());
            }
            
            for (WorkItem item : readyItems) {
                assertEquals(WorkflowState.READY, item.getState());
            }
        }
        
        @Test
        @DisplayName("Should get items by workflow state and assignee")
        void shouldGetItemsByWorkflowStateAndAssignee() {
            // Given
            String assignee = currentUser;
            
            // When
            List<WorkItem> assignedInProgressItems = workflowService.getItemsInState(WorkflowState.IN_PROGRESS, assignee);
            List<WorkItem> assignedReadyItems = workflowService.getItemsInState(WorkflowState.READY, assignee);
            
            // Then
            assertFalse(assignedInProgressItems.isEmpty(), "Should have assigned IN_PROGRESS items");
            assertTrue(assignedReadyItems.isEmpty(), "Should not have assigned READY items");
            
            // All returned items should have the correct state and assignee
            for (WorkItem item : assignedInProgressItems) {
                assertEquals(WorkflowState.IN_PROGRESS, item.getState());
                assertEquals(assignee, item.getAssignee());
            }
        }
        
        @Test
        @DisplayName("Should get items by status (alternative method)")
        void shouldGetItemsByStatus() {
            // When
            List<WorkItem> inProgressItems = workflowService.findByStatus(WorkflowState.IN_PROGRESS);
            
            // Then
            assertFalse(inProgressItems.isEmpty(), "Should have IN_PROGRESS items");
            
            // All returned items should have the correct state
            for (WorkItem item : inProgressItems) {
                assertEquals(WorkflowState.IN_PROGRESS, item.getState());
            }
        }
    }
    
    @Nested
    @DisplayName("State Transition Tests")
    class StateTransitionTests {
        private String taskItemId;
        private String bugItemId;
        
        @BeforeEach
        void setupItems() {
            // Get IDs of the sample items
            taskItemId = workflowService.getItemsInState(WorkflowState.IN_PROGRESS).get(0).getId();
            bugItemId = workflowService.getItemsInState(WorkflowState.READY).get(0).getId();
        }
        
        @Test
        @DisplayName("Should transition item to valid next state")
        void shouldTransitionItemToValidNextState() throws InvalidTransitionException {
            // Given
            WorkItem item = workflowService.getItem(taskItemId);
            assertEquals(WorkflowState.IN_PROGRESS, item.getState());
            
            // When
            WorkItem updatedItem = workflowService.transition(taskItemId, "user1", WorkflowState.DONE, "Completed task");
            
            // Then
            assertEquals(WorkflowState.DONE, updatedItem.getState());
            assertEquals(WorkflowState.DONE, workflowService.getItem(taskItemId).getState());
        }
        
        @Test
        @DisplayName("Should transition using UUID version")
        void shouldTransitionUsingUuidVersion() throws InvalidTransitionException {
            // Given
            UUID id = UUID.fromString(taskItemId);
            
            // When
            WorkItem updatedItem = workflowService.transition(id, "user1", WorkflowState.DONE, "Completed task");
            
            // Then
            assertEquals(WorkflowState.DONE, updatedItem.getState());
        }
        
        @Test
        @DisplayName("Should transition using simplified method")
        void shouldTransitionUsingSimplifiedMethod() throws InvalidTransitionException {
            // Given
            WorkItem item = workflowService.getItem(bugItemId);
            assertEquals(WorkflowState.READY, item.getState());
            
            // When
            WorkItem updatedItem = workflowService.transition(bugItemId, WorkflowState.IN_PROGRESS);
            
            // Then
            assertEquals(WorkflowState.IN_PROGRESS, updatedItem.getState());
        }
        
        @Test
        @DisplayName("Should throw exception for invalid transition")
        void shouldThrowExceptionForInvalidTransition() {
            // Given
            WorkItem item = workflowService.getItem(taskItemId);
            assertEquals(WorkflowState.IN_PROGRESS, item.getState());
            
            // When/Then
            assertThrows(InvalidTransitionException.class, () -> {
                workflowService.transition(taskItemId, WorkflowState.CREATED);
            });
        }
        
        @Test
        @DisplayName("Should throw exception for non-existent item")
        void shouldThrowExceptionForNonExistentItem() {
            // Given
            String nonExistentId = UUID.randomUUID().toString();
            
            // When/Then
            assertThrows(InvalidTransitionException.class, () -> {
                workflowService.transition(nonExistentId, WorkflowState.DONE);
            });
        }
    }
    
    @Nested
    @DisplayName("Transition Validation Tests")
    class TransitionValidationTests {
        private String taskItemId;
        
        @BeforeEach
        void setupItems() {
            // Get ID of the sample task item
            taskItemId = workflowService.getItemsInState(WorkflowState.IN_PROGRESS).get(0).getId();
        }
        
        @Test
        @DisplayName("Should validate valid transitions for in-progress item")
        void shouldValidateValidTransitionsForInProgressItem() {
            // When
            boolean canTransitionToReady = workflowService.canTransition(taskItemId, WorkflowState.READY);
            boolean canTransitionToBlocked = workflowService.canTransition(taskItemId, WorkflowState.BLOCKED);
            boolean canTransitionToDone = workflowService.canTransition(taskItemId, WorkflowState.DONE);
            boolean canTransitionToCreated = workflowService.canTransition(taskItemId, WorkflowState.CREATED);
            
            // Then
            assertTrue(canTransitionToReady, "Should allow transition to READY");
            assertTrue(canTransitionToBlocked, "Should allow transition to BLOCKED");
            assertTrue(canTransitionToDone, "Should allow transition to DONE");
            assertFalse(canTransitionToCreated, "Should not allow transition to CREATED");
        }
        
        @Test
        @DisplayName("Should return available transitions")
        void shouldReturnAvailableTransitions() {
            // When
            List<WorkflowState> availableTransitions = workflowService.getAvailableTransitions(taskItemId);
            
            // Then
            assertNotNull(availableTransitions);
            assertTrue(availableTransitions.contains(WorkflowState.READY));
            assertTrue(availableTransitions.contains(WorkflowState.BLOCKED));
            assertTrue(availableTransitions.contains(WorkflowState.DONE));
            assertFalse(availableTransitions.contains(WorkflowState.CREATED));
        }
        
        @Test
        @DisplayName("Should return current state")
        void shouldReturnCurrentState() {
            // When
            WorkflowState currentState = workflowService.getCurrentState(taskItemId);
            
            // Then
            assertEquals(WorkflowState.IN_PROGRESS, currentState);
        }
        
        @Test
        @DisplayName("Should return null state for non-existent item")
        void shouldReturnNullStateForNonExistentItem() {
            // Given
            String nonExistentId = UUID.randomUUID().toString();
            
            // When
            WorkflowState currentState = workflowService.getCurrentState(nonExistentId);
            
            // Then
            assertNull(currentState);
        }
    }
    
    @Nested
    @DisplayName("Work Assignment Tests")
    class WorkAssignmentTests {
        private String taskItemId;
        private String bugItemId;
        
        @BeforeEach
        void setupItems() {
            taskItemId = workflowService.getItemsInState(WorkflowState.IN_PROGRESS).get(0).getId();
            bugItemId = workflowService.getItemsInState(WorkflowState.READY).get(0).getId();
        }
        
        @Test
        @DisplayName("Should assign work item to user")
        void shouldAssignWorkItemToUser() throws InvalidTransitionException {
            // Given
            String newAssignee = "testuser";
            
            // When
            WorkItem updatedItem = workflowService.assignWorkItem(bugItemId, "admin", newAssignee);
            
            // Then
            assertEquals(newAssignee, updatedItem.getAssignee());
            assertEquals(newAssignee, workflowService.getItem(bugItemId).getAssignee());
        }
        
        @Test
        @DisplayName("Should assign work item with comment")
        void shouldAssignWorkItemWithComment() throws InvalidTransitionException {
            // Given
            String newAssignee = "testuser";
            String comment = "Assigning to test user for specialized testing";
            
            // When
            WorkItem updatedItem = workflowService.assignWorkItem(bugItemId, "admin", newAssignee, comment);
            
            // Then
            assertEquals(newAssignee, updatedItem.getAssignee());
        }
        
        @Test
        @DisplayName("Should throw exception when assigning non-existent item")
        void shouldThrowExceptionWhenAssigningNonExistentItem() {
            // Given
            String nonExistentId = UUID.randomUUID().toString();
            
            // When/Then
            assertThrows(InvalidTransitionException.class, () -> {
                workflowService.assignWorkItem(nonExistentId, "admin", "testuser");
            });
        }
    }
    
    @Nested
    @DisplayName("Current Work Item Tests")
    class CurrentWorkItemTests {
        @Test
        @DisplayName("Should get current active item for assigned user")
        void shouldGetCurrentActiveItemForAssignedUser() {
            // Given
            String user = currentUser;
            
            // When
            UUID currentItemId = workflowService.getCurrentActiveItemId(user);
            String currentItemIdString = workflowService.getCurrentActiveItemIdAsString(user);
            
            // Then
            assertNotNull(currentItemId, "Should have current active item");
            assertNotNull(currentItemIdString, "Should have current active item string ID");
            assertEquals(currentItemId.toString(), currentItemIdString);
            
            WorkItem item = workflowService.getItem(currentItemId);
            assertEquals(WorkflowState.IN_PROGRESS, item.getState());
            assertEquals(user, item.getAssignee());
        }
        
        @Test
        @DisplayName("Should return null for user with no active items")
        void shouldReturnNullForUserWithNoActiveItems() {
            // Given
            String userWithNoItems = "nonexistentuser";
            
            // When
            UUID currentItemId = workflowService.getCurrentActiveItemId(userWithNoItems);
            String currentItemIdString = workflowService.getCurrentActiveItemIdAsString(userWithNoItems);
            
            // Then
            assertNull(currentItemId, "Should return null UUID for user with no items");
            assertNull(currentItemIdString, "Should return null string for user with no items");
        }
        
        @Test
        @DisplayName("Should get current work in progress")
        void shouldGetCurrentWorkInProgress() {
            // Given
            String user = currentUser;
            
            // When
            Optional<WorkItem> currentItem = workflowService.getCurrentWorkInProgress(user);
            
            // Then
            assertTrue(currentItem.isPresent(), "Should have work in progress");
            WorkItem item = currentItem.get();
            assertEquals(WorkflowState.IN_PROGRESS, item.getState());
            assertEquals(user, item.getAssignee());
        }
        
        @Test
        @DisplayName("Should get current work item regardless of state")
        void shouldGetCurrentWorkItemRegardlessOfState() {
            // Given
            String user = currentUser;
            
            // When
            WorkItem currentItem = workflowService.getCurrentWorkItem(user);
            
            // Then
            assertNotNull(currentItem, "Should have current work item");
            assertEquals(user, currentItem.getAssignee());
        }
    }
    
    @Nested
    @DisplayName("Active Item Management Tests")
    class ActiveItemManagementTests {
        @Test
        @DisplayName("Should set current active item")
        void shouldSetCurrentActiveItem() {
            // Given
            String itemId = workflowService.getItemsInState(WorkflowState.READY).get(0).getId();
            UUID uuid = UUID.fromString(itemId);
            String user = "testuser";
            
            // When
            boolean result = workflowService.setCurrentActiveItem(uuid, user);
            
            // Then
            assertTrue(result, "Should successfully set active item");
        }
        
        @Test
        @DisplayName("Should return false when setting non-existent item as active")
        void shouldReturnFalseWhenSettingNonExistentItemAsActive() {
            // Given
            UUID nonExistentId = UUID.randomUUID();
            String user = "testuser";
            
            // When
            boolean result = workflowService.setCurrentActiveItem(nonExistentId, user);
            
            // Then
            assertFalse(result, "Should return false for non-existent item");
        }
        
        @Test
        @DisplayName("Should clear current active item")
        void shouldClearCurrentActiveItem() {
            // Given
            String user = "testuser";
            
            // When
            boolean result = workflowService.clearCurrentActiveItem(user);
            
            // Then
            assertTrue(result, "Should successfully clear active item");
        }
    }
}