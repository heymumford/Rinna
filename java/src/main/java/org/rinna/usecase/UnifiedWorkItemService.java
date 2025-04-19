/*
 * Domain service interface for the Rinna unified work management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.usecase;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.OriginCategory;
import org.rinna.domain.model.UnifiedWorkItem;
import org.rinna.domain.model.WorkParadigm;
import org.rinna.domain.model.WorkflowState;

/**
 * Service interface for managing unified work items.
 * This interface defines the application use cases for the unified work management system.
 */
public interface UnifiedWorkItemService {
    /**
     * Creates a new unified work item.
     * 
     * @param builder a builder containing the work item properties
     * @return the created unified work item
     */
    UnifiedWorkItem create(UnifiedWorkItem.Builder builder);
    
    /**
     * Finds a unified work item by its ID.
     * 
     * @param id the ID of the unified work item
     * @return an Optional containing the unified work item, or empty if not found
     */
    Optional<UnifiedWorkItem> findById(UUID id);
    
    /**
     * Finds all unified work items.
     * 
     * @return a list of all unified work items
     */
    List<UnifiedWorkItem> findAll();
    
    /**
     * Finds unified work items with a title containing the specified text.
     * 
     * @param title the title text to search for
     * @return a list of unified work items with matching titles
     */
    List<UnifiedWorkItem> findByTitleContaining(String title);
    
    /**
     * Finds unified work items by their workflow state.
     * 
     * @param state the workflow state to filter by
     * @return a list of unified work items in the specified state
     */
    List<UnifiedWorkItem> findByState(WorkflowState state);
    
    /**
     * Finds unified work items by their assignee.
     * 
     * @param assignee the assignee to filter by
     * @return a list of unified work items assigned to the specified person
     */
    List<UnifiedWorkItem> findByAssignee(String assignee);
    
    /**
     * Finds unified work items by their origin category.
     * 
     * @param category the origin category to filter by
     * @return a list of unified work items in the specified category
     */
    List<UnifiedWorkItem> findByOriginCategory(OriginCategory category);
    
    /**
     * Finds unified work items by their Cynefin domain.
     * 
     * @param domain the Cynefin domain to filter by
     * @return a list of unified work items in the specified domain
     */
    List<UnifiedWorkItem> findByCynefinDomain(CynefinDomain domain);
    
    /**
     * Finds unified work items by their work paradigm.
     * 
     * @param paradigm the work paradigm to filter by
     * @return a list of unified work items with the specified paradigm
     */
    List<UnifiedWorkItem> findByWorkParadigm(WorkParadigm paradigm);
    
    /**
     * Finds unified work items that have any of the specified tags.
     * 
     * @param tags the list of tags to search for
     * @return a list of unified work items with any of the specified tags
     */
    List<UnifiedWorkItem> findByAnyTag(List<String> tags);
    
    /**
     * Finds unified work items that have all of the specified tags.
     * 
     * @param tags the list of tags to search for
     * @return a list of unified work items with all of the specified tags
     */
    List<UnifiedWorkItem> findByAllTags(List<String> tags);
    
    /**
     * Updates the assignee of a unified work item.
     * 
     * @param id the ID of the unified work item
     * @param assignee the new assignee
     * @return the updated unified work item
     * @throws IllegalArgumentException if the work item is not found
     */
    UnifiedWorkItem updateAssignee(UUID id, String assignee);
    
    /**
     * Updates the state of a unified work item.
     * 
     * @param id the ID of the unified work item
     * @param state the new state
     * @return the updated unified work item
     * @throws IllegalArgumentException if the work item is not found
     * @throws InvalidTransitionException if the state transition is not allowed
     */
    UnifiedWorkItem updateState(UUID id, WorkflowState state);
    
    /**
     * Updates the origin category of a unified work item.
     * 
     * @param id the ID of the unified work item
     * @param category the new origin category
     * @return the updated unified work item
     * @throws IllegalArgumentException if the work item is not found
     */
    UnifiedWorkItem updateOriginCategory(UUID id, OriginCategory category);
    
    /**
     * Updates the Cynefin domain of a unified work item.
     * 
     * @param id the ID of the unified work item
     * @param domain the new Cynefin domain
     * @return the updated unified work item
     * @throws IllegalArgumentException if the work item is not found
     */
    UnifiedWorkItem updateCynefinDomain(UUID id, CynefinDomain domain);
    
    /**
     * Updates the work paradigm of a unified work item.
     * 
     * @param id the ID of the unified work item
     * @param paradigm the new work paradigm
     * @return the updated unified work item
     * @throws IllegalArgumentException if the work item is not found
     */
    UnifiedWorkItem updateWorkParadigm(UUID id, WorkParadigm paradigm);
    
    /**
     * Updates the cognitive load estimate of a unified work item.
     * 
     * @param id the ID of the unified work item
     * @param cognitiveLoad the new cognitive load estimate (1-10)
     * @return the updated unified work item
     * @throws IllegalArgumentException if the work item is not found or the load is out of range
     */
    UnifiedWorkItem updateCognitiveLoad(UUID id, int cognitiveLoad);
    
    /**
     * Updates the tags of a unified work item.
     * 
     * @param id the ID of the unified work item
     * @param tags the new list of tags
     * @return the updated unified work item
     * @throws IllegalArgumentException if the work item is not found
     */
    UnifiedWorkItem updateTags(UUID id, List<String> tags);
    
    /**
     * Adds tags to a unified work item.
     * 
     * @param id the ID of the unified work item
     * @param tagsToAdd the tags to add
     * @return the updated unified work item
     * @throws IllegalArgumentException if the work item is not found
     */
    UnifiedWorkItem addTags(UUID id, List<String> tagsToAdd);
    
    /**
     * Removes tags from a unified work item.
     * 
     * @param id the ID of the unified work item
     * @param tagsToRemove the tags to remove
     * @return the updated unified work item
     * @throws IllegalArgumentException if the work item is not found
     */
    UnifiedWorkItem removeTags(UUID id, List<String> tagsToRemove);
    
    /**
     * Updates the metadata of a unified work item.
     * 
     * @param id the ID of the unified work item
     * @param metadata the new metadata
     * @return the updated unified work item
     * @throws IllegalArgumentException if the work item is not found
     */
    UnifiedWorkItem updateMetadata(UUID id, Map<String, String> metadata);
    
    /**
     * Adds or updates a metadata entry for a unified work item.
     * 
     * @param id the ID of the unified work item
     * @param key the metadata key
     * @param value the metadata value
     * @return the updated unified work item
     * @throws IllegalArgumentException if the work item is not found
     */
    UnifiedWorkItem addMetadata(UUID id, String key, String value);
    
    /**
     * Removes a metadata entry from a unified work item.
     * 
     * @param id the ID of the unified work item
     * @param key the metadata key to remove
     * @return the updated unified work item
     * @throws IllegalArgumentException if the work item is not found
     */
    UnifiedWorkItem removeMetadata(UUID id, String key);
    
    /**
     * Adds a dependency to a unified work item.
     * 
     * @param id the ID of the unified work item
     * @param dependencyId the ID of the dependency work item
     * @return the updated unified work item
     * @throws IllegalArgumentException if either work item is not found
     */
    UnifiedWorkItem addDependency(UUID id, UUID dependencyId);
    
    /**
     * Removes a dependency from a unified work item.
     * 
     * @param id the ID of the unified work item
     * @param dependencyId the ID of the dependency work item to remove
     * @return the updated unified work item
     * @throws IllegalArgumentException if the work item is not found
     */
    UnifiedWorkItem removeDependency(UUID id, UUID dependencyId);
    
    /**
     * Deletes a unified work item by its ID.
     * 
     * @param id the ID of the unified work item to delete
     * @throws IllegalArgumentException if the work item is not found
     */
    void deleteById(UUID id);
    
    /**
     * Gets all dependencies of a unified work item.
     * 
     * @param id the ID of the unified work item
     * @return a list of the work item's dependencies
     * @throws IllegalArgumentException if the work item is not found
     */
    List<UnifiedWorkItem> getDependencies(UUID id);
    
    /**
     * Gets all work items that depend on the specified work item.
     * 
     * @param id the ID of the unified work item
     * @return a list of work items that depend on the specified work item
     * @throws IllegalArgumentException if the work item is not found
     */
    List<UnifiedWorkItem> getDependents(UUID id);
    
    /**
     * Calculates the critical path for a set of work items.
     * 
     * @param workItems the list of work items to analyze
     * @return the critical path as an ordered list of work items
     */
    List<UnifiedWorkItem> calculateCriticalPath(List<UnifiedWorkItem> workItems);
    
    /**
     * Gets a cognitive load report for the specified assignee.
     * Sums up the cognitive load of all work items assigned to the person.
     * 
     * @param assignee the assignee to check
     * @return the total cognitive load and a list of contributing work items
     */
    Map<String, Object> getCognitiveLoadReport(String assignee);
    
    /**
     * Gets distribution statistics of work items by category, domain, and paradigm.
     * 
     * @return a map containing distribution statistics
     */
    Map<String, Map<String, Integer>> getWorkItemDistribution();
}