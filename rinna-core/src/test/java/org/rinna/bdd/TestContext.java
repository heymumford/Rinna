/*
 * BDD test context for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.bdd;

import org.rinna.Rinna;
import org.rinna.domain.entity.Release;
import org.rinna.domain.entity.WorkItem;
import org.rinna.domain.entity.WorkItemCreateRequest;

import java.util.*;

/**
 * Test context for sharing state between Cucumber step definitions.
 * This class holds the current state of the test scenario.
 */
public class TestContext {
    private final Rinna rinna;
    private final Map<String, WorkItem> workItems;
    private final Map<String, UUID> workItemIds;
    private final Map<String, WorkItemCreateRequest> createRequests;
    private final Map<String, Release> releases;
    private final Map<UUID, Map<String, String>> workItemMetadata;
    private final Map<String, Object> configurationValues;
    private final Set<String> configurationFlags;
    private Exception lastException;
    
    /**
     * Constructs a new TestContext with default initialization.
     */
    public TestContext() {
        this.rinna = Rinna.initialize();
        this.workItems = new HashMap<>();
        this.workItemIds = new HashMap<>();
        this.createRequests = new HashMap<>();
        this.releases = new HashMap<>();
        this.workItemMetadata = new HashMap<>();
        this.configurationValues = new HashMap<>();
        this.configurationFlags = new HashSet<>();
    }
    
    /**
     * Returns the Rinna instance for this test context.
     *
     * @return the Rinna instance
     */
    public Rinna getRinna() {
        return rinna;
    }
    
    /**
     * Saves a work item in the test context.
     *
     * @param key the key to use for the work item
     * @param workItem the work item to save
     */
    public void saveWorkItem(String key, WorkItem workItem) {
        workItems.put(key, workItem);
        workItemIds.put(key, workItem.getId());
    }
    
    /**
     * Returns a work item from the test context.
     *
     * @param key the key for the work item
     * @return the work item
     */
    public WorkItem getWorkItem(String key) {
        return workItems.get(key);
    }
    
    /**
     * Returns all work item keys in the test context.
     *
     * @return the set of work item keys
     */
    public Set<String> getAllWorkItemKeys() {
        return new HashSet<>(workItems.keySet());
    }
    
    /**
     * Returns a work item ID from the test context.
     *
     * @param key the key for the work item ID
     * @return the work item ID
     */
    public UUID getWorkItemId(String key) {
        return workItemIds.get(key);
    }
    
    /**
     * Saves a create request in the test context.
     *
     * @param key the key to use for the create request
     * @param request the create request to save
     */
    public void saveCreateRequest(String key, WorkItemCreateRequest request) {
        createRequests.put(key, request);
    }
    
    /**
     * Returns a create request from the test context.
     *
     * @param key the key for the create request
     * @return the create request
     */
    public WorkItemCreateRequest getCreateRequest(String key) {
        return createRequests.get(key);
    }
    
    /**
     * Saves a release in the test context.
     *
     * @param key the key to use for the release
     * @param release the release to save
     */
    public void saveRelease(String key, Release release) {
        releases.put(key, release);
    }
    
    /**
     * Returns a release from the test context.
     *
     * @param key the key for the release
     * @return the release
     */
    public Release getRelease(String key) {
        return releases.get(key);
    }
    
    /**
     * Sets the last exception encountered during a test.
     *
     * @param exception the exception
     */
    public void setException(Exception exception) {
        this.lastException = exception;
    }
    
    /**
     * Returns the last exception encountered during a test.
     *
     * @return the last exception
     */
    public Exception getException() {
        return lastException;
    }
    
    /**
     * Clears the last exception.
     */
    public void clearException() {
        this.lastException = null;
    }
    
    /**
     * Saves metadata for a work item.
     * 
     * @param workItemId the ID of the work item
     * @param key the metadata key
     * @param value the metadata value
     */
    public void saveWorkItemMetadata(UUID workItemId, String key, String value) {
        Map<String, String> metadata = workItemMetadata.computeIfAbsent(workItemId, k -> new HashMap<>());
        metadata.put(key, value);
    }
    
    /**
     * Retrieves metadata for a work item.
     * 
     * @param workItemId the ID of the work item
     * @param key the metadata key
     * @return the metadata value as an Optional
     */
    public Optional<String> getWorkItemMetadata(UUID workItemId, String key) {
        Map<String, String> metadata = workItemMetadata.get(workItemId);
        if (metadata == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(metadata.get(key));
    }
    
    /**
     * Sets a configuration flag.
     * 
     * @param flag the flag to set
     * @param value true to enable, false to disable
     */
    public void setConfigurationFlag(String flag, boolean value) {
        if (value) {
            configurationFlags.add(flag);
        } else {
            configurationFlags.remove(flag);
        }
    }
    
    /**
     * Checks if a configuration flag is set.
     * 
     * @param flag the flag to check
     * @return true if the flag is set, false otherwise
     */
    public boolean getConfigurationFlag(String flag) {
        return configurationFlags.contains(flag);
    }
    
    /**
     * Sets a configuration value.
     * 
     * @param key the configuration key
     * @param value the configuration value
     */
    public void setConfigurationValue(String key, Object value) {
        configurationValues.put(key, value);
    }
    
    /**
     * Retrieves a configuration value.
     * 
     * @param key the configuration key
     * @param <T> the type of the value
     * @return the configuration value as an Optional
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getConfigurationValue(String key) {
        return Optional.ofNullable((T) configurationValues.get(key));
    }
}