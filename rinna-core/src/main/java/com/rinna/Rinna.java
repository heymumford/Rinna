package com.rinna;

import com.rinna.service.ItemService;
import com.rinna.service.WorkflowService;
import com.rinna.service.impl.DefaultWorkflowService;
import com.rinna.service.impl.InMemoryItemService;

/**
 * Main entry point for the Rinna system.
 */
public class Rinna {
    private final ItemService itemService;
    private final WorkflowService workflowService;
    
    /**
     * Constructs a new Rinna instance with the specified services.
     *
     * @param itemService the item service to use
     * @param workflowService the workflow service to use
     */
    public Rinna(ItemService itemService, WorkflowService workflowService) {
        this.itemService = itemService;
        this.workflowService = workflowService;
    }
    
    /**
     * Returns the item service.
     *
     * @return the item service
     */
    public ItemService items() {
        return itemService;
    }
    
    /**
     * Returns the workflow service.
     *
     * @return the workflow service
     */
    public WorkflowService workflow() {
        return workflowService;
    }
    
    /**
     * Initializes a new Rinna instance with default in-memory services.
     *
     * @return a new Rinna instance
     */
    public static Rinna initialize() {
        InMemoryItemService itemService = new InMemoryItemService();
        DefaultWorkflowService workflowService = new DefaultWorkflowService(itemService);
        return new Rinna(itemService, workflowService);
    }
}