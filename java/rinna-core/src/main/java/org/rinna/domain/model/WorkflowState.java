/*
 * Model class for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.model;

import java.util.ArrayList;
import java.util.List;

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
    DONE;

    /**
     * Checks if this state can transition to the target state.
     *
     * @param targetState the target state
     * @return true if the transition is allowed, false otherwise
     */
    public boolean canTransitionTo(WorkflowState targetState) {
        if (targetState == null) {
            return false;
        }

        // Define allowed transitions
        switch (this) {
            case FOUND:
                // From FOUND, can go to TRIAGED or TO_DO
                return targetState == TRIAGED || targetState == TO_DO;
            case TRIAGED:
                // From TRIAGED, can go to TO_DO
                return targetState == TO_DO;
            case TO_DO:
                // From TO_DO, can go to IN_PROGRESS
                return targetState == IN_PROGRESS;
            case IN_PROGRESS:
                // From IN_PROGRESS, can go to IN_TEST or back to TO_DO
                return targetState == IN_TEST || targetState == TO_DO;
            case IN_TEST:
                // From IN_TEST, can go to DONE, back to IN_PROGRESS, or back to TO_DO
                return targetState == DONE || targetState == IN_PROGRESS || targetState == TO_DO;
            case DONE:
                // From DONE, can go back to IN_TEST or TO_DO (for reopening)
                return targetState == IN_TEST || targetState == TO_DO;
            default:
                return false;
        }
    }

    /**
     * Gets the list of states that this state can transition to.
     *
     * @return a list of available transition states
     */
    public List<WorkflowState> getAvailableTransitions() {
        List<WorkflowState> availableTransitions = new ArrayList<>();

        for (WorkflowState state : WorkflowState.values()) {
            if (canTransitionTo(state)) {
                availableTransitions.add(state);
            }
        }

        return availableTransitions;
    }
}
