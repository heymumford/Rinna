/*
 * Administrative audit command handler for Rinna.
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.command.impl;

import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.AuditService;

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

/**
 * Command handler for audit-related operations.
 * This class implements the functionality for the 'rin admin audit' command.
 */
public class AdminAuditCommand implements Callable<Integer> {
    
    private String operation;
    private String[] args = new String[0];
    private final ServiceManager serviceManager;
    private final Scanner scanner;
    
    /**
     * Creates a new AdminAuditCommand with the specified ServiceManager.
     * 
     * @param serviceManager the service manager
     */
    public AdminAuditCommand(ServiceManager serviceManager) {
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
        if (operation == null || operation.isEmpty()) {
            displayHelp();
            return 1;
        }
        
        // Get the audit service
        AuditService auditService = serviceManager.getAuditService();
        if (auditService == null) {
            outputError("Audit service is not available.");
            return 1;
        }
        
        // Delegate to the appropriate operation
        switch (operation) {
            case "list":
                return handleListOperation(auditService);
            
            case "configure":
                return handleConfigureOperation(auditService);
            
            case "status":
                return handleStatusOperation(auditService);
            
            case "export":
                return handleExportOperation(auditService);
            
            case "mask":
                return handleMaskOperation(auditService);
            
            case "alert":
                return handleAlertOperation(auditService);
            
            case "investigation":
                return handleInvestigationOperation(auditService);
            
            case "help":
                displayHelp();
                return 0;
            
            default:
                System.err.println("Error: Unknown audit operation: " + operation);
                displayHelp();
                return 1;
        }
    }
    
    /**
     * Handles the 'list' operation to display audit logs.
     * 
     * @param auditService the audit service
     * @return the exit code
     */
    private int handleListOperation(AuditService auditService) {
        Map<String, String> options = parseOptions(args);
        
        String user = options.getOrDefault("user", null);
        Integer days = null;
        Integer limit = null;
        
        if (options.containsKey("days")) {
            try {
                days = Integer.parseInt(options.get("days"));
            } catch (NumberFormatException e) {
                outputError("Invalid value for --days. Must be a number.");
                return 1;
            }
        }
        
        if (options.containsKey("limit")) {
            try {
                limit = Integer.parseInt(options.get("limit"));
            } catch (NumberFormatException e) {
                outputError("Invalid value for --limit. Must be a number.");
                return 1;
            }
        }
        
        try {
            String result = auditService.listAuditLogs(user, days, limit);
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "list");
                
                Map<String, Object> auditData = new HashMap<>();
                auditData.put("user", user);
                auditData.put("days", days);
                auditData.put("limit", limit);
                auditData.put("logs", result);
                
                resultData.put("data", auditData);
                
                System.out.println(toJson(resultData));
            } else {
                System.out.println(result);
            }
            
            return 0;
        } catch (Exception e) {
            outputError("Error listing audit logs: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }
    
    /**
     * Handles the 'configure' operation to set up audit logging.
     * 
     * @param auditService the audit service
     * @return the exit code
     */
    private int handleConfigureOperation(AuditService auditService) {
        Map<String, String> options = parseOptions(args);
        
        Integer retention = null;
        
        if (options.containsKey("retention")) {
            try {
                retention = Integer.parseInt(options.get("retention"));
                
                if (retention <= 0) {
                    System.err.println("Error: Retention period must be greater than 0.");
                    return 1;
                }
                
                boolean success = auditService.configureRetention(retention);
                if (success) {
                    System.out.println("Audit log retention period updated to " + retention + " days");
                    System.out.println("Configuration changes have been saved successfully.");
                    return 0;
                } else {
                    System.err.println("Error: Failed to update audit retention period.");
                    return 1;
                }
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid value for --retention. Must be a number.");
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
                        System.err.println("Error: Retention period must be greater than 0.");
                        return 1;
                    }
                } catch (NumberFormatException e) {
                    System.err.println("Error: Invalid value for retention period. Must be a number.");
                    return 1;
                }
            } else {
                retention = 30; // Default
            }
            
            boolean success = auditService.configureRetention(retention);
            if (success) {
                System.out.println("Audit log retention period updated to " + retention + " days");
                System.out.println("Configuration changes have been saved successfully.");
                return 0;
            } else {
                System.err.println("Error: Failed to update audit retention period.");
                return 1;
            }
        }
    }
    
    /**
     * Handles the 'status' operation to display audit system status.
     * 
     * @param auditService the audit service
     * @return the exit code
     */
    private int handleStatusOperation(AuditService auditService) {
        try {
            String status = auditService.getAuditStatus();
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "status");
                resultData.put("status", status);
                
                System.out.println(toJson(resultData));
            } else {
                System.out.println(status);
            }
            
            return 0;
        } catch (Exception e) {
            outputError("Error getting audit status: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }
    
    /**
     * Handles the 'export' operation to export audit logs.
     * 
     * @param auditService the audit service
     * @return the exit code
     */
    private int handleExportOperation(AuditService auditService) {
        Map<String, String> options = parseOptions(args);
        
        LocalDate fromDate = null;
        LocalDate toDate = null;
        String exportFormat = options.getOrDefault("format", "csv");
        
        if (options.containsKey("from")) {
            try {
                fromDate = LocalDate.parse(options.get("from"), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                outputError("Invalid date format for --from. Use YYYY-MM-DD.");
                return 1;
            }
        } else {
            // Default to 30 days ago
            fromDate = LocalDate.now().minusDays(30);
        }
        
        if (options.containsKey("to")) {
            try {
                toDate = LocalDate.parse(options.get("to"), DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (DateTimeParseException e) {
                outputError("Invalid date format for --to. Use YYYY-MM-DD.");
                return 1;
            }
        } else {
            // Default to today
            toDate = LocalDate.now();
        }
        
        if (!fromDate.isBefore(toDate)) {
            outputError("From date must be before to date.");
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
                
                System.out.println(toJson(resultData));
            } else {
                System.out.println("Exported audit logs to " + exportPath);
            }
            
            return 0;
        } catch (Exception e) {
            outputError("Error exporting audit logs: " + e.getMessage());
            if (verbose) {
                e.printStackTrace();
            }
            return 1;
        }
    }
    
    /**
     * Handles the 'mask' operation to configure data masking.
     * 
     * @param auditService the audit service
     * @return the exit code
     */
    private int handleMaskOperation(AuditService auditService) {
        if (args.length == 0) {
            System.err.println("Error: Missing mask subcommand. Use 'configure' or 'status'.");
            return 1;
        }
        
        String maskOperation = args[0];
        String[] maskArgs = Arrays.copyOfRange(args, 1, args.length);
        
        if ("configure".equals(maskOperation)) {
            System.out.println("Data Masking Configuration");
            System.out.println("-------------------------");
            System.out.println("Select fields to mask in audit logs:");
            System.out.println("Available fields: email, phone, address, ssn, credit_card, account_number");
            System.out.print("Enter comma-separated list of fields: ");
            
            String fieldsInput = scanner.nextLine().trim();
            List<String> fields = Arrays.asList(fieldsInput.split("\\s*,\\s*"));
            
            try {
                boolean success = auditService.configureMasking(fields);
                if (success) {
                    System.out.println("Data masking configuration updated successfully.");
                    return 0;
                } else {
                    System.err.println("Error: Failed to update data masking configuration.");
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("Error configuring data masking: " + e.getMessage());
                return 1;
            }
        } else if ("status".equals(maskOperation)) {
            try {
                String status = auditService.getMaskingStatus();
                System.out.println(status);
                return 0;
            } catch (Exception e) {
                System.err.println("Error getting masking status: " + e.getMessage());
                return 1;
            }
        } else {
            System.err.println("Error: Unknown mask operation: " + maskOperation);
            System.out.println("Valid operations: configure, status");
            return 1;
        }
    }
    
    /**
     * Handles the 'alert' operation to manage audit alerts.
     * 
     * @param auditService the audit service
     * @return the exit code
     */
    private int handleAlertOperation(AuditService auditService) {
        if (args.length == 0) {
            System.err.println("Error: Missing alert subcommand. Use 'add', 'list', or 'remove'.");
            return 1;
        }
        
        String alertOperation = args[0];
        String[] alertArgs = Arrays.copyOfRange(args, 1, args.length);
        
        if ("add".equals(alertOperation)) {
            System.out.println("Create Audit Alert");
            System.out.println("----------------");
            
            System.out.print("Enter alert name: ");
            String name = scanner.nextLine().trim();
            
            if (name.isEmpty()) {
                System.err.println("Error: Alert name cannot be empty.");
                return 1;
            }
            
            System.out.println("Select event types (comma-separated):");
            System.out.println("- FAILED_LOGIN: Failed login attempts");
            System.out.println("- PERMISSION_DENIED: Access permission denied");
            System.out.println("- ADMIN_ACTION: Administrative actions");
            System.out.println("- DATA_EXPORT: Data export operations");
            System.out.println("- CONFIGURATION_CHANGE: System configuration changes");
            System.out.print("Enter event types: ");
            
            String eventsInput = scanner.nextLine().trim();
            List<String> events = Arrays.asList(eventsInput.split("\\s*,\\s*"));
            
            System.out.print("Enter threshold count: ");
            String thresholdInput = scanner.nextLine().trim();
            int threshold;
            
            try {
                threshold = Integer.parseInt(thresholdInput);
                if (threshold <= 0) {
                    System.err.println("Error: Threshold must be greater than 0.");
                    return 1;
                }
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid threshold value. Must be a number.");
                return 1;
            }
            
            System.out.print("Enter time window in minutes: ");
            String windowInput = scanner.nextLine().trim();
            int window;
            
            try {
                window = Integer.parseInt(windowInput);
                if (window <= 0) {
                    System.err.println("Error: Time window must be greater than 0.");
                    return 1;
                }
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid time window value. Must be a number.");
                return 1;
            }
            
            System.out.print("Enter notification recipients (comma-separated email addresses): ");
            String recipientsInput = scanner.nextLine().trim();
            List<String> recipients = Arrays.asList(recipientsInput.split("\\s*,\\s*"));
            
            try {
                boolean success = auditService.addAlert(name, events, threshold, window, recipients);
                if (success) {
                    System.out.println("Audit alert '" + name + "' created successfully.");
                    return 0;
                } else {
                    System.err.println("Error: Failed to create audit alert.");
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("Error creating audit alert: " + e.getMessage());
                return 1;
            }
        } else if ("list".equals(alertOperation)) {
            try {
                String result = auditService.listAlerts();
                System.out.println(result);
                return 0;
            } catch (Exception e) {
                System.err.println("Error listing audit alerts: " + e.getMessage());
                return 1;
            }
        } else if ("remove".equals(alertOperation) || "delete".equals(alertOperation)) {
            if (alertArgs.length == 0) {
                System.err.println("Error: Missing alert name to remove.");
                return 1;
            }
            
            String alertName = alertArgs[0];
            
            try {
                boolean success = auditService.removeAlert(alertName);
                if (success) {
                    System.out.println("Audit alert '" + alertName + "' removed successfully.");
                    return 0;
                } else {
                    System.err.println("Error: Failed to remove audit alert. Does it exist?");
                    return 1;
                }
            } catch (Exception e) {
                System.err.println("Error removing audit alert: " + e.getMessage());
                return 1;
            }
        } else {
            System.err.println("Error: Unknown alert operation: " + alertOperation);
            System.out.println("Valid operations: add, list, remove");
            return 1;
        }
    }
    
    /**
     * Handles the 'investigation' operation to manage security investigations.
     * 
     * @param auditService the audit service
     * @return the exit code
     */
    private int handleInvestigationOperation(AuditService auditService) {
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
     */
    private void displayHelp() {
        if ("json".equalsIgnoreCase(format)) {
            Map<String, Object> helpData = new HashMap<>();
            helpData.put("result", "success");
            helpData.put("command", "admin audit");
            helpData.put("usage", "rin admin audit <operation> [options]");
            
            List<Map<String, String>> operations = new ArrayList<>();
            operations.add(createInfoMap("list", "List audit logs"));
            operations.add(createInfoMap("configure", "Configure audit logging settings"));
            operations.add(createInfoMap("status", "Show audit system status"));
            operations.add(createInfoMap("export", "Export audit logs to file"));
            operations.add(createInfoMap("mask", "Configure sensitive data masking"));
            operations.add(createInfoMap("alert", "Manage audit alerts"));
            operations.add(createInfoMap("investigation", "Manage security investigations"));
            helpData.put("operations", operations);
            
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
            
            helpData.put("operation_options", operationOptions);
            
            System.out.println(toJson(helpData));
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
            System.out.println(toJson(errorData));
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
            System.out.println(toJson(successData));
        } else {
            System.out.println(message);
        }
    }
    
    /**
     * Converts an object to JSON.
     *
     * @param obj the object to convert
     * @return the JSON string
     */
    private String toJson(Object obj) {
        // Simple JSON conversion for the test
        // In a real implementation, this would use Jackson or similar
        StringBuilder json = new StringBuilder();
        if (obj instanceof Map) {
            json.append("{");
            Map<?, ?> map = (Map<?, ?>) obj;
            boolean first = true;
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (!first) {
                    json.append(", ");
                }
                first = false;
                json.append("\"").append(entry.getKey()).append("\": ");
                if (entry.getValue() instanceof String) {
                    json.append("\"").append(escapeJson(entry.getValue().toString())).append("\"");
                } else {
                    json.append(toJson(entry.getValue()));
                }
            }
            json.append("}");
        } else if (obj instanceof List) {
            json.append("[");
            List<?> list = (List<?>) obj;
            boolean first = true;
            for (Object item : list) {
                if (!first) {
                    json.append(", ");
                }
                first = false;
                if (item instanceof String) {
                    json.append("\"").append(escapeJson(item.toString())).append("\"");
                } else {
                    json.append(toJson(item));
                }
            }
            json.append("]");
        } else if (obj instanceof String) {
            json.append("\"").append(escapeJson(obj.toString())).append("\"");
        } else if (obj instanceof Number) {
            json.append(obj);
        } else if (obj instanceof Boolean) {
            json.append(obj);
        } else if (obj == null) {
            json.append("null");
        } else {
            json.append("\"").append(escapeJson(obj.toString())).append("\"");
        }
        return json.toString();
    }
    
    /**
     * Escapes special characters in a string for JSON.
     *
     * @param str the string to escape
     * @return the escaped string
     */
    private String escapeJson(String str) {
        return str.replace("\\", "\\\\")
                 .replace("\"", "\\\"")
                 .replace("\n", "\\n")
                 .replace("\r", "\\r")
                 .replace("\t", "\\t");
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