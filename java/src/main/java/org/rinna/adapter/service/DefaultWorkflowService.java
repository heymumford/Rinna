/*
 * Service implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

import org.rinna.domain.model.CommentType;
import org.rinna.domain.model.DefaultWorkItem;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemRecord;
import org.rinna.domain.model.WorkflowState;
import org.rinna.repository.ItemRepository;
import org.rinna.usecase.CommentService;
import org.rinna.usecase.HistoryService;
import org.rinna.usecase.InvalidTransitionException;
import org.rinna.usecase.WorkflowService;

/**
 * Default implementation of the WorkflowService interface.
 */
public class DefaultWorkflowService implements WorkflowService {
    private final ItemRepository itemRepository;
    private final CommentService commentService;
    private final HistoryService historyService;
    
    /**
     * Constructs a new DefaultWorkflowService with the given repositories and services.
     */
    public DefaultWorkflowService(
            ItemRepository itemRepository,
            CommentService commentService,
            HistoryService historyService) {
        this.itemRepository = Objects.requireNonNull(itemRepository, "Item repository cannot be null");
        this.commentService = Objects.requireNonNull(commentService, "Comment service cannot be null");
        this.historyService = Objects.requireNonNull(historyService, "History service cannot be null");
    }
    
    @Override
    public WorkItem transition(UUID itemId, WorkflowState targetState) throws InvalidTransitionException {
        return transition(itemId, "System", targetState, null);
    }
    
    @Override
    public WorkItem transition(UUID itemId, String user, WorkflowState targetState, String comment) throws InvalidTransitionException {
        Objects.requireNonNull(itemId, "Item ID cannot be null");
        Objects.requireNonNull(user, "User cannot be null");
        Objects.requireNonNull(targetState, "Target state cannot be null");
        
        WorkItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found: " + itemId));
        
        WorkflowState currentState = item.getStatus();
        
        if (!currentState.canTransitionTo(targetState)) {
            throw new InvalidTransitionException(itemId, currentState, targetState);
        }
        
        // Check if transitioning to IN_PROGRESS and user already has work in progress
        if (targetState == WorkflowState.IN_PROGRESS && !currentState.equals(WorkflowState.IN_PROGRESS)) {
            List<WorkItem> inProgressItems = itemRepository.findByAssigneeAndStatus(user, WorkflowState.IN_PROGRESS);
            if (!inProgressItems.isEmpty() && !inProgressItems.stream().anyMatch(wi -> wi.getId().equals(itemId))) {
                WorkItem wipItem = inProgressItems.get(0);
                throw new InvalidTransitionException(
                    itemId, 
                    currentState, 
                    targetState, 
                    "User " + user + " already has work item " + wipItem.getId() + " in progress");
            }
        }
        
        // Handle both default and record implementations
        WorkItem updatedItem = switch (item) {
            case WorkItemRecord record -> record.withStatus(targetState);
            case DefaultWorkItem defaultItem -> defaultItem.toRecord().withStatus(targetState);
            case null -> throw new IllegalArgumentException("Item cannot be null");
            default -> throw new UnsupportedOperationException("Unsupported WorkItem implementation: " + item.getClass().getName());
        };
        
        WorkItem savedItem = itemRepository.save(updatedItem);
        
        // Record comment if provided
        if (comment != null && !comment.isBlank()) {
            commentService.addTransitionComment(
                itemId,
                user,
                currentState.name(),
                targetState.name(),
                comment
            );
        }
        
        // Record history entry
        historyService.recordStateChange(
            itemId,
            user,
            currentState.name(),
            targetState.name(),
            comment
        );
        
        return savedItem;
    }
    
    @Override
    public boolean canTransition(UUID itemId, WorkflowState targetState) {
        Objects.requireNonNull(itemId, "Item ID cannot be null");
        Objects.requireNonNull(targetState, "Target state cannot be null");
        
        return itemRepository.findById(itemId)
                .map(item -> item.getStatus().canTransitionTo(targetState))
                .orElse(false);
    }
    
    @Override
    public List<WorkflowState> getAvailableTransitions(UUID itemId) {
        Objects.requireNonNull(itemId, "Item ID cannot be null");
        
        return itemRepository.findById(itemId)
                .map(item -> item.getStatus().getAvailableTransitions())
                .orElse(List.of());
    }
    
    @Override
    public Optional<WorkItem> getCurrentWorkInProgress(String user) {
        List<WorkItem> inProgressItems = itemRepository.findByAssigneeAndStatus(user, WorkflowState.IN_PROGRESS);
        return inProgressItems.isEmpty() ? Optional.empty() : Optional.of(inProgressItems.get(0));
    }
    
    @Override
    public WorkItem assignWorkItem(UUID itemId, String user, String assignee) throws InvalidTransitionException {
        return assignWorkItem(itemId, user, assignee, null);
    }
    
    @Override
    public WorkItem assignWorkItem(UUID itemId, String user, String assignee, String comment) throws InvalidTransitionException {
        Objects.requireNonNull(itemId, "Item ID cannot be null");
        Objects.requireNonNull(user, "User cannot be null");
        
        WorkItem item = itemRepository.findById(itemId)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found: " + itemId));
        
        // Check if the work item is already in progress by another user
        if (item.getStatus() == WorkflowState.IN_PROGRESS && 
            item.getAssignee() != null && 
            !item.getAssignee().equals(assignee) && 
            !item.getAssignee().equals(user)) {
            
            throw new InvalidTransitionException(
                itemId, 
                item.getStatus(), 
                item.getStatus(), 
                "Cannot reassign work item in progress by " + item.getAssignee());
        }
        
        String oldAssignee = item.getAssignee();
        
        // Handle both default and record implementations
        WorkItem updatedItem = switch (item) {
            case WorkItemRecord record -> record.withAssignee(assignee);
            case DefaultWorkItem defaultItem -> defaultItem.toRecord().withAssignee(assignee);
            case null -> throw new IllegalArgumentException("Item cannot be null");
            default -> throw new UnsupportedOperationException("Unsupported WorkItem implementation: " + item.getClass().getName());
        };
        
        WorkItem savedItem = itemRepository.save(updatedItem);
        
        // Record comment if provided
        if (comment != null && !comment.isBlank()) {
            commentService.addComment(itemId, user, comment, CommentType.ASSIGNMENT_CHANGE);
        }
        
        // Record history entry
        historyService.recordFieldChange(
            itemId,
            user,
            "assignee",
            oldAssignee == null ? "none" : oldAssignee,
            assignee == null ? "none" : assignee
        );
        
        return savedItem;
    }
}