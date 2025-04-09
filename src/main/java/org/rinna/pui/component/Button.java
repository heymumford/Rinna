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
import org.rinna.pui.input.KeyHandler;
import org.rinna.pui.render.ButtonRenderer;
import org.rinna.pui.render.ComponentRenderer;
import org.rinna.pui.style.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * A button component that can be clicked to trigger an action.
 */
public class Button implements Component {
    
    /**
     * Interface for button click event listeners.
     */
    public interface ClickListener {
        /**
         * Called when the button is clicked.
         * 
         * @param button the button that was clicked
         */
        void onClick(Button button);
    }
    
    private String id;
    private String text;
    private Point position;
    private Dimension size;
    private Style style;
    private Container parent;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean focused = false;
    private ButtonRenderer renderer;
    private Object layoutConstraints;
    private List<ClickListener> clickListeners;
    
    /**
     * Creates a new button with the specified text and ID.
     * 
     * @param id the button ID
     * @param text the button text
     */
    public Button(String id, String text) {
        this.id = id;
        this.text = text;
        this.renderer = new ButtonRenderer();
        // Add padding for the button (2 spaces on each side)
        this.size = new Dimension(text.length() + 4, 3);
        this.clickListeners = new ArrayList<>();
    }
    
    /**
     * Creates a new button with the specified text and a default ID.
     * 
     * @param text the button text
     */
    public Button(String text) {
        this("button-" + System.currentTimeMillis(), text);
    }
    
    /**
     * Gets the button text.
     * 
     * @return the button text
     */
    public String getText() {
        return text;
    }
    
    /**
     * Sets the button text.
     * 
     * @param text the new button text
     */
    public void setText(String text) {
        this.text = text;
        
        // Update size based on new text length
        setSize(new Dimension(text.length() + 4, 3));
    }
    
    /**
     * Adds a click listener to this button.
     * 
     * @param listener the listener to add
     */
    public void addClickListener(ClickListener listener) {
        clickListeners.add(listener);
    }
    
    /**
     * Removes a click listener from this button.
     * 
     * @param listener the listener to remove
     * @return true if the listener was removed, false otherwise
     */
    public boolean removeClickListener(ClickListener listener) {
        return clickListeners.remove(listener);
    }
    
    /**
     * Simulates a click on this button, notifying all listeners.
     */
    public void click() {
        if (!isEnabled()) {
            return;
        }
        
        for (ClickListener listener : clickListeners) {
            listener.onClick(this);
        }
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
        if (!isEnabled()) {
            return false;
        }
        
        // Space or Enter keys trigger a click
        if (key == 32 || key == KeyHandler.KEY_ENTER) {
            click();
            return true;
        }
        
        return false;
    }
    
    @Override
    public ComponentRenderer getRenderer() {
        return renderer;
    }
    
    @Override
    public void update(long deltaMs) {
        // Buttons don't have dynamic state to update
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