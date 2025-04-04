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
                .orElseThrow(() -> new IllegalArgumentException(STR."Work item not found: \{id}"));
        
        return switch (item) {
            case DefaultWorkItem defaultItem -> 
                itemRepository.save(defaultItem.setAssignee(assignee));
            default -> 
                throw new UnsupportedOperationException("Cannot update non-default work item");
        };
    }
    
    @Override
    public void deleteById(UUID id) {
        itemRepository.deleteById(Objects.requireNonNull(id, "ID cannot be null"));
    }
}