/*
 * BDD test context for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.bdd;

import org.rinna.utils.TestRinna;
import org.rinna.domain.APIToken;
import org.rinna.domain.model.Project;
import org.rinna.domain.model.Release;
import org.rinna.domain.WebhookConfig;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Test context for sharing state between Cucumber step definitions.
 * This class holds the current state of the test scenario.
 */
public class TestContext {
    private final TestRinna rinna;
    private final Map<String, WorkItem> workItems;
    private final Map<String, UUID> workItemIds;
    private final Map<String, WorkItemCreateRequest> createRequests;
    private final Map<String, Release> releases;
    private final Map<UUID, Map<String, String>> workItemMetadata;
    private final Map<String, Object> configurationValues;
    private final Set<String> configurationFlags;
    private final Map<String, APIToken> apiTokens;
    private final Map<String, Project> projects;
    private final Map<String, WebhookConfig> webhookConfigs;
    private final Map<String, String> jsonPayloads;
    private final Map<String, Map<String, String>> clientReports;
    private Exception lastException;
    private int lastStatusCode;
    
    // Extensions for interactive command testing
    private final Queue<String> inputQueue = new ConcurrentLinkedQueue<>();
    private ByteArrayOutputStream capturedOutput;
    private ByteArrayOutputStream capturedError;
    private final Map<Class<?>, Object> services = new HashMap<>();
    
    // For admin operations testing
    private String userRole;
    private String lastCommandOutput;
    
    /**
     * Constructs a new TestContext with default initialization.
     */
    public TestContext() {
        this.rinna = TestRinna.initialize();
        this.workItems = new HashMap<>();
        this.workItemIds = new HashMap<>();
        this.createRequests = new HashMap<>();
        this.releases = new HashMap<>();
        this.workItemMetadata = new HashMap<>();
        this.configurationValues = new HashMap<>();
        this.configurationFlags = new HashSet<>();
        this.apiTokens = new HashMap<>();
        this.projects = new HashMap<>();
        this.webhookConfigs = new HashMap<>();
        this.jsonPayloads = new HashMap<>();
        this.clientReports = new HashMap<>();
        this.lastStatusCode = 200;
        this.capturedOutput = new ByteArrayOutputStream();
        this.capturedError = new ByteArrayOutputStream();
    }
    
    /**
     * Returns the Rinna instance for this test context.
     *
     * @return the Rinna instance
     */
    public TestRinna getRinna() {
        return rinna;
    }
    
    /**
     * Saves a work item in the test context.
     *
     * @param key the key to use for the work item
     * @param workItem the work item to save
     */
    public void saveWorkItem(String key, WorkItem workItem) {
        workItems.put(key, workItem);
        workItemIds.put(key, workItem.getId());
    }
    
    /**
     * Returns a work item from the test context.
     *
     * @param key the key for the work item
     * @return the work item
     */
    public WorkItem getWorkItem(String key) {
        return workItems.get(key);
    }
    
    /**
     * Returns all work item keys in the test context.
     *
     * @return the set of work item keys
     */
    public Set<String> getAllWorkItemKeys() {
        return new HashSet<>(workItems.keySet());
    }
    
    /**
     * Returns a work item ID from the test context.
     *
     * @param key the key for the work item ID
     * @return the work item ID
     */
    public UUID getWorkItemId(String key) {
        return workItemIds.get(key);
    }
    
    /**
     * Saves a create request in the test context.
     *
     * @param key the key to use for the create request
     * @param request the create request to save
     */
    public void saveCreateRequest(String key, WorkItemCreateRequest request) {
        createRequests.put(key, request);
    }
    
    /**
     * Returns a create request from the test context.
     *
     * @param key the key for the create request
     * @return the create request
     */
    public WorkItemCreateRequest getCreateRequest(String key) {
        return createRequests.get(key);
    }
    
    /**
     * Saves a release in the test context.
     *
     * @param key the key to use for the release
     * @param release the release to save
     */
    public void saveRelease(String key, Release release) {
        releases.put(key, release);
    }
    
    /**
     * Returns a release from the test context.
     *
     * @param key the key for the release
     * @return the release
     */
    public Release getRelease(String key) {
        return releases.get(key);
    }
    
    /**
     * Sets the last exception encountered during a test.
     *
     * @param exception the exception
     */
    public void setException(Exception exception) {
        this.lastException = exception;
    }
    
    /**
     * Returns the last exception encountered during a test.
     *
     * @return the last exception
     */
    public Exception getException() {
        return lastException;
    }
    
    /**
     * Clears the last exception.
     */
    public void clearException() {
        this.lastException = null;
    }
    
    /**
     * Saves metadata for a work item.
     * 
     * @param workItemId the ID of the work item
     * @param key the metadata key
     * @param value the metadata value
     */
    public void saveWorkItemMetadata(UUID workItemId, String key, String value) {
        Map<String, String> metadata = workItemMetadata.computeIfAbsent(workItemId, k -> new HashMap<>());
        metadata.put(key, value);
    }
    
    /**
     * Retrieves metadata for a work item.
     * 
     * @param workItemId the ID of the work item
     * @param key the metadata key
     * @return the metadata value as an Optional
     */
    public Optional<String> getWorkItemMetadata(UUID workItemId, String key) {
        Map<String, String> metadata = workItemMetadata.get(workItemId);
        if (metadata == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(metadata.get(key));
    }
    
    /**
     * Sets a configuration flag.
     * 
     * @param flag the flag to set
     * @param value true to enable, false to disable
     */
    public void setConfigurationFlag(String flag, boolean value) {
        if (value) {
            configurationFlags.add(flag);
        } else {
            configurationFlags.remove(flag);
        }
    }
    
    /**
     * Checks if a configuration flag is set.
     * 
     * @param flag the flag to check
     * @return true if the flag is set, false otherwise
     */
    public boolean getConfigurationFlag(String flag) {
        return configurationFlags.contains(flag);
    }
    
    /**
     * Sets a configuration value.
     * 
     * @param key the configuration key
     * @param value the configuration value
     */
    public void setConfigurationValue(String key, Object value) {
        configurationValues.put(key, value);
    }
    
    /**
     * Retrieves a configuration value.
     * 
     * @param key the configuration key
     * @param <T> the type of the value
     * @return the configuration value as an Optional
     */
    @SuppressWarnings("unchecked")
    public <T> Optional<T> getConfigurationValue(String key) {
        return Optional.ofNullable((T) configurationValues.get(key));
    }
    
    /**
     * Saves an API token.
     * 
     * @param key the key for the token
     * @param token the API token
     */
    public void saveAPIToken(String key, APIToken token) {
        apiTokens.put(key, token);
    }
    
    /**
     * Returns an API token.
     * 
     * @param key the key for the token
     * @return the API token
     */
    public APIToken getAPIToken(String key) {
        return apiTokens.get(key);
    }
    
    /**
     * Saves a project.
     * 
     * @param key the key for the project
     * @param project the project
     */
    public void saveProject(String key, Project project) {
        projects.put(key, project);
    }
    
    /**
     * Returns a project.
     * 
     * @param key the key for the project
     * @return the project
     */
    public Project getProject(String key) {
        return projects.get(key);
    }
    
    /**
     * Saves a webhook configuration.
     * 
     * @param key the key for the configuration
     * @param config the webhook configuration
     */
    public void saveWebhookConfig(String key, WebhookConfig config) {
        webhookConfigs.put(key, config);
    }
    
    /**
     * Returns a webhook configuration.
     * 
     * @param key the key for the configuration
     * @return the webhook configuration
     */
    public WebhookConfig getWebhookConfig(String key) {
        return webhookConfigs.get(key);
    }
    
    /**
     * Saves a JSON payload.
     * 
     * @param key the key for the payload
     * @param payload the JSON payload as a string
     */
    public void saveJsonPayload(String key, String payload) {
        jsonPayloads.put(key, payload);
    }
    
    /**
     * Returns a JSON payload.
     * 
     * @param key the key for the payload
     * @return the JSON payload as a string
     */
    public String getJsonPayload(String key) {
        return jsonPayloads.get(key);
    }
    
    /**
     * Saves a client report.
     * 
     * @param key the key for the report
     * @param report the report as a map of attributes
     */
    public void saveClientReport(String key, Map<String, String> report) {
        clientReports.put(key, report);
    }
    
    /**
     * Returns a client report.
     * 
     * @param key the key for the report
     * @return the report as a map of attributes
     */
    public Map<String, String> getClientReport(String key) {
        return clientReports.get(key);
    }
    
    /**
     * Sets the last HTTP status code.
     * 
     * @param statusCode the HTTP status code
     */
    public void setStatusCode(int statusCode) {
        this.lastStatusCode = statusCode;
    }
    
    /**
     * Returns the last HTTP status code.
     * 
     * @return the HTTP status code
     */
    public int getStatusCode() {
        return lastStatusCode;
    }
    
    /**
     * Register a service to be used by step definitions.
     *
     * @param serviceClass the service class
     * @param service the service implementation
     * @param <T> the service type
     */
    public <T> void registerService(Class<T> serviceClass, T service) {
        services.put(serviceClass, service);
    }
    
    /**
     * Get a registered service.
     *
     * @param serviceClass the service class
     * @param <T> the service type
     * @return the service implementation
     */
    @SuppressWarnings("unchecked")
    public <T> T getService(Class<T> serviceClass) {
        return (T) services.get(serviceClass);
    }
    
    /**
     * Set the next input to be used by interactive commands.
     *
     * @param input the input string
     */
    public void setNextInput(String input) {
        inputQueue.add(input);
    }
    
    /**
     * Get the next input from the queue.
     *
     * @return the next input
     */
    public String getNextInput() {
        return inputQueue.poll();
    }
    
    /**
     * Create a command runner for executing commands in BDD tests.
     *
     * @return the command runner
     */
    public CommandRunner getCommandRunner() {
        return new CommandRunner(this);
    }
    
    /**
     * Set up input/output capture for testing interactive commands.
     */
    public void setupIOCapture() {
        capturedOutput = new ByteArrayOutputStream();
        capturedError = new ByteArrayOutputStream();
        
        System.setOut(new PrintStream(capturedOutput));
        System.setErr(new PrintStream(capturedError));
        
        // Set up input stream for interactive input if there's anything in the queue
        if (!inputQueue.isEmpty()) {
            StringBuilder inputBuilder = new StringBuilder();
            while (!inputQueue.isEmpty()) {
                inputBuilder.append(inputQueue.poll()).append("\n");
            }
            System.setIn(new ByteArrayInputStream(inputBuilder.toString().getBytes()));
        }
    }
    
    /**
     * Clean up after IO capture.
     *
     * @return an array with [stdout, stderr]
     */
    public String[] cleanupIOCapture() {
        System.setOut(System.out);
        System.setErr(System.err);
        System.setIn(System.in);
        
        return new String[]{
            capturedOutput.toString(),
            capturedError.toString()
        };
    }
    
    /**
     * Gets the user's role.
     *
     * @return the user's role
     */
    public String getUserRole() {
        return userRole;
    }
    
    /**
     * Sets the user's role.
     *
     * @param userRole the user's role
     */
    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }
    
    /**
     * Gets the last command output.
     *
     * @return the last command output
     */
    public String getLastCommandOutput() {
        return lastCommandOutput;
    }
    
    /**
     * Sets the last command output.
     *
     * @param output the last command output
     */
    public void setLastCommandOutput(String output) {
        this.lastCommandOutput = output;
    }
    
    /**
     * Helper class for running commands in BDD tests.
     */
    public static class CommandRunner {
        private final TestContext context;
        
        /**
         * Create a new command runner.
         *
         * @param context the test context
         */
        public CommandRunner(TestContext context) {
            this.context = context;
        }
        
        /**
         * Run a command and return the output.
         *
         * @param command the command to run
         * @param args the command arguments
         * @return an array with [stdout, stderr]
         */
        public String[] runCommand(String command, String args) {
            // Set up IO capture
            context.setupIOCapture();
            
            try {
                // This would actually invoke the command in a real implementation
                // For our mock implementation, we'll just return some simulated output
                switch (command) {
                    case "rin":
                        // Parse the command
                        String[] parts = args.split("\\s+", 2);
                        String subCommand = parts[0];
                        
                        switch (subCommand) {
                            case "makechildren":
                                return simulateMakeChildrenCommand(parts.length > 1 ? parts[1] : "");
                            case "list":
                                return simulateListCommand(parts.length > 1 ? parts[1] : "");
                            case "update":
                                return simulateUpdateCommand(parts.length > 1 ? parts[1] : "");
                            case "print":
                                return simulatePrintCommand(parts.length > 1 ? parts[1] : "");
                            default:
                                System.out.println("Simulated output for command: " + command + " " + args);
                                break;
                        }
                        break;
                    default:
                        System.out.println("Unknown command: " + command);
                        System.err.println("Error: Command not found: " + command);
                        break;
                }
            } finally {
                return context.cleanupIOCapture();
            }
            
            return new String[]{"", "Error: Command not implemented"};
        }
        
        private String[] simulateMakeChildrenCommand(String args) {
            if (args.isEmpty()) {
                System.err.println("Error: No work item IDs provided");
                return context.cleanupIOCapture();
            }
            
            if (args.matches(".*[a-zA-Z].*") && !args.contains("--title=")) {
                System.err.println("Error: Invalid work item ID format");
                return context.cleanupIOCapture();
            }
            
            // Check for --title parameter
            String title = "Parent of child items";
            if (args.contains("--title=")) {
                String[] parts = args.split("--title=", 2);
                if (parts.length > 1) {
                    String titlePart = parts[1];
                    // Extract title from quotes if present
                    if (titlePart.startsWith("'") && titlePart.contains("'")) {
                        titlePart = titlePart.substring(1, titlePart.indexOf("'", 1));
                    }
                    title = titlePart;
                }
            }
            
            System.out.println("Successfully created parent work item with title: " + title);
            System.out.println("Parent ID: " + java.util.UUID.randomUUID());
            
            return context.cleanupIOCapture();
        }
        
        private String[] simulateListCommand(String args) {
            if ("p".equals(args)) {
                System.out.println("ID                                      | Title             | Type   | Priority | State        | Assignee");
                System.out.println("--------------------------------------- | ----------------- | ------ | -------- | ------------ | --------");
                System.out.println(java.util.UUID.randomUUID() + " | User Auth Features | FEATURE | HIGH     | IN_PROGRESS  | bob");
                System.out.println(java.util.UUID.randomUUID() + " | Admin Functions   | FEATURE | MEDIUM   | READY        | alice");
                
                return context.cleanupIOCapture();
            } else if ("pretty".equals(args)) {
                System.out.println("Project Alpha");
                System.out.println("  ├── User Management");
                System.out.println("  │   ├── Login UI");
                System.out.println("  │   └── Registration");
                System.out.println("  ├── Admin Panel");
                System.out.println("  │   └── User Roles");
                System.out.println("  └── Settings UI");
                
                return context.cleanupIOCapture();
            }
            
            System.out.println("ID                                      | Title             | Type | Priority | State        | Assignee");
            System.out.println("--------------------------------------- | ----------------- | ---- | -------- | ------------ | --------");
            System.out.println(java.util.UUID.randomUUID() + " | Task 1            | TASK | MEDIUM   | IN_PROGRESS  | bob");
            System.out.println(java.util.UUID.randomUUID() + " | Task 2            | TASK | LOW      | READY        | alice");
            
            return context.cleanupIOCapture();
        }
        
        private String[] simulateUpdateCommand(String args) {
            if (args.isEmpty()) {
                System.err.println("Error: No work item ID provided");
                return context.cleanupIOCapture();
            }
            
            System.out.println("Work Item: " + args);
            System.out.println("[1] Title: Implement registration form");
            System.out.println("[2] Description: Create a new registration form with validation");
            System.out.println("[3] Priority: MEDIUM");
            System.out.println("[4] State: IN_PROGRESS");
            System.out.println("[5] Assignee: bob");
            System.out.println("[0] Cancel");
            System.out.println();
            System.out.println("Enter the number of the field to update: ");
            
            // Simulate reading user input
            String fieldSelection = context.getNextInput();
            if (fieldSelection == null) {
                fieldSelection = "0"; // Default to cancel
            }
            
            int field = 0;
            try {
                field = Integer.parseInt(fieldSelection);
            } catch (NumberFormatException e) {
                System.err.println("Error: Invalid selection");
                return context.cleanupIOCapture();
            }
            
            if (field < 0 || field > 5) {
                System.err.println("Error: Invalid selection");
                return context.cleanupIOCapture();
            }
            
            if (field == 0) {
                System.out.println("Update cancelled");
                return context.cleanupIOCapture();
            }
            
            System.out.println("Enter new value: ");
            String newValue = context.getNextInput();
            if (newValue == null || newValue.isEmpty()) {
                System.err.println("Error: Empty value not allowed");
                return context.cleanupIOCapture();
            }
            
            System.out.println("Field updated successfully");
            
            return context.cleanupIOCapture();
        }
        
        private String[] simulatePrintCommand(String args) {
            if (args.isEmpty()) {
                System.err.println("Error: No work item ID provided");
                return context.cleanupIOCapture();
            }
            
            if (args.matches(".*[a-zA-Z].*")) {
                System.err.println("Error: Invalid work item ID");
                return context.cleanupIOCapture();
            }
            
            System.out.println("Work Item Details");
            System.out.println("----------------");
            System.out.println("ID: " + java.util.UUID.randomUUID());
            System.out.println("Title: Implement registration form");
            System.out.println("Description: Create a new registration form with validation");
            System.out.println("Type: TASK");
            System.out.println("Priority: MEDIUM");
            System.out.println("State: IN_PROGRESS");
            System.out.println("Assignee: bob");
            System.out.println("Reporter: alice");
            System.out.println("Created: 2025-04-05T14:32:10Z");
            System.out.println("Updated: 2025-04-06T09:15:22Z");
            System.out.println();
            System.out.println("Parent: " + java.util.UUID.randomUUID() + " (User Authentication Feature)");
            System.out.println("Children: None");
            System.out.println();
            System.out.println("History:");
            System.out.println("2025-04-05T14:32:10Z: STATE_CHANGE by alice: READY → IN_PROGRESS");
            System.out.println("2025-04-06T09:15:22Z: ASSIGNMENT_CHANGE by alice: alice → bob");
            System.out.println();
            System.out.println("Metadata:");
            System.out.println("estimated_hours: 8");
            System.out.println("actual_hours: 6");
            System.out.println("sprint: Sprint 42");
            System.out.println("story_points: 5");
            System.out.println("test_coverage: 87.5%");
            
            return context.cleanupIOCapture();
        }
    }
}