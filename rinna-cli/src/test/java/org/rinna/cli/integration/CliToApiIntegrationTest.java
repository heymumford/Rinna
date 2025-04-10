/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna.cli.integration;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.rinna.Rinna;
import org.rinna.cli.command.ServerCommand;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.test.ParallelExecutionManager.FileSystemAccess;
import org.rinna.cli.test.ParallelExecutionManager.IsolatedTest;

/**
 * Integration tests for CLI-to-API interactions.
 * These tests verify that the CLI can interact correctly with the API server.
 */
@Tag("integration")
@DisplayName("CLI to API Integration Tests")
@IsolatedTest // Use this to avoid port conflicts in parallel tests
@FileSystemAccess // Use this because we interact with file system for configuration
class CliToApiIntegrationTest {

    private ByteArrayOutputStream outputStream;
    private ByteArrayOutputStream errorStream;
    private final PrintStream originalOut = System.out;
    private final PrintStream originalErr = System.err;
    
    private static Rinna rinnaCore;
    private static int apiPort = 18081; // Use a non-standard port to avoid conflicts
    private static boolean apiStarted = false;
    
    @BeforeAll
    static void startApiServer() {
        // Initialize Rinna core
        rinnaCore = Rinna.initialize();
        
        // Start API server
        apiStarted = rinnaCore.startApiServer(apiPort);
        
        // Wait for server to start
        if (apiStarted) {
            try {
                TimeUnit.SECONDS.sleep(1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
    
    @AfterAll
    static void stopApiServer() {
        if (rinnaCore != null) {
            rinnaCore.stopApiServer();
        }
    }
    
    @BeforeEach
    void setUp() {
        // Skip tests if API server could not be started
        assumeTrue(apiStarted, "API server could not be started");
        
        // Capture console output
        outputStream = new ByteArrayOutputStream();
        errorStream = new ByteArrayOutputStream();
        System.setOut(new PrintStream(outputStream));
        System.setErr(new PrintStream(errorStream));
    }
    
    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        System.setErr(originalErr);
    }
    
    @Nested
    @DisplayName("Server Command Tests")
    class ServerCommandTests {
        
        @Test
        @DisplayName("Should check server status correctly")
        void shouldCheckServerStatusCorrectly() {
            // Setup ServerCommand with status check
            ServerCommand serverCmd = new ServerCommand();
            serverCmd.setStatus(true);
            
            // Execute command
            int exitCode = serverCmd.call();
            
            // Verify command execution
            assertEquals(0, exitCode, "Command should execute successfully");
            
            // Verify output contains running status
            String output = outputStream.toString();
            assertTrue(output.contains("running") || output.contains("available"), 
                "Output should indicate server is running");
            assertTrue(output.contains(String.valueOf(apiPort)), 
                "Output should contain server port");
        }
        
        @Test
        @DisplayName("Should connect to API endpoint")
        void shouldConnectToApiEndpoint() throws IOException {
            // Make a direct HTTP request to the API health endpoint
            URL url = new URL("http://localhost:" + apiPort + "/health");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            
            // Get response
            int responseCode = connection.getResponseCode();
            assertEquals(200, responseCode, "Health endpoint should return 200 OK");
            
            // Read response body
            try (Scanner scanner = new Scanner(connection.getInputStream(), StandardCharsets.UTF_8.name())) {
                String responseBody = scanner.useDelimiter("\\A").next();
                assertTrue(responseBody.contains("status"), "Response should contain status field");
                assertTrue(responseBody.contains("UP") || responseBody.contains("OK"), 
                    "Status should indicate the server is up");
            }
        }
    }
    
    @Nested
    @DisplayName("Service Integration Tests")
    class ServiceIntegrationTests {
        
        @Test
        @DisplayName("Should detect running API service")
        void shouldDetectRunningApiService() {
            // Get service status via ServiceManager
            ServiceManager serviceManager = ServiceManager.getInstance();
            ServiceManager.ServiceStatusInfo status = serviceManager.getServiceStatus("api");
            
            // Verify
            assertNotNull(status, "Service status should not be null");
            assertTrue(status.isAvailable(), "API service should be available");
            assertEquals("RUNNING", status.getState(), "API service should be running");
        }
        
        @Test
        @DisplayName("Should retrieve service endpoint information")
        void shouldRetrieveServiceEndpointInformation() {
            // Get service endpoint info
            ServiceManager serviceManager = ServiceManager.getInstance();
            String endpoint = serviceManager.getServiceEndpoint("api");
            
            // Verify
            assertNotNull(endpoint, "Service endpoint should not be null");
            assertTrue(endpoint.contains("localhost"), "Endpoint should be localhost");
            assertTrue(endpoint.contains(String.valueOf(apiPort)), "Endpoint should contain the correct port");
        }
    }
}