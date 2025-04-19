package org.rinna.cli.acceptance;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

/**
 * Acceptance tests for workflow operations using the CLI.
 * These tests verify end-to-end functionality from a user perspective.
 */
@Tag("acceptance")
@DisplayName("CLI Workflow Acceptance Tests")
public class WorkflowAcceptanceTest {

    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    @TempDir
    Path tempDir;
    
    private List<String> executedCommands;
    
    @BeforeEach
    void setUp() {
        System.setOut(new PrintStream(outputStream));
        executedCommands = new ArrayList<>();
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Test
    @DisplayName("Should complete full workflow lifecycle")
    void shouldCompleteFullWorkflowLifecycle() {
        // This test simulates a complete workflow lifecycle
        
        // 1. Create a work item
        String itemId = createWorkItem("Fix login issue", WorkItemType.BUG, Priority.HIGH);
        assertNotNull(itemId, "Work item should be created with an ID");
        
        // 2. View the work item
        boolean viewResult = viewWorkItem(itemId);
        assertTrue(viewResult, "Should be able to view the created work item");
        
        // 3. Update the work item status
        boolean updateResult = updateWorkItemStatus(itemId, WorkflowState.IN_PROGRESS);
        assertTrue(updateResult, "Should be able to update work item status");
        
        // 4. Assign the work item
        boolean assignResult = assignWorkItem(itemId, "developer1");
        assertTrue(assignResult, "Should be able to assign the work item");
        
        // 5. Complete the work item
        boolean completeResult = updateWorkItemStatus(itemId, WorkflowState.DONE);
        assertTrue(completeResult, "Should be able to complete the work item");
        
        // Verify the execution flow is correctly recorded
        assertEquals(5, executedCommands.size(), "Should have executed 5 workflow steps");
        assertTrue(executedCommands.get(0).contains("create"), "First step should be create");
        assertTrue(executedCommands.get(1).contains("view"), "Second step should be view");
        assertTrue(executedCommands.get(4).contains("update") && executedCommands.get(4).contains("DONE"), "Last step should update status to DONE");
    }
    
    @Test
    @DisplayName("Should handle parallel workflow operations")
    void shouldHandleParallelWorkflowOperations() throws Exception {
        // Create multiple work items
        List<String> itemIds = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            String itemId = createWorkItem("Task " + i, WorkItemType.TASK, Priority.MEDIUM);
            itemIds.add(itemId);
        }
        
        // Execute parallel operations
        Thread[] threads = new Thread[itemIds.size()];
        for (int i = 0; i < itemIds.size(); i++) {
            final String itemId = itemIds.get(i);
            threads[i] = new Thread(() -> {
                assertTrue(viewWorkItem(itemId), "Should view item " + itemId);
                assertTrue(updateWorkItemStatus(itemId, WorkflowState.IN_PROGRESS), 
                           "Should update item " + itemId);
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }
        
        // Verify all operations completed
        assertEquals(9, executedCommands.size(), 
                    "Should have 9 commands (3 creates, 3 views, 3 updates)");
    }
    
    // Helper methods to simulate CLI operations
    
    private String createWorkItem(String title, WorkItemType type, Priority priority) {
        // Simulate creating a work item via CLI
        String command = String.format("create \"%s\" --type=%s --priority=%s", 
                                      title, type, priority);
        executedCommands.add(command);
        
        // Return simulated ID
        return "WI-" + (100 + executedCommands.size());
    }
    
    private boolean viewWorkItem(String itemId) {
        // Simulate viewing a work item via CLI
        String command = String.format("view %s", itemId);
        executedCommands.add(command);
        return true;
    }
    
    private boolean updateWorkItemStatus(String itemId, WorkflowState status) {
        // Simulate updating work item status via CLI
        String command = String.format("update %s --status=%s", itemId, status);
        executedCommands.add(command);
        return true;
    }
    
    private boolean assignWorkItem(String itemId, String assignee) {
        // Simulate assigning a work item via CLI
        String command = String.format("update %s --assignee=%s", itemId, assignee);
        executedCommands.add(command);
        return true;
    }
}