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
import org.rinna.cli.util.OutputFormatter;

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
    }
    
    /**
     * Sets the output format (text or json).
     *
     * @param format the output format
     */
    public void setFormat(String format) {
        this.format = format;
    }
    
    /**
     * Sets whether to display verbose output.
     * 
     * @param verbose true for verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
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
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", operation != null ? operation : "help");
        params.put("argsCount", args.length);
        if (args.length > 0) {
            params.put("arg0", args[0]);
        }
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking operation
        String operationId = metadataService.startOperation("admin-recovery", "RECOVERY", params);
        
        try {
            if (operation == null || operation.isEmpty()) {
                displayHelp(operationId);
                return 1;
            }
            
            // Get the recovery service
            RecoveryService recoveryService = serviceManager.getRecoveryService();
            if (recoveryService == null) {
                String errorMessage = "Recovery service is not available.";
                outputError(errorMessage);
                metadataService.failOperation(operationId, new IllegalStateException(errorMessage));
                return 1;
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
                    outputError(errorMessage);
                    displayHelp(operationId);
                    metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                    result = 1;
                    break;
            }
            
            // Complete operation if successful
            if (result == 0) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                resultData.put("operation", operation);
                metadataService.completeOperation(operationId, resultData);
            }
            
            return result;
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(operationId, e);
            return 1;
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
        
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "start");
        params.put("backupId", backupId);
        params.put("format", format);
        
        // Start tracking sub-operation
        String startOperationId = metadataService.trackOperation("admin-recovery-start", params);
        
        if (backupId == null) {
            String errorMessage = "Missing required parameter --backup-id.";
            outputError(errorMessage);
            metadataService.failOperation(startOperationId, new IllegalArgumentException(errorMessage));
            return 1;
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
            metadataService.completeOperation(startOperationId, resultData);
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
        
        // Track confirmation request
        Map<String, Object> confirmParams = new HashMap<>();
        confirmParams.put("confirmationRequested", true);
        confirmParams.put("backupId", backupId);
        metadataService.trackOperationWithData("admin-recovery-confirmation-request", confirmParams);
        
        String confirm = scanner.nextLine().trim().toLowerCase();
        
        // Track confirmation response
        Map<String, Object> confirmResponseParams = new HashMap<>();
        confirmResponseParams.put("confirmation", confirm);
        confirmResponseParams.put("backupId", backupId);
        metadataService.trackOperationWithData("admin-recovery-confirmation-response", confirmResponseParams);
        
        if (!"yes".equals(confirm) && !"y".equals(confirm)) {
            String cancelMessage = "Recovery cancelled.";
            outputSuccess(cancelMessage);
            
            // Track cancellation
            Map<String, Object> cancelData = new HashMap<>();
            cancelData.put("cancelled", true);
            cancelData.put("backupId", backupId);
            metadataService.completeOperation(startOperationId, cancelData);
            
            return 0;
        }
        
        // Execute recovery with tracking
        Map<String, Object> recoveryParams = new HashMap<>();
        recoveryParams.put("backupId", backupId);
        recoveryParams.put("confirmed", true);
        String executeOperationId = metadataService.trackOperation("admin-recovery-execute", recoveryParams);
        
        try {
            System.out.println();
            System.out.println("Starting recovery process...");
            System.out.println("This may take several minutes. Please do not interrupt the process.");
            System.out.println();
            
            boolean success = recoveryService.startRecovery(backupId);
            
            if (success) {
                String successMessage = "Recovery completed successfully!";
                outputSuccess(successMessage);
                
                // Track successful recovery
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                resultData.put("backupId", backupId);
                metadataService.completeOperation(executeOperationId, resultData);
                metadataService.completeOperation(startOperationId, resultData);
                
                return 0;
            } else {
                String errorMessage = "Recovery failed. See log for details.";
                outputError(errorMessage);
                metadataService.failOperation(executeOperationId, new RuntimeException(errorMessage));
                metadataService.failOperation(startOperationId, new RuntimeException(errorMessage));
                return 1;
            }
        } catch (Exception e) {
            String errorMessage = "Error during recovery: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(executeOperationId, e);
            metadataService.failOperation(startOperationId, e);
            return 1;
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
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "status");
        params.put("format", format);
        
        // Start tracking sub-operation
        String statusOperationId = metadataService.trackOperation("admin-recovery-status", params);
        
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
            metadataService.completeOperation(statusOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error getting recovery status: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(statusOperationId, e);
            return 1;
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
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "plan");
        params.put("format", format);
        
        // Start tracking sub-operation
        String planOperationId = metadataService.trackOperation("admin-recovery-plan", params);
        
        if (args.length == 0) {
            String errorMessage = "Missing plan subcommand. Use 'generate' or 'test'.";
            outputError(errorMessage);
            metadataService.failOperation(planOperationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        String planOperation = args[0];
        params.put("planOperation", planOperation);
        
        if ("generate".equals(planOperation)) {
            return handlePlanGenerateOperation(recoveryService, planOperationId);
        } else if ("test".equals(planOperation)) {
            return handlePlanTestOperation(recoveryService, planOperationId);
        } else {
            String errorMessage = "Unknown plan operation: " + planOperation;
            outputError(errorMessage);
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "error");
                resultData.put("message", errorMessage);
                resultData.put("validOperations", new String[]{"generate", "test"});
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println("Valid operations: generate, test");
            }
            
            metadataService.failOperation(planOperationId, new IllegalArgumentException(errorMessage));
            return 1;
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
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "plan-generate");
        params.put("format", format);
        
        // Start tracking sub-operation
        String generateOperationId = metadataService.trackOperation("admin-recovery-plan-generate", params);
        
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
            metadataService.completeOperation(generateOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error generating recovery plan: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(generateOperationId, e);
            return 1;
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
        
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "plan-test");
        params.put("simulation", simulation);
        params.put("format", format);
        
        // Start tracking sub-operation
        String testOperationId = metadataService.trackOperation("admin-recovery-plan-test", params);
        
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
            metadataService.completeOperation(testOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error testing recovery plan: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(testOperationId, e);
            return 1;
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
        // Create operation parameters for tracking
        Map<String, Object> helpData = new HashMap<>();
        helpData.put("command", "admin recovery");
        helpData.put("action", "help");
        helpData.put("format", format);
        
        // Start tracking sub-operation
        String helpOperationId = metadataService.trackOperation("admin-recovery-help", helpData);
        
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
            metadataService.completeOperation(helpOperationId, resultData);
        } catch (Exception e) {
            metadataService.failOperation(helpOperationId, e);
            throw e; // Rethrow to be caught by caller
        }
    }
    
    /**
     * Outputs an error message in either JSON or text format.
     *
     * @param message the error message
     */
    private void outputError(String message) {
        if ("json".equalsIgnoreCase(format)) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("result", "error");
            errorData.put("message", message);
            System.out.println(OutputFormatter.toJson(errorData, verbose));
        } else {
            System.err.println("Error: " + message);
        }
    }
    
    /**
     * Outputs a success message or data in either JSON or text format.
     *
     * @param message the success message or data
     */
    private void outputSuccess(String message) {
        if ("json".equalsIgnoreCase(format)) {
            Map<String, Object> successData = new HashMap<>();
            successData.put("result", "success");
            successData.put("message", message);
            System.out.println(OutputFormatter.toJson(successData, verbose));
        } else {
            System.out.println(message);
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