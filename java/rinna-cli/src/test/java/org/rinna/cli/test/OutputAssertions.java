/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.test;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

/**
 * Utility class for standardized output assertions in tests.
 * Provides utility methods for verifying command output formatting
 * and content in a consistent way across test classes.
 */
public class OutputAssertions {
    
    /**
     * Asserts that the output contains all the expected work item fields.
     * 
     * @param output the captured output to verify
     * @param item the work item to check against
     */
    public static void assertWorkItemOutput(String output, WorkItem item) {
        assertFalse(output.isEmpty(), "Output should not be empty");
        
        // Check item ID
        if (item.getId() != null) {
            assertTrue(output.contains(item.getId()), "Output should contain the work item ID");
        }
        
        // Check title
        if (item.getTitle() != null) {
            assertTrue(output.contains(item.getTitle()), "Output should contain the work item title");
        }
        
        // Check type
        if (item.getType() != null) {
            assertTrue(output.contains(item.getType().toString()), "Output should contain the work item type");
        }
        
        // Check priority
        if (item.getPriority() != null) {
            assertTrue(output.contains(item.getPriority().toString()), "Output should contain the work item priority");
        }
        
        // Check state
        if (item.getState() != null) {
            assertTrue(output.contains(item.getState().toString()) || 
                       output.contains(item.getStatus().toString()), 
                      "Output should contain the work item state/status");
        }
    }
    
    /**
     * Asserts that the output contains the expected JSON structure.
     * 
     * @param output the captured output to verify
     * @param expectedKeys array of keys expected to be in the JSON
     */
    public static void assertJsonOutput(String output, String... expectedKeys) {
        assertFalse(output.isEmpty(), "Output should not be empty");
        
        // Verify JSON format
        assertTrue(output.contains("{"), "Output should start with JSON opening brace");
        assertTrue(output.contains("}"), "Output should end with JSON closing brace");
        
        // Verify expected keys
        for (String key : expectedKeys) {
            assertTrue(output.contains("\"" + key + "\":") || output.contains("\"" + key + "\" :"), 
                      "Output should contain JSON key: " + key);
        }
    }
    
    /**
     * Asserts that the output contains a tabular format with the expected columns.
     * 
     * @param output the captured output to verify
     * @param expectedColumns array of column names expected in the table header
     * @param expectedRowCount expected number of data rows
     */
    public static void assertTableOutput(String output, String[] expectedColumns, int expectedRowCount) {
        assertFalse(output.isEmpty(), "Output should not be empty");
        
        // Verify header
        for (String column : expectedColumns) {
            assertTrue(output.contains(column.toUpperCase()), "Output should contain column header: " + column);
        }
        
        // Verify row count (approximately by counting newlines)
        // Header + separator + data rows + footer
        int expectedLineCount = 4 + expectedRowCount; // 1 for header, 2 for separators, 1 for footer
        int lineCount = output.split("\n").length;
        
        // Allow for slight variations (some output formats might have extra blank lines)
        assertTrue(Math.abs(lineCount - expectedLineCount) <= 2, 
                  "Output should have approximately " + expectedLineCount + " lines, found " + lineCount);
    }
    
    /**
     * Asserts that the output contains a message about empty results.
     * 
     * @param output the captured output to verify
     */
    public static void assertEmptyResultsMessage(String output) {
        assertFalse(output.isEmpty(), "Output should not be empty");
        assertTrue(output.contains("No") && 
                  (output.contains("found") || output.contains("available") || output.contains("exist")), 
                  "Output should indicate no results found");
    }
    
    /**
     * Asserts that the output contains a success confirmation message.
     * 
     * @param output the captured output to verify
     * @param actionType the type of action (e.g., "created", "updated", "deleted")
     */
    public static void assertSuccessMessage(String output, String actionType) {
        assertFalse(output.isEmpty(), "Output should not be empty");
        
        // Check for common success terminology
        boolean containsActionType = output.toLowerCase().contains(actionType.toLowerCase());
        boolean containsConfirmation = output.toLowerCase().contains("success") || 
                                      output.toLowerCase().contains("complete") || 
                                      output.toLowerCase().contains("done");
        
        assertTrue(containsActionType || containsConfirmation, 
                  "Output should confirm the action was successful");
    }
    
    /**
     * Asserts that the error output contains an error message of the expected type.
     * 
     * @param errorOutput the captured error output to verify
     * @param errorType the type of error to check for (e.g., "not found", "invalid", "required")
     */
    public static void assertErrorMessage(String errorOutput, String errorType) {
        assertFalse(errorOutput.isEmpty(), "Error output should not be empty");
        assertTrue(errorOutput.toLowerCase().contains(errorType.toLowerCase()), 
                  "Error output should contain error type: " + errorType);
    }
    
    /**
     * Asserts that the output contains all items in a work item list.
     * 
     * @param output the captured output to verify
     * @param items the list of work items to check against
     */
    public static void assertWorkItemListOutput(String output, List<WorkItem> items) {
        assertFalse(output.isEmpty(), "Output should not be empty");
        
        // Verify each item's title is present
        for (WorkItem item : items) {
            if (item.getTitle() != null && !item.getTitle().isEmpty()) {
                assertTrue(output.contains(item.getTitle()) || 
                          (item.getTitle().length() > 35 && output.contains(item.getTitle().substring(0, 32))), 
                          "Output should contain title of item: " + item.getTitle());
            }
        }
    }
    
    /**
     * Asserts that the output contains a state transition message.
     * 
     * @param output the captured output to verify
     * @param itemId the ID of the work item
     * @param targetState the target state of the transition
     */
    public static void assertStateTransitionMessage(String output, String itemId, WorkflowState targetState) {
        assertFalse(output.isEmpty(), "Output should not be empty");
        
        // Check for item ID
        assertTrue(output.contains(itemId), "Output should contain the work item ID");
        
        // Check for target state
        assertTrue(output.contains(targetState.toString()), "Output should contain the target state");
        
        // Check for transition terminology
        boolean containsTransitionTerm = output.toLowerCase().contains("transition") || 
                                        output.toLowerCase().contains("updated") || 
                                        output.toLowerCase().contains("changed") || 
                                        output.toLowerCase().contains("moved");
        
        assertTrue(containsTransitionTerm, "Output should confirm state transition");
    }
    
    /**
     * Asserts that the output contains a work item creation confirmation.
     * 
     * @param output the captured output to verify
     * @param itemType the type of work item created
     * @param title the title of the created work item
     */
    public static void assertWorkItemCreationMessage(String output, WorkItemType itemType, String title) {
        assertFalse(output.isEmpty(), "Output should not be empty");
        
        // Check for item type
        assertTrue(output.contains(itemType.toString()), "Output should contain the work item type");
        
        // Check for title
        assertTrue(output.contains(title) || 
                  (title.length() > 35 && output.contains(title.substring(0, 32))), 
                  "Output should contain the work item title");
        
        // Check for creation terminology
        boolean containsCreationTerm = output.toLowerCase().contains("created") || 
                                      output.toLowerCase().contains("added") || 
                                      output.toLowerCase().contains("new");
        
        assertTrue(containsCreationTerm, "Output should confirm work item creation");
    }
}