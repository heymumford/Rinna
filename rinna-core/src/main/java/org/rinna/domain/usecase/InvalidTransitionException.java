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
     */
    public InvalidTransitionException(UUID itemId, WorkflowState currentState, WorkflowState targetState) {
        super(STR."Invalid transition for item \{itemId}: \{currentState} → \{targetState}");
        this.itemId = itemId;
        this.currentState = currentState;
        this.targetState = targetState;
    }
    
    /**
     * Constructs a new InvalidTransitionException with a custom message.
     */
    public InvalidTransitionException(String message) {
        super(message);
        this.itemId = null;
        this.currentState = null;
        this.targetState = null;
    }
    
    public UUID getItemId() {
        return itemId;
    }
    
    public WorkflowState getCurrentState() {
        return currentState;
    }
    
    public WorkflowState getTargetState() {
        return targetState;
    }
}