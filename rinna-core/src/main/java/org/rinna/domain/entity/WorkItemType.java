/*
 * Domain entity enum for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.entity;

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
    
    /**
     * Returns whether this type can have children of the given type.
     * 
     * @param childType the potential child type
     * @return true if this type can have children of the given type
     */
    public boolean canHaveChildOfType(WorkItemType childType) {
        switch (this) {
            case GOAL:
                return childType == FEATURE;
            case FEATURE:
                return childType == BUG || childType == CHORE;
            default:
                return false;
        }
    }
}