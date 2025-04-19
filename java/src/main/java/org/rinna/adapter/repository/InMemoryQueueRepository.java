/*
 * Repository implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.rinna.domain.model.DefaultWorkQueue;
import org.rinna.domain.model.WorkQueue;
import org.rinna.repository.QueueRepository;

/**
 * In-memory implementation of the QueueRepository interface.
 * This class stores work queues in memory, which is suitable for testing
 * and development but not for production use.
 */
public class InMemoryQueueRepository implements QueueRepository {
    private final Map<UUID, WorkQueue> queues = new HashMap<>();
    
    @Override
    public WorkQueue save(WorkQueue queue) {
        queues.put(queue.getId(), queue);
        return queue;
    }
    
    @Override
    public Optional<WorkQueue> findById(UUID id) {
        return Optional.ofNullable(queues.get(id));
    }
    
    @Override
    public Optional<WorkQueue> findByName(String name) {
        return queues.values().stream()
                .filter(queue -> queue.getName().equals(name))
                .findFirst();
    }
    
    @Override
    public List<WorkQueue> findAll() {
        return new ArrayList<>(queues.values());
    }
    
    @Override
    public List<WorkQueue> findByActive(boolean active) {
        return queues.values().stream()
                .filter(queue -> queue.isActive() == active)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean deleteById(UUID id) {
        return queues.remove(id) != null;
    }
    
    @Override
    public long count() {
        return queues.size();
    }
    
    /**
     * Creates the default queue if it doesn't exist.
     * 
     * @return the default queue
     */
    public WorkQueue ensureDefaultQueue() {
        Optional<WorkQueue> defaultQueue = findByName("Default Queue");
        if (defaultQueue.isPresent()) {
            return defaultQueue.get();
        } else {
            WorkQueue queue = new DefaultWorkQueue("Default Queue", "The default work queue for all items");
            return save(queue);
        }
    }
    
    /**
     * Clears all queues from the repository.
     * This is useful for testing.
     */
    public void clear() {
        queues.clear();
    }
}