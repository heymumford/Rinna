/*
 * Entry point for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna;

import org.rinna.config.RinnaConfig;
import org.rinna.domain.usecase.ItemService;
import org.rinna.domain.usecase.WorkflowService;

/**
 * Main entry point for the Rinna system.
 * This class provides access to the core services of the system.
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
     * Initializes a new Rinna instance with default services.
     *
     * @return a new Rinna instance
     */
    public static Rinna initialize() {
        RinnaConfig config = new RinnaConfig();
        return new Rinna(config.getItemService(), config.getWorkflowService());
    }
}