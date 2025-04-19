/*
 * DependencyRepository - Repository interface for work item dependencies
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.WorkItemDependency;

/**
 * Repository interface for work item dependencies.
 */
public interface DependencyRepository {

    /**
     * Finds a dependency by its ID.
     *
     * @param id the ID of the dependency
     * @return an Optional containing the dependency if found, or empty if not found
     */
    Optional<WorkItemDependency> findById(UUID id);
    
    /**
     * Saves a dependency.
     *
     * @param dependency the dependency to save
     * @return the saved dependency
     */
    WorkItemDependency save(WorkItemDependency dependency);
    
    /**
     * Removes a dependency between two work items.
     *
     * @param dependentId the ID of the dependent work item
     * @param dependencyId the ID of the dependency work item
     * @return true if the dependency was removed, false if it didn't exist
     */
    boolean remove(UUID dependentId, UUID dependencyId);
    
    /**
     * Finds all dependencies.
     *
     * @return a list of all dependencies
     */
    List<WorkItemDependency> findAll();
    
    /**
     * Finds all dependencies where the given work item is the dependent.
     * These are the items that the given work item depends on.
     *
     * @param workItemId the ID of the work item
     * @return a list of dependencies
     */
    List<WorkItemDependency> findIncomingDependencies(UUID workItemId);
    
    /**
     * Finds all dependencies where the given work item is the dependency.
     * These are the items that depend on the given work item.
     *
     * @param workItemId the ID of the work item
     * @return a list of dependencies
     */
    List<WorkItemDependency> findOutgoingDependencies(UUID workItemId);
    
    /**
     * Finds a dependency between two work items.
     *
     * @param dependentId the ID of the dependent work item
     * @param dependencyId the ID of the dependency work item
     * @return an Optional containing the dependency if found, or empty if not found
     */
    Optional<WorkItemDependency> findByWorkItems(UUID dependentId, UUID dependencyId);
}