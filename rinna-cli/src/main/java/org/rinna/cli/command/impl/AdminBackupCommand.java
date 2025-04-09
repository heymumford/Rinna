/*
 * Administrative backup command handler for Rinna.
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.command.impl;

import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.BackupService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Command handler for backup-related operations.
 * This class implements the functionality for the 'rin admin backup' command.
 */
public class AdminBackupCommand implements Callable<Integer> {
    
    private String operation;
    private String[] args = new String[0];
    private final ServiceManager serviceManager;
    private final Scanner scanner;
    
    /**
     * Creates a new AdminBackupCommand with the specified ServiceManager.
     * 
     * @param serviceManager the service manager
     */
    public AdminBackupCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
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
        // Not implemented yet
    }
    
    /**
     * Sets whether to display verbose output.
     * 
     * @param verbose true for verbose output
     */
    public void setVerbose(boolean verbose) {
        // Not implemented yet
    }
    
    @Override
    public Integer call() {
        if (operation == null || operation.isEmpty()) {
            displayHelp();
            return 1;
        }
        
        // Get the backup service
        BackupService backupService = serviceManager.getBackupService();
        if (backupService == null) {
            System.err.println("Error: Backup service is not available.");
            return 1;
        }
        
        // Delegate to the appropriate operation
        switch (operation) {
            case "configure":
                return handleConfigureOperation(backupService);
            
            case "status":
                return handleStatusOperation(backupService);
            
            case "start":
                return handleStartOperation(backupService);
            
            case "list":
                return handleListOperation(backupService);
            
            case "strategy":
                return handleStrategyOperation(backupService);
            
            case "history":
                return handleHistoryOperation(backupService);
            
            case "security":
                return handleSecurityOperation(backupService);
            
            case "verify":
                return handleVerifyOperation(backupService);
            
            case "notifications":
                return handleNotificationsOperation(backupService);
            
            case "locations":
                return handleLocationsOperation(backupService);
            
            case "help":
                displayHelp();
                return 0;
            
            default:
                System.err.println("Error: Unknown backup operation: " + operation);
                displayHelp();
                return 1;
        }
    }
    
    /**
     * Handles the 'configure' operation to set up backup settings.
     * 
     * @param backupService the backup service
     * @return the exit code
     */
    private int handleConfigureOperation(BackupService backupService) {
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
                        System.err.println("Error: Invalid backup type. Using default (full).");
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
                    System.err.println("Error: Retention period must be greater than 0. Using default (30).");
                    retention = 30;
                }
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid retention period. Must be a number. Using default (30).");
            }
        }
        
        System.out.print("Enter backup location (directory path) [/var/backups/rinna]: ");
        String location = scanner.nextLine().trim();
        if (location.isEmpty()) {
            location = "/var/backups/rinna"; // Default
        }
        
        try {
            boolean success = backupService.configureBackup(type, frequency, time, retention, location);
            if (success) {
                System.out.println();
                System.out.println("Backup configuration updated successfully!");
                System.out.println("New configuration will take effect immediately.");
                return 0;
            } else {
                System.err.println("Error: Failed to update backup configuration.");
                return 1;
            }
        } catch (Exception e) {
            System.err.println("Error configuring backup: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Handles the 'status' operation to display backup configuration status.
     * 
     * @param backupService the backup service
     * @return the exit code
     */
    private int handleStatusOperation(BackupService backupService) {
        try {
            String status = backupService.getBackupStatus();
            System.out.println(status);
            return 0;
        } catch (Exception e) {
            System.err.println("Error getting backup status: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Handles the 'start' operation to initiate a backup.
     * 
     * @param backupService the backup service
     * @return the exit code
     */
    private int handleStartOperation(BackupService backupService) {
        Map<String, String> options = parseOptions(args);
        String type = options.getOrDefault("type", "full");
        
        if (!Arrays.asList("full", "incremental", "differential").contains(type)) {
            System.err.println("Error: Invalid backup type. Must be 'full', 'incremental', or 'differential'.");
            return 1;
        }
        
        try {
            String backupId = backupService.startBackup(type);
            System.out.println("Backup completed successfully!");
            System.out.println("Backup ID: " + backupId);
            return 0;
        } catch (Exception e) {
            System.err.println("Error starting backup: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Handles the 'list' operation to display available backups.
     * 
     * @param backupService the backup service
     * @return the exit code
     */
    private int handleListOperation(BackupService backupService) {
        try {
            String list = backupService.listBackups();
            System.out.println(list);
            return 0;
        } catch (Exception e) {
            System.err.println("Error listing backups: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Handles the 'strategy' operation to manage backup strategies.
     * 
     * @param backupService the backup service
     * @return the exit code
     */
    private int handleStrategyOperation(BackupService backupService) {
        if (args.length == 0) {
            System.err.println("Error: Missing strategy subcommand. Use 'configure' or 'status'.");
            return 1;
        }
        
        String strategyOperation = args[0];
        
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
                            System.err.println("Error: Invalid backup strategy. Using default (incremental).");
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
                        System.err.println("Error: Invalid frequency. Using default (weekly).");
                    }
                }
                
                String incrementalLabel = "incremental".equals(strategy) ? "incremental" : "differential";
                System.out.print("Select " + incrementalLabel + " backup frequency (daily, bidaily) [daily]: ");
                String incFreqInput = scanner.nextLine().trim();
                if (!incFreqInput.isEmpty()) {
                    if ("daily".equals(incFreqInput) || "bidaily".equals(incFreqInput)) {
                        incrementalFrequency = incFreqInput;
                    } else {
                        System.err.println("Error: Invalid frequency. Using default (daily).");
                    }
                }
            }
            
            try {
                boolean success = backupService.configureBackupStrategy(strategy, fullFrequency, incrementalFrequency);
                if (success) {
                    System.out.println();
                    System.out.println("Backup strategy updated successfully!");
                    System.out.println("New strategy will be used for next scheduled backup.");
                    return 0;
                } else {
                    System.err.println("Error: Failed to update backup strategy.");
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("Error configuring backup strategy: " + e.getMessage());
                return 1;
            }
        } else if ("status".equals(strategyOperation)) {
            try {
                String status = backupService.getBackupStrategyStatus();
                System.out.println(status);
                return 0;
            } catch (Exception e) {
                System.err.println("Error getting backup strategy status: " + e.getMessage());
                return 1;
            }
        } else {
            System.err.println("Error: Unknown strategy operation: " + strategyOperation);
            System.out.println("Valid operations: configure, status");
            return 1;
        }
    }
    
    /**
     * Handles the 'history' operation to display backup history.
     * 
     * @param backupService the backup service
     * @return the exit code
     */
    private int handleHistoryOperation(BackupService backupService) {
        try {
            String history = backupService.getBackupHistory();
            System.out.println(history);
            return 0;
        } catch (Exception e) {
            System.err.println("Error getting backup history: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Handles the 'security' operation to manage backup security.
     * 
     * @param backupService the backup service
     * @return the exit code
     */
    private int handleSecurityOperation(BackupService backupService) {
        if (args.length == 0) {
            System.err.println("Error: Missing security subcommand. Use 'configure' or 'status'.");
            return 1;
        }
        
        String securityOperation = args[0];
        
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
                    System.err.println("Error: Passphrase cannot be empty.");
                    return 1;
                }
                
                System.out.print("Confirm passphrase: ");
                String confirmPassphrase = scanner.nextLine().trim();
                
                if (!passphrase.equals(confirmPassphrase)) {
                    System.err.println("Error: Passphrases do not match.");
                    return 1;
                }
            }
            
            try {
                boolean success = backupService.configureBackupSecurity(enableEncryption, algorithm, passphrase);
                if (success) {
                    System.out.println();
                    System.out.println("Backup security configured successfully!");
                    if (enableEncryption) {
                        System.out.println("All future backups will be encrypted.");
                        System.out.println("IMPORTANT: Store your passphrase securely. If lost, backups cannot be recovered.");
                    } else {
                        System.out.println("Encryption disabled. Backups will be stored unencrypted.");
                    }
                    return 0;
                } else {
                    System.err.println("Error: Failed to configure backup security.");
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("Error configuring backup security: " + e.getMessage());
                return 1;
            }
        } else if ("status".equals(securityOperation)) {
            try {
                String status = backupService.getBackupSecurityStatus();
                System.out.println(status);
                return 0;
            } catch (Exception e) {
                System.err.println("Error getting backup security status: " + e.getMessage());
                return 1;
            }
        } else {
            System.err.println("Error: Unknown security operation: " + securityOperation);
            System.out.println("Valid operations: configure, status");
            return 1;
        }
    }
    
    /**
     * Handles the 'verify' operation to check backup integrity.
     * 
     * @param backupService the backup service
     * @return the exit code
     */
    private int handleVerifyOperation(BackupService backupService) {
        Map<String, String> options = parseOptions(args);
        String backupId = options.getOrDefault("backup-id", "latest");
        
        try {
            String report = backupService.verifyBackup(backupId);
            System.out.println(report);
            return 0;
        } catch (Exception e) {
            System.err.println("Error verifying backup: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Handles the 'notifications' operation to manage backup notifications.
     * 
     * @param backupService the backup service
     * @return the exit code
     */
    private int handleNotificationsOperation(BackupService backupService) {
        if (args.length == 0) {
            System.err.println("Error: Missing notifications subcommand. Use 'configure' or 'status'.");
            return 1;
        }
        
        String notificationOperation = args[0];
        
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
                    System.err.println("Error: Invalid input. Using default (yes).");
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
                    System.err.println("Error: Recipients cannot be empty.");
                    return 1;
                }
            }
            
            try {
                boolean success = backupService.configureBackupNotifications(enableNotifications, events, recipients);
                if (success) {
                    System.out.println();
                    System.out.println("Backup notifications configured successfully!");
                    if (enableNotifications) {
                        System.out.println("Notifications will be sent for future backup operations.");
                    } else {
                        System.out.println("Notifications disabled. No notifications will be sent for backup operations.");
                    }
                    return 0;
                } else {
                    System.err.println("Error: Failed to configure backup notifications.");
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("Error configuring backup notifications: " + e.getMessage());
                return 1;
            }
        } else if ("status".equals(notificationOperation)) {
            try {
                String status = backupService.getBackupNotificationsStatus();
                System.out.println(status);
                return 0;
            } catch (Exception e) {
                System.err.println("Error getting backup notifications status: " + e.getMessage());
                return 1;
            }
        } else {
            System.err.println("Error: Unknown notifications operation: " + notificationOperation);
            System.out.println("Valid operations: configure, status");
            return 1;
        }
    }
    
    /**
     * Handles the 'locations' operation to manage backup storage locations.
     * 
     * @param backupService the backup service
     * @return the exit code
     */
    private int handleLocationsOperation(BackupService backupService) {
        if (args.length == 0) {
            System.err.println("Error: Missing locations subcommand. Use 'configure' or 'list'.");
            return 1;
        }
        
        String locationsOperation = args[0];
        
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
                System.err.println("Error: At least one backup location is required.");
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
                            System.err.println("Error: Invalid strategy. Using default (synchronized).");
                            break;
                    }
                }
            }
            
            try {
                boolean success = backupService.configureBackupLocations(locations, strategy);
                if (success) {
                    System.out.println();
                    System.out.println("Backup locations configured successfully!");
                    if (locations.size() > 1) {
                        System.out.println("Backups will be stored at multiple locations for redundancy.");
                    }
                    return 0;
                } else {
                    System.err.println("Error: Failed to configure backup locations.");
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("Error configuring backup locations: " + e.getMessage());
                return 1;
            }
        } else if ("list".equals(locationsOperation)) {
            try {
                String list = backupService.listBackupLocations();
                System.out.println(list);
                return 0;
            } catch (Exception e) {
                System.err.println("Error listing backup locations: " + e.getMessage());
                return 1;
            }
        } else {
            System.err.println("Error: Unknown locations operation: " + locationsOperation);
            System.out.println("Valid operations: configure, list");
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
     */
    private void displayHelp() {
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
}