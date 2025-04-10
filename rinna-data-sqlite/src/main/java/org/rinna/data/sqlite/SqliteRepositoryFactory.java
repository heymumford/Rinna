/*
 * SQLite persistence implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.data.sqlite;

import org.rinna.domain.repository.ItemRepository;
import org.rinna.domain.repository.MetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Factory for creating SQLite repository instances.
 * This class provides a simple way to create and manage SQLite repositories.
 */
public class SqliteRepositoryFactory implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SqliteRepositoryFactory.class);
    
    private final SqliteConnectionManager connectionManager;
    private final SqliteItemRepository itemRepository;
    private final SqliteMetadataRepository metadataRepository;
    
    /**
     * Creates a new SqliteRepositoryFactory with default database location.
     */
    public SqliteRepositoryFactory() {
        this(new SqliteConnectionManager());
    }
    
    /**
     * Creates a new SqliteRepositoryFactory with the specified connection manager.
     *
     * @param connectionManager the SQLite connection manager
     */
    public SqliteRepositoryFactory(SqliteConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.metadataRepository = new SqliteMetadataRepository(connectionManager);
        this.itemRepository = new SqliteItemRepository(connectionManager, metadataRepository);
        
        logger.info("SQLite repository factory initialized with database: {}", 
                connectionManager.getDatabasePath());
    }
    
    /**
     * Gets the ItemRepository instance.
     *
     * @return the item repository
     */
    public ItemRepository getItemRepository() {
        return itemRepository;
    }
    
    /**
     * Gets the MetadataRepository instance.
     *
     * @return the metadata repository
     */
    public MetadataRepository getMetadataRepository() {
        return metadataRepository;
    }
    
    /**
     * Gets the SQLite connection manager.
     *
     * @return the connection manager
     */
    public SqliteConnectionManager getConnectionManager() {
        return connectionManager;
    }
    
    /**
     * Closes all resources used by this factory.
     */
    @Override
    public void close() {
        if (connectionManager != null) {
            connectionManager.close();
            logger.info("SQLite repository factory closed");
        }
    }
}