package org.rinna.cli.command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.InvalidTransitionException;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Command to manage workflow transitions.
 * Follows the ViewCommand pattern with proper MetadataService integration
 * for tracking workflow operations.
 */
public class WorkflowCommand implements Callable<Integer> {
    
    // Command parameters
    private String itemId;
    private WorkflowState targetState;
    private String comment;
    private String format = "text";
    private boolean verbose = false;
    private String username;
    private String action = "transition"; // transition, list, or help
    
    // Services
    private final ServiceManager serviceManager;
    private final ConfigurationService configService;
    private final MetadataService metadataService;
    private final MockWorkflowService workflowService;
    private final MockItemService itemService;
    private final ContextManager contextManager;
    
    /**
     * Default constructor using singleton service manager.
     */
    public WorkflowCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Constructs a new WorkflowCommand instance with a specific service manager.
     * Primarily used for testing and dependency injection.
     *
     * @param serviceManager the service manager to use
     */
    public WorkflowCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.configService = serviceManager.getConfigurationService();
        this.metadataService = serviceManager.getMetadataService();
        this.workflowService = serviceManager.getMockWorkflowService();
        this.itemService = serviceManager.getMockItemService();
        this.contextManager = ContextManager.getInstance();
        
        // Get current user from configuration
        this.username = configService.getCurrentUser();
        if (this.username == null || this.username.isEmpty()) {
            this.username = System.getProperty("user.name");
        }
    }
    
    @Override
    public Integer call() {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        params.put("format", format);
        params.put("verbose", verbose);
        params.put("action", action);
        params.put("itemId", itemId);
        if (targetState != null) {
            params.put("targetState", targetState.toString());
        }
        if (comment != null) {
            params.put("hasComment", comment != null && !comment.isEmpty());
        }
        
        // Start tracking main operation
        String operationId = metadataService.startOperation("workflow-command", "WORKFLOW", params);
        
        try {
            // Handle different actions
            switch (action.toLowerCase()) {
                case "transition":
                    return handleTransition(operationId);
                case "list":
                    return handleList(operationId);
                case "help":
                    return handleHelp(operationId);
                default:
                    displayError("Unknown action: " + action, 
                              "Valid actions are: transition, list, help");
                    
                    metadataService.failOperation(operationId,
                        new IllegalArgumentException("Unknown action: " + action));
                    
                    return 1;
            }
        } catch (Exception e) {
            displayError("Error: " + e.getMessage(), null);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the transition action.
     * 
     * @param operationId the operation ID for tracking
     * @return exit code (0 for success, 1 for failure)
     */
    private int handleTransition(String operationId) {
        // Validate parameters
        if (itemId == null || itemId.isEmpty()) {
            displayError("Work item ID is required", null);
            
            metadataService.failOperation(operationId,
                new IllegalArgumentException("Work item ID is required"));
            
            return 1;
        }
        
        if (targetState == null) {
            displayError("Target state is required", null);
            
            metadataService.failOperation(operationId,
                new IllegalArgumentException("Target state is required"));
            
            return 1;
        }
        
        try {
            // Track the fetch operation
            String fetchOpId = metadataService.startOperation(
                "workflow-fetch", "READ", 
                Map.of("itemId", itemId));
            
            // Get the current state of the work item
            WorkflowState currentState = workflowService.getCurrentState(itemId);
            if (currentState == null) {
                metadataService.failOperation(fetchOpId,
                    new IllegalArgumentException("Work item not found"));
                
                displayError("Work item not found: " + itemId, null);
                
                metadataService.failOperation(operationId,
                    new IllegalArgumentException("Work item not found: " + itemId));
                
                return 1;
            }
            
            metadataService.completeOperation(fetchOpId, 
                Map.of("currentState", currentState.toString(), "itemFound", true));
            
            // Track validation operation
            String validateOpId = metadataService.startOperation(
                "workflow-validate", "VALIDATE", 
                Map.of(
                    "itemId", itemId,
                    "currentState", currentState.toString(),
                    "targetState", targetState.toString()
                ));
            
            // Check if the transition is valid
            List<WorkflowState> availableTransitions = workflowService.getAvailableTransitions(itemId);
            boolean canTransition = workflowService.canTransition(itemId, targetState);
            
            if (!canTransition) {
                // Create result with available transitions
                Map<String, Object> validateResult = new HashMap<>();
                validateResult.put("valid", false);
                validateResult.put("availableTransitions", availableTransitions.toString());
                
                metadataService.completeOperation(validateOpId, validateResult);
                
                if ("json".equalsIgnoreCase(format)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("result", "error");
                    response.put("message", String.format("Invalid transition from %s to %s", 
                        currentState, targetState));
                    
                    // Add available transitions
                    if (!availableTransitions.isEmpty()) {
                        String[] transitions = new String[availableTransitions.size()];
                        for (int i = 0; i < availableTransitions.size(); i++) {
                            transitions[i] = availableTransitions.get(i).toString();
                        }
                        response.put("availableTransitions", transitions);
                    }
                    
                    System.out.println(OutputFormatter.toJson(response));
                } else {
                    System.err.printf("Error: Invalid transition from %s to %s%n", 
                        currentState, targetState);
                    
                    // Show available transitions
                    if (!availableTransitions.isEmpty()) {
                        System.err.println("Available transitions:");
                        for (WorkflowState state : availableTransitions) {
                            System.err.println("  " + state);
                        }
                    }
                }
                
                metadataService.failOperation(operationId,
                    new InvalidTransitionException(String.format(
                        "Invalid transition from %s to %s", currentState, targetState)));
                
                return 1;
            }
            
            metadataService.completeOperation(validateOpId, 
                Map.of("valid", true));
            
            // Track the transition operation
            String transitionOpId = metadataService.startOperation(
                "workflow-execute", "UPDATE", 
                Map.of(
                    "itemId", itemId,
                    "currentState", currentState.toString(),
                    "targetState", targetState.toString(),
                    "username", username,
                    "hasComment", comment != null && !comment.isEmpty()
                ));
            
            // Perform the transition
            String effectiveComment = comment != null ? comment : "State changed via workflow command";
            WorkItem updatedItem = workflowService.transition(
                itemId, 
                username, 
                targetState, 
                effectiveComment
            );
            
            // Complete the transition operation
            Map<String, Object> transitionResult = new HashMap<>();
            transitionResult.put("success", true);
            transitionResult.put("fromState", currentState.toString());
            transitionResult.put("toState", targetState.toString());
            transitionResult.put("itemId", itemId);
            transitionResult.put("timestamp", System.currentTimeMillis());
            
            metadataService.completeOperation(transitionOpId, transitionResult);
            
            // Track the display operation
            String displayOpId = metadataService.startOperation(
                "workflow-display", "READ", 
                Map.of(
                    "format", format,
                    "itemId", itemId
                ));
            
            if ("json".equalsIgnoreCase(format)) {
                displayTransitionResultAsJson(itemId, currentState, targetState, effectiveComment);
            } else {
                displayTransitionResultAsText(itemId, currentState, targetState, effectiveComment);
            }
            
            metadataService.completeOperation(displayOpId, Map.of("success", true));
            
            // Complete the main operation
            Map<String, Object> result = new HashMap<>();
            result.put("itemId", itemId);
            result.put("fromState", currentState.toString());
            result.put("toState", targetState.toString());
            result.put("success", true);
            
            metadataService.completeOperation(operationId, result);
            
            return 0;
            
        } catch (InvalidTransitionException e) {
            displayError(e.getMessage(), null);
            
            metadataService.failOperation(operationId, e);
            return 1;
        } catch (Exception e) {
            displayError("Error updating work item: " + e.getMessage(), null);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Display transition result in JSON format.
     */
    private void displayTransitionResultAsJson(String itemId, WorkflowState fromState, 
                                             WorkflowState toState, String comment) {
        Map<String, Object> response = new HashMap<>();
        response.put("result", "success");
        response.put("action", "transition");
        response.put("itemId", itemId);
        response.put("fromState", fromState.toString());
        response.put("toState", toState.toString());
        
        if (comment != null && !comment.isEmpty()) {
            response.put("comment", comment);
        }
        
        System.out.println(OutputFormatter.toJson(response));
    }
    
    /**
     * Display transition result in text format.
     */
    private void displayTransitionResultAsText(String itemId, WorkflowState fromState, 
                                             WorkflowState toState, String comment) {
        System.out.printf("Updated work item %s from %s to %s%n", itemId, fromState, toState);
        
        if (comment != null && !comment.isEmpty()) {
            System.out.printf("Comment: %s%n", comment);
        }
    }
    
    /**
     * Handles the list action (show available states for a work item).
     * 
     * @param operationId the operation ID for tracking
     * @return exit code (0 for success, 1 for failure)
     */
    private int handleList(String operationId) {
        // Validate parameters
        if (itemId == null || itemId.isEmpty()) {
            displayError("Work item ID is required", "Usage: workflow list <id>");
            
            metadataService.failOperation(operationId,
                new IllegalArgumentException("Work item ID is required"));
            
            return 1;
        }
        
        try {
            // Track the fetch operation
            String fetchOpId = metadataService.startOperation(
                "workflow-list-fetch", "READ", 
                Map.of("itemId", itemId));
            
            // Get the current state of the work item
            WorkflowState currentState = workflowService.getCurrentState(itemId);
            if (currentState == null) {
                metadataService.failOperation(fetchOpId,
                    new IllegalArgumentException("Work item not found"));
                
                displayError("Work item not found: " + itemId, null);
                
                metadataService.failOperation(operationId,
                    new IllegalArgumentException("Work item not found: " + itemId));
                
                return 1;
            }
            
            metadataService.completeOperation(fetchOpId, 
                Map.of("currentState", currentState.toString(), "itemFound", true));
            
            // Track transitions lookup operation
            String transitionsOpId = metadataService.startOperation(
                "workflow-list-transitions", "READ", 
                Map.of(
                    "itemId", itemId,
                    "currentState", currentState.toString()
                ));
            
            // Get available transitions
            List<WorkflowState> availableTransitions = workflowService.getAvailableTransitions(itemId);
            
            // Complete transitions operation
            Map<String, Object> transitionsResult = new HashMap<>();
            transitionsResult.put("count", availableTransitions.size());
            transitionsResult.put("transitions", availableTransitions.toString());
            
            metadataService.completeOperation(transitionsOpId, transitionsResult);
            
            // Track display operation
            String displayOpId = metadataService.startOperation(
                "workflow-list-display", "READ", 
                Map.of(
                    "format", format,
                    "itemId", itemId,
                    "count", availableTransitions.size()
                ));
            
            if ("json".equalsIgnoreCase(format)) {
                displayWorkflowStatusAsJson(itemId, currentState, availableTransitions);
            } else {
                displayWorkflowStatusAsText(itemId, currentState, availableTransitions);
            }
            
            metadataService.completeOperation(displayOpId, Map.of("success", true));
            
            // Complete the main operation
            Map<String, Object> result = new HashMap<>();
            result.put("itemId", itemId);
            result.put("currentState", currentState.toString());
            result.put("transitionCount", availableTransitions.size());
            result.put("success", true);
            
            metadataService.completeOperation(operationId, result);
            
            return 0;
            
        } catch (Exception e) {
            displayError("Error getting available states: " + e.getMessage(), null);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Display workflow status in JSON format.
     */
    private void displayWorkflowStatusAsJson(String itemId, WorkflowState currentState, 
                                           List<WorkflowState> availableTransitions) {
        Map<String, Object> response = new HashMap<>();
        response.put("result", "success");
        response.put("action", "list");
        response.put("itemId", itemId);
        response.put("currentState", currentState.toString());
        
        if (availableTransitions.isEmpty()) {
            response.put("availableTransitions", new String[0]);
            response.put("message", "No available transitions from current state");
        } else {
            String[] transitions = new String[availableTransitions.size()];
            for (int i = 0; i < availableTransitions.size(); i++) {
                transitions[i] = availableTransitions.get(i).toString();
            }
            response.put("availableTransitions", transitions);
        }
        
        System.out.println(OutputFormatter.toJson(response));
    }
    
    /**
     * Display workflow status in text format.
     */
    private void displayWorkflowStatusAsText(String itemId, WorkflowState currentState, 
                                           List<WorkflowState> availableTransitions) {
        System.out.printf("Work item %s is currently in state: %s%n", itemId, currentState);
        
        if (availableTransitions.isEmpty()) {
            System.out.println("No available transitions from current state.");
        } else {
            System.out.println("Available transitions:");
            for (WorkflowState state : availableTransitions) {
                System.out.println("  " + state);
            }
        }
    }
    
    /**
     * Handles the help action.
     * 
     * @param operationId the operation ID for tracking
     * @return exit code (0 for success, 1 for failure)
     */
    private int handleHelp(String operationId) {
        try {
            // Track help display
            String displayOpId = metadataService.startOperation(
                "workflow-help-display", "READ", 
                Map.of("format", format));
            
            if ("json".equalsIgnoreCase(format)) {
                displayHelpAsJson();
            } else {
                displayHelpAsText();
            }
            
            metadataService.completeOperation(displayOpId, Map.of("success", true));
            
            // Complete the main operation
            metadataService.completeOperation(operationId, Map.of("success", true));
            
            return 0;
        } catch (Exception e) {
            displayError("Error displaying help: " + e.getMessage(), null);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Display help in JSON format.
     */
    private void displayHelpAsJson() {
        Map<String, Object> help = new HashMap<>();
        help.put("result", "success");
        help.put("command", "workflow");
        help.put("usage", "rin workflow [action] [options]");
        
        // Actions
        Map<String, Object>[] actions = new Map[3];
        
        Map<String, Object> transitionAction = new HashMap<>();
        transitionAction.put("name", "transition");
        transitionAction.put("description", "Change a work item's state");
        transitionAction.put("usage", "rin workflow <id> <state> [comment]");
        actions[0] = transitionAction;
        
        Map<String, Object> listAction = new HashMap<>();
        listAction.put("name", "list");
        listAction.put("description", "Show available states for a work item");
        listAction.put("usage", "rin workflow list <id>");
        actions[1] = listAction;
        
        Map<String, Object> helpAction = new HashMap<>();
        helpAction.put("name", "help");
        helpAction.put("description", "Show this help information");
        helpAction.put("usage", "rin workflow help");
        actions[2] = helpAction;
        
        help.put("actions", actions);
        
        // Options
        Map<String, Object>[] options = new Map[2];
        
        Map<String, Object> jsonOpt = new HashMap<>();
        jsonOpt.put("name", "--json");
        jsonOpt.put("description", "Output in JSON format");
        options[0] = jsonOpt;
        
        Map<String, Object> verboseOpt = new HashMap<>();
        verboseOpt.put("name", "--verbose");
        verboseOpt.put("description", "Show verbose output with additional details");
        options[1] = verboseOpt;
        
        help.put("options", options);
        
        System.out.println(OutputFormatter.toJson(help));
    }
    
    /**
     * Display help in text format.
     */
    private void displayHelpAsText() {
        System.out.println("Workflow Command Usage:");
        System.out.println("  rin workflow <id> <state> [comment]   Change a work item's state");
        System.out.println("  rin workflow list <id>                Show available states for a work item");
        System.out.println("  rin workflow help                     Show this help information");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --json                                Output in JSON format");
        System.out.println("  --verbose                             Show verbose output with additional details");
    }
    
    /**
     * Display an error message in the appropriate format.
     * 
     * @param message the error message
     * @param details additional details (can be null)
     */
    private void displayError(String message, String details) {
        if ("json".equalsIgnoreCase(format)) {
            Map<String, Object> error = new HashMap<>();
            error.put("result", "error");
            error.put("message", message);
            
            if (details != null && !details.isEmpty()) {
                error.put("details", details);
            }
            
            System.out.println(OutputFormatter.toJson(error));
        } else {
            System.err.println("Error: " + message);
            
            if (details != null && !details.isEmpty()) {
                System.err.println(details);
            }
        }
    }
    
    /**
     * Legacy method for execute a workflow transition with detailed arguments.
     * Maintained for backward compatibility.
     *
     * @param args the command arguments
     * @return text output of the command execution
     */
    public String execute(String[] args) {
        if (args.length < 2) {
            return "Error: Invalid arguments\nUsage: workflow <id> <state> [comment]";
        }
        
        // Set up command parameters
        this.itemId = args[0];
        this.targetState = WorkflowState.fromString(args[1]);
        if (args.length > 2) {
            this.comment = args[2];
        }
        this.action = "transition";
        this.format = "text";
        
        // Execute command and capture output
        StringBuilder output = new StringBuilder();
        
        // Redirect System.out and System.err temporarily to capture output
        java.io.PrintStream originalOut = System.out;
        java.io.PrintStream originalErr = System.err;
        
        try {
            java.io.ByteArrayOutputStream capturedOut = new java.io.ByteArrayOutputStream();
            java.io.ByteArrayOutputStream capturedErr = new java.io.ByteArrayOutputStream();
            
            System.setOut(new java.io.PrintStream(capturedOut));
            System.setErr(new java.io.PrintStream(capturedErr));
            
            int result = call();
            
            // Restore original streams
            System.setOut(originalOut);
            System.setErr(originalErr);
            
            // Get captured output
            output.append(capturedOut.toString());
            
            // Append error output if there was an error
            if (result != 0) {
                output.append(capturedErr.toString());
            }
            
            return output.toString();
        } catch (Exception e) {
            // Restore original streams
            System.setOut(originalOut);
            System.setErr(originalErr);
            
            return "Error: " + e.getMessage() + "\nUsage: workflow <id> <state> [comment]";
        }
    }
    
    /**
     * Legacy method for listing available states for a work item.
     * Maintained for backward compatibility.
     *
     * @param args the command arguments
     * @return text output of the available states
     */
    public String executeList(String[] args) {
        if (args.length < 1) {
            return "Error: Missing work item ID\nUsage: workflow list <id>";
        }
        
        // Set up command parameters
        this.itemId = args[0];
        this.action = "list";
        this.format = "text";
        
        // Execute command and capture output
        StringBuilder output = new StringBuilder();
        
        // Redirect System.out and System.err temporarily to capture output
        java.io.PrintStream originalOut = System.out;
        java.io.PrintStream originalErr = System.err;
        
        try {
            java.io.ByteArrayOutputStream capturedOut = new java.io.ByteArrayOutputStream();
            java.io.ByteArrayOutputStream capturedErr = new java.io.ByteArrayOutputStream();
            
            System.setOut(new java.io.PrintStream(capturedOut));
            System.setErr(new java.io.PrintStream(capturedErr));
            
            int result = call();
            
            // Restore original streams
            System.setOut(originalOut);
            System.setErr(originalErr);
            
            // Get captured output
            output.append(capturedOut.toString());
            
            // Append error output if there was an error
            if (result != 0) {
                output.append(capturedErr.toString());
            }
            
            return output.toString();
        } catch (Exception e) {
            // Restore original streams
            System.setOut(originalOut);
            System.setErr(originalErr);
            
            return "Error: " + e.getMessage() + "\nUsage: workflow list <id>";
        }
    }
    
    public String getItemId() {
        return itemId;
    }
    
    public WorkflowCommand setItemId(String itemId) {
        this.itemId = itemId;
        return this;
    }
    
    public WorkflowState getTargetState() {
        return targetState;
    }
    
    public WorkflowCommand setTargetState(WorkflowState targetState) {
        this.targetState = targetState;
        return this;
    }
    
    public String getComment() {
        return comment;
    }
    
    public WorkflowCommand setComment(String comment) {
        this.comment = comment;
        return this;
    }
    
    public String getFormat() {
        return format;
    }
    
    public WorkflowCommand setFormat(String format) {
        this.format = format;
        return this;
    }
    
    /**
     * Sets the JSON output flag (for backward compatibility).
     * 
     * @param jsonOutput true to output in JSON format, false for text
     * @return this command instance for method chaining
     */
    public WorkflowCommand setJsonOutput(boolean jsonOutput) {
        this.format = jsonOutput ? "json" : "text";
        return this;
    }
    
    public boolean isVerbose() {
        return verbose;
    }
    
    public WorkflowCommand setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }
    
    public String getUsername() {
        return username;
    }
    
    public WorkflowCommand setUsername(String username) {
        this.username = username;
        return this;
    }
    
    public String getAction() {
        return action;
    }
    
    public WorkflowCommand setAction(String action) {
        this.action = action;
        return this;
    }
}
