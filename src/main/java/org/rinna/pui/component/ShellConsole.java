package org.rinna.pui.component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.rinna.pui.cli.ShellIntegrationLayer;

/**
 * A PUI component that provides a full shell console experience.
 * Integrates directly with the ShellIntegrationLayer to provide
 * a SUSBS-compliant shell interface within the PUI.
 */
public class ShellConsole extends Component {
    
    private final ShellIntegrationLayer shellIntegration;
    private final List<String> outputLines = new ArrayList<>();
    private final List<String> inputLines = new ArrayList<>();
    
    private int width = 80;
    private int height = 24;
    private int scrollOffset = 0;
    private String currentInput = "";
    private int cursorPosition = 0;
    private String prompt = "$ ";
    private boolean isProcessing = false;
    private boolean showCompletions = false;
    private List<String> completions = new ArrayList<>();
    private int selectedCompletionIndex = -1;
    
    /**
     * Create a new shell console with the default shell integration layer.
     */
    public ShellConsole() {
        this(new ShellIntegrationLayer());
    }
    
    /**
     * Create a new shell console with a custom shell integration layer.
     * 
     * @param shellIntegration The shell integration layer to use
     */
    public ShellConsole(ShellIntegrationLayer shellIntegration) {
        this.shellIntegration = shellIntegration;
        
        // Set up command execution listener
        shellIntegration.addCommandExecutionListener(command -> {
            // Just show the command in the output, actual execution
            // happens in the handleEnterKey method
            if (!outputLines.isEmpty()) {
                outputLines.add("");
            }
            outputLines.add(prompt + command);
        });
        
        // Add initial welcome message
        outputLines.add("Rinna Shell Console - SUSBS Compliant");
        outputLines.add("Type 'help' for available commands, or '!help' for system shell help");
        outputLines.add("");
        outputLines.add(prompt);
        
        // Load command history
        inputLines.addAll(shellIntegration.getCommandHistory());
    }
    
    /**
     * Set the dimensions of the console.
     * 
     * @param width Width in characters
     * @param height Height in lines
     * @return This instance for method chaining
     */
    public ShellConsole setDimensions(int width, int height) {
        this.width = width;
        this.height = height;
        return this;
    }
    
    /**
     * Set the prompt displayed before the input line.
     * 
     * @param prompt The prompt string
     * @return This instance for method chaining
     */
    public ShellConsole setPrompt(String prompt) {
        this.prompt = prompt;
        return this;
    }
    
    /**
     * Handle key input for command entry and navigation.
     * 
     * @param key The key that was pressed
     * @return true if the key was handled, false otherwise
     */
    @Override
    public boolean handleKeyInput(int key) {
        if (isProcessing) {
            return true; // Block input during processing
        }
        
        switch (key) {
            case 10: // Enter
                handleEnterKey();
                return true;
                
            case 27: // Escape
                if (showCompletions) {
                    showCompletions = false;
                    selectedCompletionIndex = -1;
                } else {
                    currentInput = "";
                    cursorPosition = 0;
                }
                return true;
                
            case 9: // Tab
                handleTabKey();
                return true;
                
            case 258: // Down Arrow
                handleDownArrow();
                return true;
                
            case 259: // Up Arrow
                handleUpArrow();
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
                
            case 262: // Home
                cursorPosition = 0;
                return true;
                
            case 358: // End
                cursorPosition = currentInput.length();
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
                outputLines.clear();
                outputLines.add(prompt);
                scrollOffset = 0;
                return true;
                
            case 'v' & 0x1f: // Ctrl+V (paste)
                // Not implemented, would require system clipboard access
                return true;
                
            default:
                // Normal character input
                if (key >= 32 && key <= 126) { // printable ASCII
                    currentInput = currentInput.substring(0, cursorPosition) +
                            (char) key +
                            currentInput.substring(cursorPosition);
                    cursorPosition++;
                    
                    // Hide completions when typing
                    showCompletions = false;
                    selectedCompletionIndex = -1;
                    return true;
                }
                return false;
        }
    }
    
    /**
     * Render the console component.
     */
    @Override
    public void render() {
        // Clear area
        clearArea(x, y, width, height);
        
        // Draw border
        drawBox(x, y, width, height, "Shell Console");
        
        // Calculate visible area
        int contentHeight = height - 2; // Account for border
        int contentWidth = width - 2;
        
        // Ensure scroll offset is valid
        if (outputLines.size() > contentHeight) {
            int maxOffset = outputLines.size() - contentHeight;
            if (scrollOffset > maxOffset) {
                scrollOffset = maxOffset;
            }
        } else {
            scrollOffset = 0;
        }
        
        // Draw output lines
        int visibleLines = Math.min(contentHeight, outputLines.size() - scrollOffset);
        for (int i = 0; i < visibleLines; i++) {
            int lineIndex = scrollOffset + i;
            String line = outputLines.get(lineIndex);
            
            // Wrap lines that are too long
            if (line.length() > contentWidth) {
                // Simple line wrapping for demo
                String visiblePart = line.substring(0, contentWidth - 3) + "...";
                moveCursor(x + 1, y + 1 + i);
                print(visiblePart);
            } else {
                moveCursor(x + 1, y + 1 + i);
                print(line);
            }
        }
        
        // Draw input line if not showing completions
        if (!showCompletions) {
            // If there are output lines, the last one should be the prompt
            // Draw the current input after the prompt
            if (!outputLines.isEmpty()) {
                int lastLineIndex = outputLines.size() - 1;
                if (lastLineIndex >= scrollOffset && lastLineIndex < scrollOffset + contentHeight) {
                    int lineY = y + 1 + (lastLineIndex - scrollOffset);
                    int promptWidth = prompt.length();
                    
                    // If the last line is just the prompt, append input to it
                    if (outputLines.get(lastLineIndex).equals(prompt)) {
                        moveCursor(x + 1 + promptWidth, lineY);
                        print(currentInput);
                        
                        // Position cursor
                        moveCursor(x + 1 + promptWidth + cursorPosition, lineY);
                    }
                }
            }
        } else {
            // Draw completions
            int completionY = y + height - 6; // Near the bottom
            int completionX = x + 2;
            
            clearArea(completionX, completionY, width - 4, 5);
            drawBox(completionX, completionY, width - 4, 5, "Completions");
            
            int visibleCompletions = Math.min(3, completions.size());
            for (int i = 0; i < visibleCompletions; i++) {
                if (i < completions.size()) {
                    String completion = completions.get(i);
                    
                    if (i == selectedCompletionIndex) {
                        setInverse(true);
                    }
                    
                    moveCursor(completionX + 1, completionY + 1 + i);
                    print(String.format("%-" + (width - 6) + "s", completion));
                    
                    if (i == selectedCompletionIndex) {
                        setInverse(false);
                    }
                }
            }
        }
        
        // If processing, show indicator
        if (isProcessing) {
            moveCursor(x + width - 12, y);
            print("[Processing]");
        }
    }
    
    /**
     * Handle the Enter key press.
     */
    private void handleEnterKey() {
        if (currentInput.isEmpty()) {
            // Just add a newline with prompt
            outputLines.add("");
            outputLines.add(prompt);
            return;
        }
        
        // Add to input history
        inputLines.add(currentInput);
        
        // Remember the input
        final String input = currentInput;
        
        // Clear current input
        currentInput = "";
        cursorPosition = 0;
        
        // Set processing flag
        isProcessing = true;
        
        // Execute command asynchronously
        CompletableFuture.supplyAsync(() -> {
            if (input.equals("clear")) {
                // Special case for clear
                outputLines.clear();
                return "";
            } else if (input.equals("help")) {
                // Special case for help
                return getHelpText();
            } else {
                // Normal command execution
                return shellIntegration.executeCommand(input);
            }
        }).thenAccept(result -> {
            // Add result to output
            if (!result.isEmpty()) {
                for (String line : result.split("\n")) {
                    outputLines.add(line);
                }
            }
            
            // Add new prompt
            outputLines.add("");
            outputLines.add(prompt);
            
            // Auto-scroll to bottom
            scrollOffset = Math.max(0, outputLines.size() - (height - 2));
            
            // Clear processing flag
            isProcessing = false;
        });
    }
    
    /**
     * Handle the Tab key press for auto-completion.
     */
    private void handleTabKey() {
        if (showCompletions && selectedCompletionIndex >= 0) {
            // Apply selected completion
            if (selectedCompletionIndex < completions.size()) {
                currentInput = completions.get(selectedCompletionIndex);
                cursorPosition = currentInput.length();
                showCompletions = false;
                selectedCompletionIndex = -1;
            }
        } else {
            // Show completions
            completions = shellIntegration.getCompletionSuggestions(currentInput);
            if (!completions.isEmpty()) {
                showCompletions = true;
                selectedCompletionIndex = 0;
            }
        }
    }
    
    /**
     * Handle the Up Arrow key press for history navigation.
     */
    private void handleUpArrow() {
        if (showCompletions) {
            // Navigate completions
            if (selectedCompletionIndex > 0) {
                selectedCompletionIndex--;
            }
        } else {
            // Navigate history
            if (!inputLines.isEmpty()) {
                int index = inputLines.indexOf(currentInput);
                if (index > 0) {
                    currentInput = inputLines.get(index - 1);
                } else if (index == -1) {
                    // Not in history, go to most recent
                    currentInput = inputLines.get(inputLines.size() - 1);
                }
                cursorPosition = currentInput.length();
            }
        }
    }
    
    /**
     * Handle the Down Arrow key press for history navigation.
     */
    private void handleDownArrow() {
        if (showCompletions) {
            // Navigate completions
            if (selectedCompletionIndex < completions.size() - 1) {
                selectedCompletionIndex++;
            }
        } else {
            // Navigate history
            if (!inputLines.isEmpty()) {
                int index = inputLines.indexOf(currentInput);
                if (index >= 0 && index < inputLines.size() - 1) {
                    currentInput = inputLines.get(index + 1);
                    cursorPosition = currentInput.length();
                } else if (index == inputLines.size() - 1) {
                    // At the end of history, clear input
                    currentInput = "";
                    cursorPosition = 0;
                }
            }
        }
    }
    
    /**
     * Get the help text for the shell console.
     * 
     * @return The help text
     */
    private String getHelpText() {
        StringBuilder help = new StringBuilder();
        help.append("Rinna Shell Console - SUSBS Compliant\n");
        help.append("-----------------------------------\n");
        help.append("\n");
        help.append("Commands:\n");
        help.append("  [command]      - Execute a Rinna command\n");
        help.append("  ![command]     - Execute a system shell command\n");
        help.append("  !shell         - Drop to interactive shell\n");
        help.append("  help           - Show this help\n");
        help.append("  clear          - Clear the console\n");
        help.append("\n");
        help.append("Keyboard Shortcuts:\n");
        help.append("  Tab            - Auto-complete command\n");
        help.append("  Up/Down        - Navigate command history\n");
        help.append("  Ctrl+A         - Move cursor to beginning of line\n");
        help.append("  Ctrl+E         - Move cursor to end of line\n");
        help.append("  Ctrl+L         - Clear screen\n");
        
        return help.toString();
    }
    
    /**
     * Get the current command input.
     * 
     * @return The current input
     */
    public String getCurrentInput() {
        return currentInput;
    }
    
    /**
     * Set the current command input.
     * 
     * @param input The input to set
     * @return This instance for method chaining
     */
    public ShellConsole setCurrentInput(String input) {
        this.currentInput = input != null ? input : "";
        this.cursorPosition = this.currentInput.length();
        return this;
    }
    
    /**
     * Add output to the console.
     * 
     * @param text The text to add
     * @return This instance for method chaining
     */
    public ShellConsole addOutput(String text) {
        for (String line : text.split("\n")) {
            outputLines.add(line);
        }
        
        // Auto-scroll to bottom
        scrollOffset = Math.max(0, outputLines.size() - (height - 2));
        
        return this;
    }
    
    /**
     * Clear the console output.
     * 
     * @return This instance for method chaining
     */
    public ShellConsole clearOutput() {
        outputLines.clear();
        outputLines.add(prompt);
        scrollOffset = 0;
        return this;
    }
    
    /**
     * Get the shell integration layer used by this console.
     * 
     * @return The shell integration layer
     */
    public ShellIntegrationLayer getShellIntegration() {
        return shellIntegration;
    }
}