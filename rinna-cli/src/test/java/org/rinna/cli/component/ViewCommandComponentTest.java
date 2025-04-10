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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.ViewCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.ModelMapper;
import org.rinna.cli.util.OutputFormatter;

/**
 * Component test for the ViewCommand class.
 * This test focuses on the integration of ViewCommand with the services it depends on.
 */
@DisplayName("ViewCommand Component Tests")
public class ViewCommandComponentTest {

    private ServiceManager mockServiceManager;
    private ItemService mockItemService;
    private MetadataService mockMetadataService;
    
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    private UUID testItemId;
    private WorkItem testWorkItem;
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    private ArgumentCaptor<Map<String, Object>> resultCaptor;
    
    private MockedStatic<ServiceManager> mockedStaticServiceManager;
    private MockedStatic<ModelMapper> mockedStaticModelMapper;
    private MockedStatic<OutputFormatter> mockedStaticOutputFormatter;
    
    @BeforeEach
    void setUp() {
        // Set up mocks
        mockServiceManager = mock(ServiceManager.class);
        mockItemService = mock(ItemService.class);
        mockMetadataService = mock(MetadataService.class);
        
        // Set up static mocks
        mockedStaticServiceManager = Mockito.mockStatic(ServiceManager.class);
        mockedStaticServiceManager.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        
        mockedStaticModelMapper = Mockito.mockStatic(ModelMapper.class);
        
        mockedStaticOutputFormatter = Mockito.mockStatic(OutputFormatter.class);
        mockedStaticOutputFormatter.when(() -> OutputFormatter.toJson(any(WorkItem.class), anyBoolean()))
            .thenAnswer(invocation -> {
                WorkItem item = invocation.getArgument(0);
                return "{\"id\":\"" + item.getId() + "\",\"title\":\"" + item.getTitle() + "\"}";
            });
        
        // Set up service manager
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up metadata service
        when(mockMetadataService.startOperation(anyString(), anyString(), any())).thenReturn("operation-id");
        
        // Set up argument captors
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
        testItemId = UUID.randomUUID();
        testWorkItem = createTestWorkItem();
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
        if (mockedStaticModelMapper != null) {
            mockedStaticModelMapper.close();
        }
        if (mockedStaticOutputFormatter != null) {
            mockedStaticOutputFormatter.close();
        }
    }
    
    private WorkItem createTestWorkItem() {
        WorkItem item = new WorkItem();
        item.setId(testItemId.toString());
        item.setTitle("Fix login bug");
        item.setType(WorkItemType.BUG);
        item.setPriority(Priority.HIGH);
        item.setStatus(WorkflowState.IN_PROGRESS);
        item.setAssignee("alice");
        item.setDescription("A critical bug in the login system");
        item.setReporter("bob");
        item.setProject("UserAuth");
        item.setVersion("2.1.0");
        item.setCreated(LocalDateTime.now().minusDays(5));
        item.setUpdated(LocalDateTime.now().minusHours(12));
        item.setDueDate(LocalDateTime.now().plusDays(2));
        
        return item;
    }
    
    @Test
    @DisplayName("Should integrate with ItemService to retrieve work item")
    void shouldIntegrateWithItemServiceToRetrieveWorkItem() {
        // Arrange
        ViewCommand command = new ViewCommand(mockServiceManager);
        String testItemIdString = testItemId.toString();
        command.setId(testItemIdString);
        
        // Set up UUID parsing
        mockedStaticModelMapper.when(() -> ModelMapper.toUUID(testItemIdString)).thenReturn(testItemId);
        
        // Set up ItemService mock
        when(mockItemService.getItem(testItemIdString)).thenReturn(testWorkItem);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify integration with ItemService
        verify(mockItemService).getItem(testItemIdString);
        
        // Verify output contains expected elements
        String output = outContent.toString();
        assertTrue(output.contains("Work Item: " + testItemIdString), "Output should include work item ID");
        assertTrue(output.contains("Title: " + testWorkItem.getTitle()), "Output should include work item title");
        assertTrue(output.contains("Status: " + testWorkItem.getStatus()), "Output should include work item status");
        
        // Verify metadata tracking
        verify(mockMetadataService).startOperation(eq("view"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals(testItemIdString, params.get("item_id"), "Operation tracking should include item ID");
        
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        Map<String, Object> result2 = resultCaptor.getValue();
        assertEquals(testItemIdString, result2.get("item_id"), "Result should include item ID");
    }
    
    @Test
    @DisplayName("Should integrate with OutputFormatter for JSON output")
    void shouldIntegrateWithOutputFormatterForJsonOutput() {
        // Arrange
        ViewCommand command = new ViewCommand(mockServiceManager);
        String testItemIdString = testItemId.toString();
        command.setId(testItemIdString);
        command.setFormat("json");
        
        // Set up UUID parsing
        mockedStaticModelMapper.when(() -> ModelMapper.toUUID(testItemIdString)).thenReturn(testItemId);
        
        // Set up ItemService mock
        when(mockItemService.getItem(testItemIdString)).thenReturn(testWorkItem);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify OutputFormatter integration
        mockedStaticOutputFormatter.verify(() -> 
            OutputFormatter.toJson(eq(testWorkItem), eq(false)));
        
        // Verify output is JSON
        String output = outContent.toString();
        assertTrue(output.contains("{"), "Output should be JSON formatted");
        assertTrue(output.contains("}"), "Output should be JSON formatted");
        
        // Verify metadata tracking includes format
        verify(mockMetadataService).startOperation(eq("view"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("json", params.get("format"), "Operation tracking should include format parameter");
    }
    
    @Test
    @DisplayName("Should handle item service exceptions gracefully")
    void shouldHandleItemServiceExceptionsGracefully() {
        // Arrange
        ViewCommand command = new ViewCommand(mockServiceManager);
        String testItemIdString = testItemId.toString();
        command.setId(testItemIdString);
        
        // Set up UUID parsing
        mockedStaticModelMapper.when(() -> ModelMapper.toUUID(testItemIdString)).thenReturn(testItemId);
        
        // Set up ItemService mock to throw an exception
        when(mockItemService.getItem(testItemIdString)).thenThrow(new RuntimeException("Test exception"));
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(1, result, "Command should fail when item service throws exception");
        
        // Verify error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Error retrieving work item"), 
                "Error output should indicate problem retrieving work item");
        
        // Verify metadata tracking includes exception
        verify(mockMetadataService).failOperation(anyString(), any(Exception.class));
    }
    
    @Test
    @DisplayName("Should handle non-existent work item gracefully")
    void shouldHandleNonExistentWorkItemGracefully() {
        // Arrange
        ViewCommand command = new ViewCommand(mockServiceManager);
        String testItemIdString = testItemId.toString();
        command.setId(testItemIdString);
        
        // Set up UUID parsing
        mockedStaticModelMapper.when(() -> ModelMapper.toUUID(testItemIdString)).thenReturn(testItemId);
        
        // Set up ItemService mock to return null (item not found)
        when(mockItemService.getItem(testItemIdString)).thenReturn(null);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(1, result, "Command should fail when work item is not found");
        
        // Verify error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Work item not found"), 
                "Error output should indicate work item not found");
        
        // Verify metadata tracking includes failure with appropriate exception
        verify(mockMetadataService).failOperation(anyString(), any(IllegalArgumentException.class));
    }
    
    @Test
    @DisplayName("Should handle invalid UUID format gracefully")
    void shouldHandleInvalidUuidFormatGracefully() {
        // Arrange
        ViewCommand command = new ViewCommand(mockServiceManager);
        String invalidId = "invalid-id";
        command.setId(invalidId);
        
        // Set up UUID parsing to throw an exception
        mockedStaticModelMapper.when(() -> ModelMapper.toUUID(invalidId))
            .thenThrow(new IllegalArgumentException("Invalid UUID format"));
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(1, result, "Command should fail when item ID has invalid format");
        
        // Verify error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Invalid work item ID format"), 
                "Error output should indicate invalid item ID format");
        
        // Verify metadata tracking includes failure with appropriate exception
        verify(mockMetadataService).failOperation(anyString(), any(IllegalArgumentException.class));
    }
    
    @Test
    @DisplayName("Should include verbose details when requested")
    void shouldIncludeVerboseDetailsWhenRequested() {
        // Arrange
        ViewCommand command = new ViewCommand(mockServiceManager);
        String testItemIdString = testItemId.toString();
        command.setId(testItemIdString);
        command.setVerbose(true);
        
        // Set up UUID parsing
        mockedStaticModelMapper.when(() -> ModelMapper.toUUID(testItemIdString)).thenReturn(testItemId);
        
        // Set up ItemService mock
        when(mockItemService.getItem(testItemIdString)).thenReturn(testWorkItem);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify output contains verbose details
        String output = outContent.toString();
        assertTrue(output.contains("Description:"), "Output should include description heading");
        assertTrue(output.contains(testWorkItem.getDescription()), "Output should include work item description");
        assertTrue(output.contains("Reporter:"), "Output should include reporter heading");
        assertTrue(output.contains(testWorkItem.getReporter()), "Output should include reporter name");
        assertTrue(output.contains("Project:"), "Output should include project heading");
        assertTrue(output.contains(testWorkItem.getProject()), "Output should include project name");
        
        // Verify metadata tracking includes verbose parameter
        verify(mockMetadataService).startOperation(eq("view"), eq("READ"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals(true, params.get("verbose"), "Operation tracking should include verbose parameter");
    }
    
    @Test
    @DisplayName("Should track detailed operation results")
    void shouldTrackDetailedOperationResults() {
        // Arrange
        ViewCommand command = new ViewCommand(mockServiceManager);
        String testItemIdString = testItemId.toString();
        command.setId(testItemIdString);
        
        // Set up UUID parsing
        mockedStaticModelMapper.when(() -> ModelMapper.toUUID(testItemIdString)).thenReturn(testItemId);
        
        // Set up ItemService mock
        when(mockItemService.getItem(testItemIdString)).thenReturn(testWorkItem);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify detailed operation completion tracking
        verify(mockMetadataService).completeOperation(anyString(), resultCaptor.capture());
        Map<String, Object> operationResult = resultCaptor.getValue();
        
        // Check required result fields
        assertEquals(testItemIdString, operationResult.get("item_id"), "Result should include item ID");
        assertEquals(testWorkItem.getTitle(), operationResult.get("title"), "Result should include item title");
        assertEquals(testWorkItem.getStatus().toString(), operationResult.get("status"), "Result should include item status");
    }
}