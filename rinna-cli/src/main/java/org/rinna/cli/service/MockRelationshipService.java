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

import org.rinna.cli.domain.model.WorkItemRelationshipType;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Mock implementation of relationship service functionality for CLI use.
 */
public class MockRelationshipService {
    
    // Maps a child ID to its parent ID
    private final Map<UUID, UUID> parentRelationships = new ConcurrentHashMap<>();
    
    // Maps a parent ID to a list of its child IDs
    private final Map<UUID, List<UUID>> childRelationships = new ConcurrentHashMap<>();
    
    // Maps a relationship (parent ID, child ID) to its type
    private final Map<String, WorkItemRelationshipType> relationshipTypes = new ConcurrentHashMap<>();
    
    /**
     * Creates a parent-child relationship between work items.
     *
     * @param childId the ID of the child work item
     * @param parentId the ID of the parent work item
     * @param type the type of relationship
     * @return true if relationship was created, false if it already exists
     */
    public boolean createRelationship(UUID childId, UUID parentId, WorkItemRelationshipType type) {
        if (childId.equals(parentId)) {
            throw new IllegalArgumentException("Child and parent cannot be the same work item");
        }
        
        // Check if the relationship already exists
        if (parentRelationships.containsKey(childId)) {
            return false;
        }
        
        // Create the relationship
        parentRelationships.put(childId, parentId);
        childRelationships.computeIfAbsent(parentId, k -> new ArrayList<>()).add(childId);
        
        // Set the relationship type
        String key = parentId.toString() + ":" + childId.toString();
        relationshipTypes.put(key, type);
        
        return true;
    }
    
    /**
     * Removes a parent-child relationship between work items.
     *
     * @param childId the ID of the child work item
     * @return true if relationship was removed, false if it didn't exist
     */
    public boolean removeRelationship(UUID childId) {
        UUID parentId = parentRelationships.remove(childId);
        
        if (parentId == null) {
            return false;
        }
        
        // Remove the child from the parent's list of children
        List<UUID> children = childRelationships.get(parentId);
        if (children != null) {
            children.remove(childId);
            if (children.isEmpty()) {
                childRelationships.remove(parentId);
            }
        }
        
        // Remove the relationship type
        String key = parentId.toString() + ":" + childId.toString();
        relationshipTypes.remove(key);
        
        return true;
    }
    
    /**
     * Gets the parent work item for a given child.
     *
     * @param childId the ID of the child work item
     * @return the ID of the parent work item, or null if none exists
     */
    public UUID getParentWorkItem(UUID childId) {
        return parentRelationships.get(childId);
    }
    
    /**
     * Gets all child work items for a given parent.
     *
     * @param parentId the ID of the parent work item
     * @return a list of child work item IDs
     */
    public List<UUID> getChildWorkItems(UUID parentId) {
        List<UUID> children = childRelationships.get(parentId);
        return children != null ? new ArrayList<>(children) : new ArrayList<>();
    }
    
    /**
     * Gets the type of relationship between a parent and child.
     *
     * @param childId the ID of the child work item
     * @param parentId the ID of the parent work item
     * @return the relationship type, or null if no relationship exists
     */
    public WorkItemRelationshipType getRelationshipType(UUID childId, UUID parentId) {
        String key = parentId.toString() + ":" + childId.toString();
        return relationshipTypes.get(key);
    }
    
    /**
     * Check if a work item has a parent.
     *
     * @param workItemId the ID of the work item
     * @return true if the work item has a parent, false otherwise
     */
    public boolean hasParent(UUID workItemId) {
        return parentRelationships.containsKey(workItemId);
    }
    
    /**
     * Check if a work item has any children.
     *
     * @param workItemId the ID of the work item
     * @return true if the work item has children, false otherwise
     */
    public boolean hasChildren(UUID workItemId) {
        List<UUID> children = childRelationships.get(workItemId);
        return children != null && !children.isEmpty();
    }
    
    /**
     * Gets all descendants of a work item (children, grandchildren, etc.).
     *
     * @param workItemId the ID of the work item
     * @return a list of all descendant work item IDs
     */
    public List<UUID> getAllDescendants(UUID workItemId) {
        List<UUID> descendants = new ArrayList<>();
        List<UUID> children = getChildWorkItems(workItemId);
        
        descendants.addAll(children);
        
        for (UUID childId : children) {
            descendants.addAll(getAllDescendants(childId));
        }
        
        return descendants;
    }
}