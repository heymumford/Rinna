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

import java.io.IOException;

import org.rinna.pui.component.Component;
import org.rinna.pui.component.Container;
import org.rinna.pui.geom.Dimension;
import org.rinna.pui.geom.Point;
import org.rinna.pui.style.Style;
import org.rinna.pui.style.Theme;

/**
 * Low-level terminal renderer that provides methods for drawing text and other elements.
 */
public class TerminalRenderer {
    
    private int width;
    private int height;
    private String[][] buffer;
    private Style[][] styleBuffer;
    
    /**
     * Creates a new terminal renderer with default settings.
     */
    public TerminalRenderer() {
        // Default to 80x24 terminal size
        this.width = 80;
        this.height = 24;
        resetBuffers();
    }
    
    /**
     * Initializes the terminal for rendering.
     * This puts the terminal in raw mode and hides the cursor.
     * 
     * @throws IOException if an I/O error occurs
     */
    public void initialize() throws IOException {
        // Enter alternate screen buffer
        System.out.print("\u001b[?1049h");
        
        // Hide cursor
        System.out.print("\u001b[?25l");
        
        // Clear screen
        System.out.print("\u001b[2J");
        
        // Get terminal size
        // In a real implementation, this would use JNA or similar to get the actual terminal size
        updateTerminalSize();
    }
    
    /**
     * Restores the terminal to its original state.
     * This exits raw mode and shows the cursor.
     * 
     * @throws IOException if an I/O error occurs
     */
    public void cleanup() throws IOException {
        // Show cursor
        System.out.print("\u001b[?25h");
        
        // Exit alternate screen buffer
        System.out.print("\u001b[?1049l");
        
        // Reset colors
        System.out.print("\u001b[0m");
    }
    
    /**
     * Updates the terminal size.
     * In a real implementation, this would use JNA or similar to get the actual terminal size.
     */
    private void updateTerminalSize() {
        // This is a placeholder for a real implementation
        // In a real implementation, this would use JNA or similar to get the actual terminal size
        
        // For now, we'll assume a standard terminal size of 80x24
        // You may update this based on your actual implementation needs
        this.width = 80;
        this.height = 24;
        
        resetBuffers();
    }
    
    /**
     * Resets the rendering buffers.
     */
    private void resetBuffers() {
        this.buffer = new String[height][width];
        this.styleBuffer = new Style[height][width];
        
        // Initialize buffers with spaces and default style
        Style defaultStyle = new Style();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                buffer[y][x] = " ";
                styleBuffer[y][x] = defaultStyle;
            }
        }
    }
    
    /**
     * Clears the rendering buffers.
     */
    public void clear() {
        resetBuffers();
    }
    
    /**
     * Gets the width of the terminal.
     * 
     * @return the terminal width
     */
    public int getWidth() {
        return width;
    }
    
    /**
     * Gets the height of the terminal.
     * 
     * @return the terminal height
     */
    public int getHeight() {
        return height;
    }
    
    /**
     * Gets the terminal size as a Dimension.
     * 
     * @return the terminal size
     */
    public Dimension getSize() {
        return new Dimension(width, height);
    }
    
    /**
     * Draws a string at the specified position with the specified style.
     * 
     * @param text the text to draw
     * @param position the position to draw at
     * @param style the style to use
     */
    public void drawString(String text, Point position, Style style) {
        int x = position.getX();
        int y = position.getY();
        
        if (y < 0 || y >= height) {
            return;
        }
        
        for (int i = 0; i < text.length(); i++) {
            int posX = x + i;
            if (posX < 0 || posX >= width) {
                continue;
            }
            
            buffer[y][posX] = String.valueOf(text.charAt(i));
            styleBuffer[y][posX] = style;
        }
    }
    
    /**
     * Draws a horizontal line at the specified position with the specified length and style.
     * 
     * @param position the starting position
     * @param length the length of the line
     * @param style the style to use
     * @param lineChar the character to use for the line
     */
    public void drawHorizontalLine(Point position, int length, Style style, String lineChar) {
        int x = position.getX();
        int y = position.getY();
        
        if (y < 0 || y >= height) {
            return;
        }
        
        for (int i = 0; i < length; i++) {
            int posX = x + i;
            if (posX < 0 || posX >= width) {
                continue;
            }
            
            buffer[y][posX] = lineChar;
            styleBuffer[y][posX] = style;
        }
    }
    
    /**
     * Draws a vertical line at the specified position with the specified length and style.
     * 
     * @param position the starting position
     * @param length the length of the line
     * @param style the style to use
     * @param lineChar the character to use for the line
     */
    public void drawVerticalLine(Point position, int length, Style style, String lineChar) {
        int x = position.getX();
        int y = position.getY();
        
        for (int i = 0; i < length; i++) {
            int posY = y + i;
            if (posY < 0 || posY >= height || x < 0 || x >= width) {
                continue;
            }
            
            buffer[posY][x] = lineChar;
            styleBuffer[posY][x] = style;
        }
    }
    
    /**
     * Draws a rectangle at the specified position with the specified size and style.
     * 
     * @param position the top-left corner position
     * @param size the size of the rectangle
     * @param style the style to use
     * @param filled whether the rectangle should be filled
     */
    public void drawRect(Point position, Dimension size, Style style, boolean filled) {
        int x = position.getX();
        int y = position.getY();
        int width = size.getWidth();
        int height = size.getHeight();
        
        // Draw horizontal lines
        drawHorizontalLine(new Point(x, y), width, style, style.getBorderStyle().getHorizontal());
        drawHorizontalLine(new Point(x, y + height - 1), width, style, style.getBorderStyle().getHorizontal());
        
        // Draw vertical lines
        drawVerticalLine(new Point(x, y), height, style, style.getBorderStyle().getVertical());
        drawVerticalLine(new Point(x + width - 1, y), height, style, style.getBorderStyle().getVertical());
        
        // Draw corners
        if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
            buffer[y][x] = style.getBorderStyle().getTopLeft();
            styleBuffer[y][x] = style;
        }
        
        if (x + width - 1 >= 0 && x + width - 1 < this.width && y >= 0 && y < this.height) {
            buffer[y][x + width - 1] = style.getBorderStyle().getTopRight();
            styleBuffer[y][x + width - 1] = style;
        }
        
        if (x >= 0 && x < this.width && y + height - 1 >= 0 && y + height - 1 < this.height) {
            buffer[y + height - 1][x] = style.getBorderStyle().getBottomLeft();
            styleBuffer[y + height - 1][x] = style;
        }
        
        if (x + width - 1 >= 0 && x + width - 1 < this.width && y + height - 1 >= 0 && y + height - 1 < this.height) {
            buffer[y + height - 1][x + width - 1] = style.getBorderStyle().getBottomRight();
            styleBuffer[y + height - 1][x + width - 1] = style;
        }
        
        // Fill the rectangle if requested
        if (filled) {
            for (int row = y + 1; row < y + height - 1; row++) {
                for (int col = x + 1; col < x + width - 1; col++) {
                    if (row >= 0 && row < this.height && col >= 0 && col < this.width) {
                        buffer[row][col] = " ";
                        styleBuffer[row][col] = style;
                    }
                }
            }
        }
    }
    
    /**
     * Renders a component using its renderer.
     * 
     * @param component the component to render
     * @param theme the current theme
     */
    public void render(Component component, Theme theme) {
        if (!component.isVisible()) {
            return;
        }
        
        ComponentRenderer renderer = component.getRenderer();
        if (renderer != null) {
            renderer.render(component, this, theme);
        }
        
        // If the component is a container, render its children
        if (component instanceof Container) {
            Container container = (Container) component;
            for (Component child : container.getComponents()) {
                if (child.isVisible()) {
                    render(child, theme);
                }
            }
        }
    }
    
    /**
     * Refreshes the terminal display.
     * This flushes the buffers to the terminal.
     */
    public void refresh() {
        // Move cursor to top-left corner
        System.out.print("\u001b[H");
        
        // Render the buffer
        Style currentStyle = null;
        
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                Style style = styleBuffer[y][x];
                
                // Only output style escape sequences if the style has changed
                if (style != currentStyle) {
                    outputStyleEscapeSequence(style);
                    currentStyle = style;
                }
                
                System.out.print(buffer[y][x]);
            }
            
            // Move to the next line
            System.out.println();
        }
        
        // Reset styles at the end
        System.out.print("\u001b[0m");
        
        // Flush output
        System.out.flush();
    }
    
    /**
     * Outputs the ANSI escape sequence for the specified style.
     * 
     * @param style the style
     */
    private void outputStyleEscapeSequence(Style style) {
        // Reset all attributes first
        System.out.print("\u001b[0m");
        
        // Set foreground color
        System.out.print(style.getForeground().getForegroundEscapeSequence());
        
        // Set background color
        System.out.print(style.getBackground().getBackgroundEscapeSequence());
        
        // Set other attributes
        if (style.isBold()) {
            System.out.print("\u001b[1m");
        }
        
        if (style.isItalic()) {
            System.out.print("\u001b[3m");
        }
        
        if (style.isUnderline()) {
            System.out.print("\u001b[4m");
        }
        
        if (style.isBlink()) {
            System.out.print("\u001b[5m");
        }
    }
}