/*
 * Factory class for the Rinna unified work management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.service;

import org.rinna.adapter.service.DefaultCustomCategoryService;
import org.rinna.domain.repository.UnifiedWorkItemRepository;
import org.rinna.usecase.CustomCategoryService;

/**
 * Factory for creating CustomCategoryService instances.
 */
public final class CategoryServiceFactory {
    
    private static CustomCategoryService instance;
    
    private CategoryServiceFactory() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Gets the default instance of CustomCategoryService.
     * 
     * @return The default instance
     */
    public static synchronized CustomCategoryService getCustomCategoryService() {
        if (instance == null) {
            UnifiedWorkItemRepository repository = RepositoryFactory.getUnifiedWorkItemRepository();
            instance = new DefaultCustomCategoryService(repository);
        }
        
        return instance;
    }
    
    /**
     * Creates a new CustomCategoryService instance.
     * 
     * @param repository The repository to use
     * @return A new CustomCategoryService instance
     */
    public static CustomCategoryService create(UnifiedWorkItemRepository repository) {
        return new DefaultCustomCategoryService(repository);
    }
}