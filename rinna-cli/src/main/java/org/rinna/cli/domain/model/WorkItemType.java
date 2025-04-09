/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.domain.model;

/**
 * Enum representing different types of work items.
 */
public enum WorkItemType {
    /**
     * A task represents a unit of work that needs to be done.
     */
    TASK,
    
    /**
     * A bug represents an issue that needs to be fixed.
     */
    BUG,
    
    /**
     * A story represents a feature from a user's perspective.
     */
    STORY,
    
    /**
     * An epic represents a large body of work that can be broken down into smaller stories.
     */
    EPIC,
    
    /**
     * A milestone represents a significant point in the project timeline.
     */
    MILESTONE
}