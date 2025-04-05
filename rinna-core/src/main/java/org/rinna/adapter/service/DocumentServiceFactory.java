/*
 * Service factory for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import org.rinna.domain.entity.DocumentConfig;
import org.rinna.domain.usecase.DocumentService;

import java.util.logging.Logger;

/**
 * Factory for creating document service instances.
 * This factory creates the appropriate document service based on configuration and available libraries.
 */
public class DocumentServiceFactory {
    private static final Logger LOGGER = Logger.getLogger(DocumentServiceFactory.class.getName());
    
    /**
     * Creates a document service with the given configuration.
     * If Docmosis is available and configured, a DocmosisDocumentService is returned.
     * Otherwise, a DefaultDocumentService is returned.
     * 
     * @param config the document configuration
     * @return a document service instance
     */
    public static DocumentService createDocumentService(DocumentConfig config) {
        // Try to create Docmosis service first if preferred
        if (config.isPreferDocmosis()) {
            DocmosisDocumentService docmosisService = new DocmosisDocumentService(config);
            if (docmosisService.isAvailable()) {
                LOGGER.info("Using Docmosis document service");
                return docmosisService;
            }
            LOGGER.info("Docmosis not available, falling back to default document service");
        }
        
        // Fall back to default service
        return new DefaultDocumentService(config);
    }
}