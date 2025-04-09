/*
 * BacklogRepository - Repository interface for user backlogs
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing user backlogs.
 * A backlog is an ordered list of work items for a user.
 */
public interface BacklogRepository {
    
    /**
     * Gets the backlog for the current user.
     *
     * @return the list of work item IDs in the backlog, in priority order
     */
    List<UUID> getBacklog();
    
    /**
     * Gets the backlog for a specific user.
     *
     * @param username the username
     * @return the list of work item IDs in the backlog, in priority order
     */
    List<UUID> getBacklog(String username);
    
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
     * Removes a work item from a user's backlog.
     *
     * @param workItemId the ID of the work item to remove
     * @param username the username
     * @return true if the item was removed successfully
     */
    boolean removeFromBacklog(UUID workItemId, String username);
    
    /**
     * Moves a work item to the specified position in the current user's backlog.
     *
     * @param workItemId the ID of the work item to move
     * @param position the new position (0-based index)
     * @return true if the item was moved successfully
     */
    boolean moveInBacklog(UUID workItemId, int position);
    
    /**
     * Moves a work item to the specified position in a user's backlog.
     *
     * @param workItemId the ID of the work item to move
     * @param position the new position (0-based index)
     * @param username the username
     * @return true if the item was moved successfully
     */
    boolean moveInBacklog(UUID workItemId, int position, String username);
}