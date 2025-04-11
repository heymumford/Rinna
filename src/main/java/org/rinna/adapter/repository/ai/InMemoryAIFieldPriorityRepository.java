/*
 * In-memory implementation of AIFieldPriorityRepository
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository.ai;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.rinna.domain.model.ai.AIFieldPriority;
import org.rinna.domain.repository.ai.AIFieldPriorityRepository;

/**
 * In-memory implementation of AIFieldPriorityRepository.
 */
public class InMemoryAIFieldPriorityRepository implements AIFieldPriorityRepository {

    private final Map<UUID, AIFieldPriority> fieldPriorities = new ConcurrentHashMap<>();
    private final Map<String, AIFieldPriority> fieldPrioritiesByName = new ConcurrentHashMap<>();

    @Override
    public AIFieldPriority save(AIFieldPriority fieldPriority) {
        fieldPriorities.put(fieldPriority.id(), fieldPriority);
        fieldPrioritiesByName.put(fieldPriority.fieldName(), fieldPriority);
        return fieldPriority;
    }

    @Override
    public Optional<AIFieldPriority> findById(UUID id) {
        return Optional.ofNullable(fieldPriorities.get(id));
    }

    @Override
    public Optional<AIFieldPriority> findByFieldName(String fieldName) {
        return Optional.ofNullable(fieldPrioritiesByName.get(fieldName));
    }

    @Override
    public List<AIFieldPriority> findAll() {
        return List.copyOf(fieldPriorities.values());
    }

    @Override
    public List<AIFieldPriority> findByMinimumPriorityScore(double minPriorityScore) {
        return fieldPriorities.values().stream()
                .filter(fp -> fp.priorityScore() >= minPriorityScore)
                .collect(Collectors.toList());
    }

    @Override
    public List<AIFieldPriority> findAllSortedByPriorityScore(int limit) {
        return fieldPriorities.values().stream()
                .sorted(Comparator.comparing(AIFieldPriority::priorityScore).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public Optional<AIFieldPriority> incrementUsageCount(String fieldName) {
        return findByFieldName(fieldName)
                .map(fp -> {
                    AIFieldPriority updated = fp.withIncrementedUsage();
                    save(updated);
                    return updated;
                });
    }

    @Override
    public Optional<AIFieldPriority> updateCompletionRate(String fieldName, double completionRate) {
        return findByFieldName(fieldName)
                .map(fp -> {
                    AIFieldPriority updated = fp.withCompletionRate(completionRate);
                    save(updated);
                    return updated;
                });
    }

    @Override
    public Optional<AIFieldPriority> updateValueRating(String fieldName, double valueRating) {
        return findByFieldName(fieldName)
                .map(fp -> {
                    AIFieldPriority updated = fp.withValueRating(valueRating);
                    save(updated);
                    return updated;
                });
    }

    @Override
    public Optional<AIFieldPriority> recalculatePriorityScore(String fieldName) {
        return findByFieldName(fieldName)
                .map(fp -> {
                    AIFieldPriority updated = fp.withRecalculatedPriority();
                    save(updated);
                    return updated;
                });
    }

    @Override
    public void delete(UUID id) {
        findById(id).ifPresent(fp -> {
            fieldPriorities.remove(id);
            fieldPrioritiesByName.remove(fp.fieldName());
        });
    }
}