package org.rinna.cli.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.rinna.cli.service.ComplianceService;
import org.rinna.cli.service.MockComplianceService;
import org.rinna.cli.service.MockSecurityService;
import org.rinna.cli.service.ServiceManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

import java.util.Arrays;

/**
 * Step definitions for admin compliance command tests.
 */
public class AdminComplianceCommandSteps {
    
    private final TestContext context;
    private final CommandProcessor commandProcessor;
    private final MockSecurityService mockSecurityService;
    private final MockComplianceService mockComplianceService;
    
    /**
     * Constructor with test context.
     * 
     * @param context the test context
     */
    public AdminComplianceCommandSteps(TestContext context) {
        this.context = context;
        this.commandProcessor = context.getCommandProcessor();
        this.mockSecurityService = context.getMockSecurityService();
        this.mockComplianceService = context.getMockComplianceService();
    }
    
    @Given("a project {string} is configured for compliance")
    public void a_project_is_configured_for_compliance(String projectName) {
        // Mock the configureProjectCompliance method to indicate that the project is configured
        when(mockComplianceService.configureProjectCompliance(
                eq(projectName), 
                anyList(), 
                anyString()))
            .thenReturn(true);
        
        // Mock the project validation result
        when(mockComplianceService.validateProjectCompliance(projectName))
            .thenReturn("Project Compliance Validation: " + projectName + "\n" +
                         "===============================" + "=".repeat(projectName.length()) + "\n\n" +
                         "Project Details:\n" +
                         "- Name: " + projectName + "\n" +
                         "- Compliance Frameworks: GDPR, HIPAA\n" +
                         "- Reviewer: compliance-reviewer\n\n" +
                         "Validation Results:\n" +
                         "------------------\n" +
                         "Total checks performed: 5\n" +
                         "Passed: 3 (60%)\n" +
                         "Failed: 2 (40%)\n\n" +
                         "Passed Checks:\n" +
                         "✓ Documentation meets standard requirements\n" +
                         "✓ Access control implemented properly\n" +
                         "✓ Data retention policies defined\n\n" +
                         "Failed Checks:\n" +
                         "✗ Security testing not performed in last quarter\n" +
                         "✗ Privacy impact assessment missing\n\n" +
                         "Overall Compliance Status: PARTIALLY COMPLIANT");
        
        // Mock the project compliance status
        when(mockComplianceService.getProjectComplianceStatus(projectName))
            .thenReturn("Project Compliance Status: " + projectName + "\n" +
                         "============================" + "=".repeat(projectName.length()) + "\n\n" +
                         "Frameworks: GDPR, HIPAA\n" +
                         "Reviewer: compliance-reviewer\n\n" +
                         "Issues Summary:\n" +
                         "- Open issues: 1\n" +
                         "- In-progress issues: 1\n" +
                         "- Resolved issues: 2\n" +
                         "- Total issues: 4\n\n" +
                         "Last Assessment: 2025-03-08\n" +
                         "Next Scheduled Assessment: 2025-06-08\n\n" +
                         "Compliance Status: PARTIALLY COMPLIANT (50%)");
    }
    
    // These step definitions are shared with other step definition classes
    // If you already have them defined elsewhere, you can remove the duplicate methods
    
    @Given("I am logged in as an administrator")
    public void i_am_logged_in_as_an_administrator() {
        mockSecurityService.login("admin", "admin123");
        mockSecurityService.setAdminStatus(true);
    }
    
    @Given("I am logged in as a regular user")
    public void i_am_logged_in_as_a_regular_user() {
        mockSecurityService.login("user", "password");
        mockSecurityService.setAdminStatus(false);
    }
}