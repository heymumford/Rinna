/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.pui.cli;

import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockSearchService;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkflowState;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Bridge between the PUI components and CLI search services.
 * This class provides context-aware search functionality for PUI components.
 */
public class SearchBridge {
    
    private static SearchBridge instance;
    
    private final ServiceManager serviceManager;
    private final MockSearchService searchService;
    private final MockItemService itemService;
    
    // Search context and history
    private final Set<String> recentSearches = new HashSet<>();
    private final Map<String, Integer> searchFrequency = new HashMap<>();
    private final Map<SearchContext, List<String>> contextualSuggestions = new HashMap<>();
    private final Map<String, Set<String>> relatedTerms = new HashMap<>();
    
    // Cache for suggestions
    private final Map<String, List<String>> suggestionCache = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 100;
    private static final int MAX_RECENT_SEARCHES = 20;
    
    /**
     * Search context enum for contextual suggestions.
     */
    public enum SearchContext {
        GLOBAL,          // General search across all items
        WORK_ITEM,       // Searching for work items
        PERSON,          // Searching for people (assignees, reporters)
        PROJECT,         // Searching for projects
        STATE,           // Searching for workflow states
        TYPE,            // Searching for work item types
        PRIORITY,        // Searching for priorities
        COMMAND,         // Searching for CLI commands
        TAG              // Searching for tags
    }
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private SearchBridge() {
        this.serviceManager = ServiceManager.getInstance();
        this.searchService = serviceManager.getMockSearchService();
        this.itemService = serviceManager.getMockItemService();
        
        // Initialize context-specific suggestions
        initializeContextualSuggestions();
        initializeRelatedTerms();
    }
    
    /**
     * Gets the singleton instance of SearchBridge.
     * 
     * @return the singleton instance
     */
    public static synchronized SearchBridge getInstance() {
        if (instance == null) {
            instance = new SearchBridge();
        }
        return instance;
    }
    
    /**
     * Initializes context-specific suggestions.
     */
    private void initializeContextualSuggestions() {
        // Command suggestions
        List<String> commandSuggestions = Arrays.asList(
            "add", "list", "view", "update", "done", "assign", "comment",
            "stats", "help", "history", "notify", "server", "admin", "grep",
            "find", "schedule", "report", "backlog", "bulk", "workflow"
        );
        contextualSuggestions.put(SearchContext.COMMAND, commandSuggestions);
        
        // State suggestions
        List<String> stateSuggestions = Arrays.asList(
            "created", "ready", "in_progress", "review", "testing", "done",
            "blocked", "found", "triaged", "to_do", "in_test"
        );
        contextualSuggestions.put(SearchContext.STATE, stateSuggestions);
        
        // Type suggestions
        List<String> typeSuggestions = Arrays.asList(
            "task", "bug", "feature", "epic", "story", "spike"
        );
        contextualSuggestions.put(SearchContext.TYPE, typeSuggestions);
        
        // Priority suggestions
        List<String> prioritySuggestions = Arrays.asList(
            "critical", "high", "medium", "low", "trivial"
        );
        contextualSuggestions.put(SearchContext.PRIORITY, prioritySuggestions);
        
        // Person suggestions (some common names)
        List<String> personSuggestions = Arrays.asList(
            "john.doe", "jane.smith", "alex.dev", "sarah.manager", 
            "eric.mumford", "mark.writer", "security.team", "unassigned"
        );
        contextualSuggestions.put(SearchContext.PERSON, personSuggestions);
        
        // Project suggestions
        List<String> projectSuggestions = Arrays.asList(
            "RINNA-1", "RINNA-2", "API-1", "DOC-1", "INFRA-1"
        );
        contextualSuggestions.put(SearchContext.PROJECT, projectSuggestions);
    }
    
    /**
     * Initializes related terms for better suggestions.
     */
    private void initializeRelatedTerms() {
        // Define related terms for common search queries
        addRelatedTerms("bug", "defect", "issue", "error", "problem", "fix");
        addRelatedTerms("feature", "enhancement", "improvement", "new");
        addRelatedTerms("urgent", "critical", "important", "high", "priority");
        addRelatedTerms("done", "completed", "finished", "closed", "resolved");
        addRelatedTerms("assigned", "assignee", "owner", "responsible");
        addRelatedTerms("blocked", "impediment", "blocker", "stuck");
    }
    
    /**
     * Adds related terms to the map.
     * 
     * @param term the main term
     * @param relatedTerms the related terms
     */
    private void addRelatedTerms(String term, String... relatedTerms) {
        Set<String> terms = new HashSet<>(Arrays.asList(relatedTerms));
        this.relatedTerms.put(term, terms);
        
        // Also add reverse mappings
        for (String related : relatedTerms) {
            Set<String> reverseTerms = this.relatedTerms.computeIfAbsent(related, k -> new HashSet<>());
            reverseTerms.add(term);
        }
    }
    
    /**
     * Gets suggestions based on the input text and search context.
     * 
     * @param text the input text
     * @param context the search context
     * @return a list of suggestions
     */
    public List<String> getSuggestions(String text, SearchContext context) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Check cache first
        String cacheKey = context.name() + ":" + text.toLowerCase();
        if (suggestionCache.containsKey(cacheKey)) {
            return suggestionCache.get(cacheKey);
        }
        
        List<String> suggestions = new ArrayList<>();
        
        // Add context-specific suggestions
        if (contextualSuggestions.containsKey(context)) {
            List<String> contextSuggestions = contextualSuggestions.get(context);
            for (String suggestion : contextSuggestions) {
                if (suggestion.toLowerCase().contains(text.toLowerCase())) {
                    suggestions.add(suggestion);
                }
            }
        }
        
        // Add dynamic suggestions based on context
        switch (context) {
            case WORK_ITEM:
                suggestions.addAll(getWorkItemSuggestions(text));
                break;
                
            case GLOBAL:
                suggestions.addAll(getGlobalSuggestions(text));
                break;
                
            case PERSON:
                suggestions.addAll(getPersonSuggestions(text));
                break;
                
            case TAG:
                suggestions.addAll(getTagSuggestions(text));
                break;
                
            default:
                // Use the context-specific suggestions already added
                break;
        }
        
        // Add suggestions based on related terms
        for (String term : text.toLowerCase().split("\\s+")) {
            if (relatedTerms.containsKey(term)) {
                for (String related : relatedTerms.get(term)) {
                    if (!suggestions.contains(related)) {
                        suggestions.add(related);
                    }
                }
            }
        }
        
        // Add recent searches that match
        for (String recent : recentSearches) {
            if (recent.toLowerCase().contains(text.toLowerCase()) && !suggestions.contains(recent)) {
                suggestions.add(recent);
            }
        }
        
        // Sort suggestions by relevance and frequency
        suggestions.sort((s1, s2) -> {
            // First by exact match
            boolean exactMatch1 = s1.equalsIgnoreCase(text);
            boolean exactMatch2 = s2.equalsIgnoreCase(text);
            if (exactMatch1 && !exactMatch2) return -1;
            if (!exactMatch1 && exactMatch2) return 1;
            
            // Then by starts with
            boolean startsWith1 = s1.toLowerCase().startsWith(text.toLowerCase());
            boolean startsWith2 = s2.toLowerCase().startsWith(text.toLowerCase());
            if (startsWith1 && !startsWith2) return -1;
            if (!startsWith1 && startsWith2) return 1;
            
            // Then by frequency
            int freq1 = searchFrequency.getOrDefault(s1.toLowerCase(), 0);
            int freq2 = searchFrequency.getOrDefault(s2.toLowerCase(), 0);
            if (freq1 != freq2) return Integer.compare(freq2, freq1);
            
            // Finally by alphabetical order
            return s1.compareToIgnoreCase(s2);
        });
        
        // Limit the number of suggestions
        if (suggestions.size() > 10) {
            suggestions = suggestions.subList(0, 10);
        }
        
        // Cache the results
        addToCache(cacheKey, suggestions);
        
        return suggestions;
    }
    
    /**
     * Gets work item suggestions based on the input text.
     * 
     * @param text the input text
     * @return a list of suggestions
     */
    private List<String> getWorkItemSuggestions(String text) {
        List<String> suggestions = new ArrayList<>();
        
        try {
            // Get matching work items
            List<WorkItem> items = searchService.search(text);
            
            // Add work item titles
            for (WorkItem item : items) {
                if (item.getTitle() != null && !item.getTitle().isEmpty()) {
                    suggestions.add(item.getTitle());
                }
            }
            
            // Add work item IDs
            for (WorkItem item : items) {
                if (item.getId() != null && !item.getId().isEmpty()) {
                    suggestions.add(item.getId());
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting work item suggestions: " + e.getMessage());
        }
        
        return suggestions;
    }
    
    /**
     * Gets global suggestions based on the input text.
     * 
     * @param text the input text
     * @return a list of suggestions
     */
    private List<String> getGlobalSuggestions(String text) {
        List<String> suggestions = new ArrayList<>();
        
        // Add from other contexts
        for (SearchContext otherContext : SearchContext.values()) {
            if (otherContext != SearchContext.GLOBAL) {
                List<String> contextSuggestions = getSuggestions(text, otherContext);
                for (String suggestion : contextSuggestions) {
                    if (!suggestions.contains(suggestion)) {
                        suggestions.add(suggestion);
                    }
                }
            }
        }
        
        return suggestions;
    }
    
    /**
     * Gets person suggestions based on the input text.
     * 
     * @param text the input text
     * @return a list of suggestions
     */
    private List<String> getPersonSuggestions(String text) {
        Set<String> people = new HashSet<>();
        
        // Add from static list
        List<String> staticSuggestions = contextualSuggestions.getOrDefault(SearchContext.PERSON, Collections.emptyList());
        people.addAll(staticSuggestions);
        
        // Add from work items
        try {
            List<WorkItem> items = itemService.getAllItems();
            for (WorkItem item : items) {
                if (item.getAssignee() != null && !item.getAssignee().isEmpty()) {
                    people.add(item.getAssignee());
                }
                if (item.getReporter() != null && !item.getReporter().isEmpty()) {
                    people.add(item.getReporter());
                }
            }
        } catch (Exception e) {
            System.err.println("Error getting person suggestions: " + e.getMessage());
        }
        
        // Filter by input text
        return people.stream()
            .filter(person -> person.toLowerCase().contains(text.toLowerCase()))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets tag suggestions based on the input text.
     * 
     * @param text the input text
     * @return a list of suggestions
     */
    private List<String> getTagSuggestions(String text) {
        Set<String> tags = new HashSet<>();
        
        // Predefined tags
        tags.addAll(Arrays.asList(
            "important", "urgent", "review", "needs-testing", "documentation",
            "tech-debt", "refactoring", "enhancement", "ui", "api", "backend",
            "frontend", "database", "security", "performance", "usability"
        ));
        
        // Filter by input text
        return tags.stream()
            .filter(tag -> tag.toLowerCase().contains(text.toLowerCase()))
            .collect(Collectors.toList());
    }
    
    /**
     * Adds a search to the history.
     * 
     * @param text the search text
     */
    public void addToHistory(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        
        // Add to recent searches
        recentSearches.add(text);
        if (recentSearches.size() > MAX_RECENT_SEARCHES) {
            // Remove oldest search (not the most efficient, but simple)
            recentSearches.iterator().remove();
        }
        
        // Update frequency counter
        String lowerText = text.toLowerCase();
        searchFrequency.put(lowerText, searchFrequency.getOrDefault(lowerText, 0) + 1);
        
        // Clear cache entries containing this text to ensure fresh results next time
        List<String> keysToRemove = new ArrayList<>();
        for (String key : suggestionCache.keySet()) {
            if (key.toLowerCase().contains(lowerText)) {
                keysToRemove.add(key);
            }
        }
        for (String key : keysToRemove) {
            suggestionCache.remove(key);
        }
    }
    
    /**
     * Adds suggestions to the cache.
     * 
     * @param key the cache key
     * @param suggestions the suggestions to cache
     */
    private void addToCache(String key, List<String> suggestions) {
        suggestionCache.put(key, new ArrayList<>(suggestions));
        
        // Limit cache size
        if (suggestionCache.size() > MAX_CACHE_SIZE) {
            // Remove a random entry
            String keyToRemove = suggestionCache.keySet().iterator().next();
            suggestionCache.remove(keyToRemove);
        }
    }
    
    /**
     * Clears the suggestion cache.
     */
    public void clearCache() {
        suggestionCache.clear();
    }
    
    /**
     * Creates a context-aware suggestion provider for the given search context.
     * 
     * @param context the search context
     * @return a suggestion provider function
     */
    public Function<String, List<String>> createSuggestionProvider(SearchContext context) {
        return text -> getSuggestions(text, context);
    }
    
    /**
     * Executes a search with the given text and context.
     * 
     * @param text the search text
     * @param context the search context
     * @return a list of work items matching the search
     */
    public List<WorkItem> search(String text, SearchContext context) {
        if (text == null || text.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Add to history
        addToHistory(text);
        
        try {
            // Determine search strategy based on context
            switch (context) {
                case WORK_ITEM:
                    return searchService.search(text);
                    
                case PERSON:
                    return searchByPerson(text);
                    
                case STATE:
                    return searchByState(text);
                    
                case TYPE:
                    return searchByType(text);
                    
                case PRIORITY:
                    return searchByPriority(text);
                    
                case PROJECT:
                    return searchByProject(text);
                    
                case GLOBAL:
                default:
                    return searchService.search(text);
            }
        } catch (Exception e) {
            System.err.println("Error executing search: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Searches for work items by person (assignee or reporter).
     * 
     * @param person the person name
     * @return a list of matching work items
     */
    private List<WorkItem> searchByPerson(String person) {
        List<WorkItem> results = new ArrayList<>();
        
        try {
            List<WorkItem> allItems = itemService.getAllItems();
            for (WorkItem item : allItems) {
                if ((item.getAssignee() != null && item.getAssignee().toLowerCase().contains(person.toLowerCase())) ||
                    (item.getReporter() != null && item.getReporter().toLowerCase().contains(person.toLowerCase()))) {
                    results.add(item);
                }
            }
        } catch (Exception e) {
            System.err.println("Error searching by person: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Searches for work items by state.
     * 
     * @param state the state name
     * @return a list of matching work items
     */
    private List<WorkItem> searchByState(String state) {
        List<WorkItem> results = new ArrayList<>();
        
        try {
            // Try to parse the state name
            WorkflowState workflowState = null;
            for (WorkflowState ws : WorkflowState.values()) {
                if (ws.name().toLowerCase().contains(state.toLowerCase())) {
                    workflowState = ws;
                    break;
                }
            }
            
            if (workflowState != null) {
                List<WorkItem> allItems = itemService.getAllItems();
                for (WorkItem item : allItems) {
                    if (workflowState.equals(item.getState())) {
                        results.add(item);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error searching by state: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Searches for work items by type.
     * 
     * @param type the type name
     * @return a list of matching work items
     */
    private List<WorkItem> searchByType(String type) {
        List<WorkItem> results = new ArrayList<>();
        
        try {
            // Try to parse the type name
            WorkItemType itemType = null;
            for (WorkItemType wit : WorkItemType.values()) {
                if (wit.name().toLowerCase().contains(type.toLowerCase())) {
                    itemType = wit;
                    break;
                }
            }
            
            if (itemType != null) {
                List<WorkItem> allItems = itemService.getAllItems();
                for (WorkItem item : allItems) {
                    if (itemType.equals(item.getType())) {
                        results.add(item);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error searching by type: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Searches for work items by priority.
     * 
     * @param priority the priority name
     * @return a list of matching work items
     */
    private List<WorkItem> searchByPriority(String priority) {
        List<WorkItem> results = new ArrayList<>();
        
        try {
            // Try to parse the priority name
            Priority itemPriority = null;
            for (Priority p : Priority.values()) {
                if (p.name().toLowerCase().contains(priority.toLowerCase())) {
                    itemPriority = p;
                    break;
                }
            }
            
            if (itemPriority != null) {
                List<WorkItem> allItems = itemService.getAllItems();
                for (WorkItem item : allItems) {
                    if (itemPriority.equals(item.getPriority())) {
                        results.add(item);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error searching by priority: " + e.getMessage());
        }
        
        return results;
    }
    
    /**
     * Searches for work items by project.
     * 
     * @param project the project name or ID
     * @return a list of matching work items
     */
    private List<WorkItem> searchByProject(String project) {
        // This is a placeholder implementation
        // In a real implementation, it would filter work items by project
        return searchService.search(project);
    }
}