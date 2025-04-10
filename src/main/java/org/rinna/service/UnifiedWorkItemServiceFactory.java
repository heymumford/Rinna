/*
 * Factory for UnifiedWorkItemService for the Rinna unified work management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.service;

import org.rinna.adapter.repository.InMemoryUnifiedWorkItemRepository;
import org.rinna.adapter.service.DefaultUnifiedWorkItemService;
import org.rinna.domain.repository.UnifiedWorkItemRepository;
import org.rinna.usecase.UnifiedWorkItemService;

/**
 * Factory for creating instances of UnifiedWorkItemService.
 * This class provides a centralized way to create and configure services.
 */
public class UnifiedWorkItemServiceFactory {
    private static UnifiedWorkItemService instance;
    
    /**
     * Gets a singleton instance of UnifiedWorkItemService.
     * 
     * @return the UnifiedWorkItemService instance
     */
    public static synchronized UnifiedWorkItemService getInstance() {
        if (instance == null) {
            // Create the repository
            UnifiedWorkItemRepository repository = new InMemoryUnifiedWorkItemRepository();
            
            // Create the service with the repository
            instance = new DefaultUnifiedWorkItemService(repository);
        }
        
        return instance;
    }
    
    /**
     * Creates a new instance of UnifiedWorkItemService with the specified repository.
     * 
     * @param repository the repository to use
     * @return a new UnifiedWorkItemService instance
     */
    public static UnifiedWorkItemService createInstance(UnifiedWorkItemRepository repository) {
        return new DefaultUnifiedWorkItemService(repository);
    }
    
    /**
     * Private constructor to prevent instantiation.
     */
    private UnifiedWorkItemServiceFactory() {
        // This constructor is intentionally empty
    }
}