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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
}