/*
 * Component of the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna;

import org.junit.jupiter.api.Test;
import org.rinna.domain.entity.Release;
import org.rinna.domain.entity.WorkItem;
import org.rinna.domain.entity.WorkItemCreateRequest;
import org.rinna.domain.entity.WorkItemType;
import org.rinna.domain.usecase.ItemService;
import org.rinna.domain.usecase.ReleaseService;
import org.rinna.domain.usecase.WorkflowService;

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
    }
    
    @Test
    public void testCustomServices() {
        // Create mock services
        ItemService mockItemService = new MockItemService();
        WorkflowService mockWorkflowService = new MockWorkflowService();
        ReleaseService mockReleaseService = new MockReleaseService();
        
        // Create a Rinna instance with the mock services
        Rinna rinna = new Rinna(mockItemService, mockWorkflowService, mockReleaseService);
        
        // Verify that the services are the ones we provided
        assertSame(mockItemService, rinna.items());
        assertSame(mockWorkflowService, rinna.workflow());
        assertSame(mockReleaseService, rinna.releases());
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
        public WorkItem transition(java.util.UUID itemId, org.rinna.domain.entity.WorkflowState targetState) 
                throws org.rinna.domain.usecase.InvalidTransitionException {
            return null;
        }
        
        @Override
        public boolean canTransition(java.util.UUID itemId, org.rinna.domain.entity.WorkflowState targetState) {
            return false;
        }
        
        @Override
        public java.util.List<org.rinna.domain.entity.WorkflowState> getAvailableTransitions(java.util.UUID itemId) {
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
}