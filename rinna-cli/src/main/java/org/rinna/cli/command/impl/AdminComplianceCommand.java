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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Command handler for compliance-related operations.
 * This class implements the functionality for the 'rin admin compliance' command.
 */
public class AdminComplianceCommand implements Callable<Integer> {
    
    private String operation;
    private String[] args = new String[0];
    private final ServiceManager serviceManager;
    private final Scanner scanner;
    
    /**
     * Creates a new AdminComplianceCommand with the specified ServiceManager.
     * 
     * @param serviceManager the service manager
     */
    public AdminComplianceCommand(ServiceManager serviceManager) {
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
        
        // Get the compliance service
        ComplianceService complianceService = serviceManager.getComplianceService();
        if (complianceService == null) {
            System.err.println("Error: Compliance service is not available.");
            return 1;
        }
        
        // Delegate to the appropriate operation
        switch (operation) {
            case "report":
                return handleReportOperation(complianceService);
            
            case "configure":
                return handleConfigureOperation(complianceService);
            
            case "validate":
                return handleValidateOperation(complianceService);
            
            case "status":
                return handleStatusOperation(complianceService);
            
            case "help":
                displayHelp();
                return 0;
            
            default:
                System.err.println("Error: Unknown compliance operation: " + operation);
                displayHelp();
                return 1;
        }
    }
    
    /**
     * Handles the 'report' operation to generate compliance reports.
     * 
     * @param complianceService the compliance service
     * @return the exit code
     */
    private int handleReportOperation(ComplianceService complianceService) {
        Map<String, String> options = parseOptions(args);
        String type = options.getOrDefault("type", "GDPR");
        String period = options.getOrDefault("period", "current");
        
        try {
            String report = complianceService.generateComplianceReport(type, period);
            System.out.println(report);
            return 0;
        } catch (Exception e) {
            System.err.println("Error generating compliance report: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Handles the 'configure' operation to set up project compliance.
     * 
     * @param complianceService the compliance service
     * @return the exit code
     */
    private int handleConfigureOperation(ComplianceService complianceService) {
        Map<String, String> options = parseOptions(args);
        String projectName = options.getOrDefault("project", null);
        
        if (projectName == null) {
            System.err.println("Error: Missing required parameter --project.");
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
        
        System.out.print("Assign a compliance reviewer (username): ");
        String reviewer = scanner.nextLine().trim();
        
        if (reviewer.isEmpty()) {
            System.err.println("Error: Compliance reviewer cannot be empty.");
            return 1;
        }
        
        try {
            boolean success = complianceService.configureProjectCompliance(projectName, frameworks, reviewer);
            if (success) {
                System.out.println();
                System.out.println("Compliance configuration updated successfully for project: " + projectName);
                System.out.println("Frameworks: " + String.join(", ", frameworks));
                System.out.println("Reviewer: " + reviewer);
                return 0;
            } else {
                System.err.println("Error: Failed to update compliance configuration.");
                return 1;
            }
        } catch (Exception e) {
            System.err.println("Error configuring compliance: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Handles the 'validate' operation to check project compliance.
     * 
     * @param complianceService the compliance service
     * @return the exit code
     */
    private int handleValidateOperation(ComplianceService complianceService) {
        Map<String, String> options = parseOptions(args);
        String projectName = options.getOrDefault("project", null);
        
        if (projectName == null) {
            System.err.println("Error: Missing required parameter --project.");
            return 1;
        }
        
        try {
            String validationReport = complianceService.validateProjectCompliance(projectName);
            System.out.println(validationReport);
            return 0;
        } catch (Exception e) {
            System.err.println("Error validating compliance: " + e.getMessage());
            return 1;
        }
    }
    
    /**
     * Handles the 'status' operation to display compliance status.
     * 
     * @param complianceService the compliance service
     * @return the exit code
     */
    private int handleStatusOperation(ComplianceService complianceService) {
        Map<String, String> options = parseOptions(args);
        String projectName = options.getOrDefault("project", null);
        
        try {
            String status;
            if (projectName != null) {
                status = complianceService.getProjectComplianceStatus(projectName);
            } else {
                status = complianceService.getSystemComplianceStatus();
            }
            
            System.out.println(status);
            return 0;
        } catch (Exception e) {
            System.err.println("Error getting compliance status: " + e.getMessage());
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
     */
    private void displayHelp() {
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
}