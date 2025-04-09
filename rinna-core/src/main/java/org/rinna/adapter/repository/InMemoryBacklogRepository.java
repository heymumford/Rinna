/*
 * InMemoryBacklogRepository - In-memory implementation of user backlogs
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import org.rinna.domain.repository.BacklogRepository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory implementation of the BacklogRepository interface.
 * Stores backlogs as ordered lists of work item IDs.
 */
public class InMemoryBacklogRepository implements BacklogRepository {
    
    // Map of username -> list of work item IDs in backlog order
    private final Map<String, List<UUID>> backlogs = new ConcurrentHashMap<>();
    
    // Default username for the current user
    private static final String DEFAULT_USER = System.getProperty("user.name");
    
    /**
     * Gets the backlog for the current user.
     *
     * @return the list of work item IDs in the backlog, in priority order
     */
    @Override
    public List<UUID> getBacklog() {
        return getBacklog(DEFAULT_USER);
    }
    
    /**
     * Gets the backlog for a specific user.
     *
     * @param username the username
     * @return the list of work item IDs in the backlog, in priority order
     */
    @Override
    public List<UUID> getBacklog(String username) {
        return backlogs.computeIfAbsent(username, k -> new ArrayList<>());
    }
    
    /**
     * Adds a work item to the current user's backlog.
     *
     * @param workItemId the ID of the work item to add
     * @return true if the item was added successfully
     */
    @Override
    public boolean addToBacklog(UUID workItemId) {
        return addToBacklog(workItemId, DEFAULT_USER);
    }
    
    /**
     * Adds a work item to a user's backlog.
     *
     * @param workItemId the ID of the work item to add
     * @param username the username
     * @return true if the item was added successfully
     */
    @Override
    public boolean addToBacklog(UUID workItemId, String username) {
        List<UUID> backlog = getBacklog(username);
        if (!backlog.contains(workItemId)) {
            backlog.add(workItemId);
            return true;
        }
        return false;
    }
    
    /**
     * Removes a work item from the current user's backlog.
     *
     * @param workItemId the ID of the work item to remove
     * @return true if the item was removed successfully
     */
    @Override
    public boolean removeFromBacklog(UUID workItemId) {
        return removeFromBacklog(workItemId, DEFAULT_USER);
    }
    
    /**
     * Removes a work item from a user's backlog.
     *
     * @param workItemId the ID of the work item to remove
     * @param username the username
     * @return true if the item was removed successfully
     */
    @Override
    public boolean removeFromBacklog(UUID workItemId, String username) {
        List<UUID> backlog = getBacklog(username);
        return backlog.remove(workItemId);
    }
    
    /**
     * Moves a work item to the specified position in the current user's backlog.
     *
     * @param workItemId the ID of the work item to move
     * @param position the new position (0-based index)
     * @return true if the item was moved successfully
     */
    @Override
    public boolean moveInBacklog(UUID workItemId, int position) {
        return moveInBacklog(workItemId, position, DEFAULT_USER);
    }
    
    /**
     * Moves a work item to the specified position in a user's backlog.
     *
     * @param workItemId the ID of the work item to move
     * @param position the new position (0-based index)
     * @param username the username
     * @return true if the item was moved successfully
     */
    @Override
    public boolean moveInBacklog(UUID workItemId, int position, String username) {
        List<UUID> backlog = getBacklog(username);
        int currentIndex = backlog.indexOf(workItemId);
        
        if (currentIndex == -1) {
            return false;
        }
        
        if (position < 0) {
            position = 0;
        } else if (position >= backlog.size()) {
            position = backlog.size() - 1;
        }
        
        if (currentIndex == position) {
            return true;
        }
        
        backlog.remove(currentIndex);
        backlog.add(position, workItemId);
        return true;
    }
    
    /**
     * Clears all backlogs. Used for testing.
     */
    public void clear() {
        backlogs.clear();
    }
}