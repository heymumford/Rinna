/*
 * Default implementation of UnifiedWorkItemService for the Rinna unified work management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.rinna.domain.model.CynefinDomain;
import org.rinna.domain.model.OriginCategory;
import org.rinna.domain.model.UnifiedWorkItem;
import org.rinna.domain.model.UnifiedWorkItemRecord;
import org.rinna.domain.model.WorkParadigm;
import org.rinna.domain.model.WorkflowState;
import org.rinna.domain.repository.UnifiedWorkItemRepository;
import org.rinna.usecase.InvalidTransitionException;
import org.rinna.usecase.UnifiedWorkItemService;

/**
 * Default implementation of the UnifiedWorkItemService.
 * This class implements the application use cases for the unified work management system.
 */
public class DefaultUnifiedWorkItemService implements UnifiedWorkItemService {
    private final UnifiedWorkItemRepository repository;
    
    /**
     * Constructor with repository dependency injection.
     * 
     * @param repository the unified work item repository
     */
    public DefaultUnifiedWorkItemService(UnifiedWorkItemRepository repository) {
        this.repository = repository;
    }
    
    @Override
    public UnifiedWorkItem create(UnifiedWorkItem.Builder builder) {
        // If no ID is provided, generate one
        if (builder.id() == null) {
            builder.id(UUID.randomUUID());
        }
        
        // Create the work item
        UnifiedWorkItem workItem = builder.build();
        
        // Save it to the repository
        return repository.save(workItem);
    }
    
    @Override
    public Optional<UnifiedWorkItem> findById(UUID id) {
        return repository.findById(id);
    }
    
    @Override
    public List<UnifiedWorkItem> findAll() {
        return repository.findAll();
    }
    
    @Override
    public List<UnifiedWorkItem> findByTitleContaining(String title) {
        return repository.findByTitleContaining(title);
    }
    
    @Override
    public List<UnifiedWorkItem> findByState(WorkflowState state) {
        return repository.findByState(state);
    }
    
    @Override
    public List<UnifiedWorkItem> findByAssignee(String assignee) {
        return repository.findByAssignee(assignee);
    }
    
    @Override
    public List<UnifiedWorkItem> findByOriginCategory(OriginCategory category) {
        return repository.findByOriginCategory(category);
    }
    
    @Override
    public List<UnifiedWorkItem> findByCynefinDomain(CynefinDomain domain) {
        return repository.findByCynefinDomain(domain);
    }
    
    @Override
    public List<UnifiedWorkItem> findByWorkParadigm(WorkParadigm paradigm) {
        return repository.findByWorkParadigm(paradigm);
    }
    
    @Override
    public List<UnifiedWorkItem> findByAnyTag(List<String> tags) {
        return repository.findByAnyTag(tags);
    }
    
    @Override
    public List<UnifiedWorkItem> findByAllTags(List<String> tags) {
        return repository.findByAllTags(tags);
    }
    
    @Override
    public UnifiedWorkItem updateAssignee(UUID id, String assignee) {
        // Find the work item
        UnifiedWorkItem existingItem = findWorkItemOrThrow(id);
        
        // Create a new builder with the existing item's properties
        UnifiedWorkItem.Builder builder = UnifiedWorkItemRecord.builder(existingItem);
        
        // Update the assignee
        builder.assignee(assignee);
        
        // Build the updated item and save it
        UnifiedWorkItem updatedItem = builder.build();
        return repository.save(updatedItem);
    }
    
    @Override
    public UnifiedWorkItem updateState(UUID id, WorkflowState state) {
        // Find the work item
        UnifiedWorkItem existingItem = findWorkItemOrThrow(id);
        
        // Check if the transition is valid
        if (!isValidTransition(existingItem.state(), state)) {
            throw new InvalidTransitionException(
                "Invalid transition from " + existingItem.state() + " to " + state);
        }
        
        // Create a new builder with the existing item's properties
        UnifiedWorkItem.Builder builder = UnifiedWorkItemRecord.builder(existingItem);
        
        // Update the state
        builder.state(state);
        
        // Build the updated item and save it
        UnifiedWorkItem updatedItem = builder.build();
        return repository.save(updatedItem);
    }
    
    @Override
    public UnifiedWorkItem updateOriginCategory(UUID id, OriginCategory category) {
        // Find the work item
        UnifiedWorkItem existingItem = findWorkItemOrThrow(id);
        
        // Create a new builder with the existing item's properties
        UnifiedWorkItem.Builder builder = UnifiedWorkItemRecord.builder(existingItem);
        
        // Update the origin category
        builder.originCategory(category);
        
        // Build the updated item and save it
        UnifiedWorkItem updatedItem = builder.build();
        return repository.save(updatedItem);
    }
    
    @Override
    public UnifiedWorkItem updateCynefinDomain(UUID id, CynefinDomain domain) {
        // Find the work item
        UnifiedWorkItem existingItem = findWorkItemOrThrow(id);
        
        // Create a new builder with the existing item's properties
        UnifiedWorkItem.Builder builder = UnifiedWorkItemRecord.builder(existingItem);
        
        // Update the Cynefin domain
        builder.cynefinDomain(domain);
        
        // Build the updated item and save it
        UnifiedWorkItem updatedItem = builder.build();
        return repository.save(updatedItem);
    }
    
    @Override
    public UnifiedWorkItem updateWorkParadigm(UUID id, WorkParadigm paradigm) {
        // Find the work item
        UnifiedWorkItem existingItem = findWorkItemOrThrow(id);
        
        // Create a new builder with the existing item's properties
        UnifiedWorkItem.Builder builder = UnifiedWorkItemRecord.builder(existingItem);
        
        // Update the work paradigm
        builder.workParadigm(paradigm);
        
        // Build the updated item and save it
        UnifiedWorkItem updatedItem = builder.build();
        return repository.save(updatedItem);
    }
    
    @Override
    public UnifiedWorkItem updateCognitiveLoad(UUID id, int cognitiveLoad) {
        // Validate the cognitive load range
        if (cognitiveLoad < 1 || cognitiveLoad > 10) {
            throw new IllegalArgumentException("Cognitive load must be between 1 and 10");
        }
        
        // Find the work item
        UnifiedWorkItem existingItem = findWorkItemOrThrow(id);
        
        // Create a new builder with the existing item's properties
        UnifiedWorkItem.Builder builder = UnifiedWorkItemRecord.builder(existingItem);
        
        // Update the cognitive load
        builder.cognitiveLoad(cognitiveLoad);
        
        // Build the updated item and save it
        UnifiedWorkItem updatedItem = builder.build();
        return repository.save(updatedItem);
    }
    
    @Override
    public UnifiedWorkItem updateTags(UUID id, List<String> tags) {
        // Find the work item
        UnifiedWorkItem existingItem = findWorkItemOrThrow(id);
        
        // Create a new builder with the existing item's properties
        UnifiedWorkItem.Builder builder = UnifiedWorkItemRecord.builder(existingItem);
        
        // Update the tags
        builder.tags(tags);
        
        // Build the updated item and save it
        UnifiedWorkItem updatedItem = builder.build();
        return repository.save(updatedItem);
    }
    
    @Override
    public UnifiedWorkItem addTags(UUID id, List<String> tagsToAdd) {
        // Find the work item
        UnifiedWorkItem existingItem = findWorkItemOrThrow(id);
        
        // Get the existing tags
        List<String> existingTags = existingItem.tags();
        if (existingTags == null) {
            existingTags = new ArrayList<>();
        } else {
            // Create a new list to avoid modifying the original
            existingTags = new ArrayList<>(existingTags);
        }
        
        // Add the new tags if they don't already exist
        for (String tag : tagsToAdd) {
            if (!existingTags.contains(tag)) {
                existingTags.add(tag);
            }
        }
        
        // Create a new builder with the existing item's properties
        UnifiedWorkItem.Builder builder = UnifiedWorkItemRecord.builder(existingItem);
        
        // Update the tags
        builder.tags(existingTags);
        
        // Build the updated item and save it
        UnifiedWorkItem updatedItem = builder.build();
        return repository.save(updatedItem);
    }
    
    @Override
    public UnifiedWorkItem removeTags(UUID id, List<String> tagsToRemove) {
        // Find the work item
        UnifiedWorkItem existingItem = findWorkItemOrThrow(id);
        
        // Get the existing tags
        List<String> existingTags = existingItem.tags();
        if (existingTags == null || existingTags.isEmpty()) {
            // No tags to remove
            return existingItem;
        }
        
        // Create a new list with tags that are not in the remove list
        List<String> updatedTags = existingTags.stream()
                .filter(tag -> !tagsToRemove.contains(tag))
                .collect(Collectors.toList());
        
        // Create a new builder with the existing item's properties
        UnifiedWorkItem.Builder builder = UnifiedWorkItemRecord.builder(existingItem);
        
        // Update the tags
        builder.tags(updatedTags);
        
        // Build the updated item and save it
        UnifiedWorkItem updatedItem = builder.build();
        return repository.save(updatedItem);
    }
    
    @Override
    public UnifiedWorkItem updateMetadata(UUID id, Map<String, String> metadata) {
        // Find the work item
        UnifiedWorkItem existingItem = findWorkItemOrThrow(id);
        
        // Create a new builder with the existing item's properties
        UnifiedWorkItem.Builder builder = UnifiedWorkItemRecord.builder(existingItem);
        
        // Update the metadata
        builder.metadata(metadata);
        
        // Build the updated item and save it
        UnifiedWorkItem updatedItem = builder.build();
        return repository.save(updatedItem);
    }
    
    @Override
    public UnifiedWorkItem addMetadata(UUID id, String key, String value) {
        // Find the work item
        UnifiedWorkItem existingItem = findWorkItemOrThrow(id);
        
        // Get the existing metadata
        Map<String, String> existingMetadata = existingItem.metadata();
        if (existingMetadata == null) {
            existingMetadata = new HashMap<>();
        } else {
            // Create a new map to avoid modifying the original
            existingMetadata = new HashMap<>(existingMetadata);
        }
        
        // Add or update the metadata
        existingMetadata.put(key, value);
        
        // Create a new builder with the existing item's properties
        UnifiedWorkItem.Builder builder = UnifiedWorkItemRecord.builder(existingItem);
        
        // Update the metadata
        builder.metadata(existingMetadata);
        
        // Build the updated item and save it
        UnifiedWorkItem updatedItem = builder.build();
        return repository.save(updatedItem);
    }
    
    @Override
    public UnifiedWorkItem removeMetadata(UUID id, String key) {
        // Find the work item
        UnifiedWorkItem existingItem = findWorkItemOrThrow(id);
        
        // Get the existing metadata
        Map<String, String> existingMetadata = existingItem.metadata();
        if (existingMetadata == null || !existingMetadata.containsKey(key)) {
            // No metadata to remove
            return existingItem;
        }
        
        // Create a new map without the key
        Map<String, String> updatedMetadata = new HashMap<>(existingMetadata);
        updatedMetadata.remove(key);
        
        // Create a new builder with the existing item's properties
        UnifiedWorkItem.Builder builder = UnifiedWorkItemRecord.builder(existingItem);
        
        // Update the metadata
        builder.metadata(updatedMetadata);
        
        // Build the updated item and save it
        UnifiedWorkItem updatedItem = builder.build();
        return repository.save(updatedItem);
    }
    
    @Override
    public UnifiedWorkItem addDependency(UUID id, UUID dependencyId) {
        // Find the work item
        UnifiedWorkItem existingItem = findWorkItemOrThrow(id);
        
        // Find the dependency
        findWorkItemOrThrow(dependencyId);
        
        // Get the existing dependencies
        List<UUID> existingDependencies = existingItem.dependencies();
        if (existingDependencies == null) {
            existingDependencies = new ArrayList<>();
        } else {
            // Create a new list to avoid modifying the original
            existingDependencies = new ArrayList<>(existingDependencies);
        }
        
        // Add the dependency if it doesn't already exist
        if (!existingDependencies.contains(dependencyId)) {
            existingDependencies.add(dependencyId);
        }
        
        // Create a new builder with the existing item's properties
        UnifiedWorkItem.Builder builder = UnifiedWorkItemRecord.builder(existingItem);
        
        // Update the dependencies
        builder.dependencies(existingDependencies);
        
        // Build the updated item and save it
        UnifiedWorkItem updatedItem = builder.build();
        return repository.save(updatedItem);
    }
    
    @Override
    public UnifiedWorkItem removeDependency(UUID id, UUID dependencyId) {
        // Find the work item
        UnifiedWorkItem existingItem = findWorkItemOrThrow(id);
        
        // Get the existing dependencies
        List<UUID> existingDependencies = existingItem.dependencies();
        if (existingDependencies == null || !existingDependencies.contains(dependencyId)) {
            // No dependency to remove
            return existingItem;
        }
        
        // Create a new list without the dependency
        List<UUID> updatedDependencies = existingDependencies.stream()
                .filter(depId -> !depId.equals(dependencyId))
                .collect(Collectors.toList());
        
        // Create a new builder with the existing item's properties
        UnifiedWorkItem.Builder builder = UnifiedWorkItemRecord.builder(existingItem);
        
        // Update the dependencies
        builder.dependencies(updatedDependencies);
        
        // Build the updated item and save it
        UnifiedWorkItem updatedItem = builder.build();
        return repository.save(updatedItem);
    }
    
    @Override
    public void deleteById(UUID id) {
        // Find the work item (to ensure it exists)
        findWorkItemOrThrow(id);
        
        // Delete it
        repository.deleteById(id);
    }
    
    @Override
    public List<UnifiedWorkItem> getDependencies(UUID id) {
        // Find the work item
        UnifiedWorkItem item = findWorkItemOrThrow(id);
        
        // Get the dependencies
        List<UUID> dependencyIds = item.dependencies();
        if (dependencyIds == null || dependencyIds.isEmpty()) {
            return List.of();
        }
        
        // Find and return the dependencies
        return dependencyIds.stream()
                .map(repository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<UnifiedWorkItem> getDependents(UUID id) {
        // Find the work item (to ensure it exists)
        findWorkItemOrThrow(id);
        
        // Find all work items that depend on this one
        return repository.findAll().stream()
                .filter(item -> {
                    List<UUID> dependencies = item.dependencies();
                    return dependencies != null && dependencies.contains(id);
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public List<UnifiedWorkItem> calculateCriticalPath(List<UnifiedWorkItem> workItems) {
        // Implementation of critical path algorithm
        // This is a simplified version that just returns the items in topological order
        Map<UUID, UnifiedWorkItem> itemsMap = workItems.stream()
                .collect(Collectors.toMap(UnifiedWorkItem::id, item -> item));
        
        // Build a dependency graph
        Map<UUID, List<UUID>> graph = new HashMap<>();
        Map<UUID, Integer> inDegree = new HashMap<>();
        
        for (UnifiedWorkItem item : workItems) {
            UUID itemId = item.id();
            List<UUID> dependencies = item.dependencies();
            
            if (dependencies == null || dependencies.isEmpty()) {
                graph.put(itemId, new ArrayList<>());
                inDegree.put(itemId, 0);
            } else {
                // Filter out dependencies that are not in the workItems list
                List<UUID> validDependencies = dependencies.stream()
                        .filter(itemsMap::containsKey)
                        .collect(Collectors.toList());
                
                graph.put(itemId, new ArrayList<>());
                inDegree.put(itemId, validDependencies.size());
                
                // Add this item as a dependent to each of its dependencies
                for (UUID depId : validDependencies) {
                    if (!graph.containsKey(depId)) {
                        graph.put(depId, new ArrayList<>());
                    }
                    graph.get(depId).add(itemId);
                }
            }
        }
        
        // Find all items with no dependencies (inDegree = 0)
        List<UUID> nodesWithNoDependencies = new ArrayList<>();
        for (Map.Entry<UUID, Integer> entry : inDegree.entrySet()) {
            if (entry.getValue() == 0) {
                nodesWithNoDependencies.add(entry.getKey());
            }
        }
        
        // Perform topological sort
        List<UUID> sortedIds = new ArrayList<>();
        while (!nodesWithNoDependencies.isEmpty()) {
            UUID nodeId = nodesWithNoDependencies.remove(0);
            sortedIds.add(nodeId);
            
            for (UUID dependentId : graph.get(nodeId)) {
                inDegree.put(dependentId, inDegree.get(dependentId) - 1);
                if (inDegree.get(dependentId) == 0) {
                    nodesWithNoDependencies.add(dependentId);
                }
            }
        }
        
        // Check for cyclic dependencies
        if (sortedIds.size() != workItems.size()) {
            throw new IllegalArgumentException("The work items have cyclic dependencies");
        }
        
        // Convert the sorted IDs back to work items
        return sortedIds.stream()
                .map(itemsMap::get)
                .collect(Collectors.toList());
    }
    
    @Override
    public Map<String, Object> getCognitiveLoadReport(String assignee) {
        // Find all work items assigned to the person
        List<UnifiedWorkItem> assignedItems = findByAssignee(assignee);
        
        // Calculate the total cognitive load
        int totalLoad = assignedItems.stream()
                .mapToInt(UnifiedWorkItem::cognitiveLoad)
                .sum();
        
        // Create the result map
        Map<String, Object> result = new HashMap<>();
        result.put("assignee", assignee);
        result.put("totalCognitiveLoad", totalLoad);
        result.put("workItems", assignedItems);
        
        // Add some additional information
        result.put("itemCount", assignedItems.size());
        result.put("averageCognitiveLoad", assignedItems.isEmpty() ? 0 : (double) totalLoad / assignedItems.size());
        
        // Add distribution by category
        Map<OriginCategory, List<UnifiedWorkItem>> byCategory = assignedItems.stream()
                .collect(Collectors.groupingBy(UnifiedWorkItem::originCategory));
        Map<String, Integer> categoryDistribution = new HashMap<>();
        for (Map.Entry<OriginCategory, List<UnifiedWorkItem>> entry : byCategory.entrySet()) {
            categoryDistribution.put(entry.getKey().getDisplayName(), entry.getValue().size());
        }
        result.put("categoryDistribution", categoryDistribution);
        
        return result;
    }
    
    @Override
    public Map<String, Map<String, Integer>> getWorkItemDistribution() {
        // Get all work items
        List<UnifiedWorkItem> allItems = findAll();
        
        // Calculate distribution by category
        Map<OriginCategory, Long> byCategory = allItems.stream()
                .collect(Collectors.groupingBy(UnifiedWorkItem::originCategory, Collectors.counting()));
        Map<String, Integer> categoryDistribution = new HashMap<>();
        for (Map.Entry<OriginCategory, Long> entry : byCategory.entrySet()) {
            categoryDistribution.put(entry.getKey().getDisplayName(), entry.getValue().intValue());
        }
        
        // Calculate distribution by domain
        Map<CynefinDomain, Long> byDomain = allItems.stream()
                .collect(Collectors.groupingBy(UnifiedWorkItem::cynefinDomain, Collectors.counting()));
        Map<String, Integer> domainDistribution = new HashMap<>();
        for (Map.Entry<CynefinDomain, Long> entry : byDomain.entrySet()) {
            domainDistribution.put(entry.getKey().getDisplayName(), entry.getValue().intValue());
        }
        
        // Calculate distribution by paradigm
        Map<WorkParadigm, Long> byParadigm = allItems.stream()
                .collect(Collectors.groupingBy(UnifiedWorkItem::workParadigm, Collectors.counting()));
        Map<String, Integer> paradigmDistribution = new HashMap<>();
        for (Map.Entry<WorkParadigm, Long> entry : byParadigm.entrySet()) {
            paradigmDistribution.put(entry.getKey().getDisplayName(), entry.getValue().intValue());
        }
        
        // Create the result map
        Map<String, Map<String, Integer>> result = new HashMap<>();
        result.put("byCategory", categoryDistribution);
        result.put("byDomain", domainDistribution);
        result.put("byParadigm", paradigmDistribution);
        
        return result;
    }
    
    /**
     * Helper method to find a work item by ID or throw an exception.
     * 
     * @param id the ID of the work item
     * @return the work item
     * @throws IllegalArgumentException if the work item is not found
     */
    private UnifiedWorkItem findWorkItemOrThrow(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Work item not found with ID: " + id));
    }
    
    /**
     * Checks if a state transition is valid.
     * 
     * @param currentState the current state
     * @param newState the new state
     * @return true if the transition is valid, false otherwise
     */
    private boolean isValidTransition(WorkflowState currentState, WorkflowState newState) {
        // For now, allow all transitions
        // In a real implementation, this would check the workflow rules
        return true;
    }
}