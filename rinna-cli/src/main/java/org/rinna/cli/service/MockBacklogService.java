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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.util.ModelMapper;

/**
 * Mock implementation of backlog service functionality for CLI use.
 * 
 * This class implements both the CLI-specific backlog functionality
 * and the domain BacklogService interface using the adapter pattern.
 */
public class MockBacklogService implements BacklogService {
    private final Map<String, Integer> backlogPriorities = new HashMap<>();
    private final List<WorkItem> backlogItems = new ArrayList<>();
    private final Map<String, Map<String, List<WorkItem>>> userBacklogs = new HashMap<>();
    private String currentUser = System.getProperty("user.name");
    
    /**
     * Adds a work item to the backlog.
     *
     * @param workItem the work item to add
     * @return the added work item
     */
    public WorkItem addToBacklog(WorkItem workItem) {
        backlogItems.add(workItem);
        backlogPriorities.put(workItem.getId(), backlogItems.size());
        
        // Also add to user backlog
        String username = currentUser;
        userBacklogs.computeIfAbsent(username, k -> new HashMap<>())
                   .computeIfAbsent("backlog", k -> new ArrayList<>())
                   .add(workItem);
                   
        return workItem;
    }
    
    /**
     * Gets all items in the backlog.
     *
     * @return a list of backlog items
     */
    public List<WorkItem> getBacklogItems() {
        return new ArrayList<>(backlogItems);
    }
    
    /**
     * Gets the priority of a work item in the backlog.
     *
     * @param workItemId the work item ID
     * @return the backlog priority, or Integer.MAX_VALUE if not found
     */
    public int getBacklogPriority(String workItemId) {
        return backlogPriorities.getOrDefault(workItemId, Integer.MAX_VALUE);
    }
    
    /**
     * Sets the priority of a work item in the backlog.
     *
     * @param workItemId the work item ID
     * @param priority the priority to set
     */
    public void setBacklogPriority(String workItemId, int priority) {
        backlogPriorities.put(workItemId, priority);
    }
    
    /**
     * Sets the priority enum value of a work item.
     *
     * @param workItemId the work item ID
     * @param priority the priority enum value to set
     * @return true if successful, false otherwise
     */
    public boolean setPriority(String workItemId, org.rinna.cli.model.Priority priority) {
        // Find the work item in user backlogs or general backlog
        WorkItem targetItem = null;
        
        // Check in user backlogs
        for (Map<String, List<WorkItem>> userBacklog : userBacklogs.values()) {
            for (List<WorkItem> items : userBacklog.values()) {
                for (WorkItem item : items) {
                    if (item.getId().equals(workItemId)) {
                        item.setPriority(priority);
                        targetItem = item;
                        break;
                    }
                }
                if (targetItem != null) break;
            }
            if (targetItem != null) break;
        }
        
        // Check in general backlog if not found
        if (targetItem == null) {
            for (WorkItem item : backlogItems) {
                if (item.getId().equals(workItemId)) {
                    item.setPriority(priority);
                    targetItem = item;
                    break;
                }
            }
        }
        
        return targetItem != null;
    }
    
    /**
     * Removes a work item from the backlog.
     *
     * @param workItemId the work item ID to remove
     * @return true if the item was removed, false if not found
     */
    public boolean removeFromBacklog(String workItemId) {
        backlogPriorities.remove(workItemId);
        
        // Also remove from user backlogs
        for (Map<String, List<WorkItem>> userBacklog : userBacklogs.values()) {
            userBacklog.getOrDefault("backlog", Collections.emptyList())
                      .removeIf(item -> item.getId().equals(workItemId));
        }
        
        return backlogItems.removeIf(item -> item.getId().equals(workItemId));
    }
    
    /**
     * Sets the current user context for this service.
     *
     * @param username the username to set as current user
     */
    public void setCurrentUser(String username) {
        this.currentUser = username;
    }
    
    /**
     * Gets the current user context for this service.
     *
     * @return the current username
     */
    public String getCurrentUser() {
        return currentUser;
    }
    
    // Domain BacklogService implementation
    
    // Mock implementation
    public List<org.rinna.cli.domain.model.DomainWorkItem> getBacklogAsDomain() {
        return getBacklogForUser(currentUser).stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    // Mock implementation
    public List<org.rinna.cli.domain.model.DomainWorkItem> getBacklogAsDomain(String username) {
        return getBacklogForUser(username).stream()
                .map(ModelMapper::toDomainWorkItem)
                .collect(Collectors.toList());
    }
    
    // Mock implementation
    public boolean addToBacklog(UUID workItemId) {
        return addToBacklog(workItemId, currentUser);
    }
    
    // Mock implementation
    public boolean addToBacklog(UUID workItemId, String username) {
        // Get the work item from the item service
        ServiceManager serviceManager = ServiceManager.getInstance();
        MockItemService itemService = serviceManager.getMockItemService();
        WorkItem item = itemService.getItem(workItemId.toString());
        
        if (item == null) {
            return false;
        }
        
        // Add to the user's backlog
        userBacklogs.computeIfAbsent(username, k -> new HashMap<>())
                   .computeIfAbsent("backlog", k -> new ArrayList<>())
                   .add(item);
        
        return true;
    }
    
    // Mock implementation
    public boolean removeFromBacklog(UUID workItemId) {
        String itemId = workItemId.toString();
        boolean removed = false;
        
        // Remove from user's backlog
        Map<String, List<WorkItem>> userBacklog = userBacklogs.get(currentUser);
        if (userBacklog != null && userBacklog.containsKey("backlog")) {
            removed = userBacklog.get("backlog").removeIf(item -> item.getId().equals(itemId));
        }
        
        // Also remove from general backlog
        if (backlogItems.removeIf(item -> item.getId().equals(itemId))) {
            removed = true;
            backlogPriorities.remove(itemId);
        }
        
        return removed;
    }
    
    // Mock implementation
    public boolean moveInBacklogImpl(UUID workItemId, int position) {
        Map<String, List<WorkItem>> userBacklog = userBacklogs.get(currentUser);
        if (userBacklog == null || !userBacklog.containsKey("backlog")) {
            return false;
        }
        
        List<WorkItem> backlog = userBacklog.get("backlog");
        
        // Find the work item in the backlog
        int currentPosition = -1;
        WorkItem targetItem = null;
        for (int i = 0; i < backlog.size(); i++) {
            if (backlog.get(i).getId().equals(workItemId.toString())) {
                currentPosition = i;
                targetItem = backlog.get(i);
                break;
            }
        }
        
        if (currentPosition == -1 || targetItem == null) {
            return false;
        }
        
        // Remove from current position
        backlog.remove(currentPosition);
        
        // Insert at the new position
        position = Math.min(position, backlog.size());
        position = Math.max(position, 0);
        backlog.add(position, targetItem);
        
        // Update priorities
        updatePriorities(backlog);
        
        return true;
    }
    
    // Mock implementation
    public boolean moveUp(UUID workItemId) {
        Map<String, List<WorkItem>> userBacklog = userBacklogs.get(currentUser);
        if (userBacklog == null || !userBacklog.containsKey("backlog")) {
            return false;
        }
        
        List<WorkItem> backlog = userBacklog.get("backlog");
        
        // Find the work item in the backlog
        int currentPosition = -1;
        for (int i = 0; i < backlog.size(); i++) {
            if (backlog.get(i).getId().equals(workItemId.toString())) {
                currentPosition = i;
                break;
            }
        }
        
        if (currentPosition <= 0) {
            return false;  // Already at the top or not found
        }
        
        // Swap with the item above
        WorkItem item = backlog.get(currentPosition);
        backlog.set(currentPosition, backlog.get(currentPosition - 1));
        backlog.set(currentPosition - 1, item);
        
        // Update priorities
        updatePriorities(backlog);
        
        return true;
    }
    
    // Mock implementation
    public boolean moveDown(UUID workItemId) {
        Map<String, List<WorkItem>> userBacklog = userBacklogs.get(currentUser);
        if (userBacklog == null || !userBacklog.containsKey("backlog")) {
            return false;
        }
        
        List<WorkItem> backlog = userBacklog.get("backlog");
        
        // Find the work item in the backlog
        int currentPosition = -1;
        for (int i = 0; i < backlog.size(); i++) {
            if (backlog.get(i).getId().equals(workItemId.toString())) {
                currentPosition = i;
                break;
            }
        }
        
        if (currentPosition == -1 || currentPosition >= backlog.size() - 1) {
            return false;  // Already at the bottom or not found
        }
        
        // Swap with the item below
        WorkItem item = backlog.get(currentPosition);
        backlog.set(currentPosition, backlog.get(currentPosition + 1));
        backlog.set(currentPosition + 1, item);
        
        // Update priorities
        updatePriorities(backlog);
        
        return true;
    }
    
    // Mock implementation
    public boolean moveToTop(UUID workItemId) {
        return moveInBacklogImpl(workItemId, 0);
    }
    
    // Mock implementation
    public boolean moveToBottom(UUID workItemId) {
        Map<String, List<WorkItem>> userBacklog = userBacklogs.get(currentUser);
        if (userBacklog == null || !userBacklog.containsKey("backlog")) {
            return false;
        }
        
        return moveInBacklogImpl(workItemId, userBacklog.get("backlog").size() - 1);
    }
    
    /**
     * Helper method to get a user's backlog.
     * 
     * @param username the username
     * @return the user's backlog items
     */
    private List<WorkItem> getBacklogForUser(String username) {
        Map<String, List<WorkItem>> userBacklog = userBacklogs.get(username);
        if (userBacklog == null || !userBacklog.containsKey("backlog")) {
            return Collections.emptyList();
        }
        
        // Return a sorted copy of the backlog based on priority
        List<WorkItem> result = new ArrayList<>(userBacklog.get("backlog"));
        result.sort(Comparator.comparingInt(item -> 
            backlogPriorities.getOrDefault(item.getId(), Integer.MAX_VALUE)));
        
        return result;
    }
    
    /**
     * Updates the priorities based on the current order of the backlog.
     * 
     * @param backlog the backlog to update
     */
    private void updatePriorities(List<WorkItem> backlog) {
        for (int i = 0; i < backlog.size(); i++) {
            backlogPriorities.put(backlog.get(i).getId(), i);
        }
    }
    
    @Override
    public List<WorkItem> getBacklog(String user) {
        return getBacklogForUser(user);
    }
    
    @Override
    public List<WorkItem> getBacklogSection(String user, String section) {
        Map<String, List<WorkItem>> userBacklog = userBacklogs.get(user);
        if (userBacklog == null || !userBacklog.containsKey(section)) {
            return Collections.emptyList();
        }
        
        return new ArrayList<>(userBacklog.get(section));
    }
    
    @Override
    public boolean addToBacklog(String user, WorkItem workItem) {
        userBacklogs.computeIfAbsent(user, k -> new HashMap<>())
                   .computeIfAbsent("backlog", k -> new ArrayList<>())
                   .add(workItem);
        return true;
    }
    
    @Override
    public boolean addToBacklogSection(String user, String section, WorkItem workItem) {
        userBacklogs.computeIfAbsent(user, k -> new HashMap<>())
                   .computeIfAbsent(section, k -> new ArrayList<>())
                   .add(workItem);
        return true;
    }
    
    @Override
    public boolean removeFromBacklog(String user, String itemId) {
        Map<String, List<WorkItem>> userBacklog = userBacklogs.get(user);
        if (userBacklog == null) {
            return false;
        }
        
        boolean removed = false;
        for (List<WorkItem> sectionItems : userBacklog.values()) {
            if (sectionItems.removeIf(item -> item.getId().equals(itemId))) {
                removed = true;
            }
        }
        
        return removed;
    }
    
    @Override
    public boolean moveInBacklog(UUID workItemId, int position) {
        // Call the implementation method to avoid recursive calls
        return moveInBacklogImpl(workItemId, position);
    }
    
    @Override
    public boolean prioritizeInBacklog(UUID workItemId, int priority) {
        setBacklogPriority(workItemId.toString(), priority);
        return true;
    }
    
    @Override
    public Map<String, List<WorkItem>> getAllBacklogs() {
        Map<String, List<WorkItem>> result = new HashMap<>();
        
        for (String user : userBacklogs.keySet()) {
            result.put(user, getBacklogForUser(user));
        }
        
        return result;
    }
    
    @Override
    public boolean createBacklog(String user) {
        userBacklogs.computeIfAbsent(user, k -> new HashMap<>())
                   .computeIfAbsent("backlog", k -> new ArrayList<>());
        return true;
    }
    
    @Override
    public int getBacklogSize(String user) {
        Map<String, List<WorkItem>> userBacklog = userBacklogs.get(user);
        if (userBacklog == null || !userBacklog.containsKey("backlog")) {
            return 0;
        }
        
        return userBacklog.get("backlog").size();
    }
    
    @Override
    public boolean moveToSection(String user, UUID itemId, String fromSection, String toSection) {
        Map<String, List<WorkItem>> userBacklog = userBacklogs.get(user);
        if (userBacklog == null || !userBacklog.containsKey(fromSection)) {
            return false;
        }
        
        List<WorkItem> fromItems = userBacklog.get(fromSection);
        WorkItem itemToMove = null;
        
        // Find the item in the from section
        for (WorkItem item : fromItems) {
            if (item.getId().equals(itemId.toString())) {
                itemToMove = item;
                break;
            }
        }
        
        if (itemToMove == null) {
            return false;
        }
        
        // Remove from the from section
        fromItems.remove(itemToMove);
        
        // Add to the to section
        userBacklog.computeIfAbsent(toSection, k -> new ArrayList<>()).add(itemToMove);
        
        return true;
    }
}