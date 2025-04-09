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

/**
 * Represents different border styles for components.
 */
public enum BorderStyle {
    
    NONE("", "", "", "", "", "", "", ""),
    
    SINGLE("┌", "┐", "└", "┘", "─", "│", "┬", "┴"),
    
    DOUBLE("╔", "╗", "╚", "╝", "═", "║", "╦", "╩"),
    
    ROUNDED("╭", "╮", "╰", "╯", "─", "│", "┬", "┴"),
    
    DASHED("┌", "┐", "└", "┘", "┄", "┊", "┬", "┴"),
    
    BOLD("┏", "┓", "┗", "┛", "━", "┃", "┳", "┻");
    
    private final String topLeft;
    private final String topRight;
    private final String bottomLeft;
    private final String bottomRight;
    private final String horizontal;
    private final String vertical;
    private final String topT;
    private final String bottomT;
    
    /**
     * Creates a new border style with the specified characters.
     * 
     * @param topLeft the top-left corner character
     * @param topRight the top-right corner character
     * @param bottomLeft the bottom-left corner character
     * @param bottomRight the bottom-right corner character
     * @param horizontal the horizontal line character
     * @param vertical the vertical line character
     * @param topT the top T-junction character
     * @param bottomT the bottom T-junction character
     */
    BorderStyle(String topLeft, String topRight, String bottomLeft, String bottomRight,
                String horizontal, String vertical, String topT, String bottomT) {
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomLeft = bottomLeft;
        this.bottomRight = bottomRight;
        this.horizontal = horizontal;
        this.vertical = vertical;
        this.topT = topT;
        this.bottomT = bottomT;
    }
    
    /**
     * Gets the top-left corner character.
     * 
     * @return the top-left corner character
     */
    public String getTopLeft() {
        return topLeft;
    }
    
    /**
     * Gets the top-right corner character.
     * 
     * @return the top-right corner character
     */
    public String getTopRight() {
        return topRight;
    }
    
    /**
     * Gets the bottom-left corner character.
     * 
     * @return the bottom-left corner character
     */
    public String getBottomLeft() {
        return bottomLeft;
    }
    
    /**
     * Gets the bottom-right corner character.
     * 
     * @return the bottom-right corner character
     */
    public String getBottomRight() {
        return bottomRight;
    }
    
    /**
     * Gets the horizontal line character.
     * 
     * @return the horizontal line character
     */
    public String getHorizontal() {
        return horizontal;
    }
    
    /**
     * Gets the vertical line character.
     * 
     * @return the vertical line character
     */
    public String getVertical() {
        return vertical;
    }
    
    /**
     * Gets the top T-junction character.
     * 
     * @return the top T-junction character
     */
    public String getTopT() {
        return topT;
    }
    
    /**
     * Gets the bottom T-junction character.
     * 
     * @return the bottom T-junction character
     */
    public String getBottomT() {
        return bottomT;
    }
}