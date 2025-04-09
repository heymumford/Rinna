/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.domain.service;

import java.util.List;

/**
 * Domain interface for system monitoring services in the CLI module.
 * This service allows monitoring system resources, managing alerts,
 * and generating performance reports.
 */
public interface MonitoringService {
    
    /**
     * Get the dashboard display.
     *
     * @return the dashboard display
     */
    String getDashboard();
    
    /**
     * Get server metrics.
     *
     * @param detailed whether to include detailed metrics
     * @return the server metrics
     */
    String getServerMetrics(boolean detailed);
    
    /**
     * Configure a threshold.
     *
     * @param metric the metric to configure
     * @param value the threshold value
     * @return true if successful, false otherwise
     */
    boolean configureThreshold(String metric, String value);
    
    /**
     * Generate a performance report.
     *
     * @param period the period for the report
     * @return the report
     */
    String generateReport(String period);
    
    /**
     * Add a monitoring alert.
     *
     * @param name the alert name
     * @param metric the metric to monitor
     * @param threshold the threshold value
     * @param recipients the notification recipients
     * @return true if successful, false otherwise
     */
    boolean addAlert(String name, String metric, String threshold, List<String> recipients);
    
    /**
     * List monitoring alerts.
     *
     * @return the alerts listing
     */
    String listAlerts();
    
    /**
     * Remove a monitoring alert.
     *
     * @param name the alert name
     * @return true if successful, false otherwise
     */
    boolean removeAlert(String name);
    
    /**
     * Get active sessions information.
     *
     * @return the active sessions information
     */
    String getActiveSessions();
    
    /**
     * Get monitoring thresholds.
     *
     * @return the monitoring thresholds
     */
    String getThresholds();
}