package org.rinna.pui.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Handler for shell escape functionality in PUI.
 * Allows users to drop to a shell for advanced operations,
 * implementing SUSBS shell escape requirements.
 */
public class ShellEscapeHandler {
    
    private final String shellPath;
    private Consumer<List<String>> commandHistoryConsumer;
    
    /**
     * Create a new shell escape handler with the default shell.
     */
    public ShellEscapeHandler() {
        this("/bin/bash");
    }
    
    /**
     * Create a new shell escape handler with a specific shell.
     * 
     * @param shellPath Path to the shell executable
     */
    public ShellEscapeHandler(String shellPath) {
        this.shellPath = shellPath;
    }
    
    /**
     * Set a consumer for command history after returning from shell.
     * 
     * @param consumer Consumer that will be called with the shell command history
     * @return This instance for method chaining
     */
    public ShellEscapeHandler setCommandHistoryConsumer(Consumer<List<String>> consumer) {
        this.commandHistoryConsumer = consumer;
        return this;
    }
    
    /**
     * Drop to a shell session.
     * 
     * @return List of commands executed in the shell
     */
    public List<String> escapeToShell() {
        List<String> commandHistory = new ArrayList<>();
        
        // Save terminal state
        System.out.print("\033[?1049h");  // Save screen
        System.out.print("\033[?25h");    // Show cursor
        System.out.print("\033[0m");      // Reset attributes
        System.out.flush();
        
        try {
            // Create history file for tracking commands
            String historyFile = System.getProperty("java.io.tmpdir") + "/rinna_shell_history_" + System.currentTimeMillis();
            
            // Build shell command with history tracking
            ProcessBuilder pb = new ProcessBuilder(
                shellPath, 
                "-c", 
                "echo 'Rinna Shell Escape (type \"exit\" to return to Rinna)'; " +
                "HISTFILE=" + historyFile + "; " +
                "HISTFILESIZE=1000; " +
                "set -o history; " +
                shellPath + "; " +
                "echo 'Returning to Rinna...'"
            );
            
            // Inherit IO for interactive use
            pb.inheritIO();
            
            // Start process and wait for it to complete
            Process process = pb.start();
            int exitCode = process.waitFor();
            
            // Read command history from history file
            try {
                commandHistory = readHistoryFile(historyFile);
            } catch (IOException e) {
                System.err.println("Error reading shell history: " + e.getMessage());
            }
            
            // Notify consumer if set
            if (commandHistoryConsumer != null) {
                commandHistoryConsumer.accept(commandHistory);
            }
            
            if (exitCode != 0) {
                System.err.println("Shell exited with code " + exitCode);
            }
        } catch (IOException | InterruptedException e) {
            System.err.println("Error during shell escape: " + e.getMessage());
        } finally {
            // Restore terminal state
            System.out.print("\033[?1049l");  // Restore screen
            System.out.flush();
        }
        
        return commandHistory;
    }
    
    /**
     * Execute a single shell command.
     * 
     * @param command The command to execute
     * @return The command output
     */
    public String executeCommand(String command) {
        StringBuilder output = new StringBuilder();
        
        try {
            Process process = new ProcessBuilder(shellPath, "-c", command)
                .redirectErrorStream(true)
                .start();
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            process.waitFor();
            
            // Add to command history consumer if set
            if (commandHistoryConsumer != null) {
                List<String> history = new ArrayList<>();
                history.add(command);
                commandHistoryConsumer.accept(history);
            }
            
        } catch (IOException | InterruptedException e) {
            return "Error executing command: " + e.getMessage();
        }
        
        return output.toString();
    }
    
    /**
     * Read command history from a history file.
     * 
     * @param historyFile Path to the history file
     * @return List of commands from the history
     * @throws IOException If an I/O error occurs
     */
    private List<String> readHistoryFile(String historyFile) throws IOException {
        List<String> history = new ArrayList<>();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new java.io.FileInputStream(historyFile)))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                // Skip empty lines and lines that start with # (history timestamps)
                if (!line.isEmpty() && !line.startsWith("#")) {
                    history.add(line);
                }
            }
        } catch (java.io.FileNotFoundException e) {
            // History file might not exist, which is fine
            System.err.println("History file not found: " + historyFile);
        }
        
        return history;
    }
}