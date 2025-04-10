/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.adapter.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.rinna.domain.SearchResult;
import org.rinna.domain.WorkItem;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.SearchService;

/**
 * Default implementation of the SearchService interface.
 */
public class DefaultSearchService implements SearchService {
    
    private final ItemService itemService;
    
    /**
     * Creates a new DefaultSearchService.
     *
     * @param itemService the item service
     */
    public DefaultSearchService(ItemService itemService) {
        this.itemService = itemService;
    }
    
    @Override
    public List<SearchResult> searchWorkItems(String pattern, boolean caseSensitive, boolean exactMatch) {
        List<SearchResult> results = new ArrayList<>();
        
        // Prepare search pattern
        Pattern regexPattern = preparePattern(pattern, caseSensitive, exactMatch);
        
        // Get all work items
        List<WorkItem> workItems = itemService.getAllWorkItems();
        
        // Search in each work item
        for (WorkItem item : workItems) {
            // Search in title
            searchInField(results, item.id(), "title", item.title(), regexPattern);
            
            // Search in description
            searchInField(results, item.id(), "description", item.description(), regexPattern);
        }
        
        return results;
    }
    
    @Override
    public List<SearchResult> searchWorkItemWithContext(String pattern, int contextLines, 
                                                      boolean caseSensitive, boolean exactMatch) {
        // This is similar to the regular search, but we'll include more text context
        // and mark the matches for displaying with surrounding lines
        
        List<SearchResult> results = new ArrayList<>();
        
        // Prepare search pattern
        Pattern regexPattern = preparePattern(pattern, caseSensitive, exactMatch);
        
        // Get all work items
        List<WorkItem> workItems = itemService.getAllWorkItems();
        
        // Search in each work item
        for (WorkItem item : workItems) {
            // For title, we don't need context since it's typically short
            searchInField(results, item.id(), "title", item.title(), regexPattern);
            
            // For description, include context
            // First check if there are any matches
            Matcher matcher = regexPattern.matcher(item.description());
            if (matcher.find()) {
                // Reset matcher
                matcher.reset();
                
                // Include the full description as context
                // (in a real system, we'd extract only the relevant context lines)
                List<SearchResult.Match> matches = new ArrayList<>();
                
                while (matcher.find()) {
                    matches.add(new SearchResult.Match(
                            matcher.start(), 
                            matcher.end(), 
                            matcher.group()
                    ));
                }
                
                if (!matches.isEmpty()) {
                    results.add(new SearchResult(item.id(), "description", item.description(), matches));
                }
            }
        }
        
        return results;
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
            matches.add(new SearchResult.Match(matcher.start(), matcher.end()));
        }
        
        if (!matches.isEmpty()) {
            results.add(new SearchResult(itemId, fieldName, text, matches));
        }
    }
}