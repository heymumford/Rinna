/*
 * Administrative backup command handler for Rinna.
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.command.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

import org.rinna.cli.service.BackupService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

/**
 * Command handler for backup-related operations.
 * This class implements the functionality for the 'rin admin backup' command.
 * It follows the ViewCommand pattern with MetadataService integration for operation tracking.
 */
public class AdminBackupCommand implements Callable<Integer> {
    
    private String operation;
    private String[] args = new String[0];
    private final ServiceManager serviceManager;
    private final Scanner scanner;
    private final MetadataService metadataService;
    
    private String format = "text";
    private boolean verbose = false;
    
    /**
     * Creates a new AdminBackupCommand with the specified ServiceManager.
     * 
     * @param serviceManager the service manager
     */
    public AdminBackupCommand(ServiceManager serviceManager) {
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
        String operationId = metadataService.startOperation("admin-backup", "BACKUP", params);
        
        try {
            if (operation == null || operation.isEmpty()) {
                displayHelp(operationId);
                return 1;
            }
            
            // Get the backup service
            BackupService backupService = serviceManager.getBackupService();
            if (backupService == null) {
                String errorMessage = "Backup service is not available.";
                outputError(errorMessage);
                metadataService.failOperation(operationId, new IllegalStateException(errorMessage));
                return 1;
            }
            
            // Delegate to the appropriate operation
            int result;
            switch (operation) {
                case "configure":
                    result = handleConfigureOperation(backupService, operationId);
                    break;
                
                case "status":
                    result = handleStatusOperation(backupService, operationId);
                    break;
                
                case "start":
                    result = handleStartOperation(backupService, operationId);
                    break;
                
                case "list":
                    result = handleListOperation(backupService, operationId);
                    break;
                
                case "strategy":
                    result = handleStrategyOperation(backupService, operationId);
                    break;
                
                case "history":
                    result = handleHistoryOperation(backupService, operationId);
                    break;
                
                case "security":
                    result = handleSecurityOperation(backupService, operationId);
                    break;
                
                case "verify":
                    result = handleVerifyOperation(backupService, operationId);
                    break;
                
                case "notifications":
                    result = handleNotificationsOperation(backupService, operationId);
                    break;
                
                case "locations":
                    result = handleLocationsOperation(backupService, operationId);
                    break;
                
                case "help":
                    displayHelp(operationId);
                    result = 0;
                    break;
                
                default:
                    String errorMessage = "Unknown backup operation: " + operation;
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
     * Handles the 'configure' operation to set up backup settings.
     * 
     * @param backupService the backup service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleConfigureOperation(BackupService backupService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "configure");
        
        // Start tracking sub-operation
        String configOperationId = metadataService.trackOperation("admin-backup-configure", params);
        
        try {
            System.out.println("Backup Configuration");
            System.out.println("====================");
            System.out.println();
            
            System.out.println("Select backup type:");
            System.out.println("1. full - Complete backup of all data");
            System.out.println("2. incremental - Backup of changed data only");
            System.out.println("3. differential - Backup of all changes since last full backup");
            System.out.print("Enter choice [1]: ");
            
            String typeInput = scanner.nextLine().trim();
            String type = "full"; // Default
            
            if (!typeInput.isEmpty()) {
                switch (typeInput) {
                    case "1":
                        type = "full";
                        break;
                    case "2":
                        type = "incremental";
                        break;
                    case "3":
                        type = "differential";
                        break;
                    default:
                        if ("full".equals(typeInput) || "incremental".equals(typeInput) || "differential".equals(typeInput)) {
                            type = typeInput;
                        } else {
                            outputError("Invalid backup type. Using default (full).");
                        }
                        break;
                }
            }
            
            System.out.print("Enter backup frequency (daily, weekly, monthly) [daily]: ");
            String frequency = scanner.nextLine().trim();
            if (frequency.isEmpty()) {
                frequency = "daily"; // Default
            }
            
            System.out.print("Enter backup time (HH:MM in 24-hour format) [02:00]: ");
            String time = scanner.nextLine().trim();
            if (time.isEmpty()) {
                time = "02:00"; // Default
            }
            
            System.out.print("Enter retention period in days [30]: ");
            String retentionInput = scanner.nextLine().trim();
            int retention = 30; // Default
            
            if (!retentionInput.isEmpty()) {
                try {
                    retention = Integer.parseInt(retentionInput);
                    if (retention <= 0) {
                        outputError("Retention period must be greater than 0. Using default (30).");
                        retention = 30;
                    }
                } catch (NumberFormatException e) {
                    outputError("Invalid retention period. Must be a number. Using default (30).");
                }
            }
            
            System.out.print("Enter backup location (directory path) [/var/backups/rinna]: ");
            String location = scanner.nextLine().trim();
            if (location.isEmpty()) {
                location = "/var/backups/rinna"; // Default
            }
            
            // Update tracking parameters with collected data
            params.put("type", type);
            params.put("frequency", frequency);
            params.put("time", time);
            params.put("retention", retention);
            params.put("location", location);
            
            // Make service call
            boolean success = backupService.configureBackup(type, frequency, time, retention, location);
            
            if (success) {
                if ("json".equalsIgnoreCase(format)) {
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("result", "success");
                    resultData.put("operation", "configure");
                    
                    Map<String, Object> configData = new HashMap<>();
                    configData.put("type", type);
                    configData.put("frequency", frequency);
                    configData.put("time", time);
                    configData.put("retention", retention);
                    configData.put("location", location);
                    
                    resultData.put("data", configData);
                    resultData.put("message", "Backup configuration updated successfully");
                    
                    System.out.println(toJson(resultData));
                } else {
                    System.out.println();
                    System.out.println("Backup configuration updated successfully!");
                    System.out.println("New configuration will take effect immediately.");
                }
                
                // Track operation success
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                resultData.put("type", type);
                resultData.put("frequency", frequency);
                resultData.put("time", time);
                resultData.put("retention", retention);
                resultData.put("location", location);
                metadataService.completeOperation(configOperationId, resultData);
                
                return 0;
            } else {
                String errorMessage = "Failed to update backup configuration.";
                outputError(errorMessage);
                
                // Track operation failure
                metadataService.failOperation(configOperationId, new RuntimeException(errorMessage));
                
                return 1;
            }
        } catch (Exception e) {
            String errorMessage = "Error configuring backup: " + e.getMessage();
            outputError(errorMessage);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            // Track operation failure
            metadataService.failOperation(configOperationId, e);
            
            return 1;
        }
    }
    
    /**
     * Handles the 'status' operation to display backup configuration status.
     * 
     * @param backupService the backup service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleStatusOperation(BackupService backupService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "status");
        
        // Start tracking sub-operation
        String statusOperationId = metadataService.trackOperation("admin-backup-status", params);
        
        try {
            String status = backupService.getBackupStatus();
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "status");
                resultData.put("status", status);
                
                System.out.println(toJson(resultData));
            } else {
                System.out.println(status);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            metadataService.completeOperation(statusOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error getting backup status: " + e.getMessage();
            outputError(errorMessage);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            // Track operation failure
            metadataService.failOperation(statusOperationId, e);
            
            return 1;
        }
    }
    
    /**
     * Handles the 'start' operation to initiate a backup.
     * 
     * @param backupService the backup service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleStartOperation(BackupService backupService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "start");
        
        Map<String, String> options = parseOptions(args);
        String type = options.getOrDefault("type", "full");
        params.put("type", type);
        
        // Start tracking sub-operation
        String startOperationId = metadataService.trackOperation("admin-backup-start", params);
        
        try {
            if (!Arrays.asList("full", "incremental", "differential").contains(type)) {
                String errorMessage = "Invalid backup type. Must be 'full', 'incremental', or 'differential'.";
                outputError(errorMessage);
                
                // Track operation failure
                metadataService.failOperation(startOperationId, new IllegalArgumentException(errorMessage));
                
                return 1;
            }
            
            String backupId = backupService.startBackup(type);
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "start");
                resultData.put("type", type);
                resultData.put("backupId", backupId);
                resultData.put("message", "Backup completed successfully");
                
                System.out.println(toJson(resultData));
            } else {
                System.out.println("Backup completed successfully!");
                System.out.println("Backup ID: " + backupId);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("type", type);
            resultData.put("backupId", backupId);
            metadataService.completeOperation(startOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error starting backup: " + e.getMessage();
            outputError(errorMessage);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            // Track operation failure
            metadataService.failOperation(startOperationId, e);
            
            return 1;
        }
    }
    
    /**
     * Handles the 'list' operation to display available backups.
     * 
     * @param backupService the backup service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleListOperation(BackupService backupService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "list");
        
        // Start tracking sub-operation
        String listOperationId = metadataService.trackOperation("admin-backup-list", params);
        
        try {
            String list = backupService.listBackups();
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "list");
                resultData.put("backups", list);
                
                System.out.println(toJson(resultData));
            } else {
                System.out.println(list);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            metadataService.completeOperation(listOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error listing backups: " + e.getMessage();
            outputError(errorMessage);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            // Track operation failure
            metadataService.failOperation(listOperationId, e);
            
            return 1;
        }
    }
    
    /**
     * Handles the 'strategy' operation to manage backup strategies.
     * 
     * @param backupService the backup service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleStrategyOperation(BackupService backupService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "strategy");
        
        if (args.length == 0) {
            outputError("Missing strategy subcommand. Use 'configure' or 'status'.");
            
            // Track operation failure
            String strategyOperationId = metadataService.trackOperation("admin-backup-strategy", params);
            metadataService.failOperation(strategyOperationId, 
                new IllegalArgumentException("Missing strategy subcommand"));
            
            return 1;
        }
        
        String strategyOperation = args[0];
        params.put("suboperation", strategyOperation);
        
        // Start tracking sub-operation
        String strategyOperationId = metadataService.trackOperation("admin-backup-strategy", params);
        
        try {
            if ("configure".equals(strategyOperation)) {
                System.out.println("Backup Strategy Configuration");
                System.out.println("============================");
                System.out.println();
                
                System.out.println("Select backup strategy:");
                System.out.println("1. full-only - Only run full backups");
                System.out.println("2. incremental - Run incremental backups between full backups");
                System.out.println("3. differential - Run differential backups between full backups");
                System.out.print("Enter choice [2]: ");
                
                String strategyInput = scanner.nextLine().trim();
                String strategy = "incremental"; // Default
                
                if (!strategyInput.isEmpty()) {
                    switch (strategyInput) {
                        case "1":
                            strategy = "full-only";
                            break;
                        case "2":
                            strategy = "incremental";
                            break;
                        case "3":
                            strategy = "differential";
                            break;
                        default:
                            if ("full-only".equals(strategyInput) || "incremental".equals(strategyInput) || 
                                "differential".equals(strategyInput)) {
                                strategy = strategyInput;
                            } else {
                                outputError("Invalid backup strategy. Using default (incremental).");
                            }
                            break;
                    }
                }
                
                String fullFrequency = "weekly"; // Default
                String incrementalFrequency = "daily"; // Default
                
                if (!"full-only".equals(strategy)) {
                    System.out.print("Select full backup frequency (weekly, monthly) [weekly]: ");
                    String fullFreqInput = scanner.nextLine().trim();
                    if (!fullFreqInput.isEmpty()) {
                        if ("weekly".equals(fullFreqInput) || "monthly".equals(fullFreqInput)) {
                            fullFrequency = fullFreqInput;
                        } else {
                            outputError("Invalid frequency. Using default (weekly).");
                        }
                    }
                    
                    String incrementalLabel = "incremental".equals(strategy) ? "incremental" : "differential";
                    System.out.print("Select " + incrementalLabel + " backup frequency (daily, bidaily) [daily]: ");
                    String incFreqInput = scanner.nextLine().trim();
                    if (!incFreqInput.isEmpty()) {
                        if ("daily".equals(incFreqInput) || "bidaily".equals(incFreqInput)) {
                            incrementalFrequency = incFreqInput;
                        } else {
                            outputError("Invalid frequency. Using default (daily).");
                        }
                    }
                }
                
                // Update tracking parameters with collected data
                params.put("strategy", strategy);
                params.put("fullFrequency", fullFrequency);
                params.put("incrementalFrequency", incrementalFrequency);
                
                boolean success = backupService.configureBackupStrategy(strategy, fullFrequency, incrementalFrequency);
                if (success) {
                    if ("json".equalsIgnoreCase(format)) {
                        Map<String, Object> resultData = new HashMap<>();
                        resultData.put("result", "success");
                        resultData.put("operation", "strategy");
                        resultData.put("suboperation", "configure");
                        
                        Map<String, Object> strategyData = new HashMap<>();
                        strategyData.put("strategy", strategy);
                        strategyData.put("fullFrequency", fullFrequency);
                        strategyData.put("incrementalFrequency", incrementalFrequency);
                        
                        resultData.put("data", strategyData);
                        resultData.put("message", "Backup strategy updated successfully");
                        
                        System.out.println(toJson(resultData));
                    } else {
                        System.out.println();
                        System.out.println("Backup strategy updated successfully!");
                        System.out.println("New strategy will be used for next scheduled backup.");
                    }
                    
                    // Track operation success
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("success", true);
                    resultData.put("strategy", strategy);
                    resultData.put("fullFrequency", fullFrequency);
                    resultData.put("incrementalFrequency", incrementalFrequency);
                    metadataService.completeOperation(strategyOperationId, resultData);
                    
                    return 0;
                } else {
                    String errorMessage = "Failed to update backup strategy.";
                    outputError(errorMessage);
                    
                    // Track operation failure
                    metadataService.failOperation(strategyOperationId, new RuntimeException(errorMessage));
                    
                    return 1;
                }
            } else if ("status".equals(strategyOperation)) {
                String status = backupService.getBackupStrategyStatus();
                
                if ("json".equalsIgnoreCase(format)) {
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("result", "success");
                    resultData.put("operation", "strategy");
                    resultData.put("suboperation", "status");
                    resultData.put("status", status);
                    
                    System.out.println(toJson(resultData));
                } else {
                    System.out.println(status);
                }
                
                // Track operation success
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                metadataService.completeOperation(strategyOperationId, resultData);
                
                return 0;
            } else {
                String errorMessage = "Unknown strategy operation: " + strategyOperation;
                outputError(errorMessage);
                System.out.println("Valid operations: configure, status");
                
                // Track operation failure
                metadataService.failOperation(strategyOperationId, 
                    new IllegalArgumentException(errorMessage));
                
                return 1;
            }
        } catch (Exception e) {
            String errorMessage = "Error in strategy operation: " + e.getMessage();
            outputError(errorMessage);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            // Track operation failure
            metadataService.failOperation(strategyOperationId, e);
            
            return 1;
        }
    }
    
    /**
     * Handles the 'history' operation to display backup history.
     * 
     * @param backupService the backup service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleHistoryOperation(BackupService backupService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "history");
        
        // Start tracking sub-operation
        String historyOperationId = metadataService.trackOperation("admin-backup-history", params);
        
        try {
            String history = backupService.getBackupHistory();
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "history");
                resultData.put("history", history);
                
                System.out.println(toJson(resultData));
            } else {
                System.out.println(history);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            metadataService.completeOperation(historyOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error getting backup history: " + e.getMessage();
            outputError(errorMessage);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            // Track operation failure
            metadataService.failOperation(historyOperationId, e);
            
            return 1;
        }
    }
    
    /**
     * Handles the 'security' operation to manage backup security.
     * 
     * @param backupService the backup service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleSecurityOperation(BackupService backupService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "security");
        
        if (args.length == 0) {
            outputError("Missing security subcommand. Use 'configure' or 'status'.");
            
            // Track operation failure
            String securityOperationId = metadataService.trackOperation("admin-backup-security", params);
            metadataService.failOperation(securityOperationId, 
                new IllegalArgumentException("Missing security subcommand"));
            
            return 1;
        }
        
        String securityOperation = args[0];
        params.put("suboperation", securityOperation);
        
        // Start tracking sub-operation
        String securityOperationId = metadataService.trackOperation("admin-backup-security", params);
        
        if ("configure".equals(securityOperation)) {
            System.out.println("Backup Security Configuration");
            System.out.println("============================");
            System.out.println();
            
            System.out.print("Enable encryption for backups? (yes/no) [yes]: ");
            String enableInput = scanner.nextLine().trim().toLowerCase();
            boolean enableEncryption = true; // Default
            
            if (!enableInput.isEmpty()) {
                if ("no".equals(enableInput) || "n".equals(enableInput)) {
                    enableEncryption = false;
                } else if (!"yes".equals(enableInput) && !"y".equals(enableInput)) {
                    System.err.println("Error: Invalid input. Using default (yes).");
                }
            }
            
            String algorithm = "AES-256"; // Default
            String passphrase = null;
            
            if (enableEncryption) {
                System.out.println("Select encryption algorithm:");
                System.out.println("1. AES-256 (Recommended)");
                System.out.println("2. Twofish");
                System.out.println("3. ChaCha20");
                System.out.print("Enter choice [1]: ");
                
                String algorithmInput = scanner.nextLine().trim();
                if (!algorithmInput.isEmpty()) {
                    switch (algorithmInput) {
                        case "1":
                            algorithm = "AES-256";
                            break;
                        case "2":
                            algorithm = "Twofish";
                            break;
                        case "3":
                            algorithm = "ChaCha20";
                            break;
                        default:
                            System.err.println("Error: Invalid algorithm. Using default (AES-256).");
                            break;
                    }
                }
                
                System.out.print("Enter a secure passphrase: ");
                passphrase = scanner.nextLine().trim();
                
                if (passphrase.isEmpty()) {
                    String errorMessage = "Passphrase cannot be empty.";
                    outputError(errorMessage);
                    metadataService.failOperation(securityOperationId, 
                        new IllegalArgumentException(errorMessage));
                    return 1;
                }
                
                System.out.print("Confirm passphrase: ");
                String confirmPassphrase = scanner.nextLine().trim();
                
                if (!passphrase.equals(confirmPassphrase)) {
                    String errorMessage = "Passphrases do not match.";
                    outputError(errorMessage);
                    metadataService.failOperation(securityOperationId, 
                        new IllegalArgumentException(errorMessage));
                    return 1;
                }
            }
            
            // Update tracking parameters with collected data
            params.put("enableEncryption", enableEncryption);
            params.put("algorithm", algorithm);
            params.put("hasPassphrase", passphrase != null && !passphrase.isEmpty());
            
            try {
                boolean success = backupService.configureBackupSecurity(enableEncryption, algorithm, passphrase);
                if (success) {
                    if ("json".equalsIgnoreCase(format)) {
                        Map<String, Object> resultData = new HashMap<>();
                        resultData.put("result", "success");
                        resultData.put("operation", "security");
                        resultData.put("suboperation", "configure");
                        
                        Map<String, Object> securityData = new HashMap<>();
                        securityData.put("enableEncryption", enableEncryption);
                        securityData.put("algorithm", algorithm);
                        securityData.put("hasPassphrase", passphrase != null && !passphrase.isEmpty());
                        
                        resultData.put("data", securityData);
                        resultData.put("message", "Backup security configured successfully");
                        
                        System.out.println(toJson(resultData));
                    } else {
                        System.out.println();
                        System.out.println("Backup security configured successfully!");
                        if (enableEncryption) {
                            System.out.println("All future backups will be encrypted.");
                            System.out.println("IMPORTANT: Store your passphrase securely. If lost, backups cannot be recovered.");
                        } else {
                            System.out.println("Encryption disabled. Backups will be stored unencrypted.");
                        }
                    }
                    
                    // Track operation success
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("success", true);
                    resultData.put("enableEncryption", enableEncryption);
                    resultData.put("algorithm", algorithm);
                    resultData.put("hasPassphrase", passphrase != null && !passphrase.isEmpty());
                    metadataService.completeOperation(securityOperationId, resultData);
                    
                    return 0;
                } else {
                    String errorMessage = "Failed to configure backup security.";
                    outputError(errorMessage);
                    
                    // Track operation failure
                    metadataService.failOperation(securityOperationId, new RuntimeException(errorMessage));
                    
                    return 1;
                }
            } catch (Exception e) {
                String errorMessage = "Error configuring backup security: " + e.getMessage();
                outputError(errorMessage);
                
                if (verbose) {
                    e.printStackTrace();
                }
                
                // Track operation failure
                metadataService.failOperation(securityOperationId, e);
                
                return 1;
            }
        } else if ("status".equals(securityOperation)) {
            try {
                String status = backupService.getBackupSecurityStatus();
                
                if ("json".equalsIgnoreCase(format)) {
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("result", "success");
                    resultData.put("operation", "security");
                    resultData.put("suboperation", "status");
                    resultData.put("status", status);
                    
                    System.out.println(toJson(resultData));
                } else {
                    System.out.println(status);
                }
                
                // Track operation success
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                metadataService.completeOperation(securityOperationId, resultData);
                
                return 0;
            } catch (Exception e) {
                String errorMessage = "Error getting backup security status: " + e.getMessage();
                outputError(errorMessage);
                
                if (verbose) {
                    e.printStackTrace();
                }
                
                // Track operation failure
                metadataService.failOperation(securityOperationId, e);
                
                return 1;
            }
        } else {
            String errorMessage = "Unknown security operation: " + securityOperation;
            outputError(errorMessage);
            System.out.println("Valid operations: configure, status");
            
            // Track operation failure
            metadataService.failOperation(securityOperationId, 
                new IllegalArgumentException(errorMessage));
            
            return 1;
        }
        
    }
    
    /**
     * Handles the 'verify' operation to check backup integrity.
     * 
     * @param backupService the backup service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleVerifyOperation(BackupService backupService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "verify");
        
        Map<String, String> options = parseOptions(args);
        String backupId = options.getOrDefault("backup-id", "latest");
        params.put("backupId", backupId);
        
        // Start tracking sub-operation
        String verifyOperationId = metadataService.trackOperation("admin-backup-verify", params);
        
        try {
            String report = backupService.verifyBackup(backupId);
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "verify");
                resultData.put("backupId", backupId);
                resultData.put("report", report);
                
                System.out.println(toJson(resultData));
            } else {
                System.out.println(report);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("backupId", backupId);
            metadataService.completeOperation(verifyOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error verifying backup: " + e.getMessage();
            outputError(errorMessage);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            // Track operation failure
            metadataService.failOperation(verifyOperationId, e);
            
            return 1;
        }
    }
    
    /**
     * Handles the 'notifications' operation to manage backup notifications.
     * 
     * @param backupService the backup service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleNotificationsOperation(BackupService backupService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "notifications");
        
        if (args.length == 0) {
            outputError("Missing notifications subcommand. Use 'configure' or 'status'.");
            
            // Track operation failure
            String notificationOperationId = metadataService.trackOperation("admin-backup-notifications", params);
            metadataService.failOperation(notificationOperationId, 
                new IllegalArgumentException("Missing notifications subcommand"));
            
            return 1;
        }
        
        String notificationOperation = args[0];
        params.put("suboperation", notificationOperation);
        
        // Start tracking sub-operation
        String notificationOperationId = metadataService.trackOperation("admin-backup-notifications", params);
        
        try {
            if ("configure".equals(notificationOperation)) {
                System.out.println("Backup Notification Configuration");
                System.out.println("==============================");
                System.out.println();
                
                System.out.print("Enable backup notifications? (yes/no) [yes]: ");
                String enableInput = scanner.nextLine().trim().toLowerCase();
                boolean enableNotifications = true; // Default
                
                if (!enableInput.isEmpty()) {
                    if ("no".equals(enableInput) || "n".equals(enableInput)) {
                        enableNotifications = false;
                    } else if (!"yes".equals(enableInput) && !"y".equals(enableInput)) {
                        outputError("Invalid input. Using default (yes).");
                    }
                }
                
                List<String> events = new ArrayList<>();
                String recipients = "";
                
                if (enableNotifications) {
                    System.out.println("Select notification events (comma-separated):");
                    System.out.println("- success: Notify on successful backups");
                    System.out.println("- failure: Notify on failed backups");
                    System.out.println("- warning: Notify on backups with warnings");
                    System.out.println("- all: Notify on all backup events");
                    System.out.print("Enter events [all]: ");
                    
                    String eventsInput = scanner.nextLine().trim();
                    if (eventsInput.isEmpty()) {
                        events.add("all"); // Default
                    } else {
                        events = Arrays.asList(eventsInput.split("\\s*,\\s*"));
                    }
                    
                    System.out.print("Enter notification recipients (comma-separated email addresses): ");
                    recipients = scanner.nextLine().trim();
                    
                    if (recipients.isEmpty()) {
                        String errorMessage = "Recipients cannot be empty.";
                        outputError(errorMessage);
                        metadataService.failOperation(notificationOperationId, 
                            new IllegalArgumentException(errorMessage));
                        return 1;
                    }
                }
                
                // Update tracking parameters with collected data
                params.put("enableNotifications", enableNotifications);
                params.put("events", events);
                params.put("recipients", recipients);
                
                boolean success = backupService.configureBackupNotifications(enableNotifications, events, recipients);
                if (success) {
                    if ("json".equalsIgnoreCase(format)) {
                        Map<String, Object> resultData = new HashMap<>();
                        resultData.put("result", "success");
                        resultData.put("operation", "notifications");
                        resultData.put("suboperation", "configure");
                        
                        Map<String, Object> notificationData = new HashMap<>();
                        notificationData.put("enableNotifications", enableNotifications);
                        notificationData.put("events", events);
                        notificationData.put("recipients", recipients);
                        
                        resultData.put("data", notificationData);
                        resultData.put("message", "Backup notifications configured successfully");
                        
                        System.out.println(toJson(resultData));
                    } else {
                        System.out.println();
                        System.out.println("Backup notifications configured successfully!");
                        if (enableNotifications) {
                            System.out.println("Notifications will be sent for future backup operations.");
                        } else {
                            System.out.println("Notifications disabled. No notifications will be sent for backup operations.");
                        }
                    }
                    
                    // Track operation success
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("success", true);
                    resultData.put("enableNotifications", enableNotifications);
                    resultData.put("events", events);
                    resultData.put("recipients", recipients);
                    metadataService.completeOperation(notificationOperationId, resultData);
                    
                    return 0;
                } else {
                    String errorMessage = "Failed to configure backup notifications.";
                    outputError(errorMessage);
                    
                    // Track operation failure
                    metadataService.failOperation(notificationOperationId, new RuntimeException(errorMessage));
                    
                    return 1;
                }
            } else if ("status".equals(notificationOperation)) {
                String status = backupService.getBackupNotificationsStatus();
                
                if ("json".equalsIgnoreCase(format)) {
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("result", "success");
                    resultData.put("operation", "notifications");
                    resultData.put("suboperation", "status");
                    resultData.put("status", status);
                    
                    System.out.println(toJson(resultData));
                } else {
                    System.out.println(status);
                }
                
                // Track operation success
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                metadataService.completeOperation(notificationOperationId, resultData);
                
                return 0;
            } else {
                String errorMessage = "Unknown notifications operation: " + notificationOperation;
                outputError(errorMessage);
                System.out.println("Valid operations: configure, status");
                
                // Track operation failure
                metadataService.failOperation(notificationOperationId, 
                    new IllegalArgumentException(errorMessage));
                
                return 1;
            }
        } catch (Exception e) {
            String errorMessage = "Error in notifications operation: " + e.getMessage();
            outputError(errorMessage);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            // Track operation failure
            metadataService.failOperation(notificationOperationId, e);
            
            return 1;
        }
    }
    
    /**
     * Handles the 'locations' operation to manage backup storage locations.
     * 
     * @param backupService the backup service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleLocationsOperation(BackupService backupService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "locations");
        
        if (args.length == 0) {
            outputError("Missing locations subcommand. Use 'configure' or 'list'.");
            
            // Track operation failure
            String locationsOperationId = metadataService.trackOperation("admin-backup-locations", params);
            metadataService.failOperation(locationsOperationId, 
                new IllegalArgumentException("Missing locations subcommand"));
            
            return 1;
        }
        
        String locationsOperation = args[0];
        params.put("suboperation", locationsOperation);
        
        // Start tracking sub-operation
        String locationsOperationId = metadataService.trackOperation("admin-backup-locations", params);
        
        try {
            if ("configure".equals(locationsOperation)) {
                System.out.println("Backup Location Configuration");
                System.out.println("===========================");
                System.out.println();
                System.out.println("Configure backup storage locations.");
                
                List<String> locations = new ArrayList<>();
                String location;
                
                do {
                    System.out.print("Enter a backup location (directory path): ");
                    location = scanner.nextLine().trim();
                    
                    if (!location.isEmpty() && !location.equals("finish")) {
                        locations.add(location);
                        System.out.print("Add another location or type 'finish' to complete: ");
                    }
                } while (!location.isEmpty() && !location.equals("finish"));
                
                if (locations.isEmpty()) {
                    String errorMessage = "At least one backup location is required.";
                    outputError(errorMessage);
                    metadataService.failOperation(locationsOperationId, 
                        new IllegalArgumentException(errorMessage));
                    return 1;
                }
                
                String strategy = "synchronized"; // Default
                
                if (locations.size() > 1) {
                    System.out.println("Select mirroring strategy:");
                    System.out.println("1. synchronized - Write to all locations simultaneously");
                    System.out.println("2. sequenced - Write to primary, then copy to secondary");
                    System.out.println("3. load-balanced - Distribute backups among locations");
                    System.out.print("Enter choice [1]: ");
                    
                    String strategyInput = scanner.nextLine().trim();
                    if (!strategyInput.isEmpty()) {
                        switch (strategyInput) {
                            case "1":
                                strategy = "synchronized";
                                break;
                            case "2":
                                strategy = "sequenced";
                                break;
                            case "3":
                                strategy = "load-balanced";
                                break;
                            default:
                                outputError("Invalid strategy. Using default (synchronized).");
                                break;
                        }
                    }
                }
                
                // Update tracking parameters with collected data
                params.put("locations", locations);
                params.put("strategy", strategy);
                
                boolean success = backupService.configureBackupLocations(locations, strategy);
                if (success) {
                    if ("json".equalsIgnoreCase(format)) {
                        Map<String, Object> resultData = new HashMap<>();
                        resultData.put("result", "success");
                        resultData.put("operation", "locations");
                        resultData.put("suboperation", "configure");
                        
                        Map<String, Object> locationData = new HashMap<>();
                        locationData.put("locations", locations);
                        locationData.put("strategy", strategy);
                        
                        resultData.put("data", locationData);
                        resultData.put("message", "Backup locations configured successfully");
                        
                        System.out.println(toJson(resultData));
                    } else {
                        System.out.println();
                        System.out.println("Backup locations configured successfully!");
                        if (locations.size() > 1) {
                            System.out.println("Backups will be stored at multiple locations for redundancy.");
                        }
                    }
                    
                    // Track operation success
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("success", true);
                    resultData.put("locations", locations);
                    resultData.put("strategy", strategy);
                    metadataService.completeOperation(locationsOperationId, resultData);
                    
                    return 0;
                } else {
                    String errorMessage = "Failed to configure backup locations.";
                    outputError(errorMessage);
                    
                    // Track operation failure
                    metadataService.failOperation(locationsOperationId, new RuntimeException(errorMessage));
                    
                    return 1;
                }
            } else if ("list".equals(locationsOperation)) {
                String list = backupService.listBackupLocations();
                
                if ("json".equalsIgnoreCase(format)) {
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("result", "success");
                    resultData.put("operation", "locations");
                    resultData.put("suboperation", "list");
                    resultData.put("locations", list);
                    
                    System.out.println(toJson(resultData));
                } else {
                    System.out.println(list);
                }
                
                // Track operation success
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                metadataService.completeOperation(locationsOperationId, resultData);
                
                return 0;
            } else {
                String errorMessage = "Unknown locations operation: " + locationsOperation;
                outputError(errorMessage);
                System.out.println("Valid operations: configure, list");
                
                // Track operation failure
                metadataService.failOperation(locationsOperationId, 
                    new IllegalArgumentException(errorMessage));
                
                return 1;
            }
        } catch (Exception e) {
            String errorMessage = "Error in locations operation: " + e.getMessage();
            outputError(errorMessage);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            // Track operation failure
            metadataService.failOperation(locationsOperationId, e);
            
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
     * Displays help information for backup commands.
     * 
     * @param operationId the parent operation ID for tracking
     */
    private void displayHelp(String operationId) {
        // Create operation parameters for tracking
        Map<String, Object> helpData = new HashMap<>();
        helpData.put("command", "admin backup");
        helpData.put("action", "help");
        helpData.put("format", format);
        
        // Start tracking sub-operation
        String helpOperationId = metadataService.trackOperation("admin-backup-help", helpData);
        
        try {
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> jsonHelpData = new HashMap<>();
                jsonHelpData.put("result", "success");
                jsonHelpData.put("command", "admin backup");
                jsonHelpData.put("usage", "rin admin backup <operation> [options]");
                
                List<Map<String, String>> operations = new ArrayList<>();
                operations.add(createInfoMap("configure", "Configure backup settings"));
                operations.add(createInfoMap("status", "Show backup configuration status"));
                operations.add(createInfoMap("start", "Manually initiate a backup"));
                operations.add(createInfoMap("list", "List available backups"));
                operations.add(createInfoMap("strategy", "Manage backup strategies"));
                operations.add(createInfoMap("history", "View backup history"));
                operations.add(createInfoMap("security", "Configure backup encryption"));
                operations.add(createInfoMap("verify", "Test backup integrity"));
                operations.add(createInfoMap("notifications", "Configure backup notifications"));
                operations.add(createInfoMap("locations", "Manage backup storage locations"));
                jsonHelpData.put("operations", operations);
                
                Map<String, List<Map<String, String>>> operationOptions = new HashMap<>();
                
                List<Map<String, String>> startOptions = new ArrayList<>();
                startOptions.add(createInfoMap("--type=<type>", "Backup type (full, incremental, differential)"));
                operationOptions.put("start", startOptions);
                
                List<Map<String, String>> verifyOptions = new ArrayList<>();
                verifyOptions.add(createInfoMap("--backup-id=<id>", "ID of backup to verify (or 'latest')"));
                operationOptions.put("verify", verifyOptions);
                
                jsonHelpData.put("operation_options", operationOptions);
                
                System.out.println(OutputFormatter.toJson(jsonHelpData, verbose));
            } else {
                System.out.println("Usage: rin admin backup <operation> [options]");
                System.out.println();
                System.out.println("Operations:");
                System.out.println("  configure     - Configure backup settings");
                System.out.println("  status        - Show backup configuration status");
                System.out.println("  start         - Manually initiate a backup");
                System.out.println("  list          - List available backups");
                System.out.println("  strategy      - Manage backup strategies");
                System.out.println("  history       - View backup history");
                System.out.println("  security      - Configure backup encryption");
                System.out.println("  verify        - Test backup integrity");
                System.out.println("  notifications - Configure backup notifications");
                System.out.println("  locations     - Manage backup storage locations");
                System.out.println();
                System.out.println("Options for 'start':");
                System.out.println("  --type=<type>    - Backup type (full, incremental, differential)");
                System.out.println();
                System.out.println("Options for 'verify':");
                System.out.println("  --backup-id=<id> - ID of backup to verify (or 'latest')");
                System.out.println();
                System.out.println("For detailed help on a specific operation, use:");
                System.out.println("  rin admin backup <operation> help");
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
    
    /**
     * Converts an object to JSON string.
     * 
     * @param obj the object to convert
     * @return JSON string representation
     */
    private String toJson(Object obj) {
        return OutputFormatter.toJson((Map<String, Object>) obj, verbose);
    }
}