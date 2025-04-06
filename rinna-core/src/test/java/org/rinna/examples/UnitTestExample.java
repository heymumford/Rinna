package org.rinna.examples;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItemType;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Example of a unit test for Rinna.
 * Unit tests focus on testing a single unit in isolation with mocked dependencies.
 */
@Tag("unit")
public class UnitTestExample {

    @Test
    @DisplayName("Create work item with properly set fields")
    void createWorkItemShouldSetAllFields() {
        // Setup
        ItemService mockService = Mockito.mock(ItemService.class);
        WorkItemCreateRequest request = new WorkItemCreateRequest(
            "Test work item",
            "This is a test work item",
            Priority.MEDIUM,
            WorkItemType.FEATURE
        );
        
        WorkItem mockItem = Mockito.mock(WorkItem.class);
        Mockito.when(mockItem.getTitle()).thenReturn("Test work item");
        Mockito.when(mockItem.getDescription()).thenReturn("This is a test work item");
        Mockito.when(mockItem.getPriority()).thenReturn(Priority.MEDIUM);
        Mockito.when(mockItem.getType()).thenReturn(WorkItemType.FEATURE);
        
        Mockito.when(mockService.createWorkItem(request)).thenReturn(mockItem);
        
        // Execute
        WorkItem result = mockService.createWorkItem(request);
        
        // Verify
        assertEquals("Test work item", result.getTitle(), "Title should match request");
        assertEquals("This is a test work item", result.getDescription(), "Description should match request");
        assertEquals(Priority.MEDIUM, result.getPriority(), "Priority should match request");
        assertEquals(WorkItemType.FEATURE, result.getType(), "Type should match request");
        
        // Verify interactions
        Mockito.verify(mockService).createWorkItem(request);
    }

    @Test
    @DisplayName("Work item with invalid data should throw exception")
    void invalidWorkItemShouldThrowException() {
        // This is a pure unit test of data validation logic
        Exception exception = assertThrows(IllegalArgumentException.class, () -> {
            new WorkItemCreateRequest(
                "", // Empty title
                "Some description",
                Priority.LOW,
                WorkItemType.CHORE
            );
        });
        
        assertTrue(exception.getMessage().contains("title"), 
                  "Exception message should mention the invalid field");
    }
}
