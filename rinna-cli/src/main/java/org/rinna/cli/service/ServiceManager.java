/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.service;

import org.rinna.cli.adapter.HistoryServiceAdapter;
import org.rinna.cli.messaging.MessageClient;
import org.rinna.cli.messaging.MessageService;
import org.rinna.cli.model.WorkItem;
import org.rinna.cli.notifications.NotificationService;

import java.util.List;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages services for the CLI application.
 */
public final class ServiceManager {
    private static ServiceManager instance;
    
    private Object workflowService;
    private Object commentService;
    private Object historyService;
    private Object itemService;
    private Object searchService;
    private MessageService messageService;
    private MessageClient messageClient;
    private ProjectContext projectContext;
    private ConfigurationService configurationService;
    private Object backlogService;
    private Object monitoringService;
    private Object recoveryService;
    private Object criticalPathService;
    private DiagnosticsService diagnosticsService;
    private MockRelationshipService relationshipService;
    private AuditService auditService;
    private BackupService backupService;
    private ComplianceService complianceService;
    private MetadataService metadataService;
    private MockReportService reportService;
    private MockNotificationService mockNotificationService;
    
    // Thread-safe map to store user-specific data
    private final Map<String, Map<String, Object>> userDataMap = new ConcurrentHashMap<>();
    
    // Private constructor for singleton
    private ServiceManager() {
        // Initialize service manager
        initializeServices();
    }
    
    /**
     * Initialize service components.
     * 
     * This method is responsible for setting up all service instances for the CLI.
     * It follows the service locator pattern to provide access to services.
     */
    private void initializeServices() {
        // Configure service initialization strategy based on environment
        // In production, we'll detect if services are available via proper detection
        boolean useRemoteServices = ConfigurationService.areRemoteServicesAvailable();
        
        // Create CLI-specific service instances that can work independently
        MockWorkflowService mockWorkflowService = ServiceFactory.createCliWorkflowService();
        MockBacklogService mockBacklogService = ServiceFactory.createCliBacklogService();
        MockItemService mockItemService = ServiceFactory.createCliItemService();
        MockCommentService mockCommentService = ServiceFactory.createCliCommentService();
        MockHistoryService mockHistoryService = ServiceFactory.createCliHistoryService();
        MockSearchService mockSearchService = ServiceFactory.createCliSearchService();
        MockMonitoringService mockMonitoringService = ServiceFactory.createCliMonitoringService();
        MockRecoveryService mockRecoveryService = ServiceFactory.createCliRecoveryService();
        MockReportService mockReportService = ServiceFactory.createCliReportService();
        
        // Create domain adapter services - these will either connect to remote services
        // or fall back to local implementations
        this.workflowService = ServiceFactory.createWorkflowService();
        this.backlogService = ServiceFactory.createBacklogService();
        this.itemService = ServiceFactory.createItemService();
        this.commentService = ServiceFactory.createCommentService();
        this.historyService = ServiceFactory.createHistoryService();
        this.searchService = ServiceFactory.createSearchService();
        this.monitoringService = ServiceFactory.createMonitoringService();
        this.recoveryService = ServiceFactory.createRecoveryService();
        this.criticalPathService = ServiceFactory.createCriticalPathService();
        this.reportService = mockReportService;
        
        // Store CLI services directly in their specialized fields
        this.messageService = new MockMessageService();
        this.messageClient = new MockMessageClient();
        this.diagnosticsService = new MockDiagnosticsService();
        this.relationshipService = new MockRelationshipService();
        this.auditService = new MockAuditService();
        this.backupService = new MockBackupService();
        this.complianceService = new MockComplianceService();
        this.metadataService = MockMetadataService.getInstance();
        this.mockNotificationService = MockNotificationService.getInstance();
        this.projectContext = ProjectContext.getInstance();
        this.configurationService = ConfigurationService.getInstance();
    }
    
    /**
     * Get singleton instance of ServiceManager.
     *
     * @return the singleton instance
     */
    public static synchronized ServiceManager getInstance() {
        if (instance == null) {
            instance = new ServiceManager();
        }
        return instance;
    }
    
    /**
     * Get status information for a service.
     *
     * @param serviceName the name of the service
     * @return status information for the service
     */
    public ServiceStatusInfo getServiceStatus(String serviceName) {
        // For mock/test services, return a fake status
        if ("mock-service".equals(serviceName)) {
            return new ServiceStatusInfo(true, "RUNNING", "Mock service for testing");
        }
        
        // Check messaging service
        if ("messaging".equals(serviceName)) {
            boolean connected = messageClient.isConnected();
            String state = connected ? "RUNNING" : "DISCONNECTED";
            String message = connected ? "Messaging service is running" : "Messaging service is not connected";
            return new ServiceStatusInfo(connected, state, message);
        }
        
        // For real services, this would query the actual service status
        // In this implementation, any unknown service is considered unavailable
        return new ServiceStatusInfo(false, "UNKNOWN", "Service not found: " + serviceName);
    }
    
    /**
     * Create a configuration file for a service.
     *
     * @param serviceName the name of the service to configure
     * @param configPath the path where the config file should be created
     * @return true if successful, false otherwise
     */
    public boolean createServiceConfig(String serviceName, String configPath) {
        try {
            // Create simple JSON config file
            String content = "{\n" +
                            "  \"serviceName\": \"" + serviceName + "\",\n" +
                            "  \"enabled\": true,\n" +
                            "  \"port\": 8080,\n" +
                            "  \"logLevel\": \"INFO\"\n" +
                            "}\n";
            
            // Using Files.writeString for simplified file writing with proper encoding
            Files.writeString(Paths.get(configPath), content, StandardCharsets.UTF_8);
            return true;
        } catch (IOException e) {
            System.err.println("Error creating service config: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Check if a local service endpoint is available.
     *
     * @return true if local endpoint is available
     */
    public boolean hasLocalEndpoint() {
        try {
            // Check if the local server port is available (not occupied)
            try (java.net.Socket socket = new java.net.Socket()) {
                // Try to connect to localhost on the standard Rinna API port (8080)
                socket.connect(new java.net.InetSocketAddress("localhost", 8080), 500);
                // If we can connect, something is running on this port
                return true;
            } catch (java.net.ConnectException e) {
                // If connection refused, nothing is running on the port
                return false;
            }
        } catch (Exception e) {
            // For any error, assume the endpoint is not available
            return false;
        }
    }
    
    /**
     * Connect to the local service endpoint.
     * This method attempts to establish a connection to the local service,
     * creating it if necessary.
     *
     * @return true if connection successful
     */
    public boolean connectLocalEndpoint() {
        // First check if the endpoint is already available
        if (hasLocalEndpoint()) {
            return messageClient.connect();
        }
        
        // If no local endpoint is available, try to start one
        try {
            // Get the server port from configuration
            ConfigurationService config = ConfigurationService.getInstance();
            String serverUrl = config.getServerUrl();
            int port = 8080; // Default port
            
            try {
                // Extract port from the URL if possible
                java.net.URL url = new java.net.URL(serverUrl);
                if (url.getPort() != -1) {
                    port = url.getPort();
                }
            } catch (Exception e) {
                // Use default port if URL parsing fails
            }
            
            // Launch server process
            ProcessBuilder processBuilder = new ProcessBuilder(
                "java", "-cp", System.getProperty("java.class.path"),
                "org.rinna.adapter.service.ApiHealthServer", String.valueOf(port)
            );
            
            Process process = processBuilder.start();
            
            // Wait a bit for the server to start
            Thread.sleep(2000);
            
            // Try to connect
            return messageClient.connect();
        } catch (Exception e) {
            System.err.println("Failed to start local endpoint: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Gets the workflow service.
     *
     * @return the workflow service
     */
    public Object getWorkflowService() {
        return workflowService;
    }
    
    /**
     * Gets the domain-compatible comment service.
     *
     * @return the domain comment service
     */
    public Object getCommentService() {
        return commentService;
    }
    
    /**
     * Gets the CLI-specific comment service for direct CLI model access.
     *
     * @return the CLI-specific comment service
     */
    public MockCommentService getMockCommentService() {
        return ServiceFactory.createCliCommentService();
    }
    
    /**
     * Gets the domain-compatible history service.
     *
     * @return the domain history service
     */
    public Object getHistoryService() {
        return historyService;
    }
    
    /**
     * Gets the CLI-specific history service for direct CLI model access.
     *
     * @return the CLI-specific history service
     */
    public MockHistoryService getMockHistoryService() {
        return ServiceFactory.createCliHistoryService();
    }
    
    /**
     * Gets the item service.
     *
     * @return the item service
     */
    public ItemService getItemService() {
        return (ItemService) itemService;
    }
    
    /**
     * Gets the CLI-specific item service for direct CLI model access.
     *
     * @return the CLI-specific item service
     */
    public MockItemService getMockItemService() {
        return ServiceFactory.createCliItemService();
    }
    
    /**
     * Gets the CLI-specific workflow service for direct CLI model access.
     *
     * @return the CLI-specific workflow service
     */
    public MockWorkflowService getMockWorkflowService() {
        return ServiceFactory.createCliWorkflowService();
    }
    
    /**
     * Sets the item service.
     *
     * @param itemService the item service to set
     */
    public void setItemService(Object itemService) {
        this.itemService = itemService;
    }
    
    /**
     * Gets the domain-compatible search service.
     *
     * @return the domain search service
     */
    public Object getSearchService() {
        return searchService;
    }
    
    /**
     * Gets the CLI-specific search service for direct CLI model access.
     *
     * @return the CLI-specific search service
     */
    public MockSearchService getMockSearchService() {
        return ServiceFactory.createCliSearchService();
    }
    
    /**
     * Gets the message service.
     *
     * @return the message service
     */
    public MessageService getMessageService() {
        return messageService;
    }
    
    /**
     * Gets the message client.
     *
     * @return the message client
     */
    public MessageClient getMessageClient() {
        return messageClient;
    }
    
    /**
     * Gets the project context.
     *
     * @return the project context
     */
    public ProjectContext getProjectContext() {
        return projectContext;
    }
    
    /**
     * Gets the configuration service.
     *
     * @return the configuration service
     */
    public ConfigurationService getConfigurationService() {
        return configurationService;
    }
    
    /**
     * Gets the backlog service.
     *
     * @return the backlog service
     */
    public Object getBacklogService() {
        return backlogService;
    }
    
    /**
     * Gets the CLI-specific backlog service for direct CLI model access.
     *
     * @return the CLI-specific backlog service
     */
    public MockBacklogService getMockBacklogService() {
        return ServiceFactory.createCliBacklogService();
    }
    
    /**
     * Gets the domain-compatible monitoring service.
     *
     * @return the domain monitoring service
     */
    public Object getMonitoringService() {
        return monitoringService;
    }
    
    /**
     * Gets the CLI-specific monitoring service for direct CLI model access.
     *
     * @return the CLI-specific monitoring service
     */
    public MockMonitoringService getMockMonitoringService() {
        return ServiceFactory.createCliMonitoringService();
    }
    
    /**
     * Gets the domain-compatible recovery service.
     *
     * @return the domain recovery service
     */
    public Object getRecoveryService() {
        return recoveryService;
    }
    
    /**
     * Gets the CLI-specific recovery service for direct CLI model access.
     *
     * @return the CLI-specific recovery service
     */
    public MockRecoveryService getMockRecoveryService() {
        return ServiceFactory.createCliRecoveryService();
    }
    
    /**
     * Gets the domain-compatible critical path service.
     *
     * @return the domain critical path service
     */
    public Object getCriticalPathService() {
        return criticalPathService;
    }
    
    /**
     * Gets the CLI-specific critical path service for direct CLI model access.
     *
     * @return the CLI-specific critical path service
     */
    public MockCriticalPathService getMockCriticalPathService() {
        return ServiceFactory.createCliCriticalPathService();
    }
    
    /**
     * Gets the diagnostics service.
     *
     * @return the diagnostics service
     */
    public DiagnosticsService getDiagnosticsService() {
        return (DiagnosticsService) diagnosticsService;
    }
    
    /**
     * Gets the relationship service.
     *
     * @return the relationship service
     */
    public Object getRelationshipService() {
        return relationshipService;
    }
    
    /**
     * Gets the CLI-specific relationship service for direct CLI model access.
     *
     * @return the CLI-specific relationship service
     */
    public MockRelationshipService getMockRelationshipService() {
        return relationshipService;
    }
    
    /**
     * Gets the audit service.
     *
     * @return the audit service
     */
    public AuditService getAuditService() {
        return auditService;
    }
    
    /**
     * Gets the backup service.
     *
     * @return the backup service
     */
    public BackupService getBackupService() {
        return backupService;
    }
    
    /**
     * Gets the compliance service.
     *
     * @return the compliance service
     */
    public ComplianceService getComplianceService() {
        return complianceService;
    }
    
    /**
     * Gets the metadata service for tracking operation metadata.
     *
     * @return the metadata service
     */
    public MetadataService getMetadataService() {
        return metadataService;
    }
    
    /**
     * Gets the report service.
     *
     * @return the report service
     */
    public MockReportService getMockReportService() {
        return reportService;
    }
    
    /**
     * Gets the notification service.
     *
     * @return the notification service
     */
    public MockNotificationService getMockNotificationService() {
        return mockNotificationService;
    }
    
    /**
     * Gets user-specific data map, creating it if it doesn't exist.
     * This method allows services to store and retrieve user-specific data.
     *
     * @param username the username to get data for
     * @return a map containing user-specific data
     */
    public Map<String, Object> getUserData(String username) {
        return userDataMap.computeIfAbsent(username, k -> new ConcurrentHashMap<>());
    }
    
    /**
     * Clears all data for a specific user.
     *
     * @param username the username to clear data for
     * @return true if user data existed and was cleared, false otherwise
     */
    public boolean clearUserData(String username) {
        return userDataMap.remove(username) != null;
    }
    
    /**
     * Inner class representing service status information.
     */
    public static class ServiceStatusInfo {
        private final boolean available;
        private final String state;
        private final String message;
        
        /**
         * Constructs a new ServiceStatusInfo instance.
         *
         * @param available whether the service is available
         * @param state the current state of the service
         * @param message a descriptive message about the service
         */
        public ServiceStatusInfo(boolean available, String state, String message) {
            this.available = available;
            this.state = state;
            this.message = message;
        }
        
        public boolean isAvailable() {
            return available;
        }
        
        public String getState() {
            return state;
        }
        
        public String getMessage() {
            return message;
        }
        
        @Override
        public String toString() {
            return "ServiceStatus{" +
                   "available=" + available +
                   ", state='" + state + '\'' +
                   ", message='" + message + '\'' +
                   '}';
        }
    }
}