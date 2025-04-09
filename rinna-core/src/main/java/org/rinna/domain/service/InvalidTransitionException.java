/*
 * Domain exception for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.service;

import org.rinna.domain.model.WorkflowState;

import java.util.UUID;

/**
 * Exception thrown when an invalid workflow transition is attempted.
 * 
 * <p>This exception occurs when a work item cannot be moved from its current workflow state
 * to a requested target state according to the defined workflow rules. It captures information
 * about the work item and the invalid transition that was attempted.</p>
 * 
 * <p>The exception provides access to the work item ID, current state, and target state
 * to allow the caller to understand why the transition failed and potentially
 * take corrective action.</p>
 * 
 * @author Eric C. Mumford
 * @since 1.0
 */
public class InvalidTransitionException extends Exception {
    private static final long serialVersionUID = 1L;
    
    private final UUID itemId;
    private final WorkflowState currentState;
    private final WorkflowState targetState;
    
    /**
     * Constructs a new InvalidTransitionException with the specified item ID and states.
     * 
     * <p>Creates an exception with a standardized error message that includes the item ID
     * and the invalid transition that was attempted.</p>
     * 
     * @param itemId the ID of the work item for which the transition was attempted
     * @param currentState the current workflow state of the work item
     * @param targetState the workflow state that was requested but is invalid
     */
    public InvalidTransitionException(UUID itemId, WorkflowState currentState, WorkflowState targetState) {
        super("Invalid transition for item " + itemId + ": " + currentState + " → " + targetState);
        this.itemId = itemId;
        this.currentState = currentState;
        this.targetState = targetState;
    }
    
    /**
     * Constructs a new InvalidTransitionException with a custom message.
     * 
     * <p>This constructor should be used when specific information about the item ID
     * and states is not available or when a more detailed custom message is needed.</p>
     * 
     * @param message a custom error message describing the invalid transition
     */
    public InvalidTransitionException(String message) {
        super(message);
        this.itemId = null;
        this.currentState = null;
        this.targetState = null;
    }
    
    /**
     * Returns the ID of the work item for which the invalid transition was attempted.
     * 
     * @return the work item ID, or null if not available
     */
    public UUID getItemId() {
        return itemId;
    }
    
    /**
     * Returns the current workflow state of the work item.
     * 
     * @return the current workflow state, or null if not available
     */
    public WorkflowState getCurrentState() {
        return currentState;
    }
    
    /**
     * Returns the target workflow state that was requested but is invalid.
     * 
     * @return the target workflow state, or null if not available
     */
    public WorkflowState getTargetState() {
        return targetState;
    }
}