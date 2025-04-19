/*
 * In-memory implementation of AIFieldConfidenceRepository
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository.ai;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.rinna.domain.model.ai.AIFieldConfidence;
import org.rinna.domain.model.ai.AIUserFeedback;
import org.rinna.domain.repository.ai.AIFieldConfidenceRepository;

/**
 * In-memory implementation of AIFieldConfidenceRepository.
 */
public class InMemoryAIFieldConfidenceRepository implements AIFieldConfidenceRepository {

    private final Map<UUID, AIFieldConfidence> confidenceRecords = new ConcurrentHashMap<>();
    private final Map<String, Map<String, AIFieldConfidence>> confidenceByFieldAndModel = new ConcurrentHashMap<>();

    @Override
    public AIFieldConfidence save(AIFieldConfidence fieldConfidence) {
        confidenceRecords.put(fieldConfidence.id(), fieldConfidence);
        
        confidenceByFieldAndModel
                .computeIfAbsent(fieldConfidence.fieldName(), k -> new ConcurrentHashMap<>())
                .put(fieldConfidence.modelId(), fieldConfidence);
        
        return fieldConfidence;
    }

    @Override
    public Optional<AIFieldConfidence> findById(UUID id) {
        return Optional.ofNullable(confidenceRecords.get(id));
    }

    @Override
    public Optional<AIFieldConfidence> findByFieldNameAndModelId(String fieldName, String modelId) {
        return Optional.ofNullable(
                confidenceByFieldAndModel.getOrDefault(fieldName, Map.of())
                        .get(modelId));
    }

    @Override
    public List<AIFieldConfidence> findByModelId(String modelId) {
        return confidenceRecords.values().stream()
                .filter(fc -> fc.modelId().equals(modelId))
                .collect(Collectors.toList());
    }

    @Override
    public List<AIFieldConfidence> findByFieldName(String fieldName) {
        Map<String, AIFieldConfidence> modelMap = confidenceByFieldAndModel.get(fieldName);
        if (modelMap == null) {
            return List.of();
        }
        return List.copyOf(modelMap.values());
    }

    @Override
    public List<AIFieldConfidence> findAll() {
        return List.copyOf(confidenceRecords.values());
    }

    @Override
    public List<AIFieldConfidence> findByMinimumAcceptanceRate(double minAcceptanceRate) {
        return confidenceRecords.values().stream()
                .filter(fc -> fc.acceptanceRate() >= minAcceptanceRate)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AIFieldConfidence> updateWithFeedback(
            String fieldName, String modelId, AIUserFeedback.FeedbackType feedbackType) {
        return findByFieldNameAndModelId(fieldName, modelId)
                .map(fc -> {
                    AIFieldConfidence updated = fc.withFeedback(feedbackType);
                    save(updated);
                    return updated;
                });
    }

    @Override
    public void delete(UUID id) {
        findById(id).ifPresent(fc -> {
            confidenceRecords.remove(id);
            
            Map<String, AIFieldConfidence> modelMap = confidenceByFieldAndModel.get(fc.fieldName());
            if (modelMap != null) {
                modelMap.remove(fc.modelId());
                
                if (modelMap.isEmpty()) {
                    confidenceByFieldAndModel.remove(fc.fieldName());
                }
            }
        });
    }
}