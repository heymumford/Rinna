/*
 * Domain entity enum for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

/**
 * Enumeration of possible history entry types in the Rinna system.
 */
public enum HistoryEntryType {
    /**
     * A comment on the work item.
     */
    COMMENT,
    
    /**
     * A state change in the work item's workflow status.
     */
    STATE_CHANGE,
    
    /**
     * A change in the work item's fields (title, description, etc.).
     */
    FIELD_CHANGE,
    
    /**
     * A change in the work item's assignment.
     */
    ASSIGNMENT_CHANGE,
    
    /**
     * A change in the work item's priority.
     */
    PRIORITY_CHANGE,
    
    /**
     * A change in the work item's relationships (parent, children).
     */
    RELATIONSHIP_CHANGE,
    
    /**
     * A custom or system event not covered by other types.
     */
    OTHER
}
