package org.rinna.cli.polyglot.framework;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.TestExecutionExceptionHandler;
import org.rinna.cli.test.ParallelExecutionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core test harness for polyglot tests that integrate Java, Go, and Python components.
 * This class provides utilities for:
 * <ul>
 *   <li>Starting, monitoring, and stopping cross-language processes</li>
 *   <li>Standardized test setup/teardown across languages</li>
 *   <li>Verifying cross-language communication</li>
 *   <li>Consistent reporting mechanisms</li>
 * </ul>
 */
@ExtendWith(PolyglotTestHarness.PolyglotTestCleanupExtension.class)
public class PolyglotTestHarness {
    private static final Logger logger = LoggerFactory.getLogger(PolyglotTestHarness.class);
    
    // Process tracking for cleanup
    private static final Map<String, Process> runningProcesses = new HashMap<>();
    
    // Temporary directory for test resources
    private Path testTempDir;
    
    // Project root directory (determined at runtime)
    private Path projectRoot;
    
    // Flag indicating if the test harness has been initialized
    private boolean initialized = false;
    
    /**
     * Initialize the polyglot test harness.
     * 
     * @return This harness instance for method chaining
     * @throws IOException If temp directory creation fails
     */
    public PolyglotTestHarness initialize() throws IOException {
        if (initialized) {
            return this;
        }
        
        // Determine project root directory
        projectRoot = determineProjectRoot();
        logger.info("Project root directory: {}", projectRoot);
        
        // Create temporary directory for test resources
        testTempDir = Files.createTempDirectory("polyglot-test-");
        logger.info("Created temp directory: {}", testTempDir);
        
        // Mark as initialized
        initialized = true;
        return this;
    }
    
    /**
     * Get the project root directory.
     * 
     * @return Path to the project root
     */
    public Path getProjectRoot() {
        checkInitialized();
        return projectRoot;
    }
    
    /**
     * Get the temporary directory for test resources.
     * 
     * @return Path to the temporary directory
     */
    public Path getTestTempDir() {
        checkInitialized();
        return testTempDir;
    }
    
    /**
     * Execute a Go command and wait for it to complete.
     * 
     * @param command The Go command to execute
     * @param workDir Working directory (optional, defaults to project's api directory)
     * @param timeoutSeconds Maximum time to wait for completion
     * @return The process output
     */
    public String executeGoCommand(String command, Path workDir, int timeoutSeconds) throws IOException, InterruptedException {
        checkInitialized();
        
        Path apiDir = workDir != null ? workDir : projectRoot.resolve("api");
        logger.info("Executing Go command in directory: {}", apiDir);
        
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.directory(apiDir.toFile());
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        String processId = "go-" + System.currentTimeMillis();
        runningProcesses.put(processId, process);
        
        try {
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("Go command timed out after " + timeoutSeconds + " seconds: " + command);
            }
            
            String output = new String(process.getInputStream().readAllBytes());
            if (process.exitValue() != 0) {
                logger.error("Go command failed with exit code {}: {}", process.exitValue(), command);
                logger.error("Command output: {}", output);
                throw new RuntimeException("Go command failed with exit code " + process.exitValue());
            }
            
            return output;
        } finally {
            runningProcesses.remove(processId);
        }
    }
    
    /**
     * Execute a Python command and wait for it to complete.
     * 
     * @param command The Python command to execute
     * @param workDir Working directory (optional, defaults to project's python directory)
     * @param timeoutSeconds Maximum time to wait for completion
     * @return The process output
     */
    public String executePythonCommand(String command, Path workDir, int timeoutSeconds) throws IOException, InterruptedException {
        checkInitialized();
        
        Path pythonDir = workDir != null ? workDir : projectRoot.resolve("python");
        logger.info("Executing Python command in directory: {}", pythonDir);
        
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.directory(pythonDir.toFile());
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        String processId = "python-" + System.currentTimeMillis();
        runningProcesses.put(processId, process);
        
        try {
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("Python command timed out after " + timeoutSeconds + " seconds: " + command);
            }
            
            String output = new String(process.getInputStream().readAllBytes());
            if (process.exitValue() != 0) {
                logger.error("Python command failed with exit code {}: {}", process.exitValue(), command);
                logger.error("Command output: {}", output);
                throw new RuntimeException("Python command failed with exit code " + process.exitValue());
            }
            
            return output;
        } finally {
            runningProcesses.remove(processId);
        }
    }
    
    /**
     * Execute a CLI command and wait for it to complete.
     * 
     * @param command The CLI command to execute
     * @param timeoutSeconds Maximum time to wait for completion
     * @return The process output
     */
    public String executeCliCommand(String command, int timeoutSeconds) throws IOException, InterruptedException {
        checkInitialized();
        
        logger.info("Executing CLI command: {}", command);
        
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.directory(projectRoot.toFile());
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        String processId = "cli-" + System.currentTimeMillis();
        runningProcesses.put(processId, process);
        
        try {
            boolean completed = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                throw new RuntimeException("CLI command timed out after " + timeoutSeconds + " seconds: " + command);
            }
            
            String output = new String(process.getInputStream().readAllBytes());
            return output;
        } finally {
            runningProcesses.remove(processId);
        }
    }
    
    /**
     * Start the API server for testing.
     * 
     * @param port The port to use (optional, defaults to random available port)
     * @return The port the server is running on
     */
    public int startApiServer(Integer port) throws IOException {
        checkInitialized();
        
        int serverPort = port != null ? port : findAvailablePort();
        logger.info("Starting API server on port {}", serverPort);
        
        String command = projectRoot.resolve("bin/rin-server").toString() + " start --port " + serverPort;
        ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
        pb.directory(projectRoot.toFile());
        pb.redirectErrorStream(true);
        
        Process process = pb.start();
        String processId = "api-server-" + serverPort;
        runningProcesses.put(processId, process);
        
        // Wait for server to start
        waitForServerToStart(serverPort);
        
        return serverPort;
    }
    
    /**
     * Stop the API server.
     * 
     * @param port The port the server is running on
     */
    public void stopApiServer(int port) {
        String processId = "api-server-" + port;
        Process process = runningProcesses.get(processId);
        
        if (process != null) {
            logger.info("Stopping API server on port {}", port);
            process.destroyForcibly();
            runningProcesses.remove(processId);
            
            // Also try using the CLI command to ensure clean shutdown
            try {
                String command = projectRoot.resolve("bin/rin-server").toString() + " stop";
                Runtime.getRuntime().exec(command);
            } catch (IOException e) {
                logger.warn("Failed to stop API server using CLI command", e);
            }
        }
    }
    
    /**
     * Start multiple cross-language components and tests in parallel.
     * 
     * @param javaRunnable Code to execute for Java tests
     * @param goCommand Go command to execute (can be null)
     * @param pythonCommand Python command to execute (can be null)
     * @param timeoutSeconds Maximum time to wait for completion
     * @return Combined results from all components
     */
    public Map<String, Object> executeParallel(
            Runnable javaRunnable, 
            String goCommand,
            String pythonCommand,
            int timeoutSeconds) {
        
        checkInitialized();
        Map<String, Object> results = new HashMap<>();
        
        CompletableFuture<String> goFuture = null;
        if (goCommand != null) {
            goFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return executeGoCommand(goCommand, null, timeoutSeconds);
                } catch (Exception e) {
                    throw new RuntimeException("Go execution failed", e);
                }
            });
        }
        
        CompletableFuture<String> pythonFuture = null;
        if (pythonCommand != null) {
            pythonFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return executePythonCommand(pythonCommand, null, timeoutSeconds);
                } catch (Exception e) {
                    throw new RuntimeException("Python execution failed", e);
                }
            });
        }
        
        CompletableFuture<Void> javaFuture = CompletableFuture.runAsync(() -> {
            try {
                javaRunnable.run();
            } catch (Exception e) {
                throw new RuntimeException("Java execution failed", e);
            }
        });
        
        try {
            javaFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            results.put("java", "completed");
            
            if (goFuture != null) {
                results.put("go", goFuture.get(timeoutSeconds, TimeUnit.SECONDS));
            }
            
            if (pythonFuture != null) {
                results.put("python", pythonFuture.get(timeoutSeconds, TimeUnit.SECONDS));
            }
        } catch (Exception e) {
            logger.error("Parallel execution failed", e);
            throw new RuntimeException("Parallel execution failed", e);
        }
        
        return results;
    }
    
    /**
     * Clean up resources used by the test harness.
     */
    public void cleanup() {
        logger.info("Cleaning up polyglot test harness resources");
        
        // Stop all running processes
        for (Process process : runningProcesses.values()) {
            try {
                process.destroyForcibly();
            } catch (Exception e) {
                logger.warn("Failed to destroy process", e);
            }
        }
        runningProcesses.clear();
        
        // Delete temporary directory
        if (testTempDir != null) {
            try {
                deleteDirectory(testTempDir.toFile());
                logger.info("Deleted temp directory: {}", testTempDir);
            } catch (IOException e) {
                logger.warn("Failed to delete temp directory: {}", testTempDir, e);
            }
        }
        
        initialized = false;
    }
    
    // JUnit extension to ensure cleanup
    public static class PolyglotTestCleanupExtension implements TestExecutionExceptionHandler {
        @Override
        public void handleTestExecutionException(ExtensionContext context, Throwable throwable) throws Throwable {
            // Clean up running processes
            for (Process process : runningProcesses.values()) {
                try {
                    process.destroyForcibly();
                } catch (Exception e) {
                    logger.warn("Failed to destroy process during exception handling", e);
                }
            }
            runningProcesses.clear();
            
            // Re-throw the original exception
            throw throwable;
        }
    }
    
    // Helper methods
    
    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("PolyglotTestHarness has not been initialized. Call initialize() first.");
        }
    }
    
    private Path determineProjectRoot() {
        // Try to find the project root by looking for pom.xml
        Path currentPath = Paths.get("").toAbsolutePath();
        while (currentPath != null && !Files.exists(currentPath.resolve("pom.xml"))) {
            currentPath = currentPath.getParent();
        }
        
        if (currentPath == null || !Files.exists(currentPath.resolve("pom.xml"))) {
            throw new IllegalStateException("Could not determine project root directory. pom.xml not found in any parent directory.");
        }
        
        return currentPath;
    }
    
    private int findAvailablePort() {
        // Find an available port using ServerSocket
        try (java.net.ServerSocket socket = new java.net.ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException e) {
            logger.warn("Failed to find available port, using default 8080", e);
            return 8080;
        }
    }
    
    private void waitForServerToStart(int port) throws IOException {
        logger.info("Waiting for API server to start on port {}", port);
        
        int maxAttempts = 30;
        int attemptDelay = 1000;
        
        for (int i = 0; i < maxAttempts; i++) {
            try {
                java.net.Socket socket = new java.net.Socket("localhost", port);
                socket.close();
                logger.info("API server is running on port {}", port);
                return;
            } catch (IOException e) {
                if (i == maxAttempts - 1) {
                    throw new IOException("API server failed to start on port " + port + " after " + maxAttempts + " attempts", e);
                }
                
                try {
                    Thread.sleep(attemptDelay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Interrupted while waiting for API server to start", ie);
                }
            }
        }
    }
    
    private void deleteDirectory(File directory) throws IOException {
        if (!directory.exists()) {
            return;
        }
        
        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    deleteDirectory(file);
                } else {
                    Files.delete(file.toPath());
                }
            }
        }
        
        Files.delete(directory.toPath());
    }
}