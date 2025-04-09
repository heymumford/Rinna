package org.rinna.usecase;

import java.util.List;
import java.util.Map;
import java.util.Date;

/**
 * Service for monitoring system health and metrics.
 */
public interface MonitoringService {
    /**
     * Get overall system health status.
     *
     * @return the system health status
     */
    SystemHealth getSystemHealth();
    
    /**
     * Get system metrics.
     *
     * @param metricType the type of metrics to get
     * @return the system metrics
     */
    SystemMetrics getSystemMetrics(MetricType metricType);
    
    /**
     * Get active alerts.
     *
     * @return list of active alerts
     */
    List<Alert> getActiveAlerts();
    
    /**
     * Get alert history.
     *
     * @param startDate the start date for the history
     * @param endDate the end date for the history
     * @return list of alerts in the date range
     */
    List<Alert> getAlertHistory(Date startDate, Date endDate);
    
    /**
     * Configure alert thresholds.
     *
     * @param alertType the type of alert
     * @param warningThreshold the warning threshold
     * @param criticalThreshold the critical threshold
     */
    void configureAlertThresholds(AlertType alertType, double warningThreshold, double criticalThreshold);
    
    /**
     * System health status.
     */
    interface SystemHealth {
        /**
         * Get the overall status.
         *
         * @return the status
         */
        HealthStatus getStatus();
        
        /**
         * Get component statuses.
         *
         * @return map of component name to status
         */
        Map<String, HealthStatus> getComponentStatuses();
        
        /**
         * Get the timestamp of the health check.
         *
         * @return the timestamp
         */
        Date getTimestamp();
    }
    
    /**
     * System metrics.
     */
    interface SystemMetrics {
        /**
         * Get the type of metrics.
         *
         * @return the metric type
         */
        MetricType getType();
        
        /**
         * Get the metrics data.
         *
         * @return map of metric name to value
         */
        Map<String, Double> getMetrics();
        
        /**
         * Get the timestamp of the metrics collection.
         *
         * @return the timestamp
         */
        Date getTimestamp();
    }
    
    /**
     * System alert.
     */
    interface Alert {
        /**
         * Get the alert ID.
         *
         * @return the alert ID
         */
        String getId();
        
        /**
         * Get the alert type.
         *
         * @return the alert type
         */
        AlertType getType();
        
        /**
         * Get the alert severity.
         *
         * @return the alert severity
         */
        AlertSeverity getSeverity();
        
        /**
         * Get the alert message.
         *
         * @return the alert message
         */
        String getMessage();
        
        /**
         * Get the start time of the alert.
         *
         * @return the start time
         */
        Date getStartTime();
        
        /**
         * Get the end time of the alert.
         *
         * @return the end time (null if alert is still active)
         */
        Date getEndTime();
        
        /**
         * Check if the alert is active.
         *
         * @return true if the alert is active, false otherwise
         */
        boolean isActive();
    }
    
    /**
     * Health status.
     */
    enum HealthStatus {
        HEALTHY,
        DEGRADED,
        UNHEALTHY
    }
    
    /**
     * Metric type.
     */
    enum MetricType {
        SYSTEM,
        APPLICATION,
        WORKFLOW,
        DATABASE,
        NETWORK,
        API
    }
    
    /**
     * Alert type.
     */
    enum AlertType {
        CPU_USAGE,
        MEMORY_USAGE,
        DISK_SPACE,
        RESPONSE_TIME,
        ERROR_RATE,
        CONNECTION_COUNT,
        QUEUE_SIZE,
        DATABASE_CONNECTIONS,
        APPLICATION_SPECIFIC
    }
    
    /**
     * Alert severity.
     */
    enum AlertSeverity {
        WARNING,
        CRITICAL
    }
}