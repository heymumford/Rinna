/*
 * BDD Step definitions for Server commands
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
import org.rinna.cli.service.MockServerService;
import org.rinna.cli.service.ServiceManager;

/**
 * Step definitions for server command tests.
 */
public class ServerCommandSteps {
    
    private final TestContext testContext = TestContext.getInstance();
    private final MockServerService mockServerService;
    private final CommandProcessor commandProcessor;
    
    /**
     * Constructor initializes test context and mock services.
     */
    public ServerCommandSteps() {
        mockServerService = testContext.getMockServerService();
        commandProcessor = testContext.getCommandProcessor();
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
     * Setup mock responses for the service manager methods.
     */
    private void setupMockServiceManager() {
        // Configure mock responses for ServiceManager methods
        Mockito.when(testContext.getMockServiceManager().getServiceStatus(Mockito.eq("api")))
               .thenReturn(new ServiceManager.ServiceStatusInfo(true, "RUNNING", "API server running on port 8080"));
        
        Mockito.when(testContext.getMockServiceManager().getServiceStatus(Mockito.eq("database")))
               .thenReturn(new ServiceManager.ServiceStatusInfo(true, "RUNNING", "Connected to database"));
        
        Mockito.when(testContext.getMockServiceManager().getServiceStatus(Mockito.eq("docs")))
               .thenReturn(new ServiceManager.ServiceStatusInfo(false, "STOPPED", "Documentation server not running"));
        
        // Set up for service configuration
        Mockito.when(testContext.getMockServiceManager().createServiceConfig(Mockito.eq("api"), Mockito.anyString()))
               .thenReturn(true);
               
        Mockito.when(testContext.getMockServiceManager().createServiceConfig(Mockito.eq("database"), Mockito.anyString()))
               .thenReturn(true);
               
        Mockito.when(testContext.getMockServiceManager().createServiceConfig(Mockito.eq("docs"), Mockito.anyString()))
               .thenReturn(true);
    }
}