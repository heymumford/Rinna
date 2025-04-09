package org.rinna.pui.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Bridge class that maps PUI operations to corresponding shell commands.
 * Implements SUSBS compliance by ensuring PUI operations have direct shell command equivalents.
 */
public class ShellCommandBridge {
    
    private static final String DEFAULT_COMMAND_PREFIX = "rin";
    
    // Mapping of PUI operations to shell command templates
    private final Map<String, String> operationCommandMap = new HashMap<>();
    
    // Listeners for command execution
    private final List<Consumer<String>> commandExecutionListeners = new ArrayList<>();
    
    // Command history
    private final List<String> commandHistory = new ArrayList<>();
    
    public ShellCommandBridge() {
        initializeCommandMappings();
    }
    
    /**
     * Initialize default command mappings for common operations.
     */
    private void initializeCommandMappings() {
        // Work item commands
        operationCommandMap.put("workitem.add", "add ${type} --title=\"${title}\" --priority=${priority}");
        operationCommandMap.put("workitem.update", "update ${id} --${field}=\"${value}\"");
        operationCommandMap.put("workitem.list", "list ${filter}");
        operationCommandMap.put("workitem.view", "view ${id}");
        operationCommandMap.put("workitem.done", "done ${id}");
        
        // Workflow commands
        operationCommandMap.put("workflow.transition", "workflow transition ${id} ${state}");
        operationCommandMap.put("workflow.states", "workflow states");
        
        // Dependency commands
        operationCommandMap.put("dependency.add", "dependency add ${sourceId} ${targetId} --type=${type}");
        operationCommandMap.put("dependency.remove", "dependency remove ${sourceId} ${targetId}");
        operationCommandMap.put("dependency.list", "dependency list ${id}");
        
        // Statistics commands
        operationCommandMap.put("stats.dashboard", "stats dashboard");
        operationCommandMap.put("stats.detail", "stats detail ${metric}");
        
        // Search commands
        operationCommandMap.put("search.global", "search \"${query}\"");
        operationCommandMap.put("search.filtered", "search \"${query}\" --context=${context}");
    }
    
    /**
     * Add a custom command mapping.
     * 
     * @param operation PUI operation identifier
     * @param commandTemplate Shell command template with placeholder variables
     * @return This instance for method chaining
     */
    public ShellCommandBridge addCommandMapping(String operation, String commandTemplate) {
        operationCommandMap.put(operation, commandTemplate);
        return this;
    }
    
    /**
     * Generate a shell command from a PUI operation and parameters.
     * 
     * @param operation The operation identifier
     * @param params Map of parameters to substitute in the command template
     * @return The generated shell command
     * @throws IllegalArgumentException if the operation is not supported
     */
    public String generateCommand(String operation, Map<String, String> params) {
        if (!operationCommandMap.containsKey(operation)) {
            throw new IllegalArgumentException("Unsupported operation: " + operation);
        }
        
        String template = operationCommandMap.get(operation);
        String command = DEFAULT_COMMAND_PREFIX + " " + substituteParameters(template, params);
        
        return command;
    }
    
    /**
     * Execute a PUI operation by generating and executing the equivalent shell command.
     * 
     * @param operation The operation identifier
     * @param params Map of parameters for the operation
     * @return The result of executing the shell command
     */
    public String executeOperation(String operation, Map<String, String> params) {
        String command = generateCommand(operation, params);
        String result = executeShellCommand(command);
        
        // Add to history
        commandHistory.add(command);
        
        // Notify listeners
        notifyCommandExecutionListeners(command);
        
        return result;
    }
    
    /**
     * Execute a shell command directly.
     * 
     * @param command The shell command to execute
     * @return The command output
     */
    public String executeShellCommand(String command) {
        // In a real implementation, this would use ProcessBuilder or similar
        // to execute the actual shell command
        
        // For now, we'll just simulate execution
        System.out.println("Executing shell command: " + command);
        
        // Add to history
        commandHistory.add(command);
        
        // Notify listeners
        notifyCommandExecutionListeners(command);
        
        // Simulate command output
        return "Executed: " + command;
    }
    
    /**
     * Get the command history.
     * 
     * @return List of executed commands
     */
    public List<String> getCommandHistory() {
        return new ArrayList<>(commandHistory);
    }
    
    /**
     * Add a command execution listener.
     * 
     * @param listener Consumer that will be called with the executed command
     * @return This instance for method chaining
     */
    public ShellCommandBridge addCommandExecutionListener(Consumer<String> listener) {
        commandExecutionListeners.add(listener);
        return this;
    }
    
    /**
     * Generate a shell script from a series of operations.
     * 
     * @param operations List of operations with parameters
     * @return Shell script content
     */
    public String generateScript(List<Map.Entry<String, Map<String, String>>> operations) {
        StringBuilder script = new StringBuilder("#!/bin/bash\n\n");
        script.append("# Generated by Rinna PUI\n\n");
        
        for (Map.Entry<String, Map<String, String>> entry : operations) {
            String operation = entry.getKey();
            Map<String, String> params = entry.getValue();
            
            String command = generateCommand(operation, params);
            script.append(command).append("\n");
        }
        
        return script.toString();
    }
    
    /**
     * Substitute parameter placeholders in a template string.
     * 
     * @param template The template string with ${placeholder} variables
     * @param params Map of parameter names to values
     * @return The template with parameters substituted
     */
    private String substituteParameters(String template, Map<String, String> params) {
        String result = template;
        
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String placeholder = "${" + entry.getKey() + "}";
            String value = entry.getValue();
            
            result = result.replace(placeholder, value);
        }
        
        return result;
    }
    
    /**
     * Notify all command execution listeners.
     * 
     * @param command The executed command
     */
    private void notifyCommandExecutionListeners(String command) {
        for (Consumer<String> listener : commandExecutionListeners) {
            listener.accept(command);
        }
    }
}