/*
 * Domain service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.service;

import org.rinna.domain.model.WorkItem;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for identifying critical paths in projects.
 * 
 * <p>This interface defines the application use cases for critical path analysis, which is
 * a project management technique used to identify and manage the sequence of dependent tasks
 * that determine the minimum time needed to complete a project.</p>
 * 
 * <p>Critical path analysis helps identify:</p>
 * <ul>
 *   <li>Which tasks must be completed on time to ensure the project finishes on schedule</li>
 *   <li>Which tasks can be delayed without affecting the project timeline</li>
 *   <li>The shortest possible duration for the project</li>
 *   <li>Bottlenecks and blockers in the workflow</li>
 * </ul>
 * 
 * <p>This service manages dependencies between work items and provides methods for
 * analyzing the dependency graph to find critical paths and blocking items.</p>
 * 
 * @author Eric C. Mumford
 * @since 1.2
 */
public interface CriticalPathService {
    
    /**
     * Finds items that block the progress of other items in the project.
     * 
     * <p>These are items that have multiple dependents and are not completed. Blocking items
     * represent bottlenecks in the workflow that, if resolved, could unblock multiple other
     * tasks. This analysis is particularly useful for prioritizing work and identifying
     * potential project risks.</p>
     * 
     * <p>The returned list is sorted by the number of dependent items, with the most
     * critical blockers appearing first.</p>
     * 
     * @return a list of work items that are blocking others, sorted by criticality
     * @see #findItemsDependingOn(UUID)
     */
    List<WorkItem> findBlockingItems();
    
    /**
     * Finds the critical path for the project.
     * 
     * <p>The critical path is the sequence of dependent work items that determines
     * the minimum time needed to complete the project. Any delay in completing
     * an item on the critical path will delay the entire project.</p>
     * 
     * <p>This method uses the project's current dependency graph and estimated
     * completion times to calculate the longest path through the project network,
     * which represents the critical path.</p>
     * 
     * <p>The returned list contains the work items in the order they appear
     * on the critical path, from project start to completion.</p>
     * 
     * @return a list of work items representing the critical path, in order
     */
    List<WorkItem> findCriticalPath();
    
    /**
     * Finds work items that depend on the specified work item.
     * 
     * <p>This method returns all items that are directly dependent on the specified
     * work item. These items cannot proceed until the specified item is completed.</p>
     * 
     * <p>This is useful for understanding the impact of delays or for planning
     * the order in which work should be completed.</p>
     * 
     * @param itemId the ID of the work item to find dependents for
     * @return a list of work items that directly depend on the specified item
     * @see #hasDependency(UUID, UUID)
     */
    List<WorkItem> findItemsDependingOn(UUID itemId);
    
    /**
     * Adds a dependency between two work items.
     * 
     * <p>After this call, the dependent item will be considered to depend on the blocker item,
     * meaning the dependent item cannot be completed until the blocker item is completed.</p>
     * 
     * <p>This method validates that adding this dependency wouldn't create a circular
     * dependency chain, which would make it impossible to complete the project.</p>
     * 
     * @param dependentId the ID of the dependent work item that needs the blocker completed
     * @param blockerId the ID of the blocker work item that must be completed first
     * @return true if the dependency was added, false if it already existed or would create a cycle
     * @throws IllegalArgumentException if either itemId does not exist
     */
    boolean addDependency(UUID dependentId, UUID blockerId);
    
    /**
     * Removes a dependency between two work items.
     * 
     * <p>This method removes the requirement that the dependent item cannot
     * be completed until the blocker item is completed. After this call,
     * the items can be completed independently of each other.</p>
     * 
     * <p>Removing dependencies might alter the critical path and should be
     * done carefully, typically when requirements change or when initial
     * dependency assumptions proved incorrect.</p>
     * 
     * @param dependentId the ID of the dependent work item
     * @param blockerId the ID of the blocker work item
     * @return true if the dependency was removed, false if it didn't exist
     * @throws IllegalArgumentException if either itemId does not exist
     */
    boolean removeDependency(UUID dependentId, UUID blockerId);
    
    /**
     * Checks if one work item depends on another.
     * 
     * <p>This method determines whether there is a direct dependency between
     * the two specified work items, where the dependent item cannot proceed
     * until the blocker item is completed.</p>
     * 
     * <p>Note that this method only checks for direct dependencies. To find
     * indirect dependencies (dependencies through other items), you would need
     * to traverse the dependency graph.</p>
     * 
     * @param dependentId the ID of the potentially dependent work item
     * @param blockerId the ID of the potentially blocking work item
     * @return true if the dependent item directly depends on the blocker item
     * @throws IllegalArgumentException if either itemId does not exist
     */
    boolean hasDependency(UUID dependentId, UUID blockerId);
}