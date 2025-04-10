/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.domain.service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.rinna.cli.domain.model.DomainWorkItem;
import org.rinna.cli.domain.model.DomainWorkflowState;

/**
 * Domain service interface for workflow operations - used to minimize dependency issues
 * during CLI module migration. This is a temporary interface that matches
 * the core domain WorkflowService interface.
 */
public interface WorkflowService {
    /**
     * Transitions a work item to a new state.
     *
     * @param itemId the work item ID
     * @param targetState the target state
     * @return the updated work item
     * @throws InvalidTransitionException if the transition is invalid
     */
    DomainWorkItem transition(UUID itemId, DomainWorkflowState targetState) throws InvalidTransitionException;
    
    /**
     * Transitions a work item to a new state with a comment.
     *
     * @param itemId the work item ID
     * @param user the user making the transition
     * @param targetState the target state
     * @param comment the transition comment
     * @return the updated work item
     * @throws InvalidTransitionException if the transition is invalid
     */
    DomainWorkItem transition(UUID itemId, String user, DomainWorkflowState targetState, String comment) throws InvalidTransitionException;
    
    /**
     * Checks if a transition is valid for a work item.
     *
     * @param itemId the work item ID
     * @param targetState the target state
     * @return true if the transition is valid
     */
    boolean canTransition(UUID itemId, DomainWorkflowState targetState);
    
    /**
     * Gets available transitions for a work item.
     *
     * @param itemId the work item ID
     * @return the list of available transitions
     */
    List<DomainWorkflowState> getAvailableTransitions(UUID itemId);
    
    /**
     * Gets the current work item in progress for a user.
     *
     * @param user the user
     * @return the current work item in progress
     */
    Optional<DomainWorkItem> getCurrentWorkInProgress(String user);
    
    /**
     * Assigns a work item to a user.
     *
     * @param itemId the work item ID
     * @param user the user making the assignment
     * @param assignee the assignee
     * @return the updated work item
     * @throws InvalidTransitionException if the assignment is invalid
     */
    DomainWorkItem assignWorkItem(UUID itemId, String user, String assignee) throws InvalidTransitionException;
    
    /**
     * Assigns a work item to a user with a comment.
     *
     * @param itemId the work item ID
     * @param user the user making the assignment
     * @param assignee the assignee
     * @param comment the assignment comment
     * @return the updated work item
     * @throws InvalidTransitionException if the assignment is invalid
     */
    DomainWorkItem assignWorkItem(UUID itemId, String user, String assignee, String comment) throws InvalidTransitionException;
    
    /**
     * Gets the ID of the current active work item for a user.
     *
     * @param user the user
     * @return the active work item ID
     */
    UUID getCurrentActiveItemId(String user);
}