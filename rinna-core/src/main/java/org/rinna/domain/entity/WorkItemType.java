/*
 * Domain entity enum for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.entity;

import java.util.List;
import java.util.Map;

/**
 * Enumeration of possible work item types in the Rinna system.
 * These types form a hierarchy from highest level (GOAL) to lowest level implementations.
 */
public enum WorkItemType {
    /**
     * High-level objective that may span multiple releases.
     */
    GOAL,
    
    /**
     * Specific functionality that supports a goal.
     */
    FEATURE,
    
    /**
     * Issue that needs to be fixed.
     */
    BUG,
    
    /**
     * Maintenance task or other non-feature work.
     */
    CHORE;
    
    // Static mapping of allowed child types
    private static final Map<WorkItemType, List<WorkItemType>> VALID_CHILD_TYPES = Map.of(
        GOAL, List.of(FEATURE),
        FEATURE, List.of(BUG, CHORE),
        BUG, List.of(),
        CHORE, List.of()
    );
    
    /**
     * Returns whether this type can have children of the given type.
     * 
     * @param childType the potential child type
     * @return true if this type can have children of the given type
     */
    public boolean canHaveChildOfType(WorkItemType childType) {
        return VALID_CHILD_TYPES.getOrDefault(this, List.of()).contains(childType);
    }
}