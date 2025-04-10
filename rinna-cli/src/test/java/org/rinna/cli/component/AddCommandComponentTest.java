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
import org.rinna.cli.command.AddCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Component test for the AddCommand class.
 * This test focuses on the integration of AddCommand with the services it depends on.
 */
@DisplayName("AddCommand Component Tests")
public class AddCommandComponentTest {

    private ServiceManager mockServiceManager;
    private ItemService mockItemService;
    private ConfigurationService mockConfigService;
    private MetadataService mockMetadataService;
    
    private ByteArrayOutputStream outContent;
    private ByteArrayOutputStream errContent;
    private PrintStream originalOut;
    private PrintStream originalErr;
    
    private UUID testItemId;
    private WorkItem createdWorkItem;
    private ArgumentCaptor<Map<String, Object>> paramsCaptor;
    private ArgumentCaptor<Object> resultCaptor;
    private ArgumentCaptor<WorkItem> workItemCaptor;
    
    private MockedStatic<ServiceManager> mockedStaticServiceManager;
    private MockedStatic<OutputFormatter> mockedStaticOutputFormatter;
    
    @BeforeEach
    void setUp() {
        // Set up mocks
        mockServiceManager = mock(ServiceManager.class);
        mockItemService = mock(ItemService.class);
        mockConfigService = mock(ConfigurationService.class);
        mockMetadataService = mock(MetadataService.class);
        
        // Set up static mocks
        mockedStaticServiceManager = Mockito.mockStatic(ServiceManager.class);
        mockedStaticServiceManager.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        
        mockedStaticOutputFormatter = Mockito.mockStatic(OutputFormatter.class);
        mockedStaticOutputFormatter.when(() -> OutputFormatter.toJson(any(Map.class), anyBoolean()))
            .thenAnswer(invocation -> {
                Map<String, Object> map = (Map<String, Object>) invocation.getArgument(0);
                return "{\"result\":\"" + map.get("result") + "\",\"item_id\":\"" + map.get("item_id") + "\",\"operationId\":\"" + map.get("operationId") + "\"}";
            });
        
        // Set up service manager
        when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        
        // Set up metadata service
        when(mockMetadataService.startOperation(anyString(), anyString(), any()))
            .thenReturn("test-operation-id");
        
        // Set up config service defaults
        when(mockConfigService.getCurrentUser()).thenReturn("testuser");
        when(mockConfigService.getCurrentProject()).thenReturn("testproject");
        
        // Set up argument captors
        paramsCaptor = ArgumentCaptor.forClass(Map.class);
        resultCaptor = ArgumentCaptor.forClass(Object.class);
        workItemCaptor = ArgumentCaptor.forClass(WorkItem.class);
        
        // Redirect stdout and stderr for verification
        originalOut = System.out;
        originalErr = System.err;
        outContent = new ByteArrayOutputStream();
        errContent = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Initialize test data
        testItemId = UUID.randomUUID();
        createdWorkItem = createTestWorkItem();
        
        // Default ItemService behavior
        when(mockItemService.createWorkItem(any(WorkItem.class))).thenReturn(createdWorkItem);
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
    
    private WorkItem createTestWorkItem() {
        WorkItem item = new WorkItem();
        item.setId(testItemId.toString());
        item.setTitle("Test Work Item");
        item.setDescription("Test description");
        item.setType(WorkItemType.TASK);
        item.setPriority(Priority.MEDIUM);
        item.setStatus(WorkflowState.CREATED);
        item.setAssignee("testuser");
        item.setReporter("testuser");
        item.setProject("testproject");
        item.setCreated(LocalDateTime.now());
        item.setUpdated(LocalDateTime.now());
        
        return item;
    }
    
    @Test
    @DisplayName("Should integrate with ItemService to create a work item")
    void shouldIntegrateWithItemServiceToCreateWorkItem() {
        // Arrange
        AddCommand command = new AddCommand(mockServiceManager);
        command.setTitle("Test Title");
        command.setDescription("Test Description");
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify integration with ItemService
        verify(mockItemService).createWorkItem(workItemCaptor.capture());
        WorkItem capturedItem = workItemCaptor.getValue();
        assertEquals("Test Title", capturedItem.getTitle(), "Title should be set correctly");
        assertEquals("Test Description", capturedItem.getDescription(), "Description should be set correctly");
        
        // Verify output contains expected elements
        String output = outContent.toString();
        assertTrue(output.contains("Created work item:"), "Output should indicate item was created");
        assertTrue(output.contains("Title: " + createdWorkItem.getTitle()), "Output should include work item title");
        
        // Verify metadata tracking
        verify(mockMetadataService).startOperation(eq("add"), eq("CREATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("Test Title", params.get("title"), "Operation tracking should include title");
        
        verify(mockMetadataService).completeOperation(anyString(), any());
    }
    
    @Test
    @DisplayName("Should set all work item fields from command parameters")
    void shouldSetAllWorkItemFieldsFromCommandParameters() {
        // Arrange
        AddCommand command = new AddCommand(mockServiceManager);
        command.setTitle("Complex Item");
        command.setDescription("Detailed description");
        command.setType(WorkItemType.BUG);
        command.setPriority(Priority.HIGH);
        command.setAssignee("john.doe");
        command.setProject("frontend");
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify all fields were set on the work item
        verify(mockItemService).createWorkItem(workItemCaptor.capture());
        WorkItem capturedItem = workItemCaptor.getValue();
        assertEquals("Complex Item", capturedItem.getTitle(), "Title should be set correctly");
        assertEquals("Detailed description", capturedItem.getDescription(), "Description should be set correctly");
        assertEquals(WorkItemType.BUG, capturedItem.getType(), "Type should be set correctly");
        assertEquals(Priority.HIGH, capturedItem.getPriority(), "Priority should be set correctly");
        assertEquals("john.doe", capturedItem.getAssignee(), "Assignee should be set correctly");
        assertEquals("frontend", capturedItem.getProject(), "Project should be set correctly");
        
        // Verify metadata tracking includes all parameters
        verify(mockMetadataService).startOperation(eq("add"), eq("CREATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("Complex Item", params.get("title"), "Operation tracking should include title");
        assertEquals("BUG", params.get("type"), "Operation tracking should include type");
        assertEquals("HIGH", params.get("priority"), "Operation tracking should include priority");
        assertEquals("john.doe", params.get("assignee"), "Operation tracking should include assignee");
        assertEquals("frontend", params.get("project"), "Operation tracking should include project");
    }
    
    @Test
    @DisplayName("Should use current user and project from configuration when not specified")
    void shouldUseCurrentUserAndProjectFromConfigurationWhenNotSpecified() {
        // Arrange
        AddCommand command = new AddCommand(mockServiceManager);
        command.setTitle("Default User Item");
        
        // Set up config service to return specific values
        when(mockConfigService.getCurrentUser()).thenReturn("current.user");
        when(mockConfigService.getCurrentProject()).thenReturn("current.project");
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify defaults were applied from configuration
        verify(mockItemService).createWorkItem(workItemCaptor.capture());
        WorkItem capturedItem = workItemCaptor.getValue();
        assertEquals("current.user", capturedItem.getAssignee(), "Assignee should default to current user");
        assertEquals("current.user", capturedItem.getReporter(), "Reporter should be set to current user");
        assertEquals("current.project", capturedItem.getProject(), "Project should default to current project");
    }
    
    @Test
    @DisplayName("Should validate that title is required")
    void shouldValidateThatTitleIsRequired() {
        // Arrange
        AddCommand command = new AddCommand(mockServiceManager);
        // Do not set a title
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(1, result, "Command should fail when title is missing");
        
        // Verify error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Title is required"), 
                "Error output should indicate title is required");
        
        // Verify metadata tracking includes failure
        verify(mockMetadataService).failOperation(anyString(), any(IllegalArgumentException.class));
    }
    
    @Test
    @DisplayName("Should support JSON output format")
    void shouldSupportJsonOutputFormat() {
        // Arrange
        AddCommand command = new AddCommand(mockServiceManager);
        command.setTitle("JSON Item");
        command.setFormat("json");
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify OutputFormatter integration
        mockedStaticOutputFormatter.verify(() -> 
            OutputFormatter.toJson(any(Map.class), eq(false)));
        
        // Verify output is JSON formatted
        String output = outContent.toString();
        assertTrue(output.contains("{"), "Output should be JSON formatted");
        assertTrue(output.contains("}"), "Output should be JSON formatted");
        assertTrue(output.contains("\"result\":\"success\""), "JSON should indicate success");
        assertTrue(output.contains("\"item_id\":\"" + createdWorkItem.getId() + "\""), 
                "JSON should include the created item ID");
        
        // Verify metadata tracking includes format parameter
        verify(mockMetadataService).startOperation(eq("add"), eq("CREATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals("json", params.get("format"), "Operation tracking should include format parameter");
    }
    
    @Test
    @DisplayName("Should support verbose output mode")
    void shouldSupportVerboseOutputMode() {
        // Arrange
        AddCommand command = new AddCommand(mockServiceManager);
        command.setTitle("Verbose Item");
        command.setDescription("Detailed description for verbose output");
        command.setVerbose(true);
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify verbose output includes additional details
        String output = outContent.toString();
        assertTrue(output.contains("Description:"), "Verbose output should include description heading");
        assertTrue(output.contains("Detailed description for verbose output"), 
                "Verbose output should include the full description");
        assertTrue(output.contains("Reporter:"), "Verbose output should include reporter heading");
        assertTrue(output.contains("Created:"), "Verbose output should include creation date");
        assertTrue(output.contains("Operation ID:"), "Verbose output should include operation ID");
        
        // Verify metadata tracking includes verbose parameter
        verify(mockMetadataService).startOperation(eq("add"), eq("CREATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        assertEquals(true, params.get("verbose"), "Operation tracking should include verbose parameter");
    }
    
    @Test
    @DisplayName("Should handle service exceptions gracefully")
    void shouldHandleServiceExceptionsGracefully() {
        // Arrange
        AddCommand command = new AddCommand(mockServiceManager);
        command.setTitle("Error Item");
        
        // Set up ItemService to throw an exception
        when(mockItemService.createWorkItem(any(WorkItem.class)))
            .thenThrow(new RuntimeException("Test service exception"));
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(1, result, "Command should fail when service throws exception");
        
        // Verify error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Error creating work item"), 
                "Error output should indicate the operation that failed");
        assertTrue(errorOutput.contains("Test service exception"), 
                "Error output should include the exception message");
        
        // Verify metadata tracking includes exception
        verify(mockMetadataService).failOperation(anyString(), any(Exception.class));
    }
    
    @Test
    @DisplayName("Should truncate long descriptions in operation tracking")
    void shouldTruncateLongDescriptionsInOperationTracking() {
        // Arrange
        AddCommand command = new AddCommand(mockServiceManager);
        command.setTitle("Long Description Item");
        
        // Create a very long description (over 50 characters)
        StringBuilder longDesc = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            longDesc.append("Long description text. ");
        }
        command.setDescription(longDesc.toString());
        
        // Act
        int result = command.call();
        
        // Assert
        assertEquals(0, result, "Command should execute successfully");
        
        // Verify description is truncated in operation tracking
        verify(mockMetadataService).startOperation(eq("add"), eq("CREATE"), paramsCaptor.capture());
        Map<String, Object> params = paramsCaptor.getValue();
        String trackedDescription = (String) params.get("description");
        assertTrue(trackedDescription.length() <= 50, 
                "Description should be truncated to 50 characters or less in operation tracking");
        assertTrue(trackedDescription.endsWith("..."), 
                "Truncated description should end with ellipsis");
    }
}