/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.command.BulkCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

/**
 * Component test for BulkCommand. This test verifies the interaction between
 * BulkCommand and various services, as well as the hierarchical operation tracking
 * functionality.
 */
@ExtendWith(MockitoExtension.class)
public class BulkCommandComponentTest {

    // Real component under test
    private BulkCommand command;
    
    // Mock dependencies
    @Mock
    private ServiceManager serviceManager;
    
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
    private ContextManager contextManager;
    
    // Test data
    private List<WorkItem> testItems;
    
    // Capture arguments for verification
    @Captor
    private ArgumentCaptor<Map<String, Object>> metadataParamsCaptor;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> metadataResultCaptor;
    
    @Captor
    private ArgumentCaptor<Map<String, String>> customFieldsCaptor;
    
    // For capturing console output
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    @BeforeEach
    public void setUp() {
        // Redirect System.out and System.err for testing
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Create test data
        createTestData();
        
        // Configure mock services
        when(serviceManager.getItemService()).thenReturn(itemService);
        when(serviceManager.getWorkflowService()).thenReturn(workflowService);
        when(serviceManager.getSearchService()).thenReturn(searchService);
        when(serviceManager.getMetadataService()).thenReturn(metadataService);
        when(serviceManager.getConfigurationService()).thenReturn(configService);
        
        when(configService.getCurrentUser()).thenReturn("testuser");
        
        // Configure ContextManager mock
        when(ContextManager.getInstance()).thenReturn(contextManager);
        
        // Configure operation tracking
        setupOperationTracking();
        
        // Initialize the command with mock services
        command = new BulkCommand(serviceManager);
    }
    
    private void createTestData() {
        testItems = new ArrayList<>();
        
        // Create test WorkItems
        for (int i = 1; i <= 5; i++) {
            WorkItem item = new WorkItem();
            item.setId("WI-" + (100 + i));
            item.setTitle("Test Work Item " + i);
            item.setDescription("Description for test item " + i);
            
            // Alternate between states
            if (i % 3 == 0) {
                item.setState(WorkflowState.IN_PROGRESS);
            } else if (i % 3 == 1) {
                item.setState(WorkflowState.CREATED);
            } else {
                item.setState(WorkflowState.READY);
            }
            
            // Alternate between priorities
            if (i % 3 == 0) {
                item.setPriority(Priority.HIGH);
            } else if (i % 3 == 1) {
                item.setPriority(Priority.MEDIUM);
            } else {
                item.setPriority(Priority.LOW);
            }
            
            // Alternate between types
            if (i % 3 == 0) {
                item.setType(WorkItemType.BUG);
            } else if (i % 3 == 1) {
                item.setType(WorkItemType.FEATURE);
            } else {
                item.setType(WorkItemType.TASK);
            }
            
            // Assign to different users
            item.setAssignee("user" + (i % 3 + 1));
            
            // Set creation date
            item.setCreated(LocalDateTime.now().minusDays(i));
            
            testItems.add(item);
        }
    }
    
    private void setupOperationTracking() {
        // Generate predictable operation IDs for testing
        when(metadataService.startOperation(eq("bulk-command"), eq("UPDATE"), any()))
            .thenReturn("op-main");
        
        when(metadataService.startOperation(eq("bulk-filter"), eq("SEARCH"), any()))
            .thenReturn("op-filter");
            
        when(metadataService.startOperation(eq("bulk-filter-method"), eq("SEARCH"), any()))
            .thenReturn("op-filter-method");
            
        when(metadataService.startOperation(eq("bulk-primary-filter"), eq("SEARCH"), any()))
            .thenReturn("op-primary-filter");
            
        when(metadataService.startOperation(eq("bulk-secondary-filter"), eq("FILTER"), any()))
            .thenReturn("op-secondary-filter");
            
        when(metadataService.startOperation(startsWith("bulk-filter-"), eq("FILTER"), any()))
            .thenReturn("op-specific-filter");
            
        when(metadataService.startOperation(eq("bulk-update-apply"), eq("UPDATE"), any()))
            .thenReturn("op-update");
            
        when(metadataService.startOperation(eq("bulk-apply-updates-method"), eq("UPDATE"), any()))
            .thenReturn("op-apply-updates");
            
        when(metadataService.startOperation(eq("bulk-update-item"), eq("UPDATE"), any()))
            .thenReturn("op-update-item");
            
        when(metadataService.startOperation(startsWith("bulk-update-item-"), eq("UPDATE"), any()))
            .thenReturn("op-update-item-field");
            
        when(metadataService.startOperation(startsWith("bulk-update-type-"), eq("UPDATE"), any()))
            .thenReturn("op-update-type");
            
        when(metadataService.startOperation(eq("bulk-result-display"), eq("READ"), any()))
            .thenReturn("op-display");
            
        when(metadataService.startOperation(eq("bulk-warning-display"), eq("READ"), any()))
            .thenReturn("op-warning");
    }
    
    @Test
    public void testComplexFilteringScenario() {
        // Setup mocks for the service responses
        when(searchService.searchByText("Test")).thenReturn(testItems);
        
        // Configure command
        command.setFilter("text", "Test")  // Primary filter
               .setFilter("status", "CREATED") // Secondary filter
               .setFilter("assignee", "user1") // Tertiary filter
               .setUpdate("set-priority", "HIGH")
               .setUpdate("set-status", "IN_PROGRESS");
        
        // Execute command
        Integer result = command.call();
        
        // Verify the command was successful
        assertEquals(0, result);
        
        // Verify hierarchical operation tracking
        verify(metadataService).startOperation(eq("bulk-command"), eq("UPDATE"), metadataParamsCaptor.capture());
        Map<String, Object> mainParams = metadataParamsCaptor.getValue();
        assertEquals(3, mainParams.get("filterCount"));
        assertEquals(2, mainParams.get("updateCount"));
        assertTrue(mainParams.containsKey("filter.text"));
        assertTrue(mainParams.containsKey("filter.status"));
        assertTrue(mainParams.containsKey("filter.assignee"));
        
        // Verify filter operations
        verify(metadataService).startOperation(eq("bulk-filter"), eq("SEARCH"), any());
        verify(metadataService).startOperation(eq("bulk-filter-method"), eq("SEARCH"), any());
        verify(metadataService).startOperation(eq("bulk-primary-filter"), eq("SEARCH"), any());
        verify(metadataService).startOperation(eq("bulk-secondary-filter"), eq("FILTER"), any());
        verify(metadataService, atLeastOnce()).startOperation(startsWith("bulk-filter-"), eq("FILTER"), any());
        
        // Verify update operations
        verify(metadataService).startOperation(eq("bulk-update-apply"), eq("UPDATE"), any());
        verify(metadataService).startOperation(eq("bulk-apply-updates-method"), eq("UPDATE"), any());
        verify(metadataService, atLeastOnce()).startOperation(eq("bulk-update-item"), eq("UPDATE"), any());
        verify(metadataService, atLeastOnce()).startOperation(startsWith("bulk-update-item-"), eq("UPDATE"), any());
        verify(metadataService, times(2)).startOperation(startsWith("bulk-update-type-"), eq("UPDATE"), any());
        
        // Verify result operations
        verify(metadataService).startOperation(eq("bulk-result-display"), eq("READ"), any());
        
        // Verify operation completion
        verify(metadataService, atLeastOnce()).completeOperation(anyString(), any());
        verify(metadataService).completeOperation(eq("op-main"), any());
        
        // Verify service interactions
        verify(searchService).searchByText("Test");
        // Use explicit UUID type for the first parameter to avoid ambiguity
        verify(workflowService, atLeastOnce()).transition(any(String.class), eq("testuser"), eq(WorkflowState.IN_PROGRESS), anyString());
        verify(itemService, atLeastOnce()).updatePriority(any(), eq(Priority.HIGH), eq("testuser"));
        
        // Verify output
        String output = outContent.toString();
        assertTrue(output.contains("Successfully updated"));
    }
    
    @Test
    public void testCustomFieldsWithJsonOutput() {
        // Setup mocks
        when(workflowService.findByStatus(WorkflowState.CREATED)).thenReturn(
            testItems.stream().filter(i -> i.getState() == WorkflowState.CREATED).toList());
        
        // Configure command
        command.setFilter("status", "CREATED")
               .setUpdate("field-component", "ui")
               .setUpdate("field-effort", "3")
               .setUpdate("field-reviewer", "seniordev")
               .setFormat("json");
        
        // Execute command
        Integer result = command.call();
        
        // Verify the command was successful
        assertEquals(0, result);
        
        // Verify custom fields parameters
        verify(metadataService).startOperation(eq("bulk-command"), eq("UPDATE"), metadataParamsCaptor.capture());
        Map<String, Object> params = metadataParamsCaptor.getValue();
        assertTrue(params.containsKey("update.field-component"));
        assertTrue(params.containsKey("update.field-effort"));
        assertTrue(params.containsKey("update.field-reviewer"));
        
        // Verify custom fields operations
        verify(metadataService, atLeastOnce()).startOperation(eq("bulk-update-item-custom-fields"), eq("UPDATE"), any());
        
        // Verify updates to item service
        verify(itemService, atLeastOnce()).updateCustomFields(anyString(), customFieldsCaptor.capture());
        Map<String, String> fields = customFieldsCaptor.getValue();
        assertEquals(3, fields.size());
        assertEquals("ui", fields.get("component"));
        assertEquals("3", fields.get("effort"));
        assertEquals("seniordev", fields.get("reviewer"));
        
        // Verify JSON output format
        String output = outContent.toString();
        assertTrue(output.contains("\"result\":"));
        assertTrue(output.contains("\"fields\":"));
    }
    
    @Test
    public void testErrorHandlingAndOperationFailure() {
        // Setup mocks
        when(workflowService.findByStatus(WorkflowState.CREATED)).thenReturn(
            testItems.stream().filter(i -> i.getState() == WorkflowState.CREATED).toList());
        
        // Configure command with invalid status
        command.setFilter("status", "TODO")
               .setUpdate("set-status", "INVALID_STATUS");
        
        // Execute command
        Integer result = command.call();
        
        // Verify the command failed
        assertEquals(1, result);
        
        // Verify operation failures were tracked
        verify(metadataService, atLeastOnce()).failOperation(anyString(), any(Exception.class));
        
        // Check specifically for status update operation failure
        verify(metadataService).failOperation(eq("op-update-item-field"), any(IllegalArgumentException.class));
        
        // Verify error message
        String errOutput = errContent.toString();
        assertTrue(errOutput.contains("Invalid status"));
    }
    
    @Test
    public void testNoMatchingItemsWarning() {
        // Setup mock to return empty list
        when(workflowService.findByStatus(any())).thenReturn(Collections.emptyList());
        
        // Configure command
        command.setFilter("status", "DONE")
               .setUpdate("set-priority", "LOW");
        
        // Execute command
        Integer result = command.call();
        
        // Verify command completes successfully but with warning
        assertEquals(0, result);
        
        // Verify warning display operation
        verify(metadataService).startOperation(eq("bulk-warning-display"), eq("READ"), any());
        
        // Verify no update operations were performed
        verify(itemService, never()).updatePriority(any(), any(), any());
        verify(workflowService, never()).transition(any(), any(), any(), any());
        
        // Verify warning output
        String output = outContent.toString();
        assertTrue(output.contains("No tasks found matching the filter criteria"));
    }
    
    @Test
    public void testVerboseOutputMode() {
        // Setup mocks
        when(itemService.findByType(WorkItemType.BUG)).thenReturn(
            testItems.stream().filter(i -> i.getType() == WorkItemType.BUG).toList());
        
        // Configure command with verbose mode
        command.setFilter("type", "BUG")
               .setUpdate("set-assignee", "bugfixer")
               .setVerbose(true);
        
        // Execute command
        Integer result = command.call();
        
        // Verify the command was successful
        assertEquals(0, result);
        
        // Verify verbose parameter was passed
        verify(metadataService).startOperation(eq("bulk-command"), eq("UPDATE"), metadataParamsCaptor.capture());
        Map<String, Object> params = metadataParamsCaptor.getValue();
        assertEquals(true, params.get("verbose"));
        
        // Verify detailed updates in output
        verify(itemService, atLeastOnce()).assignTo(any(), eq("bugfixer"), eq("testuser"));
        
        // Check output contains item details
        String output = outContent.toString();
        assertTrue(output.contains("Updated: WI-"));
    }
    
    @Test
    public void testMultipleUpdateTypes() {
        // Setup mocks
        Map<String, String> metadata = Map.of("project", "TestProject");
        when(searchService.findItemsByMetadata(eq(metadata))).thenReturn(testItems.subList(0, 2));
        
        // Configure command with multiple update types
        command.setFilter("project", "TestProject")
               .setUpdate("set-status", "IN_PROGRESS")
               .setUpdate("set-priority", "HIGH")
               .setUpdate("set-assignee", "developer")
               .setUpdate("set-title", "Updated Title")
               .setUpdate("set-description", "Updated description with more details");
        
        // Execute command
        Integer result = command.call();
        
        // Verify the command was successful
        assertEquals(0, result);
        
        // Verify all update types were started
        verify(metadataService).startOperation(eq("bulk-update-type-set-status"), eq("UPDATE"), any());
        verify(metadataService).startOperation(eq("bulk-update-type-set-priority"), eq("UPDATE"), any());
        verify(metadataService).startOperation(eq("bulk-update-type-set-assignee"), eq("UPDATE"), any());
        verify(metadataService).startOperation(eq("bulk-update-type-set-title"), eq("UPDATE"), any());
        verify(metadataService).startOperation(eq("bulk-update-type-set-description"), eq("UPDATE"), any());
        
        // Verify service interactions
        verify(workflowService, times(2)).transition(anyString(), eq("testuser"), eq(WorkflowState.IN_PROGRESS), anyString());
        verify(itemService, times(2)).updatePriority(any(), eq(Priority.HIGH), eq("testuser"));
        verify(itemService, times(2)).assignTo(any(), eq("developer"), eq("testuser"));
        verify(itemService, times(2)).updateTitle(any(), eq("Updated Title"), eq("testuser"));
        verify(itemService, times(2)).updateDescription(any(), eq("Updated description with more details"), eq("testuser"));
        
        // Verify operation completion tracking
        verify(metadataService, times(5)).completeOperation(eq("op-update-type"), any());
    }
}