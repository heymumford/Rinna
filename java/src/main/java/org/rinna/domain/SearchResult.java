/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.domain;

import java.util.List;
import java.util.UUID;

/**
 * Represents a search result containing information about matches in a work item.
 */
public class SearchResult {
    
    private final UUID itemId;
    private final String field;
    private final String text;
    private final List<Match> matches;
    
    /**
     * Creates a new SearchResult.
     *
     * @param itemId the ID of the work item
     * @param field the field where the match was found
     * @param text the text containing the match
     * @param matches the list of matches
     */
    public SearchResult(UUID itemId, String field, String text, List<Match> matches) {
        this.itemId = itemId;
        this.field = field;
        this.text = text;
        this.matches = matches;
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
     * Gets the field name.
     *
     * @return the field name
     */
    public String getField() {
        return field;
    }
    
    /**
     * Gets the text containing the match.
     *
     * @return the text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Gets the list of matches.
     *
     * @return the matches
     */
    public List<Match> getMatches() {
        return matches;
    }
    
    /**
     * Represents a match in text.
     */
    public static class Match {
        private final int start;
        private final int end;
        private final String text;
        
        /**
         * Creates a new Match.
         *
         * @param start the start position of the match
         * @param end the end position of the match
         */
        public Match(int start, int end) {
            this.start = start;
            this.end = end;
            this.text = null;
        }
        
        /**
         * Creates a new Match with the matched text.
         *
         * @param start the start position of the match
         * @param end the end position of the match
         * @param text the matched text
         */
        public Match(int start, int end, String text) {
            this.start = start;
            this.end = end;
            this.text = text;
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
        
        /**
         * Gets the matched text.
         *
         * @return the matched text
         */
        public String getText() {
            return text;
        }
    }
}