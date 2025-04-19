# Project Dashboard UI Guide

This document provides information about the Pragmatic User Interface (PUI) components developed for project dashboards in the Rinna system, particularly focusing on visualizing statistics and metrics in an interactive terminal interface.

## Overview

The Project Dashboard UI provides real-time visualization of project statistics and metrics, including:

1. Key performance indicators for projects
2. Distribution of work items by state, type, priority, and assignee
3. Velocity metrics like throughput, lead time, and cycle time
4. Completion and overdue rates
5. Project details and assignments

This implementation follows Rinna's PUI (Pragmatic User Interface) design principles, emphasizing efficiency and developer-centric operations.

## Components

### BarChart

The `BarChart` component visualizes data as horizontal bars, making it ideal for showing distributions like work items by state, type, or assignee. Key features include:

- Customizable sorting by value or label
- Configurable maximum number of bars
- Value and percentage display options
- Customizable colors
- Support for titles and axis labels

```java
// Create a bar chart
BarChart chart = new BarChart("items-by-state", width, height);
chart.setTitle("Work Items by State")
     .setData(stateDistribution)
     .setMaxBars(5)
     .setSortByValue(true)
     .setSortOrder(BarChart.SortOrder.DESCENDING)
     .setShowValues(true)
     .setShowPercentages(true);
```

### ProgressMeter

The `ProgressMeter` component displays progress toward a goal or completion with a visual bar that can change color based on the value. It's perfect for showing completion rates, burndown progress, or other percentage-based metrics.

```java
// Create a progress meter
ProgressMeter meter = new ProgressMeter("completion-meter", width, height);
meter.setValue(75.5)        // Current value (75.5%)
     .setMaxValue(100)      // Maximum value
     .setTitle("Completion Rate")
     .setUnit("%")          // Unit for display
     .setShowPercentage(true)
     .setUseColors(true)    // Color coding based on value
     .setThresholds(33, 67); // Color transition thresholds
```

### StatisticsBridge

The `StatisticsBridge` provides a simplified interface between the PUI components and the statistics service, enabling access to:

- Summary statistics for quick overviews
- Detailed statistics for specific projects or categories
- Chart data extraction for visualizations
- Project and assignee listings for filtering

```java
// Get the statistics bridge singleton
StatisticsBridge bridge = StatisticsBridge.getInstance();

// Get statistics for display
List<StatisticValue> allStats = bridge.getAllStatistics();
List<StatisticValue> summaryStats = bridge.getSummaryStatistics();
List<StatisticValue> projectStats = bridge.getProjectStatistics("PROJECT-1");

// Get specific statistics
StatisticValue completionRate = bridge.getStatistic(StatisticType.COMPLETION_RATE);

// Extract chart data
Map<String, Double> stateDistribution = bridge.getChartData(
    bridge.getStatistic(StatisticType.ITEMS_BY_STATE)
);
```

### Project Dashboard Demo

The `ProjectDashboardDemo` application demonstrates a comprehensive project dashboard with:

- Project selection for filtering statistics
- Key metrics panel showing essential performance indicators
- Distribution charts for work item breakdowns
- Velocity metrics panel showing throughput and time-based metrics
- Project details with assignments and priorities
- Auto-refreshing data for real-time monitoring

## Dashboard Layout

The dashboard is organized into four main panels:

1. **Key Metrics Panel** (top-left)
   - Total work items
   - Work in progress count
   - Overdue items count
   - Completion rate with visual progress meter

2. **Distribution Panel** (top-right)
   - Bar chart showing items by state
   - Bar chart showing items by type

3. **Velocity Panel** (bottom-left)
   - Throughput (items/day)
   - Lead time (days)
   - Cycle time (days)
   - Completed items in the last 30 days
   - Created items in the last 30 days

4. **Details Panel** (bottom-right)
   - Project information
   - Work item assignment breakdown
   - Priority distribution with color coding

## Data Visualization Best Practices

The dashboard components follow these data visualization best practices:

1. **Color Coding**
   - Green for good/high completion rates
   - Yellow for moderate completion rates
   - Red for low completion rates or issues (like overdue items)
   - Different colors for distinguishing between data categories

2. **Sorting and Limiting**
   - Sort data by value to highlight the most significant items
   - Limit the number of bars to prevent information overload
   - Ensure all important elements are visible without scrolling

3. **Real-Time Updates**
   - Automatic refreshing of data for current information
   - Clear timestamps showing when data was last updated
   - Ability to manually refresh when needed

4. **Interactive Filtering**
   - Project selection to focus on specific project data
   - Consistent formatting across all visualizations
   - Clear labels and units for all metrics

## Running the Dashboard

To run the project dashboard demo, use the provided script:

```bash
./run-project-dashboard.sh
```

This will compile and run the `ProjectDashboardDemo` class, showing an interactive dashboard with real-time statistics.

## Customization

The dashboard can be customized in several ways:

1. **Chart Appearance**
   - Modify colors, sorting, and display options for charts
   - Adjust the thresholds for color transitions in progress meters
   - Change the layout and organization of panels

2. **Metrics and Statistics**
   - Add or remove metrics based on project needs
   - Create new chart types for specialized visualizations
   - Adjust refresh rates for different use cases

3. **Integration with Other Data**
   - Connect to different data sources beyond the statistics service
   - Add historical trend data for time-based visualizations
   - Integrate with prediction models for forecasting

## Future Enhancements

Planned enhancements to the project dashboard include:

1. **Burndown/Burnup Charts**
   - Visual representation of work progress over time
   - Trend analysis for completion predictions

2. **Cumulative Flow Diagrams**
   - Visualizing work item states over time
   - Identifying bottlenecks in the workflow

3. **Advanced Filtering**
   - Filter by multiple criteria simultaneously
   - Save and load custom dashboard configurations

4. **Team and User Dashboards**
   - Personalized dashboards focused on individual or team metrics
   - Side-by-side comparison of team performance

5. **Alert Highlighting**
   - Visual alerts for metrics outside acceptable ranges
   - Notification system for important threshold crossings