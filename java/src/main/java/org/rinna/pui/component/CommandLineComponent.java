package org.rinna.pui.component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.rinna.pui.cli.ShellCommandBridge;
import org.rinna.pui.cli.ShellEscapeHandler;

/**
 * Component for input and execution of commands with shell integration.
 * Provides a SUSBS-compliant command input interface with shell features.
 */
public class CommandLineComponent extends Component {
    
    private final ShellCommandBridge commandBridge;
    private final ShellEscapeHandler shellEscapeHandler;
    private final List<String> commandHistory = new ArrayList<>();
    private int historyIndex = -1;
    private int width = 50;
    private String prompt = "rin> ";
    private String currentInput = "";
    private int cursorPosition = 0;
    private Consumer<String> commandExecutionListener;
    private int historyDisplayStart = 0;
    private int historyDisplayLimit = 5;
    private boolean showHistory = false;
    private Map<String, String> shortcuts = new HashMap<>();
    
    /**
     * Create a new command line component with shell integration.
     * 
     * @param commandBridge The command bridge to use for executing commands
     */
    public CommandLineComponent(ShellCommandBridge commandBridge) {
        this.commandBridge = commandBridge;
        this.shellEscapeHandler = new ShellEscapeHandler();
        
        // Set up default shortcuts for shell integration
        shortcuts.put("Ctrl+R", "Search Command History");
        shortcuts.put("Ctrl+E", "Shell Escape");
        shortcuts.put("Ctrl+L", "Clear Screen");
        shortcuts.put("Tab", "Auto-complete");
        shortcuts.put("Esc", "Clear Input");
        shortcuts.put("Up/Down", "Navigate History");
    }
    
    /**
     * Set the prompt text.
     * 
     * @param prompt The prompt text to display
     * @return This instance for method chaining
     */
    public CommandLineComponent setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }
    
    /**
     * Set the width of the component.
     * 
     * @param width The width in characters
     * @return This instance for method chaining
     */
    public CommandLineComponent setWidth(int width) {
        this.width = width;
        return this;
    }
    
    /**
     * Set a listener for command execution.
     * 
     * @param listener Consumer that will be called with the executed command
     * @return This instance for method chaining
     */
    public CommandLineComponent setCommandExecutionListener(Consumer<String> listener) {
        this.commandExecutionListener = listener;
        return this;
    }
    
    /**
     * Render the component.
     */
    @Override
    public void render() {
        // Clear area
        clearArea(x, y, width, 1);
        
        // Determine visible input portion
        String visibleInput = currentInput;
        int promptLength = prompt.length();
        int availableWidth = width - promptLength;
        
        if (currentInput.length() > availableWidth) {
            // Scroll input to keep cursor visible
            int startPos = Math.max(0, cursorPosition - availableWidth + 10);
            if (startPos + availableWidth > currentInput.length()) {
                startPos = Math.max(0, currentInput.length() - availableWidth);
            }
            
            visibleInput = currentInput.substring(startPos);
            if (visibleInput.length() > availableWidth) {
                visibleInput = visibleInput.substring(0, availableWidth);
            }
        }
        
        // Draw prompt and input
        moveCursor(x, y);
        print(prompt);
        print(visibleInput);
        
        // Position cursor
        int cursorX = x + promptLength + (cursorPosition - (currentInput.length() - visibleInput.length()));
        if (cursorX >= x + width) {
            cursorX = x + width - 1;
        }
        moveCursor(cursorX, y);
        
        // Render history if showing
        if (showHistory) {
            renderHistory();
        }
    }
    
    /**
     * Render command history above the command line.
     */
    private void renderHistory() {
        int historyY = y - historyDisplayLimit;
        if (historyY < 0) {
            historyY = 0;
        }
        
        clearArea(x, historyY, width, historyDisplayLimit);
        drawBox(x, historyY, width, historyDisplayLimit, "History");
        
        int count = Math.min(historyDisplayLimit - 2, commandHistory.size() - historyDisplayStart);
        for (int i = 0; i < count; i++) {
            int historyIndex = historyDisplayStart + i;
            if (historyIndex < commandHistory.size()) {
                String command = commandHistory.get(historyIndex);
                if (command.length() > width - 4) {
                    command = command.substring(0, width - 7) + "...";
                }
                
                moveCursor(x + 1, historyY + i + 1);
                print(String.format("%-" + (width - 2) + "s", command));
            }
        }
    }
    
    /**
     * Handle key input for command entry and history navigation.
     * 
     * @param key The key that was pressed
     * @return true if the key was handled, false otherwise
     */
    @Override
    public boolean handleKeyInput(int key) {
        switch (key) {
            case 10: // Enter
                if (!currentInput.trim().isEmpty()) {
                    executeCommand(currentInput);
                    currentInput = "";
                    cursorPosition = 0;
                    historyIndex = -1;
                }
                return true;
                
            case 27: // Escape
                currentInput = "";
                cursorPosition = 0;
                historyIndex = -1;
                showHistory = false;
                return true;
                
            case 258: // Down Arrow
                navigateHistoryDown();
                return true;
                
            case 259: // Up Arrow
                navigateHistoryUp();
                return true;
                
            case 260: // Left Arrow
                if (cursorPosition > 0) {
                    cursorPosition--;
                }
                return true;
                
            case 261: // Right Arrow
                if (cursorPosition < currentInput.length()) {
                    cursorPosition++;
                }
                return true;
                
            case 263: // Backspace
                if (cursorPosition > 0) {
                    currentInput = currentInput.substring(0, cursorPosition - 1) +
                            currentInput.substring(cursorPosition);
                    cursorPosition--;
                }
                return true;
                
            case 330: // Delete
                if (cursorPosition < currentInput.length()) {
                    currentInput = currentInput.substring(0, cursorPosition) +
                            currentInput.substring(cursorPosition + 1);
                }
                return true;
                
            case 1: // Ctrl+A (beginning of line)
                cursorPosition = 0;
                return true;
                
            case 5: // Ctrl+E (end of line)
                cursorPosition = currentInput.length();
                return true;
                
            case 12: // Ctrl+L (clear screen)
                // Clear screen would be handled by parent component
                return false;
                
            case 18: // Ctrl+R (search history)
                toggleHistoryDisplay();
                return true;
                
            case 5: // Ctrl+E (shell escape)
                shellEscape();
                return true;
                
            case 9: // Tab (auto-complete)
                // Auto-complete would be handled separately
                return true;
                
            default:
                // Normal character input
                if (key >= 32 && key <= 126) { // printable ASCII
                    currentInput = currentInput.substring(0, cursorPosition) +
                            (char) key +
                            currentInput.substring(cursorPosition);
                    cursorPosition++;
                    return true;
                }
                return false;
        }
    }
    
    /**
     * Navigate up in command history.
     */
    private void navigateHistoryUp() {
        if (commandHistory.isEmpty()) {
            return;
        }
        
        if (historyIndex < commandHistory.size() - 1) {
            historyIndex++;
            currentInput = commandHistory.get(commandHistory.size() - 1 - historyIndex);
            cursorPosition = currentInput.length();
        }
    }
    
    /**
     * Navigate down in command history.
     */
    private void navigateHistoryDown() {
        if (historyIndex > 0) {
            historyIndex--;
            currentInput = commandHistory.get(commandHistory.size() - 1 - historyIndex);
            cursorPosition = currentInput.length();
        } else if (historyIndex == 0) {
            historyIndex = -1;
            currentInput = "";
            cursorPosition = 0;
        }
    }
    
    /**
     * Toggle history display.
     */
    private void toggleHistoryDisplay() {
        showHistory = !showHistory;
        historyDisplayStart = 0;
    }
    
    /**
     * Execute a command.
     * 
     * @param command The command to execute
     */
    private void executeCommand(String command) {
        // Special commands starting with ! for shell integrations
        if (command.startsWith("!")) {
            executeShellCommand(command.substring(1));
            return;
        }
        
        // Add to history
        commandHistory.add(command);
        
        // Notify listener
        if (commandExecutionListener != null) {
            commandExecutionListener.accept(command);
        }
        
        // For SUSBS compliance, we convert the UI command to a shell command
        // and execute it through the shell bridge
        // This is a simplified example; a real implementation would parse the command
        if (command.equals("help")) {
            // Display help with shell shortcuts
            System.out.println("\nShell Integration Shortcuts:");
            shortcuts.forEach((key, desc) -> System.out.println(key + " - " + desc));
            System.out.println("\n! prefix executes shell commands directly");
            System.out.println("!shell to drop to shell");
            return;
        }
        
        // This is where we would parse the command and call the appropriate
        // service through the command bridge
        System.out.println("Executing: " + command);
    }
    
    /**
     * Execute a shell command directly.
     * 
     * @param command The shell command to execute
     */
    private void executeShellCommand(String command) {
        if (command.equals("shell")) {
            shellEscape();
            return;
        }
        
        // Execute the shell command
        String output = shellEscapeHandler.executeCommand(command);
        System.out.println(output);
        
        // Add to history
        commandHistory.add("!" + command);
    }
    
    /**
     * Drop to a shell session.
     */
    private void shellEscape() {
        List<String> shellCommands = shellEscapeHandler.escapeToShell();
        
        // Add executed shell commands to history
        for (String cmd : shellCommands) {
            commandHistory.add("!" + cmd);
        }
    }
    
    /**
     * Get the command history.
     * 
     * @return List of command history
     */
    public List<String> getCommandHistory() {
        return new ArrayList<>(commandHistory);
    }
    
    /**
     * Clear the command history.
     * 
     * @return This instance for method chaining
     */
    public CommandLineComponent clearHistory() {
        commandHistory.clear();
        historyIndex = -1;
        return this;
    }
    
    /**
     * Add a command to execute.
     * 
     * @param command The command to execute
     * @return This instance for method chaining
     */
    public CommandLineComponent executeCommand(String command, boolean addToHistory) {
        if (addToHistory) {
            commandHistory.add(command);
        }
        
        // Notify listener
        if (commandExecutionListener != null) {
            commandExecutionListener.accept(command);
        }
        
        return this;
    }
}