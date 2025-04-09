package org.rinna.cli.service;

/**
 * Represents possible service statuses.
 */
public enum ServiceStatus {
    RUNNING,
    STOPPED,
    STARTING,
    STOPPING,
    ERROR,
    UNKNOWN;
    
    /**
     * Convert from string ignoring case.
     *
     * @param value the string value to convert
     * @return the corresponding ServiceStatus or UNKNOWN if not found
     */
    public static ServiceStatus fromString(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(value.toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }
}
