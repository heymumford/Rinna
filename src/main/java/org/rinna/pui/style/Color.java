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
 * Represents a color in the terminal.
 * Uses ANSI color codes for terminal compatibility.
 */
public enum Color {
    
    BLACK(0),
    RED(1),
    GREEN(2),
    YELLOW(3),
    BLUE(4),
    MAGENTA(5),
    CYAN(6),
    WHITE(7),
    
    BRIGHT_BLACK(8),
    BRIGHT_RED(9),
    BRIGHT_GREEN(10),
    BRIGHT_YELLOW(11),
    BRIGHT_BLUE(12),
    BRIGHT_MAGENTA(13),
    BRIGHT_CYAN(14),
    BRIGHT_WHITE(15);
    
    private final int code;
    
    /**
     * Creates a new color with the specified ANSI code.
     * 
     * @param code the ANSI color code
     */
    Color(int code) {
        this.code = code;
    }
    
    /**
     * Gets the ANSI color code for this color.
     * 
     * @return the ANSI color code
     */
    public int getCode() {
        return code;
    }
    
    /**
     * Gets the ANSI escape sequence for setting this color as the foreground color.
     * 
     * @return the ANSI escape sequence
     */
    public String getForegroundEscapeSequence() {
        return code < 8 ? "\u001b[3" + code + "m" : "\u001b[9" + (code - 8) + "m";
    }
    
    /**
     * Gets the ANSI escape sequence for setting this color as the background color.
     * 
     * @return the ANSI escape sequence
     */
    public String getBackgroundEscapeSequence() {
        return code < 8 ? "\u001b[4" + code + "m" : "\u001b[10" + (code - 8) + "m";
    }
}