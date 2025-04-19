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

/**
 * Interface for layout managers that define how components are arranged within a container.
 */
public interface Layout {
    
    /**
     * Lays out a specific component within a container.
     * 
     * @param component the component to layout
     * @param parent the parent container
     */
    void layoutComponent(Component component, Container parent);
    
    /**
     * Lays out all components within a container.
     * 
     * @param parent the parent container
     */
    void layoutContainer(Container parent);
    
    /**
     * Gets constraints for a specific component.
     * 
     * @param component the component
     * @return the layout constraints, or null if the component has no constraints
     */
    Object getConstraints(Component component);
    
    /**
     * Sets constraints for a specific component.
     * 
     * @param component the component
     * @param constraints the layout constraints
     */
    void setConstraints(Component component, Object constraints);
}