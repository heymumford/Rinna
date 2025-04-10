/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.usecase;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.WorkflowState;

/**
 * Service interface for developer-focused querying and filtering of work items.
 * QueryService extends beyond the basic search functionality to provide advanced filtering
 * capabilities for developers to find relevant work items using multiple criteria.
 */
public interface QueryService {
    
    /**
     * Query work items with the given filter criteria.
     *
     * @param filter the filter criteria to apply
     * @return the list of work items matching the criteria
     */
    List<WorkItem> queryWorkItems(QueryFilter filter);
    
    /**
     * Count work items matching the given filter criteria.
     * 
     * @param filter the filter criteria to apply
     * @return the count of work items matching the criteria
     */
    int countWorkItems(QueryFilter filter);
    
    /**
     * Filter to specify query criteria.
     * This builder-pattern class allows for flexible query construction.
     */
    class QueryFilter {
        private String textPattern;
        private boolean caseSensitive;
        private boolean exactMatch;
        private List<String> fields;
        
        private WorkItemType type;
        private Priority priority;
        private WorkflowState state;
        private String assignee;
        private String reporter;
        private String project;
        
        private LocalDateTime createdAfter;
        private LocalDateTime createdBefore;
        private LocalDateTime updatedAfter;
        private LocalDateTime updatedBefore;
        
        private List<UUID> linkedItemIds;
        private List<String> tags;
        
        private int limit = 100;
        private int offset = 0;
        private String sortBy = "created";
        private boolean ascending = false;
        
        private QueryFilter() {}
        
        /**
         * Creates a new query filter builder.
         *
         * @return the query filter builder
         */
        public static QueryFilter create() {
            return new QueryFilter();
        }
        
        /**
         * Sets text search pattern.
         *
         * @param pattern the search pattern
         * @return this filter for chaining
         */
        public QueryFilter withText(String pattern) {
            this.textPattern = pattern;
            return this;
        }
        
        /**
         * Sets case sensitivity for text search.
         *
         * @param caseSensitive true for case-sensitive search
         * @return this filter for chaining
         */
        public QueryFilter caseSensitive(boolean caseSensitive) {
            this.caseSensitive = caseSensitive;
            return this;
        }
        
        /**
         * Sets exact match for text search.
         *
         * @param exactMatch true for whole word matching
         * @return this filter for chaining
         */
        public QueryFilter exactMatch(boolean exactMatch) {
            this.exactMatch = exactMatch;
            return this;
        }
        
        /**
         * Sets fields to search in.
         *
         * @param fields the fields to search in
         * @return this filter for chaining
         */
        public QueryFilter inFields(List<String> fields) {
            this.fields = fields;
            return this;
        }
        
        /**
         * Filters by work item type.
         *
         * @param type the work item type
         * @return this filter for chaining
         */
        public QueryFilter ofType(WorkItemType type) {
            this.type = type;
            return this;
        }
        
        /**
         * Filters by priority.
         *
         * @param priority the priority
         * @return this filter for chaining
         */
        public QueryFilter withPriority(Priority priority) {
            this.priority = priority;
            return this;
        }
        
        /**
         * Filters by workflow state.
         *
         * @param state the workflow state
         * @return this filter for chaining
         */
        public QueryFilter inState(WorkflowState state) {
            this.state = state;
            return this;
        }
        
        /**
         * Filters by assignee.
         *
         * @param assignee the assignee username
         * @return this filter for chaining
         */
        public QueryFilter assignedTo(String assignee) {
            this.assignee = assignee;
            return this;
        }
        
        /**
         * Filters by reporter.
         *
         * @param reporter the reporter username
         * @return this filter for chaining
         */
        public QueryFilter reportedBy(String reporter) {
            this.reporter = reporter;
            return this;
        }
        
        /**
         * Filters by project.
         *
         * @param project the project identifier
         * @return this filter for chaining
         */
        public QueryFilter inProject(String project) {
            this.project = project;
            return this;
        }
        
        /**
         * Filters by creation date range (after).
         *
         * @param date the date to filter after
         * @return this filter for chaining
         */
        public QueryFilter createdAfter(LocalDateTime date) {
            this.createdAfter = date;
            return this;
        }
        
        /**
         * Filters by creation date range (before).
         *
         * @param date the date to filter before
         * @return this filter for chaining
         */
        public QueryFilter createdBefore(LocalDateTime date) {
            this.createdBefore = date;
            return this;
        }
        
        /**
         * Filters for items created on the given date.
         *
         * @param date the date
         * @return this filter for chaining
         */
        public QueryFilter createdOn(LocalDate date) {
            this.createdAfter = date.atStartOfDay();
            this.createdBefore = date.plusDays(1).atStartOfDay();
            return this;
        }
        
        /**
         * Filters by update date range (after).
         *
         * @param date the date to filter after
         * @return this filter for chaining
         */
        public QueryFilter updatedAfter(LocalDateTime date) {
            this.updatedAfter = date;
            return this;
        }
        
        /**
         * Filters by update date range (before).
         *
         * @param date the date to filter before
         * @return this filter for chaining
         */
        public QueryFilter updatedBefore(LocalDateTime date) {
            this.updatedBefore = date;
            return this;
        }
        
        /**
         * Filters for items updated on the given date.
         *
         * @param date the date
         * @return this filter for chaining
         */
        public QueryFilter updatedOn(LocalDate date) {
            this.updatedAfter = date.atStartOfDay();
            this.updatedBefore = date.plusDays(1).atStartOfDay();
            return this;
        }
        
        /**
         * Filters by linked item IDs.
         *
         * @param itemIds the linked item IDs
         * @return this filter for chaining
         */
        public QueryFilter linkedTo(List<UUID> itemIds) {
            this.linkedItemIds = itemIds;
            return this;
        }
        
        /**
         * Filters by tags.
         *
         * @param tags the tags
         * @return this filter for chaining
         */
        public QueryFilter withTags(List<String> tags) {
            this.tags = tags;
            return this;
        }
        
        /**
         * Sets result limit.
         *
         * @param limit the maximum number of results
         * @return this filter for chaining
         */
        public QueryFilter limit(int limit) {
            this.limit = limit;
            return this;
        }
        
        /**
         * Sets result offset for pagination.
         *
         * @param offset the result offset
         * @return this filter for chaining
         */
        public QueryFilter offset(int offset) {
            this.offset = offset;
            return this;
        }
        
        /**
         * Sets sort field.
         *
         * @param field the field to sort by
         * @return this filter for chaining
         */
        public QueryFilter sortBy(String field) {
            this.sortBy = field;
            return this;
        }
        
        /**
         * Sets sort direction.
         *
         * @param ascending true for ascending order
         * @return this filter for chaining
         */
        public QueryFilter ascending(boolean ascending) {
            this.ascending = ascending;
            return this;
        }
        
        // Getters for all fields
        
        public String getTextPattern() {
            return textPattern;
        }
        
        public boolean isCaseSensitive() {
            return caseSensitive;
        }
        
        public boolean isExactMatch() {
            return exactMatch;
        }
        
        public List<String> getFields() {
            return fields;
        }
        
        public WorkItemType getType() {
            return type;
        }
        
        public Priority getPriority() {
            return priority;
        }
        
        public WorkflowState getState() {
            return state;
        }
        
        public String getAssignee() {
            return assignee;
        }
        
        public String getReporter() {
            return reporter;
        }
        
        public String getProject() {
            return project;
        }
        
        public LocalDateTime getCreatedAfter() {
            return createdAfter;
        }
        
        public LocalDateTime getCreatedBefore() {
            return createdBefore;
        }
        
        public LocalDateTime getUpdatedAfter() {
            return updatedAfter;
        }
        
        public LocalDateTime getUpdatedBefore() {
            return updatedBefore;
        }
        
        public List<UUID> getLinkedItemIds() {
            return linkedItemIds;
        }
        
        public List<String> getTags() {
            return tags;
        }
        
        public int getLimit() {
            return limit;
        }
        
        public int getOffset() {
            return offset;
        }
        
        public String getSortBy() {
            return sortBy;
        }
        
        public boolean isAscending() {
            return ascending;
        }
    }
}