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

import org.rinna.adapter.service.DefaultFeatureFlagService;
import org.rinna.repository.FeatureFlagRepository;
import org.rinna.usecase.FeatureFlagService;

/**
 * Factory for creating feature flag service instances.
 * This centralizes service creation to ensure consistent use of services
 * throughout the application.
 */
public final class FeatureFlagServiceFactory {
    
    // Singleton instance of the feature flag service
    private static FeatureFlagService featureFlagService;
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private FeatureFlagServiceFactory() {
        // Utility class should not be instantiated
    }
    
    /**
     * Creates or returns a FeatureFlagService instance.
     * 
     * @return the feature flag service
     */
    public static synchronized FeatureFlagService getFeatureFlagService() {
        if (featureFlagService == null) {
            // Get the repository from the repository factory
            FeatureFlagRepository repository = RepositoryFactory.getFeatureFlagRepository();
            
            // Create the service with the repository
            featureFlagService = new DefaultFeatureFlagService(repository);
        }
        
        return featureFlagService;
    }
    
    /**
     * Sets the default instance of FeatureFlagService.
     * 
     * @param service The service to set as default
     */
    public static synchronized void setFeatureFlagService(FeatureFlagService service) {
        featureFlagService = service;
    }
    
    /**
     * Creates a new FeatureFlagService instance with the specified repository.
     * This is primarily used for testing.
     * 
     * @param repository the feature flag repository
     * @return a new feature flag service
     */
    public static FeatureFlagService createFeatureFlagService(FeatureFlagRepository repository) {
        return new DefaultFeatureFlagService(repository);
    }
    
    /**
     * Creates a new FeatureFlagService instance with an in-memory repository.
     * This is primarily used for testing.
     * 
     * @return a new feature flag service with in-memory repository
     */
    public static FeatureFlagService createInMemoryFeatureFlagService() {
        return new DefaultFeatureFlagService(RepositoryFactory.createInMemoryFeatureFlagRepository());
    }
}