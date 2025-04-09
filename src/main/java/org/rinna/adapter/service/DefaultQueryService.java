/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.adapter.service;

import org.rinna.domain.model.WorkItem;
import org.rinna.repository.ItemRepository;
import org.rinna.repository.MetadataRepository;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.QueryService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Default implementation of the QueryService interface.
 * Provides developer-focused filtering and querying capabilities.
 */
public class DefaultQueryService implements QueryService {

    private final ItemService itemService;
    private final ItemRepository itemRepository;
    private final MetadataRepository metadataRepository;

    /**
     * Creates a new DefaultQueryService.
     *
     * @param itemService       the item service
     * @param itemRepository    the item repository
     * @param metadataRepository the metadata repository
     */
    public DefaultQueryService(ItemService itemService, 
                              ItemRepository itemRepository,
                              MetadataRepository metadataRepository) {
        this.itemService = itemService;
        this.itemRepository = itemRepository;
        this.metadataRepository = metadataRepository;
    }

    @Override
    public List<WorkItem> queryWorkItems(QueryFilter filter) {
        // Get all work items first
        List<WorkItem> allItems = itemService.getAllWorkItems();
        
        // Apply filtering
        List<WorkItem> filteredItems = applyFilters(allItems, filter);
        
        // Apply sorting
        sortItems(filteredItems, filter);
        
        // Apply pagination
        return applyPagination(filteredItems, filter);
    }

    @Override
    public int countWorkItems(QueryFilter filter) {
        // Get all work items
        List<WorkItem> allItems = itemService.getAllWorkItems();
        
        // Apply filtering and return count
        return applyFilters(allItems, filter).size();
    }

    /**
     * Applies all filtering criteria to the list of work items.
     *
     * @param items the list of work items
     * @param filter the filter criteria
     * @return the filtered list
     */
    private List<WorkItem> applyFilters(List<WorkItem> items, QueryFilter filter) {
        List<Predicate<WorkItem>> predicates = new ArrayList<>();
        
        // Add text search predicate if pattern is specified
        if (filter.getTextPattern() != null && !filter.getTextPattern().isEmpty()) {
            predicates.add(createTextSearchPredicate(filter));
        }
        
        // Add type filter
        if (filter.getType() != null) {
            predicates.add(item -> item.type() == filter.getType());
        }
        
        // Add priority filter
        if (filter.getPriority() != null) {
            predicates.add(item -> item.priority() == filter.getPriority());
        }
        
        // Add state filter
        if (filter.getState() != null) {
            predicates.add(item -> item.state() == filter.getState());
        }
        
        // Add assignee filter
        if (filter.getAssignee() != null && !filter.getAssignee().isEmpty()) {
            predicates.add(item -> filter.getAssignee().equals(item.assignee()));
        }
        
        // Add reporter filter (using metadata)
        if (filter.getReporter() != null && !filter.getReporter().isEmpty()) {
            predicates.add(createReporterPredicate(filter));
        }
        
        // Add project filter
        if (filter.getProject() != null && !filter.getProject().isEmpty()) {
            predicates.add(item -> filter.getProject().equals(item.project()));
        }
        
        // Add date filters
        addDateFilters(predicates, filter);
        
        // Add link filter
        if (filter.getLinkedItemIds() != null && !filter.getLinkedItemIds().isEmpty()) {
            predicates.add(createLinkedItemsPredicate(filter));
        }
        
        // Add tag filter
        if (filter.getTags() != null && !filter.getTags().isEmpty()) {
            predicates.add(createTagsPredicate(filter));
        }
        
        // Combine all predicates with AND logic
        return items.stream()
                .filter(predicates.stream().reduce(x -> true, Predicate::and))
                .collect(Collectors.toList());
    }
    
    /**
     * Creates a predicate for text search.
     *
     * @param filter the filter criteria
     * @return the text search predicate
     */
    private Predicate<WorkItem> createTextSearchPredicate(QueryFilter filter) {
        Pattern pattern = preparePattern(filter.getTextPattern(), 
                                       filter.isCaseSensitive(), 
                                       filter.isExactMatch());
        
        return item -> {
            // Determine which fields to search
            List<String> fields = filter.getFields();
            
            // If no fields specified, search in title and description
            if (fields == null || fields.isEmpty()) {
                fields = List.of("title", "description");
            }
            
            // Check each field
            for (String field : fields) {
                switch (field.toLowerCase()) {
                    case "title":
                        if (item.title() != null && pattern.matcher(item.title()).find()) {
                            return true;
                        }
                        break;
                    case "description":
                        if (item.description() != null && pattern.matcher(item.description()).find()) {
                            return true;
                        }
                        break;
                    case "id":
                        if (item.id() != null && pattern.matcher(item.id().toString()).find()) {
                            return true;
                        }
                        break;
                    case "assignee":
                        if (item.assignee() != null && pattern.matcher(item.assignee()).find()) {
                            return true;
                        }
                        break;
                    case "project":
                        if (item.project() != null && pattern.matcher(item.project()).find()) {
                            return true;
                        }
                        break;
                    default:
                        // For other fields, check metadata
                        Map<String, String> metadata = metadataRepository.getMetadata(item.id());
                        if (metadata != null && metadata.containsKey(field) && 
                            pattern.matcher(metadata.get(field)).find()) {
                            return true;
                        }
                }
            }
            
            return false;
        };
    }
    
    /**
     * Creates a predicate for reporter filtering.
     *
     * @param filter the filter criteria
     * @return the reporter predicate
     */
    private Predicate<WorkItem> createReporterPredicate(QueryFilter filter) {
        return item -> {
            Map<String, String> metadata = metadataRepository.getMetadata(item.id());
            return metadata != null && 
                  filter.getReporter().equals(metadata.getOrDefault("reporter", ""));
        };
    }
    
    /**
     * Adds date-related filters to the predicate list.
     *
     * @param predicates the list of predicates
     * @param filter the filter criteria
     */
    private void addDateFilters(List<Predicate<WorkItem>> predicates, QueryFilter filter) {
        // Created after
        if (filter.getCreatedAfter() != null) {
            predicates.add(item -> {
                LocalDateTime created = item.created();
                return created != null && created.isAfter(filter.getCreatedAfter());
            });
        }
        
        // Created before
        if (filter.getCreatedBefore() != null) {
            predicates.add(item -> {
                LocalDateTime created = item.created();
                return created != null && created.isBefore(filter.getCreatedBefore());
            });
        }
        
        // Updated after
        if (filter.getUpdatedAfter() != null) {
            predicates.add(item -> {
                // Use metadata for last updated time
                Map<String, String> metadata = metadataRepository.getMetadata(item.id());
                if (metadata != null && metadata.containsKey("last_updated")) {
                    try {
                        LocalDateTime updated = LocalDateTime.parse(metadata.get("last_updated"));
                        return updated.isAfter(filter.getUpdatedAfter());
                    } catch (Exception e) {
                        // If parsing fails, skip this predicate
                        return true;
                    }
                }
                return true; // No update time, assume it matches
            });
        }
        
        // Updated before
        if (filter.getUpdatedBefore() != null) {
            predicates.add(item -> {
                // Use metadata for last updated time
                Map<String, String> metadata = metadataRepository.getMetadata(item.id());
                if (metadata != null && metadata.containsKey("last_updated")) {
                    try {
                        LocalDateTime updated = LocalDateTime.parse(metadata.get("last_updated"));
                        return updated.isBefore(filter.getUpdatedBefore());
                    } catch (Exception e) {
                        // If parsing fails, skip this predicate
                        return true;
                    }
                }
                return true; // No update time, assume it matches
            });
        }
    }
    
    /**
     * Creates a predicate for linked items filtering.
     *
     * @param filter the filter criteria
     * @return the linked items predicate
     */
    private Predicate<WorkItem> createLinkedItemsPredicate(QueryFilter filter) {
        return item -> {
            // In a real implementation, we would check the relationships from a dedicated repository
            // For now, we'll use a simplified approach with metadata
            Map<String, String> metadata = metadataRepository.getMetadata(item.id());
            if (metadata == null || !metadata.containsKey("linked_items")) {
                return false;
            }
            
            String linkedItemsStr = metadata.get("linked_items");
            List<UUID> linkedItems = parseLinkedItems(linkedItemsStr);
            
            // Check if any of the required linked items are in this item's linked items
            return linkedItems.stream().anyMatch(filter.getLinkedItemIds()::contains);
        };
    }
    
    /**
     * Parses linked items from a string.
     *
     * @param linkedItemsStr the linked items string
     * @return the list of linked item IDs
     */
    private List<UUID> parseLinkedItems(String linkedItemsStr) {
        List<UUID> result = new ArrayList<>();
        if (linkedItemsStr == null || linkedItemsStr.isEmpty()) {
            return result;
        }
        
        String[] items = linkedItemsStr.split(",");
        for (String item : items) {
            try {
                result.add(UUID.fromString(item.trim()));
            } catch (IllegalArgumentException e) {
                // Skip invalid UUIDs
            }
        }
        
        return result;
    }
    
    /**
     * Creates a predicate for tags filtering.
     *
     * @param filter the filter criteria
     * @return the tags predicate
     */
    private Predicate<WorkItem> createTagsPredicate(QueryFilter filter) {
        return item -> {
            Map<String, String> metadata = metadataRepository.getMetadata(item.id());
            if (metadata == null || !metadata.containsKey("tags")) {
                return false;
            }
            
            String tagsStr = metadata.get("tags");
            List<String> tags = parseTags(tagsStr);
            
            // Check if all required tags are present
            return tags.containsAll(filter.getTags());
        };
    }
    
    /**
     * Parses tags from a string.
     *
     * @param tagsStr the tags string
     * @return the list of tags
     */
    private List<String> parseTags(String tagsStr) {
        List<String> result = new ArrayList<>();
        if (tagsStr == null || tagsStr.isEmpty()) {
            return result;
        }
        
        String[] tags = tagsStr.split(",");
        for (String tag : tags) {
            result.add(tag.trim().toLowerCase());
        }
        
        return result;
    }
    
    /**
     * Sorts the list of work items according to the filter criteria.
     *
     * @param items the list of work items
     * @param filter the filter criteria
     */
    private void sortItems(List<WorkItem> items, QueryFilter filter) {
        String sortField = filter.getSortBy();
        if (sortField == null || sortField.isEmpty()) {
            sortField = "created"; // Default sort field
        }
        
        Comparator<WorkItem> comparator = null;
        
        switch (sortField.toLowerCase()) {
            case "id":
                comparator = Comparator.comparing(WorkItem::id);
                break;
            case "title":
                comparator = Comparator.comparing(WorkItem::title, 
                        Comparator.nullsLast(String::compareToIgnoreCase));
                break;
            case "type":
                comparator = Comparator.comparing(WorkItem::type, Comparator.nullsLast(Enum::compareTo));
                break;
            case "priority":
                comparator = Comparator.comparing(WorkItem::priority, Comparator.nullsLast(Enum::compareTo));
                break;
            case "state":
                comparator = Comparator.comparing(WorkItem::state, Comparator.nullsLast(Enum::compareTo));
                break;
            case "assignee":
                comparator = Comparator.comparing(WorkItem::assignee, 
                        Comparator.nullsLast(String::compareToIgnoreCase));
                break;
            case "project":
                comparator = Comparator.comparing(WorkItem::project, 
                        Comparator.nullsLast(String::compareToIgnoreCase));
                break;
            case "created":
            default:
                comparator = Comparator.comparing(WorkItem::created, Comparator.nullsLast(LocalDateTime::compareTo));
                break;
        }
        
        // Reverse order for descending sort
        if (!filter.isAscending()) {
            comparator = comparator.reversed();
        }
        
        // Sort the list
        items.sort(comparator);
    }
    
    /**
     * Applies pagination to the list of work items.
     *
     * @param items the list of work items
     * @param filter the filter criteria
     * @return the paginated list
     */
    private List<WorkItem> applyPagination(List<WorkItem> items, QueryFilter filter) {
        int offset = Math.max(0, filter.getOffset());
        int limit = Math.max(1, filter.getLimit());
        
        // Apply offset
        List<WorkItem> result = offset < items.size() ? 
                items.subList(offset, items.size()) : new ArrayList<>();
        
        // Apply limit
        return result.size() > limit ? result.subList(0, limit) : result;
    }
    
    /**
     * Prepares a regex pattern based on search settings.
     *
     * @param pattern the search pattern
     * @param caseSensitive true for case-sensitive search
     * @param exactMatch true for whole word matching
     * @return the compiled pattern
     */
    private Pattern preparePattern(String pattern, boolean caseSensitive, boolean exactMatch) {
        String regex;
        
        if (exactMatch) {
            // Match whole words only
            regex = "\\b" + Pattern.quote(pattern) + "\\b";
        } else {
            // Match anywhere in text
            regex = Pattern.quote(pattern);
        }
        
        // Set case sensitivity flag
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
        
        return Pattern.compile(regex, flags);
    }
}