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
import java.util.stream.Collectors;

import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.stats.StatisticType;
import org.rinna.cli.stats.StatisticValue;
import org.rinna.cli.stats.StatisticsService;
import org.rinna.cli.stats.StatisticsVisualizer;
import org.rinna.cli.util.OutputFormatter;

/**
 * Command for displaying statistics about the project.
 * This command provides various views of project statistics:
 * - Summary view (default): Shows key metrics
 * - Dashboard view: Shows a visual dashboard of metrics
 * - Distribution view: Shows item distributions with charts
 * - Detail view: Shows detailed stats for a specific area
 * 
 * Usage examples:
 * - rin stats                    # Show summary statistics
 * - rin stats dashboard          # Show statistics dashboard
 * - rin stats all                # Show all available statistics
 * - rin stats distribution       # Show distribution charts
 * - rin stats detail workflow    # Show workflow details
 * - rin stats --format=json      # Output in JSON format
 * - rin stats --verbose          # Show verbose output with additional details
 * - rin stats --limit=5          # Limit output to top 5 items per category
 */
public class StatsCommand implements Callable<Integer> {
    
    private String type = "summary"; // default to summary view
    private String format = "table"; // default to table format
    private int limit = 0; // 0 means no limit
    private String[] filterArgs = new String[0];
    private boolean verbose = false; // Verbose output flag
    
    private final ServiceManager serviceManager;
    private final MetadataService metadataService;
    private final ConfigurationService configService;
    private final StatisticsService statsService;
    
    /**
     * Creates a new StatsCommand with default services.
     */
    public StatsCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Creates a new StatsCommand with the specified service manager.
     * 
     * @param serviceManager the service manager to use
     */
    public StatsCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.metadataService = serviceManager.getMetadataService();
        this.configService = serviceManager.getConfigurationService();
        this.statsService = StatisticsService.getInstance();
    }
    
    /**
     * Gets the statistics command type.
     *
     * @return the statistics type
     */
    public String getType() {
        return type;
    }
    
    /**
     * Sets the type of statistics to display.
     * 
     * @param type the statistics type
     */
    public void setType(String type) {
        if (type != null && !type.isEmpty()) {
            this.type = type.toLowerCase();
        }
    }
    
    /**
     * Gets the output format.
     *
     * @return the output format
     */
    public String getFormat() {
        return format;
    }
    
    /**
     * Sets the output format.
     * 
     * @param format the output format
     */
    public void setFormat(String format) {
        if (format != null && !format.isEmpty()) {
            this.format = format.toLowerCase();
        }
    }
    
    /**
     * Gets the maximum number of items to display.
     *
     * @return the limit
     */
    public int getLimit() {
        return limit;
    }
    
    /**
     * Sets the maximum number of items to display.
     * 
     * @param limit the limit
     */
    public void setLimit(int limit) {
        this.limit = limit;
    }
    
    /**
     * Gets the filter arguments.
     *
     * @return the filter arguments
     */
    public String[] getFilterArgs() {
        return filterArgs;
    }
    
    /**
     * Sets filter arguments.
     * 
     * @param filterArgs the filter arguments
     */
    public void setFilterArgs(String[] filterArgs) {
        this.filterArgs = filterArgs;
    }
    
    /**
     * Sets the JSON output flag.
     * This is a backwards compatibility method for the --json flag.
     * 
     * @param jsonOutput true to output in JSON format, false for text
     */
    public void setJsonOutput(boolean jsonOutput) {
        if (jsonOutput) {
            this.format = "json";
        }
    }
    
    /**
     * Gets whether JSON output is enabled.
     *
     * @return true if JSON output is enabled
     */
    public boolean isJsonOutput() {
        return "json".equalsIgnoreCase(format);
    }

    /**
     * Gets whether verbose output is enabled.
     *
     * @return true if verbose output is enabled
     */
    public boolean isVerbose() {
        return verbose;
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
     * Execute the statistics command.
     */
    @Override
    public Integer call() {
        // Generate a unique operation ID for tracking this command execution
        String operationId = generateOperationId();
        
        try {
            // Validate inputs
            validateInputs(operationId);
            
            // Handle refresh command as a special case
            if ("refresh".equals(type)) {
                return refreshStatistics(operationId);
            }
            
            // Process the specific statistics request
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("type", type);
            resultData.put("format", format);
            
            // Handle different types of statistics
            switch (type) {
                case "summary":
                    displaySummary();
                    resultData.put("view", "summary");
                    break;
                    
                case "all":
                    displayAllStats();
                    resultData.put("view", "all");
                    break;
                    
                case "dashboard":
                    displayDashboard();
                    resultData.put("view", "dashboard");
                    break;
                    
                case "distribution":
                    displayDistribution();
                    resultData.put("view", "distribution");
                    break;
                    
                case "trend":
                    displayTrend();
                    resultData.put("view", "trend");
                    break;
                    
                case "detail":
                    displayDetail();
                    resultData.put("view", "detail");
                    if (filterArgs != null && filterArgs.length > 0) {
                        resultData.put("detail_type", filterArgs[0]);
                    }
                    break;
                    
                case "help":
                    displayHelp();
                    resultData.put("view", "help");
                    break;
                    
                default:
                    // Check if it's a specific statistic type
                    try {
                        StatisticType statType = StatisticType.valueOf(type.toUpperCase());
                        displaySpecificStat(statType);
                        resultData.put("view", "specific_stat");
                        resultData.put("stat_type", statType.name());
                    } catch (IllegalArgumentException e) {
                        return handleError(operationId, 
                            new IllegalArgumentException("Unknown statistics type: " + type), 
                            "Unknown statistics type: " + type);
                    }
            }
            
            // Record the successful operation
            metadataService.completeOperation(operationId, resultData);
            return 0;
            
        } catch (Exception e) {
            return handleError(operationId, e, "Error displaying statistics: " + e.getMessage());
        }
    }
    
    /**
     * Generates an operation ID for tracking this command execution.
     *
     * @return the operation ID
     */
    private String generateOperationId() {
        // Operation tracking parameters
        Map<String, Object> params = new HashMap<>();
        params.put("type", type);
        params.put("format", format);
        params.put("limit", limit);
        params.put("json_output", isJsonOutput());
        params.put("verbose", verbose);
        if (filterArgs != null && filterArgs.length > 0) {
            params.put("filter_args", String.join(",", filterArgs));
        }
        
        // Start tracking the operation
        return metadataService.startOperation("stats", "READ", params);
    }
    
    /**
     * Handles error reporting consistently.
     *
     * @param operationId the operation ID for tracking
     * @param e the exception that occurred
     * @param userMessage the message to display to the user
     * @return 1 to indicate failure
     */
    private int handleError(String operationId, Exception e, String userMessage) {
        if (isJsonOutput()) {
            System.out.println(
                OutputFormatter.formatJsonMessage(
                    "error",
                    e.getMessage(),
                    null
                )
            );
        } else {
            System.err.println("Error: " + userMessage);
            if (verbose) {
                e.printStackTrace();
            }
        }
        
        // Record the failed operation with error details
        metadataService.failOperation(operationId, e);
        
        return 1;
    }
    
    /**
     * Validates command inputs and throws exceptions for invalid values.
     *
     * @param operationId the operation ID for tracking
     * @throws IllegalArgumentException if inputs are invalid
     */
    private void validateInputs(String operationId) throws IllegalArgumentException {
        // For now, we don't have specific validation needs for stats command
        // This method is included for consistency with the ViewCommand pattern
    }
    
    /**
     * Handles the refresh statistics operation.
     *
     * @param operationId the operation ID for tracking
     * @return 0 for success, non-zero for failure
     */
    private int refreshStatistics(String operationId) {
        try {
            statsService.refreshStatistics();
            
            // Format the output based on JSON or text
            if (isJsonOutput()) {
                System.out.println(
                    OutputFormatter.formatJsonMessage(
                        "success", 
                        "Statistics refreshed", 
                        Map.of("action", "refresh")
                    )
                );
            } else {
                System.out.println("Statistics refreshed.");
            }
            
            // Record the successful operation
            Map<String, Object> result = new HashMap<>();
            result.put("action", "refresh");
            result.put("status", "success");
            metadataService.completeOperation(operationId, result);
            
            return 0;
        } catch (Exception e) {
            return handleError(operationId, e, "Failed to refresh statistics: " + e.getMessage());
        }
    }
    
    /**
     * Displays a summary of key statistics.
     */
    private void displaySummary() {
        // Get the service from ServiceManager
        List<StatisticValue> summaryStats = statsService.getSummaryStatistics();
        
        if (summaryStats.isEmpty()) {
            if (isJsonOutput()) {
                System.out.println(
                    OutputFormatter.formatJsonMessage(
                        "warning", 
                        "No statistics available", 
                        Map.of("action", "Run 'rin stats refresh' to update statistics")
                    )
                );
            } else {
                System.out.println("No statistics available. Run 'rin stats refresh' to update statistics.");
            }
            return;
        }
        
        if (isJsonOutput()) {
            // Generate JSON output
            StringBuilder json = new StringBuilder();
            json.append("{\n  \"result\": \"success\",\n  \"type\": \"summary\",\n  \"statistics\": [\n");
            
            for (int i = 0; i < summaryStats.size(); i++) {
                StatisticValue stat = summaryStats.get(i);
                json.append("    {\n")
                    .append("      \"type\": \"").append(stat.getType()).append("\",\n")
                    .append("      \"description\": \"").append(stat.getDescription()).append("\",\n")
                    .append("      \"value\": ").append(String.format("%.2f", stat.getValue())).append(",\n")
                    .append("      \"unit\": \"").append(stat.getUnit()).append("\"");
                
                if (!stat.getBreakdown().isEmpty()) {
                    json.append(",\n      \"breakdown\": {\n");
                    
                    int breakdownCount = 0;
                    for (Map.Entry<String, Double> entry : stat.getBreakdown().entrySet()) {
                        json.append("        \"").append(entry.getKey()).append("\": ")
                            .append(String.format("%.2f", entry.getValue()));
                        
                        if (breakdownCount < stat.getBreakdown().size() - 1) {
                            json.append(",\n");
                        } else {
                            json.append("\n");
                        }
                        breakdownCount++;
                    }
                    
                    json.append("      }");
                }
                
                json.append("\n    }");
                
                if (i < summaryStats.size() - 1) {
                    json.append(",");
                }
                
                json.append("\n");
            }
            
            json.append("  ]\n}");
            System.out.println(json.toString());
        } else {
            // Generate text output
            if ("table".equals(format)) {
                System.out.println(StatisticsVisualizer.createTable(summaryStats));
            } else if ("dashboard".equals(format)) {
                displayDashboard();
            } else {
                System.out.println("Unsupported format: " + format + " for summary statistics.");
                System.out.println("Available formats: table, dashboard");
            }
            
            if (verbose) {
                System.out.println("\nStatistics last updated: " + 
                    (summaryStats.isEmpty() ? "never" : summaryStats.get(0).getFormattedTimestamp()));
            }
        }
    }
    
    /**
     * Displays all available statistics.
     */
    private void displayAllStats() {
        List<StatisticValue> allStats = statsService.getAllStatistics();
        
        if (allStats.isEmpty()) {
            if (isJsonOutput()) {
                System.out.println(
                    OutputFormatter.formatJsonMessage(
                        "warning", 
                        "No statistics available", 
                        Map.of("action", "Run 'rin stats refresh' to update statistics")
                    )
                );
            } else {
                System.out.println("No statistics available. Run 'rin stats refresh' to update statistics.");
            }
            return;
        }
        
        if (isJsonOutput()) {
            // Generate JSON output
            StringBuilder json = new StringBuilder();
            json.append("{\n  \"result\": \"success\",\n  \"type\": \"all\",\n  \"statistics\": [\n");
            
            for (int i = 0; i < allStats.size(); i++) {
                StatisticValue stat = allStats.get(i);
                json.append("    {\n")
                    .append("      \"type\": \"").append(stat.getType()).append("\",\n")
                    .append("      \"description\": \"").append(stat.getDescription()).append("\",\n")
                    .append("      \"value\": ").append(String.format("%.2f", stat.getValue())).append(",\n")
                    .append("      \"unit\": \"").append(stat.getUnit()).append("\"");
                
                if (!stat.getBreakdown().isEmpty()) {
                    json.append(",\n      \"breakdown\": {\n");
                    
                    int breakdownCount = 0;
                    for (Map.Entry<String, Double> entry : stat.getBreakdown().entrySet()) {
                        json.append("        \"").append(entry.getKey()).append("\": ")
                            .append(String.format("%.2f", entry.getValue()));
                        
                        if (breakdownCount < stat.getBreakdown().size() - 1) {
                            json.append(",\n");
                        } else {
                            json.append("\n");
                        }
                        breakdownCount++;
                    }
                    
                    json.append("      }");
                }
                
                json.append("\n    }");
                
                if (i < allStats.size() - 1) {
                    json.append(",");
                }
                
                json.append("\n");
            }
            
            json.append("  ]\n}");
            System.out.println(json.toString());
        } else {
            // Generate text output
            if ("table".equals(format)) {
                System.out.println(StatisticsVisualizer.createTable(allStats));
            } else {
                System.out.println("Unsupported format: " + format + " for all statistics.");
                System.out.println("Available format: table");
            }
            
            if (verbose) {
                System.out.println("\nStatistics last updated: " + 
                    (allStats.isEmpty() ? "never" : allStats.get(0).getFormattedTimestamp()));
                System.out.println("Total statistics count: " + allStats.size());
            }
        }
    }
    
    /**
     * Displays a dashboard with key metrics and visualizations.
     */
    private void displayDashboard() {
        StatisticsService statsService = StatisticsService.getInstance();
        List<StatisticValue> allStats = statsService.getAllStatistics();
        
        if (allStats.isEmpty()) {
            System.out.println("No statistics available. Run 'rin stats refresh' to update statistics.");
            return;
        }
        
        System.out.println(StatisticsVisualizer.createDashboard(allStats));
    }
    
    /**
     * Displays distribution statistics with bar charts.
     */
    private void displayDistribution() {
        // Get the distribution statistics
        List<StatisticValue> distributions = Arrays.asList(
            statsService.getStatistic(StatisticType.ITEMS_BY_TYPE),
            statsService.getStatistic(StatisticType.ITEMS_BY_STATE),
            statsService.getStatistic(StatisticType.ITEMS_BY_PRIORITY),
            statsService.getStatistic(StatisticType.ITEMS_BY_ASSIGNEE)
        );
        
        // Filter out null values
        distributions = distributions.stream()
            .filter(s -> s != null)
            .collect(Collectors.toList());
        
        if (distributions.isEmpty()) {
            if (isJsonOutput()) {
                System.out.println(
                    OutputFormatter.formatJsonMessage(
                        "warning", 
                        "No distribution statistics available", 
                        null
                    )
                );
            } else {
                System.out.println("No distribution statistics available.");
            }
            return;
        }
        
        if (isJsonOutput()) {
            // Generate JSON output
            StringBuilder json = new StringBuilder();
            json.append("{\n  \"result\": \"success\",\n  \"type\": \"distribution\",\n  \"distributions\": [\n");
            
            for (int i = 0; i < distributions.size(); i++) {
                StatisticValue stat = distributions.get(i);
                json.append("    {\n")
                    .append("      \"type\": \"").append(stat.getType()).append("\",\n")
                    .append("      \"description\": \"").append(stat.getDescription()).append("\",\n")
                    .append("      \"total\": ").append(String.format("%.0f", stat.getValue())).append(",\n");
                
                if (!stat.getBreakdown().isEmpty()) {
                    json.append("      \"breakdown\": {\n");
                    
                    // Apply limit if specified
                    Map<String, Double> breakdown = stat.getBreakdown();
                    List<Map.Entry<String, Double>> sortedBreakdown = new ArrayList<>(breakdown.entrySet());
                    sortedBreakdown.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
                    
                    // Apply limit if needed
                    if (limit > 0 && limit < sortedBreakdown.size()) {
                        sortedBreakdown = sortedBreakdown.subList(0, limit);
                    }
                    
                    for (int j = 0; j < sortedBreakdown.size(); j++) {
                        Map.Entry<String, Double> entry = sortedBreakdown.get(j);
                        json.append("        \"").append(entry.getKey()).append("\": ")
                            .append(String.format("%.0f", entry.getValue()));
                        
                        if (j < sortedBreakdown.size() - 1) {
                            json.append(",\n");
                        } else {
                            json.append("\n");
                        }
                    }
                    
                    json.append("      }");
                } else {
                    json.append("      \"breakdown\": {}");
                }
                
                json.append("\n    }");
                
                if (i < distributions.size() - 1) {
                    json.append(",");
                }
                
                json.append("\n");
            }
            
            json.append("  ]\n}");
            System.out.println(json.toString());
        } else {
            // Print each distribution as a bar chart
            for (StatisticValue stat : distributions) {
                System.out.println(StatisticsVisualizer.createBarChart(stat, limit));
                System.out.println();
            }
            
            if (verbose) {
                System.out.println("Total distribution categories: " + distributions.size());
                if (limit > 0) {
                    System.out.println("Output limited to " + limit + " entries per category");
                }
            }
        }
    }
    
    /**
     * Displays trend statistics over time.
     */
    private void displayTrend() {
        // For now, just display a message that this is not yet implemented
        System.out.println("Trend statistics are not yet implemented.");
        System.out.println("This feature will show how metrics change over time.");
    }
    
    /**
     * Displays detailed statistics for a specific aspect.
     */
    private void displayDetail() {
        if (filterArgs == null || filterArgs.length == 0) {
            System.err.println("Error: Detail type required.");
            System.err.println("Usage: rin stats detail [type]");
            System.err.println("Available types: completion, workflow, priority, assignments");
            return;
        }
        
        String detailType = filterArgs[0].toLowerCase();
        
        StatisticsService statsService = StatisticsService.getInstance();
        
        switch (detailType) {
            case "completion":
                displayCompletionDetails(statsService);
                break;
                
            case "workflow":
                displayWorkflowDetails(statsService);
                break;
                
            case "priority":
                displayPriorityDetails(statsService);
                break;
                
            case "assignments":
                displayAssignmentDetails(statsService);
                break;
                
            default:
                System.err.println("Error: Unknown detail type: " + detailType);
                System.err.println("Available types: completion, workflow, priority, assignments");
        }
    }
    
    /**
     * Displays details about completion metrics.
     */
    private void displayCompletionDetails(StatisticsService statsService) {
        // Get relevant completion statistics
        List<StatisticValue> completionStats = Arrays.asList(
            statsService.getStatistic(StatisticType.COMPLETION_RATE),
            statsService.getStatistic(StatisticType.ITEMS_COMPLETED),
            statsService.getStatistic(StatisticType.AVG_COMPLETION_TIME),
            statsService.getStatistic(StatisticType.LEAD_TIME),
            statsService.getStatistic(StatisticType.CYCLE_TIME)
        );
        
        // Filter out null values
        completionStats = completionStats.stream()
            .filter(s -> s != null)
            .collect(Collectors.toList());
        
        if (completionStats.isEmpty()) {
            System.out.println("No completion statistics available.");
            return;
        }
        
        System.out.println("COMPLETION METRICS");
        System.out.println("==================\n");
        System.out.println(StatisticsVisualizer.createTable(completionStats));
    }
    
    /**
     * Displays details about workflow metrics.
     */
    private void displayWorkflowDetails(StatisticsService statsService) {
        // Get workflow state distribution
        StatisticValue stateDistribution = statsService.getStatistic(StatisticType.ITEMS_BY_STATE);
        
        if (stateDistribution == null) {
            System.out.println("No workflow statistics available.");
            return;
        }
        
        System.out.println("WORKFLOW METRICS");
        System.out.println("================\n");
        
        // Print the state distribution
        System.out.println(StatisticsVisualizer.createBarChart(stateDistribution, 0));
        
        // Print related workflow metrics
        List<StatisticValue> workflowStats = Arrays.asList(
            statsService.getStatistic(StatisticType.WORK_IN_PROGRESS),
            statsService.getStatistic(StatisticType.THROUGHPUT),
            statsService.getStatistic(StatisticType.BURNDOWN_RATE)
        );
        
        // Filter out null values
        workflowStats = workflowStats.stream()
            .filter(s -> s != null)
            .collect(Collectors.toList());
        
        if (!workflowStats.isEmpty()) {
            System.out.println("\nKey Workflow Metrics:");
            System.out.println(StatisticsVisualizer.createTable(workflowStats));
        }
    }
    
    /**
     * Displays details about priority metrics.
     */
    private void displayPriorityDetails(StatisticsService statsService) {
        // Get priority distribution
        StatisticValue priorityDistribution = statsService.getStatistic(StatisticType.ITEMS_BY_PRIORITY);
        
        if (priorityDistribution == null) {
            System.out.println("No priority statistics available.");
            return;
        }
        
        System.out.println("PRIORITY METRICS");
        System.out.println("================\n");
        
        // Print the priority distribution
        System.out.println(StatisticsVisualizer.createBarChart(priorityDistribution, 0));
        
        // Print number of overdue items
        StatisticValue overdueItems = statsService.getStatistic(StatisticType.OVERDUE_ITEMS);
        if (overdueItems != null) {
            System.out.println("\nOverdue Items: " + (int) overdueItems.getValue());
        }
    }
    
    /**
     * Displays details about assignment metrics.
     */
    private void displayAssignmentDetails(StatisticsService statsService) {
        // Get assignee distribution
        StatisticValue assigneeDistribution = statsService.getStatistic(StatisticType.ITEMS_BY_ASSIGNEE);
        
        if (assigneeDistribution == null) {
            System.out.println("No assignment statistics available.");
            return;
        }
        
        System.out.println("ASSIGNMENT METRICS");
        System.out.println("==================\n");
        
        // Print the assignee distribution
        System.out.println(StatisticsVisualizer.createBarChart(assigneeDistribution, limit));
    }
    
    /**
     * Displays a specific statistic by type.
     * 
     * @param type the statistic type
     */
    private void displaySpecificStat(StatisticType type) {
        StatisticValue stat = statsService.getStatistic(type);
        
        if (stat == null) {
            if (isJsonOutput()) {
                System.out.println(
                    OutputFormatter.formatJsonMessage(
                        "warning", 
                        "No data available for statistic: " + type, 
                        null
                    )
                );
            } else {
                System.out.println("No data available for statistic: " + type);
            }
            return;
        }
        
        if (isJsonOutput()) {
            // Generate JSON output
            StringBuilder json = new StringBuilder();
            json.append("{\n")
                .append("  \"result\": \"success\",\n")
                .append("  \"statistic\": {\n")
                .append("    \"type\": \"").append(stat.getType()).append("\",\n")
                .append("    \"description\": \"").append(stat.getDescription()).append("\",\n")
                .append("    \"value\": ").append(String.format("%.2f", stat.getValue())).append(",\n")
                .append("    \"unit\": \"").append(stat.getUnit()).append("\"");
            
            if (!stat.getBreakdown().isEmpty()) {
                json.append(",\n    \"breakdown\": {\n");
                
                // Apply limit if specified
                Map<String, Double> breakdown = stat.getBreakdown();
                List<Map.Entry<String, Double>> sortedBreakdown = new ArrayList<>(breakdown.entrySet());
                sortedBreakdown.sort((e1, e2) -> Double.compare(e2.getValue(), e1.getValue()));
                
                // Apply limit if needed
                if (limit > 0 && limit < sortedBreakdown.size()) {
                    sortedBreakdown = sortedBreakdown.subList(0, limit);
                }
                
                for (int j = 0; j < sortedBreakdown.size(); j++) {
                    Map.Entry<String, Double> entry = sortedBreakdown.get(j);
                    json.append("      \"").append(entry.getKey()).append("\": ")
                        .append(String.format("%.0f", entry.getValue()));
                    
                    if (j < sortedBreakdown.size() - 1) {
                        json.append(",\n");
                    } else {
                        json.append("\n");
                    }
                }
                
                json.append("    }");
            }
            
            json.append("\n  },\n")
                .append("  \"timestamp\": \"").append(stat.getFormattedTimestamp()).append("\"\n")
                .append("}");
            
            System.out.println(json.toString());
        } else {
            // Text output
            System.out.println(type.name());
            System.out.println("-".repeat(type.name().length()));
            System.out.println();
            
            // Check if this is a distribution statistic with a breakdown
            if (!stat.getBreakdown().isEmpty()) {
                System.out.println(StatisticsVisualizer.createBarChart(stat, limit));
            } else {
                System.out.println(stat);
            }
            
            if (verbose) {
                System.out.println("\nStatistic updated: " + stat.getFormattedTimestamp());
            }
        }
    }
    
    /**
     * Displays help information for the stats command.
     */
    private void displayHelp() {
        if (isJsonOutput()) {
            // Display help in JSON format
            StringBuilder json = new StringBuilder();
            json.append("{\n")
                .append("  \"result\": \"success\",\n")
                .append("  \"command\": \"stats\",\n")
                .append("  \"usage\": \"rin stats [type] [options]\",\n")
                .append("  \"types\": [\n")
                .append("    { \"name\": \"summary\", \"description\": \"Show summary statistics (default)\" },\n")
                .append("    { \"name\": \"dashboard\", \"description\": \"Show a dashboard with key metrics and visualizations\" },\n")
                .append("    { \"name\": \"all\", \"description\": \"Show all available statistics\" },\n")
                .append("    { \"name\": \"distribution\", \"description\": \"Show item distributions\" },\n")
                .append("    { \"name\": \"detail\", \"description\": \"Show detailed statistics for a specific aspect\" },\n")
                .append("    { \"name\": \"refresh\", \"description\": \"Refresh the statistics cache\" },\n")
                .append("    { \"name\": \"help\", \"description\": \"Show this help information\" }\n")
                .append("  ],\n")
                .append("  \"statistic_types\": [\n");
            
            int i = 0;
            for (StatisticType type : StatisticType.values()) {
                json.append("    \"").append(type.name().toLowerCase()).append("\"");
                if (i < StatisticType.values().length - 1) {
                    json.append(",");
                }
                json.append("\n");
                i++;
            }
            
            json.append("  ],\n")
                .append("  \"options\": [\n")
                .append("    { \"name\": \"--format=<format>\", \"description\": \"Specify output format (table, dashboard)\" },\n")
                .append("    { \"name\": \"--limit=<n>\", \"description\": \"Limit output to top N items\" },\n")
                .append("    { \"name\": \"--json\", \"description\": \"Output in JSON format\" },\n")
                .append("    { \"name\": \"--verbose\", \"description\": \"Show verbose output with additional details\" }\n")
                .append("  ],\n")
                .append("  \"detail_types\": [\n")
                .append("    { \"name\": \"completion\", \"description\": \"Show completion metrics\" },\n")
                .append("    { \"name\": \"workflow\", \"description\": \"Show workflow metrics\" },\n")
                .append("    { \"name\": \"priority\", \"description\": \"Show priority metrics\" },\n")
                .append("    { \"name\": \"assignments\", \"description\": \"Show assignment metrics\" }\n")
                .append("  ],\n")
                .append("  \"examples\": [\n")
                .append("    { \"command\": \"rin stats\", \"description\": \"Show summary statistics\" },\n")
                .append("    { \"command\": \"rin stats dashboard\", \"description\": \"Show statistics dashboard\" },\n")
                .append("    { \"command\": \"rin stats items_by_type\", \"description\": \"Show items by type\" },\n")
                .append("    { \"command\": \"rin stats distribution --limit=5\", \"description\": \"Show top 5 items in distributions\" },\n")
                .append("    { \"command\": \"rin stats --json\", \"description\": \"Output summary statistics in JSON format\" }\n")
                .append("  ]\n")
                .append("}");
                
            System.out.println(json.toString());
        } else {
            // Display help in text format
            System.out.println("Statistics Command Usage:");
            System.out.println("rin stats [type] [options]");
            System.out.println();
            System.out.println("Types:");
            System.out.println("  summary    - Show summary statistics (default)");
            System.out.println("  dashboard  - Show a dashboard with key metrics and visualizations");
            System.out.println("  all        - Show all available statistics");
            System.out.println("  distribution - Show item distributions");
            System.out.println("  detail     - Show detailed statistics for a specific aspect");
            System.out.println("  refresh    - Refresh the statistics cache");
            System.out.println("  help       - Show this help information");
            System.out.println();
            System.out.println("You can also specify a specific statistic type directly:");
            for (StatisticType type : StatisticType.values()) {
                System.out.println("  " + type.name().toLowerCase());
            }
            System.out.println();
            System.out.println("Options:");
            System.out.println("  --format=<format>  - Specify output format (table, dashboard)");
            System.out.println("  --limit=<n>        - Limit output to top N items");
            System.out.println("  --json             - Output in JSON format");
            System.out.println("  --verbose          - Show verbose output with additional details");
            System.out.println();
            System.out.println("Detail Types:");
            System.out.println("  rin stats detail completion  - Show completion metrics");
            System.out.println("  rin stats detail workflow    - Show workflow metrics");
            System.out.println("  rin stats detail priority    - Show priority metrics");
            System.out.println("  rin stats detail assignments - Show assignment metrics");
            System.out.println();
            System.out.println("Examples:");
            System.out.println("  rin stats                    - Show summary statistics");
            System.out.println("  rin stats dashboard          - Show statistics dashboard");
            System.out.println("  rin stats items_by_type      - Show items by type");
            System.out.println("  rin stats distribution --limit=5  - Show top 5 items in distributions");
            System.out.println("  rin stats --json             - Output summary statistics in JSON format");
        }
    }
}