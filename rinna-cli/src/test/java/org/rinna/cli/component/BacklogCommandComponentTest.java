package org.rinna.cli.component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.command.BacklogCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockBacklogService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Component tests for BacklogCommand.
 * These tests verify the integration with services and proper operation tracking.
 */
public class BacklogCommandComponentTest {
    
    // Mock services
    private ServiceManager mockServiceManager;
    private MetadataService mockMetadataService;
    private MockBacklogService mockBacklogService;
    private MockItemService mockItemService;
    
    // Capture the standard output and error
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    // Mock static objects
    private MockedStatic<ServiceManager> mockStaticServiceManager;
    private MockedStatic<OutputFormatter> mockStaticOutputFormatter;
    
    // Operation tracking
    private ArgumentCaptor<String> operationNameCaptor;
    private ArgumentCaptor<String> operationActionCaptor;
    private ArgumentCaptor<Map<String, Object>> operationParamsCaptor;
    
    @BeforeEach
    public void setUp() {
        // Redirect stdout and stderr for capturing output
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Initialize mocks
        mockServiceManager = mock(ServiceManager.class);
        mockMetadataService = mock(MetadataService.class);
        mockBacklogService = mock(MockBacklogService.class);
        mockItemService = mock(MockItemService.class);
        
        // Configure mocks
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getMockBacklogService()).thenReturn(mockBacklogService);
        when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);
        
        // Setup argument captors
        operationNameCaptor = ArgumentCaptor.forClass(String.class);
        operationActionCaptor = ArgumentCaptor.forClass(String.class);
        operationParamsCaptor = ArgumentCaptor.forClass(Map.class);
        
        // Mock static objects
        mockStaticServiceManager = Mockito.mockStatic(ServiceManager.class);
        mockStaticServiceManager.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
        
        mockStaticOutputFormatter = Mockito.mockStatic(OutputFormatter.class);
        when(OutputFormatter.toJson(any(Map.class))).thenReturn("{ \"mock\": \"json\" }");
        
        // Mock operation tracking
        when(mockMetadataService.startOperation(
                operationNameCaptor.capture(),
                operationActionCaptor.capture(),
                operationParamsCaptor.capture()
        )).thenReturn("op-123");
    }
    
    @AfterEach
    public void tearDown() {
        // Restore original stdout and stderr
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Close static mocks
        if (mockStaticServiceManager != null) {
            mockStaticServiceManager.close();
        }
        if (mockStaticOutputFormatter != null) {
            mockStaticOutputFormatter.close();
        }
    }
    
    /**
     * Test listing backlog items.
     */
    @Test
    public void testListBacklog() {
        // Create sample backlog items
        List<WorkItem> backlogItems = createSampleBacklogItems();
        
        // Mock the backlog service to return these items
        when(mockBacklogService.getBacklogItems()).thenReturn(backlogItems);
        
        // Create and configure the command
        BacklogCommand command = new BacklogCommand(mockServiceManager);
        command.setAction("list");
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result, "Command should succeed with exit code 0");
        
        // Verify the output
        String output = outContent.toString();
        assertTrue(output.contains("Backlog Items:"), 
                "Output should contain 'Backlog Items:' but was:\n" + output);
        assertTrue(output.contains("WI-123"), 
                "Output should contain 'WI-123' but was:\n" + output);
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("backlog"), eq("BACKLOG"), anyMap());
        verify(mockMetadataService, atLeastOnce()).completeOperation(anyString(), anyMap());
        
        // Verify parameters
        Map<String, Object> params = operationParamsCaptor.getValue();
        assertEquals("list", params.get("action"), "Action parameter should be 'list'");
    }
    
    /**
     * Test listing backlog items with JSON output.
     */
    @Test
    public void testListBacklogWithJsonOutput() {
        // Create sample backlog items
        List<WorkItem> backlogItems = createSampleBacklogItems();
        
        // Mock the backlog service to return these items
        when(mockBacklogService.getBacklogItems()).thenReturn(backlogItems);
        
        // Create and configure the command
        BacklogCommand command = new BacklogCommand(mockServiceManager);
        command.setAction("list");
        command.setFormat("json");
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result, "Command should succeed with exit code 0");
        
        // Verify JSON output was used
        verify(mockStaticOutputFormatter, atLeastOnce())
                .when(() -> OutputFormatter.toJson(any(Map.class)));
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("backlog"), eq("BACKLOG"), anyMap());
        verify(mockMetadataService, atLeastOnce()).completeOperation(anyString(), anyMap());
        
        // Verify parameters
        Map<String, Object> params = operationParamsCaptor.getValue();
        assertEquals("list", params.get("action"), "Action parameter should be 'list'");
        assertEquals("json", params.get("format"), "Format parameter should be 'json'");
    }
    
    /**
     * Test adding an item to the backlog.
     */
    @Test
    public void testAddToBacklog() {
        // Create a sample work item
        WorkItem item = createSampleWorkItem("WI-123");
        
        // Mock the item service to return this item
        when(mockItemService.getItem("WI-123")).thenReturn(item);
        
        // Mock successful addition to backlog
        when(mockBacklogService.addToBacklog(any(UUID.class))).thenReturn(true);
        
        // Create and configure the command
        BacklogCommand command = new BacklogCommand(mockServiceManager);
        command.setAction("add");
        command.setItemId("WI-123");
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result, "Command should succeed with exit code 0");
        
        // Verify the output
        String output = outContent.toString();
        assertTrue(output.contains("added to backlog successfully"), 
                "Output should indicate success but was:\n" + output);
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("backlog"), eq("BACKLOG"), anyMap());
        verify(mockMetadataService, atLeastOnce()).completeOperation(anyString(), anyMap());
        
        // Verify parameters
        Map<String, Object> params = operationParamsCaptor.getValue();
        assertEquals("add", params.get("action"), "Action parameter should be 'add'");
        assertEquals("WI-123", params.get("item_id"), "Item ID parameter should be correct");
    }
    
    /**
     * Test adding an item to the backlog with failure.
     */
    @Test
    public void testAddToBacklogFailure() {
        // Create a sample work item
        WorkItem item = createSampleWorkItem("WI-123");
        
        // Mock the item service to return this item
        when(mockItemService.getItem("WI-123")).thenReturn(item);
        
        // Mock failed addition to backlog
        when(mockBacklogService.addToBacklog(any(UUID.class))).thenReturn(false);
        
        // Create and configure the command
        BacklogCommand command = new BacklogCommand(mockServiceManager);
        command.setAction("add");
        command.setItemId("WI-123");
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(1, result, "Command should fail with exit code 1");
        
        // Verify the error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Failed to add item"), 
                "Error output should indicate failure but was:\n" + errorOutput);
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("backlog"), eq("BACKLOG"), anyMap());
        verify(mockMetadataService, atLeastOnce()).failOperation(anyString(), any(Exception.class));
    }
    
    /**
     * Test adding an item to the backlog without providing an ID.
     */
    @Test
    public void testAddToBacklogWithoutID() {
        // Create and configure the command
        BacklogCommand command = new BacklogCommand(mockServiceManager);
        command.setAction("add");
        // Deliberately omit setting the item ID
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(1, result, "Command should fail with exit code 1");
        
        // Verify the error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Item ID is required"), 
                "Error output should indicate missing ID but was:\n" + errorOutput);
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("backlog"), eq("BACKLOG"), anyMap());
        verify(mockMetadataService, atLeastOnce()).failOperation(anyString(), any(Exception.class));
    }
    
    /**
     * Test prioritizing a backlog item.
     */
    @Test
    public void testPrioritizeBacklog() {
        // Create a sample work item
        WorkItem item = createSampleWorkItem("WI-123");
        
        // Mock the item service to return this item
        when(mockItemService.getItem("WI-123")).thenReturn(item);
        
        // Mock successful priority setting
        when(mockBacklogService.setPriority(eq("WI-123"), any(Priority.class))).thenReturn(true);
        
        // Create and configure the command
        BacklogCommand command = new BacklogCommand(mockServiceManager);
        command.setAction("prioritize");
        command.setItemId("WI-123");
        command.setPriority(Priority.HIGH);
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result, "Command should succeed with exit code 0");
        
        // Verify the output
        String output = outContent.toString();
        assertTrue(output.contains("Updated priority"), 
                "Output should indicate success but was:\n" + output);
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("backlog"), eq("BACKLOG"), anyMap());
        verify(mockMetadataService, atLeastOnce()).completeOperation(anyString(), anyMap());
        
        // Verify parameters
        Map<String, Object> params = operationParamsCaptor.getValue();
        assertEquals("prioritize", params.get("action"), "Action parameter should be 'prioritize'");
        assertEquals("WI-123", params.get("item_id"), "Item ID parameter should be correct");
        assertEquals("HIGH", params.get("priority"), "Priority parameter should be correct");
    }
    
    /**
     * Test prioritizing a backlog item without providing a priority.
     */
    @Test
    public void testPrioritizeBacklogWithoutPriority() {
        // Create a sample work item
        WorkItem item = createSampleWorkItem("WI-123");
        
        // Mock the item service to return this item
        when(mockItemService.getItem("WI-123")).thenReturn(item);
        
        // Create and configure the command
        BacklogCommand command = new BacklogCommand(mockServiceManager);
        command.setAction("prioritize");
        command.setItemId("WI-123");
        // Deliberately omit setting the priority
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(1, result, "Command should fail with exit code 1");
        
        // Verify the error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Priority is required"), 
                "Error output should indicate missing priority but was:\n" + errorOutput);
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("backlog"), eq("BACKLOG"), anyMap());
        verify(mockMetadataService, atLeastOnce()).failOperation(anyString(), any(Exception.class));
    }
    
    /**
     * Test removing an item from the backlog.
     */
    @Test
    public void testRemoveFromBacklog() {
        // Create a sample work item
        WorkItem item = createSampleWorkItem("WI-123");
        
        // Mock the item service to return this item
        when(mockItemService.getItem("WI-123")).thenReturn(item);
        
        // Mock successful removal from backlog
        when(mockBacklogService.removeFromBacklog("WI-123")).thenReturn(true);
        
        // Create and configure the command
        BacklogCommand command = new BacklogCommand(mockServiceManager);
        command.setAction("remove");
        command.setItemId("WI-123");
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(0, result, "Command should succeed with exit code 0");
        
        // Verify the output
        String output = outContent.toString();
        assertTrue(output.contains("Removed"), 
                "Output should indicate success but was:\n" + output);
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("backlog"), eq("BACKLOG"), anyMap());
        verify(mockMetadataService, atLeastOnce()).completeOperation(anyString(), anyMap());
        
        // Verify parameters
        Map<String, Object> params = operationParamsCaptor.getValue();
        assertEquals("remove", params.get("action"), "Action parameter should be 'remove'");
        assertEquals("WI-123", params.get("item_id"), "Item ID parameter should be correct");
    }
    
    /**
     * Test removing an item from the backlog with failure.
     */
    @Test
    public void testRemoveFromBacklogFailure() {
        // Create a sample work item
        WorkItem item = createSampleWorkItem("WI-123");
        
        // Mock the item service to return this item
        when(mockItemService.getItem("WI-123")).thenReturn(item);
        
        // Mock failed removal from backlog
        when(mockBacklogService.removeFromBacklog("WI-123")).thenReturn(false);
        
        // Create and configure the command
        BacklogCommand command = new BacklogCommand(mockServiceManager);
        command.setAction("remove");
        command.setItemId("WI-123");
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(1, result, "Command should fail with exit code 1");
        
        // Verify the error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Failed to remove item"), 
                "Error output should indicate failure but was:\n" + errorOutput);
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("backlog"), eq("BACKLOG"), anyMap());
        verify(mockMetadataService, atLeastOnce()).failOperation(anyString(), any(Exception.class));
    }
    
    /**
     * Test using an unknown action.
     */
    @Test
    public void testUnknownAction() {
        // Create and configure the command
        BacklogCommand command = new BacklogCommand(mockServiceManager);
        command.setAction("unknown-action");
        
        // Execute the command
        Integer result = command.call();
        
        // Verify the result
        assertEquals(1, result, "Command should fail with exit code 1");
        
        // Verify the error output
        String errorOutput = errContent.toString();
        assertTrue(errorOutput.contains("Unknown backlog action"), 
                "Error output should indicate unknown action but was:\n" + errorOutput);
        assertTrue(errorOutput.contains("Valid actions"), 
                "Error output should list valid actions but was:\n" + errorOutput);
        
        // Verify operation tracking
        verify(mockMetadataService, atLeastOnce()).startOperation(eq("backlog"), eq("BACKLOG"), anyMap());
        verify(mockMetadataService, atLeastOnce()).failOperation(anyString(), any(Exception.class));
    }
    
    /**
     * Helper method to create sample backlog items.
     * 
     * @return a list of sample backlog items
     */
    private List<WorkItem> createSampleBacklogItems() {
        List<WorkItem> items = new ArrayList<>();
        
        WorkItem item1 = new WorkItem();
        item1.setId("WI-123");
        item1.setTitle("Implement user authentication");
        item1.setType(WorkItemType.FEATURE);
        item1.setPriority(Priority.HIGH);
        item1.setState(WorkflowState.OPEN);
        items.add(item1);
        
        WorkItem item2 = new WorkItem();
        item2.setId("WI-124");
        item2.setTitle("Fix sorting in the reports page");
        item2.setType(WorkItemType.BUG);
        item2.setPriority(Priority.MEDIUM);
        item2.setState(WorkflowState.IN_PROGRESS);
        items.add(item2);
        
        return items;
    }
    
    /**
     * Helper method to create a sample work item.
     * 
     * @param id the item ID
     * @return a sample work item
     */
    private WorkItem createSampleWorkItem(String id) {
        WorkItem item = new WorkItem();
        item.setId(id);
        item.setTitle("Sample Work Item");
        item.setType(WorkItemType.TASK);
        item.setPriority(Priority.MEDIUM);
        item.setState(WorkflowState.OPEN);
        return item;
    }
}