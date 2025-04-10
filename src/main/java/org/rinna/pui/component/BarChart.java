/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.pui.component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.rinna.pui.geom.Dimension;
import org.rinna.pui.style.Color;
import org.rinna.pui.style.Style;

/**
 * A component that displays data as a horizontal bar chart.
 * Can be used to visualize distributions and other numerical data.
 */
public class BarChart extends Container {
    
    private Map<String, Double> data = new LinkedHashMap<>();
    private String title;
    private String yAxisLabel;
    private String xAxisLabel;
    private int maxBars = Integer.MAX_VALUE;
    private boolean sortByValue = true;
    private boolean showValues = true;
    private boolean showPercentages = false;
    private List<Color> barColors = new ArrayList<>();
    private double maxValue = -1;
    private SortOrder sortOrder = SortOrder.DESCENDING;
    
    /**
     * Sort order for the bars.
     */
    public enum SortOrder {
        ASCENDING,
        DESCENDING,
        NONE
    }
    
    /**
     * Creates a new bar chart with the specified ID and dimensions.
     *
     * @param id The component ID
     * @param width The width
     * @param height The height
     */
    public BarChart(String id, int width, int height) {
        super(id);
        setSize(new Dimension(width, height));
        
        // Initialize with default colors
        barColors.add(Color.BLUE);
        barColors.add(Color.GREEN);
        barColors.add(Color.YELLOW);
        barColors.add(Color.MAGENTA);
        barColors.add(Color.CYAN);
        barColors.add(Color.RED);
    }
    
    /**
     * Sets the data for the chart.
     *
     * @param data The data to display
     * @return This component for method chaining
     */
    public BarChart setData(Map<String, Double> data) {
        this.data = new LinkedHashMap<>(data);
        rebuild();
        return this;
    }
    
    /**
     * Sets the title of the chart.
     *
     * @param title The chart title
     * @return This component for method chaining
     */
    public BarChart setTitle(String title) {
        this.title = title;
        rebuild();
        return this;
    }
    
    /**
     * Sets the y-axis label.
     *
     * @param label The y-axis label
     * @return This component for method chaining
     */
    public BarChart setYAxisLabel(String label) {
        this.yAxisLabel = label;
        rebuild();
        return this;
    }
    
    /**
     * Sets the x-axis label.
     *
     * @param label The x-axis label
     * @return This component for method chaining
     */
    public BarChart setXAxisLabel(String label) {
        this.xAxisLabel = label;
        rebuild();
        return this;
    }
    
    /**
     * Sets the maximum number of bars to display.
     *
     * @param maxBars The maximum number of bars
     * @return This component for method chaining
     */
    public BarChart setMaxBars(int maxBars) {
        this.maxBars = maxBars;
        rebuild();
        return this;
    }
    
    /**
     * Sets whether to sort the bars by value.
     *
     * @param sortByValue True to sort by value, false to use the original order
     * @return This component for method chaining
     */
    public BarChart setSortByValue(boolean sortByValue) {
        this.sortByValue = sortByValue;
        rebuild();
        return this;
    }
    
    /**
     * Sets the sort order for the bars.
     *
     * @param sortOrder The sort order
     * @return This component for method chaining
     */
    public BarChart setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        rebuild();
        return this;
    }
    
    /**
     * Sets whether to show values at the end of bars.
     *
     * @param showValues True to show values, false to hide them
     * @return This component for method chaining
     */
    public BarChart setShowValues(boolean showValues) {
        this.showValues = showValues;
        rebuild();
        return this;
    }
    
    /**
     * Sets whether to show percentages alongside values.
     *
     * @param showPercentages True to show percentages, false to hide them
     * @return This component for method chaining
     */
    public BarChart setShowPercentages(boolean showPercentages) {
        this.showPercentages = showPercentages;
        rebuild();
        return this;
    }
    
    /**
     * Sets the colors to use for the bars.
     *
     * @param colors The colors to use
     * @return This component for method chaining
     */
    public BarChart setBarColors(List<Color> colors) {
        if (colors != null && !colors.isEmpty()) {
            this.barColors = new ArrayList<>(colors);
        }
        rebuild();
        return this;
    }
    
    /**
     * Sets a fixed maximum value for scaling.
     * If not set, the maximum value in the data will be used.
     *
     * @param maxValue The maximum value for scaling
     * @return This component for method chaining
     */
    public BarChart setMaxValue(double maxValue) {
        this.maxValue = maxValue;
        rebuild();
        return this;
    }
    
    /**
     * Rebuilds the chart with the current settings.
     */
    private void rebuild() {
        // Clear existing components
        removeAllComponents();
        
        if (data.isEmpty()) {
            Label noDataLabel = new Label("No data available");
            noDataLabel.setAlignment(Label.Alignment.CENTER);
            addComponent(noDataLabel);
            return;
        }
        
        // Create a copy of the data for processing
        Map<String, Double> chartData = new LinkedHashMap<>(data);
        
        // Sort the data if needed
        if (sortByValue) {
            // Create a sorted map
            List<Map.Entry<String, Double>> entries = new ArrayList<>(chartData.entrySet());
            
            if (sortOrder == SortOrder.ASCENDING) {
                entries.sort(Map.Entry.comparingByValue());
            } else if (sortOrder == SortOrder.DESCENDING) {
                entries.sort(Map.Entry.<String, Double>comparingByValue().reversed());
            }
            
            chartData = new LinkedHashMap<>();
            for (Map.Entry<String, Double> entry : entries) {
                chartData.put(entry.getKey(), entry.getValue());
            }
        }
        
        // Limit the number of bars if needed
        if (chartData.size() > maxBars) {
            chartData = chartData.entrySet().stream()
                .limit(maxBars)
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    Map.Entry::getValue,
                    (e1, e2) -> e1,
                    LinkedHashMap::new
                ));
        }
        
        // Create the main layout
        BoxLayout layout = new BoxLayout(BoxLayout.Orientation.VERTICAL, 1);
        setLayout(layout);
        
        // Add title if provided
        if (title != null && !title.isEmpty()) {
            Label titleLabel = new Label(title);
            titleLabel.setAlignment(Label.Alignment.CENTER);
            Style titleStyle = new Style().setBold(true);
            titleLabel.setStyle(titleStyle);
            addComponent(titleLabel);
        }
        
        // Calculate the maximum label length for alignment
        int maxLabelLength = chartData.keySet().stream()
            .mapToInt(String::length)
            .max()
            .orElse(10);
        
        // Find the maximum value for scaling
        double actualMaxValue = this.maxValue > 0 
            ? this.maxValue 
            : chartData.values().stream().mapToDouble(Double::doubleValue).max().orElse(1.0);
        
        // Calculate the total for percentages if needed
        double total = showPercentages 
            ? chartData.values().stream().mapToDouble(Double::doubleValue).sum() 
            : 0.0;
        
        // Calculate bar width based on available space
        int barAreaWidth = getWidth() - maxLabelLength - 12; // Reserve space for labels and values
        if (barAreaWidth < 5) barAreaWidth = 5; // Minimum bar area width
        
        // Add a container for the y-axis label if provided
        if (yAxisLabel != null && !yAxisLabel.isEmpty()) {
            Label yAxisLabelComponent = new Label(yAxisLabel);
            yAxisLabelComponent.setAlignment(Label.Alignment.LEFT);
            addComponent(yAxisLabelComponent);
        }
        
        // Add bars for each data point
        int colorIndex = 0;
        for (Map.Entry<String, Double> entry : chartData.entrySet()) {
            String label = entry.getKey();
            double value = entry.getValue();
            
            // Calculate bar width proportional to value
            int barWidth = (int) Math.round((value / actualMaxValue) * barAreaWidth);
            if (barWidth < 1) barWidth = 1; // Ensure minimum width
            
            // Choose bar color
            Color barColor = barColors.get(colorIndex % barColors.size());
            colorIndex++;
            
            // Create a container for this bar row
            Container barRow = new Container("bar-row-" + label);
            BoxLayout barRowLayout = new BoxLayout(BoxLayout.Orientation.HORIZONTAL, 0);
            barRow.setLayout(barRowLayout);
            
            // Add the label
            Label barLabel = new Label(String.format("%-" + maxLabelLength + "s", label));
            barRow.addComponent(barLabel);
            
            // Create the bar
            Label barComponent = new Label(" ".repeat(barWidth));
            Style barStyle = new Style()
                .setBackground(barColor)
                .setForeground(Color.BLACK);
            barComponent.setStyle(barStyle);
            barRow.addComponent(barComponent);
            
            // Add the value label if needed
            if (showValues) {
                String valueText;
                if (showPercentages && total > 0) {
                    double percentage = (value / total) * 100;
                    valueText = String.format(" %.2f (%.1f%%)", value, percentage);
                } else {
                    valueText = String.format(" %.2f", value);
                }
                
                Label valueLabel = new Label(valueText);
                barRow.addComponent(valueLabel);
            }
            
            // Add the bar row to the chart
            addComponent(barRow);
        }
        
        // Add x-axis label if provided
        if (xAxisLabel != null && !xAxisLabel.isEmpty()) {
            Label xAxisLabelComponent = new Label(xAxisLabel);
            xAxisLabelComponent.setAlignment(Label.Alignment.CENTER);
            addComponent(xAxisLabelComponent);
        }
    }
}