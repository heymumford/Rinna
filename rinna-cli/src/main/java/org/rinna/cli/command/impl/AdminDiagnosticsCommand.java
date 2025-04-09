/*
 * Administrative diagnostics command handler for Rinna.
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.command.impl;

import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.DiagnosticsService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.util.OutputFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Command handler for system diagnostics operations.
 * This class implements the functionality for the 'rin admin diagnostics' command.
 * It follows the ViewCommand pattern with MetadataService integration for operation tracking.
 */
public class AdminDiagnosticsCommand implements Callable<Integer> {
    
    private String operation;
    private String[] args = new String[0];
    private final ServiceManager serviceManager;
    private final Scanner scanner;
    private final MetadataService metadataService;
    private String format = "text";
    private boolean verbose = false;
    
    /**
     * Creates a new AdminDiagnosticsCommand with the specified ServiceManager.
     * 
     * @param serviceManager the service manager
     */
    public AdminDiagnosticsCommand(ServiceManager serviceManager) {
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
     * Sets the arguments for the operation.
     * 
     * @param args the arguments
     */
    public void setArgs(String[] args) {
        this.args = args;
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
        String operationId = metadataService.startOperation("admin-diagnostics", "DIAGNOSTICS", params);
        
        try {
            if (operation == null || operation.isEmpty()) {
                displayHelp(operationId);
                return 1;
            }
            
            // Get the diagnostics service
            DiagnosticsService diagnosticsService = serviceManager.getDiagnosticsService();
            if (diagnosticsService == null) {
                String errorMessage = "Diagnostics service is not available.";
                outputError(errorMessage);
                metadataService.failOperation(operationId, new IllegalStateException(errorMessage));
                return 1;
            }
            
            // Delegate to the appropriate operation
            int result;
            switch (operation) {
                case "run":
                    result = handleRunOperation(diagnosticsService, operationId);
                    break;
                
                case "schedule":
                    result = handleScheduleOperation(diagnosticsService, operationId);
                    break;
                
                case "database":
                    result = handleDatabaseOperation(diagnosticsService, operationId);
                    break;
                
                case "warning":
                    result = handleWarningOperation(diagnosticsService, operationId);
                    break;
                
                case "action":
                    result = handleActionOperation(diagnosticsService, operationId);
                    break;
                
                case "help":
                    displayHelp(operationId);
                    result = 0;
                    break;
                
                default:
                    String errorMessage = "Unknown diagnostics operation: " + operation;
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
     * Handles the 'run' operation to execute diagnostics.
     * 
     * @param diagnosticsService the diagnostics service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleRunOperation(DiagnosticsService diagnosticsService, String parentOperationId) {
        Map<String, String> options = parseOptions(args);
        boolean full = options.containsKey("full");
        
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "run");
        params.put("full", full);
        params.put("format", format);
        
        // Start tracking sub-operation
        String runOperationId = metadataService.trackOperation("admin-diagnostics-run", params);
        
        try {
            String results = diagnosticsService.runDiagnostics(full);
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "run");
                resultData.put("full", full);
                resultData.put("diagnostics", results);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println(results);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("full", full);
            metadataService.completeOperation(runOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error running diagnostics: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(runOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'schedule' operation to schedule diagnostic checks.
     * 
     * @param diagnosticsService the diagnostics service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleScheduleOperation(DiagnosticsService diagnosticsService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "schedule");
        params.put("format", format);
        
        // Start tracking sub-operation
        String scheduleOperationId = metadataService.trackOperation("admin-diagnostics-schedule", params);
        
        if (args.length > 0 && "list".equals(args[0])) {
            return handleScheduleListOperation(diagnosticsService, scheduleOperationId);
        }
        
        try {
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> typeOptions = new HashMap<>();
                typeOptions.put("api", "API Server checks");
                typeOptions.put("database", "Database checks");
                typeOptions.put("storage", "File storage checks");
                typeOptions.put("memory", "Memory management checks");
                typeOptions.put("threads", "Thread pool checks");
                typeOptions.put("network", "Network diagnostics");
                typeOptions.put("all", "All diagnostics");
                
                Map<String, String> frequencyOptions = new HashMap<>();
                frequencyOptions.put("1", "hourly - Run every hour");
                frequencyOptions.put("2", "daily - Run once per day");
                frequencyOptions.put("3", "weekly - Run once per week");
                frequencyOptions.put("4", "monthly - Run once per month");
                
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "schedule");
                resultData.put("message", "Input required for scheduling diagnostics");
                resultData.put("checkTypes", typeOptions);
                resultData.put("frequencies", frequencyOptions);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
                
                // Can't continue in JSON mode without interactive input
                metadataService.completeOperation(scheduleOperationId, resultData);
                return 0;
            }
            
            System.out.println("Schedule Recurring Diagnostics");
            System.out.println("===========================");
            System.out.println();
            
            System.out.println("Select diagnostic check types (comma-separated):");
            System.out.println("- api: API Server checks");
            System.out.println("- database: Database checks");
            System.out.println("- storage: File storage checks");
            System.out.println("- memory: Memory management checks");
            System.out.println("- threads: Thread pool checks");
            System.out.println("- network: Network diagnostics");
            System.out.println("- all: All diagnostics");
            System.out.print("Enter check types: ");
            
            String checksInput = scanner.nextLine().trim();
            
            if (checksInput.isEmpty()) {
                String errorMessage = "Check types cannot be empty.";
                outputError(errorMessage);
                metadataService.failOperation(scheduleOperationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            List<String> checks = Arrays.asList(checksInput.split("\\s*,\\s*"));
            
            // Track check types selection
            Map<String, Object> checkParams = new HashMap<>();
            checkParams.put("checks", checks);
            metadataService.trackOperationWithData("admin-diagnostics-schedule-checks", checkParams);
            
            System.out.println("Select schedule frequency:");
            System.out.println("1. hourly - Run every hour");
            System.out.println("2. daily - Run once per day");
            System.out.println("3. weekly - Run once per week");
            System.out.println("4. monthly - Run once per month");
            System.out.print("Enter choice [2]: ");
            
            String frequencyInput = scanner.nextLine().trim();
            String frequency = "daily"; // Default
            
            if (!frequencyInput.isEmpty()) {
                switch (frequencyInput) {
                    case "1":
                        frequency = "hourly";
                        break;
                    case "2":
                        frequency = "daily";
                        break;
                    case "3":
                        frequency = "weekly";
                        break;
                    case "4":
                        frequency = "monthly";
                        break;
                    default:
                        if ("hourly".equals(frequencyInput) || "daily".equals(frequencyInput) || 
                            "weekly".equals(frequencyInput) || "monthly".equals(frequencyInput)) {
                            frequency = frequencyInput;
                        } else {
                            System.err.println("Error: Invalid frequency. Using default (daily).");
                        }
                        break;
                }
            }
            
            // Track frequency selection
            Map<String, Object> frequencyParams = new HashMap<>();
            frequencyParams.put("frequency", frequency);
            metadataService.trackOperationWithData("admin-diagnostics-schedule-frequency", frequencyParams);
            
            String timePrompt = "hourly".equals(frequency) 
                ? "Enter schedule minute (0-59) [0]: " 
                : "Enter schedule time (HH:MM) [02:00]: ";
            
            System.out.print(timePrompt);
            
            String timeInput = scanner.nextLine().trim();
            String time = "hourly".equals(frequency) ? "0" : "02:00"; // Default
            
            if (!timeInput.isEmpty()) {
                time = timeInput;
            }
            
            // Track time selection
            Map<String, Object> timeParams = new HashMap<>();
            timeParams.put("time", time);
            metadataService.trackOperationWithData("admin-diagnostics-schedule-time", timeParams);
            
            System.out.print("Enter notification recipients (comma-separated email addresses): ");
            String recipientsInput = scanner.nextLine().trim();
            
            List<String> recipients = Arrays.asList(recipientsInput.split("\\s*,\\s*"));
            
            // Track recipients selection
            Map<String, Object> recipientsParams = new HashMap<>();
            recipientsParams.put("recipients", recipients);
            metadataService.trackOperationWithData("admin-diagnostics-schedule-recipients", recipientsParams);
            
            // Execute schedule creation with tracking
            Map<String, Object> createParams = new HashMap<>();
            createParams.put("checks", checks);
            createParams.put("frequency", frequency);
            createParams.put("time", time);
            createParams.put("recipients", recipients);
            String createOperationId = metadataService.trackOperation("admin-diagnostics-schedule-create", createParams);
            
            try {
                String taskId = diagnosticsService.scheduleDiagnostics(checks, frequency, time, recipients);
                
                String successMessage = "Diagnostic task scheduled successfully!\nTask ID: " + taskId;
                outputSuccess(successMessage);
                
                // Track successful creation
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                resultData.put("taskId", taskId);
                resultData.put("checks", checks);
                resultData.put("frequency", frequency);
                resultData.put("time", time);
                resultData.put("recipients", recipients);
                metadataService.completeOperation(createOperationId, resultData);
                metadataService.completeOperation(scheduleOperationId, resultData);
                
                return 0;
            } catch (Exception e) {
                String errorMessage = "Error scheduling diagnostics: " + e.getMessage();
                outputError(errorMessage);
                if (verbose) {
                    e.printStackTrace();
                }
                metadataService.failOperation(createOperationId, e);
                metadataService.failOperation(scheduleOperationId, e);
                return 1;
            }
        } catch (Exception e) {
            String errorMessage = "Error in schedule operation: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(scheduleOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'schedule list' operation to list scheduled diagnostic tasks.
     * 
     * @param diagnosticsService the diagnostics service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleScheduleListOperation(DiagnosticsService diagnosticsService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "schedule-list");
        params.put("format", format);
        
        // Start tracking sub-operation
        String listOperationId = metadataService.trackOperation("admin-diagnostics-schedule-list", params);
        
        try {
            String schedules = diagnosticsService.listScheduledDiagnostics();
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "schedule-list");
                resultData.put("schedules", schedules);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println(schedules);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            metadataService.completeOperation(listOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error listing scheduled diagnostics: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(listOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'database' operation to analyze database performance.
     * 
     * @param diagnosticsService the diagnostics service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleDatabaseOperation(DiagnosticsService diagnosticsService, String parentOperationId) {
        Map<String, String> options = parseOptions(args);
        boolean analyze = options.containsKey("analyze");
        
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "database");
        params.put("analyze", analyze);
        params.put("format", format);
        
        // Start tracking sub-operation
        String dbOperationId = metadataService.trackOperation("admin-diagnostics-database", params);
        
        if (!analyze) {
            String errorMessage = "Missing required flag --analyze.";
            outputError(errorMessage);
            metadataService.failOperation(dbOperationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        try {
            String report = diagnosticsService.analyzeDatabasePerformance();
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "database");
                resultData.put("analyze", true);
                resultData.put("report", report);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println(report);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("analyze", true);
            metadataService.completeOperation(dbOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error analyzing database performance: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(dbOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'warning' operation to resolve system warnings.
     * 
     * @param diagnosticsService the diagnostics service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleWarningOperation(DiagnosticsService diagnosticsService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "warning");
        params.put("format", format);
        
        if (args.length > 0) {
            params.put("subcommand", args[0]);
        }
        
        // Start tracking sub-operation
        String warningOperationId = metadataService.trackOperation("admin-diagnostics-warning", params);
        
        if (args.length == 0 || !"resolve".equals(args[0])) {
            String errorMessage = "Missing 'resolve' subcommand.";
            outputError(errorMessage);
            metadataService.failOperation(warningOperationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        Map<String, String> options = parseOptions(args);
        String warningId = options.getOrDefault("id", null);
        
        if (warningId == null) {
            String errorMessage = "Missing required parameter --id.";
            outputError(errorMessage);
            metadataService.failOperation(warningOperationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        // Update operation parameters with warning ID
        Map<String, Object> idParams = new HashMap<>();
        idParams.put("warningId", warningId);
        metadataService.trackOperationWithData("admin-diagnostics-warning-id", idParams);
        
        try {
            // Get warning details with tracking
            Map<String, Object> detailsParams = new HashMap<>();
            detailsParams.put("warningId", warningId);
            String detailsOperationId = metadataService.trackOperation("admin-diagnostics-warning-get-details", detailsParams);
            
            Map<String, String> warningDetails;
            try {
                warningDetails = diagnosticsService.getWarningDetails(warningId);
                
                if (warningDetails.isEmpty()) {
                    String errorMessage = "Warning not found: " + warningId;
                    outputError(errorMessage);
                    metadataService.failOperation(detailsOperationId, new IllegalArgumentException(errorMessage));
                    metadataService.failOperation(warningOperationId, new IllegalArgumentException(errorMessage));
                    return 1;
                }
                
                // Track successful retrieval
                metadataService.completeOperation(detailsOperationId, warningDetails);
            } catch (Exception e) {
                String errorMessage = "Error retrieving warning details: " + e.getMessage();
                outputError(errorMessage);
                if (verbose) {
                    e.printStackTrace();
                }
                metadataService.failOperation(detailsOperationId, e);
                metadataService.failOperation(warningOperationId, e);
                return 1;
            }
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> warningData = new HashMap<>();
                warningData.put("warningId", warningId);
                warningData.putAll(warningDetails);
                
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "warning-resolve");
                resultData.put("warning", warningData);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
                
                // Can't continue in JSON mode without interactive input
                metadataService.completeOperation(warningOperationId, resultData);
                return 0;
            }
            
            System.out.println("Warning Resolution: " + warningId);
            System.out.println("==============================");
            System.out.println();
            System.out.println("Warning Type: " + warningDetails.getOrDefault("type", "Unknown"));
            System.out.println("Timestamp: " + warningDetails.getOrDefault("timestamp", "Unknown"));
            System.out.println("Severity: " + warningDetails.getOrDefault("severity", "Unknown"));
            System.out.println("Description: " + warningDetails.getOrDefault("description", "Unknown"));
            System.out.println();
            
            // Get available actions with tracking
            Map<String, Object> actionsParams = new HashMap<>();
            actionsParams.put("warningId", warningId);
            String actionsOperationId = metadataService.trackOperation("admin-diagnostics-warning-get-actions", actionsParams);
            
            List<String> availableActions;
            try {
                availableActions = diagnosticsService.getAvailableWarningActions(warningId);
                
                if (availableActions.isEmpty()) {
                    outputSuccess("No available actions for this warning.");
                    
                    // Track successful retrieval with no actions
                    Map<String, Object> noActionsResult = new HashMap<>();
                    noActionsResult.put("success", true);
                    noActionsResult.put("warningId", warningId);
                    noActionsResult.put("actionsCount", 0);
                    metadataService.completeOperation(actionsOperationId, noActionsResult);
                    metadataService.completeOperation(warningOperationId, noActionsResult);
                    
                    return 0;
                }
                
                // Track successful retrieval of actions
                Map<String, Object> actionsResult = new HashMap<>();
                actionsResult.put("success", true);
                actionsResult.put("warningId", warningId);
                actionsResult.put("actionsCount", availableActions.size());
                actionsResult.put("actions", availableActions);
                metadataService.completeOperation(actionsOperationId, actionsResult);
                
            } catch (Exception e) {
                String errorMessage = "Error retrieving available actions: " + e.getMessage();
                outputError(errorMessage);
                if (verbose) {
                    e.printStackTrace();
                }
                metadataService.failOperation(actionsOperationId, e);
                metadataService.failOperation(warningOperationId, e);
                return 1;
            }
            
            System.out.println("Available Actions:");
            int i = 1;
            for (String action : availableActions) {
                System.out.println(i++ + ". " + action);
            }
            System.out.println();
            
            System.out.print("Select an action to perform: ");
            String actionInput = scanner.nextLine().trim();
            int actionIndex;
            
            try {
                actionIndex = Integer.parseInt(actionInput) - 1;
            } catch (NumberFormatException e) {
                String errorMessage = "Invalid selection.";
                outputError(errorMessage);
                metadataService.failOperation(warningOperationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            if (actionIndex < 0 || actionIndex >= availableActions.size()) {
                String errorMessage = "Invalid selection.";
                outputError(errorMessage);
                metadataService.failOperation(warningOperationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            String selectedAction = availableActions.get(actionIndex);
            
            // Track action selection
            Map<String, Object> selectionParams = new HashMap<>();
            selectionParams.put("warningId", warningId);
            selectionParams.put("selectedAction", selectedAction);
            selectionParams.put("actionIndex", actionIndex);
            metadataService.trackOperationWithData("admin-diagnostics-warning-select-action", selectionParams);
            
            System.out.println();
            System.out.println("Performing action: " + selectedAction);
            System.out.println();
            
            // Perform warning action with tracking
            Map<String, Object> performParams = new HashMap<>();
            performParams.put("warningId", warningId);
            performParams.put("action", selectedAction);
            String performOperationId = metadataService.trackOperation("admin-diagnostics-warning-perform-action", performParams);
            
            try {
                boolean success = diagnosticsService.performWarningAction(warningId, selectedAction);
                
                if (success) {
                    String successMessage = "Action performed successfully!";
                    outputSuccess(successMessage);
                    
                    // Track successful action
                    Map<String, Object> actionResult = new HashMap<>();
                    actionResult.put("success", true);
                    actionResult.put("warningId", warningId);
                    actionResult.put("action", selectedAction);
                    metadataService.completeOperation(performOperationId, actionResult);
                    metadataService.completeOperation(warningOperationId, actionResult);
                    
                    return 0;
                } else {
                    String errorMessage = "Failed to perform action.";
                    outputError(errorMessage);
                    metadataService.failOperation(performOperationId, new RuntimeException(errorMessage));
                    metadataService.failOperation(warningOperationId, new RuntimeException(errorMessage));
                    return 1;
                }
            } catch (Exception e) {
                String errorMessage = "Error performing action: " + e.getMessage();
                outputError(errorMessage);
                if (verbose) {
                    e.printStackTrace();
                }
                metadataService.failOperation(performOperationId, e);
                metadataService.failOperation(warningOperationId, e);
                return 1;
            }
        } catch (Exception e) {
            String errorMessage = "Error resolving warning: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(warningOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'action' operation to perform diagnostic actions.
     * 
     * @param diagnosticsService the diagnostics service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleActionOperation(DiagnosticsService diagnosticsService, String parentOperationId) {
        Map<String, String> options = parseOptions(args);
        
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "action");
        params.put("format", format);
        params.put("options", options);
        
        // Start tracking sub-operation
        String actionOperationId = metadataService.trackOperation("admin-diagnostics-action", params);
        
        if (options.containsKey("memory-reclaim")) {
            // Create memory reclaim operation tracking
            Map<String, Object> reclaimParams = new HashMap<>();
            reclaimParams.put("action", "memory-reclaim");
            String reclaimOperationId = metadataService.trackOperation("admin-diagnostics-action-memory-reclaim", reclaimParams);
            
            try {
                boolean success = diagnosticsService.performMemoryReclamation();
                
                if (success) {
                    String successMessage = "Memory reclamation completed successfully!";
                    outputSuccess(successMessage);
                    
                    // Track successful action
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("success", true);
                    resultData.put("action", "memory-reclaim");
                    metadataService.completeOperation(reclaimOperationId, resultData);
                    metadataService.completeOperation(actionOperationId, resultData);
                    
                    return 0;
                } else {
                    String errorMessage = "Memory reclamation failed.";
                    outputError(errorMessage);
                    metadataService.failOperation(reclaimOperationId, new RuntimeException(errorMessage));
                    metadataService.failOperation(actionOperationId, new RuntimeException(errorMessage));
                    return 1;
                }
            } catch (Exception e) {
                String errorMessage = "Error performing memory reclamation: " + e.getMessage();
                outputError(errorMessage);
                if (verbose) {
                    e.printStackTrace();
                }
                metadataService.failOperation(reclaimOperationId, e);
                metadataService.failOperation(actionOperationId, e);
                return 1;
            }
        } else {
            String errorMessage = "Missing required action flag.";
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> availableActions = new HashMap<>();
                availableActions.put("memory-reclaim", "Reclaim system memory");
                
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "error");
                resultData.put("message", errorMessage);
                resultData.put("availableActions", availableActions);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                outputError(errorMessage);
                System.out.println("Available actions: --memory-reclaim");
            }
            
            metadataService.failOperation(actionOperationId, new IllegalArgumentException(errorMessage));
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
     * Displays help information for diagnostics commands.
     * 
     * @param operationId the parent operation ID for tracking
     */
    private void displayHelp(String operationId) {
        // Create operation parameters for tracking
        Map<String, Object> helpData = new HashMap<>();
        helpData.put("command", "admin diagnostics");
        helpData.put("action", "help");
        helpData.put("format", format);
        
        // Start tracking sub-operation
        String helpOperationId = metadataService.trackOperation("admin-diagnostics-help", helpData);
        
        try {
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> jsonHelpData = new HashMap<>();
                jsonHelpData.put("result", "success");
                jsonHelpData.put("command", "admin diagnostics");
                jsonHelpData.put("usage", "rin admin diagnostics <operation> [options]");
                
                List<Map<String, String>> operations = new ArrayList<>();
                operations.add(createInfoMap("run", "Run system diagnostics"));
                operations.add(createInfoMap("schedule", "Schedule recurring diagnostics"));
                operations.add(createInfoMap("database", "Analyze database performance"));
                operations.add(createInfoMap("warning", "Manage system warnings"));
                operations.add(createInfoMap("action", "Perform diagnostic actions"));
                jsonHelpData.put("operations", operations);
                
                Map<String, List<Map<String, String>>> operationOptions = new HashMap<>();
                
                List<Map<String, String>> runOptions = new ArrayList<>();
                runOptions.add(createInfoMap("--full", "Run full diagnostics (more comprehensive)"));
                operationOptions.put("run", runOptions);
                
                List<Map<String, String>> scheduleOptions = new ArrayList<>();
                scheduleOptions.add(createInfoMap("list", "Show scheduled diagnostic tasks"));
                operationOptions.put("schedule", scheduleOptions);
                
                List<Map<String, String>> databaseOptions = new ArrayList<>();
                databaseOptions.add(createInfoMap("--analyze", "Analyze database performance"));
                operationOptions.put("database", databaseOptions);
                
                List<Map<String, String>> warningOptions = new ArrayList<>();
                warningOptions.add(createInfoMap("--id=<id>", "ID of the warning to resolve"));
                operationOptions.put("warning", warningOptions);
                
                List<Map<String, String>> actionOptions = new ArrayList<>();
                actionOptions.add(createInfoMap("--memory-reclaim", "Reclaim system memory"));
                operationOptions.put("action", actionOptions);
                
                jsonHelpData.put("operation_options", operationOptions);
                
                System.out.println(OutputFormatter.toJson(jsonHelpData, verbose));
            } else {
                System.out.println("Usage: rin admin diagnostics <operation> [options]");
                System.out.println();
                System.out.println("Operations:");
                System.out.println("  run      - Run system diagnostics");
                System.out.println("  schedule - Schedule recurring diagnostics");
                System.out.println("  database - Analyze database performance");
                System.out.println("  warning  - Manage system warnings");
                System.out.println("  action   - Perform diagnostic actions");
                System.out.println();
                System.out.println("Options for 'run':");
                System.out.println("  --full          - Run full diagnostics (more comprehensive)");
                System.out.println();
                System.out.println("Options for 'schedule list':");
                System.out.println("  list            - Show scheduled diagnostic tasks");
                System.out.println();
                System.out.println("Options for 'database':");
                System.out.println("  --analyze       - Analyze database performance");
                System.out.println();
                System.out.println("Options for 'warning resolve':");
                System.out.println("  --id=<id>       - ID of the warning to resolve");
                System.out.println();
                System.out.println("Options for 'action':");
                System.out.println("  --memory-reclaim - Reclaim system memory");
                System.out.println();
                System.out.println("For detailed help on a specific operation, use:");
                System.out.println("  rin admin diagnostics <operation> help");
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