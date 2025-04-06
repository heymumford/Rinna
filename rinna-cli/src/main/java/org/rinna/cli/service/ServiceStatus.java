package org.rinna.cli.service;

/**
 * Status values for Rinna services.
 */
public enum ServiceStatus {
    RUNNING,
    STOPPED,
    STARTING,
    STOPPING,
    UNKNOWN;
    
    /**
     * Checks if the service is available/functional.
     * By default, only RUNNING status is considered available.
     *
     * @return true if the service is available, false otherwise
     */
    public boolean isAvailable() {
        return this == RUNNING;
    }
    
    /**
     * Gets the state name as a string.
     *
     * @return the name of the service state
     */
    public String getState() {
        return this.name();
    }
}
