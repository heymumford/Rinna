/*
 * Configuration for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.config;

import org.rinna.adapter.persistence.InMemoryItemRepository;
import org.rinna.adapter.service.DefaultItemService;
import org.rinna.adapter.service.DefaultWorkflowService;
import org.rinna.domain.repository.ItemRepository;
import org.rinna.domain.usecase.ItemService;
import org.rinna.domain.usecase.WorkflowService;

/**
 * Configuration class for wiring together Rinna components.
 * This class follows the Dependency Injection pattern to provide properly
 * initialized instances of all components.
 */
public class RinnaConfig {
    private ItemRepository itemRepository;
    private ItemService itemService;
    private WorkflowService workflowService;
    
    /**
     * Initializes the configuration with default components.
     */
    public RinnaConfig() {
        this.itemRepository = new InMemoryItemRepository();
        this.itemService = new DefaultItemService(itemRepository);
        this.workflowService = new DefaultWorkflowService(itemRepository);
    }
    
    /**
     * Returns the item repository.
     * 
     * @return the item repository
     */
    public ItemRepository getItemRepository() {
        return itemRepository;
    }
    
    /**
     * Returns the item service.
     * 
     * @return the item service
     */
    public ItemService getItemService() {
        return itemService;
    }
    
    /**
     * Returns the workflow service.
     * 
     * @return the workflow service
     */
    public WorkflowService getWorkflowService() {
        return workflowService;
    }
    
    /**
     * Sets a custom item repository.
     * This will automatically update the services to use the new repository.
     * 
     * @param itemRepository the item repository
     * @return this configuration
     */
    public RinnaConfig setItemRepository(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
        this.itemService = new DefaultItemService(itemRepository);
        this.workflowService = new DefaultWorkflowService(itemRepository);
        return this;
    }
    
    /**
     * Sets a custom item service.
     * 
     * @param itemService the item service
     * @return this configuration
     */
    public RinnaConfig setItemService(ItemService itemService) {
        this.itemService = itemService;
        return this;
    }
    
    /**
     * Sets a custom workflow service.
     * 
     * @param workflowService the workflow service
     * @return this configuration
     */
    public RinnaConfig setWorkflowService(WorkflowService workflowService) {
        this.workflowService = workflowService;
        return this;
    }
}