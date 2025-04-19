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
 * Interface for compliance management services.
 */
public interface ComplianceService {
    
    /**
     * Gets the current compliance status.
     *
     * @return formatted compliance status
     */
    String getComplianceStatus();
    
    /**
     * Gets the current compliance framework.
     *
     * @return the compliance framework name
     */
    String getComplianceFramework();
    
    /**
     * Sets the compliance framework.
     *
     * @param framework the framework name
     * @return true if successful
     */
    boolean setComplianceFramework(String framework);
    
    /**
     * Generates a compliance report.
     *
     * @param reportType the report type
     * @return the report content
     */
    String generateReport(String reportType);
    
    /**
     * Validates project compliance.
     *
     * @param projectId the project ID
     * @return validation results
     */
    String validateProject(String projectId);
    
    /**
     * Lists all available compliance reports.
     *
     * @return list of available reports
     */
    List<String> listReports();
    
    /**
     * Lists all compliance frameworks.
     *
     * @return list of available frameworks
     */
    List<String> listFrameworks();
    
    /**
     * Gets requirements for a specific framework.
     *
     * @param framework the framework name
     * @return map of requirement IDs to descriptions
     */
    Map<String, String> getFrameworkRequirements(String framework);
    
    /**
     * Schedules a compliance audit.
     *
     * @param projectId the project ID
     * @param auditType the audit type
     * @param scheduledDate the scheduled date (YYYY-MM-DD)
     * @return the audit ID
     */
    String scheduleAudit(String projectId, String auditType, String scheduledDate);
    
    /**
     * Lists scheduled compliance audits.
     *
     * @return formatted list of scheduled audits
     */
    String listScheduledAudits();
    
    /**
     * Gets the list of pending compliance issues.
     *
     * @return formatted list of compliance issues
     */
    String getPendingIssues();
    
    /**
     * Configures compliance notification settings.
     *
     * @param enabled whether notifications are enabled
     * @param recipients the notification recipients (comma-separated)
     * @return true if successful
     */
    boolean configureNotifications(boolean enabled, String recipients);
    
    /**
     * Sets a remediation plan for a compliance issue.
     *
     * @param issueId the issue ID
     * @param plan the remediation plan
     * @param deadline the deadline date (YYYY-MM-DD)
     * @return true if successful
     */
    boolean setRemediationPlan(String issueId, String plan, String deadline);
    
    /**
     * Marks a compliance issue as resolved.
     *
     * @param issueId the issue ID
     * @param resolutionNotes notes about the resolution
     * @return true if successful
     */
    boolean resolveIssue(String issueId, String resolutionNotes);
    
    /**
     * Generates a compliance report for a specific framework and time period.
     *
     * @param framework the compliance framework
     * @param period the reporting period
     * @return formatted compliance report
     */
    String generateComplianceReport(String framework, String period);
    
    /**
     * Configures project compliance settings.
     *
     * @param projectName the project name
     * @param frameworks the compliance frameworks to apply
     * @param reviewer the compliance reviewer
     * @return true if successful
     */
    boolean configureProjectCompliance(String projectName, List<String> frameworks, String reviewer);
    
    /**
     * Validates a project against compliance requirements.
     *
     * @param projectName the project name
     * @return validation results
     */
    String validateProjectCompliance(String projectName);
    
    /**
     * Gets compliance status for a specific project.
     *
     * @param projectName the project name
     * @return formatted compliance status
     */
    String getProjectComplianceStatus(String projectName);
    
    /**
     * Gets system-wide compliance status.
     *
     * @return formatted system compliance status
     */
    String getSystemComplianceStatus();
}