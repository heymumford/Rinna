/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.domain;

/**
 * Enum for visibility levels of work items.
 */
public enum Visibility {
    /**
     * Visible only to the creator.
     */
    PRIVATE,
    
    /**
     * Visible to the team members.
     */
    TEAM,
    
    /**
     * Visible to all users in the system.
     */
    PUBLIC
}