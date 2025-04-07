/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.util.ModelMapper;
import org.rinna.domain.model.WorkItemCreateRequest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mock implementation of the domain ItemService interface,
 * adapted to work with CLI models.
 */
public class MockItemService implements org.rinna.domain.service.ItemService {
    
    private final List<WorkItem> items = new ArrayList<>();
    
    /**
     * Constructor initializing some sample work items.
     */
    public MockItemService() {
        // Add some sample items
        WorkItem item1 = new WorkItem();
        item1.setId("123e4567-e89b-12d3-a456-426614174000");
        item1.setTitle("Implement authentication feature");
        item1.setDescription("Create JWT-based authentication for API endpoints");
        item1.setType(WorkItemType.TASK);
        item1.setPriority(Priority.HIGH);
        item1.setState(WorkflowState.IN_PROGRESS);
        item1.setAssignee(System.getProperty("user.name"));
        item1.setProject("DEMO");
        item1.setCreated(LocalDateTime.now().minusDays(5));
        item1.setUpdated(LocalDateTime.now().minusHours(2));
        items.add(item1);
        
        WorkItem item2 = new WorkItem();
        item2.setId("223e4567-e89b-12d3-a456-426614174001");
        item2.setTitle("Fix bug in payment module");
        item2.setDescription("Transaction history is not updating after payment completion");
        item2.setType(WorkItemType.BUG);
        item2.setPriority(Priority.CRITICAL);
        item2.setState(WorkflowState.READY);
        item2.setProject("DEMO");
        item2.setCreated(LocalDateTime.now().minusDays(2));
        item2.setUpdated(LocalDateTime.now().minusDays(2));
        items.add(item2);
        
        WorkItem item3 = new WorkItem();
        item3.setId("323e4567-e89b-12d3-a456-426614174002");
        item3.setTitle("Update documentation");
        item3.setDescription("Add API reference documentation for new endpoints");
        item3.setType(WorkItemType.TASK);
        item3.setPriority(Priority.LOW);
        item3.setState(WorkflowState.DONE);
        item3.setAssignee("jane");
        item3.setProject("DOCS");
        item3.setCreated(LocalDateTime.now().minusDays(10));
        item3.setUpdated(LocalDateTime.now().minusDays(1));
        items.add(item3);
    }
    
    /**
     * Gets all work items from the CLI model.
     *
     * @return a list of all CLI model work items
     */
    public List<WorkItem> getAllCliItems() {
        return new ArrayList<>(items);
    }
    
    /**
     * Gets a specific work item by ID from the CLI model.
     *
     * @param id the work item ID
     * @return the CLI model work item, or null if not found
     */
    public WorkItem getCliItem(String id) {
        return items.stream()
            .filter(item -> id.equals(item.getId()))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Creates a new work item in the CLI model.
     *
     * @param item the CLI model work item to create
     * @return the created CLI model work item
     */
    public WorkItem createCliItem(WorkItem item) {
        // Ensure the item has an ID
        if (item.getId() == null) {
            item.setId(UUID.randomUUID().toString());
        }
        
        // Set creation and update timestamps
        item.setCreated(LocalDateTime.now());
        item.setUpdated(LocalDateTime.now());
        
        // Add to our list
        items.add(item);
        
        return item;
    }
    
    /**
     * Updates an existing work item in the CLI model.
     *
     * @param item the CLI model work item to update
     * @return the updated CLI model work item, or null if not found
     */
    public WorkItem updateCliItem(WorkItem item) {
        // Find the existing item
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(item.getId())) {
                index = i;
                break;
            }
        }
        
        if (index < 0) {
            // Item not found
            return null;
        }
        
        // Get the existing item
        WorkItem existingItem = items.get(index);
        
        // Preserve the creation date
        item.setCreated(existingItem.getCreated());
        
        // Update the modified date
        item.setUpdated(LocalDateTime.now());
        
        // Replace the item in our list
        items.set(index, item);
        
        return item;
    }
    
    /**
     * Deletes a work item from the CLI model.
     *
     * @param id the ID of the work item to delete
     * @return true if deleted, false if not found
     */
    public boolean deleteCliItem(String id) {
        // Find the existing item
        int index = -1;
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i).getId().equals(id)) {
                index = i;
                break;
            }
        }
        
        if (index < 0) {
            // Item not found
            return false;
        }
        
        // Remove the item
        items.remove(index);
        return true;
    }
    
    // Domain ItemService implementation
    
    @Override
    public org.rinna.domain.model.WorkItem create(WorkItemCreateRequest request) {
        WorkItem cliItem = new WorkItem();
        cliItem.setId(UUID.randomUUID().toString());
        cliItem.setTitle(request.getTitle());
        cliItem.setDescription(request.getDescription());
        cliItem.setType(ModelMapper.toCliWorkItemType(request.getType()));
        cliItem.setPriority(ModelMapper.toCliPriority(request.getPriority()));
        cliItem.setState(WorkflowState.CREATED); // Default state for new items
        cliItem.setAssignee(request.getAssignee());
        cliItem.setProject(request.getProjectKey());
        cliItem.setCreated(LocalDateTime.now());
        cliItem.setUpdated(LocalDateTime.now());
        
        items.add(cliItem);
        
        return ModelMapper.toDomainWorkItem(cliItem);
    }
    
    @Override
    public Optional<org.rinna.domain.model.WorkItem> findById(UUID id) {
        return items.stream()
            .filter(item -> id.toString().equals(item.getId()))
            .findFirst()
            .map(ModelMapper::toDomainWorkItem);
    }
    
    @Override
    public List<org.rinna.domain.model.WorkItem> findAll() {
        return items.stream()
            .map(ModelMapper::toDomainWorkItem)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<org.rinna.domain.model.WorkItem> findByType(String type) {
        return items.stream()
            .filter(item -> item.getType() != null && type.equals(item.getType().name()))
            .map(ModelMapper::toDomainWorkItem)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<org.rinna.domain.model.WorkItem> findByStatus(String status) {
        return items.stream()
            .filter(item -> item.getState() != null && status.equals(item.getState().name()))
            .map(ModelMapper::toDomainWorkItem)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<org.rinna.domain.model.WorkItem> findByAssignee(String assignee) {
        return items.stream()
            .filter(item -> assignee.equals(item.getAssignee()))
            .map(ModelMapper::toDomainWorkItem)
            .collect(Collectors.toList());
    }
    
    @Override
    public org.rinna.domain.model.WorkItem updateAssignee(UUID id, String assignee) {
        Optional<WorkItem> optItem = items.stream()
            .filter(item -> id.toString().equals(item.getId()))
            .findFirst();
            
        if (optItem.isPresent()) {
            WorkItem item = optItem.get();
            item.setAssignee(assignee);
            item.setUpdated(LocalDateTime.now());
            return ModelMapper.toDomainWorkItem(item);
        }
        
        return null;
    }
    
    @Override
    public void deleteById(UUID id) {
        items.removeIf(item -> id.toString().equals(item.getId()));
    }
    
    @Override
    public boolean existsById(UUID id) {
        return items.stream()
            .anyMatch(item -> id.toString().equals(item.getId()));
    }
    
    @Override
    public boolean updateMetadata(UUID id, Map<String, String> metadata) {
        Optional<WorkItem> optItem = items.stream()
            .filter(item -> id.toString().equals(item.getId()))
            .findFirst();
            
        if (optItem.isPresent()) {
            WorkItem item = optItem.get();
            if (metadata.containsKey("project")) {
                item.setProject(metadata.get("project"));
            }
            // Handle other metadata fields as needed
            item.setUpdated(LocalDateTime.now());
            return true;
        }
        
        return false;
    }
}