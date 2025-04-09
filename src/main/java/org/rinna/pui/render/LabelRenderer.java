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
import org.rinna.pui.component.Label;
import org.rinna.pui.geom.Point;
import org.rinna.pui.style.Style;
import org.rinna.pui.style.Theme;

/**
 * Renderer for Label components.
 */
public class LabelRenderer implements ComponentRenderer {
    
    @Override
    public void render(Component component, TerminalRenderer renderer, Theme theme) {
        if (!(component instanceof Label)) {
            return;
        }
        
        Label label = (Label) component;
        String text = label.getText();
        Point position = label.getPosition();
        
        if (text == null || position == null) {
            return;
        }
        
        // Get the style from the component or the theme
        Style style = label.getStyle();
        if (style == null) {
            style = theme.getStyle("label");
            if (style == null) {
                style = theme.getStyle("default");
                if (style == null) {
                    style = new Style();
                }
            }
        }
        
        // If the component is disabled, use the disabled style
        if (!label.isEnabled()) {
            Style disabledStyle = theme.getStyle("label.disabled");
            if (disabledStyle != null) {
                style = disabledStyle;
            }
        }
        
        // Draw the text
        renderer.drawString(text, position, style);
    }
}