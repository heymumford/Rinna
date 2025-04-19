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

import java.util.HashMap;
import java.util.Map;

import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;

/**
 * Layout manager that arranges components in a single row or column.
 */
public class BoxLayout implements Layout {
    
    /**
     * Orientation of the layout.
     */
    public enum Orientation {
        HORIZONTAL,
        VERTICAL
    }
    
    private final Orientation orientation;
    private final int gap;
    private final Map<Component, Object> constraints;
    
    /**
     * Creates a new box layout with the specified orientation and gap.
     * 
     * @param orientation the orientation
     * @param gap the gap between components
     */
    public BoxLayout(Orientation orientation, int gap) {
        this.orientation = orientation;
        this.gap = gap;
        this.constraints = new HashMap<>();
    }
    
    /**
     * Creates a new box layout with the specified orientation and no gap.
     * 
     * @param orientation the orientation
     */
    public BoxLayout(Orientation orientation) {
        this(orientation, 0);
    }
    
    @Override
    public void layoutComponent(Component component, Container parent) {
        // BoxLayout only supports layoutContainer, individual component layout is not supported
    }
    
    @Override
    public void layoutContainer(Container parent) {
        Point parentPos = parent.getPosition();
        Dimension parentSize = parent.getSize();
        
        if (parentPos == null || parentSize == null) {
            return;
        }
        
        int x = parentPos.getX();
        int y = parentPos.getY();
        int width = parentSize.getWidth();
        int height = parentSize.getHeight();
        
        // Account for padding
        int padding = 0;
        if (parent.getStyle() != null) {
            padding = parent.getStyle().getPadding();
        }
        
        x += padding;
        y += padding;
        width -= padding * 2;
        height -= padding * 2;
        
        // Account for border
        if (parent.getStyle() != null && parent.getStyle().getBorderStyle() != null) {
            x += 1;
            y += 1;
            width -= 2;
            height -= 2;
        }
        
        // Get the visible components
        java.util.List<Component> components = new java.util.ArrayList<>();
        for (Component component : parent.getComponents()) {
            if (component.isVisible()) {
                components.add(component);
            }
        }
        
        if (components.isEmpty()) {
            return;
        }
        
        // Calculate component sizes based on constraints
        if (orientation == Orientation.HORIZONTAL) {
            layoutHorizontal(components, x, y, width, height);
        } else {
            layoutVertical(components, x, y, width, height);
        }
    }
    
    /**
     * Lays out components horizontally.
     * 
     * @param components the components to layout
     * @param x the starting x coordinate
     * @param y the starting y coordinate
     * @param width the available width
     * @param height the available height
     */
    private void layoutHorizontal(java.util.List<Component> components, int x, int y, int width, int height) {
        // Calculate total weight of components
        int totalWeight = 0;
        for (Component component : components) {
            BoxConstraints c = getBoxConstraints(component);
            totalWeight += c.weight;
        }
        
        // Calculate total fixed width and remaining width for weighted components
        int totalFixedWidth = 0;
        int fixedComponentCount = 0;
        
        for (Component component : components) {
            BoxConstraints c = getBoxConstraints(component);
            Dimension prefSize = component.getSize();
            
            if (c.weight == 0 && prefSize != null) {
                totalFixedWidth += prefSize.getWidth();
                fixedComponentCount++;
            }
        }
        
        // Account for gaps
        int totalGaps = components.size() - 1;
        int totalGapWidth = totalGaps * gap;
        
        // Calculate remaining width for weighted components
        int remainingWidth = width - totalFixedWidth - totalGapWidth;
        
        // Layout components
        int currentX = x;
        for (int i = 0; i < components.size(); i++) {
            Component component = components.get(i);
            BoxConstraints c = getBoxConstraints(component);
            Dimension prefSize = component.getSize();
            
            int componentWidth;
            if (c.weight > 0 && totalWeight > 0) {
                componentWidth = (remainingWidth * c.weight) / totalWeight;
            } else if (prefSize != null) {
                componentWidth = prefSize.getWidth();
            } else {
                componentWidth = 10; // Default minimum width
            }
            
            int componentHeight = c.fillHeight ? height : (prefSize != null ? prefSize.getHeight() : 1);
            int componentY = y;
            
            if (!c.fillHeight && prefSize != null) {
                // Align component vertically
                switch (c.vAlign) {
                    case TOP:
                        componentY = y;
                        break;
                    case CENTER:
                        componentY = y + (height - prefSize.getHeight()) / 2;
                        break;
                    case BOTTOM:
                        componentY = y + height - prefSize.getHeight();
                        break;
                }
            }
            
            component.setPosition(new Point(currentX, componentY));
            component.setSize(new Dimension(componentWidth, componentHeight));
            
            currentX += componentWidth + gap;
        }
    }
    
    /**
     * Lays out components vertically.
     * 
     * @param components the components to layout
     * @param x the starting x coordinate
     * @param y the starting y coordinate
     * @param width the available width
     * @param height the available height
     */
    private void layoutVertical(java.util.List<Component> components, int x, int y, int width, int height) {
        // Calculate total weight of components
        int totalWeight = 0;
        for (Component component : components) {
            BoxConstraints c = getBoxConstraints(component);
            totalWeight += c.weight;
        }
        
        // Calculate total fixed height and remaining height for weighted components
        int totalFixedHeight = 0;
        int fixedComponentCount = 0;
        
        for (Component component : components) {
            BoxConstraints c = getBoxConstraints(component);
            Dimension prefSize = component.getSize();
            
            if (c.weight == 0 && prefSize != null) {
                totalFixedHeight += prefSize.getHeight();
                fixedComponentCount++;
            }
        }
        
        // Account for gaps
        int totalGaps = components.size() - 1;
        int totalGapHeight = totalGaps * gap;
        
        // Calculate remaining height for weighted components
        int remainingHeight = height - totalFixedHeight - totalGapHeight;
        
        // Layout components
        int currentY = y;
        for (int i = 0; i < components.size(); i++) {
            Component component = components.get(i);
            BoxConstraints c = getBoxConstraints(component);
            Dimension prefSize = component.getSize();
            
            int componentHeight;
            if (c.weight > 0 && totalWeight > 0) {
                componentHeight = (remainingHeight * c.weight) / totalWeight;
            } else if (prefSize != null) {
                componentHeight = prefSize.getHeight();
            } else {
                componentHeight = 1; // Default minimum height
            }
            
            int componentWidth = c.fillWidth ? width : (prefSize != null ? prefSize.getWidth() : 10);
            int componentX = x;
            
            if (!c.fillWidth && prefSize != null) {
                // Align component horizontally
                switch (c.hAlign) {
                    case LEFT:
                        componentX = x;
                        break;
                    case CENTER:
                        componentX = x + (width - prefSize.getWidth()) / 2;
                        break;
                    case RIGHT:
                        componentX = x + width - prefSize.getWidth();
                        break;
                }
            }
            
            component.setPosition(new Point(componentX, currentY));
            component.setSize(new Dimension(componentWidth, componentHeight));
            
            currentY += componentHeight + gap;
        }
    }
    
    @Override
    public Object getConstraints(Component component) {
        return constraints.get(component);
    }
    
    @Override
    public void setConstraints(Component component, Object constraints) {
        if (constraints instanceof BoxConstraints) {
            this.constraints.put(component, constraints);
        } else {
            throw new IllegalArgumentException("Constraints must be of type BoxConstraints");
        }
    }
    
    /**
     * Gets the box constraints for a component.
     * If no constraints are set, returns default constraints.
     * 
     * @param component the component
     * @return the box constraints
     */
    private BoxConstraints getBoxConstraints(Component component) {
        Object c = constraints.get(component);
        return c instanceof BoxConstraints ? (BoxConstraints) c : new BoxConstraints();
    }
    
    /**
     * Horizontal alignment options.
     */
    public enum HorizontalAlignment {
        LEFT,
        CENTER,
        RIGHT
    }
    
    /**
     * Vertical alignment options.
     */
    public enum VerticalAlignment {
        TOP,
        CENTER,
        BOTTOM
    }
    
    /**
     * Constraints for box layout.
     */
    public static class BoxConstraints {
        private int weight;
        private boolean fillWidth;
        private boolean fillHeight;
        private HorizontalAlignment hAlign;
        private VerticalAlignment vAlign;
        
        /**
         * Creates default box constraints.
         */
        public BoxConstraints() {
            this.weight = 0;
            this.fillWidth = false;
            this.fillHeight = false;
            this.hAlign = HorizontalAlignment.LEFT;
            this.vAlign = VerticalAlignment.TOP;
        }
        
        /**
         * Creates box constraints with the specified weight.
         * 
         * @param weight the weight of the component
         */
        public BoxConstraints(int weight) {
            this();
            this.weight = weight;
        }
        
        /**
         * Creates box constraints with the specified settings.
         * 
         * @param weight the weight of the component
         * @param fillWidth whether to fill the available width
         * @param fillHeight whether to fill the available height
         */
        public BoxConstraints(int weight, boolean fillWidth, boolean fillHeight) {
            this(weight);
            this.fillWidth = fillWidth;
            this.fillHeight = fillHeight;
        }
        
        /**
         * Creates box constraints with the specified settings.
         * 
         * @param weight the weight of the component
         * @param fillWidth whether to fill the available width
         * @param fillHeight whether to fill the available height
         * @param hAlign the horizontal alignment
         * @param vAlign the vertical alignment
         */
        public BoxConstraints(int weight, boolean fillWidth, boolean fillHeight,
                             HorizontalAlignment hAlign, VerticalAlignment vAlign) {
            this(weight, fillWidth, fillHeight);
            this.hAlign = hAlign;
            this.vAlign = vAlign;
        }
        
        /**
         * Sets the weight of the component.
         * 
         * @param weight the weight
         * @return this instance for method chaining
         */
        public BoxConstraints setWeight(int weight) {
            this.weight = weight;
            return this;
        }
        
        /**
         * Sets whether to fill the available width.
         * 
         * @param fillWidth whether to fill the width
         * @return this instance for method chaining
         */
        public BoxConstraints setFillWidth(boolean fillWidth) {
            this.fillWidth = fillWidth;
            return this;
        }
        
        /**
         * Sets whether to fill the available height.
         * 
         * @param fillHeight whether to fill the height
         * @return this instance for method chaining
         */
        public BoxConstraints setFillHeight(boolean fillHeight) {
            this.fillHeight = fillHeight;
            return this;
        }
        
        /**
         * Sets the horizontal alignment.
         * 
         * @param hAlign the horizontal alignment
         * @return this instance for method chaining
         */
        public BoxConstraints setHorizontalAlignment(HorizontalAlignment hAlign) {
            this.hAlign = hAlign;
            return this;
        }
        
        /**
         * Sets the vertical alignment.
         * 
         * @param vAlign the vertical alignment
         * @return this instance for method chaining
         */
        public BoxConstraints setVerticalAlignment(VerticalAlignment vAlign) {
            this.vAlign = vAlign;
            return this;
        }
    }
}