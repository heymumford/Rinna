/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.command;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class BulkCommandTest {

    private BulkCommand command;
    
    @Mock
    private ItemService itemService;
    
    @Mock
    private WorkflowService workflowService;
    
    @Mock
    private SearchService searchService;
    
    @Mock
    private MetadataService metadataService;
    
    @Mock
    private ConfigurationService configService;
    
    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private ContextManager contextManager;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> metadataParamsCaptor;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> metadataResultCaptor;
    
    // Test data
    private List<WorkItem> testItems;
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;

    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Setup test data
        WorkItem item1 = new WorkItem();
        item1.setId("WI-123");
        item1.setTitle("Fix authentication bug");
        item1.setState(WorkflowState.TO_DO);
        item1.setPriority(Priority.HIGH);
        item1.setType(WorkItemType.BUG);
        item1.setAssignee("developer1");
        item1.setCreated(LocalDateTime.now().minusDays(3));
        
        WorkItem item2 = new WorkItem();
        item2.setId("WI-124");
        item2.setTitle("Add new feature");
        item2.setState(WorkflowState.TO_DO);
        item2.setPriority(Priority.MEDIUM);
        item2.setType(WorkItemType.FEATURE);
        item2.setAssignee("developer2");
        item2.setCreated(LocalDateTime.now().minusDays(2));
        
        WorkItem item3 = new WorkItem();
        item3.setId("WI-125");
        item3.setTitle("Improve documentation");
        item3.setState(WorkflowState.IN_PROGRESS);
        item3.setPriority(Priority.LOW);
        item3.setType(WorkItemType.TASK);
        item3.setAssignee("developer3");
        item3.setCreated(LocalDateTime.now().minusDays(1));
        
        testItems = Arrays.asList(item1, item2, item3);
        
        // Setup mocks
        when(serviceManager.getItemService()).thenReturn(itemService);
        when(serviceManager.getWorkflowService()).thenReturn(workflowService);
        when(serviceManager.getSearchService()).thenReturn(searchService);
        when(serviceManager.getMetadataService()).thenReturn(metadataService);
        when(serviceManager.getConfigurationService()).thenReturn(configService);
        
        when(configService.getCurrentUser()).thenReturn("testUser");
        
        when(ContextManager.getInstance()).thenReturn(contextManager);
        
        // Setup operation tracking mock responses
        when(metadataService.startOperation(anyString(), anyString(), any())).thenReturn("op-123");
        
        // Create command with mock service manager for testing
        command = new BulkCommand(serviceManager);
    }
    
    @Test
    public void testCallWithNoFilters() {
        // Call without setting any filters
        Integer result = command.call();
        
        // Verify the operation was tracked and failed
        verify(metadataService).startOperation(eq("bulk-command"), eq("UPDATE"), any());
        verify(metadataService).failOperation(anyString(), any(IllegalArgumentException.class));
        
        // Verify the result indicates an error
        assertEquals(1, result);
        
        // Check that the error message is displayed
        String output = errContent.toString();
        assertTrue(output.contains("Error: No filters specified"));
    }
    
    @Test
    public void testCallWithNoUpdates() {
        // Set filter but no updates
        command.setFilter("status", "TO_DO");
        
        // Call command
        Integer result = command.call();
        
        // Verify the operation was tracked and failed
        verify(metadataService).startOperation(eq("bulk-command"), eq("UPDATE"), any());
        verify(metadataService).failOperation(anyString(), any(IllegalArgumentException.class));
        
        // Verify the result indicates an error
        assertEquals(1, result);
        
        // Check that the error message is displayed
        String output = errContent.toString();
        assertTrue(output.contains("Error: No updates specified"));
    }
    
    @Test
    public void testStatusFilterAndPriorityUpdate() {
        // Setup test data and mocks
        when(workflowService.findByStatus(WorkflowState.TO_DO)).thenReturn(Arrays.asList(testItems.get(0), testItems.get(1)));
        
        // Set filter and update
        command.setFilter("status", "TO_DO");
        command.setUpdate("set-priority", "HIGH");
        
        // Set operation IDs for different stages
        when(metadataService.startOperation(eq("bulk-command"), eq("UPDATE"), any())).thenReturn("op-main");
        when(metadataService.startOperation(eq("bulk-filter"), eq("SEARCH"), any())).thenReturn("op-filter");
        when(metadataService.startOperation(eq("bulk-filter-method"), eq("SEARCH"), any())).thenReturn("op-filter-method");
        when(metadataService.startOperation(eq("bulk-primary-filter"), eq("SEARCH"), any())).thenReturn("op-primary-filter");
        when(metadataService.startOperation(eq("bulk-update-apply"), eq("UPDATE"), any())).thenReturn("op-update");
        when(metadataService.startOperation(eq("bulk-apply-updates-method"), eq("UPDATE"), any())).thenReturn("op-apply-updates");
        when(metadataService.startOperation(eq("bulk-result-display"), eq("READ"), any())).thenReturn("op-display");
        
        // Call command
        Integer result = command.call();
        
        // Verify successful execution
        assertEquals(0, result);
        
        // Verify operation tracking
        verify(metadataService).startOperation(eq("bulk-command"), eq("UPDATE"), metadataParamsCaptor.capture());
        Map<String, Object> mainParams = metadataParamsCaptor.getValue();
        assertEquals("testUser", mainParams.get("username"));
        assertEquals(1, mainParams.get("filterCount"));
        
        // Verify filter operation was started and completed
        verify(metadataService).startOperation(eq("bulk-filter"), eq("SEARCH"), any());
        verify(metadataService).completeOperation(eq("op-filter"), any());
        
        // Verify update operations
        verify(metadataService).startOperation(eq("bulk-update-apply"), eq("UPDATE"), any());
        verify(metadataService).startOperation(eq("bulk-apply-updates-method"), eq("UPDATE"), any());
        
        // Verify item service interactions for priority updates
        verify(itemService, times(2)).updatePriority(any(), eq(Priority.HIGH), eq("testUser"));
        
        // Verify main operation completion
        verify(metadataService).completeOperation(eq("op-main"), metadataResultCaptor.capture());
        Map<String, Object> results = metadataResultCaptor.getValue();
        assertTrue((Boolean) results.get("success"));
        assertEquals(2, results.get("itemCount"));
        
        // Check output contains success message
        String output = outContent.toString();
        assertTrue(output.contains("Successfully updated"));
    }
    
    @Test
    public void testMultipleUpdatesWithJsonFormat() {
        // Setup test data and mocks
        when(itemService.findByAssignee("developer1")).thenReturn(Arrays.asList(testItems.get(0)));
        
        // Set filter and multiple updates
        command.setFilter("assignee", "developer1");
        command.setUpdate("set-status", "IN_PROGRESS");
        command.setUpdate("set-priority", "MEDIUM");
        command.setFormat("json");
        
        // Set operation tracking
        when(metadataService.startOperation(anyString(), anyString(), any())).thenReturn("op-123");
        
        // Call command
        Integer result = command.call();
        
        // Verify successful execution
        assertEquals(0, result);
        
        // Verify operations were tracked
        verify(metadataService, atLeastOnce()).startOperation(anyString(), anyString(), any());
        verify(metadataService, atLeastOnce()).completeOperation(anyString(), any());
        
        // Verify service interactions
        verify(workflowService).transition(eq("WI-123"), eq("testUser"), eq(WorkflowState.IN_PROGRESS), anyString());
        verify(itemService).updatePriority(any(), eq(Priority.MEDIUM), eq("testUser"));
        
        // Check output contains JSON
        String output = outContent.toString();
        assertTrue(output.contains("\"result\":"));
        assertTrue(output.contains("\"success\""));
    }
    
    @Test
    public void testCustomFieldsUpdate() {
        // Setup test data and mocks
        when(searchService.searchByText("authentication")).thenReturn(Arrays.asList(testItems.get(0)));
        
        // Set filter and custom field update
        command.setFilter("text", "authentication");
        command.setUpdate("field-component", "auth-service");
        command.setUpdate("field-estimated-hours", "4");
        
        // Call command
        Integer result = command.call();
        
        // Verify successful execution
        assertEquals(0, result);
        
        // Verify operation tracking
        verify(metadataService, atLeastOnce()).startOperation(eq("bulk-update-item-custom-fields"), eq("UPDATE"), any());
        
        // Verify custom fields update
        ArgumentCaptor<Map<String, String>> fieldsCaptor = ArgumentCaptor.forClass(Map.class);
        verify(itemService).updateCustomFields(eq("WI-123"), fieldsCaptor.capture());
        
        Map<String, String> customFields = fieldsCaptor.getValue();
        assertEquals(2, customFields.size());
        assertEquals("auth-service", customFields.get("component"));
        assertEquals("4", customFields.get("estimated-hours"));
    }
    
    @Test
    public void testInvalidStatusUpdate() {
        // Setup mocks
        when(workflowService.findByStatus(WorkflowState.TO_DO)).thenReturn(Arrays.asList(testItems.get(0), testItems.get(1)));
        
        // Set filter and invalid update
        command.setFilter("status", "TO_DO");
        command.setUpdate("set-status", "INVALID_STATUS");
        
        // Call command
        Integer result = command.call();
        
        // Verify error handling
        assertEquals(1, result);
        
        // Verify error operation tracking
        verify(metadataService).failOperation(anyString(), any(IllegalArgumentException.class));
        
        // Check error message
        String output = errContent.toString();
        assertTrue(output.contains("Error: Invalid status: INVALID_STATUS"));
    }
    
    @Test
    public void testNoMatchingItems() {
        // Setup mocks to return empty list
        when(searchService.searchByText("nonexistent")).thenReturn(List.of());
        
        // Set filter and update
        command.setFilter("text", "nonexistent");
        command.setUpdate("set-priority", "HIGH");
        
        // Call command
        Integer result = command.call();
        
        // Verify successful execution but with warning
        assertEquals(0, result);
        
        // Verify warning operation tracking
        verify(metadataService).startOperation(eq("bulk-warning-display"), eq("READ"), any());
        
        // Check warning message
        String output = outContent.toString();
        assertTrue(output.contains("No tasks found matching the filter criteria"));
    }
    
    @Test
    public void testHierarchicalOperationTracking() {
        // Setup mocks
        when(itemService.findByType(WorkItemType.BUG)).thenReturn(Arrays.asList(testItems.get(0)));
        
        // Set filter and update
        command.setFilter("type", "BUG");
        command.setUpdate("set-status", "IN_PROGRESS");
        
        // Call command
        Integer result = command.call();
        
        // Verify successful execution
        assertEquals(0, result);
        
        // Count the number of operations started
        verify(metadataService, atLeast(8)).startOperation(anyString(), anyString(), any());
        
        // Verify parent-child relationship through operation tracking
        // Main operation
        verify(metadataService).startOperation(eq("bulk-command"), eq("UPDATE"), any());
        
        // Filter operations
        verify(metadataService).startOperation(eq("bulk-filter"), eq("SEARCH"), any());
        verify(metadataService).startOperation(eq("bulk-filter-method"), eq("SEARCH"), any());
        verify(metadataService).startOperation(eq("bulk-primary-filter"), eq("SEARCH"), any());
        
        // Update operations
        verify(metadataService).startOperation(eq("bulk-update-apply"), eq("UPDATE"), any());
        verify(metadataService).startOperation(eq("bulk-apply-updates-method"), eq("UPDATE"), any());
        
        // Item-specific operations
        verify(metadataService).startOperation(eq("bulk-update-item"), eq("UPDATE"), any());
        verify(metadataService).startOperation(eq("bulk-update-item-status"), eq("UPDATE"), any());
        
        // Display operation
        verify(metadataService).startOperation(eq("bulk-result-display"), eq("READ"), any());
    }
    
    @Test
    public void testMethodChaining() {
        // Test method chaining API
        BulkCommand chainedCommand = command
            .setFilter("status", "TODO")
            .setUpdate("set-priority", "HIGH")
            .setFormat("json")
            .setVerbose(true);
        
        // Verify it's the same instance
        assertSame(command, chainedCommand);
    }
}