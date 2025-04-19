/*
 * Component of the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna;

import org.junit.jupiter.api.Test;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.Release;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkQueue;
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
        public WorkItem transition(java.util.UUID itemId, org.rinna.domain.model.WorkflowState targetState) 
                throws org.rinna.domain.service.InvalidTransitionException {
            return null;
        }
        
        @Override
        public boolean canTransition(java.util.UUID itemId, org.rinna.domain.model.WorkflowState targetState) {
            return false;
        }
        
        @Override
        public java.util.List<org.rinna.domain.model.WorkflowState> getAvailableTransitions(java.util.UUID itemId) {
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
        public java.util.List<WorkItem> getQueueItemsByState(java.util.UUID queueId, org.rinna.domain.model.WorkflowState state) {
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
        public org.rinna.domain.model.WorkItemMetadata save(org.rinna.domain.model.WorkItemMetadata metadata) {
            return metadata;
        }
        
        @Override
        public java.util.Optional<org.rinna.domain.model.WorkItemMetadata> findById(java.util.UUID id) {
            return java.util.Optional.empty();
        }
        
        @Override
        public java.util.List<org.rinna.domain.model.WorkItemMetadata> findByWorkItemId(java.util.UUID workItemId) {
            return java.util.List.of();
        }
        
        @Override
        public java.util.Optional<org.rinna.domain.model.WorkItemMetadata> findByWorkItemIdAndKey(
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
        public java.util.List<org.rinna.domain.model.WorkItemMetadata> findAll() {
            return java.util.List.of();
        }
    }
}