/*
 * Domain entity enum for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain;

import java.util.List;
import java.util.Map;

/**
 * Enumeration of possible workflow states in the Rinna system.
 * These states represent the progression of a work item through its lifecycle from creation to completion.
 * <p>
 * The workflow follows a defined state machine with specific allowed transitions between states,
 * enforcing proper process flow. The transition rules are defined in the VALID_TRANSITIONS map.
 * </p>
 * <p>
 * The workflow state machine follows this general progression:
 * <ol>
 *   <li>FOUND: Initial state for newly created work items</li>
 *   <li>TRIAGED: Work item has been reviewed and prioritized</li>
 *   <li>TO_DO: Work item is ready to be worked on</li>
 *   <li>IN_PROGRESS: Work item is actively being worked on</li>
 *   <li>IN_TEST: Work item implementation is complete and being tested</li>
 *   <li>DONE: Work item is complete and verified</li>
 * </ol>
 * </p>
 * <p>
 * Not all transitions between states are allowed. For example, a work item cannot go directly
 * from FOUND to DONE without going through intermediate states. The allowed transitions
 * are defined in the VALID_TRANSITIONS static map.
 * </p>
 */
public enum WorkflowState {
    /** Initial state for newly created work items. */
    FOUND,
    
    /** Work item has been reviewed and prioritized. */
    TRIAGED,
    
    /** Work item is ready to be worked on. */
    TO_DO,
    
    /** Work item is actively being worked on. */
    IN_PROGRESS,
    
    /** Work item implementation is complete and being tested. */
    IN_TEST,
    
    /** Work item is complete and verified. */
    DONE;
    
    // Static mapping of allowed transitions
    private static final Map<WorkflowState, List<WorkflowState>> VALID_TRANSITIONS = Map.of(
        FOUND, List.of(TRIAGED),
        TRIAGED, List.of(TO_DO, DONE),
        TO_DO, List.of(IN_PROGRESS, DONE),
        IN_PROGRESS, List.of(IN_TEST, TO_DO),
        IN_TEST, List.of(DONE, IN_PROGRESS),
        DONE, List.of()
    );
    
    /**
     * Returns whether this state can transition to the given target state.
     * 
     * @param targetState the potential target state
     * @return true if this state can transition to the target state
     */
    public boolean canTransitionTo(WorkflowState targetState) {
        return VALID_TRANSITIONS.getOrDefault(this, List.of()).contains(targetState);
    }
    
    /**
     * Returns the list of states that this state can transition to.
     * 
     * @return list of valid target states
     */
    public List<WorkflowState> getAvailableTransitions() {
        return VALID_TRANSITIONS.getOrDefault(this, List.of());
    }
}