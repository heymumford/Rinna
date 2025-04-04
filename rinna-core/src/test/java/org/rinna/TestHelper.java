/*
 * Test utility for setting up and tearing down tests for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.rinna.domain.entity.*;
import org.rinna.domain.usecase.ItemService;
import org.rinna.domain.usecase.ReleaseService;
import org.rinna.domain.usecase.WorkflowService;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Test helper utility for setting up common test scenarios and providing clean teardown.
 * Can be used as a JUnit Jupiter extension or directly in test classes.
 */
public class TestHelper implements BeforeEachCallback, AfterEachCallback {
    
    private final List<UUID> createdWorkItemIds = new ArrayList<>();
    private final List<UUID> createdReleaseIds = new ArrayList<>();
    private Rinna rinna;
    
    /**
     * Initialize a fresh Rinna instance for testing.
     * @return a new Rinna instance
     */
    public Rinna initializeRinna() {
        this.rinna = Rinna.initialize();
        return this.rinna;
    }
    
    /**
     * Get the current Rinna instance, initializing if necessary.
     * @return the current Rinna instance
     */
    public Rinna getRinna() {
        if (this.rinna == null) {
            return initializeRinna();
        }
        return this.rinna;
    }
    
    /**
     * Create a test work item with the given title.
     * @param title the work item title
     * @return the created work item
     */
    public WorkItem createTestWorkItem(String title) {
        return createTestWorkItem(title, WorkItemType.FEATURE, Priority.MEDIUM, null);
    }
    
    /**
     * Create a test work item with the given title and type.
     * @param title the work item title
     * @param type the work item type
     * @return the created work item
     */
    public WorkItem createTestWorkItem(String title, WorkItemType type) {
        return createTestWorkItem(title, type, Priority.MEDIUM, null);
    }
    
    /**
     * Create a test work item with the given parameters.
     * @param title the work item title
     * @param type the work item type
     * @param priority the work item priority
     * @param assignee the assignee (can be null)
     * @return the created work item
     */
    public WorkItem createTestWorkItem(String title, WorkItemType type, Priority priority, String assignee) {
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title(title)
                .type(type)
                .priority(priority)
                .assignee(assignee)
                .build();
        
        WorkItem workItem = getRinna().items().create(request);
        createdWorkItemIds.add(workItem.getId());
        return workItem;
    }
    
    /**
     * Create a test release with the given version.
     * @param version the release version
     * @return the created release
     */
    public Release createTestRelease(String version) {
        Release release = getRinna().releases().createRelease(version);
        createdReleaseIds.add(release.getId());
        return release;
    }
    
    /**
     * Clean up all created test items.
     */
    public void cleanup() {
        if (rinna != null) {
            ItemService itemService = rinna.items();
            ReleaseService releaseService = rinna.releases();
            
            // Clean up work items
            for (UUID id : createdWorkItemIds) {
                Optional<WorkItem> workItem = itemService.findById(id);
                if (workItem.isPresent()) {
                    // In a real application, we would delete the item
                    // Since we're using in-memory repositories, we can just leave it
                }
            }
            
            // Clean up releases
            for (UUID id : createdReleaseIds) {
                // In a real application, we would delete the release
                // Since we're using in-memory repositories, we can just leave it
            }
            
            createdWorkItemIds.clear();
            createdReleaseIds.clear();
        }
    }
    
    /**
     * Move a work item through the workflow to the desired state.
     * @param workItem the work item to transition
     * @param targetState the desired target state
     * @return the updated work item
     * @throws Exception if any transition fails
     */
    public WorkItem transitionTo(WorkItem workItem, WorkflowState targetState) throws Exception {
        UUID id = workItem.getId();
        WorkflowService workflowService = getRinna().workflow();
        WorkflowState currentState = workItem.getStatus();
        
        // If already in the target state, just return
        if (currentState == targetState) {
            return workItem;
        }
        
        // Define the valid state progression
        List<WorkflowState> stateProgression = List.of(
                WorkflowState.FOUND,
                WorkflowState.TRIAGED,
                WorkflowState.TO_DO,
                WorkflowState.IN_PROGRESS,
                WorkflowState.IN_TEST,
                WorkflowState.DONE
        );
        
        int currentIndex = stateProgression.indexOf(currentState);
        int targetIndex = stateProgression.indexOf(targetState);
        
        if (currentIndex < 0 || targetIndex < 0) {
            throw new IllegalArgumentException("Invalid workflow state");
        }
        
        // Can only move forward in the workflow for now
        if (targetIndex < currentIndex) {
            throw new IllegalArgumentException("Cannot move backwards in workflow");
        }
        
        // Step through each state transition
        WorkItem updatedItem = workItem;
        for (int i = currentIndex + 1; i <= targetIndex; i++) {
            WorkflowState nextState = stateProgression.get(i);
            updatedItem = workflowService.transition(id, nextState);
        }
        
        return updatedItem;
    }
    
    // JUnit Jupiter extension methods
    
    @Override
    public void beforeEach(ExtensionContext context) {
        initializeRinna();
    }
    
    @Override
    public void afterEach(ExtensionContext context) {
        cleanup();
    }
}