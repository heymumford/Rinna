/*
 * Domain entity enum for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

/**
 * Enumeration of possible comment types in the Rinna system.
 */
public enum CommentType {
    /**
     * Standard user comment.
     */
    STANDARD,
    
    /**
     * System-generated comment (not associated with a specific user).
     */
    SYSTEM,
    
    /**
     * Comment that records a state transition.
     */
    TRANSITION,
    
    /**
     * Comment that records a metadata change.
     */
    METADATA_CHANGE,
    
    /**
     * Comment that records an assignment change.
     */
    ASSIGNMENT_CHANGE
}
