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

import org.rinna.cli.stats.StatisticType;
import org.rinna.cli.stats.StatisticValue;
import org.rinna.cli.stats.StatisticsService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.MockStatisticsService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Bridge between the PUI components and CLI statistics services.
 * This class provides simplified access to CLI statistics for PUI components.
 */
public class StatisticsBridge {
    
    private static StatisticsBridge instance;
    
    private final StatisticsService statisticsService;
    
    /**
     * Private constructor to enforce singleton pattern.
     */
    private StatisticsBridge() {
        this.statisticsService = StatisticsService.getInstance();
    }
    
    /**
     * Gets the singleton instance of StatisticsBridge.
     * 
     * @return the singleton instance
     */
    public static synchronized StatisticsBridge getInstance() {
        if (instance == null) {
            instance = new StatisticsBridge();
        }
        return instance;
    }
    
    /**
     * Gets all available statistics.
     * 
     * @return a list of statistics
     */
    public List<StatisticValue> getAllStatistics() {
        try {
            return statisticsService.getAllStatistics();
        } catch (Exception e) {
            System.err.println("Error retrieving statistics: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Gets summary statistics (most important overview stats).
     * 
     * @return a list of summary statistics
     */
    public List<StatisticValue> getSummaryStatistics() {
        try {
            return statisticsService.getSummaryStatistics();
        } catch (Exception e) {
            System.err.println("Error retrieving summary statistics: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Gets a specific statistic by type.
     * 
     * @param type the statistic type
     * @return the statistic value, or null if not available
     */
    public StatisticValue getStatistic(StatisticType type) {
        try {
            return statisticsService.getStatistic(type);
        } catch (Exception e) {
            System.err.println("Error retrieving statistic: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Gets statistics for a specific project.
     * 
     * @param projectId the project ID
     * @return a list of statistics for the project
     */
    public List<StatisticValue> getProjectStatistics(String projectId) {
        try {
            return statisticsService.getProjectStatistics(projectId);
        } catch (Exception e) {
            System.err.println("Error retrieving project statistics: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Gets statistics by category.
     * 
     * @param category the category (e.g., "project", "assignee", "type", "priority", "state")
     * @param value the category value
     * @return a list of statistics for the category
     */
    public List<StatisticValue> getStatisticsByCategory(String category, String value) {
        try {
            return statisticsService.getStatisticsByCategory(category, value);
        } catch (Exception e) {
            System.err.println("Error retrieving statistics by category: " + e.getMessage());
            return Collections.emptyList();
        }
    }
    
    /**
     * Refreshes all statistics.
     */
    public void refreshStatistics() {
        try {
            statisticsService.refreshStatistics();
        } catch (Exception e) {
            System.err.println("Error refreshing statistics: " + e.getMessage());
        }
    }
    
    /**
     * Extracts chart data from a statistic with a breakdown.
     * 
     * @param statistic the statistic with breakdown data
     * @return a map of categories to values for charting
     */
    public Map<String, Double> getChartData(StatisticValue statistic) {
        if (statistic == null) {
            return Collections.emptyMap();
        }
        
        Map<String, Double> breakdown = statistic.getBreakdown();
        if (breakdown == null || breakdown.isEmpty()) {
            return Collections.emptyMap();
        }
        
        return new HashMap<>(breakdown);
    }
    
    /**
     * Gets all projects with statistics.
     * 
     * @return a list of project IDs
     */
    public List<String> getAllProjects() {
        // This is a placeholder implementation
        // In a real implementation, it would query the project service
        List<String> projects = new ArrayList<>();
        projects.add("RINNA-1");
        projects.add("RINNA-2");
        projects.add("API-1");
        return projects;
    }
    
    /**
     * Gets all assignees with statistics.
     * 
     * @return a list of assignee names
     */
    public List<String> getAllAssignees() {
        // This is a placeholder implementation
        // In a real implementation, it would extract unique assignees from work items
        List<String> assignees = new ArrayList<>();
        assignees.add("John Doe");
        assignees.add("Jane Smith");
        assignees.add("Eric Mumford");
        assignees.add("Alex Developer");
        return assignees;
    }
}