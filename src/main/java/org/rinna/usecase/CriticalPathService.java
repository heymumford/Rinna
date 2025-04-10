/*
 * CriticalPathService - Service interface for critical path operations
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.usecase;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemDependency;

/**
 * Service interface for managing work item dependencies and critical path operations.
 * The critical path is the sequence of dependent tasks that determine the minimum time needed to complete the project.
 */
public interface CriticalPathService {

    /**
     * Adds a dependency between two work items.
     *
     * @param dependentId the ID of the work item that depends on another
     * @param dependencyId the ID of the work item that must be completed first
     * @param dependencyType the type of dependency (e.g., "BLOCKS", "RELATES_TO")
     * @param createdBy the user who is creating the dependency
     * @return the created dependency
     * @throws IllegalArgumentException if either work item does not exist
     * @throws IllegalStateException if the dependency would create a cycle
     */
    WorkItemDependency addDependency(UUID dependentId, UUID dependencyId, String dependencyType, String createdBy);
    
    /**
     * Removes a dependency between two work items.
     *
     * @param dependentId the ID of the work item that depends on another
     * @param dependencyId the ID of the work item that is depended upon
     * @return true if the dependency was removed, false if it didn't exist
     */
    boolean removeDependency(UUID dependentId, UUID dependencyId);
    
    /**
     * Gets all dependencies for a work item.
     *
     * @param workItemId the ID of the work item
     * @param direction "incoming" for dependencies this item depends on, "outgoing" for items that depend on this
     * @return the list of dependencies
     */
    List<WorkItemDependency> getDependencies(UUID workItemId, String direction);
    
    /**
     * Calculates the critical path for the project.
     * The critical path is the sequence of dependent tasks that determine the minimum time needed
     * to complete the project.
     *
     * @return the list of work items in the critical path, in order
     */
    List<WorkItem> calculateCriticalPath();
    
    /**
     * Calculates the critical path for a specific work item.
     * This returns the critical path from the project start to the specified work item.
     *
     * @param workItemId the ID of the target work item
     * @return the list of work items in the critical path to this item, in order
     */
    List<WorkItem> calculateCriticalPathTo(UUID workItemId);
    
    /**
     * Identifies all blockers in the current project.
     * A blocker is a work item on the critical path that is currently impeded.
     *
     * @return a list of blocked work items on the critical path
     */
    List<WorkItem> identifyBlockers();
    
    /**
     * Marks a work item as blocked, which affects the critical path.
     *
     * @param workItemId the ID of the work item to mark as blocked
     * @param reason the reason for the blockage
     * @param blockedBy the ID of the user or entity causing the blockage (optional)
     * @return the updated work item
     */
    WorkItem markAsBlocked(UUID workItemId, String reason, String blockedBy);
    
    /**
     * Marks a work item as unblocked.
     *
     * @param workItemId the ID of the work item to mark as unblocked
     * @return the updated work item
     */
    WorkItem markAsUnblocked(UUID workItemId);
    
    /**
     * Gets the estimated completion dates for the project based on the critical path.
     *
     * @return a map of milestone work item IDs to their estimated completion dates
     */
    Map<UUID, java.time.LocalDate> getEstimatedCompletionDates();
    
    /**
     * Identifies parallel work paths in the project that can be worked on simultaneously.
     *
     * @return a list of lists, where each inner list represents a parallel path
     */
    List<List<WorkItem>> identifyParallelPaths();
    
    /**
     * Determines the impact of a delay on a specific work item.
     *
     * @param workItemId the ID of the work item that might be delayed
     * @param delayDays the number of days of delay
     * @return the list of work items that would be affected by the delay
     */
    List<WorkItem> calculateDelayImpact(UUID workItemId, int delayDays);
}