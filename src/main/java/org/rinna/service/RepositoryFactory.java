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

import org.rinna.adapter.repository.InMemoryItemRepository;
import org.rinna.adapter.repository.InMemoryMetadataRepository;
import org.rinna.adapter.repository.InMemoryQueueRepository;
import org.rinna.adapter.repository.InMemoryReleaseRepository;
import org.rinna.repository.ItemRepository;
import org.rinna.repository.MetadataRepository;
import org.rinna.repository.QueueRepository;
import org.rinna.repository.ReleaseRepository;

/**
 * Factory for creating repository instances.
 * This centralizes repository creation to ensure consistent use of repositories
 * throughout the application.
 */
public class RepositoryFactory {
    
    // Singleton instances for in-memory repositories
    private static ItemRepository itemRepository;
    private static MetadataRepository metadataRepository;
    private static QueueRepository queueRepository;
    private static ReleaseRepository releaseRepository;
    
    /**
     * Creates or returns an ItemRepository instance.
     *
     * @return the item repository
     */
    public static synchronized ItemRepository createItemRepository() {
        if (itemRepository == null) {
            itemRepository = new InMemoryItemRepository();
        }
        return itemRepository;
    }
    
    /**
     * Creates or returns a MetadataRepository instance.
     *
     * @return the metadata repository
     */
    public static synchronized MetadataRepository createMetadataRepository() {
        if (metadataRepository == null) {
            metadataRepository = new InMemoryMetadataRepository();
        }
        return metadataRepository;
    }
    
    /**
     * Creates or returns a QueueRepository instance.
     *
     * @return the queue repository
     */
    public static synchronized QueueRepository createQueueRepository() {
        if (queueRepository == null) {
            queueRepository = new InMemoryQueueRepository();
        }
        return queueRepository;
    }
    
    /**
     * Creates or returns a ReleaseRepository instance.
     *
     * @return the release repository
     */
    public static synchronized ReleaseRepository createReleaseRepository() {
        if (releaseRepository == null) {
            releaseRepository = new InMemoryReleaseRepository();
        }
        return releaseRepository;
    }
}