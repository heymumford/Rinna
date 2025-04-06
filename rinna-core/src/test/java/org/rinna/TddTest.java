/*
 * Base test class for TDD development in the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.ReleaseService;
import org.rinna.domain.service.WorkflowService;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertTimeout;

/**
 * Base class for TDD-style development tests.
 * Provides common test lifecycle management and utilities.
 * Includes timing information for development.
 */
@ExtendWith(MockitoExtension.class)
public abstract class TddTest {
    
    protected final Logger logger = Logger.getLogger(getClass().getName());
    protected final TestHelper testHelper = new TestHelper();
    protected Rinna rinna;
    protected ItemService itemService;
    protected WorkflowService workflowService;
    protected ReleaseService releaseService;
    private long testStartTime;
    
    @BeforeEach
    void setUp(TestInfo testInfo) {
        testStartTime = System.currentTimeMillis();
        logger.info(() -> "Starting test: " + testInfo.getDisplayName());
        
        // Initialize services and repositories
        rinna = testHelper.initializeRinna();
        itemService = rinna.items();
        workflowService = rinna.workflow();
        releaseService = rinna.releases();
    }
    
    @AfterEach
    void tearDown(TestInfo testInfo) {
        testHelper.cleanup();
        
        // Log test duration
        long duration = System.currentTimeMillis() - testStartTime;
        logger.info(() -> String.format("Test completed in %d ms: %s", 
                duration, testInfo.getDisplayName()));
    }
    
    /**
     * Create a sample work item for testing.
     * @param title the title of the work item
     * @return the created work item
     */
    protected WorkItem createSampleWorkItem(String title) {
        return testHelper.createTestWorkItem(title);
    }
    
    /**
     * Create a sample bug for testing.
     * @param title the title of the bug
     * @return the created bug
     */
    protected WorkItem createSampleBug(String title) {
        return testHelper.createTestWorkItem(title, WorkItemType.BUG, Priority.HIGH, null);
    }
    
    /**
     * Helper to transition a work item to a specific state.
     * @param workItem the work item to transition
     * @param targetState the target state
     * @return the updated work item
     */
    protected WorkItem transitionToState(WorkItem workItem, WorkflowState targetState) {
        try {
            return testHelper.transitionTo(workItem, targetState);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error transitioning work item", e);
            throw new RuntimeException("Failed to transition work item to " + targetState, e);
        }
    }
    
    /**
     * Run an operation with a timeout, useful for performance-sensitive tests.
     * @param duration the maximum duration
     * @param operation the operation to run
     */
    protected void assertFastOperation(Duration duration, Runnable operation) {
        assertTimeout(duration, () -> operation.run());
    }
}