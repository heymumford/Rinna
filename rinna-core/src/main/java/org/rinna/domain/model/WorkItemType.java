/*
 * Model class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

/**
 * Defines the different types of work items supported by Rinna.
 */
public enum WorkItemType {
    /**
     * High-level software development objective.
     */
    GOAL,
    
    /**
     * Incremental, deliverable functionality.
     */
    FEATURE,
    
    /**
     * Unexpected software issue requiring correction.
     */
    BUG,
    
    /**
     * Non-functional task ensuring system health and stability.
     */
    CHORE
}