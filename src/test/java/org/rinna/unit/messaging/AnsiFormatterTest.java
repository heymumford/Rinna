/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.unit.messaging;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Execution;
import org.junit.jupiter.api.ExecutionMode;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.rinna.base.UnitTest;
import org.rinna.cli.messaging.AnsiFormatter;

/**
 * Unit tests for AnsiFormatter class.
 */
@Tag("unit")
@Tag("smoke")
@Execution(ExecutionMode.CONCURRENT)
class AnsiFormatterTest extends UnitTest {

    // Method to get private method accessibility for testing
    private Method getPrivateMethod(String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = AnsiFormatter.class.getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method;
    }

    /**
     * Test constants defined in AnsiFormatter.
     */
    @Test
    void testAnsiFormatterConstants() {
        Assertions.assertNotNull(AnsiFormatter.RESET);
        Assertions.assertNotNull(AnsiFormatter.BOLD);
        Assertions.assertNotNull(AnsiFormatter.ITALIC);
        Assertions.assertNotNull(AnsiFormatter.UNDERLINE);
        Assertions.assertNotNull(AnsiFormatter.BLINK);
        Assertions.assertNotNull(AnsiFormatter.INVERSE);
        
        // Check foreground colors
        Assertions.assertNotNull(AnsiFormatter.FG_RED);
        Assertions.assertNotNull(AnsiFormatter.FG_GREEN);
        Assertions.assertNotNull(AnsiFormatter.FG_BLUE);
        Assertions.assertNotNull(AnsiFormatter.FG_CYAN);
        Assertions.assertNotNull(AnsiFormatter.FG_MAGENTA);
        Assertions.assertNotNull(AnsiFormatter.FG_YELLOW);
        Assertions.assertNotNull(AnsiFormatter.FG_WHITE);
        
        // Check bright colors
        Assertions.assertNotNull(AnsiFormatter.BRIGHT_FG_RED);
        Assertions.assertNotNull(AnsiFormatter.BRIGHT_FG_GREEN);
        
        // Check background colors
        Assertions.assertNotNull(AnsiFormatter.BG_RED);
        Assertions.assertNotNull(AnsiFormatter.BG_GREEN);
    }
    
    /**
     * Test format method with null input.
     */
    @Test
    void testFormatWithNull() {
        Assertions.assertEquals("", AnsiFormatter.format(null));
    }
    
    /**
     * Test format method with empty string.
     */
    @Test
    void testFormatWithEmptyString() {
        Assertions.assertEquals("", AnsiFormatter.format(""));
    }
    
    /**
     * Test format method with string without tags.
     */
    @Test
    void testFormatWithNoTags() {
        String input = "This is a test string without any formatting tags";
        String formatted = AnsiFormatter.format(input);
        
        // The string should end with a reset code
        Assertions.assertTrue(formatted.endsWith(AnsiFormatter.RESET));
        
        // Remove the reset and compare
        String contentOnly = formatted.substring(0, formatted.length() - AnsiFormatter.RESET.length());
        Assertions.assertEquals(input, contentOnly);
    }
    
    /**
     * Test format method with color tags.
     */
    @ParameterizedTest
    @CsvSource({
        "RED, This is red text, " + AnsiFormatter.FG_RED + "This is red text" + AnsiFormatter.RESET,
        "GREEN, This is green text, " + AnsiFormatter.FG_GREEN + "This is green text" + AnsiFormatter.RESET,
        "BLUE, This is blue text, " + AnsiFormatter.FG_BLUE + "This is blue text" + AnsiFormatter.RESET
    })
    void testFormatWithColorTags(String color, String text, String expected) {
        String input = "|" + color + "|" + text + "|";
        String formatted = AnsiFormatter.format(input);
        Assertions.assertEquals(expected, formatted);
    }
    
    /**
     * Test format method with style tags.
     */
    @ParameterizedTest
    @CsvSource({
        "BOLD, This is bold text, " + AnsiFormatter.BOLD + "This is bold text" + AnsiFormatter.RESET,
        "B, This is also bold text, " + AnsiFormatter.BOLD + "This is also bold text" + AnsiFormatter.RESET,
        "UNDERLINE, This is underlined text, " + AnsiFormatter.UNDERLINE + "This is underlined text" + AnsiFormatter.RESET,
        "U, This is also underlined text, " + AnsiFormatter.UNDERLINE + "This is also underlined text" + AnsiFormatter.RESET,
        "BLINK, This is blinking text, " + AnsiFormatter.BLINK + "This is blinking text" + AnsiFormatter.RESET
    })
    void testFormatWithStyleTags(String style, String text, String expected) {
        String input = "|" + style + "|" + text + "|";
        String formatted = AnsiFormatter.format(input);
        Assertions.assertEquals(expected, formatted);
    }
    
    /**
     * Test format method with nested tags.
     */
    @Test
    void testFormatWithNestedTags() {
        String input = "|RED|This is red |BOLD|and bold|.|";
        String expected = AnsiFormatter.FG_RED + "This is red " + AnsiFormatter.BOLD + "and bold" + 
                          AnsiFormatter.RESET + "." + AnsiFormatter.RESET;
        String formatted = AnsiFormatter.format(input);
        Assertions.assertEquals(expected, formatted);
    }
    
    /**
     * Test format method with invalid tags.
     */
    @Test
    void testFormatWithInvalidTags() {
        String input = "|INVALID_COLOR|This text has an invalid color tag|";
        String formatted = AnsiFormatter.format(input);
        
        // The invalid tag should remain in the text
        Assertions.assertTrue(formatted.contains("|INVALID_COLOR|"));
        Assertions.assertTrue(formatted.endsWith(AnsiFormatter.RESET));
    }
    
    /**
     * Test stripAnsi method.
     */
    @Test
    void testStripAnsi() throws Exception {
        Method stripAnsi = getPrivateMethod("stripAnsi", String.class);
        
        String input = AnsiFormatter.BOLD + "Bold" + AnsiFormatter.RESET + " " + 
                       AnsiFormatter.FG_RED + "Red" + AnsiFormatter.RESET;
        String result = (String) stripAnsi.invoke(null, input);
        
        Assertions.assertEquals("Bold Red", result);
    }
    
    /**
     * Test createMessageBox method.
     */
    @Test
    void testCreateMessageBox() {
        String title = "Test Box";
        String content = "This is a test message box.\nIt has multiple lines.";
        int width = 40;
        
        String messageBox = AnsiFormatter.createMessageBox(title, content, width);
        
        // Basic validation - should contain the title and content
        Assertions.assertTrue(messageBox.contains(title));
        Assertions.assertTrue(messageBox.contains("This is a test message box."));
        Assertions.assertTrue(messageBox.contains("It has multiple lines."));
        
        // Should have box drawing characters
        Assertions.assertTrue(messageBox.contains(AnsiFormatter.BOX_TL));
        Assertions.assertTrue(messageBox.contains(AnsiFormatter.BOX_TR));
        Assertions.assertTrue(messageBox.contains(AnsiFormatter.BOX_BL));
        Assertions.assertTrue(messageBox.contains(AnsiFormatter.BOX_BR));
    }
    
    /**
     * Test wrapText method with long lines.
     */
    @Test
    void testWrapText() throws Exception {
        Method wrapText = getPrivateMethod("wrapText", String.class, int.class);
        
        String longText = "This is a very long text that should be wrapped at appropriate word boundaries " +
                         "to ensure readability in a terminal with limited width.";
        int width = 30;
        
        String[] result = (String[]) wrapText.invoke(null, longText, width);
        
        // Validate that no line exceeds the specified width
        for (String line : result) {
            Assertions.assertTrue(line.length() <= width, 
                    "Line exceeds max width: '" + line + "' (length: " + line.length() + ")");
        }
        
        // Ensure content is preserved
        String unwrapped = String.join(" ", result);
        Assertions.assertTrue(longText.startsWith(unwrapped.substring(0, 20)));
    }
    
    /**
     * Test wrapText method with ANSI codes.
     */
    @Test
    void testWrapTextWithAnsiCodes() throws Exception {
        Method wrapText = getPrivateMethod("wrapText", String.class, int.class);
        
        String styledText = AnsiFormatter.BOLD + "This is bold" + AnsiFormatter.RESET + " and " +
                           AnsiFormatter.FG_RED + "this is red" + AnsiFormatter.RESET + " text that should wrap.";
        int width = 20;
        
        String[] result = (String[]) wrapText.invoke(null, styledText, width);
        
        // Validate that visible text in each line doesn't exceed width
        Method stripAnsi = getPrivateMethod("stripAnsi", String.class);
        for (String line : result) {
            String stripped = (String) stripAnsi.invoke(null, line);
            Assertions.assertTrue(stripped.length() <= width,
                    "Visible text exceeds max width: '" + stripped + "' (length: " + stripped.length() + ")");
        }
    }
    
    /**
     * Test createBanner method.
     */
    @Test
    void testCreateBanner() {
        String text = "Test Banner";
        String banner = AnsiFormatter.createBanner(text);
        
        // Check that the banner contains the text
        Assertions.assertTrue(banner.contains(text));
        
        // Check for decoration characters
        Assertions.assertTrue(banner.contains("═")); // Border character
        Assertions.assertTrue(banner.contains("★")); // Star character
    }
    
    /**
     * Test isAnsiSupported method.
     */
    @Test
    void testIsAnsiSupported() throws Exception {
        Method isAnsiSupported = getPrivateMethod("isAnsiSupported");
        
        // Can't effectively test the environment variables, but can verify it returns a boolean
        Boolean result = (Boolean) isAnsiSupported.invoke(null);
        Assertions.assertNotNull(result);
    }
}