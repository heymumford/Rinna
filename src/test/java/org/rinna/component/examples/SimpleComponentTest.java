package org.rinna.component.examples;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.rinna.adapter.repository.InMemoryItemRepository;
import org.rinna.adapter.service.DefaultItemService;

import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.repository.ItemRepository;
import org.rinna.usecase.ItemService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Example of a component test using the new test framework.
 * This test demonstrates testing multiple classes working together.
 */
@DisplayName("Item Service with Repository Component Tests")
@Tag("component")
public class SimpleComponentTest  {
    
    private ItemService itemService;
    private ItemRepository itemRepository;
    
    @BeforeEach
    void setup() {
        // For component tests, we use real implementations of classes in the same module
        itemRepository = new InMemoryItemRepository();
        itemService = new DefaultItemService(itemRepository);
        
        // Set up test data
        setupComponentContext();
    }
    
    @Test
    @DisplayName("Creating a work item should store it in the repository")
    void creatingWorkItemShouldStoreItInRepository() {
        // Create a work item through the service
        WorkItemCreateRequest request = new WorkItemCreateRequest(
            "Test Item",
            "This is a test item",
            WorkItemType.TASK,
            null
        );
        
        WorkItem createdItem = itemService.createWorkItem(request);
        
        // Verify the item was created and stored correctly
        assertNotNull(createdItem);
        assertNotNull(createdItem.getId());
        assertEquals("Test Item", createdItem.getTitle());
        assertEquals(WorkItemType.TASK, createdItem.getType());
        assertEquals(WorkflowState.BACKLOG, createdItem.getState());
        
        // Verify the item can be retrieved from the repository
        WorkItem retrievedItem = itemService.getWorkItem(createdItem.getId());
        assertNotNull(retrievedItem);
        assertEquals(createdItem.getId(), retrievedItem.getId());
    }
    
    @Test
    @DisplayName("Finding work items by state should return correct items")
    void findingWorkItemsByStateShouldReturnCorrectItems() {
        // Create several work items in different states
        itemService.createWorkItem(new WorkItemCreateRequest("Item 1", "Description 1", WorkItemType.TASK, null));
        
        WorkItem item2 = itemService.createWorkItem(
            new WorkItemCreateRequest("Item 2", "Description 2", WorkItemType.BUG, null));
        itemService.updateWorkItemState(item2.getId(), WorkflowState.IN_PROGRESS);
        
        WorkItem item3 = itemService.createWorkItem(
            new WorkItemCreateRequest("Item 3", "Description 3", WorkItemType.FEATURE, null));
        itemService.updateWorkItemState(item3.getId(), WorkflowState.DONE);
        
        // Find items by state
        List<WorkItem> backlogItems = itemService.findWorkItemsByState(WorkflowState.BACKLOG);
        List<WorkItem> inProgressItems = itemService.findWorkItemsByState(WorkflowState.IN_PROGRESS);
        List<WorkItem> doneItems = itemService.findWorkItemsByState(WorkflowState.DONE);
        
        // Verify results
        assertEquals(1, backlogItems.size());
        assertEquals("Item 1", backlogItems.get(0).getTitle());
        
        assertEquals(1, inProgressItems.size());
        assertEquals("Item 2", inProgressItems.get(0).getTitle());
        
        assertEquals(1, doneItems.size());
        assertEquals("Item 3", doneItems.get(0).getTitle());
    }
}
