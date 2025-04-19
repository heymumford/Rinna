/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.pui.style;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a collection of styles for different component types.
 */
public class Theme {
    
    private Map<String, Style> styles;
    
    /**
     * Creates a new empty theme.
     */
    public Theme() {
        this.styles = new HashMap<>();
    }
    
    /**
     * Gets the style for a specific component type.
     * 
     * @param componentType the component type
     * @return the style, or null if no style is defined for the component type
     */
    public Style getStyle(String componentType) {
        return styles.get(componentType);
    }
    
    /**
     * Sets the style for a specific component type.
     * 
     * @param componentType the component type
     * @param style the style
     * @return this instance for method chaining
     */
    public Theme setStyle(String componentType, Style style) {
        styles.put(componentType, style);
        return this;
    }
    
    /**
     * Updates this theme from another theme.
     * Only styles that are defined in the other theme will be updated.
     * 
     * @param other the other theme
     * @return this instance for method chaining
     */
    public Theme updateFrom(Theme other) {
        for (Map.Entry<String, Style> entry : other.styles.entrySet()) {
            styles.put(entry.getKey(), entry.getValue());
        }
        return this;
    }
    
    /**
     * Creates a default theme with basic styles for common component types.
     * 
     * @return the default theme
     */
    public static Theme createDefault() {
        Theme theme = new Theme();
        
        // Default style for all components
        Style defaultStyle = new Style()
            .setForeground(Color.WHITE)
            .setBackground(Color.BLACK);
        theme.setStyle("default", defaultStyle);
        
        // Container style
        Style containerStyle = defaultStyle.copy()
            .setBorderStyle(BorderStyle.SINGLE)
            .setPadding(1);
        theme.setStyle("container", containerStyle);
        
        // Label style
        Style labelStyle = defaultStyle.copy();
        theme.setStyle("label", labelStyle);
        
        // Button style
        Style buttonStyle = defaultStyle.copy()
            .setBackground(Color.BLUE)
            .setBold(true)
            .setBorderStyle(BorderStyle.SINGLE);
        theme.setStyle("button", buttonStyle);
        
        // Button focused style
        Style buttonFocusedStyle = buttonStyle.copy()
            .setBackground(Color.BRIGHT_BLUE);
        theme.setStyle("button.focused", buttonFocusedStyle);
        
        // Button disabled style
        Style buttonDisabledStyle = buttonStyle.copy()
            .setForeground(Color.BRIGHT_BLACK)
            .setBackground(Color.BLACK);
        theme.setStyle("button.disabled", buttonDisabledStyle);
        
        // TextBox style
        Style textBoxStyle = defaultStyle.copy()
            .setBackground(Color.BLACK)
            .setBorderStyle(BorderStyle.SINGLE);
        theme.setStyle("textbox", textBoxStyle);
        
        // TextBox focused style
        Style textBoxFocusedStyle = textBoxStyle.copy()
            .setBorderStyle(BorderStyle.BOLD);
        theme.setStyle("textbox.focused", textBoxFocusedStyle);
        
        // List style
        Style listStyle = defaultStyle.copy()
            .setBorderStyle(BorderStyle.SINGLE);
        theme.setStyle("list", listStyle);
        
        // List item style
        Style listItemStyle = defaultStyle.copy();
        theme.setStyle("list.item", listItemStyle);
        
        // List item selected style
        Style listItemSelectedStyle = listItemStyle.copy()
            .setBackground(Color.BLUE);
        theme.setStyle("list.item.selected", listItemSelectedStyle);
        
        // Menu style
        Style menuStyle = defaultStyle.copy()
            .setBackground(Color.BLUE)
            .setBorderStyle(BorderStyle.SINGLE);
        theme.setStyle("menu", menuStyle);
        
        // Menu item style
        Style menuItemStyle = defaultStyle.copy()
            .setBackground(Color.BLUE);
        theme.setStyle("menu.item", menuItemStyle);
        
        // Menu item focused style
        Style menuItemFocusedStyle = menuItemStyle.copy()
            .setBackground(Color.BRIGHT_BLUE)
            .setBold(true);
        theme.setStyle("menu.item.focused", menuItemFocusedStyle);
        
        // Header style
        Style headerStyle = defaultStyle.copy()
            .setBackground(Color.BLUE)
            .setBold(true);
        theme.setStyle("header", headerStyle);
        
        // Footer style
        Style footerStyle = defaultStyle.copy()
            .setBackground(Color.BLUE);
        theme.setStyle("footer", footerStyle);
        
        // Dialog style
        Style dialogStyle = defaultStyle.copy()
            .setBorderStyle(BorderStyle.DOUBLE)
            .setPadding(2);
        theme.setStyle("dialog", dialogStyle);
        
        // Error message style
        Style errorStyle = defaultStyle.copy()
            .setForeground(Color.BRIGHT_RED)
            .setBold(true);
        theme.setStyle("error", errorStyle);
        
        // Success message style
        Style successStyle = defaultStyle.copy()
            .setForeground(Color.BRIGHT_GREEN)
            .setBold(true);
        theme.setStyle("success", successStyle);
        
        return theme;
    }
}