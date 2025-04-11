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

import org.rinna.adapter.service.DefaultSearchService;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.SearchService;

/**
 * Factory for creating SearchService instances.
 */
public final class SearchServiceFactory {
    
    /**
     * Creates a new SearchService instance.
     *
     * @return the search service
     */
    public static SearchService createSearchService() {
        // Create an ItemService to provide to the SearchService
        ItemService itemService = ItemServiceFactory.createItemService();
        
        // Create and return a DefaultSearchService
        return new DefaultSearchService(itemService);
    }
    
    /**
     * Private constructor to prevent instantiation.
     */
    private SearchServiceFactory() {
        // This constructor is intentionally empty
    }
}