/*
 * InMemoryBacklogRepository - In-memory implementation of BacklogRepository
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.rinna.repository.BacklogRepository;

/**
 * In-memory implementation of the BacklogRepository interface.
 */
public class InMemoryBacklogRepository implements BacklogRepository {

    private final Map<String, List<UUID>> backlogs = new ConcurrentHashMap<>();
    
    @Override
    public List<UUID> getBacklog(String username) {
        return backlogs.getOrDefault(username, new ArrayList<>());
    }
    
    @Override
    public boolean setBacklog(String username, List<UUID> backlog) {
        backlogs.put(username, new ArrayList<>(backlog));
        return true;
    }
    
    @Override
    public boolean addToBacklog(String username, UUID workItemId) {
        List<UUID> backlog = backlogs.computeIfAbsent(username, k -> new ArrayList<>());
        
        // Check if the item is already in the backlog
        if (backlog.contains(workItemId)) {
            return false;
        }
        
        backlog.add(workItemId);
        return true;
    }
    
    @Override
    public boolean removeFromBacklog(String username, UUID workItemId) {
        List<UUID> backlog = backlogs.get(username);
        if (backlog == null) {
            return false;
        }
        
        return backlog.remove(workItemId);
    }
    
    @Override
    public int getPositionInBacklog(String username, UUID workItemId) {
        List<UUID> backlog = backlogs.get(username);
        if (backlog == null) {
            return -1;
        }
        
        return backlog.indexOf(workItemId);
    }
}