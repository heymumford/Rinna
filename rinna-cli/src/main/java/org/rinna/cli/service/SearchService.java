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
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkflowState;

import java.util.List;
import java.util.Map;

/**
 * Interface for search services.
 */
public interface SearchService {
    
    /**
     * Search for work items by text.
     *
     * @param text the text to search for
     * @return a list of matching work items
     */
    List<WorkItem> searchByText(String text);
    
    /**
     * Search for work items by text and filter by a specific state.
     *
     * @param text the text to search for
     * @param state the state to filter by
     * @return a list of matching work items
     */
    List<WorkItem> searchByTextAndState(String text, WorkflowState state);
    
    /**
     * Search for work items by text and filter by a specific type.
     *
     * @param text the text to search for
     * @param type the type to filter by
     * @return a list of matching work items
     */
    List<WorkItem> searchByTextAndType(String text, WorkItemType type);
    
    /**
     * Search for work items by text and filter by a specific priority.
     *
     * @param text the text to search for
     * @param priority the priority to filter by
     * @return a list of matching work items
     */
    List<WorkItem> searchByTextAndPriority(String text, Priority priority);
    
    /**
     * Search for text within work items.
     *
     * @param text the text to search for
     * @return a list of search results
     */
    List<WorkItem> findText(String text);
    
    /**
     * Search for text within work items with case-sensitivity.
     *
     * @param text the text to search for
     * @param caseSensitive whether to use case-sensitive search
     * @return a list of search results
     */
    List<WorkItem> findText(String text, boolean caseSensitive);
    
    /**
     * Search for a pattern within work items.
     *
     * @param pattern the regular expression pattern to search for
     * @return a list of search results
     */
    List<WorkItem> findPattern(String pattern);
    
    /**
     * Search for a pattern within work items with case-sensitivity.
     *
     * @param pattern the regular expression pattern to search for
     * @param caseSensitive whether to use case-sensitive search
     * @return a list of search results
     */
    List<WorkItem> findPattern(String pattern, boolean caseSensitive);
    
    /**
     * Find work items by searching the text content.
     *
     * @param text the text to search for
     * @return a list of work items containing the text
     */
    List<WorkItem> findItemsByText(String text);
    
    /**
     * Find work items by metadata.
     *
     * @param metadata the metadata to search for
     * @return a list of work items with matching metadata
     */
    List<WorkItem> findItemsByMetadata(Map<String, String> metadata);
    
    /**
     * Searches for work items using multiple criteria.
     *
     * @param criteria a map of criteria (field name to value)
     * @param limit the maximum number of results to return
     * @return a list of matching work items
     */
    List<WorkItem> findWorkItems(java.util.Map<String, String> criteria, int limit);
}