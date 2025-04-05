/*
 * Domain entity enum for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain;

/**
 * Enumeration of possible priority levels for work items.
 */
public enum Priority {
    /**
     * Highest priority, requires immediate attention.
     */
    HIGH,
    
    /**
     * Normal priority, should be addressed in the current cycle.
     */
    MEDIUM,
    
    /**
     * Low priority, can be deferred if necessary.
     */
    LOW;
    
    /**
     * Returns the numerical value of this priority.
     * 
     * @return the numerical value (0-2, where 0 is highest priority)
     */
    public int getValue() {
        return ordinal();
    }
    
    /**
     * Returns the priority level for the given numerical value.
     * 
     * @param value the numerical value
     * @return the corresponding Priority
     */
    public static Priority fromValue(int value) {
        if (value < 0 || value >= values().length) {
            throw new IllegalArgumentException("Invalid priority value: " + value);
        }
        return values()[value];
    }
}