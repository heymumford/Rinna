/*
 * Service component for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.service.impl;

import org.rinna.model.DefaultWorkItem;
import org.rinna.model.WorkItem;
import org.rinna.model.WorkflowState;
import org.rinna.service.InvalidTransitionException;
import org.rinna.service.ItemService;
import org.rinna.service.WorkflowService;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of the WorkflowService interface.
 */
public class DefaultWorkflowService implements WorkflowService {
    private final ItemService itemService;
    private final Map<WorkflowState, List<WorkflowState>> validTransitions;
    
    /**
     * Constructs a new DefaultWorkflowService with the specified ItemService.
     *
     * @param itemService the item service to use
     */
    public DefaultWorkflowService(ItemService itemService) {
        this.itemService = itemService;
        this.validTransitions = Map.of(
            WorkflowState.FOUND, List.of(WorkflowState.TRIAGED),
            WorkflowState.TRIAGED, List.of(WorkflowState.TO_DO),
            WorkflowState.TO_DO, List.of(WorkflowState.IN_PROGRESS),
            WorkflowState.IN_PROGRESS, List.of(WorkflowState.IN_TEST),
            WorkflowState.IN_TEST, List.of(WorkflowState.DONE),
            WorkflowState.DONE, Collections.emptyList()
        );
    }
    
    @Override
    public WorkItem transition(UUID itemId, WorkflowState targetState) throws InvalidTransitionException {
        WorkItem item = itemService.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Item not found: " + itemId));
        
        if (!canTransition(item, targetState)) {
            throw new InvalidTransitionException(
                    "Cannot transition from " + item.getStatus() + " to " + targetState);
        }
        
        DefaultWorkItem updatedItem = ((DefaultWorkItem) item).withStatus(targetState);
        return ((InMemoryItemService) itemService).update(updatedItem);
    }
    
    @Override
    public boolean canTransition(UUID itemId, WorkflowState targetState) {
        Optional<WorkItem> optionalItem = itemService.findById(itemId);
        return optionalItem.isPresent() && canTransition(optionalItem.get(), targetState);
    }
    
    private boolean canTransition(WorkItem item, WorkflowState targetState) {
        WorkflowState currentState = item.getStatus();
        List<WorkflowState> allowedTransitions = validTransitions.getOrDefault(currentState, 
                Collections.emptyList());
        return allowedTransitions.contains(targetState);
    }
    
    @Override
    public List<WorkflowState> getAvailableTransitions(UUID itemId) {
        return itemService.findById(itemId)
                .map(item -> validTransitions.getOrDefault(item.getStatus(), Collections.emptyList()))
                .orElse(Collections.emptyList());
    }
}