/*
 * Domain service implementation for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.domain.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkflowState;
import org.rinna.domain.service.CriticalPathService;
import org.rinna.domain.service.ItemService;

/**
 * Default implementation of the CriticalPathService interface.
 * This class implements critical path analysis for a project.
 */
public class DefaultCriticalPathService implements CriticalPathService {
    
    private final ItemService itemService;
    
    // Maps dependentId -> Set of blockerIds (items that block this item)
    private final Map<UUID, Set<UUID>> dependencies = new HashMap<>();
    
    // Maps blockerId -> Set of dependentIds (items that depend on this item)
    private final Map<UUID, Set<UUID>> reverseDependencies = new HashMap<>();
    
    /**
     * Constructs a new DefaultCriticalPathService with the given ItemService.
     * 
     * @param itemService the service to use for retrieving work items
     */
    public DefaultCriticalPathService(ItemService itemService) {
        this.itemService = itemService;
    }
    
    @Override
    public List<WorkItem> findBlockingItems() {
        List<WorkItem> allItems = itemService.findAll();
        
        // Find items that block multiple other items and are not completed
        return allItems.stream()
            .filter(item -> {
                // Get the items that depend on this item
                Set<UUID> dependents = reverseDependencies.getOrDefault(item.getId(), Collections.emptySet());
                
                // Check if this item has multiple dependents and is not completed
                return dependents.size() > 1 && 
                       item.getStatus() != WorkflowState.DONE && 
                       item.getStatus() != WorkflowState.RELEASED;
            })
            .sorted(Comparator.comparing(WorkItem::getPriority).reversed()) // Sort by priority (highest first)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<WorkItem> findCriticalPath() {
        List<WorkItem> allItems = itemService.findAll();
        
        if (allItems.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Find items with no dependencies (root items)
        List<WorkItem> rootItems = allItems.stream()
            .filter(item -> !dependencies.containsKey(item.getId()) || 
                           dependencies.get(item.getId()).isEmpty())
            .collect(Collectors.toList());
        
        if (rootItems.isEmpty()) {
            // If we have a cycle, just pick the highest priority item as a starting point
            rootItems = List.of(
                allItems.stream()
                    .max(Comparator.comparing(WorkItem::getPriority))
                    .orElse(allItems.get(0))
            );
        }
        
        // Find the longest path through the dependency graph
        Map<UUID, Double> distances = new HashMap<>();
        Map<UUID, UUID> predecessors = new HashMap<>();
        
        // Initialize all items with distance -Infinity
        for (WorkItem item : allItems) {
            distances.put(item.getId(), Double.NEGATIVE_INFINITY);
        }
        
        // Root items have distance 0
        for (WorkItem rootItem : rootItems) {
            distances.put(rootItem.getId(), 0.0);
        }
        
        // Topological sort of items
        List<WorkItem> sortedItems = topologicalSort(allItems);
        
        // Calculate distances
        for (WorkItem item : sortedItems) {
            double currentDistance = distances.get(item.getId());
            
            // Get items that depend on this item
            Set<UUID> dependents = reverseDependencies.getOrDefault(item.getId(), Collections.emptySet());
            
            for (UUID dependentId : dependents) {
                // Calculate weight based on item priority
                double weight = calculateItemWeight(itemService.findById(dependentId).orElse(null));
                
                double newDistance = currentDistance + weight;
                if (newDistance > distances.getOrDefault(dependentId, Double.NEGATIVE_INFINITY)) {
                    distances.put(dependentId, newDistance);
                    predecessors.put(dependentId, item.getId());
                }
            }
        }
        
        // Find the item with the highest distance (end of critical path)
        UUID endItemId = allItems.stream()
            .map(WorkItem::getId)
            .max(Comparator.comparing(distances::get))
            .orElse(null);
        
        if (endItemId == null) {
            return Collections.emptyList();
        }
        
        // Reconstruct the critical path
        List<UUID> pathIds = new ArrayList<>();
        UUID currentId = endItemId;
        
        while (currentId != null) {
            pathIds.add(currentId);
            currentId = predecessors.get(currentId);
        }
        
        // Reverse the path to get it in the correct order
        Collections.reverse(pathIds);
        
        // Convert IDs to WorkItems
        return pathIds.stream()
            .map(id -> itemService.findById(id).orElse(null))
            .filter(item -> item != null)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<WorkItem> findItemsDependingOn(UUID itemId) {
        // Get the set of items that depend on the specified item
        Set<UUID> dependentIds = reverseDependencies.getOrDefault(itemId, Collections.emptySet());
        
        // Convert the IDs to WorkItems
        return dependentIds.stream()
            .map(id -> itemService.findById(id).orElse(null))
            .filter(item -> item != null)
            .collect(Collectors.toList());
    }
    
    @Override
    public boolean addDependency(UUID dependentId, UUID blockerId) {
        // Ensure the items exist
        if (!itemService.findById(dependentId).isPresent() || 
            !itemService.findById(blockerId).isPresent()) {
            return false;
        }
        
        // Add to dependencies map
        dependencies.computeIfAbsent(dependentId, k -> new HashSet<>()).add(blockerId);
        
        // Add to reverse dependencies map
        reverseDependencies.computeIfAbsent(blockerId, k -> new HashSet<>()).add(dependentId);
        
        return true;
    }
    
    @Override
    public boolean removeDependency(UUID dependentId, UUID blockerId) {
        boolean removed = false;
        
        // Remove from dependencies map
        if (dependencies.containsKey(dependentId)) {
            removed = dependencies.get(dependentId).remove(blockerId);
            
            // Remove empty sets
            if (dependencies.get(dependentId).isEmpty()) {
                dependencies.remove(dependentId);
            }
        }
        
        // Remove from reverse dependencies map
        if (reverseDependencies.containsKey(blockerId)) {
            reverseDependencies.get(blockerId).remove(dependentId);
            
            // Remove empty sets
            if (reverseDependencies.get(blockerId).isEmpty()) {
                reverseDependencies.remove(blockerId);
            }
        }
        
        return removed;
    }
    
    @Override
    public boolean hasDependency(UUID dependentId, UUID blockerId) {
        return dependencies.containsKey(dependentId) && 
               dependencies.get(dependentId).contains(blockerId);
    }
    
    /**
     * Performs a topological sort of work items based on their dependencies.
     * 
     * @param items the list of work items to sort
     * @return a topologically sorted list of work items
     */
    private List<WorkItem> topologicalSort(List<WorkItem> items) {
        List<WorkItem> sorted = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        Set<UUID> visiting = new HashSet<>();
        
        for (WorkItem item : items) {
            if (!visited.contains(item.getId())) {
                topologicalSortVisit(item, items, visited, visiting, sorted);
            }
        }
        
        return sorted;
    }
    
    private void topologicalSortVisit(WorkItem item, List<WorkItem> items, 
                                     Set<UUID> visited, Set<UUID> visiting, 
                                     List<WorkItem> sorted) {
        if (visiting.contains(item.getId())) {
            // Cycle detected, breaking it by not visiting again
            return;
        }
        
        if (visited.contains(item.getId())) {
            return;
        }
        
        visiting.add(item.getId());
        
        // Get the items that depend on this item
        Set<UUID> dependents = reverseDependencies.getOrDefault(item.getId(), Collections.emptySet());
        
        for (UUID dependentId : dependents) {
            itemService.findById(dependentId).ifPresent(dependent -> 
                topologicalSortVisit(dependent, items, visited, visiting, sorted)
            );
        }
        
        visiting.remove(item.getId());
        visited.add(item.getId());
        sorted.add(item);
    }
    
    /**
     * Calculates a weight for an item based on its properties.
     * Higher priority items have higher weights.
     * 
     * @param item the work item
     * @return a weight representing the item's importance
     */
    private double calculateItemWeight(WorkItem item) {
        if (item == null) {
            return 0.0;
        }
        
        double weight = 1.0;
        
        // Add weight based on priority
        switch (item.getPriority()) {
            case CRITICAL:
                weight += 4.0;
                break;
            case HIGH:
                weight += 3.0;
                break;
            case MEDIUM:
                weight += 2.0;
                break;
            case LOW:
                weight += 1.0;
                break;
        }
        
        // Lower weight for completed items
        if (item.getStatus() == WorkflowState.DONE || 
            item.getStatus() == WorkflowState.RELEASED) {
            weight *= 0.5;
        }
        
        return weight;
    }
}