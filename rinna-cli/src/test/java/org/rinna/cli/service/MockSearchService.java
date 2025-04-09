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

import org.rinna.cli.domain.model.SearchResult;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mock implementation of search service functionality for CLI use.
 */
public class MockSearchService implements SearchService {

    private final List<SearchResult> results = new ArrayList<>();
    private boolean throwException = false;
    
    /**
     * Gets all items in the search results.
     * 
     * @return a list of all search results
     */
    public List<SearchResult> getAllResults() {
        return new ArrayList<>(results);
    }
    
    /**
     * Adds a search result to the mock service.
     * 
     * @param result the search result to add
     */
    public void addResult(SearchResult result) {
        results.add(result);
    }
    
    /**
     * Clears all search results.
     */
    public void clearResults() {
        results.clear();
    }
    
    /**
     * Sets whether to throw an exception during searches.
     * 
     * @param throwException true to throw an exception
     */
    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }
    
    /**
     * Gets all items.
     * 
     * @return a list of all work items
     */
    public List<WorkItem> getAllItems() {
        // Create some sample work items for demonstration
        List<WorkItem> workItems = new ArrayList<>();
        
        WorkItem item1 = createWorkItem("Feature One", "This is a feature item");
        item1.setType(WorkItemType.FEATURE);
        item1.setPriority(Priority.HIGH);
        item1.setState(WorkflowState.READY);
        
        WorkItem item2 = createWorkItem("Bug Two", "This is a bug item");
        item2.setType(WorkItemType.BUG);
        item2.setPriority(Priority.MEDIUM);
        item2.setState(WorkflowState.IN_PROGRESS);
        
        WorkItem item3 = createWorkItem("Task Three", "This is a task item");
        item3.setType(WorkItemType.TASK);
        item3.setPriority(Priority.LOW);
        item3.setState(WorkflowState.DONE);
        
        workItems.add(item1);
        workItems.add(item2);
        workItems.add(item3);
        
        return workItems;
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
        if (throwException) {
            throw new RuntimeException("Test exception");
        }
        
        // Simple text search - checks if the text appears in title or description
        String lowerText = text.toLowerCase();
        return getAllItems().stream()
            .filter(item -> {
                String lowerTitle = item.getTitle().toLowerCase();
                String lowerDesc = item.getDescription() != null ? 
                    item.getDescription().toLowerCase() : "";
                return lowerTitle.contains(lowerText) || lowerDesc.contains(lowerText);
            })
            .toList();
    }
    
    @Override
    public List<WorkItem> searchByTextAndState(String text, WorkflowState state) {
        if (throwException) {
            throw new RuntimeException("Test exception");
        }
        
        return searchByText(text).stream()
            .filter(item -> state.equals(item.getState()))
            .toList();
    }
    
    @Override
    public List<WorkItem> searchByTextAndType(String text, WorkItemType type) {
        if (throwException) {
            throw new RuntimeException("Test exception");
        }
        
        return searchByText(text).stream()
            .filter(item -> type.equals(item.getType()))
            .toList();
    }
    
    @Override
    public List<WorkItem> searchByTextAndPriority(String text, Priority priority) {
        if (throwException) {
            throw new RuntimeException("Test exception");
        }
        
        return searchByText(text).stream()
            .filter(item -> priority.equals(item.getPriority()))
            .toList();
    }
    
    @Override
    public List<SearchResult> findText(String text) {
        return findText(text, false);
    }
    
    @Override
    public List<SearchResult> findText(String text, boolean caseSensitive) {
        if (throwException) {
            throw new RuntimeException("Test exception");
        }
        
        return results.stream()
            .filter(result -> {
                if (caseSensitive) {
                    return result.getSearchTerm().contains(text);
                } else {
                    return result.getSearchTerm().toLowerCase().contains(text.toLowerCase());
                }
            })
            .toList();
    }
    
    @Override
    public List<SearchResult> findPattern(String pattern) {
        return findPattern(pattern, false);
    }
    
    @Override
    public List<SearchResult> findPattern(String pattern, boolean caseSensitive) {
        if (throwException) {
            throw new RuntimeException("Test exception");
        }
        
        return results.stream()
            .filter(result -> {
                if (caseSensitive) {
                    return result.getSearchTerm().matches(pattern);
                } else {
                    return result.getSearchTerm().toLowerCase().matches(pattern.toLowerCase());
                }
            })
            .toList();
    }
    
    @Override
    public List<WorkItem> findWorkItems(Map<String, String> criteria, int limit) {
        if (throwException) {
            throw new RuntimeException("Test exception");
        }
        
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
                        .toList();
                    break;
                case "status":
                case "state":
                    filteredItems = filteredItems.stream()
                        .filter(item -> item.getState() != null && 
                            item.getState().toString().toLowerCase().equals(value))
                        .toList();
                    break;
                case "priority":
                    filteredItems = filteredItems.stream()
                        .filter(item -> item.getPriority() != null && 
                            item.getPriority().toString().toLowerCase().equals(value))
                        .toList();
                    break;
                case "assignee":
                    filteredItems = filteredItems.stream()
                        .filter(item -> item.getAssignee() != null && 
                            item.getAssignee().toLowerCase().contains(value))
                        .toList();
                    break;
                case "title":
                    filteredItems = filteredItems.stream()
                        .filter(item -> item.getTitle() != null && 
                            item.getTitle().toLowerCase().contains(value))
                        .toList();
                    break;
                case "description":
                    filteredItems = filteredItems.stream()
                        .filter(item -> item.getDescription() != null && 
                            item.getDescription().toLowerCase().contains(value))
                        .toList();
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