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

import java.time.LocalDate;
import java.util.List;

/**
 * Interface for audit services.
 */
public interface AuditService {
    
    /**
     * Lists audit logs based on filters.
     *
     * @param user Optional username filter
     * @param days Optional number of days to include
     * @param limit Optional limit on number of results
     * @return Formatted log listing
     */
    String listAuditLogs(String user, Integer days, Integer limit);
    
    /**
     * Configures the retention period for audit logs.
     *
     * @param days The number of days to retain logs
     * @return True if successful
     */
    boolean configureRetention(Integer days);
    
    /**
     * Gets the current status of the audit system.
     *
     * @return Formatted status information
     */
    String getAuditStatus();
    
    /**
     * Exports audit logs to a file.
     *
     * @param fromDate The start date for export
     * @param toDate The end date for export
     * @param format The export format (csv, json, pdf)
     * @return The path to the exported file
     */
    String exportAuditLogs(LocalDate fromDate, LocalDate toDate, String format);
    
    /**
     * Configures masking for sensitive data in audit logs.
     *
     * @param fields List of field names to mask
     * @return True if successful
     */
    boolean configureMasking(List<String> fields);
    
    /**
     * Gets the current masking configuration status.
     *
     * @return Formatted masking status
     */
    String getMaskingStatus();
    
    /**
     * Adds a new audit alert.
     *
     * @param name The alert name
     * @param events The event types to trigger on
     * @param threshold The threshold count
     * @param window The time window in minutes
     * @param recipients The notification recipients
     * @return True if successful
     */
    boolean addAlert(String name, List<String> events, int threshold, int window, List<String> recipients);
    
    /**
     * Lists all configured audit alerts.
     *
     * @return Formatted alert listing
     */
    String listAlerts();
    
    /**
     * Removes an audit alert.
     *
     * @param name The alert name
     * @return True if successful
     */
    boolean removeAlert(String name);
    
    /**
     * Creates a new security investigation.
     *
     * @param user The username to investigate
     * @param days The number of days of history to include
     * @return The case ID
     */
    String createInvestigation(String user, Integer days);
    
    /**
     * Gets the findings for a security investigation.
     *
     * @param caseId The case ID
     * @return Formatted findings
     */
    String getInvestigationFindings(String caseId);
    
    /**
     * Performs an action as part of a security investigation.
     *
     * @param action The action to perform
     * @param user The target username
     * @return True if successful
     */
    boolean performInvestigationAction(String action, String user);
}