/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.adapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.rinna.cli.domain.model.DefaultDomainWorkItem;
import org.rinna.cli.domain.model.DomainWorkItem;
import org.rinna.cli.domain.service.CriticalPathService;
import org.rinna.cli.service.MockCriticalPathService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.util.ModelMapper;
import org.rinna.cli.util.StateMapper;

/**
 * Adapter class that converts between CLI implementation and domain critical path service models.
 * This adapter implements the CLI domain CriticalPathService interface while delegating
 * to the CLI's MockCriticalPathService implementation.
 */
public class CriticalPathServiceAdapter implements CriticalPathService {
    
    private final MockCriticalPathService mockCriticalPathService;
    private final MockItemService mockItemService;
    
    /**
     * Creates a new CriticalPathServiceAdapter.
     * 
     * @param mockCriticalPathService the underlying mock critical path service
     * @param mockItemService the mock item service for retrieving work items
     */
    public CriticalPathServiceAdapter(MockCriticalPathService mockCriticalPathService, 
                                     MockItemService mockItemService) {
        this.mockCriticalPathService = mockCriticalPathService;
        this.mockItemService = mockItemService;
    }
    
    @Override
    public List<DomainWorkItem> findBlockingItems() {
        // Get blockers from the mock service
        List<Map<String, Object>> blockers = mockCriticalPathService.getBlockers();
        
        // Convert each blocker to a domain work item
        List<DomainWorkItem> result = new ArrayList<>();
        for (Map<String, Object> blocker : blockers) {
            String blockerId = (String) blocker.get("id");
            org.rinna.cli.model.WorkItem cliItem = mockItemService.getItem(blockerId);
            
            if (cliItem != null) {
                // Convert CLI item to domain item
                DomainWorkItem domainItem = ModelMapper.toDomainWorkItem(cliItem);
                result.add(domainItem);
            }
        }
        
        return result;
    }
    
    @Override
    public List<DomainWorkItem> findCriticalPath() {
        // Get the critical path from the mock service
        List<String> criticalPath = mockCriticalPathService.getCriticalPath();
        
        // Convert each item ID to a domain work item
        List<DomainWorkItem> result = new ArrayList<>();
        for (String itemId : criticalPath) {
            org.rinna.cli.model.WorkItem cliItem = mockItemService.getItem(itemId);
            
            if (cliItem != null) {
                // Convert CLI item to domain item
                DomainWorkItem domainItem = ModelMapper.toDomainWorkItem(cliItem);
                result.add(domainItem);
            }
        }
        
        return result;
    }
    
    @Override
    public List<DomainWorkItem> findItemsDependingOn(UUID itemId) {
        // Get the item's critical path details
        Map<String, Object> itemPath = mockCriticalPathService.getItemCriticalPath(itemId.toString());
        
        // Check if the item is on the critical path
        if (!(boolean) itemPath.getOrDefault("onCriticalPath", false)) {
            return new ArrayList<>();
        }
        
        // Get the direct dependencies
        @SuppressWarnings("unchecked")
        List<String> directDeps = (List<String>) itemPath.get("directDependencies");
        
        // Convert to domain work items
        List<DomainWorkItem> result = new ArrayList<>();
        if (directDeps != null) {
            for (String depId : directDeps) {
                org.rinna.cli.model.WorkItem cliItem = mockItemService.getItem(depId);
                
                if (cliItem != null) {
                    // Convert CLI item to domain item
                    DomainWorkItem domainItem = ModelMapper.toDomainWorkItem(cliItem);
                    result.add(domainItem);
                }
            }
        }
        
        return result;
    }
    
    @Override
    public boolean addDependency(UUID dependentId, UUID blockerId) {
        // Check if both items exist
        org.rinna.cli.model.WorkItem dependent = mockItemService.getItem(dependentId.toString());
        org.rinna.cli.model.WorkItem blocker = mockItemService.getItem(blockerId.toString());
        
        if (dependent == null || blocker == null) {
            return false;
        }
        
        // Check if dependency already exists
        if (hasDependency(dependentId, blockerId)) {
            return false;
        }
        
        // Add the dependency
        mockCriticalPathService.addDependency(dependentId.toString(), blockerId.toString());
        return true;
    }
    
    @Override
    public boolean removeDependency(UUID dependentId, UUID blockerId) {
        // Since MockCriticalPathService doesn't have a removeDependency method,
        // we'll simulate it by checking if the dependency exists, then clearing and rebuilding
        // Note: This is not an efficient implementation for a production system
        
        // Check if dependency exists
        if (!hasDependency(dependentId, blockerId)) {
            return false;
        }
        
        // Get the current dependency graph
        Map<String, List<String>> dependencyGraph = mockCriticalPathService.getDependencyGraph();
        
        // Clear all dependencies
        mockCriticalPathService.clearDependencies();
        
        // Rebuild dependencies excluding the one we want to remove
        for (Map.Entry<String, List<String>> entry : dependencyGraph.entrySet()) {
            String dependent = entry.getKey();
            List<String> dependencies = entry.getValue();
            
            for (String dependency : dependencies) {
                // Skip the dependency we want to remove
                if (dependent.equals(dependentId.toString()) && dependency.equals(blockerId.toString())) {
                    continue;
                }
                
                mockCriticalPathService.addDependency(dependent, dependency);
            }
        }
        
        return true;
    }
    
    @Override
    public boolean hasDependency(UUID dependentId, UUID blockerId) {
        // Get the item's critical path details
        Map<String, Object> itemPath = mockCriticalPathService.getItemCriticalPath(dependentId.toString());
        
        // Check if the item is on the critical path
        if (!(boolean) itemPath.getOrDefault("onCriticalPath", false)) {
            return false;
        }
        
        // Get direct dependencies
        @SuppressWarnings("unchecked")
        List<String> directDeps = (List<String>) itemPath.get("directDependencies");
        
        // Check if blockerId is in the direct dependencies
        return directDeps != null && directDeps.contains(blockerId.toString());
    }
    
    /**
     * Converts a CLI WorkItem to a domain WorkItem.
     *
     * @param cliItem the CLI work item
     * @return the domain work item
     */
    private DomainWorkItem convertToDomainWorkItem(org.rinna.cli.model.WorkItem cliItem) {
        // First try to use the ModelMapper utility if available
        try {
            return ModelMapper.toDomainWorkItem(cliItem);
        } catch (Exception e) {
            // Fallback to direct conversion
            DefaultDomainWorkItem domainItem = new DefaultDomainWorkItem();
            
            if (cliItem.getId() != null) {
                try {
                    domainItem.setId(UUID.fromString(cliItem.getId()));
                } catch (IllegalArgumentException ex) {
                    domainItem.setId(UUID.randomUUID());
                }
            } else {
                domainItem.setId(UUID.randomUUID());
            }
            
            domainItem.setTitle(cliItem.getTitle());
            domainItem.setDescription(cliItem.getDescription());
            if (cliItem.getType() != null) {
                domainItem.setType(StateMapper.toDomainType(cliItem.getType()));
            }
            if (cliItem.getStatus() != null) {
                domainItem.setState(StateMapper.toDomainState(cliItem.getStatus()));
            }
            if (cliItem.getPriority() != null) {
                domainItem.setPriority(StateMapper.toDomainPriority(cliItem.getPriority()));
            }
            domainItem.setAssignee(cliItem.getAssignee());
            domainItem.setReporter(cliItem.getReporter());
            
            return domainItem;
        }
    }
}