/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.pui.input;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles keyboard input from the terminal.
 * Provides methods for reading and interpreting key presses.
 */
public class KeyHandler {
    
    // Key code constants
    public static final int KEY_ESCAPE = 27;
    public static final int KEY_ENTER = 13;
    public static final int KEY_BACKSPACE = 127;
    public static final int KEY_TAB = 9;
    
    public static final int KEY_UP = 256 + 65;     // User-defined key code for Up arrow
    public static final int KEY_DOWN = 256 + 66;   // User-defined key code for Down arrow
    public static final int KEY_RIGHT = 256 + 67;  // User-defined key code for Right arrow
    public static final int KEY_LEFT = 256 + 68;   // User-defined key code for Left arrow
    
    public static final int KEY_HOME = 256 + 72;   // User-defined key code for Home
    public static final int KEY_END = 256 + 70;    // User-defined key code for End
    
    public static final int KEY_PGUP = 256 + 53;   // User-defined key code for Page Up
    public static final int KEY_PGDN = 256 + 54;   // User-defined key code for Page Down
    
    public static final int KEY_F1 = 256 + 80;     // User-defined key code for F1
    public static final int KEY_F2 = 256 + 81;     // User-defined key code for F2
    public static final int KEY_F3 = 256 + 82;     // User-defined key code for F3
    public static final int KEY_F4 = 256 + 83;     // User-defined key code for F4
    
    private InputStream inputStream;
    private boolean rawMode;
    private final Map<List<Integer>, Integer> escapeSequences;
    private final List<Integer> buffer;
    
    /**
     * Creates a new key handler with default settings.
     */
    public KeyHandler() {
        this.inputStream = System.in;
        this.rawMode = false;
        this.escapeSequences = new HashMap<>();
        this.buffer = new ArrayList<>();
        
        initializeEscapeSequences();
    }
    
    /**
     * Initializes the escape sequence map with common terminal key codes.
     */
    private void initializeEscapeSequences() {
        // Arrow keys
        registerEscapeSequence(new int[]{27, 91, 65}, KEY_UP);       // Up arrow
        registerEscapeSequence(new int[]{27, 91, 66}, KEY_DOWN);     // Down arrow
        registerEscapeSequence(new int[]{27, 91, 67}, KEY_RIGHT);    // Right arrow
        registerEscapeSequence(new int[]{27, 91, 68}, KEY_LEFT);     // Left arrow
        
        // Home and End
        registerEscapeSequence(new int[]{27, 91, 72}, KEY_HOME);     // Home
        registerEscapeSequence(new int[]{27, 91, 70}, KEY_END);      // End
        
        // Alternative Home and End sequences
        registerEscapeSequence(new int[]{27, 91, 49, 126}, KEY_HOME);
        registerEscapeSequence(new int[]{27, 91, 52, 126}, KEY_END);
        
        // Page Up and Page Down
        registerEscapeSequence(new int[]{27, 91, 53, 126}, KEY_PGUP);
        registerEscapeSequence(new int[]{27, 91, 54, 126}, KEY_PGDN);
        
        // Function keys
        registerEscapeSequence(new int[]{27, 79, 80}, KEY_F1);
        registerEscapeSequence(new int[]{27, 79, 81}, KEY_F2);
        registerEscapeSequence(new int[]{27, 79, 82}, KEY_F3);
        registerEscapeSequence(new int[]{27, 79, 83}, KEY_F4);
        
        // Alternative function key sequences
        registerEscapeSequence(new int[]{27, 91, 49, 49, 126}, KEY_F1);
        registerEscapeSequence(new int[]{27, 91, 49, 50, 126}, KEY_F2);
        registerEscapeSequence(new int[]{27, 91, 49, 51, 126}, KEY_F3);
        registerEscapeSequence(new int[]{27, 91, 49, 52, 126}, KEY_F4);
    }
    
    /**
     * Registers an escape sequence for a special key.
     * 
     * @param sequence the escape sequence
     * @param keyCode the key code to return when this sequence is detected
     */
    private void registerEscapeSequence(int[] sequence, int keyCode) {
        List<Integer> sequenceList = new ArrayList<>(sequence.length);
        for (int code : sequence) {
            sequenceList.add(code);
        }
        escapeSequences.put(sequenceList, keyCode);
    }
    
    /**
     * Initializes the terminal for key handling.
     * This puts the terminal in raw mode.
     * 
     * @throws IOException if an I/O error occurs
     */
    public void initialize() throws IOException {
        // Put terminal in raw mode
        try {
            String[] cmd = {"/bin/sh", "-c", "stty raw -echo </dev/tty"};
            Runtime.getRuntime().exec(cmd).waitFor();
            rawMode = true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Failed to put terminal in raw mode", e);
        }
    }
    
    /**
     * Restores the terminal to its original state.
     * This exits raw mode.
     * 
     * @throws IOException if an I/O error occurs
     */
    public void cleanup() throws IOException {
        if (rawMode) {
            try {
                String[] cmd = {"/bin/sh", "-c", "stty sane </dev/tty"};
                Runtime.getRuntime().exec(cmd).waitFor();
                rawMode = false;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Failed to restore terminal mode", e);
            }
        }
    }
    
    /**
     * Reads a key from the terminal.
     * 
     * @return the key code, or -1 if no key is available
     * @throws IOException if an I/O error occurs
     */
    public int readKey() throws IOException {
        if (!rawMode) {
            throw new IllegalStateException("Terminal is not in raw mode. Call initialize() first.");
        }
        
        if (inputStream.available() == 0) {
            return -1;
        }
        
        int key = inputStream.read();
        
        // Check for escape sequences
        if (key == 27) { // ESC
            buffer.clear();
            buffer.add(key);
            
            // Try to read the rest of the escape sequence
            long startTime = System.currentTimeMillis();
            while (System.currentTimeMillis() - startTime < 50) { // 50ms timeout
                if (inputStream.available() > 0) {
                    int nextChar = inputStream.read();
                    buffer.add(nextChar);
                    
                    // Check if we have a complete escape sequence
                    for (Map.Entry<List<Integer>, Integer> entry : escapeSequences.entrySet()) {
                        if (buffer.equals(entry.getKey())) {
                            buffer.clear();
                            return entry.getValue();
                        }
                    }
                    
                    // Keep reading more characters if buffer matches a prefix of any escape sequence
                    boolean isPrefix = false;
                    for (List<Integer> sequence : escapeSequences.keySet()) {
                        if (sequence.size() >= buffer.size() && buffer.equals(sequence.subList(0, buffer.size()))) {
                            isPrefix = true;
                            break;
                        }
                    }
                    
                    if (!isPrefix) {
                        break;
                    }
                }
                
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            // If we couldn't match an escape sequence, return the first character
            if (!buffer.isEmpty()) {
                int result = buffer.get(0);
                buffer.clear();
                return result;
            }
        }
        
        return key;
    }
    
    /**
     * Checks if a key is available to read.
     * 
     * @return true if a key is available, false otherwise
     * @throws IOException if an I/O error occurs
     */
    public boolean isKeyAvailable() throws IOException {
        return inputStream.available() > 0;
    }
    
    /**
     * Sets the input stream to read from.
     * This is primarily used for testing.
     * 
     * @param inputStream the input stream
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }
}