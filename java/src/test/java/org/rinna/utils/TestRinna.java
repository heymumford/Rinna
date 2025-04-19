package org.rinna.utils;

import org.rinna.usecase.ItemService;
import org.rinna.usecase.QueueService;
import org.rinna.usecase.ReleaseService;
import org.rinna.usecase.WorkflowService;

/**
 * Test utility class that provides access to services for testing.
 * This class is used in tests that were written for the old package structure.
 */
public class TestRinna {

    private static TestRinna instance;
    
    private final ItemService itemService;
    private final WorkflowService workflowService;
    private final ReleaseService releaseService;
    private final QueueService queueService;
    
    private TestRinna() {
        this.itemService = null; // For now just null, replace with mock implementations if needed
        this.workflowService = null;
        this.releaseService = null;
        this.queueService = null;
    }
    
    public static TestRinna initialize() {
        if (instance == null) {
            instance = new TestRinna();
        }
        return instance;
    }
    
    public ItemService items() {
        return itemService;
    }
    
    public WorkflowService workflow() {
        return workflowService;
    }
    
    public ReleaseService releases() {
        return releaseService;
    }
    
    public QueueService queues() {
        return queueService;
    }
}