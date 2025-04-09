/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.command.*;
import org.rinna.cli.command.CriticalPathCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.ServiceStatus;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.notifications.Notification;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Integration test for MetadataService with various CLI commands.
 * This test demonstrates common patterns for operation tracking across commands.
 */
@ExtendWith(MockitoExtension.class)
public class MetadataServiceIntegrationTest {

    @Mock
    private MetadataService metadataService;
    
    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private ItemService itemService;
    
    @Mock
    private WorkflowService workflowService;
    
    @Mock
    private ConfigurationService configService;
    
    @Mock
    private ContextManager contextManager;
    
    @Mock
    private MockNotificationService notificationService;
    
    @Mock
    private MockCriticalPathService criticalPathService;
    
    @Mock
    private SearchService searchService;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> resultCaptor;
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outContent));
        
        // Configure common mocks
        when(serviceManager.getMetadataService()).thenReturn(metadataService);
        when(serviceManager.getItemService()).thenReturn(itemService);
        when(serviceManager.getMockItemService()).thenReturn(itemService);
        when(serviceManager.getWorkflowService()).thenReturn(workflowService);
        when(serviceManager.getMockWorkflowService()).thenReturn(workflowService);
        when(serviceManager.getConfigurationService()).thenReturn(configService);
        when(serviceManager.getMockNotificationService()).thenReturn(notificationService);
        when(serviceManager.getMockCriticalPathService()).thenReturn(criticalPathService);
        when(serviceManager.getMockSearchService()).thenReturn(searchService);
        
        when(configService.getCurrentUser()).thenReturn("testuser");
        
        when(ContextManager.getInstance()).thenReturn(contextManager);
        
        // Configure operation tracking
        when(metadataService.startOperation(anyString(), anyString(), any())).thenReturn("op-123");
    }
    
    @Test
    public void testWorkflowCommandOperationTracking() {
        // Setup mocks
        WorkItem workItem = new WorkItem();
        workItem.setId("WI-123");
        workItem.setState(WorkflowState.TODO);
        workItem.setTitle("Test Work Item");
        
        when(itemService.getItem("WI-123")).thenReturn(workItem);
        
        // Create command
        WorkflowCommand command = new WorkflowCommand(serviceManager);
        command.setItemId("WI-123");
        command.setTargetState(WorkflowState.IN_PROGRESS);
        command.setComment("Moving to in progress");
        
        // Execute command
        Integer result = command.call();
        
        // Verify command succeeded
        assertEquals(0, result);
        
        // Verify operation tracking
        // 1. Main command operation
        verify(metadataService).startOperation(eq("workflow-command"), eq("UPDATE"), paramsCaptor.capture());
        Map<String, Object> mainParams = paramsCaptor.getValue();
        assertEquals("WI-123", mainParams.get("itemId"));
        assertEquals("IN_PROGRESS", mainParams.get("targetState"));
        assertTrue(mainParams.containsKey("comment"));
        
        // 2. Validation operation
        verify(metadataService).startOperation(eq("workflow-validate"), eq("VALIDATE"), any());
        
        // 3. Transition operation
        verify(metadataService).startOperation(eq("workflow-transition"), eq("UPDATE"), any());
        
        // 4. Display operation
        verify(metadataService).startOperation(eq("workflow-display"), eq("READ"), any());
        
        // Verify operation completion
        verify(metadataService, times(4)).completeOperation(eq("op-123"), any());
        
        // Verify service interactions
        verify(workflowService).transition(eq("WI-123"), eq("testuser"), 
            eq(WorkflowState.IN_PROGRESS), eq("Moving to in progress"));
    }
    
    @Test
    public void testNotifyCommandOperationTracking() {
        // Setup mock data
        List<Notification> notifications = new ArrayList<>();
        Notification notification1 = new Notification();
        notification1.setId("notif-1");
        notification1.setMessage("Test notification 1");
        notification1.setRead(false);
        
        Notification notification2 = new Notification();
        notification2.setId("notif-2");
        notification2.setMessage("Test notification 2");
        notification2.setRead(false);
        
        notifications.add(notification1);
        notifications.add(notification2);
        
        when(notificationService.getAllNotifications()).thenReturn(notifications);
        when(notificationService.markAsRead("notif-1")).thenReturn(true);
        
        // Create command
        NotifyCommand command = new NotifyCommand(serviceManager);
        
        // Test listing notifications
        command.setAction("list");
        Integer listResult = command.call();
        assertEquals(0, listResult);
        
        // Verify list operation tracking
        verify(metadataService).startOperation(eq("notify-command"), eq("READ"), any());
        verify(metadataService).startOperation(eq("notify-list"), eq("READ"), any());
        
        // Test marking a notification as read
        command.setAction("read");
        command.setNotificationId("notif-1");
        Integer readResult = command.call();
        assertEquals(0, readResult);
        
        // Verify read operation tracking
        verify(metadataService).startOperation(eq("notify-read"), eq("UPDATE"), paramsCaptor.capture());
        Map<String, Object> readParams = paramsCaptor.getValue();
        assertEquals("notif-1", readParams.get("notificationId"));
        
        // Verify operation completion with results
        verify(metadataService).completeOperation(eq("op-123"), resultCaptor.capture());
        Map<String, Object> readResult = resultCaptor.getValue();
        assertTrue((Boolean) readResult.get("success"));
    }
    
    @Test
    public void testServerCommandOperationTracking() {
        // Setup mock data
        ServiceStatus runningStatus = new ServiceStatus();
        runningStatus.setRunning(true);
        runningStatus.setStartTime(LocalDateTime.now().minusHours(1));
        runningStatus.setPid(12345);
        runningStatus.setPort(8080);
        
        when(serviceManager.getStatus()).thenReturn(runningStatus);
        
        // Create command
        ServerCommand command = new ServerCommand(serviceManager);
        
        // Test status action
        command.setAction("status");
        Integer statusResult = command.call();
        assertEquals(0, statusResult);
        
        // Verify status operation tracking
        verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
        verify(metadataService).startOperation(eq("server-status"), eq("READ"), any());
        
        // Test stop action
        command.setAction("stop");
        command.call();
        
        // Verify stop operation tracking
        verify(metadataService).startOperation(eq("server-stop"), eq("EXECUTE"), any());
        
        // Verify operation completion with results
        verify(metadataService, atLeastOnce()).completeOperation(eq("op-123"), resultCaptor.capture());
    }
    
    @Test
    public void testBulkCommandOperationTracking() {
        // Setup mock data
        List<WorkItem> items = new ArrayList<>();
        WorkItem item1 = new WorkItem();
        item1.setId("WI-101");
        item1.setTitle("Test Item 1");
        item1.setState(WorkflowState.TODO);
        item1.setPriority(Priority.MEDIUM);
        
        WorkItem item2 = new WorkItem();
        item2.setId("WI-102");
        item2.setTitle("Test Item 2");
        item2.setState(WorkflowState.TODO);
        item2.setPriority(Priority.LOW);
        
        items.add(item1);
        items.add(item2);
        
        when(searchService.findItemsByText("test")).thenReturn(items);
        
        // Create command with filter and update
        BulkCommand command = new BulkCommand(serviceManager);
        command.setFilter("text", "test");
        command.setUpdate("set-priority", "HIGH");
        
        // Execute command
        Integer result = command.call();
        assertEquals(0, result);
        
        // Verify hierarchical operation structure
        // 1. Main command operation
        verify(metadataService).startOperation(eq("bulk-command"), eq("UPDATE"), any());
        
        // 2. Filter operations
        verify(metadataService).startOperation(eq("bulk-filter"), eq("SEARCH"), any());
        verify(metadataService).startOperation(eq("bulk-filter-method"), eq("SEARCH"), any());
        verify(metadataService).startOperation(eq("bulk-primary-filter"), eq("SEARCH"), any());
        
        // 3. Update operations
        verify(metadataService).startOperation(eq("bulk-update-apply"), eq("UPDATE"), any());
        verify(metadataService).startOperation(eq("bulk-apply-updates-method"), eq("UPDATE"), any());
        verify(metadataService).startOperation(eq("bulk-update-type-set-priority"), eq("UPDATE"), any());
        
        // 4. Item-specific operations (for each item)
        verify(metadataService, times(2)).startOperation(eq("bulk-update-item"), eq("UPDATE"), any());
        verify(metadataService, times(2)).startOperation(eq("bulk-update-item-priority"), eq("UPDATE"), any());
        
        // 5. Display operation
        verify(metadataService).startOperation(eq("bulk-result-display"), eq("READ"), any());
        
        // Verify operation completion
        verify(metadataService, atLeast(10)).completeOperation(eq("op-123"), any());
        
        // Verify service interactions
        verify(itemService, times(2)).updatePriority(any(), eq(Priority.HIGH), eq("testuser"));
    }
    
    @Test
    public void testCriticalPathCommandOperationTracking() {
        // Setup mock data
        List<String> criticalPath = Arrays.asList("WI-101", "WI-102", "WI-103");
        when(criticalPathService.getCriticalPath()).thenReturn(criticalPath);
        
        Map<String, Object> details = new HashMap<>();
        details.put("pathLength", 3);
        details.put("totalEffort", 40);
        details.put("bottlenecks", Collections.singletonList("WI-101"));
        details.put("estimatedCompletionDate", LocalDate.now().plusDays(5));
        when(criticalPathService.getCriticalPathDetails()).thenReturn(details);
        
        List<Map<String, Object>> criticalPathWithEstimates = new ArrayList<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("id", "WI-101");
        item1.put("estimatedEffort", 16);
        criticalPathWithEstimates.add(item1);
        when(criticalPathService.getCriticalPathWithEstimates()).thenReturn(criticalPathWithEstimates);
        
        // Create work item
        WorkItem workItem = new WorkItem();
        workItem.setId("WI-101");
        workItem.setTitle("Test Item");
        workItem.setState(WorkflowState.TODO);
        when(itemService.getItem("WI-101")).thenReturn(workItem);
        
        // Create command
        CriticalPathCommand command = new CriticalPathCommand(serviceManager);
        
        // Execute command
        Integer result = command.call();
        
        // Verify success
        assertEquals(0, result);
        
        // Verify operation tracking
        verify(metadataService).startOperation(eq("critical-path"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("text", params.get("format"));
        
        // Verify operation completion
        verify(metadataService).completeOperation(eq("op-123"), resultCaptor.capture());
        Map<String, Object> results = resultCaptor.getValue();
        assertEquals("text", results.get("outputFormat"));
        assertEquals(3, results.get("pathLength"));
        
        // Test with specific options
        command = new CriticalPathCommand(serviceManager);
        command.setShowBlockers(true);
        
        // Setup blockers data
        List<Map<String, Object>> blockers = new ArrayList<>();
        Map<String, Object> blocker = new HashMap<>();
        blocker.put("id", "WI-101");
        blocker.put("directlyBlocks", Arrays.asList("WI-102"));
        blocker.put("totalImpact", Arrays.asList("WI-102", "WI-103"));
        blockers.add(blocker);
        when(criticalPathService.getBlockers()).thenReturn(blockers);
        
        // Execute command with blockers option
        result = command.call();
        
        // Verify operation tracking for blockers view
        verify(metadataService, times(2)).startOperation(eq("critical-path"), eq("READ"), any());
        verify(metadataService, times(2)).completeOperation(eq("op-123"), any());
    }
    
    @Test
    public void testCommonOperationFailurePatterns() {
        // Setup mocks to trigger errors
        when(itemService.getItem("WI-999")).thenThrow(new IllegalArgumentException("Item not found"));
        
        // 1. Test workflow command error handling
        WorkflowCommand workflowCommand = new WorkflowCommand(serviceManager);
        workflowCommand.setItemId("WI-999");
        workflowCommand.setTargetState(WorkflowState.IN_PROGRESS);
        
        // Execute command
        Integer result = workflowCommand.call();
        
        // Verify command failed
        assertEquals(1, result);
        
        // Verify operation failure tracking
        verify(metadataService).failOperation(eq("op-123"), any(IllegalArgumentException.class));
        
        // 2. Test bulk command error handling
        // Configure search service to throw exception for certain parameters
        doThrow(new IllegalArgumentException("Invalid status"))
            .when(workflowService).transition(eq("WI-101"), any(), any(), any());
            
        when(searchService.findItemsByText("error")).thenReturn(Collections.singletonList(
            createWorkItem("WI-101", "Error Item", WorkflowState.TODO, Priority.MEDIUM)));
        
        BulkCommand bulkCommand = new BulkCommand(serviceManager);
        bulkCommand.setFilter("text", "error");
        bulkCommand.setUpdate("set-status", "IN_PROGRESS");
        
        // Execute command
        Integer bulkResult = bulkCommand.call();
        
        // Verify command failed
        assertEquals(1, bulkResult);
        
        // Verify failure was tracked in nested operations
        verify(metadataService, atLeastOnce()).failOperation(eq("op-123"), any(IllegalArgumentException.class));
    }
    
    private WorkItem createWorkItem(String id, String title, WorkflowState state, Priority priority) {
        WorkItem item = new WorkItem();
        item.setId(id);
        item.setTitle(title);
        item.setState(state);
        item.setPriority(priority);
        return item;
    }
}