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
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockCriticalPathService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.ServiceManager;

@ExtendWith(MockitoExtension.class)
public class CriticalPathCommandTest {

    private CriticalPathCommand command;
    
    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private MetadataService metadataService;
    
    @Mock
    private MockCriticalPathService criticalPathService;
    
    @Mock
    private MockItemService itemService;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    
    @Captor
    private ArgumentCaptor<Map<String, Object>> resultCaptor;
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalErr = System.err;
    
    @BeforeEach
    public void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Setup service manager mocks
        when(serviceManager.getMetadataService()).thenReturn(metadataService);
        when(serviceManager.getCriticalPathService()).thenReturn(criticalPathService);
        when(serviceManager.getItemService()).thenReturn(itemService);
        
        // Setup operation tracking
        when(metadataService.startOperation(anyString(), anyString(), any())).thenReturn("op-123");
        
        // Setup test critical path
        List<String> criticalPath = Arrays.asList("WI-101", "WI-102", "WI-103");
        when(criticalPathService.getCriticalPath()).thenReturn(criticalPath);
        
        // Setup mock work items
        setupMockWorkItems();
        
        // Setup critical path details
        setupCriticalPathDetails();
        
        // Create command
        command = new CriticalPathCommand(serviceManager);
    }
    
    private void setupMockWorkItems() {
        // Setup mock work items
        WorkItem item1 = new WorkItem();
        item1.setId("WI-101");
        item1.setTitle("First critical task");
        item1.setState(WorkflowState.TO_DO);
        
        WorkItem item2 = new WorkItem();
        item2.setId("WI-102");
        item2.setTitle("Second critical task");
        item2.setState(WorkflowState.IN_PROGRESS);
        
        WorkItem item3 = new WorkItem();
        item3.setId("WI-103");
        item3.setTitle("Third critical task");
        item3.setState(WorkflowState.TO_DO);
        
        when(itemService.getItem("WI-101")).thenReturn(item1);
        when(itemService.getItem("WI-102")).thenReturn(item2);
        when(itemService.getItem("WI-103")).thenReturn(item3);
    }
    
    private void setupCriticalPathDetails() {
        // Setup critical path details
        Map<String, Object> details = new HashMap<>();
        details.put("pathLength", 3);
        details.put("totalEffort", 40);
        details.put("bottlenecks", Collections.singletonList("WI-101"));
        details.put("estimatedCompletionDate", LocalDate.now().plusDays(5));
        when(criticalPathService.getCriticalPathDetails()).thenReturn(details);
        
        // Setup critical path with estimates
        List<Map<String, Object>> criticalPathWithEstimates = new ArrayList<>();
        
        Map<String, Object> item1 = new HashMap<>();
        item1.put("id", "WI-101");
        item1.put("estimatedEffort", 8);
        item1.put("cumulativeEffort", 8);
        item1.put("estimatedCompletionDate", LocalDate.now().plusDays(1));
        
        Map<String, Object> item2 = new HashMap<>();
        item2.put("id", "WI-102");
        item2.put("estimatedEffort", 16);
        item2.put("cumulativeEffort", 24);
        item2.put("estimatedCompletionDate", LocalDate.now().plusDays(3));
        
        Map<String, Object> item3 = new HashMap<>();
        item3.put("id", "WI-103");
        item3.put("estimatedEffort", 16);
        item3.put("cumulativeEffort", 40);
        item3.put("estimatedCompletionDate", LocalDate.now().plusDays(5));
        
        criticalPathWithEstimates.add(item1);
        criticalPathWithEstimates.add(item2);
        criticalPathWithEstimates.add(item3);
        
        when(criticalPathService.getCriticalPathWithEstimates()).thenReturn(criticalPathWithEstimates);
        
        // Setup blockers
        List<Map<String, Object>> blockers = new ArrayList<>();
        Map<String, Object> blocker = new HashMap<>();
        blocker.put("id", "WI-101");
        blocker.put("directlyBlocks", Arrays.asList("WI-102"));
        blocker.put("totalImpact", Arrays.asList("WI-102", "WI-103"));
        blockers.add(blocker);
        
        when(criticalPathService.getBlockers()).thenReturn(blockers);
        
        // Setup item critical path
        Map<String, Object> itemPath = new HashMap<>();
        itemPath.put("onCriticalPath", true);
        itemPath.put("position", 2);
        itemPath.put("directDependencies", Collections.singletonList("WI-101"));
        itemPath.put("indirectDependencies", Collections.emptyList());
        
        when(criticalPathService.getItemCriticalPath("WI-102")).thenReturn(itemPath);
        
        // Setup for item not on critical path
        Map<String, Object> nonCriticalPath = new HashMap<>();
        nonCriticalPath.put("onCriticalPath", false);
        nonCriticalPath.put("criticalPath", Collections.emptyList());
        
        when(criticalPathService.getItemCriticalPath("WI-999")).thenReturn(nonCriticalPath);
    }
    
    @Test
    public void testDefaultCriticalPathOutput() {
        // Execute command with default parameters
        Integer result = command.call();
        
        // Verify success
        assertEquals(0, result);
        
        // Verify operation tracking
        verify(metadataService).startOperation(eq("critical-path"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertFalse((Boolean) params.get("showBlockers"));
        assertEquals("text", params.get("format"));
        assertFalse((Boolean) params.get("verbose"));
        
        // Verify operation completion
        verify(metadataService).completeOperation(eq("op-123"), resultCaptor.capture());
        Map<String, Object> results = resultCaptor.getValue();
        assertEquals("text", results.get("outputFormat"));
        assertEquals(3, results.get("pathLength"));
        
        // Verify output contains critical path information
        String output = outContent.toString();
        assertTrue(output.contains("Project critical path:"));
        assertTrue(output.contains("WI-101"));
        assertTrue(output.contains("First critical task"));
        assertTrue(output.contains("WI-102"));
        assertTrue(output.contains("Second critical task"));
        assertTrue(output.contains("WI-103"));
        assertTrue(output.contains("Third critical task"));
        assertTrue(output.contains("Estimated completion date:"));
    }
    
    @Test
    public void testJsonFormatOutput() {
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
        verify(metadataService).completeOperation(eq("op-123"), resultCaptor.capture());
        Map<String, Object> results = resultCaptor.getValue();
        assertEquals("json", results.get("outputFormat"));
        
        // Verify JSON output
        String output = outContent.toString();
        assertTrue(output.contains("\"result\":\"success\""));
        assertTrue(output.contains("\"criticalPath\":"));
        assertTrue(output.contains("\"details\":"));
        assertTrue(output.contains("\"pathLength\":3"));
    }
    
    @Test
    public void testBlockersView() {
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
        verify(metadataService).completeOperation(eq("op-123"), resultCaptor.capture());
        Map<String, Object> results = resultCaptor.getValue();
        assertTrue((Boolean) results.get("showBlockers"));
        assertEquals(1, results.get("blockerCount"));
        
        // Verify output contains blocker information
        String output = outContent.toString();
        assertTrue(output.contains("Blocking items on critical path:"));
        assertTrue(output.contains("WI-101"));
        assertTrue(output.contains("First critical task"));
        assertTrue(output.contains("Directly blocks:"));
        assertTrue(output.contains("WI-102"));
    }
    
    @Test
    public void testSpecificItemView() {
        // Configure command to show specific item
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
        verify(metadataService).completeOperation(eq("op-123"), resultCaptor.capture());
        Map<String, Object> results = resultCaptor.getValue();
        assertEquals("WI-102", results.get("itemId"));
        assertTrue((Boolean) results.get("onCriticalPath"));
        assertEquals(1, results.get("dependencies"));
        
        // Verify output contains item dependency information
        String output = outContent.toString();
        assertTrue(output.contains("Dependencies for work item: WI-102"));
        assertTrue(output.contains("Direct dependency: WI-101"));
        assertTrue(output.contains("Title: First critical task"));
    }
    
    @Test
    public void testItemNotOnCriticalPath() {
        // Configure command to show item not on critical path
        command.setItemId("WI-999");
        
        // Execute command
        Integer result = command.call();
        
        // Verify success
        assertEquals(0, result);
        
        // Verify operation tracking
        verify(metadataService).startOperation(eq("critical-path"), eq("READ"), any());
        
        // Verify operation completion
        verify(metadataService).completeOperation(eq("op-123"), resultCaptor.capture());
        Map<String, Object> results = resultCaptor.getValue();
        assertEquals("WI-999", results.get("itemId"));
        assertFalse((Boolean) results.get("onCriticalPath"));
        
        // Verify output indicates item is not on critical path
        String output = outContent.toString();
        assertTrue(output.contains("Dependencies for work item: WI-999"));
        assertTrue(output.contains("This item is not on the critical path."));
    }
    
    @Test
    public void testVerboseOutput() {
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
        
        // Verify operation completion includes additional verbose data
        verify(metadataService).completeOperation(eq("op-123"), resultCaptor.capture());
        Map<String, Object> results = resultCaptor.getValue();
        assertEquals(40, results.get("totalEffort"));
        assertEquals(1, results.get("bottlenecks"));
        
        // Verify verbose output
        String output = outContent.toString();
        assertTrue(output.contains("Total estimated effort:"));
        assertTrue(output.contains("Identified bottlenecks:"));
    }
    
    @Test
    public void testErrorHandling() {
        // Setup exception in criticalPathService
        when(criticalPathService.getCriticalPathWithEstimates())
            .thenThrow(new RuntimeException("Test error"));
        
        // Execute command
        Integer result = command.call();
        
        // Verify failure
        assertEquals(1, result);
        
        // Verify operation tracking
        verify(metadataService).startOperation(eq("critical-path"), eq("READ"), any());
        
        // Verify operation failure is tracked
        verify(metadataService).failOperation(eq("op-123"), any(RuntimeException.class));
        
        // Verify error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Error:"));
        assertTrue(errorOutput.contains("Test error"));
    }
    
    @Test
    public void testJsonErrorHandling() {
        // Setup command for JSON output
        command.setFormat("json");
        
        // Setup exception
        when(criticalPathService.getCriticalPathWithEstimates())
            .thenThrow(new RuntimeException("JSON test error"));
        
        // Execute command
        Integer result = command.call();
        
        // Verify failure
        assertEquals(1, result);
        
        // Verify operation failure is tracked
        verify(metadataService).failOperation(eq("op-123"), any(RuntimeException.class));
        
        // Verify JSON error output
        String output = outContent.toString();
        assertTrue(output.contains("\"result\":\"error\""));
        assertTrue(output.contains("\"message\":\"JSON test error\""));
    }
    
    @Test
    public void testMethodChaining() {
        // Set properties individually instead of chaining (chaining not supported)
        command.setShowBlockers(true);
        command.setItemId("WI-102");
        command.setFormat("json");
        command.setVerbose(true);
        
        // Verify properties were set
        assertTrue(command.isShowBlockers());
        assertEquals("WI-102", command.getItemId());
        assertEquals("json", command.getFormat());
        assertTrue(command.isVerbose());
    }
}