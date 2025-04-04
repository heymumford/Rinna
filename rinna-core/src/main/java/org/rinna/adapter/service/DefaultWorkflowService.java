/*
 * Service implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import org.rinna.domain.entity.DefaultWorkItem;
import org.rinna.domain.entity.WorkItem;
import org.rinna.domain.entity.WorkflowState;
import org.rinna.domain.repository.ItemRepository;
import org.rinna.domain.usecase.InvalidTransitionException;
import org.rinna.domain.usecase.WorkflowService;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Default implementation of the WorkflowService interface.
 */
public class DefaultWorkflowService implements WorkflowService {
    private final ItemRepository itemRepository;
    
    /**
     * Constructs a new DefaultWorkflowService with the given repository.
     * 
     * @param itemRepository the item repository
     */
    public DefaultWorkflowService(ItemRepository itemRepository) {
        this.itemRepository = Objects.requireNonNull(itemRepository, "Item repository cannot be null");
    }
    
    @Override
    public WorkItem transition(UUID itemId, WorkflowState targetState) 
            throws InvalidTransitionException {
        Objects.requireNonNull(itemId, "Item ID cannot be null");
        Objects.requireNonNull(targetState, "Target state cannot be null");
        
        WorkItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found: " + itemId));
        
        WorkflowState currentState = item.getStatus();
        
        if (!canTransition(currentState, targetState)) {
            throw new InvalidTransitionException(itemId, currentState, targetState);
        }
        
        if (item instanceof DefaultWorkItem) {
            DefaultWorkItem defaultItem = (DefaultWorkItem) item;
            defaultItem.setStatus(targetState);
            return itemRepository.save(defaultItem);
        } else {
            throw new UnsupportedOperationException("Cannot transition non-default work item");
        }
    }
    
    @Override
    public boolean canTransition(UUID itemId, WorkflowState targetState) {
        Objects.requireNonNull(itemId, "Item ID cannot be null");
        Objects.requireNonNull(targetState, "Target state cannot be null");
        
        return itemRepository.findById(itemId)
                .map(item -> canTransition(item.getStatus(), targetState))
                .orElse(false);
    }
    
    /**
     * Checks if a transition from the current state to the target state is valid.
     * 
     * @param currentState the current workflow state
     * @param targetState the target workflow state
     * @return true if the transition is valid, false otherwise
     */
    private boolean canTransition(WorkflowState currentState, WorkflowState targetState) {
        return currentState.canTransitionTo(targetState);
    }
    
    @Override
    public List<WorkflowState> getAvailableTransitions(UUID itemId) {
        Objects.requireNonNull(itemId, "Item ID cannot be null");
        
        return itemRepository.findById(itemId)
                .map(item -> {
                    WorkflowState currentState = item.getStatus();
                    return Arrays.stream(WorkflowState.values())
                            .filter(state -> canTransition(currentState, state))
                            .collect(Collectors.toList());
                })
                .orElse(List.of());
    }
}