package org.rinna.cli.command;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import org.rinna.cli.model.WorkItem;
import org.rinna.cli.model.WorkflowState;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MockItemService;
import org.rinna.cli.service.MockWorkflowService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Command to mark a work item as being in the testing phase.
 * This is NOT a test class, but a command implementation for the CLI.
 * Follows the ViewCommand pattern with operation tracking.
 * 
 * @see org.rinna.cli.command.WorkflowCommand
 */
@SuppressWarnings("PMD.TestClassWithoutTestCases")
public class TestCommand implements Callable<Integer> {
    
    // Command parameters
    private String itemId;
    private String tester;
    private String format = "text"; // Output format (text or json)
    private boolean verbose = false;
    private String username;
    
    // Service dependencies
    private final ServiceManager serviceManager;
    private final MockWorkflowService workflowService;
    private final MockItemService itemService;
    private final ConfigurationService configService;
    private final MetadataService metadataService;
    private final ContextManager contextManager;
    
    /**
     * Constructs a new TestCommand with the default service manager.
     */
    public TestCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Constructs a new TestCommand with the provided service manager.
     * This constructor allows for dependency injection, making the command more testable.
     * 
     * @param serviceManager the service manager to use
     */
    public TestCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.workflowService = serviceManager.getMockWorkflowService();
        this.itemService = serviceManager.getMockItemService();
        this.configService = serviceManager.getConfigurationService();
        this.metadataService = serviceManager.getMetadataService();
        this.contextManager = ContextManager.getInstance();
        
        // Get current user from configuration
        this.username = configService.getCurrentUser();
        if (this.username == null || this.username.isEmpty()) {
            this.username = System.getProperty("user.name");
        }
    }
    
    /**
     * Executes the command with proper operation tracking.
     */
    @Override
    public Integer call() {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        params.put("format", format);
        params.put("verbose", verbose);
        
        if (itemId != null) {
            params.put("itemId", itemId);
        }
        
        if (tester != null) {
            params.put("tester", tester);
        }
        
        // Start tracking main operation
        String operationId = metadataService.startOperation("test-command", "UPDATE", params);
        
        try {
            // 1. Validate inputs
            if (itemId == null || itemId.isEmpty()) {
                return handleError("Work item ID is required", new IllegalArgumentException("Missing item ID"), operationId);
            }
            
            // 2. Verify work item exists and check its state
            String verifyOpId = metadataService.startOperation("test-verify-item", "READ", Map.of("itemId", itemId));
            
            WorkItem workItem;
            try {
                workItem = itemService.getItem(itemId);
                if (workItem == null) {
                    throw new IllegalArgumentException("Work item not found: " + itemId);
                }
                
                Map<String, Object> verifyResult = new HashMap<>();
                verifyResult.put("itemExists", true);
                verifyResult.put("currentState", workItem.getState().toString());
                metadataService.completeOperation(verifyOpId, verifyResult);
            } catch (Exception e) {
                metadataService.failOperation(verifyOpId, e);
                return handleError("Failed to verify work item", e, operationId);
            }
            
            // 3. Transition work item to TESTING state if needed
            if (workItem.getState() != WorkflowState.TESTING) {
                String transitionOpId = metadataService.startOperation("test-transition", "UPDATE", 
                        Map.of("itemId", itemId, "targetState", "TESTING"));
                
                try {
                    String comment = "Moved to testing";
                    if (tester != null && !tester.isEmpty()) {
                        comment += " (Tester: " + tester + ")";
                    }
                    
                    workflowService.transition(itemId, username, WorkflowState.TESTING, comment);
                    
                    Map<String, Object> transitionResult = new HashMap<>();
                    transitionResult.put("success", true);
                    transitionResult.put("previousState", workItem.getState().toString());
                    transitionResult.put("newState", "TESTING");
                    metadataService.completeOperation(transitionOpId, transitionResult);
                } catch (Exception e) {
                    metadataService.failOperation(transitionOpId, e);
                    return handleError("Failed to transition work item", e, operationId);
                }
            }
            
            // 4. Assign tester if provided
            if (tester != null && !tester.isEmpty()) {
                String assignOpId = metadataService.startOperation("test-assign-tester", "UPDATE", 
                        Map.of("itemId", itemId, "tester", tester));
                
                try {
                    // Store tester in metadata
                    Map<String, String> customFields = new HashMap<>();
                    customFields.put("tester", tester);
                    customFields.put("lastTestDate", LocalDate.now().toString());
                    
                    itemService.updateCustomFields(itemId, customFields);
                    
                    Map<String, Object> assignResult = new HashMap<>();
                    assignResult.put("success", true);
                    assignResult.put("tester", tester);
                    metadataService.completeOperation(assignOpId, assignResult);
                } catch (Exception e) {
                    metadataService.failOperation(assignOpId, e);
                    return handleError("Failed to assign tester", e, operationId);
                }
            }
            
            // 5. Record the item in context for future commands
            contextManager.setCurrentItemId(itemId);
            
            // 6. Output the result
            boolean isJsonOutput = "json".equalsIgnoreCase(format);
            displayResult(workItem, isJsonOutput, operationId);
            
            return 0;
        } catch (Exception e) {
            return handleError("Error executing test command", e, operationId);
        }
    }
    
    /**
     * Displays the result of the command.
     * 
     * @param workItem the work item
     * @param isJsonOutput whether to output in JSON format
     * @param operationId the operation ID for tracking
     */
    private void displayResult(WorkItem workItem, boolean isJsonOutput, String operationId) {
        // Create a sub-operation for displaying the result
        String displayOpId = metadataService.startOperation("test-display-result", "READ", 
                Map.of("itemId", itemId, "format", format));
        
        try {
            if (isJsonOutput) {
                displayResultAsJson(workItem);
            } else {
                displayResultAsText(workItem);
            }
            
            // Record operation success
            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("itemId", itemId);
            result.put("state", workItem.getState().toString());
            if (tester != null && !tester.isEmpty()) {
                result.put("tester", tester);
            }
            
            metadataService.completeOperation(displayOpId, result);
            metadataService.completeOperation(operationId, result);
        } catch (Exception e) {
            metadataService.failOperation(displayOpId, e);
            metadataService.failOperation(operationId, e);
            throw e;
        }
    }
    
    /**
     * Displays the result in JSON format.
     * 
     * @param workItem the work item
     */
    private void displayResultAsJson(WorkItem workItem) {
        OutputFormatter formatter = new OutputFormatter(true);
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("itemId", itemId);
        result.put("title", workItem.getTitle());
        result.put("state", workItem.getState().toString());
        result.put("changed", workItem.getState() != WorkflowState.TESTING);
        
        if (tester != null && !tester.isEmpty()) {
            result.put("tester", tester);
            result.put("lastTestDate", LocalDate.now().toString());
        }
        
        formatter.outputObject("testResult", result);
    }
    
    /**
     * Displays the result in text format.
     * 
     * @param workItem the work item
     */
    private void displayResultAsText(WorkItem workItem) {
        if (workItem.getState() != WorkflowState.TESTING) {
            System.out.printf("Setting work item %s to testing state%n", itemId);
            System.out.println("Updated state: TESTING");
        } else {
            System.out.println("Work item already in TESTING state");
        }
        
        if (tester != null && !tester.isEmpty()) {
            System.out.printf("Assigned tester: %s%n", tester);
        } else {
            System.out.println("No tester assigned");
        }
    }
    
    /**
     * Handles errors with appropriate output based on verbose setting.
     * 
     * @param message the error message
     * @param e the exception
     * @param operationId the operation ID for tracking
     * @return the exit code
     */
    private int handleError(String message, Exception e, String operationId) {
        // Record failure
        if (operationId != null) {
            metadataService.failOperation(operationId, e);
        }
        
        boolean isJsonOutput = "json".equalsIgnoreCase(format);
        
        if (isJsonOutput) {
            OutputFormatter formatter = new OutputFormatter(true);
            Map<String, Object> error = new HashMap<>();
            error.put("error", message);
            error.put("message", e.getMessage());
            formatter.outputObject("error", error);
        } else {
            System.err.println("Error: " + message);
            if (verbose) {
                e.printStackTrace();
            }
        }
        
        return 1;
    }
    
    /**
     * Gets the item ID.
     * 
     * @return the item ID
     */
    public String getItemId() {
        return itemId;
    }
    
    /**
     * Sets the item ID.
     * 
     * @param itemId the item ID
     * @return this command for chaining
     */
    public TestCommand setItemId(String itemId) {
        this.itemId = itemId;
        return this;
    }
    
    /**
     * Gets the tester.
     * 
     * @return the tester
     */
    public String getTester() {
        return tester;
    }
    
    /**
     * Sets the tester.
     * 
     * @param tester the tester
     * @return this command for chaining
     */
    public TestCommand setTester(String tester) {
        this.tester = tester;
        return this;
    }
    
    /**
     * Sets the output format.
     * 
     * @param format the output format ("text" or "json")
     * @return this command for chaining
     */
    public TestCommand setFormat(String format) {
        this.format = format;
        return this;
    }
    
    /**
     * Sets the output format to JSON.
     * This is a legacy setter that maps to the format parameter.
     * 
     * @param jsonOutput true for JSON output
     * @return this command for chaining
     */
    public TestCommand setJsonOutput(boolean jsonOutput) {
        if (jsonOutput) {
            this.format = "json";
        }
        return this;
    }
    
    /**
     * Gets whether verbose output is enabled.
     * 
     * @return whether verbose output is enabled
     */
    public boolean isVerbose() {
        return verbose;
    }
    
    /**
     * Sets whether verbose output is enabled.
     * 
     * @param verbose whether verbose output is enabled
     * @return this command for chaining
     */
    public TestCommand setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }
    
    /**
     * Sets the username.
     * 
     * @param username the username
     * @return this command for chaining
     */
    public TestCommand setUsername(String username) {
        this.username = username;
        return this;
    }
}