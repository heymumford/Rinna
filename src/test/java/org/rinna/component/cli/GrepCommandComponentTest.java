/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.component.cli;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.base.ComponentTest;
import org.rinna.cli.command.GrepCommand;
import org.rinna.cli.service.CommandExecutor;
import org.rinna.cli.service.ContextManager;
import org.rinna.domain.DefaultWorkItem;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.SearchService;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Component tests for the GrepCommand class.
 * These tests verify that the GrepCommand works correctly in conjunction with its dependencies.
 */
@DisplayName("GrepCommand Component Tests")
public class GrepCommandComponentTest extends ComponentTest {
    
    @Mock
    private ItemService itemService;
    
    @Mock
    private SearchService searchService;
    
    private CommandExecutor commandExecutor;
    private ContextManager contextManager;
    
    private DefaultWorkItem item1;
    private DefaultWorkItem item2;
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Set up System.out/err capturing
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Create a real ContextManager
        contextManager = ContextManager.getInstance();
        
        // Create the command executor with test dependencies
        commandExecutor = new CommandExecutor(itemService, searchService);
        
        // Set up test work items
        setupWorkItems();
        
        // Reset captors between tests
        outputCaptor.reset();
        errorCaptor.reset();
    }
    
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    /**
     * Sets up test work items.
     */
    private void setupWorkItems() {
        // Create real work items
        item1 = new DefaultWorkItem(
                UUID.randomUUID(),
                "API implementation task",
                "Create the REST API endpoints for user management",
                WorkItemType.TASK,
                Priority.HIGH,
                WorkflowState.IN_PROGRESS,
                "developer1",
                "manager1",
                Instant.now().minusSeconds(86400),
                Instant.now()
        );
        
        item2 = new DefaultWorkItem(
                UUID.randomUUID(),
                "Documentation task",
                "Create API documentation for developers",
                WorkItemType.DOCUMENTATION,
                Priority.MEDIUM,
                WorkflowState.READY,
                "developer2",
                "manager1",
                Instant.now().minusSeconds(172800),
                Instant.now().minusSeconds(3600)
        );
        
        // Set up item service mock
        when(itemService.getAllWorkItems()).thenReturn(Arrays.asList(item1, item2));
        when(itemService.getItem(item1.id())).thenReturn(item1);
        when(itemService.getItem(item2.id())).thenReturn(item2);
        
        // Set up search service mock to use real search logic on the test items
        when(searchService.searchWorkItems(anyString(), anyBoolean(), anyBoolean()))
                .thenAnswer(invocation -> {
                    String pattern = invocation.getArgument(0);
                    boolean caseSensitive = invocation.getArgument(1);
                    boolean exactMatch = invocation.getArgument(2);
                    
                    // Simplified search implementation for testing
                    return RealSearchImplementation.search(
                            Arrays.asList(item1, item2), pattern, caseSensitive, exactMatch);
                });
    }
    
    @Test
    @DisplayName("GrepCommand should correctly interact with SearchService")
    void grepCommandShouldCorrectlyInteractWithSearchService() {
        // Setup
        GrepCommand grepCommand = new GrepCommand();
        grepCommand.setPattern("API");
        
        // Execute
        commandExecutor.executeCommand(grepCommand);
        
        // Verify
        verify(searchService).searchWorkItems(eq("API"), eq(false), eq(false));
        
        // Check for output indicating matches
        String output = outputCaptor.toString();
        assertTrue(output.contains("API implementation task") || 
                output.contains("Create API documentation"),
                "Output should contain matching items");
    }
    
    @ParameterizedTest
    @CsvSource({
        "API, false, false, true",  // Case-insensitive search for API
        "api, false, false, true",  // Case-insensitive search for api
        "API, true, false, true",   // Case-sensitive search for API
        "api, true, false, false",  // Case-sensitive search for api (no matches)
    })
    @DisplayName("GrepCommand should respect case sensitivity settings")
    void grepCommandShouldRespectCaseSensitivitySettings(
            String pattern, boolean caseSensitive, boolean exactMatch, boolean shouldMatch) {
        
        // Special mock for case-sensitive search testing
        when(searchService.searchWorkItems(eq(pattern), eq(caseSensitive), eq(exactMatch)))
                .thenReturn(shouldMatch ? 
                        RealSearchImplementation.search(Arrays.asList(item1, item2), pattern, caseSensitive, exactMatch) : 
                        Collections.emptyList());
        
        // Setup
        GrepCommand grepCommand = new GrepCommand();
        grepCommand.setPattern(pattern);
        grepCommand.setCaseSensitive(caseSensitive);
        grepCommand.setExactMatch(exactMatch);
        
        // Execute
        commandExecutor.executeCommand(grepCommand);
        
        // Verify
        verify(searchService).searchWorkItems(eq(pattern), eq(caseSensitive), eq(exactMatch));
        
        // Check for appropriate output
        String output = outputCaptor.toString();
        if (shouldMatch) {
            assertTrue(output.contains("matches found"), 
                    "Output should indicate matches were found");
        } else {
            assertTrue(output.contains("No matches found"), 
                    "Output should indicate no matches were found");
        }
    }
    
    @Test
    @DisplayName("GrepCommand should update search history in ContextManager")
    void grepCommandShouldUpdateSearchHistoryInContextManager() {
        // Setup
        GrepCommand grepCommand = new GrepCommand();
        grepCommand.setPattern("documentation");
        
        // Pre-condition: search history should be empty or not contain our search
        assertFalse(contextManager.getSearchHistory().contains("documentation"), 
                "Search history should not contain our search term yet");
        
        // Execute with the commandExecutor to ensure ContextManager interaction
        commandExecutor.executeCommand(grepCommand);
        
        // Verify search term is added to history
        assertTrue(contextManager.getSearchHistory().contains("documentation"), 
                "Search history should contain our search term");
    }
    
    @Test
    @DisplayName("CommandExecutor should correctly handle pipelines with GrepCommand")
    void commandExecutorShouldCorrectlyHandlePipelinesWithGrepCommand() {
        // Set up command pipeline: ls | grep API
        commandExecutor.executePipeline("ls | grep API");
        
        // Verify
        verify(itemService).getAllWorkItems(); // ls command should get all items
        verify(searchService).searchWorkItems(eq("API"), anyBoolean(), anyBoolean());
        
        // Output should contain filtered results
        String output = outputCaptor.toString();
        assertTrue(output.contains("API"), "Output should contain grep results");
    }
    
    // Simplified test search implementation
    static class RealSearchImplementation {
        
        public static List<SearchResult> search(List<DefaultWorkItem> items, String pattern, 
                                              boolean caseSensitive, boolean exactMatch) {
            List<SearchResult> results = new ArrayList<>();
            
            for (DefaultWorkItem item : items) {
                searchInField(item.id(), "title", item.title(), pattern, caseSensitive, exactMatch, results);
                searchInField(item.id(), "description", item.description(), pattern, caseSensitive, exactMatch, results);
            }
            
            return results;
        }
        
        private static void searchInField(UUID itemId, String field, String text, String pattern, 
                                        boolean caseSensitive, boolean exactMatch, List<SearchResult> results) {
            String textToSearch = caseSensitive ? text : text.toLowerCase();
            String patternToSearch = caseSensitive ? pattern : pattern.toLowerCase();
            
            if (exactMatch) {
                // Split by word boundaries and look for exact matches
                String[] words = textToSearch.split("\\W+");
                for (int i = 0; i < words.length; i++) {
                    if (words[i].equals(patternToSearch)) {
                        int start = textToSearch.indexOf(words[i]);
                        if (start >= 0) {
                            List<SearchResult.Match> matches = Collections.singletonList(
                                    new SearchResult.Match(start, start + words[i].length()));
                            results.add(new SearchResult(itemId, field, text, matches));
                        }
                    }
                }
            } else {
                // Look for pattern as a substring
                int start = textToSearch.indexOf(patternToSearch);
                if (start >= 0) {
                    List<SearchResult.Match> matches = Collections.singletonList(
                            new SearchResult.Match(start, start + patternToSearch.length()));
                    results.add(new SearchResult(itemId, field, text, matches));
                }
            }
        }
    }
    
    // Search result model for component testing
    static class SearchResult {
        private final UUID itemId;
        private final String field;
        private final String text;
        private final List<Match> matches;
        
        public SearchResult(UUID itemId, String field, String text, List<Match> matches) {
            this.itemId = itemId;
            this.field = field;
            this.text = text;
            this.matches = matches;
        }
        
        public UUID getItemId() { return itemId; }
        public String getField() { return field; }
        public String getText() { return text; }
        public List<Match> getMatches() { return matches; }
        
        static class Match {
            private final int start;
            private final int end;
            
            public Match(int start, int end) {
                this.start = start;
                this.end = end;
            }
            
            public int getStart() { return start; }
            public int getEnd() { return end; }
        }
    }
}