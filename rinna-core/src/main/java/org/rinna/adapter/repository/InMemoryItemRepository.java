/*
 * Repository implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemRecord;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.domain.repository.ItemRepository;

/**
 * In-memory implementation of the ItemRepository interface that stores work items in memory.
 * This implementation uses ConcurrentHashMap to provide thread-safe access to work items and their metadata.
 * Primarily intended for testing, demonstration, and local development purposes only.
 * 
 * <p>This repository supports all standard operations including creating, retrieving, updating, and
 * deleting work items, as well as querying by various attributes and storing custom metadata.</p>
 */
public class InMemoryItemRepository implements ItemRepository {
    private final Map<UUID, WorkItem> items = new ConcurrentHashMap<>();
    // Map to store metadata for each work item
    private final Map<UUID, Map<String, String>> itemMetadata = new ConcurrentHashMap<>();

    /**
     * Saves a work item in the in-memory repository.
     * If an item with the same ID already exists, it will be replaced.
     *
     * @param item the work item to save
     * @return the saved work item (same instance as provided)
     */
    @Override
    public WorkItem save(WorkItem item) {
        items.put(item.getId(), item);
        return item;
    }

    /**
     * Creates a new work item from a create request.
     * Generates a random UUID for the new item and converts the request to a WorkItem record.
     *
     * @param request the create request containing initial work item details
     * @return the newly created work item
     */
    @Override
    public WorkItem create(WorkItemCreateRequest request) {
        WorkItem item = WorkItemRecord.fromRequest(UUID.randomUUID(), request);
        return save(item);
    }

    /**
     * Finds a work item by its unique identifier.
     *
     * @param id the UUID of the work item to find
     * @return an Optional containing the work item if found, or an empty Optional if not found
     */
    @Override
    public Optional<WorkItem> findById(UUID id) {
        return Optional.ofNullable(items.get(id));
    }

    /**
     * Retrieves all work items stored in the repository.
     * Returns a new defensive copy of the internal collection to prevent modification.
     *
     * @return a list containing all work items
     */
    @Override
    public List<WorkItem> findAll() {
        return new ArrayList<>(items.values());
    }

    /**
     * Finds work items by their type.
     * Converts the string type to a WorkItemType enum value (case-insensitive).
     * If the type is invalid, returns an empty list instead of throwing an exception.
     *
     * @param type the type of work items to find (case-insensitive)
     * @return a list of work items of the given type, or an empty list if the type is invalid
     */
    @Override
    public List<WorkItem> findByType(String type) {
        try {
            WorkItemType workItemType = WorkItemType.valueOf(type.toUpperCase());
            return items.values().stream()
                    .filter(item -> item.getType() == workItemType)
                    .toList();
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }

    /**
     * Finds work items by their workflow status.
     * Converts the string status to a WorkflowState enum value (case-insensitive).
     * If the status is invalid, returns an empty list instead of throwing an exception.
     *
     * @param status the workflow status to search for (case-insensitive)
     * @return a list of work items with the given status, or an empty list if the status is invalid
     */
    @Override
    public List<WorkItem> findByStatus(String status) {
        try {
            WorkflowState workflowState = WorkflowState.valueOf(status.toUpperCase());
            return items.values().stream()
                    .filter(item -> item.getStatus() == workflowState)
                    .toList();
        } catch (IllegalArgumentException e) {
            return Collections.emptyList();
        }
    }

    @Override
    public List<WorkItem> findByAssignee(String assignee) {
        return items.values().stream()
                .filter(item -> Objects.equals(assignee, item.getAssignee()))
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        items.remove(id);
    }

    /**
     * Clears all items from the repository (for testing).
     */
    public void clear() {
        items.clear();
    }

    @Override
    public boolean existsById(UUID id) {
        return items.containsKey(id);
    }

    @Override
    public WorkItem updateMetadata(UUID id, Map<String, String> metadata) {
        if (!existsById(id)) {
            return null;
        }

        // Get or create metadata map for this item
        Map<String, String> itemMeta = itemMetadata.computeIfAbsent(id, k -> new ConcurrentHashMap<>());

        // Update metadata with new values
        if (metadata != null) {
            itemMeta.putAll(metadata);
        }

        return items.get(id);
    }

    @Override
    public List<WorkItem> findByCustomField(String field, String value) {
        if (field == null || value == null) {
            return Collections.emptyList();
        }

        return items.entrySet().stream()
                .filter(entry -> {
                    Map<String, String> meta = itemMetadata.get(entry.getKey());
                    return meta != null && value.equals(meta.get(field));
                })
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }
}
