/*
 * Domain exception for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.usecase;

import org.rinna.domain.entity.WorkflowState;

import java.util.UUID;

/**
 * Exception thrown when an invalid workflow transition is attempted.
 */
public class InvalidTransitionException extends Exception {
    private static final long serialVersionUID = 1L;
    
    private final UUID itemId;
    private final WorkflowState currentState;
    private final WorkflowState targetState;
    
    /**
     * Constructs a new InvalidTransitionException with the specified item ID and states.
     * 
     * @param itemId the ID of the work item
     * @param currentState the current workflow state
     * @param targetState the target workflow state
     */
    public InvalidTransitionException(UUID itemId, WorkflowState currentState, WorkflowState targetState) {
        super(String.format(
                "Invalid transition for item %s: %s → %s", 
                itemId, 
                currentState, 
                targetState));
        this.itemId = itemId;
        this.currentState = currentState;
        this.targetState = targetState;
    }
    
    /**
     * Returns the ID of the work item.
     * 
     * @return the item ID
     */
    public UUID getItemId() {
        return itemId;
    }
    
    /**
     * Returns the current workflow state.
     * 
     * @return the current state
     */
    public WorkflowState getCurrentState() {
        return currentState;
    }
    
    /**
     * Returns the target workflow state.
     * 
     * @return the target state
     */
    public WorkflowState getTargetState() {
        return targetState;
    }
}