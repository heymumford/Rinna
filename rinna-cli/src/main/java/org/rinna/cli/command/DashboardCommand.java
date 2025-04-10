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

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.stream.Collectors;

import org.rinna.cli.messaging.AnsiFormatter;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.stats.StatisticsVisualizer;
import org.rinna.cli.util.OutputFormatter;

/**
 * Command that displays a unified operation analytics dashboard to visualize command usage patterns.
 * 
 * This command provides various views to analyze CLI operation data:
 * - Summary view: Shows key metrics about command usage
 * - Commands view: Shows detailed breakdown of command usage 
 * - Users view: Shows operations by user
 * - Performance view: Shows performance metrics for commands
 * - Timeline view: Shows operation patterns over time
 * 
 * Usage examples:
 * - rin dashboard               # Show summary dashboard
 * - rin dashboard commands      # Show command usage analytics
 * - rin dashboard users         # Show user activity analytics
 * - rin dashboard performance   # Show performance analytics
 * - rin dashboard timeline      # Show timeline view
 * - rin dashboard --days=7      # Show data for the last 7 days
 * - rin dashboard --verbose     # Show detailed output
 */
public class DashboardCommand implements Callable<Integer> {
    
    // Default dashboard view
    private String view = "summary";
    
    // Number of days of data to include
    private int days = 30;
    
    // Maximum number of items to display in detailed breakdowns
    private int limit = 10;
    
    // Custom date range filters
    private LocalDateTime startDate = null;
    private LocalDateTime endDate = null;
    
    // Filter by specific command
    private String commandFilter = null;
    
    // Filter by specific user
    private String userFilter = null;
    
    // Output format options
    private boolean jsonOutput = false;
    private boolean verbose = false;
    private String format = "text";
    
    // Auto-refresh interval in seconds (0 for no auto-refresh)
    private int refreshInterval = 0;
    
    // Services
    private final ServiceManager serviceManager;
    private final MetadataService metadataService;
    
    /**
     * Creates a new DashboardCommand with default services.
     */
    public DashboardCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new DashboardCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public DashboardCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.metadataService = serviceManager.getMetadataService();
    }
    
    /**
     * Sets the dashboard view to display.
     * 
     * @param view the view to display (summary, commands, users, performance, timeline)
     */
    public void setView(String view) {
        if (view != null && !view.isEmpty()) {
            this.view = view.toLowerCase();
        }
    }
    
    /**
     * Sets the number of days of data to include.
     * 
     * @param days the number of days
     */
    public void setDays(int days) {
        if (days > 0) {
            this.days = days;
        }
    }
    
    /**
     * Sets the maximum number of items to display in detailed breakdowns.
     * 
     * @param limit the maximum number of items
     */
    public void setLimit(int limit) {
        if (limit > 0) {
            this.limit = limit;
        }
    }
    
    /**
     * Sets the start date for the dashboard data.
     * 
     * @param startDate the start date in ISO format (yyyy-MM-dd'T'HH:mm:ss)
     */
    public void setStartDate(String startDate) {
        try {
            this.startDate = LocalDateTime.parse(startDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            System.err.println("Warning: Invalid start date format. Using default date range.");
        }
    }
    
    /**
     * Sets the end date for the dashboard data.
     * 
     * @param endDate the end date in ISO format (yyyy-MM-dd'T'HH:mm:ss)
     */
    public void setEndDate(String endDate) {
        try {
            this.endDate = LocalDateTime.parse(endDate, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            System.err.println("Warning: Invalid end date format. Using default date range.");
        }
    }
    
    /**
     * Sets the command filter.
     * 
     * @param commandFilter the command to filter by
     */
    public void setCommandFilter(String commandFilter) {
        this.commandFilter = commandFilter;
    }
    
    /**
     * Sets the user filter.
     * 
     * @param userFilter the user to filter by
     */
    public void setUserFilter(String userFilter) {
        this.userFilter = userFilter;
    }
    
    /**
     * Sets the JSON output flag.
     * 
     * @param jsonOutput true to output in JSON format
     */
    public void setJsonOutput(boolean jsonOutput) {
        this.jsonOutput = jsonOutput;
        if (jsonOutput) {
            this.format = "json";
        }
    }
    
    /**
     * Sets the verbose output flag.
     * 
     * @param verbose true for verbose output
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }
    
    /**
     * Sets the output format.
     * 
     * @param format the output format (text or json)
     */
    public void setFormat(String format) {
        if (format != null && !format.isEmpty()) {
            this.format = format.toLowerCase();
            if ("json".equals(format)) {
                this.jsonOutput = true;
            }
        }
    }
    
    /**
     * Sets the auto-refresh interval in seconds.
     * 
     * @param seconds the refresh interval in seconds
     */
    public void setRefreshInterval(int seconds) {
        if (seconds >= 0) {
            this.refreshInterval = seconds;
        }
    }
    
    @Override
    public Integer call() {
        // Prepare operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("view", view);
        params.put("days", days);
        params.put("limit", limit);
        if (startDate != null) params.put("startDate", startDate.toString());
        if (endDate != null) params.put("endDate", endDate.toString());
        if (commandFilter != null) params.put("commandFilter", commandFilter);
        if (userFilter != null) params.put("userFilter", userFilter);
        params.put("format", format);
        params.put("verbose", verbose);
        params.put("refreshInterval", refreshInterval);
        
        // Start operation tracking
        String operationId = metadataService.startOperation("dashboard", "READ", params);
        
        try {
            // If no specific dates provided, calculate based on days
            if (startDate == null) {
                startDate = LocalDateTime.now().minusDays(days);
            }
            if (endDate == null) {
                endDate = LocalDateTime.now();
            }
            
            // Handle auto-refresh if enabled
            if (refreshInterval > 0) {
                return handleAutoRefresh(operationId);
            }
            
            // One-time display based on view
            switch (view) {
                case "summary":
                    displaySummaryDashboard(operationId);
                    break;
                case "commands":
                    displayCommandsAnalytics(operationId);
                    break;
                case "users":
                    displayUserAnalytics(operationId);
                    break;
                case "performance":
                    displayPerformanceAnalytics(operationId);
                    break;
                case "timeline":
                    displayTimelineAnalytics(operationId);
                    break;
                case "help":
                    displayHelp(operationId);
                    break;
                default:
                    System.err.println("Unknown dashboard view: " + view);
                    System.err.println("Valid views: summary, commands, users, performance, timeline, help");
                    
                    metadataService.failOperation(operationId, 
                        new IllegalArgumentException("Unknown dashboard view: " + view));
                    return 1;
            }
            
            // Record successful operation
            Map<String, Object> result = new HashMap<>();
            result.put("view", view);
            result.put("format", format);
            metadataService.completeOperation(operationId, result);
            
            return 0;
            
        } catch (Exception e) {
            // Handle errors
            String errorMessage = "Error displaying operation dashboard: " + e.getMessage();
            
            if (jsonOutput) {
                System.out.println(OutputFormatter.formatJsonMessage("error", errorMessage, null));
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
     * Handles auto-refresh of the dashboard display.
     *
     * @param operationId the operation tracking ID
     * @return exit code
     */
    private int handleAutoRefresh(String operationId) throws Exception {
        if (jsonOutput) {
            System.err.println("Auto-refresh is not supported in JSON output mode");
            metadataService.failOperation(operationId, 
                new IllegalArgumentException("Auto-refresh not supported in JSON mode"));
            return 1;
        }
        
        // Display the dashboard in a loop with refresh interval
        try {
            while (true) {
                // Clear screen between refreshes
                System.out.print("\033[H\033[2J");
                System.out.flush();
                
                // Display current time
                System.out.println(AnsiFormatter.BOLD + "RINNA OPERATIONS DASHBOARD" + AnsiFormatter.RESET + 
                                  " (Auto-refresh: " + refreshInterval + "s)");
                System.out.println("Last update: " + LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                System.out.println();
                
                // Update date range for each refresh
                if (days > 0) {
                    endDate = LocalDateTime.now();
                    startDate = endDate.minusDays(days);
                }
                
                // Display the requested view
                switch (view) {
                    case "summary":
                        displaySummaryDashboard(operationId);
                        break;
                    case "commands":
                        displayCommandsAnalytics(operationId);
                        break;
                    case "users":
                        displayUserAnalytics(operationId);
                        break;
                    case "performance":
                        displayPerformanceAnalytics(operationId);
                        break;
                    case "timeline":
                        displayTimelineAnalytics(operationId);
                        break;
                    default:
                        displaySummaryDashboard(operationId);
                }
                
                // Display exit instructions
                System.out.println("\nPress Ctrl+C to exit the dashboard");
                
                // Wait for refresh interval
                Thread.sleep(refreshInterval * 1000);
            }
        } catch (InterruptedException e) {
            // Normal exit when user interrupts
            System.out.println("\nDashboard display ended by user.");
            
            // Record successful operation
            Map<String, Object> result = new HashMap<>();
            result.put("view", view);
            result.put("format", format);
            result.put("refreshes", "user-interrupted");
            metadataService.completeOperation(operationId, result);
            
            return 0;
        }
    }
    
    /**
     * Displays a summary dashboard with key operational metrics.
     *
     * @param operationId the operation tracking ID
     */
    private void displaySummaryDashboard(String operationId) {
        // Get operation statistics
        Map<String, Object> statistics = metadataService.getOperationStatistics(commandFilter, startDate, endDate);
        
        // Format as JSON if requested
        if (jsonOutput) {
            displayJsonSummary(statistics);
            return;
        }
        
        // Display text dashboard
        System.out.println(AnsiFormatter.BOLD + "RINNA OPERATIONS DASHBOARD" + AnsiFormatter.RESET);
        System.out.println("==========================\n");
        
        // Date range header
        System.out.println("Date Range: " + startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + 
                          " to " + endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (commandFilter != null) {
            System.out.println("Command Filter: " + commandFilter);
        }
        if (userFilter != null) {
            System.out.println("User Filter: " + userFilter);
        }
        System.out.println();
        
        // Key metrics section
        System.out.println(AnsiFormatter.BOLD + "KEY OPERATIONAL METRICS" + AnsiFormatter.RESET);
        System.out.println("---------------------");
        
        long totalOps = (long) statistics.getOrDefault("totalOperations", 0L);
        long completedOps = (long) statistics.getOrDefault("completedOperations", 0L);
        long failedOps = (long) statistics.getOrDefault("failedOperations", 0L);
        double successRate = (double) statistics.getOrDefault("successRate", 0.0);
        double avgDuration = (double) statistics.getOrDefault("averageDurationMs", 0.0);
        
        System.out.println("Total Operations: " + totalOps);
        System.out.println("Completed Operations: " + completedOps);
        System.out.println("Failed Operations: " + failedOps);
        
        // Success rate with colored progress bar
        System.out.print("Success Rate: ");
        System.out.println(StatisticsVisualizer.createProgressMeter(successRate, 100));
        
        // Average duration
        System.out.printf("Average Duration: %.2f ms\n", avgDuration);
        
        // Top commands section
        System.out.println("\n" + AnsiFormatter.BOLD + "TOP COMMANDS" + AnsiFormatter.RESET);
        System.out.println("------------");
        
        @SuppressWarnings("unchecked")
        Map<String, Long> commandCounts = (Map<String, Long>) statistics.getOrDefault("operationsByCommand", new HashMap<>());
        
        if (commandCounts.isEmpty()) {
            System.out.println("No command usage data available.");
        } else {
            // Convert to a sortable list and limit to top N
            List<Map.Entry<String, Long>> sortedCommands = commandCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limit)
                .collect(Collectors.toList());
            
            // Find max value for scaling
            long maxCount = sortedCommands.isEmpty() ? 1 : sortedCommands.get(0).getValue();
            
            // Display each command with a bar chart
            for (Map.Entry<String, Long> entry : sortedCommands) {
                String command = entry.getKey();
                long count = entry.getValue();
                int barLength = (int) Math.round((count / (double) maxCount) * 40);
                
                System.out.printf("%-15s: ", command);
                System.out.print(AnsiFormatter.BRIGHT_FG_CYAN);
                for (int i = 0; i < barLength; i++) {
                    System.out.print("█");
                }
                System.out.print(AnsiFormatter.RESET);
                System.out.printf(" %d\n", count);
            }
        }
        
        // Operation types section
        System.out.println("\n" + AnsiFormatter.BOLD + "OPERATION TYPES" + AnsiFormatter.RESET);
        System.out.println("---------------");
        
        @SuppressWarnings("unchecked")
        Map<String, Long> typeCounts = (Map<String, Long>) statistics.getOrDefault("operationsByType", new HashMap<>());
        
        if (typeCounts.isEmpty()) {
            System.out.println("No operation type data available.");
        } else {
            // Convert to a sortable list
            List<Map.Entry<String, Long>> sortedTypes = typeCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());
            
            // Find max value for scaling
            long maxCount = sortedTypes.isEmpty() ? 1 : sortedTypes.get(0).getValue();
            
            // Display each type with a bar chart
            for (Map.Entry<String, Long> entry : sortedTypes) {
                String type = entry.getKey();
                long count = entry.getValue();
                int barLength = (int) Math.round((count / (double) maxCount) * 40);
                
                System.out.printf("%-10s: ", type);
                
                // Use different colors for different operation types
                String color;
                switch (type) {
                    case "CREATE":
                        color = AnsiFormatter.BRIGHT_FG_GREEN;
                        break;
                    case "READ":
                        color = AnsiFormatter.BRIGHT_FG_BLUE;
                        break;
                    case "UPDATE":
                        color = AnsiFormatter.BRIGHT_FG_YELLOW;
                        break;
                    case "DELETE":
                        color = AnsiFormatter.BRIGHT_FG_RED;
                        break;
                    case "ADMIN":
                        color = AnsiFormatter.BRIGHT_FG_MAGENTA;
                        break;
                    default:
                        color = AnsiFormatter.BRIGHT_FG_CYAN;
                }
                
                System.out.print(color);
                for (int i = 0; i < barLength; i++) {
                    System.out.print("█");
                }
                System.out.print(AnsiFormatter.RESET);
                System.out.printf(" %d (%.1f%%)\n", count, (count / (double) totalOps) * 100);
            }
        }
        
        // Display helpful information about other views
        if (verbose) {
            System.out.println("\n" + AnsiFormatter.BOLD + "AVAILABLE DASHBOARD VIEWS" + AnsiFormatter.RESET);
            System.out.println("-------------------------");
            System.out.println("Commands View:    rin dashboard commands     (Detailed command usage analysis)");
            System.out.println("Users View:       rin dashboard users        (User activity analysis)");
            System.out.println("Performance View: rin dashboard performance  (Command performance analysis)");
            System.out.println("Timeline View:    rin dashboard timeline     (Operation timeline analysis)");
        }
    }
    
    /**
     * Displays command usage analytics.
     *
     * @param operationId the operation tracking ID
     */
    private void displayCommandsAnalytics(String operationId) {
        // Get operation statistics
        Map<String, Object> statistics = metadataService.getOperationStatistics(commandFilter, startDate, endDate);
        
        // Get operations for detailed analysis
        List<MetadataService.OperationMetadata> operations = metadataService.listOperations(commandFilter, null, 1000);
        
        // Filter operations by date range
        operations = operations.stream()
            .filter(op -> !op.getStartTime().isBefore(startDate))
            .filter(op -> !op.getStartTime().isAfter(endDate))
            .collect(Collectors.toList());
        
        // Format as JSON if requested
        if (jsonOutput) {
            displayJsonCommandsAnalytics(statistics, operations);
            return;
        }
        
        // Display text output
        System.out.println(AnsiFormatter.BOLD + "COMMAND USAGE ANALYTICS" + AnsiFormatter.RESET);
        System.out.println("=======================\n");
        
        // Date range header
        System.out.println("Date Range: " + startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + 
                          " to " + endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (commandFilter != null) {
            System.out.println("Command Filter: " + commandFilter);
        }
        System.out.println();
        
        // Command breakdown section
        System.out.println(AnsiFormatter.BOLD + "COMMAND USAGE BREAKDOWN" + AnsiFormatter.RESET);
        System.out.println("-----------------------");
        
        @SuppressWarnings("unchecked")
        Map<String, Long> commandCounts = (Map<String, Long>) statistics.getOrDefault("operationsByCommand", new HashMap<>());
        
        if (commandCounts.isEmpty()) {
            System.out.println("No command usage data available.");
        } else {
            // Convert to a sortable list and limit to top N
            List<Map.Entry<String, Long>> sortedCommands = commandCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .collect(Collectors.toList());
            
            // Find max value for scaling
            long maxCount = sortedCommands.isEmpty() ? 1 : sortedCommands.get(0).getValue();
            long totalOps = sortedCommands.stream().mapToLong(Map.Entry::getValue).sum();
            
            // Display each command with a bar chart
            for (Map.Entry<String, Long> entry : sortedCommands) {
                String command = entry.getKey();
                long count = entry.getValue();
                double percentage = (count / (double) totalOps) * 100;
                int barLength = (int) Math.round((count / (double) maxCount) * 40);
                
                System.out.printf("%-15s: ", command);
                System.out.print(AnsiFormatter.BRIGHT_FG_CYAN);
                for (int i = 0; i < barLength; i++) {
                    System.out.print("█");
                }
                System.out.print(AnsiFormatter.RESET);
                System.out.printf(" %d (%.1f%%)\n", count, percentage);
            }
        }
        
        // Command success rates
        System.out.println("\n" + AnsiFormatter.BOLD + "COMMAND SUCCESS RATES" + AnsiFormatter.RESET);
        System.out.println("---------------------");
        
        // Calculate success rate for each command
        Map<String, double[]> commandSuccessRates = new HashMap<>();
        for (MetadataService.OperationMetadata op : operations) {
            String command = op.getCommandName();
            boolean success = "COMPLETED".equals(op.getStatus());
            
            if (!commandSuccessRates.containsKey(command)) {
                commandSuccessRates.put(command, new double[2]); // [success count, total count]
            }
            
            double[] counts = commandSuccessRates.get(command);
            if (success) {
                counts[0]++;
            }
            counts[1]++;
        }
        
        // Display success rates with progress meters
        List<Map.Entry<String, double[]>> sortedByUsage = commandSuccessRates.entrySet().stream()
            .sorted((e1, e2) -> Double.compare(e2.getValue()[1], e1.getValue()[1])) // Sort by total count
            .limit(limit)
            .collect(Collectors.toList());
        
        if (sortedByUsage.isEmpty()) {
            System.out.println("No command success rate data available.");
        } else {
            for (Map.Entry<String, double[]> entry : sortedByUsage) {
                String command = entry.getKey();
                double[] counts = entry.getValue();
                double successRate = counts[1] > 0 ? (counts[0] / counts[1]) * 100 : 0;
                
                System.out.printf("%-15s: ", command);
                System.out.println(StatisticsVisualizer.createProgressMeter(successRate, 100));
            }
        }
        
        // Command parameters analysis
        if (verbose) {
            System.out.println("\n" + AnsiFormatter.BOLD + "COMMON COMMAND PARAMETERS" + AnsiFormatter.RESET);
            System.out.println("--------------------------");
            
            // Group operations by command
            Map<String, List<MetadataService.OperationMetadata>> opsByCommand = operations.stream()
                .collect(Collectors.groupingBy(MetadataService.OperationMetadata::getCommandName));
            
            // Analyze parameter usage for top commands
            for (String command : commandCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .limit(5)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList())) {
                
                List<MetadataService.OperationMetadata> commandOps = opsByCommand.getOrDefault(command, new ArrayList<>());
                if (commandOps.isEmpty()) {
                    continue;
                }
                
                System.out.println("\nCommand: " + command);
                
                // Count parameter occurrences
                Map<String, Integer> paramCounts = new HashMap<>();
                for (MetadataService.OperationMetadata op : commandOps) {
                    if (op.getParameters() != null) {
                        for (String param : op.getParameters().keySet()) {
                            // Skip internal parameters
                            if (param.startsWith("_") || param.equals("details") || param.equals("errors")) {
                                continue;
                            }
                            paramCounts.put(param, paramCounts.getOrDefault(param, 0) + 1);
                        }
                    }
                }
                
                // Display top parameters
                List<Map.Entry<String, Integer>> sortedParams = paramCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .collect(Collectors.toList());
                
                if (sortedParams.isEmpty()) {
                    System.out.println("  No parameter data available");
                } else {
                    for (Map.Entry<String, Integer> param : sortedParams) {
                        double percentage = (param.getValue() / (double) commandOps.size()) * 100;
                        System.out.printf("  %-20s: used in %.1f%% of operations\n", 
                                         param.getKey(), percentage);
                    }
                }
            }
        }
    }
    
    /**
     * Displays user activity analytics.
     *
     * @param operationId the operation tracking ID
     */
    private void displayUserAnalytics(String operationId) {
        // Get operations for detailed analysis
        List<MetadataService.OperationMetadata> operations = metadataService.listOperations(commandFilter, null, 1000);
        
        // Filter operations by date range
        operations = operations.stream()
            .filter(op -> !op.getStartTime().isBefore(startDate))
            .filter(op -> !op.getStartTime().isAfter(endDate))
            .collect(Collectors.toList());
        
        // Further filter by user if specified
        if (userFilter != null) {
            operations = operations.stream()
                .filter(op -> userFilter.equals(op.getUsername()))
                .collect(Collectors.toList());
        }
        
        // Format as JSON if requested
        if (jsonOutput) {
            displayJsonUserAnalytics(operations);
            return;
        }
        
        // Display text output
        System.out.println(AnsiFormatter.BOLD + "USER ACTIVITY ANALYTICS" + AnsiFormatter.RESET);
        System.out.println("======================\n");
        
        // Date range header
        System.out.println("Date Range: " + startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + 
                          " to " + endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (userFilter != null) {
            System.out.println("User Filter: " + userFilter);
        }
        if (commandFilter != null) {
            System.out.println("Command Filter: " + commandFilter);
        }
        System.out.println();
        
        if (operations.isEmpty()) {
            System.out.println("No operation data available for the specified filters.");
            return;
        }
        
        // User activity breakdown
        System.out.println(AnsiFormatter.BOLD + "USER ACTIVITY BREAKDOWN" + AnsiFormatter.RESET);
        System.out.println("-----------------------");
        
        // Group operations by user
        Map<String, List<MetadataService.OperationMetadata>> opsByUser = operations.stream()
            .collect(Collectors.groupingBy(MetadataService.OperationMetadata::getUsername));
        
        // Convert to a sortable list and limit to top N
        List<Map.Entry<String, List<MetadataService.OperationMetadata>>> sortedUsers = opsByUser.entrySet().stream()
            .sorted(Comparator.<Map.Entry<String, List<MetadataService.OperationMetadata>>>comparingInt(
                e -> e.getValue().size()).reversed())
            .limit(limit)
            .collect(Collectors.toList());
        
        // Find max value for scaling
        int maxCount = sortedUsers.isEmpty() ? 1 : sortedUsers.get(0).getValue().size();
        int totalOps = operations.size();
        
        // Display each user with a bar chart
        for (Map.Entry<String, List<MetadataService.OperationMetadata>> entry : sortedUsers) {
            String user = entry.getKey();
            int count = entry.getValue().size();
            double percentage = (count / (double) totalOps) * 100;
            int barLength = (int) Math.round((count / (double) maxCount) * 40);
            
            System.out.printf("%-15s: ", user);
            System.out.print(AnsiFormatter.BRIGHT_FG_CYAN);
            for (int i = 0; i < barLength; i++) {
                System.out.print("█");
            }
            System.out.print(AnsiFormatter.RESET);
            System.out.printf(" %d (%.1f%%)\n", count, percentage);
        }
        
        // User command preferences
        System.out.println("\n" + AnsiFormatter.BOLD + "USER COMMAND PREFERENCES" + AnsiFormatter.RESET);
        System.out.println("------------------------");
        
        if (userFilter != null) {
            // For a specific user, show detailed command breakdown
            List<MetadataService.OperationMetadata> userOps = opsByUser.getOrDefault(userFilter, new ArrayList<>());
            if (userOps.isEmpty()) {
                System.out.println("No operations found for user: " + userFilter);
            } else {
                // Group by command
                Map<String, Long> commandCounts = userOps.stream()
                    .collect(Collectors.groupingBy(MetadataService.OperationMetadata::getCommandName, Collectors.counting()));
                
                // Sort and display
                List<Map.Entry<String, Long>> sortedCommands = commandCounts.entrySet().stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .collect(Collectors.toList());
                
                long maxCommandCount = sortedCommands.isEmpty() ? 1 : sortedCommands.get(0).getValue();
                
                for (Map.Entry<String, Long> cmd : sortedCommands) {
                    String command = cmd.getKey();
                    long count = cmd.getValue();
                    double percentage = (count / (double) userOps.size()) * 100;
                    int barLength = (int) Math.round((count / (double) maxCommandCount) * 40);
                    
                    System.out.printf("%-15s: ", command);
                    System.out.print(AnsiFormatter.BRIGHT_FG_CYAN);
                    for (int i = 0; i < barLength; i++) {
                        System.out.print("█");
                    }
                    System.out.print(AnsiFormatter.RESET);
                    System.out.printf(" %d (%.1f%%)\n", count, percentage);
                }
            }
        } else {
            // For all users, show top command for each user
            for (Map.Entry<String, List<MetadataService.OperationMetadata>> entry : sortedUsers) {
                String user = entry.getKey();
                List<MetadataService.OperationMetadata> userOps = entry.getValue();
                
                // Get top command for this user
                Map<String, Long> userCommandCounts = userOps.stream()
                    .collect(Collectors.groupingBy(MetadataService.OperationMetadata::getCommandName, Collectors.counting()));
                
                String topCommand = userCommandCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse("unknown");
                
                long topCommandCount = userCommandCounts.getOrDefault(topCommand, 0L);
                double topCommandPercentage = (topCommandCount / (double) userOps.size()) * 100;
                
                System.out.printf("%-15s: favorite command is '%-15s' (%d uses, %.1f%% of activity)\n",
                                 user, topCommand, topCommandCount, topCommandPercentage);
            }
        }
        
        // User success rates
        System.out.println("\n" + AnsiFormatter.BOLD + "USER SUCCESS RATES" + AnsiFormatter.RESET);
        System.out.println("------------------");
        
        for (Map.Entry<String, List<MetadataService.OperationMetadata>> entry : sortedUsers) {
            String user = entry.getKey();
            List<MetadataService.OperationMetadata> userOps = entry.getValue();
            
            long successCount = userOps.stream()
                .filter(op -> "COMPLETED".equals(op.getStatus()))
                .count();
            
            double successRate = (successCount / (double) userOps.size()) * 100;
            
            System.out.printf("%-15s: ", user);
            System.out.println(StatisticsVisualizer.createProgressMeter(successRate, 100));
        }
    }
    
    /**
     * Displays performance analytics for commands.
     *
     * @param operationId the operation tracking ID
     */
    private void displayPerformanceAnalytics(String operationId) {
        // Get operations for detailed analysis
        List<MetadataService.OperationMetadata> operations = metadataService.listOperations(commandFilter, null, 1000);
        
        // Filter operations by date range and completed status
        operations = operations.stream()
            .filter(op -> !op.getStartTime().isBefore(startDate))
            .filter(op -> !op.getStartTime().isAfter(endDate))
            .filter(op -> "COMPLETED".equals(op.getStatus())) // Only look at completed operations for performance
            .filter(op -> op.getEndTime() != null) // Ensure we have end time for duration calculation
            .collect(Collectors.toList());
        
        // Format as JSON if requested
        if (jsonOutput) {
            displayJsonPerformanceAnalytics(operations);
            return;
        }
        
        // Display text output
        System.out.println(AnsiFormatter.BOLD + "PERFORMANCE ANALYTICS" + AnsiFormatter.RESET);
        System.out.println("=====================\n");
        
        // Date range header
        System.out.println("Date Range: " + startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + 
                          " to " + endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (commandFilter != null) {
            System.out.println("Command Filter: " + commandFilter);
        }
        System.out.println();
        
        if (operations.isEmpty()) {
            System.out.println("No completed operation data available for the specified filters.");
            return;
        }
        
        // Calculate performance metrics by command
        Map<String, List<Long>> durationsByCommand = new HashMap<>();
        for (MetadataService.OperationMetadata op : operations) {
            String command = op.getCommandName();
            long durationMs = ChronoUnit.MILLIS.between(op.getStartTime(), op.getEndTime());
            
            if (!durationsByCommand.containsKey(command)) {
                durationsByCommand.put(command, new ArrayList<>());
            }
            durationsByCommand.get(command).add(durationMs);
        }
        
        // Average duration by command
        System.out.println(AnsiFormatter.BOLD + "AVERAGE COMMAND EXECUTION TIME" + AnsiFormatter.RESET);
        System.out.println("-----------------------------");
        
        // Calculate statistics for each command
        List<Map.Entry<String, Double>> avgDurations = new ArrayList<>();
        for (Map.Entry<String, List<Long>> entry : durationsByCommand.entrySet()) {
            String command = entry.getKey();
            List<Long> durations = entry.getValue();
            double avgDuration = durations.stream().mapToLong(d -> d).average().orElse(0);
            avgDurations.add(Map.entry(command, avgDuration));
        }
        
        // Sort by average duration (descending)
        avgDurations.sort(Map.Entry.<String, Double>comparingByValue().reversed());
        
        // Display average durations with bar charts
        double maxAvgDuration = avgDurations.isEmpty() ? 1 : avgDurations.get(0).getValue();
        
        for (Map.Entry<String, Double> entry : avgDurations) {
            String command = entry.getKey();
            double avgDuration = entry.getValue();
            int barLength = (int) Math.round((avgDuration / maxAvgDuration) * 40);
            
            System.out.printf("%-15s: ", command);
            
            // Color-code based on duration
            String color;
            if (avgDuration < 100) {
                color = AnsiFormatter.BRIGHT_FG_GREEN;  // Fast
            } else if (avgDuration < 500) {
                color = AnsiFormatter.BRIGHT_FG_YELLOW; // Medium
            } else {
                color = AnsiFormatter.BRIGHT_FG_RED;    // Slow
            }
            
            System.out.print(color);
            for (int i = 0; i < barLength; i++) {
                System.out.print("█");
            }
            System.out.print(AnsiFormatter.RESET);
            System.out.printf(" %.2f ms\n", avgDuration);
        }
        
        // Performance variance by command
        System.out.println("\n" + AnsiFormatter.BOLD + "PERFORMANCE CONSISTENCY" + AnsiFormatter.RESET);
        System.out.println("-----------------------");
        
        // Calculate min, max, and variance for each command
        for (Map.Entry<String, List<Long>> entry : durationsByCommand.entrySet()) {
            String command = entry.getKey();
            List<Long> durations = entry.getValue();
            
            if (durations.size() < 2) {
                continue; // Skip commands with too few data points
            }
            
            long minDuration = durations.stream().min(Long::compare).orElse(0L);
            long maxDuration = durations.stream().max(Long::compare).orElse(0L);
            double avgDuration = durations.stream().mapToLong(d -> d).average().orElse(0);
            
            // Calculate standard deviation
            double variance = durations.stream()
                .mapToDouble(d -> Math.pow(d - avgDuration, 2))
                .average()
                .orElse(0);
            double stdDev = Math.sqrt(variance);
            
            // Calculate coefficient of variation (relative variance)
            double cv = avgDuration > 0 ? (stdDev / avgDuration) * 100 : 0;
            
            System.out.printf("%-15s: ", command);
            System.out.printf("min=%.2f ms, avg=%.2f ms, max=%.2f ms, variance=%.2f%%\n",
                             (double) minDuration, avgDuration, (double) maxDuration, cv);
        }
        
        // Slowest individual operations
        System.out.println("\n" + AnsiFormatter.BOLD + "TOP 10 SLOWEST OPERATIONS" + AnsiFormatter.RESET);
        System.out.println("------------------------");
        
        // Sort all operations by duration
        List<MetadataService.OperationMetadata> sortedByDuration = operations.stream()
            .sorted((op1, op2) -> {
                long d1 = ChronoUnit.MILLIS.between(op1.getStartTime(), op1.getEndTime());
                long d2 = ChronoUnit.MILLIS.between(op2.getStartTime(), op2.getEndTime());
                return Long.compare(d2, d1); // Descending
            })
            .limit(10)
            .collect(Collectors.toList());
        
        if (sortedByDuration.isEmpty()) {
            System.out.println("No operation duration data available.");
        } else {
            System.out.printf("%-8s %-15s %-24s %-12s\n", 
                             "ID", "Command", "Time", "Duration");
            System.out.println(String.join("", "-".repeat(8), " ", "-".repeat(15), " ", 
                                         "-".repeat(24), " ", "-".repeat(12)));
            
            for (MetadataService.OperationMetadata op : sortedByDuration) {
                String shortId = op.getId().substring(0, 6);
                String command = op.getCommandName();
                String time = op.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
                long durationMs = ChronoUnit.MILLIS.between(op.getStartTime(), op.getEndTime());
                
                System.out.printf("%-8s %-15s %-24s %-12d ms\n", 
                                 shortId, command, time, durationMs);
            }
        }
    }
    
    /**
     * Displays timeline analytics for operations.
     *
     * @param operationId the operation tracking ID
     */
    private void displayTimelineAnalytics(String operationId) {
        // Get operations for detailed analysis
        List<MetadataService.OperationMetadata> operations = metadataService.listOperations(commandFilter, null, 1000);
        
        // Filter operations by date range
        operations = operations.stream()
            .filter(op -> !op.getStartTime().isBefore(startDate))
            .filter(op -> !op.getStartTime().isAfter(endDate))
            .collect(Collectors.toList());
        
        // Format as JSON if requested
        if (jsonOutput) {
            displayJsonTimelineAnalytics(operations);
            return;
        }
        
        // Display text output
        System.out.println(AnsiFormatter.BOLD + "OPERATION TIMELINE ANALYTICS" + AnsiFormatter.RESET);
        System.out.println("===========================\n");
        
        // Date range header
        System.out.println("Date Range: " + startDate.format(DateTimeFormatter.ISO_LOCAL_DATE) + 
                          " to " + endDate.format(DateTimeFormatter.ISO_LOCAL_DATE));
        if (commandFilter != null) {
            System.out.println("Command Filter: " + commandFilter);
        }
        System.out.println();
        
        if (operations.isEmpty()) {
            System.out.println("No operation data available for the specified filters.");
            return;
        }
        
        // Group operations by day
        Map<String, List<MetadataService.OperationMetadata>> opsByDay = operations.stream()
            .collect(Collectors.groupingBy(op -> 
                op.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE)));
        
        // Activity by day
        System.out.println(AnsiFormatter.BOLD + "ACTIVITY BY DAY" + AnsiFormatter.RESET);
        System.out.println("--------------");
        
        // Sort days chronologically
        List<String> sortedDays = opsByDay.keySet().stream()
            .sorted()
            .collect(Collectors.toList());
        
        // Get max count for scaling
        int maxDayCount = opsByDay.values().stream()
            .mapToInt(List::size)
            .max()
            .orElse(1);
        
        // Display activity for each day with a bar chart
        for (String day : sortedDays) {
            List<MetadataService.OperationMetadata> dayOps = opsByDay.get(day);
            int count = dayOps.size();
            int barLength = (int) Math.round((count / (double) maxDayCount) * 40);
            
            System.out.printf("%-12s: ", day);
            System.out.print(AnsiFormatter.BRIGHT_FG_CYAN);
            for (int i = 0; i < barLength; i++) {
                System.out.print("█");
            }
            System.out.print(AnsiFormatter.RESET);
            System.out.printf(" %d\n", count);
        }
        
        // Activity by hour (aggregated across all days)
        System.out.println("\n" + AnsiFormatter.BOLD + "ACTIVITY BY HOUR OF DAY" + AnsiFormatter.RESET);
        System.out.println("---------------------");
        
        // Initialize counters for each hour
        int[] hourCounts = new int[24];
        for (MetadataService.OperationMetadata op : operations) {
            int hour = op.getStartTime().getHour();
            hourCounts[hour]++;
        }
        
        // Find max for scaling
        int maxHourCount = 0;
        for (int count : hourCounts) {
            maxHourCount = Math.max(maxHourCount, count);
        }
        
        // Display activity for each hour
        for (int hour = 0; hour < 24; hour++) {
            int count = hourCounts[hour];
            int barLength = (int) Math.round((count / (double) maxHourCount) * 40);
            
            System.out.printf("%02d:00-%02d:59: ", hour, hour);
            System.out.print(AnsiFormatter.BRIGHT_FG_CYAN);
            for (int i = 0; i < barLength; i++) {
                System.out.print("█");
            }
            System.out.print(AnsiFormatter.RESET);
            System.out.printf(" %d\n", count);
        }
        
        // Recent activity trend
        System.out.println("\n" + AnsiFormatter.BOLD + "RECENT ACTIVITY TREND" + AnsiFormatter.RESET);
        System.out.println("--------------------");
        
        // Get most recent operations
        List<MetadataService.OperationMetadata> recentOps = operations.stream()
            .sorted(Comparator.comparing(MetadataService.OperationMetadata::getStartTime).reversed())
            .limit(20)
            .sorted(Comparator.comparing(MetadataService.OperationMetadata::getStartTime))
            .collect(Collectors.toList());
        
        if (recentOps.size() < 5) {
            System.out.println("Not enough data for trend analysis.");
        } else {
            System.out.println("Last 20 operations:");
            
            for (MetadataService.OperationMetadata op : recentOps) {
                String time = op.getStartTime().format(DateTimeFormatter.ISO_LOCAL_TIME);
                String status = "COMPLETED".equals(op.getStatus()) ? "✓" : "✗";
                String statusColor = "COMPLETED".equals(op.getStatus()) ? 
                    AnsiFormatter.BRIGHT_FG_GREEN : AnsiFormatter.BRIGHT_FG_RED;
                
                System.out.printf("%s %s%s%s %s %s\n", 
                                 time, 
                                 statusColor, status, AnsiFormatter.RESET,
                                 op.getCommandName(),
                                 op.getUsername());
            }
        }
    }
    
    /**
     * Displays help information about the dashboard command.
     *
     * @param operationId the operation tracking ID
     */
    private void displayHelp(String operationId) {
        if (jsonOutput) {
            Map<String, Object> helpData = new HashMap<>();
            helpData.put("command", "dashboard");
            helpData.put("description", "Displays a unified operations analytics dashboard");
            
            // Views
            List<Map<String, String>> views = new ArrayList<>();
            views.add(createHelpEntry("summary", "Summary dashboard with key metrics (default)"));
            views.add(createHelpEntry("commands", "Detailed command usage analytics"));
            views.add(createHelpEntry("users", "User activity analytics"));
            views.add(createHelpEntry("performance", "Command performance analytics"));
            views.add(createHelpEntry("timeline", "Operation timeline analytics"));
            helpData.put("views", views);
            
            // Options
            List<Map<String, String>> options = new ArrayList<>();
            options.add(createHelpEntry("--days=<n>", "Number of days of data to include (default: 30)"));
            options.add(createHelpEntry("--limit=<n>", "Maximum items to display in breakdowns (default: 10)"));
            options.add(createHelpEntry("--start-date=<date>", "Start date in ISO format (yyyy-MM-dd'T'HH:mm:ss)"));
            options.add(createHelpEntry("--end-date=<date>", "End date in ISO format (yyyy-MM-dd'T'HH:mm:ss)"));
            options.add(createHelpEntry("--command=<name>", "Filter by command name"));
            options.add(createHelpEntry("--user=<name>", "Filter by username"));
            options.add(createHelpEntry("--refresh=<seconds>", "Auto-refresh interval in seconds"));
            options.add(createHelpEntry("--format=<format>", "Output format (text or json)"));
            options.add(createHelpEntry("--verbose", "Show detailed information"));
            helpData.put("options", options);
            
            // Examples
            List<Map<String, String>> examples = new ArrayList<>();
            examples.add(createHelpEntry("dashboard", "Show summary dashboard"));
            examples.add(createHelpEntry("dashboard commands", "Show command usage analytics"));
            examples.add(createHelpEntry("dashboard users --user=john", "Show analytics for user 'john'"));
            examples.add(createHelpEntry("dashboard --days=7", "Show data for the last 7 days"));
            examples.add(createHelpEntry("dashboard --command=add", "Filter dashboard for 'add' command"));
            examples.add(createHelpEntry("dashboard --refresh=5", "Refresh dashboard every 5 seconds"));
            helpData.put("examples", examples);
            
            System.out.println(OutputFormatter.toJson(helpData, verbose));
        } else {
            System.out.println(AnsiFormatter.BOLD + "OPERATION DASHBOARD COMMAND" + AnsiFormatter.RESET);
            System.out.println("=========================\n");
            
            System.out.println("Displays a unified operations analytics dashboard to visualize command usage patterns.");
            System.out.println();
            
            System.out.println(AnsiFormatter.BOLD + "Available Views:" + AnsiFormatter.RESET);
            System.out.println("  summary      Summary dashboard with key metrics (default)");
            System.out.println("  commands     Detailed command usage analytics");
            System.out.println("  users        User activity analytics");
            System.out.println("  performance  Command performance analytics");
            System.out.println("  timeline     Operation timeline analytics");
            System.out.println("  help         Show this help information");
            System.out.println();
            
            System.out.println(AnsiFormatter.BOLD + "Options:" + AnsiFormatter.RESET);
            System.out.println("  --days=<n>             Number of days of data to include (default: 30)");
            System.out.println("  --limit=<n>            Maximum items to display in breakdowns (default: 10)");
            System.out.println("  --start-date=<date>    Start date in ISO format (yyyy-MM-dd'T'HH:mm:ss)");
            System.out.println("  --end-date=<date>      End date in ISO format (yyyy-MM-dd'T'HH:mm:ss)");
            System.out.println("  --command=<name>       Filter by command name");
            System.out.println("  --user=<name>          Filter by username");
            System.out.println("  --refresh=<seconds>    Auto-refresh interval in seconds");
            System.out.println("  --format=<format>      Output format (text or json)");
            System.out.println("  --verbose              Show detailed information");
            System.out.println();
            
            System.out.println(AnsiFormatter.BOLD + "Examples:" + AnsiFormatter.RESET);
            System.out.println("  rin dashboard               # Show summary dashboard");
            System.out.println("  rin dashboard commands      # Show command usage analytics");
            System.out.println("  rin dashboard users --user=john  # Show analytics for user 'john'");
            System.out.println("  rin dashboard --days=7      # Show data for the last 7 days");
            System.out.println("  rin dashboard --command=add # Filter dashboard for 'add' command");
            System.out.println("  rin dashboard --refresh=5   # Refresh dashboard every 5 seconds");
        }
        
        // Record successful operation
        Map<String, Object> result = new HashMap<>();
        result.put("action", "help");
        result.put("format", format);
        metadataService.completeOperation(operationId, result);
    }
    
    /**
     * Displays a JSON summary of operational statistics.
     *
     * @param statistics the statistics data
     */
    private void displayJsonSummary(Map<String, Object> statistics) {
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("view", "summary");
        jsonData.put("date_range", Map.of(
            "start", startDate.toString(),
            "end", endDate.toString()
        ));
        
        if (commandFilter != null) {
            jsonData.put("command_filter", commandFilter);
        }
        if (userFilter != null) {
            jsonData.put("user_filter", userFilter);
        }
        
        // Key metrics
        Map<String, Object> keyMetrics = new HashMap<>();
        keyMetrics.put("total_operations", statistics.getOrDefault("totalOperations", 0));
        keyMetrics.put("completed_operations", statistics.getOrDefault("completedOperations", 0));
        keyMetrics.put("failed_operations", statistics.getOrDefault("failedOperations", 0));
        keyMetrics.put("success_rate", statistics.getOrDefault("successRate", 0.0));
        keyMetrics.put("average_duration_ms", statistics.getOrDefault("averageDurationMs", 0.0));
        jsonData.put("key_metrics", keyMetrics);
        
        // Command breakdown
        jsonData.put("commands_by_usage", statistics.getOrDefault("operationsByCommand", new HashMap<>()));
        
        // Operation types
        jsonData.put("operations_by_type", statistics.getOrDefault("operationsByType", new HashMap<>()));
        
        // Output the JSON
        System.out.println(OutputFormatter.toJson(jsonData, verbose));
    }
    
    /**
     * Displays JSON command analytics data.
     *
     * @param statistics the operation statistics
     * @param operations the list of operations
     */
    private void displayJsonCommandsAnalytics(Map<String, Object> statistics, List<MetadataService.OperationMetadata> operations) {
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("view", "commands");
        jsonData.put("date_range", Map.of(
            "start", startDate.toString(),
            "end", endDate.toString()
        ));
        
        if (commandFilter != null) {
            jsonData.put("command_filter", commandFilter);
        }
        
        // Command usage breakdown
        jsonData.put("command_usage", statistics.getOrDefault("operationsByCommand", new HashMap<>()));
        
        // Command success rates
        Map<String, Map<String, Object>> commandSuccessRates = new HashMap<>();
        for (MetadataService.OperationMetadata op : operations) {
            String command = op.getCommandName();
            boolean success = "COMPLETED".equals(op.getStatus());
            
            if (!commandSuccessRates.containsKey(command)) {
                Map<String, Object> metrics = new HashMap<>();
                metrics.put("success_count", 0);
                metrics.put("total_count", 0);
                metrics.put("success_rate", 0.0);
                commandSuccessRates.put(command, metrics);
            }
            
            Map<String, Object> metrics = commandSuccessRates.get(command);
            if (success) {
                metrics.put("success_count", ((Number)metrics.get("success_count")).intValue() + 1);
            }
            metrics.put("total_count", ((Number)metrics.get("total_count")).intValue() + 1);
            
            // Update success rate
            double successRate = ((Number)metrics.get("success_count")).doubleValue() / 
                                ((Number)metrics.get("total_count")).doubleValue() * 100;
            metrics.put("success_rate", successRate);
        }
        jsonData.put("command_success_rates", commandSuccessRates);
        
        // If verbose, add parameter usage data
        if (verbose) {
            Map<String, Map<String, Object>> parameterUsage = new HashMap<>();
            
            // Group operations by command
            Map<String, List<MetadataService.OperationMetadata>> opsByCommand = operations.stream()
                .collect(Collectors.groupingBy(MetadataService.OperationMetadata::getCommandName));
            
            for (Map.Entry<String, List<MetadataService.OperationMetadata>> entry : opsByCommand.entrySet()) {
                String command = entry.getKey();
                List<MetadataService.OperationMetadata> commandOps = entry.getValue();
                
                // Count parameter occurrences
                Map<String, Integer> paramCounts = new HashMap<>();
                for (MetadataService.OperationMetadata op : commandOps) {
                    if (op.getParameters() != null) {
                        for (String param : op.getParameters().keySet()) {
                            // Skip internal parameters
                            if (param.startsWith("_") || param.equals("details") || param.equals("errors")) {
                                continue;
                            }
                            paramCounts.put(param, paramCounts.getOrDefault(param, 0) + 1);
                        }
                    }
                }
                
                // Calculate usage percentages
                Map<String, Object> paramUsage = new HashMap<>();
                for (Map.Entry<String, Integer> paramEntry : paramCounts.entrySet()) {
                    String param = paramEntry.getKey();
                    int count = paramEntry.getValue();
                    double percentage = (count / (double) commandOps.size()) * 100;
                    
                    Map<String, Object> paramData = new HashMap<>();
                    paramData.put("count", count);
                    paramData.put("percentage", percentage);
                    paramUsage.put(param, paramData);
                }
                
                parameterUsage.put(command, paramUsage);
            }
            
            jsonData.put("parameter_usage", parameterUsage);
        }
        
        // Output the JSON
        System.out.println(OutputFormatter.toJson(jsonData, verbose));
    }
    
    /**
     * Displays JSON user analytics data.
     *
     * @param operations the list of operations
     */
    private void displayJsonUserAnalytics(List<MetadataService.OperationMetadata> operations) {
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("view", "users");
        jsonData.put("date_range", Map.of(
            "start", startDate.toString(),
            "end", endDate.toString()
        ));
        
        if (userFilter != null) {
            jsonData.put("user_filter", userFilter);
        }
        if (commandFilter != null) {
            jsonData.put("command_filter", commandFilter);
        }
        
        // Group operations by user
        Map<String, List<MetadataService.OperationMetadata>> opsByUser = operations.stream()
            .collect(Collectors.groupingBy(MetadataService.OperationMetadata::getUsername));
        
        // User activity data
        Map<String, Map<String, Object>> userActivity = new HashMap<>();
        for (Map.Entry<String, List<MetadataService.OperationMetadata>> entry : opsByUser.entrySet()) {
            String user = entry.getKey();
            List<MetadataService.OperationMetadata> userOps = entry.getValue();
            
            Map<String, Object> userData = new HashMap<>();
            userData.put("operation_count", userOps.size());
            
            // Calculate success rate
            long successCount = userOps.stream()
                .filter(op -> "COMPLETED".equals(op.getStatus()))
                .count();
            double successRate = (successCount / (double) userOps.size()) * 100;
            userData.put("success_count", successCount);
            userData.put("success_rate", successRate);
            
            // Command breakdown
            Map<String, Long> commandCounts = userOps.stream()
                .collect(Collectors.groupingBy(MetadataService.OperationMetadata::getCommandName, Collectors.counting()));
            userData.put("command_usage", commandCounts);
            
            // Find top command
            String topCommand = commandCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("unknown");
            userData.put("top_command", topCommand);
            
            userActivity.put(user, userData);
        }
        
        jsonData.put("user_activity", userActivity);
        
        // Output the JSON
        System.out.println(OutputFormatter.toJson(jsonData, verbose));
    }
    
    /**
     * Displays JSON performance analytics data.
     *
     * @param operations the list of operations
     */
    private void displayJsonPerformanceAnalytics(List<MetadataService.OperationMetadata> operations) {
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("view", "performance");
        jsonData.put("date_range", Map.of(
            "start", startDate.toString(),
            "end", endDate.toString()
        ));
        
        if (commandFilter != null) {
            jsonData.put("command_filter", commandFilter);
        }
        
        // Calculate performance metrics by command
        Map<String, List<Long>> durationsByCommand = new HashMap<>();
        for (MetadataService.OperationMetadata op : operations) {
            String command = op.getCommandName();
            long durationMs = ChronoUnit.MILLIS.between(op.getStartTime(), op.getEndTime());
            
            if (!durationsByCommand.containsKey(command)) {
                durationsByCommand.put(command, new ArrayList<>());
            }
            durationsByCommand.get(command).add(durationMs);
        }
        
        // Performance metrics for each command
        Map<String, Map<String, Object>> commandPerformance = new HashMap<>();
        for (Map.Entry<String, List<Long>> entry : durationsByCommand.entrySet()) {
            String command = entry.getKey();
            List<Long> durations = entry.getValue();
            
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("operation_count", durations.size());
            
            if (!durations.isEmpty()) {
                double avgDuration = durations.stream().mapToLong(d -> d).average().orElse(0);
                long minDuration = durations.stream().min(Long::compare).orElse(0L);
                long maxDuration = durations.stream().max(Long::compare).orElse(0L);
                
                metrics.put("avg_duration_ms", avgDuration);
                metrics.put("min_duration_ms", minDuration);
                metrics.put("max_duration_ms", maxDuration);
                
                // Calculate standard deviation
                if (durations.size() > 1) {
                    double variance = durations.stream()
                        .mapToDouble(d -> Math.pow(d - avgDuration, 2))
                        .average()
                        .orElse(0);
                    double stdDev = Math.sqrt(variance);
                    
                    metrics.put("std_deviation_ms", stdDev);
                    
                    // Coefficient of variation (relative variance)
                    double cv = avgDuration > 0 ? (stdDev / avgDuration) * 100 : 0;
                    metrics.put("coefficient_of_variation", cv);
                }
            }
            
            commandPerformance.put(command, metrics);
        }
        
        jsonData.put("command_performance", commandPerformance);
        
        // Slowest operations
        List<Map<String, Object>> slowestOps = operations.stream()
            .sorted((op1, op2) -> {
                long d1 = ChronoUnit.MILLIS.between(op1.getStartTime(), op1.getEndTime());
                long d2 = ChronoUnit.MILLIS.between(op2.getStartTime(), op2.getEndTime());
                return Long.compare(d2, d1); // Descending
            })
            .limit(10)
            .map(op -> {
                Map<String, Object> opData = new HashMap<>();
                opData.put("id", op.getId());
                opData.put("command", op.getCommandName());
                opData.put("start_time", op.getStartTime().toString());
                opData.put("duration_ms", ChronoUnit.MILLIS.between(op.getStartTime(), op.getEndTime()));
                opData.put("user", op.getUsername());
                return opData;
            })
            .collect(Collectors.toList());
        
        jsonData.put("slowest_operations", slowestOps);
        
        // Output the JSON
        System.out.println(OutputFormatter.toJson(jsonData, verbose));
    }
    
    /**
     * Displays JSON timeline analytics data.
     *
     * @param operations the list of operations
     */
    private void displayJsonTimelineAnalytics(List<MetadataService.OperationMetadata> operations) {
        Map<String, Object> jsonData = new HashMap<>();
        jsonData.put("view", "timeline");
        jsonData.put("date_range", Map.of(
            "start", startDate.toString(),
            "end", endDate.toString()
        ));
        
        if (commandFilter != null) {
            jsonData.put("command_filter", commandFilter);
        }
        
        // Group operations by day
        Map<String, List<MetadataService.OperationMetadata>> opsByDay = operations.stream()
            .collect(Collectors.groupingBy(op -> 
                op.getStartTime().format(DateTimeFormatter.ISO_LOCAL_DATE)));
        
        // Daily activity counts
        Map<String, Integer> dailyActivity = new HashMap<>();
        for (Map.Entry<String, List<MetadataService.OperationMetadata>> entry : opsByDay.entrySet()) {
            dailyActivity.put(entry.getKey(), entry.getValue().size());
        }
        jsonData.put("daily_activity", dailyActivity);
        
        // Hourly activity (aggregated across all days)
        int[] hourCounts = new int[24];
        for (MetadataService.OperationMetadata op : operations) {
            int hour = op.getStartTime().getHour();
            hourCounts[hour]++;
        }
        
        Map<String, Integer> hourlyActivity = new HashMap<>();
        for (int hour = 0; hour < 24; hour++) {
            hourlyActivity.put(String.format("%02d:00", hour), hourCounts[hour]);
        }
        jsonData.put("hourly_activity", hourlyActivity);
        
        // Recent operations
        List<Map<String, Object>> recentOps = operations.stream()
            .sorted(Comparator.comparing(MetadataService.OperationMetadata::getStartTime).reversed())
            .limit(20)
            .map(op -> {
                Map<String, Object> opData = new HashMap<>();
                opData.put("id", op.getId());
                opData.put("command", op.getCommandName());
                opData.put("time", op.getStartTime().toString());
                opData.put("status", op.getStatus());
                opData.put("user", op.getUsername());
                return opData;
            })
            .collect(Collectors.toList());
        
        jsonData.put("recent_operations", recentOps);
        
        // Output the JSON
        System.out.println(OutputFormatter.toJson(jsonData, verbose));
    }
    
    /**
     * Helper method to create a help entry map.
     *
     * @param name the entry name
     * @param description the entry description
     * @return a map with name and description
     */
    private Map<String, String> createHelpEntry(String name, String description) {
        Map<String, String> entry = new HashMap<>();
        entry.put("name", name);
        entry.put("description", description);
        return entry;
    }
}