/*
 * Domain entity enum for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.entity;

/**
 * Enumeration of possible workflow states in the Rinna system.
 * These states represent the progression of a work item through its lifecycle.
 */
public enum WorkflowState {
    /**
     * Initial state for newly created work items.
     */
    FOUND,
    
    /**
     * Work item has been reviewed and prioritized.
     */
    TRIAGED,
    
    /**
     * Work item is ready to be worked on.
     */
    TO_DO,
    
    /**
     * Work item is actively being worked on.
     */
    IN_PROGRESS,
    
    /**
     * Work item implementation is complete and being tested.
     */
    IN_TEST,
    
    /**
     * Work item is complete and verified.
     */
    DONE;
    
    /**
     * Returns whether this state can transition to the given target state.
     * 
     * @param targetState the potential target state
     * @return true if this state can transition to the target state
     */
    public boolean canTransitionTo(WorkflowState targetState) {
        switch (this) {
            case FOUND:
                return targetState == TRIAGED;
            case TRIAGED:
                return targetState == TO_DO || targetState == DONE;
            case TO_DO:
                return targetState == IN_PROGRESS || targetState == DONE;
            case IN_PROGRESS:
                return targetState == IN_TEST || targetState == TO_DO;
            case IN_TEST:
                return targetState == DONE || targetState == IN_PROGRESS;
            case DONE:
                return false;
            default:
                return false;
        }
    }
}