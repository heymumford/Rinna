/*
 * In-memory repository implementation for the Rinna unified work management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.OriginCategory;
import org.rinna.domain.model.UnifiedWorkItem;
import org.rinna.domain.model.WorkParadigm;
import org.rinna.domain.model.WorkflowState;
import org.rinna.domain.repository.UnifiedWorkItemRepository;

/**
 * In-memory implementation of UnifiedWorkItemRepository.
 * This class provides an in-memory storage and retrieval mechanism for unified work items.
 * It is designed for development and testing purposes.
 */
public class InMemoryUnifiedWorkItemRepository implements UnifiedWorkItemRepository {
    private final Map<UUID, UnifiedWorkItem> workItemsById = new ConcurrentHashMap<>();

    @Override
    public UnifiedWorkItem save(UnifiedWorkItem item) {
        workItemsById.put(item.id(), item);
        return item;
    }

    @Override
    public Optional<UnifiedWorkItem> findById(UUID id) {
        return Optional.ofNullable(workItemsById.get(id));
    }

    @Override
    public List<UnifiedWorkItem> findAll() {
        return new ArrayList<>(workItemsById.values());
    }

    @Override
    public List<UnifiedWorkItem> findByTitleContaining(String title) {
        if (title == null || title.isEmpty()) {
            return List.of();
        }
        
        String lowerCaseTitle = title.toLowerCase();
        return workItemsById.values().stream()
                .filter(item -> item.title().toLowerCase().contains(lowerCaseTitle))
                .collect(Collectors.toList());
    }

    @Override
    public List<UnifiedWorkItem> findByState(WorkflowState state) {
        if (state == null) {
            return List.of();
        }
        
        return workItemsById.values().stream()
                .filter(item -> state.equals(item.state()))
                .collect(Collectors.toList());
    }

    @Override
    public List<UnifiedWorkItem> findByAssignee(String assignee) {
        if (assignee == null || assignee.isEmpty()) {
            return List.of();
        }
        
        return workItemsById.values().stream()
                .filter(item -> assignee.equals(item.assignee()))
                .collect(Collectors.toList());
    }

    @Override
    public List<UnifiedWorkItem> findByOriginCategory(OriginCategory category) {
        if (category == null) {
            return List.of();
        }
        
        return workItemsById.values().stream()
                .filter(item -> category.equals(item.originCategory()))
                .collect(Collectors.toList());
    }

    @Override
    public List<UnifiedWorkItem> findByCynefinDomain(CynefinDomain domain) {
        if (domain == null) {
            return List.of();
        }
        
        return workItemsById.values().stream()
                .filter(item -> domain.equals(item.cynefinDomain()))
                .collect(Collectors.toList());
    }

    @Override
    public List<UnifiedWorkItem> findByWorkParadigm(WorkParadigm paradigm) {
        if (paradigm == null) {
            return List.of();
        }
        
        return workItemsById.values().stream()
                .filter(item -> paradigm.equals(item.workParadigm()))
                .collect(Collectors.toList());
    }

    @Override
    public List<UnifiedWorkItem> findByAnyTag(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        
        return workItemsById.values().stream()
                .filter(item -> {
                    List<String> itemTags = item.tags();
                    if (itemTags == null || itemTags.isEmpty()) {
                        return false;
                    }
                    return tags.stream().anyMatch(itemTags::contains);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<UnifiedWorkItem> findByAllTags(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        
        return workItemsById.values().stream()
                .filter(item -> {
                    List<String> itemTags = item.tags();
                    if (itemTags == null || itemTags.isEmpty()) {
                        return false;
                    }
                    return itemTags.containsAll(tags);
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<UnifiedWorkItem> findByCognitiveLoadGreaterThanEqual(int minimumLoad) {
        return workItemsById.values().stream()
                .filter(item -> item.cognitiveLoad() >= minimumLoad)
                .collect(Collectors.toList());
    }

    @Override
    public List<UnifiedWorkItem> findByCognitiveLoadLessThanEqual(int maximumLoad) {
        return workItemsById.values().stream()
                .filter(item -> item.cognitiveLoad() <= maximumLoad)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteById(UUID id) {
        workItemsById.remove(id);
    }
}