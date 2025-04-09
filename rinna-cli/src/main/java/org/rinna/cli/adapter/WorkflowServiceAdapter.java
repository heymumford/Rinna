/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.adapter;

import org.rinna.cli.domain.model.DomainWorkItem;
import org.rinna.cli.domain.model.DomainWorkflowState;
import org.rinna.cli.domain.service.InvalidTransitionException;
import org.rinna.cli.domain.service.WorkflowService;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.util.ModelMapper;
import org.rinna.cli.util.StateMapper;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter class that converts between CLI and domain workflow service models.
 * This adapter implements the domain WorkflowService interface while delegating
 * to the CLI's MockWorkflowService implementation.
 */
public class WorkflowServiceAdapter implements WorkflowService {
    
    private final MockWorkflowService mockWorkflowService;
    
    /**
     * Creates a new WorkflowServiceAdapter.
     * 
     * @param mockWorkflowService the underlying mock workflow service
     */
    public WorkflowServiceAdapter(MockWorkflowService mockWorkflowService) {
        this.mockWorkflowService = mockWorkflowService;
    }
    
    @Override
    public DomainWorkItem transition(UUID itemId, DomainWorkflowState targetState) throws InvalidTransitionException {
        try {
            // Convert domain state to CLI state
            WorkflowState cliTargetState = StateMapper.fromDomainState(targetState);
            
            // Call the CLI service
            WorkItem updatedItem = mockWorkflowService.transition(
                itemId.toString(), 
                cliTargetState
            );
            
            // Convert the result back to a domain work item
            return ModelMapper.toDomainWorkItem(updatedItem);
        } catch (org.rinna.cli.service.InvalidTransitionException e) {
            // Map CLI exception to domain exception
            throw new InvalidTransitionException(e.getMessage());
        }
    }
    
    @Override
    public DomainWorkItem transition(UUID itemId, String user, DomainWorkflowState targetState, String comment) 
            throws InvalidTransitionException {
        try {
            // Convert domain state to CLI state
            WorkflowState cliTargetState = StateMapper.fromDomainState(targetState);
            
            // Call the CLI service
            WorkItem updatedItem = mockWorkflowService.transition(
                itemId.toString(), 
                user, 
                cliTargetState, 
                comment
            );
            
            // Convert the result back to a domain work item
            return ModelMapper.toDomainWorkItem(updatedItem);
        } catch (org.rinna.cli.service.InvalidTransitionException e) {
            // Map CLI exception to domain exception
            throw new InvalidTransitionException(e.getMessage());
        }
    }
    
    @Override
    public boolean canTransition(UUID itemId, DomainWorkflowState targetState) {
        // Convert domain state to CLI state
        WorkflowState cliTargetState = StateMapper.fromDomainState(targetState);
        
        // Call the CLI service
        return mockWorkflowService.canTransition(itemId.toString(), cliTargetState);
    }
    
    @Override
    public List<DomainWorkflowState> getAvailableTransitions(UUID itemId) {
        // Get available transitions from CLI service
        List<WorkflowState> cliStates = mockWorkflowService.getAvailableTransitions(itemId.toString());
        
        // Convert CLI states to domain states
        return cliStates.stream()
                .map(StateMapper::toDomainState)
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<DomainWorkItem> getCurrentWorkInProgress(String user) {
        // Call the CLI service
        Optional<WorkItem> cliItem = mockWorkflowService.getCurrentWorkInProgress(user);
        
        // Convert the result to a domain work item if present
        return cliItem.map(ModelMapper::toDomainWorkItem);
    }
    
    @Override
    public DomainWorkItem assignWorkItem(UUID itemId, String user, String assignee) throws InvalidTransitionException {
        try {
            // Call the CLI service
            WorkItem updatedItem = mockWorkflowService.assignWorkItem(
                itemId.toString(), 
                user, 
                assignee
            );
            
            // Convert the result back to a domain work item
            return ModelMapper.toDomainWorkItem(updatedItem);
        } catch (org.rinna.cli.service.InvalidTransitionException e) {
            // Map CLI exception to domain exception
            throw new InvalidTransitionException(e.getMessage());
        }
    }
    
    @Override
    public DomainWorkItem assignWorkItem(UUID itemId, String user, String assignee, String comment) 
            throws InvalidTransitionException {
        try {
            // Call the CLI service
            WorkItem updatedItem = mockWorkflowService.assignWorkItem(
                itemId.toString(), 
                user, 
                assignee, 
                comment
            );
            
            // Convert the result back to a domain work item
            return ModelMapper.toDomainWorkItem(updatedItem);
        } catch (org.rinna.cli.service.InvalidTransitionException e) {
            // Map CLI exception to domain exception
            throw new InvalidTransitionException(e.getMessage());
        }
    }
    
    @Override
    public UUID getCurrentActiveItemId(String user) {
        // Call the CLI service directly
        return mockWorkflowService.getCurrentActiveItemId(user);
    }
}