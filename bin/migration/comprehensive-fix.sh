#!/bin/bash
# Script to fix imports in Java files for the new package structure

set -e

echo "Running comprehensive fix for Java imports..."

# Directory to process
TEST_DIR="/home/emumford/NativeLinuxProjects/Rinna/src/test/java"

# Process individual files manually
echo "Fixing CleanArchitectureTest.java..."
cat > "$TEST_DIR/org/rinna/CleanArchitectureTest.java" << 'EOF'
/*
 * Test for the Rinna Clean Architecture implementation
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna;

import org.junit.jupiter.api.Test;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemCreateRequest;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.usecase.InvalidTransitionException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for verifying that the Clean Architecture implementation works correctly.
 */
public class CleanArchitectureTest {

    @Test
    public void testCreateAndRetrieveWorkItem() {
        // Initialize Rinna with Clean Architecture structure
        Rinna rinna = Rinna.initialize();
        
        // Create a work item
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("Test Task")
                .description("This is a test task")
                .type(WorkItemType.FEATURE)
                .priority(Priority.MEDIUM)
                .assignee("testuser")
                .build();
        
        WorkItem createdItem = rinna.items().create(request);
        
        // Verify the item was created correctly
        assertNotNull(createdItem);
        assertEquals("Test Task", createdItem.getTitle());
        assertEquals("This is a test task", createdItem.getDescription());
        assertEquals(WorkItemType.FEATURE, createdItem.getType());
        assertEquals(Priority.MEDIUM, createdItem.getPriority());
        assertEquals("testuser", createdItem.getAssignee());
        assertEquals(WorkflowState.FOUND, createdItem.getStatus());
        
        // Retrieve the item and verify it's the same
        UUID itemId = createdItem.getId();
        WorkItem retrievedItem = rinna.items().findById(itemId).orElse(null);
        assertNotNull(retrievedItem);
        assertEquals(createdItem.getId(), retrievedItem.getId());
        assertEquals(createdItem.getTitle(), retrievedItem.getTitle());
    }
    
    @Test
    public void testWorkflowTransition() throws InvalidTransitionException {
        // Initialize Rinna with Clean Architecture structure
        Rinna rinna = Rinna.initialize();
        
        // Create a work item
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("Test Task")
                .type(WorkItemType.FEATURE)
                .build();
        
        WorkItem createdItem = rinna.items().create(request);
        UUID itemId = createdItem.getId();
        
        // Initial state should be FOUND
        assertEquals(WorkflowState.FOUND, createdItem.getStatus());
        
        // Valid transition: FOUND -> TRIAGED
        WorkItem triagedItem = rinna.workflow().transition(itemId, WorkflowState.TRIAGED);
        assertEquals(WorkflowState.TRIAGED, triagedItem.getStatus());
        
        // Valid transition: TRIAGED -> TO_DO
        WorkItem toDoItem = rinna.workflow().transition(itemId, WorkflowState.TO_DO);
        assertEquals(WorkflowState.TO_DO, toDoItem.getStatus());
        
        // Valid transition: TO_DO -> IN_PROGRESS
        WorkItem inProgressItem = rinna.workflow().transition(itemId, WorkflowState.IN_PROGRESS);
        assertEquals(WorkflowState.IN_PROGRESS, inProgressItem.getStatus());
        
        // Invalid transition: IN_PROGRESS -> TRIAGED
        assertThrows(InvalidTransitionException.class, () -> {
            rinna.workflow().transition(itemId, WorkflowState.TRIAGED);
        });
    }
    
    @Test
    public void testDependencyRule() {
        // This test verifies that the dependency rule of Clean Architecture is followed
        // Domain entities and use cases should not depend on adapters or framework
        
        // Create Rinna instance
        Rinna rinna = Rinna.initialize();
        
        // Create a work item using the use case
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("Test Task")
                .type(WorkItemType.FEATURE)
                .build();
        
        WorkItem createdItem = rinna.items().create(request);
        
        // Verify the returned item is from the domain, not an adapter type
        assertTrue(createdItem.getClass().getName().contains("domain"));
        
        // Verify we can interact with the domain object
        assertEquals(WorkflowState.FOUND, createdItem.getStatus());
        assertEquals(WorkItemType.FEATURE, createdItem.getType());
    }
}
EOF

echo "Fixing RinnaTest.java..."
cat > "$TEST_DIR/org/rinna/RinnaTest.java" << 'EOF'
/*
 * Component of the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna;

import org.junit.jupiter.api.Test;
import org.rinna.domain.Priority;
import org.rinna.domain.Release;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemCreateRequest;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkQueue;
import org.rinna.repository.MetadataRepository;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.QueueService;
import org.rinna.usecase.ReleaseService;
import org.rinna.usecase.WorkflowService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Rinna class.
 */
public class RinnaTest {

    @Test
    public void testInitialize() {
        // Initialize a new Rinna instance
        Rinna rinna = Rinna.initialize();
        
        // Verify that the instance is not null
        assertNotNull(rinna);
        
        // Verify that the services are available
        assertNotNull(rinna.items());
        assertNotNull(rinna.workflow());
        assertNotNull(rinna.releases());
        assertNotNull(rinna.queue());
        assertNotNull(rinna.getMetadataRepository());
    }
    
    @Test
    public void testCustomServices() {
        // Create mock services
        ItemService mockItemService = new MockItemService();
        WorkflowService mockWorkflowService = new MockWorkflowService();
        ReleaseService mockReleaseService = new MockReleaseService();
        QueueService mockQueueService = new MockQueueService();
        MetadataRepository mockMetadataRepository = new MockMetadataRepository();
        
        // Create a Rinna instance with the mock services
        Rinna rinna = new Rinna(mockItemService, mockWorkflowService, 
                mockReleaseService, mockQueueService, mockMetadataRepository);
        
        // Verify that the services are the ones we provided
        assertSame(mockItemService, rinna.items());
        assertSame(mockWorkflowService, rinna.workflow());
        assertSame(mockReleaseService, rinna.releases());
        assertSame(mockQueueService, rinna.queue());
        assertSame(mockMetadataRepository, rinna.getMetadataRepository());
    }
    
    /**
     * Mock implementation of ItemService for testing.
     */
    private static class MockItemService implements ItemService {
        @Override
        public WorkItem create(WorkItemCreateRequest request) {
            return null;
        }
        
        @Override
        public java.util.Optional<WorkItem> findById(java.util.UUID id) {
            return java.util.Optional.empty();
        }
        
        @Override
        public java.util.List<WorkItem> findAll() {
            return java.util.List.of();
        }
        
        @Override
        public java.util.List<WorkItem> findByType(String type) {
            return java.util.List.of();
        }
        
        @Override
        public java.util.List<WorkItem> findByStatus(String status) {
            return java.util.List.of();
        }
        
        @Override
        public java.util.List<WorkItem> findByAssignee(String assignee) {
            return java.util.List.of();
        }
        
        @Override
        public WorkItem updateAssignee(java.util.UUID id, String assignee) {
            return null;
        }
        
        @Override
        public void deleteById(java.util.UUID id) {
            // No-op
        }
    }
    
    /**
     * Mock implementation of WorkflowService for testing.
     */
    private static class MockWorkflowService implements WorkflowService {
        @Override
        public WorkItem transition(java.util.UUID itemId, org.rinna.domain.WorkflowState targetState) 
                throws org.rinna.usecase.InvalidTransitionException {
            return null;
        }
        
        @Override
        public boolean canTransition(java.util.UUID itemId, org.rinna.domain.WorkflowState targetState) {
            return false;
        }
        
        @Override
        public java.util.List<org.rinna.domain.WorkflowState> getAvailableTransitions(java.util.UUID itemId) {
            return java.util.List.of();
        }
    }
    
    /**
     * Mock implementation of ReleaseService for testing.
     */
    private static class MockReleaseService implements ReleaseService {
        @Override
        public Release createRelease(String version, String description) {
            return null;
        }
        
        @Override
        public Release createNextMinorVersion(java.util.UUID releaseId, String description) {
            return null;
        }
        
        @Override
        public Release createNextPatchVersion(java.util.UUID releaseId, String description) {
            return null;
        }
        
        @Override
        public Release createNextMajorVersion(java.util.UUID releaseId, String description) {
            return null;
        }
        
        @Override
        public void addWorkItem(java.util.UUID releaseId, java.util.UUID workItemId) {
            // No-op
        }
        
        @Override
        public void removeWorkItem(java.util.UUID releaseId, java.util.UUID workItemId) {
            // No-op
        }
        
        @Override
        public boolean containsWorkItem(java.util.UUID releaseId, java.util.UUID workItemId) {
            return false;
        }
        
        @Override
        public java.util.List<WorkItem> getWorkItems(java.util.UUID releaseId) {
            return java.util.List.of();
        }
        
        @Override
        public java.util.Optional<Release> findById(java.util.UUID id) {
            return java.util.Optional.empty();
        }
        
        @Override
        public java.util.Optional<Release> findByVersion(String version) {
            return java.util.Optional.empty();
        }
        
        @Override
        public java.util.List<Release> findAll() {
            return java.util.List.of();
        }
    }
    
    /**
     * Mock implementation of QueueService for testing.
     */
    private static class MockQueueService implements QueueService {
        @Override
        public WorkQueue createQueue(String name, String description) {
            return null;
        }
        
        @Override
        public void addWorkItemToQueue(java.util.UUID queueId, java.util.UUID workItemId) {
            // No-op
        }
        
        @Override
        public boolean removeWorkItemFromQueue(java.util.UUID queueId, java.util.UUID workItemId) {
            return false;
        }
        
        @Override
        public java.util.Optional<WorkItem> getNextWorkItem(java.util.UUID queueId) {
            return java.util.Optional.empty();
        }
        
        @Override
        public java.util.List<WorkItem> getQueueItems(java.util.UUID queueId) {
            return java.util.List.of();
        }
        
        @Override
        public java.util.List<WorkItem> getQueueItemsByType(java.util.UUID queueId, WorkItemType type) {
            return java.util.List.of();
        }
        
        @Override
        public java.util.List<WorkItem> getQueueItemsByState(java.util.UUID queueId, org.rinna.domain.WorkflowState state) {
            return java.util.List.of();
        }
        
        @Override
        public java.util.List<WorkItem> getQueueItemsByPriority(java.util.UUID queueId, Priority priority) {
            return java.util.List.of();
        }
        
        @Override
        public java.util.List<WorkItem> getQueueItemsByAssignee(java.util.UUID queueId, String assignee) {
            return java.util.List.of();
        }
        
        @Override
        public void reprioritizeQueue(java.util.UUID queueId) {
            // No-op
        }
        
        @Override
        public void reprioritizeQueueWithWeights(java.util.UUID queueId, java.util.Map<String, Integer> weights) {
            // No-op
        }
        
        @Override
        public void reprioritizeQueueByCapacity(java.util.UUID queueId, int teamCapacity) {
            // No-op
        }
        
        @Override
        public void activateQueue(java.util.UUID queueId) {
            // No-op
        }
        
        @Override
        public void deactivateQueue(java.util.UUID queueId) {
            // No-op
        }
        
        @Override
        public java.util.Optional<WorkQueue> findById(java.util.UUID id) {
            return java.util.Optional.empty();
        }
        
        @Override
        public java.util.Optional<WorkQueue> findByName(String name) {
            return java.util.Optional.empty();
        }
        
        @Override
        public java.util.List<WorkQueue> findAllQueues() {
            return java.util.List.of();
        }
        
        @Override
        public java.util.List<WorkQueue> findActiveQueues() {
            return java.util.List.of();
        }
        
        @Override
        public WorkItem submitProductionIncident(String title, String description) {
            return null;
        }
        
        @Override
        public WorkItem submitFeatureRequest(String title, String description, Priority priority) {
            return null;
        }
        
        @Override
        public WorkItem submitTechnicalTask(String title, String description, Priority priority) {
            return null;
        }
        
        @Override
        public WorkItem submitChildWorkItem(String title, WorkItemType type, java.util.UUID parentId, 
                                           String description, Priority priority) {
            return null;
        }
        
        @Override
        public WorkQueue createDefaultQueue() {
            return null;
        }
        
        @Override
        public WorkQueue getDefaultQueue() {
            return null;
        }
        
        @Override
        public boolean isUrgent(java.util.UUID workItemId) {
            return false;
        }
        
        @Override
        public void setUrgent(java.util.UUID workItemId, boolean urgent) {
            // No-op
        }
        
        @Override
        public java.util.List<WorkItem> findUrgentItems() {
            return java.util.List.of();
        }
    }
    
    /**
     * Mock implementation of MetadataRepository for testing.
     */
    private static class MockMetadataRepository implements MetadataRepository {
        @Override
        public org.rinna.domain.WorkItemMetadata save(org.rinna.domain.WorkItemMetadata metadata) {
            return metadata;
        }
        
        @Override
        public java.util.Optional<org.rinna.domain.WorkItemMetadata> findById(java.util.UUID id) {
            return java.util.Optional.empty();
        }
        
        @Override
        public java.util.List<org.rinna.domain.WorkItemMetadata> findByWorkItemId(java.util.UUID workItemId) {
            return java.util.List.of();
        }
        
        @Override
        public java.util.Optional<org.rinna.domain.WorkItemMetadata> findByWorkItemIdAndKey(
                java.util.UUID workItemId, String key) {
            return java.util.Optional.empty();
        }
        
        @Override
        public java.util.Map<String, String> getMetadataMap(java.util.UUID workItemId) {
            return java.util.Map.of();
        }
        
        @Override
        public boolean deleteById(java.util.UUID id) {
            return false;
        }
        
        @Override
        public int deleteByWorkItemId(java.util.UUID workItemId) {
            return 0;
        }
        
        @Override
        public boolean deleteByWorkItemIdAndKey(java.util.UUID workItemId, String key) {
            return false;
        }
        
        @Override
        public java.util.List<org.rinna.domain.WorkItemMetadata> findAll() {
            return java.util.List.of();
        }
    }
}
EOF

echo "Fixing TddTest.java..."
cat > "$TEST_DIR/org/rinna/TddTest.java" << 'EOF'
/*
 * Tests for Rinna using Test-Driven Development
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemCreateRequest;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.ReleaseService;
import org.rinna.usecase.WorkflowService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for Rinna using Test-Driven Development.
 */
public class TddTest {
    
    private Rinna rinna;
    private ItemService itemService;
    private WorkflowService workflowService; 
    private ReleaseService releaseService;
    
    @BeforeEach
    public void setup() {
        rinna = Rinna.initialize();
        itemService = rinna.items();
        workflowService = rinna.workflow();
        releaseService = rinna.releases();
    }
    
    @Nested
    @DisplayName("Work Item Creation")
    class WorkItemCreationTests {
        
        @Test
        @DisplayName("Should create a work item with the specified properties")
        public void testCreateWorkItem() {
            // Given
            WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("Test Bug")
                .description("This is a test bug")
                .type(WorkItemType.BUG)
                .priority(Priority.HIGH)
                .assignee("bob")
                .build();
            
            // When
            WorkItem workItem = itemService.create(request);
            
            // Then
            assertNotNull(workItem);
            assertNotNull(workItem.getId());
            assertEquals("Test Bug", workItem.getTitle());
            assertEquals("This is a test bug", workItem.getDescription());
            assertEquals(WorkItemType.BUG, workItem.getType());
            assertEquals(Priority.HIGH, workItem.getPriority());
            assertEquals("bob", workItem.getAssignee());
            assertEquals(WorkflowState.FOUND, workItem.getStatus());
        }
        
        @Test
        @DisplayName("Should create a default work item with minimal properties")
        public void testCreateMinimalWorkItem() {
            // Given
            WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("Minimal Item")
                .type(WorkItemType.TASK)
                .build();
            
            // When
            WorkItem workItem = itemService.create(request);
            
            // Then
            assertNotNull(workItem);
            assertEquals("Minimal Item", workItem.getTitle());
            assertEquals(WorkItemType.TASK, workItem.getType());
            assertEquals(Priority.MEDIUM, workItem.getPriority());  // Default priority
            assertNull(workItem.getAssignee());  // No assignee by default
            assertEquals(WorkflowState.FOUND, workItem.getStatus());  // Default status
        }
    }
    
    @Nested
    @DisplayName("Workflow Management")
    class WorkflowManagementTests {
        
        @Test
        @DisplayName("Should transition a work item through its workflow states")
        public void testWorkflowTransition() throws org.rinna.usecase.InvalidTransitionException {
            // Given
            WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title("Workflow Test")
                .type(WorkItemType.FEATURE)
                .build();
            
            WorkItem workItem = itemService.create(request);
            assertEquals(WorkflowState.FOUND, workItem.getStatus());
            
            // When/Then
            // Transition FOUND -> TRIAGED
            WorkItem triagedItem = workflowService.transition(workItem.getId(), WorkflowState.TRIAGED);
            assertEquals(WorkflowState.TRIAGED, triagedItem.getStatus());
            
            // Transition TRIAGED -> TO_DO
            WorkItem todoItem = workflowService.transition(workItem.getId(), WorkflowState.TO_DO);
            assertEquals(WorkflowState.TO_DO, todoItem.getStatus());
        }
    }
}
EOF

echo "Fixing TestHelper.java..."
cat > "$TEST_DIR/org/rinna/TestHelper.java" << 'EOF'
/*
 * Helper methods for Rinna tests.
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna;

import org.junit.jupiter.api.Assumptions;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.ReleaseService;
import org.rinna.usecase.WorkflowService;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Helper methods for Rinna tests.
 */
public class TestHelper {
    
    /**
     * Creates a Rinna instance for testing.
     * 
     * @return a Rinna instance
     */
    public static Rinna createTestInstance() {
        return Rinna.initialize();
    }
    
    /**
     * Gets the ItemService from a Rinna instance.
     * 
     * @return an ItemService
     */
    public static ItemService getItemService() {
        Rinna rinna = createTestInstance();
        return rinna.items();
    }
    
    /**
     * Gets the WorkflowService from a Rinna instance.
     * 
     * @return a WorkflowService
     */
    public static WorkflowService getWorkflowService() {
        Rinna rinna = createTestInstance();
        return rinna.workflow();
    }
    
    /**
     * Creates a test work item.
     * 
     * @param title the title of the work item
     * @param type the type of the work item
     * @return the created work item
     */
    public static org.rinna.domain.WorkItem createTestWorkItem(String title, org.rinna.domain.WorkItemType type) {
        ItemService itemService = getItemService();
        org.rinna.domain.WorkItemCreateRequest request = new org.rinna.domain.WorkItemCreateRequest.Builder()
                .title(title)
                .type(type)
                .description("Test description for " + title)
                .build();
        return itemService.create(request);
    }
    
    /**
     * Creates a test release.
     * 
     * @param version the version of the release
     * @return the created release
     */
    public static org.rinna.domain.Release createTestRelease(String version) {
        ReleaseService releaseService = getWorkflowService().getClass().getClassLoader()
                .getDependency(ReleaseService.class);
        Assumptions.assumeTrue(releaseService != null, "ReleaseService should be available");
        return releaseService.createRelease(version, "Test release " + version + " created at " + 
                                            LocalDateTime.now());
    }
    
    /**
     * Generates a random UUID.
     * 
     * @return a random UUID
     */
    public static UUID randomUuid() {
        return UUID.randomUUID();
    }
}
EOF

# Make the script executable
chmod +x "$0"

echo "Comprehensive fix completed!"