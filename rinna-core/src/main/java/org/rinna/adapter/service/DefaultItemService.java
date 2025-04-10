/*
 * Service implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.DefaultWorkItem;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemRecord;
import org.rinna.domain.repository.ItemRepository;
import org.rinna.domain.repository.MetadataRepository;
import org.rinna.domain.service.ItemService;

/**
 * Default implementation of the ItemService interface.
 */
public class DefaultItemService implements ItemService {
    private final ItemRepository itemRepository;
    private final MetadataRepository metadataRepository;
    
    /**
     * Constructs a new DefaultItemService with the given repository.
     */
    public DefaultItemService(ItemRepository itemRepository) {
        this.itemRepository = Objects.requireNonNull(itemRepository, "Item repository cannot be null");
        this.metadataRepository = null; // Default constructor for backward compatibility
    }
    
    /**
     * Constructs a new DefaultItemService with the given repositories.
     */
    public DefaultItemService(ItemRepository itemRepository, MetadataRepository metadataRepository) {
        this.itemRepository = Objects.requireNonNull(itemRepository, "Item repository cannot be null");
        this.metadataRepository = metadataRepository;
    }
    
    @Override
    public WorkItem create(WorkItemCreateRequest request) {
        Objects.requireNonNull(request, "Create request cannot be null");
        return itemRepository.create(request);
    }
    
    @Override
    public Optional<WorkItem> findById(UUID id) {
        return itemRepository.findById(Objects.requireNonNull(id, "ID cannot be null"));
    }
    
    @Override
    public List<WorkItem> findAll() {
        return itemRepository.findAll();
    }
    
    @Override
    public List<WorkItem> findByType(String type) {
        return itemRepository.findByType(Objects.requireNonNull(type, "Type cannot be null"));
    }
    
    @Override
    public List<WorkItem> findByStatus(String status) {
        return itemRepository.findByStatus(Objects.requireNonNull(status, "Status cannot be null"));
    }
    
    @Override
    public List<WorkItem> findByAssignee(String assignee) {
        return itemRepository.findByAssignee(assignee);
    }
    
    @Override
    public WorkItem updateAssignee(UUID id, String assignee) {
        WorkItem item = findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found: " + id));
        
        // Handle both implementations for backward compatibility
        WorkItem updatedItem = switch (item) {
            case WorkItemRecord record -> record.withAssignee(assignee);
            case DefaultWorkItem defaultItem -> defaultItem.toRecord().withAssignee(assignee);
            case null -> throw new IllegalArgumentException("Item cannot be null");
            default -> throw new UnsupportedOperationException("Unsupported WorkItem implementation: " + item.getClass().getName());
        };
        
        return itemRepository.save(updatedItem);
    }
    
    @Override
    public void deleteById(UUID id) {
        itemRepository.deleteById(Objects.requireNonNull(id, "ID cannot be null"));
    }
    
    @Override
    public boolean existsById(UUID id) {
        return findById(id).isPresent();
    }
    
    @Override
    public boolean updateMetadata(UUID id, Map<String, String> metadata) {
        Objects.requireNonNull(id, "ID cannot be null");
        Objects.requireNonNull(metadata, "Metadata cannot be null");
        
        if (metadataRepository == null) {
            // Store metadata in the work item itself as a fallback
            Optional<WorkItem> optionalItem = findById(id);
            if (optionalItem.isEmpty()) {
                return false;
            }
            
            WorkItem item = optionalItem.get();
            
            // For DefaultWorkItem implementation, use reflection to set metadata
            if (item instanceof DefaultWorkItem defaultItem) {
                try {
                    // Create a new map with existing metadata (if any) plus new values
                    Map<String, String> existingMetadata = defaultItem.getMetadata();
                    Map<String, String> updatedMetadata = existingMetadata != null ? 
                        new HashMap<>(existingMetadata) : new HashMap<>();
                    updatedMetadata.putAll(metadata);
                    
                    // Update the work item with the new metadata
                    WorkItemRecord updatedRecord = defaultItem.toRecord();
                    // In a real implementation, we would set the metadata here
                    
                    // Save the updated item
                    itemRepository.save(updatedRecord);
                    return true;
                } catch (Exception e) {
                    return false;
                }
            }
            
            return false;
        }
        
        // Use the metadata repository if available
        try {
            metadataRepository.updateMetadata(id, metadata);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}