/*
 * InMemoryDependencyRepository - In-memory implementation of DependencyRepository
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.repository;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.rinna.domain.WorkItemDependency;
import org.rinna.repository.DependencyRepository;

/**
 * In-memory implementation of the DependencyRepository interface.
 */
public class InMemoryDependencyRepository implements DependencyRepository {

    private final Map<UUID, WorkItemDependency> dependencies = new ConcurrentHashMap<>();
    
    @Override
    public Optional<WorkItemDependency> findById(UUID id) {
        return Optional.ofNullable(dependencies.get(id));
    }
    
    @Override
    public WorkItemDependency save(WorkItemDependency dependency) {
        dependencies.put(dependency.getId(), dependency);
        return dependency;
    }
    
    @Override
    public boolean remove(UUID dependentId, UUID dependencyId) {
        Optional<WorkItemDependency> dependency = findByWorkItems(dependentId, dependencyId);
        
        if (dependency.isPresent()) {
            dependencies.remove(dependency.get().getId());
            return true;
        }
        
        return false;
    }
    
    @Override
    public List<WorkItemDependency> findAll() {
        return new ArrayList<>(dependencies.values());
    }
    
    @Override
    public List<WorkItemDependency> findIncomingDependencies(UUID workItemId) {
        return dependencies.values().stream()
                .filter(d -> d.getDependentId().equals(workItemId))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<WorkItemDependency> findOutgoingDependencies(UUID workItemId) {
        return dependencies.values().stream()
                .filter(d -> d.getDependencyId().equals(workItemId))
                .collect(Collectors.toList());
    }
    
    @Override
    public Optional<WorkItemDependency> findByWorkItems(UUID dependentId, UUID dependencyId) {
        return dependencies.values().stream()
                .filter(d -> d.getDependentId().equals(dependentId) && d.getDependencyId().equals(dependencyId))
                .findFirst();
    }
}