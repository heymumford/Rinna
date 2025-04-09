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
 * Enum representing different priority levels for work items.
 */
public enum Priority {
    /**
     * Critical priority - requires immediate attention.
     */
    CRITICAL,
    
    /**
     * High priority - should be addressed as soon as possible.
     */
    HIGH,
    
    /**
     * Medium priority - should be addressed in the normal course of work.
     */
    MEDIUM,
    
    /**
     * Low priority - can be addressed when time permits.
     */
    LOW,
    
    /**
     * Trivial priority - nice to have but not essential.
     */
    TRIVIAL
}