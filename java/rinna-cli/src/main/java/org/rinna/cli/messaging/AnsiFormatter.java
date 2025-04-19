/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.messaging;

/**
 * Utility class for working with ANSI escape sequences for BBS-style message formatting.
 * Provides methods for rendering colorful text and special effects in terminal.
 */
public class AnsiFormatter {
    
    // ANSI escape code prefix
    private static final String ESC = "\u001B[";
    
    // Text formatting
    public static final String RESET = ESC + "0m";
    public static final String BOLD = ESC + "1m";
    public static final String ITALIC = ESC + "3m";
    public static final String UNDERLINE = ESC + "4m";
    public static final String BLINK = ESC + "5m";
    public static final String RAPID_BLINK = ESC + "6m";
    public static final String INVERSE = ESC + "7m";
    public static final String HIDDEN = ESC + "8m";
    public static final String STRIKETHROUGH = ESC + "9m";
    
    // Foreground colors
    public static final String FG_BLACK = ESC + "30m";
    public static final String FG_RED = ESC + "31m";
    public static final String FG_GREEN = ESC + "32m";
    public static final String FG_YELLOW = ESC + "33m";
    public static final String FG_BLUE = ESC + "34m";
    public static final String FG_MAGENTA = ESC + "35m";
    public static final String FG_CYAN = ESC + "36m";
    public static final String FG_WHITE = ESC + "37m";
    
    // Background colors
    public static final String BG_BLACK = ESC + "40m";
    public static final String BG_RED = ESC + "41m";
    public static final String BG_GREEN = ESC + "42m";
    public static final String BG_YELLOW = ESC + "43m";
    public static final String BG_BLUE = ESC + "44m";
    public static final String BG_MAGENTA = ESC + "45m";
    public static final String BG_CYAN = ESC + "46m";
    public static final String BG_WHITE = ESC + "47m";
    
    // Bright foreground colors
    public static final String BRIGHT_FG_BLACK = ESC + "90m";
    public static final String BRIGHT_FG_RED = ESC + "91m";
    public static final String BRIGHT_FG_GREEN = ESC + "92m";
    public static final String BRIGHT_FG_YELLOW = ESC + "93m";
    public static final String BRIGHT_FG_BLUE = ESC + "94m";
    public static final String BRIGHT_FG_MAGENTA = ESC + "95m";
    public static final String BRIGHT_FG_CYAN = ESC + "96m";
    public static final String BRIGHT_FG_WHITE = ESC + "97m";
    
    // Bright background colors
    public static final String BRIGHT_BG_BLACK = ESC + "100m";
    public static final String BRIGHT_BG_RED = ESC + "101m";
    public static final String BRIGHT_BG_GREEN = ESC + "102m";
    public static final String BRIGHT_BG_YELLOW = ESC + "103m";
    public static final String BRIGHT_BG_BLUE = ESC + "104m";
    public static final String BRIGHT_BG_MAGENTA = ESC + "105m";
    public static final String BRIGHT_BG_CYAN = ESC + "106m";
    public static final String BRIGHT_BG_WHITE = ESC + "107m";
    
    // BBS-style box drawing characters
    public static final String BOX_TL = "┌";
    public static final String BOX_TR = "┐";
    public static final String BOX_BL = "└";
    public static final String BOX_BR = "┘";
    public static final String BOX_H = "─";
    public static final String BOX_V = "│";
    
    // Terminal control
    public static final String CLEAR_SCREEN = ESC + "2J";
    public static final String CURSOR_HOME = ESC + "H";
    public static final String CURSOR_UP = ESC + "1A";
    public static final String CURSOR_DOWN = ESC + "1B";
    public static final String CURSOR_RIGHT = ESC + "1C";
    public static final String CURSOR_LEFT = ESC + "1D";
    
    /**
     * Parses raw text with BBScode-like tags and converts them to ANSI escape sequences.
     * Format: |COLOR|text or |STYLE|text with | as closing tag
     * Example: |RED|This is red text| |B|Bold text| |INVERSE|Inverse text|
     *
     * @param text text with BBScode tags
     * @return text with ANSI escape sequences
     */
    public static String format(String text) {
        if (text == null) {
            return "";
        }
        
        // Check if the terminal supports ANSI escape sequences
        if (!isAnsiSupported()) {
            // Remove all tags if ANSI is not supported
            return text.replaceAll("\\|[A-Z_]+\\|", "").replaceAll("\\|", "");
        }
        
        String result = text;
        
        // Replace foreground color tags
        result = result.replace("|BLACK|", FG_BLACK);
        result = result.replace("|RED|", FG_RED);
        result = result.replace("|GREEN|", FG_GREEN);
        result = result.replace("|YELLOW|", FG_YELLOW);
        result = result.replace("|BLUE|", FG_BLUE);
        result = result.replace("|MAGENTA|", FG_MAGENTA);
        result = result.replace("|CYAN|", FG_CYAN);
        result = result.replace("|WHITE|", FG_WHITE);
        
        // Replace bright foreground color tags
        result = result.replace("|BRIGHT_BLACK|", BRIGHT_FG_BLACK);
        result = result.replace("|BRIGHT_RED|", BRIGHT_FG_RED);
        result = result.replace("|BRIGHT_GREEN|", BRIGHT_FG_GREEN);
        result = result.replace("|BRIGHT_YELLOW|", BRIGHT_FG_YELLOW);
        result = result.replace("|BRIGHT_BLUE|", BRIGHT_FG_BLUE);
        result = result.replace("|BRIGHT_MAGENTA|", BRIGHT_FG_MAGENTA);
        result = result.replace("|BRIGHT_CYAN|", BRIGHT_FG_CYAN);
        result = result.replace("|BRIGHT_WHITE|", BRIGHT_FG_WHITE);
        
        // Replace background color tags
        result = result.replace("|BG_BLACK|", BG_BLACK);
        result = result.replace("|BG_RED|", BG_RED);
        result = result.replace("|BG_GREEN|", BG_GREEN);
        result = result.replace("|BG_YELLOW|", BG_YELLOW);
        result = result.replace("|BG_BLUE|", BG_BLUE);
        result = result.replace("|BG_MAGENTA|", BG_MAGENTA);
        result = result.replace("|BG_CYAN|", BG_CYAN);
        result = result.replace("|BG_WHITE|", BG_WHITE);
        
        // Replace text style tags
        result = result.replace("|B|", BOLD);
        result = result.replace("|BOLD|", BOLD);
        result = result.replace("|I|", ITALIC);
        result = result.replace("|ITALIC|", ITALIC);
        result = result.replace("|U|", UNDERLINE);
        result = result.replace("|UNDERLINE|", UNDERLINE);
        result = result.replace("|BLINK|", BLINK);
        result = result.replace("|FAST_BLINK|", RAPID_BLINK);
        result = result.replace("|INVERSE|", INVERSE);
        result = result.replace("|STRIKE|", STRIKETHROUGH);
        
        // Replace closing tags
        result = result.replace("|", RESET);
        
        // Ensure the string ends with a reset code
        if (!result.endsWith(RESET)) {
            result += RESET;
        }
        
        return result;
    }
    
    /**
     * Creates a BBS-style message box with a title and content.
     *
     * @param title the title of the box
     * @param content the content inside the box
     * @param width the width of the box
     * @return formatted box as a string
     */
    public static String createMessageBox(String title, String content, int width) {
        if (!isAnsiSupported()) {
            // Return plain text if ANSI is not supported
            return title + "\n\n" + content;
        }
        
        StringBuilder box = new StringBuilder();
        
        // Calculate actual width based on the title length
        int boxWidth = Math.max(width, title.length() + 4);
        
        // Top border with title
        box.append(BRIGHT_FG_CYAN).append(BOX_TL);
        box.append(BOX_H).append(" ").append(BRIGHT_FG_WHITE).append(BOLD).append(title).append(RESET).append(BRIGHT_FG_CYAN).append(" ");
        
        for (int i = 0; i < boxWidth - title.length() - 4; i++) {
            box.append(BOX_H);
        }
        
        box.append(BOX_TR).append(RESET).append("\n");
        
        // Wrap content to fit within the box
        String[] lines = wrapText(content, boxWidth - 4);
        
        // Content with borders
        for (String line : lines) {
            box.append(BRIGHT_FG_CYAN).append(BOX_V).append(RESET).append(" ");
            box.append(line);
            
            // Pad with spaces to reach the right border
            int padding = boxWidth - line.length() - 4;
            for (int i = 0; i < padding; i++) {
                box.append(" ");
            }
            
            box.append(" ").append(BRIGHT_FG_CYAN).append(BOX_V).append(RESET).append("\n");
        }
        
        // Bottom border
        box.append(BRIGHT_FG_CYAN).append(BOX_BL);
        for (int i = 0; i < boxWidth - 2; i++) {
            box.append(BOX_H);
        }
        box.append(BOX_BR).append(RESET);
        
        return box.toString();
    }
    
    /**
     * Wraps text at word boundaries to fit within a specified width.
     * Preserves ANSI escape sequences when wrapping.
     *
     * @param text the text to wrap
     * @param width the maximum width for each line
     * @return array of wrapped lines
     */
    private static String[] wrapText(String text, int width) {
        // Split input text by newlines first
        String[] paragraphs = text.split("\n");
        java.util.List<String> lines = new java.util.ArrayList<>();
        
        for (String paragraph : paragraphs) {
            // Skip empty paragraphs
            if (paragraph.trim().isEmpty()) {
                lines.add("");
                continue;
            }
            
            StringBuilder currentLine = new StringBuilder();
            int currentWidth = 0;
            
            // Split into words
            String[] words = paragraph.split(" ");
            
            for (String word : words) {
                // Skip empty words
                if (word.isEmpty()) {
                    continue;
                }
                
                // Calculate visual length (excluding ANSI sequences)
                String plainWord = stripAnsi(word);
                int wordLength = plainWord.length();
                
                // Check if adding this word would exceed the line width
                if (currentWidth + wordLength + (currentWidth > 0 ? 1 : 0) > width) {
                    // Add current line to lines and start a new line
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                    currentWidth = wordLength;
                } else {
                    // Add word to current line
                    if (currentWidth > 0) {
                        currentLine.append(" ");
                        currentWidth++;
                    }
                    currentLine.append(word);
                    currentWidth += wordLength;
                }
            }
            
            // Add the last line if not empty
            if (currentWidth > 0) {
                lines.add(currentLine.toString());
            }
        }
        
        return lines.toArray(new String[0]);
    }
    
    /**
     * Strips ANSI escape sequences from a string to calculate its visual length.
     *
     * @param input the input string with ANSI sequences
     * @return the string without ANSI sequences
     */
    private static String stripAnsi(String input) {
        return input.replaceAll("\u001B\\[[0-9;]*m", "");
    }
    
    /**
     * Checks if the terminal supports ANSI escape sequences.
     *
     * @return true if ANSI is supported, false otherwise
     */
    private static boolean isAnsiSupported() {
        String term = System.getenv("TERM");
        String colorterm = System.getenv("COLORTERM");
        String forceColor = System.getenv("FORCE_COLOR");
        
        return (term != null && (term.contains("color") || term.contains("ansi") || term.equals("xterm"))) ||
               (colorterm != null) ||
               (forceColor != null && !forceColor.equals("0"));
    }
    
    /**
     * Creates a retro BBS-style animated banner.
     *
     * @param text the text to display in the banner
     * @return the animated banner text
     */
    public static String createBanner(String text) {
        if (!isAnsiSupported()) {
            return text;
        }
        
        StringBuilder banner = new StringBuilder();
        
        // Top border
        banner.append(BRIGHT_FG_MAGENTA);
        for (int i = 0; i < text.length() + 8; i++) {
            banner.append("═");
        }
        banner.append(RESET).append("\n");
        
        // Middle part with text
        banner.append(BRIGHT_FG_MAGENTA).append("║").append(RESET)
              .append(" ").append(BOLD).append(BLINK).append(BRIGHT_FG_YELLOW)
              .append("★").append(RESET).append(" ")
              .append(BOLD).append(BRIGHT_FG_CYAN).append(text).append(RESET)
              .append(" ").append(BOLD).append(BLINK).append(BRIGHT_FG_YELLOW)
              .append("★").append(RESET).append(" ")
              .append(BRIGHT_FG_MAGENTA).append("║").append(RESET).append("\n");
        
        // Bottom border
        banner.append(BRIGHT_FG_MAGENTA);
        for (int i = 0; i < text.length() + 8; i++) {
            banner.append("═");
        }
        banner.append(RESET);
        
        return banner.toString();
    }
}