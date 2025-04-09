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

import org.rinna.pui.geom.Dimension;
import org.rinna.pui.style.Color;
import org.rinna.pui.style.Style;

/**
 * A component that displays a progress meter.
 * Can be used to visualize completion rates, percentages, and other progress metrics.
 */
public class ProgressMeter extends Container {
    
    private double value;
    private double maxValue;
    private String title;
    private String unit = "%";
    private boolean showPercentage = true;
    private boolean useColors = true;
    private Color lowColor = Color.RED;
    private Color mediumColor = Color.YELLOW;
    private Color highColor = Color.GREEN;
    private double lowThreshold = 33.0;
    private double highThreshold = 67.0;
    private static final String PROGRESS_CHAR = "█";
    
    /**
     * Creates a new progress meter with the specified ID and dimensions.
     *
     * @param id The component ID
     * @param width The width
     * @param height The height
     */
    public ProgressMeter(String id, int width, int height) {
        super(id);
        setSize(new Dimension(width, height));
        this.value = 0;
        this.maxValue = 100;
        
        // Set up layout
        BoxLayout layout = new BoxLayout(BoxLayout.Orientation.VERTICAL, 0);
        setLayout(layout);
        
        // Build initial state
        rebuild();
    }
    
    /**
     * Sets the current value.
     *
     * @param value The current value
     * @return This component for method chaining
     */
    public ProgressMeter setValue(double value) {
        this.value = value;
        rebuild();
        return this;
    }
    
    /**
     * Sets the maximum value (for scaling).
     *
     * @param maxValue The maximum value
     * @return This component for method chaining
     */
    public ProgressMeter setMaxValue(double maxValue) {
        if (maxValue > 0) {
            this.maxValue = maxValue;
            rebuild();
        }
        return this;
    }
    
    /**
     * Sets the title of the progress meter.
     *
     * @param title The title
     * @return This component for method chaining
     */
    public ProgressMeter setTitle(String title) {
        this.title = title;
        rebuild();
        return this;
    }
    
    /**
     * Sets the unit for the value.
     *
     * @param unit The unit string
     * @return This component for method chaining
     */
    public ProgressMeter setUnit(String unit) {
        this.unit = unit;
        rebuild();
        return this;
    }
    
    /**
     * Sets whether to show the percentage value.
     *
     * @param showPercentage True to show percentage, false to hide it
     * @return This component for method chaining
     */
    public ProgressMeter setShowPercentage(boolean showPercentage) {
        this.showPercentage = showPercentage;
        rebuild();
        return this;
    }
    
    /**
     * Sets whether to use color coding based on value.
     *
     * @param useColors True to use colors, false for monochrome
     * @return This component for method chaining
     */
    public ProgressMeter setUseColors(boolean useColors) {
        this.useColors = useColors;
        rebuild();
        return this;
    }
    
    /**
     * Sets the color for low values.
     *
     * @param color The color for low values
     * @return This component for method chaining
     */
    public ProgressMeter setLowColor(Color color) {
        this.lowColor = color;
        rebuild();
        return this;
    }
    
    /**
     * Sets the color for medium values.
     *
     * @param color The color for medium values
     * @return This component for method chaining
     */
    public ProgressMeter setMediumColor(Color color) {
        this.mediumColor = color;
        rebuild();
        return this;
    }
    
    /**
     * Sets the color for high values.
     *
     * @param color The color for high values
     * @return This component for method chaining
     */
    public ProgressMeter setHighColor(Color color) {
        this.highColor = color;
        rebuild();
        return this;
    }
    
    /**
     * Sets the thresholds for color transitions.
     *
     * @param lowThreshold The threshold between low and medium (as percentage)
     * @param highThreshold The threshold between medium and high (as percentage)
     * @return This component for method chaining
     */
    public ProgressMeter setThresholds(double lowThreshold, double highThreshold) {
        this.lowThreshold = lowThreshold;
        this.highThreshold = highThreshold;
        rebuild();
        return this;
    }
    
    /**
     * Rebuilds the progress meter with the current settings.
     */
    private void rebuild() {
        // Clear existing components
        removeAllComponents();
        
        // Calculate percentage for display
        double percentage = (value / maxValue) * 100.0;
        
        // Add title if provided
        if (title != null && !title.isEmpty()) {
            Label titleLabel = new Label(title);
            titleLabel.setAlignment(Label.Alignment.LEFT);
            Style titleStyle = new Style().setBold(true);
            titleLabel.setStyle(titleStyle);
            addComponent(titleLabel);
        }
        
        // Create value label
        String valueText;
        if (showPercentage) {
            valueText = String.format("%.1f%% (%.2f %s)", percentage, value, unit);
        } else {
            valueText = String.format("%.2f %s", value, unit);
        }
        
        Label valueLabel = new Label(valueText);
        addComponent(valueLabel);
        
        // Create the progress bar
        Container barContainer = new Container("bar-container");
        BoxLayout barLayout = new BoxLayout(BoxLayout.Orientation.HORIZONTAL, 0);
        barContainer.setLayout(barLayout);
        
        // Calculate bar width
        int barWidth = getWidth() - 2; // Reserve space for brackets
        if (barWidth < 10) barWidth = 10; // Minimum width
        
        int filledWidth = (int) Math.round((percentage / 100.0) * barWidth);
        if (filledWidth > barWidth) filledWidth = barWidth;
        if (filledWidth < 0) filledWidth = 0;
        
        // Add brackets
        Label leftBracket = new Label("[");
        barContainer.addComponent(leftBracket);
        
        // Add filled portion
        if (filledWidth > 0) {
            Label filledBar = new Label(PROGRESS_CHAR.repeat(filledWidth));
            
            // Set color based on percentage and settings
            if (useColors) {
                Color barColor;
                if (percentage < lowThreshold) {
                    barColor = lowColor;
                } else if (percentage < highThreshold) {
                    barColor = mediumColor;
                } else {
                    barColor = highColor;
                }
                
                Style barStyle = new Style().setForeground(barColor);
                filledBar.setStyle(barStyle);
            }
            
            barContainer.addComponent(filledBar);
        }
        
        // Add empty portion
        int emptyWidth = barWidth - filledWidth;
        if (emptyWidth > 0) {
            Label emptyBar = new Label(" ".repeat(emptyWidth));
            barContainer.addComponent(emptyBar);
        }
        
        // Add right bracket
        Label rightBracket = new Label("]");
        barContainer.addComponent(rightBracket);
        
        // Add the bar container
        addComponent(barContainer);
    }
}