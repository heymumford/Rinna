/*
 * Factory class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.service;

import org.rinna.adapter.service.DefaultTemplateService;
import org.rinna.usecase.TemplateService;
import org.rinna.usecase.UnifiedWorkItemService;

/**
 * Factory for creating TemplateService instances.
 * This class follows the factory pattern to provide a standard way to create
 * TemplateService implementations.
 */
public final class TemplateServiceFactory {
    
    private static TemplateService instance;
    
    /**
     * Gets the default TemplateService instance.
     * If an instance doesn't exist, it creates one with the default implementation.
     * 
     * @param workItemService The UnifiedWorkItemService to use for creating work items
     * @return The TemplateService instance
     */
    public static synchronized TemplateService getDefaultService(UnifiedWorkItemService workItemService) {
        if (instance == null) {
            instance = new DefaultTemplateService(workItemService);
        }
        return instance;
    }
    
    /**
     * Creates a new TemplateService instance with the default implementation.
     * This method always creates a new instance, which can be useful for tests or
     * when multiple independent instances are needed.
     * 
     * @param workItemService The UnifiedWorkItemService to use for creating work items
     * @return A new TemplateService instance
     */
    public static TemplateService createService(UnifiedWorkItemService workItemService) {
        return new DefaultTemplateService(workItemService);
    }
    
    private TemplateServiceFactory() {
        // Private constructor to prevent instantiation
    }
}