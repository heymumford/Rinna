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
 * Enum representing different types of history entries.
 */
public enum HistoryEntryType {
    /**
     * A change in workflow state.
     */
    STATE_CHANGE,
    
    /**
     * A change in a field value.
     */
    FIELD_CHANGE,
    
    /**
     * A change in assignment.
     */
    ASSIGNMENT_CHANGE,
    
    /**
     * A change in priority.
     */
    PRIORITY_CHANGE,
    
    /**
     * A comment was added.
     */
    COMMENT,
    
    /**
     * A change in the work item's relationships (parent, children).
     */
    RELATIONSHIP_CHANGE,
    
    /**
     * An undo operation was performed.
     */
    UNDO
}