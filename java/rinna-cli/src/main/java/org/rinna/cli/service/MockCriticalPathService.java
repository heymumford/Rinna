/*
 * Mock critical path service for testing
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.service;

import java.time.LocalDate;
import java.util.*;


/**
 * Mock implementation of critical path analysis service for testing.
 */
public class MockCriticalPathService {
    
    private List<String> criticalPath = new ArrayList<>();
    private Map<String, List<String>> directDependencies = new HashMap<>();
    private Map<String, List<String>> reverseDependencies = new HashMap<>();
    private Map<String, Integer> estimatedEffort = new HashMap<>();
    private boolean refreshCalled = false;
    
    // Default critical path
    public MockCriticalPathService() {
        // Initialize with empty data
    }
    
    /**
     * Sets up a critical path for testing.
     * 
     * @param itemIds Ordered list of work item IDs in the critical path
     */
    public void setCriticalPath(List<String> itemIds) {
        this.criticalPath = new ArrayList<>(itemIds);
    }
    
    /**
     * Adds a dependency between work items.
     * 
     * @param dependentItem  The dependent work item ID
     * @param dependencyItem The item it depends on
     */
    public void addDependency(String dependentItem, String dependencyItem) {
        // Add to direct dependencies (item -> dependencies)
        List<String> dependencies = directDependencies.computeIfAbsent(dependentItem, k -> new ArrayList<>());
        if (!dependencies.contains(dependencyItem)) {
            dependencies.add(dependencyItem);
        }
        
        // Add to reverse dependencies (dependency -> items dependent on it)
        List<String> dependents = reverseDependencies.computeIfAbsent(dependencyItem, k -> new ArrayList<>());
        if (!dependents.contains(dependentItem)) {
            dependents.add(dependentItem);
        }
    }
    
    /**
     * Sets the estimated effort for a work item.
     * 
     * @param itemId The work item ID
     * @param effort The estimated effort in hours
     */
    public void setEstimatedEffort(String itemId, int effort) {
        estimatedEffort.put(itemId, effort);
    }
    
    /**
     * Gets the critical path for the project.
     * 
     * @return List of work item IDs in the critical path
     */
    public List<String> getCriticalPath() {
        return new ArrayList<>(criticalPath);
    }
    
    /**
     * Gets detailed information about the critical path.
     * 
     * @return Map containing detailed information about the critical path
     */
    public Map<String, Object> getCriticalPathDetails() {
        Map<String, Object> details = new HashMap<>();
        details.put("criticalPath", criticalPath);
        details.put("pathLength", criticalPath.size());
        
        // Add estimated effort if available
        int totalEffort = criticalPath.stream()
            .mapToInt(id -> estimatedEffort.getOrDefault(id, 0))
            .sum();
        details.put("totalEffort", totalEffort);
        
        // Add bottlenecks - assume first open item is a bottleneck
        List<String> bottlenecks = new ArrayList<>();
        if (!criticalPath.isEmpty()) {
            bottlenecks.add(criticalPath.get(0));
        }
        details.put("bottlenecks", bottlenecks);
        
        // Add estimated completion date
        LocalDate completionDate = LocalDate.now().plusDays(totalEffort / 8); // Assume 8 hours/day
        details.put("estimatedCompletionDate", completionDate);
        
        return details;
    }
    
    /**
     * Gets the critical path for a specific work item.
     * 
     * @param itemId The work item ID
     * @return Map containing critical path information for the specific item
     */
    public Map<String, Object> getItemCriticalPath(String itemId) {
        Map<String, Object> result = new HashMap<>();
        
        // If item not on critical path, return empty
        if (!criticalPath.contains(itemId)) {
            result.put("onCriticalPath", false);
            result.put("criticalPath", Collections.emptyList());
            return result;
        }
        
        result.put("onCriticalPath", true);
        
        // Find the position in critical path
        int index = criticalPath.indexOf(itemId);
        result.put("criticalPath", criticalPath);
        result.put("position", index + 1);
        
        // Get direct dependencies
        List<String> direct = directDependencies.getOrDefault(itemId, Collections.emptyList());
        result.put("directDependencies", direct);
        
        // Calculate indirect dependencies (transitive closure)
        Set<String> indirect = new HashSet<>();
        for (String directDep : direct) {
            calculateIndirectDependencies(directDep, indirect);
        }
        // Remove direct dependencies from indirect
        indirect.removeAll(direct);
        result.put("indirectDependencies", new ArrayList<>(indirect));
        
        return result;
    }
    
    private void calculateIndirectDependencies(String itemId, Set<String> result) {
        List<String> dependencies = directDependencies.getOrDefault(itemId, Collections.emptyList());
        for (String dependency : dependencies) {
            if (result.add(dependency)) {
                calculateIndirectDependencies(dependency, result);
            }
        }
    }
    
    /**
     * Gets information about blocking items.
     * 
     * @return List of blocking items with their impact
     */
    public List<Map<String, Object>> getBlockers() {
        List<Map<String, Object>> blockers = new ArrayList<>();
        
        // Consider first item in the critical path as the main blocker
        if (!criticalPath.isEmpty()) {
            String blockerId = criticalPath.get(0);
            Map<String, Object> blocker = new HashMap<>();
            blocker.put("id", blockerId);
            
            // Get direct impact
            List<String> directImpact = reverseDependencies.getOrDefault(blockerId, Collections.emptyList());
            blocker.put("directlyBlocks", directImpact);
            
            // Calculate total impact
            Set<String> totalImpact = new HashSet<>(directImpact);
            for (String item : directImpact) {
                calculateTotalImpact(item, totalImpact);
            }
            blocker.put("totalImpact", new ArrayList<>(totalImpact));
            
            blockers.add(blocker);
        }
        
        return blockers;
    }
    
    private void calculateTotalImpact(String itemId, Set<String> result) {
        List<String> impacted = reverseDependencies.getOrDefault(itemId, Collections.emptyList());
        for (String item : impacted) {
            if (result.add(item)) {
                calculateTotalImpact(item, result);
            }
        }
    }
    
    /**
     * Gets the dependency graph.
     * 
     * @return Map representing the dependency graph
     */
    public Map<String, List<String>> getDependencyGraph() {
        return new HashMap<>(directDependencies);
    }
    
    /**
     * Gets critical path with time estimates.
     * 
     * @return List of critical path items with time estimates
     */
    public List<Map<String, Object>> getCriticalPathWithEstimates() {
        List<Map<String, Object>> result = new ArrayList<>();
        
        int cumulativeEffort = 0;
        LocalDate baseDate = LocalDate.now();
        
        for (String itemId : criticalPath) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", itemId);
            
            int effort = estimatedEffort.getOrDefault(itemId, 0);
            item.put("estimatedEffort", effort);
            
            cumulativeEffort += effort;
            item.put("cumulativeEffort", cumulativeEffort);
            
            // Calculate estimated completion date (assume 8 hours/day)
            LocalDate completionDate = baseDate.plusDays(cumulativeEffort / 8);
            item.put("estimatedCompletionDate", completionDate);
            
            result.add(item);
        }
        
        return result;
    }
    
    /**
     * Checks if the calculation has been refreshed.
     * 
     * @return true if refreshed, false otherwise
     */
    public boolean wasRefreshCalled() {
        return refreshCalled;
    }
    
    /**
     * Refreshes the critical path calculation.
     */
    public void refresh() {
        refreshCalled = true;
    }
    
    /**
     * Clears all dependencies.
     */
    public void clearDependencies() {
        directDependencies.clear();
        reverseDependencies.clear();
        criticalPath.clear();
        estimatedEffort.clear();
    }
    
    /**
     * Resets the mock state.
     */
    public void reset() {
        refreshCalled = false;
        // Don't clear dependencies here, as they might be needed for multiple tests
    }
}