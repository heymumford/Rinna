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
import org.rinna.pui.style.Style;

/**
 * Base interface for all UI components.
 * Defines the core functionality that all components must implement.
 */
public interface Component {
    
    /**
     * Gets the ID of this component.
     * 
     * @return the component ID
     */
    String getId();
    
    /**
     * Gets the position of this component.
     * 
     * @return the component position
     */
    Point getPosition();
    
    /**
     * Sets the position of this component.
     * 
     * @param position the new position
     */
    void setPosition(Point position);
    
    /**
     * Gets the size of this component.
     * 
     * @return the component size
     */
    Dimension getSize();
    
    /**
     * Sets the size of this component.
     * 
     * @param size the new size
     */
    void setSize(Dimension size);
    
    /**
     * Gets the style of this component.
     * 
     * @return the component style
     */
    Style getStyle();
    
    /**
     * Sets the style of this component.
     * 
     * @param style the new style
     */
    void setStyle(Style style);
    
    /**
     * Gets the parent container of this component.
     * 
     * @return the parent container, or null if this component has no parent
     */
    Container getParent();
    
    /**
     * Sets the parent container of this component.
     * 
     * @param parent the new parent container
     */
    void setParent(Container parent);
    
    /**
     * Checks if this component is visible.
     * 
     * @return true if this component is visible, false otherwise
     */
    boolean isVisible();
    
    /**
     * Sets the visibility of this component.
     * 
     * @param visible the new visibility state
     */
    void setVisible(boolean visible);
    
    /**
     * Checks if this component is enabled.
     * 
     * @return true if this component is enabled, false otherwise
     */
    boolean isEnabled();
    
    /**
     * Sets the enabled state of this component.
     * 
     * @param enabled the new enabled state
     */
    void setEnabled(boolean enabled);
    
    /**
     * Checks if this component is focused.
     * 
     * @return true if this component is focused, false otherwise
     */
    boolean isFocused();
    
    /**
     * Sets the focused state of this component.
     * 
     * @param focused the new focused state
     */
    void setFocused(boolean focused);
    
    /**
     * Handles a key press event.
     * 
     * @param key the key code
     * @return true if the key was handled, false otherwise
     */
    boolean handleKey(int key);
    
    /**
     * Gets the renderer for this component.
     * 
     * @return the component renderer
     */
    ComponentRenderer getRenderer();
    
    /**
     * Updates this component's state.
     * This method is called once per frame.
     * 
     * @param deltaMs the time elapsed since the last update, in milliseconds
     */
    void update(long deltaMs);
    
    /**
     * Gets the layout constraints for this component.
     * 
     * @return the layout constraints, or null if this component has no constraints
     */
    Object getLayoutConstraints();
    
    /**
     * Sets the layout constraints for this component.
     * 
     * @param constraints the new layout constraints
     */
    void setLayoutConstraints(Object constraints);
}