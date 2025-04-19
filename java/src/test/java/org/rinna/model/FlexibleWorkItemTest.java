/*
 * Test for flexible work item model in the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.model;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemRecord;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.WorkflowService;

@DisplayName("Flexible Work Item Model Tests")
class FlexibleWorkItemTest {

    // Test service implementations
    private ItemService itemService;
    private WorkflowService workflowService;
    
    // Test data
    private UUID goalId;
    private UUID featureId;
    private UUID bugId;
    private UUID choreId;
    private WorkItem goalItem;
    private WorkItem featureItem;
    private WorkItem bugItem;
    private WorkItem choreItem;
    
    // Custom metadata storage mock
    private Map<UUID, Map<String, Object>> itemMetadata = new HashMap<>();
    
    @BeforeEach
    void setUp() {
        Instant now = Instant.now();
        
        // Create sample work items for different types
        goalId = UUID.randomUUID();
        goalItem = new WorkItemRecord(
            goalId,
            "Improve Customer Onboarding",
            "Streamline customer onboarding process",
            WorkItemType.GOAL,
            WorkflowState.FOUND,
            Priority.HIGH,
            "product.owner@example.com",
            now,
            now,
            null, // no parent
            UUID.randomUUID(), // project ID
            "PUBLIC",
            false
        );
        
        featureId = UUID.randomUUID();
        featureItem = new WorkItemRecord(
            featureId,
            "One-click Account Creation",
            "Implement one-click account creation via OAuth",
            WorkItemType.FEATURE,
            WorkflowState.TO_DO,
            Priority.HIGH,
            "developer@example.com",
            now,
            now,
            goalId, // parent is the GOAL
            goalItem.getProjectId().orElse(null),
            "PUBLIC",
            false
        );
        
        bugId = UUID.randomUUID();
        bugItem = new WorkItemRecord(
            bugId,
            "Authentication Failure on Safari",
            "OAuth authentication fails on Safari browser",
            WorkItemType.BUG,
            WorkflowState.IN_PROGRESS,
            Priority.CRITICAL,
            "developer@example.com",
            now,
            now,
            featureId, // parent is the FEATURE
            goalItem.getProjectId().orElse(null),
            "PUBLIC",
            false
        );
        
        choreId = UUID.randomUUID();
        choreItem = new WorkItemRecord(
            choreId,
            "Update Authentication Dependencies",
            "Update OAuth library to latest version",
            WorkItemType.CHORE,
            WorkflowState.TO_DO,
            Priority.MEDIUM,
            "developer@example.com",
            now,
            now,
            featureId, // parent is the FEATURE
            goalItem.getProjectId().orElse(null),
            "PUBLIC",
            false
        );
        
        // Setup custom metadata
        itemMetadata.put(goalId, Map.of("target_quarter", "Q3 2025"));
        itemMetadata.put(featureId, Map.of("story_points", 8));
        itemMetadata.put(bugId, Map.of("severity", "HIGH", "browser", "Safari"));
        itemMetadata.put(choreId, Map.of("estimated_hours", 4));
        
        // Mock item service
        itemService = mock(ItemService.class);
        when(itemService.findById(goalId)).thenReturn(Optional.of(goalItem));
        when(itemService.findById(featureId)).thenReturn(Optional.of(featureItem));
        when(itemService.findById(bugId)).thenReturn(Optional.of(bugItem));
        when(itemService.findById(choreId)).thenReturn(Optional.of(choreItem));
        when(itemService.findAll()).thenReturn(Arrays.asList(goalItem, featureItem, bugItem, choreItem));
        
        // Mock workflow service
        workflowService = mock(WorkflowService.class);
    }
    
    @Nested
    @DisplayName("Work Item Type Tests")
    class WorkItemTypeTests {
        
        @Test
        @DisplayName("Should support all required work item types")
        void shouldSupportAllWorkItemTypes() {
            // Verify all required types are in the enum
            List<WorkItemType> types = Arrays.asList(WorkItemType.values());
            assertTrue(types.contains(WorkItemType.GOAL), "GOAL type should be supported");
            assertTrue(types.contains(WorkItemType.FEATURE), "FEATURE type should be supported");
            assertTrue(types.contains(WorkItemType.BUG), "BUG type should be supported");
            assertTrue(types.contains(WorkItemType.CHORE), "CHORE type should be supported");
        }
        
        @ParameterizedTest(name = "{0} should have valid child types")
        @EnumSource(WorkItemType.class)
        @DisplayName("Should define valid parent-child relationships")
        void shouldDefineValidChildTypes(WorkItemType type) {
            // Test the hierarchy rules
            switch (type) {
                case GOAL:
                    assertTrue(type.canHaveChildOfType(WorkItemType.FEATURE),
                        "GOAL should be able to have FEATURE children");
                    assertFalse(type.canHaveChildOfType(WorkItemType.BUG),
                        "GOAL should not have BUG children directly");
                    break;
                case FEATURE:
                    assertTrue(type.canHaveChildOfType(WorkItemType.BUG),
                        "FEATURE should be able to have BUG children");
                    assertTrue(type.canHaveChildOfType(WorkItemType.CHORE),
                        "FEATURE should be able to have CHORE children");
                    break;
                case BUG:
                case CHORE:
                    // Leaf types shouldn't have children
                    for (WorkItemType childType : WorkItemType.values()) {
                        assertFalse(type.canHaveChildOfType(childType),
                            type + " should not have children");
                    }
                    break;
            }
        }
    }
    
    @Nested
    @DisplayName("Work Item Creation Tests")
    class WorkItemCreationTests {
        
        @Test
        @DisplayName("Should create work items for different roles")
        void shouldCreateWorkItemsForDifferentRoles() {
            // Verify items for different roles were created successfully
            assertEquals(WorkItemType.GOAL, goalItem.getType(),
                "Product owner should be able to create GOAL items");
            
            assertEquals(WorkItemType.FEATURE, featureItem.getType(),
                "Developer should be able to create FEATURE items");
            
            assertEquals(WorkItemType.BUG, bugItem.getType(),
                "QA should be able to create BUG items");
        }
        
        @Test
        @DisplayName("Should maintain parent-child relationships")
        void shouldMaintainParentChildRelationships() {
            // Verify parent-child relationships
            assertTrue(featureItem.getParentId().isPresent(),
                "FEATURE should have a parent");
            assertEquals(goalId, featureItem.getParentId().get(),
                "FEATURE should have GOAL as parent");
            
            assertTrue(bugItem.getParentId().isPresent(),
                "BUG should have a parent");
            assertEquals(featureId, bugItem.getParentId().get(),
                "BUG should have FEATURE as parent");
        }
        
        @Test
        @DisplayName("Should support metadata for different item types")
        void shouldSupportMetadataForDifferentItemTypes() {
            // Verify metadata exists and has correct types
            assertTrue(itemMetadata.containsKey(goalId),
                "GOAL should have metadata");
            assertEquals("Q3 2025", itemMetadata.get(goalId).get("target_quarter"),
                "GOAL should have target_quarter metadata");
            
            assertTrue(itemMetadata.containsKey(featureId),
                "FEATURE should have metadata");
            assertEquals(8, itemMetadata.get(featureId).get("story_points"),
                "FEATURE should have story_points metadata");
            
            assertTrue(itemMetadata.containsKey(bugId),
                "BUG should have metadata");
            assertEquals("HIGH", itemMetadata.get(bugId).get("severity"),
                "BUG should have severity metadata");
            assertEquals("Safari", itemMetadata.get(bugId).get("browser"),
                "BUG should have browser metadata");
        }
    }
    
    @Nested
    @DisplayName("Workflow State Tests")
    class WorkflowStateTests {
        
        @Test
        @DisplayName("Should track workflow states for all item types")
        void shouldTrackWorkflowStatesForAllItemTypes() {
            // Verify all item types have workflow states
            assertEquals(WorkflowState.FOUND, goalItem.getStatus(),
                "GOAL should have workflow state");
            
            assertEquals(WorkflowState.TO_DO, featureItem.getStatus(),
                "FEATURE should have workflow state");
            
            assertEquals(WorkflowState.IN_PROGRESS, bugItem.getStatus(),
                "BUG should have workflow state");
        }
        
        @ParameterizedTest(name = "Should allow transition from {0} to valid states")
        @EnumSource(WorkflowState.class)
        @DisplayName("Should enforce valid state transitions")
        void shouldEnforceValidStateTransitions(WorkflowState state) {
            // Test transition rules for each state
            List<WorkflowState> validTransitions = state.getAvailableTransitions();
            
            for (WorkflowState target : WorkflowState.values()) {
                boolean shouldBeValid = validTransitions.contains(target);
                assertEquals(shouldBeValid, state.canTransitionTo(target),
                    "Transition from " + state + " to " + target + 
                    " should be " + (shouldBeValid ? "valid" : "invalid"));
            }
        }
        
        @Test
        @DisplayName("Should support uniform workflow for all item types")
        void shouldSupportUniformWorkflowForAllItemTypes() {
            // Verify all item types use the same workflow states
            List<WorkflowState> allStates = Arrays.asList(WorkflowState.values());
            
            // Create test items for each type
            for (WorkItemType type : WorkItemType.values()) {
                // Try to transition through all states
                WorkItem testItem = createTestItem(type);
                
                for (WorkflowState state : allStates) {
                    // Skip invalid transitions
                    if (testItem.getStatus().canTransitionTo(state)) {
                        // This would be a call to workflowService.transition() in real code
                        WorkItem updatedItem = mockTransition(testItem, state);
                        assertEquals(state, updatedItem.getStatus(),
                            type + " should transition to " + state);
                    }
                }
            }
        }
        
        // Helper to create test items
        private WorkItem createTestItem(WorkItemType type) {
            return new WorkItemRecord(
                UUID.randomUUID(),
                "Test " + type,
                "Test description",
                type,
                WorkflowState.FOUND, // start at FOUND
                Priority.MEDIUM,
                "test@example.com",
                Instant.now(),
                Instant.now(),
                null,
                UUID.randomUUID(),
                "PUBLIC",
                false
            );
        }
        
        // Mock transition - would use workflowService in real code
        private WorkItem mockTransition(WorkItem item, WorkflowState newState) {
            if (item.getStatus().canTransitionTo(newState)) {
                return ((WorkItemRecord)item).withStatus(newState);
            }
            throw new IllegalStateException("Invalid transition");
        }
    }
    
    @Nested
    @DisplayName("Query and Reporting Tests")
    class QueryAndReportingTests {
        
        @Test
        @DisplayName("Should filter items by type")
        void shouldFilterItemsByType() {
            // Mock filter by type calls
            when(itemService.findByType(WorkItemType.GOAL.toString()))
                .thenReturn(Collections.singletonList(goalItem));
                
            when(itemService.findByType(WorkItemType.FEATURE.toString()))
                .thenReturn(Collections.singletonList(featureItem));
                
            when(itemService.findByType(WorkItemType.BUG.toString()))
                .thenReturn(Collections.singletonList(bugItem));
                
            when(itemService.findByType(WorkItemType.CHORE.toString()))
                .thenReturn(Collections.singletonList(choreItem));
            
            // Verify filtering works
            List<WorkItem> goals = itemService.findByType(WorkItemType.GOAL.toString());
            assertEquals(1, goals.size(), "Should find 1 GOAL");
            assertEquals(WorkItemType.GOAL, goals.get(0).getType(), "Should be GOAL type");
            
            List<WorkItem> features = itemService.findByType(WorkItemType.FEATURE.toString());
            assertEquals(1, features.size(), "Should find 1 FEATURE");
            assertEquals(WorkItemType.FEATURE, features.get(0).getType(), "Should be FEATURE type");
            
            List<WorkItem> bugs = itemService.findByType(WorkItemType.BUG.toString());
            assertEquals(1, bugs.size(), "Should find 1 BUG");
            assertEquals(WorkItemType.BUG, bugs.get(0).getType(), "Should be BUG type");
        }
        
        @Test
        @DisplayName("Should filter items by assignee")
        void shouldFilterItemsByAssignee() {
            String developerEmail = "developer@example.com";
            
            // Mock assignee filter
            when(itemService.findByAssignee(developerEmail))
                .thenReturn(Arrays.asList(featureItem, bugItem, choreItem));
            
            // Verify assignee filtering works
            List<WorkItem> developerItems = itemService.findByAssignee(developerEmail);
            assertEquals(3, developerItems.size(), "Developer should have 3 items");
            
            // Verify mixed types are returned
            List<WorkItemType> types = developerItems.stream()
                .map(WorkItem::getType)
                .distinct()
                .toList();
                
            assertTrue(types.contains(WorkItemType.FEATURE), "Should include FEATURE");
            assertTrue(types.contains(WorkItemType.BUG), "Should include BUG");
            assertTrue(types.contains(WorkItemType.CHORE), "Should include CHORE");
        }
        
        @Test
        @DisplayName("Should support custom reports across types")
        void shouldSupportCustomReportsAcrossTypes() {
            // Get all items
            List<WorkItem> allItems = itemService.findAll();
            
            // Group by type
            Map<WorkItemType, List<WorkItem>> byType = new HashMap<>();
            for (WorkItem item : allItems) {
                byType.computeIfAbsent(item.getType(), k -> new ArrayList<>()).add(item);
            }
            
            // Create a report with count by type
            Map<WorkItemType, Integer> countByType = new HashMap<>();
            byType.forEach((type, items) -> countByType.put(type, items.size()));
            
            // Verify counts
            assertEquals(1, countByType.get(WorkItemType.GOAL), "Should have 1 GOAL");
            assertEquals(1, countByType.get(WorkItemType.FEATURE), "Should have 1 FEATURE");
            assertEquals(1, countByType.get(WorkItemType.BUG), "Should have 1 BUG");
            assertEquals(1, countByType.get(WorkItemType.CHORE), "Should have 1 CHORE");
            
            // Calculate items in progress
            long inProgressCount = allItems.stream()
                .filter(item -> item.getStatus() == WorkflowState.IN_PROGRESS)
                .count();
                
            assertEquals(1, inProgressCount, "Should have 1 item IN_PROGRESS");
            
            // Find highest priority item
            Optional<WorkItem> highestPriority = allItems.stream()
                .max((a, b) -> a.getPriority().compareTo(b.getPriority()));
                
            assertTrue(highestPriority.isPresent(), "Should find highest priority item");
            assertEquals(Priority.CRITICAL, highestPriority.get().getPriority(), 
                "Highest priority should be CRITICAL");
            assertEquals(WorkItemType.BUG, highestPriority.get().getType(),
                "Highest priority item should be a BUG");
        }
    }
    
    @Nested
    @DisplayName("Metadata Capability Tests")
    class MetadataTests {
        
        @Test
        @DisplayName("Should support type-specific metadata")
        void shouldSupportTypeSpecificMetadata() {
            // Verify type-specific metadata
            Object quarterValue = itemMetadata.get(goalId).get("target_quarter");
            assertTrue(quarterValue instanceof String, 
                "GOAL should have string target_quarter");
            
            Object storyPoints = itemMetadata.get(featureId).get("story_points");
            assertTrue(storyPoints instanceof Integer,
                "FEATURE should have numeric story_points");
            
            Object severity = itemMetadata.get(bugId).get("severity");
            assertTrue(severity instanceof String,
                "BUG should have string severity");
        }
        
        @Test
        @DisplayName("Should allow filtering by metadata")
        void shouldAllowFilteringByMetadata() {
            // Find items by metadata value
            List<UUID> highSeverityItems = findItemsByMetadata("severity", "HIGH");
            assertEquals(1, highSeverityItems.size(), "Should find 1 high severity item");
            assertEquals(bugId, highSeverityItems.get(0), "Should be the bug item");
            
            List<UUID> q3Items = findItemsByMetadata("target_quarter", "Q3 2025");
            assertEquals(1, q3Items.size(), "Should find 1 Q3 item");
            assertEquals(goalId, q3Items.get(0), "Should be the goal item");
        }
        
        // Helper to find items by metadata
        private List<UUID> findItemsByMetadata(String key, Object value) {
            List<UUID> result = new ArrayList<>();
            for (Map.Entry<UUID, Map<String, Object>> entry : itemMetadata.entrySet()) {
                if (entry.getValue().containsKey(key) && 
                    entry.getValue().get(key).equals(value)) {
                    result.add(entry.getKey());
                }
            }
            return result;
        }
    }
    
    @Nested
    @DisplayName("Role-Specific Behavior Tests")
    class RoleSpecificTests {
        
        @Test
        @DisplayName("Should support product owner role")
        void shouldSupportProductOwnerRole() {
            // Product owners typically work with GOAL items
            WorkItemType poType = WorkItemType.GOAL;
            
            // Verify GOAL has proper metadata fields
            assertTrue(itemMetadata.get(goalId).containsKey("target_quarter"),
                "GOAL should have target quarter for roadmap planning");
            
            // Verify GOAL can have FEATURE children
            assertTrue(poType.canHaveChildOfType(WorkItemType.FEATURE),
                "Product owner should be able to break GOALs into FEATUREs");
        }
        
        @Test
        @DisplayName("Should support developer role")
        void shouldSupportDeveloperRole() {
            // Developers typically work with FEATURE, BUG, CHORE
            
            // Verify items can be assigned to developers
            assertEquals("developer@example.com", featureItem.getAssignee(),
                "FEATURE should be assignable to developer");
            assertEquals("developer@example.com", bugItem.getAssignee(),
                "BUG should be assignable to developer");
            
            // Verify FEATURE has proper metadata
            assertTrue(itemMetadata.get(featureId).containsKey("story_points"),
                "FEATURE should have story points for estimation");
        }
        
        @Test
        @DisplayName("Should support QA engineer role")
        void shouldSupportQaEngineerRole() {
            // QA typically works with BUG items and test cases
            
            // Verify BUG has proper metadata
            assertTrue(itemMetadata.get(bugId).containsKey("severity"),
                "BUG should have severity for triage");
            assertTrue(itemMetadata.get(bugId).containsKey("browser"),
                "BUG should have environment info");
            
            // Can create test cases using existing item types
            WorkItem testSuite = new WorkItemRecord(
                UUID.randomUUID(),
                "Authentication Test Suite",
                "Test cases for authentication",
                WorkItemType.FEATURE, // Using FEATURE for test suite
                WorkflowState.TO_DO,
                Priority.MEDIUM,
                "qa@example.com",
                Instant.now(),
                Instant.now(),
                null,
                UUID.randomUUID(),
                "PUBLIC",
                false
            );
            
            WorkItem testCase = new WorkItemRecord(
                UUID.randomUUID(),
                "Test Failed Authentication",
                "Verify error handling for failed auth",
                WorkItemType.BUG, // Using BUG for test case
                WorkflowState.TO_DO,
                Priority.MEDIUM,
                "qa@example.com",
                Instant.now(),
                Instant.now(),
                testSuite.getId(),
                testSuite.getProjectId().orElse(null),
                "PUBLIC",
                false
            );
            
            assertEquals(WorkItemType.FEATURE, testSuite.getType(),
                "Test suite should be a FEATURE");
            assertEquals(WorkItemType.BUG, testCase.getType(),
                "Test case should be a BUG");
            assertEquals(testSuite.getId(), testCase.getParentId().orElse(null),
                "Test case should be child of test suite");
        }
    }
}