package org.rinna.cli.model;

/**
 * Represents the status of a service in the system.
 * This class is used to track and report the operational status of various services.
 */
public class ServiceStatus {
    private final String serviceName;
    private final String status;
    private final boolean available;
    private final String message;
    private final long lastChecked;

    /**
     * Creates a new ServiceStatus instance.
     *
     * @param serviceName The name of the service
     * @param status The current status (e.g., "RUNNING", "STOPPED", "ERROR")
     * @param available Whether the service is currently available
     * @param message Additional status message or details
     */
    public ServiceStatus(String serviceName, String status, boolean available, String message) {
        this.serviceName = serviceName;
        this.status = status;
        this.available = available;
        this.message = message;
        this.lastChecked = System.currentTimeMillis();
    }

    /**
     * Gets the service name.
     *
     * @return The service name
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * Gets the current status.
     *
     * @return The status string
     */
    public String getStatus() {
        return status;
    }

    /**
     * Checks if the service is available.
     *
     * @return true if available, false otherwise
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Gets the status message.
     *
     * @return The status message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Gets the timestamp when the status was last checked.
     *
     * @return The timestamp in milliseconds
     */
    public long getLastChecked() {
        return lastChecked;
    }

    @Override
    public String toString() {
        return "ServiceStatus{" +
                "serviceName='" + serviceName + '\'' +
                ", status='" + status + '\'' +
                ", available=" + available +
                ", message='" + message + '\'' +
                ", lastChecked=" + lastChecked +
                '}';
    }
}