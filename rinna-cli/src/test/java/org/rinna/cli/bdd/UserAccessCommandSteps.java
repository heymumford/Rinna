/*
 * BDD Step definitions for User Access commands
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.bdd;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.When;
import io.cucumber.java.en.Then;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.rinna.cli.service.MockSecurityService;

/**
 * Step definitions for user access command tests.
 */
public class UserAccessCommandSteps {
    
    private final TestContext testContext = TestContext.getInstance();
    private final MockSecurityService mockSecurityService;
    
    /**
     * Constructor initializes test context and mock services.
     */
    public UserAccessCommandSteps() {
        mockSecurityService = testContext.getMockSecurityService();
    }
    
    /**
     * Set up a scenario where the user is an administrator.
     */
    @Given("the current user is authenticated as an administrator")
    public void theCurrentUserIsAuthenticatedAsAnAdministrator() {
        mockSecurityService.setAuthenticated(true);
        mockSecurityService.setAdmin(true);
        mockSecurityService.setCurrentUser("admin");
    }
    
    /**
     * Set up a scenario where the user is authenticated but not an administrator.
     */
    @Given("the current user is authenticated but not an administrator")
    public void theCurrentUserIsAuthenticatedButNotAnAdministrator() {
        mockSecurityService.setAuthenticated(true);
        mockSecurityService.setAdmin(false);
        mockSecurityService.setCurrentUser("regularuser");
    }
    
    /**
     * Set up a scenario where the user is not authenticated.
     */
    @Given("the current user is not authenticated")
    public void theCurrentUserIsNotAuthenticated() {
        mockSecurityService.setAuthenticated(false);
        mockSecurityService.setAdmin(false);
        mockSecurityService.setCurrentUser(null);
    }
    
    /**
     * Execute a command and capture the output.
     */
    @When("I run the command {string}")
    public void iRunTheCommand(String commandLine) {
        CommandProcessor processor = testContext.getCommandProcessor();
        
        // Setup the mock methods for the security service
        setupMockSecurityService();
        
        // Execute the command
        testContext.redirectConsoleOutput();
        processor.processCommand(commandLine);
        testContext.restoreConsoleOutput();
        
        // Capture output for assertions
        String output = testContext.getStandardOutput() + testContext.getErrorOutput();
        testContext.setLastCommandOutput(output);
    }
    
    /**
     * Verify the command execution was successful.
     */
    @Then("the command should execute successfully")
    public void theCommandShouldExecuteSuccessfully() {
        Assertions.assertEquals(0, testContext.getLastCommandExitCode(), 
            "Expected command to succeed with exit code 0, but got " + testContext.getLastCommandExitCode());
    }
    
    /**
     * Verify the command execution failed with a specific exit code.
     */
    @Then("the command should fail with exit code {int}")
    public void theCommandShouldFailWithExitCode(int expectedExitCode) {
        Assertions.assertEquals(expectedExitCode, testContext.getLastCommandExitCode(), 
            "Expected command to fail with exit code " + expectedExitCode + 
            ", but got " + testContext.getLastCommandExitCode());
    }
    
    /**
     * Verify that the output contains a specific text.
     */
    @Then("the output should contain {string}")
    public void theOutputShouldContain(String expectedText) {
        Assertions.assertTrue(testContext.getLastCommandOutput().contains(expectedText), 
            "Expected output to contain '" + expectedText + "', but it was not found in:\n" + 
            testContext.getLastCommandOutput());
    }
    
    /**
     * Setup the mock security service with appropriate behaviors.
     */
    private void setupMockSecurityService() {
        // For grant permission test
        Mockito.when(mockSecurityService.grantPermission(Mockito.eq("testuser"), Mockito.eq("view")))
               .thenReturn(true);
        
        // For revoke permission test
        Mockito.when(mockSecurityService.revokePermission(Mockito.eq("testuser"), Mockito.eq("view")))
               .thenReturn(true);
        
        // For grant admin access test
        Mockito.when(mockSecurityService.grantAdminAccess(Mockito.eq("testuser"), Mockito.eq("projects")))
               .thenReturn(true);
        
        // For revoke admin access test
        Mockito.when(mockSecurityService.revokeAdminAccess(Mockito.eq("testuser"), Mockito.eq("projects")))
               .thenReturn(true);
        
        // For promote to admin test
        Mockito.when(mockSecurityService.promoteToAdmin(Mockito.eq("testuser")))
               .thenReturn(true);
    }
}