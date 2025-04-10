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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

/**
 * Mock implementation of search service functionality for CLI use.
 */
public class MockSearchService implements SearchService {

    // Not marked as final anymore to allow initialization later
    private MockItemService mockItemService;
    
    /**
     * Creates a new MockSearchService.
     */
    public MockSearchService() {
        // Default constructor - used by test subclasses
        this.mockItemService = null;
    }
    
    /**
     * Internal method to initialize the mockItemService.
     * Not called by test subclasses to avoid infinite recursion.
     */
    public void initialize() {
        if (this.mockItemService == null) {
            this.mockItemService = (MockItemService) ServiceManager.getInstance().getItemService();
        }
    }
    
    /**
     * Gets all items.
     * 
     * @return a list of all work items
     */
    public List<WorkItem> getAllItems() {
        // In test context or when not fully initialized, just return sample items
        List<WorkItem> results = new ArrayList<>();
        
        // Add some sample work items for demonstration
        WorkItem item1 = createWorkItem("Implement authentication feature", "Create JWT-based authentication for API endpoints");
        WorkItem item2 = createWorkItem("Fix bug in payment module", "Transaction history is not updating after payment completion");
        WorkItem item3 = createWorkItem("Update documentation", "Add API reference documentation for new endpoints");
        
        results.add(item1);
        results.add(item2);
        results.add(item3);
        
        return results;
    }
    
    /**
     * Creates a simple work item for testing.
     *
     * @param title the title
     * @param description the description
     * @return a work item
     */
    private WorkItem createWorkItem(String title, String description) {
        WorkItem item = new WorkItem();
        item.setId(UUID.randomUUID().toString());
        item.setTitle(title);
        item.setDescription(description);
        item.setType(WorkItemType.TASK);
        item.setPriority(Priority.MEDIUM);
        item.setState(WorkflowState.READY);
        return item;
    }
    
    @Override
    public List<WorkItem> searchByText(String text) {
        // Simple text search - checks if the text appears in title or description
        String lowerText = text.toLowerCase();
        return getAllItems().stream()
            .filter(item -> {
                String lowerTitle = item.getTitle().toLowerCase();
                String lowerDesc = item.getDescription() != null ? 
                    item.getDescription().toLowerCase() : "";
                return lowerTitle.contains(lowerText) || lowerDesc.contains(lowerText);
            })
            .collect(Collectors.toList());
    }
    
    @Override
    public List<WorkItem> searchByTextAndState(String text, WorkflowState state) {
        return searchByText(text).stream()
            .filter(item -> state.equals(item.getState()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<WorkItem> searchByTextAndType(String text, WorkItemType type) {
        return searchByText(text).stream()
            .filter(item -> type.equals(item.getType()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<WorkItem> searchByTextAndPriority(String text, Priority priority) {
        return searchByText(text).stream()
            .filter(item -> priority.equals(item.getPriority()))
            .collect(Collectors.toList());
    }
    
    @Override
    public List<WorkItem> findText(String text) {
        return findText(text, false);
    }
    
    @Override
    public List<WorkItem> findText(String text, boolean caseSensitive) {
        List<WorkItem> results = new ArrayList<>();
        
        for (WorkItem item : getAllItems()) {
            String title = item.getTitle();
            String desc = item.getDescription();
            boolean foundInTitle = false;
            boolean foundInDesc = false;
            
            if (caseSensitive) {
                foundInTitle = title != null && title.contains(text);
                foundInDesc = desc != null && desc.contains(text);
            } else {
                String lowerText = text.toLowerCase();
                foundInTitle = title != null && title.toLowerCase().contains(lowerText);
                foundInDesc = desc != null && desc.toLowerCase().contains(lowerText);
            }
            
            if (foundInTitle || foundInDesc) {
                results.add(item);
            }
        }
        
        return results;
    }
    
    // Using default implementation of findItemsByText from the interface
    
    /**
     * Find work items by metadata.
     *
     * @param metadata the metadata to search for
     * @return a list of work items with matching metadata
     */
    @Override
    public List<WorkItem> findItemsByMetadata(Map<String, String> metadata) {
        List<WorkItem> results = new ArrayList<>();
        
        for (WorkItem item : getAllItems()) {
            boolean matches = true;
            
            for (Map.Entry<String, String> entry : metadata.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                
                // Check if this metadata matches the item
                if ("project".equals(key)) {
                    if (item.getProject() == null || !item.getProject().equals(value)) {
                        matches = false;
                        break;
                    }
                } else if ("assignee".equals(key)) {
                    if (item.getAssignee() == null || !item.getAssignee().equals(value)) {
                        matches = false;
                        break;
                    }
                } else if ("reporter".equals(key)) {
                    if (item.getReporter() == null || !item.getReporter().equals(value)) {
                        matches = false;
                        break;
                    }
                }
                // Add other metadata fields as needed
            }
            
            if (matches) {
                results.add(item);
            }
        }
        
        return results;
    }
    
    @Override
    public List<WorkItem> findPattern(String pattern) {
        return findPattern(pattern, false);
    }
    
    @Override
    public List<WorkItem> findPattern(String pattern, boolean caseSensitive) {
        List<WorkItem> results = new ArrayList<>();
        
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
        Pattern regex = Pattern.compile(pattern, flags);
        
        for (WorkItem item : getAllItems()) {
            String title = item.getTitle();
            String desc = item.getDescription();
            
            boolean foundInTitle = title != null && regex.matcher(title).find();
            boolean foundInDesc = desc != null && regex.matcher(desc).find();
            
            if (foundInTitle || foundInDesc) {
                results.add(item);
            }
        }
        
        return results;
    }
    
    @Override
    public List<WorkItem> findWorkItems(Map<String, String> criteria, int limit) {
        List<WorkItem> allItems = getAllItems();
        List<WorkItem> filteredItems = new ArrayList<>(allItems);
        
        // Filter items based on criteria
        for (Map.Entry<String, String> entry : criteria.entrySet()) {
            String key = entry.getKey().toLowerCase();
            String value = entry.getValue().toLowerCase();
            
            switch (key) {
                case "type":
                    filteredItems = filteredItems.stream()
                        .filter(item -> item.getType() != null && 
                            item.getType().toString().toLowerCase().equals(value))
                        .collect(Collectors.toList());
                    break;
                case "status":
                case "state":
                    filteredItems = filteredItems.stream()
                        .filter(item -> item.getState() != null && 
                            item.getState().toString().toLowerCase().equals(value))
                        .collect(Collectors.toList());
                    break;
                case "priority":
                    filteredItems = filteredItems.stream()
                        .filter(item -> item.getPriority() != null && 
                            item.getPriority().toString().toLowerCase().equals(value))
                        .collect(Collectors.toList());
                    break;
                case "assignee":
                    filteredItems = filteredItems.stream()
                        .filter(item -> item.getAssignee() != null && 
                            item.getAssignee().toLowerCase().contains(value))
                        .collect(Collectors.toList());
                    break;
                case "title":
                    filteredItems = filteredItems.stream()
                        .filter(item -> item.getTitle() != null && 
                            item.getTitle().toLowerCase().contains(value))
                        .collect(Collectors.toList());
                    break;
                case "description":
                    filteredItems = filteredItems.stream()
                        .filter(item -> item.getDescription() != null && 
                            item.getDescription().toLowerCase().contains(value))
                        .collect(Collectors.toList());
                    break;
                default:
                    // Ignore unknown criteria
            }
        }
        
        // Limit results
        if (limit > 0 && filteredItems.size() > limit) {
            return filteredItems.subList(0, limit);
        }
        
        return filteredItems;
    }
}