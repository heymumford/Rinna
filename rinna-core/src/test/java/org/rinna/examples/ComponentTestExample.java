package org.rinna.examples;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rinna.adapter.service.DefaultItemService;
import org.rinna.domain.entity.WorkItem;
import org.rinna.domain.entity.WorkItemCreateRequest;
import org.rinna.domain.entity.Priority;
import org.rinna.domain.entity.WorkItemType;
import org.rinna.domain.repository.ItemRepository;
import org.rinna.domain.usecase.ItemService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example of a component test for Rinna.
 * Component tests focus on testing related components working together.
 * External dependencies are still mocked.
 */
@Tag("component")
public class ComponentTestExample {

    private ItemRepository mockRepository;
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        // Mock the repository (external dependency)
        mockRepository = Mockito.mock(ItemRepository.class);
        
        // Use the real service implementation
        itemService = new DefaultItemService(mockRepository);
    }

    @Test
    @DisplayName("Create work item should save to repository")
    void createWorkItemShouldSaveToRepository() {
        // Setup
        WorkItemCreateRequest request = new WorkItemCreateRequest(
            "Component test item",
            "Testing the item service with repo",
            Priority.HIGH,
            WorkItemType.FEATURE
        );
        
        // Mock the repository behavior
        WorkItem mockItem = Mockito.mock(WorkItem.class);
        Mockito.when(mockItem.getId()).thenReturn("WI-123");
        Mockito.when(mockRepository.save(Mockito.any())).thenReturn(mockItem);
        
        // Execute
        WorkItem result = itemService.createWorkItem(request);
        
        // Verify
        assertNotNull(result, "Result should not be null");
        assertEquals("WI-123", result.getId(), "Item should have an ID assigned");
        
        // Verify repository was called with appropriate data
        Mockito.verify(mockRepository).save(Mockito.any());
    }

    @Test
    @DisplayName("Get work items should retrieve from repository")
    void getWorkItemsShouldRetrieveFromRepository() {
        // Setup - mock the repository to return empty list
        Mockito.when(mockRepository.findAll()).thenReturn(java.util.Collections.emptyList());
        
        // Execute
        java.util.List<WorkItem> results = itemService.getAllWorkItems();
        
        // Verify
        assertNotNull(results, "Results should not be null");
        assertTrue(results.isEmpty(), "No items should be returned");
        
        // Verify repository was called
        Mockito.verify(mockRepository).findAll();
    }
}
