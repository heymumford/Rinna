/*
 * BacklogRepository - Repository interface for managing developer backlogs
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository interface for managing developer backlogs.
 */
public interface BacklogRepository {

    /**
     * Gets a user's backlog.
     *
     * @param username the username
     * @return a list of work item IDs in the backlog, in priority order
     */
    List<UUID> getBacklog(String username);
    
    /**
     * Sets a user's backlog.
     *
     * @param username the username
     * @param backlog the list of work item IDs, in priority order
     * @return true if the backlog was updated successfully
     */
    boolean setBacklog(String username, List<UUID> backlog);
    
    /**
     * Adds a work item to a user's backlog.
     *
     * @param username the username
     * @param workItemId the ID of the work item to add
     * @return true if the item was added successfully
     */
    boolean addToBacklog(String username, UUID workItemId);
    
    /**
     * Removes a work item from a user's backlog.
     *
     * @param username the username
     * @param workItemId the ID of the work item to remove
     * @return true if the item was removed successfully
     */
    boolean removeFromBacklog(String username, UUID workItemId);
    
    /**
     * Gets the position of a work item in a user's backlog.
     *
     * @param username the username
     * @param workItemId the ID of the work item
     * @return the position (0-based index), or -1 if the item is not in the backlog
     */
    int getPositionInBacklog(String username, UUID workItemId);
}