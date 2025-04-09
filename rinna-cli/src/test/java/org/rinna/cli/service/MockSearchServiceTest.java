/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.rinna.cli.domain.model.SearchResult;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("MockSearchService Tests")
class MockSearchServiceTest {
    private MockSearchService searchService;
    
    @BeforeEach
    void setUp() {
        // Create a subclass that overrides initialize to avoid ServiceManager dependency
        searchService = new MockSearchService() {
            @Override
            public void initialize() {
                // Do nothing - we'll use the sample items
            }
        };
    }
    
    @Nested
    @DisplayName("Sample Data Tests")
    class SampleDataTests {
        @Test
        @DisplayName("Should provide sample work items")
        void shouldProvideSampleWorkItems() {
            // When
            List<WorkItem> items = searchService.getAllItems();
            
            // Then
            assertNotNull(items);
            assertEquals(3, items.size());
            
            // Check specific items and their properties
            WorkItem authItem = items.stream()
                .filter(item -> item.getTitle().equals("Implement authentication feature"))
                .findFirst()
                .orElse(null);
            assertNotNull(authItem);
            assertEquals("Create JWT-based authentication for API endpoints", authItem.getDescription());
            assertEquals(WorkItemType.TASK, authItem.getType());
            assertEquals(Priority.MEDIUM, authItem.getPriority());
            assertEquals(WorkflowState.READY, authItem.getState());
            
            WorkItem bugItem = items.stream()
                .filter(item -> item.getTitle().equals("Fix bug in payment module"))
                .findFirst()
                .orElse(null);
            assertNotNull(bugItem);
            assertEquals("Transaction history is not updating after payment completion", bugItem.getDescription());
        }
    }
    
    @Nested
    @DisplayName("Basic Search Tests")
    class BasicSearchTests {
        @Test
        @DisplayName("Should search by text in title")
        void shouldSearchByTextInTitle() {
            // When
            List<WorkItem> results = searchService.searchByText("authentication");
            
            // Then
            assertEquals(1, results.size());
            assertEquals("Implement authentication feature", results.get(0).getTitle());
        }
        
        @Test
        @DisplayName("Should search by text in description")
        void shouldSearchByTextInDescription() {
            // When
            List<WorkItem> results = searchService.searchByText("API");
            
            // Then
            assertEquals(2, results.size()); // Should find both authentication item and documentation item
            
            boolean foundAuth = results.stream()
                .anyMatch(item -> item.getTitle().equals("Implement authentication feature"));
            boolean foundDocs = results.stream()
                .anyMatch(item -> item.getTitle().equals("Update documentation"));
                
            assertTrue(foundAuth);
            assertTrue(foundDocs);
        }
        
        @Test
        @DisplayName("Should return empty list for no matches")
        void shouldReturnEmptyListForNoMatches() {
            // When
            List<WorkItem> results = searchService.searchByText("nonexistent");
            
            // Then
            assertNotNull(results);
            assertTrue(results.isEmpty());
        }
        
        @Test
        @DisplayName("Should handle case-insensitive search by default")
        void shouldHandleCaseInsensitiveSearchByDefault() {
            // When
            List<WorkItem> results = searchService.searchByText("AUTHENTICATION");
            
            // Then
            assertEquals(1, results.size());
            assertEquals("Implement authentication feature", results.get(0).getTitle());
        }
    }
    
    @Nested
    @DisplayName("Filtered Search Tests")
    class FilteredSearchTests {
        @Test
        @DisplayName("Should search by text and state")
        void shouldSearchByTextAndState() {
            // When
            List<WorkItem> results = searchService.searchByTextAndState("API", WorkflowState.READY);
            
            // Then
            assertFalse(results.isEmpty());
            for (WorkItem item : results) {
                assertTrue(
                    item.getTitle().toLowerCase().contains("api") || 
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains("api"))
                );
                assertEquals(WorkflowState.READY, item.getState());
            }
        }
        
        @Test
        @DisplayName("Should search by text and type")
        void shouldSearchByTextAndType() {
            // When
            List<WorkItem> results = searchService.searchByTextAndType("API", WorkItemType.TASK);
            
            // Then
            assertFalse(results.isEmpty());
            for (WorkItem item : results) {
                assertTrue(
                    item.getTitle().toLowerCase().contains("api") || 
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains("api"))
                );
                assertEquals(WorkItemType.TASK, item.getType());
            }
        }
        
        @Test
        @DisplayName("Should search by text and priority")
        void shouldSearchByTextAndPriority() {
            // When
            List<WorkItem> results = searchService.searchByTextAndPriority("API", Priority.MEDIUM);
            
            // Then
            assertFalse(results.isEmpty());
            for (WorkItem item : results) {
                assertTrue(
                    item.getTitle().toLowerCase().contains("api") || 
                    (item.getDescription() != null && item.getDescription().toLowerCase().contains("api"))
                );
                assertEquals(Priority.MEDIUM, item.getPriority());
            }
        }
    }
    
    @Nested
    @DisplayName("Advanced Search Tests")
    class AdvancedSearchTests {
        @Test
        @DisplayName("Should find text with matches")
        void shouldFindTextWithMatches() {
            // When
            List<SearchResult> results = searchService.findText("API");
            
            // Then
            assertFalse(results.isEmpty());
            
            for (SearchResult result : results) {
                assertNotNull(result.getItemId());
                assertFalse(result.getMatches().isEmpty());
                
                // Verify each match contains the search term
                for (SearchResult.Match match : result.getMatches()) {
                    assertTrue(match.getMatchedText().toLowerCase().contains("api"));
                    assertTrue(match.getStartIndex() >= 0);
                    assertTrue(match.getEndIndex() > match.getStartIndex());
                }
            }
        }
        
        @Test
        @DisplayName("Should support case sensitive search")
        void shouldSupportCaseSensitiveSearch() {
            // When
            List<SearchResult> caseInsensitiveResults = searchService.findText("api", false);
            List<SearchResult> caseSensitiveResults = searchService.findText("API", true);
            
            // Then
            assertFalse(caseInsensitiveResults.isEmpty());
            
            boolean hasDifference = caseInsensitiveResults.size() != caseSensitiveResults.size();
            if (!hasDifference) {
                // Check if matches are different
                for (int i = 0; i < caseInsensitiveResults.size(); i++) {
                    if (caseInsensitiveResults.get(i).getMatches().size() != 
                        caseSensitiveResults.get(i).getMatches().size()) {
                        hasDifference = true;
                        break;
                    }
                }
            }
            
            // There should be a difference between case-sensitive and case-insensitive searches
            // Note: This might not always be true depending on the test data case, but it should be for "API"
            assertTrue(hasDifference);
        }
        
        @Test
        @DisplayName("Should find pattern with regex")
        void shouldFindPatternWithRegex() {
            // When: Search with a regex pattern
            List<SearchResult> results = searchService.findPattern(".*ment.*");
            
            // Then: Should find items with "ment" substring
            assertFalse(results.isEmpty());
            
            boolean foundPayment = false;
            boolean foundImplement = false;
            boolean foundDocument = false;
            
            for (SearchResult result : results) {
                for (SearchResult.Match match : result.getMatches()) {
                    String matchText = match.getMatchedText();
                    if (matchText.contains("payment")) foundPayment = true;
                    if (matchText.contains("Implement")) foundImplement = true;
                    if (matchText.contains("document")) foundDocument = true;
                }
            }
            
            assertTrue(foundPayment || foundImplement || foundDocument);
        }
        
        @Test
        @DisplayName("Should support case sensitive pattern search")
        void shouldSupportCaseSensitivePatternSearch() {
            // When
            List<SearchResult> caseInsensitiveResults = searchService.findPattern("api", false);
            List<SearchResult> caseSensitiveResults = searchService.findPattern("API", true);
            
            // Then
            assertFalse(caseInsensitiveResults.isEmpty());
            
            boolean hasDifference = caseInsensitiveResults.size() != caseSensitiveResults.size();
            if (!hasDifference) {
                // Check if matches are different
                for (int i = 0; i < caseInsensitiveResults.size(); i++) {
                    if (caseInsensitiveResults.get(i).getMatches().size() != 
                        caseSensitiveResults.get(i).getMatches().size()) {
                        hasDifference = true;
                        break;
                    }
                }
            }
            
            // There should be a difference between case-sensitive and case-insensitive searches
            assertTrue(hasDifference);
        }
    }
    
    @Nested
    @DisplayName("Multiple Criteria Search Tests")
    class MultipleCriteriaSearchTests {
        @Test
        @DisplayName("Should find items matching single criterion")
        void shouldFindItemsMatchingSingleCriterion() {
            // Given
            Map<String, String> criteria = new HashMap<>();
            criteria.put("type", "TASK");
            
            // When
            List<WorkItem> results = searchService.findWorkItems(criteria, 10);
            
            // Then
            assertFalse(results.isEmpty());
            for (WorkItem item : results) {
                assertEquals(WorkItemType.TASK, item.getType());
            }
        }
        
        @Test
        @DisplayName("Should find items matching multiple criteria")
        void shouldFindItemsMatchingMultipleCriteria() {
            // Given
            Map<String, String> criteria = new HashMap<>();
            criteria.put("type", "TASK");
            criteria.put("state", "READY");
            
            // When
            List<WorkItem> results = searchService.findWorkItems(criteria, 10);
            
            // Then
            assertFalse(results.isEmpty());
            for (WorkItem item : results) {
                assertEquals(WorkItemType.TASK, item.getType());
                assertEquals(WorkflowState.READY, item.getState());
            }
        }
        
        @Test
        @DisplayName("Should handle title and description criteria")
        void shouldHandleTitleAndDescriptionCriteria() {
            // Given
            Map<String, String> criteria = new HashMap<>();
            criteria.put("title", "authentication");
            
            // When
            List<WorkItem> results = searchService.findWorkItems(criteria, 10);
            
            // Then
            assertEquals(1, results.size());
            assertEquals("Implement authentication feature", results.get(0).getTitle());
            
            // Try description search
            criteria.clear();
            criteria.put("description", "JWT");
            
            // When
            results = searchService.findWorkItems(criteria, 10);
            
            // Then
            assertEquals(1, results.size());
            assertEquals("Create JWT-based authentication for API endpoints", results.get(0).getDescription());
        }
        
        @Test
        @DisplayName("Should respect limit parameter")
        void shouldRespectLimitParameter() {
            // Given
            Map<String, String> criteria = new HashMap<>();
            criteria.put("type", "TASK");
            
            // When
            List<WorkItem> allResults = searchService.findWorkItems(criteria, 0); // No limit
            List<WorkItem> limitedResults = searchService.findWorkItems(criteria, 1); // Limit to 1
            
            // Then
            assertTrue(allResults.size() > 1); // Should have multiple task items
            assertEquals(1, limitedResults.size()); // Should be limited to 1
        }
        
        @Test
        @DisplayName("Should ignore unknown criteria")
        void shouldIgnoreUnknownCriteria() {
            // Given
            Map<String, String> criteria = new HashMap<>();
            criteria.put("nonexistentField", "value");
            
            // When
            List<WorkItem> results = searchService.findWorkItems(criteria, 10);
            
            // Then
            assertEquals(searchService.getAllItems().size(), results.size());
        }
    }
}