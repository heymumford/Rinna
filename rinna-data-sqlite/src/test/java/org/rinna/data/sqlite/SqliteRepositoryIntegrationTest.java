/*
 * SQLite persistence integration tests for the Rinna workflow management system
 *
 * Copyright (c) 2025 Eric C. Mumford (@heymumford)
 * This file is subject to the terms and conditions defined in
 * the LICENSE file, which is part of this source code package.
 */

package org.rinna.data.sqlite;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.rinna.domain.model.Priority;
import org.rinna.domain.model.WorkItem;
import org.rinna.domain.model.WorkItemCreateRequest;
import org.rinna.domain.model.WorkItemMetadata;
import org.rinna.domain.model.WorkItemType;
import org.rinna.domain.repository.MetadataRepository;
import org.rinna.repository.ItemRepository;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the SQLite repositories.
 * These tests verify that the repositories work correctly with a real SQLite database.
 */
@Tag("integration")
class SqliteRepositoryIntegrationTest {
    
    @TempDir
    Path tempDir;
    
    private SqliteRepositoryFactory factory;
    private ItemRepository itemRepository;
    private MetadataRepository metadataRepository;
    
    @BeforeEach
    void setUp() {
        // Use a temporary directory for test database
        SqliteConnectionManager connectionManager = new SqliteConnectionManager(
                tempDir.toString(), "integration-test.db");
        
        factory = new SqliteRepositoryFactory(connectionManager);
        itemRepository = factory.getItemRepository();
        metadataRepository = factory.getMetadataRepository();
    }
    
    @AfterEach
    void tearDown() {
        if (factory != null) {
            factory.close();
        }
    }
    
    @Test
    void testFullWorkflowIntegration() {
        // 1. Create some work items with metadata
        WorkItem feature = createWorkItem("Major Feature", "A major feature implementation",
                WorkItemType.FEATURE, Priority.HIGH, "alice", createMetadata("component", "core"));
        
        WorkItem bug = createWorkItem("Critical Bug", "A critical bug fix",
                WorkItemType.BUG, Priority.HIGH, "bob", createMetadata("severity", "high"));
        
        WorkItem chore = createWorkItem("Refactoring", "Code refactoring task",
                WorkItemType.CHORE, Priority.MEDIUM, "charlie", createMetadata("effort", "medium"));
        
        // 2. Verify they were created and can be retrieved
        List<WorkItem> allItems = itemRepository.findAll();
        assertEquals(3, allItems.size());
        
        // 3. Verify finding by various criteria
        // By type
        List<WorkItem> features = itemRepository.findByType(WorkItemType.FEATURE);
        assertEquals(1, features.size());
        assertEquals("Major Feature", features.get(0).getTitle());
        
        // By assignee
        List<WorkItem> bobItems = itemRepository.findByAssignee("bob");
        assertEquals(1, bobItems.size());
        assertEquals("Critical Bug", bobItems.get(0).getTitle());
        
        // By custom field (metadata)
        List<WorkItem> highSeverityItems = itemRepository.findByCustomField("severity", "high");
        assertEquals(1, highSeverityItems.size());
        assertEquals("Critical Bug", highSeverityItems.get(0).getTitle());
        
        // 4. Test metadata operations
        Map<String, String> featureMetadata = metadataRepository.getMetadataMap(feature.getId());
        assertEquals(1, featureMetadata.size());
        assertEquals("core", featureMetadata.get("component"));
        
        // Add more metadata
        Map<String, String> additionalMetadata = new HashMap<>();
        additionalMetadata.put("component", "ui"); // Update existing
        additionalMetadata.put("priority", "1");   // Add new
        itemRepository.updateMetadata(feature.getId(), additionalMetadata);
        
        Map<String, String> updatedMetadata = metadataRepository.getMetadataMap(feature.getId());
        assertEquals(2, updatedMetadata.size());
        assertEquals("ui", updatedMetadata.get("component"));
        assertEquals("1", updatedMetadata.get("priority"));
        
        // 5. Verify finding by updated metadata
        List<WorkItem> uiComponents = itemRepository.findByCustomField("component", "ui");
        assertEquals(1, uiComponents.size());
        assertEquals("Major Feature", uiComponents.get(0).getTitle());
        
        // 6. Delete an item and verify it's gone
        itemRepository.deleteById(chore.getId());
        List<WorkItem> remainingItems = itemRepository.findAll();
        assertEquals(2, remainingItems.size());
        
        // The metadata should be deleted too (cascade)
        List<WorkItemMetadata> allMetadata = metadataRepository.findAll();
        for (WorkItemMetadata metadata : allMetadata) {
            assertNotEquals(chore.getId(), metadata.getWorkItemId());
        }
    }
    
    private WorkItem createWorkItem(String title, String description, WorkItemType type,
                                    Priority priority, String assignee, Map<String, String> metadata) {
        WorkItemCreateRequest request = new WorkItemCreateRequest.Builder()
                .title(title)
                .description(description)
                .type(type)
                .priority(priority)
                .assignee(assignee)
                .metadata(metadata)
                .build();
        
        return itemRepository.create(request);
    }
    
    private Map<String, String> createMetadata(String key, String value) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put(key, value);
        return metadata;
    }
}