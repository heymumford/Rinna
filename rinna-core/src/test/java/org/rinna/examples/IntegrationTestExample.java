package org.rinna.examples;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.rinna.adapter.persistence.InMemoryItemRepository;
import org.rinna.adapter.service.DefaultItemService;
import org.rinna.domain.entity.WorkItem;
import org.rinna.domain.entity.WorkItemCreateRequest;
import org.rinna.domain.entity.Priority;
import org.rinna.domain.entity.WorkItemType;
import org.rinna.domain.repository.ItemRepository;
import org.rinna.domain.usecase.ItemService;
import org.rinna.domain.usecase.WorkflowService;
import org.rinna.adapter.service.DefaultWorkflowService;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example of an integration test for Rinna.
 * Integration tests verify interactions between different modules,
 * typically using real implementations (not mocks).
 */
@Tag("integration")
public class IntegrationTestExample {

    private ItemRepository itemRepository;
    private ItemService itemService;
    private WorkflowService workflowService;

    @BeforeEach
    void setUp() {
        // Use real implementations
        itemRepository = new InMemoryItemRepository();
        itemService = new DefaultItemService(itemRepository);
        workflowService = new DefaultWorkflowService(itemRepository);
    }

    @Test
    @DisplayName("End-to-end work item creation and transition")
    void endToEndWorkItemCreationAndTransition() {
        // Create a work item
        WorkItemCreateRequest request = new WorkItemCreateRequest(
            "Integration test item",
            "Testing the complete workflow",
            Priority.HIGH,
            WorkItemType.FEATURE
        );
        
        // Save via item service
        WorkItem createdItem = itemService.createWorkItem(request);
        assertNotNull(createdItem.getId(), "Created item should have an ID");
        
        // Transition to next state via workflow service
        workflowService.transitionToNextState(createdItem.getId());
        
        // Retrieve the updated item
        WorkItem updatedItem = itemService.getWorkItem(createdItem.getId());
        
        // Verify the state transition worked
        assertNotEquals(createdItem.getState(), updatedItem.getState(), 
                      "State should have changed");
    }

    @Test
    @DisplayName("Work item lifecycle through multiple transitions")
    void workItemLifecycleThroughMultipleTransitions() {
        // Create a work item
        WorkItem item = itemService.createWorkItem(new WorkItemCreateRequest(
            "Lifecycle test", "Testing full lifecycle", Priority.MEDIUM, WorkItemType.FEATURE
        ));
        
        // Record the initial state
        var initialState = item.getState();
        
        // Transition through several states
        workflowService.transitionToNextState(item.getId()); // First transition
        workflowService.transitionToNextState(item.getId()); // Second transition
        
        // Retrieve the final state
        WorkItem finalItem = itemService.getWorkItem(item.getId());
        
        // Verify multiple transitions worked
        assertNotEquals(initialState, finalItem.getState(), 
                      "State should have changed after multiple transitions");
    }
}
