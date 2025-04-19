package org.rinna.cli.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.rinna.cli.domain.model.SearchResult;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.model.WorkflowState;

/**
 * Test implementation of the SearchService for use in tests.
 */
public class TestSearchService implements SearchService {
    private final List<WorkItem> workItems = new ArrayList<>();
    private boolean throwException = false;
    private boolean throwOutOfMemoryError = false;
    private boolean throwNullPointerException = false;
    private boolean throwIllegalArgumentException = false;
    private boolean simulateSystemResourceError = false;
    
    // Tracking variables for testing
    private boolean searchWasCalled = false;
    private Map<String, String> lastSearchCriteria = new HashMap<>();
    private int lastSearchLimit = 0;
    private AtomicInteger searchCallCount = new AtomicInteger(0);
    private String lastSearchTerm = null;
    private boolean lastCaseSensitive = false;
    private String lastSearchPattern = null;
    
    public TestSearchService() {
        // Notice we're not calling super() which is what caused the infinite recursion
    }
    
    /**
     * Initializes the test data with sample work items.
     */
    public void initializeTestData() {
        workItems.clear();
        
        // Create test items
        WorkItem item1 = new WorkItem();
        item1.setId(UUID.randomUUID().toString());
        item1.setTitle("Feature One");
        item1.setDescription("First feature");
        item1.setType(WorkItemType.FEATURE);
        item1.setPriority(Priority.HIGH);
        item1.setStatus(WorkflowState.READY);
        item1.setAssignee("alice");
        item1.setProject("Project-A");
        item1.setCreated(LocalDateTime.now().minusDays(5));
        item1.setUpdated(LocalDateTime.now().minusDays(2));
        workItems.add(item1);
        
        WorkItem item2 = new WorkItem();
        item2.setId(UUID.randomUUID().toString());
        item2.setTitle("Bug Two");
        item2.setDescription("Second bug");
        item2.setType(WorkItemType.BUG);
        item2.setPriority(Priority.MEDIUM);
        item2.setStatus(WorkflowState.IN_PROGRESS);
        item2.setAssignee("bob");
        item2.setProject("Project-A");
        item2.setCreated(LocalDateTime.now().minusDays(3));
        item2.setUpdated(LocalDateTime.now().minusDays(1));
        workItems.add(item2);
        
        WorkItem item3 = new WorkItem();
        item3.setId(UUID.randomUUID().toString());
        item3.setTitle("Task Three");
        item3.setDescription("Third task");
        item3.setType(WorkItemType.TASK);
        item3.setPriority(Priority.LOW);
        item3.setStatus(WorkflowState.DONE);
        item3.setAssignee("charlie");
        item3.setProject("Project-B");
        item3.setCreated(LocalDateTime.now().minusDays(1));
        item3.setUpdated(LocalDateTime.now());
        workItems.add(item3);
    }
    
    /**
     * Adds a work item with a long title.
     * 
     * @param longTitle the long title to set
     */
    public void addItemWithLongTitle(String longTitle) {
        // Clear other work items to ensure the long title item is displayed
        workItems.clear();
        
        WorkItem item = new WorkItem();
        item.setId(UUID.randomUUID().toString());
        item.setTitle(longTitle);
        item.setDescription("Item with a long title");
        item.setType(WorkItemType.TASK);
        item.setPriority(Priority.MEDIUM);
        item.setStatus(WorkflowState.READY);
        workItems.add(item);
    }
    
    /**
     * Sets whether to throw an exception during operations.
     * 
     * @param throwException true to throw exceptions, false otherwise
     */
    public void setThrowException(boolean throwException) {
        this.throwException = throwException;
    }
    
    /**
     * Sets whether to throw an OutOfMemoryError during operations.
     * 
     * @param throwOutOfMemoryError true to throw error, false otherwise
     */
    public void setThrowOutOfMemoryError(boolean throwOutOfMemoryError) {
        this.throwOutOfMemoryError = throwOutOfMemoryError;
    }
    
    /**
     * Sets whether to throw a NullPointerException during operations.
     * 
     * @param throwNullPointerException true to throw exception, false otherwise
     */
    public void setThrowNullPointerException(boolean throwNullPointerException) {
        this.throwNullPointerException = throwNullPointerException;
    }
    
    /**
     * Sets whether to throw an IllegalArgumentException during operations.
     * 
     * @param throwIllegalArgumentException true to throw exception, false otherwise
     */
    public void setThrowIllegalArgumentException(boolean throwIllegalArgumentException) {
        this.throwIllegalArgumentException = throwIllegalArgumentException;
    }
    
    /**
     * Sets whether to simulate a system resource error (like out of file descriptors).
     * 
     * @param simulateSystemResourceError true to simulate error, false otherwise
     */
    public void setSimulateSystemResourceError(boolean simulateSystemResourceError) {
        this.simulateSystemResourceError = simulateSystemResourceError;
    }
    
    /**
     * Gets all items for testing.
     * 
     * @return a list of all work items
     */
    public List<WorkItem> getAllItems() {
        return new ArrayList<>(workItems);
    }
    
    /**
     * Adds a custom work item to the test data.
     * 
     * @param item the work item to add
     */
    public void addTestItem(WorkItem item) {
        workItems.add(item);
    }
    
    /**
     * Generates a large dataset with the specified number of items.
     * 
     * @param count the number of items to generate
     */
    public void generateLargeDataset(int count) {
        workItems.clear();
        
        for (int i = 0; i < count; i++) {
            WorkItem item = new WorkItem();
            item.setId(UUID.randomUUID().toString());
            item.setTitle("Generated Item " + i);
            item.setDescription("Description for item " + i);
            
            // Distribute types, priorities, and states evenly
            WorkItemType[] types = WorkItemType.values();
            item.setType(types[i % types.length]);
            
            Priority[] priorities = Priority.values();
            item.setPriority(priorities[i % priorities.length]);
            
            WorkflowState[] states = WorkflowState.values();
            item.setStatus(states[i % states.length]);
            
            // Distribute assignees among 5 users
            item.setAssignee("user" + (i % 5));
            
            // Distribute projects among 3 projects
            item.setProject("Project-" + (char)('A' + (i % 3)));
            
            // Set dates with some variability
            item.setCreated(LocalDateTime.now().minusDays(i % 30));
            item.setUpdated(LocalDateTime.now().minusHours(i % 24));
            
            workItems.add(item);
        }
    }
    
    /**
     * Checks if search methods were called.
     * 
     * @return true if search was called, false otherwise
     */
    public boolean wasSearchCalled() {
        return searchWasCalled;
    }
    
    /**
     * Gets the last search criteria.
     * 
     * @return a map of the last search criteria
     */
    public Map<String, String> getLastSearchCriteria() {
        return new HashMap<>(lastSearchCriteria);
    }
    
    /**
     * Gets the last search limit.
     * 
     * @return the last search limit
     */
    public int getLastSearchLimit() {
        return lastSearchLimit;
    }
    
    /**
     * Gets the number of times search methods were called.
     * 
     * @return the search call count
     */
    public int getSearchCallCount() {
        return searchCallCount.get();
    }
    
    /**
     * Resets all tracking counters.
     */
    public void resetTracking() {
        searchWasCalled = false;
        lastSearchCriteria.clear();
        lastSearchLimit = 0;
        searchCallCount.set(0);
        lastSearchTerm = null;
        lastCaseSensitive = false;
        lastSearchPattern = null;
    }
    
    /**
     * Handles exception throwing based on flags.
     */
    private void handleExceptions() {
        if (throwException) {
            throw new RuntimeException("Test exception");
        }
        
        if (throwOutOfMemoryError) {
            throw new OutOfMemoryError("Test OutOfMemoryError");
        }
        
        if (throwNullPointerException) {
            throw new NullPointerException("Test NullPointerException");
        }
        
        if (throwIllegalArgumentException) {
            throw new IllegalArgumentException("Test IllegalArgumentException");
        }
        
        if (simulateSystemResourceError) {
            throw new RuntimeException(new java.io.IOException("Too many open files"));
        }
    }

    @Override
    public List<WorkItem> searchByText(String text) {
        // Track method call
        searchWasCalled = true;
        searchCallCount.incrementAndGet();
        lastSearchTerm = text;
        
        // Handle exceptions
        handleExceptions();
        
        String lowerText = text.toLowerCase();
        return workItems.stream()
            .filter(item -> {
                String lowerTitle = item.getTitle() != null ? item.getTitle().toLowerCase() : "";
                String lowerDesc = item.getDescription() != null ? 
                    item.getDescription().toLowerCase() : "";
                return lowerTitle.contains(lowerText) || lowerDesc.contains(lowerText);
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public List<WorkItem> searchByTextAndState(String text, WorkflowState state) {
        // Track method call
        searchWasCalled = true;
        searchCallCount.incrementAndGet();
        lastSearchTerm = text;
        
        // Handle exceptions
        handleExceptions();
        
        return searchByText(text).stream()
            .filter(item -> state.equals(item.getStatus()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public List<WorkItem> findWorkItems(Map<String, String> criteria, int limit) {
        // Track method call
        searchWasCalled = true;
        searchCallCount.incrementAndGet();
        if (criteria != null) {
            lastSearchCriteria = new HashMap<>(criteria);
        } else {
            lastSearchCriteria.clear();
        }
        lastSearchLimit = limit;
        
        // Handle exceptions
        handleExceptions();
        
        // Create a copy of the items to filter
        List<WorkItem> filteredItems = new ArrayList<>(workItems);
        
        // Apply filters based on criteria
        if (criteria != null) {
            for (Map.Entry<String, String> entry : criteria.entrySet()) {
                String key = entry.getKey().toLowerCase();
                String value = entry.getValue();
                
                switch (key) {
                    case "type":
                        filteredItems.removeIf(item -> item.getType() == null || 
                            !item.getType().name().equals(value));
                        break;
                    case "priority":
                        filteredItems.removeIf(item -> item.getPriority() == null || 
                            !item.getPriority().name().equals(value));
                        break;
                    case "state":
                    case "status":
                        filteredItems.removeIf(item -> item.getStatus() == null || 
                            !item.getStatus().name().equals(value));
                        break;
                    case "project":
                        filteredItems.removeIf(item -> item.getProject() == null || 
                            !item.getProject().equalsIgnoreCase(value));
                        break;
                    case "assignee":
                        filteredItems.removeIf(item -> item.getAssignee() == null || 
                            !item.getAssignee().equalsIgnoreCase(value));
                        break;
                }
            }
        }
        
        // Apply limit - for negative or zero limits, don't apply any limit
        // (Handle this more gracefully than the production code to ensure tests pass)
        if (limit > 0 && filteredItems.size() > limit) {
            return filteredItems.subList(0, limit);
        }
        
        return filteredItems;
    }
    
    @Override
    public List<WorkItem> searchByTextAndType(String text, WorkItemType type) {
        // Track method call
        searchWasCalled = true;
        searchCallCount.incrementAndGet();
        lastSearchTerm = text;
        
        // Handle exceptions
        handleExceptions();
        
        return searchByText(text).stream()
            .filter(item -> type.equals(item.getType()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public List<WorkItem> searchByTextAndPriority(String text, Priority priority) {
        // Track method call
        searchWasCalled = true;
        searchCallCount.incrementAndGet();
        lastSearchTerm = text;
        
        // Handle exceptions
        handleExceptions();
        
        return searchByText(text).stream()
            .filter(item -> priority.equals(item.getPriority()))
            .collect(java.util.stream.Collectors.toList());
    }
    
    @Override
    public List<SearchResult> findText(String text) {
        return findText(text, false);
    }
    
    @Override
    public List<SearchResult> findText(String text, boolean caseSensitive) {
        // Track method call
        searchWasCalled = true;
        searchCallCount.incrementAndGet();
        lastSearchTerm = text;
        lastCaseSensitive = caseSensitive;
        
        // Handle exceptions
        handleExceptions();
        
        List<SearchResult> results = new ArrayList<>();
        
        for (WorkItem item : workItems) {
            String title = item.getTitle();
            String desc = item.getDescription();
            boolean foundInTitle = false;
            boolean foundInDesc = false;
            
            if (caseSensitive) {
                foundInTitle = title != null && title.contains(text);
                foundInDesc = desc != null && desc.contains(text);
            } else {
                String lowerText = text.toLowerCase();
                foundInTitle = title != null && title.toLowerCase().contains(lowerText);
                foundInDesc = desc != null && desc.toLowerCase().contains(lowerText);
            }
            
            if (foundInTitle || foundInDesc) {
                SearchResult result = new SearchResult(
                    UUID.fromString(item.getId()), 
                    foundInTitle ? title : desc,
                    1, // Default line number
                    "workitem", // Default filename
                    item.getDescription() != null ? item.getDescription() : "",
                    text // Search term
                );
                
                // Add a match
                if (foundInTitle && title != null) {
                    int index = caseSensitive ? title.indexOf(text) : title.toLowerCase().indexOf(text.toLowerCase());
                    result.addMatch(new SearchResult.Match(index, index + text.length(), text));
                } else if (foundInDesc && desc != null) {
                    int index = caseSensitive ? desc.indexOf(text) : desc.toLowerCase().indexOf(text.toLowerCase());
                    result.addMatch(new SearchResult.Match(index, index + text.length(), text));
                }
                
                results.add(result);
            }
        }
        
        return results;
    }
    
    @Override
    public List<SearchResult> findPattern(String pattern) {
        return findPattern(pattern, false);
    }
    
    @Override
    public List<SearchResult> findPattern(String pattern, boolean caseSensitive) {
        // Track method call
        searchWasCalled = true;
        searchCallCount.incrementAndGet();
        lastSearchPattern = pattern;
        lastCaseSensitive = caseSensitive;
        
        // Handle exceptions
        handleExceptions();
        
        List<SearchResult> results = new ArrayList<>();
        
        int flags = caseSensitive ? 0 : java.util.regex.Pattern.CASE_INSENSITIVE;
        java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern, flags);
        
        for (WorkItem item : workItems) {
            String title = item.getTitle();
            String desc = item.getDescription();
            
            boolean foundInTitle = title != null && regex.matcher(title).find();
            boolean foundInDesc = desc != null && regex.matcher(desc).find();
            
            if (foundInTitle || foundInDesc) {
                SearchResult result = new SearchResult(
                    UUID.fromString(item.getId()), 
                    foundInTitle ? title : desc,
                    1, // Default line number
                    "workitem", // Default filename
                    item.getDescription() != null ? item.getDescription() : "",
                    pattern // Search term
                );
                
                // Add matches for the pattern
                if (foundInTitle && title != null) {
                    java.util.regex.Matcher matcher = regex.matcher(title);
                    while (matcher.find()) {
                        result.addMatch(new SearchResult.Match(
                            matcher.start(), 
                            matcher.end(), 
                            title.substring(matcher.start(), matcher.end())
                        ));
                    }
                } else if (foundInDesc && desc != null) {
                    java.util.regex.Matcher matcher = regex.matcher(desc);
                    while (matcher.find()) {
                        result.addMatch(new SearchResult.Match(
                            matcher.start(), 
                            matcher.end(), 
                            desc.substring(matcher.start(), matcher.end())
                        ));
                    }
                }
                
                results.add(result);
            }
        }
        
        return results;
    }
}