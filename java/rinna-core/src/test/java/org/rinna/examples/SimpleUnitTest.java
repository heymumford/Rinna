package org.rinna.examples;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.rinna.base.UnitTest;
import org.rinna.domain.model.WorkItemType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Example of a unit test using the new test framework.
 * This test demonstrates the principles of unit testing in Rinna.
 */
@DisplayName("Work Item Type Tests")
public class SimpleUnitTest extends UnitTest {
    
    @Test
    @DisplayName("Work item types should have correct names")
    void workItemTypesShouldHaveCorrectNames() {
        // Test that work item types have the expected names
        assertEquals("TASK", WorkItemType.TASK.name());
        assertEquals("BUG", WorkItemType.BUG.name());
        assertEquals("FEATURE", WorkItemType.FEATURE.name());
        assertEquals("EPIC", WorkItemType.EPIC.name());
    }
    
    @Test
    @DisplayName("Work item types should have correct display names")
    void workItemTypesShouldHaveCorrectDisplayNames() {
        // Test that work item types have the expected display names
        assertEquals("Task", WorkItemType.TASK.getDisplayName());
        assertEquals("Bug", WorkItemType.BUG.getDisplayName());
        assertEquals("Feature", WorkItemType.FEATURE.getDisplayName());
        assertEquals("Epic", WorkItemType.EPIC.getDisplayName());
    }
    
    @Test
    @DisplayName("valueOf() should return correct WorkItemType")
    void valueOfShouldReturnCorrectWorkItemType() {
        // Test that valueOf returns the correct work item type
        assertEquals(WorkItemType.TASK, WorkItemType.valueOf("TASK"));
        assertEquals(WorkItemType.BUG, WorkItemType.valueOf("BUG"));
        assertEquals(WorkItemType.FEATURE, WorkItemType.valueOf("FEATURE"));
        assertEquals(WorkItemType.EPIC, WorkItemType.valueOf("EPIC"));
    }
    
    @Test
    @DisplayName("getByName() should find WorkItemType by display name")
    void getByNameShouldFindWorkItemTypeByDisplayName() {
        // Test that getByName() finds the correct work item type
        assertEquals(WorkItemType.TASK, WorkItemType.getByName("Task"));
        assertEquals(WorkItemType.BUG, WorkItemType.getByName("Bug"));
        assertEquals(WorkItemType.FEATURE, WorkItemType.getByName("Feature"));
        assertEquals(WorkItemType.EPIC, WorkItemType.getByName("Epic"));
    }
}