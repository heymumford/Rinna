/*
 * Domain service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.usecase;

import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkflowState;

import java.util.List;
import java.util.UUID;

/**
 * Service interface for managing workflow transitions.
 * This interface defines the application use cases for workflow transitions.
 */
public interface WorkflowService {
    
    /**
     * Transitions a work item to a new state.
     *
     * @param itemId the ID of the work item to transition
     * @param targetState the target workflow state
     * @return the updated work item
     * @throws InvalidTransitionException if the transition is not valid
     */
    WorkItem transition(UUID itemId, WorkflowState targetState) throws InvalidTransitionException;
    
    /**
     * Checks if a transition to the target state is valid for the given work item.
     *
     * @param itemId the ID of the work item
     * @param targetState the target workflow state
     * @return true if the transition is valid, false otherwise
     */
    boolean canTransition(UUID itemId, WorkflowState targetState);
    
    /**
     * Gets the available workflow states that a work item can transition to.
     *
     * @param itemId the ID of the work item
     * @return a list of available workflow states
     */
    List<WorkflowState> getAvailableTransitions(UUID itemId);
}