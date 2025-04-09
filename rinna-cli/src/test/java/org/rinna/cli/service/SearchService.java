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

import org.rinna.cli.domain.model.SearchResult;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

import java.util.List;
import java.util.Map;

/**
 * Interface for search service functionality.
 */
public interface SearchService {
    
    /**
     * Searches for work items containing the specified text.
     *
     * @param text the text to search for
     * @return a list of matching work items
     */
    List<WorkItem> searchByText(String text);
    
    /**
     * Searches for work items containing the specified text and in the specified state.
     *
     * @param text the text to search for
     * @param state the workflow state
     * @return a list of matching work items
     */
    List<WorkItem> searchByTextAndState(String text, WorkflowState state);
    
    /**
     * Searches for work items containing the specified text and of the specified type.
     *
     * @param text the text to search for
     * @param type the work item type
     * @return a list of matching work items
     */
    List<WorkItem> searchByTextAndType(String text, WorkItemType type);
    
    /**
     * Searches for work items containing the specified text and with the specified priority.
     *
     * @param text the text to search for
     * @param priority the priority
     * @return a list of matching work items
     */
    List<WorkItem> searchByTextAndPriority(String text, Priority priority);
    
    /**
     * Finds text matches in work items (case-insensitive).
     *
     * @param text the text to search for
     * @return a list of search results
     */
    List<SearchResult> findText(String text);
    
    /**
     * Finds text matches in work items with optional case sensitivity.
     *
     * @param text the text to search for
     * @param caseSensitive whether the search is case-sensitive
     * @return a list of search results
     */
    List<SearchResult> findText(String text, boolean caseSensitive);
    
    /**
     * Finds pattern matches in work items using regular expressions (case-insensitive).
     *
     * @param pattern the regular expression pattern
     * @return a list of search results
     */
    List<SearchResult> findPattern(String pattern);
    
    /**
     * Finds pattern matches in work items using regular expressions with optional case sensitivity.
     *
     * @param pattern the regular expression pattern
     * @param caseSensitive whether the search is case-sensitive
     * @return a list of search results
     */
    List<SearchResult> findPattern(String pattern, boolean caseSensitive);
    
    /**
     * Finds work items matching the specified criteria.
     *
     * @param criteria a map of criteria (field name to value)
     * @param limit the maximum number of results to return (0 for no limit)
     * @return a list of matching work items
     */
    List<WorkItem> findWorkItems(Map<String, String> criteria, int limit);
}