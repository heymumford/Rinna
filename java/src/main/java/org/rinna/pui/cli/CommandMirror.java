package org.rinna.pui.cli;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides bidirectional mapping between PUI operations and shell commands.
 * Implements SUSBS command mirroring requirement to ensure every PUI
 * operation has a direct shell command equivalent and vice versa.
 */
public class CommandMirror {
    
    // Mapping from PUI operation to shell command templates
    private final Map<String, String> puiToShellMap = new HashMap<>();
    
    // Mapping from shell command pattern to PUI operation
    private final Map<Pattern, MirrorMapping> shellToPuiMap = new HashMap<>();
    
    // Default command prefix
    private static final String DEFAULT_COMMAND_PREFIX = "rin";
    
    /**
     * Create a new command mirror with default mappings.
     */
    public CommandMirror() {
        initializeDefaultMappings();
    }
    
    /**
     * Initialize default mappings between PUI operations and shell commands.
     */
    private void initializeDefaultMappings() {
        // Work item commands
        addMapping("workitem.add", "add ${type} --title=\"${title}\" --priority=${priority}",
                "add\\s+(\\w+)\\s+--title=\"([^\"]*)\"\\s+--priority=(\\w+)",
                args -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("type", args.get(0));
                    params.put("title", args.get(1));
                    params.put("priority", args.get(2));
                    return params;
                });
        
        addMapping("workitem.list", "list ${filter}",
                "list(?:\\s+(.*))?",
                args -> {
                    Map<String, String> params = new HashMap<>();
                    if (args.size() > 0 && args.get(0) != null) {
                        params.put("filter", args.get(0));
                    } else {
                        params.put("filter", "");
                    }
                    return params;
                });
        
        addMapping("workitem.view", "view ${id}",
                "view\\s+(\\S+)",
                args -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("id", args.get(0));
                    return params;
                });
        
        addMapping("workitem.update", "update ${id} --${field}=\"${value}\"",
                "update\\s+(\\S+)\\s+--([\\w-]+)=\"([^\"]*)\"",
                args -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("id", args.get(0));
                    params.put("field", args.get(1));
                    params.put("value", args.get(2));
                    return params;
                });
        
        addMapping("workitem.done", "done ${id}",
                "done\\s+(\\S+)",
                args -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("id", args.get(0));
                    return params;
                });
        
        // Workflow commands
        addMapping("workflow.transition", "workflow transition ${id} ${state}",
                "workflow\\s+transition\\s+(\\S+)\\s+(\\S+)",
                args -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("id", args.get(0));
                    params.put("state", args.get(1));
                    return params;
                });
        
        addMapping("workflow.states", "workflow states",
                "workflow\\s+states",
                args -> new HashMap<>());
        
        // Dependency commands
        addMapping("dependency.add", "dependency add ${sourceId} ${targetId} --type=${type}",
                "dependency\\s+add\\s+(\\S+)\\s+(\\S+)\\s+--type=(\\w+)",
                args -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("sourceId", args.get(0));
                    params.put("targetId", args.get(1));
                    params.put("type", args.get(2));
                    return params;
                });
        
        addMapping("dependency.remove", "dependency remove ${sourceId} ${targetId}",
                "dependency\\s+remove\\s+(\\S+)\\s+(\\S+)",
                args -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("sourceId", args.get(0));
                    params.put("targetId", args.get(1));
                    return params;
                });
        
        addMapping("dependency.list", "dependency list ${id}",
                "dependency\\s+list\\s+(\\S+)",
                args -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("id", args.get(0));
                    return params;
                });
        
        // Statistics commands
        addMapping("stats.dashboard", "stats dashboard",
                "stats\\s+dashboard",
                args -> new HashMap<>());
        
        addMapping("stats.detail", "stats detail ${metric}",
                "stats\\s+detail\\s+(\\S+)",
                args -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("metric", args.get(0));
                    return params;
                });
        
        // Search commands
        addMapping("search.global", "search \"${query}\"",
                "search\\s+\"([^\"]*)\"",
                args -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("query", args.get(0));
                    return params;
                });
        
        addMapping("search.filtered", "search \"${query}\" --context=${context}",
                "search\\s+\"([^\"]*)\"\\s+--context=(\\S+)",
                args -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("query", args.get(0));
                    params.put("context", args.get(1));
                    return params;
                });
        
        // Admin commands
        addMapping("admin.audit", "admin audit ${action}",
                "admin\\s+audit\\s+(\\S+)(?:\\s+(.*))?",
                args -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("action", args.get(0));
                    if (args.size() > 1 && args.get(1) != null) {
                        params.put("options", args.get(1));
                    }
                    return params;
                });
        
        addMapping("admin.compliance", "admin compliance ${action} ${target}",
                "admin\\s+compliance\\s+(\\S+)\\s+(\\S+)",
                args -> {
                    Map<String, String> params = new HashMap<>();
                    params.put("action", args.get(0));
                    params.put("target", args.get(1));
                    return params;
                });
        
        // Server commands
        addMapping("server.start", "server start --port=${port}",
                "server\\s+start(?:\\s+--port=(\\d+))?",
                args -> {
                    Map<String, String> params = new HashMap<>();
                    if (args.size() > 0 && args.get(0) != null) {
                        params.put("port", args.get(0));
                    } else {
                        params.put("port", "8080");
                    }
                    return params;
                });
        
        addMapping("server.stop", "server stop",
                "server\\s+stop",
                args -> new HashMap<>());
    }
    
    /**
     * Add a bidirectional mapping between a PUI operation and a shell command.
     * 
     * @param puiOperation The PUI operation identifier
     * @param shellTemplate The shell command template with placeholders
     * @param shellPattern The regex pattern for parsing the shell command
     * @param parameterExtractor The function to extract parameters from regex groups
     */
    public void addMapping(String puiOperation, String shellTemplate, String shellPattern,
                          ParameterExtractor parameterExtractor) {
        // Add PUI to shell mapping
        puiToShellMap.put(puiOperation, shellTemplate);
        
        // Add shell to PUI mapping
        Pattern pattern = Pattern.compile("^" + shellPattern + "$");
        shellToPuiMap.put(pattern, new MirrorMapping(puiOperation, parameterExtractor));
    }
    
    /**
     * Generate a shell command from a PUI operation and parameters.
     * 
     * @param operation The PUI operation identifier
     * @param params Map of parameters for the operation
     * @return The shell command string
     * @throws IllegalArgumentException if the operation is not supported
     */
    public String puiToShell(String operation, Map<String, String> params) {
        if (!puiToShellMap.containsKey(operation)) {
            throw new IllegalArgumentException("Unsupported PUI operation: " + operation);
        }
        
        String template = puiToShellMap.get(operation);
        String command = substituteParameters(template, params);
        
        return DEFAULT_COMMAND_PREFIX + " " + command;
    }
    
    /**
     * Generate a PUI operation from a shell command.
     * 
     * @param shellCommand The shell command string
     * @return Entry with the PUI operation identifier and parameter map, or null if not recognized
     */
    public Map.Entry<String, Map<String, String>> shellToPui(String shellCommand) {
        // Remove the command prefix if present
        String command = shellCommand;
        if (command.startsWith(DEFAULT_COMMAND_PREFIX + " ")) {
            command = command.substring((DEFAULT_COMMAND_PREFIX + " ").length());
        }
        
        // Check each pattern
        for (Map.Entry<Pattern, MirrorMapping> entry : shellToPuiMap.entrySet()) {
            Pattern pattern = entry.getKey();
            MirrorMapping mapping = entry.getValue();
            
            Matcher matcher = pattern.matcher(command);
            if (matcher.matches()) {
                // Extract parameters from regex groups
                List<String> args = new ArrayList<>();
                for (int i = 1; i <= matcher.groupCount(); i++) {
                    args.add(matcher.group(i));
                }
                
                // Create parameter map
                Map<String, String> params = mapping.getParameterExtractor().extractParameters(args);
                
                // Return the PUI operation and parameters
                return Map.entry(mapping.getPuiOperation(), params);
            }
        }
        
        // No matching pattern found
        return null;
    }
    
    /**
     * Check if a PUI operation is supported.
     * 
     * @param operation The PUI operation identifier
     * @return true if the operation is supported, false otherwise
     */
    public boolean isOperationSupported(String operation) {
        return puiToShellMap.containsKey(operation);
    }
    
    /**
     * Get all supported PUI operations.
     * 
     * @return List of supported operation identifiers
     */
    public List<String> getSupportedOperations() {
        return new ArrayList<>(puiToShellMap.keySet());
    }
    
    /**
     * Get the shell command template for a PUI operation.
     * 
     * @param operation The PUI operation identifier
     * @return The shell command template, or null if not found
     */
    public String getShellTemplate(String operation) {
        return puiToShellMap.get(operation);
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
            
            if (value != null) {
                result = result.replace(placeholder, value);
            }
        }
        
        return result;
    }
    
    /**
     * Class representing a mapping from shell command to PUI operation.
     */
    private static class MirrorMapping {
        private final String puiOperation;
        private final ParameterExtractor parameterExtractor;
        
        public MirrorMapping(String puiOperation, ParameterExtractor parameterExtractor) {
            this.puiOperation = puiOperation;
            this.parameterExtractor = parameterExtractor;
        }
        
        public String getPuiOperation() {
            return puiOperation;
        }
        
        public ParameterExtractor getParameterExtractor() {
            return parameterExtractor;
        }
    }
    
    /**
     * Functional interface for extracting parameters from regex groups.
     */
    @FunctionalInterface
    public interface ParameterExtractor {
        /**
         * Extract parameters from regex groups.
         * 
         * @param args List of regex group matches
         * @return Map of parameter names to values
         */
        Map<String, String> extractParameters(List<String> args);
    }
}