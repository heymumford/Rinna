/*
 * Entry point for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna;

import org.rinna.adapter.service.ApiHealthServer;
import org.rinna.config.RinnaConfig;
import org.rinna.repository.MetadataRepository;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.QueueService;
import org.rinna.domain.service.ReleaseService;
import org.rinna.domain.service.WorkflowService;

import java.io.IOException;

/**
 * Main entry point for the Rinna system.
 * This class provides access to the core services of the system.
 */
public class Rinna {
    private static Rinna instance;
    
    private final ItemService itemService;
    private final WorkflowService workflowService;
    private final ReleaseService releaseService;
    private final QueueService queueService;
    private final MetadataRepository metadataRepository;
    private ApiHealthServer apiServer;
    
    /**
     * Constructs a new Rinna instance with the specified services.
     *
     * @param itemService the item service to use
     * @param workflowService the workflow service to use
     * @param releaseService the release service to use
     * @param queueService the queue service to use
     * @param metadataRepository the metadata repository to use
     */
    public Rinna(ItemService itemService, WorkflowService workflowService, 
                ReleaseService releaseService, QueueService queueService,
                MetadataRepository metadataRepository) {
        this.itemService = itemService;
        this.workflowService = workflowService;
        this.releaseService = releaseService;
        this.queueService = queueService;
        this.metadataRepository = metadataRepository;
    }
    
    /**
     * Returns the item service.
     *
     * @return the item service
     */
    public ItemService items() {
        return itemService;
    }
    
    /**
     * Returns the workflow service.
     *
     * @return the workflow service
     */
    public WorkflowService workflow() {
        return workflowService;
    }
    
    /**
     * Initializes a new Rinna instance with default services.
     *
     * @return a new Rinna instance
     */
    public static synchronized Rinna initialize() {
        if (instance == null) {
            // This is a simplified initialization for demonstration purposes
            // In a real application, services should be properly injected using a DI framework
            
            // Create in-memory repositories
            var itemRepository = new org.rinna.adapter.repository.InMemoryItemRepository();
            var queueRepository = new org.rinna.adapter.repository.InMemoryQueueRepository();
            var releaseRepository = new org.rinna.adapter.repository.InMemoryReleaseRepository();
            var metadataRepository = new org.rinna.adapter.repository.InMemoryMetadataRepository();
            
            // Create services
            var itemService = new org.rinna.adapter.service.DefaultItemService(itemRepository);
            var releaseService = new org.rinna.adapter.service.DefaultReleaseService(releaseRepository, itemService);
            var queueService = new org.rinna.adapter.service.DefaultQueueService(queueRepository, itemService, metadataRepository);
            var workflowService = new org.rinna.adapter.service.DefaultWorkflowService(itemRepository);
            
            instance = new Rinna(
                itemService, 
                workflowService, 
                releaseService,
                queueService,
                metadataRepository
            );
        }
        return instance;
    }
    
    /**
     * Returns the release service.
     *
     * @return the release service
     */
    public ReleaseService releases() {
        return releaseService;
    }
    
    /**
     * Returns the queue service.
     *
     * @return the queue service
     */
    public QueueService queue() {
        return queueService;
    }
    
    /**
     * Returns the metadata repository.
     *
     * @return the metadata repository
     */
    public MetadataRepository getMetadataRepository() {
        return metadataRepository;
    }
    
    /**
     * Starts the API server.
     * 
     * @param port the port to listen on
     * @return true if the server was started successfully, false otherwise
     */
    public boolean startApiServer(int port) {
        try {
            apiServer = new ApiHealthServer(port);
            apiServer.start();
            return true;
        } catch (IOException e) {
            System.err.println("Failed to start API server: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Stops the API server.
     */
    public void stopApiServer() {
        if (apiServer != null) {
            apiServer.stop();
            apiServer = null;
        }
    }
    
    /**
     * Main method to run the Rinna system.
     * 
     * @param args command line arguments
     */
    public static void main(String[] args) {
        // Parse arguments
        int port = 8081;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number: " + args[0]);
                System.exit(1);
            }
        }
        
        // Initialize the system
        Rinna rinna = initialize();
        
        // Start the API server
        if (!rinna.startApiServer(port)) {
            System.exit(1);
        }
        
        // Add a shutdown hook to stop the API server gracefully
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down Rinna...");
            rinna.stopApiServer();
        }));
        
        System.out.println("Rinna system started on port " + port);
        System.out.println("Press Ctrl+C to stop");
    }
}