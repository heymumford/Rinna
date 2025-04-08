/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.command;

import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockSearchService;
import org.rinna.cli.domain.model.SearchResult;
import org.rinna.cli.domain.model.SearchResult.Match;
import org.rinna.cli.util.OutputFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;

/**
 * Command to search for text in work items, similar to the Unix grep command.
 * - "rin grep pattern" - Basic search for pattern in work items
 * - "rin grep -i pattern" - Case-insensitive search (default)
 * - "rin grep -s pattern" - Case-sensitive search
 * - "rin grep -w pattern" - Match whole words only
 */
public class GrepCommand implements Callable<Integer> {
    private String pattern;
    private boolean caseSensitive = false;
    private boolean exactMatch = false;
    private int context = 0;
    private boolean countOnly = false;
    private boolean fileOutput = false;
    private boolean colorOutput = true;
    private String outputFormat = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final ContextManager contextManager;
    private final MetadataService metadataService;
    
    /**
     * Creates a new GrepCommand with default services.
     */
    public GrepCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new GrepCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public GrepCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.contextManager = ContextManager.getInstance();
        this.metadataService = serviceManager.getMetadataService();
    }
    
    /**
     * Sets the search pattern.
     * 
     * @param pattern the search pattern
     */
    public void setPattern(String pattern) {
        this.pattern = pattern;
    }
    
    /**
     * Sets whether the search should be case-sensitive.
     * 
     * @param caseSensitive true for case-sensitive search
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }
    
    /**
     * Sets whether the search should match whole words only.
     * 
     * @param exactMatch true for whole word matching
     */
    public void setExactMatch(boolean exactMatch) {
        this.exactMatch = exactMatch;
    }
    
    /**
     * Sets the number of context lines to show.
     * 
     * @param context the number of context lines
     */
    public void setContext(int context) {
        this.context = context;
    }
    
    /**
     * Sets whether to show only match counts.
     * 
     * @param countOnly true to show only counts
     */
    public void setCountOnly(boolean countOnly) {
        this.countOnly = countOnly;
    }
    
    /**
     * Sets whether to output to a file.
     * 
     * @param fileOutput true to output to a file
     */
    public void setFileOutput(boolean fileOutput) {
        this.fileOutput = fileOutput;
    }
    
    /**
     * Sets whether to use colored output.
     * 
     * @param colorOutput true to use colored output
     */
    public void setColorOutput(boolean colorOutput) {
        this.colorOutput = colorOutput;
    }
    
    /**
     * Sets the output format.
     * 
     * @param outputFormat the output format (text, json, csv)
     */
    public void setOutputFormat(String outputFormat) {
        this.outputFormat = outputFormat;
    }
    
    /**
     * Sets whether to show verbose output.
     * 
     * @param verbose true for verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("pattern", pattern);
        params.put("case_sensitive", caseSensitive);
        params.put("exact_match", exactMatch);
        params.put("context", context);
        params.put("count_only", countOnly);
        params.put("format", outputFormat);
        params.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("grep", "SEARCH", params);
        
        try {
            // Validate input
            if (pattern == null || pattern.isBlank()) {
                System.err.println("Error: No search pattern provided");
                System.err.println("Usage: rin grep <options> <pattern>");
                metadataService.failOperation(operationId, new IllegalArgumentException("Missing search pattern"));
                return 1;
            }
            
            // Get the search service
            MockSearchService searchService = serviceManager.getMockSearchService();
            if (searchService == null) {
                System.err.println("Error: Search service not available");
                metadataService.failOperation(operationId, new IllegalStateException("Search service not available"));
                return 1;
            }
            
            // Store the search in history
            contextManager.addToSearchHistory(pattern);
            
            // Perform the search using domain model
            List<WorkItem> matchingItems = searchService.findText(pattern, caseSensitive);
            
            // Convert WorkItems to SearchResults for display
            List<SearchResult> results = convertWorkItemsToSearchResults(matchingItems, pattern);
            
            // Display results
            if (results.isEmpty()) {
                System.out.println("No matches found for: " + pattern);
                
                // Record the successful operation with zero results
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("matches_found", 0);
                resultData.put("pattern", pattern);
                metadataService.completeOperation(operationId, resultData);
            } else {
                if (countOnly) {
                    displayCountSummary(results);
                } else {
                    if ("json".equalsIgnoreCase(outputFormat)) {
                        displayJsonOutput(results);
                    } else if ("csv".equalsIgnoreCase(outputFormat)) {
                        displayCsvOutput(results);
                    } else {
                        displayTextOutput(results);
                    }
                }
                
                // Record the successful operation with result summary
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("matches_found", countMatchesTotal(results));
                resultData.put("items_matched", results.size());
                resultData.put("pattern", pattern);
                metadataService.completeOperation(operationId, resultData);
            }
            
            return 0;
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error executing grep command: " + e.getMessage();
            System.err.println("Error: " + e.getMessage());
            
            // Record detailed error information if verbose mode is enabled
            if (verbose) {
                e.printStackTrace();
            }
            
            // Record the failed operation with error details
            metadataService.failOperation(operationId, e);
            
            return 1;
        }
    }
    
    /**
     * Counts the total number of matches across all results.
     *
     * @param results the search results
     * @return the total number of matches
     */
    private int countMatchesTotal(List<SearchResult> results) {
        int total = 0;
        for (SearchResult result : results) {
            total += result.getMatches().size();
        }
        return total;
    }
    
    /**
     * Displays a summary of the search results.
     * 
     * @param results the search results
     */
    private void displayCountSummary(List<SearchResult> results) {
        // Count total matches
        int totalMatches = 0;
        for (SearchResult result : results) {
            totalMatches += result.getMatches().size();
        }
        
        // Count distinct work items
        List<UUID> distinctWorkItems = new ArrayList<>();
        for (SearchResult result : results) {
            if (!distinctWorkItems.contains(result.getWorkItemId())) {
                distinctWorkItems.add(result.getWorkItemId());
            }
        }
        
        System.out.println("Search results for: " + pattern);
        System.out.println("Total matches: " + totalMatches);
        System.out.println("Matched work items: " + distinctWorkItems.size());
    }
    
    /**
     * Displays the search results in text format.
     * 
     * @param results the search results
     */
    private void displayTextOutput(List<SearchResult> results) {
        System.out.println("Search results for: " + pattern);
        System.out.println("-----------------------------------------");
        
        // Group results by work item
        for (SearchResult result : results) {
            // Get the work item details
            UUID itemId = result.getWorkItemId();
            WorkItem item = serviceManager.getMockItemService().getItem(itemId.toString());
            
            if (item != null) {
                System.out.println("Work Item: " + item.getId() + " - " + item.getTitle());
                System.out.println("Type: " + item.getType() + " | Priority: " + item.getPriority() + 
                                 " | Status: " + item.getState());
                
                // Display matches with context if requested
                List<Match> matches = result.getMatches();
                String content = result.getText();
                
                if (context > 0) {
                    displayMatchesWithContext(content, matches);
                } else {
                    for (Match match : matches) {
                        // Extract a substring around the match
                        int start = Math.max(0, match.getStart() - 20);
                        int end = Math.min(content.length(), match.getEnd() + 20);
                        String before = content.substring(start, match.getStart());
                        String matched = match.getMatchedText();
                        String after = content.substring(match.getEnd(), end);
                        
                        // Display with highlighting if enabled
                        if (colorOutput) {
                            System.out.println("..." + before + "\033[1;31m" + matched + "\033[0m" + after + "...");
                        } else {
                            System.out.println("..." + before + "[" + matched + "]" + after + "...");
                        }
                    }
                }
                
                System.out.println();
            }
        }
    }
    
    /**
     * Displays matches with their surrounding context.
     * 
     * @param content the content containing the matches
     * @param matches the matches to display
     */
    private void displayMatchesWithContext(String content, List<Match> matches) {
        // Split content into lines
        String[] lines = content.split("\n");
        
        // Process each match
        for (Match match : matches) {
            // Find the line containing the match
            int lineStart = 0;
            int lineNum = 0;
            int matchLine = -1;
            
            for (int i = 0; i < lines.length; i++) {
                int lineEnd = lineStart + lines[i].length();
                
                // Check if this line contains the match
                if (match.getStart() >= lineStart && match.getStart() < lineEnd) {
                    matchLine = i;
                    break;
                }
                
                lineStart = lineEnd + 1;  // +1 for the newline
            }
            
            if (matchLine >= 0) {
                // Display context lines before
                int startLine = Math.max(0, matchLine - context);
                for (int i = startLine; i < matchLine; i++) {
                    System.out.println(String.format("%4d: %s", i + 1, lines[i]));
                }
                
                // Display the matched line with highlighting
                String line = lines[matchLine];
                int matchStartInLine = match.getStart() - getLineStartPosition(lines, matchLine);
                int matchEndInLine = matchStartInLine + match.getMatchedText().length();
                
                if (colorOutput) {
                    String highlighted = line.substring(0, matchStartInLine) + 
                                        "\033[1;31m" + line.substring(matchStartInLine, matchEndInLine) + "\033[0m" + 
                                        line.substring(matchEndInLine);
                    System.out.println(String.format("%4d: %s", matchLine + 1, highlighted));
                } else {
                    String highlighted = line.substring(0, matchStartInLine) + 
                                        "[" + line.substring(matchStartInLine, matchEndInLine) + "]" + 
                                        line.substring(matchEndInLine);
                    System.out.println(String.format("%4d: %s", matchLine + 1, highlighted));
                }
                
                // Display context lines after
                int endLine = Math.min(lines.length, matchLine + context + 1);
                for (int i = matchLine + 1; i < endLine; i++) {
                    System.out.println(String.format("%4d: %s", i + 1, lines[i]));
                }
                
                System.out.println();
            }
        }
    }
    
    /**
     * Gets the starting position of a line in the content.
     * 
     * @param lines the content split into lines
     * @param lineIndex the index of the line
     * @return the starting position of the line
     */
    private int getLineStartPosition(String[] lines, int lineIndex) {
        int position = 0;
        for (int i = 0; i < lineIndex; i++) {
            position += lines[i].length() + 1;  // +1 for the newline
        }
        return position;
    }
    
    /**
     * Converts a list of WorkItems to SearchResults for display.
     *
     * @param workItems the work items to convert
     * @param pattern the search pattern
     * @return a list of search results
     */
    private List<SearchResult> convertWorkItemsToSearchResults(List<WorkItem> workItems, String pattern) {
        List<SearchResult> results = new ArrayList<>();
        
        for (WorkItem item : workItems) {
            // Create a search result for this work item
            String title = item.getTitle();
            String description = item.getDescription();
            
            if (title != null && title.toLowerCase().contains(pattern.toLowerCase())) {
                // Found in title
                SearchResult result = new SearchResult(
                    UUID.fromString(item.getId()), 
                    title,
                    1, // Line number
                    "workitem.title", 
                    title,
                    pattern
                );
                
                // Add match
                int start = title.toLowerCase().indexOf(pattern.toLowerCase());
                Match match = new Match(start, start + pattern.length(), pattern);
                result.addMatch(match);
                results.add(result);
            }
            
            if (description != null && description.toLowerCase().contains(pattern.toLowerCase())) {
                // Found in description
                SearchResult result = new SearchResult(
                    UUID.fromString(item.getId()), 
                    description,
                    1, // Line number
                    "workitem.description", 
                    description,
                    pattern
                );
                
                // Add match
                int start = description.toLowerCase().indexOf(pattern.toLowerCase());
                Match match = new Match(start, start + pattern.length(), pattern);
                result.addMatch(match);
                results.add(result);
            }
        }
        
        return results;
    }
    
    /**
     * Displays the search results in JSON format.
     * 
     * @param results the search results
     */
    private void displayJsonOutput(List<SearchResult> results) {
        StringBuilder json = new StringBuilder();
        json.append("{\n");
        json.append("  \"pattern\": \"").append(pattern).append("\",\n");
        json.append("  \"results\": [\n");
        
        boolean first = true;
        for (SearchResult result : results) {
            if (!first) {
                json.append(",\n");
            }
            first = false;
            
            // Get the work item details
            UUID itemId = result.getWorkItemId();
            WorkItem item = serviceManager.getMockItemService().getItem(itemId.toString());
            
            if (item != null) {
                json.append("    {\n");
                json.append("      \"id\": \"").append(item.getId()).append("\",\n");
                json.append("      \"title\": \"").append(item.getTitle()).append("\",\n");
                json.append("      \"type\": \"").append(item.getType()).append("\",\n");
                json.append("      \"priority\": \"").append(item.getPriority()).append("\",\n");
                json.append("      \"status\": \"").append(item.getState()).append("\",\n");
                json.append("      \"matches\": [\n");
                
                List<Match> matches = result.getMatches();
                for (int i = 0; i < matches.size(); i++) {
                    Match match = matches.get(i);
                    json.append("        {\n");
                    json.append("          \"text\": \"").append(match.getMatchedText()).append("\",\n");
                    json.append("          \"start\": ").append(match.getStart()).append(",\n");
                    json.append("          \"end\": ").append(match.getEnd()).append("\n");
                    json.append("        }");
                    
                    if (i < matches.size() - 1) {
                        json.append(",");
                    }
                    json.append("\n");
                }
                
                json.append("      ]\n");
                json.append("    }");
            }
        }
        
        json.append("\n  ]\n");
        json.append("}");
        
        System.out.println(json.toString());
    }
    
    /**
     * Displays the search results in CSV format.
     * 
     * @param results the search results
     */
    private void displayCsvOutput(List<SearchResult> results) {
        // Print CSV header
        System.out.println("WorkItemId,Title,Type,Priority,Status,MatchText,MatchStart,MatchEnd");
        
        // Print each match as a CSV row
        for (SearchResult result : results) {
            UUID itemId = result.getWorkItemId();
            WorkItem item = serviceManager.getMockItemService().getItem(itemId.toString());
            
            if (item != null) {
                // Escape any CSV-problematic characters
                String title = item.getTitle().replace("\"", "\"\"").replace(",", "\\,");
                
                for (Match match : result.getMatches()) {
                    String matchText = match.getMatchedText().replace("\"", "\"\"").replace(",", "\\,");
                    
                    System.out.println(
                        String.format("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",%d,%d",
                                     item.getId(),
                                     title,
                                     item.getType(),
                                     item.getPriority(),
                                     item.getState(),
                                     matchText,
                                     match.getStart(),
                                     match.getEnd())
                    );
                }
            }
        }
    }
    
    /**
     * Sets whether to show search history.
     *
     * @param showHistory true to show search history
     */
    public void setShowHistory(boolean showHistory) {
        // Not implemented yet - will be used for history tracking
    }
    
    /**
     * Gets the search pattern.
     *
     * @return the search pattern
     */
    public String getPattern() {
        return pattern;
    }
    
    /**
     * Gets whether to show search history.
     *
     * @return whether to show search history
     */
    public boolean isShowHistory() {
        return false; // Not implemented yet
    }
}