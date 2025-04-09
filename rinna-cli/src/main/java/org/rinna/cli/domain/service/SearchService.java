/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.domain.service;

import org.rinna.cli.domain.model.DomainPriority;
import org.rinna.cli.domain.model.DomainWorkItem;
import org.rinna.cli.domain.model.DomainWorkItemType;
import org.rinna.cli.domain.model.DomainWorkflowState;

import java.util.List;
import java.util.Map;

/**
 * Domain interface for search operations in the CLI module.
 * This service allows searching for work items using various criteria.
 */
public interface SearchService {
    
    /**
     * Search for work items by text.
     *
     * @param text the text to search for
     * @return a list of matching work items
     */
    List<DomainWorkItem> searchByText(String text);
    
    /**
     * Search for work items by text and filter by a specific state.
     *
     * @param text the text to search for
     * @param state the state to filter by
     * @return a list of matching work items
     */
    List<DomainWorkItem> searchByTextAndState(String text, DomainWorkflowState state);
    
    /**
     * Search for work items by text and filter by a specific type.
     *
     * @param text the text to search for
     * @param type the type to filter by
     * @return a list of matching work items
     */
    List<DomainWorkItem> searchByTextAndType(String text, DomainWorkItemType type);
    
    /**
     * Search for work items by text and filter by a specific priority.
     *
     * @param text the text to search for
     * @param priority the priority to filter by
     * @return a list of matching work items
     */
    List<DomainWorkItem> searchByTextAndPriority(String text, DomainPriority priority);
    
    /**
     * Search for text within work items.
     *
     * @param text the text to search for
     * @return a list of search results
     */
    List<DomainWorkItem> findText(String text);
    
    /**
     * Search for text within work items with case-sensitivity.
     *
     * @param text the text to search for
     * @param caseSensitive whether to use case-sensitive search
     * @return a list of search results
     */
    List<DomainWorkItem> findText(String text, boolean caseSensitive);
    
    /**
     * Search for a pattern within work items.
     *
     * @param pattern the regular expression pattern to search for
     * @return a list of search results
     */
    List<DomainWorkItem> findPattern(String pattern);
    
    /**
     * Search for a pattern within work items with case-sensitivity.
     *
     * @param pattern the regular expression pattern to search for
     * @param caseSensitive whether to use case-sensitive search
     * @return a list of search results
     */
    List<DomainWorkItem> findPattern(String pattern, boolean caseSensitive);
    
    /**
     * Find work items by searching the text content.
     *
     * @param text the text to search for
     * @return a list of work items containing the text
     */
    List<DomainWorkItem> findItemsByText(String text);
    
    /**
     * Find work items by metadata.
     *
     * @param metadata the metadata to search for
     * @return a list of work items with matching metadata
     */
    List<DomainWorkItem> findItemsByMetadata(Map<String, String> metadata);
    
    /**
     * Searches for work items using multiple criteria.
     *
     * @param criteria a map of criteria (field name to value)
     * @param limit the maximum number of results to return
     * @return a list of matching work items
     */
    List<DomainWorkItem> findWorkItems(Map<String, String> criteria, int limit);
}