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
import org.rinna.pui.component.List;
import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.style.Style;
import org.rinna.pui.style.Theme;

/**
 * Renderer for List components.
 */
public class ListRenderer<T> implements ComponentRenderer {
    
    @Override
    public void render(Component component, TerminalRenderer renderer, Theme theme) {
        if (!(component instanceof List)) {
            return;
        }
        
        @SuppressWarnings("unchecked")
        List<T> list = (List<T>) component;
        Point position = list.getPosition();
        Dimension size = list.getSize();
        
        if (position == null || size == null) {
            return;
        }
        
        // Get the style for the list
        Style listStyle;
        if (!list.isEnabled()) {
            listStyle = theme.getStyle("list.disabled");
        } else if (list.isFocused()) {
            listStyle = theme.getStyle("list.focused");
        } else {
            listStyle = theme.getStyle("list");
        }
        
        // If no style found in theme, use component's style or default
        if (listStyle == null) {
            listStyle = list.getStyle();
            if (listStyle == null) {
                listStyle = theme.getStyle("default");
                if (listStyle == null) {
                    listStyle = new Style();
                }
            }
        }
        
        // Draw list border and background
        renderer.drawRect(position, size, listStyle, true);
        
        // Get item styles
        Style itemStyle = theme.getStyle("list.item");
        if (itemStyle == null) {
            itemStyle = listStyle;
        }
        
        Style selectedItemStyle = theme.getStyle("list.item.selected");
        if (selectedItemStyle == null) {
            selectedItemStyle = itemStyle;
        }
        
        // Draw items
        java.util.List<T> items = list.getItems();
        int topIndex = list.getTopIndex();
        int selectedIndex = list.getSelectedIndex();
        int visibleItems = list.getVisibleItems();
        int width = size.getWidth() - 2; // Account for borders
        
        for (int i = 0; i < visibleItems; i++) {
            int itemIndex = topIndex + i;
            if (itemIndex >= items.size()) {
                break;
            }
            
            T item = items.get(itemIndex);
            String displayText = list.getItemRenderer().getDisplayString(item);
            
            // Truncate or pad the text to fit the available width
            if (displayText.length() > width) {
                displayText = displayText.substring(0, width);
            } else if (displayText.length() < width) {
                displayText = padString(displayText, width);
            }
            
            // Determine item position
            int itemX = position.getX() + 1; // Account for left border
            int itemY = position.getY() + 1 + i; // Account for top border
            Point itemPos = new Point(itemX, itemY);
            
            // Choose style based on selection state
            Style style = (itemIndex == selectedIndex) ? selectedItemStyle : itemStyle;
            
            // Draw the item
            renderer.drawString(displayText, itemPos, style);
        }
        
        // Draw scrollbar if needed
        if (items.size() > visibleItems) {
            drawScrollbar(renderer, list, listStyle);
        }
    }
    
    /**
     * Pads a string with spaces to the specified width.
     * 
     * @param str the string to pad
     * @param width the desired width
     * @return the padded string
     */
    private String padString(String str, int width) {
        StringBuilder sb = new StringBuilder(str);
        while (sb.length() < width) {
            sb.append(' ');
        }
        return sb.toString();
    }
    
    /**
     * Draws a simple scrollbar for the list.
     * 
     * @param renderer the terminal renderer
     * @param list the list component
     * @param style the style to use
     */
    private void drawScrollbar(TerminalRenderer renderer, List<T> list, Style style) {
        Point position = list.getPosition();
        Dimension size = list.getSize();
        
        int totalItems = list.getItems().size();
        int visibleItems = list.getVisibleItems();
        int topIndex = list.getTopIndex();
        
        // Calculate scrollbar position and size
        int scrollbarX = position.getX() + size.getWidth() - 1;
        int scrollbarHeight = size.getHeight() - 2;
        int scrollbarPos = (int) ((float) topIndex / (totalItems - visibleItems) * scrollbarHeight);
        
        // Draw scrollbar track
        for (int i = 0; i < scrollbarHeight; i++) {
            Point trackPos = new Point(scrollbarX, position.getY() + 1 + i);
            renderer.drawString("│", trackPos, style);
        }
        
        // Draw scrollbar thumb
        Point thumbPos = new Point(scrollbarX, position.getY() + 1 + scrollbarPos);
        renderer.drawString("█", thumbPos, style);
    }
}