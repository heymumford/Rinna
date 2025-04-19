/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.model;

import java.util.UUID;

/**
 * Represents a search result in the CLI.
 */
public class SearchResult {
    private final UUID itemId;
    private final String field;
    private final String content;
    private final Match match;
    
    /**
     * Creates a new search result.
     * 
     * @param itemId the work item ID
     * @param field the field that matched
     * @param content the content that matched
     * @param start the start position of the match
     * @param end the end position of the match
     */
    public SearchResult(UUID itemId, String field, String content, int start, int end) {
        this.itemId = itemId;
        this.field = field;
        this.content = content;
        this.match = new Match(start, end);
    }
    
    /**
     * Gets the work item ID.
     * 
     * @return the work item ID
     */
    public UUID getItemId() {
        return itemId;
    }
    
    /**
     * Gets the field that matched.
     * 
     * @return the field name
     */
    public String getField() {
        return field;
    }
    
    /**
     * Gets the content that matched.
     * 
     * @return the content
     */
    public String getContent() {
        return content;
    }
    
    /**
     * Gets the match details.
     * 
     * @return the match details
     */
    public Match getMatch() {
        return match;
    }
    
    /**
     * Represents a match position in a search result.
     */
    public static class Match {
        private final int start;
        private final int end;
        
        /**
         * Creates a new match.
         * 
         * @param start the start position
         * @param end the end position
         */
        public Match(int start, int end) {
            this.start = start;
            this.end = end;
        }
        
        /**
         * Gets the start position.
         * 
         * @return the start position
         */
        public int getStart() {
            return start;
        }
        
        /**
         * Gets the end position.
         * 
         * @return the end position
         */
        public int getEnd() {
            return end;
        }
    }
}