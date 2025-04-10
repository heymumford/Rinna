/*
 * Administrative recovery command handler for Rinna.
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.command.impl;

import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.RecoveryService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.util.MapConverter;
import org.rinna.cli.util.OutputFormatter;
import org.rinna.cli.util.ErrorHandler;
import org.rinna.cli.util.OperationTracker;
import org.rinna.cli.util.ErrorHandler.Severity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Command handler for recovery-related operations.
 * This class implements the functionality for the 'rin admin recovery' command.
 * It follows the ViewCommand pattern with MetadataService integration for operation tracking.
 */
public class AdminRecoveryCommand implements Callable<Integer> {
    
    private String operation;
    private String[] args = new String[0];
    private final ServiceManager serviceManager;
    private final Scanner scanner;
    private final MetadataService metadataService;
    private final ErrorHandler errorHandler;
    private final OperationTracker operationTracker;
    private String format = "text";
    private boolean verbose = false;
    
    /**
     * Creates a new AdminRecoveryCommand with the specified ServiceManager.
     * 
     * @param serviceManager the service manager
     */
    public AdminRecoveryCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.metadataService = serviceManager.getMetadataService();
        this.errorHandler = new ErrorHandler(metadataService);
        this.operationTracker = new OperationTracker(metadataService);
        this.scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8.name());
    }
    
    /**
     * Sets the operation to perform.
     * 
     * @param operation the operation
     */
    public void setOperation(String operation) {
        this.operation = operation;
    }
    
    /**
     * Sets whether to use JSON output format.
     * 
     * @param jsonOutput true to use JSON output
     */
    public void setJsonOutput(boolean jsonOutput) {
        this.format = jsonOutput ? "json" : "text";
        this.errorHandler.outputFormat(this.format);
    }
    
    /**
     * Sets the output format (text or json).
     *
     * @param format the output format
     */
    public void setFormat(String format) {
        this.format = format;
        this.errorHandler.outputFormat(this.format);
    }
    
    /**
     * Sets whether to display verbose output.
     * 
     * @param verbose true for verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
        this.errorHandler.verbose(this.verbose);
    }
    
    /**
     * Sets the arguments for the operation.
     * 
     * @param args the arguments
     */
    public void setArgs(String[] args) {
        this.args = args;
    }
    
    @Override
    public Integer call() {
        // Setup operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", operation != null ? operation : "help");
        params.put("argsCount", args.length);
        if (args.length > 0) {
            params.put("arg0", args[0]);
        }
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Initialize and start the operation tracker
        String operationId = operationTracker
            .command("admin-recovery")
            .operationType("RECOVERY")
            .params(params)
            .start();
        
        try {
            if (operation == null || operation.isEmpty()) {
                displayHelp(operationId);
                return 1;
            }
            
            // Get the recovery service
            RecoveryService recoveryService = serviceManager.getRecoveryService();
            if (recoveryService == null) {
                String errorMessage = "Recovery service is not available.";
                return errorHandler.handleError(operationId, "admin-recovery", 
                    errorMessage, new IllegalStateException(errorMessage), Severity.SYSTEM);
            }
            
            // Delegate to the appropriate operation
            int result;
            switch (operation) {
                case "start":
                    result = handleStartOperation(recoveryService, operationId);
                    break;
                
                case "status":
                    result = handleStatusOperation(recoveryService, operationId);
                    break;
                
                case "plan":
                    result = handlePlanOperation(recoveryService, operationId);
                    break;
                
                case "help":
                    displayHelp(operationId);
                    result = 0;
                    break;
                
                default:
                    String errorMessage = "Unknown recovery operation: " + operation;
                    displayHelp(operationId);
                    return errorHandler.handleError(operationId, "admin-recovery", 
                        errorMessage, new IllegalArgumentException(errorMessage), Severity.VALIDATION);
            }
            
            // Complete operation if successful
            if (result == 0) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                resultData.put("operation", operation);
                errorHandler.handleSuccess(operationId, resultData);
            }
            
            return result;
        } catch (Exception e) {
            return errorHandler.handleUnexpectedError(operationId, "admin-recovery", e);
        }
    }
    
    /**
     * Handles the 'start' operation to initiate a recovery.
     * 
     * @param recoveryService the recovery service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleStartOperation(RecoveryService recoveryService, String parentOperationId) {
        Map<String, String> options = parseOptions(args);
        String backupId = options.getOrDefault("backup-id", null);
        
        // Create a sub-tracker for this operation
        OperationTracker startTracker = operationTracker
            .createSubTracker("admin-recovery-start")
            .param("operation", "start")
            .param("backupId", backupId)
            .param("format", format);
        
        // Start tracking sub-operation
        String startOperationId = startTracker.start();
        
        if (backupId == null) {
            String errorMessage = "Missing required parameter --backup-id.";
            return errorHandler.handleError(startOperationId, "admin-recovery-start", 
                errorMessage, new IllegalArgumentException(errorMessage), Severity.VALIDATION);
        }
        
        if ("json".equalsIgnoreCase(format)) {
            Map<String, Object> warningData = new HashMap<>();
            warningData.put("backupId", backupId);
            warningData.put("warning", "This will restore the system to its state at the time of backup. All data created or modified since the backup will be lost.");
            warningData.put("requiresConfirmation", true);
            
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("result", "success");
            resultData.put("operation", "start");
            resultData.put("message", "System recovery preparation");
            resultData.put("data", warningData);
            
            System.out.println(OutputFormatter.toJson(resultData, verbose));
            
            // Can't continue in JSON mode without interactive input
            errorHandler.handleSuccess(startOperationId, resultData);
            return 0;
        }
        
        System.out.println("System Recovery");
        System.out.println("===============");
        System.out.println();
        System.out.println("Preparing to restore from backup: " + backupId);
        System.out.println();
        System.out.println("WARNING: This will restore the system to its state at the time of backup.");
        System.out.println("All data created or modified since the backup will be lost.");
        System.out.println();
        System.out.print("Are you sure you want to proceed? (yes/no): ");
        
        // Create confirmation request sub-tracker
        OperationTracker confirmTracker = operationTracker
            .createSubTracker("admin-recovery-confirmation-request")
            .param("confirmationRequested", true)
            .param("backupId", backupId);
        
        String confirmOpId = confirmTracker.start();
        
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        // Create confirmation response sub-tracker
        OperationTracker responseTracker = operationTracker
            .createSubTracker("admin-recovery-confirmation-response")
            .param("confirmation", confirm)
            .param("backupId", backupId);
        
        String responseOpId = responseTracker.start();
        errorHandler.handleSuccess(responseOpId, new HashMap<>());
        
        if (!"yes".equals(confirm) && !"y".equals(confirm)) {
            String cancelMessage = "Recovery cancelled.";
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> successData = new HashMap<>();
                successData.put("result", "success");
                successData.put("message", cancelMessage);
                System.out.println(OutputFormatter.toJson(successData, verbose));
            } else {
                System.out.println(cancelMessage);
            }
            
            // Track cancellation
            Map<String, Object> cancelData = new HashMap<>();
            cancelData.put("cancelled", true);
            cancelData.put("backupId", backupId);
            errorHandler.handleSuccess(startOperationId, cancelData);
            errorHandler.handleSuccess(confirmOpId, cancelData);
            
            return 0;
        }
        
        // Successfully completed confirmation
        errorHandler.handleSuccess(confirmOpId, new HashMap<String, Object>() {{
            put("confirmed", true);
            put("backupId", backupId);
        }});
        
        // Create execute recovery sub-tracker
        OperationTracker executeTracker = operationTracker
            .createSubTracker("admin-recovery-execute")
            .param("backupId", backupId)
            .param("confirmed", true);
        
        String executeOperationId = executeTracker.start();
        
        try {
            System.out.println();
            System.out.println("Starting recovery process...");
            System.out.println("This may take several minutes. Please do not interrupt the process.");
            System.out.println();
            
            boolean success = recoveryService.startRecovery(backupId);
            
            if (success) {
                String successMessage = "Recovery completed successfully!";
                
                if ("json".equalsIgnoreCase(format)) {
                    Map<String, Object> successData = new HashMap<>();
                    successData.put("result", "success");
                    successData.put("message", successMessage);
                    System.out.println(OutputFormatter.toJson(successData, verbose));
                } else {
                    System.out.println(successMessage);
                }
                
                // Track successful recovery
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                resultData.put("backupId", backupId);
                errorHandler.handleSuccess(executeOperationId, resultData);
                errorHandler.handleSuccess(startOperationId, resultData);
                
                return 0;
            } else {
                String errorMessage = "Recovery failed. See log for details.";
                errorHandler.handleError(executeOperationId, "admin-recovery-execute", 
                    errorMessage, new RuntimeException(errorMessage), Severity.ERROR);
                return errorHandler.handleError(startOperationId, "admin-recovery-start", 
                    errorMessage, new RuntimeException(errorMessage), Severity.ERROR);
            }
        } catch (Exception e) {
            String errorMessage = "Error during recovery: " + e.getMessage();
            errorHandler.handleError(executeOperationId, "admin-recovery-execute", errorMessage, e, Severity.SYSTEM);
            return errorHandler.handleError(startOperationId, "admin-recovery-start", errorMessage, e, Severity.SYSTEM);
        }
    }
    
    /**
     * Handles the 'status' operation to display recovery status.
     * 
     * @param recoveryService the recovery service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleStatusOperation(RecoveryService recoveryService, String parentOperationId) {
        // Create a sub-tracker for this operation
        OperationTracker statusTracker = operationTracker
            .createSubTracker("admin-recovery-status")
            .param("operation", "status")
            .param("format", format);
        
        // Start tracking sub-operation
        String statusOperationId = statusTracker.start();
        
        try {
            String status = recoveryService.getRecoveryStatus();
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "status");
                resultData.put("status", status);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println(status);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("status", status);
            return errorHandler.handleSuccess(statusOperationId, resultData);
        } catch (Exception e) {
            String errorMessage = "Error getting recovery status: " + e.getMessage();
            return errorHandler.handleError(statusOperationId, "admin-recovery-status", 
                errorMessage, e, Severity.SYSTEM);
        }
    }
    
    /**
     * Handles the 'plan' operation to manage disaster recovery plans.
     * 
     * @param recoveryService the recovery service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handlePlanOperation(RecoveryService recoveryService, String parentOperationId) {
        // Create a sub-tracker for this operation
        OperationTracker planTracker = operationTracker
            .createSubTracker("admin-recovery-plan")
            .param("operation", "plan")
            .param("format", format);
        
        // Start tracking sub-operation
        String planOperationId = planTracker.start();
        
        if (args.length == 0) {
            String errorMessage = "Missing plan subcommand. Use 'generate' or 'test'.";
            Map<String, String> validationErrors = new HashMap<>();
            validationErrors.put("subcommand", "Missing required subcommand");
            return errorHandler.handleValidationError(planOperationId, "admin-recovery-plan", validationErrors);
        }
        
        String planOperation = args[0];
        planTracker.param("planOperation", planOperation);
        
        if ("generate".equals(planOperation)) {
            return handlePlanGenerateOperation(recoveryService, planOperationId);
        } else if ("test".equals(planOperation)) {
            return handlePlanTestOperation(recoveryService, planOperationId);
        } else {
            String errorMessage = "Unknown plan operation: " + planOperation;
            
            // For invalid subcommands, use a validation error with specific details
            Map<String, String> validationErrors = new HashMap<>();
            validationErrors.put("subcommand", "Invalid operation: " + planOperation);
            validationErrors.put("validOptions", "Valid operations are: generate, test");
            
            // Output additional information for users about valid operations
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "error");
                resultData.put("message", errorMessage);
                resultData.put("validOperations", new String[]{"generate", "test"});
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println("Valid operations: generate, test");
            }
            
            return errorHandler.handleValidationError(planOperationId, "admin-recovery-plan", validationErrors);
        }
    }
    
    /**
     * Handles the 'plan generate' operation to generate a recovery plan.
     * 
     * @param recoveryService the recovery service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handlePlanGenerateOperation(RecoveryService recoveryService, String parentOperationId) {
        // Create a sub-tracker for this operation
        OperationTracker generateTracker = operationTracker
            .createSubTracker("admin-recovery-plan-generate")
            .param("operation", "plan-generate")
            .param("format", format);
        
        // Start tracking sub-operation
        String generateOperationId = generateTracker.start();
        
        try {
            String planPath = recoveryService.generateRecoveryPlan();
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "plan-generate");
                resultData.put("planPath", planPath);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println("Recovery plan generated successfully!");
                System.out.println("Plan saved to: " + planPath);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("planPath", planPath);
            return errorHandler.handleSuccess(generateOperationId, resultData);
        } catch (Exception e) {
            String errorMessage = "Error generating recovery plan: " + e.getMessage();
            return errorHandler.handleError(generateOperationId, "admin-recovery-plan-generate", 
                errorMessage, e, Severity.ERROR);
        }
    }
    
    /**
     * Handles the 'plan test' operation to test a recovery plan.
     * 
     * @param recoveryService the recovery service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handlePlanTestOperation(RecoveryService recoveryService, String parentOperationId) {
        Map<String, String> options = parseOptions(args);
        boolean simulation = options.containsKey("simulation");
        
        // Create a sub-tracker for this operation
        OperationTracker testTracker = operationTracker
            .createSubTracker("admin-recovery-plan-test")
            .param("operation", "plan-test")
            .param("simulation", simulation)
            .param("format", format);
        
        // Start tracking sub-operation
        String testOperationId = testTracker.start();
        
        try {
            String results = recoveryService.testRecoveryPlan(simulation);
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "plan-test");
                resultData.put("simulation", simulation);
                resultData.put("testResults", results);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println(results);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("simulation", simulation);
            resultData.put("results", results);
            return errorHandler.handleSuccess(testOperationId, resultData);
        } catch (Exception e) {
            String errorMessage = "Error testing recovery plan: " + e.getMessage();
            return errorHandler.handleError(testOperationId, "admin-recovery-plan-test", 
                errorMessage, e, Severity.ERROR);
        }
    }
    
    /**
     * Parses command line arguments into a map of options.
     * 
     * @param args the command line arguments
     * @return a map of option names to values
     */
    private Map<String, String> parseOptions(String[] args) {
        Map<String, String> options = new HashMap<>();
        
        for (String arg : args) {
            if (arg.startsWith("--")) {
                String option = arg.substring(2);
                String name;
                String value = null;
                
                int equalsIndex = option.indexOf('=');
                if (equalsIndex != -1) {
                    name = option.substring(0, equalsIndex);
                    value = option.substring(equalsIndex + 1);
                } else {
                    name = option;
                }
                
                options.put(name, value);
            }
        }
        
        return options;
    }
    
    /**
     * Displays help information for recovery commands.
     * 
     * @param operationId the parent operation ID for tracking
     */
    private void displayHelp(String operationId) {
        // Create a sub-tracker for this operation
        OperationTracker helpTracker = operationTracker
            .createSubTracker("admin-recovery-help")
            .param("command", "admin recovery")
            .param("action", "help")
            .param("format", format);
        
        // Start tracking sub-operation
        String helpOperationId = helpTracker.start();
        
        try {
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> jsonHelpData = new HashMap<>();
                jsonHelpData.put("result", "success");
                jsonHelpData.put("command", "admin recovery");
                jsonHelpData.put("usage", "rin admin recovery <operation> [options]");
                
                List<Map<String, String>> operations = new ArrayList<>();
                operations.add(createInfoMap("start", "Initiate system recovery from backup"));
                operations.add(createInfoMap("status", "Display recovery status"));
                operations.add(createInfoMap("plan", "Manage disaster recovery plans"));
                jsonHelpData.put("operations", operations);
                
                Map<String, List<Map<String, String>>> operationOptions = new HashMap<>();
                
                List<Map<String, String>> startOptions = new ArrayList<>();
                startOptions.add(createInfoMap("--backup-id=<id>", "ID of backup to recover from"));
                operationOptions.put("start", startOptions);
                
                List<Map<String, String>> planOptions = new ArrayList<>();
                planOptions.add(createInfoMap("generate", "Generate a new recovery plan"));
                planOptions.add(createInfoMap("test", "Test a recovery plan"));
                operationOptions.put("plan", planOptions);
                
                List<Map<String, String>> planTestOptions = new ArrayList<>();
                planTestOptions.add(createInfoMap("--simulation", "Run a simulated recovery test"));
                operationOptions.put("plan test", planTestOptions);
                
                jsonHelpData.put("operation_options", operationOptions);
                
                System.out.println(OutputFormatter.toJson(jsonHelpData, verbose));
            } else {
                System.out.println("Usage: rin admin recovery <operation> [options]");
                System.out.println();
                System.out.println("Operations:");
                System.out.println("  start  - Initiate system recovery from backup");
                System.out.println("  status - Display recovery status");
                System.out.println("  plan   - Manage disaster recovery plans");
                System.out.println();
                System.out.println("Options for 'start':");
                System.out.println("  --backup-id=<id> - ID of backup to recover from");
                System.out.println();
                System.out.println("Options for 'plan test':");
                System.out.println("  --simulation     - Run a simulated recovery test");
                System.out.println();
                System.out.println("For detailed help on a specific operation, use:");
                System.out.println("  rin admin recovery <operation> help");
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("format", format);
            errorHandler.handleSuccess(helpOperationId, resultData);
        } catch (Exception e) {
            errorHandler.handleUnexpectedError(helpOperationId, "admin-recovery-help", e);
            throw e; // Rethrow to be caught by caller
        }
    }
    
    /**
     * Creates a map with name and description keys.
     *
     * @param name the name
     * @param description the description
     * @return the map
     */
    private Map<String, String> createInfoMap(String name, String description) {
        Map<String, String> map = new HashMap<>();
        map.put("name", name);
        map.put("description", description);
        return map;
    }
}