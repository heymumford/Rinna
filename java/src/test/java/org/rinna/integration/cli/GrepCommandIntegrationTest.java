/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.integration.cli;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.UUID;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.rinna.base.IntegrationTest;
import org.rinna.cli.RinnaCli;
import org.rinna.cli.service.ContextManager;
import org.rinna.domain.Priority;
import org.rinna.domain.WorkItemType;
import org.rinna.domain.WorkflowState;
import org.rinna.service.DefaultSearchService;
import org.rinna.service.InMemoryItemService;
import org.rinna.service.ItemServiceFactory;
import org.rinna.service.SearchServiceFactory;

/**
 * Integration tests for the Grep command in the CLI.
 * These tests verify that the grep command correctly integrates with real service implementations.
 */
@DisplayName("GrepCommand Integration Tests")
public class GrepCommandIntegrationTest extends IntegrationTest {
    
    private InMemoryItemService itemService;
    private DefaultSearchService searchService;
    private RinnaCli cli;
    
    private UUID item1Id;
    private UUID item2Id;
    private UUID item3Id;
    
    private final ByteArrayOutputStream outputCaptor = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorCaptor = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    // Data provider for testing different command combinations
    static Stream<Arguments> grepCommands() {
        return Stream.of(
            // pattern, flags, expectedMatches, expectedSuccess
            Arguments.of("API", "", 3, true),                  // Basic search
            Arguments.of("api", "-i", 3, true),                // Case-insensitive (default)
            Arguments.of("api", "-c", 3, true),                // Count only
            Arguments.of("api documentation", "", 1, true),    // Phrase search
            Arguments.of("nonexistent", "", 0, true),          // No matches
            Arguments.of("", "", 0, false)                     // Empty pattern (should fail)
        );
    }
    
    @BeforeEach
    void setUp() {
        // Set up System.out/err capturing
        System.setOut(new PrintStream(outputCaptor));
        System.setErr(new PrintStream(errorCaptor));
        
        // Reset captors between tests
        outputCaptor.reset();
        errorCaptor.reset();
        
        // Create real services with in-memory storage
        itemService = (InMemoryItemService) ItemServiceFactory.createItemService();
        searchService = (DefaultSearchService) SearchServiceFactory.createSearchService();
        
        // Register services
        ServiceRegistry.registerService(ItemService.class, itemService);
        ServiceRegistry.registerService(SearchService.class, searchService);
        
        // Create CLI instance
        cli = new RinnaCli();
        
        // Create test data
        setupTestData();
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Clear context
        ContextManager.getInstance().clearContext();
        
        // Clear test data
        itemService.clearAll();
    }
    
    /**
     * Sets up test data for integration tests.
     */
    private void setupTestData() {
        // Create test work items
        item1Id = itemService.createWorkItem(
                "API authentication task",
                "Implement the OAuth2 API authentication flow",
                WorkItemType.TASK,
                Priority.HIGH,
                "dev1"
        );
        
        item2Id = itemService.createWorkItem(
                "Database schema design",
                "Create schema for user management including API keys",
                WorkItemType.TASK,
                Priority.MEDIUM,
                "dev2"
        );
        
        item3Id = itemService.createWorkItem(
                "API documentation",
                "Create detailed API reference documentation",
                WorkItemType.DOCUMENTATION,
                Priority.LOW,
                "dev3"
        );
        
        // Set some work item states
        itemService.updateState(item1Id, WorkflowState.IN_PROGRESS, "system");
        itemService.updateState(item2Id, WorkflowState.READY, "system");
        itemService.updateState(item3Id, WorkflowState.BACKLOG, "system");
    }
    
    @Test
    @DisplayName("Grep command should find items containing a search term")
    void grepCommandShouldFindItemsContainingSearchTerm() {
        // Execute
        int result = cli.execute(new String[]{"grep", "API"});
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Should find all items containing "API"
        assertTrue(output.contains("API authentication task"), "Output should contain item1 title");
        assertTrue(output.contains("API keys"), "Output should contain item2 description match");
        assertTrue(output.contains("API documentation"), "Output should contain item3 title");
        assertTrue(output.contains("API reference"), "Output should contain item3 description match");
        
        // Should format output correctly
        assertTrue(output.contains("matches found"), "Output should indicate matches found");
        assertTrue(output.contains("\u001B[1;31mAPI\u001B[0m"), "Output should highlight matches");
    }
    
    @Test
    @DisplayName("Case-sensitive grep should respect case")
    void caseSensitiveGrepShouldRespectCase() {
        // Execute
        int result = cli.execute(new String[]{"grep", "-s", "api"});
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Should not find uppercase "API" with lowercase "api" search
        assertFalse(output.contains("API authentication"), "Case-sensitive output should not match uppercase");
        assertTrue(output.contains("No matches found"), "Output should indicate no matches with case sensitivity");
    }
    
    @Test
    @DisplayName("Grep command with count option should show counts")
    void grepCommandWithCountOptionShouldShowCounts() {
        // Execute
        int result = cli.execute(new String[]{"grep", "-c", "API"});
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Should show count information
        assertTrue(output.contains("Total matches:"), "Output should contain match count");
        assertTrue(output.contains("Matched work items: 3"), "Output should indicate 3 matched items");
        
        // Should not show detailed matches
        assertFalse(output.contains("\u001B[1;31m"), "Count output should not contain highlighting");
    }
    
    @Test
    @DisplayName("Grep command with context should show surrounding text")
    void grepCommandWithContextShouldShowSurroundingText() {
        // Execute
        int result = cli.execute(new String[]{"grep", "-A", "2", "API"});
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Should show lines after the match
        assertTrue(output.contains("API authentication") && 
                output.contains("Implement the OAuth2"), 
                "Output should contain context after matches");
    }
    
    @Test
    @DisplayName("Grep command should work with pipes from ls command")
    void grepCommandShouldWorkWithPipesFromLsCommand() {
        // Execute pipeline: ls | grep API
        int result = cli.execute(new String[]{"ls", "|", "grep", "API"});
        
        // Verify
        assertEquals(0, result, "Command should succeed");
        
        String output = outputCaptor.toString();
        
        // Should contain filtered results from ls
        assertTrue(output.contains("API authentication task"), "Output should contain filtered item1");
        assertTrue(output.contains("API documentation"), "Output should contain filtered item3");
        
        // Should NOT contain all ls output
        assertFalse(output.contains("Database schema design"), 
                "Output should not contain non-matching items");
    }
    
    @ParameterizedTest
    @MethodSource("grepCommands")
    @DisplayName("Pairwise testing of grep CLI command variations")
    void pairwiseTestingOfGrepCliCommandVariations(String pattern, String flags, 
                                                 int expectedMatches, boolean expectedSuccess) {
        // Build command array
        String[] commandParts;
        if (flags.isEmpty()) {
            commandParts = new String[]{"grep", pattern};
        } else {
            String[] flagParts = flags.split("\\s+");
            commandParts = new String[2 + flagParts.length];
            commandParts[0] = "grep";
            System.arraycopy(flagParts, 0, commandParts, 1, flagParts.length);
            commandParts[commandParts.length - 1] = pattern;
        }
        
        // Execute
        int result = cli.execute(commandParts);
        
        // Verify
        if (expectedSuccess) {
            assertEquals(0, result, "Command should succeed");
            
            String output = outputCaptor.toString();
            
            if (expectedMatches > 0) {
                if (flags.contains("-c")) {
                    // Count mode
                    assertTrue(output.contains("Matched work items: " + expectedMatches), 
                            "Output should show correct match count");
                } else {
                    // Normal mode
                    assertTrue(output.contains("matches found"), 
                            "Output should indicate matches were found");
                }
            } else {
                assertTrue(output.contains("No matches found"), 
                        "Output should indicate no matches were found");
            }
        } else {
            assertEquals(1, result, "Command should fail");
            assertTrue(errorCaptor.toString().contains("Error"), 
                    "Error output should contain error message");
        }
    }
    
    @Test
    @DisplayName("Grep command should save search history in context")
    void grepCommandShouldSaveSearchHistoryInContext() {
        // Execute multiple searches
        cli.execute(new String[]{"grep", "API"});
        cli.execute(new String[]{"grep", "database"});
        cli.execute(new String[]{"grep", "documentation"});
        
        // Verify history is saved
        List<String> searchHistory = ContextManager.getInstance().getSearchHistory();
        assertTrue(searchHistory.contains("API"), "Search history should contain 'API'");
        assertTrue(searchHistory.contains("database"), "Search history should contain 'database'");
        assertTrue(searchHistory.contains("documentation"), "Search history should contain 'documentation'");
        
        // Execute a search with the history flag
        outputCaptor.reset();
        cli.execute(new String[]{"grep", "--history"});
        
        // Verify history is displayed
        String output = outputCaptor.toString();
        assertTrue(output.contains("Search History"), "Output should contain search history header");
        assertTrue(output.contains("API"), "Output should contain API in history");
        assertTrue(output.contains("database"), "Output should contain database in history");
        assertTrue(output.contains("documentation"), "Output should contain documentation in history");
    }
}