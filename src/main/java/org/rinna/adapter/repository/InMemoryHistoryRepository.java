/*
 * Repository implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.rinna.domain.model.HistoryEntry;
import org.rinna.domain.model.HistoryEntryType;
import org.rinna.repository.HistoryRepository;

/**
 * In-memory implementation of the HistoryRepository interface.
 */
public class InMemoryHistoryRepository implements HistoryRepository {
    private final Map<UUID, HistoryEntry> entries = new ConcurrentHashMap<>();
    
    @Override
    public HistoryEntry save(HistoryEntry entry) {
        entries.put(entry.id(), entry);
        return entry;
    }
    
    @Override
    public Optional<HistoryEntry> findById(UUID id) {
        return Optional.ofNullable(entries.get(id));
    }
    
    @Override
    public List<HistoryEntry> findByWorkItemId(UUID workItemId) {
        return entries.values().stream()
            .filter(entry -> entry.workItemId().equals(workItemId))
            .sorted((e1, e2) -> e2.timestamp().compareTo(e1.timestamp())) // Most recent first
            .collect(Collectors.toList());
    }
    
    @Override
    public List<HistoryEntry> findByWorkItemIdAndType(UUID workItemId, HistoryEntryType type) {
        return entries.values().stream()
            .filter(entry -> entry.workItemId().equals(workItemId) && entry.type() == type)
            .sorted((e1, e2) -> e2.timestamp().compareTo(e1.timestamp())) // Most recent first
            .collect(Collectors.toList());
    }
    
    @Override
    public List<HistoryEntry> findByWorkItemIdAndTimeRange(UUID workItemId, Instant from, Instant to) {
        return entries.values().stream()
            .filter(entry -> entry.workItemId().equals(workItemId) && 
                   !entry.timestamp().isBefore(from) && 
                   !entry.timestamp().isAfter(to))
            .sorted((e1, e2) -> e2.timestamp().compareTo(e1.timestamp())) // Most recent first
            .collect(Collectors.toList());
    }
    
    @Override
    public List<HistoryEntry> findByUser(String user) {
        return entries.values().stream()
            .filter(entry -> user.equals(entry.user()))
            .sorted((e1, e2) -> e2.timestamp().compareTo(e1.timestamp())) // Most recent first
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean deleteById(UUID id) {
        return entries.remove(id) != null;
    }
    
    @Override
    public int deleteByWorkItemId(UUID workItemId) {
        List<UUID> toRemove = entries.values().stream()
            .filter(entry -> entry.workItemId().equals(workItemId))
            .map(HistoryEntry::id)
            .collect(Collectors.toList());
        
        toRemove.forEach(entries::remove);
        return toRemove.size();
    }
}
