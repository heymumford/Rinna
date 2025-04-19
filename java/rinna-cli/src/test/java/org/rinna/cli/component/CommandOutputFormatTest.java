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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.command.AddCommand;
import org.rinna.cli.command.ListCommand;
import org.rinna.cli.command.ViewCommand;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockSearchService;
import org.rinna.cli.service.ServiceManager;

/**
 * Component tests for CLI command output formatting capabilities.
 * These tests verify that command output is correctly formatted based on specified formatting options.
 */
@Tag("component")
@DisplayName("CLI Command Output Format Component Tests")
class CommandOutputFormatTest {

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    @Mock
    private MockItemService mockItemService;
    
    @Mock
    private MockSearchService mockSearchService;
    
    private AutoCloseable mocks;
    
    @BeforeEach
    void setUp() {
        // Initialize mocks
        mocks = MockitoAnnotations.openMocks(this);
        
        // Create fresh streams for each test to avoid cross-test contamination
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }
    
    @AfterEach
    void tearDown() throws Exception {
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Close mocks
        if (mocks != null) {
            mocks.close();
        }
    }
    
    @Nested
    @DisplayName("View Command Format Tests")
    class ViewCommandFormatTests {
        private WorkItem testItem;
        
        @BeforeEach
        void setUpViewTests() {
            // Create a test work item with comprehensive data
            testItem = new WorkItem();
            testItem.setId(UUID.randomUUID().toString());
            testItem.setTitle("Test Work Item with Format Options");
            testItem.setDescription("This is a detailed description for testing output formatting");
            testItem.setType(WorkItemType.FEATURE);
            testItem.setPriority(Priority.HIGH);
            testItem.setState(WorkflowState.IN_PROGRESS);
            testItem.setAssignee("user1");
            testItem.setReporter("user2");
            testItem.setCreated(LocalDateTime.now().minusDays(5));
            testItem.setUpdated(LocalDateTime.now().minusHours(2));
            
            // Set up mock to return the test item
            when(mockItemService.getItem(anyString())).thenReturn(testItem);
        }
        
        @Test
        @DisplayName("Should display work item in default text format")
        void shouldDisplayWorkItemInDefaultTextFormat() {
            try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);
                
                // Setup ViewCommand with default format (text)
                ViewCommand viewCmd = new ViewCommand();
                viewCmd.setId(testItem.getId());
                
                // Execute command
                int exitCode = viewCmd.call();
                
                // Verify service interaction
                verify(mockItemService).getItem(eq(testItem.getId()));
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");
                
                // Verify output format matches expected text format
                String output = outputStream.toString();
                assertTrue(output.contains("Work Item: " + testItem.getId()), "Output should contain item ID label");
                assertTrue(output.contains("Title: " + testItem.getTitle()), "Output should contain title label");
                assertTrue(output.contains("Status: " + testItem.getStatus()), "Output should contain status label");
                assertTrue(output.contains("Type: " + testItem.getType()), "Output should contain type label");
                assertTrue(output.contains("Priority: " + testItem.getPriority()), "Output should contain priority label");
                
                // Verify verbose information is NOT included by default
                assertFalse(output.contains("Description: "), "Output should not contain description by default");
                assertFalse(output.contains("Created: "), "Output should not contain creation date by default");
            }
        }
        
        @Test
        @DisplayName("Should display work item in verbose text format")
        void shouldDisplayWorkItemInVerboseTextFormat() {
            try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);
                
                // Setup ViewCommand with verbose flag enabled
                ViewCommand viewCmd = new ViewCommand();
                viewCmd.setId(testItem.getId());
                viewCmd.setVerbose(true);
                
                // Execute command
                int exitCode = viewCmd.call();
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");
                
                // Verify output includes verbose information
                String output = outputStream.toString();
                assertTrue(output.contains("Description: " + testItem.getDescription()), 
                    "Output should contain description in verbose mode");
                assertTrue(output.contains("Reporter: " + testItem.getReporter()), 
                    "Output should contain reporter in verbose mode");
                assertTrue(output.contains("Created: " + testItem.getCreated()), 
                    "Output should contain creation date in verbose mode");
                assertTrue(output.contains("Updated: " + testItem.getUpdated()), 
                    "Output should contain update date in verbose mode");
            }
        }
        
        @Test
        @DisplayName("Should display work item in JSON format")
        void shouldDisplayWorkItemInJsonFormat() {
            try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);
                
                // Setup ViewCommand with JSON format
                ViewCommand viewCmd = new ViewCommand();
                viewCmd.setId(testItem.getId());
                viewCmd.setFormat("json");
                
                // Execute command
                int exitCode = viewCmd.call();
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");
                
                // Verify output is in JSON format
                String output = outputStream.toString();
                assertTrue(output.contains("{"), "Output should start with opening JSON brace");
                assertTrue(output.contains("}"), "Output should end with closing JSON brace");
                
                // Check for JSON key-value pairs
                assertTrue(output.contains("\"id\": \"" + testItem.getId() + "\""), 
                    "Output should contain JSON formatted ID");
                assertTrue(output.contains("\"title\": \"" + testItem.getTitle() + "\""), 
                    "Output should contain JSON formatted title");
                assertTrue(output.contains("\"type\": \"" + testItem.getType() + "\""), 
                    "Output should contain JSON formatted type");
                assertTrue(output.contains("\"priority\": \"" + testItem.getPriority() + "\""), 
                    "Output should contain JSON formatted priority");
            }
        }
    }
    
    @Nested
    @DisplayName("List Command Format Tests")
    class ListCommandFormatTests {
        private List<WorkItem> testItems;
        
        @BeforeEach
        void setUpListTests() {
            // Create test work items
            WorkItem item1 = new WorkItem();
            item1.setId(UUID.randomUUID().toString());
            item1.setTitle("First Test Item");
            item1.setType(WorkItemType.TASK);
            item1.setPriority(Priority.MEDIUM);
            item1.setState(WorkflowState.READY);
            item1.setAssignee("user1");
            
            WorkItem item2 = new WorkItem();
            item2.setId(UUID.randomUUID().toString());
            item2.setTitle("Second Test Item with a Very Long Title That Should Be Truncated in the Output");
            item2.setType(WorkItemType.BUG);
            item2.setPriority(Priority.HIGH);
            item2.setState(WorkflowState.IN_PROGRESS);
            item2.setAssignee("user2");
            
            testItems = Arrays.asList(item1, item2);
            
            // Set up mock to return the test items for various search criteria
            when(mockSearchService.findWorkItems(anyMap(), anyInt())).thenReturn(testItems);
            
            // Mock the service lookups
            when(mockItemService.getAllItems()).thenReturn(testItems);
            when(mockItemService.getItemsByType(any(WorkItemType.class))).thenReturn(testItems);
            when(mockItemService.getItemsByState(any(WorkflowState.class))).thenReturn(testItems);
        }
        
        @Test
        @DisplayName("Should display work items in standard table format")
        void shouldDisplayWorkItemsInStandardTableFormat() {
            try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getSearchService()).thenReturn(mockSearchService);
                
                // Setup ListCommand
                ListCommand listCmd = new ListCommand();
                
                // Execute command
                int exitCode = listCmd.call();
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");
                
                // Verify output contains table header
                String output = outputStream.toString();
                assertTrue(output.contains("Work Items:"), "Output should contain table title");
                assertTrue(output.contains("ID"), "Output should contain ID column header");
                assertTrue(output.contains("TITLE"), "Output should contain TITLE column header");
                assertTrue(output.contains("TYPE"), "Output should contain TYPE column header");
                assertTrue(output.contains("PRIORITY"), "Output should contain PRIORITY column header");
                assertTrue(output.contains("STATUS"), "Output should contain STATUS column header");
                
                // Verify table contains work item data
                assertTrue(output.contains("First Test Item"), "Output should contain first item title");
                // Verify long titles are truncated
                assertTrue(output.contains("Second Test Item with a Very Long Tit...") || 
                           output.contains("Second Test Item with a Very Long..."), 
                    "Output should contain truncated long title");
                
                // Verify table has a footer with item count
                assertTrue(output.contains("Displaying"), "Output should contain item count information");
                assertTrue(output.contains("2 of 2"), "Output should display correct item count");
            }
        }
        
        @Test
        @DisplayName("Should handle empty result set properly")
        void shouldHandleEmptyResultSetProperly() {
            try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getSearchService()).thenReturn(mockSearchService);
                
                // Mock empty results
                when(mockSearchService.findWorkItems(anyMap(), anyInt())).thenReturn(List.of());
                
                // Setup ListCommand
                ListCommand listCmd = new ListCommand();
                
                // Execute command
                int exitCode = listCmd.call();
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully even with no results");
                
                // Verify output contains no results message
                String output = outputStream.toString();
                assertTrue(output.contains("No work items found"), 
                    "Output should indicate no matching items were found");
                
                // Verify table headers are not displayed for empty result set
                assertFalse(output.contains("ID"), "Table headers should not be displayed for empty results");
            }
        }
        
        @Test
        @DisplayName("Should apply limit to output display")
        void shouldApplyLimitToOutputDisplay() {
            try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getSearchService()).thenReturn(mockSearchService);
                
                // Create a large list of items that would exceed the default limit
                List<WorkItem> manyItems = new java.util.ArrayList<>();
                for (int i = 0; i < 120; i++) {
                    WorkItem item = new WorkItem();
                    item.setId(UUID.randomUUID().toString());
                    item.setTitle("Item " + i);
                    item.setType(WorkItemType.TASK);
                    manyItems.add(item);
                }
                
                // Mock the service to return many items
                when(mockSearchService.findWorkItems(anyMap(), anyInt())).thenReturn(manyItems);
                
                // Setup ListCommand with a small limit
                ListCommand listCmd = new ListCommand();
                listCmd.setLimit(5); // Only show 5 items
                
                // Execute command
                int exitCode = listCmd.call();
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");
                
                // Verify limited output
                String output = outputStream.toString();
                assertTrue(output.contains("Displaying 5 of 120"), 
                    "Output should indicate the limit was applied");
                assertTrue(output.contains("Use --limit="), 
                    "Output should include hint to see more items");
                
                // Count the number of items displayed (by counting item titles)
                int occurrences = 0;
                int index = -1;
                while ((index = output.indexOf("Item ", index + 1)) != -1) {
                    occurrences++;
                }
                assertEquals(5, occurrences, "Output should contain exactly 5 items due to limit");
            }
        }
    }
    
    @Nested
    @DisplayName("Add Command Format Tests")
    class AddCommandFormatTests {
        @Test
        @DisplayName("Should display formatted success message after adding item")
        void shouldDisplayFormattedSuccessMessageAfterAddingItem() {
            try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
                // Set up ServiceManager mock
                ServiceManager mockManager = mock(ServiceManager.class);
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockManager);
                when(mockManager.getItemService()).thenReturn(mockItemService);
                
                // Create new work item to be returned by mock
                WorkItem newItem = new WorkItem();
                String itemId = UUID.randomUUID().toString();
                newItem.setId(itemId);
                newItem.setTitle("New Feature");
                newItem.setType(WorkItemType.FEATURE);
                newItem.setState(WorkflowState.CREATED);
                
                // Mock the service to return the new item
                when(mockItemService.createItem(anyString(), any(WorkItemType.class), any(Priority.class), anyString()))
                    .thenReturn(newItem);
                
                // Setup AddCommand
                AddCommand addCmd = new AddCommand();
                addCmd.setTitle("New Feature");
                addCmd.setType(WorkItemType.FEATURE);
                addCmd.setPriority(Priority.MEDIUM);
                
                // Execute command
                int exitCode = addCmd.call();
                
                // Verify command execution
                assertEquals(0, exitCode, "Command should execute successfully");
                
                // Verify output formatting
                String output = outputStream.toString();
                assertTrue(output.contains("created") || output.contains("added"), 
                    "Output should confirm item creation");
                assertTrue(output.contains(itemId), "Output should contain the new item ID");
                assertTrue(output.contains("New Feature"), "Output should contain the item title");
                assertTrue(output.contains(WorkItemType.FEATURE.toString()), 
                    "Output should contain the item type");
            }
        }
    }
}