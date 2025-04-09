/*
 * Domain repository interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.repository;

import org.rinna.domain.model.WorkItemMetadata;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for work item metadata.
 * This interface defines the data access operations for work item metadata.
 */
public interface MetadataRepository {
    
    /**
     * Saves metadata for a work item.
     * If metadata with the same work item ID and key already exists, it will be updated.
     *
     * @param metadata the metadata to save
     * @return the saved metadata
     */
    WorkItemMetadata save(WorkItemMetadata metadata);
    
    /**
     * Finds metadata by its ID.
     *
     * @param id the ID of the metadata
     * @return an Optional containing the metadata, or empty if not found
     */
    Optional<WorkItemMetadata> findById(UUID id);
    
    /**
     * Finds all metadata for a work item.
     *
     * @param workItemId the ID of the work item
     * @return a list of metadata for the work item
     */
    List<WorkItemMetadata> findByWorkItemId(UUID workItemId);
    
    /**
     * Finds metadata for a work item by key.
     *
     * @param workItemId the ID of the work item
     * @param key the metadata key
     * @return an Optional containing the metadata, or empty if not found
     */
    Optional<WorkItemMetadata> findByWorkItemIdAndKey(UUID workItemId, String key);
    
    /**
     * Returns all metadata for a work item as a key-value map.
     *
     * @param workItemId the ID of the work item
     * @return a map of metadata keys to values
     */
    Map<String, String> getMetadataMap(UUID workItemId);
    
    /**
     * Deletes metadata by its ID.
     *
     * @param id the ID of the metadata to delete
     * @return true if the metadata was deleted, false if it wasn't found
     */
    boolean deleteById(UUID id);
    
    /**
     * Deletes all metadata for a work item.
     *
     * @param workItemId the ID of the work item
     * @return the number of metadata items deleted
     */
    int deleteByWorkItemId(UUID workItemId);
    
    /**
     * Deletes metadata for a work item by key.
     *
     * @param workItemId the ID of the work item
     * @param key the metadata key
     * @return true if the metadata was deleted, false if it wasn't found
     */
    boolean deleteByWorkItemIdAndKey(UUID workItemId, String key);
    
    /**
     * Returns all metadata.
     *
     * @return a list of all metadata
     */
    List<WorkItemMetadata> findAll();
    
    /**
     * Updates metadata for a work item.
     * 
     * @param workItemId the ID of the work item
     * @param metadata the metadata to update
     * @return true if the update was successful
     */
    boolean updateMetadata(UUID workItemId, Map<String, String> metadata);
}