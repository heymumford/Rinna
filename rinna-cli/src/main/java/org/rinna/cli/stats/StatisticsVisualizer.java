/*
 * Statistics visualizer for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.stats;

import org.rinna.cli.messaging.AnsiFormatter;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utility for visualizing statistics in the terminal.
 */
public class StatisticsVisualizer {
    
    private static final int DEFAULT_BAR_WIDTH = 50;
    private static final int DEFAULT_CHART_HEIGHT = 10;
    private static final String BAR_CHAR = "█";
    private static final String HALF_BAR_CHAR = "▌";
    
    /**
     * Creates a simple horizontal bar chart for a statistic with a breakdown.
     * 
     * @param statistic the statistic to visualize
     * @param maxBars maximum number of bars to show (for top N items)
     * @return the visualization as a string
     */
    public static String createBarChart(StatisticValue statistic, int maxBars) {
        Map<String, Double> breakdown = statistic.getBreakdown();
        if (breakdown.isEmpty()) {
            return "No data available for visualization.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(statistic.getDescription()).append("\n\n");
        
        // Sort entries by value in descending order and limit to maxBars
        List<Map.Entry<String, Double>> sortedEntries = breakdown.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()))
            .limit(maxBars > 0 ? maxBars : breakdown.size())
            .collect(Collectors.toList());
        
        // Find the maximum value for scaling
        double maxValue = sortedEntries.stream()
            .mapToDouble(Map.Entry::getValue)
            .max()
            .orElse(1.0);
        
        // Find the maximum label length for alignment
        int maxLabelLength = sortedEntries.stream()
            .mapToInt(e -> e.getKey().length())
            .max()
            .orElse(10);
        
        // Create the bars
        for (Map.Entry<String, Double> entry : sortedEntries) {
            String label = entry.getKey();
            double value = entry.getValue();
            int barLength = (int) Math.round((value / maxValue) * DEFAULT_BAR_WIDTH);
            
            sb.append(String.format("%-" + maxLabelLength + "s: ", label));
            sb.append(AnsiFormatter.BRIGHT_FG_CYAN);
            
            for (int i = 0; i < barLength; i++) {
                sb.append(BAR_CHAR);
            }
            
            sb.append(AnsiFormatter.RESET);
            sb.append(String.format(" %.2f %s\n", value, statistic.getUnit()));
        }
        
        return sb.toString();
    }
    
    /**
     * Creates a table visualization for multiple statistics.
     * 
     * @param statistics the list of statistics to display
     * @return the visualization as a string
     */
    public static String createTable(List<StatisticValue> statistics) {
        if (statistics.isEmpty()) {
            return "No statistics available.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("Statistics Summary\n");
        sb.append("==================\n\n");
        
        // Find the maximum description length for alignment
        int maxDescLength = statistics.stream()
            .mapToInt(s -> s.getDescription().length())
            .max()
            .orElse(30);
        
        // Create the table
        for (StatisticValue stat : statistics) {
            sb.append(String.format("%-" + maxDescLength + "s: ", stat.getDescription()));
            
            // Format based on statistic type
            if (stat.getType() == StatisticType.COMPLETION_RATE) {
                double percentage = stat.getValue();
                sb.append(formatColoredPercentage(percentage));
            } else {
                sb.append(String.format("%8.2f %s", stat.getValue(), stat.getUnit()));
            }
            
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Creates a dashboard view with key metrics and visualizations.
     * 
     * @param statistics the list of statistics to include
     * @return the dashboard as a string
     */
    public static String createDashboard(List<StatisticValue> statistics) {
        if (statistics.isEmpty()) {
            return "No statistics available for the dashboard.";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(AnsiFormatter.createBanner("RINNA PROJECT DASHBOARD"));
        sb.append("\nDate: " + java.time.LocalDate.now().toString() + "\n\n");
        
        // Key metrics section
        sb.append(AnsiFormatter.BOLD + "KEY METRICS" + AnsiFormatter.RESET + "\n");
        sb.append("-----------\n");
        
        // Filter key metrics
        List<StatisticValue> keyMetrics = statistics.stream()
            .filter(s -> s.getType() == StatisticType.TOTAL_ITEMS || 
                         s.getType() == StatisticType.COMPLETION_RATE ||
                         s.getType() == StatisticType.WORK_IN_PROGRESS ||
                         s.getType() == StatisticType.OVERDUE_ITEMS ||
                         s.getType() == StatisticType.THROUGHPUT)
            .collect(Collectors.toList());
        
        // Create a table for key metrics
        sb.append(createTable(keyMetrics));
        sb.append("\n");
        
        // Find and add bar charts for distributions
        for (StatisticType type : new StatisticType[] {
            StatisticType.ITEMS_BY_STATE, 
            StatisticType.ITEMS_BY_TYPE,
            StatisticType.ITEMS_BY_PRIORITY
        }) {
            StatisticValue stat = statistics.stream()
                .filter(s -> s.getType() == type)
                .findFirst()
                .orElse(null);
                
            if (stat != null && !stat.getBreakdown().isEmpty()) {
                sb.append(AnsiFormatter.BOLD + type.name() + AnsiFormatter.RESET + "\n");
                sb.append("-".repeat(type.name().length()) + "\n");
                sb.append(createBarChart(stat, 5));
                sb.append("\n");
            }
        }
        
        // Add a progress meter for completion rate
        StatisticValue completionRate = statistics.stream()
            .filter(s -> s.getType() == StatisticType.COMPLETION_RATE)
            .findFirst()
            .orElse(null);
            
        if (completionRate != null) {
            sb.append(AnsiFormatter.BOLD + "PROJECT COMPLETION" + AnsiFormatter.RESET + "\n");
            sb.append("-----------------\n");
            sb.append(createProgressMeter(completionRate.getValue(), 100));
            sb.append("\n\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Creates a progress meter visualization for a percentage value.
     * 
     * @param value the current value
     * @param max the maximum value
     * @return the visualization as a string
     */
    public static String createProgressMeter(double value, double max) {
        StringBuilder sb = new StringBuilder();
        
        int percentage = (int) Math.round((value / max) * 100);
        int barWidth = (int) Math.round((value / max) * DEFAULT_BAR_WIDTH);
        
        // Title and percentage
        sb.append(String.format("Progress: %d%% Complete\n", percentage));
        
        // Draw the progress bar
        sb.append("[");
        
        // Determine color based on percentage
        String barColor;
        if (percentage < 33) {
            barColor = AnsiFormatter.BRIGHT_FG_RED;
        } else if (percentage < 67) {
            barColor = AnsiFormatter.BRIGHT_FG_YELLOW;
        } else {
            barColor = AnsiFormatter.BRIGHT_FG_GREEN;
        }
        
        sb.append(barColor);
        
        for (int i = 0; i < barWidth; i++) {
            sb.append(BAR_CHAR);
        }
        
        sb.append(AnsiFormatter.RESET);
        
        // Empty space
        for (int i = barWidth; i < DEFAULT_BAR_WIDTH; i++) {
            sb.append(" ");
        }
        
        sb.append("]");
        
        return sb.toString();
    }
    
    /**
     * Creates a sparkline visualization for a series of numeric values.
     * 
     * @param values the series of values to visualize
     * @param title the title for the sparkline
     * @return the visualization as a string
     */
    public static String createSparkline(List<Double> values, String title) {
        if (values == null || values.isEmpty()) {
            return title + ": No data";
        }
        
        // Unicode block elements for sparkline
        final char[] sparkChars = new char[] { ' ', '▁', '▂', '▃', '▄', '▅', '▆', '▇', '█' };
        
        double min = values.stream().min(Double::compare).orElse(0.0);
        double max = values.stream().max(Double::compare).orElse(0.0);
        double range = max - min;
        
        StringBuilder sb = new StringBuilder();
        sb.append(title).append(": ");
        
        if (range == 0) {
            // All values are the same, use middle character
            for (int i = 0; i < values.size(); i++) {
                sb.append(sparkChars[4]);
            }
        } else {
            // Scale values to spark character range
            for (double value : values) {
                int index = (int) Math.round(((value - min) / range) * (sparkChars.length - 1));
                sb.append(sparkChars[index]);
            }
        }
        
        // Add min and max values
        sb.append(String.format(" (min: %.2f, max: %.2f)", min, max));
        
        return sb.toString();
    }
    
    /**
     * Formats a percentage value with color-coding based on the value.
     * 
     * @param percentage the percentage value
     * @return the formatted string with color
     */
    private static String formatColoredPercentage(double percentage) {
        String color;
        if (percentage < 33) {
            color = AnsiFormatter.BRIGHT_FG_RED;
        } else if (percentage < 67) {
            color = AnsiFormatter.BRIGHT_FG_YELLOW;
        } else {
            color = AnsiFormatter.BRIGHT_FG_GREEN;
        }
        
        return String.format("%s%6.2f%%%s", color, percentage, AnsiFormatter.RESET);
    }
}