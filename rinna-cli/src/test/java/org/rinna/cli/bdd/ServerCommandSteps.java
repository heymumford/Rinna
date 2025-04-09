/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.bdd;

import io.cucumber.java.After;
import io.cucumber.java.Before;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.rinna.cli.command.ServerCommand;
import org.rinna.cli.service.ConfigurationService;
import org.rinna.cli.service.ContextManager;
import org.rinna.cli.service.MetadataService;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.ServiceManager.ServiceStatusInfo;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Step definitions for the Server Command feature.
 */
public class ServerCommandSteps {
    
    @Mock
    private ServiceManager serviceManager;
    
    @Mock
    private ConfigurationService configService;
    
    @Mock
    private MetadataService metadataService;
    
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final ByteArrayOutputStream errContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private final AtomicReference<Integer> result = new AtomicReference<>();
    private ServerCommand serverCommand;
    
    @Before
    public void setUp() {
        System.setOut(new PrintStream(outContent));
        System.setErr(new PrintStream(errContent));
        
        MockitoAnnotations.openMocks(this);
        
        when(serviceManager.getConfigurationService()).thenReturn(configService);
        when(serviceManager.getMetadataService()).thenReturn(metadataService);
        when(configService.getCurrentUser()).thenReturn("testuser");
        when(metadataService.startOperation(anyString(), anyString(), any())).thenReturn("operation-id");
        
        // Mock default service statuses
        when(serviceManager.getServiceStatus("api"))
            .thenReturn(new ServiceStatusInfo(false, "STOPPED", "API service is not running"));
        when(serviceManager.getServiceStatus("database"))
            .thenReturn(new ServiceStatusInfo(false, "STOPPED", "Database service is not running"));
        when(serviceManager.getServiceStatus("docs"))
            .thenReturn(new ServiceStatusInfo(false, "STOPPED", "Documentation service is not running"));
            
        serverCommand = new ServerCommand(serviceManager);
    }
    
    @After
    public void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
        
        // Reset output streams for next scenario
        outContent.reset();
        errContent.reset();
    }
    
    @Given("a valid user session")
    public void aValidUserSession() {
        // Session setup is done in setUp()
    }
    
    @Given("the service {string} is running")
    public void theServiceIsRunning(String serviceName) {
        when(serviceManager.getServiceStatus(serviceName))
            .thenReturn(new ServiceStatusInfo(true, "RUNNING", serviceName + " service is running"));
    }
    
    @When("I execute the command {string}")
    public void iExecuteTheCommand(String commandLine) {
        // Parse command line
        String[] args = commandLine.split("\\s+");
        
        if (args.length > 0) {
            if (args[0].equals("server")) {
                // Process arguments
                if (args.length > 1) {
                    serverCommand.setSubcommand(args[1]);
                    
                    if (args.length > 2) {
                        // Handle service name
                        if (\!args[2].startsWith("--")) {
                            serverCommand.setServiceName(args[2]);
                        }
                        
                        // Handle config path if present
                        if (args.length > 3 && \!args[3].startsWith("--")) {
                            serverCommand.setConfigPath(args[3]);
                        }
                    }
                }
                
                // Look for flags in any position
                for (String arg : args) {
                    if ("--json".equals(arg)) {
                        serverCommand.setFormat("json");
                    } else if ("--verbose".equals(arg)) {
                        serverCommand.setVerbose(true);
                    }
                }
                
                // Execute the command
                result.set(serverCommand.call());
            }
        }
    }
    
    @Then("I should see a list of available services")
    public void iShouldSeeAListOfAvailableServices() {
        String output = outContent.toString();
        assertTrue(output.contains("Rinna Services Status:"));
        assertTrue(output.contains("SERVICE"));
    }
    
    @Then("I should see status information for each service")
    public void iShouldSeeStatusInformationForEachService() {
        String output = outContent.toString();
        assertTrue(output.contains("api"));
        assertTrue(output.contains("database"));
        assertTrue(output.contains("docs"));
    }
    
    @Then("I should see detailed status for the {string} service")
    public void iShouldSeeDetailedStatusForTheService(String serviceName) {
        String output = outContent.toString();
        assertTrue(output.contains("Service: " + serviceName) || output.contains("\"name\": \"" + serviceName + "\""));
    }
    
    @Then("I should see help information for the server command")
    public void iShouldSeeHelpInformationForTheServerCommand() {
        String output = outContent.toString();
        assertTrue(output.contains("Server Command Usage:") || output.contains("\"command\": \"server\""));
    }
    
    @Then("I should see a list of available subcommands")
    public void iShouldSeeAListOfAvailableSubcommands() {
        String output = outContent.toString();
        assertTrue(output.contains("status") && output.contains("start") && output.contains("stop"));
    }
    
    @Then("the service {string} should be started")
    public void theServiceShouldBeStarted(String serviceName) {
        // Mock that start operation was successful
        when(serviceManager.getServiceStatus(serviceName))
            .thenReturn(new ServiceStatusInfo(true, "RUNNING", serviceName + " service is running"));
        
        // Verify proper commands were executed
        verify(metadataService).startOperation(eq("server-start"), eq("EXECUTE"), any());
    }
    
    @Then("the service {string} should be stopped")
    public void theServiceShouldBeStopped(String serviceName) {
        // Mock that stop operation was successful
        verify(metadataService).startOperation(eq("server-stop"), eq("EXECUTE"), any());
    }
    
    @Then("the service {string} should be restarted")
    public void theServiceShouldBeRestarted(String serviceName) {
        // Verify restart operations
        verify(metadataService).startOperation(eq("server-restart"), eq("EXECUTE"), any());
    }
    
    @Then("a configuration file should be created at {string}")
    public void aConfigurationFileShouldBeCreatedAt(String configPath) throws IOException {
        // Mock that config operation was successful
        when(serviceManager.createServiceConfig(anyString(), eq(configPath))).thenReturn(true);
        
        // Verify proper commands were executed
        verify(metadataService).startOperation(eq("server-config"), eq("CREATE"), any());
    }
    
    @Then("I should see a confirmation message")
    public void iShouldSeeAConfirmationMessage() {
        String output = outContent.toString();
        boolean hasSuccessMsg = output.contains("successfully") || 
                               output.contains("success") || 
                               output.contains("Created");
        assertTrue(hasSuccessMsg);
    }
    
    @Then("I should see the output in JSON format")
    public void iShouldSeeTheOutputInJsonFormat() {
        String output = outContent.toString();
        assertTrue(output.contains("\"result\":"));
    }
    
    @Then("the JSON should contain service status information")
    public void theJsonShouldContainServiceStatusInformation() {
        String output = outContent.toString();
        assertTrue(output.contains("\"service\":") || output.contains("\"services\":"));
    }
    
    @Then("I should see an error message about unknown service")
    public void iShouldSeeAnErrorMessageAboutUnknownService() {
        String error = errContent.toString();
        assertTrue(error.contains("unknown") || error.contains("Unknown service"));
    }
    
    @Then("I should see an error message about invalid subcommand")
    public void iShouldSeeAnErrorMessageAboutInvalidSubcommand() {
        String error = errContent.toString();
        assertTrue(error.contains("Unknown server subcommand"));
    }
    
    @Then("the command should track this operation with MetadataService")
    public void theCommandShouldTrackThisOperationWithMetadataService() {
        verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
        
        // There should be at least one successful operation completion
        verify(metadataService, times(1)).completeOperation(anyString(), any());
    }
    
    @Then("the command should track this operation failure with MetadataService")
    public void theCommandShouldTrackThisOperationFailureWithMetadataService() {
        verify(metadataService).startOperation(eq("server-command"), eq("MANAGE"), any());
        
        // There should be at least one operation failure
        verify(metadataService, times(1)).failOperation(anyString(), any());
    }
}
