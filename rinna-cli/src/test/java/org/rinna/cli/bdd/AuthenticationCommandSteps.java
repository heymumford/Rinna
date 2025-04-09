/*
 * BDD Step definitions for Authentication commands
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
import org.rinna.cli.security.SecurityManager;
import org.rinna.cli.service.MockSecurityService;

/**
 * Step definitions for authentication command tests.
 */
public class AuthenticationCommandSteps {
    
    private final TestContext testContext = TestContext.getInstance();
    private final MockSecurityService mockSecurityService;
    private final CommandProcessor commandProcessor;
    
    /**
     * Constructor initializes test context and mock services.
     */
    public AuthenticationCommandSteps() {
        mockSecurityService = testContext.getMockSecurityService();
        commandProcessor = testContext.getCommandProcessor();
    }
    
    /**
     * Set up unauthenticated user context.
     */
    @Given("the user is not authenticated")
    public void theUserIsNotAuthenticated() {
        mockSecurityService.setAuthenticated(false);
        mockSecurityService.setAdmin(false);
        mockSecurityService.setCurrentUser(null);
    }
    
    /**
     * Set up authenticated user context with a specific username.
     * 
     * @param username the username to authenticate as
     */
    @Given("the user is authenticated as {string}")
    public void theUserIsAuthenticatedAs(String username) {
        mockSecurityService.setAuthenticated(true);
        mockSecurityService.setCurrentUser(username);
        
        // Set admin status based on username
        boolean isAdmin = "admin".equals(username);
        mockSecurityService.setAdmin(isAdmin);
    }
    
    /**
     * Execute a command and capture the output.
     */
    @When("I run the command {string}")
    public void iRunTheCommand(String commandLine) {
        // Setup the mock methods for the service manager
        setupMockServiceManager();
        
        // Execute the command
        testContext.redirectConsoleOutput();
        testContext.resetCapturedOutput();
        commandProcessor.processCommand(commandLine);
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
     * Verify that the output does not contain a specific text.
     */
    @Then("the output should not contain {string}")
    public void theOutputShouldNotContain(String expectedText) {
        Assertions.assertFalse(testContext.getLastCommandOutput().contains(expectedText), 
            "Expected output to NOT contain '" + expectedText + "', but it was found in:\n" + 
            testContext.getLastCommandOutput());
    }
    
    /**
     * Setup mock responses for the security manager methods.
     */
    private void setupMockServiceManager() {
        // Setup mock login behavior
        Mockito.when(mockSecurityService.login(Mockito.eq("admin"), Mockito.eq("admin123")))
               .thenReturn(true);
        
        Mockito.when(mockSecurityService.login(Mockito.eq("user"), Mockito.eq("user123")))
               .thenReturn(true);
        
        Mockito.when(mockSecurityService.login(Mockito.anyString(), Mockito.eq("wrongpassword")))
               .thenReturn(false);
               
        // Setup mock for isAdmin check after successful login
        Mockito.when(mockSecurityService.isAdmin())
               .thenAnswer(invocation -> "admin".equals(mockSecurityService.getCurrentUser()));
        
        // Set up authentication mocking
        try (org.mockito.MockedStatic<SecurityManager> securityManagerMock = Mockito.mockStatic(SecurityManager.class)) {
            securityManagerMock.when(SecurityManager::getInstance).thenReturn(mockSecurityService);
        }
    }
}