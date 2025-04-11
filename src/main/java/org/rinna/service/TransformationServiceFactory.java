/*
 * Service factory for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.service;

import org.rinna.adapter.service.DefaultTransformationInstanceService;
import org.rinna.adapter.service.DefaultTransformationTemplateService;
import org.rinna.domain.repository.TransformationInstanceRepository;
import org.rinna.domain.repository.TransformationTemplateRepository;
import org.rinna.usecase.TransformationInstanceService;
import org.rinna.usecase.TransformationTemplateService;
import org.rinna.usecase.UnifiedWorkItemService;

/**
 * Factory for creating transformation service instances.
 */
public final class TransformationServiceFactory {
    
    private static TransformationTemplateService transformationTemplateService;
    private static TransformationInstanceService transformationInstanceService;
    
    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private TransformationServiceFactory() {
        // Utility class should not be instantiated
    }
    
    /**
     * Gets the default instance of TransformationTemplateService.
     * 
     * @return The default instance
     */
    public static synchronized TransformationTemplateService getTransformationTemplateService() {
        if (transformationTemplateService == null) {
            TransformationTemplateRepository templateRepository = RepositoryFactory.getTransformationTemplateRepository();
            TransformationInstanceRepository instanceRepository = RepositoryFactory.getTransformationInstanceRepository();
            
            transformationTemplateService = new DefaultTransformationTemplateService(
                    templateRepository, instanceRepository);
        }
        
        return transformationTemplateService;
    }
    
    /**
     * Sets the default instance of TransformationTemplateService.
     * 
     * @param service The service to set as default
     */
    public static synchronized void setTransformationTemplateService(TransformationTemplateService service) {
        transformationTemplateService = service;
    }
    
    /**
     * Gets the default instance of TransformationInstanceService.
     * 
     * @return The default instance
     */
    public static synchronized TransformationInstanceService getTransformationInstanceService() {
        if (transformationInstanceService == null) {
            TransformationTemplateRepository templateRepository = RepositoryFactory.getTransformationTemplateRepository();
            TransformationInstanceRepository instanceRepository = RepositoryFactory.getTransformationInstanceRepository();
            UnifiedWorkItemService workItemService = UnifiedWorkItemServiceFactory.getUnifiedWorkItemService();
            
            transformationInstanceService = new DefaultTransformationInstanceService(
                    instanceRepository, templateRepository, workItemService);
        }
        
        return transformationInstanceService;
    }
    
    /**
     * Sets the default instance of TransformationInstanceService.
     * 
     * @param service The service to set as default
     */
    public static synchronized void setTransformationInstanceService(TransformationInstanceService service) {
        transformationInstanceService = service;
    }
}