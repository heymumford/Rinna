/*
 * In-memory implementation of AIPredictionRepository
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

import org.rinna.domain.model.ai.AISmartFieldPrediction;
import org.rinna.domain.repository.ai.AIPredictionRepository;

/**
 * In-memory implementation of AIPredictionRepository.
 */
public class InMemoryAIPredictionRepository implements AIPredictionRepository {

    private final Map<UUID, AISmartFieldPrediction> predictions = new ConcurrentHashMap<>();

    @Override
    public AISmartFieldPrediction save(AISmartFieldPrediction prediction) {
        predictions.put(prediction.id(), prediction);
        return prediction;
    }

    @Override
    public Optional<AISmartFieldPrediction> findById(UUID id) {
        return Optional.ofNullable(predictions.get(id));
    }

    @Override
    public List<AISmartFieldPrediction> findByWorkItemId(UUID workItemId) {
        return predictions.values().stream()
                .filter(prediction -> prediction.workItemId().equals(workItemId))
                .collect(Collectors.toList());
    }

    @Override
    public List<AISmartFieldPrediction> findByWorkItemIdAndFieldName(UUID workItemId, String fieldName) {
        return predictions.values().stream()
                .filter(prediction -> prediction.workItemId().equals(workItemId) 
                        && prediction.fieldName().equals(fieldName))
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AISmartFieldPrediction> updateAcceptanceStatus(UUID predictionId, boolean accepted) {
        return findById(predictionId)
                .map(prediction -> {
                    AISmartFieldPrediction updatedPrediction = prediction.withUserAccepted(accepted);
                    predictions.put(predictionId, updatedPrediction);
                    return updatedPrediction;
                });
    }

    @Override
    public List<AISmartFieldPrediction> findMostRecentByWorkItemId(UUID workItemId, int limit) {
        return predictions.values().stream()
                .filter(prediction -> prediction.workItemId().equals(workItemId))
                .sorted(Comparator.comparing(AISmartFieldPrediction::createdAt).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public void delete(UUID id) {
        predictions.remove(id);
    }

    @Override
    public void deleteByWorkItemId(UUID workItemId) {
        List<UUID> toDelete = new ArrayList<>();
        
        for (AISmartFieldPrediction prediction : predictions.values()) {
            if (prediction.workItemId().equals(workItemId)) {
                toDelete.add(prediction.id());
            }
        }
        
        toDelete.forEach(predictions::remove);
    }
}