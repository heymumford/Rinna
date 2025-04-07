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
import org.rinna.cli.util.ModelMapper;
import org.rinna.domain.model.WorkflowState;
import org.rinna.domain.service.WorkflowService;
import org.rinna.usecase.InvalidTransitionException;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Mock implementation of the WorkflowService interface for testing.
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
    
    /**
     * Gets a work item by ID.
     *
     * @param itemId the work item ID
     * @return the work item, or null if not found
     */
    public org.rinna.domain.model.WorkItem getItem(UUID itemId) {
        return workItems.stream()
                .filter(item -> UUID.fromString(item.getId()).equals(itemId))
                .findFirst()
                .map(ModelMapper::toDomainWorkItem)
                .orElse(null);
    }
    
    /**
     * Gets work items in a specific state for an assignee.
     *
     * @param state the workflow state
     * @param assignee the assignee
     * @return a list of work items
     */
    public List<org.rinna.domain.model.WorkItem> getItemsInState(WorkflowState state, String assignee) {
        return workItems.stream()
                .filter(item -> ModelMapper.toCliWorkflowState(state).equals(item.getState()) && 
                       assignee.equals(item.getAssignee()))
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    @Override
    public org.rinna.domain.model.WorkItem transition(UUID itemId, WorkflowState targetState) throws InvalidTransitionException {
        return transition(itemId, "system", targetState, null);
    }
    
    @Override
    public org.rinna.domain.model.WorkItem transition(UUID itemId, String user, WorkflowState targetState, String comment) throws InvalidTransitionException {
        // Find the item in our list
        Optional<WorkItem> optItem = workItems.stream()
                .filter(item -> UUID.fromString(item.getId()).equals(itemId))
                .findFirst();
        
        if (optItem.isEmpty()) {
            throw new InvalidTransitionException("Work item not found: " + itemId);
        }
        
        WorkItem item = optItem.get();
        
        // Record the state change for history tracking
        String previousState = item.getState().toString();
        
        // Update the state
        item.setState(ModelMapper.toCliWorkflowState(targetState));
        
        // Return the updated domain item
        return ModelMapper.toDomainWorkItem(item);
    }
    
    @Override
    public boolean canTransition(UUID itemId, WorkflowState targetState) {
        // Find the item in our list
        Optional<WorkItem> optItem = workItems.stream()
                .filter(item -> UUID.fromString(item.getId()).equals(itemId))
                .findFirst();
        
        if (optItem.isEmpty()) {
            return false;
        }
        
        WorkItem item = optItem.get();
        WorkflowState currentState = ModelMapper.toDomainWorkflowState(item.getState());
        
        // Use the domain model's validation logic
        return currentState.canTransitionTo(targetState);
    }
    
    @Override
    public List<WorkflowState> getAvailableTransitions(UUID itemId) {
        // Find the item in our list
        Optional<WorkItem> optItem = workItems.stream()
                .filter(item -> UUID.fromString(item.getId()).equals(itemId))
                .findFirst();
        
        if (optItem.isEmpty()) {
            return Collections.emptyList();
        }
        
        WorkItem item = optItem.get();
        WorkflowState currentState = ModelMapper.toDomainWorkflowState(item.getState());
        
        // Use the domain model's available transitions
        return currentState.getAvailableTransitions();
    }
    
    @Override
    public Optional<org.rinna.domain.model.WorkItem> getCurrentWorkInProgress(String user) {
        // Find a work item assigned to the user that's in progress
        return workItems.stream()
            .filter(item -> user.equals(item.getAssignee()) && 
                   org.rinna.cli.model.WorkflowState.IN_PROGRESS.equals(item.getState()))
            .findFirst()
            .map(ModelMapper::toDomainWorkItem);
    }
    
    /**
     * Gets the current work in progress for a user as a CLI model WorkItem.
     *
     * @param user the user name
     * @return an Optional containing the work item in progress, or empty if none
     */
    public Optional<WorkItem> getCurrentCliWorkInProgress(String user) {
        // Find a work item assigned to the user that's in progress
        return workItems.stream()
            .filter(item -> user.equals(item.getAssignee()) && 
                   org.rinna.cli.model.WorkflowState.IN_PROGRESS.equals(item.getState()))
            .findFirst();
    }
    
    @Override
    public org.rinna.domain.model.WorkItem assignWorkItem(UUID itemId, String user, String assignee) throws InvalidTransitionException {
        return assignWorkItem(itemId, user, assignee, null);
    }
    
    @Override
    public org.rinna.domain.model.WorkItem assignWorkItem(UUID itemId, String user, String assignee, String comment) throws InvalidTransitionException {
        // Find the item in our list
        Optional<WorkItem> optItem = workItems.stream()
                .filter(item -> UUID.fromString(item.getId()).equals(itemId))
                .findFirst();
        
        if (optItem.isEmpty()) {
            throw new InvalidTransitionException("Work item not found: " + itemId);
        }
        
        WorkItem item = optItem.get();
        
        // Record the assignment change for history tracking
        String previousAssignee = item.getAssignee();
        
        // Update the assignee
        item.setAssignee(assignee);
        
        // Return the updated domain item
        return ModelMapper.toDomainWorkItem(item);
    }
    
    /**
     * Gets the ID of the current active work item for a user.
     *
     * @param user the user name
     * @return the UUID of the current active work item, or null if none
     */
    @Override
    public UUID getCurrentActiveItemId(String user) {
        // Return the ID of the first in-progress item
        Optional<WorkItem> activeItem = getCurrentCliWorkInProgress(user);
        return activeItem.map(item -> UUID.fromString(item.getId())).orElse(null);
    }
}