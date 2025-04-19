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
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Mock implementation of the ComplianceService interface.
 */
public class MockComplianceService implements ComplianceService {
    
    private String framework = "ISO 27001";
    private boolean notificationsEnabled = true;
    private String notificationRecipients = "admin@example.com";
    private final List<String> availableFrameworks = Arrays.asList(
        "ISO 27001", "SOC 2", "HIPAA", "GDPR", "PCI DSS");
    private final Map<String, Map<String, String>> frameworkRequirements = new HashMap<>();
    private final List<Issue> issues = new ArrayList<>();
    private final List<Audit> scheduledAudits = new ArrayList<>();
    private final Map<String, ProjectCompliance> projectComplianceMap = new HashMap<>();
    
    /**
     * Creates a new MockComplianceService with sample data.
     */
    public MockComplianceService() {
        initializeRequirements();
        initializeIssues();
    }
    
    /**
     * Initializes the framework requirements.
     */
    private void initializeRequirements() {
        Map<String, String> iso27001Reqs = new HashMap<>();
        iso27001Reqs.put("A.5.1", "Information security policies");
        iso27001Reqs.put("A.6.1", "Internal organization");
        iso27001Reqs.put("A.6.2", "Mobile devices and teleworking");
        iso27001Reqs.put("A.7.1", "Prior to employment");
        iso27001Reqs.put("A.7.2", "During employment");
        frameworkRequirements.put("ISO 27001", iso27001Reqs);
        
        Map<String, String> soc2Reqs = new HashMap<>();
        soc2Reqs.put("CC1.0", "Control Environment");
        soc2Reqs.put("CC2.0", "Communication and Information");
        soc2Reqs.put("CC3.0", "Risk Assessment");
        soc2Reqs.put("CC4.0", "Monitoring Activities");
        soc2Reqs.put("CC5.0", "Control Activities");
        frameworkRequirements.put("SOC 2", soc2Reqs);
        
        Map<String, String> hipaaReqs = new HashMap<>();
        hipaaReqs.put("164.308", "Administrative safeguards");
        hipaaReqs.put("164.310", "Physical safeguards");
        hipaaReqs.put("164.312", "Technical safeguards");
        hipaaReqs.put("164.314", "Organizational requirements");
        hipaaReqs.put("164.316", "Policies and procedures");
        frameworkRequirements.put("HIPAA", hipaaReqs);
    }
    
    /**
     * Initializes sample compliance issues.
     */
    private void initializeIssues() {
        issues.add(new Issue("ISS-001", "ISO 27001", "A.5.1", "Missing information security policy document",
                             "demo", "OPEN", null, null));
        issues.add(new Issue("ISS-002", "ISO 27001", "A.6.1", "Roles and responsibilities not clearly defined",
                             "demo", "IN_PROGRESS", "Updating role definitions", "2025-04-30"));
        issues.add(new Issue("ISS-003", "ISO 27001", "A.7.2", "User awareness training not documented",
                             "demo", "RESOLVED", "Training program implemented", null));
    }
    
    /**
     * Represents a compliance issue.
     */
    private static class Issue {
        private final String id;
        private final String framework;
        private final String requirementId;
        private final String description;
        private final String projectId;
        private String status;
        private String remediationPlan;
        private String deadline;
        
        public Issue(String id, String framework, String requirementId, String description,
                     String projectId, String status, String remediationPlan, String deadline) {
            this.id = id;
            this.framework = framework;
            this.requirementId = requirementId;
            this.description = description;
            this.projectId = projectId;
            this.status = status;
            this.remediationPlan = remediationPlan;
            this.deadline = deadline;
        }
    }
    
    /**
     * Represents a scheduled compliance audit.
     */
    private static class Audit {
        private final String id;
        private final String projectId;
        private final String auditType;
        private final String scheduledDate;
        
        public Audit(String id, String projectId, String auditType, String scheduledDate) {
            this.id = id;
            this.projectId = projectId;
            this.auditType = auditType;
            this.scheduledDate = scheduledDate;
        }
    }
    
    /**
     * Represents project-specific compliance configuration.
     */
    private static class ProjectCompliance {
        private final String projectId;
        private List<String> frameworks;
        private String reviewer;
        
        public ProjectCompliance(String projectId, List<String> frameworks, String reviewer) {
            this.projectId = projectId;
            this.frameworks = frameworks;
            this.reviewer = reviewer;
        }
    }
    
    @Override
    public String getComplianceStatus() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Compliance Status\n");
        sb.append("=================\n\n");
        
        sb.append("Active Framework: ").append(framework).append("\n\n");
        
        sb.append("Compliance Summary:\n");
        sb.append("- Total requirements: ").append(frameworkRequirements.get(framework).size()).append("\n");
        sb.append("- Compliant requirements: 3\n");
        sb.append("- Non-compliant requirements: 2\n");
        sb.append("- Compliance percentage: 60%\n\n");
        
        sb.append("Issues Summary:\n");
        sb.append("- Open issues: 1\n");
        sb.append("- In-progress issues: 1\n");
        sb.append("- Resolved issues: 1\n\n");
        
        sb.append("Next scheduled audit: 2025-05-15 (Full)\n");
        
        return sb.toString();
    }
    
    @Override
    public String getComplianceFramework() {
        return framework;
    }
    
    @Override
    public boolean setComplianceFramework(String framework) {
        if (availableFrameworks.contains(framework)) {
            this.framework = framework;
            return true;
        }
        return false;
    }
    
    @Override
    public String generateReport(String reportType) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Compliance Report: ").append(reportType).append("\n");
        sb.append("==================");
        for (int i = 0; i < reportType.length(); i++) {
            sb.append("=");
        }
        sb.append("\n\n");
        
        sb.append("Date: ").append(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
        sb.append("Framework: ").append(framework).append("\n\n");
        
        if ("summary".equalsIgnoreCase(reportType)) {
            sb.append("Compliance Summary:\n");
            sb.append("- Total requirements: ").append(frameworkRequirements.get(framework).size()).append("\n");
            sb.append("- Compliant requirements: 3\n");
            sb.append("- Non-compliant requirements: 2\n");
            sb.append("- Compliance percentage: 60%\n\n");
            
            sb.append("Top Issues:\n");
            sb.append("1. Missing information security policy document\n");
            sb.append("2. Roles and responsibilities not clearly defined\n");
        } else if ("detailed".equalsIgnoreCase(reportType)) {
            sb.append("Detailed Requirements Status:\n");
            sb.append("----------------------------\n\n");
            
            for (Map.Entry<String, String> req : frameworkRequirements.get(framework).entrySet()) {
                sb.append("Requirement ").append(req.getKey()).append(": ").append(req.getValue()).append("\n");
                if (req.getKey().equals("A.5.1") || req.getKey().equals("A.6.1")) {
                    sb.append("Status: Non-compliant\n");
                    sb.append("Issue: ");
                    if (req.getKey().equals("A.5.1")) {
                        sb.append("Missing information security policy document\n");
                    } else {
                        sb.append("Roles and responsibilities not clearly defined\n");
                    }
                } else {
                    sb.append("Status: Compliant\n");
                }
                sb.append("\n");
            }
        } else if ("financial".equalsIgnoreCase(reportType)) {
            sb.append("Financial Compliance Report\n");
            sb.append("---------------------------\n\n");
            
            sb.append("Financial Control Assessment:\n");
            sb.append("- Segregation of duties: Compliant\n");
            sb.append("- Authorization procedures: Compliant\n");
            sb.append("- Audit trail maintenance: Non-compliant\n");
            sb.append("- Financial reporting controls: Compliant\n\n");
            
            sb.append("Risk Assessment:\n");
            sb.append("- Financial reporting risk: Low\n");
            sb.append("- Fraud risk: Low\n");
            sb.append("- Operational risk: Medium\n");
        } else {
            sb.append("Unknown report type: ").append(reportType).append("\n");
            sb.append("Available report types: summary, detailed, financial\n");
        }
        
        return sb.toString();
    }
    
    @Override
    public String validateProject(String projectId) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Project Compliance Validation: ").append(projectId).append("\n");
        sb.append("===============================");
        for (int i = 0; i < projectId.length(); i++) {
            sb.append("=");
        }
        sb.append("\n\n");
        
        sb.append("Framework: ").append(framework).append("\n");
        sb.append("Date: ").append(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n\n");
        
        sb.append("Validation Results:\n");
        
        int issueCount = 0;
        for (Issue issue : issues) {
            if (issue.projectId.equals(projectId) && !issue.status.equals("RESOLVED")) {
                issueCount++;
                sb.append(issueCount).append(". [").append(issue.requirementId).append("] ")
                  .append(issue.description).append(" (").append(issue.status).append(")\n");
                
                if (issue.remediationPlan != null) {
                    sb.append("   Remediation: ").append(issue.remediationPlan);
                    if (issue.deadline != null) {
                        sb.append(" (Due: ").append(issue.deadline).append(")");
                    }
                    sb.append("\n");
                }
            }
        }
        
        if (issueCount == 0) {
            sb.append("No compliance issues found for this project.\n");
        }
        
        sb.append("\nOverall Compliance: ");
        if (issueCount == 0) {
            sb.append("COMPLIANT");
        } else {
            sb.append("NON-COMPLIANT (").append(issueCount).append(" issues)");
        }
        
        return sb.toString();
    }
    
    @Override
    public List<String> listReports() {
        return Arrays.asList("summary", "detailed", "financial", "risk", "security", "privacy");
    }
    
    @Override
    public List<String> listFrameworks() {
        return new ArrayList<>(availableFrameworks);
    }
    
    @Override
    public Map<String, String> getFrameworkRequirements(String framework) {
        return new HashMap<>(frameworkRequirements.getOrDefault(framework, new HashMap<>()));
    }
    
    @Override
    public String scheduleAudit(String projectId, String auditType, String scheduledDate) {
        String auditId = "AUD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        scheduledAudits.add(new Audit(auditId, projectId, auditType, scheduledDate));
        return auditId;
    }
    
    @Override
    public String listScheduledAudits() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Scheduled Compliance Audits\n");
        sb.append("==========================\n\n");
        
        if (scheduledAudits.isEmpty()) {
            sb.append("No audits currently scheduled.\n");
            return sb.toString();
        }
        
        sb.append(String.format("%-10s | %-10s | %-15s | %s\n",
                "ID", "Project", "Type", "Scheduled Date"));
        sb.append(String.format("%-10s-|-%-10s-|-%-15s-|-%s\n",
                "----------", "----------", "---------------", "---------------"));
        
        for (Audit audit : scheduledAudits) {
            sb.append(String.format("%-10s | %-10s | %-15s | %s\n",
                    audit.id, audit.projectId, audit.auditType, audit.scheduledDate));
        }
        
        return sb.toString();
    }
    
    @Override
    public String getPendingIssues() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Pending Compliance Issues\n");
        sb.append("========================\n\n");
        
        boolean hasPendingIssues = false;
        for (Issue issue : issues) {
            if (!issue.status.equals("RESOLVED")) {
                hasPendingIssues = true;
                sb.append("Issue: ").append(issue.id).append("\n");
                sb.append("Requirement: [").append(issue.framework).append("] ")
                  .append(issue.requirementId).append("\n");
                sb.append("Description: ").append(issue.description).append("\n");
                sb.append("Project: ").append(issue.projectId).append("\n");
                sb.append("Status: ").append(issue.status).append("\n");
                
                if (issue.remediationPlan != null) {
                    sb.append("Remediation: ").append(issue.remediationPlan).append("\n");
                    if (issue.deadline != null) {
                        sb.append("Deadline: ").append(issue.deadline).append("\n");
                    }
                }
                
                sb.append("\n");
            }
        }
        
        if (!hasPendingIssues) {
            sb.append("No pending compliance issues found.\n");
        }
        
        return sb.toString();
    }
    
    @Override
    public boolean configureNotifications(boolean enabled, String recipients) {
        this.notificationsEnabled = enabled;
        if (recipients != null && !recipients.isEmpty()) {
            this.notificationRecipients = recipients;
        }
        return true;
    }
    
    @Override
    public boolean setRemediationPlan(String issueId, String plan, String deadline) {
        for (Issue issue : issues) {
            if (issue.id.equals(issueId)) {
                issue.remediationPlan = plan;
                issue.deadline = deadline;
                issue.status = "IN_PROGRESS";
                return true;
            }
        }
        return false;
    }
    
    @Override
    public boolean resolveIssue(String issueId, String resolutionNotes) {
        for (Issue issue : issues) {
            if (issue.id.equals(issueId)) {
                issue.status = "RESOLVED";
                issue.remediationPlan = resolutionNotes;
                return true;
            }
        }
        return false;
    }
    
    /**
     * Generates a compliance report for a specific framework and time period.
     *
     * @param framework the compliance framework
     * @param period the reporting period
     * @return formatted compliance report
     */
    public String generateComplianceReport(String framework, String period) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Compliance Report: ").append(framework).append("\n");
        sb.append("===================");
        for (int i = 0; i < framework.length(); i++) {
            sb.append("=");
        }
        sb.append("\n\n");
        
        sb.append("Reporting Period: ").append(period).append("\n");
        sb.append("Date Generated: ").append(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n\n");
        
        sb.append("Framework Details:\n");
        sb.append("- Name: ").append(framework).append("\n");
        sb.append("- Type: Regulatory Compliance\n");
        sb.append("- Authority: ");
        switch (framework.toUpperCase()) {
            case "GDPR":
                sb.append("European Union");
                break;
            case "HIPAA":
                sb.append("U.S. Department of Health and Human Services");
                break;
            case "SOC2":
                sb.append("American Institute of CPAs (AICPA)");
                break;
            case "PCI-DSS":
                sb.append("Payment Card Industry Security Standards Council");
                break;
            case "ISO27001":
                sb.append("International Organization for Standardization (ISO)");
                break;
            default:
                sb.append("Unknown");
        }
        sb.append("\n\n");
        
        // Report overview
        sb.append("Compliance Overview:\n");
        sb.append("-------------------\n");
        sb.append("Controls Assessed: 25\n");
        sb.append("Controls Passed: 18\n");
        sb.append("Controls Failed: 5\n");
        sb.append("Controls N/A: 2\n");
        sb.append("Overall Compliance: 78%\n\n");
        
        // Critical findings
        sb.append("Critical Findings:\n");
        sb.append("-----------------\n");
        sb.append("1. Access control procedures not documented (Section A.9.2)\n");
        sb.append("2. Regular security testing not performed (Section A.12.6)\n");
        sb.append("3. Incident response plan incomplete (Section A.16.1)\n\n");
        
        // Recommendations
        sb.append("Recommendations:\n");
        sb.append("---------------\n");
        sb.append("1. Document and implement formal access control procedures\n");
        sb.append("2. Schedule and perform quarterly security testing\n");
        sb.append("3. Complete and test the incident response plan\n");
        sb.append("4. Regular compliance training for all staff\n\n");
        
        sb.append("Next Steps:\n");
        sb.append("----------\n");
        sb.append("1. Remediate critical findings within 30 days\n");
        sb.append("2. Schedule follow-up assessment for ").append(LocalDate.now().plusMonths(3).format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
        sb.append("3. Conduct compliance training session\n");
        
        return sb.toString();
    }
    
    /**
     * Configures project compliance settings.
     *
     * @param projectName the project name
     * @param frameworks the compliance frameworks to apply
     * @param reviewer the compliance reviewer
     * @return true if successful
     */
    public boolean configureProjectCompliance(String projectName, List<String> frameworks, String reviewer) {
        ProjectCompliance compliance = projectComplianceMap.get(projectName);
        
        if (compliance == null) {
            compliance = new ProjectCompliance(projectName, frameworks, reviewer);
            projectComplianceMap.put(projectName, compliance);
        } else {
            compliance.frameworks = frameworks;
            compliance.reviewer = reviewer;
        }
        
        return true;
    }
    
    /**
     * Validates a project against compliance requirements.
     *
     * @param projectName the project name
     * @return validation results
     */
    public String validateProjectCompliance(String projectName) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Project Compliance Validation: ").append(projectName).append("\n");
        sb.append("===============================");
        for (int i = 0; i < projectName.length(); i++) {
            sb.append("=");
        }
        sb.append("\n\n");
        
        ProjectCompliance compliance = projectComplianceMap.get(projectName);
        
        if (compliance == null) {
            sb.append("Project ").append(projectName).append(" has not been configured for compliance.\n");
            sb.append("Use 'rin admin compliance configure --project=").append(projectName).append("' to set up compliance requirements.\n");
            return sb.toString();
        }
        
        sb.append("Project Details:\n");
        sb.append("- Name: ").append(projectName).append("\n");
        sb.append("- Compliance Frameworks: ").append(String.join(", ", compliance.frameworks)).append("\n");
        sb.append("- Reviewer: ").append(compliance.reviewer).append("\n\n");
        
        sb.append("Validation Results:\n");
        sb.append("------------------\n");
        
        // Generate some mock validation results
        List<String> passedChecks = Arrays.asList(
            "Documentation meets standard requirements",
            "Access control implemented properly",
            "Data retention policies defined"
        );
        
        List<String> failedChecks = Arrays.asList(
            "Security testing not performed in last quarter",
            "Privacy impact assessment missing"
        );
        
        int passCount = passedChecks.size();
        int failCount = failedChecks.size();
        int totalChecks = passCount + failCount;
        
        sb.append("Total checks performed: ").append(totalChecks).append("\n");
        sb.append("Passed: ").append(passCount).append(" (").append(passCount * 100 / totalChecks).append("%)\n");
        sb.append("Failed: ").append(failCount).append(" (").append(failCount * 100 / totalChecks).append("%)\n\n");
        
        sb.append("Passed Checks:\n");
        for (int i = 0; i < passedChecks.size(); i++) {
            sb.append("✓ ").append(passedChecks.get(i)).append("\n");
        }
        
        sb.append("\nFailed Checks:\n");
        for (int i = 0; i < failedChecks.size(); i++) {
            sb.append("✗ ").append(failedChecks.get(i)).append("\n");
        }
        
        sb.append("\nOverall Compliance Status: ");
        if (failCount == 0) {
            sb.append("COMPLIANT");
        } else if (failCount <= 2) {
            sb.append("PARTIALLY COMPLIANT");
        } else {
            sb.append("NON-COMPLIANT");
        }
        
        return sb.toString();
    }
    
    /**
     * Gets compliance status for a specific project.
     *
     * @param projectName the project name
     * @return formatted compliance status
     */
    public String getProjectComplianceStatus(String projectName) {
        StringBuilder sb = new StringBuilder();
        
        sb.append("Project Compliance Status: ").append(projectName).append("\n");
        sb.append("============================");
        for (int i = 0; i < projectName.length(); i++) {
            sb.append("=");
        }
        sb.append("\n\n");
        
        ProjectCompliance compliance = projectComplianceMap.get(projectName);
        
        if (compliance == null) {
            sb.append("Project ").append(projectName).append(" has not been configured for compliance.\n");
            sb.append("Use 'rin admin compliance configure --project=").append(projectName).append("' to set up compliance requirements.\n");
            return sb.toString();
        }
        
        sb.append("Frameworks: ").append(String.join(", ", compliance.frameworks)).append("\n");
        sb.append("Reviewer: ").append(compliance.reviewer).append("\n\n");
        
        // Calculate stats based on issues related to this project
        int openIssues = 0;
        int inProgressIssues = 0;
        int resolvedIssues = 0;
        
        for (Issue issue : issues) {
            if (issue.projectId.equals(projectName)) {
                if ("OPEN".equals(issue.status)) {
                    openIssues++;
                } else if ("IN_PROGRESS".equals(issue.status)) {
                    inProgressIssues++;
                } else if ("RESOLVED".equals(issue.status)) {
                    resolvedIssues++;
                }
            }
        }
        
        int totalIssues = openIssues + inProgressIssues + resolvedIssues;
        
        sb.append("Issues Summary:\n");
        sb.append("- Open issues: ").append(openIssues).append("\n");
        sb.append("- In-progress issues: ").append(inProgressIssues).append("\n");
        sb.append("- Resolved issues: ").append(resolvedIssues).append("\n");
        sb.append("- Total issues: ").append(totalIssues).append("\n\n");
        
        // Calculate compliance percentage
        int compliancePercentage;
        if (totalIssues == 0) {
            compliancePercentage = 100;
        } else {
            compliancePercentage = (resolvedIssues * 100) / totalIssues;
        }
        
        sb.append("Last Assessment: ").append(LocalDate.now().minusMonths(1).format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
        sb.append("Next Scheduled Assessment: ").append(LocalDate.now().plusMonths(2).format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n\n");
        
        sb.append("Compliance Status: ");
        if (compliancePercentage >= 90) {
            sb.append("COMPLIANT (").append(compliancePercentage).append("%)");
        } else if (compliancePercentage >= 70) {
            sb.append("PARTIALLY COMPLIANT (").append(compliancePercentage).append("%)");
        } else {
            sb.append("NON-COMPLIANT (").append(compliancePercentage).append("%)");
        }
        
        return sb.toString();
    }
    
    /**
     * Gets system-wide compliance status.
     *
     * @return formatted system compliance status
     */
    public String getSystemComplianceStatus() {
        StringBuilder sb = new StringBuilder();
        
        sb.append("System Compliance Status\n");
        sb.append("=======================\n\n");
        
        sb.append("Default Framework: ").append(framework).append("\n\n");
        
        // Count projects with compliance configured
        int configuredProjects = projectComplianceMap.size();
        
        sb.append("Projects Summary:\n");
        sb.append("- Projects with compliance configured: ").append(configuredProjects).append("\n");
        
        // Calculate issues by status
        int openIssues = 0;
        int inProgressIssues = 0;
        int resolvedIssues = 0;
        
        for (Issue issue : issues) {
            if ("OPEN".equals(issue.status)) {
                openIssues++;
            } else if ("IN_PROGRESS".equals(issue.status)) {
                inProgressIssues++;
            } else if ("RESOLVED".equals(issue.status)) {
                resolvedIssues++;
            }
        }
        
        int totalIssues = openIssues + inProgressIssues + resolvedIssues;
        
        sb.append("\nIssues Summary:\n");
        sb.append("- Open issues: ").append(openIssues).append("\n");
        sb.append("- In-progress issues: ").append(inProgressIssues).append("\n");
        sb.append("- Resolved issues: ").append(resolvedIssues).append("\n");
        sb.append("- Total issues: ").append(totalIssues).append("\n\n");
        
        // Calculate system compliance percentage
        int compliancePercentage;
        if (totalIssues == 0) {
            compliancePercentage = 100;
        } else {
            compliancePercentage = (resolvedIssues * 100) / totalIssues;
        }
        
        sb.append("Scheduled Assessments: ").append(scheduledAudits.size()).append("\n");
        sb.append("Next Scheduled Assessment: ");
        if (scheduledAudits.isEmpty()) {
            sb.append("None");
        } else {
            sb.append(scheduledAudits.get(0).scheduledDate);
        }
        sb.append("\n\n");
        
        sb.append("System Compliance Status: ");
        if (compliancePercentage >= 90) {
            sb.append("COMPLIANT (").append(compliancePercentage).append("%)");
        } else if (compliancePercentage >= 70) {
            sb.append("PARTIALLY COMPLIANT (").append(compliancePercentage).append("%)");
        } else {
            sb.append("NON-COMPLIANT (").append(compliancePercentage).append("%)");
        }
        
        return sb.toString();
    }
}