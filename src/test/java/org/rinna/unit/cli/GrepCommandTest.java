/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.unit.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.base.UnitTest;
import org.rinna.cli.command.GrepCommand;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.ServiceManager;
import org.rinna.domain.Priority;
import org.rinna.domain.SearchResult;
import org.rinna.domain.WorkItem;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.SearchService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the GrepCommand class.
 * This demonstrates a comprehensive pairwise testing approach for unit tests.
 */
@DisplayName("GrepCommand Unit Tests")
public class GrepCommandTest extends UnitTest {
    
    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private ContextManager contextManager;
    
    @Mock
    private ItemService itemService;
    
    @Mock
    private SearchService searchService;
    
    @Mock
    private WorkItem item1;
    
    @Mock
    private WorkItem item2;
    
    @Mock
    private WorkItem item3;
    
    private GrepCommand grepCommand;
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private final UUID item1Id = UUID.randomUUID();
    private final UUID item2Id = UUID.randomUUID();
    private final UUID item3Id = UUID.randomUUID();
    
    // Pairwise test data for parameters
    static Stream<Arguments> searchOptions() {
        return Stream.of(
            // pattern, caseSensitive, exactMatch, count, context
            Arguments.of("api", false, false, false, 0),      // Basic search
            Arguments.of("API", true, false, false, 0),       // Case-sensitive
            Arguments.of("api", false, true, false, 0),       // Exact match
            Arguments.of("api", false, false, true, 0),       // Count only
            Arguments.of("api", false, false, false, 2),      // With context
            Arguments.of("API", true, true, false, 0),        // Case+Exact
            Arguments.of("API", true, false, true, 0),        // Case+Count
            Arguments.of("API", true, false, false, 2),       // Case+Context
            Arguments.of("api", false, true, true, 0),        // Exact+Count
            Arguments.of("api", false, true, false, 2),       // Exact+Context
            Arguments.of("api", false, false, true, 2)        // Count+Context
        );
    }
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up System.out/err capturing
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Set up default command
        grepCommand = new GrepCommand();
        setMockServiceManager();
        
        // Reset captors between tests
        outputCaptor.reset();
        errorCaptor.reset();
        
        // Set up mock work items
        setupMockWorkItems();
        
        // Set up mock search results
        setupMockSearchResults();
    }
    
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    /**
     * Sets up a mock ServiceManager to be used by the GrepCommand.
     */
    private void setMockServiceManager() {
        try (var serviceManagerMock = mockStatic(ServiceManager.class)) {
            serviceManagerMock.when(ServiceManager::getInstance).thenReturn(serviceManager);
            
            // Set up mock responses
            when(serviceManager.getItemService()).thenReturn(itemService);
            when(serviceManager.getSearchService()).thenReturn(searchService);
        }
        
        try (var contextManagerMock = mockStatic(ContextManager.class)) {
            contextManagerMock.when(ContextManager::getInstance).thenReturn(contextManager);
        }
    }
    
    /**
     * Sets up mock work items for testing.
     */
    private void setupMockWorkItems() {
        // Set up mock work item 1
        when(item1.id()).thenReturn(item1Id);
        when(item1.title()).thenReturn("Implement API auth");
        when(item1.description()).thenReturn("Create API auth with validation");
        when(item1.type()).thenReturn(WorkItemType.TASK);
        when(item1.priority()).thenReturn(Priority.MEDIUM);
        when(item1.state()).thenReturn(WorkflowState.IN_PROGRESS);
        when(item1.assignee()).thenReturn("bob");
        when(item1.reporter()).thenReturn("alice");
        when(item1.createdAt()).thenReturn(Instant.now().minusSeconds(86400)); // 1 day ago
        when(item1.updatedAt()).thenReturn(Instant.now().minusSeconds(3600));  // 1 hour ago
        
        // Set up mock work item 2
        when(item2.id()).thenReturn(item2Id);
        when(item2.title()).thenReturn("Create user UI");
        when(item2.description()).thenReturn("User interface registration form");
        when(item2.type()).thenReturn(WorkItemType.TASK);
        when(item2.priority()).thenReturn(Priority.LOW);
        when(item2.state()).thenReturn(WorkflowState.READY);
        when(item2.assignee()).thenReturn("alice");
        when(item2.reporter()).thenReturn("bob");
        when(item2.createdAt()).thenReturn(Instant.now().minusSeconds(172800)); // 2 days ago
        when(item2.updatedAt()).thenReturn(Instant.now().minusSeconds(7200));   // 2 hours ago
        
        // Set up mock work item 3
        when(item3.id()).thenReturn(item3Id);
        when(item3.title()).thenReturn("Document API docs");
        when(item3.description()).thenReturn("Create API documentation");
        when(item3.type()).thenReturn(WorkItemType.DOCUMENTATION);
        when(item3.priority()).thenReturn(Priority.HIGH);
        when(item3.state()).thenReturn(WorkflowState.BACKLOG);
        when(item3.assignee()).thenReturn("bob");
        when(item3.reporter()).thenReturn("charlie");
        when(item3.createdAt()).thenReturn(Instant.now().minusSeconds(259200)); // 3 days ago
        when(item3.updatedAt()).thenReturn(Instant.now().minusSeconds(10800));  // 3 hours ago
        
        // Set up mock service to return these work items
        when(itemService.getItem(item1Id)).thenReturn(item1);
        when(itemService.getItem(item2Id)).thenReturn(item2);
        when(itemService.getItem(item3Id)).thenReturn(item3);
        when(itemService.getAllWorkItems()).thenReturn(Arrays.asList(item1, item2, item3));
    }
    
    /**
     * Sets up mock search results for testing.
     */
    private void setupMockSearchResults() {
        // For case-insensitive "api" search
        SearchResult result1 = new SearchResult(item1Id, "title", "Implement API auth", 
                Collections.singletonList(new SearchResult.Match(10, 13))); // "API" at position 10
        
        SearchResult result2 = new SearchResult(item1Id, "description", "Create API auth with validation",
                Collections.singletonList(new SearchResult.Match(7, 10))); // "API" at position 7
        
        SearchResult result3 = new SearchResult(item3Id, "title", "Document API docs",
                Collections.singletonList(new SearchResult.Match(9, 12))); // "API" at position 9
        
        SearchResult result4 = new SearchResult(item3Id, "description", "Create API documentation",
                Collections.singletonList(new SearchResult.Match(7, 10))); // "API" at position 7
        
        // Set up search service to return results for various search types
        
        // Default case-insensitive search
        when(searchService.searchWorkItems(eq("api"), anyBoolean(), anyBoolean()))
                .thenReturn(Arrays.asList(result1, result2, result3, result4));
        
        // Case-sensitive search for "API"
        when(searchService.searchWorkItems(eq("API"), eq(true), anyBoolean()))
                .thenReturn(Arrays.asList(result1, result3));
        
        // Case-sensitive search for "api" (would return no results)
        when(searchService.searchWorkItems(eq("api"), eq(true), anyBoolean()))
                .thenReturn(Collections.emptyList());
        
        // Exact match search
        when(searchService.searchWorkItems(eq("api"), anyBoolean(), eq(true)))
                .thenReturn(Arrays.asList(result2, result4));
        
        // Special case for item search to test contextual matches
        SearchResult contextResult = new SearchResult(item1Id, "description", 
                "The API auth module provides authentication for all system users.\nIt validates credentials securely.\nAnd logs all authentication attempts.",
                Collections.singletonList(new SearchResult.Match(4, 7))); // "API" at position 4
        
        when(searchService.searchWorkItemWithContext(eq("api"), anyInt(), anyBoolean(), anyBoolean()))
                .thenReturn(Collections.singletonList(contextResult));
    }
    
    @Test
    @DisplayName("Basic grep command should find matching work items")
    void basicGrepCommandShouldFindMatchingWorkItems() {
        // Setup
        grepCommand.setPattern("api");
        
        // Execute
        Integer result = grepCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Should contain work item info and highlight matches
        assertTrue(output.contains(item1Id.toString()), "Output should contain item1 ID");
        assertTrue(output.contains("Implement API auth"), "Output should contain item1 title");
        assertTrue(output.contains(item3Id.toString()), "Output should contain item3 ID");
        assertTrue(output.contains("Document API docs"), "Output should contain item3 title");
        
        // Should highlight matches
        assertTrue(output.contains("\u001B[1;31mAPI\u001B[0m"), "Output should highlight matches");
        
        // Verify search service was called with correct parameters
        verify(searchService).searchWorkItems(eq("api"), eq(false), eq(false));
        
        // Verify the context manager was not used
        verify(contextManager, never()).setLastViewedWorkItem(any());
    }
    
    @Test
    @DisplayName("Case-sensitive grep command should respect case")
    void caseSensitiveGrepCommandShouldRespectCase() {
        // Setup
        grepCommand.setPattern("API");
        grepCommand.setCaseSensitive(true);
        
        // Execute
        Integer result = grepCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Should contain work items with exact case match
        assertTrue(output.contains("Implement \u001B[1;31mAPI\u001B[0m auth"), 
                "Output should contain case-sensitive matches");
        
        // Verify search service was called with case-sensitive flag
        verify(searchService).searchWorkItems(eq("API"), eq(true), eq(false));
    }
    
    @Test
    @DisplayName("Exact match grep command should match whole words")
    void exactMatchGrepCommandShouldMatchWholeWords() {
        // Setup
        grepCommand.setPattern("api");
        grepCommand.setExactMatch(true);
        
        // Execute
        Integer result = grepCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify search service was called with exact match flag
        verify(searchService).searchWorkItems(eq("api"), eq(false), eq(true));
    }
    
    @Test
    @DisplayName("Count only grep command should show match counts")
    void countOnlyGrepCommandShouldShowMatchCounts() {
        // Setup
        grepCommand.setPattern("api");
        grepCommand.setCountOnly(true);
        
        // Execute
        Integer result = grepCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Should contain count information
        assertTrue(output.contains("Total matches:"), "Output should contain match count");
        assertTrue(output.contains("Matched work items:"), "Output should contain work item count");
        
        // Should not contain highlighted matches
        assertFalse(output.contains("\u001B[1;31m"), "Output should not contain highlighting");
    }
    
    @Test
    @DisplayName("Context grep command should show lines around matches")
    void contextGrepCommandShouldShowLinesAroundMatches() {
        // Setup
        grepCommand.setPattern("api");
        grepCommand.setContext(2);
        
        // Execute
        Integer result = grepCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Should contain context lines
        assertTrue(output.contains("provides authentication"), 
                "Output should contain context before match");
        assertTrue(output.contains("validates credentials"), 
                "Output should contain context after match");
        
        // Verify search service was called with context parameter
        verify(searchService).searchWorkItemWithContext(eq("api"), eq(2), eq(false), eq(false));
    }
    
    @ParameterizedTest
    @MethodSource("searchOptions")
    @DisplayName("Pairwise testing of grep command options")
    void pairwiseTestingOfGrepCommandOptions(String pattern, boolean caseSensitive, 
                                           boolean exactMatch, boolean countOnly, int context) {
        // Setup
        grepCommand.setPattern(pattern);
        grepCommand.setCaseSensitive(caseSensitive);
        grepCommand.setExactMatch(exactMatch);
        grepCommand.setCountOnly(countOnly);
        
        if (context > 0) {
            grepCommand.setContext(context);
        }
        
        // Execute
        Integer result = grepCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        // Verify appropriate service method was called
        if (context > 0) {
            verify(searchService).searchWorkItemWithContext(
                    eq(pattern), eq(context), eq(caseSensitive), eq(exactMatch));
        } else {
            verify(searchService).searchWorkItems(
                    eq(pattern), eq(caseSensitive), eq(exactMatch));
        }
        
        // Verify output formatting based on options
        String output = outputCaptor.toString();
        
        if (countOnly) {
            assertTrue(output.contains("Total matches:"), 
                    "Count only output should contain match count");
        } else {
            if (context > 0) {
                // Context lines should be shown
                assertTrue(output.contains("provides authentication") || 
                           output.contains("validates credentials"),
                        "Context output should contain surrounding lines");
            }
            
            // Highlight should be present in non-count mode
            assertTrue(output.contains("\u001B[1;31m"), 
                    "Non-count output should highlight matches");
        }
    }
    
    @Test
    @DisplayName("Grep command should fail with empty pattern")
    void grepCommandShouldFailWithEmptyPattern() {
        // Setup
        grepCommand.setPattern("");
        
        // Execute
        Integer result = grepCommand.call();
        
        // Verify
        assertEquals(1, result, "Command should fail");
        assertTrue(errorCaptor.toString().contains("Empty search pattern"), 
                "Error output should indicate empty pattern");
    }
    
    @Test
    @DisplayName("Grep command should handle no search results")
    void grepCommandShouldHandleNoSearchResults() {
        // Setup
        grepCommand.setPattern("nonexistent");
        when(searchService.searchWorkItems(eq("nonexistent"), anyBoolean(), anyBoolean()))
                .thenReturn(Collections.emptyList());
        
        // Execute
        Integer result = grepCommand.call();
        
        // Verify
        assertEquals(0, result, "Command should succeed even with no results");
        assertTrue(outputCaptor.toString().contains("No matches found"), 
                "Output should indicate no matches found");
    }
    
    @Test
    @DisplayName("Grep command should handle security vulnerabilities")
    void grepCommandShouldHandleSecurityVulnerabilities() {
        // Setup
        grepCommand.setPattern("api; rm -rf /");
        
        // Execute
        Integer result = grepCommand.call();
        
        // Verify search input is sanitized
        verify(searchService).searchWorkItems(eq("api; rm -rf /"), anyBoolean(), anyBoolean());
        
        // Command should still succeed
        assertEquals(0, result, "Command should handle potential injection strings");
    }
}