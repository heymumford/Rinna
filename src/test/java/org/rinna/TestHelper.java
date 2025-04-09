/*
 * Helper utilities for testing the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna;

import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.ReleaseService;
import org.rinna.usecase.WorkflowService;
import org.rinna.utils.TestRinna;

import java.util.UUID;

/**
 * Helper methods and utilities for Rinna tests.
 */
public class TestHelper {

    /**
     * Creates a test work item with default values.
     * 
     * @return A work item create request with default test values
     */
    public static WorkItemCreateRequest createDefaultWorkItemRequest() {
        return new WorkItemCreateRequest.Builder()
                .title("Test Item")
                .description("This is a test item")
                .type(WorkItemType.TASK)
                .priority(Priority.MEDIUM)
                .assignee("tester")
                .build();
    }
    
    /**
     * Creates a test work item with custom values.
     * 
     * @param title The title of the work item
     * @param type The type of the work item
     * @param priority The priority of the work item
     * @return A work item create request with the specified values
     */
    public static WorkItemCreateRequest createCustomWorkItemRequest(
            String title, WorkItemType type, Priority priority) {
        return new WorkItemCreateRequest.Builder()
                .title(title)
                .description("Custom test item")
                .type(type)
                .priority(priority)
                .build();
    }
    
    /**
     * Creates a test release with default values.
     * 
     * @param name The name of the release
     * @return A release create request with the specified name
     */
    public static String createDefaultRelease(String name) {
        return "Release: " + name;
    }
}
