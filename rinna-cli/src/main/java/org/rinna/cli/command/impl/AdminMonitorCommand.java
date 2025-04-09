/*
 * Administrative monitoring command handler for Rinna.
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.command.impl;

import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.MonitoringService;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.util.OutputFormatter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.Callable;

/**
 * Command handler for system monitoring operations.
 * This class implements the functionality for the 'rin admin monitor' command.
 * It follows the ViewCommand pattern with MetadataService integration for operation tracking.
 */
public class AdminMonitorCommand implements Callable<Integer> {
    
    private String operation;
    private String[] args = new String[0];
    private final ServiceManager serviceManager;
    private final Scanner scanner;
    private final MetadataService metadataService;
    private String format = "text";
    private boolean verbose = false;
    
    /**
     * Creates a new AdminMonitorCommand with the specified ServiceManager.
     * 
     * @param serviceManager the service manager
     */
    public AdminMonitorCommand(ServiceManager serviceManager) {
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
    
    /**
     * Sets the arguments for the operation.
     * 
     * @param args the arguments
     */
    public void setArgs(String[] args) {
        this.args = args;
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
        String operationId = metadataService.startOperation("admin-monitor", "MONITOR", params);
        
        try {
            if (operation == null || operation.isEmpty()) {
                displayHelp(operationId);
                return 1;
            }
            
            // Get the monitoring service
            MonitoringService monitoringService = serviceManager.getMonitoringService();
            if (monitoringService == null) {
                String errorMessage = "Monitoring service is not available.";
                outputError(errorMessage);
                metadataService.failOperation(operationId, new IllegalStateException(errorMessage));
                return 1;
            }
            
            // Delegate to the appropriate operation
            int result;
            switch (operation) {
                case "dashboard":
                    result = handleDashboardOperation(monitoringService, operationId);
                    break;
                
                case "server":
                    result = handleServerOperation(monitoringService, operationId);
                    break;
                
                case "configure":
                    result = handleConfigureOperation(monitoringService, operationId);
                    break;
                
                case "report":
                    result = handleReportOperation(monitoringService, operationId);
                    break;
                
                case "alerts":
                    result = handleAlertsOperation(monitoringService, operationId);
                    break;
                
                case "sessions":
                    result = handleSessionsOperation(monitoringService, operationId);
                    break;
                
                case "thresholds":
                    result = handleThresholdsOperation(monitoringService, operationId);
                    break;
                
                case "help":
                    displayHelp(operationId);
                    result = 0;
                    break;
                
                default:
                    String errorMessage = "Unknown monitoring operation: " + operation;
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
     * Handles the 'dashboard' operation to display system health dashboard.
     * 
     * @param monitoringService the monitoring service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleDashboardOperation(MonitoringService monitoringService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "dashboard");
        params.put("format", format);
        
        // Start tracking sub-operation
        String dashboardOperationId = metadataService.trackOperation("admin-monitor-dashboard", params);
        
        try {
            String dashboard = monitoringService.getDashboard();
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "dashboard");
                resultData.put("dashboard", dashboard);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println(dashboard);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            metadataService.completeOperation(dashboardOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error displaying dashboard: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(dashboardOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'server' operation to display detailed server metrics.
     * 
     * @param monitoringService the monitoring service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleServerOperation(MonitoringService monitoringService, String parentOperationId) {
        // Parse options from arguments
        Map<String, String> options = parseOptions(args);
        boolean detailed = options.containsKey("detailed");
        
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "server");
        params.put("detailed", detailed);
        params.put("format", format);
        
        // Start tracking sub-operation
        String serverOperationId = metadataService.trackOperation("admin-monitor-server", params);
        
        try {
            String metrics = monitoringService.getServerMetrics(detailed);
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "server");
                resultData.put("detailed", detailed);
                resultData.put("metrics", metrics);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println(metrics);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("detailed", detailed);
            metadataService.completeOperation(serverOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error getting server metrics: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(serverOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'configure' operation to configure monitoring settings.
     * 
     * @param monitoringService the monitoring service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleConfigureOperation(MonitoringService monitoringService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "configure");
        params.put("format", format);
        
        // Start tracking sub-operation
        String configureOperationId = metadataService.trackOperation("admin-monitor-configure", params);
        
        try {
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> thresholdData = new HashMap<>();
                thresholdData.put("CPU Load", "85%");
                thresholdData.put("Memory Usage", "90%");
                thresholdData.put("Disk Usage", "85%");
                thresholdData.put("Network Connections", "1000");
                thresholdData.put("Response Time", "500ms");
                thresholdData.put("Error Rate", "1%");
                thresholdData.put("Refresh Interval", "60s");
                
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "configure");
                resultData.put("message", "Select threshold to configure");
                resultData.put("thresholds", thresholdData);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
                
                // Can't continue in JSON mode without interactive input
                metadataService.completeOperation(configureOperationId, resultData);
                return 0;
            } else {
                System.out.println("Monitoring Configuration");
                System.out.println("=======================");
                System.out.println();
                
                System.out.println("Select threshold to configure:");
                System.out.println("1. CPU Load (current: 85%)");
                System.out.println("2. Memory Usage (current: 90%)");
                System.out.println("3. Disk Usage (current: 85%)");
                System.out.println("4. Network Connections (current: 1000)");
                System.out.println("5. Response Time (current: 500ms)");
                System.out.println("6. Error Rate (current: 1%)");
                System.out.println("7. Refresh Interval (current: 60s)");
                System.out.print("Enter the number of the threshold to configure: ");
            }
            
            String thresholdInput = scanner.nextLine().trim();
            String metric;
            String currentValue;
            
            switch (thresholdInput) {
                case "1":
                    metric = "CPU Load";
                    currentValue = "85";
                    break;
                case "2":
                    metric = "Memory Usage";
                    currentValue = "90";
                    break;
                case "3":
                    metric = "Disk Usage";
                    currentValue = "85";
                    break;
                case "4":
                    metric = "Network Connections";
                    currentValue = "1000";
                    break;
                case "5":
                    metric = "Response Time";
                    currentValue = "500";
                    break;
                case "6":
                    metric = "Error Rate";
                    currentValue = "1";
                    break;
                case "7":
                    metric = "Refresh Interval";
                    currentValue = "60";
                    break;
                default:
                    String errorMessage = "Invalid selection.";
                    outputError(errorMessage);
                    metadataService.failOperation(configureOperationId, new IllegalArgumentException(errorMessage));
                    return 1;
            }
            
            // Update operation tracking with selected metric
            Map<String, Object> metricParams = new HashMap<>();
            metricParams.put("metric", metric);
            metricParams.put("currentValue", currentValue);
            metadataService.trackOperationWithData("admin-monitor-configure-select", metricParams);
            
            System.out.print("Enter new threshold value for " + metric + " [" + currentValue + "]: ");
            String newValue = scanner.nextLine().trim();
            
            if (newValue.isEmpty()) {
                newValue = currentValue;
            }
            
            // Update operation tracking with new value
            Map<String, Object> updateParams = new HashMap<>();
            updateParams.put("metric", metric);
            updateParams.put("oldValue", currentValue);
            updateParams.put("newValue", newValue);
            String updateOperationId = metadataService.trackOperation("admin-monitor-configure-update", updateParams);
            
            try {
                boolean success = monitoringService.configureThreshold(metric, newValue);
                if (success) {
                    String successMessage = "Monitoring threshold for " + metric + " updated to " + newValue + 
                        (metric.equals("Response Time") ? "ms" : 
                         metric.equals("Refresh Interval") ? "s" : 
                         metric.equals("Network Connections") ? "" : "%");
                    
                    outputSuccess(successMessage);
                    
                    // Track successful update
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("success", true);
                    resultData.put("metric", metric);
                    resultData.put("oldValue", currentValue);
                    resultData.put("newValue", newValue);
                    metadataService.completeOperation(updateOperationId, resultData);
                    metadataService.completeOperation(configureOperationId, resultData);
                    
                    return 0;
                } else {
                    String errorMessage = "Failed to update monitoring threshold.";
                    outputError(errorMessage);
                    metadataService.failOperation(updateOperationId, new RuntimeException(errorMessage));
                    metadataService.failOperation(configureOperationId, new RuntimeException(errorMessage));
                    return 1;
                }
            } catch (Exception e) {
                String errorMessage = "Error configuring threshold: " + e.getMessage();
                outputError(errorMessage);
                if (verbose) {
                    e.printStackTrace();
                }
                metadataService.failOperation(updateOperationId, e);
                metadataService.failOperation(configureOperationId, e);
                return 1;
            }
        } catch (Exception e) {
            String errorMessage = "Error in configure operation: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(configureOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'report' operation to generate system performance reports.
     * 
     * @param monitoringService the monitoring service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleReportOperation(MonitoringService monitoringService, String parentOperationId) {
        // Parse options from arguments
        Map<String, String> options = parseOptions(args);
        String period = options.getOrDefault("period", "daily");
        
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "report");
        params.put("period", period);
        params.put("format", format);
        
        // Start tracking sub-operation
        String reportOperationId = metadataService.trackOperation("admin-monitor-report", params);
        
        if (!Arrays.asList("hourly", "daily", "weekly", "monthly").contains(period)) {
            String errorMessage = "Invalid period. Must be one of: hourly, daily, weekly, monthly.";
            outputError(errorMessage);
            metadataService.failOperation(reportOperationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        try {
            String report = monitoringService.generateReport(period);
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "report");
                resultData.put("period", period);
                resultData.put("report", report);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println(report);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            resultData.put("period", period);
            metadataService.completeOperation(reportOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error generating report: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(reportOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'alerts' operation to manage monitoring alerts.
     * 
     * @param monitoringService the monitoring service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleAlertsOperation(MonitoringService monitoringService, String parentOperationId) {
        if (args.length == 0) {
            String errorMessage = "Missing alerts subcommand. Use 'add', 'list', or 'remove'.";
            outputError(errorMessage);
            metadataService.trackOperationError(parentOperationId, "alerts", errorMessage, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        String alertsOperation = args[0];
        
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "alerts");
        params.put("alertsOperation", alertsOperation);
        params.put("format", format);
        
        // Start tracking sub-operation
        String alertsOperationId = metadataService.trackOperation("admin-monitor-alerts", params);
        
        if ("add".equals(alertsOperation)) {
            return handleAddAlertOperation(monitoringService, alertsOperationId);
        } else if ("list".equals(alertsOperation)) {
            return handleListAlertsOperation(monitoringService, alertsOperationId);
        } else if ("remove".equals(alertsOperation) || "delete".equals(alertsOperation)) {
            return handleRemoveAlertOperation(monitoringService, alertsOperationId);
        } else {
            String errorMessage = "Unknown alerts operation: " + alertsOperation;
            outputError(errorMessage);
            System.out.println("Valid operations: add, list, remove");
            metadataService.failOperation(alertsOperationId, new IllegalArgumentException(errorMessage));
            return 1;
        }
    }
    
    /**
     * Handles the 'alerts add' operation to add a new monitoring alert.
     * 
     * @param monitoringService the monitoring service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleAddAlertOperation(MonitoringService monitoringService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "alerts-add");
        params.put("format", format);
        
        // Start tracking sub-operation
        String addAlertOperationId = metadataService.trackOperation("admin-monitor-alerts-add", params);
        
        try {
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> metricsData = new HashMap<>();
                metricsData.put("1", "CPU Load");
                metricsData.put("2", "Memory Usage");
                metricsData.put("3", "Disk Usage");
                metricsData.put("4", "Network Connections");
                metricsData.put("5", "Response Time");
                metricsData.put("6", "Error Rate");
                
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "alerts-add");
                resultData.put("message", "Input required for creating alert");
                resultData.put("metrics", metricsData);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
                
                // Can't continue in JSON mode without interactive input
                metadataService.completeOperation(addAlertOperationId, resultData);
                return 0;
            } else {
                System.out.println("Create Monitoring Alert");
                System.out.println("=====================");
            }
            
            System.out.print("Enter alert name: ");
            String name = scanner.nextLine().trim();
            
            if (name.isEmpty()) {
                String errorMessage = "Alert name cannot be empty.";
                outputError(errorMessage);
                metadataService.failOperation(addAlertOperationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            // Update operation parameters with name
            Map<String, Object> nameParams = new HashMap<>();
            nameParams.put("name", name);
            metadataService.trackOperationWithData("admin-monitor-alerts-add-name", nameParams);
            
            System.out.println("Select metric:");
            System.out.println("1. CPU Load");
            System.out.println("2. Memory Usage");
            System.out.println("3. Disk Usage");
            System.out.println("4. Network Connections");
            System.out.println("5. Response Time");
            System.out.println("6. Error Rate");
            System.out.print("Enter choice: ");
            
            String metricInput = scanner.nextLine().trim();
            String metric;
            
            switch (metricInput) {
                case "1":
                    metric = "CPU Load";
                    break;
                case "2":
                    metric = "Memory Usage";
                    break;
                case "3":
                    metric = "Disk Usage";
                    break;
                case "4":
                    metric = "Network Connections";
                    break;
                case "5":
                    metric = "Response Time";
                    break;
                case "6":
                    metric = "Error Rate";
                    break;
                default:
                    String errorMessage = "Invalid selection.";
                    outputError(errorMessage);
                    metadataService.failOperation(addAlertOperationId, new IllegalArgumentException(errorMessage));
                    return 1;
            }
            
            // Update operation parameters with metric
            Map<String, Object> metricParams = new HashMap<>();
            metricParams.put("metric", metric);
            metadataService.trackOperationWithData("admin-monitor-alerts-add-metric", metricParams);
            
            System.out.print("Enter threshold value: ");
            String threshold = scanner.nextLine().trim();
            
            if (threshold.isEmpty()) {
                String errorMessage = "Threshold cannot be empty.";
                outputError(errorMessage);
                metadataService.failOperation(addAlertOperationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            // Update operation parameters with threshold
            Map<String, Object> thresholdParams = new HashMap<>();
            thresholdParams.put("threshold", threshold);
            metadataService.trackOperationWithData("admin-monitor-alerts-add-threshold", thresholdParams);
            
            System.out.print("Enter notification recipients (comma-separated email addresses): ");
            String recipientsInput = scanner.nextLine().trim();
            
            if (recipientsInput.isEmpty()) {
                String errorMessage = "Recipients cannot be empty.";
                outputError(errorMessage);
                metadataService.failOperation(addAlertOperationId, new IllegalArgumentException(errorMessage));
                return 1;
            }
            
            List<String> recipients = Arrays.asList(recipientsInput.split("\\s*,\\s*"));
            
            // Update operation parameters with recipients
            Map<String, Object> recipientsParams = new HashMap<>();
            recipientsParams.put("recipients", recipients);
            metadataService.trackOperationWithData("admin-monitor-alerts-add-recipients", recipientsParams);
            
            // Execute alert creation with tracking
            Map<String, Object> createParams = new HashMap<>();
            createParams.put("name", name);
            createParams.put("metric", metric);
            createParams.put("threshold", threshold);
            createParams.put("recipients", recipients);
            String createOperationId = metadataService.trackOperation("admin-monitor-alerts-add-create", createParams);
            
            try {
                boolean success = monitoringService.addAlert(name, metric, threshold, recipients);
                if (success) {
                    String successMessage = "Monitoring alert '" + name + "' created successfully.";
                    outputSuccess(successMessage);
                    
                    // Track successful creation
                    Map<String, Object> resultData = new HashMap<>();
                    resultData.put("success", true);
                    resultData.put("name", name);
                    resultData.put("metric", metric);
                    resultData.put("threshold", threshold);
                    resultData.put("recipients", recipients);
                    metadataService.completeOperation(createOperationId, resultData);
                    metadataService.completeOperation(addAlertOperationId, resultData);
                    
                    return 0;
                } else {
                    String errorMessage = "Failed to create monitoring alert.";
                    outputError(errorMessage);
                    metadataService.failOperation(createOperationId, new RuntimeException(errorMessage));
                    metadataService.failOperation(addAlertOperationId, new RuntimeException(errorMessage));
                    return 1;
                }
            } catch (Exception e) {
                String errorMessage = "Error creating monitoring alert: " + e.getMessage();
                outputError(errorMessage);
                if (verbose) {
                    e.printStackTrace();
                }
                metadataService.failOperation(createOperationId, e);
                metadataService.failOperation(addAlertOperationId, e);
                return 1;
            }
        } catch (Exception e) {
            String errorMessage = "Error in add alert operation: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(addAlertOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'alerts list' operation to list monitoring alerts.
     * 
     * @param monitoringService the monitoring service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleListAlertsOperation(MonitoringService monitoringService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "alerts-list");
        params.put("format", format);
        
        // Start tracking sub-operation
        String listAlertsOperationId = metadataService.trackOperation("admin-monitor-alerts-list", params);
        
        try {
            String alerts = monitoringService.listAlerts();
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "alerts-list");
                resultData.put("alerts", alerts);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println(alerts);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            metadataService.completeOperation(listAlertsOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error listing monitoring alerts: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(listAlertsOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'alerts remove' operation to remove a monitoring alert.
     * 
     * @param monitoringService the monitoring service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleRemoveAlertOperation(MonitoringService monitoringService, String parentOperationId) {
        if (args.length < 2) {
            String errorMessage = "Missing alert name to remove.";
            outputError(errorMessage);
            metadataService.trackOperationError(parentOperationId, "alerts-remove", errorMessage, new IllegalArgumentException(errorMessage));
            return 1;
        }
        
        String alertName = args[1];
        
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "alerts-remove");
        params.put("alertName", alertName);
        params.put("format", format);
        
        // Start tracking sub-operation
        String removeAlertOperationId = metadataService.trackOperation("admin-monitor-alerts-remove", params);
        
        try {
            boolean success = monitoringService.removeAlert(alertName);
            if (success) {
                String successMessage = "Monitoring alert '" + alertName + "' removed successfully.";
                outputSuccess(successMessage);
                
                // Track successful removal
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("success", true);
                resultData.put("alertName", alertName);
                metadataService.completeOperation(removeAlertOperationId, resultData);
                
                return 0;
            } else {
                String errorMessage = "Failed to remove monitoring alert. Does it exist?";
                outputError(errorMessage);
                metadataService.failOperation(removeAlertOperationId, new RuntimeException(errorMessage));
                return 1;
            }
        } catch (Exception e) {
            String errorMessage = "Error removing monitoring alert: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(removeAlertOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'sessions' operation to display active user sessions.
     * 
     * @param monitoringService the monitoring service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleSessionsOperation(MonitoringService monitoringService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "sessions");
        params.put("format", format);
        
        // Start tracking sub-operation
        String sessionsOperationId = metadataService.trackOperation("admin-monitor-sessions", params);
        
        try {
            String sessions = monitoringService.getActiveSessions();
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "sessions");
                resultData.put("sessions", sessions);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println(sessions);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            metadataService.completeOperation(sessionsOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error getting active sessions: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(sessionsOperationId, e);
            return 1;
        }
    }
    
    /**
     * Handles the 'thresholds' operation to display monitoring thresholds.
     * 
     * @param monitoringService the monitoring service
     * @param parentOperationId the parent operation ID for tracking
     * @return the exit code
     */
    private int handleThresholdsOperation(MonitoringService monitoringService, String parentOperationId) {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("operation", "thresholds");
        params.put("format", format);
        
        // Start tracking sub-operation
        String thresholdsOperationId = metadataService.trackOperation("admin-monitor-thresholds", params);
        
        try {
            String thresholds = monitoringService.getThresholds();
            
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> resultData = new HashMap<>();
                resultData.put("result", "success");
                resultData.put("operation", "thresholds");
                resultData.put("thresholds", thresholds);
                
                System.out.println(OutputFormatter.toJson(resultData, verbose));
            } else {
                System.out.println(thresholds);
            }
            
            // Track operation success
            Map<String, Object> resultData = new HashMap<>();
            resultData.put("success", true);
            metadataService.completeOperation(thresholdsOperationId, resultData);
            
            return 0;
        } catch (Exception e) {
            String errorMessage = "Error getting monitoring thresholds: " + e.getMessage();
            outputError(errorMessage);
            if (verbose) {
                e.printStackTrace();
            }
            metadataService.failOperation(thresholdsOperationId, e);
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
     * Displays help information for monitoring commands.
     * 
     * @param operationId the parent operation ID for tracking
     */
    private void displayHelp(String operationId) {
        // Create operation parameters for tracking
        Map<String, Object> helpData = new HashMap<>();
        helpData.put("command", "admin monitor");
        helpData.put("action", "help");
        helpData.put("format", format);
        
        // Start tracking sub-operation
        String helpOperationId = metadataService.trackOperation("admin-monitor-help", helpData);
        
        try {
            if ("json".equalsIgnoreCase(format)) {
                Map<String, Object> jsonHelpData = new HashMap<>();
                jsonHelpData.put("result", "success");
                jsonHelpData.put("command", "admin monitor");
                jsonHelpData.put("usage", "rin admin monitor <operation> [options]");
                
                List<Map<String, String>> operations = new ArrayList<>();
                operations.add(createInfoMap("dashboard", "View system health dashboard"));
                operations.add(createInfoMap("server", "View detailed server metrics"));
                operations.add(createInfoMap("configure", "Configure monitoring thresholds"));
                operations.add(createInfoMap("report", "Generate system performance report"));
                operations.add(createInfoMap("alerts", "Manage monitoring alerts"));
                operations.add(createInfoMap("sessions", "View active user sessions"));
                operations.add(createInfoMap("thresholds", "View monitoring thresholds"));
                jsonHelpData.put("operations", operations);
                
                Map<String, List<Map<String, String>>> operationOptions = new HashMap<>();
                
                List<Map<String, String>> serverOptions = new ArrayList<>();
                serverOptions.add(createInfoMap("--detailed", "Show detailed metrics"));
                operationOptions.put("server", serverOptions);
                
                List<Map<String, String>> reportOptions = new ArrayList<>();
                reportOptions.add(createInfoMap("--period=<period>", "Report period (hourly, daily, weekly, monthly)"));
                operationOptions.put("report", reportOptions);
                
                jsonHelpData.put("operation_options", operationOptions);
                
                System.out.println(OutputFormatter.toJson(jsonHelpData, verbose));
            } else {
                System.out.println("Usage: rin admin monitor <operation> [options]");
                System.out.println();
                System.out.println("Operations:");
                System.out.println("  dashboard  - View system health dashboard");
                System.out.println("  server     - View detailed server metrics");
                System.out.println("  configure  - Configure monitoring thresholds");
                System.out.println("  report     - Generate system performance report");
                System.out.println("  alerts     - Manage monitoring alerts");
                System.out.println("  sessions   - View active user sessions");
                System.out.println("  thresholds - View monitoring thresholds");
                System.out.println();
                System.out.println("Options for 'server':");
                System.out.println("  --detailed        - Show detailed metrics");
                System.out.println();
                System.out.println("Options for 'report':");
                System.out.println("  --period=<period> - Report period (hourly, daily, weekly, monthly)");
                System.out.println();
                System.out.println("For detailed help on a specific operation, use:");
                System.out.println("  rin admin monitor <operation> help");
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
}