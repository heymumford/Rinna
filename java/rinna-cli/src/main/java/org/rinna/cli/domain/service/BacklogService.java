/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.domain.service;

import java.util.List;
import java.util.UUID;

import org.rinna.cli.domain.model.DomainWorkItem;

/**
 * Service interface for managing developer backlogs.
 * A backlog is a prioritized list of work items for a developer.
 */
public interface BacklogService {

    /**
     * Gets the current user's backlog.
     *
     * @return a list of work items in the backlog, in priority order
     */
    List<DomainWorkItem> getBacklog();
    
    /**
     * Gets a user's backlog.
     *
     * @param username the username
     * @return a list of work items in the backlog, in priority order
     */
    List<DomainWorkItem> getBacklog(String username);
    
    /**
     * Adds a work item to the current user's backlog.
     *
     * @param workItemId the ID of the work item to add
     * @return true if the item was added successfully
     */
    boolean addToBacklog(UUID workItemId);
    
    /**
     * Adds a work item to a user's backlog.
     *
     * @param workItemId the ID of the work item to add
     * @param username the username
     * @return true if the item was added successfully
     */
    boolean addToBacklog(UUID workItemId, String username);
    
    /**
     * Removes a work item from the current user's backlog.
     *
     * @param workItemId the ID of the work item to remove
     * @return true if the item was removed successfully
     */
    boolean removeFromBacklog(UUID workItemId);
    
    /**
     * Moves a work item to the specified position in the current user's backlog.
     *
     * @param workItemId the ID of the work item to move
     * @param position the new position (0-based index)
     * @return true if the item was moved successfully
     */
    boolean moveInBacklog(UUID workItemId, int position);
    
    /**
     * Moves a work item up one position in the current user's backlog.
     *
     * @param workItemId the ID of the work item to move
     * @return true if the item was moved successfully
     */
    boolean moveUp(UUID workItemId);
    
    /**
     * Moves a work item down one position in the current user's backlog.
     *
     * @param workItemId the ID of the work item to move
     * @return true if the item was moved successfully
     */
    boolean moveDown(UUID workItemId);
    
    /**
     * Moves a work item to the top of the current user's backlog.
     *
     * @param workItemId the ID of the work item to move
     * @return true if the item was moved successfully
     */
    boolean moveToTop(UUID workItemId);
    
    /**
     * Moves a work item to the bottom of the current user's backlog.
     *
     * @param workItemId the ID of the work item to move
     * @return true if the item was moved successfully
     */
    boolean moveToBottom(UUID workItemId);
}