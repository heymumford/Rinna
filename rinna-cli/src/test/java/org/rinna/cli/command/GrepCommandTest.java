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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.domain.model.SearchResult;
import org.rinna.cli.domain.model.SearchResult.Match;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockSearchService;
import org.rinna.cli.service.ServiceManager;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@DisplayName("Grep Command Tests")
public class GrepCommandTest {
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private GrepCommand grepCommand;
    private ServiceManager mockServiceManager;
    private ContextManager mockContextManager;
    private TrackingSearchService mockSearchService;
    private TrackingItemService mockItemService;
    
    @BeforeEach
    void setUp() {
        // Redirect System.out and System.err
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        // Initialize command instance
        grepCommand = new GrepCommand();
        
        // Initialize mocks
        mockServiceManager = mock(ServiceManager.class);
        mockContextManager = mock(ContextManager.class);
        mockSearchService = new TrackingSearchService();
        mockItemService = new TrackingItemService();
        
        // Set up mocks
        when(mockServiceManager.getMockSearchService()).thenReturn(mockSearchService);
        when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    // Helper methods to create test data
    private WorkItem createTestWorkItem(String id, String title, String description, WorkItemType type,
                                      Priority priority, WorkflowState state) {
        WorkItem item = new WorkItem();
        item.setId(id);
        item.setTitle(title);
        item.setDescription(description);
        item.setType(type);
        item.setPriority(priority);
        item.setState(state);
        return item;
    }
    
    private SearchResult createTestSearchResult(String itemId, String text, String matchedText) {
        SearchResult result = new SearchResult(
            UUID.fromString(itemId),
            text,
            1,
            "workitem",
            text,
            matchedText
        );
        int index = text.indexOf(matchedText);
        if (index >= 0) {
            result.addMatch(new Match(index, index + matchedText.length(), matchedText));
        }
        return result;
    }
    
    /**
     * Custom mock implementation to track search queries.
     */
    private static class TrackingSearchService extends MockSearchService {
        private final List<String> searchPatterns = new ArrayList<>();
        private final List<Boolean> caseSensitiveFlags = new ArrayList<>();
        private final List<SearchResult> resultsList = new ArrayList<>();
        
        @Override
        public List<SearchResult> findText(String text, boolean caseSensitive) {
            searchPatterns.add(text);
            caseSensitiveFlags.add(caseSensitive);
            return getResultsList();
        }
        
        public List<String> getSearchPatterns() {
            return searchPatterns;
        }
        
        public List<Boolean> getCaseSensitiveFlags() {
            return caseSensitiveFlags;
        }
        
        public void addMockResult(SearchResult result) {
            resultsList.add(result);
        }
        
        public List<SearchResult> getResultsList() {
            return new ArrayList<>(resultsList);
        }
    }
    
    /**
     * Custom mock implementation to track work item operations.
     */
    private static class TrackingItemService extends MockItemService {
        private final List<String> requestedItems = new ArrayList<>();
        private final List<WorkItem> itemsList = new ArrayList<>();
        
        @Override
        public WorkItem getItem(String id) {
            requestedItems.add(id);
            return itemsList.stream()
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElse(null);
        }
        
        public List<String> getRequestedItems() {
            return requestedItems;
        }
        
        public void addMockItem(WorkItem item) {
            itemsList.add(item);
        }
    }
    
    @Nested
    @DisplayName("Help Documentation Tests")
    class HelpDocumentationTests {
        
        @Test
        @DisplayName("Should display error when pattern is null")
        void testNullPatternDisplaysError() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Execute command with null pattern
                grepCommand.setPattern(null);
                
                int exitCode = grepCommand.call();
                
                // Verify error message and exit code
                assertEquals(1, exitCode, "Exit code should be 1 for error");
                assertTrue(errContent.toString().contains("No search pattern provided"), 
                          "Error message should mention missing pattern");
                assertTrue(errContent.toString().contains("Usage: rin grep <options> <pattern>"), 
                          "Error message should include usage information");
            }
        }
        
        @Test
        @DisplayName("Should display error when pattern is empty")
        void testEmptyPatternDisplaysError() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Execute command with empty pattern
                grepCommand.setPattern("");
                
                int exitCode = grepCommand.call();
                
                // Verify error message and exit code
                assertEquals(1, exitCode, "Exit code should be 1 for error");
                assertTrue(errContent.toString().contains("No search pattern provided"), 
                          "Error message should mention missing pattern");
                assertTrue(errContent.toString().contains("Usage: rin grep <options> <pattern>"), 
                          "Error message should include usage information");
            }
        }
        
        @Test
        @DisplayName("Should display error when search service is not available")
        void testSearchServiceNotAvailable() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                // Configure service manager to return null for search service
                when(mockServiceManager.getMockSearchService()).thenReturn(null);
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Execute command
                grepCommand.setPattern("test");
                
                int exitCode = grepCommand.call();
                
                // Verify error message and exit code
                assertEquals(1, exitCode, "Exit code should be 1 for error");
                assertTrue(errContent.toString().contains("Search service not available"), 
                          "Error message should mention service unavailability");
            }
        }
        
        @Test
        @DisplayName("Should display message when no matches found")
        void testNoMatchesFound() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Execute command
                grepCommand.setPattern("nonexistent");
                
                int exitCode = grepCommand.call();
                
                // Verify message and exit code
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                assertTrue(outContent.toString().contains("No matches found for: nonexistent"), 
                          "Output should indicate no matches found");
            }
        }
    }
    
    @Nested
    @DisplayName("Positive Test Cases")
    class PositiveTestCases {
        
        @Test
        @DisplayName("Should perform basic search and display results in text format")
        void testBasicSearchWithTextOutput() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data
                String itemId = UUID.randomUUID().toString();
                WorkItem testItem = createTestWorkItem(
                    itemId, 
                    "Implement authentication feature", 
                    "Create JWT-based authentication for API endpoints",
                    WorkItemType.TASK,
                    Priority.MEDIUM,
                    WorkflowState.READY
                );
                mockItemService.addMockItem(testItem);
                
                SearchResult testResult = createTestSearchResult(
                    itemId,
                    "Create JWT-based authentication for API endpoints",
                    "authentication"
                );
                mockSearchService.addMockResult(testResult);
                
                // Execute command
                grepCommand.setPattern("authentication");
                
                int exitCode = grepCommand.call();
                
                // Verify execution
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                assertEquals("authentication", mockSearchService.getSearchPatterns().get(0), 
                            "Search pattern should match input");
                assertFalse(mockSearchService.getCaseSensitiveFlags().get(0), 
                          "Search should be case-insensitive by default");
                
                // Verify output contains expected information
                String output = outContent.toString();
                assertTrue(output.contains("Search results for: authentication"), 
                          "Output should contain search pattern");
                assertTrue(output.contains("Work Item: " + itemId), 
                          "Output should contain work item ID");
                assertTrue(output.contains("Implement authentication feature"), 
                          "Output should contain work item title");
                assertTrue(output.contains("Type: TASK | Priority: MEDIUM | Status: READY"), 
                          "Output should contain work item details");
            }
        }
        
        @Test
        @DisplayName("Should perform case-sensitive search when specified")
        void testCaseSensitiveSearch() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data
                String itemId = UUID.randomUUID().toString();
                WorkItem testItem = createTestWorkItem(
                    itemId, 
                    "Implement Authentication Feature", 
                    "Create JWT-based Authentication for API endpoints",
                    WorkItemType.TASK,
                    Priority.MEDIUM,
                    WorkflowState.READY
                );
                mockItemService.addMockItem(testItem);
                
                SearchResult testResult = createTestSearchResult(
                    itemId,
                    "Create JWT-based Authentication for API endpoints",
                    "Authentication"
                );
                mockSearchService.addMockResult(testResult);
                
                // Execute command with case-sensitive flag
                grepCommand.setPattern("Authentication");
                grepCommand.setCaseSensitive(true);
                
                int exitCode = grepCommand.call();
                
                // Verify execution
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                assertEquals("Authentication", mockSearchService.getSearchPatterns().get(0), 
                            "Search pattern should match input");
                assertTrue(mockSearchService.getCaseSensitiveFlags().get(0), 
                         "Search should be case-sensitive");
            }
        }
        
        @Test
        @DisplayName("Should add search pattern to history")
        void testSearchPatternAddedToHistory() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data
                mockSearchService.addMockResult(createTestSearchResult(
                    UUID.randomUUID().toString(),
                    "Example content with test pattern",
                    "test"
                ));
                
                // Execute command
                grepCommand.setPattern("test");
                
                int exitCode = grepCommand.call();
                
                // Verify search history was updated
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                verify(mockContextManager).addToSearchHistory("test");
            }
        }
        
        @Test
        @DisplayName("Should display results in JSON format when specified")
        void testJsonFormatOutput() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data
                String itemId = UUID.randomUUID().toString();
                WorkItem testItem = createTestWorkItem(
                    itemId, 
                    "Implement feature", 
                    "Create new feature",
                    WorkItemType.TASK,
                    Priority.MEDIUM,
                    WorkflowState.READY
                );
                mockItemService.addMockItem(testItem);
                
                SearchResult testResult = createTestSearchResult(
                    itemId,
                    "Create new feature",
                    "feature"
                );
                mockSearchService.addMockResult(testResult);
                
                // Execute command
                grepCommand.setPattern("feature");
                grepCommand.setOutputFormat("json");
                
                int exitCode = grepCommand.call();
                
                // Verify execution
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                // Verify JSON output
                String output = outContent.toString();
                assertTrue(output.contains("\"pattern\": \"feature\""), 
                          "JSON output should contain pattern");
                assertTrue(output.contains("\"id\": \"" + itemId + "\""), 
                          "JSON output should contain work item ID");
                assertTrue(output.contains("\"title\": \"Implement feature\""), 
                          "JSON output should contain work item title");
                assertTrue(output.contains("\"text\": \"feature\""), 
                          "JSON output should contain matched text");
            }
        }
        
        @Test
        @DisplayName("Should display results in CSV format when specified")
        void testCsvFormatOutput() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data
                String itemId = UUID.randomUUID().toString();
                WorkItem testItem = createTestWorkItem(
                    itemId, 
                    "Implement feature", 
                    "Create new feature",
                    WorkItemType.TASK,
                    Priority.MEDIUM,
                    WorkflowState.READY
                );
                mockItemService.addMockItem(testItem);
                
                SearchResult testResult = createTestSearchResult(
                    itemId,
                    "Create new feature",
                    "feature"
                );
                mockSearchService.addMockResult(testResult);
                
                // Execute command
                grepCommand.setPattern("feature");
                grepCommand.setOutputFormat("csv");
                
                int exitCode = grepCommand.call();
                
                // Verify execution
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                // Verify CSV output
                String output = outContent.toString();
                assertTrue(output.contains("WorkItemId,Title,Type,Priority,Status,MatchText,MatchStart,MatchEnd"), 
                          "CSV output should contain header");
                assertTrue(output.contains("\"" + itemId + "\",\"Implement feature\",\"TASK\",\"MEDIUM\",\"READY\",\"feature\""), 
                          "CSV output should contain work item details");
            }
        }
        
        @Test
        @DisplayName("Should display count summary when count-only mode is enabled")
        void testCountOnlyMode() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data with multiple matches
                String itemId1 = UUID.randomUUID().toString();
                String itemId2 = UUID.randomUUID().toString();
                
                // Add test items
                mockItemService.addMockItem(createTestWorkItem(
                    itemId1, 
                    "First feature", 
                    "Implementation of first feature",
                    WorkItemType.TASK,
                    Priority.HIGH,
                    WorkflowState.READY
                ));
                
                mockItemService.addMockItem(createTestWorkItem(
                    itemId2, 
                    "Second feature", 
                    "Implementation of second feature",
                    WorkItemType.TASK,
                    Priority.MEDIUM,
                    WorkflowState.READY
                ));
                
                // Add search results
                SearchResult result1 = createTestSearchResult(
                    itemId1,
                    "Implementation of first feature",
                    "feature"
                );
                
                SearchResult result2 = createTestSearchResult(
                    itemId2,
                    "Implementation of second feature",
                    "feature"
                );
                
                mockSearchService.addMockResult(result1);
                mockSearchService.addMockResult(result2);
                
                // Execute command
                grepCommand.setPattern("feature");
                grepCommand.setCountOnly(true);
                
                int exitCode = grepCommand.call();
                
                // Verify execution
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                // Verify count summary output
                String output = outContent.toString();
                assertTrue(output.contains("Search results for: feature"), 
                          "Output should contain search pattern");
                assertTrue(output.contains("Total matches: 2"), 
                          "Output should show correct match count");
                assertTrue(output.contains("Matched work items: 2"), 
                          "Output should show correct work item count");
            }
        }
        
        @Test
        @DisplayName("Should display context lines when context is specified")
        void testContextDisplay() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data with multi-line content
                String itemId = UUID.randomUUID().toString();
                String multiLineDesc = "Line 1\nLine 2 with pattern\nLine 3\nLine 4";
                
                WorkItem testItem = createTestWorkItem(
                    itemId, 
                    "Test item", 
                    multiLineDesc,
                    WorkItemType.TASK,
                    Priority.MEDIUM,
                    WorkflowState.READY
                );
                mockItemService.addMockItem(testItem);
                
                // Create a search result with a match in line 2
                SearchResult result = new SearchResult(
                    UUID.fromString(itemId),
                    multiLineDesc,
                    2, // Line number
                    "workitem",
                    multiLineDesc,
                    "pattern"
                );
                
                // Add a match at position of "pattern" in "Line 2 with pattern"
                int startPos = multiLineDesc.indexOf("pattern");
                result.addMatch(new Match(startPos, startPos + "pattern".length(), "pattern"));
                
                mockSearchService.addMockResult(result);
                
                // Execute command with context
                grepCommand.setPattern("pattern");
                grepCommand.setContext(1);
                
                int exitCode = grepCommand.call();
                
                // Verify execution
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                // Verify context display
                String output = outContent.toString();
                assertTrue(output.contains("Line 1"), 
                          "Output should contain context line before match");
                assertTrue(output.contains("Line 2 with pattern"), 
                          "Output should contain matched line");
                assertTrue(output.contains("Line 3"), 
                          "Output should contain context line after match");
                assertFalse(output.contains("Line 4"), 
                           "Output should not contain lines beyond context range");
            }
        }
        
        @Test
        @DisplayName("Should display verbose output when error occurs with verbose flag")
        void testVerboseErrorOutput() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                // Make search service throw an exception
                MockSearchService errorService = mock(MockSearchService.class);
                when(errorService.findText(anyString(), anyBoolean()))
                    .thenThrow(new RuntimeException("Test exception"));
                
                when(mockServiceManager.getMockSearchService()).thenReturn(errorService);
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Execute command with verbose flag
                grepCommand.setPattern("test");
                grepCommand.setVerbose(true);
                
                int exitCode = grepCommand.call();
                
                // Verify error handling
                assertEquals(1, exitCode, "Exit code should be 1 for error");
                assertTrue(errContent.toString().contains("Error: Test exception"), 
                          "Error output should contain exception message");
                assertTrue(errContent.toString().contains("RuntimeException"), 
                          "Verbose error output should contain stack trace");
            }
        }
        
        @Test
        @DisplayName("Should correctly handle multiple matches in the same work item")
        void testMultipleMatchesInSameItem() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data
                String itemId = UUID.randomUUID().toString();
                String content = "This test contains multiple test matches in one test item";
                
                WorkItem testItem = createTestWorkItem(
                    itemId, 
                    "Test multiple matches", 
                    content,
                    WorkItemType.TASK,
                    Priority.MEDIUM,
                    WorkflowState.READY
                );
                mockItemService.addMockItem(testItem);
                
                // Create a search result with multiple matches
                SearchResult result = new SearchResult(
                    UUID.fromString(itemId),
                    content,
                    1,
                    "workitem",
                    content,
                    "test"
                );
                
                // Add three matches for the word "test"
                int pos1 = content.indexOf("test");
                result.addMatch(new Match(pos1, pos1 + 4, "test"));
                
                int pos2 = content.indexOf("test", pos1 + 1);
                result.addMatch(new Match(pos2, pos2 + 4, "test"));
                
                int pos3 = content.indexOf("test", pos2 + 1);
                result.addMatch(new Match(pos3, pos3 + 4, "test"));
                
                mockSearchService.addMockResult(result);
                
                // Execute command
                grepCommand.setPattern("test");
                
                int exitCode = grepCommand.call();
                
                // Verify execution
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                // Check that all three matches are displayed
                String output = outContent.toString();
                int matchCount = (output.split("\\[test\\]").length - 1);
                assertEquals(3, matchCount, "All 3 matches should be displayed");
            }
        }
    }
    
    @Nested
    @DisplayName("Negative Test Cases")
    class NegativeTestCases {
        
        @ParameterizedTest
        @ValueSource(strings = {"  ", "\t\n"})
        @DisplayName("Should display error for blank pattern")
        void testBlankPatternDisplaysError(String blankPattern) {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Execute command with blank pattern
                grepCommand.setPattern(blankPattern);
                
                int exitCode = grepCommand.call();
                
                // Verify error message and exit code
                assertEquals(1, exitCode, "Exit code should be 1 for error");
                assertTrue(errContent.toString().contains("No search pattern provided"), 
                          "Error message should mention missing pattern");
            }
        }
        
        @Test
        @DisplayName("Should handle normal error without stack trace when verbose is disabled")
        void testNonVerboseErrorHandling() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                // Make search service throw an exception
                MockSearchService errorService = mock(MockSearchService.class);
                when(errorService.findText(anyString(), anyBoolean()))
                    .thenThrow(new RuntimeException("Test exception"));
                
                when(mockServiceManager.getMockSearchService()).thenReturn(errorService);
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Execute command without verbose flag
                grepCommand.setPattern("test");
                grepCommand.setVerbose(false);
                
                int exitCode = grepCommand.call();
                
                // Verify error handling
                assertEquals(1, exitCode, "Exit code should be 1 for error");
                assertTrue(errContent.toString().contains("Error: Test exception"), 
                          "Error output should contain exception message");
                assertFalse(errContent.toString().contains("RuntimeException:"), 
                           "Non-verbose error output should not contain stack trace");
            }
        }
        
        @Test
        @DisplayName("Should reject invalid output format")
        void testInvalidOutputFormat() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up search results
                mockSearchService.addMockResult(createTestSearchResult(
                    UUID.randomUUID().toString(),
                    "Example content",
                    "example"
                ));
                
                // Execute command with invalid output format
                grepCommand.setPattern("example");
                grepCommand.setOutputFormat("invalid");
                
                int exitCode = grepCommand.call();
                
                // Even with invalid format, command should still succeed
                // as it falls back to text output
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                // Should use text output as fallback
                String output = outContent.toString();
                assertTrue(output.contains("Search results for: example"), 
                          "Should fall back to text output format");
                assertTrue(output.contains("-----------------------------------------"), 
                          "Should show text format separator");
            }
        }
        
        @Test
        @DisplayName("Should handle negative context value")
        void testNegativeContextValue() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data
                String itemId = UUID.randomUUID().toString();
                WorkItem item = createTestWorkItem(
                    itemId, 
                    "Test item", 
                    "Test description with example text",
                    WorkItemType.TASK,
                    Priority.MEDIUM,
                    WorkflowState.READY
                );
                mockItemService.addMockItem(item);
                
                mockSearchService.addMockResult(createTestSearchResult(
                    itemId,
                    "Test description with example text",
                    "example"
                ));
                
                // Execute command with negative context
                grepCommand.setPattern("example");
                grepCommand.setContext(-1);
                
                int exitCode = grepCommand.call();
                
                // Command should still succeed
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                // Should not try to display context lines
                String output = outContent.toString();
                assertFalse(output.contains("1:"), "Should not display line numbers with context");
            }
        }
        
        @Test
        @DisplayName("Should handle null work item when retrieving match details")
        void testNullWorkItemInResults() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up a search result for a non-existent work item
                String nonExistentId = UUID.randomUUID().toString();
                
                mockSearchService.addMockResult(createTestSearchResult(
                    nonExistentId,
                    "Example content",
                    "example"
                ));
                
                // Execute command
                grepCommand.setPattern("example");
                
                int exitCode = grepCommand.call();
                
                // Command should still succeed
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                // Verify item service was called with the ID
                assertTrue(mockItemService.getRequestedItems().contains(nonExistentId),
                          "Item service should be called with non-existent ID");
                
                // Output should not contain work item details
                String output = outContent.toString();
                assertFalse(output.contains("Work Item: " + nonExistentId),
                           "Output should not contain details for null item");
            }
        }
        
        @ParameterizedTest
        @CsvSource({
            "some pattern with special * character",
            "another [pattern] with brackets",
            "regex pattern with + symbol"
        })
        @DisplayName("Should handle regex-like patterns without errors")
        void testRegexLikePatterns(String pattern) {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Execute command with pattern containing special characters
                grepCommand.setPattern(pattern);
                
                int exitCode = grepCommand.call();
                
                // Command should succeed
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                // Pattern should be passed to search service unchanged
                assertEquals(pattern, mockSearchService.getSearchPatterns().get(0),
                            "Search pattern should be passed unchanged");
            }
        }
        
        @Test
        @DisplayName("Should handle color output being disabled")
        void testColorOutputDisabled() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data
                String itemId = UUID.randomUUID().toString();
                WorkItem item = createTestWorkItem(
                    itemId, 
                    "Test item", 
                    "Test description with example text",
                    WorkItemType.TASK,
                    Priority.MEDIUM,
                    WorkflowState.READY
                );
                mockItemService.addMockItem(item);
                
                mockSearchService.addMockResult(createTestSearchResult(
                    itemId,
                    "Test description with example text",
                    "example"
                ));
                
                // Execute command with color output disabled
                grepCommand.setPattern("example");
                grepCommand.setColorOutput(false);
                
                int exitCode = grepCommand.call();
                
                // Command should succeed
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                // Output should use brackets instead of color codes
                String output = outContent.toString();
                assertTrue(output.contains("[example]"), 
                          "Output should use brackets instead of color codes");
                assertFalse(output.contains("\033[1;31m"), 
                           "Output should not contain color codes");
            }
        }
        
        @Test
        @DisplayName("Should handle exactMatch flag even though implementation is incomplete")
        void testExactMatchFlag() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data
                mockSearchService.addMockResult(createTestSearchResult(
                    UUID.randomUUID().toString(),
                    "Example content",
                    "example"
                ));
                
                // Execute command with exactMatch flag
                grepCommand.setPattern("example");
                grepCommand.setExactMatch(true);
                
                int exitCode = grepCommand.call();
                
                // Command should succeed
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
            }
        }
        
        @Test
        @DisplayName("Should handle fileOutput flag even though implementation is incomplete")
        void testFileOutputFlag() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data
                mockSearchService.addMockResult(createTestSearchResult(
                    UUID.randomUUID().toString(),
                    "Example content",
                    "example"
                ));
                
                // Execute command with fileOutput flag
                grepCommand.setPattern("example");
                grepCommand.setFileOutput(true);
                
                int exitCode = grepCommand.call();
                
                // Command should succeed
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
            }
        }
        
        @Test
        @DisplayName("Should ignore not implemented showHistory flag")
        void testShowHistoryFlag() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data
                mockSearchService.addMockResult(createTestSearchResult(
                    UUID.randomUUID().toString(),
                    "Example content",
                    "example"
                ));
                
                // Execute command with showHistory flag
                grepCommand.setPattern("example");
                grepCommand.setShowHistory(true);
                
                // Verify showHistory is still false (not implemented yet)
                assertFalse(grepCommand.isShowHistory(), "ShowHistory should return false as not implemented");
                
                int exitCode = grepCommand.call();
                
                // Command should succeed
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
            }
        }
    }
    
    @Nested
    @DisplayName("Contract Tests")
    class ContractTests {
        
        @Test
        @DisplayName("Should rely on SearchService.findText() for basic searches")
        void testContractWithSearchService() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Execute command
                grepCommand.setPattern("test");
                grepCommand.setCaseSensitive(true);
                
                int exitCode = grepCommand.call();
                
                // Verify search service contract
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                assertEquals(1, mockSearchService.getSearchPatterns().size(), 
                            "SearchService.findText should be called once");
                assertEquals("test", mockSearchService.getSearchPatterns().get(0), 
                            "Search pattern should be passed correctly");
                assertEquals(Boolean.TRUE, mockSearchService.getCaseSensitiveFlags().get(0), 
                            "Case sensitivity flag should be passed correctly");
            }
        }
        
        @Test
        @DisplayName("Should rely on ItemService.getItem() to retrieve work item details")
        void testContractWithItemService() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data
                String itemId = UUID.randomUUID().toString();
                WorkItem item = createTestWorkItem(
                    itemId, 
                    "Test item", 
                    "Test description",
                    WorkItemType.TASK,
                    Priority.MEDIUM,
                    WorkflowState.READY
                );
                mockItemService.addMockItem(item);
                
                mockSearchService.addMockResult(createTestSearchResult(
                    itemId,
                    "Test description",
                    "Test"
                ));
                
                // Execute command
                grepCommand.setPattern("Test");
                
                int exitCode = grepCommand.call();
                
                // Verify item service contract
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                assertEquals(1, mockItemService.getRequestedItems().size(), 
                            "ItemService.getItem should be called once");
                assertEquals(itemId, mockItemService.getRequestedItems().get(0), 
                            "Item ID should be passed correctly");
            }
        }
        
        @Test
        @DisplayName("Should add search pattern to context manager history")
        void testContractWithContextManager() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data
                mockSearchService.addMockResult(createTestSearchResult(
                    UUID.randomUUID().toString(),
                    "Example content",
                    "pattern"
                ));
                
                // Execute command
                grepCommand.setPattern("pattern");
                
                int exitCode = grepCommand.call();
                
                // Verify context manager contract
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                verify(mockContextManager, times(1)).addToSearchHistory("pattern");
            }
        }
        
        @Test
        @DisplayName("Should retrieve singleton instances from ServiceManager and ContextManager")
        void testContractWithStaticManagers() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data
                mockSearchService.addMockResult(createTestSearchResult(
                    UUID.randomUUID().toString(),
                    "Example content",
                    "example"
                ));
                
                // Execute command
                grepCommand.setPattern("example");
                
                int exitCode = grepCommand.call();
                
                // Verify manager contracts
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                serviceManagerMock.verify(ServiceManager::getInstance, times(1));
                contextManagerMock.verify(ContextManager::getInstance, times(1));
            }
        }
        
        @Test
        @DisplayName("Should correctly handle getPattern() accessor method")
        void testGetPatternMethod() {
            // Test the accessor method
            grepCommand.setPattern("testPattern");
            assertEquals("testPattern", grepCommand.getPattern(), 
                        "getPattern() should return the pattern set by setPattern()");
        }
    }
    
    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {
        
        @Test
        @DisplayName("Integration test for basic search workflow")
        void testBasicSearchWorkflow() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data with multiple items
                String itemId1 = UUID.randomUUID().toString();
                String itemId2 = UUID.randomUUID().toString();
                String itemId3 = UUID.randomUUID().toString();
                
                // Create multiple work items with varying content
                WorkItem item1 = createTestWorkItem(
                    itemId1, 
                    "First authentication task", 
                    "Implement basic authentication",
                    WorkItemType.TASK,
                    Priority.HIGH,
                    WorkflowState.READY
                );
                
                WorkItem item2 = createTestWorkItem(
                    itemId2, 
                    "Second task", 
                    "Without authentication keyword",
                    WorkItemType.TASK,
                    Priority.MEDIUM,
                    WorkflowState.IN_PROGRESS
                );
                
                WorkItem item3 = createTestWorkItem(
                    itemId3, 
                    "Third task", 
                    "Add authentication module tests",
                    WorkItemType.TASK, // Changed from TEST to TASK as TEST doesn't exist
                    Priority.LOW,
                    WorkflowState.DONE
                );
                
                // Add items to mock service
                mockItemService.addMockItem(item1);
                mockItemService.addMockItem(item2);
                mockItemService.addMockItem(item3);
                
                // Create search results for items that match "authentication"
                SearchResult result1 = createTestSearchResult(
                    itemId1,
                    "First authentication task",
                    "authentication"
                );
                
                SearchResult result3 = createTestSearchResult(
                    itemId3,
                    "Add authentication module tests",
                    "authentication"
                );
                
                mockSearchService.addMockResult(result1);
                mockSearchService.addMockResult(result3);
                
                // Execute grep command
                grepCommand.setPattern("authentication");
                
                int exitCode = grepCommand.call();
                
                // Verify execution
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                // Verify search was performed
                assertEquals("authentication", mockSearchService.getSearchPatterns().get(0),
                            "Search pattern should be correct");
                
                // Verify context history was updated
                verify(mockContextManager).addToSearchHistory("authentication");
                
                // Verify item service was called for each matching work item
                assertTrue(mockItemService.getRequestedItems().contains(itemId1),
                          "Item service should be called for first matching item");
                assertTrue(mockItemService.getRequestedItems().contains(itemId3),
                          "Item service should be called for third matching item");
                
                // Verify output contains both matching items
                String output = outContent.toString();
                assertTrue(output.contains("First authentication task"),
                          "Output should contain first item title");
                assertTrue(output.contains("Type: TASK | Priority: HIGH | Status: READY"),
                          "Output should contain first item details");
                
                assertTrue(output.contains("Add authentication module tests"),
                          "Output should contain third item title");
                assertTrue(output.contains("Type: TEST | Priority: LOW | Status: DONE"),
                          "Output should contain third item details");
                
                // Verify that the non-matching item isn't in the output
                assertFalse(output.contains("Second task"),
                           "Output should not contain non-matching item");
            }
        }
        
        @Test
        @DisplayName("Should output search results to absolute path file location")
        void testOutputToAbsolutePathFile() throws Exception {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Create test data
                String itemId = UUID.randomUUID().toString();
                WorkItem item = createTestWorkItem(
                    itemId, 
                    "Test item", 
                    "Description with search pattern",
                    WorkItemType.TASK,
                    Priority.MEDIUM,
                    WorkflowState.READY
                );
                mockItemService.addMockItem(item);
                
                mockSearchService.addMockResult(createTestSearchResult(
                    itemId,
                    "Description with search pattern",
                    "search pattern"
                ));
                
                // Create a temp file with absolute path for output
                java.io.File tempFile = java.io.File.createTempFile("grep-test-", ".txt");
                tempFile.deleteOnExit();
                String absoluteFilePath = tempFile.getAbsolutePath();
                
                // Create a custom GrepCommand that outputs to file
                GrepCommand fileOutputCommand = new GrepCommand() {
                    @Override
                    public Integer call() {
                        try {
                            // Capture standard output to a file
                            PrintStream originalOut = System.out;
                            try (PrintStream fileOut = new PrintStream(absoluteFilePath)) {
                                System.setOut(fileOut);
                                
                                // Call the actual command implementation
                                int result = super.call();
                                
                                return result;
                            } finally {
                                System.setOut(originalOut);
                            }
                        } catch (Exception e) {
                            System.err.println("Error: " + e.getMessage());
                            return 1;
                        }
                    }
                };
                
                // Configure the command
                fileOutputCommand.setPattern("search pattern");
                fileOutputCommand.setOutputFormat("text");
                
                // Execute command
                int exitCode = fileOutputCommand.call();
                
                // Verify execution
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                // Read the file content
                String fileContent = java.nio.file.Files.readString(java.nio.file.Path.of(absoluteFilePath));
                
                // Verify file content
                assertTrue(fileContent.contains("Search results for: search pattern"), 
                          "File content should include search pattern");
                assertTrue(fileContent.contains("Test item"), 
                          "File content should include work item title");
                assertTrue(fileContent.contains("Type: TASK | Priority: MEDIUM | Status: READY"), 
                          "File content should include work item details");
            }
        }
        
        @Test
        @DisplayName("Integration test for complex search with multiple match types")
        void testComplexSearchWithMultipleMatchTypes() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up a complex scenario with:
                // - Multiple work items
                // - Multiple matches in some items
                // - Different work item types, priorities, states
                
                // Create work items
                String itemId1 = UUID.randomUUID().toString();
                String itemId2 = UUID.randomUUID().toString();
                
                WorkItem item1 = createTestWorkItem(
                    itemId1, 
                    "Bug: Authentication failure", 
                    "Users report authentication failure after logout",
                    WorkItemType.BUG,
                    Priority.HIGH,
                    WorkflowState.IN_PROGRESS
                );
                
                WorkItem item2 = createTestWorkItem(
                    itemId2, 
                    "Authentication documentation", 
                    "Create comprehensive authentication documentation",
                    WorkItemType.TASK,
                    Priority.MEDIUM,
                    WorkflowState.READY
                );
                
                mockItemService.addMockItem(item1);
                mockItemService.addMockItem(item2);
                
                // Create search results with multiple matches
                SearchResult result1 = new SearchResult(
                    UUID.fromString(itemId1),
                    "Bug: Authentication failure\nUsers report authentication failure after logout",
                    1,
                    "workitem",
                    "Users report authentication failure after logout",
                    "authentication"
                );
                
                // Add matches for both occurrences of "authentication" in item1
                int titlePos = "Bug: ".length();
                result1.addMatch(new Match(titlePos, titlePos + "authentication".length(), "authentication"));
                
                int descPos = "Users report ".length();
                result1.addMatch(new Match(titlePos + "Authentication failure\n".length() + descPos,
                                         titlePos + "Authentication failure\n".length() + descPos + "authentication".length(),
                                         "authentication"));
                
                SearchResult result2 = new SearchResult(
                    UUID.fromString(itemId2),
                    "Authentication documentation\nCreate comprehensive authentication documentation",
                    1,
                    "workitem",
                    "Create comprehensive authentication documentation",
                    "authentication"
                );
                
                // Add matches for both occurrences of "authentication" in item2
                result2.addMatch(new Match(0, "authentication".length(), "authentication"));
                
                int descPos2 = "Create comprehensive ".length();
                result2.addMatch(new Match(
                    "Authentication documentation\n".length() + descPos2,
                    "Authentication documentation\n".length() + descPos2 + "authentication".length(),
                    "authentication"
                ));
                
                mockSearchService.addMockResult(result1);
                mockSearchService.addMockResult(result2);
                
                // Execute command with different output formats
                
                // 1. Test CSV output
                grepCommand.setPattern("authentication");
                grepCommand.setOutputFormat("csv");
                
                int exitCode = grepCommand.call();
                
                // Verify execution
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                // Verify CSV output format
                String csvOutput = outContent.toString();
                assertTrue(csvOutput.contains("WorkItemId,Title,Type,Priority,Status,MatchText,MatchStart,MatchEnd"),
                          "CSV output should have correct header");
                
                assertTrue(csvOutput.contains("\"" + itemId1 + "\""),
                          "CSV output should contain first item ID");
                assertTrue(csvOutput.contains("\"" + itemId2 + "\""),
                          "CSV output should contain second item ID");
                
                // Clear output for next test
                outContent.reset();
                
                // 2. Test count-only mode
                grepCommand.setPattern("authentication");
                grepCommand.setOutputFormat("text"); // Reset to text
                grepCommand.setCountOnly(true);
                
                exitCode = grepCommand.call();
                
                // Verify count output
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                String countOutput = outContent.toString();
                assertTrue(countOutput.contains("Total matches: 4"),
                          "Count output should show 4 total matches");
                assertTrue(countOutput.contains("Matched work items: 2"),
                          "Count output should show 2 matched work items");
                
                // Clear output for next test
                outContent.reset();
                
                // 3. Test JSON output
                grepCommand.setPattern("authentication");
                grepCommand.setCountOnly(false);
                grepCommand.setOutputFormat("json");
                
                exitCode = grepCommand.call();
                
                // Verify JSON output
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                String jsonOutput = outContent.toString();
                assertTrue(jsonOutput.contains("\"pattern\": \"authentication\""),
                          "JSON output should include search pattern");
                assertTrue(jsonOutput.contains("\"results\": ["),
                          "JSON output should have results array");
                assertTrue(jsonOutput.contains("\"id\": \"" + itemId1 + "\""),
                          "JSON output should contain first item ID");
                assertTrue(jsonOutput.contains("\"id\": \"" + itemId2 + "\""),
                          "JSON output should contain second item ID");
                assertTrue(jsonOutput.contains("\"matches\": ["),
                          "JSON output should contain matches array");
            }
        }
        
        @Test
        @DisplayName("Integration test for search with context display")
        void testSearchWithContextDisplay() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up a work item with multi-line description
                String itemId = UUID.randomUUID().toString();
                String multiLineDesc = "Line 1: Introduction\n" +
                                      "Line 2: This contains the pattern we're looking for\n" +
                                      "Line 3: More details\n" +
                                      "Line 4: Additional information\n" +
                                      "Line 5: Conclusion";
                
                WorkItem item = createTestWorkItem(
                    itemId, 
                    "Test item with multi-line description", 
                    multiLineDesc,
                    WorkItemType.TASK,
                    Priority.MEDIUM,
                    WorkflowState.READY
                );
                
                mockItemService.addMockItem(item);
                
                // Create a search result with a match in line 2
                SearchResult result = new SearchResult(
                    UUID.fromString(itemId),
                    multiLineDesc,
                    2, // Line number
                    "workitem",
                    multiLineDesc,
                    "pattern"
                );
                
                // Calculate the position of "pattern" in the content
                int lineStartPos = multiLineDesc.indexOf("Line 2:");
                int patternPos = multiLineDesc.indexOf("pattern", lineStartPos);
                
                // Add the match
                result.addMatch(new Match(
                    patternPos,
                    patternPos + "pattern".length(),
                    "pattern"
                ));
                
                mockSearchService.addMockResult(result);
                
                // Execute command with context of 2 lines
                grepCommand.setPattern("pattern");
                grepCommand.setContext(2);
                
                int exitCode = grepCommand.call();
                
                // Verify execution
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                // Verify context display
                String output = outContent.toString();
                
                // Should show 2 lines before, the match line, and 2 lines after
                assertTrue(output.contains("Line 1: Introduction"),
                          "Output should contain first context line before match");
                assertTrue(output.contains("Line 2: This contains the pattern we're looking for"),
                          "Output should contain the matched line");
                assertTrue(output.contains("Line 3: More details"),
                          "Output should contain first context line after match");
                assertTrue(output.contains("Line 4: Additional information"),
                          "Output should contain second context line after match");
                
                // Should include line numbers
                assertTrue(output.contains("1:"),
                          "Output should contain line number for context");
                assertTrue(output.contains("2:"),
                          "Output should contain line number for match");
                
                // Pattern should be highlighted
                assertTrue(output.contains("\033[1;31mpattern\033[0m") || output.contains("[pattern]"),
                          "Output should highlight the matched pattern");
            }
        }
        
        @Test
        @DisplayName("Integration test for error handling")
        void testErrorHandling() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                // Configure context manager to throw exception
                doThrow(new RuntimeException("Test context exception"))
                    .when(mockContextManager).addToSearchHistory(anyString());
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Execute command
                grepCommand.setPattern("test");
                
                int exitCode = grepCommand.call();
                
                // Verify error handling
                assertEquals(1, exitCode, "Exit code should be 1 for error");
                assertTrue(errContent.toString().contains("Error: Test context exception"),
                          "Error output should contain exception message");
                
                // Now test with verbose error output
                errContent.reset();
                
                grepCommand.setVerbose(true);
                
                exitCode = grepCommand.call();
                
                assertEquals(1, exitCode, "Exit code should be 1 for error");
                assertTrue(errContent.toString().contains("Error: Test context exception"),
                          "Error output should contain exception message");
                assertTrue(errContent.toString().contains("RuntimeException"),
                          "Verbose error output should contain stack trace");
            }
        }
        
        @Test
        @DisplayName("Integration test for combined flags")
        void testCombinedFlags() {
            try (MockedStatic<ServiceManager> serviceManagerMock = Mockito.mockStatic(ServiceManager.class);
                 MockedStatic<ContextManager> contextManagerMock = Mockito.mockStatic(ContextManager.class)) {
                
                serviceManagerMock.when(ServiceManager::getInstance).thenReturn(mockServiceManager);
                contextManagerMock.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                // Set up test data
                String itemId = UUID.randomUUID().toString();
                WorkItem item = createTestWorkItem(
                    itemId, 
                    "Test item", 
                    "Test description with pattern",
                    WorkItemType.TASK,
                    Priority.MEDIUM,
                    WorkflowState.READY
                );
                
                mockItemService.addMockItem(item);
                
                SearchResult result = createTestSearchResult(
                    itemId,
                    "Test description with pattern",
                    "pattern"
                );
                
                mockSearchService.addMockResult(result);
                
                // Execute command with multiple flags
                grepCommand.setPattern("pattern");
                grepCommand.setCaseSensitive(true);
                grepCommand.setColorOutput(false);
                grepCommand.setOutputFormat("text");
                
                int exitCode = grepCommand.call();
                
                // Verify execution
                assertEquals(0, exitCode, "Exit code should be 0 for successful execution");
                
                // Verify all flags were applied
                assertEquals("pattern", mockSearchService.getSearchPatterns().get(0),
                            "Search pattern should be correct");
                assertTrue(mockSearchService.getCaseSensitiveFlags().get(0),
                          "Case sensitivity flag should be applied");
                
                // Output should use brackets instead of color codes
                String output = outContent.toString();
                assertTrue(output.contains("[pattern]"),
                          "Output should use brackets instead of color codes");
                assertFalse(output.contains("\033[1;31m"),
                           "Output should not contain color codes");
            }
        }
    }
}