/**
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * 
 * Developed with analytical assistance from AI tools.
 * All rights reserved.
 * 
 * This source code is licensed under the MIT License
 * found in the LICENSE file in the root directory of this source tree.
 */
package org.rinna;

import org.rinna.adapter.repository.InMemoryItemRepository;
import org.rinna.adapter.repository.InMemoryMetadataRepository;
import org.rinna.adapter.repository.InMemoryQueueRepository;
import org.rinna.adapter.repository.InMemoryReleaseRepository;
import org.rinna.adapter.service.DefaultItemService;
import org.rinna.adapter.service.DefaultQueueService;
import org.rinna.adapter.service.DefaultReleaseService;
import org.rinna.adapter.service.DefaultWorkflowService;
import org.rinna.domain.service.ItemService;
import org.rinna.domain.service.QueueService;
import org.rinna.domain.service.ReleaseService;
import org.rinna.domain.service.WorkflowService;

/**
 * Main application entry point for Rinna.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("Rinna Workflow Management");
        
        // Let Rinna initialize itself with default implementations
        Rinna.initialize();
    }
}
