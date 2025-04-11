/*
 * Factory class for the Rinna unified work management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.service;

import java.util.Optional;

import org.rinna.usecase.DocumentService;

/**
 * Factory for creating DocumentService instances.
 */
public final class DocumentServiceFactory {
    
    private static Optional<DocumentService> instance = Optional.empty();
    
    private DocumentServiceFactory() {
        // Private constructor to prevent instantiation
    }
    
    /**
     * Gets the default instance of DocumentService, if available.
     * 
     * @return The default instance, or empty if not available
     */
    public static synchronized Optional<DocumentService> getDocumentService() {
        return instance;
    }
    
    /**
     * Sets the default instance of DocumentService.
     * 
     * @param documentService The document service to set as default
     */
    public static synchronized void setDocumentService(DocumentService documentService) {
        instance = Optional.ofNullable(documentService);
    }
}