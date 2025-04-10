/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.service;

import org.rinna.adapter.service.DefaultItemService;
import org.rinna.repository.ItemRepository;
import org.rinna.repository.MetadataRepository;
import org.rinna.usecase.ItemService;

/**
 * Factory for creating ItemService instances.
 */
public final class ItemServiceFactory {
    
    // Singleton instance
    private static ItemService itemService;
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private ItemServiceFactory() {
        // Utility class should not be instantiated
    }
    
    /**
     * Creates or returns an ItemService instance.
     *
     * @return the item service
     */
    public static synchronized ItemService createItemService() {
        if (itemService == null) {
            // Obtain repositories using the RepositoryFactory
            ItemRepository itemRepository = RepositoryFactory.createItemRepository();
            MetadataRepository metadataRepository = RepositoryFactory.createMetadataRepository();
            
            // Create the service
            itemService = new DefaultItemService(itemRepository, metadataRepository);
        }
        return itemService;
    }
}