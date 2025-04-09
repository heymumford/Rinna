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

import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.MetadataService.OperationMetadata;
import org.rinna.cli.service.MockMetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command for managing operations metadata.
 * 
 * Usage examples:
 * - "rin operations list" - List recent operations
 * - "rin operations view --id=<operation-id>" - View details of an operation
 * - "rin operations stats" - Show operation statistics
 * - "rin operations clear" - Clear old operation history
 * - "rin operations --format=json list" - Get operations list in JSON format
 */
public class OperationsCommand implements Callable<Integer> {
    
    private String action = "list";
    private String commandFilter;
    private String typeFilter;
    private String dateFrom;
    private String dateTo;
    private String operationId;
    private int limit = 10;
    private int days = 30;
    private boolean jsonOutput = false;
    private boolean verbose = false;
    private String format = "text";
    
    private final ServiceManager serviceManager;
    private final org.rinna.cli.service.MetadataService metadataService;
    
    /**
     * Creates a new OperationsCommand with default services.
     */
    public OperationsCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new OperationsCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public OperationsCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.metadataService = serviceManager.getMetadataService();
    }
    
    /**
     * Sets the action to perform.
     *
     * @param action the action to perform (list, stats, view, clear)
     */
    public void setAction(String action) {
        this.action = action;
    }
    
    /**
     * Sets the command filter.
     *
     * @param commandFilter the command name to filter by
     */
    public void setCommandFilter(String commandFilter) {
        this.commandFilter = commandFilter;
    }
    
    /**
     * Sets the operation type filter.
     *
     * @param typeFilter the operation type to filter by
     */
    public void setTypeFilter(String typeFilter) {
        this.typeFilter = typeFilter;
    }
    
    /**
     * Sets the from date for filtering.
     *
     * @param dateFrom the start date in ISO format (yyyy-MM-dd'T'HH:mm:ss)
     */
    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }
    
    /**
     * Sets the to date for filtering.
     *
     * @param dateTo the end date in ISO format (yyyy-MM-dd'T'HH:mm:ss)
     */
    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }
    
    /**
     * Sets the specific operation ID to view.
     *
     * @param operationId the operation ID
     */
    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }
    
    /**
     * Sets the maximum number of items to return.
     *
     * @param limit the maximum number of items
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    /**
     * Sets the number of days to retain when clearing history.
     *
     * @param days the number of days to retain
     */
    public void setDays(int days) {
        this.days = days;
    }
    
    /**
     * Sets the JSON output flag.
     *
     * @param jsonOutput true to output in JSON format, false for text
     */
    public void setJsonOutput(boolean jsonOutput) {
        this.jsonOutput = jsonOutput;
    }
    
    /**
     * Sets the verbose output flag.
     *
     * @param verbose true for verbose output, false for normal output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    /**
     * Sets the output format (text or json).
     *
     * @param format the output format
     */
    public void setFormat(String format) {
        this.format = format;
        // For backward compatibility
        if ("json".equalsIgnoreCase(format)) {
            this.jsonOutput = true;
        }
    }
    
    @Override
    public Integer call() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("action", action);
        if (commandFilter != null) params.put("command_filter", commandFilter);
        if (typeFilter != null) params.put("type_filter", typeFilter);
        if (dateFrom != null) params.put("date_from", dateFrom);
        if (dateTo != null) params.put("date_to", dateTo);
        if (operationId != null) params.put("operation_id", operationId);
        params.put("limit", limit);
        params.put("days", days);
        params.put("format", format);
        params.put("verbose", verbose);
        
        // Start tracking the operation
        String operationId = metadataService.startOperation("operations", "ADMIN", params);
        
        try {
            // Use the already initialized metadata service
            switch (action.toLowerCase()) {
                case "list":
                    return listOperations(operationId);
                case "view":
                    return viewOperation(operationId);
                case "stats":
                    return showStatistics(operationId);
                case "clear":
                    return clearHistory(operationId);
                case "help":
                    return showHelp(operationId);
                default:
                    String errorMessage = "Unknown action: " + action;
                    if (jsonOutput) {
                        Map<String, Object> errorData = new HashMap<>();
                        errorData.put("result", "error");
                        errorData.put("message", errorMessage);
                        System.out.println(OutputFormatter.toJson(errorData, verbose));
                    } else {
                        System.err.println("Error: " + errorMessage);
                        System.err.println("Valid actions are: list, view, stats, clear, help");
                    }
                    
                    metadataService.failOperation(operationId, new IllegalArgumentException(errorMessage));
                    return 1;
            }
        } catch (Exception e) {
            // Enhanced error handling with context
            String errorMessage = "Error executing operations command: " + e.getMessage();
            
            if (jsonOutput) {
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
            
            // Record the failed operation with error details
            metadataService.failOperation(operationId, e);
            
            return 1;
        }
    }
    
    /**
     * Lists operations based on filters.
     *
     * @param trackingId the operation tracking ID
     * @return exit code
     */
    private int listOperations(String trackingId) {
        List<OperationMetadata> operations = metadataService.listOperations(commandFilter, typeFilter, limit);
        
        if (jsonOutput) {
            // Create JSON data structure
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("command", "operations");
            jsonData.put("action", "list");
            jsonData.put("count", operations.size());
            
            // Add filter information
            Map<String, Object> filters = new HashMap<>();
            if (commandFilter != null) filters.put("command", commandFilter);
            if (typeFilter != null) filters.put("type", typeFilter);
            filters.put("limit", limit);
            jsonData.put("filters", filters);
            
            // Add operations list
            List<Map<String, Object>> operationsList = new ArrayList<>();
            for (OperationMetadata op : operations) {
                Map<String, Object> opMap = new HashMap<>();
                opMap.put("id", op.getId());
                opMap.put("commandName", op.getCommandName());
                opMap.put("operationType", op.getOperationType());
                opMap.put("status", op.getStatus());
                opMap.put("startTime", op.getStartTime().toString());
                
                if (op.getEndTime() != null) {
                    opMap.put("endTime", op.getEndTime().toString());
                }
                
                operationsList.add(opMap);
            }
            jsonData.put("operations", operationsList);
            
            // Use the OutputFormatter for consistent JSON output
            String json = OutputFormatter.toJson(jsonData, verbose);
            System.out.println(json);
        } else {
            // Text output
            System.out.println("Operations List");
            System.out.println("===============");
            System.out.println();
            
            if (operations.isEmpty()) {
                System.out.println("No operations found matching the criteria.");
                
                // Record the successful operation
                Map<String, Object> result = new HashMap<>();
                result.put("action", "list");
                result.put("count", 0);
                result.put("format", format);
                metadataService.completeOperation(trackingId, result);
                
                return 0;
            }
            
            System.out.printf("%-10s %-15s %-10s %-12s %-24s%n", "ID", "Command", "Type", "Status", "Start Time");
            System.out.println(String.join("", "-".repeat(10), " ", "-".repeat(15), " ", "-".repeat(10), " ", "-".repeat(12), " ", "-".repeat(24)));
            
            for (OperationMetadata op : operations) {
                String shortId = op.getId().substring(0, 8); // Truncate for display
                System.out.printf("%-10s %-15s %-10s %-12s %-24s%n",
                    shortId, op.getCommandName(), op.getOperationType(),
                    op.getStatus(), op.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            
            if (verbose) {
                System.out.println("\nUse 'operations view --id=<operation-id>' to see details of a specific operation.");
                System.out.println("Use 'operations stats' to see operation statistics.");
            }
        }
        
        // Record the successful operation
        Map<String, Object> result = new HashMap<>();
        result.put("action", "list");
        result.put("count", operations.size());
        result.put("format", format);
        if (commandFilter != null) result.put("command_filter", commandFilter);
        if (typeFilter != null) result.put("type_filter", typeFilter);
        result.put("limit", limit);
        metadataService.completeOperation(trackingId, result);
        
        return 0;
    }
    
    /**
     * Views details of a specific operation.
     *
     * @param trackingId the operation tracking ID
     * @return exit code
     */
    private int viewOperation(String trackingId) {
        if (operationId == null || operationId.isEmpty()) {
            String errorMessage = "Operation ID is required";
            
            if (jsonOutput) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("result", "error");
                errorData.put("message", errorMessage);
                System.out.println(OutputFormatter.toJson(errorData, verbose));
            } else {
                System.err.println("Error: " + errorMessage);
                System.err.println("Use --id=<operation-id> to specify the operation ID");
            }
            
            metadataService.failOperation(trackingId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        OperationMetadata metadata = metadataService.getOperationMetadata(operationId);
        
        if (metadata == null) {
            String errorMessage = "Operation not found: " + operationId;
            
            if (jsonOutput) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("result", "error");
                errorData.put("message", errorMessage);
                System.out.println(OutputFormatter.toJson(errorData, verbose));
            } else {
                System.err.println("Error: " + errorMessage);
            }
            
            metadataService.failOperation(trackingId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        if (jsonOutput) {
            // Create JSON data structure
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("command", "operations");
            jsonData.put("action", "view");
            jsonData.put("id", metadata.getId());
            
            // Add metadata information
            Map<String, Object> metadataMap = new HashMap<>();
            metadataMap.put("id", metadata.getId());
            metadataMap.put("commandName", metadata.getCommandName());
            metadataMap.put("operationType", metadata.getOperationType());
            metadataMap.put("status", metadata.getStatus());
            metadataMap.put("startTime", metadata.getStartTime().toString());
            
            if (metadata.getEndTime() != null) {
                metadataMap.put("endTime", metadata.getEndTime().toString());
                long durationMs = java.time.temporal.ChronoUnit.MILLIS.between(metadata.getStartTime(), metadata.getEndTime());
                metadataMap.put("durationMs", durationMs);
            }
            
            metadataMap.put("username", metadata.getUsername());
            metadataMap.put("clientInfo", metadata.getClientInfo());
            
            if (metadata.getParameters() != null) {
                metadataMap.put("parameters", metadata.getParameters());
            }
            
            if ("COMPLETED".equals(metadata.getStatus()) && metadata.getResult() != null) {
                metadataMap.put("result", metadata.getResult());
            } else if ("FAILED".equals(metadata.getStatus()) && metadata.getErrorMessage() != null) {
                metadataMap.put("error", metadata.getErrorMessage());
            }
            
            jsonData.put("metadata", metadataMap);
            
            // Use the OutputFormatter for consistent JSON output
            String json = OutputFormatter.toJson(jsonData, verbose);
            System.out.println(json);
        } else {
            // Text output
            System.out.println("Operation Details");
            System.out.println("=================");
            System.out.println();
            
            System.out.println("ID:           " + metadata.getId());
            System.out.println("Command:      " + metadata.getCommandName());
            System.out.println("Type:         " + metadata.getOperationType());
            System.out.println("Status:       " + metadata.getStatus());
            System.out.println("Start Time:   " + metadata.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            if (metadata.getEndTime() != null) {
                System.out.println("End Time:     " + metadata.getEndTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                long durationMs = java.time.temporal.ChronoUnit.MILLIS.between(metadata.getStartTime(), metadata.getEndTime());
                System.out.println("Duration:     " + durationMs + " ms");
            }
            
            System.out.println("User:         " + metadata.getUsername());
            System.out.println("Client:       " + metadata.getClientInfo());
            
            System.out.println("\nParameters:");
            if (metadata.getParameters() != null && !metadata.getParameters().isEmpty()) {
                for (Map.Entry<String, Object> entry : metadata.getParameters().entrySet()) {
                    String value = entry.getValue() != null ? entry.getValue().toString() : "null";
                    System.out.println("  " + entry.getKey() + ": " + value);
                }
            } else {
                System.out.println("  None");
            }
            
            if ("COMPLETED".equals(metadata.getStatus()) && metadata.getResult() != null) {
                System.out.println("\nResult: " + metadata.getResult());
            } else if ("FAILED".equals(metadata.getStatus()) && metadata.getErrorMessage() != null) {
                System.out.println("\nError: " + metadata.getErrorMessage());
            }
        }
        
        // Record the successful operation
        Map<String, Object> result = new HashMap<>();
        result.put("action", "view");
        result.put("operation_id", operationId);
        result.put("format", format);
        metadataService.completeOperation(trackingId, result);
        
        return 0;
    }
    
    /**
     * Shows statistics about operations.
     *
     * @param trackingId the operation tracking ID
     * @return exit code
     */
    private int showStatistics(String trackingId) {
        // Parse date filters if provided
        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;
        
        try {
            if (dateFrom != null && !dateFrom.isEmpty()) {
                fromDate = LocalDateTime.parse(dateFrom, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
            
            if (dateTo != null && !dateTo.isEmpty()) {
                toDate = LocalDateTime.parse(dateTo, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            }
        } catch (DateTimeParseException e) {
            String errorMessage = "Invalid date format. Use ISO format (yyyy-MM-dd'T'HH:mm:ss)";
            
            if (jsonOutput) {
                Map<String, Object> errorData = new HashMap<>();
                errorData.put("result", "error");
                errorData.put("message", errorMessage);
                System.out.println(OutputFormatter.toJson(errorData, verbose));
            } else {
                System.err.println("Error: " + errorMessage);
            }
            
            metadataService.failOperation(trackingId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        Map<String, Object> statistics = metadataService.getOperationStatistics(commandFilter, fromDate, toDate);
        
        if (jsonOutput) {
            // Create JSON data structure for statistics
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("command", "operations");
            jsonData.put("action", "stats");
            
            // Add filter information
            Map<String, Object> filters = new HashMap<>();
            if (commandFilter != null) filters.put("command", commandFilter);
            if (fromDate != null) filters.put("fromDate", fromDate.toString());
            if (toDate != null) filters.put("toDate", toDate.toString());
            jsonData.put("filters", filters);
            
            // Add all statistics
            jsonData.put("statistics", statistics);
            
            // Use the OutputFormatter for consistent JSON output
            String json = OutputFormatter.toJson(jsonData, verbose);
            System.out.println(json);
        } else {
            // Text output
            System.out.println("Operation Statistics");
            System.out.println("===================");
            System.out.println();
            
            if (commandFilter != null) {
                System.out.println("Filtered by command: " + commandFilter);
            }
            
            if (fromDate != null) {
                System.out.println("From: " + fromDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            
            if (toDate != null) {
                System.out.println("To: " + toDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            }
            
            System.out.println();
            
            // Basic statistics
            System.out.println("Total operations:     " + statistics.get("totalOperations"));
            System.out.println("Completed operations: " + statistics.get("completedOperations"));
            System.out.println("Failed operations:    " + statistics.get("failedOperations"));
            
            if (statistics.containsKey("successRate")) {
                double successRate = (double) statistics.get("successRate");
                System.out.printf("Success rate:         %.1f%%%n", successRate);
            }
            
            if (statistics.containsKey("averageDurationMs")) {
                double avgDuration = (double) statistics.get("averageDurationMs");
                System.out.printf("Average duration:     %.2f ms%n", avgDuration);
            }
            
            // Operation counts by type
            @SuppressWarnings("unchecked")
            Map<String, Long> operationsByType = (Map<String, Long>) statistics.get("operationsByType");
            if (operationsByType != null && !operationsByType.isEmpty()) {
                System.out.println("\nOperations by type:");
                for (Map.Entry<String, Long> entry : operationsByType.entrySet()) {
                    System.out.printf("  %-10s: %d%n", entry.getKey(), entry.getValue());
                }
            }
            
            // Operation counts by command
            @SuppressWarnings("unchecked")
            Map<String, Long> operationsByCommand = (Map<String, Long>) statistics.get("operationsByCommand");
            if (operationsByCommand != null && !operationsByCommand.isEmpty()) {
                System.out.println("\nOperations by command:");
                for (Map.Entry<String, Long> entry : operationsByCommand.entrySet()) {
                    System.out.printf("  %-15s: %d%n", entry.getKey(), entry.getValue());
                }
            }
            
            if (verbose) {
                System.out.println("\nUse 'operations list' to see individual operations.");
                System.out.println("Use 'operations stats --from=<date> --to=<date>' to filter by date range.");
                System.out.println("Use 'operations stats --command=<name>' to filter by command name.");
            }
        }
        
        // Record the successful operation
        Map<String, Object> result = new HashMap<>();
        result.put("action", "stats");
        result.put("format", format);
        if (commandFilter != null) result.put("command_filter", commandFilter);
        if (fromDate != null) result.put("from_date", fromDate.toString());
        if (toDate != null) result.put("to_date", toDate.toString());
        result.put("total_operations", statistics.get("totalOperations"));
        metadataService.completeOperation(trackingId, result);
        
        return 0;
    }
    
    /**
     * Clears operation history.
     *
     * @param trackingId the operation tracking ID
     * @return exit code
     */
    private int clearHistory(String trackingId) {
        int clearedCount = metadataService.clearOperationHistory(days);
        
        if (jsonOutput) {
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("command", "operations");
            jsonData.put("action", "clear");
            jsonData.put("result", "success");
            jsonData.put("clearedCount", clearedCount);
            jsonData.put("retentionDays", days);
            
            // Use the OutputFormatter for consistent JSON output
            String json = OutputFormatter.toJson(jsonData, verbose);
            System.out.println(json);
        } else {
            System.out.println("Cleared " + clearedCount + " operations older than " + days + " days.");
            
            if (verbose) {
                System.out.println("\nUse 'operations clear --days=<days>' to change the retention period.");
                System.out.println("Use 'operations list' to see remaining operations.");
            }
        }
        
        // Record the successful operation
        Map<String, Object> result = new HashMap<>();
        result.put("action", "clear");
        result.put("format", format);
        result.put("days", days);
        result.put("cleared_count", clearedCount);
        metadataService.completeOperation(trackingId, result);
        
        return 0;
    }
    
    /**
     * Shows help information.
     *
     * @param trackingId the operation tracking ID
     * @return exit code
     */
    private int showHelp(String trackingId) {
        if (jsonOutput) {
            Map<String, Object> jsonData = new HashMap<>();
            jsonData.put("command", "operations");
            jsonData.put("description", "Manage operation metadata");
            
            // Add actions information
            List<Map<String, Object>> actions = new ArrayList<>();
            
            // List action
            Map<String, Object> listAction = new HashMap<>();
            listAction.put("name", "list");
            listAction.put("description", "List operations");
            List<Map<String, String>> listOptions = new ArrayList<>();
            listOptions.add(createOption("--command", "Filter by command name"));
            listOptions.add(createOption("--type", "Filter by operation type"));
            listOptions.add(createOption("--limit", "Maximum number of results (default: 10)"));
            listAction.put("options", listOptions);
            actions.add(listAction);
            
            // View action
            Map<String, Object> viewAction = new HashMap<>();
            viewAction.put("name", "view");
            viewAction.put("description", "View operation details");
            List<Map<String, String>> viewOptions = new ArrayList<>();
            viewOptions.add(createOption("--id", "Operation ID (required)"));
            viewAction.put("options", viewOptions);
            actions.add(viewAction);
            
            // Stats action
            Map<String, Object> statsAction = new HashMap<>();
            statsAction.put("name", "stats");
            statsAction.put("description", "Show operation statistics");
            List<Map<String, String>> statsOptions = new ArrayList<>();
            statsOptions.add(createOption("--command", "Filter by command name"));
            statsOptions.add(createOption("--from", "Start date (ISO format)"));
            statsOptions.add(createOption("--to", "End date (ISO format)"));
            statsAction.put("options", statsOptions);
            actions.add(statsAction);
            
            // Clear action
            Map<String, Object> clearAction = new HashMap<>();
            clearAction.put("name", "clear");
            clearAction.put("description", "Clear operation history");
            List<Map<String, String>> clearOptions = new ArrayList<>();
            clearOptions.add(createOption("--days", "Days to retain (default: 30)"));
            clearAction.put("options", clearOptions);
            actions.add(clearAction);
            
            jsonData.put("actions", actions);
            
            // Add global options
            List<Map<String, String>> globalOptions = new ArrayList<>();
            globalOptions.add(createOption("--format", "Output format (text or json)"));
            globalOptions.add(createOption("--verbose", "Show verbose output"));
            jsonData.put("globalOptions", globalOptions);
            
            // Use the OutputFormatter for consistent JSON output
            String json = OutputFormatter.toJson(jsonData, verbose);
            System.out.println(json);
        } else {
            System.out.println("Operations Command");
            System.out.println("=================");
            System.out.println();
            System.out.println("Manage operation metadata and tracking.");
            System.out.println();
            
            System.out.println("Actions:");
            System.out.println("  list    List operations");
            System.out.println("  view    View operation details");
            System.out.println("  stats   Show operation statistics");
            System.out.println("  clear   Clear operation history");
            System.out.println("  help    Show this help message");
            System.out.println();
            
            System.out.println("General Options:");
            System.out.println("  --json                Output in JSON format");
            System.out.println("  --verbose             Show verbose output");
            System.out.println();
            
            System.out.println("List Options:");
            System.out.println("  --command=<name>      Filter by command name");
            System.out.println("  --type=<type>         Filter by operation type (CREATE, READ, UPDATE, DELETE)");
            System.out.println("  --limit=<count>       Maximum number of results (default: 10)");
            System.out.println();
            
            System.out.println("View Options:");
            System.out.println("  --id=<operation-id>   Operation ID (required)");
            System.out.println();
            
            System.out.println("Stats Options:");
            System.out.println("  --command=<name>      Filter by command name");
            System.out.println("  --from=<date>         Start date (ISO format: yyyy-MM-dd'T'HH:mm:ss)");
            System.out.println("  --to=<date>           End date (ISO format: yyyy-MM-dd'T'HH:mm:ss)");
            System.out.println();
            
            System.out.println("Clear Options:");
            System.out.println("  --days=<days>         Days to retain (default: 30)");
            System.out.println();
            
            System.out.println("Examples:");
            System.out.println("  operations list --limit=20");
            System.out.println("  operations view --id=12345678-1234-1234-1234-123456789abc");
            System.out.println("  operations stats --command=add --from=2025-01-01T00:00:00");
            System.out.println("  operations clear --days=7");
        }
        
        // Record the successful operation
        Map<String, Object> result = new HashMap<>();
        result.put("action", "help");
        result.put("format", format);
        metadataService.completeOperation(trackingId, result);
        
        return 0;
    }
    
    /**
     * Helper method to create an option map for JSON output.
     *
     * @param name the option name
     * @param description the option description
     * @return a map with name and description
     */
    private Map<String, String> createOption(String name, String description) {
        Map<String, String> option = new HashMap<>();
        option.put("name", name);
        option.put("description", description);
        return option;
    }
}