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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Command handler for system diagnostics operations.
 * This class implements the functionality for the 'rin admin diagnostics' command.
 */
public class AdminDiagnosticsCommand implements Callable<Integer> {
    
    private String operation;
    private String[] args = new String[0];
    private final ServiceManager serviceManager;
    private final Scanner scanner;
    
    /**
     * Creates a new AdminDiagnosticsCommand with the specified ServiceManager.
     * 
     * @param serviceManager the service manager
     */
    public AdminDiagnosticsCommand(ServiceManager serviceManager) {
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
        
        // Get the diagnostics service
        DiagnosticsService diagnosticsService = serviceManager.getDiagnosticsService();
        if (diagnosticsService == null) {
            System.err.println("Error: Diagnostics service is not available.");
            return 1;
        }
        
        // Delegate to the appropriate operation
        switch (operation) {
            case "run":
                return handleRunOperation(diagnosticsService);
            
            case "schedule":
                return handleScheduleOperation(diagnosticsService);
            
            case "database":
                return handleDatabaseOperation(diagnosticsService);
            
            case "warning":
                return handleWarningOperation(diagnosticsService);
            
            case "action":
                return handleActionOperation(diagnosticsService);
            
            case "help":
                displayHelp();
                return 0;
            
            default:
                System.err.println("Error: Unknown diagnostics operation: " + operation);
                displayHelp();
                return 1;
        }
    }
    
    /**
     * Handles the 'run' operation to execute diagnostics.
     * 
     * @param diagnosticsService the diagnostics service
     * @return the exit code
     */
    private int handleRunOperation(DiagnosticsService diagnosticsService) {
        Map<String, String> options = parseOptions(args);
        boolean full = options.containsKey("full");
        
        try {
            String results = diagnosticsService.runDiagnostics(full);
            System.out.println(results);
            return 0;
        } catch (Exception e) {
            System.err.println("Error running diagnostics: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Handles the 'schedule' operation to schedule diagnostic checks.
     * 
     * @param diagnosticsService the diagnostics service
     * @return the exit code
     */
    private int handleScheduleOperation(DiagnosticsService diagnosticsService) {
        if (args.length > 0 && "list".equals(args[0])) {
            try {
                String schedules = diagnosticsService.listScheduledDiagnostics();
                System.out.println(schedules);
                return 0;
            } catch (Exception e) {
                System.err.println("Error listing scheduled diagnostics: " + e.getMessage());
                return 1;
            }
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
            System.err.println("Error: Check types cannot be empty.");
            return 1;
        }
        
        List<String> checks = Arrays.asList(checksInput.split("\\s*,\\s*"));
        
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
        
        String timePrompt = "hourly".equals(frequency) 
            ? "Enter schedule minute (0-59) [0]: " 
            : "Enter schedule time (HH:MM) [02:00]: ";
        
        System.out.print(timePrompt);
        
        String timeInput = scanner.nextLine().trim();
        String time = "hourly".equals(frequency) ? "0" : "02:00"; // Default
        
        if (!timeInput.isEmpty()) {
            time = timeInput;
        }
        
        System.out.print("Enter notification recipients (comma-separated email addresses): ");
        String recipientsInput = scanner.nextLine().trim();
        
        List<String> recipients = Arrays.asList(recipientsInput.split("\\s*,\\s*"));
        
        try {
            String taskId = diagnosticsService.scheduleDiagnostics(checks, frequency, time, recipients);
            System.out.println();
            System.out.println("Diagnostic task scheduled successfully!");
            System.out.println("Task ID: " + taskId);
            return 0;
        } catch (Exception e) {
            System.err.println("Error scheduling diagnostics: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Handles the 'database' operation to analyze database performance.
     * 
     * @param diagnosticsService the diagnostics service
     * @return the exit code
     */
    private int handleDatabaseOperation(DiagnosticsService diagnosticsService) {
        Map<String, String> options = parseOptions(args);
        boolean analyze = options.containsKey("analyze");
        
        if (!analyze) {
            System.err.println("Error: Missing required flag --analyze.");
            return 1;
        }
        
        try {
            String report = diagnosticsService.analyzeDatabasePerformance();
            System.out.println(report);
            return 0;
        } catch (Exception e) {
            System.err.println("Error analyzing database performance: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Handles the 'warning' operation to resolve system warnings.
     * 
     * @param diagnosticsService the diagnostics service
     * @return the exit code
     */
    private int handleWarningOperation(DiagnosticsService diagnosticsService) {
        if (args.length == 0 || !"resolve".equals(args[0])) {
            System.err.println("Error: Missing 'resolve' subcommand.");
            return 1;
        }
        
        Map<String, String> options = parseOptions(args);
        String warningId = options.getOrDefault("id", null);
        
        if (warningId == null) {
            System.err.println("Error: Missing required parameter --id.");
            return 1;
        }
        
        try {
            Map<String, String> warningDetails = diagnosticsService.getWarningDetails(warningId);
            
            if (warningDetails.isEmpty()) {
                System.err.println("Error: Warning not found: " + warningId);
                return 1;
            }
            
            System.out.println("Warning Resolution: " + warningId);
            System.out.println("==============================");
            System.out.println();
            System.out.println("Warning Type: " + warningDetails.getOrDefault("type", "Unknown"));
            System.out.println("Timestamp: " + warningDetails.getOrDefault("timestamp", "Unknown"));
            System.out.println("Severity: " + warningDetails.getOrDefault("severity", "Unknown"));
            System.out.println("Description: " + warningDetails.getOrDefault("description", "Unknown"));
            System.out.println();
            
            List<String> availableActions = diagnosticsService.getAvailableWarningActions(warningId);
            
            if (availableActions.isEmpty()) {
                System.out.println("No available actions for this warning.");
                return 0;
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
                System.err.println("Error: Invalid selection.");
                return 1;
            }
            
            if (actionIndex < 0 || actionIndex >= availableActions.size()) {
                System.err.println("Error: Invalid selection.");
                return 1;
            }
            
            String selectedAction = availableActions.get(actionIndex);
            
            System.out.println();
            System.out.println("Performing action: " + selectedAction);
            System.out.println();
            
            boolean success = diagnosticsService.performWarningAction(warningId, selectedAction);
            
            if (success) {
                System.out.println("Action performed successfully!");
                return 0;
            } else {
                System.err.println("Error: Failed to perform action.");
                return 1;
            }
        } catch (Exception e) {
            System.err.println("Error resolving warning: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Handles the 'action' operation to perform diagnostic actions.
     * 
     * @param diagnosticsService the diagnostics service
     * @return the exit code
     */
    private int handleActionOperation(DiagnosticsService diagnosticsService) {
        Map<String, String> options = parseOptions(args);
        
        if (options.containsKey("memory-reclaim")) {
            try {
                boolean success = diagnosticsService.performMemoryReclamation();
                if (success) {
                    System.out.println("Memory reclamation completed successfully!");
                    return 0;
                } else {
                    System.err.println("Error: Memory reclamation failed.");
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("Error performing memory reclamation: " + e.getMessage());
                return 1;
            }
        } else {
            System.err.println("Error: Missing required action flag.");
            System.out.println("Available actions: --memory-reclaim");
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
     */
    private void displayHelp() {
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
}