/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.domain.model;

/**
 * Enum representing different types of work item relationships.
 */
public enum WorkItemRelationshipType {
    /**
     * A parent-child relationship.
     */
    PARENT_CHILD,
    
    /**
     * A dependency relationship.
     */
    DEPENDENCY,
    
    /**
     * A blocking relationship.
     */
    BLOCKING,
    
    /**
     * A related relationship.
     */
    RELATED,
    
    /**
     * A duplicate relationship.
     */
    DUPLICATE
}