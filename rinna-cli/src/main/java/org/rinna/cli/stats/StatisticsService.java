/*
 * Statistics service for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.stats;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

import org.rinna.cli.adapter.MockItemServiceAdapter;
import org.rinna.cli.adapter.StatisticItemAdapter;
import org.rinna.cli.domain.model.WorkflowState;
import org.rinna.cli.model.Priority;
import org.rinna.cli.model.WorkItemType;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.ServiceManager;

/**
 * Service for computing and tracking statistics about work items and workflow.
 */
public final class StatisticsService {
    private static StatisticsService instance;
    
    private final List<StatisticValue> cachedStatistics = new ArrayList<>();
    private Instant lastRefresh = Instant.EPOCH;
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);
    
    /**
     * Gets the singleton instance of the statistics service.
     *
     * @return the singleton instance
     */
    public static synchronized StatisticsService getInstance() {
        if (instance == null) {
            instance = new StatisticsService();
        }
        return instance;
    }
    
    private StatisticsService() {
        // Private constructor to enforce singleton pattern
    }
    
    /**
     * Gets a specific statistic by type.
     * 
     * @param type the statistic type
     * @return the statistic value, or null if not available
     */
    public StatisticValue getStatistic(StatisticType type) {
        ensureStatisticsAreFresh();
        
        return cachedStatistics.stream()
            .filter(stat -> stat.getType() == type)
            .findFirst()
            .orElse(computeStatistic(type));
    }
    
    /**
     * Gets all available statistics.
     * 
     * @return the list of all statistics
     */
    public List<StatisticValue> getAllStatistics() {
        ensureStatisticsAreFresh();
        return new ArrayList<>(cachedStatistics);
    }
    
    /**
     * Gets statistics for a specific project.
     * 
     * @param projectId the project ID
     * @return the list of statistics for the project
     */
    public List<StatisticValue> getProjectStatistics(String projectId) {
        if (projectId == null || projectId.isEmpty()) {
            return new ArrayList<>();
        }
        
        Object itemServiceObj = ServiceManager.getInstance().getItemService();
        if (itemServiceObj == null) {
            return new ArrayList<>();
        }
        
        MockItemService itemService = (MockItemService) itemServiceObj;
        MockItemServiceAdapter adapter = new MockItemServiceAdapter(itemService);
        List<StatisticItemAdapter> allItems = adapter.getAllItems();
        
        // Filter items by project
        List<StatisticItemAdapter> projectItems = allItems.stream()
            .filter(item -> {
                if (item.getProjectId() == null) {
                    return false;
                }
                return projectId.equals(item.getProjectId());
            })
            .collect(Collectors.toList());
        
        if (projectItems.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Compute project-specific statistics
        List<StatisticValue> statistics = new ArrayList<>();
        
        // 1. Total items in project
        statistics.add(StatisticValue.createCount(
            StatisticType.TOTAL_ITEMS, 
            projectItems.size(), 
            "Total items in project " + projectId
        ));
        
        // 2. Item distribution by type
        Map<String, Double> typeDistribution = projectItems.stream()
            .collect(Collectors.groupingBy(
                item -> item.getWorkItem().getType().name(),
                Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (double) entry.getValue()));
        
        statistics.add(StatisticValue.createDistribution(
            StatisticType.ITEMS_BY_TYPE, 
            typeDistribution, 
            "Items by type in project " + projectId
        ));
        
        // 3. Item distribution by state
        Map<String, Double> stateDistribution = projectItems.stream()
            .collect(Collectors.groupingBy(
                item -> item.getWorkItem().getState().name(),
                Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (double) entry.getValue()));
        
        statistics.add(StatisticValue.createDistribution(
            StatisticType.ITEMS_BY_STATE, 
            stateDistribution, 
            "Items by state in project " + projectId
        ));
        
        // 4. Completion rate
        long completedCount = projectItems.stream()
            .filter(item -> item.getWorkItem().getState().name().equals("DONE") || 
                          item.getWorkItem().getState().name().equals("COMPLETED"))
            .count();
        
        double completionRate = (double) completedCount / projectItems.size() * 100.0;
        
        statistics.add(StatisticValue.createPercentage(
            StatisticType.COMPLETION_RATE, 
            completionRate, 
            "Completion rate for project " + projectId
        ));
        
        // 5. Work in progress
        long wipCount = projectItems.stream()
            .filter(item -> item.getWorkItem().getState().name().equals("IN_PROGRESS"))
            .count();
        
        statistics.add(StatisticValue.createCount(
            StatisticType.WORK_IN_PROGRESS, 
            (int) wipCount, 
            "Work in progress for project " + projectId
        ));
        
        // 6. Items by assignee
        Map<String, Double> assigneeDistribution = projectItems.stream()
            .filter(item -> item.getWorkItem().getAssignee() != null && !item.getWorkItem().getAssignee().isEmpty())
            .collect(Collectors.groupingBy(
                item -> item.getWorkItem().getAssignee(),
                Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (double) entry.getValue()));
        
        // Add "Unassigned" count
        long unassignedCount = projectItems.stream()
            .filter(item -> item.getWorkItem().getAssignee() == null || item.getWorkItem().getAssignee().isEmpty())
            .count();
        
        if (unassignedCount > 0) {
            assigneeDistribution.put("Unassigned", (double) unassignedCount);
        }
        
        statistics.add(StatisticValue.createDistribution(
            StatisticType.ITEMS_BY_ASSIGNEE, 
            assigneeDistribution, 
            "Items by assignee in project " + projectId
        ));
        
        // 7. Overdue items
        Instant now = Instant.now();
        
        long overdueCount = projectItems.stream()
            .filter(item -> !item.getWorkItem().getState().name().equals("DONE") && 
                           !item.getWorkItem().getState().name().equals("COMPLETED"))
            .filter(item -> item.getDueDate() != null && item.getDueDate().isBefore(now))
            .count();
        
        statistics.add(StatisticValue.createCount(
            StatisticType.OVERDUE_ITEMS, 
            (int) overdueCount, 
            "Overdue items in project " + projectId
        ));
        
        return statistics;
    }
    
    /**
     * Gets summary statistics (a curated subset of the most important stats).
     * 
     * @return the list of summary statistics
     */
    public List<StatisticValue> getSummaryStatistics() {
        ensureStatisticsAreFresh();
        
        // Return a curated list of the most important statistics
        return cachedStatistics.stream()
            .filter(stat -> Arrays.asList(
                StatisticType.TOTAL_ITEMS,
                StatisticType.COMPLETION_RATE,
                StatisticType.WORK_IN_PROGRESS,
                StatisticType.OVERDUE_ITEMS,
                StatisticType.THROUGHPUT
            ).contains(stat.getType()))
            .collect(Collectors.toList());
    }
    
    /**
     * Gets statistics by category (project, time period, assignee, etc.).
     * 
     * @param category the category name
     * @param value the category value
     * @return the list of statistics for the category
     */
    public List<StatisticValue> getStatisticsByCategory(String category, String value) {
        if (category == null || value == null || category.isEmpty() || value.isEmpty()) {
            return new ArrayList<>();
        }
        
        Object itemServiceObj = ServiceManager.getInstance().getItemService();
        if (itemServiceObj == null) {
            return new ArrayList<>();
        }
        
        MockItemService itemService = (MockItemService) itemServiceObj;
        MockItemServiceAdapter adapter = new MockItemServiceAdapter(itemService);
        List<StatisticItemAdapter> allItems = adapter.getAllItems();
        
        // Filter items by the specified category
        List<StatisticItemAdapter> filteredItems;
        String categoryDescription;
        
        switch (category.toLowerCase()) {
            case "project":
                filteredItems = allItems.stream()
                    .filter(item -> value.equals(item.getProjectId()))
                    .collect(Collectors.toList());
                categoryDescription = "project " + value;
                break;
                
            case "assignee":
                filteredItems = allItems.stream()
                    .filter(item -> value.equals(item.getWorkItem().getAssignee()))
                    .collect(Collectors.toList());
                categoryDescription = "assignee " + value;
                break;
                
            case "type":
                WorkItemType itemType = null;
                try {
                    itemType = WorkItemType.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return new ArrayList<>();
                }
                
                final WorkItemType finalType = itemType;
                filteredItems = allItems.stream()
                    .filter(item -> finalType == item.getWorkItem().getType())
                    .collect(Collectors.toList());
                categoryDescription = "type " + finalType.name();
                break;
                
            case "priority":
                Priority priority = null;
                try {
                    priority = Priority.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return new ArrayList<>();
                }
                
                final Priority finalPriority = priority;
                filteredItems = allItems.stream()
                    .filter(item -> finalPriority == item.getWorkItem().getPriority())
                    .collect(Collectors.toList());
                categoryDescription = "priority " + finalPriority.name();
                break;
                
            case "state":
            case "status":
                WorkflowState state = null;
                try {
                    state = WorkflowState.valueOf(value.toUpperCase());
                } catch (IllegalArgumentException e) {
                    return new ArrayList<>();
                }
                
                final WorkflowState finalState = state;
                filteredItems = allItems.stream()
                    .filter(item -> finalState.name().equals(item.getWorkItem().getState().name()))
                    .collect(Collectors.toList());
                categoryDescription = "state " + finalState.name();
                break;
                
            case "timeperiod":
            case "period":
                int days;
                try {
                    days = Integer.parseInt(value);
                } catch (NumberFormatException e) {
                    days = 30; // Default to 30 days
                }
                
                Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
                filteredItems = allItems.stream()
                    .filter(item -> item.getCreatedAt() != null && !item.getCreatedAt().isBefore(cutoff))
                    .collect(Collectors.toList());
                categoryDescription = "last " + days + " days";
                break;
                
            default:
                return new ArrayList<>();
        }
        
        if (filteredItems.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Compute statistics for the filtered items
        List<StatisticValue> statistics = new ArrayList<>();
        
        // 1. Total items in category
        statistics.add(StatisticValue.createCount(
            StatisticType.TOTAL_ITEMS, 
            filteredItems.size(), 
            "Total items for " + categoryDescription
        ));
        
        // Different statistics depending on the category
        if (category.equalsIgnoreCase("timeperiod") || category.equalsIgnoreCase("period")) {
            // For time periods, focus on completed items and throughput
            int days;
            try {
                days = Integer.parseInt(value);
            } catch (NumberFormatException e) {
                days = 30; // Default to 30 days
            }
            
            // Count of completed items in the period
            long completedCount = filteredItems.stream()
                .filter(item -> item.getWorkItem().getState().name().equals("DONE") || 
                              item.getWorkItem().getState().name().equals("COMPLETED"))
                .count();
            
            statistics.add(StatisticValue.createCount(
                StatisticType.ITEMS_COMPLETED, 
                (int) completedCount, 
                "Items completed in the last " + days + " days"
            ));
            
            // Throughput calculation
            double throughput = (double) completedCount / days;
            statistics.add(StatisticValue.createTime(
                StatisticType.THROUGHPUT, 
                throughput, 
                "items/day", 
                "Throughput (items/day, last " + days + " days)"
            ));
            
            // Item distribution by type
            Map<String, Double> typeDistribution = filteredItems.stream()
                .collect(Collectors.groupingBy(
                    item -> item.getWorkItem().getType().name(),
                    Collectors.counting()))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> (double) entry.getValue()));
            
            statistics.add(StatisticValue.createDistribution(
                StatisticType.ITEMS_BY_TYPE, 
                typeDistribution, 
                "Items by type for " + categoryDescription
            ));
        } else if (category.equalsIgnoreCase("assignee")) {
            // For assignee, focus on workload and efficiency
            
            // Item distribution by state
            Map<String, Double> stateDistribution = filteredItems.stream()
                .collect(Collectors.groupingBy(
                    item -> item.getWorkItem().getState().name(),
                    Collectors.counting()))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> (double) entry.getValue()));
            
            statistics.add(StatisticValue.createDistribution(
                StatisticType.ITEMS_BY_STATE, 
                stateDistribution, 
                "Items by state for " + categoryDescription
            ));
            
            // Work in progress
            long wipCount = filteredItems.stream()
                .filter(item -> item.getWorkItem().getState().name().equals("IN_PROGRESS"))
                .count();
            
            statistics.add(StatisticValue.createCount(
                StatisticType.WORK_IN_PROGRESS, 
                (int) wipCount, 
                "Work in progress for " + categoryDescription
            ));
            
            // Overdue items
            Instant now = Instant.now();
            
            long overdueCount = filteredItems.stream()
                .filter(item -> !item.getWorkItem().getState().name().equals("DONE") && 
                               !item.getWorkItem().getState().name().equals("COMPLETED"))
                .filter(item -> item.getDueDate() != null && item.getDueDate().isBefore(now))
                .count();
            
            statistics.add(StatisticValue.createCount(
                StatisticType.OVERDUE_ITEMS, 
                (int) overdueCount, 
                "Overdue items for " + categoryDescription
            ));
            
            // Completion rate
            long completedCount = filteredItems.stream()
                .filter(item -> item.getWorkItem().getState().name().equals("DONE") || 
                              item.getWorkItem().getState().name().equals("COMPLETED"))
                .count();
            
            if (!filteredItems.isEmpty()) {
                double completionRate = (double) completedCount / filteredItems.size() * 100.0;
                
                statistics.add(StatisticValue.createPercentage(
                    StatisticType.COMPLETION_RATE, 
                    completionRate, 
                    "Completion rate for " + categoryDescription
                ));
            }
        } else {
            // For other categories, provide a standard set of statistics
            
            // Item distribution by state
            Map<String, Double> stateDistribution = filteredItems.stream()
                .collect(Collectors.groupingBy(
                    item -> item.getWorkItem().getState().name(),
                    Collectors.counting()))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> (double) entry.getValue()));
            
            statistics.add(StatisticValue.createDistribution(
                StatisticType.ITEMS_BY_STATE, 
                stateDistribution, 
                "Items by state for " + categoryDescription
            ));
            
            // Item distribution by assignee
            Map<String, Double> assigneeDistribution = filteredItems.stream()
                .filter(item -> item.getWorkItem().getAssignee() != null && !item.getWorkItem().getAssignee().isEmpty())
                .collect(Collectors.groupingBy(
                    item -> item.getWorkItem().getAssignee(),
                    Collectors.counting()))
                .entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> (double) entry.getValue()));
            
            // Add "Unassigned" count
            long unassignedCount = filteredItems.stream()
                .filter(item -> item.getWorkItem().getAssignee() == null || item.getWorkItem().getAssignee().isEmpty())
                .count();
            
            if (unassignedCount > 0) {
                assigneeDistribution.put("Unassigned", (double) unassignedCount);
            }
            
            if (!assigneeDistribution.isEmpty()) {
                statistics.add(StatisticValue.createDistribution(
                    StatisticType.ITEMS_BY_ASSIGNEE, 
                    assigneeDistribution, 
                    "Items by assignee for " + categoryDescription
                ));
            }
            
            // Completion rate
            long completedCount = filteredItems.stream()
                .filter(item -> item.getWorkItem().getState().name().equals("DONE") || 
                              item.getWorkItem().getState().name().equals("COMPLETED"))
                .count();
            
            if (!filteredItems.isEmpty()) {
                double completionRate = (double) completedCount / filteredItems.size() * 100.0;
                
                statistics.add(StatisticValue.createPercentage(
                    StatisticType.COMPLETION_RATE, 
                    completionRate, 
                    "Completion rate for " + categoryDescription
                ));
            }
        }
        
        return statistics;
    }
    
    /**
     * Ensures the cached statistics are fresh (within TTL).
     */
    private void ensureStatisticsAreFresh() {
        Instant now = Instant.now();
        if (Duration.between(lastRefresh, now).compareTo(CACHE_TTL) > 0) {
            refreshStatistics();
        }
    }
    
    /**
     * Refreshes all statistics.
     */
    public void refreshStatistics() {
        // Clear the cache
        cachedStatistics.clear();
        
        // Compute all statistics
        for (StatisticType type : StatisticType.values()) {
            StatisticValue stat = computeStatistic(type);
            if (stat != null) {
                cachedStatistics.add(stat);
            }
        }
        
        // Update the refresh timestamp
        lastRefresh = Instant.now();
    }
    
    /**
     * Computes a specific statistic.
     * 
     * @param type the statistic type
     * @return the computed statistic, or null if it cannot be computed
     */
    private StatisticValue computeStatistic(StatisticType type) {
        try {
            switch (type) {
                case TOTAL_ITEMS:
                    return computeTotalItems();
                    
                case ITEMS_BY_TYPE:
                    return computeItemsByType();
                    
                case ITEMS_BY_STATE:
                    return computeItemsByState();
                    
                case ITEMS_BY_PRIORITY:
                    return computeItemsByPriority();
                    
                case ITEMS_BY_ASSIGNEE:
                    return computeItemsByAssignee();
                    
                case COMPLETION_RATE:
                    return computeCompletionRate();
                    
                case ITEMS_COMPLETED:
                    return computeItemsCompleted(30); // Last 30 days by default
                    
                case ITEMS_CREATED:
                    return computeItemsCreated(30); // Last 30 days by default
                    
                case OVERDUE_ITEMS:
                    return computeOverdueItems();
                    
                case WORK_IN_PROGRESS:
                    return computeWorkInProgress();
                    
                case THROUGHPUT:
                    return computeThroughput(7); // Last 7 days by default
                    
                case AVG_COMPLETION_TIME:
                    return computeAvgCompletionTime();
                    
                case LEAD_TIME:
                    return computeLeadTime();
                    
                case CYCLE_TIME:
                    return computeCycleTime();
                    
                case BURNDOWN_RATE:
                    return computeBurndownRate();
                    
                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Error computing statistic " + type + ": " + e.getMessage());
            return null;
        }
    }
    
    /*
     * Individual statistic computation methods
     */
    
    private StatisticValue computeTotalItems() {
        Object itemServiceObj = ServiceManager.getInstance().getItemService();
        if (itemServiceObj == null) {
            return StatisticValue.createCount(StatisticType.TOTAL_ITEMS, 0, "Total work items");
        }
        
        MockItemService itemService = (MockItemService) itemServiceObj;
        MockItemServiceAdapter adapter = new MockItemServiceAdapter(itemService);
        List<StatisticItemAdapter> allItems = adapter.getAllItems();
        return StatisticValue.createCount(StatisticType.TOTAL_ITEMS, allItems.size(), "Total work items");
    }
    
    private StatisticValue computeItemsByType() {
        Object itemServiceObj = ServiceManager.getInstance().getItemService();
        if (itemServiceObj == null) {
            return StatisticValue.createDistribution(StatisticType.ITEMS_BY_TYPE, 
                new HashMap<>(), "Work items by type");
        }
        
        MockItemService itemService = (MockItemService) itemServiceObj;
        MockItemServiceAdapter adapter = new MockItemServiceAdapter(itemService);
        List<StatisticItemAdapter> allItems = adapter.getAllItems();
        
        Map<String, Double> typeDistribution = allItems.stream()
            .collect(Collectors.groupingBy(
                item -> item.getWorkItem().getType().name(),
                Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (double) entry.getValue()));
        
        return StatisticValue.createDistribution(
            StatisticType.ITEMS_BY_TYPE, 
            typeDistribution, 
            "Work items by type"
        );
    }
    
    private StatisticValue computeItemsByState() {
        Object itemServiceObj = ServiceManager.getInstance().getItemService();
        if (itemServiceObj == null) {
            return StatisticValue.createDistribution(StatisticType.ITEMS_BY_STATE, 
                new HashMap<>(), "Work items by state");
        }
        
        MockItemService itemService = (MockItemService) itemServiceObj;
        MockItemServiceAdapter adapter = new MockItemServiceAdapter(itemService);
        List<StatisticItemAdapter> allItems = adapter.getAllItems();
        
        Map<String, Double> stateDistribution = allItems.stream()
            .collect(Collectors.groupingBy(
                item -> item.getWorkItem().getState().name(),
                Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (double) entry.getValue()));
        
        return StatisticValue.createDistribution(
            StatisticType.ITEMS_BY_STATE, 
            stateDistribution, 
            "Work items by state"
        );
    }
    
    private StatisticValue computeItemsByPriority() {
        Object itemServiceObj = ServiceManager.getInstance().getItemService();
        if (itemServiceObj == null) {
            return StatisticValue.createDistribution(StatisticType.ITEMS_BY_PRIORITY, 
                new HashMap<>(), "Work items by priority");
        }
        
        MockItemService itemService = (MockItemService) itemServiceObj;
        MockItemServiceAdapter adapter = new MockItemServiceAdapter(itemService);
        List<StatisticItemAdapter> allItems = adapter.getAllItems();
        
        Map<String, Double> priorityDistribution = allItems.stream()
            .collect(Collectors.groupingBy(
                item -> item.getWorkItem().getPriority().name(),
                Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (double) entry.getValue()));
        
        return StatisticValue.createDistribution(
            StatisticType.ITEMS_BY_PRIORITY, 
            priorityDistribution, 
            "Work items by priority"
        );
    }
    
    private StatisticValue computeItemsByAssignee() {
        Object itemServiceObj = ServiceManager.getInstance().getItemService();
        if (itemServiceObj == null) {
            return StatisticValue.createDistribution(StatisticType.ITEMS_BY_ASSIGNEE, 
                new HashMap<>(), "Work items by assignee");
        }
        
        MockItemService itemService = (MockItemService) itemServiceObj;
        MockItemServiceAdapter adapter = new MockItemServiceAdapter(itemService);
        List<StatisticItemAdapter> allItems = adapter.getAllItems();
        
        Map<String, Double> assigneeDistribution = allItems.stream()
            .filter(item -> item.getWorkItem().getAssignee() != null && !item.getWorkItem().getAssignee().isEmpty())
            .collect(Collectors.groupingBy(
                item -> item.getWorkItem().getAssignee(),
                Collectors.counting()))
            .entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> (double) entry.getValue()));
        
        // Add "Unassigned" count
        long unassignedCount = allItems.stream()
            .filter(item -> item.getWorkItem().getAssignee() == null || item.getWorkItem().getAssignee().isEmpty())
            .count();
        
        if (unassignedCount > 0) {
            assigneeDistribution.put("Unassigned", (double) unassignedCount);
        }
        
        return StatisticValue.createDistribution(
            StatisticType.ITEMS_BY_ASSIGNEE, 
            assigneeDistribution, 
            "Work items by assignee"
        );
    }
    
    private StatisticValue computeCompletionRate() {
        Object itemServiceObj = ServiceManager.getInstance().getItemService();
        if (itemServiceObj == null) {
            return StatisticValue.createPercentage(StatisticType.COMPLETION_RATE, 0, "Completion rate");
        }
        
        MockItemService itemService = (MockItemService) itemServiceObj;
        MockItemServiceAdapter adapter = new MockItemServiceAdapter(itemService);
        List<StatisticItemAdapter> allItems = adapter.getAllItems();
        
        if (allItems.isEmpty()) {
            return StatisticValue.createPercentage(StatisticType.COMPLETION_RATE, 0, "Completion rate");
        }
        
        long completedCount = allItems.stream()
            .filter(item -> item.getWorkItem().getState().name().equals("DONE") || 
                           item.getWorkItem().getState().name().equals("COMPLETED"))
            .count();
        
        double completionRate = (double) completedCount / allItems.size() * 100.0;
        
        return StatisticValue.createPercentage(
            StatisticType.COMPLETION_RATE, 
            completionRate, 
            "Completion rate"
        );
    }
    
    private StatisticValue computeItemsCompleted(int days) {
        Object itemServiceObj = ServiceManager.getInstance().getItemService();
        if (itemServiceObj == null) {
            return StatisticValue.createCount(StatisticType.ITEMS_COMPLETED, 0, 
                "Items completed in the last " + days + " days");
        }
        
        MockItemService itemService = (MockItemService) itemServiceObj;
        MockItemServiceAdapter adapter = new MockItemServiceAdapter(itemService);
        List<StatisticItemAdapter> allItems = adapter.getAllItems();
        
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        
        long completedCount = allItems.stream()
            .filter(item -> item.getWorkItem().getState().name().equals("DONE") || 
                          item.getWorkItem().getState().name().equals("COMPLETED"))
            .filter(item -> item.getUpdatedAt() != null && item.getUpdatedAt().isAfter(cutoff))
            .count();
        
        return StatisticValue.createCount(
            StatisticType.ITEMS_COMPLETED, 
            (int) completedCount, 
            "Items completed in the last " + days + " days"
        );
    }
    
    private StatisticValue computeItemsCreated(int days) {
        Object itemServiceObj = ServiceManager.getInstance().getItemService();
        if (itemServiceObj == null) {
            return StatisticValue.createCount(StatisticType.ITEMS_CREATED, 0, 
                "Items created in the last " + days + " days");
        }
        
        MockItemService itemService = (MockItemService) itemServiceObj;
        MockItemServiceAdapter adapter = new MockItemServiceAdapter(itemService);
        List<StatisticItemAdapter> allItems = adapter.getAllItems();
        
        Instant cutoff = Instant.now().minus(days, ChronoUnit.DAYS);
        
        long createdCount = allItems.stream()
            .filter(item -> item.getCreatedAt() != null && item.getCreatedAt().isAfter(cutoff))
            .count();
        
        return StatisticValue.createCount(
            StatisticType.ITEMS_CREATED, 
            (int) createdCount, 
            "Items created in the last " + days + " days"
        );
    }
    
    private StatisticValue computeOverdueItems() {
        Object itemServiceObj = ServiceManager.getInstance().getItemService();
        if (itemServiceObj == null) {
            return StatisticValue.createCount(StatisticType.OVERDUE_ITEMS, 0, "Overdue items");
        }
        
        MockItemService itemService = (MockItemService) itemServiceObj;
        MockItemServiceAdapter adapter = new MockItemServiceAdapter(itemService);
        List<StatisticItemAdapter> allItems = adapter.getAllItems();
        
        Instant now = Instant.now();
        
        long overdueCount = allItems.stream()
            .filter(item -> !item.getWorkItem().getState().name().equals("DONE") && 
                           !item.getWorkItem().getState().name().equals("COMPLETED"))
            .filter(item -> item.getDueDate() != null && item.getDueDate().isBefore(now))
            .count();
        
        return StatisticValue.createCount(
            StatisticType.OVERDUE_ITEMS, 
            (int) overdueCount, 
            "Overdue items"
        );
    }
    
    private StatisticValue computeWorkInProgress() {
        Object itemServiceObj = ServiceManager.getInstance().getItemService();
        if (itemServiceObj == null) {
            return StatisticValue.createCount(StatisticType.WORK_IN_PROGRESS, 0, "Work in progress");
        }
        
        MockItemService itemService = (MockItemService) itemServiceObj;
        MockItemServiceAdapter adapter = new MockItemServiceAdapter(itemService);
        List<StatisticItemAdapter> allItems = adapter.getAllItems();
        
        long wipCount = allItems.stream()
            .filter(item -> item.getWorkItem().getState().name().equals("IN_PROGRESS"))
            .count();
        
        return StatisticValue.createCount(
            StatisticType.WORK_IN_PROGRESS, 
            (int) wipCount, 
            "Work in progress"
        );
    }
    
    private StatisticValue computeThroughput(int days) {
        StatisticValue itemsCompleted = computeItemsCompleted(days);
        if (itemsCompleted.getValue() == 0) {
            return StatisticValue.createTime(StatisticType.THROUGHPUT, 0, "items/day", 
                "Throughput (items/day, last " + days + " days)");
        }
        
        double throughput = itemsCompleted.getValue() / days;
        
        return StatisticValue.createTime(
            StatisticType.THROUGHPUT, 
            throughput, 
            "items/day", 
            "Throughput (items/day, last " + days + " days)"
        );
    }
    
    private StatisticValue computeAvgCompletionTime() {
        Object itemServiceObj = ServiceManager.getInstance().getItemService();
        if (itemServiceObj == null) {
            return StatisticValue.createTime(StatisticType.AVG_COMPLETION_TIME, 0, "days", 
                "Average completion time");
        }
        
        MockItemService itemService = (MockItemService) itemServiceObj;
        MockItemServiceAdapter adapter = new MockItemServiceAdapter(itemService);
        List<StatisticItemAdapter> allItems = adapter.getAllItems();
        
        List<StatisticItemAdapter> completedItems = allItems.stream()
            .filter(item -> item.getWorkItem().getState().name().equals("DONE") || 
                           item.getWorkItem().getState().name().equals("COMPLETED"))
            .filter(item -> item.getCreatedAt() != null && item.getUpdatedAt() != null)
            .collect(Collectors.toList());
        
        if (completedItems.isEmpty()) {
            return StatisticValue.createTime(StatisticType.AVG_COMPLETION_TIME, 0, "days", 
                "Average completion time");
        }
        
        double avgDays = completedItems.stream()
            .mapToDouble(item -> Duration.between(item.getCreatedAt(), item.getUpdatedAt()).toDays())
            .average()
            .orElse(0);
        
        return StatisticValue.createTime(
            StatisticType.AVG_COMPLETION_TIME, 
            avgDays, 
            "days", 
            "Average completion time"
        );
    }
    
    private StatisticValue computeLeadTime() {
        // For simplicity, use the same computation as average completion time for now
        StatisticValue avgCompletionTime = computeAvgCompletionTime();
        return new StatisticValue(
            StatisticType.LEAD_TIME,
            avgCompletionTime.getValue(),
            avgCompletionTime.getUnit(),
            "Lead time (creation to completion)"
        );
    }
    
    private StatisticValue computeCycleTime() {
        Object itemServiceObj = ServiceManager.getInstance().getItemService();
        if (itemServiceObj == null) {
            return StatisticValue.createTime(StatisticType.CYCLE_TIME, 0, "days", 
                "Cycle time (in progress to completion)");
        }
        
        MockItemService itemService = (MockItemService) itemServiceObj;
        MockItemServiceAdapter adapter = new MockItemServiceAdapter(itemService);
        List<StatisticItemAdapter> allItems = adapter.getAllItems();
        
        // For a more accurate cycle time, we'd need to track when items first moved to IN_PROGRESS
        // For now, we'll use a simplified approximation
        List<StatisticItemAdapter> completedItems = allItems.stream()
            .filter(item -> item.getWorkItem().getState().name().equals("DONE") || 
                           item.getWorkItem().getState().name().equals("COMPLETED"))
            .filter(item -> item.getCreatedAt() != null && item.getUpdatedAt() != null)
            .collect(Collectors.toList());
        
        if (completedItems.isEmpty()) {
            return StatisticValue.createTime(StatisticType.CYCLE_TIME, 0, "days", 
                "Cycle time (in progress to completion)");
        }
        
        // Assume cycle time is about 70% of the total time (lead time)
        double avgDays = completedItems.stream()
            .mapToDouble(item -> Duration.between(item.getCreatedAt(), item.getUpdatedAt()).toDays() * 0.7)
            .average()
            .orElse(0);
        
        return StatisticValue.createTime(
            StatisticType.CYCLE_TIME, 
            avgDays, 
            "days", 
            "Cycle time (in progress to completion)"
        );
    }
    
    private StatisticValue computeBurndownRate() {
        // Burndown rate is similar to throughput
        StatisticValue throughput = computeThroughput(14); // Use 2 weeks for burndown
        return new StatisticValue(
            StatisticType.BURNDOWN_RATE,
            throughput.getValue(),
            throughput.getUnit(),
            "Burndown rate (items completed per day, last 14 days)"
        );
    }
}