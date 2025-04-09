package org.rinna.pui.component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Component that displays and manages command history,
 * providing a shell-like history interface for SUSBS compliance.
 */
public class CommandHistory extends Component {
    
    private final List<String> history = new ArrayList<>();
    private int width = 50;
    private int height = 10;
    private int selectedIndex = -1;
    private int scrollOffset = 0;
    private Consumer<String> commandSelectedListener;
    
    /**
     * Create a new command history component.
     */
    public CommandHistory() {
        // Set default attributes
        super();
    }
    
    /**
     * Add a command to the history.
     * 
     * @param command The command to add
     * @return This instance for method chaining
     */
    public CommandHistory addCommand(String command) {
        history.add(command);
        
        // Auto-scroll to the bottom
        if (history.size() > height) {
            scrollOffset = history.size() - height;
        }
        
        return this;
    }
    
    /**
     * Add multiple commands to the history.
     * 
     * @param commands List of commands to add
     * @return This instance for method chaining
     */
    public CommandHistory addCommands(List<String> commands) {
        history.addAll(commands);
        
        // Auto-scroll to the bottom
        if (history.size() > height) {
            scrollOffset = history.size() - height;
        }
        
        return this;
    }
    
    /**
     * Clear the command history.
     * 
     * @return This instance for method chaining
     */
    public CommandHistory clearHistory() {
        history.clear();
        selectedIndex = -1;
        scrollOffset = 0;
        return this;
    }
    
    /**
     * Get the full command history.
     * 
     * @return List of all commands in the history
     */
    public List<String> getHistory() {
        return new ArrayList<>(history);
    }
    
    /**
     * Set the dimensions of the component.
     * 
     * @param width Width in characters
     * @param height Height in lines
     * @return This instance for method chaining
     */
    public CommandHistory setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }
    
    /**
     * Set a listener for when a command is selected from the history.
     * 
     * @param listener Consumer that will be called with the selected command
     * @return This instance for method chaining
     */
    public CommandHistory setCommandSelectedListener(Consumer<String> listener) {
        this.commandSelectedListener = listener;
        return this;
    }
    
    /**
     * Handle key input for navigating and selecting commands.
     * 
     * @param key The key that was pressed
     * @return true if the key was handled, false otherwise
     */
    @Override
    public boolean handleKeyInput(int key) {
        switch (key) {
            case 'j': // down
            case 258: // down arrow
                selectNext();
                return true;
                
            case 'k': // up
            case 259: // up arrow
                selectPrevious();
                return true;
                
            case 10: // enter
                if (selectedIndex >= 0 && selectedIndex < history.size()) {
                    if (commandSelectedListener != null) {
                        commandSelectedListener.accept(history.get(selectedIndex));
                    }
                }
                return true;
                
            case 'g': // top
                scrollToTop();
                return true;
                
            case 'G': // bottom
                scrollToBottom();
                return true;
                
            default:
                return false;
        }
    }
    
    /**
     * Render the component.
     */
    @Override
    public void render() {
        // Clear area
        clearArea(x, y, width, height);
        
        // Draw border
        drawBox(x, y, width, height, "Command History");
        
        // Calculate visible range
        int visibleCount = Math.min(history.size(), height - 2);
        int startIndex = Math.min(scrollOffset, Math.max(0, history.size() - visibleCount));
        
        // Draw history items
        for (int i = 0; i < visibleCount; i++) {
            int historyIndex = startIndex + i;
            if (historyIndex < history.size()) {
                String command = history.get(historyIndex);
                
                // Truncate if too long
                if (command.length() > width - 4) {
                    command = command.substring(0, width - 7) + "...";
                }
                
                // Highlight selected
                if (historyIndex == selectedIndex) {
                    setInverse(true);
                    moveCursor(x + 1, y + i + 1);
                    print(String.format("%-" + (width - 2) + "s", command));
                    setInverse(false);
                } else {
                    moveCursor(x + 1, y + i + 1);
                    print(String.format("%-" + (width - 2) + "s", command));
                }
            }
        }
        
        // Draw scroll indicators
        if (scrollOffset > 0) {
            moveCursor(x + width / 2, y);
            print("▲");
        }
        
        if (scrollOffset + visibleCount < history.size()) {
            moveCursor(x + width / 2, y + height - 1);
            print("▼");
        }
    }
    
    /**
     * Select the next item in the history.
     */
    private void selectNext() {
        if (history.isEmpty()) {
            return;
        }
        
        if (selectedIndex < history.size() - 1) {
            selectedIndex++;
            
            // Auto-scroll if needed
            if (selectedIndex >= scrollOffset + height - 2) {
                scrollOffset = selectedIndex - height + 3;
            }
        }
    }
    
    /**
     * Select the previous item in the history.
     */
    private void selectPrevious() {
        if (history.isEmpty()) {
            return;
        }
        
        if (selectedIndex > 0) {
            selectedIndex--;
            
            // Auto-scroll if needed
            if (selectedIndex < scrollOffset) {
                scrollOffset = selectedIndex;
            }
        } else if (selectedIndex == -1 && !history.isEmpty()) {
            selectedIndex = 0;
        }
    }
    
    /**
     * Scroll to the top of the history.
     */
    private void scrollToTop() {
        scrollOffset = 0;
        selectedIndex = history.isEmpty() ? -1 : 0;
    }
    
    /**
     * Scroll to the bottom of the history.
     */
    private void scrollToBottom() {
        if (history.isEmpty()) {
            return;
        }
        
        scrollOffset = Math.max(0, history.size() - height + 2);
        selectedIndex = history.size() - 1;
    }
}