/*
 * Repository implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import org.rinna.domain.model.WorkItemMetadata;
import org.rinna.repository.MetadataRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * In-memory implementation of the MetadataRepository interface.
 * This class stores work item metadata in memory, which is suitable for testing
 * and development but not for production use.
 */
public class InMemoryMetadataRepository implements MetadataRepository {
    private final Map<UUID, WorkItemMetadata> metadata = new HashMap<>();
    
    @Override
    public WorkItemMetadata save(WorkItemMetadata workItemMetadata) {
        // If metadata with the same work item ID and key exists, remove it first
        findByWorkItemIdAndKey(workItemMetadata.getWorkItemId(), workItemMetadata.getKey())
                .ifPresent(existing -> metadata.remove(existing.getId()));
        
        metadata.put(workItemMetadata.getId(), workItemMetadata);
        return workItemMetadata;
    }
    
    @Override
    public Optional<WorkItemMetadata> findById(UUID id) {
        return Optional.ofNullable(metadata.get(id));
    }
    
    @Override
    public List<WorkItemMetadata> findByWorkItemId(UUID workItemId) {
        return metadata.values().stream()
                .filter(meta -> meta.getWorkItemId().equals(workItemId))
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<WorkItemMetadata> findByWorkItemIdAndKey(UUID workItemId, String key) {
        return metadata.values().stream()
                .filter(meta -> meta.getWorkItemId().equals(workItemId) && meta.getKey().equals(key))
                .findFirst();
    }
    
    @Override
    public Map<String, String> getMetadataMap(UUID workItemId) {
        return metadata.values().stream()
                .filter(meta -> meta.getWorkItemId().equals(workItemId))
                .collect(Collectors.toMap(WorkItemMetadata::getKey, WorkItemMetadata::getValue));
    }
    
    @Override
    public boolean deleteById(UUID id) {
        return metadata.remove(id) != null;
    }
    
    @Override
    public int deleteByWorkItemId(UUID workItemId) {
        List<UUID> toRemove = metadata.values().stream()
                .filter(meta -> meta.getWorkItemId().equals(workItemId))
                .map(WorkItemMetadata::getId)
                .collect(Collectors.toList());
        
        int count = toRemove.size();
        for (UUID id : toRemove) {
            metadata.remove(id);
        }
        
        return count;
    }
    
    @Override
    public boolean deleteByWorkItemIdAndKey(UUID workItemId, String key) {
        Optional<WorkItemMetadata> toDelete = findByWorkItemIdAndKey(workItemId, key);
        if (toDelete.isPresent()) {
            metadata.remove(toDelete.get().getId());
            return true;
        }
        return false;
    }
    
    @Override
    public List<WorkItemMetadata> findAll() {
        return new ArrayList<>(metadata.values());
    }
    
    /**
     * Clears all metadata from the repository.
     * This is useful for testing.
     */
    public void clear() {
        metadata.clear();
    }
}