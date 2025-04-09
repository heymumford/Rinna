/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a search result with matches in a file or work item.
 */
public class SearchResult {
    private UUID itemId;
    private String content;
    private int lineNumber;
    private String filename;
    private String context;
    private String searchTerm;
    private List<Match> matches;
    
    /**
     * Constructs a new SearchResult.
     *
     * @param itemId the work item ID
     * @param content the content matched
     * @param lineNumber the line number of the match
     * @param filename the filename containing the match
     * @param context surrounding context of the match
     * @param searchTerm the search term used
     */
    public SearchResult(UUID itemId, String content, int lineNumber, String filename, String context, String searchTerm) {
        this.itemId = itemId;
        this.content = content;
        this.lineNumber = lineNumber;
        this.filename = filename;
        this.context = context;
        this.searchTerm = searchTerm;
        this.matches = new ArrayList<>();
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
     * Gets the work item ID (alternative method name).
     *
     * @return the work item ID
     */
    public UUID getWorkItemId() {
        return itemId;
    }
    
    /**
     * Gets the content that matched.
     *
     * @return the matched content
     */
    public String getContent() {
        return content;
    }
    
    /**
     * Gets the text that matched (alternative method name).
     *
     * @return the matched text
     */
    public String getText() {
        return content;
    }
    
    /**
     * Gets the line number of the match.
     *
     * @return the line number
     */
    public int getLineNumber() {
        return lineNumber;
    }
    
    /**
     * Gets the filename containing the match.
     *
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }
    
    /**
     * Gets the context surrounding the match.
     *
     * @return the context
     */
    public String getContext() {
        return context;
    }
    
    /**
     * Gets the search term used.
     *
     * @return the search term
     */
    public String getSearchTerm() {
        return searchTerm;
    }
    
    /**
     * Gets the matches within the content.
     *
     * @return list of matches
     */
    public List<Match> getMatches() {
        return matches;
    }
    
    /**
     * Adds a match to this search result.
     *
     * @param match the match to add
     */
    public void addMatch(Match match) {
        matches.add(match);
    }
    
    /**
     * Represents a specific match within content.
     */
    public static class Match {
        private int startIndex;
        private int endIndex;
        private String matchedText;
        
        /**
         * Constructs a new Match.
         *
         * @param startIndex the start index of the match
         * @param endIndex the end index of the match
         * @param matchedText the text that was matched
         */
        public Match(int startIndex, int endIndex, String matchedText) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.matchedText = matchedText;
        }
        
        /**
         * Gets the start index of the match.
         *
         * @return the start index
         */
        public int getStartIndex() {
            return startIndex;
        }
        
        /**
         * Gets the start index of the match (alternative method name).
         *
         * @return the start index
         */
        public int getStart() {
            return startIndex;
        }
        
        /**
         * Gets the end index of the match.
         *
         * @return the end index
         */
        public int getEndIndex() {
            return endIndex;
        }
        
        /**
         * Gets the end index of the match (alternative method name).
         *
         * @return the end index
         */
        public int getEnd() {
            return endIndex;
        }
        
        /**
         * Gets the matched text.
         *
         * @return the matched text
         */
        public String getMatchedText() {
            return matchedText;
        }
    }
}
