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

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;

import java.util.List;
import java.util.UUID;

/**
 * Interface for workflow services.
 */
public interface WorkflowService {
    
    /**
     * Gets a work item by its ID.
     *
     * @param id the work item ID
     * @return the work item, or null if not found
     */
    WorkItem getItem(UUID id);
    
    /**
     * Gets a work item by its string ID.
     *
     * @param id the work item ID as a string
     * @return the work item, or null if not found
     */
    WorkItem getItem(String id);
    
    /**
     * Gets all work items in a specific state.
     *
     * @param state the state to filter by
     * @return a list of work items in the specified state
     */
    List<WorkItem> getItemsInState(WorkflowState state);
    
    /**
     * Gets all work items in a specific state assigned to a specific user.
     *
     * @param state the state to filter by
     * @param username the username to filter by
     * @return a list of work items in the specified state and assigned to the specified user
     */
    List<WorkItem> getItemsInState(WorkflowState state, String username);
    
    /**
     * Transitions a work item to a new state.
     *
     * @param workItemId the work item ID
     * @param username the user making the transition
     * @param newState the new state
     * @param comment a comment describing the transition
     * @return the updated work item
     * @throws InvalidTransitionException if the transition is invalid
     */
    WorkItem transition(UUID workItemId, String username, WorkflowState newState, String comment) throws InvalidTransitionException;
    
    /**
     * Transitions a work item to a new state.
     *
     * @param workItemId the work item ID as a string
     * @param username the user making the transition
     * @param newState the new state
     * @param comment a comment describing the transition
     * @return the updated work item
     * @throws InvalidTransitionException if the transition is invalid
     */
    WorkItem transition(String workItemId, String username, WorkflowState newState, String comment) throws InvalidTransitionException;
    
    /**
     * Transitions a work item to a new state without a comment.
     *
     * @param workItemId the work item ID as a string
     * @param newState the new state
     * @return the updated work item
     * @throws InvalidTransitionException if the transition is invalid
     */
    WorkItem transition(String workItemId, WorkflowState newState) throws InvalidTransitionException;
    
    /**
     * Checks if a transition is valid for a work item.
     *
     * @param itemId the work item ID as a string
     * @param targetState the target state
     * @return true if the transition is valid, false otherwise
     */
    boolean canTransition(String itemId, WorkflowState targetState);
    
    /**
     * Gets the available transitions for a work item.
     *
     * @param itemId the work item ID as a string
     * @return a list of available states
     */
    List<WorkflowState> getAvailableTransitions(String itemId);
    
    /**
     * Gets the current state of a work item.
     *
     * @param itemId the work item ID as a string
     * @return the current state
     */
    WorkflowState getCurrentState(String itemId);
    
    /**
     * Gets the current active work item for a specific user.
     *
     * @param username the username
     * @return the ID of the current active work item, or null if none
     */
    UUID getCurrentActiveItemId(String username);
    
    /**
     * Gets the current work item for a user, regardless of state.
     *
     * @param user the user name
     * @return the current work item, or null if none
     */
    WorkItem getCurrentWorkItem(String user);
    
    /**
     * Find work items by status.
     *
     * @param status the workflow state to search for
     * @return a list of work items with the given status
     */
    List<WorkItem> findByStatus(WorkflowState status);
    
    /**
     * Sets the current active work item for a specific user.
     *
     * @param workItemId the work item ID
     * @param username the username
     * @return true if the operation was successful
     */
    boolean setCurrentActiveItem(UUID workItemId, String username);
    
    /**
     * Clears the current active work item for a specific user.
     *
     * @param username the username
     * @return true if the operation was successful
     */
    boolean clearCurrentActiveItem(String username);
}