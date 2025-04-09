/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.adapter;

import org.rinna.cli.domain.service.MonitoringService;
import org.rinna.cli.service.MockMonitoringService;

import java.util.List;

/**
 * Adapter for the MonitoringService that bridges between the domain MonitoringService and
 * the CLI MockMonitoringService. This adapter implements the domain MonitoringService interface
 * while delegating to the CLI MockMonitoringService implementation.
 */
public class MonitoringServiceAdapter implements MonitoringService {
    
    private final MockMonitoringService mockMonitoringService;
    
    /**
     * Constructs a new MonitoringServiceAdapter with the specified mock monitoring service.
     *
     * @param mockMonitoringService the mock monitoring service to delegate to
     */
    public MonitoringServiceAdapter(MockMonitoringService mockMonitoringService) {
        this.mockMonitoringService = mockMonitoringService;
    }
    
    @Override
    public String getDashboard() {
        return mockMonitoringService.getDashboard();
    }
    
    @Override
    public String getServerMetrics(boolean detailed) {
        return mockMonitoringService.getServerMetrics(detailed);
    }
    
    @Override
    public boolean configureThreshold(String metric, String value) {
        return mockMonitoringService.configureThreshold(metric, value);
    }
    
    @Override
    public String generateReport(String period) {
        return mockMonitoringService.generateReport(period);
    }
    
    @Override
    public boolean addAlert(String name, String metric, String threshold, List<String> recipients) {
        return mockMonitoringService.addAlert(name, metric, threshold, recipients);
    }
    
    @Override
    public String listAlerts() {
        return mockMonitoringService.listAlerts();
    }
    
    @Override
    public boolean removeAlert(String name) {
        return mockMonitoringService.removeAlert(name);
    }
    
    @Override
    public String getActiveSessions() {
        return mockMonitoringService.getActiveSessions();
    }
    
    @Override
    public String getThresholds() {
        return mockMonitoringService.getThresholds();
    }
}