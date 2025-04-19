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
import java.util.UUID;

import org.rinna.domain.model.WorkItemRelationshipType;

/**
 * Interface for relationship services.
 */
public interface RelationshipService {
    
    /**
     * Gets the parent work item ID for a given work item.
     *
     * @param workItemId the work item ID
     * @return the parent work item ID, or null if there is no parent
     */
    UUID getParentWorkItem(UUID workItemId);
    
    /**
     * Gets the child work item IDs for a given work item.
     *
     * @param workItemId the work item ID
     * @return a list of child work item IDs
     */
    List<UUID> getChildWorkItems(UUID workItemId);
    
    /**
     * Gets the type of relationship between two work items.
     *
     * @param childId the child work item ID
     * @param parentId the parent work item ID
     * @return the relationship type
     */
    WorkItemRelationshipType getRelationshipType(UUID childId, UUID parentId);
    
    /**
     * Sets a parent-child relationship between two work items.
     *
     * @param childId the child work item ID
     * @param parentId the parent work item ID
     * @param relationType the relationship type
     * @return true if the relationship was set successfully
     */
    boolean setRelationship(UUID childId, UUID parentId, WorkItemRelationshipType relationType);
    
    /**
     * Removes a parent-child relationship between two work items.
     *
     * @param childId the child work item ID
     * @param parentId the parent work item ID
     * @return true if the relationship was removed successfully
     */
    boolean removeRelationship(UUID childId, UUID parentId);
}