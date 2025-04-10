/*
 * Domain service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.service;

import java.util.List;
import java.util.UUID;

import org.rinna.domain.model.WorkItem;

/**
 * Service interface for identifying critical paths in projects.
 * This interface defines the application use cases for critical path analysis.
 */
public interface CriticalPathService {
    
    /**
     * Finds items that block the progress of other items in the project.
     * These are items that have multiple dependents and are not completed.
     * 
     * @return a list of work items that are blocking others
     */
    List<WorkItem> findBlockingItems();
    
    /**
     * Finds the critical path for the project.
     * The critical path is the sequence of dependent work items that determines
     * the minimum time needed to complete the project.
     * 
     * @return a list of work items representing the critical path, in order
     */
    List<WorkItem> findCriticalPath();
    
    /**
     * Finds work items that depend on the specified work item.
     * 
     * @param itemId the ID of the work item
     * @return a list of work items that depend on the specified item
     */
    List<WorkItem> findItemsDependingOn(UUID itemId);
    
    /**
     * Adds a dependency between two work items.
     * After this call, the dependent item will be considered to depend on the blocker item.
     * 
     * @param dependentId the ID of the dependent work item
     * @param blockerId the ID of the blocker work item
     * @return true if the dependency was added, false if it already existed
     */
    boolean addDependency(UUID dependentId, UUID blockerId);
    
    /**
     * Removes a dependency between two work items.
     * 
     * @param dependentId the ID of the dependent work item
     * @param blockerId the ID of the blocker work item
     * @return true if the dependency was removed, false if it didn't exist
     */
    boolean removeDependency(UUID dependentId, UUID blockerId);
    
    /**
     * Checks if one work item depends on another.
     * 
     * @param dependentId the ID of the potentially dependent work item
     * @param blockerId the ID of the potentially blocking work item
     * @return true if the dependent item depends on the blocker item
     */
    boolean hasDependency(UUID dependentId, UUID blockerId);
}