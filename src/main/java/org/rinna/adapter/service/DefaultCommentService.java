/*
 * Service implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import org.rinna.domain.model.Comment;
import org.rinna.domain.model.CommentRecord;
import org.rinna.domain.model.CommentType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.repository.CommentRepository;
import org.rinna.usecase.CommentService;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Default implementation of the CommentService interface.
 */
public class DefaultCommentService implements CommentService {
    private final CommentRepository commentRepository;
    
    /**
     * Constructs a DefaultCommentService with the given repository.
     *
     * @param commentRepository the comment repository to use
     */
    public DefaultCommentService(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }
    
    @Override
    public Comment addComment(UUID workItemId, String author, String text, CommentType type) {
        Comment comment = CommentRecord.create(workItemId, author, text, type);
        return commentRepository.save(comment);
    }
    
    @Override
    public Comment addComment(UUID workItemId, String author, String text) {
        return addComment(workItemId, author, text, CommentType.STANDARD);
    }
    
    @Override
    public Comment addTransitionComment(UUID workItemId, String author, String fromState, String toState, String comment) {
        WorkflowState fromStateEnum = WorkflowState.valueOf(fromState);
        WorkflowState toStateEnum = WorkflowState.valueOf(toState);
        Comment transitionComment = CommentRecord.createTransition(workItemId, author, fromStateEnum, toStateEnum, comment);
        return commentRepository.save(transitionComment);
    }
    
    @Override
    public Optional<Comment> getComment(UUID commentId) {
        return commentRepository.findById(commentId);
    }
    
    @Override
    public List<Comment> getComments(UUID workItemId) {
        return commentRepository.findByWorkItemId(workItemId);
    }
    
    @Override
    public List<Comment> getCommentsInTimeRange(UUID workItemId, Instant from, Instant to) {
        return commentRepository.findByWorkItemIdAndTimeRange(workItemId, from, to);
    }
    
    @Override
    public List<Comment> getCommentsFromLastHours(UUID workItemId, int hours) {
        Instant now = Instant.now();
        Instant from = now.minus(hours, ChronoUnit.HOURS);
        return getCommentsInTimeRange(workItemId, from, now);
    }
    
    @Override
    public List<Comment> getCommentsFromLastDays(UUID workItemId, int days) {
        Instant now = Instant.now();
        Instant from = now.minus(days, ChronoUnit.DAYS);
        return getCommentsInTimeRange(workItemId, from, now);
    }
    
    @Override
    public List<Comment> getCommentsFromLastWeeks(UUID workItemId, int weeks) {
        Instant now = Instant.now();
        Instant from = now.minus(weeks * 7, ChronoUnit.DAYS);
        return getCommentsInTimeRange(workItemId, from, now);
    }
    
    @Override
    public boolean deleteComment(UUID commentId) {
        return commentRepository.deleteById(commentId);
    }
}
