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
import org.rinna.pui.component.TextBox;
import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.style.Style;
import org.rinna.pui.style.Theme;

/**
 * Renderer for TextBox components.
 */
public class TextBoxRenderer implements ComponentRenderer {
    
    private static final int SCROLL_MARGIN = 2;
    
    @Override
    public void render(Component component, TerminalRenderer renderer, Theme theme) {
        if (!(component instanceof TextBox)) {
            return;
        }
        
        TextBox textBox = (TextBox) component;
        Point position = textBox.getPosition();
        Dimension size = textBox.getSize();
        
        if (position == null || size == null) {
            return;
        }
        
        // Get the appropriate style
        Style style;
        if (!textBox.isEnabled()) {
            style = theme.getStyle("textbox.disabled");
        } else if (textBox.isFocused()) {
            style = theme.getStyle("textbox.focused");
        } else {
            style = theme.getStyle("textbox");
        }
        
        // If no style found in theme, use component's style or default
        if (style == null) {
            style = textBox.getStyle();
            if (style == null) {
                style = theme.getStyle("default");
                if (style == null) {
                    style = new Style();
                }
            }
        }
        
        // Draw text box border and background
        renderer.drawRect(position, size, style, true);
        
        // Get the displayable text
        String displayText = getDisplayText(textBox);
        
        // Calculate text position
        int textX = position.getX() + 1; // Leave 1 character spacing from border
        int textY = position.getY() + size.getHeight() / 2;
        Point textPos = new Point(textX, textY);
        
        // Draw the text
        renderer.drawString(displayText, textPos, style);
        
        // Draw caret if the text box is focused
        if (textBox.isFocused()) {
            int caretX = textX + getVisibleCaretPosition(textBox);
            Point caretPos = new Point(caretX, textY);
            
            Style caretStyle = new Style()
                .setBackground(style.getForeground())
                .setForeground(style.getBackground());
            
            String caretChar = " ";
            if (textBox.getCaretPosition() < textBox.getText().length()) {
                char ch = textBox.getText().charAt(textBox.getCaretPosition());
                caretChar = String.valueOf(ch);
            }
            
            renderer.drawString(caretChar, caretPos, caretStyle);
        }
    }
    
    /**
     * Gets the displayable text for a text box, taking into account password mode,
     * placeholder text, and horizontal scrolling.
     * 
     * @param textBox the text box
     * @return the displayable text
     */
    private String getDisplayText(TextBox textBox) {
        String text = textBox.getText();
        
        // If the text box is empty and there's a placeholder, show that
        if (text.isEmpty() && textBox.getPlaceholder() != null && !textBox.isFocused()) {
            text = textBox.getPlaceholder();
        } else if (textBox.isPassword()) {
            // If in password mode, replace all characters with the password character
            char passwordChar = textBox.getPasswordChar();
            StringBuilder sb = new StringBuilder(text.length());
            for (int i = 0; i < text.length(); i++) {
                sb.append(passwordChar);
            }
            text = sb.toString();
        }
        
        // Handle horizontal scrolling if the text is too long
        int visibleWidth = textBox.getWidth() - 2; // Account for border
        if (text.length() > visibleWidth) {
            // Determine the visible portion of the text
            int caretPos = textBox.getCaretPosition();
            int startPos = caretPos - visibleWidth + SCROLL_MARGIN;
            
            // Ensure startPos is within bounds
            startPos = Math.max(0, Math.min(text.length() - visibleWidth, startPos));
            
            // Get the visible portion of the text
            text = text.substring(startPos, Math.min(startPos + visibleWidth, text.length()));
        }
        
        return text;
    }
    
    /**
     * Gets the visible caret position in the text box, taking into account horizontal scrolling.
     * 
     * @param textBox the text box
     * @return the visible caret position
     */
    private int getVisibleCaretPosition(TextBox textBox) {
        int visibleWidth = textBox.getWidth() - 2; // Account for border
        int caretPos = textBox.getCaretPosition();
        String text = textBox.getText();
        
        if (text.length() <= visibleWidth) {
            // If the text fits within the visible area, the caret position is as-is
            return caretPos;
        } else {
            // Determine the visible portion of the text
            int startPos = caretPos - visibleWidth + SCROLL_MARGIN;
            
            // Ensure startPos is within bounds
            startPos = Math.max(0, Math.min(text.length() - visibleWidth, startPos));
            
            // Calculate the visible caret position
            return caretPos - startPos;
        }
    }
}