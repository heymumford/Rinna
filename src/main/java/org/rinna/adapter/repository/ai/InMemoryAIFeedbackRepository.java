/*
 * In-memory implementation of AIFeedbackRepository
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.rinna.domain.model.ai.AIUserFeedback;
import org.rinna.domain.repository.ai.AIFeedbackRepository;

/**
 * In-memory implementation of AIFeedbackRepository.
 */
public class InMemoryAIFeedbackRepository implements AIFeedbackRepository {

    private final Map<UUID, AIUserFeedback> feedback = new ConcurrentHashMap<>();

    @Override
    public AIUserFeedback save(AIUserFeedback feedback) {
        this.feedback.put(feedback.id(), feedback);
        return feedback;
    }

    @Override
    public Optional<AIUserFeedback> findById(UUID id) {
        return Optional.ofNullable(feedback.get(id));
    }

    @Override
    public List<AIUserFeedback> findByPredictionId(UUID predictionId) {
        return feedback.values().stream()
                .filter(f -> f.predictionId().equals(predictionId))
                .collect(Collectors.toList());
    }

    @Override
    public List<AIUserFeedback> findByUserId(UUID userId) {
        return feedback.values().stream()
                .filter(f -> f.userId().equals(userId))
                .collect(Collectors.toList());
    }

    @Override
    public List<AIUserFeedback> findByType(AIUserFeedback.FeedbackType type) {
        return feedback.values().stream()
                .filter(f -> f.type() == type)
                .collect(Collectors.toList());
    }

    @Override
    public List<AIUserFeedback> findMostRecent(int limit) {
        return feedback.values().stream()
                .sorted(Comparator.comparing(AIUserFeedback::createdAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id) {
        feedback.remove(id);
    }

    @Override
    public void deleteByPredictionId(UUID predictionId) {
        List<UUID> toDelete = new ArrayList<>();
        
        for (AIUserFeedback f : feedback.values()) {
            if (f.predictionId().equals(predictionId)) {
                toDelete.add(f.id());
            }
        }
        
        toDelete.forEach(feedback::remove);
    }
}