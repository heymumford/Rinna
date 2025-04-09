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

import org.rinna.pui.component.Button;
import org.rinna.pui.component.Component;
import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.style.Style;
import org.rinna.pui.style.Theme;

/**
 * Renderer for Button components.
 */
public class ButtonRenderer implements ComponentRenderer {
    
    @Override
    public void render(Component component, TerminalRenderer renderer, Theme theme) {
        if (!(component instanceof Button)) {
            return;
        }
        
        Button button = (Button) component;
        String text = button.getText();
        Point position = button.getPosition();
        Dimension size = button.getSize();
        
        if (text == null || position == null || size == null) {
            return;
        }
        
        // Get the appropriate style
        Style style;
        if (!button.isEnabled()) {
            style = theme.getStyle("button.disabled");
        } else if (button.isFocused()) {
            style = theme.getStyle("button.focused");
        } else {
            style = theme.getStyle("button");
        }
        
        // If no style found in theme, use component's style or default
        if (style == null) {
            style = button.getStyle();
            if (style == null) {
                style = theme.getStyle("default");
                if (style == null) {
                    style = new Style();
                }
            }
        }
        
        // Draw button border and background
        renderer.drawRect(position, size, style, true);
        
        // Calculate text position (centered)
        int textX = position.getX() + (size.getWidth() - text.length()) / 2;
        int textY = position.getY() + size.getHeight() / 2;
        Point textPos = new Point(textX, textY);
        
        // Draw the text
        renderer.drawString(text, textPos, style);
    }
}