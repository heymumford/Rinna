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
import java.time.LocalDate;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.command.CriticalPathCommand;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.*;

/**
 * Component test for CriticalPathCommand that validates integration between
 * CriticalPathCommand and various services, focusing on operation tracking
 * and service interactions.
 */
@ExtendWith(MockitoExtension.class)
public class CriticalPathCommandComponentTest {

    private CriticalPathCommand command;
    
    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private MetadataService metadataService;
    
    @Mock
    private MockCriticalPathService criticalPathService;
    
    @Mock
    private MockItemService itemService;
    
    @Mock
    private ContextManager contextManager;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> resultCaptor;
    
    // For capturing console output
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    @BeforeEach
    public void setUp() {
        // Redirect standard output and error
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Configure mock services
        when(serviceManager.getMetadataService()).thenReturn(metadataService);
        when(serviceManager.getMockCriticalPathService()).thenReturn(criticalPathService);
        when(serviceManager.getMockItemService()).thenReturn(itemService);
        
        // Configure ContextManager
        when(ContextManager.getInstance()).thenReturn(contextManager);
        
        // Configure operation tracking
        setupOperationTracking();
        
        // Setup test data and service behaviors
        setupTestData();
        
        // Create the command with mock services
        command = new CriticalPathCommand(serviceManager);
    }
    
    private void setupOperationTracking() {
        // Generate predictable operation IDs for testing
        when(metadataService.startOperation(eq("critical-path"), eq("READ"), any()))
            .thenReturn("op-critical-path");
    }
    
    private void setupTestData() {
        // Create sample critical path
        List<String> path = Arrays.asList("WI-101", "WI-102", "WI-103", "WI-104");
        when(criticalPathService.getCriticalPath()).thenReturn(path);
        
        // Create sample dependencies
        Map<String, List<String>> dependencies = new HashMap<>();
        dependencies.put("WI-102", Collections.singletonList("WI-101"));
        dependencies.put("WI-103", Collections.singletonList("WI-102"));
        dependencies.put("WI-104", Collections.singletonList("WI-103"));
        when(criticalPathService.getDependencyGraph()).thenReturn(dependencies);
        
        // Create mock work items
        WorkItem item1 = createWorkItem("WI-101", "Setup infrastructure", WorkflowState.IN_PROGRESS);
        WorkItem item2 = createWorkItem("WI-102", "Implement core service", WorkflowState.TODO);
        WorkItem item3 = createWorkItem("WI-103", "Create user interface", WorkflowState.TODO);
        WorkItem item4 = createWorkItem("WI-104", "Deploy to production", WorkflowState.TODO);
        
        when(itemService.getItem("WI-101")).thenReturn(item1);
        when(itemService.getItem("WI-102")).thenReturn(item2);
        when(itemService.getItem("WI-103")).thenReturn(item3);
        when(itemService.getItem("WI-104")).thenReturn(item4);
        
        // Setup critical path details
        Map<String, Object> details = new HashMap<>();
        details.put("pathLength", 4);
        details.put("totalEffort", 64);
        details.put("bottlenecks", Collections.singletonList("WI-101"));
        details.put("estimatedCompletionDate", LocalDate.now().plusDays(8));
        when(criticalPathService.getCriticalPathDetails()).thenReturn(details);
        
        // Setup critical path with estimates
        List<Map<String, Object>> criticalPathWithEstimates = new ArrayList<>();
        
        Map<String, Object> pathItem1 = new HashMap<>();
        pathItem1.put("id", "WI-101");
        pathItem1.put("estimatedEffort", 16);
        pathItem1.put("cumulativeEffort", 16);
        pathItem1.put("estimatedCompletionDate", LocalDate.now().plusDays(2));
        
        Map<String, Object> pathItem2 = new HashMap<>();
        pathItem2.put("id", "WI-102");
        pathItem2.put("estimatedEffort", 24);
        pathItem2.put("cumulativeEffort", 40);
        pathItem2.put("estimatedCompletionDate", LocalDate.now().plusDays(5));
        
        Map<String, Object> pathItem3 = new HashMap<>();
        pathItem3.put("id", "WI-103");
        pathItem3.put("estimatedEffort", 16);
        pathItem3.put("cumulativeEffort", 56);
        pathItem3.put("estimatedCompletionDate", LocalDate.now().plusDays(7));
        
        Map<String, Object> pathItem4 = new HashMap<>();
        pathItem4.put("id", "WI-104");
        pathItem4.put("estimatedEffort", 8);
        pathItem4.put("cumulativeEffort", 64);
        pathItem4.put("estimatedCompletionDate", LocalDate.now().plusDays(8));
        
        criticalPathWithEstimates.add(pathItem1);
        criticalPathWithEstimates.add(pathItem2);
        criticalPathWithEstimates.add(pathItem3);
        criticalPathWithEstimates.add(pathItem4);
        
        when(criticalPathService.getCriticalPathWithEstimates()).thenReturn(criticalPathWithEstimates);
        
        // Setup blockers
        List<Map<String, Object>> blockers = new ArrayList<>();
        Map<String, Object> blocker = new HashMap<>();
        blocker.put("id", "WI-101");
        blocker.put("directlyBlocks", Collections.singletonList("WI-102"));
        blocker.put("totalImpact", Arrays.asList("WI-102", "WI-103", "WI-104"));
        blockers.add(blocker);
        
        when(criticalPathService.getBlockers()).thenReturn(blockers);
        
        // Setup item specific data
        Map<String, Object> item102Path = new HashMap<>();
        item102Path.put("onCriticalPath", true);
        item102Path.put("position", 2);
        item102Path.put("directDependencies", Collections.singletonList("WI-101"));
        item102Path.put("indirectDependencies", Collections.emptyList());
        item102Path.put("criticalPath", path);
        
        when(criticalPathService.getItemCriticalPath("WI-102")).thenReturn(item102Path);
    }
    
    private WorkItem createWorkItem(String id, String title, WorkflowState state) {
        WorkItem item = new WorkItem();
        item.setId(id);
        item.setTitle(title);
        item.setState(state);
        return item;
    }
    
    @Test
    public void testFullCriticalPathWithOperationTracking() {
        // Execute command
        Integer result = command.call();
        
        // Verify success
        assertEquals(0, result);
        
        // Verify operation tracking
        verify(metadataService).startOperation(eq("critical-path"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("text", params.get("format"));
        assertFalse((Boolean) params.get("showBlockers"));
        assertFalse((Boolean) params.get("verbose"));
        
        // Verify operation completion
        verify(metadataService).completeOperation(eq("op-critical-path"), resultCaptor.capture());
        Map<String, Object> results = resultCaptor.getValue();
        assertEquals("text", results.get("outputFormat"));
        assertEquals(4, results.get("pathLength"));
        
        // Verify service interactions
        verify(criticalPathService).getCriticalPathWithEstimates();
        verify(criticalPathService).getCriticalPathDetails();
        
        // Verify output
        String output = outContent.toString();
        assertTrue(output.contains("Project critical path:"));
        assertTrue(output.contains("WI-101"));
        assertTrue(output.contains("Setup infrastructure"));
        assertTrue(output.contains("WI-102"));
        assertTrue(output.contains("Implement core service"));
        assertTrue(output.contains("WI-103"));
        assertTrue(output.contains("Create user interface"));
        assertTrue(output.contains("WI-104"));
        assertTrue(output.contains("Deploy to production"));
        assertTrue(output.contains("Estimated completion date:"));
    }
    
    @Test
    public void testBlockersViewWithOperationTracking() {
        // Configure command to show blockers
        command.setShowBlockers(true);
        
        // Execute command
        Integer result = command.call();
        
        // Verify success
        assertEquals(0, result);
        
        // Verify operation tracking
        verify(metadataService).startOperation(eq("critical-path"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertTrue((Boolean) params.get("showBlockers"));
        
        // Verify operation completion
        verify(metadataService).completeOperation(eq("op-critical-path"), resultCaptor.capture());
        Map<String, Object> results = resultCaptor.getValue();
        assertTrue((Boolean) results.get("showBlockers"));
        assertEquals(1, results.get("blockerCount"));
        
        // Verify service interactions
        verify(criticalPathService).getBlockers();
        verify(itemService).getItem("WI-101");
        
        // Verify output
        String output = outContent.toString();
        assertTrue(output.contains("Blocking items on critical path:"));
        assertTrue(output.contains("WI-101"));
        assertTrue(output.contains("Setup infrastructure"));
        assertTrue(output.contains("Directly blocks:"));
        assertTrue(output.contains("WI-102"));
    }
    
    @Test
    public void testItemViewWithOperationTracking() {
        // Configure command to show a specific item
        command.setItemId("WI-102");
        
        // Execute command
        Integer result = command.call();
        
        // Verify success
        assertEquals(0, result);
        
        // Verify operation tracking
        verify(metadataService).startOperation(eq("critical-path"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("WI-102", params.get("itemId"));
        
        // Verify operation completion
        verify(metadataService).completeOperation(eq("op-critical-path"), resultCaptor.capture());
        Map<String, Object> results = resultCaptor.getValue();
        assertEquals("WI-102", results.get("itemId"));
        assertTrue((Boolean) results.get("onCriticalPath"));
        assertEquals(1, results.get("dependencies"));
        
        // Verify service interactions
        verify(criticalPathService).getItemCriticalPath("WI-102");
        verify(itemService).getItem("WI-101");
        
        // Verify output
        String output = outContent.toString();
        assertTrue(output.contains("Dependencies for work item: WI-102"));
        assertTrue(output.contains("Direct dependency: WI-101"));
        assertTrue(output.contains("Title: Setup infrastructure"));
    }
    
    @Test
    public void testJsonOutputWithOperationTracking() {
        // Configure command for JSON output
        command.setFormat("json");
        
        // Execute command
        Integer result = command.call();
        
        // Verify success
        assertEquals(0, result);
        
        // Verify operation tracking
        verify(metadataService).startOperation(eq("critical-path"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("json", params.get("format"));
        
        // Verify operation completion
        verify(metadataService).completeOperation(eq("op-critical-path"), resultCaptor.capture());
        Map<String, Object> results = resultCaptor.getValue();
        assertEquals("json", results.get("outputFormat"));
        assertEquals(4, results.get("pathLength"));
        
        // Verify service interactions
        verify(criticalPathService).getCriticalPathWithEstimates();
        verify(criticalPathService).getCriticalPathDetails();
        
        // Verify JSON output
        String output = outContent.toString();
        assertTrue(output.contains("\"result\":\"success\""));
        assertTrue(output.contains("\"criticalPath\":"));
        assertTrue(output.contains("\"pathLength\":4"));
        assertTrue(output.contains("\"details\":"));
    }
    
    @Test
    public void testErrorHandlingWithOperationTracking() {
        // Configure service to throw an exception
        when(criticalPathService.getCriticalPathWithEstimates())
            .thenThrow(new RuntimeException("Test error"));
        
        // Execute command
        Integer result = command.call();
        
        // Verify failure
        assertEquals(1, result);
        
        // Verify operation tracking
        verify(metadataService).startOperation(eq("critical-path"), eq("READ"), any());
        
        // Verify operation failure is tracked
        verify(metadataService).failOperation(eq("op-critical-path"), any(RuntimeException.class));
        
        // Verify error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Error:"));
        assertTrue(errorOutput.contains("Test error"));
    }
    
    @Test
    public void testVerboseOutputWithOperationTracking() {
        // Configure command for verbose output
        command.setVerbose(true);
        
        // Execute command
        Integer result = command.call();
        
        // Verify success
        assertEquals(0, result);
        
        // Verify operation tracking
        verify(metadataService).startOperation(eq("critical-path"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertTrue((Boolean) params.get("verbose"));
        
        // Verify operation completion includes verbose details
        verify(metadataService).completeOperation(eq("op-critical-path"), resultCaptor.capture());
        Map<String, Object> results = resultCaptor.getValue();
        assertEquals(64, results.get("totalEffort"));
        assertEquals(1, results.get("bottlenecks"));
        
        // Verify verbose output
        String output = outContent.toString();
        assertTrue(output.contains("Total estimated effort:"));
        assertTrue(output.contains("Identified bottlenecks:"));
    }
}