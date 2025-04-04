/*
 * Service implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import org.rinna.domain.entity.DefaultWorkItem;
import org.rinna.domain.entity.WorkItem;
import org.rinna.domain.entity.WorkItemCreateRequest;
import org.rinna.domain.repository.ItemRepository;
import org.rinna.domain.usecase.ItemService;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of the ItemService interface.
 */
public class DefaultItemService implements ItemService {
    private final ItemRepository itemRepository;
    
    /**
     * Constructs a new DefaultItemService with the given repository.
     * 
     * @param itemRepository the item repository
     */
    public DefaultItemService(ItemRepository itemRepository) {
        this.itemRepository = Objects.requireNonNull(itemRepository, "Item repository cannot be null");
    }
    
    @Override
    public WorkItem create(WorkItemCreateRequest request) {
        Objects.requireNonNull(request, "Create request cannot be null");
        return itemRepository.create(request);
    }
    
    @Override
    public Optional<WorkItem> findById(UUID id) {
        Objects.requireNonNull(id, "ID cannot be null");
        return itemRepository.findById(id);
    }
    
    @Override
    public List<WorkItem> findAll() {
        return itemRepository.findAll();
    }
    
    @Override
    public List<WorkItem> findByType(String type) {
        Objects.requireNonNull(type, "Type cannot be null");
        return itemRepository.findByType(type);
    }
    
    @Override
    public List<WorkItem> findByStatus(String status) {
        Objects.requireNonNull(status, "Status cannot be null");
        return itemRepository.findByStatus(status);
    }
    
    @Override
    public List<WorkItem> findByAssignee(String assignee) {
        return itemRepository.findByAssignee(assignee);
    }
    
    @Override
    public WorkItem updateAssignee(UUID id, String assignee) {
        Objects.requireNonNull(id, "ID cannot be null");
        
        WorkItem item = itemRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found: " + id));
        
        if (item instanceof DefaultWorkItem) {
            DefaultWorkItem defaultItem = (DefaultWorkItem) item;
            defaultItem.setAssignee(assignee);
            return itemRepository.save(defaultItem);
        } else {
            throw new UnsupportedOperationException("Cannot update non-default work item");
        }
    }
    
    @Override
    public void deleteById(UUID id) {
        Objects.requireNonNull(id, "ID cannot be null");
        itemRepository.deleteById(id);
    }
}