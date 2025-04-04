/*
 * Model class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package com.rinna.model;

/**
 * Defines the priority levels for work items.
 */
public enum Priority {
    /**
     * Highest priority, requiring immediate attention.
     */
    HIGH,
    
    /**
     * Standard priority level for normal development work.
     */
    MEDIUM,
    
    /**
     * Lower priority items that can be addressed later.
     */
    LOW
}