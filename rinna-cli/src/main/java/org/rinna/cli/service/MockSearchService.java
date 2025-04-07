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

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.util.ModelMapper;
import org.rinna.domain.SearchResult;
import org.rinna.domain.service.SearchService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Mock implementation of the SearchService for testing.
 */
public class MockSearchService implements SearchService {

    private final MockItemService mockItemService;
    
    /**
     * Creates a new MockSearchService.
     */
    public MockSearchService() {
        // Get the mock item service from the service manager
        this.mockItemService = (MockItemService) ServiceManager.getInstance().getItemService();
    }
    
    @Override
    public List<SearchResult> searchWorkItems(String pattern, boolean caseSensitive, boolean exactMatch) {
        List<SearchResult> results = new ArrayList<>();
        
        // Prepare search pattern
        Pattern regex = preparePattern(pattern, caseSensitive, exactMatch);
        
        // Get all work items from the CLI item service
        List<WorkItem> workItems = mockItemService.getAllCliItems();
        
        // Search in each work item
        for (WorkItem item : workItems) {
            // Search in title
            searchInField(results, UUID.fromString(item.getId()), "title", item.getTitle(), regex);
            
            // Search in description
            searchInField(results, UUID.fromString(item.getId()), "description", item.getDescription(), regex);
        }
        
        return results;
    }
    
    @Override
    public List<SearchResult> searchWorkItemWithContext(String pattern, int contextLines, 
                                                     boolean caseSensitive, boolean exactMatch) {
        // For simplicity, the mock implementation treats this the same as regular search
        // In a real implementation, this would include context lines around the matches
        return searchWorkItems(pattern, caseSensitive, exactMatch);
    }
    
    /**
     * Prepares a regex pattern based on search settings.
     *
     * @param pattern the search pattern
     * @param caseSensitive true for case-sensitive search
     * @param exactMatch true for whole word matching
     * @return the compiled pattern
     */
    private Pattern preparePattern(String pattern, boolean caseSensitive, boolean exactMatch) {
        String regex;
        
        if (exactMatch) {
            // Match whole words only
            regex = "\\b" + Pattern.quote(pattern) + "\\b";
        } else {
            // Match anywhere in text
            regex = Pattern.quote(pattern);
        }
        
        // Set case sensitivity flag
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
        
        return Pattern.compile(regex, flags);
    }
    
    /**
     * Searches for a pattern in a field and adds results.
     *
     * @param results the list to add results to
     * @param itemId the work item ID
     * @param fieldName the field name
     * @param text the text to search
     * @param pattern the search pattern
     */
    private void searchInField(List<SearchResult> results, UUID itemId, String fieldName, 
                              String text, Pattern pattern) {
        if (text == null || text.isEmpty()) {
            return;
        }
        
        Matcher matcher = pattern.matcher(text);
        List<SearchResult.Match> matches = new ArrayList<>();
        
        while (matcher.find()) {
            int start = matcher.start();
            int end = matcher.end();
            String matchedText = text.substring(start, end);
            matches.add(new SearchResult.Match(start, end, matchedText));
        }
        
        if (!matches.isEmpty()) {
            results.add(new SearchResult(itemId, fieldName, text, matches));
        }
    }
}