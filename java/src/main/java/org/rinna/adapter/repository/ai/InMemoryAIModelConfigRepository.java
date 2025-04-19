/*
 * In-memory implementation of AIModelConfigRepository
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

import org.rinna.domain.model.ai.AIModelConfig;
import org.rinna.domain.repository.ai.AIModelConfigRepository;

/**
 * In-memory implementation of AIModelConfigRepository.
 */
public class InMemoryAIModelConfigRepository implements AIModelConfigRepository {

    private final Map<UUID, AIModelConfig> modelConfigs = new ConcurrentHashMap<>();
    private final Map<String, AIModelConfig> modelConfigsByModelId = new ConcurrentHashMap<>();

    @Override
    public AIModelConfig save(AIModelConfig modelConfig) {
        modelConfigs.put(modelConfig.id(), modelConfig);
        modelConfigsByModelId.put(modelConfig.modelId(), modelConfig);
        return modelConfig;
    }

    @Override
    public Optional<AIModelConfig> findById(UUID id) {
        return Optional.ofNullable(modelConfigs.get(id));
    }

    @Override
    public Optional<AIModelConfig> findByModelId(String modelId) {
        return Optional.ofNullable(modelConfigsByModelId.get(modelId));
    }

    @Override
    public List<AIModelConfig> findByModelType(String modelType) {
        return modelConfigs.values().stream()
                .filter(mc -> mc.modelType().equals(modelType))
                .collect(Collectors.toList());
    }

    @Override
    public List<AIModelConfig> findAllEnabled() {
        return modelConfigs.values().stream()
                .filter(AIModelConfig::enabled)
                .collect(Collectors.toList());
    }

    @Override
    public List<AIModelConfig> findAll() {
        return List.copyOf(modelConfigs.values());
    }

    @Override
    public Optional<AIModelConfig> updateEnabledStatus(String modelId, boolean enabled) {
        return findByModelId(modelId)
                .map(mc -> {
                    AIModelConfig updated = mc.withEnabled(enabled);
                    save(updated);
                    return updated;
                });
    }

    @Override
    public Optional<AIModelConfig> updateParameters(String modelId, Map<String, Object> parameters) {
        return findByModelId(modelId)
                .map(mc -> {
                    AIModelConfig updated = mc.withParameters(parameters);
                    save(updated);
                    return updated;
                });
    }

    @Override
    public Optional<AIModelConfig> updateSupportedFields(String modelId, List<String> supportedFields) {
        return findByModelId(modelId)
                .map(mc -> {
                    AIModelConfig updated = mc.withSupportedFields(supportedFields);
                    save(updated);
                    return updated;
                });
    }

    @Override
    public void delete(UUID id) {
        findById(id).ifPresent(mc -> {
            modelConfigs.remove(id);
            modelConfigsByModelId.remove(mc.modelId());
        });
    }
}