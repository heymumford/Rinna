/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.unit.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.adapter.service.DefaultQueryService;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.repository.ItemRepository;
import org.rinna.repository.MetadataRepository;
import org.rinna.unit.base.UnitTest;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.QueryService;
import org.rinna.usecase.QueryService.QueryFilter;

/**
 * Unit tests for the DefaultQueryService class.
 */
public class DefaultQueryServiceTest extends UnitTest {
    
    @Mock
    private ItemService itemService;
    
    @Mock
    private ItemRepository itemRepository;
    
    @Mock
    private MetadataRepository metadataRepository;
    
    private QueryService queryService;
    
    private WorkItem item1;
    private WorkItem item2;
    private WorkItem item3;
    
    private UUID id1 = UUID.randomUUID();
    private UUID id2 = UUID.randomUUID();
    private UUID id3 = UUID.randomUUID();
    
    private LocalDateTime now = LocalDateTime.now();
    private LocalDateTime yesterday = now.minusDays(1);
    private LocalDateTime lastWeek = now.minusDays(7);
    
    @BeforeEach
    public void setup() {
        MockitoAnnotations.openMocks(this);
        
        queryService = new DefaultQueryService(itemService, itemRepository, metadataRepository);
        
        // Create test items
        item1 = new WorkItem(
            id1,
            "BUG-123: Critical frontend issue",
            "This is a bug in the frontend that causes crashes",
            WorkItemType.BUG,
            Priority.HIGH,
            WorkflowState.TO_DO,
            "alice",
            "frontend",
            now
        );
        
        item2 = new WorkItem(
            id2,
            "TASK-456: Implement new feature",
            "Implement the new login screen",
            WorkItemType.TASK,
            Priority.MEDIUM,
            WorkflowState.IN_PROGRESS,
            "bob",
            "backend",
            yesterday
        );
        
        item3 = new WorkItem(
            id3,
            "STORY-789: User registration flow",
            "As a user, I want to register for an account",
            WorkItemType.STORY,
            Priority.LOW,
            WorkflowState.DONE,
            "alice",
            "frontend",
            lastWeek
        );
        
        // Mock item service to return our test items
        when(itemService.getAllWorkItems()).thenReturn(Arrays.asList(item1, item2, item3));
        
        // Mock metadata repository
        Map<String, String> metadata1 = new HashMap<>();
        metadata1.put("reporter", "john");
        metadata1.put("tags", "bug,critical,frontend");
        metadata1.put("last_updated", now.toString());
        
        Map<String, String> metadata2 = new HashMap<>();
        metadata2.put("reporter", "susan");
        metadata2.put("tags", "task,feature,backend");
        metadata2.put("last_updated", yesterday.toString());
        metadata2.put("linked_items", id1.toString());
        
        Map<String, String> metadata3 = new HashMap<>();
        metadata3.put("reporter", "john");
        metadata3.put("tags", "story,feature,frontend");
        metadata3.put("last_updated", lastWeek.toString());
        
        when(metadataRepository.getMetadata(id1)).thenReturn(metadata1);
        when(metadataRepository.getMetadata(id2)).thenReturn(metadata2);
        when(metadataRepository.getMetadata(id3)).thenReturn(metadata3);
    }
    
    @Test
    public void testQueryWithTextPattern() {
        // Query for items with "frontend" in title or description
        QueryFilter filter = QueryFilter.create()
                .withText("frontend")
                .inFields(Arrays.asList("title", "description"));
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(2, results.size());
        assertTrue(results.contains(item1));
        assertTrue(results.contains(item3));
    }
    
    @Test
    public void testQueryWithType() {
        // Query for BUG type items
        QueryFilter filter = QueryFilter.create()
                .ofType(WorkItemType.BUG);
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(1, results.size());
        assertEquals(item1, results.get(0));
    }
    
    @Test
    public void testQueryWithPriority() {
        // Query for HIGH priority items
        QueryFilter filter = QueryFilter.create()
                .withPriority(Priority.HIGH);
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(1, results.size());
        assertEquals(item1, results.get(0));
    }
    
    @Test
    public void testQueryWithState() {
        // Query for IN_PROGRESS items
        QueryFilter filter = QueryFilter.create()
                .inState(WorkflowState.IN_PROGRESS);
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(1, results.size());
        assertEquals(item2, results.get(0));
    }
    
    @Test
    public void testQueryWithAssignee() {
        // Query for items assigned to alice
        QueryFilter filter = QueryFilter.create()
                .assignedTo("alice");
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(2, results.size());
        assertTrue(results.contains(item1));
        assertTrue(results.contains(item3));
    }
    
    @Test
    public void testQueryWithReporter() {
        // Query for items reported by john
        QueryFilter filter = QueryFilter.create()
                .reportedBy("john");
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(2, results.size());
        assertTrue(results.contains(item1));
        assertTrue(results.contains(item3));
    }
    
    @Test
    public void testQueryWithProject() {
        // Query for backend project items
        QueryFilter filter = QueryFilter.create()
                .inProject("backend");
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(1, results.size());
        assertEquals(item2, results.get(0));
    }
    
    @Test
    public void testQueryWithCreatedAfter() {
        // Query for items created after yesterday
        QueryFilter filter = QueryFilter.create()
                .createdAfter(yesterday.plusHours(1));
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(1, results.size());
        assertEquals(item1, results.get(0));
    }
    
    @Test
    public void testQueryWithMultipleCriteria() {
        // Query for HIGH priority frontend BUG items
        QueryFilter filter = QueryFilter.create()
                .withPriority(Priority.HIGH)
                .inProject("frontend")
                .ofType(WorkItemType.BUG);
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(1, results.size());
        assertEquals(item1, results.get(0));
    }
    
    @Test
    public void testQueryWithTags() {
        // Query for items with critical tag
        QueryFilter filter = QueryFilter.create()
                .withTags(Arrays.asList("critical"));
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(1, results.size());
        assertEquals(item1, results.get(0));
    }
    
    @Test
    public void testQueryWithSorting() {
        // Query all items sorted by creation date ascending
        QueryFilter filter = QueryFilter.create()
                .sortBy("created")
                .ascending(true);
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(3, results.size());
        assertEquals(item3, results.get(0)); // Oldest first
        assertEquals(item2, results.get(1));
        assertEquals(item1, results.get(2)); // Newest last
    }
    
    @Test
    public void testQueryWithPagination() {
        // Query all items with limit and offset
        QueryFilter filter = QueryFilter.create()
                .sortBy("created") // Sort by created date descending (default)
                .limit(1)
                .offset(1);
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertEquals(1, results.size());
        assertEquals(item2, results.get(0)); // Second newest item
    }
    
    @Test
    public void testCountWorkItems() {
        // Count HIGH priority items
        QueryFilter filter = QueryFilter.create()
                .withPriority(Priority.HIGH);
        
        int count = queryService.countWorkItems(filter);
        
        assertEquals(1, count);
    }
    
    @Test
    public void testQueryWithNoResults() {
        // Query for non-existent priority
        QueryFilter filter = QueryFilter.create()
                .withPriority(Priority.HIGH)
                .inState(WorkflowState.DONE);
        
        List<WorkItem> results = queryService.queryWorkItems(filter);
        
        assertTrue(results.isEmpty());
    }
}