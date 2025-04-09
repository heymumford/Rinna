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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.MockedStatic;
import org.rinna.cli.command.ListCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockSearchService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Component test for the ListCommand class.
 * This test focuses on the integration of ListCommand with the services it depends on.
 */
@DisplayName("ListCommand Component Tests")
public class ListCommandComponentTest {

    private ServiceManager mockServiceManager;
    private MockSearchService mockSearchService;
    private MetadataService mockMetadataService;
    
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    private List<WorkItem> testWorkItems;
    private ArgumentCaptor<Map<String, String>> criteriaCaptor;
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    private ArgumentCaptor<Map<String, Object>> resultCaptor;
    
    private MockedStatic<ServiceManager> mockedStaticServiceManager;
    private MockedStatic<OutputFormatter> mockedStaticOutputFormatter;
    
    @BeforeEach
    void setUp() {
        // Set up mocks
        mockServiceManager = mock(ServiceManager.class);
        mockSearchService = mock(MockSearchService.class);
        mockMetadataService = mock(MetadataService.class);
        
        // Set up static mocks
        mockedStaticServiceManager = Mockito.mockStatic(ServiceManager.class);
        mockedStaticServiceManager.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        
        mockedStaticOutputFormatter = Mockito.mockStatic(OutputFormatter.class);
        // Set up JSON formatter for testing JSON output
        mockedStaticOutputFormatter.when(() -> OutputFormatter.toJson(any(Map.class), any(Boolean.class)))
            .thenAnswer(invocation -> {
                Map<String, Object> map = invocation.getArgument(0);
                return "{\"count\":" + map.get("count") + ",\"items\":[...]}";
            });
        
        // Set up service manager
        when(mockServiceManager.getMockSearchService()).thenReturn(mockSearchService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up metadata service
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn("operation-id");
        
        // Set up argument captors
        criteriaCaptor = ArgumentCaptor.forClass(Map.class);
        paramsCaptor = ArgumentCaptor.forClass(Map.class);
        resultCaptor = ArgumentCaptor.forClass(Map.class);
        
        // Redirect stdout and stderr for verification
        originalOut = System.out;
        originalErr = System.err;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Initialize test data
        initializeTestWorkItems();
        
        // Set up mock search service default behavior
        when(mockSearchService.findWorkItems(anyMap(), anyInt())).thenReturn(testWorkItems);
    }
    
    @AfterEach
    void tearDown() {
        // Restore original stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Close static mocks
        if (mockedStaticServiceManager != null) {
            mockedStaticServiceManager.close();
        }
        if (mockedStaticOutputFormatter != null) {
            mockedStaticOutputFormatter.close();
        }
    }
    
    private void initializeTestWorkItems() {
        testWorkItems = new ArrayList<>();
        
        // Create test work items
        WorkItem bugItem = new WorkItem();
        bugItem.setId(UUID.randomUUID().toString());
        bugItem.setTitle("Bug in login system");
        bugItem.setType(WorkItemType.BUG);
        bugItem.setPriority(Priority.HIGH);
        bugItem.setStatus(WorkflowState.IN_PROGRESS);
        bugItem.setAssignee("alice");
        bugItem.setProject("Project-A");
        bugItem.setDescription("Users are unable to log in using social media accounts");
        bugItem.setCreated(LocalDateTime.now().minusDays(5));
        bugItem.setUpdated(LocalDateTime.now().minusHours(12));
        
        WorkItem taskItem = new WorkItem();
        taskItem.setId(UUID.randomUUID().toString());
        taskItem.setTitle("Update documentation");
        taskItem.setType(WorkItemType.TASK);
        taskItem.setPriority(Priority.MEDIUM);
        taskItem.setStatus(WorkflowState.READY);
        taskItem.setAssignee("bob");
        taskItem.setProject("Project-A");
        taskItem.setDescription("Update API documentation with new endpoints");
        taskItem.setCreated(LocalDateTime.now().minusDays(3));
        taskItem.setUpdated(LocalDateTime.now().minusDays(2));
        
        WorkItem featureItem = new WorkItem();
        featureItem.setId(UUID.randomUUID().toString());
        featureItem.setTitle("Add export to CSV feature");
        featureItem.setType(WorkItemType.FEATURE);
        featureItem.setPriority(Priority.LOW);
        featureItem.setStatus(WorkflowState.DONE);
        featureItem.setAssignee("charlie");
        featureItem.setProject("Project-B");
        featureItem.setDescription("Add functionality to export work items to CSV format");
        featureItem.setCreated(LocalDateTime.now().minusDays(10));
        featureItem.setUpdated(LocalDateTime.now().minusHours(24));
        
        testWorkItems.add(bugItem);
        testWorkItems.add(taskItem);
        testWorkItems.add(featureItem);
    }
    
    @Test
    @DisplayName("Should integrate with SearchService to list work items")
    void shouldIntegrateWithSearchServiceToListWorkItems() {
        // Arrange
        ListCommand command = new ListCommand(mockServiceManager);
        when(mockSearchService.findWorkItems(criteriaCaptor.capture(), anyInt())).thenReturn(testWorkItems);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify integration with search service
        verify(mockSearchService).findWorkItems(any(), eq(100));
        
        // Verify output contains expected elements
        String output = outContent.toString();
        assertTrue(output.contains("Work Items:"), "Output should include Work Items header");
        assertTrue(output.contains("----------------"), "Output should include table border");
        assertTrue(output.contains("ID"), "Output should include ID column header");
        assertTrue(output.contains("TITLE"), "Output should include TITLE column header");
        
        // Verify metadata tracking
        verify(mockMetadataService).startOperation(eq("list"), eq("READ"), any());
        verify(mockMetadataService).completeOperation(anyString(), any());
    }
    
    @Test
    @DisplayName("Should integrate with SearchService to filter work items by type")
    void shouldIntegrateWithSearchServiceToFilterWorkItemsByType() {
        // Arrange
        ListCommand command = new ListCommand(mockServiceManager);
        command.setType(WorkItemType.BUG);
        
        // Create filtered list of items
        List<WorkItem> filteredItems = testWorkItems.stream()
            .filter(item -> item.getType() == WorkItemType.BUG)
            .toList();
        
        when(mockSearchService.findWorkItems(criteriaCaptor.capture(), anyInt())).thenReturn(filteredItems);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify correct criteria passed to search service
        Map<String, String> criteria = criteriaCaptor.getValue();
        assertEquals("BUG", criteria.get("type"), "Should pass BUG type to search service");
        
        // Verify metadata tracking includes type filter
        verify(mockMetadataService).startOperation(eq("list"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("BUG", params.get("type"), "Operation tracking should include type filter");
    }
    
    @Test
    @DisplayName("Should integrate with OutputFormatter to produce JSON output")
    void shouldIntegrateWithOutputFormatterToProduceJsonOutput() {
        // Arrange
        ListCommand command = new ListCommand(mockServiceManager);
        command.setFormat("json");
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify OutputFormatter integration
        mockedStaticOutputFormatter.verify(() -> 
            OutputFormatter.toJson(any(Map.class), eq(false)), times(1));
        
        // Verify output format
        String output = outContent.toString();
        assertTrue(output.contains("{"), "Output should be JSON formatted");
        assertTrue(output.contains("}"), "Output should be JSON formatted");
        
        // Verify metadata tracking includes format parameter
        verify(mockMetadataService).startOperation(eq("list"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("json", params.get("format"), "Operation tracking should include format parameter");
    }
    
    @Test
    @DisplayName("Should handle empty results correctly")
    void shouldHandleEmptyResultsCorrectly() {
        // Arrange
        ListCommand command = new ListCommand(mockServiceManager);
        when(mockSearchService.findWorkItems(anyMap(), anyInt())).thenReturn(new ArrayList<>());
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully even with empty results");
        
        // Verify output indicates no items found
        String output = outContent.toString();
        assertTrue(output.contains("No work items found"), "Output should indicate no items found");
        
        // Verify metadata tracking includes zero count
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        Map<String, Object> operationResult = resultCaptor.getValue();
        assertEquals(0, operationResult.get("count"), "Result should indicate zero count");
    }
    
    @Test
    @DisplayName("Should handle search service exceptions gracefully")
    void shouldHandleSearchServiceExceptionsGracefully() {
        // Arrange
        ListCommand command = new ListCommand(mockServiceManager);
        when(mockSearchService.findWorkItems(anyMap(), anyInt()))
            .thenThrow(new RuntimeException("Test exception"));
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(1, result, "Command should fail when search service throws exception");
        
        // Verify error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Error listing work items"), 
                "Error output should indicate problem with listing items");
        
        // Verify metadata tracking includes exception
        verify(mockMetadataService).failOperation(anyString(), any(Exception.class));
    }
    
    @Test
    @DisplayName("Should correctly apply multiple filtering criteria")
    void shouldCorrectlyApplyMultipleFilteringCriteria() {
        // Arrange
        ListCommand command = new ListCommand(mockServiceManager);
        command.setType(WorkItemType.TASK);
        command.setPriority(Priority.MEDIUM);
        command.setProject("Project-A");
        
        // Create filtered list to return
        List<WorkItem> filteredItems = testWorkItems.stream()
            .filter(item -> item.getType() == WorkItemType.TASK 
                && item.getPriority() == Priority.MEDIUM
                && "Project-A".equals(item.getProject()))
            .toList();
        
        when(mockSearchService.findWorkItems(criteriaCaptor.capture(), anyInt())).thenReturn(filteredItems);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify correct criteria passed to search service
        Map<String, String> criteria = criteriaCaptor.getValue();
        assertEquals("TASK", criteria.get("type"), "Should pass TASK type to search service");
        assertEquals("MEDIUM", criteria.get("priority"), "Should pass MEDIUM priority to search service");
        assertEquals("Project-A", criteria.get("project"), "Should pass Project-A to search service");
        
        // Verify operation tracking includes all filters
        verify(mockMetadataService).startOperation(eq("list"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("TASK", params.get("type"), "Operation tracking should include type filter");
        assertEquals("MEDIUM", params.get("priority"), "Operation tracking should include priority filter");
        assertEquals("Project-A", params.get("project"), "Operation tracking should include project filter");
    }
    
    @Test
    @DisplayName("Should apply limit parameter correctly")
    void shouldApplyLimitParameterCorrectly() {
        // Arrange
        int testLimit = 2;
        ListCommand command = new ListCommand(mockServiceManager);
        command.setLimit(testLimit);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify limit passed to search service
        verify(mockSearchService).findWorkItems(anyMap(), eq(testLimit));
        
        // Verify operation tracking includes limit
        verify(mockMetadataService).startOperation(eq("list"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals(testLimit, params.get("limit"), "Operation tracking should include limit parameter");
    }
    
    @Test
    @DisplayName("Should track detailed operation results")
    void shouldTrackDetailedOperationResults() {
        // Arrange
        ListCommand command = new ListCommand(mockServiceManager);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify detailed operation completion tracking
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        Map<String, Object> operationResult = resultCaptor.getValue();
        
        // Check required result fields
        assertTrue(operationResult.containsKey("count"), "Result should include count field");
        assertTrue(operationResult.containsKey("displayed"), "Result should include displayed field");
        assertTrue(operationResult.containsKey("criteria"), "Result should include criteria field");
        
        // Verify values
        assertEquals(testWorkItems.size(), operationResult.get("count"), 
                "Count should match test items size");
        assertEquals(Math.min(testWorkItems.size(), 100), operationResult.get("displayed"), 
                "Displayed should be min of items size and limit");
    }
}