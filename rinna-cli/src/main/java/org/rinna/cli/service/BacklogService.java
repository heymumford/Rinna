/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.rinna.cli.model.WorkItem;

/**
 * Service interface for backlog management operations.
 * This is a CLI-specific interface.
 */
public interface BacklogService {
    
    /**
     * Gets the backlog for a user.
     *
     * @param user the user name
     * @return the backlog items
     */
    List<WorkItem> getBacklog(String user);
    
    /**
     * Gets a specific section of a user's backlog.
     *
     * @param user the user name
     * @param section the backlog section (e.g., "todo", "ready", "backlog")
     * @return the backlog items in the section
     */
    List<WorkItem> getBacklogSection(String user, String section);
    
    /**
     * Adds an item to a user's backlog.
     *
     * @param user the user name
     * @param workItem the work item to add
     * @return true if successful, false otherwise
     */
    boolean addToBacklog(String user, WorkItem workItem);
    
    /**
     * Adds an item to a specific section of a user's backlog.
     *
     * @param user the user name
     * @param section the backlog section
     * @param workItem the work item to add
     * @return true if successful, false otherwise
     */
    boolean addToBacklogSection(String user, String section, WorkItem workItem);
    
    /**
     * Removes an item from a user's backlog.
     *
     * @param user the user name
     * @param itemId the work item ID
     * @return true if successful, false otherwise
     */
    boolean removeFromBacklog(String user, String itemId);
    
    /**
     * Moves a work item within the backlog.
     *
     * @param workItemId the work item ID
     * @param position the new position
     * @return true if successful, false otherwise
     */
    boolean moveInBacklog(UUID workItemId, int position);
    
    /**
     * Prioritizes a work item in the backlog.
     *
     * @param workItemId the work item ID
     * @param priority the priority level (higher is more important)
     * @return true if successful, false otherwise
     */
    boolean prioritizeInBacklog(UUID workItemId, int priority);
    
    /**
     * Gets all backlog items for all users.
     *
     * @return a map of user names to backlog items
     */
    Map<String, List<WorkItem>> getAllBacklogs();
    
    /**
     * Creates a new backlog for a user.
     *
     * @param user the user name
     * @return true if successful, false otherwise
     */
    boolean createBacklog(String user);
    
    /**
     * Gets the total number of items in a user's backlog.
     *
     * @param user the user name
     * @return the number of items
     */
    int getBacklogSize(String user);
    
    /**
     * Moves an item from one backlog section to another.
     *
     * @param user the user name
     * @param itemId the work item ID
     * @param fromSection the source section
     * @param toSection the target section
     * @return true if successful, false otherwise
     */
    boolean moveToSection(String user, UUID itemId, String fromSection, String toSection);
}