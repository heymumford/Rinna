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

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mock monitoring service for CLI use.
 */
public class MockMonitoringService implements MonitoringService {
    private final Map<String, String> thresholds = new HashMap<>();
    private final Map<String, List<String>> alertRecipients = new HashMap<>();
    private final Map<String, String> alertThresholds = new HashMap<>();
    private final Map<String, String> alertMetrics = new HashMap<>();
    
    public MockMonitoringService() {
        // Initialize default thresholds
        thresholds.put("CPU Load", "85");
        thresholds.put("Memory Usage", "90");
        thresholds.put("Disk Usage", "85");
        thresholds.put("Network Connections", "1000");
        thresholds.put("Response Time", "500");
        thresholds.put("Error Rate", "1");
        thresholds.put("Refresh Interval", "60");
        
        // Sample alerts
        alertThresholds.put("High CPU Alert", "95");
        alertMetrics.put("High CPU Alert", "CPU Load");
        List<String> recipients = new ArrayList<>();
        recipients.add("admin@example.com");
        alertRecipients.put("High CPU Alert", recipients);
    }
    
    /**
     * Get the dashboard display.
     *
     * @return the dashboard display
     */
    public String getDashboard() {
        StringBuilder sb = new StringBuilder();
        sb.append("System Monitoring Dashboard\n");
        sb.append("==========================\n\n");
        
        sb.append("System Overview:\n");
        sb.append("  CPU Load:        42.3% (Threshold: ").append(thresholds.get("CPU Load")).append("%)\n");
        sb.append("  Memory Usage:    68.7% (Threshold: ").append(thresholds.get("Memory Usage")).append("%)\n");
        sb.append("  Disk Usage:      74.2% (Threshold: ").append(thresholds.get("Disk Usage")).append("%)\n");
        sb.append("  Network Conns:   423   (Threshold: ").append(thresholds.get("Network Connections")).append(")\n");
        sb.append("  Response Time:   45ms  (Threshold: ").append(thresholds.get("Response Time")).append("ms)\n");
        sb.append("  Error Rate:      0.2%  (Threshold: ").append(thresholds.get("Error Rate")).append("%)\n\n");
        
        sb.append("Active Alerts: 0\n\n");
        
        sb.append("System Health: GOOD\n");
        sb.append("Uptime: 15 days, 7 hours\n");
        sb.append("Last Updated: ").append(new Date()).append("\n");
        
        return sb.toString();
    }
    
    /**
     * Get server metrics.
     *
     * @param detailed whether to include detailed metrics
     * @return the server metrics
     */
    public String getServerMetrics(boolean detailed) {
        StringBuilder sb = new StringBuilder();
        sb.append("Server Metrics\n");
        sb.append("=============\n\n");
        
        sb.append("System Resources:\n");
        sb.append("  CPU Load:        42.3%\n");
        sb.append("  Memory Usage:    68.7%\n");
        sb.append("  Disk Usage:      74.2%\n");
        sb.append("  Load Average:    1.25, 1.15, 1.05 (1, 5, 15 min)\n");
        sb.append("  Network In:      1.2 MB/s\n");
        sb.append("  Network Out:     0.8 MB/s\n\n");
        
        sb.append("Application Metrics:\n");
        sb.append("  Active Users:    12\n");
        sb.append("  Active Sessions: 18\n");
        sb.append("  Requests/sec:    23.5\n");
        sb.append("  Avg Response:    45ms\n");
        sb.append("  Error Rate:      0.2%\n\n");
        
        if (detailed) {
            sb.append("Detailed Resource Usage:\n");
            sb.append("  CPU Cores: 8\n");
            sb.append("  CPU Usage by Core:\n");
            sb.append("    Core 1: 45.2%\n");
            sb.append("    Core 2: 38.7%\n");
            sb.append("    Core 3: 52.1%\n");
            sb.append("    Core 4: 37.6%\n");
            sb.append("    Core 5: 42.8%\n");
            sb.append("    Core 6: 39.5%\n");
            sb.append("    Core 7: 41.2%\n");
            sb.append("    Core 8: 40.9%\n\n");
            
            sb.append("  Memory Details:\n");
            sb.append("    Total:      16.0 GB\n");
            sb.append("    Used:       11.0 GB\n");
            sb.append("    Free:        5.0 GB\n");
            sb.append("    Buffers:     2.3 GB\n");
            sb.append("    Cached:      3.7 GB\n\n");
            
            sb.append("  Disk Details:\n");
            sb.append("    Mount Point: /\n");
            sb.append("    Size:        500 GB\n");
            sb.append("    Used:        371 GB\n");
            sb.append("    Free:        129 GB\n");
            sb.append("    IOPS:        123 read, 89 write\n\n");
            
            sb.append("  Network Interfaces:\n");
            sb.append("    eth0:\n");
            sb.append("      RX:        1.2 MB/s\n");
            sb.append("      TX:        0.8 MB/s\n");
            sb.append("      Packets:   952 rx/s, 736 tx/s\n");
            sb.append("      Errors:    0 rx, 0 tx\n");
            sb.append("      Dropped:   0 rx, 0 tx\n");
        }
        
        return sb.toString();
    }
    
    /**
     * Configure a threshold.
     *
     * @param metric the metric to configure
     * @param value the threshold value
     * @return true if successful, false otherwise
     */
    public boolean configureThreshold(String metric, String value) {
        thresholds.put(metric, value);
        return true;
    }
    
    /**
     * Generate a performance report.
     *
     * @param period the period for the report
     * @return the report
     */
    public String generateReport(String period) {
        StringBuilder sb = new StringBuilder();
        sb.append("System Performance Report (").append(period).append(")\n");
        sb.append("=================================").append("=".repeat(period.length())).append("\n\n");
        
        sb.append("Report Period: ").append(period).append("\n");
        sb.append("Generated On: ").append(new Date()).append("\n\n");
        
        sb.append("Performance Summary:\n");
        sb.append("  Average CPU Load:        38.7%\n");
        sb.append("  Peak CPU Load:           78.2%\n");
        sb.append("  Average Memory Usage:    62.3%\n");
        sb.append("  Peak Memory Usage:       85.6%\n");
        sb.append("  Average Disk IO:         12.3 MB/s\n");
        sb.append("  Peak Disk IO:            45.8 MB/s\n");
        sb.append("  Average Network Traffic: 5.2 MB/s\n");
        sb.append("  Peak Network Traffic:    18.7 MB/s\n\n");
        
        sb.append("Application Metrics:\n");
        sb.append("  Total Requests:        ").append(period.equals("hourly") ? "23,456" : 
                                                    period.equals("daily") ? "342,561" : 
                                                    period.equals("weekly") ? "2,345,678" : "9,876,543").append("\n");
        sb.append("  Average Response Time: 48ms\n");
        sb.append("  Error Rate:            0.23%\n");
        sb.append("  Slowest Endpoints:\n");
        sb.append("    1. /api/analytics - 156ms\n");
        sb.append("    2. /api/reports - 134ms\n");
        sb.append("    3. /api/search - 127ms\n\n");
        
        sb.append("System Health:\n");
        sb.append("  Uptime: 99.98%\n");
        sb.append("  Alerts: 3\n");
        sb.append("  Critical Incidents: 0\n\n");
        
        sb.append("Performance Trends:\n");
        sb.append("  CPU Usage: STABLE\n");
        sb.append("  Memory Usage: INCREASING (2% per week)\n");
        sb.append("  Disk Usage: INCREASING (5% per month)\n");
        sb.append("  Network Traffic: STABLE\n\n");
        
        sb.append("Recommendations:\n");
        sb.append("  1. Monitor increasing memory usage trend\n");
        sb.append("  2. Consider disk cleanup to address usage growth\n");
        sb.append("  3. Optimize slow endpoints identified above\n");
        
        return sb.toString();
    }
    
    /**
     * Add a monitoring alert.
     *
     * @param name the alert name
     * @param metric the metric to monitor
     * @param threshold the threshold value
     * @param recipients the notification recipients
     * @return true if successful, false otherwise
     */
    public boolean addAlert(String name, String metric, String threshold, List<String> recipients) {
        alertMetrics.put(name, metric);
        alertThresholds.put(name, threshold);
        alertRecipients.put(name, new ArrayList<>(recipients));
        return true;
    }
    
    /**
     * List monitoring alerts.
     *
     * @return the alerts listing
     */
    public String listAlerts() {
        StringBuilder sb = new StringBuilder();
        sb.append("Monitoring Alerts\n");
        sb.append("================\n\n");
        
        if (alertMetrics.isEmpty()) {
            sb.append("No alerts configured.\n");
        } else {
            for (Map.Entry<String, String> entry : alertMetrics.entrySet()) {
                String name = entry.getKey();
                String metric = entry.getValue();
                String threshold = alertThresholds.get(name);
                List<String> recipients = alertRecipients.get(name);
                
                sb.append("Alert: ").append(name).append("\n");
                sb.append("  Metric:     ").append(metric).append("\n");
                sb.append("  Threshold:  ").append(threshold).append("\n");
                sb.append("  Recipients: ").append(String.join(", ", recipients)).append("\n");
                sb.append("  Status:     Active\n\n");
            }
        }
        
        return sb.toString();
    }
    
    /**
     * Remove a monitoring alert.
     *
     * @param name the alert name
     * @return true if successful, false otherwise
     */
    public boolean removeAlert(String name) {
        if (alertMetrics.containsKey(name)) {
            alertMetrics.remove(name);
            alertThresholds.remove(name);
            alertRecipients.remove(name);
            return true;
        }
        return false;
    }
    
    /**
     * Get active sessions information.
     *
     * @return the active sessions information
     */
    public String getActiveSessions() {
        StringBuilder sb = new StringBuilder();
        sb.append("Active User Sessions\n");
        sb.append("===================\n\n");
        
        sb.append("Total Active Sessions: 18\n\n");
        
        sb.append("Session ID             | Username      | IP Address     | Login Time          | Idle\n");
        sb.append("---------------------- | ------------- | -------------- | ------------------- | -----\n");
        sb.append("sess_" + UUID.randomUUID().toString().substring(0, 8) + " | admin         | 192.168.1.10   | " + new Date() + " | 2m\n");
        sb.append("sess_" + UUID.randomUUID().toString().substring(0, 8) + " | john.smith    | 192.168.1.45   | " + new Date() + " | 5m\n");
        sb.append("sess_" + UUID.randomUUID().toString().substring(0, 8) + " | sara.jones    | 192.168.2.112  | " + new Date() + " | 1m\n");
        sb.append("sess_" + UUID.randomUUID().toString().substring(0, 8) + " | mike.johnson  | 192.168.3.56   | " + new Date() + " | 8m\n");
        sb.append("sess_" + UUID.randomUUID().toString().substring(0, 8) + " | lisa.brown    | 192.168.1.78   | " + new Date() + " | 3m\n");
        sb.append("... 13 more sessions ...\n\n");
        
        sb.append("Active API Clients: 3\n");
        sb.append("Client ID               | Application       | Requests/min  | Last Activity\n");
        sb.append("----------------------- | ----------------- | ------------- | ------------------\n");
        sb.append("client_" + UUID.randomUUID().toString().substring(0, 6) + " | Mobile App         | 23           | " + new Date() + "\n");
        sb.append("client_" + UUID.randomUUID().toString().substring(0, 6) + " | Dashboard          | 8            | " + new Date() + "\n");
        sb.append("client_" + UUID.randomUUID().toString().substring(0, 6) + " | Integration        | 45           | " + new Date() + "\n");
        
        return sb.toString();
    }
    
    /**
     * Get monitoring thresholds.
     *
     * @return the monitoring thresholds
     */
    public String getThresholds() {
        StringBuilder sb = new StringBuilder();
        sb.append("Monitoring Thresholds\n");
        sb.append("====================\n\n");
        
        sb.append("System Resource Thresholds:\n");
        for (Map.Entry<String, String> entry : thresholds.entrySet()) {
            String metric = entry.getKey();
            String value = entry.getValue();
            
            // Format value with units
            String formattedValue;
            if (metric.equals("Response Time")) {
                formattedValue = value + "ms";
            } else if (metric.equals("Refresh Interval")) {
                formattedValue = value + "s";
            } else if (metric.equals("Network Connections")) {
                formattedValue = value;
            } else {
                formattedValue = value + "%";
            }
            
            sb.append("  ").append(String.format("%-20s", metric + ":")).append(formattedValue).append("\n");
        }
        
        sb.append("\nAlert Types:\n");
        sb.append("  Warning:   Notification sent to administrators\n");
        sb.append("  Critical:  Notification sent to administrators and on-call team\n");
        sb.append("  Emergency: Notification sent to all stakeholders\n\n");
        
        sb.append("Notification Methods:\n");
        sb.append("  - Email\n");
        sb.append("  - SMS (critical and emergency only)\n");
        sb.append("  - System dashboard\n");
        
        return sb.toString();
    }
}