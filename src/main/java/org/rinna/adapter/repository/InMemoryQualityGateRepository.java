/*
 * InMemoryQualityGateRepository - In-memory implementation of QualityGateRepository
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import org.rinna.repository.QualityGateRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of the QualityGateRepository interface.
 */
public class InMemoryQualityGateRepository implements QualityGateRepository {

    private final Map<String, Map<String, Object>> configurations = new ConcurrentHashMap<>();
    private final Map<UUID, List<Map<String, Object>>> history = new ConcurrentHashMap<>();
    
    @Override
    public Optional<Map<String, Object>> findByProjectId(String projectId) {
        return Optional.ofNullable(configurations.get(projectId));
    }
    
    @Override
    public boolean save(String projectId, Map<String, Object> configuration) {
        configurations.put(projectId, new HashMap<>(configuration));
        return true;
    }
    
    @Override
    public boolean delete(String projectId) {
        return configurations.remove(projectId) != null;
    }
    
    @Override
    public UUID saveHistoryEntry(UUID workItemId, Map<String, Object> historyEntry) {
        UUID entryId = UUID.randomUUID();
        
        // Add the entry ID to the history entry
        Map<String, Object> entry = new HashMap<>(historyEntry);
        entry.put("id", entryId);
        
        // Add the entry to the history
        history.computeIfAbsent(workItemId, k -> new ArrayList<>()).add(entry);
        
        return entryId;
    }
    
    @Override
    public List<Map<String, Object>> findHistoryByWorkItemId(UUID workItemId) {
        return history.getOrDefault(workItemId, Collections.emptyList()).stream()
                .map(HashMap::new)  // Create defensive copies
                .collect(Collectors.toList());
    }
}