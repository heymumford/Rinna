/*
 * Mock server service for Rinna CLI tests
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna.cli.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Collections;
import java.util.Arrays;
import java.util.List;

/**
 * Mock implementation of server service for testing server management commands.
 */
public class MockServerService {
    
    private final Map<String, ServiceStatus> services = new HashMap<>();
    
    /**
     * Initializes the mock server service with default services.
     */
    public MockServerService() {
        // Initialize with default services
        services.put("api", new ServiceStatus("api", "RUNNING", true, "API server running on port 8080"));
        services.put("database", new ServiceStatus("database", "RUNNING", true, "Connected to database"));
        services.put("docs", new ServiceStatus("docs", "STOPPED", false, "Documentation server not running"));
    }
    
    /**
     * Get status information for a service.
     *
     * @param serviceName the name of the service
     * @return status information for the service, or null if service doesn't exist
     */
    public ServiceStatus getServiceStatus(String serviceName) {
        return services.get(serviceName);
    }
    
    /**
     * Get status information for all services.
     *
     * @return list of all service statuses
     */
    public List<ServiceStatus> getAllServiceStatuses() {
        return Collections.unmodifiableList(Arrays.asList(
            services.get("api"),
            services.get("database"),
            services.get("docs")
        ));
    }
    
    /**
     * Start a service.
     *
     * @param serviceName the name of the service to start
     * @return true if service was started successfully, false otherwise
     */
    public boolean startService(String serviceName) {
        ServiceStatus status = services.get(serviceName);
        if (status != null) {
            status.setState("RUNNING");
            status.setAvailable(true);
            status.setMessage(serviceName + " service is now running");
            return true;
        }
        return false;
    }
    
    /**
     * Stop a service.
     *
     * @param serviceName the name of the service to stop
     * @return true if service was stopped successfully, false otherwise
     */
    public boolean stopService(String serviceName) {
        ServiceStatus status = services.get(serviceName);
        if (status != null) {
            status.setState("STOPPED");
            status.setAvailable(false);
            status.setMessage(serviceName + " service is now stopped");
            return true;
        }
        return false;
    }
    
    /**
     * Restart a service.
     *
     * @param serviceName the name of the service to restart
     * @return true if service was restarted successfully, false otherwise
     */
    public boolean restartService(String serviceName) {
        ServiceStatus status = services.get(serviceName);
        if (status != null) {
            status.setState("RUNNING");
            status.setAvailable(true);
            status.setMessage(serviceName + " service was restarted and is now running");
            return true;
        }
        return false;
    }
    
    /**
     * Create a configuration for a service.
     *
     * @param serviceName the name of the service to configure
     * @param configPath the path to the configuration file (can be null for default path)
     * @return true if configuration was created successfully, false otherwise
     */
    public boolean createServiceConfig(String serviceName, String configPath) {
        return services.containsKey(serviceName);
    }
    
    /**
     * Check if a service exists.
     *
     * @param serviceName the name of the service to check
     * @return true if the service exists, false otherwise
     */
    public boolean serviceExists(String serviceName) {
        return services.containsKey(serviceName);
    }
    
    /**
     * Add a new service.
     *
     * @param serviceName the name of the new service
     * @param state the initial state of the service
     * @param available whether the service is initially available
     * @param message a descriptive message about the service
     * @return true if the service was added, false if it already exists
     */
    public boolean addService(String serviceName, String state, boolean available, String message) {
        if (!services.containsKey(serviceName)) {
            services.put(serviceName, new ServiceStatus(serviceName, state, available, message));
            return true;
        }
        return false;
    }
    
    /**
     * Remove a service.
     *
     * @param serviceName the name of the service to remove
     * @return true if the service was removed, false if it didn't exist
     */
    public boolean removeService(String serviceName) {
        if (services.containsKey(serviceName)) {
            services.remove(serviceName);
            return true;
        }
        return false;
    }
    
    /**
     * Reset the mock service to its initial state.
     */
    public void reset() {
        services.clear();
        services.put("api", new ServiceStatus("api", "RUNNING", true, "API server running on port 8080"));
        services.put("database", new ServiceStatus("database", "RUNNING", true, "Connected to database"));
        services.put("docs", new ServiceStatus("docs", "STOPPED", false, "Documentation server not running"));
    }
    
    /**
     * Inner class representing service status.
     */
    public static class ServiceStatus {
        private final String name;
        private String state;
        private boolean available;
        private String message;
        
        /**
         * Constructs a new ServiceStatus instance.
         *
         * @param name the name of the service
         * @param state the current state of the service
         * @param available whether the service is available
         * @param message a descriptive message about the service
         */
        public ServiceStatus(String name, String state, boolean available, String message) {
            this.name = name;
            this.state = state;
            this.available = available;
            this.message = message;
        }
        
        public String getName() {
            return name;
        }
        
        public String getState() {
            return state;
        }
        
        public void setState(String state) {
            this.state = state;
        }
        
        public boolean isAvailable() {
            return available;
        }
        
        public void setAvailable(boolean available) {
            this.available = available;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
    }
}