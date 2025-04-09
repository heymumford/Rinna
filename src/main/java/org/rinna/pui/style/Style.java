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
 * Represents the visual style of a component.
 */
public class Style {
    
    private Color foreground;
    private Color background;
    private boolean bold;
    private boolean italic;
    private boolean underline;
    private boolean blink;
    private int padding;
    private BorderStyle borderStyle;
    
    /**
     * Creates a new style with default settings.
     */
    public Style() {
        this.foreground = Color.WHITE;
        this.background = Color.BLACK;
        this.bold = false;
        this.italic = false;
        this.underline = false;
        this.blink = false;
        this.padding = 0;
        this.borderStyle = BorderStyle.NONE;
    }
    
    /**
     * Gets the foreground color.
     * 
     * @return the foreground color
     */
    public Color getForeground() {
        return foreground;
    }
    
    /**
     * Sets the foreground color.
     * 
     * @param foreground the new foreground color
     * @return this instance for method chaining
     */
    public Style setForeground(Color foreground) {
        this.foreground = foreground;
        return this;
    }
    
    /**
     * Gets the background color.
     * 
     * @return the background color
     */
    public Color getBackground() {
        return background;
    }
    
    /**
     * Sets the background color.
     * 
     * @param background the new background color
     * @return this instance for method chaining
     */
    public Style setBackground(Color background) {
        this.background = background;
        return this;
    }
    
    /**
     * Checks if the text should be rendered as bold.
     * 
     * @return true if the text should be bold, false otherwise
     */
    public boolean isBold() {
        return bold;
    }
    
    /**
     * Sets whether the text should be rendered as bold.
     * 
     * @param bold the new bold state
     * @return this instance for method chaining
     */
    public Style setBold(boolean bold) {
        this.bold = bold;
        return this;
    }
    
    /**
     * Checks if the text should be rendered as italic.
     * 
     * @return true if the text should be italic, false otherwise
     */
    public boolean isItalic() {
        return italic;
    }
    
    /**
     * Sets whether the text should be rendered as italic.
     * 
     * @param italic the new italic state
     * @return this instance for method chaining
     */
    public Style setItalic(boolean italic) {
        this.italic = italic;
        return this;
    }
    
    /**
     * Checks if the text should be rendered with an underline.
     * 
     * @return true if the text should be underlined, false otherwise
     */
    public boolean isUnderline() {
        return underline;
    }
    
    /**
     * Sets whether the text should be rendered with an underline.
     * 
     * @param underline the new underline state
     * @return this instance for method chaining
     */
    public Style setUnderline(boolean underline) {
        this.underline = underline;
        return this;
    }
    
    /**
     * Checks if the text should be rendered with a blinking effect.
     * 
     * @return true if the text should blink, false otherwise
     */
    public boolean isBlink() {
        return blink;
    }
    
    /**
     * Sets whether the text should be rendered with a blinking effect.
     * 
     * @param blink the new blink state
     * @return this instance for method chaining
     */
    public Style setBlink(boolean blink) {
        this.blink = blink;
        return this;
    }
    
    /**
     * Gets the padding.
     * 
     * @return the padding
     */
    public int getPadding() {
        return padding;
    }
    
    /**
     * Sets the padding.
     * 
     * @param padding the new padding
     * @return this instance for method chaining
     */
    public Style setPadding(int padding) {
        this.padding = padding;
        return this;
    }
    
    /**
     * Gets the border style.
     * 
     * @return the border style
     */
    public BorderStyle getBorderStyle() {
        return borderStyle;
    }
    
    /**
     * Sets the border style.
     * 
     * @param borderStyle the new border style
     * @return this instance for method chaining
     */
    public Style setBorderStyle(BorderStyle borderStyle) {
        this.borderStyle = borderStyle;
        return this;
    }
    
    /**
     * Creates a copy of this style.
     * 
     * @return the style copy
     */
    public Style copy() {
        Style copy = new Style();
        copy.foreground = this.foreground;
        copy.background = this.background;
        copy.bold = this.bold;
        copy.italic = this.italic;
        copy.underline = this.underline;
        copy.blink = this.blink;
        copy.padding = this.padding;
        copy.borderStyle = this.borderStyle;
        return copy;
    }
}