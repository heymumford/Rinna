/*
 * Administrative compliance command handler for Rinna.
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.command.impl;

import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.ComplianceService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.util.OutputFormatter;
import org.rinna.cli.util.ErrorHandler;
import org.rinna.cli.util.OperationTracker;
import org.rinna.cli.util.ErrorHandler.Severity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Command handler for compliance-related operations.
 * This class implements the functionality for the 'rin admin compliance' command.
 * It follows the ViewCommand pattern with MetadataService integration for operation tracking.
 */
public class AdminComplianceCommand implements Callable<Integer> {
    
    private String operation;
    private String[] args = new String[0];
    private final ServiceManager serviceManager;
    private final MetadataService metadataService;
    private final Scanner scanner;
    private final ErrorHandler errorHandler;
    private final OperationTracker operationTracker;
    private String format = "text";
    private boolean verbose = false;
    
    /**
     * Creates a new AdminComplianceCommand with the specified ServiceManager.
     * 
     * @param serviceManager the service manager
     */
    public AdminComplianceCommand(ServiceManager serviceManager) {
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
            .command("admin-compliance")
            .operationType("COMPLIANCE")
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
                
                // Get the compliance service
                ComplianceService complianceService = serviceManager.getComplianceService();
                if (complianceService == null) {
                    String errorMessage = "Compliance service is not available.";
                    return errorHandler.handleError(
                        operationTracker.start(),
                        "admin-compliance", 
                        errorMessage,
                        new IllegalStateException(errorMessage),
                        ErrorHandler.Severity.SYSTEM
                    );
                }
                
                // Delegate to the appropriate operation
                String parentOperationId = operationTracker.start();
                int result;
                
                switch (operation) {
                    case "report":
                        result = handleReportOperation(complianceService, parentOperationId);
                        break;
                    
                    case "configure":
                        result = handleConfigureOperation(complianceService, parentOperationId);
                        break;
                    
                    case "validate":
                        result = handleValidateOperation(complianceService, parentOperationId);
                        break;
                    
                    case "status":
                        result = handleStatusOperation(complianceService, parentOperationId);
                        break;
                    
                    case "help":
                        displayHelp(parentOperationId);
                        result = 0;
                        break;
                    
                    default:
                        String errorMessage = "Unknown compliance operation: " + operation;
                        displayHelp(parentOperationId);
                        return errorHandler.handleError(
                            parentOperationId,
                            "admin-compliance",
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
                "admin-compliance", 
                e
            );
        }
    }
    
    /**
     * Handles the 'report' operation to generate compliance reports.
     * 
     * @param complianceService the compliance service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleReportOperation(ComplianceService complianceService, String parentOperationId) {
        // Parse options from arguments
        Map<String, String> options = parseOptions(args);
        
        String type = options.getOrDefault("type", "GDPR");
        String period = options.getOrDefault("period", "current");
        
        // Create a sub-tracker for this operation
        OperationTracker reportTracker = operationTracker
            .command("admin-compliance-report")
            .param("operation", "report")
            .param("type", type)
            .param("period", period)
            .param("format", format)
            .parent(parentOperationId);
        
        try {
            // Execute the operation and return the result
            return reportTracker.execute(() -> {
                try {
                    String report = complianceService.generateComplianceReport(type, period);
                    
                    if ("json".equalsIgnoreCase(format)) {
                        Map<String, Object> resultData = new HashMap<>();
                        resultData.put("result", "success");
                        resultData.put("operation", "report");
                        
                        Map<String, Object> reportData = new HashMap<>();
                        reportData.put("type", type);
                        reportData.put("period", period);
                        reportData.put("report", report);
                        
                        resultData.put("data", reportData);
                        
                        System.out.println(OutputFormatter.toJson(resultData, verbose));
                    } else {
                        System.out.println(report);
                    }
                    
                    // Return success with result data
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("type", type);
                    resultData.put("period", period);
                    
                    return errorHandler.handleSuccess(reportTracker.start(), resultData);
                } catch (Exception e) {
                    // Handle expected errors with error context
                    String errorMessage = "Error generating compliance report: " + e.getMessage();
                    return errorHandler.handleError(
                        reportTracker.start(),
                        "admin-compliance-report",
                        errorMessage,
                        e,
                        ErrorHandler.Severity.ERROR
                    );
                }
            });
        } catch (Exception e) {
            // Handle unexpected errors
            return errorHandler.handleUnexpectedError(
                reportTracker.start(),
                "admin-compliance-report",
                e,
                ErrorHandler.Severity.SYSTEM
            );
        }
    }
    
    /**
     * Handles the 'configure' operation to set up project compliance.
     * 
     * @param complianceService the compliance service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleConfigureOperation(ComplianceService complianceService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "configure");
        
        // Parse options from arguments
        Map<String, String> options = parseOptions(args);
        
        String projectName = options.getOrDefault("project", null);
        params.put("project", projectName);
        
        if (projectName == null) {
            String errorMessage = "Missing required parameter --project.";
            outputError(errorMessage);
            metadataService.trackOperationError(parentOperationId, "configure", errorMessage, 
                    new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        // Start tracking sub-operation
        String configureOperationId = metadataService.trackOperation("admin-compliance-configure", params);
        
        try {
            if ("json".equalsIgnoreCase(format)) {
                // In JSON mode, we need parameters passed in the command arguments
                outputError("Interactive configuration is not supported in JSON mode. Please provide all parameters as command arguments.");
                metadataService.failOperation(configureOperationId, 
                        new UnsupportedOperationException("Interactive configuration not supported in JSON mode"));
                return 1;
            }
            
            System.out.println("Compliance Configuration for Project: " + projectName);
            System.out.println("-".repeat(projectName.length() + 35));
            System.out.println();
            
            System.out.println("Select applicable compliance frameworks (comma-separated):");
            System.out.println("1. GDPR - General Data Protection Regulation");
            System.out.println("2. HIPAA - Health Insurance Portability and Accountability Act");
            System.out.println("3. SOC2 - Service Organization Control 2");
            System.out.println("4. PCI-DSS - Payment Card Industry Data Security Standard");
            System.out.println("5. ISO27001 - Information Security Management");
            System.out.print("Enter frameworks [1]: ");
            
            String frameworksInput = scanner.nextLine().trim();
            List<String> frameworks;
            
            if (frameworksInput.isEmpty()) {
                frameworks = List.of("GDPR"); // Default
            } else if (frameworksInput.matches("^[1-5](,[1-5])*$")) {
                // User entered numbers
                frameworks = Arrays.stream(frameworksInput.split(","))
                    .map(s -> {
                        switch (s) {
                            case "1": return "GDPR";
                            case "2": return "HIPAA";
                            case "3": return "SOC2";
                            case "4": return "PCI-DSS";
                            case "5": return "ISO27001";
                            default: return "";
                        }
                    })
                    .filter(s -> !s.isEmpty())
                    .toList();
            } else {
                // User entered framework names
                frameworks = Arrays.asList(frameworksInput.split("\\s*,\\s*"));
            }
            
            // Track framework selection
            metadataService.trackOperationDetail(configureOperationId, "frameworks", frameworks);
            
            System.out.print("Assign a compliance reviewer (username): ");
            String reviewer = scanner.nextLine().trim();
            
            if (reviewer.isEmpty()) {
                String errorMessage = "Compliance reviewer cannot be empty.";
                outputError(errorMessage);
                metadataService.trackOperationError(configureOperationId, "reviewer_validation", errorMessage,
                        new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            // Track reviewer assignment
            metadataService.trackOperationDetail(configureOperationId, "reviewer", reviewer);
            
            try {
                boolean success = complianceService.configureProjectCompliance(projectName, frameworks, reviewer);
                if (success) {
                    System.out.println();
                    System.out.println("Compliance configuration updated successfully for project: " + projectName);
                    System.out.println("Frameworks: " + String.join(", ", frameworks));
                    System.out.println("Reviewer: " + reviewer);
                    
                    // Track operation success
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("success", true);
                    resultData.put("project", projectName);
                    resultData.put("frameworks", frameworks);
                    resultData.put("reviewer", reviewer);
                    metadataService.completeOperation(configureOperationId, resultData);
                    
                    return 0;
                } else {
                    String errorMessage = "Failed to update compliance configuration.";
                    outputError(errorMessage);
                    metadataService.failOperation(configureOperationId, new RuntimeException(errorMessage));
                    return 1;
                }
            } catch (Exception e) {
                String errorMessage = "Error configuring compliance: " + e.getMessage();
                outputError(errorMessage);
                if (verbose) {
                    e.printStackTrace();
                }
                metadataService.failOperation(configureOperationId, e);
                return 1;
            }
        } catch (Exception e) {
            String errorMessage = "Error in configuration process: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(configureOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'validate' operation to check project compliance.
     * 
     * @param complianceService the compliance service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleValidateOperation(ComplianceService complianceService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "validate");
        
        // Parse options from arguments
        Map<String, String> options = parseOptions(args);
        
        String projectName = options.getOrDefault("project", null);
        params.put("project", projectName);
        
        if (projectName == null) {
            String errorMessage = "Missing required parameter --project.";
            outputError(errorMessage);
            metadataService.trackOperationError(parentOperationId, "validate", errorMessage, 
                    new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        // Start tracking sub-operation
        String validateOperationId = metadataService.trackOperation("admin-compliance-validate", params);
        
        try {
            String validationReport = complianceService.validateProjectCompliance(projectName);
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "validate");
                
                Map<String, Object> validateData = new HashMap<>();
                validateData.put("project", projectName);
                validateData.put("report", validationReport);
                
                resultData.put("data", validateData);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println(validationReport);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("project", projectName);
            metadataService.completeOperation(validateOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error validating compliance: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(validateOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'status' operation to display compliance status.
     * 
     * @param complianceService the compliance service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleStatusOperation(ComplianceService complianceService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "status");
        
        // Parse options from arguments
        Map<String, String> options = parseOptions(args);
        
        String projectName = options.getOrDefault("project", null);
        params.put("project", projectName);
        
        // Start tracking sub-operation
        String statusOperationId = metadataService.trackOperation("admin-compliance-status", params);
        
        try {
            String status;
            if (projectName != null) {
                status = complianceService.getProjectComplianceStatus(projectName);
            } else {
                status = complianceService.getSystemComplianceStatus();
            }
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "status");
                
                Map<String, Object> statusData = new HashMap<>();
                statusData.put("project", projectName);
                statusData.put("status", status);
                statusData.put("scope", projectName != null ? "project" : "system");
                
                resultData.put("data", statusData);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println(status);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("project", projectName);
            resultData.put("scope", projectName != null ? "project" : "system");
            metadataService.completeOperation(statusOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error getting compliance status: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(statusOperationId, e);
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
     * Displays help information for compliance commands.
     * 
     * @param operationId the parent operation ID for tracking
     */
    private void displayHelp(String operationId) {
        // Create operation parameters for tracking
        Map<String, Object> helpData = new HashMap<>();
        helpData.put("command", "admin compliance");
        helpData.put("action", "help");
        helpData.put("format", format);
        
        // Start tracking sub-operation
        String helpOperationId = metadataService.trackOperation("admin-compliance-help", helpData);
        
        try {
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> jsonHelpData = new HashMap<>();
                jsonHelpData.put("result", "success");
                jsonHelpData.put("command", "admin compliance");
                jsonHelpData.put("usage", "rin admin compliance <operation> [options]");
                
                List<Map<String, String>> operations = new ArrayList<>();
                operations.add(createInfoMap("report", "Generate compliance reports"));
                operations.add(createInfoMap("configure", "Configure project compliance requirements"));
                operations.add(createInfoMap("validate", "Validate project against compliance requirements"));
                operations.add(createInfoMap("status", "Show compliance status"));
                jsonHelpData.put("operations", operations);
                
                Map<String, List<Map<String, String>>> operationOptions = new HashMap<>();
                
                List<Map<String, String>> reportOptions = new ArrayList<>();
                reportOptions.add(createInfoMap("--type=<type>", "Compliance framework (GDPR, HIPAA, SOC2, PCI-DSS, ISO27001)"));
                reportOptions.add(createInfoMap("--period=<period>", "Reporting period (e.g., Q1-2025, current)"));
                operationOptions.put("report", reportOptions);
                
                List<Map<String, String>> configureOptions = new ArrayList<>();
                configureOptions.add(createInfoMap("--project=<name>", "Project to configure"));
                operationOptions.put("configure", configureOptions);
                
                List<Map<String, String>> validateOptions = new ArrayList<>();
                validateOptions.add(createInfoMap("--project=<name>", "Project to validate"));
                operationOptions.put("validate", validateOptions);
                
                List<Map<String, String>> statusOptions = new ArrayList<>();
                statusOptions.add(createInfoMap("--project=<name>", "Project to check (optional, defaults to system-wide)"));
                operationOptions.put("status", statusOptions);
                
                jsonHelpData.put("operation_options", operationOptions);
                
                System.out.println(OutputFormatter.toJson(jsonHelpData, verbose));
            } else {
                System.out.println("Usage: rin admin compliance <operation> [options]");
                System.out.println();
                System.out.println("Operations:");
                System.out.println("  report     - Generate compliance reports");
                System.out.println("  configure  - Configure project compliance requirements");
                System.out.println("  validate   - Validate project against compliance requirements");
                System.out.println("  status     - Show compliance status");
                System.out.println();
                System.out.println("Options for 'report':");
                System.out.println("  --type=<type>     - Compliance framework (GDPR, HIPAA, SOC2, PCI-DSS, ISO27001)");
                System.out.println("  --period=<period> - Reporting period (e.g., Q1-2025, current)");
                System.out.println();
                System.out.println("Options for 'configure':");
                System.out.println("  --project=<name>  - Project to configure");
                System.out.println();
                System.out.println("Options for 'validate':");
                System.out.println("  --project=<name>  - Project to validate");
                System.out.println();
                System.out.println("Options for 'status':");
                System.out.println("  --project=<name>  - Project to check (optional, defaults to system-wide)");
                System.out.println();
                System.out.println("For detailed help on a specific operation, use:");
                System.out.println("  rin admin compliance <operation> help");
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
            Map<String, Object> successData = errorHandler.createSuccessResult("admin-compliance", Map.of("message", message));
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