package org.rinna.cli.integration;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;
import org.rinna.cli.service.ServiceManager;
import org.rinna.cli.service.ServiceManager.ServiceStatusInfo;
import org.rinna.cli.service.ServiceStatus;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Integration tests for CLI service management.
 * These tests verify that the CLI can interact correctly with system services.
 */
@Tag("integration")
@DisplayName("CLI Service Integration Tests")
public class CliServiceIntegrationTest {

    private ServiceManager serviceManager;
    private final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;
    
    @TempDir
    Path tempDir;
    
    @BeforeEach
    void setUp() {
        serviceManager = ServiceManager.getInstance();
        System.setOut(new PrintStream(outputStream));
    }
    
    @Test
    @DisplayName("Should retrieve service status correctly")
    void shouldRetrieveServiceStatusCorrectly() {
        // Use a mock service for testing
        ServiceStatusInfo status = serviceManager.getServiceStatus("mock-service");
        
        // Verify
        assertNotNull(status, "Service status should not be null");
        assertTrue(status.isAvailable(), "Mock service should be available");
    }
    
    @Test
    @DisplayName("Should report correct status for non-existent service")
    void shouldReportCorrectStatusForNonExistentService() {
        // Get status for non-existent service
        ServiceStatusInfo status = serviceManager.getServiceStatus("non-existent-service");
        
        // Verify
        assertNotNull(status, "Service status should not be null even for non-existent service");
        assertFalse(status.isAvailable(), "Non-existent service should not be available");
        assertEquals("UNKNOWN", status.getState(), "Non-existent service should have UNKNOWN state");
    }
    
    @Test
    @DisplayName("Should create service config file correctly")
    void shouldCreateServiceConfigFileCorrectly() {
        // Create config path
        File configFile = tempDir.resolve("service-config.json").toFile();
        
        // Create config
        boolean result = serviceManager.createServiceConfig("test-service", configFile.getAbsolutePath());
        
        // Verify
        assertTrue(result, "Config creation should succeed");
        assertTrue(configFile.exists(), "Config file should exist");
        assertTrue(configFile.length() > 0, "Config file should not be empty");
    }
    
    @Test
    @DisplayName("Should handle multiple service operations concurrently")
    void shouldHandleMultipleServiceOperationsConcurrently() throws Exception {
        // Skip test on CI environments
        if (System.getenv("CI") != null) {
            // Skip this test when running in CI environment
            System.out.println("Skipping concurrent test in CI environment");
            return;
        }
        
        // Setup test services
        String[] services = {"service1", "service2", "service3"};
        
        // Execute concurrent operations
        Thread[] threads = new Thread[services.length];
        
        for (int i = 0; i < services.length; i++) {
            final String service = services[i];
            threads[i] = new Thread(() -> {
                ServiceStatusInfo status = serviceManager.getServiceStatus(service);
                assertNotNull(status, "Service status should not be null for " + service);
            });
            threads[i].start();
        }
        
        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join(TimeUnit.SECONDS.toMillis(2));
        }
        
        // Verify all threads completed successfully
        for (Thread thread : threads) {
            assertFalse(thread.isAlive(), "Thread should have completed");
        }
    }
    
    @Test
    @DisplayName("Should connect to local service endpoint")
    void shouldConnectToLocalServiceEndpoint() {
        // Skip if no local endpoint
        if (!serviceManager.hasLocalEndpoint()) {
            System.out.println("Skipping local endpoint test - endpoint not available");
            return;
        }
        
        // Connect to local endpoint
        boolean connected = serviceManager.connectLocalEndpoint();
        
        // Verify
        assertTrue(connected, "Should connect to local endpoint");
    }
}