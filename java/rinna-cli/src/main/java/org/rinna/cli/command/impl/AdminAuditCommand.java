/*
 * Administrative audit command handler for Rinna.
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.command.impl;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

import org.rinna.cli.service.AuditService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.ErrorHandler;
import org.rinna.cli.util.OperationTracker;
import org.rinna.cli.util.OutputFormatter;

/**
 * Command handler for audit-related operations.
 * This class implements the functionality for the 'rin admin audit' command.
 * It follows the ViewCommand pattern with MetadataService integration for operation tracking.
 */
public class AdminAuditCommand implements Callable<Integer> {
    
    private String operation;
    private String[] args = new String[0];
    private final ServiceManager serviceManager;
    private final Scanner scanner;
    private final MetadataService metadataService;
    private final OperationTracker operationTracker;
    private final ErrorHandler errorHandler;
    
    /**
     * Creates a new AdminAuditCommand with the specified ServiceManager.
     * 
     * @param serviceManager the service manager
     */
    public AdminAuditCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.metadataService = serviceManager.getMetadataService();
        this.scanner = new Scanner(System.in, java.nio.charset.StandardCharsets.UTF_8.name());
        
        // Initialize utility instances
        this.operationTracker = new OperationTracker(metadataService);
        this.errorHandler = new ErrorHandler(metadataService);
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
    
    private String format = "text";
    private boolean verbose = false;
    
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
        this.errorHandler.outputFormat(format);
    }
    
    /**
     * Sets whether to display verbose output.
     * 
     * @param verbose true for verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
        this.errorHandler.verbose(verbose);
    }
    
    @Override
    public Integer call() {
        // Configure the operation tracker with command details
        operationTracker
            .command("admin-audit")
            .operationType("AUDIT")
            .param("operation", operation != null ? operation : "help")
            .param("argsCount", args.length)
            .param("format", format)
            .param("verbose", verbose);
        
        // Add first argument if available
        if (args.length > 0) {
            operationTracker.param("arg0", args[0]);
        }
        
        try {
            // Execute the main operation with tracking
            return operationTracker.execute(() -> {
                // Handle missing operation
                if (operation == null || operation.isEmpty()) {
                    displayHelp(operationTracker.start());
                    return 1;
                }
                
                // Get the audit service
                AuditService auditService = serviceManager.getAuditService();
                if (auditService == null) {
                    String errorMessage = "Audit service is not available.";
                    return errorHandler.handleError(
                        operationTracker.start(),
                        "admin-audit", 
                        errorMessage,
                        new IllegalStateException(errorMessage),
                        ErrorHandler.Severity.SYSTEM
                    );
                }
                
                // Delegate to the appropriate operation
                String parentOperationId = operationTracker.start();
                int result;
                
                switch (operation) {
                    case "list":
                        result = handleListOperation(auditService, parentOperationId);
                        break;
                    
                    case "configure":
                        result = handleConfigureOperation(auditService, parentOperationId);
                        break;
                    
                    case "status":
                        result = handleStatusOperation(auditService, parentOperationId);
                        break;
                    
                    case "export":
                        result = handleExportOperation(auditService, parentOperationId);
                        break;
                    
                    case "mask":
                        result = handleMaskOperation(auditService, parentOperationId);
                        break;
                    
                    case "alert":
                        result = handleAlertOperation(auditService, parentOperationId);
                        break;
                    
                    case "investigation":
                        result = handleInvestigationOperation(auditService, parentOperationId);
                        break;
                    
                    case "help":
                        displayHelp(parentOperationId);
                        result = 0;
                        break;
                    
                    default:
                        String errorMessage = "Unknown audit operation: " + operation;
                        displayHelp(parentOperationId);
                        return errorHandler.handleError(
                            parentOperationId,
                            "admin-audit",
                            errorMessage,
                            new IllegalArgumentException(errorMessage),
                            ErrorHandler.Severity.VALIDATION
                        );
                }
                
                // If operation was successful, record the result
                if (result == 0) {
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("success", true);
                    resultData.put("operation", operation);
                    operationTracker.complete(parentOperationId, resultData);
                }
                
                return result;
            });
        } catch (Exception e) {
            // Handle any unexpected errors using the error handler
            return errorHandler.handleUnexpectedError(
                operationTracker.start(),
                "admin-audit", 
                e,
                ErrorHandler.Severity.SYSTEM
            );
        }
    }
    
    /**
     * Handles the 'list' operation to display audit logs.
     * 
     * @param auditService the audit service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleListOperation(AuditService auditService, String parentOperationId) {
        // Parse options from arguments
        Map<String, String> options = parseOptions(args);
        
        String user = options.getOrDefault("user", null);
        Integer days = null;
        Integer limit = null;
        
        // Create a sub-tracker for this operation
        OperationTracker listTracker = operationTracker
            .command("admin-audit-list")
            .param("operation", "list")
            .param("user", user)
            .param("format", format)
            .parent(parentOperationId);
        
        // Validate and parse the days parameter
        if (options.containsKey("days")) {
            try {
                days = Integer.parseInt(options.get("days"));
                listTracker.param("days", days);
            } catch (NumberFormatException e) {
                // Handle validation error with appropriate severity
                Map<String, String> validationErrors = new HashMap<>();
                validationErrors.put("days", "Invalid value for --days. Must be a number.");
                return errorHandler.handleValidationError(
                    listTracker.start(),
                    "admin-audit-list",
                    validationErrors
                );
            }
        }
        
        // Validate and parse the limit parameter
        if (options.containsKey("limit")) {
            try {
                limit = Integer.parseInt(options.get("limit"));
                listTracker.param("limit", limit);
            } catch (NumberFormatException e) {
                // Handle validation error with appropriate severity
                Map<String, String> validationErrors = new HashMap<>();
                validationErrors.put("limit", "Invalid value for --limit. Must be a number.");
                return errorHandler.handleValidationError(
                    listTracker.start(),
                    "admin-audit-list",
                    validationErrors
                );
            }
        }
        
        // Store final values for use in the lambda
        final Integer finalDays = days;
        final Integer finalLimit = limit;
        
        try {
            // Execute the operation with proper tracking
            return listTracker.execute(() -> {
                try {
                    // Get the audit logs
                    String result = auditService.listAuditLogs(user, finalDays, finalLimit);
                    
                    // Display the results in the appropriate format
                    if ("json".equalsIgnoreCase(format)) {
                        Map<String, Object> resultData = new HashMap<>();
                        resultData.put("result", "success");
                        resultData.put("operation", "list");
                        
                        Map<String, Object> auditData = new HashMap<>();
                        auditData.put("user", user);
                        auditData.put("days", finalDays);
                        auditData.put("limit", finalLimit);
                        auditData.put("logs", result);
                        
                        resultData.put("data", auditData);
                        
                        System.out.println(OutputFormatter.toJson(resultData, verbose));
                    } else {
                        System.out.println(result);
                    }
                    
                    // Build success result data
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("user", user);
                    resultData.put("days", finalDays);
                    resultData.put("limit", finalLimit);
                    
                    // Return success with tracked operation
                    return errorHandler.handleSuccess(listTracker.start(), resultData);
                } catch (Exception e) {
                    // Handle expected errors with proper context
                    String errorMessage = "Error listing audit logs: " + e.getMessage();
                    return errorHandler.handleError(
                        listTracker.start(),
                        "admin-audit-list",
                        errorMessage,
                        e,
                        ErrorHandler.Severity.ERROR
                    );
                }
            });
        } catch (Exception e) {
            // Handle unexpected errors
            return errorHandler.handleUnexpectedError(
                listTracker.start(),
                "admin-audit-list",
                e,
                ErrorHandler.Severity.SYSTEM
            );
        }
    }
    
    /**
     * Handles the 'configure' operation to set up audit logging.
     * 
     * @param auditService the audit service
     * @param operationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleConfigureOperation(AuditService auditService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "configure");
        
        Map<String, String> options = parseOptions(args);
        
        Integer retention = null;
        
        if (options.containsKey("retention")) {
            params.put("retention", options.get("retention"));
        }
        
        // Start tracking sub-operation
        String configOperationId = metadataService.trackOperation("admin-audit-configure", params);
        
        try {
            if (options.containsKey("retention")) {
                try {
                    retention = Integer.parseInt(options.get("retention"));
                    
                    if (retention <= 0) {
                        String errorMessage = "Retention period must be greater than 0.";
                        outputError(errorMessage);
                        metadataService.trackOperationError(parentOperationId, "configure", errorMessage, 
                                                           new IllegalArgumentException(errorMessage));
                        return 1;
                    }
                    
                    boolean success = auditService.configureRetention(retention);
                    if (success) {
                        System.out.println("Audit log retention period updated to " + retention + " days");
                        System.out.println("Configuration changes have been saved successfully.");
                        
                        // Track operation success
                        Map<String, Object> resultData = new HashMap<>();
                        resultData.put("success", true);
                        resultData.put("retention", retention);
                        metadataService.completeOperation(configOperationId, resultData);
                        
                        return 0;
                    } else {
                        String errorMessage = "Failed to update audit retention period.";
                        outputError(errorMessage);
                        metadataService.failOperation(configOperationId, new RuntimeException(errorMessage));
                        return 1;
                    }
                } catch (NumberFormatException e) {
                    String errorMessage = "Invalid value for --retention. Must be a number.";
                    outputError(errorMessage);
                    metadataService.failOperation(configOperationId, e);
                    return 1;
                }
            } else {
                // Interactive configuration
                System.out.println("Audit Configuration");
                System.out.println("==================");
                
                System.out.print("Enter retention period in days [30]: ");
                String input = scanner.nextLine().trim();
                
                if (!input.isEmpty()) {
                    try {
                        retention = Integer.parseInt(input);
                        if (retention <= 0) {
                            String errorMessage = "Retention period must be greater than 0.";
                            outputError(errorMessage);
                            metadataService.failOperation(configOperationId, 
                                                         new IllegalArgumentException(errorMessage));
                            return 1;
                        }
                    } catch (NumberFormatException e) {
                        String errorMessage = "Invalid value for retention period. Must be a number.";
                        outputError(errorMessage);
                        metadataService.failOperation(configOperationId, e);
                        return 1;
                    }
                } else {
                    retention = 30; // Default
                }
                
                boolean success = auditService.configureRetention(retention);
                if (success) {
                    System.out.println("Audit log retention period updated to " + retention + " days");
                    System.out.println("Configuration changes have been saved successfully.");
                    
                    // Track operation success
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("success", true);
                    resultData.put("retention", retention);
                    resultData.put("interactive", true);
                    metadataService.completeOperation(configOperationId, resultData);
                    
                    return 0;
                } else {
                    String errorMessage = "Failed to update audit retention period.";
                    outputError(errorMessage);
                    metadataService.failOperation(configOperationId, new RuntimeException(errorMessage));
                    return 1;
                }
            }
        } catch (Exception e) {
            String errorMessage = "Error configuring audit retention: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(configOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'status' operation to display audit system status.
     * 
     * @param auditService the audit service
     * @param operationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleStatusOperation(AuditService auditService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "status");
        params.put("format", format);
        
        // Start tracking sub-operation
        String statusOperationId = metadataService.trackOperation("admin-audit-status", params);
        
        try {
            String status = auditService.getAuditStatus();
            
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
            resultData.put("format", format);
            metadataService.completeOperation(statusOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error getting audit status: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(statusOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'export' operation to export audit logs.
     * 
     * @param auditService the audit service
     * @param operationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleExportOperation(AuditService auditService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "export");
        
        Map<String, String> options = parseOptions(args);
        
        LocalDate fromDate = null;
        LocalDate toDate = null;
        String exportFormat = options.getOrDefault("format", "csv");
        
        params.put("exportFormat", exportFormat);
        
        // Start tracking sub-operation
        String exportOperationId = metadataService.trackOperation("admin-audit-export", params);
        
        if (options.containsKey("from")) {
            try {
                fromDate = LocalDate.parse(options.get("from"), DateTimeFormatter.ISO_LOCAL_DATE);
                params.put("fromDate", fromDate.toString());
                metadataService.trackOperationDetail(exportOperationId, "fromDate", fromDate.toString());
            } catch (DateTimeParseException e) {
                String errorMessage = "Invalid date format for --from. Use YYYY-MM-DD.";
                outputError(errorMessage);
                metadataService.failOperation(exportOperationId, e);
                return 1;
            }
        } else {
            // Default to 30 days ago
            fromDate = LocalDate.now().minusDays(30);
            params.put("fromDate", fromDate.toString());
            metadataService.trackOperationDetail(exportOperationId, "fromDate", fromDate.toString());
            metadataService.trackOperationDetail(exportOperationId, "fromDateDefault", true);
        }
        
        if (options.containsKey("to")) {
            try {
                toDate = LocalDate.parse(options.get("to"), DateTimeFormatter.ISO_LOCAL_DATE);
                params.put("toDate", toDate.toString());
                metadataService.trackOperationDetail(exportOperationId, "toDate", toDate.toString());
            } catch (DateTimeParseException e) {
                String errorMessage = "Invalid date format for --to. Use YYYY-MM-DD.";
                outputError(errorMessage);
                metadataService.failOperation(exportOperationId, e);
                return 1;
            }
        } else {
            // Default to today
            toDate = LocalDate.now();
            params.put("toDate", toDate.toString());
            metadataService.trackOperationDetail(exportOperationId, "toDate", toDate.toString());
            metadataService.trackOperationDetail(exportOperationId, "toDateDefault", true);
        }
        
        if (!fromDate.isBefore(toDate)) {
            String errorMessage = "From date must be before to date.";
            outputError(errorMessage);
            metadataService.failOperation(exportOperationId, 
                                         new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        try {
            String exportPath = auditService.exportAuditLogs(fromDate, toDate, exportFormat);
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "export");
                
                Map<String, Object> exportData = new HashMap<>();
                exportData.put("from", fromDate.toString());
                exportData.put("to", toDate.toString());
                exportData.put("format", exportFormat);
                exportData.put("path", exportPath);
                
                resultData.put("data", exportData);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println("Exported audit logs to " + exportPath);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("exportPath", exportPath);
            resultData.put("fromDate", fromDate.toString());
            resultData.put("toDate", toDate.toString());
            resultData.put("exportFormat", exportFormat);
            metadataService.completeOperation(exportOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error exporting audit logs: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(exportOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'mask' operation to configure data masking.
     * 
     * @param auditService the audit service
     * @param operationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleMaskOperation(AuditService auditService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "mask");
        
        if (args.length == 0) {
            String errorMessage = "Missing mask subcommand. Use 'configure' or 'status'.";
            outputError(errorMessage);
            metadataService.trackOperationError(parentOperationId, "mask", errorMessage,
                                               new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        String maskOperation = args[0];
        String[] maskArgs = Arrays.copyOfRange(args, 1, args.length);
        
        params.put("maskOperation", maskOperation);
        
        // Start tracking sub-operation
        String maskOperationId = metadataService.trackOperation("admin-audit-mask", params);
        
        if ("configure".equals(maskOperation)) {
            System.out.println("Data Masking Configuration");
            System.out.println("-------------------------");
            System.out.println("Select fields to mask in audit logs:");
            System.out.println("Available fields: email, phone, address, ssn, credit_card, account_number");
            System.out.print("Enter comma-separated list of fields: ");
            
            String fieldsInput = scanner.nextLine().trim();
            List<String> fields = Arrays.asList(fieldsInput.split("\\s*,\\s*"));
            
            // Track fields selected
            metadataService.trackOperationDetail(maskOperationId, "fields", fields);
            
            try {
                boolean success = auditService.configureMasking(fields);
                if (success) {
                    System.out.println("Data masking configuration updated successfully.");
                    
                    // Track operation success
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("success", true);
                    resultData.put("fields", fields);
                    metadataService.completeOperation(maskOperationId, resultData);
                    
                    return 0;
                } else {
                    String errorMessage = "Failed to update data masking configuration.";
                    outputError(errorMessage);
                    metadataService.failOperation(maskOperationId, new RuntimeException(errorMessage));
                    return 1;
                }
            } catch (Exception e) {
                String errorMessage = "Error configuring data masking: " + e.getMessage();
                outputError(errorMessage);
                metadataService.failOperation(maskOperationId, e);
                return 1;
            }
        } else if ("status".equals(maskOperation)) {
            try {
                String status = auditService.getMaskingStatus();
                System.out.println(status);
                
                // Track operation success
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                resultData.put("statusReceived", true);
                metadataService.completeOperation(maskOperationId, resultData);
                
                return 0;
            } catch (Exception e) {
                String errorMessage = "Error getting masking status: " + e.getMessage();
                outputError(errorMessage);
                metadataService.failOperation(maskOperationId, e);
                return 1;
            }
        } else {
            String errorMessage = "Unknown mask operation: " + maskOperation;
            outputError(errorMessage);
            System.out.println("Valid operations: configure, status");
            metadataService.failOperation(maskOperationId, 
                                         new IllegalArgumentException(errorMessage));
            return 1;
        }
    }
    
    /**
     * Handles the 'alert' operation to manage audit alerts.
     * 
     * @param auditService the audit service
     * @param operationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleAlertOperation(AuditService auditService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "alert");
        
        if (args.length == 0) {
            String errorMessage = "Missing alert subcommand. Use 'add', 'list', or 'remove'.";
            outputError(errorMessage);
            metadataService.trackOperationError(parentOperationId, "alert", errorMessage,
                                               new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        String alertOperation = args[0];
        String[] alertArgs = Arrays.copyOfRange(args, 1, args.length);
        
        params.put("alertOperation", alertOperation);
        
        // Start tracking sub-operation
        String alertOperationId = metadataService.trackOperation("admin-audit-alert", params);
        
        if ("add".equals(alertOperation)) {
            System.out.println("Create Audit Alert");
            System.out.println("----------------");
            
            System.out.print("Enter alert name: ");
            String name = scanner.nextLine().trim();
            
            if (name.isEmpty()) {
                String errorMessage = "Alert name cannot be empty.";
                outputError(errorMessage);
                metadataService.failOperation(alertOperationId, 
                                             new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            metadataService.trackOperationDetail(alertOperationId, "alertName", name);
            
            System.out.println("Select event types (comma-separated):");
            System.out.println("- FAILED_LOGIN: Failed login attempts");
            System.out.println("- PERMISSION_DENIED: Access permission denied");
            System.out.println("- ADMIN_ACTION: Administrative actions");
            System.out.println("- DATA_EXPORT: Data export operations");
            System.out.println("- CONFIGURATION_CHANGE: System configuration changes");
            System.out.print("Enter event types: ");
            
            String eventsInput = scanner.nextLine().trim();
            List<String> events = Arrays.asList(eventsInput.split("\\s*,\\s*"));
            
            metadataService.trackOperationDetail(alertOperationId, "events", events);
            
            System.out.print("Enter threshold count: ");
            String thresholdInput = scanner.nextLine().trim();
            int threshold;
            
            try {
                threshold = Integer.parseInt(thresholdInput);
                if (threshold <= 0) {
                    String errorMessage = "Threshold must be greater than 0.";
                    outputError(errorMessage);
                    metadataService.failOperation(alertOperationId, 
                                                 new IllegalArgumentException(errorMessage));
                    return 1;
                }
                metadataService.trackOperationDetail(alertOperationId, "threshold", threshold);
            } catch (NumberFormatException e) {
                String errorMessage = "Invalid threshold value. Must be a number.";
                outputError(errorMessage);
                metadataService.failOperation(alertOperationId, e);
                return 1;
            }
            
            System.out.print("Enter time window in minutes: ");
            String windowInput = scanner.nextLine().trim();
            int window;
            
            try {
                window = Integer.parseInt(windowInput);
                if (window <= 0) {
                    String errorMessage = "Time window must be greater than 0.";
                    outputError(errorMessage);
                    metadataService.failOperation(alertOperationId, 
                                                 new IllegalArgumentException(errorMessage));
                    return 1;
                }
                metadataService.trackOperationDetail(alertOperationId, "window", window);
            } catch (NumberFormatException e) {
                String errorMessage = "Invalid time window value. Must be a number.";
                outputError(errorMessage);
                metadataService.failOperation(alertOperationId, e);
                return 1;
            }
            
            System.out.print("Enter notification recipients (comma-separated email addresses): ");
            String recipientsInput = scanner.nextLine().trim();
            List<String> recipients = Arrays.asList(recipientsInput.split("\\s*,\\s*"));
            
            metadataService.trackOperationDetail(alertOperationId, "recipients", recipients);
            
            try {
                boolean success = auditService.addAlert(name, events, threshold, window, recipients);
                if (success) {
                    System.out.println("Audit alert '" + name + "' created successfully.");
                    
                    // Track operation success
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("success", true);
                    resultData.put("alertName", name);
                    resultData.put("events", events);
                    resultData.put("threshold", threshold);
                    resultData.put("window", window);
                    resultData.put("recipients", recipients);
                    metadataService.completeOperation(alertOperationId, resultData);
                    
                    return 0;
                } else {
                    String errorMessage = "Failed to create audit alert.";
                    outputError(errorMessage);
                    metadataService.failOperation(alertOperationId, new RuntimeException(errorMessage));
                    return 1;
                }
            } catch (Exception e) {
                String errorMessage = "Error creating audit alert: " + e.getMessage();
                outputError(errorMessage);
                metadataService.failOperation(alertOperationId, e);
                return 1;
            }
        } else if ("list".equals(alertOperation)) {
            try {
                String result = auditService.listAlerts();
                System.out.println(result);
                
                // Track operation success
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                resultData.put("alertsListed", true);
                metadataService.completeOperation(alertOperationId, resultData);
                
                return 0;
            } catch (Exception e) {
                String errorMessage = "Error listing audit alerts: " + e.getMessage();
                outputError(errorMessage);
                metadataService.failOperation(alertOperationId, e);
                return 1;
            }
        } else if ("remove".equals(alertOperation) || "delete".equals(alertOperation)) {
            if (alertArgs.length == 0) {
                String errorMessage = "Missing alert name to remove.";
                outputError(errorMessage);
                metadataService.failOperation(alertOperationId, 
                                             new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            String alertName = alertArgs[0];
            metadataService.trackOperationDetail(alertOperationId, "alertName", alertName);
            
            try {
                boolean success = auditService.removeAlert(alertName);
                if (success) {
                    System.out.println("Audit alert '" + alertName + "' removed successfully.");
                    
                    // Track operation success
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("success", true);
                    resultData.put("alertName", alertName);
                    resultData.put("removed", true);
                    metadataService.completeOperation(alertOperationId, resultData);
                    
                    return 0;
                } else {
                    String errorMessage = "Failed to remove audit alert. Does it exist?";
                    outputError(errorMessage);
                    metadataService.failOperation(alertOperationId, new RuntimeException(errorMessage));
                    return 1;
                }
            } catch (Exception e) {
                String errorMessage = "Error removing audit alert: " + e.getMessage();
                outputError(errorMessage);
                metadataService.failOperation(alertOperationId, e);
                return 1;
            }
        } else {
            String errorMessage = "Unknown alert operation: " + alertOperation;
            outputError(errorMessage);
            System.out.println("Valid operations: add, list, remove");
            metadataService.failOperation(alertOperationId, 
                                         new IllegalArgumentException(errorMessage));
            return 1;
        }
    }
    
    /**
     * Handles the 'investigation' operation to manage security investigations.
     * 
     * @param auditService the audit service
     * @return the exit code
     */
    private int handleInvestigationOperation(AuditService auditService, String operationId) {
        if (args.length == 0) {
            System.err.println("Error: Missing investigation subcommand. Use 'create', 'findings', or 'actions'.");
            return 1;
        }
        
        String investigationOperation = args[0];
        String[] investigationArgs = Arrays.copyOfRange(args, 1, args.length);
        Map<String, String> options = parseOptions(investigationArgs);
        
        if ("create".equals(investigationOperation)) {
            String user = options.getOrDefault("user", null);
            Integer days = null;
            
            if (user == null) {
                System.err.println("Error: Missing required parameter --user.");
                return 1;
            }
            
            if (options.containsKey("days")) {
                try {
                    days = Integer.parseInt(options.get("days"));
                } catch (NumberFormatException e) {
                    System.err.println("Error: Invalid value for --days. Must be a number.");
                    return 1;
                }
            } else {
                days = 7; // Default to 7 days
            }
            
            try {
                String caseId = auditService.createInvestigation(user, days);
                System.out.println("Investigation case created successfully.");
                System.out.println("Case ID: " + caseId);
                return 0;
            } catch (Exception e) {
                System.err.println("Error creating investigation: " + e.getMessage());
                return 1;
            }
        } else if ("findings".equals(investigationOperation)) {
            String caseId = options.getOrDefault("case", null);
            
            try {
                String findings = auditService.getInvestigationFindings(caseId);
                System.out.println(findings);
                return 0;
            } catch (Exception e) {
                System.err.println("Error getting investigation findings: " + e.getMessage());
                return 1;
            }
        } else if ("actions".equals(investigationOperation)) {
            String action = options.getOrDefault("action", null);
            String user = options.getOrDefault("user", null);
            
            if (action == null) {
                System.err.println("Error: Missing required parameter --action.");
                return 1;
            }
            
            if (user == null) {
                System.err.println("Error: Missing required parameter --user.");
                return 1;
            }
            
            try {
                boolean success = auditService.performInvestigationAction(action, user);
                if (success) {
                    System.out.println("Investigation action '" + action + "' performed successfully on user '" + user + "'.");
                    return 0;
                } else {
                    System.err.println("Error: Failed to perform investigation action.");
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("Error performing investigation action: " + e.getMessage());
                return 1;
            }
        } else {
            System.err.println("Error: Unknown investigation operation: " + investigationOperation);
            System.out.println("Valid operations: create, findings, actions");
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
     * Displays help information for audit commands.
     * 
     * @param operationId the parent operation ID for tracking
     */
    private void displayHelp(String operationId) {
        // Create operation parameters for tracking
        Map<String, Object> helpData = new HashMap<>();
        helpData.put("command", "admin audit");
        helpData.put("action", "help");
        helpData.put("format", format);
        
        // Start tracking sub-operation
        String helpOperationId = metadataService.trackOperation("admin-audit-help", helpData);
        
        try {
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> jsonHelpData = new HashMap<>();
                jsonHelpData.put("result", "success");
                jsonHelpData.put("command", "admin audit");
                jsonHelpData.put("usage", "rin admin audit <operation> [options]");
                
                List<Map<String, String>> operations = new ArrayList<>();
                operations.add(createInfoMap("list", "List audit logs"));
                operations.add(createInfoMap("configure", "Configure audit logging settings"));
                operations.add(createInfoMap("status", "Show audit system status"));
                operations.add(createInfoMap("export", "Export audit logs to file"));
                operations.add(createInfoMap("mask", "Configure sensitive data masking"));
                operations.add(createInfoMap("alert", "Manage audit alerts"));
                operations.add(createInfoMap("investigation", "Manage security investigations"));
                jsonHelpData.put("operations", operations);
                
                Map<String, List<Map<String, String>>> operationOptions = new HashMap<>();
                
                List<Map<String, String>> listOptions = new ArrayList<>();
                listOptions.add(createInfoMap("--user=<username>", "Filter logs by username"));
                listOptions.add(createInfoMap("--days=<num>", "Show logs from last N days"));
                listOptions.add(createInfoMap("--limit=<num>", "Limit number of logs shown"));
                operationOptions.put("list", listOptions);
                
                List<Map<String, String>> configureOptions = new ArrayList<>();
                configureOptions.add(createInfoMap("--retention=<days>", "Set log retention period in days"));
                operationOptions.put("configure", configureOptions);
                
                List<Map<String, String>> exportOptions = new ArrayList<>();
                exportOptions.add(createInfoMap("--from=<date>", "Start date (YYYY-MM-DD)"));
                exportOptions.add(createInfoMap("--to=<date>", "End date (YYYY-MM-DD)"));
                exportOptions.add(createInfoMap("--format=<format>", "Export format (csv, json, pdf)"));
                operationOptions.put("export", exportOptions);
                
                List<Map<String, String>> investigationOptions = new ArrayList<>();
                investigationOptions.add(createInfoMap("--user=<username>", "Username to investigate"));
                investigationOptions.add(createInfoMap("--days=<num>", "Days of history to investigate"));
                investigationOptions.add(createInfoMap("--action=<action>", "Action to perform (LOCK_ACCOUNT, RESET_PASSWORD, etc.)"));
                operationOptions.put("investigation", investigationOptions);
                
                jsonHelpData.put("operation_options", operationOptions);
                
                System.out.println(OutputFormatter.toJson(jsonHelpData, verbose));
            } else {
                System.out.println("Usage: rin admin audit <operation> [options]");
                System.out.println();
                System.out.println("Operations:");
                System.out.println("  list          - List audit logs");
                System.out.println("  configure     - Configure audit logging settings");
                System.out.println("  status        - Show audit system status");
                System.out.println("  export        - Export audit logs to file");
                System.out.println("  mask          - Configure sensitive data masking");
                System.out.println("  alert         - Manage audit alerts");
                System.out.println("  investigation - Manage security investigations");
                System.out.println();
                System.out.println("Options for 'list':");
                System.out.println("  --user=<username>  - Filter logs by username");
                System.out.println("  --days=<num>       - Show logs from last N days");
                System.out.println("  --limit=<num>      - Limit number of logs shown");
                System.out.println();
                System.out.println("Options for 'configure':");
                System.out.println("  --retention=<days> - Set log retention period in days");
                System.out.println();
                System.out.println("Options for 'export':");
                System.out.println("  --from=<date>      - Start date (YYYY-MM-DD)");
                System.out.println("  --to=<date>        - End date (YYYY-MM-DD)");
                System.out.println("  --format=<format>  - Export format (csv, json, pdf)");
                System.out.println();
                System.out.println("Options for 'investigation create':");
                System.out.println("  --user=<username>  - Username to investigate");
                System.out.println("  --days=<num>       - Days of history to investigate");
                System.out.println();
                System.out.println("Options for 'investigation actions':");
                System.out.println("  --action=<action>  - Action to perform (LOCK_ACCOUNT, RESET_PASSWORD, etc.)");
                System.out.println("  --user=<username>  - Target username for the action");
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
     * @param severity the error severity
     */
    private void outputError(String message, ErrorHandler.Severity severity) {
        errorHandler.outputError(message, null, severity);
    }
    
    /**
     * Outputs an error message in either JSON or text format.
     * Uses ERROR severity by default.
     *
     * @param message the error message
     */
    private void outputError(String message) {
        errorHandler.outputError(message, null);
    }
    
    /**
     * Outputs a success message or data in either JSON or text format.
     *
     * @param message the success message or data
     */
    private void outputSuccess(String message) {
        if ("json".equalsIgnoreCase(format)) {
            Map<String, Object> successData = errorHandler.createSuccessResult("admin-audit", Map.of("message", message));
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