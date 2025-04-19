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
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages context for the CLI commands, such as tracking the last viewed work item.
 * This singleton class provides a way to maintain state between command invocations.
 */
public final class ContextManager {
    
    private static ContextManager instance;
    
    private final ConcurrentHashMap<String, UUID> lastViewedItems;
    private final ConcurrentHashMap<String, Object> sessionValues;
    private final ConcurrentHashMap<String, List<String>> searchHistory;
    private static final int MAX_HISTORY_SIZE = 20; // Maximum number of searches to remember
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private ContextManager() {
        lastViewedItems = new ConcurrentHashMap<>();
        sessionValues = new ConcurrentHashMap<>();
        searchHistory = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets the singleton instance of the ContextManager.
     *
     * @return the singleton instance
     */
    public static synchronized ContextManager getInstance() {
        if (instance == null) {
            instance = new ContextManager();
        }
        return instance;
    }
    
    /**
     * Sets the last viewed work item for the current user.
     *
     * @param workItemId the ID of the work item
     */
    public void setLastViewedWorkItem(UUID workItemId) {
        String username = System.getProperty("user.name");
        lastViewedItems.put(username, workItemId);
    }
    
    /**
     * Sets the last viewed work item for the current user using a String ID.
     *
     * @param workItemId the ID of the work item as a String
     */
    public void setLastViewedWorkItem(String workItemId) {
        try {
            UUID id = UUID.fromString(workItemId);
            setLastViewedWorkItem(id);
        } catch (IllegalArgumentException e) {
            // If the ID is not a valid UUID, we can't store it
            // This is a graceful fallback for non-UUID IDs
        }
    }
    
    /**
     * Sets the current item ID (alias for setLastViewedWorkItem)
     *
     * @param itemId the ID of the work item as a String
     */
    public void setCurrentItemId(String itemId) {
        setLastViewedWorkItem(itemId);
    }
    
    /**
     * Sets the last viewed work item (alias for setLastViewedWorkItem)
     *
     * @param item the work item to set as last viewed
     */
    public void setLastViewedItem(org.rinna.cli.model.WorkItem item) {
        if (item != null && item.getId() != null) {
            setLastViewedWorkItem(item.getId());
        }
    }
    
    /**
     * Gets the last viewed work item for the current user.
     *
     * @return the UUID of the last viewed work item, or null if none
     */
    public UUID getLastViewedWorkItem() {
        String username = System.getProperty("user.name");
        return lastViewedItems.get(username);
    }
    
    /**
     * Sets a value in the session for the current user.
     *
     * @param key the key for the value
     * @param value the value to store
     */
    public void setSessionValue(String key, Object value) {
        String username = System.getProperty("user.name");
        sessionValues.put(username + "." + key, value);
    }
    
    /**
     * Gets a value from the session for the current user.
     *
     * @param key the key for the value
     * @param <T> the type of the value
     * @return the value, or null if not found
     */
    @SuppressWarnings("unchecked")
    public <T> T getSessionValue(String key) {
        String username = System.getProperty("user.name");
        return (T) sessionValues.get(username + "." + key);
    }
    
    /**
     * Adds a search pattern to the user's search history.
     *
     * @param pattern the search pattern
     */
    public void addToSearchHistory(String pattern) {
        String username = System.getProperty("user.name");
        List<String> history = searchHistory.computeIfAbsent(username, k -> new ArrayList<>());
        
        // Remove if already exists to avoid duplicates
        history.remove(pattern);
        
        // Add to the beginning of the list
        history.add(0, pattern);
        
        // Trim history if it grows too large
        if (history.size() > MAX_HISTORY_SIZE) {
            history = history.subList(0, MAX_HISTORY_SIZE);
            searchHistory.put(username, history);
        }
    }
    
    /**
     * Gets the search history for the current user.
     *
     * @return the search history
     */
    public List<String> getSearchHistory() {
        String username = System.getProperty("user.name");
        List<String> history = searchHistory.get(username);
        if (history == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(history); // Return a copy to prevent modification
    }
    
    /**
     * Clears all context for the current user.
     */
    public void clearContext() {
        String username = System.getProperty("user.name");
        lastViewedItems.remove(username);
        searchHistory.remove(username);
        
        // Remove all session values for this user
        sessionValues.keySet().removeIf(k -> k.startsWith(username + "."));
    }
}