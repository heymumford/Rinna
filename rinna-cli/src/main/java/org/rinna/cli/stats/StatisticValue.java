/*
 * Statistic value class for Rinna CLI
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.stats;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents a specific statistic value or group of related values.
 */
public class StatisticValue {
    private final StatisticType type;
    private final double value;
    private final Map<String, Double> breakdown;
    private final String unit;
    private final String description;
    private final Instant timestamp;
    
    private static final DateTimeFormatter FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * Creates a new statistic value with a simple numeric value.
     * 
     * @param type the statistic type
     * @param value the value of the statistic
     * @param unit the unit of measurement
     * @param description a description of the statistic
     */
    public StatisticValue(StatisticType type, double value, String unit, String description) {
        this.type = type;
        this.value = value;
        this.unit = unit;
        this.description = description;
        this.breakdown = new HashMap<>();
        this.timestamp = Instant.now();
    }
    
    /**
     * Creates a new statistic value with a breakdown of related values.
     * 
     * @param type the statistic type
     * @param value the overall value of the statistic
     * @param breakdown a map of subcategories to their values
     * @param unit the unit of measurement
     * @param description a description of the statistic
     */
    public StatisticValue(StatisticType type, double value, Map<String, Double> breakdown, 
                         String unit, String description) {
        this.type = type;
        this.value = value;
        this.breakdown = new HashMap<>(breakdown);
        this.unit = unit;
        this.description = description;
        this.timestamp = Instant.now();
    }
    
    /**
     * Creates a count statistic.
     * 
     * @param type the statistic type
     * @param count the count value
     * @param description a description of what's being counted
     * @return a new statistic value
     */
    public static StatisticValue createCount(StatisticType type, int count, String description) {
        return new StatisticValue(type, count, "items", description);
    }
    
    /**
     * Creates a percentage statistic.
     * 
     * @param type the statistic type
     * @param percentage the percentage value (0-100)
     * @param description a description of the percentage
     * @return a new statistic value
     */
    public static StatisticValue createPercentage(StatisticType type, double percentage, String description) {
        return new StatisticValue(type, percentage, "%", description);
    }
    
    /**
     * Creates a time-based statistic.
     * 
     * @param type the statistic type
     * @param timeValue the time value
     * @param unit the time unit (e.g., "days", "hours")
     * @param description a description of the time measurement
     * @return a new statistic value
     */
    public static StatisticValue createTime(StatisticType type, double timeValue, String unit, String description) {
        return new StatisticValue(type, timeValue, unit, description);
    }
    
    /**
     * Creates a distribution statistic with breakdown by category.
     * 
     * @param type the statistic type
     * @param distribution the distribution map (category to count)
     * @param description a description of the distribution
     * @return a new statistic value
     */
    public static StatisticValue createDistribution(StatisticType type, Map<String, Double> distribution, String description) {
        double total = distribution.values().stream().mapToDouble(Double::doubleValue).sum();
        return new StatisticValue(type, total, distribution, "items", description);
    }
    
    public StatisticType getType() {
        return type;
    }
    
    public double getValue() {
        return value;
    }
    
    public Map<String, Double> getBreakdown() {
        return new HashMap<>(breakdown);
    }
    
    public String getUnit() {
        return unit;
    }
    
    public String getDescription() {
        return description;
    }
    
    public Instant getTimestamp() {
        return timestamp;
    }
    
    public String getFormattedTimestamp() {
        LocalDateTime dateTime = LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault());
        return dateTime.format(FORMATTER);
    }
    
    /**
     * Formats the statistic value as a string.
     * 
     * @return the formatted statistic value
     */
    public String format() {
        StringBuilder sb = new StringBuilder();
        
        sb.append(description)
          .append(": ")
          .append(String.format("%.2f", value))
          .append(" ")
          .append(unit);
        
        if (!breakdown.isEmpty()) {
            sb.append(" (");
            boolean first = true;
            for (Map.Entry<String, Double> entry : breakdown.entrySet()) {
                if (!first) {
                    sb.append(", ");
                }
                sb.append(entry.getKey())
                  .append(": ")
                  .append(String.format("%.2f", entry.getValue()));
                first = false;
            }
            sb.append(")");
        }
        
        return sb.toString();
    }
    
    /**
     * Formats the statistic breakdown as a multi-line string with each category on a separate line.
     * 
     * @return the formatted breakdown
     */
    public String formatBreakdown() {
        if (breakdown.isEmpty()) {
            return "No breakdown available";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(description).append(":\n");
        
        breakdown.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue(), e1.getValue())) // Sort by value descending
            .forEach(entry -> {
                sb.append(String.format("  %-20s: %6.2f %s\n", 
                    entry.getKey(), entry.getValue(), unit));
            });
        
        return sb.toString();
    }
    
    @Override
    public String toString() {
        return format();
    }
}