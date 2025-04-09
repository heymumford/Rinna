/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.component.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.rinna.adapter.repository.InMemoryItemRepository;
import org.rinna.adapter.repository.InMemoryMetadataRepository;
import org.rinna.adapter.service.DefaultItemService;
import org.rinna.adapter.service.DefaultQueryService;
import org.rinna.component.base.ComponentTest;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.repository.ItemRepository;
import org.rinna.repository.MetadataRepository;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.QueryService;
import org.rinna.usecase.QueryService.QueryFilter;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Component tests for the QueryService with actual implementations.
 */
public class QueryServiceComponentTest extends ComponentTest {
    
    private ItemRepository itemRepository;
    private MetadataRepository metadataRepository;
    private ItemService itemService;
    private QueryService queryService;
    
    private UUID id1;
    private UUID id2;
    private UUID id3;
    
    private LocalDateTime now = LocalDateTime.now();
    private LocalDateTime yesterday = now.minusDays(1);
    private LocalDateTime lastWeek = now.minusDays(7);
    
    @BeforeEach
    public void setup() {
        // Create real repositories
        itemRepository = new InMemoryItemRepository();
        metadataRepository = new InMemoryMetadataRepository();
        
        // Create real services
        itemService = new DefaultItemService(itemRepository, metadataRepository);
        queryService = new DefaultQueryService(itemService, itemRepository, metadataRepository);
        
        // Add test data
        // 1. First create work items
        WorkItem item1 = createBug();
        WorkItem item2 = createTask();
        WorkItem item3 = createStory();
        
        // 2. Save metadata
        addMetadata();
    }
    
    private WorkItem createBug() {
        WorkItem item = new WorkItem(
            null, // ID will be generated
            "BUG-123: Critical frontend issue",
            "This is a bug in the frontend that causes crashes",
            WorkItemType.BUG,
            Priority.HIGH,
            WorkflowState.TO_DO,
            "alice",
            "frontend",
            now
        );
        
        // Save and get the generated ID
        id1 = itemService.createWorkItem(item);
        return itemService.getWorkItem(id1);
    }
    
    private WorkItem createTask() {
        WorkItem item = new WorkItem(
            null, // ID will be generated
            "TASK-456: Implement new feature",
            "Implement the new login screen",
            WorkItemType.TASK,
            Priority.MEDIUM,
            WorkflowState.IN_PROGRESS,
            "bob",
            "backend",
            yesterday
        );
        
        // Save and get the generated ID
        id2 = itemService.createWorkItem(item);
        return itemService.getWorkItem(id2);
    }
    
    private WorkItem createStory() {
        WorkItem item = new WorkItem(
            null, // ID will be generated
            "STORY-789: User registration flow",
            "As a user, I want to register for an account",
            WorkItemType.STORY,
            Priority.LOW,
            WorkflowState.DONE,
            "alice",
            "frontend",
            lastWeek
        );
        
        // Save and get the generated ID
        id3 = itemService.createWorkItem(item);
        return itemService.getWorkItem(id3);
    }
    
    private void addMetadata() {
        // Add metadata for bug item
        Map<String, String> metadata1 = new HashMap<>();
        metadata1.put("reporter", "john");
        metadata1.put("tags", "bug,critical,frontend");
        metadata1.put("last_updated", now.toString());
        metadataRepository.saveMetadata(id1, metadata1);
        
        // Add metadata for task item
        Map<String, String> metadata2 = new HashMap<>();
        metadata2.put("reporter", "susan");
        metadata2.put("tags", "task,feature,backend");
        metadata2.put("last_updated", yesterday.toString());
        metadata2.put("linked_items", id1.toString());
        metadataRepository.saveMetadata(id2, metadata2);
        
        // Add metadata for story item
        Map<String, String> metadata3 = new HashMap<>();
        metadata3.put("reporter", "john");
        metadata3.put("tags", "story,feature,frontend");
        metadata3.put("last_updated", lastWeek.toString());
        metadataRepository.saveMetadata(id3, metadata3);
    }
    
    @Test
    public void testBasicQuery() {
        // Verify that all items were properly created
        List<WorkItem> allItems = queryService.queryWorkItems(QueryFilter.create());
        assertEquals(3, allItems.size());
    }
    
    @Test
    public void testQueryByType() {
        // Query for BUG type items
        QueryFilter filter = QueryFilter.create()
                .ofType(WorkItemType.BUG);
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(1, results.size());
        assertEquals(WorkItemType.BUG, results.get(0).type());
    }
    
    @Test
    public void testQueryByPriorityAndAssignee() {
        // Query for alice's items with HIGH priority
        QueryFilter filter = QueryFilter.create()
                .withPriority(Priority.HIGH)
                .assignedTo("alice");
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(1, results.size());
        assertEquals(Priority.HIGH, results.get(0).priority());
        assertEquals("alice", results.get(0).assignee());
    }
    
    @Test
    public void testQueryByTextPattern() {
        // Query for items with "frontend" in description
        QueryFilter filter = QueryFilter.create()
                .withText("frontend")
                .inFields(Arrays.asList("description"));
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(1, results.size());
        assertTrue(results.get(0).description().contains("frontend"));
    }
    
    @Test
    public void testQueryByReporter() {
        // Query for items reported by john
        QueryFilter filter = QueryFilter.create()
                .reportedBy("john");
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(2, results.size());
    }
    
    @Test
    public void testQueryByTags() {
        // Query for items with critical tag
        QueryFilter filter = QueryFilter.create()
                .withTags(Arrays.asList("critical"));
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(1, results.size());
    }
    
    @Test
    public void testQueryWithSorting() {
        // Query all items sorted by priority descending
        QueryFilter filter = QueryFilter.create()
                .sortBy("priority")
                .ascending(false);
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(3, results.size());
        assertEquals(Priority.HIGH, results.get(0).priority());
        assertEquals(Priority.MEDIUM, results.get(1).priority());
        assertEquals(Priority.LOW, results.get(2).priority());
    }
    
    @Test
    public void testQueryWithPagination() {
        // Query with pagination
        QueryFilter filter = QueryFilter.create()
                .sortBy("created")
                .ascending(true)
                .limit(2)
                .offset(0);
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(2, results.size());
        
        // Test second page
        filter = QueryFilter.create()
                .sortBy("created")
                .ascending(true)
                .limit(2)
                .offset(2);
        
        results = queryService.queryWorkItems(filter);
        
        assertEquals(1, results.size());
    }
    
    @Test
    public void testCountQuery() {
        // Count frontend project items
        QueryFilter filter = QueryFilter.create()
                .inProject("frontend");
        
        int count = queryService.countWorkItems(filter);
        
        assertEquals(2, count);
    }
    
    @Test
    public void testComplexQuery() {
        // Complex query with multiple criteria
        QueryFilter filter = QueryFilter.create()
                .inProject("frontend")
                .assignedTo("alice")
                .withText("user")
                .withTags(Arrays.asList("story"))
                .sortBy("priority")
                .ascending(true);
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(1, results.size());
        assertEquals(WorkItemType.STORY, results.get(0).type());
    }
}