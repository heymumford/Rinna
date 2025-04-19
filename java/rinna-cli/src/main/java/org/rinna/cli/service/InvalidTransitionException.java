/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import org.rinna.cli.model.WorkflowState;

/**
 * Exception thrown when an invalid workflow transition is attempted.
 * This is a CLI-specific version that matches the domain exception.
 */
public class InvalidTransitionException extends Exception {
    private static final long serialVersionUID = 1L;
    
    private final String itemId;
    private final WorkflowState currentState;
    private final WorkflowState targetState;
    
    /**
     * Constructs a new InvalidTransitionException with the specified item ID and states.
     */
    public InvalidTransitionException(String itemId, WorkflowState currentState, WorkflowState targetState) {
        super("Invalid transition for item " + itemId + ": " + currentState + " → " + targetState);
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
    
    public String getItemId() {
        return itemId;
    }
    
    public WorkflowState getCurrentState() {
        return currentState;
    }
    
    public WorkflowState getTargetState() {
        return targetState;
    }
}