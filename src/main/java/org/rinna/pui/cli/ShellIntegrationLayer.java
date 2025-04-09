package org.rinna.pui.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Comprehensive shell integration layer for PUI operations.
 * Provides a unified API for all shell-related functionality including:
 * - Command execution and tracking
 * - Environment variable management
 * - Shell script generation
 * - Completion and suggestion
 * - Process management
 * - Asynchronous execution
 * 
 * This layer handles the complexity of shell integration, allowing
 * PUI components to interact with the shell in a consistent way.
 */
public class ShellIntegrationLayer {
    
    private final ShellCommandBridge commandBridge;
    private final ShellEscapeHandler shellEscapeHandler;
    private final CommandGenerator commandGenerator;
    private final CommandMirror commandMirror;
    private final List<String> commandHistory = new ArrayList<>();
    private final Map<String, String> environmentVariables = new HashMap<>();
    private final List<Consumer<String>> commandExecutionListeners = new ArrayList<>();
    private final List<Consumer<Map<String, String>>> environmentChangeListeners = new ArrayList<>();
    private final Map<String, Process> runningProcesses = new HashMap<>();
    private static final String DEFAULT_COMMAND_PREFIX = "rin";
    
    /**
     * Create a new shell integration layer with default components.
     */
    public ShellIntegrationLayer() {
        this.commandBridge = new ShellCommandBridge();
        this.shellEscapeHandler = new ShellEscapeHandler();
        this.commandMirror = new CommandMirror();
        this.commandGenerator = new CommandGenerator(commandBridge);
        
        // Initialize with current environment variables
        loadEnvironmentVariables();
        
        // Set up shell escape handler to sync command history
        shellEscapeHandler.setCommandHistoryConsumer(commands -> {
            for (String cmd : commands) {
                addToCommandHistory("!" + cmd);
            }
        });
    }
    
    /**
     * Create a shell integration layer with custom components.
     * 
     * @param commandBridge The command bridge to use
     * @param shellEscapeHandler The shell escape handler to use
     * @param commandMirror The command mirror to use
     * @param commandGenerator The command generator to use
     */
    public ShellIntegrationLayer(ShellCommandBridge commandBridge, 
                                ShellEscapeHandler shellEscapeHandler,
                                CommandMirror commandMirror,
                                CommandGenerator commandGenerator) {
        this.commandBridge = commandBridge;
        this.shellEscapeHandler = shellEscapeHandler;
        this.commandMirror = commandMirror;
        this.commandGenerator = commandGenerator;
        
        // Initialize with current environment variables
        loadEnvironmentVariables();
        
        // Set up shell escape handler to sync command history
        shellEscapeHandler.setCommandHistoryConsumer(commands -> {
            for (String cmd : commands) {
                addToCommandHistory("!" + cmd);
            }
        });
    }
    
    /**
     * Execute a PUI operation via the shell.
     * 
     * @param operation The operation identifier
     * @param params Map of parameters for the operation
     * @return The result of executing the command
     */
    public String executeOperation(String operation, Map<String, String> params) {
        String command = commandMirror.puiToShell(operation, params);
        return executeCommand(command);
    }
    
    /**
     * Execute a shell command directly.
     * 
     * @param command The shell command to execute
     * @return The command output
     */
    public String executeCommand(String command) {
        // Special handling for commands starting with !
        if (command.startsWith("!")) {
            String shellCommand = command.substring(1);
            String result = executeShellCommand(shellCommand);
            addToCommandHistory(command);
            return result;
        }
        
        // For PUI commands, execute through appropriate channel
        String result = executeRinCommand(command);
        addToCommandHistory(command);
        return result;
    }
    
    /**
     * Execute a command asynchronously.
     * 
     * @param command The command to execute
     * @return CompletableFuture with the command result
     */
    public CompletableFuture<String> executeCommandAsync(String command) {
        return CompletableFuture.supplyAsync(() -> executeCommand(command));
    }
    
    /**
     * Execute a direct shell command (without the rin prefix).
     * 
     * @param command The shell command to execute
     * @return The command output
     */
    public String executeShellCommand(String command) {
        // Check for special commands
        if (command.equals("shell") || command.equals("bash")) {
            return "Dropping to shell...";
        }
        
        StringBuilder output = new StringBuilder();
        Process process = null;
        
        try {
            // Create process builder with shell
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
            
            // Set environment variables
            pb.environment().putAll(environmentVariables);
            
            // Redirect error stream to output stream
            pb.redirectErrorStream(true);
            
            // Start process
            process = pb.start();
            final String processId = String.valueOf(System.currentTimeMillis());
            runningProcesses.put(processId, process);
            
            // Read process output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line).append("\n");
                }
            }
            
            // Wait for process to complete
            process.waitFor();
            
            // Remove from running processes
            runningProcesses.remove(processId);
            
            return output.toString();
        } catch (IOException | InterruptedException e) {
            return "Error executing command: " + e.getMessage();
        } finally {
            if (process != null && process.isAlive()) {
                process.destroy();
            }
        }
    }
    
    /**
     * Execute a Rinna command (with rin prefix).
     * 
     * @param command The command to execute (without rin prefix)
     * @return The command output
     */
    public String executeRinCommand(String command) {
        return executeShellCommand(DEFAULT_COMMAND_PREFIX + " " + command);
    }
    
    /**
     * Start a long-running process.
     * 
     * @param command The command to execute
     * @param outputConsumer Consumer for the process output
     * @return Process ID for tracking
     */
    public String startProcess(String command, Consumer<String> outputConsumer) {
        try {
            // Create process builder
            ProcessBuilder pb = new ProcessBuilder("/bin/bash", "-c", command);
            pb.environment().putAll(environmentVariables);
            
            // Start process
            Process process = pb.start();
            final String processId = String.valueOf(System.currentTimeMillis());
            runningProcesses.put(processId, process);
            
            // Create thread for reading output
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    
                    String line;
                    while ((line = reader.readLine()) != null) {
                        outputConsumer.accept(line);
                    }
                } catch (IOException e) {
                    outputConsumer.accept("Error reading process output: " + e.getMessage());
                }
            });
            outputThread.setDaemon(true);
            outputThread.start();
            
            // Add to command history
            addToCommandHistory("!" + command + " &");
            
            return processId;
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * Stop a running process.
     * 
     * @param processId The process ID to stop
     * @return true if the process was stopped, false otherwise
     */
    public boolean stopProcess(String processId) {
        Process process = runningProcesses.get(processId);
        if (process != null && process.isAlive()) {
            process.destroy();
            runningProcesses.remove(processId);
            return true;
        }
        return false;
    }
    
    /**
     * Drop to a shell session.
     * 
     * @return List of commands executed in the shell
     */
    public List<String> escapeToShell() {
        return shellEscapeHandler.escapeToShell();
    }
    
    /**
     * Get auto-completion suggestions for a command.
     * 
     * @param partialCommand The partial command
     * @return List of completion suggestions
     */
    public List<String> getCompletionSuggestions(String partialCommand) {
        List<String> suggestions = new ArrayList<>();
        
        try {
            // Use bash completion mechanism
            String completionCommand = "compgen -c " + partialCommand;
            String result = executeShellCommand(completionCommand);
            
            for (String line : result.split("\n")) {
                if (!line.trim().isEmpty()) {
                    suggestions.add(line.trim());
                }
            }
        } catch (Exception e) {
            // Fallback to basic completion
            for (String cmd : commandHistory) {
                if (cmd.startsWith(partialCommand)) {
                    suggestions.add(cmd);
                }
            }
        }
        
        return suggestions;
    }
    
    /**
     * Get or set an environment variable.
     * 
     * @param name The variable name
     * @param value The variable value, or null to get current value
     * @return The current value, or the previous value if setting
     */
    public String environmentVariable(String name, String value) {
        String oldValue = environmentVariables.get(name);
        
        if (value != null) {
            environmentVariables.put(name, value);
            
            // Notify listeners
            Map<String, String> change = new HashMap<>();
            change.put(name, value);
            notifyEnvironmentChangeListeners(change);
        }
        
        return oldValue;
    }
    
    /**
     * Get all environment variables.
     * 
     * @return Map of environment variables
     */
    public Map<String, String> getAllEnvironmentVariables() {
        return new HashMap<>(environmentVariables);
    }
    
    /**
     * Set multiple environment variables.
     * 
     * @param variables Map of variables to set
     */
    public void setEnvironmentVariables(Map<String, String> variables) {
        environmentVariables.putAll(variables);
        
        // Notify listeners
        notifyEnvironmentChangeListeners(variables);
    }
    
    /**
     * Generate a shell script from command history.
     * 
     * @param scriptPath The path to save the script
     * @param startIndex The index to start from (0 for all)
     * @return true if the script was generated, false otherwise
     */
    public boolean generateScriptFromHistory(String scriptPath, int startIndex) {
        try {
            commandGenerator.clear();
            
            // Add header comment
            commandGenerator.addComment("Script generated from command history");
            commandGenerator.addBlankLine();
            
            // Add command history
            List<String> historyToInclude = commandHistory.subList(
                    Math.max(0, Math.min(startIndex, commandHistory.size())), 
                    commandHistory.size());
            
            for (String command : historyToInclude) {
                if (command.startsWith("!")) {
                    // Shell command
                    Map<String, String> params = new HashMap<>();
                    params.put("command", command.substring(1));
                    commandGenerator.addOperation("shell", params);
                } else {
                    // Rinna command
                    Map<String, String> params = new HashMap<>();
                    params.put("command", command);
                    commandGenerator.addOperation("rin", params);
                }
            }
            
            // Generate and save script
            commandGenerator.saveScript(scriptPath);
            
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Generate a shell script from specific operations.
     * 
     * @param scriptPath The path to save the script
     * @param operations List of operations with parameters
     * @return true if the script was generated, false otherwise
     */
    public boolean generateScript(String scriptPath, List<Map.Entry<String, Map<String, String>>> operations) {
        try {
            commandGenerator.clear();
            
            // Add header comment
            commandGenerator.addComment("Script generated from PUI operations");
            commandGenerator.addBlankLine();
            
            // Add operations
            for (Map.Entry<String, Map<String, String>> operation : operations) {
                if (operation.getKey().equals("comment")) {
                    commandGenerator.addComment(operation.getValue().get("comment"));
                } else if (operation.getKey().equals("blank")) {
                    commandGenerator.addBlankLine();
                } else {
                    commandGenerator.addOperation(operation.getKey(), operation.getValue());
                }
            }
            
            // Generate and save script
            commandGenerator.saveScript(scriptPath);
            
            return true;
        } catch (IOException e) {
            return false;
        }
    }
    
    /**
     * Add a command to the history.
     * 
     * @param command The command to add
     */
    public void addToCommandHistory(String command) {
        commandHistory.add(command);
        
        // Notify listeners
        notifyCommandExecutionListeners(command);
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
     */
    public void clearCommandHistory() {
        commandHistory.clear();
    }
    
    /**
     * Add a command execution listener.
     * 
     * @param listener The listener to add
     */
    public void addCommandExecutionListener(Consumer<String> listener) {
        commandExecutionListeners.add(listener);
    }
    
    /**
     * Add an environment change listener.
     * 
     * @param listener The listener to add
     */
    public void addEnvironmentChangeListener(Consumer<Map<String, String>> listener) {
        environmentChangeListeners.add(listener);
    }
    
    /**
     * Check if a command is available in the shell.
     * 
     * @param command The command to check
     * @return true if the command is available, false otherwise
     */
    public boolean isCommandAvailable(String command) {
        try {
            String result = executeShellCommand("command -v " + command + " >/dev/null 2>&1 && echo 'true' || echo 'false'");
            return result.trim().equals("true");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the working directory.
     * 
     * @return The current working directory
     */
    public String getWorkingDirectory() {
        try {
            String result = executeShellCommand("pwd");
            return result.trim();
        } catch (Exception e) {
            return System.getProperty("user.dir");
        }
    }
    
    /**
     * Change the working directory.
     * 
     * @param directory The directory to change to
     * @return true if the directory was changed, false otherwise
     */
    public boolean changeWorkingDirectory(String directory) {
        try {
            // Check if directory exists
            File dir = new File(directory);
            if (!dir.exists() || !dir.isDirectory()) {
                return false;
            }
            
            // Change directory
            String result = executeShellCommand("cd " + directory + " && echo 'success'");
            return result.trim().equals("success");
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Get the PUI command equivalent for a shell command.
     * 
     * @param shellCommand The shell command
     * @return The equivalent PUI operation and parameters, or null if no equivalent
     */
    public Map.Entry<String, Map<String, String>> getPuiEquivalent(String shellCommand) {
        return commandMirror.shellToPui(shellCommand);
    }
    
    /**
     * Generate a shell command from a PUI operation and parameters.
     * 
     * @param operation The PUI operation identifier
     * @param params Map of parameters for the operation
     * @return The shell command string
     */
    public String getShellEquivalent(String operation, Map<String, String> params) {
        return commandMirror.puiToShell(operation, params);
    }
    
    /**
     * Check if a PUI operation has a shell command equivalent.
     * 
     * @param operation The PUI operation identifier
     * @return true if the operation has a shell equivalent, false otherwise
     */
    public boolean hasShellEquivalent(String operation) {
        return commandMirror.isOperationSupported(operation);
    }
    
    /**
     * Get all PUI operations that have shell command equivalents.
     * 
     * @return List of operation identifiers
     */
    public List<String> getSupportedOperations() {
        return commandMirror.getSupportedOperations();
    }
    
    /**
     * Get the shell command template for a PUI operation.
     * 
     * @param operation The PUI operation identifier
     * @return The shell command template, or null if not found
     */
    public String getShellTemplate(String operation) {
        return commandMirror.getShellTemplate(operation);
    }
    
    
    /**
     * Load environment variables from the system.
     */
    private void loadEnvironmentVariables() {
        // Get system environment variables
        environmentVariables.putAll(System.getenv());
        
        // Get Java properties as environment variables
        Properties properties = System.getProperties();
        for (String name : properties.stringPropertyNames()) {
            if (name.startsWith("RINNA_")) {
                environmentVariables.put(name, properties.getProperty(name));
            }
        }
    }
    
    /**
     * Notify command execution listeners.
     * 
     * @param command The executed command
     */
    private void notifyCommandExecutionListeners(String command) {
        for (Consumer<String> listener : commandExecutionListeners) {
            listener.accept(command);
        }
    }
    
    /**
     * Notify environment change listeners.
     * 
     * @param changes Map of environment variable changes
     */
    private void notifyEnvironmentChangeListeners(Map<String, String> changes) {
        for (Consumer<Map<String, String>> listener : environmentChangeListeners) {
            listener.accept(changes);
        }
    }
}