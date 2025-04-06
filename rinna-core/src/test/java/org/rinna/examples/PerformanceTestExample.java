package org.rinna.examples;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.rinna.persistence.InMemoryItemRepository;
import org.rinna.service.DefaultItemService;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItemType;
import org.rinna.repository.ItemRepository;
import org.rinna.domain.service.ItemService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example of a performance test for Rinna.
 * Performance tests verify the system meets performance requirements
 * by measuring execution time and resource usage.
 */
@Tag("performance")
public class PerformanceTestExample {

    private ItemRepository itemRepository;
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemRepository = new InMemoryItemRepository();
        itemService = new DefaultItemService(itemRepository);
    }

    @Test
    @DisplayName("Bulk creation performance test")
    void bulkCreationPerformanceTest() {
        // Number of items to create
        final int ITEM_COUNT = 1000;
        final long MAX_EXECUTION_TIME_MS = 1000; // 1 second max
        
        // Prepare requests
        List<WorkItemCreateRequest> requests = new ArrayList<>();
        for (int i = 0; i < ITEM_COUNT; i++) {
            requests.add(new WorkItemCreateRequest(
                "Performance Item " + i,
                "Description for item " + i,
                Priority.MEDIUM,
                WorkItemType.FEATURE
            ));
        }
        
        // Measure execution time
        long startTime = System.nanoTime();
        
        // Create all items
        List<WorkItem> createdItems = new ArrayList<>();
        for (WorkItemCreateRequest request : requests) {
            createdItems.add(itemService.createWorkItem(request));
        }
        
        long endTime = System.nanoTime();
        long durationMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        // Verify performance criteria
        assertTrue(durationMs < MAX_EXECUTION_TIME_MS, 
                 "Bulk creation should take less than " + MAX_EXECUTION_TIME_MS + "ms");
        
        // Log performance results
        System.out.println(String.format("Created %d items in %d ms, average %d ms per item", 
                                        ITEM_COUNT, durationMs, durationMs / ITEM_COUNT));
        
        // Verify all items were created
        assertEquals(ITEM_COUNT, createdItems.size(), "All items should be created");
    }

    @Test
    @DisplayName("Query performance test")
    void queryPerformanceTest() {
        // Create a large dataset first
        final int ITEM_COUNT = 5000;
        final long MAX_QUERY_TIME_MS = 500; // 500ms max
        
        // Create test data
        for (int i = 0; i < ITEM_COUNT; i++) {
            itemService.createWorkItem(new WorkItemCreateRequest(
                "Item " + i,
                "Description",
                i % 3 == 0 ? Priority.HIGH : (i % 3 == 1 ? Priority.MEDIUM : Priority.LOW),
                i % 2 == 0 ? WorkItemType.BUG : WorkItemType.FEATURE
            ));
        }
        
        // Measure query time for all items
        long startTime = System.nanoTime();
        List<WorkItem> allItems = itemService.getAllWorkItems();
        long endTime = System.nanoTime();
        long queryAllTimeMs = TimeUnit.NANOSECONDS.toMillis(endTime - startTime);
        
        // Verify performance criteria
        assertTrue(queryAllTimeMs < MAX_QUERY_TIME_MS, 
                 "Query for all items should take less than " + MAX_QUERY_TIME_MS + "ms");
        
        // Log performance results
        System.out.println(String.format("Retrieved %d items in %d ms", 
                                        allItems.size(), queryAllTimeMs));
    }
}
