/*
 * Mock statistics service for testing
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.service;

import java.util.*;
import java.util.stream.Collectors;

import org.rinna.cli.stats.StatisticType;
import org.rinna.cli.stats.StatisticValue;

/**
 * Mock implementation of the StatisticsService for testing.
 */
public class MockStatisticsService {
    
    private Map<StatisticType, StatisticValue> statisticsMap = new HashMap<>();
    private boolean refreshCalled = false;
    
    /**
     * Initializes the mock service with default statistics.
     */
    public MockStatisticsService() {
        setupDefaultStatistics();
    }
    
    /**
     * Sets up default statistics for testing.
     */
    private void setupDefaultStatistics() {
        // Total items
        statisticsMap.put(StatisticType.TOTAL_ITEMS, 
            StatisticValue.createCount(StatisticType.TOTAL_ITEMS, 8, "Total work items"));
        
        // Items by state
        Map<String, Double> stateDistribution = new HashMap<>();
        stateDistribution.put("DONE", 4.0);
        stateDistribution.put("IN_PROGRESS", 2.0);
        stateDistribution.put("OPEN", 2.0);
        statisticsMap.put(StatisticType.ITEMS_BY_STATE, 
            StatisticValue.createDistribution(StatisticType.ITEMS_BY_STATE, stateDistribution, "Work items by state"));
        
        // Items by priority
        Map<String, Double> priorityDistribution = new HashMap<>();
        priorityDistribution.put("CRITICAL", 1.0);
        priorityDistribution.put("HIGH", 3.0);
        priorityDistribution.put("MEDIUM", 2.0);
        priorityDistribution.put("LOW", 2.0);
        statisticsMap.put(StatisticType.ITEMS_BY_PRIORITY, 
            StatisticValue.createDistribution(StatisticType.ITEMS_BY_PRIORITY, priorityDistribution, "Work items by priority"));
        
        // Items by type
        Map<String, Double> typeDistribution = new HashMap<>();
        typeDistribution.put("FEATURE", 2.0);
        typeDistribution.put("BUG", 2.0);
        typeDistribution.put("TASK", 4.0);
        statisticsMap.put(StatisticType.ITEMS_BY_TYPE, 
            StatisticValue.createDistribution(StatisticType.ITEMS_BY_TYPE, typeDistribution, "Work items by type"));
        
        // Items by assignee
        Map<String, Double> assigneeDistribution = new HashMap<>();
        assigneeDistribution.put("alice", 3.0);
        assigneeDistribution.put("bob", 2.0);
        assigneeDistribution.put("charlie", 2.0);
        assigneeDistribution.put("dave", 1.0);
        statisticsMap.put(StatisticType.ITEMS_BY_ASSIGNEE, 
            StatisticValue.createDistribution(StatisticType.ITEMS_BY_ASSIGNEE, assigneeDistribution, "Work items by assignee"));
        
        // Completion rate
        statisticsMap.put(StatisticType.COMPLETION_RATE, 
            StatisticValue.createPercentage(StatisticType.COMPLETION_RATE, 50.0, "Completion rate"));
        
        // Average completion time
        statisticsMap.put(StatisticType.AVG_COMPLETION_TIME, 
            StatisticValue.createTime(StatisticType.AVG_COMPLETION_TIME, 5.3, "days", "Average completion time"));
        
        // Items completed
        statisticsMap.put(StatisticType.ITEMS_COMPLETED, 
            StatisticValue.createCount(StatisticType.ITEMS_COMPLETED, 4, "Items completed in the last 30 days"));
        
        // Items created
        statisticsMap.put(StatisticType.ITEMS_CREATED, 
            StatisticValue.createCount(StatisticType.ITEMS_CREATED, 2, "Items created in the last 30 days"));
        
        // Overdue items
        statisticsMap.put(StatisticType.OVERDUE_ITEMS, 
            StatisticValue.createCount(StatisticType.OVERDUE_ITEMS, 1, "Overdue items"));
        
        // Work in progress
        statisticsMap.put(StatisticType.WORK_IN_PROGRESS, 
            StatisticValue.createCount(StatisticType.WORK_IN_PROGRESS, 2, "Work in progress"));
        
        // Throughput
        statisticsMap.put(StatisticType.THROUGHPUT, 
            StatisticValue.createTime(StatisticType.THROUGHPUT, 0.57, "items/day", "Throughput (items/day, last 7 days)"));
        
        // Lead time
        statisticsMap.put(StatisticType.LEAD_TIME, 
            StatisticValue.createTime(StatisticType.LEAD_TIME, 6.8, "days", "Lead time (creation to completion)"));
        
        // Cycle time
        statisticsMap.put(StatisticType.CYCLE_TIME, 
            StatisticValue.createTime(StatisticType.CYCLE_TIME, 4.2, "days", "Cycle time (in progress to completion)"));
        
        // Burndown rate
        statisticsMap.put(StatisticType.BURNDOWN_RATE, 
            StatisticValue.createTime(StatisticType.BURNDOWN_RATE, 0.5, "items/day", "Burndown rate (items per day, last 14 days)"));
    }
    
    /**
     * Gets a specific statistic by type.
     * 
     * @param type the statistic type
     * @return the statistic value, or null if not available
     */
    public StatisticValue getStatistic(StatisticType type) {
        return statisticsMap.get(type);
    }
    
    /**
     * Gets all available statistics.
     * 
     * @return the list of all statistics
     */
    public List<StatisticValue> getAllStatistics() {
        return new ArrayList<>(statisticsMap.values());
    }
    
    /**
     * Gets summary statistics (a curated subset of the most important stats).
     * 
     * @return the list of summary statistics
     */
    public List<StatisticValue> getSummaryStatistics() {
        // Return a curated list of the most important statistics
        return Arrays.asList(
            statisticsMap.get(StatisticType.TOTAL_ITEMS),
            statisticsMap.get(StatisticType.COMPLETION_RATE),
            statisticsMap.get(StatisticType.WORK_IN_PROGRESS),
            statisticsMap.get(StatisticType.OVERDUE_ITEMS),
            statisticsMap.get(StatisticType.THROUGHPUT)
        );
    }
    
    /**
     * Gets distribution statistics for a given category.
     * 
     * @param category the distribution category (status, priority, type, assignee)
     * @return the statistics for that distribution
     */
    public StatisticValue getDistributionStatistics(String category) {
        switch (category.toLowerCase()) {
            case "status":
                return statisticsMap.get(StatisticType.ITEMS_BY_STATE);
            case "priority":
                return statisticsMap.get(StatisticType.ITEMS_BY_PRIORITY);
            case "type":
                return statisticsMap.get(StatisticType.ITEMS_BY_TYPE);
            case "assignee":
                return statisticsMap.get(StatisticType.ITEMS_BY_ASSIGNEE);
            default:
                return null;
        }
    }
    
    /**
     * Gets detailed metrics for a specific area (completion, workflow).
     * 
     * @param area the area to get detailed metrics for
     * @return the list of statistics for that area
     */
    public List<StatisticValue> getDetailedMetrics(String area) {
        switch (area.toLowerCase()) {
            case "completion":
                return Arrays.asList(
                    statisticsMap.get(StatisticType.COMPLETION_RATE),
                    statisticsMap.get(StatisticType.AVG_COMPLETION_TIME),
                    statisticsMap.get(StatisticType.ITEMS_COMPLETED)
                );
            case "workflow":
                return Arrays.asList(
                    statisticsMap.get(StatisticType.LEAD_TIME),
                    statisticsMap.get(StatisticType.CYCLE_TIME),
                    statisticsMap.get(StatisticType.WORK_IN_PROGRESS)
                );
            default:
                return new ArrayList<>();
        }
    }
    
    /**
     * Gets dashboard statistics.
     * 
     * @return the list of dashboard statistics
     */
    public Map<String, Object> getDashboardStatistics() {
        Map<String, Object> dashboard = new HashMap<>();
        
        // Add summary statistics
        dashboard.put("summary", getSummaryStatistics().stream()
            .collect(Collectors.toMap(s -> s.getType().name(), StatisticValue::getValue)));
        
        // Add distributions
        dashboard.put("statusDistribution", statisticsMap.get(StatisticType.ITEMS_BY_STATE).getBreakdown());
        dashboard.put("priorityDistribution", statisticsMap.get(StatisticType.ITEMS_BY_PRIORITY).getBreakdown());
        dashboard.put("typeDistribution", statisticsMap.get(StatisticType.ITEMS_BY_TYPE).getBreakdown());
        
        // Add charts data
        Map<String, Object> progressData = new HashMap<>();
        progressData.put("labels", Arrays.asList("Week 1", "Week 2", "Week 3", "Week 4"));
        progressData.put("completed", Arrays.asList(2, 3, 5, 7));
        progressData.put("created", Arrays.asList(3, 2, 3, 1));
        dashboard.put("progressChart", progressData);
        
        return dashboard;
    }
    
    /**
     * Marks the refresh method as called.
     */
    public void refreshStatistics() {
        this.refreshCalled = true;
    }
    
    /**
     * Checks if the refresh method was called.
     * 
     * @return true if refresh was called
     */
    public boolean wasRefreshCalled() {
        return refreshCalled;
    }
    
    /**
     * Resets the mock state.
     */
    public void reset() {
        refreshCalled = false;
        setupDefaultStatistics();
    }
    
    /**
     * Sets a custom statistic value for testing.
     * 
     * @param type the statistic type
     * @param value the statistic value
     */
    public void setStatistic(StatisticType type, StatisticValue value) {
        statisticsMap.put(type, value);
    }
}