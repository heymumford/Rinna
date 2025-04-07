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

import org.rinna.cli.messaging.MessageClient;
import org.rinna.cli.messaging.MessageService;
import org.rinna.domain.service.CommentService;
import org.rinna.domain.service.HistoryService;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.SearchService;
import org.rinna.domain.service.WorkflowService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Manages services for the CLI application.
 */
public final class ServiceManager {
    private static ServiceManager instance;
    
    private WorkflowService workflowService;
    private CommentService commentService;
    private HistoryService historyService;
    private ItemService itemService;
    private SearchService searchService;
    private MessageService messageService;
    private MessageClient messageClient;
    private ProjectContext projectContext;
    private ConfigurationService configurationService;
    
    // Private constructor for singleton
    private ServiceManager() {
        // Initialize service manager
        initializeServices();
    }
    
    /**
     * Initialize service components.
     */
    private void initializeServices() {
        // In a real implementation, these would be properly initialized
        // For now, we're using mock implementations
        this.workflowService = new MockWorkflowService();
        this.commentService = new MockCommentService();
        this.historyService = new MockHistoryService();
        this.itemService = new MockItemService();
        this.searchService = new MockSearchService();
        this.messageService = new MockMessageService();
        this.messageClient = new MockMessageClient();
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
        // In a real implementation, this would check if local endpoint is available
        // For this sample, we'll always return true
        return true;
    }
    
    /**
     * Connect to the local service endpoint.
     *
     * @return true if connection successful
     */
    public boolean connectLocalEndpoint() {
        // In a real implementation, this would actually connect to a service
        // For this sample, we'll always return true
        return messageClient.connect();
    }
    
    /**
     * Gets the workflow service.
     *
     * @return the workflow service
     */
    public WorkflowService getWorkflowService() {
        return workflowService;
    }
    
    /**
     * Gets the comment service.
     *
     * @return the comment service
     */
    public CommentService getCommentService() {
        return commentService;
    }
    
    /**
     * Gets the history service.
     *
     * @return the history service
     */
    public HistoryService getHistoryService() {
        return historyService;
    }
    
    /**
     * Gets the item service.
     *
     * @return the item service
     */
    public ItemService getItemService() {
        return itemService;
    }
    
    /**
     * Gets the CLI-specific item service for direct CLI model access.
     *
     * @return the CLI-specific item service
     */
    public MockItemService getMockItemService() {
        return (MockItemService) itemService;
    }
    
    /**
     * Gets the CLI-specific workflow service for direct CLI model access.
     *
     * @return the CLI-specific workflow service
     */
    public MockWorkflowService getMockWorkflowService() {
        return (MockWorkflowService) workflowService;
    }
    
    /**
     * Sets the item service.
     *
     * @param itemService the item service to set
     */
    public void setItemService(ItemService itemService) {
        this.itemService = itemService;
    }
    
    /**
     * Gets the search service.
     *
     * @return the search service
     */
    public SearchService getSearchService() {
        return searchService;
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