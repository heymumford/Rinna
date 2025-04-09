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

import org.rinna.cli.domain.model.DomainWorkItem;
import org.rinna.cli.domain.service.BacklogService;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.service.MockBacklogService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.ModelMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Adapter that implements the CLI domain BacklogService interface 
 * while delegating to the CLI MockBacklogService.
 * This adapter bridges between domain interfaces and CLI implementations.
 */
public class BacklogServiceAdapter implements BacklogService {
    
    private final MockBacklogService mockBacklogService;
    private final MockItemService mockItemService;
    
    /**
     * Constructs a new BacklogServiceAdapter.
     * 
     * @param mockBacklogService the CLI backlog service to delegate to
     */
    public BacklogServiceAdapter(MockBacklogService mockBacklogService) {
        this.mockBacklogService = mockBacklogService;
        
        // Get the item service for model conversions
        ServiceManager serviceManager = ServiceManager.getInstance();
        this.mockItemService = serviceManager.getMockItemService();
    }
    
    @Override
    public List<DomainWorkItem> getBacklog() {
        // Get the current user's backlog from the mock service
        String currentUser = mockBacklogService.getCurrentUser();
        List<WorkItem> cliItems = mockBacklogService.getBacklog(currentUser);
        
        // Convert CLI items to domain items
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<DomainWorkItem> getBacklog(String username) {
        // Get the specified user's backlog from the mock service
        List<WorkItem> cliItems = mockBacklogService.getBacklog(username);
        
        // Convert CLI items to domain items
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    @Override
    public boolean addToBacklog(UUID workItemId) {
        // Delegate to the mock service
        return mockBacklogService.addToBacklog(workItemId);
    }
    
    @Override
    public boolean addToBacklog(UUID workItemId, String username) {
        // Delegate to the mock service
        return mockBacklogService.addToBacklog(workItemId, username);
    }
    
    @Override
    public boolean removeFromBacklog(UUID workItemId) {
        // Delegate to the mock service
        return mockBacklogService.removeFromBacklog(workItemId);
    }
    
    @Override
    public boolean moveInBacklog(UUID workItemId, int position) {
        // Delegate to the mock service
        return mockBacklogService.moveInBacklog(workItemId, position);
    }
    
    @Override
    public boolean moveUp(UUID workItemId) {
        // Delegate to the mock service
        return mockBacklogService.moveUp(workItemId);
    }
    
    @Override
    public boolean moveDown(UUID workItemId) {
        // Delegate to the mock service
        return mockBacklogService.moveDown(workItemId);
    }
    
    @Override
    public boolean moveToTop(UUID workItemId) {
        // Delegate to the mock service
        return mockBacklogService.moveToTop(workItemId);
    }
    
    @Override
    public boolean moveToBottom(UUID workItemId) {
        // Delegate to the mock service
        return mockBacklogService.moveToBottom(workItemId);
    }
}