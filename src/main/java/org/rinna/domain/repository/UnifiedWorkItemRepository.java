/*
 * Domain repository interface for the Rinna unified work management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.OriginCategory;
import org.rinna.domain.model.UnifiedWorkItem;
import org.rinna.domain.model.WorkParadigm;
import org.rinna.domain.model.WorkflowState;

/**
 * Repository interface for unified work items.
 * This interface defines the persistence operations for unified work items.
 * It follows the Repository pattern from Domain-Driven Design.
 */
public interface UnifiedWorkItemRepository {
    /**
     * Saves a unified work item.
     * 
     * @param item the unified work item to save
     * @return the saved unified work item
     */
    UnifiedWorkItem save(UnifiedWorkItem item);
    
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
     * Finds unified work items by their title (partial match).
     * 
     * @param title the title to search for
     * @return a list of unified work items with matching titles
     */
    List<UnifiedWorkItem> findByTitleContaining(String title);
    
    /**
     * Finds unified work items by their state.
     * 
     * @param state the state of unified work items to find
     * @return a list of unified work items with the given state
     */
    List<UnifiedWorkItem> findByState(WorkflowState state);
    
    /**
     * Finds unified work items by their assignee.
     * 
     * @param assignee the assignee of unified work items to find
     * @return a list of unified work items assigned to the given assignee
     */
    List<UnifiedWorkItem> findByAssignee(String assignee);
    
    /**
     * Finds unified work items by their origin category.
     * 
     * @param category the origin category to find
     * @return a list of unified work items with the given origin category
     */
    List<UnifiedWorkItem> findByOriginCategory(OriginCategory category);
    
    /**
     * Finds unified work items by their Cynefin domain.
     * 
     * @param domain the Cynefin domain to find
     * @return a list of unified work items with the given Cynefin domain
     */
    List<UnifiedWorkItem> findByCynefinDomain(CynefinDomain domain);
    
    /**
     * Finds unified work items by their work paradigm.
     * 
     * @param paradigm the work paradigm to find
     * @return a list of unified work items with the given work paradigm
     */
    List<UnifiedWorkItem> findByWorkParadigm(WorkParadigm paradigm);
    
    /**
     * Finds unified work items that have any of the provided tags.
     * 
     * @param tags the tags to search for
     * @return a list of unified work items with any of the given tags
     */
    List<UnifiedWorkItem> findByAnyTag(List<String> tags);
    
    /**
     * Finds unified work items that have all the provided tags.
     * 
     * @param tags the tags to search for
     * @return a list of unified work items with all the given tags
     */
    List<UnifiedWorkItem> findByAllTags(List<String> tags);
    
    /**
     * Finds unified work items with a cognitive load equal to or greater than the specified value.
     * 
     * @param minimumLoad the minimum cognitive load to search for
     * @return a list of unified work items with the minimum cognitive load
     */
    List<UnifiedWorkItem> findByCognitiveLoadGreaterThanEqual(int minimumLoad);
    
    /**
     * Finds unified work items with a cognitive load equal to or less than the specified value.
     * 
     * @param maximumLoad the maximum cognitive load to search for
     * @return a list of unified work items with the maximum cognitive load
     */
    List<UnifiedWorkItem> findByCognitiveLoadLessThanEqual(int maximumLoad);
    
    /**
     * Deletes a unified work item by its ID.
     * 
     * @param id the ID of the unified work item to delete
     */
    void deleteById(UUID id);
}