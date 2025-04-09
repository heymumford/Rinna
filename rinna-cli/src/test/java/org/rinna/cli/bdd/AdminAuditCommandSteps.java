package org.rinna.cli.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.rinna.cli.service.AuditService;
import org.rinna.cli.service.MockAuditService;
import org.rinna.cli.service.MockSecurityService;
import org.rinna.cli.service.ServiceManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

/**
 * Step definitions for admin audit command tests.
 */
public class AdminAuditCommandSteps {
    
    private final TestContext context;
    private final CommandProcessor commandProcessor;
    private final MockSecurityService mockSecurityService;
    private final MockAuditService mockAuditService;
    private String investigationCaseId;
    
    /**
     * Constructor with test context.
     * 
     * @param context the test context
     */
    public AdminAuditCommandSteps(TestContext context) {
        this.context = context;
        this.commandProcessor = context.getCommandProcessor();
        this.mockSecurityService = context.getMockSecurityService();
        this.mockAuditService = context.getMockAuditService();
    }
    
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
    
    @Given("a security investigation exists with case ID {string} for user {string}")
    public void a_security_investigation_exists_with_case_id_for_user(String caseId, String username) {
        this.investigationCaseId = caseId;
        // Create a mock investigation
        when(mockAuditService.getInvestigationFindings(caseId))
            .thenReturn("Investigation Findings: " + caseId + "\n" +
                         "============================\n\n" +
                         "Subject: " + username + "\n" +
                         "Period: Last 14 days\n\n" +
                         "Activity Summary:\n" +
                         "- Login attempts: 12 (10 successful, 2 failed)\n" +
                         "- Resource access: 45 operations\n" +
                         "- Administrative actions: 3\n" +
                         "- Data exports: 1\n\n" +
                         "Findings:\n" +
                         "- No suspicious login patterns detected\n" +
                         "- Normal access patterns to resources\n" +
                         "- All administrative actions properly authorized\n");
    }
    
    @When("I run {string} with input:")
    public void i_run_command_with_input(String command, String input) {
        // Save the current System.in
        System.setIn(new java.io.ByteArrayInputStream(input.getBytes()));
        
        // Process the command
        context.setExitCode(commandProcessor.processCommand(command));
        
        // Restore the original System.in
        System.setIn(System.in);
    }
    
    // These step definitions may be shared with other step definition classes
    // If you already have them defined elsewhere, you can remove them here
    
    @Then("the command should succeed")
    public void the_command_should_succeed() {
        assertEquals(0, context.getLastCommandExitCode(), 
                "Command should succeed with exit code 0, but got " + context.getLastCommandExitCode() +
                "\nOutput: " + context.getOutputContent());
    }
    
    @Then("the command should fail")
    public void the_command_should_fail() {
        assertTrue(context.getLastCommandExitCode() > 0, 
                "Command should fail with non-zero exit code, but got " + context.getLastCommandExitCode() +
                "\nOutput: " + context.getOutputContent());
    }
    
    @Then("the output should contain {string}")
    public void the_output_should_contain(String expected) {
        String output = context.getOutputContent();
        assertTrue(output.contains(expected), 
                "Output should contain '" + expected + "' but got: " + output);
    }
}