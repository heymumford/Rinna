/*
 * Test for critical path service in the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.unit.service;

import static org.junit.jupiter.api.Assertions.*;

import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemRecord;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.domain.service.impl.DefaultCriticalPathService;

@DisplayName("Critical Path Service Tests")
class CriticalPathServiceTest {

    private CriticalPathService criticalPathService;
    private ItemService mockItemService;
    
    private UUID item1Id = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private UUID item2Id = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private UUID item3Id = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private UUID item4Id = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private UUID item5Id = UUID.fromString("00000000-0000-0000-0000-000000000005");
    
    private WorkItem item1, item2, item3, item4, item5;
    
    @BeforeEach
    void setUp() {
        // Create sample work items
        Instant now = Instant.now();
        
        // Create work items with dependencies
        item1 = new WorkItemRecord(
            item1Id, "Setup database", "Initial database setup", 
            WorkItemType.TASK, WorkflowState.IN_PROGRESS, Priority.HIGH,
            "alice", now, now, null, null, "PUBLIC", false
        );
        
        item2 = new WorkItemRecord(
            item2Id, "Create API endpoints", "Implement REST API", 
            WorkItemType.FEATURE, WorkflowState.FOUND, Priority.MEDIUM,
            "bob", now, now, null, null, "PUBLIC", false
        );
        
        item3 = new WorkItemRecord(
            item3Id, "Implement authentication", "Add user login", 
            WorkItemType.FEATURE, WorkflowState.FOUND, Priority.HIGH,
            null, now, now, null, null, "PUBLIC", false
        );
        
        item4 = new WorkItemRecord(
            item4Id, "Create frontend UI", "Implement React components", 
            WorkItemType.FEATURE, WorkflowState.FOUND, Priority.MEDIUM,
            "carol", now, now, null, null, "PUBLIC", false
        );
        
        item5 = new WorkItemRecord(
            item5Id, "Deploy to production", "Final deployment steps", 
            WorkItemType.TASK, WorkflowState.FOUND, Priority.CRITICAL,
            null, now, now, null, null, "PUBLIC", false
        );
        
        // Mock ItemService
        mockItemService = new TestItemService(Arrays.asList(item1, item2, item3, item4, item5));
        
        // Create the critical path service with the mock ItemService
        criticalPathService = new DefaultCriticalPathService(mockItemService);
        
        // Set up dependencies for testing
        ((DefaultCriticalPathService) criticalPathService).addDependency(item2Id, item1Id); // API depends on DB
        ((DefaultCriticalPathService) criticalPathService).addDependency(item3Id, item1Id); // Auth depends on DB
        ((DefaultCriticalPathService) criticalPathService).addDependency(item4Id, item2Id); // UI depends on API
        ((DefaultCriticalPathService) criticalPathService).addDependency(item4Id, item3Id); // UI depends on Auth
        ((DefaultCriticalPathService) criticalPathService).addDependency(item5Id, item4Id); // Deploy depends on UI
    }
    
    @Test
    @DisplayName("Should find items that block others")
    void findBlockingItems() {
        List<WorkItem> blockers = criticalPathService.findBlockingItems();
        assertEquals(2, blockers.size(), "Should find 2 blocking items");
        assertTrue(blockers.contains(item1), "Database setup should be a blocker");
        assertTrue(blockers.contains(item3), "Authentication should be a blocker (due to priority)");
    }
    
    @Test
    @DisplayName("Should find critical path for the project")
    void findCriticalPath() {
        List<WorkItem> criticalPath = criticalPathService.findCriticalPath();
        assertEquals(4, criticalPath.size(), "Critical path should have 4 items");
        assertEquals(item1, criticalPath.get(0), "Path should start with database setup");
        assertEquals(item3, criticalPath.get(1), "Authentication should be next (higher priority)");
        assertEquals(item4, criticalPath.get(2), "UI should be after Auth");
        assertEquals(item5, criticalPath.get(3), "Deployment should be last");
    }
    
    @Test
    @DisplayName("Should find items by dependency")
    void findItemsDependingOn() {
        List<WorkItem> dependents = criticalPathService.findItemsDependingOn(item1Id);
        assertEquals(2, dependents.size(), "Two items depend on database setup");
        assertTrue(dependents.contains(item2), "API should depend on database");
        assertTrue(dependents.contains(item3), "Auth should depend on database");
    }
    
    @Test
    @DisplayName("Should find empty path for project with no items")
    void findCriticalPathForEmptyProject() {
        // Create a new service with empty test data
        CriticalPathService emptyService = new DefaultCriticalPathService(new TestItemService(Collections.emptyList()));
        List<WorkItem> criticalPath = emptyService.findCriticalPath();
        assertTrue(criticalPath.isEmpty(), "Critical path should be empty for project with no items");
    }
    
    // Simple test implementation of ItemService
    private static class TestItemService implements ItemService {
        private final List<WorkItem> items;
        
        TestItemService(List<WorkItem> items) {
            this.items = items;
        }
        
        @Override
        public WorkItem create(org.rinna.domain.model.WorkItemCreateRequest request) {
            throw new UnsupportedOperationException("Not implemented for test");
        }
        
        @Override
        public java.util.Optional<WorkItem> findById(UUID id) {
            return items.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst();
        }
        
        @Override
        public List<WorkItem> findAll() {
            return items;
        }
        
        @Override
        public List<WorkItem> findByType(String type) {
            return items.stream()
                .filter(item -> item.getType().toString().equals(type))
                .toList();
        }
        
        @Override
        public List<WorkItem> findByStatus(String status) {
            return items.stream()
                .filter(item -> item.getStatus().toString().equals(status))
                .toList();
        }
        
        @Override
        public List<WorkItem> findByAssignee(String assignee) {
            return items.stream()
                .filter(item -> {
                    String itemAssignee = item.getAssignee();
                    return itemAssignee != null && itemAssignee.equals(assignee);
                })
                .toList();
        }
        
        @Override
        public WorkItem updateAssignee(UUID id, String assignee) {
            throw new UnsupportedOperationException("Not implemented for test");
        }
        
        @Override
        public void deleteById(UUID id) {
            throw new UnsupportedOperationException("Not implemented for test");
        }
    }
}
