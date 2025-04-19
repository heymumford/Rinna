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

import java.util.List;
import java.util.Map;

/**
 * Interface for system diagnostics services.
 */
public interface DiagnosticsService {
    
    /**
     * Run diagnostics with the specified depth.
     *
     * @param full whether to run full diagnostics
     * @return the formatted diagnostic results
     */
    String runDiagnostics(boolean full);
    
    /**
     * Get a list of scheduled diagnostics.
     *
     * @return the formatted list of scheduled diagnostics
     */
    String listScheduledDiagnostics();
    
    /**
     * Schedule diagnostics with the specified parameters.
     *
     * @param checks list of diagnostic checks to schedule
     * @param frequency the schedule frequency
     * @param time the schedule time
     * @param recipients list of email recipients for notifications
     * @return the ID of the scheduled task
     */
    String scheduleDiagnostics(List<String> checks, String frequency, String time, List<String> recipients);
    
    /**
     * Analyze database performance.
     *
     * @return the formatted database performance report
     */
    String analyzeDatabasePerformance();
    
    /**
     * Get details for a specific warning.
     *
     * @param warningId the ID of the warning
     * @return a map of warning details
     */
    Map<String, String> getWarningDetails(String warningId);
    
    /**
     * Get available actions for a specific warning.
     *
     * @param warningId the ID of the warning
     * @return list of available actions
     */
    List<String> getAvailableWarningActions(String warningId);
    
    /**
     * Perform an action to address a warning.
     *
     * @param warningId the ID of the warning
     * @param action the action to perform
     * @return true if the action was successful, false otherwise
     */
    boolean performWarningAction(String warningId, String action);
    
    /**
     * Perform memory reclamation.
     *
     * @return true if the operation was successful, false otherwise
     */
    boolean performMemoryReclamation();
}