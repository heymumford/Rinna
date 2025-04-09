package org.rinna.cli.command;

import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.util.OutputFormatter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Command to manage Rinna services.
 * Follows the ViewCommand pattern with proper MetadataService integration
 * for tracking service management operations.
 */
public class ServerCommand implements Callable<Integer> {
    
    private String subcommand;
    private String serviceName;
    private String configPath;
    private String format = "text";
    private boolean verbose = false;
    private String username;
    
    // Services
    private final ServiceManager serviceManager;
    private final ConfigurationService configService;
    private final MetadataService metadataService;
    private final ContextManager contextManager;
    
    /**
     * Default constructor using singleton service manager.
     */
    public ServerCommand() {
        this(ServiceManager.getInstance());
    }
    
    /**
     * Constructor with service manager for dependency injection.
     * 
     * @param serviceManager the service manager
     */
    public ServerCommand(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
        this.configService = serviceManager.getConfigurationService();
        this.metadataService = serviceManager.getMetadataService();
        this.contextManager = ContextManager.getInstance();
        
        // Get current user from configuration
        this.username = configService.getCurrentUser();
        if (this.username == null || this.username.isEmpty()) {
            this.username = System.getProperty("user.name");
        }
    }
    
    /**
     * Sets the output format.
     * 
     * @param format the output format ("text" or "json")
     * @return this command instance for method chaining
     */
    public ServerCommand setFormat(String format) {
        this.format = format;
        return this;
    }
    
    /**
     * Sets the JSON output flag (for backward compatibility).
     * 
     * @param jsonOutput true to output in JSON format, false for text
     * @return this command instance for method chaining
     */
    public ServerCommand setJsonOutput(boolean jsonOutput) {
        this.format = jsonOutput ? "json" : "text";
        return this;
    }
    
    /**
     * Sets the verbose output flag.
     * 
     * @param verbose true for verbose output, false for normal output
     * @return this command instance for method chaining
     */
    public ServerCommand setVerbose(boolean verbose) {
        this.verbose = verbose;
        return this;
    }
    
    /**
     * Sets the username.
     * 
     * @param username the username
     * @return this command instance for method chaining
     */
    public ServerCommand setUsername(String username) {
        this.username = username;
        return this;
    }
    
    @Override
    public Integer call() {
        // Create operation parameters for tracking
        Map<String, Object> params = new HashMap<>();
        params.put("username", username);
        params.put("format", format);
        params.put("verbose", verbose);
        params.put("subcommand", subcommand);
        if (serviceName != null) {
            params.put("serviceName", serviceName);
        }
        if (configPath != null) {
            params.put("configPath", configPath);
        }
        
        // Start tracking main operation
        String operationId = metadataService.startOperation("server-command", "MANAGE", params);
        
        try {
            if (subcommand == null || subcommand.isEmpty()) {
                // Default subcommand is to show status
                String statusOpId = metadataService.startOperation(
                    "server-status", "READ", 
                    Map.of("username", username, "allServices", true)
                );
                
                // Display status and track result
                boolean success = showStatus();
                
                Map<String, Object> statusResult = new HashMap<>();
                statusResult.put("success", success);
                metadataService.completeOperation(statusOpId, statusResult);
                
                // Complete main operation
                metadataService.completeOperation(operationId, 
                    Map.of("action", "status", "result", success ? "success" : "error"));
                
                return success ? 0 : 1;
            }
            
            // Handle subcommands
            boolean success = false;
            switch (subcommand.toLowerCase()) {
                case "status":
                    String statusOpId = metadataService.startOperation(
                        "server-status", "READ", 
                        Map.of(
                            "username", username, 
                            "serviceName", serviceName != null ? serviceName : "all"
                        )
                    );
                    
                    success = showStatus();
                    
                    Map<String, Object> statusResult = new HashMap<>();
                    statusResult.put("success", success);
                    metadataService.completeOperation(statusOpId, statusResult);
                    break;
                    
                case "start":
                    String startOpId = metadataService.startOperation(
                        "server-start", "EXECUTE", 
                        Map.of(
                            "username", username, 
                            "serviceName", serviceName != null ? serviceName : "unknown"
                        )
                    );
                    
                    success = startService(startOpId);
                    
                    // Operation completion is handled in startService
                    break;
                    
                case "stop":
                    String stopOpId = metadataService.startOperation(
                        "server-stop", "EXECUTE", 
                        Map.of(
                            "username", username, 
                            "serviceName", serviceName != null ? serviceName : "unknown"
                        )
                    );
                    
                    success = stopService(stopOpId);
                    
                    // Operation completion is handled in stopService
                    break;
                    
                case "restart":
                    String restartOpId = metadataService.startOperation(
                        "server-restart", "EXECUTE", 
                        Map.of(
                            "username", username, 
                            "serviceName", serviceName != null ? serviceName : "unknown"
                        )
                    );
                    
                    success = restartService(restartOpId);
                    
                    // Operation completion is handled in restartService
                    break;
                    
                case "config":
                    String configOpId = metadataService.startOperation(
                        "server-config", "CREATE", 
                        Map.of(
                            "username", username, 
                            "serviceName", serviceName != null ? serviceName : "unknown",
                            "configPath", configPath != null ? configPath : "default"
                        )
                    );
                    
                    success = configureService(configOpId);
                    
                    // Operation completion is handled in configureService
                    break;
                    
                case "help":
                    String helpOpId = metadataService.startOperation(
                        "server-help", "READ", 
                        Map.of("username", username)
                    );
                    
                    showHelp();
                    success = true;
                    
                    metadataService.completeOperation(helpOpId, Map.of("success", true));
                    break;
                    
                default:
                    displayError("Unknown server subcommand: " + subcommand, 
                                "Valid subcommands: status, start, stop, restart, config, help");
                    
                    metadataService.failOperation(operationId, 
                        new IllegalArgumentException("Unknown server subcommand: " + subcommand));
                    
                    return 1;
            }
            
            // Complete main operation
            Map<String, Object> result = new HashMap<>();
            result.put("action", subcommand);
            result.put("result", success ? "success" : "error");
            result.put("serviceName", serviceName);
            
            metadataService.completeOperation(operationId, result);
            
            return success ? 0 : 1;
        } catch (Exception e) {
            displayError("Error: " + e.getMessage(), null);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            metadataService.failOperation(operationId, e);
            return 1;
        }
    }
    
    /**
     * Show status for services.
     * 
     * @return true if successful, false otherwise
     */
    private boolean showStatus() {
        try {
            if (serviceName != null && !serviceName.isEmpty()) {
                // Show status for a specific service
                ServiceManager.ServiceStatusInfo status = serviceManager.getServiceStatus(serviceName);
                
                if ("json".equalsIgnoreCase(format)) {
                    displayStatusAsJson(serviceName, status);
                } else {
                    displayStatusAsText(serviceName, status);
                }
            } else {
                // Show status for all services
                // Define the known services
                String[] knownServices = {"api", "database", "docs"};
                
                // Get the status of each service
                Map<String, ServiceManager.ServiceStatusInfo> serviceStatuses = new HashMap<>();
                int runningCount = 0;
                int stoppedCount = 0;
                
                for (String service : knownServices) {
                    ServiceManager.ServiceStatusInfo status = serviceManager.getServiceStatus(service);
                    serviceStatuses.put(service, status);
                    
                    if (status.isAvailable() && "RUNNING".equals(status.getState())) {
                        runningCount++;
                    } else {
                        stoppedCount++;
                    }
                }
                
                // Prepare stats
                Map<String, Object> stats = new HashMap<>();
                stats.put("total", serviceStatuses.size());
                stats.put("running", runningCount);
                stats.put("stopped", stoppedCount);
                
                if ("json".equalsIgnoreCase(format)) {
                    displayMultiStatusAsJson(serviceStatuses, stats);
                } else {
                    displayMultiStatusAsText(serviceStatuses, stats);
                }
            }
            return true;
        } catch (Exception e) {
            displayError("Error getting service status: " + e.getMessage(), null);
            if (verbose) {
                e.printStackTrace();
            }
            return false;
        }
    }
    
    /**
     * Display status for a single service in text format.
     */
    private void displayStatusAsText(String serviceName, ServiceManager.ServiceStatusInfo status) {
        System.out.printf("Service: %s%n", serviceName);
        System.out.printf("Status: %s%n", status.getState());
        System.out.printf("Available: %s%n", status.isAvailable() ? "Yes" : "No");
        System.out.printf("Message: %s%n", status.getMessage());
        
        if (verbose) {
            // Add additional information if available
            System.out.println("\nDetailed information:");
            System.out.println(status.toString());
        }
    }
    
    /**
     * Display status for a single service in JSON format.
     */
    private void displayStatusAsJson(String serviceName, ServiceManager.ServiceStatusInfo status) {
        Map<String, Object> response = new HashMap<>();
        response.put("result", "success");
        response.put("action", "status");
        
        Map<String, Object> serviceData = new HashMap<>();
        serviceData.put("name", serviceName);
        serviceData.put("status", status.getState());
        serviceData.put("available", status.isAvailable());
        serviceData.put("message", status.getMessage());
        
        response.put("service", serviceData);
        
        System.out.println(OutputFormatter.toJson(response));
    }
    
    /**
     * Display status for multiple services in text format.
     */
    private void displayMultiStatusAsText(Map<String, ServiceManager.ServiceStatusInfo> serviceStatuses, Map<String, Object> stats) {
        System.out.println("Rinna Services Status:");
        System.out.println("--------------------------------------------------------------------------------");
        System.out.printf("%-20s %-15s %-10s %-30s%n", 
                "SERVICE", "STATUS", "AVAILABLE", "MESSAGE");
        System.out.println("--------------------------------------------------------------------------------");
        
        for (Map.Entry<String, ServiceManager.ServiceStatusInfo> entry : serviceStatuses.entrySet()) {
            String service = entry.getKey();
            ServiceManager.ServiceStatusInfo status = entry.getValue();
            
            System.out.printf("%-20s %-15s %-10s %-30s%n", 
                    service, 
                    status.getState(), 
                    status.isAvailable() ? "Yes" : "No", 
                    truncateIfNeeded(status.getMessage(), 30));
        }
        
        System.out.println("--------------------------------------------------------------------------------");
        
        if (verbose) {
            System.out.println("\nTotal services: " + stats.get("total"));
            System.out.println("Running services: " + stats.get("running"));
            System.out.println("Stopped services: " + stats.get("stopped"));
            
            try {
                // Show additional information for running services
                for (Map.Entry<String, ServiceManager.ServiceStatusInfo> entry : serviceStatuses.entrySet()) {
                    if (entry.getValue().isAvailable()) {
                        System.out.println("\nDetailed information for " + entry.getKey() + ":");
                        
                        // Get and display process information
                        Process process = Runtime.getRuntime().exec(
                            new String[] { "sh", "-c", 
                                         "ps -ef | grep '" + getMainClassName(entry.getKey()) + 
                                         "' | grep -v grep" });
                        
                        java.io.BufferedReader reader = new java.io.BufferedReader(
                            new java.io.InputStreamReader(process.getInputStream()));
                        
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println("  " + line);
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println("Error getting detailed process information: " + e.getMessage());
            }
        }
    }
    
    /**
     * Display status for multiple services in JSON format.
     */
    private void displayMultiStatusAsJson(Map<String, ServiceManager.ServiceStatusInfo> serviceStatuses, Map<String, Object> stats) {
        Map<String, Object> response = new HashMap<>();
        response.put("result", "success");
        response.put("action", "status");
        
        // Build services array
        Map<String, Object>[] servicesArray = new Map[serviceStatuses.size()];
        int i = 0;
        for (Map.Entry<String, ServiceManager.ServiceStatusInfo> entry : serviceStatuses.entrySet()) {
            String service = entry.getKey();
            ServiceManager.ServiceStatusInfo status = entry.getValue();
            
            Map<String, Object> serviceData = new HashMap<>();
            serviceData.put("name", service);
            serviceData.put("status", status.getState());
            serviceData.put("available", status.isAvailable());
            serviceData.put("message", status.getMessage());
            
            servicesArray[i++] = serviceData;
        }
        
        response.put("services", servicesArray);
        response.put("stats", stats);
        
        System.out.println(OutputFormatter.toJson(response));
    }
    
    /**
     * Start a service.
     * 
     * @param operationId the operation ID for tracking
     * @return true if successful, false otherwise
     */
    private boolean startService(String operationId) {
        if (serviceName == null || serviceName.isEmpty()) {
            displayError("Service name is required to start a service", null);
            
            metadataService.failOperation(operationId, 
                new IllegalArgumentException("Service name is required"));
            
            return false;
        }
        
        try {
            int port = 8080; // Default port
            Process process = null;
            
            Map<String, Object> startParams = new HashMap<>();
            startParams.put("serviceName", serviceName);
            
            // Different service types require different startup commands
            switch (serviceName.toLowerCase()) {
                case "api":
                    ConfigurationService config = configService;
                    String serverUrl = config.getServerUrl();
                    
                    try {
                        // Extract port from the URL if possible
                        java.net.URL url = new java.net.URL(serverUrl);
                        if (url.getPort() != -1) {
                            port = url.getPort();
                        }
                    } catch (Exception e) {
                        // Use default port if URL parsing fails
                    }
                    
                    startParams.put("serviceType", "api");
                    startParams.put("port", port);
                    
                    // Track detailed API start
                    String apiStartOpId = metadataService.startOperation(
                        "server-start-api", "EXECUTE", startParams);
                    
                    // Start the API service process
                    process = startApiService(port);
                    
                    // Record the API start operation result
                    Map<String, Object> apiStartResult = new HashMap<>();
                    apiStartResult.put("port", port);
                    apiStartResult.put("processStarted", process != null);
                    metadataService.completeOperation(apiStartOpId, apiStartResult);
                    break;
                    
                case "database":
                    startParams.put("serviceType", "database");
                    
                    // Track detailed database start
                    String dbStartOpId = metadataService.startOperation(
                        "server-start-db", "EXECUTE", startParams);
                    
                    // Start the database service
                    process = startDatabaseService();
                    
                    // Record the database start operation result
                    Map<String, Object> dbStartResult = new HashMap<>();
                    dbStartResult.put("processStarted", process != null);
                    metadataService.completeOperation(dbStartOpId, dbStartResult);
                    break;
                    
                case "docs":
                    startParams.put("serviceType", "docs");
                    
                    // Track detailed docs start
                    String docsStartOpId = metadataService.startOperation(
                        "server-start-docs", "EXECUTE", startParams);
                    
                    // Start the documentation service
                    process = startDocumentationService();
                    
                    // Record the docs start operation result
                    Map<String, Object> docsStartResult = new HashMap<>();
                    docsStartResult.put("processStarted", process != null);
                    metadataService.completeOperation(docsStartOpId, docsStartResult);
                    break;
                    
                default:
                    displayError("Unknown service: " + serviceName, "Known services: api, database, docs");
                    
                    metadataService.failOperation(operationId, 
                        new IllegalArgumentException("Unknown service: " + serviceName));
                    
                    return false;
            }
            
            // Check if the service started successfully
            if (process != null) {
                // For health check, wait a bit
                Thread.sleep(1000);
                
                // Check if the process is still running (non-zero exit code means failure)
                boolean isRunning = process.isAlive();
                if (isRunning) {
                    String pid = getPid(process);
                    
                    Map<String, Object> successResult = new HashMap<>();
                    successResult.put("service", serviceName);
                    successResult.put("status", "RUNNING");
                    successResult.put("pid", pid);
                    
                    if ("json".equalsIgnoreCase(format)) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("result", "success");
                        response.put("action", "start");
                        response.put("service", serviceName);
                        response.put("status", "RUNNING");
                        response.put("pid", pid);
                        
                        System.out.println(OutputFormatter.toJson(response));
                    } else {
                        System.out.printf("Starting service: %s%n", serviceName);
                        System.out.println("Service started successfully");
                        
                        if (verbose) {
                            System.out.println("Service is now in RUNNING state");
                            System.out.println("Service process ID: " + pid);
                        }
                    }
                    
                    metadataService.completeOperation(operationId, successResult);
                    return true;
                } else {
                    displayError("Service failed to start", null);
                    
                    metadataService.failOperation(operationId, 
                        new RuntimeException("Service failed to start"));
                    
                    return false;
                }
            } else {
                displayError("Failed to create service process", null);
                
                metadataService.failOperation(operationId, 
                    new RuntimeException("Failed to create service process"));
                
                return false;
            }
        } catch (Exception e) {
            displayError("Error starting service: " + e.getMessage(), null);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            metadataService.failOperation(operationId, e);
            return false;
        }
    }
    
    /**
     * Starts the API service.
     * 
     * @param port the port to run the API service on
     * @return the service process
     * @throws IOException if the process can't be started
     */
    private Process startApiService(int port) throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
            "java", "-cp", System.getProperty("java.class.path"),
            "org.rinna.adapter.service.ApiHealthServer", String.valueOf(port)
        );
        
        // Redirect error stream to output stream
        processBuilder.redirectErrorStream(true);
        
        // Start the process
        return processBuilder.start();
    }
    
    /**
     * Starts the database service.
     * 
     * @return the service process
     * @throws IOException if the process can't be started
     */
    private Process startDatabaseService() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
            "java", "-cp", System.getProperty("java.class.path"),
            "org.rinna.data.DatabaseServer"
        );
        
        // Add configuration parameters
        Map<String, String> env = processBuilder.environment();
        env.put("RINNA_DB_PATH", System.getProperty("user.home") + "/.rinna/data");
        
        // Redirect error stream to output stream
        processBuilder.redirectErrorStream(true);
        
        // Start the process
        return processBuilder.start();
    }
    
    /**
     * Starts the documentation service.
     * 
     * @return the service process
     * @throws IOException if the process can't be started
     */
    private Process startDocumentationService() throws IOException {
        ProcessBuilder processBuilder = new ProcessBuilder(
            "java", "-cp", System.getProperty("java.class.path"),
            "org.rinna.docs.DocServer"
        );
        
        // Redirect error stream to output stream
        processBuilder.redirectErrorStream(true);
        
        // Start the process
        return processBuilder.start();
    }
    
    /**
     * Gets the process ID (PID) of a running process.
     * 
     * @param process the process
     * @return the process ID as a string, or "unknown" if not available
     */
    private String getPid(Process process) {
        if (process == null) {
            return "unknown";
        }
        
        try {
            // Get the process ID using reflection (Java 9+ has a getPid() method)
            java.lang.reflect.Method pidMethod = process.getClass().getDeclaredMethod("pid");
            pidMethod.setAccessible(true);
            return String.valueOf(pidMethod.invoke(process));
        } catch (Exception e) {
            // For older Java versions or if reflection fails
            return String.valueOf(process.hashCode());
        }
    }
    
    /**
     * Stop a service.
     * 
     * @param operationId the operation ID for tracking
     * @return true if successful, false otherwise
     */
    private boolean stopService(String operationId) {
        if (serviceName == null || serviceName.isEmpty()) {
            displayError("Service name is required to stop a service", null);
            
            metadataService.failOperation(operationId, 
                new IllegalArgumentException("Service name is required"));
            
            return false;
        }
        
        try {
            // Get service status to make sure it's running
            ServiceManager.ServiceStatusInfo status = serviceManager.getServiceStatus(serviceName);
            
            Map<String, Object> statusParams = new HashMap<>();
            statusParams.put("serviceName", serviceName);
            statusParams.put("currentState", status.getState());
            statusParams.put("available", status.isAvailable());
            
            if (!status.isAvailable() || !"RUNNING".equals(status.getState())) {
                if ("json".equalsIgnoreCase(format)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("result", "warning");
                    response.put("message", "Service " + serviceName + " is not running");
                    response.put("status", status.getState());
                    
                    System.out.println(OutputFormatter.toJson(response));
                } else {
                    System.out.printf("Warning: Service %s is not running (current state: %s)%n", 
                                    serviceName, status.getState());
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("warning", "Service not running");
                result.put("status", status.getState());
                
                metadataService.completeOperation(operationId, result);
                return false;
            }
            
            boolean success = false;
            String serviceClass = "";
            
            // Find and stop the service process
            switch (serviceName.toLowerCase()) {
                case "api":
                    serviceClass = "org.rinna.adapter.service.ApiHealthServer";
                    
                    // Track detailed API stop
                    String apiStopOpId = metadataService.startOperation(
                        "server-stop-api", "EXECUTE", 
                        Map.of("serviceName", serviceName, "serviceClass", serviceClass));
                    
                    success = stopServiceByClass(serviceClass);
                    
                    Map<String, Object> apiStopResult = new HashMap<>();
                    apiStopResult.put("success", success);
                    metadataService.completeOperation(apiStopOpId, apiStopResult);
                    break;
                    
                case "database":
                    serviceClass = "org.rinna.data.DatabaseServer";
                    
                    // Track detailed database stop
                    String dbStopOpId = metadataService.startOperation(
                        "server-stop-db", "EXECUTE", 
                        Map.of("serviceName", serviceName, "serviceClass", serviceClass));
                    
                    success = stopServiceByClass(serviceClass);
                    
                    Map<String, Object> dbStopResult = new HashMap<>();
                    dbStopResult.put("success", success);
                    metadataService.completeOperation(dbStopOpId, dbStopResult);
                    break;
                    
                case "docs":
                    serviceClass = "org.rinna.docs.DocServer";
                    
                    // Track detailed docs stop
                    String docsStopOpId = metadataService.startOperation(
                        "server-stop-docs", "EXECUTE", 
                        Map.of("serviceName", serviceName, "serviceClass", serviceClass));
                    
                    success = stopServiceByClass(serviceClass);
                    
                    Map<String, Object> docsStopResult = new HashMap<>();
                    docsStopResult.put("success", success);
                    metadataService.completeOperation(docsStopOpId, docsStopResult);
                    break;
                    
                default:
                    displayError("Unknown service: " + serviceName, "Known services: api, database, docs");
                    
                    metadataService.failOperation(operationId, 
                        new IllegalArgumentException("Unknown service: " + serviceName));
                    
                    return false;
            }
            
            if (success) {
                if ("json".equalsIgnoreCase(format)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("result", "success");
                    response.put("action", "stop");
                    response.put("service", serviceName);
                    response.put("status", "STOPPED");
                    
                    System.out.println(OutputFormatter.toJson(response));
                } else {
                    System.out.printf("Stopping service: %s%n", serviceName);
                    System.out.println("Service stopped successfully");
                    
                    if (verbose) {
                        System.out.println("Service is now in STOPPED state");
                    }
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("service", serviceName);
                result.put("status", "STOPPED");
                result.put("serviceClass", serviceClass);
                
                metadataService.completeOperation(operationId, result);
                return true;
            } else {
                displayError("Failed to stop service " + serviceName, null);
                
                metadataService.failOperation(operationId, 
                    new RuntimeException("Failed to stop service " + serviceName));
                
                return false;
            }
        } catch (Exception e) {
            displayError("Error stopping service: " + e.getMessage(), null);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            metadataService.failOperation(operationId, e);
            return false;
        }
    }
    
    /**
     * Stops a service by its main class name.
     * This uses platform-specific commands to find and stop the service.
     * 
     * @param mainClassName the main class name of the service
     * @return true if the service was stopped successfully
     */
    private boolean stopServiceByClass(String mainClassName) {
        try {
            // The approach to stop a service depends on the OS
            String os = System.getProperty("os.name").toLowerCase();
            Process process;
            
            if (os.contains("win")) {
                // Windows approach
                // First find the process ID
                process = Runtime.getRuntime().exec(
                    new String[] { "wmic", "process", "where", 
                                  "commandline like '%" + mainClassName + "%' and name like '%java%'", 
                                  "get", "processid" });
                
                // Parse the output to get the PID
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
                
                String line;
                String pid = null;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (line.matches("\\d+")) {
                        pid = line;
                        break;
                    }
                }
                
                // If found a PID, kill it
                if (pid != null) {
                    process = Runtime.getRuntime().exec(new String[] { "taskkill", "/F", "/PID", pid });
                    return process.waitFor() == 0;
                }
            } else {
                // Unix/Linux/Mac approach
                // Find the process ID
                process = Runtime.getRuntime().exec(
                    new String[] { "sh", "-c", 
                                  "ps -ef | grep '" + mainClassName + 
                                  "' | grep -v grep | awk '{print $2}'" });
                
                // Parse the output to get the PID
                java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream()));
                
                String line;
                String pid = null;
                while ((line = reader.readLine()) != null) {
                    line = line.trim();
                    if (!line.isEmpty()) {
                        pid = line;
                        break;
                    }
                }
                
                // If found a PID, kill it
                if (pid != null) {
                    process = Runtime.getRuntime().exec(new String[] { "kill", "-15", pid });
                    
                    // Check if the process was killed
                    int exitCode = process.waitFor();
                    if (exitCode != 0) {
                        // If termination signal didn't work, force kill
                        process = Runtime.getRuntime().exec(new String[] { "kill", "-9", pid });
                        return process.waitFor() == 0;
                    }
                    return true;
                }
            }
            
            return false;
        } catch (Exception e) {
            System.err.println("Error stopping service: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Restart a service.
     * 
     * @param operationId the operation ID for tracking
     * @return true if successful, false otherwise
     */
    private boolean restartService(String operationId) {
        if (serviceName == null || serviceName.isEmpty()) {
            displayError("Service name is required to restart a service", null);
            
            metadataService.failOperation(operationId, 
                new IllegalArgumentException("Service name is required"));
            
            return false;
        }
        
        try {
            if (!"json".equalsIgnoreCase(format)) {
                System.out.printf("Restarting service: %s%n", serviceName);
            }
            
            long startTime = System.currentTimeMillis();
            
            // Create separate operations for stop and start phases
            String stopOpId = metadataService.startOperation(
                "server-restart-stop", "EXECUTE", 
                Map.of("serviceName", serviceName, "username", username));
            
            // First stop the service
            boolean stopSuccess = stopService(stopOpId);
            
            // Give it a moment to fully stop
            Thread.sleep(1000);
            
            // If stop failed but it's because the service wasn't running,
            // we can still try to start it
            boolean continueWithStart = stopSuccess || 
                !serviceManager.getServiceStatus(serviceName).isAvailable();
            
            if (continueWithStart) {
                String startOpId = metadataService.startOperation(
                    "server-restart-start", "EXECUTE", 
                    Map.of("serviceName", serviceName, "username", username));
                
                // Start the service
                boolean startSuccess = startService(startOpId);
                
                long duration = System.currentTimeMillis() - startTime;
                float seconds = duration / 1000.0f;
                
                if (startSuccess) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("service", serviceName);
                    result.put("status", "RUNNING");
                    result.put("duration_ms", duration);
                    result.put("seconds", seconds);
                    
                    if ("json".equalsIgnoreCase(format)) {
                        Map<String, Object> response = new HashMap<>();
                        response.put("result", "success");
                        response.put("action", "restart");
                        response.put("service", serviceName);
                        response.put("status", "RUNNING");
                        response.put("duration_ms", duration);
                        
                        System.out.println(OutputFormatter.toJson(response));
                    } else {
                        System.out.println("Service restarted successfully");
                        
                        if (verbose) {
                            System.out.println("Service was stopped and started again");
                            System.out.printf("Restart completed in %.1f seconds%n", seconds);
                        }
                    }
                    
                    metadataService.completeOperation(operationId, result);
                    return true;
                } else {
                    displayError("Failed to start service after stopping it", null);
                    
                    metadataService.failOperation(operationId, 
                        new RuntimeException("Failed to start service after stopping it"));
                    
                    return false;
                }
            } else {
                displayError("Failed to stop service for restart", null);
                
                metadataService.failOperation(operationId, 
                    new RuntimeException("Failed to stop service for restart"));
                
                return false;
            }
        } catch (Exception e) {
            displayError("Error restarting service: " + e.getMessage(), null);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            metadataService.failOperation(operationId, e);
            return false;
        }
    }
    
    /**
     * Configure a service.
     * 
     * @param operationId the operation ID for tracking
     * @return true if successful, false otherwise
     */
    private boolean configureService(String operationId) {
        if (serviceName == null || serviceName.isEmpty()) {
            displayError("Service name is required to configure a service", null);
            
            metadataService.failOperation(operationId, 
                new IllegalArgumentException("Service name is required"));
            
            return false;
        }
        
        try {
            if (configPath == null || configPath.isEmpty()) {
                // Generate a default config path
                configPath = System.getProperty("user.home") + "/.rinna/services/" + serviceName + ".json";
            }
            
            Map<String, Object> configParams = new HashMap<>();
            configParams.put("serviceName", serviceName);
            configParams.put("configPath", configPath);
            configParams.put("defaultPort", 8080);
            configParams.put("defaultLogLevel", "INFO");
            configParams.put("defaultAutoStart", true);
            
            // Create a sub-operation for actual config creation
            String createConfigOpId = metadataService.startOperation(
                "server-create-config", "CREATE", configParams);
            
            // In a real implementation, this would configure the service
            boolean success = serviceManager.createServiceConfig(serviceName, configPath);
            
            if (success) {
                Map<String, Object> createConfigResult = new HashMap<>();
                createConfigResult.put("configPath", configPath);
                createConfigResult.put("success", true);
                metadataService.completeOperation(createConfigOpId, createConfigResult);
                
                if ("json".equalsIgnoreCase(format)) {
                    Map<String, Object> response = new HashMap<>();
                    response.put("result", "success");
                    response.put("action", "config");
                    response.put("service", serviceName);
                    response.put("configPath", configPath);
                    
                    System.out.println(OutputFormatter.toJson(response));
                } else {
                    System.out.printf("Created configuration for %s at: %s%n", serviceName, configPath);
                    
                    if (verbose) {
                        System.out.println("\nConfiguration contains default settings for:");
                        System.out.println("- Service port: 8080");
                        System.out.println("- Log level: INFO");
                        System.out.println("- Auto-start: true");
                    }
                }
                
                Map<String, Object> result = new HashMap<>();
                result.put("service", serviceName);
                result.put("configPath", configPath);
                result.put("created", true);
                
                metadataService.completeOperation(operationId, result);
                return true;
            } else {
                metadataService.failOperation(createConfigOpId, 
                    new RuntimeException("Failed to create service configuration"));
                
                displayError("Failed to create service configuration", null);
                
                metadataService.failOperation(operationId, 
                    new RuntimeException("Failed to create service configuration"));
                
                return false;
            }
        } catch (Exception e) {
            displayError("Error configuring service: " + e.getMessage(), null);
            
            if (verbose) {
                e.printStackTrace();
            }
            
            metadataService.failOperation(operationId, e);
            return false;
        }
    }
    
    public String getSubcommand() {
        return subcommand;
    }
    
    public void setSubcommand(String subcommand) {
        this.subcommand = subcommand;
    }
    
    public String getServiceName() {
        return serviceName;
    }
    
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
    
    public String getConfigPath() {
        return configPath;
    }
    
    public void setConfigPath(String configPath) {
        this.configPath = configPath;
    }
    
    /**
     * Truncates a string if it exceeds the specified length.
     * 
     * @param text the text to truncate
     * @param maxLength the maximum length
     * @return the truncated text
     */
    private String truncateIfNeeded(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        
        if (text.length() <= maxLength) {
            return text;
        }
        
        return text.substring(0, maxLength - 3) + "...";
    }
    
    /**
     * Gets the main class name for a service.
     * 
     * @param serviceName the name of the service
     * @return the main class name
     */
    private String getMainClassName(String serviceName) {
        switch (serviceName.toLowerCase()) {
            case "api":
                return "org.rinna.adapter.service.ApiHealthServer";
                
            case "database":
                return "org.rinna.data.DatabaseServer";
                
            case "docs":
                return "org.rinna.docs.DocServer";
                
            default:
                return serviceName;
        }
    }
    
    /**
     * Displays help information for the server command.
     */
    private void showHelp() {
        if ("json".equalsIgnoreCase(format)) {
            displayHelpAsJson();
        } else {
            displayHelpAsText();
        }
    }
    
    /**
     * Display help information in JSON format.
     */
    private void displayHelpAsJson() {
        Map<String, Object> help = new HashMap<>();
        help.put("result", "success");
        help.put("command", "server");
        help.put("usage", "rin server <subcommand> [options]");
        
        // Subcommands
        Map<String, Object>[] subcommands = new Map[6];
        
        Map<String, Object> statusCmd = new HashMap<>();
        statusCmd.put("name", "status");
        statusCmd.put("description", "Show service status");
        statusCmd.put("usage", "rin server status [service-name]");
        subcommands[0] = statusCmd;
        
        Map<String, Object> startCmd = new HashMap<>();
        startCmd.put("name", "start");
        startCmd.put("description", "Start a service");
        startCmd.put("usage", "rin server start <service-name>");
        subcommands[1] = startCmd;
        
        Map<String, Object> stopCmd = new HashMap<>();
        stopCmd.put("name", "stop");
        stopCmd.put("description", "Stop a service");
        stopCmd.put("usage", "rin server stop <service-name>");
        subcommands[2] = stopCmd;
        
        Map<String, Object> restartCmd = new HashMap<>();
        restartCmd.put("name", "restart");
        restartCmd.put("description", "Restart a service");
        restartCmd.put("usage", "rin server restart <service-name>");
        subcommands[3] = restartCmd;
        
        Map<String, Object> configCmd = new HashMap<>();
        configCmd.put("name", "config");
        configCmd.put("description", "Configure a service");
        configCmd.put("usage", "rin server config <service-name> [config-path]");
        subcommands[4] = configCmd;
        
        Map<String, Object> helpCmd = new HashMap<>();
        helpCmd.put("name", "help");
        helpCmd.put("description", "Show this help information");
        helpCmd.put("usage", "rin server help");
        subcommands[5] = helpCmd;
        
        help.put("subcommands", subcommands);
        
        // Options
        Map<String, Object>[] options = new Map[2];
        
        Map<String, Object> jsonOpt = new HashMap<>();
        jsonOpt.put("name", "--json");
        jsonOpt.put("description", "Output in JSON format");
        options[0] = jsonOpt;
        
        Map<String, Object> verboseOpt = new HashMap<>();
        verboseOpt.put("name", "--verbose");
        verboseOpt.put("description", "Show verbose output with additional details");
        options[1] = verboseOpt;
        
        help.put("options", options);
        
        // Services
        help.put("services", new String[]{"api", "database", "docs"});
        
        System.out.println(OutputFormatter.toJson(help));
    }
    
    /**
     * Display help information in text format.
     */
    private void displayHelpAsText() {
        System.out.println("Server Command Usage:");
        System.out.println("  rin server [subcommand] [options]");
        System.out.println();
        System.out.println("Subcommands:");
        System.out.println("  status   - Show service status (default if no subcommand specified)");
        System.out.println("  start    - Start a service");
        System.out.println("  stop     - Stop a service");
        System.out.println("  restart  - Restart a service");
        System.out.println("  config   - Configure a service");
        System.out.println("  help     - Show this help information");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --json       - Output in JSON format");
        System.out.println("  --verbose    - Show verbose output with additional details");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  rin server                        - Show status of all services");
        System.out.println("  rin server status api             - Show status of the API service");
        System.out.println("  rin server start api              - Start the API service");
        System.out.println("  rin server config api config.json - Configure the API service using config.json");
    }
    
    /**
     * Display an error message in the appropriate format.
     * 
     * @param message the error message
     * @param details additional details (can be null)
     */
    private void displayError(String message, String details) {
        if ("json".equalsIgnoreCase(format)) {
            Map<String, Object> error = new HashMap<>();
            error.put("result", "error");
            error.put("message", message);
            
            if (details != null && !details.isEmpty()) {
                error.put("details", details);
            }
            
            System.out.println(OutputFormatter.toJson(error));
        } else {
            System.err.println("Error: " + message);
            
            if (details != null && !details.isEmpty()) {
                System.err.println(details);
            }
        }
    }
}