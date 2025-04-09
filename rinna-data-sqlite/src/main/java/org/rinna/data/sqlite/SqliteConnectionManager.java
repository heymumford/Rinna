/*
 * SQLite persistence implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.data.sqlite;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Manages SQLite database connections using HikariCP connection pool.
 * This class is responsible for creating and maintaining the database connection.
 */
public class SqliteConnectionManager implements AutoCloseable {
    private static final Logger logger = LoggerFactory.getLogger(SqliteConnectionManager.class);
    private static final String DEFAULT_DATABASE_NAME = "rinna.db";
    private static final String DEFAULT_DATABASE_PATH = System.getProperty("user.home") + "/.rinna";
    
    private final HikariDataSource dataSource;
    private final String databasePath;
    private boolean initialized = false;
    
    /**
     * Creates a new SqliteConnectionManager with the default database path.
     */
    public SqliteConnectionManager() {
        this(DEFAULT_DATABASE_PATH, DEFAULT_DATABASE_NAME);
    }
    
    /**
     * Creates a new SqliteConnectionManager with a custom database path and name.
     *
     * @param dbPath the path to the database directory
     * @param dbName the database file name
     */
    public SqliteConnectionManager(String dbPath, String dbName) {
        try {
            // Ensure the database directory exists
            Path dirPath = Paths.get(dbPath);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            
            this.databasePath = Paths.get(dbPath, dbName).toString();
            File dbFile = new File(databasePath);
            
            // Configure HikariCP
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl("jdbc:sqlite:" + databasePath);
            config.setMaximumPoolSize(10); // Adjust based on your needs
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000); // 30 seconds
            config.setIdleTimeout(600000); // 10 minutes
            config.setMaxLifetime(1800000); // 30 minutes
            config.setAutoCommit(true);
            config.addDataSourceProperty("foreign_keys", "true");
            
            this.dataSource = new HikariDataSource(config);
            
            boolean dbExists = dbFile.exists() && dbFile.length() > 0;
            if (!dbExists) {
                initialize();
            }
            
            logger.info("SQLite connection manager initialized with database: {}", databasePath);
        } catch (Exception e) {
            logger.error("Error initializing SQLite connection manager", e);
            throw new RuntimeException("Failed to initialize SQLite database", e);
        }
    }
    
    /**
     * Initializes the database schema if it doesn't exist.
     */
    private void initialize() {
        if (initialized) {
            return;
        }
        
        logger.info("Initializing SQLite database schema at: {}", databasePath);
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Enable foreign keys
            stmt.execute("PRAGMA foreign_keys = ON;");
            
            // Create work_items table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS work_items (
                    id TEXT PRIMARY KEY,
                    title TEXT NOT NULL,
                    description TEXT,
                    type TEXT NOT NULL,
                    status TEXT NOT NULL,
                    priority TEXT NOT NULL,
                    assignee TEXT,
                    created_at TEXT NOT NULL,
                    updated_at TEXT NOT NULL,
                    parent_id TEXT,
                    project_id TEXT,
                    visibility TEXT NOT NULL DEFAULT 'PUBLIC',
                    local_only INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY (parent_id) REFERENCES work_items(id) ON DELETE SET NULL
                )
            """);
            
            // Create work_item_metadata table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS work_item_metadata (
                    id TEXT PRIMARY KEY,
                    work_item_id TEXT NOT NULL,
                    key TEXT NOT NULL,
                    value TEXT,
                    created_at TEXT NOT NULL,
                    FOREIGN KEY (work_item_id) REFERENCES work_items(id) ON DELETE CASCADE,
                    UNIQUE(work_item_id, key)
                )
            """);
            
            // Create indexes for faster lookups
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_work_items_type ON work_items(type)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_work_items_status ON work_items(status)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_work_items_assignee ON work_items(assignee)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_work_items_parent_id ON work_items(parent_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_work_items_project_id ON work_items(project_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_metadata_work_item_id ON work_item_metadata(work_item_id)");
            stmt.execute("CREATE INDEX IF NOT EXISTS idx_metadata_key ON work_item_metadata(key)");
            
            initialized = true;
            logger.info("SQLite database schema initialized successfully");
        } catch (SQLException e) {
            logger.error("Error initializing SQLite database schema", e);
            throw new RuntimeException("Failed to initialize SQLite database schema", e);
        }
    }
    
    /**
     * Gets a connection from the connection pool.
     *
     * @return a database connection
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }
    
    /**
     * Closes the connection pool and releases resources.
     */
    @Override
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            logger.info("SQLite connection manager closed");
        }
    }
    
    /**
     * Gets the path to the database file.
     *
     * @return the database file path
     */
    public String getDatabasePath() {
        return databasePath;
    }
}