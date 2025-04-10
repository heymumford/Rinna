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

import org.rinna.adapter.service.DefaultQueryService;
import org.rinna.repository.ItemRepository;
import org.rinna.repository.MetadataRepository;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.QueryService;

/**
 * Factory for creating QueryService instances.
 */
public final class QueryServiceFactory {
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private QueryServiceFactory() {
        // Utility class should not be instantiated
    }
    
    /**
     * Creates a new QueryService instance.
     *
     * @return the query service
     */
    public static QueryService createQueryService() {
        // Create or get the required dependencies
        ItemService itemService = ItemServiceFactory.createItemService();
        
        // Obtain repositories
        ItemRepository itemRepository = RepositoryFactory.createItemRepository();
        MetadataRepository metadataRepository = RepositoryFactory.createMetadataRepository();
        
        // Create and return a DefaultQueryService
        return new DefaultQueryService(itemService, itemRepository, metadataRepository);
    }
}