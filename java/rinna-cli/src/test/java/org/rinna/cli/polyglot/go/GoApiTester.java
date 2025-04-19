package org.rinna.cli.polyglot.go;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.rinna.cli.polyglot.framework.PolyglotTestHarness;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Test utility for interacting with the Go API server.
 * This class provides methods for:
 * <ul>
 *   <li>Starting and stopping the API server</li>
 *   <li>Making HTTP requests to API endpoints</li>
 *   <li>Validating API responses</li>
 *   <li>Testing API service integration with Java components</li>
 * </ul>
 */
public class GoApiTester {
    private static final Logger logger = LoggerFactory.getLogger(GoApiTester.class);
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    private final PolyglotTestHarness harness;
    private final HttpClient httpClient;
    
    private int serverPort = -1;
    private String baseUrl = null;
    
    /**
     * Get the current server port.
     * 
     * @return The server port or -1 if server is not running
     */
    public int getServerPort() {
        return serverPort;
    }
    
    /**
     * Create a new GoApiTester instance.
     * 
     * @param harness The polyglot test harness to use
     */
    public GoApiTester(PolyglotTestHarness harness) {
        this.harness = harness;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }
    
    /**
     * Start the Go API server for testing.
     * 
     * @param port The port to use (optional, defaults to random available port)
     * @return This tester instance for method chaining
     * @throws IOException If server startup fails
     */
    public GoApiTester startServer(Integer port) throws IOException {
        serverPort = harness.startApiServer(port);
        baseUrl = "http://localhost:" + serverPort;
        logger.info("Go API server started on port {}", serverPort);
        return this;
    }
    
    /**
     * Stop the Go API server.
     * 
     * @return This tester instance for method chaining
     */
    public GoApiTester stopServer() {
        if (serverPort != -1) {
            harness.stopApiServer(serverPort);
            logger.info("Go API server stopped on port {}", serverPort);
            serverPort = -1;
            baseUrl = null;
        }
        return this;
    }
    
    /**
     * Build and start the Go API server from source.
     * 
     * @param port The port to use (optional, defaults to random available port)
     * @return This tester instance for method chaining
     * @throws IOException If server build or startup fails
     * @throws InterruptedException If the build process is interrupted
     */
    public GoApiTester buildAndStartServer(Integer port) throws IOException, InterruptedException {
        logger.info("Building Go API server from source");
        
        Path apiDir = harness.getProjectRoot().resolve("api");
        harness.executeGoCommand("go build -o ./bin/rinnasrv ./cmd/rinnasrv", apiDir, 60);
        
        return startServer(port);
    }
    
    /**
     * Execute a Go test and return the result.
     * 
     * @param testPath Path to the test file or package (relative to api directory)
     * @param verbose Enable verbose output
     * @return Test output
     * @throws IOException If test execution fails
     * @throws InterruptedException If the test process is interrupted
     */
    public String runGoTest(String testPath, boolean verbose) throws IOException, InterruptedException {
        String command = "go test " + (verbose ? "-v " : "") + testPath;
        return harness.executeGoCommand(command, null, 30);
    }
    
    /**
     * Make a GET request to the API server.
     * 
     * @param path API endpoint path (e.g., "/api/health")
     * @return HTTP response body as string
     */
    public String get(String path) throws IOException, InterruptedException {
        checkServerRunning();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .GET()
                .timeout(Duration.ofSeconds(10))
                .header("Accept", "application/json")
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.debug("GET {} -> Status: {}", path, response.statusCode());
        
        if (response.statusCode() >= 400) {
            throw new IOException("API request failed with status code " + response.statusCode() + ": " + response.body());
        }
        
        return response.body();
    }
    
    /**
     * Make a POST request to the API server.
     * 
     * @param path API endpoint path (e.g., "/api/projects")
     * @param body Request body as JSON string
     * @return HTTP response body as string
     */
    public String post(String path, String body) throws IOException, InterruptedException {
        checkServerRunning();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.debug("POST {} -> Status: {}", path, response.statusCode());
        
        if (response.statusCode() >= 400) {
            throw new IOException("API request failed with status code " + response.statusCode() + ": " + response.body());
        }
        
        return response.body();
    }
    
    /**
     * Make a PUT request to the API server.
     * 
     * @param path API endpoint path (e.g., "/api/projects/123")
     * @param body Request body as JSON string
     * @return HTTP response body as string
     */
    public String put(String path, String body) throws IOException, InterruptedException {
        checkServerRunning();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .PUT(HttpRequest.BodyPublishers.ofString(body))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.debug("PUT {} -> Status: {}", path, response.statusCode());
        
        if (response.statusCode() >= 400) {
            throw new IOException("API request failed with status code " + response.statusCode() + ": " + response.body());
        }
        
        return response.body();
    }
    
    /**
     * Make a DELETE request to the API server.
     * 
     * @param path API endpoint path (e.g., "/api/projects/123")
     * @return HTTP response body as string
     */
    public String delete(String path) throws IOException, InterruptedException {
        checkServerRunning();
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .DELETE()
                .timeout(Duration.ofSeconds(10))
                .build();
        
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        logger.debug("DELETE {} -> Status: {}", path, response.statusCode());
        
        if (response.statusCode() >= 400) {
            throw new IOException("API request failed with status code " + response.statusCode() + ": " + response.body());
        }
        
        return response.body();
    }
    
    /**
     * Check if the API server is healthy.
     * 
     * @return True if the server is healthy, false otherwise
     */
    public boolean isHealthy() {
        try {
            String response = get("/api/health");
            JsonNode jsonNode = objectMapper.readTree(response);
            return jsonNode.has("status") && "ok".equals(jsonNode.get("status").asText());
        } catch (Exception e) {
            logger.warn("Health check failed", e);
            return false;
        }
    }
    
    /**
     * Wait for the API server to be healthy.
     * 
     * @param timeoutSeconds Maximum time to wait in seconds
     * @return True if the server became healthy within the timeout, false otherwise
     */
    public boolean waitForHealthy(int timeoutSeconds) {
        logger.info("Waiting for API server to be healthy (timeout: {}s)", timeoutSeconds);
        
        long startTime = System.currentTimeMillis();
        long endTime = startTime + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        
        while (System.currentTimeMillis() < endTime) {
            if (isHealthy()) {
                long elapsed = System.currentTimeMillis() - startTime;
                logger.info("API server is healthy (elapsed: {}ms)", elapsed);
                return true;
            }
            
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for API server to be healthy", e);
                return false;
            }
        }
        
        logger.warn("API server did not become healthy within timeout ({}s)", timeoutSeconds);
        return false;
    }
    
    // Helper methods
    
    private void checkServerRunning() {
        if (serverPort == -1 || baseUrl == null) {
            throw new IllegalStateException("API server is not running. Call startServer() first.");
        }
    }
}