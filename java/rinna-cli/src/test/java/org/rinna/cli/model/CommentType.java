/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.model;

/**
 * Enum representing different types of comments that can be attached to a work item.
 */
public enum CommentType {
    /**
     * A regular comment.
     */
    REGULAR,
    
    /**
     * A system-generated comment.
     */
    SYSTEM,
    
    /**
     * A comment related to a state transition.
     */
    STATE_TRANSITION,
    
    /**
     * A comment related to a work item assignment.
     */
    ASSIGNMENT,
    
    /**
     * A comment related to a field change.
     */
    FIELD_CHANGE,
    
    /**
     * A comment containing code or technical details.
     */
    CODE;
    
    @Override
    public String toString() {
        return name().toLowerCase().replace('_', '-');
    }
}