package org.rinna.cli.component;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.command.FindCommand;
import org.rinna.cli.service.*;
import org.rinna.domain.model.*;

/**
 * Component tests for the FindCommand, focusing on integration with 
 * dependent services and operation tracking.
 */
public class FindCommandComponentTest {

    private AutoCloseable closeable;
    private ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private PrintStream originalOut = System.out;
    private FindCommand findCommand;
    
    @Mock
    private ServiceManager mockServiceManager;
    
    @Mock
    private ItemService mockItemService;
    
    @Mock
    private MetadataService mockMetadataService;
    
    @Mock
    private AuthorizationService mockAuthorizationService;
    
    @Mock
    private ConfigurationService mockConfigurationService;
    
    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        System.setOut(new PrintStream(outContent));
        
        // Standard mocking setup
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getAuthorizationService()).thenReturn(mockAuthorizationService);
        when(mockServiceManager.getConfigurationService()).thenReturn(mockConfigurationService);
        
        // Authentication
        when(mockAuthorizationService.isAuthenticated()).thenReturn(true);
        when(mockAuthorizationService.hasPermission(anyString())).thenReturn(true);
        
        // Common configuration
        when(mockConfigurationService.getStringValue(eq("output.format"), anyString())).thenReturn("text");
        
        findCommand = new FindCommand(mockServiceManager);
    }
    
    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        closeable.close();
    }
    
    @Test
    @DisplayName("Should properly integrate with ItemService for basic search")
    void shouldIntegrateWithItemService() {
        // Sample work items
        List<WorkItem> mockItems = createSampleWorkItems();
        when(mockItemService.findItems(any())).thenReturn(mockItems);
        
        // Execute command
        findCommand.setNamePattern("Test");
        int exitCode = findCommand.call();
        
        // Verify
        assertEquals(0, exitCode);
        verify(mockItemService, times(1)).findItems(any());
        verify(mockMetadataService, times(1)).recordOperation(eq("command.find"), anyString(), any());
        
        String output = outContent.toString();
        assertTrue(output.contains("Found 3 work items"));
        assertTrue(output.contains("Test Task"));
        assertTrue(output.contains("Test Bug"));
    }
    
    @ParameterizedTest
    @CsvSource({
        "TASK, 1",
        "BUG, 1", 
        "FEATURE, 1"
    })
    @DisplayName("Should filter by different work item types")
    void shouldFilterByWorkItemTypes(String type, int expectedCount) {
        // Sample work items
        List<WorkItem> mockItems = createSampleWorkItems();
        when(mockItemService.findItems(any())).thenReturn(mockItems);
        
        // Execute command
        findCommand.setType(type);
        int exitCode = findCommand.call();
        
        // Verify
        assertEquals(0, exitCode);
        verify(mockItemService, times(1)).findItems(any());
        verify(mockMetadataService, times(1)).recordOperation(eq("command.find"), anyString(), any());
        
        String output = outContent.toString();
        assertTrue(output.contains("Found " + expectedCount + " work"));
    }
    
    @Test
    @DisplayName("Should filter by date criteria correctly")
    void shouldFilterByDateCriteria() {
        // Sample work items
        List<WorkItem> mockItems = createSampleWorkItems();
        when(mockItemService.findItems(any())).thenReturn(mockItems);
        
        // Execute command
        findCommand.setCreatedAfter("2023-10-01");
        int exitCode = findCommand.call();
        
        // Verify
        assertEquals(0, exitCode);
        verify(mockItemService, times(1)).findItems(any());
        verify(mockMetadataService, times(1)).recordOperation(eq("command.find"), anyString(), any());
    }
    
    @Test
    @DisplayName("Should display results in JSON format when requested")
    void shouldDisplayResultsInJsonFormat() {
        // Sample work items
        List<WorkItem> mockItems = createSampleWorkItems();
        when(mockItemService.findItems(any())).thenReturn(mockItems);
        
        // Execute command
        findCommand.setJsonOutput(true);
        int exitCode = findCommand.call();
        
        // Verify
        assertEquals(0, exitCode);
        verify(mockItemService, times(1)).findItems(any());
        verify(mockMetadataService, times(1)).recordOperation(eq("command.find"), anyString(), any());
        
        String output = outContent.toString();
        // Basic JSON validation
        assertTrue(output.contains("[") && output.contains("]"), "Output should be in JSON format");
        assertTrue(output.contains("\"id\"") && output.contains("\"title\""), "JSON should contain expected fields");
    }
    
    @Test
    @DisplayName("Should respect count only display option")
    void shouldRespectCountOnlyOption() {
        // Sample work items
        List<WorkItem> mockItems = createSampleWorkItems();
        when(mockItemService.findItems(any())).thenReturn(mockItems);
        
        // Execute command
        findCommand.setCountOnly(true);
        int exitCode = findCommand.call();
        
        // Verify
        assertEquals(0, exitCode);
        verify(mockItemService, times(1)).findItems(any());
        verify(mockMetadataService, times(1)).recordOperation(eq("command.find"), anyString(), any());
        
        String output = outContent.toString();
        assertTrue(output.contains("Found 3 work item"));
        assertFalse(output.contains("State:"), "Output should not contain details in count-only mode");
        assertFalse(output.contains("Title:"), "Output should not contain details in count-only mode");
    }
    
    @Test
    @DisplayName("Should handle no matched items gracefully")
    void shouldHandleNoMatchedItems() {
        // Empty result
        when(mockItemService.findItems(any())).thenReturn(new ArrayList<>());
        
        // Execute command
        findCommand.setNamePattern("NonExistent");
        int exitCode = findCommand.call();
        
        // Verify
        assertEquals(0, exitCode);
        verify(mockItemService, times(1)).findItems(any());
        verify(mockMetadataService, times(1)).recordOperation(eq("command.find"), anyString(), any());
        
        String output = outContent.toString();
        assertTrue(output.contains("No work items found"));
    }
    
    @Test
    @DisplayName("Should not track operation when displaying help")
    void shouldNotTrackOperationWhenDisplayingHelp() {
        // Execute command
        findCommand.setHelp(true);
        int exitCode = findCommand.call();
        
        // Verify
        assertEquals(0, exitCode);
        verify(mockItemService, never()).findItems(any());
        verify(mockMetadataService, never()).recordOperation(anyString(), anyString(), any());
        
        String output = outContent.toString();
        assertTrue(output.contains("Usage:"));
        assertTrue(output.contains("Options:"));
    }
    
    @Test
    @DisplayName("Should fail gracefully with invalid date format")
    void shouldFailGracefullyWithInvalidDateFormat() {
        // Execute command
        findCommand.setCreatedAfter("not-a-date");
        int exitCode = findCommand.call();
        
        // Verify
        assertNotEquals(0, exitCode);
        verify(mockItemService, never()).findItems(any());
        verify(mockMetadataService, never()).recordOperation(anyString(), anyString(), any());
        
        String output = outContent.toString();
        assertTrue(output.contains("Invalid date format") || output.contains("format"));
    }
    
    @Test
    @DisplayName("Should fail gracefully with invalid priority")
    void shouldFailGracefullyWithInvalidPriority() {
        // Execute command
        findCommand.setPriority("ULTRA_CRITICAL");
        int exitCode = findCommand.call();
        
        // Verify
        assertNotEquals(0, exitCode);
        verify(mockItemService, never()).findItems(any());
        verify(mockMetadataService, never()).recordOperation(anyString(), anyString(), any());
        
        String output = outContent.toString();
        assertTrue(output.contains("Invalid priority") || output.contains("priority"));
    }
    
    // Helper method to create sample work items
    private List<WorkItem> createSampleWorkItems() {
        List<WorkItem> items = new ArrayList<>();
        
        DefaultWorkItem task = new DefaultWorkItem("WI-101", "Test Task");
        task.setType(WorkItemType.TASK);
        task.setState(WorkflowState.IN_PROGRESS);
        task.setPriority(Priority.MEDIUM);
        task.setAssignee("user1");
        task.setCreatedAt(Instant.now().minus(5, ChronoUnit.DAYS));
        task.setUpdatedAt(Instant.now().minus(2, ChronoUnit.DAYS));
        
        DefaultWorkItem bug = new DefaultWorkItem("WI-102", "Test Bug");
        bug.setType(WorkItemType.BUG);
        bug.setState(WorkflowState.OPEN);
        bug.setPriority(Priority.HIGH);
        bug.setAssignee("user2");
        bug.setCreatedAt(Instant.now().minus(3, ChronoUnit.DAYS));
        bug.setUpdatedAt(Instant.now().minus(1, ChronoUnit.DAYS));
        
        DefaultWorkItem feature = new DefaultWorkItem("WI-103", "Test Feature");
        feature.setType(WorkItemType.FEATURE);
        feature.setState(WorkflowState.CLOSED);
        feature.setPriority(Priority.LOW);
        feature.setAssignee("user1");
        feature.setCreatedAt(Instant.now().minus(10, ChronoUnit.DAYS));
        feature.setUpdatedAt(Instant.now());
        
        items.add(task);
        items.add(bug);
        items.add(feature);
        
        return items;
    }
}