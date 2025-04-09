/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.usecase;

import org.rinna.domain.SearchResult;

import java.util.List;

/**
 * Service interface for searching work items.
 */
public interface SearchService {
    
    /**
     * Searches for a pattern in all work items.
     *
     * @param pattern the search pattern
     * @param caseSensitive true for case-sensitive search
     * @param exactMatch true for whole word matching
     * @return the list of search results
     */
    List<SearchResult> searchWorkItems(String pattern, boolean caseSensitive, boolean exactMatch);
    
    /**
     * Searches for a pattern in all work items with context lines.
     *
     * @param pattern the search pattern
     * @param contextLines the number of context lines to include
     * @param caseSensitive true for case-sensitive search
     * @param exactMatch true for whole word matching
     * @return the list of search results
     */
    List<SearchResult> searchWorkItemWithContext(String pattern, int contextLines, 
                                                boolean caseSensitive, boolean exactMatch);
}