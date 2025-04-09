/*
 * SQLite persistence implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.data.sqlite;

import org.rinna.domain.model.WorkItemMetadata;
import org.rinna.domain.repository.MetadataRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * SQLite implementation of the MetadataRepository interface.
 * Provides persistence of WorkItemMetadata entities in an SQLite database.
 */
public class SqliteMetadataRepository implements MetadataRepository {
    private static final Logger logger = LoggerFactory.getLogger(SqliteMetadataRepository.class);
    
    private final SqliteConnectionManager connectionManager;
    
    /**
     * Creates a new SqliteMetadataRepository with a connection manager.
     *
     * @param connectionManager the SQLite connection manager
     */
    public SqliteMetadataRepository(SqliteConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }
    
    @Override
    public WorkItemMetadata save(WorkItemMetadata metadata) {
        logger.debug("Saving metadata: {} for work item: {}", 
                metadata.getKey(), metadata.getWorkItemId());
        
        String sql = """
            INSERT OR REPLACE INTO work_item_metadata 
            (id, work_item_id, key, value, created_at)
            VALUES (?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, metadata.getId().toString());
            stmt.setString(2, metadata.getWorkItemId().toString());
            stmt.setString(3, metadata.getKey());
            stmt.setString(4, metadata.getValue());
            stmt.setTimestamp(5, Timestamp.from(metadata.getCreatedAt()));
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                logger.warn("Failed to save metadata: {} for work item: {}", 
                        metadata.getKey(), metadata.getWorkItemId());
                throw new RuntimeException("Failed to save metadata: " + metadata.getId());
            }
            
            logger.debug("Metadata saved successfully: {} for work item: {}", 
                    metadata.getKey(), metadata.getWorkItemId());
            return metadata;
        } catch (SQLException e) {
            logger.error("Error saving metadata: {} for work item: {}", 
                    metadata.getKey(), metadata.getWorkItemId(), e);
            throw new RuntimeException("Error saving metadata: " + metadata.getId(), e);
        }
    }
    
    /**
     * Helper method to save metadata directly with key-value.
     *
     * @param workItemId the work item ID
     * @param key the metadata key
     * @param value the metadata value
     * @return the saved metadata
     */
    public WorkItemMetadata saveMetadata(UUID workItemId, String key, String value) {
        WorkItemMetadata metadata = new WorkItemMetadata(UUID.randomUUID(), workItemId, key, value, Instant.now());
        return save(metadata);
    }
    
    @Override
    public Optional<WorkItemMetadata> findById(UUID id) {
        logger.debug("Finding metadata by ID: {}", id);
        
        String sql = """
            SELECT id, work_item_id, key, value, created_at
            FROM work_item_metadata
            WHERE id = ?
        """;
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    WorkItemMetadata metadata = mapResultSetToMetadata(rs);
                    logger.debug("Found metadata: {}", id);
                    return Optional.of(metadata);
                } else {
                    logger.debug("Metadata not found: {}", id);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding metadata by ID: {}", id, e);
            throw new RuntimeException("Error finding metadata by ID: " + id, e);
        }
    }
    
    @Override
    public List<WorkItemMetadata> findByWorkItemId(UUID workItemId) {
        logger.debug("Finding metadata for work item: {}", workItemId);
        
        String sql = """
            SELECT id, work_item_id, key, value, created_at
            FROM work_item_metadata
            WHERE work_item_id = ?
            ORDER BY key
        """;
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, workItemId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<WorkItemMetadata> metadataList = new ArrayList<>();
                while (rs.next()) {
                    metadataList.add(mapResultSetToMetadata(rs));
                }
                
                logger.debug("Found {} metadata items for work item: {}", 
                        metadataList.size(), workItemId);
                return metadataList;
            }
        } catch (SQLException e) {
            logger.error("Error finding metadata for work item: {}", workItemId, e);
            throw new RuntimeException("Error finding metadata for work item: " + workItemId, e);
        }
    }
    
    @Override
    public Optional<WorkItemMetadata> findByWorkItemIdAndKey(UUID workItemId, String key) {
        logger.debug("Finding metadata for work item: {} and key: {}", workItemId, key);
        
        String sql = """
            SELECT id, work_item_id, key, value, created_at
            FROM work_item_metadata
            WHERE work_item_id = ? AND key = ?
        """;
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, workItemId.toString());
            stmt.setString(2, key);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    WorkItemMetadata metadata = mapResultSetToMetadata(rs);
                    logger.debug("Found metadata for work item: {} and key: {}", workItemId, key);
                    return Optional.of(metadata);
                } else {
                    logger.debug("Metadata not found for work item: {} and key: {}", workItemId, key);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding metadata for work item: {} and key: {}", workItemId, key, e);
            throw new RuntimeException(
                    "Error finding metadata for work item: " + workItemId + " and key: " + key, e);
        }
    }
    
    @Override
    public Map<String, String> getMetadataMap(UUID workItemId) {
        logger.debug("Getting metadata map for work item: {}", workItemId);
        
        String sql = """
            SELECT key, value
            FROM work_item_metadata
            WHERE work_item_id = ?
        """;
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, workItemId.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                Map<String, String> metadataMap = new HashMap<>();
                while (rs.next()) {
                    metadataMap.put(rs.getString("key"), rs.getString("value"));
                }
                
                logger.debug("Found {} metadata entries for work item: {}", 
                        metadataMap.size(), workItemId);
                return metadataMap;
            }
        } catch (SQLException e) {
            logger.error("Error getting metadata map for work item: {}", workItemId, e);
            throw new RuntimeException("Error getting metadata map for work item: " + workItemId, e);
        }
    }
    
    @Override
    public boolean deleteById(UUID id) {
        logger.debug("Deleting metadata by ID: {}", id);
        
        String sql = "DELETE FROM work_item_metadata WHERE id = ?";
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id.toString());
            
            int rowsAffected = stmt.executeUpdate();
            boolean deleted = rowsAffected > 0;
            
            logger.debug("Deleted metadata: {} (success: {})", id, deleted);
            return deleted;
        } catch (SQLException e) {
            logger.error("Error deleting metadata: {}", id, e);
            throw new RuntimeException("Error deleting metadata: " + id, e);
        }
    }
    
    @Override
    public int deleteByWorkItemId(UUID workItemId) {
        logger.debug("Deleting all metadata for work item: {}", workItemId);
        
        String sql = "DELETE FROM work_item_metadata WHERE work_item_id = ?";
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, workItemId.toString());
            
            int rowsAffected = stmt.executeUpdate();
            
            logger.debug("Deleted {} metadata items for work item: {}", rowsAffected, workItemId);
            return rowsAffected;
        } catch (SQLException e) {
            logger.error("Error deleting metadata for work item: {}", workItemId, e);
            throw new RuntimeException("Error deleting metadata for work item: " + workItemId, e);
        }
    }
    
    @Override
    public boolean deleteByWorkItemIdAndKey(UUID workItemId, String key) {
        logger.debug("Deleting metadata for work item: {} and key: {}", workItemId, key);
        
        String sql = "DELETE FROM work_item_metadata WHERE work_item_id = ? AND key = ?";
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, workItemId.toString());
            stmt.setString(2, key);
            
            int rowsAffected = stmt.executeUpdate();
            boolean deleted = rowsAffected > 0;
            
            logger.debug("Deleted metadata for work item: {} and key: {} (success: {})", 
                    workItemId, key, deleted);
            return deleted;
        } catch (SQLException e) {
            logger.error("Error deleting metadata for work item: {} and key: {}", 
                    workItemId, key, e);
            throw new RuntimeException(
                    "Error deleting metadata for work item: " + workItemId + " and key: " + key, e);
        }
    }
    
    @Override
    public List<WorkItemMetadata> findAll() {
        logger.debug("Finding all metadata");
        
        String sql = """
            SELECT id, work_item_id, key, value, created_at
            FROM work_item_metadata
            ORDER BY work_item_id, key
        """;
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            List<WorkItemMetadata> metadataList = new ArrayList<>();
            while (rs.next()) {
                metadataList.add(mapResultSetToMetadata(rs));
            }
            
            logger.debug("Found {} metadata items", metadataList.size());
            return metadataList;
        } catch (SQLException e) {
            logger.error("Error finding all metadata", e);
            throw new RuntimeException("Error finding all metadata", e);
        }
    }
    
    /**
     * Maps a database result set to a WorkItemMetadata object.
     *
     * @param rs the result set containing metadata
     * @return a WorkItemMetadata object
     * @throws SQLException if a database access error occurs
     */
    private WorkItemMetadata mapResultSetToMetadata(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        UUID workItemId = UUID.fromString(rs.getString("work_item_id"));
        String key = rs.getString("key");
        String value = rs.getString("value");
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        
        return new WorkItemMetadata(id, workItemId, key, value, createdAt);
    }
}