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
import org.rinna.pui.geom.Point;
import org.rinna.pui.render.ComponentRenderer;
import org.rinna.pui.render.LabelRenderer;
import org.rinna.pui.style.Style;

/**
 * A component that displays a text label.
 */
public class Label implements Component {
    
    private String id;
    private String text;
    private Point position;
    private Dimension size;
    private Style style;
    private Container parent;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean focused = false;
    private LabelRenderer renderer;
    private Object layoutConstraints;
    
    /**
     * Creates a new label with the specified text and ID.
     * 
     * @param id the label ID
     * @param text the label text
     */
    public Label(String id, String text) {
        this.id = id;
        this.text = text;
        this.renderer = new LabelRenderer();
        this.size = new Dimension(text.length(), 1);
    }
    
    /**
     * Creates a new label with the specified text and a default ID.
     * 
     * @param text the label text
     */
    public Label(String text) {
        this("label-" + System.currentTimeMillis(), text);
    }
    
    /**
     * Gets the label text.
     * 
     * @return the label text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Sets the label text.
     * 
     * @param text the new label text
     */
    public void setText(String text) {
        this.text = text;
        
        // Update size based on new text length
        setSize(new Dimension(text.length(), 1));
    }
    
    @Override
    public String getId() {
        return id;
    }
    
    @Override
    public Point getPosition() {
        return position;
    }
    
    @Override
    public void setPosition(Point position) {
        this.position = position;
    }
    
    @Override
    public Dimension getSize() {
        return size;
    }
    
    @Override
    public void setSize(Dimension size) {
        this.size = size;
    }
    
    @Override
    public Style getStyle() {
        return style;
    }
    
    @Override
    public void setStyle(Style style) {
        this.style = style;
    }
    
    @Override
    public Container getParent() {
        return parent;
    }
    
    @Override
    public void setParent(Container parent) {
        this.parent = parent;
    }
    
    @Override
    public boolean isVisible() {
        return visible;
    }
    
    @Override
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public boolean isFocused() {
        return focused;
    }
    
    @Override
    public void setFocused(boolean focused) {
        this.focused = focused;
    }
    
    @Override
    public boolean handleKey(int key) {
        // Labels don't handle key events
        return false;
    }
    
    @Override
    public ComponentRenderer getRenderer() {
        return renderer;
    }
    
    @Override
    public void update(long deltaMs) {
        // Labels don't have dynamic state to update
    }
    
    @Override
    public Object getLayoutConstraints() {
        return layoutConstraints;
    }
    
    @Override
    public void setLayoutConstraints(Object constraints) {
        this.layoutConstraints = constraints;
    }
}