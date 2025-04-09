package org.rinna.cli.polyglot;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.rinna.cli.polyglot.framework.PolyglotTestHarness;
import org.rinna.cli.polyglot.go.GoApiTester;
import org.rinna.cli.polyglot.java.JavaCliTester;
import org.rinna.cli.polyglot.java.JavaCliTester.CliResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Integration tests for CLI and API components using the polyglot test harness.
 * These tests verify that the Java CLI can properly communicate with the Go API server.
 */
@Tag("polyglot")
@Tag("integration")
public class CliApiIntegrationTest {
    private static final Logger logger = LoggerFactory.getLogger(CliApiIntegrationTest.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private PolyglotTestHarness harness;
    private GoApiTester goTester;
    private JavaCliTester javaTester;
    
    @BeforeEach
    void setUp(TestInfo testInfo) throws IOException {
        logger.info("Starting test: {}", testInfo.getDisplayName());
        
        // Initialize the test harness
        harness = new PolyglotTestHarness().initialize();
        
        // Initialize component testers
        goTester = new GoApiTester(harness);
        javaTester = new JavaCliTester(harness);
    }
    
    @AfterEach
    void tearDown() {
        if (harness != null) {
            harness.cleanup();
        }
    }
    
    @Test
    @DisplayName("CLI should be able to connect to API server and check health status")
    void cliShouldConnectToApiServerAndCheckHealth() throws Exception {
        // Start the API server
        int port = goTester.startServer(null).waitForHealthy(30) ? goTester.getServerPort() : 8080;
        
        // Execute the CLI command to check API health
        String output = javaTester.executeMainCliScript("server", "status");
        
        // Verify the output indicates the server is running
        assertTrue(output.contains("API server is running"), "CLI should report API server as running");
        assertTrue(output.contains("Status: OK"), "CLI should report API server status as OK");
    }
    
    @Test
    @DisplayName("CLI should be able to create a work item via API")
    void cliShouldCreateWorkItemViaApi() throws Exception {
        // Start the API server
        goTester.startServer(null).waitForHealthy(30);
        
        // Execute the CLI command to add a work item
        String title = "Test work item created via API " + System.currentTimeMillis();
        String output = javaTester.executeCliScript("add", 
                "--title", "\"" + title + "\"", 
                "--type", "TASK", 
                "--priority", "HIGH", 
                "--description", "\"This is a test work item created by the CliApiIntegrationTest\"",
                "--api");
        
        // Verify the output indicates the work item was created
        assertTrue(output.contains("Successfully created work item"), "CLI should report work item was created");
        assertTrue(output.contains("ID:"), "CLI should report the ID of the created work item");
        
        // Extract the ID of the created work item
        String id = extractIdFromOutput(output);
        assertNotNull(id, "Should be able to extract work item ID from output");
        
        // Verify the work item exists in the API
        String getItemPath = "/api/workitems/" + id;
        String itemJson = goTester.get(getItemPath);
        
        // Parse the JSON response and verify the work item data
        JsonNode itemNode = objectMapper.readTree(itemJson);
        assertEquals(title, itemNode.path("title").asText(), "Work item title should match");
        assertEquals("TASK", itemNode.path("type").asText(), "Work item type should match");
        assertEquals("HIGH", itemNode.path("priority").asText(), "Work item priority should match");
    }
    
    @Test
    @DisplayName("CLI list command should display work items from API")
    void cliListCommandShouldDisplayWorkItemsFromApi() throws Exception {
        // Start the API server
        goTester.startServer(null).waitForHealthy(30);
        
        // Create a test work item via the API directly
        String title = "Test list item " + System.currentTimeMillis();
        String createJson = "{"
                + "\"title\": \"" + title + "\","
                + "\"type\": \"TASK\","
                + "\"priority\": \"MEDIUM\","
                + "\"status\": \"NEW\""
                + "}";
        
        String createResponse = goTester.post("/api/workitems", createJson);
        JsonNode createNode = objectMapper.readTree(createResponse);
        String itemId = createNode.path("id").asText();
        
        // Execute the CLI list command with API flag
        String listOutput = javaTester.executeCliScript("list", "--api");
        
        // Verify the output includes the created work item
        assertTrue(listOutput.contains(itemId), "List output should include the created work item ID");
        assertTrue(listOutput.contains(title), "List output should include the created work item title");
    }
    
    @Test
    @DisplayName("CLI and API server should run concurrently with proper interaction")
    void cliAndApiServerShouldRunConcurrentlyWithProperInteraction() throws Exception {
        // Start the API server
        goTester.startServer(null).waitForHealthy(30);
        
        // Create multiple work items concurrently
        int numItems = 5;
        CompletableFuture<?>[] futures = new CompletableFuture[numItems];
        
        for (int i = 0; i < numItems; i++) {
            final int itemNum = i + 1;
            futures[i] = CompletableFuture.runAsync(() -> {
                try {
                    String title = "Concurrent test item " + itemNum;
                    javaTester.executeCliScript("add", 
                            "--title", "\"" + title + "\"", 
                            "--type", "TASK", 
                            "--priority", "MEDIUM", 
                            "--api");
                } catch (Exception e) {
                    throw new RuntimeException("Failed to create work item " + itemNum, e);
                }
            });
        }
        
        // Wait for all items to be created
        CompletableFuture.allOf(futures).get(60, TimeUnit.SECONDS);
        
        // Execute the CLI list command and verify all items are shown
        String listOutput = javaTester.executeCliScript("list", "--api", "--limit", "10");
        
        for (int i = 0; i < numItems; i++) {
            String title = "Concurrent test item " + (i + 1);
            assertTrue(listOutput.contains(title), "List output should include item " + (i + 1));
        }
    }
    
    @Test
    @DisplayName("CLI should handle API server startup and shutdown")
    void cliShouldHandleApiServerStartupAndShutdown() throws Exception {
        // Use the CLI to start the server
        String startOutput = javaTester.executeMainCliScript("server", "start");
        assertTrue(startOutput.contains("Starting API server"), "CLI should report server starting");
        
        // Wait for server to be ready (checking with the Go tester)
        TimeUnit.SECONDS.sleep(2);  // Brief wait for server to initialize
        
        // The CLI started the server, now try to connect with our goTester
        boolean isHealthy = goTester.waitForHealthy(30);
        assertTrue(isHealthy, "API server should become healthy");
        
        // Check server status using CLI
        String statusOutput = javaTester.executeMainCliScript("server", "status");
        assertTrue(statusOutput.contains("API server is running"), "CLI should report server is running");
        
        // Stop the server using CLI
        String stopOutput = javaTester.executeMainCliScript("server", "stop");
        assertTrue(stopOutput.contains("Stopping API server"), "CLI should report server stopping");
        
        // Verify server is stopped
        TimeUnit.SECONDS.sleep(2);  // Brief wait for server to stop
        try {
            goTester.get("/api/health");
            fail("API server should be stopped");
        } catch (IOException e) {
            // Expected - server should be down
        }
    }
    
    // Helper methods
    
    private String extractIdFromOutput(String output) {
        // Extract ID from output line like "ID: 123e4567-e89b-12d3-a456-426614174000"
        for (String line : output.split("\\n")) {
            line = line.trim();
            if (line.startsWith("ID:")) {
                return line.substring(3).trim();
            }
        }
        return null;
    }
}