/*
 * DefaultBacklogService - Default implementation of BacklogService
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.WorkItem;
import org.rinna.repository.BacklogRepository;
import org.rinna.repository.ItemRepository;
import org.rinna.usecase.BacklogService;

/**
 * Default implementation of the BacklogService interface.
 */
public class DefaultBacklogService implements BacklogService {

    private final BacklogRepository backlogRepository;
    private final ItemRepository itemRepository;
    private final UserContextProvider userContextProvider;
    
    /**
     * Creates a new DefaultBacklogService with the given repositories.
     *
     * @param backlogRepository the repository for backlogs
     * @param itemRepository the repository for work items
     * @param userContextProvider the provider for the current user context
     */
    public DefaultBacklogService(BacklogRepository backlogRepository, 
                             ItemRepository itemRepository,
                             UserContextProvider userContextProvider) {
        this.backlogRepository = backlogRepository;
        this.itemRepository = itemRepository;
        this.userContextProvider = userContextProvider;
    }
    
    @Override
    public List<WorkItem> getBacklog() {
        return getBacklog(userContextProvider.getCurrentUsername());
    }
    
    @Override
    public List<WorkItem> getBacklog(String username) {
        List<UUID> backlogIds = backlogRepository.getBacklog(username);
        List<WorkItem> backlog = new ArrayList<>();
        
        for (UUID id : backlogIds) {
            Optional<WorkItem> item = itemRepository.findById(id);
            if (item.isPresent()) {
                backlog.add(item.get());
            }
        }
        
        return backlog;
    }
    
    @Override
    public boolean addToBacklog(UUID workItemId) {
        return addToBacklog(workItemId, userContextProvider.getCurrentUsername());
    }
    
    @Override
    public boolean addToBacklog(UUID workItemId, String username) {
        // Verify that the work item exists
        Optional<WorkItem> item = itemRepository.findById(workItemId);
        if (item.isEmpty()) {
            return false;
        }
        
        return backlogRepository.addToBacklog(username, workItemId);
    }
    
    @Override
    public boolean removeFromBacklog(UUID workItemId) {
        return backlogRepository.removeFromBacklog(
            userContextProvider.getCurrentUsername(), workItemId);
    }
    
    @Override
    public boolean moveInBacklog(UUID workItemId, int position) {
        String username = userContextProvider.getCurrentUsername();
        List<UUID> backlog = backlogRepository.getBacklog(username);
        
        // Check if the item is in the backlog
        int currentPosition = backlog.indexOf(workItemId);
        if (currentPosition == -1) {
            return false;
        }
        
        // Check if the position is valid
        if (position < 0 || position >= backlog.size()) {
            return false;
        }
        
        // Remove the item from its current position
        backlog.remove(currentPosition);
        
        // Insert it at the new position
        backlog.add(position, workItemId);
        
        // Update the backlog
        return backlogRepository.setBacklog(username, backlog);
    }
    
    @Override
    public boolean moveUp(UUID workItemId) {
        String username = userContextProvider.getCurrentUsername();
        int currentPosition = backlogRepository.getPositionInBacklog(username, workItemId);
        
        // Check if the item is in the backlog
        if (currentPosition == -1) {
            return false;
        }
        
        // Check if the item is already at the top
        if (currentPosition == 0) {
            return false;
        }
        
        // Move the item up one position
        return moveInBacklog(workItemId, currentPosition - 1);
    }
    
    @Override
    public boolean moveDown(UUID workItemId) {
        String username = userContextProvider.getCurrentUsername();
        List<UUID> backlog = backlogRepository.getBacklog(username);
        int currentPosition = backlogRepository.getPositionInBacklog(username, workItemId);
        
        // Check if the item is in the backlog
        if (currentPosition == -1) {
            return false;
        }
        
        // Check if the item is already at the bottom
        if (currentPosition == backlog.size() - 1) {
            return false;
        }
        
        // Move the item down one position
        return moveInBacklog(workItemId, currentPosition + 1);
    }
    
    @Override
    public boolean moveToTop(UUID workItemId) {
        String username = userContextProvider.getCurrentUsername();
        int currentPosition = backlogRepository.getPositionInBacklog(username, workItemId);
        
        // Check if the item is in the backlog
        if (currentPosition == -1) {
            return false;
        }
        
        // Check if the item is already at the top
        if (currentPosition == 0) {
            return false;
        }
        
        // Move the item to the top
        return moveInBacklog(workItemId, 0);
    }
    
    @Override
    public boolean moveToBottom(UUID workItemId) {
        String username = userContextProvider.getCurrentUsername();
        List<UUID> backlog = backlogRepository.getBacklog(username);
        int currentPosition = backlogRepository.getPositionInBacklog(username, workItemId);
        
        // Check if the item is in the backlog
        if (currentPosition == -1) {
            return false;
        }
        
        // Check if the item is already at the bottom
        if (currentPosition == backlog.size() - 1) {
            return false;
        }
        
        // Move the item to the bottom
        return moveInBacklog(workItemId, backlog.size() - 1);
    }
    
    /**
     * Provider interface for the current user context.
     */
    public interface UserContextProvider {
        /**
         * Gets the current username.
         *
         * @return the current username
         */
        String getCurrentUsername();
    }
}