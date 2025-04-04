/*
 * Repository implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.persistence;

import org.rinna.domain.entity.DefaultWorkItem;
import org.rinna.domain.entity.WorkItem;
import org.rinna.domain.entity.WorkItemCreateRequest;
import org.rinna.domain.entity.WorkItemType;
import org.rinna.domain.entity.WorkflowState;
import org.rinna.domain.repository.ItemRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of the ItemRepository interface.
 * This implementation stores work items in memory and is intended for testing
 * and demonstration purposes only.
 */
public class InMemoryItemRepository implements ItemRepository {
    private final Map<UUID, WorkItem> items = new ConcurrentHashMap<>();
    
    @Override
    public WorkItem save(WorkItem item) {
        items.put(item.getId(), item);
        return item;
    }
    
    @Override
    public WorkItem create(WorkItemCreateRequest request) {
        UUID id = UUID.randomUUID();
        DefaultWorkItem item = new DefaultWorkItem(id, request);
        items.put(id, item);
        return item;
    }
    
    @Override
    public Optional<WorkItem> findById(UUID id) {
        return Optional.ofNullable(items.get(id));
    }
    
    @Override
    public List<WorkItem> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(items.values()));
    }
    
    @Override
    public List<WorkItem> findByType(String type) {
        try {
            WorkItemType workItemType = WorkItemType.valueOf(type.toUpperCase());
            return items.values().stream()
                    .filter(item -> item.getType() == workItemType)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<WorkItem> findByStatus(String status) {
        try {
            WorkflowState workflowState = WorkflowState.valueOf(status.toUpperCase());
            return items.values().stream()
                    .filter(item -> item.getStatus() == workflowState)
                    .collect(Collectors.toList());
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }
    
    @Override
    public List<WorkItem> findByAssignee(String assignee) {
        if (assignee == null) {
            return items.values().stream()
                    .filter(item -> item.getAssignee() == null)
                    .collect(Collectors.toList());
        }
        return items.values().stream()
                .filter(item -> assignee.equals(item.getAssignee()))
                .collect(Collectors.toList());
    }
    
    @Override
    public void deleteById(UUID id) {
        items.remove(id);
    }
    
    /**
     * Clears all items from the repository.
     * This method is intended for testing purposes only.
     */
    public void clear() {
        items.clear();
    }
}