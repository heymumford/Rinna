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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.rinna.cli.domain.model.DomainPriority;
import org.rinna.cli.domain.model.DomainWorkItem;
import org.rinna.cli.domain.model.DomainWorkItemType;
import org.rinna.cli.domain.service.ItemService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.util.ModelMapper;
import org.rinna.cli.util.StateMapper;

/**
 * Adapter that implements the CLI domain ItemService interface
 * while delegating to the CLI MockItemService implementation.
 * This adapter bridges between domain interfaces and CLI implementations.
 */
public class ItemServiceAdapter implements ItemService {

    private final MockItemService mockItemService;
    
    /**
     * Constructs a new ItemServiceAdapter.
     * 
     * @param mockItemService the CLI item service to delegate to
     */
    public ItemServiceAdapter(MockItemService mockItemService) {
        this.mockItemService = mockItemService;
    }
    
    @Override
    public DomainWorkItem create(UUID id, String title, DomainWorkItemType type, DomainPriority priority, String description) {
        // Create a new CLI work item
        org.rinna.cli.model.WorkItem cliItem = new org.rinna.cli.model.WorkItem();
        cliItem.setId(id.toString());
        cliItem.setTitle(title);
        cliItem.setDescription(description);
        
        // Convert enums using StateMapper
        if (type != null) {
            cliItem.setType(StateMapper.fromDomainType(type));
        }
        
        if (priority != null) {
            cliItem.setPriority(StateMapper.fromDomainPriority(priority));
        }
        
        // Create the item using the CLI service
        org.rinna.cli.model.WorkItem createdItem = mockItemService.createItem(cliItem);
        
        // Convert back to domain model
        return ModelMapper.toDomainWorkItem(createdItem);
    }

    @Override
    public Optional<DomainWorkItem> findById(UUID id) {
        // Get the item from the CLI service
        org.rinna.cli.model.WorkItem cliItem = mockItemService.getItem(id.toString());
        
        // Convert to domain model if found
        if (cliItem != null) {
            return Optional.of(ModelMapper.toDomainWorkItem(cliItem));
        }
        
        return Optional.empty();
    }

    @Override
    public List<DomainWorkItem> findAll() {
        // Get all items from the CLI service
        List<org.rinna.cli.model.WorkItem> cliItems = mockItemService.getAllItems();
        
        // Convert to domain models
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainWorkItem> findByType(DomainWorkItemType type) {
        // Convert domain type to CLI type
        org.rinna.cli.model.WorkItemType cliType = StateMapper.fromDomainType(type);
        
        // Get items from CLI service
        List<org.rinna.cli.model.WorkItem> cliItems = mockItemService.findByType(cliType);
        
        // Convert to domain models
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }

    @Override
    public List<DomainWorkItem> findByAssignee(String assignee) {
        // Get items from CLI service
        List<org.rinna.cli.model.WorkItem> cliItems = mockItemService.findByAssignee(assignee);
        
        // Convert to domain models
        return cliItems.stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }

    @Override
    public DomainWorkItem updateAssignee(UUID id, String assignee) {
        // Update assignee using CLI service
        org.rinna.cli.model.WorkItem cliItem = mockItemService.updateAssignee(id.toString(), assignee);
        
        // Convert to domain model if successful
        if (cliItem != null) {
            return ModelMapper.toDomainWorkItem(cliItem);
        }
        
        return null;
    }

    @Override
    public void deleteById(UUID id) {
        // Delete the item using CLI service
        mockItemService.deleteItem(id.toString());
    }

    @Override
    public boolean existsById(UUID id) {
        // Check if item exists using CLI service
        return mockItemService.exists(id.toString());
    }

    @Override
    public boolean updateMetadata(UUID id, Map<String, String> metadata) {
        // Update metadata using CLI service
        return mockItemService.updateMetadata(id.toString(), metadata);
    }
}