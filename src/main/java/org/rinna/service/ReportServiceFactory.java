/*
 * Factory class for the Rinna unified work management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.service;

import java.util.Optional;

import org.rinna.adapter.service.DefaultReportService;
import org.rinna.domain.repository.UnifiedWorkItemRepository;
import org.rinna.usecase.CustomCategoryService;
import org.rinna.usecase.DocumentService;
import org.rinna.usecase.ReportService;
import org.rinna.usecase.UnifiedWorkItemService;
import org.rinna.usecase.VocabularyMapService;

/**
 * Factory for creating ReportService instances.
 */
public final class ReportServiceFactory {
    
    private static ReportService instance;
    
    private ReportServiceFactory() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Gets the default instance of ReportService.
     * 
     * @return The default instance
     */
    public static synchronized ReportService getInstance() {
        if (instance == null) {
            UnifiedWorkItemRepository repository = RepositoryFactory.getUnifiedWorkItemRepository();
            UnifiedWorkItemService workItemService = ItemServiceFactory.getUnifiedWorkItemService();
            CustomCategoryService categoryService = CategoryServiceFactory.getCustomCategoryService();
            VocabularyMapService vocabularyMapService = VocabularyMapServiceFactory.getInstance();
            
            // DocumentService is optional
            Optional<DocumentService> documentService = DocumentServiceFactory.getDocumentService();
            
            instance = new DefaultReportService(
                    repository,
                    workItemService,
                    categoryService,
                    vocabularyMapService,
                    documentService);
        }
        
        return instance;
    }
    
    /**
     * Creates a new ReportService instance.
     * 
     * @param repository The repository to use
     * @param workItemService The work item service to use
     * @param categoryService The category service to use
     * @param vocabularyMapService The vocabulary map service to use
     * @param documentService The document service to use (optional)
     * @return A new ReportService instance
     */
    public static ReportService create(
            UnifiedWorkItemRepository repository,
            UnifiedWorkItemService workItemService,
            CustomCategoryService categoryService,
            VocabularyMapService vocabularyMapService,
            Optional<DocumentService> documentService) {
        
        return new DefaultReportService(
                repository,
                workItemService,
                categoryService,
                vocabularyMapService,
                documentService);
    }
}