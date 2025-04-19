/*
 * Domain entity interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Enhanced work item interface that implements the Unified Work Management approach.
 * This interface extends the basic WorkItem with additional classification and 
 * relationship properties to support cross-cutting work management.
 */
public interface UnifiedWorkItem extends WorkItem {
    /**
     * Returns the origin category of this work item.
     * 
     * @return the origin category
     */
    OriginCategory getCategory();
    
    /**
     * Returns the CYNEFIN domain classification of this work item.
     * 
     * @return the CYNEFIN domain
     */
    CynefinDomain getCynefinDomain();
    
    /**
     * Returns the work paradigm for this work item.
     * 
     * @return the work paradigm
     */
    WorkParadigm getWorkParadigm();
    
    /**
     * Returns the cognitive load assessment value for this work item.
     * The cognitive load is typically a value between 1-10 indicating the
     * mental effort required to understand and complete the work.
     * 
     * @return an Optional containing the cognitive load value, or empty if not assessed
     */
    Optional<Integer> getCognitiveLoad();
    
    /**
     * Returns the desired outcome of completing this work item.
     * 
     * @return an Optional containing the outcome description, or empty if not specified
     */
    Optional<String> getOutcome();
    
    /**
     * Returns the set of IDs for work items that are dependencies of this work item.
     * These are items that must be completed before this work item can be completed.
     * 
     * @return the set of dependency IDs
     */
    Set<UUID> getDependencies();
    
    /**
     * Returns the set of IDs for work items that are related to this work item.
     * Related items provide context but are not necessarily dependencies.
     * 
     * @return the set of related item IDs
     */
    Set<UUID> getRelatedItems();
    
    /**
     * Returns the release ID associated with this work item, if any.
     * 
     * @return an Optional containing the release ID, or empty if not associated with a release
     */
    Optional<UUID> getReleaseId();
    
    /**
     * Returns the custom metadata for this work item.
     * Metadata provides a flexible way to extend the work item model with
     * type-specific or domain-specific attributes.
     * 
     * @return the metadata as a map of key-value pairs
     */
    Map<String, String> getMetadata();
    
    /**
     * Returns the key of the project associated with this work item.
     * 
     * @return the project key
     */
    String getProjectKey();
    
    /**
     * Returns the timestamp when this work item is due, if specified.
     * 
     * @return an Optional containing the due date, or empty if not specified
     */
    Optional<Instant> getDueDate();
    
    /**
     * Returns the estimated effort required to complete this work item.
     * The unit of effort depends on the project configuration (e.g., hours, points).
     * 
     * @return an Optional containing the effort estimate, or empty if not estimated
     */
    Optional<Double> getEstimatedEffort();
    
    /**
     * Returns the actual effort spent on this work item.
     * The unit of effort depends on the project configuration (e.g., hours, points).
     * 
     * @return an Optional containing the actual effort, or empty if not recorded
     */
    Optional<Double> getActualEffort();
}