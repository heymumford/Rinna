/*
 * DefaultCriticalPathService - Default implementation of CriticalPathService
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.adapter.service;

import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemDependency;
import org.rinna.repository.DependencyRepository;
import org.rinna.repository.ItemRepository;
import org.rinna.usecase.CriticalPathService;
import org.rinna.domain.WorkflowState;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Default implementation of the CriticalPathService interface.
 * This implementation uses the Critical Path Method (CPM) algorithm to calculate
 * the critical path based on work item dependencies.
 */
public class DefaultCriticalPathService implements CriticalPathService {

    private final ItemRepository itemRepository;
    private final DependencyRepository dependencyRepository;
    
    /**
     * Creates a new DefaultCriticalPathService with the given repositories.
     *
     * @param itemRepository the repository for work items
     * @param dependencyRepository the repository for work item dependencies
     */
    public DefaultCriticalPathService(ItemRepository itemRepository, DependencyRepository dependencyRepository) {
        this.itemRepository = itemRepository;
        this.dependencyRepository = dependencyRepository;
    }
    
    @Override
    public WorkItemDependency addDependency(UUID dependentId, UUID dependencyId, String dependencyType, String createdBy) {
        // Check if both work items exist
        Optional<WorkItem> dependent = itemRepository.findById(dependentId);
        Optional<WorkItem> dependency = itemRepository.findById(dependencyId);
        
        if (dependent.isEmpty()) {
            throw new IllegalArgumentException("Dependent work item not found: " + dependentId);
        }
        if (dependency.isEmpty()) {
            throw new IllegalArgumentException("Dependency work item not found: " + dependencyId);
        }
        
        // Check for cycles
        if (wouldCreateCycle(dependentId, dependencyId)) {
            throw new IllegalStateException("Adding this dependency would create a cycle");
        }
        
        // Create and save the dependency
        WorkItemDependency newDependency = new WorkItemDependency.Builder()
                .dependentId(dependentId)
                .dependencyId(dependencyId)
                .dependencyType(dependencyType)
                .createdBy(createdBy)
                .build();
                
        return dependencyRepository.save(newDependency);
    }
    
    @Override
    public boolean removeDependency(UUID dependentId, UUID dependencyId) {
        return dependencyRepository.remove(dependentId, dependencyId);
    }
    
    @Override
    public List<WorkItemDependency> getDependencies(UUID workItemId, String direction) {
        if ("incoming".equalsIgnoreCase(direction)) {
            return dependencyRepository.findIncomingDependencies(workItemId);
        } else if ("outgoing".equalsIgnoreCase(direction)) {
            return dependencyRepository.findOutgoingDependencies(workItemId);
        } else {
            throw new IllegalArgumentException("Direction must be either 'incoming' or 'outgoing'");
        }
    }
    
    @Override
    public List<WorkItem> calculateCriticalPath() {
        // Build the dependency graph
        Map<UUID, List<UUID>> dependencyGraph = buildDependencyGraph();
        
        // Find sources (nodes with no incoming edges)
        Set<UUID> sources = findSources(dependencyGraph);
        
        // Find sinks (nodes with no outgoing edges)
        Set<UUID> sinks = findSinks(dependencyGraph);
        
        // If there are no sources or sinks, return empty list
        if (sources.isEmpty() || sinks.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Calculate longest path from each source to each sink
        List<UUID> longestPath = new ArrayList<>();
        
        for (UUID source : sources) {
            for (UUID sink : sinks) {
                List<UUID> path = findLongestPath(source, sink, dependencyGraph);
                if (path.size() > longestPath.size()) {
                    longestPath = path;
                }
            }
        }
        
        // Convert the path of UUIDs to a list of WorkItems
        return longestPath.stream()
                .map(itemRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<WorkItem> calculateCriticalPathTo(UUID workItemId) {
        // Build the dependency graph
        Map<UUID, List<UUID>> dependencyGraph = buildDependencyGraph();
        
        // Find sources (nodes with no incoming edges)
        Set<UUID> sources = findSources(dependencyGraph);
        
        // If there are no sources, return empty list
        if (sources.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Calculate longest path from each source to the target
        List<UUID> longestPath = new ArrayList<>();
        
        for (UUID source : sources) {
            List<UUID> path = findLongestPath(source, workItemId, dependencyGraph);
            if (path.size() > longestPath.size()) {
                longestPath = path;
            }
        }
        
        // Convert the path of UUIDs to a list of WorkItems
        return longestPath.stream()
                .map(itemRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<WorkItem> identifyBlockers() {
        // Get the critical path
        List<WorkItem> criticalPath = calculateCriticalPath();
        
        // Find all items on the critical path that are blocked
        return criticalPath.stream()
                .filter(item -> item.getMetadata() != null && item.getMetadata().containsKey("blocked") && 
                        Boolean.parseBoolean(item.getMetadata().get("blocked")))
                .collect(Collectors.toList());
    }
    
    @Override
    public WorkItem markAsBlocked(UUID workItemId, String reason, String blockedBy) {
        Optional<WorkItem> optionalItem = itemRepository.findById(workItemId);
        
        if (optionalItem.isEmpty()) {
            throw new IllegalArgumentException("Work item not found: " + workItemId);
        }
        
        WorkItem item = optionalItem.get();
        Map<String, String> metadata = new HashMap<>(item.getMetadata() != null ? item.getMetadata() : new HashMap<>());
        metadata.put("blocked", "true");
        metadata.put("blocked_reason", reason);
        if (blockedBy != null) {
            metadata.put("blocked_by", blockedBy);
        }
        metadata.put("blocked_date", LocalDate.now().toString());
        
        // Create a copy of the work item with the updated metadata
        WorkItem updatedItem = itemRepository.updateMetadata(workItemId, metadata);
        
        return updatedItem;
    }
    
    @Override
    public WorkItem markAsUnblocked(UUID workItemId) {
        Optional<WorkItem> optionalItem = itemRepository.findById(workItemId);
        
        if (optionalItem.isEmpty()) {
            throw new IllegalArgumentException("Work item not found: " + workItemId);
        }
        
        WorkItem item = optionalItem.get();
        Map<String, String> metadata = new HashMap<>(item.getMetadata() != null ? item.getMetadata() : new HashMap<>());
        metadata.put("blocked", "false");
        metadata.put("unblocked_date", LocalDate.now().toString());
        
        // Create a copy of the work item with the updated metadata
        WorkItem updatedItem = itemRepository.updateMetadata(workItemId, metadata);
        
        return updatedItem;
    }
    
    @Override
    public Map<UUID, LocalDate> getEstimatedCompletionDates() {
        // Get the critical path
        List<WorkItem> criticalPath = calculateCriticalPath();
        
        // Calculate estimated completion dates based on estimated duration
        Map<UUID, LocalDate> completionDates = new HashMap<>();
        LocalDate currentDate = LocalDate.now();
        
        for (WorkItem item : criticalPath) {
            // Skip completed items
            if (item.getStatus() == WorkflowState.DONE) {
                continue;
            }
            
            // Get estimated duration from metadata or use default
            int estimatedDays = 1; // Default to 1 day
            if (item.getMetadata() != null && item.getMetadata().containsKey("estimated_days")) {
                try {
                    estimatedDays = Integer.parseInt(item.getMetadata().get("estimated_days"));
                } catch (NumberFormatException e) {
                    // Ignore parsing errors, use default
                }
            }
            
            // Update current date
            currentDate = currentDate.plusDays(estimatedDays);
            
            // Store completion date for this item
            completionDates.put(item.getId(), currentDate);
        }
        
        return completionDates;
    }
    
    @Override
    public List<List<WorkItem>> identifyParallelPaths() {
        // Build the dependency graph
        Map<UUID, List<UUID>> dependencyGraph = buildDependencyGraph();
        
        // Find sources (nodes with no incoming edges)
        Set<UUID> sources = findSources(dependencyGraph);
        
        // Find sinks (nodes with no outgoing edges)
        Set<UUID> sinks = findSinks(dependencyGraph);
        
        // If there are no sources or sinks, return empty list
        if (sources.isEmpty() || sinks.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Find all paths from sources to sinks
        List<List<UUID>> allPaths = new ArrayList<>();
        
        for (UUID source : sources) {
            for (UUID sink : sinks) {
                List<List<UUID>> paths = findAllPaths(source, sink, dependencyGraph);
                allPaths.addAll(paths);
            }
        }
        
        // Sort paths by length (longest first)
        allPaths.sort((p1, p2) -> Integer.compare(p2.size(), p1.size()));
        
        // Convert paths of UUIDs to lists of WorkItems
        return allPaths.stream()
                .map(path -> path.stream()
                        .map(itemRepository::findById)
                        .filter(Optional::isPresent)
                        .map(Optional::get)
                        .collect(Collectors.toList()))
                .collect(Collectors.toList());
    }
    
    @Override
    public List<WorkItem> calculateDelayImpact(UUID workItemId, int delayDays) {
        // Get all items that depend on this one (directly or indirectly)
        Set<UUID> affectedItems = new HashSet<>();
        findAllDependents(workItemId, affectedItems, new HashSet<>());
        
        // Convert to WorkItems
        return affectedItems.stream()
                .map(itemRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }
    
    // Helper methods
    
    private boolean wouldCreateCycle(UUID dependentId, UUID dependencyId) {
        // Check if adding this dependency would create a cycle
        // by seeing if the dependency already depends on the dependent
        Set<UUID> visited = new HashSet<>();
        return hasPathBetween(dependencyId, dependentId, visited);
    }
    
    private boolean hasPathBetween(UUID start, UUID end, Set<UUID> visited) {
        if (start.equals(end)) {
            return true;
        }
        
        visited.add(start);
        
        for (WorkItemDependency dependency : dependencyRepository.findOutgoingDependencies(start)) {
            UUID nextId = dependency.getDependentId();
            if (!visited.contains(nextId) && hasPathBetween(nextId, end, visited)) {
                return true;
            }
        }
        
        return false;
    }
    
    private Map<UUID, List<UUID>> buildDependencyGraph() {
        Map<UUID, List<UUID>> graph = new HashMap<>();
        
        // Get all dependencies and build adjacency list
        List<WorkItemDependency> allDependencies = dependencyRepository.findAll();
        
        for (WorkItemDependency dependency : allDependencies) {
            UUID from = dependency.getDependencyId(); // From dependency to dependent
            UUID to = dependency.getDependentId();
            
            if (!graph.containsKey(from)) {
                graph.put(from, new ArrayList<>());
            }
            graph.get(from).add(to);
            
            // Ensure all items are in the graph, even if they have no outgoing edges
            if (!graph.containsKey(to)) {
                graph.put(to, new ArrayList<>());
            }
        }
        
        return graph;
    }
    
    private Set<UUID> findSources(Map<UUID, List<UUID>> graph) {
        // First, assume all nodes are sources
        Set<UUID> sources = new HashSet<>(graph.keySet());
        
        // Remove any node that appears as a destination
        for (List<UUID> destinations : graph.values()) {
            sources.removeAll(destinations);
        }
        
        return sources;
    }
    
    private Set<UUID> findSinks(Map<UUID, List<UUID>> graph) {
        Set<UUID> sinks = new HashSet<>();
        
        for (UUID node : graph.keySet()) {
            if (graph.get(node).isEmpty()) {
                sinks.add(node);
            }
        }
        
        return sinks;
    }
    
    private List<UUID> findLongestPath(UUID start, UUID end, Map<UUID, List<UUID>> graph) {
        // Use dynamic programming to find the longest path
        Map<UUID, List<UUID>> longestPaths = new HashMap<>();
        
        // Initialize paths
        for (UUID node : graph.keySet()) {
            longestPaths.put(node, new ArrayList<>());
        }
        
        // Start with just the start node
        longestPaths.get(start).add(start);
        
        // Topological sort
        List<UUID> sortedNodes = topologicalSort(graph);
        
        // Process nodes in topological order
        for (UUID node : sortedNodes) {
            for (UUID neighbor : graph.getOrDefault(node, Collections.emptyList())) {
                List<UUID> currentPath = longestPaths.get(node);
                List<UUID> neighborPath = longestPaths.get(neighbor);
                
                if (currentPath.size() + 1 > neighborPath.size()) {
                    // Create a new path extending the current path
                    List<UUID> newPath = new ArrayList<>(currentPath);
                    newPath.add(neighbor);
                    longestPaths.put(neighbor, newPath);
                }
            }
        }
        
        return longestPaths.getOrDefault(end, Collections.emptyList());
    }
    
    private List<UUID> topologicalSort(Map<UUID, List<UUID>> graph) {
        Set<UUID> visited = new HashSet<>();
        List<UUID> result = new ArrayList<>();
        
        for (UUID node : graph.keySet()) {
            if (!visited.contains(node)) {
                topologicalSortDFS(node, graph, visited, result);
            }
        }
        
        Collections.reverse(result);
        return result;
    }
    
    private void topologicalSortDFS(UUID node, Map<UUID, List<UUID>> graph, Set<UUID> visited, List<UUID> result) {
        visited.add(node);
        
        for (UUID neighbor : graph.getOrDefault(node, Collections.emptyList())) {
            if (!visited.contains(neighbor)) {
                topologicalSortDFS(neighbor, graph, visited, result);
            }
        }
        
        result.add(node);
    }
    
    private List<List<UUID>> findAllPaths(UUID start, UUID end, Map<UUID, List<UUID>> graph) {
        List<List<UUID>> allPaths = new ArrayList<>();
        List<UUID> currentPath = new ArrayList<>();
        Set<UUID> visited = new HashSet<>();
        
        currentPath.add(start);
        visited.add(start);
        
        findAllPathsDFS(start, end, graph, visited, currentPath, allPaths);
        
        return allPaths;
    }
    
    private void findAllPathsDFS(UUID current, UUID end, Map<UUID, List<UUID>> graph, 
                               Set<UUID> visited, List<UUID> currentPath, List<List<UUID>> allPaths) {
        if (current.equals(end)) {
            // We've reached the end, add a copy of the current path to allPaths
            allPaths.add(new ArrayList<>(currentPath));
            return;
        }
        
        for (UUID neighbor : graph.getOrDefault(current, Collections.emptyList())) {
            if (!visited.contains(neighbor)) {
                // Add neighbor to the path and mark as visited
                visited.add(neighbor);
                currentPath.add(neighbor);
                
                // Recurse
                findAllPathsDFS(neighbor, end, graph, visited, currentPath, allPaths);
                
                // Backtrack
                visited.remove(neighbor);
                currentPath.remove(currentPath.size() - 1);
            }
        }
    }
    
    private void findAllDependents(UUID itemId, Set<UUID> result, Set<UUID> visited) {
        if (visited.contains(itemId)) {
            return;
        }
        
        visited.add(itemId);
        
        for (WorkItemDependency dependency : dependencyRepository.findOutgoingDependencies(itemId)) {
            UUID dependentId = dependency.getDependentId();
            result.add(dependentId);
            findAllDependents(dependentId, result, visited);
        }
    }
}