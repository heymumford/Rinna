/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */

package org.rinna.cli.command;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import org.rinna.cli.command.impl.AdminAuditCommand;
import org.rinna.cli.command.impl.AdminBackupCommand;
import org.rinna.cli.command.impl.AdminComplianceCommand;
import org.rinna.cli.command.impl.AdminDiagnosticsCommand;
import org.rinna.cli.command.impl.AdminMonitorCommand;
import org.rinna.cli.command.impl.AdminRecoveryCommand;
import org.rinna.cli.security.SecurityManager;
import org.rinna.cli.service.AuditService;
import org.rinna.cli.service.BackupService;
import org.rinna.cli.service.ComplianceService;
import org.rinna.cli.service.DiagnosticsService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MonitoringService;
import org.rinna.cli.service.RecoveryService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * The AdminCommand handles all administrative operations in Rinna.
 * It delegates to specific admin subcommands based on the first argument.
 * This command follows the ViewCommand pattern with operation tracking.
 */
public class AdminCommand implements Callable<Integer> {
    
    private String subcommand;
    private String[] args = new String[0];
    private String format = "text";
    private boolean verbose = false;
    
    private final ServiceManager serviceManager;
    private final SecurityManager securityManager;
    private final MetadataService metadataService;
    
    /**
     * Default constructor for picocli.
     */
    public AdminCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Constructor with ServiceManager for testing.
     * 
     * @param serviceManager the service manager to use
     */
    public AdminCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.securityManager = SecurityManager.getInstance();
        this.metadataService = serviceManager.getMetadataService();
    }
    
    /**
     * Sets the subcommand to execute.
     * 
     * @param subcommand the subcommand
     */
    public void setSubcommand(String subcommand) {
        this.subcommand = subcommand;
    }
    
    /**
     * Sets the arguments for the subcommand.
     * 
     * @param args the arguments
     */
    public void setArgs(String[] args) {
        this.args = args;
    }
    
    /**
     * Sets the JSON output flag.
     * 
     * @param jsonOutput true to output in JSON format, false for text
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
     * Sets the verbose output flag.
     * 
     * @param verbose true for verbose output, false for normal output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    @Override
    public Integer call() {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("subcommand", subcommand != null ? subcommand : "help");
        params.put("argsCount", args.length);
        if (args.length > 0) {
            params.put("operation", args[0]);
        }
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking operation
        String operationId = metadataService.startOperation("admin", "ADMIN", params);
        
        try {
            if (subcommand == null || subcommand.isEmpty()) {
                displayHelp(operationId);
                return 1;
            }
            
            // Check if user has admin privileges
            if (!checkAdminPrivileges(operationId)) {
                return 1;
            }
            
            // Delegate to the appropriate subcommand
            int result;
            switch (subcommand) {
                case "audit":
                    result = handleAuditCommand(operationId);
                    break;
                
                case "compliance":
                    result = handleComplianceCommand(operationId);
                    break;
                
                case "monitor":
                    result = handleMonitorCommand(operationId);
                    break;
                
                case "diagnostics":
                    result = handleDiagnosticsCommand(operationId);
                    break;
                
                case "backup":
                    result = handleBackupCommand(operationId);
                    break;
                
                case "recovery":
                    result = handleRecoveryCommand(operationId);
                    break;
                
                case "help":
                    displayHelp(operationId);
                    result = 0;
                    break;
                
                default:
                    String errorMessage = "Unknown admin command: " + subcommand;
                    if ("json".equalsIgnoreCase(format)) {
                        Map<String, Object> errorData = new HashMap<>();
                        errorData.put("result", "error");
                        errorData.put("message", errorMessage);
                        System.out.println(OutputFormatter.toJson(errorData, verbose));
                    } else {
                        System.err.println("Error: " + errorMessage);
                        displayHelp(operationId);
                    }
                    metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                    result = 1;
                    break;
            }
            
            // Complete the operation with result
            if (result == 0) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                resultData.put("subcommand", subcommand);
                metadataService.completeOperation(operationId, resultData);
            }
            
            return result;
        } catch (Exception e) {
            String errorMessage = e.getMessage();
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("result", "error");
                errorData.put("message", errorMessage);
                System.out.println(OutputFormatter.toJson(errorData, verbose));
            } else {
                System.err.println("Error: " + errorMessage);
                if (verbose) {
                    e.printStackTrace();
                }
            }
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Handles audit-related commands.
     * 
     * @param operationId the tracking operation ID
     * @return the exit code
     */
    private int handleAuditCommand(String operationId) {
        // Create audit command tracking data
        Map<String, Object> trackingData = new HashMap<>();
        trackingData.put("command", "admin");
        trackingData.put("subcommand", "audit");
        if (args.length > 0) {
            trackingData.put("operation", args[0]);
        }
        
        // Get the audit service from the ServiceManager
        AuditService auditService = serviceManager.getAuditService();
        
        // Create the audit command with the service
        AdminAuditCommand auditCommand = new AdminAuditCommand(serviceManager);
        
        // Set the operation and arguments
        if (args.length > 0) {
            auditCommand.setOperation(args[0]);
            
            if (args.length > 1) {
                String[] operationArgs = Arrays.copyOfRange(args, 1, args.length);
                auditCommand.setArgs(operationArgs);
                trackingData.put("args", Arrays.toString(operationArgs));
            }
        }
        
        // Pass along the format and verbose flags
        auditCommand.setJsonOutput("json".equalsIgnoreCase(format));
        auditCommand.setVerbose(verbose);
        
        // Track operation if applicable
        String auditOperationId = metadataService.trackOperation("admin-audit", trackingData);
        int result = auditCommand.call();
        
        // Update tracking data with result
        trackingData.put("result", result == 0 ? "success" : "error");
        metadataService.completeOperation(auditOperationId, trackingData);
        
        return result;
    }
    
    /**
     * Handles compliance-related commands.
     * 
     * @param operationId the tracking operation ID
     * @return the exit code
     */
    private int handleComplianceCommand(String operationId) {
        // Create compliance command tracking data
        Map<String, Object> trackingData = new HashMap<>();
        trackingData.put("command", "admin");
        trackingData.put("subcommand", "compliance");
        if (args.length > 0) {
            trackingData.put("operation", args[0]);
        }
        
        // Get the compliance service from the ServiceManager
        ComplianceService complianceService = serviceManager.getComplianceService();
        
        // Create the compliance command with the service
        AdminComplianceCommand complianceCommand = new AdminComplianceCommand(serviceManager);
        
        // Set the operation and arguments
        if (args.length > 0) {
            complianceCommand.setOperation(args[0]);
            
            if (args.length > 1) {
                String[] operationArgs = Arrays.copyOfRange(args, 1, args.length);
                complianceCommand.setArgs(operationArgs);
                trackingData.put("args", Arrays.toString(operationArgs));
            }
        }
        
        // Pass along the format and verbose flags
        complianceCommand.setJsonOutput("json".equalsIgnoreCase(format));
        complianceCommand.setVerbose(verbose);
        
        // Track operation if applicable
        String complianceOperationId = metadataService.trackOperation("admin-compliance", trackingData);
        int result = complianceCommand.call();
        
        // Update tracking data with result
        trackingData.put("result", result == 0 ? "success" : "error");
        metadataService.completeOperation(complianceOperationId, trackingData);
        
        return result;
    }
    
    /**
     * Handles system monitoring commands.
     * 
     * @param operationId the tracking operation ID
     * @return the exit code
     */
    private int handleMonitorCommand(String operationId) {
        // Create monitor command tracking data
        Map<String, Object> trackingData = new HashMap<>();
        trackingData.put("command", "admin");
        trackingData.put("subcommand", "monitor");
        if (args.length > 0) {
            trackingData.put("operation", args[0]);
        }
        
        // Get the monitoring service from the ServiceManager
        MonitoringService monitoringService = serviceManager.getMockMonitoringService();
        
        // Create the monitor command with the service
        AdminMonitorCommand monitorCommand = new AdminMonitorCommand(serviceManager);
        
        // Set the operation and arguments
        if (args.length > 0) {
            monitorCommand.setOperation(args[0]);
            
            if (args.length > 1) {
                String[] operationArgs = Arrays.copyOfRange(args, 1, args.length);
                monitorCommand.setArgs(operationArgs);
                trackingData.put("args", Arrays.toString(operationArgs));
            }
        }
        
        // Pass along the format and verbose flags
        monitorCommand.setJsonOutput("json".equalsIgnoreCase(format));
        monitorCommand.setVerbose(verbose);
        
        // Track operation if applicable
        String monitorOperationId = metadataService.trackOperation("admin-monitor", trackingData);
        int result = monitorCommand.call();
        
        // Update tracking data with result
        trackingData.put("result", result == 0 ? "success" : "error");
        metadataService.completeOperation(monitorOperationId, trackingData);
        
        return result;
    }
    
    /**
     * Handles system diagnostics commands.
     * 
     * @param operationId the tracking operation ID
     * @return the exit code
     */
    private int handleDiagnosticsCommand(String operationId) {
        // Create diagnostics command tracking data
        Map<String, Object> trackingData = new HashMap<>();
        trackingData.put("command", "admin");
        trackingData.put("subcommand", "diagnostics");
        if (args.length > 0) {
            trackingData.put("operation", args[0]);
        }
        
        // Get the diagnostics service from the ServiceManager
        DiagnosticsService diagnosticsService = serviceManager.getDiagnosticsService();
        
        // Create the diagnostics command with the service
        AdminDiagnosticsCommand diagnosticsCommand = new AdminDiagnosticsCommand(serviceManager);
        
        // Set the operation and arguments
        if (args.length > 0) {
            diagnosticsCommand.setOperation(args[0]);
            
            if (args.length > 1) {
                String[] operationArgs = Arrays.copyOfRange(args, 1, args.length);
                diagnosticsCommand.setArgs(operationArgs);
                trackingData.put("args", Arrays.toString(operationArgs));
            }
        }
        
        // Pass along the format and verbose flags
        diagnosticsCommand.setJsonOutput("json".equalsIgnoreCase(format));
        diagnosticsCommand.setVerbose(verbose);
        
        // Track operation if applicable
        String diagnosticsOperationId = metadataService.trackOperation("admin-diagnostics", trackingData);
        int result = diagnosticsCommand.call();
        
        // Update tracking data with result
        trackingData.put("result", result == 0 ? "success" : "error");
        metadataService.completeOperation(diagnosticsOperationId, trackingData);
        
        return result;
    }
    
    /**
     * Handles backup-related commands.
     * 
     * @param operationId the tracking operation ID
     * @return the exit code
     */
    private int handleBackupCommand(String operationId) {
        // Create backup command tracking data
        Map<String, Object> trackingData = new HashMap<>();
        trackingData.put("command", "admin");
        trackingData.put("subcommand", "backup");
        if (args.length > 0) {
            trackingData.put("operation", args[0]);
        }
        
        // Get the backup service from the ServiceManager
        BackupService backupService = serviceManager.getBackupService();
        
        // Create the backup command with the service
        AdminBackupCommand backupCommand = new AdminBackupCommand(serviceManager);
        
        // Set the operation and arguments
        if (args.length > 0) {
            backupCommand.setOperation(args[0]);
            
            if (args.length > 1) {
                String[] operationArgs = Arrays.copyOfRange(args, 1, args.length);
                backupCommand.setArgs(operationArgs);
                trackingData.put("args", Arrays.toString(operationArgs));
            }
        }
        
        // Pass along the format and verbose flags
        backupCommand.setJsonOutput("json".equalsIgnoreCase(format));
        backupCommand.setVerbose(verbose);
        
        // Track operation if applicable
        String backupOperationId = metadataService.trackOperation("admin-backup", trackingData);
        int result = backupCommand.call();
        
        // Update tracking data with result
        trackingData.put("result", result == 0 ? "success" : "error");
        metadataService.completeOperation(backupOperationId, trackingData);
        
        return result;
    }
    
    /**
     * Handles recovery-related commands.
     * 
     * @param operationId the tracking operation ID
     * @return the exit code
     */
    private int handleRecoveryCommand(String operationId) {
        // Create recovery command tracking data
        Map<String, Object> trackingData = new HashMap<>();
        trackingData.put("command", "admin");
        trackingData.put("subcommand", "recovery");
        if (args.length > 0) {
            trackingData.put("operation", args[0]);
        }
        
        // Get the recovery service from the ServiceManager
        RecoveryService recoveryService = serviceManager.getMockRecoveryService();
        
        // Create the recovery command with the service
        AdminRecoveryCommand recoveryCommand = new AdminRecoveryCommand(serviceManager);
        
        // Set the operation and arguments
        if (args.length > 0) {
            recoveryCommand.setOperation(args[0]);
            
            if (args.length > 1) {
                String[] operationArgs = Arrays.copyOfRange(args, 1, args.length);
                recoveryCommand.setArgs(operationArgs);
                trackingData.put("args", Arrays.toString(operationArgs));
            }
        }
        
        // Pass along the format and verbose flags
        recoveryCommand.setJsonOutput("json".equalsIgnoreCase(format));
        recoveryCommand.setVerbose(verbose);
        
        // Track operation if applicable
        String recoveryOperationId = metadataService.trackOperation("admin-recovery", trackingData);
        int result = recoveryCommand.call();
        
        // Update tracking data with result
        trackingData.put("result", result == 0 ? "success" : "error");
        metadataService.completeOperation(recoveryOperationId, trackingData);
        
        return result;
    }
    
    /**
     * Verifies that the current user has administrative privileges for the current operation.
     * 
     * @param operationId the tracking operation ID
     * @return true if the user has admin privileges
     */
    private boolean checkAdminPrivileges(String operationId) {
        // First check if user is authenticated
        if (!securityManager.isAuthenticated()) {
            String errorMessage = "Authentication required. Please log in first.";
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("result", "error");
                errorData.put("message", errorMessage);
                errorData.put("action", "Use 'rin login' to authenticate");
                System.out.println(OutputFormatter.toJson(errorData, verbose));
            } else {
                System.err.println("Error: " + errorMessage);
                System.err.println("Use 'rin login' to authenticate.");
            }
            metadataService.failOperation(operationId, new SecurityException(errorMessage));
            return false;
        }
        
        // Check if user is a full admin
        if (securityManager.isAdmin()) {
            return true;
        }
        
        // Check for area-specific admin access
        if (subcommand != null && !subcommand.isEmpty()) {
            if (securityManager.hasAdminAccess(subcommand)) {
                return true;
            }
        }
        
        String errorMessage = "You do not have administrative privileges for the '" 
                + subcommand + "' area.";
        if ("json".equalsIgnoreCase(format)) {
            Map<String, Object> errorData = new HashMap<>();
            errorData.put("result", "error");
            errorData.put("message", errorMessage);
            errorData.put("action", "Contact your system administrator for access");
            System.out.println(OutputFormatter.toJson(errorData, verbose));
        } else {
            System.err.println("Error: " + errorMessage);
            System.err.println("Contact your system administrator for access.");
        }
        metadataService.failOperation(operationId, new SecurityException(errorMessage));
        return false;
    }
    
    /**
     * Displays help information for admin commands.
     * 
     * @param operationId the tracking operation ID
     */
    private void displayHelp(String operationId) {
        // Collect help data for tracking
        Map<String, Object> helpData = new HashMap<>();
        helpData.put("command", "admin");
        helpData.put("action", "help");
        
        if ("json".equalsIgnoreCase(format)) {
            // Create JSON data for help output
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("result", "success");
            jsonData.put("command", "admin");
            jsonData.put("usage", "rin admin <command> [options]");
            
            // Create commands list
            List<Map<String, String>> commands = new ArrayList<>();
            commands.add(createCommandInfo("audit", "Audit log management and reporting"));
            commands.add(createCommandInfo("compliance", "Regulatory compliance management"));
            commands.add(createCommandInfo("monitor", "System health monitoring"));
            commands.add(createCommandInfo("diagnostics", "System diagnostics and troubleshooting"));
            commands.add(createCommandInfo("backup", "Data backup configuration and execution"));
            commands.add(createCommandInfo("recovery", "System recovery from backups"));
            jsonData.put("commands", commands);
            
            // Add help tip and options
            jsonData.put("help_tip", "Run 'rin admin <command> help' for more information on a specific command");
            
            List<Map<String, String>> options = new ArrayList<>();
            options.add(createCommandInfo("--json", "Output in JSON format"));
            options.add(createCommandInfo("--verbose", "Show verbose output with additional details"));
            jsonData.put("options", options);
            
            // Output JSON
            System.out.println(OutputFormatter.toJson(jsonData, verbose));
        } else {
            // Plain text output
            System.out.println("Usage: rin admin <command> [options]");
            System.out.println();
            System.out.println("Administrative Commands:");
            System.out.println("  audit       - Audit log management and reporting");
            System.out.println("  compliance  - Regulatory compliance management");
            System.out.println("  monitor     - System health monitoring");
            System.out.println("  diagnostics - System diagnostics and troubleshooting");
            System.out.println("  backup      - Data backup configuration and execution");
            System.out.println("  recovery    - System recovery from backups");
            System.out.println();
            System.out.println("Options:");
            System.out.println("  --json      - Output in JSON format");
            System.out.println("  --verbose   - Show verbose output with additional details");
            System.out.println();
            System.out.println("Run 'rin admin <command> help' for more information on a specific command.");
        }
        
        // Complete the operation
        metadataService.completeOperation(operationId, helpData);
    }
    
    /**
     * Helper method to create command info maps for JSON output.
     * 
     * @param name the command name
     * @param description the command description
     * @return a map containing the command info
     */
    private Map<String, String> createCommandInfo(String name, String description) {
        Map<String, String> info = new HashMap<>();
        info.put("name", name);
        info.put("description", description);
        return info;
    }
}