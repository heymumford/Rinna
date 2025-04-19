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

import java.util.*;
import java.util.stream.Collectors;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;

/**
 * Mock implementation of workflow service functionality for CLI use.
 */
public class MockWorkflowService implements WorkflowService {
    private final List<WorkItem> workItems = new ArrayList<>();
    
    /**
     * Constructor initializing some sample work items.
     */
    public MockWorkflowService() {
        // Initialize with some sample data
        WorkItem item1 = new WorkItem();
        item1.setId("123e4567-e89b-12d3-a456-426614174000");
        item1.setTitle("Implement authentication feature");
        item1.setType(org.rinna.cli.model.WorkItemType.TASK);
        item1.setPriority(org.rinna.cli.model.Priority.HIGH);
        item1.setState(org.rinna.cli.model.WorkflowState.IN_PROGRESS);
        item1.setAssignee(System.getProperty("user.name"));
        workItems.add(item1);
        
        WorkItem item2 = new WorkItem();
        item2.setId("223e4567-e89b-12d3-a456-426614174001");
        item2.setTitle("Fix bug in payment module");
        item2.setType(org.rinna.cli.model.WorkItemType.BUG);
        item2.setPriority(org.rinna.cli.model.Priority.CRITICAL);
        item2.setState(org.rinna.cli.model.WorkflowState.READY);
        workItems.add(item2);
    }
    
    // CLI-specific methods
    
    /**
     * Gets a work item by ID.
     *
     * @param id the work item ID as UUID
     * @return the work item, or null if not found
     */
    @Override
    public WorkItem getItem(UUID id) {
        return getItem(id.toString());
    }
    
    /**
     * Gets a work item by ID.
     *
     * @param itemId the work item ID
     * @return the work item, or null if not found
     */
    @Override
    public WorkItem getItem(String itemId) {
        return workItems.stream()
                .filter(item -> item.getId().equals(itemId))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * Gets work item's status (keeping both getStatus and getState for backward compatibility)
     * 
     * @param item the work item
     * @return the status
     */
    public WorkflowState getStatus(WorkItem item) {
        return item.getState();
    }
    
    /**
     * Gets items in a specific state.
     *
     * @param state the workflow state
     * @return a list of work items
     */
    @Override
    public List<WorkItem> getItemsInState(WorkflowState state) {
        List<WorkItem> result = new ArrayList<>();
        for (WorkItem item : workItems) {
            if (state.equals(item.getState())) {
                result.add(item);
            }
        }
        return result;
    }
    
    /**
     * Gets items with a specific status. Alias for getItemsInState for backwards compatibility.
     *
     * @param status the workflow status
     * @return a list of work items
     */
    public List<WorkItem> getItemsWithStatus(WorkflowState status) {
        return getItemsInState(status);
    }
    
    /**
     * Gets work items in a specific state for an assignee.
     *
     * @param state the workflow state
     * @param assignee the assignee
     * @return a list of work items
     */
    @Override
    public List<WorkItem> getItemsInState(WorkflowState state, String assignee) {
        List<WorkItem> result = new ArrayList<>();
        for (WorkItem item : workItems) {
            if (state.equals(item.getState()) && assignee.equals(item.getAssignee())) {
                result.add(item);
            }
        }
        return result;
    }
    
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
    @Override
    public WorkItem transition(UUID workItemId, String username, WorkflowState newState, String comment) throws InvalidTransitionException {
        return transition(workItemId.toString(), username, newState, comment);
    }
    
    /**
     * Transitions a work item to a new state.
     *
     * @param itemId the work item ID string
     * @param targetState the target state from CLI module
     * @return the updated work item
     * @throws InvalidTransitionException if the transition is invalid
     */
    @Override
    public WorkItem transition(String itemId, WorkflowState targetState) throws InvalidTransitionException {
        return transition(itemId, "system", targetState, null);
    }
    
    /**
     * Transitions a work item to a new state with a user and comment.
     *
     * @param itemId the work item ID string
     * @param user the user initiating the transition
     * @param targetState the target state from CLI module
     * @param comment a comment about the transition
     * @return the updated work item
     * @throws InvalidTransitionException if the transition is invalid
     */
    @Override
    public WorkItem transition(String itemId, String user, WorkflowState targetState, String comment) throws InvalidTransitionException {
        // Find the item in our list
        WorkItem item = getItem(itemId);
        
        if (item == null) {
            throw new InvalidTransitionException("Work item not found: " + itemId);
        }
        
        // Record the state change for history tracking
        String previousState = item.getState().toString();
        
        // Check if transition is valid (simplified for mock)
        if (!canTransition(itemId, targetState)) {
            throw new InvalidTransitionException("Invalid transition from " + previousState + " to " + targetState);
        }
        
        // Update the state
        item.setState(targetState);
        
        return item;
    }
    
    /**
     * Checks if a work item can transition to a target state.
     *
     * @param itemId the work item ID string
     * @param targetState the target state from CLI module
     * @return true if the transition is valid, false otherwise
     */
    @Override
    public boolean canTransition(String itemId, WorkflowState targetState) {
        WorkItem item = getItem(itemId);
        
        if (item == null) {
            return false;
        }
        
        WorkflowState currentState = item.getState();
        
        // Simplified transition rules
        switch (currentState) {
            case CREATED:
                return targetState == WorkflowState.READY || 
                       targetState == WorkflowState.BLOCKED;
                
            case READY:
                return targetState == WorkflowState.IN_PROGRESS || 
                       targetState == WorkflowState.BLOCKED;
                
            case IN_PROGRESS:
                return targetState == WorkflowState.READY || 
                       targetState == WorkflowState.BLOCKED || 
                       targetState == WorkflowState.DONE;
                
            case BLOCKED:
                return targetState == WorkflowState.READY;
                
            case DONE:
                return targetState == WorkflowState.READY;
                
            default:
                return false;
        }
    }
    
    /**
     * Gets the current state of a work item.
     *
     * @param itemId the work item ID as a string
     * @return the current state
     */
    @Override
    public WorkflowState getCurrentState(String itemId) {
        WorkItem item = getItem(itemId);
        return item != null ? item.getState() : null;
    }
    
    /**
     * Gets the available transitions for a work item.
     *
     * @param itemId the work item ID string
     * @return a list of available transition states from CLI module
     */
    @Override
    public List<WorkflowState> getAvailableTransitions(String itemId) {
        List<WorkflowState> availableStates = new ArrayList<>();
        
        for (WorkflowState state : WorkflowState.values()) {
            if (canTransition(itemId, state)) {
                availableStates.add(state);
            }
        }
        
        return availableStates;
    }
    
    /**
     * Assigns a work item to a user.
     *
     * @param itemId the work item ID string
     * @param user the user initiating the assignment
     * @param assignee the user to assign to
     * @return the updated work item
     * @throws InvalidTransitionException if the item is not found
     */
    public WorkItem assignWorkItem(String itemId, String user, String assignee) throws InvalidTransitionException {
        return assignWorkItem(itemId, user, assignee, null);
    }
    
    /**
     * Assigns a work item to a user with a comment.
     *
     * @param itemId the work item ID string
     * @param user the user initiating the assignment
     * @param assignee the user to assign to
     * @param comment a comment about the assignment
     * @return the updated work item
     * @throws InvalidTransitionException if the item is not found
     */
    public WorkItem assignWorkItem(String itemId, String user, String assignee, String comment) throws InvalidTransitionException {
        WorkItem item = getItem(itemId);
        
        if (item == null) {
            throw new InvalidTransitionException("Work item not found: " + itemId);
        }
        
        // Record the assignment change for history tracking
        String previousAssignee = item.getAssignee();
        
        // Update the assignee
        item.setAssignee(assignee);
        
        return item;
    }
    
    /**
     * Gets the ID of the current active work item for a user.
     *
     * @param username the username
     * @return the ID of the current active work item, or null if none
     */
    @Override
    public UUID getCurrentActiveItemId(String username) {
        // Return the ID of the first in-progress item
        Optional<WorkItem> activeItem = getCurrentWorkInProgress(username);
        String itemId = activeItem.map(WorkItem::getId).orElse(null);
        
        // Convert the string ID to UUID if it exists
        if (itemId != null) {
            try {
                return UUID.fromString(itemId);
            } catch (IllegalArgumentException e) {
                return null;
            }
        }
        
        return null;
    }
    
    /**
     * Gets the ID of the current active work item for a user as a string.
     *
     * @param user the user name
     * @return the ID of the current active work item, or null if none
     */
    public String getCurrentActiveItemIdAsString(String user) {
        // Return the ID of the first in-progress item
        Optional<WorkItem> activeItem = getCurrentWorkInProgress(user);
        return activeItem.map(WorkItem::getId).orElse(null);
    }
    
    /**
     * Gets the current work in progress for a user.
     *
     * @param user the user name
     * @return an Optional containing the work item in progress, or empty if none
     */
    public Optional<WorkItem> getCurrentWorkInProgress(String user) {
        // Find a work item assigned to the user that's in progress
        return workItems.stream()
            .filter(item -> user.equals(item.getAssignee()) && WorkflowState.IN_PROGRESS.equals(item.getState()))
            .findFirst();
    }
    
    /**
     * Gets the current work item for a user, regardless of state.
     *
     * @param user the user name
     * @return the current work item, or null if none
     */
    @Override
    public WorkItem getCurrentWorkItem(String user) {
        // Find a work item assigned to the user
        Optional<WorkItem> item = workItems.stream()
            .filter(i -> user.equals(i.getAssignee()))
            .findFirst();
        return item.orElse(null);
    }
    
    /**
     * Find work items by status.
     *
     * @param status the workflow state to search for
     * @return a list of work items with the given status
     */
    @Override
    public List<WorkItem> findByStatus(WorkflowState status) {
        return workItems.stream()
            .filter(item -> status.equals(item.getState()))
            .collect(Collectors.toList());
    }
    
    /**
     * Sets the current active work item for a specific user.
     *
     * @param workItemId the work item ID
     * @param username the username
     * @return true if the operation was successful
     */
    @Override
    public boolean setCurrentActiveItem(UUID workItemId, String username) {
        WorkItem item = getItem(workItemId);
        
        if (item == null) {
            return false;
        }
        
        // Store the active item ID in a user-specific map
        Map<String, Object> userData = ServiceManager.getInstance().getUserData(username);
        userData.put("activeItemId", workItemId.toString());
        
        // Mark item as in progress if it's not already
        if (item.getState() != WorkflowState.IN_PROGRESS) {
            try {
                transition(workItemId.toString(), username, WorkflowState.IN_PROGRESS, 
                           "Started work via setCurrentActiveItem");
            } catch (InvalidTransitionException e) {
                // If we can't transition, just continue - we've still marked it as active
            }
        }
        
        return true;
    }
    
    /**
     * Clears the current active work item for a specific user.
     *
     * @param username the username
     * @return true if the operation was successful
     */
    @Override
    public boolean clearCurrentActiveItem(String username) {
        // Remove the active item ID from user data
        Map<String, Object> userData = ServiceManager.getInstance().getUserData(username);
        Object previousActiveId = userData.remove("activeItemId");
        
        // Return true if there was an active item that we cleared
        return previousActiveId != null;
    }
    
    // Domain adapter methods would go here, but have been removed
    // to avoid dependencies on the core domain model
}