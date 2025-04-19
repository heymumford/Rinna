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
import org.rinna.pui.style.Theme;

/**
 * Interface for component renderers.
 * Defines how a specific component type is rendered to the terminal.
 */
public interface ComponentRenderer {
    
    /**
     * Renders a component to the terminal.
     * 
     * @param component the component to render
     * @param renderer the terminal renderer
     * @param theme the current theme
     */
    void render(Component component, TerminalRenderer renderer, Theme theme);
}