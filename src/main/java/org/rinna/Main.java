/*
 * Main application entry point for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */
package org.rinna;

import org.rinna.adapter.service.ApiHealthServer;
import org.rinna.config.RinnaConfig;
import org.rinna.data.sqlite.SqliteConnectionManager;
import org.rinna.data.sqlite.SqliteRepositoryFactory;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.repository.MetadataRepository;
import org.rinna.logging.MultiLanguageLogger;
import org.rinna.repository.ItemRepository;
import org.rinna.repository.QueueRepository;
import org.rinna.repository.ReleaseRepository;
import org.rinna.usecase.ItemService;
import org.rinna.usecase.QueueService;
import org.rinna.usecase.ReleaseService;
import org.rinna.usecase.WorkflowService;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;

/**
 * Main application entry point for Rinna.
 * <p>
 * This class provides a command-line interface for the Rinna workflow management system.
 * It supports different storage backends (in-memory or SQLite) and can start the API server.
 * <p>
 * Available command-line options:
 * <ul>
 *   <li>--help: Show help information</li>
 *   <li>--version: Show version information</li>
 *   <li>--api: Start the API server</li>
 *   <li>--port: Specify the port for the API server (default: 8081)</li>
 *   <li>--storage: Specify the storage backend (memory or sqlite, default: memory)</li>
 *   <li>--db-path: Specify the path to the SQLite database file (default: ~/.rinna/rinna.db)</li>
 *   <li>--init-demo: Initialize with demo data</li>
 * </ul>
 */
public class Main {
    private static final MultiLanguageLogger logger = MultiLanguageLogger.getLogger(Main.class);
    private static final String VERSION = "1.10.0";
    private static final int DEFAULT_PORT = 8081;
    
    private static RinnaInstanceBuilder rinnaBuilder;
    
    /**
     * Main entry point.
     *
     * @param args command-line arguments
     */
    public static void main(String[] args) {
        // Parse command-line arguments
        Map<String, String> options = parseArgs(args);
        
        // Show help and exit if requested
        if (options.containsKey("help")) {
            showHelp();
            return;
        }
        
        // Show version and exit if requested
        if (options.containsKey("version")) {
            showVersion();
            return;
        }
        
        try {
            // Create a builder for the Rinna instance
            // Initialize the multi-language logging system
            MultiLanguageLogger.initialize();
            
            // Create the Rinna instance builder
            rinnaBuilder = new RinnaInstanceBuilder();
            
            // Configure the storage backend
            configureStorage(options);
            
            // Build the Rinna instance
            Rinna rinna = rinnaBuilder.build();
            
            // Initialize with demo data if requested
            if (options.containsKey("init-demo")) {
                initializeDemoData(rinna);
            }
            
            // Start the API server if requested
            if (options.containsKey("api")) {
                startApiServer(options, rinna);
            } else {
                // If API server is not started, print a welcome message and exit
                System.out.println("Rinna Workflow Management System " + VERSION);
                System.out.println("Use --help to see available options");
                System.out.println("Exiting as no action was requested");
            }
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            logger.error("Application error", e);
            System.exit(1);
        }
    }
    
    /**
     * Parses command-line arguments into a map of options.
     *
     * @param args the command-line arguments
     * @return a map of options
     */
    private static Map<String, String> parseArgs(String[] args) {
        Map<String, String> options = new HashMap<>();
        
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            
            if (arg.equals("--help") || arg.equals("-h")) {
                options.put("help", "true");
            } else if (arg.equals("--version") || arg.equals("-v")) {
                options.put("version", "true");
            } else if (arg.equals("--api")) {
                options.put("api", "true");
            } else if (arg.equals("--port") && i + 1 < args.length) {
                options.put("port", args[++i]);
            } else if (arg.equals("--storage") && i + 1 < args.length) {
                options.put("storage", args[++i]);
            } else if (arg.equals("--db-path") && i + 1 < args.length) {
                options.put("db-path", args[++i]);
            } else if (arg.equals("--init-demo")) {
                options.put("init-demo", "true");
            } else {
                System.err.println("Unknown option: " + arg);
                options.put("help", "true");
                break;
            }
        }
        
        return options;
    }
    
    /**
     * Displays help information.
     */
    private static void showHelp() {
        System.out.println("Rinna Workflow Management System " + VERSION);
        System.out.println("Usage: java -jar rinna.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --help, -h              Show this help message");
        System.out.println("  --version, -v           Show version information");
        System.out.println("  --api                   Start the API server");
        System.out.println("  --port <port>           Specify the port for the API server (default: 8081)");
        System.out.println("  --storage <type>        Specify the storage backend (memory or sqlite, default: memory)");
        System.out.println("  --db-path <path>        Specify the path to the SQLite database file");
        System.out.println("                          (default: ~/.rinna/rinna.db)");
        System.out.println("  --init-demo             Initialize with demo data");
        System.out.println();
        System.out.println("Examples:");
        System.out.println("  java -jar rinna.jar --api                 Start the API server with in-memory storage");
        System.out.println("  java -jar rinna.jar --api --port 8080     Start the API server on port 8080");
        System.out.println("  java -jar rinna.jar --api --storage sqlite Start the API server with SQLite storage");
    }
    
    /**
     * Displays version information.
     */
    private static void showVersion() {
        System.out.println("Rinna Workflow Management System " + VERSION);
        System.out.println("Java version: " + System.getProperty("java.version"));
        System.out.println("OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version"));
    }
    
    /**
     * Configures the storage backend based on command-line options.
     *
     * @param options the command-line options
     */
    private static void configureStorage(Map<String, String> options) {
        String storageType = options.getOrDefault("storage", "memory");
        
        if (storageType.equalsIgnoreCase("sqlite")) {
            // Get the database path from options or use the default
            String dbPath = options.getOrDefault("db-path", null);
            
            if (dbPath != null) {
                // Use the specified database path
                Path path = Paths.get(dbPath);
                
                // Ensure the parent directory exists
                Path parent = path.getParent();
                if (parent != null && !Files.exists(parent)) {
                    try {
                        Files.createDirectories(parent);
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to create directory: " + parent, e);
                    }
                }
                
                // Extract the directory and filename
                String dir = path.getParent().toString();
                String filename = path.getFileName().toString();
                
                // Configure the SQLite storage
                SqliteConnectionManager connectionManager = new SqliteConnectionManager(dir, filename);
                SqliteRepositoryFactory repositoryFactory = new SqliteRepositoryFactory(connectionManager);
                
                rinnaBuilder.withItemRepository(repositoryFactory.getItemRepository())
                           .withMetadataRepository(repositoryFactory.getMetadataRepository());
                
                logger.info("Using SQLite storage at {}", path);
                System.out.println("Using SQLite storage at " + path);
            } else {
                // Use the default database path
                SqliteRepositoryFactory repositoryFactory = new SqliteRepositoryFactory();
                
                rinnaBuilder.withItemRepository(repositoryFactory.getItemRepository())
                           .withMetadataRepository(repositoryFactory.getMetadataRepository());
                
                logger.info("Using SQLite storage at default location");
                System.out.println("Using SQLite storage at default location");
            }
        } else {
            // Use in-memory storage (default)
            logger.info("Using in-memory storage");
            System.out.println("Using in-memory storage");
            
            // The builder will use in-memory repositories by default
        }
    }
    
    /**
     * Starts the API server.
     *
     * @param options the command-line options
     * @param rinna the Rinna instance
     */
    private static void startApiServer(Map<String, String> options, Rinna rinna) {
        // Get the port from options or use the default
        int port;
        try {
            port = Integer.parseInt(options.getOrDefault("port", String.valueOf(DEFAULT_PORT)));
        } catch (NumberFormatException e) {
            System.err.println("Invalid port number: " + options.get("port"));
            port = DEFAULT_PORT;
        }
        
        try {
            // Start the API server
            if (rinna.startApiServer(port)) {
                logger.info("API server started on port {}", port);
                System.out.println("API server started on port " + port);
                System.out.println("Press Ctrl+C to stop");
                
                // Add a shutdown hook to stop the API server gracefully
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    System.out.println("Shutting down Rinna...");
                    rinna.stopApiServer();
                }));
                
                // Keep the main thread alive
                try {
                    Thread.currentThread().join();
                } catch (InterruptedException e) {
                    logger.info("Main thread interrupted, shutting down");
                }
            } else {
                System.err.println("Failed to start API server on port " + port);
                System.exit(1);
            }
        } catch (Exception e) {
            System.err.println("Error starting API server: " + e.getMessage());
            logger.error("API server error", e);
            System.exit(1);
        }
    }
    
    /**
     * Initializes the system with demo data.
     *
     * @param rinna the Rinna instance
     */
    private static void initializeDemoData(Rinna rinna) {
        logger.info("Initializing demo data");
        System.out.println("Initializing demo data...");
        
        ItemService itemService = rinna.items();
        
        // Create some work items
        List<WorkItemCreateRequest> demoItems = Arrays.asList(
            // Features
            new WorkItemCreateRequest.Builder()
                .title("Implement user authentication")
                .description("Add user authentication with OAuth2")
                .type(WorkItemType.FEATURE)
                .priority(Priority.HIGH)
                .assignee("alice")
                .build(),
                
            new WorkItemCreateRequest.Builder()
                .title("Add dashboard statistics")
                .description("Create statistics dashboard for project overview")
                .type(WorkItemType.FEATURE)
                .priority(Priority.MEDIUM)
                .assignee("bob")
                .build(),
                
            // Bugs
            new WorkItemCreateRequest.Builder()
                .title("Fix sorting in work item list")
                .description("Items are not sorted correctly by priority")
                .type(WorkItemType.BUG)
                .priority(Priority.HIGH)
                .assignee("charlie")
                .build(),
                
            new WorkItemCreateRequest.Builder()
                .title("Error when creating work item with special characters")
                .description("Work item creation fails when title contains '&' or '<' characters")
                .type(WorkItemType.BUG)
                .priority(Priority.MEDIUM)
                .assignee("alice")
                .build(),
                
            // Chores
            new WorkItemCreateRequest.Builder()
                .title("Update dependencies")
                .description("Update all dependencies to the latest versions")
                .type(WorkItemType.CHORE)
                .priority(Priority.LOW)
                .assignee("bob")
                .build(),
                
            new WorkItemCreateRequest.Builder()
                .title("Refactor error handling")
                .description("Improve error handling and logging across the system")
                .type(WorkItemType.CHORE)
                .priority(Priority.MEDIUM)
                .assignee("charlie")
                .build()
        );
        
        // Create the demo items
        for (WorkItemCreateRequest request : demoItems) {
            itemService.create(request);
        }
        
        logger.info("Demo data initialized");
        System.out.println("Demo data initialized");
    }
    
    /**
     * Builder for creating Rinna instances with custom configurations.
     */
    private static class RinnaInstanceBuilder {
        private ItemRepository itemRepository;
        private MetadataRepository metadataRepository;
        
        /**
         * Sets the item repository.
         *
         * @param itemRepository the item repository
         * @return this builder
         */
        public RinnaInstanceBuilder withItemRepository(ItemRepository itemRepository) {
            this.itemRepository = itemRepository;
            return this;
        }
        
        /**
         * Sets the metadata repository.
         *
         * @param metadataRepository the metadata repository
         * @return this builder
         */
        public RinnaInstanceBuilder withMetadataRepository(MetadataRepository metadataRepository) {
            this.metadataRepository = metadataRepository;
            return this;
        }
        
        /**
         * Builds a Rinna instance with the configured repositories.
         *
         * @return a new Rinna instance
         */
        public Rinna build() {
            if (itemRepository != null && metadataRepository != null) {
                // Create services
                ItemService itemService = new org.rinna.adapter.service.DefaultItemService(itemRepository);
                
                WorkflowService workflowService = new org.rinna.adapter.service.DefaultWorkflowService(itemRepository);
                
                QueueRepository queueRepository = new org.rinna.adapter.repository.InMemoryQueueRepository();
                QueueService queueService = new org.rinna.adapter.service.DefaultQueueService(
                        queueRepository, itemService, metadataRepository);
                
                ReleaseRepository releaseRepository = new org.rinna.adapter.repository.InMemoryReleaseRepository();
                ReleaseService releaseService = new org.rinna.adapter.service.DefaultReleaseService(
                        releaseRepository, itemService);
                
                // Create and return the Rinna instance
                return new Rinna(itemService, workflowService, releaseService, queueService, metadataRepository);
            } else {
                // If no repositories are specified, use the default initialization
                return Rinna.initialize();
            }
        }
    }
}
