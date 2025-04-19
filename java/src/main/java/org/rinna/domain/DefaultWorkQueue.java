/*
 * Domain entity implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of the WorkQueue interface.
 */
public class DefaultWorkQueue implements WorkQueue {
    private final UUID id;
    private final String name;
    private final String description;
    private boolean active;
    private final List<WorkItem> items;
    private final Instant createdAt;
    private Instant updatedAt;
    
    /**
     * Creates a new DefaultWorkQueue with the given parameters.
     * 
     * @param id the unique identifier
     * @param name the name
     * @param description the description
     * @param active whether the queue is active
     */
    public DefaultWorkQueue(UUID id, String name, String description, boolean active) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.active = active;
        this.items = new ArrayList<>();
        this.createdAt = Instant.now();
        this.updatedAt = this.createdAt;
    }
    
    /**
     * Creates a new DefaultWorkQueue with a generated ID.
     * 
     * @param name the name
     * @param description the description
     */
    public DefaultWorkQueue(String name, String description) {
        this(UUID.randomUUID(), name, description, true);
    }
    
    @Override
    public UUID getId() {
        return id;
    }
    
    @Override
    public String getName() {
        return name;
    }
    
    @Override
    public String getDescription() {
        return description;
    }
    
    @Override
    public boolean isActive() {
        return active;
    }
    
    /**
     * Sets whether this queue is active.
     * 
     * @param active true to make active, false to make inactive
     */
    public void setActive(boolean active) {
        this.active = active;
        this.updatedAt = Instant.now();
    }
    
    @Override
    public List<WorkItem> getItems() {
        return new ArrayList<>(items);
    }
    
    @Override
    public List<WorkItem> getItemsByType(WorkItemType type) {
        return items.stream()
            .filter(item -> item.getType() == type)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<WorkItem> getItemsByState(WorkflowState state) {
        return items.stream()
            .filter(item -> item.getStatus() == state)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<WorkItem> getItemsByPriority(Priority priority) {
        return items.stream()
            .filter(item -> item.getPriority() == priority)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<WorkItem> getItemsByAssignee(String assignee) {
        return items.stream()
            .filter(item -> assignee.equals(item.getAssignee()))
            .collect(Collectors.toList());
    }
    
    @Override
    public Optional<WorkItem> getNextItem() {
        return items.isEmpty() ? Optional.empty() : Optional.of(items.get(0));
    }
    
    @Override
    public void addItem(WorkItem item) {
        if (!items.contains(item)) {
            items.add(item);
            this.updatedAt = Instant.now();
            reprioritize();
        }
    }
    
    @Override
    public boolean removeItem(UUID itemId) {
        boolean removed = items.removeIf(item -> item.getId().equals(itemId));
        if (removed) {
            this.updatedAt = Instant.now();
        }
        return removed;
    }
    
    @Override
    public void reprioritize() {
        // Implementation of the prioritization algorithm
        items.sort(createPriorityComparator());
        this.updatedAt = Instant.now();
    }
    
    /**
     * Creates a comparator for work item prioritization.
     * The prioritization rules are:
     * 1. HIGH priority items come before MEDIUM items which come before LOW items
     * 2. Within the same priority:
     *    - Production issues (determined by metadata) come first
     *    - BUGs come before FEATUREs which come before CHOREs which come before GOALs
     * 3. Within the same priority and type, older items come first (oldest to newest)
     * 
     * @return the priority comparator
     */
    private Comparator<WorkItem> createPriorityComparator() {
        return Comparator
            // First by priority (HIGH is lowest ordinal value)
            .comparing(WorkItem::getPriority)
            // Then by type with custom ordering: BUG -> FEATURE -> CHORE -> GOAL 
            .thenComparing(item -> getTypeWeight(item.getType()))
            // Then by age (older items first)
            .thenComparing(WorkItem::getCreatedAt);
    }
    
    private int getTypeWeight(WorkItemType type) {
        switch (type) {
            case BUG: return 0;
            case FEATURE: return 1;
            case CHORE: return 2;
            case GOAL: return 3;
            default: return 4;
        }
    }
    
    @Override
    public int size() {
        return items.size();
    }
    
    @Override
    public boolean isEmpty() {
        return items.isEmpty();
    }
    
    /**
     * Returns the creation timestamp of this work queue.
     * 
     * @return the creation timestamp
     */
    public Instant getCreatedAt() {
        return createdAt;
    }
    
    /**
     * Returns the last update timestamp of this work queue.
     * 
     * @return the last update timestamp
     */
    public Instant getUpdatedAt() {
        return updatedAt;
    }
}