/*
 * Repository implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import org.rinna.domain.model.Comment;
import org.rinna.domain.model.CommentType;
import org.rinna.repository.CommentRepository;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of the CommentRepository interface.
 */
public class InMemoryCommentRepository implements CommentRepository {
    private final Map<UUID, Comment> comments = new ConcurrentHashMap<>();
    
    @Override
    public Comment save(Comment comment) {
        comments.put(comment.id(), comment);
        return comment;
    }
    
    @Override
    public Optional<Comment> findById(UUID id) {
        return Optional.ofNullable(comments.get(id));
    }
    
    @Override
    public List<Comment> findByWorkItemId(UUID workItemId) {
        return comments.values().stream()
            .filter(comment -> comment.workItemId().equals(workItemId))
            .sorted((c1, c2) -> c2.timestamp().compareTo(c1.timestamp())) // Most recent first
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Comment> findByWorkItemIdAndType(UUID workItemId, CommentType type) {
        return comments.values().stream()
            .filter(comment -> comment.workItemId().equals(workItemId) && comment.type() == type)
            .sorted((c1, c2) -> c2.timestamp().compareTo(c1.timestamp())) // Most recent first
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Comment> findByWorkItemIdAndTimeRange(UUID workItemId, Instant from, Instant to) {
        return comments.values().stream()
            .filter(comment -> comment.workItemId().equals(workItemId) && 
                   !comment.timestamp().isBefore(from) && 
                   !comment.timestamp().isAfter(to))
            .sorted((c1, c2) -> c2.timestamp().compareTo(c1.timestamp())) // Most recent first
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Comment> findByAuthor(String author) {
        return comments.values().stream()
            .filter(comment -> comment.author().equals(author))
            .sorted((c1, c2) -> c2.timestamp().compareTo(c1.timestamp())) // Most recent first
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean deleteById(UUID id) {
        return comments.remove(id) != null;
    }
    
    @Override
    public int deleteByWorkItemId(UUID workItemId) {
        List<UUID> toRemove = comments.values().stream()
            .filter(comment -> comment.workItemId().equals(workItemId))
            .map(Comment::id)
            .collect(Collectors.toList());
        
        toRemove.forEach(comments::remove);
        return toRemove.size();
    }
}
