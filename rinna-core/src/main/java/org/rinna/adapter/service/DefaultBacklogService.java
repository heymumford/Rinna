/*
 * DefaultBacklogService - Implementation of BacklogService
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import org.rinna.domain.model.WorkItem;
import org.rinna.domain.repository.BacklogRepository;
import org.rinna.domain.repository.ItemRepository;
import org.rinna.domain.service.BacklogService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of the BacklogService interface.
 */
public class DefaultBacklogService implements BacklogService {
    
    private final BacklogRepository backlogRepository;
    private final ItemRepository itemRepository;
    
    /**
     * Creates a new DefaultBacklogService with the specified repositories.
     *
     * @param backlogRepository the backlog repository
     * @param itemRepository the item repository
     */
    public DefaultBacklogService(BacklogRepository backlogRepository, ItemRepository itemRepository) {
        this.backlogRepository = backlogRepository;
        this.itemRepository = itemRepository;
    }
    
    /**
     * Gets the current user's backlog.
     *
     * @return a list of work items in the backlog, in priority order
     */
    @Override
    public List<WorkItem> getBacklog() {
        return getBacklogItems(backlogRepository.getBacklog());
    }
    
    /**
     * Gets a user's backlog.
     *
     * @param username the username
     * @return a list of work items in the backlog, in priority order
     */
    @Override
    public List<WorkItem> getBacklog(String username) {
        return getBacklogItems(backlogRepository.getBacklog(username));
    }
    
    /**
     * Helper method to convert a list of work item IDs to work items.
     *
     * @param itemIds the list of work item IDs
     * @return the list of work items
     */
    private List<WorkItem> getBacklogItems(List<UUID> itemIds) {
        List<WorkItem> result = new ArrayList<>();
        for (UUID id : itemIds) {
            Optional<WorkItem> item = itemRepository.findById(id);
            item.ifPresent(result::add);
        }
        return result;
    }
    
    /**
     * Adds a work item to the current user's backlog.
     *
     * @param workItemId the ID of the work item to add
     * @return true if the item was added successfully
     */
    @Override
    public boolean addToBacklog(UUID workItemId) {
        if (!itemRepository.existsById(workItemId)) {
            return false;
        }
        return backlogRepository.addToBacklog(workItemId);
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
        if (!itemRepository.existsById(workItemId)) {
            return false;
        }
        return backlogRepository.addToBacklog(workItemId, username);
    }
    
    /**
     * Removes a work item from the current user's backlog.
     *
     * @param workItemId the ID of the work item to remove
     * @return true if the item was removed successfully
     */
    @Override
    public boolean removeFromBacklog(UUID workItemId) {
        return backlogRepository.removeFromBacklog(workItemId);
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
        return backlogRepository.moveInBacklog(workItemId, position);
    }
    
    /**
     * Moves a work item up one position in the current user's backlog.
     *
     * @param workItemId the ID of the work item to move
     * @return true if the item was moved successfully
     */
    @Override
    public boolean moveUp(UUID workItemId) {
        List<UUID> backlog = backlogRepository.getBacklog();
        int currentIndex = backlog.indexOf(workItemId);
        
        if (currentIndex <= 0) {
            // Already at top or not in backlog
            return false;
        }
        
        return backlogRepository.moveInBacklog(workItemId, currentIndex - 1);
    }
    
    /**
     * Moves a work item down one position in the current user's backlog.
     *
     * @param workItemId the ID of the work item to move
     * @return true if the item was moved successfully
     */
    @Override
    public boolean moveDown(UUID workItemId) {
        List<UUID> backlog = backlogRepository.getBacklog();
        int currentIndex = backlog.indexOf(workItemId);
        
        if (currentIndex == -1 || currentIndex >= backlog.size() - 1) {
            // Not in backlog or already at bottom
            return false;
        }
        
        return backlogRepository.moveInBacklog(workItemId, currentIndex + 1);
    }
    
    /**
     * Moves a work item to the top of the current user's backlog.
     *
     * @param workItemId the ID of the work item to move
     * @return true if the item was moved successfully
     */
    @Override
    public boolean moveToTop(UUID workItemId) {
        List<UUID> backlog = backlogRepository.getBacklog();
        
        if (!backlog.contains(workItemId)) {
            // Not in backlog
            return false;
        }
        
        return backlogRepository.moveInBacklog(workItemId, 0);
    }
    
    /**
     * Moves a work item to the bottom of the current user's backlog.
     *
     * @param workItemId the ID of the work item to move
     * @return true if the item was moved successfully
     */
    @Override
    public boolean moveToBottom(UUID workItemId) {
        List<UUID> backlog = backlogRepository.getBacklog();
        
        if (!backlog.contains(workItemId)) {
            // Not in backlog
            return false;
        }
        
        return backlogRepository.moveInBacklog(workItemId, backlog.size() - 1);
    }
}