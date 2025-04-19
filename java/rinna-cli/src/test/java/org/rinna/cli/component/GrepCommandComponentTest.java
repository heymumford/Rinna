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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.rinna.cli.command.GrepCommand;
import org.rinna.cli.domain.model.SearchResult;
import org.rinna.cli.domain.model.SearchResult.Match;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.ItemService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockSearchService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Component test for the GrepCommand class, focusing on:
 * - Proper integration with MetadataService for operation tracking
 * - Correct handling of different search options and parameters
 * - Integration with search and item services
 * - Proper output formatting based on format options
 * - Error handling and parameter validation
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GrepCommand Component Tests")
public class GrepCommandComponentTest {

    private static final String OPERATION_ID = "op-12345";
    private static final String SUB_OPERATION_ID = "subop-12345";

    @Mock
    private ServiceManager mockServiceManager;

    @Mock
    private MetadataService mockMetadataService;

    @Mock
    private MockSearchService mockSearchService;

    @Mock
    private ItemService mockItemService;

    @Mock
    private ContextManager mockContextManager;

    private GrepCommand grepCommand;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;

    @BeforeEach
    void setUp() {
        // Configure mock service manager
        when(mockServiceManager.getMetadataService()).thenReturn(mockMetadataService);
        when(mockServiceManager.getSearchService()).thenReturn(mockSearchService);
        when(mockServiceManager.getMockSearchService()).thenReturn(mockSearchService);
        when(mockServiceManager.getItemService()).thenReturn(mockItemService);
        when(mockServiceManager.getMockItemService()).thenReturn(mockItemService);

        // Configure mock metadata service
        when(mockMetadataService.startOperation(anyString(), anyString(), anyMap())).thenReturn(OPERATION_ID);

        // Initialize the command with mocked services
        grepCommand = new GrepCommand(mockServiceManager);
        
        // Redirect stdout and stderr for output validation
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }

    // Helper method to create sample work items
    private List<WorkItem> createSampleWorkItems() {
        List<WorkItem> items = new ArrayList<>();
        
        WorkItem item1 = new WorkItem();
        item1.setId(UUID.randomUUID().toString());
        item1.setTitle("Important task with search term");
        item1.setDescription("This is a description that contains the search term we're looking for");
        item1.setType(WorkItemType.TASK);
        item1.setPriority(Priority.HIGH);
        item1.setState(WorkflowState.IN_PROGRESS);
        items.add(item1);
        
        WorkItem item2 = new WorkItem();
        item2.setId(UUID.randomUUID().toString());
        item2.setTitle("Another task");
        item2.setDescription("This description does not have what we need");
        item2.setType(WorkItemType.TASK);
        item2.setPriority(Priority.MEDIUM);
        item2.setState(WorkflowState.TO_DO);
        items.add(item2);
        
        return items;
    }

    // Helper method to create sample search results
    private List<SearchResult> createSampleSearchResults() {
        List<SearchResult> results = new ArrayList<>();
        
        // Create a sample work item
        WorkItem item = createSampleWorkItems().get(0);
        UUID itemId = UUID.fromString(item.getId());
        
        // Create search result for title match
        SearchResult titleResult = new SearchResult(
            itemId, 
            item.getTitle(),
            1, 
            "workitem.title", 
            item.getTitle(),
            "search"
        );
        
        // Add match to title result
        Match titleMatch = new Match(17, 23, "search");
        titleResult.addMatch(titleMatch);
        results.add(titleResult);
        
        // Create search result for description match
        SearchResult descResult = new SearchResult(
            itemId, 
            item.getDescription(),
            1, 
            "workitem.description", 
            item.getDescription(),
            "search"
        );
        
        // Add match to description result
        Match descMatch = new Match(42, 48, "search");
        descResult.addMatch(descMatch);
        results.add(descResult);
        
        return results;
    }

    @Nested
    @DisplayName("Service Integration Tests")
    class ServiceIntegrationTests {
        
        @Test
        @DisplayName("Should integrate with MetadataService for operation tracking")
        void shouldIntegrateWithMetadataServiceForOperationTracking() {
            // Given
            grepCommand.setPattern("search");
            when(mockSearchService.findText(anyString(), anyBoolean())).thenReturn(createSampleWorkItems());
            when(mockItemService.getItem(anyString())).thenReturn(createSampleWorkItems().get(0));
            
            // When
            int exitCode = grepCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify operation tracking
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).startOperation(eq("grep"), eq("SEARCH"), paramsCaptor.capture());
            
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals("search", params.get("pattern"));
            assertFalse((Boolean) params.get("case_sensitive"));
            
            // Verify operation completion
            ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
            
            Map<String, Object> result = resultCaptor.getValue();
            assertTrue(result.containsKey("matches_found"));
            assertTrue(result.containsKey("items_matched"));
            assertEquals("search", result.get("pattern"));
        }
        
        @Test
        @DisplayName("Should integrate with SearchService for finding text")
        void shouldIntegrateWithSearchServiceForFindingText() {
            // Given
            grepCommand.setPattern("search");
            when(mockSearchService.findText(anyString(), anyBoolean())).thenReturn(createSampleWorkItems());
            when(mockItemService.getItem(anyString())).thenReturn(createSampleWorkItems().get(0));
            
            // When
            int exitCode = grepCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify search service was called with correct parameters
            verify(mockSearchService).findText("search", false);
        }
        
        @Test
        @DisplayName("Should integrate with ItemService for retrieving work item details")
        void shouldIntegrateWithItemServiceForRetrievingWorkItemDetails() {
            // Given
            grepCommand.setPattern("search");
            List<WorkItem> workItems = createSampleWorkItems();
            when(mockSearchService.findText(anyString(), anyBoolean())).thenReturn(workItems);
            when(mockItemService.getItem(anyString())).thenReturn(workItems.get(0));
            
            // When
            int exitCode = grepCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify item service was called to get details
            verify(mockItemService, atLeastOnce()).getItem(anyString());
        }
        
        @Test
        @DisplayName("Should integrate with ContextManager for search history")
        void shouldIntegrateWithContextManagerForSearchHistory() {
            // Given
            try (MockedStatic<ContextManager> mockedContextManager = Mockito.mockStatic(ContextManager.class)) {
                mockedContextManager.when(ContextManager::getInstance).thenReturn(mockContextManager);
                
                grepCommand.setPattern("search");
                when(mockSearchService.findText(anyString(), anyBoolean())).thenReturn(createSampleWorkItems());
                when(mockItemService.getItem(anyString())).thenReturn(createSampleWorkItems().get(0));
                
                // When
                int exitCode = grepCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify context manager was used to store search history
                verify(mockContextManager).addToSearchHistory("search");
            }
        }
    }
    
    @Nested
    @DisplayName("Command Options Tests")
    class CommandOptionsTests {
        
        @Test
        @DisplayName("Should handle case-sensitive search option")
        void shouldHandleCaseSensitiveSearchOption() {
            // Given
            grepCommand.setPattern("Search");
            grepCommand.setCaseSensitive(true);
            when(mockSearchService.findText(anyString(), anyBoolean())).thenReturn(createSampleWorkItems());
            when(mockItemService.getItem(anyString())).thenReturn(createSampleWorkItems().get(0));
            
            // When
            int exitCode = grepCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify search service was called with case-sensitive=true
            verify(mockSearchService).findText("Search", true);
            
            // Verify operation parameters included case sensitivity
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).startOperation(eq("grep"), eq("SEARCH"), paramsCaptor.capture());
            
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(true, params.get("case_sensitive"));
        }
        
        @Test
        @DisplayName("Should handle exact match option")
        void shouldHandleExactMatchOption() {
            // Given
            grepCommand.setPattern("search");
            grepCommand.setExactMatch(true);
            when(mockSearchService.findText(anyString(), anyBoolean())).thenReturn(createSampleWorkItems());
            when(mockItemService.getItem(anyString())).thenReturn(createSampleWorkItems().get(0));
            
            // When
            int exitCode = grepCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify operation parameters included exact match
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).startOperation(eq("grep"), eq("SEARCH"), paramsCaptor.capture());
            
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(true, params.get("exact_match"));
        }
        
        @Test
        @DisplayName("Should handle context lines option")
        void shouldHandleContextLinesOption() {
            // Given
            grepCommand.setPattern("search");
            grepCommand.setContext(3);
            when(mockSearchService.findText(anyString(), anyBoolean())).thenReturn(createSampleWorkItems());
            when(mockItemService.getItem(anyString())).thenReturn(createSampleWorkItems().get(0));
            
            // When
            int exitCode = grepCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify operation parameters included context
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).startOperation(eq("grep"), eq("SEARCH"), paramsCaptor.capture());
            
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(3, params.get("context"));
        }
        
        @Test
        @DisplayName("Should handle count only option")
        void shouldHandleCountOnlyOption() {
            // Given
            grepCommand.setPattern("search");
            grepCommand.setCountOnly(true);
            when(mockSearchService.findText(anyString(), anyBoolean())).thenReturn(createSampleWorkItems());
            when(mockItemService.getItem(anyString())).thenReturn(createSampleWorkItems().get(0));
            
            // When
            int exitCode = grepCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify operation parameters included count_only
            ArgumentCaptor<Map<String, Object>> paramsCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).startOperation(eq("grep"), eq("SEARCH"), paramsCaptor.capture());
            
            Map<String, Object> params = paramsCaptor.getValue();
            assertEquals(true, params.get("count_only"));
            
            // Verify output contains count summary
            String output = outputStream.toString();
            assertTrue(output.contains("Total matches:"));
            assertTrue(output.contains("Matched work items:"));
        }
    }
    
    @Nested
    @DisplayName("Output Format Tests")
    class OutputFormatTests {
        
        @Test
        @DisplayName("Should output in text format by default")
        void shouldOutputInTextFormatByDefault() {
            // Given
            grepCommand.setPattern("search");
            when(mockSearchService.findText(anyString(), anyBoolean())).thenReturn(createSampleWorkItems());
            when(mockItemService.getItem(anyString())).thenReturn(createSampleWorkItems().get(0));
            
            // When
            int exitCode = grepCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify text output
            String output = outputStream.toString();
            assertTrue(output.contains("Search results for: search"));
            assertTrue(output.contains("Work Item:"));
            assertTrue(output.contains("Type:"));
        }
        
        @Test
        @DisplayName("Should output in JSON format when specified")
        void shouldOutputInJsonFormatWhenSpecified() {
            // Given
            try (MockedStatic<OutputFormatter> mockedOutputFormatter = Mockito.mockStatic(OutputFormatter.class)) {
                mockedOutputFormatter.when(() -> OutputFormatter.toJson(any(), anyBoolean()))
                    .thenReturn("{\"json\":\"output\"}");
                
                grepCommand.setPattern("search");
                grepCommand.setOutputFormat("json");
                when(mockSearchService.findText(anyString(), anyBoolean())).thenReturn(createSampleWorkItems());
                when(mockItemService.getItem(anyString())).thenReturn(createSampleWorkItems().get(0));
                
                // When
                int exitCode = grepCommand.call();
                
                // Then
                assertEquals(0, exitCode);
                
                // Verify JSON output was generated
                mockedOutputFormatter.verify(() -> OutputFormatter.toJson(any(), eq(false)));
                
                // Verify output contains JSON string
                String output = outputStream.toString();
                assertTrue(output.contains("{\"json\":\"output\"}"));
            }
        }
        
        @Test
        @DisplayName("Should output in CSV format when specified")
        void shouldOutputInCsvFormatWhenSpecified() {
            // Given
            grepCommand.setPattern("search");
            grepCommand.setOutputFormat("csv");
            when(mockSearchService.findText(anyString(), anyBoolean())).thenReturn(createSampleWorkItems());
            when(mockItemService.getItem(anyString())).thenReturn(createSampleWorkItems().get(0));
            
            // When
            int exitCode = grepCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify CSV output
            String output = outputStream.toString();
            assertTrue(output.contains("WorkItemId,Title,Type,Priority,Status,MatchText,MatchStart,MatchEnd"));
        }
    }
    
    @Nested
    @DisplayName("Error Handling Tests")
    class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should track operation failure when no search pattern provided")
        void shouldTrackOperationFailureWhenNoSearchPatternProvided() {
            // Given
            // No pattern set
            
            // When
            int exitCode = grepCommand.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify error message
            String error = errorStream.toString();
            assertTrue(error.contains("No search pattern provided"));
            
            // Verify operation failure was tracked
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalArgumentException.class));
        }
        
        @Test
        @DisplayName("Should track operation failure when search service is unavailable")
        void shouldTrackOperationFailureWhenSearchServiceIsUnavailable() {
            // Given
            grepCommand.setPattern("search");
            when(mockServiceManager.getSearchService()).thenReturn(null);
            
            // When
            int exitCode = grepCommand.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify error message
            String error = errorStream.toString();
            assertTrue(error.contains("Search service not available"));
            
            // Verify operation failure was tracked
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(IllegalStateException.class));
        }
        
        @Test
        @DisplayName("Should track operation failure when exception occurs during search")
        void shouldTrackOperationFailureWhenExceptionOccursDuringSearch() {
            // Given
            grepCommand.setPattern("search");
            when(mockSearchService.findText(anyString(), anyBoolean()))
                .thenThrow(new RuntimeException("Search service error"));
            
            // When
            int exitCode = grepCommand.call();
            
            // Then
            assertEquals(1, exitCode);
            
            // Verify error message
            String error = errorStream.toString();
            assertTrue(error.contains("Error:"));
            
            // Verify operation failure was tracked
            verify(mockMetadataService).failOperation(eq(OPERATION_ID), any(RuntimeException.class));
        }
    }
    
    @Nested
    @DisplayName("Empty Results Handling Tests")
    class EmptyResultsHandlingTests {
        
        @Test
        @DisplayName("Should handle no matches found")
        void shouldHandleNoMatchesFound() {
            // Given
            grepCommand.setPattern("nonexistent");
            when(mockSearchService.findText(anyString(), anyBoolean())).thenReturn(new ArrayList<>());
            
            // When
            int exitCode = grepCommand.call();
            
            // Then
            assertEquals(0, exitCode);
            
            // Verify output message
            String output = outputStream.toString();
            assertTrue(output.contains("No matches found for: nonexistent"));
            
            // Verify operation completion with zero results
            ArgumentCaptor<Map<String, Object>> resultCaptor = ArgumentCaptor.forClass(Map.class);
            verify(mockMetadataService).completeOperation(eq(OPERATION_ID), resultCaptor.capture());
            
            Map<String, Object> result = resultCaptor.getValue();
            assertEquals(0, result.get("matches_found"));
            assertEquals("nonexistent", result.get("pattern"));
        }
    }
}