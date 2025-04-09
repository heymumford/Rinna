/*
 * Model class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

/**
 * Defines the workflow states an item can transition through.
 */
public enum WorkflowState {
    /**
     * Item initially identified.
     */
    FOUND,
    
    /**
     * Item assessed and prioritized.
     */
    TRIAGED,
    
    /**
     * Item ready to be worked on.
     */
    TO_DO,
    
    /**
     * Item currently being worked on.
     */
    IN_PROGRESS,
    
    /**
     * Item under verification.
     */
    IN_TEST,
    
    /**
     * Item completed.
     */
    DONE
}