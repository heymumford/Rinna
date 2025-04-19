/*
 * Domain entity for the Rinna workflow management system - Ryorin-do v0.2 enhancement
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Interface for adding Ryorin-do v0.2 functionality to WorkItems.
 * This follows the Decorator pattern to extend WorkItem capabilities
 * without modifying the existing interface.
 */
public interface RyorindoWorkItemEnhancer {
    
    /**
     * Returns the WorkItem being enhanced.
     * 
     * @return the WorkItem
     */
    WorkItem getWorkItem();
    
    /**
     * Returns the CYNEFIN domain classification for this work item.
     * 
     * @return an Optional containing the CYNEFIN domain, or empty if not classified
     */
    default Optional<CynefinDomain> getCynefinDomain() {
        // In a real implementation, this would retrieve from metadata or a database
        return Optional.empty();
    }
    
    /**
     * Returns the work paradigm for this work item.
     * 
     * @return an Optional containing the work paradigm, or empty if not specified
     */
    default Optional<WorkParadigm> getWorkParadigm() {
        // In a real implementation, this would retrieve from metadata or a database
        return Optional.empty();
    }
    
    /**
     * Returns the list of assignees for this work item.
     * 
     * @return the list of assignees
     */
    default List<String> getAssignees() {
        String assignee = getWorkItem().getAssignee();
        return assignee != null ? List.of(assignee) : Collections.emptyList();
    }
    
    /**
     * Returns the due date for this work item.
     * 
     * @return an Optional containing the due date, or empty if not specified
     */
    default Optional<Instant> getDueDate() {
        // In a real implementation, this would retrieve from metadata or a database
        return Optional.empty();
    }
    
    /**
     * Returns the estimated effort for this work item.
     * 
     * @return an Optional containing the estimated effort, or empty if not specified
     */
    default Optional<Double> getEstimatedEffort() {
        // In a real implementation, this would retrieve from metadata or a database
        return Optional.empty();
    }
    
    /**
     * Returns the actual effort for this work item.
     * 
     * @return an Optional containing the actual effort, or empty if not tracked
     */
    default Optional<Double> getActualEffort() {
        // In a real implementation, this would retrieve from metadata or a database
        return Optional.empty();
    }
    
    /**
     * Returns the desired outcome for this work item.
     * 
     * @return an Optional containing the outcome, or empty if not specified
     */
    default Optional<String> getOutcome() {
        // In a real implementation, this would retrieve from metadata or a database
        return Optional.empty();
    }
    
    /**
     * Returns the key results for measuring success of this work item.
     * 
     * @return the list of key results
     */
    default List<String> getKeyResults() {
        // In a real implementation, this would retrieve from metadata or a database
        return Collections.emptyList();
    }
    
    /**
     * Returns the work items that this item depends on.
     * 
     * @return the list of dependency IDs
     */
    default List<UUID> getDependencies() {
        // In a real implementation, this would retrieve from relationships or a database
        return Collections.emptyList();
    }
    
    /**
     * Returns the related work items.
     * 
     * @return the list of related item IDs
     */
    default List<UUID> getRelatedItems() {
        // In a real implementation, this would retrieve from relationships or a database
        return Collections.emptyList();
    }
    
    /**
     * Returns the cognitive load assessment for this work item.
     * 
     * @return an Optional containing the cognitive load (1-10), or empty if not assessed
     */
    default Optional<Integer> getCognitiveLoadAssessment() {
        // In a real implementation, this would retrieve from metadata or a database
        return Optional.empty();
    }
    
    /**
     * Returns AI-generated recommendations for this work item.
     * 
     * @return the list of recommendations
     */
    default List<String> getAiRecommendations() {
        // In a real implementation, this would retrieve from metadata or a database
        return Collections.emptyList();
    }
    
    /**
     * Returns knowledge links relevant to this work item.
     * 
     * @return the list of knowledge links
     */
    default List<String> getKnowledgeLinks() {
        // In a real implementation, this would retrieve from metadata or a database
        return Collections.emptyList();
    }
    
    /**
     * Returns file attachments for this work item.
     * 
     * @return the list of attachment references
     */
    default List<String> getAttachments() {
        // In a real implementation, this would retrieve from metadata or a database
        return Collections.emptyList();
    }
    
    /**
     * Returns the workstream IDs this work item belongs to.
     * 
     * @return the list of workstream IDs
     */
    default List<UUID> getWorkstreamIds() {
        // In a real implementation, this would retrieve from relationships or a database
        return Collections.emptyList();
    }
    
    /**
     * Returns the allocation percentage for this work item.
     * 
     * @return an Optional containing the allocation percentage (0-100), or empty if not specified
     */
    default Optional<Integer> getAllocation() {
        // In a real implementation, this would retrieve from metadata or a database
        return Optional.empty();
    }
    
    /**
     * Returns the category of this work item.
     * 
     * @return an Optional containing the category, or empty if not categorized
     */
    default Optional<String> getCategory() {
        // In a real implementation, this would retrieve from metadata or a database
        return Optional.empty();
    }
    
    /**
     * Returns the recommended approach for handling this work item
     * based on its CYNEFIN domain.
     * 
     * @return an Optional containing the recommended approach, or empty if no domain is set
     */
    default Optional<String> getRecommendedApproach() {
        return getCynefinDomain().map(CynefinDomain::getRecommendedApproach);
    }
    
    /**
     * Returns whether this work item requires expert analysis
     * based on its CYNEFIN domain.
     * 
     * @return true if expert analysis is recommended, false otherwise
     */
    default boolean requiresExpertAnalysis() {
        return getCynefinDomain()
            .map(CynefinDomain::requiresExpertAnalysis)
            .orElse(false);
    }
    
    /**
     * Returns whether this work item requires experimentation
     * based on its CYNEFIN domain.
     * 
     * @return true if experimentation is recommended, false otherwise
     */
    default boolean requiresExperimentation() {
        return getCynefinDomain()
            .map(CynefinDomain::requiresExperimentation)
            .orElse(false);
    }
    
    /**
     * Returns whether this work item requires immediate action
     * based on its CYNEFIN domain.
     * 
     * @return true if immediate action is recommended, false otherwise
     */
    default boolean requiresImmediateAction() {
        return getCynefinDomain()
            .map(CynefinDomain::requiresImmediateAction)
            .orElse(false);
    }
}