/*
 * Service management for the Rinna workflow system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.cli.service;

import org.rinna.cli.config.CliConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages the Rinna API services.
 * Handles starting, stopping, and checking the status of the required services.
 */
public class ServiceManager {
    private static final Logger LOG = LoggerFactory.getLogger(ServiceManager.class);
    
    private static final String USER_HOME = System.getProperty("user.home");
    private static final String RINNA_DIR = ".rinna";
    private static final String PID_FILE = "rinna-server.pid";
    private static final int MAX_HEALTH_CHECK_ATTEMPTS = 10;
    private static final int HEALTH_CHECK_INTERVAL_MS = 500;
    
    private final CliConfig config;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean servicesStarted = new AtomicBoolean(false);
    
    /**
     * Constructs a new ServiceManager with the given configuration.
     *
     * @param config the CLI configuration
     */
    public ServiceManager(CliConfig config) {
        this.config = config;
    }
    
    /**
     * Checks if all required services are running.
     *
     * @return true if all services are running
     */
    public boolean areServicesRunning() {
        try {
            // Check Go API health
            if (!isGoApiRunning()) {
                LOG.debug("Go API service is not running");
                return false;
            }
            
            // Check Java backend health
            if (!isJavaBackendRunning()) {
                LOG.debug("Java backend service is not running");
                return false;
            }
            
            return true;
        } catch (Exception e) {
            LOG.warn("Error checking service status", e);
            return false;
        }
    }
    
    /**
     * Ensures that all required services are running.
     * If any service is not running, attempts to start it.
     *
     * @return true if all services are running (or have been started)
     */
    public boolean ensureServicesRunning() {
        if (areServicesRunning()) {
            LOG.debug("All services are already running");
            return true;
        }
        
        LOG.info("Starting required services...");
        boolean success = startServices();
        
        if (success) {
            LOG.info("All services started successfully");
        } else {
            LOG.error("Failed to start all required services");
        }
        
        return success;
    }
    
    /**
     * Starts all required services.
     *
     * @return true if all services started successfully
     */
    public boolean startServices() {
        if (servicesStarted.get()) {
            LOG.debug("Services already started by this instance");
            return true;
        }
        
        try {
            // Create the .rinna directory if it doesn't exist
            Path rinnaDir = Paths.get(USER_HOME, RINNA_DIR);
            if (!Files.exists(rinnaDir)) {
                Files.createDirectories(rinnaDir);
            }
            
            // Start the services using the start-services.sh script
            String projectRoot = getProjectRoot();
            if (projectRoot == null) {
                LOG.error("Could not determine project root directory");
                return false;
            }
            
            String scriptPath = Paths.get(projectRoot, "bin", "start-services.sh").toString();
            LOG.debug("Starting services using script: {}", scriptPath);
            
            // Start the process asynchronously
            ProcessBuilder processBuilder = new ProcessBuilder("/bin/bash", scriptPath);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();
            
            // Store the PID
            long pid = process.pid();
            Path pidFile = Paths.get(USER_HOME, RINNA_DIR, PID_FILE);
            Files.writeString(pidFile, Long.toString(pid), StandardCharsets.UTF_8);
            
            // Monitor the output in a separate thread
            executor.submit(() -> {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (config.isDebugEnabled()) {
                            LOG.debug("Service output: {}", line);
                        }
                    }
                } catch (IOException e) {
                    LOG.warn("Error reading service output", e);
                }
            });
            
            // Wait for services to be ready
            boolean ready = waitForServicesReady();
            if (ready) {
                servicesStarted.set(true);
            }
            return ready;
            
        } catch (IOException e) {
            LOG.error("Error starting services", e);
            return false;
        }
    }
    
    /**
     * Stops all running services.
     *
     * @return true if all services stopped successfully
     */
    public boolean stopServices() {
        try {
            Path pidFile = Paths.get(USER_HOME, RINNA_DIR, PID_FILE);
            if (!Files.exists(pidFile)) {
                LOG.debug("No PID file found, services may not be running");
                return true;
            }
            
            String pidStr = Files.readString(pidFile, StandardCharsets.UTF_8).trim();
            long pid = Long.parseLong(pidStr);
            
            // Stop the services by sending SIGTERM to the process group
            LOG.debug("Stopping services with PID {}", pid);
            
            // On Unix systems, use process group ID to kill all related processes
            ProcessBuilder processBuilder = new ProcessBuilder("kill", "-15", "-" + pid);
            Process process = processBuilder.start();
            boolean exited = process.waitFor(5, TimeUnit.SECONDS);
            
            if (!exited) {
                LOG.warn("Kill command did not complete in time, trying SIGKILL");
                ProcessBuilder forceKill = new ProcessBuilder("kill", "-9", "-" + pid);
                forceKill.start().waitFor(3, TimeUnit.SECONDS);
            }
            
            // Remove the PID file
            Files.deleteIfExists(pidFile);
            
            // Shutdown the executor
            executor.shutdown();
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            
            servicesStarted.set(false);
            return true;
            
        } catch (IOException | InterruptedException | NumberFormatException e) {
            LOG.error("Error stopping services", e);
            return false;
        }
    }
    
    /**
     * Gets the status of all services.
     *
     * @return a ServiceStatus object with details about each service
     */
    public ServiceStatus getServicesStatus() {
        ServiceStatus status = new ServiceStatus();
        
        // Check Go API
        try {
            boolean goApiRunning = isGoApiRunning();
            status.setGoApiRunning(goApiRunning);
            
            // If Go API is running, get more details
            if (goApiRunning) {
                String healthUrl = config.getApiEndpoint() + "/health";
                String healthResponse = fetchUrl(healthUrl);
                status.setGoApiDetails(healthResponse);
            }
        } catch (Exception e) {
            status.setGoApiRunning(false);
            status.setGoApiDetails("Error: " + e.getMessage());
        }
        
        // Check Java backend
        try {
            boolean javaRunning = isJavaBackendRunning();
            status.setJavaBackendRunning(javaRunning);
            
            // If Java backend is running, get more details
            if (javaRunning) {
                String healthUrl = config.getJavaBackendHealthUrl();
                String healthResponse = fetchUrl(healthUrl);
                status.setJavaBackendDetails(healthResponse);
            }
        } catch (Exception e) {
            status.setJavaBackendRunning(false);
            status.setJavaBackendDetails("Error: " + e.getMessage());
        }
        
        return status;
    }
    
    /**
     * Waits for all services to be ready.
     *
     * @return true if all services are ready
     */
    private boolean waitForServicesReady() {
        LOG.debug("Waiting for services to become ready...");
        
        Instant startTime = Instant.now();
        
        // Try multiple times with a delay
        for (int attempt = 0; attempt < MAX_HEALTH_CHECK_ATTEMPTS; attempt++) {
            try {
                // Give services time to start
                Thread.sleep(HEALTH_CHECK_INTERVAL_MS);
                
                // Check if both services are running
                if (isGoApiRunning() && isJavaBackendRunning()) {
                    Duration elapsed = Duration.between(startTime, Instant.now());
                    LOG.debug("Services ready after {} ms", elapsed.toMillis());
                    return true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                LOG.warn("Interrupted while waiting for services", e);
                return false;
            } catch (Exception e) {
                LOG.debug("Service check failed: {}", e.getMessage());
                // Continue trying
            }
        }
        
        LOG.error("Timeout waiting for services to become ready");
        return false;
    }
    
    /**
     * Checks if the Go API is running.
     *
     * @return true if the Go API is running
     * @throws IOException if an error occurs during the check
     */
    private boolean isGoApiRunning() throws IOException {
        String healthUrl = config.getApiEndpoint() + "/health";
        return checkHealthEndpoint(healthUrl);
    }
    
    /**
     * Checks if the Java backend is running.
     *
     * @return true if the Java backend is running
     * @throws IOException if an error occurs during the check
     */
    private boolean isJavaBackendRunning() throws IOException {
        String healthUrl = config.getJavaBackendHealthUrl();
        return checkHealthEndpoint(healthUrl);
    }
    
    /**
     * Checks a health endpoint to see if it's responding.
     *
     * @param healthUrl the URL to check
     * @return true if the endpoint is responding with a 200 status
     * @throws IOException if an error occurs during the check
     */
    private boolean checkHealthEndpoint(String healthUrl) throws IOException {
        URL url = new URL(healthUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(config.getConnectionTimeout());
        connection.setReadTimeout(config.getReadTimeout());
        connection.setRequestMethod("GET");
        
        try {
            int statusCode = connection.getResponseCode();
            return statusCode == 200;
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Fetches the content of a URL as a string.
     *
     * @param urlString the URL to fetch
     * @return the content as a string
     * @throws IOException if an error occurs during the fetch
     */
    private String fetchUrl(String urlString) throws IOException {
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setConnectTimeout(config.getConnectionTimeout());
        connection.setReadTimeout(config.getReadTimeout());
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
            StringBuilder response = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                response.append(line);
            }
            return response.toString();
        } finally {
            connection.disconnect();
        }
    }
    
    /**
     * Tries to determine the project root directory.
     *
     * @return the project root directory, or null if it can't be determined
     */
    private String getProjectRoot() {
        // Get the current working directory
        String cwd = System.getProperty("user.dir");
        
        // Check if bin/start-services.sh exists in the current or parent directory
        File current = new File(cwd);
        File scriptFile = new File(current, "bin/start-services.sh");
        if (scriptFile.exists()) {
            return current.getAbsolutePath();
        }
        
        // Check parent directory
        File parent = current.getParentFile();
        if (parent != null) {
            scriptFile = new File(parent, "bin/start-services.sh");
            if (scriptFile.exists()) {
                return parent.getAbsolutePath();
            }
        }
        
        // Try using environment variable if set
        String rinnaHome = System.getenv("RINNA_HOME");
        if (rinnaHome != null && !rinnaHome.isEmpty()) {
            scriptFile = new File(rinnaHome, "bin/start-services.sh");
            if (scriptFile.exists()) {
                return rinnaHome;
            }
        }
        
        // Could not determine project root
        return null;
    }
}