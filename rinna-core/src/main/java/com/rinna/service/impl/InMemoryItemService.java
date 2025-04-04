/*
 * Service component for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package com.rinna.service.impl;

import com.rinna.model.DefaultWorkItem;
import com.rinna.model.WorkItem;
import com.rinna.model.WorkItemCreateRequest;
import com.rinna.service.ItemService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of the ItemService interface.
 */
public class InMemoryItemService implements ItemService {
    private final Map<UUID, WorkItem> items = new ConcurrentHashMap<>();
    
    @Override
    public WorkItem create(WorkItemCreateRequest request) {
        WorkItem item = DefaultWorkItem.fromCreateRequest(request);
        items.put(item.getId(), item);
        return item;
    }
    
    @Override
    public Optional<WorkItem> findById(UUID id) {
        return Optional.ofNullable(items.get(id));
    }
    
    @Override
    public List<WorkItem> findAll() {
        return new ArrayList<>(items.values());
    }
    
    /**
     * Updates a work item in the repository.
     *
     * @param item the work item to update
     * @return the updated work item
     */
    public WorkItem update(WorkItem item) {
        items.put(item.getId(), item);
        return item;
    }
    
    /**
     * Clears all items from the repository.
     */
    public void clear() {
        items.clear();
    }
}