/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.pui.render;

import org.rinna.pui.component.Component;
import org.rinna.pui.component.MillerColumnsContainer;
import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;

/**
 * Renderer for MillerColumnsContainer. This renderer delegates rendering
 * to the container's child components as it's essentially a specialized layout
 * container for hierarchical navigation.
 */
public class MillerColumnsRenderer implements ComponentRenderer {

    @Override
    public void render(TerminalRenderer renderer, Component component, Point position, Dimension size) {
        // The Miller columns container is just a specialized container
        // The actual rendering is handled by rendering its child components
        // which are organized by the container's layout manager
    }
    
    @Override
    public boolean canRender(Component component) {
        return component instanceof MillerColumnsContainer;
    }
}