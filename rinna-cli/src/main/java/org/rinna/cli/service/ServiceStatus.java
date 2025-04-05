/*
 * Service status tracking for the Rinna workflow system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.service;

/**
 * Represents the status of Rinna services.
 * Contains information about each service's status and details.
 */
public class ServiceStatus {
    private boolean goApiRunning;
    private boolean javaBackendRunning;
    private String goApiDetails;
    private String javaBackendDetails;
    
    /**
     * Default constructor.
     */
    public ServiceStatus() {
        this.goApiRunning = false;
        this.javaBackendRunning = false;
        this.goApiDetails = "Not checked";
        this.javaBackendDetails = "Not checked";
    }
    
    /**
     * Returns whether the Go API is running.
     *
     * @return true if the Go API is running
     */
    public boolean isGoApiRunning() {
        return goApiRunning;
    }
    
    /**
     * Sets whether the Go API is running.
     *
     * @param goApiRunning true if the Go API is running
     */
    public void setGoApiRunning(boolean goApiRunning) {
        this.goApiRunning = goApiRunning;
    }
    
    /**
     * Returns whether the Java backend is running.
     *
     * @return true if the Java backend is running
     */
    public boolean isJavaBackendRunning() {
        return javaBackendRunning;
    }
    
    /**
     * Sets whether the Java backend is running.
     *
     * @param javaBackendRunning true if the Java backend is running
     */
    public void setJavaBackendRunning(boolean javaBackendRunning) {
        this.javaBackendRunning = javaBackendRunning;
    }
    
    /**
     * Returns details about the Go API.
     *
     * @return details about the Go API
     */
    public String getGoApiDetails() {
        return goApiDetails;
    }
    
    /**
     * Sets details about the Go API.
     *
     * @param goApiDetails details about the Go API
     */
    public void setGoApiDetails(String goApiDetails) {
        this.goApiDetails = goApiDetails;
    }
    
    /**
     * Returns details about the Java backend.
     *
     * @return details about the Java backend
     */
    public String getJavaBackendDetails() {
        return javaBackendDetails;
    }
    
    /**
     * Sets details about the Java backend.
     *
     * @param javaBackendDetails details about the Java backend
     */
    public void setJavaBackendDetails(String javaBackendDetails) {
        this.javaBackendDetails = javaBackendDetails;
    }
    
    /**
     * Returns whether all services are running.
     *
     * @return true if all services are running
     */
    public boolean areAllServicesRunning() {
        return goApiRunning && javaBackendRunning;
    }
    
    /**
     * Returns a string representation of the service status.
     *
     * @return a string representation of the service status
     */
    @Override
    public String toString() {
        return String.format(
                "Service Status:\n" +
                "- Go API: %s\n" +
                "- Java Backend: %s",
                goApiRunning ? "Running" : "Not running",
                javaBackendRunning ? "Running" : "Not running"
        );
    }
}