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

import java.util.List;

/**
 * Interface for container components that can hold other components.
 */
public class Container implements Component {
    
    private String id;
    private List<Component> components;
    private Layout layout;
    private Component focusedComponent;
    private Container parent;
    private boolean visible = true;
    private boolean enabled = true;
    private boolean focused = false;
    
    /**
     * Creates a new container with a default ID.
     */
    public Container() {
        this("container-" + System.currentTimeMillis());
    }
    
    /**
     * Creates a new container with the specified ID.
     * 
     * @param id the container ID
     */
    public Container(String id) {
        this.id = id;
    }
    
    /**
     * Gets the layout used by this container.
     * 
     * @return the layout
     */
    public Layout getLayout() {
        return layout;
    }
    
    /**
     * Sets the layout for this container.
     * 
     * @param layout the layout to use
     */
    public void setLayout(Layout layout) {
        this.layout = layout;
    }
    
    /**
     * Adds a component to this container.
     * 
     * @param component the component to add
     */
    public void addComponent(Component component) {
        components.add(component);
        component.setParent(this);
        if (layout != null) {
            layout.layoutComponent(component, this);
        }
    }
    
    /**
     * Removes a component from this container.
     * 
     * @param component the component to remove
     * @return true if the component was removed, false otherwise
     */
    public boolean removeComponent(Component component) {
        boolean removed = components.remove(component);
        if (removed) {
            component.setParent(null);
            if (component == focusedComponent) {
                focusedComponent = null;
            }
        }
        return removed;
    }
    
    /**
     * Gets all components in this container.
     * 
     * @return the list of components
     */
    public List<Component> getComponents() {
        return components;
    }
    
    /**
     * Gets the component with the specified ID.
     * 
     * @param id the component ID
     * @return the component, or null if no component with the specified ID exists
     */
    public Component getComponentById(String id) {
        for (Component component : components) {
            if (id.equals(component.getId())) {
                return component;
            }
        }
        return null;
    }
    
    /**
     * Gets the focused component.
     * 
     * @return the focused component, or null if no component is focused
     */
    public Component getFocusedComponent() {
        return focusedComponent;
    }
    
    /**
     * Sets the focused component.
     * 
     * @param component the component to focus
     */
    public void setFocusedComponent(Component component) {
        if (components.contains(component)) {
            if (focusedComponent != null) {
                focusedComponent.setFocused(false);
            }
            focusedComponent = component;
            focusedComponent.setFocused(true);
        }
    }
    
    /**
     * Moves focus to the next focusable component.
     * 
     * @return true if focus was moved, false otherwise
     */
    public boolean focusNext() {
        if (components.isEmpty()) {
            return false;
        }
        
        int currentIndex = focusedComponent != null ? components.indexOf(focusedComponent) : -1;
        int startIndex = (currentIndex + 1) % components.size();
        
        for (int i = 0; i < components.size(); i++) {
            int index = (startIndex + i) % components.size();
            Component component = components.get(index);
            if (component.isEnabled() && component.isVisible()) {
                setFocusedComponent(component);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Moves focus to the previous focusable component.
     * 
     * @return true if focus was moved, false otherwise
     */
    public boolean focusPrevious() {
        if (components.isEmpty()) {
            return false;
        }
        
        int currentIndex = focusedComponent != null ? components.indexOf(focusedComponent) : 0;
        int startIndex = (currentIndex - 1 + components.size()) % components.size();
        
        for (int i = 0; i < components.size(); i++) {
            int index = (startIndex - i + components.size()) % components.size();
            Component component = components.get(index);
            if (component.isEnabled() && component.isVisible()) {
                setFocusedComponent(component);
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Validates this container's layout.
     * This will recalculate the position and size of all components.
     */
    public void validateLayout() {
        if (layout != null) {
            layout.layoutContainer(this);
        }
    }
    
    @Override
    public String getId() {
        return id;
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
        
        // Update children visibility
        if (!visible) {
            for (Component component : components) {
                component.setVisible(false);
            }
        }
    }
    
    @Override
    public boolean isEnabled() {
        return enabled;
    }
    
    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        
        // Update children enabled state
        if (!enabled) {
            for (Component component : components) {
                component.setEnabled(false);
            }
        }
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
        // Tab key (9) for cycling focus
        if (key == 9) {
            return focusNext();
        }
        
        // Shift+Tab for reverse cycling focus
        if (key == 353) { // Shift+Tab
            return focusPrevious();
        }
        
        // Delegate to focused component if any
        if (focusedComponent != null) {
            return focusedComponent.handleKey(key);
        }
        
        return false;
    }
    
    // Remaining Component interface methods would be implemented here
    // with placeholder implementations to compile
    
    @Override
    public org.rinna.pui.geom.Point getPosition() {
        return null;
    }
    
    @Override
    public void setPosition(org.rinna.pui.geom.Point position) {
    }
    
    @Override
    public org.rinna.pui.geom.Dimension getSize() {
        return null;
    }
    
    @Override
    public void setSize(org.rinna.pui.geom.Dimension size) {
    }
    
    @Override
    public org.rinna.pui.style.Style getStyle() {
        return null;
    }
    
    @Override
    public void setStyle(org.rinna.pui.style.Style style) {
    }
    
    @Override
    public org.rinna.pui.render.ComponentRenderer getRenderer() {
        return null;
    }
    
    @Override
    public void update(long deltaMs) {
        // Update all components
        for (Component component : components) {
            component.update(deltaMs);
        }
    }
    
    @Override
    public Object getLayoutConstraints() {
        return null;
    }
    
    @Override
    public void setLayoutConstraints(Object constraints) {
    }
}