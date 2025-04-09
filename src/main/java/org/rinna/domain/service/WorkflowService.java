/*
 * Domain service interface for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.service;

import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkflowState;
import org.rinna.usecase.InvalidTransitionException;

import java.util.List;
import java.util.Optional;
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
     * Transitions a work item to a new state with user and comment details.
     *
     * @param itemId the ID of the work item to transition
     * @param user the user making the transition
     * @param targetState the target workflow state
     * @param comment an optional comment explaining the transition
     * @return the updated work item
     * @throws InvalidTransitionException if the transition is not valid
     */
    WorkItem transition(UUID itemId, String user, WorkflowState targetState, String comment) throws InvalidTransitionException;
    
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
    
    /**
     * Gets the current work item in progress for a user, if any.
     * A user can only have one work item in progress at a time.
     *
     * @param user the user to check
     * @return an Optional containing the work item in progress, or empty if none
     */
    Optional<WorkItem> getCurrentWorkInProgress(String user);
    
    /**
     * Assigns a work item to a user.
     *
     * @param itemId the ID of the work item to assign
     * @param user the user making the assignment
     * @param assignee the user to assign the work item to
     * @return the updated work item
     * @throws InvalidTransitionException if the assignment is not valid
     */
    WorkItem assignWorkItem(UUID itemId, String user, String assignee) throws InvalidTransitionException;
    
    /**
     * Assigns a work item to a user with a comment.
     *
     * @param itemId the ID of the work item to assign
     * @param user the user making the assignment
     * @param assignee the user to assign the work item to
     * @param comment an optional comment explaining the assignment
     * @return the updated work item
     * @throws InvalidTransitionException if the assignment is not valid
     */
    WorkItem assignWorkItem(UUID itemId, String user, String assignee, String comment) throws InvalidTransitionException;
    
    /**
     * Gets the ID of the current active work item for a user.
     * Used to enforce that undo history is cleared when changing work items.
     *
     * @param user the user name
     * @return the UUID of the current active work item, or null if none
     */
    UUID getCurrentActiveItemId(String user);
}