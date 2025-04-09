/*
 * SQLite persistence implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.data.sqlite;

import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemRecord;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.domain.repository.ItemRepository;
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
 * SQLite implementation of the ItemRepository interface.
 * Provides persistence of WorkItem entities in an SQLite database.
 */
public class SqliteItemRepository implements ItemRepository {
    private static final Logger logger = LoggerFactory.getLogger(SqliteItemRepository.class);
    
    private final SqliteConnectionManager connectionManager;
    private final SqliteMetadataRepository metadataRepository;
    
    /**
     * Creates a new SqliteItemRepository with a connection manager.
     *
     * @param connectionManager the SQLite connection manager
     * @param metadataRepository the SQLite metadata repository for handling item metadata
     */
    public SqliteItemRepository(SqliteConnectionManager connectionManager, 
                               SqliteMetadataRepository metadataRepository) {
        this.connectionManager = connectionManager;
        this.metadataRepository = metadataRepository;
    }
    
    @Override
    public WorkItem save(WorkItem item) {
        logger.debug("Saving work item: {}", item.getId());
        
        String sql = """
            INSERT OR REPLACE INTO work_items 
            (id, title, description, type, status, priority, assignee, 
             created_at, updated_at, parent_id, project_id, visibility, local_only)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, item.getId().toString());
            stmt.setString(2, item.getTitle());
            stmt.setString(3, item.getDescription());
            stmt.setString(4, item.getType().name());
            stmt.setString(5, item.getStatus().name());
            stmt.setString(6, item.getPriority().name());
            stmt.setString(7, item.getAssignee());
            stmt.setTimestamp(8, Timestamp.from(item.getCreatedAt()));
            stmt.setTimestamp(9, Timestamp.from(item.getUpdatedAt()));
            stmt.setString(10, item.getParentId().map(UUID::toString).orElse(null));
            stmt.setString(11, item.getProjectId().map(UUID::toString).orElse(null));
            stmt.setString(12, item.getVisibility());
            stmt.setInt(13, item.isLocalOnly() ? 1 : 0);
            
            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected == 0) {
                logger.warn("Failed to save work item: {}", item.getId());
                throw new RuntimeException("Failed to save work item: " + item.getId());
            }
            
            logger.debug("Work item saved successfully: {}", item.getId());
            return item;
        } catch (SQLException e) {
            logger.error("Error saving work item: {}", item.getId(), e);
            throw new RuntimeException("Error saving work item: " + item.getId(), e);
        }
    }
    
    @Override
    public WorkItem create(WorkItemCreateRequest request) {
        UUID id = UUID.randomUUID();
        WorkItem item = WorkItemRecord.fromRequest(id, request);
        WorkItem savedItem = save(item);
        
        // Save metadata if provided
        if (request.metadata() != null && !request.metadata().isEmpty()) {
            for (Map.Entry<String, String> entry : request.metadata().entrySet()) {
                metadataRepository.saveMetadata(id, entry.getKey(), entry.getValue());
            }
        }
        
        return savedItem;
    }
    
    @Override
    public Optional<WorkItem> findById(UUID id) {
        logger.debug("Finding work item by ID: {}", id);
        
        String sql = """
            SELECT id, title, description, type, status, priority, assignee, 
                   created_at, updated_at, parent_id, project_id, visibility, local_only
            FROM work_items
            WHERE id = ?
        """;
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, id.toString());
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    WorkItem item = mapResultSetToWorkItem(rs);
                    logger.debug("Found work item: {}", id);
                    return Optional.of(item);
                } else {
                    logger.debug("Work item not found: {}", id);
                    return Optional.empty();
                }
            }
        } catch (SQLException e) {
            logger.error("Error finding work item by ID: {}", id, e);
            throw new RuntimeException("Error finding work item by ID: " + id, e);
        }
    }
    
    @Override
    public List<WorkItem> findAll() {
        logger.debug("Finding all work items");
        
        String sql = """
            SELECT id, title, description, type, status, priority, assignee, 
                   created_at, updated_at, parent_id, project_id, visibility, local_only
            FROM work_items
            ORDER BY created_at DESC
        """;
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            List<WorkItem> items = new ArrayList<>();
            while (rs.next()) {
                items.add(mapResultSetToWorkItem(rs));
            }
            
            logger.debug("Found {} work items", items.size());
            return items;
        } catch (SQLException e) {
            logger.error("Error finding all work items", e);
            throw new RuntimeException("Error finding all work items", e);
        }
    }
    
    @Override
    public List<WorkItem> findByType(WorkItemType type) {
        logger.debug("Finding work items by type: {}", type);
        
        String sql = """
            SELECT id, title, description, type, status, priority, assignee, 
                   created_at, updated_at, parent_id, project_id, visibility, local_only
            FROM work_items
            WHERE type = ?
            ORDER BY created_at DESC
        """;
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, type.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<WorkItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapResultSetToWorkItem(rs));
                }
                
                logger.debug("Found {} work items with type: {}", items.size(), type);
                return items;
            }
        } catch (SQLException e) {
            logger.error("Error finding work items by type: {}", type, e);
            throw new RuntimeException("Error finding work items by type: " + type, e);
        }
    }
    
    @Override
    public List<WorkItem> findByStatus(WorkflowState status) {
        logger.debug("Finding work items by status: {}", status);
        
        String sql = """
            SELECT id, title, description, type, status, priority, assignee, 
                   created_at, updated_at, parent_id, project_id, visibility, local_only
            FROM work_items
            WHERE status = ?
            ORDER BY created_at DESC
        """;
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status.name());
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<WorkItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapResultSetToWorkItem(rs));
                }
                
                logger.debug("Found {} work items with status: {}", items.size(), status);
                return items;
            }
        } catch (SQLException e) {
            logger.error("Error finding work items by status: {}", status, e);
            throw new RuntimeException("Error finding work items by status: " + status, e);
        }
    }
    
    @Override
    public List<WorkItem> findByAssignee(String assignee) {
        logger.debug("Finding work items by assignee: {}", assignee);
        
        String sql = """
            SELECT id, title, description, type, status, priority, assignee, 
                   created_at, updated_at, parent_id, project_id, visibility, local_only
            FROM work_items
            WHERE assignee = ?
            ORDER BY created_at DESC
        """;
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, assignee);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<WorkItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapResultSetToWorkItem(rs));
                }
                
                logger.debug("Found {} work items assigned to: {}", items.size(), assignee);
                return items;
            }
        } catch (SQLException e) {
            logger.error("Error finding work items by assignee: {}", assignee, e);
            throw new RuntimeException("Error finding work items by assignee: " + assignee, e);
        }
    }
    
    @Override
    public WorkItem updateMetadata(UUID id, Map<String, String> metadata) {
        logger.debug("Updating metadata for work item: {}", id);
        
        // Find the item first to verify it exists
        WorkItem item = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found: " + id));
        
        // Use a transaction to ensure consistency
        try (Connection conn = connectionManager.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // Clear existing metadata
                metadataRepository.deleteByWorkItemId(id);
                
                // Add new metadata
                for (Map.Entry<String, String> entry : metadata.entrySet()) {
                    metadataRepository.saveMetadata(id, entry.getKey(), entry.getValue());
                }
                
                conn.commit();
                logger.debug("Metadata updated successfully for work item: {}", id);
                return item;
            } catch (Exception e) {
                conn.rollback();
                logger.error("Error updating metadata, transaction rolled back: {}", id, e);
                throw new RuntimeException("Error updating metadata: " + id, e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error updating metadata for work item: {}", id, e);
            throw new RuntimeException("Error updating metadata: " + id, e);
        }
    }
    
    @Override
    public List<WorkItem> findByCustomField(String field, String value) {
        logger.debug("Finding work items by custom field: {} = {}", field, value);
        
        String sql = """
            SELECT wi.id, wi.title, wi.description, wi.type, wi.status, wi.priority, wi.assignee, 
                   wi.created_at, wi.updated_at, wi.parent_id, wi.project_id, wi.visibility, wi.local_only
            FROM work_items wi
            JOIN work_item_metadata wm ON wi.id = wm.work_item_id
            WHERE wm.key = ? AND wm.value = ?
            ORDER BY wi.created_at DESC
        """;
        
        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, field);
            stmt.setString(2, value);
            
            try (ResultSet rs = stmt.executeQuery()) {
                List<WorkItem> items = new ArrayList<>();
                while (rs.next()) {
                    items.add(mapResultSetToWorkItem(rs));
                }
                
                logger.debug("Found {} work items with custom field: {} = {}", 
                        items.size(), field, value);
                return items;
            }
        } catch (SQLException e) {
            logger.error("Error finding work items by custom field: {} = {}", field, value, e);
            throw new RuntimeException(
                    "Error finding work items by custom field: " + field + " = " + value, e);
        }
    }
    
    @Override
    public void deleteById(UUID id) {
        logger.debug("Deleting work item by ID: {}", id);
        
        // Use a transaction to ensure consistency
        try (Connection conn = connectionManager.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // First delete metadata (should be handled by foreign key cascade,
                // but let's be explicit)
                metadataRepository.deleteByWorkItemId(id);
                
                // Then delete the work item
                String sql = "DELETE FROM work_items WHERE id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, id.toString());
                    int rowsAffected = stmt.executeUpdate();
                    
                    conn.commit();
                    logger.debug("Deleted work item: {} (rows affected: {})", id, rowsAffected);
                }
            } catch (Exception e) {
                conn.rollback();
                logger.error("Error deleting work item, transaction rolled back: {}", id, e);
                throw new RuntimeException("Error deleting work item: " + id, e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            logger.error("Error deleting work item: {}", id, e);
            throw new RuntimeException("Error deleting work item: " + id, e);
        }
    }
    
    /**
     * Maps a database result set to a WorkItem object.
     *
     * @param rs the result set containing work item data
     * @return a WorkItem object
     * @throws SQLException if a database access error occurs
     */
    private WorkItem mapResultSetToWorkItem(ResultSet rs) throws SQLException {
        UUID id = UUID.fromString(rs.getString("id"));
        String title = rs.getString("title");
        String description = rs.getString("description");
        WorkItemType type = WorkItemType.valueOf(rs.getString("type"));
        WorkflowState status = WorkflowState.valueOf(rs.getString("status"));
        Priority priority = Priority.valueOf(rs.getString("priority"));
        String assignee = rs.getString("assignee");
        Instant createdAt = rs.getTimestamp("created_at").toInstant();
        Instant updatedAt = rs.getTimestamp("updated_at").toInstant();
        
        UUID parentId = null;
        if (rs.getString("parent_id") != null) {
            parentId = UUID.fromString(rs.getString("parent_id"));
        }
        
        UUID projectId = null;
        if (rs.getString("project_id") != null) {
            projectId = UUID.fromString(rs.getString("project_id"));
        }
        
        String visibility = rs.getString("visibility");
        boolean localOnly = rs.getInt("local_only") == 1;
        
        return new WorkItemRecord(
                id, title, description, type, status, priority, assignee,
                createdAt, updatedAt, parentId, projectId, visibility, localOnly
        );
    }
}